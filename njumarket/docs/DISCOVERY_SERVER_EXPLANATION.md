# Discovery Server (Eureka) 功能说明

## 📋 概述

`njumarket-discovery` 模块是 **Eureka 服务发现服务器**，虽然代码量很少，但它是整个微服务架构的**核心基础设施**。

## ✅ 已实现的功能

### 1. 服务注册中心

**作用**：所有微服务启动时都会向 Eureka 注册自己的信息（服务名、IP、端口）

**实现**：
- `@EnableEurekaServer` 注解启用 Eureka Server
- 配置端口 8761
- 不向自身注册（`register-with-eureka: false`）

### 2. 服务发现

**作用**：其他服务（Gateway、Feign Client）通过 Eureka 查找可用的服务实例

**工作流程**：
```
1. Auth Service 启动 → 向 Eureka 注册（服务名：njumarket-service-auth，端口：8091）
2. Commodity Service 启动 → 向 Eureka 注册（服务名：njumarket-service-commodity，端口：8092）
3. Gateway 启动 → 从 Eureka 获取所有服务列表
4. 前端请求 → Gateway 通过服务名查找实例 → 转发请求
```

### 3. 健康检查

**作用**：Eureka 定期检查服务是否在线，自动剔除下线服务

**机制**：
- 服务每30秒发送心跳
- 90秒未收到心跳则标记为下线
- 自动从服务列表中移除

## 🔍 为什么代码这么少？

Eureka Server 是 **基础设施组件**，Spring Cloud 已经封装好了大部分功能：

1. **自动服务注册**：其他服务通过 `@EnableDiscoveryClient` 自动注册
2. **自动服务发现**：Gateway 和 Feign Client 自动从 Eureka 获取服务列表
3. **自动负载均衡**：Feign 自动选择可用实例
4. **自动健康检查**：Eureka 自动监控服务状态

**类比**：就像数据库服务器，虽然代码少，但它是整个系统的数据存储中心。

## 📊 实际运行效果

### 启动后访问 http://localhost:8761

你会看到 Eureka 控制台，显示：
- **已注册的服务列表**：
  - njumarket-gateway (1 instance)
  - njumarket-service-auth (1 instance)
  - njumarket-service-commodity (1 instance)
  - njumarket-service-order (1 instance)
  - njumarket-service-message (1 instance)

- **每个服务的实例信息**：
  - IP地址
  - 端口
  - 健康状态
  - 最后心跳时间

### Gateway 如何使用 Eureka

```yaml
# Gateway 配置
routes:
  - id: auth-service
    uri: lb://njumarket-service-auth  # lb = load balance，从Eureka获取实例列表
```

**工作流程**：
1. Gateway 启动时从 Eureka 获取 `njumarket-service-auth` 的所有实例
2. 收到请求时，通过负载均衡算法选择一个实例
3. 转发请求到该实例

### Feign Client 如何使用 Eureka

```java
@FeignClient(name = "njumarket-service-commodity")  // 通过服务名查找
public interface CommodityClient {
    // ...
}
```

**工作流程**：
1. Feign Client 启动时从 Eureka 获取 `njumarket-service-commodity` 的所有实例
2. 调用时自动选择可用实例
3. 如果实例下线，自动切换到其他实例

## 🎯 核心价值

### 1. 解耦服务地址

**没有 Eureka**：
```java
// ❌ 硬编码服务地址
@FeignClient(url = "http://localhost:8092")
```

**有 Eureka**：
```java
// ✅ 通过服务名查找，支持多实例、负载均衡
@FeignClient(name = "njumarket-service-commodity")
```

### 2. 支持水平扩展

**场景**：Commodity Service 需要扩容

**没有 Eureka**：
- 需要手动配置多个实例地址
- 需要手动实现负载均衡

**有 Eureka**：
- 启动多个实例，自动注册到 Eureka
- Feign 自动负载均衡
- 无需修改任何代码

### 3. 自动故障转移

**场景**：某个服务实例宕机

**没有 Eureka**：
- 需要手动从配置中移除
- 可能导致请求失败

**有 Eureka**：
- 自动检测到服务下线
- 自动从服务列表中移除
- Feign 自动切换到其他实例

## 📝 配置说明

### 当前配置

```yaml
server:
  port: 8761

spring:
  application:
    name: njumarket-discovery

eureka:
  client:
    register-with-eureka: false  # Eureka Server 不向自身注册
    fetch-registry: false        # Eureka Server 不需要获取注册表
```

### 生产环境建议

```yaml
eureka:
  server:
    enable-self-preservation: false  # 关闭自我保护（开发环境）
    eviction-interval-timer-in-ms: 5000  # 清理间隔
  instance:
    hostname: eureka-server-1
    prefer-ip-address: true
  client:
    register-with-eureka: true   # 集群模式需要相互注册
    fetch-registry: true
    service-url:
      defaultZone: http://eureka-server-2:8761/eureka/,http://eureka-server-3:8761/eureka/
```

## 🔗 与其他组件的关系

```
┌─────────────────┐
│  Eureka Server  │ ← 服务注册中心
│    (8761)       │
└────────┬────────┘
         │
    ┌────┴────┬──────────┬──────────┬──────────┐
    │         │          │          │          │
┌───▼───┐ ┌──▼───┐ ┌────▼────┐ ┌───▼────┐ ┌───▼────┐
│Gateway│ │ Auth │ │Commodity│ │ Order  │ │Message │
│ (8080)│ │(8091)│ │  (8092) │ │ (8093) │ │ (8094) │
└───────┘ └──────┘ └─────────┘ └────────┘ └────────┘
    │         │          │          │          │
    └─────────┴──────────┴──────────┴──────────┘
             所有服务都向 Eureka 注册
```

## 💡 总结

**Discovery 模块虽然代码少，但它是微服务架构的"大脑"**：

1. ✅ **已实现**：服务注册、服务发现、健康检查
2. ✅ **自动工作**：无需额外代码，Spring Cloud 自动处理
3. ✅ **核心价值**：解耦服务地址、支持扩展、自动故障转移

**类比**：
- 就像 DNS 服务器，虽然代码少，但所有域名解析都依赖它
- 就像电话簿，虽然简单，但所有人都需要它来找到对方

## 🚀 验证方法

1. 启动 Discovery Server
2. 访问 http://localhost:8761
3. 查看 Eureka 控制台，应该看到所有已注册的服务
4. 停止某个服务，等待90秒后刷新，该服务会从列表中消失

---

**结论**：Discovery 模块不是预留的，它已经完整实现了服务注册与发现功能，是整个微服务架构的基础设施。
