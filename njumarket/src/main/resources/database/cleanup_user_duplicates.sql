-- ========================================
-- 用户表重复数据清理脚本
-- User Table Duplicate Data Cleanup Script
-- ========================================

-- 设置字符集和排序规则
SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ========================================
-- 1. 备份现有数据（可选）
-- ========================================
-- CREATE TABLE users_backup AS SELECT * FROM users;
-- CREATE TABLE user_profiles_backup AS SELECT * FROM user_profiles;

-- ========================================
-- 2. 检查重复数据情况
-- ========================================

-- 检查users表中的重复手机号
SELECT 
    primary_phone, 
    COUNT(*) as count,
    GROUP_CONCAT(user_id) as user_ids
FROM users 
GROUP BY primary_phone 
HAVING COUNT(*) > 1;

-- 检查users表中的重复用户名
SELECT 
    username, 
    COUNT(*) as count,
    GROUP_CONCAT(user_id) as user_ids
FROM users 
WHERE username IS NOT NULL AND username != ''
GROUP BY username 
HAVING COUNT(*) > 1;

-- 检查没有对应user_profile的用户
SELECT u.user_id, u.primary_phone, u.username
FROM users u
LEFT JOIN user_profiles up ON u.user_id = up.user_id
WHERE up.user_id IS NULL;

-- ========================================
-- 3. 清理重复数据
-- ========================================

-- 删除重复的手机号记录（保留最新的）
DELETE u1 FROM users u1
INNER JOIN users u2 
WHERE u1.user_id < u2.user_id 
AND u1.primary_phone = u2.primary_phone;

-- 删除重复的用户名记录（保留最新的）
DELETE u1 FROM users u1
INNER JOIN users u2 
WHERE u1.user_id < u2.user_id 
AND u1.username = u2.username
AND u1.username IS NOT NULL 
AND u1.username != '';

-- ========================================
-- 4. 为没有user_profile的用户创建档案
-- ========================================

-- 为没有user_profile的用户创建默认档案
INSERT INTO user_profiles (
    profile_id,
    user_id,
    nickname,
    avatar,
    credit_score,
    buyer_rating,
    seller_rating,
    total_sales,
    total_purchases,
    vip_level
)
SELECT 
    CONCAT('PROFILE_', UNIX_TIMESTAMP(), '_', FLOOR(RAND() * 1000)) as profile_id,
    u.user_id,
    COALESCE(u.username, CONCAT('用户', SUBSTRING(u.user_id, -6))) as nickname,
    NULL as avatar,
    100 as credit_score,
    5.0 as buyer_rating,
    5.0 as seller_rating,
    0 as total_sales,
    0 as total_purchases,
    'NORMAL' as vip_level
FROM users u
LEFT JOIN user_profiles up ON u.user_id = up.user_id
WHERE up.user_id IS NULL;

-- ========================================
-- 5. 清理user_profiles表中的重复数据
-- ========================================

-- 删除重复的user_profile记录（保留最新的）
DELETE up1 FROM user_profiles up1
INNER JOIN user_profiles up2 
WHERE up1.profile_id < up2.profile_id 
AND up1.user_id = up2.user_id;

-- ========================================
-- 6. 数据验证
-- ========================================

-- 验证清理结果
SELECT 'Users count' as table_name, COUNT(*) as count FROM users
UNION ALL
SELECT 'User profiles count' as table_name, COUNT(*) as count FROM user_profiles
UNION ALL
SELECT 'Users without profiles' as table_name, COUNT(*) as count 
FROM users u LEFT JOIN user_profiles up ON u.user_id = up.user_id 
WHERE up.user_id IS NULL;

-- 检查是否还有重复数据
SELECT 'Duplicate phones' as check_type, COUNT(*) as count
FROM (
    SELECT primary_phone, COUNT(*) as cnt
    FROM users 
    GROUP BY primary_phone 
    HAVING cnt > 1
) as dup_phones;

SELECT 'Duplicate usernames' as check_type, COUNT(*) as count
FROM (
    SELECT username, COUNT(*) as cnt
    FROM users 
    WHERE username IS NOT NULL AND username != ''
    GROUP BY username 
    HAVING cnt > 1
) as dup_usernames;

SELECT 'Duplicate user profiles' as check_type, COUNT(*) as count
FROM (
    SELECT user_id, COUNT(*) as cnt
    FROM user_profiles 
    GROUP BY user_id 
    HAVING cnt > 1
) as dup_profiles;

-- ========================================
-- 7. 添加必要的索引（如果不存在）
-- ========================================

-- 确保users表有正确的索引
-- ALTER TABLE users ADD UNIQUE INDEX uk_primary_phone (primary_phone);
-- ALTER TABLE users ADD UNIQUE INDEX uk_username (username);

-- 确保user_profiles表有正确的索引
-- ALTER TABLE user_profiles ADD UNIQUE INDEX uk_user_id (user_id);

-- ========================================
-- 8. 更新统计信息
-- ========================================

-- 更新表的统计信息
ANALYZE TABLE users;
ANALYZE TABLE user_profiles;

-- 恢复外键检查
SET FOREIGN_KEY_CHECKS = 1;

-- ========================================
-- 清理完成
-- ========================================
SELECT 'User table cleanup completed successfully!' as status;
