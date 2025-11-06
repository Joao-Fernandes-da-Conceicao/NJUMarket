package com.njumarket.njumarket.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

/**
 * 订单数据传输对象
 */
@Data
public class OrderDTO {
    private String orderId;
    private String buyerId;
    private String sellerId;
    
    @NotBlank(message = "商品ID不能为空")
    private String commodityId;
    
    private String orderStatus;
    private String sellerVisibility;
    private String buyerVisibility;
    
    @NotNull(message = "支付金额不能为空")
    @DecimalMin(value = "0.01", message = "支付金额必须大于0")
    private Double payAmount;
    
    @NotNull(message = "购买数量不能为空")
    @Min(value = 1, message = "购买数量必须大于0")
    private Integer quantity;
    
    @NotBlank(message = "收货地址不能为空")
    private String shippingAddress;
    private String trackingNumber;
    private String remark;
    private String createTime; // 订单创建时间
    private String payTime; // 支付时间
    private String shippingTime; // 发货时间
    private String deliveryTime; // 签收时间
    
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
    
    // ========== Profile字段（直接字段，类似商品） ==========
    private String sellerNickname; // 卖家昵称（批量查询优化）
    private String sellerAvatar;   // 卖家头像（批量查询优化）
    private String buyerNickname;  // 买家昵称（批量查询优化）
    private String buyerAvatar;    // 买家头像（批量查询优化）
    
    // ========== 卖家详细信息（保留用于详情接口，批量查询不返回） ==========
    private SellerInfo seller;
    private BuyerInfo buyer;
    
    // 卖家信息内部类
    @Data
    public static class SellerInfo {
        private String userId;
        private String username;
        private String nickname;
        private String avatar;
        private String phone;
        private String email;
        private Boolean isDeleted;
        private String status; // ACTIVE, DELETED
    }
    
    // 买家信息内部类
    @Data
    public static class BuyerInfo {
        private String userId;
        private String username;
        private String nickname;
        private String avatar;
        private String phone;
        private String email;
        private Boolean isDeleted;
        private String status; // ACTIVE, DELETED
    }
}
