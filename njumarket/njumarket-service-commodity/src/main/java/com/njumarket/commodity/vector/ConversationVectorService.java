package com.njumarket.commodity.vector;

import java.util.List;

/**
 * 对话历史向量化服务接口
 */
public interface ConversationVectorService {
    
    /**
     * 存储对话消息向量
     * @param conversationId 对话ID
     * @param messageId 消息ID
     * @param userId 用户ID
     * @param content 消息内容
     * @param role 消息角色（user/assistant/system）
     */
    void storeConversationVector(String conversationId, String messageId, String userId, 
                                 String content, String role);
    
    /**
     * 存储对话消息向量（带推荐商品ID列表）
     * @param conversationId 对话ID
     * @param messageId 消息ID
     * @param userId 用户ID
     * @param content 消息内容
     * @param role 消息角色（user/assistant/system）
     * @param recommendedCommodityIds 推荐的商品ID列表（可选，仅用于assistant消息）
     */
    void storeConversationVector(String conversationId, String messageId, String userId, 
                                 String content, String role, List<String> recommendedCommodityIds);
    
    /**
     * 搜索相关对话历史
     * @param query 查询文本
     * @param userId 用户ID
     * @param limit 返回数量限制
     * @return 相关对话消息列表
     */
    List<ConversationMessage> searchRelevantConversations(String query, String userId, int limit);
    
    /**
     * 删除对话历史向量
     * @param conversationId 对话ID
     */
    void deleteConversationVectors(String conversationId);
    
    /**
     * 获取用户的所有AI聊天记录（按时间倒序）
     * @param userId 用户ID
     * @param limit 返回数量限制
     * @return 对话消息列表（按时间倒序）
     */
    List<ConversationMessage> getUserAIChatHistory(String userId, int limit);
    
    /**
     * 获取用户的所有chat列表（按最后消息时间倒序）
     * @param userId 用户ID
     * @param limit 返回数量限制
     * @return chat列表
     */
    List<ChatInfo> getUserChatList(String userId, int limit);
    
    /**
     * 获取指定chat的消息列表（按时间正序）
     * @param conversationId 对话ID
     * @param userId 用户ID（用于权限验证）
     * @param limit 返回数量限制
     * @return 消息列表
     */
    List<ConversationMessage> getChatMessages(String conversationId, String userId, int limit);
    
    /**
     * 对话消息实体
     */
    class ConversationMessage {
        private String conversationId;
        private String messageId;
        private String content;
        private String role;
        private Double similarity;
        private List<String> recommendedCommodityIds; // 推荐的商品ID列表（仅assistant消息有）
        private java.time.Instant createdAt; // 创建时间
        
        // Getters and Setters
        public String getConversationId() { return conversationId; }
        public void setConversationId(String conversationId) { this.conversationId = conversationId; }
        
        public String getMessageId() { return messageId; }
        public void setMessageId(String messageId) { this.messageId = messageId; }
        
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
        
        public String getRole() { return role; }
        public void setRole(String role) { this.role = role; }
        
        public Double getSimilarity() { return similarity; }
        public void setSimilarity(Double similarity) { this.similarity = similarity; }
        
        public List<String> getRecommendedCommodityIds() { return recommendedCommodityIds; }
        public void setRecommendedCommodityIds(List<String> recommendedCommodityIds) { 
            this.recommendedCommodityIds = recommendedCommodityIds; 
        }
        
        public java.time.Instant getCreatedAt() { return createdAt; }
        public void setCreatedAt(java.time.Instant createdAt) { this.createdAt = createdAt; }
    }
    
    /**
     * Chat信息实体
     */
    class ChatInfo {
        private String conversationId;
        private String title; // 聊天标题（第一条用户消息的前50个字符）
        private String lastMessage; // 最后一条消息内容
        private String lastMessageRole; // 最后一条消息角色
        private java.time.Instant lastMessageTime; // 最后一条消息时间
        private Integer messageCount; // 消息数量
        
        // Getters and Setters
        public String getConversationId() { return conversationId; }
        public void setConversationId(String conversationId) { this.conversationId = conversationId; }
        
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        
        public String getLastMessage() { return lastMessage; }
        public void setLastMessage(String lastMessage) { this.lastMessage = lastMessage; }
        
        public String getLastMessageRole() { return lastMessageRole; }
        public void setLastMessageRole(String lastMessageRole) { this.lastMessageRole = lastMessageRole; }
        
        public java.time.Instant getLastMessageTime() { return lastMessageTime; }
        public void setLastMessageTime(java.time.Instant lastMessageTime) { this.lastMessageTime = lastMessageTime; }
        
        public Integer getMessageCount() { return messageCount; }
        public void setMessageCount(Integer messageCount) { this.messageCount = messageCount; }
    }
}

