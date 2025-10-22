package com.njumarket.njumarket.controller.user;

import com.njumarket.njumarket.dto.Result;
import com.njumarket.njumarket.dto.CommodityDTO;
import com.njumarket.njumarket.service.CommodityService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * 用户商品控制器（卖家功能）
 */
@RestController
@RequestMapping("/api/user/commodity")
@RequiredArgsConstructor
public class UserCommodityController {

    private final CommodityService commodityService;

    /**
     * 发布商品
     */
    @PostMapping("/publish")
    public Result publishCommodity(@RequestBody CommodityDTO commodityDTO) {
        return commodityService.publishCommodity(commodityDTO);
    }

    /**
     * 获取我发布的商品
     */
    @GetMapping("/my")
    public Result getMyCommodities(@RequestParam(defaultValue = "1") Integer page,
                                  @RequestParam(defaultValue = "10") Integer size,
                                  @RequestParam(required = false) String status) {
        return commodityService.getMyCommodities(page, size, status);
    }

    /**
     * 更新商品信息
     */
    @PutMapping("/{commodityId}")
    public Result updateCommodity(@PathVariable String commodityId,
                                @RequestBody CommodityDTO commodityDTO) {
        return commodityService.updateCommodity(commodityId, commodityDTO);
    }

    /**
     * 下架商品
     */
    @PostMapping("/{commodityId}/remove")
    public Result removeCommodity(@PathVariable String commodityId) {
        return commodityService.removeCommodity(commodityId);
    }

    /**
     * 重新上架商品
     */
    @PostMapping("/{commodityId}/republish")
    public Result republishCommodity(@PathVariable String commodityId) {
        return commodityService.republishCommodity(commodityId);
    }

    /**
     * 上传商品图片
     */
    @PostMapping("/upload-image")
    public Result uploadImage(@RequestParam("file") MultipartFile file) {
        return commodityService.uploadImage(file);
    }

    /**
     * 批量操作商品
     */
    @PostMapping("/batch")
    public Result batchOperation(@RequestBody String[] commodityIds,
                               @RequestParam String operation) {
        return commodityService.batchOperation(commodityIds, operation);
    }

    /**
     * 获取商品销售统计
     */
    @GetMapping("/sales-statistics")
    public Result getSalesStatistics(@RequestParam(required = false) String period) {
        return commodityService.getSalesStatistics(period);
    }

    /**
     * 复制商品（基于已有商品创建新商品）
     */
    @PostMapping("/{commodityId}/copy")
    public Result copyCommodity(@PathVariable String commodityId) {
        return commodityService.copyCommodity(commodityId);
    }
}
