package com.njumarket.commodity.vector.function;

import com.njumarket.commodity.entity.Commodity;
import com.njumarket.commodity.vector.AISearchService;
import com.njumarket.njumarket.utils.SecurityUtils;
import dev.langchain4j.agent.tool.Tool;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 商品搜索工具
 * 用于 LangChain4j Function Calling
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SearchCommoditiesTool {
    
    private final AISearchService aiSearchService;
    
    // 用于存储最近一次搜索的商品列表（供外部访问）
    @Getter
    @Setter
    private List<Commodity> lastSearchResults = new ArrayList<>();
    
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
        
        // 使用统一上下文传递机制获取用户ID
        String userId = SecurityUtils.getCurrentUserId();
        
        if (log.isDebugEnabled()) {
            log.debug("搜索商品 - userId={}, query={}", userId, query);
        }
        
        List<Commodity> commodities = aiSearchService.search(
            query, 
            location, 
            limit != null ? limit : 20,
            userId
        );
        
        // 保存搜索结果供外部访问
        this.lastSearchResults = commodities;
        
        if (commodities.isEmpty()) {
            return "抱歉，没有找到相关商品。";
        }
        
        // 将搜索结果转换为字符串格式
        StringBuilder sb = new StringBuilder();
        sb.append("找到 ").append(commodities.size()).append(" 个相关商品：\n");
        for (int i = 0; i < commodities.size() && i < 10; i++) {
            Commodity c = commodities.get(i);
            sb.append(String.format("%d. %s - ¥%.2f", i + 1, c.getTitle(), c.getPrice()));
            if (c.getDescription() != null && !c.getDescription().isEmpty()) {
                String desc = c.getDescription().length() > 50 ? 
                    c.getDescription().substring(0, 50) + "..." : c.getDescription();
                sb.append(" (").append(desc).append(")");
            }
            sb.append("\n");
        }
        
        return sb.toString();
    }
}

