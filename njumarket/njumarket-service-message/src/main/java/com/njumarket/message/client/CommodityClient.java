package com.njumarket.message.client;


import com.njumarket.njumarket.dto.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

/**
 * Commodity Service Feign Client
 * 用于Message Service调用Commodity Service
 */
@FeignClient(name = "njumarket-service-commodity", path = "/api/public")
public interface CommodityClient {
    
    /**
     * 根据ID查询商品详情（公开接口）
     */
    @GetMapping("/commodity/{commodityId}")
    Result getCommodityById(@PathVariable String commodityId);
}

