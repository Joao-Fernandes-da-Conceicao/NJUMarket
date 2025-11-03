-- ========================================
-- 为会话表添加用户级别的最后消息字段
-- 解决"最后消息被删除但仍显示"的问题
-- ========================================

SET NAMES utf8mb4;

-- ========================================
-- 添加用户1和用户2的最后消息字段
-- ========================================

ALTER TABLE conversations 
ADD COLUMN user_1_last_message_content TEXT COMMENT '用户1可见的最后一条消息内容',
ADD COLUMN user_1_last_message_time DATETIME COMMENT '用户1可见的最后一条消息时间',
ADD COLUMN user_2_last_message_content TEXT COMMENT '用户2可见的最后一条消息内容',
ADD COLUMN user_2_last_message_time DATETIME COMMENT '用户2可见的最后一条消息时间';

-- ========================================
-- 初始化数据：将原有最后消息复制到user1和user2字段
-- 注意：目前没有任何数据被删除，所以可以安全地复制
-- ========================================

UPDATE conversations
SET 
    user_1_last_message_content = last_message_content,
    user_1_last_message_time = last_message_time,
    user_2_last_message_content = last_message_content,
    user_2_last_message_time = last_message_time
WHERE last_message_content IS NOT NULL AND last_message_time IS NOT NULL;

-- ========================================
-- 说明
-- ========================================

/*
字段用途：
1. user_1_last_message_content/time: 用户1（userId1）可见的最后一条消息（过滤用户1删除的）
2. user_2_last_message_content/time: 用户2（userId2）可见的最后一条消息（过滤用户2删除的）
3. last_message_content/time: 保留原有字段，用于管理后台（不过滤，显示真实最新消息）

更新逻辑：
- 发送消息时：更新两个用户的字段（如果消息对用户可见）
- 删除消息时：如果删除的是最后一条，查询倒数第二条并更新对应用户字段
- 查询时：用户端使用user1/user2字段，管理端使用last_message字段
*/

-- ========================================
-- 脚本执行完成
-- ========================================

