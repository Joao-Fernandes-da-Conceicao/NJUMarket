# 微服务架构结构性问题分析

## 🔴 严重问题（必须修复）

### 1. 内部API通过Gateway暴露（严重安全风险）

**问题描述**：
- 所有服务的`/api/internal/**`路径都可以通过Gateway访问
- Gateway路由配置中，`/auth/**`会被路由到`auth-service`，但`auth-service`的`InternalController`路径是`/api/internal/**`
- 外部用户可以通过Gateway直接调用内部API

**影响**：
- 安全风险：外部用户可以绕过业务逻辑直接操作数据
- 服务边界被破坏：内部API应该只允许服务间调用

**当前状态**：
```yaml
# Gateway路由配置
- id: auth-service
  uri: lb://njumarket-service-auth
  predicates:
    - Path=/auth/**
  filters:
    - StripPrefix=1  # /auth/** 变成 /api/user/**
```

```java
// auth-service InternalController
@RequestMapping("/api/internal")  // 这个路径可以通过 /auth/api/internal 访问！
```

**解决方案**：
1. **方案1（推荐）**：在Gateway中过滤内部API路径
   ```yaml
   # 在Gateway路由配置中排除内部API
   - id: auth-service
     uri: lb://njumarket-service-auth
     predicates:
       - Path=/auth/**,!/auth/api/internal/**
   ```

2. **方案2**：内部API使用不同的端口或路径前缀
   - 内部API使用`/internal/**`路径
   - Gateway不路由`/internal/**`路径
   - 服务间直接调用（不通过Gateway）

3. **方案3**：在Gateway中添加IP白名单Filter
   - 只允许内网IP访问`/api/internal/**`
   - 其他请求返回403

---

### 2. 共享数据库（违反微服务数据隔离原则）

**问题描述**：
- 所有服务共享同一个MySQL数据库（`nju_market`）
- 虽然按服务划分了Repository，但数据库层面没有隔离

**影响**：
- 服务间耦合：一个服务的数据库变更可能影响其他服务
- 无法独立扩展：无法为不同服务配置不同的数据库资源
- 数据安全：所有服务都能访问所有表（虽然代码层面限制了）

**当前状态**：
```yaml
# 所有服务的application.yml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/nju_market  # 共享数据库
```

**解决方案**：
1. **短期方案**：保持共享数据库，但添加数据库用户权限控制
   - 为每个服务创建独立的数据库用户
   - 只授予访问本服务相关表的权限

2. **长期方案**：拆分数据库
   - `nju_market_auth` - 认证服务数据库
   - `nju_market_commodity` - 商品服务数据库
   - `nju_market_order` - 订单服务数据库
   - `nju_market_message` - 消息服务数据库
   - 使用消息队列或事件总线实现数据同步（如需要）

---

### 3. 实体类在Common模块共享（服务间耦合）

**问题描述**：
- 所有Entity类都在`njumarket-common`模块中
- 服务间通过共享Entity类传递数据
- 一个服务的Entity变更会影响所有服务

**影响**：
- 服务间紧耦合：无法独立演进
- 版本冲突：不同服务可能需要不同版本的Entity
- 数据暴露：服务可能接收到不需要的字段

**当前状态**：
```java
// common模块中的Entity
@Entity
public class User { ... }

@Entity
public class Commodity { ... }

@Entity
public class Order { ... }
```

**解决方案**：
1. **方案1（推荐）**：使用DTO替代Entity
   - 各服务定义自己的DTO类
   - Feign Client使用DTO传递数据
   - 服务内部使用Entity，对外暴露DTO

2. **方案2**：拆分Common模块
   - `njumarket-common-core` - 通用工具类
   - `njumarket-common-entity` - 共享Entity（仅用于数据库映射）
   - `njumarket-common-dto` - 共享DTO（用于服务间通信）

---

## 🟡 中等问题（建议修复）

### 4. Gateway路由路径不匹配

**问题描述**：
- Gateway路由配置：`/auth/**` → `auth-service`（StripPrefix=1）
- Gateway Filter检查：`/auth/**` 和 `/api/user/**`
- 实际请求路径：`/auth/api/user/login` → 变成 `/api/user/login`

**影响**：
- JWT认证Filter可能无法正确匹配路径
- 路径规则混乱，难以维护

**当前状态**：
```yaml
# Gateway路由
- id: auth-service
  predicates:
    - Path=/auth/**
  filters:
    - StripPrefix=1  # /auth/api/user/login → /api/user/login
```

```java
// Gateway Filter
if (!requestURI.startsWith("/auth/") && !requestURI.startsWith("/api/user/")) {
    // 但Gateway接收到的路径是 /auth/api/user/login，不是 /api/user/login
}
```

**解决方案**：
1. 统一路径规则
   - Gateway路由：`/auth/**` → `auth-service`
   - Filter检查：`/auth/**`（Gateway接收到的原始路径）
   - 服务内部：`/api/user/**`（StripPrefix后的路径）

2. 或者调整路由配置
   ```yaml
   - id: auth-service
     uri: lb://njumarket-service-auth
     predicates:
       - Path=/api/user/**,/api/contact/**
     # 不StripPrefix，直接转发
   ```

---

### 5. 缺少服务间调用的认证机制

**问题描述**：
- 服务间通过Feign Client调用时，没有认证机制
- 任何服务都可以调用其他服务的内部API
- 没有服务间调用的Token或密钥验证

**影响**：
- 安全风险：如果某个服务被攻击，可以调用所有其他服务
- 无法追踪服务间调用来源

**解决方案**：
1. **方案1**：使用服务间Token
   - Gateway生成服务间调用Token
   - 各服务验证Token的有效性
   - Token包含服务标识和权限信息

2. **方案2**：使用IP白名单
   - 只允许特定IP范围的服务调用内部API
   - 在Gateway或各服务中配置IP白名单

3. **方案3**：使用mTLS（双向TLS）
   - 服务间使用TLS证书认证
   - 更安全但配置复杂

---

### 6. 缺少Feign Client超时和重试配置

**问题描述**：
- Feign Client没有配置超时时间
- 没有重试机制
- 服务调用失败时无法自动恢复

**影响**：
- 服务调用可能长时间挂起
- 网络抖动时无法自动重试
- 系统容错能力差

**解决方案**：
```yaml
# 在各服务的application.yml中添加
feign:
  client:
    config:
      default:
        connectTimeout: 5000
        readTimeout: 10000
        retryer:
          period: 100
          maxPeriod: 1000
          maxAttempts: 3
```

---

### 7. 缺少服务降级和熔断

**问题描述**：
- 没有服务降级机制
- 没有熔断保护
- 服务调用失败时无法优雅降级

**影响**：
- 一个服务故障可能导致整个系统崩溃
- 无法在服务不可用时提供降级方案

**解决方案**：
- 使用Resilience4j或Sentinel实现熔断
- 为Feign Client添加Fallback类

---

## 🟢 低优先级问题（按需修复）

### 8. 缺少API版本控制

**问题描述**：
- API路径中没有版本号（如`/api/v1/user/**`）
- 无法平滑升级API

**解决方案**：
- 在路径中添加版本号：`/api/v1/user/**`
- Gateway路由时保留版本号

---

### 9. 缺少分布式链路追踪

**问题描述**：
- 无法追踪跨服务调用链路
- 无法分析性能瓶颈

**解决方案**：
- 使用Sleuth + Zipkin
- 或使用SkyWalking

---

### 10. 配置分散

**问题描述**：
- 各服务的配置分散在各自的`application.yml`中
- 修改配置需要重启服务

**解决方案**：
- 使用Spring Cloud Config Server
- 统一管理配置

---

## 📊 优先级总结

### 🔴 立即修复（安全风险）
1. **内部API暴露问题** - 严重安全风险
2. **共享数据库** - 违反微服务原则

### 🟡 近期修复（架构改进）
3. **实体类共享** - 服务间耦合
4. **Gateway路径匹配** - 功能问题
5. **服务间认证** - 安全改进
6. **Feign超时重试** - 稳定性改进

### 🟢 按需修复（优化）
7. **服务降级熔断** - 容错能力
8. **API版本控制** - 可维护性
9. **链路追踪** - 可观测性
10. **配置中心** - 配置管理

---

## 🎯 修复建议

### 第一阶段（1周）
1. 修复内部API暴露问题（Gateway路径过滤）
2. 添加Feign Client超时和重试配置
3. 修复Gateway路径匹配问题

### 第二阶段（2-3周）
4. 实现服务间认证机制
5. 拆分Entity和DTO
6. 添加服务降级和熔断

### 第三阶段（按需）
7. 拆分数据库
8. 添加链路追踪
9. 实现配置中心

