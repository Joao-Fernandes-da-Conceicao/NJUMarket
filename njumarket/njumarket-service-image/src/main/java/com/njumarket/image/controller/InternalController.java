package com.njumarket.image.controller;

import com.njumarket.njumarket.dto.Result;
import com.njumarket.image.dto.ImageUploadDTO;
import com.njumarket.njumarket.exception.BusinessException;
import com.njumarket.image.service.ImageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * 内部API控制器
 * 用于微服务间调用，不对外暴露
 */
@Slf4j
@RestController
@RequestMapping("/api/internal")
@RequiredArgsConstructor
public class InternalController {
    
    private final ImageService imageService;
    
    /**
     * 上传头像（内部接口）
     */
    @PostMapping("/image/upload-avatar")
    public Result uploadAvatar(@RequestParam("userId") String userId,
                                                @RequestPart("file") MultipartFile file) {
        try {
            ImageUploadDTO result = imageService.uploadAvatar(userId, file);
            if (result.isSuccess()) {
                return Result.ok("头像上传成功", result);
            } else {
                throw new BusinessException(result.getMessage());
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("上传头像失败: userId={}, error={}", userId, e.getMessage(), e);
            throw new BusinessException("上传头像失败: " + e.getMessage());
        }
    }
    
    /**
     * 根据头像URL删除头像文件（内部接口）
     */
    @DeleteMapping("/image/avatar-by-url")
    public Result deleteAvatarByUrl(@RequestParam String avatarUrl) {
        try {
            boolean deleted = imageService.deleteAvatarByUrl(avatarUrl);
            if (deleted) {
                return Result.ok("头像删除成功");
            } else {
                throw new BusinessException("头像删除失败");
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("删除头像失败: avatarUrl={}, error={}", avatarUrl, e.getMessage(), e);
            throw new BusinessException("删除头像失败: " + e.getMessage());
        }
    }
    
    /**
     * 上传商品图片（内部接口）
     */
    @PostMapping("/image/upload-commodity")
    public Result uploadCommodityImage(@RequestParam("userId") String userId,
                                                        @RequestPart("file") MultipartFile file) {
        try {
            ImageUploadDTO result = imageService.uploadCommodityImage(userId, file);
            if (result.isSuccess()) {
                return Result.ok("商品图片上传成功", result);
            } else {
                throw new BusinessException(result.getMessage());
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("上传商品图片失败: userId={}, error={}", userId, e.getMessage(), e);
            throw new BusinessException("上传商品图片失败: " + e.getMessage());
        }
    }
    
    /**
     * 为指定商品上传图片（内部接口）
     */
    @PostMapping("/image/upload-commodity/{commodityId}")
    public Result uploadCommodityImageForCommodity(@PathVariable String commodityId,
                                                                     @RequestPart("file") MultipartFile file) {
        try {
            ImageUploadDTO result = imageService.uploadCommodityImageForCommodity(commodityId, file);
            if (result.isSuccess()) {
                return Result.ok("商品图片上传成功", result);
            } else {
                throw new BusinessException(result.getMessage());
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("上传商品图片失败: commodityId={}, error={}", commodityId, e.getMessage(), e);
            throw new BusinessException("上传商品图片失败: " + e.getMessage());
        }
    }
    
    /**
     * 根据商品图片URL删除商品图片文件（内部接口）
     */
    @DeleteMapping("/image/commodity-by-url")
    public Result deleteCommodityImageByUrl(@RequestParam String imageUrl) {
        try {
            boolean deleted = imageService.deleteCommodityImageByUrl(imageUrl);
            if (deleted) {
                return Result.ok("商品图片删除成功");
            } else {
                throw new BusinessException("商品图片删除失败");
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("删除商品图片失败: imageUrl={}, error={}", imageUrl, e.getMessage(), e);
            throw new BusinessException("删除商品图片失败: " + e.getMessage());
        }
    }
}

