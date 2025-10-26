package com.njumarket.njumarket.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "conversations")
public class Conversation {
    
    @Id
    @Column(name = "conversation_id")
    private String conversationId;
    
    @Column(name = "buyer_id", nullable = false)
    private String buyerId;
    
    @Column(name = "seller_id", nullable = false)
    private String sellerId;
    
    @Column(name = "commodity_id")
    private String commodityId;
    
    @Column(name = "order_id")
    private String orderId;
    
    @Column(name = "last_message_content", columnDefinition = "TEXT")
    private String lastMessageContent;
    
    @Column(name = "last_message_time")
    private LocalDateTime lastMessageTime;
    
    @Column(name = "buyer_unread_count")
    private Integer buyerUnreadCount = 0;
    
    @Column(name = "seller_unread_count")
    private Integer sellerUnreadCount = 0;
    
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
    
    // 根据当前用户ID获取对方ID
    public String getOtherUserId(String currentUserId) {
        if (buyerId.equals(currentUserId)) {
            return sellerId;
        } else if (sellerId.equals(currentUserId)) {
            return buyerId;
        }
        return null;
    }
    
    // 根据当前用户ID获取未读数
    public Integer getUnreadCountForUser(String userId) {
        if (buyerId.equals(userId)) {
            return buyerUnreadCount;
        } else if (sellerId.equals(userId)) {
            return sellerUnreadCount;
        }
        return 0;
    }
    
    // 标记消息为已读
    public void markAsReadForUser(String userId) {
        if (buyerId.equals(userId)) {
            buyerUnreadCount = 0;
        } else if (sellerId.equals(userId)) {
            sellerUnreadCount = 0;
        }
    }
    
    // 增加未读数
    public void incrementUnreadForUser(String userId) {
        if (buyerId.equals(userId)) {
            buyerUnreadCount++;
        } else if (sellerId.equals(userId)) {
            sellerUnreadCount++;
        }
    }
    
    // Getters and Setters
    public String getConversationId() {
        return conversationId;
    }
    
    public void setConversationId(String conversationId) {
        this.conversationId = conversationId;
    }
    
    public String getBuyerId() {
        return buyerId;
    }
    
    public void setBuyerId(String buyerId) {
        this.buyerId = buyerId;
    }
    
    public String getSellerId() {
        return sellerId;
    }
    
    public void setSellerId(String sellerId) {
        this.sellerId = sellerId;
    }
    
    public String getCommodityId() {
        return commodityId;
    }
    
    public void setCommodityId(String commodityId) {
        this.commodityId = commodityId;
    }
    
    public String getOrderId() {
        return orderId;
    }
    
    public void setOrderId(String orderId) {
        this.orderId = orderId;
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
    
    public Integer getBuyerUnreadCount() {
        return buyerUnreadCount;
    }
    
    public void setBuyerUnreadCount(Integer buyerUnreadCount) {
        this.buyerUnreadCount = buyerUnreadCount;
    }
    
    public Integer getSellerUnreadCount() {
        return sellerUnreadCount;
    }
    
    public void setSellerUnreadCount(Integer sellerUnreadCount) {
        this.sellerUnreadCount = sellerUnreadCount;
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
