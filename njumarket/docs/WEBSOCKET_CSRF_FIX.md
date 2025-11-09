# WebSocket CORS 问题修复文档

## 问题描述

**CORS重复问题**：WebSocket连接时出现CORS头重复的问题，`Access-Control-Allow-Origin`头出现多次，导致浏览器拒绝请求。

错误信息：
```
Access to XMLHttpRequest at 'http://localhost:8080/api/ws/info?token=...' from origin 'http://localhost:8081' 
has been blocked by CORS policy: The 'Access-Control-Allow-Origin' header contains multiple values 
'http://localhost:8081, http://localhost:8081', but only one is allowed.
```

## 问题原因分析

1. **Gateway的全局CORS配置**：Gateway的全局CORS配置（`[/**]`）会为所有路径添加CORS头，包括WebSocket路径
2. **服务端的CORS配置**：Notification Service的WebSocket配置（`setAllowedOriginPatterns`）也会添加CORS头
3. **重复添加**：导致`Access-Control-Allow-Origin`头出现多次，浏览器拒绝请求

## 修复方案

### 1. Gateway - 全局CORS配置优化

**文件位置**：`njumarket-gateway/src/main/resources/application.yml`

**修复内容**：
- 从全局CORS配置中排除WebSocket路径（`/api/ws/**`）
- 只配置其他API路径的CORS，避免Gateway为WebSocket路径添加CORS头
- WebSocket路径的CORS完全由服务端处理

**关键配置**：
```yaml
globalcors:
  cors-configurations:
    # 排除WebSocket路径，只配置其他API路径的CORS
    '[/api/user/**]':
      allowedOriginPatterns: "*"
      # ... 其他配置
    # 注意：/api/ws/** 不在此配置中，由服务端处理CORS
```

### 5. Gateway - CorsFilter 优化

**文件位置**：`njumarket-gateway/src/main/java/com/njumarket/gateway/filter/CorsFilter.java`

**修复内容**：
- 优化路径匹配，确保匹配所有WebSocket相关路径（包括`/api/ws/info`）
- 移除所有Gateway可能添加的CORS头，包括Vary头
- 清理Vary头中的CORS相关值

**关键代码**：
```java
// 匹配所有WebSocket相关路径
if (requestURI.startsWith("/api/ws")) {
    // 移除所有CORS相关头，包括Vary头
    // 服务端会自己添加正确的CORS头
}
```

## 修复效果

1. **彻底解决CORS重复问题**：Gateway不再为WebSocket路径添加CORS头，完全由服务端处理
2. **保持安全性**：实际的WebSocket连接仍需要JWT验证和X-User-Id验证
3. **规范处理**：遵循Spring Cloud Gateway和SockJS的最佳实践
4. **兼容性好**：支持SockJS的所有传输方式

## 验证方法

1. 启动所有服务（Gateway、Notification Service等）
2. 前端建立WebSocket连接
3. 检查浏览器开发者工具的网络请求，确认：
   - SockJS的info端点请求成功（不需要JWT）
   - WebSocket握手请求成功（需要JWT和X-User-Id）
   - 响应头中`Access-Control-Allow-Origin`只出现一次（由服务端添加）
   - 没有CORS重复的错误
4. 检查日志，确认：
   - CorsFilter正确移除了Gateway添加的CORS头
   - WebSocket握手成功

## 注意事项

1. **生产环境**：建议将`setAllowedOriginPatterns("*")`改为具体的域名白名单
2. **安全性**：虽然禁用了CSRF，但通过JWT验证和X-User-Id验证保证了安全性
3. **监控**：建议监控WebSocket连接的成功率和错误日志

## 相关文件

- `njumarket-gateway/src/main/java/com/njumarket/gateway/filter/CorsFilter.java`
- `njumarket-gateway/src/main/resources/application.yml` (WebSocket路由配置和全局CORS配置)

