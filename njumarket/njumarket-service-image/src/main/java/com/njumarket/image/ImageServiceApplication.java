package com.njumarket.image;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = {
        "com.njumarket.image",
        "com.njumarket.njumarket"
})
@EntityScan(basePackages = "com.njumarket.image.entity")
@EnableJpaRepositories(basePackages = "com.njumarket.image.repository")
@EnableDiscoveryClient
@EnableFeignClients(basePackages = "com.njumarket")
public class ImageServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ImageServiceApplication.class, args);
    }
}

