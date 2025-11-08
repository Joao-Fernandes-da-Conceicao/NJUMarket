# NJUMarket 测试脚本使用指南

## 📋 目录说明

本目录包含用于测试和演示的脚本文件，适用于**微服务架构版本**。

## 📁 文件清单

### 核心脚本

- **`batch_create_users_simple.py`** - 批量创建测试用户脚本
  - 自动创建100个测试用户
  - 自动获取Token并保存为CSV和JSON格式
  - 适用于JMeter压力测试

- **`线程组.jmx`** - JMeter测试计划文件
  - 包含压力测试配置
  - 需要配合 `user_tokens.csv` 使用

### 配置文件

- **`requirements.txt`** - Python依赖包列表
  - 仅需 `requests` 库

### 生成文件（运行脚本后自动生成）

运行 `batch_create_users_simple.py` 后会自动生成：
- **`user_tokens.csv`** - 用户Token列表（CSV格式，用于JMeter）
- **`user_tokens.json`** - 用户Token列表（JSON格式，用于调试）

**注意**：这些文件会在运行脚本时自动生成，无需手动创建。

## 🚀 快速开始

### 前置条件

1. **启动所有微服务**
   ```bash
   # Windows
   start-all-services.bat
   
   # Linux/Mac
   ./start-all-services.sh
   ```

2. **等待服务启动完成**
   - 访问 http://localhost:8761 查看 Eureka 控制台
   - 确保所有服务都已注册成功
   - 访问 http://localhost:8080 确认 Gateway 正常运行

3. **安装Python依赖**
   ```bash
   pip install -r requirements.txt
   ```

### 步骤1：创建测试用户

**Windows:**
```cmd
cd scripts
python batch_create_users_simple.py
```

**Linux/Mac:**
```bash
cd scripts
python3 batch_create_users_simple.py
```

**输出：**
- 自动生成 `user_tokens.csv`（用于JMeter）
- 自动生成 `user_tokens.json`（用于调试）

### 步骤2：运行JMeter测试

1. 打开JMeter
2. 打开 `线程组.jmx` 文件
3. 配置CSV数据源：
   - 使用 `user_tokens.csv` 作为数据源
   - 配置变量名：`username`, `phone`, `token`, `userId`
4. 运行测试计划

## 📝 脚本配置说明

### batch_create_users_simple.py

**配置项：**
```python
BASE_URL = "http://localhost:8080"      # API Gateway 地址
API_PREFIX = "/api/user/auth"           # 认证服务路径
USER_COUNT = 100                        # 创建用户数量
PASSWORD = "123456"                     # 默认密码
USERNAME_PREFIX = "username_test"       # 用户名前缀
```

**生成规则：**
- 手机号：`13800000001` 到 `13800000100`
- 用户名：`username_test_1` 到 `username_test_100`
- 密码：统一为 `123456`

**输出格式：**

**CSV格式（user_tokens.csv）：**
```csv
username,phone,token,userId
username_test_1,13800000001,eyJhbGciOi...,user_001
username_test_2,13800000002,eyJhbGciOi...,user_002
...
```

**JSON格式（user_tokens.json）：**
```json
[
  {
    "username": "username_test_1",
    "phone": "13800000001",
    "token": "eyJhbGciOi...",
    "refreshToken": "eyJhbGciOi...",
    "userId": "user_001"
  },
  ...
]
```

## 🔧 故障排查

### 问题1：连接失败

**错误信息：** `Connection refused` 或 `无法连接到服务器`

**解决方案：**
1. 确认所有微服务已启动
2. 检查 Gateway 是否运行在 `http://localhost:8080`
3. 检查防火墙设置

### 问题2：用户已存在

**错误信息：** `该手机号已注册` 或 `用户已存在`

**解决方案：**
- 脚本会自动尝试登录已存在的用户
- 如果登录失败，需要手动清理数据库或使用不同的手机号范围

### 问题3：Python命令不可用

**Windows:**
```cmd
# 使用 py 命令代替 python
py batch_create_users_simple.py

# 或使用 python3
python3 batch_create_users_simple.py
```

**Linux/Mac:**
```bash
# 使用 python3
python3 batch_create_users_simple.py
```

### 问题4：依赖包安装失败

**解决方案：**
```bash
# 使用 pip3
pip3 install -r requirements.txt

# 或使用 py -m pip (Windows)
py -m pip install -r requirements.txt
```

## 📊 JMeter测试说明

### 配置CSV数据源

1. 添加 **CSV Data Set Config**
2. 配置：
   - Filename: `user_tokens.csv`
   - Variable Names: `username,phone,token,userId`
   - Delimiter: `,`
   - Recycle on EOF: `True`
   - Stop thread on EOF: `False`

### 使用Token

在HTTP请求头中添加：
```
Authorization: Bearer ${token}
```

### 测试场景

- **商品浏览**：`GET /api/public/commodity/search`
- **下单测试**：`POST /api/user/order/create`
- **订单查询**：`GET /api/user/order/buyer`

## 🏗️ 微服务架构说明

本脚本通过 **API Gateway** 访问后端服务：

```
Python脚本 → API Gateway (8080) → Auth Service (8091)
```

**API路径：**
- 注册：`POST http://localhost:8080/api/user/auth/register-new`
- 登录：`POST http://localhost:8080/api/user/auth/login`

**注意：**
- 所有请求必须通过 Gateway（端口8080）
- 不要直接访问各个微服务的端口
- Gateway 负责路由和认证

## 📚 相关文档

- 项目文档：`../docs/PROJECT_DOCUMENTATION_V2.0.md`
- 微服务配置：`../docs/MICROSERVICES_SETUP_GUIDE.md`
- 数据库初始化：`../database/README.md`

## ⚠️ 注意事项

1. **测试环境专用**：本脚本仅用于测试环境，不要在生产环境使用
2. **密码安全**：生成的用户密码统一为 `123456`，仅用于测试
3. **数据清理**：测试完成后建议清理测试用户数据
4. **服务依赖**：确保所有微服务正常运行后再执行脚本

## 🎯 适用场景

- ✅ 软件工程课程演示
- ✅ 压力测试准备
- ✅ 功能测试数据准备
- ✅ 开发环境快速搭建
