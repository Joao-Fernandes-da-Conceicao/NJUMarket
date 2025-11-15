# Zipkin Feign Client 追踪上下文传播修复方案

## 问题总结

**现象**：
- 卖家查询订单有 Zipkin 信息
- Feign Client 查询买家头像昵称也有 Zipkin 信息
- 时间上包含关系（同一请求）
- **但二者不成链，前者看不到后者**

**根本原因**：
Feign Client 调用创建了**新的独立 Trace**，而不是使用当前请求的 Trace，导致追踪上下文没有正确传播。

## 解决方案

### 方案 1：验证 Spring Cloud OpenFeign 追踪支持（推荐）

**Spring Cloud OpenFeign 3.x 默认支持 Micrometer Tracing**，但需要确保：

1. **依赖已添加**（✅ 已确认）：
```xml
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-tracing</artifactId>
</dependency>
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-tracing-bridge-brave</artifactId>
</dependency>
<dependency>
    <groupId>io.zipkin.reporter2</groupId>
    <artifactId>zipkin-reporter-brave</artifactId>
</dependency>
```

2. **配置已设置**（✅ 已确认）：
```yaml
management:
  tracing:
    sampling:
      probability: 1.0
  zipkin:
    tracing:
      endpoint: http://zipkin:9411/api/v2/spans
```

3. **检查 Spring Cloud 版本**：
- Spring Cloud 2023.0.3 (Spring Boot 3.2.0)
- 应该自动支持 Feign Client 追踪

### 方案 2：添加追踪日志验证（诊断用）

在 `OrderServiceImpl.getOrderDetail()` 方法中添加追踪日志：

```java
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import lombok.RequiredArgsConstructor;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {
    
    private final Tracer tracer;  // 添加 Tracer 注入
    
    // ... 其他依赖 ...
    
    @Override
    public Result getOrderDetail(String orderId) {
        // 获取当前追踪上下文
        Span currentSpan = tracer.currentSpan();
        String traceId = currentSpan != null ? currentSpan.context().traceId() : "null";
        String spanId = currentSpan != null ? currentSpan.context().spanId() : "null";
        
        log.info("【追踪】开始查询订单详情，Trace ID: {}, Span ID: {}", traceId, spanId);
        
        // ... 原有代码 ...
        
        // 在 Feign Client 调用前
        log.info("【追踪】准备调用 Feign Client getUserProfilesByIds，当前 Trace ID: {}", 
            tracer.currentSpan() != null ? tracer.currentSpan().context().traceId() : "null");
        
        Result buyerProfilesResult = authClient.getUserProfilesByIds(buyerIds);
        
        // 在 Feign Client 调用后
        log.info("【追踪】Feign Client 调用完成，当前 Trace ID: {}", 
            tracer.currentSpan() != null ? tracer.currentSpan().context().traceId() : "null");
        
        // ...
    }
}
```

**验证步骤**：
1. 执行一次查询订单操作
2. 查看日志，确认 Trace ID 是否一致
3. 在 Zipkin UI 中搜索该 Trace ID，查看是否在同一个 Trace 中

### 方案 3：检查 Feign Client 配置

**确认 `@EnableFeignClients` 配置**：
```java
@EnableFeignClients(basePackages = "com.njumarket")
public class OrderServiceApplication {
    // ...
}
```

**检查是否有自定义 Feign Client 配置干扰追踪**：
- 查看是否有自定义的 `RequestInterceptor`
- 查看是否有自定义的 `FeignClientConfiguration`

### 方案 4：显式配置 Feign Client 追踪（如果需要）

如果方案 1-3 都无法解决问题，可以尝试显式配置：

**创建 Feign Client 配置类**：
```java
package com.njumarket.order.config;

import org.springframework.cloud.openfeign.FeignClientBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FeignTracingConfig {
    
    // Spring Cloud OpenFeign 3.x 默认启用追踪
    // 如果追踪不工作，可能需要检查是否有其他配置干扰
    // 通常不需要手动配置
}
```

## 诊断步骤

### 步骤 1：验证追踪是否工作

1. **在 Zipkin UI 中查看**：
   - 打开 `http://localhost:9411`
   - 搜索 `njumarket-service-order`
   - 查看是否有 `getOrderDetail` 相关的 Trace

2. **查看 Trace 详情**：
   - 点击一个 Trace
   - 查看是否有 `Auth Service` 的调用
   - 如果没有，说明 Feign Client 调用没有被追踪

### 步骤 2：添加追踪日志

按照方案 2 添加追踪日志，验证追踪上下文是否正确传播。

### 步骤 3：检查日志输出

执行一次查询订单操作，查看日志：
- Trace ID 是否一致
- 是否有追踪相关的错误日志

### 步骤 4：验证 Feign Client 调用

在 Auth Service 中添加日志：
```java
@GetMapping("/user/profile/batch")
public Result getUserProfilesByIds(@RequestParam List<String> userIds) {
    log.info("【追踪】收到批量查询用户档案请求，用户数量: {}", userIds.size());
    // 检查请求头中是否有追踪相关的 Header
    // ...
}
```

## 可能的原因

### 原因 1：Spring Cloud 版本问题

**检查**：
- Spring Cloud 2023.0.3 应该支持 Feign Client 追踪
- 如果版本不兼容，可能需要升级

### 原因 2：追踪上下文丢失

**可能的情况**：
- 异步调用导致上下文丢失
- 线程切换导致上下文丢失
- 某些中间件干扰追踪上下文传播

### 原因 3：配置问题

**检查**：
- 追踪采样率是否正确
- Zipkin 端点配置是否正确
- 是否有其他配置干扰追踪

## 推荐操作

1. **首先尝试方案 2**：添加追踪日志，验证问题
2. **在 Zipkin UI 中验证**：查看调用链是否完整
3. **如果问题仍然存在**：检查 Spring Cloud 版本和配置
4. **最后考虑方案 4**：显式配置 Feign Client 追踪

## 预期结果

**修复后应该看到**：
```
Gateway (8080)
  └─ Order Service (8093) - getOrderDetail()
      └─ Auth Service (8091) - getUserProfilesByIds()
```

**在 Zipkin UI 中**：
- 同一个 Trace 中包含所有调用
- 调用链完整显示
- 时间线正确显示调用顺序

## 日期

2025-11-13

