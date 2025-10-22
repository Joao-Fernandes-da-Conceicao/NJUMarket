-- ========================================
-- 南京大学校园二手交易平台数据库初始化脚本
-- NJU Campus Second-hand Trading Platform Database Initialization
-- ========================================

-- 设置字符集和排序规则
SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ========================================
-- 1. 用户表 (users)
-- ========================================
DROP TABLE IF EXISTS `users`;
CREATE TABLE `users` (
  `user_id` varchar(50) NOT NULL COMMENT '用户ID',
  `primary_phone` varchar(20) NOT NULL COMMENT '主要手机号',
  `register_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '注册时间',
  `account_status` varchar(20) NOT NULL DEFAULT 'ACTIVE' COMMENT '账户状态: ACTIVE-活跃, SUSPENDED-暂停, BANNED-封禁',
  PRIMARY KEY (`user_id`),
  UNIQUE KEY `uk_primary_phone` (`primary_phone`),
  KEY `idx_account_status` (`account_status`),
  KEY `idx_register_time` (`register_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- ========================================
-- 2. 用户档案表 (user_profiles)
-- ========================================
DROP TABLE IF EXISTS `user_profiles`;
CREATE TABLE `user_profiles` (
  `profile_id` varchar(50) NOT NULL COMMENT '档案ID',
  `user_id` varchar(50) NOT NULL COMMENT '用户ID',
  `nickname` varchar(50) DEFAULT NULL COMMENT '昵称',
  `avatar` varchar(500) DEFAULT NULL COMMENT '头像URL',
  `credit_score` int NOT NULL DEFAULT 100 COMMENT '信用分',
  `buyer_rating` decimal(3,2) DEFAULT 5.00 COMMENT '买家评分',
  `seller_rating` decimal(3,2) DEFAULT 5.00 COMMENT '卖家评分',
  `total_sales` int NOT NULL DEFAULT 0 COMMENT '总销售数',
  `total_purchases` int NOT NULL DEFAULT 0 COMMENT '总购买数',
  `vip_level` varchar(20) DEFAULT 'NORMAL' COMMENT 'VIP等级: NORMAL-普通, BRONZE-青铜, SILVER-白银, GOLD-黄金, PLATINUM-铂金',
  PRIMARY KEY (`profile_id`),
  UNIQUE KEY `uk_user_id` (`user_id`),
  KEY `idx_credit_score` (`credit_score`),
  KEY `idx_vip_level` (`vip_level`),
  CONSTRAINT `fk_user_profiles_user_id` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户档案表';

-- ========================================
-- 3. 联系方式表 (contact_info)
-- ========================================
DROP TABLE IF EXISTS `contact_info`;
CREATE TABLE `contact_info` (
  `contact_id` varchar(50) NOT NULL COMMENT '联系方式ID',
  `owner_id` varchar(50) NOT NULL COMMENT '所有者用户ID',
  `type` varchar(20) NOT NULL COMMENT '联系方式类型: PHONE-电话, EMAIL-邮箱, WECHAT-微信, QQ-QQ',
  `value_encrypted` varchar(500) NOT NULL COMMENT '加密后的联系方式值',
  PRIMARY KEY (`contact_id`),
  KEY `idx_owner_id` (`owner_id`),
  KEY `idx_type` (`type`),
  CONSTRAINT `fk_contact_info_owner_id` FOREIGN KEY (`owner_id`) REFERENCES `users` (`user_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='联系方式表';

-- ========================================
-- 4. 商品表 (commodities)
-- ========================================
DROP TABLE IF EXISTS `commodities`;
CREATE TABLE `commodities` (
  `commodity_id` varchar(50) NOT NULL COMMENT '商品ID',
  `seller_id` varchar(50) NOT NULL COMMENT '卖家用户ID',
  `title` varchar(200) NOT NULL COMMENT '商品标题',
  `description` text COMMENT '商品描述',
  `price` decimal(10,2) NOT NULL COMMENT '商品价格',
  `stock` int NOT NULL COMMENT '库存数量',
  `location` varchar(200) DEFAULT NULL COMMENT '商品位置',
  `publish_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发布时间',
  `commodity_status` varchar(20) NOT NULL DEFAULT 'DRAFT' COMMENT '商品状态: DRAFT-草稿, PUBLISHED-已发布, SOLD_OUT-售罄, REMOVED-已下架',
  `click_count` int NOT NULL DEFAULT 0 COMMENT '点击次数',
  `report_count` int NOT NULL DEFAULT 0 COMMENT '举报次数',
  PRIMARY KEY (`commodity_id`),
  KEY `idx_seller_id` (`seller_id`),
  KEY `idx_commodity_status` (`commodity_status`),
  KEY `idx_publish_time` (`publish_time`),
  KEY `idx_price` (`price`),
  KEY `idx_click_count` (`click_count`),
  CONSTRAINT `fk_commodities_seller_id` FOREIGN KEY (`seller_id`) REFERENCES `users` (`user_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='商品表';

-- ========================================
-- 5. 订单表 (orders)
-- ========================================
DROP TABLE IF EXISTS `orders`;
CREATE TABLE `orders` (
  `order_id` varchar(50) NOT NULL COMMENT '订单ID',
  `buyer_id` varchar(50) NOT NULL COMMENT '买家用户ID',
  `seller_id` varchar(50) NOT NULL COMMENT '卖家用户ID',
  `commodity_id` varchar(50) NOT NULL COMMENT '商品ID',
  `order_status` varchar(20) NOT NULL DEFAULT 'CREATED' COMMENT '订单状态: CREATED-已创建, PAID-已支付, SHIPPED-已发货, COMPLETED-已完成, CANCELLED-已取消, REFUNDED-已退款',
  `pay_amount` decimal(10,2) NOT NULL COMMENT '支付金额',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `quantity` int NOT NULL DEFAULT 1 COMMENT '购买数量',
  PRIMARY KEY (`order_id`),
  KEY `idx_buyer_id` (`buyer_id`),
  KEY `idx_seller_id` (`seller_id`),
  KEY `idx_commodity_id` (`commodity_id`),
  KEY `idx_order_status` (`order_status`),
  KEY `idx_create_time` (`create_time`),
  CONSTRAINT `fk_orders_buyer_id` FOREIGN KEY (`buyer_id`) REFERENCES `users` (`user_id`) ON DELETE CASCADE,
  CONSTRAINT `fk_orders_seller_id` FOREIGN KEY (`seller_id`) REFERENCES `users` (`user_id`) ON DELETE CASCADE,
  CONSTRAINT `fk_orders_commodity_id` FOREIGN KEY (`commodity_id`) REFERENCES `commodities` (`commodity_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='订单表';

-- ========================================
-- 6. 消息表 (messages)
-- ========================================
DROP TABLE IF EXISTS `messages`;
CREATE TABLE `messages` (
  `message_id` varchar(50) NOT NULL COMMENT '消息ID',
  `sender_id` varchar(50) NOT NULL COMMENT '发送者用户ID',
  `receiver_id` varchar(50) NOT NULL COMMENT '接收者用户ID',
  `content` text NOT NULL COMMENT '消息内容',
  `send_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发送时间',
  `message_type` varchar(20) NOT NULL DEFAULT 'TEXT' COMMENT '消息类型: TEXT-文本, IMAGE-图片, SYSTEM-系统消息, NOTIFICATION-通知',
  `is_read` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否已读: 0-未读, 1-已读',
  PRIMARY KEY (`message_id`),
  KEY `idx_sender_id` (`sender_id`),
  KEY `idx_receiver_id` (`receiver_id`),
  KEY `idx_send_time` (`send_time`),
  KEY `idx_is_read` (`is_read`),
  CONSTRAINT `fk_messages_sender_id` FOREIGN KEY (`sender_id`) REFERENCES `users` (`user_id`) ON DELETE CASCADE,
  CONSTRAINT `fk_messages_receiver_id` FOREIGN KEY (`receiver_id`) REFERENCES `users` (`user_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='消息表';

-- ========================================
-- 7. 投诉表 (complaints)
-- ========================================
DROP TABLE IF EXISTS `complaints`;
CREATE TABLE `complaints` (
  `complaint_id` varchar(50) NOT NULL COMMENT '投诉ID',
  `complainant_id` varchar(50) NOT NULL COMMENT '投诉人用户ID',
  `defendant_id` varchar(50) NOT NULL COMMENT '被投诉人用户ID',
  `related_order_id` varchar(50) DEFAULT NULL COMMENT '相关订单ID',
  `content` text NOT NULL COMMENT '投诉内容',
  `evidence_files` text COMMENT '证据文件列表(JSON格式)',
  `status` varchar(20) NOT NULL DEFAULT 'SUBMITTED' COMMENT '投诉状态: SUBMITTED-已提交, PROCESSING-处理中, RESOLVED-已解决, REJECTED-已拒绝',
  `submit_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '提交时间',
  `resolve_time` datetime DEFAULT NULL COMMENT '解决时间',
  PRIMARY KEY (`complaint_id`),
  KEY `idx_complainant_id` (`complainant_id`),
  KEY `idx_defendant_id` (`defendant_id`),
  KEY `idx_related_order_id` (`related_order_id`),
  KEY `idx_status` (`status`),
  KEY `idx_submit_time` (`submit_time`),
  CONSTRAINT `fk_complaints_complainant_id` FOREIGN KEY (`complainant_id`) REFERENCES `users` (`user_id`) ON DELETE CASCADE,
  CONSTRAINT `fk_complaints_defendant_id` FOREIGN KEY (`defendant_id`) REFERENCES `users` (`user_id`) ON DELETE CASCADE,
  CONSTRAINT `fk_complaints_related_order_id` FOREIGN KEY (`related_order_id`) REFERENCES `orders` (`order_id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='投诉表';

-- ========================================
-- 8. 审核记录表 (audit_records)
-- ========================================
DROP TABLE IF EXISTS `audit_records`;
CREATE TABLE `audit_records` (
  `record_id` varchar(50) NOT NULL COMMENT '记录ID',
  `commodity_id` varchar(50) NOT NULL COMMENT '商品ID',
  `reviewer_id` varchar(50) DEFAULT NULL COMMENT '审核员ID',
  `reason` text COMMENT '审核原因',
  `decision` varchar(20) NOT NULL DEFAULT 'PENDING' COMMENT '审核决定: APPROVED-通过, REJECTED-拒绝, PENDING-待审核',
  `audit_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '审核时间',
  `audit_type` varchar(20) NOT NULL DEFAULT 'AUTO' COMMENT '审核类型: AUTO-自动审核, MANUAL-人工审核',
  PRIMARY KEY (`record_id`),
  KEY `idx_commodity_id` (`commodity_id`),
  KEY `idx_reviewer_id` (`reviewer_id`),
  KEY `idx_decision` (`decision`),
  KEY `idx_audit_time` (`audit_time`),
  KEY `idx_audit_type` (`audit_type`),
  CONSTRAINT `fk_audit_records_commodity_id` FOREIGN KEY (`commodity_id`) REFERENCES `commodities` (`commodity_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='审核记录表';

-- ========================================
-- 9. 封禁记录表 (ban_records)
-- ========================================
DROP TABLE IF EXISTS `ban_records`;
CREATE TABLE `ban_records` (
  `ban_id` varchar(50) NOT NULL COMMENT '封禁ID',
  `user_id` varchar(50) NOT NULL COMMENT '用户ID',
  `phone` varchar(20) DEFAULT NULL COMMENT '手机号',
  `device_id` varchar(100) DEFAULT NULL COMMENT '设备ID',
  `real_name_id` varchar(50) DEFAULT NULL COMMENT '实名ID',
  `reason` text NOT NULL COMMENT '封禁原因',
  `start_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '封禁开始时间',
  `end_at` datetime DEFAULT NULL COMMENT '封禁结束时间',
  `ban_type` varchar(20) NOT NULL COMMENT '封禁类型: TEMPORARY-临时, PERMANENT-永久, DEVICE-设备, PHONE-手机, REAL_NAME-实名',
  `is_active` tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否生效: 0-无效, 1-有效',
  PRIMARY KEY (`ban_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_phone` (`phone`),
  KEY `idx_device_id` (`device_id`),
  KEY `idx_ban_type` (`ban_type`),
  KEY `idx_is_active` (`is_active`),
  KEY `idx_start_at` (`start_at`),
  KEY `idx_end_at` (`end_at`),
  CONSTRAINT `fk_ban_records_user_id` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='封禁记录表';

-- ========================================
-- 10. 用户活动记录表 (user_activity_records)
-- ========================================
DROP TABLE IF EXISTS `user_activity_records`;
CREATE TABLE `user_activity_records` (
  `record_id` varchar(50) NOT NULL COMMENT '记录ID',
  `user_id` varchar(50) NOT NULL COMMENT '用户ID',
  `activity_type` varchar(50) NOT NULL COMMENT '活动类型: LOGIN-登录, PUBLISH-发布, PURCHASE-购买, BROWSE-浏览, SEARCH-搜索',
  `activity_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '活动时间',
  `activity_data` text COMMENT '活动数据(JSON格式)',
  `ip_address` varchar(50) DEFAULT NULL COMMENT 'IP地址',
  `user_agent` varchar(500) DEFAULT NULL COMMENT '用户代理',
  PRIMARY KEY (`record_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_activity_type` (`activity_type`),
  KEY `idx_activity_time` (`activity_time`),
  KEY `idx_ip_address` (`ip_address`),
  CONSTRAINT `fk_user_activity_records_user_id` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户活动记录表';

-- ========================================
-- 11. 促销活动表 (promotions)
-- ========================================
DROP TABLE IF EXISTS `promotions`;
CREATE TABLE `promotions` (
  `promotion_id` varchar(50) NOT NULL COMMENT '促销ID',
  `user_id` varchar(50) DEFAULT NULL COMMENT '用户ID(用户专属促销)',
  `type` varchar(20) NOT NULL COMMENT '促销类型: COUPON-优惠券, FULL_REDUCE-满减, LIMITED_DISCOUNT-限时折扣',
  `rules` text COMMENT '促销规则(JSON格式)',
  `start_time` datetime NOT NULL COMMENT '开始时间',
  `end_time` datetime NOT NULL COMMENT '结束时间',
  `status` varchar(20) NOT NULL DEFAULT 'INACTIVE' COMMENT '状态: ACTIVE-活跃, INACTIVE-未激活, EXPIRED-已过期, USED-已使用',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `usage_count` int NOT NULL DEFAULT 0 COMMENT '使用次数',
  `max_usage` int DEFAULT NULL COMMENT '最大使用次数',
  PRIMARY KEY (`promotion_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_type` (`type`),
  KEY `idx_status` (`status`),
  KEY `idx_start_time` (`start_time`),
  KEY `idx_end_time` (`end_time`),
  KEY `idx_create_time` (`create_time`),
  CONSTRAINT `fk_promotions_user_id` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='促销活动表';

-- ========================================
-- 12. 数据统计表 (data_statistics)
-- ========================================
DROP TABLE IF EXISTS `data_statistics`;
CREATE TABLE `data_statistics` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '自增主键',
  `cycle` varchar(20) NOT NULL COMMENT '统计周期: DAILY-日, WEEKLY-周, MONTHLY-月, YEARLY-年',
  `dimension` varchar(50) NOT NULL COMMENT '统计维度: SALES-销售, USER_ACTIVITY-用户活动, COMMODITY_VIEWS-商品浏览, REVENUE-收入',
  `value` decimal(15,2) NOT NULL COMMENT '统计值',
  `category` varchar(50) DEFAULT NULL COMMENT '分类',
  `date_key` varchar(20) NOT NULL COMMENT '日期键(格式: YYYY-MM-DD 或 YYYY-MM 或 YYYY)',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `extra_data` text COMMENT '额外数据(JSON格式)',
  PRIMARY KEY (`id`),
  KEY `idx_cycle` (`cycle`),
  KEY `idx_dimension` (`dimension`),
  KEY `idx_date_key` (`date_key`),
  KEY `idx_create_time` (`create_time`),
  KEY `idx_category` (`category`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='数据统计表';

-- ========================================
-- 初始化数据
-- ========================================

-- 插入测试用户数据
INSERT INTO `users` (`user_id`, `primary_phone`, `account_status`) VALUES
('user_001', '13800138001', 'ACTIVE'),
('user_002', '13800138002', 'ACTIVE'),
('user_003', '13800138003', 'ACTIVE'),
('admin_001', '13900139001', 'ACTIVE');

-- 插入用户档案数据
INSERT INTO `user_profiles` (`profile_id`, `user_id`, `nickname`, `credit_score`, `vip_level`) VALUES
('profile_001', 'user_001', '小明同学', 100, 'NORMAL'),
('profile_002', 'user_002', '小红同学', 95, 'NORMAL'),
('profile_003', 'user_003', '小李同学', 105, 'BRONZE'),
('profile_004', 'admin_001', '管理员', 100, 'NORMAL');

-- 插入测试商品数据
INSERT INTO `commodities` (`commodity_id`, `seller_id`, `title`, `description`, `price`, `stock`, `location`, `commodity_status`) VALUES
('commodity_001', 'user_001', '二手教材-高等数学', '九成新，无笔记，适合大一学生使用', 25.00, 1, '仙林校区', 'PUBLISHED'),
('commodity_002', 'user_002', '台式电脑主机', '配置：i5-8400，GTX1060，8G内存，适合游戏和学习', 2500.00, 1, '鼓楼校区', 'PUBLISHED'),
('commodity_003', 'user_003', '自行车', '捷安特山地车，骑行2年，保养良好', 800.00, 1, '仙林校区', 'PUBLISHED');

-- 重新启用外键检查
SET FOREIGN_KEY_CHECKS = 1;

-- ========================================
-- 脚本执行完成
-- ========================================
