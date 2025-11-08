@echo off
chcp 65001 >nul
echo ========================================
echo NJUMarket 微服务一键启动脚本
echo ========================================
echo.

REM 设置项目根目录（脚本在njumarket目录下，所以直接使用当前目录）
set PROJECT_ROOT=%~dp0

REM 检查Maven是否安装
where mvn >nul 2>&1
if %ERRORLEVEL% NEQ 0 (
    echo [错误] 未找到Maven，请先安装Maven并添加到PATH
    pause
    exit /b 1
)

REM 检查Java是否安装
where java >nul 2>&1
if %ERRORLEVEL% NEQ 0 (
    echo [错误] 未找到Java，请先安装JDK 17+并添加到PATH
    pause
    exit /b 1
)

REM 清理Maven缓存（避免缓存文件损坏问题）
echo [0/8] 清理Maven缓存...
cd /d "%PROJECT_ROOT%"
mvn clean -q >nul 2>&1
if %ERRORLEVEL% EQU 0 (
    echo   ✓ 清理完成
) else (
    echo   ⚠️ 清理警告（继续启动）
)
echo.

echo [1/8] 启动 Eureka Discovery Server (端口: 8761)...
start "Eureka Discovery" cmd /k "cd /d %PROJECT_ROOT%\njumarket-discovery && mvn spring-boot:run"
timeout /t 10 /nobreak >nul

echo [2/8] 启动 API Gateway (端口: 8080)...
start "API Gateway" cmd /k "cd /d %PROJECT_ROOT%\njumarket-gateway && mvn spring-boot:run"
timeout /t 10 /nobreak >nul

echo [3/8] 启动 Auth Service (端口: 8091)...
start "Auth Service" cmd /k "cd /d %PROJECT_ROOT%\njumarket-service-auth && mvn spring-boot:run"
timeout /t 5 /nobreak >nul

echo [4/8] 启动 Commodity Service (端口: 8092)...
start "Commodity Service" cmd /k "cd /d %PROJECT_ROOT%\njumarket-service-commodity && mvn spring-boot:run"
timeout /t 5 /nobreak >nul

echo [5/8] 启动 Order Service (端口: 8093)...
start "Order Service" cmd /k "cd /d %PROJECT_ROOT%\njumarket-service-order && mvn spring-boot:run"
timeout /t 5 /nobreak >nul

echo [6/8] 启动 Message Service (端口: 8094)...
start "Message Service" cmd /k "cd /d %PROJECT_ROOT%\njumarket-service-message && mvn spring-boot:run"
timeout /t 5 /nobreak >nul

echo [7/8] 启动 Image Service (端口: 8095)...
start "Image Service" cmd /k "cd /d %PROJECT_ROOT%\njumarket-service-image && mvn spring-boot:run"
timeout /t 5 /nobreak >nul

echo [8/8] 启动 Admin Service (端口: 8096)...
start "Admin Service" cmd /k "cd /d %PROJECT_ROOT%\njumarket-service-admin && mvn spring-boot:run"
timeout /t 5 /nobreak >nul

echo.
echo ========================================
echo ✅ 所有服务已启动！
echo ========================================
echo.
echo 📋 服务访问地址：
echo   - Eureka控制台:    http://localhost:8761
echo   - API Gateway:     http://localhost:8080
echo   - Auth Service:    http://localhost:8091
echo   - Commodity Service: http://localhost:8092
echo   - Order Service:   http://localhost:8093
echo   - Message Service: http://localhost:8094
echo   - Image Service:   http://localhost:8095
echo   - Admin Service:   http://localhost:8096
echo.
echo 💡 提示：
echo   - 每个服务都在独立的窗口中运行
echo   - 关闭窗口即可停止对应服务
echo   - 查看服务状态：访问 http://localhost:8761 查看Eureka控制台
echo   - 单独启动服务：cd njumarket-xxx ^&^& mvn spring-boot:run
echo.
pause
