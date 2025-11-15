package com.njumarket.notification.mq;

import com.njumarket.njumarket.dto.event.OrderEvent;
import com.njumarket.notification.config.RabbitMQConfig;
import com.njumarket.notification.service.NotificationService;
import com.njumarket.notification.service.ChangeRecordService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * 订单事件消息消费者
 * 接收订单变更事件并处理通知
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventConsumer {

    private final NotificationService notificationService;
    private final ChangeRecordService changeRecordService;

    /**
     * 初始化检查
     */
    @PostConstruct
    public void init() {
        log.info("✅ OrderEventConsumer 已初始化，准备监听队列: {}", RabbitMQConfig.ORDER_QUEUE);
    }

    /**
     * 消费订单事件消息
     */
    @RabbitListener(queues = RabbitMQConfig.ORDER_QUEUE, containerFactory = "rabbitListenerContainerFactory")
    public void handleOrderEvent(OrderEvent event) {
        try {
            log.info("✅ 收到订单事件: userId={}, orderId={}, operation={}, timestamp={}", 
                    event.getUserId(), event.getOrderId(), event.getOperation(), event.getTimestamp());
            
            // 记录订单变更（将操作类型映射到记录操作）
            String recordOperation = mapOperationToRecordOperation(event.getOperation());
            changeRecordService.recordOrderChange(
                    event.getOrderId(), 
                    recordOperation, 
                    event.getTimestamp()
            );
            
            // 推送订单变更通知
            notificationService.pushOrderChange(
                    event.getUserId(), 
                    event.getOrderId(), 
                    event.getOperation()
            );
            
            log.info("订单事件处理完成: userId={}, orderId={}, operation={}", 
                    event.getUserId(), event.getOrderId(), event.getOperation());
        } catch (Exception e) {
            log.error("处理订单事件失败: userId={}, orderId={}, operation={}, error={}", 
                    event.getUserId(), event.getOrderId(), event.getOperation(), e.getMessage(), e);
            // 注意：这里不抛出异常，避免消息重复消费
            // 如果需要重试，可以配置死信队列
        }
    }
    
    /**
     * 将事件操作类型映射到记录操作类型
     */
    private String mapOperationToRecordOperation(String operation) {
        switch (operation) {
            case "ORDER_CREATED":
                return "CREATE";
            case "ORDER_PAID":
                return "PAY";
            case "ORDER_SHIPPED":
                return "SHIP";
            case "ORDER_COMPLETED":
                return "COMPLETE";
            case "ORDER_CANCELLED":
                return "CANCEL";
            case "REFUND_REQUESTED":
                return "REFUND_REQUEST";
            case "REFUND_APPROVED":
                return "REFUND_APPROVE";
            case "REFUND_REJECTED":
                return "REFUND_REJECT";
            default:
                return operation; // 默认返回原操作类型
        }
    }
}

