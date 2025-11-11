#!/bin/bash

# NJUMarket Docker 启动脚本

set -e

echo "=========================================="
echo "  NJUMarket Docker 启动脚本"
echo "=========================================="
echo ""

# 检查 Docker 是否运行
if ! docker info > /dev/null 2>&1; then
    echo "❌ 错误: Docker 未运行，请先启动 Docker"
    exit 1
fi

# 检查 Docker Compose 是否安装
if ! command -v docker-compose &> /dev/null; then
    echo "❌ 错误: Docker Compose 未安装"
    exit 1
fi

echo "📦 步骤 1/3: 构建 Docker 镜像..."
echo "   这可能需要 10-20 分钟，请耐心等待..."
docker-compose build

echo ""
echo "🚀 步骤 2/3: 启动所有服务..."
docker-compose up -d

echo ""
echo "⏳ 步骤 3/3: 等待服务启动..."
echo "   正在检查服务健康状态..."

# 等待 MySQL 启动
echo "   等待 MySQL 启动..."
timeout=60
counter=0
while ! docker-compose exec -T mysql mysqladmin ping -h localhost -u root -pHqz20050316 --silent > /dev/null 2>&1; do
    sleep 2
    counter=$((counter + 2))
    if [ $counter -ge $timeout ]; then
        echo "   ⚠️  MySQL 启动超时"
        break
    fi
done

# 等待 Redis 启动
echo "   等待 Redis 启动..."
timeout=30
counter=0
while ! docker-compose exec -T redis redis-cli -a hqz20050316 ping > /dev/null 2>&1; do
    sleep 2
    counter=$((counter + 2))
    if [ $counter -ge $timeout ]; then
        echo "   ⚠️  Redis 启动超时"
        break
    fi
done

# 等待 Eureka 启动
echo "   等待 Eureka 启动..."
sleep 10

echo ""
echo "✅ 启动完成！"
echo ""
echo "📋 服务访问地址："
echo "   - Eureka Dashboard: http://localhost:8761"
echo "   - API Gateway:      http://localhost:8080"
echo "   - Auth Service:      http://localhost:8091"
echo ""
echo "📊 查看服务状态："
echo "   docker-compose ps"
echo ""
echo "📝 查看日志："
echo "   docker-compose logs -f"
echo ""
echo "🛑 停止服务："
echo "   docker-compose down"
echo ""

