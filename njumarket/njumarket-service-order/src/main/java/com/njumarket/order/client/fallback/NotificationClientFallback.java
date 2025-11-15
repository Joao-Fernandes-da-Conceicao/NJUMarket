package com.njumarket.order.client.fallback;

import com.njumarket.njumarket.dto.Result;
import com.njumarket.order.client.NotificationClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Notification Client Fallback
 * 当通知服务不可用时的降级处理
 * 注意：通知失败不应影响订单主流程，因此返回成功但记录日志
 */
@Slf4j
@Component
public class NotificationClientFallback implements NotificationClient {
    
    @Override
    public Result recordOrderChange(String orderId, String operation, String timestamp) {
        log.warn("通知服务不可用，触发熔断降级: orderId={}, operation={}, timestamp={}", 
            orderId, operation, timestamp);
        // 变更记录失败不影响订单流程，返回成功但记录日志
        log.warn("订单变更记录失败，但不影响订单流程");
        return Result.ok("变更记录失败，但不影响订单流程");
    }
    
    @Override
    public Result pushOrderChange(String userId, String orderId, String operation) {
        log.warn("通知服务不可用，触发熔断降级: userId={}, orderId={}, operation={}", 
            userId, orderId, operation);
        // 推送失败不影响订单流程，返回成功但记录日志
        log.warn("订单变更推送失败，但不影响订单流程");
        return Result.ok("推送失败，但不影响订单流程");
    }
}

