package com.njumarket.ai.service;

import com.njumarket.ai.tool.SearchCommoditiesTool;
import com.njumarket.ai.client.AuthClient;
import com.njumarket.ai.vector.ConversationVectorService;
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
    @SuppressWarnings("unused")
    private final EmbeddingModel embeddingModel;
    private final ConversationVectorService conversationVectorService;
    private final AuthClient authClient;
    private final SearchCommoditiesTool searchCommoditiesTool;
    
    private final ShoppingAssistant shoppingAssistant;
    private final Map<String, ChatMemory> chatMemoryMap = new HashMap<>();
    
    private static final int MAX_MEMORY_MESSAGES = 15;
    private static final int MAX_SUMMARY_TRIGGER = 20;
    private static final int MAX_CONVERSATION_HISTORY = 5;
    
    public interface ShoppingAssistant {
        String chat(@MemoryId String memoryId, @dev.langchain4j.service.UserMessage String userMessage);
    }
    
    private final Map<String, ConversationContext> contextMap = new HashMap<>();
    
    private static class ConversationContext {
        String userId;
        String userProfileContext;
        String conversationSummary;
        List<ConversationVectorService.ConversationMessage> relevantHistory;
        @SuppressWarnings("unused")
        long lastUpdateTime;
    }
    
    public AIAgentService(ChatLanguageModel chatLanguageModel,
                         EmbeddingModel embeddingModel,
                         ConversationVectorService conversationVectorService,
                         AuthClient authClient,
                         SearchCommoditiesTool searchCommoditiesTool) {
        this.chatLanguageModel = chatLanguageModel;
        this.embeddingModel = embeddingModel;
        this.conversationVectorService = conversationVectorService;
        this.authClient = authClient;
        this.searchCommoditiesTool = searchCommoditiesTool;
        
        this.shoppingAssistant = AiServices.builder(ShoppingAssistant.class)
            .chatLanguageModel(chatLanguageModel)
            .tools(searchCommoditiesTool)
            .chatMemoryProvider(chatMemoryId -> {
                String memoryId = chatMemoryId != null ? chatMemoryId.toString() : "default";
                return chatMemoryMap.computeIfAbsent(memoryId, 
                    id -> MessageWindowChatMemory.withMaxMessages(MAX_MEMORY_MESSAGES));
            })
            .systemMessageProvider(chatMemoryId -> {
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
    
    public static class ChatResult {
        private String reply;
        private List<SearchCommoditiesTool.CommodityDTO> recommendedCommodities;
        
        public ChatResult(String reply, List<SearchCommoditiesTool.CommodityDTO> recommendedCommodities) {
            this.reply = reply;
            this.recommendedCommodities = recommendedCommodities != null ? recommendedCommodities : new ArrayList<>();
        }
        
        public String getReply() { return reply; }
        public List<SearchCommoditiesTool.CommodityDTO> getRecommendedCommodities() { return recommendedCommodities; }
    }
    
    public interface StreamChatCallback {
        void onToken(String token);
        void onComplete(String fullReply, List<SearchCommoditiesTool.CommodityDTO> recommendedCommodities);
        void onError(String error);
    }
    
    public ChatResult chat(String userMessage, String userId, String conversationId) {
        try {
            if (!StringUtils.hasText(userMessage) || !StringUtils.hasText(userId)) {
                return new ChatResult("抱歉，我无法理解您的问题。", new ArrayList<>());
            }
            
            searchCommoditiesTool.setLastSearchResults(new ArrayList<>());
            
            String memoryId = StringUtils.hasText(conversationId) ? conversationId : userId + "_default";
            
            String userProfileContext = getUserProfileContext(userId);
            
            List<ConversationVectorService.ConversationMessage> relevantHistory = 
                conversationVectorService.searchRelevantConversations(userMessage, userId, MAX_CONVERSATION_HISTORY);
            
            ChatMemory chatMemory = chatMemoryMap.computeIfAbsent(memoryId, 
                id -> {
                    ChatMemory newMemory = MessageWindowChatMemory.withMaxMessages(MAX_MEMORY_MESSAGES);
                    if (StringUtils.hasText(conversationId)) {
                        loadHistoryToChatMemory(newMemory, conversationId, userId);
                    }
                    return newMemory;
                });
            
            String conversationSummary = null;
            List<ChatMessage> memoryMessages = chatMemory.messages();
            if (memoryMessages.size() >= MAX_SUMMARY_TRIGGER) {
                conversationSummary = summarizeOldMessages(memoryMessages, MAX_MEMORY_MESSAGES);
                log.debug("生成对话摘要: memoryId={}, summaryLength={}", memoryId, 
                    conversationSummary != null ? conversationSummary.length() : 0);
            }
            
            ConversationContext context = new ConversationContext();
            context.userId = userId;
            context.userProfileContext = userProfileContext;
            context.conversationSummary = conversationSummary;
            context.relevantHistory = relevantHistory;
            context.lastUpdateTime = System.currentTimeMillis();
            contextMap.put(memoryId, context);
            
            injectRelevantHistory(chatMemory, relevantHistory, memoryMessages);
            
            String assistantReply = shoppingAssistant.chat(memoryId, userMessage);
            
            List<SearchCommoditiesTool.CommodityDTO> recommendedCommodities = searchCommoditiesTool.getLastSearchResults();
            
            if (StringUtils.hasText(conversationId)) {
                String userMessageId = UUID.randomUUID().toString();
                String assistantMessageId = UUID.randomUUID().toString();
                
                conversationVectorService.storeConversationVector(
                    conversationId, userMessageId, userId, userMessage, "user");
                
                List<String> commodityIds = recommendedCommodities != null && !recommendedCommodities.isEmpty()
                    ? recommendedCommodities.stream()
                        .map(SearchCommoditiesTool.CommodityDTO::getCommodityId)
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
    
    public void chatStream(String userMessage, String userId, String conversationId, StreamChatCallback callback) {
        try {
            if (!StringUtils.hasText(userMessage) || !StringUtils.hasText(userId)) {
                callback.onError("用户消息或用户ID不能为空");
                return;
            }
            
            searchCommoditiesTool.setLastSearchResults(new ArrayList<>());
            
            String memoryId = StringUtils.hasText(conversationId) ? conversationId : userId + "_default";
            
            String userProfileContext = getUserProfileContext(userId);
            
            List<ConversationVectorService.ConversationMessage> relevantHistory = 
                conversationVectorService.searchRelevantConversations(userMessage, userId, MAX_CONVERSATION_HISTORY);
            
            ChatMemory chatMemory = chatMemoryMap.computeIfAbsent(memoryId, 
                id -> {
                    ChatMemory newMemory = MessageWindowChatMemory.withMaxMessages(MAX_MEMORY_MESSAGES);
                    if (StringUtils.hasText(conversationId)) {
                        loadHistoryToChatMemory(newMemory, conversationId, userId);
                    }
                    return newMemory;
                });
            
            String conversationSummary = null;
            List<ChatMessage> memoryMessages = chatMemory.messages();
            if (memoryMessages.size() >= MAX_SUMMARY_TRIGGER) {
                conversationSummary = summarizeOldMessages(memoryMessages, MAX_MEMORY_MESSAGES);
                log.debug("生成对话摘要: memoryId={}, summaryLength={}", memoryId, 
                    conversationSummary != null ? conversationSummary.length() : 0);
            }
            
            String systemPrompt = buildEnhancedSystemPrompt(userId, userProfileContext, conversationSummary, relevantHistory);
            
            List<ChatMessage> allMessages = new ArrayList<>();
            allMessages.add(SystemMessage.from(systemPrompt));
            allMessages.addAll(chatMemory.messages());
            allMessages.add(UserMessage.from(userMessage));
            
            StringBuilder fullReply = new StringBuilder();
            
            ConversationContext context = new ConversationContext();
            context.userId = userId;
            context.userProfileContext = getUserProfileContext(userId);
            context.conversationSummary = conversationSummary;
            context.relevantHistory = relevantHistory;
            context.lastUpdateTime = System.currentTimeMillis();
            contextMap.put(memoryId, context);
            
            injectRelevantHistory(chatMemory, relevantHistory, memoryMessages);
            
            CompletableFuture.runAsync(() -> {
                try {
                    String completeReply = shoppingAssistant.chat(memoryId, userMessage);
                    
                    if (completeReply != null) {
                        completeReply = completeReply
                            .replaceAll("<\\|FunctionCallBegin\\|>.*?<\\|FunctionCallEnd\\|>", "")
                            .replaceAll("<\\|FunctionCallBegin\\|>", "")
                            .replaceAll("<\\|FunctionCallEnd\\|>", "")
                            .trim();
                    }
                    
                    if (completeReply == null || completeReply.trim().isEmpty()) {
                        completeReply = "我已经为您搜索了相关商品，请查看推荐列表。";
                    }
                    
                    int chunkSize = 3;
                    for (int i = 0; i < completeReply.length(); i += chunkSize) {
                        int end = Math.min(i + chunkSize, completeReply.length());
                        String chunk = completeReply.substring(i, end);
                        fullReply.append(chunk);
                        callback.onToken(chunk);
                        
                        try {
                            Thread.sleep(10);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            break;
                        }
                    }
                    
                    String finalReply = fullReply.toString();
                    
                    List<SearchCommoditiesTool.CommodityDTO> recommendedCommodities = searchCommoditiesTool.getLastSearchResults();
                    
                    if (StringUtils.hasText(conversationId)) {
                        String userMessageId = UUID.randomUUID().toString();
                        String assistantMessageId = UUID.randomUUID().toString();
                        
                        conversationVectorService.storeConversationVector(
                            conversationId, userMessageId, userId, userMessage, "user");
                        
                        List<String> commodityIds = recommendedCommodities != null && !recommendedCommodities.isEmpty()
                            ? recommendedCommodities.stream()
                                .map(SearchCommoditiesTool.CommodityDTO::getCommodityId)
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
    
    public AgentSearchResult intelligentSearch(String query, String userId, String conversationId) {
        try {
            ChatResult chatResult = chat(query, userId, conversationId);
            
            AgentSearchResult result = new AgentSearchResult();
            result.setCommodities(chatResult.getRecommendedCommodities() != null ? 
                chatResult.getRecommendedCommodities() : new ArrayList<>());
            result.setExplanation(chatResult.getReply());
            result.setOriginalQuery(query);
            result.setEnhancedQuery(query);
            
            log.info("智能搜索完成（通过Agent）: userId={}, query={}, commodities={}", 
                userId, query, result.getCommodities().size());
            
            return result;
            
        } catch (Exception e) {
            log.error("智能搜索失败: userId={}, error={}", userId, e.getMessage(), e);
            AgentSearchResult result = new AgentSearchResult();
            result.setCommodities(new ArrayList<>());
            result.setExplanation("抱歉，搜索时遇到了问题，请稍后重试。");
            result.setOriginalQuery(query);
            result.setEnhancedQuery(query);
            return result;
        }
    }
    
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
    
    private String buildEnhancedSystemPrompt(String userId, String userProfileContext, 
                                            String conversationSummary, 
                                            List<ConversationVectorService.ConversationMessage> relevantHistory) {
        StringBuilder prompt = new StringBuilder(buildSystemPrompt(userId));
        
        if (StringUtils.hasText(userProfileContext)) {
            prompt.append("\n\n=== 用户画像信息 ===\n");
            prompt.append(userProfileContext);
            prompt.append("\n请根据用户的偏好和需求提供个性化推荐。\n");
        }
        
        if (StringUtils.hasText(conversationSummary)) {
            prompt.append("\n\n=== 之前的对话摘要 ===\n");
            prompt.append(conversationSummary);
            prompt.append("\n这是之前对话的摘要，请参考这些信息理解上下文。\n");
        }
        
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
    
    private String getUserProfileContext(String userId) {
        try {
            Result result = authClient.getUserProfileVector(userId);
            if (result != null && result.getSuccess() != null && result.getSuccess() && result.getData() != null) {
                return "用户已建立购物偏好画像，请根据历史行为提供个性化推荐。";
            }
        } catch (Exception e) {
            log.debug("获取用户画像失败（可选）: userId={}, error={}", userId, e.getMessage());
        }
        return null;
    }
    
    private String summarizeOldMessages(List<ChatMessage> messages, int keepRecent) {
        if (messages == null || messages.size() <= keepRecent) {
            return null;
        }
        
        try {
            int toSummarize = messages.size() - keepRecent;
            List<ChatMessage> oldMessages = messages.subList(0, toSummarize);
            
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
    
    private void loadHistoryToChatMemory(ChatMemory chatMemory, String conversationId, String userId) {
        try {
            List<ConversationVectorService.ConversationMessage> historyMessages = 
                conversationVectorService.getChatMessages(conversationId, userId, MAX_MEMORY_MESSAGES * 2);
            
            if (historyMessages == null || historyMessages.isEmpty()) {
                log.debug("没有找到历史消息: conversationId={}", conversationId);
                return;
            }
            
            int count = 0;
            for (ConversationVectorService.ConversationMessage msg : historyMessages) {
                if (count >= MAX_MEMORY_MESSAGES) {
                    break;
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
        }
    }
    
    private void injectRelevantHistory(ChatMemory chatMemory, 
                                      List<ConversationVectorService.ConversationMessage> relevantHistory,
                                      List<ChatMessage> memoryMessages) {
        if (relevantHistory == null || relevantHistory.isEmpty()) {
            return;
        }
        
        int relevantCount = relevantHistory.size();
        int memorySize = memoryMessages.size();
        
        log.debug("发现相关历史: count={}, memorySize={}", relevantCount, memorySize);
    }
    
    public static class AgentSearchResult {
        private List<SearchCommoditiesTool.CommodityDTO> commodities;
        private String explanation;
        private String originalQuery;
        private String enhancedQuery;
        
        public List<SearchCommoditiesTool.CommodityDTO> getCommodities() { return commodities; }
        public void setCommodities(List<SearchCommoditiesTool.CommodityDTO> commodities) { this.commodities = commodities; }
        
        public String getExplanation() { return explanation; }
        public void setExplanation(String explanation) { this.explanation = explanation; }
        
        public String getOriginalQuery() { return originalQuery; }
        public void setOriginalQuery(String originalQuery) { this.originalQuery = originalQuery; }
        
        public String getEnhancedQuery() { return enhancedQuery; }
        public void setEnhancedQuery(String enhancedQuery) { this.enhancedQuery = enhancedQuery; }
    }
}

