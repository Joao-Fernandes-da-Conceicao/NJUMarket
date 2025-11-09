package com.njumarket.notification.service;

import java.time.LocalDateTime;

/**
 * 变更记录服务接口
 * 用于记录商品和订单的变更，支持增量轮询
 */
public interface ChangeRecordService {
    
    /**
     * 记录商品变更
     * @param commodityId 商品ID
     * @param operation 操作类型（CREATE, UPDATE, SHELF, UNSHELF等）
     * @param timestamp 变更时间戳
     */
    void recordCommodityChange(String commodityId, String operation, LocalDateTime timestamp);
    
    /**
     * 记录订单变更
     * @param orderId 订单ID
     * @param operation 操作类型（CREATE, UPDATE, PAY, SHIP等）
     * @param timestamp 变更时间戳
     */
    void recordOrderChange(String orderId, String operation, LocalDateTime timestamp);
    
    /**
     * 获取指定时间戳之后的所有商品变更
     * @param afterTimestamp 时间戳（只返回此时间之后的变化）
     * @return 变更记录的JSON字符串列表
     */
    java.util.List<String> getCommodityChangesAfter(LocalDateTime afterTimestamp);
    
    /**
     * 获取指定时间戳之后的所有订单变更
     * @param afterTimestamp 时间戳（只返回此时间之后的变化）
     * @return 变更记录的JSON字符串列表
     */
    java.util.List<String> getOrderChangesAfter(LocalDateTime afterTimestamp);
}

