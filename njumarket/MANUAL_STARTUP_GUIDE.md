# NJUMarket 手动启动指南

## 📋 启动顺序说明

微服务必须按照以下顺序启动，确保服务依赖关系正确：

### 阶段1：基础设施服务（必须先启动）

这些服务是其他服务的基础，必须最先启动。

#### 1. Eureka Discovery Server
**端口**: 8761  
**启动命令**:
```bash
cd njumarket-discovery
mvn spring-boot:run
```

**验证**: 访问 http://localhost:8761，应看到Eureka控制台

**等待时间**: 启动后等待 **10-15秒**，确保Eureka完全启动

---

### 阶段2：网关服务（依赖Eureka）

#### 2. API Gateway
**端口**: 8080  
**启动命令**:
```bash
cd njumarket-gateway
mvn spring-boot:run
```

**依赖**: 需要Eureka已启动并运行

**验证**: 启动后检查日志，确认已注册到Eureka

**等待时间**: 启动后等待 **10秒**

---

### 阶段3：核心业务服务（依赖Eureka）

这些服务可以按顺序启动，也可以部分并行启动。

#### 3. Auth Service（认证服务）
**端口**: 8091  
**启动命令**:
```bash
cd njumarket-service-auth
mvn spring-boot:run
```

**依赖**: 
- Eureka Discovery Server
- MySQL数据库
- Redis

**说明**: 其他服务可能依赖认证服务，建议先启动

**等待时间**: 启动后等待 **5秒**

---

#### 4. Commodity Service（商品服务）
**端口**: 8092  
**启动命令**:
```bash
cd njumarket-service-commodity
mvn spring-boot:run
```

**依赖**: 
- Eureka Discovery Server
- MySQL数据库
- Redis
- Auth Service（通过Feign Client调用）

**等待时间**: 启动后等待 **5秒**

---

#### 5. Order Service（订单服务）
**端口**: 8093  
**启动命令**:
```bash
cd njumarket-service-order
mvn spring-boot:run
```

**依赖**: 
- Eureka Discovery Server
- MySQL数据库
- Redis
- Auth Service（通过Feign Client调用）
- Commodity Service（通过Feign Client调用）

**等待时间**: 启动后等待 **5秒**

---

#### 6. Message Service（消息服务）
**端口**: 8094  
**启动命令**:
```bash
cd njumarket-service-message
mvn spring-boot:run
```

**依赖**: 
- Eureka Discovery Server
- MySQL数据库
- Redis
- Auth Service（通过Feign Client调用）
- Commodity Service（通过Feign Client调用）
- Order Service（通过Feign Client调用）

**等待时间**: 启动后等待 **5秒**

---

#### 7. Image Service（图片服务）
**端口**: 8095  
**启动命令**:
```bash
cd njumarket-service-image
mvn spring-boot:run
```

**依赖**: 
- Eureka Discovery Server
- Redis（用于缓存）

**说明**: 图片服务相对独立，可以较早启动

**等待时间**: 启动后等待 **5秒**

---

#### 8. Admin Service（管理员服务）
**端口**: 8096  
**启动命令**:
```bash
cd njumarket-service-admin
mvn spring-boot:run
```

**依赖**: 
- Eureka Discovery Server
- MySQL数据库
- Redis
- Auth Service（通过Feign Client调用）
- Commodity Service（通过Feign Client调用）
- Order Service（通过Feign Client调用）
- Message Service（通过Feign Client调用）

**说明**: 管理员服务依赖多个业务服务，建议最后启动

**等待时间**: 启动后等待 **5秒**

---

## 🚀 完整启动流程示例

### Windows系统（使用多个命令行窗口）

**窗口1 - Eureka:**
```cmd
cd D:\软工作业\NJUMarket\njumarket\njumarket-discovery
mvn spring-boot:run
```
等待看到 "Started DiscoveryServerApplication" 后继续

**窗口2 - Gateway:**
```cmd
cd D:\软工作业\NJUMarket\njumarket\njumarket-gateway
mvn spring-boot:run
```
等待看到 "Started GatewayApplication" 后继续

**窗口3 - Auth Service:**
```cmd
cd D:\软工作业\NJUMarket\njumarket\njumarket-service-auth
mvn spring-boot:run
```

**窗口4 - Commodity Service:**
```cmd
cd D:\软工作业\NJUMarket\njumarket\njumarket-service-commodity
mvn spring-boot:run
```

**窗口5 - Order Service:**
```cmd
cd D:\软工作业\NJUMarket\njumarket\njumarket-service-order
mvn spring-boot:run
```

**窗口6 - Message Service:**
```cmd
cd D:\软工作业\NJUMarket\njumarket\njumarket-service-message
mvn spring-boot:run
```

**窗口7 - Image Service:**
```cmd
cd D:\软工作业\NJUMarket\njumarket\njumarket-service-image
mvn spring-boot:run
```

**窗口8 - Admin Service:**
```cmd
cd D:\软工作业\NJUMarket\njumarket\njumarket-service-admin
mvn spring-boot:run
```

### Linux/Mac系统

在终端中使用 `&` 在后台启动，或使用多个终端窗口：

```bash
# 终端1 - Eureka
cd njumarket/njumarket-discovery
mvn spring-boot:run &

# 等待10秒后
sleep 10

# 终端2 - Gateway
cd njumarket/njumarket-gateway
mvn spring-boot:run &

# 等待10秒后
sleep 10

# 终端3 - Auth Service
cd njumarket/njumarket-service-auth
mvn spring-boot:run &

# 等待5秒后
sleep 5

# 终端4 - Commodity Service
cd njumarket/njumarket-service-commodity
mvn spring-boot:run &

# ... 依此类推
```

---

## ✅ 验证启动成功

### 1. 检查Eureka控制台

访问 http://localhost:8761

应看到以下服务已注册：
- ✅ njumarket-gateway
- ✅ njumarket-service-auth
- ✅ njumarket-service-commodity
- ✅ njumarket-service-order
- ✅ njumarket-service-message
- ✅ njumarket-service-image
- ✅ njumarket-service-admin

### 2. 检查服务日志

每个服务的日志中应看到：
- `Registered instance` - 已注册到Eureka
- `Started ...Application` - 服务启动成功
- 没有 `Connection refused` 或 `Timeout` 错误

### 3. 测试API

通过Gateway测试服务：

```bash
# 测试Gateway健康检查
curl http://localhost:8080/actuator/health

# 测试Auth Service（需要认证）
curl http://localhost:8080/api/user/auth/login
```

---

## 📊 启动顺序总结表

| 阶段 | 服务 | 端口 | 等待时间 | 关键依赖 |
|------|------|------|----------|----------|
| **阶段1** | Eureka Discovery | 8761 | 10-15秒 | 无 |
| **阶段2** | API Gateway | 8080 | 10秒 | Eureka |
| **阶段3** | Auth Service | 8091 | 5秒 | Eureka, MySQL, Redis |
| **阶段3** | Image Service | 8095 | 5秒 | Eureka, Redis |
| **阶段3** | Commodity Service | 8092 | 5秒 | Eureka, Auth Service |
| **阶段3** | Order Service | 8093 | 5秒 | Eureka, Auth, Commodity |
| **阶段3** | Message Service | 8094 | 5秒 | Eureka, Auth, Commodity, Order |
| **阶段3** | Admin Service | 8096 | 5秒 | Eureka, 所有业务服务 |

---

## ⚠️ 注意事项

1. **必须按顺序启动**：Eureka → Gateway → 业务服务
2. **等待时间很重要**：确保前一个服务完全启动后再启动下一个
3. **检查日志**：如果服务启动失败，查看日志中的错误信息
4. **数据库和Redis**：确保MySQL和Redis已启动并配置正确
5. **端口占用**：确保所有端口（8761, 8080, 8091-8096）未被占用

---

## 🛑 停止服务

### Windows
直接关闭各个命令行窗口

### Linux/Mac
```bash
# 查找所有Spring Boot进程
ps aux | grep "spring-boot:run"

# 停止指定进程
kill <PID>

# 或停止所有
pkill -f "spring-boot:run"
```

---

## 💡 开发建议

1. **使用IDE**：在IDE中分别运行各服务的Application类，便于调试
2. **查看日志**：每个服务都有独立的日志输出
3. **热部署**：开发时可以使用Spring Boot DevTools
4. **最小启动**：开发时可以先只启动必要的服务（Eureka + Gateway + 当前开发的服务）

