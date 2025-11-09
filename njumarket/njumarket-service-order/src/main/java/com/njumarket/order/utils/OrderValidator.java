package com.njumarket.order.utils;

import com.njumarket.order.entity.Order;
import com.njumarket.order.repository.OrderRepository;
import com.njumarket.njumarket.exception.BusinessException;
import org.springframework.data.repository.CrudRepository;

/**
 * 订单业务校验工具类
 * 专门用于订单相关的业务校验
 */
public class OrderValidator {
    
    /**
     * 检查订单是否存在
     * @param orderId 订单ID
     * @param repository 订单Repository
     * @return 订单对象
     * @throws BusinessException 如果订单不存在
     */
    public static Order requireOrder(String orderId, CrudRepository<Order, String> repository) {
        return repository.findById(orderId)
            .orElseThrow(() -> new BusinessException("订单不存在"));
    }
    
    /**
     * 检查用户是否为订单买家
     * @param order 订单对象
     * @param userId 用户ID
     * @throws BusinessException 如果用户不是订单买家
     */
    public static void requireBuyer(Order order, String userId) {
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        if (userId == null || !userId.equals(order.getBuyerId())) {
            throw new BusinessException("无权限操作此订单，只有买家可以操作");
        }
    }
    
    /**
     * 检查用户是否为订单卖家
     * @param order 订单对象
     * @param userId 用户ID
     * @throws BusinessException 如果用户不是订单卖家
     */
    public static void requireSeller(Order order, String userId) {
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        if (userId == null || !userId.equals(order.getSellerId())) {
            throw new BusinessException("无权限操作此订单，只有卖家可以操作");
        }
    }
    
    /**
     * 检查用户是否为订单买家或卖家
     * @param order 订单对象
     * @param userId 用户ID
     * @throws BusinessException 如果用户既不是买家也不是卖家
     */
    public static void requireBuyerOrSeller(Order order, String userId) {
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        if (userId == null || (!userId.equals(order.getBuyerId()) && !userId.equals(order.getSellerId()))) {
            throw new BusinessException("无权限操作此订单，只有买卖双方可以操作");
        }
    }
    
    /**
     * 检查订单状态
     * @param order 订单对象
     * @param expectedStatuses 期望的状态（可变参数，支持多个状态）
     * @throws BusinessException 如果订单状态不匹配
     */
    public static void requireOrderStatus(Order order, String... expectedStatuses) {
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        String actualStatus = order.getOrderStatus();
        for (String expectedStatus : expectedStatuses) {
            if (expectedStatus.equals(actualStatus)) {
                return; // 匹配成功
            }
        }
        throw new BusinessException("订单状态不正确，期望: " + String.join(", ", expectedStatuses) + "，实际: " + actualStatus);
    }
}

