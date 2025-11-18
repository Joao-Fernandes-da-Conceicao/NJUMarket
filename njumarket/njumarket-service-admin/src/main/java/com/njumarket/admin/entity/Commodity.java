package com.njumarket.admin.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.ColumnTransformer;

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
    private String location; // 保留原有字段，用于兼容
    
    // ========== 地址相关字段 ==========
    @Column(name = "address_id", length = 50)
    private String addressId; // 引用用户地址表，表示商品所在位置
    
    // 地址快照字段（保存发布时的地址信息）
    @Column(name = "address_snapshot_province", length = 50)
    private String addressSnapshotProvince;
    
    @Column(name = "address_snapshot_city", length = 50)
    private String addressSnapshotCity;
    
    @Column(name = "address_snapshot_district", length = 50)
    private String addressSnapshotDistrict;
    
    @Column(name = "address_snapshot_street", length = 200)
    private String addressSnapshotStreet;
    
    @Column(name = "address_snapshot_detail", length = 500)
    private String addressSnapshotDetail;
    
    @Column(name = "address_snapshot_full", columnDefinition = "TEXT")
    private String addressSnapshotFull;
    
    // 地理位置字段（用于地理搜索和距离计算）
    @Column(name = "location_geography", columnDefinition = "geography(Point,4326)")
    @ColumnTransformer(
        write = "public.ST_SetSRID(public.ST_GeomFromText(CAST(? AS text)), 4326)::public.geography",
        read = "public.ST_AsText(location_geography)"
    )
    private String locationGeography; // PostGIS Geography类型，存储为WKT格式
    
    @Column(name = "longitude")
    private Double longitude;
    
    @Column(name = "latitude")
    private Double latitude;
    
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

