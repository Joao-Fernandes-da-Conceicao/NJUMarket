package com.njumarket.admin.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 会话实体类（admin-service 副本）
 * 用于管理端直接访问数据库
 * ⚠️ 注意：移除了跨服务的关联关系，只保留基本字段
 */
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
    
    @Column(name = "last_message_content", columnDefinition = "TEXT")
    private String lastMessageContent; // 用于管理后台（不过滤，显示真实最新消息）
    
    @Column(name = "last_message_time")
    private LocalDateTime lastMessageTime; // 用于管理后台（不过滤，显示真实最新消息）
    
    // ✅ 用户级别的最后消息字段（过滤用户删除的）
    @Column(name = "user_1_last_message_content", columnDefinition = "TEXT")
    private String user1LastMessageContent; // 用户1可见的最后一条消息
    
    @Column(name = "user_1_last_message_time")
    private LocalDateTime user1LastMessageTime; // 用户1可见的最后一条消息时间
    
    @Column(name = "user_2_last_message_content", columnDefinition = "TEXT")
    private String user2LastMessageContent; // 用户2可见的最后一条消息
    
    @Column(name = "user_2_last_message_time")
    private LocalDateTime user2LastMessageTime; // 用户2可见的最后一条消息时间
    
    @Column(name = "user_1_count")
    private Integer user1Count = 0;
    
    @Column(name = "user_2_count")
    private Integer user2Count = 0;
    
    @Column(name = "status")
    private String status = "ACTIVE";
    
    // ✅ 双方可见性字段（用于用户删除会话，不影响其他功能）
    @Column(name = "user_1_visibility", nullable = false)
    private Boolean user1Visibility = true;
    
    @Column(name = "user_2_visibility", nullable = false)
    private Boolean user2Visibility = true;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
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
    
    public String getUser1LastMessageContent() {
        return user1LastMessageContent;
    }
    
    public void setUser1LastMessageContent(String user1LastMessageContent) {
        this.user1LastMessageContent = user1LastMessageContent;
    }
    
    public LocalDateTime getUser1LastMessageTime() {
        return user1LastMessageTime;
    }
    
    public void setUser1LastMessageTime(LocalDateTime user1LastMessageTime) {
        this.user1LastMessageTime = user1LastMessageTime;
    }
    
    public String getUser2LastMessageContent() {
        return user2LastMessageContent;
    }
    
    public void setUser2LastMessageContent(String user2LastMessageContent) {
        this.user2LastMessageContent = user2LastMessageContent;
    }
    
    public LocalDateTime getUser2LastMessageTime() {
        return user2LastMessageTime;
    }
    
    public void setUser2LastMessageTime(LocalDateTime user2LastMessageTime) {
        this.user2LastMessageTime = user2LastMessageTime;
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
    
    public Boolean getUser1Visibility() {
        return user1Visibility != null ? user1Visibility : true;
    }
    
    public void setUser1Visibility(Boolean user1Visibility) {
        this.user1Visibility = user1Visibility;
    }
    
    public Boolean getUser2Visibility() {
        return user2Visibility != null ? user2Visibility : true;
    }
    
    public void setUser2Visibility(Boolean user2Visibility) {
        this.user2Visibility = user2Visibility;
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
    
    /**
     * 获取指定用户可见的最后一条消息内容
     * @param userId 用户ID
     * @return 最后一条消息内容
     */
    public String getLastMessageContentForUser(String userId) {
        if (userId == null) {
            return null;
        }
        if (userId.equals(userId1)) {
            return user1LastMessageContent;
        } else if (userId.equals(userId2)) {
            return user2LastMessageContent;
        }
        return null;
    }
    
    /**
     * 获取指定用户可见的最后一条消息时间
     * @param userId 用户ID
     * @return 最后一条消息时间
     */
    public LocalDateTime getLastMessageTimeForUser(String userId) {
        if (userId == null) {
            return null;
        }
        if (userId.equals(userId1)) {
            return user1LastMessageTime;
        } else if (userId.equals(userId2)) {
            return user2LastMessageTime;
        }
        return null;
    }
    
    /**
     * 设置指定用户可见的最后一条消息
     * @param userId 用户ID
     * @param content 消息内容
     * @param time 消息时间
     */
    public void setLastMessageForUser(String userId, String content, LocalDateTime time) {
        if (userId == null) {
            return;
        }
        if (userId.equals(userId1)) {
            this.user1LastMessageContent = content;
            this.user1LastMessageTime = time;
        } else if (userId.equals(userId2)) {
            this.user2LastMessageContent = content;
            this.user2LastMessageTime = time;
        }
    }
}

