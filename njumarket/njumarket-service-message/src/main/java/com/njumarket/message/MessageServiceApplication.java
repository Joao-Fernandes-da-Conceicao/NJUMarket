package com.njumarket.message;

import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = {
        "com.njumarket.message",
        "com.njumarket.njumarket"
})
@EntityScan(basePackages = "com.njumarket.message.entity")
@EnableJpaRepositories(basePackages = "com.njumarket.message.repository")
@EnableDiscoveryClient
@EnableFeignClients(basePackages = "com.njumarket")
@EnableScheduling
@EnableRabbit  // ✅ 启用 RabbitMQ 监听器（@RabbitListener）
public class MessageServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(MessageServiceApplication.class, args);
    }
}

