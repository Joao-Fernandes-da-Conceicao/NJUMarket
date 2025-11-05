# 🧪 测试脚本和文档总览

## 📁 本文件夹包含什么？

这个文件夹包含所有**测试相关的脚本和文档**，用于：
- ✅ 批量创建测试用户
- ✅ JMeter压力测试
- ✅ 验证库存超卖防护

---

## 📚 文档导航

### ⚡ 快速开始（5分钟）

**[QUICK_START.md](./QUICK_START.md)** 🚀 **最快上手**
- 3步完成测试
- 适合快速验证

### 💻 Windows用户必读

**[WINDOWS_COMMANDS.md](./WINDOWS_COMMANDS.md)** 📖 **Windows命令参考**
- Windows常用命令
- 命令对比说明
- 使用技巧

**[TROUBLESHOOTING.md](./TROUBLESHOOTING.md)** 🔧 **故障排查指南**
- 常见错误及解决方案
- pip/python命令不可用
- 连接错误、验证码失败等

### 🎯 新手必读（按顺序阅读）

1. **[README_TESTING.md](./README_TESTING.md)** ⭐ **完整指南**
   - 从创建用户到运行测试的完整流程
   - 详细步骤说明
   - 适合初学者

2. **[README_BATCH_USERS.md](./README_BATCH_USERS.md)**
   - 如何批量创建测试用户
   - 脚本使用方法

3. **[JMETER_GUIDE.md](./JMETER_GUIDE.md)**
   - JMeter快速入门
   - 如何创建测试计划
   - 如何查看测试结果

4. **[STOCK_OVERSALE_SOLUTION.md](./STOCK_OVERSALE_SOLUTION.md)**
   - 库存超卖问题的解决方案
   - 为什么需要三层保护机制

### 📖 进阶文档

- **[JMETER_STOCK_OVERSALE_TEST.md](./JMETER_STOCK_OVERSALE_TEST.md)** - JMeter详细技术文档

---

## 🛠️ 脚本文件

### 用户创建脚本

- **`batch_create_users_simple.py`** ⭐ **推荐使用**
  - 自动创建100个测试用户
  - 自动获取token
  - 最简单易用

- **`batch_create_users.py`**
  - 完整版脚本
  - 支持多种模式
  - 需要手动输入验证码

### 配置文件

- **`requirements.txt`**
  - Python依赖包列表
  - 运行 `pip install -r requirements.txt` 安装

- **`user_tokens_template.csv`**
  - CSV文件模板
  - 参考格式

---

## 🚀 快速开始（3步）

### 第1步：创建测试用户

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
- 如果 `pip` 命令不可用，使用 `py -m pip` 代替 `pip`
- 如果 `python` 命令不可用，使用 `py` 代替 `python`

**输出**：`user_tokens.csv` 文件（包含100个用户的token）

### 第2步：准备测试商品

在数据库中执行：
```sql
-- 创建测试商品（库存=10）
INSERT INTO commodities (
    commodity_id, seller_id, title, price, stock, commodity_status
) VALUES (
    'test-commodity-001',
    'USER_xxx',  -- 替换为实际的seller_id
    '测试商品',
    100.00,
    10,
    'ON_SHELF'
);
```

### 第3步：运行JMeter测试

1. 打开JMeter
2. 创建测试计划（参考 [JMETER_GUIDE.md](./JMETER_GUIDE.md)）
3. 使用 `user_tokens.csv` 作为CSV数据源
4. 运行测试

**详细步骤**：查看 [README_TESTING.md](./README_TESTING.md) 或 [QUICK_START.md](./QUICK_START.md)

---

## 📊 测试流程概览

```
┌─────────────────┐
│ 1. 创建测试用户 │  ← 使用 batch_create_users_simple.py
└────────┬────────┘
         │
         ↓
┌─────────────────┐
│ 2. 准备测试商品 │  ← 在数据库中创建
└────────┬────────┘
         │
         ↓
┌─────────────────┐
│ 3. 配置JMeter   │  ← 参考 JMETER_GUIDE.md
└────────┬────────┘
         │
         ↓
┌─────────────────┐
│ 4. 运行测试     │  ← 100个并发用户
└────────┬────────┘
         │
         ↓
┌─────────────────┐
│ 5. 验证结果     │  ← 检查库存和订单数
└─────────────────┘
```

---

## 🎓 学习路径

### 学习路径推荐

#### 路径1：快速验证（5分钟）
1. 阅读 [QUICK_START.md](./QUICK_START.md)
2. 跟着步骤操作
3. 验证结果

#### 路径2：完整学习（30分钟）
1. **第一步**：阅读 [README_TESTING.md](./README_TESTING.md)
   - 了解完整的测试流程
   - 跟着步骤操作

2. **第二步**：运行脚本创建用户
   - 参考 [README_BATCH_USERS.md](./README_BATCH_USERS.md)

3. **第三步**：学习JMeter
   - 参考 [JMETER_GUIDE.md](./JMETER_GUIDE.md)

4. **第四步**：理解解决方案
   - 参考 [STOCK_OVERSALE_SOLUTION.md](./STOCK_OVERSALE_SOLUTION.md)

---

## ❓ 常见问题

### Q: 我应该从哪里开始？

**A**: 
- **想快速验证**：看 [QUICK_START.md](./QUICK_START.md)
- **想完整学习**：看 [README_TESTING.md](./README_TESTING.md)

### Q: 脚本运行失败怎么办？

**A**: 
1. 检查后端服务是否运行（`http://localhost:8080`）
2. 检查Redis是否运行
3. 查看脚本输出的错误信息

### Q: JMeter不会用怎么办？

**A**: 查看 [JMETER_GUIDE.md](./JMETER_GUIDE.md)，里面有详细的图文说明。

### Q: 测试结果怎么看？

**A**: 
1. 查看数据库中的库存和订单数
2. 查看JMeter的聚合报告
3. 参考 [README_TESTING.md](./README_TESTING.md) 中的验证步骤

---

## 📝 文件清单

### 📚 文档（7个）
1. **README.md** - 本文件（总览和导航）
2. **QUICK_START.md** - 5分钟快速开始 ⚡
3. **README_TESTING.md** - 完整测试指南 ⭐
4. **README_BATCH_USERS.md** - 用户创建脚本说明
5. **JMETER_GUIDE.md** - JMeter快速入门
6. **STOCK_OVERSALE_SOLUTION.md** - 库存超卖解决方案
7. **JMETER_STOCK_OVERSALE_TEST.md** - JMeter详细技术文档

### 🛠️ 脚本（2个）
1. **batch_create_users_simple.py** - 简化版用户创建脚本 ⭐推荐
2. **batch_create_users.py** - 完整版用户创建脚本

### 📋 配置文件（2个）
1. **requirements.txt** - Python依赖包
2. **user_tokens_template.csv** - CSV文件模板

---

**适合人群**：初学者开发者  
**最后更新**：2025-01-XX