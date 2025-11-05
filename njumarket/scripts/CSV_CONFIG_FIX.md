# 🔧 JMeter CSV配置问题修复

## ❌ 问题现象

- Token未过期但返回401错误
- Authorization header可能读取到的是 "token" 字符串而不是实际的JWT token

## 🔍 根本原因

### 错误的配置：
```
ignoreFirstLine = false  ❌
variableNames = token    ❌
```

### 问题分析：

1. **CSV文件格式**：
   ```csv
   username,phone,token,userId          ← 第一行（表头）
   username_test_1,13800000001,eyJ...,USER_xxx
   ```

2. **当 `ignoreFirstLine=false` 时**：
   - JMeter会读取第一行作为数据
   - 第一行是：`username,phone,token,userId`
   - 如果 `variableNames=token`，token变量会读取到第3列的值
   - **结果**：`${token}` = `"token"`（字符串）而不是实际的JWT token！

3. **Authorization Header变成**：
   - `Bearer ${token}` = `Bearer token`（字面意思）
   - 而不是 `Bearer eyJhbGci...`（实际的JWT token）

## ✅ 正确的配置

### 必须设置的参数：

```
Variable Names: username,phone,token,userId  ✓
Ignore first line: true                       ✓
Encoding: UTF-8                                ✓
Delimiter: ,                                   ✓
```

### 为什么需要匹配所有列？

即使你只用 `token` 变量，也必须设置 `variableNames=username,phone,token,userId` 来匹配CSV的所有列。否则：
- JMeter会按列位置读取，而不是按列名
- 如果只设置 `token`，它会读取第1列的值（username）！

## 🔧 修复步骤

### 方法1：在JMeter GUI中修复

1. **打开JMeter测试计划**
2. **找到 `CSV 数据文件设置`**
3. **修改以下配置**：
   - **Variable Names**: `username,phone,token,userId`
   - **Ignore first line**: 勾选 ✓
   - **Encoding**: `UTF-8`
4. **保存测试计划**

### 方法2：直接修改JMX文件

已修复 `线程组.jmx` 文件，配置如下：

```xml
<stringProp name="variableNames">username,phone,token,userId</stringProp>
<boolProp name="ignoreFirstLine">true</boolProp>
<stringProp name="fileEncoding">UTF-8</stringProp>
```

## 🧪 验证方法

### 1. 使用Debug Sampler验证变量值

1. 在JMeter中，右键 `Thread Group` → `Add` → `Sampler` → `Debug Sampler`
2. 运行测试
3. 在 `View Results Tree` 中查看 `${token}` 的值
4. **应该看到**：完整的JWT token（以 `eyJ` 开头）
5. **不应该看到**：`"token"` 字符串

### 2. 查看后端日志

后端会记录详细的401错误原因：
- `请求缺少Authorization头` - 说明header没有正确传递
- `Token验证失败` - 说明token格式错误或已过期
- `Token中无法获取用户ID` - 说明token解析失败

### 3. 检查请求头

在JMeter的 `View Results Tree` 中：
1. 选择失败的请求
2. 查看 `Request` → `Headers` 标签
3. 确认 `Authorization: Bearer eyJhbGci...`（实际token）

## 📝 常见错误对比

| 配置 | 结果 | 说明 |
|------|------|------|
| `ignoreFirstLine=false`<br>`variableNames=token` | ❌ `${token}` = `"token"` | 读取到表头字符串 |
| `ignoreFirstLine=true`<br>`variableNames=token` | ❌ `${token}` = `username_test_1` | 只读取第1列 |
| `ignoreFirstLine=true`<br>`variableNames=username,phone,token,userId` | ✅ `${token}` = `eyJhbGci...` | 正确读取 |

## 🎯 总结

**关键点**：
1. ✅ **必须**设置 `ignoreFirstLine=true` 忽略表头
2. ✅ **必须**设置 `variableNames` 匹配CSV的所有列
3. ✅ **即使只用一个变量**，也要匹配所有列

**修复后**：
- `${token}` 会正确读取到实际的JWT token
- Authorization header会是：`Bearer eyJhbGci...`
- 401错误应该消失

---

**文档版本**：v1.0  
**最后更新**：2025-11-05

