# 南大集市 NJUMarket

一个基于微服务架构的校园二手交易平台。

## 📋 项目简介

NJUMarket 是一个采用微服务架构的校园二手交易平台，支持商品发布、订单管理、实时消息等功能。

- **架构**: 微服务架构（Spring Cloud）
- **后端**: Spring Boot 3.2.0 + Spring Cloud 2023.0.3
- **前端**: Vue 3 + Element Plus
- **数据库**: MySQL 8.0+
- **缓存**: Redis 6.0+

## 🚀 快速开始

### 前置要求

- **JDK**: 17+
- **Maven**: 3.6+
- **MySQL**: 8.0+
- **Redis**: 6.0+
- **IDE**: IntelliJ IDEA / VS Code（推荐）

### 配置步骤

#### 1. 克隆项目

```bash
git clone <repository-url>
cd NJUMarket/njumarket
```

#### 2. 配置数据库

**创建数据库**：
```sql
CREATE DATABASE nju_market CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

**初始化数据库结构**：
```bash
mysql -u root -p nju_market < database/schema.sql
```

**配置数据库连接**：
修改各服务的 `application.yml` 中的数据库配置：
```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/nju_market?useSSL=false&useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai
    username: your_username
    password: your_password
```

#### 3. 配置Redis

确保Redis服务已启动，默认配置：
- **Host**: localhost
- **Port**: 6379
- **Password**: 根据实际情况修改（可在 `application.yml` 中使用环境变量）

#### 4. 导入项目到IDE

**IntelliJ IDEA**：
1. File → Open → 选择 `njumarket` 目录
2. 等待Maven自动导入依赖
3. 确保JDK版本为17+

**VS Code**：
1. 打开 `njumarket` 目录
2. 安装Java扩展包（Extension Pack for Java）
3. 等待Maven依赖下载完成

#### 5. 启动服务

**启动顺序**：
1. **Eureka Discovery Server** (端口: 8761)
   - 运行 `njumarket-discovery` 模块的 `DiscoveryServerApplication`
   - 验证: 访问 http://localhost:8761

2. **API Gateway** (端口: 8080)
   - 运行 `njumarket-gateway` 模块的 `GatewayApplication`

3. **各微服务**（可并行启动）
   - `AuthServiceApplication` (8091) - 认证服务
   - `CommodityServiceApplication` (8092) - 商品服务
   - `OrderServiceApplication` (8093) - 订单服务
   - `MessageServiceApplication` (8094) - 消息服务
   - `ImageServiceApplication` (8095) - 图片服务
   - `AdminServiceApplication` (8096) - 管理服务

**验证服务注册**：
访问 http://localhost:8761 查看Eureka控制台，确认所有服务已注册。

#### 6. 测试API

通过Gateway访问API：
```bash
# 测试健康检查
curl http://localhost:8080/actuator/health

# 测试商品搜索
curl http://localhost:8080/api/public/commodity/search
```

## 📁 项目结构

```
njumarket/
├── pom.xml                          # 父POM文件
├── mvnw, mvnw.cmd                   # Maven Wrapper
│
├── njumarket-common/                # 公共模块
│   └── 实体类、DTO、工具类、异常处理等
│
├── njumarket-discovery/             # 服务注册中心（Eureka）
│   └── DiscoveryServerApplication
│
├── njumarket-gateway/               # API网关
│   └── GatewayApplication
│
├── njumarket-service-auth/          # 认证服务
│   └── AuthServiceApplication
│
├── njumarket-service-commodity/     # 商品服务
│   └── CommodityServiceApplication
│
├── njumarket-service-order/        # 订单服务
│   └── OrderServiceApplication
│
├── njumarket-service-message/      # 消息服务
│   └── MessageServiceApplication
│
├── njumarket-service-image/        # 图片服务
│   └── ImageServiceApplication
│
├── njumarket-service-admin/        # 管理服务
│   └── AdminServiceApplication
│
├── database/                       # 数据库脚本
│   ├── schema.sql                  # 数据库结构
│   └── README.md                   # 数据库说明
│
├── docs/                          # 项目文档
│   └── PROJECT_DOCUMENTATION_V2.0.md  # 完整文档
│
└── scripts/                        # 测试脚本
    ├── batch_create_users_simple.py  # 批量创建用户
    └── 线程组.jmx                    # JMeter测试计划
```

## 🔧 服务端口

| 服务 | 端口 | 说明 |
|------|------|------|
| Eureka Discovery | 8761 | 服务注册中心 |
| API Gateway | 8080 | API网关（统一入口） |
| Auth Service | 8091 | 认证服务 |
| Commodity Service | 8092 | 商品服务 |
| Order Service | 8093 | 订单服务 |
| Message Service | 8094 | 消息服务 |
| Image Service | 8095 | 图片服务 |
| Admin Service | 8096 | 管理服务 |

## 📝 重要说明

### 环境变量配置

建议使用环境变量配置敏感信息：

```bash
# Windows
set REDIS_PASSWORD=your_password
set DB_PASSWORD=your_password

# Linux/Mac
export REDIS_PASSWORD=your_password
export DB_PASSWORD=your_password
```

### 数据库初始化

- `database/schema.sql` 仅包含数据库结构，**不包含测试数据**
- 测试用户可通过后端注册API创建
- 管理员账号需要手动创建（参考 `database/README.md`）

### 测试数据

使用 `scripts/batch_create_users_simple.py` 批量创建测试用户：

```bash
cd scripts
pip install -r requirements.txt
python batch_create_users_simple.py
```

## 📚 相关文档

- **完整文档**: `docs/PROJECT_DOCUMENTATION_V2.0.md`
- **数据库说明**: `database/README.md`
- **测试脚本**: `scripts/README.md`

## ⚠️ 常见问题

### 服务无法启动

1. **检查端口占用**：确保8761, 8080, 8091-8096未被占用
2. **检查数据库连接**：确认MySQL服务运行正常，数据库已创建
3. **检查Redis连接**：确认Redis服务运行正常
4. **检查JDK版本**：确保使用JDK 17+

### 服务无法注册到Eureka

1. **检查Eureka Server**：确保Discovery Server已启动
2. **检查配置**：确认各服务的Eureka地址配置正确
3. **检查网络**：确认服务间网络连通

### Maven依赖下载失败

1. **检查网络**：确保能访问Maven中央仓库
2. **清理缓存**：删除 `~/.m2/repository` 后重新下载
3. **使用镜像**：配置Maven镜像（如阿里云镜像）

## 🎯 开发建议

1. **使用IDE运行**：推荐使用IntelliJ IDEA，可直接运行各服务的Application类
2. **查看日志**：各服务日志会输出到控制台，注意查看错误信息
3. **Eureka控制台**：定期查看 http://localhost:8761 确认服务状态
4. **API测试**：使用Postman或curl测试API接口

## 📄 许可证

本项目为软件工程课程项目。

---

**提示**：更多详细信息请参考 `docs/PROJECT_DOCUMENTATION_V2.0.md`

