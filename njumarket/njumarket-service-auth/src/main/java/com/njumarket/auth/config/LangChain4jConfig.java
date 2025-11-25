package com.njumarket.auth.config;

import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.time.Duration;

/**
 * LangChain4j 配置类（Auth Service）
 * 配置 Doubao API（兼容 OpenAI API）
 */
@Slf4j
@Configuration
public class LangChain4jConfig {
    
    @Value("${langchain4j.open-ai.api-key:${DOUBAO_API_KEY:8736b46e-a38e-4d2c-8859-0460a1167310}}")
    private String apiKey;
    
    @Value("${langchain4j.open-ai.base-url:${DOUBAO_BASE_URL:https://ark.cn-beijing.volces.com/api/v3}}")
    private String baseUrl;
    
    @Value("${langchain4j.open-ai.embedding-model:${DOUBAO_EMBEDDING_MODEL:doubao-embedding-text-240715}}")
    private String embeddingModel;
    
    /**
     * 配置 Embedding 模型（Auth Service 只需要 Embedding）
     */
    @Bean
    @Primary
    public EmbeddingModel embeddingModel() {
        log.info("配置 LangChain4j Embedding 模型 - base-url: {}, model: {}, api-key: {}", 
            baseUrl, embeddingModel, maskApiKey(apiKey));
        
        return OpenAiEmbeddingModel.builder()
            .apiKey(apiKey)
            .baseUrl(baseUrl)
            .modelName(embeddingModel)
            .timeout(Duration.ofSeconds(60))
            .logRequests(true)
            .logResponses(true)
            .build();
    }
    
    private String maskApiKey(String apiKey) {
        if (apiKey == null || apiKey.length() <= 8) {
            return "***";
        }
        return apiKey.substring(0, 4) + "..." + apiKey.substring(apiKey.length() - 4);
    }
}

