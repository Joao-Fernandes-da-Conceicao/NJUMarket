# NJUMarket v2.0 快速启动指南

## 📋 前置要求

- JDK 17+
- Maven 3.6+
- MySQL 8.0+
- Redis 6.0+

## 🚀 启动步骤

### 1. 启动MySQL和Redis

确保MySQL和Redis服务已启动并运行。

### 2. 启动Eureka Server

```bash
cd njumarket/njumarket-discovery
mvn spring-boot:run
```

**验证**: 访问 http://localhost:8761

### 3. 启动各微服务

**方式一：分别启动（推荐用于开发）**

```bash
# 终端1 - Auth Service
cd njumarket/njumarket-service-auth
mvn spring-boot:run

# 终端2 - Commodity Service
cd njumarket/njumarket-service-commodity
mvn spring-boot:run

# 终端3 - Order Service
cd njumarket/njumarket-service-order
mvn spring-boot:run

# 终端4 - Message Service
cd njumarket/njumarket-service-message
mvn spring-boot:run
```

**方式二：使用IDE**

在IDE中分别运行各服务的Application类：
- `DiscoveryServerApplication` (8761)
- `AuthServiceApplication` (8081)
- `CommodityServiceApplication` (8082)
- `OrderServiceApplication` (8083)
- `MessageServiceApplication` (8084)

### 4. 启动Gateway

```bash
cd njumarket/njumarket-gateway
mvn spring-boot:run
```

**验证**: 访问 http://localhost:8080

## ✅ 验证服务注册

访问 Eureka Dashboard: http://localhost:8761

应看到以下服务：
- njumarket-gateway
- njumarket-service-auth
- njumarket-service-commodity
- njumarket-service-order
- njumarket-service-message

## 🔍 测试API

通过Gateway访问API：

```bash
# 测试Gateway健康检查
curl http://localhost:8080/actuator/health

# 测试服务路由（需要Service和Controller迁移完成后）
curl http://localhost:8080/auth/health
```

## 📝 注意事项

1. **启动顺序**: 必须先启动Eureka Server，再启动其他服务
2. **端口占用**: 确保端口8761, 8080-8084未被占用
3. **数据库**: 确保数据库已创建并配置正确
4. **Redis**: 确保Redis服务运行正常

## 🐛 常见问题

### 服务无法注册到Eureka

- 检查Eureka Server是否启动
- 检查服务配置中的Eureka地址
- 检查网络连接

### Gateway无法路由

- 检查Gateway是否注册到Eureka
- 检查目标服务是否已注册
- 检查路由配置是否正确

### 数据库连接失败

- 检查数据库服务是否启动
- 检查连接URL、用户名、密码
- 检查数据库是否存在

