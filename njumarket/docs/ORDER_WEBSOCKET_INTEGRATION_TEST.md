# 订单WebSocket通知前后端联调文档

## 概述

本文档用于指导订单WebSocket通知功能的前后端联调测试。由于代码是复用的，我们只需要测试所有可能的**操作类型**，而不需要测试所有订单状态变化。

## 测试要点

1. **操作类型**：不同的订单操作会产生不同的通知类型
2. **是否发送完整OrderDTO**：某些操作类型会发送完整的订单对象
3. **是否恢复可见性**：某些操作会自动恢复被软删除订单的可见性
4. **目标角色**：通知发送给卖家(SELLER)还是买家(BUYER)

---

## 一、所有订单操作类型清单

### 1.1 订单创建 (ORDER_CREATED)

**触发操作**：买家创建订单

**后端实现**：
```java
// OrderServiceImpl.java - createOrder方法
OrderDTO orderDTOForNotification = convertToDTO(order);
pushOrderChangeNotificationWithDTO(order.getSellerId(), order.getOrderId(), 
    "ORDER_CREATED", order.getOrderStatus(), "SELLER", orderDTOForNotification);
```

**通知特点**：
- ✅ **发送完整OrderDTO**（`order`字段包含完整订单对象）
- 目标角色：`SELLER`（通知卖家）
- 不涉及可见性恢复

**测试步骤**：
1. 买家登录，选择商品创建订单
2. 卖家应该收到WebSocket通知，类型为`ORDER_CHANGE`
3. 验证通知内容：
   - `changeType`: `"ORDER_CREATED"`
   - `targetRole`: `"SELLER"`
   - `order`: 包含完整的订单DTO对象
4. 如果卖家在"卖家订单"页面，订单应该直接添加到列表
5. 如果卖家不在"卖家订单"页面，应该显示角标

---

### 1.2 订单支付 (ORDER_PAID)

**触发操作**：买家支付订单

**后端实现**：
```java
// OrderServiceImpl.java - payOrder方法
pushOrderChangeNotification(order.getSellerId(), order.getOrderId(), 
    "ORDER_PAID", order.getOrderStatus(), "SELLER");
```

**通知特点**：
- ❌ **不发送完整OrderDTO**（只包含基本信息）
- 目标角色：`SELLER`（通知卖家）
- 不涉及可见性恢复

**测试步骤**：
1. 买家支付一个待支付订单
2. 卖家应该收到WebSocket通知
3. 验证通知内容：
   - `changeType`: `"ORDER_PAID"`
   - `targetRole`: `"SELLER"`
   - `order`: 不存在（或为null）
4. 如果卖家在"卖家订单"页面，订单状态应该更新为`PAID`
5. 如果卖家不在"卖家订单"页面，应该显示角标

---

### 1.3 订单发货 (ORDER_SHIPPED)

**触发操作**：卖家发货订单

**后端实现**：
```java
// OrderServiceImpl.java - shipOrder方法
pushOrderChangeNotification(order.getBuyerId(), order.getOrderId(), 
    "ORDER_SHIPPED", order.getOrderStatus(), "BUYER");
```

**通知特点**：
- ❌ **不发送完整OrderDTO**（只包含基本信息）
- 目标角色：`BUYER`（通知买家）
- 不涉及可见性恢复
- ⚠️ **前端会异步获取详情**（因为需要trackingNumber等信息）

**测试步骤**：
1. 卖家为已支付订单发货（填写快递单号）
2. 买家应该收到WebSocket通知
3. 验证通知内容：
   - `changeType`: `"ORDER_SHIPPED"`
   - `targetRole`: `"BUYER"`
   - `order`: 不存在（或为null）
4. 如果买家在"我的订单"页面：
   - 订单状态立即更新为`SHIPPED`
   - 前端会异步调用`orderAPI.getDetail()`获取完整信息（包括trackingNumber）
5. 如果买家不在"我的订单"页面，应该显示角标

---

### 1.4 订单完成 (ORDER_COMPLETED)

**触发操作**：买家确认收货

**后端实现**：
```java
// OrderServiceImpl.java - confirmOrder方法
pushOrderChangeNotification(order.getSellerId(), order.getOrderId(), 
    "ORDER_COMPLETED", order.getOrderStatus(), "SELLER");
```

**通知特点**：
- ❌ **不发送完整OrderDTO**（只包含基本信息）
- 目标角色：`SELLER`（通知卖家）
- 不涉及可见性恢复

**测试步骤**：
1. 买家确认收货
2. 卖家应该收到WebSocket通知
3. 验证通知内容：
   - `changeType`: `"ORDER_COMPLETED"`
   - `targetRole`: `"SELLER"`
   - `order`: 不存在（或为null）
4. 如果卖家在"卖家订单"页面，订单状态应该更新为`COMPLETED`
5. 如果卖家不在"卖家订单"页面，应该显示角标

---

### 1.5 订单取消 (ORDER_CANCELLED)

**触发操作**：买家或卖家取消订单

**后端实现**：
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

**通知特点**：
- ❌ **不发送完整OrderDTO**（只包含基本信息）
- 目标角色：根据取消者判断（买家取消通知卖家，卖家取消通知买家）
- 不涉及可见性恢复

**测试步骤**：
1. **场景A**：买家取消订单
   - 买家应该能看到订单状态更新
   - 卖家应该收到WebSocket通知（`targetRole: "SELLER"`）
2. **场景B**：卖家取消订单
   - 卖家应该能看到订单状态更新
   - 买家应该收到WebSocket通知（`targetRole: "BUYER"`）
3. 验证通知内容：
   - `changeType`: `"ORDER_CANCELLED"`
   - `targetRole`: 根据取消者确定
   - `order`: 不存在（或为null）

---

### 1.6 退款申请 (REFUND_REQUESTED)

**触发操作**：买家申请退款

**后端实现**：
```java
// OrderServiceImpl.java - requestRefund方法
boolean sellerVisibilityRestored = "HIDDEN".equals(order.getSellerVisibility());
order.setSellerVisibility("PUBLIC");

pushOrderChangeNotification(order.getSellerId(), order.getOrderId(), 
    "REFUND_REQUESTED", order.getOrderStatus(), "SELLER");

if (sellerVisibilityRestored) {
    OrderDTO orderDTOForRestored = convertToDTO(order);
    pushOrderChangeNotificationWithDTO(order.getSellerId(), order.getOrderId(), 
        "ORDER_VISIBILITY_RESTORED", order.getOrderStatus(), "SELLER", orderDTOForRestored);
}
```

**通知特点**：
- ❌ **REFUND_REQUESTED通知不发送完整OrderDTO**（只包含基本信息）
- ✅ **如果恢复可见性，会额外发送ORDER_VISIBILITY_RESTORED通知，包含完整OrderDTO**
- 目标角色：`SELLER`（通知卖家）
- ✅ **涉及可见性恢复**（如果订单被卖家软删除，会自动恢复）

**测试步骤**：
1. **场景A**：正常退款申请（订单未被软删除）
   - 买家申请退款
   - 卖家应该收到一个`REFUND_REQUESTED`通知
   - 验证：`order`字段不存在
2. **场景B**：退款申请恢复可见性（订单被卖家软删除）
   - 卖家先软删除订单（设置可见性为HIDDEN）
   - 买家申请退款
   - 卖家应该收到**两个**通知：
     - `REFUND_REQUESTED`通知（不包含完整OrderDTO）
     - `ORDER_VISIBILITY_RESTORED`通知（包含完整OrderDTO）
   - 验证第二个通知：
     - `changeType`: `"ORDER_VISIBILITY_RESTORED"`
     - `targetRole`: `"SELLER"`
     - `order`: 包含完整的订单DTO对象
   - 如果卖家在"卖家订单"页面，订单应该直接更新或添加到列表

---

### 1.7 退款处理 - 同意 (REFUND_APPROVED)

**触发操作**：卖家同意退款申请

**后端实现**：
```java
// OrderServiceImpl.java - handleRefund方法
boolean buyerVisibilityRestored = "HIDDEN".equals(order.getBuyerVisibility());
order.setBuyerVisibility("PUBLIC");

String notificationType = "REFUND_APPROVED";
pushOrderChangeNotification(order.getBuyerId(), order.getOrderId(), 
    notificationType, order.getOrderStatus(), "BUYER");

if (buyerVisibilityRestored) {
    OrderDTO orderDTOForRestored = convertToDTO(order);
    pushOrderChangeNotificationWithDTO(order.getBuyerId(), order.getOrderId(), 
        "ORDER_VISIBILITY_RESTORED", order.getOrderStatus(), "BUYER", orderDTOForRestored);
}
```

**通知特点**：
- ❌ **REFUND_APPROVED通知不发送完整OrderDTO**（只包含基本信息）
- ✅ **如果恢复可见性，会额外发送ORDER_VISIBILITY_RESTORED通知，包含完整OrderDTO**
- 目标角色：`BUYER`（通知买家）
- ✅ **涉及可见性恢复**（如果订单被买家软删除，会自动恢复）

**测试步骤**：
1. **场景A**：正常退款处理（订单未被软删除）
   - 卖家同意退款申请
   - 买家应该收到一个`REFUND_APPROVED`通知
   - 验证：`order`字段不存在
2. **场景B**：退款处理恢复可见性（订单被买家软删除）
   - 买家先软删除订单（设置可见性为HIDDEN）
   - 卖家同意退款申请
   - 买家应该收到**两个**通知：
     - `REFUND_APPROVED`通知（不包含完整OrderDTO）
     - `ORDER_VISIBILITY_RESTORED`通知（包含完整OrderDTO）
   - 验证第二个通知：
     - `changeType`: `"ORDER_VISIBILITY_RESTORED"`
     - `targetRole`: `"BUYER"`
     - `order`: 包含完整的订单DTO对象
   - 如果买家在"我的订单"页面，订单应该直接更新或添加到列表

---

### 1.8 退款处理 - 拒绝 (REFUND_REJECTED)

**触发操作**：卖家拒绝退款申请

**后端实现**：
```java
// OrderServiceImpl.java - handleRefund方法
boolean buyerVisibilityRestored = "HIDDEN".equals(order.getBuyerVisibility());
order.setBuyerVisibility("PUBLIC");

String notificationType = "REFUND_REJECTED";
pushOrderChangeNotification(order.getBuyerId(), order.getOrderId(), 
    notificationType, order.getOrderStatus(), "BUYER");

if (buyerVisibilityRestored) {
    OrderDTO orderDTOForRestored = convertToDTO(order);
    pushOrderChangeNotificationWithDTO(order.getBuyerId(), order.getOrderId(), 
        "ORDER_VISIBILITY_RESTORED", order.getOrderStatus(), "BUYER", orderDTOForRestored);
}
```

**通知特点**：
- ❌ **REFUND_REJECTED通知不发送完整OrderDTO**（只包含基本信息）
- ✅ **如果恢复可见性，会额外发送ORDER_VISIBILITY_RESTORED通知，包含完整OrderDTO**
- 目标角色：`BUYER`（通知买家）
- ✅ **涉及可见性恢复**（如果订单被买家软删除，会自动恢复）

**测试步骤**：
1. **场景A**：正常退款拒绝（订单未被软删除）
   - 卖家拒绝退款申请
   - 买家应该收到一个`REFUND_REJECTED`通知
   - 验证：`order`字段不存在
2. **场景B**：退款拒绝恢复可见性（订单被买家软删除）
   - 买家先软删除订单（设置可见性为HIDDEN）
   - 卖家拒绝退款申请
   - 买家应该收到**两个**通知：
     - `REFUND_REJECTED`通知（不包含完整OrderDTO）
     - `ORDER_VISIBILITY_RESTORED`通知（包含完整OrderDTO）
   - 验证第二个通知包含完整OrderDTO

---

## 二、操作类型总结表

| 操作类型 | changeType | 目标角色 | 发送完整OrderDTO | 涉及可见性恢复 | 备注 |
|---------|------------|---------|----------------|--------------|------|
| 订单创建 | ORDER_CREATED | SELLER | ✅ 是 | ❌ 否 | 必须测试 |
| 订单支付 | ORDER_PAID | SELLER | ❌ 否 | ❌ 否 | 必须测试 |
| 订单发货 | ORDER_SHIPPED | BUYER | ❌ 否 | ❌ 否 | 必须测试（前端会异步获取详情） |
| 订单完成 | ORDER_COMPLETED | SELLER | ❌ 否 | ❌ 否 | 必须测试 |
| 订单取消 | ORDER_CANCELLED | SELLER/BUYER | ❌ 否 | ❌ 否 | 必须测试（两种情况） |
| 退款申请 | REFUND_REQUESTED | SELLER | ❌ 否 | ✅ 可能 | 必须测试（测试恢复可见性场景） |
| 退款同意 | REFUND_APPROVED | BUYER | ❌ 否 | ✅ 可能 | 必须测试（测试恢复可见性场景） |
| 退款拒绝 | REFUND_REJECTED | BUYER | ❌ 否 | ✅ 可能 | 必须测试（测试恢复可见性场景） |
| 可见性恢复 | ORDER_VISIBILITY_RESTORED | SELLER/BUYER | ✅ 是 | ✅ 是 | 仅在恢复可见性时发送 |

---

## 三、联调测试建议

### 3.1 核心测试场景（必须测试）

#### 场景1：订单创建（包含完整OrderDTO）
- ✅ 测试完整OrderDTO是否正确发送
- ✅ 测试卖家在"卖家订单"页面时，订单是否正确添加到列表
- ✅ 测试卖家不在"卖家订单"页面时，角标是否正确显示

#### 场景2：订单状态更新（不包含完整OrderDTO）
- ✅ 测试支付、发货、完成、取消等操作
- ✅ 测试在相关页面时，订单状态是否正确更新
- ✅ 测试不在相关页面时，角标是否正确显示

#### 场景3：退款申请恢复可见性
- ✅ 测试卖家软删除订单后，买家申请退款
- ✅ 验证是否收到`ORDER_VISIBILITY_RESTORED`通知
- ✅ 验证`ORDER_VISIBILITY_RESTORED`通知是否包含完整OrderDTO
- ✅ 测试卖家在"卖家订单"页面时，订单是否正确更新/添加

#### 场景4：退款处理恢复可见性
- ✅ 测试买家软删除订单后，卖家处理退款（同意/拒绝）
- ✅ 验证是否收到`ORDER_VISIBILITY_RESTORED`通知
- ✅ 验证`ORDER_VISIBILITY_RESTORED`通知是否包含完整OrderDTO
- ✅ 测试买家在"我的订单"页面时，订单是否正确更新/添加

### 3.2 简化测试场景（可选）

由于代码是复用的，以下场景可以选择性测试：
- ❌ 不需要测试所有订单状态的组合
- ❌ 不需要测试每个操作在所有状态下的行为
- ✅ 只需要测试每个操作类型的通知是否正常发送
- ✅ 只需要测试是否包含完整OrderDTO的情况

### 3.3 测试检查清单

每个测试场景需要验证：

1. **WebSocket连接**：
   - [ ] WebSocket连接正常
   - [ ] 能够收到通知消息

2. **通知格式**：
   - [ ] `type`: `"ORDER_CHANGE"`
   - [ ] `orderId`: 订单ID
   - [ ] `changeType`: 正确的变化类型
   - [ ] `orderStatus`: 正确的订单状态
   - [ ] `targetRole`: 正确的目标角色
   - [ ] `timestamp`: 时间戳

3. **完整OrderDTO（如果应该包含）**：
   - [ ] `order`字段存在
   - [ ] `order`包含所有必要字段（orderId, orderStatus, commoditySnapshot等）
   - [ ] 如果订单在列表中，能够直接更新
   - [ ] 如果订单不在列表中，能够正确添加到列表

4. **可见性恢复（如果涉及）**：
   - [ ] 订单可见性从HIDDEN恢复为PUBLIC
   - [ ] 收到`ORDER_VISIBILITY_RESTORED`通知
   - [ ] `ORDER_VISIBILITY_RESTORED`通知包含完整OrderDTO
   - [ ] 订单能够正确显示在列表中

5. **前端响应**：
   - [ ] 在相关页面时，订单正确更新/添加
   - [ ] 不在相关页面时，角标正确显示
   - [ ] 角标在进入相关页面后正确清除

---

## 四、调试工具建议

### 4.1 前端调试

在浏览器控制台中检查：

```javascript
// 查看订单store状态
import { useOrderStore } from './stores/order'
const orderStore = useOrderStore()
console.log('订单通知列表:', orderStore.notifications)
console.log('卖家订单有更新:', orderStore.sellerOrderHasNew)
console.log('买家订单有更新:', orderStore.buyerOrderHasNew)

// 查看WebSocket消息
// 在websocket.js中已添加日志，检查控制台输出
```

### 4.2 后端调试

在后端日志中检查：

```java
// OrderServiceImpl.java中已有日志
log.debug("订单变化通知推送尝试（带重试）: userId={}, orderId={}, changeType={}, orderStatus={}, targetRole={}, hasOrderDTO={}", ...);

// 检查WebSocket重试队列
// 如果用户离线，通知会被加入重试队列
```

### 4.3 测试数据准备

1. **创建测试订单**：
   - 准备商品、买家、卖家账号
   - 创建多个不同状态的订单

2. **软删除订单**：
   - 测试恢复可见性场景时，需要先软删除订单
   - 使用API: `PUT /user/order/{orderId}/seller-visibility?sellerVisibility=HIDDEN`
   - 使用API: `PUT /user/order/{orderId}/buyer-visibility?buyerVisibility=HIDDEN`

---

## 五、常见问题排查

### 5.1 收不到通知

1. 检查WebSocket连接是否正常
2. 检查用户是否在线（离线通知会加入重试队列）
3. 检查`targetRole`是否正确匹配当前用户角色
4. 检查后端日志，确认通知已发送

### 5.2 订单没有更新

1. 检查是否在正确的页面（卖家订单页面 vs 我的订单页面）
2. 检查订单是否在当前筛选范围内（如果使用了tab筛选）
3. 检查前端`smartUpdateOrder`函数是否正确执行
4. 检查浏览器控制台是否有错误

### 5.3 完整OrderDTO缺失

1. 检查`changeType`是否为`ORDER_CREATED`或`ORDER_VISIBILITY_RESTORED`
2. 检查后端`pushOrderChangeNotificationWithDTO`是否传递了`orderDTO`参数
3. 检查前端是否正确提取`notification.order`字段

### 5.4 可见性恢复不工作

1. 检查订单的可见性是否真的是HIDDEN
2. 检查后端是否检测到`visibilityRestored = true`
3. 检查是否发送了`ORDER_VISIBILITY_RESTORED`通知
4. 检查前端是否正确处理了该通知类型

---

## 六、测试优先级

### 高优先级（必须完成）
1. ✅ 订单创建（包含完整OrderDTO）
2. ✅ 退款申请恢复可见性（包含完整OrderDTO）
3. ✅ 退款处理恢复可见性（包含完整OrderDTO）
4. ✅ 基本订单状态更新（支付、发货、完成）

### 中优先级（建议完成）
5. ✅ 订单取消（两种角色）
6. ✅ 发货时异步获取详情

### 低优先级（可选）
7. ⚠️ 所有状态的完整组合测试
8. ⚠️ 边界情况测试

---

## 七、联调完成标准

### 必须满足的条件

1. ✅ 所有8种操作类型的通知都能正常发送
2. ✅ `ORDER_CREATED`和`ORDER_VISIBILITY_RESTORED`包含完整OrderDTO
3. ✅ 其他操作类型不包含完整OrderDTO（或为null）
4. ✅ 恢复可见性时，能够正确发送`ORDER_VISIBILITY_RESTORED`通知
5. ✅ 前端能够正确解析通知并更新/添加订单
6. ✅ 角标能够正确显示和清除
7. ✅ 在相关页面时，订单能够实时更新
8. ✅ 不在相关页面时，角标能够正确显示

---

## 八、附录

### 8.1 通知JSON格式示例

**ORDER_CREATED（包含完整OrderDTO）**：
```json
{
  "type": "ORDER_CHANGE",
  "orderId": "order123",
  "changeType": "ORDER_CREATED",
  "orderStatus": "CREATED",
  "targetRole": "SELLER",
  "timestamp": "2024-01-01T12:00:00",
  "order": {
    "orderId": "order123",
    "orderStatus": "CREATED",
    "payAmount": 100.0,
    "quantity": 1,
    "commoditySnapshotTitle": "商品标题",
    // ... 其他字段
  }
}
```

**ORDER_PAID（不包含完整OrderDTO）**：
```json
{
  "type": "ORDER_CHANGE",
  "orderId": "order123",
  "changeType": "ORDER_PAID",
  "orderStatus": "PAID",
  "targetRole": "SELLER",
  "timestamp": "2024-01-01T12:00:00"
}
```

**ORDER_VISIBILITY_RESTORED（包含完整OrderDTO）**：
```json
{
  "type": "ORDER_CHANGE",
  "orderId": "order123",
  "changeType": "ORDER_VISIBILITY_RESTORED",
  "orderStatus": "REFUND_REQUESTED",
  "targetRole": "SELLER",
  "timestamp": "2024-01-01T12:00:00",
  "order": {
    "orderId": "order123",
    "orderStatus": "REFUND_REQUESTED",
    "sellerVisibility": "PUBLIC",
    // ... 其他字段
  }
}
```

### 8.2 相关代码文件

**后端**：
- `njumarket/src/main/java/com/njumarket/njumarket/service/impl/OrderServiceImpl.java`
  - `pushOrderChangeNotification`（轻量级通知）
  - `pushOrderChangeNotificationWithDTO`（包含完整OrderDTO）

**前端**：
- `njumarket-front/NJUMarket/src/stores/order.js`
  - `handleOrderChangeNotification`（处理通知）
  - `registerOrderUpdateCallback`（注册更新回调）
- `njumarket-front/NJUMarket/src/views/SellerOrders.vue`
  - `smartUpdateOrder`（卖家订单智能更新）
- `njumarket-front/NJUMarket/src/views/MyOrders.vue`
  - `smartUpdateOrder`（买家订单智能更新）

---

## 九、订单软删除限制说明

### 9.1 订单软删除规则

根据后端代码 `Order.canModifyVisibility()` 方法的实现，订单软删除的限制如下：

**禁止软删除的订单状态**（需要处理订单或退款，必须保持可见）：
- ❌ `CREATED`（待支付）- 买家需要支付，卖家需要等待
- ❌ `PAID`（待发货）- 卖家需要发货，买家需要等待
- ❌ `SHIPPED`（待收货）- 买家需要确认收货，卖家需要等待
- ❌ `REFUND_REQUESTED`（退款申请中）- 卖家需要处理退款申请

**允许软删除的订单状态**：
- ✅ `COMPLETED`（已完成）
- ✅ `CANCELLED`（已取消）
- ✅ `REFUND_APPROVED`（退款完成）
- ✅ `REFUND_REJECTED`（退款被拒绝）- **允许软删除，但如果买家重新申请退款，会自动恢复可见性**
- ✅ 其他所有状态

### 9.2 限制原因

禁止软删除的状态需要买卖双方处理订单或退款，必须保持可见：
- **待支付（CREATED）**：买家需要支付，卖家需要等待
- **待发货（PAID）**：卖家需要发货，买家需要等待
- **待收货（SHIPPED）**：买家需要确认收货，卖家需要等待
- **退款申请中（REFUND_REQUESTED）**：卖家需要处理退款申请

**退款被拒绝（REFUND_REJECTED）的特殊处理**：
- 允许软删除（订单已完成流程，可以隐藏）
- 但如果买家重新申请退款，系统会**自动恢复卖家可见性**，确保卖家能够看到新的退款申请

### 9.3 自动恢复可见性机制

当订单进入需要处理的状态时，如果之前被软删除（HIDDEN），系统会**自动恢复可见性**：

1. **退款申请时（REFUND_REQUESTED）**：
   - 无论订单之前是 `COMPLETED` 还是 `REFUND_REJECTED` 状态，如果卖家之前软删除了订单，系统会自动设置 `sellerVisibility = "PUBLIC"`
   - 触发 `ORDER_VISIBILITY_RESTORED` 通知（包含完整OrderDTO）
   - **特别说明**：即使 `REFUND_REJECTED` 状态允许软删除，但买家重新申请退款时，会强制恢复卖家可见性

2. **退款处理时（REFUND_APPROVED/REFUND_REJECTED）**：
   - 如果买家之前软删除了订单，系统会自动设置 `buyerVisibility = "PUBLIC"`
   - 触发 `ORDER_VISIBILITY_RESTORED` 通知（包含完整OrderDTO）

### 9.4 后端实现代码位置

```java
// Order.java - canModifyVisibility() 方法
public Boolean canModifyVisibility() {
    // 禁止删除的状态：待支付、待发货、待收货、退款申请中
    // 这些状态需要买卖双方处理订单或退款，必须保持可见
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

### 9.5 测试建议

在测试可见性恢复功能时，需要注意：

1. **测试前准备**：
   - 将订单状态设置为允许软删除的状态（如 `COMPLETED`）
   - 软删除订单（设置可见性为 `HIDDEN`）

2. **测试退款申请恢复可见性**：
   - **场景A**：从 `COMPLETED` 状态申请退款
     - 确保订单状态为 `COMPLETED`
     - 卖家软删除订单（设置 `sellerVisibility = "HIDDEN"`）
     - 买家申请退款后，订单状态变为 `REFUND_REQUESTED`
     - 验证：卖家可见性自动恢复为 `PUBLIC`，收到 `ORDER_VISIBILITY_RESTORED` 通知
   - **场景B**：从 `REFUND_REJECTED` 状态重新申请退款
     - 确保订单状态为 `REFUND_REJECTED`（允许软删除）
     - 卖家软删除订单（设置 `sellerVisibility = "HIDDEN"`）
     - 买家重新申请退款后，订单状态变为 `REFUND_REQUESTED`
     - 验证：卖家可见性自动恢复为 `PUBLIC`，收到 `ORDER_VISIBILITY_RESTORED` 通知

3. **测试退款处理恢复可见性**：
   - 确保订单状态为 `REFUND_REQUESTED`
   - 卖家处理退款（同意或拒绝）后，订单状态变为 `REFUND_APPROVED` 或 `REFUND_REJECTED`
   - 此时即使买家之前软删除了订单，也会自动恢复可见性

4. **测试软删除限制**：
   - 尝试在 `CREATED` 状态时软删除订单，应该返回错误：`"订单状态不允许修改可见性"`
   - 尝试在 `PAID` 状态时软删除订单，应该返回错误：`"订单状态不允许修改可见性"`
   - 尝试在 `SHIPPED` 状态时软删除订单，应该返回错误：`"订单状态不允许修改可见性"`
   - 尝试在 `REFUND_REQUESTED` 状态时软删除订单，应该返回错误：`"订单状态不允许修改可见性"`
   - ✅ 尝试在 `REFUND_REJECTED` 状态时软删除订单，应该成功（允许软删除）
   - ✅ 在 `REFUND_REJECTED` 状态软删除后，买家重新申请退款，应该自动恢复卖家可见性

---

## 更新记录

- 2024-01-XX: 初始版本，包含所有订单操作类型的联调指南
- 2024-01-XX: 新增订单软删除限制说明章节

