-- ========================================
-- NJUMarket 商品状态简化数据库更新脚本
-- ========================================
SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ========================================
-- 第一部分：更新商品状态字段
-- ========================================

-- 更新商品状态：将复杂状态映射为新的三种状态
UPDATE `commodities` SET 
    `commodity_status` = CASE 
        -- 草稿状态保持不变
        WHEN `commodity_status` = 'DRAFT' THEN 'DRAFT'
        
        -- 已发布状态改为草稿状态（因为合并了草稿和已发布）
        WHEN `commodity_status` = 'PUBLISHED' THEN 'DRAFT'
        
        -- 已上架状态保持不变
        WHEN `commodity_status` = 'ON_SHELF' THEN 'ON_SHELF'
        
        -- 已售完状态保持不变
        WHEN `commodity_status` = 'SOLD_OUT' THEN 'SOLD_OUT'
        
        -- 已下架状态改为草稿状态
        WHEN `commodity_status` = 'REMOVED' THEN 'DRAFT'
        
        -- 暂停状态改为草稿状态
        WHEN `commodity_status` = 'SUSPENDED' THEN 'DRAFT'
        
        -- 在售状态改为已上架状态
        WHEN `commodity_status` = 'ACTIVE' THEN 'ON_SHELF'
        
        -- 其他未知状态默认为草稿
        ELSE 'DRAFT'
    END,
    
    -- 同时更新可见性设置
    `seller_visibility` = CASE 
        WHEN `commodity_status` IN ('ON_SHELF', 'SOLD_OUT') THEN 'PUBLIC'
        ELSE 'PRIVATE'
    END,
    
    `buyer_visibility` = CASE 
        WHEN `commodity_status` IN ('ON_SHELF', 'SOLD_OUT') THEN 'PUBLIC'
        ELSE 'HIDDEN'
    END;

-- ========================================
-- 第二部分：更新订单状态字段（如果需要）
-- ========================================

-- 更新订单状态：保持原有逻辑，但确保与新商品状态一致
UPDATE `orders` SET 
    `order_status` = CASE 
        -- 保持原有订单状态不变
        WHEN `order_status` = 'CREATED' THEN 'CREATED'
        WHEN `order_status` = 'PAID' THEN 'PAID'
        WHEN `order_status` = 'SHIPPED' THEN 'SHIPPED'
        WHEN `order_status` = 'COMPLETED' THEN 'COMPLETED'
        WHEN `order_status` = 'CANCELLED' THEN 'CANCELLED'
        WHEN `order_status` = 'REFUNDED' THEN 'REFUNDED'
        WHEN `order_status` = 'RETURN_REQUESTED' THEN 'RETURN_REQUESTED'
        WHEN `order_status` = 'RETURN_APPROVED' THEN 'RETURN_APPROVED'
        WHEN `order_status` = 'RETURN_REJECTED' THEN 'RETURN_REJECTED'
        WHEN `order_status` = 'RETURN_COMPLETED' THEN 'RETURN_COMPLETED'
        ELSE 'CREATED'
    END;

-- ========================================
-- 第三部分：更新商品分类和可见性设置
-- ========================================

-- 确保所有商品的可见性设置正确
UPDATE `commodities` SET 
    `seller_visibility` = CASE 
        WHEN `commodity_status` = 'ON_SHELF' THEN 'PUBLIC'
        WHEN `commodity_status` = 'SOLD_OUT' THEN 'PUBLIC'
        ELSE 'PRIVATE'
    END,
    
    `buyer_visibility` = CASE 
        WHEN `commodity_status` = 'ON_SHELF' THEN 'PUBLIC'
        WHEN `commodity_status` = 'SOLD_OUT' THEN 'PUBLIC'
        ELSE 'HIDDEN'
    END;

-- ========================================
-- 第四部分：更新商品发布时间
-- ========================================

-- 对于从PUBLISHED状态改为DRAFT状态的商品，清除发布时间
UPDATE `commodities` SET 
    `publish_time` = NULL 
WHERE `commodity_status` = 'DRAFT' 
  AND `publish_time` IS NOT NULL;

-- 对于ON_SHELF状态的商品，确保有发布时间
UPDATE `commodities` SET 
    `publish_time` = COALESCE(`publish_time`, `create_time`)
WHERE `commodity_status` = 'ON_SHELF' 
  AND `publish_time` IS NULL;

-- ========================================
-- 第五部分：更新商品库存设置
-- ========================================

-- 确保已售完状态的商品库存为0
UPDATE `commodities` SET 
    `stock` = 0 
WHERE `commodity_status` = 'SOLD_OUT' 
  AND `stock` > 0;

-- ========================================
-- 第六部分：创建状态说明视图
-- ========================================

-- 创建商品状态说明视图
CREATE OR REPLACE VIEW `v_commodity_status_info` AS
SELECT 
    `commodity_status`,
    CASE 
        WHEN `commodity_status` = 'DRAFT' THEN '草稿状态'
        WHEN `commodity_status` = 'ON_SHELF' THEN '已上架状态'
        WHEN `commodity_status` = 'SOLD_OUT' THEN '已售完状态'
        ELSE '未知状态'
    END AS `status_description`,
    CASE 
        WHEN `commodity_status` = 'DRAFT' THEN '不可见，不可购买，可编辑'
        WHEN `commodity_status` = 'ON_SHELF' THEN '可见，可购买，正式销售'
        WHEN `commodity_status` = 'SOLD_OUT' THEN '可见，不可购买'
        ELSE '未知行为'
    END AS `status_behavior`,
    CASE 
        WHEN `commodity_status` = 'DRAFT' THEN 'warning'
        WHEN `commodity_status` = 'ON_SHELF' THEN 'success'
        WHEN `commodity_status` = 'SOLD_OUT' THEN 'info'
        ELSE 'info'
    END AS `status_color`
FROM `commodities`
GROUP BY `commodity_status`;

-- ========================================
-- 第七部分：创建状态统计视图
-- ========================================

-- 创建商品状态统计视图
CREATE OR REPLACE VIEW `v_commodity_status_statistics` AS
SELECT 
    `commodity_status`,
    COUNT(*) AS `count`,
    CASE 
        WHEN `commodity_status` = 'DRAFT' THEN '草稿'
        WHEN `commodity_status` = 'ON_SHELF' THEN '已上架'
        WHEN `commodity_status` = 'SOLD_OUT' THEN '已售完'
        ELSE '未知'
    END AS `status_name`,
    ROUND(COUNT(*) * 100.0 / (SELECT COUNT(*) FROM `commodities`), 2) AS `percentage`
FROM `commodities`
GROUP BY `commodity_status`
ORDER BY `count` DESC;

-- ========================================
-- 第八部分：创建状态转换日志表
-- ========================================

-- 创建商品状态转换日志表
CREATE TABLE IF NOT EXISTS `commodity_status_logs` (
    `id` bigint(20) NOT NULL AUTO_INCREMENT,
    `commodity_id` varchar(50) NOT NULL COMMENT '商品ID',
    `old_status` varchar(20) DEFAULT NULL COMMENT '原状态',
    `new_status` varchar(20) NOT NULL COMMENT '新状态',
    `operation` varchar(50) NOT NULL COMMENT '操作类型',
    `operator_id` varchar(50) DEFAULT NULL COMMENT '操作者ID',
    `operator_type` varchar(20) DEFAULT 'USER' COMMENT '操作者类型',
    `reason` varchar(255) DEFAULT NULL COMMENT '操作原因',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_commodity_id` (`commodity_id`),
    KEY `idx_create_time` (`create_time`),
    KEY `idx_operator` (`operator_id`, `operator_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品状态转换日志表';

-- ========================================
-- 第九部分：插入状态转换记录
-- ========================================

-- 记录本次状态转换
INSERT INTO `commodity_status_logs` (
    `commodity_id`, 
    `old_status`, 
    `new_status`, 
    `operation`, 
    `operator_type`, 
    `reason`
)
SELECT 
    `commodity_id`,
    `commodity_status` AS `old_status`,
    CASE 
        WHEN `commodity_status` = 'DRAFT' THEN 'DRAFT'
        WHEN `commodity_status` = 'PUBLISHED' THEN 'DRAFT'
        WHEN `commodity_status` = 'ON_SHELF' THEN 'ON_SHELF'
        WHEN `commodity_status` = 'SOLD_OUT' THEN 'SOLD_OUT'
        WHEN `commodity_status` = 'REMOVED' THEN 'DRAFT'
        WHEN `commodity_status` = 'SUSPENDED' THEN 'DRAFT'
        WHEN `commodity_status` = 'ACTIVE' THEN 'ON_SHELF'
        ELSE 'DRAFT'
    END AS `new_status`,
    'SYSTEM_UPDATE' AS `operation`,
    'SYSTEM' AS `operator_type`,
    '商品状态简化更新：从复杂状态映射为三种状态' AS `reason`
FROM `commodities`
WHERE `commodity_status` IN ('PUBLISHED', 'REMOVED', 'SUSPENDED', 'ACTIVE');

-- ========================================
-- 第十部分：验证更新结果
-- ========================================

-- 显示更新后的状态统计
SELECT 
    '更新完成' AS `status`,
    COUNT(*) AS `total_commodities`,
    SUM(CASE WHEN `commodity_status` = 'DRAFT' THEN 1 ELSE 0 END) AS `draft_count`,
    SUM(CASE WHEN `commodity_status` = 'ON_SHELF' THEN 1 ELSE 0 END) AS `onshelf_count`,
    SUM(CASE WHEN `commodity_status` = 'SOLD_OUT' THEN 1 ELSE 0 END) AS `soldout_count`
FROM `commodities`;

-- 显示状态转换日志
SELECT 
    `old_status`,
    `new_status`,
    COUNT(*) AS `count`
FROM `commodity_status_logs`
WHERE `operation` = 'SYSTEM_UPDATE'
GROUP BY `old_status`, `new_status`
ORDER BY `count` DESC;

-- ========================================
-- 第十一部分：状态说明
-- ========================================

/*
新的商品状态说明：

1. DRAFT（草稿状态）
   - 行为：不可见，不可购买，可编辑
   - 可见性：PRIVATE/HIDDEN
   - 用途：商品编辑、保存草稿

2. ON_SHELF（已上架状态）
   - 行为：可见，可购买，正式销售
   - 可见性：PUBLIC/PUBLIC
   - 用途：正式销售状态（唯一可购买状态）

3. SOLD_OUT（已售完状态）
   - 行为：可见，不可购买
   - 可见性：PUBLIC/PUBLIC
   - 用途：库存为0，显示售完

状态转换规则：
- DRAFT → ON_SHELF：上架商品
- ON_SHELF → DRAFT：下架商品
- ON_SHELF → SOLD_OUT：设为售罄
- SOLD_OUT → DRAFT：设为草稿
- DRAFT → ON_SHELF：重新上架

API接口：
- POST /api/user/commodity/{id}/shelf：上架商品
- POST /api/user/commodity/{id}/unshelf：下架商品
- POST /api/user/commodity/{id}/draft：设为草稿
- POST /api/user/commodity/{id}/sold-out：设为售罄
- POST /api/user/commodity/{id}/republish：重新上架
*/

SET FOREIGN_KEY_CHECKS = 1;

-- ========================================
-- 脚本执行完成
-- ========================================
