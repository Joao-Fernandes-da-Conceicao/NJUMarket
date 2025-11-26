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
@FeignClient(name = "njumarket-service-commodity", path = "/api/internal")
public interface CommodityClient {

    /**
     * AI语义搜索（内部接口）
     * @param query 搜索查询
     * @param location 位置偏好
     * @param limit 返回数量限制
     * @param userId 用户ID（用于过滤自己的商品）
     * @return 商品列表
     */
    @GetMapping("/commodity/ai-search")
    Result aiSearch(@RequestParam String query, 
                   @RequestParam(required = false) String location,
                   @RequestParam(required = false) Integer limit,
                   @RequestParam(required = false) String userId);
    
    /**
     * 根据ID查询商品详情（内部接口）
     * @param commodityId 商品ID
     * @return 商品详情
     */
    @GetMapping("/commodity/{commodityId}")
    Result getCommodityById(@PathVariable String commodityId);
    
    /**
     * 批量查询商品详情（内部接口）
     * 用于根据商品ID列表批量获取商品信息
     * @param commodityIds 商品ID列表
     * @return 商品列表
     */
    @PostMapping("/commodities/batch")
    Result getCommoditiesByIds(@RequestBody List<String> commodityIds);
}

