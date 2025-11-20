# 南大集市 NJUMarket

一个基于微服务架构的校园二手交易平台。

## 📋 项目简介

NJUMarket 是一个采用微服务架构的校园二手交易平台，支持商品发布、订单管理、实时消息等功能。

**v2.1版本特点**：
- ✅ **完整功能**：用户端和管理端功能全部实现
- ✅ **微服务架构**：7个微服务，服务注册与发现、API网关、服务间通信全部完成
- ✅ **数据一致性**：消息软删除时自动更新会话最新消息（用户端和管理端均已实现）
- ✅ **管理端功能**：用户管理、商品管理、订单管理、会话管理、消息管理、管理员管理等全部功能
- ✅ **代码质量**：反射滥用问题已解决，使用Spring Security标准注解，符合开发规范
- ✅ **性能优化**：广泛使用批量查询，防止N+1查询问题，Feign调用次数为常数次
- ✅ **Docker容器化**：一键启动所有服务，简化部署和调试流程（v2.1.0）
- ✅ **监控与文档**：Spring Boot Actuator监控、Swagger API文档（v2.1.0）
- ✅ **熔断降级**：Resilience4j熔断降级，防止服务雪崩（v2.1.1）
- ✅ **配置中心**：Spring Cloud Config Server集中管理配置，支持环境隔离（v2.1.1）

- **架构**: 微服务架构（Spring Cloud）
- **后端**: Spring Boot 3.2.0 + Spring Cloud 2023.0.3
- **前端**: Vue 3 + Element Plus
- **数据库**: MySQL 8.0+
- **缓存**: Redis 6.0+

## 🚀 快速开始

### 方式一：Docker 容器化（推荐）⭐

**一键启动所有服务**，无需手动配置数据库和Redis：

```bash
# 进入项目目录
cd NJUMarket/njumarket

# 方式1: 使用启动脚本（推荐新手）
cd docker
start.bat        # Windows
./start.sh       # Linux/Mac

# 方式2: 使用 Docker Compose
docker-compose up -d --build

# 方式3: 使用 Makefile
make build
make up
```

**验证服务**：
- Eureka Dashboard: http://localhost:8761
- API Gateway: http://localhost:8080
- 健康检查: http://localhost:8080/actuator/health

**详细文档**：查看 [DOCKER_QUICKSTART.md](./njumarket/DOCKER_QUICKSTART.md) 或 [docker/README.md](./njumarket/docker/README.md)

---

### 方式二：本地运行

#### 前置要求

- **JDK**: 17+
- **Maven**: 3.6+
- **MySQL**: 8.0+
- **Redis**: 6.0+
- **IDE**: IntelliJ IDEA / VS Code（推荐）

#### 配置步骤

#### 1. 克隆项目

```bash
git clone <repository-url>
cd NJUMarket/njumarket
```

#### 2. 配置数据库

**创建数据库**：
```sql
CREATE DATABASE nju_market CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

**初始化数据库结构**：
```bash
mysql -u root -p nju_market < database/schema.sql
```

**配置数据库连接**：
修改各服务的 `application.yml` 中的数据库配置：
```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/nju_market?useSSL=false&useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai
    username: your_username
    password: your_password
```

#### 3. 配置Redis

确保Redis服务已启动，默认配置：
- **Host**: localhost
- **Port**: 6379
- **Password**: 根据实际情况修改（可在 `application.yml` 中使用环境变量）

#### 4. 导入项目到IDE

**IntelliJ IDEA**：
1. File → Open → 选择 `njumarket` 目录
2. 等待Maven自动导入依赖
3. 确保JDK版本为17+

**VS Code**：
1. 打开 `njumarket` 目录
2. 安装Java扩展包（Extension Pack for Java）
3. 等待Maven依赖下载完成

#### 5. 启动服务

**启动顺序**：
1. **Eureka Discovery Server** (端口: 8761)
   - 运行 `njumarket-discovery` 模块的 `DiscoveryServerApplication`
   - 验证: 访问 http://localhost:8761

2. **API Gateway** (端口: 8080)
   - 运行 `njumarket-gateway` 模块的 `GatewayApplication`

3. **各微服务**（可并行启动）
   - `AuthServiceApplication` (8091) - 认证服务
   - `CommodityServiceApplication` (8092) - 商品服务
   - `OrderServiceApplication` (8093) - 订单服务
   - `MessageServiceApplication` (8094) - 消息服务
   - `ImageServiceApplication` (8095) - 图片服务
   - `AdminServiceApplication` (8096) - 管理服务（✅ v2.0完整实现）
   - `NotificationServiceApplication` (8097) - 通知服务

**验证服务注册**：
访问 http://localhost:8761 查看Eureka控制台，确认所有服务已注册。

#### 6. 测试API

通过Gateway访问API：
```bash
# 测试健康检查
curl http://localhost:8080/actuator/health

# 测试商品搜索
curl http://localhost:8080/api/public/commodity/search
```

## 📁 项目结构

```
njumarket/
├── pom.xml                          # 父POM文件
├── mvnw, mvnw.cmd                   # Maven Wrapper
│
├── njumarket-common/                # 公共模块
│   └── 实体类、DTO、工具类、异常处理等
│
├── njumarket-discovery/             # 服务注册中心（Eureka）
│   └── DiscoveryServerApplication
│
├── njumarket-gateway/               # API网关
│   └── GatewayApplication
│
├── njumarket-service-auth/          # 认证服务
│   └── AuthServiceApplication
│
├── njumarket-service-commodity/     # 商品服务
│   └── CommodityServiceApplication
│
├── njumarket-service-order/        # 订单服务
│   └── OrderServiceApplication
│
├── njumarket-service-message/      # 消息服务
│   └── MessageServiceApplication
│
├── njumarket-service-image/        # 图片服务
│   └── ImageServiceApplication
│
├── njumarket-service-admin/        # 管理服务
│   └── AdminServiceApplication
│
├── njumarket-service-notification/ # 通知服务
│   └── NotificationServiceApplication
│
├── database/                       # 数据库脚本
│   ├── schema.sql                  # 数据库结构
│   └── README.md                   # 数据库说明
│
├── docker/                         # Docker 容器化
│   ├── README.md                   # Docker 详细文档
│   ├── start.bat                   # Windows 启动脚本
│   └── start.sh                    # Linux/Mac 启动脚本
│
├── Dockerfile                      # Docker 构建文件
├── docker-compose.yml              # Docker Compose 编排文件
├── DOCKER_QUICKSTART.md            # Docker 快速启动指南
│
├── docs/                          # 项目文档
│   ├── PROJECT_DOCUMENTATION_V2.0.md  # 2.0版本总览
│   ├── PROJECT_DOCUMENTATION_V2.0.2.md  # v2.0.2详细文档（2.0阶段完成）
│   ├── PROJECT_DOCUMENTATION_V2.1.0.md  # v2.1.0详细文档（Actuator、Docker、Swagger）
│   └── BATCH_QUERY_ANALYSIS.md    # 批量查询分析报告
│
└── scripts/                        # 测试脚本
    ├── batch_create_users_simple.py  # 批量创建用户
    └── 线程组.jmx                    # JMeter测试计划
```

## 🔧 服务端口

| 服务 | 端口 | 说明 |
|------|------|------|
| Eureka Discovery | 8761 | 服务注册中心 |
| API Gateway | 8080 | API网关（统一入口） |
| Auth Service | 8091 | 认证服务 |
| Commodity Service | 8092 | 商品服务 |
| Order Service | 8093 | 订单服务 |
| Message Service | 8094 | 消息服务 |
| Image Service | 8095 | 图片服务 |
| Admin Service | 8096 | 管理服务 |
| Notification Service | 8097 | 通知服务 |

## 📝 重要说明

### 环境变量配置

建议使用环境变量配置敏感信息：

```bash
# Windows
set REDIS_PASSWORD=your_password
set DB_PASSWORD=your_password

# Linux/Mac
export REDIS_PASSWORD=your_password
export DB_PASSWORD=your_password
```

### 数据库初始化

- `database/schema.sql` 仅包含数据库结构，**不包含测试数据**
- 测试用户可通过后端注册API创建
- 管理员账号需要手动创建（参考 `database/README.md`）

### 测试数据

使用 `scripts/batch_create_users_simple.py` 批量创建测试用户：

```bash
cd scripts
pip install -r requirements.txt
python batch_create_users_simple.py
```

### v2.0版本管理端功能

管理端功能已完整实现，包括：
- ✅ **用户管理**：列表、详情、状态管理、信息编辑、删除
- ✅ **商品管理**：列表、详情、状态管理、信息编辑、删除（支持搜索卖家昵称）
- ✅ **订单管理**：列表、详情、状态修改、信息编辑、删除（支持搜索买家/卖家昵称）
- ✅ **会话管理**：列表、详情、信息编辑、删除
- ✅ **消息管理**：列表、详情、信息编辑、删除（支持软删除同步最新消息）
- ✅ **管理员管理**：列表、创建、编辑、删除、权限管理（仅system权限可用）
- ✅ **数据统计**：用户总数、商品总数、订单总数实时查询

**管理端访问**：
- 前端地址：`http://localhost:8082`（管理端前端）
- API地址：`http://localhost:8080/api/admin/**`（通过Gateway，前端代理自动转发）

## 📚 相关文档

### 项目文档
- **v2.0总览**: `docs/PROJECT_DOCUMENTATION_V2.0.md` - 2.0版本总览和架构设计
- **v2.0.2文档**: `docs/PROJECT_DOCUMENTATION_V2.0.2.md` - v2.0.2详细文档（反射优化、Spring Security标准注解）✅ **2.0阶段已完成**
- **v2.1.0文档**: `docs/PROJECT_DOCUMENTATION_V2.1.0.md` - v2.1.0详细文档（Actuator监控、Docker容器化、Swagger API文档）✅ **2.1.0阶段已完成**
- **批量查询分析**: `docs/BATCH_QUERY_ANALYSIS.md` - 批量查询使用情况分析报告

### Docker 文档
- **Docker 快速启动**: `DOCKER_QUICKSTART.md` - Docker 快速启动指南
- **Docker 详细文档**: `docker/README.md` - Docker 完整使用指南、故障排查、开发模式

### 其他文档
- **数据库说明**: `database/README.md`
- **测试脚本**: `scripts/README.md`

## 📊 版本信息

**当前版本**: v2.1.0 ✅ **2.1.0阶段已完成**

**版本历史**：
- **v2.0.0** (2024年): 从单体到微服务的架构迁移完成
- **v2.0.1** (2025-11-09): DTO验证优化、异常处理完善、关键Bug修复
- **v2.0.2** (2025-11-10): 反射滥用问题解决、使用Spring Security标准注解 ✅ **2.0阶段已完成**
- **v2.1.0** (2025-11-11): Actuator监控、Docker容器化、Swagger API文档 ✅ **2.1.0阶段已完成**

**2.0阶段核心成就**：
- ✅ 微服务架构完整实现（7个微服务）
- ✅ 用户端和管理端功能全部完成
- ✅ 代码质量提升（DTO验证、异常处理、反射优化）
- ✅ 符合开发规范（使用Spring Security标准注解）
- ✅ 性能优化（批量查询，防止N+1问题）

**2.1.0阶段核心成就**：
- ✅ Spring Boot Actuator 监控集成（健康检查、指标收集）
- ✅ Docker 容器化完成（一键启动所有服务）
- ✅ Swagger 3 API 文档集成
- ✅ 文档整理优化（Docker 文档合并，便于上手）

## ⚠️ 常见问题

### Docker 相关问题

**镜像拉取失败**：
- 配置 Docker 镜像加速器（参考 `docker/README.md`）
- 或手动拉取镜像：`docker pull maven:3.9-eclipse-temurin-17`

**端口被占用**：
- 停止本地 MySQL/Redis 服务：`net stop MySQL80`
- 或修改 `docker-compose.yml` 中的端口映射

**MySQL 初始化失败**：
- 手动导入数据库：`Get-Content database\schema.sql -Encoding UTF8 | docker exec -i njumarket-mysql mysql -uroot -pHqz20050316 nju_market`

**详细故障排查**：参考 [docker/README.md](./njumarket/docker/README.md) 的"故障排查"章节

### 本地运行问题

**服务无法启动**：

1. **检查端口占用**：确保8761, 8080, 8091-8097未被占用
2. **检查数据库连接**：确认MySQL服务运行正常，数据库已创建
3. **检查Redis连接**：确认Redis服务运行正常
4. **检查JDK版本**：确保使用JDK 17+

### 服务无法注册到Eureka

1. **检查Eureka Server**：确保Discovery Server已启动
2. **检查配置**：确认各服务的Eureka地址配置正确
3. **检查网络**：确认服务间网络连通

### Maven依赖下载失败

1. **检查网络**：确保能访问Maven中央仓库
2. **清理缓存**：删除 `~/.m2/repository` 后重新下载
3. **使用镜像**：配置Maven镜像（如阿里云镜像）

## 🎯 开发建议

### Docker 开发模式（推荐）

**混合模式**：只启动基础设施（MySQL、Redis），本地运行服务
```bash
docker-compose up -d mysql redis
```
然后修改本地 `application.yml` 中的连接地址为 `localhost`，在 IDE 中运行服务。

**优势**：
- 代码热重载
- 快速调试
- 不占用过多资源

### 本地开发模式

1. **使用IDE运行**：推荐使用IntelliJ IDEA，可直接运行各服务的Application类
2. **查看日志**：各服务日志会输出到控制台，注意查看错误信息
3. **Eureka控制台**：定期查看 http://localhost:8761 确认服务状态
4. **API测试**：使用Postman或curl测试API接口
5. **Swagger文档**：访问各服务的 `/swagger-ui.html` 查看API文档

## 📄 许可证

本项目为软件工程课程项目。

---

**提示**：
- **Docker 容器化**：推荐使用 Docker 一键启动，详见 [DOCKER_QUICKSTART.md](./njumarket/DOCKER_QUICKSTART.md)
- **项目文档**：更多详细信息请参考 `docs/PROJECT_DOCUMENTATION_V2.1.0.md`

