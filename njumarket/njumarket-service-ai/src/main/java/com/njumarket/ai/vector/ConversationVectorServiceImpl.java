package com.njumarket.ai.vector;

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
            
            Embedding embedding = embeddingModel.embed(content).content();
            if (embedding == null || embedding.vector() == null || embedding.vector().length == 0) {
                log.error("向量生成失败: conversationId={}, messageId={}", conversationId, messageId);
                return;
            }
            float[] embeddingArray = embedding.vector();
            if (embeddingArray == null || embeddingArray.length == 0) {
                log.error("向量数组为空: conversationId={}, messageId={}", conversationId, messageId);
                return;
            }
            
            int actualDimension = embeddingArray.length;
            int targetDimension = Math.min(actualDimension, 2000);
            
            float[] truncatedArray = new float[targetDimension];
            System.arraycopy(embeddingArray, 0, truncatedArray, 0, targetDimension);
            
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("conversationId", conversationId);
            metadata.put("messageId", messageId);
            metadata.put("userId", userId);
            metadata.put("role", role);
            metadata.put("createdAt", System.currentTimeMillis());
            
            if ("assistant".equals(role) && recommendedCommodityIds != null && !recommendedCommodityIds.isEmpty()) {
                metadata.put("recommendedCommodityIds", recommendedCommodityIds);
                log.debug("存储推荐商品ID列表: conversationId={}, messageId={}, count={}", 
                    conversationId, messageId, recommendedCommodityIds.size());
            }
            
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
            
            Embedding embedding = embeddingModel.embed(query).content();
            if (embedding == null || embedding.vector() == null || embedding.vector().length == 0) {
                log.warn("查询向量生成失败: query={}", query);
                return Collections.emptyList();
            }
            float[] embeddingArray = embedding.vector();
            if (embeddingArray == null || embeddingArray.length == 0) {
                log.warn("查询向量数组为空: query={}", query);
                return Collections.emptyList();
            }
            
            int actualDimension = embeddingArray.length;
            int targetDimension = Math.min(actualDimension, 2000);
            
            float[] truncatedArray = new float[targetDimension];
            System.arraycopy(embeddingArray, 0, truncatedArray, 0, targetDimension);
            
            PGvector queryVector = new PGvector(truncatedArray);
            
            String sql = String.format(
                "SELECT conversation_id, message_id, content, role, metadata, 1 - (embedding <=> ?) as similarity " +
                "FROM %s WHERE user_id = ? ORDER BY similarity DESC LIMIT %d",
                VECTOR_TABLE, limit
            );
            
            List<Map<String, Object>> results = jdbcTemplate.query(
                new PreparedStatementCreator() {
                    @Override
                    public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
                        PGvector.addVectorType(con);
                        try (java.sql.Statement stmt = con.createStatement()) {
                            stmt.execute("SET search_path TO public, nju_market");
                        }
                        PreparedStatement ps = con.prepareStatement(sql);
                        ps.setObject(1, queryVector, java.sql.Types.OTHER);
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
                    row.put("similarity", rs.getDouble("similarity"));
                    try {
                        String metadataJson = rs.getString("metadata");
                        if (metadataJson != null) {
                            row.put("metadata", metadataJson);
                        }
                    } catch (Exception e) {
                    }
                    return row;
                }
            );
            
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
                    Object createdAtObj = row.get("created_at");
                    if (createdAtObj instanceof java.sql.Timestamp) {
                        msg.setCreatedAt(((java.sql.Timestamp) createdAtObj).toInstant());
                    }
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
            
            String sql = String.format(
                "SELECT conversation_id, message_id, content, role, metadata, created_at " +
                "FROM %s WHERE user_id = ? " +
                "ORDER BY created_at DESC LIMIT %d",
                VECTOR_TABLE, limit
            );
            
            List<Map<String, Object>> results = jdbcTemplate.query(
                new PreparedStatementCreator() {
                    @Override
                    public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
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
                    try {
                        String metadataJson = rs.getString("metadata");
                        if (metadataJson != null) {
                            row.put("metadata", metadataJson);
                        }
                    } catch (Exception e) {
                    }
                    return row;
                }
            );
            
            com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
            return results.stream()
                .map(row -> {
                    ConversationMessage msg = new ConversationMessage();
                    msg.setConversationId((String) row.get("conversation_id"));
                    msg.setMessageId((String) row.get("message_id"));
                    msg.setContent((String) row.get("content"));
                    msg.setRole((String) row.get("role"));
                    msg.setSimilarity(null);
                    Object createdAtObj = row.get("created_at");
                    if (createdAtObj instanceof java.sql.Timestamp) {
                        msg.setCreatedAt(((java.sql.Timestamp) createdAtObj).toInstant());
                    }
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
    public List<ChatInfo> getUserChatList(String userId, int limit) {
        try {
            if (!StringUtils.hasText(userId)) {
                return Collections.emptyList();
            }
            
            String conversationIdsSql = String.format(
                "SELECT DISTINCT conversation_id " +
                "FROM %s " +
                "WHERE user_id = ? " +
                "ORDER BY conversation_id",
                VECTOR_TABLE
            );
            
            List<String> conversationIds = jdbcTemplate.query(
                conversationIdsSql,
                (rs, rowNum) -> rs.getString("conversation_id"),
                userId
            );
            
            if (conversationIds.isEmpty()) {
                return Collections.emptyList();
            }
            
            if (conversationIds.size() > limit) {
                conversationIds = conversationIds.subList(0, limit);
            }
            
            List<ChatInfo> chatInfos = new ArrayList<>();
            for (String convId : conversationIds) {
                String lastMessageSql = String.format(
                    "SELECT content, role, created_at " +
                    "FROM %s " +
                    "WHERE conversation_id = ? AND user_id = ? " +
                    "ORDER BY created_at DESC LIMIT 1",
                    VECTOR_TABLE
                );
                
                Map<String, Object> lastMessage = jdbcTemplate.queryForMap(lastMessageSql, convId, userId);
                
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
                
                String countSql = String.format(
                    "SELECT COUNT(*) as cnt " +
                    "FROM %s " +
                    "WHERE conversation_id = ? AND user_id = ?",
                    VECTOR_TABLE
                );
                Integer messageCount = jdbcTemplate.queryForObject(countSql, Integer.class, convId, userId);
                
                ChatInfo chatInfo = new ChatInfo();
                chatInfo.setConversationId(convId);
                
                if (title != null && title.length() > 50) {
                    title = title.substring(0, 50) + "...";
                }
                chatInfo.setTitle(title != null ? title : "新对话");
                
                String lastMessageContent = (String) lastMessage.get("content");
                if (lastMessageContent != null && lastMessageContent.length() > 100) {
                    lastMessageContent = lastMessageContent.substring(0, 100) + "...";
                }
                chatInfo.setLastMessage(lastMessageContent);
                chatInfo.setLastMessageRole((String) lastMessage.get("role"));
                
                java.sql.Timestamp timestamp = (java.sql.Timestamp) lastMessage.get("created_at");
                if (timestamp != null) {
                    chatInfo.setLastMessageTime(timestamp.toInstant());
                }
                
                chatInfo.setMessageCount(messageCount != null ? messageCount : 0);
                
                chatInfos.add(chatInfo);
            }
            
            chatInfos.sort((a, b) -> {
                if (a.getLastMessageTime() == null && b.getLastMessageTime() == null) return 0;
                if (a.getLastMessageTime() == null) return 1;
                if (b.getLastMessageTime() == null) return -1;
                return b.getLastMessageTime().compareTo(a.getLastMessageTime());
            });
            
            return chatInfos;
                
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
            
            String sql = String.format(
                "SELECT conversation_id, message_id, content, role, metadata, created_at " +
                "FROM %s " +
                "WHERE conversation_id = ? AND user_id = ? " +
                "ORDER BY created_at ASC " +
                "LIMIT %d",
                VECTOR_TABLE, limit
            );
            
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
                    try {
                        String metadataJson = rs.getString("metadata");
                        if (metadataJson != null) {
                            row.put("metadata", metadataJson);
                        }
                    } catch (Exception e) {
                    }
                    return row;
                }
            );
            
            com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
            return results.stream()
                .map(row -> {
                    ConversationMessage msg = new ConversationMessage();
                    msg.setConversationId((String) row.get("conversation_id"));
                    msg.setMessageId((String) row.get("message_id"));
                    msg.setContent((String) row.get("content"));
                    msg.setRole((String) row.get("role"));
                    msg.setSimilarity(null);
                    Object createdAtObj = row.get("created_at");
                    if (createdAtObj instanceof java.sql.Timestamp) {
                        msg.setCreatedAt(((java.sql.Timestamp) createdAtObj).toInstant());
                    }
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
    
    private void storeVector(String conversationId, String messageId, String userId,
                            float[] embedding, String content, String role,
                            Map<String, Object> metadata) {
        PGvector pgVector = new PGvector(embedding);
        
        String metadataJson = new com.fasterxml.jackson.databind.ObjectMapper()
            .valueToTree(metadata).toString();
        
        String sql = String.format(
            "INSERT INTO %s (conversation_id, message_id, user_id, embedding, content, role, metadata, created_at) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?::jsonb, CURRENT_TIMESTAMP)",
            VECTOR_TABLE
        );
        
        jdbcTemplate.update(new PreparedStatementCreator() {
            @Override
            public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
                PGvector.addVectorType(con);
                try (java.sql.Statement stmt = con.createStatement()) {
                    stmt.execute("SET search_path TO public, nju_market");
                }
                PreparedStatement ps = con.prepareStatement(sql);
                ps.setString(1, conversationId);
                ps.setString(2, messageId);
                ps.setString(3, userId);
                ps.setObject(4, pgVector, java.sql.Types.OTHER);
                ps.setString(5, content);
                ps.setString(6, role);
                ps.setString(7, metadataJson);
                return ps;
            }
        });
    }
}

