# LangChain4j 迁移指南

## 📌 迁移概述

本项目已从 **Spring AI** 迁移到 **LangChain4j**，以解决 Spring AI 版本发布混乱、生态不稳定的问题。

> **迁移时间**：2025 年 11 月  
> **版本**：v3.2.0  
> **主要变更**：从 Spring AI 1.1.0 迁移到 LangChain4j 0.35.0

---

## ✨ 迁移原因

### Spring AI 的问题

1. **版本发布混乱**：从 1.0.0-M4 到 1.1.0，API 频繁变更
2. **生态不稳定**：依赖管理复杂，Maven 仓库配置繁琐
3. **文档不完善**：官方文档更新不及时，社区支持有限

### LangChain4j 的优势

1. **更稳定的 API**：API 设计更成熟，向后兼容性更好
2. **更好的生态**：支持 15+ LLM 提供商、20+ 向量数据库
3. **更简洁的配置**：配置更直观，不需要复杂的 URL 拦截器
4. **更好的文档**：中文文档完善，社区活跃

---

## 🔄 主要变更

### 1. 依赖替换

#### 父 POM (`njumarket/pom.xml`)

**之前（Spring AI）**：
```xml
<properties>
    <spring-ai.version>1.1.0</spring-ai.version>
</properties>

<dependencyManagement>
    <dependency>
        <groupId>org.springframework.ai</groupId>
        <artifactId>spring-ai-bom</artifactId>
        <version>${spring-ai.version}</version>
        <type>pom</type>
        <scope>import</scope>
    </dependency>
</dependencyManagement>
```

**现在（LangChain4j）**：
```xml
<properties>
    <langchain4j.version>0.35.0</langchain4j.version>
</properties>

<dependencyManagement>
    <dependency>
        <groupId>dev.langchain4j</groupId>
        <artifactId>langchain4j</artifactId>
        <version>${langchain4j.version}</version>
    </dependency>
    <dependency>
        <groupId>dev.langchain4j</groupId>
        <artifactId>langchain4j-open-ai</artifactId>
        <version>${langchain4j.version}</version>
    </dependency>
    <dependency>
        <groupId>dev.langchain4j</groupId>
        <artifactId>langchain4j-spring-boot-starter</artifactId>
        <version>${langchain4j.version}</version>
    </dependency>
</dependencyManagement>
```

#### 子模块依赖

**Commodity Service** 和 **Auth Service**：
- ❌ 移除：`spring-ai-openai-spring-boot-starter`
- ❌ 移除：`spring-ai-pgvector-store-spring-boot-starter`
- ✅ 添加：`langchain4j`
- ✅ 添加：`langchain4j-open-ai`
- ✅ 添加：`langchain4j-spring-boot-starter`

---

### 2. API 变更

#### Embedding API

**之前（Spring AI）**：
```java
EmbeddingResponse embeddingResponse = embeddingModel.embedForResponse(List.of(content));
float[] embeddingArray = embeddingResponse.getResult().getOutput();
```

**现在（LangChain4j）**：
```java
Embedding embedding = embeddingModel.embed(content).content();
float[] embeddingArray = embedding.vector();
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
String content = assistant.chat(userMessage);
```

#### Function Calling

**之前（Spring AI）**：
```java
@Bean
public FunctionCallback searchCommoditiesFunctionCallback() {
    return FunctionCallbackWrapper.builder(this)
        .withName("searchCommodities")
        .withDescription("...")
        .build();
}
```

**现在（LangChain4j）**：
```java
@Component
public class SearchCommoditiesTool {
    @Tool("搜索商品。根据用户的查询文本、位置偏好和数量限制，返回相关的商品列表。")
    public String searchCommodities(String query, String location, Integer limit) {
        // 实现搜索逻辑
    }
}
```

---

### 3. 配置变更

#### 配置文件

**之前（Spring AI）**：
```yaml
spring:
  ai:
    openai:
      api-key: ${DOUBAO_API_KEY}
      base-url: ${DOUBAO_BASE_URL:https://ark.cn-beijing.volces.com/api}
      chat:
        options:
          model: ${DOUBAO_CHAT_MODEL}
      embedding:
        options:
          model: ${DOUBAO_EMBEDDING_MODEL}
```

**现在（LangChain4j）**：
```yaml
langchain4j:
  open-ai:
    api-key: ${DOUBAO_API_KEY}
    base-url: ${DOUBAO_BASE_URL:https://ark.cn-beijing.volces.com/api/v3}
    chat-model: ${DOUBAO_CHAT_MODEL:doubao-1-5-pro-32k-250115}
    embedding-model: ${DOUBAO_EMBEDDING_MODEL:doubao-embedding-text-240715}
    timeout: 60s
```

#### 配置类

**之前（Spring AI）**：
- 需要 `DoubaoApiConfig` 通过反射修改 `OpenAiApi` 的 `RestClient`
- 需要拦截器修复 URL 路径（`/v1/embeddings` → `/v3/embeddings`）

**现在（LangChain4j）**：
- 直接配置 `base-url`，不需要 URL 拦截器
- 配置更简洁，代码更清晰

---

### 4. 代码变更

#### AISearchService

- ✅ 使用 `EmbeddingModel.embed()` 替代 `embedForResponse()`
- ✅ 使用 `Embedding.vector()` 获取向量数组

#### CommodityVectorServiceImpl

- ✅ 使用 LangChain4j Embedding API
- ✅ 保持向量截断逻辑（2000 维限制）

#### ConversationVectorServiceImpl

- ✅ 使用 LangChain4j Embedding API
- ✅ 保持向量搜索逻辑

#### UserProfileVectorServiceImpl

- ✅ 使用 LangChain4j Embedding API
- ✅ 保持用户画像向量化逻辑

#### AIAgentService

- ✅ 使用 `AiServices` 创建 AI Service
- ✅ 使用 `@Tool` 注解定义工具函数
- ✅ 自动处理 Function Calling

---

## 📊 功能对比

| 功能 | Spring AI | LangChain4j |
|------|-----------|-------------|
| **Embedding** | ✅ `embedForResponse()` | ✅ `embed()` |
| **Chat** | ✅ `ChatModel.call()` | ✅ `AiServices` |
| **Function Calling** | ✅ `FunctionCallbackWrapper` | ✅ `@Tool` 注解 |
| **配置复杂度** | ⚠️ 需要 URL 拦截器 | ✅ 直接配置 |
| **API 稳定性** | ❌ 版本间 API 变更频繁 | ✅ API 更稳定 |
| **文档质量** | ⚠️ 文档更新不及时 | ✅ 中文文档完善 |

---

## ⚠️ 注意事项

### 1. 向量维度

- Doubao Embedding 模型返回 2560 维向量
- HNSW 索引最多支持 2000 维
- 代码中会自动截断到 2000 维

### 2. API URL

- LangChain4j 直接使用 `base-url`，不需要额外的路径处理
- 配置 `base-url: https://ark.cn-beijing.volces.com/api/v3` 即可

### 3. Function Calling

- LangChain4j 使用 `@Tool` 注解定义工具函数
- `AiServices` 会自动注册工具并处理 Function Calling
- 不需要手动处理工具调用响应

---

## 🔧 迁移步骤

### 步骤 1：更新依赖

1. 更新父 POM 的 `langchain4j.version`
2. 更新子模块的依赖声明
3. 刷新 Maven 依赖：`mvn clean install -U`

### 步骤 2：更新配置

1. 更新 `application-dev.yml` 配置文件
2. 移除 `DoubaoApiConfig`（不再需要）
3. 添加 `LangChain4jConfig`（可选，用于自定义配置）

### 步骤 3：更新代码

1. 替换所有 `EmbeddingModel` 的使用
2. 替换所有 `ChatModel` 的使用
3. 更新 Function Calling 实现

### 步骤 4：测试

1. 测试向量生成功能
2. 测试 AI Agent 对话功能
3. 测试 Function Calling 功能

---

## 📚 相关文档

- [LangChain4j 官方文档](https://docs.langchain4j.info/)
- [LangChain4j 中文文档](https://langchain4j.cn/)
- [LangChain4j GitHub](https://github.com/langchain4j/langchain4j)

---

## ✅ 迁移检查清单

- [x] 更新父 POM 依赖
- [x] 更新子模块依赖
- [x] 创建 LangChain4j 配置类
- [x] 更新 AISearchService
- [x] 更新 CommodityVectorServiceImpl
- [x] 更新 ConversationVectorServiceImpl
- [x] 更新 UserProfileVectorServiceImpl（Auth Service）
- [x] 更新 UserProfileVectorServiceImpl（Commodity Service）
- [x] 更新 AIAgentService
- [x] 创建 SearchCommoditiesTool
- [x] 更新配置文件
- [x] 移除 DoubaoApiConfig
- [x] 移除 SearchCommoditiesFunction（Spring AI 版本）

---

**迁移完成时间**：2025 年 11 月  
**文档版本**：v3.2.0  
**维护者**：NJUMarket 开发团队

