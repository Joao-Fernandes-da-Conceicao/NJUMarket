-- ========================================
-- NJUMarket 数据库完整更新脚本
-- Complete database update script for NJUMarket
-- 包含：可见性字段、双可见性扩展、退货功能
-- Includes: visibility fields, dual visibility extension, return functionality
-- ========================================

-- 设置字符集和排序规则
SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ========================================
-- 第一部分：基础可见性字段和商品订单字段
-- Part 1: Basic visibility fields and commodity/order fields
-- ========================================

-- 1.1 为商品表添加基础字段
ALTER TABLE `commodities` 
ADD COLUMN `category` varchar(50) DEFAULT NULL COMMENT '商品分类' AFTER `location`,
ADD COLUMN `condition_level` varchar(20) DEFAULT 'GOOD' COMMENT '商品成色: EXCELLENT-优秀, GOOD-良好, FAIR-一般, POOR-较差' AFTER `category`,
ADD COLUMN `images` text COMMENT '商品图片URL列表(JSON格式)' AFTER `condition_level`,
ADD INDEX `idx_category` (`category`),
ADD INDEX `idx_condition_level` (`condition_level`);

-- 1.2 为订单表添加基础字段
ALTER TABLE `orders` 
ADD COLUMN `shipping_time` datetime DEFAULT NULL COMMENT '发货时间' AFTER `create_time`,
ADD COLUMN `delivery_time` datetime DEFAULT NULL COMMENT '签收时间' AFTER `shipping_time`,
ADD COLUMN `tracking_number` varchar(100) DEFAULT NULL COMMENT '快递单号' AFTER `delivery_time`,
ADD COLUMN `shipping_address` text COMMENT '收货地址' AFTER `tracking_number`,
ADD COLUMN `remark` text COMMENT '订单备注' AFTER `shipping_address`,
ADD INDEX `idx_shipping_time` (`shipping_time`),
ADD INDEX `idx_delivery_time` (`delivery_time`);

-- ========================================
-- 第二部分：双可见性字段（卖家可见性和买家可见性）
-- Part 2: Dual visibility fields (seller visibility and buyer visibility)
-- ========================================

-- 2.1 为商品表添加双可见性字段
ALTER TABLE `commodities` 
ADD COLUMN `seller_visibility` varchar(20) NOT NULL DEFAULT 'PUBLIC' COMMENT '卖家可见性: PUBLIC-公开, PRIVATE-私有, HIDDEN-隐藏' AFTER `commodity_status`,
ADD COLUMN `buyer_visibility` varchar(20) NOT NULL DEFAULT 'PUBLIC' COMMENT '买家可见性: PUBLIC-公开, PRIVATE-私有, HIDDEN-隐藏' AFTER `seller_visibility`,
ADD INDEX `idx_seller_visibility` (`seller_visibility`),
ADD INDEX `idx_buyer_visibility` (`buyer_visibility`);

-- 2.2 为订单表添加双可见性字段
ALTER TABLE `orders` 
ADD COLUMN `seller_visibility` varchar(20) NOT NULL DEFAULT 'PUBLIC' COMMENT '卖家可见性: PUBLIC-公开, PRIVATE-私有, HIDDEN-隐藏' AFTER `order_status`,
ADD COLUMN `buyer_visibility` varchar(20) NOT NULL DEFAULT 'PUBLIC' COMMENT '买家可见性: PUBLIC-公开, PRIVATE-私有, HIDDEN-隐藏' AFTER `seller_visibility`,
ADD INDEX `idx_seller_visibility` (`seller_visibility`),
ADD INDEX `idx_buyer_visibility` (`buyer_visibility`);

-- ========================================
-- 第三部分：退货相关字段
-- Part 3: Return-related fields
-- ========================================

-- 3.1 为订单表添加退货相关字段
ALTER TABLE `orders` 
ADD COLUMN `return_reason` text COMMENT '退货原因' AFTER `remark`,
ADD COLUMN `return_request_time` datetime DEFAULT NULL COMMENT '退货申请时间' AFTER `return_reason`,
ADD COLUMN `return_approval_time` datetime DEFAULT NULL COMMENT '退货审批时间' AFTER `return_request_time`,
ADD COLUMN `return_rejection_reason` text COMMENT '退货拒绝原因' AFTER `return_approval_time`,
ADD COLUMN `return_tracking_number` varchar(100) DEFAULT NULL COMMENT '退货快递单号' AFTER `return_rejection_reason`,
ADD COLUMN `return_completion_time` datetime DEFAULT NULL COMMENT '退货完成时间' AFTER `return_tracking_number`,
ADD INDEX `idx_return_status` (`order_status`);

-- ========================================
-- 第四部分：数据更新
-- Part 4: Data updates
-- ========================================

-- 4.1 更新商品可见性数据
UPDATE `commodities` SET 
    `seller_visibility` = 'PUBLIC',
    `buyer_visibility` = 'PUBLIC' 
WHERE `commodity_status` = 'PUBLISHED';

UPDATE `commodities` SET 
    `seller_visibility` = 'HIDDEN',
    `buyer_visibility` = 'HIDDEN' 
WHERE `commodity_status` = 'REMOVED';

UPDATE `commodities` SET 
    `seller_visibility` = 'PRIVATE',
    `buyer_visibility` = 'PRIVATE' 
WHERE `commodity_status` = 'DRAFT';

-- 4.2 更新订单可见性数据
UPDATE `orders` SET 
    `seller_visibility` = 'PUBLIC',
    `buyer_visibility` = 'PUBLIC' 
WHERE `order_status` IN ('CREATED', 'PAID', 'SHIPPED', 'COMPLETED');

UPDATE `orders` SET 
    `seller_visibility` = 'HIDDEN',
    `buyer_visibility` = 'HIDDEN' 
WHERE `order_status` IN ('CANCELLED', 'REFUNDED');

-- ========================================
-- 第五部分：创建辅助表
-- Part 5: Create auxiliary tables
-- ========================================

-- 5.1 创建商品分类表
CREATE TABLE IF NOT EXISTS `commodity_categories` (
  `category_id` varchar(50) NOT NULL COMMENT '分类ID',
  `category_name` varchar(100) NOT NULL COMMENT '分类名称',
  `parent_id` varchar(50) DEFAULT NULL COMMENT '父分类ID',
  `sort_order` int NOT NULL DEFAULT 0 COMMENT '排序',
  `is_active` tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否启用',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`category_id`),
  KEY `idx_parent_id` (`parent_id`),
  KEY `idx_sort_order` (`sort_order`),
  KEY `idx_is_active` (`is_active`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='商品分类表';

-- 插入默认分类数据
INSERT INTO `commodity_categories` (`category_id`, `category_name`, `sort_order`) VALUES
('cat_001', '教材书籍', 1),
('cat_002', '电子产品', 2),
('cat_003', '生活用品', 3),
('cat_004', '服装配饰', 4),
('cat_005', '运动健身', 5),
('cat_006', '其他', 99);

-- 5.2 创建可见性类型表
CREATE TABLE IF NOT EXISTS `visibility_types` (
  `type_id` varchar(20) NOT NULL COMMENT '类型ID',
  `type_name` varchar(50) NOT NULL COMMENT '类型名称',
  `description` text COMMENT '描述',
  `is_active` tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否启用',
  PRIMARY KEY (`type_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='可见性类型表';

-- 插入可见性类型数据
INSERT INTO `visibility_types` (`type_id`, `type_name`, `description`) VALUES
('PUBLIC', '公开', '所有人都可以看到'),
('PRIVATE', '私有', '只有自己可以看到'),
('HIDDEN', '隐藏', '完全隐藏，不对外显示');

-- 5.3 创建退货记录表
CREATE TABLE IF NOT EXISTS `return_records` (
  `return_id` varchar(50) NOT NULL COMMENT '退货记录ID',
  `order_id` varchar(50) NOT NULL COMMENT '订单ID',
  `return_reason` text COMMENT '退货原因',
  `return_request_time` datetime NOT NULL COMMENT '退货申请时间',
  `return_approval_time` datetime DEFAULT NULL COMMENT '退货审批时间',
  `return_status` varchar(20) NOT NULL COMMENT '退货状态',
  `return_rejection_reason` text COMMENT '退货拒绝原因',
  `return_tracking_number` varchar(100) DEFAULT NULL COMMENT '退货快递单号',
  `return_completion_time` datetime DEFAULT NULL COMMENT '退货完成时间',
  `created_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`return_id`),
  KEY `idx_order_id` (`order_id`),
  KEY `idx_return_status` (`return_status`),
  CONSTRAINT `fk_return_order` FOREIGN KEY (`order_id`) REFERENCES `orders` (`order_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='退货记录表';

-- 5.4 创建退货原因类型表
CREATE TABLE IF NOT EXISTS `return_reason_types` (
  `reason_id` varchar(20) NOT NULL COMMENT '原因ID',
  `reason_name` varchar(50) NOT NULL COMMENT '原因名称',
  `description` text COMMENT '描述',
  `is_active` tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否启用',
  PRIMARY KEY (`reason_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='退货原因类型表';

-- 插入退货原因类型数据
INSERT INTO `return_reason_types` (`reason_id`, `reason_name`, `description`) VALUES
('QUALITY_ISSUE', '质量问题', '商品存在质量问题'),
('DESCRIPTION_MISMATCH', '描述不符', '商品与描述不符'),
('DAMAGED_IN_TRANSIT', '运输损坏', '商品在运输过程中损坏'),
('WRONG_ITEM', '发错商品', '收到的商品与订单不符'),
('CHANGE_MIND', '改变主意', '买家改变主意不想要了'),
('SIZE_ISSUE', '尺寸问题', '商品尺寸不合适'),
('COLOR_ISSUE', '颜色问题', '商品颜色与预期不符'),
('OTHER', '其他原因', '其他原因');

-- 5.5 创建订单状态变更记录表
CREATE TABLE IF NOT EXISTS `order_status_logs` (
  `log_id` varchar(50) NOT NULL COMMENT '日志ID',
  `order_id` varchar(50) NOT NULL COMMENT '订单ID',
  `from_status` varchar(20) DEFAULT NULL COMMENT '原状态',
  `to_status` varchar(20) NOT NULL COMMENT '新状态',
  `operator_id` varchar(50) DEFAULT NULL COMMENT '操作者ID',
  `operator_type` varchar(20) NOT NULL COMMENT '操作者类型: BUYER-买家, SELLER-卖家, ADMIN-管理员, SYSTEM-系统',
  `reason` text COMMENT '变更原因',
  `seller_visibility_before` varchar(20) DEFAULT NULL COMMENT '变更前卖家可见性',
  `seller_visibility_after` varchar(20) DEFAULT NULL COMMENT '变更后卖家可见性',
  `buyer_visibility_before` varchar(20) DEFAULT NULL COMMENT '变更前买家可见性',
  `buyer_visibility_after` varchar(20) DEFAULT NULL COMMENT '变更后买家可见性',
  `return_reason` text COMMENT '退货原因',
  `return_rejection_reason` text COMMENT '退货拒绝原因',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`log_id`),
  KEY `idx_order_id` (`order_id`),
  KEY `idx_operator_id` (`operator_id`),
  KEY `idx_create_time` (`create_time`),
  CONSTRAINT `fk_order_status_logs_order_id` FOREIGN KEY (`order_id`) REFERENCES `orders` (`order_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='订单状态变更记录表';

-- ========================================
-- 第六部分：创建视图和触发器
-- Part 6: Create views and triggers
-- ========================================

-- 6.1 创建退货统计视图
CREATE VIEW `v_return_statistics` AS
SELECT 
    DATE(return_request_time) as return_date,
    COUNT(*) as total_returns,
    SUM(CASE WHEN return_status = 'APPROVED' THEN 1 ELSE 0 END) as approved_returns,
    SUM(CASE WHEN return_status = 'REJECTED' THEN 1 ELSE 0 END) as rejected_returns,
    SUM(CASE WHEN return_status = 'COMPLETED' THEN 1 ELSE 0 END) as completed_returns
FROM `return_records`
WHERE return_request_time >= DATE_SUB(CURDATE(), INTERVAL 30 DAY)
GROUP BY DATE(return_request_time)
ORDER BY return_date DESC;

-- 6.2 创建退货触发器
DELIMITER $$
CREATE TRIGGER `tr_order_return_request` 
AFTER UPDATE ON `orders` 
FOR EACH ROW 
BEGIN
    IF NEW.order_status = 'RETURN_REQUESTED' AND OLD.order_status != 'RETURN_REQUESTED' THEN
        INSERT INTO `return_records` (
            `return_id`, `order_id`, `return_reason`, `return_request_time`, `return_status`
        ) VALUES (
            CONCAT('RET', NEW.order_id, UNIX_TIMESTAMP()), 
            NEW.order_id, 
            NEW.return_reason, 
            NEW.return_request_time, 
            'REQUESTED'
        );
    END IF;
END$$
DELIMITER ;

-- ========================================
-- 第七部分：订单状态说明
-- Part 7: Order status descriptions
-- ========================================

-- 订单状态说明：
-- CREATED: 已创建
-- PAID: 已支付
-- SHIPPED: 已发货
-- COMPLETED: 已完成
-- CANCELLED: 已取消
-- REFUNDED: 已退款
-- RETURN_REQUESTED: 退货申请中
-- RETURN_APPROVED: 退货已同意
-- RETURN_REJECTED: 退货已拒绝
-- RETURN_COMPLETED: 退货已完成

-- 商品状态说明：
-- DRAFT: 草稿
-- PUBLISHED: 已发布
-- SOLD_OUT: 售罄
-- REMOVED: 已下架

-- 可见性状态说明：
-- PUBLIC: 公开可见
-- PRIVATE: 私有（仅自己可见）
-- HIDDEN: 隐藏（完全不可见）

-- 重新启用外键检查
SET FOREIGN_KEY_CHECKS = 1;

-- ========================================
-- 脚本执行完成
-- Complete database update script finished
-- ========================================
