package com.njumarket.ai.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.njumarket.ai.client.CommodityClient;
import com.njumarket.ai.service.AIAgentService;
import com.njumarket.ai.service.AIConversationService;
import com.njumarket.ai.vector.ConversationVectorService;
import com.njumarket.njumarket.dto.Result;
import com.njumarket.njumarket.dto.internal.CommodityInternalDTO;
import com.njumarket.njumarket.utils.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * AI Agent 控制器（用户端）
 */
@Slf4j
@Tag(name = "AI Agent", description = "AI Agent对话和搜索相关接口")
@RestController
@RequestMapping("/api/user/ai-agent")
@RequiredArgsConstructor
public class AIAgentController {
    
    private final AIAgentService aiAgentService;
    private final AIConversationService aiConversationService;
    private final ConversationVectorService conversationVectorService;
    private final CommodityClient commodityClient;
    
    @Autowired(required = false)
    private ObjectMapper objectMapper;
    
    @Operation(summary = "AI Agent 对话", description = "与 AI Agent 进行智能对话，支持多轮对话和上下文理解")
    @PostMapping("/chat")
    public Result aiAgentChat(
            @RequestParam String message,
            @RequestParam(required = false) String conversationId) {
        try {
            String userId = SecurityUtils.requireCurrentUserId();
            
            log.info("AI Agent 对话 - userId: {}, conversationId: {}, message: {}", 
                userId, conversationId, message);
            
            if (conversationId == null || conversationId.trim().isEmpty()) {
                conversationId = UUID.randomUUID().toString();
            }
            
            String title = message.length() > 50 ? message.substring(0, 50) : message;
            aiConversationService.createOrGetConversation(conversationId, userId, title);
            
            AIAgentService.ChatResult chatResult = aiAgentService.chat(
                message, userId, conversationId);
            
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
    
    @Operation(summary = "AI Agent 流式对话", description = "与 AI Agent 进行流式对话，实时返回回复内容")
    @GetMapping(value = "/chat-stream", produces = "text/event-stream")
    public SseEmitter aiAgentChatStream(
            @RequestParam String message,
            @RequestParam(required = false) String conversationId) {
        try {
            String userId = SecurityUtils.requireCurrentUserId();
            
            log.info("AI Agent 流式对话 - userId: {}, conversationId: {}, message: {}", 
                userId, conversationId, message);
            
            final String finalConversationId = (conversationId == null || conversationId.trim().isEmpty()) 
                ? UUID.randomUUID().toString() 
                : conversationId;
            
            String title = message.length() > 50 ? message.substring(0, 50) : message;
            aiConversationService.createOrGetConversation(finalConversationId, userId, title);
            
            SseEmitter emitter = new SseEmitter(60000L);
            
            AIAgentService.StreamChatCallback callback = new AIAgentService.StreamChatCallback() {
                @Override
                public void onToken(String token) {
                    try {
                        emitter.send(SseEmitter.event()
                            .data(token)
                            .name("token"));
                    } catch (Exception e) {
                        log.error("发送流式 token 失败: {}", e.getMessage(), e);
                        emitter.completeWithError(e);
                    }
                }
                
                @Override
                public void onComplete(String fullReply, List<com.njumarket.ai.tool.SearchCommoditiesTool.CommodityDTO> recommendedCommodities) {
                    try {
                        Map<String, Object> completeData = new HashMap<>();
                        completeData.put("reply", fullReply);
                        completeData.put("conversationId", finalConversationId);
                        completeData.put("recommendedCommodities", recommendedCommodities);
                        completeData.put("hasRecommendations", 
                            recommendedCommodities != null && !recommendedCommodities.isEmpty());
                        
                        emitter.send(SseEmitter.event()
                            .data(completeData)
                            .name("complete"));
                        
                        emitter.complete();
                        
                        aiConversationService.incrementMessageCount(finalConversationId, 2);
                    } catch (Exception e) {
                        log.error("发送流式完成事件失败: {}", e.getMessage(), e);
                        emitter.completeWithError(e);
                    }
                }
                
                @Override
                public void onError(String error) {
                    try {
                        Map<String, Object> errorData = new HashMap<>();
                        errorData.put("error", error);
                        emitter.send(SseEmitter.event()
                            .data(errorData)
                            .name("error"));
                        emitter.completeWithError(new RuntimeException(error));
                    } catch (Exception e) {
                        log.error("发送流式错误事件失败: {}", e.getMessage(), e);
                        emitter.completeWithError(e);
                    }
                }
            };
            
            CompletableFuture.runAsync(() -> {
                try {
                    aiAgentService.chatStream(message, userId, finalConversationId, callback);
                } catch (Exception e) {
                    log.error("AI Agent 流式对话异常: {}", e.getMessage(), e);
                    callback.onError("AI 对话异常: " + e.getMessage());
                }
            });
            
            return emitter;
            
        } catch (Exception e) {
            log.error("AI Agent 流式对话失败: {}", e.getMessage(), e);
            throw new RuntimeException("AI Agent 流式对话失败: " + e.getMessage());
        }
    }
    
    @Operation(summary = "AI Agent 智能搜索", description = "使用 AI Agent 进行智能搜索，返回搜索结果和 AI 解释")
    @GetMapping("/search")
    public Result aiAgentSearch(
            @RequestParam String query,
            @RequestParam(required = false) String conversationId) {
        try {
            String userId = SecurityUtils.requireCurrentUserId();
            
            log.info("AI Agent 智能搜索 - userId: {}, conversationId: {}, query: {}", 
                userId, conversationId, query);
            
            AIAgentService.AgentSearchResult agentResult = 
                aiAgentService.intelligentSearch(query, userId, conversationId);
            
            Map<String, Object> result = new HashMap<>();
            result.put("commodities", agentResult.getCommodities());
            result.put("explanation", agentResult.getExplanation());
            result.put("originalQuery", agentResult.getOriginalQuery());
            result.put("enhancedQuery", agentResult.getEnhancedQuery());
            result.put("total", agentResult.getCommodities() != null ? agentResult.getCommodities().size() : 0);
            
            return Result.ok("AI Agent 智能搜索成功", result);
            
        } catch (Exception e) {
            log.error("AI Agent 智能搜索失败: {}", e.getMessage(), e);
            return Result.fail("AI Agent 智能搜索失败: " + e.getMessage());
        }
    }
    
    @Operation(summary = "获取用户的所有AI聊天列表", description = "获取用户的所有AI聊天会话列表，按最后消息时间倒序")
    @GetMapping("/chats")
    public Result getAIChatList(
            @RequestParam(defaultValue = "50") Integer limit) {
        try {
            String userId = SecurityUtils.requireCurrentUserId();
            
            log.info("获取用户AI聊天列表 - userId: {}, limit: {}", userId, limit);
            
            List<ConversationVectorService.ChatInfo> chatList = 
                conversationVectorService.getUserChatList(userId, limit);
            
            return Result.ok("获取聊天列表成功", chatList);
            
        } catch (Exception e) {
            log.error("获取AI聊天列表失败: {}", e.getMessage(), e);
            return Result.fail("获取AI聊天列表失败: " + e.getMessage());
        }
    }
    
    @Operation(summary = "获取指定chat的消息列表", description = "获取指定对话ID的所有消息，按时间正序")
    @GetMapping("/chats/{conversationId}/messages")
    public Result getAIChatMessages(
            @PathVariable String conversationId,
            @RequestParam(defaultValue = "100") Integer limit) {
        try {
            String userId = SecurityUtils.requireCurrentUserId();
            
            log.info("获取chat消息列表 - userId: {}, conversationId: {}, limit: {}", 
                userId, conversationId, limit);
            
            List<ConversationVectorService.ConversationMessage> messages = 
                conversationVectorService.getChatMessages(conversationId, userId, limit);
            
            // 收集所有推荐商品的ID
            Set<String> allCommodityIds = messages.stream()
                .filter(msg -> msg.getRecommendedCommodityIds() != null && !msg.getRecommendedCommodityIds().isEmpty())
                .flatMap(msg -> msg.getRecommendedCommodityIds().stream())
                .collect(Collectors.toSet());
            
            // 批量获取商品详情
            Map<String, CommodityInternalDTO> commodityMap = new HashMap<>();
            if (!allCommodityIds.isEmpty()) {
                try {
                    Result commoditiesResult = commodityClient.getCommoditiesByIds(new ArrayList<>(allCommodityIds));
                    if (commoditiesResult != null && commoditiesResult.getSuccess() && commoditiesResult.getData() != null) {
                        List<CommodityInternalDTO> commodities;
                        Object data = commoditiesResult.getData();
                        if (objectMapper != null) {
                            commodities = objectMapper.convertValue(data, 
                                new TypeReference<List<CommodityInternalDTO>>() {});
                        } else {
                            @SuppressWarnings("unchecked")
                            List<Map<String, Object>> dataList = (List<Map<String, Object>>) data;
                            commodities = dataList.stream()
                                .map(this::mapToCommodityInternalDTO)
                                .collect(Collectors.toList());
                        }
                        commodityMap = commodities.stream()
                            .collect(Collectors.toMap(CommodityInternalDTO::getCommodityId, c -> c));
                    }
                } catch (Exception e) {
                    log.error("批量获取商品详情失败: {}", e.getMessage(), e);
                }
            }
            
            // 构建最终的消息列表，包含商品详情
            final Map<String, CommodityInternalDTO> finalCommodityMap = commodityMap;
            List<Map<String, Object>> enrichedMessages = messages.stream()
                .map(msg -> {
                    Map<String, Object> msgMap = new HashMap<>();
                    msgMap.put("conversationId", msg.getConversationId());
                    msgMap.put("messageId", msg.getMessageId());
                    msgMap.put("content", msg.getContent());
                    msgMap.put("role", msg.getRole());
                    msgMap.put("similarity", msg.getSimilarity());
                    msgMap.put("recommendedCommodityIds", msg.getRecommendedCommodityIds());
                    msgMap.put("createdAt", msg.getCreatedAt());
                    
                    // 填充推荐商品详情
                    List<Map<String, Object>> recommendedCommodities = new ArrayList<>();
                    if (msg.getRecommendedCommodityIds() != null && !msg.getRecommendedCommodityIds().isEmpty()) {
                        recommendedCommodities = msg.getRecommendedCommodityIds().stream()
                            .filter(finalCommodityMap::containsKey)
                            .map(id -> convertCommodityToMap(finalCommodityMap.get(id)))
                            .collect(Collectors.toList());
                    }
                    msgMap.put("recommendedCommodities", recommendedCommodities);
                    
                    return msgMap;
                })
                .collect(Collectors.toList());
            
            return Result.ok("获取消息列表成功", enrichedMessages);
            
        } catch (Exception e) {
            log.error("获取chat消息列表失败: {}", e.getMessage(), e);
            return Result.fail("获取chat消息列表失败: " + e.getMessage());
        }
    }
    
    /**
     * 将 Map 转换为 CommodityInternalDTO（备用方案）
     */
    private CommodityInternalDTO mapToCommodityInternalDTO(Map<String, Object> map) {
        CommodityInternalDTO dto = new CommodityInternalDTO();
        dto.setCommodityId((String) map.get("commodityId"));
        dto.setSellerId((String) map.get("sellerId"));
        dto.setTitle((String) map.get("title"));
        dto.setDescription((String) map.get("description"));
        if (map.get("price") != null) {
            if (map.get("price") instanceof Number) {
                dto.setPrice(new java.math.BigDecimal(map.get("price").toString()));
            } else {
                dto.setPrice((java.math.BigDecimal) map.get("price"));
            }
        }
        if (map.get("stock") != null) {
            dto.setStock(map.get("stock") instanceof Integer ? 
                (Integer) map.get("stock") : ((Number) map.get("stock")).intValue());
        }
        dto.setCategory((String) map.get("category"));
        dto.setConditionLevel((String) map.get("conditionLevel"));
        dto.setStatus((String) map.get("status"));
        dto.setLocation((String) map.get("location"));
        dto.setAddressSnapshotFull((String) map.get("addressSnapshotFull"));
        dto.setAddressSnapshotProvince((String) map.get("addressSnapshotProvince"));
        dto.setAddressSnapshotCity((String) map.get("addressSnapshotCity"));
        dto.setAddressSnapshotDistrict((String) map.get("addressSnapshotDistrict"));
        dto.setImages((String) map.get("images"));
        if (map.get("createTime") != null) {
            dto.setCreateTime((java.time.LocalDateTime) map.get("createTime"));
        }
        return dto;
    }
    
    /**
     * 将 CommodityInternalDTO 转换为 Map（用于返回给前端）
     * 转换字段以匹配前端 CommodityCard.vue 组件的期望格式
     */
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
        map.put("commodityStatus", dto.getStatus()); // CommodityInternalDTO 使用 status 字段
        
        // 前端期望 publishTime 字段，而不是 createTime
        map.put("publishTime", dto.getCreateTime());
        map.put("createTime", dto.getCreateTime());
        map.put("updateTime", dto.getUpdateTime());
        
        // 将 images 字符串转换为数组格式（前端 CommodityCard.vue 期望数组）
        if (dto.getImages() != null && !dto.getImages().isEmpty()) {
            try {
                // 尝试将 JSON 字符串解析为数组
                if (dto.getImages().startsWith("[")) {
                    // 已经是 JSON 数组格式
                    List<String> imageList = objectMapper.readValue(dto.getImages(), new TypeReference<List<String>>() {});
                    map.put("images", imageList);
                } else {
                    // 单个 URL 字符串，转换为数组
                    map.put("images", Collections.singletonList(dto.getImages()));
                }
            } catch (Exception e) {
                log.warn("解析商品图片失败，使用单图格式: commodityId={}, error={}", dto.getCommodityId(), e.getMessage());
                // 如果解析失败，作为单个 URL 放入数组
                map.put("images", Collections.singletonList(dto.getImages()));
            }
        } else {
            map.put("images", Collections.emptyList());
        }
        
        // 添加默认值（前端需要的其他字段）
        map.put("clickCount", 0); // 默认浏览次数为0（AI推荐不需要显示真实浏览次数）
        
        return map;
    }
}

