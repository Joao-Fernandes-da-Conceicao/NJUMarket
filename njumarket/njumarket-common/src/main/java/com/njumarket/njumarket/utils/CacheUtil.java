package com.njumarket.njumarket.utils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * 通用缓存工具类
 * 支持强一致性和最终一致性两种缓存模式
 * 
 * 强一致性模式：适用于需要实时准确数据的场景（如下单时的库存查询）
 * - 写入时：先更新数据库，再删除缓存（Cache-Aside模式）
 * - 读取时：先查缓存，缓存未命中则查数据库并写入缓存
 * 
 * 最终一致性模式：适用于允许短暂数据不一致的场景（如商品列表、用户信息）
 * - 写入时：先更新数据库，再更新缓存（Write-Through模式）
 * - 读取时：先查缓存，缓存未命中则查数据库并写入缓存
 * 
 * 注意：此组件只在存在 RedisTemplate Bean 时才会创建，适用于需要 Redis 缓存的服务
 */
@Slf4j
@Component
@ConditionalOnBean(RedisTemplate.class)
public class CacheUtil {
    
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;
    
    public CacheUtil(RedisTemplate<String, Object> redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }
    
    /**
     * 最终一致性缓存：获取缓存，如果不存在则从数据源加载并缓存
     * 适用于商品列表、用户信息等允许短暂不一致的场景
     * 
     * @param key 缓存key
     * @param ttl 过期时间（秒）
     * @param typeReference 类型引用（用于反序列化）
     * @param dataLoader 数据加载器（缓存未命中时调用）
     * @param <T> 数据类型
     * @return 缓存的数据
     */
    public <T> T getWithFallback(String key, long ttl, TypeReference<T> typeReference, Supplier<T> dataLoader) {
        try {
            // 1. 尝试从缓存获取
            Object cached = redisTemplate.opsForValue().get(key);
            if (cached != null) {
                log.debug("缓存命中: key={}", key);
                return objectMapper.convertValue(cached, typeReference);
            }
            
            // 2. 缓存未命中，从数据源加载
            log.debug("缓存未命中，从数据源加载: key={}", key);
            T data = dataLoader.get();
            
            // 3. 将数据写入缓存（最终一致性：允许短暂不一致）
            if (data != null) {
                set(key, data, ttl);
            }
            
            return data;
            
        } catch (Exception e) {
            log.error("缓存操作失败，降级到数据源: key={}, error={}", key, e.getMessage(), e);
            // 缓存异常时降级到数据源
            return dataLoader.get();
        }
    }
    
    /**
     * 最终一致性缓存：获取缓存，如果不存在则从数据源加载并缓存（泛型版本）
     * 
     * @param key 缓存key
     * @param ttl 过期时间（秒）
     * @param clazz 类型
     * @param dataLoader 数据加载器
     * @param <T> 数据类型
     * @return 缓存的数据
     */
    public <T> T getWithFallback(String key, long ttl, Class<T> clazz, Supplier<T> dataLoader) {
        try {
            // 1. 尝试从缓存获取
            Object cached = redisTemplate.opsForValue().get(key);
            if (cached != null) {
                log.debug("缓存命中: key={}", key);
                return objectMapper.convertValue(cached, clazz);
            }
            
            // 2. 缓存未命中，从数据源加载
            log.debug("缓存未命中，从数据源加载: key={}", key);
            T data = dataLoader.get();
            
            // 3. 将数据写入缓存
            if (data != null) {
                set(key, data, ttl);
            }
            
            return data;
            
        } catch (Exception e) {
            log.error("缓存操作失败，降级到数据源: key={}, error={}", key, e.getMessage(), e);
            // 缓存异常时降级到数据源
            return dataLoader.get();
        }
    }
    
    /**
     * 强一致性缓存：获取缓存，如果不存在则从数据源加载并缓存
     * 适用于需要实时准确数据的场景（如下单时的库存查询）
     * 
     * 注意：强一致性模式下，写入操作应该先更新数据库，再删除缓存（Cache-Aside模式）
     * 读取操作使用此方法：先查缓存，缓存未命中则查数据库并写入缓存
     * 
     * @param key 缓存key
     * @param ttl 过期时间（秒）
     * @param typeReference 类型引用
     * @param dataLoader 数据加载器
     * @param <T> 数据类型
     * @return 缓存的数据
     */
    public <T> T getWithStrongConsistency(String key, long ttl, TypeReference<T> typeReference, Supplier<T> dataLoader) {
        try {
            // 1. 尝试从缓存获取
            Object cached = redisTemplate.opsForValue().get(key);
            if (cached != null) {
                log.debug("缓存命中（强一致性）: key={}", key);
                return objectMapper.convertValue(cached, typeReference);
            }
            
            // 2. 缓存未命中，从数据源加载
            log.debug("缓存未命中，从数据源加载（强一致性）: key={}", key);
            T data = dataLoader.get();
            
            // 3. 将数据写入缓存
            if (data != null) {
                set(key, data, ttl);
            }
            
            return data;
            
        } catch (Exception e) {
            log.error("缓存操作失败，降级到数据源（强一致性）: key={}, error={}", key, e.getMessage(), e);
            // 缓存异常时降级到数据源
            return dataLoader.get();
        }
    }
    
    /**
     * 强一致性缓存：获取缓存（泛型版本）
     */
    public <T> T getWithStrongConsistency(String key, long ttl, Class<T> clazz, Supplier<T> dataLoader) {
        try {
            Object cached = redisTemplate.opsForValue().get(key);
            if (cached != null) {
                log.debug("缓存命中（强一致性）: key={}", key);
                return objectMapper.convertValue(cached, clazz);
            }
            
            log.debug("缓存未命中，从数据源加载（强一致性）: key={}", key);
            T data = dataLoader.get();
            
            if (data != null) {
                set(key, data, ttl);
            }
            
            return data;
            
        } catch (Exception e) {
            log.error("缓存操作失败，降级到数据源（强一致性）: key={}, error={}", key, e.getMessage(), e);
            return dataLoader.get();
        }
    }
    
    /**
     * 设置缓存（最终一致性模式：写入时更新缓存）
     * 
     * @param key 缓存key
     * @param value 缓存值
     * @param ttl 过期时间（秒）
     */
    public void set(String key, Object value, long ttl) {
        try {
            redisTemplate.opsForValue().set(key, value, ttl, TimeUnit.SECONDS);
            log.debug("缓存写入成功: key={}, ttl={}s", key, ttl);
        } catch (Exception e) {
            log.error("缓存写入失败: key={}, error={}", key, e.getMessage(), e);
            // 缓存写入失败不影响主流程
        }
    }
    
    /**
     * 设置缓存（带Duration）
     */
    public void set(String key, Object value, Duration duration) {
        try {
            redisTemplate.opsForValue().set(key, value, duration);
            log.debug("缓存写入成功: key={}, ttl={}s", key, duration.getSeconds());
        } catch (Exception e) {
            log.error("缓存写入失败: key={}, error={}", key, e.getMessage(), e);
        }
    }
    
    /**
     * 删除缓存（强一致性模式：写入时删除缓存，下次读取时重新加载）
     * 
     * @param key 缓存key
     */
    public void delete(String key) {
        try {
            redisTemplate.delete(key);
            log.debug("缓存删除成功: key={}", key);
        } catch (Exception e) {
            log.error("缓存删除失败: key={}, error={}", key, e.getMessage(), e);
            // 缓存删除失败不影响主流程
        }
    }
    
    /**
     * 批量删除缓存（支持通配符）
     * 
     * @param pattern 匹配模式（如 "cache:commodity:*"）
     */
    public void deleteByPattern(String pattern) {
        try {
            redisTemplate.delete(redisTemplate.keys(pattern));
            log.debug("批量删除缓存成功: pattern={}", pattern);
        } catch (Exception e) {
            log.error("批量删除缓存失败: pattern={}, error={}", pattern, e.getMessage(), e);
        }
    }
    
    /**
     * 检查缓存是否存在
     * 
     * @param key 缓存key
     * @return 是否存在
     */
    public boolean exists(String key) {
        try {
            Boolean exists = redisTemplate.hasKey(key);
            return Boolean.TRUE.equals(exists);
        } catch (Exception e) {
            log.error("检查缓存存在性失败: key={}, error={}", key, e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * 获取缓存（不加载数据源）
     * 
     * @param key 缓存key
     * @param clazz 类型
     * @param <T> 数据类型
     * @return 缓存的数据，不存在返回null
     */
    public <T> T get(String key, Class<T> clazz) {
        try {
            Object cached = redisTemplate.opsForValue().get(key);
            if (cached == null) {
                return null;
            }
            return objectMapper.convertValue(cached, clazz);
        } catch (Exception e) {
            log.error("获取缓存失败: key={}, error={}", key, e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * 获取缓存（使用TypeReference）
     */
    public <T> T get(String key, TypeReference<T> typeReference) {
        try {
            Object cached = redisTemplate.opsForValue().get(key);
            if (cached == null) {
                return null;
            }
            return objectMapper.convertValue(cached, typeReference);
        } catch (Exception e) {
            log.error("获取缓存失败: key={}, error={}", key, e.getMessage(), e);
            return null;
        }
    }
}

