# 账户状态问题调试指南

## 问题描述
登录后访问个人订单，显示账号被禁用，前端强制登出账号。但是根据数据库，是active。

## 已实施的修复

### 1. 账户状态检查
在以下服务的 `UserContextFilter` 中添加了账户状态检查：
- `order-service`
- `commodity-service`
- `message-service`

检查逻辑：
- 如果 `accountStatus` 为 null 或空字符串，返回 403
- 如果 `accountStatus` 不等于 "ACTIVE"（区分大小写），返回 403

### 2. 日志记录
在以下位置添加了详细的日志记录：
- `auth-service` 的 `InternalController.getUserById()`：记录从数据库查询的用户状态
- `order-service` 的 `UserContextFilter`：记录从 auth-service 获取的账户状态

### 3. 日志配置
在 `application.yml` 中添加了日志级别配置，确保相关日志能够输出。

## 调试步骤

### 1. 检查数据库中的实际值
```sql
SELECT user_id, account_status FROM users WHERE user_id = 'user_003';
```

**重要**：确保 `account_status` 的值是 **大写的 "ACTIVE"**，因为代码中检查的是 `"ACTIVE".equals(accountStatus)`，这是区分大小写的。

### 2. 重新编译并运行服务
```bash
# 重新编译所有服务
mvn clean install

# 启动服务（按顺序）
# 1. Eureka
# 2. Gateway
# 3. Auth Service
# 4. Order Service
# 5. 其他服务
```

### 3. 测试并查看日志
1. 登录系统
2. 访问个人订单接口：`GET /api/user/order/my?page=1&size=10`
3. 查看以下日志：

**auth-service 日志**（应该显示）：
```
auth-service查询用户: userId=user_003, accountStatus=[ACTIVE], accountStatus是否为null=false
auth-service返回UserInternalDTO: userId=user_003, accountStatus=[ACTIVE], accountStatus是否为null=false
```

**order-service 日志**（应该显示）：
```
从auth-service获取的accountStatus: [ACTIVE], userId=user_003, accountStatus是否为null=false
用户信息已设置: userId=user_003, username=..., accountStatus=ACTIVE, SecurityContext已设置
```

### 4. 如果仍然出现问题

#### 情况1：日志显示 accountStatus 为 null
- 检查 `InternalDTOConverter.toInternalDTO(User user)` 是否正确设置了 `accountStatus`
- 检查数据库中的 `account_status` 字段是否真的存在且不为 null

#### 情况2：日志显示 accountStatus 不是 "ACTIVE"
- 检查数据库中的实际值（可能是小写 "active" 或其他值）
- 如果是小写，需要更新数据库或修改代码使其不区分大小写

#### 情况3：日志显示 accountStatus 为 "ACTIVE" 但仍然返回 403
- 检查 `UserContextFilter` 中的比较逻辑
- 检查是否有其他过滤器或拦截器也在检查账户状态

## 代码修改位置

### 1. UserContextFilter（order-service、commodity-service、message-service）
```java
// 检查用户账户状态
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

### 2. InternalController（auth-service）
```java
// 调试：打印用户状态信息
log.info("auth-service查询用户: userId={}, accountStatus=[{}], accountStatus是否为null={}", 
    user.getUserId(), user.getAccountStatus(), user.getAccountStatus() == null);

UserInternalDTO dto = internalDTOConverter.toInternalDTO(user);

// 调试：打印DTO状态信息
log.info("auth-service返回UserInternalDTO: userId={}, accountStatus=[{}], accountStatus是否为null={}", 
    dto.getUserId(), dto.getAccountStatus(), dto.getAccountStatus() == null);
```

## 注意事项

1. **大小写敏感**：代码中检查的是 `"ACTIVE"`（大写），确保数据库中的值也是大写
2. **null 检查**：代码会检查 `accountStatus` 是否为 null 或空字符串
3. **日志级别**：确保日志级别设置为 `INFO` 或更低，才能看到调试日志

## 如果问题仍然存在

如果按照上述步骤操作后问题仍然存在，请提供：
1. 数据库中的实际 `account_status` 值
2. `auth-service` 的完整日志（特别是 `InternalController.getUserById()` 的日志）
3. `order-service` 的完整日志（特别是 `UserContextFilter` 的日志）
4. Gateway 的日志（如果有相关错误）

