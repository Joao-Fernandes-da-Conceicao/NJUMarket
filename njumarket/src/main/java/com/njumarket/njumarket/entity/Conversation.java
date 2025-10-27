package com.njumarket.njumarket.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "conversations", 
       uniqueConstraints = @UniqueConstraint(name = "uk_user_pair_active", 
                                            columnNames = {"user_id_1", "user_id_2", "status"}))
public class Conversation {
    
    @Id
    @Column(name = "conversation_id")
    private String conversationId;
    
    @Column(name = "user_id_1", nullable = false, length = 50)
    private String userId1; // 较小的userId，确保一致性
    
    @Column(name = "user_id_2", nullable = false, length = 50)
    private String userId2; // 较大的userId
    
    // 保留buyerId和sellerId作为兼容字段（用于查询，实际存储使用userId1和userId2）
    @Transient
    private String buyerId; // 兼容字段，不持久化
    
    @Transient
    private String sellerId; // 兼容字段，不持久化
    
    @Column(name = "last_message_content", columnDefinition = "TEXT")
    private String lastMessageContent;
    
    @Column(name = "last_message_time")
    private LocalDateTime lastMessageTime;
    
    @Column(name = "user_1_count")
    private Integer user1Count = 0;
    
    @Column(name = "user_2_count")
    private Integer user2Count = 0;
    
    @Column(name = "status")
    private String status = "ACTIVE";
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    // 瞬态字段，用于前端显示
    @Transient
    private UserProfile buyerProfile;
    
    @Transient
    private UserProfile sellerProfile;
    
    @Transient
    private Integer unreadCount; // 当前用户的未读数
    
    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (updatedAt == null) {
            updatedAt = LocalDateTime.now();
        }
        if (conversationId == null) {
            conversationId = generateConversationId();
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    private String generateConversationId() {
        return "CONV_" + System.currentTimeMillis() + "_" + 
               (int)(Math.random() * 10000);
    }
    
    /**
     * 标准化用户ID对（确保userId1 < userId2）
     */
    public static String[] normalizeUserPair(String userId1, String userId2) {
        if (userId1 == null || userId2 == null) {
            throw new IllegalArgumentException("用户ID不能为空");
        }
        if (userId1.compareTo(userId2) < 0) {
            return new String[]{userId1, userId2};
        } else {
            return new String[]{userId2, userId1};
        }
    }
    
    /**
     * 根据提供的userId1和userId2初始化（自动排序）
     */
    public void setUserPair(String userId1, String userId2) {
        String[] normalized = normalizeUserPair(userId1, userId2);
        this.userId1 = normalized[0];
        this.userId2 = normalized[1];
    }
    
    // 根据当前用户ID获取对方ID
    public String getOtherUserId(String currentUserId) {
        if (userId1.equals(currentUserId)) {
            return userId2;
        } else if (userId2.equals(currentUserId)) {
            return userId1;
        }
        return null;
    }
    
    // 检查用户是否参与此对话
    public boolean involvesUser(String userId) {
        return userId1.equals(userId) || userId2.equals(userId);
    }
    
    // 判断用户在对话中是第一个用户还是第二个用户
    public boolean isUser1(String userId) {
        return userId1.equals(userId);
    }
    
    // 根据当前用户ID获取未读数
    public Integer getUnreadCountForUser(String userId) {
        if (userId1.equals(userId)) {
            return user1Count; // 使用user1Count存储userId1的未读数
        } else if (userId2.equals(userId)) {
            return user2Count; // 使用user2Count存储userId2的未读数
        }
        return 0;
    }
    
    // 标记消息为已读
    public void markAsReadForUser(String userId) {
        if (userId1.equals(userId)) {
            user1Count = 0;
        } else if (userId2.equals(userId)) {
            user2Count = 0;
        }
    }
    
    // 增加未读数
    public void incrementUnreadForUser(String userId) {
        if (userId1.equals(userId)) {
            user1Count++;
        } else if (userId2.equals(userId)) {
            user2Count++;
        }
    }
    
    // Getters and Setters
    public String getConversationId() {
        return conversationId;
    }
    
    public void setConversationId(String conversationId) {
        this.conversationId = conversationId;
    }
    
    public String getUserId1() {
        return userId1;
    }
    
    public void setUserId1(String userId1) {
        this.userId1 = userId1;
    }
    
    public String getUserId2() {
        return userId2;
    }
    
    public void setUserId2(String userId2) {
        this.userId2 = userId2;
    }
    
    // 兼容方法：根据当前用户获取buyerId/sellerId（用于向后兼容）
    public String getBuyerId() {
        return buyerId; // 兼容字段
    }
    
    public void setBuyerId(String buyerId) {
        this.buyerId = buyerId; // 兼容字段，不持久化
    }
    
    public String getSellerId() {
        return sellerId; // 兼容字段
    }
    
    public void setSellerId(String sellerId) {
        this.sellerId = sellerId; // 兼容字段，不持久化
    }
    
    public String getLastMessageContent() {
        return lastMessageContent;
    }
    
    public void setLastMessageContent(String lastMessageContent) {
        this.lastMessageContent = lastMessageContent;
    }
    
    public LocalDateTime getLastMessageTime() {
        return lastMessageTime;
    }
    
    public void setLastMessageTime(LocalDateTime lastMessageTime) {
        this.lastMessageTime = lastMessageTime;
    }
    
    public Integer getUser1Count() {
        return user1Count;
    }
    
    public void setUser1Count(Integer user1Count) {
        this.user1Count = user1Count;
    }
    
    public Integer getUser2Count() {
        return user2Count;
    }
    
    public void setUser2Count(Integer user2Count) {
        this.user2Count = user2Count;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    public UserProfile getBuyerProfile() {
        return buyerProfile;
    }
    
    public void setBuyerProfile(UserProfile buyerProfile) {
        this.buyerProfile = buyerProfile;
    }
    
    public UserProfile getSellerProfile() {
        return sellerProfile;
    }
    
    public void setSellerProfile(UserProfile sellerProfile) {
        this.sellerProfile = sellerProfile;
    }
    
    public Integer getUnreadCount() {
        return unreadCount;
    }
    
    public void setUnreadCount(Integer unreadCount) {
        this.unreadCount = unreadCount;
    }
}
