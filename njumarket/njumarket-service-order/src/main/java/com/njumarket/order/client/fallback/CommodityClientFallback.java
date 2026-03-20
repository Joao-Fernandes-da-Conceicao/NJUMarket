package com.njumarket.order.client.fallback;

import com.njumarket.njumarket.dto.Result;
import com.njumarket.order.client.CommodityClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class CommodityClientFallback implements CommodityClient {

    @Override
    public Result getCommodityById(String commodityId) {
        log.warn("商品服务不可用，触发熔断降级: commodityId={}", commodityId);
        return Result.fail("商品服务暂时不可用，请稍后重试");
    }
}
