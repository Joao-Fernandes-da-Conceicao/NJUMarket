# NJUMarket 项目 50x 和 40x 错误分析

## 分析依据
基于 v1.x 总结文档和项目架构，从三个角度分析可能的错误场景。

---

## 一、业务流转角度分析

### 1.1 订单创建流程

**流转路径**：
```
前端 → Gateway → Order Service → Commodity Service (Feign) → Redis (分布式锁) → MySQL
```

**可能的 50x 错误**：
1. **Redis 连接失败** (500)
   - 场景：分布式锁获取时 Redis 不可用
   - 影响：订单创建失败
   - 位置：`OrderServiceImpl.createOrder()` → `redisLockUtil.tryLock()`

2. **Feign 调用超时** (500)
   - 场景：Commodity Service 响应超时或不可用
   - 影响：无法获取商品信息，订单创建失败
   - 位置：`OrderServiceImpl.createOrder()` → `commodityClient.getCommodityForUpdate()`

3. **数据库连接池耗尽** (500)
   - 场景：高并发下数据库连接不足
   - 影响：订单创建失败
   - 位置：`orderRepository.save()`

4. **事务回滚异常** (500)
   - 场景：库存更新失败但事务回滚时异常
   - 影响：数据不一致风险
   - 位置：`@Transactional` 方法

**可能的 40x 错误**：
1. **商品不存在** (400/404)
   - 场景：商品ID无效或商品已删除
   - 影响：订单创建失败
   - 位置：`CommodityValidator.requireCommodity()`

2. **库存不足** (400)
   - 场景：库存检查失败
   - 影响：订单创建失败
   - 位置：`OrderServiceImpl.createOrder()` 库存检查

3. **价格不匹配** (400)
   - 场景：前端价格与后端计算不一致
   - 影响：订单创建失败
   - 位置：`OrderServiceImpl.createOrder()` 价格验证

### 1.2 商品上架流程

**流转路径**：
```
前端 → Gateway → Commodity Service → Notification Service (Feign) → Redis (变更记录) → MySQL
```

**可能的 50x 错误**：
1. **Notification Service 不可用** (500)
   - 场景：推送服务宕机或网络问题
   - 影响：商品变更记录失败，但商品上架成功（已捕获异常）
   - 位置：`CommodityServiceImpl.shelfCommodity()` → `notificationClient.recordCommodityChange()`

2. **Redis 写入失败** (500)
   - 场景：Redis 连接失败或内存不足
   - 影响：变更记录丢失，但业务操作成功
   - 位置：`ChangeRecordServiceImpl.recordCommodityChange()`

**可能的 40x 错误**：
1. **商品状态不正确** (400)
   - 场景：商品不是 PUBLISHED 状态
   - 影响：上架失败
   - 位置：`CommodityValidator.requireCommodityStatus()`

2. **权限不足** (403)
   - 场景：非商品所有者尝试上架
   - 影响：上架失败
   - 位置：`CommodityValidator.requireCommodityOwner()`

### 1.3 消息发送流程

**流转路径**：
```
前端 → Gateway → Message Service → Notification Service (Feign) → WebSocket推送 → Redis (离线队列)
```

**可能的 50x 错误**：
1. **WebSocket 连接丢失** (500)
   - 场景：用户离线但推送服务尝试推送
   - 影响：消息进入离线队列（已处理）
   - 位置：`WebSocketRetryServiceImpl.pushWithRetry()`

2. **Notification Service 不可用** (500)
   - 场景：推送服务宕机
   - 影响：消息发送成功但推送失败（已捕获异常）
   - 位置：`MessageServiceImpl.sendMessage()` → `notificationClient.pushMessage()`

**可能的 40x 错误**：
1. **会话不存在** (404)
   - 场景：会话ID无效
   - 影响：消息发送失败
   - 位置：`MessageServiceImpl.sendMessage()` → `ConversationRepository.findById()`

2. **权限不足** (403)
   - 场景：非会话参与者尝试发送消息
   - 影响：消息发送失败
   - 位置：`MessageServiceImpl.sendMessage()` 权限检查

---

## 二、HTTP 普通请求流转角度分析

### 2.1 Gateway 层问题

**流转路径**：
```
前端 → Gateway (JWT验证) → 后端服务
```

**可能的 50x 错误**：
1. **Redis 连接失败** (500)
   - 场景：Gateway 无法连接 Redis 验证 Token
   - 影响：所有需要认证的请求失败
   - 位置：`JwtAuthenticationFilter.filter()` → `reactiveStringRedisTemplate.opsForValue().get()`

2. **JWT 解析异常** (500)
   - 场景：Token 格式错误导致解析失败
   - 影响：认证失败
   - 位置：`JwtUtils.validateToken()` 或 `JwtUtils.getUserIdFromToken()`

3. **服务发现失败** (500)
   - 场景：Eureka 不可用或服务未注册
   - 影响：请求无法路由到后端服务
   - 位置：Gateway 路由配置

**可能的 40x 错误**：
1. **Token 缺失** (401)
   - 场景：请求头缺少 Authorization
   - 影响：认证失败
   - 位置：`JwtAuthenticationFilter.filter()` → `getTokenFromRequest()`

2. **Token 无效** (401)
   - 场景：Token 过期或签名错误
   - 影响：认证失败
   - 位置：`JwtUtils.validateToken()`

3. **Token 不匹配** (401)
   - 场景：Redis 中的 Token 与请求中的不一致（多设备登录）
   - 影响：认证失败
   - 位置：`JwtAuthenticationFilter.filter()` Token 比较

4. **路由不匹配** (404)
   - 场景：请求路径不在 Gateway 路由配置中
   - 影响：请求无法路由
   - 位置：Gateway 路由配置

### 2.2 后端服务层问题

**流转路径**：
```
Gateway → 后端服务 (UserContextFilter) → Controller → Service → Repository → MySQL
```

**可能的 50x 错误**：
1. **X-User-Id 缺失** (500)
   - 场景：Gateway 未正确传递 X-User-Id（但 Gateway 已处理，理论上不会发生）
   - 影响：SecurityContext 设置失败
   - 位置：`UserContextFilter.doFilterInternal()` → `SecurityUtils.requireCurrentUser()`

2. **数据库连接失败** (500)
   - 场景：MySQL 连接池耗尽或数据库不可用
   - 影响：所有数据库操作失败
   - 位置：Repository 方法调用

3. **Feign 调用异常** (500)
   - 场景：服务间调用超时或目标服务不可用
   - 影响：业务逻辑失败
   - 位置：所有 Feign Client 调用

4. **序列化/反序列化异常** (500)
   - 场景：DTO 转换失败或 JSON 解析错误
   - 影响：请求处理失败
   - 位置：`ObjectMapper.convertValue()` 或 Feign 响应解析

**可能的 40x 错误**：
1. **参数验证失败** (400)
   - 场景：Bean Validation 验证失败
   - 影响：请求被拒绝
   - 位置：Controller 方法参数 `@Valid` 注解

2. **资源不存在** (404)
   - 场景：查询的资源不存在
   - 影响：返回 404
   - 位置：Service 层 `orElseThrow(() -> new BusinessException("资源不存在"))`

3. **权限不足** (403)
   - 场景：`@PreAuthorize` 权限检查失败
   - 影响：请求被拒绝
   - 位置：Controller 方法 `@PreAuthorize` 注解

### 2.3 管理端请求流转

**流转路径**：
```
前端 → Gateway (Admin JWT验证) → Admin Service (AdminContextFilter) → Controller
```

**可能的 50x 错误**：
1. **反射调用失败** (500)
   - 场景：`AdminContextFilter` 中反射创建 `auth.entity.Admin` 失败
   - 影响：Admin SecurityContext 设置失败
   - 位置：`AdminContextFilter.convertToAuthAdmin()`

2. **类型转换异常** (500)
   - 场景：`admin.entity.Admin` 与 `auth.entity.Admin` 字段不匹配
   - 影响：UserHolder 设置失败
   - 位置：`AdminContextFilter.setField()`

**可能的 40x 错误**：
1. **Admin Token 无效** (401)
   - 场景：Admin JWT Token 过期或无效
   - 影响：管理端请求失败
   - 位置：`AdminAuthenticationFilter.filter()`

---

## 三、WebSocket 流转角度分析

### 3.1 WebSocket 连接建立

**流转路径**：
```
前端 → Gateway (JWT验证) → Notification Service → WebSocketHandshakeInterceptor → WebSocketChannelInterceptor
```

**可能的 50x 错误**：
1. **Gateway WebSocket 路由失败** (500)
   - 场景：Gateway 不支持 WebSocket 升级或路由配置错误
   - 影响：WebSocket 连接失败
   - 位置：Gateway WebSocket 路由配置

2. **X-User-Id 缺失** (500)
   - 场景：Gateway 未正确传递 X-User-Id（但 Gateway 已处理）
   - 影响：WebSocket 握手失败
   - 位置：`WebSocketHandshakeInterceptor.beforeHandshake()` → 返回 false

3. **Principal 设置失败** (500)
   - 场景：`WebSocketChannelInterceptor` 设置 Principal 时异常
   - 影响：WebSocket 连接建立但无法识别用户
   - 位置：`WebSocketChannelInterceptor.preSend()`

**可能的 40x 错误**：
1. **JWT Token 无效** (401)
   - 场景：WebSocket 握手请求的 Token 无效
   - 影响：连接被拒绝
   - 位置：Gateway `JwtAuthenticationFilter`

2. **X-User-Id 缺失** (401)
   - 场景：握手拦截器未找到 X-User-Id
   - 影响：连接被拒绝
   - 位置：`WebSocketHandshakeInterceptor.beforeHandshake()` → 返回 false

### 3.2 WebSocket 消息推送

**流转路径**：
```
业务服务 → Notification Service → WebSocketRetryService → SimpMessagingTemplate → WebSocket客户端
```

**可能的 50x 错误**：
1. **用户不在线** (500)
   - 场景：用户离线，推送失败（但已进入离线队列，不是真正的错误）
   - 影响：消息进入重试队列
   - 位置：`WebSocketRetryServiceImpl.pushWithRetry()` → `webSocketEventListener.isUserOnline()`

2. **Redis 连接失败** (500)
   - 场景：离线队列写入 Redis 失败
   - 影响：离线消息丢失
   - 位置：`WebSocketRetryServiceImpl.addToRetryQueue()`

3. **消息序列化失败** (500)
   - 场景：消息对象无法序列化为 JSON
   - 影响：推送失败
   - 位置：`ObjectMapper.writeValueAsString()`

4. **重试队列处理异常** (500)
   - 场景：定时任务处理重试队列时异常
   - 影响：离线消息无法重试
   - 位置：`WebSocketRetryServiceImpl.retryFailedMessages()`

**可能的 40x 错误**：
1. **用户ID无效** (400)
   - 场景：推送目标用户ID不存在
   - 影响：推送失败（但不会返回 40x，而是静默失败）
   - 位置：`SimpMessagingTemplate.convertAndSendToUser()`

### 3.3 WebSocket 路由问题

**当前架构问题**：
- **问题**：WebSocket 路由配置指向了已迁移的服务
  - `/api/ws/order/**` → `order-service` (但 order-service 已删除 WebSocket)
  - `/api/ws/**` → `message-service` (但应该统一到 notification-service)

**可能的 50x 错误**：
1. **服务不存在** (500)
   - 场景：order-service 或 message-service 的 WebSocket 端点不存在
   - 影响：WebSocket 连接失败
   - 位置：Gateway 路由配置

2. **路由配置错误** (500)
   - 场景：StripPrefix 配置错误导致路径不匹配
   - 影响：WebSocket 连接失败
   - 位置：Gateway WebSocket 路由配置

---

## 四、关键问题总结

### 4.1 高优先级问题（可能导致系统不可用）

1. **Gateway Redis 连接失败** → 所有认证请求失败
2. **数据库连接池耗尽** → 所有数据库操作失败
3. **Eureka 服务发现失败** → 服务间调用失败
4. **WebSocket 路由配置错误** → WebSocket 功能完全不可用（迁移后）

### 4.2 中优先级问题（影响部分功能）

1. **Feign 调用超时** → 服务间调用失败
2. **Notification Service 不可用** → 推送功能失败（但业务操作成功）
3. **Redis 变更记录失败** → 增量轮询功能受影响

### 4.3 低优先级问题（已处理或影响较小）

1. **推送失败进入离线队列** → 已处理，不影响业务
2. **参数验证失败** → 正常业务校验，返回 400
3. **资源不存在** → 正常业务逻辑，返回 404

---

## 五、建议的改进措施

### 5.1 错误处理增强

1. **Feign 调用添加降级策略**
   - 使用 Hystrix 或 Resilience4j 实现熔断降级
   - 避免级联故障

2. **Redis 连接失败处理**
   - 添加重试机制
   - 降级到数据库查询（如果可能）

3. **数据库连接池监控**
   - 添加连接池监控
   - 及时告警

### 5.2 WebSocket 路由修复

1. **更新 Gateway 路由配置**
   - 将所有 WebSocket 路由指向 `notification-service`
   - 删除指向 `order-service` 和 `message-service` 的 WebSocket 路由

2. **统一 WebSocket 端点**
   - 通知服务统一管理所有 WebSocket 连接
   - 前端统一订阅 `/user/queue/notification`

### 5.3 监控和告警

1. **添加健康检查**
   - 各服务健康检查端点
   - Gateway 健康检查

2. **错误日志聚合**
   - 统一错误日志格式
   - 错误率监控

---

**文档版本**：v1.0  
**创建日期**：2025-01-XX  
**分析范围**：业务流转、HTTP 请求流转、WebSocket 流转

