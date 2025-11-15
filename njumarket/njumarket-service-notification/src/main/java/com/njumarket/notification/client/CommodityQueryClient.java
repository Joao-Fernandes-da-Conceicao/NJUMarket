package com.njumarket.notification.client;

import com.njumarket.njumarket.dto.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

/**
 * Commodity Query Service Feign Client
 * 用于推送服务调用商品服务的查询功能
 * 启用 Resilience4j 熔断器保护
 * 注意：查询失败时返回空列表，不影响增量轮询
 */
@FeignClient(
    name = "njumarket-service-commodity", 
    contextId = "notificationCommodityQueryClient",
    fallback = com.njumarket.notification.client.fallback.CommodityQueryClientFallback.class
)
public interface CommodityQueryClient {
    
    /**
     * 批量查询商品状态（用户接口，用于聊天界面）
     */
    @PostMapping("/api/user/commodity/batch-status")
    Result getCommoditiesBatchStatus(@RequestBody List<String> commodityIds);
}

