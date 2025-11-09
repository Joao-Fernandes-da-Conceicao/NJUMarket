package com.njumarket.admin.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 消息实体类（admin-service 副本）
 * 用于管理端直接访问数据库
 * ⚠️ 注意：移除了跨服务的关联关系，只保留基本字段
 */
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
    
    @Column(name = "commodity_id", length = 50)
    private String commodityId; // 商品ID（实时查询，用于商品卡片）
    
    @Column(name = "order_id", length = 50)
    private String orderId; // 订单ID（实时查询，用于订单卡片）
    
    @Column(name = "is_read")
    private Boolean isRead = false;
    
    @Column(name = "read_time")
    private LocalDateTime readTime;
    
    @Column(name = "deleted_by_sender")
    private Boolean deletedBySender = false;
    
    @Column(name = "deleted_by_receiver")
    private Boolean deletedByReceiver = false;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
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
        if (deletedBySender == null) {
            deletedBySender = false;
        }
        if (deletedByReceiver == null) {
            deletedByReceiver = false;
        }
    }
    
    private String generateMessageId() {
        return "MSG_" + System.currentTimeMillis() + "_" + 
               (int)(Math.random() * 10000);
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
    
    public Boolean getDeletedBySender() {
        return deletedBySender;
    }
    
    public void setDeletedBySender(Boolean deletedBySender) {
        this.deletedBySender = deletedBySender;
    }
    
    public Boolean getDeletedByReceiver() {
        return deletedByReceiver;
    }
    
    public void setDeletedByReceiver(Boolean deletedByReceiver) {
        this.deletedByReceiver = deletedByReceiver;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}

