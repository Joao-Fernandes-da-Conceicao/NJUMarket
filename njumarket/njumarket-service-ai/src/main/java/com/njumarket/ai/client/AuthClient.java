package com.njumarket.ai.client;

import com.njumarket.njumarket.dto.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * Auth Service Feign Client
 */
@FeignClient(name = "njumarket-service-auth", path = "/api/internal")
public interface AuthClient {

    @GetMapping("/user/{userId}")
    Result getUserById(@PathVariable String userId);
}
