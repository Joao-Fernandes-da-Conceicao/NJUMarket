package com.njumarket.auth.client;

import com.njumarket.njumarket.dto.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * Commodity Service Feign Client
 * 用于 Auth Service 调用 Commodity Service
 */
@FeignClient(name = "njumarket-service-commodity", path = "/api/internal")
public interface CommodityClient {
    
    /**
     * 获取用户发布的商品列表（内部接口）
     * @param sellerId 卖家ID
     * @param page 页码
     * @param size 每页大小
     * @param status 商品状态（可选）
     * @return 商品列表
     */
    @GetMapping("/commodity/seller/{sellerId}")
    Result getUserCommodities(@PathVariable String sellerId,
                             @RequestParam(defaultValue = "1") Integer page,
                             @RequestParam(defaultValue = "100") Integer size,
                             @RequestParam(required = false) String status);
    
    /**
     * 获取用户的AI聊天记录（内部接口）
     * @param userId 用户ID
     * @param limit 返回数量限制
     * @return 对话历史
     */
    @GetMapping("/commodity/ai-chat-history/{userId}")
    Result getAIChatHistory(@PathVariable String userId,
                           @RequestParam(defaultValue = "50") Integer limit);
}

