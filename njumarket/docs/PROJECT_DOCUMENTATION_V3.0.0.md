# 南大集市 NJUMarket v3.0.0 项目文档

## 📌 版本概述

**NJUMarket v3.0.0** 是"AI 语义搜索"版本，核心目标是引入 Spring AI 框架和向量数据库，实现基于语义理解的商品搜索功能，为后续 AI Agent 能力打下基础。

> **版本状态**：✅ 已完成  
> **完成时间**：2025 年 11 月  
> **主要成果**：Spring AI 集成、pgvector 向量数据库、语义搜索、火山引擎 Doubao API 集成

---

## ✨ 核心成果

| 模块 | 功能 | 状态 |
| --- | --- | --- |
| **Spring AI 集成** | 引入 Spring AI 1.0.0-M4，支持 Embedding 和 Chat 模型 | ✅ |
| **向量数据库** | PostgreSQL + pgvector 扩展，支持向量存储和相似度搜索 | ✅ |
| **商品向量化** | 商品发布/更新时自动生成向量，支持批量向量化 | ✅ |
| **AI 语义搜索** | 基于向量相似度的语义搜索，支持自然语言查询 | ✅ |
| **用户画像向量化** | 用户画像向量化，支持相似用户搜索 | ✅ |
| **对话向量化** | 对话历史向量化，支持相关对话检索 | ✅ |
| **火山引擎集成** | 集成 Doubao Embedding 和 Chat 模型 | ✅ |
| **前端 AI 搜索** | 前端新增 AI 搜索按钮，支持语义搜索 | ✅ |

---

## 🧠 功能实现

### 1. Spring AI 框架集成

#### 1.1 依赖管理

在父 POM 中统一管理 Spring AI 版本：

```xml
<properties>
    <spring-ai.version>1.0.0-M4</spring-ai.version>
</properties>

<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>org.springframework.ai</groupId>
            <artifactId>spring-ai-bom</artifactId>
            <version>${spring-ai.version}</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
```

#### 1.2 Maven 仓库配置

添加 Spring Maven 仓库以支持里程碑版本：

```xml
<repositories>
    <repository>
        <id>spring-maven-public</id>
        <url>https://repo.spring.io/maven/public</url>
    </repository>
    <repository>
        <id>spring-milestones</id>
        <url>https://repo.spring.io/milestone</url>
    </repository>
</repositories>
```

#### 1.3 API 适配

适配 Spring AI 1.0.0-M4 API 变更：

- **EmbeddingClient** → **EmbeddingModel**
- **ChatClient** → **ChatModel**
- **PromptResponse** → **ChatResponse**
- `embed()` → `embedForResponse(List.of(content))`
- `call(prompt)` → `call(prompt)`

---

### 2. 向量数据库架构

#### 2.1 pgvector 扩展

使用 PostgreSQL 的 pgvector 扩展存储和查询向量：

```sql
-- 启用 pgvector 扩展
CREATE EXTENSION IF NOT EXISTS vector;

-- 验证扩展
SELECT * FROM pg_extension WHERE extname = 'vector';
```

#### 2.2 向量表设计

**商品向量表** (`commodity_vectors`):
```sql
CREATE TABLE nju_market.commodity_vectors (
    id BIGSERIAL PRIMARY KEY,
    commodity_id VARCHAR(50) NOT NULL UNIQUE,
    embedding vector(2000),  -- HNSW 索引最大支持 2000 维
    content TEXT NOT NULL,
    metadata JSONB,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- HNSW 索引（高性能向量搜索）
CREATE INDEX idx_commodity_vectors_embedding ON nju_market.commodity_vectors 
USING hnsw (embedding vector_cosine_ops);
```

**用户画像向量表** (`user_profile_vectors`):
```sql
CREATE TABLE nju_market.user_profile_vectors (
    id BIGSERIAL PRIMARY KEY,
    user_id VARCHAR(50) NOT NULL UNIQUE,
    embedding vector(2000),
    content TEXT NOT NULL,
    metadata JSONB,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_user_profile_vectors_embedding ON nju_market.user_profile_vectors 
USING hnsw (embedding vector_cosine_ops);
```

**对话向量表** (`conversation_vectors`):
```sql
CREATE TABLE nju_market.conversation_vectors (
    id BIGSERIAL PRIMARY KEY,
    conversation_id VARCHAR(50) NOT NULL,
    message_id VARCHAR(50) NOT NULL,
    user_id VARCHAR(50) NOT NULL,
    embedding vector(2000),
    content TEXT NOT NULL,
    role VARCHAR(20) NOT NULL,
    metadata JSONB,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(conversation_id, message_id)
);

CREATE INDEX idx_conversation_vectors_embedding ON nju_market.conversation_vectors 
USING hnsw (embedding vector_cosine_ops);
CREATE INDEX idx_conversation_vectors_user_id ON nju_market.conversation_vectors (user_id);
```

#### 2.3 向量维度限制

- **HNSW 索引限制**：PostgreSQL pgvector 的 HNSW 索引最多支持 2000 维
- **模型维度**：Doubao Embedding 模型生成 2560 维向量
- **解决方案**：向量截断到 2000 维（保留前 2000 维）

---

### 3. 商品向量化服务

#### 3.1 向量生成流程

```java
@Service
public class CommodityVectorServiceImpl implements CommodityVectorService {
    
    @Override
    @Async
    public void generateAndStoreVector(Commodity commodity) {
        // 1. 构建商品文本描述
        String content = buildCommodityContent(commodity);
        
        // 2. 调用 Embedding API 生成向量
        EmbeddingResponse response = embeddingModel.embedForResponse(List.of(content));
        float[] embedding = response.getResult().getOutput();
        
        // 3. 截断到 2000 维
        float[] truncated = Arrays.copyOf(embedding, Math.min(embedding.length, 2000));
        
        // 4. 存储到数据库
        storeVector(commodity.getCommodityId(), truncated, content, metadata);
    }
}
```

#### 3.2 向量存储

使用 `com.pgvector.PGvector` JDBC wrapper 存储向量：

```java
PGvector pgVector = new PGvector(embedding);
PreparedStatement ps = con.prepareStatement(sql);
ps.setObject(1, pgVector, java.sql.Types.OTHER);
```

#### 3.3 批量向量化

提供批量向量化接口，支持历史商品迁移：

```bash
POST /api/internal/commodity/vector/batch-generate?batchSize=100
```

---

### 4. AI 语义搜索

#### 4.1 搜索流程

```java
@Service
public class AISearchService {
    
    public List<Commodity> search(String query, String location, Integer limit) {
        // 1. 生成查询向量
        EmbeddingResponse response = embeddingModel.embedForResponse(List.of(query));
        float[] queryVector = response.getResult().getOutput();
        
        // 2. 截断到 2000 维
        float[] truncated = Arrays.copyOf(queryVector, Math.min(queryVector.length, 2000));
        
        // 3. 向量相似度搜索
        String sql = "SELECT cv.commodity_id, " +
                     "       1 - (cv.embedding <=> ?) as similarity " +
                     "FROM nju_market.commodity_vectors cv " +
                     "INNER JOIN nju_market.commodities c ON cv.commodity_id = c.commodity_id " +
                     "WHERE c.commodity_status = 'ON_SHELF' " +
                     "  AND c.seller_visibility = 'PUBLIC' " +
                     "  AND c.buyer_visibility = 'PUBLIC' " +
                     "  AND c.stock > 0 " +
                     "ORDER BY similarity DESC " +
                     "LIMIT ?";
        
        // 4. 返回商品列表
        return commodities;
    }
}
```

#### 4.2 相似度计算

- **余弦距离**：`embedding <=> query_vector`（值越小越相似）
- **余弦相似度**：`1 - (embedding <=> query_vector)`（值越大越相似）
- **排序**：按相似度降序排序，最相似的商品排在前面

#### 4.3 位置过滤

支持位置偏好过滤和排序：

```java
if (StringUtils.hasText(location)) {
    commodities = filterAndSortByLocation(commodityIds, commodities, location);
}
```

#### 4.4 API 接口

**公开接口**：
```bash
GET /api/public/commodity/ai-search?query=二手笔记本电脑&location=南京&limit=20
```

**内部接口**：
```bash
POST /api/internal/commodity/vector/batch-generate?batchSize=100
```

---

### 5. 用户画像向量化

#### 5.1 用户画像构建

基于用户基本信息、偏好、行为数据构建用户画像文本：

```java
private String buildUserProfileContent(String userId) {
    StringBuilder content = new StringBuilder();
    // 用户基本信息
    content.append("用户：").append(user.getUsername()).append(" ");
    // 用户偏好
    content.append("偏好：").append(preferences).append(" ");
    return content.toString();
}
```

#### 5.2 相似用户搜索

```java
public List<String> searchSimilarUsers(List<Double> queryVector, int limit) {
    // 向量相似度搜索
    String sql = "SELECT user_id, 1 - (embedding <=> ?) as similarity " +
                 "FROM nju_market.user_profile_vectors " +
                 "ORDER BY similarity DESC " +
                 "LIMIT ?";
    return userIds;
}
```

---

### 6. 对话向量化

#### 6.1 对话历史向量化

存储对话消息的向量，支持相关对话检索：

```java
@Async
public void storeConversationVector(String conversationId, String messageId, 
                                   String userId, String content, String role) {
    // 生成向量并存储
    EmbeddingResponse response = embeddingModel.embedForResponse(List.of(content));
    float[] embedding = response.getResult().getOutput();
    storeVector(conversationId, messageId, userId, embedding, content, role);
}
```

#### 6.2 相关对话检索

```java
public List<ConversationMessage> searchRelevantConversations(String query, 
                                                             String userId, 
                                                             int limit) {
    // 搜索该用户的相关对话历史
    String sql = "SELECT conversation_id, message_id, content, role, " +
                 "       1 - (embedding <=> ?) as similarity " +
                 "FROM nju_market.conversation_vectors " +
                 "WHERE user_id = ? " +
                 "ORDER BY similarity DESC " +
                 "LIMIT ?";
    return conversations;
}
```

---

### 7. 火山引擎 Doubao API 集成

#### 7.1 API 配置

**Commodity Service 配置** (`njumarket-service-commodity-dev.yml`):
```yaml
spring:
  ai:
    openai:
      api-key: ${DOUBAO_API_KEY:your-api-key}
      base-url: https://ark.cn-beijing.volces.com/api
      chat:
        options:
          model: doubao-1-5-pro-32k-250115
      embedding:
        options:
          model: doubao-embedding-text-240715
          dimensions: 2000
```

**Auth Service 配置** (`njumarket-service-auth-dev.yml`):
```yaml
spring:
  ai:
    openai:
      api-key: ${DOUBAO_API_KEY:your-api-key}
      base-url: https://ark.cn-beijing.volces.com/api
      embedding:
        options:
          model: doubao-embedding-text-240715
          dimensions: 2000
```

#### 7.2 API 路径适配

由于火山引擎 API 路径与 OpenAI 不完全兼容，创建了自定义配置类：

```java
@Configuration
public class DoubaoApiConfig {
    
    @Bean
    @Primary
    public OpenAiApi openAiApi(OpenAiApiProperties properties) {
        OpenAiApi api = new OpenAiApi(properties);
        // 使用反射添加拦截器，将 /v1/embeddings 重写为 /v3/embeddings
        return api;
    }
}
```

#### 7.3 环境变量

推荐使用环境变量配置 API Key：

```bash
# Windows
set DOUBAO_API_KEY=your-api-key-here

# Linux/Mac
export DOUBAO_API_KEY=your-api-key-here
```

---

### 8. 前端 AI 搜索

#### 8.1 AI 搜索按钮

在商品列表页面新增 AI 搜索按钮（金黄色按钮）：

```vue
<el-button 
    type="warning" 
    @click="handleAISearch"
    :loading="aiSearchLoading">
    AI搜索
</el-button>
```

#### 8.2 搜索实现

```javascript
const handleAISearch = async () => {
    aiSearchLoading.value = true;
    try {
        const response = await axios.get('/api/public/commodity/ai-search', {
            params: {
                query: searchQuery.value,
                location: locationFilter.value,
                limit: 20
            }
        });
        commodities.value = response.data.data;
    } finally {
        aiSearchLoading.value = false;
    }
};
```

---

## 🔧 技术实现细节

### 1. JDBC 向量处理

#### 1.1 pgvector JDBC Wrapper

使用官方 `com.pgvector:pgvector` JDBC wrapper：

```xml
<dependency>
    <groupId>com.pgvector</groupId>
    <artifactId>pgvector</artifactId>
    <version>0.1.4</version>
</dependency>
```

#### 1.2 向量参数绑定

```java
// 注册 pgvector 类型
PGvector.addVectorType(con);

// 设置 search_path
stmt.execute("SET search_path TO public, nju_market");

// 绑定向量参数
PGvector pgVector = new PGvector(embedding);
ps.setObject(1, pgVector, java.sql.Types.OTHER);
```

#### 1.3 SQL 查询优化

- **不使用类型转换**：直接使用 `?` 占位符，PostgreSQL 自动识别类型
- **ORDER BY 优化**：使用 `ORDER BY similarity DESC` 而不是 `ORDER BY embedding <=> ?`
- **LIMIT 处理**：直接拼接 LIMIT 值，不使用 `?` 占位符（PostgreSQL 不支持）

---

### 2. 向量维度处理

#### 2.1 维度截断

```java
// HNSW 索引最多支持 2000 维
int targetDimension = Math.min(embedding.length, 2000);
float[] truncated = new float[targetDimension];
System.arraycopy(embedding, 0, truncated, 0, targetDimension);
```

#### 2.2 数据库列类型

```sql
-- 向量列定义为 2000 维
embedding vector(2000)
```

---

### 3. 异步处理

#### 3.1 向量生成异步化

```java
@Async
public void generateAndStoreVector(Commodity commodity) {
    // 异步生成向量，不阻塞主流程
}
```

#### 3.2 批量处理

```java
@Transactional
public void batchGenerateVectors(int batchSize) {
    // 批量生成向量，提高效率
}
```

---

## 📊 性能优化

### 1. 向量索引

- **HNSW 索引**：高性能向量相似度搜索
- **索引参数**：`vector_cosine_ops`（余弦相似度操作符）

### 2. 查询优化

- **相似度计算**：使用 `<=>` 操作符（余弦距离）
- **排序优化**：直接使用 `similarity DESC`，避免重复计算
- **LIMIT 限制**：限制返回数量，减少数据传输

### 3. 缓存策略

- **向量缓存**：考虑缓存常用查询向量
- **结果缓存**：缓存热门搜索查询结果

---

## ⚠️ 当前限制与未来规划

### 当前实现（v3.0.0）

✅ **已实现**：
- 基于向量相似度的语义搜索
- 商品、用户画像、对话的向量化
- 简单的关键词匹配 Agent（`AIAgentService`）

❌ **未实现**：
- 真正的 AI Agent（Function Calling）
- 多轮对话上下文理解
- 智能工具调用
- 复杂的推理和规划能力

### 未来规划（v3.1.0+）

#### 1. Function Calling Agent

使用 Spring AI 的 Function Calling 功能，实现真正的 AI Agent：

```java
// 定义搜索工具
@Bean
public Function<SearchRequest, SearchResponse> searchCommoditiesFunction() {
    return new Function<>() {
        @Override
        public String getName() {
            return "searchCommodities";
        }
        
        @Override
        public SearchResponse apply(SearchRequest request) {
            return aiSearchService.search(request);
        }
    };
}
```

#### 2. 多轮对话

- 维护对话上下文
- 理解用户意图
- 支持澄清和追问

#### 3. 智能推荐

- 基于用户画像的个性化推荐
- 协同过滤推荐
- 混合推荐算法

---

## 🗄️ 数据库迁移

### 1. 创建向量表

执行以下 SQL 脚本：

```bash
# 商品向量表
psql -d njumarket -f database/vector-init.sql

# 用户画像和对话向量表
psql -d njumarket -f database/agent-vector-init.sql

# 更新向量维度到 2000
psql -d njumarket -f database/update-vector-dimension-to-2000.sql
```

### 2. 安装 pgvector 扩展

```sql
CREATE EXTENSION IF NOT EXISTS vector;
```

### 3. 验证安装

```sql
-- 检查扩展
SELECT * FROM pg_extension WHERE extname = 'vector';

-- 检查向量类型
SELECT * FROM pg_type WHERE typname = 'vector';

-- 检查操作符
SELECT oprname FROM pg_operator 
WHERE oprname IN ('<=>', '<->', '<#>');
```

---

## 🔍 配置说明

### 1. 环境变量

| 变量名 | 说明 | 必需 |
|--------|------|------|
| `DOUBAO_API_KEY` | 火山引擎 Doubao API 密钥 | ✅ |
| `DOUBAO_BASE_URL` | API 基础 URL | ❌ |
| `DOUBAO_CHAT_MODEL` | 聊天模型 | ❌ |
| `DOUBAO_EMBEDDING_MODEL` | 向量化模型 | ❌ |

### 2. 配置文件

**Commodity Service** (`njumarket-service-commodity-dev.yml`):
- Spring AI OpenAI 配置
- 向量维度配置（2000）
- API 基础 URL

**Auth Service** (`njumarket-service-auth-dev.yml`):
- Spring AI OpenAI 配置
- 向量维度配置（2000）

---

## 🧪 测试指南

### 1. 测试向量生成

```bash
# 批量生成向量
curl -X POST "http://localhost:8092/api/internal/commodity/vector/batch-generate?batchSize=10"
```

### 2. 测试 AI 搜索

```bash
# API 测试
curl -X GET "http://localhost:8092/api/public/commodity/ai-search?query=二手笔记本电脑&location=南京"

# 前端测试
访问首页，输入查询，点击"AI搜索"按钮
```

### 3. 验证向量数据

```sql
-- 检查向量数量
SELECT COUNT(*) FROM nju_market.commodity_vectors;

-- 检查向量维度
SELECT 
    commodity_id,
    array_length(string_to_array(embedding::text, ','), 1) as dimension
FROM nju_market.commodity_vectors
LIMIT 10;
```

---

## 📝 API 文档

### 公开接口

#### AI 搜索

```http
GET /api/public/commodity/ai-search
```

**参数**：
- `query` (String, 必需): 搜索查询文本
- `location` (String, 可选): 位置偏好
- `limit` (Integer, 可选): 返回数量限制（默认 20）

**响应**：
```json
{
  "success": true,
  "data": [
    {
      "commodityId": "COMMODITY_123",
      "title": "二手笔记本电脑",
      "price": 3000.00,
      ...
    }
  ]
}
```

### 内部接口

#### 批量生成向量

```http
POST /api/internal/commodity/vector/batch-generate
```

**参数**：
- `batchSize` (Integer, 可选): 批次大小（默认 100）

**响应**：
```json
{
  "success": true,
  "message": "批量向量化完成，处理了 100 个商品"
}
```

---

## 🐛 常见问题

### 1. pgvector 扩展未安装

**错误**：`ERROR: type "vector" does not exist`

**解决**：
```sql
CREATE EXTENSION IF NOT EXISTS vector;
```

### 2. 向量维度不匹配

**错误**：`ERROR: expected 2000 dimensions, not 2560`

**解决**：确保向量已截断到 2000 维

### 3. API Key 无效

**错误**：`401 Unauthorized`

**解决**：检查环境变量 `DOUBAO_API_KEY` 是否正确设置

### 4. 向量搜索无结果

**可能原因**：
- 商品向量尚未生成
- 搜索查询与商品内容不匹配
- 数据库索引未正确创建

**解决**：
```sql
-- 检查向量数量
SELECT COUNT(*) FROM nju_market.commodity_vectors;

-- 重新生成向量
POST /api/internal/commodity/vector/batch-generate
```

---

## 📚 相关文档

- [AI 配置文档](./AI_CONFIGURATION.md)
- [Agent 架构说明](./AGENT_ARCHITECTURE.md)
- [pgvector 安装指南](../database/install-pgvector.md)
- [向量表恢复指南](../database/recover-vector-tables.md)

---

## 🎯 版本总结

**NJUMarket v3.0.0** 成功引入了 AI 语义搜索能力，实现了：

1. ✅ **Spring AI 框架集成**：支持 Embedding 和 Chat 模型
2. ✅ **向量数据库**：PostgreSQL + pgvector，支持高性能向量搜索
3. ✅ **语义搜索**：基于向量相似度的自然语言搜索
4. ✅ **向量化服务**：商品、用户画像、对话的向量化
5. ✅ **火山引擎集成**：Doubao Embedding 和 Chat 模型

**当前限制**：
- 仅实现了简单的语义搜索，还不是真正的 AI Agent
- 缺少 Function Calling 能力
- 缺少多轮对话上下文理解

**下一步**：
- 实现基于 Function Calling 的 AI Agent
- 支持多轮对话和智能工具调用
- 实现个性化推荐系统

---

**版本完成时间**：2025 年 11 月  
**文档版本**：v3.0.0  
**维护者**：NJUMarket 开发团队

