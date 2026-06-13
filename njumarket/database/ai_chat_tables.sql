-- AI 会话与消息（权威存储，供 njumarket-service-ai）
-- 在已有库 nju_market 下执行（与 admin 端 ai_conversations 对齐）

CREATE TABLE IF NOT EXISTS nju_market.ai_conversations (
    conversation_id VARCHAR(50) PRIMARY KEY,
    user_id         VARCHAR(50) NOT NULL,
    title           VARCHAR(200),
    message_count   INTEGER DEFAULT 0,
    status          VARCHAR(20) DEFAULT 'ACTIVE',
    -- 当前窗口内 user+assistant 消息条数（不含【历史摘要】）；用于恢复 ChatMemory 与归纳时机
    window_message_count INTEGER NOT NULL DEFAULT 0,
    -- 持久化后的【历史摘要】正文（不含前缀）；与 window_message_count 配合还原状态
    memory_summary  TEXT,
    created_at      TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS nju_market.ai_messages (
    message_id                 VARCHAR(64) PRIMARY KEY,
    conversation_id            VARCHAR(50) NOT NULL REFERENCES nju_market.ai_conversations (conversation_id) ON DELETE CASCADE,
    user_id                    VARCHAR(50) NOT NULL,
    role                       VARCHAR(20) NOT NULL,
    content                    TEXT,
    recommended_commodity_ids  TEXT,
    created_at                 TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_ai_messages_conv_created ON nju_market.ai_messages (conversation_id, created_at);
