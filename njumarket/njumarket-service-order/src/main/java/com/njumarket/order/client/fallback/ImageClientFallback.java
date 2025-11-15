package com.njumarket.order.client.fallback;

import com.njumarket.njumarket.dto.Result;
import com.njumarket.order.client.ImageClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

/**
 * Image Client Fallback
 * 当图片服务不可用时的降级处理
 */
@Slf4j
@Component
public class ImageClientFallback implements ImageClient {
    
    @Override
    public Result uploadImage(String userId, MultipartFile file) {
        log.warn("图片服务不可用，触发熔断降级: userId={}, fileName={}", 
            userId, file != null ? file.getOriginalFilename() : "null");
        return Result.fail("图片服务暂时不可用，请稍后重试");
    }
    
    @Override
    public Result deleteImageByUrl(String imageUrl) {
        log.warn("图片服务不可用，触发熔断降级: imageUrl={}", imageUrl);
        // 图片删除失败不影响主流程，返回成功但记录日志
        log.warn("图片删除失败，但不影响主流程");
        return Result.ok("图片删除失败，但不影响主流程");
    }
}

