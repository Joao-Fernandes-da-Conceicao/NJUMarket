package com.njumarket.auth.client;

import com.njumarket.njumarket.dto.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;

/**
 * AI 服务向量接口客户端（用户画像向量写入/检索）。
 */
@FeignClient(
    name = "njumarket-service-ai",
    path = "/api/internal",
    fallback = com.njumarket.auth.client.fallback.AIClientFallback.class
)
public interface AIClient {

    @PostMapping("/vector/user-profile/upsert")
    Result upsertUserProfileVector(@RequestBody Map<String, Object> body);

    @PostMapping("/vector/user-profile/search")
    Result searchUserProfileVector(@RequestBody Map<String, Object> body);
}

