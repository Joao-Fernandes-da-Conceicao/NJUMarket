-- ========================================
-- 添加联系人/聊天功能的联合索引
-- 用于优化对话列表和消息查询性能
-- ========================================

-- 为 conversations 表添加联合索引
ALTER TABLE `conversations` 
ADD INDEX `idx_buyer_time` (`buyer_id`, `last_message_time`),
ADD INDEX `idx_seller_time` (`seller_id`, `last_message_time`);

-- 为 messages 表添加联合索引
ALTER TABLE `messages` 
ADD INDEX `idx_sender_time` (`sender_id`, `created_at`),
ADD INDEX `idx_receiver_time` (`receiver_id`, `created_at`);

-- ========================================
-- 索引说明
-- ========================================
/*
1. conversations 表联合索引：
   - idx_buyer_time (buyer_id, last_message_time)
     用途：快速查询买家的对话列表，按最后消息时间排序
     场景：买家查看消息中心
   
   - idx_seller_time (seller_id, last_message_time)
     用途：快速查询卖家的对话列表，按最后消息时间排序
     场景：卖家查看消息中心

2. messages 表联合索引：
   - idx_sender_time (sender_id, created_at)
     用途：快速查询发送者的历史消息，按时间排序
     场景：发送消息历史查询
   
   - idx_receiver_time (receiver_id, created_at)
     用途：快速查询接收者的未读消息，按时间排序
     场景：接收未读消息列表，分页查询

3. 性能优势：
   - 减少回表查询次数
   - 支持高效的排序和分页操作
   - 提升大量数据场景下的查询速度
*/

