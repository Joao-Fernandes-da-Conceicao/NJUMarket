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
     * 订单事件交换机（Topic 类型）
     */
    public static final String ORDER_EXCHANGE = "order.exchange";

    /**
     * 订单事件路由键（匹配 order.* 全部子类型）
     * 消费者队列由各实例在启动时自行声明（AnonymousQueue），与消息推送队列同一扇出模型。
     */
    public static final String ORDER_ROUTING_KEY = "order.#";

    // ========== 消息推送事件相关 ==========

    /**
     * 消息推送事件交换机（与 Message 服务共享，Topic 类型）。
     * 每个 Notification 实例在启动时声明一个 AnonymousQueue（exclusive + autoDelete），
     * 并绑定到此交换机，实现扇出（fanout）语义：所有在线实例都能收到每条推送事件，
     * 各自判断目标用户是否连接在本实例，是则推送，否则静默丢弃。
     */
    public static final String MESSAGE_PUSH_EXCHANGE = "message.push.exchange";

    /**
     * 消息推送事件路由键（匹配 message.push.* 全部子类型）
     */
    public static final String MESSAGE_PUSH_ROUTING_KEY = "message.push.#";

    /**
     * 创建订单事件交换机（Topic 类型）。
     */
    @Bean
    public TopicExchange orderExchange() {
        return new TopicExchange(ORDER_EXCHANGE, true, false);
    }

    /**
     * 每个 Notification 实例专属的订单事件临时队列（exclusive + autoDelete）。
     * 与消息推送队列采用相同的扇出模型：所有在线实例各收一份事件，
     * 由持有目标用户 WebSocket 连接的实例完成推送。
     */
    @Bean
    public org.springframework.amqp.core.AnonymousQueue instanceOrderQueue() {
        org.springframework.amqp.core.AnonymousQueue queue = new org.springframework.amqp.core.AnonymousQueue();
        log.info("✅ 创建实例专属订单事件队列: {}", queue.getName());
        return queue;
    }

    /**
     * 将实例专属订单队列绑定到订单事件交换机。
     */
    @Bean
    public Binding instanceOrderBinding(
            org.springframework.amqp.core.AnonymousQueue instanceOrderQueue,
            TopicExchange orderExchange) {
        Binding binding = BindingBuilder
                .bind(instanceOrderQueue)
                .to(orderExchange)
                .with(ORDER_ROUTING_KEY);
        log.info("✅ 绑定实例专属订单队列到交换机: {} -> {} (routingKey: {})",
                instanceOrderQueue.getName(), ORDER_EXCHANGE, ORDER_ROUTING_KEY);
        return binding;
    }

    /**
     * 创建消息推送事件交换机（Topic 类型，与 Message 服务共享）。
     */
    @Bean
    public TopicExchange messagePushExchange() {
        return new TopicExchange(MESSAGE_PUSH_EXCHANGE, true, false);
    }

    /**
     * 每个 Notification 实例专属的临时队列（exclusive + autoDelete）。
     * 实例启动时自动创建并绑定，实例下线时 RabbitMQ 自动删除，无需手动清理。
     * 多实例部署时，每个实例拥有独立队列，交换机将每条消息扇出给所有实例。
     */
    @Bean
    public org.springframework.amqp.core.AnonymousQueue instanceMessagePushQueue() {
        org.springframework.amqp.core.AnonymousQueue queue = new org.springframework.amqp.core.AnonymousQueue();
        log.info("✅ 创建实例专属消息推送队列: {}", queue.getName());
        return queue;
    }

    /**
     * 将实例专属队列绑定到消息推送交换机。
     */
    @Bean
    public Binding instanceMessagePushBinding(
            org.springframework.amqp.core.AnonymousQueue instanceMessagePushQueue,
            TopicExchange messagePushExchange) {
        Binding binding = BindingBuilder
                .bind(instanceMessagePushQueue)
                .to(messagePushExchange)
                .with(MESSAGE_PUSH_ROUTING_KEY);
        log.info("✅ 绑定实例专属队列到消息推送交换机: {} -> {} (routingKey: {})",
                instanceMessagePushQueue.getName(), MESSAGE_PUSH_EXCHANGE, MESSAGE_PUSH_ROUTING_KEY);
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
        log.info("✅ RabbitMQ 配置初始化完成 - 订单交换机: {}, 路由键: {}; 消息推送交换机: {}（队列由各实例动态创建）",
                ORDER_EXCHANGE, ORDER_ROUTING_KEY, MESSAGE_PUSH_EXCHANGE);
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

