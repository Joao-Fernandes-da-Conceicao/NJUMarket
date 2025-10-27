# 消息机制优化 - 后端改动分析

## 需求概述

1. **Conversation 改为用户对唯一**: 删除 `commodity_id` 和 `order_id` 字段，确保一对用户只能有一个 conversation
2. **支持发送商品和订单卡片**: Message 实体支持实时商品/订单卡片（基于 ID 实时查询，非快照）
   - 商品卡片：暂时 TODO（需要卖家商品查询页面）
   - 订单卡片：从双方订单中查询（已实现）

## 数据库改动分析

### 1. `conversations` 表修改

**当前状态**（根据用户描述）:
```sql
conversation_id, buyer_id, seller_id, commodity_id, order_id, 
last_message_content, last_message_time, buyer_unread_count, 
seller_unread_count, status, created_at, updated_at
```

**需要修改**:
- ✅ **删除列**: `commodity_id`, `order_id`
- ✅ **添加唯一约束**: 确保 `(buyer_id, seller_id)` 组合唯一（或 `(user_id_1, user_id_2)` 如果已迁移）

**注意**: 当前实体类 `Conversation.java` 使用的是 `user_id_1` 和 `user_id_2`，但用户描述的数据库结构是 `buyer_id` 和 `seller_id`。需要确认：
- 如果数据库还未迁移，应保持 `buyer_id` 和 `seller_id`，并在此基础添加唯一约束
- 如果已迁移到 `user_id_1` 和 `user_id_2`，则删除 `commodity_id` 和 `order_id` 即可

### 2. `messages` 表修改

**需要添加**:
- ✅ `commodity_id` VARCHAR(50) - 用于商品卡片（实时查询）
- ✅ `order_id` VARCHAR(50) - 用于订单卡片（实时查询）

**注意**: 当前 Message 实体已有 `commoditySnapshotId` 和 `orderSnapshotId`（快照），新增的 `commodity_id` 和 `order_id` 用于实时查询，两者用途不同。

## 后端代码改动分析

### 1. Entity 层改动

#### `Conversation.java`
**当前问题**:
- 实体类使用 `user_id_1` 和 `user_id_2`，但数据库可能是 `buyer_id` 和 `seller_id`
- 需要确认数据库结构并保持一致

**需要修改**（如果数据库仍使用 `buyer_id` 和 `seller_id`）:
```java
// 如果数据库未迁移，需要将实体类改回使用 buyer_id 和 seller_id
@Column(name = "buyer_id", nullable = false)
private String buyerId;

@Column(name = "seller_id", nullable = false)
private String sellerId;

// 删除 commodityId 和 orderId 相关字段（如果存在）
// 添加唯一约束（数据库层面或 JPA 层面）
@Table(uniqueConstraints = @UniqueConstraint(
    name = "uk_buyer_seller_active", 
    columnNames = {"buyer_id", "seller_id", "status"}
))
```

**如果数据库已迁移到 `user_id_1` 和 `user_id_2`**:
- ✅ 当前实体类结构已正确
- ✅ 只需确认数据库已删除 `commodity_id` 和 `order_id`

#### `Message.java`
**需要添加字段**:
```java
@Column(name = "commodity_id", length = 50)
private String commodityId; // 实时商品ID（用于商品卡片）

@Column(name = "order_id", length = 50)
private String orderId; // 实时订单ID（用于订单卡片）
```

**消息类型枚举**（建议）:
```java
// messageType 可能的取值：
// "TEXT" - 文本消息
// "IMAGE" - 图片消息
// "COMMODITY_CARD" - 商品卡片（实时）
// "ORDER_CARD" - 订单卡片（实时）
```

### 2. Repository 层改动

#### `ConversationRepository.java`
**需要修改的方法**:

1. **`getOrCreateConversation` 相关**:
   - 当前使用 `findByBuyerIdAndSellerId` 或 `findByCommodityAndUsers`
   - 需要改为：基于 `buyer_id` 和 `seller_id` 查找，忽略 `commodity_id` 和 `order_id`
   - 确保唯一性：如果存在活跃对话，直接返回；否则创建

2. **添加或修改查询方法**:
```java
// 确保一对用户只有一个活跃对话
Optional<Conversation> findByBuyerIdAndSellerIdAndStatus(
    @Param("buyerId") String buyerId, 
    @Param("sellerId") String sellerId,
    @Param("status") String status
);
```

**如果使用 `user_id_1` 和 `user_id_2`**:
- ✅ `findByUserPairActive` 已存在，但需要确保唯一约束生效

### 3. Service 层改动

#### `ContactServiceImpl.java`

**1. `getOrCreateConversation` 方法**（需要修改）:
```java
// 当前逻辑：
// - 根据 commodityId 或 buyerId/sellerId 查找对话
// - 如果不存在则创建，可能传入 commodityId 和 orderId

// 修改后逻辑：
// - 仅基于 buyerId 和 sellerId 查找（忽略 commodityId 和 orderId）
// - 确保唯一性：一对用户只有一个活跃对话
// - 创建时不再传入 commodityId 和 orderId
```

**2. `sendMessage` 方法**（需要修改）:
```java
// 当前逻辑：
// - 支持发送文本、图片消息
// - 支持 commoditySnapshotId 和 orderSnapshotId（快照）

// 修改后逻辑：
// - 支持发送文本、图片消息（保持）
// - 支持发送商品卡片（messageType = "COMMODITY_CARD", commodityId 实时查询）TODO
// - 支持发送订单卡片（messageType = "ORDER_CARD", orderId 实时查询）
// - 验证：发送订单卡片时，验证订单属于双方（buyerId 或 sellerId）
```

**3. 新增方法**（可选）:
```java
// 验证订单是否属于双方用户
private boolean validateOrderAccess(String orderId, String userId1, String userId2) {
    // 查询订单，检查 buyerId 和 sellerId 是否匹配
    // 返回 true 如果订单属于这两个用户之一
}
```

### 4. Controller 层改动

#### `ContactController.java`

**`createConversation` 方法**（需要修改）:
```java
// 当前：
@PostMapping("/conversations/create")
public Result createConversation(
    @RequestAttribute("userId") String userId,
    @RequestParam String otherUserId,
    @RequestParam(required = false) String commodityId,  // 需要删除
    @RequestParam(required = false) String orderId) {     // 需要删除
    return contactService.getOrCreateConversation(userId, otherUserId, commodityId, orderId);
}

// 修改后：
@PostMapping("/conversations/create")
public Result createConversation(
    @RequestAttribute("userId") String userId,
    @RequestParam String otherUserId) {
    return contactService.getOrCreateConversation(userId, otherUserId);
}
```

**`sendMessage` 方法**（需要修改）:
```java
// SendMessageRequest 已包含 commodityId 和 orderId
// 需要在 Service 层处理商品/订单卡片的发送逻辑
```

### 5. DTO 层改动

#### `SendMessageRequest.java`
- ✅ 已包含 `commodityId` 和 `orderId` 字段
- ✅ 无需修改，但需要明确用途：
  - `commodityId`: 用于发送商品卡片（实时查询）
  - `orderId`: 用于发送订单卡片（实时查询）

### 6. 新增业务逻辑

#### 订单卡片发送逻辑
```java
// 在 ContactServiceImpl.sendMessage 中：
if ("ORDER_CARD".equals(request.getMessageType()) && request.getOrderId() != null) {
    // 1. 验证订单是否存在且属于双方用户
    Optional<Order> orderOpt = orderRepository.findById(request.getOrderId());
    if (orderOpt.isEmpty()) {
        return Result.fail("订单不存在");
    }
    Order order = orderOpt.get();
    
    // 验证：订单的 buyerId 和 sellerId 必须匹配当前对话的双方
    String otherUserId = conversation.getOtherUserId(userId);
    if (!order.getBuyerId().equals(userId) && !order.getBuyerId().equals(otherUserId)) {
        return Result.fail("无权发送此订单卡片");
    }
    if (!order.getSellerId().equals(userId) && !order.getSellerId().equals(otherUserId)) {
        return Result.fail("无权发送此订单卡片");
    }
    
    // 2. 设置消息的 orderId 字段（用于前端实时查询）
    message.setOrderId(request.getOrderId());
    message.setMessageType("ORDER_CARD");
    message.setContent("订单卡片"); // 或订单状态等简要信息
}
```

#### 商品卡片发送逻辑（TODO）
```java
// 暂时 TODO，等待卖家商品查询页面实现
if ("COMMODITY_CARD".equals(request.getMessageType()) && request.getCommodityId() != null) {
    // TODO: 实现商品卡片发送逻辑
    return Result.fail("商品卡片功能暂未实现");
}
```

## 数据库迁移脚本

### 方案 A: 如果数据库仍使用 `buyer_id` 和 `seller_id`

```sql
-- 1. 删除 commodity_id 和 order_id 列
ALTER TABLE conversations 
DROP COLUMN IF EXISTS commodity_id,
DROP COLUMN IF EXISTS order_id;

-- 2. 清理重复的对话（保留最新的）
DELETE c1 FROM conversations c1
INNER JOIN conversations c2 
WHERE c1.conversation_id < c2.conversation_id
  AND c1.buyer_id = c2.buyer_id 
  AND c1.seller_id = c2.seller_id
  AND c1.status = 'ACTIVE'
  AND c2.status = 'ACTIVE';

-- 3. 添加唯一约束（确保一对用户只有一个活跃对话）
ALTER TABLE conversations 
ADD UNIQUE INDEX uk_buyer_seller_active (buyer_id, seller_id, status);

-- 4. 为 messages 表添加商品和订单ID字段（实时查询）
ALTER TABLE messages 
ADD COLUMN IF NOT EXISTS commodity_id VARCHAR(50) COMMENT '商品ID（实时查询）',
ADD COLUMN IF NOT EXISTS order_id VARCHAR(50) COMMENT '订单ID（实时查询）';

-- 5. 添加索引
CREATE INDEX IF NOT EXISTS idx_messages_commodity_id ON messages(commodity_id);
CREATE INDEX IF NOT EXISTS idx_messages_order_id ON messages(order_id);
```

### 方案 B: 如果数据库已迁移到 `user_id_1` 和 `user_id_2`

```sql
-- 1. 删除 commodity_id 和 order_id 列（如果还存在）
ALTER TABLE conversations 
DROP COLUMN IF EXISTS commodity_id,
DROP COLUMN IF EXISTS order_id;

-- 2. 确保唯一约束存在
-- （实体类中已有 @UniqueConstraint，确认数据库是否已创建）
ALTER TABLE conversations 
ADD UNIQUE INDEX IF NOT EXISTS uk_user_pair_active (user_id_1, user_id_2, status);

-- 3. 为 messages 表添加商品和订单ID字段
ALTER TABLE messages 
ADD COLUMN IF NOT EXISTS commodity_id VARCHAR(50) COMMENT '商品ID（实时查询）',
ADD COLUMN IF NOT EXISTS order_id VARCHAR(50) COMMENT '订单ID（实时查询）';

-- 4. 添加索引
CREATE INDEX IF NOT EXISTS idx_messages_commodity_id ON messages(commodity_id);
CREATE INDEX IF NOT EXISTS idx_messages_order_id ON messages(order_id);
```

## 总结：需要修改的文件

### 必须修改
1. ✅ **数据库迁移脚本**: 删除 `conversations.commodity_id` 和 `order_id`，添加唯一约束
2. ✅ **Message.java**: 添加 `commodity_id` 和 `order_id` 字段
3. ✅ **ContactServiceImpl.java**: 
   - 修改 `getOrCreateConversation`（移除 commodityId/orderId 参数）
   - 修改 `sendMessage`（支持订单卡片发送）
4. ✅ **ContactController.java**: 修改 `createConversation`（移除参数）
5. ✅ **ContactService.java**: 修改接口方法签名

### 可能需要修改（取决于数据库当前状态）
6. ⚠️ **Conversation.java**: 
   - 如果数据库仍使用 `buyer_id`/`seller_id`，需要将实体类改回此结构
   - 如果已迁移到 `user_id_1`/`user_id_2`，则无需修改
7. ⚠️ **ConversationRepository.java**: 
   - 根据数据库结构调整查询方法

### 无需修改
8. ✅ **SendMessageRequest.java**: 已包含所需字段，无需修改

## 风险评估

1. **数据一致性**: 删除 `commodity_id` 和 `order_id` 前需要确保没有业务逻辑依赖这些字段
2. **重复对话处理**: 需要清理可能存在的重复对话数据
3. **向后兼容**: 如果前端仍传递 `commodityId` 和 `orderId` 到 `createConversation`，需要优雅处理（忽略这些参数）

## 建议实施步骤

1. **第一步**: 确认数据库当前结构（`buyer_id`/`seller_id` 还是 `user_id_1`/`user_id_2`）
2. **第二步**: 创建数据库迁移脚本并执行
3. **第三步**: 修改 Entity 层（Message 添加字段，Conversation 确认结构）
4. **第四步**: 修改 Service 层（`getOrCreateConversation` 和 `sendMessage`）
5. **第五步**: 修改 Controller 层（移除 `createConversation` 的 commodityId/orderId 参数）
6. **第六步**: 测试验证（创建对话唯一性、发送订单卡片）

