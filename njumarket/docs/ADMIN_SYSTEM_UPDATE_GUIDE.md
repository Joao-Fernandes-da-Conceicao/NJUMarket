# NJU Market 管理员系统更新说明

## 权限级别更新

根据新的需求，管理员系统已从三级权限（SUPER、SENIOR、NORMAL）简化为两级权限：

### 1. system（系统管理员）
- **唯一性**：系统中只能有一个system级别的管理员
- **权限范围**：拥有所有权限，可以管理所有管理员账号
- **特殊限制**：不能删除自己

### 2. administrator（普通管理员）
- **数量限制**：可以有多个administrator级别的管理员
- **权限范围**：只能更改自己的账号数据
- **限制**：不能更改和删除其他管理员的账号

## 权限控制规则

### system管理员权限
```java
// 系统管理员拥有所有权限
if (admin.isSystemAdmin()) {
    return true;
}
```

**允许的操作：**
- ✅ 创建管理员账号
- ✅ 删除管理员账号（除了自己）
- ✅ 重置任何管理员的密码
- ✅ 更新任何管理员的权限
- ✅ 更新任何管理员的状态
- ✅ 获取管理员统计信息
- ✅ 更新任何管理员的信息
- ✅ 所有其他操作

**限制：**
- ❌ 不能删除自己

### administrator管理员权限
```java
// 普通管理员权限受限
if (admin.isAdministrator()) {
    // 不能执行敏感操作
    if (requestURI.contains("/api/admin/create")) return false;
    if (requestURI.matches("/api/admin/[^/]+$") && "DELETE".equals(method)) return false;
    if (requestURI.contains("/reset-password")) return false;
    if (requestURI.contains("/permissions")) return false;
    if (requestURI.contains("/status")) return false;
    if (requestURI.contains("/statistics")) return false;
    
    // 只能更新自己的信息
    if (requestURI.matches("/api/admin/[^/]+$") && "PUT".equals(method)) {
        String targetAdminId = pathParts[3];
        if (!admin.getAdminId().equals(targetAdminId)) {
            return false;
        }
    }
}
```

**允许的操作：**
- ✅ 查看管理员列表
- ✅ 查看管理员详情
- ✅ 修改自己的密码
- ✅ 更新自己的基本信息
- ✅ 查看自己的信息

**限制：**
- ❌ 创建管理员账号
- ❌ 删除管理员账号
- ❌ 重置密码
- ❌ 更新权限
- ❌ 更新管理员状态
- ❌ 获取管理员统计信息
- ❌ 更新其他管理员的信息

## 数据库更新

### 管理员表结构
```sql
CREATE TABLE `admins` (
    `admin_id` VARCHAR(50) NOT NULL COMMENT '管理员ID',
    `username` VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名',
    `password` VARCHAR(255) NOT NULL COMMENT '密码（加密存储）',
    `real_name` VARCHAR(50) DEFAULT NULL COMMENT '真实姓名',
    `email` VARCHAR(100) DEFAULT NULL COMMENT '邮箱',
    `department` VARCHAR(50) DEFAULT NULL COMMENT '部门',
    `position` VARCHAR(50) DEFAULT NULL COMMENT '职位',
    `admin_level` VARCHAR(20) NOT NULL DEFAULT 'administrator' COMMENT '管理员级别：system-系统管理员，administrator-普通管理员',
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

### 默认账号

#### 系统管理员
- **用户名**: `system`
- **密码**: `system123`
- **级别**: system
- **权限**: 所有权限

#### 普通管理员
- **用户名**: `manager`
- **密码**: `manager123`
- **级别**: administrator
- **权限**: 基础查看权限

## API接口更新

### 1. 管理员登录（无变化）
```http
POST /api/admin/login
Content-Type: application/json

{
    "username": "system",
    "password": "system123"
}
```

### 2. 创建管理员（仅system可访问）
```http
POST /api/admin/create
Authorization: Bearer {system_token}
Content-Type: application/json

{
    "username": "newadmin",
    "password": "password123",
    "realName": "新管理员",
    "email": "newadmin@njumarket.com",
    "department": "技术部",
    "position": "开发工程师",
    "adminLevel": "administrator",
    "permissions": "user:view,commodity:view",
    "remark": "新创建的管理员账号"
}
```

### 3. 更新管理员信息
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

**权限说明：**
- system管理员：可以更新任何管理员的信息
- administrator管理员：只能更新自己的信息

### 4. 删除管理员（仅system可访问）
```http
DELETE /api/admin/{adminId}
Authorization: Bearer {system_token}
```

**限制：**
- system管理员不能删除自己

### 5. 重置密码（仅system可访问）
```http
PUT /api/admin/{adminId}/reset-password?newPassword=newpass123
Authorization: Bearer {system_token}
```

### 6. 修改密码（所有管理员可访问）
```http
PUT /api/admin/change-password?adminId={adminId}&oldPassword=oldpass&newPassword=newpass
Authorization: Bearer {token}
```

**权限说明：**
- 所有管理员都可以修改自己的密码
- administrator管理员不能修改其他管理员的密码

## 权限验证示例

### 1. system管理员操作
```http
# 创建管理员 - 成功
POST /api/admin/create
Authorization: Bearer {system_token}
→ 200 OK

# 删除管理员 - 成功
DELETE /api/admin/ADMIN_ADMINISTRATOR_001
Authorization: Bearer {system_token}
→ 200 OK

# 删除自己 - 失败
DELETE /api/admin/ADMIN_SYSTEM_001
Authorization: Bearer {system_token}
→ 403 Forbidden: "不能删除系统管理员"
```

### 2. administrator管理员操作
```http
# 创建管理员 - 失败
POST /api/admin/create
Authorization: Bearer {administrator_token}
→ 403 Forbidden: "权限不足，无法执行此操作"

# 删除管理员 - 失败
DELETE /api/admin/ADMIN_ADMINISTRATOR_002
Authorization: Bearer {administrator_token}
→ 403 Forbidden: "权限不足，无法执行此操作"

# 更新自己的信息 - 成功
PUT /api/admin/ADMIN_ADMINISTRATOR_001
Authorization: Bearer {administrator_token}
→ 200 OK

# 更新其他管理员信息 - 失败
PUT /api/admin/ADMIN_ADMINISTRATOR_002
Authorization: Bearer {administrator_token}
→ 403 Forbidden: "权限不足，无法执行此操作"
```

## 迁移指南

### 1. 数据库迁移
如果从旧版本升级，需要执行以下SQL：

```sql
-- 更新现有管理员的级别
UPDATE `admins` SET `admin_level` = 'system' WHERE `admin_level` = 'SUPER';
UPDATE `admins` SET `admin_level` = 'administrator' WHERE `admin_level` IN ('SENIOR', 'NORMAL');

-- 确保只有一个system管理员
-- 如果有多个system管理员，需要手动调整
```

### 2. 代码更新
- 更新所有引用旧权限级别的地方
- 更新权限检查逻辑
- 更新前端权限显示

### 3. 测试验证
- 测试system管理员的完整权限
- 测试administrator管理员的受限权限
- 验证权限边界条件

## 安全建议

### 1. system管理员安全
- 使用强密码
- 定期更换密码
- 限制登录IP
- 监控登录日志

### 2. administrator管理员管理
- 定期审查administrator管理员列表
- 及时禁用离职管理员
- 监控权限使用情况

### 3. 系统安全
- 定期备份管理员数据
- 监控异常操作
- 实施操作日志记录

---

**文档版本**: 2.0  
**最后更新**: 2025-01-22  
**维护人员**: NJU Market开发团队
