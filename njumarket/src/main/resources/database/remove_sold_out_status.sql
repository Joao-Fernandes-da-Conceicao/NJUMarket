-- ========================================
-- 移除已售罄状态脚本
-- ========================================
-- 此脚本用于移除项目中的SOLD_OUT状态和相关逻辑
-- 执行前请确保备份数据库

SET FOREIGN_KEY_CHECKS = 0;

-- ========================================
-- 第一部分：更新商品状态
-- ========================================

-- 将SOLD_OUT状态的商品改为DRAFT状态
UPDATE `commodities` SET 
    `commodity_status` = 'DRAFT',
    `seller_visibility` = 'PRIVATE',
    `buyer_visibility` = 'HIDDEN'
WHERE `commodity_status` = 'SOLD_OUT';

-- ========================================
-- 第二部分：更新状态说明视图
-- ========================================

-- 更新商品状态说明视图，移除SOLD_OUT状态
CREATE OR REPLACE VIEW `v_commodity_status_info` AS
SELECT 
    `commodity_status`,
    CASE 
        WHEN `commodity_status` = 'DRAFT' THEN '草稿状态'
        WHEN `commodity_status` = 'PUBLISHED' THEN '已发布状态'
        WHEN `commodity_status` = 'ON_SHELF' THEN '已上架状态'
        WHEN `commodity_status` = 'OFF_SHELF' THEN '已下架状态'
        ELSE '未知状态'
    END AS `status_description`,
    CASE 
        WHEN `commodity_status` = 'DRAFT' THEN '不可见，不可购买，可编辑'
        WHEN `commodity_status` = 'PUBLISHED' THEN '可见，不可购买，可编辑'
        WHEN `commodity_status` = 'ON_SHELF' THEN '可见，可购买，正式销售'
        WHEN `commodity_status` = 'OFF_SHELF' THEN '不可见，不可购买'
        ELSE '未知行为'
    END AS `status_behavior`,
    CASE 
        WHEN `commodity_status` = 'DRAFT' THEN 'warning'
        WHEN `commodity_status` = 'PUBLISHED' THEN 'info'
        WHEN `commodity_status` = 'ON_SHELF' THEN 'success'
        WHEN `commodity_status` = 'OFF_SHELF' THEN 'danger'
        ELSE 'info'
    END AS `status_color`
FROM `commodities`
GROUP BY `commodity_status`;

-- ========================================
-- 第三部分：更新状态统计视图
-- ========================================

-- 更新商品状态统计视图，移除SOLD_OUT状态
CREATE OR REPLACE VIEW `v_commodity_status_statistics` AS
SELECT 
    `commodity_status`,
    COUNT(*) AS `count`,
    CASE 
        WHEN `commodity_status` = 'DRAFT' THEN '草稿'
        WHEN `commodity_status` = 'PUBLISHED' THEN '已发布'
        WHEN `commodity_status` = 'ON_SHELF' THEN '已上架'
        WHEN `commodity_status` = 'OFF_SHELF' THEN '已下架'
        ELSE '未知'
    END AS `status_name`,
    CASE 
        WHEN `commodity_status` = 'DRAFT' THEN 'warning'
        WHEN `commodity_status` = 'PUBLISHED' THEN 'info'
        WHEN `commodity_status` = 'ON_SHELF' THEN 'success'
        WHEN `commodity_status` = 'OFF_SHELF' THEN 'danger'
        ELSE 'info'
    END AS `status_color`
FROM `commodities`
GROUP BY `commodity_status`;

-- ========================================
-- 第四部分：更新商品状态枚举约束
-- ========================================

-- 更新商品状态字段的注释，移除SOLD_OUT状态
ALTER TABLE `commodities` 
MODIFY COLUMN `commodity_status` varchar(20) NOT NULL DEFAULT 'DRAFT' 
COMMENT '商品状态: DRAFT-草稿, PUBLISHED-已发布, ON_SHELF-已上架, OFF_SHELF-已下架';

-- ========================================
-- 第五部分：创建状态转换说明
-- ========================================

-- 创建新的状态转换说明视图
CREATE OR REPLACE VIEW `v_commodity_status_transitions` AS
SELECT 
    'DRAFT' AS `from_status`,
    'PUBLISHED' AS `to_status`,
    '发布商品' AS `action_name`,
    '将草稿商品发布为已发布状态' AS `action_description`,
    'POST /api/user/commodity/{id}/publish' AS `api_endpoint`
UNION ALL
SELECT 
    'PUBLISHED' AS `from_status`,
    'ON_SHELF' AS `to_status`,
    '上架商品' AS `action_name`,
    '将已发布商品上架销售' AS `action_description`,
    'POST /api/user/commodity/{id}/shelf' AS `api_endpoint`
UNION ALL
SELECT 
    'ON_SHELF' AS `from_status`,
    'OFF_SHELF' AS `to_status`,
    '下架商品' AS `action_name`,
    '将已上架商品下架' AS `action_description`,
    'POST /api/user/commodity/{id}/unshelf' AS `api_endpoint`
UNION ALL
SELECT 
    'OFF_SHELF' AS `from_status`,
    'ON_SHELF' AS `to_status`,
    '重新上架' AS `action_name`,
    '将已下架商品重新上架' AS `action_description`,
    'POST /api/user/commodity/{id}/republish' AS `api_endpoint`
UNION ALL
SELECT 
    'PUBLISHED' AS `from_status`,
    'DRAFT' AS `to_status`,
    '设为草稿' AS `action_name`,
    '将已发布商品设为草稿状态' AS `action_description`,
    'POST /api/user/commodity/{id}/draft' AS `api_endpoint`
UNION ALL
SELECT 
    'ON_SHELF' AS `from_status`,
    'DRAFT' AS `to_status`,
    '设为草稿' AS `action_name`,
    '将已上架商品设为草稿状态' AS `action_description`,
    'POST /api/user/commodity/{id}/draft' AS `api_endpoint`
UNION ALL
SELECT 
    'OFF_SHELF' AS `from_status`,
    'DRAFT' AS `to_status`,
    '设为草稿' AS `action_name`,
    '将已下架商品设为草稿状态' AS `action_description`,
    'POST /api/user/commodity/{id}/draft' AS `api_endpoint`;

SET FOREIGN_KEY_CHECKS = 1;

-- ========================================
-- 脚本执行完成
-- ========================================

/*
移除已售罄状态完成说明：

1. 状态更新：
   - 将所有SOLD_OUT状态的商品改为DRAFT状态
   - 设置可见性为PRIVATE/HIDDEN

2. 新的状态体系：
   - DRAFT：草稿状态（不可见，不可购买，可编辑）
   - PUBLISHED：已发布状态（可见，不可购买，可编辑）
   - ON_SHELF：已上架状态（可见，可购买，正式销售）
   - OFF_SHELF：已下架状态（不可见，不可购买）

3. 状态转换规则：
   - DRAFT → PUBLISHED：发布商品
   - PUBLISHED → ON_SHELF：上架商品
   - ON_SHELF → OFF_SHELF：下架商品
   - OFF_SHELF → ON_SHELF：重新上架
   - PUBLISHED/ON_SHELF/OFF_SHELF → DRAFT：设为草稿

4. API接口变更：
   - 移除了 POST /api/user/commodity/{id}/sold-out 接口
   - 其他接口保持不变

5. 前端变更：
   - 移除了"已售完"标签页
   - 移除了"设为售罄"按钮
   - 更新了状态显示逻辑
*/
