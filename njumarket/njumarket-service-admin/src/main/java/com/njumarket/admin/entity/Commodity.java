package com.njumarket.admin.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * 商品实体类（admin-service 副本）
 * 用于管理端直接访问数据库
 * ⚠️ 注意：移除了跨服务的关联关系，只保留基本字段
 */
@Entity
@Table(name = "commodities")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Commodity {
    
    @Id
    @Column(name = "commodity_id", length = 50)
    private String commodityId;
    
    @Column(name = "seller_id", length = 50, nullable = false)
    private String sellerId;
    
    @Column(name = "title", length = 200, nullable = false)
    private String title;
    
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;
    
    @Column(name = "price", nullable = false)
    private Double price;
    
    @Column(name = "stock", nullable = false)
    private Integer stock;
    
    @Column(name = "location", length = 200)
    private String location;
    
    @CreationTimestamp
    @Column(name = "publish_time", nullable = false)
    private LocalDateTime publishTime;
    
    @Column(name = "commodity_status", length = 20, nullable = false)
    private String commodityStatus; // DRAFT, PUBLISHED, ON_SHELF, OFF_SHELF
    
    @Column(name = "seller_visibility", length = 20, nullable = false)
    private String sellerVisibility = "PUBLIC"; // PUBLIC, PRIVATE, HIDDEN
    
    @Column(name = "buyer_visibility", length = 20, nullable = false)
    private String buyerVisibility = "PUBLIC"; // PUBLIC, PRIVATE, HIDDEN
    
    @Column(name = "category", length = 50)
    private String category;
    
    @Column(name = "condition_level", length = 20)
    private String conditionLevel = "GOOD"; // EXCELLENT, GOOD, FAIR, POOR
    
    @Column(name = "images", columnDefinition = "TEXT")
    private String images; // JSON格式的图片URL列表
    
    @Column(name = "click_count", nullable = false)
    private Integer clickCount = 0;
    
    @Column(name = "report_count", nullable = false)
    private Integer reportCount = 0;
}

