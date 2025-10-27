-- 消息机制优化：删除conversations表的commodity_id和order_id字段
-- 确保一对用户只有一个活跃的conversation

-- ==================== 第一步：确认数据结构 ====================
-- 如果数据库仍使用buyer_id和seller_id，需要先迁移到user_id_1和user_id_2
-- 如果已迁移，则直接执行后续步骤

-- ==================== 第二步：删除conversations表的commodity_id和order_id ====================

-- 删除列（如果存在）
ALTER TABLE conversations 
DROP COLUMN IF EXISTS commodity_id,
DROP COLUMN IF EXISTS order_id;

-- ==================== 第三步：确保唯一约束 ====================

-- 如果使用user_id_1和user_id_2（推荐方案）
-- 检查唯一约束是否已存在
-- ALTER TABLE conversations 
-- ADD UNIQUE INDEX IF NOT EXISTS uk_user_pair_active (user_id_1, user_id_2, status);

-- 如果仍使用buyer_id和seller_id（需要先迁移）
-- 清理重复的对话（保留最新的）
DELETE c1 FROM conversations c1
INNER JOIN conversations c2 
WHERE c1.conversation_id < c2.conversation_id
  AND (
    -- 如果使用buyer_id/seller_id
    ((c1.buyer_id = c2.buyer_id AND c1.seller_id = c2.seller_id) OR
     (c1.buyer_id = c2.seller_id AND c1.seller_id = c2.buyer_id))
    -- 如果使用user_id_1/user_id_2
    OR (c1.user_id_1 = c2.user_id_1 AND c1.user_id_2 = c2.user_id_2)
  )
  AND c1.status = 'ACTIVE'
  AND c2.status = 'ACTIVE';

-- 添加唯一约束（根据实际字段选择）
-- 方案A：如果使用user_id_1和user_id_2
ALTER TABLE conversations 
ADD UNIQUE INDEX IF NOT EXISTS uk_user_pair_active (user_id_1, user_id_2, status);

-- 方案B：如果使用buyer_id和seller_id（不推荐，建议迁移到user_id_1/user_id_2）
-- ALTER TABLE conversations 
-- ADD UNIQUE INDEX uk_buyer_seller_active (buyer_id, seller_id, status);

-- ==================== 第四步：为messages表添加commodity_id和order_id字段 ====================
-- 注意：这些字段用于实时查询卡片，不是快照

ALTER TABLE messages 
ADD COLUMN IF NOT EXISTS commodity_id VARCHAR(50) COMMENT '商品ID（实时查询，用于商品卡片）',
ADD COLUMN IF NOT EXISTS order_id VARCHAR(50) COMMENT '订单ID（实时查询，用于订单卡片）';

-- 添加索引
CREATE INDEX IF NOT EXISTS idx_messages_commodity_id ON messages(commodity_id);
CREATE INDEX IF NOT EXISTS idx_messages_order_id ON messages(order_id);

-- ==================== 完成 ====================
-- 迁移完成后，建议执行以下验证：
-- 1. 检查conversations表结构：DESC conversations;
-- 2. 检查messages表结构：DESC messages;
-- 3. 检查唯一约束：SHOW INDEX FROM conversations WHERE Key_name = 'uk_user_pair_active';
-- 4. 检查是否有重复对话：SELECT user_id_1, user_id_2, COUNT(*) FROM conversations WHERE status = 'ACTIVE' GROUP BY user_id_1, user_id_2 HAVING COUNT(*) > 1;

