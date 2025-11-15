# 地址框架实施 TODO（v2.3.x）

本文档详细列出了地址框架的完整实施计划，作为 v2.3.x 版本的开发指南。

## 📋 概述

**目标**: 建立完整的地址管理系统和地理位置服务，为 Spring AI 智能推荐提供地理位置数据支持。

**预计时间**: 3-4 周

**优先级**: 中高（对智能推荐功能重要）

---

## 🗄️ 数据库设计

### 1. 用户地址表（user_addresses）

```sql
CREATE TABLE `user_addresses` (
  `address_id` VARCHAR(50) PRIMARY KEY COMMENT '地址ID',
  `user_id` VARCHAR(50) NOT NULL COMMENT '用户ID',
  `province` VARCHAR(50) COMMENT '省',
  `city` VARCHAR(50) COMMENT '市',
  `district` VARCHAR(50) COMMENT '区/县',
  `street` VARCHAR(200) COMMENT '街道/详细地址',
  `postal_code` VARCHAR(20) COMMENT '邮编',
  `latitude` DECIMAL(10,7) COMMENT '纬度',
  `longitude` DECIMAL(10,7) COMMENT '经度',
  `is_default` BOOLEAN DEFAULT FALSE COMMENT '是否默认地址',
  `contact_name` VARCHAR(50) COMMENT '收货人姓名',
  `contact_phone` VARCHAR(20) COMMENT '收货人电话',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  INDEX `idx_user_id` (`user_id`),
  INDEX `idx_location` (`latitude`, `longitude`),
  FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户地址表';
```

### 2. 扩展商品表（commodities）

```sql
ALTER TABLE `commodities` 
ADD COLUMN `province` VARCHAR(50) COMMENT '省' AFTER `location`,
ADD COLUMN `city` VARCHAR(50) COMMENT '市' AFTER `province`,
ADD COLUMN `district` VARCHAR(50) COMMENT '区/县' AFTER `city`,
ADD COLUMN `street` VARCHAR(200) COMMENT '街道/详细地址' AFTER `district`,
ADD COLUMN `latitude` DECIMAL(10,7) COMMENT '纬度' AFTER `street`,
ADD COLUMN `longitude` DECIMAL(10,7) COMMENT '经度' AFTER `latitude`,
ADD INDEX `idx_location` (`latitude`, `longitude`);
```

### 3. 扩展订单表（orders）

```sql
ALTER TABLE `orders`
ADD COLUMN `address_id` VARCHAR(50) COMMENT '地址ID（关联user_addresses）' AFTER `shipping_address`,
ADD COLUMN `address_snapshot_province` VARCHAR(50) COMMENT '地址快照-省' AFTER `address_id`,
ADD COLUMN `address_snapshot_city` VARCHAR(50) COMMENT '地址快照-市' AFTER `address_snapshot_province`,
ADD COLUMN `address_snapshot_district` VARCHAR(50) COMMENT '地址快照-区/县' AFTER `address_snapshot_city`,
ADD COLUMN `address_snapshot_street` VARCHAR(200) COMMENT '地址快照-街道' AFTER `address_snapshot_district`,
ADD COLUMN `address_snapshot_latitude` DECIMAL(10,7) COMMENT '地址快照-纬度' AFTER `address_snapshot_street`,
ADD COLUMN `address_snapshot_longitude` DECIMAL(10,7) COMMENT '地址快照-经度' AFTER `address_snapshot_latitude`,
ADD INDEX `idx_address_id` (`address_id`);
```

---

## 🔧 后端开发 TODO

### 阶段一：基础框架（1-2周）

#### 1.1 实体类和DTO

- [ ] 创建 `Address` 实体类
  - 文件：`njumarket-service-auth/src/main/java/com/njumarket/auth/entity/Address.java`
  - 包含所有字段的映射
  - 添加业务方法（如 `isValid()`）

- [ ] 创建 `AddressDTO`（数据传输对象）
  - 文件：`njumarket-service-auth/src/main/java/com/njumarket/auth/dto/AddressDTO.java`
  - 用于API传输

- [ ] 创建 `AddressCreateDTO` 和 `AddressUpdateDTO`
  - 包含验证注解
  - 用于创建和更新地址

#### 1.2 Repository层

- [ ] 创建 `AddressRepository`
  - 文件：`njumarket-service-auth/src/main/java/com/njumarket/auth/repository/AddressRepository.java`
  - 继承 `JpaRepository<Address, String>`
  - 添加查询方法：
    - `findByUserId()` - 根据用户ID查询地址列表
    - `findByUserIdAndIsDefault()` - 查询用户默认地址
    - `countByUserId()` - 统计用户地址数量

#### 1.3 Service层

- [ ] 创建 `AddressService` 接口
  - 文件：`njumarket-service-auth/src/main/java/com/njumarket/auth/service/AddressService.java`
  - 定义方法签名

- [ ] 实现 `AddressServiceImpl`
  - 文件：`njumarket-service-auth/src/main/java/com/njumarket/auth/service/impl/AddressServiceImpl.java`
  - 实现方法：
    - `createAddress()` - 创建地址
    - `updateAddress()` - 更新地址
    - `deleteAddress()` - 删除地址
    - `getUserAddresses()` - 获取用户地址列表
    - `getAddressById()` - 根据ID获取地址
    - `setDefaultAddress()` - 设置默认地址
    - `getDefaultAddress()` - 获取默认地址

#### 1.4 Controller层

- [ ] 创建 `AddressController`
  - 文件：`njumarket-service-auth/src/main/java/com/njumarket/auth/controller/AddressController.java`
  - 实现API端点：
    - `POST /api/user/address/create` - 创建地址
    - `PUT /api/user/address/{id}` - 更新地址
    - `DELETE /api/user/address/{id}` - 删除地址
    - `GET /api/user/address/list` - 获取地址列表
    - `GET /api/user/address/{id}` - 获取地址详情
    - `PUT /api/user/address/{id}/default` - 设置默认地址

#### 1.5 集成到Order服务

- [ ] 修改 `OrderDTO`
  - 添加 `addressId` 字段（可选，兼容旧版本）
  - 保留 `shippingAddress` 字段（向后兼容）

- [ ] 修改 `OrderServiceImpl.createOrder()`
  - 支持使用 `addressId` 创建订单
  - 如果提供 `addressId`，从 `AddressService` 获取地址信息
  - 创建订单时保存地址快照（省市区详细地址、经纬度）
  - 如果只提供 `shippingAddress` 文本，保持向后兼容

- [ ] 修改 `OrderServiceImpl.getOrderDetail()`
  - 返回完整地址信息（省市区详细地址）
  - 如果订单有地址快照，优先使用快照

#### 1.6 集成到Commodity服务

- [ ] 修改 `CommodityDTO`
  - 添加位置相关字段（省市区、经纬度）

- [ ] 修改 `CommodityServiceImpl.publishCommodity()`
  - 支持位置信息输入
  - 保存位置信息到数据库

- [ ] 修改 `CommodityServiceImpl.getCommodityDetail()`
  - 返回位置信息

### 阶段二：地理位置服务（1周）

#### 2.1 地图API集成

- [ ] 选择地图服务提供商
  - 推荐：高德地图API
  - 备选：百度地图API、腾讯地图API

- [ ] 配置API密钥
  - 在 Config Server 中添加配置
  - 使用环境变量管理密钥

- [ ] 创建 `MapApiClient`
  - 文件：`njumarket-common/src/main/java/com/njumarket/njumarket/client/MapApiClient.java`
  - 使用 RestTemplate 或 Feign Client
  - 实现方法：
    - `geocode()` - 地理编码（地址转经纬度）
    - `reverseGeocode()` - 逆地理编码（经纬度转地址）

- [ ] 创建 `MapApiService`
  - 文件：`njumarket-service-auth/src/main/java/com/njumarket/auth/service/MapApiService.java`
  - 封装地图API调用逻辑
  - 添加缓存机制（避免重复调用）

#### 2.2 地理计算功能

- [ ] 创建 `GeographicUtils`
  - 文件：`njumarket-common/src/main/java/com/njumarket/njumarket/util/GeographicUtils.java`
  - 实现 `calculateDistance()` 方法（Haversine公式）
  - 计算两点间距离（单位：公里）

- [ ] 创建 `GeographicController`
  - 文件：`njumarket-service-commodity/src/main/java/com/njumarket/commodity/controller/GeographicController.java`
  - 实现API端点：
    - `GET /api/public/commodity/nearby` - 附近商品查询
      - 参数：`latitude`, `longitude`, `radius` (km)
    - `GET /api/public/commodity/distance` - 计算商品与地址距离
      - 参数：`commodityId`, `addressId`

#### 2.3 地址解析和验证

- [ ] 在 `AddressServiceImpl` 中集成地址解析
  - 创建地址时自动调用地理编码API获取经纬度
  - 更新地址时重新解析经纬度

- [ ] 添加地址验证逻辑
  - 验证省市区是否有效
  - 验证详细地址格式

### 阶段三：数据迁移（1天）

- [ ] 编写数据迁移脚本
  - 文件：`njumarket/database/migrations/add_address_framework.sql`
  - 创建新表
  - 添加新字段
  - 创建索引

- [ ] 编写数据迁移工具（可选）
  - 将现有订单地址文本解析为结构化地址
  - 为现有地址补充经纬度（调用地图API）

---

## 🎨 前端开发 TODO

### 阶段一：地址选择组件（2-3天）

- [ ] 选择地址选择器组件
  - 方案A：使用 `vue-area-linkage`
  - 方案B：使用 Element Plus + 自定义省市区数据
  - 方案C：集成高德/百度地图地址选择器

- [ ] 创建 `AddressPicker.vue` 组件
  - 文件：`njumarket-front/NJUMarket/src/components/address/AddressPicker.vue`
  - 实现省市区三级联动
  - 详细地址输入框
  - 表单验证

- [ ] 创建 `AddressForm.vue` 组件
  - 文件：`njumarket-front/NJUMarket/src/components/address/AddressForm.vue`
  - 包含地址选择器
  - 收货人姓名、电话输入
  - 设置为默认地址选项
  - 表单提交处理

### 阶段二：地址管理页面（2-3天）

- [ ] 创建 `AddressManagement.vue` 页面
  - 文件：`njumarket-front/NJUMarket/src/views/AddressManagement.vue`
  - 地址列表展示（卡片形式）
  - 添加地址按钮
  - 编辑地址功能（弹窗或路由跳转）
  - 删除地址功能（确认对话框）
  - 设置默认地址功能
  - 空状态提示

- [ ] 创建地址API
  - 文件：`njumarket-front/NJUMarket/src/api/address.js`
  - 实现所有地址相关API调用

- [ ] 创建地址Store（可选）
  - 文件：`njumarket-front/NJUMarket/src/stores/address.js`
  - 管理地址列表状态
  - 缓存默认地址

### 阶段三：集成到现有页面（2-3天）

- [ ] 修改 `CreateOrder.vue`
  - 从地址列表选择地址（替代文本输入）
  - 显示选中地址详情
  - 显示商品与地址的距离（如果有）
  - 支持快速添加新地址（弹窗）
  - 如果没有地址，引导用户添加地址

- [ ] 修改 `PublishCommodity.vue`
  - 地址选择器替代位置文本输入
  - 可选地图选点（如果集成地图组件）
  - 显示选择的位置信息
  - 位置信息验证

- [ ] 修改 `CommodityDetail.vue`
  - 显示商品位置信息（省市区详细地址）
  - 显示与用户默认地址的距离（如果已登录）
  - 地图展示商品位置（可选，如果集成地图组件）

- [ ] 修改 `OrderDetail.vue`
  - 显示完整地址信息（省市区详细地址）
  - 地图展示收货地址（可选）
  - 地址信息格式化显示

### 阶段四：工具函数和优化（1-2天）

- [ ] 创建地址格式化工具函数
  - 文件：`njumarket-front/NJUMarket/src/utils/addressUtils.js`
  - `formatAddress()` - 格式化地址显示
  - `formatDistance()` - 格式化距离显示（如：1.5km、500m）

- [ ] 创建地址验证工具函数
  - 验证地址格式
  - 验证必填字段

- [ ] 优化用户体验
  - 地址选择器默认值设置
  - 地址列表加载状态
  - 错误提示优化

---

## 🧪 测试 TODO

### 功能测试

- [ ] 地址CRUD功能测试
  - 创建地址
  - 更新地址
  - 删除地址
  - 设置默认地址

- [ ] 地址选择器测试
  - 省市区三级联动
  - 地址验证

- [ ] 订单创建流程测试
  - 使用地址ID创建订单
  - 地址快照保存
  - 向后兼容（文本地址）

- [ ] 商品发布流程测试
  - 位置信息输入
  - 位置信息保存

- [ ] 地理查询测试
  - 附近商品查询
  - 距离计算

### 性能测试

- [ ] 地理查询性能测试
  - 索引优化效果
  - 大量数据查询性能

- [ ] 地图API调用性能测试
  - 缓存效果
  - 并发调用处理

### 边界情况测试

- [ ] 地址解析失败处理
  - API调用失败
  - 地址格式错误

- [ ] 地图API调用失败降级
  - 网络错误
  - API限流

- [ ] 地址数据格式兼容
  - 旧订单地址显示
  - 新老数据混合场景

---

## 📚 技术选型

### 地图服务

**推荐：高德地图API**
- 文档完善
- 免费额度充足（个人开发者：30万次/天）
- 支持地理编码、逆地理编码、路径规划等

**备选：百度地图API**
- 功能类似
- 免费额度：30万次/天

**备选：腾讯地图API**
- 功能类似
- 免费额度：30万次/天

### 前端组件

**方案A：vue-area-linkage**
- 成熟的省市区选择组件
- 支持三级联动
- 需要引入省市区数据

**方案B：Element Plus + 自定义数据**
- 使用 Element Plus 的 Cascader 组件
- 自定义省市区数据源
- 更灵活，但需要维护数据

**方案C：地图地址选择器**
- 集成高德/百度地图地址选择器
- 支持地图选点
- 用户体验最好，但实现复杂

### 地理计算

**方案A：MySQL ST_Distance_Sphere**
- 使用数据库函数计算距离
- 性能好，适合大量数据查询
- 需要 MySQL 5.7+

**方案B：Java Haversine 公式**
- 在应用层计算距离
- 灵活性高
- 适合少量数据计算

---

## 🎯 业务价值

### 短期价值

1. **用户体验提升**
   - 地址管理更规范
   - 地址选择更方便
   - 订单地址准确性提高

2. **数据质量提升**
   - 地址数据标准化
   - 支持地址验证
   - 减少地址错误

### 长期价值（Spring AI推荐）

1. **基于距离的推荐**
   - 优先推荐附近商品
   - 提高交易成功率
   - 提升用户体验

2. **地理位置分析**
   - 分析用户活动区域
   - 分析商品分布区域
   - 区域偏好分析

3. **智能匹配**
   - 结合用户位置、商品位置、用户偏好
   - 生成个性化推荐列表
   - 提高推荐准确性

---

## ⚠️ 注意事项

1. **数据迁移**
   - 现有订单地址数据可能格式不统一
   - 需要编写迁移脚本处理旧数据
   - 考虑向后兼容

2. **用户接受度**
   - 需要用户重新输入地址
   - 提供清晰的引导和说明
   - 支持批量导入（可选）

3. **API成本**
   - 地图API调用可能有费用
   - 但免费额度通常足够
   - 需要监控API调用量

4. **隐私问题**
   - 经纬度涉及用户隐私
   - 需要合规处理
   - 考虑数据加密

5. **性能优化**
   - 地理查询需要索引优化
   - 地图API调用需要缓存
   - 大量数据查询需要分页

---

## 📅 实施时间表

| 阶段 | 内容 | 预计时间 | 负责人 |
|------|------|----------|--------|
| 阶段一 | 后端基础框架 | 1-2周 | 后端开发 |
| 阶段二 | 地理位置服务 | 1周 | 后端开发 |
| 阶段三 | 前端地址组件 | 2-3天 | 前端开发 |
| 阶段四 | 地址管理页面 | 2-3天 | 前端开发 |
| 阶段五 | 集成到现有页面 | 2-3天 | 前端开发 |
| 阶段六 | 测试和优化 | 2-3天 | 全栈 |
| **总计** | | **3-4周** | |

---

## 🔗 相关文档

- [PROJECT_DOCUMENTATION_V2.1.1.md](./PROJECT_DOCUMENTATION_V2.1.1.md) - 项目文档
- [高德地图API文档](https://lbs.amap.com/api/webservice/summary) - 地图API参考
- [Spring AI 文档](https://docs.spring.io/spring-ai/reference/) - AI推荐集成参考

---

**版本**: v2.3.x  
**状态**: 📋 **计划中**  
**创建日期**: 2025-11-12

