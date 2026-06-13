package com.njumarket.trade.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * 订单快照实体类
 * 用于在消息中发送订单信息，即使原订单被删除或修改，快照信息仍然保留
 */
@Entity
@Table(name = "order_snapshots")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderSnapshot {
    
    @Id
    @Column(name = "snapshot_id", length = 50)
    private String snapshotId;
    
    @Column(name = "original_order_id", length = 50)
    private String originalOrderId; // 原始订单ID（可选，用于追溯）
    
    @Column(name = "order_status", length = 20)
    private String orderStatus;
    
    @Column(name = "pay_amount")
    private Double payAmount;
    
    @Column(name = "quantity")
    private Integer quantity;
    
    @Column(name = "shipping_address", columnDefinition = "TEXT")
    private String shippingAddress;
    
    @Column(name = "remark", columnDefinition = "TEXT")
    private String remark;
    
    @Column(name = "commodity_snapshot_id", length = 50)
    private String commoditySnapshotId; // 关联的商品快照ID
    
    @Column(name = "buyer_id", length = 50)
    private String buyerId;
    
    @Column(name = "seller_id", length = 50)
    private String sellerId;
    
    @CreationTimestamp
    @Column(name = "snapshot_time", nullable = false, updatable = false)
    private LocalDateTime snapshotTime;
    
    /**
     * 从订单实体创建快照
     */
    public static OrderSnapshot fromOrder(Order order, String commoditySnapshotId) {
        if (order == null) {
            return null;
        }
        
        OrderSnapshot snapshot = new OrderSnapshot();
        snapshot.setSnapshotId("OS_" + System.currentTimeMillis() + "_" + (int)(Math.random() * 10000));
        snapshot.setOriginalOrderId(order.getOrderId());
        snapshot.setOrderStatus(order.getOrderStatus());
        snapshot.setPayAmount(order.getPayAmount());
        snapshot.setQuantity(order.getQuantity());
        snapshot.setShippingAddress(order.getShippingAddress());
        snapshot.setRemark(order.getRemark());
        snapshot.setBuyerId(order.getBuyerId());
        snapshot.setSellerId(order.getSellerId());
        
        if (commoditySnapshotId != null) {
            snapshot.setCommoditySnapshotId(commoditySnapshotId);
        }
        
        return snapshot;
    }
}

