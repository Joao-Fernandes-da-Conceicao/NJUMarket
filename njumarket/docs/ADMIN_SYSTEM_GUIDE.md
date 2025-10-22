# NJU Market 管理员系统使用说明

## 概述

NJU Market管理员系统是为内部人员设计的后台管理系统，提供管理员账号管理、权限控制等功能。管理员只需要用户名和密码即可登录，无需手机号验证。

## 系统特性

### 1. 简化的登录机制
- **仅需用户名和密码**：内部人员使用，无需手机号验证
- **JWT Token认证**：安全的Token机制
- **Session管理**：支持Session和Token双重认证

### 2. 权限分级管理
- **SUPER（超级管理员）**：拥有所有权限
- **SENIOR（高级管理员）**：拥有大部分管理权限
- **NORMAL（普通管理员）**：拥有基础管理权限

### 3. 完整的账号管理
- 创建、更新、删除管理员账号
- 密码重置和修改
- 账号状态管理
- 登录记录追踪

## 数据库结构

### 管理员表 (admins)
```sql
CREATE TABLE `admins` (
    `admin_id` VARCHAR(50) NOT NULL COMMENT '管理员ID',
    `username` VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名',
    `password` VARCHAR(255) NOT NULL COMMENT '密码（加密存储）',
    `real_name` VARCHAR(50) DEFAULT NULL COMMENT '真实姓名',
    `email` VARCHAR(100) DEFAULT NULL COMMENT '邮箱',
    `department` VARCHAR(50) DEFAULT NULL COMMENT '部门',
    `position` VARCHAR(50) DEFAULT NULL COMMENT '职位',
    `admin_level` VARCHAR(20) NOT NULL DEFAULT 'NORMAL' COMMENT '管理员级别',
    `permissions` TEXT DEFAULT NULL COMMENT '权限列表（JSON格式）',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `last_login_time` DATETIME DEFAULT NULL COMMENT '最后登录时间',
    `last_login_ip` VARCHAR(50) DEFAULT NULL COMMENT '最后登录IP',
    `account_status` VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' COMMENT '账户状态',
    `login_count` INT NOT NULL DEFAULT 0 COMMENT '登录次数',
    `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
    PRIMARY KEY (`admin_id`)
);
```

## API接口

### 1. 管理员登录
```http
POST /api/admin/login
Content-Type: application/json

{
    "username": "admin",
    "password": "admin123"
}
```

**响应示例：**
```json
{
    "code": 200,
    "message": "登录成功",
    "data": {
        "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
        "adminId": "ADMIN_SUPER_001",
        "username": "admin",
        "adminLevel": "SUPER",
        "expiresIn": 86400
    }
}
```

### 2. 获取当前管理员信息
```http
GET /api/admin/me
Authorization: Bearer {token}
```

### 3. 管理员登出
```http
POST /api/admin/logout
Authorization: Bearer {token}
```

### 4. 创建管理员
```http
POST /api/admin/create
Authorization: Bearer {token}
Content-Type: application/json

{
    "username": "newadmin",
    "password": "password123",
    "realName": "新管理员",
    "email": "newadmin@njumarket.com",
    "department": "技术部",
    "position": "开发工程师",
    "adminLevel": "NORMAL",
    "permissions": "user:view,commodity:view",
    "remark": "新创建的管理员账号"
}
```

### 5. 获取管理员列表
```http
GET /api/admin/list?page=0&size=10&keyword=
Authorization: Bearer {token}
```

### 6. 更新管理员信息
```http
PUT /api/admin/{adminId}
Authorization: Bearer {token}
Content-Type: application/json

{
    "realName": "更新后的姓名",
    "email": "updated@njumarket.com",
    "department": "运营部",
    "position": "运营经理"
}
```

### 7. 删除管理员
```http
DELETE /api/admin/{adminId}
Authorization: Bearer {token}
```

### 8. 重置密码
```http
PUT /api/admin/{adminId}/reset-password?newPassword=newpass123
Authorization: Bearer {token}
```

### 9. 修改密码
```http
PUT /api/admin/change-password?adminId={adminId}&oldPassword=oldpass&newPassword=newpass
Authorization: Bearer {token}
```

### 10. 更新管理员状态
```http
PUT /api/admin/{adminId}/status?status=SUSPENDED
Authorization: Bearer {token}
```

### 11. 获取管理员统计信息
```http
GET /api/admin/statistics
Authorization: Bearer {token}
```

## 默认账号

### 超级管理员
- **用户名**: `admin`
- **密码**: `admin123`
- **级别**: SUPER
- **权限**: 所有权限

### 普通管理员
- **用户名**: `manager`
- **密码**: `manager123`
- **级别**: NORMAL
- **权限**: 基础查看权限

## 权限系统

### 权限格式
权限以逗号分隔的字符串形式存储，例如：
```
"user:view,user:edit,commodity:view,commodity:edit,order:view,complaint:view"
```

### 权限分类
- **user:** - 用户管理权限
- **commodity:** - 商品管理权限
- **order:** - 订单管理权限
- **complaint:** - 投诉管理权限
- **admin:** - 管理员管理权限
- **system:** - 系统配置权限

### 权限操作
- **view** - 查看权限
- **edit** - 编辑权限
- **delete** - 删除权限
- **create** - 创建权限

## 安全配置

### 1. 登录拦截器配置
```java
// WebConfig.java
registry.addInterceptor(loginInterceptor)
    .addPathPatterns("/api/user/**", "/api/admin/**")
    .excludePathPatterns(
        "/api/user/auth/login",
        "/api/admin/login"
    );
```

### 2. CORS配置
```java
registry.addMapping("/api/**")
    .allowedOriginPatterns("*")
    .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
    .allowedHeaders("*")
    .allowCredentials(true)
    .maxAge(3600);
```

## 部署说明

### 1. 数据库初始化
执行SQL脚本创建管理员表：
```bash
mysql -u root -p nju_market < src/main/resources/database/create_admin_tables.sql
```

### 2. 修改默认密码
**重要**：部署后请立即修改默认管理员密码！

### 3. 环境配置
确保以下配置正确：
- 数据库连接配置
- Redis连接配置
- JWT密钥配置

## 使用流程

### 1. 首次部署
1. 执行数据库脚本创建管理员表
2. 使用默认账号 `admin/admin123` 登录
3. 立即修改默认密码
4. 创建其他管理员账号

### 2. 日常管理
1. 管理员登录系统
2. 查看用户、商品、订单等信息
3. 处理投诉和审核
4. 管理其他管理员账号

### 3. 权限管理
1. 根据部门职责分配权限
2. 定期审查权限设置
3. 及时禁用离职管理员账号

## 注意事项

1. **密码安全**：使用强密码，定期更换
2. **权限最小化**：只分配必要的权限
3. **日志监控**：关注登录日志和操作记录
4. **备份数据**：定期备份管理员数据
5. **网络安全**：使用HTTPS传输

## 故障排除

### 1. 登录失败
- 检查用户名和密码是否正确
- 确认账号状态为ACTIVE
- 检查JWT Token是否有效

### 2. 权限不足
- 确认管理员级别
- 检查权限配置
- 联系超级管理员

### 3. 数据库连接问题
- 检查数据库配置
- 确认表结构正确
- 查看错误日志

---

**文档版本**: 1.0  
**最后更新**: 2025-01-22  
**维护人员**: NJU Market开发团队
