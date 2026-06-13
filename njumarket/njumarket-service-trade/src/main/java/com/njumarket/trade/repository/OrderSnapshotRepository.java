package com.njumarket.trade.repository;

import com.njumarket.trade.entity.OrderSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 订单快照数据访问层
 */
@Repository
public interface OrderSnapshotRepository extends JpaRepository<OrderSnapshot, String> {
    
    /**
     * 根据原始订单ID查找快照
     */
    List<OrderSnapshot> findByOriginalOrderId(String originalOrderId);
    
    /**
     * 根据买家ID查找快照
     */
    List<OrderSnapshot> findByBuyerId(String buyerId);
    
    /**
     * 根据卖家ID查找快照
     */
    List<OrderSnapshot> findBySellerId(String sellerId);
    
    /**
     * 根据商品快照ID查找订单快照
     */
    List<OrderSnapshot> findByCommoditySnapshotId(String commoditySnapshotId);
}

