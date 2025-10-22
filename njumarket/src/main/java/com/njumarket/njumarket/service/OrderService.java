package com.njumarket.njumarket.service;

import com.njumarket.njumarket.dto.Result;
import com.njumarket.njumarket.dto.OrderDTO;

/**
 * 订单服务接口
 */
public interface OrderService {
    
    // ========== 买家功能 ==========
    /**
     * 创建订单
     */
    Result createOrder(OrderDTO orderDTO);
    
    /**
     * 支付订单
     */
    Result payOrder(String orderId);
    
    /**
     * 确认收货
     */
    Result confirmOrder(String orderId);
    
    /**
     * 取消订单
     */
    Result cancelOrder(String orderId, String reason);
    
    /**
     * 申请退款
     */
    Result requestRefund(String orderId, String reason);
    
    /**
     * 获取买家订单列表
     */
    Result getBuyerOrders(Integer page, Integer size, String status);
    
    // ========== 卖家功能 ==========
    /**
     * 发货
     */
    Result shipOrder(String orderId, String trackingNumber);
    
    /**
     * 处理退款申请
     */
    Result handleRefund(String orderId, String decision, String remark);
    
    /**
     * 获取卖家订单列表
     */
    Result getSellerOrders(Integer page, Integer size, String status);
    
    // ========== 通用功能 ==========
    /**
     * 获取订单详情
     */
    Result getOrderDetail(String orderId);
    
    /**
     * 评价订单
     */
    Result rateOrder(String orderId, Integer rating, String comment);
    
    // ========== 内部方法 ==========
    /**
     * 完成订单
     */
    Result completeOrder(String orderId);
    
    /**
     * 申请退货
     */
    Result requestReturn(String orderId);
    
    /**
     * 计算有效交易量
     */
    Integer calcValidVolume(String userId);
    
    /**
     * 获取订单历史
     */
    Result getOrderHistory(String userId);
}
