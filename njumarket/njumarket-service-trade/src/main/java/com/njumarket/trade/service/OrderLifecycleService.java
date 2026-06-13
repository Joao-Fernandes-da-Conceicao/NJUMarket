package com.njumarket.trade.service;

import com.njumarket.njumarket.dto.Result;
import com.njumarket.trade.dto.OrderDTO;
import com.njumarket.trade.dto.OrderSnapshotDTO;

/**
 * 订单核心生命周期服务
 * 负责订单从创建到完成的所有状态流转写操作
 */
public interface OrderLifecycleService {

    Result createOrder(OrderDTO orderDTO);

    Result payOrder(String orderId);

    Result confirmOrder(String orderId);

    Result cancelOrder(String orderId, String reason);

    Result shipOrder(String orderId, String trackingNumber);

    Result completeOrder(String orderId);

    Result rateOrder(String orderId, Integer rating, String comment);

    /**
     * 基于已有订单快照创建新订单（同商品重复购买）
     */
    Result createOrderFromSnapshot(String orderId, OrderSnapshotDTO orderSnapshotDTO);
}
