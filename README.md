# 南大集市 NJUMarket

一个基于微服务架构 + 语义搜索 + LangChain4j AI Agent 的校园二手交易平台。

## 📋 项目简介

NJUMarket 是一个采用微服务架构的校园二手交易平台，支持商品发布、订单管理、实时消息等功能。

**v3.1.1 亮点**：
- ✅ **LangChain4j AI Agent**：Cursor-like 对话、Function Calling 搜索工具、自动推荐商品
- ✅ **语义搜索**：商品、用户画像、对话均向量化（pgvector），支持自然语言检索
- ✅ **统一过滤**：ES + AI 搜索统一过滤库存、可见性、卖家身份，保证结果真实可售
- ✅ **库存一致性**：Redis 分布式锁 + 数据库悲观锁 + 条件更新，多层保障
- ✅ **快速部署**：Docker Compose 一键拉起所有服务，附详细运维手册
- ✅ **监控治理**：Actuator、Zipkin、Prometheus/Grafana、Resilience4j 全覆盖

- **架构**: Spring Cloud 微服务 + 向量检索 + AI Agent
- **后端**: Spring Boot 3.2.x、Spring Cloud 2023.0.x、LangChain4j 0.35
- **前端**: Vue 3 + Vite + Element Plus
- **数据库**: PostgreSQL 16 + pgvector、Redis 7、Elasticsearch 8.13（IK 分词器）

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

> ⚠️ **首次启动后请进入容器完成扩展安装**  
> - Elasticsearch：`bin/elasticsearch-plugin install https://get.infini.cloud/elasticsearch/analysis-ik/8.13.4`  
> - PostgreSQL：`apt install -y postgresql-16-pgvector && CREATE EXTENSION vector;`  
> 详细步骤见 [docker/README.md](./njumarket/docker/README.md#额外准备elasticsearch-ik-插件--postgresql-pgvector)

**详细文档**：查看 [DOCKER_QUICKSTART.md](./njumarket/DOCKER_QUICKSTART.md) 和 [docker/README.md](./njumarket/docker/README.md)

---

### 方式二：本地运行

#### 前置要求

- **JDK**: 17+
- **Maven**: 3.6+
- **PostgreSQL**: 16（需启用 pgvector）
- **Elasticsearch**: 8.13（需安装 IK 插件）
- **Redis**: 6.0+
- **IDE**: IntelliJ IDEA / VS Code（推荐）

#### 配置步骤

#### 1. 克隆项目

```bash
git clone <repository-url>
cd NJUMarket/njumarket
```

#### 2. 配置数据库

1. **创建数据库 & 启用 pgvector**
   ```sql
   CREATE DATABASE njumarket;
   \c njumarket
   CREATE SCHEMA nju_market;
   CREATE EXTENSION IF NOT EXISTS vector;
   ```

2. **导入结构**
   ```bash
   psql -U postgres -d njumarket -f database/schema.sql
   ```

3. **配置数据库连接**
   修改各服务 `application.yml`：
   ```yaml
   spring:
     datasource:
       url: jdbc:postgresql://localhost:5432/njumarket?currentSchema=nju_market
       username: postgres
       password: your_password
   ```

4. **本地 Elasticsearch**  
   启动 8.13.x 版本，并安装 IK 插件：  
   `bin/elasticsearch-plugin install https://get.infini.cloud/elasticsearch/analysis-ik/8.13.4`

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
│   ├── PROJECT_DOCUMENTATION_V3.1.1.md  # 最新版本总览
│   ├── PROJECT_DOCUMENTATION_V3.1.0.md  # LangChain4j / 用户画像 / AI Agent
│   ├── PROJECT_DOCUMENTATION_V3.0.0.md  # Spring AI → LangChain4j 迁移
│   ├── PROJECT_DOCUMENTATION_V2.x*.md   # 历史版本档案
│   ├── PROJECT_SUMMARY_INTERNSHIP.md    # 实习向总结（偏后端）
│   └── BATCH_QUERY_ANALYSIS.md          # 批量查询分析报告
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

- `database/schema.sql` 仅包含结构（PostgreSQL + pgvector），**不含测试数据**
- 需先执行 `CREATE DATABASE njumarket;` 和 `CREATE EXTENSION vector;`
- 测试用户可通过注册接口或脚本创建
- 管理员账号需要手动创建（参考 `database/README.md`）

### 测试数据

使用 `scripts/batch_create_users_simple.py` 批量创建测试用户：

```bash
cd scripts
pip install -r requirements.txt
python batch_create_users_simple.py
```

### 管理端功能（v2.x 完整实现）

管理后台覆盖以下能力：
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
- `docs/PROJECT_DOCUMENTATION_V3.1.1.md`：当前版本综述（AI Agent、语义搜索）
- `docs/PROJECT_DOCUMENTATION_V3.1.0.md`：LangChain4j 迁移、用户画像生成
- `docs/PROJECT_DOCUMENTATION_V3.0.0.md`：AI 语义搜索初始版本
- `docs/PROJECT_SUMMARY_INTERNSHIP.md`：面向实习/面试的项目总结
- 历史版本可查阅 `docs/PROJECT_DOCUMENTATION_V2.*.md`
- `docs/BATCH_QUERY_ANALYSIS.md`：批量查询优化分析

### Docker 文档
- **Docker 快速启动**: `DOCKER_QUICKSTART.md` - Docker 快速启动指南
- **Docker 详细文档**: `docker/README.md` - Docker 完整使用指南、故障排查、开发模式

### 其他文档
- **数据库说明**: `database/README.md`
- **测试脚本**: `scripts/README.md`

## 📊 版本信息

**当前版本**: v3.1.1 ✅ 已完成最初规划功能

**版本里程碑（摘选）**：
- **v3.1.1 (2025)**：搜索过滤统一、ThreadLocal 清理、部署文档升级
- **v3.1.0 (2025)**：LangChain4j AI Agent、用户画像、对话向量检索
- **v3.0.0 (2025)**：语义搜索、Spring AI → LangChain4j 迁移
- **v2.x 系列 (2024-2025)**：微服务拆分、管理端完成、容器化、监控治理

## ⚠️ 常见问题

### Docker 相关问题

**镜像拉取失败**：
- 配置 Docker 镜像加速器（参考 `docker/README.md`）
- 或手动拉取镜像：`docker pull maven:3.9-eclipse-temurin-17`

- **端口被占用**：
- 停止本地 PostgreSQL/Redis 服务：`net stop postgresql-x64-16`
- 或修改 `docker-compose.yml` 中的端口映射

**PostgreSQL 初始化失败**：
- 手动导入结构：`docker-compose exec -T postgres psql -U postgres -d njumarket < database/schema.sql`

**详细故障排查**：参考 [docker/README.md](./njumarket/docker/README.md) 的"故障排查"章节

### 本地运行问题

**服务无法启动**：

1. **检查端口占用**：确保 8761、8080、8091-8097 未被占用
2. **检查 PostgreSQL**：确认服务运行、pgvector 已启用、数据库/Schema 已创建
3. **检查 Elasticsearch**：确认 9200 端口可用且 IK 插件已安装
4. **检查 Redis**：确认 6379 端口可访问
5. **检查 JDK**：确保使用 JDK 17+

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

**混合模式**：只启动基础设施（PostgreSQL、Redis、Elasticsearch），本地运行服务
```bash
docker-compose up -d postgres redis elasticsearch
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

