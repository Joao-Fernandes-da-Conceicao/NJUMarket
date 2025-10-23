# 商品状态进一步简化总结

## 问题分析

用户要求进一步简化商品状态逻辑：
- **合并草稿和已发布状态** - 减少状态数量
- **只有已上架状态可购买** - 明确购买条件
- **简化为三种状态** - 从四种状态进一步简化

## 新的三种状态设计

### 1. **DRAFT** - 草稿状态
- **行为**: 不可见，不可购买，可编辑
- **可见性**: PRIVATE/HIDDEN
- **用途**: 商品编辑、保存草稿

### 2. **ON_SHELF** - 已上架状态
- **行为**: 可见，可购买，正式销售
- **可见性**: PUBLIC/PUBLIC
- **用途**: 正式销售状态（唯一可购买状态）

### 3. **SOLD_OUT** - 已售完状态
- **行为**: 可见，不可购买
- **可见性**: PUBLIC/PUBLIC
- **用途**: 库存为0，显示售完

## 状态流转图

```
DRAFT ──shelf──> ON_SHELF ──sold-out──> SOLD_OUT
  ↑                    │                    │
  │                    │                    │
  │                    │                    │
  └──unshelf───────────┴──draft─────────────┘
```

## 状态转换规则

### 1. 上架商品 (DRAFT → ON_SHELF)
- **API**: `POST /api/user/commodity/{commodityId}/shelf`
- **前置条件**: 商品状态为DRAFT，库存>0
- **操作**: 状态改为ON_SHELF，可见性设为PUBLIC

### 2. 下架商品 (ON_SHELF → DRAFT)
- **API**: `POST /api/user/commodity/{commodityId}/unshelf`
- **前置条件**: 商品状态为ON_SHELF
- **操作**: 状态改为DRAFT，可见性设为PRIVATE/HIDDEN

### 3. 设为草稿 (ON_SHELF/SOLD_OUT → DRAFT)
- **API**: `POST /api/user/commodity/{commodityId}/draft`
- **前置条件**: 商品状态为ON_SHELF或SOLD_OUT
- **操作**: 状态改为DRAFT，可见性设为PRIVATE/HIDDEN

### 4. 设为售罄 (ON_SHELF → SOLD_OUT)
- **API**: `POST /api/user/commodity/{commodityId}/sold-out`
- **前置条件**: 商品状态为ON_SHELF
- **操作**: 状态改为SOLD_OUT，库存设为0

### 5. 重新上架 (DRAFT → ON_SHELF)
- **API**: `POST /api/user/commodity/{commodityId}/republish`
- **前置条件**: 商品状态为DRAFT，库存>0
- **操作**: 状态改为ON_SHELF，可见性设为PUBLIC

## 后端重构内容

### 1. 实体类更新
```java
// 更新前
private String commodityStatus; // DRAFT, PUBLISHED, ON_SHELF, SOLD_OUT

// 更新后  
private String commodityStatus; // DRAFT, ON_SHELF, SOLD_OUT
```

### 2. Controller API更新
```java
// 新的API
@PostMapping("/{commodityId}/shelf")      // 上架商品 (DRAFT → ON_SHELF)
@PostMapping("/{commodityId}/unshelf")    // 下架商品 (ON_SHELF → DRAFT)
@PostMapping("/{commodityId}/draft")      // 设为草稿 (→ DRAFT)
@PostMapping("/{commodityId}/sold-out")   // 设为售罄 (ON_SHELF → SOLD_OUT)
@PostMapping("/{commodityId}/republish")  // 重新上架 (DRAFT → ON_SHELF)

// 移除的API
@PostMapping("/{commodityId}/publish")    // 发布商品（已合并到shelf）
@PostMapping("/{commodityId}/activate")  // 激活商品（不合理）
```

### 3. Service接口更新
```java
// 新的方法
Result shelfCommodity(String commodityId);     // 上架商品
Result unshelfCommodity(String commodityId);   // 下架商品
Result draftCommodity(String commodityId);     // 设为草稿
Result soldOutCommodity(String commodityId);   // 设为售罄

// 移除的方法
Result publishCommodityStatus(String commodityId); // 发布商品
Result activateCommodity(String commodityId);      // 激活商品
Result suspendCommodity(String commodityId);       // 暂停商品
```

### 4. 业务逻辑重构

#### 4.1 shelfCommodity方法
- **功能**: 草稿 → 已上架
- **检查**: 状态为DRAFT，库存>0
- **操作**: 设为ON_SHELF，可见性PUBLIC

#### 4.2 unshelfCommodity方法
- **功能**: 已上架 → 草稿
- **检查**: 状态为ON_SHELF
- **操作**: 设为DRAFT，可见性PRIVATE/HIDDEN

#### 4.3 draftCommodity方法
- **功能**: 已上架/已售完 → 草稿
- **检查**: 状态为ON_SHELF或SOLD_OUT
- **操作**: 设为DRAFT，可见性PRIVATE/HIDDEN

#### 4.4 soldOutCommodity方法
- **功能**: 已上架 → 已售完
- **检查**: 状态为ON_SHELF
- **操作**: 设为SOLD_OUT，库存为0

#### 4.5 republishCommodity方法
- **功能**: 草稿 → 已上架
- **检查**: 状态为DRAFT，库存>0
- **操作**: 设为ON_SHELF，可见性PUBLIC

#### 4.6 removeCommodity方法
- **功能**: 已上架 → 草稿
- **检查**: 状态为ON_SHELF
- **操作**: 设为DRAFT，可见性PRIVATE/HIDDEN

### 5. 实体方法更新
```java
// unpublish方法更新
public Boolean unpublish() {
    this.commodityStatus = "DRAFT";  // 改为DRAFT而不是PUBLISHED
    this.sellerVisibility = "PRIVATE";
    this.buyerVisibility = "HIDDEN";
    return true;
}
```

## 前端重构内容

### 1. 状态筛选标签页更新
```vue
<!-- 更新前 -->
<el-tab-pane label="全部" name="all"></el-tab-pane>
<el-tab-pane label="在售" name="ACTIVE"></el-tab-pane>
<el-tab-pane label="已下架" name="INACTIVE"></el-tab-pane>
<el-tab-pane label="已售完" name="SOLD_OUT"></el-tab-pane>

<!-- 更新后 -->
<el-tab-pane label="全部" name="all"></el-tab-pane>
<el-tab-pane label="草稿" name="DRAFT"></el-tab-pane>
<el-tab-pane label="已上架" name="ON_SHELF"></el-tab-pane>
<el-tab-pane label="已售完" name="SOLD_OUT"></el-tab-pane>
```

### 2. 操作按钮更新
```vue
<!-- 草稿商品：上架按钮 -->
<el-button v-if="commodity.commodityStatus === 'DRAFT'" type="primary" @click="handleShelf">
  上架
</el-button>

<!-- 已上架商品：下架按钮 -->
<el-button v-if="commodity.commodityStatus === 'ON_SHELF'" @click="handleUnshelf">
  下架
</el-button>

<!-- 已售完商品：设为草稿按钮 -->
<el-button v-if="commodity.commodityStatus === 'SOLD_OUT'" @click="handleDraft">
  设为草稿
</el-button>

<!-- 已上架商品：设为售罄按钮 -->
<el-button v-if="commodity.commodityStatus === 'ON_SHELF'" type="warning" @click="handleSoldOut">
  设为售罄
</el-button>
```

### 3. JavaScript方法更新
```javascript
// 新的方法
const handleShelf = async (commodityId) => {
  const response = await commodityAPI.shelf(commodityId)
  // 处理响应
}

const handleUnshelf = async (commodityId) => {
  const response = await commodityAPI.unshelf(commodityId)
  // 处理响应
}

const handleDraft = async (commodityId) => {
  const response = await commodityAPI.draft(commodityId)
  // 处理响应
}

const handleSoldOut = async (commodityId) => {
  const response = await commodityAPI.soldOut(commodityId)
  // 处理响应
}

// 移除的方法
// handlePublishDraft, handleActivate, handleRemove, handleRepublish
```

### 4. 状态映射更新
```javascript
// 状态类型映射
const getStatusType = (status) => {
  const statusMap = {
    'DRAFT': 'warning',      // 草稿 - 警告色
    'ON_SHELF': 'success',   // 已上架 - 成功色
    'SOLD_OUT': 'info'       // 已售完 - 信息色
  }
  return statusMap[status] || 'info'
}

// 状态文本映射
const getStatusText = (status) => {
  const statusMap = {
    'DRAFT': '草稿',
    'ON_SHELF': '已上架',
    'SOLD_OUT': '已售完'
  }
  return statusMap[status] || status
}
```

### 5. API接口更新
```javascript
// 新的API方法
export const commodityAPI = {
  // 上架商品
  shelf: (id) => api.post(`/user/commodity/${id}/shelf`),
  
  // 下架商品
  unshelf: (id) => api.post(`/user/commodity/${id}/unshelf`),
  
  // 设为草稿
  draft: (id) => api.post(`/user/commodity/${id}/draft`),
  
  // 设为售罄
  soldOut: (id) => api.post(`/user/commodity/${id}/sold-out`),
  
  // 重新上架
  republish: (id) => api.post(`/user/commodity/${id}/republish`)
}

// 移除的API方法
// publishDraft, activate
```

## 完整的API列表

### 商品状态管理API
```
POST /api/user/commodity/{commodityId}/shelf       # 上架商品 (DRAFT → ON_SHELF)
POST /api/user/commodity/{commodityId}/unshelf     # 下架商品 (ON_SHELF → DRAFT)
POST /api/user/commodity/{commodityId}/draft       # 设为草稿 (→ DRAFT)
POST /api/user/commodity/{commodityId}/sold-out    # 设为售罄 (ON_SHELF → SOLD_OUT)
POST /api/user/commodity/{commodityId}/republish    # 重新上架 (DRAFT → ON_SHELF)
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
- **DRAFT**: 橙色，显示"草稿"
- **ON_SHELF**: 绿色，显示"已上架"
- **SOLD_OUT**: 蓝色，显示"已售完"

### 2. 操作按钮
根据当前状态显示可用操作：

#### 草稿状态 (DRAFT)
- ✅ 上架商品
- ✅ 编辑商品
- ✅ 删除商品

#### 已上架状态 (ON_SHELF)
- ✅ 下架商品
- ✅ 设为售罄
- ✅ 设为草稿
- ✅ 编辑商品

#### 已售完状态 (SOLD_OUT)
- ✅ 设为草稿
- ✅ 编辑商品

### 3. 购买逻辑
- **只有ON_SHELF状态的商品可以购买**
- 其他状态的商品显示"不可购买"或隐藏购买按钮
- 前端需要检查商品状态再显示购买按钮

### 4. 状态流转界面
提供清晰的状态流转界面，显示：
- 当前状态
- 可用的状态转换
- 状态说明
- 操作确认

## 优势

### 1. 逻辑更清晰
- 状态数量从4个减少到3个
- 状态转换规则更简单
- 每个状态的行为定义更明确

### 2. 用户体验更好
- 操作流程更直观
- 状态管理更简单
- 购买条件更明确

### 3. 业务逻辑更合理
- 符合实际业务流程
- 状态定义符合业务需求
- 操作权限控制合理

### 4. 维护性更好
- 代码逻辑简化
- 状态转换规则统一
- 易于理解和维护

## 总结

通过进一步简化商品状态逻辑，解决了原有设计中的问题：

✅ **状态简化**: 从4个状态简化为3个核心状态
✅ **逻辑清晰**: 状态转换规则简单明了
✅ **用户体验**: 操作流程更直观，购买条件更明确
✅ **业务合理**: 符合实际业务流程和用户需求
✅ **维护性好**: 代码逻辑简化，易于理解和维护

现在商品状态管理更加合理和直观：
- **草稿状态**: 不可见，不可购买，可编辑
- **已上架状态**: 可见，可购买，正式销售（唯一可购买状态）
- **已售完状态**: 可见，不可购买

用户可以方便地管理商品状态，从草稿到上架到售完的完整流程更加合理和直观！
