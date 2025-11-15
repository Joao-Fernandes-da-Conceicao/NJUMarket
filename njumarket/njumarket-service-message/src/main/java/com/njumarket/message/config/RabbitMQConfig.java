package com.njumarket.message.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ 配置类
 * 用于消息服务发送推送任务到Notification服务
 * Message服务只负责消息和聊天的相关操作，推送功能完全移交给Notification服务
 */
@Configuration
public class RabbitMQConfig {

    // ========== 消息推送事件相关 ==========
    
    /**
     * 消息推送事件交换机
     */
    public static final String MESSAGE_PUSH_EXCHANGE = "message.push.exchange";
    
    /**
     * 消息推送事件队列
     */
    public static final String MESSAGE_PUSH_QUEUE = "message.push.queue";
    
    /**
     * 消息推送事件路由键
     * 使用 message.push.# 匹配所有以 message.push. 开头的路由键（如 message.push.message.new, message.push.unread.count.update 等）
     * # 匹配零个或多个单词，* 只匹配一个单词
     */
    public static final String MESSAGE_PUSH_ROUTING_KEY = "message.push.#";

    /**
     * 创建消息推送事件交换机（Topic 类型）
     */
    @Bean
    public TopicExchange messagePushExchange() {
        return new TopicExchange(MESSAGE_PUSH_EXCHANGE, true, false);
    }

    /**
     * 创建消息推送事件队列
     */
    @Bean
    public Queue messagePushQueue() {
        return QueueBuilder.durable(MESSAGE_PUSH_QUEUE).build();
    }

    /**
     * 绑定消息推送队列到交换机
     */
    @Bean
    public Binding messagePushBinding() {
        return BindingBuilder
                .bind(messagePushQueue())
                .to(messagePushExchange())
                .with(MESSAGE_PUSH_ROUTING_KEY);
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

    /**
     * 配置监听器容器工厂（用于 @RabbitListener）
     * 确保消费者使用正确的消息转换器
     */
    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(jsonMessageConverter());
        // 设置并发消费者数量
        factory.setConcurrentConsumers(1);
        factory.setMaxConcurrentConsumers(5);
        return factory;
    }
    
}

