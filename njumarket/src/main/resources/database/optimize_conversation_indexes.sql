-- ============================================
-- 对话表（conversations）索引优化
-- ============================================
-- 优化目标：解决OR条件导致的索引失效问题
-- 优化方案：分别查询userId1和userId2，避免OR条件
-- ============================================

-- 1. 查看现有索引
SHOW INDEX FROM conversations;

-- 2. 创建优化索引（用于userId1查询）
-- 索引字段：user_id_1, status, user_1_visibility, user_1_last_message_time
-- 用途：优化用户作为userId1的对话查询，支持按用户级别最后消息时间排序
CREATE INDEX idx_user1_status_visibility_time 
ON conversations(user_id_1, status, user_1_visibility, user_1_last_message_time DESC);

-- 3. 创建优化索引（用于userId2查询）
-- 索引字段：user_id_2, status, user_2_visibility, user_2_last_message_time
-- 用途：优化用户作为userId2的对话查询，支持按用户级别最后消息时间排序
CREATE INDEX idx_user2_status_visibility_time 
ON conversations(user_id_2, status, user_2_visibility, user_2_last_message_time DESC);

-- 4. 验证新索引已创建
SHOW INDEX FROM conversations;

-- ============================================
-- 索引说明
-- ============================================
-- 1. idx_user1_status_visibility_time
--    - 覆盖查询：WHERE userId1 = ? AND status = ? AND user1Visibility = true
--    - 支持排序：ORDER BY user1LastMessageTime DESC
--    - 避免全表扫描，提高userId1查询性能
--
-- 2. idx_user2_status_visibility_time
--    - 覆盖查询：WHERE userId2 = ? AND status = ? AND user2Visibility = true
--    - 支持排序：ORDER BY user2LastMessageTime DESC
--    - 避免全表扫描，提高userId2查询性能
--
-- 3. 优化效果
--    - 避免OR条件导致的索引失效
--    - 每个查询都能充分利用索引
--    - 支持数据库层面的排序，减少内存排序开销
--
-- 4. 注意事项
--    - 这两个索引是新增的，不会影响现有功能
--    - 索引会占用一定的存储空间，但查询性能提升明显
--    - 如果数据量很大，可以考虑在业务低峰期创建索引
-- ============================================

