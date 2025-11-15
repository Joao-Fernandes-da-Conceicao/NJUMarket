# Zipkin Feign Client 配置诊断指南

## 概述

本文档提供诊断 Zipkin 在 Feign Client 连接情况下配置是否生效的完整指南。

## 问题现象

- ✅ Gateway -> Service 的调用链正常显示
- ❌ Service -> Service（通过 Feign Client）的调用链缺失
- Feign Client 调用有 Zipkin 信息，但不在同一个 Trace 中

## 诊断步骤

### 步骤 1: 运行诊断脚本

#### Windows (PowerShell)
```powershell
cd njumarket/scripts
.\diagnose-zipkin-feign-config.ps1
```

#### Linux/Mac (Bash)
```bash
cd njumarket/scripts
chmod +x diagnose-zipkin-feign-config.sh
./diagnose-zipkin-feign-config.sh
```

### 步骤 2: 手动检查配置

#### 2.1 检查服务健康状态

```bash
# 检查各个服务的健康状态
curl http://localhost:8093/actuator/health
curl http://localhost:8091/actuator/health
curl http://localhost:8092/actuator/health
```

#### 2.2 检查配置属性

```bash
# 检查 Order Service 的配置
curl http://localhost:8093/actuator/configprops | jq '.contexts.application.beans | to_entries | map(select(.key | contains("openfeign") or contains("zipkin") or contains("tracing")))'

# 检查 Feign Micrometer 配置
curl http://localhost:8093/actuator/configprops | jq '.contexts.application.beans."spring.cloud.openfeign.micrometer"'

# 检查 Zipkin 端点配置
curl http://localhost:8093/actuator/configprops | jq '.contexts.application.beans."management.zipkin.tracing"'
```

#### 2.3 检查追踪采样率

```bash
curl http://localhost:8093/actuator/configprops | jq '.contexts.application.beans."management.tracing.sampling"'
```

### 步骤 3: 检查 Zipkin 服务

```bash
# 检查 Zipkin UI
curl http://localhost:9411

# 检查 Zipkin API
curl http://localhost:9411/api/v2/services
curl http://localhost:9411/api/v2/traces?limit=10
```

### 步骤 4: 验证追踪上下文传播

1. **触发一个包含 Feign Client 调用的请求**
   ```bash
   # 例如：查询订单详情（会调用 Auth Service 获取用户信息）
   curl -H "Authorization: Bearer YOUR_TOKEN" \
        http://localhost:8080/api/user/order/{orderId}
   ```

2. **在 Zipkin UI 中查看追踪**
   - 访问 http://localhost:9411
   - 查看最新的追踪记录
   - 检查是否在同一个 Trace 中看到多个服务调用

3. **验证追踪上下文**
   - ✅ **正确情况**: 一个 Trace 包含多个 Span（Gateway -> Order Service -> Auth Service）
   - ❌ **错误情况**: 多个独立的 Trace（Gateway -> Order Service 一个 Trace，Order Service -> Auth Service 另一个 Trace）

## 配置检查清单

### ✅ 依赖配置（pom.xml）

确保所有服务都包含以下依赖：

```xml
<!-- Micrometer Observation -->
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-observation</artifactId>
</dependency>

<!-- Micrometer Tracing -->
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-tracing</artifactId>
</dependency>

<!-- Micrometer Tracing Bridge (Brave) -->
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-tracing-bridge-brave</artifactId>
</dependency>

<!-- Zipkin Reporter -->
<dependency>
    <groupId>io.zipkin.reporter2</groupId>
    <artifactId>zipkin-reporter-brave</artifactId>
</dependency>

<!-- Spring Cloud OpenFeign -->
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-openfeign</artifactId>
</dependency>
```

### ✅ 配置文件（application.yml 或 Config Server 配置）

确保所有服务都包含以下配置：

```yaml
spring:
  cloud:
    openfeign:
      micrometer:
        enabled: true  # ✅ 关键配置：启用 Feign Micrometer 支持

management:
  tracing:
    sampling:
      probability: 1.0  # 100% 采样率（生产环境建议降低到 0.1）
  zipkin:
    tracing:
      endpoint: http://zipkin:9411/api/v2/spans  # ✅ 正确的配置路径
      connect-timeout: 10000
      read-timeout: 10000
```

### ✅ 注解配置

确保主应用类包含 `@EnableFeignClients` 注解：

```java
@SpringBootApplication
@EnableFeignClients(basePackages = "com.njumarket")
public class OrderServiceApplication {
    // ...
}
```

### ⚠️ Docker Compose 环境变量配置

**问题**: docker-compose.yml 中部分服务使用了错误的环境变量名。

**当前配置（错误）**:
```yaml
- MANAGEMENT_TRACING_EXPORT_ZIPKIN_ENDPOINT=http://zipkin:9411/api/v2/spans
```

**正确配置应该是**:
```yaml
- MANAGEMENT_ZIPKIN_TRACING_ENDPOINT=http://zipkin:9411/api/v2/spans
```

或者使用 `SPRING_APPLICATION_JSON`:
```yaml
- SPRING_APPLICATION_JSON={"management":{"zipkin":{"tracing":{"endpoint":"http://zipkin:9411/api/v2/spans"}}}}
```

或者使用 `JAVA_TOOL_OPTIONS`:
```yaml
- JAVA_TOOL_OPTIONS=-Dfile.encoding=UTF-8 -Duser.timezone=Asia/Shanghai "-Dmanagement.zipkin.tracing.endpoint=http://zipkin:9411/api/v2/spans"
```

## 常见问题排查

### 问题 1: Feign Micrometer 未启用

**症状**: `spring.cloud.openfeign.micrometer.enabled` 为 `false` 或未找到

**解决方案**:
1. 检查配置文件（Config Server 或本地 application.yml）
2. 确保配置路径正确：`spring.cloud.openfeign.micrometer.enabled: true`
3. 重启服务

### 问题 2: Zipkin 端点配置错误

**症状**: Zipkin 端点未配置或配置路径错误

**解决方案**:
1. 检查配置路径：应该是 `management.zipkin.tracing.endpoint`（不是 `management.tracing.export.zipkin.endpoint`）
2. 检查 docker-compose.yml 中的环境变量
3. 确保 Zipkin 服务正常运行

### 问题 3: 追踪上下文未传播

**症状**: Feign Client 调用创建了新的独立 Trace

**可能原因**:
1. `spring.cloud.openfeign.micrometer.enabled` 未启用
2. 缺少必要的依赖
3. Spring Cloud 版本兼容性问题

**解决方案**:
1. 确保所有依赖都已添加
2. 显式启用 `spring.cloud.openfeign.micrometer.enabled: true`
3. 检查服务日志，查看是否有相关错误信息

### 问题 4: 采样率为 0

**症状**: 没有追踪数据

**解决方案**:
1. 检查 `management.tracing.sampling.probability` 配置
2. 确保采样率大于 0（开发环境建议设置为 1.0）

## 验证成功的标志

当配置正确时，你应该看到：

1. ✅ 所有服务的健康检查通过
2. ✅ `spring.cloud.openfeign.micrometer.enabled` 为 `true`
3. ✅ Zipkin 端点配置正确
4. ✅ 追踪采样率为 1.0（开发环境）
5. ✅ 在 Zipkin UI 中，一个 Trace 包含多个 Span（Gateway -> Service A -> Service B）

## 相关文档

- [Feign Client 调用链缺失问题分析](./FEIGN_CLIENT_TRACING_MISSING_ANALYSIS.md)
- [Feign Client 追踪配置检查](./FEIGN_TRACING_CONFIG_CHECK.md)
- [Zipkin Feign Client 追踪上下文传播修复方案](./ZIPKIN_FEIGN_TRACING_FIX.md)

## 日期

2025-01-XX

