# 前端API与后端路径同步检查清单

## 问题描述
商品浏览界面报500错误：`http://localhost:8080/api/public/commodity/search?page=1&size=20`

## 前端API配置（baseURL: `http://localhost:8080/api`）

### 商品相关API (`commodityAPI`)
- ✅ `/public/commodity/search` → 后端: `/api/public/commodity/search` (PublicController)
- ✅ `/public/commodity/{id}` → 后端: `/api/public/commodity/{commodityId}` (PublicController)
- ✅ `/public/commodity/hot` → 后端: `/api/public/commodity/hot` (PublicController)
- ✅ `/public/commodity/latest` → 后端: `/api/public/commodity/latest` (PublicController)
- ✅ `/public/commodity/categories` → 后端: `/api/public/commodity/categories` (PublicController)
- ✅ `/public/commodity/category/{category}` → 后端: `/api/public/commodity/category/{category}` (PublicController)
- ✅ `/public/commodity/seller/{sellerId}` → 后端: `/api/public/commodity/seller/{sellerId}` (PublicController)
- ✅ `/user/commodity/publish` → 后端: `/api/user/commodity/publish` (UserCommodityController)
- ✅ `/user/commodity/draft` → 后端: `/api/user/commodity/draft` (UserCommodityController)
- ✅ `/user/commodity/my` → 后端: `/api/user/commodity/my` (UserCommodityController)
- ✅ `/user/commodity/{id}` → 后端: `/api/user/commodity/{commodityId}` (UserCommodityController)
- ✅ `/user/commodity/batch-status` → 后端: `/api/user/commodity/batch-status` (UserCommodityController)

### 认证相关API (`authAPI`)
- ✅ `/user/auth/login` → 后端: `/api/user/auth/login` (UserAuthController)
- ✅ `/user/auth/register-new` → 后端: `/api/user/auth/register-new` (UserAuthController)
- ✅ `/user/auth/send-code` → 后端: `/api/user/auth/send-code` (UserAuthController)
- ✅ `/user/auth/login-by-code` → 后端: `/api/user/auth/login-by-code` (UserAuthController)
- ✅ `/user/auth/logout` → 后端: `/api/user/auth/logout` (UserAuthController)
- ✅ `/user/auth/reset-password` → 后端: `/api/user/auth/reset-password` (UserAuthController)
- ✅ `/user/auth/me` → 后端: `/api/user/auth/me` (UserAuthController)
- ✅ `/user/auth/refresh-token` → 后端: `/api/user/auth/refresh-token` (UserAuthController)

### 订单相关API (`orderAPI`)
- ✅ `/user/order/create` → 后端: `/api/user/order/create` (OrderController)
- ✅ `/user/order/{id}/pay` → 后端: `/api/user/order/{orderId}/pay` (OrderController)
- ✅ `/user/order/{id}/confirm` → 后端: `/api/user/order/{orderId}/confirm` (OrderController)
- ✅ `/user/order/{id}/cancel` → 后端: `/api/user/order/{orderId}/cancel` (OrderController)
- ✅ `/user/order/buyer` → 后端: `/api/user/order/buyer` (OrderController)
- ✅ `/user/order/seller` → 后端: `/api/user/order/seller` (OrderController)
- ✅ `/user/order/{id}` → 后端: `/api/user/order/{orderId}` (OrderController)

### 用户资料相关API (`profileAPI`)
- ✅ `/user/profile/me` → 后端: `/api/user/profile/me` (UserProfileController)
- ✅ `/user/profile/{id}` → 后端: `/api/user/profile/{userId}` (UserProfileController)
- ✅ `/user/profile/avatar` → 后端: `/api/user/profile/avatar` (UserProfileController)

### 图片相关API (`imageAPI`)
- ✅ `/api/images/avatars/{fileName}` → Gateway路由: `/api/images/**` → ImageService

## Gateway路由配置检查

### 商品服务路由
```yaml
- id: commodity-service-public
  uri: lb://njumarket-service-commodity
  predicates:
    - Path=/api/public/commodity/**
  # ✅ 正确：不StripPrefix，直接转发

- id: commodity-service-user
  uri: lb://njumarket-service-commodity
  predicates:
    - Path=/api/user/commodity/**
  # ✅ 正确：不StripPrefix，直接转发
```

## 可能的问题

### 1. 500错误可能原因
1. **后端代码运行时错误**
   - 空指针异常
   - 方法调用失败
   - 依赖注入失败

2. **服务未正确启动**
   - Commodity Service未启动
   - Gateway未正确路由

3. **数据库/Redis连接问题**
   - MySQL连接失败
   - Redis连接失败

4. **Feign Client调用失败**
   - AuthClient调用失败（获取UserProfile）

### 2. 检查步骤
1. ✅ 检查Commodity Service是否正常启动
2. ✅ 检查Gateway路由配置是否正确
3. ✅ 检查后端日志，查看具体错误信息
4. ✅ 检查数据库连接是否正常
5. ✅ 检查Redis连接是否正常
6. ✅ 检查Feign Client调用是否成功

## 解决方案

### 立即检查
1. 查看Commodity Service的启动日志
2. 查看Gateway的日志
3. 测试直接访问后端服务：`http://localhost:8092/api/public/commodity/search?page=1&size=20`
4. 检查Feign Client调用（AuthClient.getUserProfilesByIds）

### 如果后端服务直接访问也报错
- 检查`CommodityQueryServiceImpl.searchCommodities`方法
- 检查`CommodityRepository`的方法是否存在
- 检查`convertCommoditiesToDTOWithBatchProfile`方法

### 如果Gateway路由有问题
- 检查Gateway的`application.yml`路由配置
- 检查服务注册到Eureka是否成功

