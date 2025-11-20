# 南大集市 NJUMarket v2.3.1 项目文档

## 📋 目录
- [版本概述](#版本概述)
- [核心成果](#核心成果)
- [功能实现](#功能实现)
- [技术架构](#技术架构)
- [API 接口](#api-接口)
- [前端功能](#前端功能)
- [缓存优化](#缓存优化)
- [技术要点](#技术要点)
- [文件清单](#文件清单)

---

## 版本概述

**NJUMarket v2.3.1** 是"用户手机号修改 + 缓存优化"版本，主要完成了以下工作：

1. **用户手机号修改功能**：用户可以在个人资料页面修改自己的手机号，需要新手机号的验证码验证
2. **缓存删除机制完善**：为所有商品修改操作添加了缓存删除机制，确保数据一致性
3. **CacheUtil 优化**：删除了未使用的"强一致性"方法，简化代码结构
4. **Admin 服务修复**：修复了 CacheUtil Bean 未找到的问题

> **版本状态**：✅ 已完成  
> **完成时间**：2025年  
> **主要贡献**：用户手机号修改功能、缓存一致性优化

---

## 核心成果

### ✅ 已完成功能

| 模块 | 功能 | 状态 |
|------|------|------|
| 用户服务 | 用户修改手机号 API | ✅ 完成 |
| 用户服务 | 手机号修改验证码验证 | ✅ 完成 |
| 用户服务 | 手机号修改缓存删除 | ✅ 完成 |
| 商品服务 | 商品修改缓存删除完善 | ✅ 完成 |
| 商品服务 | 商品可见性修改缓存删除 | ✅ 完成 |
| 商品服务 | 商品状态修改缓存删除 | ✅ 完成 |
| 前端 | 用户资料页面手机号修改 | ✅ 完成 |
| 前端 | 手机号修改对话框 | ✅ 完成 |
| 工具类 | CacheUtil 代码简化 | ✅ 完成 |
| 管理服务 | CacheUtil 可选注入修复 | ✅ 完成 |

---

## 功能实现

### 1. 用户手机号修改功能

#### 1.1 功能描述

用户可以在个人资料编辑页面修改自己的手机号。修改流程包括：
1. 用户输入新手机号
2. 系统向新手机号发送验证码
3. 用户输入验证码
4. 系统验证验证码并更新手机号
5. 自动删除用户信息缓存

#### 1.2 安全机制

- **验证码验证**：必须使用新手机号的验证码进行验证
- **手机号唯一性检查**：确保新手机号未被其他用户使用
- **手机号格式验证**：验证手机号格式是否正确
- **用户身份验证**：只有登录用户才能修改自己的手机号

#### 1.3 缓存处理

修改手机号后，系统会自动删除以下缓存：
- 用户信息缓存（`CACHE_USER_INFO_KEY`）

遵循 **Cache Aside 模式**：先更新数据库，再删除缓存。

---

### 2. 商品缓存删除机制完善

#### 2.1 问题背景

在 v2.3.0 版本中，部分商品修改操作（如修改可见性、设为草稿等）没有删除缓存，导致用户可能看到旧的商品信息。

#### 2.2 解决方案

为所有商品修改操作添加了缓存删除机制：

**已添加缓存删除的方法**：
- `draftCommodity` - 商品设为草稿
- `updateCommodityVisibility` - 更新商品可见性
- `updateCommoditySellerVisibility` - 更新卖家可见性
- `updateCommodityBuyerVisibility` - 更新买家可见性
- `updateCommodityFull` (InternalController) - 管理端完整更新商品
- `deleteCommodity` (InternalController) - 管理端删除商品

**已有缓存删除的方法**（保持不变）：
- `updateCommodity` - 更新商品信息
- `deleteCommodity` - 删除商品
- `shelfCommodity` - 商品上架
- `unshelfCommodity` - 商品下架
- `republishCommodity` - 重新上架商品
- `updateCommodityStock` - 更新商品库存

#### 2.3 缓存删除策略

所有商品修改操作都会删除以下缓存：
1. **商品详情缓存**：`CACHE_COMMODITY_DETAIL_KEY + commodityId`
2. **热门商品缓存**：`CACHE_COMMODITY_HOT_KEY + ":*"`（通配符删除所有 limit）
3. **最新商品缓存**：`CACHE_COMMODITY_LATEST_KEY + ":*"`（通配符删除所有 limit）
4. **商品列表缓存**：`CACHE_COMMODITY_LIST_KEY + "*"`（通配符删除所有列表）

---

### 3. CacheUtil 代码优化

#### 3.1 问题背景

`CacheUtil` 中定义了 `getWithStrongConsistency` 方法，但实际上：
- 该方法从未被使用
- 强一致性和最终一致性在读取时的逻辑完全相同
- 真正的区别在于写入时（Cache Aside vs Write Through），而写入操作由业务代码手动实现

#### 3.2 优化内容

1. **删除未使用的方法**：
   - `getWithStrongConsistency(String key, long ttl, TypeReference<T> typeReference, Supplier<T> dataLoader)`
   - `getWithStrongConsistency(String key, long ttl, Class<T> clazz, Supplier<T> dataLoader)`

2. **更新类注释**：
   - 删除了关于"强一致性模式"和"最终一致性模式"的说明
   - 简化为：使用 Cache Aside 模式，读取时先查缓存，写入时由业务代码手动删除缓存

3. **更新方法注释**：
   - `getWithFallback`：删除了"最终一致性缓存"的描述
   - `set`：删除了"最终一致性模式"的描述
   - `delete`：将"强一致性模式"改为"Cache Aside 模式"

---

### 4. Admin 服务 CacheUtil 修复

#### 4.1 问题背景

Admin 服务启动时出现 `CacheUtil Bean Not Found` 错误，因为：
- `CacheUtil` 使用了 `@ConditionalOnBean(RedisTemplate.class)` 注解
- 如果 Admin 服务未配置 Redis，`CacheUtil` Bean 不会被创建
- `AdminServiceImpl` 使用 `@RequiredArgsConstructor` 强制注入 `CacheUtil`，导致启动失败

#### 4.2 解决方案

将 `CacheUtil` 从 `@RequiredArgsConstructor` 的 final 字段改为使用 `@Autowired(required = false)` 的可选注入：

```java
// ✅ CacheUtil 可选注入（如果 Redis 未配置，则为 null）
@Autowired(required = false)
private CacheUtil cacheUtil;
```

这样：
- 如果 Admin 服务配置了 Redis，`CacheUtil` 会被正常注入
- 如果 Admin 服务未配置 Redis，`CacheUtil` 为 `null`，不会导致启动失败
- 代码中已有 `if (cacheUtil != null)` 检查，可以安全处理 `null` 情况

---

## 技术架构

### 后端架构

```
njumarket-service-auth (用户服务)
├── UserAuthController
│   └── POST /api/user/auth/update-phone (修改手机号接口)
├── UserService
│   └── updatePhone(String newPhone, String code) (修改手机号业务逻辑)
└── UserServiceImpl
    ├── updatePhone (验证码验证、更新手机号)
    └── bindPhoneToUniqueUser (绑定手机号，包含缓存删除)

njumarket-service-commodity (商品服务)
├── CommodityServiceImpl
│   └── evictCommodityCache (统一的缓存删除方法)
└── InternalController
    └── updateCommodityFull (管理端更新商品，包含缓存删除)

njumarket-service-admin (管理服务)
└── AdminServiceImpl
    └── CacheUtil 可选注入修复
```

### 前端架构

```
用户端 (njumarket-front/NJUMarket)
├── EditProfile.vue
│   ├── 手机号显示区域
│   ├── 修改手机号对话框
│   ├── 验证码发送逻辑
│   └── 手机号修改逻辑
└── api/index.js
    └── authAPI.updatePhone (修改手机号 API)
```

---

## API 接口

### 用户手机号修改 API

**接口路径**：`POST /api/user/auth/update-phone`

**请求头**：
```
Authorization: Bearer {accessToken}
Content-Type: application/json
```

**请求体**：
```json
{
  "newPhone": "13800138000",
  "code": "123456"
}
```

**响应示例**：
```json
{
  "success": true,
  "message": "手机号修改成功",
  "data": null
}
```

**错误响应**：
```json
{
  "success": false,
  "errorMsg": "验证码错误",
  "data": null
}
```

**业务逻辑**：
1. 验证用户是否登录
2. 验证新手机号格式
3. 检查新手机号是否已被使用
4. 验证新手机号的验证码
5. 更新用户手机号
6. 删除用户信息缓存
7. 删除验证码

---

## 前端功能

### 用户资料编辑页面

#### 手机号显示

在编辑资料页面的"基本信息"部分，添加了手机号显示区域：

```vue
<el-form-item label="手机号">
  <div class="phone-display">
    <span class="phone-value">{{ currentPhone || '未设置' }}</span>
    <UnifiedButton 
      type="default" 
      size="small"
      @click="showPhoneDialog = true"
    >
      修改手机号
    </UnifiedButton>
  </div>
</el-form-item>
```

#### 修改手机号对话框

点击"修改手机号"按钮后，弹出对话框：

```vue
<el-dialog
  v-model="showPhoneDialog"
  title="修改手机号"
  width="500px"
  :close-on-click-modal="false"
>
  <el-form
    ref="phoneFormRef"
    :model="phoneForm"
    :rules="phoneRules"
  >
    <el-form-item label="新手机号" prop="newPhone">
      <el-input
        v-model="phoneForm.newPhone"
        placeholder="请输入新手机号"
      />
    </el-form-item>
    
    <el-form-item label="验证码" prop="code">
      <div class="code-input-group">
        <el-input
          v-model="phoneForm.code"
          placeholder="请输入验证码"
          maxlength="6"
        />
        <UnifiedButton
          :disabled="phoneCodeCountdown > 0"
          @click="sendPhoneCode"
        >
          {{ phoneCodeCountdown > 0 ? `${phoneCodeCountdown}s` : '发送验证码' }}
        </UnifiedButton>
      </div>
    </el-form-item>
  </el-form>
</el-dialog>
```

#### 功能特点

1. **表单验证**：
   - 手机号格式验证（11位数字，1开头）
   - 验证码长度验证（6位数字）

2. **验证码发送**：
   - 60秒倒计时
   - 发送前验证手机号格式
   - 发送成功提示

3. **修改流程**：
   - 表单验证
   - API 调用
   - 成功提示
   - 自动刷新用户信息
   - 关闭对话框

---

## 缓存优化

### Cache Aside 模式

本项目统一使用 **Cache Aside 模式**：

**读取流程**：
1. 先查缓存
2. 缓存未命中，查数据库
3. 将数据写入缓存

**写入流程**：
1. 更新数据库
2. 删除缓存

### 缓存删除时机

#### 用户信息缓存删除

以下操作会删除用户信息缓存（`CACHE_USER_INFO_KEY`）：
- 修改用户状态（`updateUserStatus`）
- 修改用户基本信息（`updateUserBasic`）- 当修改 `primaryPhone` 时
- 完整更新用户信息（`updateUserFull`）- 当修改 `username`、`primaryPhone`、`accountStatus` 时
- 修改手机号（`updatePhone`）
- 重置密码（`resetPassword`）
- 修改密码（`changePassword`）
- 删除用户（`deleteUser`）

#### 商品缓存删除

以下操作会删除商品相关缓存：
- 更新商品信息（`updateCommodity`）
- 删除商品（`deleteCommodity`）
- 商品上架（`shelfCommodity`）
- 商品下架（`unshelfCommodity`）
- 商品设为草稿（`draftCommodity`）
- 重新上架商品（`republishCommodity`）
- 更新商品可见性（`updateCommodityVisibility`）
- 更新卖家可见性（`updateCommoditySellerVisibility`）
- 更新买家可见性（`updateCommodityBuyerVisibility`）
- 更新商品库存（`updateCommodityStock`）
- 管理端完整更新商品（`updateCommodityFull`）
- 管理端删除商品（`deleteCommodity`）

---

## 技术要点

### 1. 验证码验证流程

```java
// 1. 从Redis中获取验证码（验证码应该发送到新手机号）
String codeKey = RedisConstants.LOGIN_CODE_KEY + newPhone;
String cachedCode = stringRedisTemplate.opsForValue().get(codeKey);
if (cachedCode == null) {
    throw new BusinessException("验证码已过期，请重新获取");
}

// 2. 验证验证码
if (!cachedCode.equals(code.trim())) {
    throw new BusinessException("验证码错误");
}

// 3. 更新手机号
bindPhoneToUniqueUser(currentUser.getUserId(), newPhone.trim());

// 4. 删除验证码
stringRedisTemplate.delete(codeKey);
```

### 2. 统一的商品缓存删除方法

```java
/**
 * 清除商品相关缓存（最终一致性：写入时删除缓存）
 * 当商品更新、删除、上架、下架等操作时调用
 * 
 * @param commodityId 商品ID
 */
private void evictCommodityCache(String commodityId) {
    try {
        // 清除商品详情缓存
        cacheUtil.delete(RedisConstants.CACHE_COMMODITY_DETAIL_KEY + commodityId);
        
        // 清除热门商品和最新商品缓存（使用通配符删除所有limit的缓存）
        cacheUtil.deleteByPattern(RedisConstants.CACHE_COMMODITY_HOT_KEY + ":*");
        cacheUtil.deleteByPattern(RedisConstants.CACHE_COMMODITY_LATEST_KEY + ":*");
        
        // 清除商品列表缓存（使用通配符删除所有列表缓存）
        cacheUtil.deleteByPattern(RedisConstants.CACHE_COMMODITY_LIST_KEY + "*");
        
        log.debug("商品缓存已清除: commodityId={}", commodityId);
    } catch (Exception e) {
        log.error("清除商品缓存失败: commodityId={}, error={}", commodityId, e.getMessage(), e);
        // 缓存清除失败不影响主流程
    }
}
```

### 3. 可选依赖注入

```java
// ✅ CacheUtil 可选注入（如果 Redis 未配置，则为 null）
@Autowired(required = false)
private CacheUtil cacheUtil;

// 使用时检查 null
if (cacheUtil != null) {
    cacheUtil.delete(cacheKey);
}
```

---

## 文件清单

### 后端文件

**用户服务**：
- `njumarket-service-auth/src/main/java/com/njumarket/auth/dto/UpdatePhoneDTO.java` - 修改手机号 DTO
- `njumarket-service-auth/src/main/java/com/njumarket/auth/controller/UserAuthController.java` - 添加修改手机号接口
- `njumarket-service-auth/src/main/java/com/njumarket/auth/service/UserService.java` - 添加 updatePhone 方法
- `njumarket-service-auth/src/main/java/com/njumarket/auth/service/impl/UserServiceImpl.java` - 实现 updatePhone 和 bindPhoneToUniqueUser

**商品服务**：
- `njumarket-service-commodity/src/main/java/com/njumarket/commodity/service/impl/CommodityServiceImpl.java` - 完善缓存删除机制
- `njumarket-service-commodity/src/main/java/com/njumarket/commodity/controller/InternalController.java` - 添加缓存删除

**管理服务**：
- `njumarket-service-admin/src/main/java/com/njumarket/admin/service/impl/AdminServiceImpl.java` - CacheUtil 可选注入修复

**工具类**：
- `njumarket-common/src/main/java/com/njumarket/njumarket/utils/CacheUtil.java` - 删除未使用的强一致性方法

### 前端文件

**用户端**：
- `njumarket-front/NJUMarket/src/views/EditProfile.vue` - 添加手机号修改功能
- `njumarket-front/NJUMarket/src/api/index.js` - 添加 authAPI.updatePhone

---

## 总结

v2.3.1 版本成功完成了用户手机号修改功能和缓存优化，主要成果包括：

1. ✅ **用户手机号修改功能**：完整的用户手机号修改流程，包含验证码验证和缓存删除
2. ✅ **商品缓存删除完善**：为所有商品修改操作添加了缓存删除机制，确保数据一致性
3. ✅ **CacheUtil 代码优化**：删除了未使用的方法，简化代码结构
4. ✅ **Admin 服务修复**：修复了 CacheUtil Bean 未找到的问题，支持可选注入

> **文档版本**：v2.3.1  
> **最后更新**：2025年  
> **维护者**：NJUMarket 开发团队

