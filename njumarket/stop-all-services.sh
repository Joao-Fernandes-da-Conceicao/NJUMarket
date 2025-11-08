#!/bin/bash

echo "========================================"
echo "NJUMarket 微服务停止脚本"
echo "========================================"
echo ""

# 查找所有运行中的Spring Boot服务
echo "正在查找运行中的服务..."
PIDS=$(ps aux | grep "spring-boot:run" | grep -v grep | awk '{print $2}')

if [ -z "$PIDS" ]; then
    echo "未找到运行中的服务"
    exit 0
fi

echo "找到以下进程："
ps aux | grep "spring-boot:run" | grep -v grep

echo ""
read -p "确认要停止所有服务吗？(y/N): " -n 1 -r
echo ""

if [[ $REPLY =~ ^[Yy]$ ]]; then
    echo "正在停止服务..."
    for PID in $PIDS; do
        echo "  停止进程 $PID..."
        kill $PID 2>/dev/null
    done
    
    sleep 2
    
    # 强制杀死仍在运行的进程
    REMAINING=$(ps aux | grep "spring-boot:run" | grep -v grep | awk '{print $2}')
    if [ ! -z "$REMAINING" ]; then
        echo "强制停止剩余进程..."
        for PID in $REMAINING; do
            kill -9 $PID 2>/dev/null
        done
    fi
    
    echo ""
    echo "✅ 所有服务已停止"
else
    echo "已取消"
fi

