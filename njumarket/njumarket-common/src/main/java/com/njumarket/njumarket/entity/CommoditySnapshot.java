package com.njumarket.njumarket.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * 商品快照实体类
 * 用于在消息中发送商品信息，即使原商品被删除或修改，快照信息仍然保留
 */
@Entity
@Table(name = "commodity_snapshots")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CommoditySnapshot {
    
    @Id
    @Column(name = "snapshot_id", length = 50)
    private String snapshotId;
    
    @Column(name = "original_commodity_id", length = 50)
    private String originalCommodityId; // 原始商品ID（可选，用于追溯）
    
    @Column(name = "title", length = 200, nullable = false)
    private String title;
    
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;
    
    @Column(name = "price", nullable = false)
    private Double price;
    
    @Column(name = "stock")
    private Integer stock;
    
    @Column(name = "location", length = 200)
    private String location;
    
    @Column(name = "category", length = 50)
    private String category;
    
    @Column(name = "condition_level", length = 20)
    private String conditionLevel;
    
    @Column(name = "images", columnDefinition = "TEXT")
    private String images; // 逗号分隔的图片URL
    
    @Column(name = "commodity_status", length = 20)
    private String commodityStatus;
    
    @Column(name = "seller_id", length = 50)
    private String sellerId;
    
    @Column(name = "seller_name", length = 100)
    private String sellerName;
    
    @Column(name = "seller_phone", length = 20)
    private String sellerPhone;
    
    @Column(name = "seller_email", length = 100)
    private String sellerEmail;
    
    @CreationTimestamp
    @Column(name = "snapshot_time", nullable = false, updatable = false)
    private LocalDateTime snapshotTime;
    
    /**
     * 从商品实体创建快照
     */
    public static CommoditySnapshot fromCommodity(Commodity commodity, User seller) {
        if (commodity == null) {
            return null;
        }
        
        CommoditySnapshot snapshot = new CommoditySnapshot();
        snapshot.setSnapshotId("CS_" + System.currentTimeMillis() + "_" + (int)(Math.random() * 10000));
        snapshot.setOriginalCommodityId(commodity.getCommodityId());
        snapshot.setTitle(commodity.getTitle());
        snapshot.setDescription(commodity.getDescription());
        snapshot.setPrice(commodity.getPrice());
        snapshot.setStock(commodity.getStock());
        snapshot.setLocation(commodity.getLocation());
        snapshot.setCategory(commodity.getCategory());
        snapshot.setConditionLevel(commodity.getConditionLevel());
        snapshot.setImages(commodity.getImages());
        snapshot.setCommodityStatus(commodity.getCommodityStatus());
        
        if (seller != null) {
            snapshot.setSellerId(seller.getUserId());
            snapshot.setSellerName(seller.getUsername() != null ? seller.getUsername() : seller.getUserId());
            snapshot.setSellerPhone(seller.getPrimaryPhone());
            // sellerEmail可以从UserProfile或ContactInfo获取，此处暂设为null
            snapshot.setSellerEmail(null);
        }
        
        return snapshot;
    }
}

