package com.njumarket.trade.client;

import com.njumarket.njumarket.dto.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;

/**
 * AI 服务 Feign 客户端：用于索引重建时请求商品检索丰度增强。
 * 指向 Python LangGraph Agent 服务（njumarket-ai-python），不再依赖 Eureka 注册的 Java AI 服务。
 */
@FeignClient(
    name = "njumarket-ai-python",
    url = "${njumarket.ai.python.base-url:http://localhost:8099}",
    path = "/api/internal",
    fallback = com.njumarket.trade.client.fallback.AIClientFallback.class
)
public interface AIClient {

    /**
     * 请求 AI 服务对商品信息做单次 Chat，生成丰度更高的可检索文本。
     * 请求体需包含：title（必填）, description, category, conditionLevel, location, addressSnapshotFull。
     * 成功时 data 为 Map，含 key "enrichedKeywordPayload" 对应丰度文本。
     */
    @PostMapping("/commodity-enrich")
    Result enrichCommodityForSearch(@RequestBody Map<String, Object> body);

    /**
     * 向 AI 服务写入商品向量（Milvus）。
     * body 示例：{id,bizId,content,embedding}
     */
    @PostMapping("/vector/commodity/upsert")
    Result upsertCommodityVector(@RequestBody Map<String, Object> body);

    /**
     * 调用 AI 服务执行商品向量检索（Milvus）。
     * body 示例：{embedding,topK}
     */
    @PostMapping("/vector/commodity/search")
    Result searchCommodityVector(@RequestBody Map<String, Object> body);
}
