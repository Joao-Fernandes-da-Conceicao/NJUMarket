package com.njumarket.order.client;

import com.njumarket.njumarket.dto.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * Change Record Service Feign Client
 * 用于Order Service调用Commodity Service的变更记录功能
 */
@FeignClient(name = "njumarket-service-commodity", contextId = "changeRecordClient", path = "/api/internal")
public interface ChangeRecordClient {
    
    /**
     * 获取商品变更记录
     */
    @GetMapping("/change-record/commodity")
    Result getCommodityChangesAfter(@RequestParam String timestamp);
    
    /**
     * 获取订单变更记录
     */
    @GetMapping("/change-record/order")
    Result getOrderChangesAfter(@RequestParam String timestamp);
    
    /**
     * 记录订单变更
     */
    @PostMapping("/change-record/order")
    Result recordOrderChange(@RequestParam String orderId,
                            @RequestParam String operation,
                            @RequestParam String timestamp);
}
