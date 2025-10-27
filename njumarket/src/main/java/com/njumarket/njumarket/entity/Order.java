package com.njumarket.njumarket.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
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
    private String shippingAddress;
    
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
    
    // 多对一关系：订单属于某个买家
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "buyer_id", insertable = false, updatable = false)
    private User buyer;
    
    // 多对一关系：订单属于某个卖家
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id", insertable = false, updatable = false)
    private User seller;
    
    // 多对一关系：订单关联某个商品
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "commodity_id", insertable = false, updatable = false)
    private Commodity commodity;
    
    // 一对多关系：订单相关的投诉
    @OneToMany(mappedBy = "relatedOrder", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Complaint> complaints;
    
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
     * 创建商品快照
     * @param commodity 商品实体
     * @param seller 卖家用户实体
     * @return 创建是否成功
     */
    public Boolean createCommoditySnapshot(Commodity commodity, User seller) {
        if (commodity == null || seller == null) {
            return false;
        }
        
        this.commoditySnapshotTitle = commodity.getTitle();
        this.commoditySnapshotDescription = commodity.getDescription();
        this.commoditySnapshotPrice = commodity.getPrice();
        this.commoditySnapshotLocation = commodity.getLocation();
        this.commoditySnapshotCategory = commodity.getCategory();
        this.commoditySnapshotConditionLevel = commodity.getConditionLevel();
        // 只保存第一张图片
        this.commoditySnapshotImages = getFirstImage(commodity.getImages());
        this.commoditySnapshotStatus = commodity.getCommodityStatus();
        this.commoditySnapshotSellerName = seller.getUsername() != null ? seller.getUsername() : seller.getUserId();
        this.commoditySnapshotSellerPhone = seller.getPrimaryPhone(); // 使用主要手机号
        this.commoditySnapshotSellerEmail = null; // 暂时设为null，后续可从ContactInfo获取
        this.commoditySnapshotTime = LocalDateTime.now();
        
        return true;
    }
    
    /**
     * 创建商品快照（带完整卖家信息）
     * @param commodity 商品实体
     * @param sellerName 卖家名称
     * @param sellerPhone 卖家电话
     * @param sellerEmail 卖家邮箱
     * @return 创建是否成功
     */
    public Boolean createCommoditySnapshot(Commodity commodity, String sellerName, String sellerPhone, String sellerEmail) {
        if (commodity == null) {
            return false;
        }
        
        this.commoditySnapshotTitle = commodity.getTitle();
        this.commoditySnapshotDescription = commodity.getDescription();
        this.commoditySnapshotPrice = commodity.getPrice();
        this.commoditySnapshotLocation = commodity.getLocation();
        this.commoditySnapshotCategory = commodity.getCategory();
        this.commoditySnapshotConditionLevel = commodity.getConditionLevel();
        // 只保存第一张图片
        this.commoditySnapshotImages = getFirstImage(commodity.getImages());
        this.commoditySnapshotStatus = commodity.getCommodityStatus();
        this.commoditySnapshotSellerName = sellerName;
        this.commoditySnapshotSellerPhone = sellerPhone;
        this.commoditySnapshotSellerEmail = sellerEmail;
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
     * 修改逻辑：允许已完成、已取消、退款完成状态的订单删除（软删除）
     * 不允许退款请求中状态的订单删除，因为需要处理退款
     * @return 是否可以修改
     */
    public Boolean canModifyVisibility() {
        // 允许删除的状态：已完成、已取消、退款完成
        String[] allowedStatuses = {"COMPLETED", "CANCELLED", "REFUND_APPROVED"};
        for (String status : allowedStatuses) {
            if (status.equals(this.orderStatus)) {
                return true;
            }
        }
        
        // 禁止删除的状态：正在进行退款处理
        String[] blockedStatuses = {"REFUND_REQUESTED", "REFUND_REJECTED"};
        for (String status : blockedStatuses) {
            if (status.equals(this.orderStatus)) {
                return false;
            }
        }
        
        // 其他状态允许修改
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