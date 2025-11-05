# 数据库连接问题排查指南

## 📋 错误信息

```
Communications link failure
Connection refused: no further information
Unable to open JDBC Connection for DDL execution
```

## 🔍 问题分析

这是一个典型的 **MySQL 连接失败**问题，可能的原因：

1. **MySQL 服务未启动**（最常见）
2. **数据库配置错误**（host、port、数据库名）
3. **用户名或密码错误**
4. **防火墙阻止连接**
5. **MySQL 端口被占用或被更改**

---

## ✅ 排查步骤

### 步骤1：检查 MySQL 服务是否运行

#### Windows

```cmd
# 方法1：检查服务状态
net start | findstr MySQL

# 方法2：使用服务管理器
# 按 Win+R，输入 services.msc，查找 MySQL 服务

# 方法3：检查进程
tasklist | findstr mysql
```

**如果服务未运行，启动 MySQL**：
```cmd
# 启动 MySQL 服务（需要管理员权限）
net start MySQL80
# 或者
net start MySQL
```

#### PowerShell

```powershell
# 检查服务状态
Get-Service | Where-Object {$_.Name -like "*mysql*"}

# 启动服务
Start-Service MySQL80
```

---

### 步骤2：验证数据库连接信息

**当前配置**（`application.properties`）：
```properties
spring.datasource.url=jdbc:mysql://localhost:3306/nju_market
spring.datasource.username=root
spring.datasource.password=Hqz20050316
```

**检查项**：
1. ✅ **主机地址**：`localhost`（本地）
2. ✅ **端口**：`3306`（MySQL 默认端口）
3. ✅ **数据库名**：`nju_market`
4. ✅ **用户名**：`root`
5. ✅ **密码**：`Hqz20050316`

---

### 步骤3：测试数据库连接

#### 使用命令行测试

```cmd
# 尝试连接 MySQL
mysql -u root -p

# 输入密码后，检查数据库是否存在
SHOW DATABASES;

# 如果 nju_market 不存在，创建它
CREATE DATABASE nju_market CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

#### 使用 MySQL Workbench 测试

1. 打开 MySQL Workbench
2. 创建新连接：
   - Host: `localhost`
   - Port: `3306`
   - Username: `root`
   - Password: `Hqz20050316`
3. 点击 "Test Connection" 测试连接

---

### 步骤4：检查 MySQL 端口

```cmd
# Windows 检查端口占用
netstat -ano | findstr :3306

# 如果端口被占用，查看占用进程
tasklist | findstr <PID>
```

**如果端口不是 3306**，需要修改配置文件：
```properties
spring.datasource.url=jdbc:mysql://localhost:实际端口/nju_market?...
```

---

### 步骤5：检查数据库是否存在

```sql
-- 连接 MySQL 后执行
SHOW DATABASES;

-- 如果 nju_market 不存在，创建它
CREATE DATABASE nju_market CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 使用数据库
USE nju_market;
```

---

## 🔧 解决方案

### 方案1：启动 MySQL 服务（推荐）

**Windows（CMD）**：
```cmd
# 以管理员身份运行 CMD
net start MySQL80
```

**Windows（PowerShell）**：
```powershell
# 以管理员身份运行 PowerShell
Start-Service MySQL80
```

**Windows（服务管理器）**：
1. 按 `Win + R`，输入 `services.msc`
2. 找到 `MySQL80` 或 `MySQL` 服务
3. 右键 → 启动

---

### 方案2：创建数据库（如果不存在）

```sql
-- 连接 MySQL
mysql -u root -p

-- 创建数据库
CREATE DATABASE nju_market CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 验证创建成功
SHOW DATABASES;
```

---

### 方案3：检查并修复配置文件

**检查 `application.properties`**：
```properties
# 确保配置正确
spring.datasource.url=jdbc:mysql://localhost:3306/nju_market?useSSL=false&useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai
spring.datasource.username=root
spring.datasource.password=Hqz20050316
```

**如果 MySQL 端口不是 3306**：
```properties
# 修改端口号
spring.datasource.url=jdbc:mysql://localhost:实际端口/nju_market?...
```

---

### 方案4：检查防火墙

**Windows 防火墙**：
1. 打开 "Windows Defender 防火墙"
2. 检查是否阻止了 MySQL 端口（3306）
3. 如果需要，添加例外规则

---

### 方案5：重置 MySQL 密码（如果密码错误）

```cmd
# 1. 停止 MySQL 服务
net stop MySQL80

# 2. 以跳过权限表方式启动 MySQL
mysqld --skip-grant-tables

# 3. 新开一个 CMD 窗口，连接 MySQL
mysql -u root

# 4. 重置密码
USE mysql;
UPDATE user SET authentication_string=PASSWORD('Hqz20050316') WHERE User='root';
FLUSH PRIVILEGES;
EXIT;

# 5. 重启 MySQL 服务
net start MySQL80
```

---

## 🚀 快速修复脚本

### Windows 批处理脚本

创建 `fix_mysql_connection.bat`：

```batch
@echo off
echo 检查 MySQL 服务状态...
net start | findstr MySQL

echo.
echo 尝试启动 MySQL 服务...
net start MySQL80

echo.
echo 等待 5 秒...
timeout /t 5

echo.
echo 测试数据库连接...
mysql -u root -pHqz20050316 -e "SHOW DATABASES;" 2>nul

if %errorlevel% equ 0 (
    echo ✅ MySQL 连接成功！
) else (
    echo ❌ MySQL 连接失败，请检查：
    echo   1. MySQL 服务是否运行
    echo   2. 用户名和密码是否正确
    echo   3. 数据库是否存在
)

pause
```

---

## 📝 验证清单

在启动应用前，确认以下事项：

- [ ] MySQL 服务正在运行
- [ ] 数据库 `nju_market` 已创建
- [ ] 用户名 `root` 和密码 `Hqz20050316` 正确
- [ ] 端口 `3306` 没有被占用或更改
- [ ] 可以通过命令行连接 MySQL
- [ ] `application.properties` 配置正确

---

## 🔍 常见问题

### Q1: MySQL 服务名称是什么？

**A**: 不同安装方式的服务名称可能不同：
- `MySQL80`（MySQL 8.0）
- `MySQL`（MySQL 5.7 或自定义名称）
- `MySQL57`（MySQL 5.7）

**查看所有服务**：
```cmd
net start
```

### Q2: 如何找到 MySQL 安装路径？

**A**: 
```cmd
# 查看 MySQL 进程
tasklist | findstr mysql

# 查看服务详细信息
sc query MySQL80
```

### Q3: 忘记了 MySQL 密码怎么办？

**A**: 参考 "方案5：重置 MySQL 密码"

### Q4: 端口 3306 被占用怎么办？

**A**: 
1. 找到占用端口的进程：`netstat -ano | findstr :3306`
2. 结束进程或更改 MySQL 端口
3. 修改 `application.properties` 中的端口号

---

## 📚 相关文档

- MySQL 官方文档：https://dev.mysql.com/doc/
- Spring Boot 数据库配置：https://spring.io/guides/gs/accessing-data-mysql/

---

**文档版本**：v1.0  
**最后更新**：2025-11-05  
**维护者**：NJUMarket 开发团队

