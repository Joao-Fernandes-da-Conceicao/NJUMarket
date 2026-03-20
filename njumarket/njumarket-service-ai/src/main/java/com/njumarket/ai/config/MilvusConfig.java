package com.njumarket.ai.config;

import io.milvus.v2.client.ConnectConfig;
import io.milvus.v2.client.MilvusClientV2;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class MilvusConfig {

    private final MilvusProperties milvusProperties;

    @Bean(destroyMethod = "close")
    public MilvusClientV2 milvusClientV2() {
        ConnectConfig.ConnectConfigBuilder<?, ?> builder = ConnectConfig.builder()
                .uri(milvusProperties.getUri())
                .dbName(milvusProperties.getDbName());
        if (milvusProperties.getToken() != null && !milvusProperties.getToken().isBlank()) {
            builder.token(milvusProperties.getToken());
        }
        return new MilvusClientV2(builder.build());
    }
}

