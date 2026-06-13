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

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * AI Agent 服务：会话与消息权威存 PostgreSQL；用户画像摘要存 Redis；Milvus 作语义索引（轮次向量 + 画像 chunk）。
 *
 * <p>对话流程：
 * <ol>
 *   <li>每次 chat：从关系库恢复 ChatMemory（最近窗口）；向量/Milvus 仅作辅助 system 片段</li>
 *   <li>原生 ChatMemory：窗口制，超过阈值则摘要压缩</li>
 *   <li>Agent 工具（ES/向量/混合检索）</li>
 *   <li>持久化：单条消息入 PostgreSQL；每轮 user+assistant 合并写入 Milvus（失败不影响主流程）</li>
 *   <li>画像异步更新 → Redis + Milvus chunk</li>
 * </ol>
 */
@Slf4j
@Service
public class AIAgentService {

    /** 摘要后保留的最近消息条数（用户/助手消息为主） */
    private static final int MAX_MEMORY_MESSAGES = 15;
    /** ChatMemory 最大条数（缓冲），超过 {@link #SUMMARY_TRIGGER} 则触发摘要 */
    private static final int CHAT_MEMORY_BUFFER = 32;
    private static final int SUMMARY_TRIGGER = 24;
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
    /** 本轮请求的辅助 system 片段（按 memoryId），供 {@code systemMessageProvider} 读取，不写入 ChatMemory */
    private final Map<String, SessionAugmentation> sessionAugmentationByMemoryId = new ConcurrentHashMap<>();
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

        Object[] toolObjects = new Object[] { this.searchCommoditiesTool };

        this.shoppingAssistant = AiServices.builder(ShoppingAssistant.class)
            .chatModel(chatLanguageModel)
            .streamingChatModel(streamingChatLanguageModel)
            .tools(toolObjects)
            .maxSequentialToolsInvocations(MAX_TOOL_ITERATIONS)
            .chatMemoryProvider(chatMemoryId -> {
                String id = chatMemoryId != null ? chatMemoryId.toString() : "default";
                return chatMemoryMap.computeIfAbsent(id,
                    k -> MessageWindowChatMemory.withMaxMessages(CHAT_MEMORY_BUFFER));
            })
            .systemMessageProvider(this::buildSystemPromptForMemoryId)
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
        String memoryId = StringUtils.hasText(conversationId) ? conversationId : userId + "_default";
        try {
            beginCommodityTurn(memoryId);

            initChatMemory(memoryId, conversationId, userId);
            prepareSessionAugmentation(memoryId, userId, conversationId, userMessage);

            ChatMemory chatMemory = chatMemoryMap.get(memoryId);
            if (chatMemory != null && chatMemory.messages().size() >= SUMMARY_TRIGGER) {
                summarizeAndCompress(chatMemory, chatMemory.messages(), memoryId);
            }

            String assistantReply = shoppingAssistant.chat(memoryId, userMessage);
            List<SearchCommoditiesTool.CommodityDTO> recommended = resolveRecommendedCommodities(memoryId);

            if (StringUtils.hasText(conversationId)) {
                persistMessages(conversationId, userId, userMessage, assistantReply, recommended);
                persistConversationWindowState(conversationId);
                checkAndUpdateProfile(userId, conversationId);
            }

            log.info("AI 对话完成: userId={}, cid={}, recommended={}", userId, conversationId,
                recommended != null ? recommended.size() : 0);
            return new ChatResult(assistantReply, recommended);

        } catch (Exception e) {
            log.error("AI 对话失败: userId={}, error={}", userId, e.getMessage(), e);
            return new ChatResult("抱歉，我遇到了一些问题，请稍后再试。", null);
        } finally {
            sessionAugmentationByMemoryId.remove(memoryId);
            endCommodityTurn(memoryId);
        }
    }

    public Flux<ServerSentEvent<String>> chatStream(String userMessage, String userId, String conversationId) {
        if (!StringUtils.hasText(userMessage) || !StringUtils.hasText(userId)) {
            return Flux.just(errorEvent("用户消息或用户ID不能为空"));
        }

        String memoryId = StringUtils.hasText(conversationId) ? conversationId : userId + "_default";
        try {
            initChatMemory(memoryId, conversationId, userId);
            prepareSessionAugmentation(memoryId, userId, conversationId, userMessage);

            ChatMemory chatMemory = chatMemoryMap.get(memoryId);
            if (chatMemory != null && chatMemory.messages().size() >= SUMMARY_TRIGGER) {
                summarizeAndCompress(chatMemory, chatMemory.messages(), memoryId);
            }

            StringBuilder fullReply = new StringBuilder();

            return shoppingAssistant.chatStream(memoryId, userMessage)
                .doOnSubscribe(s -> beginCommodityTurn(memoryId))
                .map(token -> {
                    fullReply.append(token);
                    return ServerSentEvent.<String>builder()
                        .event("token")
                        .data(token)
                        .build();
                })
                .concatWith(buildCompleteEvent(memoryId, conversationId, userId, userMessage, fullReply))
                .doFinally(sig -> {
                    sessionAugmentationByMemoryId.remove(memoryId);
                    endCommodityTurn(memoryId);
                })
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

    /**
     * 从关系库恢复最近窗口到 ChatMemory；无 conversationId 时仅用进程内空窗口。
     * 语义召回与画像见 {@link #prepareSessionAugmentation}。
     */
    private void initChatMemory(String memoryId, String conversationId, String userId) {
        if (!StringUtils.hasText(conversationId)) {
            chatMemoryMap.put(memoryId, MessageWindowChatMemory.withMaxMessages(CHAT_MEMORY_BUFFER));
            return;
        }
        ChatMemory restored = buildChatMemoryFromRelationalDb(conversationId, userId);
        chatMemoryMap.put(memoryId, restored);
    }

    /**
     * 按会话快照（memory_summary + window_message_count）从 PostgreSQL 还原窗口；
     * 无快照或 window=0 时退回「最近 {@link #CHAT_MEMORY_BUFFER} 条」。
     */
    private ChatMemory buildChatMemoryFromRelationalDb(String conversationId, String userId) {
        Optional<AIConversationStorage.ConversationMemorySnapshot> snapOpt =
                storage.getConversationMemorySnapshot(conversationId);
        int tailUserAi = snapOpt.map(AIConversationStorage.ConversationMemorySnapshot::windowMessageCount).orElse(0);
        String summaryBody = snapOpt.map(AIConversationStorage.ConversationMemorySnapshot::memorySummary)
                .filter(StringUtils::hasText)
                .map(String::trim)
                .orElse(null);

        int loadLimit = tailUserAi > 0 ? Math.min(tailUserAi, CHAT_MEMORY_BUFFER) : CHAT_MEMORY_BUFFER;
        List<AIConversationStorage.MessageRecord> msgs = storage.getMessages(conversationId, userId, loadLimit);

        ChatMemory mem = MessageWindowChatMemory.withMaxMessages(CHAT_MEMORY_BUFFER);
        if (StringUtils.hasText(summaryBody)) {
            mem.add(SystemMessage.from("【历史摘要】" + summaryBody));
        }
        for (AIConversationStorage.MessageRecord r : msgs) {
            if (r == null || !StringUtils.hasText(r.role())) {
                continue;
            }
            String text = r.content() != null ? r.content() : "";
            if ("user".equalsIgnoreCase(r.role())) {
                mem.add(UserMessage.from(text));
            } else if ("assistant".equalsIgnoreCase(r.role())) {
                mem.add(AiMessage.from(text));
            }
        }
        log.debug("已从关系库恢复 ChatMemory: cid={}, tailUserAi={}, summary={}, loadedRows={}",
                conversationId, tailUserAi, summaryBody != null, msgs.size());
        return mem;
    }

    /**
     * 将当前 ChatMemory 中的【历史摘要】与窗口内 user/assistant 条数写回会话行，供下次恢复与归纳阈值对齐。
     */
    private void persistConversationWindowState(String conversationId) {
        if (!StringUtils.hasText(conversationId)) {
            return;
        }
        try {
            ChatMemory cm = chatMemoryMap.get(conversationId);
            if (cm == null) {
                return;
            }
            String summaryBody = null;
            int userAiCount = 0;
            for (ChatMessage m : cm.messages()) {
                if (m instanceof SystemMessage sm) {
                    String t = sm.text();
                    if (t != null && t.startsWith("【历史摘要】")) {
                        summaryBody = t.substring("【历史摘要】".length()).trim();
                    }
                } else if (m instanceof UserMessage || m instanceof AiMessage) {
                    userAiCount++;
                }
            }
            storage.updateConversationMemorySnapshot(conversationId, summaryBody, userAiCount);
        } catch (Exception e) {
            log.debug("持久化会话窗口快照失败（忽略）: cid={}, error={}", conversationId, e.getMessage());
        }
    }

    /**
     * 将 Redis 画像摘要、Milvus 对话/用户画像语义召回写入 {@link #sessionAugmentationByMemoryId}，
     * 由 {@link #buildSystemPromptForMemoryId} 拼入 system，不写入 ChatMemory。
     */
    private void prepareSessionAugmentation(String memoryId, String userId, String conversationId, String userMessage) {
        sessionAugmentationByMemoryId.remove(memoryId);
        try {
            String redisProfile = storage.getProfileSummary(userId)
                .map(AIConversationStorage.ProfileSummary::profileSummary)
                .filter(StringUtils::hasText)
                .orElse(null);

            String convRecall = buildConversationSemanticRecall(conversationId, userMessage);
            String profileSemantic = buildUserProfileSemanticRecall(userId, userMessage);

            SessionAugmentation aug = new SessionAugmentation(redisProfile, convRecall, profileSemantic);
            if (!aug.isEmpty()) {
                sessionAugmentationByMemoryId.put(memoryId, aug);
            }
        } catch (Exception e) {
            log.debug("准备会话辅助上下文失败（忽略）: {}", e.getMessage());
        }
    }

    private String buildConversationSemanticRecall(String conversationId, String userMessage) {
        if (!StringUtils.hasText(conversationId) || !StringUtils.hasText(userMessage)) {
            return null;
        }
        try {
            int topK = Math.max(milvusProperties.getMemoryTopK(), MEMORY_RECALL_LIMIT);
            List<Map<String, Object>> hits = milvusVectorService.searchConversationByText(userMessage, conversationId, topK);
            StringBuilder sb = new StringBuilder();
            int added = 0;
            for (Map<String, Object> hit : hits) {
                Object entityRaw = hit.get("entity");
                if (!(entityRaw instanceof Map<?, ?> entity)) continue;
                Object contentObj = entity.get("content");
                if (contentObj == null) continue;
                String content = String.valueOf(contentObj);
                if (content.contains("\nrole=assistant\ncontent=") && content.startsWith("role=user\ncontent=")) {
                    String u = content.substring("role=user\ncontent=".length());
                    int split = u.indexOf("\nrole=assistant\ncontent=");
                    if (split >= 0) {
                        String userPart = u.substring(0, split);
                        String asstPart = u.substring(split + "\nrole=assistant\ncontent=".length());
                        sb.append("[轮次] 用户: ").append(userPart).append(" | 助手: ").append(asstPart).append('\n');
                        added++;
                    }
                } else if (content.startsWith("role=user\ncontent=")) {
                    sb.append("[用户片段] ").append(content.substring("role=user\ncontent=".length())).append('\n');
                    added++;
                } else if (content.startsWith("role=assistant\ncontent=")) {
                    sb.append("[助手片段] ").append(content.substring("role=assistant\ncontent=".length())).append('\n');
                    added++;
                }
                if (added >= MEMORY_RECALL_LIMIT) {
                    break;
                }
            }
            if (sb.length() == 0) {
                return null;
            }
            log.debug("Milvus 对话语义召回（辅助）: cid={}, lines={}", conversationId, added);
            return sb.toString().trim();
        } catch (Exception e) {
            log.warn("对话语义召回失败: {}", e.getMessage());
            return null;
        }
    }

    private String buildUserProfileSemanticRecall(String userId, String userMessage) {
        try {
            List<Map<String, Object>> hits = milvusVectorService.searchUserProfileByText(userMessage, 3);
            if (hits == null || hits.isEmpty()) {
                return null;
            }
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
            return StringUtils.hasText(context) ? context : null;
        } catch (Exception e) {
            log.debug("用户画像语义召回失败（忽略）: userId={}, error={}", userId, e.getMessage());
            return null;
        }
    }

    private String buildSystemPromptForMemoryId(Object memoryIdObj) {
        String mid = memoryIdObj != null ? memoryIdObj.toString() : "default";
        String base = buildBaseSystemPrompt();
        SessionAugmentation aug = sessionAugmentationByMemoryId.get(mid);
        if (aug == null || aug.isEmpty()) {
            return base;
        }
        return base + "\n\n" + aug.toAppendix();
    }

    protected void persistMessages(String conversationId, String userId,
                                   String userText, String assistantText,
                                   List<SearchCommoditiesTool.CommodityDTO> recommended) {
        try {
            AIConversationStorage.MessageRecord userRec = storage.appendMessage(conversationId, userId, "user", userText, null);

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
            upsertConversationTurnVectorSafe(conversationId, userRec, assistantRec, userText, assistantText);

        } catch (Exception e) {
            log.error("持久化消息失败（用户回复不受影响）: cid={}, error={}", conversationId, e.getMessage(), e);
        }
    }

    /**
     * 将「一轮 user+assistant」作为单条向量写入 Milvus（与语义召回格式一致）；失败不影响主流程。
     */
    private void upsertConversationTurnVectorSafe(String conversationId,
                                                  AIConversationStorage.MessageRecord userRec,
                                                  AIConversationStorage.MessageRecord assistantRec,
                                                  String userText,
                                                  String assistantText) {
        if (!milvusProperties.isEnabled()) {
            return;
        }
        try {
            String pairId = "pair_" + userRec.messageId() + "_" + assistantRec.messageId();
            String ut = userText != null ? userText : "";
            String at = assistantText != null ? assistantText : "";
            String vectorContent = "role=user\ncontent=" + ut + "\nrole=assistant\ncontent=" + at;
            milvusVectorService.upsertConversationMessageByText(pairId, conversationId, vectorContent);
        } catch (Exception e) {
            log.warn("Milvus 轮次向量写入失败（不影响主流程）: cid={}, error={}", conversationId, e.getMessage());
        }
    }

    /**
     * 先发出 {@code complete} SSE，再异步持久化。
     * <p>若在持久化完成前才发 complete，最后一个 token 与 complete 之间会长时间无字节写入，
     * 网关/代理/Tomcat 可能按读空闲掐断连接，表现为前端流中断但库中已有回复。</p>
     */
    private Flux<ServerSentEvent<String>> buildCompleteEvent(String memoryId, String conversationId, String userId,
                                                             String userMessage, StringBuilder fullReply) {
        return Flux.defer(() -> {
            String reply = fullReply.toString();
            List<SearchCommoditiesTool.CommodityDTO> recommended = resolveRecommendedCommodities(memoryId);

            try {
                Map<String, Object> data = new HashMap<>();
                data.put("reply", reply);
                data.put("conversationId", conversationId);
                data.put("recommendedCommodities", recommended);
                data.put("hasRecommendations", recommended != null && !recommended.isEmpty());

                String json = objectMapper.writeValueAsString(data);
                ServerSentEvent<String> complete = ServerSentEvent.<String>builder()
                    .event("complete")
                    .data(json)
                    .build();

                return Flux.just(complete).doOnComplete(() -> {
                    if (!StringUtils.hasText(conversationId)) {
                        return;
                    }
                    final String cid = conversationId;
                    final String uid = userId;
                    final String umsg = userMessage;
                    final String r = reply;
                    final List<SearchCommoditiesTool.CommodityDTO> rec = recommended;
                    Schedulers.boundedElastic().schedule(() -> {
                        try {
                            persistMessages(cid, uid, umsg, r, rec);
                            persistConversationWindowState(cid);
                            checkAndUpdateProfile(uid, cid);
                        } catch (Exception e) {
                            log.error("持久化失败（客户端已收到 complete）: cid={}, error={}", cid, e.getMessage(), e);
                        }
                    });
                });
            } catch (Exception e) {
                log.error("构建 complete 事件失败: {}", e.getMessage(), e);
                return Flux.empty();
            }
        });
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
            if (messages.size() < SUMMARY_TRIGGER) {
                return;
            }
            int keepRecent = MAX_MEMORY_MESSAGES;

            List<ChatMessage> oldMessages = messages.subList(0, messages.size() - keepRecent);
            StringBuilder sb = new StringBuilder();
            for (ChatMessage msg : oldMessages) {
                if (msg instanceof UserMessage um) {
                    sb.append("[用户]: ").append(um.singleText()).append("\n");
                } else if (msg instanceof AiMessage am) {
                    sb.append("[助手]: ").append(am.text()).append("\n");
                } else if (msg instanceof SystemMessage sm) {
                    sb.append("[历史摘要]: ").append(sm.text()).append("\n");
                }
            }
            String prompt = "请用简洁的语言（不超过200字）总结以下对话的主要内容和用户需求偏好：\n\n"
                + sb + "\n一段话总结：";
            ChatResponse resp = chatLanguageModel.chat(UserMessage.from(prompt));
            String summary = resp.aiMessage().text().trim();

            List<ChatMessage> recentMessages = new ArrayList<>(
                messages.subList(messages.size() - keepRecent, messages.size()));
            ChatMemory newMemory = MessageWindowChatMemory.withMaxMessages(CHAT_MEMORY_BUFFER);
            newMemory.add(SystemMessage.from("【历史摘要】" + summary));
            recentMessages.forEach(newMemory::add);
            chatMemoryMap.put(memoryId, newMemory);

            log.debug("对话摘要完成，压缩了 {} 条消息", oldMessages.size());
            persistConversationWindowState(memoryId);
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

    private void beginCommodityTurn(String memoryId) {
        searchCommoditiesTool.beginConversationTurn(memoryId);
    }

    private void endCommodityTurn(String memoryId) {
        searchCommoditiesTool.endConversationTurn(memoryId);
    }

    private List<SearchCommoditiesTool.CommodityDTO> resolveRecommendedCommodities(String memoryId) {
        List<SearchCommoditiesTool.CommodityDTO> list =
            searchCommoditiesTool.getRecommendedCommoditiesForResponse(memoryId);
        return list != null ? list : Collections.emptyList();
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
