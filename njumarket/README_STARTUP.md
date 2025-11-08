# NJUMarket 微服务启动指南

## 📋 前置要求

- **JDK 17+** - Java开发环境
- **Maven 3.6+** - 项目构建工具
- **MySQL 8.0+** - 数据库（确保已启动）
- **Redis 6.0+** - 缓存和会话存储（确保已启动）

## 🚀 快速启动

### Windows系统

双击运行 `start-all-services.bat` 或在命令行执行：

```cmd
start-all-services.bat
```

### Linux/Mac系统

在终端执行：

```bash
chmod +x start-all-services.sh
./start-all-services.sh
```

## 📦 服务列表

启动脚本会按顺序启动以下服务：

| 序号 | 服务名称 | 端口 | 说明 |
|------|---------|------|------|
| 1 | Eureka Discovery Server | 8761 | 服务注册与发现 |
| 2 | API Gateway | 8080 | API网关 |
| 3 | Auth Service | 8091 | 用户认证服务 |
| 4 | Commodity Service | 8092 | 商品服务 |
| 5 | Order Service | 8093 | 订单服务 |
| 6 | Message Service | 8094 | 消息服务 |
| 7 | Image Service | 8095 | 图片服务 |
| 8 | Admin Service | 8096 | 管理员服务 |

## 🔍 验证服务启动

### 1. 检查Eureka控制台

访问 http://localhost:8761

应看到所有服务已注册：
- njumarket-gateway
- njumarket-service-auth
- njumarket-service-commodity
- njumarket-service-order
- njumarket-service-message
- njumarket-service-image
- njumarket-service-admin

### 2. 检查服务健康状态

通过Gateway访问各服务的健康检查端点：

```bash
# Gateway健康检查
curl http://localhost:8080/actuator/health

# Auth Service健康检查
curl http://localhost:8091/actuator/health
```

## 🛑 停止服务

### Windows系统

每个服务都在独立的命令行窗口中运行，直接关闭对应窗口即可停止服务。

或运行停止脚本：

```cmd
stop-all-services.bat
```

### Linux/Mac系统

运行停止脚本：

```bash
chmod +x stop-all-services.sh
./stop-all-services.sh
```

或手动停止：

```bash
# 查找所有Spring Boot进程
ps aux | grep "spring-boot:run"

# 停止指定进程
kill <PID>

# 强制停止
kill -9 <PID>
```

## 🔧 单独启动服务

如果需要单独启动某个服务进行调试：

```bash
# 进入服务目录
cd njumarket/njumarket-service-auth

# 启动服务
mvn spring-boot:run
```

## 📝 启动顺序说明

**重要**：服务必须按以下顺序启动：

1. **Eureka Discovery Server** - 必须先启动，其他服务需要注册到Eureka
2. **API Gateway** - 依赖Eureka进行服务发现
3. **Auth Service** - 其他服务可能依赖认证服务
4. **其他业务服务** - 可以并行启动

## 🧹 清理Maven缓存

如果遇到Maven编译错误（如 `inputFiles.lst: Input length = 1`），可以运行清理脚本：

**Windows:**
```cmd
clean-maven-cache.bat
```

**Linux/Mac:**
```bash
chmod +x clean-maven-cache.sh
./clean-maven-cache.sh
```

或者手动执行：
```bash
mvn clean
```

## ⚠️ 常见问题

### 1. Maven缓存文件损坏

**错误信息：**
```
Error reading old mojo status ... inputFiles.lst: Input length = 1
```

**解决方案：**
- 运行 `clean-maven-cache.bat` 或 `clean-maven-cache.sh`
- 或执行 `mvn clean` 清理所有target目录
- 然后重新编译或启动

### 2. 端口被占用

如果某个端口已被占用，会看到类似错误：

```
Web server failed to start. Port 8091 was already in use.
```

**解决方案**：
- 检查并关闭占用端口的进程
- 或修改 `application.yml` 中的端口配置

### 2. 服务无法注册到Eureka

**可能原因**：
- Eureka Server未启动
- 网络连接问题
- 配置错误

**解决方案**：
- 确认Eureka Server已启动（访问 http://localhost:8761）
- 检查各服务的 `application.yml` 中的Eureka配置

### 3. Maven未找到

**错误信息**：
```
[错误] 未找到Maven，请先安装Maven
```

**解决方案**：
- 安装Maven并添加到系统PATH
- 或使用项目自带的 `mvnw`（Maven Wrapper）

### 4. Java版本不匹配

**要求**：JDK 17+

**检查Java版本**：
```bash
java -version
```

## 📚 相关文档

- [快速启动指南](docs/QUICK_START_GUIDE.md)
- [微服务架构文档](docs/MICROSERVICES_ARCHITECTURE.md)
- [配置指南](docs/MICROSERVICES_CONFIGURATION_GUIDE.md)

## 💡 开发建议

1. **开发环境**：建议使用IDE（如IntelliJ IDEA）分别启动各个服务，便于调试
2. **日志查看**：每个服务都有独立的日志输出，便于排查问题
3. **热部署**：开发时可以使用Spring Boot DevTools实现热部署
4. **数据库**：确保数据库已创建并配置正确
5. **Redis**：确保Redis服务运行正常，用于Token存储和缓存

