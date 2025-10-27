-- ========================================
-- 添加支付时间字段到订单表
-- ========================================

-- 添加支付时间字段
ALTER TABLE `orders` 
ADD COLUMN `pay_time` datetime DEFAULT NULL COMMENT '支付时间' AFTER `create_time`;

-- 添加索引（可选，用于按支付时间查询）
ALTER TABLE `orders` 
ADD INDEX `idx_pay_time` (`pay_time`);

-- 对于已支付的订单，可以根据订单状态设置支付时间
-- 注意：这只是一个示例，实际时间应该是订单状态变为PAID时的时间
-- 由于无法准确追溯，这里设置为NULL，表示支付时间未知
-- 如果需要，可以根据业务逻辑调整
-- UPDATE `orders` 
-- SET `pay_time` = `create_time` 
-- WHERE `order_status` IN ('PAID', 'SHIPPED', 'COMPLETED') 
--   AND `pay_time` IS NULL;

