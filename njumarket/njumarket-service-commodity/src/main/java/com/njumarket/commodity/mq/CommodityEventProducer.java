package com.njumarket.commodity.mq;

import com.njumarket.commodity.config.RabbitMQConfig;
import com.njumarket.njumarket.dto.event.CommodityEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 商品事件消息生产者
 * 用于发送商品变更事件到消息队列
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CommodityEventProducer {

    private final RabbitTemplate rabbitTemplate;

    /**
     * 发送商品事件
     * 
     * @param userId 用户ID（接收通知的用户）
     * @param commodityId 商品ID
     * @param operation 操作类型（COMMODITY_UPDATED, COMMODITY_SHELVED 等）
     */
    public void sendCommodityEvent(String userId, String commodityId, String operation) {
        try {
            CommodityEvent event = new CommodityEvent(userId, commodityId, operation, LocalDateTime.now());
            
            // 根据操作类型确定路由键
            String routingKey = "commodity." + operation.toLowerCase().replace("_", ".");
            
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.COMMODITY_EXCHANGE,
                    routingKey,
                    event
            );
            
            log.info("商品事件已发送: userId={}, commodityId={}, operation={}", userId, commodityId, operation);
        } catch (Exception e) {
            log.error("发送商品事件失败: userId={}, commodityId={}, operation={}, error={}", 
                    userId, commodityId, operation, e.getMessage(), e);
        }
    }
}

