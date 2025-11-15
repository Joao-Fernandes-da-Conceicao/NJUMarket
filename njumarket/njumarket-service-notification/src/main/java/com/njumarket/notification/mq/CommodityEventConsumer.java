package com.njumarket.notification.mq;

import com.njumarket.njumarket.dto.event.CommodityEvent;
import com.njumarket.notification.config.RabbitMQConfig;
import com.njumarket.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * 商品事件消息消费者
 * 接收商品变更事件并处理通知
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CommodityEventConsumer {

    private final NotificationService notificationService;

    /**
     * 消费商品事件消息
     */
    @RabbitListener(queues = RabbitMQConfig.COMMODITY_QUEUE)
    public void handleCommodityEvent(CommodityEvent event) {
        try {
            log.info("收到商品事件: userId={}, commodityId={}, operation={}", 
                    event.getUserId(), event.getCommodityId(), event.getOperation());
            
            // 推送商品变更通知
            notificationService.pushCommodityChange(
                    event.getUserId(), 
                    event.getCommodityId(), 
                    event.getOperation()
            );
            
            log.info("商品事件处理完成: userId={}, commodityId={}, operation={}", 
                    event.getUserId(), event.getCommodityId(), event.getOperation());
        } catch (Exception e) {
            log.error("处理商品事件失败: userId={}, commodityId={}, operation={}, error={}", 
                    event.getUserId(), event.getCommodityId(), event.getOperation(), e.getMessage(), e);
            // 注意：这里不抛出异常，避免消息重复消费
        }
    }
}

