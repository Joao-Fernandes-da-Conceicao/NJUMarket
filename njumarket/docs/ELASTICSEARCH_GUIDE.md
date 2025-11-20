# ElasticSearch 集成与运维指南

> 适用于 NJUMarket v2.4.0 及以上版本，涵盖 ElasticSearch 部署、配置、搜索实现原理与常用治理操作。

---

## 1. 能力概述：容器化 & 持久化

- **容器化基础**：项目已具备完整的 Docker/Docker Compose 编排（见根目录 `docker-compose.yml`），所有微服务都通过统一网络、健康检查、Volume 管理运行。因此引入 ElasticSearch 只需新增一个服务节即可。
- **持久化能力**：Compose 已大量使用 `volumes`（如 `redis_data`、`postgres_data`），复制此模式即可为 ElasticSearch 提供数据与日志的持久化卷，确保容器重启后索引数据不丢失。
- **推荐做法**：在 `docker-compose.yml` 中追加如下服务（片段），即可获得容器化 + 持久化支撑：

```yaml
  elasticsearch:
    image: docker.elastic.co/elasticsearch/elasticsearch:8.13.4
    container_name: njumarket-elasticsearch
    environment:
      - discovery.type=single-node
      - ES_JAVA_OPTS=-Xms1g -Xmx1g
      - xpack.security.enabled=false      # 内网环境可关闭；生产建议开启
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

> ⚠️ 必须在容器内安装 IK 分词插件：`docker exec -it njumarket-elasticsearch ./bin/elasticsearch-plugin install analysis-ik`，安装后重启容器。

---

## 2. 环境配置流程

1. **准备 ElasticSearch 集群**
   - 可选：自建（Docker、物理机）或云托管（阿里云 ES、Elastic Cloud）。
   - 安装 IK 分词插件（`analysis-ik`）；若走云服务需在控制台启用对应插件。
2. **填写运行时变量**
   - `ELASTICSEARCH_URIS`：如 `http://localhost:9200`（本地）或 `http://elasticsearch:9200`（Compose 内网）。
   - `ELASTICSEARCH_USERNAME`/`ELASTICSEARCH_PASSWORD`：若关闭安全可保留默认。
   - Feature Flag：
     - `COMMODITY_SEARCH_ENABLED`：总开关
     - `COMMODITY_SEARCH_ES_ENABLED`：是否启用 ElasticSearch
     - `COMMODITY_SEARCH_AUTO_REINDEX`：服务启动时自动重建索引（默认 false）
     - `COMMODITY_SEARCH_SYNC_PAGE_SIZE`：重建批大小，推荐 200~1000
3. **Config Server**
   - `njumarket-config/src/main/resources/config-repo/njumarket-service-commodity.yml` / `-dev.yml` 已内置上述键值，可通过环境变量覆盖。
4. **启动服务**
   - 先启动 ElasticSearch，再依次启动 Config Server、Eureka、微服务。
   - 首次上线（或导入历史数据）运行内部接口 `POST /api/internal/commodity/search/reindex` 完成索引全量同步。

---

## 3. 搜索实现原理

| 模块 | 说明 |
| --- | --- |
| `CommoditySearchDocument` | 定义索引结构，字段绑定 IK 分词器（`ik_max_word` 写入、`ik_smart` 搜索），同时保留 Keyword/数值类型用于过滤与排序。 |
| `CommoditySearchRepository` | 基于 `spring-data-elasticsearch` 的仓储，负责增删查。 |
| `CommoditySearchService` | 构建多字段 `bool` 查询：`multi_match` 匹配标题/描述/地址等，Filter 层约束上架状态、可见性、价格区间、分类、位置等，同时支持最新/价格排序；失败时自动回退 PostgreSQL 查询。 |
| `CommoditySearchInitializer` | 根据 `commodity.search.sync.auto-reindex-on-startup` 决定是否在应用启动后后台重建索引。 |
| `CommodityServiceImpl` & `InternalController` | 在商品发布、更新、上下架、可见性调整、删除等操作后自动同步或移除索引，避免“写库不写索”。 |
| `CommodityQueryServiceImpl` | `/api/public/commodity/search` 入口在有关键词/过滤条件时优先走 ElasticSearch，返回 ID 后再回表装配 DTO；若 ES 不可用则透明降级。 |

> 搜索仅返回 `commodityStatus=ON_SHELF` 且 `sellerVisibility=buyerVisibility=PUBLIC` 的商品，保障展示一致性。

---

## 4. 常用运维操作

| 场景 | 接口/命令 | 说明 |
| --- | --- | --- |
| 全量重建索引 | `POST /api/internal/commodity/search/reindex` | 返回 `{"indexed": <count>}`；建议在低峰期执行。 |
| 单条补偿同步 | `POST /api/internal/commodity/{commodityId}/search-sync` | 处理异常数据或手工修复。 |
| 关闭搜索能力 | 设置 `COMMODITY_SEARCH_ENABLED=false` 或 `COMMODITY_SEARCH_ES_ENABLED=false` | 服务自动回退数据库搜索，无需重启前端。 |
| 验证查询效果 | `GET /api/public/commodity/search?keyword=二手手机&category=电子产品&sortBy=price_desc` | 可组合位置、价格过滤验证。 |
| 监控健康 | `curl http://<es-host>:9200/_cluster/health` | 搭建 Prometheus/Grafana 后可纳入统一监控。 |

---

## 5. 故障与排查

1. **服务启动报错 `analysis-ik` 缺失**：确认插件已安装并重启 ES。
2. **搜索接口 500**：若日志提示 ES 连接失败，检查 `ELASTICSEARCH_URIS`、网络连通、账号密码；必要时先临时关闭搜索开关。
3. **索引数据陈旧**：调用单条同步或全量重建；检查商品写操作是否落入日志中的 `syncCommoditySearchIndex`。
4. **性能调优**：适当提升容器内存（`ES_JAVA_OPTS`）、增加节点、为高查询量场景开启只读副本。

---

## 6. 推广与展望

- 有了容器化 + 持久化能力，可以轻松在测试、预发、生产复制 ElasticSearch 集群，且通过 Compose 统一管理。
- ElasticSearch 已成为 AI 搜索的底座，未来可进一步引入语义检索（Embedding + ES 向量检索）、智能推荐等能力。

---

> 若需将本指南纳入团队 Wiki，可直接引用本文或在此基础上补充企业内部流程（审批、变更、监控等），保持信息单一来源。

