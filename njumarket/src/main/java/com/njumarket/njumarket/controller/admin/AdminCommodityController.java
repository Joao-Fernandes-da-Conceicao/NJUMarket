package com.njumarket.njumarket.controller.admin;

import com.njumarket.njumarket.dto.Result;
import com.njumarket.njumarket.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 管理员-商品管理控制器
 */
@RestController
@RequestMapping("/api/admin/commodity")
@RequiredArgsConstructor
public class AdminCommodityController {

    private final AdminService adminService;

    /**
     * 获取待审核商品列表
     */
    @GetMapping("/audit/pending")
    public Result getPendingAudits(@RequestParam(defaultValue = "1") Integer page,
                                  @RequestParam(defaultValue = "10") Integer size) {
        return adminService.getPendingAudits(page, size);
    }

    /**
     * 审核商品
     */
    @PostMapping("/{commodityId}/audit")
    public Result auditCommodity(@PathVariable String commodityId, 
                               @RequestParam String decision,
                               @RequestParam(required = false) String reason) {
        return adminService.auditCommodity(commodityId, decision, reason);
    }

    /**
     * 批量审核商品
     */
    @PostMapping("/audit/batch")
    public Result batchAudit(@RequestBody String[] commodityIds,
                           @RequestParam String decision) {
        return adminService.batchAudit(commodityIds, decision);
    }

    /**
     * 获取商品列表
     */
    @GetMapping("/list")
    public Result getCommodityList(@RequestParam(defaultValue = "1") Integer page,
                                  @RequestParam(defaultValue = "10") Integer size,
                                  @RequestParam(required = false) String status) {
        return adminService.getCommodityList(page, size, status);
    }

    /**
     * 强制下架商品
     */
    @PostMapping("/{commodityId}/remove")
    public Result removeCommodity(@PathVariable String commodityId,
                                @RequestParam String reason) {
        return adminService.removeCommodity(commodityId, reason);
    }

    /**
     * 获取审核记录
     */
    @GetMapping("/audit/records")
    public Result getAuditRecords(@RequestParam(defaultValue = "1") Integer page,
                                 @RequestParam(defaultValue = "10") Integer size) {
        return adminService.getAuditRecords(page, size);
    }
}
