package com.njumarket.admin.client;

import com.njumarket.njumarket.dto.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@FeignClient(name = "njumarket-service-commodity")
public interface CommodityClient {

    /**
     * 根据ID查询商品详情（公开接口）
     */
    @GetMapping("/api/public/commodity/{commodityId}")
    Result getCommodityById(@PathVariable String commodityId);

    /**
     * 完整更新商品（内部接口）
     */
    @PutMapping("/api/internal/commodity/{commodityId}/full")
    Result updateCommodityFull(@PathVariable String commodityId,
                               @RequestBody Map<String, Object> payload);

    /**
     * 删除商品（内部接口）
     */
    @DeleteMapping("/api/internal/commodity/{commodityId}")
    Result deleteCommodity(@PathVariable String commodityId);
}

