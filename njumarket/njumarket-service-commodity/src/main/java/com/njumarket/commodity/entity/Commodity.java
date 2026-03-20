package com.njumarket.commodity.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * 商品实体类
 * 包含商品的基本信息、状态管理和业务方法
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
    
    // TODO: 库存设计重构 —— stock 字段当前存于商品表，由商品服务直接管理，存在跨服务扣减的耦合问题。
    //   后续规划：将库存拆分为独立的 commodity_inventory 表，归属于订单服务；
    //   商品服务仅保留商品基础信息（快照），超卖防护由订单服务本地完成。
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
    // 注意：使用 @ColumnTransformer 在写入时将 WKT 字符串转换为 PostGIS geography 类型
    // 在读取时将 geography 类型转换回 WKT 字符串
    @Column(name = "location_geography", columnDefinition = "geography(Point,4326)")
    @org.hibernate.annotations.ColumnTransformer(
        write = "public.ST_SetSRID(public.ST_GeomFromText(CAST(? AS text)), 4326)::public.geography",  // 写入时：将 WKT 字符串转换为 geometry，设置 SRID，再转换为 geography
        read = "public.ST_AsText(location_geography)" // 读取时：直接使用 PostGIS 的 ST_AsText 处理 geography 类型，明确指定 public schema（注意：Hibernate 会自动添加表别名）
    )
    private String locationGeography; // PostGIS Geography类型，存储为WKT格式
    
    @Column(name = "longitude")
    private Double longitude;
    
    @Column(name = "latitude")
    private Double latitude;
    
    @CreationTimestamp
    @Column(name = "publish_time", nullable = false, columnDefinition = "timestamp with time zone")
    @jakarta.persistence.Convert(converter = com.njumarket.commodity.config.LocalDateTimeSecondsConverter.class)
    private LocalDateTime publishTime;
    
    @Column(name = "commodity_status", length = 20, nullable = false)
    private String commodityStatus; // DRAFT, PUBLISHED, ON_SHELF, OFF_SHELF

    /**
     * 管理端软隐藏标志。
     * PUBLIC  = 正常对买家可见（配合 ON_SHELF 状态）
     * HIDDEN  = 管理端强制屏蔽，不出现在任何买家查询结果中
     * 注意：卖家可见性由 sellerId 过滤保证，不需要单独字段控制。
     */
    @Column(name = "buyer_visibility", length = 20, nullable = false)
    private String buyerVisibility = "PUBLIC"; // PUBLIC, HIDDEN
    
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
    
    /**
     * 更新库存（正数增加，负数减少）。
     */
    public Boolean updateStock(Integer num) {
        if (this.stock + num >= 0) {
            this.stock += num;
            return true;
        }
        return false;
    }
}

