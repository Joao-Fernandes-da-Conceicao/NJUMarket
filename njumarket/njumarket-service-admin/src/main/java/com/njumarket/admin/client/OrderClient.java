package com.njumarket.admin.client;

import com.njumarket.njumarket.dto.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@FeignClient(name = "njumarket-service-order", path = "/api/internal")
public interface OrderClient {

    @GetMapping("/order/{orderId}")
    Result getOrderById(@PathVariable String orderId);

    @PutMapping("/order/{orderId}/full")
    Result updateOrderFull(@PathVariable String orderId,
                          @RequestBody Map<String, Object> payload);

    @DeleteMapping("/order/{orderId}")
    Result deleteOrder(@PathVariable String orderId);
}

