-- ========================================
-- NJUMarket 商品表联合索引优化
-- NJU Market Commodity Table Composite Index Optimization
-- ========================================

SET NAMES utf8mb4;

-- ========================================
-- 第一部分：根据 CommodityRepository 查询需求创建联合索引
-- ========================================

-- 1. 卖家ID + 商品状态联合索引
-- 用途：优化 findBySellerIdAndCommodityStatus 查询
-- 使用场景：查询卖家特定状态的商品（如：查询我上架的商品）
CREATE INDEX idx_seller_status ON commodities(seller_id, commodity_status);

-- 2. 商品状态 + 卖家可见性 + 买家可见性联合索引
-- 用途：优化 findByCommodityStatusAndSellerVisibilityAndBuyerVisibility 查询
-- 使用场景：查询符合条件的可见商品
CREATE INDEX idx_status_seller_buyer_visibility ON commodities(commodity_status, seller_visibility, buyer_visibility);

-- 3. 商品状态 + 发布时间联合索引（DESC排序）
-- 用途：优化按时间倒序查询已上架商品
-- 使用场景：查询最新上架的商品（如：findLatestCommodities）
CREATE INDEX idx_status_publish_time ON commodities(commodity_status, publish_time DESC);

-- 4. 商品状态 + 点击量联合索引（DESC排序）
-- 用途：优化按点击量倒序查询已上架商品
-- 使用场景：查询热门商品（如：findHotCommodities）
CREATE INDEX idx_status_click_count ON commodities(commodity_status, click_count DESC);

-- 5. 分类 + 商品状态 + 卖家可见性 + 买家可见性联合索引
-- 用途：优化 findByCategoryAndVisible 查询
-- 使用场景：按分类查询可见商品（用于商品浏览页面的分类筛选）
CREATE INDEX idx_category_status_visibility ON commodities(category, commodity_status, seller_visibility, buyer_visibility);

-- 6. 价格范围查询优化索引
-- 用途：优化 findByPriceRange 查询（虽然使用 BETWEEN，但索引可以提供帮助）
-- 使用场景：按价格范围查询商品
CREATE INDEX idx_status_price ON commodities(commodity_status, price);

-- 7. 卖家ID + 发布时间联合索引（DESC排序）
-- 用途：优化按卖家查询其商品的发布时间排序
-- 使用场景：查询卖家最近发布的商品
CREATE INDEX idx_seller_publish_time ON commodities(seller_id, publish_time DESC);

-- 8. 卖家ID + 点击量联合索引（DESC排序）
-- 用途：查询卖家热门商品
-- 使用场景：统计卖家浏览量最高的商品
CREATE INDEX idx_seller_click_count ON commodities(seller_id, click_count DESC);

-- ========================================
-- 第二部分：为关键查询字段创建单列索引（如果不存在）
-- ========================================

-- 分类索引（如果不存在）
CREATE INDEX idx_category ON commodities(category);

-- 卖家可见性索引（如果不存在）
CREATE INDEX idx_seller_visibility ON commodities(seller_visibility);

-- 买家可见性索引（如果不存在）
CREATE INDEX idx_buyer_visibility ON commodities(buyer_visibility);

-- 库存索引（用于售罄查询）
CREATE INDEX idx_stock ON commodities(stock);

-- ========================================
-- 第三部分：验证索引创建结果
-- ========================================

-- 显示所有已创建的索引
SHOW INDEX FROM commodities;

-- 显示索引使用情况
SELECT 
    TABLE_NAME,
    INDEX_NAME,
    SEQ_IN_INDEX,
    COLUMN_NAME,
    CARDINALITY
FROM information_schema.STATISTICS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'commodities'
ORDER BY INDEX_NAME, SEQ_IN_INDEX;

-- ========================================
-- 第四部分：索引说明
-- ========================================

/*
联合索引说明：

1. idx_seller_status (seller_id, commodity_status)
   - 优化：findBySellerIdAndCommodityStatus
   - 使用：WHERE seller_id = ? AND commodity_status = ?

2. idx_status_seller_buyer_visibility (commodity_status, seller_visibility, buyer_visibility)
   - 优化：findByCommodityStatusAndSellerVisibilityAndBuyerVisibility
   - 使用：WHERE commodity_status = ? AND seller_visibility = ? AND buyer_visibility = ?

3. idx_status_publish_time (commodity_status, publish_time DESC)
   - 优化：按状态和时间排序查询
   - 使用：WHERE commodity_status = ? ORDER BY publish_time DESC

4. idx_status_click_count (commodity_status, click_count DESC)
   - 优化：热门商品查询
   - 使用：WHERE commodity_status = ? ORDER BY click_count DESC

5. idx_category_status_visibility (category, commodity_status, seller_visibility, buyer_visibility)
   - 优化：分类查询可见商品
   - 使用：WHERE category = ? AND commodity_status = ? AND seller_visibility = ? AND buyer_visibility = ?

6. idx_status_price (commodity_status, price)
   - 优化：价格范围查询
   - 使用：WHERE commodity_status = ? AND price BETWEEN ? AND ?

7. idx_seller_publish_time (seller_id, publish_time DESC)
   - 优化：卖家商品按时间排序
   - 使用：WHERE seller_id = ? ORDER BY publish_time DESC

8. idx_seller_click_count (seller_id, click_count DESC)
   - 优化：卖家热门商品查询
   - 使用：WHERE seller_id = ? ORDER BY click_count DESC

索引选择原则：
- 最左前缀匹配：联合索引从左到右匹配
- 排序优化：DESC 排序需要创建对应的索引
- 覆盖索引：索引包含所有查询字段可以避免回表
- 选择性：高选择性字段优先放在索引前面

注意：
- 索引创建会增加存储空间和写入性能开销
- 需要定期执行 ANALYZE TABLE 更新索引统计信息
- 对于频繁更新的字段（如 click_count），索引维护成本较高

建议：
- 根据实际查询模式和性能监控结果调整索引
- 定期检查未使用的索引并删除
- 使用 EXPLAIN 分析查询执行计划
*/

-- ========================================
-- 脚本执行完成
-- ========================================

