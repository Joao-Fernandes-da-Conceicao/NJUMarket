package com.njumarket.order.client.fallback;

import com.njumarket.njumarket.dto.Result;
import com.njumarket.order.client.CommodityQueryClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Commodity Query Client Fallback
 * 当商品查询服务不可用时的降级处理
 */
@Slf4j
@Component
public class CommodityQueryClientFallback implements CommodityQueryClient {
    
    @Override
    public Result getCommodityById(String commodityId) {
        log.warn("商品查询服务不可用，触发熔断降级: commodityId={}", commodityId);
        return Result.fail("商品查询服务暂时不可用，请稍后重试");
    }
    
    @Override
    public Result getCommoditiesBatchStatus(List<String> commodityIds) {
        log.warn("商品查询服务不可用，触发熔断降级: commodityIds={}", commodityIds);
        return Result.fail("商品查询服务暂时不可用，请稍后重试");
    }
}

