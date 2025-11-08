@echo off
chcp 65001 >nul
echo ========================================
echo 清理Maven缓存和target目录
echo ========================================
echo.

REM 设置项目根目录（脚本在njumarket目录下）
set PROJECT_ROOT=%~dp0

echo 正在使用Maven清理所有服务的target目录...
echo.

REM 切换到项目根目录
cd /d "%PROJECT_ROOT%"

REM 使用Maven clean命令清理（更可靠）
mvn clean -q

if %ERRORLEVEL% EQU 0 (
    echo.
    echo ✅ 清理完成！
    echo.
    echo 提示：现在可以重新运行 mvn compile 或启动脚本
) else (
    echo.
    echo ⚠️ 清理过程中出现警告，但可能已部分清理
    echo 提示：可以手动删除各模块的target目录
)

echo.
pause

