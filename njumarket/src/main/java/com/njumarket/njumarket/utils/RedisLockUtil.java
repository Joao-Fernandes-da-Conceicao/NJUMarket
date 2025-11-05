package com.njumarket.njumarket.utils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

/**
 * Redis分布式锁工具类
 * 
 * 使用Redis实现分布式锁，解决多服务器场景下的并发控制问题
 * 
 * 实现原理：
 * 1. 使用 SET key value NX EX 命令实现原子性加锁
 * 2. 使用Lua脚本实现原子性释放锁（确保只能释放自己的锁）
 * 3. 支持自动续期（可选，防止业务执行时间过长导致锁过期）
 * 
 * 适用场景：
 * - 多服务器部署，共享同一数据库
 * - 需要跨服务器的并发控制
 * - 防止库存超卖、重复下单等并发问题
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RedisLockUtil {

    private final StringRedisTemplate stringRedisTemplate;

    /**
     * 加锁（非阻塞）
     * 
     * @param key 锁的key
     * @param value 锁的value（通常使用线程ID或UUID，用于释放锁时验证）
     * @param timeout 锁的超时时间（秒）
     * @return true表示加锁成功，false表示加锁失败
     */
    public boolean tryLock(String key, String value, long timeout) {
        try {
            Boolean result = stringRedisTemplate.opsForValue()
                    .setIfAbsent(key, value, timeout, TimeUnit.SECONDS);
            return Boolean.TRUE.equals(result);
        } catch (Exception e) {
            log.error("加锁失败 - key: {}, error: {}", key, e.getMessage(), e);
            return false;
        }
    }

    /**
     * 加锁（带重试机制）
     * 
     * @param key 锁的key
     * @param value 锁的value
     * @param timeout 锁的超时时间（秒）
     * @param waitTime 等待时间（秒）
     * @param retryInterval 重试间隔（毫秒）
     * @return true表示加锁成功，false表示超时仍未获得锁
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
                log.warn("加锁等待被中断 - key: {}", key);
                return false;
            }
        }
        
        log.warn("加锁超时 - key: {}, waitTime: {}s", key, waitTime);
        return false;
    }

    /**
     * 释放锁（使用Lua脚本确保原子性）
     * 
     * 原理：使用Lua脚本确保"判断value+删除key"的原子性
     * 防止误删其他线程的锁（比如：锁过期后，其他线程获得锁，当前线程误删）
     * 
     * @param key 锁的key
     * @param value 锁的value（必须与加锁时的value一致）
     * @return true表示释放成功，false表示释放失败（可能是锁已过期或被其他线程持有）
     */
    public boolean releaseLock(String key, String value) {
        try {
            // Lua脚本：只有当key存在且value匹配时才删除
            String script = "if redis.call('get', KEYS[1]) == ARGV[1] then " +
                           "    return redis.call('del', KEYS[1]) " +
                           "else " +
                           "    return 0 " +
                           "end";
            
            DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>();
            redisScript.setScriptText(script);
            redisScript.setResultType(Long.class);
            
            Long result = stringRedisTemplate.execute(redisScript, 
                    Collections.singletonList(key), 
                    value);
            
            boolean released = result != null && result > 0;
            if (!released) {
                log.warn("释放锁失败 - key: {}, value: {}, 可能锁已过期或被其他线程持有", key, value);
            }
            return released;
        } catch (Exception e) {
            log.error("释放锁异常 - key: {}, error: {}", key, e.getMessage(), e);
            return false;
        }
    }

    /**
     * 检查锁是否存在
     * 
     * @param key 锁的key
     * @return true表示锁存在，false表示锁不存在
     */
    public boolean isLocked(String key) {
        try {
            return Boolean.TRUE.equals(stringRedisTemplate.hasKey(key));
        } catch (Exception e) {
            log.error("检查锁状态异常 - key: {}, error: {}", key, e.getMessage(), e);
            return false;
        }
    }

    /**
     * 续期锁（延长锁的过期时间）
     * 
     * @param key 锁的key
     * @param value 锁的value（必须与加锁时的value一致）
     * @param timeout 新的过期时间（秒）
     * @return true表示续期成功，false表示续期失败
     */
    public boolean renewLock(String key, String value, long timeout) {
        try {
            // Lua脚本：只有当key存在且value匹配时才续期
            String script = "if redis.call('get', KEYS[1]) == ARGV[1] then " +
                           "    return redis.call('expire', KEYS[1], ARGV[2]) " +
                           "else " +
                           "    return 0 " +
                           "end";
            
            DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>();
            redisScript.setScriptText(script);
            redisScript.setResultType(Long.class);
            
            Long result = stringRedisTemplate.execute(redisScript, 
                    Collections.singletonList(key), 
                    value, 
                    String.valueOf(timeout));
            
            return result != null && result > 0;
        } catch (Exception e) {
            log.error("续期锁异常 - key: {}, error: {}", key, e.getMessage(), e);
            return false;
        }
    }

    /**
     * 生成锁的value（使用线程ID + 时间戳）
     * 用于区分不同线程的锁，防止误删
     * 
     * @return 锁的value
     */
    public static String generateLockValue() {
        return Thread.currentThread().getId() + "_" + System.currentTimeMillis();
    }
}
