# 🔧 故障排查指南

## ❌ 常见错误及解决方案

### 1. `pip` 不是内部或外部命令

**错误信息**：
```
'pip' is not recognized as an internal or external command,
operable program or batch file.
```

**原因**：
- Python已安装，但pip不在系统PATH环境变量中
- 或者pip未正确安装

**解决方案**：

✅ **方法1：使用 `py -m pip`（推荐）**
```cmd
py -m pip install -r requirements.txt
```

✅ **方法2：检查Python安装**
```cmd
py --version
```
如果显示版本号，说明Python已安装

✅ **方法3：重新安装pip**
```cmd
py -m ensurepip --upgrade
```

✅ **方法4：将Python添加到PATH**
1. 找到Python安装目录（通常在 `C:\Users\你的用户名\AppData\Local\Programs\Python\`）
2. 将 `Python安装目录` 和 `Python安装目录\Scripts` 添加到系统PATH环境变量

---

### 2. `python` 不是内部或外部命令

**错误信息**：
```
'python' is not recognized as an internal or external command,
operable program or batch file.
```

**解决方案**：

✅ **使用 `py` 启动器（推荐）**
```cmd
py batch_create_users_simple.py
```

✅ **或者添加到PATH环境变量**
- 找到Python安装目录
- 添加到系统PATH

---

### 3. 脚本运行失败：连接错误

**错误信息**：
```
Connection refused
或
Connection timeout
```

**解决方案**：

1. **检查后端服务是否运行**：
   - 访问：http://localhost:8080
   - 或者查看后端控制台日志

2. **检查Redis服务**：
   ```cmd
   # 检查Redis是否运行
   # Windows: 查看服务管理器或任务管理器
   ```

3. **检查端口占用**：
   ```cmd
   netstat -ano | findstr :8080
   ```

---

### 4. 验证码获取失败

**错误信息**：
```
验证码获取失败
或
Redis连接失败
```

**解决方案**：

1. **检查Redis配置**：
   - 打开 `batch_create_users_simple.py`
   - 检查 `REDIS_HOST`、`REDIS_PORT`、`REDIS_PASSWORD` 配置
   - 确保与后端 `application.properties` 中的配置一致

2. **检查Redis服务**：
   - Redis服务是否正常运行
   - 端口是否正确

3. **查看后端日志**：
   - 验证码会打印在后端控制台
   - 检查是否有错误信息

---

### 5. JMeter测试时返回401错误

**错误信息**：
```
HTTP 401 Unauthorized
```

**解决方案**：

1. **检查token是否正确**：
   - 打开 `user_tokens.csv` 文件
   - 确认token列有值

2. **检查HTTP Header配置**：
   - 在JMeter中，HTTP Header Manager
   - 确认 `Authorization` 值为 `Bearer ${token}`（注意空格）

3. **检查token是否过期**：
   - Token可能已过期，重新运行脚本生成新token

---

### 6. 测试结果中订单数超过库存

**错误信息**：
- 订单数 > 库存数
- 库存变为负数

**解决方案**：

1. **检查分布式锁是否生效**：
   - 查看后端日志
   - 确认Redis连接正常
   - 确认分布式锁已实现

2. **检查数据库事务**：
   - 确认使用了 `@Transactional` 注解
   - 确认悲观锁已实现

3. **检查数据库连接**：
   - 确认数据库连接正常
   - 确认事务隔离级别正确

---

## 🔍 诊断步骤

### 步骤1：检查Python环境

```cmd
# 检查Python版本
py --version

# 检查pip是否可用
py -m pip --version

# 如果pip不可用，尝试安装
py -m ensurepip --upgrade
```

### 步骤2：检查依赖安装

```cmd
# 进入脚本目录
cd njumarket\scripts

# 安装依赖
py -m pip install -r requirements.txt

# 检查已安装的包
py -m pip list
```

### 步骤3：检查后端服务

```cmd
# 检查端口是否被占用
netstat -ano | findstr :8080

# 或者访问
curl http://localhost:8080
```

### 步骤4：检查Redis服务

```cmd
# Windows: 检查Redis服务
# 打开服务管理器，查找Redis服务
```

---

## 💡 快速修复命令

### 一键修复pip问题

```cmd
# 进入脚本目录
cd njumarket\scripts

# 使用py -m pip安装依赖
py -m pip install requests redis

# 运行脚本
py batch_create_users_simple.py
```

### 一键修复Python问题

```cmd
# 使用py启动器
py batch_create_users_simple.py
```

---

## 📞 获取帮助

如果以上方法都无法解决问题：

1. **查看详细错误信息**：
   - 复制完整的错误信息
   - 检查后端日志

2. **检查环境配置**：
   - Python版本
   - Redis配置
   - 数据库配置

3. **查看相关文档**：
   - [Windows命令参考](./WINDOWS_COMMANDS.md)
   - [批量用户创建指南](./README_BATCH_USERS.md)
   - [完整测试指南](./README_TESTING.md)

---

**文档版本**：v1.0  
**最后更新**：2025-01-XX
