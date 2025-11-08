# Gateway路由配置迁移说明

## 📋 变更概述

本次更新修复了Gateway路由配置，使其正确匹配前端API调用路径。

---

## 🔄 路由配置变更

### 变更前（错误配置）

```yaml
routes:
  - id: auth-service
    uri: lb://njumarket-service-auth
    predicates:
      - Path=/auth/**
    filters:
      - StripPrefix=1  # /auth/** → /api/user/**（错误）
```

**问题**：
- 前端调用 `/api/user/auth/login`
- Gateway路由 `/auth/**` 无法匹配
- 即使匹配，StripPrefix=1 后变成 `/api/user/**`，但服务期望的是 `/api/user/**`

### 变更后（正确配置）

```yaml
routes:
  # 认证服务路由（用户相关API）
  - id: auth-service-user
    uri: lb://njumarket-service-auth
    predicates:
      - Path=/api/user/**,/api/contact/**
    # 不StripPrefix，直接转发到服务（服务内部路径就是/api/user/**）
```

**优势**：
- 前端调用 `/api/user/auth/login` → Gateway直接转发到 `auth-service` 的 `/api/user/auth/login`
- 路径完全匹配，无需转换

---

## 📊 完整路由配置

### 1. 认证服务（Auth Service）

**路由**：`/api/user/**` 和 `/api/contact/**`

**说明**：
- 用户认证、用户资料、联系方式相关API
- 不StripPrefix，直接转发

**示例**：
- `/api/user/auth/login` → `auth-service` 的 `/api/user/auth/login`
- `/api/user/profile/me` → `auth-service` 的 `/api/user/profile/me`
- `/api/contact/**` → `auth-service` 的 `/api/contact/**`

---

### 2. 商品服务（Commodity Service）

**路由1**：`/api/public/commodity/**`（公开接口）

**说明**：
- 商品搜索、商品详情等公开接口
- 无需登录即可访问

**示例**：
- `/api/public/commodity/search` → `commodity-service` 的 `/api/public/commodity/search`
- `/api/public/commodity/{id}` → `commodity-service` 的 `/api/public/commodity/{id}`

**路由2**：`/api/user/commodity/**`（用户接口）

**说明**：
- 发布商品、管理商品等需要登录的接口

**示例**：
- `/api/user/commodity/publish` → `commodity-service` 的 `/api/user/commodity/publish`
- `/api/user/commodity/my` → `commodity-service` 的 `/api/user/commodity/my`

---

### 3. 订单服务（Order Service）

**路由**：`/api/user/order/**`、`/api/user/chat/**`、`/api/user/complaint/**`

**说明**：
- 订单管理、聊天数据、投诉相关API

**示例**：
- `/api/user/order/create` → `order-service` 的 `/api/user/order/create`
- `/api/user/chat/incremental-update` → `order-service` 的 `/api/user/chat/incremental-update`

---

### 4. 消息服务（Message Service）

**路由**：`/api/contact/**`、`/api/user/message/**`

**说明**：
- 消息、会话相关API

**示例**：
- `/api/contact/conversations` → `message-service` 的 `/api/contact/conversations`
- `/api/user/message/**` → `message-service` 的 `/api/user/message/**`

---

### 5. 图片服务（Image Service）

**路由**：`/api/images/**`

**说明**：
- 图片访问接口（头像、商品图片）
- 不StripPrefix，直接转发

**示例**：
- `/api/images/avatars/{fileName}` → `image-service` 的 `/api/images/avatars/{fileName}`
- `/api/images/commodities/{fileName}` → `image-service` 的 `/api/images/commodities/{fileName}`

---

### 6. 管理服务（Admin Service）

**路由**：`/api/admin/**`

**说明**：
- 管理员相关API

**示例**：
- `/api/admin/login` → `admin-service` 的 `/api/admin/login`
- `/api/admin/users` → `admin-service` 的 `/api/admin/users`

---

## 🔒 内部API保护

**重要**：所有 `/api/internal/**` 路径被 `InternalApiFilter` 阻止，外部用户无法通过Gateway访问。

**服务间调用**：通过Feign Client直接调用（不经过Gateway）

---

## 📝 前端API配置

### 用户端（User Frontend）

**配置文件**：`njumarket-front/NJUMarket/src/api/index.js`

**配置**：
```javascript
const api = axios.create({
  baseURL: 'http://localhost:8080/api',  // Gateway地址
  timeout: 10000
})
```

**API调用示例**：
```javascript
// 登录
api.post('/user/auth/login', data)
// 实际请求：http://localhost:8080/api/user/auth/login

// 获取商品详情
api.get('/public/commodity/123')
// 实际请求：http://localhost:8080/api/public/commodity/123
```

---

### 管理端（Admin Frontend）

**配置文件**：`njumarket-front-admin/my-vue3-app/src/api/http.js`

**配置**：
```javascript
const http = axios.create({
  baseURL: 'http://localhost:8080/api/admin',  // Gateway地址
  timeout: 15000
})
```

**API调用示例**：
```javascript
// 管理员登录
http.post('/login', data)
// 实际请求：http://localhost:8080/api/admin/login

// 获取用户列表
http.get('/users')
// 实际请求：http://localhost:8080/api/admin/users
```

---

## ✅ 验证清单

- [x] Gateway路由配置正确匹配前端API路径
- [x] JWT认证Filter路径检查已更新
- [x] 管理员认证Filter路径检查已更新
- [x] 内部API保护Filter已添加
- [x] 前端API基础路径指向Gateway（`http://localhost:8080/api`）
- [x] 前端图片URL已更新为 `/api/images/**`

---

## 🚀 测试建议

1. **测试用户端API**：
   - 登录：`POST /api/user/auth/login`
   - 获取商品：`GET /api/public/commodity/{id}`
   - 创建订单：`POST /api/user/order/create`

2. **测试管理端API**：
   - 管理员登录：`POST /api/admin/login`
   - 获取用户列表：`GET /api/admin/users`

3. **测试内部API保护**：
   - 尝试访问：`GET /api/internal/user/{id}`（应该返回403）

4. **测试图片访问**：
   - 访问头像：`GET /api/images/avatars/{fileName}`
   - 访问商品图片：`GET /api/images/commodities/{fileName}`

---

## 📚 相关文档

- [架构问题分析](./ARCHITECTURE_ISSUES_ANALYSIS.md)
- [TODO列表（2.1.x版本）](./TODO_V2.1.x.md)

