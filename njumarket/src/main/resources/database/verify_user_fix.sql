-- ========================================
-- 用户数据修复验证脚本
-- User Data Fix Verification Script
-- ========================================

-- 设置字符集和排序规则
SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ========================================
-- 1. 验证用户和用户档案的关联关系
-- ========================================

-- 检查所有用户是否都有对应的用户档案
SELECT 
    'Users without profiles' as check_type,
    COUNT(*) as count
FROM users u
LEFT JOIN user_profiles up ON u.user_id = up.user_id
WHERE up.user_id IS NULL;

-- 检查用户档案是否都有对应的用户
SELECT 
    'Profiles without users' as check_type,
    COUNT(*) as count
FROM user_profiles up
LEFT JOIN users u ON up.user_id = u.user_id
WHERE u.user_id IS NULL;

-- ========================================
-- 2. 验证数据一致性
-- ========================================

-- 检查重复数据
SELECT 
    'Duplicate phones' as check_type,
    COUNT(*) as count
FROM (
    SELECT primary_phone, COUNT(*) as cnt
    FROM users 
    GROUP BY primary_phone 
    HAVING cnt > 1
) as dup_phones;

SELECT 
    'Duplicate usernames' as check_type,
    COUNT(*) as count
FROM (
    SELECT username, COUNT(*) as cnt
    FROM users 
    WHERE username IS NOT NULL AND username != ''
    GROUP BY username 
    HAVING cnt > 1
) as dup_usernames;

SELECT 
    'Duplicate user profiles' as check_type,
    COUNT(*) as count
FROM (
    SELECT user_id, COUNT(*) as cnt
    FROM user_profiles 
    GROUP BY user_id 
    HAVING cnt > 1
) as dup_profiles;

-- ========================================
-- 3. 验证用户档案数据完整性
-- ========================================

-- 检查用户档案中的必要字段
SELECT 
    'Profiles with null nickname' as check_type,
    COUNT(*) as count
FROM user_profiles 
WHERE nickname IS NULL OR nickname = '';

SELECT 
    'Profiles with null credit_score' as check_type,
    COUNT(*) as count
FROM user_profiles 
WHERE credit_score IS NULL;

SELECT 
    'Profiles with null buyer_rating' as check_type,
    COUNT(*) as count
FROM user_profiles 
WHERE buyer_rating IS NULL;

SELECT 
    'Profiles with null seller_rating' as check_type,
    COUNT(*) as count
FROM user_profiles 
WHERE seller_rating IS NULL;

-- ========================================
-- 4. 显示修复后的数据统计
-- ========================================

SELECT '=== 数据统计 ===' as info;

SELECT 
    'Total users' as metric,
    COUNT(*) as value
FROM users;

SELECT 
    'Total user profiles' as metric,
    COUNT(*) as value
FROM user_profiles;

SELECT 
    'Users with profiles' as metric,
    COUNT(*) as value
FROM users u
INNER JOIN user_profiles up ON u.user_id = up.user_id;

SELECT 
    'Active users' as metric,
    COUNT(*) as value
FROM users 
WHERE account_status = 'ACTIVE';

SELECT 
    'Users with avatars' as metric,
    COUNT(*) as value
FROM user_profiles 
WHERE avatar IS NOT NULL AND avatar != '';

SELECT 
    'Users with nicknames' as metric,
    COUNT(*) as value
FROM user_profiles 
WHERE nickname IS NOT NULL AND nickname != '';

-- ========================================
-- 5. 显示示例数据
-- ========================================

SELECT '=== 示例用户数据 ===' as info;

SELECT 
    u.user_id,
    u.primary_phone,
    u.username,
    u.account_status,
    u.register_time,
    up.nickname,
    up.avatar,
    up.credit_score,
    up.buyer_rating,
    up.seller_rating,
    up.vip_level
FROM users u
LEFT JOIN user_profiles up ON u.user_id = up.user_id
ORDER BY u.register_time DESC
LIMIT 5;

-- 恢复外键检查
SET FOREIGN_KEY_CHECKS = 1;

-- ========================================
-- 验证完成
-- ========================================
SELECT 'User data verification completed!' as status;
