package com.njumarket.admin;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = {
        "com.njumarket.admin",
        "com.njumarket.njumarket"
})
@EntityScan(basePackages = "com.njumarket.admin.entity")
@EnableJpaRepositories(basePackages = "com.njumarket.admin.repository")
@EnableDiscoveryClient
@EnableFeignClients(basePackages = "com.njumarket")
public class AdminServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AdminServiceApplication.class, args);
    }
}

