# Zipkin 调用链与批量查询机制分析

## 问题描述

通过 Zipkin 发现调用链很短，主要是 `gateway -> service-x` 的链条。用户想知道：
1. 这是否和批量查询机制有关？
2. 通过批量查询引用别的 client 会加入 Zipkin 的链条吗？

## 批量查询机制分析

### 1. 批量查询的实现方式

项目中存在多处批量查询实现：

#### Order Service 批量查询
```java
// OrderServiceImpl.java
@Override
public Result getOrdersBatchStatus(List<String> orderIds) {
    // ... 批量查询订单
    
    // ✅ 批量查询profile（使用Feign Client）
    if (!userIds.isEmpty()) {
        Result profileResult = authClient.getUserProfilesByIds(new ArrayList<>(userIds));
        // ...
    }
}
```

#### Commodity Service 批量查询
```java
// CommodityQueryServiceImpl.java
@Override
public Result getCommoditiesBatchStatus(List<String> commodityIds) {
    // ... 批量查询商品
    
    // ✅ 批量查询所有卖家的 Profile（避免 N+1 查询）
    if (!sellerIds.isEmpty()) {
        Result profilesResult = authClient.getUserProfilesByIds(new ArrayList<>(sellerIds));
        // ...
    }
}
```

### 2. Feign Client 调用追踪

**关键点**：批量查询通过 Feign Client 调用其他服务时，**应该会被 Zipkin 追踪**。

#### Feign Client 定义
```java
@FeignClient(name = "njumarket-service-auth", path = "/api/internal")
public interface AuthClient {
    @GetMapping("/user/profile/batch")
    Result getUserProfilesByIds(@RequestParam List<String> userIds);
}
```

#### 追踪机制
- Spring Cloud OpenFeign **默认支持 Micrometer Tracing**
- 只要配置了 Zipkin，Feign Client 的调用**会被自动追踪**
- 调用链应该显示：`Gateway -> Order Service -> Auth Service`

## 为什么调用链看起来短？

### 1. 批量查询减少了调用次数

**对比**：
- **不使用批量查询**：如果有 10 个订单，需要调用 Auth Service 10 次
  - Zipkin 中会显示 10 个 `Order Service -> Auth Service` 的调用
- **使用批量查询**：10 个订单只需要调用 Auth Service 1 次
  - Zipkin 中只显示 1 个 `Order Service -> Auth Service` 的调用

**影响**：
- 调用链的**深度**不变（仍然是 `Gateway -> Order Service -> Auth Service`）
- 但调用链的**数量**减少了（从 N 条变成 1 条）

### 2. 调用链应该存在

**正确的调用链应该是**：
```
Gateway (8080)
  └─ Order Service (8093)
      └─ Auth Service (8091)  ← 批量查询调用
```

**如果只看到**：
```
Gateway (8080)
  └─ Order Service (8093)
```

**可能的原因**：
1. **Feign Client 调用确实被追踪了，但可能因为**：
   - 批量查询的调用时间很短，在 Zipkin UI 中可能不明显
   - 需要展开查看完整的调用链
   - 采样率设置问题（虽然设置了 100%，但可能还有其他因素）

2. **配置问题**：
   - Feign Client 的追踪可能没有正确配置
   - 需要检查 Feign Client 是否启用了追踪

3. **调用时机**：
   - 批量查询可能在某些情况下没有被触发
   - 或者批量查询的结果被缓存了，没有实际调用

## 验证方法

### 1. 在 Zipkin UI 中查看完整调用链

**步骤**：
1. 打开 Zipkin UI：`http://localhost:9411`
2. 选择一个 `gateway -> order-service` 的 Trace
3. **展开查看**：点击 Trace 查看详细信息
4. 应该能看到：
   ```
   Gateway HTTP GET /api/user/order/batch-status
   └─ Order Service HTTP POST /api/user/order/batch-status
       └─ Order Service 内部处理
           └─ Auth Service HTTP GET /api/internal/user/profile/batch  ← 应该在这里
   ```

### 2. 检查 Feign Client 追踪配置

**确认配置**：
- ✅ Micrometer Tracing 依赖已添加
- ✅ Zipkin 配置正确
- ✅ Feign Client 默认支持追踪（无需额外配置）

**验证代码**：
```java
// 在 OrderServiceImpl 中，这个调用应该被追踪
Result profileResult = authClient.getUserProfilesByIds(new ArrayList<>(userIds));
```

### 3. 添加日志验证

**在批量查询方法中添加日志**：
```java
@Override
public Result getOrdersBatchStatus(List<String> orderIds) {
    log.info("开始批量查询订单状态，订单数量: {}", orderIds.size());
    
    // ... 批量查询订单
    
    if (!userIds.isEmpty()) {
        log.info("开始批量查询用户档案，用户数量: {}", userIds.size());
        Result profileResult = authClient.getUserProfilesByIds(new ArrayList<>(userIds));
        log.info("批量查询用户档案完成，结果: {}", profileResult.getSuccess());
    }
}
```

**在 Auth Service 的批量查询接口中添加日志**：
```java
@GetMapping("/user/profile/batch")
public Result getUserProfilesByIds(@RequestParam List<String> userIds) {
    log.info("收到批量查询用户档案请求，用户数量: {}", userIds.size());
    // ...
}
```

### 4. 使用 Actuator 验证

**查看追踪配置**：
```bash
curl http://localhost:8093/actuator/configprops | grep -i tracing
```

**查看追踪端点**：
```bash
curl http://localhost:8093/actuator/trace
```

## 结论

### 1. 批量查询机制的影响

**✅ 批量查询会减少调用次数**：
- 从 N 次单独调用变成 1 次批量调用
- 调用链的**数量**减少，但**深度**不变

**✅ Feign Client 调用应该被追踪**：
- Spring Cloud OpenFeign 默认支持 Micrometer Tracing
- 批量查询通过 Feign Client 调用其他服务时，**应该会被 Zipkin 追踪**
- 调用链应该显示：`Gateway -> Order Service -> Auth Service`

### 2. 如果调用链确实很短

**可能的原因**：
1. **批量查询的调用时间很短**，在 Zipkin UI 中可能不明显
2. **需要展开查看完整的调用链**，不要只看顶层
3. **某些情况下批量查询没有被触发**（比如数据已经在缓存中）
4. **Feign Client 追踪配置问题**（虽然默认支持，但可能需要检查）

### 3. 建议

1. **在 Zipkin UI 中展开查看完整的调用链**
2. **添加日志验证批量查询是否被调用**
3. **检查 Feign Client 的追踪配置**
4. **使用 Actuator 验证追踪配置**

## 示例：完整的调用链

**理想的调用链应该是**：
```
Gateway (8080)
  └─ HTTP POST /api/user/order/batch-status
      └─ Order Service (8093)
          └─ OrderServiceImpl.getOrdersBatchStatus()
              └─ Auth Service (8091)  ← 批量查询调用
                  └─ InternalController.getUserProfilesByIds()
                      └─ 数据库查询
```

**如果这个调用链存在，说明批量查询的 Feign Client 调用已经被正确追踪。**

## 日期

2025-11-13

