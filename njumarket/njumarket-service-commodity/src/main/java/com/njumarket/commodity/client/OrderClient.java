package com.njumarket.commodity.client;

import com.njumarket.njumarket.dto.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Order Service Feign Client
 * 用于Commodity Service调用Order Service
 * 启用 Resilience4j 熔断器保护
 */
@FeignClient(
    name = "njumarket-service-order", 
    path = "/api/internal",
    fallback = com.njumarket.commodity.client.fallback.OrderClientFallback.class
)
public interface OrderClient {

    /**
     * 检查商品是否有订单（内部接口）
     */
    @GetMapping("/order/check-commodity/{commodityId}")
    Result checkCommodityHasOrders(@PathVariable String commodityId);
}

