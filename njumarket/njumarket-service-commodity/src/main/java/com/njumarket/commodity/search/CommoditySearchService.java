package com.njumarket.commodity.search;

import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.json.JsonData;
import com.njumarket.commodity.client.AIClient;
import com.njumarket.commodity.entity.Commodity;
import com.njumarket.commodity.repository.CommodityRepository;
import com.njumarket.njumarket.dto.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataRetrievalFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.client.elc.NativeQueryBuilder;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * 商品搜索服务。
 * 索引创建/重建时通过 Feign 调用 AI 服务单次 Chat 获取丰度更高的可检索文本，
 * 写入 ES 的 keywordPayload，提升泛化词（如「笔记本」）的召回。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CommoditySearchService {

    private static final String STATUS_ON_SHELF = "ON_SHELF";
    private static final String VISIBILITY_PUBLIC = "PUBLIC";

    private final CommoditySearchRepository commoditySearchRepository;
    private final ElasticsearchOperations elasticsearchOperations;
    private final CommodityRepository commodityRepository;
    private final CommoditySearchProperties properties;
    private final AIClient aiClient;

    public boolean isEnabled() {
        return properties.isEnabled() && properties.getElasticsearch().isEnabled();
    }

    public boolean shouldUseElasticSearch(String keyword, String location, Double minPrice, Double maxPrice, String category) {
        if (!isEnabled()) {
            return false;
        }
        return StringUtils.hasText(keyword)
                || StringUtils.hasText(location)
                || minPrice != null
                || maxPrice != null
                || StringUtils.hasText(category);
    }

    public Optional<CommoditySearchResult> search(String keyword,
                                                  int page,
                                                  int size,
                                                  String location,
                                                  Double minPrice,
                                                  Double maxPrice,
                                                  String category,
                                                  String sortBy) {
        return search(keyword, page, size, location, minPrice, maxPrice, category, sortBy, null);
    }

    public Optional<CommoditySearchResult> search(String keyword,
                                                  int page,
                                                  int size,
                                                  String location,
                                                  Double minPrice,
                                                  Double maxPrice,
                                                  String category,
                                                  String sortBy,
                                                  String userId) {
        if (!isEnabled()) {
            return Optional.empty();
        }

        try {
            Pageable pageable = PageRequest.of(Math.max(page - 1, 0), Math.max(size, 1), resolveSort(sortBy));
            NativeQueryBuilder queryBuilder = buildQueryBuilder(keyword, location, minPrice, maxPrice, category, pageable, userId);
            NativeQuery query = queryBuilder.build();

            if (log.isDebugEnabled()) {
                log.debug("ElasticSearch 查询参数：keyword={}, location={}, category={}, minPrice={}, maxPrice={}, page={}, size={}",
                        keyword, location, category, minPrice, maxPrice, page, size);
            }

            SearchHits<CommoditySearchDocument> searchHits;
            try {
                // 使用 _source 过滤，排除 publishTime 字段，避免日期转换问题
                NativeQuery queryWithSourceFilter = NativeQuery.builder()
                        .withQuery(query.getQuery())
                        .withPageable(query.getPageable())
                        .withSourceFilter(new org.springframework.data.elasticsearch.core.query.SourceFilter() {
                            @Override
                            public String[] getIncludes() {
                                // 只包含需要的字段，排除 publishTime
                                return new String[]{"commodityId", "title", "description", "keywordPayload",
                                    "commodityStatus", "buyerVisibility", "category",
                                    "price", "addressSnapshotFull"};
                            }
                            
                            @Override
                            public String[] getExcludes() {
                                // 明确排除 publishTime，避免日期转换问题
                                return new String[]{"publishTime"};
                            }
                        })
                        .build();
                searchHits = elasticsearchOperations.search(queryWithSourceFilter, CommoditySearchDocument.class);
            } catch (org.springframework.data.elasticsearch.core.convert.ConversionException e) {
                // 如果还是失败，尝试只获取 ID
                log.warn("文档反序列化失败（日期转换问题），尝试备用方案: {}", e.getMessage());
                try {
                    // 备用方案：使用原生查询，只获取 _id
                    searchHits = elasticsearchOperations.search(query, CommoditySearchDocument.class);
                } catch (Exception e2) {
                    log.error("ElasticSearch 查询失败（日期转换问题），将回退到数据库: {}", e2.getMessage(), e2);
                    return Optional.empty();
                }
            } catch (Exception e) {
                log.error("ElasticSearch 查询失败: {}", e.getMessage(), e);
                return Optional.empty();
            }
            
            if (searchHits == null) {
                log.warn("ElasticSearch 查询返回 null");
                return Optional.empty();
            }

            long totalHits = searchHits.getTotalHits();
            log.info("ElasticSearch 查询结果：totalHits={}, 实际返回={}", totalHits, searchHits.getSearchHits().size());

            if (totalHits == 0) {
                // 诊断：检查索引中是否有数据
                try {
                    Query matchAllQuery = Query.of(q -> q.matchAll(ma -> ma));
                    NativeQuery countQuery = NativeQuery.builder().withQuery(matchAllQuery).build();
                    SearchHits<CommoditySearchDocument> allHits = elasticsearchOperations.search(countQuery, CommoditySearchDocument.class);
                    long totalDocs = allHits.getTotalHits();
                    log.warn("索引诊断：索引中总文档数={}，但查询命中0条。可能原因：1)商品状态不是ON_SHELF 2)可见性不是PUBLIC 3)关键词不匹配", totalDocs);
                    
                    if (totalDocs > 0 && log.isDebugEnabled()) {
                        // 采样检查前几条文档的状态
                        allHits.getSearchHits().stream().limit(3).forEach(hit -> {
                            CommoditySearchDocument doc = hit.getContent();
                            log.debug("样本文档：id={}, status={}, buyerVis={}, title='{}'",
                                    doc.getCommodityId(), doc.getCommodityStatus(),
                                    doc.getBuyerVisibility(), doc.getTitle());
                        });
                    }
                } catch (Exception e) {
                    log.debug("诊断查询失败: {}", e.getMessage());
                }
            }

            // 直接从 SearchHit 获取 ID，避免反序列化完整文档（解决日期转换问题）
            List<String> ids = searchHits.getSearchHits().stream()
                    .map(SearchHit::getId)  // 直接使用 _id，不反序列化 _source
                    .collect(Collectors.toList());

            Map<String, List<String>> highlights = extractHighlights(searchHits);

            return Optional.of(new CommoditySearchResult(ids, totalHits, highlights));
        } catch (Exception e) {
            log.error("ElasticSearch 查询失败，将回退到数据库: {}", e.getMessage(), e);
            return Optional.empty();
        }
    }

    /**
     * 同步商品到 ES 索引。
     * 只有 ON_SHELF 状态的商品才写入索引；其他状态（DRAFT / PUBLISHED / OFF_SHELF）
     * 直接从索引中删除，保持索引干净，避免无用文档占用空间。
     */
    public void syncCommodity(Commodity commodity) {
        if (!isEnabled() || commodity == null) {
            return;
        }
        // 非上架状态 → 从索引中移除（幂等，不存在时静默忽略）
        if (!STATUS_ON_SHELF.equalsIgnoreCase(commodity.getCommodityStatus())) {
            removeCommodity(commodity.getCommodityId());
            return;
        }
        try {
            log.debug("同步商品到 ES：id={}, status={}", commodity.getCommodityId(), commodity.getCommodityStatus());
            CommoditySearchDocument doc = buildSearchDocumentWithEnrichment(commodity);
            commoditySearchRepository.save(doc);
        } catch (Exception e) {
            log.warn("商品同步到 ElasticSearch 失败: commodityId={}, error={}", commodity.getCommodityId(), e.getMessage());
        }
    }

    public void syncCommodity(String commodityId) {
        if (!isEnabled()) {
            return;
        }
        Commodity commodity = commodityRepository.findById(commodityId)
                .orElseThrow(() -> new DataRetrievalFailureException("商品不存在: " + commodityId));
        syncCommodity(commodity);
    }

    public void removeCommodity(String commodityId) {
        if (!isEnabled()) {
            return;
        }
        try {
            commoditySearchRepository.deleteById(commodityId);
        } catch (Exception e) {
            log.warn("从 ElasticSearch 删除商品失败: commodityId={}, error={}", commodityId, e.getMessage());
        }
    }

    /**
     * 构建搜索文档并同步向量：
     * 1) 通过 AI 单次 Chat 生成增广文本；
     * 2) 将增广文本（含结构化字段）发送给 AI 服务做向量化并写入 Milvus；
     * 3) ES 仍保留基础关键词检索（不依赖增广文本）。
     */
    private CommoditySearchDocument buildSearchDocumentWithEnrichment(Commodity commodity) {
        String enriched = null;
        try {
            Map<String, Object> body = new HashMap<>();
            body.put("title", commodity.getTitle());
            body.put("description", commodity.getDescription());
            body.put("category", commodity.getCategory());
            body.put("conditionLevel", commodity.getConditionLevel());
            body.put("location", commodity.getLocation());
            body.put("addressSnapshotFull", commodity.getAddressSnapshotFull());
            Result result = aiClient.enrichCommodityForSearch(body);
            if (result != null && Boolean.TRUE.equals(result.getSuccess()) && result.getData() instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> data = (Map<String, Object>) result.getData();
                Object payload = data.get("enrichedKeywordPayload");
                if (payload != null && payload.toString().length() > 0) {
                    enriched = payload.toString().trim();
                }
            }
        } catch (Exception e) {
            log.debug("商品丰度增强跳过（将使用原标题与描述）: commodityId={}, error={}", commodity.getCommodityId(), e.getMessage());
        }
        try {
            StringBuilder vectorText = new StringBuilder();
            vectorText.append("标题: ").append(nullSafe(commodity.getTitle())).append('\n')
                    .append("描述: ").append(nullSafe(commodity.getDescription())).append('\n')
                    .append("类别: ").append(nullSafe(commodity.getCategory())).append('\n')
                    .append("成色: ").append(nullSafe(commodity.getConditionLevel())).append('\n')
                    .append("位置: ").append(nullSafe(commodity.getAddressSnapshotFull()));
            if (enriched != null && !enriched.isEmpty()) {
                vectorText.append('\n').append("增广文本: ").append(enriched);
            }
            Map<String, Object> vectorBody = new HashMap<>();
            vectorBody.put("id", commodity.getCommodityId());
            vectorBody.put("bizId", commodity.getCommodityId());
            vectorBody.put("content", vectorText.toString());
            aiClient.upsertCommodityVector(vectorBody);
        } catch (Exception e) {
            log.debug("写入商品 Milvus 向量失败（不影响主流程）: commodityId={}, error={}", commodity.getCommodityId(), e.getMessage());
        }
        return CommoditySearchDocument.fromCommodity(commodity);
    }

    private static String nullSafe(String text) {
        return text == null ? "" : text;
    }

    @Transactional(readOnly = true)
    public long rebuildIndex() {
        if (!isEnabled()) {
            return 0L;
        }

        recreateIndexIfNecessary();

        int batchSize = Math.max(properties.getSync().getPageSize(), 100);
        AtomicLong total = new AtomicLong();

        int page = 0;
        Page<Commodity> commodityPage;
        do {
            Pageable pageable = PageRequest.of(page, batchSize, Sort.by(Sort.Direction.ASC, "commodityId"));
            // 只重建 ON_SHELF 商品的索引，其他状态不应出现在搜索结果中
            commodityPage = commodityRepository.findByCommodityStatus(STATUS_ON_SHELF, pageable);
            if (commodityPage.isEmpty()) {
                log.info("重建索引：第 {} 批未查询到数据，提前结束", page);
                break;
            }

            List<CommoditySearchDocument> documents = commodityPage.getContent().stream()
                    .map(c -> buildSearchDocumentWithEnrichment(c))
                    .collect(Collectors.toList());
            
            // 详细日志：检查写入前的数据完整性
            if (log.isInfoEnabled()) {
                documents.stream().limit(3).forEach(doc -> {
                    log.info("准备写入ES文档：id={}, title='{}', titleNull={}, desc='{}', descNull={}, descLen={}, keywordPayload='{}', keywordLen={}, status={}, sellerVis={}, buyerVis={}",
                            doc.getCommodityId(),
                            doc.getTitle(),
                            doc.getTitle() == null,
                            doc.getDescription() != null ? (doc.getDescription().length() > 50 ? doc.getDescription().substring(0, 50) + "..." : doc.getDescription()) : "null",
                            doc.getDescription() == null,
                            doc.getDescription() == null ? 0 : doc.getDescription().length(),
                            doc.getKeywordPayload() != null ? (doc.getKeywordPayload().length() > 50 ? doc.getKeywordPayload().substring(0, 50) + "..." : doc.getKeywordPayload()) : "null",
                            doc.getKeywordPayload() == null ? 0 : doc.getKeywordPayload().length(),
                            doc.getCommodityStatus(),
                            doc.getBuyerVisibility());
                });
            }
            
            commoditySearchRepository.saveAll(documents);
            
            // 验证写入：使用原生查询只获取文档是否存在，避免日期转换问题
            if (!documents.isEmpty() && log.isDebugEnabled()) {
                try {
                    CommoditySearchDocument sampleDoc = documents.get(0);
                    // 使用原生查询，只检查文档是否存在，不反序列化完整文档
                    Query existsQuery = Query.of(q -> q.ids(ids -> ids.values(sampleDoc.getCommodityId())));
                    NativeQuery verifyQuery = NativeQuery.builder()
                            .withQuery(existsQuery)
                            .withSourceFilter(new org.springframework.data.elasticsearch.core.query.SourceFilter() {
                                @Override
                                public String[] getIncludes() {
                                    return new String[]{"commodityId"};  // 只获取 ID，避免日期转换
                                }
                                
                                @Override
                                public String[] getExcludes() {
                                    return new String[]{"*"};  // 排除所有其他字段
                                }
                            })
                            .build();
                    SearchHits<CommoditySearchDocument> verifyHits = elasticsearchOperations.search(verifyQuery, CommoditySearchDocument.class);
                    if (verifyHits != null && verifyHits.getTotalHits() > 0) {
                        log.debug("验证写入成功：文档已存在于 ES，id={}", sampleDoc.getCommodityId());
                    } else {
                        log.warn("警告：文档写入后无法在 ES 中找到，id={}", sampleDoc.getCommodityId());
                    }
                } catch (Exception e) {
                    // 验证失败不影响主流程，只记录警告
                    log.warn("验证写入时出错（不影响数据写入）: {}", e.getMessage());
                    if (log.isDebugEnabled()) {
                        log.debug("验证错误详情", e);
                    }
                }
            }
            
            total.addAndGet(documents.size());
            log.info("重建索引：批次={}，写入 {} 条，累计 {}", page, documents.size(), total.get());
            page++;
        } while (!commodityPage.isLast());

        // 刷新索引，确保数据立即可搜索
        try {
            var indexOperations = elasticsearchOperations.indexOps(CommoditySearchDocument.class);
            indexOperations.refresh();
            log.info("索引刷新完成");
        } catch (Exception e) {
            log.warn("索引刷新失败: {}", e.getMessage());
        }
        
        log.info("ElasticSearch 索引重建完成，共索引 {} 条 ON_SHELF 商品", total.get());
        return total.get();
    }

    private NativeQueryBuilder buildQueryBuilder(String keyword,
                                                 String location,
                                                 Double minPrice,
                                                 Double maxPrice,
                                                 String category,
                                                 Pageable pageable,
                                                 String userId) {
        Query query = Query.of(q -> q.bool(configureBoolQuery(keyword, location, minPrice, maxPrice, category, userId)));
        return NativeQuery.builder()
                .withQuery(query)
                .withPageable(pageable);
    }

    private BoolQuery configureBoolQuery(String keyword,
                                         String location,
                                         Double minPrice,
                                         Double maxPrice,
                                         String category,
                                         String userId) {
        return BoolQuery.of(bool -> {
            if (StringUtils.hasText(keyword)) {
                bool.must(m -> m.multiMatch(mm -> mm
                        .query(keyword)
                        .fields("title^4", "description^2", "keywordPayload", "addressSnapshotFull", "category")));
            } else {
                bool.must(m -> m.matchAll(ma -> ma));
            }

            bool.filter(f -> f.term(t -> t.field("commodityStatus").value(STATUS_ON_SHELF)));
            bool.filter(f -> f.term(t -> t.field("buyerVisibility").value(VISIBILITY_PUBLIC)));
            
            // 过滤掉库存为0的商品
            bool.filter(f -> f.range(r -> r.field("stock").gt(JsonData.of(0))));

            // 如果提供了userId，过滤掉自己的商品
            if (StringUtils.hasText(userId)) {
                bool.mustNot(mn -> mn.term(t -> t.field("sellerId").value(userId)));
            }

            if (StringUtils.hasText(category)) {
                bool.filter(f -> f.term(t -> t.field("category").value(category)));
            }
            if (StringUtils.hasText(location)) {
                bool.filter(f -> f.matchPhrase(mp -> mp.field("addressSnapshotFull").query(location)));
            }
            if (minPrice != null || maxPrice != null) {
                bool.filter(f -> f.range(r -> {
                    r.field("price");
                    if (minPrice != null) {
                        r.gte(JsonData.of(minPrice));
                    }
                    if (maxPrice != null) {
                        r.lte(JsonData.of(maxPrice));
                    }
                    return r;
                }));
            }
            return bool;
        });
    }

    private Map<String, List<String>> extractHighlights(SearchHits<CommoditySearchDocument> searchHits) {
        if (searchHits == null || CollectionUtils.isEmpty(searchHits.getSearchHits())) {
            return Collections.emptyMap();
        }
        Map<String, List<String>> result = new HashMap<>();
        for (SearchHit<CommoditySearchDocument> hit : searchHits) {
            Map<String, List<String>> highlightFields = hit.getHighlightFields();
            if (CollectionUtils.isEmpty(highlightFields)) {
                continue;
            }
            Collection<List<String>> fragments = highlightFields.values();
            List<String> merged = fragments.stream()
                    .filter(list -> list != null && !list.isEmpty())
                    .flatMap(List::stream)
                    .limit(5)
                    .collect(Collectors.toList());
            if (!merged.isEmpty()) {
                // 使用 hit.getId() 而不是 hit.getContent().getCommodityId()，避免反序列化问题
                result.put(hit.getId(), merged);
            }
        }
        return result;
    }

    /** 默认按相关度 _score 排序；外源指定 sortBy 时：price_asc/price_desc 按价格，latest 按发布时间，relevance 或未识别则按相关度。 */
    private Sort resolveSort(String sortBy) {
        if (!StringUtils.hasText(sortBy)) {
            return Sort.by(Sort.Order.desc("_score"));
        }
        return switch (sortBy) {
            case "price_asc" -> Sort.by(Sort.Order.asc("price"));
            case "price_desc" -> Sort.by(Sort.Order.desc("price"));
            case "latest" -> Sort.by(Sort.Order.desc("publishTime"));
            case "relevance" -> Sort.by(Sort.Order.desc("_score"));
            default -> Sort.by(Sort.Order.desc("_score"));
        };
    }

    private void recreateIndexIfNecessary() {
        try {
            var indexOperations = elasticsearchOperations.indexOps(CommoditySearchDocument.class);
            if (indexOperations.exists()) {
                // 重建索引时需要删除旧索引（因为 settings 中的 analyzer 依赖 IK 插件，不能动态修改）
                log.info("删除旧索引以重新创建...");
                indexOperations.delete();
                log.info("旧索引删除成功，等待索引完全删除...");
                
                // 等待索引完全删除（最多等待 5 秒）
                int retries = 0;
                while (indexOperations.exists() && retries < 10) {
                    Thread.sleep(500);
                    retries++;
                }
                if (indexOperations.exists()) {
                    throw new RuntimeException("索引删除超时，请手动删除索引后重试");
                }
                log.info("索引已完全删除");
            }
            // Spring Data Elasticsearch 5.x: createWithMapping() 会自动应用 settings 和 mapping
            indexOperations.createWithMapping();
            log.info("ElasticSearch 索引创建成功");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("重建索引被中断", e);
        } catch (Exception e) {
            log.error("重建 ElasticSearch 索引失败: {}", e.getMessage(), e);
            throw new RuntimeException("重建 ElasticSearch 索引失败，请确保 IK 分词插件已正确安装并重启 ES。如果问题持续，请手动删除索引: DELETE /commodities", e);
        }
    }
}

