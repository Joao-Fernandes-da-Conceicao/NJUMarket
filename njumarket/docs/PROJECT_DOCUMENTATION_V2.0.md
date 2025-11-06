# 南大集市 NJUMarket v2.0 阶段规划

## 📋 目录
- [版本概述](#版本概述)
- [核心目标](#核心目标)
- [微服务拆分方案](#微服务拆分方案)
- [Spring Cloud 生态集成](#spring-cloud-生态集成)
- [分布式系统实践](#分布式系统实践)
- [技术学习重点](#技术学习重点)
- [实施计划](#实施计划)
- [预期成果](#预期成果)

---

## 版本概述

### 版本信息
- **版本**: v2.0
- **计划开始时间**: 2025-01-XX
- **基于版本**: v1.3.0
- **状态**: 规划中

### 版本定位
v2.0 版本将专注于**微服务架构改造**，将现有的单体应用拆分为多个微服务，学习Spring Cloud生态，实现服务注册与发现、配置中心、API网关、熔断降级等功能。通过微服务架构改造，提升系统的可扩展性、可维护性和高可用性。

### 主要目标
- ✅ **微服务拆分**：将单体应用拆分为5个核心微服务
- ✅ **Spring Cloud集成**：服务注册、配置中心、API网关
- ✅ **分布式系统**：分布式事务、分布式锁、分布式ID
- ✅ **消息队列**：RabbitMQ/RocketMQ实现异步处理
- ✅ **搜索引擎**：Elasticsearch实现商品和消息搜索
- ✅ **MyBatis复杂查询**：学习MyBatis处理复杂查询场景
- ✅ **缓存机制**：多级缓存、缓存一致性、缓存预热

---

## 核心目标

### 1. 架构升级
- **从单体到微服务**：学习微服务架构设计原则
- **服务拆分策略**：按业务领域拆分服务
- **服务治理**：服务注册、发现、配置、监控

### 2. 技术栈扩展
- **Spring Cloud**：学习Spring Cloud生态
- **消息队列**：学习消息队列的使用场景
- **搜索引擎**：学习Elasticsearch全文搜索
- **分布式系统**：学习分布式事务、分布式锁
- **MyBatis**：学习MyBatis处理复杂查询场景
- **缓存系统**：学习多级缓存、缓存一致性策略

### 3. 系统能力提升
- **可扩展性**：服务独立部署和扩展
- **可维护性**：服务职责清晰，代码组织更好
- **高可用性**：服务熔断、降级、限流

---

## 微服务拆分方案

### 1. 服务拆分原则

**拆分依据**：
- **业务领域**：按业务功能划分服务边界
- **数据独立性**：服务拥有独立的数据存储
- **服务粒度**：服务不宜过大也不宜过小
- **通信成本**：考虑服务间通信成本

### 2. 核心微服务设计

#### 2.1 用户服务（User Service）
**职责**：
- 用户认证（登录、注册、Token管理）
- 用户资料管理（昵称、头像、信用分、评分）
- VIP等级管理
- 用户排行榜

**数据存储**：
- `users` 表
- `user_profiles` 表

**API接口**：
- `/api/user/auth/*` - 认证相关
- `/api/user/profile/*` - 用户资料相关

**技术栈**：
- Spring Boot
- Spring Cloud
- JPA + MySQL（基础CRUD）
- MyBatis（复杂查询）
- Redis（Token存储）

---

#### 2.2 商品服务（Commodity Service）
**职责**：
- 商品发布、编辑、上下架
- 商品浏览、搜索、筛选
- 商品详情查询
- 商品可见性控制
- 商品快照管理

**数据存储**：
- `commodities` 表
- `commodity_images` 表
- `commodity_snapshots` 表（订单中使用）

**API接口**：
- `/api/user/commodity/*` - 商品管理相关

**技术栈**：
- Spring Boot
- Spring Cloud
- JPA + MySQL（基础CRUD）
- MyBatis（复杂查询、统计报表）
- Elasticsearch（商品搜索）
- Redis（商品缓存、多级缓存）

---

#### 2.3 订单服务（Order Service）
**职责**：
- 订单创建、支付、发货、收货
- 订单取消、退款、退货流程
- 订单状态管理
- 订单可见性控制
- 订单统计

**数据存储**：
- `orders` 表
- `order_snapshots` 表

**API接口**：
- `/api/user/order/*` - 订单管理相关

**技术栈**：
- Spring Boot
- Spring Cloud
- JPA + MySQL（基础CRUD）
- MyBatis（复杂查询、统计报表）
- Redis（分布式锁、库存缓存、多级缓存）
- RabbitMQ（订单异步处理）

---

#### 2.4 消息服务（Message Service）
**职责**：
- 会话管理（对话创建、查询、删除）
- 消息发送、接收、历史加载
- 消息软删除
- 未读数管理
- WebSocket实时通信

**数据存储**：
- `conversations` 表
- `messages` 表

**API接口**：
- `/api/user/contact/*` - 消息相关

**技术栈**：
- Spring Boot
- Spring Cloud
- JPA + MySQL
- WebSocket（实时通信）
- Redis（未读数缓存）

---

#### 2.5 通知服务（Notification Service）
**职责**：
- 订单变化通知（WebSocket推送）
- 消息通知（WebSocket推送）
- 系统通知
- 通知历史记录

**数据存储**：
- `notifications` 表（可选）

**API接口**：
- `/api/user/notification/*` - 通知相关

**技术栈**：
- Spring Boot
- Spring Cloud
- WebSocket（实时推送）
- RabbitMQ（通知队列）
- Redis（通知缓存）

---

### 3. 服务间通信

#### 3.1 同步通信
**技术选型**：OpenFeign
- **用户服务调用**：商品服务查询卖家信息、订单服务查询用户信息
- **商品服务调用**：订单服务查询商品信息
- **订单服务调用**：商品服务扣减库存、用户服务查询用户信息

#### 3.2 异步通信
**技术选型**：RabbitMQ / RocketMQ
- **订单创建事件**：订单服务 → 商品服务（扣减库存）
- **订单支付事件**：订单服务 → 通知服务（推送通知）
- **订单完成事件**：订单服务 → 用户服务（更新信用分）

---

## Spring Cloud 生态集成

### 1. 服务注册与发现

#### 1.1 技术选型
**选项1：Eureka**
- 优点：Spring Cloud原生支持，简单易用
- 缺点：Netflix已停止维护

**选项2：Nacos**
- 优点：功能强大，支持配置中心，阿里开源
- 缺点：学习成本稍高

**推荐**：Nacos（功能更全面，支持配置中心）

#### 1.2 实现内容
- 服务注册：各微服务启动时注册到注册中心
- 服务发现：服务间通过服务名调用，无需硬编码IP
- 健康检查：注册中心监控服务健康状态
- 负载均衡：Ribbon / LoadBalancer实现负载均衡

---

### 2. 配置中心

#### 2.1 技术选型
**选项1：Spring Cloud Config**
- 优点：Spring Cloud原生支持
- 缺点：需要Git仓库，配置更新需要重启

**选项2：Nacos Config**
- 优点：支持动态配置，无需重启
- 缺点：需要引入Nacos依赖

**推荐**：Nacos Config（支持动态配置）

#### 2.2 实现内容
- 集中配置管理：所有服务的配置统一管理
- 动态配置更新：配置变更无需重启服务
- 配置版本管理：支持配置版本回滚
- 环境隔离：开发、测试、生产环境配置隔离

---

### 3. API网关

#### 3.1 技术选型
**Spring Cloud Gateway**
- 优点：Spring Cloud原生支持，性能好
- 功能：路由、过滤、限流、熔断

#### 3.2 实现内容
- **路由转发**：统一入口，路由到各个微服务
- **请求过滤**：认证、鉴权、日志记录
- **限流控制**：API限流，防止服务过载
- **熔断降级**：服务异常时降级处理

**路由规则示例**：
```
/api/user/** → 用户服务
/api/commodity/** → 商品服务
/api/order/** → 订单服务
/api/contact/** → 消息服务
/api/notification/** → 通知服务
```

---

### 4. 服务调用

#### 4.1 OpenFeign
**功能**：
- 声明式HTTP客户端
- 负载均衡
- 服务降级

**使用场景**：
- 订单服务调用商品服务查询商品信息
- 订单服务调用用户服务查询用户信息
- 消息服务调用用户服务查询用户资料

#### 4.2 负载均衡
**技术选型**：Spring Cloud LoadBalancer
- 替换Ribbon（已停止维护）
- 支持多种负载均衡策略（轮询、随机、权重）

---

### 5. 熔断降级

#### 5.1 技术选型
**选项1：Hystrix**
- 优点：Netflix开源，功能完善
- 缺点：已停止维护

**选项2：Sentinel**
- 优点：阿里开源，功能强大，支持限流、熔断、降级
- 缺点：学习成本稍高

**推荐**：Sentinel（功能更全面，持续维护）

#### 5.2 实现内容
- **熔断机制**：服务异常率超过阈值时熔断
- **降级策略**：服务不可用时返回默认值或缓存数据
- **限流控制**：QPS限流，防止服务过载
- **实时监控**：Sentinel Dashboard监控服务状态

---

## 分布式系统实践

### 1. 分布式事务

#### 1.1 技术选型
**Seata**
- AT模式：自动事务，无需手动编写补偿逻辑
- TCC模式：Try-Confirm-Cancel，需要手动实现
- SAGA模式：长事务，适合复杂业务流程

**学习重点**：
- AT模式：简单易用，适合大多数场景
- TCC模式：理解Try-Confirm-Cancel三个阶段
- 分布式事务的CAP理论

#### 1.2 应用场景
- **订单创建**：订单服务 + 商品服务（扣减库存）
- **订单支付**：订单服务 + 用户服务（更新信用分）
- **订单退款**：订单服务 + 商品服务（恢复库存）+ 用户服务（更新信用分）

---

### 2. 分布式锁

#### 2.1 技术选型
**Redis分布式锁**（已实现，需优化）
- 使用Redisson实现
- 支持可重入锁、读写锁
- 支持锁续期

**优化方向**：
- 使用Redisson替代手动实现
- 支持锁超时自动释放
- 支持锁续期机制

#### 2.2 应用场景
- **库存扣减**：防止并发超卖
- **订单创建**：防止重复创建订单
- **消息发送**：防止重复发送消息

---

### 3. 分布式ID

#### 3.1 技术选型
**选项1：Snowflake算法**
- 优点：性能好，趋势递增
- 缺点：依赖机器时钟

**选项2：UUID**
- 优点：简单，无需中心化服务
- 缺点：无序，不适合作为主键

**选项3：数据库自增ID + 号段模式**
- 优点：性能好，趋势递增
- 缺点：需要数据库支持

**推荐**：Snowflake算法（性能好，趋势递增）

#### 3.2 实现内容
- **ID生成器**：统一ID生成服务
- **ID格式**：时间戳 + 机器ID + 序列号
- **ID分配**：各服务独立ID段，避免冲突

---

## 消息队列应用

### 1. 技术选型

#### 1.1 RabbitMQ
**优点**：
- 功能完善，支持多种消息模型
- 管理界面友好
- 社区活跃

**适用场景**：
- 订单异步处理
- 消息异步推送
- 系统通知

#### 1.2 RocketMQ
**优点**：
- 性能好，支持高并发
- 支持顺序消息
- 阿里开源，国内使用广泛

**适用场景**：
- 高并发场景
- 顺序消息场景

**推荐**：RabbitMQ（功能完善，学习成本低）

---

### 2. 消息模型

#### 2.1 点对点模型（Queue）
**应用场景**：
- 订单异步处理
- 消息异步推送

**实现**：
- 生产者发送消息到队列
- 消费者从队列消费消息
- 消息只能被一个消费者消费

#### 2.2 发布订阅模型（Topic/Exchange）
**应用场景**：
- 订单状态变更通知
- 系统广播通知

**实现**：
- 生产者发送消息到Exchange
- Exchange路由消息到多个Queue
- 多个消费者可以消费同一消息

---

### 3. 消息可靠性

#### 3.1 消息确认机制
- **生产者确认**：消息发送成功确认
- **消费者确认**：消息消费成功确认
- **消息重试**：消费失败时重试

#### 3.2 消息持久化
- **队列持久化**：队列重启后消息不丢失
- **消息持久化**：消息持久化到磁盘
- **Exchange持久化**：Exchange重启后不丢失

---

## MyBatis 复杂查询实践

### 1. 技术选型

#### 1.1 为什么引入MyBatis
**JPA的局限性**：
- 复杂SQL查询难以表达（多表关联、子查询、动态SQL）
- 性能优化困难（无法精确控制SQL）
- 统计报表查询复杂（聚合函数、分组、排序）

**MyBatis的优势**：
- 灵活的动态SQL（if、choose、foreach等）
- 精确控制SQL语句
- 支持复杂查询和统计报表
- 性能优化更容易

**混合使用策略**：
- **JPA**：用于基础CRUD操作（简单、快速）
- **MyBatis**：用于复杂查询和统计报表（灵活、可控）

---

### 2. 应用场景

#### 2.1 复杂查询场景

**商品服务**：
- 多条件动态查询（分类、价格区间、状态、可见性等）
- 商品统计报表（按分类统计、按时间统计）
- 商品排行榜（销量、点击量、评分）

**订单服务**：
- 订单统计报表（按状态统计、按时间统计、按用户统计）
- 订单趋势分析（日/周/月订单量）
- 订单金额统计（总收入、平均订单金额）

**用户服务**：
- 用户统计报表（注册用户数、活跃用户数）
- 用户行为分析（购买频率、消费金额）
- 用户排行榜（信用分、评分、交易量）

**消息服务**：
- 消息统计（未读数统计、消息发送量）
- 对话活跃度分析（按时间统计对话数）

#### 2.2 实现示例

**商品多条件动态查询**：
```xml
<!-- CommodityMapper.xml -->
<select id="findCommoditiesByConditions" resultType="CommodityDTO">
    SELECT c.*, u.nickname as sellerNickname, u.avatar as sellerAvatar
    FROM commodities c
    LEFT JOIN user_profiles u ON c.seller_id = u.user_id
    <where>
        <if test="category != null and category != ''">
            AND c.category = #{category}
        </if>
        <if test="minPrice != null">
            AND c.price >= #{minPrice}
        </if>
        <if test="maxPrice != null">
            AND c.price <= #{maxPrice}
        </if>
        <if test="status != null and status != ''">
            AND c.commodity_status = #{status}
        </if>
        <if test="sellerVisibility != null">
            AND c.seller_visibility = #{sellerVisibility}
        </if>
        <if test="buyerVisibility != null">
            AND c.buyer_visibility = #{buyerVisibility}
        </if>
    </where>
    ORDER BY c.publish_time DESC
    LIMIT #{offset}, #{size}
</select>
```

**订单统计报表**：
```xml
<!-- OrderMapper.xml -->
<select id="getOrderStatistics" resultType="OrderStatisticsDTO">
    SELECT 
        DATE(create_time) as date,
        COUNT(*) as totalOrders,
        SUM(total_amount) as totalAmount,
        AVG(total_amount) as avgAmount,
        COUNT(CASE WHEN status = 'PAID' THEN 1 END) as paidOrders,
        COUNT(CASE WHEN status = 'COMPLETED' THEN 1 END) as completedOrders
    FROM orders
    WHERE create_time >= #{startDate} AND create_time <= #{endDate}
    GROUP BY DATE(create_time)
    ORDER BY date DESC
</select>
```

---

### 3. MyBatis配置

#### 3.1 依赖配置
```xml
<!-- pom.xml -->
<dependency>
    <groupId>org.mybatis.spring.boot</groupId>
    <artifactId>mybatis-spring-boot-starter</artifactId>
    <version>3.0.3</version>
</dependency>
```

#### 3.2 配置项
```yaml
# application.yml
mybatis:
  mapper-locations: classpath:mapper/*.xml
  type-aliases-package: com.njumarket.njumarket.entity
  configuration:
    map-underscore-to-camel-case: true
    log-impl: org.apache.ibatis.logging.slf4j.Slf4jImpl
```

#### 3.3 Mapper接口
```java
@Mapper
public interface CommodityMapper {
    List<CommodityDTO> findCommoditiesByConditions(CommodityQueryDTO query);
    CommodityStatisticsDTO getCommodityStatistics(StatisticsQueryDTO query);
}
```

---

### 4. 学习重点

#### 4.1 动态SQL
- **if标签**：条件判断
- **choose/when/otherwise**：多条件选择
- **foreach标签**：循环处理
- **where标签**：自动处理WHERE子句
- **set标签**：自动处理SET子句

#### 4.2 结果映射
- **resultMap**：复杂对象映射
- **association**：一对一关联
- **collection**：一对多关联
- **discriminator**：鉴别器映射

#### 4.3 性能优化
- **批量操作**：批量插入、批量更新
- **分页查询**：PageHelper插件
- **缓存机制**：一级缓存、二级缓存

---

## 缓存机制实现

### 1. 多级缓存架构

#### 1.1 缓存层次设计

**三级缓存架构**：
```
L1: 本地缓存（Caffeine） → L2: Redis分布式缓存 → L3: 数据库
```

**缓存策略**：
- **热点数据**：L1 + L2（本地缓存 + Redis）
- **普通数据**：L2（Redis）
- **冷数据**：L3（数据库）

#### 1.2 缓存选型

**本地缓存（L1）**：
- **Caffeine**：高性能本地缓存
- **优点**：速度快、内存占用小
- **缺点**：无法跨服务共享
- **适用场景**：热点数据、配置信息

**分布式缓存（L2）**：
- **Redis**：分布式缓存
- **优点**：跨服务共享、支持复杂数据结构
- **缺点**：网络延迟
- **适用场景**：用户信息、商品信息、订单信息

---

### 2. 缓存实现方案

#### 2.1 Spring Cache + Caffeine + Redis

**配置示例**：
```java
@Configuration
@EnableCaching
public class CacheConfig {
    
    // L1: 本地缓存（Caffeine）
    @Bean
    public CacheManager localCacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        cacheManager.setCaffeine(Caffeine.newBuilder()
            .maximumSize(1000)
            .expireAfterWrite(5, TimeUnit.MINUTES)
            .recordStats());
        return cacheManager;
    }
    
    // L2: 分布式缓存（Redis）
    @Bean
    public CacheManager redisCacheManager(RedisConnectionFactory factory) {
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofMinutes(30))
            .serializeKeysWith(RedisSerializationContext.SerializationPair
                .fromSerializer(new StringRedisSerializer()))
            .serializeValuesWith(RedisSerializationContext.SerializationPair
                .fromSerializer(new GenericJackson2JsonRedisSerializer()));
        
        return RedisCacheManager.builder(factory)
            .cacheDefaults(config)
            .build();
    }
}
```

**使用示例**：
```java
@Service
public class CommodityService {
    
    // L1缓存：热点商品（本地缓存）
    @Cacheable(value = "commodity:hot", cacheManager = "localCacheManager")
    public CommodityDTO getHotCommodity(String commodityId) {
        return getCommodityFromRedis(commodityId);
    }
    
    // L2缓存：普通商品（Redis）
    @Cacheable(value = "commodity", cacheManager = "redisCacheManager")
    public CommodityDTO getCommodity(String commodityId) {
        return commodityRepository.findById(commodityId)
            .map(this::convertToDTO)
            .orElse(null);
    }
}
```

---

### 3. 缓存一致性策略

#### 3.1 Cache Aside模式

**读流程**：
1. 先查缓存，命中则返回
2. 缓存未命中，查数据库
3. 将结果写入缓存

**写流程**：
1. 更新数据库
2. 删除缓存（不更新缓存，让下次查询自动缓存）

**优点**：
- 实现简单
- 缓存和数据库解耦

**缺点**：
- 可能出现缓存不一致（极小概率）

#### 3.2 Read/Write Through模式

**读流程**：
1. 先查缓存，命中则返回
2. 缓存未命中，查数据库并写入缓存

**写流程**：
1. 更新缓存
2. 更新数据库

**优点**：
- 缓存一致性更好

**缺点**：
- 实现复杂
- 写操作需要同时更新缓存和数据库

#### 3.3 Write Behind模式

**写流程**：
1. 更新缓存
2. 异步更新数据库

**优点**：
- 写性能好

**缺点**：
- 数据可能丢失
- 实现复杂

**推荐**：使用Cache Aside模式（简单、可靠）

---

### 4. 缓存预热

#### 4.1 预热策略

**应用启动时预热**：
- 加载热点商品到本地缓存
- 加载热门用户信息到Redis
- 加载配置信息到本地缓存

**定时预热**：
- 每小时刷新热门商品缓存
- 每天刷新统计数据缓存

**实现示例**：
```java
@Component
public class CacheWarmUp {
    
    @PostConstruct
    public void warmUpCache() {
        // 预热热点商品
        List<String> hotCommodityIds = getHotCommodityIds();
        hotCommodityIds.forEach(id -> {
            commodityService.getHotCommodity(id);
        });
        
        // 预热用户信息
        List<String> activeUserIds = getActiveUserIds();
        activeUserIds.forEach(id -> {
            userService.getUserProfile(id);
        });
    }
}
```

---

### 5. 缓存穿透、击穿、雪崩

#### 5.1 缓存穿透

**问题**：查询不存在的数据，每次都查数据库

**解决方案**：
- **布隆过滤器**：快速判断数据是否存在
- **空值缓存**：将空结果也缓存，设置较短TTL

**实现示例**：
```java
@Cacheable(value = "commodity", unless = "#result == null")
public CommodityDTO getCommodity(String commodityId) {
    CommodityDTO dto = commodityRepository.findById(commodityId)
        .map(this::convertToDTO)
        .orElse(null);
    
    // 空值也缓存，防止穿透
    if (dto == null) {
        // 缓存空值，TTL较短（5分钟）
        cacheManager.getCache("commodity").put(commodityId, new EmptyCommodityDTO());
    }
    
    return dto;
}
```

#### 5.2 缓存击穿

**问题**：热点数据过期，大量请求同时查数据库

**解决方案**：
- **分布式锁**：只允许一个线程查数据库
- **热点数据永不过期**：后台异步更新

**实现示例**：
```java
public CommodityDTO getCommodity(String commodityId) {
    CommodityDTO cached = cacheManager.getCache("commodity").get(commodityId, CommodityDTO.class);
    if (cached != null) {
        return cached;
    }
    
    // 使用分布式锁，防止击穿
    RLock lock = redisson.getLock("commodity:lock:" + commodityId);
    try {
        lock.lock(10, TimeUnit.SECONDS);
        // 双重检查
        cached = cacheManager.getCache("commodity").get(commodityId, CommodityDTO.class);
        if (cached != null) {
            return cached;
        }
        
        // 查数据库
        CommodityDTO dto = commodityRepository.findById(commodityId)
            .map(this::convertToDTO)
            .orElse(null);
        
        // 写入缓存
        cacheManager.getCache("commodity").put(commodityId, dto);
        return dto;
    } finally {
        lock.unlock();
    }
}
```

#### 5.3 缓存雪崩

**问题**：大量缓存同时过期，大量请求查数据库

**解决方案**：
- **随机TTL**：避免同时过期
- **多级缓存**：本地缓存 + Redis
- **熔断降级**：数据库压力大时降级

**实现示例**：
```java
@Cacheable(value = "commodity", cacheManager = "redisCacheManager")
public CommodityDTO getCommodity(String commodityId) {
    // TTL随机化，避免雪崩
    RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
        .entryTtl(Duration.ofMinutes(30 + new Random().nextInt(10))); // 30-40分钟随机
    
    // ...
}
```

---

### 6. 缓存监控

#### 6.1 缓存命中率监控

**指标**：
- 缓存命中率
- 缓存QPS
- 缓存大小
- 缓存过期时间

**实现**：
- Caffeine提供统计功能
- Redis提供INFO命令查看统计

#### 6.2 缓存性能监控

**指标**：
- 缓存响应时间
- 缓存错误率
- 缓存内存使用率

**工具**：
- Spring Boot Actuator
- Prometheus + Grafana

---

## 搜索引擎集成

### 1. Elasticsearch

#### 1.1 技术选型
**Elasticsearch 8.x**
- 全文搜索引擎
- 支持分布式搜索
- 支持实时搜索

#### 1.2 应用场景
- **商品搜索**：商品标题、描述全文搜索
- **消息搜索**：消息内容全文搜索
- **用户搜索**：用户昵称搜索

#### 1.3 实现内容
- **索引设计**：商品索引、消息索引
- **分词器**：IK分词器（中文分词）
- **搜索高亮**：搜索结果高亮显示
- **搜索建议**：搜索自动补全
- **搜索历史**：用户搜索历史记录

---

### 2. 搜索服务设计

#### 2.1 商品搜索服务
**功能**：
- 商品索引创建和更新
- 商品全文搜索
- 商品搜索建议
- 商品搜索历史

**技术栈**：
- Spring Boot
- Elasticsearch Client
- IK分词器

#### 2.2 消息搜索服务
**功能**：
- 消息索引创建和更新
- 消息全文搜索
- 消息搜索高亮

**技术栈**：
- Spring Boot
- Elasticsearch Client
- IK分词器

---

## 技术学习重点

### 1. 微服务架构
- **服务拆分原则**：如何合理拆分服务
- **服务治理**：服务注册、发现、配置、监控
- **服务通信**：同步通信、异步通信
- **服务监控**：服务健康检查、性能监控

### 2. Spring Cloud
- **服务注册与发现**：Eureka / Nacos
- **配置中心**：Spring Cloud Config / Nacos Config
- **API网关**：Spring Cloud Gateway
- **服务调用**：OpenFeign
- **熔断降级**：Hystrix / Sentinel

### 3. 分布式系统
- **分布式事务**：Seata（AT、TCC、SAGA模式）
- **分布式锁**：Redis分布式锁优化
- **分布式ID**：Snowflake算法
- **CAP理论**：一致性、可用性、分区容错性

### 4. 消息队列
- **消息模型**：点对点、发布订阅
- **消息可靠性**：消息确认、消息重试、消息持久化
- **消息顺序**：顺序消息实现
- **消息幂等性**：防止重复消费

### 5. 搜索引擎
- **Elasticsearch**：索引设计、查询DSL
- **全文搜索**：IK分词器、搜索高亮
- **搜索优化**：搜索建议、搜索历史

### 6. MyBatis
- **动态SQL**：if、choose、foreach等标签
- **结果映射**：resultMap、association、collection
- **性能优化**：批量操作、分页查询、缓存机制

### 7. 缓存系统
- **多级缓存**：本地缓存（Caffeine）+ 分布式缓存（Redis）
- **缓存一致性**：Cache Aside、Read/Write Through模式
- **缓存问题**：穿透、击穿、雪崩的解决方案
- **缓存预热**：应用启动预热、定时预热

---

## 实施计划

### Phase 1: 微服务拆分（2-3周）

#### Week 1: 服务拆分准备
- [ ] 设计微服务拆分方案
- [ ] 搭建Spring Cloud基础环境（Nacos、Gateway）
- [ ] 创建各微服务项目骨架
- [ ] 配置服务注册与发现

#### Week 2: 核心服务拆分
- [ ] 拆分用户服务（User Service）
- [ ] 拆分商品服务（Commodity Service）
- [ ] 拆分订单服务（Order Service）
- [ ] 服务间通信测试（OpenFeign）

#### Week 3: 消息和通知服务拆分
- [ ] 拆分消息服务（Message Service）
- [ ] 拆分通知服务（Notification Service）
- [ ] 服务间通信完善
- [ ] 集成测试

---

### Phase 2: Spring Cloud生态集成（2-3周）

#### Week 4: 配置中心和API网关
- [ ] Nacos Config配置中心集成
- [ ] Spring Cloud Gateway API网关配置
- [ ] 路由规则配置
- [ ] 请求过滤和限流

#### Week 5: 服务调用和熔断
- [ ] OpenFeign服务调用配置
- [ ] Sentinel熔断降级集成
- [ ] 负载均衡配置
- [ ] 服务监控配置

#### Week 6: 分布式系统实践
- [ ] Seata分布式事务集成
- [ ] 分布式锁优化（Redisson）
- [ ] 分布式ID生成器实现
- [ ] 分布式系统测试

---

### Phase 3: 消息队列和搜索引擎（2-3周）

#### Week 7: 消息队列集成
- [ ] RabbitMQ安装和配置
- [ ] 订单异步处理实现
- [ ] 消息可靠性保证
- [ ] 消息队列监控

#### Week 8: Elasticsearch集成
- [ ] Elasticsearch安装和配置
- [ ] 商品搜索服务实现
- [ ] 消息搜索服务实现
- [ ] IK分词器配置

#### Week 9: MyBatis和缓存机制
- [ ] MyBatis集成和配置
- [ ] 复杂查询Mapper实现
- [ ] 统计报表查询实现
- [ ] 多级缓存架构设计
- [ ] 缓存一致性策略实现
- [ ] 缓存预热和监控

#### Week 10: 系统集成和测试
- [ ] 各服务集成测试
- [ ] 性能测试
- [ ] 压力测试
- [ ] 缓存性能测试
- [ ] 文档完善

---

## 预期成果

### 1. 架构成果
- ✅ 5个核心微服务独立部署和运行
- ✅ Spring Cloud生态完整集成
- ✅ API网关统一入口
- ✅ 服务注册与发现正常工作
- ✅ 配置中心动态配置生效

### 2. 功能成果
- ✅ 分布式事务保证数据一致性
- ✅ 消息队列实现异步处理
- ✅ Elasticsearch实现全文搜索
- ✅ MyBatis实现复杂查询和统计报表
- ✅ 多级缓存提升系统性能
- ✅ 服务熔断降级保证系统稳定性

### 3. 技术成果
- ✅ 深入理解微服务架构
- ✅ 掌握Spring Cloud生态
- ✅ 理解分布式系统原理
- ✅ 掌握消息队列使用
- ✅ 掌握搜索引擎集成
- ✅ 掌握MyBatis复杂查询
- ✅ 掌握多级缓存架构设计

### 4. 文档成果
- ✅ 微服务架构设计文档
- ✅ 服务拆分方案文档
- ✅ Spring Cloud集成文档
- ✅ 分布式系统实践文档
- ✅ 消息队列应用文档
- ✅ 搜索引擎集成文档
- ✅ MyBatis复杂查询实践文档
- ✅ 缓存机制实现文档

---

## 技术难点与解决方案

### 1. 服务拆分难点
**难点**：如何合理拆分服务，避免过度拆分或拆分不足
**解决方案**：
- 按业务领域拆分，保持服务职责单一
- 考虑数据独立性，避免跨服务事务
- 考虑服务间通信成本，避免频繁调用

### 2. 分布式事务难点
**难点**：跨服务事务如何保证一致性
**解决方案**：
- 使用Seata AT模式，自动事务管理
- 对于复杂场景，使用TCC模式
- 对于长事务，使用SAGA模式

### 3. 服务间通信难点
**难点**：服务间通信的可靠性和性能
**解决方案**：
- 同步通信使用OpenFeign，支持重试和降级
- 异步通信使用消息队列，保证可靠性
- 使用缓存减少服务间调用

### 4. 数据一致性难点
**难点**：分布式环境下数据一致性保证
**解决方案**：
- 使用分布式事务（Seata）
- 使用最终一致性（消息队列）
- 使用分布式锁（Redis）

---

## 风险评估与应对

### 1. 技术风险
**风险**：Spring Cloud学习曲线陡峭
**应对**：
- 分阶段实施，先实现基础功能
- 参考官方文档和最佳实践
- 遇到问题及时查阅资料

### 2. 性能风险
**风险**：微服务拆分后性能可能下降
**应对**：
- 服务间通信使用缓存减少调用
- 异步处理减少同步调用
- 合理使用消息队列

### 3. 复杂度风险
**风险**：微服务架构复杂度增加
**应对**：
- 完善的文档和注释
- 清晰的代码组织
- 统一的开发规范

---

## 总结

v2.0 阶段将通过微服务架构改造，将系统从单体应用升级为微服务架构，学习Spring Cloud生态，实现分布式系统实践。这将是一个重要的技术提升阶段，为后续的系统扩展和优化打下坚实基础。

**项目状态**：📋 **v2.0 阶段规划完成，准备开始实施**

---

**文档版本**：v2.0 规划  
**最后更新**：2025-01-XX

