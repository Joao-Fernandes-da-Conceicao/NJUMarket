# 未读数字段更新说明

## 更新内容

后端数据库字段已从 `buyer_unread_count` / `seller_unread_count` 重命名为 `user_1_count` / `user_2_count`。

## 前端兼容性

✅ **前端无需修改代码**

### 原因

1. **对话未读数** (`conversation.unreadCount`)
   - 前端使用：`conversation.unreadCount`
   - 后端返回：`ConversationDTO.unreadCount`
   - 计算方式：通过 `conversation.getUnreadCountForUser(currentUserId)` 方法计算
   - 该方法已更新为使用新的字段名 `user1Count` / `user2Count`

2. **总未读数** (`totalUnreadCount`)
   - 前端使用：`messageStore.totalUnreadCount`
   - 后端 API：`/contact/unread-count`
   - 计算方式：通过 `ConversationRepository.getTotalUnreadCount()` 查询
   - 该查询已更新为使用新的字段名

### 前端使用位置

1. **ConversationList.vue**
   - 显示对话未读数：`conversation.unreadCount`
   - 显示总未读数：`totalUnreadCount`

2. **AppHeader.vue**
   - 显示消息徽章：`unreadCount` (来自 `messageStore.totalUnreadCount`)

3. **message.js (Store)**
   - 存储和更新：`conversation.unreadCount`、`totalUnreadCount`
   - 标记已读后重置：`conversation.unreadCount = 0`

### 数据流

```
数据库 (user_1_count / user_2_count)
  ↓
Conversation 实体 (user1Count / user2Count)
  ↓
getUnreadCountForUser() 方法计算
  ↓
ConversationDTO.unreadCount
  ↓
前端 conversation.unreadCount ✅
```

## 验证

前端功能应正常工作，因为：
- ✅ DTO 字段名未改变 (`unreadCount`)
- ✅ API 响应格式未改变
- ✅ 前端代码使用抽象的字段名，不直接访问数据库字段

## 注意事项

如果将来需要直接访问 `user1Count` 或 `user2Count`：
- 这些字段不在 DTO 中暴露（设计如此）
- 应始终使用 `unreadCount` 字段，它已经根据当前用户正确计算

