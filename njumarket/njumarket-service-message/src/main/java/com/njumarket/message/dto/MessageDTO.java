package com.njumarket.message.dto;

import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;

public class MessageDTO {
    private String messageId;
    
    @NotBlank(message = "会话ID不能为空")
    private String conversationId;
    
    private String senderId;
    
    @NotBlank(message = "接收者ID不能为空")
    private String receiverId;
    
    private String messageType;
    
    private String content;
    private String imageUrl;
    private String commodityId;
    private CommoditySnapshotDTO commoditySnapshot; // 商品快照（COMMODITY_CARD 类型消息专用）
    private String orderId;
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

    public CommoditySnapshotDTO getCommoditySnapshot() {
        return commoditySnapshot;
    }

    public void setCommoditySnapshot(CommoditySnapshotDTO commoditySnapshot) {
        this.commoditySnapshot = commoditySnapshot;
    }

    /**
     * 商品快照 DTO（内嵌在 MessageDTO 中）
     * 记录发送 COMMODITY_CARD 消息时商品的核心状态，永久保留，不随商品后续变更而改变。
     */
    public static class CommoditySnapshotDTO {
        private String commodityId;
        private String title;
        private Double price;
        private String imageUrl;
        private String status;

        public String getCommodityId() { return commodityId; }
        public void setCommodityId(String commodityId) { this.commodityId = commodityId; }

        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }

        public Double getPrice() { return price; }
        public void setPrice(Double price) { this.price = price; }

        public String getImageUrl() { return imageUrl; }
        public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
    }
}

