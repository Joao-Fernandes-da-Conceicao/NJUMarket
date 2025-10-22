-- ========================================
-- 用户表添加密码字段更新脚本
-- Add Password Field to Users Table Update Script
-- ========================================

-- 设置字符集
SET NAMES utf8mb4;

-- ========================================
-- 1. 为用户表添加密码相关字段
-- ========================================

-- 添加密码字段
ALTER TABLE `users` 
ADD COLUMN `password` varchar(255) DEFAULT NULL COMMENT '用户密码(加密存储)' AFTER `primary_phone`;

-- 添加用户名字段（可选，用于账号密码登录）
ALTER TABLE `users` 
ADD COLUMN `username` varchar(50) DEFAULT NULL COMMENT '用户名(可选)' AFTER `primary_phone`;

-- 添加密码相关的索引
ALTER TABLE `users` 
ADD UNIQUE KEY `uk_username` (`username`);

-- 添加密码最后修改时间字段
ALTER TABLE `users` 
ADD COLUMN `password_updated_at` datetime DEFAULT NULL COMMENT '密码最后修改时间' AFTER `password`;

-- ========================================
-- 2. 更新现有数据（可选）
-- ========================================

-- 临时禁用安全更新模式
SET SQL_SAFE_UPDATES = 0;

-- 为现有用户设置默认密码（建议在生产环境中要求用户重新设置密码）
-- 这里使用BCrypt加密的"123456"作为默认密码
-- 实际密码: 123456
-- BCrypt哈希: $2a$10$N.zmdr9k7uOIQzUHPPLOPOxrOVJ2eswjzfoy9rI8.sChyZwta7aaa
UPDATE `users` 
SET `password` = '$2a$10$N.zmdr9k7uOIQzUHPPLOPOxrOVJ2eswjzfoy9rI8.sChyZwta7aaa',
    `password_updated_at` = NOW()
WHERE `password` IS NULL;

-- 重新启用安全更新模式
SET SQL_SAFE_UPDATES = 1;

-- ========================================
-- 3. 验证更新结果
-- ========================================

-- 查看表结构
DESCRIBE `users`;

-- 查看更新后的数据
SELECT user_id, primary_phone, username, 
       CASE 
           WHEN password IS NOT NULL THEN '已设置密码' 
           ELSE '未设置密码' 
       END as password_status,
       password_updated_at,
       account_status
FROM `users` 
LIMIT 5;

-- ========================================
-- 4. 创建密码验证相关的存储过程（可选）
-- ========================================

DELIMITER //

-- 验证用户密码的存储过程
CREATE PROCEDURE ValidateUserPassword(
    IN p_identifier VARCHAR(50),  -- 用户标识符（手机号或用户名）
    IN p_password VARCHAR(255),   -- 明文密码
    OUT p_user_id VARCHAR(50),    -- 输出用户ID
    OUT p_is_valid BOOLEAN        -- 输出验证结果
)
BEGIN
    DECLARE v_stored_password VARCHAR(255);
    DECLARE v_account_status VARCHAR(20);
    
    -- 初始化输出参数
    SET p_user_id = NULL;
    SET p_is_valid = FALSE;
    
    -- 根据手机号或用户名查找用户
    SELECT user_id, password, account_status 
    INTO p_user_id, v_stored_password, v_account_status
    FROM users 
    WHERE (primary_phone = p_identifier OR username = p_identifier)
      AND account_status = 'ACTIVE'
    LIMIT 1;
    
    -- 如果找到用户且密码不为空
    IF p_user_id IS NOT NULL AND v_stored_password IS NOT NULL THEN
        -- 注意：这里只是示例，实际的BCrypt验证需要在Java代码中进行
        -- 这里只做简单的字符串比较作为示例
        IF v_stored_password = p_password THEN
            SET p_is_valid = TRUE;
        END IF;
    END IF;
END //

DELIMITER ;

-- ========================================
-- 5. 创建用户密码更新的存储过程
-- ========================================

DELIMITER //

CREATE PROCEDURE UpdateUserPassword(
    IN p_user_id VARCHAR(50),
    IN p_new_password VARCHAR(255),
    OUT p_success BOOLEAN
)
BEGIN
    DECLARE v_count INT DEFAULT 0;
    
    -- 初始化输出参数
    SET p_success = FALSE;
    
    -- 检查用户是否存在
    SELECT COUNT(*) INTO v_count 
    FROM users 
    WHERE user_id = p_user_id AND account_status = 'ACTIVE';
    
    -- 如果用户存在，更新密码
    IF v_count > 0 THEN
        UPDATE users 
        SET password = p_new_password,
            password_updated_at = NOW()
        WHERE user_id = p_user_id;
        
        SET p_success = TRUE;
    END IF;
END //

DELIMITER ;

-- ========================================
-- 执行完成提示
-- ========================================
SELECT '用户表密码字段添加完成！' as message;
SELECT '默认密码已设置为: 123456' as default_password;
SELECT '请提醒用户及时修改默认密码！' as security_notice;
