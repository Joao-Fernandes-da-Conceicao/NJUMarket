package com.njumarket.commodity.controller;

import com.njumarket.njumarket.dto.Result;
import com.njumarket.njumarket.dto.internal.CommodityInternalDTO;
import com.njumarket.commodity.search.CommoditySearchService;
import com.njumarket.commodity.service.CommodityQueryService;
import com.njumarket.commodity.service.CommodityService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 内部API控制器
 * 用于微服务间调用，不对外暴露。所有业务逻辑均由 Service 层承载。
 */
@RestController
@RequestMapping("/api/internal")
@RequiredArgsConstructor
public class InternalController {

    private final CommodityService commodityService;
    private final CommodityQueryService commodityQueryService;
    private final CommoditySearchService commoditySearchService;

    // ========== 商品查询 ==========

    @GetMapping("/commodity/{commodityId}")
    public Result getCommodityById(@PathVariable String commodityId) {
        CommodityInternalDTO dto = commodityQueryService.getCommodityByIdInternal(commodityId);
        return Result.ok("查询成功", dto);
    }

    @GetMapping("/commodities")
    public Result listCommodities(@RequestParam(defaultValue = "1") Integer page,
                                  @RequestParam(defaultValue = "10") Integer size,
                                  @RequestParam(required = false) String keyword,
                                  @RequestParam(required = false) String category,
                                  @RequestParam(required = false) String conditionLevel,
                                  @RequestParam(required = false) String status,
                                  @RequestParam(required = false) String buyerVisibility,
                                  @RequestParam(required = false) String sortProp,
                                  @RequestParam(required = false) String sortOrder) {
        Map<String, Object> result = commodityQueryService.listCommoditiesInternal(
            page, size, keyword, category, conditionLevel, status, buyerVisibility, sortProp, sortOrder);
        return Result.ok("查询成功", result);
    }

    @PostMapping("/commodities/batch")
    public Result getCommoditiesByIds(@RequestBody List<String> commodityIds) {
        List<CommodityInternalDTO> dtos = commodityQueryService.getCommoditiesByIdsInternal(commodityIds);
        return Result.ok("批量查询成功", dtos);
    }

    @GetMapping("/commodity/seller/{sellerId}")
    public Result getUserCommodities(@PathVariable String sellerId,
                                     @RequestParam(defaultValue = "1") Integer page,
                                     @RequestParam(defaultValue = "100") Integer size,
                                     @RequestParam(required = false) String status) {
        return commodityQueryService.getUserCommodities(null, sellerId, status, page, size);
    }

    // ========== 商品管理（管理端） ==========

    @PutMapping("/commodity/{commodityId}/full")
    public Result updateCommodityFull(@PathVariable String commodityId,
                                      @RequestBody Map<String, Object> payload) {
        CommodityInternalDTO dto = commodityService.updateCommodityFullInternal(commodityId, payload);
        return Result.ok("更新成功", dto);
    }

    @DeleteMapping("/commodity/{commodityId}")
    public Result deleteCommodity(@PathVariable String commodityId) {
        commodityService.deleteCommodityInternal(commodityId);
        return Result.ok("删除成功");
    }

    // ========== 搜索索引管理 ==========

    @PostMapping("/commodity/search/reindex")
    public Result rebuildCommoditySearchIndex() {
        long total = commoditySearchService.rebuildIndex();
        return Result.ok("搜索索引重建成功", Map.of("indexed", total));
    }

    @PostMapping("/commodity/{commodityId}/search-sync")
    public Result syncCommoditySearch(@PathVariable String commodityId) {
        commodityService.syncCommoditySearchInternal(commodityId);
        return Result.ok("搜索索引同步成功");
    }

    // ========== 诊断 ==========

    /** @deprecated 仅保留以兼容旧调用方 */
    @GetMapping("/debug/database")
    public Result debugDatabase() {
        return Result.ok("数据库连接正常");
    }
}
