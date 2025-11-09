package com.njumarket.message.client;


import com.njumarket.njumarket.dto.Result;
// User 和 UserProfile 实体不再直接引用，通过 Feign Client 返回 DTO
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * Auth Service Feign Client
 * 用于Message Service调用Auth Service
 */
@FeignClient(name = "njumarket-service-auth", path = "/api/internal")
public interface AuthClient {
    
    /**
     * 根据ID查询用户（内部接口）
     */
    @GetMapping("/user/{userId}")
    Result getUserById(@PathVariable String userId);
    
    /**
     * 批量查询用户（内部接口）
     */
    @GetMapping("/user/batch")
    Result getUsersByIds(@RequestParam List<String> userIds);
    
    /**
     * 批量查询用户档案（内部接口）
     */
    @GetMapping("/user/profile/batch")
    Result getUserProfilesByIds(@RequestParam List<String> userIds);
}

