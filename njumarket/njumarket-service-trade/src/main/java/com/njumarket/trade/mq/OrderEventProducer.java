package com.njumarket.trade.mq;

import com.njumarket.njumarket.dto.event.OrderEvent;
import com.njumarket.trade.config.RabbitMQConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 订单事件消息生产者
 * 用于发送订单变更事件到消息队列
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventProducer {

    private final RabbitTemplate rabbitTemplate;

    /**
     * 发送订单事件
     *
     * @param userId        接收通知的用户 ID
     * @param orderId       订单 ID
     * @param operation     操作类型（ORDER_CREATED, ORDER_PAID 等）
     * @param recipientRole 接收方在订单中的角色："BUYER" 或 "SELLER"
     */
    public void sendOrderEvent(String userId, String orderId, String operation, String recipientRole) {
        try {
            OrderEvent event = new OrderEvent(userId, orderId, operation, recipientRole, LocalDateTime.now());
            
            // 根据操作类型确定路由键
            String routingKey = "order." + operation.toLowerCase().replace("_", ".");
            
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.ORDER_EXCHANGE,
                    routingKey,
                    event
            );
            
            log.info("订单事件已发送: userId={}, orderId={}, operation={}, recipientRole={}, exchange={}, routingKey={}",
                    userId, orderId, operation, recipientRole, RabbitMQConfig.ORDER_EXCHANGE, routingKey);
        } catch (Exception e) {
            log.error("发送订单事件失败: userId={}, orderId={}, operation={}, error={}",
                    userId, orderId, operation, e.getMessage(), e);
        }
    }
}

