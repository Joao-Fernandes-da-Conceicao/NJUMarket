# JMeter 压力测试简明指南

## 📋 什么是JMeter？

JMeter是一个**开源的压力测试工具**，用于测试Web应用的性能。

**简单理解**：模拟多个用户同时访问你的网站，测试系统能否承受。

---

## 🚀 快速开始（5分钟上手）

### 步骤1：安装JMeter

1. **下载**：
   - 访问：https://jmeter.apache.org/download_jmeter.cgi
   - 下载 `apache-jmeter-x.x.zip`（选择.zip格式）

2. **解压**（Windows）：
   - 右键zip文件 → 解压到当前文件夹
   - 或者解压到任意目录（如：`D:\apache-jmeter-5.6`）
   
3. **创建快捷方式**（推荐）：
   - 找到 `D:\apache-jmeter-5.6\bin\jmeter.bat`
   - 右键 → 发送到 → 桌面快捷方式
   - 以后直接双击桌面图标即可

3. **运行**（Windows）：
   
   **方法1：双击运行**（推荐）：
   - 找到 `D:\apache-jmeter-5.6\bin\jmeter.bat`
   - 双击运行
   
   **方法2：命令行运行**：
   ```cmd
   D:\apache-jmeter-5.6\bin\jmeter.bat
   ```
   
   **方法3：创建快捷方式**：
   - 右键 `jmeter.bat` → 发送到 → 桌面快捷方式
   - 以后直接双击桌面图标即可

### 步骤2：创建最简单的测试

#### 2.1 创建线程组

1. 启动JMeter后，你会看到左侧有一个 `Test Plan`
2. 右键 `Test Plan` → `Add` → `Threads (Users)` → `Thread Group`
3. 在右侧配置：
   - **Number of Threads**: `10` （10个用户）
   - **Ramp-up Period**: `1` （1秒内启动）
   - **Loop Count**: `1` （执行1次）

#### 2.2 添加HTTP请求

1. 在左侧面板，右键 `Thread Group` → `Add` → `Sampler` → `HTTP Request`
2. 在右侧配置面板设置：
   - **Server Name or IP**: `localhost`
   - **Port Number**: `8080`
   - **Path**: `/api/user/auth/login`
   - **HTTP Request**: 选择 `POST`

3. 添加请求体：
   - 点击下方的 `Body Data` 标签页
   - 在文本框中输入：
   ```json
   {
     "identifier": "13800000001",
     "password": "123456"
   }
   ```

#### 2.3 添加结果查看器

1. 在左侧面板，右键 `Thread Group` → `Add` → `Listener` → `View Results Tree`
2. 点击顶部工具栏的 **绿色播放按钮** ▶️（或按 `Ctrl+R`）
3. 查看结果！

---

## 📝 完整测试示例：库存超卖测试

### 场景说明

测试100个用户同时购买同一商品（库存只有10），验证是否会出现超卖。

### 详细步骤

#### 步骤1：创建线程组

**操作步骤**：
1. 在JMeter左侧面板，找到 `Test Plan`
2. 右键 `Test Plan` → `Add` → `Threads (Users)` → `Thread Group`

**配置参数**（在右侧配置面板）：
- **Number of Threads (users)**: `100` （100个并发用户）
- **Ramp-up Period (seconds)**: `5` （5秒内启动所有用户）
- **Loop Count**: `1` （每个用户只执行一次）

#### 步骤2：添加CSV数据（用户token）

**操作步骤**：
1. 在左侧面板，右键 `Thread Group` → `Add` → `Config Element` → `CSV Data Set Config`

**配置参数**（在右侧配置面板）：
- **Filename**: 点击 `Browse...` 按钮，选择 `user_tokens.csv` 文件
  - 或者直接输入：`D:\软工作业\NJUMarket\njumarket\scripts\user_tokens.csv`
- **Variable Names**: `username,phone,token,userId`
- **Delimiter**: `,` （逗号）
- **Recycle on EOF**: 勾选 `True`

**CSV文件格式**：
```csv
username,phone,token,userId
username_test_1,13800000001,eyJhbGci...,USER_xxx
username_test_2,13800000002,eyJhbGci...,USER_xxx
```

#### 步骤3：添加HTTP请求（创建订单）

**操作步骤**：
1. 在左侧面板，右键 `Thread Group` → `Add` → `Sampler` → `HTTP Request`

**基本配置**（在右侧配置面板）：
- **Name**: `创建订单`（可以自定义）
- **Server Name or IP**: `localhost`
- **Port Number**: `8080`
- **HTTP Request**: 选择 `POST`
- **Path**: `/api/user/order/create`

**添加请求头**：
1. 在左侧面板，右键 `HTTP Request` → `Add` → `Config Element` → `HTTP Header Manager`
2. 在右侧配置面板，点击 `Add` 按钮添加：
   - **Name**: `Content-Type`
   - **Value**: `application/json`
3. 再次点击 `Add` 添加：
   - **Name**: `Authorization`
   - **Value**: `Bearer ${token}`

**添加请求体**：
1. 在HTTP Request的配置面板下方，找到 `Body Data` 标签页
2. 点击 `Body Data` 标签
3. 在文本框中输入：
```json
{
  "commodityId": "test-commodity-001",
  "quantity": 1,
  "payAmount": 100.00,
  "shippingAddress": "测试地址",
  "remark": "JMeter压力测试"
}
```

#### 步骤4：添加断言（验证结果）

**操作步骤**：
1. 在左侧面板，右键 `HTTP Request` → `Add` → `Assertions` → `Response Assertion`

**配置参数**（在右侧配置面板）：
- **Apply to**: 选择 `Main sample only`
- 勾选 **Response Code**，在输入框中输入 `200`
- 在 **Patterns to Test** 区域：
  1. 点击 `Add` 按钮
  2. 输入：`订单创建成功`
  3. 再次点击 `Add` 按钮
  4. 输入：`库存不足` （这也是正常的业务响应）

#### 步骤5：添加结果监听器

**聚合报告**（推荐）：
1. 在左侧面板，右键 `Thread Group` → `Add` → `Listener` → `Aggregate Report`
2. 运行测试后，在这里查看统计信息

**查看结果树**（调试用，正式测试时建议关闭）：
1. 在左侧面板，右键 `Thread Group` → `Add` → `Listener` → `View Results Tree`
2. 运行测试后，在这里查看每个请求的详细信息

### 执行测试

1. **保存测试计划**：
   - 点击顶部菜单 `File` → `Save Test Plan As...`
   - 选择保存位置（如：`D:\软工作业\NJUMarket\njumarket\scripts\`）
   - 文件名输入：`stock_oversale_test.jmx`
   - 点击 `保存`

2. **运行测试**：
   - 点击顶部工具栏的 **绿色播放按钮** ▶️（或按 `Ctrl+R`）
   - 等待测试完成

3. **查看结果**：
   - 在左侧面板点击 `Aggregate Report`，查看统计信息
   - 在左侧面板点击 `View Results Tree`，查看详细响应（调试用）

---

## 📊 如何看懂测试结果

### 聚合报告（Aggregate Report）

| 指标 | 说明 | 正常值 |
|------|------|--------|
| **Samples** | 总请求数 | 100（你设置的线程数） |
| **Average** | 平均响应时间 | < 1000ms（1秒内） |
| **Min/Max** | 最小/最大响应时间 | - |
| **Error %** | 错误率 | 0%（无系统错误） |
| **Throughput** | 吞吐量（每秒处理数） | 越高越好 |

### 查看结果树（View Results Tree）

- **绿色** ✅ = 请求成功（HTTP 200）
- **红色** ❌ = 请求失败（HTTP 4xx/5xx）

点击任意请求可以查看：
- **Request**：发送的请求
- **Response Data**：服务器返回的响应

---

## 🎯 常用配置技巧

### 1. 使用变量

在请求中使用 `${变量名}` 引用CSV中的变量：

```
Path: /api/user/${userId}
Header: Authorization: Bearer ${token}
```

### 2. 设置超时时间

```
右键 HTTP Request
  → 高级
    → Timeouts (milliseconds)
      → Connect: 5000
      → Response: 10000
```

### 3. 添加随机延迟

```
右键 Thread Group
  → Add
    → Timer
      → Uniform Random Timer
```

配置：
- **Constant Delay**: `1000` （固定延迟1秒）
- **Random Delay**: `500` （随机延迟0-0.5秒）

### 4. 只运行部分请求

选中要运行的请求，右键 → `Enable/Disable` 可以启用/禁用

---

## ⚠️ 常见错误及解决方法

### 错误1：Connection refused

**原因**：后端服务没有运行

**解决**：
1. 检查后端服务是否启动
2. 检查端口号是否正确（默认8080）

### 错误2：401 Unauthorized

**原因**：Token无效或过期

**解决**：
1. 检查CSV文件中的token是否正确
2. 检查HTTP Header中Authorization格式：`Bearer ${token}`
3. 重新运行用户创建脚本获取新token

### 错误3：所有请求都失败

**原因**：可能是请求格式错误

**解决**：
1. 在 `View Results Tree` 中查看详细错误信息
2. 检查请求体JSON格式是否正确
3. 检查Content-Type是否为`application/json`

### 错误4：测试结果不准确

**原因**：可能使用了缓存的结果

**解决**：
1. 每次测试前清理结果树（右键 → Clear）
2. 重新运行测试

---

## 💡 测试最佳实践

### 1. 先测试少量用户

- 先用10个用户测试，确认配置正确
- 再逐步增加到100、1000

### 2. 保存测试计划

- 测试前保存JMX文件
- 方便后续修改和复用

### 3. 使用命令行模式

- GUI模式适合调试
- 命令行模式适合正式测试（更快）

### 4. 查看后端日志

- 同时观察后端日志
- 了解服务器处理情况

---

## 📚 更多资源

- [JMeter官方文档](https://jmeter.apache.org/usermanual/index.html)
- [JMeter中文教程](https://www.jianshu.com/p/4e3d3b2c1e1e)

---

**适合人群**：初学者  
**难度等级**：⭐⭐（简单）  
**预计时间**：30分钟上手
