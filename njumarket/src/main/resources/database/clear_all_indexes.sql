-- ========================================
-- 清空表的所有索引（保留主键索引）
-- Clear All Indexes (Keep Primary Key)
-- ========================================
-- 
-- 使用说明：
-- 1. 此脚本会删除指定表的所有非主键索引
-- 2. 主键索引（PRIMARY）不会被删除
-- 3. 执行前请备份数据库
-- 
-- 使用方法：
-- 1. 修改下面的表名（table_name）
-- 2. 执行此脚本
-- 3. 或者使用下面的查询语句生成删除索引的SQL
-- ========================================

SET NAMES utf8mb4;

-- ========================================
-- 方法1：手动删除指定表的索引
-- ========================================

-- 示例：删除 orders 表的所有非主键索引
-- 注意：需要先查询出所有索引名称，然后逐个删除

-- 查看 orders 表的所有索引
-- SHOW INDEX FROM orders;

-- 删除 orders 表的索引（示例，需要根据实际索引名称修改）
-- DROP INDEX idx_buyer_id ON orders;
-- DROP INDEX idx_seller_id ON orders;
-- DROP INDEX idx_order_status ON orders;
-- ... 其他索引

-- ========================================
-- 方法2：自动生成删除索引的SQL（推荐）
-- ========================================

-- 生成删除 orders 表所有非主键索引的SQL
SELECT CONCAT('DROP INDEX ', INDEX_NAME, ' ON ', TABLE_NAME, ';') AS drop_index_sql
FROM information_schema.STATISTICS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'orders'  -- 修改表名
  AND INDEX_NAME != 'PRIMARY'
GROUP BY TABLE_NAME, INDEX_NAME
ORDER BY TABLE_NAME, INDEX_NAME;

-- 生成删除 commodities 表所有非主键索引的SQL
SELECT CONCAT('DROP INDEX ', INDEX_NAME, ' ON ', TABLE_NAME, ';') AS drop_index_sql
FROM information_schema.STATISTICS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'commodities'  -- 修改表名
  AND INDEX_NAME != 'PRIMARY'
GROUP BY TABLE_NAME, INDEX_NAME
ORDER BY TABLE_NAME, INDEX_NAME;

-- 生成删除 users 表所有非主键索引的SQL
SELECT CONCAT('DROP INDEX ', INDEX_NAME, ' ON ', TABLE_NAME, ';') AS drop_index_sql
FROM information_schema.STATISTICS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'users'  -- 修改表名
  AND INDEX_NAME != 'PRIMARY'
GROUP BY TABLE_NAME, INDEX_NAME
ORDER BY TABLE_NAME, INDEX_NAME;

-- 生成删除 user_profiles 表所有非主键索引的SQL
SELECT CONCAT('DROP INDEX ', INDEX_NAME, ' ON ', TABLE_NAME, ';') AS drop_index_sql
FROM information_schema.STATISTICS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'user_profiles'  -- 修改表名
  AND INDEX_NAME != 'PRIMARY'
GROUP BY TABLE_NAME, INDEX_NAME
ORDER BY TABLE_NAME, INDEX_NAME;

-- ========================================
-- 方法3：查看所有表的索引信息
-- ========================================

-- 查看指定数据库所有表的索引
SELECT 
    TABLE_NAME,
    INDEX_NAME,
    GROUP_CONCAT(COLUMN_NAME ORDER BY SEQ_IN_INDEX) AS COLUMNS,
    INDEX_TYPE,
    NON_UNIQUE
FROM information_schema.STATISTICS
WHERE TABLE_SCHEMA = DATABASE()
  AND INDEX_NAME != 'PRIMARY'
GROUP BY TABLE_NAME, INDEX_NAME, INDEX_TYPE, NON_UNIQUE
ORDER BY TABLE_NAME, INDEX_NAME;

-- ========================================
-- 方法4：批量删除所有表的非主键索引（谨慎使用！）
-- ========================================

-- ⚠️ 警告：此方法会删除数据库中所有表的非主键索引！
-- 执行前请确保已备份数据库！

/*
-- 生成删除所有表索引的SQL（先查看，确认后再执行）
SELECT CONCAT('DROP INDEX ', INDEX_NAME, ' ON ', TABLE_NAME, ';') AS drop_index_sql
FROM information_schema.STATISTICS
WHERE TABLE_SCHEMA = DATABASE()
  AND INDEX_NAME != 'PRIMARY'
GROUP BY TABLE_NAME, INDEX_NAME
ORDER BY TABLE_NAME, INDEX_NAME;
*/

-- ========================================
-- 实际操作示例：清空 orders 表的所有索引
-- ========================================

-- 步骤1：查看当前索引
SHOW INDEX FROM orders;

-- 步骤2：生成删除索引的SQL
SELECT CONCAT('DROP INDEX ', INDEX_NAME, ' ON orders;') AS drop_index_sql
FROM information_schema.STATISTICS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'orders'
  AND INDEX_NAME != 'PRIMARY'
GROUP BY INDEX_NAME;

-- 步骤3：复制生成的SQL并执行（示例）
/*
DROP INDEX idx_buyer_id ON orders;
DROP INDEX idx_seller_id ON orders;
DROP INDEX idx_order_status ON orders;
DROP INDEX idx_buyer_visibility ON orders;
DROP INDEX idx_seller_visibility ON orders;
-- ... 其他索引
*/

-- ========================================
-- 实际操作示例：清空 commodities 表的所有索引
-- ========================================

-- 步骤1：查看当前索引
SHOW INDEX FROM commodities;

-- 步骤2：生成删除索引的SQL
SELECT CONCAT('DROP INDEX ', INDEX_NAME, ' ON commodities;') AS drop_index_sql
FROM information_schema.STATISTICS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'commodities'
  AND INDEX_NAME != 'PRIMARY'
GROUP BY INDEX_NAME;

-- 步骤3：复制生成的SQL并执行

-- ========================================
-- 注意事项
-- ========================================

/*
1. 删除索引前请备份数据库
2. 删除索引后，相关查询性能会下降
3. 建议在业务低峰期执行
4. 删除索引后，可以重新创建优化后的索引
5. 主键索引（PRIMARY）不会被删除，这是正确的行为
*/

-- ========================================
-- 验证索引是否已删除
-- ========================================

-- 查看指定表的索引（应该只剩主键索引）
SHOW INDEX FROM orders;
SHOW INDEX FROM commodities;
SHOW INDEX FROM users;
SHOW INDEX FROM user_profiles;

