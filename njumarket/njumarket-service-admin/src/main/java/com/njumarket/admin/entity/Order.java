package com.njumarket.admin.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 订单实体类（admin-service 副本）
 * 用于管理端直接访问数据库
 * ⚠️ 注意：移除了跨服务的关联关系，只保留基本字段
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
}

