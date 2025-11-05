# 商品表索引分析与优化建议

## 📋 概述

本文档详细分析 `commodities` 表的索引使用情况，识别冗余索引，并提供优化建议。

---

## 1. 现有索引列表

### 1.1 主键索引
- ✅ `PRIMARY` (commodity_id) - **必需，不可删除**

### 1.2 单字段索引
1. `idx_seller_id` (seller_id)
2. `idx_commodity_status` (commodity_status)
3. `idx_publish_time` (publish_time)
4. `idx_price` (price)
5. `idx_click_count` (click_count)
6. `idx_category` (category)
7. `idx_condition_level` (condition_level)
8. `idx_seller_visibility` (seller_visibility)
9. `idx_buyer_visibility` (buyer_visibility)
10. `idx_stock` (stock)

### 1.3 联合索引
11. `idx_seller_status` (seller_id, commodity_status)
12. `idx_status_seller_buyer_visibility` (commodity_status, seller_visibility, buyer_visibility)
13. `idx_status_publish_time` (commodity_status, publish_time DESC)
14. `idx_status_click_count` (commodity_status, click_count DESC)
15. `idx_category_status_visibility` (category, commodity_status, seller_visibility, buyer_visibility)
16. `idx_status_price` (commodity_status, price)
17. `idx_seller_publish_time` (seller_id, publish_time DESC)
18. `idx_seller_click_count` (seller_id, click_count DESC)

---

## 2. 查询场景分析

### 2.1 高频查询场景

#### 场景1：按卖家ID查询商品（分页）
```java
Page<Commodity> findBySellerId(String sellerId, Pageable pageable);
```
**使用索引**：`idx_seller_id` ✅
**排序**：通常按 `publish_time` DESC，可使用 `idx_seller_publish_time`

#### 场景2：按卖家ID和状态查询（分页）
```java
Page<Commodity> findBySellerIdAndCommodityStatus(String sellerId, String status, Pageable pageable);
```
**使用索引**：`idx_seller_status` ✅
**排序**：通常按 `publish_time` DESC，需要联合索引

#### 场景3：公开商品列表（高频）
```java
Page<Commodity> findByCommodityStatusAndSellerVisibilityAndBuyerVisibility(
    "ON_SHELF", "PUBLIC", "PUBLIC", pageable
);
```
**使用索引**：`idx_status_seller_buyer_visibility` ✅
**排序**：通常按 `publish_time` DESC，可使用 `idx_status_publish_time`

#### 场景4：按分类查询商品（高频）
```java
Page<Commodity> findByCategoryAndVisible(String category, pageable);
// WHERE category = ? AND status = 'ON_SHELF' AND seller_visibility = 'PUBLIC' AND buyer_visibility = 'PUBLIC'
```
**使用索引**：`idx_category_status_visibility` ✅

#### 场景5：热门商品（按点击量排序）
```java
findHotCommodities(Pageable pageable);
// WHERE status = 'ON_SHELF' AND seller_visibility = 'PUBLIC' AND buyer_visibility = 'PUBLIC'
// ORDER BY click_count DESC
```
**使用索引**：`idx_status_click_count` ✅

#### 场景6：最新商品（按发布时间排序）
```java
findLatestCommodities(Pageable pageable);
// WHERE status = 'ON_SHELF' AND seller_visibility = 'PUBLIC' AND buyer_visibility = 'PUBLIC'
// ORDER BY publish_time DESC
```
**使用索引**：`idx_status_publish_time` ✅

#### 场景7：价格范围查询
```java
findByPriceRange(Double minPrice, Double maxPrice);
// WHERE price BETWEEN ? AND ? AND status = 'ON_SHELF' AND visibility = 'PUBLIC'
```
**使用索引**：`idx_status_price` ✅

#### 场景8：管理端动态查询
```java
// 使用 Specification，可能涉及多个字段组合
// WHERE category = ? AND condition_level = ? AND status = ? AND seller_visibility = ? AND buyer_visibility = ?
// ORDER BY publish_time DESC / click_count DESC / price ASC
```
**使用索引**：根据查询条件选择相应的联合索引

---

## 3. 索引冗余分析

### 3.1 冗余索引识别

#### ❌ 可以删除的单字段索引

1. **`idx_commodity_status`** - 冗余
   - **原因**：已有联合索引 `idx_status_publish_time`、`idx_status_click_count`、`idx_status_price`、`idx_status_seller_buyer_visibility` 都包含 `commodity_status` 作为第一列
   - **影响**：单独查询 `commodity_status` 时，可以使用联合索引的前缀部分（虽然效率略低，但可接受）

2. **`idx_seller_visibility`** - 冗余
   - **原因**：已有联合索引 `idx_status_seller_buyer_visibility` 包含 `seller_visibility`
   - **影响**：单独查询 `seller_visibility` 的频率很低，通常与 `status` 和 `buyer_visibility` 一起使用

3. **`idx_buyer_visibility`** - 冗余
   - **原因**：已有联合索引 `idx_status_seller_buyer_visibility` 包含 `buyer_visibility`
   - **影响**：单独查询 `buyer_visibility` 的频率很低，通常与 `status` 和 `seller_visibility` 一起使用

4. **`idx_publish_time`** - 可能冗余
   - **原因**：已有联合索引 `idx_status_publish_time`、`idx_seller_publish_time` 都包含 `publish_time`
   - **影响**：如果查询只按 `publish_time` 排序（没有 WHERE 条件），此索引有用；但这种情况很少
   - **建议**：保留，因为排序时可能需要

5. **`idx_price`** - 可能冗余
   - **原因**：已有联合索引 `idx_status_price` 包含 `price`
   - **影响**：如果查询只按 `price` 排序（没有 WHERE 条件），此索引有用；但这种情况很少
   - **建议**：保留，因为价格范围查询可能需要

6. **`idx_click_count`** - 可能冗余
   - **原因**：已有联合索引 `idx_status_click_count`、`idx_seller_click_count` 都包含 `click_count`
   - **影响**：如果查询只按 `click_count` 排序（没有 WHERE 条件），此索引有用；但这种情况很少
   - **建议**：保留，因为热门商品查询可能需要

#### ✅ 必需保留的索引

1. **`idx_seller_id`** - 必需
   - **原因**：`findBySellerId()` 查询频繁使用
   - **注意**：虽然有 `idx_seller_status`，但单独查询 `seller_id` 时，MySQL 会选择更合适的索引

2. **`idx_category`** - 必需
   - **原因**：`findByCategoryAndVisible()` 查询使用 `idx_category_status_visibility`，但如果只按 `category` 查询，此索引有用

3. **`idx_condition_level`** - 必需
   - **原因**：管理端查询可能按 `condition_level` 筛选，没有联合索引包含此字段

4. **`idx_stock`** - 必需
   - **原因**：库存查询和更新需要，没有联合索引包含此字段

---

## 4. 缺失的索引

### 4.1 需要添加的索引

#### 索引1：卖家商品排序优化（推荐）
```sql
-- 如果按卖家ID查询并排序，需要更完整的联合索引
CREATE INDEX idx_seller_status_publish_time 
ON commodities(seller_id, commodity_status, publish_time DESC);
```
**原因**：
- `findBySellerIdAndCommodityStatus()` 查询后通常按 `publish_time` 排序
- 现有 `idx_seller_status` 不包含排序字段，需要额外排序操作
- 可以替代 `idx_seller_status` 和 `idx_seller_publish_time`

#### 索引2：价格范围查询优化（可选）
```sql
-- 如果价格范围查询频繁，可以添加
CREATE INDEX idx_status_price_range 
ON commodities(commodity_status, seller_visibility, buyer_visibility, price);
```
**原因**：
- `findByPriceRange()` 查询需要 `status`、`visibility` 和 `price`
- 现有 `idx_status_price` 不包含 `visibility` 字段

#### 索引3：管理端查询优化（可选）
```sql
-- 如果管理端经常按分类和状态筛选
CREATE INDEX idx_category_condition_status 
ON commodities(category, condition_level, commodity_status);
```
**原因**：
- 管理端查询可能同时按 `category`、`condition_level`、`status` 筛选
- 现有 `idx_category_status_visibility` 不包含 `condition_level`

---

## 5. 索引优化建议

### 5.1 可以删除的索引（推荐）

```sql
-- 删除冗余的单字段索引
DROP INDEX idx_commodity_status ON commodities;
DROP INDEX idx_seller_visibility ON commodities;
DROP INDEX idx_buyer_visibility ON commodities;
```

**删除理由**：
- 这些字段都已包含在联合索引中
- 删除后可以减少索引维护成本
- 查询性能影响很小（可以使用联合索引的前缀）

### 5.2 可以合并的索引

#### 方案1：合并卖家相关索引
```sql
-- 删除旧的索引
DROP INDEX idx_seller_status ON commodities;
DROP INDEX idx_seller_publish_time ON commodities;

-- 创建新的联合索引（包含排序字段）
CREATE INDEX idx_seller_status_publish_time 
ON commodities(seller_id, commodity_status, publish_time DESC);
```

**优势**：
- 一个索引支持多种查询场景
- 包含排序字段，避免额外排序操作
- 减少索引数量

#### 方案2：合并点击量相关索引
```sql
-- 如果 idx_seller_click_count 使用频率不高，可以删除
DROP INDEX idx_seller_click_count ON commodities;
-- 保留 idx_status_click_count，因为热门商品查询更频繁
```

---

## 6. 最终索引方案

### 6.1 保留的索引

| 索引名 | 字段 | 用途 |
|--------|------|------|
| PRIMARY | commodity_id | 主键 |
| idx_seller_id | seller_id | 按卖家查询 |
| idx_category | category | 按分类查询（前缀匹配） |
| idx_condition_level | condition_level | 按成色查询 |
| idx_stock | stock | 库存查询 |
| idx_publish_time | publish_time | 按时间排序（全局） |
| idx_price | price | 价格范围查询 |
| idx_click_count | click_count | 按点击量排序（全局） |
| **idx_seller_status_publish_time** | seller_id, status, publish_time | **卖家商品查询+排序（新增）** |
| idx_status_seller_buyer_visibility | status, seller_visibility, buyer_visibility | 公开商品查询 |
| idx_status_publish_time | status, publish_time | 最新商品查询 |
| idx_status_click_count | status, click_count | 热门商品查询 |
| idx_status_price | status, price | 价格范围查询 |
| idx_category_status_visibility | category, status, seller_visibility, buyer_visibility | 分类商品查询 |

### 6.2 删除的索引

| 索引名 | 删除原因 |
|--------|---------|
| idx_commodity_status | 已包含在多个联合索引中 |
| idx_seller_visibility | 已包含在联合索引中 |
| idx_buyer_visibility | 已包含在联合索引中 |
| idx_seller_status | 被 `idx_seller_status_publish_time` 替代 |
| idx_seller_publish_time | 被 `idx_seller_status_publish_time` 替代 |
| idx_seller_click_count | 使用频率低，可删除 |

---

## 7. 索引优化脚本

### 7.1 删除冗余索引

```sql
-- 删除冗余的单字段索引
DROP INDEX IF EXISTS idx_commodity_status ON commodities;
DROP INDEX IF EXISTS idx_seller_visibility ON commodities;
DROP INDEX IF EXISTS idx_buyer_visibility ON commodities;

-- 删除可以合并的索引
DROP INDEX IF EXISTS idx_seller_status ON commodities;
DROP INDEX IF EXISTS idx_seller_publish_time ON commodities;
DROP INDEX IF EXISTS idx_seller_click_count ON commodities;
```

### 7.2 创建优化后的索引

```sql
-- 创建卖家商品查询+排序的联合索引
CREATE INDEX idx_seller_status_publish_time 
ON commodities(seller_id, commodity_status, publish_time DESC);
```

### 7.3 验证索引

```sql
-- 查看优化后的索引
SHOW INDEX FROM commodities;

-- 测试查询性能
EXPLAIN SELECT * FROM commodities 
WHERE seller_id = 'xxx' AND commodity_status = 'ON_SHELF' 
ORDER BY publish_time DESC 
LIMIT 20;
```

---

## 8. 性能影响评估

### 8.1 删除索引的影响

| 索引 | 删除影响 | 评估 |
|------|---------|------|
| idx_commodity_status | 单独查询 status 时性能略降 | ⚠️ 可接受（通常与其他条件一起查询） |
| idx_seller_visibility | 单独查询 seller_visibility 时性能略降 | ✅ 影响很小（很少单独查询） |
| idx_buyer_visibility | 单独查询 buyer_visibility 时性能略降 | ✅ 影响很小（很少单独查询） |

### 8.2 新增索引的收益

| 索引 | 收益 | 评估 |
|------|------|------|
| idx_seller_status_publish_time | 卖家商品查询+排序性能提升 | ✅ 显著提升（避免额外排序） |

### 8.3 索引维护成本

**优化前**：19 个索引
**优化后**：14 个索引
**减少**：5 个索引（约 26% 减少）

**维护成本降低**：
- INSERT/UPDATE/DELETE 操作更快（减少索引维护）
- 存储空间减少（约 10-20%）

---

## 9. 实施建议

### 9.1 分阶段实施

**阶段1：删除明显的冗余索引**
```sql
DROP INDEX idx_commodity_status ON commodities;
DROP INDEX idx_seller_visibility ON commodities;
DROP INDEX idx_buyer_visibility ON commodities;
```

**阶段2：合并卖家相关索引**
```sql
DROP INDEX idx_seller_status ON commodities;
DROP INDEX idx_seller_publish_time ON commodities;
CREATE INDEX idx_seller_status_publish_time 
ON commodities(seller_id, commodity_status, publish_time DESC);
```

**阶段3：删除低频索引**
```sql
DROP INDEX idx_seller_click_count ON commodities;
```

### 9.2 监控指标

**实施后监控**：
- 查询性能（通过慢查询日志）
- 索引使用率（通过 `EXPLAIN` 分析）
- 写入性能（INSERT/UPDATE/DELETE 耗时）

---

## 10. 总结

### 10.1 优化效果

**索引数量**：从 19 个减少到 14 个（减少 26%）
**冗余索引**：删除 6 个冗余索引
**新增索引**：1 个优化索引（`idx_seller_status_publish_time`）

### 10.2 性能提升

- ✅ **查询性能**：卖家商品查询+排序性能提升（避免额外排序）
- ✅ **写入性能**：INSERT/UPDATE/DELETE 性能提升（减少索引维护）
- ✅ **存储空间**：减少约 10-20%

### 10.3 风险控制

- ⚠️ **单独查询 status**：性能略降，但可接受（通常与其他条件一起查询）
- ⚠️ **单独查询 visibility**：影响很小（很少单独查询）

---

**文档版本**：v1.0  
**最后更新**：2025-11-05  
**维护者**：NJUMarket 开发团队

