package com.njumarket.commodity.client.fallback;

import com.njumarket.commodity.client.NotificationClient;
import com.njumarket.njumarket.dto.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * NotificationClient Fallback 实现
 * 当通知服务不可用时，静默失败，不影响商品操作
 */
@Slf4j
@Component
public class NotificationClientFallback implements NotificationClient {
    
    @Override
    public Result recordCommodityChange(String commodityId, String operation, String timestamp) {
        log.warn("通知服务不可用，触发熔断降级: recordCommodityChange, commodityId={}, operation={}", 
            commodityId, operation);
        return Result.ok("通知服务暂时不可用，商品变更记录失败"); // 静默失败，不影响主流程
    }
    
    @Override
    public Result pushCommodityChange(String userId, String commodityId, String operation) {
        log.warn("通知服务不可用，触发熔断降级: pushCommodityChange, userId={}, commodityId={}, operation={}", 
            userId, commodityId, operation);
        return Result.ok("通知服务暂时不可用，商品变更推送失败"); // 静默失败，不影响主流程
    }
}

