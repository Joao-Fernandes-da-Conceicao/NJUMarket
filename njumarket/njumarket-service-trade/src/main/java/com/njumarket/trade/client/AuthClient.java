package com.njumarket.trade.client;

import com.njumarket.njumarket.dto.Result;
import com.njumarket.trade.client.fallback.AuthClientFallback;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * Auth Service Feign Client（商品检索 / 订单 / 用户缓存等共用）
 */
@FeignClient(name = "njumarket-service-auth",
        path = "/api/internal",
        fallback = AuthClientFallback.class)
public interface AuthClient {

    @GetMapping("/user/{userId}")
    Result getUserById(@PathVariable String userId);

    @GetMapping("/user/batch")
    Result getUsersByIds(@RequestParam List<String> userIds);

    @GetMapping("/user/profile/batch")
    Result getUserProfilesByIds(@RequestParam List<String> userIds);

    @PutMapping("/user/{userId}/order-reminder")
    Result setOrderReminderStatus(@PathVariable String userId,
                                  @RequestParam String role,
                                  @RequestParam Boolean hasNew);

    @GetMapping("/address/{addressId}")
    Result getAddressById(@PathVariable String addressId);

    @GetMapping("/address/default")
    Result getDefaultAddress(@RequestParam String userId);

    @GetMapping("/user/{userId}/profile-vector")
    Result getUserProfileVector(@PathVariable String userId);

    @GetMapping("/user/search-similar")
    Result searchSimilarUsers(@RequestParam String queryVector, @RequestParam(defaultValue = "10") Integer limit);
}
