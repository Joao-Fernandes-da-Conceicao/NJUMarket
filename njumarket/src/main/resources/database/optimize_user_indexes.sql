-- ============================================================
-- User 和 UserProfile 表索引优化脚本
-- ============================================================
-- 目的：优化用户头像和昵称的查询性能
-- 适用场景：批量查询用户资料、JOIN查询、高频头像昵称访问
-- ============================================================

-- ============================================================
-- 1. UserProfile 表优化
-- ============================================================

-- 1.1 创建覆盖索引（Covering Index）用于头像和昵称查询
-- 说明：包含 userId, nickname, avatar 三个字段，避免回表查询
-- 使用场景：批量查询用户资料时，只需要这三个字段，可以直接从索引获取
DROP INDEX IF EXISTS idx_user_profile_nickname_avatar ON user_profiles;

CREATE INDEX idx_user_profile_nickname_avatar 
ON user_profiles(user_id, nickname, avatar);

-- 1.2 优化昵称模糊查询（如果经常需要根据昵称搜索）
-- 说明：对于 LIKE '%nickname%' 查询，虽然索引效果有限，但可以优化前缀匹配
-- 注意：如果昵称查询频率不高，可以跳过此索引
-- DROP INDEX IF EXISTS idx_user_profile_nickname ON user_profiles;
-- CREATE INDEX idx_user_profile_nickname ON user_profiles(nickname);

-- ============================================================
-- 2. Users 表优化
-- ============================================================

-- 2.1 创建联合索引用于登录查询优化
-- 说明：优化 findByUsernameOrPhone 查询，同时支持 primaryPhone 和 username 查询
-- 注意：由于 primaryPhone 和 username 已有唯一索引，此索引主要用于 OR 查询优化
-- 如果 OR 查询频率不高，可以跳过此索引
-- DROP INDEX IF EXISTS idx_user_login ON users;
-- CREATE INDEX idx_user_login ON users(primary_phone, username);

-- 2.2 创建联合索引用于账户状态 + 注册时间查询
-- 说明：管理端经常需要按账户状态筛选并排序，此索引可以优化此类查询
-- 使用场景：AdminServiceImpl.listUsers() 中按 accountStatus 筛选并按 registerTime 排序
DROP INDEX IF EXISTS idx_user_status_register_time ON users;

CREATE INDEX idx_user_status_register_time 
ON users(account_status, register_time DESC);

-- ============================================================
-- 3. JOIN 查询优化
-- ============================================================

-- 3.1 UserProfile 表的 user_id 索引已经存在（uk_user_id）
-- 说明：JOIN 查询时，user_profiles.user_id 已经有唯一索引，性能良好
-- 无需额外创建索引

-- 3.2 如果经常需要 JOIN 查询并筛选活跃用户
-- 说明：创建联合索引优化"查询活跃用户及其Profile"的场景
-- 注意：如果此类查询频率不高，可以跳过
-- DROP INDEX IF EXISTS idx_user_active_profile ON users;
-- CREATE INDEX idx_user_active_profile ON users(account_status, user_id) 
-- WHERE account_status = 'ACTIVE';

-- ============================================================
-- 4. 索引使用说明
-- ============================================================

-- 4.1 批量查询优化（findByUserIdIn）
-- 使用索引：uk_user_id (user_profiles.user_id)
-- 性能：O(log n) 查找，对于批量查询非常高效

-- 4.2 覆盖索引优化（idx_user_profile_nickname_avatar）
-- 使用场景：只需要 userId, nickname, avatar 时
-- 性能：直接从索引获取数据，无需回表，性能提升明显

-- 4.3 JOIN 查询优化
-- UserRepository 中的 LEFT JOIN FETCH 查询：
-- - findByPrimaryPhone: 使用 uk_primary_phone 索引
-- - findByUsername: 使用 uk_username 索引
-- - findByUsernameOrPhone: 使用 uk_primary_phone 和 uk_username 索引

-- 4.4 管理端查询优化
-- AdminServiceImpl.listUsers():
-- - 按 accountStatus 筛选：使用 idx_account_status 索引
-- - 按 registerTime 排序：使用 idx_user_status_register_time 联合索引

-- ============================================================
-- 5. 索引维护建议
-- ============================================================

-- 5.1 定期检查索引使用情况
-- 执行以下 SQL 查看索引使用统计：
-- SELECT * FROM sys.schema_unused_indexes WHERE object_schema = 'your_database_name';

-- 5.2 监控索引大小
-- 执行以下 SQL 查看索引大小：
-- SELECT 
--     TABLE_NAME,
--     INDEX_NAME,
--     ROUND(STAT_VALUE * @@innodb_page_size / 1024 / 1024, 2) AS 'Index Size (MB)'
-- FROM 
--     mysql.innodb_index_stats
-- WHERE 
--     STAT_NAME = 'size' 
--     AND DATABASE_NAME = 'your_database_name'
--     AND TABLE_NAME IN ('users', 'user_profiles')
-- ORDER BY 
--     STAT_VALUE DESC;

-- 5.3 如果索引使用率低，可以考虑删除
-- 注意：删除索引前请先确认该索引确实未被使用

-- ============================================================
-- 6. 性能测试建议
-- ============================================================

-- 6.1 测试批量查询性能
-- EXPLAIN SELECT user_id, nickname, avatar 
-- FROM user_profiles 
-- WHERE user_id IN ('user1', 'user2', ..., 'user100');

-- 6.2 测试 JOIN 查询性能
-- EXPLAIN SELECT u.*, up.nickname, up.avatar 
-- FROM users u 
-- LEFT JOIN user_profiles up ON u.user_id = up.user_id 
-- WHERE u.primary_phone = '13800000001';

-- 6.3 测试管理端查询性能
-- EXPLAIN SELECT u.* 
-- FROM users u 
-- WHERE u.account_status = 'ACTIVE' 
-- ORDER BY u.register_time DESC 
-- LIMIT 20;

-- ============================================================
-- 7. 注意事项
-- ============================================================

-- 7.1 索引不是越多越好
-- - 每个索引都会增加 INSERT/UPDATE/DELETE 的开销
-- - 索引会占用额外的存储空间
-- - 只创建真正需要的索引

-- 7.2 覆盖索引的选择
-- - idx_user_profile_nickname_avatar 索引包含常用字段
-- - 如果查询的字段经常变化，可能需要调整索引字段

-- 7.3 定期重新评估
-- - 随着业务发展，查询模式可能变化
-- - 定期检查索引使用情况，删除不必要的索引

-- ============================================================
-- 执行完成
-- ============================================================

