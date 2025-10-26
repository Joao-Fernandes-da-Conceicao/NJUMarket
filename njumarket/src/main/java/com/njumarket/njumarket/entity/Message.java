package com.njumarket.njumarket.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "messages")
public class Message {
    
    @Id
    @Column(name = "message_id")
    private String messageId;
    
    @Column(name = "conversation_id", nullable = false)
    private String conversationId;
    
    @Column(name = "sender_id", nullable = false)
    private String senderId;
    
    @Column(name = "receiver_id", nullable = false)
    private String receiverId;
    
    @Column(name = "message_type")
    private String messageType = "TEXT";
    
    @Column(name = "content", columnDefinition = "TEXT", nullable = false)
    private String content;
    
    @Column(name = "image_url")
    private String imageUrl;
    
    @Column(name = "commodity_snapshot", columnDefinition = "JSON")
    private String commoditySnapshot;
    
    @Column(name = "order_snapshot", columnDefinition = "JSON")
    private String orderSnapshot;
    
    @Column(name = "is_read")
    private Boolean isRead = false;
    
    @Column(name = "read_time")
    private LocalDateTime readTime;
    
    @Column(name = "is_deleted")
    private Boolean isDeleted = false;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    // 瞬态字段，用于前端显示
    @Transient
    private UserProfile senderProfile;
    
    @Transient
    private Boolean isMine; // 是否是当前用户发送的消息
    
    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (messageId == null) {
            messageId = generateMessageId();
        }
        if (isRead == null) {
            isRead = false;
        }
        if (isDeleted == null) {
            isDeleted = false;
        }
    }
    
    private String generateMessageId() {
        return "MSG_" + System.currentTimeMillis() + "_" + 
               (int)(Math.random() * 10000);
    }
    
    // 判断消息是否属于某个用户
    public boolean isOwnedBy(String userId) {
        return senderId.equals(userId);
    }
    
    // 标记消息为已读
    public void markAsRead() {
        if (!isRead) {
            isRead = true;
            readTime = LocalDateTime.now();
        }
    }
    
    // Getters and Setters
    public String getMessageId() {
        return messageId;
    }
    
    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }
    
    public String getConversationId() {
        return conversationId;
    }
    
    public void setConversationId(String conversationId) {
        this.conversationId = conversationId;
    }
    
    public String getSenderId() {
        return senderId;
    }
    
    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }
    
    public String getReceiverId() {
        return receiverId;
    }
    
    public void setReceiverId(String receiverId) {
        this.receiverId = receiverId;
    }
    
    public String getMessageType() {
        return messageType;
    }
    
    public void setMessageType(String messageType) {
        this.messageType = messageType;
    }
    
    public String getContent() {
        return content;
    }
    
    public void setContent(String content) {
        this.content = content;
    }
    
    public String getImageUrl() {
        return imageUrl;
    }
    
    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }
    
    public String getCommoditySnapshot() {
        return commoditySnapshot;
    }
    
    public void setCommoditySnapshot(String commoditySnapshot) {
        this.commoditySnapshot = commoditySnapshot;
    }
    
    public String getOrderSnapshot() {
        return orderSnapshot;
    }
    
    public void setOrderSnapshot(String orderSnapshot) {
        this.orderSnapshot = orderSnapshot;
    }
    
    public Boolean getIsRead() {
        return isRead;
    }
    
    public void setIsRead(Boolean isRead) {
        this.isRead = isRead;
    }
    
    public LocalDateTime getReadTime() {
        return readTime;
    }
    
    public void setReadTime(LocalDateTime readTime) {
        this.readTime = readTime;
    }
    
    public Boolean getIsDeleted() {
        return isDeleted;
    }
    
    public void setIsDeleted(Boolean isDeleted) {
        this.isDeleted = isDeleted;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public UserProfile getSenderProfile() {
        return senderProfile;
    }
    
    public void setSenderProfile(UserProfile senderProfile) {
        this.senderProfile = senderProfile;
    }
    
    public Boolean getIsMine() {
        return isMine;
    }
    
    public void setIsMine(Boolean isMine) {
        this.isMine = isMine;
    }
}