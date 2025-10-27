-- 创建对话表（Conversation）
CREATE TABLE IF NOT EXISTS conversations (
    conversation_id VARCHAR(255) PRIMARY KEY COMMENT '对话ID',
    buyer_id VARCHAR(50) NOT NULL COMMENT '买家ID',
    seller_id VARCHAR(50) NOT NULL COMMENT '卖家ID',
    commodity_id VARCHAR(255) COMMENT '关联商品ID（可选）',
    order_id VARCHAR(255) COMMENT '关联订单ID（可选）',
    last_message_content TEXT COMMENT '最后一条消息内容',
    last_message_time TIMESTAMP NULL COMMENT '最后消息时间',
    buyer_unread_count INT DEFAULT 0 COMMENT '买家未读消息数',
    seller_unread_count INT DEFAULT 0 COMMENT '卖家未读消息数',
    status VARCHAR(20) DEFAULT 'ACTIVE' COMMENT '对话状态：ACTIVE-活跃，ARCHIVED-已归档，DELETED-已删除',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_buyer_id (buyer_id),
    INDEX idx_seller_id (seller_id),
    INDEX idx_commodity_id (commodity_id),
    INDEX idx_order_id (order_id),
    INDEX idx_last_message_time (last_message_time),
    -- 联合索引：买家+时间，用于快速查询买家的对话列表
    INDEX idx_buyer_time (buyer_id, last_message_time),
    -- 联合索引：卖家+时间，用于快速查询卖家的对话列表
    INDEX idx_seller_time (seller_id, last_message_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='对话表';

-- 创建消息表（Message）
CREATE TABLE IF NOT EXISTS messages (
    message_id VARCHAR(255) PRIMARY KEY COMMENT '消息ID',
    conversation_id VARCHAR(255) NOT NULL COMMENT '对话ID',
    sender_id VARCHAR(50) NOT NULL COMMENT '发送者ID',
    receiver_id VARCHAR(50) NOT NULL COMMENT '接收者ID',
    message_type VARCHAR(20) DEFAULT 'TEXT' COMMENT '消息类型：TEXT-文本，IMAGE-图片，COMMODITY-商品卡片，ORDER-订单卡片',
    content TEXT NOT NULL COMMENT '消息内容',
    image_url VARCHAR(500) COMMENT '图片URL（当消息类型为IMAGE时）',
    commodity_snapshot JSON COMMENT '商品快照（当消息类型为COMMODITY时）',
    order_snapshot JSON COMMENT '订单快照（当消息类型为ORDER时）',
    is_read BOOLEAN DEFAULT FALSE COMMENT '是否已读',
    read_time TIMESTAMP NULL COMMENT '已读时间',
    is_deleted BOOLEAN DEFAULT FALSE COMMENT '是否已删除',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '发送时间',
    FOREIGN KEY (conversation_id) REFERENCES conversations(conversation_id) ON DELETE CASCADE,
    INDEX idx_conversation_id (conversation_id),
    INDEX idx_sender_id (sender_id),
    INDEX idx_receiver_id (receiver_id),
    INDEX idx_created_at (created_at),
    INDEX idx_is_read (is_read),
    -- 联合索引：发送者+时间，用于快速查询发送者的消息
    INDEX idx_sender_time (sender_id, created_at),
    -- 联合索引：接收者+时间，用于快速查询接收者的消息
    INDEX idx_receiver_time (receiver_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='消息表';

-- 创建联系人黑名单表（可选功能）
CREATE TABLE IF NOT EXISTS contact_blacklist (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id VARCHAR(50) NOT NULL COMMENT '用户ID',
    blocked_user_id VARCHAR(50) NOT NULL COMMENT '被屏蔽用户ID',
    reason VARCHAR(255) COMMENT '屏蔽原因',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '屏蔽时间',
    UNIQUE KEY uk_user_blocked (user_id, blocked_user_id),
    INDEX idx_user_id (user_id),
    INDEX idx_blocked_user_id (blocked_user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='联系人黑名单表';

-- 创建消息通知设置表（可选功能）
CREATE TABLE IF NOT EXISTS message_notification_settings (
    user_id VARCHAR(50) PRIMARY KEY COMMENT '用户ID',
    enable_email_notification BOOLEAN DEFAULT TRUE COMMENT '启用邮件通知',
    enable_push_notification BOOLEAN DEFAULT TRUE COMMENT '启用推送通知',
    enable_sound BOOLEAN DEFAULT TRUE COMMENT '启用声音提醒',
    quiet_hours_start TIME COMMENT '免打扰开始时间',
    quiet_hours_end TIME COMMENT '免打扰结束时间',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='消息通知设置表';
