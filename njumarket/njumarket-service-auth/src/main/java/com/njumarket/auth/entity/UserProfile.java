package com.njumarket.auth.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.Map;

/**
 * 用户档案实体类
 * 存储用户的详细信息和评分
 */
@Entity
@Table(name = "user_profiles")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserProfile {
    
    @Id
    @Column(name = "profile_id", length = 50)
    private String profileId;
    
    @Column(name = "user_id", length = 50, nullable = false, unique = true)
    private String userId;
    
    @Column(name = "nickname", length = 50)
    private String nickname;
    
    @Column(name = "avatar", length = 500)
    private String avatar;
    
    @Column(name = "credit_score", nullable = false)
    private Integer creditScore = 100;
    
    @Column(name = "buyer_rating")
    private Double buyerRating = 5.0;
    
    @Column(name = "seller_rating")
    private Double sellerRating = 5.0;
    
    @Column(name = "total_sales", nullable = false)
    private Integer totalSales = 0;
    
    @Column(name = "total_purchases", nullable = false)
    private Integer totalPurchases = 0;
    
    @Column(name = "vip_level", length = 20)
    private String vipLevel = "NORMAL"; // NORMAL, BRONZE, SILVER, GOLD, PLATINUM
    
    // ✅ v1.3.x: 订单提醒字段（可选，向后兼容）
    @Column(name = "seller_order_has_new", nullable = true)
    private Boolean sellerOrderHasNew = false;
    
    @Column(name = "buyer_order_has_new", nullable = true)
    private Boolean buyerOrderHasNew = false;
    
    // 一对一关系：档案属于某个用户
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    @JsonIgnore  // 避免Jackson序列化时的循环引用（服务间通信已使用内部DTO，但保留此注解以防直接序列化实体）
    private User user;
    
    /**
     * 更新用户档案信息
     * @param fields 要更新的字段
     * @return 更新是否成功
     */
    public Boolean updateProfile(Map<String, Object> fields) {
        // 业务逻辑：更新指定字段
        if (fields.containsKey("nickname")) {
            this.nickname = (String) fields.get("nickname");
        }
        if (fields.containsKey("avatar")) {
            this.avatar = (String) fields.get("avatar");
        }
        return true;
    }
    
    /**
     * 更新用户评分
     * @param rating 新评分
     * @param role 角色类型（buyer/seller）
     * @return 更新是否成功
     */
    public Boolean updateRating(Double rating, String role) {
        if ("buyer".equals(role)) {
            this.buyerRating = rating;
        } else if ("seller".equals(role)) {
            this.sellerRating = rating;
        }
        return true;
    }
}

