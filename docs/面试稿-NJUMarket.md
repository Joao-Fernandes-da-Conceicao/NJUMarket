# NJUMarket 技术面试稿

> 南京大学校园二手交易平台，微服务架构，前后端分离 + Python AI Agent。

---

## 一、整体架构概览

| 层次 | 技术选型 |
|---|---|
| 网关 | Spring Cloud Gateway（JWT 鉴权、路由转发） |
| 注册中心 | Eureka（服务发现） |
| 配置中心 | Spring Cloud Config（Git 仓库驱动） |
| 业务服务 | Spring Boot 3.2 / Java 21（Trade、Auth、Message、Notification、Image、Admin） |
| AI Agent | Python 3.11 + FastAPI + LangGraph + LangChain |
| 消息队列 | RabbitMQ 3.12（订单事件 + 消息推送） |
| 搜索引擎 | Elasticsearch 8.13.4 + IK 中文分词 |
| 向量数据库 | Milvus（商品向量 + 用户画像向量） |
| 缓存 | Redis（多 DB 隔离、Cache-Aside、用户画像） |
| 关系数据库 | PostgreSQL 16（全量业务数据） |
| 实时推送 | WebSocket + STOMP（订单状态变更、聊天消息） |
| 容器化 | Docker Compose（中间件全容器化，服务本地运行） |

---

## 二、缓存策略（Redis）

### 2.1 多 DB 隔离
各服务使用不同 Redis DB，防止 key 冲突：

| 服务 | DB |
|---|---|
| Auth（Token/Session） | 0 |
| Notification | 1 |
| Trade（商品/订单缓存） | 2 |
| Image | 3 |
| Admin | 4 |
| Message | 5 |

### 2.2 Cache-Aside 模式
以商品批量查询为例：
1. 先逐个从 Redis 读取（`cache:commodity:info:{commodityId}`）
2. 缓存 miss 的 ID 批量回落到 PostgreSQL 查询
3. 查询结果写回 Redis（TTL = 30min）
4. 商品状态变更时主动失效对应 key

```java
// 批量读缓存，缺失的从 DB 补充并回写
Set<String> uniqueIds = new LinkedHashSet<>(commodityIds);
Map<String, CommodityInternalDTO> commodityMap = batchFetchCommoditiesWithCache(uniqueIds);
```

### 2.3 用户画像缓存（Session 下沉）
Gateway 已完成 JWT 验证并将用户信息注入请求头（`X-User-Id / X-Username / X-User-Status`），各下游服务**不再调用 Auth 服务**，直接从请求头构建用户上下文。

用户 Profile（nickname、avatar）等展示信息统一缓存在：
- `cache:user:info:{userId}`（基础信息）
- `cache:user:profile:{userId}`（Profile 展示信息）

各服务通过 `UserCacheService` 读取，优先 Redis，miss 后 Feign 回落 Auth 服务并写回缓存。

### 2.4 消息重试队列（Redis ZSet）
WebSocket 推送失败时，消息写入 Redis ZSet（`websocket:retry:{userId}`，score = 重试时间戳），重试服务轮询到期消息重新推送，最多重试 3 次后丢弃。

---

## 三、WebSocket（STOMP over SockJS）

### 3.1 架构设计

```
前端 SockJS ──► Gateway /api/ws/** ──► Notification 服务
                  (StripPrefix=1)      Spring WebSocket + SimpleBroker
```

- 协议层：**STOMP over SockJS**（WebSocket 不可用时自动降级 HTTP 长轮询）
- 代理：**SimpleBroker**（内存，单节点）
- 订阅地址：`/user/{userId}/queue/notifications`（用户专属队列）
- 客户端发送地址：`/app/ack`（消息 ACK 确认）

### 3.2 认证流程
1. **HTTP 握手阶段**：`WebSocketHandshakeInterceptor` 从请求头提取 Gateway 注入的 `X-User-Id`，写入 `sessionAttributes`
2. **STOMP CONNECT 阶段**：`WebSocketChannelInterceptor` 从 `sessionAttributes` 读取 userId，设置为 `Principal`
3. `SimpMessagingTemplate.convertAndSendToUser(userId, ...)` 即可精准推送

### 3.3 心跳配置
```java
config.enableSimpleBroker("/queue", "/topic")
      .setHeartbeatValue(new long[]{10000, 10000});  // 服务端/客户端各 10s 一次
```
协商规则：双方各取 `max(client, server)` 间隔；连续 3 个周期（30s）未收到，视为半连接并主动关闭，触发前端自动重连。

### 3.4 推送线程池
MQ 消费线程将推送任务提交到专用 `websocketPushTaskExecutor`（core=5, max=20, queue=100），避免阻塞 MQ 消费线程。

---

## 四、RabbitMQ 消息队列

### 4.1 订单事件总线

**Topic Exchange** `order.exchange`，路由键 `order.#`（匹配所有子类型）：

| 操作 | 路由键 | recipientRole | 说明 |
|---|---|---|---|
| ORDER_CREATED | order.created | SELLER | 通知卖家有新订单 |
| ORDER_PAID | order.paid | SELLER | 通知卖家已付款 |
| ORDER_SHIPPED | order.shipped | BUYER | 通知买家已发货 |
| ORDER_CONFIRMED | order.confirmed | SELLER | 通知卖家已确认收货 |
| ORDER_CANCELLED | order.cancelled | 动态 | 判断是谁取消决定通知对方 |
| ORDER_COMPLETED | order.completed | BUYER | 通知买家交易完成 |

**AnonymousQueue（实例独占）**：每个 Notification 服务实例启动时声明一个 `exclusive + autoDelete` 临时队列，绑定到 `order.exchange`。实例下线时队列自动删除，无需手动维护。  
**扇出语义**：多实例部署时，每个实例各收一份事件，由持有目标用户 WebSocket 连接的实例完成推送，其余静默丢弃。

### 4.2 消息推送事件

**Topic Exchange** `message.push.exchange`，路由键 `message.push.#`。  
用户发送聊天消息后，Message 服务生产一条 `MessagePushEvent` 到此交换机，Notification 服务消费并推送 WebSocket 通知给接收方。同样采用 AnonymousQueue 扇出模型。

### 4.3 消息序列化
使用 `Jackson2JsonMessageConverter`（注册 `JavaTimeModule`），RabbitMQ 队列中消息体为 JSON，支持 `LocalDateTime` 等 Java 8 时间类型，避免了默认 Java 序列化的版本兼容问题。

### 4.4 其他 MQ 知识点（面试补充）

**RabbitMQ vs Kafka 对比**：

| 维度 | RabbitMQ | Kafka |
|---|---|---|
| 设计目标 | 消息路由（灵活拓扑） | 日志流（高吞吐） |
| 消息模型 | Exchange + Queue + Binding | Topic + Partition + Offset |
| 消息保留 | 消费后默认删除 | 按时间/大小保留（可重放） |
| 路由能力 | Direct/Topic/Fanout/Headers | 仅 Topic（分区路由） |
| 吞吐量 | 万级/s | 百万级/s |
| 适用场景 | 任务队列、事件通知、RPC | 日志收集、事件溯源、流计算 |
| 顺序保证 | 单队列 FIFO | 单分区内有序 |

本项目选 RabbitMQ 原因：订单事件路由复杂（需要 Topic Exchange 按 recipientRole 路由），消息量不大，且 Spring AMQP 生态成熟。

**RocketMQ 补充**：阿里系，支持事务消息（二阶段提交，解决分布式事务）、延迟消息（订单超时未支付自动取消）、顺序消息（同一订单操作顺序消费）。若本项目需要订单超时取消，可用 RocketMQ 延迟消息替代定时任务轮询。

---

## 五、Elasticsearch 搜索

### 5.1 索引设计

索引名：`commodities`，核心字段：

| 字段 | 类型 | 分析器 | 说明 |
|---|---|---|---|
| title | text | zh_max (索引) / zh_smart (搜索) | 商品标题 |
| description | text | zh_max / zh_smart | 商品描述 |
| keywordPayload | text | zh_max / zh_smart | AI 增广的可检索丰度文本 |
| addressSnapshotFull | text | zh_max / zh_smart | 完整地址 |
| category | keyword | — | 品类（精确过滤） |
| commodityStatus | keyword | — | 状态（精确过滤） |
| price | double | — | 价格（范围查询） |

### 5.2 IK 中文分词

自定义两种分析器（`commodity-settings.json`）：

```json
{
  "zh_max": { "tokenizer": "ik_max_word" },  // 最细粒度：索引时尽可能多切词（提升召回）
  "zh_smart": { "tokenizer": "ik_smart" }     // 智能粒度：搜索时语义聚合（提升精准）
}
```

**索引用 `ik_max_word`、搜索用 `ik_smart`** 是 IK 标准用法：索引阶段切出所有可能词组，搜索阶段用智能分词精准匹配，最大化召回的同时保证相关性。

### 5.3 AI 增广的 keywordPayload

传统 ES 搜索"笔记本"无法匹配"Surface Pro"，因为 ES 是关键词匹配，无语义理解。

解决方案：在商品入库时，调用 LLM（本项目为豆包 API）对商品信息做一次 Chat，生成一段包含同义词、常见说法、使用场景的丰度文本，写入 `keywordPayload`：

```
原始标题：surface pro 11 32G+1T顶配，带键盘
增广后：Surface Pro 11 顶配款，32GB内存搭配1TB固态硬盘，附带原装键盘，
集平板与笔记本于一体，Windows 二合一设备，轻薄便携，适合办公学习…
```

用户搜"二合一笔记本"时，通过 `keywordPayload` 即可匹配到 Surface Pro。

### 5.4 混合检索（Hybrid Search）
AI Agent 支持两种检索模式：
- **hybrid**（默认）：先 ES 关键词匹配（`keywordPayload` 全文搜索），再融合 Milvus 向量相似度结果
- **vector**：纯 Milvus 向量检索，适合口语化/抽象需求（如"我想要性价比高的学习用品"）

---

## 六、LangGraph AI Agent

### 6.1 为什么用 LangGraph 而不是单次 LLM Call

单次 LLM 直接回答购物需求存在问题：无法保证 LLM 按约束严格过滤商品、无法重试检索、无法反思回复质量。

LangGraph 将 Agent 建模为**有向图（DAG）**，每个节点是一个 LLM 调用或工具调用，边可以是条件路由，状态在图中流转。

### 6.2 图结构

```
START
  │
[router]─── intent=shopping ──► [retrieve] ──► [filter] ──► [reflect]
  │                                                              │
  └── intent=general ──► [respond_general]         action=accept ──► [respond_shopping] ──► END
                                                   action=refilter ──► [filter]（循环，最多3次）
                                                   action=reretrieve ──► [retrieve]（循环，最多3次）
```

### 6.3 各节点职责

| 节点 | LLM 调用 | 职责 |
|---|---|---|
| router | 结构化输出 `RouterOutput` | 判断 shopping/general；提取检索关键词；提取 P0 硬约束（价格/品类/成色/品牌） |
| retrieve | 无（工具调用） | hybrid/vector 检索商品，在进入 LLM 前执行 Python 硬过滤（价格/品牌剔除） |
| filter | 结构化输出 `FilterOutput` | LLM 从候选中挑选最符合的商品 ID，最多 10 个，并给出排序理由 |
| reflect | 结构化输出 `ReflectionOutput` | 反思筛选质量：accept / refilter / reretrieve，防止结果跑偏 |
| respond_shopping | 流式输出 | 自然语言总结推荐理由，不暴露商品ID（由前端渲染卡片） |
| respond_general | 流式输出 | 直接对话，不经过检索链路 |

### 6.4 P0 / P1 约束优先级架构

- **P0（硬约束）**：来自当前消息，不可违背（价格上限、品类、成色、必须/排除品牌）
  - 在 retrieve 后先由 Python 代码硬过滤，不依赖 LLM 注意力
  - 注入每个后续节点的 prompt，让 LLM 也意识到约束存在
- **P1（软偏好）**：来自用户历史画像，仅在满足 P0 后参考（如"喜欢 ROG"）

### 6.5 结构化输出（Structured Output）

使用 `llm.with_structured_output(PydanticModel)` 强制 LLM 输出 JSON Schema 对应的 Pydantic 模型，避免自由文本解析失败：

```python
router_llm = llm.with_structured_output(RouterOutput)
out = router_llm.invoke([HumanMessage(content=route_prompt)])
# out.intent, out.search_query 等字段类型安全
```

### 6.6 用户画像与会话记忆

- **短期记忆**：对话消息存入 PostgreSQL（`ai_messages` 表），按 `conversation_id` 聚合，注入上下文窗口（最近 N 条）
- **长期画像**：每次对话结束后异步触发 `profile_jobs`，调用 LLM 将对话摘要归纳为用户画像（品牌偏好、风格、价格敏感度等），写入 PostgreSQL 并同步到 Milvus 向量（用于语义相似召回）
- **Session 增广**：每次请求时，将用户历史画像注入 Agent 的系统 prompt，让 LLM 能感知 P1 软偏好

---

## 七、Milvus 向量数据库

### 7.1 两个集合（Collection）

| 集合 | 内容 | 用途 |
|---|---|---|
| `commodity_vectors` | 商品描述向量（title + description + category + 增广文本） | 语义检索商品 |
| `user_profile_vectors` | 用户画像向量（摘要文本 embedding） | 用户个性化召回 |

### 7.2 写入时机

- **商品向量**：Trade 服务 reindex 时，对每条商品调用 `POST /api/internal/vector/commodity/upsert`，Python AI 服务接收并调用 embedding 模型生成向量后 upsert 到 Milvus
- **用户画像向量**：每次 AI 对话结束后，异步将画像摘要 embedding 并 upsert

### 7.3 检索流程（hybrid 模式）

```
用户问题 ──► embedding ──► Milvus.search(topK=20)
                                    │
                        获取 commodity_id 列表
                                    │
                        ES 全文搜索（keywordPayload）
                                    │
                        融合两路结果（去重、合并）──► 进入 LLM filter 节点
```

### 7.4 MilvusClient 使用

```python
client = MilvusClient(uri=settings.milvus_uri, token=settings.milvus_token)
# 搜索
results = client.search(
    collection_name="commodity_vectors",
    data=[embedding_vector],   # List[float]
    limit=20,
    output_fields=["id"],      # 只返回 bizId，不返回原始文本
)
```

向量维度由 embedding 模型决定（本项目使用豆包 embedding 服务），Milvus 集合在初始化时定义 `dim` 参数和 metric_type（余弦相似度 `COSINE` 或内积 `IP`）。

---

## 八、其他技术要点

### 8.1 Gateway 鉴权链路
1. JWT 在 Gateway `JwtAuthenticationFilter` 统一验证
2. 验证通过后将 `userId / username / status` 写入下游请求头
3. 各下游服务的 `UserContextFilter` 读取请求头构建用户上下文，**无需再调用 Auth 服务**
4. 内部接口路径（`/api/internal/**`）在 Gateway 层面完全屏蔽，不对外暴露

### 8.2 Feign 服务间调用
- 同名服务两个客户端用 `contextId` 区分（防 `BeanDefinitionOverrideException`）
- 内部调用优先走 `/api/internal/**`（无需 JWT），避免在服务间传递 Token
- Fallback 降级：AI 服务不可用时，商品丰度增强静默失败，使用原始标题/描述构建索引

### 8.3 商品丰度增广与搜索索引的关系
Java LangChain4j AI 服务已废弃，改由 **Python LangGraph Agent 服务**同时承担：
- 用户对话 AI（主功能）
- `POST /api/internal/commodity-enrich`（商品丰度增广，供 Trade 服务 reindex 调用）

Trade 服务 `AIClient` Feign 直接指向 Python 服务（`http://localhost:8099`），不走 Eureka，因为 Python 服务未注册到 Eureka。
