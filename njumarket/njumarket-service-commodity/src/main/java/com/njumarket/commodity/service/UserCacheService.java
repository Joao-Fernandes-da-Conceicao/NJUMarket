package com.njumarket.commodity.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.njumarket.commodity.client.AuthClient;
import com.njumarket.njumarket.dto.Result;
import com.njumarket.njumarket.dto.internal.UserInternalDTO;
import com.njumarket.njumarket.dto.internal.UserProfileInternalDTO;
import com.njumarket.njumarket.utils.CacheUtil;
import com.njumarket.njumarket.utils.RedisConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.*;

/**
 * 用户信息缓存服务（Session 下沉化）
 *
 * <p>访问策略：
 * <ol>
 *   <li>先检查共享 Redis 缓存（cache:user:info:{userId} / cache:user:profile:{userId}）</li>
 *   <li>缓存未命中时再调用 Auth Service Feign Client</li>
 *   <li>Feign 结果写回 Redis 缓存</li>
 * </ol>
 *
 * <p>Auth Service 在用户信息变更时会主动失效对应 Key，保证最终一致性。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserCacheService {

    private final CacheUtil cacheUtil;
    private final AuthClient authClient;
    private final ObjectMapper objectMapper;

    private static final TypeReference<List<UserProfileInternalDTO>> PROFILE_LIST_TYPE =
            new TypeReference<>() {};

    /**
     * 获取单个用户基础信息（Redis → Feign 回退）
     */
    public UserInternalDTO getUserById(String userId) {
        if (!StringUtils.hasText(userId)) return null;
        return cacheUtil.getWithFallback(
                RedisConstants.CACHE_USER_INFO_KEY + userId,
                RedisConstants.CACHE_USER_INFO_TTL * 60L,
                UserInternalDTO.class,
                () -> {
                    try {
                        Result result = authClient.getUserById(userId);
                        if (result == null || !result.getSuccess() || result.getData() == null) return null;
                        return objectMapper.convertValue(result.getData(), UserInternalDTO.class);
                    } catch (Exception e) {
                        log.warn("Feign 获取用户信息失败: userId={}, error={}", userId, e.getMessage());
                        return null;
                    }
                }
        );
    }

    /**
     * 批量获取用户档案（nickname / avatar / location 等展示信息）
     *
     * <p>先逐个检查 Redis，剩余 miss 批量调 Feign，结果写回缓存后一并返回。
     *
     * @return Map&lt;userId, UserProfileInternalDTO&gt;
     */
    public Map<String, UserProfileInternalDTO> getUserProfilesByIds(Collection<String> userIds) {
        if (userIds == null || userIds.isEmpty()) return Collections.emptyMap();

        Map<String, UserProfileInternalDTO> result = new LinkedHashMap<>();
        List<String> misses = new ArrayList<>();

        for (String uid : userIds) {
            if (!StringUtils.hasText(uid)) continue;
            UserProfileInternalDTO cached = cacheUtil.get(
                    RedisConstants.CACHE_USER_PROFILE_KEY + uid, UserProfileInternalDTO.class);
            if (cached != null) {
                result.put(uid, cached);
            } else {
                misses.add(uid);
            }
        }

        if (!misses.isEmpty()) {
            try {
                Result feignResult = authClient.getUserProfilesByIds(misses);
                if (feignResult != null && feignResult.getSuccess() && feignResult.getData() != null) {
                    List<UserProfileInternalDTO> profiles =
                            objectMapper.convertValue(feignResult.getData(), PROFILE_LIST_TYPE);
                    for (UserProfileInternalDTO p : profiles) {
                        cacheUtil.set(RedisConstants.CACHE_USER_PROFILE_KEY + p.getUserId(),
                                p, RedisConstants.CACHE_USER_PROFILE_TTL * 60L);
                        result.put(p.getUserId(), p);
                    }
                }
            } catch (Exception e) {
                log.warn("批量 Feign 获取用户档案失败: misses={}, error={}", misses, e.getMessage());
            }
        }

        return result;
    }
}
