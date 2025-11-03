# 南大集市 NJUMarket v1.1.2 项目文档

## 📋 目录
- [版本概述](#版本概述)
- [核心功能更新](#核心功能更新)
- [订单通知系统](#订单通知系统)
- [智能订单更新](#智能订单更新)
- [软删除与可见性机制](#软删除与可见性机制)
- [技术细节](#技术细节)
- [已知问题与限制](#已知问题与限制)

---

## 版本概述

### 版本信息
- **版本**: v1.1.2
- **发布时间**: 2025-01-XX
- **基于版本**: v1.1.1
- **状态**: 已发布，订单通知功能已完成

### 版本定位
v1.1.2 版本专注于**订单实时通知**和**用户体验优化**，通过WebSocket实现订单变化的实时推送，智能更新订单列表，自动处理软删除恢复可见性，显著提升了买卖双方对订单状态的实时感知能力。

### 主要成就
- ✅ **订单变化实时通知**：WebSocket推送订单状态变化，买卖双方实时感知
- ✅ **智能角标提醒**：根据角色和目标页面智能显示角标
- ✅ **智能订单更新**：在订单页面时，无需刷新即可实时更新订单状态
- ✅ **完整订单信息推送**：订单创建和可见性恢复时推送完整OrderDTO
- ✅ **软删除自动恢复**：退款流程中自动恢复被软删除订单的可见性
- ✅ **代码复用优化**：统一的通知处理机制，减少重复代码

---

## 核心功能更新

### 1. 订单变化通知系统

#### 1.1 WebSocket通知机制

**实现位置**：
- 后端：`OrderServiceImpl.pushOrderChangeNotification()` / `pushOrderChangeNotificationWithDTO()`
- 前端：`stores/order.js` - `handleOrderChangeNotification()`

**功能说明**：
- 所有订单状态变化都会通过WebSocket实时推送给相关用户
- 支持8种操作类型的通知（订单创建、支付、发货、完成、取消、退款申请、退款处理等）
- 根据操作类型和用户角色，智能判断通知目标（卖家或买家）

**通知结构**：
```json
{
  "type": "ORDER_CHANGE",
  "orderId": "order123",
  "changeType": "ORDER_CREATED",
  "orderStatus": "CREATED",
  "targetRole": "SELLER",
  "timestamp": "2024-01-01T12:00:00",
  "order": { /* 完整OrderDTO，仅在ORDER_CREATED和ORDER_VISIBILITY_RESTORED时包含 */ }
}
```

**技术实现**：
```java
// 后端：推送订单变化通知
private void pushOrderChangeNotificationWithDTO(String userId, String orderId, 
    String changeType, String orderStatus, String targetRole, OrderDTO orderDTO) {
    Map<String, Object> notification = new HashMap<>();
    notification.put("type", "ORDER_CHANGE");
    notification.put("orderId", orderId);
    notification.put("changeType", changeType);
    notification.put("orderStatus", orderStatus);
    notification.put("targetRole", targetRole);
    notification.put("timestamp", LocalDateTime.now().toString());
    
    // 如果是订单创建或可见性恢复，发送完整OrderDTO
    if (orderDTO != null && ("ORDER_CREATED".equals(changeType) || 
                             "ORDER_VISIBILITY_RESTORED".equals(changeType))) {
        notification.put("order", orderDTO);
    }
    
    webSocketRetryService.pushWithRetry(userId, notification, "ORDER_CHANGE");
}
```

**关键特性**：
1. **目标角色标识**：通过`targetRole`字段（"SELLER"或"BUYER"）明确通知目标
2. **完整订单推送**：订单创建和可见性恢复时推送完整OrderDTO，前端可直接使用
3. **重试机制**：使用`WebSocketRetryService`确保离线用户也能收到通知
4. **操作类型覆盖**：涵盖所有订单操作类型，确保完整覆盖

#### 1.2 角标提醒机制

**实现位置**：
- 前端：`stores/order.js` - `sellerOrderHasNew` / `buyerOrderHasNew`
- 前端：`components/layout/AppHeader.vue` - 角标显示

**功能说明**：
- 卖家订单变化时，在"卖家订单"入口显示角标
- 买家订单变化时，在"我的订单"入口显示角标
- 仅在用户不在相关页面时显示角标，避免重复提醒
- 进入相关页面后自动清除角标

**技术实现**：
```javascript
// 前端：处理订单变化通知
handleOrderChangeNotification(notification) {
  const { changeType, orderId, orderStatus, targetRole, order: orderDTO } = notification
  const currentPath = window.location.pathname
  
  if (targetRole === 'SELLER') {
    if (currentPath.startsWith('/seller-orders')) {
      // 在卖家订单页面，调用智能更新回调
      if (this.orderUpdateCallbacks.seller) {
        this.orderUpdateCallbacks.seller(orderId, changeType, orderStatus, orderDTO)
      }
    } else {
      // 不在卖家订单页面，显示角标
      this.setSellerOrderHasNew(true)
    }
  } else if (targetRole === 'BUYER') {
    if (currentPath === '/orders' || currentPath.startsWith('/orders/')) {
      // 在买家订单页面，调用智能更新回调
      if (this.orderUpdateCallbacks.buyer) {
        this.orderUpdateCallbacks.buyer(orderId, changeType, orderStatus, orderDTO)
      }
    } else {
      // 不在买家订单页面，显示角标
      this.setBuyerOrderHasNew(true)
    }
  }
}
```

**角标显示位置**：
- **桌面端**：顶部导航栏的"卖家订单"和"我的订单"链接
- **移动端**：侧边菜单中的"卖家订单"和"我的订单"项
- **角标样式**：圆形点状角标（类似消息角标），仅显示/隐藏，不显示数字

---

## 订单通知系统

### 2. 所有订单操作类型

#### 2.1 操作类型清单

| 操作类型 | changeType | 目标角色 | 发送完整OrderDTO | 涉及可见性恢复 | 说明 |
|---------|------------|---------|----------------|--------------|------|
| 订单创建 | ORDER_CREATED | SELLER | ✅ 是 | ❌ 否 | 买家创建订单，通知卖家 |
| 订单支付 | ORDER_PAID | SELLER | ❌ 否 | ❌ 否 | 买家支付订单，通知卖家 |
| 订单发货 | ORDER_SHIPPED | BUYER | ❌ 否 | ❌ 否 | 卖家发货，通知买家（前端会异步获取详情） |
| 订单完成 | ORDER_COMPLETED | SELLER | ❌ 否 | ❌ 否 | 买家确认收货，通知卖家 |
| 订单取消 | ORDER_CANCELLED | SELLER/BUYER | ❌ 否 | ❌ 否 | 根据取消者判断目标角色 |
| 退款申请 | REFUND_REQUESTED | SELLER | ❌ 否 | ✅ 可能 | 买家申请退款，可能恢复卖家可见性 |
| 退款同意 | REFUND_APPROVED | BUYER | ❌ 否 | ✅ 可能 | 卖家同意退款，可能恢复买家可见性 |
| 退款拒绝 | REFUND_REJECTED | BUYER | ❌ 否 | ✅ 可能 | 卖家拒绝退款，可能恢复买家可见性 |
| 可见性恢复 | ORDER_VISIBILITY_RESTORED | SELLER/BUYER | ✅ 是 | ✅ 是 | 仅在恢复可见性时发送 |

#### 2.2 完整OrderDTO推送场景

**场景1：订单创建**
```java
// OrderServiceImpl.java - createOrder方法
OrderDTO orderDTOForNotification = convertToDTO(order);
pushOrderChangeNotificationWithDTO(order.getSellerId(), order.getOrderId(), 
    "ORDER_CREATED", order.getOrderStatus(), "SELLER", orderDTOForNotification);
```

**场景2：可见性恢复**
```java
// 退款申请时恢复卖家可见性
if (sellerVisibilityRestored) {
    OrderDTO orderDTOForRestored = convertToDTO(order);
    pushOrderChangeNotificationWithDTO(order.getSellerId(), order.getOrderId(), 
        "ORDER_VISIBILITY_RESTORED", order.getOrderStatus(), "SELLER", orderDTOForRestored);
}

// 退款处理时恢复买家可见性
if (buyerVisibilityRestored) {
    OrderDTO orderDTOForRestored = convertToDTO(order);
    pushOrderChangeNotificationWithDTO(order.getBuyerId(), order.getOrderId(), 
        "ORDER_VISIBILITY_RESTORED", order.getOrderStatus(), "BUYER", orderDTOForRestored);
}
```

**推送完整OrderDTO的原因**：
1. **订单创建**：卖家需要立即看到新订单的完整信息，无需额外API请求
2. **可见性恢复**：订单被软删除后恢复，需要完整信息重新显示在列表中

#### 2.3 目标角色判断逻辑

**取消订单的特殊处理**：
```java
// OrderServiceImpl.java - cancelOrder方法
if (order.getBuyerId().equals(currentUser.getUserId())) {
    // 买家取消 -> 通知卖家
    pushOrderChangeNotification(order.getSellerId(), order.getOrderId(), 
        "ORDER_CANCELLED", order.getOrderStatus(), "SELLER");
} else {
    // 卖家取消 -> 通知买家
    pushOrderChangeNotification(order.getBuyerId(), order.getOrderId(), 
        "ORDER_CANCELLED", order.getOrderStatus(), "BUYER");
}
```

**其他操作的目标角色**：
- 卖家相关操作（支付、完成）→ `targetRole = "SELLER"`
- 买家相关操作（发货、退款处理结果）→ `targetRole = "BUYER"`

---

## 智能订单更新

### 3. 页面内实时更新

#### 3.1 智能更新机制

**实现位置**：
- 前端：`views/SellerOrders.vue` - `smartUpdateOrder()`
- 前端：`views/MyOrders.vue` - `smartUpdateOrder()`

**功能说明**：
- 用户在订单页面时，收到WebSocket通知后直接更新对应的订单卡片
- 无需重新加载整个列表，提升用户体验和性能
- 根据`changeType`智能判断需要更新的字段
- 处理订单不在列表中的情况（新订单或不在筛选范围内）

**技术实现**：
```javascript
// 智能更新订单（根据WebSocket通知）
const smartUpdateOrder = async (orderId, changeType, orderStatus, orderDTO = null) => {
  // 如果是可见性恢复，使用完整的orderDTO直接更新或添加
  if (changeType === 'ORDER_VISIBILITY_RESTORED') {
    if (orderDTO) {
      const orderIndex = orders.value.findIndex(o => o.orderId === orderId)
      if (orderIndex !== -1) {
        // 订单在列表中，直接更新
        orders.value[orderIndex] = orderDTO
      } else {
        // 订单不在列表中，检查是否应该添加到当前筛选范围
        if (activeTab.value === 'all' || activeTab.value === orderDTO.orderStatus) {
          orders.value.unshift(orderDTO)
          total.value = (total.value || 0) + 1
        }
      }
    }
    return
  }
  
  // 查找订单在列表中的位置
  const orderIndex = orders.value.findIndex(o => o.orderId === orderId)
  
  if (orderIndex === -1) {
    // 订单不在当前列表中
    if (changeType === 'ORDER_CREATED' && orderDTO) {
      // 新订单创建，如果有完整OrderDTO，直接添加到列表
      if (activeTab.value === 'all' || activeTab.value === orderDTO.orderStatus) {
        orders.value.unshift(orderDTO)
        total.value = (total.value || 0) + 1
      }
    }
    return
  }
  
  // 订单在当前列表中，智能更新
  const order = orders.value[orderIndex]
  
  // 根据changeType更新订单状态
  switch (changeType) {
    case 'ORDER_PAID':
      order.orderStatus = 'PAID'
      break
    case 'ORDER_SHIPPED':
      order.orderStatus = 'SHIPPED'
      // 异步获取详情以更新trackingNumber等字段
      orderAPI.getDetail(orderId).then(response => {
        if (response.success && response.data) {
          const idx = orders.value.findIndex(o => o.orderId === orderId)
          if (idx !== -1) {
            Object.assign(orders.value[idx], response.data)
          }
        }
      })
      break
    case 'ORDER_COMPLETED':
      order.orderStatus = 'COMPLETED'
      break
    // ... 其他状态变化
  }
  
  // 如果订单状态变化后不在当前筛选范围内，从列表中移除
  if (activeTab.value !== 'all' && order.orderStatus !== activeTab.value) {
    orders.value.splice(orderIndex, 1)
    total.value = Math.max(0, total.value - 1)
  } else {
    // 更新响应式
    orders.value[orderIndex] = { ...order }
  }
}
```

**关键特性**：
1. **局部更新**：只更新变化的订单卡片，不刷新整个列表
2. **智能判断**：根据`changeType`判断需要更新的字段
3. **筛选处理**：状态变化后自动处理筛选范围（移除或添加）
4. **异步获取**：需要详细信息时（如`ORDER_SHIPPED`需要trackingNumber），异步获取详情
5. **完整订单处理**：收到完整OrderDTO时，直接替换订单对象

#### 3.2 更新回调注册机制

**实现位置**：
- 前端：`stores/order.js` - `registerOrderUpdateCallback()` / `unregisterOrderUpdateCallback()`

**功能说明**：
- 订单页面注册更新回调，Store在收到通知时调用
- 组件卸载时取消注册，避免内存泄漏
- 支持卖家和买家分别注册回调

**技术实现**：
```javascript
// Order Store
state: () => ({
  orderUpdateCallbacks: {
    buyer: null,
    seller: null
  }
}),

actions: {
  registerOrderUpdateCallback(role, callback) {
    if (role === 'buyer' || role === 'seller') {
      this.orderUpdateCallbacks[role] = callback
    }
  },
  
  unregisterOrderUpdateCallback(role) {
    if (role === 'buyer' || role === 'seller') {
      this.orderUpdateCallbacks[role] = null
    }
  }
}

// 订单页面
onMounted(() => {
  orderStore.registerOrderUpdateCallback('seller', smartUpdateOrder)
  fetchOrders()
})

onUnmounted(() => {
  orderStore.unregisterOrderUpdateCallback('seller')
})
```

---

## 软删除与可见性机制

### 4. 软删除限制规则

#### 4.1 禁止软删除的状态

**禁止软删除的订单状态**（需要处理订单或退款，必须保持可见）：
- ❌ `CREATED`（待支付）- 买家需要支付，卖家需要等待
- ❌ `PAID`（待发货）- 卖家需要发货，买家需要等待
- ❌ `SHIPPED`（待收货）- 买家需要确认收货，卖家需要等待
- ❌ `REFUND_REQUESTED`（退款申请中）- 卖家需要处理退款申请

**实现位置**：
- 后端：`Order.canModifyVisibility()`

**技术实现**：
```java
public Boolean canModifyVisibility() {
    // 禁止删除的状态：待支付、待发货、待收货、退款申请中
    String[] blockedStatuses = {"CREATED", "PAID", "SHIPPED", "REFUND_REQUESTED"};
    for (String status : blockedStatuses) {
        if (status.equals(this.orderStatus)) {
            return false;
        }
    }
    
    // 其他状态允许修改（包括 COMPLETED, CANCELLED, REFUND_APPROVED, REFUND_REJECTED 等）
    return true;
}
```

#### 4.2 允许软删除的状态

**允许软删除的订单状态**：
- ✅ `COMPLETED`（已完成）
- ✅ `CANCELLED`（已取消）
- ✅ `REFUND_APPROVED`（退款完成）
- ✅ `REFUND_REJECTED`（退款被拒绝）- **允许软删除，但如果买家重新申请退款，会自动恢复可见性**
- ✅ 其他所有状态

### 4.3 自动恢复可见性机制

#### 4.3.1 退款申请时恢复卖家可见性

**场景**：
- 买家从`COMPLETED`状态申请退款
- 买家从`REFUND_REJECTED`状态重新申请退款
- 如果卖家之前软删除了订单，系统会自动恢复可见性

**技术实现**：
```java
// OrderServiceImpl.java - requestRefund方法
boolean sellerVisibilityRestored = "HIDDEN".equals(order.getSellerVisibility());
order.setSellerVisibility("PUBLIC");

pushOrderChangeNotification(order.getSellerId(), order.getOrderId(), 
    "REFUND_REQUESTED", order.getOrderStatus(), "SELLER");

// 如果是恢复可见性，推送完整的OrderDTO
if (sellerVisibilityRestored) {
    OrderDTO orderDTOForRestored = convertToDTO(order);
    pushOrderChangeNotificationWithDTO(order.getSellerId(), order.getOrderId(), 
        "ORDER_VISIBILITY_RESTORED", order.getOrderStatus(), "SELLER", orderDTOForRestored);
}
```

#### 4.3.2 退款处理时恢复买家可见性

**场景**：
- 卖家同意或拒绝退款申请
- 如果买家之前软删除了订单，系统会自动恢复可见性

**技术实现**：
```java
// OrderServiceImpl.java - handleRefund方法
boolean buyerVisibilityRestored = "HIDDEN".equals(order.getBuyerVisibility());
order.setBuyerVisibility("PUBLIC");

String notificationType = "APPROVE".equals(decision) ? "REFUND_APPROVED" : "REFUND_REJECTED";
pushOrderChangeNotification(order.getBuyerId(), order.getOrderId(), 
    notificationType, order.getOrderStatus(), "BUYER");

// 如果是恢复可见性，推送完整的OrderDTO
if (buyerVisibilityRestored) {
    OrderDTO orderDTOForRestored = convertToDTO(order);
    pushOrderChangeNotificationWithDTO(order.getBuyerId(), order.getOrderId(), 
        "ORDER_VISIBILITY_RESTORED", order.getOrderStatus(), "BUYER", orderDTOForRestored);
}
```

**恢复可见性的原因**：
1. **退款申请**：卖家必须能看到退款申请，以便及时处理
2. **退款处理结果**：买家必须能看到处理结果，以便采取后续行动
3. **用户体验**：即使订单被软删除，重要事件发生时也会自动恢复可见性

---

## 技术细节

### 5.1 WebSocket通知处理流程

#### 5.1.1 后端推送流程

```
订单状态变化
    ↓
OrderServiceImpl.操作方法（如payOrder、shipOrder等）
    ↓
pushOrderChangeNotification / pushOrderChangeNotificationWithDTO
    ↓
WebSocketRetryService.pushWithRetry
    ↓
Redis队列（如果用户离线）
    ↓
WebSocket推送（如果用户在线）
```

#### 5.1.2 前端处理流程

```
WebSocket消息接收
    ↓
websocket.js - handleMessage
    ↓
order.js - handleOrderChangeNotification
    ↓
判断当前页面路径
    ↓
[如果在相关页面]
    → 调用smartUpdateOrder回调
    → 直接更新订单列表
    
[如果不在相关页面]
    → 设置角标（sellerOrderHasNew / buyerOrderHasNew）
    → 用户点击进入页面后清除角标
```

### 5.2 角标清除机制

**实现位置**：
- 前端：`views/SellerOrders.vue` - `onMounted()`
- 前端：`views/MyOrders.vue` - `onMounted()`

**技术实现**：
```javascript
onMounted(() => {
  // 清除卖家订单变化提醒角标
  orderStore.clearSellerOrderNotification()
  // 注册订单更新回调
  orderStore.registerOrderUpdateCallback('seller', smartUpdateOrder)
  fetchOrders()
})
```

### 5.3 代码复用优化

**统一的通知处理**：
- 所有订单操作使用统一的通知推送方法
- 根据操作类型判断是否需要发送完整OrderDTO
- 根据操作类型和用户角色判断目标角色

**前端回调机制**：
- 使用注册回调的方式，避免Store和组件之间的强耦合
- 支持多个页面注册回调（理论上）
- 组件卸载时自动清理，避免内存泄漏

---

## 已知问题与限制

### 6.1 订单发货详情获取

**说明**：
- `ORDER_SHIPPED`通知不包含完整OrderDTO（只包含基本信息）
- 订单列表显示的图片等信息来自商品快照字段（`commoditySnapshotImages`），该字段存储在订单表中，不是联表查询
- 订单列表API返回的订单对象已包含所有快照信息（标题、描述、价格、图片等），无需额外详情API调用即可显示
- 异步获取详情仅用于更新`trackingNumber`等发货后才有的字段，不影响图片显示

**实现细节**：
- 订单列表使用`OrderCard`组件显示订单，图片来自`order.commoditySnapshotImages`字段
- `ORDER_SHIPPED`时，仅更新订单状态为`SHIPPED`（订单卡片不显示trackingNumber，因此不需要异步获取详情）
- 订单详情页面（`OrderDetail.vue`）是单独页面，查看时会调用`orderAPI.getDetail()`获取完整信息，自然包含所有字段（包括trackingNumber）

**结论**：
- 这不是一个限制，而是设计上的优化
- 订单列表显示不依赖详情API，图片等信息来自订单列表API返回的快照字段
- 订单卡片不显示trackingNumber，因此不需要额外的详情API调用
- 如果将来需要在订单卡片中显示物流单号，可以恢复异步获取详情的代码（已在代码中注释保留）

### 6.2 软删除恢复场景

**限制说明**：
- 只有在退款流程中才会自动恢复可见性
- 其他场景（如手动恢复）暂不支持

**缓解措施**：
- 退款流程是最常见的需要恢复可见性的场景
- 其他场景可以通过手动恢复或API调用实现

### 6.3 角标显示时机

**限制说明**：
- 角标仅在用户不在相关页面时显示
- 如果用户在相关页面，会直接更新列表，不显示角标

**设计考虑**：
- 避免重复提醒，提升用户体验
- 如果用户正在查看订单列表，直接更新更合理

---

## 总结

### v1.1.2 核心成就

1. **订单变化实时通知**：
   - WebSocket推送所有订单状态变化
   - 支持8种操作类型的通知
   - 根据目标角色智能判断通知对象
   - 使用重试机制确保离线用户也能收到通知

2. **智能角标提醒**：
   - 根据当前页面智能显示角标
   - 避免重复提醒和干扰
   - 进入相关页面后自动清除

3. **智能订单更新**：
   - 在订单页面时，无需刷新即可实时更新
   - 根据changeType智能判断需要更新的字段
   - 处理筛选范围变化和新订单添加
   - 支持完整OrderDTO的直接更新

4. **完整订单信息推送**：
   - 订单创建时推送完整OrderDTO，前端可直接使用
   - 可见性恢复时推送完整OrderDTO，直接更新或添加到列表
   - 减少API请求，提升性能和用户体验

5. **软删除自动恢复**：
   - 退款申请时自动恢复卖家可见性
   - 退款处理时自动恢复买家可见性
   - 确保重要事件发生时订单保持可见

6. **代码复用优化**：
   - 统一的通知处理机制
   - 回调注册机制，避免强耦合
   - 清晰的代码结构和职责划分

### 下一步规划

#### v1.1.3：管理端功能完善（已完成 ✅）
- **完整的后台管理功能**：用户、商品、订单的完整CRUD功能
- **N+1查询优化**：管理端商品和订单列表的批量查询优化
- **UI/UX优化**：选择器组件、图片上传、统一组件高度等
- **数据展示优化**：订单列表中显示买家和卖家头像、昵称
- **订单编辑功能**：支持订单状态和可见性的便捷编辑

#### v1.1.4：消息管理和管理员管理
**版本定位**：完善管理端剩余功能，包括消息管理和系统管理员管理功能

**核心功能**：
- **消息管理功能**
  - 用户消息管理：查看、搜索、删除用户间的聊天消息
  - 会话管理：查看所有会话列表，管理会话状态
  - 消息内容审核：支持消息内容的查看和审核
  - 消息统计：消息数量、活跃会话等统计信息
  
- **系统管理员管理功能**
  - 管理员账号列表：查看所有管理员账号
  - 创建管理员：系统管理员可以创建新的管理员账号
  - 编辑管理员：修改管理员信息、状态
  - 删除管理员：删除不需要的管理员账号（保护超级管理员）
  - 权限管理：管理员角色和权限分配（如需要）
  - 密码重置：重置管理员密码

**技术实现方向**：
- 前端消息管理页面完善（Messages.vue）
- 前端管理员管理页面实现
- 管理员权限控制（区分系统管理员和普通管理员）
- 消息审核功能（如需要）
- 管理员操作日志（可选）

#### v1.1.5+：持续优化用户体验
- **通知聚合**：同一订单的多个变化聚合为一条通知
- **通知历史**：保存通知历史，用户可以查看
- **通知设置**：允许用户自定义通知偏好
- **订单筛选优化**：支持更多筛选条件（如时间范围、金额范围等）
- **UI/UX优化**：持续改进界面和交互体验

#### v1.2.x：防止库存超卖（并发控制）
**版本定位**：专注于**并发控制**和**数据一致性**保障

**核心功能**：
- **库存超卖防护**
  - 数据库锁机制（悲观锁/乐观锁）
  - 库存扣减原子操作
  - 订单创建时的库存检查和扣减
  - 并发下单场景下的库存一致性保证
- **订单并发控制**
  - 防止重复下单
  - 订单状态的并发更新控制
  - 退款流程的并发处理
- **并发测试**
  - 高并发场景下的压力测试
  - 库存准确性验证
  - 订单一致性验证

**技术实现方向**：
- 使用数据库事务和锁机制
- 乐观锁版本号控制
- Redis分布式锁（如需要）
- 库存预留机制

#### v1.3.x：缓存和索引优化（性能优化）
**版本定位**：专注于**性能优化**和**系统效率**提升

**核心功能**：
- **缓存机制优化**
  - UserProfile缓存（Cache-Aside模式）
  - 商品信息缓存（热点商品）
  - 订单列表缓存（用户订单列表）
  - 消息列表缓存（前端内存缓存或服务端Redis缓存）
  - 缓存预热机制
  - 缓存一致性保证
  - 防止缓存穿透/击穿/雪崩
- **数据库索引优化**
  - 常用查询字段索引优化
  - 复合索引设计
  - 查询计划优化
  - 慢查询监控和分析
- **性能监控和日志系统**
  - 慢查询监控
  - 操作审计日志
  - 性能指标采集
  - 系统资源监控

**技术实现方向**：
- Redis缓存集成
- Spring Cache抽象层
- JPA查询优化
- 数据库索引策略
- 监控系统集成（如Prometheus）

#### v2.x：Spring AI集成（智能化升级）
**版本定位**：引入**人工智能**能力，提升用户体验和平台智能化水平

**核心功能**：
- **AI商品推荐**
  - 基于用户行为的个性化推荐
  - 商品相似度推荐
  - 智能搜索推荐
- **AI内容生成**
  - 商品描述智能生成
  - 商品标题优化建议
  - 商品分类智能识别
- **AI客服助手**
  - 智能问答系统
  - 自动回复常见问题
  - 聊天记录智能分析
- **AI数据分析**
  - 商品热度分析
  - 价格趋势预测
  - 用户行为分析
- **数据导出功能**
  - 订单批量导出（Excel/CSV格式）
    - 支持按时间范围、订单状态等条件筛选导出
    - 包含订单关键信息（订单号、金额、状态、时间、买卖双方信息等）
    - 用于报税、对账、记录留存等场景
  - 商品数据导出
    - 支持导出商品列表及统计信息
  - 用户数据导出
    - 支持导出用户信息及交易统计

**技术实现方向**：
- Spring AI框架集成
- 大语言模型（LLM）接入
- 向量数据库（如需要）
- 推荐算法实现
- AI模型训练和优化
- Excel/CSV导出功能（Apache POI或EasyExcel）
- 数据导出模板设计
- 大数据量导出优化（分页导出、异步导出）

**版本定位说明**：
- **v1.1.x**：专注于**用户体验优化**（UI、交互、实时更新、稳定性）
- **v1.2.x**：专注于**并发控制**和**数据一致性**（防止库存超卖、订单并发处理）
- **v1.3.x**：专注于**性能优化**和**系统效率**（缓存、索引、监控）
- **v2.x**：专注于**智能化升级**（Spring AI集成、推荐系统、智能客服）

---

**文档版本**：v1.1.2  
**最后更新**：2025-01-XX  
**维护者**：NJUMarket 开发团队

