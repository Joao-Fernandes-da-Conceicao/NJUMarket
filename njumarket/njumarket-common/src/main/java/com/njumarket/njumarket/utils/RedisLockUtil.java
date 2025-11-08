package com.njumarket.njumarket.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Redis分布式锁工具类
 */
@Slf4j
@Component
public class RedisLockUtil {
    
    private final StringRedisTemplate stringRedisTemplate;
    
    private static final String UNLOCK_SCRIPT = 
        "if redis.call('get', KEYS[1]) == ARGV[1] then " +
        "    return redis.call('del', KEYS[1]) " +
        "else " +
        "    return 0 " +
        "end";
    
    public RedisLockUtil(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }
    
    /**
     * 生成锁的值（UUID）
     */
    public static String generateLockValue() {
        return UUID.randomUUID().toString();
    }
    
    /**
     * 尝试获取锁
     * @param key 锁的key
     * @param value 锁的value
     * @param timeout 超时时间（秒）
     * @return 是否获取成功
     */
    public boolean tryLock(String key, String value, long timeout) {
        try {
            Boolean result = stringRedisTemplate.opsForValue()
                .setIfAbsent(key, value, timeout, TimeUnit.SECONDS);
            return Boolean.TRUE.equals(result);
        } catch (Exception e) {
            log.error("获取锁失败: key={}, error={}", key, e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * 尝试获取锁（带重试）
     * @param key 锁的key
     * @param value 锁的value
     * @param timeout 超时时间（秒）
     * @param waitTime 等待时间（秒）
     * @param retryInterval 重试间隔（毫秒）
     * @return 是否获取成功
     */
    public boolean tryLock(String key, String value, long timeout, long waitTime, long retryInterval) {
        long endTime = System.currentTimeMillis() + waitTime * 1000;
        while (System.currentTimeMillis() < endTime) {
            if (tryLock(key, value, timeout)) {
                return true;
            }
            try {
                Thread.sleep(retryInterval);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        return false;
    }
    
    /**
     * 释放锁
     * @param key 锁的key
     * @param value 锁的value
     * @return 是否释放成功
     */
    public boolean releaseLock(String key, String value) {
        try {
            DefaultRedisScript<Long> script = new DefaultRedisScript<>();
            script.setScriptText(UNLOCK_SCRIPT);
            script.setResultType(Long.class);
            Long result = stringRedisTemplate.execute(script, 
                Collections.singletonList(key), value);
            return result != null && result > 0;
        } catch (Exception e) {
            log.error("释放锁失败: key={}, error={}", key, e.getMessage(), e);
            return false;
        }
    }
}
