# 南大集市 NJUMarket v2.2.0 项目文档

## 📋 目录
- [版本概述](#版本概述)
- [版本更新内容](#版本更新内容)
- [Zipkin 分布式追踪优化](#zipkin-分布式追踪优化)
- [RabbitMQ 消息队列集成](#rabbitmq-消息队列集成)
- [使用指南](#使用指南)
- [技术总结](#技术总结)
- [后续版本规划](#后续版本规划)
  - [v2.2.1 已完成](#v221-已完成) - [查看文档](./PROJECT_DOCUMENTATION_V2.2.1.md)

---

## 版本概述

**NJUMarket v2.2.0** 是项目的分布式追踪和消息队列版本，主要完成了 Zipkin 分布式追踪的修复优化和 RabbitMQ 消息队列的集成。

### 版本信息
- **版本**: v2.2.0
- **发布日期**: 2025-11-15
- **状态**: ✅ **已完成**
- **主要目标**: 分布式追踪修复、异步消息处理、系统解耦

### 版本历史
- **v2.1.0** (2025-11-11): Actuator 监控、Docker 容器化、Swagger API 文档 ✅ **已完成**
- **v2.1.1** (2025-11-12): Resilience4j 熔断降级、Config Server 配置中心、环境隔离 ✅ **已完成**
- **v2.1.2** (2025-11-13): Prometheus + Grafana 监控集成 ✅ **已完成**
- **v2.2.0** (2025-11-15): Zipkin 追踪修复、RabbitMQ 消息队列集成 ✅ **已完成**

### 主要成就

#### 分布式追踪优化
- ✅ 修复 Feign Client 追踪上下文传播问题
- ✅ 添加 `feign-micrometer` 依赖
- ✅ 配置 B3 传播格式
- ✅ 确保完整的调用链追踪

#### 消息队列集成
- ✅ 集成 RabbitMQ 消息队列
- ✅ 实现订单事件异步通知（订单服务 → 通知服务）
- ✅ 实现消息推送事件异步处理（消息服务内部）
- ✅ 支持 `LocalDateTime` 序列化
- ✅ 实现 Redis 去重机制

#### 架构优化
- ✅ 服务间解耦（订单服务与通知服务）
- ✅ 服务内解耦（消息服务内部推送）
- ✅ 异步处理提升性能
- ✅ 解决消息重复推送问题

---

## 版本更新内容

### 1. Zipkin 分布式追踪修复 ✅

**目标**: 修复 Feign Client 调用链追踪缺失问题

**问题描述**:
- Gateway → Service 的调用链正常显示
- Service → Service（通过 Feign Client）的调用链缺失
- Feign Client 调用有 Zipkin 信息，但不在同一个 Trace 中

**完成内容**:

#### 1.1 添加 feign-micrometer 依赖
- **位置**: 所有服务的 `pom.xml`
- **依赖**: `io.github.openfeign:feign-micrometer`
- **作用**: 确保 Feign Client 的追踪上下文正确传播

#### 1.2 配置 B3 传播格式
- **配置项**: `management.tracing.propagation.type: B3`
- **位置**: 
  - `docker-compose.yml` 环境变量
  - 各服务的配置文件（`*.yml`）
- **作用**: 使用 Zipkin 标准传播格式

#### 1.3 启用追踪功能
- **配置项**: `management.tracing.enabled: true`
- **配置项**: `spring.cloud.openfeign.micrometer.enabled: true`
- **位置**: 
  - `docker-compose.yml` 环境变量
  - 各服务的配置文件（`*.yml`）

**修复效果**:
- ✅ Feign Client 调用链完整显示
- ✅ 所有服务调用在同一个 Trace 中
- ✅ 追踪 ID 统一一致

**详细说明**: 见 [Zipkin 分布式追踪优化](#zipkin-分布式追踪优化)

### 2. RabbitMQ 消息队列集成 ✅

**目标**: 实现服务间和服务内异步消息处理

**完成内容**:

#### 2.1 RabbitMQ 服务配置
- **位置**: `docker-compose.yml`
- **镜像**: `rabbitmq:3.12-management-alpine`
- **端口**: 
  - 5672: AMQP 端口
  - 15672: 管理界面端口
- **认证**: admin/admin

#### 2.2 订单事件异步通知
- **场景**: 订单服务 → 通知服务
- **事件类型**:
  - `ORDER_CREATED`: 订单创建
  - `ORDER_PAID`: 订单支付
  - `ORDER_SHIPPED`: 订单发货
  - `ORDER_COMPLETED`: 订单完成
  - `ORDER_CANCELLED`: 订单取消
  - `REFUND_REQUESTED`: 退款申请
  - `REFUND_APPROVED`: 退款批准
  - `REFUND_REJECTED`: 退款拒绝
- **实现**:
  - 生产者: `OrderEventProducer`（订单服务）
  - 消费者: `OrderEventConsumer`（通知服务）
  - 交换机: `order.exchange`（Topic 类型）
  - 队列: `order.queue`
  - 路由键: `order.#`（匹配所有订单事件）

#### 2.3 消息推送事件异步处理
- **场景**: 消息服务内部
- **目的**: 解决刷新后重复推送的问题
- **事件类型**:
  - `MESSAGE_NEW`: 新消息
  - `UNREAD_COUNT_UPDATE`: 未读数量更新
  - `CONVERSATION_RESTORED`: 会话恢复
  - `MESSAGE_READ`: 消息已读
- **实现**:
  - 生产者: `MessagePushEventProducer`（消息服务）
  - 消费者: `MessagePushEventConsumer`（消息服务）
  - 交换机: `message.push.exchange`（Topic 类型）
  - 队列: `message.push.queue`
  - 路由键: `message.push.#`（匹配所有推送事件）
  - 去重机制: Redis 存储已推送消息 ID

#### 2.4 消息转换器配置
- **支持类型**: `LocalDateTime` 等 Java 8 时间类型
- **实现**: 
  - 注册 `JavaTimeModule`
  - 禁用 `WRITE_DATES_AS_TIMESTAMPS`
  - 使用 ISO-8601 格式

#### 2.5 监听器配置
- **注解**: `@EnableRabbit`（消费者服务）
- **容器工厂**: `SimpleRabbitListenerContainerFactory`
- **并发配置**: 
  - 最小消费者: 1
  - 最大消费者: 5

**详细说明**: 见 [RabbitMQ 消息队列集成](#rabbitmq-消息队列集成)

---

## Zipkin 分布式追踪优化

### 问题分析

#### 问题现象
1. Gateway → Service 调用链正常
2. Service → Service（Feign Client）调用链缺失
3. 追踪 ID 不一致

#### 根本原因
1. **缺少 `feign-micrometer` 依赖**: Feign Client 无法自动集成 Micrometer
2. **缺少 B3 传播格式配置**: 追踪上下文无法正确传播
3. **配置路径错误**: `docker-compose.yml` 中配置路径不正确

### 解决方案

#### 1. 添加依赖

**所有服务的 `pom.xml`**:
```xml
<!-- Feign Micrometer 集成：确保 Feign Client 追踪上下文正确传播 -->
<dependency>
    <groupId>io.github.openfeign</groupId>
    <artifactId>feign-micrometer</artifactId>
</dependency>
```

#### 2. 配置 B3 传播格式

**配置文件（`*.yml`）**:
```yaml
management:
  tracing:
    enabled: true
    sampling:
      probability: 1.0
    propagation:
      type: B3  # ✅ 使用 B3 传播格式（Zipkin 标准格式）
  zipkin:
    tracing:
      endpoint: http://zipkin:9411/api/v2/spans
```

**docker-compose.yml**:
```yaml
environment:
  - SPRING_APPLICATION_JSON={"management":{"tracing":{"enabled":true,"propagation":{"type":"B3"}}}}
  - JAVA_TOOL_OPTIONS=-Dmanagement.tracing.enabled=true -Dmanagement.tracing.propagation.type=B3
```

#### 3. 启用 Feign Micrometer

**配置文件（`*.yml`）**:
```yaml
spring:
  cloud:
    openfeign:
      micrometer:
        enabled: true
```

**docker-compose.yml**:
```yaml
environment:
  - SPRING_APPLICATION_JSON={"spring":{"cloud":{"openfeign":{"micrometer":{"enabled":true}}}}}
  - JAVA_TOOL_OPTIONS=-Dspring.cloud.openfeign.micrometer.enabled=true
```

### 验证方法

#### 1. 检查追踪链
1. 访问 Zipkin UI: http://localhost:9411
2. 触发一个完整的请求（Gateway → Service A → Service B）
3. 查看追踪链是否完整

#### 2. 检查追踪 ID
- 所有服务调用应该使用相同的 Trace ID
- 每个服务调用应该有唯一的 Span ID

#### 3. 检查配置
- 访问 `/actuator/configprops` 检查配置是否生效
- 访问 `/actuator/conditions` 检查自动配置状态

---

## RabbitMQ 消息队列集成

### 架构设计

#### 订单事件流程
```
┌─────────────────┐
│   订单服务       │
│  (Producer)     │
└────────┬────────┘
         │ 发送事件
         ▼
┌─────────────────┐
│ order.exchange  │
│   (Topic)       │
└────────┬────────┘
         │ 路由 (order.#)
         ▼
┌─────────────────┐
│  order.queue    │
└────────┬────────┘
         │ 消费
         ▼
┌─────────────────┐
│   通知服务       │
│  (Consumer)     │
└─────────────────┘
```

#### 消息推送流程
```
┌─────────────────┐
│   消息服务       │
│  (Producer)     │
└────────┬────────┘
         │ 发送推送事件
         ▼
┌─────────────────┐
│message.push.    │
│   exchange      │
│   (Topic)       │
└────────┬────────┘
         │ 路由 (message.push.#)
         ▼
┌─────────────────┐
│message.push.    │
│    queue        │
└────────┬────────┘
         │ 消费（去重）
         ▼
┌─────────────────┐
│   消息服务       │
│  (Consumer)     │
│  WebSocket推送   │
└─────────────────┘
```

### 配置说明

#### 1. 依赖配置

**所有相关服务的 `pom.xml`**:
```xml
<!-- RabbitMQ 消息队列 -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-amqp</artifactId>
</dependency>
```

#### 2. 连接配置

**配置文件（`*.yml`）**:
```yaml
spring:
  rabbitmq:
    host: ${RABBITMQ_HOST:rabbitmq}
    port: ${RABBITMQ_PORT:5672}
    username: ${RABBITMQ_USERNAME:admin}
    password: ${RABBITMQ_PASSWORD:admin}
    virtual-host: /
    publisher-confirm-type: correlated
    publisher-returns: true
    template:
      mandatory: true
```

#### 3. 交换机、队列、绑定配置

**生产者服务（订单服务）**:
```java
@Configuration
public class RabbitMQConfig {
    public static final String ORDER_EXCHANGE = "order.exchange";
    
    @Bean
    public TopicExchange orderExchange() {
        return new TopicExchange(ORDER_EXCHANGE, true, false);
    }
}
```

**消费者服务（通知服务）**:
```java
@Configuration
public class RabbitMQConfig {
    public static final String ORDER_EXCHANGE = "order.exchange";
    public static final String ORDER_QUEUE = "order.queue";
    public static final String ORDER_ROUTING_KEY = "order.#";  // ✅ 使用 # 匹配多级路由键
    
    @Bean
    public TopicExchange orderExchange() {
        return new TopicExchange(ORDER_EXCHANGE, true, false);
    }
    
    @Bean
    public Queue orderQueue() {
        return QueueBuilder.durable(ORDER_QUEUE).build();
    }
    
    @Bean
    public Binding orderBinding() {
        return BindingBuilder
                .bind(orderQueue())
                .to(orderExchange())
                .with(ORDER_ROUTING_KEY);
    }
}
```

#### 4. 消息转换器配置

**所有服务的 `RabbitMQConfig.java`**:
```java
@Bean
public MessageConverter jsonMessageConverter() {
    ObjectMapper objectMapper = new ObjectMapper();
    // ✅ 注册 JavaTimeModule 以支持 LocalDateTime 等 Java 8 时间类型
    objectMapper.registerModule(new JavaTimeModule());
    // ✅ 禁用将日期写为时间戳（使用 ISO-8601 格式）
    objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    return new Jackson2JsonMessageConverter(objectMapper);
}
```

#### 5. 监听器配置

**消费者服务的启动类**:
```java
@EnableRabbit  // ✅ 启用 RabbitMQ 监听器（@RabbitListener）
public class NotificationServiceApplication {
    // ...
}
```

**消费者服务的 `RabbitMQConfig.java`**:
```java
@Bean
public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(ConnectionFactory connectionFactory) {
    SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
    factory.setConnectionFactory(connectionFactory);
    factory.setMessageConverter(jsonMessageConverter());
    factory.setConcurrentConsumers(1);
    factory.setMaxConcurrentConsumers(5);
    return factory;
}
```

### 关键配置点

#### 路由键匹配规则

**重要**: 使用 `#` 而不是 `*` 来匹配多级路由键

- `*` 只匹配一个单词（如 `order.created`）
- `#` 匹配零个或多个单词（如 `order.refund.approved`）

**示例**:
- 发送路由键: `order.refund.approved`
- 绑定路由键: `order.#` ✅ 可以匹配
- 绑定路由键: `order.*` ❌ 无法匹配

#### 配置分离原则

- **生产者服务**: 只创建交换机，不创建队列和绑定
- **消费者服务**: 创建交换机、队列和绑定

**原因**: 避免配置冲突，确保队列和绑定由消费者管理

### 去重机制

**消息推送事件去重**:
```java
@RabbitListener(queues = RabbitMQConfig.MESSAGE_PUSH_QUEUE)
public void handleMessagePushEvent(MessagePushEvent event) {
    // ✅ 去重检查：如果消息已经推送过，则跳过
    String pushedKey = PUSHED_MESSAGE_KEY_PREFIX + event.getMessageId();
    Boolean alreadyPushed = redisTemplate.hasKey(pushedKey);
    
    if (Boolean.TRUE.equals(alreadyPushed)) {
        log.debug("消息已推送过，跳过: messageId={}", event.getMessageId());
        return;
    }
    
    // 推送消息
    webSocketRetryService.pushWithRetry(...);
    
    // ✅ 标记消息已推送（用于去重）
    redisTemplate.opsForValue().set(pushedKey, "1", PUSHED_MESSAGE_TTL);
}
```

---

## 使用指南

### 快速开始

#### 1. 启动服务

```bash
docker-compose up -d
```

#### 2. 访问 RabbitMQ 管理界面

- **URL**: http://localhost:15672
- **用户名**: `admin`
- **密码**: `admin`

#### 3. 查看队列和绑定

1. 登录 RabbitMQ 管理界面
2. 点击 "Queues" 标签
3. 查看 `order.queue` 和 `message.push.queue`
4. 检查消费者连接状态

#### 4. 查看交换机

1. 点击 "Exchanges" 标签
2. 查看 `order.exchange` 和 `message.push.exchange`
3. 检查绑定关系

### 验证消息流转

#### 1. 验证订单事件

1. 触发一个订单操作（如创建订单、支付订单等）
2. 查看订单服务日志，确认事件已发送
3. 查看通知服务日志，确认事件已接收
4. 在 RabbitMQ 管理界面查看队列消息数量

#### 2. 验证消息推送

1. 发送一条消息
2. 查看消息服务日志，确认推送事件已发送
3. 查看消息服务日志，确认推送事件已接收
4. 验证 WebSocket 推送是否正常

### 故障排查

#### 1. 消息未消费

**检查项**:
- 消费者服务是否启动
- `@EnableRabbit` 注解是否添加
- 监听器容器是否启动
- 队列绑定是否正确
- 路由键是否匹配

**日志检查**:
```
✅ OrderEventConsumer 已初始化，准备监听队列: order.queue
✅ 配置 RabbitMQ 监听器容器工厂
✅ 创建订单事件队列: order.queue
✅ 绑定订单队列到交换机: order.queue -> order.exchange (routingKey: order.#)
```

#### 2. 消息未路由到队列

**检查项**:
- 交换机是否存在
- 绑定关系是否正确
- 路由键是否匹配（使用 `#` 而不是 `*`）

**验证方法**:
- 在 RabbitMQ 管理界面查看交换机绑定
- 检查发送的路由键和绑定的路由键

#### 3. 消息序列化失败

**检查项**:
- `JavaTimeModule` 是否注册
- `WRITE_DATES_AS_TIMESTAMPS` 是否禁用
- 消息转换器是否配置

**错误示例**:
```
MessageConversionException: Failed to convert Message content
Caused by: InvalidDefinitionException: Java 8 date/time type `java.time.LocalDateTime` not supported
```

---

## 技术总结

### 技术选型

#### RabbitMQ
- **优势**:
  - 成熟稳定的消息队列
  - 支持多种消息模式（Topic、Direct、Fanout 等）
  - 提供管理界面
  - 与 Spring Boot 集成简单
- **适用场景**: 服务间异步通信、事件驱动架构

#### Spring AMQP
- **优势**:
  - 简化 RabbitMQ 使用
  - 提供声明式配置
  - 支持消息转换器
  - 支持事务和确认机制
- **适用场景**: Spring Boot 应用集成 RabbitMQ

### 架构设计

#### 异步处理优势
1. **解耦**: 服务间通过消息队列解耦
2. **性能**: 异步处理不阻塞主流程
3. **可靠性**: 消息持久化，支持重试
4. **扩展性**: 支持多个消费者

#### 路由键设计
- **订单事件**: `order.#` 匹配所有订单相关事件
- **消息推送**: `message.push.#` 匹配所有推送事件
- **灵活性**: 支持未来扩展新的事件类型

### 配置要点

#### 1. 路由键匹配
- ✅ 使用 `#` 匹配多级路由键
- ❌ 避免使用 `*` 匹配多级路由键

#### 2. 配置分离
- ✅ 生产者只创建交换机
- ✅ 消费者创建队列和绑定

#### 3. 消息转换器
- ✅ 注册 `JavaTimeModule`
- ✅ 禁用 `WRITE_DATES_AS_TIMESTAMPS`

#### 4. 监听器启用
- ✅ 消费者服务添加 `@EnableRabbit`
- ✅ 配置监听器容器工厂

---

## 后续版本规划

### v2.2.1 已完成 ✅

**状态**: ✅ **已完成**  
**日期**: 2025-11-15  
**文档**: [PROJECT_DOCUMENTATION_V2.2.1.md](./PROJECT_DOCUMENTATION_V2.2.1.md)

#### 主要成就

- ✅ **WebSocket 推送架构重构**: 彻底解决 Message 服务推送困难问题
- ✅ **问题根源**: 发现并解决了不同 WebSocket 实例的连接竞争问题
- ✅ **架构优化**: Message 服务完全剥离推送功能，统一由 Notification 服务管理
- ✅ **历史清理**: 清理了从单体项目时期遗留的架构问题

#### 问题解决

- ✅ 推送成功率从 67% 提升至接近 100%
- ✅ 丢包率从 33% 降至接近 0%
- ✅ 彻底消除 WebSocket 连接竞争
- ✅ 统一推送管理，架构更加清晰

#### 技术实现

- ✅ Message 服务通过 MQ 发送推送任务
- ✅ Notification 服务统一处理所有推送
- ✅ 所有推送使用同一个 WebSocket 实例
- ✅ 完整的代码清理和文档

---

### v2.3.0 规划（未来）

#### WebSocket 推送确认机制
- [ ] **对于订单和聊天将 ACK 加入 WebSocket 推送机制，前后端联改**
  - 实现客户端确认（ACK）机制
  - 前端收到消息后发送确认回执
  - 后端收到 ACK 后标记消息为已送达
  - 未收到 ACK 的消息进行重试
  - 提升消息推送的可靠性

### 短期规划（可选）

#### 消息队列增强
- [ ] 配置死信队列（DLQ）
- [ ] 实现消息重试机制
- [ ] 配置消息 TTL
- [ ] 实现消息优先级

#### 监控和告警
- [ ] 集成 RabbitMQ 监控指标到 Prometheus
- [ ] 创建 RabbitMQ 监控仪表板
- [ ] 配置队列堆积告警

#### 性能优化
- [ ] 调整消费者并发数
- [ ] 优化消息序列化性能
- [ ] 实现消息批量处理

### 长期规划

#### 消息队列集群
- [ ] 配置 RabbitMQ 集群
- [ ] 实现高可用
- [ ] 配置镜像队列

#### 其他消息队列
- [ ] 评估 Kafka 适用场景
- [ ] 评估 RocketMQ 适用场景
- [ ] 实现多消息队列支持

---

## 已知问题和限制

### 当前限制

1. **死信队列未配置**
   - 消息处理失败后无法自动重试
   - 需要手动处理失败消息

2. **监控指标未集成**
   - RabbitMQ 指标未集成到 Prometheus
   - 无法监控队列堆积情况

3. **消息顺序性**
   - 当前不保证消息顺序
   - 如需顺序处理，需要单消费者或分区队列

### 已知问题

1. **路由键匹配**
   - 已修复：使用 `#` 而不是 `*` 匹配多级路由键

2. **消息序列化**
   - 已修复：注册 `JavaTimeModule` 支持 `LocalDateTime`

3. **监听器启动**
   - 已修复：添加 `@EnableRabbit` 注解

---

## 参考资源

### RabbitMQ
- [RabbitMQ 官方文档](https://www.rabbitmq.com/documentation.html)
- [Spring AMQP 文档](https://docs.spring.io/spring-amqp/docs/current/reference/html/)
- [RabbitMQ 管理界面](http://localhost:15672)

### Spring Boot
- [Spring Boot AMQP 文档](https://docs.spring.io/spring-boot/docs/current/reference/html/messaging.html#messaging.amqp)

### 相关文档
- [ZIPKIN_FEIGN_TRACING_FIX.md](./ZIPKIN_FEIGN_TRACING_FIX.md) - Zipkin 追踪修复文档
- [ZIPKIN_TRACING_CONTEXT_PROPAGATION.md](./ZIPKIN_TRACING_CONTEXT_PROPAGATION.md) - 追踪上下文传播文档

---

## 版本总结

### 完成情况

✅ **已完成**:
- Zipkin 分布式追踪修复和优化
- RabbitMQ 消息队列集成
- 订单事件异步通知
- 消息推送事件异步处理
- 消息去重机制
- `LocalDateTime` 序列化支持
- 完整的配置和文档

⏸️ **暂未实现**:
- 死信队列配置
- 消息重试机制
- RabbitMQ 监控指标集成

### 技术亮点

1. **完整的追踪链**: 修复了 Feign Client 追踪上下文传播问题
2. **异步消息处理**: 实现了服务间和服务内的异步通信
3. **路由键设计**: 使用 `#` 匹配多级路由键，支持灵活扩展
4. **去重机制**: 使用 Redis 实现消息推送去重
5. **配置分离**: 生产者只创建交换机，消费者管理队列和绑定

### 学习价值

1. **分布式追踪**: 了解 Zipkin 和 Micrometer 集成
2. **消息队列**: 学习 RabbitMQ 和 Spring AMQP
3. **异步架构**: 理解事件驱动架构设计
4. **配置管理**: 掌握微服务配置分离原则

---

**版本**: v2.2.0  
**状态**: ✅ **已完成**  
**日期**: 2025-11-15

---

## 版本历史

- **v2.1.0** (2025-11-11): Actuator 监控、Docker 容器化、Swagger API 文档 ✅ **已完成**
- **v2.1.1** (2025-11-12): Resilience4j 熔断降级、Config Server 配置中心、环境隔离 ✅ **已完成**
- **v2.1.2** (2025-11-13): Prometheus + Grafana 监控集成 ✅ **已完成**
- **v2.2.0** (2025-11-15): Zipkin 追踪修复、RabbitMQ 消息队列集成 ✅ **已完成**
- **v2.2.1** (2025-11-15): WebSocket 推送架构重构 ✅ **已完成** - [查看文档](./PROJECT_DOCUMENTATION_V2.2.1.md)

