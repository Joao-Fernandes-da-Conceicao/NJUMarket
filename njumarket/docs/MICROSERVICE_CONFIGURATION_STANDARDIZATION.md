# 微服务配置规范化文档

## 📋 概述

本文档记录了从单体项目迁移到微服务项目时的配置规范化修复工作，确保所有服务的配置统一、规范。

## ✅ 已修复的配置问题

### 1. Redis 配置统一

**问题**：`order-service` 缺少 Redis 配置，而其他服务都有。

**修复**：
- ✅ 为 `order-service` 添加了 Redis 配置
- ✅ 统一了所有服务的 Redis 配置格式
- ✅ 使用环境变量支持不同环境的配置

**配置示例**：
```yaml
spring:
  data:
    redis:
      host: ${REDIS_HOST:localhost}
      port: ${REDIS_PORT:6379}
      password: ${REDIS_PASSWORD:hqz20050316}
      database: ${ORDER_REDIS_DATABASE:4}  # 每个服务使用不同的数据库
```

**各服务的 Redis 数据库分配**：
- `auth-service`: database 2
- `commodity-service`: database 2
- `message-service`: database 3
- `order-service`: database 4
- `gateway`: database 2

### 2. Feign Client 配置统一

**问题**：不同服务的 Feign Client 配置不一致，有些服务缺少配置。

**修复**：
- ✅ 为所有服务添加了统一的 Feign Client 配置
- ✅ 统一了超时时间设置
- ✅ 统一启用了请求/响应压缩

**标准配置**：
```yaml
feign:
  client:
    config:
      default:
        connectTimeout: 5000  # 连接超时（毫秒）
        readTimeout: 10000     # 读取超时（毫秒）
      # 可以针对特定服务配置不同的超时时间
      njumarket-service-auth:
        connectTimeout: 3000
        readTimeout: 5000
  compression:
    request:
      enabled: true
    response:
      enabled: true
```

### 3. 日志配置统一

**问题**：不同服务的日志配置不统一，有些服务缺少关键日志配置。

**修复**：
- ✅ 统一了所有服务的日志配置格式
- ✅ 为所有服务添加了 `com.njumarket.njumarket.resolver: INFO` 日志级别（用于 `CurrentUserArgumentResolver`）
- ✅ 为所有服务添加了 service、controller、filter 的日志配置

**标准配置**：
```yaml
logging:
  level:
    com.njumarket.{service}.client: DEBUG      # Feign Client 调用日志
    com.njumarket.{service}.filter: INFO        # Filter 日志
    com.njumarket.{service}.service: INFO       # Service 日志
    com.njumarket.{service}.controller: INFO    # Controller 日志
    com.njumarket.njumarket.resolver: INFO      # 参数解析器日志
```

### 4. WebMvcConfig 配置

**问题**：`CurrentUserArgumentResolver` 需要显式注册到 `WebMvcConfigurer` 中才能工作。

**修复**：
- ✅ 为所有使用 `@CurrentUser` 注解的服务创建了 `WebMvcConfig`
- ✅ 统一了 `WebMvcConfig` 的实现方式

**标准实现**：
```java
@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {
    private final CurrentUserArgumentResolver currentUserArgumentResolver;

    @Override
    public void addArgumentResolvers(@NonNull List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(currentUserArgumentResolver);
    }
}
```

**已创建 WebMvcConfig 的服务**：
- ✅ `auth-service`
- ✅ `commodity-service`
- ✅ `order-service`
- ✅ `message-service`

## 📊 配置对比表

| 配置项 | auth-service | commodity-service | order-service | message-service |
|--------|--------------|-------------------|---------------|-----------------|
| Redis 配置 | ✅ | ✅ | ✅ (已修复) | ✅ |
| Feign Client 配置 | ✅ (已修复) | ✅ | ✅ | ✅ |
| 日志配置 | ✅ (已修复) | ✅ | ✅ (已修复) | ✅ |
| WebMvcConfig | ✅ (已修复) | ✅ (已修复) | ✅ (已修复) | ✅ (已修复) |

## 🔍 配置规范检查清单

### 必需配置项

每个微服务都应该包含以下配置：

1. **服务基本信息**
   - `server.port`: 服务端口
   - `spring.application.name`: 服务名称

2. **数据库配置**
   - `spring.datasource.*`: 数据源配置
   - `spring.jpa.*`: JPA 配置

3. **Redis 配置**（如果服务使用 Redis）
   - `spring.data.redis.*`: Redis 连接配置
   - 使用环境变量支持不同环境

4. **Eureka 配置**
   - `eureka.client.service-url.defaultZone`: Eureka 服务地址

5. **Feign Client 配置**（如果服务调用其他服务）
   - `feign.client.config.*`: 超时配置
   - `feign.compression.*`: 压缩配置

6. **日志配置**
   - `logging.level.*`: 日志级别配置
   - 至少包含：service、controller、filter、resolver

7. **WebMvcConfig**（如果使用 `@CurrentUser` 注解）
   - 注册 `CurrentUserArgumentResolver`

## 🚀 后续优化建议

### 1. 配置中心

建议使用配置中心（如 Nacos、Apollo）统一管理配置，避免配置分散。

### 2. 环境变量标准化

所有敏感配置（密码、密钥等）都应该使用环境变量，避免硬编码。

### 3. 配置验证

在服务启动时验证必需配置是否存在，避免运行时错误。

### 4. 配置文档

为每个服务维护配置文档，说明每个配置项的作用和默认值。

## ✅ 内部接口路径规范

**问题**：检查所有服务的内部接口路径是否统一。

**检查结果**：
- ✅ 所有服务的 `InternalController` 都使用 `/api/internal` 路径
- ✅ 所有 Feign Client 调用内部接口时都使用 `/api/internal` 路径
- ✅ 例外：`CommodityQueryClient` 使用 `/api/public` 路径（合理，因为查询公开商品信息）

**规范**：
- 内部接口（服务间调用）：`/api/internal`
- 公开接口（前端调用）：`/api/public` 或 `/api/user`
- 管理接口：`/api/admin`

## ✅ 服务间调用规范

**检查结果**：
- ✅ 所有服务都使用 Feign Client 进行服务间调用
- ✅ 没有发现直接注入其他服务 Repository 的情况
- ✅ 所有服务都使用内部 DTO（`UserInternalDTO`、`CommodityInternalDTO`、`OrderInternalDTO`）进行数据传输

**规范**：
1. 服务间调用必须使用 Feign Client
2. 禁止直接注入其他服务的 Repository 或 Service
3. 服务间传输数据必须使用内部 DTO，不能直接传输 Entity

## 📝 相关文档

- [微服务架构文档](./MICROSERVICES_ARCHITECTURE.md)
- [Feign Client 使用指南](./FEIGN_CLIENT_MIGRATION_GUIDE.md)
- [安全重构总结](./SECURITY_REFACTORING_SUMMARY.md)

