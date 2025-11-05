# JMeter 库存超卖测试详细指南

> ⚠️ **注意**：这是详细的技术文档。如果你是初学者，建议先阅读 [README_TESTING.md](./README_TESTING.md) 和 [JMETER_GUIDE.md](./JMETER_GUIDE.md)

## 📋 文档说明

本文档是JMeter测试的**详细技术说明**，包含：
- 完整的JMeter配置步骤
- 高级测试场景
- 详细的参数说明

**适合人群**：有JMeter使用经验的开发者

**初学者推荐**：
1. 先看 [README_TESTING.md](./README_TESTING.md) - 完整测试流程
2. 再看 [JMETER_GUIDE.md](./JMETER_GUIDE.md) - JMeter快速入门
3. 最后看本文档 - 深入了解

---

## 🔧 环境准备

### 1. 安装 JMeter

1. 下载 JMeter：https://jmeter.apache.org/download_jmeter.cgi
2. 解压到目录（如：`D:\apache-jmeter-5.6`）
3. 配置环境变量（可选）：
   ```bash
   JMETER_HOME=D:\apache-jmeter-5.6
   PATH=%PATH%;%JMETER_HOME%\bin
   ```

### 2. 准备测试数据

#### 2.1 创建测试商品

在数据库中创建一个库存为 **10** 的测试商品：

```sql
-- 假设商品ID为：test-commodity-001
-- 库存为：10
-- 价格为：100.00
UPDATE commodities 
SET stock = 10, 
    commodity_status = 'ON_SHELF',
    price = 100.00
WHERE commodity_id = 'test-commodity-001';
```

#### 2.2 准备测试用户Token

需要准备多个测试用户的登录Token，用于模拟并发下单。

**获取Token的方法**：
1. 使用Postman或浏览器登录获取Token
2. 或者编写脚本批量获取Token

**Token格式**（示例）：
```
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

---

## 📝 JMeter 测试脚本创建

### 步骤1：创建测试计划

1. 启动 JMeter（Windows）：
   
   **方法1：双击运行**（推荐）：
   - 找到JMeter安装目录（如：`D:\apache-jmeter-5.6`）
   - 双击 `bin\jmeter.bat` 文件
   
   **方法2：命令行运行**：
   ```cmd
   D:\apache-jmeter-5.6\bin\jmeter.bat
   ```

2. 创建测试计划：
   - 右键 `Test Plan` → `Add` → `Threads (Users)` → `Thread Group`

### 步骤2：配置线程组

**线程组设置**：
- **Number of Threads (users)**: `100` （100个并发用户）
- **Ramp-up Period (seconds)**: `5` （5秒内启动所有线程）
- **Loop Count**: `1` （每个用户只执行一次）
- **Scheduler**: 不勾选（立即执行）

**说明**：
- 100个并发用户，每个用户购买数量为1
- 如果商品库存为10，理论上只有10个订单应该成功
- 其余90个应该返回"库存不足"

### 步骤3：添加 HTTP 请求

1. **右键 Thread Group** → `Add` → `Sampler` → `HTTP Request`

2. **配置 HTTP 请求**：
   - **Name**: `创建订单 - 库存超卖测试`
   - **Server Name or IP**: `localhost` （或你的服务器地址）
   - **Port Number**: `8080` （或你的端口）
   - **HTTP Request**: `POST`
   - **Path**: `/api/user/order/create`

3. **添加请求头**：
   - **右键 HTTP Request** → `Add` → `Config Element` → `HTTP Header Manager`
   - 添加以下Header：
     ```
     Content-Type: application/json
     Authorization: Bearer ${__V(token${__threadNum})}
     ```
     （这里使用变量，需要配合CSV Data Set Config）

### 步骤4：添加请求体（JSON）

1. **右键 HTTP Request** → `Add` → `Config Element` → `HTTP Request Defaults`（可选）
2. 在 **Body Data** 或使用 **Body Data** 标签页添加：

```json
{
  "commodityId": "test-commodity-001",
  "quantity": 1,
  "payAmount": 100.00,
  "shippingAddress": "测试地址",
  "remark": "JMeter压力测试"
}
```

### 步骤5：添加用户Token数据（CSV）

1. **右键 Thread Group** → `Add` → `Config Element` → `CSV Data Set Config`

2. **配置 CSV Data Set Config**：
   - **Filename**: `tokens.csv` （需要创建这个文件）
   - **Variable Names**: `token`
   - **Delimiter**: `,` （逗号分隔）
   - **Recycle on EOF**: `True` （循环使用）
   - **Stop thread on EOF**: `False`

3. **创建 tokens.csv 文件**（在JMeter的bin目录下）：
   ```
   token1
   token2
   token3
   ...
   token100
   ```

4. **修改 HTTP Header Manager**：
   ```
   Authorization: Bearer ${token}
   ```

### 步骤6：添加结果监听器

#### 6.1 查看结果树（调试用）

- **右键 Thread Group** → `Add` → `Listener` → `View Results Tree`
- **用途**：查看每个请求的详细信息（调试时使用，正式测试时建议关闭）

#### 6.2 聚合报告（推荐）

- **右键 Thread Group** → `Add` → `Listener` → `Aggregate Report`
- **用途**：查看统计信息（成功率、平均响应时间等）

#### 6.3 汇总报告

- **右键 Thread Group** → `Add` → `Listener` → `Summary Report`
- **用途**：查看汇总信息

#### 6.4 响应断言（验证结果）

- **右键 HTTP Request** → `Add` → `Assertions` → `Response Assertion`
- **配置**：
  - **Apply to**: `Main sample only`
  - **Response Code**: 勾选
  - **Patterns to Test**: `200` （成功响应）
  - 或者添加文本断言：`"订单创建成功"` 或 `"库存不足"`

---

## 🚀 执行测试

### 方法1：GUI模式（调试用）

1. 点击 **绿色播放按钮** 或按 `Ctrl+R`
2. 观察结果树中的响应
3. 查看聚合报告中的统计信息

### 方法2：命令行模式（正式测试）

**Windows命令提示符（CMD）**：
```cmd
cd D:\apache-jmeter-5.6\bin
jmeter.bat -n -t "D:\软工作业\NJUMarket\njumarket\scripts\stock_oversale_test.jmx" -l results.jtl -e -o report\
```

**或者使用完整路径**：
```cmd
D:\apache-jmeter-5.6\bin\jmeter.bat -n -t "D:\软工作业\NJUMarket\njumarket\scripts\stock_oversale_test.jmx" -l results.jtl -e -o report\
```

**参数说明**：
- `-n`: 非GUI模式
- `-t`: 测试脚本文件
- `-l`: 结果日志文件
- `-e`: 生成HTML报告
- `-o`: HTML报告输出目录

---

## 📊 测试结果分析

### 1. 验证库存准确性

**测试前**：
```sql
SELECT stock FROM commodities WHERE commodity_id = 'test-commodity-001';
-- 结果应该是：10
```

**执行100个并发请求后**：
```sql
SELECT stock FROM commodities WHERE commodity_id = 'test-commodity-001';
-- 结果应该是：0（如果10个订单成功）
-- 或者：> 0（如果部分订单失败）
```

**验证订单数量**：
```sql
SELECT COUNT(*) FROM orders 
WHERE commodity_id = 'test-commodity-001' 
AND order_status = 'CREATED';
-- 结果应该是：<= 10（不超过库存）
```

### 2. 查看JMeter聚合报告

**关键指标**：
- **Samples**: 总请求数（应该是100）
- **Average**: 平均响应时间
- **Min/Max**: 最小/最大响应时间
- **Error %**: 错误率（库存不足的请求不算错误）
- **Throughput**: 吞吐量（每秒处理请求数）

**预期结果**：
- **成功订单数**: 10个（等于库存）
- **库存不足响应**: 90个
- **错误率**: 0%（没有系统错误，只有库存不足的业务提示）
- **库存最终值**: 0（10个订单全部成功）

### 3. 验证无超卖

**关键验证**：
1. ✅ 订单总数 <= 库存数量
2. ✅ 最终库存 >= 0（不能为负数）
3. ✅ 所有成功的订单状态为 `CREATED`
4. ✅ 库存不足的请求返回正确的错误信息

---

## 🔍 高级测试场景

### 场景1：不同购买数量测试

**配置**：
- 商品库存：100
- 并发用户：50
- 每个用户购买数量：随机（1-5）

**测试目的**：验证不同数量下的库存扣减准确性

### 场景2：长时间高并发测试

**配置**：
- 并发用户：50
- 持续时间：5分钟
- 循环次数：持续

**测试目的**：验证系统长时间运行的稳定性

### 场景3：混合场景测试

**配置**：
- 同时执行：创建订单、取消订单、查询库存
- 验证：取消订单后库存是否正确恢复

---

## 🛠️ 常见问题

### 问题1：Token过期

**解决方案**：
- 使用较长的Token有效期
- 或者使用脚本自动刷新Token

### 问题2：数据库连接池耗尽

**解决方案**：
- 增加数据库连接池大小
- 减少并发线程数
- 增加数据库连接超时时间

### 问题3：请求被限流

**解决方案**：
- 调整限流配置
- 或者暂时关闭限流进行测试

---

## 📝 测试脚本模板

### 完整测试计划结构

```
Test Plan
├── Thread Group (100 users, 5s ramp-up)
│   ├── CSV Data Set Config (tokens.csv)
│   ├── HTTP Request Defaults
│   ├── HTTP Header Manager
│   ├── HTTP Request (创建订单)
│   │   ├── Response Assertion
│   │   └── JSON Extractor (提取订单ID)
│   └── Listeners
│       ├── View Results Tree
│       ├── Aggregate Report
│       └── Summary Report
```

---

## 🎯 测试检查清单

- [ ] 测试商品库存已设置为固定值（如10）
- [ ] 准备了足够的测试用户Token（至少100个）
- [ ] 配置了正确的API路径和端口
- [ ] 添加了响应断言验证结果
- [ ] 配置了结果监听器
- [ ] 测试前备份数据库
- [ ] 测试后验证库存和订单数量
- [ ] 确认无超卖现象（库存不为负）

---

## 📚 参考资源

- [JMeter官方文档](https://jmeter.apache.org/usermanual/index.html)
- [JMeter压力测试教程](https://www.jianshu.com/p/4e3d3b2c1e1e)
- [HTTP Request Sampler](https://jmeter.apache.org/usermanual/component_reference.html#HTTP_Request)

---

**文档版本**：v1.0  
**最后更新**：2025-01-XX  
**维护者**：NJUMarket 开发团队
