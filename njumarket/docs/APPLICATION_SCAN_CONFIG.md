# 微服务Application类包扫描配置说明

## 📋 概述

所有微服务都需要正确配置`scanBasePackages`，以便能够加载`njumarket-common`模块中的组件。

---

## ✅ 已配置的服务

### 1. Gateway Service

**文件**：`njumarket-gateway/src/main/java/com/njumarket/gateway/GatewayApplication.java`

**配置**：
```java
@SpringBootApplication(scanBasePackages = {
        "com.njumarket.gateway",
        "com.njumarket.njumarket"  // 扫描common模块，加载JwtUtils等组件
})
```

**依赖的Common组件**：
- `JwtUtils` - JWT工具类（用于JWT认证Filter）
- `ReactiveStringRedisTemplate` - 响应式Redis模板（用于Token验证）

---

### 2. Auth Service

**文件**：`njumarket-service-auth/src/main/java/com/njumarket/auth/AuthServiceApplication.java`

**配置**：
```java
@SpringBootApplication(scanBasePackages = {
        "com.njumarket.auth",
        "com.njumarket.njumarket"
})
```

**依赖的Common组件**：
- `GlobalExceptionHandler` - 全局异常处理器
- `ServiceLogAspect` - AOP日志切面
- `CurrentUserArgumentResolver` - 用户参数解析器
- `CurrentAdminArgumentResolver` - 管理员参数解析器
- `SecurityUtils` - 安全工具类
- `BusinessValidator` - 业务验证器
- `JwtUtils` - JWT工具类

---

### 3. Commodity Service

**文件**：`njumarket-service-commodity/src/main/java/com/njumarket/commodity/CommodityServiceApplication.java`

**配置**：
```java
@SpringBootApplication(scanBasePackages = {
        "com.njumarket.commodity",
        "com.njumarket.njumarket"
})
```

**依赖的Common组件**：
- `GlobalExceptionHandler` - 全局异常处理器
- `ServiceLogAspect` - AOP日志切面
- `SecurityUtils` - 安全工具类
- `BusinessValidator` - 业务验证器

---

### 4. Order Service

**文件**：`njumarket-service-order/src/main/java/com/njumarket/order/OrderServiceApplication.java`

**配置**：
```java
@SpringBootApplication(scanBasePackages = {
        "com.njumarket.order",
        "com.njumarket.njumarket"
})
```

**依赖的Common组件**：
- `GlobalExceptionHandler` - 全局异常处理器
- `ServiceLogAspect` - AOP日志切面
- `SecurityUtils` - 安全工具类
- `BusinessValidator` - 业务验证器

---

### 5. Message Service

**文件**：`njumarket-service-message/src/main/java/com/njumarket/message/MessageServiceApplication.java`

**配置**：
```java
@SpringBootApplication(scanBasePackages = {
        "com.njumarket.message",
        "com.njumarket.njumarket"
})
```

**依赖的Common组件**：
- `GlobalExceptionHandler` - 全局异常处理器
- `ServiceLogAspect` - AOP日志切面
- `SecurityUtils` - 安全工具类
- `BusinessValidator` - 业务验证器
- `WebSocketEventListener` - WebSocket事件监听器

---

### 6. Image Service

**文件**：`njumarket-service-image/src/main/java/com/njumarket/image/ImageServiceApplication.java`

**配置**：
```java
@SpringBootApplication(scanBasePackages = {
        "com.njumarket.image",
        "com.njumarket.njumarket"
})
```

**依赖的Common组件**：
- `GlobalExceptionHandler` - 全局异常处理器
- `ServiceLogAspect` - AOP日志切面

---

### 7. Admin Service

**文件**：`njumarket-service-admin/src/main/java/com/njumarket/admin/AdminServiceApplication.java`

**配置**：
```java
@SpringBootApplication(scanBasePackages = {
        "com.njumarket.admin",
        "com.njumarket.njumarket"
})
```

**依赖的Common组件**：
- `GlobalExceptionHandler` - 全局异常处理器
- `ServiceLogAspect` - AOP日志切面
- `SecurityUtils` - 安全工具类
- `BusinessValidator` - 业务验证器

---

### 8. Discovery Service

**文件**：`njumarket-discovery/src/main/java/com/njumarket/discovery/DiscoveryServerApplication.java`

**配置**：
```java
@SpringBootApplication
@EnableEurekaServer
public class DiscoveryServerApplication {
    // 不需要扫描common模块，Discovery服务不依赖common组件
}
```

**说明**：Discovery服务是Eureka Server，不依赖common模块的组件，因此不需要配置`scanBasePackages`。

---

## 📊 Common模块组件清单

### 异常处理
- `GlobalExceptionHandler` - 全局异常处理器（`@RestControllerAdvice`）

### AOP切面
- `ServiceLogAspect` - Service层日志切面（`@Aspect`）

### 参数解析器
- `CurrentUserArgumentResolver` - 用户参数解析器（`@Component`）
- `CurrentAdminArgumentResolver` - 管理员参数解析器（`@Component`）

### 工具类
- `JwtUtils` - JWT工具类（`@Component`）
- `SecurityUtils` - 安全工具类（静态方法）
- `BusinessValidator` - 业务验证器（静态方法）
- `UserHolder` - 用户持有者（ThreadLocal）
- `RedisConstants` - Redis常量

### WebSocket
- `WebSocketEventListener` - WebSocket事件监听器（`@Component`）

### 实体类
- 所有Entity类（`@Entity`）

### DTO类
- 所有DTO类

### 异常类
- `BusinessException` - 业务异常

### 注解
- `@CurrentUser` - 当前用户注解
- `@CurrentAdmin` - 当前管理员注解

---

## 🔍 验证方法

### 1. 检查Bean是否加载

启动服务后，检查日志中是否有以下信息：
```
Creating shared instance of singleton bean 'jwtUtils'
Creating shared instance of singleton bean 'globalExceptionHandler'
Creating shared instance of singleton bean 'serviceLogAspect'
```

### 2. 检查异常处理

如果`GlobalExceptionHandler`未加载，异常不会被统一处理，会返回Spring默认的错误响应。

### 3. 检查AOP日志

如果`ServiceLogAspect`未加载，Service方法不会输出AOP日志。

### 4. 检查参数解析器

如果`CurrentUserArgumentResolver`未加载，`@CurrentUser`注解无法工作。

---

## ⚠️ 注意事项

1. **Gateway特殊处理**：
   - Gateway使用响应式模式，不能使用Spring MVC组件
   - Gateway排除了`spring-boot-starter-web`依赖
   - Gateway只能使用响应式组件（如`ReactiveStringRedisTemplate`）

2. **包扫描顺序**：
   - 先扫描服务自身包（如`com.njumarket.auth`）
   - 再扫描common包（`com.njumarket.njumarket`）
   - 确保服务自身的配置优先

3. **组件冲突**：
   - 如果服务中有同名组件，服务自身的组件会优先
   - 建议使用不同的包名避免冲突

---

## 📝 配置模板

所有需要依赖common模块的服务都应该使用以下模板：

```java
@SpringBootApplication(scanBasePackages = {
        "com.njumarket.{service-name}",  // 服务自身包
        "com.njumarket.njumarket"         // common模块包
})
@EnableDiscoveryClient
@EnableFeignClients(basePackages = "com.njumarket")
public class {Service}Application {
    public static void main(String[] args) {
        SpringApplication.run({Service}Application.class, args);
    }
}
```

---

## ✅ 验证清单

- [x] Gateway Service - 已配置
- [x] Auth Service - 已配置
- [x] Commodity Service - 已配置
- [x] Order Service - 已配置
- [x] Message Service - 已配置
- [x] Image Service - 已配置
- [x] Admin Service - 已配置
- [x] Discovery Service - 不需要（Eureka Server）

---

## 🔗 相关文档

- [Gateway路由配置迁移](./GATEWAY_ROUTE_MIGRATION.md)
- [架构问题分析](./ARCHITECTURE_ISSUES_ANALYSIS.md)

