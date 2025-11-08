#!/bin/bash

echo "========================================"
echo "清理Maven缓存和target目录"
echo "========================================"
echo ""

# 设置项目根目录
PROJECT_ROOT="$(cd "$(dirname "$0")" && pwd)"

echo "正在清理所有服务的target目录..."
echo ""

# 清理所有模块的target目录
find "$PROJECT_ROOT" -type d -name "target" -path "*/njumarket-*" -exec rm -rf {} + 2>/dev/null

echo ""
echo "✅ 清理完成！"
echo ""
echo "提示：现在可以重新运行 mvn compile 或启动脚本"
echo ""

