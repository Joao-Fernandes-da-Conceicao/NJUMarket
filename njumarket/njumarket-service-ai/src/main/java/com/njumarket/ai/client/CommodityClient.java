package com.njumarket.ai.client;

import com.njumarket.njumarket.dto.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * Commodity Service Feign Client
 * 用于AI服务调用商品搜索和查询功能
 */
@FeignClient(name = "njumarket-service-trade")
public interface CommodityClient {

    /**
     * 搜索商品（公开接口）
     * 供 Function Calling 工具调用
     */
    @GetMapping("/api/public/commodity/search")
    Result searchCommodities(@RequestParam(required = false) String keyword,
                             @RequestParam(defaultValue = "1") Integer page,
                             @RequestParam(defaultValue = "20") Integer size,
                             @RequestParam(required = false) String location,
                             @RequestParam(required = false) Double minPrice,
                             @RequestParam(required = false) Double maxPrice,
                             @RequestParam(required = false) String category,
                             @RequestParam(required = false) String sortBy);

    /**
     * 根据ID查询商品详情（内部接口）
     */
    @GetMapping("/api/internal/commodity/{commodityId}")
    Result getCommodityById(@PathVariable String commodityId);

    /**
     * 批量查询商品详情（内部接口）
     */
    @PostMapping("/api/internal/commodities/batch")
    Result getCommoditiesByIds(@RequestBody List<String> commodityIds);
}
