package com.njumarket.njumarket.dto;

import java.time.LocalDateTime;

public class MessageDTO {
    private String messageId;
    private String conversationId;
    private String senderId;
    private String receiverId;
    private String messageType;
    private String content;
    private String imageUrl;
    private String commoditySnapshot;
    private String orderSnapshot;
    private Boolean isRead;
    private LocalDateTime readTime;
    private LocalDateTime createdAt;
    
    // 发送者信息
    private String senderNickname;
    private String senderAvatar;
    
    // 是否是当前用户发送的消息
    private Boolean isMine;
    
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
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public String getSenderNickname() {
        return senderNickname;
    }
    
    public void setSenderNickname(String senderNickname) {
        this.senderNickname = senderNickname;
    }
    
    public String getSenderAvatar() {
        return senderAvatar;
    }
    
    public void setSenderAvatar(String senderAvatar) {
        this.senderAvatar = senderAvatar;
    }
    
    public Boolean getIsMine() {
        return isMine;
    }
    
    public void setIsMine(Boolean isMine) {
        this.isMine = isMine;
    }
}