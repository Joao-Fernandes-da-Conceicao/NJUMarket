package com.njumarket.message.client.fallback;

import com.njumarket.message.client.OrderClient;
import com.njumarket.njumarket.dto.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * OrderClient Fallback 实现
 * 当订单服务不可用时，返回明确的错误信息，阻止发送订单卡片
 */
@Slf4j
@Component
public class OrderClientFallback implements OrderClient {
    
    @Override
    public Result getOrderById(String orderId) {
        log.warn("订单服务不可用，触发熔断降级: orderId={}", orderId);
        return Result.fail("订单服务暂时不可用，无法发送订单卡片，请稍后重试");
    }
}

