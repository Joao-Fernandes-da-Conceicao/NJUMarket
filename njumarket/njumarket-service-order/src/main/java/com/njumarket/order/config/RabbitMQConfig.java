package com.njumarket.order.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ 配置类
 * 用于订单服务发送订单事件消息
 */
@Configuration
public class RabbitMQConfig {

    // ========== 订单事件相关 ==========
    
    /**
     * 订单事件交换机
     */
    public static final String ORDER_EXCHANGE = "order.exchange";
    
    /**
     * 创建订单事件交换机（Topic 类型）
     * 注意：订单服务只需要创建交换机，不需要创建队列和绑定
     * 队列和绑定由通知服务创建
     */
    @Bean
    public TopicExchange orderExchange() {
        return new TopicExchange(ORDER_EXCHANGE, true, false);
    }

    // ========== 消息转换器 ==========
    
    /**
     * JSON 消息转换器（支持 Java 8 时间类型）
     */
    @Bean
    public MessageConverter jsonMessageConverter() {
        ObjectMapper objectMapper = new ObjectMapper();
        // ✅ 注册 JavaTimeModule 以支持 LocalDateTime 等 Java 8 时间类型
        objectMapper.registerModule(new JavaTimeModule());
        // ✅ 禁用将日期写为时间戳（使用 ISO-8601 格式）
        objectMapper.disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return new Jackson2JsonMessageConverter(objectMapper);
    }

    /**
     * 配置 RabbitTemplate
     */
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter());
        // 设置消息确认回调
        template.setConfirmCallback((correlationData, ack, cause) -> {
            if (!ack) {
                System.err.println("消息发送失败: " + cause);
            }
        });
        // 设置消息返回回调
        template.setReturnsCallback(returned -> {
            System.err.println("消息返回: " + returned.getMessage());
        });
        return template;
    }
}

