# 南大集市 NJUMarket v3.1.1 项目文档

## 📌 版本概述

**NJUMarket v3.1.1** 是一次以“搜索精准度与统一上下文”为主题的迭代版本，目标是让搜索结果更加符合用户期望，并让 AI Agent 在所有场景下都使用统一的用户上下文信息。

> **版本状态**：✅ 已完成  
> **完成时间**：2025 年 11 月  
> **主要成果**：搜索过滤增强、AI Agent 用户上下文统一、Function Calling 工具精简

---

## ✨ 核心成果

| 模块 | 功能 | 状态 |
| --- | --- | --- |
| **Elasticsearch 搜索** | 过滤掉自己的商品与库存为 0 的商品 | ✅ |
| **AI 语义搜索** | SQL 层统一过滤库存与卖家 | ✅ |
| **AI Agent Function Calling** | 去除 ThreadLocal 依赖，统一使用 `SecurityUtils` | ✅ |
| **SearchCommoditiesTool** | 精简工具逻辑，消除上下文不一致 | ✅ |

---

## 🔍 搜索过滤增强

### 1. Elasticsearch 搜索

- **位置**：`CommoditySearchService`
- **新增过滤条件**：
  - `stock > 0`
  - `sellerId != currentUserId`
- **调用方式**：`CommodityQueryServiceImpl.trySearchCommoditiesWithElastic()` 中在查询前获取 `SecurityUtils.getCurrentUserId()` 并传递给 ES 查询。

```java
bool.filter(f -> f.range(r -> r.field("stock").gt(JsonData.of(0))));
if (StringUtils.hasText(userId)) {
    bool.mustNot(mn -> mn.term(t -> t.field("sellerId").value(userId)));
}
```

### 2. AI 语义搜索（pgvector）

- **位置**：`AISearchService`
- **SQL 条件**：
  ```sql
  WHERE c.commodity_status = 'ON_SHELF'
    AND c.seller_visibility = 'PUBLIC'
    AND c.buyer_visibility = 'PUBLIC'
    AND c.stock > 0
    AND c.seller_id != ? -- 当 userId 存在时注入
  ```
- `CommodityQueryServiceImpl.aiSearch()` 会传入当前用户 ID，确保 AI 搜索与 ES 搜索行为一致。

---

## 🤖 AI Agent 上下文统一

### 1. SearchCommoditiesTool 精简

- 删除 ThreadLocal 存储，仅通过 `SecurityUtils.getCurrentUserId()` 获取用户。
- Function Calling 全程依赖用户上下文 Filter（`UserContextFilter`）提供的 `SecurityContext`。

```java
String userId = SecurityUtils.getCurrentUserId();
List<Commodity> commodities = aiSearchService.search(query, location, limit, userId);
```

### 2. AIAgentService 清理

- 去掉 `setCurrentUserId()` / `clearCurrentUserId()` 调用。
- Chat 与流式 Chat 均只依赖统一上下文，避免多线程 ThreadLocal 泄漏风险。
- 保留原有 Cursor-like 机制：消息窗口、摘要、历史检索、用户画像注入保持不变。

---

## 📚 相关修改文件

| 文件 | 说明 |
| --- | --- |
| `commodity/search/CommoditySearchService.java` | BoolQuery 增加库存与卖家过滤 |
| `commodity/service/impl/CommodityQueryServiceImpl.java` | 将当前用户 ID 传递到 ES / AI 搜索 |
| `commodity/vector/AISearchService.java` | SQL 统一过滤库存和卖家 |
| `commodity/vector/function/SearchCommoditiesTool.java` | 移除 ThreadLocal，统一用户上下文 |
| `commodity/vector/AIAgentService.java` | 删除 ThreadLocal 逻辑，清理嵌套 try |

---

## ✅ 回归测试建议

1. **常规搜索（ES）**
   - 用户 A 发布商品，搜索时确认看不到自己的商品。
   - 将商品库存设为 0，再次搜索应无结果。
2. **AI 搜索（向量）**
   - 与 ES 场景相同，确认 AI 推荐列表不会包含自己的商品或库存为 0 的商品。
3. **AI Agent 对话**
   - 连续对话 + Function Calling，确保能正确获取用户 ID，不报错。
   - 并发测试（多窗口同时使用），确认没有上下文串号。

---

## 🗂 版本信息

- **文档版本**：v3.1.1  
- **最后更新**：2025 年 11 月  
- **维护者**：NJUMarket 开发团队


