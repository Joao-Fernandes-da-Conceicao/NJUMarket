# NJUMarket 微服务改进需求分析

## 📋 概述

本文档分析了项目中未落实的1.x功能以及需要补充的微服务标准实践，帮助项目更贴合正常微服务项目的逻辑。

---

## 🔴 1.x功能未落实问题

### 1. Service层过度依赖UserHolder（高优先级）

**问题描述**：
- 文档显示已完成，但实际代码中仍有**39处**使用`UserHolder.getUser()`
- 影响所有Service实现类，降低可测试性和耦合度

**当前状态**：
- ✅ 文档中标记为"已完成"
- ❌ 实际代码中仍有大量使用

**影响范围**：
- `OrderServiceImpl`: 4处
- `CommodityQueryServiceImpl`: 2处
- `UserServiceImpl`: 2处
- `UserProfileServiceImpl`: 1处
- `ContactServiceImpl`: 1处
- `AdminServiceImpl`: 5处
- 其他Service类

**改进建议**：
```java
// ❌ 当前方式（不推荐）
public Result createOrder(OrderDTO orderDTO) {
    User currentUser = UserHolder.getUser();  // 依赖ThreadLocal
    if (currentUser == null) {
        throw new BusinessException("用户未登录");
    }
    // ...
}

// ✅ 改进方式（推荐）
public Result createOrder(String userId, OrderDTO orderDTO) {
    User currentUser = BusinessValidator.requireUser(userId, userRepository);
    // ...
}

// Controller层
@PostMapping("/create")
public Result createOrder(@CurrentUser User user, @Valid @RequestBody OrderDTO dto) {
    return orderService.createOrder(user.getUserId(), dto);
}
```

**工作量**：中等（需要修改所有Service接口和Controller）

---

### 2. GlobalExceptionHandler缺失（高优先级）

**问题描述**：
- 文档中多次提到`GlobalExceptionHandler`，但实际代码中**不存在**
- 各服务缺少统一异常处理机制

**当前状态**：
- ✅ 文档中提到已实现
- ❌ 实际代码中不存在

**影响**：
- 异常处理不统一
- 无法统一处理`BusinessException`、`MethodArgumentNotValidException`等
- 错误响应格式不一致

**改进建议**：
在`njumarket-common`中创建`GlobalExceptionHandler`，各服务自动继承：

```java
package com.njumarket.njumarket.exception;

import com.njumarket.njumarket.dto.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理业务异常
     */
    @ExceptionHandler(BusinessException.class)
    @ResponseStatus(HttpStatus.OK)
    public Result handleBusinessException(BusinessException e) {
        log.warn("业务异常: {}", e.getMessage());
        return Result.fail(e.getMessage());
    }

    /**
     * 处理参数验证异常（@RequestBody）
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        log.warn("参数验证失败: {}", message);
        return Result.fail("参数验证失败: " + message);
    }

    /**
     * 处理参数验证异常（@RequestParam, @PathVariable）
     */
    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result handleConstraintViolationException(ConstraintViolationException e) {
        String message = e.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.joining(", "));
        log.warn("参数验证失败: {}", message);
        return Result.fail("参数验证失败: " + message);
    }

    /**
     * 处理绑定异常
     */
    @ExceptionHandler(BindException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result handleBindException(BindException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        log.warn("参数绑定失败: {}", message);
        return Result.fail("参数绑定失败: " + message);
    }

    /**
     * 处理系统异常
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result handleException(Exception e) {
        log.error("系统异常", e);
        return Result.fail("系统错误，请稍后重试");
    }
}
```

**工作量**：低（创建文件即可，各服务自动生效）

---

## 🟡 微服务标准实践缺失

### 3. 健康检查和监控缺失（高优先级）

**问题描述**：
- 缺少Spring Boot Actuator配置
- 无法监控服务健康状态
- 无法收集性能指标

**改进建议**：

#### 3.1 添加Actuator依赖

在`njumarket-common/pom.xml`中添加：
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
```

#### 3.2 配置Actuator端点

在各服务的`application.yml`中添加：
```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  endpoint:
    health:
      show-details: when-authorized
  metrics:
    export:
      prometheus:
        enabled: true
```

#### 3.3 健康检查端点

- `/actuator/health` - 健康检查
- `/actuator/info` - 服务信息
- `/actuator/metrics` - 性能指标
- `/actuator/prometheus` - Prometheus指标

**工作量**：低

---

### 4. Feign Client缺少超时和重试配置（中优先级）

**问题描述**：
- Feign Client没有配置超时时间
- 没有重试机制
- 服务调用失败时无法自动恢复

**改进建议**：

在各服务的`application.yml`中添加：
```yaml
feign:
  client:
    config:
      default:
        connectTimeout: 5000  # 连接超时（毫秒）
        readTimeout: 10000     # 读取超时（毫秒）
        retryer:
          period: 100          # 重试间隔（毫秒）
          maxPeriod: 1000      # 最大重试间隔
          maxAttempts: 3       # 最大重试次数
      njumarket-service-auth:
        connectTimeout: 3000
        readTimeout: 5000
      njumarket-service-commodity:
        connectTimeout: 3000
        readTimeout: 5000
      njumarket-service-order:
        connectTimeout: 3000
        readTimeout: 5000
      njumarket-service-message:
        connectTimeout: 3000
        readTimeout: 5000
  compression:
    request:
      enabled: true
    response:
      enabled: true
  logging:
    level:
      com.njumarket: DEBUG
```

**工作量**：低

---

### 5. 服务降级和熔断缺失（中优先级）

**问题描述**：
- 没有服务降级机制
- 没有熔断保护
- 服务调用失败时无法优雅降级

**改进建议**：

#### 5.1 使用Resilience4j实现熔断

在`njumarket-common/pom.xml`中添加：
```xml
<dependency>
    <groupId>io.github.resilience4j</groupId>
    <artifactId>resilience4j-spring-boot3</artifactId>
    <version>2.1.0</version>
</dependency>
<dependency>
    <groupId>io.github.resilience4j</groupId>
    <artifactId>resilience4j-feign</artifactId>
    <version>2.1.0</version>
</dependency>
```

#### 5.2 配置熔断器

在`application.yml`中添加：
```yaml
resilience4j:
  circuitbreaker:
    configs:
      default:
        slidingWindowSize: 10
        minimumNumberOfCalls: 5
        permittedNumberOfCallsInHalfOpenState: 3
        automaticTransitionFromOpenToHalfOpenEnabled: true
        waitDurationInOpenState: 10s
        failureRateThreshold: 50
        eventConsumerBufferSize: 10
    instances:
      authService:
        baseConfig: default
      commodityService:
        baseConfig: default
```

#### 5.3 创建降级类

```java
@Component
public class AuthClientFallback implements AuthClient {
    @Override
    public Result getUserById(String userId) {
        return Result.fail("认证服务暂时不可用，请稍后重试");
    }
}

@FeignClient(name = "njumarket-service-auth", 
             path = "/api/internal",
             fallback = AuthClientFallback.class)
public interface AuthClient {
    // ...
}
```

**工作量**：中等

---

### 6. API限流缺失（中优先级）

**问题描述**：
- Gateway没有配置限流
- 无法防止恶意请求
- 无法保护后端服务

**改进建议**：

#### 6.1 Gateway限流配置

在`njumarket-gateway/application.yml`中添加：
```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: auth-service
          filters:
            - name: RequestRateLimiter
              args:
                redis-rate-limiter.replenishRate: 10  # 每秒允许的请求数
                redis-rate-limiter.burstCapacity: 20  # 突发容量
                redis-rate-limiter.requestedTokens: 1  # 每次请求消耗的令牌数
```

#### 6.2 添加Redis限流依赖

在`njumarket-gateway/pom.xml`中添加：
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis-reactive</artifactId>
</dependency>
```

**工作量**：低

---

### 7. 链路追踪缺失（低优先级）

**问题描述**：
- 无法追踪跨服务调用链路
- 无法分析性能瓶颈
- 无法定位问题

**改进建议**：

使用Sleuth + Zipkin实现链路追踪：

#### 7.1 添加依赖

在`njumarket-common/pom.xml`中添加：
```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-sleuth</artifactId>
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

#### 7.2 配置Zipkin

在`application.yml`中添加：
```yaml
spring:
  sleuth:
    zipkin:
      base-url: http://localhost:9411
    sampler:
      probability: 1.0  # 采样率（生产环境建议0.1）
```

**工作量**：中等

---

### 8. 配置中心缺失（低优先级）

**问题描述**：
- 配置分散在各个服务的`application.yml`中
- 修改配置需要重启服务
- 无法动态更新配置

**改进建议**：

使用Spring Cloud Config Server实现配置中心：

#### 8.1 创建Config Server模块

创建`njumarket-config-server`模块，统一管理配置

#### 8.2 配置Git仓库

将配置文件存储在Git仓库中，Config Server从Git读取

**工作量**：高（需要创建新模块）

---

### 9. 服务间调用错误处理不统一（中优先级）

**问题描述**：
- Feign Client调用失败时处理方式不统一
- 缺少统一的错误处理逻辑

**改进建议**：

#### 9.1 创建Feign错误解码器

```java
@Component
public class FeignErrorDecoder implements ErrorDecoder {
    @Override
    public Exception decode(String methodKey, Response response) {
        if (response.status() >= 400 && response.status() < 500) {
            return new BusinessException("客户端错误: " + response.status());
        }
        if (response.status() >= 500) {
            return new BusinessException("服务端错误: " + response.status());
        }
        return new Exception("未知错误: " + response.status());
    }
}
```

#### 9.2 配置Feign错误解码器

在`application.yml`中添加：
```yaml
feign:
  client:
    config:
      default:
        errorDecoder: com.njumarket.njumarket.feign.FeignErrorDecoder
```

**工作量**：低

---

## 📊 优先级总结

### 🔴 高优先级（立即实施）

1. **Service层减少UserHolder依赖** - 39处使用，影响可测试性
2. **创建GlobalExceptionHandler** - 统一异常处理，各服务缺失
3. **添加健康检查和监控** - 生产环境必需

### 🟡 中优先级（近期实施）

4. **Feign Client超时和重试配置** - 提升服务调用稳定性
5. **服务降级和熔断** - 提升系统容错能力
6. **API限流** - 保护后端服务
7. **服务间调用错误处理统一** - 提升错误处理一致性

### 🟢 低优先级（按需实施）

8. **链路追踪** - 便于问题定位和性能分析
9. **配置中心** - 统一配置管理

---

## 🎯 实施建议

### 第一阶段（1周）

1. 创建`GlobalExceptionHandler`（1天）
2. 添加Actuator健康检查（1天）
3. 配置Feign超时和重试（1天）
4. 配置Gateway限流（1天）

### 第二阶段（2周）

5. 实现服务降级和熔断（3天）
6. 统一Feign错误处理（2天）
7. 逐步减少UserHolder依赖（5天）

### 第三阶段（按需）

8. 实现链路追踪（可选）
9. 实现配置中心（可选）

---

## 📝 注意事项

1. **渐进式改进**：不要一次性修改所有代码，按优先级逐步实施
2. **充分测试**：每次改进后都要进行充分测试
3. **向后兼容**：确保改进不影响现有功能
4. **文档更新**：及时更新相关文档

---

## 📚 参考文档

- [Spring Boot Actuator文档](https://docs.spring.io/spring-boot/docs/current/reference/html/actuator.html)
- [Resilience4j文档](https://resilience4j.readme.io/)
- [Spring Cloud Gateway限流](https://docs.spring.io/spring-cloud-gateway/docs/current/reference/html/#the-requestratelimiter-gatewayfilter-factory)
- [Spring Cloud Sleuth文档](https://spring.io/projects/spring-cloud-sleuth)

