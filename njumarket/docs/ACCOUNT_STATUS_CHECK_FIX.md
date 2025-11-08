# 账户状态检查修复说明

## 问题描述
登录后访问个人订单，显示账号被禁用，前端强制登出账号。但是根据数据库，账号状态是ACTIVE。

## 问题分析

### 根本原因
在微服务迁移过程中，**UserContextFilter缺少了用户账户状态检查**。

**对比单体版**：
- 单体版的`JwtAuthenticationFilter`在第124-132行检查用户状态，如果状态不是ACTIVE，返回403
- 微服务版的`UserContextFilter`没有这个检查，导致即使用户状态不是ACTIVE也能继续访问

### 问题流程
1. Gateway验证JWT成功，传递`X-User-Id`到后端服务
2. UserContextFilter从auth-service获取用户信息
3. **问题**：UserContextFilter没有检查用户状态，直接设置SecurityContext
4. 如果用户状态不是ACTIVE，应该返回403，但之前没有检查

## 修复方案

### 1. 在所有服务的UserContextFilter中添加用户状态检查

已修复的服务：
- ✅ **order-service**
- ✅ **commodity-service**
- ✅ **message-service**

### 2. 检查逻辑

参考单体版`JwtAuthenticationFilter`的实现：

```java
// ✅ 检查用户账户状态（参考单体版JwtAuthenticationFilter的实现）
// 如果accountStatus为null或空字符串，也视为异常状态
String accountStatus = user.getAccountStatus();
if (accountStatus == null || accountStatus.trim().isEmpty()) {
    log.error("用户账户状态异常（为null或空）: userId={}, uri={}", user.getUserId(), requestURI);
    response.setStatus(403);
    response.setContentType("application/json;charset=UTF-8");
    response.getWriter().write("{\"success\":false,\"errorMsg\":\"账户状态异常，请联系管理员\"}");
    return;
}

if (!"ACTIVE".equals(accountStatus)) {
    String statusMessage = getAccountStatusMessage(accountStatus);
    log.warn("用户账户已被禁用: userId={}, status=[{}], uri={}", 
        user.getUserId(), accountStatus, requestURI);
    response.setStatus(403);
    response.setContentType("application/json;charset=UTF-8");
    response.getWriter().write(String.format("{\"success\":false,\"errorMsg\":\"%s\"}", statusMessage));
    return;
}
```

### 3. 添加调试日志

在auth-service的`InternalController.getUserById()`中添加日志：
- 打印User实体的accountStatus
- 打印UserInternalDTO的accountStatus

在order-service的`UserContextFilter`中添加日志：
- 打印从auth-service获取的accountStatus

## 可能的原因

如果数据库显示ACTIVE但仍然返回403，可能的原因：

1. **accountStatus为null**：虽然数据库字段有NOT NULL约束，但可能在转换过程中丢失
2. **accountStatus值不匹配**：可能是大小写问题（如"active"而非"ACTIVE"）
3. **accountStatus为空字符串**：虽然不应该发生，但需要处理

## 调试步骤

1. **查看auth-service日志**：
   - 查找`auth-service查询用户: userId=..., accountStatus=[...]`
   - 确认accountStatus的值

2. **查看order-service日志**：
   - 查找`从auth-service获取的accountStatus: [{}], userId=...`
   - 确认接收到的accountStatus值

3. **检查数据库**：
   ```sql
   SELECT user_id, account_status FROM users WHERE user_id = 'user_003';
   ```
   确认account_status的值

4. **检查Feign Client调用**：
   - 确认auth-service的`/api/internal/user/{userId}`接口返回的accountStatus

## 修复后的行为

修复后，如果用户状态不是ACTIVE：
1. UserContextFilter会检查用户状态
2. 如果状态不是ACTIVE，返回403状态码和JSON错误消息
3. 前端收到403后，会清除token并跳转到登录页
4. 用户会看到相应的错误提示（根据状态：暂停、封禁、删除等）

## 相关文件

- `njumarket/njumarket-service-order/src/main/java/com/njumarket/order/filter/UserContextFilter.java`
- `njumarket/njumarket-service-commodity/src/main/java/com/njumarket/commodity/filter/UserContextFilter.java`
- `njumarket/njumarket-service-message/src/main/java/com/njumarket/message/filter/UserContextFilter.java`
- `njumarket/njumarket-service-auth/src/main/java/com/njumarket/auth/controller/InternalController.java`

