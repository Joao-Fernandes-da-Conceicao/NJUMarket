# Spring AI 1.1.0 升级指南

## 📌 升级概述

本项目已从 Spring AI 1.0.0-M4 升级到 **Spring AI 1.1.0 正式版**，并实现了基于 Function Calling 的真正 AI Agent 搜索功能。

> **升级时间**：2025 年 11 月  
> **版本**：v3.1.0  
> **主要变更**：Spring AI 1.1.0 正式版、Function Calling AI Agent、用户画像完善

---

## ✨ 主要变更

### 1. 依赖版本升级

#### 父 POM (`njumarket/pom.xml`)

```xml
<properties>
    <spring-ai.version>1.1.0</spring-ai.version>  <!-- 从 1.0.0-M4 升级 -->
</properties>
```

#### Maven 仓库配置

- ✅ **移除** `spring-milestones` 仓库（正式版已发布到 Maven Central）
- ✅ **保留** `spring-maven-public` 仓库（备用）

#### 子模块依赖优化

**Commodity Service** (`njumarket-service-commodity/pom.xml`):
- ✅ **移除** `spring-ai-core` 依赖（已包含在 `spring-ai-openai-spring-boot-starter` 中）
- ✅ **保留** `spring-ai-openai-spring-boot-starter`
- ✅ **保留** `spring-ai-pgvector-store-spring-boot-starter`

**Auth Service** (`njumarket-service-auth/pom.xml`):
- ✅ **移除** `spring-ai-core` 依赖
- ✅ **保留** `spring-ai-openai-spring-boot-starter`

---

### 2. API 兼容性

Spring AI 1.1.0 与 1.0.0-M4 在核心 API 上保持兼容，以下 API 无需修改：

- ✅ `EmbeddingModel.embedForResponse(List<String>)` - 保持不变
- ✅ `ChatModel.call(Prompt)` - 保持不变
- ✅ `ChatResponse.getResult().getOutput().getContent()` - 保持不变
- ✅ `EmbeddingResponse.getResult().getOutput()` - 保持不变

---

### 3. Function Calling 实现

#### 3.1 创建搜索工具函数

**文件**：`njumarket-service-commodity/src/main/java/com/njumarket/commodity/vector/function/SearchCommoditiesFunction.java`

```java
@Configuration
@RequiredArgsConstructor
public class SearchCommoditiesFunction implements Function<SearchRequest, SearchResponse> {
    
    private final AISearchService aiSearchService;
    
    @Override
    public SearchResponse apply(SearchRequest request) {
        // 执行搜索逻辑
        List<Commodity> commodities = aiSearchService.search(
            request.query(), 
            request.location(), 
            request.limit() != null ? request.limit() : 20
        );
        return new SearchResponse(commodities);
    }
    
    @Bean
    public FunctionCallback searchCommoditiesFunctionCallback() {
        return FunctionCallbackWrapper.builder((Function<SearchRequest, SearchResponse>) this)
            .withName("searchCommodities")
            .withDescription("搜索商品。根据用户的查询文本、位置偏好和数量限制，返回相关的商品列表。")
            .withResponseConverter((response) -> {
                // 将搜索结果转换为字符串格式
                SearchResponse searchResponse = (SearchResponse) response;
                // ... 格式化逻辑
                return formattedResult;
            })
            .build();
    }
}
```

#### 3.2 更新 AIAgentService

**文件**：`njumarket-service-commodity/src/main/java/com/njumarket/commodity/vector/AIAgentService.java`

**主要变更**：

1. **注入 FunctionCallback**：
```java
private final FunctionCallback searchCommoditiesFunctionCallback;
```

2. **在 Prompt 中注册 Function**：
```java
Prompt prompt = Prompt.builder()
    .withMessages(messages)
    .withFunctionCallbacks(List.of(searchCommoditiesFunctionCallback))
    .build();
```

3. **更新系统提示词**：
```java
private String buildSystemPrompt(String userId) {
    // 明确告诉 Agent 可以使用 searchCommodities 工具
    // 说明何时应该使用工具
    // ...
}
```

4. **处理工具调用**：
```java
// Spring AI 1.1.0 的 FunctionCallbackWrapper 会自动处理工具调用
// 如果 LLM 决定调用工具，FunctionCallback 会自动执行并将结果返回给 LLM
ChatResponse response = chatModel.call(prompt);
String assistantReply = processFunctionCalls(response, messages, prompt);
```

---

### 4. 用户画像功能

用户画像功能已在 v3.0.0 中实现，v3.1.0 中保持不变：

- ✅ **用户画像向量化**：`UserProfileVectorServiceImpl.generateAndStoreUserProfileVector()`
- ✅ **相似用户搜索**：`UserProfileVectorServiceImpl.searchSimilarUsers()`
- ✅ **向量存储**：使用 `com.pgvector.PGvector` JDBC wrapper

---

### 5. Doubao API 配置

Doubao API 配置保持不变，与 Spring AI 1.1.0 兼容：

**配置文件** (`njumarket-service-commodity-dev.yml`):
```yaml
spring:
  ai:
    openai:
      api-key: ${DOUBAO_API_KEY:your-api-key}
      base-url: ${DOUBAO_BASE_URL:https://ark.cn-beijing.volces.com/api}
      chat:
        options:
          model: ${DOUBAO_CHAT_MODEL:doubao-1-5-pro-32k-250115}
      embedding:
        options:
          model: ${DOUBAO_EMBEDDING_MODEL:doubao-embedding-text-240715}
          dimensions: 2560
```

**自定义配置类** (`DoubaoApiConfig.java`):
- ✅ 通过反射修改 `OpenAiApi` 的 `RestClient`
- ✅ 添加拦截器将 `/v1/embeddings` 替换为 `/v3/embeddings`
- ✅ 与 Spring AI 1.1.0 兼容

---

## 🔧 升级步骤

### 步骤 1：更新依赖

1. **更新父 POM**：
```bash
# 已更新 spring-ai.version 到 1.1.0
```

2. **刷新 Maven 依赖**：
```bash
mvn clean install -U
```

### 步骤 2：验证编译

```bash
mvn clean compile
```

### 步骤 3：测试功能

1. **测试向量生成**：
```bash
curl -X POST "http://localhost:8092/api/internal/commodity/vector/batch-generate?batchSize=10"
```

2. **测试 AI Agent 对话**：
```bash
curl -X POST "http://localhost:8092/api/public/commodity/ai-agent/chat" \
  -H "Content-Type: application/json" \
  -d '{
    "message": "我想买一台二手笔记本电脑",
    "userId": "USER_123",
    "conversationId": "CONV_123"
  }'
```

3. **测试 Function Calling**：
- Agent 应该能够自动识别搜索意图
- 自动调用 `searchCommodities` 工具
- 基于搜索结果生成回复

---

## ⚠️ 注意事项

### 1. Maven 依赖刷新

升级后需要刷新 Maven 依赖，确保下载 Spring AI 1.1.0 版本：

```bash
mvn clean install -U
```

### 2. IDE 缓存清理

如果 IDE 显示编译错误，可能需要：
- 清理 IDE 缓存
- 重新导入 Maven 项目
- 刷新依赖

### 3. Function Calling 支持

确保使用的 Chat 模型支持 Function Calling：
- ✅ Doubao `doubao-1-5-pro-32k-250115` 支持 Function Calling
- ✅ 其他兼容 OpenAI API 的模型通常也支持

### 4. API 兼容性

Spring AI 1.1.0 与 1.0.0-M4 在核心 API 上兼容，但建议：
- 检查所有 Spring AI 相关的导入
- 验证方法调用是否正确
- 测试所有 AI 相关功能

---

## 📊 功能对比

| 功能 | v3.0.0 (1.0.0-M4) | v3.1.0 (1.1.0) |
|------|-------------------|----------------|
| **语义搜索** | ✅ 简单向量搜索 | ✅ 向量搜索（保持不变） |
| **AI Agent** | ❌ 关键词匹配 | ✅ Function Calling |
| **工具调用** | ❌ 硬编码 | ✅ 自动决策 |
| **多轮对话** | ⚠️ 基础支持 | ✅ 完整支持 |
| **用户画像** | ✅ 已实现 | ✅ 保持不变 |

---

## 🐛 已知问题

### 1. 编译错误（IDE 显示）

如果 IDE 显示 `EmbeddingModel cannot be resolved` 等错误：
- **原因**：IDE 缓存未更新
- **解决**：刷新 Maven 依赖，清理 IDE 缓存

### 2. Function Calling 不工作

如果 Function Calling 不工作：
- **检查**：Chat 模型是否支持 Function Calling
- **检查**：FunctionCallback 是否正确注册
- **检查**：系统提示词是否明确说明工具使用规则

---

## 📚 相关文档

- [Spring AI 1.1.0 官方文档](https://docs.spring.io/spring-ai/reference/)
- [Function Calling 指南](./AGENT_ARCHITECTURE.md)
- [AI 配置文档](./AI_CONFIGURATION.md)
- [v3.0.0 项目文档](./PROJECT_DOCUMENTATION_V3.0.0.md)

---

## ✅ 升级检查清单

- [x] 更新 `spring-ai.version` 到 1.1.0
- [x] 移除 `spring-ai-core` 依赖（已包含在 starter 中）
- [x] 移除 `spring-milestones` 仓库
- [x] 创建 `SearchCommoditiesFunction` 工具函数
- [x] 更新 `AIAgentService` 使用 Function Calling
- [x] 更新系统提示词支持工具调用
- [x] 验证 Doubao API 配置兼容性
- [x] 测试向量生成功能
- [x] 测试 AI Agent 对话功能
- [x] 测试 Function Calling 功能

---

**升级完成时间**：2025 年 11 月  
**文档版本**：v3.1.0  
**维护者**：NJUMarket 开发团队

