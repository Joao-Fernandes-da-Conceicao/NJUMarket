package com.njumarket.order.client;

import com.njumarket.njumarket.dto.Result;
import com.njumarket.order.client.fallback.ImageClientFallback;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * Image Service Feign Client
 * 用于Order Service调用Image Service（未来扩展，如投诉证据图片上传）
 * 启用 Resilience4j 熔断器保护
 */
@FeignClient(name = "njumarket-service-image", 
             path = "/api/internal",
             fallback = ImageClientFallback.class)
public interface ImageClient {
    
    /**
     * 上传图片（内部接口，用于投诉证据等）
     */
    @PostMapping(value = "/image/upload-commodity", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    Result uploadImage(@RequestParam("userId") String userId, 
                      @RequestPart("file") MultipartFile file);
    
    /**
     * 根据图片URL删除图片文件（内部接口）
     */
    @DeleteMapping("/image/commodity-by-url")
    Result deleteImageByUrl(@RequestParam String imageUrl);
}

