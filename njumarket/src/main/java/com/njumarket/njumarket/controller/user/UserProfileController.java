package com.njumarket.njumarket.controller.user;

import com.njumarket.njumarket.dto.Result;
import com.njumarket.njumarket.dto.UserProfileUpdateDTO;
import com.njumarket.njumarket.service.UserProfileService;
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
    public Result updateCurrentUserProfile(@RequestBody UserProfileUpdateDTO updateDTO) {
        return userProfileService.updateCurrentUserProfile(updateDTO);
    }

    @Operation(summary = "上传头像", description = "上传用户头像图片")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "上传成功"),
        @ApiResponse(responseCode = "400", description = "文件格式错误"),
        @ApiResponse(responseCode = "401", description = "用户未登录")
    })
    @PostMapping("/avatar")
    public Result uploadAvatar(
        @Parameter(description = "头像文件", required = true)
        @RequestParam("file") MultipartFile file) {
        // 图片逻辑暂缓实现
        return Result.fail("头像上传功能暂未实现");
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
}