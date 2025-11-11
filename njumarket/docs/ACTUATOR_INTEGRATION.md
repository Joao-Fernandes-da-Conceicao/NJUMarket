# Spring Boot Actuator 集成文档

## 概述

本文档说明如何在 NJUMarket 微服务项目中集成和使用 Spring Boot Actuator。

## 集成状态

✅ **已完成** - 所有服务已集成 Spring Boot Actuator

## 已集成的服务

- ✅ njumarket-discovery (8761)
- ✅ njumarket-gateway (8080)
- ✅ njumarket-service-auth (8091)
- ✅ njumarket-service-commodity (8092)
- ✅ njumarket-service-order (8093)
- ✅ njumarket-service-message (8094)
- ✅ njumarket-service-image (8095)
- ✅ njumarket-service-admin (8096)
- ✅ njumarket-service-notification (8097)

## 配置说明

### 1. Maven 依赖

所有服务的 `pom.xml` 中已添加：

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
```

### 2. application.yml 配置

所有服务的 `application.yml` 中已添加以下配置：

```yaml
# Actuator 配置
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics
      base-path: /actuator
  endpoint:
    health:
      show-details: when-authorized
  info:
    env:
      enabled: true
    java:
      enabled: true
    os:
      enabled: true
```

### 3. Security 配置

所有服务的 `SecurityConfig` 已配置允许访问 Actuator 端点：

- `auth-service`: 明确允许 `/actuator/**`
- 其他服务: 使用 `anyRequest().permitAll()`，已包含 Actuator 端点

## 可用的端点

### 健康检查端点

**URL**: `http://localhost:{port}/actuator/health`

**示例**:
```bash
# Gateway 健康检查
curl http://localhost:8080/actuator/health

# Auth Service 健康检查
curl http://localhost:8091/actuator/health
```

**响应示例**:
```json
{
  "status": "UP"
}
```

### 应用信息端点

**URL**: `http://localhost:{port}/actuator/info`

**示例**:
```bash
curl http://localhost:8091/actuator/info
```

**响应示例**:
```json
{
  "app": {
    "name": "njumarket-service-auth",
    "version": "0.0.1-SNAPSHOT"
  },
  "java": {
    "version": "17.0.x"
  },
  "os": {
    "name": "Windows 10",
    "arch": "amd64"
  }
}
```

### 指标端点

**URL**: `http://localhost:{port}/actuator/metrics`

**示例**:
```bash
# 获取所有可用指标
curl http://localhost:8091/actuator/metrics

# 获取特定指标（如 JVM 内存使用）
curl http://localhost:8091/actuator/metrics/jvm.memory.used
```

## 使用场景

### 1. 服务健康监控

定期检查服务健康状态，用于：
- 负载均衡器健康检查
- 服务注册中心健康状态
- 监控系统告警

### 2. 性能指标收集

收集应用性能指标，用于：
- JVM 内存使用情况
- HTTP 请求统计
- 数据库连接池状态
- 自定义业务指标

### 3. 应用信息查询

查询应用基本信息，用于：
- 版本管理
- 环境信息
- 部署信息

## 安全建议

当前配置中，Actuator 端点对所有请求开放。在生产环境中，建议：

1. **限制访问**：只允许内网访问
2. **添加认证**：使用 Spring Security 保护敏感端点
3. **隐藏敏感信息**：配置 `show-details: never` 或 `when-authorized`

### 生产环境配置示例

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics
      base-path: /actuator
  endpoint:
    health:
      show-details: never  # 生产环境隐藏详细信息
  info:
    env:
      enabled: false  # 生产环境禁用环境信息
```

## 下一步

1. **集成 Prometheus**：将 Actuator 指标导出到 Prometheus
2. **集成 Grafana**：使用 Grafana 可视化监控指标
3. **自定义健康检查**：为数据库、Redis 等添加自定义健康检查

## 参考资源

- [Spring Boot Actuator 官方文档](https://docs.spring.io/spring-boot/docs/current/reference/html/actuator.html)
- [Actuator 端点列表](https://docs.spring.io/spring-boot/docs/current/reference/html/actuator.html#actuator.endpoints)

