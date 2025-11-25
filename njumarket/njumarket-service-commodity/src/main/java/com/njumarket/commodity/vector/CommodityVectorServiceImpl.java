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
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.*;

/**
 * 商品向量化服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CommodityVectorServiceImpl implements CommodityVectorService {
    
    private final EmbeddingModel embeddingModel;
    private final CommodityRepository commodityRepository;
    private final JdbcTemplate jdbcTemplate;
    
    private static final String VECTOR_TABLE = "nju_market.commodity_vectors";
    private static final int EMBEDDING_DIMENSION = 2000; // HNSW 索引最多支持 2000 维，将 2560 维向量截断到 2000 维
    
    @Override
    @Async
    @Transactional
    public void generateAndStoreVector(Commodity commodity) {
        try {
            if (commodity == null || !StringUtils.hasText(commodity.getCommodityId())) {
                log.warn("商品为空或商品ID为空，跳过向量化");
                return;
            }
            
            // 构建商品文本内容
            String content = buildCommodityContent(commodity);
            if (!StringUtils.hasText(content)) {
                log.warn("商品内容为空，跳过向量化: commodityId={}", commodity.getCommodityId());
                return;
            }
            
            // 生成向量 - LangChain4j API
            Embedding embedding;
            try {
                embedding = embeddingModel.embed(content).content();
            } catch (Exception e) {
                // 处理 API 错误（如 404、401 等）
                if (e.getMessage() != null && e.getMessage().contains("404")) {
                    log.error("向量生成失败（404）: commodityId={}, 错误={}. 可能原因：1) base-url 配置错误 2) 模型名称不正确 3) API endpoint 路径不匹配", 
                        commodity.getCommodityId(), e.getMessage(), e);
                } else {
                    log.error("向量生成失败: commodityId={}, 错误={}", commodity.getCommodityId(), e.getMessage(), e);
                }
                return;
            }
            
            if (embedding == null || embedding.vector() == null || embedding.vector().length == 0) {
                log.error("向量生成失败: embedding 为空, commodityId={}", commodity.getCommodityId());
                return;
            }
            // 提取向量数组（LangChain4j 返回 float[]）
            float[] embeddingArray = embedding.vector();
            if (embeddingArray == null || embeddingArray.length == 0) {
                log.error("向量数组为空: commodityId={}", commodity.getCommodityId());
                return;
            }
            
            // HNSW 索引最多支持 2000 维，如果向量维度超过 2000，需要截断
            int actualDimension = embeddingArray.length;
            int targetDimension = Math.min(actualDimension, EMBEDDING_DIMENSION);
            
            if (actualDimension > EMBEDDING_DIMENSION) {
                log.warn("向量维度 {} 超过 HNSW 索引限制 {}，将截断到 {} 维: commodityId={}", 
                    actualDimension, EMBEDDING_DIMENSION, targetDimension, commodity.getCommodityId());
            }
            
            // 截断向量数组到目标维度
            float[] truncatedArray = new float[targetDimension];
            System.arraycopy(embeddingArray, 0, truncatedArray, 0, targetDimension);
            
            // 构建元数据
            Map<String, Object> metadata = buildMetadata(commodity);
            
            // 存储向量到数据库
            storeVector(commodity.getCommodityId(), truncatedArray, content, metadata);
            
            log.info("商品向量化成功: commodityId={}", commodity.getCommodityId());
        } catch (Exception e) {
            log.error("商品向量化失败: commodityId={}, error={}", 
                commodity != null ? commodity.getCommodityId() : "null", e.getMessage(), e);
        }
    }
    
    @Override
    @Async
    @Transactional
    public void updateVector(Commodity commodity) {
        // 更新向量与生成向量逻辑相同
        generateAndStoreVector(commodity);
    }
    
    @Override
    @Transactional
    public void deleteVector(String commodityId) {
        try {
            String sql = "DELETE FROM " + VECTOR_TABLE + " WHERE commodity_id = ?";
            int deleted = jdbcTemplate.update(sql, commodityId);
            if (deleted > 0) {
                log.info("删除商品向量成功: commodityId={}", commodityId);
            }
        } catch (Exception e) {
            log.error("删除商品向量失败: commodityId={}, error={}", commodityId, e.getMessage(), e);
        }
    }
    
    @Override
    // 注意：不使用 @Transactional，让每个 generateAndStoreVector 使用自己的事务
    // 这样可以避免某个商品处理失败导致整个批次的事务失败
    public void batchGenerateVectors(int batchSize) {
        log.info("开始批量生成商品向量，批次大小: {}", batchSize);
        
        int offset = 0;
        int totalProcessed = 0;
        int totalFailed = 0;
        
        while (true) {
            List<Commodity> commodities = commodityRepository.findAll(
                org.springframework.data.domain.PageRequest.of(offset, batchSize)
            ).getContent();
            
            if (commodities.isEmpty()) {
                break;
            }
            
            for (Commodity commodity : commodities) {
                try {
                    // generateAndStoreVector 有自己的 @Transactional，每个商品使用独立事务
                    generateAndStoreVector(commodity);
                    totalProcessed++;
                } catch (Exception e) {
                    totalFailed++;
                    log.error("批量向量化失败: commodityId={}, error={}", 
                        commodity.getCommodityId(), e.getMessage(), e);
                }
            }
            
            offset++;
            log.info("已处理 {} 个商品（成功: {}, 失败: {}）", 
                totalProcessed + totalFailed, totalProcessed, totalFailed);
        }
        
        log.info("批量生成商品向量完成，共处理 {} 个商品（成功: {}, 失败: {}）", 
            totalProcessed + totalFailed, totalProcessed, totalFailed);
    }
    
    /**
     * 构建商品文本内容（用于向量化）
     */
    private String buildCommodityContent(Commodity commodity) {
        StringBuilder content = new StringBuilder();
        
        // 标题（最重要）
        if (StringUtils.hasText(commodity.getTitle())) {
            content.append(commodity.getTitle()).append(" ");
        }
        
        // 描述
        if (StringUtils.hasText(commodity.getDescription())) {
            content.append(commodity.getDescription()).append(" ");
        }
        
        // 分类
        if (StringUtils.hasText(commodity.getCategory())) {
            content.append("分类：").append(commodity.getCategory()).append(" ");
        }
        
        // 成色
        if (StringUtils.hasText(commodity.getConditionLevel())) {
            content.append("成色：").append(commodity.getConditionLevel()).append(" ");
        }
        
        // 地址信息
        if (StringUtils.hasText(commodity.getAddressSnapshotFull())) {
            content.append("位置：").append(commodity.getAddressSnapshotFull()).append(" ");
        } else if (StringUtils.hasText(commodity.getLocation())) {
            content.append("位置：").append(commodity.getLocation()).append(" ");
        }
        
        return content.toString().trim();
    }
    
    /**
     * 构建商品元数据
     */
    private Map<String, Object> buildMetadata(Commodity commodity) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("commodityId", commodity.getCommodityId());
        metadata.put("sellerId", commodity.getSellerId());
        metadata.put("title", commodity.getTitle());
        metadata.put("category", commodity.getCategory());
        metadata.put("price", commodity.getPrice());
        metadata.put("conditionLevel", commodity.getConditionLevel());
        metadata.put("commodityStatus", commodity.getCommodityStatus());
        if (StringUtils.hasText(commodity.getAddressSnapshotFull())) {
            metadata.put("location", commodity.getAddressSnapshotFull());
        }
        return metadata;
    }
    
    /**
     * 存储向量到数据库
     * 使用 pgvector 官方 JDBC wrapper
     */
    private void storeVector(String commodityId, float[] embedding, String content, Map<String, Object> metadata) {
        // 使用 pgvector 官方 JDBC wrapper 创建 PGvector 对象
        PGvector pgVector = new PGvector(embedding);
        
        log.info("存储向量: commodityId={}, 向量维度={}", commodityId, embedding.length);
        
        // 将元数据转换为 JSONB
        String metadataJson = new com.fasterxml.jackson.databind.ObjectMapper()
            .valueToTree(metadata).toString();
        
        // 使用 UPSERT 语法（PostgreSQL 9.5+）
        // 使用单行 SQL 避免文本块可能的问题
        String sql = String.format(
            "INSERT INTO %s (commodity_id, embedding, content, metadata, created_at, updated_at) " +
            "VALUES (?, ?, ?, ?::jsonb, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP) " +
            "ON CONFLICT (commodity_id) DO UPDATE SET " +
            "embedding = EXCLUDED.embedding, " +
            "content = EXCLUDED.content, " +
            "metadata = EXCLUDED.metadata, " +
            "updated_at = CURRENT_TIMESTAMP",
            VECTOR_TABLE
        );
        
        try {
        // 使用 PreparedStatementCreator 和 PGvector 来处理 vector 类型
        jdbcTemplate.update(new PreparedStatementCreator() {
            @Override
            public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
                // 注册 pgvector 类型（如果还没有注册）
                PGvector.addVectorType(con);
                
                // 设置 search_path 确保能找到 vector 类型
                try (java.sql.Statement stmt = con.createStatement()) {
                    stmt.execute("SET search_path TO public, nju_market");
                }
                
                PreparedStatement ps = con.prepareStatement(sql);
                ps.setString(1, commodityId);
                // 直接使用 PGvector 对象绑定参数，使用 Types.OTHER 明确指定类型
                ps.setObject(2, pgVector, java.sql.Types.OTHER);
                ps.setString(3, content);
                ps.setString(4, metadataJson);
                return ps;
            }
        });
            
            log.debug("存储向量成功: commodityId={}", commodityId);
        } catch (org.springframework.jdbc.BadSqlGrammarException e) {
            // 特殊处理 SQL 语法错误
            log.error("SQL 语法错误: commodityId={}, sql={}, vector维度={}, error={}", 
                commodityId, sql, embedding.length, e.getMessage(), e);
            // 尝试记录更详细的错误信息
            if (e.getSQLException() != null) {
                log.error("SQLException: SQLState={}, ErrorCode={}, Message={}", 
                    e.getSQLException().getSQLState(),
                    e.getSQLException().getErrorCode(),
                    e.getSQLException().getMessage());
            }
            throw e;
        } catch (Exception e) {
            log.error("存储向量失败: commodityId={}, sql={}, vector维度={}, error={}", 
                commodityId, sql, embedding.length, e.getMessage(), e);
            // 记录完整的异常堆栈
            if (e.getCause() != null) {
                log.error("异常原因: {}", e.getCause().getMessage(), e.getCause());
            }
            throw e;
        }
    }
}

