package com.njumarket.notification.client.fallback;

import com.njumarket.notification.client.OrderQueryClient;
import com.njumarket.njumarket.dto.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * OrderQueryClient Fallback 实现
 * 当订单查询服务不可用时，返回空列表，不影响增量轮询
 */
@Slf4j
@Component
public class OrderQueryClientFallback implements OrderQueryClient {
    
    @Override
    public Result getOrdersBatchStatus(List<String> orderIds) {
        log.warn("订单查询服务不可用，触发熔断降级: getOrdersBatchStatus, orderIds={}", orderIds);
        return Result.ok("订单查询服务暂时不可用，无法批量获取订单状态", Collections.emptyList());
    }
}

