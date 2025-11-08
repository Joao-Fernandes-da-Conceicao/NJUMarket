package com.njumarket.njumarket.dto.internal;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单内部传输对象（用于服务间通信）
 * 不包含关联对象，只包含必要字段
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderInternalDTO implements Serializable {
    private String orderId;
    private String commodityId;
    private String sellerId;
    private String buyerId;
    private String orderStatus;
    private BigDecimal payAmount;
    private Integer quantity;
    private String trackingNumber;
    private String sellerVisibility;
    private String buyerVisibility;
    private LocalDateTime createTime;
    private LocalDateTime payTime;
    private LocalDateTime shippingTime;
    private LocalDateTime deliveryTime;
    // 不包含 Commodity、User、OrderSnapshot 等关联对象
}

