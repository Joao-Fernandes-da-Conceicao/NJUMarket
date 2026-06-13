# Docker 容器化部署指南

## 📋 目录

- [概述](#概述)
- [前置要求](#前置要求)
- [快速开始](#快速开始)
- [服务访问地址](#服务访问地址)
- [常用命令](#常用命令)
- [数据持久化](#数据持久化)
- [故障排查](#故障排查)
- [开发模式](#开发模式)

## 概述

NJUMarket 项目已完全容器化，使用 Docker Compose 一键启动所有服务。所有服务（PostgreSQL、Redis、Eureka、Gateway 和 7 个微服务）都可以通过 `docker-compose` 命令统一管理。

## 前置要求

- **Docker Desktop**（Windows/Mac）或 **Docker Engine**（Linux）
- **Docker Compose** v2.0+
- **至少 8GB 可用内存**
- **至少 10GB 可用磁盘空间**

### Windows 环境检查

```powershell
# 验证 Docker 是否运行
docker --version
docker-compose --version
docker ps

# 验证 WSL 2
wsl --list --verbose
```

如果命令执行失败，请确保 Docker Desktop 已启动（任务栏右下角应该有 Docker 图标）。

## 快速开始

### 方式一：使用启动脚本（推荐新手）

**Windows**：
```powershell
cd docker
.\start.bat
```

**Linux/Mac**：
```bash
cd docker
chmod +x start.sh
./start.sh
```

### 方式二：使用 Docker Compose（推荐）

```bash
# 在项目根目录（njumarket/）执行

# 1. 构建镜像（首次运行需要 10-20 分钟）
docker-compose build

# 2. 启动所有服务
docker-compose up -d

# 3. 查看服务状态
docker-compose ps
```

### 额外准备：Elasticsearch IK 插件 & PostgreSQL pgvector

容器启动后，需要在 **Elasticsearch** 与 **PostgreSQL** 内安装以下扩展，确保中文搜索和向量检索功能可用。

#### 1) Elasticsearch 安装 IK 分词器（8.13.4）

```bash
# 安装 IK 插件（国内镜像地址）
docker-compose exec elasticsearch \
  bash -c "bin/elasticsearch-plugin install https://get.infini.cloud/elasticsearch/analysis-ik/8.13.4"

# 安装完成后重启 ES
docker-compose restart elasticsearch
```

> 如果提示需要确认，输入 `y`；可通过 `curl http://localhost:9200/_cat/plugins` 检查插件列表。

#### 2) PostgreSQL 安装 pgvector 扩展

```bash
docker-compose exec postgres bash -c "
  apt update &&
  apt install -y postgresql-16-pgvector &&
  psql -U postgres -d njumarket -c \"CREATE EXTENSION IF NOT EXISTS vector;\""
```

> `pgvector` 仅需安装一次，安装后即可写入/查询商品、用户画像等向量数据。

### 方式三：使用 Makefile

```bash
make build    # 构建镜像
make up       # 启动服务
make logs     # 查看日志
make ps       # 查看状态
make down     # 停止服务
```

## 服务访问地址

启动成功后，可以通过以下地址访问服务：

| 服务 | 地址 | 说明 |
|------|------|------|
| Eureka Dashboard | http://localhost:8761 | 服务注册中心 |
| API Gateway | http://localhost:8080 | API 网关 |
| Auth Service | http://localhost:8091 | 认证服务 |
| Trade Service（商品+订单） | http://localhost:8092 | 交易服务 |
| Message Service | http://localhost:8094 | 消息服务 |
| Image Service | http://localhost:8095 | 图片服务 |
| Admin Service | http://localhost:8096 | 管理服务 |
| Notification Service | http://localhost:8097 | 通知服务 |

### 健康检查

所有服务都提供了 Actuator 健康检查端点：

```bash
# 检查 Gateway 健康状态
curl http://localhost:8080/actuator/health

# 检查 Auth Service 健康状态
curl http://localhost:8091/actuator/health
```

### 验证服务注册

访问 http://localhost:8761，应该看到以下服务已注册：
- njumarket-gateway
- njumarket-service-auth
- njumarket-service-trade
- njumarket-service-message
- njumarket-service-image
- njumarket-service-admin
- njumarket-service-notification

## 常用命令

### 查看日志

```bash
# 查看所有服务日志
docker-compose logs -f

# 查看特定服务日志
docker-compose logs -f auth-service
docker-compose logs -f gateway

# 查看最近 100 行日志
docker-compose logs --tail=100 auth-service
```

### 重启服务

```bash
# 重启所有服务
docker-compose restart

# 重启特定服务
docker-compose restart auth-service
```

### 停止服务

```bash
# 停止所有服务（保留容器）
docker-compose stop

# 停止并删除容器
docker-compose down

# 停止并删除容器和数据卷（⚠️ 会删除数据库数据）
docker-compose down -v
```

### 进入容器

```bash
# 进入 PostgreSQL 容器
docker-compose exec postgres bash

# 进入 Redis 容器
docker-compose exec redis sh

# 进入服务容器（示例：auth-service）
docker-compose exec auth-service sh
```

### 查看资源使用

```bash
# 查看容器资源使用情况
docker stats

# 查看特定容器
docker stats njumarket-gateway
```

### 重新构建

```bash
# 停止服务
docker-compose down

# 重新构建（不使用缓存）
docker-compose build --no-cache

# 启动服务
docker-compose up -d
```

## 数据持久化

### PostgreSQL 数据

PostgreSQL 数据存储在 Docker 卷 `postgres_data` 中，即使删除容器，数据也会保留。

**备份数据**：
```bash
# 备份 PostgreSQL 数据
docker-compose exec postgres pg_dump -U postgres -d njumarket > backup.sql

# 恢复 PostgreSQL 数据
docker-compose exec -T postgres psql -U postgres -d njumarket < backup.sql
```

### Redis 数据

Redis 数据存储在 Docker 卷 `njumarket_redis_data` 中。

### 查看数据卷

```bash
# 列出所有数据卷
docker volume ls

# 查看数据卷详情
docker volume inspect njumarket_postgres_data
```

## 故障排查

### 问题 1：Docker 镜像拉取失败（网络问题）⚠️ **常见问题**

**错误信息**：
```
failed to fetch oauth token: Post "https://auth.docker.io/token": dial tcp ... connectex: A connection attempt failed
```

**解决方法**：

#### 方法 A：配置 Docker 镜像加速器（推荐）

1. **打开 Docker Desktop 设置**：
   - 右键任务栏 Docker 图标 → Settings

2. **进入 Docker Engine**：
   - 左侧菜单 → Docker Engine

3. **添加镜像加速器配置**：
   ```json
   {
     "registry-mirrors": [
       "https://docker.m.daocloud.io",
       "https://dockerproxy.com",
       "https://docker.mirrors.ustc.edu.cn"
     ]
   }
   ```

4. **应用并重启**：
   - 点击 "Apply & Restart"
   - 等待 Docker Desktop 重启

5. **验证配置**：
   ```powershell
   docker info
   ```
   应该能看到 `Registry Mirrors` 配置

#### 方法 B：手动拉取镜像（适合网络不稳定环境）

```powershell
# 手动拉取所有需要的镜像（可以慢慢等待，不会超时）
docker pull maven:3.9-eclipse-temurin-17
docker pull eclipse-temurin:17-jre-alpine
docker pull mysql:8.0
docker pull redis:7-alpine

# 然后再运行构建（此时镜像已本地存在，不会再次拉取）
docker-compose build
```

### 问题 2：端口被占用 ⚠️ **常见问题**

**错误信息**：
```
Error: bind: address already in use
ports are not available: exposing port TCP 0.0.0.0:3306 -> ...: bind: Only one usage of each socket address (protocol/network address/port) is normally permitted.
```

**解决方法**：

#### 方法 A：停止占用端口的服务（推荐）

```powershell
# 查看端口占用情况
netstat -ano | findstr :3306

# 停止 PostgreSQL 服务（如果是 PostgreSQL 占用）
net stop postgresql-x64-16
# 或者
net stop postgresql-x64-15

# 停止其他服务（根据进程名称）
tasklist | findstr <PID>
net stop <服务名称>
```

#### 方法 B：修改 Docker 端口映射

如果不想停止本地服务，可以修改 `docker-compose.yml` 使用不同的端口：

```yaml
postgres:
  ports:
    - "5433:5432"  # 改为 5433，本地访问时使用 5433
```

**注意**：修改端口后，需要同步更新所有服务的数据库连接配置。

**常见端口占用情况**：

| 端口 | 常见占用服务 | 解决方法 |
|------|------------|---------|
| 5432 | PostgreSQL | 停止本地 PostgreSQL 服务或修改 Docker 端口映射 |
| 6379 | Redis | `net stop Redis` 或修改 Docker 端口映射 |
| 8080 | Tomcat/其他 Web 服务 | 停止服务或修改 Docker 端口映射 |
| 8761 | Eureka | 通常不会冲突，如果冲突则修改端口 |

### 问题 3：PostgreSQL 容器启动失败

**错误信息**：
```
ERROR: ASCII '\0' appeared in the statement, but this is not allowed unless option --binary-mode is enabled
```

**常见原因**：数据目录损坏或端口被占用

**解决方法**：

1. **清理旧的 PostgreSQL 数据卷**：
   ```powershell
   docker-compose down -v
   ```

2. **重新启动 PostgreSQL**：
   ```powershell
   docker-compose up -d postgres
   ```

3. **等待 PostgreSQL 启动完成**：
   ```powershell
   docker-compose ps postgres
   # 应该显示为 Up (healthy)
   ```

4. **检查日志**：
   ```powershell
   docker-compose logs postgres
   ```

### 问题 4：服务启动失败

**查看日志**：
```bash
# 查看失败服务的日志
docker-compose logs auth-service

# 查看所有服务状态
docker-compose ps
```

**常见原因**：
- PostgreSQL 未启动：等待 PostgreSQL 健康检查通过
- Redis 未启动：等待 Redis 健康检查通过
- Eureka 未启动：等待 Eureka 启动完成

**解决方法**：
```bash
# 查看 PostgreSQL 日志
docker-compose logs postgres

# 查看 Redis 日志
docker-compose logs redis

# 检查服务依赖
docker-compose ps
```

### 问题 5：服务无法连接数据库

**检查 PostgreSQL 是否运行**：
```bash
docker-compose exec postgres pg_isready -U postgres -d njumarket
```

**检查数据库是否已初始化**：
```bash
docker-compose exec postgres psql -U postgres -d njumarket -c "\dt nju_market.*"
```


### 问题 6：服务无法注册到 Eureka

1. **检查 Eureka 是否启动**：
   ```bash
   curl http://localhost:8761
   ```

2. **检查服务日志**：
   ```bash
   docker-compose logs auth-service | grep -i eureka
   ```

3. **检查网络连接**：
   ```bash
   docker-compose exec auth-service ping discovery
   ```

### 问题 7：内存不足

如果遇到内存不足的问题：

1. **增加 Docker 内存限制**（Docker Desktop）：
   - Settings → Resources → Memory → 至少 8GB

2. **减少并发启动的服务数量**：
   ```bash
   # 分批启动
   docker-compose up -d postgres redis discovery
   docker-compose up -d gateway auth-service
   # ...
   ```

### 问题 8：Docker Desktop 未运行

**错误信息**：
```
error during connect: This error may indicate that the docker daemon is not running
```

**解决方法**：
1. 启动 Docker Desktop
2. 等待 Docker Desktop 完全启动（任务栏图标不再闪烁）
3. 重新运行命令

## 开发模式

### 混合模式（推荐用于开发）

只启动基础设施（PostgreSQL、Redis），本地运行服务：

```bash
# 只启动基础设施
docker-compose up -d postgres redis
```

然后修改本地 `application.yml` 中的连接地址为 `localhost`，在 IDE 中运行服务。

**优势**：
- 代码热重载
- 快速调试
- 不占用过多资源

### 完全容器化模式（用于测试/演示）

```bash
docker-compose up -d
```

**优势**：
- 环境一致性
- 一键启动
- 完整测试

## 环境变量配置

所有服务的配置都通过环境变量传递，可以在 `docker-compose.yml` 中修改：

- **数据库配置**：`*_DATASOURCE_URL`、`*_DATASOURCE_USERNAME`、`*_DATASOURCE_PASSWORD`
- **Redis 配置**：`REDIS_HOST`、`REDIS_PORT`、`REDIS_PASSWORD`
- **Eureka 配置**：`EUREKA_CLIENT_SERVICEURL_DEFAULTZONE`

## 清理

### 清理未使用的资源

```bash
# 清理未使用的镜像
docker image prune -a

# 清理未使用的容器
docker container prune

# 清理未使用的数据卷（谨慎使用）
docker volume prune
```

### 完全重置

```bash
# 停止所有服务并删除所有资源
docker-compose down -v
docker system prune -a
```

## VS Code Docker 插件使用

### 快速操作容器

在 VS Code Docker 面板中：
- 右键容器 → Start / Stop / Restart
- 右键容器 → View Logs（查看日志）
- 右键容器 → Attach Shell（进入容器）

### 查看镜像和数据卷

- 展开 "Images" 可以看到所有镜像
- 展开 "Volumes" 可以看到所有数据卷
- 可以查看详情、删除等

### 编辑 Docker Compose 文件

- 打开 `docker-compose.yml`
- VS Code 会提供语法高亮和智能提示
- 可以右键文件 → "Compose Up" 或 "Compose Down"

## 快速命令参考

```bash
# 构建并启动
docker-compose up -d --build

# 查看状态
docker-compose ps

# 查看日志
docker-compose logs -f

# 停止服务
docker-compose down

# 重启服务
docker-compose restart

# 进入容器
docker-compose exec auth-service sh
```

## 注意事项

1. **首次构建**：需要 10-20 分钟（下载依赖）
2. **内存要求**：建议至少 8GB 可用内存
3. **端口占用**：确保端口 5432、6379、8080、8761、8091-8097 未被占用
4. **数据备份**：定期备份 PostgreSQL 数据卷

## 下一步

- 集成 Kubernetes 进行生产环境部署
- 配置 CI/CD 自动构建和部署
- 添加监控和日志聚合（Prometheus、Grafana、ELK）
