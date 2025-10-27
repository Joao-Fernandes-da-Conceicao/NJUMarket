-- 修正订单快照图片字段：只保留第一张图片
-- 此脚本用于批量更新已有的订单数据

-- 说明：将 commodity_snapshot_images 字段从逗号分隔的多个图片改为单个图片URL
-- 例如：'image1.jpg,image2.jpg,image3.jpg' -> 'image1.jpg'

UPDATE orders 
SET commodity_snapshot_images = SUBSTRING_INDEX(commodity_snapshot_images, ',', 1)
WHERE commodity_snapshot_images IS NOT NULL 
  AND commodity_snapshot_images != ''
  AND commodity_snapshot_images LIKE '%,%';

-- 显示更新结果
SELECT 
    order_id,
    commodity_snapshot_title,
    commodity_snapshot_images AS updated_image
FROM orders
WHERE commodity_snapshot_images IS NOT NULL
LIMIT 10;

