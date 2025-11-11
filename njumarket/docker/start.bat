@echo off
REM NJUMarket Docker 启动脚本 (Windows)

echo ==========================================
echo   NJUMarket Docker 启动脚本
echo ==========================================
echo.

REM 检查 Docker 是否运行
docker info >nul 2>&1
if errorlevel 1 (
    echo [错误] Docker 未运行，请先启动 Docker Desktop
    pause
    exit /b 1
)

echo [步骤 1/3] 构建 Docker 镜像...
echo 这可能需要 10-20 分钟，请耐心等待...
docker-compose build
if errorlevel 1 (
    echo [错误] 构建失败
    pause
    exit /b 1
)

echo.
echo [步骤 2/3] 启动所有服务...
docker-compose up -d
if errorlevel 1 (
    echo [错误] 启动失败
    pause
    exit /b 1
)

echo.
echo [步骤 3/3] 等待服务启动...
echo 正在检查服务健康状态...
timeout /t 15 /nobreak >nul

echo.
echo [完成] 启动完成！
echo.
echo 服务访问地址：
echo   - Eureka Dashboard: http://localhost:8761
echo   - API Gateway:      http://localhost:8080
echo   - Auth Service:      http://localhost:8091
echo.
echo 查看服务状态：
echo   docker-compose ps
echo.
echo 查看日志：
echo   docker-compose logs -f
echo.
echo 停止服务：
echo   docker-compose down
echo.
pause

