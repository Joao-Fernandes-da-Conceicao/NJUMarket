package com.njumarket.njumarket.controller.admin;

import com.njumarket.njumarket.dto.Result;
import com.njumarket.njumarket.dto.PromotionDTO;
import com.njumarket.njumarket.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 管理员-促销活动控制器
 */
@RestController
@RequestMapping("/api/admin/promotion")
@RequiredArgsConstructor
public class AdminPromotionController {

    private final AdminService adminService;

    /**
     * 创建促销活动
     */
    @PostMapping("/create")
    public Result createPromotion(@RequestBody PromotionDTO promotionDTO) {
        return adminService.createPromotion(promotionDTO);
    }

    /**
     * 获取促销活动列表
     */
    @GetMapping("/list")
    public Result getPromotions(@RequestParam(defaultValue = "1") Integer page,
                               @RequestParam(defaultValue = "10") Integer size,
                               @RequestParam(required = false) String status) {
        return adminService.getPromotions(page, size, status);
    }

    /**
     * 更新促销活动
     */
    @PutMapping("/{promotionId}")
    public Result updatePromotion(@PathVariable String promotionId,
                                @RequestBody PromotionDTO promotionDTO) {
        return adminService.updatePromotion(promotionId, promotionDTO);
    }

    /**
     * 激活促销活动
     */
    @PostMapping("/{promotionId}/activate")
    public Result activatePromotion(@PathVariable String promotionId) {
        return adminService.activatePromotion(promotionId);
    }

    /**
     * 停用促销活动
     */
    @PostMapping("/{promotionId}/deactivate")
    public Result deactivatePromotion(@PathVariable String promotionId) {
        return adminService.deactivatePromotion(promotionId);
    }

    /**
     * 删除促销活动
     */
    @DeleteMapping("/{promotionId}")
    public Result deletePromotion(@PathVariable String promotionId) {
        return adminService.deletePromotion(promotionId);
    }

    /**
     * 获取促销活动统计
     */
    @GetMapping("/{promotionId}/statistics")
    public Result getPromotionStatistics(@PathVariable String promotionId) {
        return adminService.getPromotionStatistics(promotionId);
    }
}
