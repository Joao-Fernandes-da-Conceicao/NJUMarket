package com.njumarket.ai.vector;

import java.util.List;

/**
 * 对话历史向量化服务接口
 */
public interface ConversationVectorService {
    
    void storeConversationVector(String conversationId, String messageId, String userId, 
                                 String content, String role);
    
    void storeConversationVector(String conversationId, String messageId, String userId, 
                                 String content, String role, List<String> recommendedCommodityIds);
    
    List<ConversationMessage> searchRelevantConversations(String query, String userId, int limit);
    
    void deleteConversationVectors(String conversationId);
    
    List<ConversationMessage> getUserAIChatHistory(String userId, int limit);
    
    List<ChatInfo> getUserChatList(String userId, int limit);
    
    List<ConversationMessage> getChatMessages(String conversationId, String userId, int limit);
    
    class ConversationMessage {
        private String conversationId;
        private String messageId;
        private String content;
        private String role;
        private Double similarity;
        private List<String> recommendedCommodityIds;
        private java.time.Instant createdAt;
        
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
    
    class ChatInfo {
        private String conversationId;
        private String title;
        private String lastMessage;
        private String lastMessageRole;
        private java.time.Instant lastMessageTime;
        private Integer messageCount;
        
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

