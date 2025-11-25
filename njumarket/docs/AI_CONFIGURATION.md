# AI 功能配置文档

## 📋 前置条件检查清单

在开始配置之前，请确认以下内容：

- [x] ✅ PostgreSQL 已安装并运行
- [x] ✅ pgvector 扩展已安装（`CREATE EXTENSION vector;`）
- [x] ✅ 向量表已创建（`commodity_vectors`、`user_profile_vectors`、`conversation_vectors`）
- [ ] ⏳ 火山引擎 Doubao API Key 已获取
- [ ] ⏳ 环境变量已配置

---

## 🔑 第一步：获取火山引擎 Doubao API Key

1. 访问 [火山引擎控制台](https://console.volcengine.com/)
2. 注册/登录账号
3. 进入 AI 服务/豆包大模型页面
4. 创建新的 API Key
5. 复制 API Key

> ⚠️ **注意**：请妥善保管 API Key，不要泄露到代码仓库中！

---

## ⚙️ 第二步：配置环境变量

### 方式一：通过环境变量配置（推荐）

在启动服务前设置环境变量：

**Windows (CMD):**
```cmd
set DOUBAO_API_KEY=your-api-key-here
set DOUBAO_BASE_URL=https://ark.cn-beijing.volces.com/api/v3
set DOUBAO_CHAT_MODEL=doubao-seed-1.6
set DOUBAO_EMBEDDING_MODEL=doubao-embedding
```

**Windows (PowerShell):**
```powershell
$env:DOUBAO_API_KEY="your-api-key-here"
$env:DOUBAO_BASE_URL="https://ark.cn-beijing.volces.com/api/v3"
$env:DOUBAO_CHAT_MODEL="doubao-seed-1.6"
$env:DOUBAO_EMBEDDING_MODEL="doubao-embedding"
```

**Linux/Mac:**
```bash
export DOUBAO_API_KEY=your-api-key-here
export DOUBAO_BASE_URL=https://ark.cn-beijing.volces.com/api/v3
export DOUBAO_CHAT_MODEL=doubao-seed-1.6
export DOUBAO_EMBEDDING_MODEL=doubao-embedding
```

### 方式二：修改配置文件（不推荐用于生产环境）

直接修改配置文件中的默认值：

**文件：`njumarket-config/src/main/resources/config-repo/njumarket-service-commodity-dev.yml`**

```yaml
spring:
  ai:
    openai:
      api-key: your-api-key-here  # 替换为你的 API Key
      base-url: https://ark.cn-beijing.volces.com/api/v3
      chat:
        options:
          model: doubao-seed-1.6
      embedding:
        options:
          model: doubao-embedding
          dimensions: 1536
```

**文件：`njumarket-config/src/main/resources/config-repo/njumarket-service-auth-dev.yml`**

```yaml
spring:
  ai:
    openai:
      api-key: your-api-key-here  # 替换为你的 API Key
      base-url: https://ark.cn-beijing.volces.com/api/v3
      embedding:
        options:
          model: doubao-embedding
          dimensions: 1536
```

---

## 🗄️ 第三步：验证数据库表

执行以下 SQL 检查表是否已创建：

```sql
-- 检查 pgvector 扩展
SELECT * FROM pg_extension WHERE extname = 'vector';

-- 检查商品向量表
SELECT table_name, table_schema 
FROM information_schema.tables 
WHERE table_schema = 'nju_market' 
  AND table_name IN ('commodity_vectors', 'user_profile_vectors', 'conversation_vectors');

-- 检查索引
SELECT indexname, tablename 
FROM pg_indexes 
WHERE schemaname = 'nju_market' 
  AND tablename LIKE '%vector%';
```

如果表不存在，请执行：
- `database/vector-init.sql` - 创建商品向量表
- `database/agent-vector-init.sql` - 创建用户和对话向量表

---

## 🚀 第四步：启动服务

1. **启动 Config Server**（如果使用）
2. **启动 Eureka Server**（如果使用）
3. **启动 Auth Service**
4. **启动 Commodity Service**

检查日志中是否有以下信息：

```
✅ Spring AI OpenAI configured
✅ EmbeddingClient initialized
✅ ChatClient initialized
```

如果看到错误，请检查：
- API Key 是否正确
- 网络连接是否正常
- 火山引擎 Doubao API 服务是否可用
- base-url 是否正确（根据火山引擎官方文档确认）

---

## 🧪 第五步：测试 AI 功能

### 5.1 测试商品向量化

**方式一：通过内部接口批量生成向量**

```bash
# 使用 curl
curl -X POST "http://localhost:8082/api/internal/commodity/vector/batch-generate?batchSize=10"

# 或使用 Postman
POST http://localhost:8082/api/internal/commodity/vector/batch-generate?batchSize=10
```

**方式二：发布新商品（自动向量化）**

发布一个新商品，系统会自动生成向量。

**验证向量是否生成：**

```sql
SELECT 
    commodity_id, 
    LENGTH(content) as content_length,
    created_at
FROM nju_market.commodity_vectors 
ORDER BY created_at DESC 
LIMIT 10;
```

### 5.2 测试 AI 搜索

**前端测试：**
1. 访问首页
2. 在搜索框输入查询（如："二手笔记本电脑"）
3. 点击 **"AI搜索"** 按钮（金黄色按钮）
4. 查看搜索结果

**API 测试：**

```bash
curl -X GET "http://localhost:8082/api/public/commodity/ai-search?query=二手笔记本电脑&location=南京"
```

### 5.3 测试 AI Agent（如果已实现）

```bash
curl -X POST "http://localhost:8082/api/public/commodity/ai-agent/chat" \
  -H "Content-Type: application/json" \
  -d '{
    "message": "我想买一台二手笔记本电脑",
    "userId": "USER_123",
    "conversationId": "CONV_123"
  }'
```

---

## 🔍 常见问题排查

### 问题 1：API Key 无效

**错误信息：**
```
401 Unauthorized
Invalid API Key
```

**解决方案：**
- 检查 API Key 是否正确
- 确认 API Key 是否已激活
- 检查火山引擎账户余额和配额

### 问题 2：向量生成失败

**错误信息：**
```
向量生成失败: commodityId=xxx
```

**解决方案：**
- 检查网络连接
- 检查 API 配额是否用完
- 查看详细错误日志

### 问题 3：向量搜索无结果

**可能原因：**
- 商品向量尚未生成
- 搜索查询与商品内容不匹配
- 数据库索引未正确创建

**解决方案：**
```sql
-- 检查向量数量
SELECT COUNT(*) FROM nju_market.commodity_vectors;

-- 检查索引
SELECT indexname FROM pg_indexes 
WHERE tablename = 'commodity_vectors' 
  AND indexname LIKE '%embedding%';
```

### 问题 4：pgvector 扩展未安装

**错误信息：**
```
ERROR: type "vector" does not exist
```

**解决方案：**
```sql
-- 安装 pgvector 扩展
CREATE EXTENSION IF NOT EXISTS vector;

-- 验证安装
SELECT * FROM pg_extension WHERE extname = 'vector';
```

---

## 📊 配置参数说明

### Commodity Service 配置

| 参数 | 说明 | 默认值 | 必需 |
|------|------|--------|------|
| `DOUBAO_API_KEY` | 火山引擎 Doubao API 密钥 | - | ✅ |
| `DOUBAO_BASE_URL` | API 基础 URL | `https://ark.cn-beijing.volces.com/api/v3` | ❌ |
| `DOUBAO_CHAT_MODEL` | 聊天模型 | `doubao-seed-1.6` | ❌ |
| `DOUBAO_EMBEDDING_MODEL` | 向量化模型 | `doubao-embedding` | ❌ |

### Auth Service 配置

| 参数 | 说明 | 默认值 | 必需 |
|------|------|--------|------|
| `DOUBAO_API_KEY` | 火山引擎 Doubao API 密钥 | - | ✅ |
| `DOUBAO_BASE_URL` | API 基础 URL | `https://ark.cn-beijing.volces.com/api/v3` | ❌ |
| `DOUBAO_EMBEDDING_MODEL` | 向量化模型 | `doubao-embedding` | ❌ |

---

## 🎯 快速开始检查清单

完成以下步骤后，即可开始测试：

- [ ] 1. 获取火山引擎 Doubao API Key
- [ ] 2. 配置环境变量 `DOUBAO_API_KEY`
- [ ] 3. 确认 pgvector 扩展已安装
- [ ] 4. 确认向量表已创建
- [ ] 5. 启动服务并检查日志
- [ ] 6. 测试商品向量化
- [ ] 7. 测试 AI 搜索功能

---

## 📝 注意事项

1. **API 费用**：火山引擎 Doubao 按使用量计费，请注意控制调用频率
2. **向量维度**：当前配置使用 1536 维向量，与 `doubao-embedding` 模型匹配
3. **性能优化**：大量商品向量化建议使用批量接口，避免频繁调用
4. **数据安全**：生产环境请使用环境变量或配置中心管理 API Key
5. **base-url 配置**：请根据火山引擎官方文档确认正确的 base-url，不同区域可能有不同的地址

---

## 🔗 相关文档

- [火山引擎豆包大模型 API 文档](https://www.volcengine.com/docs/6492/1544808)
- [pgvector 文档](https://github.com/pgvector/pgvector)
- [Spring AI 文档](https://docs.spring.io/spring-ai/reference/)

---

**配置完成后，即可开始使用 AI 搜索和 Agent 功能！** 🎉

