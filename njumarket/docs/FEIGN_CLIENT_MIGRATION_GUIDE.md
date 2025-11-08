# Feign Client 跨服务调用改造指南

## 📋 概述

本文档说明如何将项目中的直接Repository注入改造为Feign Client调用，实现真正的微服务架构。

## ✅ 已完成工作

### 1. Feign Client接口已创建

#### Message Service的Feign Client
- ✅ `AuthClient` - 调用Auth Service
- ✅ `CommodityClient` - 调用Commodity Service  
- ✅ `OrderClient` - 调用Order Service

#### Order Service的Feign Client
- ✅ `AuthClient` - 调用Auth Service
- ✅ `CommodityClient` - 调用Commodity Service
- ✅ `CommodityQueryClient` - 调用Commodity Service查询功能
- ✅ `ChangeRecordClient` - 调用Commodity Service变更记录功能

### 2. 一键启动脚本已创建
- ✅ `start-all-services.bat` (Windows)
- ✅ `start-all-services.sh` (Linux/Mac)

## 🔧 待完成工作

### 阶段1：添加内部API接口

由于Feign Client需要调用其他服务的API，需要在各服务中添加内部接口。

#### 1.1 Auth Service需要添加的接口

在 `UserAuthController` 或新建 `InternalController` 中添加：

```java
@RestController
@RequestMapping("/api/internal")
public class InternalController {
    
    private final UserService userService;
    private final UserProfileService userProfileService;
    
    /**
     * 根据ID查询用户（内部接口）
     */
    @GetMapping("/user/{userId}")
    public Result getUserById(@PathVariable String userId) {
        // 实现逻辑
    }
    
    /**
     * 批量查询用户（内部接口）
     */
    @GetMapping("/user/batch")
    public Result getUsersByIds(@RequestParam List<String> userIds) {
        // 实现逻辑
    }
    
    /**
     * 批量查询用户档案（内部接口）
     */
    @GetMapping("/user/profile/batch")
    public Result getUserProfilesByIds(@RequestParam List<String> userIds) {
        // 实现逻辑
    }
}
```

#### 1.2 Commodity Service需要添加的接口

在 `UserCommodityController` 中添加：

```java
/**
 * 查询商品（带悲观锁，用于创建订单）
 */
@GetMapping("/{commodityId}/for-update")
public Result getCommodityForUpdate(@PathVariable String commodityId) {
    // 使用findByIdForUpdate查询
}

/**
 * 更新商品库存
 */
@PostMapping("/{commodityId}/update-stock")
public Result updateCommodityStock(@PathVariable String commodityId, 
                                  @RequestParam Integer quantity) {
    // 实现库存更新逻辑
}
```

在 `PublicController` 或新建 `InternalController` 中添加：

```java
@GetMapping("/change-record/commodity")
public Result<List<String>> getCommodityChangesAfter(@RequestParam LocalDateTime timestamp) {
    // 调用ChangeRecordService
}

@GetMapping("/change-record/order")
public Result<List<String>> getOrderChangesAfter(@RequestParam LocalDateTime timestamp) {
    // 调用ChangeRecordService
}
```

### 阶段2：改造Service实现类

#### 2.1 改造ContactServiceImpl (Message Service)

**需要改造的地方**：
1. 移除直接注入的Repository：
   - `UserRepository`
   - `UserProfileRepository`
   - `OrderRepository`
   - `CommodityRepository`

2. 添加Feign Client注入：
```java
private final AuthClient authClient;
private final CommodityClient commodityClient;
private final OrderClient orderClient;
```

3. 替换所有Repository调用为Feign Client调用：

**示例改造**：
```java
// ❌ 旧代码
Optional<User> receiverOpt = userRepository.findById(request.getReceiverId());

// ✅ 新代码
Result<User> userResult = authClient.getUserById(request.getReceiverId());
if (!userResult.getSuccess() || userResult.getData() == null) {
    throw new BusinessException("接收者不存在");
}
User receiver = userResult.getData();
```

**批量查询改造**：
```java
// ❌ 旧代码
List<UserProfile> profiles = userProfileRepository.findByUserIdIn(new ArrayList<>(userIds));

// ✅ 新代码
Result<List<UserProfile>> profileResult = authClient.getUserProfilesByIds(userIds);
if (!profileResult.getSuccess() || profileResult.getData() == null) {
    throw new BusinessException("查询用户档案失败");
}
List<UserProfile> profiles = profileResult.getData();
```

#### 2.2 改造OrderServiceImpl (Order Service)

**需要改造的地方**：
1. 移除直接注入的Repository：
   - `CommodityRepository`
   - `UserRepository`
   - `UserProfileRepository`

2. 移除直接注入的Service：
   - `WebSocketRetryService` (来自Message Service)
   - `ChangeRecordService` (来自Commodity Service)
   - `UserProfileService` (来自Auth Service)

3. 添加Feign Client注入：
```java
private final AuthClient authClient;
private final CommodityClient commodityClient;
private final CommodityQueryClient commodityQueryClient;
private final ChangeRecordClient changeRecordClient;
```

4. 替换所有跨服务调用：

**商品查询改造**：
```java
// ❌ 旧代码
Optional<Commodity> commodityOpt = commodityRepository.findByIdForUpdate(orderDTO.getCommodityId());

// ✅ 新代码
Result<Commodity> commodityResult = commodityClient.getCommodityForUpdate(orderDTO.getCommodityId());
if (!commodityResult.getSuccess() || commodityResult.getData() == null) {
    throw new BusinessException("商品不存在");
}
Commodity commodity = commodityResult.getData();
```

**库存更新改造**：
```java
// ❌ 旧代码
int updateResult = commodityRepository.updateStockWithCondition(
    orderDTO.getCommodityId(), 
    orderDTO.getQuantity()
);

// ✅ 新代码
Result updateResult = commodityClient.updateCommodityStock(
    orderDTO.getCommodityId(), 
    orderDTO.getQuantity()
);
if (!updateResult.getSuccess()) {
    throw new BusinessException("商品库存不足，请刷新后重试");
}
```

#### 2.3 改造ChatDataController (Order Service)

**需要改造的地方**：
1. 移除直接注入的Service：
   - `CommodityQueryService` (来自Commodity Service)
   - `ChangeRecordService` (来自Commodity Service)

2. 添加Feign Client注入：
```java
private final CommodityQueryClient commodityQueryClient;
private final ChangeRecordClient changeRecordClient;
```

3. 替换Service调用：
```java
// ❌ 旧代码
Result commodityResult = commodityQueryService.getCommoditiesBatchStatus(
    new ArrayList<>(commodityIds)
);

// ✅ 新代码
Result commodityResult = commodityQueryClient.getCommoditiesBatchStatus(
    new ArrayList<>(commodityIds)
);
```

### 阶段3：处理依赖问题

#### 3.1 移除错误的Repository导入

在改造后的Service中，需要删除以下导入：
```java
// ❌ 删除这些导入
import com.njumarket.message.repository.UserRepository;
import com.njumarket.message.repository.UserProfileRepository;
import com.njumarket.message.repository.OrderRepository;
import com.njumarket.message.repository.CommodityRepository;
```

#### 3.2 添加Feign Client导入

```java
// ✅ 添加这些导入
import com.njumarket.message.client.AuthClient;
import com.njumarket.message.client.CommodityClient;
import com.njumarket.message.client.OrderClient;
```

### 阶段4：处理异常情况

#### 4.1 Feign Client调用失败处理

```java
try {
    Result<User> userResult = authClient.getUserById(userId);
    if (!userResult.getSuccess()) {
        log.error("调用Auth Service失败: {}", userResult.getMessage());
        throw new BusinessException("查询用户信息失败");
    }
    User user = userResult.getData();
} catch (Exception e) {
    log.error("Feign Client调用异常: {}", e.getMessage(), e);
    throw new BusinessException("服务调用失败，请稍后重试");
}
```

#### 4.2 配置Feign超时和重试

在各服务的 `application.yml` 中添加：

```yaml
feign:
  client:
    config:
      default:
        connectTimeout: 5000
        readTimeout: 10000
      njumarket-service-auth:
        connectTimeout: 3000
        readTimeout: 5000
      njumarket-service-commodity:
        connectTimeout: 3000
        readTimeout: 5000
      njumarket-service-order:
        connectTimeout: 3000
        readTimeout: 5000
      njumarket-service-message:
        connectTimeout: 3000
        readTimeout: 5000
  hystrix:
    enabled: false  # 如果不需要熔断，可以禁用
```

## 🚀 改造步骤

### 步骤1：添加内部API接口

1. 在Auth Service中添加内部接口
2. 在Commodity Service中添加内部接口
3. 测试接口是否正常工作

### 步骤2：改造Message Service

1. 修改 `ContactServiceImpl`，替换Repository注入为Feign Client
2. 更新所有跨服务调用
3. 测试功能是否正常

### 步骤3：改造Order Service

1. 修改 `OrderServiceImpl`，替换Repository注入为Feign Client
2. 修改 `ChatDataController`，替换Service注入为Feign Client
3. 更新所有跨服务调用
4. 测试功能是否正常

### 步骤4：改造Auth Service（可选）

如果Admin功能需要跨服务调用，也需要改造 `AdminServiceImpl`。

### 步骤5：测试和验证

1. 启动所有服务
2. 测试各个功能点
3. 检查日志，确认Feign Client调用正常
4. 验证数据一致性

## 📝 注意事项

### 1. 性能考虑

- Feign Client调用会增加网络延迟
- 尽量使用批量接口减少调用次数
- 对频繁查询的数据进行缓存

### 2. 事务处理

- 跨服务调用无法使用本地事务
- 需要实现分布式事务或最终一致性
- 关键操作使用Redis分布式锁保护

### 3. 错误处理

- Feign Client调用可能失败
- 需要实现重试机制
- 考虑服务降级和熔断

### 4. 数据一致性

- 跨服务操作无法保证强一致性
- 采用最终一致性策略
- 使用消息队列实现异步通知

## 🔍 验证方法

### 1. 检查服务注册

访问 http://localhost:8761，确认所有服务已注册。

### 2. 测试Feign Client调用

在日志中查看Feign Client的调用记录，确认调用成功。

### 3. 功能测试

测试各个业务功能，确认跨服务调用正常工作。

## 📚 相关文档

- [微服务配置指南](./MICROSERVICES_SETUP_GUIDE.md)
- [微服务架构文档](./MICROSERVICES_ARCHITECTURE.md)

---

**最后更新**: 2025-01-20
**状态**: 进行中


## 📋 概述

本文档说明如何将项目中的直接Repository注入改造为Feign Client调用，实现真正的微服务架构。

## ✅ 已完成工作

### 1. Feign Client接口已创建

#### Message Service的Feign Client
- ✅ `AuthClient` - 调用Auth Service
- ✅ `CommodityClient` - 调用Commodity Service  
- ✅ `OrderClient` - 调用Order Service

#### Order Service的Feign Client
- ✅ `AuthClient` - 调用Auth Service
- ✅ `CommodityClient` - 调用Commodity Service
- ✅ `CommodityQueryClient` - 调用Commodity Service查询功能
- ✅ `ChangeRecordClient` - 调用Commodity Service变更记录功能

### 2. 一键启动脚本已创建
- ✅ `start-all-services.bat` (Windows)
- ✅ `start-all-services.sh` (Linux/Mac)

## 🔧 待完成工作

### 阶段1：添加内部API接口

由于Feign Client需要调用其他服务的API，需要在各服务中添加内部接口。

#### 1.1 Auth Service需要添加的接口

在 `UserAuthController` 或新建 `InternalController` 中添加：

```java
@RestController
@RequestMapping("/api/internal")
public class InternalController {
    
    private final UserService userService;
    private final UserProfileService userProfileService;
    
    /**
     * 根据ID查询用户（内部接口）
     */
    @GetMapping("/user/{userId}")
    public Result getUserById(@PathVariable String userId) {
        // 实现逻辑
    }
    
    /**
     * 批量查询用户（内部接口）
     */
    @GetMapping("/user/batch")
    public Result getUsersByIds(@RequestParam List<String> userIds) {
        // 实现逻辑
    }
    
    /**
     * 批量查询用户档案（内部接口）
     */
    @GetMapping("/user/profile/batch")
    public Result getUserProfilesByIds(@RequestParam List<String> userIds) {
        // 实现逻辑
    }
}
```

#### 1.2 Commodity Service需要添加的接口

在 `UserCommodityController` 中添加：

```java
/**
 * 查询商品（带悲观锁，用于创建订单）
 */
@GetMapping("/{commodityId}/for-update")
public Result getCommodityForUpdate(@PathVariable String commodityId) {
    // 使用findByIdForUpdate查询
}

/**
 * 更新商品库存
 */
@PostMapping("/{commodityId}/update-stock")
public Result updateCommodityStock(@PathVariable String commodityId, 
                                  @RequestParam Integer quantity) {
    // 实现库存更新逻辑
}
```

在 `PublicController` 或新建 `InternalController` 中添加：

```java
@GetMapping("/change-record/commodity")
public Result<List<String>> getCommodityChangesAfter(@RequestParam LocalDateTime timestamp) {
    // 调用ChangeRecordService
}

@GetMapping("/change-record/order")
public Result<List<String>> getOrderChangesAfter(@RequestParam LocalDateTime timestamp) {
    // 调用ChangeRecordService
}
```

### 阶段2：改造Service实现类

#### 2.1 改造ContactServiceImpl (Message Service)

**需要改造的地方**：
1. 移除直接注入的Repository：
   - `UserRepository`
   - `UserProfileRepository`
   - `OrderRepository`
   - `CommodityRepository`

2. 添加Feign Client注入：
```java
private final AuthClient authClient;
private final CommodityClient commodityClient;
private final OrderClient orderClient;
```

3. 替换所有Repository调用为Feign Client调用：

**示例改造**：
```java
// ❌ 旧代码
Optional<User> receiverOpt = userRepository.findById(request.getReceiverId());

// ✅ 新代码
Result<User> userResult = authClient.getUserById(request.getReceiverId());
if (!userResult.getSuccess() || userResult.getData() == null) {
    throw new BusinessException("接收者不存在");
}
User receiver = userResult.getData();
```

**批量查询改造**：
```java
// ❌ 旧代码
List<UserProfile> profiles = userProfileRepository.findByUserIdIn(new ArrayList<>(userIds));

// ✅ 新代码
Result<List<UserProfile>> profileResult = authClient.getUserProfilesByIds(userIds);
if (!profileResult.getSuccess() || profileResult.getData() == null) {
    throw new BusinessException("查询用户档案失败");
}
List<UserProfile> profiles = profileResult.getData();
```

#### 2.2 改造OrderServiceImpl (Order Service)

**需要改造的地方**：
1. 移除直接注入的Repository：
   - `CommodityRepository`
   - `UserRepository`
   - `UserProfileRepository`

2. 移除直接注入的Service：
   - `WebSocketRetryService` (来自Message Service)
   - `ChangeRecordService` (来自Commodity Service)
   - `UserProfileService` (来自Auth Service)

3. 添加Feign Client注入：
```java
private final AuthClient authClient;
private final CommodityClient commodityClient;
private final CommodityQueryClient commodityQueryClient;
private final ChangeRecordClient changeRecordClient;
```

4. 替换所有跨服务调用：

**商品查询改造**：
```java
// ❌ 旧代码
Optional<Commodity> commodityOpt = commodityRepository.findByIdForUpdate(orderDTO.getCommodityId());

// ✅ 新代码
Result<Commodity> commodityResult = commodityClient.getCommodityForUpdate(orderDTO.getCommodityId());
if (!commodityResult.getSuccess() || commodityResult.getData() == null) {
    throw new BusinessException("商品不存在");
}
Commodity commodity = commodityResult.getData();
```

**库存更新改造**：
```java
// ❌ 旧代码
int updateResult = commodityRepository.updateStockWithCondition(
    orderDTO.getCommodityId(), 
    orderDTO.getQuantity()
);

// ✅ 新代码
Result updateResult = commodityClient.updateCommodityStock(
    orderDTO.getCommodityId(), 
    orderDTO.getQuantity()
);
if (!updateResult.getSuccess()) {
    throw new BusinessException("商品库存不足，请刷新后重试");
}
```

#### 2.3 改造ChatDataController (Order Service)

**需要改造的地方**：
1. 移除直接注入的Service：
   - `CommodityQueryService` (来自Commodity Service)
   - `ChangeRecordService` (来自Commodity Service)

2. 添加Feign Client注入：
```java
private final CommodityQueryClient commodityQueryClient;
private final ChangeRecordClient changeRecordClient;
```

3. 替换Service调用：
```java
// ❌ 旧代码
Result commodityResult = commodityQueryService.getCommoditiesBatchStatus(
    new ArrayList<>(commodityIds)
);

// ✅ 新代码
Result commodityResult = commodityQueryClient.getCommoditiesBatchStatus(
    new ArrayList<>(commodityIds)
);
```

### 阶段3：处理依赖问题

#### 3.1 移除错误的Repository导入

在改造后的Service中，需要删除以下导入：
```java
// ❌ 删除这些导入
import com.njumarket.message.repository.UserRepository;
import com.njumarket.message.repository.UserProfileRepository;
import com.njumarket.message.repository.OrderRepository;
import com.njumarket.message.repository.CommodityRepository;
```

#### 3.2 添加Feign Client导入

```java
// ✅ 添加这些导入
import com.njumarket.message.client.AuthClient;
import com.njumarket.message.client.CommodityClient;
import com.njumarket.message.client.OrderClient;
```

### 阶段4：处理异常情况

#### 4.1 Feign Client调用失败处理

```java
try {
    Result<User> userResult = authClient.getUserById(userId);
    if (!userResult.getSuccess()) {
        log.error("调用Auth Service失败: {}", userResult.getMessage());
        throw new BusinessException("查询用户信息失败");
    }
    User user = userResult.getData();
} catch (Exception e) {
    log.error("Feign Client调用异常: {}", e.getMessage(), e);
    throw new BusinessException("服务调用失败，请稍后重试");
}
```

#### 4.2 配置Feign超时和重试

在各服务的 `application.yml` 中添加：

```yaml
feign:
  client:
    config:
      default:
        connectTimeout: 5000
        readTimeout: 10000
      njumarket-service-auth:
        connectTimeout: 3000
        readTimeout: 5000
      njumarket-service-commodity:
        connectTimeout: 3000
        readTimeout: 5000
      njumarket-service-order:
        connectTimeout: 3000
        readTimeout: 5000
      njumarket-service-message:
        connectTimeout: 3000
        readTimeout: 5000
  hystrix:
    enabled: false  # 如果不需要熔断，可以禁用
```

## 🚀 改造步骤

### 步骤1：添加内部API接口

1. 在Auth Service中添加内部接口
2. 在Commodity Service中添加内部接口
3. 测试接口是否正常工作

### 步骤2：改造Message Service

1. 修改 `ContactServiceImpl`，替换Repository注入为Feign Client
2. 更新所有跨服务调用
3. 测试功能是否正常

### 步骤3：改造Order Service

1. 修改 `OrderServiceImpl`，替换Repository注入为Feign Client
2. 修改 `ChatDataController`，替换Service注入为Feign Client
3. 更新所有跨服务调用
4. 测试功能是否正常

### 步骤4：改造Auth Service（可选）

如果Admin功能需要跨服务调用，也需要改造 `AdminServiceImpl`。

### 步骤5：测试和验证

1. 启动所有服务
2. 测试各个功能点
3. 检查日志，确认Feign Client调用正常
4. 验证数据一致性

## 📝 注意事项

### 1. 性能考虑

- Feign Client调用会增加网络延迟
- 尽量使用批量接口减少调用次数
- 对频繁查询的数据进行缓存

### 2. 事务处理

- 跨服务调用无法使用本地事务
- 需要实现分布式事务或最终一致性
- 关键操作使用Redis分布式锁保护

### 3. 错误处理

- Feign Client调用可能失败
- 需要实现重试机制
- 考虑服务降级和熔断

### 4. 数据一致性

- 跨服务操作无法保证强一致性
- 采用最终一致性策略
- 使用消息队列实现异步通知

## 🔍 验证方法

### 1. 检查服务注册

访问 http://localhost:8761，确认所有服务已注册。

### 2. 测试Feign Client调用

在日志中查看Feign Client的调用记录，确认调用成功。

### 3. 功能测试

测试各个业务功能，确认跨服务调用正常工作。

## 📚 相关文档

- [微服务配置指南](./MICROSERVICES_SETUP_GUIDE.md)
- [微服务架构文档](./MICROSERVICES_ARCHITECTURE.md)

---

**最后更新**: 2025-01-20
**状态**: 进行中

