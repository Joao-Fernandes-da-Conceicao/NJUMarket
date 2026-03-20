package com.njumarket.ai.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.njumarket.ai.config.MilvusProperties;
import com.njumarket.ai.storage.AIConversationStorage;
import com.njumarket.ai.tool.SearchCommoditiesTool;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.MemoryId;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * AI Agent 服务（无关系库，Redis + Milvus）
 *
 * <p>对话流程：
 * <ol>
 *   <li>用户画像 → Redis 摘要 + Milvus 语义 chunk</li>
 *   <li>ChatMemory → Milvus 语义召回</li>
 *   <li>Agent 循环（ES/向量/混合检索）</li>
 *   <li>消息持久化 → Redis 列表 + Milvus 向量</li>
 *   <li>画像更新 → Redis + Milvus chunk</li>
 * </ol>
 */
@Slf4j
@Service
public class AIAgentService {

    private static final int MAX_MEMORY_MESSAGES = 15;
    private static final int SUMMARY_TRIGGER = 20;
    private static final int PROFILE_UPDATE_INTERVAL = 10;
    private static final int MAX_TOOL_ITERATIONS = 5;
    private static final int MEMORY_RECALL_LIMIT = 12;

    private final ChatModel chatLanguageModel;
    private final SearchCommoditiesTool searchCommoditiesTool;
    private final AIConversationStorage storage;
    private final ObjectMapper objectMapper;
    private final MilvusVectorService milvusVectorService;
    private final MilvusProperties milvusProperties;

    private final Map<String, ChatMemory> chatMemoryMap = new ConcurrentHashMap<>();
    private final ShoppingAssistant shoppingAssistant;

    public interface ShoppingAssistant {
        String chat(@MemoryId String memoryId, @dev.langchain4j.service.UserMessage String userMessage);
        Flux<String> chatStream(@MemoryId String memoryId, @dev.langchain4j.service.UserMessage String userMessage);
    }

    public AIAgentService(ChatModel chatLanguageModel,
                          StreamingChatModel streamingChatLanguageModel,
                          SearchCommoditiesTool searchCommoditiesTool,
                          AIConversationStorage storage,
                          ObjectMapper objectMapper,
                          MilvusVectorService milvusVectorService,
                          MilvusProperties milvusProperties) {
        this.chatLanguageModel = chatLanguageModel;
        this.searchCommoditiesTool = searchCommoditiesTool;
        this.storage = storage;
        this.objectMapper = objectMapper;
        this.milvusVectorService = milvusVectorService;
        this.milvusProperties = milvusProperties;

        this.shoppingAssistant = AiServices.builder(ShoppingAssistant.class)
            .chatModel(chatLanguageModel)
            .streamingChatModel(streamingChatLanguageModel)
            .tools(searchCommoditiesTool)
            .maxSequentialToolsInvocations(MAX_TOOL_ITERATIONS)
            .chatMemoryProvider(chatMemoryId -> {
                String id = chatMemoryId != null ? chatMemoryId.toString() : "default";
                return chatMemoryMap.computeIfAbsent(id,
                    k -> MessageWindowChatMemory.withMaxMessages(MAX_MEMORY_MESSAGES));
            })
            .systemMessageProvider(chatMemoryId -> buildBaseSystemPrompt())
            .build();
    }

    public static class ChatResult {
        private final String reply;
        private final List<SearchCommoditiesTool.CommodityDTO> recommendedCommodities;

        public ChatResult(String reply, List<SearchCommoditiesTool.CommodityDTO> commodities) {
            this.reply = reply;
            this.recommendedCommodities = commodities != null ? commodities : new ArrayList<>();
        }

        public String getReply() { return reply; }
        public List<SearchCommoditiesTool.CommodityDTO> getRecommendedCommodities() { return recommendedCommodities; }
    }

    public ChatResult chat(String userMessage, String userId, String conversationId) {
        if (!StringUtils.hasText(userMessage) || !StringUtils.hasText(userId)) {
            return new ChatResult("抱歉，我无法理解您的问题。", null);
        }
        try {
            searchCommoditiesTool.clearForNewTurn();
            String memoryId = StringUtils.hasText(conversationId) ? conversationId : userId + "_default";

            initChatMemory(memoryId, conversationId, userId);
            rebuildMemoryBySimilarity(memoryId, conversationId, userId, userMessage);
            injectSemanticProfileContext(memoryId, userId, userMessage);

            ChatMemory chatMemory = chatMemoryMap.get(memoryId);
            if (chatMemory.messages().size() >= SUMMARY_TRIGGER) {
                summarizeAndCompress(chatMemory, chatMemory.messages(), memoryId);
            }

            String assistantReply = shoppingAssistant.chat(memoryId, userMessage);
            List<SearchCommoditiesTool.CommodityDTO> recommended = searchCommoditiesTool.getRecommendedCommoditiesForResponse();

            if (StringUtils.hasText(conversationId)) {
                persistMessages(conversationId, userId, userMessage, assistantReply, recommended);
                checkAndUpdateProfile(userId, conversationId);
            }

            log.info("AI 对话完成: userId={}, cid={}, recommended={}", userId, conversationId,
                recommended != null ? recommended.size() : 0);
            return new ChatResult(assistantReply, recommended);

        } catch (Exception e) {
            log.error("AI 对话失败: userId={}, error={}", userId, e.getMessage(), e);
            return new ChatResult("抱歉，我遇到了一些问题，请稍后再试。", null);
        }
    }

    public Flux<ServerSentEvent<String>> chatStream(String userMessage, String userId, String conversationId) {
        if (!StringUtils.hasText(userMessage) || !StringUtils.hasText(userId)) {
            return Flux.just(errorEvent("用户消息或用户ID不能为空"));
        }

        try {
            searchCommoditiesTool.clearForNewTurn();
            String memoryId = StringUtils.hasText(conversationId) ? conversationId : userId + "_default";

            initChatMemory(memoryId, conversationId, userId);
            rebuildMemoryBySimilarity(memoryId, conversationId, userId, userMessage);
            injectSemanticProfileContext(memoryId, userId, userMessage);

            ChatMemory chatMemory = chatMemoryMap.get(memoryId);
            if (chatMemory.messages().size() >= SUMMARY_TRIGGER) {
                summarizeAndCompress(chatMemory, chatMemory.messages(), memoryId);
            }

            StringBuilder fullReply = new StringBuilder();

            return shoppingAssistant.chatStream(memoryId, userMessage)
                .map(token -> {
                    fullReply.append(token);
                    return ServerSentEvent.<String>builder()
                        .event("token")
                        .data(token)
                        .build();
                })
                .concatWith(buildCompleteEvent(conversationId, userId, userMessage, fullReply))
                .onErrorResume(e -> {
                    log.error("AI 流式对话失败: userId={}, cid={}, error={}", userId, conversationId, e.getMessage(), e);
                    return Flux.just(errorEvent(e.getMessage()));
                });

        } catch (Exception e) {
            log.error("AI 流式对话启动失败: userId={}, error={}", userId, e.getMessage(), e);
            return Flux.just(errorEvent(e.getMessage()));
        }
    }

    /** 获取用户画像（供 Controller 返回） */
    public Optional<AIConversationStorage.ProfileSummary> getUserProfile(String userId) {
        return storage.getProfileSummary(userId);
    }

    /** 解析推荐商品 ID 列表（JSON 数组字符串） */
    public List<String> parseRecommendedIds(String json) {
        if (!StringUtils.hasText(json)) return Collections.emptyList();
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    private void initChatMemory(String memoryId, String conversationId, String userId) {
        chatMemoryMap.computeIfAbsent(memoryId, id -> {
            ChatMemory mem = MessageWindowChatMemory.withMaxMessages(MAX_MEMORY_MESSAGES);
            injectUserProfile(mem, userId);
            return mem;
        });
    }

    private void rebuildMemoryBySimilarity(String memoryId, String conversationId, String userId, String userMessage) {
        try {
            ChatMemory newMemory = MessageWindowChatMemory.withMaxMessages(MAX_MEMORY_MESSAGES);
            injectUserProfile(newMemory, userId);
            if (StringUtils.hasText(conversationId) && StringUtils.hasText(userMessage)) {
                int topK = Math.max(milvusProperties.getMemoryTopK(), MEMORY_RECALL_LIMIT);
                List<Map<String, Object>> hits = milvusVectorService.searchConversationByText(userMessage, conversationId, topK);
                int added = 0;
                for (Map<String, Object> hit : hits) {
                    Object entityRaw = hit.get("entity");
                    if (!(entityRaw instanceof Map<?, ?> entity)) continue;
                    Object contentObj = entity.get("content");
                    if (contentObj == null) continue;
                    String content = String.valueOf(contentObj);
                    if (content.startsWith("role=user\ncontent=")) {
                        newMemory.add(UserMessage.from(content.substring("role=user\ncontent=".length())));
                        added++;
                    } else if (content.startsWith("role=assistant\ncontent=")) {
                        newMemory.add(AiMessage.from(content.substring("role=assistant\ncontent=".length())));
                        added++;
                    }
                    if (added >= MAX_MEMORY_MESSAGES - 3) break;
                }
                log.debug("Milvus 语义记忆召回完成: cid={}, recalled={}", conversationId, added);
            }
            chatMemoryMap.put(memoryId, newMemory);
        } catch (Exception e) {
            log.warn("基于 Milvus 重建语义记忆失败，回退空记忆: cid={}, error={}", conversationId, e.getMessage());
            ChatMemory fallback = MessageWindowChatMemory.withMaxMessages(MAX_MEMORY_MESSAGES);
            injectUserProfile(fallback, userId);
            chatMemoryMap.put(memoryId, fallback);
        }
    }

    private void injectUserProfile(ChatMemory chatMemory, String userId) {
        try {
            storage.getProfileSummary(userId).ifPresent(profile -> {
                if (StringUtils.hasText(profile.profileSummary())) {
                    chatMemory.add(SystemMessage.from(
                        buildBaseSystemPrompt() + "\n\n=== 用户画像 ===\n" + profile.profileSummary()
                        + "\n请根据以上偏好提供个性化推荐。"));
                }
            });
        } catch (Exception e) {
            log.debug("注入用户画像失败（可选）: userId={}, error={}", userId, e.getMessage());
        }
    }

    private void injectSemanticProfileContext(String memoryId, String userId, String userMessage) {
        try {
            List<Map<String, Object>> hits = milvusVectorService.searchUserProfileByText(userMessage, 3);
            if (hits == null || hits.isEmpty()) return;
            String context = hits.stream()
                    .map(hit -> {
                        Object entityRaw = hit.get("entity");
                        if (!(entityRaw instanceof Map<?, ?> entity)) return null;
                        Object bizId = entity.get("bizId");
                        if (bizId == null || !userId.equals(String.valueOf(bizId))) return null;
                        Object content = entity.get("content");
                        Object score = hit.get("score");
                        if (content == null) return null;
                        return "- [" + score + "] " + content;
                    })
                    .filter(Objects::nonNull)
                    .collect(Collectors.joining("\n"));
            if (!StringUtils.hasText(context)) return;
            ChatMemory chatMemory = chatMemoryMap.get(memoryId);
            if (chatMemory != null) {
                chatMemory.add(SystemMessage.from("=== 用户画像语义片段（按当前问题召回）===\n" + context));
            }
        } catch (Exception e) {
            log.debug("注入用户画像语义片段失败（忽略）: userId={}, error={}", userId, e.getMessage());
        }
    }

    protected void persistMessages(String conversationId, String userId,
                                   String userText, String assistantText,
                                   List<SearchCommoditiesTool.CommodityDTO> recommended) {
        try {
            AIConversationStorage.MessageRecord userRec = storage.appendMessage(conversationId, userId, "user", userText, null);
            upsertConversationMemory(userRec.messageId(), userRec.conversationId(), "user", userText);

            String recIds = null;
            if (recommended != null && !recommended.isEmpty()) {
                List<String> ids = recommended.stream()
                    .map(SearchCommoditiesTool.CommodityDTO::getCommodityId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
                try {
                    recIds = objectMapper.writeValueAsString(ids);
                } catch (Exception ignored) {}
            }
            AIConversationStorage.MessageRecord assistantRec = storage.appendMessage(conversationId, userId, "assistant", assistantText, recIds);
            upsertConversationMemory(assistantRec.messageId(), assistantRec.conversationId(), "assistant", assistantText);

        } catch (Exception e) {
            log.error("持久化消息失败（用户回复不受影响）: cid={}, error={}", conversationId, e.getMessage(), e);
        }
    }

    private void upsertConversationMemory(String messageId, String conversationId, String role, String content) {
        try {
            String vectorContent = "role=" + role + "\ncontent=" + content;
            milvusVectorService.upsertConversationMessageByText(messageId, conversationId, vectorContent);
        } catch (Exception e) {
            log.debug("写入对话语义记忆失败（不影响主流程）: mid={}, error={}", messageId, e.getMessage());
        }
    }

    private Flux<ServerSentEvent<String>> buildCompleteEvent(String conversationId, String userId,
                                                             String userMessage, StringBuilder fullReply) {
        return Flux.<ServerSentEvent<String>>defer(() -> {
            String reply = fullReply.toString();
            List<SearchCommoditiesTool.CommodityDTO> recommended = searchCommoditiesTool.getRecommendedCommoditiesForResponse();

            if (StringUtils.hasText(conversationId)) {
                persistMessages(conversationId, userId, userMessage, reply, recommended);
                checkAndUpdateProfile(userId, conversationId);
            }

            try {
                Map<String, Object> data = new HashMap<>();
                data.put("reply", reply);
                data.put("conversationId", conversationId);
                data.put("recommendedCommodities", recommended);
                data.put("hasRecommendations", recommended != null && !recommended.isEmpty());

                return Flux.just(ServerSentEvent.<String>builder()
                    .event("complete")
                    .data(objectMapper.writeValueAsString(data))
                    .build());
            } catch (Exception e) {
                log.error("构建 complete 事件失败: {}", e.getMessage(), e);
                return Flux.empty();
            }
        }).subscribeOn(Schedulers.boundedElastic());
    }

    private void checkAndUpdateProfile(String userId, String conversationId) {
        try {
            long msgCount = storage.countMessages(conversationId);
            if (msgCount % PROFILE_UPDATE_INTERVAL == 0) {
                updateUserProfileAsync(userId, conversationId);
            }
        } catch (Exception e) {
            log.debug("检查画像更新条件失败（忽略）: {}", e.getMessage());
        }
    }

    private void summarizeAndCompress(ChatMemory oldMemory, List<ChatMessage> messages, String memoryId) {
        try {
            int keepRecent = MAX_MEMORY_MESSAGES;
            if (messages.size() <= keepRecent) return;

            List<ChatMessage> oldMessages = messages.subList(0, messages.size() - keepRecent);
            StringBuilder sb = new StringBuilder();
            for (ChatMessage msg : oldMessages) {
                String role = msg instanceof UserMessage ? "用户" : "助手";
                String text = msg instanceof UserMessage
                    ? ((UserMessage) msg).singleText()
                    : ((AiMessage) msg).text();
                sb.append("[").append(role).append("]: ").append(text).append("\n");
            }
            String prompt = "请用简洁的语言（不超过200字）总结以下对话的主要内容和用户需求偏好：\n\n"
                + sb + "\n一段话总结：";
            ChatResponse resp = chatLanguageModel.chat(UserMessage.from(prompt));
            String summary = resp.aiMessage().text().trim();

            List<ChatMessage> recentMessages = new ArrayList<>(
                messages.subList(messages.size() - keepRecent, messages.size()));
            ChatMemory newMemory = MessageWindowChatMemory.withMaxMessages(MAX_MEMORY_MESSAGES);
            newMemory.add(SystemMessage.from("【历史摘要】" + summary));
            recentMessages.forEach(newMemory::add);
            chatMemoryMap.put(memoryId, newMemory);

            log.debug("对话摘要完成，压缩了 {} 条消息", oldMessages.size());
        } catch (Exception e) {
            log.warn("生成对话摘要失败（忽略，保持原有记忆）: {}", e.getMessage());
        }
    }

    @Async
    public void updateUserProfileAsync(String userId, String conversationId) {
        try {
            List<AIConversationStorage.MessageRecord> recent = storage.getRecentMessages(conversationId, 30);
            if (recent.isEmpty()) return;

            StringBuilder sb = new StringBuilder();
            for (AIConversationStorage.MessageRecord msg : recent) {
                String role = "user".equals(msg.role()) ? "用户" : "助手";
                sb.append("[").append(role).append("]: ").append(msg.content()).append("\n");
            }
            String prompt = "根据以下用户与购物助手的对话，用一句话（不超过100字）归纳该用户的购物偏好，"
                + "包括商品类型、价格区间、地区等信息：\n\n" + sb + "\n偏好摘要：";

            ChatResponse resp = chatLanguageModel.chat(UserMessage.from(prompt));
            String summary = resp.aiMessage().text().trim();

            storage.saveProfileSummary(userId, summary);

            List<String> chunks = buildProfileChunks(summary, recent);
            int idx = 0;
            for (String chunk : chunks) {
                String vectorId = "up_" + userId + "_" + System.currentTimeMillis() + "_" + idx++;
                milvusVectorService.upsertUserProfileVectorByText(vectorId, userId, chunk);
            }

            log.info("用户画像已更新: userId={}", userId);
        } catch (Exception e) {
            log.warn("更新用户画像失败（异步，不影响主流程）: userId={}, error={}", userId, e.getMessage());
        }
    }

    private List<String> buildProfileChunks(String summary, List<AIConversationStorage.MessageRecord> recentMessages) {
        List<String> chunks = new ArrayList<>();
        if (StringUtils.hasText(summary)) {
            chunks.add("用户长期偏好摘要: " + summary);
        }
        StringBuilder dialog = new StringBuilder();
        int count = 0;
        for (AIConversationStorage.MessageRecord msg : recentMessages) {
            if (count >= 12) break;
            String role = "user".equals(msg.role()) ? "用户" : "助手";
            dialog.append(role).append("：").append(msg.content()).append('\n');
            count++;
        }
        String dialogText = dialog.toString();
        int chunkSize = 220;
        for (int i = 0; i < dialogText.length(); i += chunkSize) {
            int end = Math.min(i + chunkSize, dialogText.length());
            String part = dialogText.substring(i, end).trim();
            if (!part.isEmpty()) {
                chunks.add("近期对话偏好片段: " + part);
            }
        }
        return chunks;
    }

    private String buildBaseSystemPrompt() {
        return "你是一个智能购物助手，帮助用户在南大集市（二手交易平台）上找到合适的商品。\n\n"
            + "你的职责：\n"
            + "1. 理解用户的购物需求\n"
            + "2. 当用户想要查找、购买或了解商品时，使用 searchCommodities 工具搜索商品\n"
            + "2.1 当用户需求描述模糊、同义表达多时，优先使用 searchCommoditiesByVector\n"
            + "2.2 当需要兼顾精确关键词和语义召回时，使用 searchCommoditiesHybrid\n"
            + "3. 基于搜索结果推荐合适的商品\n"
            + "4. 回答关于商品的问题\n"
            + "5. 提供友好的购物建议\n\n"
            + "使用工具的规则：\n"
            + "- 当用户明确表示想要查找、购买、搜索商品时，必须调用 searchCommodities 工具\n"
            + "- 如果用户描述是偏语义、口语化（例如“适合宿舍追剧的轻薄本”），优先用 searchCommoditiesByVector 或 searchCommoditiesHybrid\n"
            + "- 从用户的描述中提取搜索关键词（query 参数）作为主搜索条件\n"
            + "- location 是弱参数：仅当用户明确强调「只要某地」「附近」等时可选传入，用作软偏好即可，不要过度依赖；多数情况不传或传空\n"
            + "- 如果没有明确提到数量，默认返回 20 个结果\n"
            + "- 基于工具返回的搜索结果，为用户提供个性化的推荐和解释\n\n"
            + "结果过滤与推荐（重要）：\n"
            + "- 搜索引擎返回的结果粒度较粗，常会混入仅关键词相关、但不符合用户具体要求的商品。\n"
            + "- 你必须对搜索结果做「过滤」：只推荐真正符合用户描述的商品，不要推荐勉强相关却不符合的。\n"
            + "- 例如：用户要「二合一笔记本」时，只推荐二合一/可翻转/触屏本，不要推荐普通笔记本。\n"
            + "- 在给出文字回复前，必须调用 confirmRecommendedCommodities，传入且仅传入「经过过滤、真正符合用户需求」的 commodityId 列表；没有符合的则传空列表。只有被确认的商品会作为推荐卡片展示给用户，未确认的不会展示。\n\n"
            + "Agent 回溯（仅在「ES 搜索 + LLM 过滤」之后仍不足时）：\n"
            + "- 流程顺序固定：先做 ES 搜索（粗粒度、OR 型）→ 对本次搜索结果做 LLM 过滤并调用 confirmRecommendedCommodities → 若此时过滤后仍没有或不够合适结果，再考虑回溯。\n"
            + "- 你可以在首次检索阶段自主选择 ES、向量或混合检索；但都必须先完成过滤与 confirm，再决定是否回溯。\n"
            + "- 不要在「刚搜完、还没做过滤」时就换词重搜。回溯只发生在「已经过滤并确认过，仍不满意」之后。\n"
            + "- 回溯时：改进搜索关键词（同义词、限定词、更具体说法），再次 searchCommodities，然后对新区结果同样先过滤、再 confirm，直到有合适推荐或工具调用次数用尽（最多 5 次）。\n"
            + "- 只有在多次「搜索 + 过滤」后仍无合适结果时，才回复用户说明并建议调整需求。不要凑数推荐不匹配的商品。\n\n"
            + "注意事项：\n"
            + "- 回答要简洁明了、友好自然\n"
            + "- 基于搜索结果回答，不要编造商品信息\n"
            + "- 如果搜索结果为空，友好地建议用户调整搜索条件\n"
            + "- 如果用户的问题不明确，要友好地询问更多细节\n";
    }

    private ServerSentEvent<String> errorEvent(String message) {
        String safeMsg = message != null ? message.replace("\"", "'") : "未知错误";
        return ServerSentEvent.<String>builder()
            .event("error")
            .data("{\"error\":\"" + safeMsg + "\"}")
            .build();
    }
}
