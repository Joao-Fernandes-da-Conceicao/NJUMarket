package com.njumarket.order.client.fallback;

import com.njumarket.njumarket.dto.Result;
import com.njumarket.order.client.CommodityClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Commodity Client Fallback
 * 当商品服务不可用时的降级处理
 */
@Slf4j
@Component
public class CommodityClientFallback implements CommodityClient {
    
    @Override
    public Result getCommodityForUpdate(String commodityId) {
        log.warn("商品服务不可用，触发熔断降级: commodityId={}", commodityId);
        return Result.fail("商品服务暂时不可用，请稍后重试");
    }
    
    @Override
    public Result updateCommodityStock(String commodityId, Integer quantity) {
        log.warn("商品服务不可用，触发熔断降级: commodityId={}, quantity={}", commodityId, quantity);
        return Result.fail("商品服务暂时不可用，库存更新失败，请稍后重试");
    }
    
    @Override
    public Result restoreCommodityStock(String commodityId, Integer quantity) {
        log.warn("商品服务不可用，触发熔断降级: commodityId={}, quantity={}", commodityId, quantity);
        return Result.fail("商品服务暂时不可用，库存恢复失败，请稍后重试");
    }
}

