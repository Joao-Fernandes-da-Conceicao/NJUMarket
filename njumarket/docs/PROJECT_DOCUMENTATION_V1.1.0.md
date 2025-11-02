# 南大集市 NJUMarket v1.1.0 项目文档

## 📋 目录
- [版本概述](#版本概述)
- [核心功能更新](#核心功能更新)
- [性能优化实现](#性能优化实现)
- [实时通信机制](#实时通信机制)
- [增量轮询系统](#增量轮询系统)
- [待实现功能](#待实现功能)
- [v1.1.1 版本优化建议](#v111-版本优化建议)
- [技术架构更新](#技术架构更新)
- [已知问题与限制](#已知问题与限制)

---

## 版本概述

### 版本信息
- **版本**: v1.1.0
- **发布时间**: 2025-01-20
- **基于版本**: v1.0
- **状态**: 已发布，核心优化功能已完成

### 版本定位
v1.1.0 版本专注于**性能优化**、**实时通信**和**数据一致性**的提升，通过批量查询、WebSocket推送、增量轮询等机制，显著改善了用户体验和系统性能。

### 主要成就
- ✅ **N+1查询优化**：对话列表和消息列表查询性能提升 90%+
- ✅ **实时消息推送**：WebSocket实现消息和未读数的实时更新
- ✅ **增量轮询机制**：基于Redis ZSet的智能轮询，减少不必要的数据传输
- ✅ **批量查询接口**：统一商品/订单批量状态查询，避免重复请求
- ✅ **统一UI组件**：未读角标组件化，样式和行为统一

---

## 核心功能更新

### 1. 消息系统优化

#### 1.1 批量查询商品/订单状态

**实现位置**：
- 后端：`CommodityQueryServiceImpl.getCommoditiesBatchStatus()`
- 后端：`OrderServiceImpl.getOrdersBatchStatus()`
- 前端：`Messages.vue.enrichMessages()`

**功能说明**：
- 消息列表中包含商品卡片或订单卡片时，批量查询所有商品/订单的完整信息
- 避免为每个消息单独发起API请求，显著减少网络请求次数

**技术实现**：
```java
// 后端：批量查询商品状态（包含卖家信息）
@Override
public Result getCommoditiesBatchStatus(List<String> commodityIds) {
    // 1. 去重
    Set<String> uniqueIds = new HashSet<>(commodityIds);
    
    // 2. 批量查询商品
    List<Commodity> commodities = commodityRepository.findAllById(uniqueIds);
    
    // 3. 批量查询卖家Profile（避免N+1）
    Set<String> sellerIds = commodities.stream()
        .map(Commodity::getSellerId)
        .collect(Collectors.toSet());
    List<UserProfile> profiles = userProfileRepository.findByUserIdIn(new ArrayList<>(sellerIds));
    
    // 4. 转换为DTO并返回
    return Result.ok("批量查询成功", result);
}
```

```javascript
// 前端：批量查询并填充消息数据
const enrichMessages = async (messageList) => {
  // 收集所有商品ID和订单ID（去重）
  const commodityIds = [...new Set(messageList
    .filter(m => m.commodityId && !m.commodity)
    .map(m => m.commodityId)
  )]
  
  // 并行批量查询
  const [commodityResponse, orderResponse] = await Promise.all([
    commodityAPI.getBatchStatus(commodityIds),
    orderAPI.getBatchStatus(orderIds)
  ])
  
  // 填充消息对象
  messageList.forEach(message => {
    if (message.commodityId) {
      message.commodity = commodityMap.get(message.commodityId)
    }
    if (message.orderId) {
      message.order = orderMap.get(message.orderId)
    }
  })
}
```

**性能提升**：
- **优化前**：20条消息（10条商品卡片，10条订单卡片）= 21次API请求
- **优化后**：2次批量API请求
- **性能提升**：90%+ 请求减少

#### 1.2 统一未读角标组件

**实现位置**：
- 组件：`components/common/UnreadBadge.vue`
- 使用位置：`AppHeader.vue`、`ConversationList.vue`

**功能说明**：
- 统一管理未读角标的样式、渲染和计数逻辑
- 支持数字显示、圆点显示、文本显示三种模式
- 样式和行为完全统一，易于维护

**技术实现**：
```vue
<template>
  <span v-if="shouldShow" :class="badgeClass">
    <template v-if="type === 'number'">{{ displayNumber }}</template>
    <template v-else-if="type === 'dot'">●</template>
    <template v-else-if="type === 'text'">{{ text }}</template>
  </span>
</template>

<script setup>
const props = defineProps({
  count: { type: Number, default: 0 },
  type: { type: String, default: 'number' }, // 'number' | 'dot' | 'text'
  max: { type: Number, default: 99 },
  text: { type: String, default: '' }
})

const shouldShow = computed(() => {
  if (props.type === 'dot') return props.count > 0
  return props.count > 0
})
</script>
```

#### 1.3 未读数实时更新

**实现位置**：
- 后端：`ContactServiceImpl.sendMessage()`
- 后端：`ContactServiceImpl.markConversationAsRead()`
- 前端：`stores/message.js`

**功能说明**：
- 发送消息或标记已读时，通过WebSocket实时推送未读数更新
- 前端全局响应未读数变化，自动更新头部角标和侧边栏角标
- 支持总未读数和单个对话未读数两种更新

---

## 性能优化实现

### 2.1 N+1 查询问题解决方案

#### 2.1.1 对话列表优化

**问题描述**：
- **优化前**：获取20个对话 = 1次查询对话 + 20次查询UserProfile + 20次查询User = **41次查询**
- **查询场景**：每个对话需要查询两个用户的Profile和User信息

**优化方案**：
```java
// 收集所有相关用户ID（去重）
Set<String> userIds = new HashSet<>();
for (Conversation conv : pagedConversations) {
    userIds.add(conv.getUserId1());
    userIds.add(conv.getUserId2());
}

// 批量查询所有UserProfile（1次查询）
List<UserProfile> profiles = userProfileRepository.findByUserIdIn(new ArrayList<>(userIds));
Map<String, UserProfile> profileMap = profiles.stream()
    .collect(Collectors.toMap(UserProfile::getUserId, p -> p));

// 批量查询所有User（1次查询）
List<User> users = userRepository.findAllById(userIds);
Map<String, User> userMap = users.stream()
    .collect(Collectors.toMap(User::getUserId, u -> u));

// 使用Map进行O(1)查找
ConversationDTO dto = convertConversationToDTOWithMap(conversation, userId, profileMap, userMap);
```

**性能提升**：
- **优化后**：1次查询对话 + 1次批量查询Profile + 1次批量查询User = **3次查询**
- **性能提升**：92.7% 查询减少（41次 → 3次）

#### 2.1.2 消息列表优化

**问题描述**：
- **优化前**：获取50条消息 = 1次查询消息 + 50次查询UserProfile = **51次查询**
- **查询场景**：每条消息需要查询发送者的Profile信息

**优化方案**：
```java
// 对话中只有两个用户，只需要批量查询2次Profile
Set<String> userIds = new HashSet<>();
userIds.add(conversation.getUserId1());
userIds.add(conversation.getUserId2());

List<UserProfile> profiles = userProfileRepository.findByUserIdIn(new ArrayList<>(userIds));
Map<String, UserProfile> profileMap = profiles.stream()
    .collect(Collectors.toMap(UserProfile::getUserId, p -> p));
```

**性能提升**：
- **优化后**：1次查询消息 + 1次批量查询Profile = **2次查询**
- **性能提升**：96.1% 查询减少（51次 → 2次）

#### 2.1.3 商品/订单卡片优化

**问题描述**：
- **优化前**：20条消息包含商品卡片 = 1次查询消息 + 20次查询商品 = **21次查询**
- **查询场景**：每个商品卡片需要查询商品详情和卖家Profile

**优化方案**：
- 前端批量收集所有商品ID和订单ID
- 调用批量查询接口，一次获取所有数据
- 使用Map快速匹配并填充消息对象

**性能提升**：
- **优化后**：1次查询消息 + 1次批量查询商品 + 1次批量查询订单 = **3次查询**
- **性能提升**：85.7% 查询减少（21次 → 3次）

### 2.2 批量查询接口设计

#### 2.2.1 商品批量状态查询

**接口**：`GET /api/user/commodity/batch-status?ids=id1,id2,id3`

**返回数据**：
```json
{
  "code": 200,
  "success": true,
  "data": [
    {
      "commodityId": "xxx",
      "title": "商品标题",
      "price": 100.0,
      "commodityStatus": "ON_SHELF",
      "sellerNickname": "卖家昵称",
      "sellerAvatar": "头像URL",
      "images": ["图片1", "图片2"],
      "publishTime": "2025-01-20T10:30:00"
    }
  ]
}
```

**特点**：
- 批量查询，一次返回多个商品完整信息
- 自动包含卖家Profile信息（避免N+1）
- 支持去重和空值处理

#### 2.2.2 订单批量状态查询

**接口**：`GET /api/user/order/batch-status?ids=id1,id2,id3`

**返回数据**：
```json
{
  "code": 200,
  "success": true,
  "data": [
    {
      "orderId": "xxx",
      "orderStatus": "PAID",
      "payAmount": 100.0,
      "commoditySnapshotPrice": 100.0,
      "commoditySnapshotImages": "图片URL"
    }
  ]
}
```

**特点**：
- 批量查询，一次返回多个订单信息
- 包含商品快照信息（价格、图片等）
- 自动权限检查（只返回用户有权限查看的订单）

---

## 实时通信机制

### 3.1 WebSocket 消息推送

#### 3.1.1 消息实时推送

**实现位置**：
- 后端：`ContactServiceImpl.sendMessage()`
- 前端：`utils/websocket.js`、`stores/message.js`

**功能说明**：
- 发送消息后，立即通过WebSocket推送给接收方
- 接收方无需刷新页面即可看到新消息
- 支持消息类型：TEXT、COMMODITY_CARD、ORDER_CARD

**技术实现**：
```java
// 后端：发送消息后推送
MessageDTO messageDTO = convertMessageToDTO(message, userId);

messagingTemplate.convertAndSendToUser(
    receiverId,
    "/queue/message",
    messageDTO
);
```

```javascript
// 前端：接收WebSocket消息
wsClient.onMessage((message) => {
  const data = JSON.parse(message.body)
  if (data.type === 'MESSAGE') {
    messageStore.handleWebSocketMessage(data)
  }
})
```

#### 3.1.2 未读数实时更新

**实现位置**：
- 后端：`ContactServiceImpl.sendMessage()`、`markConversationAsRead()`
- 前端：`stores/message.js.handleUnreadCountUpdate()`

**功能说明**：
- 发送消息或标记已读时，实时推送未读数更新事件
- 前端全局响应，自动更新头部角标和侧边栏角标
- 支持总未读数和单个对话未读数两种更新

**推送格式**：
```json
{
  "type": "UNREAD_COUNT_UPDATE",
  "unreadCount": 5,              // 总未读数（用于顶部栏）
  "conversationId": "CONV_xxx",
  "conversationUnreadCount": 2,  // 单个对话未读数（用于侧边栏）
  "timestamp": "2025-01-20T10:30:00"
}
```

**前端处理**：
```javascript
// 处理未读数更新
handleUnreadCountUpdate(updateData) {
  // 更新总未读数
  if (updateData.unreadCount !== undefined) {
    this.totalUnreadCount = updateData.unreadCount
  }
  
  // 更新单个对话未读数
  const conversation = this.conversations.find(
    c => c.conversationId === updateData.conversationId
  )
  if (conversation) {
    conversation.unreadCount = updateData.conversationUnreadCount || 0
  }
}
```

#### 3.1.3 WebSocket连接管理

**实现特点**：
- 使用SockJS + STOMP协议
- 自动重连机制（前端实现）
- 连接状态监控
- 异常处理和日志记录

**前端实现**：
```javascript
// WebSocket客户端
class WebSocketClient {
  connect() {
    this.socket = new SockJS('/ws')
    this.stompClient = Stomp.over(this.socket)
    
    this.stompClient.connect({}, (frame) => {
      // 订阅用户专属队列
      this.stompClient.subscribe(`/user/${userId}/queue/message`, (message) => {
        this.handleMessage(message)
      })
    })
  }
}
```

---

## 增量轮询系统

### 4.1 系统设计

#### 4.1.1 需求背景与应用范围

**问题场景**：
- 聊天界面显示商品/订单卡片
- "咨询商品"对话框中的商品列表需要实时更新
- "咨询订单"对话框中的订单列表需要实时更新
- 商品价格、状态可能实时变化
- 订单状态可能实时更新
- 用户轮询间隔内，可能错过更新

**解决方案**：
- **全量轮询**：每次查询所有商品/订单（浪费资源）
- **增量轮询**：只查询上次轮询后的变更（高效）

**应用范围**：
增量轮询机制应用于以下三个场景，确保数据的实时性和一致性：

1. **消息记录中的商品/订单卡片**
   - 位置：消息列表（`Messages.vue`）
   - 场景：消息中包含商品卡片或订单卡片时，卡片数据需要实时更新
   - 更新方式：增量轮询结果直接更新 `message.commodity` 和 `message.order` 对象

2. **"咨询商品"对话框的商品列表**
   - 位置：商品选择对话框（`SelectCommodityOrOrderDialog.vue`，type="commodity"）
   - 场景：用户点击"咨询商品"按钮，打开对话框选择商品附件时，列表中的商品信息需要保持最新
   - 更新方式：增量轮询结果更新对话框中的商品列表项（`commodityList`）

3. **"咨询订单"对话框的订单列表**
   - 位置：订单选择对话框（`SelectCommodityOrOrderDialog.vue`，type="order"）
   - 场景：用户点击"咨询订单"按钮，打开对话框选择订单附件时，列表中的订单信息需要保持最新
   - 更新方式：增量轮询结果更新对话框中的订单列表项（`orderList`）

**统一更新机制**：
- 所有三个场景共享同一个增量轮询机制
- 轮询结果统一处理，同时更新消息卡片和对话框列表
- 确保用户在任意场景下都能看到最新的商品/订单数据

#### 4.1.2 架构设计

**后端存储**：
- Redis Sorted Set (ZSet)
- 时间分片键：`chat:commodity:changes:yyyy-MM-dd:HH`
- Score：Unix时间戳（秒）
- Value：JSON格式变更记录

**前端轮询**：
- 定期轮询：30秒间隔
- 强制轮询：收到新消息卡片时立即轮询
- 时间戳管理：localStorage存储上次轮询时间（UTC格式）

**数据流程**：
```
商品状态变更 → 记录到Redis ZSet → 前端轮询 → 批量查询完整数据 → 更新消息卡片/选择对话框列表
```

**更新目标**：
1. **消息卡片**：更新消息列表中的商品/订单卡片数据
2. **选择对话框**：更新"咨询商品"和"咨询订单"对话框中的列表数据

#### 4.1.3 时间分片策略

**设计思路**：
- 按小时分片，减少单个键的数据量
- 每个时间片独立TTL（7天）
- 查询时合并多个时间片的数据

**实现细节**：
```java
// 获取时间片键
private String getTimeSliceKey(LocalDateTime timestamp, String prefix) {
    String dateHour = timestamp.format(DateTimeFormatter.ofPattern("yyyy-MM-dd:HH"));
    return prefix + ":" + dateHour;
}

// 查询时需要查询多个时间片（从afterTimestamp到当前时间）
List<String> timeSliceKeys = getTimeSliceKeysBetween(afterTimestamp, LocalDateTime.now());
```

**优势**：
- 单个键数据量可控（每小时最多数千条）
- TTL自动清理，无需手动删除
- 查询效率高（只需要查询相关时间片）

#### 4.1.4 变更记录格式

**商品变更记录**：
```json
{
  "commodityId": "COMMODITY_xxx",
  "operation": "UPDATE",
  "timestamp": "2025-01-20T10:30:00.123456789"
}
```

**订单变更记录**：
```json
{
  "orderId": "ORDER_xxx",
  "operation": "UPDATE",
  "timestamp": "2025-01-20T10:30:00.123456789"
}
```

**操作类型**：
- `UPDATE`：更新（包括价格、状态、库存等）
- `DELETE`：删除（商品下架、订单取消等）

### 4.2 后端实现

#### 4.2.1 变更记录服务

**实现位置**：`ChangeRecordServiceImpl`

**核心方法**：
```java
// 记录商品变更
void recordCommodityChange(String commodityId, String operation, LocalDateTime timestamp)

// 记录订单变更
void recordOrderChange(String orderId, String operation, LocalDateTime timestamp)

// 查询商品变更（afterTimestamp之后）
List<String> getCommodityChangesAfter(LocalDateTime afterTimestamp)

// 查询订单变更（afterTimestamp之后）
List<String> getOrderChangesAfter(LocalDateTime afterTimestamp)
```

**查询优化**：
```java
// 1. ZSet层面过滤：使用exclusiveMinScore避免边界重复
long minScore = afterTimestamp.toEpochSecond(ZoneOffset.UTC);
long exclusiveMinScore = minScore + 1;
Set<Object> records = redisTemplate.opsForZSet()
    .rangeByScore(timeSliceKey, exclusiveMinScore, Double.MAX_VALUE);

// 2. 应用层精确过滤：解析JSON中的timestamp字段，处理纳秒级精度
for (String record : allRecords) {
    ObjectMapper mapper = new ObjectMapper();
    JsonNode jsonNode = mapper.readTree(record);
    LocalDateTime recordTimestamp = parseTimestamp(jsonNode.get("timestamp").asText());
    if (recordTimestamp.isAfter(afterTimestamp)) {
        result.add(record);
    }
}
```

#### 4.2.2 增量更新接口

**实现位置**：`ChatDataController.getIncrementalUpdate()`

**接口**：`GET /api/chat/incremental-update?lastPollTimestamp=2025-01-20T10:30:00.000Z`

**处理流程**：
1. 解析时间戳（支持多种ISO 8601格式）
2. 转换为系统时区（GMT+8）
3. 查询Redis ZSet获取变更记录
4. 提取商品ID和订单ID列表
5. 批量查询完整数据
6. 返回变更数据

**返回格式**：
```json
{
  "code": 200,
  "success": true,
  "data": {
    "commodities": [
      {
        "commodityId": "xxx",
        "title": "商品标题",
        "price": 100.0,
        "commodityStatus": "ON_SHELF",
        "sellerNickname": "卖家昵称",
        "sellerAvatar": "头像URL",
        "images": ["图片1", "图片2"]
      }
    ],
    "orders": [
      {
        "orderId": "xxx",
        "orderStatus": "PAID",
        "payAmount": 100.0,
        "commoditySnapshotPrice": 100.0
      }
    ]
  }
}
```

### 4.3 前端实现

#### 4.3.1 轮询机制

**实现位置**：`Messages.vue`

**定期轮询**：
```javascript
const POLL_INTERVAL = 30000 // 30秒

const startPolling = () => {
  pollTimer = setInterval(async () => {
    if (messages.value && messages.value.length > 0 && 
        window.location.pathname.startsWith('/messages') &&
        !isPolling) {
      await incrementalPoll(false) // 非强制轮询
    }
  }, POLL_INTERVAL)
}
```

**强制轮询**：
```javascript
// 收到新消息卡片时，立即强制轮询
const forceIncrementalPoll = async () => {
  if (isPolling) return
  
  try {
    isPolling = true
    await incrementalPoll(true) // 强制轮询
  } finally {
    isPolling = false
  }
}
```

#### 4.3.2 时间戳管理

**存储位置**：localStorage

**实现方式**：
```javascript
const LAST_POLL_TIMESTAMP_KEY = 'chat_last_poll_timestamp'

const getLastPollTimestamp = () => {
  const timestamp = localStorage.getItem(LAST_POLL_TIMESTAMP_KEY)
  return timestamp || new Date().toISOString() // UTC格式
}

const updateLastPollTimestamp = () => {
  localStorage.setItem(LAST_POLL_TIMESTAMP_KEY, new Date().toISOString())
}
```

**时区处理**：
- 前端存储UTC时间戳（`toISOString()`）
- 后端接收后转换为系统时区（GMT+8）进行比较
- 确保时区一致性

#### 4.3.3 增量更新处理

**实现流程**：
```javascript
const incrementalPoll = async (force = false) => {
  const lastTimestamp = getLastPollTimestamp()
  const response = await chatAPI.getIncrementalUpdate(lastTimestamp)
  
  if (response.success && response.data) {
    const { commodities = [], orders = [] } = response.data
    
    if (commodities.length > 0 || orders.length > 0) {
      // 增量更新前端数据（消息卡片 + 选择对话框列表）
      updateCommoditiesAndOrders(commodities, orders)
      
      // 更新时间戳
      updateLastPollTimestamp()
    }
  }
}

const updateCommoditiesAndOrders = (commodities, orders) => {
  const commodityMap = new Map(commodities.map(c => [c.commodityId, c]))
  const orderMap = new Map(orders.map(o => [o.orderId, o]))
  
  // 1. 更新消息列表中的商品/订单卡片
  messages.value.forEach((message) => {
    if (message.commodityId && commodityMap.has(message.commodityId)) {
      message.commodity = commodityMap.get(message.commodityId)
    }
    if (message.orderId && orderMap.has(message.orderId)) {
      message.order = orderMap.get(message.orderId)
    }
  })
  
  // 2. 更新"咨询商品"对话框中的商品列表
  if (commodityDialogVisible.value && commodityList.value) {
    commodityList.value.forEach((commodity) => {
      if (commodityMap.has(commodity.commodityId)) {
        Object.assign(commodity, commodityMap.get(commodity.commodityId))
      }
    })
  }
  
  // 3. 更新"咨询订单"对话框中的订单列表
  if (orderDialogVisible.value && orderList.value) {
    orderList.value.forEach((order) => {
      if (orderMap.has(order.orderId)) {
        Object.assign(order, orderMap.get(order.orderId))
      }
    })
  }
}
```

### 4.4 性能与优化

**性能指标**：
- 轮询间隔：30秒（可配置）
- 时间分片：按小时分片，单键数据量可控
- TTL管理：7天自动过期，无需手动清理
- 查询效率：ZSet查询 + 应用层过滤，双重保证

**优化措施**：
1. **时间分片**：减少单键数据量，提高查询效率
2. **双重过滤**：ZSet层面 + 应用层过滤，避免重复记录
3. **批量查询**：变更记录ID批量查询完整数据
4. **增量更新**：统一更新消息卡片和选择对话框列表，不影响新消息加载
5. **多场景应用**：增量轮询结果同时应用于消息卡片、商品选择列表、订单选择列表

---

## 待实现功能

### 5.1 订单超卖控制（暂缓）

**问题描述**：
- 当前实现：库存检查和扣减分为两步，存在并发竞争风险
- 场景：两个用户同时下单，可能都通过库存检查，导致超卖

**风险等级**：高（但在当前学习项目场景下可接受）

**暂缓原因**：
- 学习项目场景，并发下单情况较少
- 需要引入数据库锁或分布式锁，增加系统复杂度
- 优先保证核心功能稳定性

**待实现方案**：
1. **数据库行锁（悲观锁）**：
   ```java
   @Lock(LockModeType.PESSIMISTIC_WRITE)
   @Query("SELECT c FROM Commodity c WHERE c.commodityId = :commodityId")
   Optional<Commodity> findByIdForUpdate(@Param("commodityId") String commodityId);
   ```

2. **乐观锁（版本号机制）**：
   ```java
   @Entity
   public class Commodity {
       @Version
       private Long version; // 乐观锁版本号
   }
   ```

3. **Redis分布式锁**（高并发场景）：
   ```java
   RLock lock = redissonClient.getLock("commodity:" + commodityId);
   if (lock.tryLock(10, 30, TimeUnit.SECONDS)) {
       // 检查库存并扣减
   }
   ```

**预计实现版本**：v1.2.0

---

## v1.1.1 版本优化建议

### 6.1 N+1 查询优化改进

#### 6.1.1 对话列表数据库分页优化 ✅ **已完成**

**问题描述**：
- **位置**：`ContactServiceImpl.getConversations()`
- **问题**：先查询所有对话到内存，再手动分页
- **影响**：对话数量大时，内存占用高，查询慢

**已实现方案**：
```java
// 改为数据库分页
Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "lastMessageTime"));
Page<Conversation> conversationsPage = conversationRepository.findByUserIdAndStatus(userId, "ACTIVE", pageable);
List<Conversation> pagedConversations = conversationsPage.getContent();
```

**实现效果**：
- ✅ 内存占用降低 90%+（只加载当前页数据）
- ✅ 查询速度提升（数据库索引优化，利用 `lastMessageTime` 索引）
- ✅ 批量查询边界处理优化（添加空列表检查）

**代码位置**：
- 文件：`ContactServiceImpl.getConversations()`
- 实现日期：2025-01-20

#### 6.1.2 批量查询边界处理 ✅ **已完成**

**问题描述**：
- **位置**：批量查询UserProfile时，如果用户ID列表为空，仍会执行查询
- **问题**：可能执行不必要的空查询

**已实现方案**：
```java
final Map<String, UserProfile> profileMap;
if (!userIds.isEmpty()) {
    List<UserProfile> profiles = userProfileRepository.findByUserIdIn(new ArrayList<>(userIds));
    profileMap = profiles.stream()
        .collect(Collectors.toMap(UserProfile::getUserId, p -> p));
} else {
    profileMap = new HashMap<>();
}
```

**代码位置**：
- 文件：`ContactServiceImpl.getConversations()`
- 实现日期：2025-01-20

#### 6.1.3 缓存机制引入（后续版本）

**状态**：⏸️ **暂缓实现，规划至 v1.2.0 版本**

**问题**：每次查询对话列表都查询UserProfile，即使数据未变化

**优化方案**：
- **缓存策略**：Cache-Aside模式
- **缓存内容**：UserProfile数据（TTL：10分钟）
- **缓存更新**：用户资料更新时，主动失效缓存
- **缓存预热**：系统启动时预热热点数据

```java
@Cacheable(value = "userProfile", key = "#userId", unless = "#result == null")
public UserProfile getUserProfile(String userId) {
    return userProfileRepository.findByUserId(userId);
}

@CacheEvict(value = "userProfile", key = "#userId")
public void updateUserProfile(String userId, UserProfileDTO dto) {
    // 更新逻辑
}
```

**预期提升**：
- UserProfile查询减少 80%+
- 响应速度提升 50%+

**暂缓原因**：
- 当前系统性能已满足需求，通过批量查询已解决主要性能瓶颈
- 缓存机制涉及缓存一致性、缓存穿透/击穿/雪崩等复杂问题，需要更多时间设计和测试
- 优先完成核心功能优化，缓存机制作为后续性能优化的一部分

### 6.2 WebSocket推送优化

#### 6.2.1 断线重连机制实现方案 ✅ **已完成**

**实现位置与策略**：

**核心原则**：**前端实现重连机制，后端提供连接状态检测**

**为什么前端实现重连？**
1. **感知层面**：连接断开是客户端的直接感知，前端能第一时间检测到
2. **响应速度**：前端可以立即响应断开事件，无需等待后端检测
3. **用户体验**：前端可以显示重连状态、提供重连按钮等
4. **资源效率**：避免后端维护大量连接状态，减轻服务器负担

**生产环境最佳实践**：

**1. 前端重连策略（主要实现）**

```javascript
class WebSocketClient {
  constructor() {
    this.reconnectAttempts = 0
    this.maxReconnectAttempts = Infinity // 生产环境：无限重连，但限制频率
    this.reconnectDelay = 1000
    this.maxReconnectDelay = 60000 // 最大延迟60秒
    this.reconnectTimer = null
    this.pendingMessages = [] // 断线期间的消息队列
    this.lastHeartbeatTime = null
    this.heartbeatTimeout = 30000 // 30秒无心跳则认为断开
  }
  
  /**
   * 智能重连策略
   * - 指数退避：1s, 2s, 4s, 8s, 16s, 32s, 60s（最大）
   * - 页面可见性检测：页面隐藏时暂停重连，显示时立即重连
   * - 网络状态检测：离线时不重连，上线时立即重连
   */
  scheduleReconnect() {
    // 清除之前的定时器
    if (this.reconnectTimer) {
      clearTimeout(this.reconnectTimer)
    }
    
    // 检查是否应该重连
    if (!this.shouldReconnect()) {
      return
    }
    
    // 指数退避计算延迟
    const delay = Math.min(
      this.reconnectDelay * Math.pow(2, this.reconnectAttempts),
      this.maxReconnectDelay
    )
    
    this.reconnectAttempts++
    
    console.log(`WebSocket reconnecting in ${delay}ms (attempt ${this.reconnectAttempts})`)
    
    this.reconnectTimer = setTimeout(() => {
      if (!this.isConnectedState()) {
        this.connect()
      }
    }, delay)
  }
  
  /**
   * 判断是否应该重连
   */
  shouldReconnect() {
    // 页面不可见时，暂停重连（节省资源）
    if (document.hidden) {
      console.log('Page hidden, pausing reconnect')
      return false
    }
    
    // 网络离线时，不重连（等待网络恢复）
    if (!navigator.onLine) {
      console.log('Network offline, waiting for connection')
      return false
    }
    
    return true
  }
  
  /**
   * 页面可见性变化处理
   */
  setupVisibilityHandler() {
    document.addEventListener('visibilitychange', () => {
      if (!document.hidden && !this.isConnectedState()) {
        // 页面从隐藏变为可见，且连接断开，立即重连
        this.reconnectAttempts = 0 // 重置重连计数
        this.connect()
      }
    })
    
    // 网络状态变化处理
    window.addEventListener('online', () => {
      if (!this.isConnectedState()) {
        console.log('Network online, reconnecting WebSocket')
        this.reconnectAttempts = 0
        this.connect()
      }
    })
    
    window.addEventListener('offline', () => {
      console.log('Network offline, WebSocket will reconnect when online')
    })
  }
  
  /**
   * 心跳超时检测
   */
  setupHeartbeatMonitor() {
    // STOMP客户端已有心跳，这里添加超时检测
    setInterval(() => {
      if (this.isConnectedState()) {
        const now = Date.now()
        if (this.lastHeartbeatTime && (now - this.lastHeartbeatTime > this.heartbeatTimeout)) {
          console.warn('Heartbeat timeout, reconnecting...')
          this.disconnect()
          this.scheduleReconnect()
        }
      }
    }, 5000) // 每5秒检查一次
  }
  
  /**
   * 重连成功后处理
   */
  onReconnectSuccess() {
    // 1. 重置重连计数
    this.reconnectAttempts = 0
    
    // 2. 拉取断线期间错过的消息
    this.fetchMissedMessages()
    
    // 3. 重新订阅所有队列
    this.subscribeToMessages()
    
    // 4. 触发重连成功事件
    this.emit('reconnected')
  }
  
  /**
   * 获取断线期间的消息
   */
  async fetchMissedMessages() {
    // 调用后端API，获取断线期间的新消息
    try {
      const messageStore = await import('../stores/message')
      const { useMessageStore } = messageStore
      const store = useMessageStore()
      
      // 重新获取对话列表和未读数
      await store.fetchConversations(0, 20)
      
      // 如果当前有打开的对话，重新加载消息
      if (store.selectedConversationId) {
        await store.fetchMessages(store.selectedConversationId, 0, 50)
      }
    } catch (error) {
      console.error('Failed to fetch missed messages:', error)
    }
  }
}
```

**2. 后端连接状态监控（辅助实现）**

```java
/**
 * WebSocket连接监控服务
 * 监控连接状态，处理异常断开
 */
@Component
public class WebSocketMonitorService {
    
    private final SimpMessagingTemplate messagingTemplate;
    
    /**
     * 检测并处理异常断开的连接
     * 定期任务：每5分钟检查一次
     */
    @Scheduled(fixedRate = 300000) // 5分钟
    public void checkStaleConnections() {
        // 检查所有活跃连接，移除超时的连接
        // Spring WebSocket会自动清理，这里主要用于日志和告警
        log.info("Checking WebSocket connections...");
    }
    
    /**
     * 连接建立时的处理
     */
    @EventListener
    public void handleSessionConnected(SessionConnectedEvent event) {
        String userId = getUserIdFromEvent(event);
        log.info("WebSocket connected: userId={}", userId);
        
        // 可选：发送连接确认消息
        messagingTemplate.convertAndSendToUser(
            userId,
            "/queue/connection",
            Map.of("type", "CONNECTION_CONFIRMED", "timestamp", LocalDateTime.now())
        );
    }
    
    /**
     * 连接断开时的处理
     */
    @EventListener
    public void handleSessionDisconnect(SessionDisconnectEvent event) {
        String userId = getUserIdFromEvent(event);
        log.info("WebSocket disconnected: userId={}, reason={}", 
            userId, event.getCloseStatus());
        
        // 记录断开原因，用于分析连接稳定性
        if (event.getCloseStatus() != null && 
            !event.getCloseStatus().equals(CloseStatus.NORMAL)) {
            log.warn("Abnormal WebSocket disconnect: userId={}, status={}", 
                userId, event.getCloseStatus());
        }
    }
}
```

**3. 生产环境完整方案**

**前端实现要点**：
- ✅ **指数退避重连**：1s → 2s → 4s → 8s → 16s → 32s → 60s（最大）
- ✅ **无限重连**：生产环境不限制重连次数，但限制重连频率（避免频繁请求）
- ✅ **页面可见性检测**：页面隐藏时暂停重连，显示时立即重连
- ✅ **网络状态检测**：离线时不重连，上线时立即重连
- ✅ **心跳超时检测**：长时间无心跳主动断开重连
- ✅ **消息补偿机制**：重连成功后拉取断线期间的消息

**后端辅助功能**：
- ✅ **连接状态监控**：记录连接/断开事件，用于分析和告警
- ✅ **连接确认消息**：连接建立时发送确认，帮助前端验证连接
- ✅ **异常断开日志**：记录异常断开，用于排查问题

**为什么这样设计更贴合生产环境？**

1. **前端主动重连**：
   - 客户端能立即感知断开，响应更快
   - 减少后端压力，避免后端维护大量连接状态
   - 用户体验更好（可以显示重连进度）

2. **智能重连策略**：
   - **页面可见性检测**：节省资源，用户不在时不需要重连
   - **网络状态检测**：避免无效重连尝试
   - **指数退避**：避免服务器压力过大

3. **消息补偿机制**：
   - 重连成功后主动拉取错过的消息
   - 确保消息不丢失

4. **后端监控辅助**：
   - 记录连接事件，用于运维分析
   - 不参与重连逻辑，只负责监控和日志

#### 6.2.2 消息推送失败重试 ✅ **已实现并测试成功**

**测试状态**：✅ 2025-11-02 测试通过
- 用户离线场景：消息正确加入重试队列，按指数退避策略重试（5s → 10s → 20s）
- 重试上限测试：超过3次重试后正确移除消息
- 在线检测：用户上线后立即推送成功

**实现依据**：
- 推送失败的原因可能是用户下线（不需要重试）或网络信号波动（需要重试）
- 聊天功能对实时性要求不高，可以接受适度的重试延迟
- 使用指数退避策略，避免过于频繁的重试

**实现方案**：
```java
// 1. 推送前检查用户是否在线
public void pushWithRetry(String receiverId, Object messageData, String messageType) {
    boolean isOnline = webSocketEventListener.isUserOnline(receiverId);
    
    if (!isOnline) {
        // 用户不在线，记录到重试队列（指数退避：5s, 10s, 20s，最多3次）
        addToRetryQueue(receiverId, messageData, messageType);
        return;
    }
    
    // 用户在线，立即推送
    messagingTemplate.convertAndSendToUser(receiverId, "/queue/message", messageData);
}

// 2. 定时任务重试（每5秒执行一次）
@Scheduled(fixedRate = 5000)
public void retryFailedMessages() {
    // 查询所有应该重试的消息（按nextRetryTime排序）
    Set<Object> messagesToRetry = redisTemplate.opsForZSet()
        .rangeByScore(RETRY_QUEUE_KEY, 0, currentTime);
    
    for (Object msgObj : messagesToRetry) {
        RetryMessageDTO retryMsg = parseRetryMessage(msgObj);
        
        // 检查用户是否上线
        if (webSocketEventListener.isUserOnline(retryMsg.getReceiverId())) {
            // 用户上线，重试推送
            messagingTemplate.convertAndSendToUser(...);
            // 推送成功，从队列移除
        } else {
            // 用户仍离线，更新下次重试时间（指数退避）
            retryMsg.incrementRetry();
            if (retryMsg.canRetry()) {
                // 重新加入队列，等待下次重试
            } else {
                // 超过最大重试次数（3次），移除
            }
        }
    }
}
```

**重试策略**：
- **重试次数上限**：3次（不高，符合生产需求）
- **指数退避间隔**：5秒 → 10秒 → 20秒
- **消息过期时间**：30分钟（避免队列无限增长）
- **在线检测**：重试前检查用户是否在线，避免无效重试

**实现效果**：
- ✅ 网络波动导致的推送失败可以自动恢复
- ✅ 用户短暂离线后重新上线，可以收到错过的消息
- ✅ 用户真正下线时，不会无意义地重试，节省资源
- ✅ 重试次数有限，避免系统负担

**代码位置**：
- 服务接口：`WebSocketRetryService`
- 服务实现：`WebSocketRetryServiceImpl`
- 集成位置：`ContactServiceImpl.sendMessage()`、`ContactServiceImpl.markConversationAsRead()`

### 6.3 增量轮询优化

#### 6.3.1 轮询策略优化

**当前不足**：
- 固定30秒轮询，不够灵活
- 页面不可见时仍在轮询，浪费资源

**优化方案**：
```javascript
// 1. 自适应轮询间隔
let pollInterval = 30000 // 基础30秒

// 如果有变更，缩短间隔；无变更，延长间隔
const adjustPollInterval = (hasChanges) => {
  if (hasChanges) {
    pollInterval = Math.max(10000, pollInterval * 0.8) // 最短10秒
  } else {
    pollInterval = Math.min(60000, pollInterval * 1.2) // 最长60秒
  }
}

// 2. 页面可见性检测
document.addEventListener('visibilitychange', () => {
  if (document.hidden) {
    stopPolling() // 页面不可见时停止轮询
  } else {
    startPolling() // 页面可见时恢复轮询
    forceIncrementalPoll() // 立即轮询一次
  }
})
```

#### 6.3.2 Redis查询优化（待优化）

**当前实现分析**：
- ✅ **已经可以定位到正确的小时**：时间片按小时分片（格式：`yyyy-MM-dd:HH`）
- ✅ **大多数情况下只查询1个时间片**：由于轮询间隔是30秒，`afterTimestamp` 和当前时间通常在同一小时内
- ⚠️ **跨小时边界的情况**：如果上次轮询在13:59:30，当前在14:00:00，会查询2个时间片（13点和14点），这是正确的

**实际场景示例**：
```java
// 场景1：同一小时内（最常见，30秒间隔）
afterTimestamp = 14:30:00
now = 14:30:30
→ 只需查询1个时间片：chat:commodity:changes:2025-01-20:14 ✅

// 场景2：跨小时边界（较少见）
afterTimestamp = 13:59:45
now = 14:00:15
→ 需要查询2个时间片：13点和14点 ✅（正确，因为数据可能分布在这两个小时内）

// 场景3：长时间未轮询（异常情况）
afterTimestamp = 10:30:00（3小时前）
now = 14:30:00
→ 需要查询4个时间片：10、11、12、13、14 ✅（正确，因为数据可能在这4个小时内）
```

**优化方向**（可选，性能提升有限）：
```java
// 1. 快速路径：同一小时内直接查询单个时间片
private List<String> getTimeSliceKeysOptimized(LocalDateTime startTime, LocalDateTime endTime, String prefix) {
    // 检查是否在同一小时内
    if (startTime.getHour() == endTime.getHour() && 
        startTime.toLocalDate().equals(endTime.toLocalDate())) {
        // 快速路径：只查询1个时间片
        return Collections.singletonList(
            prefix + startTime.withMinute(0).withSecond(0).withNano(0)
                .format(TIME_SLICE_FORMATTER)
        );
    }
    
    // 慢速路径：跨小时或多天，查询多个时间片
    return getTimeSliceKeys(startTime, endTime, prefix);
}

// 2. 缓存时间片存在性（避免查询不存在的key）
// 注：当前代码已经用 hasKey() 检查，这个优化意义不大
```

**TTL滚动窗口机制**：

✅ **是的，TTL设置为24小时，确实是滚动窗口**：

```java
// TTL：24小时（86400秒）
private static final long TTL_SECONDS = 24 * 60 * 60L;

// 每次添加数据时更新当前时间片的TTL
redisTemplate.expire(timeSliceKey, Duration.ofSeconds(TTL_SECONDS));
```

**滚动窗口工作原理**：
1. **TTL更新策略**：每次向时间片写入数据时，将该时间片的TTL重置为24小时
2. **自动过期**：如果某个时间片在24小时内不再有新的数据写入，TTL开始倒计时，24小时后自动过期
3. **滚动特性**：数据保留最近24小时，超过24小时的数据自动清理，无需手动删除

**示例时间线**：
```
今天 14:00 - 数据写入 2025-01-20:14 时间片，TTL = 24小时（到明天14:00）
今天 15:00 - 数据写入 2025-01-20:15 时间片，TTL = 24小时（到明天15:00）
...
明天 14:00 - 2025-01-20:14 时间片TTL到期，自动删除
明天 15:00 - 2025-01-20:15 时间片TTL到期，自动删除
```

**跨日查询的影响**：
- ✅ **可以跨日查询**：比如今天查询昨天的数据（在24小时内）
- ⚠️ **但有时间限制**：只能查询最近24小时内的数据
- ❌ **超过24小时的数据无法查询**：因为时间片已经过期删除

**实际场景**：
```java
// 场景1：同一天内查询（完全支持）
afterTimestamp = 今天 10:00
now = 今天 14:00
→ 可以查询，时间片都在24小时内 ✅

// 场景2：跨日查询，在24小时内（支持）
afterTimestamp = 昨天 15:00
now = 今天 14:00（23小时后）
→ 可以查询，昨天15:00的时间片还未过期 ✅

// 场景3：跨日查询，超过24小时（不支持）
afterTimestamp = 昨天 10:00
now = 今天 14:00（28小时后）
→ 部分时间片已过期，只能查询未过期的部分 ⚠️
```

**结论**：
- 当前实现是**滚动窗口机制**，保留最近24小时的数据
- **可以跨日查询**，但只能查询24小时内的数据
- TTL自动过期机制保证了内存不会无限增长，无需手动清理旧数据

**重要说明：增量更新的实际用途**：
- ✅ **增量更新仅用于页面保持期间的实时更新**：当页面打开时，每30秒轮询一次，更新消息中商品/订单的状态
- ✅ **页面刷新时进行全量更新**：
  - 刷新时调用 `fetchConversations()` 全量加载对话列表
  - 选择对话时调用 `fetchMessages()` 全量加载消息
  - 消息中的商品/订单信息通过 `enrichMessages()` 批量查询（全量，不依赖增量轮询）
- ✅ **"超过24小时的数据无法查询"不是问题**：
  - 因为增量轮询只用于页面保持期间的实时更新，不用于加载历史数据
  - 刷新时会重新全量加载所有数据（包括历史消息和商品/订单信息）
  - 正常使用场景下，页面不会保持打开超过24小时
  - 即使超过24小时，刷新页面即可获取最新数据

**设计合理性**：
- 24小时TTL完全满足实际需求：增量轮询间隔30秒，即使页面保持1小时，也只查询20次
- 滚动窗口机制既保证了实时性，又控制了内存使用
- 全量+增量双重保障：刷新时全量加载保证数据完整性，页面保持时增量更新保证实时性

#### 6.3.3 变更记录去重优化（暂缓实现，收益有限）

**当前实现分析**：
- ✅ **ZSet本身具有去重特性**：相同的member（JSON记录）会被覆盖，score会更新为最新值
- ✅ **前端已做去重**：轮询结果使用Set去重，重复的commodityId/orderId只处理一次
- ✅ **个人用户更新频率低**：正常用户很少在5分钟内多次编辑同一商品

**正常用户行为模式**：
```
场景1：编辑商品流程（最常见）
用户编辑商品 → 保存 → 上架
→ 产生2条记录：UPDATE（编辑）和 SHELF（上架）
→ 这些是不同操作，应该记录 ✅

场景2：多次编辑（少见）
用户编辑商品 → 保存（UPDATE）
→ 再次编辑 → 保存（UPDATE）
→ 通常间隔超过5分钟，不会触发去重 ✅

场景3：频繁操作（极少见）
用户快速保存多次 → 5分钟内多次UPDATE
→ 这种情况下去重才有意义
→ 但实际使用中极少发生
```

**去重优化的开销**：
```java
// 额外开销（每次记录变更）：
1. hasKey(recentKey) - O(1)，但需要网络IO
2. set(recentKey, "1", TTL) - O(1)，需要网络IO
3. 更新ZSet记录（如果已存在）- 需要查找旧记录、删除、添加新记录

// 总开销：至少2次Redis操作 + 可能的ZSet更新操作
```

**收益分析**：
- ✅ **减少ZSet存储空间**：避免短时间内重复记录（但TTL只有24小时，影响有限）
- ✅ **减少轮询数据传输**：避免查询到重复记录（但前端已去重，影响很小）
- ⚠️ **实际收益很小**：
  - 正常用户很少5分钟内多次更新同一商品
  - 即使有重复，ZSet的member去重机制会自动覆盖
  - 前端轮询结果也做了去重处理

**结论**：
- ⏸️ **暂缓实现**：对于个人用户的低频更新场景，去重优化的额外开销可能大于收益
- ✅ **当前实现已足够**：ZSet的member去重 + 前端结果去重，已经能处理大部分情况
- 💡 **适用场景**：如果未来有高频批量更新需求（如管理员批量操作），再考虑实现去重优化

#### 6.3.4 选择对话框实时更新 ✅ **已完成**

**实现内容**：
- ✅ **直接复用聊天记录的增量更新机制**：选择对话框与聊天记录共享同一增量轮询结果
- ✅ 当聊天记录进行增量更新时，对话框自动同步更新列表数据
- ✅ 使用 Vue provide/inject 机制，实现父组件（Messages.vue）与子组件（对话框）的数据共享

**实现方案**：
```javascript
// Messages.vue：提供增量更新结果
const incrementalUpdateResult = reactive({
  commodities: [],
  orders: [],
  timestamp: null
})
provide('incrementalUpdateResult', incrementalUpdateResult)

// 在增量轮询完成时更新结果
incrementalUpdateResult.commodities = commodities
incrementalUpdateResult.orders = orders
incrementalUpdateResult.timestamp = Date.now()

// SelectCommodityOrOrderDialog.vue：注入并监听更新结果
const incrementalUpdateResult = inject('incrementalUpdateResult', null)

watch(() => incrementalUpdateResult.timestamp, (newTimestamp) => {
  if (props.modelValue && newTimestamp && items.value.length > 0) {
    updateDialogItemsFromPoll(
      incrementalUpdateResult.commodities || [],
      incrementalUpdateResult.orders || []
    )
  }
})
```

**实现效果**：
- ✅ **统一的增量更新机制**：对话框和聊天记录使用同一个增量轮询结果
- ✅ **自动同步更新**：当聊天记录增量更新时，对话框列表自动更新（价格、状态等字段）
- ✅ **无需额外轮询**：对话框不再独立执行增量轮询，完全复用父组件的机制
- ✅ **数据一致性**：确保对话框和聊天记录显示的数据完全同步

### 6.4 前端状态管理优化

#### 6.4.1 状态同步优化

**当前不足**：
- 未读数在Store和组件中都有，可能不一致
- WebSocket更新和API更新可能冲突

**优化方案**：
```javascript
// 统一状态管理
const useMessageStore = defineStore('message', {
  state: () => ({
    totalUnreadCount: 0,
    conversations: [],
    // 使用Map存储对话未读数，便于快速更新
    conversationUnreadMap: new Map()
  }),
  
  // 统一更新方法
  updateUnreadCount(conversationId, count) {
    if (conversationId) {
      this.conversationUnreadMap.set(conversationId, count)
      // 更新对话对象中的未读数
      const conversation = this.conversations.find(c => c.conversationId === conversationId)
      if (conversation) {
        conversation.unreadCount = count
      }
    }
    
    // 重新计算总未读数
    this.totalUnreadCount = Array.from(this.conversationUnreadMap.values())
      .reduce((sum, count) => sum + count, 0)
  }
})
```

#### 6.4.2 消息缓存优化（后续版本，涉及服务端缓存）

**当前实现**：
- 每次进入对话都调用 `fetchMessages()` 重新加载消息
- 这是合理的：保证数据实时性，避免缓存不一致问题

**缓存优化方案分析**：

**方案1：前端内存缓存（轻量级，用户体验优化）**
```javascript
// 消息缓存（按对话ID，内存缓存）
const messageCache = new Map()

// 加载消息时，先检查缓存
const loadMessages = async (conversationId) => {
  const cached = messageCache.get(conversationId)
  if (cached && cached.timestamp > Date.now() - 60000) { // 1分钟内有效
    messages.value = cached.messages // 立即显示缓存数据
    fetchLatestMessages(conversationId) // 后台更新
  } else {
    messages.value = await fetchMessages(conversationId)
    messageCache.set(conversationId, {
      messages: messages.value,
      timestamp: Date.now()
    })
  }
}
```
- **优点**：提升用户体验，快速显示已加载的消息
- **缺点**：页面刷新后缓存丢失
- **适用**：后续版本（v1.2.0+）考虑实现 ⏸️

**方案2：服务端Redis缓存（性能优化，后续版本）**
```java
// 后端：使用Redis缓存消息列表
@Cacheable(value = "messages", key = "#conversationId")
public List<MessageDTO> getMessages(String conversationId) {
    // 查询数据库
    return messageRepository.findByConversationId(conversationId);
}

@CacheEvict(value = "messages", key = "#conversationId")
public void sendMessage(String conversationId, MessageDTO message) {
    // 发送消息后清除缓存
    messageRepository.save(message);
}
```
- **优点**：减少数据库查询，提升响应速度
- **缺点**：需要处理缓存一致性，增加系统复杂度
- **适用**：v1.2.0+ 性能优化 ⏸️

**版本规划**：
- ⏸️ **v1.1.1**：**不实现消息缓存优化**（无论前端还是后端）
  - v1.1.1定位：专注于用户体验优化（UI交互、实时更新），**不涉及性能优化**
- **v1.2.0+**：考虑实现缓存优化（方案2涉及Redis，方案1涉及前端状态管理）
  - 如需实现，优先考虑前端内存缓存（简单，体验优化）
  - 服务端Redis缓存作为进一步性能优化选项

**当前决策**：
- ⏸️ **暂缓实现**：消息缓存优化（无论前端还是后端）纳入后续版本（v1.2.0或v1.3.0）
- ✅ **v1.1.1定位**：专注于用户体验优化（UI交互、实时更新），**不涉及性能优化**（缓存、索引等）
- 📌 **版本划分**：
  - **v1.1.1**：用户体验优化（UI、交互、实时更新）
  - **v1.2.0+**：性能优化（缓存、索引、并发控制等）

### 6.5 性能监控与日志

#### 6.5.1 性能监控

**优化方案**：
```java
// 使用Spring AOP监控方法执行时间
@Aspect
@Component
public class PerformanceMonitor {
    @Around("@annotation(MonitorPerformance)")
    public Object monitor(ProceedingJoinPoint joinPoint) throws Throwable {
        long start = System.currentTimeMillis();
        try {
            return joinPoint.proceed();
        } finally {
            long duration = System.currentTimeMillis() - start;
            if (duration > 1000) { // 超过1秒记录警告
                log.warn("Slow query: {} took {}ms", joinPoint.getSignature(), duration);
            }
        }
    }
}
```

#### 6.5.2 日志优化

**当前不足**：
- 日志格式不统一
- 缺少关键操作的审计日志

**优化方案**：
```java
// 统一日志格式
log.info("ACTION={}, USER={}, RESOURCE={}, RESULT={}, DURATION={}ms",
    "SEND_MESSAGE", userId, conversationId, "SUCCESS", duration);

// 关键操作审计日志
@AuditLog(action = "SEND_MESSAGE", resource = "CONVERSATION")
public Result sendMessage(String userId, SendMessageRequest request) {
    // 业务逻辑
}
```

---

## 技术架构更新

### 7.1 新增技术组件

- **Redis Sorted Set**：用于存储变更记录
- **WebSocket (Spring)**：实时消息推送
- **前端WebSocket客户端**：SockJS + STOMP

### 7.2 架构模式

- **批量查询模式**：统一批量查询接口，避免N+1问题
- **增量轮询模式**：基于时间戳的增量更新机制
- **实时推送模式**：WebSocket推送 + 轮询补充

---

## 已知问题与限制

### 8.1 时区混用

**问题**：前端使用UTC时间戳，后端使用系统时间（GMT+8）

**影响**：在固定GMT+8部署环境下无影响，但跨时区部署可能有问题

**处理**：已通过转换机制处理，详见`UTC_TIMEZONE_MIXING_ANALYSIS.md`

### 8.2 订单超卖风险

**问题**：库存检查和扣减不是原子操作，存在并发竞争

**影响**：高并发场景下可能超卖

**处理**：已列入待实现功能，预计v1.2.0实现

### 8.3 WebSocket连接稳定性

**问题**：断线重连机制不够完善

**影响**：网络不稳定时可能丢失消息

**处理**：v1.1.1版本优化重连机制和消息重试

---

## 总结

### v1.1.0 核心成就

1. **性能大幅提升**：N+1查询优化，查询次数减少90%+
2. **实时通信完善**：WebSocket推送消息和未读数，用户体验提升
3. **智能轮询机制**：增量轮询减少不必要的数据传输
4. **统一组件系统**：未读角标组件化，易于维护

### 下一步规划

**v1.1.1**：优化现有机制，提升用户体验和稳定性
- ✅ WebSocket推送优化（重连、重试）✅ 已完成
- 增量轮询优化（自适应间隔、页面可见性检测）
- 状态管理优化（统一状态管理，不涉及缓存）
- 前端体验优化（UI交互优化）
- ⏸️ **不涉及性能优化**：缓存、索引等性能优化纳入v1.2.0+

**v1.2.0+**：引入并发控制和性能优化
- **缓存机制优化**
  - UserProfile缓存（Cache-Aside模式）
  - 消息列表缓存（前端内存缓存或服务端Redis缓存）
  - 缓存预热机制
  - 缓存一致性保证
  - 防止缓存穿透/击穿/雪崩
- **并发控制**
  - 订单超卖控制（数据库锁、乐观锁）
  - 库存扣减原子操作
- **数据库优化**
  - 索引优化（常用查询字段）
  - 查询计划优化
- **性能监控和日志系统**
  - 慢查询监控
  - 操作审计日志

**版本定位说明**：
- **v1.1.1**：专注于**用户体验优化**（UI、交互、实时更新），不涉及性能优化
- **v1.2.0+**：专注于**性能优化和系统优化**（缓存、索引、并发控制、监控等）

---

**文档版本**：v1.1.0  
**最后更新**：2025-01-20  
**维护者**：NJUMarket 开发团队

