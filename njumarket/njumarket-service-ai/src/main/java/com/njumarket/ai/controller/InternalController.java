package com.njumarket.ai.controller;

import com.njumarket.ai.service.CommodityEnrichmentService;
import com.njumarket.ai.service.MilvusVectorService;
import com.njumarket.njumarket.dto.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

/**
 * AI 服务内部 API，供其他微服务（如商品服务）通过 Feign 调用。
 */
@Slf4j
@Tag(name = "AI 内部接口", description = "供商品服务等调用的内部 API")
@RestController
@RequestMapping("/api/internal")
@RequiredArgsConstructor
public class InternalController {

    private final CommodityEnrichmentService commodityEnrichmentService;
    private final MilvusVectorService milvusVectorService;

    /**
     * 商品检索丰度增强：单次 Chat 生成丰度更高的可检索文本，用于写入 ES。
     * 请求体示例：{ "title": "...", "description": "...", "category": "...", "conditionLevel": "...", "location": "...", "addressSnapshotFull": "..." }
     */
    @Operation(summary = "商品丰度增强", description = "根据商品信息生成用于搜索的丰度文本")
    @PostMapping("/commodity-enrich")
    public Result enrichCommodityForSearch(@RequestBody Map<String, Object> body) {
        try {
            String title = getString(body, "title");
            if (!StringUtils.hasText(title)) {
                return Result.fail("缺少 title");
            }
            String description = getString(body, "description");
            String category = getString(body, "category");
            String conditionLevel = getString(body, "conditionLevel");
            String location = getString(body, "location");
            String addressSnapshotFull = getString(body, "addressSnapshotFull");

            Optional<String> enriched = commodityEnrichmentService.enrichForSearch(
                title, description, category, conditionLevel, location, addressSnapshotFull);

            if (enriched.isPresent()) {
                return Result.ok("丰度文本生成成功", Map.of("enrichedKeywordPayload", enriched.get()));
            }
            return Result.ok("未生成丰度文本（可回退使用原标题与描述）", null);
        } catch (Exception e) {
            log.warn("商品丰度增强失败: {}", e.getMessage());
            return Result.fail("丰度增强失败: " + e.getMessage());
        }
    }

    private static String getString(Map<String, Object> map, String key) {
        Object v = map.get(key);
        if (v == null) return null;
        return v.toString().trim();
    }

    @Operation(summary = "写入商品向量", description = "供商品服务调用，向 Milvus upsert 商品向量")
    @PostMapping("/vector/commodity/upsert")
    public Result upsertCommodityVector(@RequestBody Map<String, Object> body) {
        try {
            String id = getString(body, "id");
            String bizId = getString(body, "bizId");
            String content = getString(body, "content");
            Object embeddingRaw = body.get("embedding");
            if (embeddingRaw != null) {
                milvusVectorService.upsertCommodityVector(id, bizId, content, toFloatList(embeddingRaw));
            } else {
                milvusVectorService.upsertCommodityVectorByText(id, bizId, content);
            }
            return Result.ok("商品向量写入成功");
        } catch (Exception e) {
            log.warn("商品向量写入失败: {}", e.getMessage());
            return Result.fail("商品向量写入失败: " + e.getMessage());
        }
    }

    @Operation(summary = "写入用户画像向量", description = "供用户服务调用，向 Milvus upsert 用户画像向量")
    @PostMapping("/vector/user-profile/upsert")
    public Result upsertUserProfileVector(@RequestBody Map<String, Object> body) {
        try {
            String id = getString(body, "id");
            String bizId = getString(body, "bizId");
            String content = getString(body, "content");
            Object embeddingRaw = body.get("embedding");
            if (embeddingRaw != null) {
                milvusVectorService.upsertUserProfileVector(id, bizId, content, toFloatList(embeddingRaw));
            } else {
                milvusVectorService.upsertUserProfileVectorByText(id, bizId, content);
            }
            return Result.ok("用户画像向量写入成功");
        } catch (Exception e) {
            log.warn("用户画像向量写入失败: {}", e.getMessage());
            return Result.fail("用户画像向量写入失败: " + e.getMessage());
        }
    }

    @Operation(summary = "检索商品向量", description = "供 AI 或商品服务调用，按向量检索商品")
    @PostMapping("/vector/commodity/search")
    public Result searchCommodityVector(@RequestBody Map<String, Object> body) {
        try {
            Integer topK = toInteger(body.get("topK"));
            java.util.List<Map<String, Object>> hits;
            if (body.get("embedding") != null) {
                hits = milvusVectorService.searchCommodity(toFloatList(body.get("embedding")), topK);
            } else {
                String queryText = getString(body, "queryText");
                hits = milvusVectorService.searchCommodityByText(queryText, topK);
            }
            return Result.ok("商品向量检索成功", hits);
        } catch (Exception e) {
            log.warn("商品向量检索失败: {}", e.getMessage());
            return Result.fail("商品向量检索失败: " + e.getMessage());
        }
    }

    @Operation(summary = "检索用户画像向量", description = "供 AI 或用户服务调用，按向量检索用户画像")
    @PostMapping("/vector/user-profile/search")
    public Result searchUserProfileVector(@RequestBody Map<String, Object> body) {
        try {
            Integer topK = toInteger(body.get("topK"));
            java.util.List<Map<String, Object>> hits;
            if (body.get("embedding") != null) {
                hits = milvusVectorService.searchUserProfile(toFloatList(body.get("embedding")), topK);
            } else {
                String queryText = getString(body, "queryText");
                hits = milvusVectorService.searchUserProfileByText(queryText, topK);
            }
            return Result.ok("用户画像向量检索成功", hits);
        } catch (Exception e) {
            log.warn("用户画像向量检索失败: {}", e.getMessage());
            return Result.fail("用户画像向量检索失败: " + e.getMessage());
        }
    }

    private static java.util.List<Float> toFloatList(Object raw) {
        if (!(raw instanceof java.util.List<?> list)) {
            throw new IllegalArgumentException("embedding 不能为空且必须为数组");
        }
        java.util.List<Float> result = new java.util.ArrayList<>(list.size());
        for (Object item : list) {
            if (!(item instanceof Number n)) {
                throw new IllegalArgumentException("embedding 数组元素必须为数值");
            }
            result.add(n.floatValue());
        }
        return result;
    }

    private static Integer toInteger(Object raw) {
        if (raw == null) {
            return null;
        }
        if (raw instanceof Number n) {
            return n.intValue();
        }
        try {
            return Integer.parseInt(raw.toString().trim());
        } catch (Exception e) {
            throw new IllegalArgumentException("topK 必须为整数");
        }
    }
}
