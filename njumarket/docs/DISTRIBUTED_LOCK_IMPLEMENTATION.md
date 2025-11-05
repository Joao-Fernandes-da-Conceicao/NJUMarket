# 分布式锁实现文档

## 📋 概述

本文档说明为什么在多服务器场景下需要分布式锁，以及如何实现和使用分布式锁来解决库存超卖问题。

---

## 🤔 为什么需要分布式锁？

### 问题场景

**单服务器场景**（之前）：
```
┌─────────────┐
│ 服务器1     │
│ 应用 + DB   │
└─────────────┘
```
- 只需要数据库悲观锁（`SELECT ... FOR UPDATE`）
- 所有请求都在同一服务器，数据库锁足够

**多服务器场景**（现在）：
```
┌─────────────┐    ┌─────────────┐
│ 服务器1     │    │ 服务器2     │
│ 应用        │    │ 应用        │
└──────┬──────┘    └──────┬──────┘
       │                  │
       └────────┬─────────┘
                │
        ┌───────▼────────┐
        │  数据库/Redis   │
        └────────────────┘
```

**问题**：
- 数据库悲观锁只能锁定数据库连接，不能跨服务器
- 两个服务器可能同时通过库存检查，导致超卖

### 具体示例

**场景**：库存为1，两台服务器同时收到下单请求

**没有分布式锁时**：
```
时间线：
T1: 服务器1查询商品 → stock = 1 ✅
T2: 服务器2查询商品 → stock = 1 ✅（服务器1还没更新）
T3: 服务器1检查库存充足，创建订单，扣减库存 → stock = 0
T4: 服务器2检查库存充足，创建订单，扣减库存 → stock = -1 ❌ 超卖！
```

**有分布式锁后**：
```
时间线：
T1: 服务器1获取分布式锁（Redis）→ 🔒 锁定
T2: 服务器2尝试获取分布式锁 → ⏸️ 等待（被服务器1阻塞）
T3: 服务器1查询商品 → stock = 1 ✅
T4: 服务器1检查库存充足，创建订单，扣减库存 → stock = 0
T5: 服务器1释放分布式锁 → 🔓 解锁
T6: 服务器2获得分布式锁 → 🔒 锁定
T7: 服务器2查询商品 → stock = 0
T8: 服务器2检查库存不足 → ❌ 返回"库存不足"
T9: 服务器2释放分布式锁 → 🔓 解锁
```

---

## ✅ 解决方案：三重保护机制

### 1. 分布式锁（Redis）- 跨服务器保护

**作用**：防止多台服务器同时处理同一商品的订单

**实现**：
```java
// 获取分布式锁
String lockKey = "lock:commodity:" + commodityId;
String lockValue = RedisLockUtil.generateLockValue();
boolean lockAcquired = redisLockUtil.tryLock(lockKey, lockValue, 10, 1, 100);
```

### 2. 数据库悲观锁（SELECT ... FOR UPDATE）- 数据库层面保护

**作用**：防止同一数据库连接内的并发修改

**实现**：
```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("SELECT c FROM Commodity c WHERE c.commodityId = :commodityId")
Optional<Commodity> findByIdForUpdate(@Param("commodityId") String commodityId);
```

### 3. 数据库条件更新（WHERE stock >= quantity）- 最终保护

**作用**：即使前两层失效，数据库层面也能防止超卖

**实现**：
```java
@Modifying
@Query("UPDATE Commodity c SET c.stock = c.stock - :quantity WHERE c.commodityId = :commodityId AND c.stock >= :quantity")
int updateStockWithCondition(@Param("commodityId") String commodityId, @Param("quantity") Integer quantity);
```

---

## 🔧 实现细节

### RedisLockUtil 工具类

**核心方法**：

1. **tryLock()** - 加锁
   - 使用 `SET key value NX EX timeout` 实现原子性
   - 返回 true 表示加锁成功，false 表示失败

2. **releaseLock()** - 释放锁
   - 使用 Lua 脚本确保原子性
   - 只有当 value 匹配时才删除（防止误删其他线程的锁）

3. **renewLock()** - 续期锁
   - 延长锁的过期时间
   - 适用于业务执行时间较长的情况

### 使用示例

```java
@Transactional
public Result createOrder(OrderDTO orderDTO) {
    // 第一步：获取分布式锁
    String lockKey = RedisConstants.LOCK_COMMODITY_KEY + orderDTO.getCommodityId();
    String lockValue = RedisLockUtil.generateLockValue();
    boolean lockAcquired = false;
    
    try {
        // 尝试获取锁（最多等待1秒，重试间隔100ms）
        lockAcquired = redisLockUtil.tryLock(lockKey, lockValue, 10, 1, 100);
        
        if (!lockAcquired) {
            return Result.fail("系统繁忙，请稍后重试");
        }
        
        // 第二步：使用悲观锁查询（数据库层面保护）
        Optional<Commodity> commodityOpt = commodityRepository.findByIdForUpdate(
            orderDTO.getCommodityId()
        );
        
        // 第三步：使用条件更新（最终保护）
        int updateResult = commodityRepository.updateStockWithCondition(
            orderDTO.getCommodityId(), 
            orderDTO.getQuantity()
        );
        
        // ... 创建订单逻辑
        
    } finally {
        // 必须释放锁（在finally中确保一定释放）
        if (lockAcquired) {
            redisLockUtil.releaseLock(lockKey, lockValue);
        }
    }
}
```

---

## 📊 三重保护机制对比

| 保护层 | 作用范围 | 性能影响 | 可靠性 |
|--------|---------|---------|--------|
| **分布式锁** | 跨服务器 | 中等（Redis网络开销） | 高（Redis高可用） |
| **悲观锁** | 数据库连接 | 低（数据库行锁） | 高（数据库保证） |
| **条件更新** | SQL执行 | 极低（数据库判断） | 极高（数据库约束） |

**优势**：
- ✅ **冗余保护**：即使某一层失效，其他层仍能保护
- ✅ **适合多服务器**：分布式锁解决跨服务器问题
- ✅ **性能可控**：每层都有合理的超时和重试机制

---

## ⚠️ 注意事项

### 1. 锁的释放

**必须**在 `finally` 块中释放锁，确保即使发生异常也能释放：

```java
try {
    // 业务逻辑
} finally {
    if (lockAcquired) {
        redisLockUtil.releaseLock(lockKey, lockValue);
    }
}
```

### 2. 锁的超时时间

- **不能太长**：如果服务器崩溃，锁会长时间不释放
- **不能太短**：如果业务执行时间长，锁会提前过期

**建议**：根据业务执行时间设置，通常 10-30 秒

### 3. 锁的value

**必须**使用唯一值（线程ID + 时间戳），防止误删其他线程的锁：

```java
String lockValue = RedisLockUtil.generateLockValue();
// 生成：ThreadId_Timestamp
```

### 4. Redis可用性

如果 Redis 不可用，分布式锁会失败。建议：
- 使用 Redis 集群或哨兵模式
- 添加降级策略（直接使用数据库锁）

---

## 🎯 适用场景

### 需要分布式锁的场景

✅ **多服务器部署**：多台应用服务器共享同一数据库  
✅ **跨服务器并发控制**：需要跨服务器的互斥操作  
✅ **高并发场景**：需要更细粒度的并发控制  
✅ **缓存一致性**：需要保证缓存和数据库的一致性  

### 不需要分布式锁的场景

❌ **单服务器部署**：只有一台应用服务器  
❌ **低并发场景**：并发量很低，数据库锁足够  
❌ **最终一致性可接受**：允许短暂的不一致  

---

## 📚 参考资源

- [Redis分布式锁最佳实践](https://redis.io/docs/manual/patterns/distributed-locks/)
- [Redisson分布式锁实现](https://github.com/redisson/redisson/wiki/8.-Distributed-locks-and-synchronizers)
- [Spring Data Redis文档](https://docs.spring.io/spring-data/redis/docs/current/reference/html/)

---

**文档版本**：v1.0  
**最后更新**：2025-01-XX  
**维护者**：NJUMarket 开发团队
