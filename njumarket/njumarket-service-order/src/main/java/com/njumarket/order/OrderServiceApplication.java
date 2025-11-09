package com.njumarket.order;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = {
        "com.njumarket.order",
        "com.njumarket.njumarket"
})
@EntityScan(basePackages = "com.njumarket.order.entity")
@EnableJpaRepositories(basePackages = "com.njumarket.order.repository")
@EnableDiscoveryClient
@EnableFeignClients(basePackages = "com.njumarket")
public class OrderServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(OrderServiceApplication.class, args);
    }
}

