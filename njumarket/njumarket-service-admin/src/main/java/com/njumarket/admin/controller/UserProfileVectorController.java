package com.njumarket.admin.controller;

import com.njumarket.njumarket.dto.Result;
import com.njumarket.admin.service.UserProfileVectorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 用户画像向量管理控制器（管理端）
 * 类似ES索引管理，提供画像向量的CRUD和批量操作
 */
@Tag(name = "AI画像管理", description = "用户画像向量管理相关接口，类似ES索引管理")
@RestController
@RequestMapping("/api/admin/user-profile-vectors")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SYSTEM')") // 只有 system 管理员可以管理画像向量
public class UserProfileVectorController {
    
    private final UserProfileVectorService userProfileVectorService;
    
    @Operation(summary = "重建所有用户画像", description = "为所有用户重新生成画像向量（类似ES的reindex），建议在低峰期执行")
    @PostMapping("/reindex")
    public Result rebuildAllProfiles() {
        return userProfileVectorService.rebuildAllProfiles();
    }
    
    @Operation(summary = "生成/更新用户画像", description = "为指定用户生成或更新画像向量（类似ES的sync）")
    @PostMapping("/generate/{userId}")
    public Result generateProfile(@PathVariable String userId) {
        return userProfileVectorService.generateProfile(userId);
    }
    
    @Operation(summary = "批量生成/更新用户画像", description = "批量为指定用户生成或更新画像向量")
    @PostMapping("/batch-generate")
    public Result batchGenerateProfiles(@RequestBody List<String> userIds) {
        return userProfileVectorService.batchGenerateProfiles(userIds);
    }
    
    @Operation(summary = "删除用户画像", description = "删除指定用户的画像向量")
    @DeleteMapping("/{userId}")
    public Result deleteProfile(@PathVariable String userId) {
        return userProfileVectorService.deleteProfile(userId);
    }
    
    @Operation(summary = "批量删除用户画像", description = "批量删除指定用户的画像向量")
    @DeleteMapping("/batch")
    public Result batchDeleteProfiles(@RequestBody List<String> userIds) {
        return userProfileVectorService.batchDeleteProfiles(userIds);
    }
    
    @Operation(summary = "获取画像详情", description = "获取指定用户的画像向量详情")
    @GetMapping("/{userId}")
    public Result getProfileDetail(@PathVariable String userId) {
        return userProfileVectorService.getProfileDetail(userId);
    }
    
    @Operation(summary = "获取画像统计信息", description = "获取画像向量的统计信息（总数、覆盖率等，类似ES的stats）")
    @GetMapping("/statistics")
    public Result getProfileStatistics() {
        return userProfileVectorService.getProfileStatistics();
    }
    
    @Operation(summary = "获取画像列表", description = "分页获取用户画像向量列表")
    @GetMapping
    public Result getProfileList(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String sortProp,
            @RequestParam(required = false) String sortOrder) {
        return userProfileVectorService.getProfileList(page, size, keyword, sortProp, sortOrder);
    }
}

