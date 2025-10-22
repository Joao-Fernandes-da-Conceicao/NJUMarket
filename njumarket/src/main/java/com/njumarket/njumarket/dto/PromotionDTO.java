package com.njumarket.njumarket.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 促销活动数据传输对象
 */
@Data
public class PromotionDTO {
    private String promotionId;
    private String userId;
    private String type; // COUPON, FULL_REDUCE, LIMITED_DISCOUNT
    private String title;
    private String description;
    private String rules; // JSON格式的规则
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String status;
    private Integer maxUsage;
    private Double discountAmount;
    private Double minOrderAmount;
}
