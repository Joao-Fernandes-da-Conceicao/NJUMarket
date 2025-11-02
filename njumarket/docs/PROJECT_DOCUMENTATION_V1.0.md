# 南大集市 NJUMarket v1.0 项目文档

## 📋 目录
- [项目概述](#项目概述)
- [技术架构](#技术架构)
- [功能模块](#功能模块)
- [部署指南](#部署指南)
- [提升建议](#提升建议)
  - [可用性建议](#可用性建议)
  - [并发性建议](#并发性建议)
  - [功能性建议](#功能性建议)
  - [维护性建议](#维护性建议)
- [已知限制](#已知限制)
- [后续规划](#后续规划)

---

## 项目概述

**NJUMarket** 是一个面向南大校园的二手交易平台**学习项目**，采用前后端分离架构，实现了完整的“发布-浏览-下单-支付-发货-收货-退款/退货-消息联系”业务闭环。本项目旨在通过实践学习 Spring Boot、Vue 3、JPA、缓存、并发控制等核心技术，不涉及真实的第三方服务集成（如短信、邮件、支付网关）。

### 项目定位
- **性质**: 学习项目（Learning Project）
- **目标**: 技术学习与实践，非生产部署
- **范围**: 核心业务逻辑实现 + 技术栈学习
- **不涉及**: 真实短信/邮件服务、真实支付系统、多语言国际化

### 版本信息
- **版本**: v1.0
- **状态**: 已完成核心功能，可用于学习演示
- **开发环境**:
  - 后端: `http://localhost:8080`
  - 用户前端: `http://localhost:8081`
  - 管理前端: `http://localhost:8082`

### 项目结构
```
NJUMarket/
├── njumarket/                          # 后端项目 (Spring Boot)
│   ├── src/main/java/com/njumarket/   # Java 源代码
│   │   ├── controller/                # 控制器层（用户/管理员分离）
│   │   ├── service/                   # 业务逻辑层
│   │   ├── repository/                # 数据访问层（JPA）
│   │   ├── entity/                    # 实体类
│   │   ├── dto/                       # 数据传输对象
│   │   └── config/                    # 配置类（拦截器、安全等）
│   ├── src/main/resources/
│   │   ├── database/                  # 数据库脚本
│   │   └── application.properties     # 应用配置
│   └── docs/                          # 项目文档
│
├── njumarket-front/NJUMarket/         # 用户前端项目 (Vue 3)
│   ├── src/
│   │   ├── views/                     # 页面组件
│   │   ├── components/                # 公共组件（统一组件系统）
│   │   ├── api/                       # API 接口封装
│   │   ├── router/                    # 路由配置
│   │   ├── utils/                     # 工具函数（业务规则等）
│   │   └── styles/                    # 统一样式
│   └── public/                        # 静态资源
│
└── njumarket-front-admin/my-vue3-app/ # 管理前端项目 (Vue 3)
    ├── src/
    │   ├── views/                     # 管理页面
    │   ├── components/                # 公共组件
    │   ├── api/admin/                 # 管理端 API
    │   └── router/                    # 路由配置
    └── public/
```

---

## 技术架构

### 后端技术栈
- **框架**: Spring Boot 3.2.0
- **Java 版本**: JDK 17
- **ORM**: Spring Data JPA (Hibernate)
- **数据库**: MySQL (推荐 8.0+)
- **安全**: JWT Token (管理端) + Session (用户端)
- **API 文档**: Swagger/OpenAPI 3
- **日志**: SLF4J + Logback
- **构建工具**: Maven

### 前端技术栈
#### 用户端
- **框架**: Vue 3 (Composition API)
- **路由**: Vue Router 4
- **状态管理**: Pinia
- **UI 组件库**: Element Plus
- **HTTP 客户端**: Axios
- **构建工具**: Vue CLI 5

#### 管理端
- **框架**: Vue 3
- **路由**: Vue Router 4
- **UI 组件库**: Element Plus
- **HTTP 客户端**: Axios
- **构建工具**: Vue CLI 5

### 架构特点
1. **前后端分离**: RESTful API，JSON 数据交互
2. **统一组件系统**: `UnifiedButton`、`UnifiedInput`、`UnifiedSelect`、`UnifiedTag`
3. **业务规则集中**: `utils/orderRules.js` 统一校验逻辑
4. **响应式设计**: 移动端与桌面端适配
5. **代码分层**: Controller → Service → Repository → Entity

---

## 功能模块

### 用户端功能

#### 1. 用户认证
- ✅ 用户名/手机号 + 密码登录
- ✅ 手机号 + 验证码登录（**模拟验证码**，学习项目无需真实短信服务）
- ✅ 用户注册
- ✅ 密码重置（基础流程）
- ✅ JWT Token 认证（Session 存储）

#### 2. 商品管理
- ✅ 商品发布（标题、描述、价格、库存、分类、成色、图片）
- ✅ 商品编辑、上下架
- ✅ 商品浏览、搜索、筛选（分类、成色、价格区间）
- ✅ 商品详情查看（图片轮播、卖家信息、联系卖家）
- ✅ 商品可见性控制（PUBLIC/PRIVATE/HIDDEN）
- ✅ 商品状态管理（DRAFT/PUBLISHED/ON_SHELF/OFF_SHELF）

#### 3. 订单管理
- ✅ 创建订单（数量、地址、备注）
- ✅ 订单支付（**模拟支付**，状态流转：CREATED → PAID，学习项目无需真实支付网关）
- ✅ 订单发货（SHIPPED）
- ✅ 确认收货（COMPLETED）
- ✅ 订单取消（CANCELLED）
- ✅ 退款申请与处理（REFUND_REQUESTED/APPROVED/REJECTED）
- ✅ 退货申请与处理（RETURN_REQUESTED/APPROVED/REJECTED/COMPLETED）
- ✅ 订单快照（商品信息快照，防止商品变更影响订单）
- ✅ 订单可见性控制（卖家/买家可见）

#### 4. 消息系统
- ✅ 会话列表（买卖双方对话列表，一个用户对只允许一个会话）
- ✅ 聊天窗口（消息发送、接收、多行输入）
- ✅ 消息历史加载
- ✅ 商品/订单卡片分享（消息中发送商品和订单卡片，支持实时数据获取）
- ✅ 消息对齐优化（对方消息左对齐，本方消息右对齐）
- ⚠️ WebSocket 实时推送（TODO）

#### 5. 用户中心
- ✅ 个人资料管理（昵称、头像、简介、联系方式、地区）
- ✅ 头像上传
- ✅ 我的商品管理
- ✅ 我的订单管理（买家/卖家视图分离）
- ✅ 信用评分、VIP 等级展示

### 管理端功能

#### 1. 管理员认证
- ✅ 管理员登录（JWT Token）
- ✅ 权限级别（system/administrator）
- ✅ 管理员信息展示（侧边栏）

#### 2. 数据统计
- ✅ 用户总数、商品总数、订单总数（实时查询）
- ⚠️ 消息统计（TODO）

#### 3. 用户管理
- ✅ 用户列表（分页、搜索、筛选、排序）
- ✅ 用户详情查看（包含 UserProfile）
- ✅ 用户状态管理（ACTIVE/SUSPENDED/BANNED）
- ✅ 用户信息编辑（完整字段编辑）
- ✅ 用户删除（软删除）

#### 4. 商品管理
- ✅ 商品列表（分页、搜索、筛选、排序）
  - 搜索：标题、卖家ID、卖家昵称（联表查询）
  - 筛选：分类、成色、状态、可见性
  - 排序：上架时间、点击量
- ✅ 商品详情查看（包含快照信息）
- ✅ 商品状态管理
- ✅ 商品信息编辑（完整字段编辑）
- ✅ 商品删除

#### 5. 订单管理
- ✅ 订单列表（分页、搜索、排序）
  - 搜索：买家ID、卖家ID、商品标题快照
  - 排序：创建时间
- ✅ 订单详情查看（展开显示完整信息）
- ✅ 订单状态修改
- ✅ 订单删除

#### 6. 消息管理
- ⚠️ TODO（前端占位，后端接口已实现）

---

## 部署指南

### 环境要求
- **Java**: JDK 17+
- **Node.js**: 16+
- **MySQL**: 8.0+
- **Maven**: 3.6+ (可选，IDEA 内置)

### 后端部署

1. **数据库初始化**
   ```bash
   mysql -u root -p
   CREATE DATABASE njumarket CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
   USE njumarket;
   # 执行 src/main/resources/database/init.sql
   ```

2. **配置文件**
   - 修改 `src/main/resources/application.properties`
   - 配置数据库连接、端口等

3. **启动服务**
   ```bash
   cd njumarket
   mvn spring-boot:run
   # 或使用 IDE 运行 NjumarketApplication.main()
   ```

### 前端部署

#### 用户端
```bash
cd njumarket-front/NJUMarket
npm install
npm run serve  # 开发环境 (localhost:8081)
npm run build  # 生产构建（dist/ 目录）
```

#### 管理端
```bash
cd njumarket-front-admin/my-vue3-app
npm install
npm run serve  # 开发环境 (localhost:8082)
npm run build  # 生产构建（dist/ 目录，部署到 /admin/ 子路径）
```

### 生产环境配置

1. **前端构建**
   - 用户端：静态资源托管到 Nginx/CDN
   - 管理端：构建时设置 `publicPath: '/admin/'`，部署到 `/admin/` 子路径

2. **后端部署**
   - 打包：`mvn clean package`
   - 运行：`java -jar target/njumarket-0.0.1-SNAPSHOT.jar`
   - 或使用 Docker/容器化部署

3. **Nginx 配置示例**
   ```nginx
   server {
       listen 80;
       server_name your-domain.com;
       
       # 用户端
       location / {
           root /var/www/njumarket-front/dist;
           try_files $uri $uri/ /index.html;
       }
       
       # 管理端
       location /admin {
           alias /var/www/njumarket-front-admin/dist;
           try_files $uri $uri/ /admin/index.html;
       }
       
       # API 代理
       location /api {
           proxy_pass http://localhost:8080;
           proxy_set_header Host $host;
           proxy_set_header X-Real-IP $remote_addr;
       }
   }
   ```

---

## 提升建议

### 可用性建议

#### 1. 错误处理与用户提示
- **问题**: 部分 API 错误未在前端友好展示
- **建议**:
  - 统一错误拦截器，将 HTTP 错误码映射为中文提示
  - 表单验证提示使用 Element Plus 的 `ElMessage` 或 `ElNotification`
  - 网络异常时提示“请检查网络连接”而非原始错误信息
  - 关键操作（删除、状态变更）增加确认对话框

#### 2. 加载状态优化
- **问题**: 列表加载、提交操作缺少加载指示
- **建议**:
  - 使用 `el-table` 的 `v-loading` 指令
  - 按钮提交时显示 `loading` 状态，禁用重复提交
  - 大图片上传显示进度条

#### 3. 数据验证增强
- **问题**: 部分输入缺少实时校验与格式提示
- **建议**:
  - 手机号格式校验（11位数字）
  - 价格/库存非负数校验（前端 + 后端）
  - 图片格式与大小限制提示
  - 地址长度、备注长度限制

#### 4. 空状态展示
- **问题**: 空列表、空搜索结果缺少友好提示
- **建议**:
  - 使用 Element Plus 的 `el-empty` 组件
  - 提示文案如“暂无商品，快去发布吧！”“未找到相关商品”

#### 5. 响应式优化
- **问题**: 部分页面在移动端体验待优化
- **建议**:
  - 表格在小屏时改为卡片列表
  - 管理端分页器在移动端简化（隐藏页码，只保留上一页/下一页）
  - 图片轮播支持触摸滑动

#### 6. 无障碍访问
- **问题**: 缺少 ARIA 标签与键盘导航支持
- **建议**:
  - 为按钮、输入框添加 `aria-label`
  - 关键操作支持键盘快捷键
  - 颜色对比度符合 WCAG 标准

---

### 并发性建议

#### 1. 数据库连接池优化（学习重点）
- **学习价值**: 理解连接池原理、连接泄漏检测、性能调优
- **问题**: 默认连接池配置可能不适合高并发
- **建议**:
  ```properties
  # application.properties
  spring.datasource.hikari.maximum-pool-size=20
  spring.datasource.hikari.minimum-idle=5
  spring.datasource.hikari.connection-timeout=30000
  spring.datasource.hikari.idle-timeout=600000
  spring.datasource.hikari.max-lifetime=1800000
  # 学习：开启连接泄漏检测
  spring.datasource.hikari.leak-detection-threshold=60000
  ```
- **学习点**:
  - 连接池大小如何设置（CPU 核心数 × 2 + 磁盘数）
  - 连接泄漏的检测与排查方法
  - 监控连接池使用情况（JMX 或 HikariCP 内置监控）

#### 2. JPA 查询优化
- **问题**: 部分 N+1 查询、联表查询性能待优化
- **建议**:
  - 使用 `@EntityGraph` 或 `JOIN FETCH` 避免 N+1
  - 商品列表查询卖家信息时批量查询（`IN` 查询）
  - 为常用查询字段添加数据库索引（`accountStatus`、`commodityStatus`、`orderStatus`、`publishTime`）

#### 4. 缓存机制与缓存一致性（学习重点）
- **学习价值**: 理解缓存穿透、缓存击穿、缓存雪崩、缓存一致性策略
- **建议**:
  - 使用 Spring Cache（Redis）缓存热点数据
    - 商品分类列表（缓存 1 小时）
    - 用户基本信息（UserProfile，缓存 15 分钟）
    - 统计数据（缓存 5 分钟）
  
- **缓存一致性保证（学习重点）**:
  ```java
  // 1. Cache-Aside 模式（旁路缓存）
  @Cacheable(value = "categories", unless = "#result == null")
  public List<Category> getCategories() { /* 查询数据库 */ }
  
  // 2. 更新时删除缓存（双写策略）
  @CacheEvict(value = "categories", allEntries = true)
  public void updateCategory(Category category) { /* 更新数据库并删除缓存 */ }
  
  // 3. 使用分布式锁防止缓存击穿
  @Cacheable(value = "user", key = "#userId", 
             cacheManager = "redisCacheManager")
  public User getUser(String userId) {
      // 如果缓存未命中，使用 Redis 分布式锁防止并发查询数据库
      String lockKey = "lock:user:" + userId;
      // 使用 Redisson 或 RedisTemplate 实现分布式锁
  }
  ```
  
- **学习点**:
  - **缓存穿透**: 查询不存在的数据 → 解决方案：布隆过滤器、缓存空值
  - **缓存击穿**: 热点 key 过期 → 解决方案：分布式锁、互斥锁
  - **缓存雪崩**: 大量 key 同时过期 → 解决方案：随机过期时间、多级缓存
  - **缓存一致性策略**: 
    - 先更新数据库，再删除缓存（推荐）
    - 使用消息队列异步更新缓存（最终一致性）
    - 使用 Canal 监听数据库 binlog 自动更新缓存（学习高级方案）

#### 5. 分页查询优化（学习重点）
- **学习价值**: 理解 `OFFSET` 分页的性能问题、学习游标分页实现
- **问题**: 大数据量时使用 `OFFSET` 分页性能差（深度分页问题）
- **建议**:
  - **游标分页（Cursor-based Pagination）**:
    ```java
    // 传统 OFFSET 分页（深度分页时性能差）
    Pageable pageable = PageRequest.of(page, size);
    
    // 游标分页（推荐用于时间排序场景）
    Specification<Order> spec = (root, query, cb) -> {
        if (cursorTime != null) {
            return cb.lessThan(root.get("createTime"), cursorTime);
        }
        return cb.conjunction();
    };
    Pageable pageable = PageRequest.of(0, size, Sort.by("createTime").descending());
    ```
  - 前端传递上一页最后一条记录的 `createTime` 作为游标
- **学习点**:
  - 理解 `OFFSET` 的性能问题（需要扫描和跳过大量数据）
  - 游标分页的优缺点（无法跳页，但性能好）
  - 何时使用 `OFFSET` vs 游标分页

#### 6. 接口限流与防刷（学习重点）
- **学习价值**: 理解限流算法（令牌桶、漏桶、滑动窗口）、实现防刷机制
- **问题**: 缺少接口限流，可能被恶意请求压垮
- **建议**:
  - **方案1: Guava RateLimiter（单机限流）**
    ```java
    @Component
    public class RateLimiterService {
        private final RateLimiter loginLimiter = RateLimiter.create(5.0 / 60); // 5次/分钟
        private final RateLimiter searchLimiter = RateLimiter.create(30.0 / 60);
        
        public boolean tryAcquire(String type) {
            return switch (type) {
                case "login" -> loginLimiter.tryAcquire();
                case "search" -> searchLimiter.tryAcquire();
                default -> true;
            };
        }
    }
    ```
  
  - **方案2: Redis + Lua（分布式限流，推荐学习）**
    ```lua
    -- limit.lua: 滑动窗口限流
    local key = KEYS[1]
    local window = tonumber(ARGV[1])  -- 时间窗口（秒）
    local limit = tonumber(ARGV[2])   -- 限制次数
    local current = redis.call('INCR', key)
    if current == 1 then
        redis.call('EXPIRE', key, window)
    end
    return current <= limit and 1 or 0
    ```
    - 登录接口：5次/分钟/IP
    - 搜索接口：30次/分钟/用户
    - 列表接口：60次/分钟/用户
  
- **学习点**:
  - 令牌桶 vs 漏桶算法的区别与应用场景
  - 滑动窗口限流的实现原理
  - 分布式限流 vs 单机限流的选择

#### 7. 异步处理与线程池（学习重点）
- **学习价值**: 理解异步编程、线程池调优、CompletableFuture 使用
- **问题**: 同步处理耗时操作（如发送消息、生成统计数据）
- **建议**:
  ```java
  @Configuration
  @EnableAsync
  public class AsyncConfig {
      @Bean(name = "taskExecutor")
      public ThreadPoolTaskExecutor taskExecutor() {
          ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
          executor.setCorePoolSize(5);
          executor.setMaxPoolSize(20);
          executor.setQueueCapacity(100);
          executor.setThreadNamePrefix("async-");
          executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
          executor.initialize();
          return executor;
      }
  }
  
  @Async("taskExecutor")
  public CompletableFuture<Void> sendMessage(Message message) {
      // 异步发送消息
      return CompletableFuture.completedFuture(null);
  }
  ```
- **学习点**:
  - 线程池参数调优（核心线程数、最大线程数、队列容量）
  - 拒绝策略的选择（AbortPolicy、CallerRunsPolicy、DiscardPolicy）
  - `@Async` 的异常处理（`AsyncUncaughtExceptionHandler`）
  - CompletableFuture 的使用（链式调用、异常处理）

#### 8. 数据库事务管理与并发控制（学习重点）
- **学习价值**: 理解事务隔离级别、悲观锁、乐观锁、死锁检测
- **建议**:
  ```java
  // 1. 事务隔离级别设置
  @Transactional(isolation = Isolation.READ_COMMITTED)
  public void updateOrderStatus(String orderId, String status) {
      // 默认使用数据库隔离级别，可显式设置
  }
  
  // 2. 悲观锁（Pessimistic Locking）
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT o FROM Order o WHERE o.orderId = :orderId")
  Optional<Order> findByIdWithLock(@Param("orderId") String orderId);
  
  // 3. 乐观锁（Optimistic Locking）- 使用版本号
  @Entity
  public class Order {
      @Version
      private Long version;  // 乐观锁版本号
      // ...
  }
  
  // 4. 分布式锁（使用 Redisson）
  @Autowired
  private RedissonClient redissonClient;
  
  public void processOrder(String orderId) {
      RLock lock = redissonClient.getLock("order:" + orderId);
      try {
          if (lock.tryLock(10, 30, TimeUnit.SECONDS)) {
              // 处理订单逻辑
          }
      } finally {
          lock.unlock();
      }
  }
  ```
- **学习点**:
  - 事务隔离级别：READ_UNCOMMITTED、READ_COMMITTED、REPEATABLE_READ、SERIALIZABLE
  - 悲观锁 vs 乐观锁的选择与应用场景
  - 死锁的检测与预防
  - 分布式锁的实现（Redis、Zookeeper、数据库）

---

### 功能性建议

#### 1. 消息系统增强
- **实时推送**: 集成 WebSocket（Spring WebSocket 或 Socket.io）
  - 消息送达、已读状态同步
  - 未读消息数实时更新
  - 断线重连机制
- **消息类型**: 支持图片、文件、系统通知
- **消息搜索**: 按关键词、时间范围搜索历史消息

#### 2. 商品功能扩展
- **商品推荐**: 基于浏览历史、相似商品的推荐算法
- **商品收藏**: 用户收藏夹功能
- **商品举报**: 举报机制与审核流程
- **商品评价**: 交易完成后评价系统
- **商品标签**: 多标签系统（“急售”、“九成新”、“包邮”等）

#### 3. 订单功能扩展
- **批量下单**: 购物车功能，支持多商品合并下单
- **订单拆分**: 多商品订单支持部分发货
- **物流跟踪**: 集成第三方物流 API（如快递100）
- **订单导出**: 支持导出订单列表为 Excel
- **订单统计**: 用户交易统计（月销售额、成交率等）

#### 4. 支付系统（学习项目：模拟实现）
- **说明**: 学习项目无需集成真实支付网关，但可以学习支付系统的设计思路
- **学习建议**:
  - 支付状态机设计（CREATED → PAID → REFUNDED）
  - 支付回调处理机制（幂等性保证）
  - 支付日志记录与对账逻辑（学习金融系统的可靠性设计）
  - **不实现**: 真实支付宝/微信支付 SDK 集成

#### 5. 搜索功能增强
- **全文搜索**: 使用 Elasticsearch 实现商品全文搜索
  - 支持标题、描述全文检索
  - 支持拼音搜索、模糊匹配
- **搜索建议**: 自动补全、热门搜索词
- **搜索历史**: 记录用户搜索历史

#### 6. 通知系统（学习项目：站内通知为主）
- **站内通知**: 订单状态变更、消息提醒（**实现**，学习事件驱动架构）
  ```java
  // 使用 Spring 事件机制
  @EventListener
  public void handleOrderStatusChanged(OrderStatusChangedEvent event) {
      // 发送站内通知
  }
  ```
- **邮件通知**: **模拟实现**（学习邮件模板、异步发送机制）
  - 可使用 Spring Mail 的 Mock 实现或本地邮件服务器（如 GreenMail）
  - **不实现**: 真实邮件服务集成（如阿里云邮件、SendGrid）
- **短信通知**: **模拟实现**（学习验证码生成、限流机制）
  - 验证码生成与存储（Redis 缓存，5 分钟过期）
  - **不实现**: 真实短信服务集成（如阿里云短信、腾讯云短信）

#### 7. 数据分析
- **用户行为**: 浏览记录、点击热力图
- **商品分析**: 热门商品、滞销商品分析
- **交易分析**: 日/月交易额、成交率统计
- **可视化报表**: 使用 ECharts 展示数据

#### 8. 安全增强（学习重点）
- **图片上传安全**: 
  - 文件类型校验（白名单：jpg、png、gif）
  - 文件大小限制（前端 + 后端双重校验）
  - **学习点**: 文件内容检测（魔数判断，而非仅扩展名）
  - **不实现**: 图片内容安全检查（色情/暴力识别，需第三方 AI 服务）
- **敏感信息脱敏**: 手机号、邮箱部分隐藏（学习隐私保护）
  ```java
  public String maskPhone(String phone) {
      return phone.replaceAll("(\\d{3})\\d{4}(\\d{4})", "$1****$2");
  }
  ```
- **操作日志（审计日志）**: 记录关键操作（学习合规性设计）
  ```java
  @Aspect
  @Component
  public class AuditLogAspect {
      @After("@annotation(AuditLog)")
      public void logOperation(JoinPoint joinPoint) {
          // 记录操作者、操作类型、操作时间、IP 地址
      }
  }
  ```
- **SQL 注入防护**: 学习参数化查询（JPA 已内置）
- **XSS 防护**: 学习输入转义、CSP 策略

---

### 维护性建议

#### 1. 代码规范与文档
- **问题**: 部分方法缺少注释，业务逻辑复杂
- **建议**:
  - 统一代码注释规范（JavaDoc、JSDoc）
  - API 文档完善（Swagger 注解完整）
  - 业务规则文档化（`docs/BUSINESS_RULES.md`）
  - 数据库设计文档（ER 图、字段说明）

#### 2. 单元测试与集成测试
- **问题**: 缺少测试覆盖，重构风险大
- **建议**:
  - 关键业务逻辑单元测试（JUnit 5）
    - `orderRules.js` 的校验逻辑
    - Service 层的业务规则
    - Repository 的复杂查询
  - 集成测试（Spring Boot Test）
    - API 端点测试
    - 数据库事务测试
  - 前端组件测试（Vue Test Utils）
    - 统一组件的渲染与交互
    - 业务组件的核心逻辑

#### 3. 日志系统优化
- **问题**: 日志级别不统一，关键操作缺少日志
- **建议**:
  - 统一日志格式（JSON 格式，便于 ELK 分析）
  - 关键操作记录操作日志（用户ID、操作类型、参数）
  - 错误日志包含堆栈信息与上下文
  - 日志分级：DEBUG（开发）、INFO（业务操作）、WARN（异常但可恢复）、ERROR（系统错误）

#### 4. 配置管理
- **问题**: 配置硬编码，环境切换不便
- **建议**:
  - 使用 `application.yml` 多环境配置（dev/test/prod）
  - 敏感信息使用环境变量或配置中心
  - 前端配置抽离（API 地址、功能开关）

#### 5. 代码重构建议
- **前端组件解耦**:
  - 大型页面（如 `CommodityList.vue`）进一步拆分为更小的业务组件
  - 状态管理统一使用 Pinia（减少 props drilling）
- **后端服务拆分**:
  - 考虑按业务域拆分 Service（UserService、CommodityService、OrderService、MessageService）
  - 复杂业务逻辑抽取为独立的业务规则类（如 `OrderValidator`、`PaymentProcessor`）

#### 8. 消息卡片组件优化（v1.0 更新）
**现状**:
- 已实现消息中的商品卡片（`CommodityCard.vue`）和订单卡片（`OrderCard.vue`）
- 支持实时 profile 数据获取，确保显示最新的用户昵称和头像
- 响应式布局：桌面端最大宽度 300px，移动端自适应缩小
- 对齐机制：对方消息左对齐，本方消息右对齐

**建议**:
- **组件复用性**:
  - `CommodityCard` 和 `OrderCard` 可在其他场景复用（如商品列表、订单详情）
  - 考虑抽取为更通用的 `MessageCard` 组件族，支持不同类型的卡片展示
- **Profile 数据缓存**:
  - 当前每个卡片独立请求 profile，可能造成重复请求
  - 建议在父组件（`ChatWindow.vue`）或全局状态中缓存已获取的 profile 数据
  - 使用 Map 结构存储 `userId -> profile` 映射，避免重复请求
- **响应式设计**:
  - 当前使用固定 `max-width: 300px`，在超小屏设备上可能需要进一步优化
  - 建议使用 CSS 变量定义卡片尺寸，便于全局调整
  - 考虑卡片内容的自适应（如文字截断、图片压缩）
- **性能优化**:
  - 卡片组件在消息列表中的频繁渲染可能影响性能
  - 考虑使用 `v-memo` 指令缓存卡片渲染结果（Vue 3.2+）
  - 或使用虚拟滚动（Virtual Scrolling）优化长消息列表
- **样式一致性**:
  - 当前卡片样式独立管理，建议抽取为共享样式类
  - 统一卡片的基础样式（边框、圆角、间距）到全局样式文件
  - 使用 CSS 变量管理卡片主题色和尺寸

#### 6. 依赖管理
- **问题**: 依赖版本未固定，可能导致构建不一致
- **建议**:
  - Maven: 使用 `dependencyManagement` 统一版本
  - NPM: 使用 `package-lock.json` 锁定版本（已启用）
  - 定期更新依赖，关注安全漏洞（npm audit、Dependabot）

#### 7. 错误监控与追踪
- **问题**: 生产环境错误难以追踪
- **建议**:
  - 集成错误监控服务（Sentry、日志聚合服务）
  - 前端错误捕获（Vue 全局错误处理）
  - 后端异常统一处理（`@ControllerAdvice`）

#### 8. 数据库迁移
- **问题**: 数据库变更缺少版本控制
- **建议**:
  - 使用 Flyway 或 Liquibase 管理数据库迁移
  - 每次数据库变更生成迁移脚本
  - 测试环境验证迁移脚本

#### 9. CI/CD 流程
- **问题**: 手动部署，易出错
- **建议**:
  - 集成 GitHub Actions / GitLab CI
  - 自动化测试（单元测试、集成测试）
  - 自动化构建与部署（Docker 镜像构建、部署到测试/生产环境）

#### 10. 代码审查与版本管理
- **建议**:
  - 使用 Git Flow 分支模型（main/develop/feature/hotfix）
  - 代码审查流程（Pull Request）
  - 提交信息规范（Conventional Commits）

---

## 已知限制

### v1.0 版本限制与学习项目特点
1. **消息实时性**: 消息系统基于 HTTP 轮询，无 WebSocket 实时推送（**学习点：WebSocket 实现**）
2. **支付系统**: 模拟支付，未集成真实支付网关（**学习项目：无需真实集成**）
3. **图片存储**: 本地文件系统存储，未使用对象存储（**学习点：OSS 集成方案**）
4. **搜索功能**: 基于数据库 LIKE 查询，未使用全文搜索引擎（**学习点：Elasticsearch 集成**）
5. **缓存机制**: 未实现缓存，所有查询直连数据库（**学习重点：缓存一致性保证**）
6. **测试覆盖**: 缺少单元测试与集成测试（**学习重点：测试驱动开发**）
7. **国际化**: 仅支持中文，无多语言支持（**学习项目：无需国际化**）
8. **第三方服务**: 不集成真实短信、邮件、支付服务（**学习项目：模拟实现即可**）

---

## 后续规划

### v1.1 规划（学习重点：并发与缓存）
- ✅ **Redis 缓存集成与缓存一致性保证**（重点学习）
  - Cache-Aside 模式实现
  - 缓存穿透/击穿/雪崩防护
  - 分布式锁实现（Redisson）
- ✅ **数据库索引优化**（学习索引设计）
- ✅ **事务管理与并发控制**（学习隔离级别、乐观锁、悲观锁）
- ✅ WebSocket 实时消息推送（学习 WebSocket 实现）
- ✅ 错误处理与用户提示完善
- ✅ 基础单元测试覆盖（学习测试驱动开发）

### v1.2 规划（学习重点：搜索与性能）
- ✅ **Elasticsearch 全文搜索**（学习搜索引擎集成）
- ✅ **分页查询优化**（学习游标分页 vs OFFSET 分页）
- ✅ **接口限流实现**（学习令牌桶、滑动窗口算法）
- ✅ **异步处理与线程池调优**（学习 CompletableFuture）
- ✅ 商品推荐算法（学习推荐系统基础）
- ✅ 订单统计与数据分析（学习数据聚合）
- ✅ 操作日志系统（学习审计日志设计）

### v1.3 规划（学习重点：架构与运维）
- ✅ **Docker 容器化部署**（学习容器化技术）
- ✅ **CI/CD 流程**（学习自动化部署）
- ✅ **监控与日志聚合**（学习 ELK、Prometheus）
- ✅ **分布式事务**（学习 Seata、TCC 模式）
- ⚠️ 支付系统集成（**不实现真实集成，学习设计思路**）
- ⚠️ 邮件/短信通知（**不实现真实集成，模拟实现即可**）
- ⚠️ 国际化支持（**学习项目无需多语言**）

---

## 项目总结

NJUMarket v1.0 是一个面向南大校园的二手交易平台学习项目，采用前后端分离架构，已实现完整的业务闭环。

### 核心功能成果
- **用户系统**: 完整的用户认证、资料管理、个人主页功能
- **商品系统**: 商品发布、浏览、搜索、筛选、详情查看、可见性控制
- **订单系统**: 订单创建、支付、发货、收货、退款/退货流程
- **消息系统**: 会话管理、消息发送、商品/订单卡片分享
- **管理端**: 用户/商品/订单管理、数据统计、完整 CRUD 功能

### 技术架构成果
- **前端**: Vue 3 + Element Plus + Pinia，统一组件系统，响应式设计
- **后端**: Spring Boot 3.2.0 + JPA + MySQL，分层架构清晰
- **代码组织**: Controller → Service → Repository → Entity 分层清晰
- **可维护性**: 统一组件、业务规则集中、样式统一、响应式检测统一

### 数据统计
- **用户端**: 15+ 页面，20+ 组件，50+ API 接口，5 大核心模块
- **管理端**: 6 个管理页面，4 大管理模块，完整数据操作功能
- **数据库**: 17+ 实体表，完整的关系设计

### 学习价值
本项目聚焦以下核心技术的学习与实践：
1. **缓存一致性**: 缓存穿透/击穿/雪崩解决方案、分布式锁实现
2. **并发控制**: 事务隔离级别、乐观锁、悲观锁、分布式锁
3. **性能优化**: 数据库索引设计、分页查询优化、连接池调优
4. **异步处理**: 线程池配置、CompletableFuture 使用、异步编程模式
5. **限流与防刷**: 令牌桶、滑动窗口等限流算法
6. **系统设计**: 分层架构、事件驱动、审计日志等设计模式

---

**文档版本**: v1.0  
**最后更新**: 2025-01-27  
**维护者**: NJUMarket 开发团队

---

## 更新日志

### v1.0 - 2025-01-27: 消息卡片组件与架构优化

#### 消息卡片功能
- 消息中支持发送商品卡片和订单卡片
- 卡片显示关键信息：图片、标题、价格、状态标签、用户信息
- 通过 Profile API 实时获取用户昵称和头像
- 卡片点击跳转到对应详情页
- 支持从商品/订单详情页进入聊天并预选

#### 响应式与布局优化
- 卡片最大宽度 300px，移动端自适应缩小
- 对方消息左对齐，本方消息右对齐（包括卡片、时间、状态）
- 移动端消息内容占满可用空间，去除头像避让机制

#### 架构映射文档
- 新增 `ARCHITECTURE_MAPPING.md` 文档
- 完整记录前端 API → Controller → Service → Repository → Entity → 数据库表的映射关系
- 涵盖用户认证、商品、订单、消息、图片、管理端等所有模块

#### 可维护性提升
- 统一响应式检测：所有组件使用 `config/responsive.js`
- 清理样式重复：翻页器样式统一管理，无重复代码
- Profile 数据缓存优化建议（详见维护性建议章节）

