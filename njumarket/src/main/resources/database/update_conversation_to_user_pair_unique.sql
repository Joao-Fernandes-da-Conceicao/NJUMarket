-- 消息机制优化：Conversation 改为用户对唯一，支持商品/订单卡片
-- 执行日期：请在生产环境执行前备份数据
-- 说明：数据库目前没有重复对话，因此只需 UPDATE 确保 user_id_1 < user_id_2

-- ==================== 第一步：检查并添加 user_id_1 和 user_id_2 列（如果不存在）====================
-- 如果表只有 buyer_id 和 seller_id，需要先迁移到 user_id_1 和 user_id_2
-- MySQL 不支持 IF NOT EXISTS，需要先检查
SET @exist := (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS 
               WHERE TABLE_SCHEMA = DATABASE() 
               AND TABLE_NAME = 'conversations' 
               AND COLUMN_NAME = 'user_id_1');
SET @sqlstmt := IF(@exist = 0, 
    'ALTER TABLE conversations ADD COLUMN user_id_1 VARCHAR(50) COMMENT ''用户1 ID（较小的userId）''',
    'SELECT ''Column user_id_1 already exists'' AS info');
PREPARE stmt FROM @sqlstmt;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @exist := (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS 
               WHERE TABLE_SCHEMA = DATABASE() 
               AND TABLE_NAME = 'conversations' 
               AND COLUMN_NAME = 'user_id_2');
SET @sqlstmt := IF(@exist = 0, 
    'ALTER TABLE conversations ADD COLUMN user_id_2 VARCHAR(50) COMMENT ''用户2 ID（较大的userId）''',
    'SELECT ''Column user_id_2 already exists'' AS info');
PREPARE stmt FROM @sqlstmt;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ==================== 第二步：如果 user_id_1 和 user_id_2 为空，从 buyer_id 和 seller_id 迁移数据 ====================
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
WHERE (user_id_1 IS NULL OR user_id_2 IS NULL) 
  AND buyer_id IS NOT NULL 
  AND seller_id IS NOT NULL;

-- ==================== 第三步：标准化现有数据，确保 user_id_1 < user_id_2 ====================
-- 更新所有记录，确保 user_id_1 < user_id_2（按字符串字典序比较）
UPDATE conversations 
SET 
    user_id_1 = CASE 
        WHEN user_id_1 < user_id_2 THEN user_id_1 
        ELSE user_id_2 
    END,
    user_id_2 = CASE 
        WHEN user_id_1 < user_id_2 THEN user_id_2 
        ELSE user_id_1 
    END
WHERE user_id_1 > user_id_2;

-- ==================== 第四步：删除 commodity_id 和 order_id 列 ====================
-- MySQL 不支持 IF EXISTS，需要先检查
SET @exist := (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS 
               WHERE TABLE_SCHEMA = DATABASE() 
               AND TABLE_NAME = 'conversations' 
               AND COLUMN_NAME = 'commodity_id');
SET @sqlstmt := IF(@exist > 0, 
    'ALTER TABLE conversations DROP COLUMN commodity_id',
    'SELECT ''Column commodity_id does not exist'' AS info');
PREPARE stmt FROM @sqlstmt;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @exist := (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS 
               WHERE TABLE_SCHEMA = DATABASE() 
               AND TABLE_NAME = 'conversations' 
               AND COLUMN_NAME = 'order_id');
SET @sqlstmt := IF(@exist > 0, 
    'ALTER TABLE conversations DROP COLUMN order_id',
    'SELECT ''Column order_id does not exist'' AS info');
PREPARE stmt FROM @sqlstmt;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ==================== 第五步：确保 user_id_1 和 user_id_2 为 NOT NULL ====================
ALTER TABLE conversations 
MODIFY COLUMN user_id_1 VARCHAR(50) NOT NULL,
MODIFY COLUMN user_id_2 VARCHAR(50) NOT NULL;

-- ==================== 第六步：添加唯一约束 ====================
-- 删除可能存在的旧唯一约束
SET @exist := (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS 
               WHERE TABLE_SCHEMA = DATABASE() 
               AND TABLE_NAME = 'conversations' 
               AND INDEX_NAME = 'uk_user_pair_active');
SET @sqlstmt := IF(@exist > 0, 
    'ALTER TABLE conversations DROP INDEX uk_user_pair_active',
    'SELECT ''Index uk_user_pair_active does not exist'' AS info');
PREPARE stmt FROM @sqlstmt;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 添加新的唯一约束（确保每个用户对只有一个活跃对话）
ALTER TABLE conversations 
ADD UNIQUE INDEX uk_user_pair_active (user_id_1, user_id_2, status);

-- ==================== 第七步：清理 messages 表的不必要字段并添加必要字段 ====================
-- 删除不再使用的快照相关字段
SET @exist := (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS 
               WHERE TABLE_SCHEMA = DATABASE() 
               AND TABLE_NAME = 'messages' 
               AND COLUMN_NAME = 'commodity_snapshot');
SET @sqlstmt := IF(@exist > 0, 
    'ALTER TABLE messages DROP COLUMN commodity_snapshot',
    'SELECT ''Column commodity_snapshot does not exist'' AS info');
PREPARE stmt FROM @sqlstmt;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @exist := (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS 
               WHERE TABLE_SCHEMA = DATABASE() 
               AND TABLE_NAME = 'messages' 
               AND COLUMN_NAME = 'order_snapshot');
SET @sqlstmt := IF(@exist > 0, 
    'ALTER TABLE messages DROP COLUMN order_snapshot',
    'SELECT ''Column order_snapshot does not exist'' AS info');
PREPARE stmt FROM @sqlstmt;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @exist := (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS 
               WHERE TABLE_SCHEMA = DATABASE() 
               AND TABLE_NAME = 'messages' 
               AND COLUMN_NAME = 'commodity_snapshot_id');
SET @sqlstmt := IF(@exist > 0, 
    'ALTER TABLE messages DROP COLUMN commodity_snapshot_id',
    'SELECT ''Column commodity_snapshot_id does not exist'' AS info');
PREPARE stmt FROM @sqlstmt;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @exist := (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS 
               WHERE TABLE_SCHEMA = DATABASE() 
               AND TABLE_NAME = 'messages' 
               AND COLUMN_NAME = 'order_snapshot_id');
SET @sqlstmt := IF(@exist > 0, 
    'ALTER TABLE messages DROP COLUMN order_snapshot_id',
    'SELECT ''Column order_snapshot_id does not exist'' AS info');
PREPARE stmt FROM @sqlstmt;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 添加实时商品和订单ID字段（如果不存在）
SET @exist := (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS 
               WHERE TABLE_SCHEMA = DATABASE() 
               AND TABLE_NAME = 'messages' 
               AND COLUMN_NAME = 'commodity_id');
SET @sqlstmt := IF(@exist = 0, 
    'ALTER TABLE messages ADD COLUMN commodity_id VARCHAR(50) COMMENT ''商品ID（实时查询，用于商品卡片）''',
    'SELECT ''Column commodity_id already exists'' AS info');
PREPARE stmt FROM @sqlstmt;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @exist := (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS 
               WHERE TABLE_SCHEMA = DATABASE() 
               AND TABLE_NAME = 'messages' 
               AND COLUMN_NAME = 'order_id');
SET @sqlstmt := IF(@exist = 0, 
    'ALTER TABLE messages ADD COLUMN order_id VARCHAR(50) COMMENT ''订单ID（实时查询，用于订单卡片）''',
    'SELECT ''Column order_id already exists'' AS info');
PREPARE stmt FROM @sqlstmt;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 添加索引
SET @exist := (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS 
               WHERE TABLE_SCHEMA = DATABASE() 
               AND TABLE_NAME = 'messages' 
               AND INDEX_NAME = 'idx_messages_commodity_id');
SET @sqlstmt := IF(@exist = 0, 
    'CREATE INDEX idx_messages_commodity_id ON messages(commodity_id)',
    'SELECT ''Index idx_messages_commodity_id already exists'' AS info');
PREPARE stmt FROM @sqlstmt;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @exist := (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS 
               WHERE TABLE_SCHEMA = DATABASE() 
               AND TABLE_NAME = 'messages' 
               AND INDEX_NAME = 'idx_messages_order_id');
SET @sqlstmt := IF(@exist = 0, 
    'CREATE INDEX idx_messages_order_id ON messages(order_id)',
    'SELECT ''Index idx_messages_order_id already exists'' AS info');
PREPARE stmt FROM @sqlstmt;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ==================== 第八步：重命名未读数字段 ====================
-- 将 buyer_unread_count 和 seller_unread_count 重命名为 user_1_count 和 user_2_count
-- 1. 添加新字段
SET @exist := (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS 
               WHERE TABLE_SCHEMA = DATABASE() 
               AND TABLE_NAME = 'conversations' 
               AND COLUMN_NAME = 'user_1_count');
SET @sqlstmt := IF(@exist = 0, 
    'ALTER TABLE conversations ADD COLUMN user_1_count INT DEFAULT 0 COMMENT ''用户1的未读消息数''',
    'SELECT ''Column user_1_count already exists'' AS info');
PREPARE stmt FROM @sqlstmt;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @exist := (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS 
               WHERE TABLE_SCHEMA = DATABASE() 
               AND TABLE_NAME = 'conversations' 
               AND COLUMN_NAME = 'user_2_count');
SET @sqlstmt := IF(@exist = 0, 
    'ALTER TABLE conversations ADD COLUMN user_2_count INT DEFAULT 0 COMMENT ''用户2的未读消息数''',
    'SELECT ''Column user_2_count already exists'' AS info');
PREPARE stmt FROM @sqlstmt;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 2. 迁移数据：将 buyer_unread_count 的值复制到 user_1_count，seller_unread_count 的值复制到 user_2_count
UPDATE conversations 
SET 
    user_1_count = COALESCE(buyer_unread_count, 0),
    user_2_count = COALESCE(seller_unread_count, 0)
WHERE user_1_count IS NULL OR user_2_count IS NULL;

-- 3. 确保新字段为 NOT NULL 并设置默认值
ALTER TABLE conversations 
MODIFY COLUMN user_1_count INT NOT NULL DEFAULT 0,
MODIFY COLUMN user_2_count INT NOT NULL DEFAULT 0;

-- 4. 删除旧字段
SET @exist := (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS 
               WHERE TABLE_SCHEMA = DATABASE() 
               AND TABLE_NAME = 'conversations' 
               AND COLUMN_NAME = 'buyer_unread_count');
SET @sqlstmt := IF(@exist > 0, 
    'ALTER TABLE conversations DROP COLUMN buyer_unread_count',
    'SELECT ''Column buyer_unread_count does not exist'' AS info');
PREPARE stmt FROM @sqlstmt;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @exist := (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS 
               WHERE TABLE_SCHEMA = DATABASE() 
               AND TABLE_NAME = 'conversations' 
               AND COLUMN_NAME = 'seller_unread_count');
SET @sqlstmt := IF(@exist > 0, 
    'ALTER TABLE conversations DROP COLUMN seller_unread_count',
    'SELECT ''Column seller_unread_count does not exist'' AS info');
PREPARE stmt FROM @sqlstmt;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ==================== 完成 ====================
-- 验证：
-- 1. 检查是否有 user_id_1 >= user_id_2 的记录（不应该有）：
--    SELECT * FROM conversations WHERE user_id_1 >= user_id_2;
-- 2. 检查是否有重复的活跃对话（不应该有）：
--    SELECT user_id_1, user_id_2, COUNT(*) FROM conversations WHERE status = 'ACTIVE' GROUP BY user_id_1, user_id_2 HAVING COUNT(*) > 1;
-- 3. 检查 messages 表的新字段：
--    DESC messages;
-- 4. 检查未读数字段是否正确迁移：
--    SELECT conversation_id, user_1_count, user_2_count FROM conversations LIMIT 10;

