package com.njumarket.image.controller;

import com.njumarket.image.dto.ImageUploadDTO;
import com.njumarket.image.service.ImageService;
import com.njumarket.njumarket.dto.Result;
import com.njumarket.njumarket.exception.BusinessException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * 用户侧图片上传控制器
 *
 * <p>路径前缀 /api/user/image/** 会经过 Gateway 的 JwtAuthenticationFilter，
 * userId 由 Gateway 从 session 中提取后注入 X-User-Id 请求头，无需前端传参。
 */
@Tag(name = "用户图片上传", description = "用户头像上传接口")
@Slf4j
@RestController
@RequestMapping("/api/user/image")
@RequiredArgsConstructor
public class UserImageController {

    private final ImageService imageService;

    @Operation(summary = "上传头像", description = "上传当前用户的头像，返回可访问的图片 URL")
    @PostMapping(value = "/upload-avatar", consumes = "multipart/form-data")
    public Result uploadAvatar(
            @Parameter(hidden = true) @RequestHeader(value = "X-User-Id", required = false) String userId,
            @Parameter(description = "头像文件", required = true)
            @RequestParam("file") MultipartFile file) {
        if (!StringUtils.hasText(userId)) {
            throw new BusinessException("用户未登录");
        }
        try {
            ImageUploadDTO result = imageService.uploadAvatar(userId, file);
            if (!result.isSuccess()) {
                throw new BusinessException(result.getMessage() != null ? result.getMessage() : "头像上传失败");
            }
            return Result.ok("头像上传成功", result);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("上传头像失败: userId={}, error={}", userId, e.getMessage(), e);
            throw new BusinessException("头像上传失败: " + e.getMessage());
        }
    }
}
