package com.njumarket.ai.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "njumarket.ai.milvus")
public class MilvusProperties {

    private boolean enabled = true;
    private String uri = "http://localhost:19530";
    private String token = "";
    private String dbName = "default";
    private String commodityCollection = "commodity_vectors";
    private String userProfileCollection = "user_profile_vectors";
    private String conversationCollection = "conversation_memory_vectors";
    private int dimension = 1024;
    private int topK = 10;
    private String metricType = "COSINE";
    private int memoryTopK = 12;
}

