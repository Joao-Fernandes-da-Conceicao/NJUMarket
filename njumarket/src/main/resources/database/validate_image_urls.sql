-- 验证图片URL安全性：检查是否包含逗号
-- 此脚本用于检查数据库中是否有可能导致分割错误的图片URL

-- 检查商品表中的图片URL
SELECT 
    commodity_id,
    title,
    images,
    CASE 
        WHEN images LIKE '%,%' THEN '包含逗号'
        ELSE '正常'
    END AS url_status,
    LENGTH(images) AS url_length
FROM commodities 
WHERE images IS NOT NULL 
  AND images != ''
ORDER BY url_length DESC
LIMIT 20;

-- 统计
SELECT 
    CASE 
        WHEN images LIKE '%,%' THEN '包含逗号'
        ELSE '正常'
    END AS url_status,
    COUNT(*) AS count
FROM commodities 
WHERE images IS NOT NULL 
  AND images != ''
GROUP BY url_status;

-- 检查订单快照图片URL
SELECT 
    order_id,
    commodity_snapshot_title,
    commodity_snapshot_images,
    CASE 
        WHEN commodity_snapshot_images LIKE '%,%' THEN '包含逗号'
        ELSE '正常'
    END AS url_status,
    LENGTH(commodity_snapshot_images) AS url_length
FROM orders 
WHERE commodity_snapshot_images IS NOT NULL 
  AND commodity_snapshot_images != ''
ORDER BY url_length DESC
LIMIT 20;

-- 统计订单快照图片URL
SELECT 
    CASE 
        WHEN commodity_snapshot_images LIKE '%,%' THEN '包含逗号（需要修复）'
        ELSE '正常（单张图片）'
    END AS url_status,
    COUNT(*) AS count
FROM orders 
WHERE commodity_snapshot_images IS NOT NULL 
  AND commodity_snapshot_images != ''
GROUP BY url_status;

-- 显示需要修复的订单（包含逗号的URL）
SELECT 
    order_id,
    commodity_snapshot_title,
    commodity_snapshot_images AS original_images,
    SUBSTRING_INDEX(commodity_snapshot_images, ',', 1) AS suggested_fix
FROM orders 
WHERE commodity_snapshot_images LIKE '%,%'
LIMIT 10;

