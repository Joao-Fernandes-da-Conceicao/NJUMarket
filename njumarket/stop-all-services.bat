@echo off
chcp 65001 >nul
echo ========================================
echo NJUMarket 微服务停止脚本
echo ========================================
echo.

echo 正在查找运行中的Spring Boot服务...
echo.

REM 查找所有包含spring-boot:run的Java进程
for /f "tokens=2" %%i in ('tasklist /FI "IMAGENAME eq java.exe" /FO LIST ^| findstr /I "PID"') do (
    set PID=%%i
    REM 检查进程命令行是否包含spring-boot:run
    wmic process where "ProcessId=%%i" get CommandLine 2>nul | findstr /I "spring-boot:run" >nul
    if !errorlevel! equ 0 (
        echo 找到服务进程: %%i
        set FOUND=1
    )
)

if not defined FOUND (
    echo 未找到运行中的服务
    pause
    exit /b 0
)

echo.
echo 提示：请手动关闭各个服务的命令行窗口来停止服务
echo 或者使用任务管理器结束java.exe进程
echo.
pause

