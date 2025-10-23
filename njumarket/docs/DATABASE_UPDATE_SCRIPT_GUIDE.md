# 数据库更新脚本说明

## 概述
已将三个独立的SQL脚本合并为一个完整的数据库更新脚本：`complete_database_update.sql`

## 合并的脚本
1. `update_add_visibility_fields.sql` - 基础可见性字段和商品订单字段
2. `update_extend_visibility_fields.sql` - 双可见性字段扩展
3. `update_add_return_fields.sql` - 退货功能相关字段

## 合并后的脚本内容

### 第一部分：基础可见性字段和商品订单字段
- 为商品表添加：`category`、`condition_level`、`images`字段
- 为订单表添加：`shipping_time`、`delivery_time`、`tracking_number`、`shipping_address`、`remark`字段

### 第二部分：双可见性字段（卖家可见性和买家可见性）
- 为商品表添加：`seller_visibility`、`buyer_visibility`字段
- 为订单表添加：`seller_visibility`、`buyer_visibility`字段
- 添加相应的索引

### 第三部分：退货相关字段
- 为订单表添加：`return_reason`、`return_request_time`、`return_approval_time`、`return_rejection_reason`、`return_tracking_number`、`return_completion_time`字段
- 添加退货状态索引

### 第四部分：数据更新
- 更新商品可见性数据（根据商品状态设置相应的可见性）
- 更新订单可见性数据（根据订单状态设置相应的可见性）

### 第五部分：创建辅助表
- `commodity_categories` - 商品分类表
- `visibility_types` - 可见性类型表
- `return_records` - 退货记录表
- `return_reason_types` - 退货原因类型表
- `order_status_logs` - 订单状态变更记录表

### 第六部分：创建视图和触发器
- `v_return_statistics` - 退货统计视图
- `tr_order_return_request` - 退货申请触发器

### 第七部分：状态说明
- 订单状态说明
- 商品状态说明
- 可见性状态说明

## 使用方法

### 执行脚本
```sql
-- 执行完整的数据库更新脚本
source njumarket/src/main/resources/database/complete_database_update.sql
```

### 脚本特点
1. **原子性**: 整个脚本在一个事务中执行
2. **安全性**: 包含外键检查控制
3. **完整性**: 包含所有必要的字段、索引、约束
4. **可追溯性**: 包含状态变更记录表
5. **自动化**: 包含触发器和视图

### 注意事项
1. 执行前请备份数据库
2. 确保数据库用户有足够的权限
3. 脚本会自动处理外键约束
4. 所有字段都有默认值，不会影响现有数据

## 字段说明

### 商品表新增字段
- `category`: 商品分类
- `condition_level`: 商品成色
- `images`: 商品图片
- `seller_visibility`: 卖家可见性
- `buyer_visibility`: 买家可见性

### 订单表新增字段
- `shipping_time`: 发货时间
- `delivery_time`: 签收时间
- `tracking_number`: 快递单号
- `shipping_address`: 收货地址
- `remark`: 订单备注
- `seller_visibility`: 卖家可见性
- `buyer_visibility`: 买家可见性
- `return_reason`: 退货原因
- `return_request_time`: 退货申请时间
- `return_approval_time`: 退货审批时间
- `return_rejection_reason`: 退货拒绝原因
- `return_tracking_number`: 退货快递单号
- `return_completion_time`: 退货完成时间

## 状态枚举

### 订单状态
- `CREATED`: 已创建
- `PAID`: 已支付
- `SHIPPED`: 已发货
- `COMPLETED`: 已完成
- `CANCELLED`: 已取消
- `REFUNDED`: 已退款
- `RETURN_REQUESTED`: 退货申请中
- `RETURN_APPROVED`: 退货已同意
- `RETURN_REJECTED`: 退货已拒绝
- `RETURN_COMPLETED`: 退货已完成

### 商品状态
- `DRAFT`: 草稿
- `PUBLISHED`: 已发布
- `SOLD_OUT`: 售罄
- `REMOVED`: 已下架

### 可见性状态
- `PUBLIC`: 公开可见
- `PRIVATE`: 私有（仅自己可见）
- `HIDDEN`: 隐藏（完全不可见）

## 总结
合并后的脚本提供了完整的数据库更新功能，包括：
- ✅ 基础字段扩展
- ✅ 双可见性支持
- ✅ 退货功能支持
- ✅ 辅助表和视图
- ✅ 触发器和约束
- ✅ 数据迁移和更新

这个统一的脚本确保了数据库架构的一致性和完整性。
