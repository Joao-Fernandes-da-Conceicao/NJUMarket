# Zipkin 新旧生态配置冲突问题详解

## 📋 概述

这是 Spring Boot 3.x 升级中最容易踩的坑之一。在从 Spring Cloud Sleuth 迁移到 Micrometer Tracing 的过程中，存在大量新旧配置属性混用的情况，导致配置不生效或产生冲突。

## ⚠️ 问题严重性

- **影响范围**：所有使用 Zipkin 追踪的 Spring Boot 3.x 项目
- **易错程度**：⭐⭐⭐⭐⭐（极高）
- **排查难度**：⭐⭐⭐⭐（困难）
- **AI 误导风险**：⭐⭐⭐⭐⭐（极高，AI 生成代码经常包含旧配置）

## 🔍 问题背景

### 生态变更历史

1. **Spring Boot 2.x 时代**：使用 Spring Cloud Sleuth
   - 配置属性：`spring.zipkin.base-url`
   - 配置路径：`spring.sleuth.*`

2. **Spring Boot 3.x 时代**：迁移到 Micrometer Tracing
   - 配置属性：`management.zipkin.tracing.endpoint`
   - 配置路径：`management.tracing.*`、`management.zipkin.tracing.*`

### 为什么容易出错？

1. **文档滞后**：大量中文教程和博客仍使用旧配置
2. **AI 训练数据过时**：ChatGPT、Copilot 等 AI 工具的训练数据包含旧配置
3. **配置路径相似**：新旧配置路径相似但不同，容易混淆
4. **兼容性陷阱**：某些旧配置可能"看起来"有效，但实际不生效

## 🚨 常见易错点

### 1. 配置路径错误

**❌ 错误配置**（Spring Boot 3.2 不支持）:
```yaml
management:
  tracing:
    export:
      zipkin:
        endpoint: http://zipkin:9411/api/v2/spans
```

**✅ 正确配置**（Spring Boot 3.2）:
```yaml
management:
  zipkin:
    tracing:
      endpoint: http://zipkin:9411/api/v2/spans
```

**关键区别**：
- 错误：`management.tracing.export.zipkin.endpoint`
- 正确：`management.zipkin.tracing.endpoint`

### 2. 旧配置属性混用

**❌ 错误配置**（旧版 Sleuth）:
```yaml
spring:
  zipkin:
    base-url: http://zipkin:9411
```

**✅ 正确配置**（新版 Micrometer Tracing）:
```yaml
management:
  zipkin:
    tracing:
      endpoint: http://zipkin:9411/api/v2/spans
```

**注意**：
- `spring.zipkin.base-url` 是旧版 Sleuth 的配置
- Spring Boot 3.x 不再支持此配置
- 即使配置了也不会报错，但不会生效

### 3. Docker Compose 中显式配置导致冲突

**❌ 错误做法**（同时使用环境变量和 Config Server）:
```yaml
environment:
  # Config Server 中已有配置
  - SPRING_CONFIG_IMPORT=optional:configserver:http://config-server:8888
  
  # ❌ 又在 Docker Compose 中显式配置（导致冲突）
  - MANAGEMENT_ZIPKIN_TRACING_ENDPOINT=http://zipkin:9411/api/v2/spans
  - SPRING_APPLICATION_JSON={"management":{"zipkin":{"tracing":{"endpoint":"http://zipkin:9411/api/v2/spans"}}}}
  - JAVA_TOOL_OPTIONS=-Dmanagement.zipkin.tracing.endpoint=http://zipkin:9411/api/v2/spans
```

**✅ 正确做法**（完全依赖 Config Server）:
```yaml
environment:
  - SPRING_PROFILES_ACTIVE=dev
  - SPRING_CONFIG_IMPORT=optional:configserver:http://config-server:8888
  # ✅ 所有 Zipkin 配置通过 Config Server 管理，不在 Docker Compose 中显式配置
```

### 4. AI 生成代码可能过时

**问题**：
- ChatGPT、GitHub Copilot 等 AI 工具的训练数据可能包含旧版配置
- AI 生成的代码可能混用新旧配置
- 需要人工验证配置是否正确

**解决方案**：
1. 使用 `actuator/configprops` 端点验证实际加载的配置
2. 检查配置属性路径是否与 Spring Boot 版本匹配
3. 参考官方文档而非 AI 生成的代码

## ✅ 最佳实践

### 1. 统一配置管理

**推荐**：所有 Zipkin 配置在 Config Server 中统一管理

```yaml
# Config Server: njumarket-service-order.yml
management:
  tracing:
    enabled: true
    sampling:
      probability: 1.0
    propagation:
      type: B3
  zipkin:
    tracing:
      endpoint: http://zipkin:9411/api/v2/spans
      connect-timeout: 10000
      read-timeout: 10000
```

### 2. 简化 Docker Compose 配置

**推荐**：Docker Compose 只保留必要的环境变量

```yaml
environment:
  - SPRING_PROFILES_ACTIVE=dev
  - SPRING_CONFIG_IMPORT=optional:configserver:http://config-server:8888
  # 其他必要的环境变量（数据库、Redis、Eureka 等）
  # ✅ 不配置 Zipkin 相关环境变量
```

### 3. 验证配置正确性

**使用 Actuator 端点验证**:
```bash
# 查看实际加载的配置
curl http://localhost:8093/actuator/configprops | grep -A 5 "management.zipkin.tracing"

# 应该看到：
# "prefix": "management.zipkin.tracing"
# "properties": {
#   "endpoint": "http://zipkin:9411/api/v2/spans"
# }
```

### 4. 版本匹配检查

**检查清单**：
- [ ] Spring Boot 版本：3.2.0
- [ ] Spring Cloud 版本：2023.0.3
- [ ] 配置路径：`management.zipkin.tracing.endpoint`
- [ ] 不使用旧配置：`spring.zipkin.base-url`

## 🧹 配置清理清单

在解决新旧生态冲突后，应删除以下 Docker Compose 中的配置：

### 需要删除的配置

- ❌ `ZIPKIN_HOST`、`ZIPKIN_PORT`（旧环境变量）
- ❌ `SPRING_ZIPKIN_BASE_URL`（旧版 Sleuth 配置）
- ❌ `MANAGEMENT_ZIPKIN_TRACING_ENDPOINT`（显式环境变量）
- ❌ `SPRING_APPLICATION_JSON` 中的 Zipkin 配置（显式 JSON 配置）
- ❌ `JAVA_TOOL_OPTIONS` 中的 `-Dspring.zipkin.base-url`（旧系统属性）
- ❌ `JAVA_TOOL_OPTIONS` 中的 `-Dmanagement.zipkin.tracing.endpoint`（如果 Config Server 已配置）

### 保留的配置

- ✅ `SPRING_PROFILES_ACTIVE=dev`（激活环境）
- ✅ `SPRING_CONFIG_IMPORT=optional:configserver:...`（连接 Config Server）
- ✅ 数据库、Redis、Eureka 等必要的环境变量
- ✅ 字符编码相关的环境变量（`LANG`、`LC_ALL`、`JAVA_TOOL_OPTIONS` 中的字符编码和时区）

## 🔧 故障排查

### 问题 1：追踪数据未显示在 Zipkin

**排查步骤**：
1. 检查 Zipkin 服务是否运行
2. 验证配置路径是否正确（使用 `actuator/configprops`）
3. 检查网络连接（服务能否访问 Zipkin）
4. 查看服务日志

### 问题 2：服务连接 localhost:9411 而不是 zipkin:9411

**原因**：配置路径错误或配置优先级问题

**解决方案**：
1. 验证配置路径：确保使用 `management.zipkin.tracing.endpoint`
2. 检查配置优先级：使用 `actuator/configprops` 查看实际加载的配置
3. 确保 Config Server 配置正确
4. 避免在 Docker Compose 中重复配置

### 问题 3：配置看起来正确但不生效

**可能原因**：
1. 使用了旧配置属性（如 `spring.zipkin.base-url`）
2. 配置路径错误（如 `management.tracing.export.zipkin.endpoint`）
3. 配置优先级问题（环境变量覆盖了 Config Server 配置）

**解决方案**：
1. 使用 `actuator/configprops` 验证实际加载的配置
2. 清理 Docker Compose 中的显式配置
3. 确保 Config Server 配置正确

## 📚 参考资源

### 官方文档

- [Spring Boot 3.2 官方文档 - Micrometer Tracing](https://docs.spring.io/spring-boot/docs/3.2.0/reference/html/actuator.html#actuator.micrometer-tracing)
- [Micrometer Tracing 官方文档](https://micrometer.io/docs/tracing)

### 配置属性参考

- ✅ 正确：`management.zipkin.tracing.endpoint`
- ❌ 错误：`management.tracing.export.zipkin.endpoint`
- ❌ 错误：`spring.zipkin.base-url`

## 📝 项目实践记录

### 问题发现时间

2025-11-13

### 问题解决过程

1. **问题识别**：发现 Zipkin 追踪数据未正确显示
2. **配置验证**：使用 `actuator/configprops` 发现配置路径错误
3. **配置修复**：修正配置路径为 `management.zipkin.tracing.endpoint`
4. **配置清理**：清理 Docker Compose 中所有显式的 Zipkin 配置
5. **验证通过**：追踪功能正常工作

### 最终配置

**Config Server 配置**（统一管理）:
```yaml
management:
  tracing:
    enabled: true
    sampling:
      probability: 1.0
    propagation:
      type: B3
  zipkin:
    tracing:
      endpoint: http://zipkin:9411/api/v2/spans
      connect-timeout: 10000
      read-timeout: 10000
```

**Docker Compose 配置**（简化）:
```yaml
environment:
  - SPRING_PROFILES_ACTIVE=dev
  - SPRING_CONFIG_IMPORT=optional:configserver:http://config-server:8888
  # 其他必要的环境变量
  # ✅ 不配置 Zipkin 相关环境变量
```

## 🎯 总结

1. **配置路径是关键**：`management.zipkin.tracing.endpoint` 是唯一正确的路径
2. **统一配置管理**：所有配置在 Config Server 中统一管理
3. **避免重复配置**：不要在 Docker Compose 中显式配置 Zipkin
4. **验证配置正确性**：使用 `actuator/configprops` 验证实际加载的配置
5. **警惕 AI 生成代码**：AI 可能生成过时的配置，需要人工验证

---

**最后更新**：2025-11-13  
**文档版本**：v1.0

