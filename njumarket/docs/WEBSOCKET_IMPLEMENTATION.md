# WebSocket 实时消息推送实现总结

## 概述

本项目实现了基于 Spring WebSocket (STOMP) 的实时消息推送功能，支持用户之间的点对点消息实时推送。

## 技术栈

- **后端**: Spring WebSocket + STOMP + SimpleBroker
- **前端**: SockJS + @stomp/stompjs
- **协议**: STOMP over WebSocket

## 核心配置

### 后端配置

#### 1. WebSocket 配置 (`WebSocketConfig.java`)

```java
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    
    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // SimpleBroker 支持的前缀：/topic, /queue
        // 注意：不要包含 "/user"，它由 setUserDestinationPrefix() 单独处理
        config.enableSimpleBroker("/topic", "/queue");
        
        // 客户端发送消息的前缀（未使用）
        config.setApplicationDestinationPrefixes("/app");
        
        // 用户目标前缀，用于点对点消息
        // convertAndSendToUser(userId, "/queue/message") 
        // -> 实际发送到 /user/{userId}/queue/message
        config.setUserDestinationPrefix("/user");
    }
    
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // 注册 WebSocket 端点
        registry.addEndpoint("/api/ws")
                .setAllowedOrigins("http://localhost:8081")
                .addInterceptors(handshakeInterceptor)
                .withSockJS(); // 支持 SockJS
        
        // 同时支持原生 WebSocket
        registry.addEndpoint("/api/ws")
                .setAllowedOrigins("http://localhost:8081")
                .addInterceptors(handshakeInterceptor);
    }
    
    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        // 在 CONNECT/SUBSCRIBE 帧时设置 Principal
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
                
                if (accessor != null) {
                    StompCommand command = accessor.getCommand();
                    
                    // CRITICAL: 在 CONNECT 帧时设置 Principal（最早时机）
                    if (StompCommand.CONNECT.equals(command)) {
                        String userId = null;
                        Map<String, Object> sessionAttributes = accessor.getSessionAttributes();
                        if (sessionAttributes != null) {
                            userId = (String) sessionAttributes.get("userId");
                        }
                        
                        if (userId != null) {
                            UserPrincipal principal = new UserPrincipal(userId);
                            accessor.setUser(principal);
                        }
                    }
                    
                    // 在 SUBSCRIBE 帧时验证 Principal（应该已在 CONNECT 时设置）
                    if (StompCommand.SUBSCRIBE.equals(command)) {
                        Principal existingPrincipal = accessor.getUser();
                        if (existingPrincipal == null) {
                            // Fallback: 从 sessionAttributes 重新获取
                            Map<String, Object> sessionAttributes = accessor.getSessionAttributes();
                            if (sessionAttributes != null) {
                                String userId = (String) sessionAttributes.get("userId");
                                if (userId != null) {
                                    UserPrincipal principal = new UserPrincipal(userId);
                                    accessor.setUser(principal);
                                }
                            }
                        }
                    }
                }
                
                return message;
            }
        });
    }
}
```

#### 2. 握手拦截器 (`WebSocketHandshakeInterceptor.java`)

验证 JWT token 并提取 userId，存入 session attributes：

```java
@Override
public boolean beforeHandshake(ServerHttpRequest request, ...) {
    // 从查询参数提取 token
    String token = extractTokenFromQuery(queryString);
    
    // 验证 token 并获取 userId
    String userId = jwtUtils.getUserIdFromToken(token);
    
    // 保存到 session attributes
    attributes.put("userId", userId);
    
    return true;
}
```

#### 3. 用户身份标识 (`UserPrincipal.java`)

实现 `Principal` 接口，`getName()` 返回 userId：

```java
public class UserPrincipal implements Principal {
    private final String userId;
    
    @Override
    public String getName() {
        return userId;
    }
}
```

#### 4. 消息推送 (`ContactServiceImpl.java`)

```java
// 使用 SimpMessagingTemplate 推送消息
messagingTemplate.convertAndSendToUser(
    receiverId,              // 必须与订阅时的 Principal.getName() 完全匹配
    "/queue/message",       // 目标路径
    messageDTO              // 消息内容
);
```

### 前端配置

#### WebSocket 客户端 (`websocket.js`)

```javascript
import SockJS from 'sockjs-client'
import { Client as StompClient } from '@stomp/stompjs'

class WebSocketClient {
  connect() {
    const token = localStorage.getItem('token')
    const wsUrl = `http://localhost:8080/api/ws?token=${token}`
    
    this.sock = new SockJS(wsUrl)
    this.stompClient = new StompClient({
      webSocketFactory: () => this.sock,
      reconnectDelay: 5000,
      heartbeatIncoming: 10000,
      heartbeatOutgoing: 10000
    })
    
    this.stompClient.onConnect = () => {
      this.subscribeToMessages()
      this.emit('connected')
    }
    
    this.stompClient.activate()
  }
  
  subscribeToMessages() {
    // 订阅用户专属队列
    // Spring 会自动将 /user/queue/message 映射为 /user/{userId}/queue/message
    this.stompClient.subscribe('/user/queue/message', (message) => {
      const messageData = JSON.parse(message.body)
      this.handleMessage(messageData)
    })
  }
}
```

## 关键实现要点

### 1. Principal 设置时机

**问题**: `convertAndSendToUser()` 通过 `Principal.getName()` 匹配用户，但 Principal 必须在订阅时已设置。

**解决方案**: 
- 在 `configureClientInboundChannel` 中拦截 CONNECT、CONNECTED、SUBSCRIBE 帧
- 从 session attributes 获取 userId（握手拦截器设置）
- 创建 `UserPrincipal` 并设置到 `StompHeaderAccessor`

### 2. userId 匹配

**关键**: 订阅时的 `Principal.getName()` 必须与推送时的 `receiverId` **完全一致**（包括大小写）。

例如：
- 订阅时：`Principal.getName() = "USER_1761133111693_499"`
- 推送时：`receiverId = "USER_1761133111693_499"` ✅
- 推送时：`receiverId = "user_1761133111693_499"` ❌ (大小写不匹配)

### 3. SimpleBroker 配置

**重要**：`enableSimpleBroker()` 中**不要**包含 `/user` 前缀！

```java
// ✅ 正确配置
config.enableSimpleBroker("/topic", "/queue");  // 不包含 "/user"
config.setUserDestinationPrefix("/user");        // "/user" 单独处理

// ❌ 错误配置（会导致消息路由失败）
config.enableSimpleBroker("/topic", "/queue", "/user");  // 包含 "/user" 会冲突
config.setUserDestinationPrefix("/user");
```

### 4. 消息路由流程

1. **客户端订阅**: `/user/queue/message`
2. **Spring 转换**: `/user/{Principal.getName()}/queue/message`
3. **服务器推送**: `convertAndSendToUser(userId, "/queue/message")`
4. **Spring 转换**: `/user/{userId}/queue/message`
5. **匹配条件**: `Principal.getName() == userId` 时消息路由成功

## 消息处理流程

### 发送消息

1. 前端调用 `sendMessage()` API
2. 后端 `ContactServiceImpl.sendMessage()` 保存消息到数据库
3. 后端使用 `messagingTemplate.convertAndSendToUser()` 推送消息
4. Spring WebSocket 根据 `receiverId` 和 Principal 匹配路由消息
5. 接收方前端收到消息并更新 UI

### 接收消息

1. 前端 `WebSocketClient.handleMessage()` 接收消息
2. 触发 `MESSAGE_NEW` 事件
3. `messageStore.handleWebSocketMessage()` 处理：
   - 更新对话列表（最后消息、未读数、排序）
   - 如果当前对话被选中，添加到消息列表
   - 更新未读数角标
4. Vue 3 响应式系统自动更新 UI

## 遇到的问题及解决方案

### 问题 1: Principal 未设置导致消息无法路由

**现象**: 消息推送成功，但前端收不到

**原因**: SUBSCRIBE 帧时 Principal 为 null，导致无法匹配用户

**解决**: 在 `ChannelInterceptor` 中拦截 SUBSCRIBE 帧并设置 Principal

### 问题 2: userId 格式不一致

**现象**: Principal 匹配失败

**原因**: 订阅时的 userId 格式与推送时的 receiverId 格式不一致

**解决**: 确保两者使用相同的 userId 格式（从 JWT token 中提取）

### 问题 3: SimpleBroker 配置不完整

**现象**: 消息无法路由到客户端

**原因**: SimpleBroker 只配置了 `/user` 前缀，缺少 `/topic` 和 `/queue`

**解决**: 配置 `enableSimpleBroker("/topic", "/queue", "/user")`

## 依赖配置

### 后端 (`pom.xml`)

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-websocket</artifactId>
</dependency>
<dependency>
    <groupId>jakarta.annotation</groupId>
    <artifactId>jakarta.annotation-api</artifactId>
</dependency>
```

### 前端 (`package.json`)

```json
{
  "dependencies": {
    "sockjs-client": "^1.6.1",
    "@stomp/stompjs": "^7.0.0"
  }
}
```

## 部署注意事项

### 开发环境

- 后端端口: `8080`
- 前端端口: `8081`
- WebSocket 端点: `ws://localhost:8080/api/ws`

### 生产环境

需要配置 Nginx 代理：

```nginx
location /api/ws {
    proxy_pass http://backend:8080;
    proxy_http_version 1.1;
    proxy_set_header Upgrade $http_upgrade;
    proxy_set_header Connection "upgrade";
    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
    proxy_read_timeout 86400;
}
```

## 性能优化建议

1. **连接管理**: 当前使用 SimpleBroker，适合小规模部署
2. **扩展性**: 生产环境建议使用 Redis 或 RabbitMQ 作为消息代理
3. **心跳机制**: 已配置 10 秒心跳，保持连接活跃
4. **重连机制**: 前端已实现自动重连（最多 5 次）

## 测试验证

### 验证步骤

1. 打开两个浏览器窗口，分别登录不同用户
2. 用户 A 发送消息给用户 B
3. 检查：
   - 后端日志：消息推送成功
   - 前端控制台：收到消息
   - UI 更新：对话列表、消息列表、未读数自动更新

### 成功指标

- ✅ WebSocket 连接建立
- ✅ 订阅成功
- ✅ Principal 正确设置
- ✅ 消息推送成功
- ✅ 前端收到消息
- ✅ UI 自动更新

## 部署流程

### 开发环境部署

1. **后端启动**
   ```bash
   cd njumarket
   mvn spring-boot:run
   ```
   后端将在 `http://localhost:8080` 启动

2. **前端启动**
   ```bash
   cd njumarket-front/NJUMarket
   npm run serve
   ```
   前端将在 `http://localhost:8081` 启动

3. **验证连接**
   - 打开浏览器开发者工具，查看控制台
   - 登录后应看到：`WebSocket STOMP connected successfully`
   - 应看到：`Successfully subscribed to: /user/queue/message`
   - 后端日志应显示：`Principal set in CONNECT` 和 `Principal verified in SUBSCRIBE`

### 生产环境部署

#### Nginx 配置

```nginx
# WebSocket 代理配置
location /api/ws {
    proxy_pass http://backend:8080;
    proxy_http_version 1.1;
    
    # WebSocket 必需的头部
    proxy_set_header Upgrade $http_upgrade;
    proxy_set_header Connection "upgrade";
    
    # 传递必要的信息
    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    proxy_set_header X-Forwarded-Proto $scheme;
    
    # 超时设置（WebSocket 连接需要长时间保持）
    proxy_read_timeout 86400;
    proxy_send_timeout 86400;
    proxy_connect_timeout 60;
}

# 普通 API 代理
location /api {
    proxy_pass http://backend:8080;
    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
}
```

#### 前端配置调整

在生产环境中，前端需要自动识别当前协议和域名：

```javascript
// websocket.js 中的连接配置会自动适应
const protocol = window.location.protocol === 'https:' ? 'https:' : 'http:'
const host = process.env.NODE_ENV === 'production' 
  ? window.location.host  // 生产环境：通过 Nginx 代理
  : 'localhost:8080'      // 开发环境：直接连接后端
```

## Debug 经验总结

### 问题 1: 单向消息传递（A→B 正常，B→A 失败）

**现象**：
- 用户 A 向用户 B 发送消息，B 能正常收到
- 用户 B 向用户 A 发送消息，A 收不到

**原因分析**：
- Principal 在 SUBSCRIBE 帧时才设置，但 Spring 可能已经处理了订阅注册
- Principal 需要在 CONNECT 帧时就设置，确保后续所有帧都能访问

**解决方案**：
- 在 `configureClientInboundChannel` 中，优先在 CONNECT 帧设置 Principal
- SUBSCRIBE 帧时验证 Principal 是否存在（应该已经存在）

**关键代码**：
```java
if (StompCommand.CONNECT.equals(command)) {
    // 在 CONNECT 帧时设置 Principal
    UserPrincipal principal = new UserPrincipal(userId);
    accessor.setUser(principal);
}

if (StompCommand.SUBSCRIBE.equals(command)) {
    // 验证 Principal 是否存在（应该在 CONNECT 时已设置）
    Principal existingPrincipal = accessor.getUser();
    // ... 验证和 fallback 逻辑
}
```

### 问题 2: 双方都收不到消息（配置冲突）

**现象**：
- 消息推送成功（后端日志显示 `WebSocket push completed`）
- 前端收不到任何消息
- WebSocket 连接时灵时不灵

**原因分析**：
- `enableSimpleBroker("/topic", "/queue", "/user")` 中包含了 `/user`
- 这与 `setUserDestinationPrefix("/user")` 产生冲突
- Spring 的 SimpleBroker 无法正确处理用户目标路由

**解决方案**：
- **关键修复**：从 `enableSimpleBroker` 中移除 `/user`
- 只保留：`enableSimpleBroker("/topic", "/queue")`
- `/user` 前缀由 `setUserDestinationPrefix("/user")` 单独处理

**修复前**：
```java
config.enableSimpleBroker("/topic", "/queue", "/user"); // ❌ 错误
config.setUserDestinationPrefix("/user");
```

**修复后**：
```java
config.enableSimpleBroker("/topic", "/queue"); // ✅ 正确
config.setUserDestinationPrefix("/user");
```

### 问题 3: Principal 未正确传递

**现象**：
- WebSocket 连接建立成功
- 订阅成功，但 Principal 为 null
- 消息推送失败

**解决方案**：
1. **握手拦截器**：在 `beforeHandshake` 中验证 JWT，将 `userId` 存入 `sessionAttributes`
2. **CONNECT 帧拦截器**：从 `sessionAttributes` 读取 `userId`，创建 `UserPrincipal` 并设置
3. **SUBSCRIBE 帧验证**：验证 Principal 是否存在，必要时使用 fallback

**关键代码流程**：
```
1. WebSocketHandshakeInterceptor.beforeHandshake()
   → 验证 JWT token
   → 提取 userId
   → 存入 sessionAttributes["userId"]

2. WebSocketConfig.configureClientInboundChannel()
   → CONNECT 帧：从 sessionAttributes 读取 userId
   → 创建 UserPrincipal
   → accessor.setUser(principal)

3. SUBSCRIBE 帧：验证 Principal 是否存在
   → 如果存在：正常处理
   → 如果不存在：fallback 从 sessionAttributes 重新设置
```

### 问题 4: 重启后连接失效

**现象**：
- 首次启动时 WebSocket 正常工作
- 后端或前端重启后，消息无法传递

**原因分析**：
- 前端可能没有在页面刷新时重新初始化 WebSocket
- 后端重启导致所有连接断开，前端没有自动重连

**解决方案**：
1. **前端自动重连机制**：已在 `websocket.js` 中实现自动重连（最多 5 次）
2. **用户 store 初始化**：在 `user.js` 的 `initUser()` 中自动初始化 WebSocket
3. **断开连接处理**：在 `user.js` 的 `logout()` 中调用 `disconnectWebSocket()`

**验证方法**：
```javascript
// 在浏览器控制台检查
wsClient.isConnected  // 应该是 true
wsClient.subscriptions.size  // 应该 > 0
```

### 问题 5: 后端热重载（Hot Reload）导致 WebSocket 失效

**现象**：
- 后端进行热重载（Spring DevTools 自动重启）后，WebSocket 功能失效
- 后端日志显示连接正常，但消息无法传递
- 必须同时重启前端和后端，WebSocket 才能恢复正常

**原因分析**：

Spring DevTools 的热重载机制会导致以下问题：

1. **WebSocket 连接状态不一致**：
   - DevTools 触发快速重启（Restart）时，WebSocket 服务器端被重新初始化
   - 原有的 WebSocket 连接被强制断开，但前端可能还没有检测到断开事件
   - 前端仍然认为连接是活跃的（`isConnected = true`），但实际上后端已经重新启动

2. **SimpleBroker 内存状态丢失**：
   - SimpleBroker 维护的订阅信息存储在内存中
   - 热重载时，所有订阅记录（包括 Principal 映射）都会丢失
   - 前端虽然保持着连接，但后端的订阅注册表中已经没有对应的记录
   - 即使消息推送成功，也无法找到对应的订阅者

3. **Session 和 Principal 状态不匹配**：
   - 热重载后，新的 Spring 应用上下文初始化
   - 旧的 WebSocket session 可能仍然存在，但无法正确关联到新的 Principal
   - `ChannelInterceptor` 中的 Principal 设置逻辑可能在新的上下文中执行失败

4. **前端重连机制未触发**：
   - 前端只检测到异常关闭（`event.wasClean = false` 且 `event.code !== 1000`）才会重连
   - 热重载可能导致连接以"正常关闭"的方式断开（`code = 1000`），前端不会自动重连
   - 或者连接看似正常，但实际已经失效

**技术细节**：

```
正常关闭流程：
后端热重载 → WebSocket 服务器关闭 → 发送关闭帧 → 前端收到关闭事件 → 检测到 wasClean=true → 不重连 ❌

异常关闭流程：
后端崩溃 → WebSocket 连接异常断开 → 前端收到关闭事件 → 检测到 wasClean=false → 自动重连 ✅
```

**解决方案**：

1. **禁用 Spring DevTools 对 WebSocket 的影响（推荐）**：
   ```xml
   <!-- pom.xml -->
   <dependency>
       <groupId>org.springframework.boot</groupId>
       <artifactId>spring-boot-devtools</artifactId>
       <optional>true</optional>
   </dependency>
   ```
   
   在 `application.properties` 中添加：
   ```properties
   # 禁用 DevTools 对 WebSocket 的重启
   spring.devtools.restart.exclude=com/njumarket/njumarket/websocket/**
   ```

2. **改进前端重连检测机制**：
   - 检测到连接断开后，无论是否正常关闭，都尝试重连
   - 增加心跳超时检测：如果长时间没有收到消息，主动断开并重连

3. **开发建议**：
   - **开发时**：如果修改了 WebSocket 相关代码，手动重启后端和刷新前端
   - **生产环境**：生产环境不使用 DevTools，不存在此问题
   - **测试时**：修改 WebSocket 配置后，确保同时重启前后端

**临时解决方案**：

如果热重载后 WebSocket 失效，可以：
1. 刷新前端页面（强制重新连接）
2. 或者在浏览器控制台执行：
   ```javascript
   wsClient.disconnect()
   wsClient.connect()
   ```

**验证方法**：

热重载后检查：
```javascript
// 检查连接状态
wsClient.isConnected  // 可能显示 true，但实际已失效

// 强制重新连接
wsClient.disconnect()
wsClient.connect()

// 检查订阅
wsClient.subscriptions.size  // 应该是 1（/user/queue/message）
```

### Debug 技巧

#### 1. 后端日志检查清单

启动后立即检查：
- ✅ `WebSocket endpoint registered: /api/ws`
- ✅ `Principal set in CONNECT: userId=...`
- ✅ `Principal verified in SUBSCRIBE: userId=..., destination=/user/queue/message`
- ✅ `Subscription verified: userId=..., Principal.getName()=...`

发送消息时检查：
- ✅ `Attempting WebSocket push: receiverId=..., messageId=...`
- ✅ `WebSocket push completed: receiverId=..., messageId=...`

#### 2. 前端控制台检查清单

连接时检查：
- ✅ `WebSocket STOMP connected successfully`
- ✅ `Subscribing to: /user/queue/message`
- ✅ `Successfully subscribed to: /user/queue/message subscription id: ...`

接收消息时检查：
- ✅ `Received WebSocket message: {...}`
- ✅ `Parsed message data: {...}`

#### 3. 常见错误排查

| 错误现象 | 可能原因 | 解决方法 |
|---------|---------|---------|
| 连接失败 | JWT token 无效或过期 | 检查 `localStorage.getItem('token')` |
| Principal 为 null | userId 未存入 sessionAttributes | 检查 `WebSocketHandshakeInterceptor` |
| 订阅失败 | Principal 未在 CONNECT 帧设置 | 确保在 CONNECT 帧时设置 Principal |
| 消息推送成功但收不到 | SimpleBroker 配置冲突 | 检查 `enableSimpleBroker` 是否包含 `/user` |
| 重启后失效 | 前端未重新连接 | 检查 `user.js` 的 `initUser()` 是否调用 `initWebSocket()` |
| 热重载后失效 | DevTools 导致连接状态不一致 | 刷新前端页面或手动重连；开发时避免修改 WebSocket 相关代码 |

#### 4. 测试验证步骤

1. **初始连接测试**
   ```bash
   # 后端日志应该看到
   Principal set in CONNECT: userId=USER_xxx
   Principal verified in SUBSCRIBE: userId=USER_xxx
   
   # 前端控制台应该看到
   WebSocket STOMP connected successfully
   Successfully subscribed to: /user/queue/message
   ```

2. **消息发送测试**
   - 打开两个浏览器窗口（用户 A 和用户 B）
   - A 向 B 发送消息：检查 B 的前端控制台是否收到
   - B 向 A 发送消息：检查 A 的前端控制台是否收到

3. **重启测试**
   - 重启后端，前端应该自动重连
   - 刷新前端页面，应该重新连接并订阅
   - 发送消息验证功能正常

## 相关文件

- **后端配置**: `WebSocketConfig.java`
- **握手拦截器**: `WebSocketHandshakeInterceptor.java`
- **用户身份**: `UserPrincipal.java`
- **事件监听**: `WebSocketEventListener.java`
- **消息推送**: `ContactServiceImpl.java` (sendMessage 方法)
- **前端客户端**: `websocket.js`
- **消息处理**: `stores/message.js` (handleWebSocketMessage 方法)

