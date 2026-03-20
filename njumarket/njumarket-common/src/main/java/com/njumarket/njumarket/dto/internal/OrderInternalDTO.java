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
    private LocalDateTime createTime;
    private LocalDateTime payTime;
    private LocalDateTime shippingTime;
    private LocalDateTime deliveryTime;
    
    // 地址相关字段
    private String shippingAddressId; // 引用用户地址表
    private String shippingAddressSnapshotFull; // 地址快照-完整地址
    private String shippingAddressSnapshotRecipientName; // 地址快照-收货人姓名
    private String shippingAddressSnapshotRecipientPhone; // 地址快照-收货人电话
    
    // 不包含 Commodity、User、OrderSnapshot 等关联对象
}

