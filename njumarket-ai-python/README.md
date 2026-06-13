# NJUMarket AI 服务（Python / LangChain）

与仓库内 **`njumarket-service-ai`（LangChain4j）** 功能对齐的独立实现：

- **PostgreSQL**：`nju_market.ai_conversations` / `ai_messages`（与 `database/ai_chat_tables.sql` 一致）
- **Redis**：`ai:profile:{userId}` 用户画像摘要（`AI_REDIS_DATABASE`，默认 **3**）
- **Milvus**：`commodity_vectors` / `user_profile_vectors` / `conversation_memory_vectors`（维度 **1024**，**COSINE**）
- **豆包 API**：`DOUBAO_*` 与 `njumarket-service-ai-dev.yml` 中 `langchain4j.open-ai` 一致
- **HTTP**：商品公开搜索 + 内部批量查询（`COMMODITY_BASE_URL`，默认 **8092**）

## 配置

环境变量与 Java 侧 **同名、同默认值**，见项目根目录 `.env.example`。

复制后按需修改：

```bash
cp .env.example .env
```

## 依赖

- Python 3.11+
- 已启动：PostgreSQL、Redis、Milvus（可选，关闭则 `MILVUS_ENABLED=false`）
- 本机已启动 **njumarket-service-trade**（默认 **8092**）

## 安装与运行

```bash
python -m venv .venv
.venv\Scripts\activate   # Windows
pip install -r requirements.txt
python -m app.main
```

默认监听 **`http://0.0.0.0:8098`**（与 `njumarket-config` 中 `njumarket-service-ai.yml` 的 **`server.port: 8098`** 一致；环境变量 **`SERVER_PORT`**）。

**与 Java 服务对齐（本机只跑其一即可）**

| 项目 | 说明 |
|------|------|
| 端口 | **8098**（与 LangChain4j 版 `njumarket-service-ai` 相同，网关/前端无需改端口） |
| 业务 API | 前缀 **`/api/user/ai-agent`**，与 `AIAgentController` 一致（网关 `Path=/api/user/ai-agent/**`） |
| 健康检查 | **`GET /actuator/health`** → `{"status":"UP"}`（与 Spring Boot Actuator 路径一致）；另有 **`GET /health`** 简写 |

## API（与 `AIAgentController` 一致）

前缀：`/api/user/ai-agent`

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/actuator/health` | 与 Spring Boot Actuator 一致：`{"status":"UP"}` |
| GET | `/health` | 简写探活 |
| POST | `/chat` | Query：`message`、`conversationId`；需 **`X-User-Id`** 或 `userId` |
| GET | `/chat-stream` | SSE：`token` / `complete` 事件 |
| GET | `/chats` | 会话列表 |
| GET | `/chats/{id}/messages` | 消息列表（含推荐商品批量查询） |
| GET | `/profile` | Redis 画像 |

无网关本地调试时，传 **`X-User-Id: <用户ID>`**（与 commodity 的 `UserContextFilter` 约定一致）。

## 与 Java 服务的关系

- **不删除** LangChain4j 实现；本目录为并行实现。
- 若需网关只暴露一个 AI 服务，可改路由指向本进程或 Java 进程；**无需改** commodity/auth。

## Skill 架构（与 Java `AgentSkill` 对齐）

- `app/skills/protocol.py`：`AgentSkill` 协议（order、工具、system 片段、生命周期、推荐解析）。
- `app/skills/commodity_search/`：**商品检索 Skill**（`skill.py` + `tools.py` + `session_store.py` + `prompts.py` 内聚）。
- `app/skills/registry.py`：注册表、`build_base_system_prompt`、`collect_tools`、`resolve_recommended_commodities`。

新增能力时：实现 `AgentSkill`，在 `get_enabled_skills()` 中注册，并调用 `app.chains.invalidate_agent_graph_cache()`（或 `ai_agent_service` 中的同名重导出）以重建 LangGraph。

Java 侧已补充 **`AgentSkillRegistry`**（`njumarket-service-ai/.../skill/AgentSkillRegistry.java`），与 Python 注册表语义一致，便于后续重构 `AIAgentService`。

## 流式对话（token 级）

`GET /api/user/ai-agent/chat-stream` 使用 LangGraph **`astream_events` + `on_chat_model_stream`**，按模型下发的 **AIMessageChunk** 推送（工具调用阶段无文本 token，与 Java 行为一致）。

## 目录结构

```
app/
  main.py                 # FastAPI 路由
  config.py               # 与 application.yml / dev yml 对齐
  context.py              # 请求级 memory_id（与 gateway 约定）
  ai_agent_service.py     # 编排入口：Skill 生命周期 + 调用 chains
  chains/                 # LCEL：会话增广、记忆、ReAct Agent、持久化/画像
  vector/                 # Embedding + Milvus（工具与会话增广共用）
  skills/
    protocol.py           # AgentSkill 协议
    registry.py           # 注册表与回合回调
    commodity_search/     # 商品检索 Skill（工具/会话/prompt 内聚）
    SKILL.md
  embedding_service.py    # 兼容重导出 → vector.embedding
  milvus_vector_service.py  # 兼容重导出 → vector.milvus_service
  prompts.py              # 兼容重导出 → commodity_search.prompts
  storage/                # PostgreSQL + Redis
  db/
```
