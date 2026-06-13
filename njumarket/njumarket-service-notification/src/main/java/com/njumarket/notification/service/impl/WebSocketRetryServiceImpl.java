package com.njumarket.notification.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.njumarket.notification.dto.RetryMessageDTO;
import com.njumarket.notification.service.WebSocketRetryService;
import com.njumarket.notification.websocket.WebSocketEventListener;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;

/**
 * WebSocket 消息推送重试服务实现（多实例安全版）。
 *
 * <p>设计思路（扇出模型下的重试机制）：
 * <ol>
 *   <li>每个 Notification 实例使用 <strong>实例专属的 Redis Sorted Set</strong> 作为重试队列，
 *       键名格式为 {@code websocket:retry:queue:<instanceId>}，实例间互不干扰。</li>
 *   <li>仅当目标用户 <strong>连接在本实例</strong> 时才入队：
 *       若用户不在本地，{@code convertAndSendToUser} 已静默丢弃，无需重试；
 *       若用户在本地，入队后通过 ACK 确认最终投递。</li>
 *   <li>重试策略：指数退避（5s / 10s / 20s），最多 3 次；超时后依赖客户端重连拉取未读消息。</li>
 * </ol>
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

    /**
     * 实例唯一标识（JVM 生命周期内固定），用于隔离各实例的重试队列。
     */
    private static final String INSTANCE_ID =
            java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 12);

    // 实例专属 Redis 重试队列键
    private static final String RETRY_QUEUE_KEY =
            com.njumarket.njumarket.utils.RedisConstants.WEBSOCKET_RETRY_QUEUE_KEY + ":" + INSTANCE_ID;

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
            String destination = getDestinationByMessageType(messageType);

            // 确保 messageData 中携带 messageId，供前端发送 ACK
            if (messageId != null && !messageId.trim().isEmpty() && messageData instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> dataMap = (Map<String, Object>) messageData;
                dataMap.putIfAbsent("messageId", messageId);
            }

            // 尝试推送；若用户未连接到本实例，convertAndSendToUser 静默丢弃，不抛出异常
            messagingTemplate.convertAndSendToUser(receiverId, destination, messageData);
            log.debug("WebSocket 推送已尝试: receiverId={}, messageType={}, messageId={}", receiverId, messageType, messageId);

            // 仅当用户连接在本实例时才入重试队列：
            //   - 用户在本地 → 推送已成功发出，入队等待 ACK 确认最终投递
            //   - 用户不在本地 → 推送已被静默丢弃，由持有用户连接的实例负责推送和重试；
            //     若此刻所有实例均无该用户连接（用户离线），客户端重连后通过 REST 拉取未读消息
            if (webSocketEventListener.isUserOnline(receiverId)) {
                log.debug("用户在本实例在线，加入重试队列等待 ACK: receiverId={}, messageType={}, messageId={}",
                        receiverId, messageType, messageId);
                addToRetryQueue(receiverId, messageData, messageType, messageId);
            } else {
                log.debug("用户未连接到本实例，跳过重试队列: receiverId={}, messageType={}", receiverId, messageType);
            }

        } catch (Exception e) {
            log.error("WebSocket 推送异常，尝试加入重试队列: receiverId={}, messageType={}, messageId={}, error={}",
                    receiverId, messageType, messageId, e.getMessage(), e);
            // 异常时同样只在本地在线的情况下入队（异常推送通常意味着序列化等本地问题）
            if (webSocketEventListener.isUserOnline(receiverId)) {
                addToRetryQueue(receiverId, messageData, messageType, messageId);
            }
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
     * 监听 WebSocket 会话断开事件，立即清理该用户在本实例重试队列中的全部条目。
     *
     * <p>不做此清理时，断开用户的条目要经历完整的指数退避周期（最长约 5 分钟）才被
     * 自然淘汰，期间产生大量无效的 Redis 读写和在线状态检查。
     */
    @EventListener
    public void handleSessionDisconnect(SessionDisconnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String userId = accessor.getUser() != null ? accessor.getUser().getName() : null;
        if (userId == null || userId.isBlank()) {
            return;
        }

        Set<Object> allEntries = redisTemplate.opsForZSet().range(RETRY_QUEUE_KEY, 0, -1);
        if (allEntries == null || allEntries.isEmpty()) {
            return;
        }

        int removed = 0;
        for (Object entry : allEntries) {
            try {
                RetryMessageDTO msg = objectMapper.readValue(entry.toString(), RetryMessageDTO.class);
                if (userId.equals(msg.getReceiverId())) {
                    redisTemplate.opsForZSet().remove(RETRY_QUEUE_KEY, entry);
                    removed++;
                }
            } catch (Exception e) {
                log.warn("用户下线清理：解析重试队列条目失败，跳过: {}", e.getMessage());
            }
        }

        if (removed > 0) {
            log.info("用户下线，已从重试队列清理 {} 条记录: userId={}", removed, userId);
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

