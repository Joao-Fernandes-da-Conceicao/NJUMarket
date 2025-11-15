package com.njumarket.message.client.fallback;

import com.njumarket.message.client.CommodityClient;
import com.njumarket.njumarket.dto.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * CommodityClient Fallback 实现
 * 当商品服务不可用时，返回明确的错误信息，阻止发送商品卡片
 */
@Slf4j
@Component
public class CommodityClientFallback implements CommodityClient {
    
    @Override
    public Result getCommodityById(String commodityId) {
        log.warn("商品服务不可用，触发熔断降级: commodityId={}", commodityId);
        return Result.fail("商品服务暂时不可用，无法发送商品卡片，请稍后重试");
    }
}

