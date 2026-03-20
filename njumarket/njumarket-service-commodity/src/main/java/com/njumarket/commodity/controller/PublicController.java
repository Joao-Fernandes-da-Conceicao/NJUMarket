package com.njumarket.commodity.controller;

import com.njumarket.njumarket.dto.Result;
import com.njumarket.commodity.service.CommodityQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 公共控制器（无需登录）
 * 重构后使用CommodityQueryService处理所有查询逻辑
 */
@Tag(name = "公共接口", description = "无需登录即可访问的公共接口，主要用于商品浏览")
@RestController
@RequestMapping("/api/public")
@RequiredArgsConstructor
public class PublicController {

    private final CommodityQueryService commodityQueryService;

    @Operation(summary = "搜索商品", description = "根据关键词搜索商品，支持价格、位置、分类等筛选条件")
    @GetMapping("/commodity/search")
    public Result searchCommodities(
            @Parameter(description = "搜索关键词", example = "iPhone") @RequestParam(required = false) String keyword,
            @Parameter(description = "页码", example = "1") @RequestParam(defaultValue = "1") Integer page,
            @Parameter(description = "每页数量", example = "10") @RequestParam(defaultValue = "10") Integer size,
            @Parameter(description = "位置", example = "仙林校区") @RequestParam(required = false) String location,
            @Parameter(description = "最低价格", example = "100.0") @RequestParam(required = false) Double minPrice,
            @Parameter(description = "最高价格", example = "5000.0") @RequestParam(required = false) Double maxPrice,
            @Parameter(description = "商品分类", example = "电子产品") @RequestParam(required = false) String category,
            @Parameter(description = "排序方式", example = "price_asc") @RequestParam(required = false) String sortBy) {
        return commodityQueryService.searchCommodities(keyword, page, size, location, minPrice, maxPrice, category, sortBy);
    }

    @Operation(summary = "获取商品详情", description = "根据商品ID获取商品的详细信息")
    @GetMapping("/commodity/{commodityId}")
    public Result getCommodityDetail(
            @Parameter(description = "商品ID", example = "COMMODITY_123456") @PathVariable String commodityId) {
        return commodityQueryService.getCommodityDetail(commodityId);
    }

    @Operation(summary = "获取热门商品", description = "获取点击量最高的商品列表")
    @GetMapping("/commodity/hot")
    public Result getHotCommodities(@RequestParam(defaultValue = "10") Integer limit) {
        return commodityQueryService.getHotCommodities(limit);
    }

    @Operation(summary = "获取最新商品", description = "获取最新发布的商品列表")
    @GetMapping("/commodity/latest")
    public Result getLatestCommodities(@RequestParam(defaultValue = "10") Integer limit) {
        return commodityQueryService.getLatestCommodities(limit);
    }

    @Operation(summary = "获取商品分类", description = "获取所有可用的商品分类")
    @GetMapping("/commodity/categories")
    public Result getCategories() {
        return commodityQueryService.getCategories();
    }

    @Operation(summary = "按分类获取商品", description = "根据分类获取商品列表")
    @GetMapping("/commodity/category/{category}")
    public Result getCommoditiesByCategory(@PathVariable String category,
                                         @RequestParam(defaultValue = "1") Integer page,
                                         @RequestParam(defaultValue = "10") Integer size) {
        return commodityQueryService.getCommoditiesByCategory(category, page, size);
    }

    @Operation(summary = "获取推荐商品", description = "基于浏览历史获取推荐商品，无需登录")
    @GetMapping("/commodity/recommend")
    public Result getRecommendedCommodities(@RequestParam(required = false) String sessionId,
                                          @RequestParam(defaultValue = "10") Integer limit) {
        return commodityQueryService.getRecommendedCommodities(sessionId, limit);
    }

    @Operation(summary = "记录商品浏览", description = "记录商品浏览，增加点击量")
    @PostMapping("/commodity/{commodityId}/view")
    public Result recordView(@PathVariable String commodityId,
                           @RequestParam(required = false) String sessionId) {
        return commodityQueryService.recordView(commodityId, sessionId);
    }

    @Operation(summary = "获取指定卖家的商品列表", description = "获取指定卖家的商品列表（公开可见，排除草稿），支持状态筛选")
    @GetMapping("/commodity/seller/{sellerId}")
    public Result getSellerCommodities(
            @Parameter(description = "卖家ID", required = true) @PathVariable String sellerId,
            @Parameter(description = "商品状态", example = "all") @RequestParam(required = false, defaultValue = "all") String status,
            @Parameter(description = "页码", example = "1") @RequestParam(defaultValue = "1") Integer page,
            @Parameter(description = "每页数量", example = "10") @RequestParam(defaultValue = "10") Integer size) {
        // 公共接口，user 参数为 null
        return commodityQueryService.getUserCommodities(null, sellerId, status, page, size);
    }
}

