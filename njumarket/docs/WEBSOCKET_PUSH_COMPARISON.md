# WebSocket推送机制对比分析

## 消息推送 vs 订单推送

### 1. 调用链路对比

#### 消息推送（Message Service）
```
ContactServiceImpl.sendMessage()
  ↓ (事务内，同步)
webSocketRetryService.pushWithRetry()  [Message Service]
  ↓
WebSocketRetryServiceImpl.pushWithRetry()
  ↓
✅ 已优化：先推送，再检查在线状态
```

#### 订单推送（Order Service → Notification Service）
```
OrderServiceImpl.xxx() (如payOrder, shipOrder等)
  ↓ (事务内，同步)
notificationClient.pushOrderChange()  [Feign Client，跨服务调用]
  ↓ (HTTP请求，可能有网络延迟)
NotificationServiceImpl.pushOrderChange()
  ↓
webSocketRetryService.pushWithRetry()  [Notification Service]
  ↓
WebSocketRetryServiceImpl.pushWithRetry()
  ↓
❌ 未优化：先检查在线状态，再推送
```

### 2. 关键差异

| 特性 | 消息推送 | 订单推送 |
|------|---------|---------|
| **调用方式** | 本地服务调用 | Feign跨服务调用 |
| **执行位置** | 事务内 | 事务内（但通过Feign） |
| **推送逻辑** | ✅ 先推送，再检查在线状态 | ❌ 先检查在线状态，再推送 |
| **性能影响** | 已优化，减少误判 | 未优化，可能误判用户不在线 |
| **数据库查询** | 有（未读数查询） | 无 |
| **网络开销** | 无 | 有（Feign HTTP调用） |

### 3. 性能问题分析

#### 消息推送慢的原因：
1. ✅ **已优化**：推送逻辑已改为先推送再检查
2. ⚠️ **仍有问题**：在事务内执行，有数据库查询（未读数）
3. ⚠️ **仍有问题**：推送前有两次数据库查询（`getTotalUnreadCount` 和 `getUnreadCountForUser`）

#### 订单推送快的原因：
1. ❌ **未优化但可能更快**：虽然先检查在线状态，但Feign调用可能因为网络延迟而感觉更快（异步感）
2. ✅ **无数据库查询**：推送前没有额外的数据库查询
3. ✅ **跨服务调用**：Feign调用可能被Spring Cloud的线程池异步处理

### 4. 优化建议

#### 4.1 统一推送逻辑（高优先级）
**问题**：notification服务的 `WebSocketRetryServiceImpl` 还是旧逻辑（先检查在线状态）

**解决方案**：将notification服务的推送逻辑也改为先推送再检查在线状态

#### 4.2 消息推送进一步优化（中优先级）
**问题**：消息推送在事务内执行，有数据库查询

**解决方案**：
1. 将未读数查询移到推送之后（已做）
2. 考虑将未读数更新改为异步推送（使用 `@Async`）
3. 或者将未读数查询移到事务提交后（使用 `@TransactionalEventListener`）

#### 4.3 订单推送优化（低优先级）
**问题**：Feign调用是同步的，可能阻塞事务

**解决方案**：
1. 考虑使用 `@Async` 异步调用Feign Client
2. 或者将推送操作移到事务提交后

### 5. 推荐优化顺序

1. **立即优化**：统一notification服务的推送逻辑（先推送再检查）
2. **后续优化**：将消息推送的未读数更新改为异步
3. **可选优化**：将订单推送改为异步（如果Feign调用确实阻塞）

