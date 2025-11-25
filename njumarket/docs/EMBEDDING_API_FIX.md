# Embedding API 404 错误修复指南

## 问题描述

在批量生成商品向量时，出现以下错误：
```
org.springframework.ai.retry.NonTransientAiException: 404 - 
at org.springframework.ai.openai.api.OpenAiApi.embeddings(OpenAiApi.java:323)
```

## 问题原因

**DeepSeek API 可能不支持 embedding API**，或者 embedding API 的端点路径与 OpenAI 不兼容。

## 解决方案

### 方案一：使用 OpenAI Embedding API（推荐）

如果 DeepSeek 不支持 embedding，建议使用 OpenAI 的 embedding API：

1. **获取 OpenAI API Key**
   - 访问 https://platform.openai.com/
   - 注册/登录账号
   - 创建 API Key

2. **修改配置文件**

   **文件：`njumarket-config/src/main/resources/config-repo/njumarket-service-commodity-dev.yml`**
   
   ```yaml
   spring:
     ai:
       openai:
         api-key: ${OPENAI_API_KEY:sk-your-openai-api-key-here}
         base-url: https://api.openai.com/v1
         embedding:
           options:
             model: text-embedding-3-small
             dimensions: 1536
   ```

   **文件：`njumarket-config/src/main/resources/config-repo/njumarket-service-auth-dev.yml`**
   
   ```yaml
   spring:
     ai:
       openai:
         api-key: ${OPENAI_API_KEY:sk-your-openai-api-key-here}
         base-url: https://api.openai.com/v1
         embedding:
           options:
             model: text-embedding-3-small
             dimensions: 1536
   ```

3. **设置环境变量**
   ```bash
   export OPENAI_API_KEY=sk-your-openai-api-key-here
   ```

### 方案二：检查 DeepSeek 是否支持 Embedding

1. **验证 DeepSeek API 是否支持 embedding**
   - 访问 DeepSeek API 文档
   - 检查是否有 embedding 端点
   - 确认正确的 API 路径和模型名称

2. **如果 DeepSeek 支持 embedding，检查配置**
   - 确认 `base-url` 是否正确
   - 确认模型名称是否正确
   - 确认 API Key 是否有效

### 方案三：使用其他 Embedding 服务

可以考虑使用其他支持 embedding 的服务：
- **OpenAI**（推荐，稳定可靠）
- **Azure OpenAI**
- **Google Vertex AI**
- **本地 embedding 模型**（如使用 Ollama）

## 临时解决方案

如果暂时无法解决 API 问题，可以：

1. **跳过向量化**：注释掉向量化相关代码
2. **使用模拟数据**：创建测试向量数据
3. **降级处理**：在错误处理中记录日志，但不中断批量处理

## 验证修复

修复后，重新运行批量向量化：

```bash
curl -X POST "http://localhost:8092/api/internal/commodity/vector/batch-generate?batchSize=10"
```

检查日志，确认不再出现 404 错误。

## 注意事项

1. **API 费用**：OpenAI embedding API 按使用量计费，请注意成本
2. **API 限制**：注意 API 的速率限制和配额
3. **数据安全**：生产环境请使用环境变量或配置中心管理 API Key

## 相关链接

- [OpenAI Embedding API 文档](https://platform.openai.com/docs/guides/embeddings)
- [DeepSeek API 文档](https://platform.deepseek.com/api-docs/)
- [Spring AI Embedding 文档](https://docs.spring.io/spring-ai/reference/api/embeddings.html)

