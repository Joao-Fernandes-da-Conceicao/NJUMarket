# 用户反馈机制改进文档

## 📋 概述

本文档说明针对用户封禁/停用场景的用户反馈机制改进，确保用户能够收到清晰的错误提示信息。

---

## 1. 问题分析

### 1.1 原有问题

**问题现象**：
- 登录拦截器只返回403状态码，没有返回JSON响应消息
- 登录时账户状态检查只返回通用错误信息
- 前端没有处理403错误，用户看不到具体原因
- 不同账户状态（SUSPENDED、BANNED、DELETED）使用相同的错误提示

**影响**：
- 用户体验差：用户不知道具体原因
- 难以排查问题：用户无法知道账户状态
- 缺乏友好的错误提示

---

## 2. 改进方案

### 2.1 后端改进

#### 2.1.1 LoginInterceptor 改进

**改进内容**：
- ✅ 所有错误都返回JSON响应（参考AdminInterceptor）
- ✅ 401错误：返回详细的错误信息
- ✅ 403错误：根据账户状态返回不同的提示信息

**实现代码**：

```java
// 改进前：只返回状态码
if (!"ACTIVE".equals(user.getAccountStatus())) {
    response.setStatus(403);
    return false;
}

// 改进后：返回JSON响应
if (!"ACTIVE".equals(user.getAccountStatus())) {
    String statusMessage = getAccountStatusMessage(user.getAccountStatus());
    response.setStatus(403);
    response.setContentType("application/json;charset=UTF-8");
    response.getWriter().write(String.format("{\"success\":false,\"errorMsg\":\"%s\"}", statusMessage));
    return false;
}
```

#### 2.1.2 登录服务改进

**改进内容**：
- ✅ 根据不同账户状态返回不同的提示信息
- ✅ SUSPENDED：账户已被暂停
- ✅ BANNED：账户已被封禁
- ✅ DELETED：账户已被删除

**实现代码**：

```java
// 改进前：通用错误信息
if (!"ACTIVE".equals(user.getAccountStatus())) {
    return Result.fail("账户已被禁用，请联系管理员");
}

// 改进后：根据状态返回不同信息
if (!"ACTIVE".equals(user.getAccountStatus())) {
    String statusMessage = getAccountStatusMessage(user.getAccountStatus());
    return Result.fail(statusMessage);
}

private String getAccountStatusMessage(String accountStatus) {
    switch (accountStatus) {
        case "SUSPENDED":
            return "账户已被暂停，请联系管理员了解详情";
        case "BANNED":
            return "账户已被封禁，如有疑问请联系管理员";
        case "DELETED":
            return "账户已被删除，无法使用";
        default:
            return "账户状态异常，请联系管理员";
    }
}
```

---

### 2.2 前端改进

#### 2.2.1 API拦截器改进

**改进内容**：
- ✅ 处理403错误并显示错误提示
- ✅ 清除用户数据（账户被封禁，需要重新登录）
- ✅ 显示用户友好的错误消息

**实现代码**：

```javascript
// 改进前：只处理401错误
api.interceptors.response.use(
  response => response.data,
  error => {
    if (error.response?.status === 401) {
      // 清除用户数据
    }
    return Promise.reject(error)
  }
)

// 改进后：处理401和403错误
api.interceptors.response.use(
  response => response.data,
  error => {
    if (error.response?.status === 401) {
      // 处理未登录
      import('../stores/user').then(({ useUserStore }) => {
        const userStore = useUserStore()
        userStore.clearUserData()
      })
    } else if (error.response?.status === 403) {
      // ✅ 处理账户被封禁
      const errorMsg = error.response?.data?.errorMsg || '账户已被禁用，无法访问'
      ElMessage.error(errorMsg)
      
      // 清除用户数据
      import('../stores/user').then(({ useUserStore }) => {
        const userStore = useUserStore()
        userStore.clearUserData()
      })
    }
    return Promise.reject(error)
  }
)
```

---

## 3. 账户状态说明

### 3.1 账户状态类型

| 状态 | 说明 | 用户提示 | 是否可恢复 |
|------|------|---------|-----------|
| ACTIVE | 正常 | - | - |
| SUSPENDED | 暂停 | "账户已被暂停，请联系管理员了解详情" | 是（管理员可恢复） |
| BANNED | 封禁 | "账户已被封禁，如有疑问请联系管理员" | 是（临时封禁可恢复） |
| DELETED | 删除 | "账户已被删除，无法使用" | 否（永久删除） |

### 3.2 状态判断逻辑

```java
private String getAccountStatusMessage(String accountStatus) {
    if (accountStatus == null) {
        return "账户状态异常，请联系管理员";
    }
    
    switch (accountStatus) {
        case "SUSPENDED":
            return "账户已被暂停，请联系管理员了解详情";
        case "BANNED":
            return "账户已被封禁，如有疑问请联系管理员";
        case "DELETED":
            return "账户已被删除，无法使用";
        default:
            return "账户状态异常，请联系管理员";
    }
}
```

---

## 4. 改进效果

### 4.1 改进前

**用户场景**：
- 用户尝试登录被封禁的账户
- 后端返回：`HTTP 403 Forbidden`（无消息体）
- 前端：无提示，用户不知道原因

**问题**：
- ❌ 用户不知道账户被封禁
- ❌ 无法知道具体原因
- ❌ 用户体验差

### 4.2 改进后

**用户场景**：
- 用户尝试登录被封禁的账户
- 后端返回：`HTTP 403 Forbidden` + JSON响应
  ```json
  {
    "success": false,
    "errorMsg": "账户已被封禁，如有疑问请联系管理员"
  }
  ```
- 前端：显示错误提示消息

**优势**：
- ✅ 用户知道账户被封禁
- ✅ 知道需要联系管理员
- ✅ 用户体验友好

---

## 5. 其他需要改进的地方

### 5.1 业务操作中的状态检查

**当前实现**：部分Service方法已检查账户状态并返回错误信息

**建议**：
- ✅ 统一使用`getAccountStatusMessage()`方法
- ✅ 确保所有业务操作都检查账户状态
- ✅ 返回用户友好的错误信息

### 5.2 封禁记录查询

**未来优化**：
- 查询封禁记录（BanRecord）获取封禁原因
- 显示封禁结束时间（临时封禁）
- 显示封禁详细信息

**示例**：
```java
// 查询封禁记录
BanRecord banRecord = banRecordRepository.findActiveBanByUserId(userId);
if (banRecord != null) {
    String message = String.format("账户已被封禁，原因：%s", banRecord.getReason());
    if (banRecord.getEndAt() != null) {
        message += String.format("，预计解封时间：%s", banRecord.getEndAt());
    }
    return Result.fail(message);
}
```

---

## 6. 测试场景

### 6.1 测试用例

1. **登录被暂停账户**：
   - 预期：返回"SUSPENDED"状态，显示"账户已被暂停，请联系管理员了解详情"

2. **登录被封禁账户**：
   - 预期：返回"BANNED"状态，显示"账户已被封禁，如有疑问请联系管理员"

3. **登录被删除账户**：
   - 预期：返回"DELETED"状态，显示"账户已被删除，无法使用"

4. **已登录用户被禁用**：
   - 预期：后续请求返回403，显示相应错误信息

5. **前端错误提示**：
   - 预期：403错误时显示ElMessage错误提示

---

## 7. 实施清单

### 7.1 后端改进

- [x] LoginInterceptor：返回JSON响应
- [x] LoginInterceptor：添加`getAccountStatusMessage()`方法
- [x] UserServiceImpl.login()：使用状态消息方法
- [x] UserServiceImpl.loginByCode()：使用状态消息方法
- [ ] 其他Service方法：统一使用状态消息方法（可选）

### 7.2 前端改进

- [x] API拦截器：处理403错误
- [x] API拦截器：显示错误提示
- [x] 管理端API拦截器：处理403错误
- [ ] 登录页面：显示账户状态错误（可选）

---

## 8. 相关文件

### 后端文件
- `LoginInterceptor.java` - 登录拦截器
- `UserServiceImpl.java` - 用户服务实现
- `AdminInterceptor.java` - 管理员拦截器（参考实现）

### 前端文件
- `njumarket-front/NJUMarket/src/api/index.js` - 用户端API拦截器
- `njumarket-front-admin/my-vue3-app/src/api/http.js` - 管理端API拦截器

---

**文档版本**：v1.0  
**最后更新**：2025-11-05  
**维护者**：NJUMarket 开发团队

