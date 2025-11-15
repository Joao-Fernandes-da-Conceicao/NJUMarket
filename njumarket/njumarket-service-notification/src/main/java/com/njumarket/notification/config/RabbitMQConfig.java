package com.njumarket.notification.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ 配置类
 * 用于通知服务接收订单和商品事件消息
 */
@Slf4j
@Configuration
public class RabbitMQConfig {

    // ========== 订单事件相关 ==========
    
    /**
     * 订单事件交换机
     */
    public static final String ORDER_EXCHANGE = "order.exchange";
    
    /**
     * 订单事件队列
     */
    public static final String ORDER_QUEUE = "order.queue";
    
    /**
     * 订单事件路由键
     * 使用 order.# 匹配所有以 order. 开头的路由键（如 order.refund.approved, order.created 等）
     * # 匹配零个或多个单词，* 只匹配一个单词
     */
    public static final String ORDER_ROUTING_KEY = "order.#";

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

    // ========== 消息推送事件相关 ==========
    
    /**
     * 消息推送事件交换机（与Message服务共享）
     */
    public static final String MESSAGE_PUSH_EXCHANGE = "message.push.exchange";
    
    /**
     * 消息推送事件队列（Notification服务消费）
     */
    public static final String MESSAGE_PUSH_QUEUE = "message.push.queue";
    
    /**
     * 消息推送事件路由键
     * 使用 message.push.# 匹配所有以 message.push. 开头的路由键
     */
    public static final String MESSAGE_PUSH_ROUTING_KEY = "message.push.#";

    /**
     * 创建订单事件交换机（Topic 类型）
     */
    @Bean
    public TopicExchange orderExchange() {
        return new TopicExchange(ORDER_EXCHANGE, true, false);
    }

    /**
     * 创建订单事件队列
     */
    @Bean
    public Queue orderQueue() {
        Queue queue = QueueBuilder.durable(ORDER_QUEUE).build();
        log.info("✅ 创建订单事件队列: {}", ORDER_QUEUE);
        return queue;
    }

    /**
     * 绑定订单队列到交换机
     */
    @Bean
    public Binding orderBinding() {
        Binding binding = BindingBuilder
                .bind(orderQueue())
                .to(orderExchange())
                .with(ORDER_ROUTING_KEY);
        log.info("✅ 绑定订单队列到交换机: {} -> {} (routingKey: {})", ORDER_QUEUE, ORDER_EXCHANGE, ORDER_ROUTING_KEY);
        return binding;
    }

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

    /**
     * 创建消息推送事件交换机（Topic 类型，与Message服务共享）
     */
    @Bean
    public TopicExchange messagePushExchange() {
        return new TopicExchange(MESSAGE_PUSH_EXCHANGE, true, false);
    }

    /**
     * 创建消息推送事件队列（Notification服务消费）
     */
    @Bean
    public Queue messagePushQueue() {
        Queue queue = QueueBuilder.durable(MESSAGE_PUSH_QUEUE).build();
        log.info("✅ 创建消息推送事件队列: {}", MESSAGE_PUSH_QUEUE);
        return queue;
    }

    /**
     * 绑定消息推送队列到交换机
     */
    @Bean
    public Binding messagePushBinding() {
        Binding binding = BindingBuilder
                .bind(messagePushQueue())
                .to(messagePushExchange())
                .with(MESSAGE_PUSH_ROUTING_KEY);
        log.info("✅ 绑定消息推送队列到交换机: {} -> {} (routingKey: {})", 
                MESSAGE_PUSH_QUEUE, MESSAGE_PUSH_EXCHANGE, MESSAGE_PUSH_ROUTING_KEY);
        return binding;
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
        log.info("✅ 配置 RabbitMQ 监听器容器工厂: concurrentConsumers=1, maxConcurrentConsumers=5");
        return factory;
    }

    /**
     * 配置WebSocket推送任务执行器
     * 用于在MQ消费者线程中提交推送任务到正确的线程池执行
     * 确保推送操作有正确的WebSocket上下文
     */
    @Bean(name = "websocketPushTaskExecutor")
    public org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor websocketPushTaskExecutor() {
        org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor executor = 
                new org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(20);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("websocket-push-");
        executor.setRejectedExecutionHandler(new java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        log.info("✅ 配置 WebSocket 推送任务执行器: corePoolSize=5, maxPoolSize=20");
        return executor;
    }

    /**
     * 初始化后检查配置
     */
    @PostConstruct
    public void init() {
        log.info("✅ RabbitMQ 配置初始化完成 - 订单队列: {}, 交换机: {}, 路由键: {}", 
                ORDER_QUEUE, ORDER_EXCHANGE, ORDER_ROUTING_KEY);
    }

    /**
     * 监听应用启动完成事件，检查监听器容器状态
     */
    @Bean
    public ApplicationListener<ApplicationReadyEvent> rabbitListenerStatusChecker() {
        return event -> {
            log.info("✅ 应用启动完成，检查 RabbitMQ 监听器状态...");
            // 这里可以添加检查逻辑，但 Spring 会自动启动 @RabbitListener
            log.info("✅ @RabbitListener 监听器应该已经启动（由 Spring 自动管理）");
        };
    }
}

