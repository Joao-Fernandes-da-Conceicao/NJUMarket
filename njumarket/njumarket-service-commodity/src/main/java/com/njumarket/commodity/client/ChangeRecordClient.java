package com.njumarket.commodity.client;

import com.njumarket.njumarket.dto.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * Change Record Service Feign Client
 * 用于Commodity Service调用Order Service的变更记录功能
 */
@FeignClient(name = "njumarket-service-order", contextId = "changeRecordClient", path = "/api/internal")
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
     * 记录商品变更
     */
    @PostMapping("/change-record/commodity")
    Result recordCommodityChange(@RequestParam String commodityId,
                                 @RequestParam String operation,
                                 @RequestParam String timestamp);
    
    /**
     * 记录订单变更
     */
    @PostMapping("/change-record/order")
    Result recordOrderChange(@RequestParam String orderId,
                            @RequestParam String operation,
                            @RequestParam String timestamp);
}

