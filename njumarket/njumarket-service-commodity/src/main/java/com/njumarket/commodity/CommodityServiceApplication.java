package com.njumarket.commodity;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication(scanBasePackages = {
        "com.njumarket.commodity",
        "com.njumarket.njumarket"
})
@EntityScan(basePackages = "com.njumarket.commodity.entity")
@EnableJpaRepositories(basePackages = "com.njumarket.commodity.repository")
@EnableDiscoveryClient
@EnableFeignClients(basePackages = "com.njumarket")
@EnableAsync
public class CommodityServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(CommodityServiceApplication.class, args);
    }
}

