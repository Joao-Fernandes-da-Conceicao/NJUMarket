-- ============================================
-- Conversation表索引优化迁移脚本
-- 执行时间：2025-01-XX
-- 说明：优化conversations表的索引，提升查询性能
-- ============================================

-- 1. 删除旧的索引（使用last_message_time）
DROP INDEX IF EXISTS `idx_user1_status_visibility_time` ON `conversations`;
DROP INDEX IF EXISTS `idx_user2_status_visibility_time` ON `conversations`;

-- 2. 创建新的索引（使用user_1_last_message_time和user_2_last_message_time）
-- 这些索引匹配实际查询使用的字段，可以完全覆盖查询条件，避免回表
CREATE INDEX `idx_user1_status_visibility_time` 
ON `conversations` (`user_id_1`, `status`, `user_1_visibility`, `user_1_last_message_time` DESC);

CREATE INDEX `idx_user2_status_visibility_time` 
ON `conversations` (`user_id_2`, `status`, `user_2_visibility`, `user_2_last_message_time` DESC);

-- 3. 添加包含未读数的联合索引（用于按未读数排序的场景）
-- 如果不需要按未读数排序，可以注释掉这两个索引
CREATE INDEX `idx_user1_status_visibility_count_time` 
ON `conversations` (`user_id_1`, `status`, `user_1_visibility`, `user_1_count` DESC, `user_1_last_message_time` DESC);

CREATE INDEX `idx_user2_status_visibility_count_time` 
ON `conversations` (`user_id_2`, `status`, `user_2_visibility`, `user_2_count` DESC, `user_2_last_message_time` DESC);

-- ============================================
-- 索引说明：
-- 
-- idx_user1_status_visibility_time / idx_user2_status_visibility_time:
--   - 用于按时间排序查询对话列表
--   - 完全覆盖查询条件，避免回表
--   - 匹配查询：findByUserId1AndStatusOrderByUser1LastMessageTime
--
-- idx_user1_status_visibility_count_time / idx_user2_status_visibility_count_time:
--   - 用于按未读数排序查询对话列表（未读数降序 > 时间降序）
--   - 支持查询：WHERE user_id_1 = ? AND status = ? AND user_1_visibility = 1 
--              ORDER BY user_1_count DESC, user_1_last_message_time DESC
--   - 如果不需要按未读数排序，可以删除这两个索引以节省空间
-- ============================================

