package com.njumarket.njumarket.repository;

import com.njumarket.njumarket.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 订单数据访问层
 */
@Repository
public interface OrderRepository extends JpaRepository<Order, String> {
    
    /**
     * 根据买家ID查找订单
     */
    List<Order> findByBuyerId(String buyerId);
    
    /**
     * 根据卖家ID查找订单
     */
    List<Order> findBySellerId(String sellerId);
    
    /**
     * 根据商品ID查找订单
     */
    List<Order> findByCommodityId(String commodityId);
    
    /**
     * 根据订单状态查找订单
     */
    List<Order> findByOrderStatus(String orderStatus);
    
    /**
     * 统计用户完成的订单数量
     */
    @Query("SELECT COUNT(o) FROM Order o WHERE (o.buyerId = ?1 OR o.sellerId = ?1) AND o.orderStatus = 'COMPLETED'")
    Long countCompletedOrdersByUserId(String userId);
    
    /**
     * 计算用户总交易金额
     */
    @Query("SELECT SUM(o.payAmount) FROM Order o WHERE (o.buyerId = ?1 OR o.sellerId = ?1) AND o.orderStatus = 'COMPLETED'")
    Double sumCompletedOrderAmountByUserId(String userId);
    
    /**
     * 统计平台总交易额
     */
    @Query("SELECT SUM(o.payAmount) FROM Order o WHERE o.orderStatus = 'COMPLETED'")
    Double sumTotalCompletedAmount();
}
