package com.njumarket.gateway.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Gateway Redis配置类
 * 显式配置ReactiveStringRedisTemplate，确保Redis连接正确
 */
@Slf4j
@Configuration
public class RedisConfig {
    
    @Bean
    public ReactiveStringRedisTemplate reactiveStringRedisTemplate(
            ReactiveRedisConnectionFactory connectionFactory) {
        RedisSerializationContext<String, String> serializationContext = 
            RedisSerializationContext.<String, String>newSerializationContext()
                .key(StringRedisSerializer.UTF_8)
                .value(StringRedisSerializer.UTF_8)
                .hashKey(StringRedisSerializer.UTF_8)
                .hashValue(StringRedisSerializer.UTF_8)
                .build();
        
        ReactiveStringRedisTemplate template = new ReactiveStringRedisTemplate(
            connectionFactory, 
            serializationContext
        );
        
        log.info("ReactiveStringRedisTemplate 已配置");
        return template;
    }
    
    /**
     * 启动时测试Redis连接
     */
    @Bean
    public CommandLineRunner redisConnectionTest(
            ReactiveStringRedisTemplate reactiveStringRedisTemplate,
            ReactiveRedisConnectionFactory connectionFactory) {
        return args -> {
            log.info("开始测试 Gateway Redis 连接...");
            
            // 测试基本连接
            reactiveStringRedisTemplate.opsForValue()
                .set("gateway:test:connection", "ok", java.time.Duration.ofSeconds(10))
                .doOnSuccess(result -> {
                    log.info("✅ Gateway Redis 写入测试成功");
                })
                .doOnError(error -> {
                    log.error("❌ Gateway Redis 写入测试失败: {}", error.getMessage(), error);
                })
                .then(reactiveStringRedisTemplate.opsForValue().get("gateway:test:connection"))
                .doOnNext(value -> {
                    if ("ok".equals(value)) {
                        log.info("✅ Gateway Redis 读写测试成功");
                    } else {
                        log.warn("⚠️ Gateway Redis 读取的值不匹配: {}", value);
                    }
                })
                .doOnError(error -> {
                    log.error("❌ Gateway Redis 读取测试失败: {}", error.getMessage(), error);
                })
                .then(reactiveStringRedisTemplate.hasKey("login:token:test"))
                .doOnNext(exists -> {
                    log.info("✅ Gateway Redis hasKey 测试成功: exists={}", exists);
                })
                .doOnError(error -> {
                    log.error("❌ Gateway Redis hasKey 测试失败: {}", error.getMessage(), error);
                })
                .subscribe();
            
            // 延迟测试，确保服务已完全启动
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            
            // 测试能否读取 login:token:* 格式的 key（用于验证数据库是否正确）
            reactiveStringRedisTemplate.keys("login:token:*")
                .collectList()
                .doOnNext(keys -> {
                    log.info("✅ Gateway Redis 查询 login:token:* 成功，找到 {} 个key", keys.size());
                    if (keys.size() > 0) {
                        log.info("示例key: {}", keys.get(0));
                    }
                })
                .doOnError(error -> {
                    log.error("❌ Gateway Redis 查询 login:token:* 失败: {}", error.getMessage(), error);
                })
                .subscribe();
        };
    }
}

