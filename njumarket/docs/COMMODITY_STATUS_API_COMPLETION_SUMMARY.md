# 商品状态管理API补全总结

## 问题分析

### 原有API分析
项目中原有的商品状态管理API：
- `POST /api/user/commodity/publish` - 发布商品（需要完整商品信息）
- `POST /api/user/commodity/{commodityId}/remove` - 下架商品
- `POST /api/user/commodity/{commodityId}/republish` - 重新上架商品

### 缺失的API
用户需要的单纯状态管理API：
- `POST /api/user/commodity/{commodityId}/publish` - 单纯发布商品（只改变状态）
- `POST /api/user/commodity/{commodityId}/activate` - 激活商品

## 解决方案

### 1. 新增商品状态
在`Commodity`实体中扩展了商品状态：
```java
// 原有状态：DRAFT, PUBLISHED, SOLD_OUT, REMOVED
// 新增状态：SUSPENDED
private String commodityStatus; // DRAFT, PUBLISHED, SOLD_OUT, REMOVED, SUSPENDED
```

### 2. 新增API列表

#### 2.1 发布商品（单纯改变状态）
- **URL**: `POST /api/user/commodity/{commodityId}/publish`
- **功能**: 将草稿状态的商品发布为已发布状态
- **前置条件**: 商品状态必须为`DRAFT`，库存大于0
- **操作**: 状态改为`PUBLISHED`，可见性设为`PUBLIC`

#### 2.2 激活商品
- **URL**: `POST /api/user/commodity/{commodityId}/activate`
- **功能**: 将暂停状态的商品重新激活
- **前置条件**: 商品状态必须为`SUSPENDED`，库存大于0
- **操作**: 状态改为`PUBLISHED`，可见性设为`PUBLIC`

#### 2.3 设为草稿
- **URL**: `POST /api/user/commodity/{commodityId}/draft`
- **功能**: 将已发布或暂停的商品设为草稿状态
- **前置条件**: 商品状态为`PUBLISHED`或`SUSPENDED`
- **操作**: 状态改为`DRAFT`，可见性设为`PRIVATE`/`HIDDEN`

#### 2.4 设为售罄
- **URL**: `POST /api/user/commodity/{commodityId}/sold-out`
- **功能**: 将已发布的商品设为售罄状态
- **前置条件**: 商品状态必须为`PUBLISHED`
- **操作**: 状态改为`SOLD_OUT`，库存设为0

#### 2.5 暂停商品
- **URL**: `POST /api/user/commodity/{commodityId}/suspend`
- **功能**: 将已发布的商品暂停
- **前置条件**: 商品状态必须为`PUBLISHED`
- **操作**: 状态改为`SUSPENDED`，可见性设为`PRIVATE`/`HIDDEN`

## 完整的状态流转图

```
DRAFT ──publish──> PUBLISHED ──suspend──> SUSPENDED
  ↑                    │                    │
  │                    │                    │
  │                    │                    │
  │                    ▼                    │
  │                SOLD_OUT                 │
  │                    │                    │
  │                    │                    │
  └──draft─────────────┴──activate──────────┘
```

### 状态说明
- **DRAFT**: 草稿状态，不可见
- **PUBLISHED**: 已发布，可见可购买
- **SOLD_OUT**: 售罄，可见但不可购买
- **REMOVED**: 已下架，不可见
- **SUSPENDED**: 暂停，不可见（新增）

## API详细说明

### 1. 发布商品状态
```http
POST /api/user/commodity/{commodityId}/publish
Authorization: Bearer <token>
```

**响应示例**:
```json
{
  "success": true,
  "errorMsg": null,
  "data": null,
  "total": null
}
```

**业务逻辑**:
- 检查用户登录状态
- 验证商品所有权
- 检查商品状态（必须为DRAFT）
- 检查库存（必须大于0）
- 更新状态为PUBLISHED
- 设置可见性为PUBLIC
- 更新发布时间

### 2. 激活商品
```http
POST /api/user/commodity/{commodityId}/activate
Authorization: Bearer <token>
```

**业务逻辑**:
- 检查用户登录状态
- 验证商品所有权
- 检查商品状态（必须为SUSPENDED）
- 检查库存（必须大于0）
- 更新状态为PUBLISHED
- 设置可见性为PUBLIC

### 3. 设为草稿
```http
POST /api/user/commodity/{commodityId}/draft
Authorization: Bearer <token>
```

**业务逻辑**:
- 检查用户登录状态
- 验证商品所有权
- 检查商品状态（必须为PUBLISHED或SUSPENDED）
- 更新状态为DRAFT
- 设置可见性为PRIVATE/HIDDEN

### 4. 设为售罄
```http
POST /api/user/commodity/{commodityId}/sold-out
Authorization: Bearer <token>
```

**业务逻辑**:
- 检查用户登录状态
- 验证商品所有权
- 检查商品状态（必须为PUBLISHED）
- 更新状态为SOLD_OUT
- 设置库存为0

### 5. 暂停商品
```http
POST /api/user/commodity/{commodityId}/suspend
Authorization: Bearer <token>
```

**业务逻辑**:
- 检查用户登录状态
- 验证商品所有权
- 检查商品状态（必须为PUBLISHED）
- 更新状态为SUSPENDED
- 设置可见性为PRIVATE/HIDDEN

## 权限控制

### 1. 登录检查
所有API都需要用户登录，通过`UserHolder.getUser()`获取当前用户。

### 2. 所有权验证
只有商品的所有者（sellerId）才能操作商品状态。

### 3. 状态验证
每个API都有严格的状态前置条件检查，确保状态转换的合法性。

## 错误处理

### 1. 常见错误
- **用户未登录**: "用户未登录"
- **商品不存在**: "商品不存在"
- **无权限操作**: "无权限操作此商品"
- **状态不匹配**: "只有XX状态的商品可以XX"
- **库存不足**: "商品库存不足，无法XX"

### 2. 异常处理
所有方法都有完整的try-catch异常处理，确保系统稳定性。

## 前端对接建议

### 1. 状态管理界面
- 提供商品状态切换按钮
- 根据当前状态显示可用的操作
- 提供状态说明和操作确认

### 2. 操作流程
- **草稿 → 发布**: 检查库存，确认发布
- **发布 → 暂停**: 确认暂停原因
- **暂停 → 激活**: 确认重新激活
- **发布 → 售罄**: 确认售罄状态
- **发布/暂停 → 草稿**: 确认设为草稿

### 3. 状态显示
- 使用不同颜色标识商品状态
- 提供状态说明文字
- 显示状态变更时间

## 完整的API列表

### 商品状态管理API
```
POST /api/user/commodity/{commodityId}/publish     # 发布商品
POST /api/user/commodity/{commodityId}/activate    # 激活商品
POST /api/user/commodity/{commodityId}/draft       # 设为草稿
POST /api/user/commodity/{commodityId}/sold-out    # 设为售罄
POST /api/user/commodity/{commodityId}/suspend     # 暂停商品
POST /api/user/commodity/{commodityId}/remove      # 下架商品
POST /api/user/commodity/{commodityId}/republish    # 重新上架
```

### 其他商品管理API
```
GET  /api/user/commodity/{commodityId}             # 获取商品详情
PUT  /api/user/commodity/{commodityId}             # 更新商品信息
DELETE /api/user/commodity/{commodityId}           # 删除商品
POST /api/user/commodity/publish                   # 发布新商品
GET  /api/user/commodity/my                        # 获取我的商品列表
```

## 总结

通过补全商品状态管理API，现在提供了完整的商品状态控制功能：

✅ **状态完整**: 支持DRAFT、PUBLISHED、SOLD_OUT、REMOVED、SUSPENDED五种状态
✅ **API齐全**: 提供所有状态转换的API接口
✅ **权限控制**: 严格的登录和所有权验证
✅ **业务逻辑**: 合理的状态转换规则和前置条件
✅ **错误处理**: 完善的异常处理和错误信息
✅ **前端友好**: 便于前端实现商品状态管理界面

现在用户可以方便地管理商品状态，无需修改商品的其他信息，只需要调用相应的状态管理API即可。
