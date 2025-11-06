-- ============================================
-- 验证 findMessagesBefore 方法的索引使用情况
-- ============================================

-- 1. 查看现有索引
SHOW INDEX FROM messages;

-- 2. 分析查询计划（使用示例参数）
-- 注意：需要替换实际的 conversationId 和 beforeTime
EXPLAIN SELECT m.* FROM messages m 
WHERE m.conversation_id = 'CONVERSATION_123' 
  AND m.created_at < '2024-01-01 00:00:00'
  AND NOT (m.deleted_by_sender = true AND m.deleted_by_receiver = true)
ORDER BY m.created_at DESC
LIMIT 50;

-- 3. 分析结果说明：
-- - key: 显示使用的索引名称
-- - key_len: 显示使用的索引长度（可以判断使用了索引的哪些字段）
-- - rows: 扫描的行数
-- - Extra: 额外的信息（如 Using where, Using index, Using filesort 等）

-- ============================================
-- 预期结果分析
-- ============================================

-- 情况1：使用 idx_conversation_time
-- key: idx_conversation_time
-- key_len: 约 260 (conversation_id: 255 + created_at: 5)
-- rows: 约等于符合条件的消息数
-- Extra: Using where (表示需要在索引扫描后过滤删除条件)

-- 情况2：使用 idx_conversation_deleted_time（不太可能）
-- key: idx_conversation_deleted_time
-- key_len: 约 260 (只使用了 conversation_id 部分)
-- rows: 约等于该对话的所有消息数
-- Extra: Using where; Using filesort (表示需要额外的排序操作)

-- ============================================
-- 优化建议
-- ============================================

-- 如果发现使用了 idx_conversation_deleted_time 且性能不佳，
-- 可以考虑：
-- 1. 强制使用 idx_conversation_time（如果性能更好）
-- 2. 或者删除 idx_conversation_deleted_time（如果不需要）

-- 注意：删除条件 NOT (deletedBySender = true AND deletedByReceiver = true)
-- 无法直接利用索引的 deleted_by_sender 和 deleted_by_receiver 字段，
-- 因为这是一个复杂的逻辑表达式，不是简单的等值或范围查询。

