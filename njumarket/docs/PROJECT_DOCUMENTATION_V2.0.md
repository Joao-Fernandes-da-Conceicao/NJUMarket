# 南大集市 NJUMarket v2.0 项目文档

## 📋 目录
- [版本概述](#版本概述)
- [快速启动指南](#快速启动指南)
- [从单体到微服务：不仅仅是代码迁移](#从单体到微服务不仅仅是代码迁移)
- [微服务架构设计](#微服务架构设计)
- [核心连接规范与实现](#核心连接规范与实现)
- [常见问题与解决方案](#常见问题与解决方案)
- [微服务实践教学指南](#微服务实践教学指南)
- [技术栈与配置](#技术栈与配置)
- [2.x版本规划](#2x版本规划)

---

## 版本概述

**NJUMarket v2.0** 是项目的重大架构升级版本，从单体应用架构升级为微服务架构。

### 版本信息
- **版本**: v2.0.0
- **发布日期**: 2024年
- **架构**: 微服务架构
- **状态**: ✅ **完整可用版本** - 架构迁移完成，用户端和管理端功能全部实现并测试通过

### 主要成就

#### 架构升级
- ✅ 从单体应用拆分为7个微服务（包含管理服务）
- ✅ 实现服务注册与发现（Eureka）
- ✅ 实现API网关（Spring Cloud Gateway）
- ✅ 实现服务间通信（Feign Client）
- ✅ 实现统一认证与授权机制（用户端 + 管理端）
- ✅ 实现分布式锁（Redis）
- ✅ 实现WebSocket实时推送

#### 功能完整性
- ✅ **用户端功能**：商品发布、订单管理、实时消息、用户中心等全部功能
- ✅ **管理端功能**：用户管理、商品管理、订单管理、会话管理、消息管理、管理员管理等全部功能
- ✅ **数据同步**：消息软删除时自动更新会话最新消息（用户端和管理端均已实现）

#### 代码组织
- ✅ 公共代码模块化（njumarket-common）
- ✅ 实体类、DTO、工具类统一管理
- ✅ 服务间使用内部DTO通信
- ✅ 统一异常处理机制
- ✅ 管理端直接访问数据库（内部系统，提升性能）

---

## 从单体到微服务：不仅仅是代码迁移

### 为什么需要微服务？

单体应用在业务规模较小时具有开发简单、部署方便的优势。但随着业务增长，单体应用会面临以下问题：

1. **技术栈耦合**：所有模块必须使用相同的技术栈
2. **扩展困难**：只能整体扩展，无法针对特定模块优化
3. **维护成本高**：代码库庞大，修改影响范围广
4. **团队协作困难**：多人同时修改容易产生冲突

微服务架构通过将应用拆分为多个独立服务，解决了这些问题，但同时也引入了新的挑战。

### 微服务架构的核心挑战

从单体到微服务，**不仅仅是代码的物理迁移**，更重要的是建立一套**完整的连接规范**，确保服务之间能够正确、高效、安全地协作。

---

## 微服务架构设计

### 架构图

```
┌─────────────────────────────────────────────────────────────┐
│                   前端应用 (Vue 3)                           │
│             用户端 + 管理端                                  │
└──────────────────────┬──────────────────────────────────────┘
                       │ HTTP/WebSocket
┌──────────────────────▼──────────────────────────────────────┐
│              API Gateway (8080)                            │
│         Spring Cloud Gateway                               │
│  - 路由转发                                                │
│  - 负载均衡                                                │
│  - JWT统一鉴权                                             │
│  - X-User-Id传递                                           │
└──────┬───────────┬───────────┬───────────┬─────────────────┘
       │           │           │           │
       │           │           │           │
┌──────▼───┐ ┌─────▼────┐ ┌───▼────┐ ┌───▼──────┐
│ Discovery │ │  Auth    │ │Commodity│ │  Order  │
│  (8761)   │ │ (8091)   │ │ (8092)  │ │ (8093)  │
│  Eureka   │ │  认证    │ │  商品   │ │  订单   │
└───────────┘ └──────────┘ └─────────┘ └─────────┘
                              │           │
                              │           │
                         ┌────▼───────────▼────┐
                         │    Message (8094)    │
                         │      消息服务        │
                         └─────────────────────┘
```

### 服务划分

| 服务 | 端口 | 职责 | 状态 |
|------|------|------|------|
| njumarket-discovery | 8761 | 服务注册与发现（Eureka Server） | ✅ 完成 |
| njumarket-gateway | 8080 | API网关、统一鉴权 | ✅ 完成 |
| njumarket-service-auth | 8091 | 用户认证、用户管理、管理员管理 | ✅ 完成 |
| njumarket-service-commodity | 8092 | 商品管理、商品查询、图片管理 | ✅ 完成 |
| njumarket-service-order | 8093 | 订单管理、订单查询、投诉处理 | ✅ 完成 |
| njumarket-service-message | 8094 | 消息发送、会话管理、WebSocket推送 | ✅ 完成 |
| njumarket-service-image | 8095 | 图片上传、图片管理 | ✅ 完成 |
| njumarket-service-admin | 8096 | 管理端功能（用户/商品/订单/会话/消息/管理员管理） | ✅ 完成 |
| njumarket-common | - | 公共代码模块（Entity、DTO、工具类） | ✅ 完成 |

---

## 核心连接规范与实现

### 1. 服务注册与发现规范

#### 为什么需要服务发现？

在单体应用中，服务调用是直接的（通过方法调用）。在微服务中，服务是独立的进程，需要通过网络通信。服务发现机制让服务能够动态地找到其他服务的位置。

#### 实现方式

**使用 Eureka 作为服务注册中心**：

```yaml
# 各服务的 application.yml
eureka:
  client:
    service-url:
      defaultZone: http://localhost:8761/eureka/
    register-with-eureka: true
    fetch-registry: true
  instance:
    prefer-ip-address: true
```

**关键配置**：
- `register-with-eureka: true`：服务启动时自动注册到Eureka
- `fetch-registry: true`：从Eureka获取其他服务的位置
- `prefer-ip-address: true`：使用IP地址而非主机名（避免DNS问题）

#### 常见问题

**问题1**：服务启动后无法注册到Eureka
- **原因**：Eureka Server未启动，或网络连接问题
- **解决**：确保Eureka Server先启动，检查端口8761是否被占用

**问题2**：服务注册后立即下线
- **原因**：健康检查失败，或心跳超时
- **解决**：检查服务的健康检查端点，调整心跳间隔

---

### 2. API网关规范

#### 为什么需要API网关？

API网关是微服务架构的**统一入口**，负责：
1. **路由转发**：将请求路由到正确的服务
2. **统一鉴权**：在网关层验证JWT Token，避免每个服务重复实现
3. **负载均衡**：在多个服务实例间分配请求
4. **跨域处理**：统一处理CORS
5. **请求头传递**：将用户信息传递给后端服务

#### 实现方式

**Gateway路由配置**：

```yaml
spring:
  cloud:
    gateway:
      routes:
        # 用户相关接口
        - id: auth-service-user
          uri: lb://njumarket-service-auth
          predicates:
            - Path=/api/user/**
          filters:
            - StripPrefix=1
        
        # 订单相关接口
        - id: order-service
          uri: lb://njumarket-service-order
          predicates:
            - Path=/api/user/order/**
          filters:
            - StripPrefix=1
```

**关键点**：
- `lb://`：使用负载均衡（LoadBalancer）
- `StripPrefix=1`：去掉路径前缀（如 `/api/user/order` → `/order`）
- **路由顺序很重要**：更具体的路径应该放在前面

#### JWT验证与用户信息传递

**Gateway JWT验证流程**：

```java
// JwtAuthenticationFilter.java
public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
    // 1. 提取JWT Token
    String token = extractToken(request);
    
    // 2. 验证Token（格式、过期、Redis验证）
    String userId = validateToken(token);
    
    // 3. 设置X-User-Id请求头，传递给后端服务
    ServerHttpRequest modifiedRequest = request.mutate()
        .header("X-User-Id", userId)
        .build();
    
    return chain.filter(exchange.mutate().request(modifiedRequest).build());
}
```

**关键点**：
- Gateway只验证Token，不查询用户详细信息（避免性能问题）
- 通过`X-User-Id`请求头传递用户ID
- 后端服务根据`X-User-Id`查询完整用户信息

#### 常见问题

**问题1**：路由404错误
- **原因**：路由配置错误，或服务未注册到Eureka
- **解决**：检查路由配置，确认服务已注册

**问题2**：跨域问题
- **原因**：Gateway未配置CORS
- **解决**：在Gateway配置CORS过滤器

---

### 3. 服务间通信规范

#### 为什么需要服务间通信？

在微服务架构中，业务功能被拆分到不同服务，但业务逻辑往往需要跨服务协作。例如：
- 创建订单需要查询商品信息（order-service → commodity-service）
- 创建订单需要查询卖家信息（order-service → auth-service）
- 发送消息需要查询用户信息（message-service → auth-service）

#### 实现方式：Feign Client

**Feign Client声明式调用**：

```java
@FeignClient(name = "njumarket-service-auth", path = "/api/internal")
public interface AuthClient {
    @GetMapping("/user/{userId}")
    Result getUserById(@PathVariable String userId);
    
    @PostMapping("/users/batch")
    Result getUsersByIds(@RequestBody List<String> userIds);
}
```

**关键点**：
- `@FeignClient`：声明这是一个Feign Client
- `name`：服务名称（必须与Eureka注册名称一致）
- `path`：API路径前缀
- 方法签名与Controller方法一致

#### 类型转换问题与解决方案

**问题**：Feign Client返回的`Result.getData()`是`LinkedHashMap`，不能直接转换为Entity

**原因**：JSON反序列化时，Feign不知道目标类型，只能反序列化为`Map`

**解决方案**：使用`ObjectMapper`显式转换

```java
// ❌ 错误方式
User user = (User) result.getData();  // ClassCastException!

// ✅ 正确方式
UserInternalDTO userDTO = objectMapper.convertValue(
    result.getData(),
    new TypeReference<UserInternalDTO>() {}
);
User user = convertUserDTOToEntity(userDTO);
```

**为什么使用内部DTO？**
- 避免服务间直接传递Entity（违反微服务原则）
- 只传递必要字段，减少网络传输
- 版本兼容性更好（Entity变化不影响DTO）

#### 内部DTO设计规范

**CommodityInternalDTO示例**：

```java
@Data
public class CommodityInternalDTO implements Serializable {
    private String commodityId;
    private String sellerId;
    private String title;
    private String description;
    private BigDecimal price;
    private Integer stock;
    private String images;  // ✅ 必须包含所有必要字段
    // ... 其他字段
}
```

**关键点**：
- 实现`Serializable`接口（支持序列化）
- 只包含必要字段（不包含关联对象）
- 字段类型使用基本类型或`BigDecimal`（避免精度问题）

#### 常见问题

**问题1**：`ClassCastException: LinkedHashMap cannot be cast to User`
- **原因**：直接强制类型转换
- **解决**：使用`ObjectMapper.convertValue`转换

**问题2**：`No servers available for service: njumarket-service-auth`
- **原因**：服务未注册到Eureka，或服务名称不匹配
- **解决**：检查Eureka注册中心，确认服务名称一致

**问题3**：Feign调用超时
- **原因**：默认超时时间过短
- **解决**：配置Feign超时时间

```yaml
feign:
  client:
    config:
      default:
        connectTimeout: 5000
        readTimeout: 10000
```

---

### 4. 统一认证与授权规范

#### 为什么需要统一认证？

在单体应用中，认证逻辑集中在一个地方。在微服务中，如果每个服务都实现认证，会导致：
1. **代码重复**：每个服务都要实现JWT验证
2. **性能问题**：每个服务都要查询用户信息
3. **维护困难**：认证逻辑修改需要更新所有服务

#### 实现方式：Gateway统一鉴权 + 后端服务用户上下文

**认证流程**：

```
1. 用户登录 → auth-service
   ↓
2. auth-service生成JWT Token，存储到Redis
   ↓
3. 客户端携带Token访问API
   ↓
4. Gateway验证Token（格式、过期、Redis验证）
   ↓
5. Gateway提取userId，设置X-User-Id请求头
   ↓
6. 后端服务UserContextFilter从X-User-Id获取用户信息
   ↓
7. 后端服务设置SecurityContext和UserHolder
   ↓
8. Controller使用@CurrentUser获取用户
```

#### Gateway层实现

**JwtAuthenticationFilter**：

```java
// 1. 验证Token格式和过期时间
String userId = jwtUtils.getUserIdFromToken(token);

// 2. 验证Token是否在Redis中（防止被撤销）
String cachedToken = redisTemplate.opsForValue().get("login:token:" + userId);
if (!token.equals(cachedToken)) {
    return unauthorizedResponse("Token已被撤销或用户已登出");
}

// 3. 设置X-User-Id请求头
ServerHttpRequest modifiedRequest = request.mutate()
    .header("X-User-Id", userId)
    .build();
```

#### 后端服务层实现

**UserContextFilter**：

```java
// 1. 从X-User-Id获取用户ID
String userId = request.getHeader("X-User-Id");

// 2. 通过Feign Client获取用户信息
Result userResult = authClient.getUserById(userId);
UserInternalDTO userDTO = objectMapper.convertValue(
    userResult.getData(),
    new TypeReference<UserInternalDTO>() {}
);

// 3. 检查账户状态
if (!"ACTIVE".equals(userDTO.getAccountStatus())) {
    return 403 Forbidden;
}

// 4. 设置SecurityContext和UserHolder
UsernamePasswordAuthenticationToken authentication = 
    new UsernamePasswordAuthenticationToken(user, null, null);
SecurityContextHolder.getContext().setAuthentication(authentication);
UserHolder.setUser(user);
```

#### 常见问题

**问题1**：用户登录后访问订单，显示"账号被禁用"
- **原因**：后端服务未检查账户状态
- **解决**：在`UserContextFilter`中添加账户状态检查

**问题2**：`@CurrentUser`注解无法获取用户
- **原因**：`CurrentUserArgumentResolver`未注册
- **解决**：创建`WebMvcConfig`，注册`CurrentUserArgumentResolver`

```java
@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {
    private final CurrentUserArgumentResolver currentUserArgumentResolver;
    
    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(currentUserArgumentResolver);
    }
}
```

**问题3**：WebSocket连接后无法推送消息
- **原因**：WebSocket握手时未传递`X-User-Id`，无法识别用户
- **解决**：在Gateway的`JwtAuthenticationFilter`中处理WebSocket路径，在`WebSocketConfig`中设置Principal

---

### 5. 数据一致性规范

#### 为什么需要数据一致性？

在微服务架构中，一个业务操作可能涉及多个服务的数据修改。例如：
- 创建订单：需要扣减商品库存（commodity-service）和创建订单（order-service）
- 如果只扣减库存，订单创建失败，会导致数据不一致

#### 实现方式：分布式锁 + 数据库事务

**三重保护机制**（防止超卖）：

```java
// 1. Redis分布式锁（跨服务器保护）
String lockKey = "lock:commodity:" + commodityId;
String lockValue = RedisLockUtil.generateLockValue();
boolean lockAcquired = redisLockUtil.tryLock(lockKey, lockValue, 10, 1, 100);

// 2. 数据库悲观锁（SELECT ... FOR UPDATE）
Optional<Commodity> commodity = commodityRepository.findByIdForUpdate(commodityId);

// 3. 条件更新（UPDATE ... WHERE stock >= quantity）
int updateResult = commodityRepository.updateStockWithCondition(commodityId, quantity);
if (updateResult == 0) {
    throw new BusinessException("库存不足");
}
```

**关键点**：
- **分布式锁**：防止多台服务器同时处理同一商品的订单
- **悲观锁**：在事务中锁定商品记录
- **条件更新**：原子性更新，确保库存不会为负

#### 事务管理

**单服务事务**：

```java
@Transactional
public Result createOrder(OrderDTO orderDTO) {
    // 事务内的操作
}
```

**跨服务事务**：
- 当前实现：使用**最终一致性**（补偿机制）
- 未来优化：使用分布式事务框架（如Seata）

#### 常见问题

**问题1**：`Cannot execute statement in a READ ONLY transaction`
- **原因**：`SELECT ... FOR UPDATE`需要写事务，但使用了`@Transactional(readOnly = true)`
- **解决**：移除`readOnly = true`，使用`@Transactional`

**问题2**：订单创建时商品不存在
- **原因**：`getCommodityForUpdate`方法缺少`@Transactional`注解
- **解决**：添加`@Transactional`注解

---

### 6. 配置规范化

#### 为什么需要配置规范？

在微服务架构中，每个服务都有独立的配置文件。如果配置不规范，会导致：
1. **配置不一致**：不同服务使用不同的配置格式
2. **配置缺失**：某些服务缺少必要的配置
3. **配置错误**：配置值错误导致服务无法启动

#### 配置规范清单

**每个服务必须包含的配置**：

1. **服务基本信息**
```yaml
server:
  port: 8091
spring:
  application:
    name: njumarket-service-auth
```

2. **数据库配置**
```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/nju_market
    username: root
    password: ${DB_PASSWORD:password}
```

3. **Redis配置**（如果使用）
```yaml
spring:
  data:
    redis:
      host: ${REDIS_HOST:localhost}
      port: ${REDIS_PORT:6379}
      password: ${REDIS_PASSWORD:hqz20050316}
      database: ${AUTH_REDIS_DATABASE:2}  # 每个服务使用不同的database
```

4. **Eureka配置**
```yaml
eureka:
  client:
    service-url:
      defaultZone: http://localhost:8761/eureka/
```

5. **Feign Client配置**（如果调用其他服务）
```yaml
feign:
  client:
    config:
      default:
        connectTimeout: 5000
        readTimeout: 10000
```

6. **日志配置**
```yaml
logging:
  level:
    com.njumarket.auth.service: INFO
    com.njumarket.auth.controller: INFO
    com.njumarket.auth.filter: INFO
    com.njumarket.njumarket.resolver: INFO
```

#### Redis数据库分配

| 服务 | Redis Database | 用途 |
|------|----------------|------|
| gateway | 2 | Token验证 |
| auth-service | 2 | Token存储 |
| commodity-service | 2 | 缓存 |
| message-service | 3 | 消息缓存 |
| order-service | 4 | 分布式锁 |

**为什么使用不同的database？**
- 避免Key冲突
- 便于管理和监控
- 可以独立设置过期策略

---

## 常见问题与解决方案

### 问题1：用户登录后访问订单，显示"账号被禁用"，前端强制登出

#### 问题分析

**现象**：
- 用户登录成功
- 访问订单列表时返回403 Forbidden
- 前端检测到403，强制登出用户

**根本原因**：
1. Gateway验证JWT Token通过，设置`X-User-Id`请求头
2. 后端服务的`UserContextFilter`从`X-User-Id`获取用户信息
3. **但是**：`UserContextFilter`未检查账户状态
4. 即使账户状态是`ACTIVE`，如果未检查，可能导致其他问题

**解决方案**：
在`UserContextFilter`中添加账户状态检查：

```java
UserInternalDTO userDTO = convertUserDTOToEntity(authClient.getUserById(userId));
if (!"ACTIVE".equals(userDTO.getAccountStatus())) {
    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
    response.getWriter().write("账号已被禁用");
    return;
}
```

---

### 问题2：订单创建时商品不存在

#### 问题分析

**现象**：
- 商品确实存在
- 但创建订单时返回"商品不存在"

**根本原因**：
1. `order-service`调用`commodity-service`的`/api/internal/commodity/{commodityId}/for-update`
2. `InternalController.getCommodityForUpdate`方法使用`findByIdForUpdate`（悲观锁）
3. **但是**：方法缺少`@Transactional`注解，无法执行`SELECT ... FOR UPDATE`

**解决方案**：
添加`@Transactional`注解（不能是`readOnly = true`）：

```java
@GetMapping("/commodity/{commodityId}/for-update")
@Transactional  // ✅ 必须是写事务
public Result getCommodityForUpdate(@PathVariable String commodityId) {
    Optional<Commodity> commodityOpt = commodityRepository.findByIdForUpdate(commodityId);
    // ...
}
```

---

### 问题3：下单时图片URL未正确写入订单快照

#### 问题分析

**现象**：
- 订单创建成功
- 但订单快照中的图片URL为空

**根本原因**：
1. `CommodityInternalDTO`缺少`images`字段
2. `InternalDTOConverter.toInternalDTO(Commodity)`未转换`images`字段
3. `OrderServiceImpl.convertCommodityDTOToEntity`未设置`images`字段
4. `order.createCommoditySnapshot`调用`getFirstImage(commodity.getImages())`时，`images`为`null`

**解决方案**：
1. 在`CommodityInternalDTO`中添加`images`字段
2. 在`InternalDTOConverter`中转换`images`字段
3. 在`OrderServiceImpl.convertCommodityDTOToEntity`中设置`images`字段

---

### 问题4：WebSocket连接成功，但推送失败

#### 问题分析

**现象**：
- WebSocket连接成功
- 但无法向用户推送消息

**根本原因**：
1. Gateway的`JwtAuthenticationFilter`未处理WebSocket路径（`/api/ws/**`）
2. WebSocket握手时未传递`X-User-Id`请求头
3. `WebSocketConfig`中未设置`Principal`，`SimpUserRegistry`无法识别用户

**解决方案**：
1. 在Gateway的`JwtAuthenticationFilter`中添加WebSocket路径处理
2. 在`WebSocketConfig`中添加`WebSocketHandshakeInterceptor`提取`X-User-Id`
3. 在`WebSocketConfig`中添加`WebSocketChannelInterceptor`设置`Principal`

---

### 问题5：对话获取成功，但消息获取失败

#### 问题分析

**现象**：
- `getConversations`返回对话列表
- 但`getConversationDetail`返回空消息列表

**根本原因**：
1. Feign Client返回的`Result.getData()`是`LinkedHashMap`
2. 直接强制类型转换导致`ClassCastException`
3. 异常被捕获，返回空列表

**解决方案**：
使用`ObjectMapper.convertValue`转换类型：

```java
UserInternalDTO userDTO = objectMapper.convertValue(
    userResult.getData(),
    new TypeReference<UserInternalDTO>() {}
);
```

---

## 微服务实践教学指南

### 从单体到微服务：必须实现的规范

#### 1. 服务拆分规范

**原则**：
- **按业务领域拆分**：每个服务负责一个业务领域
- **高内聚、低耦合**：服务内部紧密相关，服务间松散耦合
- **独立部署**：每个服务可以独立部署和扩展

**本项目拆分**：
- `auth-service`：用户认证、用户管理
- `commodity-service`：商品管理
- `order-service`：订单管理
- `message-service`：消息通信

#### 2. 服务发现规范

**必须实现**：
- 服务注册：服务启动时自动注册到注册中心
- 服务发现：通过服务名称查找服务实例
- 健康检查：定期检查服务健康状态

**技术选型**：
- Eureka（本项目使用）
- Consul
- Nacos

#### 3. API网关规范

**必须实现**：
- 统一入口：所有外部请求都通过网关
- 路由转发：根据路径转发到对应服务
- 统一鉴权：在网关层验证JWT Token
- 请求头传递：将用户信息传递给后端服务

**技术选型**：
- Spring Cloud Gateway（本项目使用）
- Zuul
- Kong

#### 4. 服务间通信规范

**必须实现**：
- 使用声明式HTTP客户端（Feign Client）
- 使用内部DTO传输数据（不直接传输Entity）
- 处理类型转换问题（`LinkedHashMap` → DTO）
- 配置超时时间

**技术选型**：
- Feign Client（本项目使用）
- RestTemplate
- WebClient

#### 5. 统一认证规范

**必须实现**：
- Gateway统一验证JWT Token
- 后端服务从请求头获取用户信息
- 设置Spring Security SecurityContext
- 检查账户状态

**技术选型**：
- JWT Token（本项目使用）
- OAuth2
- Session共享

#### 6. 数据一致性规范

**必须实现**：
- 分布式锁（跨服务器保护）
- 数据库悲观锁（事务内保护）
- 条件更新（原子性操作）
- 补偿机制（最终一致性）

**技术选型**：
- Redis分布式锁（本项目使用）
- 数据库事务
- 分布式事务框架（Seata）

#### 7. 配置规范

**必须实现**：
- 统一配置格式
- 环境变量支持
- 配置验证
- 配置文档

**技术选型**：
- Spring Boot配置（本项目使用）
- Spring Cloud Config
- Nacos配置中心

---

### 如何配置连接

#### 1. 服务注册与发现配置

**Eureka Server配置**：

```yaml
# njumarket-discovery/application.yml
server:
  port: 8761
eureka:
  instance:
    hostname: localhost
  client:
    register-with-eureka: false
    fetch-registry: false
```

**Eureka Client配置**（各服务）：

```yaml
eureka:
  client:
    service-url:
      defaultZone: http://localhost:8761/eureka/
    register-with-eureka: true
    fetch-registry: true
  instance:
    prefer-ip-address: true
```

#### 2. API网关配置

**Gateway路由配置**：

```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: auth-service-user
          uri: lb://njumarket-service-auth
          predicates:
            - Path=/api/user/**
          filters:
            - StripPrefix=1
```

**关键点**：
- `lb://`：使用负载均衡
- `StripPrefix=1`：去掉路径前缀
- 路由顺序：更具体的路径放在前面

#### 3. Feign Client配置

**Feign Client接口**：

```java
@FeignClient(name = "njumarket-service-auth", path = "/api/internal")
public interface AuthClient {
    @GetMapping("/user/{userId}")
    Result getUserById(@PathVariable String userId);
}
```

**Feign Client配置**：

```yaml
feign:
  client:
    config:
      default:
        connectTimeout: 5000
        readTimeout: 10000
```

#### 4. Redis配置

**各服务Redis配置**：

```yaml
spring:
  data:
    redis:
      host: ${REDIS_HOST:localhost}
      port: ${REDIS_PORT:6379}
      password: ${REDIS_PASSWORD:hqz20050316}
      database: ${AUTH_REDIS_DATABASE:2}
```

---

### 如何沟通服务

#### 1. 同步通信：Feign Client

**使用场景**：
- 需要立即获取结果
- 操作是幂等的
- 数据量小

**示例**：
```java
@Autowired
private AuthClient authClient;

public User getUser(String userId) {
    Result result = authClient.getUserById(userId);
    UserInternalDTO dto = objectMapper.convertValue(
        result.getData(),
        new TypeReference<UserInternalDTO>() {}
    );
    return convertUserDTOToEntity(dto);
}
```

#### 2. 异步通信：消息队列（未来实现）

**使用场景**：
- 不需要立即获取结果
- 操作可以异步处理
- 需要解耦服务

**技术选型**：
- RabbitMQ
- Kafka
- RocketMQ

#### 3. 实时通信：WebSocket

**使用场景**：
- 实时推送消息
- 在线状态管理

**实现方式**：
- 前端通过SockJS连接WebSocket
- 后端通过STOMP协议推送消息

---

### 要避免的问题

#### 1. 服务间直接调用Repository

**错误示例**：
```java
// ❌ 错误：order-service直接注入commodity-service的Repository
@Autowired
private CommodityRepository commodityRepository;
```

**正确方式**：
```java
// ✅ 正确：通过Feign Client调用
@Autowired
private CommodityClient commodityClient;
```

**原因**：
- 违反微服务原则（服务间应该通过API通信）
- 导致服务间紧耦合
- 无法独立部署和扩展

#### 2. 服务间直接传输Entity

**错误示例**：
```java
// ❌ 错误：直接传输Entity
Result<Commodity> getCommodity(String commodityId);
```

**正确方式**：
```java
// ✅ 正确：使用内部DTO
Result<CommodityInternalDTO> getCommodity(String commodityId);
```

**原因**：
- Entity包含关联对象，序列化问题
- Entity变化会影响所有服务
- 违反微服务数据隔离原则

#### 3. 忽略类型转换问题

**错误示例**：
```java
// ❌ 错误：直接强制类型转换
User user = (User) result.getData();  // ClassCastException!
```

**正确方式**：
```java
// ✅ 正确：使用ObjectMapper转换
UserInternalDTO dto = objectMapper.convertValue(
    result.getData(),
    new TypeReference<UserInternalDTO>() {}
);
```

#### 4. 配置不一致

**错误示例**：
- 不同服务使用不同的Redis database
- 不同服务使用不同的超时时间
- 不同服务使用不同的日志格式

**正确方式**：
- 统一配置格式
- 使用环境变量
- 维护配置文档

#### 5. 忽略账户状态检查

**错误示例**：
```java
// ❌ 错误：未检查账户状态
User user = getUser(userId);
// 直接使用user
```

**正确方式**：
```java
// ✅ 正确：检查账户状态
User user = getUser(userId);
if (!"ACTIVE".equals(user.getAccountStatus())) {
    throw new BusinessException("账号已被禁用");
}
```

---

## 快速启动指南

### 前置要求

- **JDK**: 17+
- **Maven**: 3.6+
- **MySQL**: 8.0+
- **Redis**: 6.0+

### 启动步骤

#### 1. 启动MySQL和Redis

确保MySQL和Redis服务已启动并运行。

#### 2. 初始化数据库

执行 `database/schema.sql` 脚本创建数据库结构：

   ```bash
mysql -u root -p nju_market < database/schema.sql
```

#### 3. 启动Eureka Server

```bash
cd njumarket/njumarket-discovery
   mvn spring-boot:run
   ```

**验证**: 访问 http://localhost:8761

#### 4. 启动各微服务

**方式一：使用启动脚本（推荐）**

   ```bash
# Windows
start-all-services.bat

# Linux/Mac
./start-all-services.sh
```

**方式二：分别启动（用于开发调试）**

```bash
# 终端1 - Auth Service (8091)
cd njumarket/njumarket-service-auth
   mvn spring-boot:run
   
# 终端2 - Commodity Service (8092)
cd njumarket/njumarket-service-commodity
   mvn spring-boot:run
   
# 终端3 - Order Service (8093)
cd njumarket/njumarket-service-order
   mvn spring-boot:run
   
# 终端4 - Message Service (8094)
cd njumarket/njumarket-service-message
mvn spring-boot:run

# 终端5 - Image Service (8095)
cd njumarket/njumarket-service-image
mvn spring-boot:run

# 终端6 - Admin Service (8096)
cd njumarket/njumarket-service-admin
   mvn spring-boot:run
   ```

**方式三：使用IDE**

在IDE中分别运行各服务的Application类：
- `DiscoveryServerApplication` (8761)
- `GatewayApplication` (8080)
- `AuthServiceApplication` (8091)
- `CommodityServiceApplication` (8092)
- `OrderServiceApplication` (8093)
- `MessageServiceApplication` (8094)
- `ImageServiceApplication` (8095)
- `AdminServiceApplication` (8096)

#### 5. 启动Gateway

   ```bash
cd njumarket/njumarket-gateway
   mvn spring-boot:run
   ```

**验证**: 访问 http://localhost:8080

### 验证服务注册

访问 Eureka Dashboard: http://localhost:8761

应看到以下服务：
- `njumarket-gateway`
- `njumarket-service-auth`
- `njumarket-service-commodity`
- `njumarket-service-order`
- `njumarket-service-message`
- `njumarket-service-image`
- `njumarket-service-admin`

### 测试API

通过Gateway访问API：

```bash
# 测试Gateway健康检查
curl http://localhost:8080/actuator/health

# 测试服务路由
curl http://localhost:8080/api/public/commodity/search
```

### 注意事项

1. **启动顺序**: 必须先启动Eureka Server，再启动其他服务
2. **端口占用**: 确保端口8761, 8080, 8091-8096未被占用
3. **数据库**: 确保数据库已创建并配置正确
4. **Redis**: 确保Redis服务运行正常

---

## 技术栈与配置

### 后端技术栈

- **框架**: Spring Boot 3.2.0
- **服务治理**: Spring Cloud 2023.0.3
- **服务注册**: Eureka Server
- **API网关**: Spring Cloud Gateway
- **服务间通信**: OpenFeign
- **数据持久化**: Spring Data JPA + MySQL
- **缓存**: Redis
- **安全**: Spring Security + JWT
- **实时通信**: WebSocket (STOMP over SockJS)

### 前端技术栈

- **框架**: Vue 3
- **状态管理**: Pinia
- **UI组件**: Element Plus
- **HTTP客户端**: Axios
- **WebSocket客户端**: SockJS + STOMP.js

### 部署要求

- **Java**: JDK 17+
- **Maven**: 3.6+
- **MySQL**: 8.0+
- **Redis**: 6.0+

### 配置标准化

#### Redis配置统一

**各服务的Redis数据库分配**：
- `auth-service`: database 2
- `commodity-service`: database 2
- `message-service`: database 3
- `order-service`: database 4
- `gateway`: database 2

**标准配置格式**：
```yaml
spring:
  data:
    redis:
      host: ${REDIS_HOST:localhost}
      port: ${REDIS_PORT:6379}
      password: ${REDIS_PASSWORD:hqz20050316}
      database: ${AUTH_REDIS_DATABASE:2}  # 每个服务使用不同的数据库
```

#### Feign Client配置统一

**标准配置**：
```yaml
feign:
  client:
    config:
      default:
        connectTimeout: 5000  # 连接超时（毫秒）
        readTimeout: 10000     # 读取超时（毫秒）
  compression:
    request:
      enabled: true
    response:
      enabled: true
```

#### 日志配置统一

**标准配置**：
```yaml
logging:
  level:
    com.njumarket.{service}.client: DEBUG      # Feign Client 调用日志
    com.njumarket.{service}.filter: INFO        # Filter 日志
    com.njumarket.{service}.service: INFO       # Service 日志
    com.njumarket.{service}.controller: INFO   # Controller 日志
    com.njumarket.njumarket.resolver: INFO      # 参数解析器日志
```

#### WebMvcConfig配置

所有使用 `@CurrentUser` 注解的服务都需要创建 `WebMvcConfig`：

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

#### 配置规范检查清单

每个微服务都应该包含以下配置：

1. **服务基本信息**
   - `server.port`: 服务端口
   - `spring.application.name`: 服务名称

2. **数据库配置**
   - `spring.datasource.*`: 数据源配置
   - `spring.jpa.*`: JPA 配置

3. **Redis配置**（如果服务使用Redis）
   - `spring.data.redis.*`: Redis连接配置
   - 使用环境变量支持不同环境

4. **Eureka配置**
   - `eureka.client.service-url.defaultZone`: Eureka服务地址

5. **Feign Client配置**（如果服务调用其他服务）
   - `feign.client.config.*`: 超时配置
   - `feign.compression.*`: 压缩配置

6. **日志配置**
   - `logging.level.*`: 日志级别配置
   - 至少包含：service、controller、filter、resolver

7. **WebMvcConfig**（如果使用 `@CurrentUser` 注解）
   - 注册 `CurrentUserArgumentResolver`

#### 内部接口路径规范

- **内部接口**（服务间调用）：`/api/internal`
- **公开接口**（前端调用）：`/api/public` 或 `/api/user`
- **管理接口**：`/api/admin`

#### 服务间调用规范

1. 服务间调用必须使用 Feign Client
2. 禁止直接注入其他服务的 Repository 或 Service
3. 服务间传输数据必须使用内部 DTO，不能直接传输 Entity

---

## 2.x版本规划

### 2.1.x版本（近期，高优先级）

#### 主线：微服务完善

1. **服务间认证机制**
   - 实现服务间Token（Service-to-Service Token）
   - Gateway生成服务间调用Token
   - 各服务验证Token的有效性
   - **目标**：防止未授权服务调用

2. **服务降级和熔断**
   - 使用Resilience4j或Sentinel实现熔断
   - 为Feign Client添加Fallback类
   - 实现优雅降级策略
   - **目标**：提高系统可用性

3. **实体类与DTO分离优化**
   - 完善内部DTO设计
   - 优化类型转换逻辑
   - 统一DTO转换工具
   - **目标**：减少服务间耦合

#### 支线：组件增强

4. **分布式锁优化**
   - 实现锁续期机制
   - 优化锁超时时间
   - 添加锁监控
   - **目标**：提高分布式锁可靠性

5. **WebSocket优化**
   - 实现消息持久化
   - 优化推送性能
   - 添加连接监控
   - **目标**：提高实时通信可靠性

---

### 2.2.x版本（中期，中优先级）

#### 主线：微服务治理

1. **API版本控制**
   - 在路径中添加版本号：`/api/v1/user/**`、`/api/v2/user/**`
   - Gateway路由时保留版本号
   - 支持多版本共存
   - **目标**：支持API平滑升级

2. **分布式链路追踪**
   - 使用Sleuth + Zipkin
   - 或使用SkyWalking
   - 集成到Gateway和各服务中
   - **目标**：提高问题排查效率

3. **配置中心**
   - 使用Spring Cloud Config Server
   - 统一管理配置
   - 支持动态刷新（可选）
   - **目标**：简化配置管理

#### 支线：监控与运维

4. **服务监控**
   - 集成Prometheus + Grafana
   - 监控服务健康、性能指标、错误率
   - 配置告警规则
   - **目标**：提高系统可观测性

5. **日志聚合**
   - 使用ELK Stack (Elasticsearch + Logstash + Kibana)
   - 统一日志格式
   - 实现日志检索和分析
   - **目标**：提高问题排查效率

---

### 2.3.x版本（长期，低优先级）

#### 主线：架构优化

1. **数据库拆分**（可选）
   - 拆分数据库：
     - `nju_market_auth` - 认证服务数据库
     - `nju_market_commodity` - 商品服务数据库
     - `nju_market_order` - 订单服务数据库
     - `nju_market_message` - 消息服务数据库
   - 使用消息队列或事件总线实现数据同步（如需要）
   - **注意**：本项目旨在学习微服务，允许数据库共用，此任务优先级最低

2. **消息队列集成**
   - 使用RabbitMQ或Kafka
   - 实现异步消息处理
   - 实现事件驱动架构
   - **目标**：提高系统解耦和性能

3. **分布式事务**
   - 使用Seata实现分布式事务
   - 支持TCC模式
   - 支持Saga模式
   - **目标**：保证跨服务数据一致性

#### 支线：部署与运维

4. **容器化部署**
   - 使用Docker容器化各服务
   - 使用Docker Compose或Kubernetes编排
   - 实现自动化部署
   - **目标**：简化部署流程

5. **服务网格**（可选）
   - 使用Istio或Linkerd实现服务网格
   - 将服务间通信逻辑下沉到基础设施层
   - **目标**：简化服务间通信管理

---

### 版本规划总结

| 版本 | 主线 | 支线 | 优先级 |
|------|------|------|--------|
| 2.1.x | 服务间认证、熔断、DTO优化 | 分布式锁优化、WebSocket优化 | 高 |
| 2.2.x | API版本控制、链路追踪、配置中心 | 服务监控、日志聚合 | 中 |
| 2.3.x | 数据库拆分、消息队列、分布式事务 | 容器化部署、服务网格 | 低 |

---

## 总结

NJUMarket v2.0 完成了从单体架构到微服务架构的重大升级，**不仅仅是代码的物理迁移**，更重要的是建立了一套**完整的连接规范**：

1. **服务注册与发现**：使用Eureka实现服务动态发现
2. **API网关**：使用Spring Cloud Gateway实现统一入口和鉴权
3. **服务间通信**：使用Feign Client实现声明式HTTP调用
4. **统一认证**：Gateway统一验证JWT，后端服务设置用户上下文（用户端 + 管理端）
5. **数据一致性**：使用分布式锁、悲观锁、条件更新三重保护
6. **配置规范**：统一配置格式，使用环境变量，维护配置文档
7. **管理端架构**：管理服务直接访问数据库（内部系统，提升性能），实现完整的CRUD功能

在迁移过程中，我们遇到了许多问题（用户登出、订单失效、图片URL丢失、管理端功能缺失等），这些问题都源于**微服务连接规范的不完善**。通过逐步完善这些规范，我们最终实现了一个稳定、可靠的微服务系统。

### v2.0版本核心成就

- ✅ **7个微服务全部实现**：Discovery、Gateway、Auth、Commodity、Order、Message、Image、Admin
- ✅ **用户端功能完整**：商品发布、订单管理、实时消息、用户中心等全部功能
- ✅ **管理端功能完整**：用户管理、商品管理、订单管理、会话管理、消息管理、管理员管理等全部功能
- ✅ **数据同步机制**：消息软删除时自动更新会话最新消息（用户端和管理端均已实现）
- ✅ **权限管理**：管理员两级权限（system/administrator），完整的权限控制

**2.x版本的规划**以**微服务完善**为主线，以**组件增强**为支线，逐步提升系统的可用性、可维护性和可扩展性。

---

## 相关资源

- **数据库初始化**: 参见 `database/README.md`
- **测试脚本**: 参见 `scripts/README.md`
- **项目根目录**: 包含启动脚本 `start-all-services.bat` / `start-all-services.sh`
