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
    
    // ========== 商品快照字段 ==========
    private String commoditySnapshotTitle;
    private String commoditySnapshotDescription;
    private Double commoditySnapshotPrice;
    private String commoditySnapshotLocation;
    private String commoditySnapshotCategory;
    private String commoditySnapshotConditionLevel;
    private String commoditySnapshotImages; // JSON格式的图片URL列表
    private String commoditySnapshotStatus;
    private String commoditySnapshotSellerName;
    private String commoditySnapshotSellerPhone;
    private String commoditySnapshotSellerEmail;
    private String commoditySnapshotTime;
}
