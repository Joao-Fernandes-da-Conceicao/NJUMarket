-- ==================== 添加订单提醒字段 ====================
-- ✅ v1.3.x: 为订单提醒添加持久化支持
-- 兼容性：使用 IF NOT EXISTS 确保向后兼容，字段允许 NULL，有默认值

-- 检查并添加 seller_order_has_new 字段
SET @exist := (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS 
               WHERE TABLE_SCHEMA = DATABASE() 
               AND TABLE_NAME = 'user_profiles' 
               AND COLUMN_NAME = 'seller_order_has_new');
SET @sqlstmt := IF(@exist = 0, 
    'ALTER TABLE user_profiles ADD COLUMN seller_order_has_new BOOLEAN DEFAULT FALSE COMMENT ''卖家订单是否有新变化（v1.3.x）''',
    'SELECT ''Column seller_order_has_new already exists'' AS info');
PREPARE stmt FROM @sqlstmt;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 检查并添加 buyer_order_has_new 字段
SET @exist := (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS 
               WHERE TABLE_SCHEMA = DATABASE() 
               AND TABLE_NAME = 'user_profiles' 
               AND COLUMN_NAME = 'buyer_order_has_new');
SET @sqlstmt := IF(@exist = 0, 
    'ALTER TABLE user_profiles ADD COLUMN buyer_order_has_new BOOLEAN DEFAULT FALSE COMMENT ''买家订单是否有新变化（v1.3.x）''',
    'SELECT ''Column buyer_order_has_new already exists'' AS info');
PREPARE stmt FROM @sqlstmt;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 为现有记录设置默认值（兼容旧数据）
UPDATE user_profiles 
SET seller_order_has_new = FALSE 
WHERE seller_order_has_new IS NULL;

UPDATE user_profiles 
SET buyer_order_has_new = FALSE 
WHERE buyer_order_has_new IS NULL;

