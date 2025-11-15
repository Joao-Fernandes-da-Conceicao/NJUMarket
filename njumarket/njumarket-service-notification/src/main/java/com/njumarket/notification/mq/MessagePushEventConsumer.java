package com.njumarket.notification.mq;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.njumarket.notification.config.RabbitMQConfig;
import com.njumarket.notification.service.NotificationService;
import com.njumarket.njumarket.dto.event.MessagePushEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * 消息推送事件消费者（Notification服务）
 * 接收Message服务发送的推送任务，统一通过Notification服务的WebSocket实例推送
 * 
 * 架构说明：
 * - Message服务只负责消息和聊天的相关操作，通过MQ发送推送任务
 * - Notification服务统一负责所有推送（订单、消息等），使用一个WebSocket实例
 * - MQ只负责"通知有消息需要推送"，推送和重试机制由WebSocketRetryService独立完成
 */
@Slf4j
@Component
public class MessagePushEventConsumer {

    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;
    private final StringRedisTemplate redisTemplate;
    private final ThreadPoolTaskExecutor websocketPushTaskExecutor; // WebSocket推送任务执行器
    
    public MessagePushEventConsumer(
            NotificationService notificationService,
            ObjectMapper objectMapper,
            StringRedisTemplate redisTemplate,
            @Qualifier("websocketPushTaskExecutor") ThreadPoolTaskExecutor websocketPushTaskExecutor) {
        this.notificationService = notificationService;
        this.objectMapper = objectMapper;
        this.redisTemplate = redisTemplate;
        this.websocketPushTaskExecutor = websocketPushTaskExecutor;
    }
    
    /**
     * Redis Key 前缀：用于存储已处理的消息推送事件ID（防止RabbitMQ消息重复消费）
     * 注意：这个key与ACK机制中的 message:pushed: 不同，用于不同目的
     */
    private static final String PROCESSED_EVENT_KEY_PREFIX = "message:event:processed:";
    
    /**
     * 事件处理标记过期时间（24小时）
     */
    private static final Duration PROCESSED_EVENT_TTL = Duration.ofHours(24);

    /**
     * 消费消息推送事件
     * 
     * 关键设计：
     * 1. MQ只负责"通知有消息需要推送"，不参与推送重试
     * 2. 从MQ消费后，将推送任务提交到正确的线程池执行（有WebSocket上下文）
     * 3. 推送和重试机制由WebSocketRetryService独立完成
     */
    @RabbitListener(queues = RabbitMQConfig.MESSAGE_PUSH_QUEUE, containerFactory = "rabbitListenerContainerFactory")
    public void handleMessagePushEvent(MessagePushEvent event) {
        try {
            // ✅ 去重检查：如果事件已经处理过（防止RabbitMQ消息重复消费），则跳过
            // 注意：这个检查与ACK机制无关，只是防止RabbitMQ消息重复消费
            String processedKey = PROCESSED_EVENT_KEY_PREFIX + event.getMessageId();
            Boolean alreadyProcessed = redisTemplate.hasKey(processedKey);
            
            if (Boolean.TRUE.equals(alreadyProcessed)) {
                log.debug("消息推送事件已处理过，跳过（防止重复消费）: messageId={}, receiverId={}, messageType={}", 
                        event.getMessageId(), event.getReceiverId(), event.getMessageType());
                return;
            }
            
            // ✅ 标记事件已处理（防止RabbitMQ消息重复消费）
            // 注意：这个标记与ACK机制中的 message:pushed: 不同
            // message:pushed: 只在收到ACK时设置，表示消息已真正送达前端
            redisTemplate.opsForValue().set(processedKey, "1", PROCESSED_EVENT_TTL);
            
            // ✅ 关键修复：将推送任务提交到正确的线程池执行（有WebSocket上下文）
            // 原因：RabbitMQ消费者线程缺少WebSocket上下文，导致convertAndSendToUser无法找到用户会话
            // 解决方案：使用ThreadPoolTaskExecutor将推送任务提交到Spring管理的线程池，有正确的上下文
            websocketPushTaskExecutor.execute(() -> {
                try {
                    // 反序列化消息数据
                    Object messageData = objectMapper.readValue(event.getMessageData(), Object.class);
                    
                    // ✅ 根据消息类型调用不同的推送方法
                    String messageType = event.getMessageType();
                    String receiverId = event.getReceiverId();
                    String messageId = event.getMessageId();
                    
                    if ("MESSAGE_NEW".equals(messageType)) {
                        // 推送新消息
                        notificationService.pushMessage(receiverId, messageData);
                    } else if ("UNREAD_COUNT_UPDATE".equals(messageType)) {
                        // 推送未读数更新
                        if (messageData instanceof java.util.Map) {
                            @SuppressWarnings("unchecked")
                            java.util.Map<String, Object> dataMap = (java.util.Map<String, Object>) messageData;
                            Object unreadCountObj = dataMap.get("unreadCount");
                            if (unreadCountObj instanceof Number) {
                                notificationService.pushUnreadCountUpdate(receiverId, ((Number) unreadCountObj).intValue());
                            } else {
                                log.warn("未读数更新消息格式错误: messageId={}, receiverId={}, unreadCount={}", 
                                        messageId, receiverId, unreadCountObj);
                            }
                        }
                    } else {
                        // 其他类型（CONVERSATION_RESTORED、MESSAGE_READ等）使用通用推送
                        notificationService.pushGenericMessage(receiverId, messageData, messageType);
                    }
                    
                    log.debug("消息推送任务已提交: messageId={}, receiverId={}, messageType={}", 
                            event.getMessageId(), event.getReceiverId(), event.getMessageType());
                } catch (Exception e) {
                    log.error("执行消息推送任务失败: messageId={}, receiverId={}, messageType={}, error={}", 
                            event.getMessageId(), event.getReceiverId(), event.getMessageType(), e.getMessage(), e);
                }
            });
            
            log.debug("消息推送事件处理完成（已提交推送任务）: messageId={}, receiverId={}, messageType={}", 
                    event.getMessageId(), event.getReceiverId(), event.getMessageType());
        } catch (Exception e) {
            log.error("处理消息推送事件失败: messageId={}, receiverId={}, messageType={}, error={}", 
                    event.getMessageId(), event.getReceiverId(), event.getMessageType(), e.getMessage(), e);
            // 注意：这里不抛出异常，避免消息重复消费
        }
    }
}

