package com.njumarket.notification.client.fallback;

import com.njumarket.notification.client.CommodityQueryClient;
import com.njumarket.njumarket.dto.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * CommodityQueryClient Fallback 实现
 * 当商品查询服务不可用时，返回空列表，不影响增量轮询
 */
@Slf4j
@Component
public class CommodityQueryClientFallback implements CommodityQueryClient {
    
    @Override
    public Result getCommoditiesBatchStatus(List<String> commodityIds) {
        log.warn("商品查询服务不可用，触发熔断降级: getCommoditiesBatchStatus, commodityIds={}", commodityIds);
        return Result.ok("商品查询服务暂时不可用，无法批量获取商品状态", Collections.emptyList());
    }
}

