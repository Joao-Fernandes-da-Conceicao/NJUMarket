# 推送服务（Notification Service）架构设计

## 1. 设计目标

### 1.1 解决的问题
- **职责分离**：将推送功能从业务服务中分离出来
- **统一管理**：所有 WebSocket 连接、推送逻辑、离线队列统一管理
- **解耦业务**：业务服务（order、commodity）只需调用推送服务，不关心推送细节
- **可扩展性**：未来可支持多种推送方式（WebSocket、HTTP Push、邮件、短信等）

### 1.2 核心职责
1. **WebSocket 连接管理**：统一管理所有 WebSocket 连接
2. **消息推送**：接收业务服务的推送请求，推送给指定用户
3. **离线队列**：用户离线时，消息存入队列，上线后自动推送
4. **重试机制**：推送失败时自动重试（指数退避策略）
5. **增量轮询**：提供增量查询接口，支持聊天界面的增量更新

## 2. 服务架构

```
┌─────────────────┐
│  Order Service  │
│  Commodity Svc  │ ──┐
│  Message Svc    │   │
│  (其他业务服务)  │   │
└─────────────────┘   │
                      │ Feign Client
                      ▼
        ┌─────────────────────────┐
        │  Notification Service   │
        │                         │
        │  - WebSocket 管理        │
        │  - 推送服务              │
        │  - 离线队列              │
        │  - 重试机制              │
        │  - 增量轮询              │
        └─────────────────────────┘
                      │
                      ▼
        ┌─────────────────────────┐
        │      Redis / Queue       │
        │  - 离线消息队列          │
        │  - 变更记录              │
        └─────────────────────────┘
```

## 3. 核心功能模块

### 3.1 推送服务接口

```java
public interface NotificationService {
    /**
     * 推送通知给指定用户
     * @param userId 用户ID
     * @param notificationType 通知类型（ORDER_CHANGE, COMMODITY_CHANGE, MESSAGE_NEW等）
     * @param data 通知数据
     */
    void pushNotification(String userId, String notificationType, Map<String, Object> data);
    
    /**
     * 批量推送通知
     */
    void pushNotifications(List<NotificationRequest> requests);
    
    /**
     * 检查用户是否在线
     */
    boolean isUserOnline(String userId);
}
```

### 3.2 增量轮询服务

```java
public interface IncrementalPollService {
    /**
     * 获取增量更新（商品和订单变更）
     * @param lastPollTimestamp 上次轮询时间戳
     * @return 变更的商品和订单列表
     */
    IncrementalUpdateResultVO getIncrementalUpdate(LocalDateTime lastPollTimestamp);
}
```

### 3.3 变更记录服务（从 order 服务迁移）

```java
public interface ChangeRecordService {
    void recordCommodityChange(String commodityId, String operation, LocalDateTime timestamp);
    void recordOrderChange(String orderId, String operation, LocalDateTime timestamp);
    List<String> getCommodityChangesAfter(LocalDateTime afterTimestamp);
    List<String> getOrderChangesAfter(LocalDateTime afterTimestamp);
}
```

## 4. 迁移计划

### 4.1 第一阶段：创建推送服务
1. 创建 `njumarket-service-notification` 模块
2. 迁移 WebSocket 配置和连接管理
3. 迁移 WebSocketRetryService（从 message 服务）
4. 创建统一的推送服务接口

### 4.2 第二阶段：迁移增量轮询
1. 迁移 ChangeRecordService（从 order 服务）
2. 迁移 ChatDataController（从 order 服务）
3. 更新前端调用地址

### 4.3 第三阶段：更新业务服务
1. **Order Service**：
   - 删除 WebSocket 配置
   - 删除 OrderWebSocketService
   - 通过 Feign Client 调用推送服务

2. **Commodity Service**：
   - 删除 MessageClient（改为 NotificationClient）
   - 通过 Feign Client 调用推送服务

3. **Message Service**：
   - 保留聊天消息的 WebSocket（或也迁移到推送服务）
   - 删除 WebSocketRetryService（已迁移）

## 5. 优势

### 5.1 职责清晰
- **业务服务**：专注于业务逻辑（订单、商品、消息）
- **推送服务**：专注于推送功能（WebSocket、离线队列、重试）

### 5.2 易于维护
- 所有推送相关代码集中在一个服务
- 统一的错误处理和日志记录
- 统一的性能监控和优化

### 5.3 可扩展性
- 未来可以轻松添加新的推送方式（HTTP Push、邮件等）
- 可以支持推送优先级、推送策略等高级功能
- 可以统一管理推送统计和分析

### 5.4 解耦
- 业务服务不需要关心 WebSocket 实现细节
- 推送服务可以独立升级和优化
- 降低服务间的耦合度

## 6. 注意事项

### 6.1 性能考虑
- WebSocket 连接管理需要高效（考虑使用 Redis 共享连接状态）
- 离线队列需要合理的大小限制和过期策略
- 重试机制需要避免过度重试

### 6.2 可靠性
- 推送失败不应该影响业务操作
- 需要完善的错误处理和日志记录
- 考虑使用消息队列（如 RabbitMQ）保证消息不丢失

### 6.3 兼容性
- 迁移过程中需要保持向后兼容
- 前端需要逐步迁移到新的推送服务
- 考虑灰度发布策略

## 7. 实施建议

### 7.1 渐进式迁移
1. 先创建推送服务，实现基础功能
2. 逐步迁移各个业务服务的推送功能
3. 最后统一前端调用

### 7.2 保持兼容
- 迁移期间，新旧服务可以并存
- 通过配置开关控制使用哪个服务
- 确保迁移过程中不影响现有功能

### 7.3 测试策略
- 单元测试：推送服务的各个功能模块
- 集成测试：业务服务调用推送服务
- 压力测试：WebSocket 连接数和推送性能

