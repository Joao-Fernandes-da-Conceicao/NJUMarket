package com.njumarket.ai;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication(scanBasePackages = {
        "com.njumarket.ai",
        "com.njumarket.njumarket"
})
@EntityScan(basePackages = "com.njumarket.ai.entity")
@EnableJpaRepositories(basePackages = "com.njumarket.ai.repository")
@EnableDiscoveryClient
@EnableFeignClients(basePackages = "com.njumarket")
@EnableAsync
public class AIServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AIServiceApplication.class, args);
    }
}

