#!/bin/bash

echo "========================================"
echo "NJUMarket 微服务一键启动脚本"
echo "========================================"
echo ""

# 设置项目根目录（脚本在njumarket目录下，所以直接使用当前目录）
PROJECT_ROOT="$(cd "$(dirname "$0")" && pwd)"

# 检查Maven是否安装
if ! command -v mvn &> /dev/null; then
    echo "[错误] 未找到Maven，请先安装Maven"
    exit 1
fi

# 检查Java是否安装
if ! command -v java &> /dev/null; then
    echo "[错误] 未找到Java，请先安装JDK 17+"
    exit 1
fi

# 清理Maven缓存（避免缓存文件损坏问题）
echo "[0/8] 清理Maven缓存..."
cd "$PROJECT_ROOT" || exit 1
mvn clean -q > /dev/null 2>&1
if [ $? -eq 0 ]; then
    echo "  ✓ 清理完成"
else
    echo "  ⚠️ 清理警告（继续启动）"
fi
echo ""

echo "[1/8] 启动 Eureka Discovery Server (端口: 8761)..."
cd "$PROJECT_ROOT/njumarket-discovery" || exit 1
mvn spring-boot:run > /dev/null 2>&1 &
EUREKA_PID=$!
echo "  ✓ Eureka Discovery Server 已启动 (PID: $EUREKA_PID)"
sleep 10

echo "[2/8] 启动 API Gateway (端口: 8080)..."
cd "$PROJECT_ROOT/njumarket-gateway" || exit 1
mvn spring-boot:run > /dev/null 2>&1 &
GATEWAY_PID=$!
echo "  ✓ API Gateway 已启动 (PID: $GATEWAY_PID)"
sleep 10

echo "[3/8] 启动 Auth Service (端口: 8091)..."
cd "$PROJECT_ROOT/njumarket-service-auth" || exit 1
mvn spring-boot:run > /dev/null 2>&1 &
AUTH_PID=$!
echo "  ✓ Auth Service 已启动 (PID: $AUTH_PID)"
sleep 5

echo "[4/8] 启动 Commodity Service (端口: 8092)..."
cd "$PROJECT_ROOT/njumarket-service-commodity" || exit 1
mvn spring-boot:run > /dev/null 2>&1 &
COMMODITY_PID=$!
echo "  ✓ Commodity Service 已启动 (PID: $COMMODITY_PID)"
sleep 5

echo "[5/8] 启动 Order Service (端口: 8093)..."
cd "$PROJECT_ROOT/njumarket-service-order" || exit 1
mvn spring-boot:run > /dev/null 2>&1 &
ORDER_PID=$!
echo "  ✓ Order Service 已启动 (PID: $ORDER_PID)"
sleep 5

echo "[6/8] 启动 Message Service (端口: 8094)..."
cd "$PROJECT_ROOT/njumarket-service-message" || exit 1
mvn spring-boot:run > /dev/null 2>&1 &
MESSAGE_PID=$!
echo "  ✓ Message Service 已启动 (PID: $MESSAGE_PID)"
sleep 5

echo "[7/8] 启动 Image Service (端口: 8095)..."
cd "$PROJECT_ROOT/njumarket-service-image" || exit 1
mvn spring-boot:run > /dev/null 2>&1 &
IMAGE_PID=$!
echo "  ✓ Image Service 已启动 (PID: $IMAGE_PID)"
sleep 5

echo "[8/8] 启动 Admin Service (端口: 8096)..."
cd "$PROJECT_ROOT/njumarket-service-admin" || exit 1
mvn spring-boot:run > /dev/null 2>&1 &
ADMIN_PID=$!
echo "  ✓ Admin Service 已启动 (PID: $ADMIN_PID)"
sleep 5

echo ""
echo "========================================"
echo "✅ 所有服务已启动！"
echo "========================================"
echo ""
echo "📋 服务访问地址："
echo "  - Eureka控制台:    http://localhost:8761"
echo "  - API Gateway:     http://localhost:8080"
echo "  - Auth Service:    http://localhost:8091"
echo "  - Commodity Service: http://localhost:8092"
echo "  - Order Service:   http://localhost:8093"
echo "  - Message Service: http://localhost:8094"
echo "  - Image Service:   http://localhost:8095"
echo "  - Admin Service:   http://localhost:8096"
echo ""
echo "🔍 进程ID："
echo "  - Eureka:    $EUREKA_PID"
echo "  - Gateway:   $GATEWAY_PID"
echo "  - Auth:      $AUTH_PID"
echo "  - Commodity:  $COMMODITY_PID"
echo "  - Order:     $ORDER_PID"
echo "  - Message:  $MESSAGE_PID"
echo "  - Image:     $IMAGE_PID"
echo "  - Admin:     $ADMIN_PID"
echo ""
echo "🛑 停止所有服务："
echo "  kill $EUREKA_PID $GATEWAY_PID $AUTH_PID $COMMODITY_PID $ORDER_PID $MESSAGE_PID $IMAGE_PID $ADMIN_PID"
echo ""
echo "💡 提示："
echo "  - 查看服务日志：每个服务都在后台运行，日志输出到 /dev/null"
echo "  - 单独启动服务：cd njumarket-xxx && mvn spring-boot:run"
echo "  - 检查服务状态：访问 http://localhost:8761 查看Eureka控制台"
echo ""
