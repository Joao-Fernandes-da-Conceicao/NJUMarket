package com.njumarket.ai.storage;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Optional;

/**
 * 用户画像摘要仅存 Redis（非权威业务数据）；会话与消息见关系库 {@link JpaAIConversationStorage}。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RedisProfileStorage {

    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final StringRedisTemplate redis;

    public void saveProfileSummary(String userId, String summary) {
        if (!StringUtils.hasText(userId)) return;
        String key = AIConversationStorage.KEY_PROFILE + userId;
        redis.opsForHash().put(key, "userId", userId);
        redis.opsForHash().put(key, "profileSummary", summary != null ? summary : "");
        redis.opsForHash().put(key, "updatedAt", LocalDateTime.now().format(ISO));
        redis.expire(key, java.time.Duration.ofDays(365));
    }

    public Optional<AIConversationStorage.ProfileSummary> getProfileSummary(String userId) {
        if (!StringUtils.hasText(userId)) return Optional.empty();
        Map<Object, Object> map = redis.opsForHash().entries(AIConversationStorage.KEY_PROFILE + userId);
        if (map.isEmpty()) return Optional.empty();
        String summary = (String) map.get("profileSummary");
        String updatedAtStr = (String) map.get("updatedAt");
        LocalDateTime updatedAt = updatedAtStr != null ? LocalDateTime.parse(updatedAtStr, ISO) : LocalDateTime.now();
        return Optional.of(new AIConversationStorage.ProfileSummary(userId, summary != null ? summary : "", updatedAt));
    }
}
