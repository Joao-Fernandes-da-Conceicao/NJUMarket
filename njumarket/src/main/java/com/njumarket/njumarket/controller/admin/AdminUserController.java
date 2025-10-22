package com.njumarket.njumarket.controller.admin;

import com.njumarket.njumarket.dto.Result;
import com.njumarket.njumarket.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 管理员-用户管理控制器
 */
@RestController
@RequestMapping("/api/admin/user")
@RequiredArgsConstructor
public class AdminUserController {

    private final AdminService adminService;

    /**
     * 获取用户列表
     */
    @GetMapping("/list")
    public Result getUserList(@RequestParam(defaultValue = "1") Integer page,
                             @RequestParam(defaultValue = "10") Integer size,
                             @RequestParam(required = false) String status) {
        return adminService.getUserList(page, size, status);
    }

    /**
     * 封禁用户
     */
    @PostMapping("/{userId}/ban")
    public Result banUser(@PathVariable String userId, 
                         @RequestParam String reason,
                         @RequestParam String banType,
                         @RequestParam(required = false) String endTime) {
        return adminService.banUser(userId, reason, banType, endTime);
    }

    /**
     * 解封用户
     */
    @PostMapping("/{userId}/unban")
    public Result unbanUser(@PathVariable String userId) {
        return adminService.unbanUser(userId);
    }

    /**
     * 获取用户详细信息
     */
    @GetMapping("/{userId}")
    public Result getUserDetail(@PathVariable String userId) {
        return adminService.getUserDetail(userId);
    }

    /**
     * 更新用户VIP等级
     */
    @PostMapping("/{userId}/vip")
    public Result updateUserVipLevel(@PathVariable String userId, 
                                   @RequestParam String vipLevel) {
        return adminService.updateUserVipLevel(userId, vipLevel);
    }

    /**
     * 获取封禁记录
     */
    @GetMapping("/ban-records")
    public Result getBanRecords(@RequestParam(defaultValue = "1") Integer page,
                               @RequestParam(defaultValue = "10") Integer size) {
        return adminService.getBanRecords(page, size);
    }
}
