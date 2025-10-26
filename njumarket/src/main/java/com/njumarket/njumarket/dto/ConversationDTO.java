package com.njumarket.njumarket.dto;

import java.time.LocalDateTime;
import java.util.List;

public class ConversationDTO {
    private String conversationId;
    private String buyerId;
    private String sellerId;
    private String commodityId;
    private String orderId;
    private String lastMessageContent;
    private LocalDateTime lastMessageTime;
    private Integer unreadCount;
    private String status;
    
    // 对方用户信息
    private String otherUserId;
    private String otherUserNickname;
    private String otherUserAvatar;
    private Boolean otherUserIsDeleted;
    
    // 买家信息
    private String buyerNickname;
    private String buyerAvatar;
    
    // 卖家信息
    private String sellerNickname;
    private String sellerAvatar;
    
    // 关联商品信息（如果有）
    private CommoditySnapshotDTO commoditySnapshot;
    
    // 消息列表（用于对话详情）
    private List<MessageDTO> messages;
    private Integer totalMessages;
    
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
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
    
    public Integer getUnreadCount() {
        return unreadCount;
    }
    
    public void setUnreadCount(Integer unreadCount) {
        this.unreadCount = unreadCount;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public String getOtherUserId() {
        return otherUserId;
    }
    
    public void setOtherUserId(String otherUserId) {
        this.otherUserId = otherUserId;
    }
    
    public String getOtherUserNickname() {
        return otherUserNickname;
    }
    
    public void setOtherUserNickname(String otherUserNickname) {
        this.otherUserNickname = otherUserNickname;
    }
    
    public String getOtherUserAvatar() {
        return otherUserAvatar;
    }
    
    public void setOtherUserAvatar(String otherUserAvatar) {
        this.otherUserAvatar = otherUserAvatar;
    }
    
    public Boolean getOtherUserIsDeleted() {
        return otherUserIsDeleted;
    }
    
    public void setOtherUserIsDeleted(Boolean otherUserIsDeleted) {
        this.otherUserIsDeleted = otherUserIsDeleted;
    }
    
    public String getBuyerNickname() {
        return buyerNickname;
    }
    
    public void setBuyerNickname(String buyerNickname) {
        this.buyerNickname = buyerNickname;
    }
    
    public String getBuyerAvatar() {
        return buyerAvatar;
    }
    
    public void setBuyerAvatar(String buyerAvatar) {
        this.buyerAvatar = buyerAvatar;
    }
    
    public String getSellerNickname() {
        return sellerNickname;
    }
    
    public void setSellerNickname(String sellerNickname) {
        this.sellerNickname = sellerNickname;
    }
    
    public String getSellerAvatar() {
        return sellerAvatar;
    }
    
    public void setSellerAvatar(String sellerAvatar) {
        this.sellerAvatar = sellerAvatar;
    }
    
    public CommoditySnapshotDTO getCommoditySnapshot() {
        return commoditySnapshot;
    }
    
    public void setCommoditySnapshot(CommoditySnapshotDTO commoditySnapshot) {
        this.commoditySnapshot = commoditySnapshot;
    }
    
    public List<MessageDTO> getMessages() {
        return messages;
    }
    
    public void setMessages(List<MessageDTO> messages) {
        this.messages = messages;
    }
    
    public Integer getTotalMessages() {
        return totalMessages;
    }
    
    public void setTotalMessages(Integer totalMessages) {
        this.totalMessages = totalMessages;
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
}

class CommoditySnapshotDTO {
    private String commodityId;
    private String title;
    private String imageUrl;
    private Double price;
    private String status;
    
    // Getters and Setters
    public String getCommodityId() {
        return commodityId;
    }
    
    public void setCommodityId(String commodityId) {
        this.commodityId = commodityId;
    }
    
    public String getTitle() {
        return title;
    }
    
    public void setTitle(String title) {
        this.title = title;
    }
    
    public String getImageUrl() {
        return imageUrl;
    }
    
    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }
    
    public Double getPrice() {
        return price;
    }
    
    public void setPrice(Double price) {
        this.price = price;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
}
