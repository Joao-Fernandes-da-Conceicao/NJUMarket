package com.njumarket.order.client;

import com.njumarket.njumarket.dto.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

/**
 * Commodity Query Service Feign Client
 * 用于Order Service调用Commodity Service的查询功能（公开接口和用户接口）
 */
@FeignClient(name = "njumarket-service-commodity", contextId = "commodityQueryClient", path = "/api/public")
public interface CommodityQueryClient {
    
    /**
     * 根据ID查询商品详情（公开接口）
     */
    @GetMapping("/commodity/{commodityId}")
    Result getCommodityById(@PathVariable String commodityId);
    
    /**
     * 批量查询商品状态（用户接口，用于聊天界面）
     * 注意：使用完整路径覆盖默认的 path = "/api/public"
     */
    @PostMapping("/api/user/commodity/batch-status")
    Result getCommoditiesBatchStatus(@RequestBody List<String> commodityIds);
}
