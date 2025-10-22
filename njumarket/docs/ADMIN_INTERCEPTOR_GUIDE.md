# NJU Market 管理员拦截器使用说明

## 概述

管理员拦截器(`AdminInterceptor`)是专门为管理员系统设计的安全拦截器，用于验证管理员身份、检查权限等级，并保护敏感操作。

## 功能特性

### 1. 身份验证
- **JWT Token验证**：验证管理员登录Token的有效性
- **管理员存在性检查**：确认管理员账号存在
- **账户状态检查**：验证管理员账户是否处于活跃状态

### 2. 权限分级控制
- **SUPER（超级管理员）**：拥有所有权限
- **SENIOR（高级管理员）**：拥有大部分权限，但不能删除超级管理员
- **NORMAL（普通管理员）**：拥有基础权限，不能执行敏感操作

### 3. 敏感操作保护
自动识别并保护以下敏感操作：
- 创建管理员账号
- 删除管理员账号
- 重置密码
- 更新权限
- 更新管理员状态
- 获取统计信息

## 拦截器配置

### WebConfig配置
```java
@Override
public void addInterceptors(InterceptorRegistry registry) {
    // 用户拦截器 - 只拦截用户相关路径
    registry.addInterceptor(loginInterceptor)
            .addPathPatterns("/api/user/**")
            .excludePathPatterns(
                "/api/user/auth/login",
                "/api/user/auth/register", 
                "/api/user/auth/register-new",
                "/api/user/auth/send-code",
                "/api/user/auth/login-by-code",
                "/api/user/auth/login-third-party",
                "/api/user/auth/reset-password"
            );
    
    // 管理员拦截器 - 只拦截管理员相关路径
    registry.addInterceptor(adminInterceptor)
            .addPathPatterns("/api/admin/**")
            .excludePathPatterns(
                "/api/admin/login"  // 管理员登录接口不需要拦截
            );
}
```

## 权限控制规则

### 超级管理员 (SUPER)
```java
// 拥有所有权限，可以执行任何操作
if (admin.isSuperAdmin()) {
    return true;
}
```

**允许的操作：**
- ✅ 创建管理员
- ✅ 删除管理员（包括其他超级管理员）
- ✅ 重置密码
- ✅ 更新权限
- ✅ 更新管理员状态
- ✅ 获取统计信息
- ✅ 所有其他操作

### 高级管理员 (SENIOR)
```java
// 高级管理员不能删除超级管理员
if ("SENIOR".equals(admin.getAdminLevel())) {
    if (requestURI.contains("/api/admin/") && "DELETE".equals(method)) {
        return false; // 不能删除超级管理员
    }
    return true;
}
```

**允许的操作：**
- ✅ 创建管理员
- ✅ 删除普通管理员和高级管理员
- ❌ 删除超级管理员
- ✅ 重置密码
- ✅ 更新权限
- ✅ 更新管理员状态
- ✅ 获取统计信息

### 普通管理员 (NORMAL)
```java
// 普通管理员权限受限
if ("NORMAL".equals(admin.getAdminLevel())) {
    // 不能执行敏感操作
    if (requestURI.contains("/api/admin/create")) return false;
    if (requestURI.matches("/api/admin/[^/]+$") && "DELETE".equals(method)) return false;
    if (requestURI.contains("/reset-password")) return false;
    if (requestURI.contains("/permissions")) return false;
    if (requestURI.contains("/status")) return false;
    if (requestURI.contains("/statistics")) return false;
}
```

**允许的操作：**
- ❌ 创建管理员
- ❌ 删除管理员
- ❌ 重置密码
- ❌ 更新权限
- ❌ 更新管理员状态
- ❌ 获取统计信息
- ✅ 查看管理员列表
- ✅ 查看管理员详情
- ✅ 修改自己的密码

## 错误响应

### 401 未授权
```json
{
    "code": 401,
    "message": "管理员未登录，请先登录"
}
```

**触发条件：**
- 缺少Authorization头
- Token无效或已过期
- Token格式错误
- 管理员账号不存在

### 403 权限不足
```json
{
    "code": 403,
    "message": "权限不足，无法执行此操作"
}
```

**触发条件：**
- 管理员账户被禁用
- 权限等级不足
- 尝试执行敏感操作

## 使用示例

### 1. 正常请求
```http
GET /api/admin/list
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

**响应：**
- ✅ 通过拦截器验证
- ✅ 返回管理员列表

### 2. 权限不足请求
```http
POST /api/admin/create
Authorization: Bearer {normal_admin_token}
Content-Type: application/json

{
    "username": "newadmin",
    "password": "password123"
}
```

**响应：**
```json
{
    "code": 403,
    "message": "权限不足，无法执行此操作"
}
```

### 3. Token无效请求
```http
GET /api/admin/me
Authorization: Bearer invalid_token
```

**响应：**
```json
{
    "code": 401,
    "message": "Token无效或已过期，请重新登录"
}
```

## 日志记录

### 调试日志
```java
log.debug("管理员拦截器处理请求: {} {}", method, requestURI);
log.debug("管理员拦截器验证通过: adminId={}, username={}, level={}", 
    admin.getAdminId(), admin.getUsername(), admin.getAdminLevel());
```

### 警告日志
```java
log.warn("管理员请求缺少Authorization头: {}", requestURI);
log.warn("管理员Token验证失败: {}", requestURI);
log.warn("管理员权限不足: adminId={}, username={}, operation={}", 
    adminId, admin.getUsername(), method + " " + requestURI);
```

## 安全特性

### 1. Token验证
- 支持Bearer Token格式
- 验证Token签名和过期时间
- 从Token中提取管理员ID

### 2. 权限检查
- 基于管理员级别的权限控制
- 敏感操作自动识别
- 细粒度权限控制

### 3. 状态检查
- 验证管理员账户状态
- 防止被禁用账户访问

### 4. 日志审计
- 记录所有拦截操作
- 便于安全审计和问题排查

## 配置建议

### 1. 生产环境
- 启用详细日志记录
- 定期审查权限设置
- 监控异常访问尝试

### 2. 开发环境
- 可以临时降低权限要求
- 启用调试日志
- 使用测试管理员账号

### 3. 安全加固
- 定期更换JWT密钥
- 实施IP白名单
- 添加登录失败锁定机制

## 故障排除

### 1. 拦截器不生效
- 检查WebConfig配置
- 确认路径匹配规则
- 验证拦截器Bean注册

### 2. 权限检查失败
- 确认管理员级别设置
- 检查权限字符串格式
- 验证敏感操作识别逻辑

### 3. Token验证失败
- 检查JWT密钥配置
- 确认Token格式正确
- 验证Token过期时间

---

**文档版本**: 1.0  
**最后更新**: 2025-01-22  
**维护人员**: NJU Market开发团队
