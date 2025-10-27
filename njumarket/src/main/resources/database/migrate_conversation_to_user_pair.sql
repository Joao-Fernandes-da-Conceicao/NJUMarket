-- 消息机制优化迁移脚本
-- 目标：将conversations改为基于用户对的唯一对话，支持快照发送功能

-- ==================== 第一步：创建快照表 ====================

-- 商品快照表
CREATE TABLE IF NOT EXISTS commodity_snapshots (
    snapshot_id VARCHAR(50) PRIMARY KEY COMMENT '快照ID',
    original_commodity_id VARCHAR(50) COMMENT '原始商品ID（可选，用于追溯）',
    title VARCHAR(200) NOT NULL COMMENT '商品标题',
    description TEXT COMMENT '商品描述',
    price DOUBLE NOT NULL COMMENT '商品价格',
    stock INT COMMENT '库存',
    location VARCHAR(200) COMMENT '所在地',
    category VARCHAR(50) COMMENT '分类',
    condition_level VARCHAR(20) COMMENT '成色等级',
    images TEXT COMMENT '图片URL（逗号分隔）',
    commodity_status VARCHAR(20) COMMENT '商品状态',
    seller_id VARCHAR(50) COMMENT '卖家ID',
    seller_name VARCHAR(100) COMMENT '卖家名称',
    seller_phone VARCHAR(20) COMMENT '卖家电话',
    seller_email VARCHAR(100) COMMENT '卖家邮箱',
    snapshot_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '快照创建时间',
    INDEX idx_original_commodity_id (original_commodity_id),
    INDEX idx_seller_id (seller_id),
    INDEX idx_snapshot_time (snapshot_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品快照表';

-- 订单快照表
CREATE TABLE IF NOT EXISTS order_snapshots (
    snapshot_id VARCHAR(50) PRIMARY KEY COMMENT '快照ID',
    original_order_id VARCHAR(50) COMMENT '原始订单ID（可选，用于追溯）',
    order_status VARCHAR(20) COMMENT '订单状态',
    pay_amount DOUBLE COMMENT '支付金额',
    quantity INT COMMENT '数量',
    shipping_address TEXT COMMENT '收货地址',
    remark TEXT COMMENT '备注',
    commodity_snapshot_id VARCHAR(50) COMMENT '关联的商品快照ID',
    buyer_id VARCHAR(50) COMMENT '买家ID',
    seller_id VARCHAR(50) COMMENT '卖家ID',
    snapshot_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '快照创建时间',
    INDEX idx_original_order_id (original_order_id),
    INDEX idx_commodity_snapshot_id (commodity_snapshot_id),
    INDEX idx_buyer_id (buyer_id),
    INDEX idx_seller_id (seller_id),
    INDEX idx_snapshot_time (snapshot_time),
    FOREIGN KEY (commodity_snapshot_id) REFERENCES commodity_snapshots(snapshot_id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单快照表';

-- ==================== 第二步：修改messages表 ====================

-- 添加快照ID字段（如果不存在）
ALTER TABLE messages 
ADD COLUMN IF NOT EXISTS commodity_snapshot_id VARCHAR(50) COMMENT '商品快照ID' AFTER order_snapshot,
ADD COLUMN IF NOT EXISTS order_snapshot_id VARCHAR(50) COMMENT '订单快照ID' AFTER commodity_snapshot_id;

-- 添加索引
CREATE INDEX IF NOT EXISTS idx_messages_commodity_snapshot ON messages(commodity_snapshot_id);
CREATE INDEX IF NOT EXISTS idx_messages_order_snapshot ON messages(order_snapshot_id);

-- 添加外键约束（可选，如果需要严格的引用完整性）
-- ALTER TABLE messages 
-- ADD CONSTRAINT fk_messages_commodity_snapshot FOREIGN KEY (commodity_snapshot_id) 
--     REFERENCES commodity_snapshots(snapshot_id) ON DELETE SET NULL;
-- 
-- ALTER TABLE messages 
-- ADD CONSTRAINT fk_messages_order_snapshot FOREIGN KEY (order_snapshot_id) 
--     REFERENCES order_snapshots(snapshot_id) ON DELETE SET NULL;

-- ==================== 第三步：修改conversations表 ====================

-- 1. 备份现有数据（可选，建议在生产环境执行）
-- CREATE TABLE conversations_backup AS SELECT * FROM conversations;

-- 2. 添加新字段（如果不存在）
ALTER TABLE conversations 
ADD COLUMN IF NOT EXISTS user_id_1 VARCHAR(50) COMMENT '用户1 ID（较小的userId）' AFTER seller_id,
ADD COLUMN IF NOT EXISTS user_id_2 VARCHAR(50) COMMENT '用户2 ID（较大的userId）' AFTER user_id_1;

-- 3. 迁移数据：将buyerId和sellerId标准化为user_id_1和user_id_2（确保user_id_1 < user_id_2）
UPDATE conversations 
SET 
    user_id_1 = CASE 
        WHEN buyer_id < seller_id THEN buyer_id 
        ELSE seller_id 
    END,
    user_id_2 = CASE 
        WHEN buyer_id < seller_id THEN seller_id 
        ELSE buyer_id 
    END
WHERE user_id_1 IS NULL OR user_id_2 IS NULL;

-- 4. 设置新字段为NOT NULL
ALTER TABLE conversations 
MODIFY COLUMN user_id_1 VARCHAR(50) NOT NULL,
MODIFY COLUMN user_id_2 VARCHAR(50) NOT NULL;

-- 5. 删除旧字段（删除commodityId和orderId）
ALTER TABLE conversations 
DROP COLUMN IF EXISTS commodity_id,
DROP COLUMN IF EXISTS order_id;

-- 6. 删除旧的buyerId和sellerId字段（保留user_id_1和user_id_2）
ALTER TABLE conversations 
DROP COLUMN IF EXISTS buyer_id,
DROP COLUMN IF EXISTS seller_id;

-- 7. 删除旧的索引
DROP INDEX IF EXISTS idx_buyer_id ON conversations;
DROP INDEX IF EXISTS idx_seller_id ON conversations;
DROP INDEX IF EXISTS idx_commodity_id ON conversations;
DROP INDEX IF EXISTS idx_order_id ON conversations;
DROP INDEX IF EXISTS idx_buyer_time ON conversations;
DROP INDEX IF EXISTS idx_seller_time ON conversations;

-- 8. 创建新索引
CREATE INDEX idx_user_id_1 ON conversations(user_id_1);
CREATE INDEX idx_user_id_2 ON conversations(user_id_2);
CREATE INDEX idx_user_pair ON conversations(user_id_1, user_id_2);
CREATE INDEX idx_last_message_time ON conversations(last_message_time);
CREATE INDEX idx_user1_time ON conversations(user_id_1, last_message_time);
CREATE INDEX idx_user2_time ON conversations(user_id_2, last_message_time);

-- 9. 添加唯一约束：确保一个用户对只有一个活跃的对话
ALTER TABLE conversations 
ADD UNIQUE INDEX uk_user_pair_active (user_id_1, user_id_2, status) 
WHERE status = 'ACTIVE';

-- 注意：MySQL 8.0+ 支持函数索引，但如果版本较低，可以使用触发器或应用层保证唯一性
-- 对于MySQL 5.7及以下，可以使用以下方式：
-- ALTER TABLE conversations ADD UNIQUE INDEX uk_user_pair (user_id_1, user_id_2);

-- ==================== 第四步：数据清理 ====================

-- 删除重复的对话（保留最新的）
DELETE c1 FROM conversations c1
INNER JOIN conversations c2 
WHERE c1.conversation_id < c2.conversation_id
  AND c1.user_id_1 = c2.user_id_1 
  AND c1.user_id_2 = c2.user_id_2
  AND c1.status = 'ACTIVE'
  AND c2.status = 'ACTIVE';

-- ==================== 完成 ====================
-- 迁移完成后，建议执行以下验证：
-- 1. 检查数据完整性：SELECT * FROM conversations WHERE user_id_1 IS NULL OR user_id_2 IS NULL;
-- 2. 检查唯一性：SELECT user_id_1, user_id_2, COUNT(*) FROM conversations WHERE status = 'ACTIVE' GROUP BY user_id_1, user_id_2 HAVING COUNT(*) > 1;
-- 3. 检查消息表的快照字段：SELECT * FROM messages WHERE commodity_snapshot_id IS NOT NULL OR order_snapshot_id IS NOT NULL;

