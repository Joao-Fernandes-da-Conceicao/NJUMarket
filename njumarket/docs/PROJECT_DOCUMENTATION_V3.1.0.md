# 南大集市 NJUMarket v3.1.0 项目文档

## 📌 版本概述

**NJUMarket v3.1.0** 是"AI Agent 与用户画像"版本，核心目标是完成从 Spring AI 到 LangChain4j 的迁移，实现完整的用户画像生成和真正的 AI Agent 智能对话功能。

> **版本状态**：✅ 已完成  
> **完成时间**：2025 年 11 月  
> **主要成果**：LangChain4j 迁移、用户画像多数据源整合、AI Agent Function Calling、Cursor-like 对话机制

---

## ✨ 核心成果

| 模块 | 功能 | 状态 |
| --- | --- | --- |
| **LangChain4j 迁移** | 从 Spring AI 迁移到 LangChain4j，提升稳定性和易用性 | ✅ |
| **用户画像生成** | 整合 6 大数据源，生成个性化用户画像向量 | ✅ |
| **AI Agent 对话** | 基于 LangChain4j Function Calling 的智能对话 | ✅ |
| **Cursor-like 机制** | 实现对话摘要、相关历史检索、上下文管理 | ✅ |
| **前端 AI 助手** | 完整的 AI 助手聊天界面，支持多轮对话和商品推荐 | ✅ |
| **异常处理优化** | 优雅处理 broken pipe 等 IO 异常 | ✅ |

---

## 🔄 从 Spring AI 到 LangChain4j 迁移

### 迁移原因

1. **版本稳定性**：Spring AI 版本发布混乱，API 变更频繁，生态不稳定
2. **API 易用性**：LangChain4j API 更简洁，文档更完善
3. **Function Calling**：LangChain4j 的 `@Tool` 注解和 `AiServices` 更易用
4. **社区支持**：LangChain4j 有更好的中文文档和社区支持

### 依赖变更

#### 父 POM 配置

```xml
<properties>
    <langchain4j.version>0.35.0</langchain4j.version>
</properties>

<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>dev.langchain4j</groupId>
            <artifactId>langchain4j-bom</artifactId>
            <version>${langchain4j.version}</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
```

#### 子模块依赖

**Commodity Service** 和 **Auth Service**：
- ❌ 移除：`spring-ai-openai-spring-boot-starter`
- ❌ 移除：`spring-ai-pgvector-store-spring-boot-starter`
- ✅ 添加：`langchain4j-spring-boot-starter`
- ✅ 添加：`langchain4j-open-ai`
- ✅ 添加：`langchain4j-pgvector`（Commodity Service）

### API 变更

#### Embedding API

**之前（Spring AI）**：
```java
EmbeddingResponse embeddingResponse = embeddingModel.embedForResponse(List.of(content));
float[] embeddingArray = embeddingResponse.getResult().getOutput();
```

**现在（LangChain4j）**：
```java
Embedding embedding = embeddingModel.embed(content).content();
float[] embeddingArray = embedding.vector().asFloatArray();
```

#### Chat API

**之前（Spring AI）**：
```java
Prompt prompt = Prompt.builder()
    .withMessages(messages)
    .withFunctionCallbacks(List.of(functionCallback))
    .build();
ChatResponse response = chatModel.call(prompt);
String content = response.getResult().getOutput().getContent();
```

**现在（LangChain4j）**：
```java
// 使用 AiServices 自动处理 Function Calling
ShoppingAssistant assistant = AiServices.builder(ShoppingAssistant.class)
    .chatLanguageModel(chatLanguageModel)
    .tools(searchCommoditiesTool)
    .build();
String content = assistant.chat(memoryId, userMessage);
```

### 配置变更

#### LangChain4j 配置

**文件**：`njumarket-service-commodity-dev.yml`

```yaml
langchain4j:
  open-ai:
    api-key: ${DOUBAO_API_KEY:your-api-key}
    base-url: https://ark.cn-beijing.volces.com/api/v3
    chat-model: doubao-seed-1-6-250615
    embedding-model: doubao-embedding-text-240715
    timeout: 60s
```

---

## 👤 用户画像生成

### 功能概述

用户画像生成功能整合了多个数据源，为每个用户生成个性化的向量表示，用于商品推荐和相似用户搜索。

### 数据源整合

用户画像生成会整合以下 **6 大数据源**：

1. **用户基本信息**：用户名、昵称、VIP等级、信用分、买家评分、卖家评分
2. **用户地址**：默认地址或常用地址（省市区+详细地址）
3. **发布的商品**：用户发布的商品标题、分类、价格等信息（最多50个）
4. **订单信息**：作为买家和卖家的订单数量统计
5. **AI聊天记录**：与AI助手的对话记录（从 `conversation_vectors` 表获取，最多30条）
6. **用户聊天记录**：与其他用户的聊天记录（从 Message 服务获取，最多30条）

### 实现逻辑

#### 1. 数据源获取

```java
// 1. 用户基本信息
User user = userRepository.findById(userId).orElse(null);
UserProfile profile = userProfileRepository.findByUserId(userId).orElse(null);

// 2. 用户地址
List<UserAddress> addresses = userAddressRepository.findByUserId(userId);

// 3. 发布的商品（通过 Feign Client）
Result result = commodityClient.getUserCommodities(userId, 1, 50, null);

// 4. 订单信息（通过 Feign Client）
Result buyerOrders = orderClient.getUserOrderStats(userId, "buyer");
Result sellerOrders = orderClient.getUserOrderStats(userId, "seller");

// 5. AI聊天记录（通过 Feign Client）
Result aiChatHistory = commodityClient.getAIChatHistory(userId, 30);

// 6. 用户聊天记录（通过 Feign Client）
Result userChatHistory = messageClient.getUserChatHistory(userId, 30);
```

#### 2. 文本构建

所有数据源会被整合成一个文本描述：

```
用户名：testuser。昵称：测试用户。VIP等级：NORMAL。信用分：100。买家评分：5.0。卖家评分：5.0。
常用地址：江苏省南京市鼓楼区南京大学。
发布的商品：二手笔记本电脑（电子产品），价格2000元；二手自行车（交通工具），价格300元；共5个商品。
订单情况：作为买家3单，作为卖家2单。
AI聊天偏好：想要买一台笔记本电脑；价格不要太贵；共10条聊天记录。
聊天偏好：你好，这个商品还在吗？；价格可以便宜点吗？；共20条聊天记录。
```

#### 3. 向量生成与存储

```java
// 生成向量
Embedding embedding = embeddingModel.embed(content).content();
float[] embeddingArray = embedding.vector().asFloatArray();

// 截断到 2000 维（HNSW 索引限制）
float[] truncatedArray = Arrays.copyOf(embeddingArray, Math.min(embeddingArray.length, 2000));

// 存储到数据库
storeVector(userId, truncatedArray, content, metadata);
```

### 空值处理

系统对空值进行了完善的容错处理：

- 如果用户没有任何商品，会跳过商品信息
- 如果用户没有任何订单，会跳过订单信息
- 如果用户没有地址，会跳过地址信息
- 如果用户没有聊天记录，会跳过聊天记录
- 即使所有数据都为空，也会生成基于用户ID的基本画像

### API 接口

#### 生成用户画像向量

**接口地址**：`POST /api/internal/user/{userId}/generate-profile-vector`

**说明**：单次 curl 即可触发用户画像向量生成，异步处理，整合所有数据源。

**使用示例**：
```bash
curl -X POST "http://localhost:8091/api/internal/user/USER_123/generate-profile-vector"
```

**响应示例**：
```json
{
  "success": true,
  "errorMsg": null,
  "data": null,
  "total": null,
  "message": "用户画像向量生成成功（异步处理中）"
}
```

#### 查询用户画像向量

**接口地址**：`GET /api/internal/user/{userId}/profile-vector`

**使用示例**：
```bash
curl "http://localhost:8091/api/internal/user/USER_123/profile-vector"
```

### conversation_vectors 完整利用

`conversation_vectors` 表存储了用户与 AI 助手的对话历史，包括：
- `conversation_id`：对话ID
- `message_id`：消息ID
- `user_id`：用户ID
- `content`：消息内容
- `role`：消息角色（user/assistant）
- `embedding`：消息向量
- `metadata`：元数据（JSON格式）
- `created_at`：创建时间

在用户画像生成时，系统会：
1. 从 `conversation_vectors` 表中查询用户的所有AI聊天记录
2. 按时间倒序排列（最新的在前）
3. 提取用户消息（role = "user"）用于画像生成
4. 最多提取30条用户消息，每条消息最多50个字符

---

## 🤖 AI Agent 处理逻辑

### 架构设计

AI Agent 采用 **Cursor-like 机制**，结合以下组件：

1. **内存消息窗口**：`MessageWindowChatMemory`，保留最近 15 条消息
2. **对话摘要**：当消息超过 20 条时，自动生成摘要
3. **相关历史检索**：从向量存储中检索最相关的 5 条历史对话
4. **用户画像上下文**：动态注入用户画像信息
5. **Function Calling**：自动调用 `searchCommodities` 工具搜索商品

### 核心流程

#### 1. 对话处理流程

```java
public ChatResult chat(String userMessage, String userId, String conversationId) {
    // 1. 获取用户画像信息（用于个性化）
    String userProfileContext = getUserProfileContext(userId);
    
    // 2. 搜索相关对话历史（从向量存储中）
    List<ConversationMessage> relevantHistory = 
        conversationVectorService.searchRelevantConversations(userMessage, userId, 5);
    
    // 3. 获取内存中的 ChatMemory
    ChatMemory chatMemory = chatMemoryMap.computeIfAbsent(memoryId, 
        id -> MessageWindowChatMemory.withMaxMessages(15));
    
    // 4. 检查是否需要生成摘要
    if (memoryMessages.size() >= 20) {
        conversationSummary = summarizeOldMessages(memoryMessages, 15);
    }
    
    // 5. 保存上下文信息
    ConversationContext context = new ConversationContext();
    context.userProfileContext = userProfileContext;
    context.conversationSummary = conversationSummary;
    context.relevantHistory = relevantHistory;
    contextMap.put(memoryId, context);
    
    // 6. 调用 AI Service（自动处理 Function Calling）
    String assistantReply = shoppingAssistant.chat(memoryId, userMessage);
    
    // 7. 获取工具调用返回的商品列表
    List<Commodity> recommendedCommodities = searchCommoditiesTool.getLastSearchResults();
    
    // 8. 存储对话历史到向量数据库（异步）
    conversationVectorService.storeConversationVector(conversationId, messageId, userId, userMessage, "user");
    conversationVectorService.storeConversationVector(conversationId, messageId, userId, assistantReply, "assistant");
    
    return new ChatResult(assistantReply, recommendedCommodities);
}
```

#### 2. 系统提示词构建

系统提示词是动态构建的，包含以下部分：

```java
private String buildEnhancedSystemPrompt(String userId, String userProfileContext, 
                                        String conversationSummary, 
                                        List<ConversationMessage> relevantHistory) {
    StringBuilder prompt = new StringBuilder();
    
    // 1. 基础系统提示词
    prompt.append("你是一个智能购物助手，帮助用户在南大集市（二手交易平台）上找到合适的商品。\n\n");
    prompt.append("你的职责：\n");
    prompt.append("1. 理解用户的购物需求\n");
    prompt.append("2. 当用户想要查找、购买或了解商品时，使用 searchCommodities 工具搜索商品\n");
    prompt.append("3. 基于搜索结果推荐合适的商品\n");
    
    // 2. 用户画像上下文
    if (StringUtils.hasText(userProfileContext)) {
        prompt.append("\n\n=== 用户画像信息 ===\n");
        prompt.append(userProfileContext);
        prompt.append("\n请根据用户的偏好和需求提供个性化推荐。\n");
    }
    
    // 3. 对话摘要（类似 Cursor 的 summarized text）
    if (StringUtils.hasText(conversationSummary)) {
        prompt.append("\n\n=== 之前的对话摘要 ===\n");
        prompt.append(conversationSummary);
        prompt.append("\n这是之前对话的摘要，请参考这些信息理解上下文。\n");
    }
    
    // 4. 相关历史对话
    if (relevantHistory != null && !relevantHistory.isEmpty()) {
        prompt.append("\n\n=== 相关的历史对话 ===\n");
        for (ConversationMessage msg : relevantHistory) {
            prompt.append(String.format("[%s]: %s\n", 
                "user".equals(msg.getRole()) ? "用户" : "助手", 
                msg.getContent()));
        }
        prompt.append("\n这些是相关的历史对话，请参考以更好地理解用户的需求。\n");
    }
    
    return prompt.toString();
}
```

#### 3. Function Calling 实现

使用 LangChain4j 的 `@Tool` 注解定义工具：

```java
@Component
@RequiredArgsConstructor
public class SearchCommoditiesTool {
    
    private final AISearchService aiSearchService;
    private List<Commodity> lastSearchResults = new ArrayList<>();
    
    @Tool("搜索商品。当用户想要查找、购买或了解商品时使用此工具。")
    public SearchResponse searchCommodities(
        @P("搜索关键词，从用户描述中提取") String query,
        @P("位置偏好，如果用户提到位置则使用") String location) {
        
        // 执行向量搜索
        List<Commodity> commodities = aiSearchService.search(query, location, 20);
        
        // 保存搜索结果
        lastSearchResults = commodities;
        
        // 构建响应
        return new SearchResponse(commodities.size(), 
            commodities.stream()
                .map(c -> c.getTitle() + " - " + c.getPrice())
                .collect(Collectors.joining(", ")));
    }
}
```

#### 4. 对话摘要生成

当消息数量超过阈值时，自动生成摘要：

```java
private String summarizeOldMessages(List<ChatMessage> messages, int keepRecent) {
    // 需要总结的消息数量
    int toSummarize = messages.size() - keepRecent;
    List<ChatMessage> oldMessages = messages.subList(0, toSummarize);
    
    // 构建摘要提示词
    StringBuilder conversationText = new StringBuilder();
    for (ChatMessage msg : oldMessages) {
        String role = msg instanceof UserMessage ? "用户" : "助手";
        String content = msg instanceof UserMessage ? 
            ((UserMessage) msg).singleText() : 
            ((AiMessage) msg).text();
        conversationText.append(String.format("[%s]: %s\n", role, content));
    }
    
    String summaryPrompt = String.format(
        "以下是用户与购物助手的早期对话记录，请用简洁的语言总结主要内容和用户的需求偏好：\n\n%s\n\n" +
        "请用一段话（不超过200字）总结这段对话的主要内容、用户的需求和偏好。",
        conversationText.toString()
    );
    
    // 使用 LLM 生成摘要
    Response<AiMessage> response = chatLanguageModel.generate(UserMessage.from(summaryPrompt));
    return response.content().text().trim();
}
```

### 关键参数

| 参数 | 值 | 说明 |
| --- | --- | --- |
| `MAX_MEMORY_MESSAGES` | 15 | 内存中保留的最近消息数 |
| `MAX_SUMMARY_TRIGGER` | 20 | 超过此数量时触发摘要 |
| `MAX_CONVERSATION_HISTORY` | 5 | 从向量存储中检索的相关历史数量 |
| `MAX_SEARCH_RESULTS` | 5 | 最多返回5个商品 |

### API 接口

#### AI Agent 对话

**接口地址**：`POST /api/user/commodity/ai-agent/chat`

**参数**：
- `message`：用户消息（必填）
- `conversationId`：对话ID（可选，用于多轮对话）

**响应示例**：
```json
{
  "success": true,
  "errorMsg": null,
  "data": {
    "reply": "我为您找到了以下商品：...",
    "conversationId": "ai_chat_1763799184789_ku3gc5dvz",
    "recommendedCommodities": [
      {
        "commodityId": "COMMODITY_123",
        "title": "二手笔记本电脑",
        "price": 2000.0,
        ...
      }
    ],
    "hasRecommendations": true
  },
  "message": "AI Agent 回复成功"
}
```

#### AI Agent 智能搜索

**接口地址**：`GET /api/user/commodity/ai-agent/search`

**参数**：
- `query`：搜索查询（必填）
- `conversationId`：对话ID（可选，用于上下文理解）

**响应示例**：
```json
{
  "success": true,
  "errorMsg": null,
  "data": {
    "commodities": [...],
    "explanation": "根据您的需求，我为您找到了以下商品...",
    "originalQuery": "我想买一个性价比高的手机",
    "enhancedQuery": "性价比高的手机，价格合理，功能齐全",
    "total": 10
  },
  "message": "AI Agent 智能搜索成功"
}
```

---

## 🎨 前端 AI 助手

### 功能特性

1. **聊天界面**：完整的聊天UI，支持用户和AI消息显示
2. **多轮对话**：支持 `conversationId` 管理多轮对话
3. **商品推荐**：AI 推荐的商品以卡片形式展示
4. **加载状态**：显示"正在输入"动画
5. **超时处理**：前端超时时间设置为 30 秒

### 实现细节

#### 1. 组件结构

```vue
<template>
  <div class="ai-chat-container">
    <!-- 聊天头部 -->
    <div class="chat-header">...</div>
    
    <!-- 消息列表 -->
    <div class="messages-list">
      <div v-for="message in messages" :key="index" class="message-item">
        <!-- 用户消息或AI消息 -->
        <div class="message-bubble">{{ message.content }}</div>
        
        <!-- 推荐商品卡片 -->
        <div v-if="message.recommendedCommodities" class="recommended-commodities">
          <CommodityCard v-for="commodity in message.recommendedCommodities" 
                         :key="commodity.commodityId" 
                         :commodity="commodity" />
        </div>
      </div>
    </div>
    
    <!-- 输入区域 -->
    <div class="input-area">
      <UnifiedInput v-model="inputMessage" @keydown.enter="sendMessage" />
      <UnifiedButton @click="sendMessage">发送</UnifiedButton>
    </div>
  </div>
</template>
```

#### 2. API 调用

```javascript
// 发送消息
const sendMessage = async () => {
  const response = await commodityAPI.aiAgentChat(userMessage, conversationId.value);
  
  if (response.success && response.data) {
    // 添加 AI 回复
    messages.value.push({
      role: 'assistant',
      content: response.data.reply,
      recommendedCommodities: response.data.recommendedCommodities || [],
      timestamp: Date.now()
    });
    
    // 更新 conversationId
    if (response.data.conversationId) {
      conversationId.value = response.data.conversationId;
    }
  }
};
```

#### 3. 超时配置

```javascript
// API 配置（30 秒超时）
aiAgentChat: (message, conversationId) => 
  api.post('/user/commodity/ai-agent/chat', null, { 
    params: { message, conversationId },
    timeout: 30000 // 30 秒超时
  })
```

---

## 🔧 异常处理优化

### Broken Pipe 处理

由于 AI Agent 处理时间较长，可能出现客户端提前关闭连接的情况。系统通过以下方式优雅处理：

#### 1. 服务器超时配置

```yaml
server:
  port: 8092
  connection-timeout: 60000  # 60 秒
  tomcat:
    connection-timeout: 60000
    keep-alive-timeout: 60000
```

#### 2. 全局异常处理器

在 `GlobalExceptionHandler` 中添加 `IOException` 处理：

```java
@ExceptionHandler(IOException.class)
public Result handleIOException(IOException e) {
    String errorMessage = e.getMessage();
    if (errorMessage != null && 
        (errorMessage.contains("Broken pipe") || 
         errorMessage.contains("Connection reset by peer") ||
         errorMessage.contains("Connection closed"))) {
        // 客户端已关闭连接，静默处理
        log.debug("客户端连接已关闭: {}", errorMessage);
        return null; // 返回 null，Spring MVC 会跳过响应写入
    }
    
    // 其他 IO 异常正常记录
    log.error("IO异常: {}", errorMessage, e);
    return Result.fail("网络错误，请稍后重试");
}
```

---

## 📊 技术栈

| 技术 | 版本 | 用途 |
| --- | --- | --- |
| **LangChain4j** | 0.35.0 | AI 框架，支持 Function Calling |
| **Doubao API** | - | 火山引擎 AI 服务（Chat + Embedding） |
| **PostgreSQL** | - | 关系型数据库 |
| **pgvector** | - | 向量数据库扩展 |
| **Vue.js** | 3.x | 前端框架 |
| **Element Plus** | - | UI 组件库 |

---

## 🚀 部署与配置

### 环境变量

```bash
# Doubao API 配置
export DOUBAO_API_KEY=your-api-key
export DOUBAO_BASE_URL=https://ark.cn-beijing.volces.com/api/v3
export DOUBAO_CHAT_MODEL=doubao-seed-1-6-250615
export DOUBAO_EMBEDDING_MODEL=doubao-embedding-text-240715
```

### 数据库配置

确保 PostgreSQL 已安装 pgvector 扩展：

```sql
CREATE EXTENSION IF NOT EXISTS vector;
```

### 向量表结构

- `commodity_vectors`：商品向量表
- `user_profile_vectors`：用户画像向量表
- `conversation_vectors`：对话向量表

---

## 📝 测试示例

### 用户画像生成

```bash
# 生成用户画像
curl -X POST "http://localhost:8091/api/internal/user/USER_123/generate-profile-vector"

# 查询用户画像
curl "http://localhost:8091/api/internal/user/USER_123/profile-vector"
```

### AI Agent 对话

```bash
# 发送消息
curl -X POST "http://localhost:8080/api/user/commodity/ai-agent/chat" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -d "message=我想买一个二手笔记本电脑&conversationId=ai_chat_123"
```

### AI Agent 搜索

```bash
# 智能搜索
curl "http://localhost:8080/api/user/commodity/ai-agent/search?query=我想买一个性价比高的手机" \
  -H "Authorization: Bearer YOUR_TOKEN"
```

---

## 🔮 后续优化方向

1. **用户画像增量更新**：只更新变化的数据源，减少计算量
2. **对话摘要优化**：使用更智能的摘要算法，保留关键信息
3. **商品推荐算法**：结合用户画像和商品向量，提供更精准的推荐
4. **多模态支持**：支持图片、语音等多模态输入
5. **性能优化**：缓存用户画像和对话摘要，减少重复计算

---

## 📚 相关文档

- [用户画像向量生成功能](./USER_PROFILE_VECTOR_GENERATION.md)
- [LangChain4j 迁移文档](./LANGCHAIN4J_MIGRATION.md)
- [AI 功能测试文档](./AI_FEATURE_TESTING.md)
- [项目文档 v3.0.0](./PROJECT_DOCUMENTATION_V3.0.0.md)

---

**文档版本**：v3.1.0  
**最后更新**：2025 年 11 月  
**维护者**：NJUMarket 开发团队

