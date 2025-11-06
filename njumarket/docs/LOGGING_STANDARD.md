# NJUMarket 日志记录规范

## 日志级别使用规范

### INFO - 正常业务流程
- 方法开始执行（包含关键参数）
- 业务操作成功（包含关键结果）
- 重要的状态变更

### WARN - 业务异常/警告
- 业务逻辑验证失败（用户输入错误、权限不足等）
- 可预期的异常情况（如库存不足、订单状态不正确）
- 资源获取失败但不影响主流程（如获取分布式锁失败）

### ERROR - 系统异常
- 未预期的系统异常
- 数据库操作失败
- 外部服务调用失败

### DEBUG - 调试信息
- 详细的执行流程
- 中间状态信息
- 性能相关的调试信息

## 日志格式规范

### 方法开始
```java
log.info("操作名称开始 - 关键参数1={}, 关键参数2={}", param1, param2);
```

### 业务操作成功
```java
log.info("操作名称成功 - 关键结果1={}, 关键结果2={}", result1, result2);
```

### 业务异常（抛出BusinessException前）
```java
log.warn("业务异常描述 - 关键信息1={}, 关键信息2={}", info1, info2);
throw new BusinessException("用户友好的错误信息");
```

### 系统异常（抛出异常前）
```java
log.error("操作名称失败（系统异常） - 关键信息={}, error={}", info, e.getMessage(), e);
throw new BusinessException("操作失败，请稍后重试", e);
```

## 示例

### 统一后的代码示例

```java
@Override
@Transactional
public Result createOrder(OrderDTO orderDTO) {
    log.info("创建订单开始 - commodityId={}, quantity={}, payAmount={}", 
             orderDTO.getCommodityId(), orderDTO.getQuantity(), orderDTO.getPayAmount());
    
    // 获取当前用户
    User currentUser = UserHolder.getUser();
    if (currentUser == null) {
        log.warn("创建订单失败（业务异常） - 用户未登录");
        throw new BusinessException("用户未登录");
    }
    
    // 业务逻辑验证
    Optional<Commodity> commodityOpt = commodityRepository.findByIdForUpdate(orderDTO.getCommodityId());
    if (commodityOpt.isEmpty()) {
        log.warn("创建订单失败（业务异常） - commodityId={}, 商品不存在", orderDTO.getCommodityId());
        throw new BusinessException("商品不存在");
    }
    
    Commodity commodity = commodityOpt.get();
    
    if (!"ON_SHELF".equals(commodity.getCommodityStatus())) {
        log.warn("创建订单失败（业务异常） - commodityId={}, status={}, 商品未上架", 
                 orderDTO.getCommodityId(), commodity.getCommodityStatus());
        throw new BusinessException("商品未上架，无法购买");
    }
    
    // 业务逻辑处理
    try {
        // ... 创建订单逻辑 ...
        
        log.info("创建订单成功 - orderId={}, buyerId={}, sellerId={}", 
                 order.getOrderId(), order.getBuyerId(), order.getSellerId());
        return Result.ok("订单创建成功");
        
    } catch (Exception e) {
        log.error("创建订单失败（系统异常） - commodityId={}, error={}", 
                  orderDTO.getCommodityId(), e.getMessage(), e);
        throw new BusinessException("创建订单失败，请稍后重试", e);
    }
}
```

## 注意事项

1. **不要捕获异常后返回Result.fail()**：应该抛出BusinessException，由GlobalExceptionHandler统一处理
2. **业务异常使用WARN级别**：这是可预期的异常，不是系统错误
3. **系统异常使用ERROR级别**：这是未预期的异常，需要记录详细堆栈
4. **日志要包含关键信息**：便于问题排查和追踪
5. **避免日志过多**：只在关键节点记录日志，避免过度日志

