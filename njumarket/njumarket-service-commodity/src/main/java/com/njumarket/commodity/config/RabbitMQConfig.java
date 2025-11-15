package com.njumarket.commodity.config;

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
 * 用于商品服务发送商品事件消息
 */
@Configuration
public class RabbitMQConfig {

    // ========== 商品事件相关 ==========
    
    /**
     * 商品事件交换机
     */
    public static final String COMMODITY_EXCHANGE = "commodity.exchange";
    
    /**
     * 商品事件队列
     */
    public static final String COMMODITY_QUEUE = "commodity.queue";
    
    /**
     * 商品事件路由键
     */
    public static final String COMMODITY_ROUTING_KEY = "commodity.*";

    /**
     * 创建商品事件交换机（Topic 类型）
     */
    @Bean
    public TopicExchange commodityExchange() {
        return new TopicExchange(COMMODITY_EXCHANGE, true, false);
    }

    /**
     * 创建商品事件队列
     */
    @Bean
    public Queue commodityQueue() {
        return QueueBuilder.durable(COMMODITY_QUEUE).build();
    }

    /**
     * 绑定商品队列到交换机
     */
    @Bean
    public Binding commodityBinding() {
        return BindingBuilder
                .bind(commodityQueue())
                .to(commodityExchange())
                .with(COMMODITY_ROUTING_KEY);
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
        template.setConfirmCallback((correlationData, ack, cause) -> {
            if (!ack) {
                System.err.println("消息发送失败: " + cause);
            }
        });
        template.setReturnsCallback(returned -> {
            System.err.println("消息返回: " + returned.getMessage());
        });
        return template;
    }
}

