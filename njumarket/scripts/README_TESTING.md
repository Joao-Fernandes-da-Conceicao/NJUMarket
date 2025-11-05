# 🧪 库存超卖测试完整指南

## 📋 目录

1. [测试概述](#测试概述)
2. [准备工作](#准备工作)
3. [步骤1：创建测试用户](#步骤1创建测试用户)
4. [步骤2：准备测试商品](#步骤2准备测试商品)
5. [步骤3：使用JMeter进行压力测试](#步骤3使用jmeter进行压力测试)
6. [步骤4：验证测试结果](#步骤4验证测试结果)
7. [常见问题](#常见问题)

---

## 📖 测试概述

### 测试目标

验证系统在**高并发**场景下是否能正确防止**库存超卖**问题。

### 测试场景

- **场景**：100个用户同时购买同一商品（库存为10）
- **预期结果**：只有10个订单成功，90个返回"库存不足"
- **验证指标**：
  - ✅ 最终库存 = 0（不能为负数）
  - ✅ 成功订单数 = 10（不超过库存）
  - ✅ 无超卖现象

---

## 🛠️ 准备工作

### 1. 环境要求

- ✅ 后端服务正常运行（`http://localhost:8080`）
- ✅ 数据库正常运行（MySQL）
- ✅ Redis正常运行（用于分布式锁）
- ✅ JMeter已安装（[下载地址](https://jmeter.apache.org/download_jmeter.cgi)）

### 2. 安装Python依赖（用于批量创建用户）

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

**注意**：如果 `pip` 命令不可用，使用 `py -m pip` 代替 `pip`

### 3. 了解API接口

需要知道的接口：
- **创建订单**：`POST /api/user/order/create`
- **用户登录**：`POST /api/user/auth/login`
- **用户注册**：`POST /api/user/auth/register-new`

---

## 👥 步骤1：创建测试用户

### 方法1：使用脚本批量创建（推荐）

**Windows命令提示符（CMD）**：
```cmd
cd njumarket\scripts
python batch_create_users_simple.py
```

**或者使用PowerShell**：
```powershell
cd njumarket\scripts
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

**脚本会自动**：
- ✅ 创建100个测试用户（username_test_1 到 username_test_100）
- ✅ 获取每个用户的token
- ✅ 保存到 `user_tokens.csv` 文件

**输出文件**：
- `user_tokens.csv` - 包含用户名、手机号、token等信息

### 方法2：手动创建（如果脚本失败）

如果脚本运行失败，可以手动创建几个测试用户：

1. 访问注册页面：`http://localhost:8080/register`
2. 创建用户（例如10个）
3. 登录获取token

---

## 📦 步骤2：准备测试商品

### 在数据库中创建测试商品

```sql
-- 1. 查找一个卖家用户ID（或使用测试用户）
SELECT user_id, username FROM users WHERE username LIKE 'username_test%' LIMIT 1;

-- 假设找到的seller_id是 'USER_xxx'
-- 2. 创建测试商品（库存为10）
INSERT INTO commodities (
    commodity_id, 
    seller_id, 
    title, 
    description, 
    price, 
    stock, 
    commodity_status,
    publish_time
) VALUES (
    'test-commodity-001',
    'USER_xxx',  -- 替换为实际的seller_id
    'JMeter压力测试商品',
    '用于测试库存超卖的商品',
    100.00,
    10,  -- 库存为10
    'ON_SHELF',
    NOW()
);
```

### 验证商品创建成功

```sql
-- 检查商品信息
SELECT commodity_id, title, stock, commodity_status 
FROM commodities 
WHERE commodity_id = 'test-commodity-001';
```

**确认信息**：
- ✅ `stock` = 10
- ✅ `commodity_status` = 'ON_SHELF'

---

## 🔧 步骤3：使用JMeter进行压力测试

### 3.1 安装JMeter

1. **下载JMeter**：
   - 访问：https://jmeter.apache.org/download_jmeter.cgi
   - 下载 `apache-jmeter-x.x.zip`

2. **解压并运行**（Windows）：
   
   **方法1：双击运行**（推荐）：
   - 找到JMeter安装目录（如：`D:\apache-jmeter-5.6`）
   - 双击 `bin\jmeter.bat` 文件
   
   **方法2：命令行运行**：
   ```cmd
   D:\apache-jmeter-5.6\bin\jmeter.bat
   ```
   
   **方法3：创建快捷方式**：
   - 右键 `jmeter.bat` → 发送到 → 桌面快捷方式
   - 以后直接双击桌面图标即可

### 3.2 创建测试计划

#### 步骤1：创建线程组

1. 右键 `Test Plan` → `Add` → `Threads (Users)` → `Thread Group`
2. 配置参数：
   - **Number of Threads (users)**: `100` （100个并发用户）
   - **Ramp-up Period (seconds)**: `5` （5秒内启动所有线程）
   - **Loop Count**: `1` （每个用户只执行一次）

#### 步骤2：添加CSV数据配置

1. 在左侧面板，右键 `Thread Group` → `Add` → `Config Element` → `CSV Data Set Config`

2. 在右侧配置面板设置：
   - **Filename**: 
     - 点击 `Browse...` 按钮，找到 `user_tokens.csv` 文件
     - 或者直接输入完整路径：`D:\软工作业\NJUMarket\njumarket\scripts\user_tokens.csv`
   - **Variable Names**: `username,phone,token,userId`
   - **Delimiter**: `,` （逗号）
   - **Recycle on EOF**: 勾选 `True`
   - **Stop thread on EOF**: 不勾选（保持 `False`）

#### 步骤3：添加HTTP请求

1. 右键 `Thread Group` → `Add` → `Sampler` → `HTTP Request`
2. 配置基本信息：
   - **Name**: `创建订单 - 库存超卖测试`（可以自定义）
   - **Server Name or IP**: `localhost`
   - **Port Number**: `8080`
   - **HTTP Request**: 选择 `POST`
   - **Path**: `/api/user/order/create`

3. **添加请求头**：
   - 在左侧面板，右键 `HTTP Request` → `Add` → `Config Element` → `HTTP Header Manager`
   - 在右侧配置面板，点击 `Add` 按钮添加：
     - **Name**: `Content-Type`
     - **Value**: `application/json`
   - 再次点击 `Add` 按钮添加：
     - **Name**: `Authorization`
     - **Value**: `Bearer ${token}`

4. **添加请求体**：
   - 在HTTP Request配置面板下方，找到 `Body Data` 标签页
   - 点击 `Body Data` 标签
   - 在文本框中输入：
   ```json
   {
     "commodityId": "test-commodity-001",
     "quantity": 1,
     "payAmount": 100.00,
     "shippingAddress": "测试地址",
     "remark": "JMeter压力测试"
   }
   ```

#### 步骤4：添加响应断言

1. 在左侧面板，右键 `HTTP Request` → `Add` → `Assertions` → `Response Assertion`

2. 在右侧配置面板设置：
   - **Apply to**: 选择 `Main sample only`
   - 勾选 **Response Code**，在输入框中输入 `200`
   - 在 **Patterns to Test** 区域：
     - 点击 `Add` 按钮，输入：`订单创建成功` （成功响应）
     - 再次点击 `Add` 按钮，输入：`库存不足` （这也是正常的业务响应）

#### 步骤5：添加结果监听器

1. **聚合报告**（推荐）：
   - 在左侧面板，右键 `Thread Group` → `Add` → `Listener` → `Aggregate Report`
   - 运行测试后，在这里查看统计信息

2. **查看结果树**（调试用，正式测试时建议关闭）：
   - 在左侧面板，右键 `Thread Group` → `Add` → `Listener` → `View Results Tree`
   - 运行测试后，在这里查看每个请求的详细信息

### 3.3 执行测试

#### 方法1：GUI模式（调试用）

1. 点击顶部工具栏的 **绿色播放按钮** ▶️（或按 `Ctrl+R`）
2. 等待测试完成
3. 在左侧面板点击 `Aggregate Report`，查看统计信息
4. 在左侧面板点击 `View Results Tree`，查看详细响应（调试用）

#### 方法2：命令行模式（正式测试）

**Windows命令提示符（CMD）**：
```cmd
cd D:\apache-jmeter-5.6\bin
jmeter.bat -n -t D:\软工作业\NJUMarket\njumarket\scripts\stock_oversale_test.jmx -l results.jtl -e -o report\
```

**或者使用完整路径**：
```cmd
D:\apache-jmeter-5.6\bin\jmeter.bat -n -t "D:\软工作业\NJUMarket\njumarket\scripts\stock_oversale_test.jmx" -l results.jtl -e -o report\
```

**参数说明**：
- `-n`: 非GUI模式（更快）
- `-t`: 测试脚本文件路径
- `-l`: 结果日志文件
- `-e`: 生成HTML报告
- `-o`: HTML报告输出目录

---

## ✅ 步骤4：验证测试结果

### 4.1 验证库存准确性

**测试前**：
```sql
SELECT stock FROM commodities WHERE commodity_id = 'test-commodity-001';
-- 结果应该是：10
```

**测试后**：
```sql
SELECT stock FROM commodities WHERE commodity_id = 'test-commodity-001';
-- 结果应该是：0（如果10个订单都成功）
```

### 4.2 验证订单数量

```sql
SELECT COUNT(*) as order_count 
FROM orders 
WHERE commodity_id = 'test-commodity-001' 
AND order_status = 'CREATED';
-- 结果应该是：10（不超过库存）
```

### 4.3 查看JMeter报告

**关键指标**：
- **Samples**: 总请求数（应该是100）
- **Average**: 平均响应时间
- **Error %**: 错误率（应该为0%）
- **Throughput**: 吞吐量（每秒处理请求数）

**预期结果**：
- ✅ 成功订单数 = 10（等于库存）
- ✅ 库存不足响应 = 90
- ✅ 错误率 = 0%（没有系统错误）
- ✅ 最终库存 = 0

---

## ❓ 常见问题

### Q1: 脚本运行失败，提示连接错误？

**A**: 检查：
1. 后端服务是否正常运行（`http://localhost:8080`）
2. Redis是否正常运行
3. 网络连接是否正常

### Q2: 验证码获取失败？

**A**: 
- 检查后端日志，验证码会打印在控制台
- 确认Redis服务正常运行
- 检查手机号格式是否正确（11位数字）

### Q3: JMeter测试时，所有请求都返回401？

**A**: 
- 检查token是否正确（CSV文件中的token列）
- 确认HTTP Header中Authorization格式正确：`Bearer ${token}`
- 检查token是否过期

### Q4: 测试结果中订单数超过库存？

**A**: 
- 检查是否使用了分布式锁和悲观锁
- 检查数据库事务是否正确配置
- 查看后端日志，确认锁机制是否生效

### Q5: 如何查看详细的测试结果？

**A**: 
- 使用 `View Results Tree` 查看每个请求的详细信息
- 使用命令行模式生成HTML报告
- 查看后端日志，了解详细的处理过程

---

## 📚 相关文档

- [批量用户创建指南](./README_BATCH_USERS.md)
- [JMeter详细使用说明](./JMETER_GUIDE.md)
- [库存超卖解决方案说明](./STOCK_OVERSALE_SOLUTION.md)

---

## 🎯 测试检查清单

测试前：
- [ ] 后端服务正常运行
- [ ] 数据库正常运行
- [ ] Redis正常运行
- [ ] 已创建100个测试用户
- [ ] 已创建测试商品（库存=10）
- [ ] JMeter已安装并配置

测试中：
- [ ] JMeter测试计划已正确配置
- [ ] CSV文件路径正确
- [ ] HTTP请求路径和参数正确
- [ ] Token已正确配置

测试后：
- [ ] 验证最终库存 = 0
- [ ] 验证订单数 = 10
- [ ] 验证无超卖现象（库存不为负）
- [ ] 查看JMeter报告，分析性能指标

---

**文档版本**：v1.0  
**适合人群**：初学者开发者  
**最后更新**：2025-01-XX
