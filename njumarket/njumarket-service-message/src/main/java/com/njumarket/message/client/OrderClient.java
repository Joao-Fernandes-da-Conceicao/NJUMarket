package com.njumarket.message.client;

import com.njumarket.njumarket.dto.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

/**
 * Order Service Feign Client
 * 用于Message Service调用Order Service
 */
@FeignClient(name = "njumarket-service-order", path = "/api/internal")
public interface OrderClient {
    
    /**
     * 根据ID查询订单（内部接口）
     */
    @GetMapping("/order/{orderId}")
    Result getOrderById(@PathVariable String orderId);
}

