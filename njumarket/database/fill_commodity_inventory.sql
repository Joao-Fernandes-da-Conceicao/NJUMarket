-- 从商品表 nju_market.commodities 填充库存表 nju_market.commodity_inventory
-- 可重复执行：已存在的 commodity_id 会更新，不存在的会插入

INSERT INTO nju_market.commodity_inventory (
    commodity_id,
    available_quantity,
    total_quantity,
    updated_at
)
SELECT
    c.commodity_id,
    CASE WHEN c.commodity_status = 'ON_SHELF' THEN c.stock ELSE 0 END,
    c.stock,
    CURRENT_TIMESTAMP
FROM nju_market.commodities c
ON CONFLICT (commodity_id) DO UPDATE SET
    available_quantity = EXCLUDED.available_quantity,
    total_quantity   = EXCLUDED.total_quantity,
    updated_at      = EXCLUDED.updated_at;
