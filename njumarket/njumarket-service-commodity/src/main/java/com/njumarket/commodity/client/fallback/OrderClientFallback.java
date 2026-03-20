package com.njumarket.commodity.client.fallback;

import com.njumarket.commodity.client.OrderClient;
import com.njumarket.njumarket.dto.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * OrderClient Fallback 实现
 * 当订单服务不可用时，返回明确的错误信息，阻止删除商品
 */
@Slf4j
@Component
public class OrderClientFallback implements OrderClient {
    
    @Override
    public Result checkCommodityHasOrders(String commodityId) {
        log.warn("订单服务不可用，触发熔断降级: checkCommodityHasOrders, commodityId={}", commodityId);
        return Result.fail("订单服务暂时不可用，无法检查商品是否有订单，请稍后重试");
    }

    @Override
    public Result syncInventory(Map<String, Object> payload) {
        log.warn("订单服务不可用，库存同步失败（降级）: commodityId={}", payload.get("commodityId"));
        return Result.fail("订单服务暂时不可用，库存同步失败");
    }

    @Override
    public Result adjustInventory(Map<String, Object> payload) {
        log.warn("订单服务不可用，库存调整失败（降级）: commodityId={}", payload.get("commodityId"));
        return Result.fail("订单服务暂时不可用，库存调整失败");
    }

    @Override
    public Result zeroInventory(String commodityId) {
        log.warn("订单服务不可用，库存归零失败（降级）: commodityId={}", commodityId);
        return Result.fail("订单服务暂时不可用，库存归零失败");
    }
}

