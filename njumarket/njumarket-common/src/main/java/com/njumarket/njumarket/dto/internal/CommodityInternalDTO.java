package com.njumarket.njumarket.dto.internal;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商品内部传输对象（用于服务间通信）
 * 不包含关联对象，只包含必要字段
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CommodityInternalDTO implements Serializable {
    private String commodityId;
    private String sellerId;
    private String title;
    private String description;
    private BigDecimal price;
    private Integer stock;
    private String category;
    private String conditionLevel;
    private String status;
    private String sellerVisibility;
    private String buyerVisibility;
    private String location;
    private String images; // ✅ 添加图片字段（JSON格式的图片URL列表）
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    // 不包含 User、Order、ImageReference 等关联对象
}

