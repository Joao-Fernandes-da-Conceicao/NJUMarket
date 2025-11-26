# NJUMarket 架构整理方案

## 📋 当前架构问题分析

### 1. 职责混乱问题

#### 问题1：`commodity-service` 承担过多职责
**当前状态**：
- ✅ 商品CRUD管理
- ✅ Elasticsearch全文搜索
- ✅ AI语义搜索（pgvector）
- ✅ AI Agent对话服务
- ✅ AI对话历史管理（AIConversation）
- ✅ 商品向量化
- ✅ 用户画像向量化（重复实现）
- ✅ 对话历史向量化

**问题**：
- 服务职责过重，违反单一职责原则
- AI功能与商品管理耦合，难以独立扩展
- 搜索功能（ES + AI）混在一起，边界不清

#### 问题2：AI功能分散
**当前分布**：
- `commodity-service`: AIAgentService、AISearchService、向量服务
- `admin-service`: AIConversationController（管理端）
- `commodity-service`: AIConversationService（用户端）

**问题**：
- AI相关功能分散在多个服务中
- 管理端和用户端的AI对话管理逻辑重复
- 难以统一管理和维护

#### 问题3：向量服务重复实现
**当前状态**：
- `auth-service`: UserProfileVectorService（用户画像向量化）
- `commodity-service`: UserProfileVectorService（重复实现）
- `commodity-service`: CommodityVectorService（商品向量化）
- `commodity-service`: ConversationVectorService（对话向量化）

**问题**：
- 相同功能在不同服务中重复实现
- 难以统一管理向量化策略
- 代码冗余，维护成本高

### 2. 服务边界不清

```
当前架构：
┌─────────────────────────────────────────┐
│      commodity-service (过重)           │
│  - 商品CRUD                             │
│  - ES搜索                               │
│  - AI语义搜索                           │
│  - AI Agent                             │
│  - 对话历史管理                         │
│  - 向量化服务（商品/用户/对话）         │
└─────────────────────────────────────────┘

┌─────────────────────────────────────────┐
│      admin-service                      │
│  - 管理端功能                           │
│  - AI对话管理（与commodity重复）        │
└─────────────────────────────────────────┘
```

### 3. 依赖关系复杂

**当前依赖链**：
```
AIAgentService
  ├── SearchCommoditiesTool
  │     └── CommodityQueryService
  │           ├── CommoditySearchService (ES)
  │           └── AISearchService (pgvector)
  ├── ConversationVectorService
  ├── AuthClient (获取用户画像)
  └── AIConversationService (保存对话历史)
```

**问题**：
- AI Agent依赖过多服务，耦合度高
- 难以独立测试和部署
- 服务间调用链路过长

---

## 🎯 架构整理方案

### 方案一：渐进式重构（推荐）⭐

**优点**：风险低，可逐步迁移，不影响现有功能  
**缺点**：需要多次迭代

#### 第一步：拆分AI服务

**创建 `njumarket-service-ai` 服务**

**职责划分**：
```
njumarket-service-ai
├── AI Agent核心功能
│   ├── AIAgentService（对话、流式对话）
│   ├── SearchCommoditiesTool（Function Calling工具）
│   └── AIConversationService（对话历史管理）
├── 向量服务（统一管理）
│   ├── CommodityVectorService（商品向量化）
│   ├── UserProfileVectorService（用户画像向量化）
│   └── ConversationVectorService（对话向量化）
└── AI配置
    └── LangChain4jConfig
```

**迁移步骤**：
1. 创建新服务 `njumarket-service-ai`
2. 迁移 `AIAgentService` 和 `SearchCommoditiesTool`
3. 迁移所有向量服务（统一管理）
4. 迁移 `AIConversationService` 和 `AIConversation` 实体
5. 更新 `commodity-service` 和 `admin-service`，通过 Feign 调用 AI 服务
6. 删除 `commodity-service` 中的 AI 相关代码

**依赖关系**：
```
ai-service
  ├── 依赖 commodity-service（通过Feign调用商品查询）
  ├── 依赖 auth-service（通过Feign获取用户画像）
  └── 独立管理向量化逻辑
```

#### 第二步：拆分搜索服务（可选）

**创建 `njumarket-service-search` 服务**

**职责划分**：
```
njumarket-service-search
├── Elasticsearch搜索
│   ├── CommoditySearchService
│   └── CommoditySearchRepository
├── AI语义搜索
│   └── AISearchService（pgvector）
└── 统一搜索接口
    └── UnifiedSearchService（ES + AI混合搜索）
```

**迁移步骤**：
1. 创建新服务 `njumarket-service-search`
2. 迁移 `CommoditySearchService` 和 ES 相关代码
3. 迁移 `AISearchService`（从 commodity 或 ai 服务）
4. 创建统一搜索接口，封装 ES 和 AI 搜索
5. 更新 `commodity-service`，通过 Feign 调用搜索服务

**依赖关系**：
```
search-service
  ├── 依赖 commodity-service（获取商品数据）
  ├── 独立管理 ES 索引
  └── 独立管理 pgvector 向量
```

#### 第三步：清理和优化

1. **删除重复代码**：
   - 删除 `commodity-service` 中的 AI 相关代码
   - 删除 `auth-service` 中的向量服务（统一到 ai-service）
   - 删除 `admin-service` 中重复的 AI 对话管理

2. **统一接口规范**：
   - 统一 Feign Client 命名规范
   - 统一 DTO 和响应格式
   - 统一异常处理

3. **优化依赖关系**：
   - 减少服务间直接依赖
   - 使用消息队列解耦（RabbitMQ）
   - 引入事件驱动架构

---

### 方案二：激进式重构

**优点**：一次性解决所有问题，架构清晰  
**缺点**：风险高，需要大量测试，可能影响现有功能

**新架构**：
```
┌─────────────────┐
│   Gateway       │
└────────┬────────┘
         │
    ┌────┴────┬──────────┬──────────┬──────────┐
    │         │          │          │          │
┌───▼───┐ ┌──▼───┐ ┌────▼────┐ ┌──▼───┐ ┌───▼───┐
│ Auth  │ │Order │ │Commodity│ │ AI   │ │Search │
│       │ │      │ │         │ │       │ │       │
│ 用户  │ │订单  │ │ 商品CRUD│ │ Agent │ │ ES+AI │
│ 认证  │ │管理  │ │ 图片    │ │ 对话  │ │ 搜索  │
└───────┘ └──────┘ └─────────┘ └───────┘ └───────┘
```

**服务职责**：
- `auth-service`: 用户认证、用户管理、管理员管理
- `commodity-service`: 商品CRUD、图片管理、商品状态管理
- `order-service`: 订单管理、库存扣减、投诉处理
- `ai-service`: AI Agent、向量化服务、对话历史管理
- `search-service`: Elasticsearch搜索、AI语义搜索、统一搜索接口
- `message-service`: 消息发送、会话管理、WebSocket推送
- `admin-service`: 管理端功能（调用各服务）

---

## 📝 推荐实施方案

### 阶段一：拆分AI服务（优先级：高）

**目标**：将AI相关功能独立成服务

**具体步骤**：

1. **创建 `njumarket-service-ai` 模块**
   ```bash
   # 复制 commodity-service 结构
   # 创建新的 pom.xml
   # 配置端口：8097
   ```

2. **迁移AI核心功能**
   - `vector/AIAgentService.java` → `ai/service/AIAgentService.java`
   - `vector/function/SearchCommoditiesTool.java` → `ai/tool/SearchCommoditiesTool.java`
   - `vector/AISearchService.java` → `ai/search/AISearchService.java`（暂时保留，后续可迁移到search-service）

3. **迁移向量服务**
   - `vector/CommodityVectorService.java` → `ai/vector/CommodityVectorService.java`
   - `vector/UserProfileVectorService.java` → `ai/vector/UserProfileVectorService.java`
   - `vector/ConversationVectorService.java` → `ai/vector/ConversationVectorService.java`
   - 删除 `auth-service` 中的向量服务

4. **迁移对话历史管理**
   - `entity/AIConversation.java` → `ai/entity/AIConversation.java`
   - `repository/AIConversationRepository.java` → `ai/repository/AIConversationRepository.java`
   - `service/AIConversationService.java` → `ai/service/AIConversationService.java`
   - `controller` → 创建 `ai/controller/AIConversationController.java`（用户端）
   - `admin-service` 中的 AI 对话管理改为调用 ai-service

5. **创建Feign Client**
   ```java
   // ai-service 中创建
   @FeignClient(name = "njumarket-service-commodity")
   public interface CommodityClient {
       // 商品查询接口
   }
   
   @FeignClient(name = "njumarket-service-auth")
   public interface AuthClient {
       // 用户画像接口
   }
   ```

6. **更新调用方**
   - `commodity-service/UserCommodityController` → 改为调用 ai-service
   - `admin-service/AIConversationController` → 改为调用 ai-service

7. **配置更新**
   - 更新 `docker-compose.yml`，添加 ai-service
   - 更新 Gateway 路由配置
   - 更新 Nacos 配置

**预期效果**：
- ✅ AI功能独立，职责清晰
- ✅ 向量服务统一管理
- ✅ 减少 commodity-service 的复杂度
- ✅ 便于AI功能的独立扩展和优化

---

### 阶段二：拆分搜索服务（优先级：中）

**目标**：将搜索功能独立成服务

**具体步骤**：

1. **创建 `njumarket-service-search` 模块**
   - 端口：8098

2. **迁移搜索功能**
   - `search/CommoditySearchService.java` → `search/service/CommoditySearchService.java`
   - `search/CommoditySearchRepository.java` → `search/repository/CommoditySearchRepository.java`
   - `vector/AISearchService.java` → `search/service/AISearchService.java`

3. **创建统一搜索接口**
   ```java
   public interface UnifiedSearchService {
       // ES搜索
       SearchResult searchByElasticsearch(String query, String userId);
       // AI语义搜索
       SearchResult searchByAI(String query, String userId);
       // 混合搜索（ES + AI）
       SearchResult hybridSearch(String query, String userId);
   }
   ```

4. **更新调用方**
   - `commodity-service` → 通过Feign调用search-service
   - `ai-service/SearchCommoditiesTool` → 通过Feign调用search-service

**预期效果**：
- ✅ 搜索功能独立，便于优化ES和向量搜索
- ✅ 减少 commodity-service 的职责
- ✅ 便于搜索功能的横向扩展

---

### 阶段三：优化和清理（优先级：低）

**目标**：优化依赖关系，清理冗余代码

**具体步骤**：

1. **引入事件驱动**
   - 商品发布 → 发送MQ消息 → search-service监听 → 更新ES索引和向量
   - 商品更新 → 发送MQ消息 → search-service监听 → 同步更新

2. **统一异常处理**
   - 统一Feign异常处理
   - 统一业务异常定义

3. **优化依赖关系**
   - 减少同步调用，使用异步消息
   - 引入缓存减少服务间调用

---

## 🔧 实施注意事项

### 1. 数据库迁移

**AI服务数据库**：
- 需要迁移 `ai_conversation` 表到新的数据库（或共享数据库）
- 向量表（commodity_vector、user_profile_vector、conversation_vector）需要迁移

**搜索服务数据库**：
- ES索引保持不变（ES独立部署）
- pgvector向量表需要迁移或共享

### 2. 配置管理

**统一配置中心（Nacos）**：
- AI服务配置（LangChain4j、向量模型）
- 搜索服务配置（ES连接、pgvector连接）
- 服务间调用配置（Feign超时、重试）

### 3. 测试策略

**单元测试**：
- 每个服务的核心功能需要单元测试
- Mock Feign Client 进行测试

**集成测试**：
- 服务间调用测试
- 端到端功能测试

**性能测试**：
- AI服务响应时间
- 搜索服务QPS
- 服务间调用延迟

### 4. 回滚方案

**如果出现问题**：
1. 保留原代码分支
2. 逐步迁移，每个阶段充分测试
3. 准备快速回滚脚本

---

## 📊 架构对比

### 当前架构
```
commodity-service (过重)
├── 商品CRUD
├── ES搜索
├── AI搜索
├── AI Agent
├── 对话历史
└── 向量服务

admin-service
└── AI对话管理（重复）
```

### 整理后架构（方案一）
```
commodity-service (轻量)
├── 商品CRUD
└── 图片管理

ai-service (新增)
├── AI Agent
├── 对话历史
└── 向量服务（统一）

search-service (新增，可选)
├── ES搜索
└── AI搜索

admin-service
└── 调用ai-service（不再重复）
```

---

## 🎯 总结

**推荐方案**：**方案一（渐进式重构）**

**理由**：
1. ✅ 风险可控，可逐步迁移
2. ✅ 不影响现有功能
3. ✅ 每个阶段都有明确目标
4. ✅ 便于测试和验证

**优先级排序**：
1. **阶段一：拆分AI服务**（最重要，解决核心问题）
2. **阶段二：拆分搜索服务**（可选，进一步优化）
3. **阶段三：优化和清理**（长期优化）

**预期收益**：
- ✅ 服务职责清晰，符合单一职责原则
- ✅ AI功能独立，便于扩展和优化
- ✅ 减少代码重复，降低维护成本
- ✅ 提高系统可测试性和可维护性

