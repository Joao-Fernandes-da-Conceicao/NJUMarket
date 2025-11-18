# 南大集市 NJUMarket v2.3.0 项目文档

## 📋 目录
- [版本概述](#版本概述)
- [核心成果](#核心成果)
- [数据库迁移](#数据库迁移)
- [地址体系实现](#地址体系实现)
- [技术架构](#技术架构)
- [API 接口](#api-接口)
- [前端功能](#前端功能)
- [管理端功能](#管理端功能)
- [技术要点](#技术要点)
- [后续规划](#后续规划)

---

## 版本概述

**NJUMarket v2.3.0** 是"数据库迁移 + 地址体系完整实现"版本，主要完成了以下工作：

1. **数据库迁移**：从 MySQL 迁移至 PostgreSQL，启用 PostGIS 扩展支持地理信息
2. **地址体系落地**：完整的用户地址管理、订单地址快照、商品地址快照功能
3. **前后端配套**：用户端和管理端完整的地址管理功能，集成高德地图 API

> **版本状态**：✅ 已完成  
> **完成时间**：2025年  
> **主要贡献**：地址体系完整实现、PostgreSQL 迁移、PostGIS 集成

---

## 核心成果

### ✅ 已完成功能

| 模块 | 功能 | 状态 |
|------|------|------|
| 数据库 | MySQL → PostgreSQL 迁移 | ✅ 完成 |
| 数据库 | PostGIS 扩展集成 | ✅ 完成 |
| 地址体系 | 用户地址管理（CRUD） | ✅ 完成 |
| 地址体系 | 订单地址快照 | ✅ 完成 |
| 地址体系 | 商品地址快照 | ✅ 完成 |
| 地址体系 | 高德地图 API 集成 | ✅ 完成 |
| 前端 | 用户端地址管理 | ✅ 完成 |
| 前端 | 订单地址修改 | ✅ 完成 |
| 前端 | 商品地址编辑 | ✅ 完成 |
| 管理端 | 用户地址管理 | ✅ 完成 |
| 管理端 | 订单地址编辑 | ✅ 完成 |
| 管理端 | 商品地址编辑 | ✅ 完成 |

---

## 数据库迁移

### 迁移概述

从 MySQL 5.7 迁移至 PostgreSQL 16，使用 `pgloader` 工具完成数据迁移。

### 迁移步骤

1. **环境准备**
   - 使用 `postgis/postgis:16-3.4` Docker 镜像
   - 启用 PostGIS 扩展：`CREATE EXTENSION IF NOT EXISTS postgis;`
   - 创建 `nju_market` schema

2. **数据迁移**
   - 使用 `pgloader` 工具迁移表结构和数据
   - 处理数据类型转换（如 `DATETIME` → `TIMESTAMP`）
   - 迁移索引和约束

3. **PostGIS 集成**
   - 为地址相关表添加 `geography` 类型字段
   - 使用 `ST_GeomFromText` 和 `ST_SetSRID` 函数处理地理数据
   - 配置 `@ColumnTransformer` 处理读写转换

### 关键配置

```yaml
# docker-compose.yml
services:
  postgres:
    image: postgis/postgis:16-3.4
    environment:
      POSTGRES_DB: njumarket
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: postgres
```

```sql
-- 启用 PostGIS
CREATE EXTENSION IF NOT EXISTS postgis;

-- 设置默认 schema
SET search_path TO nju_market, public;
```

---

## 地址体系实现

### 数据库设计

#### 1. 用户地址表 (`user_addresses`)

```sql
CREATE TABLE nju_market.user_addresses (
    address_id VARCHAR(50) PRIMARY KEY,
    user_id VARCHAR(50) NOT NULL,
    recipient_name VARCHAR(100) NOT NULL,
    recipient_phone VARCHAR(20) NOT NULL,
    province VARCHAR(50) NOT NULL,
    city VARCHAR(50) NOT NULL,
    district VARCHAR(50) NOT NULL,
    street_address VARCHAR(200) NOT NULL,
    detail_address VARCHAR(500),
    full_address TEXT NOT NULL,
    longitude DOUBLE PRECISION,
    latitude DOUBLE PRECISION,
    location GEOGRAPHY(POINT, 4326),  -- PostGIS 地理类型
    address_label VARCHAR(20) DEFAULT 'HOME',
    is_default BOOLEAN DEFAULT FALSE,
    is_active BOOLEAN DEFAULT TRUE,
    create_time TIMESTAMP NOT NULL,
    update_time TIMESTAMP NOT NULL
);
```

**设计要点**：
- 使用 `GEOGRAPHY(POINT, 4326)` 存储经纬度，支持地理计算
- 支持地址标签（家/公司/学校/其他）
- 支持默认地址标记
- 支持软删除（`is_active`）

#### 2. 订单地址快照字段

在 `orders` 表中添加以下快照字段：

**收货地址快照**：
- `shipping_address_snapshot_province`
- `shipping_address_snapshot_city`
- `shipping_address_snapshot_district`
- `shipping_address_snapshot_street`
- `shipping_address_snapshot_detail`
- `shipping_address_snapshot_full`
- `shipping_address_snapshot_recipient_name`
- `shipping_address_snapshot_recipient_phone`

**商品地址快照**（发货地址）：
- `commodity_snapshot_address_province`
- `commodity_snapshot_address_city`
- `commodity_snapshot_address_district`
- `commodity_snapshot_address_street`
- `commodity_snapshot_address_detail`
- `commodity_snapshot_address_full`

**设计原则**：
- 地址快照字段完全独立，不依赖 `address_id`
- `address_id` 仅作为可选引用，用于填充快照数据
- 快照一旦设置，即使原地址被删除或修改也不受影响

#### 3. 商品地址快照字段

在 `commodities` 表中添加以下字段：

- `address_snapshot_province`
- `address_snapshot_city`
- `address_snapshot_district`
- `address_snapshot_street`
- `address_snapshot_detail`
- `address_snapshot_full`
- `longitude` / `latitude`
- `location_geography GEOGRAPHY(POINT, 4326)`

### 实体类设计

#### UserAddress 实体

```java
@Entity
@Table(name = "user_addresses", schema = "nju_market")
public class UserAddress {
    @Id
    private String addressId;
    
    @Column(name = "location", columnDefinition = "geography(Point,4326)")
    @ColumnTransformer(
        read = "public.ST_AsText(location)",
        write = "public.ST_SetSRID(public.ST_GeomFromText(CAST(? AS text)), 4326)::public.geography"
    )
    private String location;  // WKT 格式：POINT(longitude latitude)
    
    // ... 其他字段
}
```

**关键点**：
- 使用 `@ColumnTransformer` 处理 PostGIS 类型转换
- `read` 表达式：将 `geography` 转换为 WKT 文本
- `write` 表达式：将 WKT 文本转换为 `geography` 类型

---

## 技术架构

### 后端架构

```
njumarket-service-auth (用户服务)
├── UserAddressController (地址管理 API)
├── UserAddressService (地址业务逻辑)
└── UserAddressRepository (数据访问)

njumarket-service-order (订单服务)
├── OrderServiceImpl (订单创建/更新)
│   ├── createOrder (创建订单时填充地址快照)
│   └── updateOrderShippingAddress (更新订单地址)
└── Order 实体 (包含地址快照字段)

njumarket-service-commodity (商品服务)
├── CommodityServiceImpl (商品创建/更新)
│   └── setCommodityAddress (设置商品地址)
└── Commodity 实体 (包含地址快照字段)

njumarket-service-admin (管理服务)
├── AdminController (管理端 API)
│   └── 用户地址管理 RESTful API
└── AdminServiceImpl (管理端业务逻辑)
```

### 前端架构

```
用户端 (njumarket-front/NJUMarket)
├── AddressManager.vue (地址管理组件)
├── AddressSelector.vue (地址选择组件)
├── AddressForm.vue (地址表单组件)
├── AddressMapPicker.vue (高德地图选择器)
├── UserHome.vue (集成地址管理)
└── OrderDetail.vue (订单地址修改)

管理端 (njumarket-front-admin/my-vue3-app)
├── UserAddressManagement.vue (用户地址管理页面)
├── OrderEdit.vue (订单编辑，支持地址修改)
└── CommodityEdit.vue (商品编辑，支持地址修改)
```

---

## API 接口

### 用户端地址管理 API

**基础路径**：`/api/user/address`

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/addresses` | 获取用户地址列表 |
| GET | `/addresses/{addressId}` | 获取地址详情 |
| POST | `/addresses` | 创建地址 |
| PUT | `/addresses/{addressId}` | 更新地址 |
| DELETE | `/addresses/{addressId}` | 删除地址 |
| PUT | `/addresses/{addressId}/default` | 设置默认地址 |
| GET | `/addresses/default` | 获取默认地址 |

### 订单地址 API

**基础路径**：`/api/user/order`

| 方法 | 路径 | 说明 |
|------|------|------|
| PUT | `/{orderId}/shipping-address` | 更新订单地址（买家/卖家） |

**权限控制**：
- 买家：只能修改收货地址，订单状态为 `CREATED` 或 `PAID`
- 卖家：只能修改发货地址（商品地址快照），订单状态为 `CREATED` 或 `PAID`

### 管理端地址管理 API

**基础路径**：`/api/admin/users/{userId}/addresses`

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/users/{userId}/addresses` | 获取用户地址列表 |
| POST | `/users/{userId}/addresses` | 创建用户地址 |
| PUT | `/users/{userId}/addresses/{addressId}` | 更新用户地址 |
| DELETE | `/users/{userId}/addresses/{addressId}` | 删除用户地址 |
| PUT | `/users/{userId}/addresses/{addressId}/default` | 设置默认地址 |

---

## 前端功能

### 用户端功能

#### 1. 地址管理 (`AddressManager.vue`)

**功能特性**：
- 地址列表展示（默认地址置顶）
- 新增/编辑/删除地址
- 设置默认地址
- 响应式设计（移动端适配）
- 自定义模态框（替代 el-dialog，解决移动端宽度问题）

**关键实现**：
- 使用自定义模态框容器，避免 Element Plus 的 teleport 机制
- 移动端适配：`max-width: 375px`，安全区域适配
- 地址排序：默认地址优先显示

#### 2. 地址选择 (`AddressSelector.vue`)

**功能特性**：
- 地址下拉选择
- 打开地址管理弹窗
- 地址变更自动刷新

#### 3. 高德地图集成 (`AddressMapPicker.vue`)

**功能特性**：
- 地图显示和标记
- 点击地图选择位置
- 拖拽标记调整位置
- 地点搜索
- 逆地理编码（坐标转地址）
- 地址字段自动填充

**配置**：
```html
<script type="text/javascript">
  window._AMapSecurityConfig = {
    securityJsCode: 'your-security-js-code'
  }
</script>
<script src="https://webapi.amap.com/maps?v=2.0&key=your-api-key&plugin=AMap.Geocoder,AMap.PlaceSearch"></script>
```

#### 4. 订单地址修改 (`OrderDetail.vue`)

**功能特性**：
- 买家可修改收货地址
- 卖家可修改发货地址
- 订单状态限制（仅 `CREATED` 或 `PAID` 状态）
- 使用地址选择器选择地址

### 管理端功能

#### 1. 用户地址管理 (`UserAddressManagement.vue`)

**功能特性**：
- 用户地址列表展示
- 新增/编辑/删除地址
- 设置默认地址
- 高德地图集成
- 地址字段自动填充和拼接

#### 2. 订单地址编辑 (`OrderEdit.vue`)

**功能特性**：
- 收货地址快照编辑（省/市/区/街道/详细地址/完整地址/收货人信息）
- 发货地址快照编辑（省/市/区/街道/详细地址/完整地址）
- 高德地图集成
- 地址字段自动填充

#### 3. 商品地址编辑 (`CommodityEdit.vue`)

**功能特性**：
- 商品地址快照编辑
- 经纬度编辑
- 高德地图集成
- 地址字段自动填充

---

## 管理端功能

### 用户管理增强

在用户管理页面 (`Users.vue`) 添加"地址管理"按钮，点击后跳转到用户地址管理页面。

### 订单管理增强

- 订单列表显示收货地址和发货地址
- 订单编辑页面支持修改地址快照

### 商品管理增强

- 商品列表显示地址信息（省+市）
- 商品编辑页面支持编辑地址和地图选择

---

## 技术要点

### PostGIS 类型转换

**问题**：PostgreSQL 的 `currentSchema` 设置导致 `public` schema 中的 PostGIS 函数不可见。

**解决方案**：
```java
@ColumnTransformer(
    read = "public.ST_AsText(location)",
    write = "public.ST_SetSRID(public.ST_GeomFromText(CAST(? AS text)), 4326)::public.geography"
)
```

**关键点**：
- 使用 `public.ST_AsText` 明确指定 schema
- 使用 `CAST(? AS text)` 确保类型匹配
- 使用 `::public.geography` 进行类型转换

### 地址快照设计原则

1. **独立性**：地址快照字段完全独立，不依赖 `address_id`
2. **填充机制**：`address_id` 仅用于填充快照数据，不作为依赖
3. **不可变性**：快照一旦设置，即使原地址被删除或修改也不受影响
4. **可选引用**：`address_id` 作为可选引用字段保存

### 前端响应式设计

**移动端适配**：
- 使用自定义模态框替代 `el-dialog`，避免 teleport 机制问题
- 设置 `max-width: 375px` 限制移动端宽度
- 使用 `env(safe-area-inset-top)` 适配安全区域
- 使用 `padding-top` 和 `max-height` 防止内容被遮挡

**关键 CSS**：
```css
@media (max-width: 768px) {
  .address-form-modal {
    padding-top: calc(env(safe-area-inset-top, 8px) + 70px);
    align-items: flex-start;
    justify-content: flex-start;
  }
  
  .address-form-modal__panel {
    max-height: calc(100vh - (env(safe-area-inset-top, 8px) + 70px) - var(--mobile-safe-margin, 6px));
  }
}
```

### 高德地图 API 集成

**初始化**：
```javascript
const map = new AMap.Map('map-container', {
  zoom: 15,
  center: [longitude, latitude]
})

const geocoder = new AMap.Geocoder()
const placeSearch = new AMap.PlaceSearch()
```

**逆地理编码**：
```javascript
geocoder.getAddress([lng, lat], (status, result) => {
  if (status === 'complete' && result.info === 'OK') {
    const addressComponent = result.regeocode.addressComponent
    // 填充地址字段
  }
})
```

**地点搜索**：
```javascript
placeSearch.search(keyword, (status, result) => {
  if (status === 'complete' && result.info === 'OK') {
    // 处理搜索结果
  }
})
```

---

## 数据库迁移脚本

### 添加地址快照字段

```sql
-- 订单收货地址快照字段
ALTER TABLE nju_market.orders
ADD COLUMN shipping_address_snapshot_province VARCHAR(50),
ADD COLUMN shipping_address_snapshot_city VARCHAR(50),
ADD COLUMN shipping_address_snapshot_district VARCHAR(50),
ADD COLUMN shipping_address_snapshot_street VARCHAR(200),
ADD COLUMN shipping_address_snapshot_detail VARCHAR(500),
ADD COLUMN shipping_address_snapshot_full TEXT,
ADD COLUMN shipping_address_snapshot_recipient_name VARCHAR(100),
ADD COLUMN shipping_address_snapshot_recipient_phone VARCHAR(20);

-- 订单商品地址快照字段
ALTER TABLE nju_market.orders
ADD COLUMN commodity_snapshot_address_province VARCHAR(50),
ADD COLUMN commodity_snapshot_address_city VARCHAR(50),
ADD COLUMN commodity_snapshot_address_district VARCHAR(50),
ADD COLUMN commodity_snapshot_address_street VARCHAR(200),
ADD COLUMN commodity_snapshot_address_detail VARCHAR(500),
ADD COLUMN commodity_snapshot_address_full TEXT;

-- 商品地址快照字段
ALTER TABLE nju_market.commodities
ADD COLUMN address_snapshot_province VARCHAR(50),
ADD COLUMN address_snapshot_city VARCHAR(50),
ADD COLUMN address_snapshot_district VARCHAR(50),
ADD COLUMN address_snapshot_street VARCHAR(200),
ADD COLUMN address_snapshot_detail VARCHAR(500),
ADD COLUMN address_snapshot_full TEXT,
ADD COLUMN longitude DOUBLE PRECISION,
ADD COLUMN latitude DOUBLE PRECISION,
ADD COLUMN location_geography GEOGRAPHY(POINT, 4326);
```

### 数据迁移（从商品表填充订单快照）

```sql
-- 从商品表填充订单的商品地址快照
UPDATE nju_market.orders o
SET 
    commodity_snapshot_address_province = c.address_snapshot_province,
    commodity_snapshot_address_city = c.address_snapshot_city,
    commodity_snapshot_address_district = c.address_snapshot_district,
    commodity_snapshot_address_street = c.address_snapshot_street,
    commodity_snapshot_address_detail = c.address_snapshot_detail,
    commodity_snapshot_address_full = c.address_snapshot_full
FROM nju_market.commodities c
WHERE o.commodity_id = c.commodity_id
  AND c.address_snapshot_province IS NOT NULL;
```

---

## 文件清单

### 后端文件

**地址管理服务**：
- `njumarket-service-auth/src/main/java/com/njumarket/auth/entity/UserAddress.java`
- `njumarket-service-auth/src/main/java/com/njumarket/auth/controller/UserAddressController.java`
- `njumarket-service-auth/src/main/java/com/njumarket/auth/service/UserAddressService.java`

**订单服务**：
- `njumarket-service-order/src/main/java/com/njumarket/order/entity/Order.java`
- `njumarket-service-order/src/main/java/com/njumarket/order/service/impl/OrderServiceImpl.java`
- `njumarket-service-order/src/main/java/com/njumarket/order/dto/UpdateOrderAddressDTO.java`

**商品服务**：
- `njumarket-service-commodity/src/main/java/com/njumarket/commodity/entity/Commodity.java`
- `njumarket-service-commodity/src/main/java/com/njumarket/commodity/service/impl/CommodityServiceImpl.java`

**管理服务**：
- `njumarket-service-admin/src/main/java/com/njumarket/admin/entity/UserAddress.java`
- `njumarket-service-admin/src/main/java/com/njumarket/admin/controller/AdminController.java`
- `njumarket-service-admin/src/main/java/com/njumarket/admin/service/impl/AdminServiceImpl.java`

**数据库脚本**：
- `database/postgres-init.sql`
- `database/address-schema.sql`
- `database/add-commodity-snapshot-address-fields.sql`

### 前端文件

**用户端**：
- `njumarket-front/NJUMarket/src/components/address/AddressManager.vue`
- `njumarket-front/NJUMarket/src/components/address/AddressSelector.vue`
- `njumarket-front/NJUMarket/src/components/address/AddressForm.vue`
- `njumarket-front/NJUMarket/src/components/address/AddressMapPicker.vue`
- `njumarket-front/NJUMarket/src/views/UserHome.vue`
- `njumarket-front/NJUMarket/src/views/OrderDetail.vue`

**管理端**：
- `njumarket-front-admin/my-vue3-app/src/views/UserAddressManagement.vue`
- `njumarket-front-admin/my-vue3-app/src/views/OrderEdit.vue`
- `njumarket-front-admin/my-vue3-app/src/views/CommodityEdit.vue`
- `njumarket-front-admin/my-vue3-app/src/components/address/AddressMapPicker.vue`

---

## 后续规划

### v2.4.x（计划中）

1. **搜索优化**
   - 基于 PostgreSQL 全文搜索（GIN 索引）
   - 关键词搜索 + 地址过滤
   - 距离排序（基于 PostGIS）

2. **地址推荐**
   - 基于历史订单的地址推荐
   - 校区/楼栋自动补全

3. **地址验证**
   - 地址格式校验
   - 地址真实性验证

### v2.5.x（计划中）

1. **向量检索**
   - 集成 `pgvector` 扩展
   - 商品描述向量化
   - 语义搜索

2. **推荐系统**
   - 基于地址的附近商品推荐
   - 基于用户行为的个性化推荐

### v3.0.0（计划中）

1. **Spring AI 集成**
   - 检索增强生成（RAG）
   - 智能问答
   - 语义搜索统一体验

---

## 总结

v2.3.0 版本成功完成了数据库迁移和地址体系的完整实现，为后续的搜索优化和 AI 能力打下了坚实基础。主要成果包括：

1. ✅ **数据库迁移**：从 MySQL 迁移至 PostgreSQL，集成 PostGIS
2. ✅ **地址体系**：完整的用户地址管理、订单地址快照、商品地址快照
3. ✅ **前后端配套**：用户端和管理端完整的地址管理功能
4. ✅ **地图集成**：高德地图 API 集成，提升用户体验
5. ✅ **响应式设计**：移动端适配，良好的用户体验

> **文档版本**：v2.3.0  
> **最后更新**：2025年  
> **维护者**：NJUMarket 开发团队
