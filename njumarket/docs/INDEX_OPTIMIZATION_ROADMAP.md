# NJUMarket 索引优化路线（非商品搜索）

> 目的：记录 MySQL → PostgreSQL 迁移后，除“商品搜索”外各域的索引优化方向，便于 2.x/3.x 阶段按需实施与验证。

## 1. 消息域

### 1.1 `messages`
- 场景：对话分页、未读统计、双向聊天记录。
- 索引建议：
  - `IDX messages (conversation_id, created_at DESC)` —— 对话消息翻页。
  - `IDX messages (conversation_id, receiver_id, is_read)` + `WHERE NOT (deleted_by_sender AND deleted_by_receiver)` —— 未读统计。
  - `IDX messages (sender_id, receiver_id, created_at DESC)` —— 双向历史。
  - `GIN / Trigram` —— 文本模糊搜索（可选教学实验）。

### 1.2 `conversations`
- 场景：用户会话列表、未读数汇总。
- 索引建议：
  - 两个部分索引：
    ```sql
    CREATE INDEX ... ON conversations(user_id_1, status, user1_last_message_time DESC)
      WHERE user1_visibility = true;
    CREATE INDEX ... ON conversations(user_id_2, status, user2_last_message_time DESC)
      WHERE user2_visibility = true;
    ```
  - `LEAST/GREATEST` 组合索引 —— `findByUserPair`。

## 2. 订单域

### 2.1 `orders`
- 场景：买家/卖家列表、状态筛选、统计成交金额。
- 索引建议：
  - `IDX (buyer_id, order_status, buyer_visibility, create_time DESC)`
  - `IDX (seller_id, order_status, seller_visibility, create_time DESC)`
  - `IDX (order_status, buyer_id, seller_id) WHERE order_status = 'COMPLETED'`

### 2.2 辅助表
- `order_status_logs`: `IDX (order_id, create_time DESC)`
- `order_snapshots`: `IDX (original_order_id)`、`IDX (buyer_id)`

## 3. 用户 & 安全域

### 3.1 `user_activity_records`
- 学习要点：时间序列索引。
- 方案：
  - `BRIN (activity_time)`
  - `BTREE (user_id, activity_time DESC)`

### 3.2 `user_profiles`
- 常见排序：`vip_level` + `credit_score`
- 索引：`(vip_level, credit_score DESC)`

### 3.3 `ban_records` / `contact_blacklist`
- 按用户、设备、手机号查询：
  - `IDX (user_id) WHERE is_active = 1`
  - `IDX (device_id, is_active)`、`IDX (phone, is_active)`
  - 确认唯一索引 `uk_user_blocked` 已同步至 PostgreSQL。

## 4. 内容/图片域

### 4.1 `image_references`
- 关注活跃图片：`IDX (upload_user_id, last_reference_time) WHERE is_deleted = B'0'`

### 4.2 `message_notification_settings`
- 数据量小，保持默认索引即可。

## 5. 分析 & 监控域

### 5.1 `data_statistics`
- 多维组合：`(cycle, dimension, date_key)`

### 5.2 `promotions`
- 用户维度：`(user_id, status, start_time, end_time)`

### 5.3 `return_records`
- `IDX (order_id, return_status)`

## 6. 管理/审计域

### 6.1 `admin_operation_logs`
- 后台常按管理员 + 时间筛选：`(admin_id, create_time DESC)`

### 6.2 `admin_sessions`
- `IDX (admin_id) WHERE is_active = 1`

## 7. PostgreSQL 特性提示

- **部分索引**：针对布尔型可见性/删除标记极其高效。
- **表达式索引**：如需 `LOWER(username)` 唯一校验，可直接创建表达式索引。
- **BRIN**：适合时间序列大表（日志/活动记录），避免 B-Tree 体积过大。
- **GIN/Trigram**：为消息内容、评论等文本搜索提供基础（与商品搜索解耦）。
- **Materialized View + 索引**：面向统计/报表类查询可进一步优化。

---

> 后续执行时，请搭配 `EXPLAIN (ANALYZE, BUFFERS)` 比较前后差异，并把实验截图/心得填入 `PROJECT_DOCUMENTATION_V2.3.0.md` 或对应版本文档，以形成可复用教材。

