# 南大集市 NJUMarket v2.4.0 项目文档

## 📌 版本概述

**NJUMarket v2.4.0** 是"中文智能搜索"版本，核心目标是引入 Elasticsearch 作为商品检索引擎，带来更贴近自然语言的中文搜索体验，并为后续 AI 能力打下基础。

> **版本状态**：✅ 已完成  
> **完成时间**：2025 年 11 月  
> **主要成果**：Elasticsearch 中文分词、多维度检索、索引同步与治理、日期精度优化、管理端 ES 管理功能

---

## ✨ 核心成果

| 模块 | 功能 | 状态 |
| --- | --- | --- |
| 商品服务 | 引入 `spring-data-elasticsearch`，建立 `commodities` 索引 | ✅ |
| 商品服务 | 全量/单条索引同步、内部重建接口、自动校准 | ✅ |
| 商品搜索 | 中文智能分词（IK Smart / IK Max Word），价格、分类、位置过滤 | ✅ |
| 商品接口 | `/api/public/commodity/search` 自动切换 Elasticsearch，支持排序 | ✅ |
| 内部接口 | `/api/internal/commodity/search/reindex`、`/api/internal/commodity/{id}/search-sync` | ✅ |
| 管理端接口 | `/api/admin/elasticsearch/reindex`、`/api/admin/elasticsearch/sync/{id}` | ✅ |
| 管理端界面 | ES 索引管理页面（`http://localhost:8082/elasticsearch`） | ✅ |
| 配置中心 | 增加 Elasticsearch 连接 & feature flag，可环境化控制 | ✅ |
| 日期处理 | PostgreSQL 日期精度优化（秒级精度）、时区转换 | ✅ |
| 数据转换 | Elasticsearch 日期转换器，支持多种日期格式 | ✅ |

---

## 🧠 功能实现

### 1. 中文智能搜索引擎

#### 1.1 索引文档设计

新增 `CommoditySearchDocument` 索引文档，聚合以下字段：
- **文本字段**：标题、描述、关键词载荷、地址（使用 IK 分词器）
- **精确字段**：商品ID、卖家ID、分类、状态、可见性（Keyword 类型）
- **数值字段**：价格、库存、点击量（用于过滤和排序）
- **日期字段**：发布时间（支持日期范围查询）

#### 1.2 中文分词配置

通过 `IK Smart` + `IK Max Word` 分词器实现中文智能分词：

```json
{
  "analysis": {
    "analyzer": {
      "zh_smart": {
        "type": "custom",
        "tokenizer": "ik_smart",
        "filter": ["lowercase", "asciifolding"]
      },
      "zh_max": {
        "type": "custom",
        "tokenizer": "ik_max_word",
        "filter": ["lowercase", "asciifolding"]
      }
    }
  }
}
```

- **索引时**：使用 `ik_max_word`（细粒度分词，提高召回率）
- **搜索时**：使用 `ik_smart`（粗粒度分词，提高精确度）

#### 1.3 查询构建

`CommoditySearchService` 负责构建 `Bool` 查询：

**关键词匹配**：
- `MultiMatch` 查询，支持多字段搜索
- 字段权重：`title^4`（标题权重最高）、`description^2`、`keywordPayload`、`addressSnapshotFull`、`category`

**过滤条件**：
- 商品状态：仅搜索 `ON_SHELF`（上架）商品
- 可见性：仅搜索 `PUBLIC`（公开）商品
- 价格区间：支持 `minPrice` 和 `maxPrice` 范围过滤
- 分类：精确匹配分类
- 位置：短语匹配地址信息

**排序支持**：
- 最新：按 `publishTime` 降序
- 价格升序：按 `price` 升序
- 价格降序：按 `price` 降序

**高亮支持**：
- 返回匹配字段的高亮片段
- 支持前端展示搜索关键词高亮

#### 1.4 搜索架构

采用 **ES 返回 ID + 数据库查询完整数据** 的架构：

```
用户搜索请求
    ↓
ES 搜索（返回商品ID列表 + 高亮）
    ↓
数据库批量查询（IN 查询）
    ↓
返回完整商品信息
```

**优势**：
- 数据一致性：数据库是唯一真实数据源
- 数据完整性：ES 只存储搜索字段，完整信息在数据库
- 实时性：价格、库存等关键信息实时查询
- 性能优化：ES 只返回 ID（轻量级），减少网络传输

### 2. 索引同步与治理

#### 2.1 自动同步机制

所有商品生命周期操作都会自动同步到 Elasticsearch：

- **商品发布**：自动创建索引
- **商品更新**：自动更新索引
- **商品下架**：自动更新索引（状态变更）
- **商品删除**：自动删除索引
- **可见性变更**：自动更新索引

#### 2.2 治理接口

内部新增两个治理接口：

1. **全量重建索引**
   ```
   POST /api/internal/commodity/search/reindex
   ```
   - 重建整个索引
   - 适用于：首次上线、索引损坏、批量数据导入后

2. **单条商品同步**
   ```
   POST /api/internal/commodity/{commodityId}/search-sync
   ```
   - 同步单条商品到索引
   - 适用于：数据修复、手动同步

#### 2.3 启动器

`CommoditySearchInitializer` 支持按配置自动重建索引：
- 配置项：`commodity.search.sync.auto-reindex-on-startup`
- 默认值：`false`（避免每次启动都重建）
- 适用场景：应急修复、首次部署

### 3. 日期处理优化

#### 3.1 PostgreSQL 日期精度问题

**问题**：
- PostgreSQL 存储的日期过于精确：`2025-11-19 19:22:37.953111+08`（微秒级）
- 只需要秒级精确度：`2025-10-29 14:34:10+08`
- PostgreSQL 的日期是带时区的（`timestamp with time zone`）

#### 3.2 解决方案

创建了 `LocalDateTimeSecondsConverter` 转换器：

**写入数据库时**：
1. 将 `LocalDateTime` 截断到秒级精度（去除纳秒部分）
2. 转换为 UTC 时区的 `Instant`（PostgreSQL `timestamp with time zone` 内部存储为 UTC）
3. 创建 `Timestamp` 对象

**从数据库读取时**：
1. `Timestamp` 内部存储为 UTC 时间
2. 转换为系统时区的 `LocalDateTime`
3. 截断到秒级精度（确保没有纳秒部分）

**应用到实体**：
```java
@CreationTimestamp
@Column(name = "publish_time", nullable = false, columnDefinition = "timestamp with time zone")
@jakarta.persistence.Convert(converter = com.njumarket.commodity.config.LocalDateTimeSecondsConverter.class)
private LocalDateTime publishTime;
```

#### 3.3 Elasticsearch 日期转换

创建了 `ElasticsearchConversionConfig`，包含多个日期转换器：

- **ObjectToLocalDateTimeConverter**：处理 Object 类型（ES 可能返回多种类型）
  - 支持 `LocalDate` 格式（`yyyy-MM-dd`）
  - 支持 `LocalDateTime` ISO 格式（`yyyy-MM-ddTHH:mm:ss`）
  - 支持 PostgreSQL 格式（`yyyy-MM-dd HH:mm:ss.ffffff+08`）
  - 自动移除微秒部分，只保留秒级精度

- **StringToLocalDateTimeConverter**：专门处理字符串类型
- **LocalDateToLocalDateTimeConverter**：处理 `LocalDate` 到 `LocalDateTime` 的转换

**ES 同步优化**：
- 在同步到 ES 时也截断到秒级精度
- 使用 `SourceFilter` 排除 `publishTime` 字段，避免搜索时反序列化问题
- 直接使用 `SearchHit.getId()` 获取文档 ID，不反序列化完整文档

### 4. 可配置化能力

配置中心新增：

```yaml
spring:
  elasticsearch:
    uris: ${ELASTICSEARCH_URIS:http://elasticsearch:9200}
    username: ${ELASTICSEARCH_USERNAME:elastic}
    password: ${ELASTICSEARCH_PASSWORD:changeme}

commodity:
  search:
    enabled: ${COMMODITY_SEARCH_ENABLED:true}
    elasticsearch:
      enabled: ${COMMODITY_SEARCH_ES_ENABLED:true}
      highlight-enabled: ${COMMODITY_SEARCH_HIGHLIGHT:true}
      index: ${COMMODITY_SEARCH_INDEX:commodities}
    sync:
      auto-reindex-on-startup: ${COMMODITY_SEARCH_AUTO_REINDEX:false}
      page-size: ${COMMODITY_SEARCH_SYNC_PAGE_SIZE:500}
```

> 本地 `-dev` 配置默认连接 `http://localhost:9200`，Docker 环境默认连接 `http://elasticsearch:9200`。

### 5. Controller & Service 改造

#### 5.1 搜索服务

`CommodityQueryServiceImpl` 在具备关键词/过滤条件时：
1. 优先使用 Elasticsearch 搜索
2. 若 ES 不可用则自动回退到 PostgreSQL
3. 保障系统可用性

#### 5.2 商品服务

`CommodityServiceImpl`、`InternalController` 在商品生命周期的关键节点同步索引：
- 避免"写库不写索"导致的脏搜索
- 确保搜索结果的实时性

---

## 🚀 使用步骤

### 1. 准备 Elasticsearch 集群

#### 1.1 Docker 部署（推荐）

在 `docker-compose.yml` 中添加：

```yaml
elasticsearch:
  image: docker.elastic.co/elasticsearch/elasticsearch:8.13.4
  container_name: njumarket-elasticsearch
  environment:
    - discovery.type=single-node
    - ES_JAVA_OPTS=-Xms1g -Xmx1g
    - xpack.security.enabled=false
  volumes:
    - es_data:/usr/share/elasticsearch/data
    - es_logs:/usr/share/elasticsearch/logs
  ports:
    - "9200:9200"
  healthcheck:
    test: ["CMD", "curl", "-f", "http://localhost:9200/_cluster/health"]
    interval: 30s
    timeout: 10s
    retries: 5
  networks:
    - njumarket-network

volumes:
  es_data:
  es_logs:
```

#### 1.2 安装 IK 分词插件

```bash
# 进入 ES 容器
docker exec -it njumarket-elasticsearch bash

# 安装 IK 分词插件
./bin/elasticsearch-plugin install https://github.com/medcl/elasticsearch-analysis-ik/releases/download/v8.13.4/elasticsearch-analysis-ik-8.13.4.zip

# 重启容器
docker restart njumarket-elasticsearch
```

> ⚠️ **重要**：插件版本必须与 ES 版本匹配（当前 ES 8.13.4）

### 2. 配置环境变量 / Config Server

设置以下环境变量：

- `ELASTICSEARCH_URIS`：ES 连接地址
  - 本地：`http://localhost:9200`
  - Docker：`http://elasticsearch:9200`
- `ELASTICSEARCH_USERNAME`：ES 用户名（如启用安全）
- `ELASTICSEARCH_PASSWORD`：ES 密码（如启用安全）
- `COMMODITY_SEARCH_ENABLED`：搜索总开关（默认 `true`）
- `COMMODITY_SEARCH_ES_ENABLED`：是否启用 Elasticsearch（默认 `true`）
- `COMMODITY_SEARCH_AUTO_REINDEX`：启动时自动重建索引（默认 `false`）
- `COMMODITY_SEARCH_SYNC_PAGE_SIZE`：重建批大小（默认 `500`）

### 3. 首次上线或修复索引

#### 3.1 通过管理端界面操作（推荐）

1. 访问管理端：`http://localhost:8082`
2. 使用 SYSTEM 权限管理员登录
3. 在侧边栏点击"ES 索引管理"
4. 执行索引管理操作：
   - **重建索引**：点击"重建索引"按钮（建议在低峰期执行）
   - **同步商品**：输入商品ID，点击"同步商品"按钮

#### 3.2 通过 API 调用（命令行）

```bash
# 调用管理端接口（通过 Gateway）
curl -X POST http://localhost:8080/api/admin/elasticsearch/reindex \
  -H "Authorization: Bearer <admin_token>"

# 或直接调用商品服务内部接口
curl -X POST http://localhost:8092/api/internal/commodity/search/reindex
```

#### 3.3 自动重建（应急场景）

在配置中临时开启：
```yaml
commodity:
  search:
    sync:
      auto-reindex-on-startup: true
```

重启服务即可自动重建索引。

---

## 📎 相关文件

| 类型 | 路径 |
| --- | --- |
| **依赖** | `njumarket-service-commodity/pom.xml` |
| **索引文档** | `CommoditySearchDocument.java` |
| **索引服务** | `CommoditySearchService.java` |
| **启动器** | `CommoditySearchInitializer.java` |
| **日期转换器** | `LocalDateTimeSecondsConverter.java` |
| **ES 转换器** | `ElasticsearchConversionConfig.java` |
| **控制器** | `PublicController.java`（调用搜索）<br>`InternalController.java`（索引治理） |
| **管理端接口** | `ElasticsearchController.java`（Admin Service）<br>`RestTemplateConfig.java`（服务间调用配置） |
| **管理端页面** | `Elasticsearch.vue`（管理端前端）<br>`elasticsearch.js`（API 封装） |
| **配置** | `njumarket-service-commodity.yml` / `njumarket-service-commodity-dev.yml` |
| **索引设置** | `resources/elasticsearch/commodity-settings.json` |
| **文档** | `ELASTICSEARCH_GUIDE.md`（运维指南） |

---

## ⚠️ 注意事项

### 1. Elasticsearch 依赖

- **IK 分词插件**：必须安装，否则索引初始化会失败
- **版本匹配**：插件版本必须与 ES 版本匹配

### 2. 搜索规则

- **商品状态**：仅搜索 `ON_SHELF`（上架）商品
- **可见性**：仅搜索 `PUBLIC`（公开）商品
- **数据一致性**：搜索依赖商品上架状态 + 可见性，只有 `ON_SHELF + PUBLIC` 的数据才会被召回

### 3. 容错机制

- **自动降级**：如果 Elasticsearch 故障，系统自动降级到 PostgreSQL 模糊搜索
- **性能影响**：降级后搜索效果会有所下降，但不会中断服务

### 4. 数据同步

- **批量导入**：当批量导入历史数据时务必调用重建接口，确保索引与数据库一致
- **实时同步**：商品增删改操作会自动同步，无需手动干预

### 5. 日期处理

- **精度**：所有日期字段统一为秒级精度
- **时区**：PostgreSQL 存储为 UTC，读取时转换为系统时区
- **ES 同步**：同步到 ES 时也保持秒级精度

---

## ✅ 回归测试建议

### 1. 搜索功能测试

- **关键词搜索**：测试中文多种表达（型号、描述、分类）
- **组合过滤**：价格区间、分类、位置等组合过滤
- **排序功能**：最新、价格升序、价格降序
- **高亮显示**：验证高亮片段是否正确返回

### 2. 数据同步测试

- **实时同步**：商品发布、更新、下架后搜索结果的即时性
- **批量同步**：重建索引接口在大量数据下的稳定性
- **数据一致性**：验证索引与数据库数据是否一致

### 3. 容错测试

- **ES 故障**：Elasticsearch 停机时搜索接口是否自动回退且不中断
- **网络异常**：ES 连接异常时的降级机制
- **数据异常**：日期转换异常时的处理

### 4. 性能测试

- **搜索响应时间**：正常情况下的搜索响应时间
- **并发搜索**：高并发场景下的性能表现
- **索引重建**：大量数据下的索引重建时间

---

## 🔮 后续优化方向

### 1. 缓存优化

- 在 `fetchCommoditiesByOrderedIds` 方法中添加 Redis 缓存
- 减少数据库查询压力
- 提升搜索响应速度

### 2. 拼音查询

- 安装 `analysis-pinyin` 插件
- 支持拼音搜索（如：输入 "shouji" 能搜索到 "手机"）
- 工作量：2-3 天

### 3. 模糊查询

- 使用 ES 原生 `fuzziness` 参数
- 支持容错搜索（如：输入 "手几" 能搜索到 "手机"）
- 工作量：1 天

### 4. 搜索优化

- 搜索相关性调优
- 个性化推荐
- AI 语义搜索

### 5. 管理端 ES 管理功能

- ✅ **已完成**：在管理端（`http://localhost:8082`）实现 ES 索引管理
- ✅ **功能**：重建索引、同步商品、索引统计
- ✅ **权限控制**：仅 SYSTEM 权限管理员可访问
- ✅ **界面**：友好的操作界面，包含操作说明和结果反馈

---

## 📊 技术栈总结

### 新增依赖

- `spring-data-elasticsearch`：Elasticsearch 集成
- `elasticsearch`：Elasticsearch Java 客户端

### 新增组件

- `CommoditySearchDocument`：索引文档模型
- `CommoditySearchService`：搜索服务
- `CommoditySearchRepository`：ES 仓储
- `CommoditySearchInitializer`：索引初始化器
- `LocalDateTimeSecondsConverter`：日期精度转换器
- `ElasticsearchConversionConfig`：ES 日期转换配置

### 技术亮点

1. **智能分词**：IK 分词器实现中文智能分词
2. **多维度检索**：支持关键词、价格、分类、位置等多维度过滤
3. **自动同步**：商品生命周期操作自动同步索引
4. **容错机制**：ES 故障时自动降级到数据库
5. **日期优化**：秒级精度、时区转换、格式兼容
6. **管理端集成**：在管理端（`http://localhost:8082`）提供 ES 索引管理界面

---

## 🎯 版本目标达成情况

| 目标 | 状态 | 说明 |
| --- | --- | --- |
| 引入 Elasticsearch | ✅ | 已完成集成 |
| 中文智能分词 | ✅ | IK 分词器已配置 |
| 多维度检索 | ✅ | 支持关键词、价格、分类、位置过滤 |
| 索引同步 | ✅ | 自动同步 + 治理接口 |
| 容错机制 | ✅ | ES 故障自动降级 |
| 日期优化 | ✅ | 秒级精度、时区转换 |
| 管理端 ES 管理 | ✅ | 管理界面（`http://localhost:8082/elasticsearch`）、权限控制 |
| 文档完善 | ✅ | 本文档 + 运维指南 |

---

> 通过本次迭代，NJUMarket 的搜索体验跨入"中文语义 + 多维度过滤"的新阶段，也为未来接入更高级的 AI 检索、推荐、问答打下了坚实的底座。  
> 
> **版本定位**：本次引入 Elasticsearch 主要是为了了解此技术栈并实现应用，已完成基础功能。后续可根据实际需求进行缓存优化、拼音查询、模糊查询等微调措施。  
> 
> **管理端 ES 管理**：在管理端（`http://localhost:8082`）提供了完整的 ES 索引管理功能，包括重建索引、同步商品等操作，方便管理员进行索引维护。只有 SYSTEM 权限的管理员可以访问。  
> 
> 祝使用愉快！ 🎉
