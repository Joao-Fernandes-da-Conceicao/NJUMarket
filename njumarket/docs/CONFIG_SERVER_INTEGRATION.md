# Spring Cloud Config Server 集成文档

## 概述

本项目已集成 **Spring Cloud Config Server**，实现了配置的集中管理和动态刷新。

### 版本信息
- **版本**: v2.1.2
- **集成时间**: 2025-11-12
- **环境隔离**: ✅ **已完成** (v2.1.2)
- **状态**: ✅ **已完成**

---

## 架构说明

### Config Server
- **服务名称**: `njumarket-config-server`
- **端口**: `8888`
- **存储方式**: 本地文件系统（`classpath:/config-repo`）
- **注册中心**: Eureka

### Config Client
所有微服务都已配置为 Config Client，包括：
- `njumarket-service-auth` (8091)
- `njumarket-service-commodity` (8092)
- `njumarket-service-order` (8093)
- `njumarket-service-message` (8094)
- `njumarket-service-image` (8095)
- `njumarket-service-admin` (8096)
- `njumarket-service-notification` (8097)
- `njumarket-gateway` (8080)

---

## 配置说明

### Config Server 配置

**位置**: `njumarket-config/src/main/resources/application.yml`

```yaml
server:
  port: 8888

spring:
  application:
    name: njumarket-config-server
  cloud:
    config:
      server:
        native:
          search-locations: classpath:/config-repo
  profiles:
    active: native
```

### Config Client 配置

**位置**: 各服务的 `application.yml`

```yaml
spring:
  application:
    name: njumarket-service-{name}
  config:
    import: optional:configserver:http://localhost:8888
    fail-fast: false  # Config Server 不可用时，使用本地配置
```

**说明**:
- `optional:configserver:...`: 可选连接，Config Server 不可用时不会导致启动失败
- `fail-fast: false`: 连接失败时使用本地配置，保证服务可用性

---

## 配置文件结构

### 配置仓库位置
`njumarket-config/src/main/resources/config-repo/`

### 配置文件命名规则
- **默认配置**: `{application-name}.yml` - 包含通用配置（Feign、Actuator、Resilience4j 等）
- **环境配置**: `{application-name}-{profile}.yml` - 包含环境相关配置（MySQL、Redis、Eureka 等）

例如：
- `njumarket-service-order.yml` (默认配置 - 通用配置)
- `njumarket-service-order-dev.yml` (开发环境 - MySQL、Redis 配置)
- `njumarket-service-order-prod.yml` (生产环境 - MySQL、Redis 配置)

### 当前配置文件

**默认配置（通用配置）**：
- `njumarket-service-auth.yml`
- `njumarket-service-commodity.yml`
- `njumarket-service-order.yml`
- `njumarket-service-message.yml`
- `njumarket-service-image.yml`
- `njumarket-service-admin.yml`
- `njumarket-service-notification.yml`
- `njumarket-gateway.yml`

**开发环境配置（环境相关配置）**：
- `njumarket-service-auth-dev.yml` - MySQL、Redis 配置
- `njumarket-service-commodity-dev.yml` - MySQL、Redis 配置
- `njumarket-service-order-dev.yml` - MySQL、Redis 配置
- `njumarket-service-message-dev.yml` - MySQL、Redis 配置
- `njumarket-service-image-dev.yml` - MySQL 配置
- `njumarket-service-admin-dev.yml` - MySQL、Redis 配置
- `njumarket-service-notification-dev.yml` - Redis 配置
- `njumarket-gateway-dev.yml` - Redis、Eureka 配置

### 环境隔离说明

**配置分离原则**：
- **默认配置** (`{application}.yml`)：包含所有环境通用的配置
  - 服务端口、应用名称
  - Feign Client 配置
  - Actuator 配置
  - Resilience4j 配置
  - 日志配置
  - 其他业务无关的通用配置

- **环境配置** (`{application}-{profile}.yml`)：包含环境相关的配置
  - MySQL 数据源配置
  - Redis 配置
  - Eureka 注册中心配置
  - 其他环境相关的配置（如文件上传路径、图片服务地址等）

**配置合并机制**：
当服务启动时，Spring Cloud Config Server 会：
1. 加载默认配置 (`{application}.yml`)
2. 加载环境配置 (`{application}-{profile}.yml`)
3. 合并配置（环境配置会覆盖默认配置中的相同 key）
4. 应用环境变量覆盖（环境变量优先级最高）

**示例**：
```yaml
# njumarket-service-order.yml (默认配置)
server:
  port: 8093
spring:
  application:
    name: njumarket-service-order
# Feign、Actuator 等通用配置...

# njumarket-service-order-dev.yml (开发环境配置)
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/nju_market
    username: root
    password: Hqz20050316
  data:
    redis:
      host: localhost
      port: 6379
```

**激活环境**：
- **本地开发**：设置 `SPRING_PROFILES_ACTIVE=dev`
- **Docker 环境**：在 `docker-compose.yml` 中设置 `SPRING_PROFILES_ACTIVE=dev`，并通过环境变量覆盖数据库和 Redis 主机地址

---

## 使用方式

### 1. 启动顺序

1. **启动 Eureka Server** (8761)
2. **启动 Config Server** (8888)
3. **启动其他微服务**

> 🐳 **Docker 场景**  
> 通过 `docker-compose up` 启动时，`config-server` 服务已经加入编排文件，并设置了 `depends_on` + `healthcheck`。所有微服务容器都会自动注入 `SPRING_CONFIG_IMPORT=optional:configserver:http://config-server:8888`，因此无需手动调整配置。

### 2. 验证配置加载

#### 方式一：查看服务日志
启动服务时，日志中会显示：
```
Located property source: [BootstrapPropertySource {name='bootstrapProperties-configClient'}, ...]
```

#### 方式二：访问 Config Server API
```bash
# 获取默认配置
curl http://localhost:8888/njumarket-service-order/default

# 获取指定环境配置
curl http://localhost:8888/njumarket-service-order/dev
```

#### 方式三：访问服务 Actuator
```bash
# 查看配置信息
curl http://localhost:8093/actuator/configprops
```

### 3. 配置刷新

#### 手动刷新（需要 Spring Boot Actuator）
1. 修改 Config Server 中的配置文件
2. 调用服务的 `/actuator/refresh` 端点：
```bash
curl -X POST http://localhost:8093/actuator/refresh
```

#### 自动刷新（需要 Spring Cloud Bus）
目前未集成，后续可考虑添加。

---

## Docker 部署要点

1. `docker-compose.yml` 新增 `config-server` 服务；其余微服务在 `depends_on` 中显式依赖它的健康状态，以保证启动顺序。
2. 每个微服务容器设置 `SPRING_CONFIG_IMPORT=optional:configserver:http://config-server:8888`，覆盖本地默认值 `http://localhost:8888`。
3. 每个微服务容器设置 `SPRING_PROFILES_ACTIVE=dev`，激活开发环境配置。
4. 通过环境变量覆盖 dev 配置中的数据库和 Redis 主机地址：
   - `AUTH_DATASOURCE_URL=jdbc:mysql://mysql:3306/...` (覆盖 `localhost`)
   - `REDIS_HOST=redis` (覆盖 `localhost`)
   - `EUREKA_CLIENT_SERVICEURL_DEFAULTZONE=http://discovery:8761/eureka` (覆盖 `localhost`)
5. 通用 `Dockerfile` 现已支持构建 `njumarket-config` 模块，Config Server 与其它服务共用同一套构建流程。

---

## 配置优先级

配置的加载顺序（优先级从高到低）：
1. **环境变量**（Docker Compose 或系统环境变量）
2. **Config Server 环境配置** (`{application}-{profile}.yml`)
3. **Config Server 默认配置** (`{application}.yml`)
4. **本地 application.yml**（本地配置，作为后备）
5. **系统属性**

**配置合并示例**：
```
默认配置 (order.yml)
  ↓
环境配置 (order-dev.yml) - 覆盖相同 key
  ↓
环境变量 (AUTH_DATASOURCE_URL) - 覆盖相同 key
  ↓
最终配置
```

**注意**: 如果 Config Server 不可用，服务会使用本地 `application.yml` 配置，保证服务可用性。

---

## 多环境配置

### 开发环境 (dev)
```yaml
# config-repo/njumarket-service-order-dev.yml
spring:
  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: ${ORDER_DATASOURCE_URL:jdbc:mysql://localhost:3306/nju_market?useSSL=false&useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai}
    username: ${ORDER_DATASOURCE_USERNAME:root}
    password: ${ORDER_DATASOURCE_PASSWORD:Hqz20050316}
  data:
    redis:
      host: ${REDIS_HOST:localhost}
      port: ${REDIS_PORT:6379}
      password: ${REDIS_PASSWORD:hqz20050316}
      database: ${ORDER_REDIS_DATABASE:2}
```

**说明**：
- 使用环境变量 `${VAR:default}` 语法，支持通过环境变量覆盖默认值
- Docker 环境中，通过 `docker-compose.yml` 设置环境变量来覆盖数据库和 Redis 主机地址

### 生产环境 (prod)
```yaml
# config-repo/njumarket-service-order-prod.yml
spring:
  datasource:
    url: jdbc:mysql://prod-db:3306/nju_market
    username: ${ORDER_DATASOURCE_USERNAME:prod_user}
    password: ${ORDER_DATASOURCE_PASSWORD}  # 必须通过环境变量提供
  data:
    redis:
      host: ${REDIS_HOST:prod-redis}
      port: ${REDIS_PORT:6379}
      password: ${REDIS_PASSWORD}  # 必须通过环境变量提供
```

### 激活环境

**方式一：启动参数**
```bash
java -jar app.jar --spring.profiles.active=dev
```

**方式二：环境变量**
```bash
export SPRING_PROFILES_ACTIVE=dev
java -jar app.jar
```

**方式三：Docker Compose**
```yaml
environment:
  - SPRING_PROFILES_ACTIVE=dev
  - ORDER_DATASOURCE_URL=jdbc:mysql://mysql:3306/nju_market
  - REDIS_HOST=redis
```

### Docker 环境配置覆盖

在 `docker-compose.yml` 中，通过环境变量覆盖 dev 配置中的默认值：

```yaml
auth-service:
  environment:
    - SPRING_PROFILES_ACTIVE=dev  # 激活 dev profile
    - AUTH_DATASOURCE_URL=jdbc:mysql://mysql:3306/nju_market  # 覆盖 dev 配置中的 localhost
    - REDIS_HOST=redis  # 覆盖 dev 配置中的 localhost
    - EUREKA_CLIENT_SERVICEURL_DEFAULTZONE=http://discovery:8761/eureka
```

**配置优先级**（从高到低）：
1. **环境变量**（Docker Compose 中设置）
2. **环境配置** (`{application}-{profile}.yml`)
3. **默认配置** (`{application}.yml`)

---

## 最佳实践

### 1. 敏感信息管理
- **密码、密钥等敏感信息**应使用环境变量或加密配置
- 不要将敏感信息直接写入配置文件

### 2. 配置分类
- **公共配置**: 可提取到 `application.yml`（所有服务共享）
- **服务特定配置**: 放在各自的 `{service-name}.yml`

### 3. 配置版本管理
- 使用 Git 管理配置仓库（未来可迁移到 Git 模式）
- 为配置变更添加注释和版本号

### 4. 配置验证
- 修改配置后，先在测试环境验证
- 使用 Config Server 的 `/actuator/health` 检查服务状态

---

## 故障排查

### 问题 1: Config Server 连接失败
**现象**: 服务启动时提示无法连接 Config Server

**解决方案**:
1. 检查 Config Server 是否启动（端口 8888）
2. 检查 Eureka 注册中心是否正常
3. 确认 `fail-fast: false` 已配置（服务会使用本地配置）

### 问题 2: 配置未生效
**现象**: 修改了 Config Server 配置，但服务未更新

**解决方案**:
1. 确认配置文件命名正确（`{application-name}.yml`）
2. 检查服务是否成功连接到 Config Server（查看日志）
3. 调用 `/actuator/refresh` 手动刷新配置

### 问题 3: 配置冲突
**现象**: 配置值与预期不符

**解决方案**:
1. 检查配置优先级（Config Server > 本地 > 环境变量）
2. 查看服务的完整配置：`/actuator/configprops`
3. 确认环境变量未覆盖 Config Server 配置

---

## 后续优化

### 1. Git 模式
将配置存储从本地文件系统迁移到 Git 仓库，支持：
- 配置版本管理
- 配置变更历史
- 多分支管理（dev、test、prod）

### 2. 配置加密
使用 Spring Cloud Config 的加密功能，保护敏感信息

### 3. 配置中心高可用
- Config Server 集群部署
- 配置缓存机制

### 4. 配置监控
- 配置变更通知
- 配置使用情况统计

---

## 参考资源

- [Spring Cloud Config 官方文档](https://spring.io/projects/spring-cloud-config)
- [Config Server 配置指南](https://docs.spring.io/spring-cloud-config/docs/current/reference/html/)
- [Config Client 使用说明](https://docs.spring.io/spring-cloud-config/docs/current/reference/html/#_spring_cloud_config_client)

---

## 总结

✅ **已完成**:
- Config Server 服务创建和配置
- 所有微服务集成 Config Client
- 配置仓库目录结构和配置文件迁移
- **环境隔离**：MySQL 和 Redis 配置已分离到 dev 环境配置文件
- **配置分离**：默认配置包含通用配置，环境配置包含环境相关配置
- **Docker 支持**：通过环境变量覆盖 dev 配置中的默认值，适配容器环境
- 配置加载和刷新机制

🎯 **学习目标达成**:
- 理解 Spring Cloud Config 的工作原理
- 掌握配置集中管理的方法
- 了解多环境配置管理
- 学习配置刷新和动态更新机制

