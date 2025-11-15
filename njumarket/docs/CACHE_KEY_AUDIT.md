# 缓存Key审计报告

## 检查时间
2025-11-12

## 检查范围
- 所有 `RedisConstants` 中定义的缓存key
- 所有使用 `cacheUtil` 的地方
- 缓存key冲突和类型一致性检查

## 检查结果

### ✅ 已修复的问题

#### 1. 用户缓存Key冲突（已修复）
- **问题**：`getUserById` 和 `getUserProfilesByIds` 使用相同的缓存key `CACHE_USER_PROFILE_KEY`
- **影响**：存储不同类型的数据（`UserInternalDTO` vs `UserProfileInternalDTO`），导致缓存冲突
- **修复**：
  - `getUserById` 使用 `CACHE_USER_INFO_KEY`（存储 `UserInternalDTO`）
  - `getUserProfilesByIds` 使用 `CACHE_USER_PROFILE_KEY`（存储 `UserProfileInternalDTO`）

### ✅ 正常使用的缓存Key

#### 1. 商品详情缓存
- **Key**: `CACHE_COMMODITY_DETAIL_KEY + commodityId` = `cache:commodity:detail:{commodityId}`
- **类型**: `CommodityDTO`
- **使用位置**:
  - 读取：`CommodityQueryServiceImpl.getCommodityDetail()`
  - 清除：`CommodityServiceImpl.evictCommodityCache()`, `updateCommodityStock()`, `updateClickCountAsync()`
- **状态**: ✅ 正常，类型一致

#### 2. 热门商品缓存
- **Key**: `CACHE_COMMODITY_HOT_KEY + ":" + limit` = `cache:commodity:hot:{limit}`
- **类型**: `List<CommodityDTO>`
- **使用位置**:
  - 读取：`CommodityQueryServiceImpl.getHotCommodities()`
  - 清除：`CommodityServiceImpl.evictCommodityCache()`（通配符删除）
- **状态**: ✅ 正常，类型一致

#### 3. 最新商品缓存
- **Key**: `CACHE_COMMODITY_LATEST_KEY + ":" + limit` = `cache:commodity:latest:{limit}`
- **类型**: `List<CommodityDTO>`
- **使用位置**:
  - 读取：`CommodityQueryServiceImpl.getLatestCommodities()`
  - 清除：`CommodityServiceImpl.evictCommodityCache()`（通配符删除）
- **状态**: ✅ 正常，类型一致

#### 4. 商品分类缓存
- **Key**: `CACHE_COMMODITY_CATEGORIES_KEY` = `cache:commodity:categories`
- **类型**: `List<String>`
- **使用位置**:
  - 读取：`CommodityQueryServiceImpl.getCategories()`
  - 清除：无（分类变化较少，不需要清除）
- **状态**: ✅ 正常，类型一致

#### 5. 用户基本信息缓存
- **Key**: `CACHE_USER_INFO_KEY + userId` = `cache:user:info:{userId}`
- **类型**: `UserInternalDTO`
- **使用位置**:
  - 读取：`InternalController.getUserById()`
  - 清除：无（通过TTL自动过期）
- **状态**: ✅ 正常，类型一致

#### 6. 用户档案缓存
- **Key**: `CACHE_USER_PROFILE_KEY + userId` = `cache:user:profile:{userId}`
- **类型**: `UserProfileInternalDTO`
- **使用位置**:
  - 读取：`InternalController.getUserProfilesByIds()`
  - 清除：无（通过TTL自动过期）
- **状态**: ✅ 正常，类型一致

### ⚠️ 未使用或部分使用的缓存Key

#### 1. USER_INFO_KEY（未使用）
- **Key**: `USER_INFO_KEY` = `user:info:`
- **状态**: ⚠️ 未使用，但已定义
- **建议**: 可以删除，或保留作为备用

#### 2. CACHE_COMMODITY_KEY（未使用）
- **Key**: `CACHE_COMMODITY_KEY` = `cache:commodity:`
- **状态**: ⚠️ 未使用，但已定义
- **建议**: 可以删除，或保留作为备用

#### 3. CACHE_COMMODITY_LIST_KEY（部分使用）
- **Key**: `CACHE_COMMODITY_LIST_KEY` = `cache:commodity:list:`
- **状态**: ⚠️ 只在清除时使用，但实际没有写入缓存
- **使用位置**:
  - 清除：`CommodityServiceImpl.evictCommodityCache()`（通配符删除）
  - 读取：无（`searchCommodities()` 和 `getCommoditiesByCategory()` 都没有使用缓存）
- **建议**: 
  - 选项1：删除此key（如果不需要缓存商品列表）
  - 选项2：为 `searchCommodities()` 和 `getCommoditiesByCategory()` 添加缓存支持

### 📊 缓存Key命名规范

当前命名规范：
- 缓存key统一使用 `cache:` 前缀
- 格式：`cache:{模块}:{类型}:{标识符}`
- 示例：
  - `cache:commodity:detail:{commodityId}`
  - `cache:user:info:{userId}`
  - `cache:user:profile:{userId}`

### 🔍 潜在问题

#### 1. 商品搜索和分类查询未使用缓存
- **位置**: `CommodityQueryServiceImpl.searchCommodities()` 和 `getCommoditiesByCategory()`
- **影响**: 每次查询都访问数据库，可能影响性能
- **建议**: 考虑添加缓存支持（注意缓存key需要包含查询参数）

#### 2. 缓存清除策略
- **商品缓存清除**: 使用通配符删除，可能影响性能
- **建议**: 考虑使用更精确的清除策略，或使用Redis的Hash结构存储多个limit的缓存

## 总结

### ✅ 已修复
- 用户缓存key冲突问题已修复

### ✅ 正常
- 所有正在使用的缓存key类型一致，无冲突
- 缓存key命名规范统一

### ⚠️ 建议优化
1. 清理未使用的缓存key常量
2. 考虑为商品搜索和分类查询添加缓存支持
3. 优化缓存清除策略（避免通配符删除）

## 检查结论

**当前缓存机制整体良好，无严重的key冲突问题。** 已修复的用户缓存key冲突是唯一发现的问题，其他缓存key使用正常。

