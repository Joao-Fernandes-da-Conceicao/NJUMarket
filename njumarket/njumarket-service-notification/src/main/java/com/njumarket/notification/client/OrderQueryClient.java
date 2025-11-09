package com.njumarket.notification.client;

import com.njumarket.njumarket.dto.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

/**
 * Order Query Service Feign Client
 * 用于推送服务调用订单服务的查询功能
 */
@FeignClient(name = "njumarket-service-order", contextId = "notificationOrderQueryClient")
public interface OrderQueryClient {
    
    /**
     * 批量查询订单状态（用户接口，用于聊天界面）
     */
    @PostMapping("/api/user/order/batch-status")
    Result getOrdersBatchStatus(@RequestBody List<String> orderIds);
}

