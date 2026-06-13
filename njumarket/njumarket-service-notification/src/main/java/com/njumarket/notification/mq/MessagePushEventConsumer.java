package com.njumarket.notification.mq;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.njumarket.notification.service.NotificationService;
import com.njumarket.njumarket.dto.event.MessagePushEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 消息推送事件消费者（Notification 服务，每实例独立）。
 *
 * <p>架构说明（多实例扇出模型）：
 * <ul>
 *   <li>Message 服务将推送事件发布到 Topic Exchange（{@code message.push.exchange}）。</li>
 *   <li>每个 Notification 实例在启动时创建一个 exclusive + autoDelete 的 AnonymousQueue
 *       并绑定到该 Exchange，因此每条事件都会被 <em>所有在线实例</em> 各自收到一份（扇出）。</li>
 *   <li>各实例独立尝试 WebSocket 推送：若目标用户连接在本实例，推送成功；否则
 *       {@code convertAndSendToUser} 静默丢弃，无任何副作用。</li>
 *   <li>重试队列（{@code WebSocketRetryService}）只在用户本地在线时入队，避免跨实例干扰。</li>
 * </ul>
 *
 * <p>注意：此处 <strong>不再做 Redis 去重</strong>。原有的去重逻辑在扇出模式下是致命 Bug：
 * 第一个实例写入去重键后，其余所有实例都会跳过该消息，导致目标用户收不到推送。
 * RabbitMQ 层面的重复投递（redelivery）由 AUTO ack 机制保证不会发生。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MessagePushEventConsumer {

    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;

    @Qualifier("websocketPushTaskExecutor")
    private final ThreadPoolTaskExecutor websocketPushTaskExecutor;

    /**
     * 消费消息推送事件。
     * 使用 SpEL 引用 AnonymousQueue bean 的运行时队列名，确保每实例监听各自的专属队列。
     */
    @RabbitListener(queues = "#{instanceMessagePushQueue.name}",
                    containerFactory = "rabbitListenerContainerFactory")
    public void handleMessagePushEvent(MessagePushEvent event) {
        if (event == null) {
            log.warn("收到空的消息推送事件，跳过");
            return;
        }

        log.debug("收到消息推送事件: receiverId={}, messageId={}, messageType={}",
                event.getReceiverId(), event.getMessageId(), event.getMessageType());

        websocketPushTaskExecutor.execute(() -> {
            try {
                Object messageData = objectMapper.readValue(event.getMessageData(), Object.class);
                String messageType = event.getMessageType();
                String receiverId = event.getReceiverId();
                String messageId = event.getMessageId();

                if ("MESSAGE_NEW".equals(messageType)) {
                    notificationService.pushMessage(receiverId, messageData);
                } else if ("UNREAD_COUNT_UPDATE".equals(messageType)) {
                    if (messageData instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> dataMap = (Map<String, Object>) messageData;
                        Object unreadCountObj = dataMap.get("unreadCount");
                        if (unreadCountObj instanceof Number) {
                            notificationService.pushUnreadCountUpdate(receiverId, ((Number) unreadCountObj).intValue());
                        } else {
                            log.warn("未读数更新消息格式错误: messageId={}, receiverId={}, unreadCount={}",
                                    messageId, receiverId, unreadCountObj);
                        }
                    }
                } else {
                    notificationService.pushGenericMessage(receiverId, messageData, messageType);
                }

                log.debug("消息推送任务执行完毕: messageId={}, receiverId={}, messageType={}",
                        messageId, receiverId, messageType);
            } catch (Exception e) {
                log.error("执行消息推送任务失败: messageId={}, receiverId={}, messageType={}, error={}",
                        event.getMessageId(), event.getReceiverId(), event.getMessageType(), e.getMessage(), e);
            }
        });
    }
}
