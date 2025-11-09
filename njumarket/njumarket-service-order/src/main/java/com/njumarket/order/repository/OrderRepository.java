package com.njumarket.order.repository;

import com.njumarket.order.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 订单数据访问层
 */
@Repository
public interface OrderRepository extends JpaRepository<Order, String>, JpaSpecificationExecutor<Order> {
    
    /**
     * 根据买家ID查找订单
     */
    List<Order> findByBuyerId(String buyerId);
    
    /**
     * 根据买家ID查找订单（分页）
     */
    Page<Order> findByBuyerId(String buyerId, Pageable pageable);
    
    /**
     * 根据买家ID查找订单（分页，排除买家不可见的订单）
     */
    @Query("SELECT o FROM Order o WHERE o.buyerId = ?1 AND o.buyerVisibility != 'HIDDEN'")
    Page<Order> findByBuyerIdAndBuyerVisibilityNotHidden(String buyerId, Pageable pageable);
    
    /**
     * 根据买家ID和订单状态查找订单（分页）
     */
    Page<Order> findByBuyerIdAndOrderStatus(String buyerId, String orderStatus, Pageable pageable);
    
    /**
     * 根据买家ID和订单状态查找订单（分页，排除买家不可见的订单）
     */
    @Query("SELECT o FROM Order o WHERE o.buyerId = ?1 AND o.orderStatus = ?2 AND o.buyerVisibility != 'HIDDEN'")
    Page<Order> findByBuyerIdAndOrderStatusAndBuyerVisibilityNotHidden(String buyerId, String orderStatus, Pageable pageable);
    
    /**
     * 根据卖家ID查找订单
     */
    List<Order> findBySellerId(String sellerId);
    
    /**
     * 根据卖家ID查找订单（分页）
     */
    Page<Order> findBySellerId(String sellerId, Pageable pageable);
    
    /**
     * 根据卖家ID查找订单（分页，排除卖家不可见的订单）
     */
    @Query("SELECT o FROM Order o WHERE o.sellerId = ?1 AND o.sellerVisibility != 'HIDDEN'")
    Page<Order> findBySellerIdAndSellerVisibilityNotHidden(String sellerId, Pageable pageable);
    
    /**
     * 根据卖家ID和订单状态查找订单（分页）
     */
    Page<Order> findBySellerIdAndOrderStatus(String sellerId, String orderStatus, Pageable pageable);
    
    /**
     * 根据卖家ID和订单状态查找订单（分页，排除卖家不可见的订单）
     */
    @Query("SELECT o FROM Order o WHERE o.sellerId = ?1 AND o.orderStatus = ?2 AND o.sellerVisibility != 'HIDDEN'")
    Page<Order> findBySellerIdAndOrderStatusAndSellerVisibilityNotHidden(String sellerId, String orderStatus, Pageable pageable);
    
    /**
     * 根据商品ID查找订单
     */
    List<Order> findByCommodityId(String commodityId);
    
    /**
     * 根据订单状态查找订单
     */
    List<Order> findByOrderStatus(String orderStatus);
    
    /**
     * 根据卖家可见性查找订单
     */
    List<Order> findBySellerVisibility(String sellerVisibility);
    
    /**
     * 根据买家可见性查找订单
     */
    List<Order> findByBuyerVisibility(String buyerVisibility);
    
    /**
     * 根据卖家可见性和买家可见性查找订单
     */
    List<Order> findBySellerVisibilityAndBuyerVisibility(String sellerVisibility, String buyerVisibility);
    
    /**
     * 根据买家ID和卖家可见性查找订单
     */
    List<Order> findByBuyerIdAndSellerVisibility(String buyerId, String sellerVisibility);
    
    /**
     * 根据买家ID和买家可见性查找订单
     */
    List<Order> findByBuyerIdAndBuyerVisibility(String buyerId, String buyerVisibility);
    
    /**
     * 根据卖家ID和卖家可见性查找订单
     */
    List<Order> findBySellerIdAndSellerVisibility(String sellerId, String sellerVisibility);
    
    /**
     * 根据卖家ID和买家可见性查找订单
     */
    List<Order> findBySellerIdAndBuyerVisibility(String sellerId, String buyerVisibility);
    
    /**
     * 根据订单状态和卖家可见性、买家可见性查找订单
     */
    List<Order> findByOrderStatusAndSellerVisibilityAndBuyerVisibility(String orderStatus, String sellerVisibility, String buyerVisibility);
    
    /**
     * 根据买家ID和订单状态列表查找订单（分页）
     */
    Page<Order> findByBuyerIdAndOrderStatusIn(String buyerId, List<String> orderStatuses, Pageable pageable);
    
    /**
     * 根据卖家ID和订单状态列表查找订单（分页）
     */
    Page<Order> findBySellerIdAndOrderStatusIn(String sellerId, List<String> orderStatuses, Pageable pageable);
    
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

