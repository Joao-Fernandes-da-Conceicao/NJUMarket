package com.njumarket.auth.client;

import com.njumarket.njumarket.dto.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * Image Service Feign Client
 * 用于Auth Service调用Image Service
 * 注意：由于Feign Client不支持MultipartFile，需要通过内部接口调用
 */
@FeignClient(name = "njumarket-service-image", path = "/api/internal")
public interface ImageClient {
    
    /**
     * 上传头像（内部接口）
     * 注意：Feign Client不支持直接传递MultipartFile，需要通过内部接口实现
     */
    @PostMapping(value = "/image/upload-avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    Result uploadAvatar(@RequestParam("userId") String userId, 
                           @RequestPart("file") MultipartFile file);
    
    /**
     * 根据头像URL删除头像文件（内部接口）
     */
    @DeleteMapping("/image/avatar-by-url")
    Result deleteAvatarByUrl(@RequestParam String avatarUrl);
}

