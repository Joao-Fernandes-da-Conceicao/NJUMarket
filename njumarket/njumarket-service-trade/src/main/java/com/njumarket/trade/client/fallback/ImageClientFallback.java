package com.njumarket.trade.client.fallback;

import com.njumarket.trade.client.ImageClient;
import com.njumarket.njumarket.dto.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

/**
 * ImageClient Fallback 实现
 * 当图片服务不可用时，返回明确的错误信息，阻止图片上传
 */
@Slf4j
@Component
public class ImageClientFallback implements ImageClient {
    
    @Override
    public Result uploadCommodityImage(String userId, MultipartFile file) {
        log.warn("图片服务不可用，触发熔断降级: uploadCommodityImage, userId={}", userId);
        return Result.fail("图片服务暂时不可用，图片上传失败，请稍后重试");
    }
    
    @Override
    public Result uploadCommodityImageForCommodity(String commodityId, MultipartFile file) {
        log.warn("图片服务不可用，触发熔断降级: uploadCommodityImageForCommodity, commodityId={}", commodityId);
        return Result.fail("图片服务暂时不可用，图片上传失败，请稍后重试");
    }
    
    @Override
    public Result deleteCommodityImageByUrl(String imageUrl) {
        log.warn("图片服务不可用，触发熔断降级: deleteCommodityImageByUrl, imageUrl={}", imageUrl);
        return Result.ok("图片服务暂时不可用，图片删除失败"); // 删除失败不影响主流程
    }
}

