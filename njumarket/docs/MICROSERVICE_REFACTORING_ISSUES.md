# 微服务重构问题清单

## 一、已修复的问题

### ✅ 1. 创建缺失的内部DTO
- ✅ `CommodityInternalDTO` - 商品内部传输对象
- ✅ `OrderInternalDTO` - 订单内部传输对象
- ✅ `InternalDTOConverter` - 添加了Commodity和Order的转换方法

### ✅ 2. 修复commodity-service的InternalController
- ✅ `getCommodityForUpdate()` 现在返回 `CommodityInternalDTO` 而非 `Commodity` 实体

### ✅ 3. 修复order-service的InternalController
- ✅ `getOrderById()` 现在返回 `OrderInternalDTO` 而非 `Order` 实体

## 二、待修复的问题

### 🔴 高优先级

#### 1. Feign Client返回类型问题
**问题**：所有Feign Client返回 `Result`（非泛型），导致类型信息丢失，调用方需要手动转换。

**影响范围**：
- `AuthClient.getUserById()` - 返回 `Result`，实际应该是 `Result<UserInternalDTO>`
- `CommodityClient.getCommodityForUpdate()` - 返回 `Result`，实际应该是 `Result<CommodityInternalDTO>`
- 所有其他Feign Client方法

**解决方案**：
由于 `Result` 类不是泛型的，需要：
1. 在调用方使用 `ObjectMapper` 或手动转换
2. 或者修改 `Result` 类为泛型（影响范围大，需谨慎）

**当前状态**：调用方已经在使用 `ObjectMapper` 或手动转换，但不够规范。

#### 2. order-service缺少InternalController
**问题**：order-service没有InternalController，其他服务无法通过Feign Client调用order-service的内部接口。

**需要添加的接口**：
- `GET /api/internal/order/{orderId}` - 查询订单详情（返回OrderInternalDTO）
- `GET /api/internal/order/check-commodity/{commodityId}` - 检查商品是否有订单
- 其他内部接口

#### 3. message-service的InternalController需要返回DTO
**问题**：message-service的InternalController可能直接返回Entity，需要检查并修复。

### 🟡 中优先级

#### 4. 统一Feign Client规范
**问题**：不同服务的Feign Client命名和路径不统一。

**规范要求**：
- 所有内部接口使用 `/api/internal` 路径
- Feign Client接口命名规范：`{ServiceName}Client`
- 方法命名规范：动词+名词（如 `getUserById`, `updateCommodityStock`）

#### 5. 错误处理统一
**问题**：Feign Client调用失败时的错误处理不统一。

**需要**：
- 统一的错误处理机制
- 重试机制配置
- 超时配置

### 🟢 低优先级

#### 6. 性能优化
**问题**：批量查询接口可能不够优化。

**建议**：
- 检查批量查询接口的实现
- 优化N+1查询问题

## 三、单体版功能对比

### 已迁移的功能
- ✅ 用户认证和授权
- ✅ 商品管理
- ✅ 订单管理
- ✅ 消息系统
- ✅ 管理员功能

### 可能丢失的功能
需要对比单体版，检查以下功能是否完整：
- [ ] WebSocket实时消息推送
- [ ] 订单状态变更通知
- [ ] 商品变更记录
- [ ] 用户活动记录
- [ ] 数据统计功能

## 四、修复计划

### 阶段1：修复关键问题（当前）
1. ✅ 创建缺失的内部DTO
2. ✅ 修复commodity-service的InternalController
3. ⏳ 创建order-service的InternalController
4. ⏳ 检查并修复message-service的InternalController

### 阶段2：规范Feign Client
1. 统一Feign Client命名和路径
2. 添加统一的错误处理
3. 配置超时和重试

### 阶段3：功能完整性检查
1. 对比单体版功能清单
2. 检查缺失功能
3. 补充缺失功能

## 五、最佳实践

### 1. 服务间通信原则
- ✅ 只传递DTO，不传递Entity
- ✅ 使用内部DTO（InternalDTO）用于服务间通信
- ✅ 使用Feign Client进行服务间调用
- ✅ 内部接口使用 `/api/internal` 路径

### 2. DTO设计原则
- ✅ 只包含必要字段
- ✅ 不包含关联对象
- ✅ 实现 `Serializable` 接口
- ✅ 使用明确的命名（InternalDTO后缀）

### 3. Feign Client设计原则
- ✅ 使用 `@FeignClient` 注解
- ✅ 指定服务名称和路径
- ✅ 使用标准的HTTP方法注解
- ⚠️ 返回类型使用 `Result`（当前非泛型，需手动转换）

## 六、注意事项

1. **Result类非泛型**：由于 `Result` 类不是泛型的，Feign Client返回时需要调用方手动转换类型。这是当前架构的限制。

2. **类型转换**：调用方需要使用 `ObjectMapper` 或手动转换，例如：
   ```java
   Result result = authClient.getUserById(userId);
   UserInternalDTO dto = objectMapper.convertValue(
       result.getData(),
       new TypeReference<UserInternalDTO>() {}
   );
   ```

3. **向后兼容**：修改 `Result` 类为泛型会影响所有现有代码，需要谨慎评估。

