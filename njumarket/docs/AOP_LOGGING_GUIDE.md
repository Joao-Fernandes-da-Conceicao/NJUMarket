# AOP日志切面使用说明

## 概述

使用AOP（面向切面编程）统一记录Service层方法的执行日志，减少重复代码，提高代码可维护性。

## 实现方式

### 1. 添加依赖

已在`pom.xml`中添加了`spring-boot-starter-aop`依赖：

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-aop</artifactId>
</dependency>
```

### 2. 切面类

创建了`ServiceLogAspect`切面类，位于：
- `njumarket/src/main/java/com/njumarket/njumarket/aspect/ServiceLogAspect.java`

### 3. 切点定义

切面拦截所有Service实现类的public方法：
```java
@Pointcut("execution(public * com.njumarket.njumarket.service.impl.*.*(..))")
public void serviceMethod() {
}
```

## 功能特性

### 1. 自动日志记录

切面会自动记录：
- **方法开始**：记录方法名、类名、参数（INFO级别）
- **方法成功**：记录方法名、类名、返回值、执行时间（INFO级别）
- **业务异常**：记录方法名、类名、异常信息、执行时间（WARN级别）
- **系统异常**：记录方法名、类名、异常信息、执行时间、堆栈（ERROR级别）

### 2. 智能方法名识别

切面会根据方法名自动推断中文操作名称：
- `createOrder` → "创建订单"
- `payOrder` → "支付订单"
- `sendMessage` → "发送消息"
- `getConversations` → "获取对话列表"
- 等等...

### 3. 参数简化显示

为了避免日志过长，切面会简化参数显示：
- DTO对象：只显示类名，如`OrderDTO(...)`
- 简单类型：直接显示值，如`"orderId123"`
- 复杂对象：只显示类名

### 4. 返回值格式化

- 成功时：显示"成功"和数据类型
- 分页对象：显示总数
- 失败时：显示错误信息

### 5. 执行时间统计

自动记录每个方法的执行时间，便于性能分析。

## 使用示例

### 使用前（需要手动写日志）

```java
@Override
public Result createOrder(OrderDTO orderDTO) {
    log.info("创建订单开始 - commodityId={}, quantity={}, payAmount={}", 
             orderDTO.getCommodityId(), orderDTO.getQuantity(), orderDTO.getPayAmount());
    
    try {
        // 业务逻辑...
        
        log.info("创建订单成功 - orderId={}, buyerId={}, sellerId={}", 
                 order.getOrderId(), order.getBuyerId(), order.getSellerId());
        return Result.ok("订单创建成功");
    } catch (BusinessException e) {
        throw e;
    } catch (Exception e) {
        log.error("创建订单失败（系统异常） - commodityId={}, error={}", 
                  orderDTO.getCommodityId(), e.getMessage(), e);
        throw new BusinessException("创建订单失败，请稍后重试", e);
    }
}
```

### 使用后（AOP自动记录）

```java
@Override
public Result createOrder(OrderDTO orderDTO) {
    // 业务逻辑...
    // AOP会自动记录日志，无需手动写log语句
    
    return Result.ok("订单创建成功");
}
```

## 日志输出示例

### 正常执行
```
INFO  - 创建订单开始 - className=OrderServiceImpl, method=createOrder, params=OrderDTO(...)
INFO  - 创建订单成功 - className=OrderServiceImpl, method=createOrder, result=成功, executionTime=125ms
```

### 业务异常
```
INFO  - 创建订单开始 - className=OrderServiceImpl, method=createOrder, params=OrderDTO(...)
WARN  - 创建订单失败（业务异常） - className=OrderServiceImpl, method=createOrder, error=商品不存在, executionTime=15ms
```

### 系统异常
```
INFO  - 创建订单开始 - className=OrderServiceImpl, method=createOrder, params=OrderDTO(...)
ERROR - 创建订单失败（系统异常） - className=OrderServiceImpl, method=createOrder, error=数据库连接失败, executionTime=5000ms
```

## 注意事项

### 1. 业务异常处理

切面会自动识别`BusinessException`，记录为WARN级别，并直接抛出（不包装）。异常处理逻辑仍由Service方法或GlobalExceptionHandler负责。

### 2. 系统异常处理

切面会将系统异常记录为ERROR级别，并直接抛出（不包装），由Service方法或GlobalExceptionHandler处理。这样可以保持异常处理的一致性。

### 3. 性能考虑

- 切面使用反射获取方法信息，性能开销很小
- 参数格式化会简化显示，避免日志过长
- 执行时间统计使用`System.currentTimeMillis()`，开销可忽略

### 4. 兼容性

- 如果Service方法中已有手动日志，AOP日志会额外记录（不会冲突）
- 建议移除Service方法中的手动日志，统一使用AOP记录

## 后续优化建议

1. **自定义注解**：可以创建`@ServiceLog`注解，允许方法级别的日志控制
2. **日志级别配置**：可以通过配置文件控制日志级别
3. **敏感信息过滤**：可以添加敏感字段过滤功能（如密码、token等）
4. **异步日志**：对于高频方法，可以考虑异步记录日志

## 迁移建议

1. **逐步迁移**：可以先保留手动日志，观察AOP日志效果
2. **移除重复日志**：确认AOP日志正常后，移除Service方法中的手动日志
3. **保留关键日志**：对于特别重要的业务节点，可以保留手动日志作为补充

