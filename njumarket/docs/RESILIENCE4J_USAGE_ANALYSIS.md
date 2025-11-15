# Resilience4j 使用场景分析

## 概述

本文档分析了项目中所有值得使用 Resilience4j 熔断降级的 Feign Client 调用场景。

---

## 已实现 Resilience4j 的服务

### ✅ Order Service (v2.1.1)

**已配置的 Feign Client**：
1. `CommodityClient` - 商品服务（库存更新、商品查询）
2. `CommodityQueryClient` - 商品查询服务（批量查询）
3. `AuthClient` - 认证服务（用户信息查询）
4. `NotificationClient` - 通知服务（订单变更推送）
5. `ImageClient` - 图片服务（图片上传/删除）

**使用场景**：
- 创建订单时查询商品（关键路径）
- 更新商品库存（关键路径）
- 查询用户信息（关键路径）
- 推送订单变更（非关键路径，已降级）

---

## 建议添加 Resilience4j 的服务

### 📋 Message Service（推荐）

**需要保护的 Feign Client**：

#### 1. CommodityClient ⭐⭐⭐
- **用途**：发送商品卡片时查询商品详情
- **调用位置**：`ContactServiceImpl.sendMessage()` - 发送商品卡片
- **重要性**：⭐⭐（非关键路径）
- **降级策略**：返回错误，阻止发送商品卡片
- **理由**：
  - 商品服务不可用时，用户无法发送商品卡片
  - 但不会影响普通消息发送
  - 可以降级为返回"商品服务暂时不可用"

#### 2. OrderClient ⭐⭐⭐
- **用途**：发送订单卡片时查询订单详情
- **调用位置**：`ContactServiceImpl.sendMessage()` - 发送订单卡片
- **重要性**：⭐⭐（非关键路径）
- **降级策略**：返回错误，阻止发送订单卡片
- **理由**：
  - 订单服务不可用时，用户无法发送订单卡片
  - 但不会影响普通消息发送
  - 可以降级为返回"订单服务暂时不可用"

#### 3. AuthClient ⭐⭐
- **用途**：验证接收者是否存在
- **调用位置**：`ContactServiceImpl.sendMessage()` - 验证接收者
- **重要性**：⭐⭐⭐（关键路径）
- **降级策略**：返回错误，阻止发送消息
- **理由**：
  - 认证服务不可用时，无法验证接收者
  - 但这是关键验证，不能跳过
  - 可以降级为返回"认证服务暂时不可用，无法验证接收者"

**优先级**：⭐⭐⭐（高）
- 商品和订单查询是非关键路径，可以降级
- 认证验证是关键路径，但可以返回明确的错误信息

---

### 📋 Commodity Service（可选）

**需要保护的 Feign Client**：

#### 1. NotificationClient ⭐⭐
- **用途**：推送商品变更通知
- **调用位置**：商品更新、上架、下架、重新发布时
- **重要性**：⭐（非关键路径）
- **降级策略**：静默失败，不影响商品操作
- **理由**：
  - 通知失败不应影响商品操作
  - 可以降级为返回成功（静默失败）

#### 2. ImageClient ⭐⭐
- **用途**：上传商品图片
- **调用位置**：商品图片上传
- **重要性**：⭐⭐⭐（关键路径）
- **降级策略**：返回错误，阻止图片上传
- **理由**：
  - 图片上传失败会影响商品发布
  - 可以降级为返回"图片服务暂时不可用"

#### 3. OrderClient ⭐
- **用途**：检查商品是否有订单
- **调用位置**：删除商品前检查
- **重要性**：⭐⭐（关键路径）
- **降级策略**：返回错误，阻止删除
- **理由**：
  - 无法检查订单时，不应允许删除商品
  - 可以降级为返回"订单服务暂时不可用，无法删除商品"

**优先级**：⭐⭐（中）
- 通知服务可以静默失败
- 图片和订单服务是关键路径，但使用频率较低

---

### 📋 Notification Service（可选）

**需要保护的 Feign Client**：

#### 1. CommodityQueryClient ⭐
- **用途**：批量查询商品状态（增量轮询）
- **调用位置**：`ChatDataController.getIncrementalUpdate()`
- **重要性**：⭐（非关键路径）
- **降级策略**：返回空列表，不影响轮询
- **理由**：
  - 商品查询失败时，可以返回空列表
  - 不影响订单查询和其他功能
  - 可以降级为返回空列表

#### 2. OrderQueryClient ⭐
- **用途**：批量查询订单状态（增量轮询）
- **调用位置**：`ChatDataController.getIncrementalUpdate()`
- **重要性**：⭐（非关键路径）
- **降级策略**：返回空列表，不影响轮询
- **理由**：
  - 订单查询失败时，可以返回空列表
  - 不影响商品查询和其他功能
  - 可以降级为返回空列表

**优先级**：⭐（低）
- 增量轮询失败不影响核心功能
- 可以降级为返回空列表，用户体验影响较小

---

## 推荐实施顺序

### ✅ 第一阶段：Message Service（已完成）

**实施内容**：
- ✅ 添加 Resilience4j 依赖
- ✅ 为 `CommodityClient` 创建 Fallback
- ✅ 为 `OrderClient` 创建 Fallback
- ✅ 配置熔断器参数

### ✅ 第二阶段：Commodity Service（已完成）

**实施内容**：
- ✅ 添加 Resilience4j 依赖（已有，补充 circuitbreaker 依赖）
- ✅ 为 `NotificationClient` 创建 Fallback（静默失败）
- ✅ 为 `ImageClient` 创建 Fallback
- ✅ 为 `OrderClient` 创建 Fallback

### ✅ 第三阶段：Notification Service（已完成）

**实施内容**：
- ✅ 添加 Resilience4j 依赖
- ✅ 为 `CommodityQueryClient` 创建 Fallback（返回空列表）
- ✅ 为 `OrderQueryClient` 创建 Fallback（返回空列表）

---

## 降级策略设计

### Message Service

#### CommodityClient Fallback
```java
@Override
public Result getCommodityById(String commodityId) {
    log.warn("商品服务不可用，触发熔断降级: commodityId={}", commodityId);
    return Result.fail("商品服务暂时不可用，无法发送商品卡片，请稍后重试");
}
```

#### OrderClient Fallback
```java
@Override
public Result getOrderById(String orderId) {
    log.warn("订单服务不可用，触发熔断降级: orderId={}", orderId);
    return Result.fail("订单服务暂时不可用，无法发送订单卡片，请稍后重试");
}
```

#### AuthClient Fallback
```java
@Override
public Result getUserById(String userId) {
    log.warn("认证服务不可用，触发熔断降级: userId={}", userId);
    return Result.fail("认证服务暂时不可用，无法验证接收者，请稍后重试");
}
```

### Commodity Service

#### NotificationClient Fallback
```java
@Override
public Result pushCommodityChange(String userId, String commodityId, String operation) {
    log.warn("通知服务不可用，触发熔断降级: userId={}, commodityId={}, operation={}", 
        userId, commodityId, operation);
    return Result.ok("通知服务暂时不可用，商品变更推送失败"); // 静默失败
}
```

### Notification Service

#### CommodityQueryClient Fallback
```java
@Override
public Result getCommoditiesBatchStatus(List<String> commodityIds) {
    log.warn("商品查询服务不可用，触发熔断降级: commodityIds={}", commodityIds);
    return Result.ok("商品查询服务暂时不可用", Collections.emptyList()); // 返回空列表
}
```

---

## 总结

### 当前状态
- ✅ Order Service：已完整实现 Resilience4j（5个 Feign Client）
- ✅ Message Service：已完整实现 Resilience4j（2个 Feign Client）
- ✅ Commodity Service：已完整实现 Resilience4j（3个 Feign Client）
- ✅ Notification Service：已完整实现 Resilience4j（2个 Feign Client）

### 实施状态
- ✅ **已完成**：所有服务的 Resilience4j 集成已完成

### 实施原则
1. **关键路径**：返回明确的错误信息，阻止操作
2. **非关键路径**：可以静默失败或返回空结果
3. **用户体验**：失败时应该有明确的提示信息

