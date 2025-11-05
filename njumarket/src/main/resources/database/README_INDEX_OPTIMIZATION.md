# 索引优化操作指南

## 📋 概述

本文档说明如何在索引优化之前清空表的所有索引，以及如何重新创建优化后的索引。

---

## 1. 清空索引（索引优化前）

### 1.1 使用 SQL 脚本

**文件**：`clear_all_indexes.sql`

**步骤**：

1. **查看当前索引**：
```sql
SHOW INDEX FROM orders;
SHOW INDEX FROM commodities;
```

2. **生成删除索引的SQL**：
```sql
-- 生成删除 orders 表所有非主键索引的SQL
SELECT CONCAT('DROP INDEX ', INDEX_NAME, ' ON orders;') AS drop_index_sql
FROM information_schema.STATISTICS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'orders'
  AND INDEX_NAME != 'PRIMARY'
GROUP BY INDEX_NAME;
```

3. **执行生成的SQL**：
```sql
-- 复制上一步生成的SQL并执行
DROP INDEX idx_buyer_id ON orders;
DROP INDEX idx_seller_id ON orders;
-- ... 其他索引
```

### 1.2 注意事项

⚠️ **重要提醒**：
- ✅ 删除索引前请**备份数据库**
- ✅ 主键索引（PRIMARY）不会被删除
- ✅ 建议在**业务低峰期**执行
- ✅ 删除索引后，相关查询性能会下降（这是正常的）
- ✅ 删除后可以重新创建优化后的索引

---

## 2. 创建优化后的索引（索引优化后）

### 2.1 订单表索引

**文件**：`optimize_indexes_v1.3.sql`（待创建）

```sql
-- 订单表优化索引
CREATE INDEX idx_buyer_visibility_time 
ON orders(buyer_id, buyer_visibility, create_time DESC);

CREATE INDEX idx_buyer_status_visibility 
ON orders(buyer_id, order_status, buyer_visibility);

CREATE INDEX idx_seller_visibility_time 
ON orders(seller_id, seller_visibility, create_time DESC);

CREATE INDEX idx_seller_status_visibility 
ON orders(seller_id, order_status, seller_visibility);
```

### 2.2 商品表索引

**文件**：`add_commodity_composite_indexes.sql`（已存在）

```sql
-- 商品表优化索引（已创建）
-- 参考现有文件
```

### 2.3 用户表索引

**文件**：`optimize_user_indexes.sql`（新创建）

```sql
-- UserProfile 表覆盖索引（用于头像和昵称查询）
CREATE INDEX idx_user_profile_nickname_avatar 
ON user_profiles(user_id, nickname, avatar);

-- Users 表联合索引（用于管理端查询）
CREATE INDEX idx_user_status_register_time 
ON users(account_status, register_time DESC);
```

**说明**：
- 覆盖索引：优化批量查询用户资料，避免回表查询
- 联合索引：优化管理端按状态筛选并排序的查询

### 2.4 对话表索引

**文件**：`optimize_conversation_indexes.sql`（新创建）

```sql
-- userId1查询优化索引
CREATE INDEX idx_user1_status_visibility_time 
ON conversations(user_id_1, status, user_1_visibility, user_1_last_message_time DESC);

-- userId2查询优化索引
CREATE INDEX idx_user2_status_visibility_time 
ON conversations(user_id_2, status, user_2_visibility, user_2_last_message_time DESC);
```

**说明**：
- 避免OR条件：分别查询userId1和userId2，避免OR条件导致的索引失效
- 覆盖查询和排序：索引覆盖WHERE条件和ORDER BY，减少回表查询
- 性能提升：每个查询都能充分利用索引，提高对话列表查询性能

### 2.5 消息表索引

**文件**：`optimize_message_indexes.sql`（新创建）

```sql
-- 主要查询索引（核心索引）
CREATE INDEX idx_conversation_time 
ON messages(conversation_id, created_at DESC);

-- 标记已读/统计未读索引（辅助索引）
CREATE INDEX idx_conversation_receiver_read 
ON messages(conversation_id, receiver_id, is_read);
```

**说明**：
- 核心索引：覆盖基于conversationId的消息查询，支持按时间排序
- 辅助索引：优化标记已读和统计未读消息的查询
- 删除标记过滤在索引后进行（性能影响可接受）
- 如果删除的消息较多（>20%），可以考虑添加包含删除标记的索引

---

## 3. 完整操作流程

### 3.1 索引优化前

```sql
-- 1. 备份数据库（重要！）
mysqldump -u root -p nju_market > backup_before_index_optimization.sql

-- 2. 查看当前索引
SHOW INDEX FROM orders;
SHOW INDEX FROM commodities;

-- 3. 生成删除索引的SQL
SELECT CONCAT('DROP INDEX ', INDEX_NAME, ' ON orders;') AS drop_index_sql
FROM information_schema.STATISTICS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'orders'
  AND INDEX_NAME != 'PRIMARY'
GROUP BY INDEX_NAME;

-- 4. 执行删除索引的SQL（复制生成的SQL）
-- DROP INDEX idx_xxx ON orders;
-- ...

-- 5. 验证索引已删除
SHOW INDEX FROM orders;
```

### 3.2 创建优化后的索引

```sql
-- 1. 创建订单表优化索引
SOURCE optimize_indexes_v1.3.sql;

-- 2. 创建商品表优化索引（如果还没有）
SOURCE add_commodity_composite_indexes.sql;

-- 3. 创建用户表优化索引
SOURCE optimize_user_indexes.sql;

-- 4. 创建对话表优化索引
SOURCE optimize_conversation_indexes.sql;

-- 5. 创建消息表优化索引
SOURCE optimize_message_indexes.sql;

-- 6. 更新索引统计信息
ANALYZE TABLE orders;
ANALYZE TABLE commodities;
ANALYZE TABLE users;
ANALYZE TABLE user_profiles;
ANALYZE TABLE conversations;
ANALYZE TABLE messages;

-- 7. 验证新索引已创建
SHOW INDEX FROM orders;
SHOW INDEX FROM commodities;
SHOW INDEX FROM users;
SHOW INDEX FROM user_profiles;
SHOW INDEX FROM conversations;
SHOW INDEX FROM messages;
```

---

## 4. 快速操作脚本

### 4.1 清空所有表索引（谨慎使用）

```sql
-- 生成删除所有表索引的SQL（先查看，确认后再执行）
SELECT CONCAT('DROP INDEX ', INDEX_NAME, ' ON ', TABLE_NAME, ';') AS drop_index_sql
FROM information_schema.STATISTICS
WHERE TABLE_SCHEMA = DATABASE()
  AND INDEX_NAME != 'PRIMARY'
GROUP BY TABLE_NAME, INDEX_NAME
ORDER BY TABLE_NAME, INDEX_NAME;
```

### 4.2 查看所有索引信息

```sql
-- 查看所有表的索引信息
SELECT 
    TABLE_NAME,
    INDEX_NAME,
    GROUP_CONCAT(COLUMN_NAME ORDER BY SEQ_IN_INDEX) AS COLUMNS,
    INDEX_TYPE,
    NON_UNIQUE
FROM information_schema.STATISTICS
WHERE TABLE_SCHEMA = DATABASE()
  AND INDEX_NAME != 'PRIMARY'
GROUP BY TABLE_NAME, INDEX_NAME, INDEX_TYPE, NON_UNIQUE
ORDER BY TABLE_NAME, INDEX_NAME;
```

---

## 5. 常见问题

### Q1: 删除索引会删除数据吗？

**A**: 不会。删除索引只是删除索引结构，不会删除表中的数据。

### Q2: 主键索引会被删除吗？

**A**: 不会。脚本会自动排除主键索引（PRIMARY），主键索引不会被删除。

### Q3: 删除索引后性能会下降吗？

**A**: 是的，删除索引后相关查询的性能会下降。这是正常的，删除后需要重新创建优化后的索引。

### Q4: 如何知道索引是否被使用？

**A**: 使用 `EXPLAIN` 查看查询计划：
```sql
EXPLAIN SELECT * FROM orders WHERE buyer_id = 'xxx';
```

### Q5: 索引优化需要多长时间？

**A**: 
- 删除索引：几秒钟
- 创建索引：取决于数据量（通常几秒到几分钟）
- 更新统计信息：几秒钟

---

## 6. 最佳实践

1. ✅ **备份数据库**：删除索引前务必备份
2. ✅ **业务低峰期执行**：避免影响业务
3. ✅ **分表执行**：不要一次性删除所有表的索引
4. ✅ **验证结果**：删除和创建后都要验证
5. ✅ **监控性能**：创建索引后监控查询性能

---

**文档版本**：v1.0  
**最后更新**：2025-11-05  
**维护者**：NJUMarket 开发团队

