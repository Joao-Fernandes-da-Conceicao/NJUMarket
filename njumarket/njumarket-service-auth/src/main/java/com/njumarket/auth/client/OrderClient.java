package com.njumarket.auth.client;

import com.njumarket.njumarket.dto.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Order Service Feign Client
 * 用于 Auth Service 调用 Order Service
 */
@FeignClient(name = "njumarket-service-order", path = "/api/internal")
public interface OrderClient {
    
    /**
     * 获取用户的订单列表（内部接口）
     * @param userId 用户ID
     * @param role 角色（buyer/seller）
     * @param page 页码
     * @param size 每页大小
     * @param status 订单状态（可选）
     * @return 订单列表
     */
    @GetMapping("/order/user/{userId}")
    Result getUserOrders(@PathVariable String userId,
                        @RequestParam String role,
                        @RequestParam(defaultValue = "1") Integer page,
                        @RequestParam(defaultValue = "100") Integer size,
                        @RequestParam(required = false) String status);
}

