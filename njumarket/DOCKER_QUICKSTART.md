# Docker 快速启动指南

## 🚀 一键启动（推荐）

### Windows

```bash
# 方式1: 使用批处理脚本
cd docker
start.bat

# 方式2: 使用 Docker Compose
cd njumarket
docker-compose up -d
```

### Linux/Mac

```bash
# 方式1: 使用 Shell 脚本
cd docker
chmod +x start.sh
./start.sh

# 方式2: 使用 Docker Compose
cd njumarket
docker-compose up -d
```

### 使用 Makefile（推荐）

```bash
# 构建并启动
make build
make up

# 查看日志
make logs

# 查看状态
make ps

# 停止服务
make down
```

## 📋 启动步骤

### 1. 构建镜像（首次运行）

```bash
docker-compose build
```

**预计时间**：10-20 分钟（首次构建）

### 2. 启动服务

```bash
docker-compose up -d
```

### 3. 查看服务状态

```bash
docker-compose ps
```

### 4. 查看日志

```bash
# 查看所有服务日志
docker-compose logs -f

# 查看特定服务日志
docker-compose logs -f auth-service
```

## ✅ 验证服务

启动成功后，访问以下地址验证：

- **Eureka Dashboard**: http://localhost:8761
- **API Gateway**: http://localhost:8080
- **健康检查**: http://localhost:8080/actuator/health

## 🛑 停止服务

```bash
docker-compose down
```

## 📚 更多信息

详细文档请参考：[docker/README.md](./docker/README.md) - 包含完整的使用指南、故障排查和开发模式说明。

