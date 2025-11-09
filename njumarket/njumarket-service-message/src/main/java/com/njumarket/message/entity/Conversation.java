package com.njumarket.message.entity;

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
    // true (1) 表示可见，false (0) 表示不可见（用户删除了此会话）
    // 默认值为 true (可见)
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
        if (currentUserId == null) {
            return null;
        }
        if (currentUserId.equals(userId1)) {
            return userId2;
        } else if (currentUserId.equals(userId2)) {
            return userId1;
        }
        return null;
    }
    
    // 检查用户是否参与此对话
    public boolean involvesUser(String userId) {
        if (userId == null) {
            return false;
        }
        return userId.equals(userId1) || userId.equals(userId2);
    }
    
    // 判断用户在对话中是第一个用户还是第二个用户
    public boolean isUser1(String userId) {
        if (userId == null) {
            return false;
        }
        return userId.equals(userId1);
    }
    
    // 根据当前用户ID获取未读数
    public Integer getUnreadCountForUser(String userId) {
        if (userId == null) {
            return 0;
        }
        if (userId.equals(userId1)) {
            return user1Count != null ? user1Count : 0; // 使用user1Count存储userId1的未读数
        } else if (userId.equals(userId2)) {
            return user2Count != null ? user2Count : 0; // 使用user2Count存储userId2的未读数
        }
        return 0;
    }
    
    // 标记消息为已读
    public void markAsReadForUser(String userId) {
        if (userId == null) {
            return;
        }
        if (userId.equals(userId1)) {
            user1Count = 0;
        } else if (userId.equals(userId2)) {
            user2Count = 0;
        }
    }
    
    // 增加未读数
    public void incrementUnreadForUser(String userId) {
        if (userId == null) {
            return;
        }
        if (userId.equals(userId1)) {
            if (user1Count == null) {
                user1Count = 0;
            }
            user1Count++;
        } else if (userId.equals(userId2)) {
            if (user2Count == null) {
                user2Count = 0;
            }
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
    
    // 用户级别的最后消息字段的Getter和Setter
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
    
    // 辅助方法：根据用户ID获取对应的最后消息内容和时间
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
    
    // 辅助方法：根据用户ID设置对应的最后消息内容和时间
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
    
    // ✅ 可见性相关方法
    /**
     * 获取用户在此会话中的可见性
     * @param userId 用户ID
     * @return true表示可见，false表示不可见
     */
    public Boolean getVisibilityForUser(String userId) {
        if (userId1.equals(userId)) {
            return user1Visibility != null ? user1Visibility : true;
        } else if (userId2.equals(userId)) {
            return user2Visibility != null ? user2Visibility : true;
        }
        return true; // 默认可见
    }
    
    /**
     * 设置用户在此会话中的可见性
     * @param userId 用户ID
     * @param visibility true表示可见，false表示不可见
     * @return 设置是否成功
     */
    public Boolean setVisibilityForUser(String userId, Boolean visibility) {
        if (userId1.equals(userId)) {
            this.user1Visibility = visibility;
            return true;
        } else if (userId2.equals(userId)) {
            this.user2Visibility = visibility;
            return true;
        }
        return false;
    }
    
    /**
     * 恢复用户在此会话中的可见性（设置为可见）
     * @param userId 用户ID
     * @return 恢复是否成功
     */
    public Boolean restoreVisibilityForUser(String userId) {
        return setVisibilityForUser(userId, true);
    }
    
    /**
     * 检查用户是否可见此会话（用于查询过滤）
     * @param userId 用户ID
     * @return true表示可见，false表示不可见
     */
    public Boolean isVisibleToUser(String userId) {
        return getVisibilityForUser(userId);
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
}

