package com.njumarket.order.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 订单实体类
 * 管理买卖双方的交易订单
 */
@Entity
@Table(name = "orders")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Order {
    
    @Id
    @Column(name = "order_id", length = 50)
    private String orderId;
    
    @Column(name = "buyer_id", length = 50, nullable = false)
    private String buyerId;
    
    @Column(name = "seller_id", length = 50, nullable = false)
    private String sellerId;
    
    @Column(name = "commodity_id", length = 50, nullable = false)
    private String commodityId;
    
    @Column(name = "order_status", length = 20, nullable = false)
    private String orderStatus; // CREATED, PAID, SHIPPED, COMPLETED, CANCELLED, REFUNDED, RETURN_REQUESTED, RETURN_APPROVED, RETURN_REJECTED, RETURN_COMPLETED
    
    @Column(name = "seller_visibility", length = 20, nullable = false)
    private String sellerVisibility = "PUBLIC"; // PUBLIC, PRIVATE, HIDDEN
    
    @Column(name = "buyer_visibility", length = 20, nullable = false)
    private String buyerVisibility = "PUBLIC"; // PUBLIC, PRIVATE, HIDDEN
    
    @Column(name = "pay_amount", nullable = false)
    private Double payAmount;
    
    @CreationTimestamp
    @Column(name = "create_time", nullable = false)
    private LocalDateTime createTime;
    
    @Column(name = "pay_time")
    private LocalDateTime payTime;
    
    @Column(name = "shipping_time")
    private LocalDateTime shippingTime;
    
    @Column(name = "delivery_time")
    private LocalDateTime deliveryTime;
    
    @Column(name = "tracking_number", length = 100)
    private String trackingNumber;
    
    @Column(name = "shipping_address", columnDefinition = "TEXT")
    private String shippingAddress; // 保留原有字段，用于兼容
    
    // ========== 地址相关字段 ==========
    @Column(name = "shipping_address_id", length = 50)
    private String shippingAddressId; // 引用用户地址表
    
    // 地址快照字段（保存下单时的地址信息）
    @Column(name = "shipping_address_snapshot_province", length = 50)
    private String shippingAddressSnapshotProvince;
    
    @Column(name = "shipping_address_snapshot_city", length = 50)
    private String shippingAddressSnapshotCity;
    
    @Column(name = "shipping_address_snapshot_district", length = 50)
    private String shippingAddressSnapshotDistrict;
    
    @Column(name = "shipping_address_snapshot_street", length = 200)
    private String shippingAddressSnapshotStreet;
    
    @Column(name = "shipping_address_snapshot_detail", length = 500)
    private String shippingAddressSnapshotDetail;
    
    @Column(name = "shipping_address_snapshot_full", columnDefinition = "TEXT")
    private String shippingAddressSnapshotFull;
    
    @Column(name = "shipping_address_snapshot_recipient_name", length = 100)
    private String shippingAddressSnapshotRecipientName;
    
    @Column(name = "shipping_address_snapshot_recipient_phone", length = 20)
    private String shippingAddressSnapshotRecipientPhone;
    
    @Column(name = "remark", columnDefinition = "TEXT")
    private String remark;
    
    // 退货相关字段
    @Column(name = "return_reason", columnDefinition = "TEXT")
    private String returnReason; // 退货原因
    
    @Column(name = "return_request_time")
    private LocalDateTime returnRequestTime; // 退货申请时间
    
    @Column(name = "return_approval_time")
    private LocalDateTime returnApprovalTime; // 退货审批时间
    
    @Column(name = "return_rejection_reason", columnDefinition = "TEXT")
    private String returnRejectionReason; // 退货拒绝原因
    
    @Column(name = "return_tracking_number", length = 100)
    private String returnTrackingNumber; // 退货快递单号
    
    @Column(name = "return_completion_time")
    private LocalDateTime returnCompletionTime; // 退货完成时间
    
    @Column(name = "quantity", nullable = false)
    private Integer quantity = 1;
    
    // ========== 商品快照字段 ==========
    @Column(name = "commodity_snapshot_title", length = 200)
    private String commoditySnapshotTitle;
    
    @Column(name = "commodity_snapshot_description", columnDefinition = "TEXT")
    private String commoditySnapshotDescription;
    
    @Column(name = "commodity_snapshot_price")
    private Double commoditySnapshotPrice;
    
    @Column(name = "commodity_snapshot_location", length = 200)
    private String commoditySnapshotLocation;
    
    // 商品地址快照字段（保存下单时商品的地址信息）
    @Column(name = "commodity_snapshot_address_province", length = 50)
    private String commoditySnapshotAddressProvince;
    
    @Column(name = "commodity_snapshot_address_city", length = 50)
    private String commoditySnapshotAddressCity;
    
    @Column(name = "commodity_snapshot_address_district", length = 50)
    private String commoditySnapshotAddressDistrict;
    
    @Column(name = "commodity_snapshot_address_street", length = 200)
    private String commoditySnapshotAddressStreet;
    
    @Column(name = "commodity_snapshot_address_detail", length = 500)
    private String commoditySnapshotAddressDetail;
    
    @Column(name = "commodity_snapshot_address_full", columnDefinition = "TEXT")
    private String commoditySnapshotAddressFull;
    
    @Column(name = "commodity_snapshot_category", length = 50)
    private String commoditySnapshotCategory;
    
    @Column(name = "commodity_snapshot_condition_level", length = 20)
    private String commoditySnapshotConditionLevel;
    
    @Column(name = "commodity_snapshot_images", columnDefinition = "TEXT")
    private String commoditySnapshotImages; // JSON格式的图片URL列表
    
    @Column(name = "commodity_snapshot_status", length = 20)
    private String commoditySnapshotStatus;
    
    @Column(name = "commodity_snapshot_seller_name", length = 100)
    private String commoditySnapshotSellerName;
    
    @Column(name = "commodity_snapshot_seller_phone", length = 20)
    private String commoditySnapshotSellerPhone;
    
    @Column(name = "commodity_snapshot_seller_email", length = 100)
    private String commoditySnapshotSellerEmail;
    
    @Column(name = "commodity_snapshot_time")
    private LocalDateTime commoditySnapshotTime;
    
    /**
     * 创建订单
     * @return 创建是否成功
     */
    public Boolean createOrder() {
        this.orderStatus = "CREATED";
        this.createTime = LocalDateTime.now();
        return true;
    }
    
    /**
     * 修改订单信息
     * @param fields 要修改的字段
     * @return 修改是否成功
     */
    public Boolean modifyOrder(Map<String, Object> fields) {
        // 业务逻辑：只有在特定状态下才能修改订单
        if ("CREATED".equals(this.orderStatus)) {
            // 更新字段逻辑
            return true;
        }
        return false;
    }
    
    /**
     * 取消订单
     * @return 取消是否成功
     */
    public Boolean cancelOrder() {
        if ("CREATED".equals(this.orderStatus) || "PAID".equals(this.orderStatus)) {
            this.orderStatus = "CANCELLED";
            return true;
        }
        return false;
    }
    
    /**
     * 支付订单
     * @return 支付是否成功
     */
    public Boolean payOrder() {
        if ("CREATED".equals(this.orderStatus)) {
            this.orderStatus = "PAID";
            this.payTime = LocalDateTime.now();
            return true;
        }
        return false;
    }
    
    /**
     * 发货
     * @param trackingNumber 快递单号
     * @return 发货是否成功
     */
    public Boolean shipOrder(String trackingNumber) {
        if ("PAID".equals(this.orderStatus)) {
            this.orderStatus = "SHIPPED";
            this.shippingTime = LocalDateTime.now();
            this.trackingNumber = trackingNumber;
            return true;
        }
        return false;
    }
    
    /**
     * 完成订单（买家签收）
     * @return 完成是否成功
     */
    public Boolean completeOrder() {
        if ("SHIPPED".equals(this.orderStatus)) {
            this.orderStatus = "COMPLETED";
            this.deliveryTime = LocalDateTime.now();
            return true;
        }
        return false;
    }
    
    /**
     * 获取第一张图片（从逗号分隔的字符串中提取）
     * @param imagesStr 逗号分隔的图片URL列表，例如："image1.jpg,image2.jpg,image3.jpg"
     * @return 第一张图片的URL，如果没有则返回空字符串
     */
    private String getFirstImage(String imagesStr) {
        if (imagesStr == null || imagesStr.isEmpty()) {
            return "";
        }
        
        // 按逗号分割，取第一张图片
        String[] images = imagesStr.split(",");
        if (images.length > 0) {
            return images[0].trim(); // 去掉首尾空格
        }
        
        return "";
    }
    
    /**
     * 创建商品快照（使用商品信息和卖家信息）
     * @param commodityTitle 商品标题
     * @param commodityDescription 商品描述
     * @param commodityPrice 商品价格
     * @param commodityLocation 商品位置（兼容字段）
     * @param commodityCategory 商品分类
     * @param commodityConditionLevel 商品成色
     * @param commodityImages 商品图片
     * @param commodityStatus 商品状态
     * @param sellerName 卖家名称
     * @param sellerPhone 卖家电话
     * @param sellerEmail 卖家邮箱
     * @param commodityAddressProvince 商品地址-省份
     * @param commodityAddressCity 商品地址-城市
     * @param commodityAddressDistrict 商品地址-区/县
     * @param commodityAddressStreet 商品地址-街道
     * @param commodityAddressDetail 商品地址-详细地址
     * @param commodityAddressFull 商品地址-完整地址
     * @return 创建是否成功
     */
    public Boolean createCommoditySnapshot(String commodityTitle, String commodityDescription, 
                                         Double commodityPrice, String commodityLocation,
                                         String commodityCategory, String commodityConditionLevel,
                                         String commodityImages, String commodityStatus,
                                         String sellerName, String sellerPhone, String sellerEmail,
                                         String commodityAddressProvince, String commodityAddressCity,
                                         String commodityAddressDistrict, String commodityAddressStreet,
                                         String commodityAddressDetail, String commodityAddressFull) {
        if (commodityTitle == null) {
            return false;
        }
        
        this.commoditySnapshotTitle = commodityTitle;
        this.commoditySnapshotDescription = commodityDescription;
        this.commoditySnapshotPrice = commodityPrice;
        this.commoditySnapshotLocation = commodityLocation; // 保留兼容字段
        this.commoditySnapshotCategory = commodityCategory;
        this.commoditySnapshotConditionLevel = commodityConditionLevel;
        // 只保存第一张图片
        this.commoditySnapshotImages = getFirstImage(commodityImages);
        this.commoditySnapshotStatus = commodityStatus;
        this.commoditySnapshotSellerName = sellerName;
        this.commoditySnapshotSellerPhone = sellerPhone;
        this.commoditySnapshotSellerEmail = sellerEmail;
        
        // 商品地址快照字段
        this.commoditySnapshotAddressProvince = commodityAddressProvince;
        this.commoditySnapshotAddressCity = commodityAddressCity;
        this.commoditySnapshotAddressDistrict = commodityAddressDistrict;
        this.commoditySnapshotAddressStreet = commodityAddressStreet;
        this.commoditySnapshotAddressDetail = commodityAddressDetail;
        this.commoditySnapshotAddressFull = commodityAddressFull;
        
        this.commoditySnapshotTime = LocalDateTime.now();
        
        return true;
    }
    
    /**
     * 检查订单是否可以恢复库存
     * 只有未发货和未付款的订单取消时才恢复库存
     * @return 是否可以恢复库存
     */
    public Boolean canRestoreStock() {
        return "CREATED".equals(this.orderStatus) || "PAID".equals(this.orderStatus);
    }
    
    /**
     * 检查商品快照是否已下架
     * @return 是否已下架
     */
    public Boolean isCommoditySnapshotOffShelf() {
        return "OFF_SHELF".equals(this.commoditySnapshotStatus) || 
               "DRAFT".equals(this.commoditySnapshotStatus);
    }
    
    /**
     * 设置订单卖家可见性
     * @param sellerVisibility 卖家可见性状态
     * @return 设置是否成功
     */
    public Boolean setSellerVisibility(String sellerVisibility) {
        if ("PUBLIC".equals(sellerVisibility) || "PRIVATE".equals(sellerVisibility) || "HIDDEN".equals(sellerVisibility)) {
            this.sellerVisibility = sellerVisibility;
            return true;
        }
        return false;
    }
    
    /**
     * 设置订单买家可见性
     * @param buyerVisibility 买家可见性状态
     * @return 设置是否成功
     */
    public Boolean setBuyerVisibility(String buyerVisibility) {
        if ("PUBLIC".equals(buyerVisibility) || "PRIVATE".equals(buyerVisibility) || "HIDDEN".equals(buyerVisibility)) {
            this.buyerVisibility = buyerVisibility;
            return true;
        }
        return false;
    }
    
    /**
     * 设置订单可见性（兼容旧接口）
     * @param visibility 可见性状态
     * @return 设置是否成功
     */
    public Boolean setVisibility(String visibility) {
        return setSellerVisibility(visibility) && setBuyerVisibility(visibility);
    }
    
    /**
     * 检查订单对卖家是否可见
     * @return 是否可见
     */
    public Boolean isVisibleToSeller() {
        return "PUBLIC".equals(this.sellerVisibility);
    }
    
    /**
     * 检查订单对买家是否可见
     * @return 是否可见
     */
    public Boolean isVisibleToBuyer() {
        return "PUBLIC".equals(this.buyerVisibility);
    }
    
    /**
     * 检查订单是否可见（兼容旧接口）
     * @return 是否可见
     */
    public Boolean isVisible() {
        return isVisibleToSeller() && isVisibleToBuyer();
    }
    
    /**
     * 检查是否可以修改可见性
     * 修改逻辑：
     * - 禁止删除：待支付、待发货、待收货、退款申请中（需要处理订单或退款）
     * - 允许删除：已完成、已取消、退款完成、退款被拒绝（退款被拒绝后可以删除，但重新申请时会自动恢复可见性）
     * @return 是否可以修改
     */
    public Boolean canModifyVisibility() {
        // 禁止删除的状态：待支付、待发货、待收货、退款申请中
        // 这些状态需要买卖双方处理订单或退款，必须保持可见
        String[] blockedStatuses = {"CREATED", "PAID", "SHIPPED", "REFUND_REQUESTED"};
        for (String status : blockedStatuses) {
            if (status.equals(this.orderStatus)) {
                return false;
            }
        }
        
        // 其他状态允许修改（包括 COMPLETED, CANCELLED, REFUND_APPROVED, REFUND_REJECTED 等）
        return true;
    }
    
    /**
     * 申请退货
     * @param returnReason 退货原因
     * @return 申请是否成功
     */
    public Boolean requestReturn(String returnReason) {
        // 只有已完成的订单可以申请退货
        if ("COMPLETED".equals(this.orderStatus)) {
            this.orderStatus = "RETURN_REQUESTED";
            this.returnReason = returnReason;
            this.returnRequestTime = LocalDateTime.now();
            return true;
        }
        return false;
    }
    
    /**
     * 审批退货申请
     * @param approved 是否同意退货
     * @param rejectionReason 拒绝原因（如果拒绝）
     * @return 审批是否成功
     */
    public Boolean approveReturnRequest(Boolean approved, String rejectionReason) {
        if ("RETURN_REQUESTED".equals(this.orderStatus)) {
            if (approved) {
                this.orderStatus = "RETURN_APPROVED";
                this.returnApprovalTime = LocalDateTime.now();
            } else {
                this.orderStatus = "RETURN_REJECTED";
                this.returnRejectionReason = rejectionReason;
                this.returnApprovalTime = LocalDateTime.now();
            }
            return true;
        }
        return false;
    }
    
    /**
     * 确认退货发货
     * @param returnTrackingNumber 退货快递单号
     * @return 确认是否成功
     */
    public Boolean confirmReturnShipment(String returnTrackingNumber) {
        if ("RETURN_APPROVED".equals(this.orderStatus)) {
            this.returnTrackingNumber = returnTrackingNumber;
            return true;
        }
        return false;
    }
    
    /**
     * 完成退货
     * @return 完成是否成功
     */
    public Boolean completeReturn() {
        if ("RETURN_APPROVED".equals(this.orderStatus) && StringUtils.hasText(this.returnTrackingNumber)) {
            this.orderStatus = "RETURN_COMPLETED";
            this.returnCompletionTime = LocalDateTime.now();
            return true;
        }
        return false;
    }
    
    /**
     * 检查是否可以申请退货
     * @return 是否可以申请退货
     */
    public Boolean canRequestReturn() {
        return "COMPLETED".equals(this.orderStatus);
    }
    
    /**
     * 检查是否可以审批退货
     * @return 是否可以审批退货
     */
    public Boolean canApproveReturn() {
        return "RETURN_REQUESTED".equals(this.orderStatus);
    }
    
    /**
     * 检查是否可以确认退货发货
     * @return 是否可以确认退货发货
     */
    public Boolean canConfirmReturnShipment() {
        return "RETURN_APPROVED".equals(this.orderStatus);
    }
    
    /**
     * 检查是否可以完成退货
     * @return 是否可以完成退货
     */
    public Boolean canCompleteReturn() {
        return "RETURN_APPROVED".equals(this.orderStatus) && StringUtils.hasText(this.returnTrackingNumber);
    }
}

