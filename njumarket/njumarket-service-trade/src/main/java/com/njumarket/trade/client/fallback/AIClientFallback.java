package com.njumarket.trade.client.fallback;

import com.njumarket.trade.client.AIClient;
import com.njumarket.njumarket.dto.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * AIClient 熔断降级：AI 服务不可用时返回失败，调用方将回退为使用原标题/描述构建 document。
 */
@Slf4j
@Component
public class AIClientFallback implements AIClient {

    @Override
    public Result enrichCommodityForSearch(Map<String, Object> body) {
        Object title = body != null ? body.get("title") : null;
        log.warn("AI 服务不可用，商品丰度增强降级（将使用原标题与描述）: title={}", title);
        return Result.fail("AI 服务暂时不可用，跳过丰度增强");
    }

    @Override
    public Result upsertCommodityVector(Map<String, Object> body) {
        Object id = body != null ? body.get("id") : null;
        log.warn("AI 服务不可用，商品向量写入降级: id={}", id);
        return Result.fail("AI 服务暂时不可用，商品向量未写入");
    }

    @Override
    public Result searchCommodityVector(Map<String, Object> body) {
        log.warn("AI 服务不可用，商品向量检索降级");
        return Result.fail("AI 服务暂时不可用，商品向量检索失败");
    }
}
