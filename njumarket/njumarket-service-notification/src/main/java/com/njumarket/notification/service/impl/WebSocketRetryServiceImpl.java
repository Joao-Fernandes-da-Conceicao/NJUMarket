package com.njumarket.notification.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.njumarket.notification.dto.RetryMessageDTO;
import com.njumarket.notification.service.WebSocketRetryService;
import com.njumarket.notification.websocket.WebSocketEventListener;
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
                log.debug("用户不在线，加入重试队列: receiverId={}, messageType={}", 
                    receiverId, messageType);
                addToRetryQueue(receiverId, messageData, messageType);
                return;
            }
            
            // 用户在线，尝试推送
            // 根据消息类型选择不同的队列
            String destination = getDestinationByMessageType(messageType);
            
            messagingTemplate.convertAndSendToUser(receiverId, destination, messageData);
            
            log.debug("WebSocket推送成功: receiverId={}, messageType={}", receiverId, messageType);
            
        } catch (Exception e) {
            // 推送过程中出现异常（如序列化失败），记录到重试队列
            log.error("❌ WebSocket推送失败，加入重试队列: receiverId={}, messageType={}, error={}, errorType={}", 
                receiverId, messageType, e.getMessage(), e.getClass().getName(), e);
            addToRetryQueue(receiverId, messageData, messageType);
        }
    }
    
    /**
     * 根据消息类型获取目标队列
     */
    private String getDestinationByMessageType(String messageType) {
        // 统一使用 /queue/message，与前端订阅保持一致
        // 前端订阅：/user/queue/message
        // 后端推送：/user/{userId}/queue/message（通过 convertAndSendToUser 自动转换）
        return "/queue/message";
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
                String retryJson = null;
                RetryMessageDTO retryMsg = null;
                try {
                    retryJson = msgObj.toString();
                    retryMsg = objectMapper.readValue(retryJson, RetryMessageDTO.class);
                    
                    // 检查是否可以重试
                    if (!retryMsg.canRetry()) {
                        log.warn("Message cannot retry, removing: receiverId={}, retryCount={}/{}, messageType={}", 
                            retryMsg.getReceiverId(), retryMsg.getRetryCount(), retryMsg.getMaxRetries(),
                            retryMsg.getMessageType());
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
                    String destination = getDestinationByMessageType(retryMsg.getMessageType());
                    
                    messagingTemplate.convertAndSendToUser(
                        retryMsg.getReceiverId(),
                        destination,
                        messageData
                    );
                    
                    // 推送成功，从队列中移除
                    redisTemplate.opsForZSet().remove(RETRY_QUEUE_KEY, retryJson);
                    log.debug("重试推送成功: receiverId={}, messageType={}, retryCount={}/{}", 
                        retryMsg.getReceiverId(), retryMsg.getMessageType(), retryMsg.getRetryCount(), retryMsg.getMaxRetries());
                    
                } catch (Exception e) {
                    log.error("❌ 重试推送失败: receiverId={}, messageType={}, retryCount={}/{}, error={}, errorType={}", 
                        retryMsg != null ? retryMsg.getReceiverId() : "unknown",
                        retryMsg != null ? retryMsg.getMessageType() : "unknown",
                        retryMsg != null ? retryMsg.getRetryCount() : 0,
                        retryMsg != null ? retryMsg.getMaxRetries() : 0,
                        e.getMessage(), e.getClass().getName(), e);
                    
                    // 重试失败，如果还有重试次数，更新重试时间；否则从队列中移除
                    if (retryMsg != null && retryMsg.hasRetryAttempts()) {
                        try {
                            retryMsg.incrementRetry();
                            String updatedJson = objectMapper.writeValueAsString(retryMsg);
                            long newScore = retryMsg.getNextRetryTime()
                                .atZone(java.time.ZoneId.systemDefault()).toEpochSecond();
                            redisTemplate.opsForZSet().remove(RETRY_QUEUE_KEY, retryJson);
                            redisTemplate.opsForZSet().add(RETRY_QUEUE_KEY, updatedJson, newScore);
                            log.info("重试失败，已更新重试时间: receiverId={}, nextRetryTime={}", 
                                retryMsg.getReceiverId(), retryMsg.getNextRetryTime());
                        } catch (Exception ex) {
                            log.error("更新重试时间失败: receiverId={}, error={}", 
                                retryMsg.getReceiverId(), ex.getMessage(), ex);
                        }
                    } else if (retryMsg != null) {
                        // 超过最大重试次数，从队列中移除
                        redisTemplate.opsForZSet().remove(RETRY_QUEUE_KEY, retryJson);
                        log.warn("超过最大重试次数，已从队列移除: receiverId={}, messageType={}", 
                            retryMsg.getReceiverId(), retryMsg.getMessageType());
                    }
                }
            }
            
        } catch (Exception e) {
            log.error("Error processing retry queue: error={}", e.getMessage(), e);
        }
    }
}

