# AI 功能测试指南

## 📌 测试概述

本文档提供向量生成功能和 AI 搜索功能的测试方法，包括 curl 命令和 Swagger 示例参数。

> **测试环境**：开发环境  
> **服务端口**：8092（Commodity Service）  
> **基础URL**：`http://localhost:8092`

---

## 🧪 测试准备

### 1. 环境要求

- ✅ 服务已启动（Commodity Service）
- ✅ 数据库连接正常（PostgreSQL + pgvector）
- ✅ LangChain4j 配置正确（Doubao API Key 已配置）
- ✅ 已有商品数据（用于向量生成和搜索测试）

### 2. 验证数据库状态

```bash
# 检查 pgvector 扩展
curl -X GET "http://localhost:8092/api/internal/debug/database"
```

**预期响应**：
```json
{
  "success": true,
  "message": "数据库诊断信息",
  "data": {
    "vector_extension_exists": true,
    "vector_type_exists": true,
    "vector_operator_exists": true,
    "vector_tables_count": 3
  }
}
```

---

## 📊 测试用例

### 1. 向量生成功能测试

#### 1.1 批量生成商品向量

**接口**：`POST /api/internal/commodity/vector/batch-generate`

**curl 命令**：
```bash
# 生成 10 个商品的向量
curl -X POST "http://localhost:8092/api/internal/commodity/vector/batch-generate?batchSize=10"

# 生成 50 个商品的向量（默认）
curl -X POST "http://localhost:8092/api/internal/commodity/vector/batch-generate?batchSize=50"

# 生成 100 个商品的向量
curl -X POST "http://localhost:8092/api/internal/commodity/vector/batch-generate?batchSize=100"
```

**Swagger 测试参数**：
- **URL**: `POST /api/internal/commodity/vector/batch-generate`
- **Query Parameters**:
  - `batchSize` (Integer, 可选): 批次大小，默认 50
    - 示例值: `10`, `50`, `100`

**预期响应**：
```json
{
  "success": true,
  "message": "批量向量化任务已启动",
  "data": null
}
```

**验证向量是否生成**：
```sql
-- 检查向量数量
SELECT COUNT(*) FROM nju_market.commodity_vectors;

-- 检查向量维度（应该是 2000 维）
SELECT 
    commodity_id,
    array_length(string_to_array(embedding::text, ','), 1) as dimension
FROM nju_market.commodity_vectors
LIMIT 10;
```

---

### 2. AI 语义搜索功能测试

#### 2.1 基础 AI 搜索

**接口**：`GET /api/public/commodity/ai-search`

**curl 命令**：
```bash
# 基础搜索（无位置偏好）
curl -X GET "http://localhost:8092/api/public/commodity/ai-search?query=二手笔记本电脑"

# 带位置偏好的搜索
curl -X GET "http://localhost:8092/api/public/commodity/ai-search?query=二手笔记本电脑&location=仙林校区"

# 使用 URL 编码（推荐）
curl -X GET "http://localhost:8092/api/public/commodity/ai-search?query=%E4%BA%8C%E6%89%8B%E7%AC%94%E8%AE%B0%E6%9C%AC%E7%94%B5%E8%84%91&location=%E4%BB%99%E6%9E%97%E6%A0%A1%E5%8C%BA"

# 使用自然语言查询
curl -X GET "http://localhost:8092/api/public/commodity/ai-search?query=我想买一个性价比高的手机"

# 搜索特定类型的商品
curl -X GET "http://localhost:8092/api/public/commodity/ai-search?query=适合学生用的平板电脑"
```

**Swagger 测试参数**：
- **URL**: `GET /api/public/commodity/ai-search`
- **Query Parameters**:
  - `query` (String, 必需): 搜索查询文本
    - 示例值: `"二手笔记本电脑"`, `"我想买一个性价比高的手机"`, `"适合学生用的平板电脑"`
  - `location` (String, 可选): 位置偏好
    - 示例值: `"仙林校区"`, `"鼓楼校区"`, `"南京"`

**预期响应**：
```json
{
  "success": true,
  "message": "AI搜索成功",
  "data": [
    {
      "commodityId": "COMMODITY_123456",
      "title": "二手笔记本电脑 ThinkPad X1",
      "price": 3500.00,
      "description": "9成新，配置良好...",
      "category": "ELECTRONICS",
      "commodityStatus": "ON_SHELF",
      "stock": 1,
      "sellerId": "USER_789",
      "sellerNickname": "张三",
      "createdAt": "2025-11-20T10:00:00",
      "updatedAt": "2025-11-20T10:00:00"
    },
    {
      "commodityId": "COMMODITY_123457",
      "title": "MacBook Pro 2019",
      "price": 6000.00,
      ...
    }
  ],
  "total": 15
}
```

**测试用例**：

| 测试场景 | query 参数 | location 参数 | 预期结果 |
|---------|-----------|--------------|---------|
| 基础搜索 | `"二手笔记本电脑"` | - | 返回相关商品列表 |
| 位置过滤 | `"二手笔记本电脑"` | `"仙林校区"` | 返回仙林校区相关商品 |
| 自然语言 | `"我想买一个性价比高的手机"` | - | 返回性价比高的手机 |
| 模糊查询 | `"适合学生用的平板"` | - | 返回学生适用的平板电脑 |
| 无结果 | `"不存在的商品类型"` | - | 返回空列表 |

---

### 3. AI Agent 对话功能测试

#### 3.1 AI Agent 对话（需要登录）

**接口**：`POST /api/user/commodity/ai-agent/chat`

**注意**：此接口需要用户登录，需要携带认证 Token。

**curl 命令**：
```bash
# 基础对话（需要替换 YOUR_TOKEN）
curl -X POST "http://localhost:8092/api/user/commodity/ai-agent/chat?message=我想买一台二手笔记本电脑&conversationId=CONV_123" \
  -H "Authorization: Bearer YOUR_TOKEN"

# 多轮对话（使用相同的 conversationId）
curl -X POST "http://localhost:8092/api/user/commodity/ai-agent/chat?message=价格在3000元左右的&conversationId=CONV_123" \
  -H "Authorization: Bearer YOUR_TOKEN"

# 新对话（不传 conversationId）
curl -X POST "http://localhost:8092/api/user/commodity/ai-agent/chat?message=帮我推荐一些商品" \
  -H "Authorization: Bearer YOUR_TOKEN"
```

**Swagger 测试参数**：
- **URL**: `POST /api/user/commodity/ai-agent/chat`
- **Headers**:
  - `Authorization`: `Bearer {token}` (必需)
- **Query Parameters**:
  - `message` (String, 必需): 用户消息
    - 示例值: `"我想买一台二手笔记本电脑"`, `"价格在3000元左右的"`, `"帮我推荐一些商品"`
  - `conversationId` (String, 可选): 对话ID，用于多轮对话
    - 示例值: `"CONV_123"`, `"CONV_456"`

**预期响应**：
```json
{
  "success": true,
  "message": "AI Agent 对话成功",
  "data": "好的，我来帮您找一些二手笔记本电脑。让我搜索一下相关的商品..."
}
```

**测试用例**：

| 测试场景 | message 参数 | conversationId | 预期行为 |
|---------|-------------|---------------|---------|
| 搜索意图 | `"我想买一台二手笔记本电脑"` | - | Agent 自动调用搜索工具 |
| 多轮对话 | `"价格在3000元左右的"` | `"CONV_123"` | 基于上下文理解 |
| 一般对话 | `"你好"` | - | 正常回复，不调用工具 |
| 询问商品 | `"这个商品怎么样？"` | `"CONV_123"` | 基于对话历史回答 |

---

### 4. AI Agent 智能搜索功能测试

#### 4.1 AI Agent 智能搜索（需要登录）

**接口**：`GET /api/user/commodity/ai-agent/search`

**注意**：此接口需要用户登录，需要携带认证 Token。

**curl 命令**：
```bash
# 基础智能搜索（需要替换 YOUR_TOKEN）
curl -X GET "http://localhost:8092/api/user/commodity/ai-agent/search?query=我想买一个性价比高的手机&conversationId=CONV_123" \
  -H "Authorization: Bearer YOUR_TOKEN"

# 带上下文的搜索
curl -X GET "http://localhost:8092/api/user/commodity/ai-agent/search?query=价格在2000元左右的&conversationId=CONV_123" \
  -H "Authorization: Bearer YOUR_TOKEN"
```

**Swagger 测试参数**：
- **URL**: `GET /api/user/commodity/ai-agent/search`
- **Headers**:
  - `Authorization`: `Bearer {token}` (必需)
- **Query Parameters**:
  - `query` (String, 必需): 搜索查询
    - 示例值: `"我想买一个性价比高的手机"`, `"价格在2000元左右的"`
  - `conversationId` (String, 可选): 对话ID，用于上下文理解
    - 示例值: `"CONV_123"`

**预期响应**：
```json
{
  "success": true,
  "message": "AI Agent 智能搜索成功",
  "data": {
    "commodities": [
      {
        "commodityId": "COMMODITY_123456",
        "title": "iPhone 12 128GB",
        "price": 2800.00,
        ...
      }
    ],
    "explanation": "根据您的需求，我为您找到了 5 个性价比高的手机，价格在 2000-3000 元之间。",
    "originalQuery": "我想买一个性价比高的手机",
    "enhancedQuery": "性价比高 手机",
    "total": 5
  }
}
```

---

### 5. 数据库诊断接口

#### 5.1 诊断数据库和 pgvector 状态

**接口**：`GET /api/internal/debug/database`

**curl 命令**：
```bash
curl -X GET "http://localhost:8092/api/internal/debug/database"
```

**Swagger 测试参数**：
- **URL**: `GET /api/internal/debug/database`
- **Query Parameters**: 无

**预期响应**：
```json
{
  "success": true,
  "message": "数据库诊断信息",
  "data": {
    "url": "jdbc:postgresql://localhost:5432/njumarket?currentSchema=nju_market",
    "catalog": "njumarket",
    "schema": "nju_market",
    "vector_extension_exists": true,
    "vector_extension_count": 1,
    "vector_type_exists": true,
    "vector_type_count": 1,
    "vector_operator_exists": true,
    "vector_operator_count": 3,
    "vector_operator_test": "success",
    "vector_operator_test_distance": 0.5,
    "vector_operators_detail": [
      {
        "oprname": "<=>",
        "left_type": "vector",
        "right_type": "vector",
        "result_type": "double precision"
      }
    ],
    "vector_tables_count": 3
  }
}
```

---

## 🔍 测试场景示例

### 场景 1：完整的向量生成和搜索流程

```bash
# 1. 检查数据库状态
curl -X GET "http://localhost:8092/api/internal/debug/database"

# 2. 批量生成向量（生成 20 个商品的向量）
curl -X POST "http://localhost:8092/api/internal/commodity/vector/batch-generate?batchSize=20"

# 3. 等待向量生成完成（异步处理，可能需要几秒到几分钟）

# 4. 测试 AI 搜索
curl -X GET "http://localhost:8092/api/public/commodity/ai-search?query=二手笔记本电脑"

# 5. 验证搜索结果
# 检查返回的商品是否与查询相关
```

### 场景 2：AI Agent 多轮对话

```bash
# 假设已获取 Token: YOUR_TOKEN

# 1. 第一轮对话（Agent 会自动调用搜索工具）
curl -X POST "http://localhost:8092/api/user/commodity/ai-agent/chat?message=我想买一台二手笔记本电脑&conversationId=CONV_TEST_001" \
  -H "Authorization: Bearer YOUR_TOKEN"

# 2. 第二轮对话（基于上下文）
curl -X POST "http://localhost:8092/api/user/commodity/ai-agent/chat?message=价格在3000元左右的&conversationId=CONV_TEST_001" \
  -H "Authorization: Bearer YOUR_TOKEN"

# 3. 第三轮对话（继续细化需求）
curl -X POST "http://localhost:8092/api/user/commodity/ai-agent/chat?message=ThinkPad 品牌的&conversationId=CONV_TEST_001" \
  -H "Authorization: Bearer YOUR_TOKEN"
```

### 场景 3：不同查询类型的测试

```bash
# 测试不同类型的查询

# 1. 具体商品名称
curl -X GET "http://localhost:8092/api/public/commodity/ai-search?query=iPhone 12"

# 2. 商品类别
curl -X GET "http://localhost:8092/api/public/commodity/ai-search?query=电子产品"

# 3. 需求描述
curl -X GET "http://localhost:8092/api/public/commodity/ai-search?query=适合学生用的"

# 4. 价格范围
curl -X GET "http://localhost:8092/api/public/commodity/ai-search?query=价格在1000到2000之间的手机"

# 5. 组合查询
curl -X GET "http://localhost:8092/api/public/commodity/ai-search?query=二手MacBook Pro 2019&location=仙林校区"
```

---

## 📝 Swagger UI 测试步骤

### 1. 访问 Swagger UI

```
http://localhost:8092/swagger-ui.html
```

### 2. 测试向量生成

1. 找到 `InternalController` → `batchGenerateVectors`
2. 点击 "Try it out"
3. 输入参数：
   - `batchSize`: `10`
4. 点击 "Execute"
5. 查看响应结果

### 3. 测试 AI 搜索

1. 找到 `PublicController` → `aiSearch`
2. 点击 "Try it out"
3. 输入参数：
   - `query`: `"二手笔记本电脑"`
   - `location`: `"仙林校区"` (可选)
4. 点击 "Execute"
5. 查看响应结果

### 4. 测试 AI Agent 对话（需要认证）

1. 先登录获取 Token（通过 Auth Service）
2. 在 Swagger UI 右上角点击 "Authorize"
3. 输入 Token: `Bearer {your_token}`
4. 找到 `UserCommodityController` → `aiAgentChat`
5. 点击 "Try it out"
6. 输入参数：
   - `message`: `"我想买一台二手笔记本电脑"`
   - `conversationId`: `"CONV_123"` (可选)
7. 点击 "Execute"
8. 查看响应结果

### 5. 测试 AI Agent 搜索（需要认证）

1. 确保已授权（同上）
2. 找到 `UserCommodityController` → `aiAgentSearch`
3. 点击 "Try it out"
4. 输入参数：
   - `query`: `"我想买一个性价比高的手机"`
   - `conversationId`: `"CONV_123"` (可选)
5. 点击 "Execute"
6. 查看响应结果

---

## ✅ 预期测试结果

### 向量生成

- ✅ 返回成功消息
- ✅ 日志显示向量生成进度
- ✅ 数据库中 `commodity_vectors` 表有数据
- ✅ 向量维度为 2000 维

### AI 搜索

- ✅ 返回相关商品列表
- ✅ 商品按相似度排序（最相关的在前）
- ✅ 支持自然语言查询
- ✅ 支持位置过滤

### AI Agent 对话

- ✅ Agent 能够理解用户意图
- ✅ 自动调用搜索工具（当需要时）
- ✅ 返回友好的回复
- ✅ 支持多轮对话上下文

### AI Agent 搜索

- ✅ 返回搜索结果和 AI 解释
- ✅ 包含原始查询和增强查询
- ✅ 商品列表按相关性排序

---

## 🐛 常见问题

### 1. 向量生成失败

**错误**：`向量生成失败: commodityId=xxx`

**排查步骤**：
1. 检查 LangChain4j 配置是否正确
2. 检查 Doubao API Key 是否有效
3. 查看日志中的详细错误信息
4. 检查网络连接

### 2. AI 搜索无结果

**可能原因**：
- 商品向量尚未生成
- 搜索查询与商品内容不匹配
- 数据库中没有商品数据

**解决方案**：
```bash
# 1. 检查向量数量
SELECT COUNT(*) FROM nju_market.commodity_vectors;

# 2. 如果向量数量为 0，先生成向量
curl -X POST "http://localhost:8092/api/internal/commodity/vector/batch-generate?batchSize=50"

# 3. 检查商品数据
SELECT COUNT(*) FROM nju_market.commodities WHERE commodity_status = 'ON_SHELF';
```

### 3. AI Agent 对话返回错误

**错误**：`401 Unauthorized`

**解决方案**：
- 确保已登录并获取 Token
- 在请求头中正确设置 `Authorization: Bearer {token}`

### 4. Function Calling 不工作

**现象**：Agent 不自动调用搜索工具

**排查步骤**：
1. 检查 `SearchCommoditiesTool` 是否正确注册
2. 检查系统提示词是否包含工具使用说明
3. 查看日志中的 Function Calling 相关信息
4. 确认 Doubao Chat 模型支持 Function Calling

---

## 📊 性能测试

### 向量生成性能

```bash
# 测试不同批次大小的性能
time curl -X POST "http://localhost:8092/api/internal/commodity/vector/batch-generate?batchSize=10"
time curl -X POST "http://localhost:8092/api/internal/commodity/vector/batch-generate?batchSize=50"
time curl -X POST "http://localhost:8092/api/internal/commodity/vector/batch-generate?batchSize=100"
```

### AI 搜索性能

```bash
# 测试搜索响应时间
time curl -X GET "http://localhost:8092/api/public/commodity/ai-search?query=二手笔记本电脑"
```

---

## 📚 相关文档

- [LangChain4j 迁移指南](./LANGCHAIN4J_MIGRATION.md)
- [AI 配置文档](./AI_CONFIGURATION.md)
- [项目文档 v3.0.0](./PROJECT_DOCUMENTATION_V3.0.0.md)

---

**文档版本**：v3.2.0  
**最后更新**：2025 年 11 月  
**维护者**：NJUMarket 开发团队

