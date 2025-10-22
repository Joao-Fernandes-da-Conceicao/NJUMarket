-- ========================================
-- 用户表添加密码字段更新脚本（安全模式兼容版本）
-- Add Password Field to Users Table Update Script (Safe Mode Compatible)
-- ========================================

-- 设置字符集
SET NAMES utf8mb4;

-- ========================================
-- 1. 为用户表添加密码相关字段
-- ========================================

-- 添加用户名字段（可选，用于账号密码登录）
ALTER TABLE `users` 
ADD COLUMN `username` varchar(50) DEFAULT NULL COMMENT '用户名(可选)' AFTER `primary_phone`;

-- 添加密码字段
ALTER TABLE `users` 
ADD COLUMN `password` varchar(255) DEFAULT NULL COMMENT '用户密码(加密存储)' AFTER `username`;

-- 添加密码最后修改时间字段
ALTER TABLE `users` 
ADD COLUMN `password_updated_at` datetime DEFAULT NULL COMMENT '密码最后修改时间' AFTER `password`;

-- 添加密码相关的索引
ALTER TABLE `users` 
ADD UNIQUE KEY `uk_username` (`username`);

-- ========================================
-- 2. 安全模式下更新现有数据
-- ========================================

-- 方法1: 使用主键逐个更新（推荐）
-- 获取所有用户ID并为每个用户设置默认密码
-- 注意：在实际执行时，请根据具体的用户ID进行调整

-- 示例：为特定用户设置密码（请根据实际情况修改user_id）
-- UPDATE `users` 
-- SET `password` = '$2a$10$N.zmdr9k7uOIQzUHPPLOPOxrOVJ2eswjzfoy9rI8.sChyZwta7aaa',
--     `password_updated_at` = NOW()
-- WHERE `user_id` = 'YOUR_USER_ID_HERE' AND `password` IS NULL;

-- 方法2: 创建存储过程批量更新
DELIMITER //

CREATE PROCEDURE UpdateAllUserPasswords()
BEGIN
    DECLARE done INT DEFAULT FALSE;
    DECLARE v_user_id VARCHAR(50);
    DECLARE user_cursor CURSOR FOR 
        SELECT user_id FROM users WHERE password IS NULL;
    DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;
    
    -- 打开游标
    OPEN user_cursor;
    
    -- 循环处理每个用户
    read_loop: LOOP
        FETCH user_cursor INTO v_user_id;
        IF done THEN
            LEAVE read_loop;
        END IF;
        
        -- 为每个用户设置默认密码
        UPDATE users 
        SET password = '$2a$10$N.zmdr9k7uOIQzUHPPLOPOxrOVJ2eswjzfoy9rI8.sChyZwta7aaa',
            password_updated_at = NOW()
        WHERE user_id = v_user_id;
        
    END LOOP;
    
    -- 关闭游标
    CLOSE user_cursor;
END //

DELIMITER ;

-- 执行存储过程来更新所有用户密码
-- CALL UpdateAllUserPasswords();

-- 删除临时存储过程
-- DROP PROCEDURE IF EXISTS UpdateAllUserPasswords;

-- ========================================
-- 3. 手动更新方式（如果需要）
-- ========================================

-- 如果您确定要为所有用户设置默认密码，可以执行以下语句：
-- 注意：这需要临时禁用安全模式

-- 步骤1: 禁用安全更新模式
-- SET SQL_SAFE_UPDATES = 0;

-- 步骤2: 批量更新
-- UPDATE `users` 
-- SET `password` = '$2a$10$N.zmdr9k7uOIQzUHPPLOPOxrOVJ2eswjzfoy9rI8.sChyZwta7aaa',
--     `password_updated_at` = NOW()
-- WHERE `password` IS NULL;

-- 步骤3: 重新启用安全更新模式
-- SET SQL_SAFE_UPDATES = 1;

-- ========================================
-- 4. 验证更新结果
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

-- 统计密码设置情况
SELECT 
    COUNT(*) as total_users,
    SUM(CASE WHEN password IS NOT NULL THEN 1 ELSE 0 END) as users_with_password,
    SUM(CASE WHEN password IS NULL THEN 1 ELSE 0 END) as users_without_password
FROM `users`;

-- ========================================
-- 5. 创建密码验证相关的存储过程
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
-- 6. 创建用户密码更新的存储过程
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
SELECT '请根据需要选择合适的密码更新方式' as instruction;
SELECT '默认密码: 123456 (如果执行了密码更新)' as default_password;
SELECT '请提醒用户及时修改默认密码！' as security_notice;
