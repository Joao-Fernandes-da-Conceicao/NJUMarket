# 南大集市 NJUMarket v2.1.1 项目文档

## 📋 目录
- [版本概述](#版本概述)
- [版本更新内容](#版本更新内容)
- [Resilience4j 熔断降级详解](#resilience4j-熔断降级详解)
- [Spring Cloud Config Server 详解](#spring-cloud-config-server-详解)
- [环境隔离机制](#环境隔离机制)
- [使用指南](#使用指南)
- [技术总结](#技术总结)
- [后续版本规划](#后续版本规划)

---

## 版本概述

**NJUMarket v2.1.1** 是项目的稳定性保障和配置管理版本，主要完成了服务熔断降级、配置中心集成和环境隔离。

### 版本信息
- **版本**: v2.1.1
- **发布日期**: 2025-11-12
- **状态**: ✅ **已完成**
- **主要目标**: 服务稳定性保障、配置集中管理、环境隔离

### 版本历史
- **v2.1.0** (2025-11-11): Actuator 监控、Docker 容器化、Swagger API 文档 ✅ **2.1.0阶段已完成**
- **v2.1.1** (2025-11-12): Resilience4j 熔断降级、Config Server 配置中心、环境隔离 ✅ **2.1.1阶段已完成**
- **v2.1.2** (2025-11-12): Micrometer Tracing + Zipkin 分布式链路追踪 ✅ **2.1.2阶段已完成**
  - **配置路径修复** (2025-11-13): 修复 Spring Boot 3.2 中 Zipkin 配置路径错误问题

### 主要成就

#### 服务稳定性保障
- ✅ 集成 Resilience4j 熔断降级（4个服务）
  - Order Service：5个 Feign Client（商品、认证、通知、图片、商品查询）
  - Message Service：2个 Feign Client（商品、订单）
  - Commodity Service：3个 Feign Client（通知、图片、订单）
  - Notification Service：2个 Feign Client（商品查询、订单查询）
- ✅ 为所有 Feign Client 添加熔断器
- ✅ 实现 Fallback 降级策略
- ✅ 配置超时控制和健康指标

#### 配置集中管理
- ✅ 集成 Spring Cloud Config Server
- ✅ 统一管理所有微服务配置
- ✅ 支持配置动态刷新
- ✅ 实现环境隔离（dev/test/prod）

#### 环境隔离
- ✅ 将 MySQL 和 Redis 配置分离到 dev 环境
- ✅ 默认配置仅保留通用配置
- ✅ 支持通过环境变量覆盖配置
- ✅ Docker 环境自动适配

---

## 版本更新内容

### 1. Resilience4j 熔断降级 ✅

**目标**: 防止服务雪崩，提高系统可用性

**完成内容**:
- **Order Service**：集成 Resilience4j，为 5 个 Feign Client 配置熔断器
  - `CommodityClient` (商品服务)
  - `CommodityQueryClient` (商品查询服务)
  - `AuthClient` (认证服务)
  - `NotificationClient` (通知服务)
  - `ImageClient` (图片服务)
- **Message Service**：集成 Resilience4j，为 2 个 Feign Client 配置熔断器
  - `CommodityClient` (商品服务 - 发送商品卡片)
  - `OrderClient` (订单服务 - 发送订单卡片)
- **Commodity Service**：集成 Resilience4j，为 3 个 Feign Client 配置熔断器
  - `NotificationClient` (通知服务 - 推送商品变更)
  - `ImageClient` (图片服务 - 上传商品图片)
  - `OrderClient` (订单服务 - 检查商品是否有订单)
- **Notification Service**：集成 Resilience4j，为 2 个 Feign Client 配置熔断器
  - `CommodityQueryClient` (商品查询服务 - 增量轮询)
  - `OrderQueryClient` (订单查询服务 - 增量轮询)
- 创建对应的 Fallback 类（共 12 个）
- 配置熔断器参数（滑动窗口、失败率阈值、超时时间等）
- 暴露熔断器健康指标

**详细说明**: 见 [Resilience4j 熔断降级详解](#resilience4j-熔断降级详解)

### 2. Spring Cloud Config Server ✅

**目标**: 简化配置管理，支持配置热更新

**完成内容**:
- 创建 Config Server 服务（端口 8888）
- 使用 Native 模式（本地文件系统存储）
- 所有微服务配置为 Config Client
- 配置文件集中管理在 `config-repo/` 目录
- 支持配置动态刷新（`/actuator/refresh` 端点）

**详细说明**: 见 [Spring Cloud Config Server 详解](#spring-cloud-config-server-详解)

### 3. 环境隔离 ✅

**目标**: 实现配置的环境隔离，支持多环境部署

**完成内容**:
- 创建 dev 环境配置文件（`{application}-dev.yml`）
- 将 MySQL 和 Redis 配置移至 dev 环境
- 默认配置仅保留通用配置（Feign、Actuator、Resilience4j 等）
- Docker 环境使用 dev profile，通过环境变量覆盖配置
- 支持未来扩展 prod 环境配置

**详细说明**: 见 [环境隔离机制](#环境隔离机制)

---

## Resilience4j 熔断降级详解

### 什么是 Resilience4j？

Resilience4j 是一个轻量级的容错库，专为 Java 8 和函数式编程设计。它提供了：
- **熔断器**（Circuit Breaker）：防止服务雪崩
- **重试**（Retry）：自动重试失败的操作
- **限流**（Rate Limiter）：控制请求速率
- **时间限制器**（Time Limiter）：限制操作执行时间
- **隔离**（Bulkhead）：隔离资源池

### 为什么需要熔断降级？

在微服务架构中，服务间调用可能因为以下原因失败：
- 网络延迟或超时
- 目标服务过载或宕机
- 数据库连接池耗尽
- 第三方服务不可用

**服务雪崩效应**：
```
服务A → 服务B（慢/失败）
  ↓
服务A 等待响应，线程阻塞
  ↓
服务A 线程池耗尽
  ↓
服务A 无法处理新请求
  ↓
调用服务A 的其他服务也受影响
  ↓
整个系统崩溃
```

**熔断器的作用**：
- 快速失败：检测到服务异常时立即返回降级结果
- 保护系统：防止线程池耗尽，避免服务雪崩
- 自动恢复：服务恢复后自动关闭熔断器

### 实现细节

#### 1. 依赖配置

**Order Service `pom.xml`**:
```xml
<dependency>
    <groupId>io.github.resilience4j</groupId>
    <artifactId>resilience4j-spring-boot3</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-circuitbreaker-resilience4j</artifactId>
</dependency>
```

#### 2. Feign Client 配置

**示例：`CommodityClient`**:
```java
@FeignClient(
    name = "njumarket-service-commodity",
    contextId = "commodityInternalClient",
    path = "/api/internal",
    fallback = CommodityClientFallback.class  // ✅ 指定 Fallback 类
)
public interface CommodityClient {
    Result getCommodityForUpdate(String commodityId);
    Result updateCommodityStock(String commodityId, Integer quantity);
    Result restoreCommodityStock(String commodityId, Integer quantity);
}
```

#### 3. Fallback 实现

**示例：`CommodityClientFallback`**:
```java
@Slf4j
@Component
public class CommodityClientFallback implements CommodityClient {
    @Override
    public Result getCommodityForUpdate(String commodityId) {
        log.warn("商品服务不可用，触发熔断降级: commodityId={}", commodityId);
        return Result.fail("商品服务暂时不可用，请稍后重试");
    }
    
    // ... 其他方法的降级实现
}
```

#### 4. 熔断器配置

**Config Server 配置** (`njumarket-service-order.yml`):
```yaml
resilience4j:
  circuitbreaker:
    instances:
      commodityService:
        registerHealthIndicator: true
        slidingWindowSize: 10
        minimumNumberOfCalls: 5
        permittedNumberOfCallsInHalfOpenState: 3
        automaticTransitionFromOpenToHalfOpenEnabled: true
        waitDurationInOpenState: 10s
        failureRateThreshold: 50
        slowCallRateThreshold: 100
        slowCallDurationThreshold: 2s
  timelimiter:
    instances:
      commodityService:
        timeoutDuration: 3s
```

**配置说明**:
- `slidingWindowSize: 10`: 滑动窗口大小为 10 次调用
- `minimumNumberOfCalls: 5`: 至少 5 次调用后才计算失败率
- `failureRateThreshold: 50`: 失败率超过 50% 时打开熔断器
- `waitDurationInOpenState: 10s`: 熔断器打开后等待 10 秒再尝试恢复
- `timeoutDuration: 3s`: 超时时间为 3 秒

#### 5. 熔断器状态

**三种状态**:
1. **CLOSED（关闭）**: 正常状态，请求正常通过
2. **OPEN（打开）**: 熔断器打开，直接返回降级结果，不调用目标服务
3. **HALF_OPEN（半开）**: 尝试恢复，允许少量请求通过，如果成功则关闭熔断器

**状态转换**:
```
CLOSED → (失败率 > 阈值) → OPEN → (等待时间后) → HALF_OPEN → (成功) → CLOSED
```

#### 6. 健康指标

**访问熔断器状态**:
```bash
# 查看所有熔断器状态
GET http://localhost:8093/actuator/health/circuitbreakers

# 查看特定熔断器状态
GET http://localhost:8093/actuator/circuitbreakers/commodityService
```

**响应示例**:
```json
{
  "status": "UP",
  "details": {
    "commodityService": {
      "status": "UP",
      "details": {
        "state": "CLOSED",
        "failureRate": 0.0,
        "slowCallRate": 0.0,
        "bufferedCalls": 10,
        "failedCalls": 0,
        "slowCalls": 0
      }
    }
  }
}
```

### 使用场景

**当前实现**:

#### Order Service（5个 Feign Client）
- ✅ 商品服务调用（库存更新、商品查询）
- ✅ 认证服务调用（用户信息查询）
- ✅ 通知服务调用（订单变更推送）
- ✅ 图片服务调用（图片上传/删除）

#### Message Service（2个 Feign Client）
- ✅ 商品服务调用（发送商品卡片时查询商品）
- ✅ 订单服务调用（发送订单卡片时查询订单）

#### Commodity Service（3个 Feign Client）
- ✅ 通知服务调用（推送商品变更通知 - 静默失败）
- ✅ 图片服务调用（上传商品图片）
- ✅ 订单服务调用（检查商品是否有订单）

#### Notification Service（2个 Feign Client）
- ✅ 商品查询服务调用（批量查询商品状态 - 返回空列表）
- ✅ 订单查询服务调用（批量查询订单状态 - 返回空列表）

**测试方法**:
```bash
# 1. 停止商品服务
docker stop njumarket-service-commodity

# 2. 测试 Order Service（创建订单会触发熔断降级）
POST http://localhost:8080/api/user/order/create

# 3. 测试 Message Service（发送商品卡片会触发熔断降级）
POST http://localhost:8080/api/user/message/send
{
  "messageType": "COMMODITY_CARD",
  "commodityId": "..."
}

# 4. 测试 Notification Service（增量轮询会返回空列表）
GET http://localhost:8080/api/user/chat/incremental-update?lastPollTimestamp=...

# 5. 查看熔断器状态
GET http://localhost:8093/actuator/health/circuitbreakers  # Order Service
GET http://localhost:8094/actuator/health/circuitbreakers  # Message Service
GET http://localhost:8092/actuator/health/circuitbreakers  # Commodity Service
GET http://localhost:8097/actuator/health/circuitbreakers  # Notification Service

# 6. 查看日志（会看到 Fallback 日志）
```

---

## Spring Cloud Config Server 详解

### 什么是 Spring Cloud Config Server？

Spring Cloud Config Server 是 Spring Cloud 提供的配置中心，用于集中管理微服务的配置。

### 为什么需要配置中心？

**单体应用的问题**:
- 配置分散在各个服务的 `application.yml` 中
- 修改配置需要重新打包部署
- 无法统一管理多环境配置
- 配置变更无法动态生效

**配置中心的优势**:
- ✅ 集中管理：所有配置统一存储在配置中心
- ✅ 环境隔离：支持 dev/test/prod 多环境配置
- ✅ 动态刷新：支持配置热更新，无需重启服务
- ✅ 版本管理：支持配置版本管理和回滚

### 架构设计

```
┌─────────────────────────────────────────┐
│         Config Server (8888)            │
│  ┌───────────────────────────────────┐  │
│  │   config-repo/                    │  │
│  │   ├── njumarket-service-order.yml │  │
│  │   ├── njumarket-service-order-    │  │
│  │   │   dev.yml                     │  │
│  │   └── ...                         │  │
│  └───────────────────────────────────┘  │
└─────────────────────────────────────────┘
              ↑
              │ HTTP
              │
┌─────────────┴─────────────┐
│   Config Client (各服务)   │
│  - Auth Service            │
│  - Commodity Service       │
│  - Order Service           │
│  - ...                     │
└────────────────────────────┘
```

### 实现细节

#### 1. Config Server 创建

**服务模块**: `njumarket-config`

**主类**:
```java
@SpringBootApplication
@EnableConfigServer
public class ConfigServerApplication {
    public static void main(String[] args) {
        SpringApplication.run(ConfigServerApplication.class, args);
    }
}
```

**配置文件** (`application.yml`):
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
    active: native  # 使用本地文件系统存储

eureka:
  client:
    service-url:
      defaultZone: http://localhost:8761/eureka
```

#### 2. Config Client 配置

**各服务的 `application.yml`**:
```yaml
spring:
  application:
    name: njumarket-service-order
  config:
    import: optional:configserver:http://localhost:8888
    fail-fast: false  # Config Server 不可用时使用本地配置
```

**Docker 环境**:
```yaml
environment:
  - SPRING_CONFIG_IMPORT=optional:configserver:http://config-server:8888
```

#### 3. 配置文件结构

```
config-repo/
├── njumarket-service-order.yml          # 默认配置（通用配置）
├── njumarket-service-order-dev.yml      # Dev 环境（MySQL、Redis）
├── njumarket-service-auth.yml
├── njumarket-service-auth-dev.yml
└── ...
```

#### 4. 配置加载机制

**配置优先级**（从高到低）:
1. **环境变量**（Docker Compose 中设置）
2. **环境配置** (`{application}-{profile}.yml`)
3. **默认配置** (`{application}.yml`)
4. **本地 application.yml**（作为后备）

**配置合并示例**:
```
默认配置 (order.yml)
  ↓
环境配置 (order-dev.yml) - 覆盖相同 key
  ↓
环境变量 (AUTH_DATASOURCE_URL) - 覆盖相同 key
  ↓
最终配置
```

#### 5. 配置动态刷新

**手动刷新**:
```bash
# 刷新订单服务配置
curl -X POST http://localhost:8093/actuator/refresh

# 返回变更的配置 key
{
  "spring.datasource.url"
}
```

**刷新范围**:
- 只影响标记了 `@RefreshScope` 的 Bean
- 需要重新创建的 Bean（如 `DataSource`、`RedisTemplate`）

---

## 环境隔离机制

### 设计原则

**配置分离**:
- **默认配置** (`{application}.yml`): 包含所有环境通用的配置
  - 服务端口、应用名称
  - Feign Client 配置
  - Actuator 配置
  - Resilience4j 配置
  - 日志配置

- **环境配置** (`{application}-{profile}.yml`): 包含环境相关的配置
  - MySQL 数据源配置
  - Redis 配置
  - Eureka 注册中心配置
  - 其他环境相关的配置

### 实现细节

#### 1. Dev 环境配置

**示例：`njumarket-service-order-dev.yml`**:
```yaml
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

eureka:
  client:
    service-url:
      defaultZone: ${EUREKA_CLIENT_SERVICEURL_DEFAULTZONE:http://localhost:8761/eureka}
```

#### 2. Docker 环境适配

**docker-compose.yml**:
```yaml
order-service:
  environment:
    - SPRING_PROFILES_ACTIVE=dev  # 激活 dev profile
    - ORDER_DATASOURCE_URL=jdbc:mysql://mysql:3306/nju_market  # 覆盖 localhost
    - REDIS_HOST=redis  # 覆盖 localhost
    - EUREKA_CLIENT_SERVICEURL_DEFAULTZONE=http://discovery:8761/eureka
```

**工作原理**:
1. 服务启动时激活 `dev` profile
2. 加载 `order-dev.yml` 配置（包含 MySQL、Redis 配置）
3. 环境变量覆盖配置中的默认值（`localhost` → `mysql`、`redis`）

#### 3. 未来扩展

**生产环境配置** (`njumarket-service-order-prod.yml`):
```yaml
spring:
  datasource:
    url: jdbc:mysql://prod-db:3306/nju_market
    username: ${ORDER_DATASOURCE_USERNAME:prod_user}
    password: ${ORDER_DATASOURCE_PASSWORD}  # 必须通过环境变量提供
  data:
    redis:
      host: ${REDIS_HOST:prod-redis}
      password: ${REDIS_PASSWORD}  # 必须通过环境变量提供
```

**激活方式**:
```bash
# 本地开发
SPRING_PROFILES_ACTIVE=dev

# 生产环境
SPRING_PROFILES_ACTIVE=prod
```

---

## 使用指南

### 1. 启动服务

**Docker 环境**（推荐）:
```bash
cd njumarket
docker-compose up -d --build
```

**本地环境**:
```bash
# 1. 启动 Eureka
# 2. 启动 Config Server
# 3. 启动其他服务
```

### 2. 验证配置加载

**查看配置**:
```bash
# 获取默认配置
curl http://localhost:8888/njumarket-service-order/default

# 获取 dev 环境配置
curl http://localhost:8888/njumarket-service-order/dev
```

**查看服务配置**:
```bash
# 查看订单服务的完整配置
curl http://localhost:8093/actuator/configprops
```

### 3. 测试熔断降级

**停止商品服务**:
```bash
docker stop njumarket-service-commodity
```

**创建订单**（会触发降级）:
```bash
POST http://localhost:8080/api/user/order/create
```

**查看熔断器状态**:
```bash
GET http://localhost:8093/actuator/health/circuitbreakers
```

### 4. 配置动态刷新

**修改配置**:
```yaml
# config-repo/njumarket-service-order-dev.yml
spring:
  datasource:
    url: jdbc:mysql://new-host:3306/nju_market
```

**刷新配置**:
```bash
curl -X POST http://localhost:8093/actuator/refresh
```

---

## 技术总结

### 学习收获

#### Resilience4j
1. **熔断器原理**: 理解了熔断器的三种状态和转换机制
2. **降级策略**: 掌握了 Fallback 降级策略的实现
3. **配置调优**: 学会了根据业务场景调整熔断器参数
4. **健康监控**: 理解了如何监控熔断器状态
5. **多服务集成**: 完成了 4 个服务的 Resilience4j 集成，共 12 个 Feign Client
6. **降级策略设计**: 理解了关键路径和非关键路径的不同降级策略

#### Spring Cloud Config Server
1. **配置中心**: 理解了配置中心的作用和优势
2. **Native 模式**: 掌握了本地文件系统存储配置的方法
3. **配置刷新**: 理解了配置动态刷新的机制和限制
4. **环境隔离**: 学会了多环境配置的管理方法

#### 环境隔离
1. **配置分离**: 理解了通用配置和环境配置的分离原则
2. **配置优先级**: 掌握了配置加载的优先级顺序
3. **Docker 适配**: 学会了通过环境变量覆盖配置的方法

### 技术栈

**新增技术**:
- Resilience4j: 容错库
- Spring Cloud Config Server: 配置中心
- Spring Cloud Config Client: 配置客户端

**技术版本**:
- Spring Boot: 3.2.0
- Spring Cloud: 2023.0.3
- Resilience4j: 2.1.0

**Resilience4j 集成统计**:
- **4 个服务**：Order、Message、Commodity、Notification
- **12 个 Feign Client**：全部配置熔断器
- **12 个 Fallback 类**：全部实现降级策略

---

## Micrometer Tracing + Zipkin 分布式链路追踪详解

### 什么是分布式链路追踪？

在微服务架构中，一个用户请求可能经过多个服务处理。分布式链路追踪可以记录请求在微服务间的完整调用链，帮助开发者：
- **问题排查**：快速定位哪个服务出现问题
- **性能分析**：识别性能瓶颈
- **依赖关系**：了解服务间的调用关系
- **调用链可视化**：通过 Zipkin UI 查看完整的调用链

### 为什么使用 Micrometer Tracing + Zipkin？

**Spring Boot 3.x 的变化**：
- Spring Boot 3.x 不再支持旧版 Spring Cloud Sleuth
- 使用 Micrometer Tracing 作为新的追踪标准
- Micrometer Tracing 支持多种追踪后端（Zipkin、Jaeger、Wavefront 等）

**技术选型**：
- **Micrometer Tracing**：Spring Boot 3.x 官方推荐的追踪库
- **Brave**：作为追踪桥接器（Tracing Bridge）
- **Zipkin**：分布式追踪系统，提供 UI 界面

### 架构设计

```
┌─────────────────────────────────────────┐
│         Zipkin Server (9411)            │
│  ┌───────────────────────────────────┐  │
│  │  收集和存储追踪数据                │  │
│  │  提供 UI 界面查看调用链            │  │
│  └───────────────────────────────────┘  │
└─────────────────────────────────────────┘
              ↑
              │ HTTP
              │
┌─────────────┴─────────────┐
│   各微服务（Gateway、Auth、│
│   Order、Commodity 等）   │
│  - Micrometer Tracing      │
│  - Brave Bridge            │
│  - Zipkin Reporter         │
└────────────────────────────┘
```

### 实现细节

#### 1. 依赖配置

**父 pom.xml**:
```xml
<!-- 注意：Micrometer Tracing 和 Zipkin Reporter 的版本由 Spring Boot 3.2.0 自动管理 -->
<!-- 如果需要覆盖版本，可以取消注释以下配置 -->
<!--
<properties>
    <micrometer-tracing.version>1.2.1</micrometer-tracing.version>
    <zipkin-reporter.version>3.3.0</zipkin-reporter.version>
</properties>

<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>io.micrometer</groupId>
            <artifactId>micrometer-tracing-bom</artifactId>
            <version>${micrometer-tracing.version}</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
        <dependency>
            <groupId>io.zipkin.reporter2</groupId>
            <artifactId>zipkin-reporter-bom</artifactId>
            <version>${zipkin-reporter.version}</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
-->
```

**各服务的 pom.xml**:
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

#### 2. 配置说明

**⚠️ 重要提示**：Spring Boot 3.2 中 Zipkin 配置的正确路径是 `management.zipkin.tracing.endpoint`，而不是 `management.tracing.export.zipkin.endpoint`。

**Config Server 配置** (`{application}.yml`):
```yaml
management:
  tracing:
    sampling:
      probability: 1.0  # 100% 采样率（生产环境建议降低到 0.1）
  # ✅ 正确的配置路径：management.zipkin.tracing.endpoint
  zipkin:
    tracing:
      endpoint: http://zipkin:9411/api/v2/spans
      connect-timeout: 10000  # 连接超时配置（毫秒）
      read-timeout: 10000  # 读取超时配置（毫秒）
```

**配置说明**:
- `management.tracing.sampling.probability`: 采样率（0.0-1.0）
  - `1.0`: 100% 采样（开发环境推荐）
  - `0.1`: 10% 采样（生产环境推荐，减少性能开销）
- `management.zipkin.tracing.endpoint`: Zipkin 服务端点（**注意路径**）
- `management.zipkin.tracing.connect-timeout`: 连接超时时间（毫秒）
- `management.zipkin.tracing.read-timeout`: 读取超时时间（毫秒）

#### 3. Docker Compose 配置

**Zipkin 服务**:
```yaml
zipkin:
  image: openzipkin/zipkin:latest
  container_name: njumarket-zipkin
  ports:
    - "9411:9411"
  environment:
    - STORAGE_TYPE=mem  # 使用内存存储（生产环境建议使用 Elasticsearch 或 MySQL）
  networks:
    - njumarket-network
  healthcheck:
    test: ["CMD-SHELL", "wget --spider -q http://localhost:9411/ || exit 1"]
    interval: 10s
    timeout: 5s
    retries: 5
    start_period: 40s
```

**各服务环境变量**（简化配置，依赖 Config Server）:
```yaml
environment:
  - SPRING_PROFILES_ACTIVE=dev
  - SPRING_CONFIG_IMPORT=optional:configserver:http://config-server:8888
  # 其他必要的环境变量（数据库、Redis、Eureka 等）
  # ✅ 注意：所有 Zipkin 配置都通过 Config Server 管理，不在 Docker Compose 中显式配置
```

**配置优先级说明**（从高到低）:
1. **系统属性** (`JAVA_TOOL_OPTIONS` 中的 `-D` 参数) - 最高优先级
2. **环境变量** (`SPRING_APPLICATION_JSON`, `MANAGEMENT_ZIPKIN_TRACING_ENDPOINT`)
3. **Config Server 配置** (`{application}.yml`) - **推荐使用**
4. **本地 application.yml** - 最低优先级

**⚠️ 重要提示**：项目已完全依赖 Config Server 进行配置管理，Docker Compose 中不再显式配置 Zipkin 相关环境变量，避免新旧生态配置冲突。

#### 4. 追踪数据流程

```
用户请求
  ↓
Gateway（生成 Trace ID）
  ↓
服务A（传播 Trace ID）
  ↓
服务B（传播 Trace ID）
  ↓
服务C（传播 Trace ID）
  ↓
所有服务将追踪数据发送到 Zipkin
  ↓
Zipkin 收集和存储
  ↓
Zipkin UI 展示调用链
```

### 使用指南

#### 1. 启动服务

**Docker 环境**:
```bash
cd njumarket
docker-compose up -d --build
```

**访问 Zipkin UI**:
```
http://localhost:9411
```

#### 2. 查看追踪数据

**在 Zipkin UI 中**:
1. 打开 `http://localhost:9411`
2. 选择服务名称（如 `njumarket-gateway`）
3. 点击 "Find Traces" 查看追踪数据
4. 点击某个 Trace 查看完整的调用链

**调用链示例**:
```
Gateway (8080)
  ├─ Auth Service (8091) - 验证用户
  ├─ Commodity Service (8092) - 查询商品
  └─ Order Service (8093)
      ├─ Commodity Service (8092) - 更新库存
      └─ Notification Service (8097) - 发送通知
```

#### 3. 分析性能

**在 Zipkin UI 中**:
- 查看每个 Span 的耗时
- 识别慢请求（红色标记）
- 分析服务间调用的延迟

**性能指标**:
- **Trace Duration**: 整个请求的总耗时
- **Span Duration**: 单个服务的耗时
- **Service Count**: 涉及的服务数量

#### 4. 采样率调整

**开发环境**（100% 采样）:
```yaml
management:
  tracing:
    sampling:
      probability: 1.0
```

**生产环境**（10% 采样）:
```yaml
management:
  tracing:
    sampling:
      probability: 0.1
```

### 追踪范围

**已集成追踪的服务**:
- ✅ Gateway（API 网关）
- ✅ Auth Service（认证服务）
- ✅ Commodity Service（商品服务）
- ✅ Order Service（订单服务）
- ✅ Message Service（消息服务）
- ✅ Image Service（图片服务）
- ✅ Admin Service（管理服务）
- ✅ Notification Service（通知服务）

**追踪内容**:
- HTTP 请求/响应
- Feign Client 调用
- 数据库查询（通过 Spring Data JPA）
- Redis 操作（通过 Spring Data Redis）

### 生产环境建议

#### 1. 存储后端

**开发环境**（内存存储）:
```yaml
environment:
  - STORAGE_TYPE=mem
```

**生产环境**（Elasticsearch 存储）:
```yaml
zipkin:
  image: openzipkin/zipkin:latest
  environment:
    - STORAGE_TYPE=elasticsearch
    - ES_HOSTS=http://elasticsearch:9200
```

#### 2. 采样率

**生产环境建议**:
- 高流量服务：0.01（1% 采样）
- 普通服务：0.1（10% 采样）
- 关键服务：0.5（50% 采样）

#### 3. 性能优化

- 使用异步发送追踪数据（默认已启用）
- 批量发送追踪数据（减少网络开销）
- 根据业务需求调整采样率

### 故障排查

**问题 1：追踪数据未显示在 Zipkin**

1. **检查 Zipkin 服务是否运行**:
   ```bash
   docker ps | grep zipkin
   curl http://localhost:9411/health
   ```

2. **检查服务配置**（重要：验证配置路径是否正确）:
   ```bash
   # 查看服务配置（检查 endpoint 是否正确）
   curl http://localhost:8093/actuator/configprops | grep -i zipkin
   
   # 应该看到：management.zipkin.tracing.endpoint
   # 如果看到：management.tracing.export.zipkin.endpoint，说明配置路径错误
   ```

3. **检查网络连接**:
   ```bash
   # 在服务容器内测试连接
   docker exec -it njumarket-service-order curl http://zipkin:9411/health
   ```

4. **查看服务日志**:
   ```bash
   docker logs njumarket-service-order | grep zipkin
   ```

**问题 2：服务连接 localhost:9411 而不是 zipkin:9411**

**原因**：配置路径错误或配置优先级问题

**解决方案**：
1. **验证配置路径**：确保使用 `management.zipkin.tracing.endpoint` 而不是 `management.tracing.export.zipkin.endpoint`
2. **检查配置优先级**：使用 `actuator/configprops` 查看实际加载的配置
3. **确保 Config Server 配置正确**：所有 Zipkin 配置应在 Config Server 中统一管理
4. **避免在 Docker Compose 中重复配置**：不要同时使用环境变量和 Config Server 配置，避免冲突

**问题 3：配置路径错误（常见问题）**

**错误配置**（Spring Boot 3.2 不支持）:
```yaml
management:
  tracing:
    export:
      zipkin:
        endpoint: http://zipkin:9411/api/v2/spans  # ❌ 错误路径
```

**正确配置**（Spring Boot 3.2）:
```yaml
management:
  zipkin:
    tracing:
      endpoint: http://zipkin:9411/api/v2/spans  # ✅ 正确路径
```

**验证方法**:
```bash
# 查看实际配置
curl http://localhost:8093/actuator/configprops | grep -A 5 "management.zipkin.tracing"

# 应该看到：
# "prefix": "management.zipkin.tracing"
# "properties": {
#   "endpoint": "http://zipkin:9411/api/v2/spans"
# }
```

### ⚠️ 重要学习点：新旧生态配置冲突问题

> **📖 详细文档**：请参考 [Zipkin 新旧生态配置冲突问题详解](./ZIPKIN_NEW_OLD_ECOSYSTEM_CONFLICT.md)

#### 问题背景

在从 Spring Cloud Sleuth 迁移到 Micrometer Tracing 的过程中，存在大量新旧配置属性混用的情况，这是 Spring Boot 3.x 升级中最容易踩的坑之一。

#### 常见易错点

**1. 配置路径错误**
- ❌ **错误**：`management.tracing.export.zipkin.endpoint`（旧版路径）
- ✅ **正确**：`management.zipkin.tracing.endpoint`（Spring Boot 3.2 正确路径）

**2. 旧配置属性混用**
- ❌ **错误**：`spring.zipkin.base-url`（旧版 Sleuth 配置）
- ✅ **正确**：`management.zipkin.tracing.endpoint`（新版 Micrometer Tracing 配置）

**3. Docker Compose 中显式配置导致冲突**
- ❌ **错误**：在 Docker Compose 中同时使用环境变量和 Config Server 配置
- ✅ **正确**：完全依赖 Config Server 管理配置，Docker Compose 只保留必要的环境变量

**4. AI 生成代码可能过时**
- 许多 AI 工具（包括 ChatGPT、Copilot）的训练数据可能包含旧版配置
- 需要验证配置属性路径是否与当前 Spring Boot 版本匹配
- 使用 `actuator/configprops` 端点验证实际加载的配置

#### 最佳实践

1. **统一配置管理**：所有 Zipkin 配置在 Config Server 中统一管理
2. **避免重复配置**：不要在 Docker Compose 中显式配置 Zipkin 相关环境变量
3. **验证配置路径**：使用 `actuator/configprops` 验证配置是否正确加载
4. **版本匹配检查**：确保配置属性路径与 Spring Boot 版本匹配

#### 配置清理清单

在解决新旧生态冲突后，应删除以下 Docker Compose 中的配置：
- ❌ `ZIPKIN_HOST`、`ZIPKIN_PORT`（旧环境变量）
- ❌ `SPRING_ZIPKIN_BASE_URL`（旧版 Sleuth 配置）
- ❌ `MANAGEMENT_ZIPKIN_TRACING_ENDPOINT`（显式环境变量）
- ❌ `SPRING_APPLICATION_JSON` 中的 Zipkin 配置（显式 JSON 配置）
- ❌ `JAVA_TOOL_OPTIONS` 中的 `-Dspring.zipkin.base-url`（旧系统属性）

**保留的配置**：
- ✅ `SPRING_PROFILES_ACTIVE=dev`（激活环境）
- ✅ `SPRING_CONFIG_IMPORT=optional:configserver:...`（连接 Config Server）
- ✅ 数据库、Redis、Eureka 等必要的环境变量
- ✅ 字符编码相关的环境变量

### 技术总结

#### 学习收获

1. **Micrometer Tracing**: 理解了 Spring Boot 3.x 的新追踪标准
2. **Brave Bridge**: 掌握了追踪桥接器的使用
3. **Zipkin 集成**: 学会了分布式追踪系统的集成方法
4. **采样率调优**: 理解了采样率对性能的影响
5. **调用链分析**: 学会了通过调用链定位问题
6. **配置路径验证**: 学会了使用 `actuator/configprops` 验证配置是否正确
7. **配置优先级**: 理解了 Spring Boot 配置加载的优先级顺序
8. **新旧生态迁移**: 深刻理解了从 Sleuth 到 Micrometer Tracing 的迁移陷阱和解决方案
9. **配置管理最佳实践**: 学会了统一配置管理和避免配置冲突的方法

#### 技术栈

**新增技术**:
- Micrometer Tracing: 分布式追踪库
- Brave: 追踪桥接器
- Zipkin: 分布式追踪系统

**技术版本**:
- Micrometer Tracing: 1.2.1（由 Spring Boot 3.2.0 自动管理）
- Zipkin Reporter: 3.3.0（由 Spring Boot 3.2.0 自动管理）
- Zipkin Server: latest

**重要配置路径**（Spring Boot 3.2）:
- ✅ 正确：`management.zipkin.tracing.endpoint`
- ❌ 错误：`management.tracing.export.zipkin.endpoint`

**集成统计**:
- **8 个服务**：全部配置追踪
- **1 个 Zipkin 服务**：提供追踪数据收集和 UI

---

## 后续版本规划

### v2.1.2（部分完成）🔄

**目标**: 可观测性增强

**完成内容**:
1. **分布式链路追踪** ✅
   - 集成 Micrometer Tracing + Zipkin（Spring Boot 3.x 使用 Micrometer Tracing 替代旧版 Sleuth）
   - 追踪请求在微服务间的完整调用链
   - 分析服务调用性能
   - 所有微服务已配置追踪
   - Zipkin 服务已集成到 Docker Compose
   - **配置路径修复**（2025-11-13）：
     - 修复了 Spring Boot 3.2 中 Zipkin 配置路径错误的问题
     - 正确路径：`management.zipkin.tracing.endpoint`（不是 `management.tracing.export.zipkin.endpoint`）
     - 添加了配置优先级说明和故障排查指南
     - 更新了环境变量和系统属性配置方式
   - **新旧生态配置冲突解决**（2025-11-13）：
     - 清理了 Docker Compose 中所有显式的 Zipkin 配置环境变量
     - 完全依赖 Config Server 进行配置管理，避免新旧配置冲突
     - 删除了旧版 Sleuth 配置（`spring.zipkin.base-url` 等）
     - 添加了重要的学习点和易错点说明

**详细说明**: 见 [Micrometer Tracing + Zipkin 分布式链路追踪详解](#micrometer-tracing--zipkin-分布式链路追踪详解)

**待完成内容**:
2. **监控指标收集和可视化** ⏳
   - 集成 Prometheus（指标收集）
   - 集成 Grafana（可视化面板）
   - 监控 JVM、HTTP、数据库等指标
   - 配置告警规则
   - 目标：实时监控系统状态

**详细TODO清单**:

#### Prometheus 集成（预计 3-5 天）

**后端配置**:
- [ ] 在所有服务的 `pom.xml` 中添加 `micrometer-registry-prometheus` 依赖
- [ ] 在 Config Server 配置中启用 Prometheus 端点
  - `management.endpoints.web.exposure.include: prometheus`
  - `management.metrics.export.prometheus.enabled: true`
- [ ] 配置 Prometheus 抓取间隔和超时时间
- [ ] 验证各服务的 `/actuator/prometheus` 端点可访问

**Prometheus 服务**:
- [ ] 在 `docker-compose.yml` 中添加 Prometheus 服务
- [ ] 配置 `prometheus.yml` 抓取配置
  - 配置所有微服务的抓取目标
  - 设置抓取间隔（建议 15s）
  - 配置标签（service、environment 等）
- [ ] 配置 Prometheus 数据持久化（volume）
- [ ] 配置 Prometheus 健康检查

**指标收集范围**:
- [ ] JVM 指标（内存、GC、线程等）
- [ ] HTTP 指标（请求数、响应时间、错误率等）
- [ ] 数据库指标（连接池、查询时间等）
- [ ] Redis 指标（连接数、操作数等）
- [ ] 自定义业务指标（订单数、商品数等）

#### Grafana 集成（预计 2-3 天）

**Grafana 服务**:
- [ ] 在 `docker-compose.yml` 中添加 Grafana 服务
- [ ] 配置 Grafana 数据源（连接 Prometheus）
- [ ] 配置 Grafana 数据持久化（volume）
- [ ] 配置 Grafana 健康检查

**仪表板创建**:
- [ ] 创建系统概览仪表板
  - 服务健康状态
  - 请求总数和 QPS
  - 错误率
  - 响应时间分布
- [ ] 创建 JVM 监控仪表板
  - 堆内存使用
  - GC 频率和耗时
  - 线程数
- [ ] 创建数据库监控仪表板
  - 连接池状态
  - 查询性能
  - 慢查询统计
- [ ] 创建业务指标仪表板
  - 订单创建数
  - 商品发布数
  - 用户活跃度

**告警规则配置**:
- [ ] 配置服务宕机告警
- [ ] 配置高错误率告警（> 5%）
- [ ] 配置高响应时间告警（P99 > 1s）
- [ ] 配置 JVM 内存告警（堆内存使用 > 80%）
- [ ] 配置数据库连接池告警（连接数 > 80%）
- [ ] 配置告警通知渠道（邮件、钉钉、企业微信等）

#### 文档和测试（预计 1-2 天）

- [ ] 更新项目文档，添加 Prometheus 和 Grafana 使用说明
- [ ] 编写监控指标说明文档
- [ ] 测试告警规则是否正常工作
- [ ] 验证数据持久化和恢复功能

**预计时间**: 1-2 周

### v2.1.3（计划中）

**目标**: 系统优化和问题修复

**计划内容**:
1. **健康检查机制优化** ⚠️
   - **问题描述**：健康检查端点没有 token 认证，导致返回的数据格式不合适
   - **影响范围**：所有服务的 `/actuator/health` 端点
   - **问题分析**：
     - 健康检查请求不经过 JWT 认证（`/actuator/**` 路径被排除）
     - 某些健康检查可能需要用户上下文或认证信息
     - 返回的数据格式可能不符合预期（缺少认证相关的响应头或数据）
   - **解决方案**：
     - 为健康检查端点配置独立的认证机制（如 API Key 或 Basic Auth）
     - 或配置健康检查端点返回标准化的响应格式（不依赖认证状态）
     - 区分内部健康检查（Docker、Eureka）和外部健康检查（监控系统）
   - **涉及服务**：
     - Gateway：健康检查路由配置
     - 所有业务服务：健康检查端点配置
   - **目标**：确保健康检查返回的数据格式正确且安全

**预计时间**: 3-5 天

### v2.2.x（计划中）

**目标**: 异步处理能力

**计划内容**:
1. **消息队列集成**
   - 集成 RabbitMQ 或 Kafka
   - 实现异步消息处理
   - 实现事件驱动架构
   - 解耦服务间通信
   - 目标：提高系统解耦和性能

**预计时间**: 2-3 周

### v2.3.x（计划中）

**目标**: 地址框架与地理位置服务

**计划内容**:
1. **地址管理系统**
   - 用户地址管理（多地址支持、默认地址）
   - 地址标准化和验证
   - 地址选择器组件（省市区三级联动）
   - 目标：提升用户体验，支持地理位置相关功能

2. **地理位置服务**
   - 集成地图API（高德/百度/腾讯）
   - 地址转经纬度（地理编码）
   - 经纬度转地址（逆地理编码）
   - 距离计算功能
   - 目标：为智能推荐提供地理位置数据

3. **商品位置扩展**
   - 商品发布时选择/输入位置
   - 商品位置信息展示
   - 基于位置的商品搜索
   - 目标：支持基于距离的商品推荐

4. **订单地址优化**
   - 订单创建时从地址列表选择
   - 订单详情显示完整地址信息
   - 地址快照（订单创建时保存地址快照）
   - 目标：提高订单地址准确性

**详细TODO清单**:

#### 后端开发（6-10天）

**数据库设计**:
- [ ] 创建 `user_addresses` 表（用户地址表）
  - `address_id` (主键)
  - `user_id` (用户ID，外键)
  - `province` (省)
  - `city` (市)
  - `district` (区/县)
  - `street` (街道/详细地址)
  - `postal_code` (邮编)
  - `latitude` (纬度，DECIMAL(10,7))
  - `longitude` (经度，DECIMAL(10,7))
  - `is_default` (是否默认地址)
  - `contact_name` (收货人姓名)
  - `contact_phone` (收货人电话)
  - `create_time`, `update_time`
  - 索引：`idx_user_id`, `idx_location` (latitude, longitude)

- [ ] 扩展 `commodities` 表
  - 添加 `province`, `city`, `district`, `street` 字段
  - 添加 `latitude`, `longitude` 字段
  - 添加空间索引（可选，用于地理查询优化）

- [ ] 扩展 `orders` 表
  - 添加 `address_id` 字段（关联用户地址）
  - 保留 `shipping_address` 字段（作为快照）
  - 添加地址快照字段（省市区详细地址、经纬度）

**服务层开发**:
- [ ] 创建 `AddressService`（地址服务）
  - `createAddress()` - 创建地址
  - `updateAddress()` - 更新地址
  - `deleteAddress()` - 删除地址
  - `getUserAddresses()` - 获取用户地址列表
  - `getAddressById()` - 根据ID获取地址
  - `setDefaultAddress()` - 设置默认地址
  - `geocodeAddress()` - 地址转经纬度（调用地图API）
  - `reverseGeocode()` - 经纬度转地址（调用地图API）
  - `calculateDistance()` - 计算两点距离（Haversine公式）

- [ ] 创建 `AddressController`（地址管理API）
  - `POST /api/user/address/create` - 创建地址
  - `PUT /api/user/address/{id}` - 更新地址
  - `DELETE /api/user/address/{id}` - 删除地址
  - `GET /api/user/address/list` - 获取地址列表
  - `GET /api/user/address/{id}` - 获取地址详情
  - `PUT /api/user/address/{id}/default` - 设置默认地址

- [ ] 创建 `GeographicController`（地理查询API）
  - `GET /api/public/commodity/nearby` - 附近商品查询
    - 参数：`latitude`, `longitude`, `radius` (km)
  - `GET /api/public/commodity/distance` - 计算商品与地址距离
    - 参数：`commodityId`, `addressId`

- [ ] 集成到 `OrderService`
  - 修改 `createOrder()` 方法，支持使用 `addressId`
  - 创建订单时保存地址快照
  - 订单详情返回完整地址信息

- [ ] 集成到 `CommodityService`
  - 修改 `publishCommodity()` 方法，支持位置信息
  - 商品详情返回位置信息
  - 支持基于位置的商品搜索

**地图API集成**:
- [ ] 选择地图服务提供商（推荐：高德地图API）
- [ ] 配置API密钥（使用Config Server管理）
- [ ] 实现 `MapApiClient`（Feign Client或RestTemplate）
  - 地理编码接口
  - 逆地理编码接口
  - 距离计算接口（可选，后端可自行计算）
- [ ] 实现地址解析和验证逻辑
- [ ] 添加缓存机制（避免重复调用API）

**数据迁移**:
- [ ] 编写数据迁移脚本
  - 将现有订单的 `shipping_address` 文本解析为结构化地址（可选）
  - 将现有商品的 `location` 文本解析为结构化地址（可选）
  - 为现有地址数据补充经纬度（调用地图API）

#### 前端开发（8-12天）

**地址选择组件**:
- [ ] 集成地址选择器组件
  - 方案A：使用 `vue-area-linkage` 或类似组件
  - 方案B：使用 Element Plus + 自定义省市区数据
  - 方案C：集成高德/百度地图地址选择器
- [ ] 实现地址表单组件
  - 省市区三级联动
  - 详细地址输入
  - 收货人姓名、电话
  - 设置为默认地址选项
- [ ] 实现地图选点功能（可选）
  - 集成地图组件（高德/百度地图）
  - 点击地图选择位置
  - 自动填充地址信息

**地址管理页面**:
- [ ] 创建 `AddressManagement.vue` 页面
  - 地址列表展示（卡片形式）
  - 添加地址按钮
  - 编辑地址功能
  - 删除地址功能
  - 设置默认地址功能
- [ ] 创建地址表单组件 `AddressForm.vue`
  - 地址选择器
  - 详细地址输入
  - 表单验证
  - 提交处理

**集成到现有页面**:
- [ ] 修改 `CreateOrder.vue`
  - 从地址列表选择地址（替代文本输入）
  - 显示选中地址详情
  - 显示商品与地址的距离（如果有）
  - 支持快速添加新地址
- [ ] 修改 `PublishCommodity.vue`
  - 地址选择器替代位置文本输入
  - 可选地图选点
  - 显示选择的位置信息
- [ ] 修改 `CommodityDetail.vue`
  - 显示商品位置信息
  - 显示与用户默认地址的距离（如果已登录）
  - 地图展示商品位置（可选）
- [ ] 修改 `OrderDetail.vue`
  - 显示完整地址信息（省市区详细地址）
  - 地图展示收货地址（可选）

**工具函数**:
- [ ] 创建地址格式化工具函数
  - `formatAddress()` - 格式化地址显示
  - `formatDistance()` - 格式化距离显示（如：1.5km）
- [ ] 创建地址验证工具函数
  - 验证地址格式
  - 验证必填字段

#### 测试和优化（2-3天）

- [ ] 功能测试
  - 地址CRUD功能测试
  - 地址选择器测试
  - 订单创建流程测试
  - 商品发布流程测试
- [ ] 性能优化
  - 地理查询索引优化
  - 地图API调用缓存
  - 地址列表分页加载
- [ ] 边界情况处理
  - 地址解析失败处理
  - 地图API调用失败降级
  - 地址数据格式兼容

#### Spring AI 推荐集成准备（与AI推荐一起实施）

- [ ] 基于距离的推荐算法
  - 计算用户地址与商品位置的距离
  - 距离作为推荐权重因子
  - 优先推荐附近商品
- [ ] 地理位置分析
  - 分析用户活动区域
  - 分析商品分布区域
  - 区域偏好分析
- [ ] 智能匹配逻辑
  - 结合用户位置、商品位置、用户偏好
  - 生成个性化推荐列表

**预计时间**: 3-4 周

**技术选型**:
- **地图服务**: 高德地图API（推荐）或百度地图API
- **前端组件**: Element Plus + vue-area-linkage 或自定义组件
- **地理计算**: MySQL `ST_Distance_Sphere` 函数或 Java Haversine 公式
- **缓存策略**: Redis 缓存地址解析结果和地理查询结果

**业务价值**:
- **短期**: 提升用户体验，地址管理更规范，订单准确性提高
- **长期**: 为Spring AI智能推荐提供地理位置数据，支持基于距离的推荐算法

### v3.0.x（计划中，需在 v2.3.x 完成后）

**目标**: Spring AI 智能搜索与推荐

**前提条件**：
- ✅ v2.3.x 地址框架与地理位置服务已完成
- ✅ 用户地址数据可用
- ✅ 商品位置数据可用

**计划内容**:
1. **Spring AI 基础设施搭建**
   - 添加 Spring AI 依赖（OpenAI 或 Ollama）
   - 配置向量数据库（PostgreSQL + pgvector 或 Milvus）
   - 配置 Embedding API（OpenAI API 或本地 Ollama）
   - 创建向量表和索引

2. **商品向量化服务**
   - 实现商品向量化服务（`CommodityVectorService`）
   - 商品发布/更新时自动生成向量
   - 历史商品批量向量化（数据迁移）
   - 向量存储和索引优化

3. **语义搜索功能**
   - 实现 AI 搜索服务（`AISearchService`）
   - 替换现有的 `aiSearch()` 方法
   - 支持自然语言查询理解
   - 结合地理位置、价格等筛选条件
   - 优化搜索性能（缓存、索引）

4. **用户画像构建**
   - 实现用户行为分析服务（`UserBehaviorAnalyzer`）
   - 实现用户画像向量化服务（`UserProfileVectorService`）
   - 基于浏览、购买、搜索历史生成用户向量
   - 实时更新用户画像

5. **智能推荐系统**
   - 实现 AI 推荐服务（`AIRecommendationService`）
   - 混合推荐策略：
     - 基于内容的推荐（商品向量相似度）
     - 协同过滤（用户行为相似度）
     - 地理位置推荐（结合地址服务）
     - 时间衰减（新商品权重更高）
   - 优化推荐多样性

**详细TODO清单**:

#### 基础设施准备（预计 1-2 周）

**依赖和配置**:
- [ ] 在父 `pom.xml` 中添加 Spring AI BOM 版本管理
- [ ] 在 Commodity Service 添加 Spring AI 依赖
  - `spring-ai-openai-spring-boot-starter`（或 `spring-ai-ollama-spring-boot-starter`）
  - `spring-ai-pgvector-store-spring-boot-starter`（或 `spring-ai-milvus-store-spring-boot-starter`）
- [ ] 在 Config Server 配置 AI 服务
  - OpenAI API Key（或 Ollama 服务地址）
  - Embedding 模型配置
  - 向量数据库连接配置

**向量数据库**:
- [ ] 方案选择：PostgreSQL + pgvector（推荐）或 Milvus
- [ ] 如果使用 pgvector：
  - [ ] 在 PostgreSQL 中启用 pgvector 扩展
  - [ ] 创建商品向量表（`commodity_vectors`）
  - [ ] 创建用户画像向量表（`user_profile_vectors`）
  - [ ] 创建向量索引（HNSW 或 IVFFlat）
- [ ] 如果使用 Milvus：
  - [ ] 在 `docker-compose.yml` 中添加 Milvus 服务
  - [ ] 配置 Milvus 集合和索引
  - [ ] 配置 Milvus 连接

#### 商品向量化（预计 1 周）

**服务实现**:
- [ ] 创建 `CommodityVectorService` 接口和实现
- [ ] 实现商品特征提取逻辑
  - 提取商品标题、描述、分类、价格等特征
  - 构建商品文本描述（用于向量化）
- [ ] 实现向量生成逻辑
  - 调用 Embedding API 生成向量
  - 处理向量维度（384 或 768）
- [ ] 实现向量存储逻辑
  - 存储向量到向量数据库
  - 关联商品 ID 和向量

**集成到商品服务**:
- [ ] 修改 `CommodityService`（商品发布服务）
  - 商品发布时自动生成向量
  - 商品更新时更新向量
- [ ] 实现异步向量化（避免阻塞主流程）
- [ ] 实现批量向量化脚本（历史商品迁移）

#### 语义搜索（预计 1 周）

**搜索服务实现**:
- [ ] 创建 `AISearchService` 接口和实现
- [ ] 实现查询向量化
  - 用户输入自然语言查询
  - 调用 Embedding API 生成查询向量
- [ ] 实现向量相似度搜索
  - 在向量数据库中搜索相似商品（余弦相似度）
  - 设置相似度阈值过滤
- [ ] 实现混合搜索
  - 结合向量搜索和传统关键词搜索
  - 结合地理位置、价格等筛选条件
  - 结果排序和去重

**替换现有搜索**:
- [ ] 修改 `CommodityQueryServiceImpl.aiSearch()` 方法
- [ ] 优化搜索性能
  - 实现搜索结果缓存
  - 优化向量索引
- [ ] 添加搜索日志和监控

#### 用户画像（预计 1-2 周）

**行为分析服务**:
- [ ] 创建 `UserBehaviorAnalyzer` 服务
- [ ] 实现用户行为数据收集
  - 浏览历史（`recordView`）
  - 购买历史（Order Service）
  - 搜索历史（搜索关键词）
  - 消息交互（Message Service）
- [ ] 实现用户偏好分析
  - 分析用户感兴趣的商品类别
  - 分析用户价格偏好
  - 分析用户地理位置偏好

**用户画像向量化**:
- [ ] 创建 `UserProfileVectorService` 服务
- [ ] 实现用户画像构建
  - 基于行为数据生成用户偏好描述
  - 调用 Embedding API 生成用户向量
- [ ] 实现用户画像存储
  - 存储到用户画像向量表
  - 定期更新用户画像

**实时更新机制**:
- [ ] 用户行为发生时触发画像更新
- [ ] 实现增量更新（避免全量重建）
- [ ] 实现画像更新队列（异步处理）

#### 智能推荐（预计 1-2 周）

**推荐服务实现**:
- [ ] 创建 `AIRecommendationService` 接口和实现
- [ ] 实现基于内容的推荐
  - 基于用户画像向量搜索相似商品
  - 计算商品与用户偏好的相似度
- [ ] 实现协同过滤推荐
  - 找到相似用户（基于用户向量）
  - 推荐相似用户喜欢的商品
- [ ] 实现地理位置推荐
  - 结合用户地址和商品位置
  - 计算距离并作为推荐权重
- [ ] 实现混合推荐算法
  - 组合多种推荐策略
  - 应用时间衰减（新商品权重更高）
  - 优化推荐多样性（避免重复推荐）

**替换现有推荐**:
- [ ] 修改 `CommodityQueryServiceImpl.getRecommendedCommodities()` 方法
- [ ] 实现推荐结果缓存
- [ ] 实现冷启动处理（新用户/新商品）

#### 测试和优化（预计 1 周）

- [ ] 功能测试
  - 语义搜索准确性测试
  - 推荐效果测试
  - 性能测试（响应时间、并发）
- [ ] 性能优化
  - 向量搜索性能优化
  - 缓存策略优化
  - 批量处理优化
- [ ] 成本控制
  - Embedding API 调用频率控制
  - 批量处理减少 API 调用
  - 缓存策略减少重复调用

**涉及的服务和框架**:

**需要修改的服务**:
- **Commodity Service**（核心改动）
  - 商品向量化服务
  - AI 搜索服务
  - AI 推荐服务
- **Auth Service**（用户画像）
  - 用户行为分析
  - 用户画像向量化
- **Order Service**（购买历史）
  - 提供购买历史数据
  - 订单完成后触发画像更新
- **Message Service**（交互数据）
  - 提供用户交互数据

**需要新增的组件**:
- 向量数据库（PostgreSQL + pgvector 或 Milvus）
- Spring AI 框架
- Embedding API（OpenAI 或 Ollama）

**依赖关系**:
- 依赖 v2.3.x 地址服务（地理位置推荐）
- 依赖用户行为数据（浏览、购买、搜索）
- 依赖商品数据（标题、描述、分类、价格、位置）

**版本说明**:
- 这是 v3.0.x 版本的核心功能
- 标志着项目从基础功能向智能化转型

**预计时间**: 5-7 周

**学习价值**:
1. Spring AI 框架使用
2. 向量数据库和向量搜索
3. Embedding 技术和文本向量化
4. 推荐系统算法实现
5. 混合推荐策略设计

### 支线任务

**可能插入的任务**:
- 分布式锁优化（锁续期机制）
- WebSocket 优化（消息持久化）
- 数据库查询优化
- 缓存策略优化

---

## 参考资源

### Resilience4j
- [Resilience4j 官方文档](https://resilience4j.readme.io/)
- [Spring Cloud Circuit Breaker](https://spring.io/projects/spring-cloud-circuitbreaker)

### Spring Cloud Config
- [Spring Cloud Config 官方文档](https://spring.io/projects/spring-cloud-config)
- [Config Server 配置指南](https://docs.spring.io/spring-cloud-config/docs/current/reference/html/)

### 相关文档
- [CONFIG_SERVER_INTEGRATION.md](./CONFIG_SERVER_INTEGRATION.md) - Config Server 详细集成文档

---

**版本**: v2.1.2  
**状态**: ✅ **已完成**  
**日期**: 2025-11-12

---

## 版本历史

- **v2.1.1** (2025-11-12): Resilience4j 熔断降级、Config Server 配置中心、环境隔离 ✅ **已完成**
- **v2.1.2** (2025-11-12): Micrometer Tracing + Zipkin 分布式链路追踪 ✅ **已完成**

