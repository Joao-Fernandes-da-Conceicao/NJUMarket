package com.njumarket.commodity.vector;

import com.njumarket.commodity.entity.Commodity;
import com.njumarket.commodity.repository.CommodityRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.pgvector.PGvector;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.model.embedding.EmbeddingModel;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * AI搜索服务
 * 使用向量相似度搜索实现语义搜索
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AISearchService {
    
    private final EmbeddingModel embeddingModel;
    private final CommodityRepository commodityRepository;
    private final JdbcTemplate jdbcTemplate;
    
    private static final String VECTOR_TABLE = "nju_market.commodity_vectors";
    private static final int DEFAULT_LIMIT = 20;
    private static final double SIMILARITY_THRESHOLD = 0.7; // 相似度阈值
    
    /**
     * AI语义搜索（不过滤用户自己的商品）
     * @param query 用户查询文本
     * @param location 位置偏好（可选）
     * @param limit 返回数量限制
     * @return 商品列表（按相似度排序）
     */
    public List<Commodity> search(String query, String location, Integer limit) {
        return search(query, location, limit, null);
    }
    
    /**
     * AI语义搜索
     * @param query 用户查询文本
     * @param location 位置偏好（可选）
     * @param limit 返回数量限制
     * @param userId 当前用户ID（可选，如果提供则过滤掉自己的商品）
     * @return 商品列表（按相似度排序）
     */
    public List<Commodity> search(String query, String location, Integer limit, String userId) {
        try {
            if (!StringUtils.hasText(query)) {
                return Collections.emptyList();
            }
            
            // 生成查询向量 - LangChain4j API
            Embedding embedding = embeddingModel.embed(query).content();
            if (embedding == null || embedding.vector() == null || embedding.vector().length == 0) {
                log.warn("查询向量生成失败: query={}", query);
                return Collections.emptyList();
            }
            // 提取向量数组（LangChain4j 返回 float[]）
            float[] embeddingArray = embedding.vector();
            if (embeddingArray == null || embeddingArray.length == 0) {
                log.warn("查询向量数组为空: query={}", query);
                return Collections.emptyList();
            }
            
            // HNSW 索引最多支持 2000 维，如果向量维度超过 2000，需要截断
            int actualDimension = embeddingArray.length;
            int targetDimension = Math.min(actualDimension, 2000);
            
            // 截断向量数组到目标维度
            float[] truncatedArray = new float[targetDimension];
            System.arraycopy(embeddingArray, 0, truncatedArray, 0, targetDimension);
            
            // 使用 pgvector 官方 JDBC wrapper 创建 PGvector 对象
            PGvector queryVector = new PGvector(truncatedArray);
            
            // 构建SQL查询：使用余弦相似度搜索
            // 注意：LIMIT 不能使用 ? 占位符，需要直接拼接（limit 是整数，不会有 SQL 注入风险）
            // 注意：不要使用 ?::vector，因为 PGvector 对象已经包含了类型信息，PostgreSQL 可以自动识别
            // ORDER BY 使用 similarity DESC 确保最相似的排在前面
            int searchLimit = limit != null && limit > 0 ? limit : DEFAULT_LIMIT;
            
            // 构建WHERE条件：过滤掉自己的商品和库存为0的商品
            StringBuilder whereClause = new StringBuilder();
            whereClause.append("WHERE c.commodity_status = 'ON_SHELF' ");
            whereClause.append("AND c.seller_visibility = 'PUBLIC' ");
            whereClause.append("AND c.buyer_visibility = 'PUBLIC' ");
            whereClause.append("AND c.stock > 0 ");
            if (StringUtils.hasText(userId)) {
                whereClause.append("AND c.seller_id != ? ");
            }
            
            String sql = String.format(
                "SELECT cv.commodity_id, 1 - (cv.embedding <=> ?) as similarity " +
                "FROM %s cv " +
                "INNER JOIN nju_market.commodities c ON cv.commodity_id = c.commodity_id " +
                "%s " +
                "ORDER BY similarity DESC " +
                "LIMIT %d",
                VECTOR_TABLE, whereClause.toString(), searchLimit
            );
            
            log.debug("执行向量搜索 SQL: {}, vector维度={}, limit={}", sql, targetDimension, searchLimit);
            
            // 使用 PreparedStatementCreator 和 PGvector 来处理向量类型
            List<Map<String, Object>> results;
            try {
                results = jdbcTemplate.query(
                    new PreparedStatementCreator() {
                        @Override
                        public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
                            // 注册 pgvector 类型（如果还没有注册）
                            PGvector.addVectorType(con);
                            
                            // 设置 search_path 确保能找到 vector 类型
                            // vector 类型通常在 public schema 中
                            try (java.sql.Statement stmt = con.createStatement()) {
                                stmt.execute("SET search_path TO public, nju_market");
                            }
                            
                            PreparedStatement ps = con.prepareStatement(sql);
                            
                            // 设置参数：第一个是查询向量，第二个（如果存在）是userId
                            int paramIndex = 1;
                            ps.setObject(paramIndex++, queryVector, java.sql.Types.OTHER);
                            if (StringUtils.hasText(userId)) {
                                ps.setString(paramIndex++, userId);
                            }
                            // LIMIT 已经在 SQL 中直接拼接，不需要设置参数
                            
                            log.debug("PreparedStatement 参数设置完成: vector维度={}, queryVector类型={}, queryVector值长度={}", 
                                targetDimension, queryVector.getClass().getName(), 
                                queryVector.getValue() != null ? queryVector.getValue().length() : 0);
                            
                            return ps;
                        }
                    },
                    (rs, rowNum) -> {
                        Map<String, Object> row = new HashMap<>();
                        row.put("commodity_id", rs.getString("commodity_id"));
                        row.put("similarity", rs.getDouble("similarity"));
                        return row;
                    }
                );
            } catch (org.springframework.jdbc.BadSqlGrammarException e) {
                // 记录详细的 SQL 错误信息
                log.error("SQL 语法错误: sql={}, vector维度={}, error={}", 
                    sql, targetDimension, e.getMessage(), e);
                if (e.getSQLException() != null) {
                    log.error("SQLException 详情: SQLState={}, ErrorCode={}, Message={}", 
                        e.getSQLException().getSQLState(),
                        e.getSQLException().getErrorCode(),
                        e.getSQLException().getMessage());
                }
                throw e;
            }
            
            if (results.isEmpty()) {
                return Collections.emptyList();
            }
            
            // 提取商品ID列表
            List<String> commodityIds = results.stream()
                .map(row -> (String) row.get("commodity_id"))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
            
            if (commodityIds.isEmpty()) {
                return Collections.emptyList();
            }
            
            // 批量查询商品详情
            List<Commodity> commodities = commodityRepository.findAllById(commodityIds);
            
            // 如果有位置偏好，进行位置过滤和排序
            if (StringUtils.hasText(location)) {
                commodities = filterAndSortByLocation(commodityIds, commodities, location);
            } else {
                // 保持向量搜索的排序
                Map<String, Commodity> commodityMap = commodities.stream()
                    .collect(Collectors.toMap(Commodity::getCommodityId, c -> c));
                commodities = commodityIds.stream()
                    .map(commodityMap::get)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
            }
            
            log.info("AI搜索完成: query={}, location={}, userId={}, found={}", query, location, userId, commodities.size());
            return commodities;
            
        } catch (org.springframework.jdbc.BadSqlGrammarException e) {
            // SQL 语法错误已经在上面处理，这里只记录简要信息
            log.error("AI搜索失败（SQL语法错误）: query={}, error={}", query, e.getMessage(), e);
            return Collections.emptyList();
        } catch (Exception e) {
            log.error("AI搜索失败: query={}, error={}", query, e.getMessage(), e);
            return Collections.emptyList();
        }
    }
    
    /**
     * 根据位置过滤和排序商品
     */
    private List<Commodity> filterAndSortByLocation(
            List<String> originalOrder, 
            List<Commodity> commodities, 
            String location) {
        
        // 将商品按位置相关性分组
        List<Commodity> locationMatch = new ArrayList<>();
        List<Commodity> other = new ArrayList<>();
        
        String locationLower = location.toLowerCase();
        
        for (Commodity commodity : commodities) {
            String fullAddress = commodity.getAddressSnapshotFull();
            if (StringUtils.hasText(fullAddress) && 
                fullAddress.toLowerCase().contains(locationLower)) {
                locationMatch.add(commodity);
            } else {
                other.add(commodity);
            }
        }
        
        // 保持原始顺序，但位置匹配的在前
        Map<String, Commodity> commodityMap = commodities.stream()
            .collect(Collectors.toMap(Commodity::getCommodityId, c -> c));
        
        List<Commodity> result = new ArrayList<>();
        
        // 先添加位置匹配的商品（保持原始顺序）
        for (String id : originalOrder) {
            Commodity c = commodityMap.get(id);
            if (c != null && locationMatch.contains(c) && !result.contains(c)) {
                result.add(c);
            }
        }
        
        // 再添加其他商品（保持原始顺序）
        for (String id : originalOrder) {
            Commodity c = commodityMap.get(id);
            if (c != null && !result.contains(c)) {
                result.add(c);
            }
        }
        
        return result;
    }
}

