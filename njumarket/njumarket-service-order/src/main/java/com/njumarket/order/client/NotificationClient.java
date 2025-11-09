package com.njumarket.order.client;

import com.njumarket.njumarket.dto.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Notification Service Feign Client
 * 用于订单服务调用推送服务的推送功能
 */
@FeignClient(name = "njumarket-service-notification", contextId = "orderNotificationClient", path = "/api/internal")
public interface NotificationClient {
    
    /**
     * 记录订单变更
     */
    @PostMapping("/change-record/order")
    Result recordOrderChange(@RequestParam String orderId,
                            @RequestParam String operation,
                            @RequestParam String timestamp);
    
    /**
     * 推送订单变更通知
     */
    @PostMapping("/notification/order-change")
    Result pushOrderChange(@RequestParam String userId,
                          @RequestParam String orderId,
                          @RequestParam String operation);
}

