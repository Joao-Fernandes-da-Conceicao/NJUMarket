package com.njumarket.auth.client.fallback;

import com.njumarket.auth.client.AIClient;
import com.njumarket.njumarket.dto.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
public class AIClientFallback implements AIClient {

    @Override
    public Result upsertUserProfileVector(Map<String, Object> body) {
        Object id = body != null ? body.get("id") : null;
        log.warn("AI 服务不可用，用户画像向量写入降级: id={}", id);
        return Result.fail("AI 服务暂时不可用，用户画像向量未写入");
    }

    @Override
    public Result searchUserProfileVector(Map<String, Object> body) {
        log.warn("AI 服务不可用，用户画像向量检索降级");
        return Result.fail("AI 服务暂时不可用，用户画像向量检索失败");
    }
}

