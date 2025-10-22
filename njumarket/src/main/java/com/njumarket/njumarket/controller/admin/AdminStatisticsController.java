package com.njumarket.njumarket.controller.admin;

import com.njumarket.njumarket.dto.Result;
import com.njumarket.njumarket.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 管理员-数据统计控制器
 */
@RestController
@RequestMapping("/api/admin/statistics")
@RequiredArgsConstructor
public class AdminStatisticsController {

    private final AdminService adminService;

    /**
     * 获取平台概览统计
     */
    @GetMapping("/overview")
    public Result getPlatformOverview() {
        return adminService.getPlatformOverview();
    }

    /**
     * 获取用户统计数据
     */
    @GetMapping("/users")
    public Result getUserStatistics(@RequestParam(required = false) String period) {
        return adminService.getUserStatistics(period);
    }

    /**
     * 获取商品统计数据
     */
    @GetMapping("/commodities")
    public Result getCommodityStatistics(@RequestParam(required = false) String period) {
        return adminService.getCommodityStatistics(period);
    }

    /**
     * 获取订单统计数据
     */
    @GetMapping("/orders")
    public Result getOrderStatistics(@RequestParam(required = false) String period) {
        return adminService.getOrderStatistics(period);
    }

    /**
     * 获取交易额统计
     */
    @GetMapping("/revenue")
    public Result getRevenueStatistics(@RequestParam(required = false) String period,
                                     @RequestParam(required = false) String category) {
        return adminService.getRevenueStatistics(period, category);
    }

    /**
     * 导出统计报表
     */
    @GetMapping("/export")
    public Result exportStatistics(@RequestParam String type,
                                 @RequestParam(required = false) String startDate,
                                 @RequestParam(required = false) String endDate) {
        return adminService.exportStatistics(type, startDate, endDate);
    }

    /**
     * 获取热门商品排行
     */
    @GetMapping("/hot-commodities")
    public Result getHotCommodities(@RequestParam(defaultValue = "10") Integer limit) {
        return adminService.getHotCommodities(limit);
    }

    /**
     * 获取活跃用户排行
     */
    @GetMapping("/active-users")
    public Result getActiveUsers(@RequestParam(defaultValue = "10") Integer limit) {
        return adminService.getActiveUsers(limit);
    }
}
