# 数据库初始化脚本

## 📋 文件说明

- **`schema.sql`**: 完整的数据库结构脚本（MySQL dump格式）
  - 包含所有表结构、索引、外键约束、视图和触发器
  - **不包含测试数据**（仅表结构）
  - 适用于全新部署

## ⚠️ 关于中文注释显示问题

**重要说明**：如果 `schema.sql` 文件中的中文注释显示为乱码（如 `'鐢ㄦ埛ID'` 而不是 `'用户ID'`），这是**文件编码显示问题**，**不影响数据库功能**。

### 为什么会出现乱码？

1. **文件编码 vs 数据库字符集**：
   - `SET NAMES utf8mb4` 只告诉 MySQL 客户端如何解析 SQL 语句
   - **不能解决文件本身的编码显示问题**

2. **文件编码问题**：
   - 文件可能是 UTF-8 编码，但被某些编辑器以 GBK/Windows-1252 编码打开
   - 或者文件本身保存时使用了错误的编码

### 是否需要修复？

**功能角度**：**不需要修复**
- ✅ SQL 语句本身是正确的
- ✅ 数据库创建和表结构不会受影响
- ✅ MySQL 客户端会根据 `SET NAMES utf8mb4` 正确处理

**可读性角度**：**建议修复**（可选）
- ❌ 乱码注释影响开发者阅读和维护
- ❌ 不利于代码审查和文档理解

### 如何修复（可选）

**方案1：在 MySQL 客户端中执行**（推荐）
```bash
# 即使文件显示乱码，MySQL 客户端也能正确执行
mysql -u root -p nju_market < database/schema.sql
```

**方案2：重新生成 schema.sql**
```bash
# 从数据库重新导出，确保 UTF-8 编码
mysqldump -u root -p --default-character-set=utf8mb4 \
  --skip-extended-insert --skip-comments \
  nju_market > database/schema.sql
```

**方案3：使用支持 UTF-8 的编辑器**
- 使用 VS Code、Notepad++ 等编辑器
- 确保编辑器以 UTF-8 编码打开文件
- 如果仍显示乱码，尝试"以编码重新打开" → "UTF-8"

## 🚀 使用方法

### 1. 创建数据库

```sql
CREATE DATABASE nju_market CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE nju_market;
```

### 2. 执行初始化脚本

**方式1：命令行执行**（推荐，即使注释乱码也能正确执行）
```bash
mysql -u root -p nju_market < database/schema.sql
```

**方式2：MySQL客户端执行**
```sql
SOURCE /path/to/database/schema.sql;
```

**方式3：在IDE中执行**
- 打开 `schema.sql` 文件
- 在数据库工具中连接到MySQL
- 选择 `nju_market` 数据库
- 执行整个脚本

### 3. 验证安装

```sql
-- 查看所有表
SHOW TABLES;

-- 查看表结构示例
DESCRIBE users;
DESCRIBE commodities;
DESCRIBE orders;
```

## ⚠️ 重要说明

### 关于测试数据

**本脚本不包含任何测试数据**，包括：
- ❌ 测试用户账号
- ❌ 测试商品数据
- ❌ 测试订单数据
- ❌ 管理员账号

**原因**：
1. 保持数据库脚本的纯净性
2. 避免测试数据污染生产环境
3. 便于开发者根据实际需求创建数据

### 如何创建测试账号

**推荐方式：使用后端注册API**

1. **用户注册**：
   ```bash
   POST /api/user/register
   {
     "phone": "13800138000",
     "code": "123456"
   }
   ```

2. **管理员账号**：
   - 需要通过数据库直接创建（管理员注册功能通常不对外开放）
   - 或使用管理端提供的初始化功能

3. **批量测试用户**：
   - 使用项目提供的Python脚本：`scripts/batch_create_users_simple.py`

### 数据库结构

本脚本包含以下核心表：

#### 核心业务表
- `users` - 用户表
- `user_profiles` - 用户档案表
- `contact_info` - 联系方式表
- `commodities` - 商品表
- `orders` - 订单表
- `messages` - 消息表
- `conversations` - 对话表

#### 管理功能表
- `admins` - 管理员表
- `admin_sessions` - 管理员会话表
- `admin_operation_logs` - 管理员操作日志表
- `complaints` - 投诉表
- `audit_records` - 审核记录表
- `ban_records` - 封禁记录表

#### 统计分析表
- `user_activity_records` - 用户活动记录表
- `promotions` - 促销活动表
- `data_statistics` - 数据统计表
- `return_records` - 退货记录表

#### 辅助表
- `image_references` - 图片引用表
- `commodity_categories` - 商品分类表
- `return_reason_types` - 退货原因类型表
- `visibility_types` - 可见性类型表

#### 视图
- `v_admin_basic_info` - 管理员基本信息视图
- `v_admin_statistics` - 管理员统计视图
- `v_return_statistics` - 退货统计视图

## 📝 注意事项

1. **字符集**: 使用 `utf8mb4` 字符集，支持完整的Unicode字符
2. **外键约束**: 启用了外键约束，确保数据完整性
3. **索引优化**: 为常用查询字段添加了索引
4. **数据类型**: 
   - 价格使用 `DOUBLE` 类型
   - 时间使用 `DATETIME` 或 `TIMESTAMP` 类型
   - 布尔值使用 `TINYINT(1)` 或 `BIT(1)` 类型
   - 长文本使用 `TEXT` 类型
5. **默认值**: 为必要字段设置了合理的默认值
6. **注释**: 所有表和字段都有中文注释说明（即使显示为乱码，也不影响功能）

## 🔄 版本信息

- **生成时间**: 2025-11-08 20:00:14
- **MySQL版本**: 8.0.41
- **字符集**: utf8mb4
- **排序规则**: utf8mb4_unicode_ci / utf8mb4_0900_ai_ci

## 📚 相关文档

- 项目文档：`docs/PROJECT_DOCUMENTATION_V2.0.md`
- 微服务配置指南：`docs/MICROSERVICES_SETUP_GUIDE.md`
- 快速开始指南：`docs/QUICK_START_GUIDE.md`
