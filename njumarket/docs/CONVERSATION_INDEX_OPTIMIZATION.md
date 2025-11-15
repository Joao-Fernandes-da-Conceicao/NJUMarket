# Conversation表索引优化分析

## ✅ 已应用的索引优化

### 优化后的索引

```sql
-- conversations表的索引（已优化）
-- 1. 基础索引：按时间排序（使用用户级别时间字段）
KEY `idx_user1_status_visibility_time` (`user_id_1`,`status`,`user_1_visibility`,`user_1_last_message_time` DESC),
KEY `idx_user2_status_visibility_time` (`user_id_2`,`status`,`user_2_visibility`,`user_2_last_message_time` DESC),

-- 2. 扩展索引：按未读数排序（包含未读数字段）
KEY `idx_user1_status_visibility_count_time` (`user_id_1`,`status`,`user_1_visibility`,`user_1_count` DESC,`user_1_last_message_time` DESC),
KEY `idx_user2_status_visibility_count_time` (`user_id_2`,`status`,`user_2_visibility`,`user_2_count` DESC,`user_2_last_message_time` DESC)
```

### 优化内容

1. **修改时间字段**：将索引中的`last_message_time`改为`user_1_last_message_time`和`user_2_last_message_time`
   - 原因：实际查询使用的是用户级别的时间字段
   - 效果：索引可以完全覆盖查询条件，避免回表

2. **添加未读数索引**：新增包含未读数的联合索引
   - 用途：支持按未读数排序查询对话列表
   - 排序规则：未读数降序 > 时间降序

## 部署说明

### 新部署
- `schema.sql`已更新，新部署会自动创建优化后的索引

### 现有数据库迁移
- 执行迁移脚本：`database/migrations/add_conversation_indexes.sql`
- 脚本会自动删除旧索引并创建新索引

## 历史索引情况（已废弃）

### 旧索引（已优化）

```sql
-- 旧索引（已废弃）
KEY `idx_user1_status_visibility_time` (`user_id_1`,`status`,`user_1_visibility`,`last_message_time` DESC),
KEY `idx_user2_status_visibility_time` (`user_id_2`,`status`,`user_2_visibility`,`last_message_time` DESC)
```

**问题**：
- 使用`last_message_time`而不是`user_1_last_message_time`/`user_2_last_message_time`
- 导致索引无法完全覆盖查询条件
- 缺少未读数索引，无法支持按未读数排序

### 索引字段分析

**idx_user1_status_visibility_time**:
- `user_id_1` - 用户ID1
- `status` - 状态（ACTIVE等）
- `user_1_visibility` - 用户1可见性
- `last_message_time` DESC - 最后消息时间（降序）

**idx_user2_status_visibility_time**:
- `user_id_2` - 用户ID2
- `status` - 状态（ACTIVE等）
- `user_2_visibility` - 用户2可见性
- `last_message_time` DESC - 最后消息时间（降序）

## 问题分析

### 1. 缺少未读数联合索引

**问题**：当前索引**没有包含未读数字段**（`user_1_count`/`user_2_count`）

**影响**：
- 如果需要按未读数排序查询对话列表，无法使用索引优化
- 查询未读数>0的对话时，需要回表过滤

**查询场景**：
```java
// 当前查询：按时间排序（可以使用索引）
findByUserId1AndStatusOrderByUser1LastMessageTime(userId, "ACTIVE")

// 如果需要按未读数排序（无法使用索引）
// SELECT * FROM conversations 
// WHERE user_id_1 = ? AND status = ? AND user_1_visibility = 1
// ORDER BY user_1_count DESC, user_1_last_message_time DESC
```

### 2. TEXT字段索引问题

**问题**：`last_message_content` 是TEXT类型，MySQL不支持直接对完整TEXT字段建索引

**影响**：
- 无法直接对TEXT字段建普通索引
- 如果需要按内容搜索或排序，性能较差

**解决方案**：

#### 方案1：前缀索引（推荐用于内容搜索）
```sql
-- 只索引前100个字符（根据实际需求调整）
ALTER TABLE conversations 
ADD INDEX idx_user1_last_content_prefix (user_id_1, status, user_1_visibility, 
    (SUBSTRING(user_1_last_message_content, 1, 100)));
```

**优点**：
- 可以支持内容搜索
- 索引大小可控

**缺点**：
- 只能搜索前N个字符
- 不支持完整内容匹配

#### 方案2：不索引TEXT字段（当前方案，推荐）
**当前实现**：只索引时间字段，不索引内容字段

**优点**：
- 索引小，查询快
- 时间排序已经满足大部分需求

**缺点**：
- 无法按内容搜索

#### 方案3：冗余字段（如果必须按内容排序）
如果业务需要按消息内容排序，可以考虑：
- 添加一个`last_message_content_hash`字段（VARCHAR）
- 存储内容的哈希值或前N个字符
- 对哈希字段建索引

## ✅ 已完成的优化

### 1. ✅ 添加未读数联合索引

**状态**：已完成

**索引**：
```sql
KEY `idx_user1_status_visibility_count_time` (`user_id_1`,`status`,`user_1_visibility`,`user_1_count` DESC,`user_1_last_message_time` DESC),
KEY `idx_user2_status_visibility_count_time` (`user_id_2`,`status`,`user_2_visibility`,`user_2_count` DESC,`user_2_last_message_time` DESC)
```

**使用场景**：
- 对话列表需要优先显示有未读消息的对话
- 排序规则：未读数降序 > 时间降序
- 支持查询：`ORDER BY user_1_count DESC, user_1_last_message_time DESC`

### 2. ✅ 优化索引：使用用户级别时间字段

**状态**：已完成

**优化内容**：
- 将索引中的`last_message_time`改为`user_1_last_message_time`和`user_2_last_message_time`
- 索引现在可以完全覆盖查询条件，避免回表

**索引**：
```sql
KEY `idx_user1_status_visibility_time` (`user_id_1`,`status`,`user_1_visibility`,`user_1_last_message_time` DESC),
KEY `idx_user2_status_visibility_time` (`user_id_2`,`status`,`user_2_visibility`,`user_2_last_message_time` DESC)
```

**匹配的查询**：
- `findByUserId1AndStatusOrderByUser1LastMessageTime(userId, "ACTIVE")`
- `findByUserId2AndStatusOrderByUser2LastMessageTime(userId, "ACTIVE")`

### 3. TEXT字段处理建议

**当前方案（推荐）**：
- 不索引TEXT字段
- 只查询时间字段用于排序
- 内容字段在需要时再查询

**如果必须支持内容搜索**：
```sql
-- 添加前缀索引（只索引前50个字符）
ALTER TABLE conversations 
ADD INDEX idx_user1_content_prefix (
    `user_id_1`, 
    `status`, 
    `user_1_visibility`,
    (SUBSTRING(`user_1_last_message_content`, 1, 50))
);
```

**或者使用全文索引**（MySQL 5.7+）：
```sql
-- 创建全文索引
ALTER TABLE conversations 
ADD FULLTEXT INDEX ft_user1_content (`user_1_last_message_content`);

-- 查询示例
SELECT * FROM conversations 
WHERE MATCH(`user_1_last_message_content`) AGAINST('关键词' IN NATURAL LANGUAGE MODE);
```

## 总结

1. **未读数索引**：如果需要按未读数排序，建议添加包含未读数的联合索引
2. **时间字段索引**：建议使用`user_1_last_message_time`而不是`last_message_time`
3. **TEXT字段**：当前不索引的方案是合理的，如果必须搜索内容，考虑前缀索引或全文索引

