# 图片引用计数系统使用指南

## 📋 概述

为了解决商品和订单共用图片导致的删除问题，实现了图片引用计数系统。只有当图片的引用计数降为0时，才会真正删除物理文件。

## 🗃️ 数据库设计

### 1. image_references 表（图片引用计数表）

| 字段 | 类型 | 说明 |
|------|------|------|
| image_id | BIGINT | 主键，自增 |
| image_path | VARCHAR(500) | 图片路径（唯一） |
| image_type | VARCHAR(20) | 图片类型（AVATAR/COMMODITY） |
| file_size | BIGINT | 文件大小（字节） |
| upload_user_id | VARCHAR(50) | 上传者ID |
| reference_count | INT | 引用计数 |
| upload_time | TIMESTAMP | 上传时间 |
| last_reference_time | TIMESTAMP | 最后引用时间 |
| is_deleted | BOOLEAN | 是否已删除 |
| deleted_time | TIMESTAMP | 删除时间 |

### 2. image_entity_references 表（图片引用关系表）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键，自增 |
| image_path | VARCHAR(500) | 图片路径 |
| entity_type | VARCHAR(50) | 实体类型（COMMODITY/ORDER/USER_PROFILE） |
| entity_id | VARCHAR(255) | 实体ID |
| field_name | VARCHAR(50) | 字段名称 |
| created_at | TIMESTAMP | 创建时间 |

## 🔧 核心功能

### 1. 引用计数管理

- **增加引用**：当新实体使用图片时
  ```java
  imageReferenceService.addReference(imagePath, "COMMODITY", uploadUserId);
  ```

- **减少引用**：当实体删除或不再使用图片时
  ```java
  boolean shouldDelete = imageReferenceService.decrementReference(imagePath);
  if (shouldDelete) {
      // 图片已物理删除
  }
  ```

- **批量操作**：
  ```java
  imageReferenceService.addReferences(imagePaths, "COMMODITY", uploadUserId);
  List<String> deletedImages = imageReferenceService.decrementReferences(imagePaths);
  ```

### 2. 引用计数逻辑

```
初始引用计数 = 1
每增加一个引用 -> reference_count++
每减少一个引用 -> reference_count--
当 reference_count <= 0 时 -> 删除物理文件
```

## 📊 实现的类和接口

### 后端文件

1. **实体类**
   - `ImageReference.java` - 图片引用实体

2. **Repository**
   - `ImageReferenceRepository.java` - 数据访问接口

3. **Service**
   - `ImageReferenceService.java` - 服务接口
   - `ImageReferenceServiceImpl.java` - 服务实现

4. **测试类**
   - `ImageReferenceInitializationTest.java` - 初始化和测试

### 数据库脚本

- `create_image_reference_table.sql` - 创建表结构

## 🚀 部署步骤

### 步骤1：验证数据库表（表已存在）

✅ **image_references 表已存在于数据库中，无需创建！**

可以通过以下命令验证：
```bash
mysql -u root -p njumarket -e "DESC image_references;"
```

**现有表结构**：
- image_id (bigint, 主键, 自增)
- image_path (varchar(500), 唯一索引)
- image_type (varchar(20))
- reference_count (int)
- upload_user_id (varchar(50))
- file_size (bigint)
- upload_time (datetime(6))
- last_reference_time (datetime(6))
- is_deleted (bit(1))
- deleted_time (datetime(6))

### 步骤2：初始化现有图片数据

使用Spring Boot测试程序初始化现有数据：

```bash
cd njumarket

# 运行初始化测试（处理所有现有图片）
mvn test -Dtest=ImageReferenceInitializationTest#initializeImageReferences
```

这个测试会：
- 扫描所有商品的images字段
- 扫描所有订单的commoditySnapshotImages字段
- 扫描所有用户的avatar字段
- 为每个图片创建引用计数记录
- 计算引用次数（如果同一图片被多处使用）

### 步骤3：查看统计信息

```bash
# 查看图片使用情况统计
mvn test -Dtest=ImageReferenceInitializationTest#statisticsImageUsage
```

### 步骤4：检查特定图片

```bash
# 检查特定图片的引用情况
mvn test -Dtest=ImageReferenceInitializationTest#checkSpecificImageReference
```

### 步骤5：清理零引用图片（可选）

```bash
# 测试清理功能（不实际删除）
mvn test -Dtest=ImageReferenceInitializationTest#cleanupUnusedImagesTest
```

## 🔄 业务集成

### 1. 商品图片上传时

```java
// 上传图片后
String imagePath = uploadedFilePath;
imageReferenceService.addReference(imagePath, "COMMODITY", userId);
```

### 2. 商品删除时

```java
// 删除商品前
List<String> images = parseImages(commodity.getImages());
List<String> deletedImages = imageReferenceService.decrementReferences(images);
log.info("删除了 {} 个图片文件", deletedImages.size());
```

### 3. 订单创建（商品快照）时

```java
// 创建订单，复制商品图片到快照
List<String> commodityImages = parseImages(commodity.getImages());
// 增加这些图片的引用计数
imageReferenceService.addReferences(commodityImages, "COMMODITY", commodity.getSellerId());
// 将图片路径保存到订单的commoditySnapshotImages
```

### 4. 订单删除时

```java
// 删除订单前
List<String> snapshotImages = parseImages(order.getCommoditySnapshotImages());
imageReferenceService.decrementReferences(snapshotImages);
```

## 📈 测试结果示例

```
=== 开始初始化图片引用计数 ===
处理商品图片完成，共处理 25 条引用
处理订单快照图片完成，共处理 12 条引用
处理用户头像完成，共处理 3 条引用
=== 初始化完成 ===
图片总数: 32
活跃图片: 32
总文件大小: 2458624 bytes (2 MB)

=== 图片使用情况统计 ===
商品图片: 20 个
用户头像: 3 个
引用次数最多的前10个图片:
  commodities/20251024_112724_commodity_user_003_ed3b09bb.png - 引用次数: 3
  commodities/20251024_103930_commodity_user_003_d4f7da0b.png - 引用次数: 2
  ...
零引用图片: 0 个
```

## ⚠️ 注意事项

### 1. 现有系统迁移

- **首次部署**：运行`initializeImageReferences`测试初始化所有现有图片
- **数据备份**：执行前备份数据库和uploads目录
- **分步执行**：先创建表，再运行初始化，最后集成到业务代码

### 2. 引用计数一致性

- **事务保护**：所有引用计数操作都应该在事务中
- **错误处理**：如果增加引用失败，应该回滚业务操作
- **定期检查**：定期运行统计测试检查数据一致性

### 3. 性能考虑

- **批量操作**：使用`addReferences`和`decrementReferences`批量处理
- **延迟删除**：可以考虑定时任务批量删除物理文件
- **索引优化**：image_path字段有唯一索引，查询效率高

## 🔒 安全措施

1. **软删除**：先标记`is_deleted = true`，延迟删除物理文件
2. **审计日志**：记录删除时间和最后引用时间
3. **权限验证**：只有上传者或管理员可以查看引用情况

## 📝 后续优化

- [ ] 定时任务：每天凌晨清理零引用图片
- [ ] 管理后台：查看和管理图片引用情况
- [ ] 图片去重：相同内容的图片共享存储
- [ ] 云存储集成：支持OSS等云存储服务

## 🧪 测试命令速查

```bash
# 1. 初始化图片引用数据
mvn test -Dtest=ImageReferenceInitializationTest#initializeImageReferences

# 2. 查看统计信息
mvn test -Dtest=ImageReferenceInitializationTest#statisticsImageUsage

# 3. 检查特定图片
mvn test -Dtest=ImageReferenceInitializationTest#checkSpecificImageReference

# 4. 测试清理功能
mvn test -Dtest=ImageReferenceInitializationTest#cleanupUnusedImagesTest
```

## 📚 相关文件

- 数据库脚本：`src/main/resources/database/create_image_reference_table.sql`
- 实体类：`entity/ImageReference.java`
- Repository：`repository/ImageReferenceRepository.java`
- Service：`service/ImageReferenceService.java`, `service/impl/ImageReferenceServiceImpl.java`
- 测试：`test/java/com/njumarket/njumarket/ImageReferenceInitializationTest.java`

---

**创建日期**：2025-10-26
**状态**：✅ 完整实现
**测试状态**：✅ 可运行
