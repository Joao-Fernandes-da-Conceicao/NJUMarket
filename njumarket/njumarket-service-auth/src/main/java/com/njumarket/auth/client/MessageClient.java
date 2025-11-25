package com.njumarket.auth.client;

import com.njumarket.njumarket.dto.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Message Service Feign Client
 * 用于 Auth Service 调用 Message Service
 */
@FeignClient(name = "njumarket-service-message", path = "/api/internal")
public interface MessageClient {
    
    /**
     * 获取用户的聊天记录（内部接口）
     * @param userId 用户ID
     * @param limit 返回数量限制
     * @return 聊天记录
     */
    @GetMapping("/message/user-chat-history/{userId}")
    Result getUserChatHistory(@PathVariable String userId,
                             @RequestParam(defaultValue = "50") Integer limit);
}

