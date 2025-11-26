package com.njumarket.ai.tool;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.njumarket.ai.client.CommodityClient;
import com.njumarket.njumarket.dto.Result;
import com.njumarket.njumarket.dto.internal.CommodityInternalDTO;
import com.njumarket.njumarket.utils.SecurityUtils;
import dev.langchain4j.agent.tool.Tool;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
    
    @Autowired(required = false)
    private ObjectMapper objectMapper;
    
    // 用于存储最近一次搜索的商品列表（供外部访问）
    @Getter
    @Setter
    private List<CommodityDTO> lastSearchResults = new ArrayList<>();
    
    /**
     * 搜索商品工具
     * LangChain4j 使用 @Tool 注解定义工具函数
     * 
     * @param query 搜索查询文本
     * @param location 位置偏好（可选）
     * @param limit 返回数量限制（可选，默认 20）
     * @return 商品列表的文本描述
     */
    @Tool("搜索商品。根据用户的查询文本、位置偏好和数量限制，返回相关的商品列表。当用户想要查找、购买或了解商品时使用此功能。")
    public String searchCommodities(String query, String location, Integer limit) {
        log.info("Function Calling: 搜索商品 - query={}, location={}, limit={}", query, location, limit);
        
        String userId = SecurityUtils.getCurrentUserId();
        
        if (log.isDebugEnabled()) {
            log.debug("搜索商品 - userId={}, query={}", userId, query);
        }
        
        try {
            // 通过Feign调用商品服务的AI搜索接口
            Result result = commodityClient.aiSearch(query, location, limit != null ? limit : 20, userId);
            
            if (result == null || !result.getSuccess() || result.getData() == null) {
                log.warn("AI搜索返回空结果: query={}", query);
                this.lastSearchResults = new ArrayList<>();
                return "抱歉，没有找到相关商品。";
            }
            
            // 转换结果 - 使用 ObjectMapper 处理 Feign Client 反序列化问题
            List<CommodityInternalDTO> commodityDTOs;
            Object data = result.getData();
            
            if (data instanceof List) {
                // 如果已经是 List，尝试转换
                if (objectMapper != null) {
                    // 使用 ObjectMapper 进行类型转换
                    commodityDTOs = objectMapper.convertValue(data, 
                        new TypeReference<List<CommodityInternalDTO>>() {});
                } else {
                    // 手动转换（备用方案）
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> dataList = (List<Map<String, Object>>) data;
                    commodityDTOs = dataList.stream()
                        .map(this::mapToCommodityInternalDTO)
                        .collect(Collectors.toList());
                }
            } else {
                log.error("AI搜索返回的数据类型不正确: type={}, data={}", 
                    data != null ? data.getClass().getName() : "null", data);
                this.lastSearchResults = new ArrayList<>();
                return "抱歉，搜索时遇到了问题，请稍后重试。";
            }
            
            // 转换为CommodityDTO列表
            List<CommodityDTO> commodities = commodityDTOs.stream()
                .map(this::convertToCommodityDTO)
                .collect(Collectors.toList());
            
            // 保存搜索结果供外部访问
            this.lastSearchResults = commodities;
            
            if (commodities.isEmpty()) {
                return "抱歉，没有找到相关商品。";
            }
            
            // 将搜索结果转换为字符串格式
            StringBuilder sb = new StringBuilder();
            sb.append("找到 ").append(commodities.size()).append(" 个相关商品：\n");
            for (int i = 0; i < commodities.size() && i < 10; i++) {
                CommodityDTO c = commodities.get(i);
                sb.append(String.format("%d. %s - ¥%.2f", i + 1, c.getTitle(), c.getPrice()));
                if (c.getDescription() != null && !c.getDescription().isEmpty()) {
                    String desc = c.getDescription().length() > 50 ? 
                        c.getDescription().substring(0, 50) + "..." : c.getDescription();
                    sb.append(" (").append(desc).append(")");
                }
                sb.append("\n");
            }
            
            return sb.toString();
            
        } catch (Exception e) {
            log.error("搜索商品失败: query={}, error={}", query, e.getMessage(), e);
            this.lastSearchResults = new ArrayList<>();
            return "抱歉，搜索时遇到了问题，请稍后重试。";
        }
    }
    
    /**
     * 将 Map 转换为 CommodityInternalDTO（备用方案）
     */
    private CommodityInternalDTO mapToCommodityInternalDTO(Map<String, Object> map) {
        CommodityInternalDTO dto = new CommodityInternalDTO();
        dto.setCommodityId((String) map.get("commodityId"));
        dto.setSellerId((String) map.get("sellerId"));
        dto.setTitle((String) map.get("title"));
        dto.setDescription((String) map.get("description"));
        if (map.get("price") != null) {
            if (map.get("price") instanceof Number) {
                dto.setPrice(new java.math.BigDecimal(map.get("price").toString()));
            } else {
                dto.setPrice((java.math.BigDecimal) map.get("price"));
            }
        }
        if (map.get("stock") != null) {
            dto.setStock(map.get("stock") instanceof Integer ? 
                (Integer) map.get("stock") : ((Number) map.get("stock")).intValue());
        }
        dto.setCategory((String) map.get("category"));
        dto.setConditionLevel((String) map.get("conditionLevel"));
        dto.setLocation((String) map.get("location"));
        dto.setAddressSnapshotFull((String) map.get("addressSnapshotFull"));
        return dto;
    }
    
    private CommodityDTO convertToCommodityDTO(CommodityInternalDTO dto) {
        CommodityDTO commodity = new CommodityDTO();
        commodity.setCommodityId(dto.getCommodityId());
        commodity.setSellerId(dto.getSellerId());
        commodity.setTitle(dto.getTitle());
        commodity.setDescription(dto.getDescription());
        commodity.setPrice(dto.getPrice());
        commodity.setStock(dto.getStock());
        commodity.setCategory(dto.getCategory());
        commodity.setConditionLevel(dto.getConditionLevel());
        commodity.setLocation(dto.getLocation());
        commodity.setAddressSnapshotFull(dto.getAddressSnapshotFull());
        return commodity;
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

