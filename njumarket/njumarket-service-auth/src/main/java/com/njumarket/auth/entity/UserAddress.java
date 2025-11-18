package com.njumarket.auth.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * 用户地址实体类
 * 存储用户的收货地址信息，支持PostGIS地理位置
 */
@Entity
@Table(name = "user_addresses", schema = "nju_market")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserAddress {
    
    @Id
    @Column(name = "address_id", length = 50)
    private String addressId;
    
    @Column(name = "user_id", length = 50, nullable = false)
    private String userId;
    
    // 地址基本信息
    @Column(name = "recipient_name", length = 100, nullable = false)
    private String recipientName;
    
    @Column(name = "recipient_phone", length = 20, nullable = false)
    private String recipientPhone;
    
    // 地址层级（省市区）
    @Column(name = "province", length = 50, nullable = false)
    private String province;
    
    @Column(name = "city", length = 50, nullable = false)
    private String city;
    
    @Column(name = "district", length = 50, nullable = false)
    private String district;
    
    // 详细地址
    @Column(name = "street_address", length = 200, nullable = false)
    private String streetAddress;
    
    @Column(name = "detail_address", length = 500)
    private String detailAddress;
    
    @Column(name = "full_address", columnDefinition = "TEXT", nullable = false)
    private String fullAddress;
    
    // 地理位置（PostGIS Geography类型）
    // 注意：使用 @ColumnTransformer 在写入时将 WKT 字符串转换为 PostGIS geography 类型
    // 在读取时将 geography 类型转换回 WKT 字符串
    @Column(name = "location", columnDefinition = "geography(Point,4326)")
    @org.hibernate.annotations.ColumnTransformer(
        write = "public.ST_SetSRID(public.ST_GeomFromText(CAST(? AS text)), 4326)::public.geography",  // 写入时：将 WKT 字符串转换为 geometry，设置 SRID，再转换为 geography
        read = "public.ST_AsText(location)" // 读取时：直接使用 PostGIS 的 ST_AsText 处理 geography 类型，明确指定 public schema
    )
    private String location; // 存储为 WKT 格式字符串，如 "POINT(118.959 32.114)"
    
    @Column(name = "longitude")
    private Double longitude;
    
    @Column(name = "latitude")
    private Double latitude;
    
    // 地址标签和状态
    @Column(name = "address_label", length = 20)
    private String addressLabel = "HOME"; // HOME, SCHOOL, COMPANY, OTHER
    
    @Column(name = "is_default", nullable = false)
    private Boolean isDefault = false;
    
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;
    
    // 时间戳
    @CreationTimestamp
    @Column(name = "create_time", nullable = false, updatable = false)
    private LocalDateTime createTime;
    
    @UpdateTimestamp
    @Column(name = "update_time", nullable = false)
    private LocalDateTime updateTime;
    
    // 关联关系
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    private User user;
    
    /**
     * 构建完整地址字符串
     * @return 完整地址
     */
    public String buildFullAddress() {
        StringBuilder sb = new StringBuilder();
        sb.append(province).append(city).append(district).append(streetAddress);
        if (detailAddress != null && !detailAddress.isEmpty()) {
            sb.append(detailAddress);
        }
        return sb.toString();
    }
    
    /**
     * 设置地理位置（经纬度）
     * @param longitude 经度
     * @param latitude 纬度
     */
    public void setLocation(Double longitude, Double latitude) {
        this.longitude = longitude;
        this.latitude = latitude;
        // 构建 PostGIS WKT 格式：POINT(longitude latitude)
        if (longitude != null && latitude != null) {
            this.location = String.format("POINT(%s %s)", longitude, latitude);
        }
    }
    
    /**
     * 检查是否为默认地址
     * @return 是否为默认地址
     */
    public Boolean isDefaultAddress() {
        return Boolean.TRUE.equals(isDefault);
    }
    
    /**
     * 检查地址是否启用
     * @return 是否启用
     */
    public Boolean isAddressActive() {
        return Boolean.TRUE.equals(isActive);
    }
}

