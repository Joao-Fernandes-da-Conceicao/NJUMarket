# 南大集市 NJUMarket v1.1.1 项目文档

## 📋 目录
- [版本概述](#版本概述)
- [核心功能更新](#核心功能更新)
- [WebSocket心跳机制优化](#websocket心跳机制优化)
- [状态同步优化](#状态同步优化)
- [移动端Bug修复](#移动端bug修复)
- [技术细节](#技术细节)
- [已知问题与限制](#已知问题与限制)

---

## 版本概述

### 版本信息
- **版本**: v1.1.1
- **发布时间**: 2025-11-02
- **基于版本**: v1.1.0
- **状态**: 已发布，用户体验优化完成

### 版本定位
v1.1.1 版本专注于**用户体验优化**和**系统稳定性**提升，通过完善WebSocket心跳机制、统一状态管理、修复移动端问题，显著改善了用户体验和系统可靠性。

### 主要成就
- ✅ **WebSocket心跳机制完善**：双重心跳检测，确保连接稳定性
- ✅ **状态同步优化**：统一未读数管理，避免状态不一致
- ✅ **移动端体验修复**：解决对话列表消失和状态不一致问题
- ✅ **代码质量提升**：修复ESLint错误和模板访问问题

---

## 核心功能更新

### 1. WebSocket心跳机制完善

#### 1.1 双重心跳检测机制

**实现位置**：
- 前端：`utils/websocket.js`

**功能说明**：
- **主机制**：通过`debug`回调检测STOMP心跳帧（每10秒）
- **备用机制**：心跳保活机制（每12秒检查连接状态，只要连接正常就更新时间戳）
- 即使聊天记录未更新，只要连接正常就不会误判连接失效

**技术实现**：
```javascript
// 主机制：通过debug回调检测心跳帧
debug: (str) => {
  if (str) {
    const trimmed = str.trim().toLowerCase()
    // 识别心跳帧：空帧或特殊格式
    if (trimmed === '' || trimmed === '\n' || 
        (trimmed.length < 20 && !trimmed.includes('connected') && 
         !trimmed.includes('message') && !trimmed.includes('error'))) {
      const previousTime = this.lastHeartbeatTime
      this.lastHeartbeatTime = Date.now()
      
      // 记录心跳确认日志（每10秒一次）
      if (previousTime) {
        const timeSinceLastHeartbeat = this.lastHeartbeatTime - previousTime
        if (timeSinceLastHeartbeat >= 8000 && timeSinceLastHeartbeat <= 15000) {
          console.log('💓 WebSocket心跳确认', {
            timestamp: new Date(this.lastHeartbeatTime).toISOString(),
            timeSinceLastHeartbeat: `${Math.round(timeSinceLastHeartbeat / 1000)}秒`,
            connectionHealthy: true
          })
        }
      }
    }
  }
}

// 备用机制：心跳保活
startHeartbeatKeepAlive() {
  this.heartbeatKeepAliveInterval = setInterval(() => {
    if (this.isConnectedState() && this.stompClient && this.stompClient.connected) {
      const previousTime = this.lastHeartbeatTime
      this.lastHeartbeatTime = Date.now()
      
      // 记录心跳保活日志（每12秒一次）
      if (previousTime) {
        const timeSinceLastUpdate = this.lastHeartbeatTime - previousTime
        if (timeSinceLastUpdate >= 11000 && timeSinceLastUpdate <= 13000) {
          console.log('💚 WebSocket连接保活确认', {
            timestamp: new Date(this.lastHeartbeatTime).toISOString(),
            timeSinceLastUpdate: `${Math.round(timeSinceLastUpdate / 1000)}秒`,
            connectionStatus: 'active',
            note: '连接正常，即使没有业务消息也会保持活跃'
          })
        }
      }
    }
  }, 12000) // 每12秒检查一次
}
```

**关键改进**：
1. **双机制保障**：即使debug回调未捕获到心跳帧，保活机制也能确保连接状态正常
2. **超时时间优化**：从40秒增加到60秒（6个心跳周期），提供足够的容错空间
3. **页面隐藏检测**：页面隐藏时跳过心跳检测，避免误判
4. **心跳确认日志**：每次心跳确认都会记录日志，便于调试和监控

#### 1.2 心跳确认日志

**日志类型**：
- 💓 **WebSocket心跳确认**：检测到心跳帧时（每10秒）
- 💚 **WebSocket连接保活确认**：保活机制触发时（每12秒）
- 📨 **收到业务消息，连接保持活跃**：收到业务消息时
- 💚 **WebSocket连接已建立，心跳机制已启动**：连接成功时

**作用**：
- 便于调试和监控连接状态
- 确认心跳机制正常工作
- 及时发现连接问题

---

## 状态同步优化

### 2.1 统一未读数管理

**实现位置**：
- 前端：`stores/message.js`

**问题描述**：
- 未读数在Store和组件中都有，可能不一致
- WebSocket更新和API更新可能冲突
- 总未读数需要手动计算，容易出错

**优化方案**：
```javascript
state: () => ({
  // 未读消息总数（统一从 conversationUnreadMap 计算得出）
  totalUnreadCount: 0,
  // ✅ 使用Map存储对话未读数，便于快速更新和统一管理
  conversationUnreadMap: new Map(),
  conversations: [],
  // ...
}),

/**
 * ✅ 统一更新未读数方法
 * 确保所有未读数更新都通过此方法，避免Store和组件中的不一致
 * 以及WebSocket更新和API更新之间的冲突
 */
updateUnreadCount(conversationId, count, updateTotal = true) {
  // 更新对话未读数Map
  if (conversationId) {
    this.conversationUnreadMap.set(conversationId, count || 0)
    
    // 同时更新对话对象中的未读数（保持响应式）
    const conversation = this.conversations.find(
      c => c.conversationId === conversationId
    )
    if (conversation) {
      conversation.unreadCount = count || 0
    }
  }
  
  // 重新计算总未读数（从Map累加）
  if (updateTotal) {
    this.totalUnreadCount = Array.from(this.conversationUnreadMap.values())
      .reduce((sum, count) => sum + (count || 0), 0)
  }
}
```

**优化效果**：
- ✅ **单一数据源**：`conversationUnreadMap`作为唯一的数据源
- ✅ **自动同步**：更新Map时自动同步到对话对象和总未读数
- ✅ **冲突避免**：所有更新都通过统一方法，避免状态不一致
- ✅ **易于维护**：逻辑集中，便于调试和维护

### 2.2 更新路径统一

**修改的方法**：
1. `fetchConversations()`: 从对话列表同步未读数到Map
2. `markAsRead()`: 使用统一方法更新
3. `handleUnreadCountUpdate()`: 使用统一方法处理WebSocket更新
4. `clearCurrentConversation()`: 清空时自动隐藏聊天窗口

**解决的问题**：
- ✅ 未读数统一管理：不再分散在Store和组件中
- ✅ WebSocket和API更新冲突：统一更新方法确保一致性
- ✅ 总未读数计算：从Map累加，保证准确性

---

## 移动端Bug修复

### 3.1 对话列表消失问题修复

**问题描述**：
- 移动端初始状态时，对话列表被隐藏，但聊天窗口显示"选择一个对话开始聊天"的空状态
- 状态不一致：`showChatWindow`为`true`，但`selectedConversationId`为`null`

**修复方案**：

1. **优化chat-panel隐藏逻辑**：
```vue
<div class="chat-panel" :class="{ 
  'hidden': isMobile ? (!showChatWindow || !selectedConversationId) : (!selectedConversationId)
}">
```

2. **初始化时清空状态**：
```javascript
onMounted(() => {
  detectMobile()
  
  // ✅ 移动端修复：确保初始状态下聊天窗口隐藏且没有选中对话
  if (isMobile.value) {
    messageStore.clearCurrentConversation()
    messageStore.hideChat()
  }
  // ...
})
```

3. **clearCurrentConversation自动隐藏窗口**：
```javascript
clearCurrentConversation() {
  this.selectedConversationId = null
  this.currentConversation = null
  this.messages = []
  // ✅ 移动端修复：清空对话时，确保聊天窗口也隐藏
  this.showChatWindow = false
}
```

**修复效果**：
- ✅ 移动端初始状态：只显示对话列表，聊天窗口隐藏
- ✅ 移动端选中对话：显示聊天窗口，对话列表隐藏
- ✅ 移动端返回列表：对话列表显示，聊天窗口隐藏
- ✅ 状态一致性：`showChatWindow`和`selectedConversationId`保持一致

### 3.2 桌面端右侧聊天栏预先加载问题修复

**问题描述**：
- 移动端错误地显示了桌面端的右侧聊天浏览栏
- 导致出现"选择聊天以开始"这种无法进入任何聊天的界面

**修复方案**：
- 优化模板中的响应式变量访问，创建计算属性`isMobile`来访问`globalIsMobile`
- 统一移动端和桌面端的显示逻辑

**修复效果**：
- ✅ 移动端：初始只显示对话列表，不会显示空聊天窗口
- ✅ 移动端：选中对话后才显示聊天窗口
- ✅ 桌面端：保持原有的并排显示逻辑，不受影响

---

## 技术细节

### 4.1 WebSocket心跳机制技术细节

#### 4.1.1 心跳检测逻辑

**超时时间**：
- 从40秒增加到60秒（6个心跳周期）
- 提供足够的容错空间，即使偶尔丢失1-2个心跳帧，也不会误判为连接失效

**检测机制**：
```javascript
setupHeartbeatMonitor() {
  this.heartbeatMonitorInterval = setInterval(() => {
    if (this.isConnectedState()) {
      // 页面隐藏时跳过检测
      if (typeof document !== 'undefined' && document.hidden) {
        return
      }
      
      const now = Date.now()
      const effectiveTimeout = 60000 // 60秒
      
      if (this.lastHeartbeatTime && (now - this.lastHeartbeatTime > effectiveTimeout)) {
        console.warn('⚠️ WebSocket心跳超时，开始重连', {
          lastHeartbeatTime: new Date(this.lastHeartbeatTime).toISOString(),
          timeout: `${effectiveTimeout / 1000}秒`,
          elapsed: `${Math.round((now - this.lastHeartbeatTime) / 1000)}秒`,
          reason: '超过60秒未收到心跳帧或业务消息'
        })
        this.disconnect()
        this.isManuallyDisconnected = false
        this.scheduleReconnect()
      }
    }
  }, 5000) // 每5秒检查一次
}
```

#### 4.1.2 心跳保活机制

**工作原理**：
- 每12秒检查一次连接状态
- 如果`stompClient.connected === true`，更新`lastHeartbeatTime`
- 即使没有业务消息，也能保持连接状态

**优势**：
- 不依赖STOMP的心跳帧检测
- 作为备用机制，确保连接状态正确

### 4.2 状态同步优化技术细节

#### 4.2.1 Map数据结构

**为什么使用Map？**
- 快速查找：O(1)时间复杂度
- 便于更新：直接通过conversationId更新
- 自动去重：相同的key会被覆盖

**使用场景**：
- 存储对话未读数：`Map<conversationId, unreadCount>`
- 快速查找和更新
- 自动累加计算总未读数

#### 4.2.2 统一更新方法

**方法签名**：
```javascript
updateUnreadCount(conversationId, count, updateTotal = true)
```

**参数说明**：
- `conversationId`: 对话ID，如果为null则只更新总未读数
- `count`: 新的未读数
- `updateTotal`: 是否重新计算总未读数（默认true）

**更新流程**：
1. 更新`conversationUnreadMap`
2. 同步更新对话对象中的`unreadCount`（保持响应式）
3. 重新计算总未读数（从Map累加）

---

## 已知问题与限制

### 5.1 WebSocket心跳检测限制

**限制说明**：
- STOMP心跳帧可能是二进制帧，不一定会在`debug`回调中出现
- 依赖备用保活机制确保连接状态正确

**缓解措施**：
- 双机制保障：主机制+备用机制
- 日志记录：便于监控和调试

### 5.2 移动端响应式检测

**限制说明**：
- 窗口大小变化时需要重新检测移动端状态
- 可能存在短暂的延迟

**缓解措施**：
- 使用`detectMobile()`函数统一检测
- 在窗口大小变化和方向变化时重新检测

---

## 总结

### v1.1.1 核心成就

1. **WebSocket心跳机制完善**：
   - 双重心跳检测机制，确保连接稳定性
   - 心跳确认日志，便于监控和调试
   - 即使聊天记录未更新，也不会误判连接失效

2. **状态同步优化**：
   - 统一未读数管理，避免状态不一致
   - 解决WebSocket更新和API更新冲突
   - 总未读数自动计算，保证准确性

3. **移动端体验修复**：
   - 解决对话列表消失问题
   - 修复状态不一致导致的UI问题
   - 确保移动端和桌面端体验一致

4. **代码质量提升**：
   - 修复ESLint错误
   - 修复模板响应式变量访问问题
   - 统一代码风格和最佳实践

### 下一步规划

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
- **v1.1.1**：专注于**用户体验优化**（UI、交互、实时更新、稳定性），不涉及性能优化
- **v1.2.0+**：专注于**性能优化和系统优化**（缓存、索引、并发控制、监控等）

---

**文档版本**：v1.1.1  
**最后更新**：2025-11-02  
**维护者**：NJUMarket 开发团队

