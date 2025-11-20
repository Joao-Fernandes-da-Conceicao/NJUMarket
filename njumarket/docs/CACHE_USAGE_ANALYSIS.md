# 项目缓存使用情况分析

## 📋 概述

本文档详细分析项目中缓存的使用情况（除了登录相关的 Token 和验证码缓存）。

## 🔍 缓存使用位置

### 1. 商品相关缓存（Commodity Service）

#### 1.1 缓存类型

| 缓存类型 | 缓存 Key | TTL | 使用位置 |
|---------|---------|-----|---------|
| **商品详情** | `cache:commodity:detail:{commodityId}` | 10分钟 | `CommodityQueryServiceImpl.getCommodityById()` |
| **热门商品** | `cache:commodity:hot:{limit}` | 15分钟 | `CommodityQueryServiceImpl.getHotCommodities()` |
| **最新商品** | `cache:commodity:latest:{limit}` | 5分钟 | `CommodityQueryServiceImpl.getLatestCommodities()` |
| **商品分类** | `cache:commodity:categories` | 60分钟 | `CommodityQueryServiceImpl.getCategories()` |
| **商品列表** | `cache:commodity:list:*` | 5分钟 | 搜索/列表查询 |

#### 1.2 缓存策略

**读取策略（Cache Aside）**：
```java
// 先查缓存，未命中则查数据库并写入缓存
String cacheKey = RedisConstants.CACHE_COMMODITY_DETAIL_KEY + commodityId;
CommodityDTO commodityDTO = cacheUtil.getWithFallback(
    cacheKey,
    RedisConstants.CACHE_COMMODITY_DETAIL_TTL * 60,
    CommodityDTO.class,
    () -> {
        // 缓存未命中，从数据库加载
        Commodity commodity = commodityRepository.findById(commodityId)
            .orElseThrow(() -> new BusinessException("商品不存在"));
        return convertToDTO(commodity);
    }
);
```

**写入策略（Cache Aside）**：
```java
// 先更新数据库，再删除缓存
commodityRepository.save(commodity);
evictCommodityCache(commodityId, evictListCache);
```

#### 1.3 缓存清除位置

- `CommodityServiceImpl.updateCommodity()` - 更新商品时删除详情缓存
- `CommodityServiceImpl.shelfCommodity()` - 上架时删除所有相关缓存
- `CommodityServiceImpl.unshelfCommodity()` - 下架时删除所有相关缓存
- `CommodityServiceImpl.deleteCommodity()` - 删除时清除所有缓存
- `InternalController.updateCommodityFull()` - 管理端更新时清除缓存

---

### 2. 用户信息缓存（Auth Service 内部）

#### 2.1 缓存类型

| 缓存类型 | 缓存 Key | TTL | 使用位置 |
|---------|---------|-----|---------|
| **用户基本信息** | `cache:user:info:{userId}` | 30分钟 | `InternalController.getUserById()` |
| **用户档案** | `cache:user:profile:{userId}` | 30分钟 | `InternalController.getUserProfilesByIds()` |

#### 2.2 用户基本信息缓存

**使用位置**：`InternalController.getUserById()`

```java
@GetMapping("/user/{userId}")
public Result getUserById(@PathVariable String userId) {
    // ✅ 使用缓存（最终一致性）
    String cacheKey = RedisConstants.CACHE_USER_INFO_KEY + userId;
    UserInternalDTO dto = cacheUtil.getWithFallback(
        cacheKey,
        RedisConstants.CACHE_USER_INFO_TTL * 60,
        UserInternalDTO.class,
        () -> {
            // 缓存未命中，从数据库加载
            User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("用户不存在"));
            return userInternalDTOConverter.toInternalDTO(user);
        }
    );
    return Result.ok("查询成功", dto);
}
```

**缓存清除位置**：
- `UserServiceImpl.updateUser()` - 更新用户信息时删除缓存
- `UserServiceImpl.updatePhone()` - 更新手机号时删除缓存
- `UserServiceImpl.deleteUser()` - 删除用户时清除缓存
- `AdminServiceImpl.updateUserFull()` - 管理端更新时清除缓存

#### 2.3 用户档案缓存（UserProfile）

**使用位置**：`InternalController.getUserProfilesByIds()`

```java
@GetMapping("/user/profile/batch")
public Result getUserProfilesByIds(@RequestParam List<String> userIds) {
    List<UserProfileInternalDTO> dtos = new ArrayList<>();
    List<String> missingUserIds = new ArrayList<>();
    
    // 1. 先从缓存获取已有的用户档案
    for (String userId : userIds) {
        String cacheKey = RedisConstants.CACHE_USER_PROFILE_KEY + userId;
        UserProfileInternalDTO cached = cacheUtil.get(cacheKey, UserProfileInternalDTO.class);
        if (cached != null) {
            dtos.add(cached);
        } else {
            missingUserIds.add(userId);
        }
    }
    
    // 2. 批量查询缺失的用户档案
    if (!missingUserIds.isEmpty()) {
        List<UserProfile> profiles = userProfileRepository.findByUserIdIn(missingUserIds);
        List<UserProfileInternalDTO> newDtos = userProfileInternalDTOConverter.toUserProfileInternalDTOList(profiles);
        
        // 3. 将新查询的档案写入缓存
        for (UserProfileInternalDTO dto : newDtos) {
            String cacheKey = RedisConstants.CACHE_USER_PROFILE_KEY + dto.getUserId();
            cacheUtil.set(cacheKey, dto, RedisConstants.CACHE_USER_PROFILE_TTL * 60);
        }
        
        dtos.addAll(newDtos);
    }
    
    return Result.ok("批量查询成功", dtos);
}
```

**特点**：
- ✅ **批量查询优化**：优先从缓存获取，缺失的再批量查询数据库
- ✅ **批量写入缓存**：新查询的数据批量写入缓存
- ✅ **减少数据库查询**：充分利用缓存，减少 N+1 查询问题

**缓存清除位置**（`UserProfileServiceImpl`）：
- `updateUserProfile()` - 更新档案时删除缓存
- `uploadAvatar()` - 上传头像时删除缓存
- `updateCreditScore()` - 更新信用分时删除缓存
- `updateBuyerRating()` - 更新买家评分时删除缓存
- `updateSellerRating()` - 更新卖家评分时删除缓存
- `updateTradeStatistics()` - 更新交易统计时删除缓存
- `checkAndUpgradeVip()` - VIP 升级时删除缓存
- `InternalController.updateUserFull()` - 管理端更新时清除缓存

---

### 3. Feign Client 调用缓存

#### 3.1 UserProfile 缓存利用

**调用链**：
```
其他服务（Commodity/Order/Message）
    ↓
Feign Client: authClient.getUserProfilesByIds()
    ↓
Auth Service: InternalController.getUserProfilesByIds()
    ↓
✅ 使用缓存（优先从缓存获取，缺失的再查数据库）
```

**使用位置**：
1. **Commodity Service**：
   - `CommodityQueryServiceImpl.getCommoditiesWithSeller()` - 商品列表查询
   - `CommodityQueryServiceImpl.getCommoditiesBatchStatus()` - 批量查询商品状态

2. **Order Service**：
   - `OrderServiceImpl.getOrdersBatchStatus()` - 批量查询订单状态

3. **Message Service**：
   - `ContactServiceImpl.updateConversationVisibility()` - 更新会话可见性

**优势**：
- ✅ Feign Client 调用时自动利用 Auth 服务内部的缓存
- ✅ 批量查询时减少数据库查询次数
- ✅ 多个服务共享同一份缓存数据

#### 3.2 用户信息缓存利用

**调用链**：
```
其他服务
    ↓
Feign Client: authClient.getUserById()
    ↓
Auth Service: InternalController.getUserById()
    ↓
✅ 使用缓存（Cache Aside 模式）
```

**使用位置**：
- 各服务通过 Feign Client 查询用户信息时，自动利用 Auth 服务内部的缓存

---

### 4. 前端缓存（localStorage）

#### 4.1 Profile 缓存

**位置**：`njumarket-front/NJUMarket/src/utils/profileCache.js`

**特性**：
- ✅ **LRU 缓存机制**：限制缓存大小（200个用户），淘汰最久未使用的数据
- ✅ **过期时间机制**：30分钟过期
- ✅ **降级机制**：localStorage 不可用时降级到内存 Map

**使用位置**：
- `Messages.vue` - 消息界面显示用户信息
- `SelectCommodityOrOrderDialog.vue` - 选择商品/订单对话框

---

## 📊 缓存使用总结

### 缓存使用情况表

| 服务 | 缓存类型 | 是否使用 | 说明 |
|-----|---------|---------|------|
| **Commodity Service** | 商品详情/列表/热门/最新/分类 | ✅ | 使用 `CacheUtil.getWithFallback()` |
| **Auth Service** | 用户基本信息 | ✅ | `InternalController.getUserById()` 使用缓存 |
| **Auth Service** | 用户档案（UserProfile） | ✅ | `InternalController.getUserProfilesByIds()` 使用缓存 |
| **Auth Service** | UserProfile 单条查询 | ❌ | `UserProfileServiceImpl.getUserProfile()` **未使用缓存** |
| **Admin Service** | 商品缓存清除 | ✅ | 管理端更新商品时清除缓存 |
| **Admin Service** | 用户缓存清除 | ✅ | 管理端更新用户时清除缓存 |
| **Feign Client** | UserProfile 批量查询 | ✅ | 通过调用 Auth 服务利用其内部缓存 |
| **前端** | Profile localStorage | ✅ | 前端本地缓存用户档案信息 |

---

## ⚠️ 发现的问题

### 1. UserProfile 单条查询未使用缓存

**问题**：
- `UserProfileServiceImpl.getUserProfile()` 方法**未使用缓存**
- 每次查询都直接访问数据库

**代码位置**：
```java
// UserProfileServiceImpl.getUserProfile()
@Override
public Result getUserProfile(String userId) {
    // ❌ 直接查询数据库，未使用缓存
    UserProfile profile = userProfileRepository.findByUserId(userId)
        .orElseThrow(() -> new BusinessException("用户档案不存在"));
    // ...
}
```

**对比**：
- ✅ `InternalController.getUserProfilesByIds()` - **使用了缓存**
- ❌ `UserProfileServiceImpl.getUserProfile()` - **未使用缓存**

**建议**：
可以考虑在 `getUserProfile()` 中也使用缓存，但需要注意：
- 该方法是用户端接口，返回的是 `UserProfileDTO`（包含完整信息）
- 批量查询返回的是 `UserProfileInternalDTO`（内部 DTO）
- 两者结构不同，需要使用不同的缓存 Key

---

## ✅ 最佳实践

### 1. Cache Aside 模式

项目中统一使用 **Cache Aside 模式**：

**读取**：
```java
// 先查缓存，未命中则查数据库并写入缓存
cacheUtil.getWithFallback(key, ttl, type, () -> {
    // 从数据库加载
    return loadFromDatabase();
});
```

**写入**：
```java
// 先更新数据库，再删除缓存
repository.save(entity);
cacheUtil.delete(cacheKey);
```

### 2. 批量查询优化

**UserProfile 批量查询**采用了智能缓存策略：
1. 优先从缓存获取已有的数据
2. 批量查询缺失的数据
3. 批量写入缓存

这样可以：
- ✅ 减少数据库查询次数
- ✅ 充分利用缓存
- ✅ 避免 N+1 查询问题

### 3. 缓存清除策略

**精确清除**：
- 商品详情更新 → 只删除详情缓存
- 商品状态变更 → 删除所有相关缓存（包括列表缓存）

**通配符清除**：
- 使用 `deleteByPattern()` 清除所有相关的列表缓存

---

## 📈 性能影响

### 缓存命中率

- **商品详情缓存**：高频访问，缓存命中率高
- **热门/最新商品**：访问频率高，缓存效果好
- **UserProfile 批量查询**：批量查询时缓存命中率高（因为多个商品/订单可能共享相同的卖家）

### 数据库查询减少

- **商品查询**：缓存命中时减少数据库查询
- **UserProfile 批量查询**：批量查询时优先使用缓存，显著减少数据库查询次数

---

## 🔧 优化建议

### 1. UserProfile 单条查询缓存

可以考虑在 `UserProfileServiceImpl.getUserProfile()` 中添加缓存：

```java
@Override
public Result getUserProfile(String userId) {
    // ✅ 使用缓存
    String cacheKey = RedisConstants.CACHE_USER_PROFILE_DETAIL_KEY + userId;
    UserProfileDTO dto = cacheUtil.getWithFallback(
        cacheKey,
        RedisConstants.CACHE_USER_PROFILE_TTL * 60,
        UserProfileDTO.class,
        () -> {
            UserProfile profile = userProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException("用户档案不存在"));
            return convertToDTO(profile);
        }
    );
    // ...
}
```

**注意**：需要使用不同的缓存 Key（`CACHE_USER_PROFILE_DETAIL_KEY`），避免与批量查询的缓存冲突。

### 2. 缓存预热

可以考虑在服务启动时预热热门数据：
- 热门商品
- 最新商品
- 常用分类

### 3. 缓存监控

建议添加缓存监控：
- 缓存命中率
- 缓存大小
- 缓存过期情况

---

## 📝 总结

### 缓存使用情况

1. ✅ **商品服务**：全面使用缓存（详情、列表、热门、最新、分类）
2. ✅ **Auth 服务**：
   - ✅ 用户基本信息缓存（`getUserById`）
   - ✅ 用户档案批量查询缓存（`getUserProfilesByIds`）
   - ❌ 用户档案单条查询缓存（`getUserProfile`）**未使用**
3. ✅ **Feign Client**：通过调用 Auth 服务自动利用其内部缓存
4. ✅ **前端**：使用 localStorage 缓存用户档案

### 关键发现

- **UserProfile 缓存主要在批量查询时使用**（`getUserProfilesByIds`）
- **Feign Client 调用时自动利用 Auth 服务内部的缓存**
- **Auth 服务内部确实使用了缓存**，不仅仅是 Feign Client 调用时才用
- **单条查询的 `getUserProfile()` 方法未使用缓存**，可以考虑优化

