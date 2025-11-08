# NJUMarket 微服务配置指南

## 📋 概述

本文档详细说明如何配置和启动NJUMarket微服务架构。项目已从单体架构迁移到微服务架构，包含以下服务：

- **Eureka Discovery Server** (端口: 8761)
- **API Gateway** (端口: 8080)
- **Auth Service** (端口: 8091)
- **Commodity Service** (端口: 8092)
- **Order Service** (端口: 8093)
- **Message Service** (端口: 8094)

## 🏗️ 项目结构

```
njumarket/
├── njumarket-parent/          # 父POM
├── njumarket-common/           # 公共模块（Entity、DTO、工具类等）
├── njumarket-discovery/        # Eureka服务注册中心
├── njumarket-gateway/          # API网关
├── njumarket-service-auth/     # 认证服务
├── njumarket-service-commodity/# 商品服务
├── njumarket-service-order/    # 订单服务
└── njumarket-service-message/  # 消息服务
```

## 🔧 环境要求

### 必需环境
- **JDK**: 17或更高版本
- **Maven**: 3.6或更高版本
- **MySQL**: 8.0或更高版本
- **Redis**: 6.0或更高版本

### 可选环境
- **IDE**: IntelliJ IDEA / Eclipse
- **Postman**: 用于API测试

## 📦 依赖服务配置

### 1. MySQL数据库配置

所有微服务共享同一个数据库（`nju_market`），但可以通过环境变量配置不同的数据源。

**默认配置**（可在各服务的`application.yml`中修改）：
```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/nju_market?useSSL=false&useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai
    username: root
    password: password
```

**环境变量配置**（推荐用于生产环境）：
- `AUTH_DATASOURCE_URL`: Auth服务数据库URL
- `AUTH_DATASOURCE_USERNAME`: Auth服务数据库用户名
- `AUTH_DATASOURCE_PASSWORD`: Auth服务数据库密码
- `COMMODITY_DATASOURCE_URL`: Commodity服务数据库URL
- `COMMODITY_DATASOURCE_USERNAME`: Commodity服务数据库用户名
- `COMMODITY_DATASOURCE_PASSWORD`: Commodity服务数据库密码
- `ORDER_DATASOURCE_URL`: Order服务数据库URL
- `ORDER_DATASOURCE_USERNAME`: Order服务数据库用户名
- `ORDER_DATASOURCE_PASSWORD`: Order服务数据库密码
- `MESSAGE_DATASOURCE_URL`: Message服务数据库URL
- `MESSAGE_DATASOURCE_USERNAME`: Message服务数据库用户名
- `MESSAGE_DATASOURCE_PASSWORD`: Message服务数据库密码

### 2. Redis配置

**默认配置**：
```yaml
spring:
  data:
    redis:
      host: localhost
      port: 6379
      password: 
      database: 2  # 各服务使用不同的database
```

**环境变量配置**：
- `REDIS_HOST`: Redis主机地址（默认: localhost）
- `REDIS_PORT`: Redis端口（默认: 6379）
- `REDIS_PASSWORD`: Redis密码（默认: 空）
- `AUTH_REDIS_DATABASE`: Auth服务Redis数据库编号（默认: 2）
- `COMMODITY_REDIS_DATABASE`: Commodity服务Redis数据库编号（默认: 2）
- `MESSAGE_REDIS_DATABASE`: Message服务Redis数据库编号（默认: 3）

## 🚀 启动步骤

### 步骤1: 启动依赖服务

1. **启动MySQL**
   ```bash
   # Windows
   net start MySQL80
   
   # Linux/Mac
   sudo systemctl start mysql
   ```

2. **启动Redis**
   ```bash
   # Windows
   redis-server
   
   # Linux/Mac
   redis-server
   ```

### 步骤2: 启动Eureka Discovery Server

Eureka服务注册中心必须最先启动，其他服务才能注册。

```bash
cd njumarket/njumarket-discovery
mvn spring-boot:run
```

或者使用IDE直接运行：
- 主类: `com.njumarket.discovery.DiscoveryServerApplication`
- 访问地址: http://localhost:8761

### 步骤3: 启动API Gateway

```bash
cd njumarket/njumarket-gateway
mvn spring-boot:run
```

或者使用IDE直接运行：
- 主类: `com.njumarket.gateway.GatewayApplication`
- 访问地址: http://localhost:8080

### 步骤4: 启动业务服务

按以下顺序启动（或并行启动）：

#### 4.1 Auth Service
```bash
cd njumarket/njumarket-service-auth
mvn spring-boot:run
```
- 主类: `com.njumarket.auth.AuthServiceApplication`
- 端口: 8091
- 服务名: `njumarket-service-auth`

#### 4.2 Commodity Service
```bash
cd njumarket/njumarket-service-commodity
mvn spring-boot:run
```
- 主类: `com.njumarket.commodity.CommodityServiceApplication`
- 端口: 8092
- 服务名: `njumarket-service-commodity`

#### 4.3 Order Service
```bash
cd njumarket/njumarket-service-order
mvn spring-boot:run
```
- 主类: `com.njumarket.order.OrderServiceApplication`
- 端口: 8093
- 服务名: `njumarket-service-order`

#### 4.4 Message Service
```bash
cd njumarket/njumarket-service-message
mvn spring-boot:run
```
- 主类: `com.njumarket.message.MessageServiceApplication`
- 端口: 8094
- 服务名: `njumarket-service-message`

## 🔍 验证服务状态

### 1. 检查Eureka服务注册

访问 http://localhost:8761，应该能看到所有已注册的服务：
- `NJUMARKET-GATEWAY`
- `NJUMARKET-SERVICE-AUTH`
- `NJUMARKET-SERVICE-COMMODITY`
- `NJUMARKET-SERVICE-ORDER`
- `NJUMARKET-SERVICE-MESSAGE`

### 2. 测试API Gateway路由

通过Gateway访问服务（所有请求都通过8080端口）：

```bash
# 测试Auth服务
curl http://localhost:8080/auth/api/user/auth/login

# 测试Commodity服务
curl http://localhost:8080/commodity/api/public/commodity/search

# 测试Order服务
curl http://localhost:8080/order/api/user/order/buyer

# 测试Message服务
curl http://localhost:8080/message/api/user/message/conversations
```

## 📝 配置文件说明

### 服务端口配置

| 服务 | 端口 | 说明 |
|------|------|------|
| Eureka Discovery | 8761 | 服务注册中心 |
| API Gateway | 8080 | 统一入口 |
| Auth Service | 8091 | 认证服务（原8081，已更改） |
| Commodity Service | 8092 | 商品服务（原8082，已更改） |
| Order Service | 8093 | 订单服务 |
| Message Service | 8094 | 消息服务 |

**注意**: 由于8081和8082端口被前端占用，微服务端口已调整为8091和8092。

### Gateway路由配置

Gateway将所有请求路由到对应的微服务：

- `/auth/**` → `njumarket-service-auth`
- `/commodity/**` → `njumarket-service-commodity`
- `/order/**` → `njumarket-service-order`
- `/message/**` → `njumarket-service-message`

**路径转换规则**: Gateway会自动去除路径前缀（`StripPrefix=1`），例如：
- 请求: `http://localhost:8080/auth/api/user/auth/login`
- 实际转发到: `http://njumarket-service-auth/api/user/auth/login`

## 🔐 安全配置

### JWT Token认证

所有需要认证的接口都需要在请求头中携带JWT Token：

```
Authorization: Bearer <token>
```

### 公开接口（无需认证）

以下接口无需认证：
- `/api/user/auth/login` - 用户登录
- `/api/user/auth/register` - 用户注册
- `/api/user/auth/register-new` - 新版注册
- `/api/user/auth/send-code` - 发送验证码
- `/api/user/auth/login-by-code` - 验证码登录
- `/api/user/auth/reset-password` - 重置密码
- `/api/user/auth/refresh-token` - 刷新Token
- `/api/public/**` - 所有公共接口（商品浏览等）
- `/api/images/**` - 图片访问接口

## 🛠️ 开发环境配置

### IDE配置

#### IntelliJ IDEA

1. **导入项目**
   - File → Open → 选择 `njumarket` 目录
   - 选择 "Import project from external model" → Maven
   - 等待Maven依赖下载完成

2. **配置运行配置**
   - Run → Edit Configurations
   - 为每个服务创建Spring Boot运行配置：
     - Discovery Server: `com.njumarket.discovery.DiscoveryServerApplication`
     - Gateway: `com.njumarket.gateway.GatewayApplication`
     - Auth Service: `com.njumarket.auth.AuthServiceApplication`
     - Commodity Service: `com.njumarket.commodity.CommodityServiceApplication`
     - Order Service: `com.njumarket.order.OrderServiceApplication`
     - Message Service: `com.njumarket.message.MessageServiceApplication`

3. **启动顺序**
   - 先启动 Discovery Server
   - 然后启动 Gateway
   - 最后启动各个业务服务（可并行）

#### Eclipse

1. **导入项目**
   - File → Import → Maven → Existing Maven Projects
   - 选择 `njumarket` 目录
   - 等待Maven依赖下载完成

2. **运行配置**
   - 右键项目 → Run As → Spring Boot App
   - 或创建Java Application运行配置

### Maven构建

#### 构建所有模块
```bash
cd njumarket
mvn clean install
```

#### 构建单个服务
```bash
cd njumarket/njumarket-service-auth
mvn clean package
```

#### 跳过测试构建
```bash
mvn clean install -DskipTests
```

## 🐛 常见问题

### 1. 服务无法注册到Eureka

**问题**: 服务启动后无法在Eureka控制台看到

**解决方案**:
- 检查Eureka Server是否已启动（http://localhost:8761）
- 检查服务配置中的Eureka地址是否正确
- 检查网络连接
- 查看服务启动日志中的错误信息

### 2. Gateway无法路由请求

**问题**: 通过Gateway访问接口返回404或503

**解决方案**:
- 检查目标服务是否已启动并注册到Eureka
- 检查Gateway路由配置是否正确
- 检查服务名称是否匹配（注意大小写）
- 查看Gateway日志

### 3. 数据库连接失败

**问题**: 服务启动时报数据库连接错误

**解决方案**:
- 检查MySQL是否已启动
- 检查数据库名称、用户名、密码是否正确
- 检查数据库是否已创建（`nju_market`）
- 检查防火墙设置

### 4. Redis连接失败

**问题**: 服务启动时报Redis连接错误

**解决方案**:
- 检查Redis是否已启动
- 检查Redis主机和端口配置
- 检查Redis密码配置（如果有）
- 检查防火墙设置

### 5. 端口冲突

**问题**: 服务启动时报端口被占用

**解决方案**:
- 检查端口是否被其他程序占用
- 修改`application.yml`中的端口配置
- 使用`netstat -ano | findstr :8091`（Windows）或`lsof -i :8091`（Linux/Mac）查看端口占用

## 📊 监控和日志

### 查看服务日志

各服务的日志输出在控制台，建议使用IDE的日志查看功能或重定向到文件：

```bash
# 启动服务并保存日志
mvn spring-boot:run > service.log 2>&1
```

### Eureka监控

访问 http://localhost:8761 查看：
- 已注册的服务列表
- 服务健康状态
- 服务实例信息

## 🔄 服务间调用（跨服务通信）

### 概述

本项目确实涉及跨服务调用。在微服务架构中，不同服务需要协作完成业务功能，例如：
- **Message Service** 需要查询用户信息（来自Auth Service）
- **Message Service** 需要查询商品信息（来自Commodity Service）
- **Message Service** 需要查询订单信息（来自Order Service）
- **Order Service** 需要查询商品信息（来自Commodity Service）
- **Order Service** 需要查询用户信息（来自Auth Service）

### 当前实现状态

**⚠️ 重要说明**：当前代码中仍存在**不正确的跨服务调用方式**，需要后续改造。

#### 问题代码示例

在 `ContactServiceImpl`（Message Service）中：
```java
// ❌ 错误：直接注入其他服务的Repository
private final UserRepository userRepository;  // 来自Auth Service
private final CommodityRepository commodityRepository;  // 来自Commodity Service
private final OrderRepository orderRepository;  // 来自Order Service
```

在 `OrderServiceImpl`（Order Service）中：
```java
// ❌ 错误：直接注入其他服务的Repository
private final CommodityRepository commodityRepository;  // 来自Commodity Service
private final UserRepository userRepository;  // 来自Auth Service
```

在 `ChatDataController`（Order Service）中：
```java
// ❌ 错误：直接注入其他服务的Service
private final CommodityQueryService commodityQueryService;  // 来自Commodity Service
private final ChangeRecordService changeRecordService;  // 来自Commodity Service
```

**为什么这是错误的？**
1. 在微服务架构中，每个服务应该只访问自己的数据库
2. 直接注入其他服务的Repository会导致服务间紧耦合
3. 无法实现服务的独立部署和扩展
4. 违反了微服务的边界原则

### 正确的跨服务调用方式

#### 方式1：通过Gateway进行调用（当前前端调用方式）

**适用场景**：前端调用后端服务

```
前端 → Gateway (8080) → 目标服务
```

**示例**：
```javascript
// 前端代码
axios.get('http://localhost:8080/commodity/api/public/commodity/search')
```

#### 方式2：使用Feign Client（推荐用于服务间调用）

**适用场景**：后端服务间调用

**实现步骤**：

1. **添加Feign依赖**（已在各服务POM中配置）

2. **启用Feign Client**（已在各服务Application类中配置）：
```java
@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients(basePackages = "com.njumarket")
public class MessageServiceApplication {
    // ...
}
```

3. **创建Feign Client接口**：

**示例1：Message Service调用Auth Service**
```java
// 在 message-service 中创建
package com.njumarket.message.client;

import com.njumarket.njumarket.dto.Result;
import com.njumarket.njumarket.entity.User;
import com.njumarket.njumarket.entity.UserProfile;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = "njumarket-service-auth", path = "/api")
public interface AuthClient {
    
    // 根据ID查询用户
    @GetMapping("/user/{userId}")
    Result<User> getUserById(@PathVariable String userId);
    
    // 批量查询用户
    @GetMapping("/user/batch")
    Result<List<User>> getUsersByIds(@RequestParam List<String> userIds);
    
    // 根据ID查询用户档案
    @GetMapping("/user/profile/{userId}")
    Result<UserProfile> getUserProfileById(@PathVariable String userId);
    
    // 批量查询用户档案
    @GetMapping("/user/profile/batch")
    Result<List<UserProfile>> getUserProfilesByIds(@RequestParam List<String> userIds);
}
```

**示例2：Message Service调用Commodity Service**
```java
// 在 message-service 中创建
package com.njumarket.message.client;

import com.njumarket.njumarket.dto.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@FeignClient(name = "njumarket-service-commodity", path = "/api")
public interface CommodityClient {
    
    // 根据ID查询商品
    @GetMapping("/public/commodity/{commodityId}")
    Result getCommodityById(@PathVariable String commodityId);
    
    // 批量查询商品状态（用于聊天界面）
    @PostMapping("/user/commodity/batch-status")
    Result getCommoditiesBatchStatus(@RequestBody List<String> commodityIds);
}
```

**示例3：Message Service调用Order Service**
```java
// 在 message-service 中创建
package com.njumarket.message.client;

import com.njumarket.njumarket.dto.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@FeignClient(name = "njumarket-service-order", path = "/api")
public interface OrderClient {
    
    // 根据ID查询订单
    @GetMapping("/user/order/{orderId}")
    Result getOrderById(@PathVariable String orderId);
    
    // 批量查询订单状态（用于聊天界面）
    @PostMapping("/user/order/batch-status")
    Result getOrdersBatchStatus(@RequestBody List<String> orderIds);
}
```

**示例4：Order Service调用Commodity Service**
```java
// 在 order-service 中创建
package com.njumarket.order.client;

import com.njumarket.njumarket.dto.Result;
import com.njumarket.njumarket.entity.Commodity;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "njumarket-service-commodity", path = "/api")
public interface CommodityClient {
    
    // 查询商品（带悲观锁，用于创建订单）
    @GetMapping("/commodity/{commodityId}/for-update")
    Result<Commodity> getCommodityForUpdate(@PathVariable String commodityId);
    
    // 更新商品库存
    @PostMapping("/commodity/{commodityId}/update-stock")
    Result updateCommodityStock(@PathVariable String commodityId, 
                                @RequestParam Integer quantity);
}
```

4. **在Service中使用Feign Client**：

```java
// ContactServiceImpl 改造示例
@Service
@RequiredArgsConstructor
public class ContactServiceImpl implements ContactService {
    
    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    
    // ✅ 正确：使用Feign Client调用其他服务
    private final AuthClient authClient;
    private final CommodityClient commodityClient;
    private final OrderClient orderClient;
    
    @Override
    public Result sendMessage(String userId, SendMessageRequest request) {
        // ✅ 使用Feign Client查询用户
        Result<User> userResult = authClient.getUserById(request.getReceiverId());
        if (!userResult.getSuccess() || userResult.getData() == null) {
            throw new BusinessException("接收者不存在");
        }
        User receiver = userResult.getData();
        
        // ✅ 使用Feign Client查询商品（如果是商品卡片消息）
        if ("COMMODITY_CARD".equals(request.getMessageType())) {
            Result commodityResult = commodityClient.getCommodityById(request.getCommodityId());
            // 处理商品信息...
        }
        
        // 其他业务逻辑...
    }
}
```

### Feign Client配置

#### 超时配置

在各服务的 `application.yml` 中添加：

```yaml
feign:
  client:
    config:
      default:
        connectTimeout: 5000  # 连接超时（毫秒）
        readTimeout: 10000     # 读取超时（毫秒）
      njumarket-service-auth:
        connectTimeout: 3000
        readTimeout: 5000
      njumarket-service-commodity:
        connectTimeout: 3000
        readTimeout: 5000
```

#### 重试配置

```yaml
feign:
  client:
    config:
      default:
        retryer:
          period: 100        # 重试间隔（毫秒）
          maxPeriod: 1000    # 最大重试间隔
          maxAttempts: 3     # 最大重试次数
```

#### 日志配置

```yaml
logging:
  level:
    com.njumarket.message.client: DEBUG  # Feign Client日志级别
```

### 跨服务调用的最佳实践

#### 1. 服务边界划分

- **Auth Service**：用户认证、用户信息、用户档案
- **Commodity Service**：商品管理、商品查询、图片管理
- **Order Service**：订单管理、投诉管理
- **Message Service**：消息管理、会话管理、WebSocket推送

#### 2. 数据一致性

**问题**：跨服务调用时如何保证数据一致性？

**解决方案**：
- **最终一致性**：使用消息队列（如RabbitMQ、Kafka）实现异步通知
- **强一致性**：使用分布式事务（如Seata），但性能开销较大
- **当前项目**：采用最终一致性，关键操作通过Redis分布式锁保护

#### 3. 服务降级和熔断

**使用Hystrix或Sentinel实现服务降级**：

```java
@FeignClient(name = "njumarket-service-commodity", 
             fallback = CommodityClientFallback.class)
public interface CommodityClient {
    // ...
}

@Component
public class CommodityClientFallback implements CommodityClient {
    @Override
    public Result getCommodityById(String commodityId) {
        return Result.fail("商品服务暂时不可用，请稍后重试");
    }
}
```

#### 4. 性能优化

- **批量查询**：使用批量接口减少调用次数
- **缓存**：对频繁查询的数据进行缓存（Redis）
- **异步调用**：非关键路径使用异步调用

### 当前项目中的跨服务调用场景

#### 场景1：Message Service → Auth Service

**调用场景**：
- 发送消息时验证接收者是否存在
- 获取对话列表时查询用户信息
- 获取用户档案信息

**当前实现**：❌ 直接注入 `UserRepository` 和 `UserProfileRepository`

**正确实现**：✅ 使用 `AuthClient` Feign Client

#### 场景2：Message Service → Commodity Service

**调用场景**：
- 发送商品卡片消息时验证商品是否存在
- 获取商品信息用于消息展示

**当前实现**：❌ 直接注入 `CommodityRepository`

**正确实现**：✅ 使用 `CommodityClient` Feign Client

#### 场景3：Message Service → Order Service

**调用场景**：
- 发送订单卡片消息时验证订单是否存在
- 获取订单信息用于消息展示

**当前实现**：❌ 直接注入 `OrderRepository`

**正确实现**：✅ 使用 `OrderClient` Feign Client

#### 场景4：Order Service → Commodity Service

**调用场景**：
- 创建订单时查询商品信息
- 更新商品库存
- 验证商品状态

**当前实现**：❌ 直接注入 `CommodityRepository`

**正确实现**：✅ 使用 `CommodityClient` Feign Client

#### 场景5：Order Service → Auth Service

**调用场景**：
- 创建订单时查询卖家信息
- 获取用户档案信息

**当前实现**：❌ 直接注入 `UserRepository` 和 `UserProfileRepository`

**正确实现**：✅ 使用 `AuthClient` Feign Client

#### 场景6：Order Service → Message Service

**调用场景**：
- 订单状态变更时推送WebSocket消息
- 订单提醒功能

**当前实现**：❌ 直接注入 `WebSocketRetryService`

**正确实现**：✅ 使用 `MessageClient` Feign Client 或消息队列

### 改造计划

#### 阶段1：创建Feign Client接口（待完成）

需要在以下服务中创建Feign Client：
- `message-service`: `AuthClient`, `CommodityClient`, `OrderClient`
- `order-service`: `AuthClient`, `CommodityClient`, `MessageClient`
- `auth-service`: `CommodityClient`, `OrderClient`, `MessageClient`（用于Admin功能）

#### 阶段2：替换直接注入（待完成）

将Service实现类中的直接Repository注入替换为Feign Client调用。

#### 阶段3：添加服务降级（可选）

实现Feign Client的Fallback类，提高系统容错能力。

### 微服务架构学习要点

#### 1. 服务拆分原则

**按业务领域拆分**（本项目采用）：
- 认证服务：用户、管理员相关
- 商品服务：商品、图片相关
- 订单服务：订单、投诉相关
- 消息服务：消息、会话相关

**优点**：
- 业务边界清晰
- 易于理解和维护
- 符合领域驱动设计（DDD）

#### 2. 服务间通信方式

**同步通信**：
- REST API（HTTP）
- Feign Client（基于REST，声明式）
- gRPC（高性能，但需要额外配置）

**异步通信**：
- 消息队列（RabbitMQ、Kafka）
- WebSocket（实时推送）

**本项目使用**：
- 前端 → 后端：REST API（通过Gateway）
- 服务间：Feign Client（待实现）
- 实时推送：WebSocket

#### 3. 数据管理

**数据库设计**：
- **共享数据库**（当前）：所有服务共享一个数据库
  - 优点：简单，无需数据同步
  - 缺点：服务间耦合，无法独立扩展
- **独立数据库**（推荐）：每个服务有自己的数据库
  - 优点：服务独立，易于扩展
  - 缺点：需要处理数据一致性

**本项目当前状态**：共享数据库，但已按服务划分Repository，为后续拆分做准备。

#### 4. 服务发现

**Eureka**（本项目使用）：
- 服务注册：服务启动时向Eureka注册
- 服务发现：通过服务名查找服务实例
- 负载均衡：自动选择可用实例

**其他方案**：
- Consul
- Nacos
- Kubernetes Service Discovery

#### 5. API网关

**作用**：
- 统一入口
- 路由转发
- 负载均衡
- 安全认证
- 限流熔断

**本项目Gateway配置**：
- 路由规则：按路径前缀路由到不同服务
- CORS配置：支持跨域请求
- 服务发现：自动从Eureka获取服务实例

#### 6. 分布式事务

**问题**：跨服务操作如何保证事务一致性？

**解决方案**：
1. **两阶段提交（2PC）**：性能差，不推荐
2. **TCC模式**：Try-Confirm-Cancel，适合强一致性
3. **Saga模式**：最终一致性，适合长事务
4. **消息队列**：异步处理，最终一致性
5. **本地事务 + 补偿**：简单，适合大多数场景

**本项目策略**：
- 单个服务内使用本地事务
- 跨服务操作使用最终一致性
- 关键操作使用Redis分布式锁保护

#### 7. 服务监控

**需要监控的指标**：
- 服务健康状态
- 请求响应时间
- 错误率
- 服务调用链路

**工具**：
- Spring Boot Actuator
- Prometheus + Grafana
- Zipkin（链路追踪）
- ELK（日志分析）

#### 8. 配置管理

**配置方式**：
- 配置文件（application.yml）
- 环境变量
- 配置中心（Nacos、Apollo）

**本项目**：使用配置文件 + 环境变量

### 微服务架构的优缺点

#### 优点

1. **独立部署**：每个服务可以独立部署和扩展
2. **技术栈灵活**：不同服务可以使用不同技术
3. **团队独立**：不同团队可以负责不同服务
4. **故障隔离**：单个服务故障不影响其他服务
5. **易于扩展**：可以针对性地扩展某个服务

#### 缺点

1. **复杂度增加**：需要处理服务发现、配置管理、监控等
2. **网络延迟**：服务间调用增加网络开销
3. **数据一致性**：跨服务事务处理复杂
4. **测试困难**：需要集成测试环境
5. **运维成本**：需要管理多个服务实例

### 学习建议

1. **理解服务边界**：明确每个服务的职责和数据边界
2. **掌握服务通信**：理解REST、Feign、消息队列等通信方式
3. **学习分布式系统**：理解CAP定理、一致性、可用性等概念
4. **实践监控和调试**：学会使用工具监控和调试微服务
5. **阅读源码**：理解Spring Cloud的实现原理

### 实际案例：Message Service如何跨服务调用

#### 场景：发送商品卡片消息

**业务需求**：用户在聊天中发送商品卡片，需要验证商品是否存在。

**当前实现（错误）**：
```java
// ContactServiceImpl.java
private final CommodityRepository commodityRepository;  // ❌ 直接注入

public Result sendMessage(...) {
    // ❌ 直接查询商品
    Optional<Commodity> commodityOpt = commodityRepository.findById(commodityId);
}
```

**正确实现（使用Feign Client）**：

1. **创建Feign Client接口**：
```java
// message-service/src/main/java/com/njumarket/message/client/CommodityClient.java
package com.njumarket.message.client;

import com.njumarket.njumarket.dto.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "njumarket-service-commodity", path = "/api")
public interface CommodityClient {
    
    @GetMapping("/public/commodity/{commodityId}")
    Result getCommodityById(@PathVariable String commodityId);
}
```

2. **在Service中使用**：
```java
// ContactServiceImpl.java
@Service
@RequiredArgsConstructor
public class ContactServiceImpl implements ContactService {
    
    // ✅ 正确：使用Feign Client
    private final CommodityClient commodityClient;
    
    public Result sendMessage(...) {
        // ✅ 通过Feign Client调用商品服务
        Result commodityResult = commodityClient.getCommodityById(commodityId);
        if (!commodityResult.getSuccess()) {
            throw new BusinessException("商品不存在");
        }
        // 处理商品信息...
    }
}
```

3. **调用流程**：
```
Message Service → Feign Client → Eureka → Commodity Service
```

**优势**：
- 服务解耦：Message Service不直接访问Commodity Service的数据库
- 服务独立：可以独立部署和扩展
- 负载均衡：Feign自动从Eureka获取服务实例并负载均衡
- 容错处理：可以配置超时、重试、降级等

### 微服务架构核心概念

#### 1. 服务边界（Service Boundary）

**定义**：每个微服务应该有自己的数据存储和业务逻辑，服务之间通过API通信。

**本项目示例**：
- Auth Service：管理User、UserProfile、Admin表
- Commodity Service：管理Commodity、CommoditySnapshot、ImageReference表
- Order Service：管理Order、OrderSnapshot、Complaint表
- Message Service：管理Message、Conversation表

**原则**：
- 一个服务不应该直接访问其他服务的数据库
- 服务间只能通过API（REST/Feign）通信
- 每个服务对自己的数据负责

#### 2. 服务发现（Service Discovery）

**Eureka工作流程**：
1. 服务启动时向Eureka注册（服务名、IP、端口）
2. 服务定期发送心跳保持注册
3. 其他服务通过服务名从Eureka获取服务实例列表
4. Feign Client自动选择可用实例进行调用

**示例**：
```java
// Message Service调用Commodity Service
@FeignClient(name = "njumarket-service-commodity")  // 通过服务名查找
public interface CommodityClient {
    // ...
}
```

#### 3. API网关（API Gateway）

**作用**：
- **统一入口**：所有外部请求都通过Gateway
- **路由转发**：根据路径转发到不同服务
- **负载均衡**：自动选择服务实例
- **安全认证**：统一处理认证逻辑
- **限流熔断**：保护后端服务

**本项目Gateway路由规则**：
```yaml
routes:
  - id: auth-service
    uri: lb://njumarket-service-auth  # lb表示负载均衡
    predicates:
      - Path=/auth/**
    filters:
      - StripPrefix=1  # 去除路径前缀
```

**请求流程**：
```
前端请求: http://localhost:8080/auth/api/user/auth/login
         ↓
Gateway接收请求，匹配路由规则
         ↓
转发到: http://njumarket-service-auth/api/user/auth/login
         ↓
Auth Service处理请求
```

#### 4. 分布式数据管理

**共享数据库模式**（当前项目）：
- 所有服务共享同一个数据库
- 每个服务只访问自己的表
- 优点：简单，无需数据同步
- 缺点：服务间仍有耦合，无法完全独立

**独立数据库模式**（推荐）：
- 每个服务有自己的数据库
- 服务间通过API通信
- 优点：完全解耦，可独立扩展
- 缺点：需要处理数据一致性

**数据一致性策略**：
- **强一致性**：分布式事务（Seata），性能开销大
- **最终一致性**：消息队列异步同步，性能好
- **本项目**：关键操作使用Redis分布式锁，保证最终一致性

#### 5. 服务通信模式

**同步通信**：
- **REST API**：HTTP请求，简单直接
- **Feign Client**：声明式REST客户端，自动负载均衡
- **gRPC**：高性能RPC框架，需要额外配置

**异步通信**：
- **消息队列**：RabbitMQ、Kafka，解耦、削峰
- **WebSocket**：实时双向通信，本项目用于消息推送

**选择原则**：
- 需要立即响应 → 同步通信（REST/Feign）
- 可以异步处理 → 异步通信（消息队列）
- 需要实时推送 → WebSocket

#### 6. 服务治理

**服务降级（Fallback）**：
当目标服务不可用时，返回默认值或错误提示，避免级联故障。

**服务熔断（Circuit Breaker）**：
当服务失败率达到阈值时，自动熔断，快速失败，保护系统。

**服务限流（Rate Limiting）**：
限制服务调用频率，防止服务过载。

**本项目当前状态**：基础配置已完成，降级和熔断待实现。

### 微服务开发最佳实践

#### 1. API设计

**RESTful规范**：
- GET：查询
- POST：创建
- PUT：更新
- DELETE：删除

**路径设计**：
```
/api/user/auth/login          # 用户认证
/api/user/order/create        # 创建订单
/api/public/commodity/search  # 公共查询
```

#### 2. 错误处理

**统一异常处理**：
```java
@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(BusinessException.class)
    public Result handleBusinessException(BusinessException e) {
        return Result.fail(e.getMessage());
    }
}
```

#### 3. 日志记录

**日志级别**：
- ERROR：错误信息
- WARN：警告信息
- INFO：关键业务操作
- DEBUG：调试信息

**日志格式**：
```java
log.info("创建订单 - userId: {}, commodityId: {}, amount: {}", 
         userId, commodityId, amount);
```

#### 4. 性能优化

**数据库优化**：
- 使用索引
- 避免N+1查询
- 批量查询

**缓存策略**：
- 热点数据缓存（Redis）
- 缓存失效策略

**服务调用优化**：
- 批量接口减少调用次数
- 异步调用非关键路径

### 常见微服务模式

#### 1. 数据库 per 服务（Database per Service）

**模式**：每个服务有自己的数据库

**优点**：
- 服务完全独立
- 可以独立扩展
- 技术栈灵活

**缺点**：
- 数据一致性复杂
- 跨服务查询困难

#### 2. API组合（API Composition）

**模式**：通过组合多个服务的API实现复杂查询

**示例**：
```java
// 获取订单详情（需要组合多个服务）
public OrderDetailDTO getOrderDetail(String orderId) {
    // 从Order Service获取订单
    Order order = orderClient.getOrder(orderId);
    
    // 从Commodity Service获取商品
    Commodity commodity = commodityClient.getCommodity(order.getCommodityId());
    
    // 从Auth Service获取用户
    User seller = authClient.getUser(order.getSellerId());
    
    // 组合返回
    return combine(order, commodity, seller);
}
```

#### 3. 事件驱动（Event-Driven）

**模式**：服务间通过事件通信

**示例**：
```java
// 订单创建后发布事件
eventPublisher.publish(new OrderCreatedEvent(orderId));

// 其他服务监听事件
@EventListener
public void handleOrderCreated(OrderCreatedEvent event) {
    // 处理订单创建事件
}
```

#### 4. CQRS（Command Query Responsibility Segregation）

**模式**：命令和查询分离

**命令**：写操作，修改数据
**查询**：读操作，查询数据

**本项目示例**：
- CommodityService：命令（创建、更新商品）
- CommodityQueryService：查询（搜索、查询商品）

### 微服务测试策略

#### 1. 单元测试

测试单个服务的方法，不依赖其他服务。

#### 2. 集成测试

测试服务与数据库、Redis等的集成。

#### 3. 契约测试

测试服务间的API契约，确保接口兼容。

#### 4. 端到端测试

测试完整的业务流程，涉及多个服务。

### 微服务部署

#### 开发环境

所有服务运行在同一台机器，通过不同端口区分。

#### 生产环境

- **容器化**：使用Docker部署
- **编排**：使用Kubernetes管理
- **监控**：Prometheus + Grafana
- **日志**：ELK Stack

### 总结

本项目是一个典型的微服务架构实践，包含：

1. **服务拆分**：按业务领域拆分为4个业务服务
2. **服务发现**：使用Eureka实现服务注册与发现
3. **API网关**：使用Spring Cloud Gateway统一入口
4. **服务通信**：前端通过Gateway，服务间使用Feign Client（待实现）
5. **数据管理**：当前共享数据库，已按服务划分Repository

**学习重点**：
- 理解微服务的边界和通信方式
- 掌握Feign Client的使用
- 理解分布式系统的挑战和解决方案
- 实践服务监控和调试

## 📚 相关文档

- [微服务架构文档](./MICROSERVICES_ARCHITECTURE.md)
- [项目文档v2.0](./PROJECT_DOCUMENTATION_V2.0.md)
- [迁移总结](./MIGRATION_SUMMARY.md)
- [快速启动指南](./QUICK_START_GUIDE.md)

## 🆘 获取帮助

如遇到问题，请：
1. 查看服务启动日志
2. 检查配置文件
3. 参考本文档的"常见问题"部分
4. 查看相关文档

---

**最后更新**: 2025-01-20
**版本**: 2.0.0

