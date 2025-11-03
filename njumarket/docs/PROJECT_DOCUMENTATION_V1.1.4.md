# 南大集市 NJUMarket v1.1.4 项目文档

## 📋 目录
- [版本概述](#版本概述)
- [核心功能更新](#核心功能更新)
- [技术架构优化](#技术架构优化)
- [数据库设计优化](#数据库设计优化)
- [用户体验优化](#用户体验优化)
- [技术细节](#技术细节)
- [已知问题和后续优化](#已知问题和后续优化)

---

## 版本概述

### 版本信息
- **版本**: v1.1.4
- **发布时间**: 2025-01-XX
- **基于版本**: v1.1.3
- **状态**: 已发布，消息系统核心功能完善完成

### 版本定位
v1.1.4 版本专注于**管理端消息管理功能实现**和**消息系统核心功能完善**，首先实现了完整的后台消息管理系统，然后解决了消息软删除后对话列表显示不准确的问题，实现了已读回执功能，优化了聊天界面的滚动体验。这是一个功能完善和用户体验改进并重的版本。

### 主要成就
- ✅ **管理端消息管理功能**：实现了完整的消息CRUD功能，管理员可以对消息进行查看、编辑和删除
- ✅ **用户级别的最后消息字段**：解决了软删除消息后对话列表显示错误的问题
- ✅ **消息可见性反向作用机制**：完整实现了消息删除对对话最后消息字段的影响
- ✅ **已读回执功能**：实现了WebSocket推送已读通知，发送者可以实时看到消息已读状态
- ✅ **智能滚动优化**：实现了基于滚动位置的智能滚动，避免打断用户查看历史消息
- ✅ **管理端消息字段完善**：管理端支持查看用户级别的最后消息字段

---

## 核心功能更新

### 1. 管理端消息管理功能

#### 1.1 功能概述
实现了完整的管理端消息管理功能，管理员可以对系统中的所有消息进行查看、编辑和删除操作。这是v1.1.4版本首先实现的核心功能。

#### 1.2 实现位置

**后端实现**：
- `AdminServiceImpl.listMessages()` - 查询对话中的消息列表
- `AdminServiceImpl.getMessageById()` - 获取消息详情
- `AdminServiceImpl.updateMessageFull()` - 完整更新消息（包括可见性、已读状态等）
- `AdminServiceImpl.deleteMessage()` - 硬删除消息

**前端实现**：
- `views/Messages.vue` - 会话列表管理
- `views/ConversationDetail.vue` - 会话详情和消息列表

#### 1.3 核心功能

**消息列表查询**：
- 支持分页查询对话中的所有消息
- **不过滤删除状态**：管理端可以看到所有消息，包括双方都删除的消息
- 显示消息的完整信息：发送者、接收者、内容、时间、已读状态、删除状态等
- 统计双方都删除的消息数量

**消息编辑功能**：
- 支持编辑消息的删除状态（`deletedBySender` / `deletedByReceiver`）
- 支持编辑消息的已读状态（`isRead`）
- **智能同步**：修改消息可见性时，自动更新相关用户的最后消息字段

**消息删除功能**：
- 支持硬删除消息（物理删除）
- **智能同步**：删除消息前检查并更新相关用户的最后消息字段

**会话详情展示**：
- 显示会话的完整信息（包括用户级别的最后消息字段）
- 消息列表支持展开查看详情
- 支持编辑会话信息（状态、可见性等）

#### 1.4 关键特性

```java
// 管理端：不过滤双方都被删除的消息，显示所有消息
Specification<Message> spec = (root, query, cb) -> {
    // 只按 conversationId 过滤，不添加任何删除状态过滤条件
    return cb.equal(root.get("conversationId"), conversationId);
};
Page<Message> p = messageRepository.findAll(spec, pageable);
```

**管理端消息管理的特殊处理**：
- 可以看到所有消息，不受用户删除状态限制
- 编辑消息可见性时，会同步更新对话的最后消息字段
- 硬删除消息时，会检查并更新相关用户的最后消息字段

### 2. 用户级别的最后消息字段

#### 2.1 问题背景
**问题描述**：
- 当用户删除最后一条消息后，对话列表中仍然显示这条已删除的消息作为"最后消息"
- 原因是对话列表直接读取 `conversation.lastMessageContent` 字段，该字段没有考虑用户的删除状态

**影响范围**：
- 用户端对话列表显示不准确
- 用户可能看到自己已删除的消息作为最后消息

#### 2.2 解决方案
**数据库设计**：
- 在 `conversations` 表中新增4个字段：
  - `user_1_last_message_content` / `user_1_last_message_time` - 用户1可见的最后消息
  - `user_2_last_message_content` / `user_2_last_message_time` - 用户2可见的最后消息
- 保留原有字段 `last_message_content` / `last_message_time` 用于管理端（不过滤删除）

**实现位置**：
- SQL迁移脚本：`database/add_user_last_message_fields.sql`
- 实体类：`Conversation.java` - 新增字段和辅助方法
- 服务层：`ContactServiceImpl.java` - 发送/删除消息时更新字段
- 管理端：`AdminServiceImpl.java` - 恢复可见性和删除消息时更新字段

#### 2.3 核心机制

**消息可见性反向作用流程**：

```
消息删除标记 (deletedBySender/deletedByReceiver)
    ↓
判断是否影响对应用户的最后消息字段
    ↓
如果是最后一条可见消息 → 查询倒数第二条并更新
    ↓
用户字段自动反映"用户可见的最后消息"
```

**字段同步策略**：
1. **实时同步**：发送/删除消息时立即更新
2. **按需修复**：恢复可见性时重新查询并更新
3. **数据隔离**：每个用户维护自己的最后消息字段，互不影响
4. **降级方案**：查询失败时记录警告日志，不影响主流程

#### 2.4 更新场景覆盖

| 场景 | 更新逻辑 | 实现位置 |
|------|---------|---------|
| **发送消息** | 同时更新 user1 和 user2 字段 | `ContactServiceImpl.sendMessage()` |
| **用户删除消息** | 只更新删除该消息的用户字段 | `ContactServiceImpl.deleteMessage()` |
| **管理端恢复可见性** | 查询并更新对应用户字段 | `AdminServiceImpl.updateConversationFull()` |
| **管理端硬删除消息** | 分别检查并更新相关用户字段 | `AdminServiceImpl.deleteMessage()` |
| **管理端修改消息可见性** | 检测变化并更新相关用户字段 | `AdminServiceImpl.updateMessageFull()` |

### 3. 已读回执功能

#### 3.1 功能描述
实现了消息已读回执功能，当接收者标记消息为已读后，通过WebSocket向发送者推送已读通知，发送者可以实时看到消息的已读状态。

#### 3.2 实现细节

**后端实现**：
- **位置**：`ContactServiceImpl.markConversationAsRead()`
- **流程**：
  1. 标记所有消息为已读
  2. 查询被标记为已读的消息列表
  3. 构建已读通知（包含 conversationId、messageIds、readTime）
  4. 通过WebSocket推送 `MESSAGE_READ` 事件给发送者

**前端实现**：
- **位置**：`stores/message.js`
- **处理函数**：`handleMessageRead()`
- **功能**：
  - 接收已读通知
  - 更新消息列表中对应消息的 `isRead` 状态
  - 只更新发送者是当前用户且消息ID在已读列表中的消息

**数据结构**：
```json
{
  "type": "MESSAGE_READ",
  "conversationId": "CONV_xxx",
  "messageIds": ["MSG_1", "MSG_2", "MSG_3"],
  "readTime": "2025-01-20T10:30:00"
}
```

#### 3.3 特性
- **实时回执**：接收者标记已读后，发送者立即收到通知
- **批量更新**：一次标记已读，会推送所有被标记的消息ID列表
- **精确匹配**：只更新发送者自己的消息，避免误更新
- **容错处理**：包含数据验证和错误日志

### 4. 智能滚动优化

#### 4.1 功能描述
实现了聊天界面的智能滚动功能，只有在滚动条位于底部时才自动滚动到最新消息，避免打断用户查看历史消息。

#### 4.2 实现细节

**检测滚动位置**：
```javascript
const isAtBottom = () => {
  const { scrollTop, scrollHeight, clientHeight } = messagesListRef.value
  return scrollHeight - scrollTop - clientHeight <= 100 // 允许100px误差
}
```

**滚动策略**：
- **收到新消息**：检测滚动位置 → 如果在底部 → 平滑滚动到底部
- **选择对话**：立即滚动到底部（不使用平滑滚动）
- **初始加载**：延迟100ms后滚动到底部（确保DOM渲染完成）

**关键改进**：
- 从监听整个数组改为监听数组长度：`watch(() => props.messages?.length, ...)`
- 解决了Vue响应式系统中数组引用不变导致watch不触发的问题

---

## 技术架构优化

### 1. 数据库设计优化

#### 新增字段
```sql
-- conversations 表新增字段
ALTER TABLE conversations
ADD COLUMN user_1_last_message_content TEXT,
ADD COLUMN user_1_last_message_time DATETIME(6),
ADD COLUMN user_2_last_message_content TEXT,
ADD COLUMN user_2_last_message_time DATETIME(6);
```

#### 初始化数据
```sql
-- 初始化现有数据：将原有的最后消息复制到新字段
UPDATE conversations
SET
    user_1_last_message_content = last_message_content,
    user_1_last_message_time = last_message_time,
    user_2_last_message_content = last_message_content,
    user_2_last_message_time = last_message_time
WHERE last_message_content IS NOT NULL;
```

### 2. 实体类增强

#### Conversation 实体
- 新增4个用户级别最后消息字段
- 新增辅助方法：
  - `getLastMessageContentForUser(userId)` - 获取用户可见的最后消息内容
  - `getLastMessageTimeForUser(userId)` - 获取用户可见的最后消息时间
  - `setLastMessageForUser(userId, content, time)` - 设置用户可见的最后消息

### 3. Repository 层优化

#### MessageRepository
- 新增 `findUnreadMessagesByConversationAndReceiver()` 方法
- 用于查询对话中接收方的未读消息（用于已读回执）

#### ConversationRepository
- 新增 `findByUserIdAndStatus()` 方法（返回List，不排序）
- 用于在内存中按用户级别的最后消息时间排序

---

## 用户体验优化

### 1. 消息可见性显示
- **用户端**：对话列表显示用户可见的最后消息（过滤用户删除的）
- **管理端**：支持查看三套最后消息字段（管理端字段 + 用户1字段 + 用户2字段）

### 2. 已读状态实时更新
- 发送消息后，如果接收者在页面查看，消息立即显示为已读
- 接收者标记已读后，发送者通过WebSocket实时收到已读通知
- 聊天界面中消息的已读状态实时更新

### 3. 智能滚动体验
- **不打断用户**：用户查看历史消息时，收到新消息不会自动滚动
- **自动跟进**：用户在底部时，收到新消息自动平滑滚动
- **快速定位**：选择对话后立即滚动到最新消息

---

## 技术细节

### 1. 管理端消息管理实现

#### 1.1 消息列表查询（不过滤删除状态）

**关键实现**：
```java
// 使用 Specification 构建查询条件，不添加删除状态过滤
Specification<Message> spec = (root, query, cb) -> {
    // 只按 conversationId 过滤，不添加任何删除状态过滤条件
    return cb.equal(root.get("conversationId"), conversationId);
};
Page<Message> p = messageRepository.findAll(spec, pageable);
```

**与用户端的区别**：
- 用户端：使用 `findByConversationId()` 会自动过滤双方都删除的消息
- 管理端：使用 `findAll(spec, pageable)` 显示所有消息，包括双方都删除的

**统计功能**：
```java
// 统计双方都删除的消息数量（用于验证和日志）
long bothDeletedCount = 0;
for (Message m : p.getContent()) {
    if (Boolean.TRUE.equals(m.getDeletedBySender()) && 
        Boolean.TRUE.equals(m.getDeletedByReceiver())) {
        bothDeletedCount++;
    }
}
```

#### 1.2 消息可见性编辑时的同步更新

**实现逻辑**：
```java
// 检测发送方可见性的变化
if (newDeletedBySender != null && !newDeletedBySender.equals(oldDeletedBySender)) {
    boolean wasVisible = !Boolean.TRUE.equals(oldDeletedBySender);
    boolean isNowVisible = !Boolean.TRUE.equals(newDeletedBySender);
    
    if (wasVisible && !isNowVisible) {
        // 从可见变为不可见（标记删除）
        // 查询倒数第二条可见消息并更新
    } else if (!wasVisible && isNowVisible) {
        // 从不可见变为可见（取消删除标记）
        // 如果这条消息比当前最后消息新，则更新
    }
}
```

#### 1.3 消息硬删除时的同步更新

**实现逻辑**：
```java
// 检查删除的消息是否为用户的最后一条可见消息
String userLastContent = conversation.getLastMessageContentForUser(userId);
LocalDateTime userLastTime = conversation.getLastMessageTimeForUser(userId);
boolean isLastMessage = message.getContent().equals(userLastContent) && 
                       message.getCreatedAt().equals(userLastTime);

if (isLastMessage) {
    // 查询倒数第二条可见消息
    List<Message> lastMessages = messageRepository.findLastMessageForUser(
        conversationId, userId, PageRequest.of(0, 1));
    
    if (!lastMessages.isEmpty()) {
        Message newLastMessage = lastMessages.get(0);
        conversation.setLastMessageForUser(userId, 
            newLastMessage.getContent(), 
            newLastMessage.getCreatedAt());
    } else {
        conversation.setLastMessageForUser(userId, null, null);
    }
}
```

### 2. 消息可见性反向作用机制

#### 删除消息时的更新逻辑
```java
// 检查删除的消息是否为该用户可见的最后一条消息
String userLastContent = conversation.getLastMessageContentForUser(userId);
LocalDateTime userLastTime = conversation.getLastMessageTimeForUser(userId);
boolean isLastMessage = message.getContent().equals(userLastContent) && 
                       message.getCreatedAt().equals(userLastTime);

if (isLastMessage) {
    // 查询倒数第二条可见消息并更新
    List<Message> lastMessages = messageRepository.findLastMessageForUser(
        conversationId, userId, PageRequest.of(0, 1));
    
    if (!lastMessages.isEmpty()) {
        Message newLastMessage = lastMessages.get(0);
        conversation.setLastMessageForUser(userId, 
            newLastMessage.getContent(), 
            newLastMessage.getCreatedAt());
    } else {
        conversation.setLastMessageForUser(userId, null, null);
    }
}
```

#### 恢复可见性时的更新逻辑
```java
// 检测可见性从 false 变为 true（恢复可见性）
if (newUser1Visibility != null && 
    Boolean.FALSE.equals(oldUser1Visibility) && 
    Boolean.TRUE.equals(newUser1Visibility)) {
    
    // 查询用户1可见的最后一条消息
    List<Message> lastMessages = messageRepository.findLastMessageForUser(
        conversationId, userId1, PageRequest.of(0, 1));
    
    if (!lastMessages.isEmpty()) {
        Message lastMessage = lastMessages.get(0);
        conversation.setUser1LastMessageContent(lastMessage.getContent());
        conversation.setUser1LastMessageTime(lastMessage.getCreatedAt());
    } else {
        conversation.setUser1LastMessageContent(null);
        conversation.setUser1LastMessageTime(null);
    }
}
```

### 2. 已读回执实现

#### 后端推送已读通知
```java
// 查询被标记为已读的消息列表（在标记为已读之前查询）
List<Message> unreadMessages = messageRepository.findUnreadMessagesByConversationAndReceiver(
    conversationId, userId);

// 标记所有消息为已读
messageRepository.markMessagesAsRead(conversationId, userId, readTime);

// 构建已读通知并推送
Map<String, Object> readNotification = new HashMap<>();
readNotification.put("type", "MESSAGE_READ");
readNotification.put("conversationId", conversationId);
readNotification.put("readTime", readTime.toString());
readNotification.put("messageIds", unreadMessages.stream()
    .map(Message::getMessageId)
    .collect(Collectors.toList()));

webSocketRetryService.pushWithRetry(otherUserId, readNotification, "MESSAGE_READ");
```

#### 前端处理已读回执
```javascript
handleMessageRead(readData) {
  const { conversationId, messageIds, readTime } = readData
  
  // 更新消息列表中的已读状态
  this.messages.forEach(msg => {
    if (msg.conversationId === conversationId && 
        msg.isMine && 
        messageIds.includes(msg.messageId) && 
        !msg.isRead) {
      msg.isRead = true
      msg.readTime = readTime || new Date().toISOString()
    }
  })
}
```

### 3. 滚动优化实现

#### 监听数组长度变化
```javascript
// 从监听整个数组改为监听数组长度
watch(() => props.messages?.length, (newLength, oldLength) => {
  // 当有新消息时（长度增加）
  if (newLength && newLength > oldLength) {
    nextTick(() => {
      setTimeout(() => {
        if (isAtBottom()) {
          scrollToBottom(true) // 平滑滚动
        }
      }, 50)
    })
  }
}, { immediate: true })
```

**关键改进**：
- 解决了 `watch(() => props.messages, ...)` 不触发的问题
- 因为 `push()` 不会改变数组引用，但会改变数组长度

---

## 已知问题和后续优化

### 1. 性能优化待改进

#### 1.1 对话列表排序优化
**当前实现**：
- `getConversations()` 方法在内存中排序所有对话
- 当用户对话数量较多时，可能影响性能

**优化建议**：
- 使用数据库排序（需要动态SQL或数据库函数）
- 或者使用Redis缓存排序后的对话ID列表

#### 1.2 内存排序的扩展性
**当前问题**：
- 所有对话加载到内存后排序和分页
- 如果用户有1000+对话，性能可能下降

**优化建议**：
- 限制内存排序的最大对话数量（如500条）
- 超过限制时使用数据库排序（降级方案）

### 2. 功能完善待改进

#### 2.1 商品卡片功能
**当前状态**：
- `ContactServiceImpl.sendMessage()` 中商品卡片功能标记为 `TODO`
- 功能暂未实现，返回错误提示

**建议**：
- 实现商品卡片消息类型
- 需要商品查询页面支持

#### 2.2 黑名单功能
**当前状态**：
- `ContactServiceImpl` 中的黑名单相关方法都是占位实现
- 返回"功能开发中"

**建议**：
- 实现用户黑名单功能
- 黑名单用户之间无法发送消息

### 3. 调试日志清理

#### 当前状态
- 代码中包含大量 `console.log` 调试日志
- 这些日志在生产环境应该移除或使用日志级别控制

**建议**：
- 使用日志库（如 `winston`、`pino`）替代 `console.log`
- 根据环境变量控制日志级别
- 移除或注释掉调试日志

### 4. 错误处理增强

#### 当前状态
- 部分异常处理使用了 `e.printStackTrace()`
- 缺少统一的错误处理和日志记录

**建议**：
- 统一使用 `log.error()` 记录异常
- 添加异常上下文信息（用户ID、操作类型等）
- 区分可恢复错误和不可恢复错误

### 5. 代码质量改进

#### 5.1 代码复用
**当前问题**：
- `ContactServiceImpl` 和 `AdminServiceImpl` 中都有更新最后消息字段的逻辑
- 存在代码重复

**建议**：
- 抽取公共方法到 `ConversationService` 或工具类
- 统一管理最后消息字段的更新逻辑

#### 5.2 方法长度
**当前问题**：
- `ContactServiceImpl.sendMessage()` 方法较长
- `AdminServiceImpl.updateMessageFull()` 方法较长

**建议**：
- 拆分为多个私有方法
- 提高代码可读性和可维护性

---

## 版本对比

### v1.1.3 → v1.1.4 主要变化

| 方面 | v1.1.3 | v1.1.4 |
|------|--------|--------|
| **管理端消息管理** | ❌ 不支持 | ✅ 完整的CRUD功能（查看、编辑、删除） |
| **最后消息字段** | 单一字段（不过滤删除） | 用户级别字段（过滤删除） |
| **已读回执** | ❌ 不支持 | ✅ WebSocket实时推送 |
| **滚动行为** | 总是滚动到底部 | ✅ 智能滚动（根据位置） |
| **管理端消息字段** | 只有基础字段 | ✅ 支持查看用户级别字段 |
| **消息可见性影响** | 仅影响消息显示 | ✅ 反向影响对话最后消息 |
| **管理端操作同步** | ❌ 不更新最后消息字段 | ✅ 自动同步更新最后消息字段 |

---

## 测试建议

### 1. 消息删除场景测试
- [ ] 删除最后一条消息，验证对话列表更新
- [ ] 删除非最后一条消息，验证对话列表不变
- [ ] 双方都删除消息，验证各自字段正确

### 2. 已读回执测试
- [ ] 发送消息后，接收者查看对话，验证发送者收到已读通知
- [ ] 发送多条消息后批量标记已读，验证所有消息ID都在通知中
- [ ] 发送者不在聊天页面时收到已读通知，验证消息状态更新

### 3. 滚动行为测试
- [ ] 在底部收到新消息，验证自动滚动
- [ ] 在顶部查看历史消息时收到新消息，验证不自动滚动
- [ ] 切换对话，验证立即滚动到底部

### 4. 管理端操作测试
- [ ] 管理端恢复对话可见性，验证最后消息字段更新
- [ ] 管理端删除消息，验证相关用户字段更新
- [ ] 管理端修改消息可见性，验证相关用户字段更新

---

## 总结

v1.1.4 版本是一个重要的用户体验改进版本，主要解决了消息系统中的核心问题：

1. **解决了"最后消息显示错误"的根本问题**：通过用户级别的最后消息字段，确保对话列表显示的准确性
2. **实现了已读回执功能**：提升了用户对消息状态的感知
3. **优化了滚动体验**：避免打断用户查看历史消息，同时自动跟进新消息

这些改进使消息系统的用户体验更加完善，为后续功能扩展打下了良好的基础。

