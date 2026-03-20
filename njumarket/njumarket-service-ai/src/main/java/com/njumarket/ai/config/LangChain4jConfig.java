package com.njumarket.ai.config;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.time.Duration;

/**
 * LangChain4j 配置类（兼容 1.0.1 API）
 *
 * <p>同时注册同步 {@link ChatModel} 和流式 {@link StreamingChatModel}，
 * 指向同一 API 端点和模型，由 AiServices 根据接口方法返回类型自动选择：
 * <ul>
 *   <li>返回 {@code String} → 使用同步模型</li>
 *   <li>返回 {@code Flux<String>} → 使用流式模型</li>
 * </ul>
 */
@Slf4j
@Configuration
public class LangChain4jConfig {

    @Value("${langchain4j.open-ai.api-key:${DOUBAO_API_KEY:8736b46e-a38e-4d2c-8859-0460a1167310}}")
    private String apiKey;

    @Value("${langchain4j.open-ai.base-url:${DOUBAO_BASE_URL:https://ark.cn-beijing.volces.com/api/v3}}")
    private String baseUrl;

    @Value("${langchain4j.open-ai.chat-model.model-name:${DOUBAO_CHAT_MODEL:doubao-seed-1-6-250615}}")
    private String chatModel;

    @Value("${langchain4j.open-ai.embedding-model.model-name:${DOUBAO_EMBEDDING_MODEL:doubao-embedding-text-240715}}")
    private String embeddingModel;

    /** 同步对话模型（用于 ShoppingAssistant.chat() 返回 String） */
    @Bean
    @Primary
    public ChatModel chatModel() {
        log.info("配置同步 Chat 模型 - base-url: {}, model: {}, api-key: {}",
            baseUrl, chatModel, maskApiKey(apiKey));
        return OpenAiChatModel.builder()
            .apiKey(apiKey)
            .baseUrl(baseUrl)
            .modelName(chatModel)
            .temperature(0.7)
            .maxCompletionTokens(2000)
            .timeout(Duration.ofSeconds(60))
            .logRequests(true)
            .logResponses(true)
            .build();
    }

    /** 流式对话模型（用于 ShoppingAssistant.chatStream() 返回 Flux<String>） */
    @Bean
    public StreamingChatModel streamingChatModel() {
        log.info("配置流式 Chat 模型 - base-url: {}, model: {}, api-key: {}",
            baseUrl, chatModel, maskApiKey(apiKey));
        return OpenAiStreamingChatModel.builder()
            .apiKey(apiKey)
            .baseUrl(baseUrl)
            .modelName(chatModel)
            .temperature(0.7)
            .maxCompletionTokens(2000)
            .timeout(Duration.ofSeconds(60))
            .logRequests(true)
            .logResponses(true)
            .build();
    }

    @Bean
    public EmbeddingModel embeddingModel() {
        log.info("配置 Embedding 模型 - base-url: {}, model: {}, api-key: {}",
            baseUrl, embeddingModel, maskApiKey(apiKey));
        return OpenAiEmbeddingModel.builder()
            .apiKey(apiKey)
            .baseUrl(baseUrl)
            .modelName(embeddingModel)
            .timeout(Duration.ofSeconds(60))
            .logRequests(false)
            .logResponses(false)
            .build();
    }

    private String maskApiKey(String key) {
        if (key == null || key.length() <= 8) return "***";
        return key.substring(0, 4) + "..." + key.substring(key.length() - 4);
    }
}
