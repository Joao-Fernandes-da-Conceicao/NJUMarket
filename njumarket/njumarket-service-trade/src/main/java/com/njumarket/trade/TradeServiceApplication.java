package com.njumarket.trade;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 交易服务：合并原商品服务与订单服务，统一部署。
 */
@SpringBootApplication(scanBasePackages = {
        "com.njumarket.trade",
        "com.njumarket.njumarket"
})
@EntityScan(basePackages = "com.njumarket.trade.entity")
@EnableJpaRepositories(basePackages = "com.njumarket.trade.repository")
@EnableDiscoveryClient
@EnableFeignClients(basePackages = "com.njumarket")
@EnableAsync
@EnableScheduling
public class TradeServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(TradeServiceApplication.class, args);
    }
}
