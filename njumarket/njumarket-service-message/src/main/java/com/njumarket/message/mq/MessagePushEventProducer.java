package com.njumarket.message.mq;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.njumarket.message.config.RabbitMQConfig;
import com.njumarket.njumarket.dto.event.MessagePushEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * 消息推送事件生产者
 * 用于异步推送消息（解决刷新后重复推送的问题）
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MessagePushEventProducer {

    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    /**
     * 发送消息推送事件
     * 
     * @param receiverId 接收者ID
     * @param messageId 消息ID（用于去重）
     * @param messageData 消息数据
     * @param messageType 消息类型（MESSAGE_NEW, UNREAD_COUNT_UPDATE 等）
     */
    public void sendMessagePushEvent(String receiverId, String messageId, Object messageData, String messageType) {
        try {
            // 将消息数据序列化为 JSON 字符串
            String messageDataJson = objectMapper.writeValueAsString(messageData);
            
            MessagePushEvent event = new MessagePushEvent(receiverId, messageId, messageDataJson, messageType);
            
            // 根据消息类型确定路由键
            String routingKey = "message.push." + messageType.toLowerCase().replace("_", ".");
            
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.MESSAGE_PUSH_EXCHANGE,
                    routingKey,
                    event
            );
            
            log.debug("消息推送事件已发送: receiverId={}, messageId={}, messageType={}, exchange={}, routingKey={}", 
                    receiverId, messageId, messageType, RabbitMQConfig.MESSAGE_PUSH_EXCHANGE, routingKey);
        } catch (Exception e) {
            log.error("发送消息推送事件失败: receiverId={}, messageId={}, messageType={}, error={}", 
                    receiverId, messageId, messageType, e.getMessage(), e);
        }
    }
}

