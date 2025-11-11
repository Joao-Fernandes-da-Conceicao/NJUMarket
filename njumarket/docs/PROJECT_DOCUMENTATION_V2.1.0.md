# 南大集市 NJUMarket v2.1.0 项目文档

## 📋 目录
- [版本概述](#版本概述)
- [版本更新内容](#版本更新内容)
- [Spring Boot Actuator 集成详解](#spring-boot-actuator-集成详解)
- [Docker 容器化详解](#docker-容器化详解)
- [API 文档（Swagger）](#api-文档swagger)
- [使用指南](#使用指南)
- [技术总结](#技术总结)

---

## 版本概述

**NJUMarket v2.1.0** 是项目的监控和容器化版本，主要完成了基础监控、API 文档和容器化部署的集成。

### 版本信息
- **版本**: v2.1.0
- **发布日期**: 2025-11-11
- **状态**: ✅ **已完成**
- **主要目标**: 基础监控、API 文档、容器化部署

### 版本历史
- **v2.0.2** (2025-11-10): 反射优化、Spring Security 标准注解 ✅ **2.0阶段已完成**
- **v2.1.0** (2025-11-11): Actuator 监控、Docker 容器化、Swagger API 文档 ✅ **2.1.0阶段已完成**

### 主要成就

#### 基础监控
- ✅ 集成 Spring Boot Actuator（所有服务）
- ✅ 配置健康检查、应用信息、指标收集端点
- ✅ 支持服务健康监控和性能指标收集

#### 容器化部署
- ✅ 完成 Docker 容器化（所有服务）
- ✅ 使用 Docker Compose 一键启动
- ✅ 实现数据持久化和服务编排
- ✅ 简化启动流程，降低调试成本

#### API 文档
- ✅ 集成 SpringDoc OpenAPI（Swagger 3）
- ✅ 自动生成 API 文档
- ✅ 支持在线测试接口

---

## 版本更新内容

### 1. Spring Boot Actuator 集成 ✅

**目标**: 提供基础的服务健康监控和性能指标收集

**完成内容**:
- 所有服务集成 `spring-boot-starter-actuator`
- 配置健康检查端点：`/actuator/health`
- 配置应用信息端点：`/actuator/info`
- 配置指标收集端点：`/actuator/metrics`
- 配置 Spring Security 允许访问 Actuator 端点

**详细说明**: 见 [Spring Boot Actuator 集成详解](#spring-boot-actuator-集成详解)

### 2. Docker 容器化 ✅

**目标**: 简化启动流程，降低调试成本，保证环境一致性

**完成内容**:
- 创建通用 Dockerfile（多阶段构建）
- 使用 Docker Compose 编排所有服务
- 配置服务依赖和启动顺序
- 实现健康检查和数据持久化
- 配置网络隔离和环境变量

**详细说明**: 见 [Docker 容器化详解](#docker-容器化详解)

### 3. API 文档（Swagger） ✅

**目标**: 提升开发效率和 API 可维护性

**完成内容**:
- 集成 SpringDoc OpenAPI（Swagger 3）
- 所有服务已配置 Swagger UI
- 代码中已使用 Swagger 注解（`@Operation`, `@Tag`, `@Schema` 等）

**访问方式**:
- Swagger UI: `http://localhost:{port}/swagger-ui.html`
- OpenAPI JSON: `http://localhost:{port}/v3/api-docs`

**联调说明**: 在 `auth-service` 的 Swagger UI 中获取 Token 后，即可在其他服务的 Swagger UI 中进行联调测试。

---

## Spring Boot Actuator 集成详解

### 什么是 Spring Boot Actuator？

Spring Boot Actuator 是 Spring Boot 提供的生产级监控和管理功能，它提供了丰富的端点（endpoints）来监控和管理应用程序。

### 为什么需要 Actuator？

在微服务架构中，监控是至关重要的：

1. **服务健康监控**: 了解服务是否正常运行
2. **性能指标收集**: 监控 JVM、HTTP、数据库等性能指标
3. **问题排查**: 快速定位服务问题
4. **运维管理**: 支持运维人员管理应用

### 集成实现

#### 1. Maven 依赖

所有服务的 `pom.xml` 中已添加：

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
```

#### 2. application.yml 配置

所有服务的 `application.yml` 中已添加以下配置：

```yaml
# Actuator 配置
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics  # 暴露的端点
      base-path: /actuator            # 端点基础路径
  endpoint:
    health:
      show-details: when-authorized   # 健康检查详细信息（需要授权时显示）
  info:
    env:
      enabled: true                   # 启用环境信息
    java:
      enabled: true                   # 启用 Java 信息
    os:
      enabled: true                   # 启用操作系统信息
```

#### 3. Spring Security 配置

所有服务的 `SecurityConfig` 已配置允许访问 Actuator 端点：

```java
.authorizeHttpRequests(auth -> auth
    // Actuator端点允许访问（用于健康检查和监控）
    .requestMatchers("/actuator/**").permitAll()
    // ... 其他配置
)
```

### 核心端点详解

#### 1. 健康检查端点 (`/actuator/health`)

**用途**: 检查服务是否健康运行

**访问方式**:
```bash
# Gateway 健康检查
curl http://localhost:8080/actuator/health

# Auth Service 健康检查
curl http://localhost:8091/actuator/health
```

**响应示例**:
```json
{
  "status": "UP"
}
```

**详细健康信息**（需要授权）:
```json
{
  "status": "UP",
  "components": {
    "db": {
      "status": "UP",
      "details": {
        "database": "MySQL",
        "validationQuery": "isValid()"
      }
    },
    "diskSpace": {
      "status": "UP",
      "details": {
        "total": 500000000000,
        "free": 300000000000,
        "threshold": 10485760
      }
    },
    "redis": {
      "status": "UP"
    }
  }
}
```

**使用场景**:
- 负载均衡器健康检查
- 服务注册中心健康状态
- 监控系统告警
- Docker 健康检查

#### 2. 应用信息端点 (`/actuator/info`)

**用途**: 获取应用基本信息

**访问方式**:
```bash
curl http://localhost:8091/actuator/info
```

**响应示例**:
```json
{
  "app": {
    "name": "njumarket-service-auth",
    "version": "0.0.1-SNAPSHOT"
  },
  "java": {
    "version": "17.0.10",
    "vendor": "Eclipse Adoptium"
  },
  "os": {
    "name": "Linux",
    "arch": "amd64",
    "version": "5.10.0"
  }
}
```

**使用场景**:
- 版本管理
- 环境信息查询
- 部署信息确认

#### 3. 指标端点 (`/actuator/metrics`)

**用途**: 收集应用性能指标

**访问方式**:
```bash
# 获取所有可用指标
curl http://localhost:8091/actuator/metrics

# 获取特定指标（如 JVM 内存使用）
curl http://localhost:8091/actuator/metrics/jvm.memory.used

# 使用标签过滤（查看堆内存）
curl "http://localhost:8091/actuator/metrics/jvm.memory.used?tag=area:heap"

# 查看具体内存区域（如老年代）
curl "http://localhost:8091/actuator/metrics/jvm.memory.used?tag=id:G1%20Old%20Gen"
```

**可用指标分类**:

1. **JVM 指标**
   - `jvm.memory.used` - JVM 内存使用
   - `jvm.memory.max` - JVM 最大内存
   - `jvm.threads.live` - 活跃线程数
   - `jvm.gc.pause` - GC 暂停时间

2. **HTTP 指标**
   - `http.server.requests` - HTTP 服务器请求数
   - `http.server.requests.active` - 活跃请求数

3. **数据库连接池指标**（HikariCP）
   - `hikaricp.connections.active` - 活跃连接数
   - `hikaricp.connections.idle` - 空闲连接数
   - `hikaricp.connections.max` - 最大连接数

4. **Redis 指标**（Lettuce）
   - `lettuce.command.completion` - Redis 命令完成时间
   - `lettuce.command.firstresponse` - Redis 首次响应时间

5. **系统指标**
   - `system.cpu.usage` - CPU 使用率
   - `disk.free` - 磁盘剩余空间
   - `disk.total` - 磁盘总空间

**使用场景**:
- 性能监控
- 问题排查
- 资源优化
- 与 Prometheus/Grafana 集成（后续版本）

### 实际应用示例

#### 示例 1: 监控 JVM 内存使用

```bash
# 查看所有内存指标
curl http://localhost:8091/actuator/metrics/jvm.memory.used

# 查看堆内存使用
curl "http://localhost:8091/actuator/metrics/jvm.memory.used?tag=area:heap"

# 查看老年代内存使用
curl "http://localhost:8091/actuator/metrics/jvm.memory.used?tag=id:G1%20Old%20Gen"
```

#### 示例 2: 监控 HTTP 请求

```bash
# 查看所有 HTTP 请求指标
curl http://localhost:8091/actuator/metrics/http.server.requests

# 查看 200 状态码的请求
curl "http://localhost:8091/actuator/metrics/http.server.requests?tag=status:200"

# 查看特定 URI 的请求
curl "http://localhost:8091/actuator/metrics/http.server.requests?tag=uri:/api/users"
```

#### 示例 3: 监控数据库连接池

```bash
# 查看活跃连接数
curl http://localhost:8091/actuator/metrics/hikaricp.connections.active

# 查看连接池大小
curl http://localhost:8091/actuator/metrics/hikaricp.connections
```

### 安全建议

当前配置中，Actuator 端点对所有请求开放。在生产环境中，建议：

1. **限制访问**: 只允许内网访问
2. **添加认证**: 使用 Spring Security 保护敏感端点
3. **隐藏敏感信息**: 配置 `show-details: never` 或 `when-authorized`

**生产环境配置示例**:
```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics
      base-path: /actuator
  endpoint:
    health:
      show-details: never  # 生产环境隐藏详细信息
  info:
    env:
      enabled: false  # 生产环境禁用环境信息
```

### 学习价值

通过集成 Actuator，我们学习了：

1. **监控的重要性**: 了解监控在微服务架构中的关键作用
2. **端点设计**: 学习如何设计和管理监控端点
3. **指标收集**: 理解如何收集和应用性能指标
4. **健康检查**: 掌握服务健康检查的实现方式
5. **安全配置**: 了解监控端点的安全配置方法

---

## Docker 容器化详解

### 为什么需要 Docker 容器化？

在微服务架构中，启动成本是一个重要问题：

1. **启动成本高**: 需要手动启动多个服务（MySQL、Redis、Eureka、7个微服务）
2. **环境不一致**: 不同开发者的环境可能不同，导致问题难以复现
3. **调试困难**: 需要同时管理多个服务的日志和状态
4. **部署复杂**: 生产环境部署需要配置大量环境变量和依赖

Docker 容器化可以解决这些问题。

### Docker 容器化架构

#### 服务列表

| 服务 | 容器名 | 端口 | 说明 |
|------|--------|------|------|
| MySQL | njumarket-mysql | 3306 | 数据库 |
| Redis | njumarket-redis | 6379 | 缓存 |
| Discovery | njumarket-discovery | 8761 | Eureka 服务注册中心 |
| Gateway | njumarket-gateway | 8080 | API 网关 |
| Auth Service | njumarket-service-auth | 8091 | 认证服务 |
| Commodity Service | njumarket-service-commodity | 8092 | 商品服务 |
| Order Service | njumarket-service-order | 8093 | 订单服务 |
| Message Service | njumarket-service-message | 8094 | 消息服务 |
| Image Service | njumarket-service-image | 8095 | 图片服务 |
| Admin Service | njumarket-service-admin | 8096 | 管理服务 |
| Notification Service | njumarket-service-notification | 8097 | 通知服务 |

#### 文件结构

```
njumarket/
├── Dockerfile                 # 通用 Dockerfile（多阶段构建）
├── docker-compose.yml         # Docker Compose 编排文件
├── .dockerignore             # Docker 忽略文件
├── Makefile                  # Make 命令快捷方式
├── docker/
│   ├── README.md             # Docker 详细文档
│   ├── WINDOWS_GUIDE.md      # Windows 使用指南
│   ├── start.sh             # Linux/Mac 启动脚本
│   └── start.bat            # Windows 启动脚本
└── DOCKER_QUICKSTART.md     # 快速启动指南
```

### 技术实现详解

#### 1. Dockerfile（多阶段构建）

**设计思路**: 使用多阶段构建，将构建和运行环境分离，减小最终镜像体积。

**完整 Dockerfile**:
```dockerfile
# 第一阶段：构建
FROM maven:3.9-eclipse-temurin-17 AS build

WORKDIR /app

# 复制 pom.xml 文件（利用 Docker 缓存层）
COPY pom.xml .
COPY njumarket-common/pom.xml ./njumarket-common/
# ... 复制所有服务的 pom.xml

# 下载依赖（利用缓存）
RUN mvn dependency:go-offline -B

# 复制源代码
COPY . .

# 构建参数：服务名称
ARG SERVICE_NAME
ARG SERVICE_PORT

# 构建指定服务（跳过测试以加快构建速度）
RUN mvn clean package -pl ${SERVICE_NAME} -am -DskipTests -B

# 第二阶段：运行
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# 安装 curl 用于健康检查
RUN apk add --no-cache curl

# 创建非 root 用户
RUN addgroup -S spring && adduser -S spring -G spring

# 声明构建参数（需要在 FROM 之后重新声明）
ARG SERVICE_NAME
ARG SERVICE_PORT=8080

# 从构建阶段复制 JAR 文件
COPY --from=build /app/${SERVICE_NAME}/target/*.jar app.jar

# 切换到非 root 用户
USER spring:spring

# 暴露端口
EXPOSE ${SERVICE_PORT}

# 健康检查
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:${SERVICE_PORT}/actuator/health || exit 1

# 启动应用
ENTRYPOINT ["java", "-jar", "app.jar"]
```

**关键技术点**:

1. **多阶段构建**: 
   - 第一阶段使用 Maven 镜像构建应用
   - 第二阶段使用轻量级 JRE 镜像运行应用
   - 最终镜像只包含运行所需的文件，体积小

2. **缓存优化**:
   - 先复制 `pom.xml`，下载依赖
   - 如果依赖未变化，Docker 会使用缓存层，加快构建速度

3. **构建参数**:
   - `SERVICE_NAME`: 指定要构建的服务
   - `SERVICE_PORT`: 指定服务端口
   - 一个 Dockerfile 可以构建所有服务

4. **安全配置**:
   - 使用非 root 用户运行应用
   - 减少安全风险

5. **健康检查**:
   - 使用 Actuator 健康检查端点
   - Docker 可以自动检测服务健康状态

#### 2. Docker Compose 配置

**设计思路**: 使用 Docker Compose 编排所有服务，管理服务依赖和启动顺序。

**核心配置示例**:
```yaml
services:
  # MySQL 数据库
  mysql:
    image: mysql:8.0
    container_name: njumarket-mysql
    environment:
      MYSQL_ROOT_PASSWORD: Hqz20050316
      MYSQL_DATABASE: nju_market
    ports:
      - "3306:3306"
    volumes:
      - mysql_data:/var/lib/mysql  # 数据持久化
    healthcheck:
      test: ["CMD", "mysqladmin", "ping", "-h", "localhost", "-u", "root", "-pHqz20050316"]
      interval: 10s
      timeout: 5s
      retries: 5
    networks:
      - njumarket-network

  # Auth Service
  auth-service:
    build:
      context: .
      dockerfile: Dockerfile
      args:
        SERVICE_NAME: njumarket-service-auth
        SERVICE_PORT: 8091
    container_name: njumarket-service-auth
    ports:
      - "8091:8091"
    environment:
      - SPRING_PROFILES_ACTIVE=docker
      - AUTH_DATASOURCE_URL=jdbc:mysql://mysql:3306/nju_market?useSSL=false&useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true
      - REDIS_HOST=redis
      - REDIS_PORT=6379
      - EUREKA_CLIENT_SERVICEURL_DEFAULTZONE=http://discovery:8761/eureka
    depends_on:
      discovery:
        condition: service_healthy
      mysql:
        condition: service_healthy
      redis:
        condition: service_healthy
    networks:
      - njumarket-network
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8091/actuator/health"]
      interval: 30s
      timeout: 10s
      retries: 5
      start_period: 60s

volumes:
  mysql_data:    # MySQL 数据卷
  redis_data:    # Redis 数据卷

networks:
  njumarket-network:  # 自定义网络
```

**关键技术点**:

1. **服务依赖管理**:
   - 使用 `depends_on` + `condition: service_healthy`
   - 确保依赖服务健康后才启动当前服务
   - 避免服务启动顺序问题

2. **数据持久化**:
   - 使用 Docker 卷（volumes）存储数据
   - MySQL 数据存储在 `mysql_data` 卷中
   - Redis 数据存储在 `redis_data` 卷中
   - 删除容器不会丢失数据

3. **网络隔离**:
   - 所有服务在同一 Docker 网络中
   - 服务通过服务名访问（如 `mysql:3306`）
   - 外部无法直接访问内部服务

4. **环境变量配置**:
   - 通过环境变量覆盖 `application.yml` 配置
   - 将 `localhost` 替换为服务名（如 `mysql:3306`）
   - 支持不同环境的配置

5. **健康检查**:
   - 每个服务配置健康检查
   - Docker 可以自动检测服务健康状态
   - 支持服务依赖的健康检查条件

#### 3. .dockerignore 文件

**用途**: 排除不需要的文件，减小构建上下文，加快构建速度。

**内容**:
```
target/
.git/
.idea/
.vscode/
*.iml
*.log
*.class
.mvn/
mvnw
mvnw.cmd
scripts/
docs/
*.md
!README.md
```

### 使用方法

#### 快速启动

```bash
# 方式1: 使用 Docker Compose（推荐）
cd njumarket
docker-compose up -d

# 方式2: 使用 Makefile
make build
make up

# 方式3: 使用脚本
cd docker
./start.sh        # Linux/Mac
start.bat         # Windows
```

#### 查看服务状态

```bash
# 查看所有容器状态
docker-compose ps

# 查看日志
docker-compose logs -f

# 查看特定服务日志
docker-compose logs -f auth-service
```

#### 停止服务

```bash
# 停止所有服务
docker-compose down

# 停止并删除数据卷（谨慎使用，会删除数据库数据）
docker-compose down -v
```

### 数据持久化

#### MySQL 数据持久化

MySQL 数据存储在 Docker 卷中，即使删除容器也不会丢失数据。

**查看数据卷**:
```bash
docker volume ls
docker volume inspect njumarket_mysql_data
```

**备份数据**:
```bash
# 导出数据
docker exec njumarket-mysql mysqldump -uroot -pHqz20050316 nju_market > backup.sql

# 导入数据
docker exec -i njumarket-mysql mysql -uroot -pHqz20050316 nju_market < backup.sql
```

#### Redis 数据持久化

Redis 数据也存储在 Docker 卷中，支持数据持久化。

#### 图片文件持久化

`image-service` 的图片文件挂载到本地目录：

```yaml
volumes:
  - ../uploads:/app/uploads  # 挂载到本地 uploads 目录
```

### 网络配置

#### Docker 网络

所有服务在同一 Docker 网络中，服务通过服务名访问：

- MySQL: `mysql:3306`
- Redis: `redis:6379`
- Eureka: `discovery:8761`

#### 服务发现

在 Docker 网络中，服务通过服务名自动发现，无需配置 IP 地址。

### 故障排查

#### 服务无法启动

1. **检查日志**:
   ```bash
   docker-compose logs service-name
   ```

2. **检查依赖服务**:
   ```bash
   docker-compose ps
   ```

3. **检查端口占用**:
   ```bash
   netstat -ano | findstr :8091  # Windows
   lsof -i :8091                 # Linux/Mac
   ```

#### 服务无法连接数据库

1. **检查 MySQL 健康状态**:
   ```bash
   docker-compose exec mysql mysqladmin ping -h localhost -u root -pHqz20050316
   ```

2. **检查网络连接**:
   ```bash
   docker-compose exec auth-service ping mysql
   ```

3. **检查环境变量**:
   ```bash
   docker-compose exec auth-service env | grep DATASOURCE
   ```

#### 构建失败

1. **检查 Maven 依赖**:
   ```bash
   docker-compose build --no-cache auth-service
   ```

2. **检查磁盘空间**:
   ```bash
   docker system df
   ```

3. **清理 Docker 缓存**:
   ```bash
   docker system prune -a
   ```

### 学习价值

通过 Docker 容器化，我们学习了：

1. **容器化技术**: 理解 Docker 的基本概念和使用方法
2. **多阶段构建**: 学习如何优化 Docker 镜像大小
3. **服务编排**: 掌握 Docker Compose 的使用方法
4. **数据持久化**: 理解 Docker 卷的使用
5. **网络配置**: 了解 Docker 网络的工作原理
6. **健康检查**: 掌握服务健康检查的实现
7. **环境变量**: 学习如何通过环境变量配置应用

### 优势总结

1. **一键启动**: 无需手动启动多个服务
2. **环境一致性**: 所有开发者使用相同的运行环境
3. **快速重启**: `docker-compose restart` 快速重启服务
4. **隔离性**: 不影响本地环境
5. **易于清理**: `docker-compose down -v` 完全清理
6. **数据持久化**: 数据存储在 Docker 卷中，不会丢失

---

## API 文档（Swagger）

### 集成状态

✅ **已完成** - 所有服务已集成 SpringDoc OpenAPI（Swagger 3）

### 访问方式

- **Swagger UI**: `http://localhost:{port}/swagger-ui.html`
- **OpenAPI JSON**: `http://localhost:{port}/v3/api-docs`

### 联调测试

在 `auth-service` 的 Swagger UI 中获取 Token 后，即可在其他服务的 Swagger UI 中进行联调测试。

---

## 使用指南

### 启动项目

#### 方式 1: Docker Compose（推荐）

```bash
cd njumarket
docker-compose up -d
```

#### 方式 2: 本地运行

1. 启动基础设施（MySQL、Redis）
2. 启动 Eureka Discovery Server
3. 启动各微服务

### 验证服务

#### 1. 检查服务注册

访问 Eureka Dashboard: http://localhost:8761

应看到所有服务已注册。

#### 2. 检查服务健康

```bash
# Gateway 健康检查
curl http://localhost:8080/actuator/health

# Auth Service 健康检查
curl http://localhost:8091/actuator/health
```

#### 3. 访问 API 文档

- Auth Service: http://localhost:8091/swagger-ui.html
- Commodity Service: http://localhost:8092/swagger-ui.html
- Order Service: http://localhost:8093/swagger-ui.html
- Message Service: http://localhost:8094/swagger-ui.html
- Image Service: http://localhost:8095/swagger-ui.html
- Admin Service: http://localhost:8096/swagger-ui.html
- Notification Service: http://localhost:8097/swagger-ui.html

### 监控服务

#### 查看指标

```bash
# 查看所有指标
curl http://localhost:8091/actuator/metrics

# 查看 JVM 内存使用
curl http://localhost:8091/actuator/metrics/jvm.memory.used

# 查看 HTTP 请求统计
curl http://localhost:8091/actuator/metrics/http.server.requests
```

#### 查看应用信息

```bash
curl http://localhost:8091/actuator/info
```

---

## 技术总结

### v2.1.0 完成的技术点

1. **Spring Boot Actuator**
   - 健康检查端点
   - 应用信息端点
   - 指标收集端点
   - 安全配置

2. **Docker 容器化**
   - 多阶段构建 Dockerfile
   - Docker Compose 服务编排
   - 数据持久化
   - 网络配置
   - 健康检查

3. **API 文档**
   - SpringDoc OpenAPI 集成
   - Swagger UI 配置
   - 在线测试支持

### 学习收获

1. **监控技术**: 理解了监控在微服务架构中的重要性
2. **容器化技术**: 掌握了 Docker 和 Docker Compose 的使用
3. **服务编排**: 学会了如何编排和管理多个服务
4. **数据持久化**: 理解了容器数据持久化的方法
5. **健康检查**: 掌握了服务健康检查的实现

### 下一步计划

根据 2.x 版本规划，下一步将完成：

1. **Resilience4j**: 服务熔断与降级
2. **Spring Cloud Config**: 配置中心
3. **Spring Cloud Sleuth + Zipkin**: 分布式链路追踪
4. **Prometheus + Grafana**: 监控指标收集和可视化

---

## 参考资源

### Spring Boot Actuator
- [Spring Boot Actuator 官方文档](https://docs.spring.io/spring-boot/docs/current/reference/html/actuator.html)
- [Actuator 端点列表](https://docs.spring.io/spring-boot/docs/current/reference/html/actuator.html#actuator.endpoints)

### Docker
- [Docker 官方文档](https://docs.docker.com/)
- [Docker Compose 文档](https://docs.docker.com/compose/)
- [Spring Boot Docker 指南](https://spring.io/guides/gs/spring-boot-docker/)

### Swagger/OpenAPI
- [SpringDoc OpenAPI 文档](https://springdoc.org/)
- [OpenAPI 规范](https://swagger.io/specification/)

---

**版本**: v2.1.0  
**状态**: ✅ **已完成**  
**日期**: 2025-11-11

