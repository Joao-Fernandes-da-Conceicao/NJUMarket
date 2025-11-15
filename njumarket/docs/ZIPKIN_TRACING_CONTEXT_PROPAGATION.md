# Zipkin 追踪上下文传播问题分析

## 问题描述

**现象**：
1. 卖家查询卖家订单有 Zipkin 信息
2. 在这个服务中使用 Feign Client 查询买家头像昵称也有 Zipkin 信息
3. 前者在时间上包含后者（说明是同一个请求）
4. **但是二者不成链，前者也看不到后者**

**示例代码**：
```java
// OrderServiceImpl.getOrderDetail()
// 1. 主请求：卖家查询订单
Result orderResult = getOrderDetail(orderId);  // ✅ 有 Zipkin 信息

// 2. 在同一个方法中调用 Feign Client
Result buyerProfilesResult = authClient.getUserProfilesByIds(buyerIds);  // ✅ 有 Zipkin 信息
// ❌ 但这两个调用不成链
```

## 问题分析

### 1. 分布式追踪上下文传播机制

**正常的调用链应该是**：
```
Gateway (8080)
  └─ Order Service (8093) - getOrderDetail()
      └─ Auth Service (8091) - getUserProfilesByIds()
```

**当前的问题**：
- Order Service 的主请求有 Zipkin 信息
- Auth Service 的 Feign Client 调用也有 Zipkin 信息
- 但这两个调用**不在同一个 Trace 中**，而是**两个独立的 Trace**

### 2. 可能的原因

#### 原因 1：Feign Client 追踪上下文未正确传播

**Spring Cloud OpenFeign 默认支持 Micrometer Tracing**，但需要确保：
1. 依赖正确配置
2. 追踪上下文正确传播
3. 没有禁用追踪功能

#### 原因 2：追踪采样问题

虽然配置了 100% 采样率，但可能存在：
- 某些调用没有被采样
- 追踪上下文在传播过程中丢失

#### 原因 3：异步调用导致上下文丢失

如果 Feign Client 调用是异步的，追踪上下文可能丢失。

## 解决方案

### 方案 1：检查 Feign Client 追踪配置

**确认依赖**：
```xml
<!-- Micrometer Tracing + Zipkin 分布式链路追踪 -->
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

**确认配置**：
```yaml
management:
  tracing:
    sampling:
      probability: 1.0  # 100% 采样率
  zipkin:
    tracing:
      endpoint: http://zipkin:9411/api/v2/spans
```

### 方案 2：显式启用 Feign Client 追踪

**检查 `@EnableFeignClients` 配置**：
```java
@SpringBootApplication
@EnableFeignClients(basePackages = "com.njumarket")
public class OrderServiceApplication {
    // ...
}
```

**确保 Feign Client 自动配置启用**：
- Spring Cloud OpenFeign 3.x 默认启用追踪
- 如果追踪不工作，可能需要显式配置

### 方案 3：添加 Feign Client 追踪拦截器（如果需要）

**创建 Feign Client 追踪拦截器**：
```java
package com.njumarket.order.config;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class FeignTracingInterceptor implements RequestInterceptor {
    
    private final Tracer tracer;
    
    @Override
    public void apply(RequestTemplate template) {
        Span currentSpan = tracer.currentSpan();
        if (currentSpan != null) {
            // 确保追踪上下文传播到 Feign Client 请求
            log.debug("Feign Client 追踪拦截器: 当前 Span ID={}, Trace ID={}", 
                currentSpan.context().spanId(), 
                currentSpan.context().traceId());
        } else {
            log.warn("Feign Client 追踪拦截器: 当前没有活动的 Span");
        }
    }
}
```

**配置 Feign Client 使用拦截器**：
```java
@FeignClient(
    name = "njumarket-service-auth", 
    path = "/api/internal",
    configuration = FeignClientConfig.class  // 添加配置类
)
public interface AuthClient {
    // ...
}
```

**配置类**：
```java
@Configuration
public class FeignClientConfig {
    
    @Bean
    public RequestInterceptor feignTracingInterceptor(Tracer tracer) {
        return new FeignTracingInterceptor(tracer);
    }
}
```

### 方案 4：检查追踪上下文传播

**添加日志验证追踪上下文**：
```java
// OrderServiceImpl.java
@Override
public Result getOrderDetail(String orderId) {
    // 获取当前追踪上下文
    Span currentSpan = tracer.currentSpan();
    if (currentSpan != null) {
        log.info("当前 Trace ID: {}, Span ID: {}", 
            currentSpan.context().traceId(), 
            currentSpan.context().spanId());
    }
    
    // ... 原有代码 ...
    
    // 在 Feign Client 调用前
    log.info("准备调用 Feign Client，当前 Trace ID: {}", 
        tracer.currentSpan() != null ? tracer.currentSpan().context().traceId() : "null");
    
    Result buyerProfilesResult = authClient.getUserProfilesByIds(buyerIds);
    
    // 在 Feign Client 调用后
    log.info("Feign Client 调用完成，当前 Trace ID: {}", 
        tracer.currentSpan() != null ? tracer.currentSpan().context().traceId() : "null");
    
    // ...
}
```

### 方案 5：检查 Spring Cloud 版本兼容性

**确认 Spring Cloud 版本**：
- Spring Cloud 2023.0.3 (Spring Boot 3.2.0)
- Spring Cloud OpenFeign 应该自动支持 Micrometer Tracing

**如果版本不兼容**，可能需要：
1. 升级 Spring Cloud 版本
2. 或手动配置追踪支持

## 验证方法

### 1. 在 Zipkin UI 中查看

**步骤**：
1. 打开 Zipkin UI：`http://localhost:9411`
2. 搜索 `njumarket-service-order` 的 Trace
3. 选择一个 Trace，查看详细信息
4. **应该看到**：
   ```
   Gateway HTTP GET /api/user/order/{orderId}
   └─ Order Service HTTP GET /api/user/order/{orderId}
       └─ Auth Service HTTP GET /api/internal/user/profile/batch  ← 应该在这里
   ```

**如果看不到**：
- 说明 Feign Client 调用没有被追踪
- 或者追踪上下文没有正确传播

### 2. 检查日志

**添加追踪日志**：
```java
@Slf4j
@Service
public class OrderServiceImpl implements OrderService {
    
    private final Tracer tracer;
    
    @Override
    public Result getOrderDetail(String orderId) {
        Span span = tracer.nextSpan().name("getOrderDetail").start();
        try (Tracer.SpanInScope ws = tracer.withSpan(span)) {
            log.info("开始查询订单详情，Trace ID: {}", span.context().traceId());
            
            // ... 原有代码 ...
            
            // Feign Client 调用
            log.info("调用 Feign Client 前，Trace ID: {}", span.context().traceId());
            Result buyerProfilesResult = authClient.getUserProfilesByIds(buyerIds);
            log.info("调用 Feign Client 后，Trace ID: {}", span.context().traceId());
            
            return Result.ok("获取订单详情成功", orderDTO);
        } finally {
            span.end();
        }
    }
}
```

### 3. 使用 Actuator 验证

**查看追踪配置**：
```bash
curl http://localhost:8093/actuator/configprops | grep -i tracing
```

**查看追踪端点**（如果启用）：
```bash
curl http://localhost:8093/actuator/trace
```

## 常见问题

### Q1: 为什么 Feign Client 调用有 Zipkin 信息，但不成链？

**A**: 可能是因为：
1. **追踪上下文没有正确传播**：Feign Client 调用创建了新的 Trace，而不是使用当前的 Trace
2. **采样问题**：虽然两个调用都被采样，但可能在不同的 Trace 中
3. **配置问题**：Feign Client 的追踪配置可能不正确

### Q2: 如何确保 Feign Client 调用在同一个 Trace 中？

**A**: 
1. **确保依赖正确**：Micrometer Tracing 和 Zipkin Reporter 依赖已添加
2. **确保配置正确**：追踪采样率设置为 1.0
3. **检查 Spring Cloud 版本**：确保版本兼容
4. **添加追踪拦截器**（如果需要）：显式传播追踪上下文

### Q3: 是否需要手动配置 Feign Client 追踪？

**A**: 
- **Spring Cloud OpenFeign 3.x 默认支持 Micrometer Tracing**
- 通常不需要手动配置
- 如果追踪不工作，可能需要检查配置或添加拦截器

## 推荐解决方案

### 步骤 1：验证当前配置

1. 确认依赖已添加
2. 确认配置正确
3. 在 Zipkin UI 中查看调用链

### 步骤 2：添加追踪日志

在 `OrderServiceImpl.getOrderDetail()` 方法中添加追踪日志，验证追踪上下文是否正确传播。

### 步骤 3：如果问题仍然存在

1. **检查 Spring Cloud 版本兼容性**
2. **添加 Feign Client 追踪拦截器**（方案 3）
3. **检查是否有其他配置干扰追踪**

## 日期

2025-11-13

