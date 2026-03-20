package com.njumarket.ai;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableAsync;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

@Slf4j
@SpringBootApplication(scanBasePackages = {
        "com.njumarket.ai",
        "com.njumarket.njumarket"
})
@EnableDiscoveryClient
@EnableFeignClients(basePackages = "com.njumarket")
@EnableAsync
public class AIServiceApplication {

    static {
        Charset def = Charset.defaultCharset();
        if (!StandardCharsets.UTF_8.equals(def)) {
            log.warn("AI 服务默认编码为 {}，非 UTF-8，可能导致 LLM 流式响应乱码。请添加 JVM 参数: -Dfile.encoding=UTF-8", def);
        }
    }

    public static void main(String[] args) {
        SpringApplication.run(AIServiceApplication.class, args);
    }
}

