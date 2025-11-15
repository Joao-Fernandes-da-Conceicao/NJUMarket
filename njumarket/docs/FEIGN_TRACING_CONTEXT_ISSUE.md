# Feign Client 追踪上下文传播问题诊断

## 问题描述

**现象**：
- Gateway -> Order Service 的调用链正常（在同一个 Trace 中）
- Order Service -> Auth Service 的 Feign Client 调用有 Zipkin 信息
- **但这两个调用不在同一个 Trace 中，而是两个独立的 Trace**
- 追踪上下文没有正确传播

## 当前配置检查

### ✅ 已配置的项目

1. **@EnableFeignClients** ✅
   ```java
   @EnableFeignClients(basePackages = "com.njumarket")
   ```

2. **依赖** ✅
   - `micrometer-tracing`
   - `micrometer-tracing-bridge-brave`
   - `zipkin-reporter-brave`
   - `spring-cloud-starter-openfeign`

3. **配置** ✅
   - `spring.cloud.openfeign.micrometer.enabled: true`
   - `management.tracing.sampling.probability: 1.0`
   - `management.zipkin.tracing.endpoint`

### ⚠️ 可能缺失的依赖

根据 Spring Cloud OpenFeign 和 Micrometer Tracing 的集成要求，可能需要：

1. **`feign-micrometer`** - Feign 与 Micrometer 的集成
2. **`micrometer-observation`** - Micrometer Observation API（Spring Boot 3.x 使用）

## 解决方案

### 方案 1：添加缺失的依赖（推荐）

在 `njumarket-service-order/pom.xml` 中添加：

```xml
<!-- Micrometer Observation（Spring Boot 3.x 需要） -->
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-observation</artifactId>
</dependency>

<!-- Feign Micrometer 集成（如果需要） -->
<dependency>
    <groupId>io.github.openfeign</groupId>
    <artifactId>feign-micrometer</artifactId>
</dependency>
```

**注意**：
- `micrometer-observation` 通常由 Spring Boot 3.x 自动管理，但可能需要显式添加
- `feign-micrometer` 在 Spring Cloud OpenFeign 3.x 中可能已经包含，但显式添加可以确保集成

### 方案 2：检查自动配置

确认 Spring Cloud OpenFeign 的自动配置是否启用：

1. **检查自动配置类**：
   - `FeignClientAutoConfiguration`
   - `FeignTracingAutoConfiguration`（如果存在）

2. **检查 Bean**：
   - `ObservationRegistry` - 应该由 Spring Boot 自动配置
   - `Tracer` - 应该由 Micrometer Tracing 自动配置

### 方案 3：验证配置优先级

确保配置的优先级正确：

1. **系统属性**（最高优先级）
2. **环境变量**
3. **Config Server 配置**
4. **本地 application.yml**

## 诊断步骤

### 步骤 1：检查依赖

```bash
# 在 Order Service 容器中检查依赖
docker exec njumarket-service-order mvn dependency:tree | grep -i micrometer
docker exec njumarket-service-order mvn dependency:tree | grep -i feign
```

### 步骤 2：检查自动配置

```bash
# 查看自动配置报告
curl http://localhost:8093/actuator/conditions
```

### 步骤 3：检查追踪配置

```bash
# 查看追踪配置
curl http://localhost:8093/actuator/configprops | grep -i tracing
curl http://localhost:8093/actuator/configprops | grep -i feign
```

### 步骤 4：检查日志

查看 Order Service 启动日志，确认：
- Feign Client 自动配置是否启用
- Micrometer Tracing 自动配置是否启用
- 是否有追踪相关的错误或警告

## 可能的原因

### 原因 1：依赖缺失

**问题**：缺少 `micrometer-observation` 或 `feign-micrometer` 依赖

**解决**：添加缺失的依赖

### 原因 2：自动配置未启用

**问题**：Spring Cloud OpenFeign 的追踪自动配置未启用

**解决**：检查自动配置条件，确保满足所有条件

### 原因 3：配置冲突

**问题**：某些配置可能禁用了追踪功能

**解决**：检查是否有 `enabled: false` 的配置

### 原因 4：版本兼容性

**问题**：Spring Cloud 版本与 Micrometer Tracing 版本不兼容

**解决**：确保版本兼容

## 推荐操作

1. **首先尝试添加依赖**：
   - 添加 `micrometer-observation`（如果缺失）
   - 添加 `feign-micrometer`（如果缺失）

2. **重启服务并验证**：
   - 重启 Order Service
   - 在 Zipkin UI 中查看调用链是否完整

3. **如果问题仍然存在**：
   - 检查自动配置报告
   - 检查日志中的错误或警告
   - 考虑添加显式的追踪拦截器（如果需要）

## 日期

2025-11-13

