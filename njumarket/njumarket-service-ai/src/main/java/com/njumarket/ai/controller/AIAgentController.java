package com.njumarket.ai.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.njumarket.ai.client.CommodityClient;
import com.njumarket.ai.service.AIAgentService;
import com.njumarket.ai.service.AIConversationService;
import com.njumarket.ai.storage.AIConversationStorage;
import com.njumarket.njumarket.dto.Result;
import com.njumarket.njumarket.dto.internal.CommodityInternalDTO;
import com.njumarket.njumarket.utils.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Tag(name = "AI Agent", description = "AI Agent 对话和搜索相关接口")
@RestController
@RequestMapping("/api/user/ai-agent")
@RequiredArgsConstructor
public class AIAgentController {

    private final AIAgentService aiAgentService;
    private final AIConversationService aiConversationService;
    private final CommodityClient commodityClient;
    private final ObjectMapper objectMapper;

    @Operation(summary = "AI Agent 对话", description = "同步对话，等待完整回复后返回")
    @PostMapping("/chat")
    public Result aiAgentChat(
            @RequestParam String message,
            @RequestParam(required = false) String conversationId) {
        try {
            String userId = SecurityUtils.requireCurrentUserId();
            log.info("AI Agent 对话 - userId={}, cid={}", userId, conversationId);

            if (!StringUtils.hasText(conversationId)) {
                conversationId = UUID.randomUUID().toString();
            }

            String title = message.length() > 50 ? message.substring(0, 50) : message;
            aiConversationService.createOrGetConversation(conversationId, userId, title);

            AIAgentService.ChatResult chatResult = aiAgentService.chat(message, userId, conversationId);
            aiConversationService.incrementMessageCount(conversationId, 2);

            Map<String, Object> result = new HashMap<>();
            result.put("reply", chatResult.getReply());
            result.put("conversationId", conversationId);
            result.put("recommendedCommodities", chatResult.getRecommendedCommodities());
            result.put("hasRecommendations",
                chatResult.getRecommendedCommodities() != null && !chatResult.getRecommendedCommodities().isEmpty());

            return Result.ok("AI Agent 回复成功", result);
        } catch (Exception e) {
            log.error("AI Agent 对话失败: {}", e.getMessage(), e);
            return Result.fail("AI Agent 对话失败: " + e.getMessage());
        }
    }

    @Operation(summary = "AI Agent 流式对话（SSE）", description = "实时 token-by-token 推送，使用 Server-Sent Events")
    @GetMapping(value = "/chat-stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> aiAgentChatStream(
            @RequestParam String message,
            @RequestParam(required = false) String conversationId) {
        try {
            String userId = SecurityUtils.requireCurrentUserId();
            log.info("AI Agent 流式对话 - userId={}, cid={}", userId, conversationId);

            final String finalCid = StringUtils.hasText(conversationId)
                ? conversationId : UUID.randomUUID().toString();

            String title = message.length() > 50 ? message.substring(0, 50) : message;
            aiConversationService.createOrGetConversation(finalCid, userId, title);

            return aiAgentService.chatStream(message, userId, finalCid)
                .doOnComplete(() -> aiConversationService.incrementMessageCount(finalCid, 2));

        } catch (Exception e) {
            log.error("AI Agent 流式对话启动失败: {}", e.getMessage(), e);
            return Flux.just(ServerSentEvent.<String>builder()
                .event("error")
                .data("{\"error\":\"" + e.getMessage().replace("\"", "'") + "\"}")
                .build());
        }
    }

    @Operation(summary = "获取用户的所有 AI 聊天列表", description = "按最后更新时间倒序返回 AI 会话列表")
    @GetMapping("/chats")
    public Result getAIChatList(@RequestParam(defaultValue = "50") Integer limit) {
        try {
            String userId = SecurityUtils.requireCurrentUserId();
            List<AIConversationStorage.ConvMeta> conversations = aiConversationService.getUserConversations(userId, limit);

            List<Map<String, Object>> result = conversations.stream().map(conv -> {
                Map<String, Object> item = new HashMap<>();
                item.put("conversationId", conv.conversationId());
                item.put("title", conv.title());
                item.put("messageCount", conv.messageCount());
                item.put("status", conv.status());
                item.put("createdAt", conv.createdAt());
                item.put("updatedAt", conv.updatedAt());

                aiConversationService.getLatestMessage(conv.conversationId()).ifPresent(lastMsg -> {
                    String preview = lastMsg.content();
                    item.put("lastMessage", preview != null && preview.length() > 60 ? preview.substring(0, 60) + "…" : preview);
                    item.put("lastMessageRole", lastMsg.role());
                    item.put("lastMessageTime", lastMsg.createdAt());
                });
                return item;
            }).collect(Collectors.toList());

            return Result.ok("获取聊天列表成功", result);
        } catch (Exception e) {
            log.error("获取 AI 聊天列表失败: {}", e.getMessage(), e);
            return Result.fail("获取聊天列表失败: " + e.getMessage());
        }
    }

    @Operation(summary = "获取指定会话的消息列表", description = "按时间正序返回，含推荐商品详情")
    @GetMapping("/chats/{conversationId}/messages")
    public Result getAIChatMessages(
            @PathVariable String conversationId,
            @RequestParam(defaultValue = "100") Integer limit) {
        try {
            String userId = SecurityUtils.requireCurrentUserId();
            log.info("获取 chat 消息: userId={}, cid={}", userId, conversationId);

            List<AIConversationStorage.MessageRecord> messages = aiConversationService.getMessages(conversationId, userId);
            if (messages.size() > limit) {
                messages = messages.subList(messages.size() - limit, messages.size());
            }

            Set<String> allCommodityIds = messages.stream()
                .filter(m -> m.recommendedCommodityIds() != null && !m.recommendedCommodityIds().isEmpty())
                .flatMap(m -> aiAgentService.parseRecommendedIds(m.recommendedCommodityIds()).stream())
                .collect(Collectors.toSet());

            Map<String, CommodityInternalDTO> commodityMap = new HashMap<>();
            if (!allCommodityIds.isEmpty()) {
                try {
                    Result cr = commodityClient.getCommoditiesByIds(new ArrayList<>(allCommodityIds));
                    if (cr != null && cr.getSuccess() && cr.getData() != null) {
                        List<CommodityInternalDTO> cList = objectMapper.convertValue(
                            cr.getData(), new TypeReference<List<CommodityInternalDTO>>() {});
                        commodityMap = cList.stream()
                            .collect(Collectors.toMap(CommodityInternalDTO::getCommodityId, c -> c));
                    }
                } catch (Exception e) {
                    log.error("批量获取商品详情失败: {}", e.getMessage(), e);
                }
            }

            final Map<String, CommodityInternalDTO> finalMap = commodityMap;
            List<Map<String, Object>> enriched = messages.stream().map(msg -> {
                Map<String, Object> m = new HashMap<>();
                m.put("messageId", msg.messageId());
                m.put("conversationId", msg.conversationId());
                m.put("role", msg.role());
                m.put("content", msg.content());
                m.put("createdAt", msg.createdAt());

                List<String> ids = aiAgentService.parseRecommendedIds(msg.recommendedCommodityIds());
                m.put("recommendedCommodityIds", ids);
                m.put("recommendedCommodities", ids.stream()
                    .filter(finalMap::containsKey)
                    .map(id -> convertCommodityToMap(finalMap.get(id)))
                    .collect(Collectors.toList()));
                return m;
            }).collect(Collectors.toList());

            return Result.ok("获取消息列表成功", enriched);
        } catch (Exception e) {
            log.error("获取 chat 消息列表失败: {}", e.getMessage(), e);
            return Result.fail("获取消息列表失败: " + e.getMessage());
        }
    }

    @Operation(summary = "获取当前用户的 AI 画像")
    @GetMapping("/profile")
    public Result getUserProfile() {
        try {
            String userId = SecurityUtils.requireCurrentUserId();
            Optional<AIConversationStorage.ProfileSummary> profile = aiAgentService.getUserProfile(userId);
            if (profile.isEmpty()) {
                return Result.ok("暂无画像，继续聊天后自动生成", null);
            }
            Map<String, Object> data = new HashMap<>();
            data.put("userId", profile.get().userId());
            data.put("profileSummary", profile.get().profileSummary());
            data.put("updatedAt", profile.get().updatedAt());
            return Result.ok("获取画像成功", data);
        } catch (Exception e) {
            log.error("获取用户画像失败: {}", e.getMessage(), e);
            return Result.fail("获取画像失败: " + e.getMessage());
        }
    }

    private Map<String, Object> convertCommodityToMap(CommodityInternalDTO dto) {
        Map<String, Object> map = new HashMap<>();
        map.put("commodityId", dto.getCommodityId());
        map.put("sellerId", dto.getSellerId());
        map.put("title", dto.getTitle());
        map.put("description", dto.getDescription());
        map.put("price", dto.getPrice());
        map.put("stock", dto.getStock());
        map.put("category", dto.getCategory());
        map.put("conditionLevel", dto.getConditionLevel());
        map.put("location", dto.getLocation());
        map.put("addressSnapshotFull", dto.getAddressSnapshotFull());
        map.put("addressSnapshotProvince", dto.getAddressSnapshotProvince());
        map.put("addressSnapshotCity", dto.getAddressSnapshotCity());
        map.put("addressSnapshotDistrict", dto.getAddressSnapshotDistrict());
        map.put("commodityStatus", dto.getStatus());
        map.put("publishTime", dto.getCreateTime());
        map.put("createTime", dto.getCreateTime());
        map.put("updateTime", dto.getUpdateTime());
        map.put("clickCount", 0);

        if (dto.getImages() != null && !dto.getImages().isEmpty()) {
            try {
                if (dto.getImages().startsWith("[")) {
                    map.put("images", objectMapper.readValue(dto.getImages(),
                        new TypeReference<List<String>>() {}));
                } else {
                    map.put("images", Collections.singletonList(dto.getImages()));
                }
            } catch (Exception e) {
                map.put("images", Collections.singletonList(dto.getImages()));
            }
        } else {
            map.put("images", Collections.emptyList());
        }
        return map;
    }
}
