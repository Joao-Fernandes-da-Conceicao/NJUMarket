# 南大集市 NJUMarket v2.1.2 项目文档

## 📋 目录
- [版本概述](#版本概述)
- [版本更新内容](#版本更新内容)
- [Prometheus + Grafana 监控详解](#prometheus--grafana-监控详解)
- [使用指南](#使用指南)
- [技术总结](#技术总结)
- [后续版本规划](#后续版本规划)

---

## 版本概述

**NJUMarket v2.1.2** 是项目的监控和可观测性版本，主要完成了 Prometheus 指标收集和 Grafana 可视化监控系统的集成。

### 版本信息
- **版本**: v2.1.2
- **发布日期**: 2025-01-XX
- **状态**: ✅ **已完成**
- **主要目标**: 指标监控、可视化展示、系统可观测性

### 版本历史
- **v2.1.0** (2025-11-11): Actuator 监控、Docker 容器化、Swagger API 文档 ✅ **已完成**
- **v2.1.1** (2025-11-12): Resilience4j 熔断降级、Config Server 配置中心、环境隔离 ✅ **已完成**
- **v2.1.2** (2025-01-XX): Prometheus + Grafana 监控集成 ✅ **已完成**

### 主要成就

#### 监控系统集成
- ✅ 集成 Prometheus 指标收集（所有服务）
- ✅ 集成 Grafana 可视化平台
- ✅ 为所有服务添加 Prometheus 端点
- ✅ 创建基础监控仪表板
- ✅ 配置自动数据源和仪表板

#### 技术指标监控
- ✅ JVM 指标（内存、线程、GC）
- ✅ HTTP 指标（请求速率、延迟、状态码）
- ✅ Resilience4j 指标（熔断器状态）
- ✅ 数据库连接池指标
- ✅ 系统资源指标（CPU、内存、负载）

#### 文档完善
- ✅ 创建 Prometheus + Grafana 使用指南
- ✅ 提供 PromQL 查询示例
- ✅ 提供故障排查指南
- ✅ 提供最佳实践建议

---

## 版本更新内容

### 1. Prometheus 指标收集 ✅

**目标**: 收集和存储微服务的各项技术指标

**完成内容**:
- **依赖添加**: 为所有服务添加 `micrometer-registry-prometheus` 依赖
  - njumarket-service-order
  - njumarket-service-auth
  - njumarket-service-commodity
  - njumarket-service-message
  - njumarket-service-image
  - njumarket-service-admin
  - njumarket-service-notification
  - njumarket-gateway
  - njumarket-discovery
  - njumarket-config
- **端点暴露**: 在所有服务配置中暴露 `prometheus` 端点
  - 配置路径: `management.endpoints.web.exposure.include`
  - 端点路径: `/actuator/prometheus`
- **Prometheus 服务**: 在 docker-compose.yml 中添加 Prometheus 服务
  - 端口: 9090
  - 数据保留: 30 天
  - 抓取间隔: 15 秒
- **抓取配置**: 配置所有服务的指标抓取规则
  - 10 个微服务
  - 1 个 Prometheus 自身监控

**详细说明**: 见 [Prometheus + Grafana 监控详解](#prometheus--grafana-监控详解)

### 2. Grafana 可视化平台 ✅

**目标**: 提供直观的监控数据可视化界面

**完成内容**:
- **Grafana 服务**: 在 docker-compose.yml 中添加 Grafana 服务
  - 端口: 3000
  - 默认账号: admin/admin
- **数据源配置**: 自动配置 Prometheus 数据源
  - 配置文件: `grafana/provisioning/datasources/prometheus.yml`
  - 连接地址: `http://prometheus:9090`
- **仪表板配置**: 创建基础 Spring Boot 指标仪表板
  - JVM 内存使用
  - HTTP 请求速率
  - HTTP 请求延迟（p95, p99）
  - JVM 线程数
  - CPU 使用率
  - GC 暂停时间
- **仪表板自动加载**: 配置仪表板自动发现和加载

### 3. 监控文档 ✅

**目标**: 提供完整的使用指南和最佳实践

**完成内容**:
- **使用指南**: 创建 `PROMETHEUS_GRAFANA_USAGE.md`
  - 快速开始指南
  - Prometheus 使用说明
  - Grafana 使用说明
  - PromQL 查询示例
  - 常见问题排查
  - 最佳实践建议

---

## Prometheus + Grafana 监控详解

### 架构设计

```
┌─────────────────┐
│   微服务应用     │
│  (10个服务)     │
└────────┬────────┘
         │ /actuator/prometheus
         ▼
┌─────────────────┐
│   Prometheus    │ ← 抓取指标 (每15秒)
│   (端口 9090)   │
└────────┬────────┘
         │ PromQL 查询
         ▼
┌─────────────────┐
│    Grafana      │ ← 可视化展示
│   (端口 3000)   │
└─────────────────┘
```

### 监控指标类型

#### 1. JVM 指标
- **内存指标**: `jvm_memory_used_bytes`, `jvm_memory_max_bytes`
- **线程指标**: `jvm_threads_live_threads`, `jvm_threads_daemon_threads`
- **GC 指标**: `jvm_gc_pause_seconds`, `jvm_gc_memory_allocated_bytes`

#### 2. HTTP 指标
- **请求计数**: `http_server_requests_seconds_count`
- **请求延迟**: `http_server_requests_seconds_sum`, `http_server_requests_seconds_bucket`
- **状态码分布**: 通过 `status` 标签过滤

#### 3. Resilience4j 指标
- **熔断器状态**: `resilience4j_circuitbreaker_state`
- **调用统计**: `resilience4j_circuitbreaker_calls`
- **失败率**: `resilience4j_circuitbreaker_failure_rate`

#### 4. 系统指标
- **CPU 使用率**: `system_cpu_usage`, `process_cpu_usage`
- **系统负载**: `system_load_average_1m`
- **进程运行时间**: `process_uptime_seconds`

### Prometheus 配置

**配置文件**: `prometheus/prometheus.yml`

**主要配置项**:
- **抓取间隔**: 15 秒
- **评估间隔**: 15 秒
- **数据保留**: 30 天
- **服务列表**: 10 个微服务 + Prometheus 自身

### Grafana 配置

**数据源配置**: `grafana/provisioning/datasources/prometheus.yml`
- 自动连接 Prometheus
- 查询超时: 15 秒

**仪表板配置**: `grafana/provisioning/dashboards/dashboard.yml`
- 自动发现仪表板
- 更新间隔: 10 秒

**预置仪表板**: `grafana/dashboards/spring-boot-metrics.json`
- 6 个监控面板
- 支持服务变量过滤

---

## 使用指南

### 快速开始

#### 1. 启动服务

```bash
docker-compose up -d
```

#### 2. 访问 Prometheus

- **URL**: http://localhost:9090
- **功能**: 查看指标、执行 PromQL 查询、查看监控目标

#### 3. 访问 Grafana

- **URL**: http://localhost:3000
- **用户名**: `admin`
- **密码**: `admin`（首次登录需修改）

### 常用操作

#### 查看监控目标

1. 访问 Prometheus → Status → Targets
2. 检查所有服务状态是否为 UP（绿色）

#### 执行 PromQL 查询

**示例 1: 查看 JVM 内存使用**
```promql
jvm_memory_used_bytes{application="njumarket-service-order"}
```

**示例 2: 查看 HTTP 请求速率**
```promql
rate(http_server_requests_seconds_count[5m])
```

**示例 3: 查看 HTTP 请求延迟 p95**
```promql
histogram_quantile(0.95, rate(http_server_requests_seconds_bucket[5m]))
```

#### 查看 Grafana 仪表板

1. 登录 Grafana
2. 点击左侧菜单 Dashboards → Browse
3. 选择 "NJUMarket Spring Boot Metrics"
4. 使用顶部变量选择要监控的服务

### 详细文档

更多使用说明请参考: [PROMETHEUS_GRAFANA_USAGE.md](./PROMETHEUS_GRAFANA_USAGE.md)

---

## 技术总结

### 技术选型

#### Prometheus
- **优势**:
  - 强大的 PromQL 查询语言
  - 高效的时间序列数据库
  - 丰富的生态系统
  - 与 Spring Boot 集成简单
- **适用场景**: 指标收集和存储

#### Grafana
- **优势**:
  - 丰富的可视化组件
  - 灵活的仪表板配置
  - 支持多种数据源
  - 易于使用和定制
- **适用场景**: 监控数据可视化

### 集成方式

#### 非侵入性集成
- ✅ 使用 Spring Boot Actuator 自动收集指标
- ✅ 无需修改业务代码
- ✅ 零配置即可使用（添加依赖即可）

#### 配置方式
- ✅ 通过配置文件暴露端点
- ✅ 通过 docker-compose 管理服务
- ✅ 通过配置文件管理抓取规则

### 监控范围

#### 已监控指标（技术指标）
- ✅ JVM 运行时指标
- ✅ HTTP 请求指标
- ✅ Resilience4j 熔断器指标
- ✅ 数据库连接池指标
- ✅ 系统资源指标

#### 未监控指标（业务指标）
- ❌ 订单创建数量
- ❌ 用户注册数量
- ❌ 商品浏览量
- ❌ 交易金额统计
- ❌ 消息发送数量

**说明**: 业务指标监控需要侵入性修改代码，属于冷任务，暂未实现。

---

## 后续版本规划

### 短期规划（可选）

#### 业务指标监控（冷任务）
- [ ] 在业务代码中添加自定义指标
- [ ] 使用 `MeterRegistry` 记录业务事件
- [ ] 创建业务指标仪表板
- [ ] 配置业务指标告警规则

**优先级**: 低（学习性项目，可能实现也可能不实现）

#### 告警系统
- [ ] 配置 Prometheus Alertmanager
- [ ] 创建告警规则
- [ ] 配置告警通知（邮件、钉钉、企业微信等）

#### 长期存储
- [ ] 集成 Thanos 或 VictoriaMetrics
- [ ] 配置长期数据存储
- [ ] 实现数据压缩和归档

### 长期规划

#### 日志聚合
- [ ] 集成 ELK Stack（Elasticsearch + Logstash + Kibana）
- [ ] 或集成 Loki + Grafana
- [ ] 实现日志集中收集和查询

#### APM 监控
- [ ] 集成 SkyWalking 或 Pinpoint
- [ ] 实现应用性能监控
- [ ] 与现有监控系统整合

#### 全链路追踪增强
- [ ] 优化 Zipkin 配置
- [ ] 增加业务标签
- [ ] 实现追踪数据持久化

---

## 已知问题和限制

### 当前限制

1. **业务指标缺失**
   - 仅监控技术指标，未监控业务指标
   - 业务指标需要侵入性修改代码
   - 属于冷任务，暂未实现

2. **告警功能未配置**
   - Prometheus 告警规则未配置
   - Alertmanager 未集成
   - 需要手动查看监控数据

3. **数据保留时间**
   - 当前保留 30 天
   - 长期历史数据无法查询
   - 需要集成长期存储方案

### 已知问题

1. **中文显示**
   - Grafana 仪表板中文可能显示异常
   - 建议使用英文标签和描述

2. **内存使用**
   - Prometheus 数据量增长可能导致内存使用增加
   - 建议定期清理或调整保留时间

---

## 参考资源

### Prometheus
- [Prometheus 官方文档](https://prometheus.io/docs/)
- [PromQL 查询语言](https://prometheus.io/docs/prometheus/latest/querying/basics/)
- [Spring Boot Actuator 指标](https://docs.spring.io/spring-boot/docs/current/reference/html/actuator.html#actuator.metrics)

### Grafana
- [Grafana 官方文档](https://grafana.com/docs/)
- [Grafana 仪表板示例](https://grafana.com/grafana/dashboards/)

### Micrometer
- [Micrometer 官方文档](https://micrometer.io/docs)
- [Prometheus Registry](https://micrometer.io/docs/registry/prometheus)

### 相关文档
- [PROMETHEUS_GRAFANA_USAGE.md](./PROMETHEUS_GRAFANA_USAGE.md) - 详细使用指南
- [ACTUATOR_INTEGRATION.md](./ACTUATOR_INTEGRATION.md) - Actuator 集成文档

---

## 版本总结

### 完成情况

✅ **已完成**:
- Prometheus 指标收集系统
- Grafana 可视化平台
- 所有服务的 Prometheus 端点配置
- 基础监控仪表板
- 完整的使用文档

⏸️ **暂未实现**:
- 业务指标监控（冷任务）
- 告警系统配置
- 长期数据存储

### 技术亮点

1. **非侵入性集成**: 无需修改业务代码即可实现监控
2. **完整的技术指标覆盖**: JVM、HTTP、Resilience4j、系统资源等
3. **易于使用**: 提供详细的使用文档和示例
4. **可扩展性**: 支持自定义指标和仪表板

### 学习价值

1. **监控系统设计**: 了解 Prometheus + Grafana 架构
2. **指标收集**: 学习 Spring Boot Actuator 和 Micrometer
3. **数据可视化**: 学习 Grafana 仪表板设计
4. **PromQL 查询**: 掌握时间序列数据查询语言

---

**版本**: v2.1.2  
**状态**: ✅ **已完成**  
**日期**: 2025-01-XX

---

## 版本历史

- **v2.1.0** (2025-11-11): Actuator 监控、Docker 容器化、Swagger API 文档 ✅ **已完成**
- **v2.1.1** (2025-11-12): Resilience4j 熔断降级、Config Server 配置中心、环境隔离 ✅ **已完成**
- **v2.1.2** (2025-01-XX): Prometheus + Grafana 监控集成 ✅ **已完成**

