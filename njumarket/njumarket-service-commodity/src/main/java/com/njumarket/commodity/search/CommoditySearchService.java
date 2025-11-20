package com.njumarket.commodity.search;

import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.json.JsonData;
import com.njumarket.commodity.entity.Commodity;
import com.njumarket.commodity.repository.CommodityRepository;
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
 * 商品搜索服务
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
        if (!isEnabled()) {
            return Optional.empty();
        }

        try {
            Pageable pageable = PageRequest.of(Math.max(page - 1, 0), Math.max(size, 1), resolveSort(sortBy));
            NativeQueryBuilder queryBuilder = buildQueryBuilder(keyword, location, minPrice, maxPrice, category, pageable);
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
                                    "commodityStatus", "sellerVisibility", "buyerVisibility", "category", 
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
                            log.debug("样本文档：id={}, status={}, sellerVis={}, buyerVis={}, title='{}'",
                                    doc.getCommodityId(), doc.getCommodityStatus(),
                                    doc.getSellerVisibility(), doc.getBuyerVisibility(), doc.getTitle());
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

    public void syncCommodity(Commodity commodity) {
        if (!isEnabled() || commodity == null) {
            return;
        }
        try {
            if (log.isDebugEnabled()) {
                log.debug("准备同步商品到 ElasticSearch：id={}, title='{}', status={}, sellerVisibility={}, buyerVisibility={}, descPresent={}",
                        commodity.getCommodityId(),
                        commodity.getTitle(),
                        commodity.getCommodityStatus(),
                        commodity.getSellerVisibility(),
                        commodity.getBuyerVisibility(),
                        StringUtils.hasText(commodity.getDescription()));
            }
            commoditySearchRepository.save(CommoditySearchDocument.fromCommodity(commodity));
            if (log.isDebugEnabled()) {
                log.debug("商品同步完成：id={}", commodity.getCommodityId());
            }
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
            commodityPage = commodityRepository.findAll(pageable);
            if (commodityPage.isEmpty()) {
                log.info("重建索引：第 {} 批未查询到数据，提前结束", page);
                break;
            }

            List<CommoditySearchDocument> documents = commodityPage.getContent().stream()
                    .map(CommoditySearchDocument::fromCommodity)
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
                            doc.getSellerVisibility(),
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
        
        log.info("ElasticSearch 索引重建完成，共索引 {} 条商品", total.get());
        return total.get();
    }

    private NativeQueryBuilder buildQueryBuilder(String keyword,
                                                 String location,
                                                 Double minPrice,
                                                 Double maxPrice,
                                                 String category,
                                                 Pageable pageable) {
        Query query = Query.of(q -> q.bool(configureBoolQuery(keyword, location, minPrice, maxPrice, category)));
        return NativeQuery.builder()
                .withQuery(query)
                .withPageable(pageable);
    }

    private BoolQuery configureBoolQuery(String keyword,
                                         String location,
                                         Double minPrice,
                                         Double maxPrice,
                                         String category) {
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
            bool.filter(f -> f.term(t -> t.field("sellerVisibility").value(VISIBILITY_PUBLIC)));

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

    private Sort resolveSort(String sortBy) {
        if (!StringUtils.hasText(sortBy)) {
            return Sort.by(Sort.Order.desc("publishTime"));
        }
        return switch (sortBy) {
            case "price_asc" -> Sort.by(Sort.Order.asc("price"));
            case "price_desc" -> Sort.by(Sort.Order.desc("price"));
            case "latest" -> Sort.by(Sort.Order.desc("publishTime"));
            default -> Sort.by(Sort.Order.desc("publishTime"));
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

