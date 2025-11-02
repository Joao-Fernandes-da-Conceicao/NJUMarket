# WebSocket 从零到实现：NJUMarket 实时消息推送教程

## 📖 第一部分：理论基础

### WebSocket 在网络协议栈中的位置

#### OSI 七层模型 vs TCP/IP 四层模型

WebSocket 位于**应用层**，但它的工作方式与传统 HTTP 有本质区别：

```
┌─────────────────────────────────────┐
│  应用层 (Application Layer)          │
│  HTTP / WebSocket / FTP / SMTP      │
├─────────────────────────────────────┤
│  传输层 (Transport Layer)            │
│  TCP / UDP                          │
├─────────────────────────────────────┤
│  网络层 (Network Layer)              │
│  IP                                 │
├─────────────────────────────────────┤
│  数据链路层 / 物理层                 │
│  Ethernet / WiFi / 4G               │
└─────────────────────────────────────┘
```

**关键点**：
- **HTTP**: 应用层协议，基于 TCP
- **WebSocket**: 应用层协议，也基于 TCP
- 两者都使用同一个传输层（TCP），但工作方式不同

#### TCP 连接的生命周期

```
传统 HTTP（短连接）：
客户端 ──TCP连接建立──> 服务器
客户端 <──HTTP请求/响应── 服务器
客户端 ──TCP连接关闭──> 服务器
（每次请求都建立和关闭连接）

WebSocket（长连接）：
客户端 ──TCP连接建立──> 服务器
客户端 <────WebSocket握手────> 服务器
客户端 <═══持久连接，双向通信═══> 服务器
（连接保持打开，双方可随时发送数据）
```

### WebSocket 协议基础

#### 1. 协议概述

**WebSocket 协议**（RFC 6455）是一个独立的**应用层协议**，定义在 TCP 之上。

**核心特征**：
- **全双工通信**：客户端和服务器可以同时发送和接收数据
- **持久连接**：一次握手，连接保持打开
- **低延迟**：无 HTTP 请求头开销，数据直接传输
- **双向性**：服务器可以主动推送数据，无需客户端请求

#### 2. WebSocket 握手过程

WebSocket 连接从 HTTP 握手开始：

```
1. 客户端发起握手请求（HTTP Upgrade）
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
GET /api/ws HTTP/1.1
Host: localhost:8080
Upgrade: websocket          ← 请求升级为 WebSocket
Connection: Upgrade          ← 请求升级连接
Sec-WebSocket-Key: dGhlIHNhbXBsZSBub25jZQ==  ← 随机密钥（Base64）
Sec-WebSocket-Version: 13    ← WebSocket 协议版本
Origin: http://localhost:8081  ← 来源域名
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

2. 服务器响应（HTTP 101 Switching Protocols）
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
HTTP/1.1 101 Switching Protocols
Upgrade: websocket          ← 同意升级
Connection: Upgrade          ← 连接已升级
Sec-WebSocket-Accept: s3pPLMBiTxaQ9kYGzzhZRbK+xOo=  ← 验证密钥
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

3. 握手完成，协议升级
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
此时连接已从 HTTP 协议升级为 WebSocket 协议
后续通信不再使用 HTTP，而是使用 WebSocket 帧格式
```

**握手的关键点**：

1. **Sec-WebSocket-Key**：
   - 客户端生成一个随机字符串（Base64 编码）
   - 服务器用这个密钥计算 `Sec-WebSocket-Accept`
   - 用于验证握手请求的有效性（防止缓存代理误认为是普通 HTTP）

2. **计算 Sec-WebSocket-Accept**：
   ```java
   String key = "dGhlIHNhbXBsZSBub25jZQ==";
   String magic = "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";  // 固定的魔法字符串
   String accept = Base64.encode(SHA1(key + magic));
   ```

3. **HTTP 101 状态码**：
   - 表示协议切换成功
   - 连接保持打开状态
   - 后续通信使用 WebSocket 帧

#### 3. WebSocket 帧格式

握手完成后，数据以**帧（Frame）**的形式传输：

```
┌─────────────────────────────────────────────────┐
│ Frame Header (2-14 bytes)                       │
├─────────────────────────────────────────────────┤
│ FIN (1 bit)    : 是否为最后一帧                  │
│ RSV1-3 (3 bits): 保留字段                        │
│ Opcode (4 bits): 帧类型                          │
│   - 0x0: 连续帧                                  │
│   - 0x1: 文本帧                                  │
│   - 0x2: 二进制帧                                 │
│   - 0x8: 关闭帧                                  │
│   - 0x9: Ping 帧                                 │
│   - 0xA: Pong 帧                                 │
│ Mask (1 bit)   : 是否掩码（客户端必须为1）         │
│ Payload Len (7/16/64 bits): 数据长度             │
│ Masking-Key (32 bits, 可选): 掩码密钥            │
├─────────────────────────────────────────────────┤
│ Payload Data (N bytes)                          │
│   实际传输的数据                                  │
└─────────────────────────────────────────────────┘
```

**帧类型示例**：

1. **文本帧**（发送文本消息）：
   ```
   FIN=1, Opcode=0x1, Payload="Hello World"
   ```

2. **二进制帧**（发送二进制数据）：
   ```
   FIN=1, Opcode=0x2, Payload=[0x01, 0x02, 0x03]
   ```

3. **控制帧**（Ping/Pong 保持连接）：
   ```
   FIN=1, Opcode=0x9  // Ping
   FIN=1, Opcode=0xA  // Pong（自动响应）
   ```

4. **关闭帧**（关闭连接）：
   ```
   FIN=1, Opcode=0x8, Payload="关闭原因代码"
   ```

#### 4. 掩码机制

**为什么需要掩码？**

WebSocket 协议要求**客户端发送的数据必须掩码**，服务器发送的数据不掩码。

**原因**：
- 防止代理服务器缓存或修改数据
- 提高安全性（防止恶意脚本注入）
- 协议设计的安全措施

**掩码过程**：
```javascript
// 客户端发送 "Hello"（未掩码）
"Hello" → [0x48, 0x65, 0x6C, 0x6C, 0x6F]

// 使用掩码密钥 XOR
maskingKey = [0x37, 0xFA, 0x21, 0x3D]
掩码后 = [0x48^0x37, 0x65^0xFA, 0x6C^0x21, 0x6C^0x3D, 0x6F^0x37]
        = [0x7F, 0x9F, 0x4D, 0x51, 0x58]

// 服务器接收后，用相同的密钥 XOR 还原
0x7F^0x37 = 0x48  → "H"
0x9F^0xFA = 0x65  → "e"
...
```

**注意**：在实际开发中，我们通常使用封装好的库（如 SockJS、STOMP），不需要手动处理掩码。

#### 5. WebSocket vs HTTP 对比

| 特性 | HTTP | WebSocket |
|-----|------|-----------|
| **连接方式** | 短连接（请求-响应后关闭） | 长连接（持久保持） |
| **通信方向** | 单向（客户端请求） | 双向（服务器可主动推送） |
| **开销** | 每次请求都带 HTTP 头 | 一次握手，后续只有帧头 |
| **实时性** | 需要轮询，有延迟 | 实时推送，无延迟 |
| **适用场景** | 传统网页浏览、REST API | 实时聊天、游戏、通知推送 |

**数据大小对比**：

```
HTTP 请求示例（约 200+ 字节）：
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
GET /api/messages HTTP/1.1
Host: localhost:8080
User-Agent: Mozilla/5.0...
Accept: application/json
Cookie: token=eyJhbGc...
Content-Type: application/json
Content-Length: 0
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
（每次请求都要发送完整的头信息）

WebSocket 帧示例（约 10 字节头 + 数据）：
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
[Frame Header: 2 bytes] + "Hello" (5 bytes) = 7 bytes
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
（握手后只需发送帧头和数据）
```

#### 6. STOMP 协议（应用层协议）

**STOMP（Simple Text Oriented Messaging Protocol）** 是基于 WebSocket 的**应用层消息协议**。

**为什么需要 STOMP？**

原始 WebSocket 只提供底层的二进制/文本帧传输，不包含：
- 消息队列（Queue）和主题（Topic）
- 订阅/取消订阅机制
- 消息路由规则
- 用户目标（User Destination）

**STOMP 提供了消息中间件的抽象**：

```
┌─────────────────────────────────────────┐
│ 应用层（STOMP）                           │
│  - 消息订阅/发布                          │
│  - 队列和主题                            │
│  - 用户目标路由                          │
├─────────────────────────────────────────┤
│ 传输层（WebSocket）                       │
│  - 帧格式                                │
│  - 双向通信                              │
│  - 持久连接                              │
├─────────────────────────────────────────┤
│ TCP/IP                                   │
│  - 可靠传输                              │
│  - 连接管理                              │
└─────────────────────────────────────────┘
```

**STOMP 帧格式**：

```
COMMAND
header1:value1
header2:value2

Body^@
```

**示例：客户端订阅消息**：

```
SUBSCRIBE
id:sub-0
destination:/user/queue/message

^@
```

**示例：服务器推送消息**：

```
MESSAGE
subscription:sub-0
message-id:msg-123
destination:/user/user_003/queue/message
content-type:application/json

{"messageId":"MSG_123","content":"Hello"}^@
```

**STOMP 命令**：
- `CONNECT`: 建立连接
- `SUBSCRIBE`: 订阅目标
- `SEND`: 发送消息
- `UNSUBSCRIBE`: 取消订阅
- `DISCONNECT`: 断开连接
- `MESSAGE`: 服务器推送的消息（客户端接收）

#### 7. 心跳机制（Keep-Alive）

**为什么需要心跳？**

WebSocket 是长连接，但中间网络设备（如 NAT、防火墙、代理）可能：
- 长时间无数据时关闭"空闲"连接
- 认为连接已断开，丢弃数据包

**解决方案：心跳（Ping/Pong）**

```
客户端                   中间设备                服务器
  │                        │                     │
  │────Ping帧 (每10秒)──>│────>                │
  │                        │                     │
  │                        │         Pong帧      │
  │<───────────────────────│<────────            │
  │                        │                     │
```

**作用**：
1. **保持连接活跃**：告诉中间设备连接仍然在使用
2. **检测连接状态**：如果 Pong 超时，说明连接已断开
3. **自动重连**：检测到断开后，客户端自动重新连接

**在本项目中的实现**：

```javascript
this.stompClient = new StompClient({
    heartbeatIncoming: 10000,   // 每 10 秒接收一次心跳
    heartbeatOutgoing: 10000    // 每 10 秒发送一次心跳
})
```

#### 8. WebSocket 连接状态机

```
┌──────────┐
│ CLOSED   │ (初始状态)
└────┬─────┘
     │ 客户端发起连接
     ▼
┌──────────┐
│CONNECTING│ (正在握手)
└────┬─────┘
     │ 握手成功 (HTTP 101)
     ▼
┌──────────┐
│  OPEN    │ (连接已建立)
└────┬─────┘
     │
     ├──> 发送/接收数据
     │
     ├──> Ping/Pong 心跳
     │
     └──> 关闭连接
           │
           ▼
     ┌──────────┐
     │ CLOSING  │ (正在关闭)
     └────┬─────┘
          │ 关闭完成
          ▼
     ┌──────────┐
     │  CLOSED  │ (已关闭)
     └──────────┘
```

**状态转换**：
- **CLOSED → CONNECTING**: 客户端调用 `connect()`
- **CONNECTING → OPEN**: 握手成功，收到 HTTP 101
- **OPEN → CLOSING**: 调用 `close()` 或网络错误
- **CLOSING → CLOSED**: 关闭完成

---

## 📚 第二部分：应用实践

### 什么是 WebSocket？

### 传统 HTTP 的问题

在 WebSocket 出现之前，要实现实时通信（如聊天），通常有两种方式：

1. **轮询（Polling）**：前端每隔几秒向服务器发送请求，询问是否有新消息
   ```javascript
   // 每 3 秒轮询一次
   setInterval(() => {
     fetch('/api/messages')
   }, 3000)
   ```
   - ❌ 浪费带宽（即使没有新消息也要请求）
   - ❌ 有延迟（最多要等 3 秒才能收到消息）
   - ❌ 服务器压力大（大量无用请求）

2. **长轮询（Long Polling）**：前端发送请求，服务器保持连接，有新消息时立即返回
   - ❌ 实现复杂
   - ❌ 仍然有 HTTP 请求开销

### WebSocket 的优势

**WebSocket 是双向、持久、全双工的连接**：

```
传统 HTTP（请求-响应）：
客户端 → 请求 → 服务器
客户端 ← 响应 ← 服务器
（连接关闭）

WebSocket（持久连接）：
客户端 ←→ 服务器（连接保持，双方可随时发送消息）
```

- ✅ **实时性**：消息立即推送，无需等待
- ✅ **效率高**：只需一次握手，后续无 HTTP 开销
- ✅ **双向通信**：服务器可以主动推送消息给客户端

## 🏗️ WebSocket 架构概览

### 基本概念

```
┌─────────────┐                    ┌─────────────┐
│   前端      │                    │   后端      │
│  (浏览器)   │                    │ (Spring)    │
└──────┬──────┘                    └──────┬──────┘
       │                                  │
       │  1. 发起 WebSocket 连接          │
       ├─────────────────────────────────>│
       │  2. 握手（HTTP Upgrade）         │
       │<─────────────────────────────────┤
       │                                  │
       │  3. 连接建立（双向通道）          │
       │<═════════════════════════════════>│
       │                                  │
       │  4. 订阅消息队列                  │
       ├─────────────────────────────────>│
       │                                  │
       │  5. 随时发送/接收消息             │
       │<═════════════════════════════════>│
       │                                  │
```

### STOMP 协议

**STOMP（Simple Text Oriented Messaging Protocol）** 是基于 WebSocket 的应用层协议，提供了：
- 消息订阅/发布机制
- 消息队列（Queue）和主题（Topic）
- 用户目标（User Destination）：点对点消息

本项目使用 **Spring WebSocket + STOMP**，架构如下：

```
前端: SockJS + STOMP Client
     ↓
WebSocket 连接
     ↓
后端: Spring WebSocket + SimpleBroker
     ↓
消息路由和推送
```

## 🔧 实现步骤详解

### 步骤 1: 后端配置 - 注册 WebSocket 端点

**文件**: `WebSocketConfig.java`

```java
@Override
public void registerStompEndpoints(StompEndpointRegistry registry) {
    // 注册端点：客户端通过 ws://localhost:8080/api/ws 连接
    registry.addEndpoint("/api/ws")
            .setAllowedOrigins("http://localhost:8081")  // 允许的前端域名
            .addInterceptors(handshakeInterceptor)        // 拦截器（用于身份验证）
            .withSockJS();  // 支持 SockJS（浏览器兼容性）
}
```

**作用**：告诉 Spring 在哪里接收 WebSocket 连接请求。

---

### 步骤 2: 身份验证 - 握手拦截器

**文件**: `WebSocketHandshakeInterceptor.java`

**问题**：WebSocket 连接时，如何知道用户是谁？

**解决方案**：从 JWT token 中提取用户 ID

```java
@Override
public boolean beforeHandshake(...) {
    // 1. 从 URL 查询参数提取 token
    String token = extractTokenFromQuery(request);
    
    // 2. 验证 token 并获取 userId
    String userId = jwtUtils.getUserIdFromToken(token);
    
    // 3. 存入 session attributes（供后续使用）
    attributes.put("userId", userId);
    
    return true;  // 允许连接
}
```

**关键点**：`userId` 需要保存到 `sessionAttributes`，因为后续的 `ChannelInterceptor` 需要用它来设置用户身份。

---

### 步骤 3: 用户身份标识 - Principal

**文件**: `UserPrincipal.java`

**问题**：Spring 如何识别不同的用户连接？

**解决方案**：实现 `Principal` 接口，让 Spring 知道每个连接属于哪个用户

```java
public class UserPrincipal implements Principal {
    private final String userId;
    
    @Override
    public String getName() {
        return userId;  // Spring 用这个来识别用户
    }
}
```

**为什么需要 Principal？**

当服务器想推送消息给用户 A 时，Spring 需要知道：
- 哪个连接属于用户 A？
- `convertAndSendToUser("userIdA", ...)` 需要匹配 `Principal.getName() == "userIdA"` 的连接

---

### 步骤 4: 设置 Principal - 通道拦截器

**文件**: `WebSocketConfig.java` → `configureClientInboundChannel()`

**问题**：什么时候设置 Principal？

**关键时机**：在 **CONNECT 帧**时设置（最早时机）

```java
if (StompCommand.CONNECT.equals(command)) {
    // 1. 从 session attributes 读取 userId（握手拦截器已设置）
    String userId = (String) sessionAttributes.get("userId");
    
    // 2. 创建 Principal
    UserPrincipal principal = new UserPrincipal(userId);
    
    // 3. 设置到 accessor（让 Spring 知道这个连接属于哪个用户）
    accessor.setUser(principal);
}
```

**为什么在 CONNECT 帧时设置？**
- 这样在后续的 SUBSCRIBE 帧时，Principal 已经存在
- Spring 注册订阅时会使用 Principal 来识别用户

---

### 步骤 5: 配置消息代理 - SimpleBroker

**文件**: `WebSocketConfig.java` → `configureMessageBroker()`

```java
@Override
public void configureMessageBroker(MessageBrokerRegistry config) {
    // 1. 启用简单内存消息代理
    // 处理以 /topic 或 /queue 开头的目标
    config.enableSimpleBroker("/topic", "/queue");
    
    // 2. 设置用户目标前缀
    // 用于点对点消息：convertAndSendToUser(userId, "/queue/message")
    // → 实际发送到 /user/{userId}/queue/message
    config.setUserDestinationPrefix("/user");
}
```

**⚠️ 重要陷阱**：
```java
// ❌ 错误：不要在这里包含 "/user"
config.enableSimpleBroker("/topic", "/queue", "/user");  

// ✅ 正确："/user" 由 setUserDestinationPrefix() 单独处理
config.enableSimpleBroker("/topic", "/queue");
config.setUserDestinationPrefix("/user");
```

**为什么？**
- `/user` 是**用户目标前缀**，不是 broker 前缀
- 包含 `/user` 会导致路由冲突，消息无法正确推送

---

### 步骤 6: 前端连接 - WebSocket 客户端

**文件**: `websocket.js`

```javascript
connect() {
    // 1. 获取 JWT token
    const token = localStorage.getItem('token')
    
    // 2. 构建连接 URL（包含 token）
    const wsUrl = `http://localhost:8080/api/ws?token=${token}`
    
    // 3. 使用 SockJS 创建连接（SockJS 提供浏览器兼容性）
    this.sock = new SockJS(wsUrl)
    
    // 4. 创建 STOMP 客户端
    this.stompClient = new StompClient({
        webSocketFactory: () => this.sock,
        reconnectDelay: 5000,      // 重连延迟
        heartbeatIncoming: 10000,   // 心跳检测（保持连接活跃）
        heartbeatOutgoing: 10000
    })
    
    // 5. 连接成功后订阅消息队列
    this.stompClient.onConnect = () => {
        this.subscribeToMessages()
    }
    
    // 6. 激活连接
    this.stompClient.activate()
}
```

---

### 步骤 7: 订阅消息队列

**文件**: `websocket.js` → `subscribeToMessages()`

```javascript
subscribeToMessages() {
    // 订阅用户专属队列
    // Spring 会自动将 /user/queue/message 转换为 /user/{userId}/queue/message
    this.stompClient.subscribe('/user/queue/message', (message) => {
        const messageData = JSON.parse(message.body)
        this.handleMessage(messageData)  // 处理收到的消息
    })
}
```

**消息路由流程**：
```
前端订阅: /user/queue/message
    ↓
Spring 转换: /user/{Principal.getName()}/queue/message
    ↓
后端推送: convertAndSendToUser("userId", "/queue/message")
    ↓
Spring 转换: /user/{userId}/queue/message
    ↓
匹配: Principal.getName() == userId → 消息送达 ✅
```

---

### 步骤 8: 后端推送消息

**文件**: `ContactServiceImpl.java` → `sendMessage()`

```java
// 1. 保存消息到数据库
Message message = new Message();
// ... 设置消息内容 ...
messageRepository.save(message);

// 2. 通过 WebSocket 推送消息
String receiverId = request.getReceiverId();
messagingTemplate.convertAndSendToUser(
    receiverId,              // 接收方用户ID（必须与 Principal.getName() 完全匹配）
    "/queue/message",        // 目标路径
    messageDTO               // 消息内容
);
```

**关键点**：
- `receiverId` 必须与订阅时的 `Principal.getName()` **完全一致**（包括大小写）
- Spring 会自动将目标转换为 `/user/{receiverId}/queue/message`
- 如果找到匹配的订阅，消息会立即推送到前端

---

### 步骤 9: 前端处理消息

**文件**: `stores/message.js` → `handleWebSocketMessage()`

```javascript
handleWebSocketMessage(messageData) {
    // 1. 更新对话列表（最后消息、未读数）
    let conversation = this.conversations.find(c => 
        c.conversationId === messageData.conversationId
    )
    conversation.lastMessageContent = messageData.content
    conversation.unreadCount++
    
    // 2. 如果当前对话被选中，添加到消息列表
    if (messageData.conversationId === this.selectedConversationId) {
        this.messages.push(messageData)
    }
    
    // 3. Vue 3 响应式系统自动更新 UI
}
```

**为什么 UI 会自动更新？**
- Vue 3 的响应式系统（`ref`, `reactive`）会检测数据变化
- 修改 `this.messages` 或 `this.conversations` 会自动触发组件重新渲染

---

## 🔑 关键概念总结

### 1. 连接建立流程

```
1. 前端: SockJS.connect('/api/ws?token=xxx')
   ↓
2. 后端: WebSocketHandshakeInterceptor
   → 验证 token，提取 userId
   → 存入 sessionAttributes
   ↓
3. 后端: ChannelInterceptor (CONNECT 帧)
   → 从 sessionAttributes 读取 userId
   → 创建 UserPrincipal
   → 设置 Principal
   ↓
4. 前端: 订阅 /user/queue/message
   ↓
5. 后端: ChannelInterceptor (SUBSCRIBE 帧)
   → 验证 Principal 存在
   → Spring 注册订阅：/user/{Principal.getName()}/queue/message
```

### 2. 消息推送流程

```
1. 后端: messagingTemplate.convertAndSendToUser("userId", "/queue/message")
   ↓
2. Spring: 转换为目标 /user/{userId}/queue/message
   ↓
3. Spring: 查找匹配的订阅（Principal.getName() == userId）
   ↓
4. Spring: 推送消息到匹配的连接
   ↓
5. 前端: 收到消息，触发 handleMessage()
   ↓
6. 前端: 更新 Vue 状态，UI 自动刷新
```

### 3. Principal 的重要性

**Principal 是什么？**
- Java `Principal` 接口表示用户的身份标识
- 本项目用 `UserPrincipal` 实现，`getName()` 返回 `userId`

**为什么重要？**
- Spring 用 `Principal.getName()` 来识别连接属于哪个用户
- `convertAndSendToUser(userId, ...)` 匹配时，需要 `Principal.getName() == userId`

**设置时机很关键**：
- ✅ **正确**：在 CONNECT 帧时设置 → 后续所有帧都有 Principal
- ❌ **错误**：在 SUBSCRIBE 帧时才设置 → Spring 可能已经注册了订阅，Principal 为 null

---

## 🐛 常见问题和解决方案

### 问题 1: 前端收不到消息

**检查清单**：
1. ✅ Principal 是否在 CONNECT 帧时设置？
2. ✅ `Principal.getName()` 是否与 `receiverId` 完全匹配？
3. ✅ SimpleBroker 配置是否正确（不包含 `/user`）？
4. ✅ 前端是否成功订阅？

**调试方法**：
```javascript
// 前端控制台
wsClient.isConnected  // 应该是 true
wsClient.subscriptions.size  // 应该是 1

// 后端日志
Principal set in CONNECT: userId=...
Principal verified in SUBSCRIBE: userId=...
```

### 问题 2: 单向消息传递（A→B 正常，B→A 失败）

**原因**：Principal 设置时机过晚

**解决**：确保在 CONNECT 帧时设置 Principal，而不是在 SUBSCRIBE 帧时

### 问题 3: 热重载后失效

**原因**：Spring DevTools 热重载时，SimpleBroker 的内存状态丢失

**解决**：
- 刷新前端页面（强制重新连接）
- 或手动重连：`wsClient.disconnect(); wsClient.connect();`

---

## 📝 完整代码流程回顾

### 后端关键文件

1. **WebSocketConfig.java**
   - 注册端点、配置 broker、设置 Principal

2. **WebSocketHandshakeInterceptor.java**
   - 握手时验证 token，提取 userId

3. **UserPrincipal.java**
   - 用户身份标识实现

4. **ContactServiceImpl.java**
   - 推送消息：`messagingTemplate.convertAndSendToUser()`

### 前端关键文件

1. **websocket.js**
   - 连接、订阅、处理消息

2. **stores/message.js**
   - 消息状态管理、UI 更新

---

## 🎯 学习要点

1. **WebSocket 是持久连接**，不像 HTTP 每次都需要建立连接
2. **Principal 是用户身份的标识**，必须在 CONNECT 帧时设置
3. **订阅和推送需要匹配**：`Principal.getName() == receiverId`
4. **SimpleBroker 配置要正确**：不要包含 `/user` 前缀
5. **前端需要自动重连**：处理连接断开的情况

---

## 📚 进一步学习

- **Spring WebSocket 官方文档**：https://docs.spring.io/spring-framework/reference/web/websocket.html
- **STOMP 协议规范**：https://stomp.github.io/
- **SockJS 文档**：https://github.com/sockjs/sockjs-client

---

## ✅ 总结

WebSocket 实时消息推送的核心流程：

```
1. 前端连接 → 2. 身份验证 → 3. 设置 Principal → 4. 订阅队列
    ↓
5. 后端推送 → 6. Spring 路由 → 7. 前端接收 → 8. UI 更新
```

**关键成功因素**：
- ✅ Principal 设置时机正确（CONNECT 帧）
- ✅ 配置正确（SimpleBroker 不包含 `/user`）
- ✅ userId 完全匹配（大小写一致）
- ✅ 前端自动重连机制

通过本项目的实现，你学会了：
- 如何建立 WebSocket 连接
- 如何进行身份验证
- 如何实现点对点消息推送
- 如何处理连接断开和重连
- 如何与 Vue 3 响应式系统集成

现在你可以基于这个基础，扩展更多实时功能，如在线状态、通知推送等！

