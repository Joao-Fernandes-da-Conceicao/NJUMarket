package com.njumarket.njumarket.dto;

import lombok.Data;

/**
 * 订单数据传输对象
 */
@Data
public class OrderDTO {
    private String orderId;
    private String buyerId;
    private String sellerId;
    private String commodityId;
    private String orderStatus;
    private String sellerVisibility;
    private String buyerVisibility;
    private Double payAmount;
    private Integer quantity;
    private String shippingAddress;
    private String trackingNumber;
    private String remark;
    
    // 退货相关字段
    private String returnReason;
    private String returnRequestTime;
    private String returnApprovalTime;
    private String returnRejectionReason;
    private String returnTrackingNumber;
    private String returnCompletionTime;
}
