# NJU Market 登录和个人信息管理系统技术文档

## 概述

本文档详细描述了NJU Market项目中用户登录认证和个人信息管理系统的实现，包括用户认证、个人信息查询、头像上传存储等核心功能的技术架构和实现细节。

## 系统架构

### 技术栈
- **后端框架**: Spring Boot 3.x
- **数据库**: MySQL 8.0
- **缓存**: Redis
- **安全框架**: Spring Security
- **文档**: Swagger/OpenAPI 3
- **文件存储**: 本地文件系统

### 核心模块
1. 用户认证模块 (Authentication)
2. 个人信息管理模块 (User Profile)
3. 图片上传存储模块 (Image Upload)
4. 安全配置模块 (Security Configuration)

## 1. 用户认证系统

### 1.1 登录功能实现

#### 控制器层
```java
@RestController
@RequestMapping("/api/user/auth")
public class UserAuthController {
    
    @PostMapping("/login")
    public Result login(@RequestBody LoginFormDTO loginForm) {
        return userService.login(loginForm);
    }
    
    @PostMapping("/register")
    public Result register(@RequestBody RegisterDTO registerDTO) {
        return userService.register(registerDTO);
    }
}
```

#### 服务层实现
- **登录验证**: 用户名/密码验证
- **JWT Token生成**: 使用JWT工具类生成访问令牌
- **Redis缓存**: 存储用户会话信息
- **密码加密**: 使用BCrypt进行密码哈希

#### 安全配置
```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) {
        return http
            .csrf().disable()
            .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            .and()
            .authorizeHttpRequests()
            .requestMatchers("/api/user/auth/**").permitAll()
            .anyRequest().authenticated()
            .and()
            .build();
    }
}
```

### 1.2 JWT Token管理

#### Token工具类
```java
@Component
public class JwtUtils {
    
    public String generateToken(String userId) {
        // JWT token生成逻辑
    }
    
    public boolean validateToken(String token) {
        // Token验证逻辑
    }
    
    public String getUserIdFromToken(String token) {
        // 从Token中提取用户ID
    }
}
```

#### 登录拦截器
```java
@Component
public class LoginInterceptor implements HandlerInterceptor {
    
    @Override
    public boolean preHandle(HttpServletRequest request, 
                           HttpServletResponse response, 
                           Object handler) {
        // Token验证和用户信息存储
    }
}
```

## 2. 个人信息管理系统

### 2.1 用户档案实体设计

```java
@Entity
@Table(name = "user_profiles")
@Data
public class UserProfile {
    
    @Id
    @Column(name = "profile_id", length = 50)
    private String profileId;
    
    @Column(name = "user_id", length = 50, nullable = false, unique = true)
    private String userId;
    
    @Column(name = "nickname", length = 50)
    private String nickname;
    
    @Column(name = "avatar", length = 500)
    private String avatar;
    
    @Column(name = "credit_score", nullable = false)
    private Integer creditScore = 100;
    
    @Column(name = "buyer_rating")
    private Double buyerRating = 5.0;
    
    @Column(name = "seller_rating")
    private Double sellerRating = 5.0;
    
    // 其他字段...
}
```

### 2.2 个人信息查询功能

#### 控制器接口
```java
@RestController
@RequestMapping("/api/user/profile")
public class UserProfileController {
    
    @GetMapping("/me")
    public Result getCurrentUserProfile() {
        return userProfileService.getCurrentUserProfile();
    }
    
    @GetMapping("/{userId}")
    public Result getUserProfile(@PathVariable String userId) {
        return userProfileService.getUserProfile(userId);
    }
    
    @PutMapping("/me")
    public Result updateCurrentUserProfile(@RequestBody UserProfileUpdateDTO updateDTO) {
        return userProfileService.updateCurrentUserProfile(updateDTO);
    }
}
```

#### 服务层实现
- **当前用户信息获取**: 从UserHolder中获取当前登录用户信息
- **用户档案查询**: 根据用户ID查询详细档案信息
- **档案更新**: 支持部分字段更新，保持数据一致性

## 3. 图片上传存储系统

### 3.1 Multipart配置

#### 应用配置
```properties
# 文件上传配置
spring.servlet.multipart.enabled=true
spring.servlet.multipart.max-file-size=5MB
spring.servlet.multipart.max-request-size=10MB
spring.servlet.multipart.file-size-threshold=2KB
spring.servlet.multipart.location=${java.io.tmpdir}
spring.servlet.multipart.resolve-lazily=false

# 图片上传配置
app.upload.avatar-path=uploads/avatars
app.image.base-url=http://localhost:8080
```

#### Web配置
```java
@Configuration
public class WebConfig implements WebMvcConfigurer {
    
    @Bean
    public MultipartResolver multipartResolver() {
        return new StandardServletMultipartResolver();
    }
    
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/api/images/avatars/**")
                .addResourceLocations("file:uploads/avatars/");
    }
}
```

### 3.2 头像上传功能

#### 控制器接口
```java
@PostMapping(value = "/avatar", consumes = "multipart/form-data")
public Result uploadAvatar(@RequestParam("file") MultipartFile file) {
    String currentUserId = UserHolder.getUser().getUserId();
    return userProfileService.uploadAvatar(currentUserId, file);
}
```

#### 图片服务实现
```java
@Service
public class ImageServiceImpl implements ImageService {
    
    @Override
    public ImageUploadDTO uploadAvatar(String userId, MultipartFile file) {
        // 1. 文件验证
        if (!validateImageFile(file)) {
            throw new IllegalArgumentException("无效的图片文件");
        }
        
        // 2. 生成唯一文件名
        String fileName = generateUniqueFileName(file.getOriginalFilename(), userId);
        
        // 3. 保存文件
        Path filePath = uploadDir.resolve(fileName);
        Files.copy(file.getInputStream(), filePath);
        
        // 4. 返回上传结果
        return buildUploadResult(fileName, file);
    }
}
```

### 3.3 头像替换机制

#### 旧头像删除逻辑
```java
@Override
public Result uploadAvatar(String userId, MultipartFile file) {
    // 1. 获取用户档案
    UserProfile profile = userProfileRepository.findByUserId(userId).get();
    String oldAvatarUrl = profile.getAvatar();
    
    // 2. 删除旧头像文件
    if (oldAvatarUrl != null && !oldAvatarUrl.trim().isEmpty()) {
        try {
            boolean deleted = imageService.deleteAvatarByUrl(oldAvatarUrl);
            log.info("旧头像删除结果: {}", deleted);
        } catch (Exception e) {
            log.error("删除旧头像失败: {}", e.getMessage());
            // 继续执行，不中断上传流程
        }
    }
    
    // 3. 上传新头像
    ImageUploadDTO uploadResult = imageService.uploadAvatar(userId, file);
    
    // 4. 更新数据库记录
    profile.setAvatar(uploadResult.getImageUrl());
    userProfileRepository.save(profile);
    
    return Result.ok(uploadResult);
}
```

#### URL解析和文件删除
```java
@Override
public boolean deleteAvatarByUrl(String avatarUrl) {
    // 从URL中提取文件名
    String fileName = extractFileNameFromUrl(avatarUrl);
    if (fileName == null) {
        return false;
    }
    
    // 删除物理文件
    Path filePath = Paths.get(avatarUploadPath, fileName);
    if (Files.exists(filePath)) {
        Files.delete(filePath);
        return true;
    }
    return false;
}

private String extractFileNameFromUrl(String avatarUrl) {
    // 解析URL格式: http://localhost:8080/api/images/avatars/filename.png
    String avatarPath = "/api/images/avatars/";
    int pathIndex = avatarUrl.indexOf(avatarPath);
    
    if (pathIndex != -1) {
        return avatarUrl.substring(pathIndex + avatarPath.length());
    }
    return null;
}
```

### 3.4 图片访问机制

#### 图片访问控制器
```java
@RestController
@RequestMapping("/api/images")
public class ImageController {
    
    @GetMapping("/avatars/{fileName}")
    public ResponseEntity<Resource> getAvatar(@PathVariable String fileName) {
        Path filePath = Paths.get(avatarUploadPath, fileName);
        File file = filePath.toFile();
        
        if (!file.exists()) {
            return ResponseEntity.notFound().build();
        }
        
        Resource resource = new FileSystemResource(file);
        String contentType = Files.probeContentType(filePath);
        
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(contentType))
            .header(HttpHeaders.CACHE_CONTROL, "public, max-age=3600")
            .body(resource);
    }
}
```

## 4. 数据流转和存储机制

### 4.1 文件存储结构
```
uploads/
└── avatars/
    ├── 20251022_223254_avatar_USER_1761133111693_499_95ca0845.png
    ├── 20251022_223850_avatar_USER_1761133111693_499_5a8ac485.png
    └── 20251022_224310_avatar_USER_1761133111693_499_e2d4c592.png
```

### 4.2 文件名生成规则
```
格式: {timestamp}_avatar_{userId}_{uuid}.{extension}
示例: 20251022_223254_avatar_USER_1761133111693_499_95ca0845.png
```

### 4.3 URL映射规则
```
访问URL: http://localhost:8080/api/images/avatars/{fileName}
存储路径: uploads/avatars/{fileName}
数据库存储: http://localhost:8080/api/images/avatars/{fileName}
```

## 5. 安全性和验证

### 5.1 文件验证
- **文件类型验证**: 仅允许 jpg, jpeg, png, gif, webp 格式
- **文件大小限制**: 最大5MB
- **文件内容验证**: 检查MIME类型和文件扩展名

### 5.2 用户权限控制
- **登录验证**: 所有个人信息操作需要有效JWT Token
- **用户身份验证**: 只能操作自己的个人信息
- **文件访问控制**: 头像文件通过公开URL访问，但上传需要认证

## 6. 错误处理和日志

### 6.1 异常处理
```java
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(MultipartException.class)
    public Result handleMultipartException(MultipartException e) {
        log.error("文件上传异常: {}", e.getMessage());
        return Result.fail("文件上传失败");
    }
}
```

### 6.2 日志记录
- **上传成功**: 记录用户ID、文件名、文件大小
- **删除操作**: 记录旧头像URL和删除结果
- **错误信息**: 详细记录异常堆栈和上下文信息

## 7. 性能优化

### 7.1 缓存策略
- **图片缓存**: HTTP缓存头设置1小时过期
- **用户信息缓存**: Redis缓存用户会话信息
- **静态资源**: 通过Spring静态资源处理器优化访问

### 7.2 文件管理
- **自动清理**: 上传新头像时自动删除旧头像文件
- **目录管理**: 自动创建上传目录
- **文件命名**: 使用时间戳和UUID避免文件名冲突

## 8. API接口文档

### 8.1 认证接口
```
POST /api/user/auth/login
Content-Type: application/json
Body: {"username": "string", "password": "string"}
Response: {"code": 200, "data": {"token": "jwt_token"}}
```

### 8.2 个人信息接口
```
GET /api/user/profile/me
Authorization: Bearer {token}
Response: {"code": 200, "data": {"userId": "string", "nickname": "string", "avatar": "url"}}
```

### 8.3 头像上传接口
```
POST /api/user/profile/avatar
Authorization: Bearer {token}
Content-Type: multipart/form-data
Body: file (image file)
Response: {"code": 200, "data": {"imageUrl": "url", "fileName": "string"}}
```

## 9. 部署和配置

### 9.1 环境要求
- Java 17+
- MySQL 8.0+
- Redis 6.0+
- 至少100MB磁盘空间用于文件存储

### 9.2 配置说明
- **数据库连接**: 配置MySQL连接信息
- **Redis连接**: 配置Redis服务器信息
- **文件存储**: 配置上传路径和访问URL
- **安全配置**: 配置JWT密钥和过期时间

## 10. 总结

本系统成功实现了完整的用户认证和个人信息管理功能，包括：

1. **安全的用户认证**: JWT Token + Redis会话管理
2. **完整的个人信息管理**: 查询、更新、搜索等功能
3. **高效的文件上传系统**: 支持头像上传、替换、删除
4. **稳定的图片访问机制**: URL映射和静态资源服务
5. **完善的错误处理**: 全局异常处理和详细日志记录

系统具有良好的扩展性和维护性，为后续功能开发奠定了坚实的基础。

---

**文档版本**: 1.0  
**最后更新**: 2025-01-22  
**维护人员**: NJU Market开发团队
