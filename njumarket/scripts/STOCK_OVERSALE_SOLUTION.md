# 库存超卖问题解决方案（简明版）

## 🤔 什么是库存超卖？

**简单理解**：商品库存只有1件，但2个人同时下单，结果都成功了，库存变成了-1。这就是**超卖**。

---

## ✅ 解决方案：三重保护机制

我们使用了**三层保护**来防止超卖：

```
┌─────────────────────────────────┐
│  第1层：分布式锁（Redis）        │  ← 防止多台服务器同时处理
│  第2层：数据库悲观锁             │  ← 防止数据库并发修改
│  第3层：条件更新（SQL）          │  ← 数据库层面最终保护
└─────────────────────────────────┘
```

### 第1层：分布式锁（Redis）

**作用**：防止多台服务器同时处理同一商品

**原理**：
- 服务器1处理商品A时，先获取Redis锁
- 服务器2也想处理商品A，但发现锁已被占用，必须等待
- 服务器1处理完后释放锁，服务器2才能继续

**代码位置**：
```java
// 获取分布式锁
String lockKey = "lock:commodity:" + commodityId;
redisLockUtil.tryLock(lockKey, lockValue, 10, 1, 100);
```

### 第2层：数据库悲观锁

**作用**：在数据库层面锁定商品行

**原理**：
- 使用 `SELECT ... FOR UPDATE` 锁定商品行
- 其他数据库连接必须等待当前事务完成

**代码位置**：
```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("SELECT c FROM Commodity c WHERE c.commodityId = :commodityId")
Optional<Commodity> findByIdForUpdate(@Param("commodityId") String commodityId);
```

### 第3层：条件更新（SQL）

**作用**：数据库层面判断库存是否充足

**原理**：
- SQL语句：`UPDATE ... WHERE stock >= quantity`
- 只有当库存 >= 购买数量时才执行更新
- 返回更新行数：1=成功，0=库存不足

**代码位置**：
```java
@Modifying
@Query("UPDATE Commodity c SET c.stock = c.stock - :quantity WHERE c.commodityId = :commodityId AND c.stock >= :quantity")
int updateStockWithCondition(@Param("commodityId") String commodityId, @Param("quantity") Integer quantity);
```

---

## 🔄 完整流程

### 用户下单流程

```
1. 用户点击"立即购买"
   ↓
2. 获取分布式锁（Redis）
   ├─ 成功 → 继续
   └─ 失败 → 返回"系统繁忙"
   ↓
3. 使用悲观锁查询商品（数据库）
   ├─ 锁定商品行
   └─ 检查库存
   ↓
4. 使用条件更新扣减库存（数据库）
   ├─ 成功（返回1）→ 继续
   └─ 失败（返回0）→ 返回"库存不足"
   ↓
5. 创建订单
   ↓
6. 释放分布式锁
   ↓
7. 返回成功
```

---

## 📊 为什么需要三层保护？

### 场景1：单服务器

**只需要**：数据库悲观锁 + 条件更新

```
服务器1
  ↓
数据库锁（足够）
```

### 场景2：多服务器（你的项目）

**需要**：分布式锁 + 数据库锁 + 条件更新

```
服务器1 ──┐
          ├─→ 数据库（共享）
服务器2 ──┘

如果没有分布式锁：
- 服务器1和2可能同时查询到库存=1
- 两个都通过检查，导致超卖

有了分布式锁：
- 服务器1先获取锁，处理订单
- 服务器2等待锁释放后，发现库存已为0
```

---

## 🎯 测试验证

### 测试场景

- **并发用户**：100个
- **商品库存**：10
- **每个用户购买**：1件

### 预期结果

- ✅ 成功订单数 = 10（不超过库存）
- ✅ 库存不足响应 = 90
- ✅ 最终库存 = 0（不能为负数）
- ✅ 无超卖现象

### 如何验证

1. **查看数据库**：
   ```sql
   -- 检查最终库存
   SELECT stock FROM commodities WHERE commodity_id = 'test-commodity-001';
   -- 应该是：0
   
   -- 检查订单数
   SELECT COUNT(*) FROM orders WHERE commodity_id = 'test-commodity-001';
   -- 应该是：10
   ```

2. **查看JMeter报告**：
   - 成功请求数 = 10
   - 库存不足响应 = 90
   - 错误率 = 0%

---

## 💡 关键要点

### 1. 分布式锁必须在finally中释放

```java
try {
    // 业务逻辑
} finally {
    if (lockAcquired) {
        redisLockUtil.releaseLock(lockKey, lockValue);
    }
}
```

### 2. 锁的超时时间要合理

- **太短**：业务还没执行完，锁就过期了
- **太长**：服务器崩溃时，锁长时间不释放

**建议**：10-30秒

### 3. 三层保护缺一不可

- **分布式锁**：解决多服务器问题
- **悲观锁**：解决数据库并发问题
- **条件更新**：最终保障

---

## 📚 相关文档

- [完整测试指南](./README_TESTING.md)
- [JMeter使用指南](./JMETER_GUIDE.md)
- [批量用户创建指南](./README_BATCH_USERS.md)

---

**适合人群**：初学者  
**难度等级**：⭐⭐⭐（中等）  
**预计阅读时间**：15分钟
