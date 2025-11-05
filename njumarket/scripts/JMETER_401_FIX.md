# 🔧 JMeter 401错误修复指南

## ❌ 问题现象

请求返回 `HTTP/1.1 401`，表示认证失败。

## 🔍 常见原因

### 1. CSV变量名配置错误

**问题**：CSV配置中 `variableNames` 只设置了 `token`，但CSV文件有4列。

**修复**：
- 在JMeter中打开 `CSV 数据文件设置`
- 将 **Variable Names** 改为：`username,phone,token,userId`
- 勾选 **Ignore first line**（忽略第一行表头）

### 2. 表头未忽略

**问题**：`ignoreFirstLine` 设置为 `false`，导致第一行被当作数据读取。

**修复**：
- 在 `CSV 数据文件设置` 中
- 勾选 **Ignore first line** = `true`

### 3. Token格式问题

**检查**：
1. 打开 `user_tokens.csv` 文件
2. 确认token列的值是完整的JWT token（以 `eyJ` 开头）
3. 确认没有多余的空格或换行符

### 4. Authorization Header格式

**检查**：
- 在 `HTTP信息头管理器` 中
- Authorization 的值应该是：`Bearer ${token}`
- 注意：`Bearer` 和 `${token}` 之间有一个空格

## ✅ 正确的CSV配置

```
Variable Names: username,phone,token,userId
Ignore first line: true ✓
Delimiter: ,
Encoding: UTF-8
```

## ✅ 正确的Header配置

```
Content-Type: application/json
Authorization: Bearer ${token}
```

## 🧪 验证步骤

1. **检查CSV文件格式**：
   ```csv
   username,phone,token,userId
   username_test_1,13800000001,eyJhbGci...,USER_xxx
   ```

2. **在JMeter中查看变量值**：
   - 添加 `Debug Sampler` 和 `View Results Tree`
   - 运行测试，查看 `${token}` 变量的实际值
   - 确认token不是 "token" 字符串，而是实际的JWT token

3. **检查后端日志**：
   - 查看是否有 "请求缺少Authorization头" 或 "Token验证失败" 的日志
   - 如果有，说明token没有正确传递

## 📝 快速修复步骤

1. **打开JMeter测试计划**
2. **找到 `CSV 数据文件设置`**
3. **修改配置**：
   - Variable Names: `username,phone,token,userId`
   - Ignore first line: `true` ✓
   - Encoding: `UTF-8`
4. **找到 `HTTP信息头管理器`**
5. **确认Authorization配置**：
   - Name: `Authorization`
   - Value: `Bearer ${token}` （注意空格）
6. **保存并重新运行测试**

---

**如果问题仍然存在**，请检查：
- CSV文件是否已正确生成（运行 `batch_create_users_simple.py`）
- Token是否过期（重新生成CSV文件）
- 后端服务是否正常运行

