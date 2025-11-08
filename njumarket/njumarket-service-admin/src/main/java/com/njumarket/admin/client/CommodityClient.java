package com.njumarket.admin.client;

import com.njumarket.njumarket.dto.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 商品服务Feign客户端（管理端）
 * 统一使用内部API接口，路径前缀为 /api/internal
 */
@FeignClient(name = "njumarket-service-commodity", path = "/api/internal")
public interface CommodityClient {

    /**
     * 查询商品列表（管理端内部接口）
     */
    @GetMapping("/commodities")
    Result listCommodities(@RequestParam(defaultValue = "1") Integer page,
                          @RequestParam(defaultValue = "10") Integer size,
                          @RequestParam(required = false) String keyword,
                          @RequestParam(required = false) String category,
                          @RequestParam(required = false) String conditionLevel,
                          @RequestParam(required = false) String status,
                          @RequestParam(required = false) String sellerVisibility,
                          @RequestParam(required = false) String buyerVisibility,
                          @RequestParam(required = false) String sortProp,
                          @RequestParam(required = false) String sortOrder);

    /**
     * 根据ID查询商品详情（内部接口）
     */
    @GetMapping("/commodity/{commodityId}")
    Result getCommodityById(@PathVariable String commodityId);

    /**
     * 完整更新商品（内部接口）
     */
    @PutMapping("/commodity/{commodityId}/full")
    Result updateCommodityFull(@PathVariable String commodityId,
                               @RequestBody Map<String, Object> payload);

    /**
     * 删除商品（内部接口）
     */
    @DeleteMapping("/commodity/{commodityId}")
    Result deleteCommodity(@PathVariable String commodityId);
}

