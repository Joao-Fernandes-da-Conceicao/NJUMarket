# 南大集市 NJUMarket v2.4.1 项目文档

## 📌 版本概述

**NJUMarket v2.4.1** 属于 2.4 系列的迭代版本，重点围绕“热点数据缓存”完善了商品与用户档案的 Cache Aside 机制，确保在不牺牲一致性的同时兼顾性能。

> **版本状态**：✅ 已完成  
> **完成时间**：2025 年 11 月  
> **主要成果**：商品详情/热门/最新缓存重构、UserProfile 缓存增强、日志可观测性优化

---

## ✨ 核心成果

| 模块 | 功能 | 状态 |
| --- | --- | --- |
| 缓存策略 | UserProfile Cache Aside 全量落地 | ✅ |
| 缓存策略 | 商品详情 Cache Aside + TTL 优化 | ✅ |
| 缓存策略 | 热门/最新商品缓存注册表 + 精准失效 | ✅ |
| 缓存策略 | 最新商品缓存支持 FIFO 插入 | ✅ |
| 可观测性 | CacheUtil 日志调为 INFO，支持默认观察 | ✅ |
| 配置管理 | Config Server 默认 DEBUG 配置按需开启 | ✅ |

---

## 🧩 功能详情

### 1. UserProfile 缓存

- `UserProfileServiceImpl.getUserProfile()` 与 `getPublicUserProfile()` 采用 Cache Aside，缓存完整 `UserProfileDTO`。
- 更新档案、头像、信用分、评分、交易统计、VIP 等所有写操作统一调用 `evictUserProfileCache(userId, reason)`，同时删除：
  - `cache:user:profile:{userId}`（批量查询缓存）
  - `cache:user:profile:detail:{userId}`（单条详情缓存）
- 缓存 TTL：30 分钟，命中优先，回源兜底。

### 2. 商品缓存

#### 2.1 商品详情

- `CommodityQueryServiceImpl.getCommodityDetail()` 使用 Cache Aside，key 为 `cache:commodity:detail:{commodityId}`。
- 更新、上下架、删除等写操作调用 `evictCommodityCache()` 清理详情缓存。
- 点击量异步更新不再清理缓存，依赖 TTL 自然过期，避免缓存频繁失效。

#### 2.2 热门/最新列表

- 新增 `cache:commodity:hot:registry` / `cache:commodity:latest:registry`，记录已生成的缓存 key。
- 热门/最新列表读取时，将缓存 key 写入 registry，便于后续精准失效。
- 更新商品、下架、删除时，扫描 registry 中的 key：
  - 如果命中该商品，则删除对应缓存并从 registry 中移除。
- 最新列表在新增/上架时支持 FIFO 插入：将商品插入到缓存前端，超出 limit 的自动淘汰。

### 3. 可观测性

- `CacheUtil` 的关键日志 (命中/未命中/写入/删除) 全部提升至 `INFO`，方便在默认日志级别下观测缓存行为。
- Config Server 的 `application.yml` 默认不再强制 DEBUG，改为按需在具体服务配置。

---

## 🧪 测试情况

| 测试类型 | 内容 | 结果 |
| --- | --- | --- |
| 单元测试 | 缓存工具类（CacheUtil） | ⚠️ 手动验证 |
| 集成测试 | UserProfile CRUD + 缓存命中/失效 | ✅ |
| 集成测试 | 商品新增/更新/删除 + 热门/最新缓存行为 | ✅ |
| 手动验证 | 观察缓存命中日志、Redis key 注册表 | ✅ |

> 备注：由于为机制性优化，主要依赖集成测试与日志验证；上线后建议结合 Redis 监控查看 key 规模与命中率。

---

## 💡 使用指南

### 1. 观察缓存日志

```bash
# 进入服务容器/实例后查看日志
tail -f logs/njumarket-service-commodity.log | grep cache
```

### 2. Redis key 约定

| Key | 含义 |
| --- | --- |
| `cache:commodity:detail:{id}` | 商品详情缓存 |
| `cache:commodity:hot:{limit}` | 热门商品缓存 |
| `cache:commodity:hot:registry` | 热门缓存 key 集合 |
| `cache:commodity:latest:{limit}` | 最新商品缓存 |
| `cache:commodity:latest:registry` | 最新缓存 key 集合 |
| `cache:user:profile:{userId}` | UserProfile 批量缓存 |
| `cache:user:profile:detail:{userId}` | UserProfile 单条缓存 |

---

## 📎 相关文件

| 类型 | 路径 |
| --- | --- |
| Cache 工具 | `njumarket-common/utils/CacheUtil.java` |
| Redis 常量 | `njumarket-common/utils/RedisConstants.java` |
| 商品查询 | `njumarket-service-commodity/.../CommodityQueryServiceImpl.java` |
| 商品写操作 | `njumarket-service-commodity/.../CommodityServiceImpl.java` |
| 用户档案服务 | `njumarket-service-auth/.../UserProfileServiceImpl.java` |
| Config Server 配置 | `njumarket-config/src/main/resources/config-repo/` |

---

## ✅ 下一步建议

1. **缓存监控**：在 Redis 或 Prometheus 中增加命中率统计，验证策略效果。
2. **缓存预热**：热门/最新列表可在定时任务中预热，降低首次查询延迟。
3. **精细化配置**：按业务热点调整不同缓存的 TTL（比如热门 15 分钟、最新 5 分钟）。
4. **灰度策略**：后续改动可在 registry 中加入“版本号”或“命名空间”，支持逐步切换。

---

**版本总结**：v2.4.1 将热点数据的缓存策略补齐到了“机制级”完善状态，商品与用户档案均具备完整的 Cache Aside 生命周期。配合 ES 搜索及之前的日期/向量处理，2.4 系列的性能与可维护性均得到巩固。下一阶段可以结合缓存命中率数据进一步做精细化调优。

