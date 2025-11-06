# NJUMarket Spring Security 标准实现文档

## 📋 目录
- [概述](#概述)
- [架构设计](#架构设计)
- [核心组件](#核心组件)
- [认证流程](#认证流程)
- [使用指南](#使用指南)
- [最佳实践](#最佳实践)
- [迁移说明](#迁移说明)

---

## 概述

### 版本信息
- **实现版本**: v2.0
- **基于**: Spring Security 6.x
- **认证方式**: JWT Token
- **状态**: ✅ 已完成标准实现

### 设计目标
1. **符合Spring Security标准实践**：使用Filter链和方法级安全控制
2. **保持向后兼容**：Service层仍可使用`UserHolder`
3. **代码简洁**：使用注解替代手动获取用户信息
4. **类型安全**：直接注入User对象，而非String userId
5. **灵活扩展**：支持方法级权限控制

---

## 架构设计

### 整体架构

```
请求 → JwtAuthenticationFilter → Spring Security Filter链 → Controller → Service
       ↓
   设置SecurityContext
   设置UserHolder（向后兼容）
```

### 核心组件关系

```
SecurityConfig (配置类)
    ↓
JwtAuthenticationFilter (JWT认证Filter)
    ↓
SecurityContextHolder (Spring Security上下文)
    ↓
CurrentUserArgumentResolver (参数解析器)
    ↓
@CurrentUser User user (Controller参数注入)
```

---

## 核心组件

### 1. JwtAuthenticationFilter

**位置**: `com.njumarket.njumarket.filter.JwtAuthenticationFilter`

**职责**:
- 从请求头提取JWT Token
- 验证Token有效性
- 查询用户信息并验证状态
- 设置Spring Security的`SecurityContext`
- 保持向后兼容：设置`UserHolder`

**关键代码**:
```java
@Override
protected void doFilterInternal(HttpServletRequest request, 
                               HttpServletResponse response, 
                               FilterChain filterChain) {
    // 1. 提取Token
    String token = getTokenFromRequest(request);
    
    // 2. 验证Token
    if (!jwtUtils.validateToken(token)) {
        // 返回401
        return;
    }
    
    // 3. 查询用户
    User user = userRepository.findById(userId).orElse(null);
    
    // 4. 设置Spring Security Authentication
    UsernamePasswordAuthenticationToken authentication = 
        new UsernamePasswordAuthenticationToken(user, null, Collections.emptyList());
    SecurityContextHolder.getContext().setAuthentication(authentication);
    
    // 5. 保持向后兼容
    UserHolder.saveUser(user);
    
    // 6. 继续Filter链
    filterChain.doFilter(request, response);
}
```

**特点**:
- 只处理`/api/user/**`和`/api/contact/**`路径
- 排除认证相关接口（登录、注册等）
- 认证失败时返回统一的JSON错误响应

---

### 2. SecurityConfig

**位置**: `com.njumarket.njumarket.config.SecurityConfig`

**职责**:
- 配置Spring Security Filter链
- 配置路径权限规则
- 启用方法级安全控制

**关键配置**:
```java
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true) // 启用方法级安全
public class SecurityConfig {
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // 公开接口
                .requestMatchers("/api/user/auth/login", ...).permitAll()
                // 需要认证的接口
                .requestMatchers("/api/user/**", "/api/contact/**").authenticated()
            )
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        
        return http.build();
    }
}
```

**权限规则**:
- **公开访问**: `/api/user/auth/**`（登录、注册等）
- **需要认证**: `/api/user/**`、`/api/contact/**`
- **管理员接口**: `/api/admin/**`（由AdminInterceptor处理）

---

### 3. @CurrentUser 注解

**位置**: `com.njumarket.njumarket.annotation.CurrentUser`

**用途**: 在Controller方法参数中注入当前登录用户

**定义**:
```java
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface CurrentUser {
}
```

**使用示例**:
```java
@PostMapping("/send")
public Result sendMessage(@CurrentUser User user, 
                         @RequestBody SendMessageRequest request) {
    return contactService.sendMessage(user.getUserId(), request);
}
```

---

### 4. CurrentUserArgumentResolver

**位置**: `com.njumarket.njumarket.resolver.CurrentUserArgumentResolver`

**职责**: 解析`@CurrentUser`注解，从`SecurityContext`获取User对象

**实现**:
```java
@Component
public class CurrentUserArgumentResolver implements HandlerMethodArgumentResolver {
    
    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(CurrentUser.class) &&
               parameter.getParameterType().equals(User.class);
    }
    
    @Override
    public Object resolveArgument(...) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            Object principal = authentication.getPrincipal();
            if (principal instanceof User) {
                return principal;
            }
        }
        return null;
    }
}
```

**注册**: 通过`WebMvcConfig`注册到Spring MVC

---

### 5. UserHolder（向后兼容）

**位置**: `com.njumarket.njumarket.utils.UserHolder`

**更新**: 优先从Spring Security `SecurityContext`获取用户

**实现**:
```java
public static User getUser() {
    // 优先从Spring Security SecurityContext获取
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication != null && authentication.isAuthenticated()) {
        Object principal = authentication.getPrincipal();
        if (principal instanceof User) {
            return (User) principal;
        }
    }
    
    // 向后兼容：从ThreadLocal获取（用于Service层等非Controller场景）
    return tl.get();
}
```

**使用场景**:
- Service层：`User currentUser = UserHolder.getUser();`
- 非Controller场景：异步任务、定时任务等

---

## 认证流程

### 1. 用户登录流程

```
1. 用户发送登录请求 → POST /api/user/auth/login
2. UserService验证用户信息
3. 生成JWT Token（使用JwtUtils）
4. 返回Token给客户端
```

### 2. API请求认证流程

```
1. 客户端发送请求 + Authorization: Bearer <token>
2. JwtAuthenticationFilter拦截请求
   ├─ 提取Token
   ├─ 验证Token有效性
   ├─ 查询用户信息
   ├─ 验证用户状态（ACTIVE）
   ├─ 设置SecurityContext
   └─ 设置UserHolder（向后兼容）
3. Spring Security Filter链继续
4. Controller方法执行
   ├─ @PreAuthorize("isAuthenticated()") 验证
   └─ @CurrentUser User user 注入用户对象
5. Service层执行（可使用UserHolder.getUser()）
```

### 3. 认证失败处理

**Token缺失**:
```json
{
  "success": false,
  "errorMsg": "用户未登录，请先登录"
}
```

**Token无效或过期**:
```json
{
  "success": false,
  "errorMsg": "Token无效或已过期，请重新登录"
}
```

**用户状态异常**:
```json
{
  "success": false,
  "errorMsg": "账户已被暂停，请联系管理员了解详情"
}
```

---

## 使用指南

### Controller层

#### 1. 类级别认证（推荐）

```java
@RestController
@RequestMapping("/api/user/profile")
@PreAuthorize("isAuthenticated()") // 所有接口都需要认证
public class UserProfileController {
    
    @GetMapping("/me")
    public Result getCurrentUserProfile() {
        // 方法内无需额外认证检查
    }
}
```

#### 2. 方法级别认证

```java
@RestController
@RequestMapping("/api/user/auth")
public class UserAuthController {
    
    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()") // 只有这个方法需要认证
    public Result getCurrentUser() {
        return userService.getCurrentUser();
    }
}
```

#### 3. 注入当前用户

```java
@PostMapping("/send")
public Result sendMessage(@CurrentUser User user, // 直接注入User对象
                         @RequestBody SendMessageRequest request) {
    return contactService.sendMessage(user.getUserId(), request);
}
```

#### 4. 方法级权限控制（高级用法）

```java
// 只有管理员可以访问
@PreAuthorize("hasRole('ADMIN')")
@DeleteMapping("/{id}")
public Result delete(@PathVariable String id) {
    // ...
}

// 只能访问自己的数据
@PreAuthorize("#userId == authentication.principal.userId")
@GetMapping("/{userId}")
public Result getUser(@PathVariable String userId) {
    // ...
}
```

---

### Service层

#### 使用UserHolder（向后兼容）

```java
@Service
public class OrderServiceImpl implements OrderService {
    
    public Result createOrder(OrderDTO orderDTO) {
        // 从UserHolder获取当前用户（自动从SecurityContext获取）
        User currentUser = UserHolder.getUser();
        if (currentUser == null) {
            return Result.fail("用户未登录");
        }
        
        // 使用currentUser.getUserId()
        // ...
    }
}
```

**注意**: `UserHolder.getUser()`会优先从Spring Security `SecurityContext`获取，如果没有则从ThreadLocal获取（向后兼容）。

---

## 最佳实践

### 1. Controller层

✅ **推荐做法**:
```java
@RestController
@RequestMapping("/api/user/profile")
@PreAuthorize("isAuthenticated()") // 类级别认证
public class UserProfileController {
    
    @PostMapping("/avatar")
    public Result uploadAvatar(@CurrentUser User user, // 使用@CurrentUser注入
                              @RequestParam("file") MultipartFile file) {
        return userProfileService.uploadAvatar(user.getUserId(), file);
    }
}
```

❌ **不推荐做法**:
```java
@RestController
@RequestMapping("/api/user/profile")
public class UserProfileController {
    
    @PostMapping("/avatar")
    public Result uploadAvatar(@RequestAttribute("userId") String userId) {
        // 使用@RequestAttribute（已废弃）
    }
    
    @PostMapping("/avatar")
    public Result uploadAvatar() {
        User user = UserHolder.getUser(); // Controller层不推荐使用UserHolder
    }
}
```

### 2. Service层

✅ **推荐做法**:
```java
@Service
public class OrderServiceImpl implements OrderService {
    
    public Result createOrder(OrderDTO orderDTO) {
        User currentUser = UserHolder.getUser(); // Service层使用UserHolder
        // ...
    }
}
```

### 3. 权限控制

✅ **推荐做法**:
```java
// 类级别：所有接口都需要认证
@PreAuthorize("isAuthenticated()")
public class UserProfileController {
    
    // 方法级别：特定方法需要特定权限
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public Result delete(@PathVariable String id) {
        // ...
    }
}
```

---

## 迁移说明

### 从LoginInterceptor迁移到Spring Security Filter

#### 已完成的迁移

1. ✅ **删除LoginInterceptor**: 功能已迁移到`JwtAuthenticationFilter`
2. ✅ **更新SecurityConfig**: 配置Filter链和路径权限
3. ✅ **创建@CurrentUser注解**: 支持Controller参数注入
4. ✅ **更新UserHolder**: 优先从SecurityContext获取用户
5. ✅ **重构Controller**: 使用`@CurrentUser`和`@PreAuthorize`

#### Controller迁移对照

**迁移前**:
```java
@PostMapping("/send")
public Result sendMessage(@RequestAttribute("userId") String userId,
                         @RequestBody SendMessageRequest request) {
    return contactService.sendMessage(userId, request);
}
```

**迁移后**:
```java
@RestController
@PreAuthorize("isAuthenticated()")
public class ContactController {
    
    @PostMapping("/send")
    public Result sendMessage(@CurrentUser User user,
                             @RequestBody SendMessageRequest request) {
        return contactService.sendMessage(user.getUserId(), request);
    }
}
```

#### Service层迁移

**无需迁移**: Service层继续使用`UserHolder.getUser()`，会自动从SecurityContext获取。

---

## 配置说明

### SecurityConfig配置

```java
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true) // 启用方法级安全
public class SecurityConfig {
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) {
        http
            // 禁用CSRF（JWT无状态，不需要CSRF保护）
            .csrf(csrf -> csrf.disable())
            
            // 无状态会话（JWT不需要Session）
            .sessionManagement(session -> 
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            
            // 路径权限配置
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/user/auth/**").permitAll() // 公开
                .requestMatchers("/api/user/**", "/api/contact/**").authenticated() // 需要认证
                .anyRequest().permitAll() // 其他允许访问
            )
            
            // 插入JWT Filter
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        
        return http.build();
    }
}
```

### WebMvcConfig配置

```java
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {
    
    private final CurrentUserArgumentResolver currentUserArgumentResolver;
    
    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(currentUserArgumentResolver); // 注册参数解析器
    }
}
```

---

## 常见问题

### 1. 如何获取当前用户？

**Controller层**:
```java
@PostMapping("/example")
public Result example(@CurrentUser User user) {
    String userId = user.getUserId();
    // ...
}
```

**Service层**:
```java
User currentUser = UserHolder.getUser();
```

**直接使用Spring Security**:
```java
Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
User user = (User) authentication.getPrincipal();
```

### 2. 如何实现方法级权限控制？

```java
// 需要认证
@PreAuthorize("isAuthenticated()")

// 需要特定角色
@PreAuthorize("hasRole('ADMIN')")

// 自定义表达式
@PreAuthorize("#userId == authentication.principal.userId")
```

### 3. 如何排除某些接口不需要认证？

**方式1**: 在SecurityConfig中配置`permitAll()`
```java
.requestMatchers("/api/user/auth/**").permitAll()
```

**方式2**: 在JwtAuthenticationFilter中排除
```java
if (isAuthEndpoint(requestURI)) {
    filterChain.doFilter(request, response);
    return;
}
```

### 4. Service层为什么还能使用UserHolder？

`UserHolder.getUser()`已更新为优先从Spring Security `SecurityContext`获取用户，如果没有则从ThreadLocal获取（向后兼容）。这样Service层代码无需修改即可工作。

---

## 技术细节

### JWT Token格式

**请求头格式**:
```
Authorization: Bearer <token>
```

**Token内容**:
```json
{
  "userId": "USER_1234567890_123",
  "phone": "13800138000",
  "type": "access",
  "iat": 1234567890,
  "exp": 1234654290
}
```

### SecurityContext结构

```java
SecurityContext
  └─ Authentication
      ├─ principal: User对象
      ├─ credentials: null（JWT不需要凭证）
      └─ authorities: []（空集合，当前未使用角色）
```

### Filter执行顺序

```
1. JwtAuthenticationFilter（自定义）
2. UsernamePasswordAuthenticationFilter（Spring Security默认，通常跳过）
3. FilterSecurityInterceptor（Spring Security权限检查）
4. Controller
```

---

## 总结

### 优势

1. ✅ **符合Spring Security标准实践**
2. ✅ **代码简洁**：使用注解替代手动获取
3. ✅ **类型安全**：直接注入User对象
4. ✅ **灵活扩展**：支持方法级权限控制
5. ✅ **向后兼容**：Service层无需修改

### 关键文件

- `JwtAuthenticationFilter.java` - JWT认证Filter
- `SecurityConfig.java` - Spring Security配置
- `@CurrentUser` - 当前用户注解
- `CurrentUserArgumentResolver.java` - 参数解析器
- `UserHolder.java` - 用户上下文工具类（已更新）

### 下一步

- ✅ 已完成：JWT Filter实现、Controller重构
- 📋 可选：实现角色和权限系统（使用`GrantedAuthority`）
- 📋 可选：实现方法级权限表达式（如`@PreAuthorize("#userId == authentication.principal.userId")`）

---

**文档版本**: v2.0  
**最后更新**: 2025-01-XX  
**维护者**: NJUMarket 开发团队

