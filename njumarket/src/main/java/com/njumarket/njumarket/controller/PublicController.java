package com.njumarket.njumarket.controller;

import com.njumarket.njumarket.dto.Result;
import com.njumarket.njumarket.service.CommodityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 公共控制器（无需登录）
 */
@Tag(name = "公共接口", description = "无需登录即可访问的公共接口，主要用于商品浏览")
@RestController
@RequestMapping("/api/public")
@RequiredArgsConstructor
public class PublicController {

    private final CommodityService commodityService;

    @Operation(summary = "搜索商品", description = "根据关键词搜索商品，支持价格、位置、分类等筛选条件")
    @GetMapping("/commodity/search")
    public Result searchCommodities(
            @Parameter(description = "搜索关键词", example = "iPhone") @RequestParam String keyword,
            @Parameter(description = "页码", example = "1") @RequestParam(defaultValue = "1") Integer page,
            @Parameter(description = "每页数量", example = "10") @RequestParam(defaultValue = "10") Integer size,
            @Parameter(description = "位置", example = "仙林校区") @RequestParam(required = false) String location,
            @Parameter(description = "最低价格", example = "100.0") @RequestParam(required = false) Double minPrice,
            @Parameter(description = "最高价格", example = "5000.0") @RequestParam(required = false) Double maxPrice,
            @Parameter(description = "商品分类", example = "电子产品") @RequestParam(required = false) String category) {
        return commodityService.searchCommodities(keyword, page, size, location, minPrice, maxPrice, category);
    }

    @Operation(summary = "AI语义搜索", description = "使用AI进行智能商品搜索和推荐")
    @GetMapping("/commodity/ai-search")
    public Result aiSearch(
            @Parameter(description = "搜索查询", example = "我想买一个性价比高的手机") @RequestParam String query,
            @Parameter(description = "位置偏好", example = "仙林校区") @RequestParam(required = false) String location) {
        return commodityService.aiSearch(query, location);
    }

    @Operation(summary = "获取商品详情", description = "根据商品ID获取商品的详细信息")
    @GetMapping("/commodity/{commodityId}")
    public Result getCommodityDetail(
            @Parameter(description = "商品ID", example = "COMMODITY_123456") @PathVariable String commodityId) {
        return commodityService.getCommodityDetail(commodityId);
    }

    /**
     * 获取热门商品
     */
    @GetMapping("/commodity/hot")
    public Result getHotCommodities(@RequestParam(defaultValue = "10") Integer limit) {
        return commodityService.getHotCommodities(limit);
    }

    /**
     * 获取最新商品
     */
    @GetMapping("/commodity/latest")
    public Result getLatestCommodities(@RequestParam(defaultValue = "10") Integer limit) {
        return commodityService.getLatestCommodities(limit);
    }

    /**
     * 获取商品分类
     */
    @GetMapping("/commodity/categories")
    public Result getCategories() {
        return commodityService.getCategories();
    }

    /**
     * 按分类获取商品
     */
    @GetMapping("/commodity/category/{category}")
    public Result getCommoditiesByCategory(@PathVariable String category,
                                         @RequestParam(defaultValue = "1") Integer page,
                                         @RequestParam(defaultValue = "10") Integer size) {
        return commodityService.getCommoditiesByCategory(category, page, size);
    }

    /**
     * 获取推荐商品（基于浏览历史，无需登录）
     */
    @GetMapping("/commodity/recommend")
    public Result getRecommendedCommodities(@RequestParam(required = false) String sessionId,
                                          @RequestParam(defaultValue = "10") Integer limit) {
        return commodityService.getRecommendedCommodities(sessionId, limit);
    }

    /**
     * 记录商品浏览（增加点击量）
     */
    @PostMapping("/commodity/{commodityId}/view")
    public Result recordView(@PathVariable String commodityId,
                           @RequestParam(required = false) String sessionId) {
        return commodityService.recordView(commodityId, sessionId);
    }
}
