package com.njumarket.notification.mq;

import com.njumarket.njumarket.dto.event.OrderEvent;
import com.njumarket.notification.config.RabbitMQConfig;
import com.njumarket.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * 订单事件消息消费者
 *
 * 当订单状态变更时，通过 WebSocket 将变更推送给「修改方的对方」。
 * OrderServiceImpl 在调用 orderEventProducer.sendOrderEvent(userId, ...) 时，
 * 传入的 userId 已经是需要被通知的另一方（买家或卖家），
 * 因此此处直接将事件推送给 event.getUserId() 即可。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventConsumer {

    private final NotificationService notificationService;

    @RabbitListener(queues = RabbitMQConfig.ORDER_QUEUE, containerFactory = "rabbitListenerContainerFactory")
    public void handleOrderEvent(OrderEvent event) {
        try {
            log.info("收到订单事件: userId={}, orderId={}, operation={}",
                    event.getUserId(), event.getOrderId(), event.getOperation());

            notificationService.pushOrderChange(
                    event.getUserId(),
                    event.getOrderId(),
                    event.getOperation()
            );

            log.info("订单事件推送完成: userId={}, orderId={}, operation={}",
                    event.getUserId(), event.getOrderId(), event.getOperation());
        } catch (Exception e) {
            log.error("处理订单事件失败: userId={}, orderId={}, operation={}, error={}",
                    event.getUserId(), event.getOrderId(), event.getOperation(), e.getMessage(), e);
        }
    }
}
