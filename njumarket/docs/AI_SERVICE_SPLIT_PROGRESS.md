# AI服务拆分进度

## ✅ 已完成的工作

### 1. 创建AI服务基础结构
- ✅ `njumarket-service-ai/pom.xml` - 依赖配置（LangChain4j、Spring Cloud、PostgreSQL、pgvector等）
- ✅ `njumarket-service-ai/src/main/java/com/njumarket/ai/AIServiceApplication.java` - 主启动类
- ✅ `njumarket-service-ai/src/main/resources/application.yml` - 配置文件（端口8097）

### 2. 配置类
- ✅ `config/SecurityConfig.java` - Spring Security配置
- ✅ `config/LangChain4jConfig.java` - LangChain4j配置（Chat模型、Embedding模型）

### 3. 实体和Repository
- ✅ `entity/AIConversation.java` - AI对话会话实体
- ✅ `repository/AIConversationRepository.java` - 对话会话Repository

### 4. 服务层
- ✅ `service/AIConversationService.java` - 对话会话服务接口
- ✅ `service/impl/AIConversationServiceImpl.java` - 对话会话服务实现

### 5. Feign Client
- ✅ `client/AuthClient.java` - 调用Auth服务获取用户信息和画像
- ✅ `client/CommodityClient.java` - 调用Commodity服务进行AI搜索（待实现接口）

## 🔄 待完成的工作

### 1. 迁移AI核心功能
- [ ] 迁移 `AIAgentService` 到 `ai/service/impl/AIAgentServiceImpl.java`
  - 需要修改依赖：`AISearchService` → 通过 `CommodityClient` 调用
  - 需要修改依赖：`AuthClient` → 使用AI服务的 `AuthClient`
  - 需要修改依赖：`ConversationVectorService` → 迁移到AI服务

### 2. 迁移向量服务
- [ ] 迁移 `ConversationVectorService` 和 `ConversationVectorServiceImpl`
- [ ] 迁移 `CommodityVectorService` 和 `CommodityVectorServiceImpl`（可选，如果AI服务需要）
- [ ] 迁移 `UserProfileVectorService` 和 `UserProfileVectorServiceImpl`（可选）

### 3. 迁移SearchCommoditiesTool
- [ ] 迁移 `SearchCommoditiesTool` 到 `ai/tool/SearchCommoditiesTool.java`
  - 修改：通过 `CommodityClient` 调用商品搜索，而不是直接调用 `AISearchService`

### 4. 创建Controller
- [ ] 创建 `controller/AIConversationController.java` - 用户端AI对话接口
- [ ] 创建 `controller/AIAgentController.java` - AI Agent对话接口

### 5. 更新commodity-service
- [ ] 在 `InternalController` 中添加AI搜索内部接口：
  ```java
  @GetMapping("/commodity/ai-search")
  public Result aiSearchInternal(@RequestParam String query, 
                                 @RequestParam(required = false) String location,
                                 @RequestParam(required = false) Integer limit,
                                 @RequestParam(required = false) String userId) {
      // 调用 aiSearchService.search()
  }
  ```

### 6. 更新调用方
- [ ] 更新 `commodity-service/UserCommodityController` - 改为调用AI服务
- [ ] 更新 `admin-service/AIConversationController` - 改为调用AI服务

### 7. 更新配置文件
- [ ] 更新 `pom.xml` - 添加 `njumarket-service-ai` 模块
- [ ] 更新 `docker-compose.yml` - 添加AI服务容器
- [ ] 更新 Gateway 路由配置 - 添加AI服务路由
- [ ] 更新 Nacos 配置 - 添加AI服务配置

### 8. 迁移UserContextFilter（可选）
- [ ] 如果需要，迁移 `UserContextFilter` 到AI服务

## 📝 注意事项

1. **AISearchService仍在commodity-service**：
   - AI服务通过Feign调用commodity-service的内部接口
   - 需要在commodity-service的InternalController中添加AI搜索接口

2. **向量服务迁移**：
   - `ConversationVectorService` 必须迁移（AI服务需要）
   - `CommodityVectorService` 和 `UserProfileVectorService` 可选迁移

3. **数据库**：
   - AI服务需要访问 `ai_conversations` 表
   - AI服务需要访问向量表（如果迁移向量服务）

4. **依赖关系**：
   ```
   ai-service
     ├── 依赖 commodity-service（通过Feign调用AI搜索）
     ├── 依赖 auth-service（通过Feign获取用户画像）
     └── 独立管理对话历史和向量化
   ```

## 🎯 下一步行动

1. 先在commodity-service的InternalController中添加AI搜索内部接口
2. 迁移ConversationVectorService到AI服务
3. 迁移AIAgentService到AI服务（修改依赖）
4. 迁移SearchCommoditiesTool到AI服务（修改为Feign调用）
5. 创建Controller
6. 更新调用方代码
7. 更新配置文件

