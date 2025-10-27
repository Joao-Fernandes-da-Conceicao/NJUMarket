-- 消息机制优化：将conversations改为基于用户对的唯一对话，支持商品/订单卡片
-- 执行日期：根据实际情况填写
-- 说明：删除commodity_id和order_id字段，确保一对用户只有一个活跃conversation

-- ==================== 第一步：清理重复对话 ====================
-- 删除重复的对话（保留最新的，状态为ACTIVE）
DELETE c1 FROM conversations c1
INNER JOIN conversations c2 
WHERE c1.conversation_id < c2.conversation_id
  AND c1.buyer_id = c2.buyer_id 
  AND c1.seller_id = c2.seller_id
  AND c1.status = 'ACTIVE'
  AND c2.status = 'ACTIVE';

-- 如果存在相同用户对的多个对话（包括非ACTIVE状态），保留最新的
DELETE c1 FROM conversations c1
INNER JOIN conversations c2 
WHERE c1.conversation_id < c2.conversation_id
  AND ((c1.buyer_id = c2.buyer_id AND c1.seller_id = c2.seller_id)
    OR (c1.buyer_id = c2.seller_id AND c1.seller_id = c2.buyer_id));

-- ==================== 第二步：删除conversations表的commodity_id和order_id ====================
ALTER TABLE conversations 
DROP COLUMN IF EXISTS commodity_id,
DROP COLUMN IF EXISTS order_id;

-- ==================== 第三步：添加唯一约束 ====================
-- 确保一对用户只有一个活跃的对话
-- 注意：MySQL 8.0.13+ 支持函数索引，但为了兼容性，我们使用应用层逻辑 + 唯一索引

-- 删除旧的唯一索引（如果存在）
DROP INDEX IF EXISTS uk_buyer_seller_active ON conversations;
DROP INDEX IF EXISTS uk_user_pair_active ON conversations;

-- 添加新的唯一约束（buyer_id, seller_id, status）
-- 注意：由于需要保证 (A,B) 和 (B,A) 被视为同一对，需要应用层逻辑保证
ALTER TABLE conversations 
ADD UNIQUE INDEX uk_buyer_seller_active (buyer_id, seller_id, status);

-- ==================== 第四步：确保messages表已有commodity_id和order_id字段 ====================
ALTER TABLE messages 
ADD COLUMN IF NOT EXISTS commodity_id VARCHAR(50) COMMENT '商品ID（实时查询，用于商品卡片）' AFTER order_snapshot_id,
ADD COLUMN IF NOT EXISTS order_id VARCHAR(50) COMMENT '订单ID（实时查询，用于订单卡片）' AFTER commodity_id;

-- 添加索引
CREATE INDEX IF NOT EXISTS idx_messages_commodity_id ON messages(commodity_id);
CREATE INDEX IF NOT EXISTS idx_messages_order_id ON messages(order_id);

-- ==================== 第五步：清理相关索引 ====================
DROP INDEX IF EXISTS idx_commodity_id ON conversations;
DROP INDEX IF EXISTS idx_order_id ON conversations;

-- ==================== 完成 ====================
-- 验证步骤：
-- 1. 检查是否有conversation仍包含commodity_id或order_id：SELECT * FROM conversations WHERE commodity_id IS NOT NULL OR order_id IS NOT NULL;
-- 2. 检查唯一性：SELECT buyer_id, seller_id, COUNT(*) FROM conversations WHERE status = 'ACTIVE' GROUP BY buyer_id, seller_id HAVING COUNT(*) > 1;
-- 3. 检查messages表的commodity_id和order_id字段：DESC messages;
