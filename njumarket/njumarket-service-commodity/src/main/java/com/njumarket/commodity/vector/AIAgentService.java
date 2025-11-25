package com.njumarket.commodity.vector;

import com.njumarket.commodity.entity.Commodity;
import com.njumarket.commodity.vector.function.SearchCommoditiesTool;
import com.njumarket.commodity.client.AuthClient;
import com.njumarket.njumarket.dto.Result;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.MemoryId;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * AI Agent 服务
 * 使用 LangChain4j 实现智能对话和搜索
 */
@Slf4j
@Service
public class AIAgentService {
    
    private final ChatLanguageModel chatLanguageModel;
    // 注意：embeddingModel 和 aiSearchService 已不再直接使用
    // 搜索功能现在完全通过 SearchCommoditiesTool 处理，它内部使用 AISearchService
    @SuppressWarnings("unused")
    private final EmbeddingModel embeddingModel;
    @SuppressWarnings("unused")
    private final AISearchService aiSearchService;
    private final ConversationVectorService conversationVectorService;
    private final AuthClient authClient;
    private final SearchCommoditiesTool searchCommoditiesTool;
    
    // LangChain4j AI Service（支持 Function Calling）
    private final ShoppingAssistant shoppingAssistant;
    
    // 内存 ChatMemory 存储（用于管理对话上下文）
    private final Map<String, ChatMemory> chatMemoryMap = new HashMap<>();
    
    private static final int MAX_MEMORY_MESSAGES = 15; // 内存中保留的最近消息数
    private static final int MAX_SUMMARY_TRIGGER = 20; // 超过此数量时触发摘要
    private static final int MAX_CONVERSATION_HISTORY = 5; // 从向量存储中检索的相关历史数量
    
    /**
     * 购物助手接口
     * LangChain4j 使用接口定义 AI Service，自动处理 Function Calling
     * 使用 @MemoryId 注解指定 memoryId，实现多轮对话记忆
     * 使用 @UserMessage 注解指定用户消息参数
     */
    public interface ShoppingAssistant {
        String chat(@MemoryId String memoryId, @dev.langchain4j.service.UserMessage String userMessage);
    }
    
    // 存储每个 conversationId 的上下文信息（用于构建增强的系统提示词）
    private final Map<String, ConversationContext> contextMap = new HashMap<>();
    
    /**
     * 对话上下文信息
     */
    private static class ConversationContext {
        String userId;
        String userProfileContext;
        String conversationSummary;
        List<ConversationVectorService.ConversationMessage> relevantHistory;
        @SuppressWarnings("unused")
        long lastUpdateTime; // 保留用于将来可能的用途（如缓存过期判断）
    }
    
    /**
     * 构造函数
     * 使用 LangChain4j 的 AiServices 自动创建代理，支持 Function Calling
     */
    public AIAgentService(ChatLanguageModel chatLanguageModel,
                         EmbeddingModel embeddingModel,
                         AISearchService aiSearchService,
                         ConversationVectorService conversationVectorService,
                         com.njumarket.commodity.client.AuthClient authClient,
                         SearchCommoditiesTool searchCommoditiesTool) {
        this.chatLanguageModel = chatLanguageModel;
        this.embeddingModel = embeddingModel;
        this.aiSearchService = aiSearchService;
        this.conversationVectorService = conversationVectorService;
        this.authClient = authClient;
        this.searchCommoditiesTool = searchCommoditiesTool;
        
        // 创建 AI Service，自动注册工具
        // 使用 conversationId 作为 memoryId，实现多轮对话记忆
        this.shoppingAssistant = AiServices.builder(ShoppingAssistant.class)
            .chatLanguageModel(chatLanguageModel)
            .tools(searchCommoditiesTool)
            .chatMemoryProvider(chatMemoryId -> {
                // 使用 MessageWindowChatMemory 管理对话历史
                // 每个 conversationId 对应一个独立的 ChatMemory
                // 保留最近 MAX_MEMORY_MESSAGES 条消息作为上下文
                String memoryId = chatMemoryId != null ? chatMemoryId.toString() : "default";
                return chatMemoryMap.computeIfAbsent(memoryId, 
                    id -> MessageWindowChatMemory.withMaxMessages(MAX_MEMORY_MESSAGES));
            })
            .systemMessageProvider(chatMemoryId -> {
                // 从上下文映射中获取增强的系统提示词
                String memoryId = chatMemoryId != null ? chatMemoryId.toString() : "default";
                ConversationContext context = contextMap.get(memoryId);
                if (context != null) {
                    return buildEnhancedSystemPrompt(context.userId, context.userProfileContext,
                        context.conversationSummary, context.relevantHistory);
                }
                return buildSystemPrompt(null);
            })
            .build();
    }
    
    /**
     * AI Agent 对话结果
     */
    public static class ChatResult {
        private String reply;
        private List<Commodity> recommendedCommodities;
        
        public ChatResult(String reply, List<Commodity> recommendedCommodities) {
            this.reply = reply;
            this.recommendedCommodities = recommendedCommodities != null ? recommendedCommodities : new ArrayList<>();
        }
        
        public String getReply() { return reply; }
        public List<Commodity> getRecommendedCommodities() { return recommendedCommodities; }
    }
    
    /**
     * 流式聊天回调接口
     */
    public interface StreamChatCallback {
        /**
         * 接收到新的 token
         * @param token 新的 token
         */
        void onToken(String token);
        
        /**
         * 流式输出完成
         * @param fullReply 完整的回复
         * @param recommendedCommodities 推荐的商品列表
         */
        void onComplete(String fullReply, List<Commodity> recommendedCommodities);
        
        /**
         * 发生错误
         * @param error 错误信息
         */
        void onError(String error);
    }
    
    /**
     * AI Agent 对话（使用 LangChain4j Function Calling）
     * @param userMessage 用户消息
     * @param userId 用户ID
     * @param conversationId 对话ID（可选，用于多轮对话）
     * @return Agent 回复和推荐商品
     */
    public ChatResult chat(String userMessage, String userId, String conversationId) {
        try {
            if (!StringUtils.hasText(userMessage) || !StringUtils.hasText(userId)) {
                return new ChatResult("抱歉，我无法理解您的问题。", new ArrayList<>());
            }
            
            // 清空上次搜索结果
            searchCommoditiesTool.setLastSearchResults(new ArrayList<>());
            
            // 确定 memoryId
            String memoryId = StringUtils.hasText(conversationId) ? conversationId : userId + "_default";
            
            // 1. 获取用户画像信息（用于个性化）
            String userProfileContext = getUserProfileContext(userId);
            
            // 2. 搜索相关对话历史（从向量存储中）
            List<ConversationVectorService.ConversationMessage> relevantHistory = 
                conversationVectorService.searchRelevantConversations(userMessage, userId, MAX_CONVERSATION_HISTORY);
            
            // 3. 获取内存中的 ChatMemory，如果不存在则创建并加载历史消息
            ChatMemory chatMemory = chatMemoryMap.computeIfAbsent(memoryId, 
                id -> {
                    // 创建新的 ChatMemory
                    ChatMemory newMemory = MessageWindowChatMemory.withMaxMessages(MAX_MEMORY_MESSAGES);
                    // 如果是旧的 conversationId，从数据库加载历史消息
                    if (StringUtils.hasText(conversationId)) {
                        loadHistoryToChatMemory(newMemory, conversationId, userId);
                    }
                    return newMemory;
                });
            
            // 4. 检查是否需要生成摘要（类似 Cursor 的 summarized text）
            String conversationSummary = null;
            List<ChatMessage> memoryMessages = chatMemory.messages();
            if (memoryMessages.size() >= MAX_SUMMARY_TRIGGER) {
                // 生成摘要：将旧消息总结成摘要
                conversationSummary = summarizeOldMessages(memoryMessages, MAX_MEMORY_MESSAGES);
                log.debug("生成对话摘要: memoryId={}, summaryLength={}", memoryId, 
                    conversationSummary != null ? conversationSummary.length() : 0);
            }
            
            // 5. 保存上下文信息（用于系统提示词提供者）
            ConversationContext context = new ConversationContext();
            context.userId = userId;
            context.userProfileContext = userProfileContext;
            context.conversationSummary = conversationSummary;
            context.relevantHistory = relevantHistory;
            context.lastUpdateTime = System.currentTimeMillis();
            contextMap.put(memoryId, context);
            
            // 6. 如果有相关历史且不在内存中，注入到上下文
            injectRelevantHistory(chatMemory, relevantHistory, memoryMessages);
            
            // 7. 调用 AI Service（自动处理 Function Calling）
            // 系统提示词会通过 systemMessageProvider 自动注入
            String assistantReply = shoppingAssistant.chat(memoryId, userMessage);
            
            // 9. 获取工具调用返回的商品列表
            List<Commodity> recommendedCommodities = searchCommoditiesTool.getLastSearchResults();
            
            // 10. 存储对话历史到向量数据库（异步）
            if (StringUtils.hasText(conversationId)) {
                String userMessageId = UUID.randomUUID().toString();
                String assistantMessageId = UUID.randomUUID().toString();
                
                // 存储用户消息（无商品ID）
                conversationVectorService.storeConversationVector(
                    conversationId, userMessageId, userId, userMessage, "user");
                
                // 存储助手回复，包含推荐的商品ID列表
                List<String> commodityIds = recommendedCommodities != null && !recommendedCommodities.isEmpty()
                    ? recommendedCommodities.stream()
                        .map(Commodity::getCommodityId)
                        .filter(Objects::nonNull)
                        .collect(java.util.stream.Collectors.toList())
                    : null;
                conversationVectorService.storeConversationVector(
                    conversationId, assistantMessageId, userId, assistantReply, "assistant", commodityIds);
            }
            
            log.info("AI Agent 对话完成: userId={}, conversationId={}, memorySize={}, relevantHistory={}, recommendedCommodities={}", 
                userId, conversationId, memoryMessages.size(), relevantHistory.size(), 
                recommendedCommodities != null ? recommendedCommodities.size() : 0);
            
            return new ChatResult(assistantReply, recommendedCommodities);
            
        } catch (Exception e) {
            log.error("AI Agent 对话失败: userId={}, error={}", userId, e.getMessage(), e);
            return new ChatResult("抱歉，我遇到了一些问题，请稍后再试。", new ArrayList<>());
        }
    }
    
    /**
     * AI Agent 流式对话（使用 LangChain4j 流式 API）
     * @param userMessage 用户消息
     * @param userId 用户ID
     * @param conversationId 对话ID（可选，用于多轮对话）
     * @param callback 流式输出回调
     */
    public void chatStream(String userMessage, String userId, String conversationId, StreamChatCallback callback) {
        try {
            if (!StringUtils.hasText(userMessage) || !StringUtils.hasText(userId)) {
                callback.onError("用户消息或用户ID不能为空");
                return;
            }
            
            // 清空上次搜索结果
            searchCommoditiesTool.setLastSearchResults(new ArrayList<>());
            
            // 确定 memoryId
            String memoryId = StringUtils.hasText(conversationId) ? conversationId : userId + "_default";
            
            // 1. 获取用户画像信息（用于个性化）
            String userProfileContext = getUserProfileContext(userId);
            
            // 2. 搜索相关对话历史（从向量存储中）
            List<ConversationVectorService.ConversationMessage> relevantHistory = 
                conversationVectorService.searchRelevantConversations(userMessage, userId, MAX_CONVERSATION_HISTORY);
            
            // 3. 获取内存中的 ChatMemory，如果不存在则创建并加载历史消息
            ChatMemory chatMemory = chatMemoryMap.computeIfAbsent(memoryId, 
                id -> {
                    // 创建新的 ChatMemory
                    ChatMemory newMemory = MessageWindowChatMemory.withMaxMessages(MAX_MEMORY_MESSAGES);
                    // 如果是旧的 conversationId，从数据库加载历史消息
                    if (StringUtils.hasText(conversationId)) {
                        loadHistoryToChatMemory(newMemory, conversationId, userId);
                    }
                    return newMemory;
                });
            
            // 4. 检查是否需要生成摘要
            String conversationSummary = null;
            List<ChatMessage> memoryMessages = chatMemory.messages();
            if (memoryMessages.size() >= MAX_SUMMARY_TRIGGER) {
                conversationSummary = summarizeOldMessages(memoryMessages, MAX_MEMORY_MESSAGES);
                log.debug("生成对话摘要: memoryId={}, summaryLength={}", memoryId, 
                    conversationSummary != null ? conversationSummary.length() : 0);
            }
            
            // 5. 构建增强的系统提示词
            String systemPrompt = buildEnhancedSystemPrompt(userId, userProfileContext, conversationSummary, relevantHistory);
            
            // 6. 构建完整的消息列表（包括系统提示词、历史消息和当前用户消息）
            List<ChatMessage> allMessages = new ArrayList<>();
            allMessages.add(SystemMessage.from(systemPrompt));
            
            // 添加历史消息（从 ChatMemory 中获取）
            allMessages.addAll(chatMemory.messages());
            
            // 添加当前用户消息
            allMessages.add(UserMessage.from(userMessage));
            
            // 7. 使用流式 API 生成回复
            // 注意：LangChain4j 0.35.0 可能不支持流式输出，这里使用异步分块返回的方式模拟流式输出
            StringBuilder fullReply = new StringBuilder();
            
            // 保存上下文信息（用于系统提示词提供者）
            ConversationContext context = new ConversationContext();
            context.userId = userId;
            context.userProfileContext = getUserProfileContext(userId);
            context.conversationSummary = conversationSummary;
            context.relevantHistory = relevantHistory;
            context.lastUpdateTime = System.currentTimeMillis();
            contextMap.put(memoryId, context);
            
            // 如果有相关历史且不在内存中，注入到上下文
            injectRelevantHistory(chatMemory, relevantHistory, memoryMessages);
            
            // 异步生成回复并分块发送
            CompletableFuture.runAsync(() -> {
                try {
                    // 使用 shoppingAssistant 来触发 Function Calling（与非流式方法保持一致）
                    // 注意：shoppingAssistant 会自动处理工具调用，并返回最终回复
                    String completeReply = shoppingAssistant.chat(memoryId, userMessage);
                    
                    // 清理工具调用标记（如果存在）
                    if (completeReply != null) {
                        // 移除 Function Calling 标记
                        completeReply = completeReply
                            .replaceAll("<\\|FunctionCallBegin\\|>.*?<\\|FunctionCallEnd\\|>", "")
                            .replaceAll("<\\|FunctionCallBegin\\|>", "")
                            .replaceAll("<\\|FunctionCallEnd\\|>", "")
                            .trim();
                    }
                    
                    // 如果回复为空，使用默认回复
                    if (completeReply == null || completeReply.trim().isEmpty()) {
                        completeReply = "我已经为您搜索了相关商品，请查看推荐列表。";
                    }
                    
                    // 模拟流式输出：将回复分成小块逐步发送
                    int chunkSize = 3; // 每次发送 3 个字符
                    for (int i = 0; i < completeReply.length(); i += chunkSize) {
                        int end = Math.min(i + chunkSize, completeReply.length());
                        String chunk = completeReply.substring(i, end);
                        fullReply.append(chunk);
                        callback.onToken(chunk);
                        
                        // 添加小延迟以模拟真实的流式输出
                        try {
                            Thread.sleep(10); // 10ms 延迟
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            break;
                        }
                    }
                    
                    // 流式输出完成
                    String finalReply = fullReply.toString();
                    
                    // 获取工具调用返回的商品列表
                    List<Commodity> recommendedCommodities = searchCommoditiesTool.getLastSearchResults();
                    
                    // 存储对话历史到向量数据库（异步）
                    if (StringUtils.hasText(conversationId)) {
                        String userMessageId = UUID.randomUUID().toString();
                        String assistantMessageId = UUID.randomUUID().toString();
                        
                        // 存储用户消息（无商品ID）
                        conversationVectorService.storeConversationVector(
                            conversationId, userMessageId, userId, userMessage, "user");
                        
                        // 存储助手回复，包含推荐的商品ID列表
                        List<String> commodityIds = recommendedCommodities != null && !recommendedCommodities.isEmpty()
                            ? recommendedCommodities.stream()
                                .map(Commodity::getCommodityId)
                                .filter(Objects::nonNull)
                                .collect(java.util.stream.Collectors.toList())
                            : null;
                        conversationVectorService.storeConversationVector(
                            conversationId, assistantMessageId, userId, finalReply, "assistant", commodityIds);
                    }
                    
                    log.info("AI Agent 流式对话完成: userId={}, conversationId={}, memorySize={}, relevantHistory={}, recommendedCommodities={}", 
                        userId, conversationId, memoryMessages.size(), relevantHistory.size(), 
                        recommendedCommodities != null ? recommendedCommodities.size() : 0);
                    
                    callback.onComplete(finalReply, recommendedCommodities);
                    
                } catch (Exception e) {
                    log.error("AI Agent 流式对话失败: userId={}, error={}", userId, e.getMessage(), e);
                    callback.onError("AI 对话失败: " + e.getMessage());
                }
            });
            
        } catch (Exception e) {
            log.error("AI Agent 流式对话失败: userId={}, error={}", userId, e.getMessage(), e);
            callback.onError("AI 对话失败: " + e.getMessage());
        }
    }
    
    /**
     * 智能搜索（带 Agent 理解）
     * 完全使用 Agent 和 Function Calling 机制，统一处理搜索和推荐
     * @param query 用户查询
     * @param userId 用户ID
     * @param conversationId 对话ID（可选）
     * @return 搜索结果和 Agent 解释
     */
    public AgentSearchResult intelligentSearch(String query, String userId, String conversationId) {
        try {
            // 直接使用 chat 方法，Agent 会自动调用 searchCommoditiesTool 进行搜索
            // 这样可以利用 Agent 的上下文理解、用户画像、对话历史等功能
            ChatResult chatResult = chat(query, userId, conversationId);
            
            AgentSearchResult result = new AgentSearchResult();
            result.setCommodities(chatResult.getRecommendedCommodities() != null ? 
                chatResult.getRecommendedCommodities() : new ArrayList<>());
            result.setExplanation(chatResult.getReply());
            result.setOriginalQuery(query);
            result.setEnhancedQuery(query); // Agent 已经在内部处理了查询增强
            
            log.info("智能搜索完成（通过Agent）: userId={}, query={}, commodities={}", 
                userId, query, result.getCommodities().size());
            
            return result;
            
        } catch (Exception e) {
            log.error("智能搜索失败: userId={}, error={}", userId, e.getMessage(), e);
            // 降级处理：返回空结果和错误提示
            AgentSearchResult result = new AgentSearchResult();
            result.setCommodities(new ArrayList<>());
            result.setExplanation("抱歉，搜索时遇到了问题，请稍后重试。");
            result.setOriginalQuery(query);
            result.setEnhancedQuery(query);
            return result;
        }
    }
    
    /**
     * 构建系统提示词（支持 Function Calling）
     */
    private String buildSystemPrompt(String userId) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("你是一个智能购物助手，帮助用户在南大集市（二手交易平台）上找到合适的商品。\n\n");
        prompt.append("你的职责：\n");
        prompt.append("1. 理解用户的购物需求\n");
        prompt.append("2. 当用户想要查找、购买或了解商品时，使用 searchCommodities 工具搜索商品\n");
        prompt.append("3. 基于搜索结果推荐合适的商品\n");
        prompt.append("4. 回答关于商品的问题\n");
        prompt.append("5. 提供友好的购物建议\n\n");
        prompt.append("使用工具的规则：\n");
        prompt.append("- 当用户明确表示想要查找、购买、搜索商品时，必须调用 searchCommodities 工具\n");
        prompt.append("- 从用户的描述中提取搜索关键词（query参数）\n");
        prompt.append("- 如果用户提到位置，使用 location 参数\n");
        prompt.append("- 如果没有明确提到数量，默认返回 20 个结果\n");
        prompt.append("- 基于工具返回的搜索结果，为用户提供个性化的推荐和解释\n\n");
        prompt.append("注意事项：\n");
        prompt.append("- 回答要简洁明了、友好自然\n");
        prompt.append("- 基于搜索结果回答，不要编造商品信息\n");
        prompt.append("- 如果搜索结果为空，友好地建议用户调整搜索条件\n");
        prompt.append("- 如果用户的问题不明确，要友好地询问更多细节\n");
        return prompt.toString();
    }
    
    /**
     * 构建增强的系统提示词（包含用户画像、摘要和相关历史）
     */
    private String buildEnhancedSystemPrompt(String userId, String userProfileContext, 
                                            String conversationSummary, 
                                            List<ConversationVectorService.ConversationMessage> relevantHistory) {
        StringBuilder prompt = new StringBuilder(buildSystemPrompt(userId));
        
        // 添加用户画像上下文
        if (StringUtils.hasText(userProfileContext)) {
            prompt.append("\n\n=== 用户画像信息 ===\n");
            prompt.append(userProfileContext);
            prompt.append("\n请根据用户的偏好和需求提供个性化推荐。\n");
        }
        
        // 添加对话摘要（类似 Cursor 的 summarized text）
        if (StringUtils.hasText(conversationSummary)) {
            prompt.append("\n\n=== 之前的对话摘要 ===\n");
            prompt.append(conversationSummary);
            prompt.append("\n这是之前对话的摘要，请参考这些信息理解上下文。\n");
        }
        
        // 添加相关历史对话
        if (relevantHistory != null && !relevantHistory.isEmpty()) {
            prompt.append("\n\n=== 相关的历史对话 ===\n");
            for (int i = 0; i < relevantHistory.size() && i < MAX_CONVERSATION_HISTORY; i++) {
                ConversationVectorService.ConversationMessage msg = relevantHistory.get(i);
                prompt.append(String.format("[%s]: %s\n", 
                    "user".equals(msg.getRole()) ? "用户" : "助手", 
                    msg.getContent()));
            }
            prompt.append("\n这些是相关的历史对话，请参考以更好地理解用户的需求。\n");
        }
        
        return prompt.toString();
    }
    
    
    /**
     * 获取用户画像上下文
     */
    private String getUserProfileContext(String userId) {
        try {
            Result result = authClient.getUserProfileVector(userId);
            if (result != null && result.getSuccess() != null && result.getSuccess() && result.getData() != null) {
                // 用户画像向量已存在，可以用于个性化
                // 这里简化实现，只返回提示信息
                return "用户已建立购物偏好画像，请根据历史行为提供个性化推荐。";
            }
        } catch (Exception e) {
            log.debug("获取用户画像失败（可选）: userId={}, error={}", userId, e.getMessage());
        }
        return null;
    }
    
    /**
     * 总结旧消息（类似 Cursor 的 summarized text 机制）
     */
    private String summarizeOldMessages(List<ChatMessage> messages, int keepRecent) {
        if (messages == null || messages.size() <= keepRecent) {
            return null;
        }
        
        try {
            // 需要总结的消息数量
            int toSummarize = messages.size() - keepRecent;
            List<ChatMessage> oldMessages = messages.subList(0, toSummarize);
            
            // 构建摘要提示词
            StringBuilder conversationText = new StringBuilder();
            for (ChatMessage msg : oldMessages) {
                String role = msg instanceof UserMessage ? "用户" : "助手";
                String content = msg instanceof UserMessage ? 
                    ((UserMessage) msg).singleText() : 
                    ((AiMessage) msg).text();
                conversationText.append(String.format("[%s]: %s\n", role, content));
            }
            
            String summaryPrompt = String.format(
                "以下是用户与购物助手的早期对话记录，请用简洁的语言总结主要内容和用户的需求偏好：\n\n%s\n\n" +
                "请用一段话（不超过200字）总结这段对话的主要内容、用户的需求和偏好。",
                conversationText.toString()
            );
            
            Response<AiMessage> response = chatLanguageModel.generate(UserMessage.from(summaryPrompt));
            String summary = response.content().text().trim();
            
            log.debug("生成对话摘要成功: messagesCount={}, summaryLength={}", 
                oldMessages.size(), summary.length());
            
            return summary;
            
        } catch (Exception e) {
            log.warn("生成对话摘要失败: error={}", e.getMessage());
            return null;
        }
    }
    
    /**
     * 从数据库加载历史消息到 ChatMemory
     * 当切换到旧的 conversationId 时，需要将历史消息加载到内存中
     */
    private void loadHistoryToChatMemory(ChatMemory chatMemory, String conversationId, String userId) {
        try {
            // 从数据库获取该 conversationId 的所有历史消息（按时间正序）
            List<ConversationVectorService.ConversationMessage> historyMessages = 
                conversationVectorService.getChatMessages(conversationId, userId, MAX_MEMORY_MESSAGES * 2);
            
            if (historyMessages == null || historyMessages.isEmpty()) {
                log.debug("没有找到历史消息: conversationId={}", conversationId);
                return;
            }
            
            // 将历史消息添加到 ChatMemory（最多保留 MAX_MEMORY_MESSAGES 条）
            int count = 0;
            for (ConversationVectorService.ConversationMessage msg : historyMessages) {
                if (count >= MAX_MEMORY_MESSAGES) {
                    break; // 只保留最近的 MAX_MEMORY_MESSAGES 条
                }
                
                if ("user".equals(msg.getRole())) {
                    chatMemory.add(UserMessage.from(msg.getContent()));
                    count++;
                } else if ("assistant".equals(msg.getRole())) {
                    chatMemory.add(AiMessage.from(msg.getContent()));
                    count++;
                }
            }
            
            log.info("加载历史消息到 ChatMemory: conversationId={}, loadedCount={}", conversationId, count);
            
        } catch (Exception e) {
            log.error("加载历史消息失败: conversationId={}, error={}", conversationId, e.getMessage(), e);
            // 加载失败不影响后续流程，继续使用空的 ChatMemory
        }
    }
    
    /**
     * 注入相关历史到 ChatMemory（如果不在内存中）
     */
    private void injectRelevantHistory(ChatMemory chatMemory, 
                                      List<ConversationVectorService.ConversationMessage> relevantHistory,
                                      List<ChatMessage> memoryMessages) {
        if (relevantHistory == null || relevantHistory.isEmpty()) {
            return;
        }
        
        // 检查哪些相关历史不在内存中（用于日志记录）
        int relevantCount = relevantHistory.size();
        int memorySize = memoryMessages.size();
        
        // 将不在内存中的相关历史添加到 ChatMemory（作为系统消息或上下文）
        // 注意：这里简化实现，不直接修改 ChatMemory，而是通过系统提示词注入
        // 如果需要更精确的控制，可以实现自定义 ChatMemory
        log.debug("发现相关历史: count={}, memorySize={}", relevantCount, memorySize);
    }
    
    // 注意：以下方法已移除，因为现在完全使用 Agent 和 Function Calling 机制：
    // - enhanceQueryWithAgent: Agent 会自动理解用户意图并调用工具
    // - generateSearchExplanation: Agent 会自动生成搜索结果的解释
    // - buildSearchContext: 不再需要手动构建搜索上下文，工具会返回格式化的结果
    
    /**
     * Agent 搜索结果
     */
    public static class AgentSearchResult {
        private List<Commodity> commodities;
        private String explanation;
        private String originalQuery;
        private String enhancedQuery;
        
        // Getters and Setters
        public List<Commodity> getCommodities() { return commodities; }
        public void setCommodities(List<Commodity> commodities) { this.commodities = commodities; }
        
        public String getExplanation() { return explanation; }
        public void setExplanation(String explanation) { this.explanation = explanation; }
        
        public String getOriginalQuery() { return originalQuery; }
        public void setOriginalQuery(String originalQuery) { this.originalQuery = originalQuery; }
        
        public String getEnhancedQuery() { return enhancedQuery; }
        public void setEnhancedQuery(String enhancedQuery) { this.enhancedQuery = enhancedQuery; }
    }
}

