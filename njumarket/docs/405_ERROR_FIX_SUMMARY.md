# 405错误修复总结 - 用户商品详情查询

## 问题描述
用户访问 `http://localhost:8080/api/user/commodity/0245de37f6fd4f56b3390f5c87259ec4` 时出现405错误。

## 错误原因分析

### 1. HTTP 405错误
- **错误类型**: Method Not Allowed
- **原因**: 请求的HTTP方法不被目标资源支持

### 2. 具体问题
- **请求路径**: `/api/user/commodity/{commodityId}`
- **请求方法**: GET
- **问题**: `UserCommodityController`中没有对应的GET方法来处理这个路径

### 3. 现有路由分析
在`UserCommodityController`中，`/{commodityId}`路径只有以下方法：
- `@PutMapping("/{commodityId}")` - 更新商品信息
- `@PostMapping("/{commodityId}/remove")` - 下架商品
- `@PostMapping("/{commodityId}/republish")` - 重新上架商品
- `@PostMapping("/{commodityId}/upload-image")` - 上传商品图片
- `@DeleteMapping("/{commodityId}")` - 删除商品

**缺少**: `@GetMapping("/{commodityId}")` - 获取商品详情

## 解决方案

### 1. 添加新的Controller方法
在`UserCommodityController`中添加获取用户自己商品详情的方法：

```java
/**
 * 获取我发布的单个商品详情
 */
@GetMapping("/{commodityId}")
public Result getMyCommodityDetail(@PathVariable String commodityId) {
    return commodityService.getMyCommodityDetail(commodityId);
}
```

### 2. 添加Service接口方法
在`CommodityService`接口中添加方法声明：

```java
/**
 * 获取我发布的单个商品详情
 */
Result getMyCommodityDetail(String commodityId);
```

### 3. 实现Service方法
在`CommodityServiceImpl`中实现具体逻辑：

```java
@Override
public Result getMyCommodityDetail(String commodityId) {
    try {
        User currentUser = UserHolder.getUser();
        if (currentUser == null) {
            return Result.fail("用户未登录");
        }
        
        log.info("获取我发布的商品详情 - userId: {}, commodityId: {}", currentUser.getUserId(), commodityId);
        
        // 查找商品
        Optional<Commodity> commodityOpt = commodityRepository.findById(commodityId);
        if (commodityOpt.isEmpty()) {
            return Result.fail("商品不存在");
        }
        
        Commodity commodity = commodityOpt.get();
        
        // 检查是否是当前用户的商品
        if (!commodity.getSellerId().equals(currentUser.getUserId())) {
            return Result.fail("无权访问该商品");
        }
        
        // 转换为DTO
        CommodityDTO commodityDTO = convertToDTO(commodity);
        
        return Result.ok("获取商品详情成功", commodityDTO);
        
    } catch (Exception e) {
        log.error("获取我发布的商品详情失败", e);
        return Result.fail("获取商品详情失败：" + e.getMessage());
    }
}
```

## 功能特点

### 1. 权限控制
- **登录检查**: 确保用户已登录
- **所有权验证**: 只能查看自己发布的商品
- **安全保护**: 防止用户访问他人的商品详情

### 2. 数据返回
- **完整信息**: 返回商品的完整详情
- **统一格式**: 使用Result.ok(message, data)格式
- **错误处理**: 完善的异常处理和错误信息

### 3. 与现有接口的区别

#### 公共商品详情 (`/api/public/commodity/{commodityId}`)
- **访问权限**: 无需登录，任何人都可以访问
- **可见性检查**: 只显示公开可见的商品
- **点击统计**: 会增加商品的点击量
- **用途**: 商品浏览、购买前查看

#### 用户商品详情 (`/api/user/commodity/{commodityId}`)
- **访问权限**: 需要登录，只能查看自己的商品
- **完整信息**: 显示商品的所有信息（包括私有状态）
- **管理功能**: 用于商品管理、编辑等操作
- **用途**: 商品管理、状态查看、编辑准备

## API使用示例

### 1. 请求示例
```http
GET /api/user/commodity/0245de37f6fd4f56b3390f5c87259ec4
Authorization: Bearer <token>
```

### 2. 成功响应
```json
{
  "success": true,
  "errorMsg": null,
  "data": {
    "commodityId": "0245de37f6fd4f56b3390f5c87259ec4",
    "title": "iPhone 15 Pro",
    "description": "全新iPhone 15 Pro，256GB存储...",
    "price": 7999.0,
    "category": "电子产品",
    "sellerId": "USER_123456",
    "sellerName": "张三",
    "images": ["image1.jpg", "image2.jpg"],
    "publishTime": "2024-01-15T10:30:00",
    "clickCount": 150,
    "commodityStatus": "PUBLISHED",
    "sellerVisibility": "PUBLIC",
    "buyerVisibility": "PUBLIC",
    "stock": 10,
    "location": "仙林校区"
  },
  "total": null
}
```

### 3. 错误响应示例

#### 未登录
```json
{
  "success": false,
  "errorMsg": "用户未登录",
  "data": null,
  "total": null
}
```

#### 商品不存在
```json
{
  "success": false,
  "errorMsg": "商品不存在",
  "data": null,
  "total": null
}
```

#### 无权访问
```json
{
  "success": false,
  "errorMsg": "无权访问该商品",
  "data": null,
  "total": null
}
```

## 路由对比

### 修复前
```
/api/user/commodity/{commodityId}  ❌ 405 Method Not Allowed
```

### 修复后
```
/api/user/commodity/{commodityId}  ✅ GET - 获取我的商品详情
/api/user/commodity/{commodityId}  ✅ PUT - 更新商品信息
/api/user/commodity/{commodityId}  ✅ DELETE - 删除商品
/api/user/commodity/{commodityId}/remove     ✅ POST - 下架商品
/api/user/commodity/{commodityId}/republish  ✅ POST - 重新上架
/api/user/commodity/{commodityId}/upload-image ✅ POST - 上传图片
```

## 前端对接建议

### 1. 商品管理页面
- 使用此接口获取商品详情进行编辑
- 显示商品的完整状态信息
- 提供编辑、下架、删除等操作入口

### 2. 权限处理
- 确保用户已登录
- 处理无权访问的情况
- 提供友好的错误提示

### 3. 数据展示
- 显示商品的完整信息
- 包括状态、可见性等管理信息
- 提供操作按钮（编辑、下架等）

## 总结

通过添加`getMyCommodityDetail`方法，解决了405错误问题：

✅ **问题解决**: 405 Method Not Allowed错误已修复
✅ **功能完善**: 用户可以查看自己发布的商品详情
✅ **权限控制**: 确保只能访问自己的商品
✅ **数据完整**: 返回商品的完整信息
✅ **错误处理**: 完善的异常处理机制
✅ **前端友好**: 便于前端进行商品管理功能开发

现在用户可以通过 `GET /api/user/commodity/{commodityId}` 正常获取自己发布的商品详情了。
