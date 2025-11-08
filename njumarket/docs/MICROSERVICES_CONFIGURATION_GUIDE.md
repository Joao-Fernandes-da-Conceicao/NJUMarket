# NJUMarket 微服务配置指南

## 📋 目录
- [环境要求](#环境要求)
- [基础配置](#基础配置)
- [服务配置](#服务配置)
- [数据库配置](#数据库配置)
- [Redis配置](#redis配置)
- [安全配置](#安全配置)
- [性能优化](#性能优化)
- [生产环境建议](#生产环境建议)

---

## 环境要求

### 开发环境

- **JDK**: 17+
- **Maven**: 3.6+
- **MySQL**: 8.0+
- **Redis**: 6.0+
- **IDE**: IntelliJ IDEA / Eclipse

### 生产环境

- **操作系统**: Linux (CentOS 7+ / Ubuntu 20.04+)
- **JVM**: OpenJDK 17
- **数据库**: MySQL 8.0+ (主从复制)
- **缓存**: Redis 6.0+ (集群模式)
- **容器**: Docker (可选)
- **编排**: Kubernetes (可选)

---

## 基础配置

### 1. Maven配置

**父POM配置** (`pom.xml`):

```xml
<properties>
    <java.version>17</java.version>
    <spring-boot.version>3.2.0</spring-boot.version>
    <spring-cloud.version>2023.0.3</spring-cloud.version>
</properties>
```

### 2. Java版本

所有服务使用 **Java 17**，确保JVM参数配置：

```bash
-Xms512m -Xmx1024m -XX:+UseG1GC
```

---

## 服务配置

### 1. Eureka Server (Discovery)

**application.yml**:
```yaml
server:
  port: 8761

spring:
  application:
    name: njumarket-discovery

eureka:
  instance:
    hostname: localhost
  client:
    register-with-eureka: false
    fetch-registry: false
    service-url:
      defaultZone: http://${eureka.instance.hostname}:${server.port}/eureka/
```

**生产环境建议**:
- 启用安全认证
- 配置多节点集群（高可用）

### 2. API Gateway

**application.yml**:
```yaml
server:
  port: 8080

spring:
  application:
    name: njumarket-gateway
  cloud:
    gateway:
      routes:
        - id: auth-service
          uri: lb://njumarket-service-auth
          predicates:
            - Path=/auth/**
        - id: commodity-service
          uri: lb://njumarket-service-commodity
          predicates:
            - Path=/commodity/**
        - id: order-service
          uri: lb://njumarket-service-order
          predicates:
            - Path=/order/**
        - id: message-service
          uri: lb://njumarket-service-message
          predicates:
            - Path=/message/**

eureka:
  client:
    service-url:
      defaultZone: http://localhost:8761/eureka/
```

**生产环境建议**:
- 配置CORS策略
- 添加限流规则
- 配置熔断器
- 启用HTTPS

### 3. Auth Service

**application.yml**:
```yaml
server:
  port: 8081

spring:
  application:
    name: njumarket-service-auth
  datasource:
    url: jdbc:mysql://localhost:3306/njumarket_auth?useSSL=false&serverTimezone=UTC&characterEncoding=utf8
    username: root
    password: ${DB_PASSWORD:your_password}
    driver-class-name: com.mysql.cj.jdbc.Driver
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: false
    properties:
      hibernate:
        dialect: org.hibernate.dialect.MySQL8Dialect
        format_sql: true
  data:
    redis:
      host: ${REDIS_HOST:localhost}
      port: ${REDIS_PORT:6379}
      password: ${REDIS_PASSWORD:}
      timeout: 3000ms
      lettuce:
        pool:
          max-active: 8
          max-idle: 8
          min-idle: 0

eureka:
  client:
    service-url:
      defaultZone: http://localhost:8761/eureka/
  instance:
    prefer-ip-address: true

# JWT配置
jwt:
  secret: ${JWT_SECRET:your-secret-key-change-in-production}
  expiration: 86400000 # 24小时
```

### 4. Commodity Service

**application.yml**:
```yaml
server:
  port: 8082

spring:
  application:
    name: njumarket-service-commodity
  datasource:
    url: jdbc:mysql://localhost:3306/njumarket_commodity?useSSL=false&serverTimezone=UTC&characterEncoding=utf8
    username: root
    password: ${DB_PASSWORD:your_password}
  jpa:
    hibernate:
      ddl-auto: update
  data:
    redis:
      host: ${REDIS_HOST:localhost}
      port: ${REDIS_PORT:6379}

eureka:
  client:
    service-url:
      defaultZone: http://localhost:8761/eureka/
```

### 5. Order Service

**application.yml**:
```yaml
server:
  port: 8083

spring:
  application:
    name: njumarket-service-order
  datasource:
    url: jdbc:mysql://localhost:3306/njumarket_order?useSSL=false&serverTimezone=UTC&characterEncoding=utf8
    username: root
    password: ${DB_PASSWORD:your_password}
  jpa:
    hibernate:
      ddl-auto: update

eureka:
  client:
    service-url:
      defaultZone: http://localhost:8761/eureka/
```

### 6. Message Service

**application.yml**:
```yaml
server:
  port: 8084

spring:
  application:
    name: njumarket-service-message
  datasource:
    url: jdbc:mysql://localhost:3306/njumarket_message?useSSL=false&serverTimezone=UTC&characterEncoding=utf8
    username: root
    password: ${DB_PASSWORD:your_password}
  jpa:
    hibernate:
      ddl-auto: update

eureka:
  client:
    service-url:
      defaultZone: http://localhost:8761/eureka/
```

---

## 数据库配置

### 数据库划分建议

**方案一: 独立数据库（推荐）**

每个服务使用独立的数据库：

- `njumarket_auth`: 用户、管理员、用户档案
- `njumarket_commodity`: 商品、商品快照、图片引用
- `njumarket_order`: 订单、订单快照、投诉
- `njumarket_message`: 消息、会话、联系方式

**优点**: 服务完全解耦，可独立扩展

**缺点**: 跨服务查询需要API调用

**方案二: 共享数据库（当前版本）**

所有服务共享一个数据库，使用不同schema或表前缀：

- 共享 `njumarket` 数据库
- 通过表名区分服务（如 `auth_users`, `commodity_commodities`）

**优点**: 迁移简单，跨服务查询方便

**缺点**: 服务耦合度高

### 数据库连接池配置

```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      connection-timeout: 30000
      idle-timeout: 600000
      max-lifetime: 1800000
```

### 数据库初始化

**开发环境**: `ddl-auto: update` (自动创建/更新表)

**生产环境**: `ddl-auto: validate` (仅验证，使用Flyway/Liquibase管理)

---

## Redis配置

### 基础配置

```yaml
spring:
  data:
    redis:
      host: ${REDIS_HOST:localhost}
      port: ${REDIS_PORT:6379}
      password: ${REDIS_PASSWORD:}
      database: 0
      timeout: 3000ms
      lettuce:
        pool:
          max-active: 8
          max-idle: 8
          min-idle: 0
          max-wait: -1ms
```

### Redis使用场景

1. **JWT Token存储**
   - Key: `login:token:{userId}`
   - TTL: 24小时

2. **验证码存储**
   - Key: `code:phone:{phone}`
   - TTL: 5分钟

3. **用户会话信息**
   - Key: `user:session:{userId}`
   - TTL: 30分钟

4. **商品缓存**（可选）
   - Key: `commodity:{commodityId}`
   - TTL: 1小时

5. **消息推送重试队列**
   - List: `message:retry:{userId}`

### 生产环境Redis配置

**单机模式**:
```yaml
spring:
  data:
    redis:
      host: redis-server
      port: 6379
      password: strong_password
```

**集群模式**:
```yaml
spring:
  data:
    redis:
      cluster:
        nodes:
          - redis-node1:6379
          - redis-node2:6379
          - redis-node3:6379
        max-redirects: 3
      password: strong_password
```

---

## 安全配置

### 1. JWT配置

**密钥管理**:
- 开发环境: 使用固定密钥
- 生产环境: 使用环境变量或密钥管理服务

```yaml
jwt:
  secret: ${JWT_SECRET}  # 从环境变量读取
  expiration: 86400000
  refresh-expiration: 604800000  # 7天
```

### 2. 数据库密码

**使用环境变量**:
```bash
export DB_PASSWORD=your_secure_password
export REDIS_PASSWORD=your_redis_password
export JWT_SECRET=your_jwt_secret_key
```

### 3. HTTPS配置

**生产环境启用HTTPS**:
```yaml
server:
  ssl:
    enabled: true
    key-store: classpath:keystore.p12
    key-store-password: ${SSL_KEYSTORE_PASSWORD}
    key-store-type: PKCS12
    key-alias: njumarket
```

### 4. CORS配置

**Gateway CORS配置**:
```java
@Configuration
public class CorsConfig {
    @Bean
    public CorsWebFilter corsWebFilter() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowCredentials(true);
        config.addAllowedOriginPattern("*");
        config.addAllowedHeader("*");
        config.addAllowedMethod("*");
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return new CorsWebFilter(source);
    }
}
```

---

## 性能优化

### 1. JVM参数

**开发环境**:
```bash
-Xms512m -Xmx1024m -XX:+UseG1GC
```

**生产环境**:
```bash
-Xms2g -Xmx4g -XX:+UseG1GC -XX:MaxGCPauseMillis=200
-XX:+HeapDumpOnOutOfMemoryError
-XX:HeapDumpPath=/var/log/heapdump.hprof
```

### 2. 数据库连接池

```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 20  # 根据并发量调整
      minimum-idle: 5
      connection-timeout: 30000
```

### 3. Redis连接池

```yaml
spring:
  data:
    redis:
      lettuce:
        pool:
          max-active: 20  # 根据并发量调整
          max-idle: 10
          min-idle: 5
```

### 4. 服务超时配置

**Gateway超时配置**:
```yaml
spring:
  cloud:
    gateway:
      httpclient:
        connect-timeout: 1000
        response-timeout: 5s
```

---

## 生产环境建议

### 1. 服务部署

**每个服务至少2个实例**:
- 实现高可用
- 负载均衡
- 滚动更新

**启动脚本示例**:
```bash
#!/bin/bash
java -Xms2g -Xmx4g \
  -XX:+UseG1GC \
  -Dspring.profiles.active=prod \
  -jar njumarket-service-auth.jar
```

### 2. 监控配置

**Spring Boot Actuator**:
```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  metrics:
    export:
      prometheus:
        enabled: true
```

**健康检查端点**:
- `/actuator/health`
- `/actuator/info`
- `/actuator/metrics`

### 3. 日志配置

**Logback配置** (`logback-spring.xml`):
```xml
<configuration>
    <appender name="FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>logs/njumarket-service-auth.log</file>
        <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
            <fileNamePattern>logs/njumarket-service-auth.%d{yyyy-MM-dd}.log</fileNamePattern>
            <maxHistory>30</maxHistory>
        </rollingPolicy>
        <encoder>
            <pattern>%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n</pattern>
        </encoder>
    </appender>
    
    <root level="INFO">
        <appender-ref ref="FILE" />
    </root>
</configuration>
```

### 4. 环境变量管理

**使用环境变量**:
```bash
# .env 文件
DB_PASSWORD=secure_password
REDIS_PASSWORD=redis_password
JWT_SECRET=jwt_secret_key
SPRING_PROFILES_ACTIVE=prod
```

**Docker环境变量**:
```yaml
# docker-compose.yml
environment:
  - DB_PASSWORD=${DB_PASSWORD}
  - REDIS_PASSWORD=${REDIS_PASSWORD}
  - JWT_SECRET=${JWT_SECRET}
```

### 5. 服务发现配置

**Eureka高可用**:
```yaml
# eureka-server-1
eureka:
  client:
    service-url:
      defaultZone: http://eureka-server-2:8761/eureka/

# eureka-server-2
eureka:
  client:
    service-url:
      defaultZone: http://eureka-server-1:8761/eureka/
```

### 6. 网关配置优化

**限流配置**:
```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: auth-service
          filters:
            - name: RequestRateLimiter
              args:
                redis-rate-limiter.replenishRate: 10
                redis-rate-limiter.burstCapacity: 20
```

**熔断配置**:
```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: commodity-service
          filters:
            - name: CircuitBreaker
              args:
                name: commodityService
                fallbackUri: forward:/fallback
```

---

## 配置检查清单

### 开发环境

- [ ] 所有服务端口不冲突
- [ ] Eureka Server正常启动
- [ ] 各服务成功注册到Eureka
- [ ] Gateway路由配置正确
- [ ] 数据库连接正常
- [ ] Redis连接正常
- [ ] JWT配置正确

### 生产环境

- [ ] 使用环境变量管理敏感信息
- [ ] 启用HTTPS
- [ ] 配置CORS策略
- [ ] 配置服务超时
- [ ] 配置连接池大小
- [ ] 启用日志文件输出
- [ ] 配置健康检查
- [ ] 配置监控指标
- [ ] 每个服务至少2个实例
- [ ] 配置负载均衡
- [ ] 配置数据库主从复制
- [ ] 配置Redis集群（如需要）

---

## 故障排查

### 服务无法注册到Eureka

**检查项**:
1. Eureka Server是否启动
2. 服务配置中的Eureka地址是否正确
3. 网络连接是否正常
4. 服务名称是否唯一

### Gateway无法路由

**检查项**:
1. Gateway是否注册到Eureka
2. 路由配置是否正确
3. 目标服务是否已注册
4. 服务名称是否匹配

### 数据库连接失败

**检查项**:
1. 数据库服务是否启动
2. 连接URL是否正确
3. 用户名密码是否正确
4. 数据库是否存在
5. 防火墙规则

### Redis连接失败

**检查项**:
1. Redis服务是否启动
2. 连接地址和端口是否正确
3. 密码是否正确（如配置）
4. 网络连接是否正常

---

## 总结

微服务配置需要根据实际业务需求和环境进行调整。开发环境可以使用简化配置，生产环境需要关注安全性、性能和可维护性。建议使用配置中心（如Spring Cloud Config或Nacos）统一管理配置，提高配置管理的效率。

