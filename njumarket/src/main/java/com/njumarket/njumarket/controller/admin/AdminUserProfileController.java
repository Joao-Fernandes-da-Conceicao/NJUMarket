package com.njumarket.njumarket.controller.admin;

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

/**
 * 管理员用户档案控制器
 */
@Tag(name = "管理员-用户档案", description = "管理员用户档案管理相关接口")
@RestController
@RequestMapping("/api/admin/user-profile")
@RequiredArgsConstructor
public class AdminUserProfileController {

    private final UserProfileService userProfileService;

    @Operation(summary = "获取用户档案", description = "管理员获取指定用户的档案信息")
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

    @Operation(summary = "更新用户档案", description = "管理员更新指定用户的档案信息")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "更新成功"),
        @ApiResponse(responseCode = "400", description = "参数错误"),
        @ApiResponse(responseCode = "404", description = "用户档案不存在")
    })
    @PutMapping("/{userId}")
    public Result updateUserProfile(
        @Parameter(description = "用户ID", required = true)
        @PathVariable String userId,
        @RequestBody UserProfileUpdateDTO updateDTO) {
        return userProfileService.updateUserProfile(userId, updateDTO);
    }

    @Operation(summary = "创建用户档案", description = "管理员为指定用户创建档案")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "创建成功"),
        @ApiResponse(responseCode = "400", description = "用户档案已存在或用户不存在")
    })
    @PostMapping("/{userId}")
    public Result createUserProfile(
        @Parameter(description = "用户ID", required = true)
        @PathVariable String userId,
        @Parameter(description = "昵称")
        @RequestParam(required = false) String nickname) {
        return userProfileService.createUserProfile(userId, nickname);
    }

    @Operation(summary = "更新用户评分", description = "管理员更新用户的买家或卖家评分")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "更新成功"),
        @ApiResponse(responseCode = "400", description = "参数错误"),
        @ApiResponse(responseCode = "404", description = "用户档案不存在")
    })
    @PutMapping("/{userId}/rating")
    public Result updateUserRating(
        @Parameter(description = "用户ID", required = true)
        @PathVariable String userId,
        @Parameter(description = "评分", required = true)
        @RequestParam Double rating,
        @Parameter(description = "角色类型", required = true, example = "buyer")
        @RequestParam String role) {
        return userProfileService.updateUserRating(userId, rating, role);
    }

    @Operation(summary = "更新信用分", description = "管理员调整用户的信用分")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "更新成功"),
        @ApiResponse(responseCode = "400", description = "参数错误"),
        @ApiResponse(responseCode = "404", description = "用户档案不存在")
    })
    @PutMapping("/{userId}/credit-score")
    public Result updateCreditScore(
        @Parameter(description = "用户ID", required = true)
        @PathVariable String userId,
        @Parameter(description = "分数变化", required = true)
        @RequestParam Integer scoreChange,
        @Parameter(description = "变更原因")
        @RequestParam(required = false) String reason) {
        return userProfileService.updateCreditScore(userId, scoreChange, reason);
    }

    @Operation(summary = "更新交易统计", description = "管理员更新用户的交易统计数据")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "更新成功"),
        @ApiResponse(responseCode = "400", description = "参数错误"),
        @ApiResponse(responseCode = "404", description = "用户档案不存在")
    })
    @PutMapping("/{userId}/trade-statistics")
    public Result updateTradeStatistics(
        @Parameter(description = "用户ID", required = true)
        @PathVariable String userId,
        @Parameter(description = "交易类型", required = true, example = "sale")
        @RequestParam String type,
        @Parameter(description = "数量", required = true)
        @RequestParam Integer count) {
        return userProfileService.updateTradeStatistics(userId, type, count);
    }

    @Operation(summary = "升级VIP等级", description = "管理员手动升级用户的VIP等级")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "升级成功"),
        @ApiResponse(responseCode = "404", description = "用户档案不存在")
    })
    @PutMapping("/{userId}/upgrade-vip")
    public Result upgradeVipLevel(
        @Parameter(description = "用户ID", required = true)
        @PathVariable String userId) {
        return userProfileService.upgradeVipLevel(userId);
    }

    @Operation(summary = "获取用户排行榜", description = "管理员获取用户排行榜")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "获取成功")
    })
    @GetMapping("/rankings")
    public Result getUserRankings(
        @Parameter(description = "排行榜类型", required = true, example = "seller")
        @RequestParam String type,
        @Parameter(description = "返回数量")
        @RequestParam(defaultValue = "20") Integer limit) {
        return userProfileService.getUserRankings(type, limit);
    }

    @Operation(summary = "搜索用户档案", description = "管理员搜索用户档案")
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
        @RequestParam(defaultValue = "20") Integer size) {
        return userProfileService.searchUserProfiles(keyword, page, size);
    }

    @Operation(summary = "获取VIP等级统计", description = "管理员获取VIP等级统计信息")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "获取成功")
    })
    @GetMapping("/vip-statistics")
    public Result getVipLevelStatistics() {
        return userProfileService.getVipLevelStatistics();
    }
}
