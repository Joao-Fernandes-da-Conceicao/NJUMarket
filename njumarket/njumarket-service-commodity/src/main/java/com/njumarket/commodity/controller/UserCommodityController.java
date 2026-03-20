package com.njumarket.commodity.controller;

import com.njumarket.njumarket.dto.Result;
import com.njumarket.commodity.dto.CommodityDTO;
import com.njumarket.commodity.service.CommodityService;
import com.njumarket.commodity.service.CommodityQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 用户商品控制器（卖家功能）
 * 重构后使用CommodityService处理商品管理，CommodityQueryService处理查询
 */
@Slf4j
@Tag(name = "用户商品管理", description = "用户商品管理相关接口，需要登录")
@RestController
@RequestMapping("/api/user/commodity")
@RequiredArgsConstructor
public class UserCommodityController {

    private final CommodityService commodityService;
    private final CommodityQueryService commodityQueryService;

    @Operation(summary = "发布商品", description = "发布新商品")
    @PostMapping("/publish")
    public Result publishCommodity(@Valid @RequestBody CommodityDTO commodityDTO) {
        return commodityService.publishCommodity(commodityDTO);
    }
    
    @Operation(summary = "创建草稿商品", description = "创建草稿商品")
    @PostMapping("/draft")
    public Result createDraftCommodity(@Valid @RequestBody CommodityDTO commodityDTO) {
        return commodityService.createDraftCommodity(commodityDTO);
    }
    
    @Operation(summary = "发布草稿商品", description = "将草稿商品发布为已发布状态")
    @PostMapping("/{commodityId}/publish")
    public Result publishDraftCommodity(@PathVariable String commodityId) {
        return commodityService.publishDraftCommodity(commodityId);
    }

    @Operation(summary = "获取我发布的商品", description = "获取当前用户发布的商品列表")
    @GetMapping("/my")
    public Result getMyCommodities(@RequestParam(defaultValue = "1") Integer page,
                                  @RequestParam(defaultValue = "10") Integer size,
                                  @RequestParam(required = false) String status) {
        return commodityService.getMyCommodities(page, size, status);
    }

    @Operation(summary = "获取我发布的单个商品详情", description = "获取当前用户发布的单个商品详情")
    @GetMapping("/{commodityId}")
    public Result getMyCommodityDetail(@PathVariable String commodityId) {
        return commodityService.getMyCommodityDetail(commodityId);
    }

    @Operation(summary = "更新商品信息", description = "更新商品的基本信息")
    @PutMapping("/{commodityId}")
    public Result updateCommodity(@PathVariable String commodityId,
                                @Valid @RequestBody CommodityDTO commodityDTO) {
        return commodityService.updateCommodity(commodityId, commodityDTO);
    }

    @Operation(summary = "删除商品", description = "删除商品（只有没有下单的商品可以删除）")
    @DeleteMapping("/{commodityId}")
    public Result deleteCommodity(@PathVariable String commodityId) {
        return commodityService.deleteCommodity(commodityId);
    }

    @Operation(summary = "上架商品", description = "将商品上架销售")
    @PostMapping("/{commodityId}/shelf")
    public Result shelfCommodity(@PathVariable String commodityId) {
        return commodityService.shelfCommodity(commodityId);
    }

    @Operation(summary = "下架商品", description = "将商品下架")
    @PostMapping("/{commodityId}/unshelf")
    public Result unshelfCommodity(@PathVariable String commodityId) {
        return commodityService.unshelfCommodity(commodityId);
    }

    @Operation(summary = "设为草稿", description = "将商品设为草稿状态")
    @PostMapping("/{commodityId}/draft")
    public Result draftCommodity(@PathVariable String commodityId) {
        return commodityService.draftCommodity(commodityId);
    }

    @Operation(summary = "重新上架商品", description = "重新上架已下架的商品")
    @PostMapping("/{commodityId}/republish")
    public Result republishCommodity(@PathVariable String commodityId) {
        return commodityService.republishCommodity(commodityId);
    }

    @Operation(summary = "上传商品图片", description = "上传商品图片")
    @PostMapping(value = "/upload-image", consumes = "multipart/form-data")
    public Result uploadImage(@RequestParam("file") MultipartFile file) {
        return commodityService.uploadImage(file);
    }

    @Operation(summary = "为指定商品上传图片", description = "为指定商品上传图片")
    @PostMapping(value = "/{commodityId}/upload-image", consumes = "multipart/form-data")
    public Result uploadCommodityImage(@PathVariable String commodityId,
                                     @RequestParam("file") MultipartFile file) {
        return commodityService.uploadCommodityImage(commodityId, file);
    }

    @Operation(summary = "批量操作商品", description = "批量操作商品（上架、下架、删除等）")
    @PostMapping("/batch")
    public Result batchOperation(@RequestBody String[] commodityIds,
                               @RequestParam String operation) {
        return commodityService.batchOperation(commodityIds, operation);
    }

    @Operation(summary = "获取商品销售统计", description = "获取当前用户的商品销售统计")
    @GetMapping("/sales-statistics")
    public Result getSalesStatistics(@RequestParam(required = false) String period) {
        return commodityService.getSalesStatistics(period);
    }

    @Operation(summary = "复制商品", description = "复制现有商品创建新商品")
    @PostMapping("/{commodityId}/copy")
    public Result copyCommodity(@PathVariable String commodityId) {
        return commodityService.copyCommodity(commodityId);
    }

    @Operation(summary = "修改商品可见性", description = "PUBLIC = 正常可见；HIDDEN = 管理端软隐藏，买家不可见")
    @PutMapping("/{commodityId}/visibility")
    public Result updateCommodityVisibility(@PathVariable String commodityId,
                                          @RequestParam String visibility) {
        return commodityService.updateCommodityVisibility(commodityId, visibility);
    }
    
    // ✅ 批量查询商品状态（用于聊天界面，轻量级查询）
    @Operation(summary = "批量查询商品状态", description = "批量查询商品基本信息，用于聊天界面显示，只返回轻量级信息")
    @PostMapping("/batch-status")
    public Result getCommoditiesBatchStatus(@RequestBody List<String> commodityIds) {
        return commodityQueryService.getCommoditiesBatchStatus(commodityIds);
    }
    
    // ========== 内部接口（用于微服务间调用） ==========
    
    /**
     * 查询商品（带悲观锁，用于创建订单）
     * 内部接口，用于Order Service创建订单时锁定商品
     */
    @Operation(summary = "查询商品（带悲观锁）", description = "内部接口，用于其他服务创建订单时锁定商品")
    @GetMapping("/{commodityId}/for-update")
    public Result getCommodityForUpdate(@PathVariable String commodityId) {
        return commodityService.getCommodityForUpdate(commodityId);
    }
    
    /**
     * 更新商品库存
     * 内部接口，用于Order Service更新商品库存
     */
    @Operation(summary = "更新商品库存", description = "内部接口，用于其他服务更新商品库存")
    @PostMapping("/{commodityId}/update-stock")
    public Result updateCommodityStock(@PathVariable String commodityId, 
                                      @RequestParam Integer quantity) {
        return commodityService.updateCommodityStock(commodityId, quantity);
    }
}

