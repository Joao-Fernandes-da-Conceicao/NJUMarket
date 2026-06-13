-- 已有库升级：为 ai_conversations 增加窗口与摘要字段（若已存在可忽略报错）
ALTER TABLE nju_market.ai_conversations ADD COLUMN IF NOT EXISTS window_message_count INTEGER NOT NULL DEFAULT 0;
ALTER TABLE nju_market.ai_conversations ADD COLUMN IF NOT EXISTS memory_summary TEXT;
