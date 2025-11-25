package com.njumarket.commodity.vector;

import com.njumarket.commodity.client.AuthClient;
import com.njumarket.commodity.client.OrderClient;
import com.njumarket.njumarket.dto.Result;
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
import java.util.stream.Collectors;

/**
 * 用户画像向量化服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserProfileVectorServiceImpl implements UserProfileVectorService {
    
    private final EmbeddingModel embeddingModel;
    private final JdbcTemplate jdbcTemplate;
    private final AuthClient authClient;
    private final OrderClient orderClient;
    
    private static final String VECTOR_TABLE = "nju_market.user_profile_vectors";
    
    @Override
    @Async
    @Transactional
    public void generateAndStoreUserProfileVector(String userId) {
        try {
            if (!StringUtils.hasText(userId)) {
                log.warn("用户ID为空，跳过向量化");
                return;
            }
            
            // 构建用户画像文本内容
            String content = buildUserProfileContent(userId);
            if (!StringUtils.hasText(content)) {
                log.warn("用户画像内容为空，跳过向量化: userId={}", userId);
                return;
            }
            
            // 生成向量 - LangChain4j API
            Embedding embedding = embeddingModel.embed(content).content();
            if (embedding == null || embedding.vector() == null || embedding.vector().length == 0) {
                log.error("向量生成失败: userId={}", userId);
                return;
            }
            // 提取向量数组（LangChain4j 返回 float[]）
            float[] embeddingArray = embedding.vector();
            if (embeddingArray == null || embeddingArray.length == 0) {
                log.error("向量数组为空: userId={}", userId);
                return;
            }
            
            // HNSW 索引最多支持 2000 维，如果向量维度超过 2000，需要截断
            int actualDimension = embeddingArray.length;
            int targetDimension = Math.min(actualDimension, 2000);
            
            // 截断向量数组到目标维度
            float[] truncatedArray = new float[targetDimension];
            System.arraycopy(embeddingArray, 0, truncatedArray, 0, targetDimension);
            
            // 构建元数据
            Map<String, Object> metadata = buildUserMetadata(userId);
            
            // 存储向量到数据库
            storeVector(userId, truncatedArray, content, metadata);
            
            log.info("用户画像向量化成功: userId={}", userId);
        } catch (Exception e) {
            log.error("用户画像向量化失败: userId={}, error={}", userId, e.getMessage(), e);
        }
    }
    
    @Override
    @Async
    @Transactional
    public void updateUserProfileVector(String userId) {
        generateAndStoreUserProfileVector(userId);
    }
    
    @Override
    @Transactional
    public void deleteUserProfileVector(String userId) {
        try {
            String sql = "DELETE FROM " + VECTOR_TABLE + " WHERE user_id = ?";
            int deleted = jdbcTemplate.update(sql, userId);
            if (deleted > 0) {
                log.info("删除用户画像向量成功: userId={}", userId);
            }
        } catch (Exception e) {
            log.error("删除用户画像向量失败: userId={}, error={}", userId, e.getMessage(), e);
        }
    }
    
    @Override
    public List<Double> getUserProfileVector(String userId) {
        try {
            String sql = "SELECT embedding FROM " + VECTOR_TABLE + " WHERE user_id = ?";
            String vectorStr = jdbcTemplate.queryForObject(sql, String.class, userId);
            if (vectorStr == null) {
                return null;
            }
            
            // 解析向量字符串
            return Arrays.stream(vectorStr.replaceAll("[\\[\\]]", "").split(","))
                .map(String::trim)
                .map(Double::parseDouble)
                .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("获取用户画像向量失败: userId={}, error={}", userId, e.getMessage());
            return null;
        }
    }
    
    /**
     * 构建用户画像文本内容
     */
    private String buildUserProfileContent(String userId) {
        StringBuilder content = new StringBuilder();
        
        try {
            // 获取用户基本信息
            Result userResult = authClient.getUserById(userId);
            if (userResult.getSuccess() && userResult.getData() != null) {
                @SuppressWarnings("unchecked")
                Map<String, Object> user = (Map<String, Object>) userResult.getData();
                String nickname = (String) user.get("nickname");
                if (StringUtils.hasText(nickname)) {
                    content.append("用户：").append(nickname).append(" ");
                }
            }
            
            // 获取用户购买历史（简化版，实际可以从Order Service获取）
            // 这里只添加占位符，实际实现需要调用Order Service
            
            // 获取用户浏览历史（可以从商品服务获取）
            // 这里只添加占位符
            
            return content.toString().trim();
        } catch (Exception e) {
            log.error("构建用户画像内容失败: userId={}, error={}", userId, e.getMessage());
            return "";
        }
    }
    
    /**
     * 构建用户元数据
     */
    private Map<String, Object> buildUserMetadata(String userId) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("userId", userId);
        metadata.put("updatedAt", System.currentTimeMillis());
        return metadata;
    }
    
    /**
     * 存储向量到数据库
     * 使用 pgvector 官方 JDBC wrapper
     */
    private void storeVector(String userId, float[] embedding, String content, Map<String, Object> metadata) {
        // 使用 pgvector 官方 JDBC wrapper 创建 PGvector 对象
        PGvector pgVector = new PGvector(embedding);
        
        String metadataJson = new com.fasterxml.jackson.databind.ObjectMapper()
            .valueToTree(metadata).toString();
        
        // 使用单行 SQL 避免文本块可能的问题
        String sql = String.format(
            "INSERT INTO %s (user_id, embedding, content, metadata, created_at, updated_at) " +
            "VALUES (?, ?, ?, ?::jsonb, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP) " +
            "ON CONFLICT (user_id) DO UPDATE SET " +
            "embedding = EXCLUDED.embedding, " +
            "content = EXCLUDED.content, " +
            "metadata = EXCLUDED.metadata, " +
            "updated_at = CURRENT_TIMESTAMP",
            VECTOR_TABLE
        );
        
        // 使用 PreparedStatementCreator 和 PGvector 来处理 vector 类型
        jdbcTemplate.update(new PreparedStatementCreator() {
            @Override
            public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
                // 注册 pgvector 类型（如果还没有注册）
                PGvector.addVectorType(con);
                
                PreparedStatement ps = con.prepareStatement(sql);
                ps.setString(1, userId);
                ps.setObject(2, pgVector);  // 使用 PGvector 对象
                ps.setString(3, content);
                ps.setString(4, metadataJson);
                return ps;
            }
        });
    }
}

