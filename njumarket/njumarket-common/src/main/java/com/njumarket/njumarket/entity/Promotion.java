package com.njumarket.njumarket.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * 促销活动实体类
 * 管理各种促销活动和优惠券
 */
@Entity
@Table(name = "promotions")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Promotion {
    
    @Id
    @Column(name = "promotion_id", length = 50)
    private String promotionId;
    
    @Column(name = "user_id", length = 50)
    private String userId;
    
    @Column(name = "type", length = 20, nullable = false)
    private String type; // COUPON, FULL_REDUCE, LIMITED_DISCOUNT
    
    @Column(name = "rules", columnDefinition = "TEXT")
    private String rules; // JSON格式存储规则
    
    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;
    
    @Column(name = "end_time", nullable = false)
    private LocalDateTime endTime;
    
    @Column(name = "status", length = 20, nullable = false)
    private String status; // ACTIVE, INACTIVE, EXPIRED, USED
    
    @CreationTimestamp
    @Column(name = "create_time", nullable = false)
    private LocalDateTime createTime;
    
    @Column(name = "usage_count", nullable = false)
    private Integer usageCount = 0;
    
    @Column(name = "max_usage")
    private Integer maxUsage;
    
    /**
     * 创建促销活动
     * @return 创建是否成功
     */
    public Boolean createPromotion() {
        this.createTime = LocalDateTime.now();
        this.status = "INACTIVE";
        return true;
    }
    
    /**
     * 激活促销活动
     * @return 激活是否成功
     */
    public Boolean activatePromotion() {
        if (LocalDateTime.now().isBefore(this.endTime)) {
            this.status = "ACTIVE";
            return true;
        }
        return false;
    }
}

