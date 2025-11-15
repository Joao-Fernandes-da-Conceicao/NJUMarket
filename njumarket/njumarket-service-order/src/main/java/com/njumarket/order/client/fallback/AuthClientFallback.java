package com.njumarket.order.client.fallback;

import com.njumarket.njumarket.dto.Result;
import com.njumarket.order.client.AuthClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Auth Client Fallback
 * 当认证服务不可用时的降级处理
 */
@Slf4j
@Component
public class AuthClientFallback implements AuthClient {
    
    @Override
    public Result getUserById(String userId) {
        log.warn("认证服务不可用，触发熔断降级: userId={}", userId);
        return Result.fail("认证服务暂时不可用，请稍后重试");
    }
    
    @Override
    public Result getUsersByIds(List<String> userIds) {
        log.warn("认证服务不可用，触发熔断降级: userIds={}", userIds);
        return Result.fail("认证服务暂时不可用，请稍后重试");
    }
    
    @Override
    public Result getUserProfilesByIds(List<String> userIds) {
        log.warn("认证服务不可用，触发熔断降级: userIds={}", userIds);
        return Result.fail("认证服务暂时不可用，请稍后重试");
    }
    
    @Override
    public Result setOrderReminderStatus(String userId, String role, Boolean hasNew) {
        log.warn("认证服务不可用，触发熔断降级: userId={}, role={}, hasNew={}", userId, role, hasNew);
        // 订单提醒状态设置失败不影响主流程，返回成功但记录日志
        log.warn("订单提醒状态设置失败，但不影响订单流程");
        return Result.ok("提醒状态设置失败，但不影响订单流程");
    }
}

