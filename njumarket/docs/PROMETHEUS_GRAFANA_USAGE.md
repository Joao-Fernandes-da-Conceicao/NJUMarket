# Prometheus + Grafana 监控使用指南

## 目录

- [概述](#概述)
- [快速开始](#快速开始)
- [Prometheus 使用指南](#prometheus-使用指南)
- [Grafana 使用指南](#grafana-使用指南)
- [监控指标说明](#监控指标说明)
- [常见问题排查](#常见问题排查)
- [最佳实践](#最佳实践)

---

## 概述

本项目已集成 **Prometheus** 和 **Grafana** 监控系统，用于收集和可视化微服务的各项指标。

### 组件说明

- **Prometheus**: 时序数据库，负责收集和存储指标数据
- **Grafana**: 可视化平台，用于创建和展示监控仪表板

### 架构图

```
┌─────────────┐
│ 微服务应用   │ ──> /actuator/prometheus (指标端点)
└─────────────┘
       │
       ▼
┌─────────────┐
│ Prometheus  │ ──> 抓取指标 (每15秒)
└─────────────┘
       │
       ▼
┌─────────────┐
│  Grafana    │ ──> 查询和可视化
└─────────────┘
```

---

## 快速开始

### 1. 启动服务

确保所有服务已启动（包括 Prometheus 和 Grafana）：

```bash
docker-compose up -d
```

### 2. 验证服务状态

检查 Prometheus 和 Grafana 是否正常运行：

```bash
# 检查 Prometheus
curl http://localhost:9090/-/healthy

# 检查 Grafana
curl http://localhost:3000/api/health
```

### 3. 访问地址

- **Prometheus UI**: http://localhost:9090
- **Grafana UI**: http://localhost:3000
  - 默认用户名: `admin`
  - 默认密码: `admin`
  - 首次登录会要求修改密码

---

## Prometheus 使用指南

### 1. 访问 Prometheus

打开浏览器访问：http://localhost:9090

### 2. 查看监控目标（Targets）

1. 点击顶部菜单 **Status** → **Targets**
2. 查看所有服务的抓取状态
3. 状态说明：
   - **UP** (绿色): 服务正常，指标抓取成功
   - **DOWN** (红色): 服务异常，无法抓取指标

### 3. 查询指标（PromQL）

#### 3.1 基本查询

在 **Graph** 页面输入 PromQL 查询语句：

**示例 1：查看 JVM 内存使用**

```promql
jvm_memory_used_bytes{application="njumarket-service-order"}
```

**示例 2：查看 HTTP 请求速率**

```promql
rate(http_server_requests_seconds_count[5m])
```

**示例 3：查看 HTTP 请求延迟（p95）**

```promql
histogram_quantile(0.95, rate(http_server_requests_seconds_bucket[5m]))
```

#### 3.2 常用 PromQL 查询

| 指标类型 | PromQL 查询 | 说明 |
|---------|------------|------|
| 请求速率 | `rate(http_server_requests_seconds_count[5m])` | 每秒请求数 |
| 请求延迟 p95 | `histogram_quantile(0.95, rate(http_server_requests_seconds_bucket[5m]))` | 95% 请求的延迟 |
| 请求延迟 p99 | `histogram_quantile(0.99, rate(http_server_requests_seconds_bucket[5m]))` | 99% 请求的延迟 |
| JVM 堆内存 | `jvm_memory_used_bytes{area="heap"}` | 堆内存使用量 |
| JVM 线程数 | `jvm_threads_live_threads` | 活跃线程数 |
| CPU 使用率 | `system_cpu_usage * 100` | 系统 CPU 使用率 |
| GC 暂停时间 | `rate(jvm_gc_pause_seconds_sum[5m])` | GC 暂停时间 |

#### 3.3 按服务过滤

```promql
# 查看特定服务的指标
jvm_memory_used_bytes{application="njumarket-service-order"}

# 查看多个服务的指标
jvm_memory_used_bytes{application=~"njumarket-service-.*"}

# 按标签过滤
http_server_requests_seconds_count{status="200", uri="/api/user/order"}
```

### 4. 查看指标列表

1. 点击顶部菜单 **Graph**
2. 点击查询框右侧的 **Metrics** 按钮
3. 浏览所有可用的指标

### 5. 配置告警规则（可选）

Prometheus 支持配置告警规则，当指标超过阈值时触发告警。

**配置文件位置**: `prometheus/alert_rules.yml`（需要创建）

**示例告警规则**:

```yaml
groups:
  - name: njumarket_alerts
    rules:
      - alert: HighMemoryUsage
        expr: jvm_memory_used_bytes{area="heap"} / jvm_memory_max_bytes{area="heap"} > 0.9
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "高内存使用率"
          description: "服务 {{ $labels.application }} 内存使用率超过 90%"
```

---

## Grafana 使用指南

### 1. 首次登录

1. 访问 http://localhost:3000
2. 输入默认用户名和密码：`admin` / `admin`
3. 首次登录会要求修改密码（建议修改）

### 2. 数据源配置

数据源已自动配置，无需手动设置。如需验证：

1. 点击左侧菜单 **Configuration** (齿轮图标) → **Data Sources**
2. 查看 **Prometheus** 数据源
3. 点击 **Prometheus** 查看配置详情
4. URL 应为: `http://prometheus:9090`

### 3. 查看预置仪表板

项目已预置一个 Spring Boot 指标仪表板：

1. 点击左侧菜单 **Dashboards** (四个方块图标)
2. 点击 **Browse** 查看所有仪表板
3. 选择 **NJUMarket Spring Boot Metrics**

### 4. 创建自定义仪表板

#### 4.1 创建新仪表板

1. 点击左侧菜单 **+** → **Create Dashboard**
2. 点击 **Add visualization** 添加面板

#### 4.2 添加指标面板

**示例：添加 JVM 内存使用面板**

1. 在查询框输入 PromQL：
   ```promql
   jvm_memory_used_bytes{application="$application"}
   ```
2. 设置面板标题：`JVM Memory Usage`
3. 选择可视化类型：`Time series`
4. 配置 Y 轴单位：`bytes`
5. 点击 **Apply** 保存

#### 4.3 使用变量

仪表板支持使用变量进行动态过滤：

1. 点击仪表板设置（齿轮图标）
2. 选择 **Variables** → **Add variable**
3. 配置变量：
   - **Name**: `application`
   - **Type**: `Query`
   - **Query**: `label_values(application)`
   - **Multi-value**: 启用（可选）
4. 在查询中使用变量：
   ```promql
   jvm_memory_used_bytes{application="$application"}
   ```

### 5. 常用仪表板模板

#### 5.1 JVM 监控面板

```promql
# 堆内存使用
jvm_memory_used_bytes{area="heap", application="$application"}

# 非堆内存使用
jvm_memory_used_bytes{area="nonheap", application="$application"}

# 线程数
jvm_threads_live_threads{application="$application"}

# GC 暂停时间
rate(jvm_gc_pause_seconds_sum{application="$application"}[5m])
```

#### 5.2 HTTP 请求监控面板

```promql
# 请求速率
rate(http_server_requests_seconds_count{application="$application"}[5m])

# 请求延迟 p95
histogram_quantile(0.95, rate(http_server_requests_seconds_bucket{application="$application"}[5m]))

# 请求延迟 p99
histogram_quantile(0.99, rate(http_server_requests_seconds_bucket{application="$application"}[5m]))

# 错误率
rate(http_server_requests_seconds_count{status=~"5..", application="$application"}[5m]) / 
rate(http_server_requests_seconds_count{application="$application"}[5m])
```

#### 5.3 系统资源监控面板

```promql
# CPU 使用率
system_cpu_usage{application="$application"} * 100

# 进程 CPU 使用率
process_cpu_usage{application="$application"} * 100

# 系统负载
system_load_average_1m{application="$application"}
```

### 6. 导出/导入仪表板

#### 导出仪表板

1. 打开仪表板
2. 点击右上角 **Settings** (齿轮图标)
3. 选择 **JSON Model**
4. 复制 JSON 内容保存

#### 导入仪表板

1. 点击左侧菜单 **+** → **Import**
2. 粘贴 JSON 内容或上传 JSON 文件
3. 选择数据源
4. 点击 **Import**

---

## 监控指标说明

### 1. JVM 指标

| 指标名称 | 说明 | 单位 |
|---------|------|------|
| `jvm_memory_used_bytes` | JVM 内存使用量 | bytes |
| `jvm_memory_max_bytes` | JVM 最大内存 | bytes |
| `jvm_memory_committed_bytes` | JVM 已提交内存 | bytes |
| `jvm_threads_live_threads` | 活跃线程数 | count |
| `jvm_threads_daemon_threads` | 守护线程数 | count |
| `jvm_gc_pause_seconds` | GC 暂停时间 | seconds |
| `jvm_gc_memory_allocated_bytes` | GC 分配的内存 | bytes |

### 2. HTTP 指标

| 指标名称 | 说明 | 单位 |
|---------|------|------|
| `http_server_requests_seconds_count` | HTTP 请求总数 | count |
| `http_server_requests_seconds_sum` | HTTP 请求总耗时 | seconds |
| `http_server_requests_seconds_max` | HTTP 请求最大耗时 | seconds |
| `http_server_requests_seconds_bucket` | HTTP 请求延迟分布 | count |

**标签说明**:
- `method`: HTTP 方法 (GET, POST, PUT, DELETE)
- `uri`: 请求路径
- `status`: HTTP 状态码 (200, 404, 500 等)
- `exception`: 异常类型（如果有）

### 3. Resilience4j 指标

| 指标名称 | 说明 | 单位 |
|---------|------|------|
| `resilience4j_circuitbreaker_state` | 熔断器状态 | 0=CLOSED, 1=OPEN, 2=HALF_OPEN |
| `resilience4j_circuitbreaker_calls` | 熔断器调用次数 | count |
| `resilience4j_circuitbreaker_failure_rate` | 失败率 | ratio |

### 4. 数据库指标

| 指标名称 | 说明 | 单位 |
|---------|------|------|
| `jdbc_connections_active` | 活跃连接数 | count |
| `jdbc_connections_idle` | 空闲连接数 | count |
| `jdbc_connections_max` | 最大连接数 | count |

### 5. 系统指标

| 指标名称 | 说明 | 单位 |
|---------|------|------|
| `system_cpu_usage` | 系统 CPU 使用率 | ratio (0-1) |
| `process_cpu_usage` | 进程 CPU 使用率 | ratio (0-1) |
| `system_load_average_1m` | 系统负载（1分钟） | load |
| `process_uptime_seconds` | 进程运行时间 | seconds |

---

## 常见问题排查

### 1. Prometheus 无法抓取指标

**问题**: Targets 页面显示服务状态为 DOWN

**排查步骤**:

1. 检查服务是否正常运行：
   ```bash
   docker ps | grep njumarket
   ```

2. 检查服务的 `/actuator/prometheus` 端点是否可访问：
   ```bash
   curl http://localhost:8093/actuator/prometheus
   ```

3. 检查 Prometheus 配置：
   - 查看 `prometheus/prometheus.yml` 中的服务地址是否正确
   - 确认服务名称与 docker-compose.yml 中的服务名一致

4. 检查网络连接：
   ```bash
   docker exec njumarket-prometheus wget -O- http://order-service:8093/actuator/prometheus
   ```

### 2. Grafana 无法连接 Prometheus

**问题**: Grafana 显示 "Data source is not working"

**排查步骤**:

1. 检查 Prometheus 是否正常运行：
   ```bash
   curl http://localhost:9090/-/healthy
   ```

2. 检查 Grafana 数据源配置：
   - 进入 Grafana → Configuration → Data Sources
   - 确认 URL 为 `http://prometheus:9090`（容器内网络）
   - 点击 **Test** 按钮测试连接

3. 检查网络连接：
   ```bash
   docker exec njumarket-grafana wget -O- http://prometheus:9090/-/healthy
   ```

### 3. 指标数据不更新

**问题**: Grafana 图表显示 "No data"

**排查步骤**:

1. 检查 Prometheus 是否在抓取数据：
   - 访问 Prometheus → Status → Targets
   - 确认所有服务状态为 UP

2. 检查时间范围：
   - 确认 Grafana 的时间范围设置正确
   - 尝试扩大时间范围（如最近 1 小时）

3. 检查 PromQL 查询：
   - 在 Prometheus 的 Graph 页面测试相同的查询
   - 确认查询语法正确

4. 检查指标名称：
   - 在 Prometheus → Graph → Metrics 中搜索指标名称
   - 确认指标名称和标签正确

### 4. 内存使用过高

**问题**: Prometheus 或 Grafana 容器内存使用过高

**解决方案**:

1. 调整 Prometheus 数据保留时间：
   ```yaml
   # docker-compose.yml
   command:
     - '--storage.tsdb.retention.time=7d'  # 改为 7 天
   ```

2. 限制容器内存：
   ```yaml
   # docker-compose.yml
   deploy:
     resources:
       limits:
         memory: 2G
   ```

### 5. 中文乱码问题

**问题**: Grafana 仪表板中文显示乱码

**解决方案**:

1. 检查容器字体配置
2. 使用英文标签和描述（推荐）

---

## 最佳实践

### 1. 指标命名规范

- 使用小写字母和下划线
- 使用有意义的名称
- 包含单位信息（如 `_bytes`, `_seconds`, `_count`）

**示例**:
- ✅ `order_created_total`
- ✅ `user_login_duration_seconds`
- ❌ `OrderCreated`
- ❌ `userLoginTime`

### 2. 标签使用

- 使用标签区分不同的维度
- 避免高基数标签（如用户ID、订单ID）
- 标签值应该是有限的、可枚举的

**示例**:
```promql
# ✅ 好的标签
http_requests_total{method="GET", status="200", service="order"}

# ❌ 不好的标签（高基数）
http_requests_total{user_id="12345", order_id="67890"}
```

### 3. 查询性能优化

- 使用 `rate()` 或 `increase()` 处理计数器
- 合理设置时间范围（避免查询过长时间范围）
- 使用 `recording rules` 预计算常用查询

### 4. 告警规则设计

- 设置合理的阈值
- 使用 `for` 子句避免瞬时波动触发告警
- 添加有意义的告警描述和标签

**示例**:
```yaml
- alert: HighErrorRate
  expr: rate(http_server_requests_seconds_count{status=~"5.."}[5m]) > 0.1
  for: 5m  # 持续 5 分钟才触发
  labels:
    severity: critical
  annotations:
    summary: "错误率过高"
    description: "服务 {{ $labels.application }} 错误率超过 10%"
```

### 5. 仪表板设计

- 按服务或功能分组
- 使用变量实现动态过滤
- 设置合理的刷新间隔（10s-30s）
- 添加说明和文档链接

### 6. 数据保留策略

- 开发环境：7-15 天
- 生产环境：30-90 天
- 使用长期存储（如 Thanos）存储历史数据

---

## 相关资源

- [Prometheus 官方文档](https://prometheus.io/docs/)
- [PromQL 查询语言](https://prometheus.io/docs/prometheus/latest/querying/basics/)
- [Grafana 官方文档](https://grafana.com/docs/)
- [Spring Boot Actuator 指标](https://docs.spring.io/spring-boot/docs/current/reference/html/actuator.html#actuator.metrics)

---

## 更新日志

- **2024-01-XX**: 初始版本，集成 Prometheus 和 Grafana
- 添加所有服务的 Prometheus 端点配置
- 创建基础 Spring Boot 指标仪表板

---

**文档维护**: 如有问题或建议，请提交 Issue 或联系项目维护者。

