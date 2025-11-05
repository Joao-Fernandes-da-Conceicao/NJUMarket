# 📝 批量用户创建脚本使用说明

## 📋 这是什么？

**简单理解**：自动创建100个测试账号，并获取每个账号的登录token，用于后续的压力测试。

**为什么需要**：JMeter测试需要多个用户的token，手动创建太麻烦，用脚本自动创建。

## 🚀 5分钟快速开始

### 步骤1：安装Python依赖

**Windows命令提示符（CMD）**：
```cmd
cd njumarket\scripts
py -m pip install -r requirements.txt
```

**或者使用PowerShell**：
```powershell
cd njumarket\scripts
py -m pip install -r requirements.txt
```

**如果 `pip` 命令不可用**：
- 使用 `py -m pip` 代替 `pip`（推荐）
- 或者如果pip在PATH中，也可以直接使用 `pip install -r requirements.txt`

**如果安装失败，手动安装**：
```cmd
py -m pip install requests
```

**注意**：现在不再需要Redis库，因为注册无需验证码。

### 步骤2：运行脚本

**Windows命令提示符（CMD）**：
```cmd
python batch_create_users_simple.py
```

**或者使用PowerShell**：
```powershell
python batch_create_users_simple.py
```

**注意**：
- 如果提示 `python` 不是内部或外部命令，使用 `py`：
  ```cmd
  py batch_create_users_simple.py
  ```
- 如果提示 `pip` 不是内部或外部命令，使用 `py -m pip`：
  ```cmd
  py -m pip install -r requirements.txt
  ```

**就这么简单！** 脚本会自动：
- ✅ 创建100个测试用户（无需验证码）
- ✅ 获取每个用户的token
- ✅ 保存到 `user_tokens.csv` 文件

### 步骤3：查看结果

打开 `user_tokens.csv` 文件，你会看到：
```csv
username,phone,token,userId
username_test_1,13800000001,eyJhbGci...,USER_xxx
username_test_2,13800000002,eyJhbGci...,USER_xxx
...
```

## 📝 使用步骤

### 推荐：简化版脚本（batch_create_users_simple.py）

**优点**：
- ✅ 无需验证码，直接注册
- ✅ 如果用户已存在，自动登录获取token
- ✅ 简单快速，一键完成

**使用方法**：
```cmd
py batch_create_users_simple.py
```

**前提条件**：
- 后端服务正常运行

---

### 完整版脚本（batch_create_users.py）

### 模式1：批量创建新用户（推荐用于测试）

1. **运行脚本**，选择选项 `1`
2. **脚本会自动**：
   - 直接注册用户（无需验证码）
   - 获取token并保存
4. **结果保存**：
   - `user_tokens.csv` - CSV格式（JMeter可用）
   - `user_tokens.json` - JSON格式（方便查看）

### 模式2：批量登录已存在用户

如果用户已经存在，可以直接登录获取token：

1. **运行脚本**，选择选项 `2`
2. **脚本会自动**：
   - 使用生成的手机号列表登录
   - 获取token
3. **结果保存**：同上

### 模式3：只生成用户列表

只生成用户信息列表，不创建账号：

1. **运行脚本**，选择选项 `3`
2. **生成文件**：`user_list.json`

## 📊 生成的用户信息

| 字段 | 格式 | 示例 |
|------|------|------|
| **用户名** | `username_test_{序号}` | `username_test_1`, `username_test_2`, ... |
| **手机号** | `1380000{序号}` | `13800000001`, `13800000002`, ... |
| **密码** | 统一密码 | `123456` |
| **昵称** | `测试用户{序号}` | `测试用户1`, `测试用户2`, ... |

## 📁 输出文件说明

### CSV格式（user_tokens.csv）- 用于JMeter

**文件内容**：
```csv
username,phone,token,userId
username_test_1,13800000001,eyJhbGci...,USER_xxx
username_test_2,13800000002,eyJhbGci...,USER_xxx
```

**在JMeter中使用**：
1. 添加 `CSV Data Set Config`
2. 配置：
   - Filename: `user_tokens.csv`
   - Variable Names: `username,phone,token,userId`
3. 在HTTP请求中使用：
   - Header: `Authorization: Bearer ${token}`
   - Body: 可以使用 `${username}`, `${phone}` 等变量

### JSON格式（user_tokens.json）- 方便查看

**文件内容**：
```json
[
  {
    "username": "username_test_1",
    "phone": "13800000001",
    "token": "eyJhbGci...",
    "userId": "USER_xxx"
  }
]
```

**用途**：方便查看和调试，不适合JMeter使用

## ⚠️ 常见问题

### Q1: 用户创建失败？

**A**: 可能原因：
- 用户已存在（脚本会自动登录获取token）
- 手机号已注册（脚本会自动登录获取token）
- 后端服务未运行（检查后端是否正常启动）
- 网络连接问题（检查BASE_URL配置）

### Q2: 如何修改用户数量？

**A**: 修改脚本中的配置：
```python
USER_COUNT = 100  # 改为你需要的数量
```

### Q3: 如何修改密码？

**A**: 修改脚本中的配置：
```python
PASSWORD = "123456"  # 改为你想要的密码
```

## 🔧 自定义配置

如果需要修改配置，编辑脚本文件：

```python
# 在 batch_create_users_simple.py 中修改
BASE_URL = "http://localhost:8080"  # 后端地址
USER_COUNT = 100  # 用户数量（改为你需要的数量）
PASSWORD = "123456"  # 统一密码
USERNAME_PREFIX = "username_test"  # 用户名前缀
```

## 📝 后续使用

### 在JMeter中使用

1. **添加CSV数据配置**：
   - 右键 Thread Group → Add → Config Element → CSV Data Set Config
   - Filename: `user_tokens.csv`
   - Variable Names: `username,phone,token,userId`

2. **配置HTTP Header**：
   - 右键 HTTP Request → Add → Config Element → HTTP Header Manager
   - 添加：`Authorization: Bearer ${token}`

3. **在请求中使用变量**：
   - 可以使用 `${username}`, `${phone}`, `${token}` 等变量

**详细步骤**：参考 [JMETER_GUIDE.md](./JMETER_GUIDE.md)

## 🐛 常见问题补充

### Q: 用户创建失败？
A: 可能原因：
- 用户名已存在（尝试修改用户名前缀）
- 手机号已注册（脚本会自动登录获取token）
- 后端服务异常（检查后端日志）
- 网络连接问题（检查BASE_URL配置）

### Q: 如何修改用户名前缀？
A: 修改脚本中的配置：
```python
USERNAME_PREFIX = "username_test"  # 改为你想要的前缀
```

## 📚 相关文档

- [完整测试指南](./README_TESTING.md) - 从创建用户到运行测试的完整流程
- [JMeter使用指南](./JMETER_GUIDE.md) - JMeter快速入门
- [库存超卖解决方案](./STOCK_OVERSALE_SOLUTION.md) - 解决方案说明

---

**适合人群**：初学者开发者  
**预计时间**：10分钟完成
