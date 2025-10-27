-- 消息机制优化：Conversation 改为用户对唯一，支持商品/订单卡片
-- 执行前请备份数据！

-- ==================== 第一步：修改 conversations 表 ====================

-- 1. 删除 commodity_id 和 order_id 列
ALTER TABLE conversations 
DROP COLUMN IF EXISTS commodity_id,
DROP COLUMN IF EXISTS order_id;

-- 2. 清理重复的活跃对话（保留最新的）
DELETE c1 FROM conversations c1
INNER JOIN conversations c2 
WHERE c1.conversation_id < c2.conversation_id
  AND (
    (c1.buyer_id = c2.buyer_id AND c1.seller_id = c2.seller_id) OR
    (c1.user_id_1 = c2.user_id_1 AND c1.user_id_2 = c2.user_id_2)
  )
  AND c1.status = 'ACTIVE'
  AND c2.status = 'ACTIVE';

-- 3. 如果使用 buyer_id 和 seller_id，添加唯一约束
-- （如果数据库已有 user_id_1 和 user_id_2，则使用 uk_user_pair_active）
ALTER TABLE conversations 
ADD UNIQUE INDEX IF NOT EXISTS uk_buyer_seller_active (buyer_id, seller_id, status),
ADD UNIQUE INDEX IF NOT EXISTS uk_user_pair_active (user_id_1, user_id_2, status);

-- ==================== 第二步：修改 messages 表 ====================

-- 1. 添加 commodity_id 和 order_id 字段（用于实时查询卡片）
ALTER TABLE messages 
ADD COLUMN IF NOT EXISTS commodity_id VARCHAR(50) COMMENT '商品ID（实时查询，用于商品卡片）',
ADD COLUMN IF NOT EXISTS order_id VARCHAR(50) COMMENT '订单ID（实时查询，用于订单卡片）';

-- 2. 添加索引
CREATE INDEX IF NOT EXISTS idx_messages_commodity_id ON messages(commodity_id);
CREATE INDEX IF NOT EXISTS idx_messages_order_id ON messages(order_id);

-- ==================== 验证 ====================
-- 执行以下查询验证：
-- 1. 检查是否有重复的活跃对话：
--    SELECT buyer_id, seller_id, COUNT(*) FROM conversations WHERE status='ACTIVE' GROUP BY buyer_id, seller_id HAVING COUNT(*) > 1;
--    SELECT user_id_1, user_id_2, COUNT(*) FROM conversations WHERE status='ACTIVE' GROUP BY user_id_1, user_id_2 HAVING COUNT(*) > 1;
-- 2. 检查 messages 表新字段：
--    DESCRIBE messages;

