# 南大集市 NJUMarket v1.2.0 项目文档

## 📋 目录
- [版本概述](#版本概述)
- [核心功能更新](#核心功能更新)
- [库存超卖防护机制](#库存超卖防护机制)
- [技术实现细节](#技术实现细节)
- [测试验证](#测试验证)
- [已知问题与限制](#已知问题与限制)
- [下一步规划](#下一步规划)

---

## 版本概述

### 版本信息
- **版本**: v1.2.0
- **发布时间**: 2025-11-05
- **基于版本**: v1.1.2
- **状态**: 已发布，库存超卖防护功能已完成

### 版本定位
v1.2.0 版本专注于**并发控制**和**数据一致性**保障，实现了完整的三重保护机制来防止库存超卖问题，确保在高并发场景下库存数据的准确性和一致性。通过Redis分布式锁、数据库悲观锁和条件更新的组合，为系统提供了强大的并发安全保障。

### 主要成就
- ✅ **三重保护机制**：Redis分布式锁 + 数据库悲观锁 + 条件更新
- ✅ **库存超卖防护**：100并发用户测试验证，无超卖现象
- ✅ **数据一致性保证**：库存和订单数据完全一致
- ✅ **完整测试工具**：JMeter压力测试配置和批量用户创建脚本
- ✅ **完善文档**：实现文档、测试文档、故障排查文档齐全

---

## 核心功能更新

### 1. 库存超卖防护系统

#### 1.1 问题背景

**库存超卖问题**：
- 商品库存只有1件，但2个用户同时下单，结果都成功了，库存变成了-1
- 在高并发场景下，库存检查和扣减不是原子操作，存在并发竞争风险

**原有实现的问题**：
```java
// 原有实现（存在并发问题）
if (commodity.getStock() < orderDTO.getQuantity()) {
    return Result.fail("商品库存不足");
}
// 此时其他线程可能已经扣减了库存
commodity.updateStock(-orderDTO.getQuantity());
commodityRepository.save(commodity);
```

#### 1.2 解决方案：三重保护机制

**实现位置**：
- 后端：`OrderServiceImpl.createOrder()` / `createOrderFromSnapshot()`
- 工具类：`RedisLockUtil.java`
- 数据访问：`CommodityRepository.java`

**三重保护架构**：
```
┌─────────────────────────────────┐
│  第1层：Redis分布式锁           │  ← 跨服务器保护
│  第2层：数据库悲观锁            │  ← 数据库层面保护
│  第3层：条件更新（SQL）         │  ← 最终保护
└─────────────────────────────────┘
```

---

## 库存超卖防护机制

### 2. 第一层：Redis分布式锁

#### 2.1 功能说明

**作用**：防止多台服务器同时处理同一商品的订单

**适用场景**：
- 多服务器部署环境
- 共享同一个数据库和Redis
- 需要跨服务器的并发控制

**实现原理**：
- 使用Redis的 `SET key value NX EX timeout` 实现原子性加锁
- 使用Lua脚本保证释放锁的原子性
- 支持锁的自动续期

#### 2.2 技术实现

**实现文件**：
- `RedisLockUtil.java` - 分布式锁工具类
- `lua/unlock.lua` - 释放锁的Lua脚本
- `lua/renew.lua` - 续期锁的Lua脚本

**核心代码**：
```java
// OrderServiceImpl.java - createOrder方法
String lockKey = RedisConstants.LOCK_COMMODITY_KEY + orderDTO.getCommodityId();
String lockValue = RedisLockUtil.generateLockValue();
long lockTimeout = RedisConstants.LOCK_COMMODITY_TTL;

boolean lockAcquired = false;
try {
    // 尝试获取分布式锁（最多等待1秒，重试间隔100ms）
    lockAcquired = redisLockUtil.tryLock(lockKey, lockValue, lockTimeout, 1, 100);
    
    if (!lockAcquired) {
        log.warn("获取分布式锁失败 - commodityId: {}", orderDTO.getCommodityId());
        return Result.fail("系统繁忙，请稍后重试");
    }
    
    // ... 订单创建逻辑 ...
    
} finally {
    if (lockAcquired) {
        boolean released = redisLockUtil.releaseLock(lockKey, lockValue);
        if (!released) {
            log.warn("释放分布式锁失败 - commodityId: {}", orderDTO.getCommodityId());
        }
    }
}
```

**关键特性**：
1. **原子性加锁**：使用 `SET NX EX` 保证原子性
2. **锁值验证**：使用唯一锁值防止误删其他线程的锁
3. **自动续期**：支持长时间任务的锁续期
4. **Lua脚本**：释放锁使用Lua脚本保证原子性

#### 2.3 为什么需要分布式锁？

**多服务器场景**：
```
服务器1 ──┐
          ├─→ 数据库（共享）
服务器2 ──┘

没有分布式锁的问题：
- 服务器1查询商品 → stock = 1 ✅
- 服务器2查询商品 → stock = 1 ✅（服务器1还没更新）
- 服务器1创建订单，扣减库存 → stock = 0
- 服务器2创建订单，扣减库存 → stock = -1 ❌ 超卖！

有了分布式锁：
- 服务器1获取锁，处理订单 → stock = 0
- 服务器2等待锁释放后，发现库存已为0 → 返回"库存不足" ✅
```

---

### 3. 第二层：数据库悲观锁

#### 3.1 功能说明

**作用**：在数据库层面锁定商品行，防止同一服务器内多个事务并发修改

**实现方式**：使用 `SELECT ... FOR UPDATE` 语句

#### 3.2 技术实现

**实现文件**：
- `CommodityRepository.java` - `findByIdForUpdate()` 方法

**核心代码**：
```java
// CommodityRepository.java
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("SELECT c FROM Commodity c WHERE c.commodityId = :commodityId")
Optional<Commodity> findByIdForUpdate(@Param("commodityId") String commodityId);
```

**使用方式**：
```java
// OrderServiceImpl.java
// ✅ 第二步：使用悲观锁查询商品（数据库层面保护）
Optional<Commodity> commodityOpt = commodityRepository.findByIdForUpdate(orderDTO.getCommodityId());
if (commodityOpt.isEmpty()) {
    return Result.fail("商品不存在");
}

Commodity commodity = commodityOpt.get();
// 此时商品行已被锁定，其他事务无法修改
```

**关键特性**：
1. **行级锁**：锁定特定商品行，不影响其他商品
2. **事务级别**：锁在事务提交或回滚时自动释放
3. **阻塞等待**：其他事务必须等待当前事务完成

---

### 4. 第三层：数据库条件更新

#### 4.1 功能说明

**作用**：数据库层面的最终保护，即使前两层保护失效，也能防止超卖

**实现方式**：使用 `UPDATE ... WHERE stock >= quantity` 条件更新

#### 4.2 技术实现

**实现文件**：
- `CommodityRepository.java` - `updateStockWithCondition()` 方法

**核心代码**：
```java
// CommodityRepository.java
@Modifying
@Query("UPDATE Commodity c SET c.stock = c.stock - :quantity " +
       "WHERE c.commodityId = :commodityId AND c.stock >= :quantity")
int updateStockWithCondition(@Param("commodityId") String commodityId, 
                            @Param("quantity") Integer quantity);
```

**使用方式**：
```java
// OrderServiceImpl.java
// ✅ 第三步：使用数据库条件更新库存（三重保护）
int updateResult = commodityRepository.updateStockWithCondition(
    orderDTO.getCommodityId(),
    orderDTO.getQuantity()
);

if (updateResult == 0) {
    log.warn("库存扣减失败 - commodityId: {}, quantity: {}, currentStock: {}",
        orderDTO.getCommodityId(), orderDTO.getQuantity(), commodity.getStock());
    return Result.fail("商品库存不足，请刷新后重试");
}
// updateResult == 1 表示更新成功
```

**关键特性**：
1. **原子性**：UPDATE语句本身就是原子操作
2. **条件判断**：WHERE子句在数据库层面检查库存
3. **返回值**：返回更新的行数（1=成功，0=库存不足）

---

### 5. 完整订单创建流程

#### 5.1 流程图

```
用户下单
    ↓
1. 获取Redis分布式锁（跨服务器保护）
   ├─ 成功 → 继续
   └─ 失败 → 返回"系统繁忙"
    ↓
2. 使用悲观锁查询商品（数据库层面保护）
   ├─ 锁定商品行
   ├─ 检查商品状态
   ├─ 检查库存
   └─ 检查其他业务规则
    ↓
3. 使用条件更新扣减库存（最终保护）
   ├─ 成功（返回1）→ 继续
   └─ 失败（返回0）→ 返回"库存不足"
    ↓
4. 创建订单
    ↓
5. 创建变更记录
    ↓
6. 推送WebSocket通知
    ↓
7. 释放分布式锁
    ↓
8. 返回成功
```

#### 5.2 代码实现

**完整实现**：`OrderServiceImpl.createOrder()`

```java
@Override
@Transactional
public Result createOrder(OrderDTO orderDTO) {
    // ✅ 第一步：获取分布式锁（跨服务器保护）
    String lockKey = RedisConstants.LOCK_COMMODITY_KEY + orderDTO.getCommodityId();
    String lockValue = RedisLockUtil.generateLockValue();
    long lockTimeout = RedisConstants.LOCK_COMMODITY_TTL;
    
    boolean lockAcquired = false;
    try {
        lockAcquired = redisLockUtil.tryLock(lockKey, lockValue, lockTimeout, 1, 100);
        
        if (!lockAcquired) {
            return Result.fail("系统繁忙，请稍后重试");
        }
        
        // ✅ 第二步：使用悲观锁查询商品（数据库层面保护）
        Optional<Commodity> commodityOpt = commodityRepository.findByIdForUpdate(orderDTO.getCommodityId());
        if (commodityOpt.isEmpty()) {
            return Result.fail("商品不存在");
        }
        
        Commodity commodity = commodityOpt.get();
        
        // 检查库存
        if (commodity.getStock() < orderDTO.getQuantity()) {
            return Result.fail("商品库存不足，当前库存：" + commodity.getStock());
        }
        
        // ✅ 第三步：使用数据库条件更新库存（三重保护）
        int updateResult = commodityRepository.updateStockWithCondition(
            orderDTO.getCommodityId(),
            orderDTO.getQuantity()
        );
        
        if (updateResult == 0) {
            return Result.fail("商品库存不足，请刷新后重试");
        }
        
        // 创建订单
        Order order = new Order();
        // ... 订单创建逻辑 ...
        orderRepository.save(order);
        
        return Result.ok("订单创建成功");
        
    } finally {
        if (lockAcquired) {
            redisLockUtil.releaseLock(lockKey, lockValue);
        }
    }
}
```

---

## 技术实现细节

### 6. RedisLockUtil 工具类

#### 6.1 核心方法

**实现文件**：`RedisLockUtil.java`

**方法列表**：
1. **tryLock()** - 加锁
   - 使用 `SET key value NX EX timeout` 实现原子性
   - 支持重试机制（最多等待时间、重试间隔）
   - 返回 true 表示加锁成功

2. **releaseLock()** - 释放锁
   - 使用 Lua 脚本确保原子性
   - 只有当 value 匹配时才删除（防止误删其他线程的锁）

3. **renewLock()** - 续期锁
   - 延长锁的过期时间
   - 用于长时间任务

4. **isLocked()** - 检查锁状态
   - 检查锁是否存在

#### 6.2 Lua脚本

**释放锁脚本**：`lua/unlock.lua`
```lua
if redis.call("get", KEYS[1]) == ARGV[1] then
    return redis.call("del", KEYS[1])
else
    return 0
end
```

**续期锁脚本**：`lua/renew.lua`
```lua
if redis.call("get", KEYS[1]) == ARGV[1] then
    return redis.call("expire", KEYS[1], ARGV[2])
else
    return 0
end
```

---

### 7. 数据库Repository方法

#### 7.1 悲观锁查询

**实现**：`CommodityRepository.findByIdForUpdate()`

```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("SELECT c FROM Commodity c WHERE c.commodityId = :commodityId")
Optional<Commodity> findByIdForUpdate(@Param("commodityId") String commodityId);
```

#### 7.2 条件更新

**实现**：`CommodityRepository.updateStockWithCondition()`

```java
@Modifying
@Query("UPDATE Commodity c SET c.stock = c.stock - :quantity " +
       "WHERE c.commodityId = :commodityId AND c.stock >= :quantity")
int updateStockWithCondition(@Param("commodityId") String commodityId, 
                            @Param("quantity") Integer quantity);
```

---

## 测试验证

### 8. 压力测试配置

#### 8.1 测试场景

**配置**：
- **并发用户数**：100
- **启动时间**：5秒
- **测试商品库存**：10
- **每个用户购买数量**：1
- **预期结果**：只有10个订单成功，90个返回"库存不足"

#### 8.2 测试工具

**JMeter配置**：
- 文件：`scripts/线程组.jmx`
- CSV数据：`scripts/user_tokens.csv`（100个用户token）
- 接口：`POST /api/user/order/create`

**批量用户创建脚本**：
- `scripts/batch_create_users_simple.py` - 简化版（无需验证码）
- `scripts/batch_create_users.py` - 完整版

#### 8.3 测试结果

**核心指标**：

| 指标 | 预期值 | 实际值 | 状态 |
|------|--------|--------|------|
| 总请求数 | 100 | 100 | ✅ |
| 成功订单数 | 10 | 10 | ✅ |
| 库存不足响应 | 90 | 90 | ✅ |
| 最终库存 | 0 | 0 | ✅ |
| 超卖现象 | 无 | 无 | ✅ |
| 错误率 | 0% | 0% | ✅ |

**验证结论**：
- ✅ 无超卖现象
- ✅ 数据一致性完美
- ✅ 性能表现良好（平均响应时间 < 200ms）
- ✅ 三重保护机制全部正常工作

---

### 9. 并发量评估

#### 9.1 项目规模

**项目特征**：
- 校园二手交易平台
- 用户规模：10万以下
- 商品特征：大部分商品库存为1（二手商品）
- 业务场景：典型的P2P交易模式

#### 9.2 并发量合理性

| 评估维度 | 实际情况 | 评估结果 |
|---------|---------|---------|
| 用户规模 | 10万用户 | ✅ 合理 |
| 并发用户数 | 100个并发 | ✅ 贴合 |
| 启动时间 | 5秒内启动 | ✅ 合理 |
| 测试库存 | 10个库存 | ✅ 贴合实际（热门商品） |
| 并发比例 | 0.1%用户同时下单 | ✅ 符合校园场景 |

**结论**：✅ **100并发用户完全贴合项目实际情况**

**理由**：
1. 校园场景特点：单个热门商品可能有50-200人同时关注，实际同时下单的并发量通常在10-100之间
2. 库存特征：大部分商品库存为1，测试使用库存10代表热门商品场景
3. 压力测试：100并发用户已能充分验证系统并发处理能力

---

## 已知问题与限制

### 10. 当前限制

#### 10.1 性能考虑

- **分布式锁等待时间**：当前设置为最多等待1秒，在高并发场景下可能需要调整
- **锁超时时间**：当前设置为10秒，对于复杂订单可能需要更长

#### 10.2 扩展性

- **单Redis实例**：当前使用单Redis实例，未来可考虑Redis集群
- **锁粒度**：当前按商品ID加锁，未来可考虑更细粒度的锁

#### 10.3 监控和告警

- **锁竞争监控**：当前只有日志记录，未来可添加监控指标
- **性能监控**：当前缺少详细的性能指标收集

---

## 下一步规划

### 11. 优化方向

#### 11.1 性能优化

- ✅ **索引优化**（v1.2.2已完成）：订单、商品、用户、对话、消息表索引优化
- **缓存优化**（v1.3.x）：商品信息缓存（Cache-Aside模式）
- **查询优化**（v1.3.x）：N+1查询问题优化

#### 11.2 功能增强

- **锁监控**：添加分布式锁的监控和告警
- **性能指标**：收集和展示性能指标
- **日志优化**：完善日志记录和查询

#### 11.3 扩展性

- **Redis集群**：支持Redis集群部署
- **锁策略优化**：更细粒度的锁策略
- **异步处理**：订单创建异步化（如需要）

---

## 相关文档

### 实现文档
- [分布式锁实现文档](./DISTRIBUTED_LOCK_IMPLEMENTATION.md)
- [库存超卖解决方案](../scripts/STOCK_OVERSALE_SOLUTION.md)

### 测试文档
- [测试报告](./V1.2.0_STOCK_OVERSALE_TEST_REPORT.md)
- [版本完成报告](./V1.2.0_COMPLETION_REPORT.md)
- [测试总结](./V1.2.0_TEST_SUMMARY.md)
- [完整测试指南](../scripts/README_TESTING.md)
- [JMeter使用指南](../scripts/JMETER_GUIDE.md)

### 故障排查
- [故障排查指南](../scripts/TROUBLESHOOTING.md)
- [CSV配置修复](../scripts/CSV_CONFIG_FIX.md)
- [401错误修复](../scripts/JMETER_401_FIX.md)

---

**文档版本**：v1.2.0  
**最后更新**：2025-11-05  
**维护者**：NJUMarket 开发团队

---

## 相关版本文档

- [v1.2.1 项目文档](./PROJECT_DOCUMENTATION_V1.2.1.md) - 分页总数修复
- [v1.2.2 项目文档](./PROJECT_DOCUMENTATION_V1.2.2.md) - 索引优化
- [性能优化建议](./PERFORMANCE_OPTIMIZATION_RECOMMENDATIONS.md) - v1.3.x/v1.4.x优化建议

