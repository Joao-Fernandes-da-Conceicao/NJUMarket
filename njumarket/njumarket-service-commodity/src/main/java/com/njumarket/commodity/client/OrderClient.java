package com.njumarket.commodity.client;

import com.njumarket.njumarket.dto.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;

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

    /**
     * 同步商品库存到订单服务（商品上架时调用）。
     * body: { "commodityId": "...", "availableQuantity": N, "totalQuantity": N }
     */
    @PostMapping("/inventory/sync")
    Result syncInventory(@RequestBody Map<String, Object> payload);

    /**
     * 按新总量调整库存（ON_SHELF 时卖家改库存后调用）。
     * body: { "commodityId": "...", "newTotalQuantity": N }
     */
    @PostMapping("/inventory/adjust")
    Result adjustInventory(@RequestBody Map<String, Object> payload);

    /**
     * 归零商品库存（下架/草稿时调用）。
     */
    @PostMapping("/inventory/zero/{commodityId}")
    Result zeroInventory(@PathVariable String commodityId);
}

