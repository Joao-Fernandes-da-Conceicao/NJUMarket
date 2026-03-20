package com.njumarket.ai.storage;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedisAIConversationStorage implements AIConversationStorage {

    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    private static final String STATUS_ACTIVE = "ACTIVE";
    private static final String STATUS_DELETED = "DELETED";
    private static final long CONV_TTL_DAYS = 365;
    private static final long MSG_TTL_DAYS = 365;
    private static final long PROFILE_TTL_DAYS = 365;

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;

    @Override
    public ConvMeta createOrGetConversation(String conversationId, String userId, String title) {
        String key = KEY_CONV + conversationId;
        Boolean exists = redis.hasKey(key);
        if (Boolean.TRUE.equals(exists)) {
            Map<Object, Object> map = redis.opsForHash().entries(key);
            return toConvMeta(conversationId, map);
        }
        LocalDateTime now = LocalDateTime.now();
        redis.opsForHash().put(key, "conversationId", conversationId);
        redis.opsForHash().put(key, "userId", userId);
        redis.opsForHash().put(key, "title", StringUtils.hasText(title) ? title : "新对话");
        redis.opsForHash().put(key, "messageCount", "0");
        redis.opsForHash().put(key, "status", STATUS_ACTIVE);
        redis.opsForHash().put(key, "createdAt", now.format(ISO));
        redis.opsForHash().put(key, "updatedAt", now.format(ISO));
        redis.expire(key, java.time.Duration.ofDays(CONV_TTL_DAYS));

        String userKey = String.format(KEY_USER_CONVS, userId);
        redis.opsForZSet().add(userKey, conversationId, now.atZone(java.time.ZoneId.systemDefault()).toEpochSecond());
        redis.expire(userKey, java.time.Duration.ofDays(CONV_TTL_DAYS));

        log.info("创建 AI 会话: conversationId={}, userId={}, title={}", conversationId, userId, title);
        return new ConvMeta(conversationId, userId, StringUtils.hasText(title) ? title : "新对话", 0, STATUS_ACTIVE, now, now);
    }

    @Override
    public void updateTitle(String conversationId, String title) {
        if (!StringUtils.hasText(conversationId)) return;
        if (title != null && title.length() > 200) title = title.substring(0, 200);
        String key = KEY_CONV + conversationId;
        redis.opsForHash().put(key, "title", title != null ? title : "");
        redis.opsForHash().put(key, "updatedAt", LocalDateTime.now().format(ISO));
    }

    @Override
    public void incrementMessageCount(String conversationId, int increment) {
        if (!StringUtils.hasText(conversationId) || increment <= 0) return;
        String key = KEY_CONV + conversationId;
        redis.opsForHash().increment(key, "messageCount", increment);
        redis.opsForHash().put(key, "updatedAt", LocalDateTime.now().format(ISO));
        Object userIdObj = redis.opsForHash().get(key, "userId");
        if (userIdObj != null) {
            String userKey = String.format(KEY_USER_CONVS, userIdObj.toString());
            redis.opsForZSet().add(userKey, conversationId, System.currentTimeMillis() / 1000.0);
        }
    }

    @Override
    public List<ConvMeta> getUserConversations(String userId, int limit) {
        if (!StringUtils.hasText(userId)) return List.of();
        String userKey = String.format(KEY_USER_CONVS, userId);
        List<String> ids = redis.opsForZSet().reverseRange(userKey, 0, limit - 1)
                .stream().map(String::valueOf).toList();
        List<ConvMeta> result = new ArrayList<>();
        for (String cid : ids) {
            Map<Object, Object> map = redis.opsForHash().entries(KEY_CONV + cid);
            if (map.isEmpty()) continue;
            if (STATUS_DELETED.equals(map.get("status"))) continue;
            result.add(toConvMeta(cid, map));
        }
        return result;
    }

    @Override
    public void deleteConversation(String conversationId, String userId) {
        if (!StringUtils.hasText(conversationId) || !StringUtils.hasText(userId)) return;
        String key = KEY_CONV + conversationId;
        Object u = redis.opsForHash().get(key, "userId");
        if (userId.equals(String.valueOf(u))) {
            redis.opsForHash().put(key, "status", STATUS_DELETED);
            redis.opsForHash().put(key, "updatedAt", LocalDateTime.now().format(ISO));
            log.info("删除 AI 会话: conversationId={}, userId={}", conversationId, userId);
        }
    }

    @Override
    public Optional<MessageRecord> getLatestMessage(String conversationId) {
        if (!StringUtils.hasText(conversationId)) return Optional.empty();
        String listKey = String.format(KEY_CONV_MSGS, conversationId);
        String json = redis.opsForList().index(listKey, -1);
        return parseMessage(json);
    }

    @Override
    public List<MessageRecord> getMessages(String conversationId, String userId, int limit) {
        if (!StringUtils.hasText(conversationId) || !StringUtils.hasText(userId)) return List.of();
        String listKey = String.format(KEY_CONV_MSGS, conversationId);
        List<String> jsons = redis.opsForList().range(listKey, 0, -1);
        if (jsons == null || jsons.isEmpty()) return List.of();
        List<MessageRecord> out = new ArrayList<>();
        for (String j : jsons) {
            parseMessage(j).ifPresent(out::add);
        }
        if (out.size() > limit) {
            out = out.subList(out.size() - limit, out.size());
        }
        return out;
    }

    @Override
    public MessageRecord appendMessage(String conversationId, String userId, String role, String content, String recommendedCommodityIds) {
        String messageId = UUID.randomUUID().toString();
        LocalDateTime now = LocalDateTime.now();
        MessageRecord rec = new MessageRecord(messageId, conversationId, userId, role, content, recommendedCommodityIds, now);
        String json = toJson(rec);
        String listKey = String.format(KEY_CONV_MSGS, conversationId);
        redis.opsForList().rightPush(listKey, json);
        redis.expire(listKey, java.time.Duration.ofDays(MSG_TTL_DAYS));
        return rec;
    }

    @Override
    public long countMessages(String conversationId) {
        if (!StringUtils.hasText(conversationId)) return 0;
        Long n = redis.opsForList().size(String.format(KEY_CONV_MSGS, conversationId));
        return n != null ? n : 0;
    }

    @Override
    public List<MessageRecord> getRecentMessages(String conversationId, int limit) {
        if (!StringUtils.hasText(conversationId)) return List.of();
        String listKey = String.format(KEY_CONV_MSGS, conversationId);
        List<String> jsons = redis.opsForList().range(listKey, -limit, -1);
        if (jsons == null || jsons.isEmpty()) return List.of();
        List<MessageRecord> out = new ArrayList<>();
        for (String j : jsons) {
            parseMessage(j).ifPresent(out::add);
        }
        return out;
    }

    @Override
    public void saveProfileSummary(String userId, String summary) {
        if (!StringUtils.hasText(userId)) return;
        String key = KEY_PROFILE + userId;
        redis.opsForHash().put(key, "userId", userId);
        redis.opsForHash().put(key, "profileSummary", summary != null ? summary : "");
        redis.opsForHash().put(key, "updatedAt", LocalDateTime.now().format(ISO));
        redis.expire(key, java.time.Duration.ofDays(PROFILE_TTL_DAYS));
    }

    @Override
    public Optional<ProfileSummary> getProfileSummary(String userId) {
        if (!StringUtils.hasText(userId)) return Optional.empty();
        Map<Object, Object> map = redis.opsForHash().entries(KEY_PROFILE + userId);
        if (map.isEmpty()) return Optional.empty();
        String summary = (String) map.get("profileSummary");
        String updatedAtStr = (String) map.get("updatedAt");
        LocalDateTime updatedAt = updatedAtStr != null ? LocalDateTime.parse(updatedAtStr, ISO) : LocalDateTime.now();
        return Optional.of(new ProfileSummary(userId, summary != null ? summary : "", updatedAt));
    }

    private ConvMeta toConvMeta(String cid, Map<Object, Object> map) {
        String userId = (String) map.get("userId");
        String title = (String) map.get("title");
        Object mc = map.get("messageCount");
        int messageCount = mc != null ? Integer.parseInt(mc.toString()) : 0;
        String status = (String) map.get("status");
        String ca = (String) map.get("createdAt");
        String ua = (String) map.get("updatedAt");
        LocalDateTime createdAt = ca != null ? LocalDateTime.parse(ca, ISO) : LocalDateTime.now();
        LocalDateTime updatedAt = ua != null ? LocalDateTime.parse(ua, ISO) : LocalDateTime.now();
        return new ConvMeta(cid, userId != null ? userId : "", title != null ? title : "", messageCount,
                status != null ? status : STATUS_ACTIVE, createdAt, updatedAt);
    }

    private String toJson(MessageRecord rec) {
        try {
            return objectMapper.writeValueAsString(Map.of(
                    "messageId", rec.messageId(),
                    "conversationId", rec.conversationId(),
                    "userId", rec.userId(),
                    "role", rec.role(),
                    "content", rec.content() != null ? rec.content() : "",
                    "recommendedCommodityIds", rec.recommendedCommodityIds() != null ? rec.recommendedCommodityIds() : "",
                    "createdAt", rec.createdAt().format(ISO)
            ));
        } catch (Exception e) {
            throw new RuntimeException("序列化消息失败", e);
        }
    }

    private Optional<MessageRecord> parseMessage(String json) {
        if (!StringUtils.hasText(json)) return Optional.empty();
        try {
            Map<String, Object> m = objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
            String messageId = (String) m.get("messageId");
            String conversationId = (String) m.get("conversationId");
            String userId = (String) m.get("userId");
            String role = (String) m.get("role");
            String content = (String) m.get("content");
            String recommendedCommodityIds = (String) m.get("recommendedCommodityIds");
            String ca = (String) m.get("createdAt");
            LocalDateTime createdAt = ca != null ? LocalDateTime.parse(ca, ISO) : LocalDateTime.now();
            return Optional.of(new MessageRecord(messageId, conversationId, userId, role, content, recommendedCommodityIds, createdAt));
        } catch (Exception e) {
            log.warn("解析消息 JSON 失败: {}", e.getMessage());
            return Optional.empty();
        }
    }
}
