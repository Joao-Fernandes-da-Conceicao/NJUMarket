package com.njumarket.notification.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.njumarket.notification.dto.RetryMessageDTO;
import com.njumarket.notification.service.WebSocketRetryService;
import com.njumarket.notification.websocket.WebSocketEventListener;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
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
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;
    
    // Redis重试队列key
    private static final String RETRY_QUEUE_KEY = com.njumarket.njumarket.utils.RedisConstants.WEBSOCKET_RETRY_QUEUE_KEY;
    
    // 消息过期时间（30分钟）
    private static final long MESSAGE_EXPIRE_SECONDS = com.njumarket.njumarket.utils.RedisConstants.WEBSOCKET_RETRY_TTL;
    
    /**
     * Redis Key 前缀：用于存储已推送的消息ID（去重）
     */
    private static final String PUSHED_MESSAGE_KEY_PREFIX = "message:pushed:";
    
    /**
     * 消息去重过期时间（24小时）
     */
    private static final java.time.Duration PUSHED_MESSAGE_TTL = java.time.Duration.ofHours(24);
    
    @Override
    public void pushWithRetry(String receiverId, Object messageData, String messageType, String messageId) {
        try {
            // ✅ 优化：先尝试推送，再检查在线状态
            // 原因：SimpUserRegistry可能更新延迟，导致误判用户不在线
            // convertAndSendToUser是异步的，即使推送失败也不会抛出异常
            // 所以即使在线检查返回false，也尝试推送一次（Spring会自动处理离线用户）
            
            // 根据消息类型选择不同的队列
            String destination = getDestinationByMessageType(messageType);
            
            // ✅ 确保messageData中包含messageId（如果messageId不为null）
            // 这样前端才能提取messageId并发送ACK
            if (messageId != null && !messageId.trim().isEmpty()) {
                if (messageData instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> dataMap = (Map<String, Object>) messageData;
                    // 如果messageData中没有messageId，则添加
                    if (!dataMap.containsKey("messageId")) {
                        dataMap.put("messageId", messageId);
                    }
                }
            }
            
            // ✅ 先尝试推送（不阻塞）
            // 注意：推送后不设置"已推送"标记，只有收到ACK才设置
            messagingTemplate.convertAndSendToUser(receiverId, destination, messageData);
            log.debug("WebSocket推送尝试: receiverId={}, messageType={}, messageId={}", receiverId, messageType, messageId);
            
            // ✅ 为了确保消息不丢失，总是加入重试队列
            // 重试机制基于ACK确认：只有收到ACK才认为消息真正送达
            // 如果用户在线，第一次推送会成功，前端会发送ACK，ACK处理时会从队列移除
            // 如果用户离线，第一次推送会被丢弃，重试时会继续等待用户上线
            // 如果推送成功但前端未收到（网络问题），重试时会继续推送直到收到ACK
            log.debug("消息已加入重试队列（等待ACK确认）: receiverId={}, messageType={}, messageId={}", 
                receiverId, messageType, messageId);
            addToRetryQueue(receiverId, messageData, messageType, messageId);
            
            // 注意：Spring的convertAndSendToUser是异步的，不会抛出异常
            // 如果推送失败（内部错误），我们无法直接检测到
            // 但通常情况下，如果用户在线且订阅正常，推送会成功
            // 如果用户不在线，Spring会自动丢弃消息，不会报错
            
        } catch (Exception e) {
            // 推送过程中出现异常（如序列化失败），记录到重试队列
            log.error("❌ WebSocket推送失败（异常），加入重试队列: receiverId={}, messageType={}, messageId={}, error={}, errorType={}", 
                receiverId, messageType, messageId, e.getMessage(), e.getClass().getName(), e);
            addToRetryQueue(receiverId, messageData, messageType, messageId);
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
    private void addToRetryQueue(String receiverId, Object messageData, String messageType, String messageId) {
        try {
            // ✅ 如果没有 messageId，生成一个唯一ID
            String finalMessageId = messageId;
            if (finalMessageId == null || finalMessageId.trim().isEmpty()) {
                finalMessageId = receiverId + "_" + messageType + "_" + System.currentTimeMillis();
            }
            
            RetryMessageDTO retryMsg = new RetryMessageDTO();
            retryMsg.setReceiverId(receiverId);
            retryMsg.setMessageId(finalMessageId);
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
                    
                    // ✅ ACK检查：如果消息已收到ACK确认，则跳过重试
                    // 只有收到ACK才认为消息真正送达，推送成功不代表前端收到
                    if (retryMsg.getMessageId() != null && !retryMsg.getMessageId().trim().isEmpty()) {
                        String pushedKey = PUSHED_MESSAGE_KEY_PREFIX + retryMsg.getMessageId();
                        Boolean alreadyAcked = stringRedisTemplate.hasKey(pushedKey);
                        
                        if (Boolean.TRUE.equals(alreadyAcked)) {
                            // 消息已收到ACK确认，从队列中移除，不再重试
                            redisTemplate.opsForZSet().remove(RETRY_QUEUE_KEY, retryJson);
                            log.info("✅ 重试时发现消息已收到ACK确认，跳过: receiverId={}, messageType={}, messageId={}, retryCount={}", 
                                retryMsg.getReceiverId(), retryMsg.getMessageType(), retryMsg.getMessageId(), retryMsg.getRetryCount());
                            continue;
                        }
                    }
                    
                    // 用户在线，尝试推送
                    Object messageData = objectMapper.readValue(retryMsg.getMessageData(), Object.class);
                    String destination = getDestinationByMessageType(retryMsg.getMessageType());
                    
                    // ✅ 推送消息（注意：推送后不设置"已推送"标记，只有收到ACK才设置）
                    messagingTemplate.convertAndSendToUser(
                        retryMsg.getReceiverId(),
                        destination,
                        messageData
                    );
                    
                    // ✅ 增加重试次数，更新下次重试时间
                    int oldRetryCount = retryMsg.getRetryCount();
                    retryMsg.incrementRetry();
                    int newRetryCount = retryMsg.getRetryCount();
                    
                    log.info("🔄 重试推送: receiverId={}, messageType={}, messageId={}, retryCount={} -> {}, nextRetryTime={}", 
                        retryMsg.getReceiverId(), retryMsg.getMessageType(), retryMsg.getMessageId(), oldRetryCount, newRetryCount, retryMsg.getNextRetryTime());
                    
                    // ✅ 检查是否还有重试次数
                    if (retryMsg.hasRetryAttempts()) {
                        // 还有重试次数，更新队列中的消息（更新重试次数和下次重试时间）
                        // 注意：消息继续留在队列中，等待ACK确认或下次重试
                        redisTemplate.opsForZSet().remove(RETRY_QUEUE_KEY, retryJson);
                        String updatedJson = objectMapper.writeValueAsString(retryMsg);
                        long newScore = retryMsg.getNextRetryTime()
                            .atZone(java.time.ZoneId.systemDefault()).toEpochSecond();
                        redisTemplate.opsForZSet().add(RETRY_QUEUE_KEY, updatedJson, newScore);
                        log.info("📝 消息已更新，等待ACK确认或下次重试: receiverId={}, retryCount={}/{}, nextRetryTime={}", 
                            retryMsg.getReceiverId(), newRetryCount, retryMsg.getMaxRetries(), retryMsg.getNextRetryTime());
                    } else {
                        // 达到最大重试次数，从队列中移除
                        redisTemplate.opsForZSet().remove(RETRY_QUEUE_KEY, retryJson);
                        log.warn("⚠️ 消息达到最大重试次数，已从队列移除: receiverId={}, messageType={}, messageId={}, finalRetryCount={}/{}", 
                            retryMsg.getReceiverId(), retryMsg.getMessageType(), retryMsg.getMessageId(), newRetryCount, retryMsg.getMaxRetries());
                    }
                    
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
    
    /**
     * 处理ACK确认
     * 收到前端ACK后，从重试队列中移除对应的消息
     */
    @Override
    public void handleAck(String userId, String messageId, String messageType) {
        try {
            log.info("处理ACK确认: userId={}, messageId={}, messageType={}", userId, messageId, messageType);
            
            // ✅ 标记消息已确认（用于去重，避免重复推送）
            String pushedKey = PUSHED_MESSAGE_KEY_PREFIX + messageId;
            stringRedisTemplate.opsForValue().set(pushedKey, "1", PUSHED_MESSAGE_TTL);
            
            // ✅ 从重试队列中查找并移除对应的消息
            Set<Object> allMessages = redisTemplate.opsForZSet().range(RETRY_QUEUE_KEY, 0, -1);
            
            if (allMessages == null || allMessages.isEmpty()) {
                log.debug("重试队列为空，无需处理ACK: userId={}, messageId={}", userId, messageId);
                return;
            }
            
            boolean found = false;
            for (Object msgObj : allMessages) {
                try {
                    String retryJson = msgObj.toString();
                    RetryMessageDTO retryMsg = objectMapper.readValue(retryJson, RetryMessageDTO.class);
                    
                    // 匹配消息：userId、messageId、messageType都匹配
                    if (userId.equals(retryMsg.getReceiverId()) && 
                        messageId.equals(retryMsg.getMessageId()) &&
                        (messageType == null || messageType.equals(retryMsg.getMessageType()))) {
                        
                        // 从重试队列中移除
                        redisTemplate.opsForZSet().remove(RETRY_QUEUE_KEY, retryJson);
                        found = true;
                        
                        log.info("✅ ACK确认成功，已从重试队列移除: userId={}, messageId={}, messageType={}, retryCount={}", 
                            userId, messageId, messageType, retryMsg.getRetryCount());
                        break;
                    }
                } catch (Exception e) {
                    log.warn("解析重试队列消息失败，跳过: error={}", e.getMessage());
                    continue;
                }
            }
            
            if (!found) {
                log.debug("ACK确认：未在重试队列中找到对应消息（可能已处理或未加入队列）: userId={}, messageId={}, messageType={}", 
                    userId, messageId, messageType);
            }
            
        } catch (Exception e) {
            log.error("处理ACK确认失败: userId={}, messageId={}, messageType={}, error={}", 
                userId, messageId, messageType, e.getMessage(), e);
        }
    }
}

