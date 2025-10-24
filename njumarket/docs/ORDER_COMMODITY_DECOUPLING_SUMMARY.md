# 订单商品解耦重构总结

## 重构目标

本次重构旨在降低订单和商品模块之间的耦合度，实现以下目标：

1. **订单存储商品快照**：订单创建时保存商品信息的快照，减少对商品表的依赖
2. **条件性库存恢复**：只有未发货和未付款的订单取消时才恢复库存
3. **支持查询下架商品**：允许查询下架商品但提示状态，无法下单
4. **实现"再来一单"功能**：基于商品快照重新创建订单
5. **图片处理优化**：订单图片不复用商品图片，置为null（暂时）

## 数据库变更

### 1. 订单表结构更新

在`orders`表中添加了以下商品快照字段：

```sql
-- 商品快照字段
ALTER TABLE `orders` 
ADD COLUMN `commodity_snapshot_title` varchar(200) COMMENT '商品快照-标题',
ADD COLUMN `commodity_snapshot_description` text COMMENT '商品快照-描述',
ADD COLUMN `commodity_snapshot_price` decimal(10,2) COMMENT '商品快照-价格',
ADD COLUMN `commodity_snapshot_location` varchar(200) COMMENT '商品快照-位置',
ADD COLUMN `commodity_snapshot_category` varchar(50) COMMENT '商品快照-分类',
ADD COLUMN `commodity_snapshot_condition_level` varchar(20) COMMENT '商品快照-成色',
ADD COLUMN `commodity_snapshot_images` text COMMENT '商品快照-图片(JSON格式)',
ADD COLUMN `commodity_snapshot_status` varchar(20) COMMENT '商品快照-状态',
ADD COLUMN `commodity_snapshot_seller_name` varchar(100) COMMENT '商品快照-卖家名称',
ADD COLUMN `commodity_snapshot_seller_phone` varchar(20) COMMENT '商品快照-卖家电话',
ADD COLUMN `commodity_snapshot_seller_email` varchar(100) COMMENT '商品快照-卖家邮箱',
ADD COLUMN `commodity_snapshot_time` datetime COMMENT '商品快照时间';
```

### 2. 数据迁移

通过联合查询更新现有订单的商品快照信息：

```sql
UPDATE `orders` o 
JOIN `commodities` c ON o.commodity_id = c.commodity_id
JOIN `users` u ON c.seller_id = u.user_id
SET 
    o.commodity_snapshot_title = c.title,
    o.commodity_snapshot_description = c.description,
    o.commodity_snapshot_price = c.price,
    o.commodity_snapshot_location = c.location,
    o.commodity_snapshot_category = c.category,
    o.commodity_snapshot_condition_level = c.condition_level,
    o.commodity_snapshot_images = NULL, -- 暂时置为NULL
    o.commodity_snapshot_status = c.commodity_status,
    o.commodity_snapshot_seller_name = u.username,
    o.commodity_snapshot_seller_phone = u.primary_phone,
    o.commodity_snapshot_seller_email = NULL,
    o.commodity_snapshot_time = o.create_time;
```

## 代码变更

### 1. Order实体类更新

**新增字段**：
- 商品快照相关字段（标题、描述、价格、位置、分类、成色、图片、状态、卖家信息等）
- 快照时间字段

**新增方法**：
- `createCommoditySnapshot(Commodity commodity, User seller)`：创建商品快照
- `canRestoreStock()`：检查订单是否可以恢复库存
- `isCommoditySnapshotOffShelf()`：检查商品快照是否已下架

### 2. OrderDTO更新

添加了所有商品快照字段，用于数据传输。

### 3. OrderService更新

**新增方法**：
- `reorderFromSnapshot(String orderId)`：基于商品快照实现"再来一单"功能

**修改逻辑**：
- **订单创建**：创建订单时同时创建商品快照
- **订单取消**：只有未发货和未付款的订单取消时才恢复库存
- **DTO转换**：包含商品快照字段的转换

### 4. CommodityQueryService更新

**新增方法**：
- `canCommodityBeQueried(Commodity commodity, User user)`：检查商品是否可以被查询（包括下架商品）
- `canCommodityBeOrdered(Commodity commodity)`：检查商品是否可以下单
- `getCommodityByIdForReorder(String commodityId)`：查询商品（支持下架商品）

**修改逻辑**：
- 支持查询下架商品，但会提示状态信息
- 区分"可查询"和"可下单"的权限

### 5. 控制器更新

**UserOrderController**：
- 新增`reorderFromSnapshot`接口：`POST /api/user/order/{orderId}/reorder`

## 解耦效果

### 1. 数据层解耦

**之前**：
- 订单表通过外键强依赖商品表
- 订单查询需要JOIN商品表
- 商品删除会级联删除订单

**之后**：
- 订单表包含商品快照信息
- 订单查询可以独立进行
- 商品状态变更不影响历史订单显示

### 2. 业务逻辑解耦

**之前**：
- 订单创建时必须验证商品状态
- 订单取消时无条件恢复库存
- 订单显示依赖实时商品信息

**之后**：
- 订单创建时保存商品快照
- 订单取消时条件性恢复库存
- 订单显示使用快照信息

### 3. 功能增强

**新增功能**：
- "再来一单"功能：基于历史订单快速重新下单
- 下架商品查询：允许查看下架商品但提示状态
- 库存恢复优化：避免已发货订单的库存混乱

## 使用说明

### 1. 数据库更新

执行数据库更新脚本：
```bash
mysql -u username -p database_name < njumarket/src/main/resources/database/update_order_commodity_decoupling.sql
```

### 2. API使用

**再来一单**：
```http
POST /api/user/order/{orderId}/reorder
```

**查询下架商品**：
```http
GET /api/public/commodity/{commodityId}?includeOffShelf=true
```

### 3. 前端适配

前端需要适配以下变更：
- 订单详情显示商品快照信息
- 添加"再来一单"按钮
- 处理下架商品的显示逻辑

## 注意事项

### 1. 数据一致性

- 商品快照在订单创建时生成，不会自动更新
- 历史订单显示的是创建时的商品信息
- 商品删除不会影响历史订单

### 2. 性能考虑

- 订单表增加了多个字段，存储空间增加
- 商品快照查询不需要JOIN商品表，查询性能提升
- 建议定期清理过期的商品快照数据

### 3. 扩展性

- 商品快照字段可以根据需要扩展
- 图片处理可以后续优化为独立存储
- 支持更复杂的商品快照版本管理

## 测试建议

### 1. 功能测试

- 测试订单创建时的商品快照生成
- 测试不同状态订单的库存恢复逻辑
- 测试"再来一单"功能的完整性
- 测试下架商品的查询和显示

### 2. 数据测试

- 验证现有订单的商品快照数据迁移
- 测试商品状态变更对历史订单的影响
- 验证库存恢复的准确性

### 3. 性能测试

- 测试订单查询性能（有/无商品快照）
- 测试大量订单创建时的性能影响
- 测试数据库存储空间使用情况

## 后续优化建议

1. **图片处理**：实现商品图片的独立存储和复制
2. **快照版本管理**：支持商品快照的版本控制
3. **数据清理**：定期清理过期的商品快照数据
4. **缓存优化**：对商品快照查询进行缓存优化
5. **监控告警**：添加商品快照生成的监控和告警

## 总结

本次重构成功实现了订单和商品模块的解耦，提高了系统的可维护性和扩展性。通过商品快照机制，订单模块不再强依赖商品模块的实时状态，同时新增的"再来一单"功能提升了用户体验。解耦后的系统更加稳定，能够更好地处理商品状态变更和历史数据的一致性。
