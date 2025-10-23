# 商品状态逻辑重构总结

## 问题分析

### 原有问题
1. **状态设计不合理**: SUSPENDED状态增加了不必要的复杂性
2. **行为不一致**: REMOVED和DRAFT都是不可见状态但处理方式不同
3. **转换逻辑复杂**: 过多的状态导致状态转换规则复杂
4. **用户体验差**: 对未上架商品上架会显示"只有暂停状态的商品才能激活"的错误信息

### 原有状态
- **DRAFT**: 草稿状态
- **PUBLISHED**: 已发布状态  
- **SOLD_OUT**: 已售完状态
- **REMOVED**: 已下架状态
- **SUSPENDED**: 暂停状态（不合理）

## 解决方案

### 1. 新的状态设计
简化为四个核心状态，从行为角度考虑：

1. **DRAFT** - 草稿状态
   - **行为**: 不可见，可编辑
   - **可见性**: PRIVATE/HIDDEN
   - **用途**: 商品编辑、保存草稿

2. **PUBLISHED** - 已发布状态
   - **行为**: 可见，可购买
   - **可见性**: PUBLIC/PUBLIC
   - **用途**: 商品发布但未正式上架

3. **ON_SHELF** - 已上架状态
   - **行为**: 可见，可购买，正式销售
   - **可见性**: PUBLIC/PUBLIC
   - **用途**: 正式销售状态

4. **SOLD_OUT** - 已售完状态
   - **行为**: 可见，不可购买
   - **可见性**: PUBLIC/PUBLIC
   - **用途**: 库存为0，显示售完

### 2. 状态流转图

```
DRAFT ──publish──> PUBLISHED ──shelf──> ON_SHELF ──sold-out──> SOLD_OUT
  ↑                    │                    │                    │
  │                    │                    │                    │
  │                    │                    │                    │
  │                    │                    │                    │
  └──draft─────────────┴──unshelf──────────┴──draft──────────────┘
```

### 3. 状态转换规则

#### 3.1 发布商品 (DRAFT → PUBLISHED)
- **API**: `POST /api/user/commodity/{commodityId}/publish`
- **前置条件**: 商品状态为DRAFT，库存>0
- **操作**: 状态改为PUBLISHED，可见性设为PUBLIC

#### 3.2 上架商品 (PUBLISHED → ON_SHELF)
- **API**: `POST /api/user/commodity/{commodityId}/shelf`
- **前置条件**: 商品状态为PUBLISHED，库存>0
- **操作**: 状态改为ON_SHELF，可见性设为PUBLIC

#### 3.3 下架商品 (ON_SHELF → PUBLISHED)
- **API**: `POST /api/user/commodity/{commodityId}/unshelf`
- **前置条件**: 商品状态为ON_SHELF
- **操作**: 状态改为PUBLISHED，可见性设为PUBLIC

#### 3.4 设为草稿 (PUBLISHED/ON_SHELF/SOLD_OUT → DRAFT)
- **API**: `POST /api/user/commodity/{commodityId}/draft`
- **前置条件**: 商品状态为PUBLISHED、ON_SHELF或SOLD_OUT
- **操作**: 状态改为DRAFT，可见性设为PRIVATE/HIDDEN

#### 3.5 设为售罄 (ON_SHELF → SOLD_OUT)
- **API**: `POST /api/user/commodity/{commodityId}/sold-out`
- **前置条件**: 商品状态为ON_SHELF
- **操作**: 状态改为SOLD_OUT，库存设为0

#### 3.6 重新上架 (PUBLISHED → ON_SHELF)
- **API**: `POST /api/user/commodity/{commodityId}/republish`
- **前置条件**: 商品状态为PUBLISHED，库存>0
- **操作**: 状态改为ON_SHELF，可见性设为PUBLIC

## 重构内容

### 1. 实体类更新
```java
// 更新前
private String commodityStatus; // DRAFT, PUBLISHED, SOLD_OUT, REMOVED, SUSPENDED

// 更新后  
private String commodityStatus; // DRAFT, PUBLISHED, ON_SHELF, SOLD_OUT
```

### 2. Controller API更新
```java
// 新增API
@PostMapping("/{commodityId}/shelf")      // 上架商品
@PostMapping("/{commodityId}/unshelf")    // 下架商品

// 移除API
@PostMapping("/{commodityId}/activate")  // 激活商品（不合理）
@PostMapping("/{commodityId}/suspend")    // 暂停商品（不合理）
```

### 3. Service接口更新
```java
// 新增方法
Result shelfCommodity(String commodityId);     // 上架商品
Result unshelfCommodity(String commodityId);   // 下架商品

// 移除方法
Result activateCommodity(String commodityId);  // 激活商品
Result suspendCommodity(String commodityId);  // 暂停商品
```

### 4. 业务逻辑重构

#### 4.1 publishCommodityStatus方法
- **功能**: 草稿 → 已发布
- **检查**: 状态为DRAFT，库存>0
- **操作**: 设为PUBLISHED，可见性PUBLIC

#### 4.2 shelfCommodity方法
- **功能**: 已发布 → 已上架
- **检查**: 状态为PUBLISHED，库存>0
- **操作**: 设为ON_SHELF，可见性PUBLIC

#### 4.3 unshelfCommodity方法
- **功能**: 已上架 → 已发布
- **检查**: 状态为ON_SHELF
- **操作**: 设为PUBLISHED，可见性PUBLIC

#### 4.4 draftCommodity方法
- **功能**: 已发布/已上架/已售完 → 草稿
- **检查**: 状态为PUBLISHED、ON_SHELF或SOLD_OUT
- **操作**: 设为DRAFT，可见性PRIVATE/HIDDEN

#### 4.5 soldOutCommodity方法
- **功能**: 已上架 → 已售完
- **检查**: 状态为ON_SHELF
- **操作**: 设为SOLD_OUT，库存为0

#### 4.6 republishCommodity方法
- **功能**: 已发布 → 已上架
- **检查**: 状态为PUBLISHED，库存>0
- **操作**: 设为ON_SHELF，可见性PUBLIC

#### 4.7 removeCommodity方法
- **功能**: 已上架 → 已发布
- **检查**: 状态为ON_SHELF
- **操作**: 设为PUBLISHED，可见性PUBLIC

### 5. 实体方法更新
```java
// unpublish方法更新
public Boolean unpublish() {
    this.commodityStatus = "PUBLISHED";  // 改为PUBLISHED而不是REMOVED
    this.sellerVisibility = "PUBLIC";
    this.buyerVisibility = "PUBLIC";
    return true;
}
```

## 完整的API列表

### 商品状态管理API
```
POST /api/user/commodity/{commodityId}/publish     # 发布商品 (DRAFT → PUBLISHED)
POST /api/user/commodity/{commodityId}/shelf       # 上架商品 (PUBLISHED → ON_SHELF)
POST /api/user/commodity/{commodityId}/unshelf     # 下架商品 (ON_SHELF → PUBLISHED)
POST /api/user/commodity/{commodityId}/draft       # 设为草稿 (→ DRAFT)
POST /api/user/commodity/{commodityId}/sold-out    # 设为售罄 (ON_SHELF → SOLD_OUT)
POST /api/user/commodity/{commodityId}/republish    # 重新上架 (PUBLISHED → ON_SHELF)
```

### 其他商品管理API
```
GET  /api/user/commodity/{commodityId}             # 获取商品详情
PUT  /api/user/commodity/{commodityId}             # 更新商品信息
DELETE /api/user/commodity/{commodityId}           # 删除商品
POST /api/user/commodity/publish                   # 发布新商品
GET  /api/user/commodity/my                        # 获取我的商品列表
```

## 前端对接建议

### 1. 状态显示
- **DRAFT**: 灰色，显示"草稿"
- **PUBLISHED**: 蓝色，显示"已发布"
- **ON_SHELF**: 绿色，显示"已上架"
- **SOLD_OUT**: 红色，显示"已售完"

### 2. 操作按钮
根据当前状态显示可用操作：

#### 草稿状态 (DRAFT)
- ✅ 发布商品
- ✅ 编辑商品
- ✅ 删除商品

#### 已发布状态 (PUBLISHED)
- ✅ 上架商品
- ✅ 设为草稿
- ✅ 编辑商品

#### 已上架状态 (ON_SHELF)
- ✅ 下架商品
- ✅ 设为售罄
- ✅ 设为草稿
- ✅ 编辑商品

#### 已售完状态 (SOLD_OUT)
- ✅ 设为草稿
- ✅ 编辑商品

### 3. 状态流转界面
提供清晰的状态流转界面，显示：
- 当前状态
- 可用的状态转换
- 状态说明
- 操作确认

## 优势

### 1. 逻辑清晰
- 状态数量减少，逻辑更清晰
- 状态转换规则简单明了
- 每个状态的行为定义明确

### 2. 用户体验好
- 操作流程更直观
- 错误信息更准确
- 状态管理更简单

### 3. 维护性好
- 代码逻辑简化
- 状态转换规则统一
- 易于理解和维护

### 4. 业务合理
- 符合实际业务流程
- 状态定义符合业务需求
- 操作权限控制合理

## 总结

通过重构商品状态逻辑，解决了原有设计中的问题：

✅ **状态简化**: 从5个状态简化为4个核心状态
✅ **逻辑清晰**: 状态转换规则简单明了
✅ **用户体验**: 操作流程更直观，错误信息更准确
✅ **业务合理**: 符合实际业务流程和用户需求
✅ **维护性好**: 代码逻辑简化，易于理解和维护

现在用户可以方便地管理商品状态，从草稿到发布到上架到售完的完整流程更加合理和直观。
