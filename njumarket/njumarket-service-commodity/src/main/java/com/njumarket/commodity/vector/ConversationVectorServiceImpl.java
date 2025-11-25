package com.njumarket.commodity.vector;

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
 * 对话历史向量化服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ConversationVectorServiceImpl implements ConversationVectorService {
    
    private final EmbeddingModel embeddingModel;
    private final JdbcTemplate jdbcTemplate;
    
    private static final String VECTOR_TABLE = "nju_market.conversation_vectors";
    
    @Override
    @Async
    @Transactional
    public void storeConversationVector(String conversationId, String messageId, String userId, 
                                       String content, String role) {
        // 调用重载方法，商品ID列表为null（兼容旧代码）
        storeConversationVector(conversationId, messageId, userId, content, role, null);
    }
    
    @Override
    @Async
    @Transactional
    public void storeConversationVector(String conversationId, String messageId, String userId, 
                                       String content, String role, List<String> recommendedCommodityIds) {
        try {
            if (!StringUtils.hasText(conversationId) || !StringUtils.hasText(messageId) || 
                !StringUtils.hasText(userId) || !StringUtils.hasText(content)) {
                log.warn("参数不完整，跳过向量化: conversationId={}, messageId={}, userId={}", 
                    conversationId, messageId, userId);
                return;
            }
            
            // 生成向量 - LangChain4j API
            Embedding embedding = embeddingModel.embed(content).content();
            if (embedding == null || embedding.vector() == null || embedding.vector().length == 0) {
                log.error("向量生成失败: conversationId={}, messageId={}", conversationId, messageId);
                return;
            }
            // 提取向量数组（LangChain4j 返回 float[]）
            float[] embeddingArray = embedding.vector();
            if (embeddingArray == null || embeddingArray.length == 0) {
                log.error("向量数组为空: conversationId={}, messageId={}", conversationId, messageId);
                return;
            }
            
            // HNSW 索引最多支持 2000 维，如果向量维度超过 2000，需要截断
            int actualDimension = embeddingArray.length;
            int targetDimension = Math.min(actualDimension, 2000);
            
            // 截断向量数组到目标维度
            float[] truncatedArray = new float[targetDimension];
            System.arraycopy(embeddingArray, 0, truncatedArray, 0, targetDimension);
            
            // 构建元数据
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("conversationId", conversationId);
            metadata.put("messageId", messageId);
            metadata.put("userId", userId);
            metadata.put("role", role);
            metadata.put("createdAt", System.currentTimeMillis());
            
            // 如果是assistant消息且有推荐商品，将商品ID列表存入metadata
            if ("assistant".equals(role) && recommendedCommodityIds != null && !recommendedCommodityIds.isEmpty()) {
                metadata.put("recommendedCommodityIds", recommendedCommodityIds);
                log.debug("存储推荐商品ID列表: conversationId={}, messageId={}, count={}", 
                    conversationId, messageId, recommendedCommodityIds.size());
            }
            
            // 存储向量
            storeVector(conversationId, messageId, userId, truncatedArray, content, role, metadata);
            
            log.debug("对话向量存储成功: conversationId={}, messageId={}, role={}, hasCommodities={}", 
                conversationId, messageId, role, 
                recommendedCommodityIds != null && !recommendedCommodityIds.isEmpty());
        } catch (Exception e) {
            log.error("对话向量存储失败: conversationId={}, messageId={}, error={}", 
                conversationId, messageId, e.getMessage(), e);
        }
    }
    
    @Override
    public List<ConversationMessage> searchRelevantConversations(String query, String userId, int limit) {
        try {
            if (!StringUtils.hasText(query) || !StringUtils.hasText(userId)) {
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
            
            // 构建SQL查询：搜索该用户的相关对话历史
            // 注意：LIMIT 不能使用 ? 占位符，需要直接拼接
            // 注意：不要使用 ?::vector，因为 PGvector 对象已经包含了类型信息
            // ORDER BY 使用 similarity DESC 确保最相似的排在前面
            String sql = String.format(
                "SELECT conversation_id, message_id, content, role, metadata, 1 - (embedding <=> ?) as similarity " +
                "FROM %s WHERE user_id = ? ORDER BY similarity DESC LIMIT %d",
                VECTOR_TABLE, limit
            );
            
            // 使用 PreparedStatementCreator 和 PGvector 来处理向量类型
            List<Map<String, Object>> results = jdbcTemplate.query(
                new PreparedStatementCreator() {
                    @Override
                    public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
                        // 注册 pgvector 类型（如果还没有注册）
                        PGvector.addVectorType(con);
                        
                        // 设置 search_path 确保能找到 vector 类型
                        try (java.sql.Statement stmt = con.createStatement()) {
                            stmt.execute("SET search_path TO public, nju_market");
                        }
                        
                        PreparedStatement ps = con.prepareStatement(sql);
                        
                        // 直接使用 PGvector 对象绑定参数，使用 Types.OTHER 明确指定类型
                        // 第一个参数用于 SELECT 子句中的相似度计算
                        ps.setObject(1, queryVector, java.sql.Types.OTHER);
                        // 第二个参数是 user_id
                        ps.setString(2, userId);
                        // ORDER BY 使用 similarity DESC，不需要额外的向量参数
                        // LIMIT 已经在 SQL 中直接拼接，不需要设置参数
                        
                        return ps;
                    }
                },
                (rs, rowNum) -> {
                    Map<String, Object> row = new HashMap<>();
                    row.put("conversation_id", rs.getString("conversation_id"));
                    row.put("message_id", rs.getString("message_id"));
                    row.put("content", rs.getString("content"));
                    row.put("role", rs.getString("role"));
                    row.put("similarity", rs.getDouble("similarity"));
                    // 提取 metadata JSONB
                    try {
                        String metadataJson = rs.getString("metadata");
                        if (metadataJson != null) {
                            row.put("metadata", metadataJson);
                        }
                    } catch (Exception e) {
                        // metadata 可能为 null，忽略
                    }
                    return row;
                }
            );
            
            // 转换为 ConversationMessage 列表
            com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
            return results.stream()
                .map(row -> {
                    ConversationMessage msg = new ConversationMessage();
                    msg.setConversationId((String) row.get("conversation_id"));
                    msg.setMessageId((String) row.get("message_id"));
                    msg.setContent((String) row.get("content"));
                    msg.setRole((String) row.get("role"));
                    Object sim = row.get("similarity");
                    if (sim instanceof Number) {
                        msg.setSimilarity(((Number) sim).doubleValue());
                    }
                    // 设置创建时间
                    Object createdAtObj = row.get("created_at");
                    if (createdAtObj instanceof java.sql.Timestamp) {
                        msg.setCreatedAt(((java.sql.Timestamp) createdAtObj).toInstant());
                    }
                    // 从 metadata 中提取商品ID列表
                    try {
                        String metadataJson = (String) row.get("metadata");
                        if (metadataJson != null) {
                            Map<String, Object> metadata = objectMapper.readValue(metadataJson, Map.class);
                            Object commodityIdsObj = metadata.get("recommendedCommodityIds");
                            if (commodityIdsObj instanceof List) {
                                @SuppressWarnings("unchecked")
                                List<String> commodityIds = (List<String>) commodityIdsObj;
                                msg.setRecommendedCommodityIds(commodityIds);
                            }
                        }
                    } catch (Exception e) {
                        // metadata 解析失败，忽略（兼容旧数据）
                    }
                    return msg;
                })
                .collect(Collectors.toList());
                
        } catch (Exception e) {
            log.error("搜索相关对话失败: query={}, userId={}, error={}", query, userId, e.getMessage(), e);
            return Collections.emptyList();
        }
    }
    
    @Override
    @Transactional
    public void deleteConversationVectors(String conversationId) {
        try {
            String sql = "DELETE FROM " + VECTOR_TABLE + " WHERE conversation_id = ?";
            int deleted = jdbcTemplate.update(sql, conversationId);
            if (deleted > 0) {
                log.info("删除对话向量成功: conversationId={}, deleted={}", conversationId, deleted);
            }
        } catch (Exception e) {
            log.error("删除对话向量失败: conversationId={}, error={}", conversationId, e.getMessage(), e);
        }
    }
    
    @Override
    public List<ConversationMessage> getUserAIChatHistory(String userId, int limit) {
        try {
            if (!StringUtils.hasText(userId)) {
                return Collections.emptyList();
            }
            
            // 构建SQL查询：获取用户的所有AI聊天记录，按时间倒序
            // 注意：LIMIT 不能使用 ? 占位符，需要直接拼接
            String sql = String.format(
                "SELECT conversation_id, message_id, content, role, metadata, created_at " +
                "FROM %s WHERE user_id = ? " +
                "ORDER BY created_at DESC LIMIT %d",
                VECTOR_TABLE, limit
            );
            
            // 查询结果
            List<Map<String, Object>> results = jdbcTemplate.query(
                new PreparedStatementCreator() {
                    @Override
                    public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
                        // 设置 search_path 确保能找到表
                        try (java.sql.Statement stmt = con.createStatement()) {
                            stmt.execute("SET search_path TO public, nju_market");
                        }
                        
                        PreparedStatement ps = con.prepareStatement(sql);
                        ps.setString(1, userId);
                        return ps;
                    }
                },
                (rs, rowNum) -> {
                    Map<String, Object> row = new HashMap<>();
                    row.put("conversation_id", rs.getString("conversation_id"));
                    row.put("message_id", rs.getString("message_id"));
                    row.put("content", rs.getString("content"));
                    row.put("role", rs.getString("role"));
                    row.put("created_at", rs.getTimestamp("created_at"));
                    // 提取 metadata JSONB
                    try {
                        String metadataJson = rs.getString("metadata");
                        if (metadataJson != null) {
                            row.put("metadata", metadataJson);
                        }
                    } catch (Exception e) {
                        // metadata 可能为 null，忽略
                    }
                    return row;
                }
            );
            
            // 转换为 ConversationMessage 列表
            com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
            return results.stream()
                .map(row -> {
                    ConversationMessage msg = new ConversationMessage();
                    msg.setConversationId((String) row.get("conversation_id"));
                    msg.setMessageId((String) row.get("message_id"));
                    msg.setContent((String) row.get("content"));
                    msg.setRole((String) row.get("role"));
                    // 相似度字段不适用，设为null
                    msg.setSimilarity(null);
                    // 设置创建时间
                    Object createdAtObj = row.get("created_at");
                    if (createdAtObj instanceof java.sql.Timestamp) {
                        msg.setCreatedAt(((java.sql.Timestamp) createdAtObj).toInstant());
                    }
                    // 从 metadata 中提取商品ID列表
                    try {
                        String metadataJson = (String) row.get("metadata");
                        if (metadataJson != null) {
                            Map<String, Object> metadata = objectMapper.readValue(metadataJson, Map.class);
                            Object commodityIdsObj = metadata.get("recommendedCommodityIds");
                            if (commodityIdsObj instanceof List) {
                                @SuppressWarnings("unchecked")
                                List<String> commodityIds = (List<String>) commodityIdsObj;
                                msg.setRecommendedCommodityIds(commodityIds);
                            }
                        }
                    } catch (Exception e) {
                        // metadata 解析失败，忽略（兼容旧数据）
                    }
                    return msg;
                })
                .collect(Collectors.toList());
                
        } catch (Exception e) {
            log.error("获取用户AI聊天记录失败: userId={}, error={}", userId, e.getMessage(), e);
            return Collections.emptyList();
        }
    }
    
    @Override
    public List<ConversationVectorService.ChatInfo> getUserChatList(String userId, int limit) {
        try {
            if (!StringUtils.hasText(userId)) {
                return Collections.emptyList();
            }
            
            // 构建SQL查询：获取用户的所有chat，按最后消息时间倒序
            // 先获取所有conversation_id，然后分别查询每个chat的信息
            String conversationIdsSql = String.format(
                "SELECT DISTINCT conversation_id " +
                "FROM %s " +
                "WHERE user_id = ? " +
                "ORDER BY conversation_id",
                VECTOR_TABLE
            );
            
            // 先获取所有conversation_id
            List<String> conversationIds = jdbcTemplate.query(
                conversationIdsSql,
                (rs, rowNum) -> rs.getString("conversation_id"),
                userId
            );
            
            if (conversationIds.isEmpty()) {
                return Collections.emptyList();
            }
            
            // 限制数量
            if (conversationIds.size() > limit) {
                conversationIds = conversationIds.subList(0, limit);
            }
            
            // 为每个conversation_id查询详细信息
            List<ConversationVectorService.ChatInfo> chatInfos = new ArrayList<>();
            for (String convId : conversationIds) {
                // 查询最后一条消息
                String lastMessageSql = String.format(
                    "SELECT content, role, created_at " +
                    "FROM %s " +
                    "WHERE conversation_id = ? AND user_id = ? " +
                    "ORDER BY created_at DESC LIMIT 1",
                    VECTOR_TABLE
                );
                
                Map<String, Object> lastMessage = jdbcTemplate.queryForMap(lastMessageSql, convId, userId);
                
                // 查询第一条用户消息作为标题
                String titleSql = String.format(
                    "SELECT content " +
                    "FROM %s " +
                    "WHERE conversation_id = ? AND user_id = ? AND role = 'user' " +
                    "ORDER BY created_at ASC LIMIT 1",
                    VECTOR_TABLE
                );
                
                String title = null;
                try {
                    Map<String, Object> titleRow = jdbcTemplate.queryForMap(titleSql, convId, userId);
                    title = (String) titleRow.get("content");
                } catch (Exception e) {
                    // 如果没有用户消息，使用第一条消息
                    String firstMessageSql = String.format(
                        "SELECT content " +
                        "FROM %s " +
                        "WHERE conversation_id = ? AND user_id = ? " +
                        "ORDER BY created_at ASC LIMIT 1",
                        VECTOR_TABLE
                    );
                    try {
                        Map<String, Object> firstRow = jdbcTemplate.queryForMap(firstMessageSql, convId, userId);
                        title = (String) firstRow.get("content");
                    } catch (Exception e2) {
                        title = "新对话";
                    }
                }
                
                // 查询消息数量
                String countSql = String.format(
                    "SELECT COUNT(*) as cnt " +
                    "FROM %s " +
                    "WHERE conversation_id = ? AND user_id = ?",
                    VECTOR_TABLE
                );
                Integer messageCount = jdbcTemplate.queryForObject(countSql, Integer.class, convId, userId);
                
                // 构建ChatInfo
                ConversationVectorService.ChatInfo chatInfo = new ConversationVectorService.ChatInfo();
                chatInfo.setConversationId(convId);
                
                // 设置标题
                if (title != null && title.length() > 50) {
                    title = title.substring(0, 50) + "...";
                }
                chatInfo.setTitle(title != null ? title : "新对话");
                
                // 设置最后一条消息
                String lastMessageContent = (String) lastMessage.get("content");
                if (lastMessageContent != null && lastMessageContent.length() > 100) {
                    lastMessageContent = lastMessageContent.substring(0, 100) + "...";
                }
                chatInfo.setLastMessage(lastMessageContent);
                chatInfo.setLastMessageRole((String) lastMessage.get("role"));
                
                // 设置最后消息时间
                java.sql.Timestamp timestamp = (java.sql.Timestamp) lastMessage.get("created_at");
                if (timestamp != null) {
                    chatInfo.setLastMessageTime(timestamp.toInstant());
                }
                
                // 设置消息数量
                chatInfo.setMessageCount(messageCount != null ? messageCount : 0);
                
                chatInfos.add(chatInfo);
            }
            
            // 按最后消息时间倒序排序
            chatInfos.sort((a, b) -> {
                if (a.getLastMessageTime() == null && b.getLastMessageTime() == null) return 0;
                if (a.getLastMessageTime() == null) return 1;
                if (b.getLastMessageTime() == null) return -1;
                return b.getLastMessageTime().compareTo(a.getLastMessageTime());
            });
            
            return chatInfos;
            
            /* 原来的复杂SQL查询（有问题，改用上面的方式）
            List<Map<String, Object>> results = jdbcTemplate.query(
                new PreparedStatementCreator() {
                    @Override
                    public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
                        try (java.sql.Statement stmt = con.createStatement()) {
                            stmt.execute("SET search_path TO public, nju_market");
                        }
                        
                        PreparedStatement ps = con.prepareStatement(sql);
                        ps.setString(1, userId);
                        ps.setString(2, userId);
                        ps.setString(3, userId);
                        ps.setString(4, userId);
                        return ps;
                    }
                },
            */
                
        } catch (Exception e) {
            log.error("获取用户chat列表失败: userId={}, error={}", userId, e.getMessage(), e);
            return Collections.emptyList();
        }
    }
    
    @Override
    public List<ConversationMessage> getChatMessages(String conversationId, String userId, int limit) {
        try {
            if (!StringUtils.hasText(conversationId) || !StringUtils.hasText(userId)) {
                return Collections.emptyList();
            }
            
            // 构建SQL查询：获取指定chat的消息列表，按时间正序
            String sql = String.format(
                "SELECT conversation_id, message_id, content, role, metadata, created_at " +
                "FROM %s " +
                "WHERE conversation_id = ? AND user_id = ? " +
                "ORDER BY created_at ASC " +
                "LIMIT %d",
                VECTOR_TABLE, limit
            );
            
            // 查询结果
            List<Map<String, Object>> results = jdbcTemplate.query(
                new PreparedStatementCreator() {
                    @Override
                    public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
                        try (java.sql.Statement stmt = con.createStatement()) {
                            stmt.execute("SET search_path TO public, nju_market");
                        }
                        
                        PreparedStatement ps = con.prepareStatement(sql);
                        ps.setString(1, conversationId);
                        ps.setString(2, userId);
                        return ps;
                    }
                },
                (rs, rowNum) -> {
                    Map<String, Object> row = new HashMap<>();
                    row.put("conversation_id", rs.getString("conversation_id"));
                    row.put("message_id", rs.getString("message_id"));
                    row.put("content", rs.getString("content"));
                    row.put("role", rs.getString("role"));
                    row.put("created_at", rs.getTimestamp("created_at"));
                    // 提取 metadata JSONB
                    try {
                        String metadataJson = rs.getString("metadata");
                        if (metadataJson != null) {
                            row.put("metadata", metadataJson);
                        }
                    } catch (Exception e) {
                        // metadata 可能为 null，忽略
                    }
                    return row;
                }
            );
            
            // 转换为 ConversationMessage 列表
            com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
            return results.stream()
                .map(row -> {
                    ConversationMessage msg = new ConversationMessage();
                    msg.setConversationId((String) row.get("conversation_id"));
                    msg.setMessageId((String) row.get("message_id"));
                    msg.setContent((String) row.get("content"));
                    msg.setRole((String) row.get("role"));
                    msg.setSimilarity(null);
                    // 设置创建时间
                    Object createdAtObj = row.get("created_at");
                    if (createdAtObj instanceof java.sql.Timestamp) {
                        msg.setCreatedAt(((java.sql.Timestamp) createdAtObj).toInstant());
                    }
                    // 从 metadata 中提取商品ID列表
                    try {
                        String metadataJson = (String) row.get("metadata");
                        if (metadataJson != null) {
                            Map<String, Object> metadata = objectMapper.readValue(metadataJson, Map.class);
                            Object commodityIdsObj = metadata.get("recommendedCommodityIds");
                            if (commodityIdsObj instanceof List) {
                                @SuppressWarnings("unchecked")
                                List<String> commodityIds = (List<String>) commodityIdsObj;
                                msg.setRecommendedCommodityIds(commodityIds);
                            }
                        }
                    } catch (Exception e) {
                        // metadata 解析失败，忽略（兼容旧数据）
                    }
                    return msg;
                })
                .collect(Collectors.toList());
                
        } catch (Exception e) {
            log.error("获取chat消息列表失败: conversationId={}, userId={}, error={}", 
                conversationId, userId, e.getMessage(), e);
            return Collections.emptyList();
        }
    }
    
    /**
     * 存储向量到数据库
     * 使用 pgvector 官方 JDBC wrapper
     */
    private void storeVector(String conversationId, String messageId, String userId,
                            float[] embedding, String content, String role,
                            Map<String, Object> metadata) {
        // 使用 pgvector 官方 JDBC wrapper 创建 PGvector 对象
        PGvector pgVector = new PGvector(embedding);
        
        String metadataJson = new com.fasterxml.jackson.databind.ObjectMapper()
            .valueToTree(metadata).toString();
        
        // 使用单行 SQL 避免文本块可能的问题
        String sql = String.format(
            "INSERT INTO %s (conversation_id, message_id, user_id, embedding, content, role, metadata, created_at) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?::jsonb, CURRENT_TIMESTAMP)",
            VECTOR_TABLE
        );
        
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
                ps.setString(1, conversationId);
                ps.setString(2, messageId);
                ps.setString(3, userId);
                // 直接使用 PGvector 对象绑定参数，使用 Types.OTHER 明确指定类型
                ps.setObject(4, pgVector, java.sql.Types.OTHER);
                ps.setString(5, content);
                ps.setString(6, role);
                ps.setString(7, metadataJson);
                return ps;
            }
        });
    }
}

