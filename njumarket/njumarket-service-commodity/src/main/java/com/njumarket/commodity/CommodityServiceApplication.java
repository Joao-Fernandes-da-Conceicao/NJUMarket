package com.njumarket.commodity;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = {
        "com.njumarket.commodity",
        "com.njumarket.njumarket"
})
@EntityScan(basePackages = "com.njumarket.commodity.entity")
@EnableJpaRepositories(basePackages = "com.njumarket.commodity.repository")
@EnableDiscoveryClient
@EnableFeignClients(basePackages = "com.njumarket")
public class CommodityServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(CommodityServiceApplication.class, args);
    }
}

