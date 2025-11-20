# Elasticsearch 数据一致性机制说明

## 📋 概述

本项目采用**实时同步机制**保持 Elasticsearch 与 PostgreSQL 数据库的一致性。所有商品数据的增删改操作都会立即同步到 ES，确保搜索结果的实时性和准确性。

## 🔄 同步机制

### 1. 同步策略：**写时同步（Write-Through）**

**核心原则**：
- **数据库是唯一真实数据源**（Single Source of Truth）
- **ES 是搜索索引**，数据来源于数据库
- **所有数据库写操作后立即同步到 ES**

### 2. 同步时机

所有商品生命周期操作都会触发 ES 同步：

| 操作类型 | 数据库操作 | ES 操作 | 调用位置 |
|---------|-----------|---------|---------|
| **创建商品** | `INSERT` | `save()` (创建索引) | `publishCommodity()`, `createDraftCommodity()` |
| **更新商品** | `UPDATE` | `save()` (更新索引) | `updateCommodity()` |
| **删除商品** | `DELETE` | `deleteById()` (删除索引) | `deleteCommodity()` |
| **上架商品** | `UPDATE status=ON_SHELF` | `save()` (更新索引) | `shelfCommodity()` |
| **下架商品** | `UPDATE status=OFF_SHELF` | `save()` (更新索引) | `unshelfCommodity()` |
| **设为草稿** | `UPDATE status=DRAFT` | `save()` (更新索引) | `draftCommodity()` |
| **更新可见性** | `UPDATE visibility` | `save()` (更新索引) | `updateCommodityVisibility()` |
| **管理端更新** | `UPDATE` | `save()` (更新索引) | `updateCommodityFull()` |

### 3. 同步实现

#### 3.1 同步方法

在 `CommodityServiceImpl` 中，所有数据库写操作后都会调用：

```java
// 同步到 ES（创建或更新）
private void syncCommoditySearchIndex(Commodity commodity) {
    try {
        commoditySearchService.syncCommodity(commodity);
    } catch (Exception e) {
        log.warn("同步商品搜索索引失败: commodityId={}, error={}", 
            commodity.getCommodityId(), e.getMessage());
    }
}

// 从 ES 删除
private void removeCommodityFromSearchIndex(String commodityId) {
    try {
        commoditySearchService.removeCommodity(commodityId);
    } catch (Exception e) {
        log.warn("从搜索索引删除商品失败: commodityId={}, error={}", 
            commodityId, e.getMessage());
    }
}
```

#### 3.2 ES 服务层实现

在 `CommoditySearchService` 中：

```java
// 同步商品（创建或更新）
public void syncCommodity(Commodity commodity) {
    if (!isEnabled() || commodity == null) {
        return;
    }
    try {
        // save() 方法：如果文档存在则更新，不存在则创建
        commoditySearchRepository.save(
            CommoditySearchDocument.fromCommodity(commodity)
        );
    } catch (Exception e) {
        log.warn("商品同步到 ElasticSearch 失败: commodityId={}, error={}", 
            commodity.getCommodityId(), e.getMessage());
    }
}

// 删除商品
public void removeCommodity(String commodityId) {
    if (!isEnabled()) {
        return;
    }
    try {
        commoditySearchRepository.deleteById(commodityId);
    } catch (Exception e) {
        log.warn("从 ElasticSearch 删除商品失败: commodityId={}, error={}", 
            commodityId, e.getMessage());
    }
}
```

## 📊 同步流程

### 创建/更新流程

```
用户操作（创建/更新商品）
    ↓
数据库保存（PostgreSQL）
    ↓
同步到 ES（commoditySearchRepository.save()）
    ├─ 成功：索引已更新
    └─ 失败：记录警告日志，不影响数据库操作
    ↓
清除缓存（Redis）
    ↓
返回结果
```

### 删除流程

```
用户操作（删除商品）
    ↓
数据库删除（PostgreSQL）
    ↓
从 ES 删除（commoditySearchRepository.deleteById()）
    ├─ 成功：索引已删除
    └─ 失败：记录警告日志，不影响数据库操作
    ↓
清除缓存（Redis）
    ↓
返回结果
```

## ✅ 一致性保证

### 1. 实时同步

- **同步时机**：数据库操作成功后立即同步
- **同步方式**：同步调用（在同一事务中）
- **失败处理**：记录警告日志，不影响数据库操作

### 2. 容错机制

- **ES 不可用**：同步失败只记录警告，不影响数据库操作
- **搜索降级**：ES 查询失败时自动降级到数据库查询
- **数据修复**：提供手动同步接口，可修复数据不一致

### 3. 数据修复

如果出现数据不一致，可以通过以下方式修复：

1. **单条修复**：
   ```
   POST /api/internal/commodity/{commodityId}/search-sync
   ```

2. **全量重建**：
   ```
   POST /api/internal/commodity/search/reindex
   ```

3. **管理端操作**：
   - 访问 `http://localhost:8082/elasticsearch`
   - 使用"同步商品"功能修复单条数据
   - 使用"重建索引"功能全量修复

## ⚠️ 注意事项

### 1. 同步失败不影响主流程

- ES 同步失败只记录警告日志
- 数据库操作仍然成功
- 用户操作不受影响
- 可以通过手动同步修复

### 2. 搜索过滤条件

ES 搜索会自动过滤：
- 商品状态必须是 `ON_SHELF`（上架）
- 可见性必须是 `PUBLIC`（公开）

因此：
- 下架的商品不会出现在搜索结果中
- 设为草稿的商品不会出现在搜索结果中
- 隐藏的商品不会出现在搜索结果中

### 3. 批量操作

- 批量导入数据后，需要调用重建索引接口
- 数据库直接修改（绕过 Service 层）不会自动同步，需要手动同步

### 4. 事务处理

- ES 同步在数据库事务提交后执行
- ES 同步失败不会回滚数据库事务
- 这是**最终一致性**模式，不是强一致性

## 🔍 代码位置

### 同步调用位置

所有同步调用都在 `CommodityServiceImpl` 中：

```java
// 创建商品
publishCommodity() → syncCommoditySearchIndex()
createDraftCommodity() → syncCommoditySearchIndex()

// 更新商品
updateCommodity() → syncCommoditySearchIndex()
updateCommodityFull() → syncCommoditySearchIndex()  // 管理端

// 状态变更
shelfCommodity() → syncCommoditySearchIndex()
unshelfCommodity() → syncCommoditySearchIndex()
draftCommodity() → syncCommoditySearchIndex()
removeCommodity() → syncCommoditySearchIndex()  // 管理端强制下架

// 可见性变更
updateCommodityVisibility() → syncCommoditySearchIndex()
updateCommoditySellerVisibility() → syncCommoditySearchIndex()
updateCommodityBuyerVisibility() → syncCommoditySearchIndex()

// 删除商品
deleteCommodity() → removeCommodityFromSearchIndex()
```

### ES 服务实现

- `CommoditySearchService.syncCommodity()` - 同步商品
- `CommoditySearchService.removeCommodity()` - 删除商品
- `CommoditySearchRepository.save()` - 保存/更新索引
- `CommoditySearchRepository.deleteById()` - 删除索引

## 📈 性能考虑

### 1. 同步性能

- **同步方式**：同步调用，但失败不影响主流程
- **性能影响**：ES 写入通常很快（< 50ms），影响很小
- **优化建议**：如果数据量大，可以考虑异步批量同步

### 2. 搜索性能

- **ES 查询**：通常 < 100ms
- **数据库查询**：批量查询（IN 查询），通常 < 200ms
- **缓存优化**：可以添加 Redis 缓存进一步优化

## 🎯 总结

本项目采用**写时同步（Write-Through）**机制保持 ES 数据一致性：

1. ✅ **所有数据库写操作后立即同步到 ES**
2. ✅ **删除操作会从 ES 删除对应文档**
3. ✅ **同步失败不影响数据库操作**
4. ✅ **提供手动同步接口修复数据不一致**
5. ✅ **搜索失败时自动降级到数据库查询**

这种机制确保了：
- **数据实时性**：数据库变更立即反映到搜索
- **系统可用性**：ES 故障不影响数据库操作
- **数据一致性**：通过自动同步和手动修复保证一致性

