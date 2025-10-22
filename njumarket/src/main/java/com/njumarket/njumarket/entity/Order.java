package com.njumarket.njumarket.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

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
    private String orderStatus; // CREATED, PAID, SHIPPED, COMPLETED, CANCELLED, REFUNDED
    
    @Column(name = "pay_amount", nullable = false)
    private Double payAmount;
    
    @CreationTimestamp
    @Column(name = "create_time", nullable = false)
    private LocalDateTime createTime;
    
    @Column(name = "quantity", nullable = false)
    private Integer quantity = 1;
    
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
            return true;
        }
        return false;
    }
    
    /**
     * 发货
     * @return 发货是否成功
     */
    public Boolean shipOrder() {
        if ("PAID".equals(this.orderStatus)) {
            this.orderStatus = "SHIPPED";
            return true;
        }
        return false;
    }
    
    /**
     * 完成订单
     * @return 完成是否成功
     */
    public Boolean completeOrder() {
        if ("SHIPPED".equals(this.orderStatus)) {
            this.orderStatus = "COMPLETED";
            return true;
        }
        return false;
    }
}
