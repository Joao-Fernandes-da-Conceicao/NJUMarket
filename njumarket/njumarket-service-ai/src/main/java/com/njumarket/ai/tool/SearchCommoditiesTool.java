package com.njumarket.ai.tool;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.njumarket.ai.client.CommodityClient;
import com.njumarket.ai.service.MilvusVectorService;
import com.njumarket.njumarket.dto.Result;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.ToolMemoryId;
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
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 商品搜索工具执行器（LangChain4j Function Calling）。
 * <p>内部按 memoryId 维护会话级状态（推荐列表、累计搜索结果），由 AIAgentService 在每轮生命周期中清理。
 * <p>工具方法必须带 {@link ToolMemoryId}，由框架注入，不进入发给模型的 JSON Schema；
 * 这样在 Reactor 流式、异步线程中也能正确写入会话状态（推荐商品卡片）。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SearchCommoditiesTool {

    private final CommodityClient commodityClient;
    private final ObjectMapper objectMapper;
    private final MilvusVectorService milvusVectorService;
    private final ConcurrentHashMap<String, CommoditySearchTurnState> stateByMemoryId = new ConcurrentHashMap<>();

    private static String toMemoryId(Object memoryId) {
        return memoryId == null ? null : memoryId.toString();
    }

    private CommoditySearchTurnState stateFor(String memoryId) {
        if (memoryId == null) {
            log.warn("SearchCommoditiesTool: memoryId 为空，使用临时状态（推荐卡片可能丢失）");
            return new CommoditySearchTurnState();
        }
        return stateByMemoryId.computeIfAbsent(memoryId, k -> new CommoditySearchTurnState());
    }

    public void beginConversationTurn(String memoryId) {
        if (memoryId != null) {
            stateByMemoryId.put(memoryId, new CommoditySearchTurnState());
        }
    }

    public void endConversationTurn(String memoryId) {
        if (memoryId != null) {
            stateByMemoryId.remove(memoryId);
        }
    }

    public List<CommodityDTO> getRecommendedCommoditiesForResponse(String memoryId) {
        if (memoryId == null) {
            return Collections.emptyList();
        }
        CommoditySearchTurnState st = stateByMemoryId.get(memoryId);
        if (st == null) {
            return Collections.emptyList();
        }
        if (st.recommendedIdsForResponse != null && !st.recommendedIdsForResponse.isEmpty()) {
            return st.recommendedIdsForResponse.stream()
                    .map(st.accumulatedSearchResults::get)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
        }
        return st.lastSearchResults != null ? new ArrayList<>(st.lastSearchResults) : Collections.emptyList();
    }

    @Tool("搜索商品。根据用户的查询文本和数量限制返回相关商品列表；location 为可选的弱参数，仅当用户明确强调地点时再传。")
    public String searchCommodities(String query, String location, Integer limit, @ToolMemoryId Object memoryId) {
        return searchCommoditiesImpl(query, location, limit, toMemoryId(memoryId));
    }

    private String searchCommoditiesImpl(String query, String location, Integer limit, String memoryId) {
        log.info("Function Calling: 搜索商品 - query={}, location={}, limit={}, memoryId={}", query, location, limit, memoryId);
        CommoditySearchTurnState st = stateFor(memoryId);

        try {
            int safeLimit = limit != null && limit > 0 ? limit : 20;
            Result result = commodityClient.searchCommodities(query, 1, safeLimit, location,
                    null, null, null, "relevance");

            if (result == null || !Boolean.TRUE.equals(result.getSuccess()) || result.getData() == null) {
                log.warn("商品搜索返回空结果: query={}", query);
                st.lastSearchResults = new ArrayList<>();
                return "抱歉，没有找到相关商品。";
            }

            List<CommodityDTO> commodities = extractCommodities(result.getData());
            st.lastSearchResults = commodities;
            for (CommodityDTO c : commodities) {
                if (c.getCommodityId() != null) {
                    st.accumulatedSearchResults.put(c.getCommodityId(), c);
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
            st.lastSearchResults = new ArrayList<>();
            return "抱歉，搜索时遇到了问题，请稍后重试。";
        }
    }

    @Tool("语义向量搜索商品。适合用户表达模糊、同义词多、口语化强的需求。仅需 query 和 limit。")
    public String searchCommoditiesByVector(String query, Integer limit, @ToolMemoryId Object memoryId) {
        return searchCommoditiesByVectorImpl(query, limit, toMemoryId(memoryId));
    }

    private String searchCommoditiesByVectorImpl(String query, Integer limit, String memoryId) {
        log.info("Function Calling: 向量搜索商品 - query={}, limit={}, memoryId={}", query, limit, memoryId);
        CommoditySearchTurnState st = stateFor(memoryId);
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
            st.lastSearchResults = commodities;
            commodities.forEach(c -> {
                if (c.getCommodityId() != null) {
                    st.accumulatedSearchResults.put(c.getCommodityId(), c);
                }
            });
            if (commodities.isEmpty()) {
                return "向量检索未找到明显匹配商品。";
            }
            return buildCommodityResultText("向量检索", commodities, st);
        } catch (Exception e) {
            log.error("向量搜索失败: query={}, error={}", query, e.getMessage(), e);
            st.lastSearchResults = new ArrayList<>();
            return "抱歉，向量搜索时遇到了问题，请稍后重试。";
        }
    }

    @Tool("混合检索商品：同时执行关键词检索与向量检索并合并去重，召回更全。location 为弱参数。")
    public String searchCommoditiesHybrid(String query, String location, Integer limit, @ToolMemoryId Object memoryId) {
        String mid = toMemoryId(memoryId);
        log.info("Function Calling: 混合搜索商品 - query={}, location={}, limit={}, memoryId={}", query, location, limit, mid);
        CommoditySearchTurnState st = stateFor(mid);
        int safeLimit = limit != null && limit > 0 ? limit : 20;
        String esText = searchCommoditiesImpl(query, location, safeLimit, mid);
        List<CommodityDTO> esResults = new ArrayList<>(st.lastSearchResults);
        String vectorText = searchCommoditiesByVectorImpl(query, safeLimit, mid);
        List<CommodityDTO> vectorResults = new ArrayList<>(st.lastSearchResults);

        Map<String, CommodityDTO> merged = new LinkedHashMap<>();
        esResults.forEach(c -> merged.put(c.getCommodityId(), c));
        vectorResults.forEach(c -> merged.putIfAbsent(c.getCommodityId(), c));

        List<CommodityDTO> finalResults = new ArrayList<>(merged.values());
        st.lastSearchResults = finalResults;
        finalResults.forEach(c -> {
            if (c.getCommodityId() != null) {
                st.accumulatedSearchResults.put(c.getCommodityId(), c);
            }
        });
        if (finalResults.isEmpty()) {
            return "混合检索未找到相关商品。";
        }
        return buildCommodityResultText("混合检索(ES+Vector)", finalResults, st)
                + "\n（补充信息）ES: " + summarizeToolText(esText) + "；Vector: " + summarizeToolText(vectorText);
    }

    @Tool("确认本轮要推荐给用户的商品。传入经过过滤、真正符合用户需求的 commodityId 列表（仅这些会作为推荐卡片返回给用户）。若没有符合的则传空列表。")
    public String confirmRecommendedCommodities(List<String> commodityIds, @ToolMemoryId Object memoryId) {
        String mid = toMemoryId(memoryId);
        CommoditySearchTurnState st = stateFor(mid);
        if (commodityIds != null) {
            st.recommendedIdsForResponse = new ArrayList<>(commodityIds);
            log.info("Agent 确认推荐商品数: {}, memoryId={}", commodityIds.size(), mid);
        }
        return "已记录推荐列表，将仅向用户展示这些商品。";
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

    private String buildCommodityResultText(String source, List<CommodityDTO> commodities, CommoditySearchTurnState st) {
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

    /** 单轮内搜索与推荐累积 */
    private static final class CommoditySearchTurnState {
        private List<CommodityDTO> lastSearchResults = new ArrayList<>();
        private final Map<String, CommodityDTO> accumulatedSearchResults = new LinkedHashMap<>();
        private List<String> recommendedIdsForResponse;
    }
}
