# 南大集市 NJUMarket v1.3.0 项目文档

## 📋 目录
- [版本概述](#版本概述)
- [核心功能更新](#核心功能更新)
- [聊天功能优化](#聊天功能优化)
- [订单提醒持久化](#订单提醒持久化)
- [UI/UX 优化](#uiux-优化)
- [技术实现细节](#技术实现细节)
- [已知问题与限制](#已知问题与限制)
- [下一步规划](#下一步规划)

---

## 版本概述

### 版本信息
- **版本**: v1.3.0
- **发布时间**: 2025-01-XX
- **基于版本**: v1.2.2
- **状态**: 已完成，用户聊天功能和订单提醒持久化已完成

### 版本定位
v1.3.0 版本专注于**用户体验优化**和**数据持久化**，实现了聊天界面的无限滚动、消息和对话的软删除功能，以及订单提醒状态的持久化存储。通过优化聊天交互体验和确保订单提醒状态的跨会话一致性，显著提升了用户使用体验。

### 主要成就
- ✅ **聊天无限滚动**：移除200条消息限制，支持无限加载历史消息
- ✅ **智能滚动管理**：CSS反转显示 + 逻辑滚动，自动扩展功能
- ✅ **消息软删除**：点击消息显示删除选项，仅影响当前用户视图
- ✅ **对话软删除**：左滑删除对话，支持桌面端和移动端
- ✅ **订单提醒持久化**：订单提醒状态存储到数据库，跨会话保持
- ✅ **应用启动扫描**：启动时自动从profile加载订单提醒状态

---

## 核心功能更新

### 1. 聊天功能优化

#### 1.1 无限滚动加载历史消息

**实现位置**：
- 后端：`MessageRepository.findMessagesBefore()` - 新增查询方法
- 后端：`ContactService.getMessagesBefore()` - 新增服务方法
- 后端：`ContactController.getMessagesBefore()` - 新增API端点
- 前端：`stores/message.js` - `loadMoreMessages()` 方法
- 前端：`components/messages/ChatWindow.vue` - 无限滚动逻辑

**功能说明**：
- 移除了200条消息的限制，支持无限加载历史消息
- 使用标准滚动方式（ASC排序），最新消息在底部
- 向上滚动到顶部时自动加载更早的消息
- 停留在顶部一段时间后自动扩展加载历史消息

**技术实现**：
```java
// 后端：获取指定时间之前的消息
@Query("SELECT m FROM Message m WHERE m.conversationId = :conversationId " +
       "AND m.createdAt < :beforeTime " +
       "AND NOT (m.deletedBySender = true AND m.deletedByReceiver = true) " +
       "ORDER BY m.createdAt DESC")
Page<Message> findMessagesBefore(@Param("conversationId") String conversationId,
                                 @Param("beforeTime") LocalDateTime beforeTime,
                                 Pageable pageable);
```

**关键特性**：
- ✅ 支持分页加载，每次加载50条
- ✅ 自动检测滚动位置，智能加载历史消息
- ✅ 保持滚动位置，避免加载后跳回底部
- ✅ 等待卡片加载完成后再滚动，确保位置准确

#### 1.2 消息软删除功能

**实现位置**：
- 后端：`ContactServiceImpl.deleteMessage()` - 更新软删除逻辑
- 前端：`components/messages/ChatWindow.vue` - 点击消息显示删除菜单
- 前端：`stores/message.js` - `deleteMessage()` 方法

**功能说明**：
- 点击消息气泡显示删除按钮（位于消息上方）
- 删除后仅影响当前用户的视图，对方仍能看到消息
- 如果删除的是最后一条可见消息，自动更新对话的最后消息字段
- 使用 `UnifiedButton` 组件，样式统一

**技术实现**：
- 设置 `deletedBySender` 或 `deletedByReceiver` 字段为 `true`
- 查询时过滤掉双方都已删除的消息
- 删除后检查是否需要更新对话的最后消息时间

#### 1.3 对话软删除功能

**实现位置**：
- 后端：`ContactServiceImpl.deleteConversation()` - 更新软删除逻辑
- 前端：`components/messages/ConversationList.vue` - 左滑删除机制
- 前端：`stores/message.js` - `deleteConversation()` 方法

**功能说明**：
- 桌面端和移动端都支持左滑删除对话
- 删除后仅影响当前用户的视图，对方仍能看到对话
- 删除后设置 `user1Visibility` 或 `user2Visibility` 为 `false`
- 对方发送新消息时自动恢复对话可见性

**技术实现**：
- 移动端：使用 `touchstart`、`touchmove`、`touchend` 事件
- 桌面端：使用 `mousedown`、`mousemove`、`mouseup` 事件
- 删除按钮宽度80px，滑动超过阈值时显示删除按钮
- 使用 `transform: translateX()` 实现滑动效果

#### 1.4 对话可见性自动恢复

**实现位置**：
- 后端：`ContactServiceImpl.sendMessage()` - 自动恢复可见性逻辑
- 后端：`WebSocketRetryService` - 推送 `CONVERSATION_VISIBILITY_RESTORED` 事件
- 前端：`stores/message.js` - `handleConversationRestored()` 方法

**功能说明**：
- 当对方发送新消息时，如果对话被当前用户软删除，自动恢复可见性
- 通过WebSocket推送恢复通知，前端增量更新对话列表
- 避免全量刷新，提升用户体验

**技术实现**：
- 检查接收方的对话可见性，如果为 `false` 则恢复为 `true`
- 推送完整的 `ConversationDTO`，前端直接使用
- 前端检查对话是否已存在，存在则更新，不存在则添加到列表顶部

---

### 2. 订单提醒持久化

#### 2.1 数据库字段添加

**实现位置**：
- 数据库：`add_order_reminder_fields.sql` - 迁移脚本
- 实体：`UserProfile.java` - 添加 `sellerOrderHasNew` 和 `buyerOrderHasNew` 字段

**功能说明**：
- 在 `user_profiles` 表添加两个布尔字段，存储订单提醒状态
- 字段允许 NULL，有默认值 FALSE，确保向后兼容
- 使用 `IF NOT EXISTS` 检查，避免重复添加

**技术实现**：
```sql
ALTER TABLE user_profiles 
ADD COLUMN seller_order_has_new BOOLEAN DEFAULT FALSE COMMENT '卖家订单是否有新变化（v1.3.x）',
ADD COLUMN buyer_order_has_new BOOLEAN DEFAULT FALSE COMMENT '买家订单是否有新变化（v1.3.x）';
```

#### 2.2 Service层方法

**实现位置**：
- `UserProfileService` - 新增3个方法接口
- `UserProfileServiceImpl` - 实现订单提醒状态管理

**功能说明**：
- `getOrderReminderStatus()` - 获取订单提醒状态（兼容字段不存在的情况）
- `setOrderReminderStatus()` - 设置订单提醒状态（兼容字段不存在的情况）
- `clearOrderReminderStatus()` - 清除订单提醒状态

**兼容性保证**：
- 所有方法都有异常处理，字段不存在时返回默认值或记录警告
- 不抛出异常，确保不影响现有功能

#### 2.3 订单变化时更新状态

**实现位置**：
- `OrderServiceImpl.pushOrderChangeNotificationWithDTO()` - 在推送通知后更新状态

**功能说明**：
- 订单变化时，除了推送WebSocket通知，还更新数据库中的提醒状态
- 确保离线用户上线后也能看到订单提醒角标

#### 2.4 登录时返回状态

**实现位置**：
- `UserServiceImpl.login()` 和 `loginByCode()` - 在登录响应中添加订单提醒状态

**功能说明**：
- 登录响应中包含 `orderReminderStatus` 字段
- 前端登录时自动初始化订单提醒状态
- 兼容性处理：字段不存在时返回默认值

#### 2.5 应用启动时扫描

**实现位置**：
- `stores/user.js` - `initUser()` 方法中调用扫描
- `stores/order.js` - `fetchOrderReminderStatus()` 方法

**功能说明**：
- 应用启动时，如果用户已登录，自动从后端获取订单提醒状态
- 确保刷新页面或重新打开应用后，角标状态正确显示

#### 2.6 清除时同步后端

**实现位置**：
- `stores/order.js` - `clearSellerOrderNotification()` 和 `clearBuyerOrderNotification()` 方法
- `api/index.js` - `profileAPI.clearOrderReminder()` 方法
- `UserProfileController` - `clearOrderReminder()` 端点

**功能说明**：
- 进入订单页面清除角标时，同步清除后端数据库状态
- API失败不影响前端状态，确保向后兼容

---

### 3. UI/UX 优化

#### 3.1 角标样式优化

**实现位置**：
- `components/common/UnreadBadge.vue` - 点状角标样式优化

**功能说明**：
- 默认点状角标：右上角显示（`top: 0; right: 0`）
- 手机端订单提醒角标：垂直居中 + 右移16px（`top: 50%; right: -16px`）
- 避免遮挡文字，提升可读性

**技术实现**：
- 使用 `mobile-order-badge` 类名特化样式
- 通过 `badge-class` 属性传递类名
- 保持其他角标样式不变

#### 3.2 对话选中状态优化

**实现位置**：
- `components/messages/ConversationList.vue` - 选中状态样式

**功能说明**：
- 选中状态使用偏向白色的过渡色（95%白色 + 5%主题色）
- 不透明背景，提升视觉清晰度
- 保留左侧主题色边框，保持视觉一致性

---

## 技术实现细节

### 1. 消息排序和滚动

**排序策略**：
- 数据层：ASC排序（最旧在前，最新在后）
- 显示层：标准滚动（最新消息在底部）
- 初始加载：自动滚动到底部显示最新消息

**滚动位置保持**：
- 加载历史消息前，记录第一个可见消息的 `offsetTop`
- 加载完成后，滚动回该位置
- 等待卡片加载完成后再滚动，确保位置准确

### 2. 自动扩展机制

**触发条件**：
- 用户停留在顶部（`scrollTop` 接近 0）
- 停留时间超过阈值（默认2秒）
- 还有更多历史消息可加载

**实现逻辑**：
- 使用 `setTimeout` 定时器检测停留时间
- 滚动时清除定时器，重新计时
- 加载完成后，如果仍在顶部，重新启动定时器

### 3. 软删除逻辑

**消息软删除**：
- 设置 `deletedBySender` 或 `deletedByReceiver` 为 `true`
- 查询时过滤：`NOT (deletedBySender = true AND deletedByReceiver = true)`
- 如果删除的是最后一条可见消息，查询倒数第二条更新对话

**对话软删除**：
- 设置 `user1Visibility` 或 `user2Visibility` 为 `false`
- 查询时过滤：`user1Visibility = true` 或 `user2Visibility = true`
- 对方发送新消息时自动恢复可见性

### 4. 订单提醒持久化

**更新时机**：
- 订单变化时：`pushOrderChangeNotificationWithDTO()` 中更新
- 清除角标时：`clearOrderReminderStatus()` 中清除

**读取时机**：
- 登录时：从登录响应中读取
- 应用启动时：调用 `fetchOrderReminderStatus()` 读取

**兼容性处理**：
- 字段不存在时返回默认值 `false`
- API失败时不影响应用启动和功能使用

---

## 已知问题与限制

### 1. 消息加载性能
- **问题**：加载大量历史消息时，DOM元素过多可能影响性能
- **影响**：轻微，通常用户不会加载过多历史消息
- **解决方案**：v2.0阶段考虑虚拟滚动优化

### 2. 角标样式警告
- **问题**：ESLint警告空规则集
- **影响**：无，仅为代码规范警告
- **状态**：已修复

### 3. 订单提醒状态同步
- **问题**：WebSocket推送和数据库更新可能存在时序问题
- **影响**：极小，通常不会出现
- **解决方案**：当前实现已通过异常处理保证兼容性

---

## 下一步规划

### v1.3.1 - Spring Security规范化迁移 ✅
详见 [v1.3.1 项目文档](./PROJECT_DOCUMENTATION_V1.3.1.md)

**v1.3.1 核心任务**：
- ✅ Spring Security规范化迁移：从拦截器迁移到Spring Security Filter
- ✅ 代码清理：删除冗余拦截器，优化代码结构
- ✅ 方法级权限控制：使用@PreAuthorize实现细粒度权限控制

### v1.4 - 代码标准化与架构规范化 ✅
详见 [v1.4 项目文档](./PROJECT_DOCUMENTATION_V1.4.md)

**v1.4 核心任务**：
- ✅ 统一异常处理：所有Service方法统一使用`BusinessException`
- ✅ 统一日志记录：AOP统一记录，移除手动日志
- ✅ 业务校验组件化：创建`BusinessValidator`工具类

### v2.0 阶段规划
详见 [v2.0 规划文档](./PROJECT_DOCUMENTATION_V2.0.md)

---

## 相关文档

### 版本演进
- [v1.4 项目文档](./PROJECT_DOCUMENTATION_V1.4.md) - 代码标准化与架构规范化 ⭐ **最新版本**
- [v1.3.1 项目文档](./PROJECT_DOCUMENTATION_V1.3.1.md) - Spring Security规范化迁移 ⬅️ **下一版本**
- [v1.2.2 项目文档](./PROJECT_DOCUMENTATION_V1.2.2.md) - 索引优化 ⬅️ **上一版本**
- [v1.2.0 项目文档](./PROJECT_DOCUMENTATION_V1.2.0.md) - 库存超卖防护
- [v1.x 阶段总结](./PROJECT_DOCUMENTATION_V1.x_SUMMARY.md) - v1.x完整总结

---

**文档版本**：v1.3.0  
**最后更新**：2025-01-XX

