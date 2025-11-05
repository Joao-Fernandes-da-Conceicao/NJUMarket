# ⚡ 5分钟快速开始测试

## 🎯 目标

快速验证库存超卖防护是否生效。

---

## 📋 前提条件

- ✅ 后端服务运行中（`http://localhost:8080`）
- ✅ 数据库运行中
- ✅ Redis运行中
- ✅ Python已安装

---

## 🚀 三步完成测试

### 第1步：创建测试用户（2分钟）

**Windows命令提示符（CMD）**：
```cmd
cd njumarket\scripts
py -m pip install -r requirements.txt
py batch_create_users_simple.py
```

**或者使用PowerShell**：
```powershell
cd njumarket\scripts
py -m pip install -r requirements.txt
py batch_create_users_simple.py
```

**注意**：
- 如果 `pip` 不可用，使用 `py -m pip` 代替 `pip`
- 如果 `python` 不可用，使用 `py` 代替 `python`

**输出**：`user_tokens.csv` 文件

### 第2步：准备测试商品（1分钟）

在数据库中执行：
```sql
-- 找一个测试用户作为卖家
SELECT user_id FROM users WHERE username LIKE 'username_test%' LIMIT 1;

-- 创建测试商品（假设seller_id是'USER_xxx'）
INSERT INTO commodities (
    commodity_id, seller_id, title, price, stock, commodity_status, publish_time
) VALUES (
    'test-001',
    'USER_xxx',  -- 替换为实际的user_id
    '测试商品',
    100.00,
    10,  -- 库存只有10
    'ON_SHELF',
    NOW()
);
```

### 第3步：运行JMeter测试（2分钟）

1. **打开JMeter**
2. **创建测试计划**：
   - Thread Group（100用户，5秒启动）
   - CSV Data Set Config（使用 `user_tokens.csv`）
   - HTTP Request（POST `/api/user/order/create`）
   - Aggregate Report（查看结果）

3. **运行测试**

---

## ✅ 验证结果

### 检查数据库

```sql
-- 检查库存（应该是0）
SELECT stock FROM commodities WHERE commodity_id = 'test-001';

-- 检查订单数（应该是10，不超过库存）
SELECT COUNT(*) FROM orders WHERE commodity_id = 'test-001';
```

### 查看JMeter报告

- **成功请求** = 10（等于库存）
- **库存不足响应** = 90
- **错误率** = 0%

---

## 📚 详细文档

- [完整测试指南](./README_TESTING.md) - 详细步骤
- [JMeter使用指南](./JMETER_GUIDE.md) - JMeter入门

---

**预计时间**：5分钟  
**难度**：⭐⭐（简单）
