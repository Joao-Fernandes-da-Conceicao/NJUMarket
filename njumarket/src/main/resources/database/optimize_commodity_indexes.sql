-- ============================================================
-- Commodities 表索引优化脚本
-- ============================================================
-- 目的：删除冗余索引，优化查询性能，减少索引维护成本
-- ============================================================

-- ============================================================
-- 第一部分：删除冗余索引
-- ============================================================

-- 1. 删除冗余的单字段索引（已包含在联合索引中）
DROP INDEX IF EXISTS idx_commodity_status ON commodities;
-- 原因：idx_status_publish_time, idx_status_click_count, idx_status_price, 
--       idx_status_seller_buyer_visibility 都包含 commodity_status 作为第一列

DROP INDEX IF EXISTS idx_seller_visibility ON commodities;
-- 原因：idx_status_seller_buyer_visibility 包含 seller_visibility

DROP INDEX IF EXISTS idx_buyer_visibility ON commodities;
-- 原因：idx_status_seller_buyer_visibility 包含 buyer_visibility

-- 2. 删除可以合并的索引（将被新的联合索引替代）
DROP INDEX IF EXISTS idx_seller_status ON commodities;
-- 原因：将被 idx_seller_status_publish_time 替代（包含排序字段）

DROP INDEX IF EXISTS idx_seller_publish_time ON commodities;
-- 原因：将被 idx_seller_status_publish_time 替代（包含状态字段）

DROP INDEX IF EXISTS idx_seller_click_count ON commodities;
-- 原因：使用频率低，可以删除（如果后续需要，可以重新创建）

-- ============================================================
-- 第二部分：创建优化后的索引
-- ============================================================

-- 1. 创建卖家商品查询+排序的联合索引（推荐）
-- 用途：优化 findBySellerIdAndCommodityStatus 查询，并支持按发布时间排序
-- 使用场景：查询卖家特定状态的商品，并按发布时间排序
CREATE INDEX idx_seller_status_publish_time 
ON commodities(seller_id, commodity_status, publish_time DESC);

-- ============================================================
-- 第三部分：保留的索引（说明）
-- ============================================================

-- 以下索引保留，因为它们都有实际用途：

-- PRIMARY (commodity_id) - 主键索引，必需
-- idx_seller_id (seller_id) - 按卖家查询，必需
-- idx_publish_time (publish_time) - 全局按时间排序，保留
-- idx_price (price) - 价格范围查询，保留
-- idx_click_count (click_count) - 全局按点击量排序，保留
-- idx_category (category) - 按分类查询前缀匹配，保留
-- idx_condition_level (condition_level) - 按成色查询，保留
-- idx_stock (stock) - 库存查询，保留
-- idx_status_seller_buyer_visibility - 公开商品查询，保留
-- idx_status_publish_time - 最新商品查询，保留
-- idx_status_click_count - 热门商品查询，保留
-- idx_status_price - 价格范围查询，保留
-- idx_category_status_visibility - 分类商品查询，保留

-- ============================================================
-- 第四部分：验证索引优化结果
-- ============================================================

-- 查看优化后的索引
SHOW INDEX FROM commodities;

-- 查看索引详细信息
SELECT 
    TABLE_NAME,
    INDEX_NAME,
    GROUP_CONCAT(COLUMN_NAME ORDER BY SEQ_IN_INDEX) AS COLUMNS,
    INDEX_TYPE,
    NON_UNIQUE
FROM information_schema.STATISTICS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'commodities'
  AND INDEX_NAME != 'PRIMARY'
GROUP BY TABLE_NAME, INDEX_NAME, INDEX_TYPE, NON_UNIQUE
ORDER BY INDEX_NAME;

-- ============================================================
-- 第五部分：性能测试建议
-- ============================================================

-- 测试1：卖家商品查询+排序
-- EXPLAIN SELECT * FROM commodities 
-- WHERE seller_id = 'xxx' AND commodity_status = 'ON_SHELF' 
-- ORDER BY publish_time DESC 
-- LIMIT 20;
-- 预期：使用 idx_seller_status_publish_time 索引

-- 测试2：公开商品列表查询
-- EXPLAIN SELECT * FROM commodities 
-- WHERE commodity_status = 'ON_SHELF' 
--   AND seller_visibility = 'PUBLIC' 
--   AND buyer_visibility = 'PUBLIC' 
-- ORDER BY publish_time DESC 
-- LIMIT 20;
-- 预期：使用 idx_status_seller_buyer_visibility 或 idx_status_publish_time

-- 测试3：分类商品查询
-- EXPLAIN SELECT * FROM commodities 
-- WHERE category = 'xxx' 
--   AND commodity_status = 'ON_SHELF' 
--   AND seller_visibility = 'PUBLIC' 
--   AND buyer_visibility = 'PUBLIC' 
-- LIMIT 20;
-- 预期：使用 idx_category_status_visibility 索引

-- ============================================================
-- 第六部分：索引优化说明
-- ============================================================

/*
索引优化总结：

1. 删除的索引（6个）：
   - idx_commodity_status（冗余）
   - idx_seller_visibility（冗余）
   - idx_buyer_visibility（冗余）
   - idx_seller_status（可合并）
   - idx_seller_publish_time（可合并）
   - idx_seller_click_count（低频使用）

2. 新增的索引（1个）：
   - idx_seller_status_publish_time（卖家商品查询+排序优化）

3. 优化效果：
   - 索引数量：从 19 个减少到 14 个（减少 26%）
   - 查询性能：卖家商品查询+排序性能提升（避免额外排序）
   - 写入性能：INSERT/UPDATE/DELETE 性能提升（减少索引维护）
   - 存储空间：减少约 10-20%

4. 注意事项：
   - 删除索引后，单独查询 status 时性能略降（但可接受）
   - 建议定期监控索引使用情况
   - 如果后续需要 idx_seller_click_count，可以重新创建

5. 后续优化建议：
   - 定期检查未使用的索引
   - 根据实际查询模式调整索引
   - 使用 EXPLAIN 分析查询执行计划
*/

-- ============================================================
-- 执行完成
-- ============================================================

