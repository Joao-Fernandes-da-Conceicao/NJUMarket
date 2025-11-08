# NJUMarket 微服务架构文档

## 📋 目录
- [架构概述](#架构概述)
- [微服务划分](#微服务划分)
- [技术栈](#技术栈)
- [服务通信](#服务通信)
- [数据管理](#数据管理)
- [部署架构](#部署架构)
- [配置管理](#配置管理)

---

## 架构概述

NJUMarket v2.0 采用微服务架构，将原有的单体应用拆分为多个独立的服务，每个服务专注于特定的业务领域。

### 架构图

```
┌─────────────────────────────────────────────────────────────┐
│                     前端应用层                                │
│  (Vue 3 用户端 + 管理端)                                      │
└──────────────────────┬──────────────────────────────────────┘
                       │ HTTP/WebSocket
┌──────────────────────▼──────────────────────────────────────┐
│                  API Gateway (8080)                          │
│              Spring Cloud Gateway                            │
│  - 路由转发                                                  │
│  - 负载均衡                                                  │
│  - 统一鉴权                                                  │
└──────┬───────────┬───────────┬───────────┬─────────────────┘
       │           │           │           │
       │           │           │           │
┌──────▼───┐ ┌─────▼────┐ ┌───▼────┐ ┌───▼──────┐
│ Discovery │ │  Auth    │ │Commodity│ │  Order  │
│  (8761)   │ │ (8081)   │ │ (8082)  │ │ (8083)  │
└───────────┘ └──────────┘ └─────────┘ └─────────┘
                              │           │
                              │           │
                         ┌────▼───────────▼────┐
                         │    Message (8084)    │
                         └─────────────────────┘
```

### 核心组件

1. **服务注册与发现 (Eureka)**
   - 端口: 8761
   - 作用: 服务注册、服务发现、健康检查

2. **API网关 (Spring Cloud Gateway)**
   - 端口: 8080
   - 作用: 统一入口、路由转发、负载均衡、鉴权

3. **认证服务 (auth-service)**
   - 端口: 8081
   - 职责: 用户认证、授权、用户管理、管理员管理

4. **商品服务 (commodity-service)**
   - 端口: 8082
   - 职责: 商品管理、商品查询、图片管理

5. **订单服务 (order-service)**
   - 端口: 8083
   - 职责: 订单管理、订单查询、投诉处理

6. **消息服务 (message-service)**
   - 端口: 8084
   - 职责: 消息发送、会话管理、WebSocket推送

7. **公共模块 (njumarket-common)**
   - 职责: 共享实体、DTO、工具类、异常处理

---

## 微服务划分

### 1. njumarket-discovery (服务注册中心)

**职责**: Eureka服务注册与发现

**技术栈**:
- Spring Cloud Netflix Eureka Server

**配置**:
- 端口: 8761
- 不向自身注册

### 2. njumarket-gateway (API网关)

**职责**:
- 统一API入口
- 路由转发
- 负载均衡
- 统一鉴权（可选）

**路由规则**:
```
/auth/**          -> auth-service
/commodity/**     -> commodity-service
/order/**         -> order-service
/message/**       -> message-service
```

**技术栈**:
- Spring Cloud Gateway
- Spring Cloud Netflix Eureka Client

### 3. njumarket-service-auth (认证服务)

**职责**:
- 用户注册、登录、登出
- 用户信息管理
- 用户档案管理
- 管理员认证与管理
- 密码管理
- JWT Token生成与验证

**数据实体**:
- User
- UserProfile
- Admin

**Repository**:
- UserRepository
- UserProfileRepository
- AdminRepository

**Service**:
- UserService
- UserProfileService
- AdminService
- PasswordService

**Controller**:
- UserAuthController
- UserProfileController
- AdminController

### 4. njumarket-service-commodity (商品服务)

**职责**:
- 商品发布、编辑、删除
- 商品查询（公开查询）
- 商品状态管理（上架/下架）
- 商品可见性管理
- 图片上传与管理
- 商品快照管理

**数据实体**:
- Commodity
- CommoditySnapshot
- ImageReference

**Repository**:
- CommodityRepository
- CommoditySnapshotRepository
- ImageReferenceRepository

**Service**:
- CommodityService
- CommodityQueryService
- ImageService
- ImageReferenceService

**Controller**:
- UserCommodityController
- PublicController (商品查询部分)
- ImageController

### 5. njumarket-service-order (订单服务)

**职责**:
- 订单创建、支付、发货、确认收货
- 订单查询（买家/卖家）
- 订单状态管理
- 订单可见性管理
- 退货/退款处理
- 投诉处理
- 订单快照管理

**数据实体**:
- Order
- OrderSnapshot
- Complaint

**Repository**:
- OrderRepository
- OrderSnapshotRepository
- ComplaintRepository

**Service**:
- OrderService
- ComplaintService

**Controller**:
- UserOrderController
- UserComplaintController

### 6. njumarket-service-message (消息服务)

**职责**:
- 消息发送与接收
- 会话管理
- 消息查询与搜索
- WebSocket实时推送
- 联系方式管理

**数据实体**:
- Message
- Conversation
- ContactInfo

**Repository**:
- MessageRepository
- ConversationRepository

**Service**:
- MessageService
- ContactService
- WebSocketRetryService

**Controller**:
- UserMessageController
- ChatDataController
- ContactController

### 7. njumarket-common (公共模块)

**职责**: 提供共享代码，避免重复

**包含内容**:
- **实体类 (entity)**: 所有JPA实体
- **DTO (dto)**: 数据传输对象
- **工具类 (utils)**: JwtUtils, RedisConstants, RegexUtils, UserHolder, BusinessValidator
- **异常类 (exception)**: BusinessException
- **注解 (annotation)**: @CurrentUser, @CurrentAdmin

**依赖**:
- Spring Boot Starter Validation
- JWT相关库
- Jackson
- Hutool
- Commons Lang3

---

## 技术栈

### 后端技术栈

- **框架**: Spring Boot 3.2.0
- **服务注册**: Spring Cloud Netflix Eureka
- **API网关**: Spring Cloud Gateway
- **服务通信**: 
  - REST API (同步)
  - WebSocket (实时推送)
  - Feign Client (可选，用于服务间调用)
- **数据持久化**: Spring Data JPA + MySQL
- **缓存**: Redis
- **安全**: Spring Security + JWT
- **文档**: SpringDoc OpenAPI (Swagger)

### 前端技术栈

- **框架**: Vue 3
- **状态管理**: Pinia
- **UI组件**: Element Plus
- **HTTP客户端**: Axios

---

## 服务通信

### 1. 同步通信 (REST API)

**场景**: 
- 前端通过Gateway调用后端服务
- 服务间调用（如订单服务调用商品服务查询商品信息）

**实现方式**:
- Spring Cloud Gateway路由转发
- Feign Client（服务间调用）

**示例**:
```java
// 订单服务调用商品服务
@FeignClient(name = "commodity-service")
public interface CommodityClient {
    @GetMapping("/commodity/{commodityId}")
    Result getCommodity(@PathVariable String commodityId);
}
```

### 2. 异步通信 (WebSocket)

**场景**:
- 实时消息推送
- 订单状态变更通知

**实现方式**:
- WebSocket连接（消息服务）
- Redis发布订阅（可选，用于跨服务消息推送）

### 3. 数据一致性

**策略**:
- **最终一致性**: 通过事件驱动或定时同步
- **分布式事务**: 对于关键操作，考虑使用Seata或Saga模式

---

## 数据管理

### 数据库设计

**原则**: 每个服务拥有独立的数据库（可选，当前版本可共享数据库）

**数据库划分**:
- `njumarket_auth`: 用户、管理员、用户档案
- `njumarket_commodity`: 商品、商品快照、图片引用
- `njumarket_order`: 订单、订单快照、投诉
- `njumarket_message`: 消息、会话、联系方式

**共享数据**:
- 用户基本信息（User）: 所有服务都需要，通过auth-service提供API
- 商品快照: 订单服务需要，通过commodity-service提供API

### 缓存策略

**Redis使用**:
- JWT Token存储
- 验证码存储
- 用户会话信息
- 商品缓存（可选）
- 消息推送重试队列

---

## 部署架构

### 开发环境

```
┌─────────────────────────────────────────┐
│  Eureka Server (8761)                  │
└─────────────────────────────────────────┘
┌─────────────────────────────────────────┐
│  Gateway (8080)                         │
└─────────────────────────────────────────┘
┌──────┐ ┌──────┐ ┌──────┐ ┌──────┐
│ Auth │ │Commo│ │Order │ │Message│
│ 8081 │ │ 8082 │ │ 8083 │ │ 8084 │
└──────┘ └──────┘ └──────┘ └──────┘
```

### 生产环境建议

1. **服务实例**: 每个服务至少2个实例（高可用）
2. **负载均衡**: 通过Gateway或Nginx实现
3. **数据库**: 主从复制（读写分离）
4. **缓存**: Redis集群
5. **监控**: Spring Boot Actuator + Prometheus + Grafana
6. **日志**: ELK Stack (Elasticsearch + Logstash + Kibana)

---

## 配置管理

### 服务端口分配

| 服务 | 端口 | 说明 |
|------|------|------|
| Eureka Server | 8761 | 服务注册中心 |
| Gateway | 8080 | API网关 |
| Auth Service | 8081 | 认证服务 |
| Commodity Service | 8082 | 商品服务 |
| Order Service | 8083 | 订单服务 |
| Message Service | 8084 | 消息服务 |

### 配置文件结构

每个服务都有独立的 `application.yml`，包含：
- 服务名称
- 端口配置
- Eureka客户端配置
- 数据库配置
- Redis配置
- JWT配置

### 环境变量

建议使用环境变量管理敏感配置：
- 数据库密码
- Redis密码
- JWT密钥

---

## 迁移指南

### 从单体到微服务的迁移步骤

1. ✅ **创建微服务模块结构**
   - 创建父POM
   - 创建各微服务模块
   - 创建公共模块

2. ✅ **迁移共享代码**
   - 实体类 → common模块
   - DTO → common模块
   - 工具类 → common模块
   - 异常类 → common模块

3. ✅ **迁移Repository**
   - 按业务领域迁移到对应服务
   - 更新包名和导入

4. 🔄 **迁移Service和Controller**
   - 按业务领域迁移
   - 处理跨服务调用
   - 更新依赖注入

5. ⏳ **配置和测试**
   - 配置各服务的application.yml
   - 配置Gateway路由
   - 测试服务间通信
   - 端到端测试

### 注意事项

1. **跨服务调用**: 使用Feign Client或RestTemplate
2. **事务管理**: 跨服务事务需要使用分布式事务方案
3. **数据一致性**: 考虑最终一致性，避免强一致性带来的性能问题
4. **服务拆分**: 避免过度拆分，保持服务内聚性
5. **API版本**: 考虑API版本管理，便于后续升级

---

## 后续优化方向

1. **服务监控**: 集成Spring Boot Actuator、Prometheus
2. **链路追踪**: 集成Sleuth/Zipkin
3. **配置中心**: 集成Spring Cloud Config或Nacos
4. **消息队列**: 集成RabbitMQ或Kafka（异步处理）
5. **分布式事务**: 集成Seata（关键业务）
6. **API限流**: Gateway集成限流组件
7. **服务熔断**: 集成Hystrix或Sentinel

---

## 总结

NJUMarket v2.0 微服务架构将原有的单体应用拆分为6个核心服务，每个服务专注于特定的业务领域，提高了系统的可扩展性、可维护性和可部署性。通过API网关统一入口，Eureka实现服务发现，实现了松耦合的微服务架构。

