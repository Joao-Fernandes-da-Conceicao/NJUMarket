-- 为会话表添加双方可见性字段
-- 用于用户删除会话功能（软删除，仅影响消息界面查询）
-- 默认值为1（true），表示可见

ALTER TABLE conversations
ADD COLUMN user_1_visibility TINYINT(1) NOT NULL DEFAULT 1 COMMENT '用户1可见性：1=可见，0=不可见（用户删除）',
ADD COLUMN user_2_visibility TINYINT(1) NOT NULL DEFAULT 1 COMMENT '用户2可见性：1=可见，0=不可见（用户删除）';

-- 为已有数据设置默认值（已有会话默认为可见）
UPDATE conversations
SET user_1_visibility = 1, user_2_visibility = 1
WHERE user_1_visibility IS NULL OR user_2_visibility IS NULL;

