package com.njumarket.message.client;

import com.njumarket.njumarket.dto.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

/**
 * Order Service Feign Client
 * 用于Message Service调用Order Service
 * 启用 Resilience4j 熔断器保护
 */
@FeignClient(
    name = "njumarket-service-trade",
    contextId = "messageOrderClient",
    path = "/api/internal",
    fallback = com.njumarket.message.client.fallback.OrderClientFallback.class
)
public interface OrderClient {
    
    /**
     * 根据ID查询订单（内部接口）
     */
    @GetMapping("/order/{orderId}")
    Result getOrderById(@PathVariable String orderId);

    /**
     * 批量查询订单基础信息（内部接口，供增量轮询使用）
     */
    @PostMapping("/orders/batch")
    Result getOrdersBatch(@RequestBody List<String> orderIds);

    /**
     * 批量查询商品基础信息（内部接口，复用 /api/internal 路径）
     */
    @PostMapping("/commodities/batch")
    Result getCommoditiesBatch(@RequestBody List<String> commodityIds);
}

