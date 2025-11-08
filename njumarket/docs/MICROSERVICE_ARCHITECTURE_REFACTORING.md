# 微服务架构重整指南

## 一、循环引用问题的根本原因

### 1.1 问题现象
- `User` 和 `UserProfile` 之间存在双向 `@OneToOne` 关联
- 通过 Feign Client 返回实体对象时，Jackson 序列化出现循环引用
- 导致序列化深度超过限制（1000层）或类型转换错误（`LinkedHashMap` 无法转换为 `UserProfile`）

### 1.2 为什么单体项目没有这个问题？

**单体项目中的处理方式：**
```java
// 单体项目中，JPA 懒加载机制可以避免序列化关联对象
@OneToOne(fetch = FetchType.LAZY)
private UserProfile userProfile;

// 当需要序列化时，可以：
// 1. 使用 @JsonIgnore 忽略关联字段
// 2. 使用 DTO 进行数据传输
// 3. 在 Service 层手动控制序列化范围
```

**微服务中的问题：**
```java
// ❌ 错误：直接返回实体对象
@GetMapping("/user/profile/batch")
public Result getUserProfilesByIds(@RequestParam List<String> userIds) {
    List<UserProfile> profiles = userProfileRepository.findByUserIdIn(userIds);
    return Result.ok(profiles);  // 直接返回实体，包含关联对象
}

// 当 Feign Client 调用时：
// 1. Jackson 尝试序列化 UserProfile
// 2. 发现 UserProfile.user 字段（虽然有 @JsonIgnore，但可能在某些情况下失效）
// 3. 尝试序列化 User
// 4. 发现 User.userProfile 字段
// 5. 形成循环引用 → 无限递归或深度超限
```

### 1.3 当前架构的问题

**问题1：直接返回实体类**
```java
// ❌ auth-service 的 InternalController
@GetMapping("/user/profile/batch")
public Result getUserProfilesByIds(@RequestParam List<String> userIds) {
    List<UserProfile> profiles = userProfileRepository.findByUserIdIn(userIds);
    return Result.ok(profiles);  // 直接返回实体
}
```

**问题2：类型信息丢失**
```java
// ❌ Result 类不是泛型的
public class Result {
    private Object data;  // 类型信息丢失
}

// 导致 Feign Client 反序列化时：
// JSON → LinkedHashMap（因为不知道具体类型）
// 而不是 JSON → UserProfile
```

**问题3：跨服务传递完整实体**
- 实体类包含数据库关联、业务逻辑、敏感信息
- 不应该跨服务传递，应该只传递必要的数据

---

## 二、微服务架构重整方案

### 2.1 核心原则

1. **服务间只传递 DTO，不传递实体类**
2. **每个服务维护自己的实体类副本（如果需要）**
3. **使用专门的内部 DTO 用于服务间通信**
4. **打破实体间的双向关联（在跨服务场景中）**

### 2.2 架构重整步骤

#### 步骤1：创建内部 DTO（Internal DTO）

**在 `njumarket-common` 中创建内部 DTO：**

```java
// UserProfileInternalDTO.java
package com.njumarket.njumarket.dto.internal;

import lombok.Data;
import java.io.Serializable;

/**
 * 用户档案内部传输对象（用于服务间通信）
 * 不包含关联对象，只包含必要字段
 */
@Data
public class UserProfileInternalDTO implements Serializable {
    private String profileId;
    private String userId;
    private String nickname;
    private String avatar;
    private String location;
    private String bio;
    private Boolean sellerOrderHasNew;
    private Boolean buyerOrderHasNew;
    // 不包含 User 关联对象
}
```

```java
// UserInternalDTO.java
package com.njumarket.njumarket.dto.internal;

import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户内部传输对象（用于服务间通信）
 * 不包含关联对象，只包含必要字段
 */
@Data
public class UserInternalDTO implements Serializable {
    private String userId;
    private String username;
    private String primaryPhone;
    private String accountStatus;
    private LocalDateTime registerTime;
    // 不包含 UserProfile、Order、Complaint 等关联对象
}
```

#### 步骤2：修改 InternalController 返回 DTO

**修改 `auth-service` 的 `InternalController`：**

```java
// ✅ 正确：返回内部 DTO
@GetMapping("/user/profile/batch")
public Result getUserProfilesByIds(@RequestParam List<String> userIds) {
    List<UserProfile> profiles = userProfileRepository.findByUserIdIn(userIds);
    
    // 转换为内部 DTO（不包含关联对象）
    List<UserProfileInternalDTO> dtos = profiles.stream()
        .map(this::convertToInternalDTO)
        .collect(Collectors.toList());
    
    return Result.ok(dtos);
}

private UserProfileInternalDTO convertToInternalDTO(UserProfile profile) {
    UserProfileInternalDTO dto = new UserProfileInternalDTO();
    dto.setProfileId(profile.getProfileId());
    dto.setUserId(profile.getUserId());
    dto.setNickname(profile.getNickname());
    dto.setAvatar(profile.getAvatar());
    dto.setLocation(profile.getLocation());
    dto.setBio(profile.getBio());
    dto.setSellerOrderHasNew(profile.getSellerOrderHasNew());
    dto.setBuyerOrderHasNew(profile.getBuyerOrderHasNew());
    return dto;
}
```

#### 步骤3：修改 Feign Client 使用 DTO

**修改 `commodity-service` 的 `AuthClient`：**

```java
@FeignClient(name = "njumarket-service-auth", path = "/api/internal")
public interface AuthClient {
    
    /**
     * 批量查询用户档案（内部接口）
     * ✅ 返回内部 DTO，而不是实体类
     */
    @GetMapping("/user/profile/batch")
    Result getUserProfilesByIds(@RequestParam List<String> userIds);
    // 注意：Result 的 data 字段类型是 Object，需要在调用方转换
}
```

**修改 `CommodityQueryServiceImpl`：**

```java
// ✅ 正确：使用内部 DTO
Result profilesResult = authClient.getUserProfilesByIds(new ArrayList<>(sellerIds));
if (profilesResult.getSuccess() && profilesResult.getData() != null) {
    List<UserProfileInternalDTO> profiles = objectMapper.convertValue(
        profilesResult.getData(),
        new TypeReference<List<UserProfileInternalDTO>>() {}
    );
    
    // 转换为 Map，只提取需要的字段
    profileMap = profiles.stream()
        .collect(Collectors.toMap(
            UserProfileInternalDTO::getUserId,
            dto -> {
                // 只提取需要的字段（nickname, avatar）
                // 不需要完整的 UserProfile 对象
                return dto;
            }
        ));
}
```

#### 步骤4：使用专门的内部 DTO 转换器

**创建 `InternalDTOConverter`：**

```java
package com.njumarket.njumarket.dto.internal;

import com.njumarket.njumarket.entity.User;
import com.njumarket.njumarket.entity.UserProfile;
import org.springframework.stereotype.Component;

@Component
public class InternalDTOConverter {
    
    public UserProfileInternalDTO toInternalDTO(UserProfile profile) {
        if (profile == null) return null;
        
        UserProfileInternalDTO dto = new UserProfileInternalDTO();
        dto.setProfileId(profile.getProfileId());
        dto.setUserId(profile.getUserId());
        dto.setNickname(profile.getNickname());
        dto.setAvatar(profile.getAvatar());
        dto.setLocation(profile.getLocation());
        dto.setBio(profile.getBio());
        dto.setSellerOrderHasNew(profile.getSellerOrderHasNew());
        dto.setBuyerOrderHasNew(profile.getBuyerOrderHasNew());
        return dto;
    }
    
    public UserInternalDTO toInternalDTO(User user) {
        if (user == null) return null;
        
        UserInternalDTO dto = new UserInternalDTO();
        dto.setUserId(user.getUserId());
        dto.setUsername(user.getUsername());
        dto.setPrimaryPhone(user.getPrimaryPhone());
        dto.setAccountStatus(user.getAccountStatus());
        dto.setRegisterTime(user.getRegisterTime());
        return dto;
    }
}
```

---

## 三、架构对比

### 3.1 单体架构 vs 微服务架构

| 方面 | 单体架构 | 微服务架构（当前） | 微服务架构（推荐） |
|------|---------|------------------|------------------|
| **数据传输** | 实体类或 DTO | ❌ 实体类 | ✅ 内部 DTO |
| **关联对象** | 可以使用懒加载 | ❌ 导致循环引用 | ✅ 不包含关联对象 |
| **类型安全** | 编译时检查 | ❌ 运行时类型转换 | ✅ 明确的 DTO 类型 |
| **服务边界** | 无边界 | ❌ 实体类跨服务传递 | ✅ 清晰的边界 |
| **性能** | 数据库关联查询 | ❌ 序列化/反序列化开销 | ✅ 最小化数据传输 |

### 3.2 当前架构的问题总结

1. **直接返回实体类**：违反微服务边界原则
2. **类型信息丢失**：`Result` 不是泛型，导致类型转换错误
3. **循环引用**：实体间的双向关联导致序列化问题
4. **数据泄露风险**：可能传递敏感信息或不需要的字段

### 3.3 推荐架构的优势

1. **清晰的边界**：每个服务只暴露必要的数据
2. **类型安全**：使用明确的 DTO 类型，避免类型转换错误
3. **性能优化**：只传输需要的数据，减少网络开销
4. **易于维护**：DTO 变更不影响其他服务

---

## 四、实施计划

### 阶段1：创建内部 DTO（优先级：高）

1. 在 `njumarket-common` 中创建 `internal` 包
2. 创建 `UserProfileInternalDTO` 和 `UserInternalDTO`
3. 创建 `InternalDTOConverter` 工具类

### 阶段2：修改 auth-service（优先级：高）

1. 修改 `InternalController` 返回内部 DTO
2. 使用 `InternalDTOConverter` 进行转换
3. 测试内部 API 是否正常工作

### 阶段3：修改调用方服务（优先级：中）

1. 修改 `commodity-service` 的 `CommodityQueryServiceImpl`
2. 修改 `order-service` 的 `OrderServiceImpl`
3. 修改 `message-service` 的 `ContactServiceImpl`
4. 修改 `admin-service` 的 `AdminServiceImpl`

### 阶段4：清理和优化（优先级：低）

1. 移除实体类上的 `@JsonIgnore`（如果不再需要）
2. 统一使用 `ObjectMapper` 进行类型转换
3. 添加单元测试验证 DTO 转换

---

## 五、最佳实践

### 5.1 DTO 设计原则

1. **只包含必要字段**：不包含关联对象、敏感信息
2. **使用 `Serializable`**：确保可以跨网络传输
3. **明确的命名**：使用 `InternalDTO` 后缀区分内部和外部 DTO
4. **版本控制**：DTO 变更时考虑向后兼容

### 5.2 服务间通信原则

1. **最小化数据传输**：只传递必要的数据
2. **批量查询优先**：避免 N+1 查询问题
3. **错误处理**：统一的错误处理和重试机制
4. **监控和日志**：记录服务间调用的性能指标

### 5.3 实体类设计原则

1. **避免双向关联**：在跨服务场景中，只保留单向关联
2. **使用懒加载**：减少数据库查询
3. **分离关注点**：实体类用于数据库操作，DTO 用于数据传输

---

## 六、总结

### 6.1 循环引用的根本原因

- **实体间的双向关联** + **跨服务传递实体类** = 循环引用问题

### 6.2 解决方案

- **使用内部 DTO**：打破实体间的关联，只传递必要数据
- **明确的类型定义**：避免类型转换错误
- **清晰的服务边界**：每个服务只暴露必要的数据

### 6.3 架构演进方向

从 **"共享实体类"** → **"共享 DTO"** → **"服务独立，通过 DTO 通信"**

这是微服务架构演进的自然过程，也是解决循环引用问题的根本方法。

