# 性能优化建议文档

## 📋 目录
- [概述](#概述)
- [v1.3.x 优化建议](#v13x-优化建议)
- [v1.4.x 优化建议](#v14x-优化建议)
- [缓存策略详细设计](#缓存策略详细设计)
- [索引优化详细方案](#索引优化详细方案)
- [实施优先级](#实施优先级)

---

## 概述

本文档针对 NJUMarket 项目的性能优化提供详细建议，主要涵盖：
- **数据库索引优化**（v1.3.x）
- **缓存策略和缓存预热**（v1.3.x / v1.4.x）
- **查询优化**（v1.3.x）
- **其他性能优化**（v1.4.x）

---

## v1.3.x 优化建议

### 1. 数据库索引优化（**必须实施**）

#### 1.1 订单表索引优化

**问题**：当前订单查询主要依赖主键和简单索引，分页查询和COUNT查询性能待优化

**建议索引**：

```sql
-- 1. 买家订单查询索引（最常用）
CREATE INDEX idx_buyer_visibility_time ON orders(buyer_id, buyer_visibility, create_time DESC);

-- 2. 买家订单状态查询索引
CREATE INDEX idx_buyer_status_visibility ON orders(buyer_id, order_status, buyer_visibility);

-- 3. 卖家订单查询索引
CREATE INDEX idx_seller_visibility_time ON orders(seller_id, seller_visibility, create_time DESC);

-- 4. 卖家订单状态查询索引
CREATE INDEX idx_seller_status_visibility ON orders(seller_id, order_status, seller_visibility);

-- 5. 订单状态统计索引
CREATE INDEX idx_status_time ON orders(order_status, create_time DESC);
```

**性能影响**：
- COUNT查询速度提升 **50-80%**
- 分页查询速度提升 **30-50%**
- 支持高效的状态筛选和时间排序

#### 1.2 商品表索引优化

**已有索引**：部分索引已创建（见`add_commodity_composite_indexes.sql`）

**补充索引**：

```sql
-- 1. 商品搜索优化索引（关键词搜索）
-- 注意：MySQL全文索引需要MyISAM或InnoDB 5.6+，且支持中文分词
CREATE FULLTEXT INDEX idx_title_desc_fulltext ON commodities(title, description);

-- 2. 商品浏览优化索引（状态+可见性+时间）
CREATE INDEX idx_status_visibility_time ON commodities(
    commodity_status, 
    seller_visibility, 
    buyer_visibility, 
    publish_time DESC
);

-- 3. 商品价格筛选索引（状态+价格）
CREATE INDEX idx_status_price ON commodities(commodity_status, price);
```

**性能影响**：
- 关键词搜索速度提升 **60-90%**（使用全文索引）
- 商品列表查询速度提升 **40-60%**

#### 1.3 用户表索引优化

```sql
-- 1. 用户状态查询索引
CREATE INDEX idx_account_status ON users(account_status);

-- 2. 用户档案查询索引
CREATE INDEX idx_user_profile_user_id ON user_profiles(user_id);
```

#### 1.4 索引维护建议

```sql
-- 定期更新索引统计信息（提高查询优化器效率）
ANALYZE TABLE orders;
ANALYZE TABLE commodities;
ANALYZE TABLE users;
ANALYZE TABLE user_profiles;
```

---

### 2. 缓存策略实施（**强烈建议**）

#### 2.1 商品浏览缓存（**最高优先级**）

**缓存策略选择：Cache Aside**

**为什么选择 Cache Aside？**
- ✅ **读多写少**：商品浏览占 90%+，写操作（发布/编辑）占 10%-
- ✅ **最终一致性可接受**：商品信息更新延迟几秒钟完全可接受
- ✅ **实现简单**：写操作时只需删除缓存，让下次查询自动缓存
- ✅ **减少不一致风险**：不更新缓存，只删除缓存，避免缓存与数据库不一致

**详细分析**：参见 [缓存策略分析文档](./CACHE_STRATEGY_ANALYSIS.md)

**为什么需要缓存预热？**

商品浏览是**超高频率请求**：
- 首页商品列表：每个用户访问首页都会请求
- 分类浏览：用户频繁切换分类查看
- 搜索功能：用户频繁搜索商品
- 热门/最新商品：首页展示，访问频率极高

**不缓存的风险**：
- 数据库压力大：大量并发查询
- 响应时间长：每次都需要查询数据库
- 用户体验差：页面加载慢

**缓存策略设计**：

```java
@Service
public class CommodityCacheService {
    
    // 缓存键前缀
    private static final String CACHE_KEY_PREFIX = "commodity:";
    private static final String LIST_CACHE_KEY_PREFIX = "commodity:list:";
    
    // 缓存过期时间（秒）
    private static final int CACHE_TTL_DEFAULT = 300; // 5分钟
    private static final int CACHE_TTL_HOT = 600; // 10分钟（热门商品缓存更久）
    
    /**
     * 缓存商品列表（带分页）
     */
    public List<CommodityDTO> getCachedCommodityList(String key, int page, int size) {
        String cacheKey = LIST_CACHE_KEY_PREFIX + key + ":page:" + page + ":size:" + size;
        return redisTemplate.opsForValue().get(cacheKey);
    }
    
    /**
     * 设置商品列表缓存
     */
    public void setCachedCommodityList(String key, int page, int size, 
                                      List<CommodityDTO> commodities, int ttl) {
        String cacheKey = LIST_CACHE_KEY_PREFIX + key + ":page:" + page + ":size:" + size;
        redisTemplate.opsForValue().set(cacheKey, commodities, ttl, TimeUnit.SECONDS);
    }
    
    /**
     * 缓存预热：预加载热门查询
     */
    @PostConstruct
    public void warmUpCache() {
        log.info("开始缓存预热...");
        
        // 1. 预热首页商品列表（前3页）
        warmUpHomePageCommodities();
        
        // 2. 预热热门商品
        warmUpHotCommodities();
        
        // 3. 预热最新商品
        warmUpLatestCommodities();
        
        // 4. 预热各分类商品（前2页）
        warmUpCategoryCommodities();
        
        log.info("缓存预热完成");
    }
    
    private void warmUpHomePageCommodities() {
        // 预热首页商品列表（无筛选条件，按时间排序）
        for (int page = 1; page <= 3; page++) {
            try {
                Pageable pageable = PageRequest.of(page - 1, 20);
                Page<Commodity> commodityPage = commodityRepository
                    .findByCommodityStatusAndSellerVisibilityAndBuyerVisibility(
                        "ON_SHELF", "PUBLIC", "PUBLIC", pageable);
                
                List<CommodityDTO> dtos = convertToDTOs(commodityPage.getContent());
                setCachedCommodityList("home:all", page, 20, dtos, CACHE_TTL_DEFAULT);
                
                log.info("预热首页商品列表 - 第{}页，共{}条", page, dtos.size());
            } catch (Exception e) {
                log.error("预热首页商品列表失败 - 第{}页", page, e);
            }
        }
    }
    
    private void warmUpHotCommodities() {
        // 预热热门商品（前10个）
        try {
            Pageable pageable = PageRequest.of(0, 10);
            List<Commodity> hotCommodities = commodityRepository.findHotCommodities(pageable);
            List<CommodityDTO> dtos = convertToDTOs(hotCommodities);
            
            String cacheKey = CACHE_KEY_PREFIX + "hot:10";
            redisTemplate.opsForValue().set(cacheKey, dtos, CACHE_TTL_HOT, TimeUnit.SECONDS);
            
            log.info("预热热门商品 - 共{}条", dtos.size());
        } catch (Exception e) {
            log.error("预热热门商品失败", e);
        }
    }
    
    private void warmUpLatestCommodities() {
        // 预热最新商品（前10个）
        try {
            Pageable pageable = PageRequest.of(0, 10);
            List<Commodity> latestCommodities = commodityRepository.findLatestCommodities(pageable);
            List<CommodityDTO> dtos = convertToDTOs(latestCommodities);
            
            String cacheKey = CACHE_KEY_PREFIX + "latest:10";
            redisTemplate.opsForValue().set(cacheKey, dtos, CACHE_TTL_DEFAULT, TimeUnit.SECONDS);
            
            log.info("预热最新商品 - 共{}条", dtos.size());
        } catch (Exception e) {
            log.error("预热最新商品失败", e);
        }
    }
    
    private void warmUpCategoryCommodities() {
        // 预热各分类商品（前2页）
        List<String> categories = Arrays.asList("书籍", "电子产品", "生活用品", "服装", "其他");
        
        for (String category : categories) {
            for (int page = 1; page <= 2; page++) {
                try {
                    Pageable pageable = PageRequest.of(page - 1, 20);
                    Page<Commodity> commodityPage = commodityRepository
                        .findByCategoryAndVisible(category, pageable);
                    
                    List<CommodityDTO> dtos = convertToDTOs(commodityPage.getContent());
                    setCachedCommodityList("category:" + category, page, 20, dtos, CACHE_TTL_DEFAULT);
                    
                    log.info("预热分类商品 - {}，第{}页，共{}条", category, page, dtos.size());
                } catch (Exception e) {
                    log.error("预热分类商品失败 - {}，第{}页", category, page, e);
                }
            }
        }
    }
    
    /**
     * 缓存失效：商品更新时清除相关缓存
     */
    public void invalidateCommodityCache(String commodityId) {
        // 清除商品详情缓存
        String detailKey = CACHE_KEY_PREFIX + "detail:" + commodityId;
        redisTemplate.delete(detailKey);
        
        // 清除所有列表缓存（简化处理，实际可以使用更细粒度的失效）
        Set<String> keys = redisTemplate.keys(LIST_CACHE_KEY_PREFIX + "*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
        
        log.info("清除商品缓存 - commodityId: {}", commodityId);
    }
}
```

**缓存预热触发时机**：
1. **应用启动时**：使用`@PostConstruct`自动预热
2. **定时任务**：每小时刷新一次热门/最新商品缓存
3. **手动触发**：管理员可以手动触发缓存刷新

**缓存失效策略**：
- **商品更新**：立即清除相关缓存
- **商品上架/下架**：清除列表缓存
- **定时刷新**：定期刷新热门/最新商品缓存

#### 2.2 商品详情缓存

```java
/**
 * 缓存商品详情（单条商品）
 */
public CommodityDTO getCachedCommodityDetail(String commodityId) {
    String cacheKey = CACHE_KEY_PREFIX + "detail:" + commodityId;
    return redisTemplate.opsForValue().get(cacheKey);
}

public void setCachedCommodityDetail(String commodityId, CommodityDTO commodity, int ttl) {
    String cacheKey = CACHE_KEY_PREFIX + "detail:" + commodityId;
    redisTemplate.opsForValue().set(cacheKey, commodity, ttl, TimeUnit.SECONDS);
}
```

**缓存策略**：
- **TTL**：10分钟（商品详情变化不频繁）
- **失效**：商品更新时立即清除

#### 2.3 用户资料缓存

```java
/**
 * 缓存用户资料（减少N+1查询）
 */
public UserProfileDTO getCachedUserProfile(String userId) {
    String cacheKey = "user:profile:" + userId;
    return redisTemplate.opsForValue().get(cacheKey);
}

public void setCachedUserProfile(String userId, UserProfileDTO profile) {
    String cacheKey = "user:profile:" + userId;
    redisTemplate.opsForValue().set(cacheKey, profile, 900, TimeUnit.SECONDS); // 15分钟
}
```

---

### 3. 查询优化

#### 3.1 N+1查询进一步优化

**已优化**：订单列表、商品列表的批量查询

**待优化**：
- 商品详情页的卖家信息（已批量查询，可进一步缓存）
- 订单详情页的关联信息（商品、用户信息）

#### 3.2 分页查询优化

**优化COUNT查询**：
- 为常用查询字段创建覆盖索引
- 考虑使用近似计数（对于大数据量场景）

---

## v1.4.x 优化建议

### 4. 高级缓存策略

#### 4.1 缓存预热定时任务

```java
@Scheduled(cron = "0 0 * * * ?") // 每小时执行一次
public void refreshHotCommoditiesCache() {
    log.info("定时刷新热门商品缓存...");
    warmUpHotCommodities();
}

@Scheduled(cron = "0 */30 * * * ?") // 每30分钟执行一次
public void refreshLatestCommoditiesCache() {
    log.info("定时刷新最新商品缓存...");
    warmUpLatestCommodities();
}
```

#### 4.2 缓存穿透防护

```java
/**
 * 防止缓存穿透：对于不存在的商品，缓存空值
 */
public CommodityDTO getCommodityDetailWithProtection(String commodityId) {
    String cacheKey = CACHE_KEY_PREFIX + "detail:" + commodityId;
    CommodityDTO cached = redisTemplate.opsForValue().get(cacheKey);
    
    if (cached != null) {
        // 检查是否是空值标记
        if (cached.getCommodityId() == null) {
            return null; // 商品不存在，且已缓存
        }
        return cached;
    }
    
    // 查询数据库
    Commodity commodity = commodityRepository.findById(commodityId).orElse(null);
    
    if (commodity == null) {
        // 缓存空值，防止缓存穿透（短TTL）
        CommodityDTO emptyDTO = new CommodityDTO();
        redisTemplate.opsForValue().set(cacheKey, emptyDTO, 60, TimeUnit.SECONDS);
        return null;
    }
    
    CommodityDTO dto = convertToDTO(commodity);
    setCachedCommodityDetail(commodityId, dto, 600);
    return dto;
}
```

#### 4.3 缓存击穿防护（分布式锁）

```java
/**
 * 防止缓存击穿：使用分布式锁
 */
public CommodityDTO getCommodityDetailWithLock(String commodityId) {
    String cacheKey = CACHE_KEY_PREFIX + "detail:" + commodityId;
    CommodityDTO cached = redisTemplate.opsForValue().get(cacheKey);
    
    if (cached != null) {
        return cached;
    }
    
    // 使用分布式锁防止缓存击穿
    String lockKey = "lock:commodity:" + commodityId;
    String lockValue = UUID.randomUUID().toString();
    
    try {
        boolean lockAcquired = redisLockUtil.tryLock(lockKey, lockValue, 10, 1, 100);
        
        if (lockAcquired) {
            // 再次检查缓存（双重检查）
            cached = redisTemplate.opsForValue().get(cacheKey);
            if (cached != null) {
                return cached;
            }
            
            // 查询数据库
            Commodity commodity = commodityRepository.findById(commodityId)
                .orElseThrow(() -> new ResourceNotFoundException("商品不存在"));
            
            CommodityDTO dto = convertToDTO(commodity);
            setCachedCommodityDetail(commodityId, dto, 600);
            return dto;
        } else {
            // 获取锁失败，等待一段时间后重试
            Thread.sleep(50);
            return getCommodityDetailWithLock(commodityId);
        }
    } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        throw new RuntimeException("获取商品详情失败", e);
    } finally {
        redisLockUtil.releaseLock(lockKey, lockValue);
    }
}
```

#### 4.4 缓存雪崩防护

```java
/**
 * 防止缓存雪崩：为缓存TTL添加随机值
 */
private int getRandomTTL(int baseTTL) {
    // 在基础TTL基础上，随机增加0-20%的时间
    int randomOffset = (int) (baseTTL * 0.2 * Math.random());
    return baseTTL + randomOffset;
}

public void setCachedCommodityList(String key, int page, int size, 
                                  List<CommodityDTO> commodities) {
    int ttl = getRandomTTL(CACHE_TTL_DEFAULT); // 添加随机值
    setCachedCommodityList(key, page, size, commodities, ttl);
}
```

---

### 5. 数据库连接池优化

```properties
# application.properties
# HikariCP连接池配置（针对高并发场景）
spring.datasource.hikari.maximum-pool-size=20
spring.datasource.hikari.minimum-idle=5
spring.datasource.hikari.connection-timeout=30000
spring.datasource.hikari.idle-timeout=600000
spring.datasource.hikari.max-lifetime=1800000
spring.datasource.hikari.leak-detection-threshold=60000
```

---

### 6. 查询结果压缩

**适用场景**：大量商品列表数据

```java
/**
 * 使用压缩存储缓存数据（减少Redis内存占用）
 */
public void setCachedCommodityListCompressed(String key, List<CommodityDTO> commodities) {
    try {
        // 序列化
        byte[] serialized = objectMapper.writeValueAsBytes(commodities);
        
        // 压缩（使用GZIP）
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (GZIPOutputStream gzos = new GZIPOutputStream(baos)) {
            gzos.write(serialized);
        }
        byte[] compressed = baos.toByteArray();
        
        // 存储到Redis
        redisTemplate.opsForValue().set(key, compressed, CACHE_TTL_DEFAULT, TimeUnit.SECONDS);
    } catch (Exception e) {
        log.error("压缩缓存数据失败", e);
    }
}
```

---

## 缓存策略详细设计

### 7. 缓存架构设计

#### 7.1 缓存层级

```
┌─────────────────────────────────┐
│   L1: 应用内存缓存（Caffeine）   │  ← 热点数据，极快
├─────────────────────────────────┤
│   L2: Redis分布式缓存            │  ← 共享缓存，快速
├─────────────────────────────────┤
│   L3: 数据库                    │  ← 持久化存储
└─────────────────────────────────┘
```

**建议**：
- **v1.3.x**：先实现L2（Redis缓存）
- **v1.4.x**：考虑添加L1（本地缓存）作为二级缓存

#### 7.2 缓存键设计规范

```java
// 商品相关
"commodity:detail:{commodityId}"           // 商品详情
"commodity:list:{type}:page:{page}:size:{size}"  // 商品列表
"commodity:hot:{limit}"                   // 热门商品
"commodity:latest:{limit}"                // 最新商品

// 用户相关
"user:profile:{userId}"                   // 用户资料
"user:orders:{userId}:page:{page}"        // 用户订单列表

// 订单相关
"order:detail:{orderId}"                  // 订单详情
```

---

## 索引优化详细方案

### 8. 索引创建脚本

创建统一的索引优化脚本：`database/optimize_indexes_v1.3.sql`

```sql
-- ========================================
-- NJUMarket v1.3.x 索引优化脚本
-- ========================================

-- 订单表索引
CREATE INDEX IF NOT EXISTS idx_buyer_visibility_time 
ON orders(buyer_id, buyer_visibility, create_time DESC);

CREATE INDEX IF NOT EXISTS idx_buyer_status_visibility 
ON orders(buyer_id, order_status, buyer_visibility);

CREATE INDEX IF NOT EXISTS idx_seller_visibility_time 
ON orders(seller_id, seller_visibility, create_time DESC);

CREATE INDEX IF NOT EXISTS idx_seller_status_visibility 
ON orders(seller_id, order_status, seller_visibility);

-- 商品表索引（补充）
CREATE INDEX IF NOT EXISTS idx_status_visibility_time 
ON commodities(commodity_status, seller_visibility, buyer_visibility, publish_time DESC);

-- 用户表索引
CREATE INDEX IF NOT EXISTS idx_account_status ON users(account_status);
CREATE INDEX IF NOT EXISTS idx_user_profile_user_id ON user_profiles(user_id);

-- 更新统计信息
ANALYZE TABLE orders;
ANALYZE TABLE commodities;
ANALYZE TABLE users;
ANALYZE TABLE user_profiles;
```

---

## 实施优先级

### 9. 优化实施顺序

#### Phase 1: v1.3.0（高优先级）
1. ✅ **索引优化**（必须）
   - 订单表索引
   - 商品表补充索引
   - 用户表索引

2. ✅ **商品浏览缓存**（强烈建议）
   - 缓存预热实现
   - 商品列表缓存
   - 热门/最新商品缓存

3. ✅ **商品详情缓存**（建议）
   - 单商品缓存
   - 缓存失效机制

#### Phase 2: v1.3.1（中优先级）
1. ✅ **用户资料缓存**
2. ✅ **缓存预热定时任务**
3. ✅ **缓存穿透防护**

#### Phase 3: v1.4.0（优化增强）
1. ✅ **缓存击穿防护**（分布式锁）
2. ✅ **缓存雪崩防护**（随机TTL）
3. ✅ **数据库连接池优化**
4. ✅ **查询结果压缩**

---

## 性能提升预期

### 10. 优化效果预期

| 优化项 | 当前性能 | 优化后性能 | 提升幅度 |
|--------|---------|-----------|---------|
| 商品列表查询 | 100-200ms | 10-50ms（缓存命中） | **80-90%** |
| 订单列表查询 | 50-100ms | 20-50ms | **50-60%** |
| 商品详情查询 | 50-80ms | 5-20ms（缓存命中） | **75-85%** |
| COUNT查询 | 30-80ms | 10-30ms | **60-70%** |
| 数据库压力 | 高 | 低（缓存分担） | **60-80%** |

---

## 监控和调优

### 11. 性能监控

#### 11.1 缓存命中率监控

```java
/**
 * 缓存统计
 */
@Component
public class CacheStatistics {
    private final AtomicLong hitCount = new AtomicLong(0);
    private final AtomicLong missCount = new AtomicLong(0);
    
    public void recordHit() {
        hitCount.incrementAndGet();
    }
    
    public void recordMiss() {
        missCount.incrementAndGet();
    }
    
    public double getHitRate() {
        long total = hitCount.get() + missCount.get();
        return total > 0 ? (double) hitCount.get() / total : 0;
    }
}
```

#### 11.2 慢查询监控

```properties
# 开启慢查询日志
spring.jpa.properties.hibernate.session.events.log.LOG_QUERIES_SLOWER_THAN_MS=100
```

---

## 总结

### 关键建议

1. **索引优化**（v1.3.x必须）：大幅提升查询性能，特别是COUNT查询
2. **缓存预热**（v1.3.x强烈建议）：对于商品浏览这种超高频率请求，缓存预热是必需的
3. **缓存策略**（v1.3.x / v1.4.x）：逐步实施，从简单到复杂
4. **监控和调优**（v1.4.x）：持续监控性能指标，根据实际情况调整

### 实施建议

- **v1.3.0**：索引优化 + 基础缓存（商品列表、详情）
- **v1.3.1**：缓存预热 + 缓存防护（穿透、击穿、雪崩）
- **v1.4.0**：高级优化（连接池、压缩、本地缓存）

---

**文档版本**：v1.0  
**最后更新**：2025-11-05  
**维护者**：NJUMarket 开发团队

