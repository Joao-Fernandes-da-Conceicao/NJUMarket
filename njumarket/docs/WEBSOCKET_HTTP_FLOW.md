# WebSocket 和 HTTP 请求流转文档

## 请求流转路径

### 1. HTTP 请求流转

#### 用户 API 请求（/api/user/**）
```
前端 → Gateway (8080)
  ↓ JwtAuthenticationFilter 验证 JWT
  ↓ 添加 X-User-Id 请求头
  ↓ 路由到对应服务
后端服务 (UserContextFilter 提取 X-User-Id)
  ↓ 设置 SecurityContext
  ↓ Controller 处理请求
  ↓ 返回响应
```

#### 管理员 API 请求（/api/admin/**）
```
前端 → Gateway (8080)
  ↓ AdminAuthenticationFilter 验证 JWT
  ↓ 添加 X-Admin-Id 请求头
  ↓ 路由到 admin-service
admin-service (AdminContextFilter 提取 X-Admin-Id)
  ↓ 设置 SecurityContext
  ↓ Controller 处理请求
  ↓ 返回响应
```

### 2. WebSocket 请求流转

#### 聊天 WebSocket（/api/ws/**）
```
前端 → Gateway (8080)
  ↓ JwtAuthenticationFilter 验证 JWT（HTTP 握手请求）
  ↓ 添加 X-User-Id 请求头
  ↓ 路由到 message-service（StripPrefix=1: /api/ws → /ws）
message-service
  ↓ WebSocketHandshakeInterceptor 提取 X-User-Id
  ↓ 存储到 session attributes
  ↓ WebSocketChannelInterceptor 设置 Principal
  ↓ WebSocket 连接建立
  ↓ 订阅 /user/queue/message 接收消息
```

#### 订单通知 WebSocket（/api/ws/order/**）
```
前端 → Gateway (8080)
  ↓ JwtAuthenticationFilter 验证 JWT（HTTP 握手请求）
  ↓ 添加 X-User-Id 请求头
  ↓ 路由到 order-service（StripPrefix=1: /api/ws/order → /ws/order）
order-service
  ↓ WebSocketHandshakeInterceptor 提取 X-User-Id
  ↓ 存储到 session attributes
  ↓ WebSocketChannelInterceptor 设置 Principal
  ↓ WebSocket 连接建立
  ↓ 订阅 /user/queue/order 接收订单通知
```

## 关键配置点

### Gateway 路由配置
- **消息服务 WebSocket**: `/api/ws/**` → `message-service` (StripPrefix=1)
- **订单服务 WebSocket**: `/api/ws/order/**` → `order-service` (StripPrefix=1)
- **注意**: 路由顺序很重要，更具体的路径（/api/ws/order/**）必须在更通用的路径（/api/ws/**）之前

### JWT 认证流程
1. Gateway 的 `JwtAuthenticationFilter` 验证 JWT Token
2. 从 Token 中提取 userId
3. 验证 Redis 中的 Token 有效性
4. 添加 `X-User-Id` 请求头传递给后端服务

### WebSocket 认证流程
1. WebSocket 握手请求（HTTP）经过 Gateway JWT 验证
2. Gateway 添加 `X-User-Id` 请求头
3. 后端服务的 `WebSocketHandshakeInterceptor` 提取 `X-User-Id`
4. 存储到 session attributes
5. `WebSocketChannelInterceptor` 在 CONNECT 时设置 Principal

## 常见错误及解决方案

### 401 Unauthorized
- **原因**: JWT Token 无效或过期
- **解决**: 检查 Token 是否有效，是否在 Redis 中存在

### 403 Forbidden
- **原因**: Spring Security 拦截了请求
- **解决**: 确保 SecurityConfig 允许 WebSocket 端点

### 404 Not Found
- **原因**: 路由配置错误或路径不匹配
- **解决**: 检查 Gateway 路由配置和服务的端点路径

### 500 Internal Server Error
- **原因**: 后端服务异常或配置错误
- **解决**: 检查服务日志，确保所有依赖正确配置

