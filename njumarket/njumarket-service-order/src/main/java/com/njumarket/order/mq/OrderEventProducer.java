package com.njumarket.order.mq;

import com.njumarket.njumarket.dto.event.OrderEvent;
import com.njumarket.order.config.RabbitMQConfig;
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
     * @param userId 用户ID（接收通知的用户）
     * @param orderId 订单ID
     * @param operation 操作类型（ORDER_CREATED, ORDER_PAID 等）
     */
    public void sendOrderEvent(String userId, String orderId, String operation) {
        try {
            OrderEvent event = new OrderEvent(userId, orderId, operation, LocalDateTime.now());
            
            // 根据操作类型确定路由键
            String routingKey = "order." + operation.toLowerCase().replace("_", ".");
            
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.ORDER_EXCHANGE,
                    routingKey,
                    event
            );
            
            log.info("订单事件已发送: userId={}, orderId={}, operation={}, exchange={}, routingKey={}", 
                    userId, orderId, operation, RabbitMQConfig.ORDER_EXCHANGE, routingKey);
        } catch (Exception e) {
            log.error("发送订单事件失败: userId={}, orderId={}, operation={}, error={}", 
                    userId, orderId, operation, e.getMessage(), e);
        }
    }
}

