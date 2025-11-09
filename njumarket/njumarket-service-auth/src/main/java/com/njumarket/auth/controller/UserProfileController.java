package com.njumarket.auth.controller;

import com.njumarket.njumarket.dto.Result;
import com.njumarket.auth.dto.UserProfileUpdateDTO;
import com.njumarket.njumarket.model.IUser;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import com.njumarket.njumarket.exception.BusinessException;
import com.njumarket.auth.service.UserProfileService;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * 用户档案控制器
 */
@Tag(name = "用户档案", description = "用户档案管理相关接口")
@RestController
@RequestMapping("/api/user/profile")
@RequiredArgsConstructor
public class UserProfileController {

    private final UserProfileService userProfileService;

    @Operation(summary = "获取当前用户档案", description = "获取当前登录用户的详细档案信息")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "获取成功"),
        @ApiResponse(responseCode = "401", description = "用户未登录"),
        @ApiResponse(responseCode = "404", description = "用户档案不存在")
    })
    @GetMapping("/me")
    public Result getCurrentUserProfile() {
        return userProfileService.getCurrentUserProfile();
    }

    @Operation(summary = "获取用户档案", description = "根据用户ID获取用户档案信息")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "获取成功"),
        @ApiResponse(responseCode = "404", description = "用户档案不存在")
    })
    @GetMapping("/{userId}")
    public Result getUserProfile(
        @Parameter(description = "用户ID", required = true)
        @PathVariable String userId) {
        return userProfileService.getUserProfile(userId);
    }

    @Operation(summary = "更新当前用户档案", description = "更新当前登录用户的档案信息")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "更新成功"),
        @ApiResponse(responseCode = "400", description = "参数错误"),
        @ApiResponse(responseCode = "401", description = "用户未登录")
    })
    @PutMapping("/me")
    public Result updateCurrentUserProfile(@Valid @RequestBody UserProfileUpdateDTO updateDTO) {
        return userProfileService.updateCurrentUserProfile(updateDTO);
    }

    @Operation(summary = "上传头像", description = "上传用户头像图片")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "上传成功"),
        @ApiResponse(responseCode = "400", description = "文件格式错误"),
        @ApiResponse(responseCode = "401", description = "用户未登录")
    })
    @PostMapping(value = "/avatar", consumes = "multipart/form-data")
    public Result uploadAvatar(
        @AuthenticationPrincipal IUser user,
        @Parameter(description = "头像文件", required = true)
        @RequestParam("file") MultipartFile file) {
        return userProfileService.uploadAvatar(user.getUserId(), file);
    }

    @Operation(summary = "删除头像", description = "删除用户当前头像")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "删除成功"),
        @ApiResponse(responseCode = "401", description = "用户未登录"),
        @ApiResponse(responseCode = "404", description = "用户档案不存在")
    })
    @DeleteMapping("/avatar")
    public Result deleteAvatar(@AuthenticationPrincipal IUser user) {
        return userProfileService.deleteAvatar(user.getUserId());
    }

    @Operation(summary = "搜索用户档案", description = "根据昵称搜索用户档案")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "搜索成功")
    })
    @GetMapping("/search")
    public Result searchUserProfiles(
        @Parameter(description = "搜索关键词", required = true)
        @RequestParam String keyword,
        @Parameter(description = "页码，从0开始")
        @RequestParam(defaultValue = "0") Integer page,
        @Parameter(description = "每页大小")
        @RequestParam(defaultValue = "10") Integer size) {
        return userProfileService.searchUserProfiles(keyword, page, size);
    }

    @Operation(summary = "获取用户排行榜", description = "获取买家或卖家评分排行榜")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "获取成功")
    })
    @GetMapping("/rankings")
    public Result getUserRankings(
        @Parameter(description = "排行榜类型", required = true, example = "seller")
        @RequestParam String type,
        @Parameter(description = "返回数量")
        @RequestParam(defaultValue = "10") Integer limit) {
        return userProfileService.getUserRankings(type, limit);
    }

    @Operation(summary = "获取VIP等级统计", description = "获取各VIP等级的用户数量统计")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "获取成功")
    })
    @GetMapping("/vip-statistics")
    public Result getVipLevelStatistics() {
        return userProfileService.getVipLevelStatistics();
    }
    
    // ✅ v1.3.x: 订单提醒相关接口（向后兼容）
    @Operation(summary = "获取订单提醒状态", description = "获取当前用户的订单提醒状态")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "获取成功"),
        @ApiResponse(responseCode = "401", description = "用户未登录")
    })
    @GetMapping("/order-reminder/status")
    public Result getOrderReminderStatus(@AuthenticationPrincipal IUser user) {
        java.util.Map<String, Boolean> status = userProfileService.getOrderReminderStatus(user.getUserId());
        return Result.ok(status);
    }
    
    @Operation(summary = "清除订单提醒状态", description = "清除指定角色的订单提醒状态（进入订单页面时调用）")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "清除成功"),
        @ApiResponse(responseCode = "400", description = "参数错误"),
        @ApiResponse(responseCode = "401", description = "用户未登录")
    })
    @PostMapping("/order-reminder/clear")
    public Result clearOrderReminder(
        @AuthenticationPrincipal IUser user,
        @Parameter(description = "角色", required = true, example = "SELLER")
        @RequestParam String role) {
        userProfileService.clearOrderReminderStatus(user.getUserId(), role);
        return Result.ok("订单提醒状态已清除");
    }
}

