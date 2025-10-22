# 用户密码字段更新说明

## 概述

本次更新为用户表(`users`)添加了密码相关字段，以支持账号密码登录功能。

## 数据库更新

### 1. 执行更新脚本

#### 方式一：标准版本（需要禁用安全模式）
```sql
-- 执行标准更新脚本
source update_add_password.sql;
```

#### 方式二：安全模式兼容版本（推荐）
```sql
-- 执行安全模式兼容脚本
source update_add_password_safe.sql;
```

#### 方式三：手动处理安全模式错误
如果遇到错误 `Error Code: 1175`，可以：

**选项A：临时禁用安全模式**
```sql
SET SQL_SAFE_UPDATES = 0;
-- 执行更新语句
UPDATE users SET password = '$2a$10$N.zmdr9k7uOIQzUHPPLOPOxrOVJ2eswjzfoy9rI8.sChyZwta7aaa', password_updated_at = NOW() WHERE password IS NULL;
SET SQL_SAFE_UPDATES = 1;
```

**选项B：在MySQL Workbench中禁用安全模式**
1. 打开 `Edit` → `Preferences` → `SQL Editor`
2. 取消勾选 `Safe Updates`
3. 重新连接数据库
4. 执行更新脚本

### 2. 新增字段

| 字段名 | 类型 | 说明 | 默认值 |
|--------|------|------|--------|
| `username` | varchar(50) | 用户名（可选） | NULL |
| `password` | varchar(255) | 加密密码 | NULL |
| `password_updated_at` | datetime | 密码最后修改时间 | NULL |

### 3. 新增索引

- `uk_username`: 用户名唯一索引

## 功能更新

### 1. 登录方式支持

- ✅ **手机验证码登录**（原有功能）
- ✅ **账号密码登录**（新增功能）
  - 支持用户名登录
  - 支持手机号登录

### 2. 密码管理功能

- ✅ **设置密码**: `setPassword(userId, newPassword)`
- ✅ **修改密码**: `changePassword(userId, oldPassword, newPassword)`
- ✅ **重置密码**: `resetPassword(userId, newPassword)` (管理员功能)
- ✅ **密码验证**: 使用BCrypt加密算法

### 3. API接口更新

#### 登录接口 (`POST /api/user/auth/login`)

**请求参数**:
```json
{
  "identifier": "用户名或手机号",
  "password": "密码",
  "loginType": "PASSWORD"
}
```

**响应示例**:
```json
{
  "success": true,
  "data": {
    "token": "jwt_access_token",
    "refreshToken": "jwt_refresh_token",
    "userInfo": {
      "userId": "USER_123456",
      "primaryPhone": "13800138000",
      "accountStatus": "ACTIVE"
    }
  }
}
```

## 安全特性

### 1. 密码加密
- 使用BCrypt算法加密存储
- 盐值随机生成，防止彩虹表攻击

### 2. 密码策略
- 最小长度：6位
- 不允许设置与当前密码相同的新密码

### 3. 登录安全
- 账户状态检查
- 密码错误日志记录
- 支持多种登录方式

## 默认数据

### 测试账户
更新脚本会为现有用户设置默认密码：
- **默认密码**: `123456`
- **加密后**: `$2a$10$N.zmdr9k7uOIQzUHPPLOPOxrOVJ2eswjzfoy9rI8.sChyZwta7aaa`

⚠️ **安全提醒**: 生产环境中请立即要求用户修改默认密码！

## 使用示例

### 1. 账号密码登录
```bash
curl -X POST http://localhost:8080/api/user/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "identifier": "13800138000",
    "password": "123456",
    "loginType": "PASSWORD"
  }'
```

### 2. 修改密码
```bash
curl -X POST http://localhost:8080/api/user/profile/change-password \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer your_jwt_token" \
  -d '{
    "oldPassword": "123456",
    "newPassword": "newpassword123"
  }'
```

## 兼容性说明

- ✅ 向后兼容：现有手机验证码登录功能不受影响
- ✅ 渐进式升级：用户可以选择是否设置密码
- ✅ 数据完整性：现有用户数据不会丢失

## 注意事项

1. **数据备份**: 执行更新前请备份数据库
2. **密码安全**: 提醒用户设置强密码
3. **日志监控**: 关注登录失败日志，防止暴力破解
4. **定期更新**: 建议用户定期更换密码

## 故障排除

### 常见问题

1. **Q**: 遇到 `Error Code: 1175` 安全更新模式错误怎么办？
   **A**: 
   - **推荐方案**: 使用 `update_add_password_safe.sql` 脚本
   - **临时方案**: 执行 `SET SQL_SAFE_UPDATES = 0;` 然后重新执行更新
   - **永久方案**: 在MySQL Workbench中禁用Safe Updates选项

2. **Q**: 用户忘记密码怎么办？
   **A**: 可以使用手机验证码登录，或联系管理员重置密码

3. **Q**: 如何批量重置用户密码？
   **A**: 
   - 使用存储过程 `UpdateAllUserPasswords()`
   - 使用管理员接口逐个重置
   - 直接执行SQL更新（需要禁用安全模式）

4. **Q**: 密码加密算法可以更换吗？
   **A**: 可以，修改`PasswordService`实现即可

5. **Q**: 如何验证密码更新是否成功？
   **A**: 执行查询语句检查：
   ```sql
   SELECT user_id, 
          CASE WHEN password IS NOT NULL THEN '已设置' ELSE '未设置' END as password_status 
   FROM users LIMIT 5;
   ```

6. **Q**: 存储过程执行失败怎么办？
   **A**: 
   - 检查MySQL版本是否支持存储过程
   - 确保有足够的权限创建存储过程
   - 可以跳过存储过程，手动执行UPDATE语句

---

**更新时间**: 2025-10-22  
**版本**: v1.0  
**负责人**: 开发团队
