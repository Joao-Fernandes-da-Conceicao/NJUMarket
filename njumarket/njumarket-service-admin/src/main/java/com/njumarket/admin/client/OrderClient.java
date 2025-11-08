package com.njumarket.admin.client;

import com.njumarket.njumarket.dto.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 订单服务Feign客户端（管理端）
 * 统一使用内部API接口，路径前缀为 /api/internal
 */
@FeignClient(name = "njumarket-service-order", path = "/api/internal")
public interface OrderClient {

    /**
     * 查询订单列表（管理端内部接口）
     */
    @GetMapping("/orders")
    Result listOrders(@RequestParam(defaultValue = "1") Integer page,
                     @RequestParam(defaultValue = "10") Integer size,
                     @RequestParam(required = false) String keyword,
                     @RequestParam(required = false) String status,
                     @RequestParam(required = false) String sellerVisibility,
                     @RequestParam(required = false) String buyerVisibility,
                     @RequestParam(required = false) String sortProp,
                     @RequestParam(required = false) String sortOrder);

    /**
     * 根据ID查询订单详情（内部接口）
     */
    @GetMapping("/order/{orderId}")
    Result getOrderById(@PathVariable String orderId);

    /**
     * 完整更新订单（内部接口）
     */
    @PutMapping("/order/{orderId}/full")
    Result updateOrderFull(@PathVariable String orderId,
                          @RequestBody Map<String, Object> payload);

    /**
     * 删除订单（内部接口）
     */
    @DeleteMapping("/order/{orderId}")
    Result deleteOrder(@PathVariable String orderId);
}

