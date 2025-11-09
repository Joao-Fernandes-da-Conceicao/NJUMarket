package com.njumarket.message.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.njumarket.message.dto.RetryMessageDTO;
import com.njumarket.message.service.WebSocketRetryService;
import com.njumarket.message.websocket.WebSocketEventListener;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Set;

/**
 * WebSocket消息推送重试服务实现
 * 
 * 设计思路：
 * 1. 推送前检查用户是否在线（如果不在线，不推送，直接加入重试队列）
 * 2. 如果用户在线但推送失败（内部错误），记录到重试队列
 * 3. 定时任务检查重试队列，如果用户在线且达到重试时间，重试推送
 * 4. 使用指数退避策略（5s, 10s, 20s），最多重试3次
 * 
 * 适用场景：
 * - 用户网络波动导致推送失败
 * - 用户短暂离线后重新上线
 * - 系统内部错误导致的推送失败
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WebSocketRetryServiceImpl implements WebSocketRetryService {
    
    private final SimpMessagingTemplate messagingTemplate;
    private final WebSocketEventListener webSocketEventListener;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;
    
    // Redis重试队列key
    private static final String RETRY_QUEUE_KEY = com.njumarket.njumarket.utils.RedisConstants.WEBSOCKET_RETRY_QUEUE_KEY;
    
    // 消息过期时间（30分钟）
    private static final long MESSAGE_EXPIRE_SECONDS = com.njumarket.njumarket.utils.RedisConstants.WEBSOCKET_RETRY_TTL;
    
    @Override
    public void pushWithRetry(String receiverId, Object messageData, String messageType) {
        try {
            // 检查用户是否在线
            boolean isOnline = webSocketEventListener.isUserOnline(receiverId);
            
            if (!isOnline) {
                // 用户不在线，记录到重试队列
                log.debug("User offline, adding to retry queue: receiverId={}, messageType={}", 
                    receiverId, messageType);
                addToRetryQueue(receiverId, messageData, messageType);
                return;
            }
            
            // 用户在线，尝试推送
            String destination = "/queue/message";
            
            messagingTemplate.convertAndSendToUser(receiverId, destination, messageData);
            log.debug("WebSocket push attempted: receiverId={}, messageType={}", receiverId, messageType);
            
            // 注意：Spring的convertAndSendToUser是异步的，不会抛出异常
            // 如果推送失败（内部错误），我们无法直接检测到
            // 但通常情况下，如果用户在线且订阅正常，推送会成功
            
        } catch (Exception e) {
            // 推送过程中出现异常（如序列化失败），记录到重试队列
            log.warn("WebSocket push failed, adding to retry queue: receiverId={}, messageType={}, error={}", 
                receiverId, messageType, e.getMessage());
            addToRetryQueue(receiverId, messageData, messageType);
        }
    }
    
    /**
     * 添加消息到重试队列
     */
    private void addToRetryQueue(String receiverId, Object messageData, String messageType) {
        try {
            RetryMessageDTO retryMsg = new RetryMessageDTO();
            retryMsg.setReceiverId(receiverId);
            retryMsg.setMessageData(objectMapper.writeValueAsString(messageData));
            retryMsg.setMessageType(messageType);
            retryMsg.setRetryCount(0);
            retryMsg.setCreateTime(LocalDateTime.now());
            retryMsg.calculateNextRetryTime();
            
            // 存储到Redis（使用Sorted Set，按nextRetryTime排序）
            String retryJson = objectMapper.writeValueAsString(retryMsg);
            // 使用nextRetryTime的epoch秒数作为score，便于按时间排序
            long score = retryMsg.getNextRetryTime().atZone(java.time.ZoneId.systemDefault()).toEpochSecond();
            
            redisTemplate.opsForZSet().add(RETRY_QUEUE_KEY, retryJson, score);
            // 设置过期时间（避免队列无限增长）
            redisTemplate.expire(RETRY_QUEUE_KEY, java.time.Duration.ofSeconds(MESSAGE_EXPIRE_SECONDS));
            
        } catch (Exception e) {
            log.error("Failed to add message to retry queue: receiverId={}, error={}", 
                receiverId, e.getMessage(), e);
        }
    }
    
    /**
     * 处理重试队列中的消息
     * 定时任务：每5秒执行一次
     */
    @Override
    @Scheduled(fixedRate = 5000) // 每5秒执行一次
    public void retryFailedMessages() {
        try {
            long currentTime = LocalDateTime.now().atZone(java.time.ZoneId.systemDefault()).toEpochSecond();
            
            // 查询所有应该重试的消息（score <= currentTime）
            Set<Object> messagesToRetry = redisTemplate.opsForZSet()
                .rangeByScore(RETRY_QUEUE_KEY, 0, currentTime);
            
            if (messagesToRetry == null || messagesToRetry.isEmpty()) {
                return;
            }
            
            log.debug("Found {} messages to retry", messagesToRetry.size());
            
            for (Object msgObj : messagesToRetry) {
                try {
                    String retryJson = msgObj.toString();
                    RetryMessageDTO retryMsg = objectMapper.readValue(retryJson, RetryMessageDTO.class);
                    
                    // 检查是否可以重试
                    if (!retryMsg.canRetry()) {
                        // 详细记录为什么不能重试
                        boolean hasAttempts = retryMsg.hasRetryAttempts();
                        boolean timeReached = retryMsg.isRetryTimeReached();
                        LocalDateTime now = java.time.LocalDateTime.now();
                        log.warn("Message cannot retry, removing: receiverId={}, retryCount={}/{}, messageType={}, " +
                                "hasAttempts={}, timeReached={}, now={}, nextRetryTime={}, createTime={}", 
                            retryMsg.getReceiverId(), retryMsg.getRetryCount(), retryMsg.getMaxRetries(),
                            retryMsg.getMessageType(), hasAttempts, timeReached, now, 
                            retryMsg.getNextRetryTime(), retryMsg.getCreateTime());
                        redisTemplate.opsForZSet().remove(RETRY_QUEUE_KEY, retryJson);
                        continue;
                    }
                    
                    // 检查用户是否在线
                    boolean isOnline = webSocketEventListener.isUserOnline(retryMsg.getReceiverId());
                    if (!isOnline) {
                        // 用户不在线，更新下次重试时间（指数退避）
                        int oldRetryCount = retryMsg.getRetryCount();
                        retryMsg.incrementRetry();
                        int newRetryCount = retryMsg.getRetryCount();
                        
                        log.info("Retry attempt (user offline): receiverId={}, retryCount={} -> {}, nextRetryTime={}, messageType={}", 
                            retryMsg.getReceiverId(), oldRetryCount, newRetryCount, retryMsg.getNextRetryTime(), retryMsg.getMessageType());
                        
                        // 更新Redis中的重试消息（先删除旧的，再添加新的）
                        redisTemplate.opsForZSet().remove(RETRY_QUEUE_KEY, retryJson);
                        
                        // 只检查是否还有重试次数（时间会在下次定时任务时检查）
                        if (retryMsg.hasRetryAttempts()) {
                            String updatedJson = objectMapper.writeValueAsString(retryMsg);
                            long newScore = retryMsg.getNextRetryTime()
                                .atZone(java.time.ZoneId.systemDefault()).toEpochSecond();
                            redisTemplate.opsForZSet().add(RETRY_QUEUE_KEY, updatedJson, newScore);
                            
                            log.info("Message rescheduled for retry: receiverId={}, retryCount={}/{}, nextRetryTime={}", 
                                retryMsg.getReceiverId(), newRetryCount, retryMsg.getMaxRetries(), retryMsg.getNextRetryTime());
                        } else {
                            // 超过最大重试次数
                            log.warn("Message exceeded max retries after reschedule: receiverId={}, finalRetryCount={}/{}, messageType={}", 
                                retryMsg.getReceiverId(), newRetryCount, retryMsg.getMaxRetries(), retryMsg.getMessageType());
                        }
                        continue;
                    }
                    
                    // 用户在线，尝试推送
                    Object messageData = objectMapper.readValue(retryMsg.getMessageData(), Object.class);
                    String destination = "/queue/message";
                    
                    messagingTemplate.convertAndSendToUser(
                        retryMsg.getReceiverId(),
                        destination,
                        messageData
                    );
                    
                    // 推送成功，从队列中移除
                    redisTemplate.opsForZSet().remove(RETRY_QUEUE_KEY, retryJson);
                    log.info("Retry push successful: receiverId={}, messageType={}, retryCount={}", 
                        retryMsg.getReceiverId(), retryMsg.getMessageType(), retryMsg.getRetryCount());
                    
                } catch (Exception e) {
                    log.error("Failed to retry message: error={}", e.getMessage(), e);
                    // 重试失败，继续处理下一条消息
                }
            }
            
        } catch (Exception e) {
            log.error("Error processing retry queue: error={}", e.getMessage(), e);
        }
    }
}

