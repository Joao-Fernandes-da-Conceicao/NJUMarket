# Service方法异常处理统一指南

## 统一模式

### 1. 方法开始日志
```java
log.info("操作名称开始 - 关键参数1={}, 关键参数2={}", param1, param2);
```

### 2. 业务验证失败（抛出BusinessException）
```java
if (condition) {
    log.warn("操作名称失败（业务异常） - 关键信息={}", info);
    throw new BusinessException("用户友好的错误信息");
}
```

### 3. 操作成功日志
```java
log.info("操作名称成功 - 关键结果={}", result);
return Result.ok("操作成功");
```

### 4. 异常处理
```java
try {
    // 业务逻辑
} catch (BusinessException e) {
    throw e; // 业务异常直接重新抛出
} catch (Exception e) {
    log.error("操作名称失败（系统异常） - 关键信息={}, error={}", info, e.getMessage(), e);
    throw new BusinessException("操作失败，请稍后重试", e);
}
```

## 替换规则

### 规则1：用户未登录检查
**替换前：**
```java
if (currentUser == null) {
    return Result.fail("用户未登录");
}
```

**替换后：**
```java
if (currentUser == null) {
    log.warn("操作名称失败（业务异常） - 用户未登录");
    throw new BusinessException("用户未登录");
}
```

### 规则2：资源不存在检查
**替换前：**
```java
if (resourceOpt.isEmpty()) {
    return Result.fail("资源不存在");
}
```

**替换后：**
```java
if (resourceOpt.isEmpty()) {
    log.warn("操作名称失败（业务异常） - resourceId={}, 资源不存在", resourceId);
    throw new BusinessException("资源不存在");
}
```

### 规则3：权限检查
**替换前：**
```java
if (!hasPermission) {
    return Result.fail("无权限操作");
}
```

**替换后：**
```java
if (!hasPermission) {
    log.warn("操作名称失败（业务异常） - userId={}, resourceId={}, 无权限操作", userId, resourceId);
    throw new BusinessException("无权限操作");
}
```

### 规则4：状态检查
**替换前：**
```java
if (!"VALID_STATUS".equals(status)) {
    return Result.fail("状态异常");
}
```

**替换后：**
```java
if (!"VALID_STATUS".equals(status)) {
    log.warn("操作名称失败（业务异常） - resourceId={}, status={}, 状态异常", resourceId, status);
    throw new BusinessException("状态异常");
}
```

### 规则5：try-catch异常处理
**替换前：**
```java
try {
    // 业务逻辑
    return Result.ok("成功");
} catch (Exception e) {
    log.error("操作失败", e);
    return Result.fail("操作失败：" + e.getMessage());
}
```

**替换后：**
```java
try {
    // 业务逻辑
    log.info("操作名称成功 - 关键结果={}", result);
    return Result.ok("成功");
} catch (BusinessException e) {
    throw e;
} catch (Exception e) {
    log.error("操作名称失败（系统异常） - 关键信息={}, error={}", info, e.getMessage(), e);
    throw new BusinessException("操作失败，请稍后重试", e);
}
```

## 注意事项

1. **确保导入BusinessException**：
   ```java
   import com.njumarket.njumarket.exception.BusinessException;
   ```

2. **日志格式统一**：
   - 方法开始：`log.info("操作名称开始 - 参数={}", param)`
   - 业务异常：`log.warn("操作名称失败（业务异常） - 信息={}", info)`
   - 操作成功：`log.info("操作名称成功 - 结果={}", result)`
   - 系统异常：`log.error("操作名称失败（系统异常） - 信息={}, error={}", info, e.getMessage(), e)`

3. **异常处理顺序**：
   - 先catch BusinessException，直接重新抛出
   - 再catch Exception，包装为BusinessException抛出

4. **保持原有业务逻辑**：
   - 只改变异常处理方式，不改变业务逻辑
   - 保持原有的验证顺序和逻辑

