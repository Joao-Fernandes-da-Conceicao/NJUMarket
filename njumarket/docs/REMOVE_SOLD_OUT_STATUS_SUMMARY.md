# 移除已售罄状态总结

## 概述

本次更新完全移除了项目中的"已售罄"（SOLD_OUT）状态和相关逻辑，简化了商品状态管理体系。

## 变更内容

### 1. 后端变更

#### 1.1 实体类更新
- **文件**: `Commodity.java`
- **变更**: 移除了注释中的SOLD_OUT状态说明
- **影响**: 商品状态字段注释更新为：`DRAFT, PUBLISHED, ON_SHELF, OFF_SHELF`

#### 1.2 服务层更新
- **文件**: `CommodityService.java`
- **变更**: 移除了`soldOutCommodity`方法声明
- **文件**: `CommodityServiceImpl.java`
- **变更**: 
  - 移除了`soldOutCommodity`方法实现
  - 移除了批量操作中的`soldOut`处理逻辑

#### 1.3 控制器更新
- **文件**: `UserCommodityController.java`
- **变更**: 移除了`/sold-out`接口和相关方法

### 2. 前端变更

#### 2.1 页面组件更新
- **文件**: `MyCommodities.vue`
- **变更**:
  - 移除了"已售完"标签页
  - 移除了"设为售罄"按钮
  - 移除了`handleSoldOut`方法
  - 更新了状态映射，移除SOLD_OUT相关逻辑

#### 2.2 通用组件更新
- **文件**: `CommodityCard.vue`
- **变更**:
  - 移除了SOLD_OUT状态的颜色和文本映射
  - 更新了状态显示逻辑

#### 2.3 API接口更新
- **文件**: `index.js`
- **变更**: 移除了`soldOut` API方法

#### 2.4 状态管理更新
- **文件**: `commodity.js`
- **变更**: 移除了store中SOLD_OUT状态的统计处理

### 3. 数据库变更

#### 3.1 数据迁移
- **文件**: `remove_sold_out_status.sql`
- **变更**:
  - 将所有SOLD_OUT状态的商品改为DRAFT状态
  - 设置可见性为PRIVATE/HIDDEN
  - 更新了状态说明视图和统计视图

#### 3.2 状态约束更新
- 更新了商品状态字段的注释
- 移除了SOLD_OUT状态的相关约束

## 新的状态体系

### 状态定义
1. **DRAFT** - 草稿状态
   - 行为：不可见，不可购买，可编辑
   - 可见性：PRIVATE/HIDDEN
   - 用途：商品编辑、保存草稿

2. **PUBLISHED** - 已发布状态
   - 行为：可见，不可购买，可编辑
   - 可见性：PUBLIC/PUBLIC
   - 用途：商品发布但未正式上架

3. **ON_SHELF** - 已上架状态
   - 行为：可见，可购买，正式销售
   - 可见性：PUBLIC/PUBLIC
   - 用途：正式销售状态

4. **OFF_SHELF** - 已下架状态
   - 行为：不可见，不可购买
   - 可见性：PRIVATE/HIDDEN
   - 用途：商品下架

### 状态转换规则
```
DRAFT ──publish──> PUBLISHED ──shelf──> ON_SHELF ──unshelf──> OFF_SHELF
  ↑                    │                    │                    │
  │                    │                    │                    │
  │                    │                    │                    │
  └──draft─────────────┴──draft─────────────┴──draft──────────────┘
```

### API接口
- `POST /api/user/commodity/{id}/publish` - 发布商品 (DRAFT → PUBLISHED)
- `POST /api/user/commodity/{id}/shelf` - 上架商品 (PUBLISHED → ON_SHELF)
- `POST /api/user/commodity/{id}/unshelf` - 下架商品 (ON_SHELF → OFF_SHELF)
- `POST /api/user/commodity/{id}/republish` - 重新上架 (OFF_SHELF → ON_SHELF)
- `POST /api/user/commodity/{id}/draft` - 设为草稿 (任意状态 → DRAFT)

## 影响分析

### 正面影响
1. **简化状态管理**: 减少了状态数量，降低了系统复杂度
2. **提高用户体验**: 减少了用户困惑，状态转换更清晰
3. **降低维护成本**: 减少了代码量和测试用例
4. **提高系统稳定性**: 减少了状态转换的边界情况

### 注意事项
1. **数据迁移**: 需要执行数据库脚本将SOLD_OUT状态商品迁移到DRAFT状态
2. **前端缓存**: 需要清理前端缓存，确保状态显示正确
3. **API兼容性**: 移除了售罄相关接口，需要更新前端调用
4. **文档更新**: 需要更新相关技术文档和用户手册

## 部署建议

### 1. 数据库更新
```sql
-- 执行数据库脚本
source njumarket/src/main/resources/database/remove_sold_out_status.sql
```

### 2. 应用部署
1. 停止应用服务
2. 部署新版本代码
3. 执行数据库脚本
4. 启动应用服务
5. 验证功能正常

### 3. 验证清单
- [ ] 商品状态显示正确
- [ ] 状态转换功能正常
- [ ] 前端页面无售罄相关元素
- [ ] API接口响应正确
- [ ] 数据库状态一致

## 回滚方案

如果需要回滚到包含SOLD_OUT状态的版本：

1. **代码回滚**: 恢复到之前的代码版本
2. **数据库回滚**: 执行以下SQL恢复SOLD_OUT状态
```sql
-- 将DRAFT状态的商品恢复为SOLD_OUT（需要根据业务逻辑判断）
UPDATE commodities SET commodity_status = 'SOLD_OUT' 
WHERE commodity_status = 'DRAFT' AND stock = 0;
```

## 总结

本次更新成功移除了项目中的"已售罄"状态，简化了商品状态管理体系。新的四状态体系更加清晰和易于理解，有助于提高系统的可维护性和用户体验。建议在测试环境充分验证后再部署到生产环境。
