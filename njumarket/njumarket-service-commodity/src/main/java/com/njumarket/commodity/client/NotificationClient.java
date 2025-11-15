package com.njumarket.commodity.client;

import com.njumarket.njumarket.dto.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Notification Service Feign Client
 * 用于商品服务调用推送服务的推送功能
 * 启用 Resilience4j 熔断器保护
 * 注意：通知失败不应影响商品操作主流程
 */
@FeignClient(
    name = "njumarket-service-notification", 
    contextId = "commodityNotificationClient", 
    path = "/api/internal",
    fallback = com.njumarket.commodity.client.fallback.NotificationClientFallback.class
)
public interface NotificationClient {
    
    /**
     * 记录商品变更
     */
    @PostMapping("/change-record/commodity")
    Result recordCommodityChange(@RequestParam String commodityId,
                                 @RequestParam String operation,
                                 @RequestParam String timestamp);
    
    /**
     * 推送商品变更通知
     */
    @PostMapping("/notification/commodity-change")
    Result pushCommodityChange(@RequestParam String userId,
                              @RequestParam String commodityId,
                              @RequestParam String operation);
}

