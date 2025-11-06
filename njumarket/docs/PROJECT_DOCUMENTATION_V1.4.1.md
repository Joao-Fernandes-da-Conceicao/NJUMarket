# 南大集市 NJUMarket v1.4.1 项目文档

## 📋 版本概述

### 版本信息
- **版本**: v1.4.1
- **发布时间**: 2025-01-XX
- **基于版本**: v1.4.0
- **状态**: ✅ **已完成**

### 版本定位
v1.4.1 是一个**意外更新版本**，主要实现了**双Token自动刷新机制**，并移除了冗余的Token续期逻辑，统一了Token管理策略。

---

## 核心功能更新

### 1. 双Token自动刷新机制

#### 1.1 前端实现

**Token存储**：
- ✅ 前端Store支持`accessToken`和`refreshToken`分别存储
- ✅ 兼容旧版本的`token`字段（通过getter映射）
- ✅ localStorage存储：`accessToken`、`refreshToken`、`expiresIn`

**自动刷新逻辑**：
- ✅ 响应拦截器检测401错误
- ✅ 自动使用RefreshToken刷新AccessToken
- ✅ 防止并发刷新（多个请求同时触发时，只执行一次刷新）
- ✅ 刷新成功后自动重试原请求，用户无感知
- ✅ 刷新失败时清除数据并提示用户重新登录

**关键代码**：
```javascript
// 防止并发刷新
let refreshTokenPromise = null

// 401错误处理
if (error.response?.status === 401 && !originalRequest._retry) {
  originalRequest._retry = true
  
  // 如果已经有刷新请求在进行，等待它完成
  if (refreshTokenPromise) {
    await refreshTokenPromise
  } else {
    refreshTokenPromise = userStore.refreshAccessToken()
    await refreshTokenPromise
  }
  
  // 刷新成功，使用新的AccessToken重试原请求
  originalRequest.headers.Authorization = `Bearer ${newAccessToken}`
  return api(originalRequest)
}
```

#### 1.2 后端实现

**Token生成**：
- ✅ `UserServiceImpl.generateAndStoreTokens()`生成AccessToken和RefreshToken
- ✅ AccessToken有效期：24小时
- ✅ RefreshToken有效期：7天
- ✅ 两个Token都存储到Redis

**Token刷新接口**：
- ✅ `/api/user/auth/refresh-token`接口允许未认证访问
- ✅ 验证RefreshToken（JWT + Redis）
- ✅ 生成新的AccessToken和RefreshToken
- ✅ 更新Redis中的Token

**Token验证**：
- ✅ `JwtAuthenticationFilter`验证AccessToken（JWT + Redis）
- ✅ 排除刷新接口，不需要JWT验证

---

### 2. 移除冗余的Token续期机制

#### 2.1 问题分析

**原有设计冲突**：
- ❌ `JwtAuthenticationFilter`中有Token续期逻辑（每次请求延长Redis过期时间）
- ❌ 这与双Token机制冲突：AccessToken过期后应使用RefreshToken刷新，而不是续期

**Admin端确认**：
- ✅ `AdminAuthenticationFilter`没有Redis依赖
- ✅ `AdminAuthenticationFilter`没有Token续期逻辑
- ✅ Admin端仅验证JWT本身的有效性

#### 2.2 解决方案

**移除续期逻辑**：
- ✅ 移除`JwtAuthenticationFilter`中的Token续期代码
- ✅ 移除未使用的`TimeUnit`导入
- ✅ 添加注释说明：Redis的作用是验证Token是否被撤销，不用于续期

**统一Token管理策略**：
- ✅ 用户端：双Token机制（AccessToken + RefreshToken）
- ✅ Admin端：单Token机制（Token过期后重新登录）
- ✅ Redis作用：验证Token是否被撤销（登出）和防止Token被替换

---

## 技术实现细节

### Token生命周期

```
1. 用户登录
   → 返回 accessToken (24h) + refreshToken (7d)
   → 前端保存到 localStorage

2. API请求（AccessToken有效）
   → 携带 AccessToken
   → 后端验证通过（JWT + Redis）
   → 返回响应 ✅

3. API请求（AccessToken过期）
   → 携带 AccessToken
   → 后端返回 401
   → 前端检测到 401
   → 检查是否有 RefreshToken
   → 调用刷新接口
   → 获取新的 AccessToken + RefreshToken
   → 更新 localStorage
   → 使用新 AccessToken 重试原请求 ✅

4. RefreshToken也过期
   → 刷新接口返回 401
   → 前端清除所有数据
   → 提示用户重新登录 ❌
```

### Redis存储结构

```
login:token:{userId} = AccessToken (24h TTL)
refresh:token:{userId} = RefreshToken (7d TTL)
```

---

## 代码变更

### 前端文件
- ✅ `njumarket-front/NJUMarket/src/stores/user.js` - 支持双Token存储和刷新
- ✅ `njumarket-front/NJUMarket/src/api/index.js` - 实现自动刷新逻辑
- ✅ `njumarket-front/NJUMarket/src/utils/websocket.js` - 使用accessToken连接

### 后端文件
- ✅ `njumarket/src/main/java/com/njumarket/njumarket/filter/JwtAuthenticationFilter.java` - 移除续期逻辑
- ✅ `njumarket/src/main/java/com/njumarket/njumarket/config/SecurityConfig.java` - 允许刷新接口未认证访问
- ✅ `njumarket/src/main/java/com/njumarket/njumarket/service/impl/UserServiceImpl.java` - 双Token生成和刷新逻辑

---

## 已知问题与限制

### 1. 向后兼容性
- ✅ 前端兼容旧版本的`token`字段
- ✅ 登录/注册接口兼容返回`token`或`accessToken`

### 2. Admin端限制
- ⚠️ Admin端没有RefreshToken机制
- ⚠️ Admin端Token过期后需要重新登录

---

## 下一步规划

- ✅ v1.4.1已完成，v1.x阶段正式结束
- 📋 进入v2.0阶段：微服务架构 + MyBatis + 多级缓存

---

## 相关文档

- [v1.4.0 文档](./PROJECT_DOCUMENTATION_V1.4.0.md) - 代码标准化与架构规范化
- [v1.x 阶段总结](./PROJECT_DOCUMENTATION_V1.x_SUMMARY.md) - 完整的v1.x阶段总结
- [Spring Security实现文档](./SPRING_SECURITY_IMPLEMENTATION.md) - Spring Security标准实现

