package com.njumarket.auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = {
        "com.njumarket.auth",
        "com.njumarket.njumarket"
})
@EntityScan(basePackages = "com.njumarket.njumarket.entity")
@EnableJpaRepositories(basePackages = "com.njumarket.auth.repository")
@EnableDiscoveryClient
@EnableFeignClients(basePackages = "com.njumarket")
public class AuthServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AuthServiceApplication.class, args);
    }
}

