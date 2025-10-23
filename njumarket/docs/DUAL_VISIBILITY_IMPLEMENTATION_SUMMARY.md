# 双可见性功能实现总结

## 功能概述
将原有的单一可见性字段扩展为**卖家可见性**和**买家可见性**，实现更精细的可见性控制。

## 实现的功能

### 1. 数据库架构更新
- **文件**: `njumarket/src/main/resources/database/update_extend_visibility_fields.sql`
- **变更内容**:
  - 商品表：`visibility` → `seller_visibility` + `buyer_visibility`
  - 订单表：`visibility` → `seller_visibility` + `buyer_visibility`
  - 新增可见性类型说明表：`visibility_types`
  - 更新订单状态记录表，支持记录可见性变更

### 2. 实体类更新

#### 2.1 Commodity实体
- **新增字段**:
  - `sellerVisibility`: 卖家可见性（PUBLIC/PRIVATE/HIDDEN）
  - `buyerVisibility`: 买家可见性（PUBLIC/PRIVATE/HIDDEN）
- **新增方法**:
  - `setSellerVisibility()`: 设置卖家可见性
  - `setBuyerVisibility()`: 设置买家可见性
  - `isVisibleToSeller()`: 检查对卖家是否可见
  - `isVisibleToBuyer()`: 检查对买家是否可见
  - `setVisibility()`: 兼容旧接口，同时设置两种可见性

#### 2.2 Order实体
- **新增字段**:
  - `sellerVisibility`: 卖家可见性（PUBLIC/PRIVATE/HIDDEN）
  - `buyerVisibility`: 买家可见性（PUBLIC/PRIVATE/HIDDEN）
- **新增方法**:
  - `setSellerVisibility()`: 设置卖家可见性
  - `setBuyerVisibility()`: 设置买家可见性
  - `isVisibleToSeller()`: 检查对卖家是否可见
  - `isVisibleToBuyer()`: 检查对买家是否可见
  - `setVisibility()`: 兼容旧接口，同时设置两种可见性

### 3. DTO类更新
- **CommodityDTO**: 添加`sellerVisibility`和`buyerVisibility`字段
- **OrderDTO**: 添加`sellerVisibility`和`buyerVisibility`字段

### 4. Repository层更新

#### 4.1 CommodityRepository
- **新增查询方法**:
  - `findBySellerVisibility()`: 根据卖家可见性查询
  - `findByBuyerVisibility()`: 根据买家可见性查询
  - `findBySellerVisibilityAndBuyerVisibility()`: 根据双可见性查询
  - `findByCommodityStatusAndSellerVisibilityAndBuyerVisibility()`: 根据状态和双可见性查询
- **更新查询语句**: 所有公共查询都要求`sellerVisibility = 'PUBLIC' AND buyerVisibility = 'PUBLIC'`

#### 4.2 OrderRepository
- **新增查询方法**:
  - `findBySellerVisibility()`: 根据卖家可见性查询
  - `findByBuyerVisibility()`: 根据买家可见性查询
  - `findByBuyerIdAndSellerVisibility()`: 根据买家ID和卖家可见性查询
  - `findByBuyerIdAndBuyerVisibility()`: 根据买家ID和买家可见性查询
  - `findBySellerIdAndSellerVisibility()`: 根据卖家ID和卖家可见性查询
  - `findBySellerIdAndBuyerVisibility()`: 根据卖家ID和买家可见性查询

### 5. Service层更新

#### 5.1 CommodityService接口
- **新增方法**:
  - `updateCommoditySellerVisibility()`: 修改商品卖家可见性
  - `updateCommodityBuyerVisibility()`: 修改商品买家可见性
  - `updateCommodityVisibility()`: 兼容旧接口

#### 5.2 OrderService接口
- **新增方法**:
  - `updateOrderSellerVisibility()`: 修改订单卖家可见性
  - `updateOrderBuyerVisibility()`: 修改订单买家可见性
  - `updateOrderVisibility()`: 兼容旧接口

#### 5.3 Service实现
- **权限控制**:
  - 商品卖家可见性：只有商品所有者可以修改
  - 商品买家可见性：只有商品所有者可以修改
  - 订单卖家可见性：只有卖家可以修改
  - 订单买家可见性：只有买家可以修改
- **业务逻辑**:
  - 商品发布时默认设置为PUBLIC
  - 商品下架时同时设置为HIDDEN
  - 订单创建时默认设置为PUBLIC

### 6. Controller层更新

#### 6.1 UserCommodityController
- **新增接口**:
  - `PUT /api/user/commodity/{commodityId}/seller-visibility`: 修改商品卖家可见性
  - `PUT /api/user/commodity/{commodityId}/buyer-visibility`: 修改商品买家可见性
  - `PUT /api/user/commodity/{commodityId}/visibility`: 兼容旧接口

#### 6.2 UserOrderController
- **新增接口**:
  - `PUT /api/user/order/{orderId}/seller-visibility`: 修改订单卖家可见性
  - `PUT /api/user/order/{orderId}/buyer-visibility`: 修改订单买家可见性
  - `PUT /api/user/order/{orderId}/visibility`: 兼容旧接口

## 可见性类型说明

### 可见性值
- **PUBLIC**: 公开可见
- **PRIVATE**: 私有（仅自己可见）
- **HIDDEN**: 隐藏（完全不可见）

### 应用场景

#### 商品可见性
- **卖家可见性**: 控制商品在卖家管理界面中的显示
- **买家可见性**: 控制商品在公共浏览和搜索中的显示
- **组合效果**:
  - `PUBLIC + PUBLIC`: 完全公开
  - `PUBLIC + PRIVATE`: 卖家可见，买家不可见
  - `PRIVATE + PUBLIC`: 卖家不可见，买家可见
  - `PRIVATE + PRIVATE`: 完全私有
  - `HIDDEN + *`: 完全隐藏

#### 订单可见性
- **卖家可见性**: 控制订单在卖家订单列表中的显示
- **买家可见性**: 控制订单在买家订单列表中的显示
- **组合效果**:
  - `PUBLIC + PUBLIC`: 双方都可见
  - `PUBLIC + PRIVATE`: 卖家可见，买家不可见
  - `PRIVATE + PUBLIC`: 卖家不可见，买家可见
  - `PRIVATE + PRIVATE`: 双方都不可见
  - `HIDDEN + *`: 完全隐藏

## 权限控制规则

### 商品可见性修改权限
- **卖家可见性**: 只有商品所有者可以修改
- **买家可见性**: 只有商品所有者可以修改
- **状态限制**: 已售罄的商品不能修改可见性

### 订单可见性修改权限
- **卖家可见性**: 只有订单卖家可以修改
- **买家可见性**: 只有订单买家可以修改
- **状态限制**: 已完成或已取消的订单不能修改可见性

## 兼容性设计

### 向后兼容
- 保留了原有的`setVisibility()`和`isVisible()`方法
- 原有的可见性修改接口仍然可用
- 数据库迁移脚本自动处理数据转换

### 渐进式升级
- 新功能使用双可见性字段
- 旧功能继续使用兼容方法
- 可以逐步迁移到新的双可见性接口

## 使用示例

### 商品可见性管理
```java
// 设置商品对卖家公开，对买家私有
commodity.setSellerVisibility("PUBLIC");
commodity.setBuyerVisibility("PRIVATE");

// 检查商品对买家是否可见
if (commodity.isVisibleToBuyer()) {
    // 商品对买家可见
}
```

### 订单可见性管理
```java
// 卖家设置订单对自己隐藏
order.setSellerVisibility("HIDDEN");

// 买家设置订单对自己公开
order.setBuyerVisibility("PUBLIC");
```

### API调用示例
```bash
# 修改商品卖家可见性
PUT /api/user/commodity/{commodityId}/seller-visibility?sellerVisibility=PRIVATE

# 修改订单买家可见性
PUT /api/user/order/{orderId}/buyer-visibility?buyerVisibility=HIDDEN
```

## 数据库更新

执行以下脚本更新数据库：
```sql
-- 执行双可见性扩展脚本
source njumarket/src/main/resources/database/update_extend_visibility_fields.sql
```

## 总结

双可见性功能的实现提供了更精细的可见性控制：

✅ **数据库架构**: 扩展为双可见性字段
✅ **实体类**: 添加双可见性字段和业务方法
✅ **Repository**: 支持双可见性查询
✅ **Service**: 实现双可见性业务逻辑和权限控制
✅ **Controller**: 提供双可见性管理接口
✅ **兼容性**: 保持向后兼容
✅ **权限控制**: 精确的角色权限管理

这个实现为商品和订单提供了更灵活的可见性管理，满足了不同角色对可见性的不同需求。
