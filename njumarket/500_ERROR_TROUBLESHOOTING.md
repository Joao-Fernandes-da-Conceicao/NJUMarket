# 500错误排查指南

## 问题描述
前端请求全部报500错误，order-service 已成功启动。

## 可能原因

### 1. Gateway 路由问题
**检查方法**：
- 查看 Gateway 的日志，确认请求是否到达 Gateway
- 检查 Gateway 是否正确路由到 order-service
- 确认 Eureka 中 order-service 已注册

**测试方法**：
- 直接访问 order-service：`http://localhost:8093/api/user/order/buyer?page=1&size=10`
- 如果直接访问也报错，说明是 order-service 的问题
- 如果直接访问正常，说明是 Gateway 路由的问题

### 2. 服务间调用失败（Feign Client）
**可能原因**：
- Auth Service 未启动或未注册
- Commodity Service 未启动或未注册
- Message Service 未启动或未注册
- Feign Client 调用超时

**检查方法**：
- 查看 order-service 的日志，查找 Feign Client 调用错误
- 确认所有依赖服务都已启动并注册到 Eureka
- 检查 Feign Client 的超时配置

### 3. 数据库连接问题
**检查方法**：
- 查看 order-service 的日志，查找数据库连接错误
- 确认数据库服务是否正常运行
- 检查数据库连接配置是否正确

### 4. 运行时异常
**检查方法**：
- 查看 order-service 的完整日志，查找异常堆栈
- 检查是否有空指针异常、类型转换异常等
- 查看 Gateway 的日志，确认错误响应

### 5. 请求头缺失
**可能原因**：
- Gateway 的 JWT Filter 未正确添加 `X-User-Id` 请求头
- order-service 的 `SecurityUtils.requireCurrentUser()` 无法获取用户信息

**检查方法**：
- 查看 Gateway 的日志，确认 JWT Filter 是否正常工作
- 查看 order-service 的日志，确认是否因为用户信息缺失而报错

## 快速排查步骤

### 步骤1：检查服务注册状态
访问 Eureka：`http://localhost:8761`
- 确认 `njumarket-service-order` 已注册
- 确认所有依赖服务都已注册：
  - `njumarket-service-auth`
  - `njumarket-service-commodity`
  - `njumarket-service-message`

### 步骤2：直接测试 order-service
绕过 Gateway，直接访问 order-service：
```bash
# 测试获取买家订单（需要先登录获取Token）
curl -H "Authorization: Bearer YOUR_TOKEN" \
     -H "X-User-Id: USER_ID" \
     http://localhost:8093/api/user/order/buyer?page=1&size=10
```

### 步骤3：查看 Gateway 日志
查看 Gateway 的日志，确认：
- 请求是否到达 Gateway
- Gateway 是否正确路由到 order-service
- 是否有路由错误

### 步骤4：查看 order-service 日志
查看 order-service 的运行时日志，查找：
- 异常堆栈信息
- Feign Client 调用错误
- 数据库连接错误
- 业务逻辑错误

### 步骤5：检查 Gateway 路由配置
确认 Gateway 的 `application.yml` 中 order-service 的路由配置：
```yaml
- id: order-service
  uri: lb://njumarket-service-order
  predicates:
    - Path=/api/user/order/**,/api/user/chat/**,/api/user/complaint/**
```

## 常见错误和解决方案

### 错误1：Feign Client 调用失败
**症状**：日志中出现 `FeignException` 或 `ConnectException`
**解决方案**：
- 确认依赖服务已启动
- 检查 Feign Client 的超时配置
- 确认服务名称是否正确

### 错误2：用户信息缺失
**症状**：日志中出现 `SecurityUtils.requireCurrentUser()` 相关的异常
**解决方案**：
- 检查 Gateway 的 JWT Filter 是否正确添加 `X-User-Id` 请求头
- 确认请求头传递到 order-service

### 错误3：数据库连接失败
**症状**：日志中出现数据库连接错误
**解决方案**：
- 确认数据库服务是否正常运行
- 检查数据库连接配置
- 确认数据库用户权限

### 错误4：空指针异常
**症状**：日志中出现 `NullPointerException`
**解决方案**：
- 查看异常堆栈，定位具体位置
- 检查相关代码的空值判断

## 建议的调试方法

1. **启用详细日志**：
   ```yaml
   logging:
     level:
       com.njumarket.order: DEBUG
       org.springframework.cloud.openfeign: DEBUG
   ```

2. **使用 Postman 测试**：
   - 直接测试 order-service
   - 测试 Gateway 路由
   - 查看完整的错误响应

3. **检查浏览器控制台**：
   - 查看网络请求的详细信息
   - 查看错误响应的内容

## 当前状态检查清单

- [ ] Gateway 是否正常运行
- [ ] order-service 是否已注册到 Eureka
- [ ] 所有依赖服务是否已启动
- [ ] 数据库连接是否正常
- [ ] Gateway 路由配置是否正确
- [ ] JWT Filter 是否正常工作
- [ ] Feign Client 配置是否正确

