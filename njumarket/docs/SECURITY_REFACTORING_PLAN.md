# 安全机制重构方案

## 一、设计目标

1. **统一安全机制**：参考单体版（1.4.1）的Spring Security实现，在微服务架构中统一安全机制
2. **简化配置**：后端服务使用简化的Spring Security配置，只用于SecurityContext管理
3. **可靠性**：确保Gateway和后端服务之间的用户上下文传递可靠
4. **向后兼容**：保持@CurrentUser注解和SecurityUtils的正常工作

## 二、架构设计

### 2.1 Gateway层（响应式）

**职责**：
- JWT Token验证（格式、过期、Redis验证）
- 提取userId并传递给后端服务
- 不设置SecurityContext（响应式环境不支持）

**实现**：
- `JwtAuthenticationFilter`：验证JWT，设置`X-User-Id`请求头
- `AdminAuthenticationFilter`：管理员JWT验证，设置`X-Admin-Id`请求头

### 2.2 后端服务层（Servlet）

**职责**：
- 从Gateway传递的请求头获取用户ID
- 通过Feign Client获取完整的User对象
- 设置Spring Security SecurityContext
- 同时设置UserHolder（向后兼容）

**实现**：
- `UserContextFilter`：从`X-User-Id`获取用户，设置SecurityContext和UserHolder
- `SecurityConfig`：简化的Spring Security配置，只用于SecurityContext管理
- `CurrentUserArgumentResolver`：从SecurityContext解析@CurrentUser参数

### 2.3 工具类

**SecurityUtils**：
- 优先从SecurityContext获取用户
- 向后兼容UserHolder
- 提供类型安全的鉴权方法

**UserHolder**：
- 优先从SecurityContext获取用户
- 作为ThreadLocal备份

## 三、实现步骤

### 步骤1：重构Gateway层的JWT Filter
- 确保JWT验证逻辑正确
- 确保userId提取和传递正确
- 添加详细的日志

### 步骤2：在后端服务中引入Spring Security
- 添加Spring Security依赖
- 创建简化的SecurityConfig
- 配置为无状态会话

### 步骤3：重构UserContextFilter
- 从X-User-Id获取userId
- 通过Feign Client获取User对象
- 设置SecurityContext（UsernamePasswordAuthenticationToken）
- 同时设置UserHolder

### 步骤4：确保@CurrentUser注解工作
- 确保CurrentUserArgumentResolver正确注册
- 确保从SecurityContext正确解析User

### 步骤5：更新所有服务的SecurityConfig
- auth-service
- commodity-service
- order-service
- message-service
- admin-service

### 步骤6：测试验证
- 测试用户认证流程
- 测试@CurrentUser注解
- 测试SecurityUtils方法
- 测试管理员认证

## 四、关键代码结构

### Gateway层
```
JwtAuthenticationFilter
  - 验证JWT Token
  - 验证Redis中的Token
  - 提取userId
  - 设置X-User-Id请求头
```

### 后端服务层
```
UserContextFilter (OncePerRequestFilter)
  - 从X-User-Id获取userId
  - 通过AuthClient获取User对象
  - 设置SecurityContext
  - 设置UserHolder
  - 清理ThreadLocal

SecurityConfig
  - 禁用CSRF
  - 无状态会话
  - 允许所有请求（认证由Filter处理）

CurrentUserArgumentResolver
  - 从SecurityContext获取User
  - 支持@CurrentUser注解
```

## 五、优势

1. **统一性**：与单体版保持一致的安全机制
2. **可靠性**：双重保障（SecurityContext + UserHolder）
3. **简化**：后端服务不需要完整的Spring Security Filter链
4. **兼容性**：保持@CurrentUser和SecurityUtils的向后兼容

