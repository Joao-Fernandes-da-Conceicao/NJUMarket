-- ========================================
-- NJUMarket 消息双向删除功能
-- NJU Market Message Bidirectional Deletion
-- ========================================

SET NAMES utf8mb4;

-- ========================================
-- 为 messages 表添加双向删除字段
-- ========================================

-- 添加发送方删除标记
ALTER TABLE messages 
ADD COLUMN deleted_by_sender BOOLEAN DEFAULT FALSE COMMENT '发送方是否删除';

-- 添加接收方删除标记
ALTER TABLE messages 
ADD COLUMN deleted_by_receiver BOOLEAN DEFAULT FALSE COMMENT '接收方是否删除';

-- 初始化现有数据
UPDATE messages SET deleted_by_sender = FALSE, deleted_by_receiver = FALSE 
WHERE message_id IS NOT NULL;

-- ========================================
-- 删除原有的单项删除字段
-- ========================================

-- 删除 is_deleted 列（因为已被双向删除替代）
ALTER TABLE messages DROP COLUMN is_deleted;

-- ========================================
-- 说明
-- ========================================

/*
双向删除逻辑：

1. deleted_by_sender = true：发送方已删除此消息
2. deleted_by_receiver = true：接收方已删除此消息

显示规则：
- 如果发送方删除且接收方未删除：发送方看不到，接收方能看到
- 如果接收方删除且发送方未删除：接收方看不到，发送方能看到
- 如果双方都删除：双方都看不到（逻辑上完全删除）
- 如果双方都未删除：双方都能看到

查询过滤：
WHERE NOT (deleted_by_sender = true AND deleted_by_receiver = true)

这样只有当双方都删除时才从数据库中移除（或者在查询时过滤掉）。

删除说明：
已删除 is_deleted 列，完全使用双向删除机制。
*/

-- ========================================
-- 脚本执行完成
-- ========================================

