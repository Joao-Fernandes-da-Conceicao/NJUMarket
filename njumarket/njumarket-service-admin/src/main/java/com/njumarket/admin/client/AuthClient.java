package com.njumarket.admin.client;

import com.njumarket.njumarket.dto.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@FeignClient(name = "njumarket-service-auth", path = "/api/internal")
public interface AuthClient {

    @GetMapping("/user/{userId}")
    Result getUserById(@PathVariable String userId);

    @GetMapping("/user/batch")
    Result getUsersByIds(@RequestParam List<String> userIds);

    @GetMapping("/user/profile/batch")
    Result getUserProfilesByIds(@RequestParam List<String> userIds);

    @PutMapping("/user/{userId}/full")
    Result updateUserFull(@PathVariable String userId, @RequestBody Map<String, Object> payload);

    @DeleteMapping("/user/{userId}")
    Result deleteUser(@PathVariable String userId);
    
    @PutMapping("/user/{userId}/status")
    Result updateUserStatus(@PathVariable String userId, @RequestParam String status);
    
    @PutMapping("/user/{userId}")
    Result updateUserBasic(@PathVariable String userId,
                          @RequestParam(required = false) String nickname,
                          @RequestParam(required = false) String phone,
                          @RequestParam(required = false) String email);
    
    /**
     * 查询用户列表（管理端内部接口）
     */
    @GetMapping("/users")
    Result listUsers(@RequestParam(defaultValue = "1") Integer page,
                    @RequestParam(defaultValue = "10") Integer size,
                    @RequestParam(required = false) String keyword,
                    @RequestParam(required = false) String accountStatus,
                    @RequestParam(required = false) String sortProp,
                    @RequestParam(required = false) String sortOrder);
}

