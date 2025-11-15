# 南大集市 NJUMarket v2.2.1 项目文档

## 📋 目录
- [版本概述](#版本概述)
- [问题背景](#问题背景)
- [问题根源分析](#问题根源分析)
- [解决方案](#解决方案)
- [架构重构详情](#架构重构详情)
- [技术实现](#技术实现)
- [使用指南](#使用指南)
- [技术总结](#技术总结)
- [后续版本规划](#后续版本规划)

---

## 版本概述

**NJUMarket v2.2.1** 是项目的 WebSocket 推送架构重构版本，彻底解决了 Message 服务推送困难的问题，完成了推送功能的统一管理。

### 版本信息
- **版本**: v2.2.1
- **发布日期**: 2025-11-15
- **状态**: ✅ **已完成**
- **主要目标**: WebSocket 推送架构重构、解决推送困难问题、统一推送管理

### 版本历史
- **v2.1.0** (2025-11-11): Actuator 监控、Docker 容器化、Swagger API 文档 ✅ **已完成**
- **v2.1.1** (2025-11-12): Resilience4j 熔断降级、Config Server 配置中心、环境隔离 ✅ **已完成**
- **v2.1.2** (2025-11-13): Prometheus + Grafana 监控集成 ✅ **已完成**
- **v2.2.0** (2025-11-15): Zipkin 追踪修复、RabbitMQ 消息队列集成 ✅ **已完成**
- **v2.2.1** (2025-11-15): WebSocket 推送架构重构 ✅ **已完成**

### 主要成就

#### 问题解决
- ✅ 彻底解决 Message 服务推送困难问题（丢包率从 33% 降至接近 0%）
- ✅ 消除不同 WebSocket 实例的连接竞争
- ✅ 统一推送管理，所有推送通过 Notification 服务处理

#### 架构优化
- ✅ Message 服务完全剥离推送功能，只负责消息和聊天业务逻辑
- ✅ Notification 服务统一管理所有推送（订单、消息、未读数等）
- ✅ 通过 MQ 实现服务间解耦，Message 服务通过 MQ 发送推送任务
- ✅ 所有推送使用同一个 WebSocket 实例，避免连接竞争

#### 代码清理
- ✅ 移除 Message 服务中所有 WebSocket 相关代码
- ✅ 移除 Message 服务中的 NotificationClient 和 Fallback
- ✅ 简化 Message 服务架构，职责更加清晰

---

## 问题背景

### 问题现象

在 v2.2.0 及之前的版本中，Message 服务的 WebSocket 推送存在严重问题：

1. **高丢包率**: 消息推送的丢包率高达约 33%，基本上发送 3 条消息就有 1 条无法推送成功
2. **重试无效**: 即使多次重试，消息仍然无法推送成功
3. **对比明显**: Notification 服务的推送几乎不会失败，成功率接近 100%
4. **历史问题**: 这个问题从单体项目时期就一直存在，持续到微服务架构

### 问题影响

- 用户经常收不到聊天消息
- 未读数更新不及时
- 会话恢复通知丢失
- 已读回执无法正常推送
- 严重影响用户体验

---

## 问题根源分析

### 核心问题：不同 WebSocket 实例的连接竞争

经过深入分析和诊断，问题的根源在于：

#### 1. 前端连接的是 Notification 服务的 WebSocket

```
Gateway 路由配置：
/api/ws/** → njumarket-service-notification

前端连接：
ws://localhost:8080/api/ws → 实际连接到 Notification 服务
```

#### 2. Message 服务使用独立的 WebSocket 实例

```
Message 服务配置：
- 独立的 WebSocketConfig
- 独立的 SimpleBroker 实例
- 独立的 SimpUserRegistry

推送目标：
/user/{userId}/queue/message
```

#### 3. 连接竞争导致推送失败

```
前端连接状态：
- 已连接：Notification 服务的 WebSocket（/user/queue/message）
- 未连接：Message 服务的 WebSocket

Message 服务推送：
- 尝试推送到 /user/{userId}/queue/message
- 但前端连接的是 Notification 服务的 WebSocket
- 不同的 SimpleBroker 实例，无法找到用户会话
- 推送失败，消息丢失
```

### 历史原因追溯

#### 单体项目时期（v1.x）

在单体项目时期，不同功能模块（消息、订单通知等）各自实现了 WebSocket 推送，没有统一管理：

```
单体项目架构：
- 消息模块：独立的 WebSocket 推送
- 订单模块：独立的 WebSocket 推送
- 商品模块：独立的 WebSocket 推送

问题：
- 每个模块使用不同的 WebSocket 端点
- 前端需要连接多个 WebSocket
- 或者只连接一个，其他推送失败
```

#### 微服务拆分时期（v2.0）

从单体项目拆分为微服务时，没有彻底清理推送功能：

```
拆分过程：
1. 创建 Notification 服务，统一管理推送
2. 但 Message 服务保留了原有的 WebSocket 推送代码
3. 前端只连接 Notification 服务的 WebSocket
4. Message 服务的推送功能被保留但无法使用

结果：
- Message 服务有推送代码，但推送不出去
- Notification 服务推送正常
- 问题被隐藏，直到现在才被发现
```

### 技术细节

#### SimpleBroker 的内存特性

Spring WebSocket 的 `SimpleBroker` 是基于内存的消息代理：

```java
// 每个服务实例有独立的 SimpleBroker
config.enableSimpleBroker("/queue", "/topic");

// 用户会话存储在内存中
SimpUserRegistry.getUser(userId)  // 只能找到本服务实例的用户
```

#### convertAndSendToUser 的工作原理

```java
// Message 服务尝试推送
messagingTemplate.convertAndSendToUser(userId, "/queue/message", data);

// 内部流程：
1. 查找 SimpUserRegistry 中的用户会话
2. 但用户连接在 Notification 服务的 WebSocket
3. Message 服务的 SimpUserRegistry 中找不到用户
4. 推送失败，消息被丢弃（不会抛出异常）
```

---

## 解决方案

### 架构重构：彻底分离推送功能

#### 核心原则

1. **Message 服务**: 只负责消息和聊天的业务逻辑，不涉及推送
2. **Notification 服务**: 统一负责所有推送（订单、消息、未读数等）
3. **通信方式**: Message 服务通过 MQ 发送推送任务到 Notification 服务

#### 架构对比

**重构前（v2.2.0）**:
```
Message 服务:
├── 消息业务逻辑 ✅
├── WebSocket 推送 ❌ (无法使用)
└── NotificationClient ❌ (直接调用，但推送失败)

Notification 服务:
├── 订单推送 ✅
└── WebSocket 实例 ✅

前端:
└── 连接 Notification 服务的 WebSocket ✅

问题: Message 服务推送失败，丢包率 33%
```

**重构后（v2.2.1）**:
```
Message 服务:
├── 消息业务逻辑 ✅
└── MQ 生产者 ✅ (发送推送任务)

Notification 服务:
├── 订单推送 ✅
├── 消息推送 ✅ (通过 MQ 接收任务)
├── WebSocket 实例 ✅ (统一管理)
└── MQ 消费者 ✅ (接收 Message 服务的推送任务)

前端:
└── 连接 Notification 服务的 WebSocket ✅

结果: 所有推送成功，丢包率接近 0%
```

---

## 架构重构详情

### 1. Message 服务变更

#### 移除的组件

1. **WebSocket 配置**
   - `WebSocketConfig.java` - WebSocket 配置类
   - `WebSocketEventListener.java` - WebSocket 事件监听器
   - `WebSocketInfoController.java` - WebSocket 信息控制器

2. **推送服务**
   - `WebSocketRetryService.java` - 推送服务接口
   - `WebSocketRetryServiceImpl.java` - 推送服务实现
   - `WebSocketAckController.java` - ACK 确认控制器

3. **Feign Client**
   - `NotificationClient.java` - Notification 服务客户端
   - `NotificationClientFallback.java` - 降级处理

4. **MQ 消费者**
   - `MessagePushEventConsumer.java` - 消息推送事件消费者（现在由 Notification 服务消费）

#### 保留的组件

1. **MQ 生产者**
   - `MessagePushEventProducer.java` - 发送推送任务到 MQ

2. **业务逻辑**
   - `ContactServiceImpl.java` - 消息业务逻辑（修改为通过 MQ 发送推送任务）

#### 代码变更示例

**重构前**:
```java
// ContactServiceImpl.java
private final NotificationClient notificationClient;

// 直接调用 Notification 服务推送
notificationClient.pushMessage(receiverId, messageData);
```

**重构后**:
```java
// ContactServiceImpl.java
private final MessagePushEventProducer messagePushEventProducer;

// 通过 MQ 发送推送任务
messagePushEventProducer.sendMessagePushEvent(
    receiverId, 
    messageId, 
    messageData, 
    "MESSAGE_NEW"
);
```

### 2. Notification 服务变更

#### 新增的组件

1. **MQ 消费者**
   - `MessagePushEventConsumer.java` - 消费 Message 服务发送的推送任务

2. **RabbitMQ 配置**
   - 添加消息推送事件队列和交换机配置
   - 添加 `websocketPushTaskExecutor` 线程池

#### 代码示例

```java
// MessagePushEventConsumer.java
@RabbitListener(queues = RabbitMQConfig.MESSAGE_PUSH_QUEUE)
public void handleMessagePushEvent(MessagePushEvent event) {
    // 将推送任务提交到正确的线程池执行（有 WebSocket 上下文）
    websocketPushTaskExecutor.execute(() -> {
        // 根据消息类型调用不同的推送方法
        if ("MESSAGE_NEW".equals(messageType)) {
            notificationService.pushMessage(receiverId, messageData);
        } else if ("UNREAD_COUNT_UPDATE".equals(messageType)) {
            notificationService.pushUnreadCountUpdate(receiverId, unreadCount);
        } else {
            notificationService.pushGenericMessage(receiverId, messageData, messageType);
        }
    });
}
```

### 3. 数据流

#### 消息推送流程

```
1. 用户发送消息
   ↓
2. Message 服务处理业务逻辑（保存消息、更新会话等）
   ↓
3. Message 服务通过 MQ 发送推送任务
   MessagePushEventProducer.sendMessagePushEvent()
   ↓
4. RabbitMQ 消息队列
   Exchange: message.push.exchange
   Queue: message.push.queue
   ↓
5. Notification 服务消费 MQ 消息
   MessagePushEventConsumer.handleMessagePushEvent()
   ↓
6. Notification 服务执行推送
   WebSocketRetryService.pushWithRetry()
   ↓
7. 通过 WebSocket 推送到前端
   messagingTemplate.convertAndSendToUser()
   ↓
8. 前端接收消息并发送 ACK
   ↓
9. Notification 服务处理 ACK
   WebSocketAckController.handleAck()
```

#### 支持的消息类型

- `MESSAGE_NEW` - 新消息推送
- `UNREAD_COUNT_UPDATE` - 未读数更新
- `CONVERSATION_RESTORED` - 会话恢复通知
- `MESSAGE_READ` - 已读回执

---

## 技术实现

### 1. RabbitMQ 配置

#### Message 服务（生产者）

```java
// RabbitMQConfig.java
public static final String MESSAGE_PUSH_EXCHANGE = "message.push.exchange";
public static final String MESSAGE_PUSH_QUEUE = "message.push.queue";
public static final String MESSAGE_PUSH_ROUTING_KEY = "message.push.#";

@Bean
public TopicExchange messagePushExchange() {
    return new TopicExchange(MESSAGE_PUSH_EXCHANGE, true, false);
}

@Bean
public Queue messagePushQueue() {
    return QueueBuilder.durable(MESSAGE_PUSH_QUEUE).build();
}
```

#### Notification 服务（消费者）

```java
// RabbitMQConfig.java
// 共享相同的 Exchange 和 Queue 配置
@Bean
public TopicExchange messagePushExchange() {
    return new TopicExchange(MESSAGE_PUSH_EXCHANGE, true, false);
}

@Bean
public Queue messagePushQueue() {
    return QueueBuilder.durable(MESSAGE_PUSH_QUEUE).build();
}

@Bean
public Binding messagePushBinding() {
    return BindingBuilder
        .bind(messagePushQueue())
        .to(messagePushExchange())
        .with(MESSAGE_PUSH_ROUTING_KEY);
}
```

### 2. 线程池配置

#### 关键问题

RabbitMQ 消费者线程缺少 WebSocket 上下文，导致 `convertAndSendToUser` 无法找到用户会话。

#### 解决方案

使用 `ThreadPoolTaskExecutor` 将推送任务提交到正确的线程池执行：

```java
@Bean(name = "websocketPushTaskExecutor")
public ThreadPoolTaskExecutor websocketPushTaskExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(5);
    executor.setMaxPoolSize(20);
    executor.setQueueCapacity(100);
    executor.setThreadNamePrefix("websocket-push-");
    executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
    executor.initialize();
    return executor;
}
```

### 3. 消息去重机制

#### Redis 去重

```java
// 防止 RabbitMQ 消息重复消费
String processedKey = "message:event:processed:" + event.getMessageId();
Boolean alreadyProcessed = redisTemplate.hasKey(processedKey);

if (Boolean.TRUE.equals(alreadyProcessed)) {
    return; // 已处理，跳过
}

redisTemplate.opsForValue().set(processedKey, "1", Duration.ofHours(24));
```

### 4. ACK 确认机制

#### 前端发送 ACK

```javascript
// websocket.js
sendAck(messageId, messageType) {
    const ackData = {
        messageId: messageId,
        messageType: messageType
    };
    this.stompClient.publish({
        destination: '/app/ack',
        body: JSON.stringify(ackData)
    });
}
```

#### 后端处理 ACK

```java
// WebSocketAckController.java
@PostMapping("/ack")
public Result handleAck(@RequestBody AckRequest request) {
    webSocketRetryService.handleAck(
        request.getMessageId(),
        request.getMessageType()
    );
    return Result.ok("ACK 确认成功");
}
```

---

## 使用指南

### 1. 消息推送

#### Message 服务发送推送任务

```java
// ContactServiceImpl.java
messagePushEventProducer.sendMessagePushEvent(
    receiverId,        // 接收者 ID
    messageId,         // 消息 ID（用于 ACK 确认）
    messageData,       // 消息数据
    "MESSAGE_NEW"      // 消息类型
);
```

#### Notification 服务自动处理

Notification 服务会自动消费 MQ 消息并推送到前端，无需额外配置。

### 2. 监控和调试

#### 查看 MQ 消息

访问 RabbitMQ 管理界面：http://localhost:15672

- 查看队列 `message.push.queue` 的消息数量
- 查看消息内容
- 监控消息消费速率

#### 查看日志

```bash
# Message 服务日志
grep "消息推送任务已发送到MQ" logs/message-service.log

# Notification 服务日志
grep "消息推送事件处理完成" logs/notification-service.log
grep "WebSocket推送尝试" logs/notification-service.log
```

### 3. 故障排查

#### 推送失败

1. **检查 MQ 连接**
   - 确认 RabbitMQ 服务正常运行
   - 检查队列是否存在
   - 查看队列中的消息数量

2. **检查 WebSocket 连接**
   - 确认前端已连接到 Notification 服务的 WebSocket
   - 查看浏览器控制台的 WebSocket 连接状态
   - 检查 Gateway 路由配置

3. **检查用户 ID 匹配**
   - 确认推送的 `receiverId` 与 WebSocket 连接时的 `userId` 一致
   - 查看 Notification 服务的日志，确认用户是否在线

#### 消息重复

1. **检查 Redis 去重**
   - 查看 Redis 中的 `message:event:processed:*` 键
   - 确认去重机制正常工作

2. **检查 ACK 确认**
   - 查看前端是否正常发送 ACK
   - 查看后端是否正常处理 ACK

---

## 技术总结

### 完成情况

✅ **已完成**:
- Message 服务推送功能彻底移除
- Notification 服务统一管理所有推送
- 通过 MQ 实现服务间解耦
- 解决 WebSocket 连接竞争问题
- 推送成功率从 67% 提升至接近 100%
- 完整的代码清理和文档

### 技术亮点

1. **架构清晰**: Message 服务只负责业务逻辑，Notification 服务统一管理推送
2. **解耦彻底**: 通过 MQ 实现服务间异步通信，避免直接依赖
3. **问题根治**: 彻底解决 WebSocket 连接竞争问题，而不是临时修复
4. **历史清理**: 清理了从单体项目时期遗留的架构问题

### 学习价值

1. **微服务拆分**: 了解如何正确拆分微服务，避免功能重复
2. **WebSocket 架构**: 理解 WebSocket 在微服务架构中的正确使用方式
3. **消息队列**: 学习如何使用 MQ 实现服务间解耦
4. **问题诊断**: 掌握如何诊断和解决复杂的架构问题

### 经验教训

1. **拆分要彻底**: 从单体项目拆分微服务时，要彻底清理重复功能
2. **统一管理**: 相同类型的功能（如推送）应该统一管理，避免分散
3. **及时发现问题**: 问题可能隐藏很久，需要深入分析才能发现根源
4. **架构设计**: 好的架构设计可以避免很多问题，重构要彻底

---

## 后续版本规划

### v2.3.0 规划（未来）

#### 数据库迁移
- ⏸️ 从 MySQL 迁移到 PostgreSQL
- ⏸️ 数据迁移脚本
- ⏸️ 性能优化

#### 地址管理
- ⏸️ 引入地址管理功能
- ⏸️ 用户收货地址管理
- ⏸️ 地址选择组件

#### 智能搜索
- ⏸️ 集成 Elasticsearch
- ⏸️ 全文搜索功能
- ⏸️ 搜索建议和自动完成

#### AI 集成
- ⏸️ 引入 Spring AI
- ⏸️ 智能推荐
- ⏸️ 智能客服

---

## 参考资源

### WebSocket
- [Spring WebSocket 文档](https://docs.spring.io/spring-framework/reference/web/websocket.html)
- [STOMP 协议规范](https://stomp.github.io/)

### RabbitMQ
- [RabbitMQ 官方文档](https://www.rabbitmq.com/documentation.html)
- [Spring AMQP 文档](https://docs.spring.io/spring-amqp/docs/current/reference/html/)

### 相关文档
- [PROJECT_DOCUMENTATION_V2.2.0.md](./PROJECT_DOCUMENTATION_V2.2.0.md) - RabbitMQ 集成文档
- [WEBSOCKET_PUSH_COMPARISON.md](./WEBSOCKET_PUSH_COMPARISON.md) - WebSocket 推送对比分析

---

**版本**: v2.2.1  
**状态**: ✅ **已完成**  
**日期**: 2025-11-15

---

## 版本历史

- **v2.1.0** (2025-11-11): Actuator 监控、Docker 容器化、Swagger API 文档 ✅ **已完成**
- **v2.1.1** (2025-11-12): Resilience4j 熔断降级、Config Server 配置中心、环境隔离 ✅ **已完成**
- **v2.1.2** (2025-11-13): Prometheus + Grafana 监控集成 ✅ **已完成**
- **v2.2.0** (2025-11-15): Zipkin 追踪修复、RabbitMQ 消息队列集成 ✅ **已完成**
- **v2.2.1** (2025-11-15): WebSocket 推送架构重构 ✅ **已完成**

