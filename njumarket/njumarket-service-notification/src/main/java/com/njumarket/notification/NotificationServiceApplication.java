package com.njumarket.notification;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 推送服务启动类
 * 统一管理所有推送功能：WebSocket、离线队列、重试机制、增量轮询
 */
@SpringBootApplication(scanBasePackages = {
        "com.njumarket.notification",
        "com.njumarket.njumarket"
})
@EnableFeignClients(basePackages = {
        "com.njumarket.notification.client",
        "com.njumarket"
})
@EnableDiscoveryClient
@EnableScheduling
public class NotificationServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(NotificationServiceApplication.class, args);
    }
}

