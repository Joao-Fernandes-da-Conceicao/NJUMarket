# 商品浏览API 400错误修复总结

## 问题描述

用户访问 `http://localhost:8080/api/public/commodity/search?page=1&size=20` 时出现 **400 Bad Request** 错误。

## 问题分析

### 1. **Controller参数问题**
- `PublicController.searchCommodities()` 方法中 `keyword` 参数被标记为必需（`@RequestParam String keyword`）
- 但用户的请求中没有提供 `keyword` 参数，导致400错误

### 2. **Service实现问题**
- `CommodityServiceImpl.searchCommodities()` 方法中还在查询 `"PUBLISHED"` 状态
- 但我们已经将商品状态简化为三种：`DRAFT`、`ON_SHELF`、`SOLD_OUT`
- 应该查询 `"ON_SHELF"` 状态（已上架状态）

### 3. **Repository查询问题**
- `CommodityRepository` 中的多个查询方法还在使用 `'PUBLISHED'` 状态
- 需要更新为 `'ON_SHELF'` 状态

## 修复方案

### 1. **修复Controller参数**
```java
// 修复前
@RequestParam String keyword

// 修复后
@RequestParam(required = false) String keyword
```

**文件**: `njumarket/src/main/java/com/njumarket/njumarket/controller/PublicController.java`

### 2. **修复Service实现**
```java
// 修复前
commodityPage = commodityRepository.findByCommodityStatusAndSellerVisibilityAndBuyerVisibility("PUBLISHED", "PUBLIC", "PUBLIC", pageable);

// 修复后
commodityPage = commodityRepository.findByCommodityStatusAndSellerVisibilityAndBuyerVisibility("ON_SHELF", "PUBLIC", "PUBLIC", pageable);
```

**文件**: `njumarket/src/main/java/com/njumarket/njumarket/service/impl/CommodityServiceImpl.java`

### 3. **修复Repository查询**
```java
// 修复前
@Query("SELECT c FROM Commodity c WHERE ... AND c.commodityStatus = 'PUBLISHED'")

// 修复后
@Query("SELECT c FROM Commodity c WHERE ... AND c.commodityStatus = 'ON_SHELF'")
```

**文件**: `njumarket/src/main/java/com/njumarket/njumarket/repository/CommodityRepository.java`

## 修复的具体方法

### CommodityRepository 中修复的查询方法：

1. **searchByKeyword** - 搜索商品（标题和描述）
2. **findByCategoryAndVisible** - 根据分类查找商品
3. **findByPriceRange** - 根据价格范围查找商品
4. **findHotCommodities** - 获取热门商品
5. **findLatestCommodities** - 获取最新商品
6. **countPublishedCommodities** - 统计已上架商品数量

## 修复后的行为

### 1. **无keyword参数时**
- 返回所有已上架（`ON_SHELF`）状态的商品
- 可见性为 `PUBLIC/PUBLIC` 的商品

### 2. **有keyword参数时**
- 搜索标题和描述包含关键词的已上架商品
- 只返回可见的商品

### 3. **有category参数时**
- 返回指定分类的已上架商品
- 只返回可见的商品

## 测试验证

### 修复前的问题请求：
```
GET http://localhost:8080/api/public/commodity/search?page=1&size=20
```
**结果**: 400 Bad Request（缺少必需的keyword参数）

### 修复后的请求：
```
GET http://localhost:8080/api/public/commodity/search?page=1&size=20
```
**结果**: 200 OK（返回所有已上架商品）

### 其他支持的请求：
```
GET http://localhost:8080/api/public/commodity/search?keyword=手机&page=1&size=20
GET http://localhost:8080/api/public/commodity/search?category=电子产品&page=1&size=20
GET http://localhost:8080/api/public/commodity/search?minPrice=100&maxPrice=1000&page=1&size=20
```

## 状态逻辑说明

### 新的商品状态：
- **DRAFT**: 草稿状态（不可见，不可购买）
- **ON_SHELF**: 已上架状态（可见，可购买）
- **SOLD_OUT**: 已售完状态（可见，不可购买）

### 浏览商品逻辑：
- 只有 `ON_SHELF` 状态的商品才会在搜索结果中显示
- 只有 `ON_SHELF` 状态的商品才能被购买
- 其他状态的商品对用户不可见

## 总结

通过修复以下三个层面的问题：

✅ **Controller层**: 将keyword参数改为可选
✅ **Service层**: 更新状态查询逻辑
✅ **Repository层**: 更新所有相关查询方法

现在商品浏览API可以正常工作，支持：
- 无参数浏览所有已上架商品
- 关键词搜索已上架商品
- 分类筛选已上架商品
- 价格范围筛选已上架商品

用户现在可以正常访问 `http://localhost:8080/api/public/commodity/search?page=1&size=20` 来浏览商品了！
