package com.njumarket.message.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 商品快照实体
 * 当用户发送 COMMODITY_CARD 类型消息时，保存商品在发送时刻的核心信息。
 * 快照永久保留，不随商品后续变更而改变，确保聊天历史中卡片内容的准确性。
 */
@Entity
@Table(name = "commodity_snapshots")
public class CommoditySnapshot {

    @Id
    @Column(name = "snapshot_id", length = 50)
    private String snapshotId;

    @Column(name = "message_id", length = 255, nullable = false, unique = true)
    private String messageId;

    @Column(name = "commodity_id", length = 50, nullable = false)
    private String commodityId;

    @Column(name = "title", length = 200)
    private String title;

    @Column(name = "price")
    private Double price;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Column(name = "status", length = 20)
    private String status;

    @Column(name = "snapshot_time")
    private LocalDateTime snapshotTime;

    @PrePersist
    protected void onCreate() {
        if (snapshotId == null) {
            snapshotId = "SNAP_" + System.currentTimeMillis() + "_" + (int) (Math.random() * 10000);
        }
        if (snapshotTime == null) {
            snapshotTime = LocalDateTime.now();
        }
    }

    public String getSnapshotId() { return snapshotId; }
    public void setSnapshotId(String snapshotId) { this.snapshotId = snapshotId; }

    public String getMessageId() { return messageId; }
    public void setMessageId(String messageId) { this.messageId = messageId; }

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

    public LocalDateTime getSnapshotTime() { return snapshotTime; }
    public void setSnapshotTime(LocalDateTime snapshotTime) { this.snapshotTime = snapshotTime; }
}
