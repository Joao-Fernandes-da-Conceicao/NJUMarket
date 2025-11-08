package com.njumarket.commodity.client;

import com.njumarket.njumarket.dto.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * Auth Service Feign Client
 * 用于Commodity Service调用Auth Service
 */
@FeignClient(name = "njumarket-service-auth", path = "/api/internal")
public interface AuthClient {

    /**
     * 根据用户ID查询用户（内部接口）
     */
    @GetMapping("/user/{userId}")
    Result getUserById(@PathVariable String userId);

    /**
     * 批量查询用户档案（内部接口）
     */
    @GetMapping("/user/profile/batch")
    Result getUserProfilesByIds(@RequestParam List<String> userIds);
}

