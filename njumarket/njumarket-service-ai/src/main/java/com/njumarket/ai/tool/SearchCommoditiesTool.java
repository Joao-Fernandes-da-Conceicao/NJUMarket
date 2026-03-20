package com.njumarket.ai.tool;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.njumarket.ai.client.CommodityClient;
import com.njumarket.ai.service.MilvusVectorService;
import com.njumarket.njumarket.dto.Result;
import dev.langchain4j.agent.tool.Tool;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 商品搜索工具
 * 用于 LangChain4j Function Calling
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SearchCommoditiesTool {

    private final CommodityClient commodityClient;
    private final ObjectMapper objectMapper;
    private final MilvusVectorService milvusVectorService;

    /** 最近一次搜索的原始列表（兼容） */
    @Getter
    @Setter
    private List<CommodityDTO> lastSearchResults = new ArrayList<>();

    /** 本轮对话中所有搜索结果的并集（按 commodityId 去重），供 LLM 多轮搜索后从中勾选推荐 */
    private final Map<String, CommodityDTO> accumulatedSearchResults = new LinkedHashMap<>();

    /** LLM 通过 confirmRecommendedCommodities 确认的本轮推荐 ID，为空则退回用 lastSearchResults */
    @Getter
    @Setter
    private List<String> recommendedIdsForResponse;

    /**
     * 搜索商品工具
     * LangChain4j 使用 @Tool 注解定义工具函数
     *
     * @param query 搜索查询文本
     * @param location 位置偏好（可选、弱参数，仅用户明确强调地点时传入，多数情况不传）
     * @param limit 返回数量限制（可选，默认 20）
     * @return 商品列表的文本描述
     */
    @Tool("搜索商品。根据用户的查询文本和数量限制返回相关商品列表；location 为可选的弱参数，仅当用户明确强调地点时再传。")
    public String searchCommodities(String query, String location, Integer limit) {
        log.info("Function Calling: 搜索商品 - query={}, location={}, limit={}", query, location, limit);

        try {
            int safeLimit = limit != null && limit > 0 ? limit : 20;
            Result result = commodityClient.searchCommodities(query, 1, safeLimit, location,
                    null, null, null, "relevance");

            if (result == null || !Boolean.TRUE.equals(result.getSuccess()) || result.getData() == null) {
                log.warn("商品搜索返回空结果: query={}", query);
                this.lastSearchResults = new ArrayList<>();
                return "抱歉，没有找到相关商品。";
            }

            List<CommodityDTO> commodities = extractCommodities(result.getData());
            this.lastSearchResults = commodities;
            for (CommodityDTO c : commodities) {
                if (c.getCommodityId() != null) {
                    accumulatedSearchResults.put(c.getCommodityId(), c);
                }
            }

            if (commodities.isEmpty()) {
                return "抱歉，没有找到相关商品。";
            }

            StringBuilder sb = new StringBuilder();
            sb.append("找到 ").append(commodities.size()).append(" 个相关商品（含 commodityId，推荐时请只勾选真正符合用户需求的）：\n");
            for (int i = 0; i < commodities.size() && i < 15; i++) {
                CommodityDTO c = commodities.get(i);
                sb.append(String.format("%d. [commodityId=%s] %s - ¥%.2f",
                    i + 1, c.getCommodityId(), c.getTitle(), c.getPrice() != null ? c.getPrice().doubleValue() : 0.0));
                if (c.getDescription() != null && !c.getDescription().isEmpty()) {
                    String desc = c.getDescription().length() > 60 ? c.getDescription().substring(0, 60) + "..." : c.getDescription();
                    sb.append(" (").append(desc).append(")");
                }
                sb.append("\n");
            }
            sb.append("请根据用户需求过滤后，仅对真正符合的商品调用 confirmRecommendedCommodities 传入其 commodityId 列表。");
            return sb.toString();

        } catch (Exception e) {
            log.error("搜索商品失败: query={}, error={}", query, e.getMessage(), e);
            this.lastSearchResults = new ArrayList<>();
            return "抱歉，搜索时遇到了问题，请稍后重试。";
        }
    }

    @Tool("语义向量搜索商品。适合用户表达模糊、同义词多、口语化强的需求。仅需 query 和 limit。")
    public String searchCommoditiesByVector(String query, Integer limit) {
        log.info("Function Calling: 向量搜索商品 - query={}, limit={}", query, limit);
        try {
            int safeLimit = limit != null && limit > 0 ? limit : 20;
            List<Map<String, Object>> vectorHits = milvusVectorService.searchCommodityByText(query, safeLimit);
            List<String> ids = vectorHits.stream()
                    .map(hit -> {
                        Object id = hit.get("id");
                        return id != null ? id.toString() : null;
                    })
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
            List<CommodityDTO> commodities = fetchCommoditiesByIds(ids);
            this.lastSearchResults = commodities;
            commodities.forEach(c -> {
                if (c.getCommodityId() != null) {
                    accumulatedSearchResults.put(c.getCommodityId(), c);
                }
            });
            if (commodities.isEmpty()) {
                return "向量检索未找到明显匹配商品。";
            }
            return buildCommodityResultText("向量检索", commodities);
        } catch (Exception e) {
            log.error("向量搜索失败: query={}, error={}", query, e.getMessage(), e);
            this.lastSearchResults = new ArrayList<>();
            return "抱歉，向量搜索时遇到了问题，请稍后重试。";
        }
    }

    @Tool("混合检索商品：同时执行关键词检索与向量检索并合并去重，召回更全。location 为弱参数。")
    public String searchCommoditiesHybrid(String query, String location, Integer limit) {
        log.info("Function Calling: 混合搜索商品 - query={}, location={}, limit={}", query, location, limit);
        int safeLimit = limit != null && limit > 0 ? limit : 20;
        String esText = searchCommodities(query, location, safeLimit);
        List<CommodityDTO> esResults = new ArrayList<>(lastSearchResults);
        String vectorText = searchCommoditiesByVector(query, safeLimit);
        List<CommodityDTO> vectorResults = new ArrayList<>(lastSearchResults);

        Map<String, CommodityDTO> merged = new LinkedHashMap<>();
        esResults.forEach(c -> merged.put(c.getCommodityId(), c));
        vectorResults.forEach(c -> merged.putIfAbsent(c.getCommodityId(), c));

        List<CommodityDTO> finalResults = new ArrayList<>(merged.values());
        this.lastSearchResults = finalResults;
        finalResults.forEach(c -> {
            if (c.getCommodityId() != null) {
                accumulatedSearchResults.put(c.getCommodityId(), c);
            }
        });
        if (finalResults.isEmpty()) {
            return "混合检索未找到相关商品。";
        }
        return buildCommodityResultText("混合检索(ES+Vector)", finalResults)
                + "\n（补充信息）ES: " + summarizeToolText(esText) + "；Vector: " + summarizeToolText(vectorText);
    }

    /**
     * 确认本轮要推荐给用户的商品 ID 列表。只应包含经过过滤、真正符合用户需求的商品。
     * 在完成搜索并过滤后调用，传入符合要求的 commodityId 列表；未调用则沿用最后一次搜索的完整结果。
     */
    @Tool("确认本轮要推荐给用户的商品。传入经过过滤、真正符合用户需求的 commodityId 列表（仅这些会作为推荐卡片返回给用户）。若没有符合的则传空列表。")
    public String confirmRecommendedCommodities(List<String> commodityIds) {
        if (commodityIds != null) {
            this.recommendedIdsForResponse = new ArrayList<>(commodityIds);
            log.info("Agent 确认推荐商品数: {}", commodityIds.size());
        }
        return "已记录推荐列表，将仅向用户展示这些商品。";
    }

    /** 新一轮对话开始时调用，清空累积结果与推荐 ID */
    public void clearForNewTurn() {
        this.lastSearchResults = new ArrayList<>();
        this.accumulatedSearchResults.clear();
        this.recommendedIdsForResponse = null;
    }

    /**
     * 供响应使用：若 LLM 调用了 confirmRecommendedCommodities，则只返回其确认的 ID 对应商品；否则退回最后一次搜索的完整列表。
     */
    public List<CommodityDTO> getRecommendedCommoditiesForResponse() {
        if (recommendedIdsForResponse != null && !recommendedIdsForResponse.isEmpty()) {
            return recommendedIdsForResponse.stream()
                .map(accumulatedSearchResults::get)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toList());
        }
        return lastSearchResults;
    }

    private List<CommodityDTO> extractCommodities(Object data) {
        try {
            Map<String, Object> pageResult = objectMapper.convertValue(data,
                new TypeReference<Map<String, Object>>() {});
            Object commoditiesRaw = pageResult.get("commodities");
            if (commoditiesRaw == null) return new ArrayList<>();
            return objectMapper.convertValue(commoditiesRaw, new TypeReference<List<CommodityDTO>>() {});
        } catch (Exception e) {
            log.error("解析商品列表失败: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    private List<CommodityDTO> fetchCommoditiesByIds(List<String> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyList();
        }
        try {
            Result result = commodityClient.getCommoditiesByIds(ids);
            if (result == null || !Boolean.TRUE.equals(result.getSuccess()) || result.getData() == null) {
                return Collections.emptyList();
            }
            Object data = result.getData();
            if (data instanceof List<?>) {
                List<CommodityDTO> list = objectMapper.convertValue(data, new TypeReference<List<CommodityDTO>>() {});
                Map<String, CommodityDTO> byId = list.stream()
                        .filter(c -> c.getCommodityId() != null)
                        .collect(Collectors.toMap(CommodityDTO::getCommodityId, c -> c, (a, b) -> a));
                return ids.stream().map(byId::get).filter(Objects::nonNull).collect(Collectors.toList());
            }
            return Collections.emptyList();
        } catch (Exception e) {
            log.warn("批量查询商品详情失败: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    private String buildCommodityResultText(String source, List<CommodityDTO> commodities) {
        StringBuilder sb = new StringBuilder();
        sb.append(source).append("找到 ").append(commodities.size()).append(" 个相关商品（含 commodityId）：\n");
        for (int i = 0; i < commodities.size() && i < 15; i++) {
            CommodityDTO c = commodities.get(i);
            sb.append(String.format("%d. [commodityId=%s] %s - ¥%.2f",
                    i + 1, c.getCommodityId(), c.getTitle(), c.getPrice() != null ? c.getPrice().doubleValue() : 0.0));
            if (c.getDescription() != null && !c.getDescription().isEmpty()) {
                String desc = c.getDescription().length() > 60 ? c.getDescription().substring(0, 60) + "..." : c.getDescription();
                sb.append(" (").append(desc).append(")");
            }
            sb.append("\n");
        }
        sb.append("请过滤后调用 confirmRecommendedCommodities。");
        return sb.toString();
    }

    private String summarizeToolText(String text) {
        if (text == null) return "";
        String normalized = text.replace('\n', ' ').trim();
        return normalized.length() > 60 ? normalized.substring(0, 60) + "..." : normalized;
    }

    /**
     * 简化的商品DTO（用于AI服务内部）
     */
    @Getter
    @Setter
    public static class CommodityDTO {
        private String commodityId;
        private String sellerId;
        private String title;
        private String description;
        private java.math.BigDecimal price;
        private Integer stock;
        private String category;
        private String conditionLevel;
        private String location;
        private String addressSnapshotFull;
    }
}
