package com.njumarket.commodity.client;

import com.njumarket.njumarket.dto.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * Image Service Feign Client
 * 用于Commodity Service调用Image Service
 * 启用 Resilience4j 熔断器保护
 */
@FeignClient(
    name = "njumarket-service-image", 
    path = "/api/internal",
    fallback = com.njumarket.commodity.client.fallback.ImageClientFallback.class
)
public interface ImageClient {
    
    /**
     * 上传商品图片（内部接口）
     */
    @PostMapping(value = "/image/upload-commodity", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    Result uploadCommodityImage(@RequestParam("userId") String userId, 
                                @RequestPart("file") MultipartFile file);
    
    /**
     * 为指定商品上传图片（内部接口）
     */
    @PostMapping(value = "/image/upload-commodity/{commodityId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    Result uploadCommodityImageForCommodity(@PathVariable String commodityId,
                                            @RequestPart("file") MultipartFile file);
    
    /**
     * 根据商品图片URL删除商品图片文件（内部接口）
     */
    @DeleteMapping("/image/commodity-by-url")
    Result deleteCommodityImageByUrl(@RequestParam String imageUrl);
}

