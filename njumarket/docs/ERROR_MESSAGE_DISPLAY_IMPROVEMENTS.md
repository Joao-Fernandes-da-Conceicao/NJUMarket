# 错误消息显示改进文档

## 📋 概述

本文档说明针对所有 errorMsg 显式弹窗显示的改进，确保用户能够看到清晰的错误提示信息。

---

## 1. 问题分析

### 1.1 原有问题

**问题现象**：
- 登录被封禁账号时，后端返回了正确的 errorMsg
- 但前端可能没有显示弹窗，用户看不到错误信息
- 不同场景的错误处理不一致

**响应示例**：
```json
{
    "success": false,
    "errorMsg": "账户已被暂停，请联系管理员了解详情",
    "data": null,
    "total": null,
    "message": "账户已被暂停，请联系管理员了解详情"
}
```

---

## 2. 改进方案

### 2.1 API拦截器改进（用户端）

**改进内容**：
- ✅ 响应拦截器中检查 `success=false`，自动显示 errorMsg 弹窗
- ✅ 401错误：显示错误提示
- ✅ 403错误：显示错误提示
- ✅ 其他HTTP错误（400、500等）：显示 errorMsg 弹窗

**实现代码**：

```javascript
// 响应拦截器
api.interceptors.response.use(
  response => {
    // ✅ 如果响应中success=false，也显示errorMsg弹窗
    if (response.data && response.data.success === false) {
      const errorMsg = response.data.errorMsg || response.data.message || '操作失败'
      if (typeof window !== 'undefined') {
        import('element-plus').then(({ ElMessage }) => {
          ElMessage.error(errorMsg)
        })
      }
    }
    return response.data
  },
  error => {
    // 处理各种HTTP错误，显示errorMsg
    if (error.response?.status === 401) {
      // 显示401错误提示
      const errorMsg = error.response?.data?.errorMsg || error.response?.data?.message || '用户未登录，请先登录'
      ElMessage.error(errorMsg)
    } else if (error.response?.status === 403) {
      // 显示403错误提示
      const errorMsg = error.response?.data?.errorMsg || error.response?.data?.message || '账户已被禁用，无法访问'
      ElMessage.error(errorMsg)
    } else if (error.response?.data) {
      // 处理其他HTTP错误，显示errorMsg
      const errorMsg = error.response?.data?.errorMsg || error.response?.data?.message || '操作失败，请稍后重试'
      ElMessage.error(errorMsg)
    }
    return Promise.reject(error)
  }
)
```

### 2.2 UserStore 改进

**改进内容**：
- ✅ login() 方法：优先使用 errorMsg，如果为空则使用 message
- ✅ loginByCode() 方法：优先使用 errorMsg，如果为空则使用 message

**实现代码**：

```javascript
async login(loginData) {
  const response = await authAPI.login(loginData)
  if (response.success) {
    // 登录成功逻辑...
    return response
  } else {
    // ✅ 优先使用errorMsg，如果为空则使用message
    const errorMsg = response.errorMsg || response.message || '登录失败'
    throw new Error(errorMsg)
  }
}
```

### 2.3 登录页面改进

**改进内容**：
- ✅ 密码登录：显示 errorMsg 弹窗
- ✅ 验证码登录：显示 errorMsg 弹窗

**实现代码**：

```javascript
// 密码登录
try {
  await userStore.login({ identifier, password })
  ElMessage.success('登录成功')
  router.push('/')
} catch (error) {
  // ✅ 显式弹窗显示errorMsg
  const errorMsg = error.message || error.errorMsg || '登录失败'
  ElMessage.error(errorMsg)
}
```

### 2.4 管理端改进

**改进内容**：
- ✅ API拦截器：显示所有 errorMsg 弹窗
- ✅ 登录页面：显示 errorMsg 弹窗

---

## 3. 错误消息显示流程

### 3.1 登录封禁账号场景

**流程**：
```
1. 用户登录被封禁账号
   ↓
2. 后端返回：{ success: false, errorMsg: "账户已被暂停，请联系管理员了解详情" }
   ↓
3. API拦截器检测到 success=false
   ↓
4. 自动显示 ElMessage.error("账户已被暂停，请联系管理员了解详情")
   ↓
5. userStore.login() 抛出错误
   ↓
6. Login.vue catch 中再次显示错误（双重保险）
```

### 3.2 已登录用户被封禁场景

**流程**：
```
1. 已登录用户执行操作
   ↓
2. LoginInterceptor 检测到账户状态异常
   ↓
3. 返回 HTTP 403 + JSON响应：{ success: false, errorMsg: "账户已被封禁..." }
   ↓
4. API拦截器检测到 403 错误
   ↓
5. 自动显示 ElMessage.error("账户已被封禁...")
   ↓
6. 清除用户数据
```

---

## 4. 改进效果

### 4.1 改进前

**问题**：
- ❌ 登录被封禁账号时，可能没有显示错误提示
- ❌ 用户不知道具体原因
- ❌ 不同场景的错误处理不一致

### 4.2 改进后

**优势**：
- ✅ **API拦截器自动显示**：所有 success=false 的响应都会自动显示 errorMsg
- ✅ **双重保障**：API拦截器和页面catch都显示错误
- ✅ **统一处理**：所有错误都使用相同的机制
- ✅ **用户友好**：用户能看到清晰的错误提示

---

## 5. 错误消息优先级

### 5.1 消息来源优先级

```
1. errorMsg（最高优先级）
2. message（次优先级）
3. 默认消息（fallback）
```

### 5.2 实现示例

```javascript
// 优先级：errorMsg > message > 默认消息
const errorMsg = response.errorMsg || response.message || '操作失败'
ElMessage.error(errorMsg)
```

---

## 6. 测试场景

### 6.1 测试用例

1. **登录被封禁账号（SUSPENDED）**：
   - 预期：显示"账户已被暂停，请联系管理员了解详情"

2. **登录被封禁账号（BANNED）**：
   - 预期：显示"账户已被封禁，如有疑问请联系管理员"

3. **登录被删除账号（DELETED）**：
   - 预期：显示"账户已被删除，无法使用"

4. **其他操作失败**：
   - 预期：显示对应的 errorMsg

---

## 7. 相关文件

### 后端文件
- `LoginInterceptor.java` - 登录拦截器（返回JSON响应）
- `UserServiceImpl.java` - 用户服务（返回详细错误信息）

### 前端文件
- `njumarket-front/NJUMarket/src/api/index.js` - 用户端API拦截器
- `njumarket-front/NJUMarket/src/stores/user.js` - 用户Store
- `njumarket-front/NJUMarket/src/views/Login.vue` - 登录页面
- `njumarket-front-admin/my-vue3-app/src/api/http.js` - 管理端API拦截器
- `njumarket-front-admin/my-vue3-app/src/views/Login.vue` - 管理端登录页面

---

## 8. 总结

### 8.1 改进要点

1. ✅ **API拦截器自动显示**：所有 success=false 的响应都会自动显示 errorMsg
2. ✅ **双重保障**：API拦截器和页面catch都显示错误
3. ✅ **统一处理**：401、403、400、500等错误都显示 errorMsg
4. ✅ **优先级明确**：errorMsg > message > 默认消息

### 8.2 用户体验提升

- ✅ 用户能看到清晰的错误提示
- ✅ 知道具体原因（账户状态、操作失败原因等）
- ✅ 知道下一步该做什么（联系管理员、重新登录等）

---

**文档版本**：v1.0  
**最后更新**：2025-11-05  
**维护者**：NJUMarket 开发团队

