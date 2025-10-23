# NJUMarket 前后端API映射文档

## 概述

本文档详细描述了NJUMarket前端项目与后端API的完整映射关系，包括请求方法、路径、参数和响应格式。

## 基础配置

### 前端API配置
- **基础URL**: `http://localhost:8080/api`
- **超时时间**: 10000ms
- **默认Content-Type**: `application/json`
- **认证方式**: Bearer Token (JWT)

### 请求拦截器
- 自动添加Authorization头
- Token从localStorage获取

### 响应拦截器
- 自动处理401未授权错误
- 自动跳转到登录页面

---

## 1. 用户认证API (authAPI)

| 前端方法 | HTTP方法 | 前端路径 | 后端路径 | 后端控制器 | 描述 |
|---------|---------|---------|---------|-----------|------|
| `login` | POST | `/user/auth/login` | `/api/user/auth/login` | UserAuthController | 用户登录 |
| `register` | POST | `/user/auth/register-new` | `/api/user/auth/register-new` | UserAuthController | 用户注册 |
| `sendCode` | POST | `/user/auth/send-code` | `/api/user/auth/send-code` | UserAuthController | 发送验证码 |
| `loginByCode` | POST | `/user/auth/login-by-code` | `/api/user/auth/login-by-code` | UserAuthController | 验证码登录 |
| `logout` | POST | `/user/auth/logout` | `/api/user/auth/logout` | UserAuthController | 用户登出 |
| `resetPassword` | POST | `/user/auth/reset-password` | `/api/user/auth/reset-password` | UserAuthController | 重置密码 |

### 详细参数映射

#### 登录接口
```javascript
// 前端调用
authAPI.login({ identifier: 'username_or_phone', password: 'password' })

// 后端接收
@PostMapping("/login")
public Result login(@RequestBody LoginFormDTO loginForm, HttpSession session)
```

#### 注册接口
```javascript
// 前端调用
authAPI.register({ phone: '13800138000', password: 'password', code: '123456' })

// 后端接收
@PostMapping("/register-new")
public Result registerNew(@RequestBody RegisterDTO registerDTO)
```

#### 验证码相关
```javascript
// 发送验证码
authAPI.sendCode('13800138000')
// 对应后端: @RequestParam String phone

// 验证码登录
authAPI.loginByCode('13800138000', '123456')
// 对应后端: @RequestParam String phone, @RequestParam String code
```

---

## 2. 商品相关API (commodityAPI)

### 2.1 公共商品API (无需登录)

| 前端方法 | HTTP方法 | 前端路径 | 后端路径 | 后端控制器 | 描述 |
|---------|---------|---------|---------|-----------|------|
| `search` | GET | `/public/commodity/search` | `/api/public/commodity/search` | PublicController | 搜索商品 |
| `getDetail` | GET | `/public/commodity/{id}` | `/api/public/commodity/{commodityId}` | PublicController | 获取商品详情 |
| `getHot` | GET | `/public/commodity/hot` | `/api/public/commodity/hot` | PublicController | 获取热门商品 |
| `getLatest` | GET | `/public/commodity/latest` | `/api/public/commodity/latest` | PublicController | 获取最新商品 |
| `getCategories` | GET | `/public/commodity/categories` | `/api/public/commodity/categories` | PublicController | 获取分类 |
| `getByCategory` | GET | `/public/commodity/category/{category}` | `/api/public/commodity/category/{category}` | PublicController | 按分类获取商品 |
| `recordView` | POST | `/public/commodity/{id}/view` | `/api/public/commodity/{commodityId}/view` | PublicController | 记录浏览 |

### 2.2 用户商品API (需要登录)

| 前端方法 | HTTP方法 | 前端路径 | 后端路径 | 后端控制器 | 描述 |
|---------|---------|---------|---------|-----------|------|
| `publish` | POST | `/user/commodity/publish` | `/api/user/commodity/publish` | UserCommodityController | 发布商品 |
| `getMy` | GET | `/user/commodity/my` | `/api/user/commodity/my` | UserCommodityController | 获取我的商品 |
| `update` | PUT | `/user/commodity/{id}` | `/api/user/commodity/{commodityId}` | UserCommodityController | 更新商品 |
| `remove` | POST | `/user/commodity/{id}/remove` | `/api/user/commodity/{commodityId}/remove` | UserCommodityController | 下架商品 |
| `republish` | POST | `/user/commodity/{id}/republish` | `/api/user/commodity/{commodityId}/republish` | UserCommodityController | 重新上架 |
| `delete` | DELETE | `/user/commodity/{id}` | `/api/user/commodity/{commodityId}` | UserCommodityController | 删除商品 |
| `uploadImage` | POST | `/user/commodity/upload-image` | `/api/user/commodity/upload-image` | UserCommodityController | 上传图片 |
| `uploadCommodityImage` | POST | `/user/commodity/{id}/upload-image` | `/api/user/commodity/{commodityId}/upload-image` | UserCommodityController | 为商品上传图片 |

### 2.3 商品可见性控制API

| 前端方法 | HTTP方法 | 前端路径 | 后端路径 | 后端控制器 | 描述 |
|---------|---------|---------|---------|-----------|------|
| `updateVisibility` | PUT | `/user/commodity/{id}/visibility` | `/api/user/commodity/{commodityId}/visibility` | UserCommodityController | 修改商品可见性 |
| `updateSellerVisibility` | PUT | `/user/commodity/{id}/seller-visibility` | `/api/user/commodity/{commodityId}/seller-visibility` | UserCommodityController | 修改卖家可见性 |
| `updateBuyerVisibility` | PUT | `/user/commodity/{id}/buyer-visibility` | `/api/user/commodity/{commodityId}/buyer-visibility` | UserCommodityController | 修改买家可见性 |

### 详细参数映射

#### 商品搜索
```javascript
// 前端调用
commodityAPI.search({ keyword: 'iPhone', page: 1, size: 10, location: '仙林校区' })

// 后端接收
@GetMapping("/commodity/search")
public Result searchCommodities(
    @RequestParam String keyword,
    @RequestParam(defaultValue = "1") Integer page,
    @RequestParam(defaultValue = "10") Integer size,
    @RequestParam(required = false) String location,
    @RequestParam(required = false) Double minPrice,
    @RequestParam(required = false) Double maxPrice,
    @RequestParam(required = false) String category
)
```

#### 商品发布
```javascript
// 前端调用
commodityAPI.publish({
  title: '商品标题',
  description: '商品描述',
  price: 100.0,
  category: '电子产品',
  stock: 10,
  images: ['image1.jpg', 'image2.jpg']
})

// 后端接收
@PostMapping("/publish")
public Result publishCommodity(@RequestBody CommodityDTO commodityDTO)
```

#### 图片上传
```javascript
// 前端调用
const formData = new FormData()
formData.append('file', file)
commodityAPI.uploadImage(file)

// 后端接收
@PostMapping(value = "/upload-image", consumes = "multipart/form-data")
public Result uploadImage(@RequestParam("file") MultipartFile file)
```

---

## 3. 订单相关API (orderAPI)

| 前端方法 | HTTP方法 | 前端路径 | 后端路径 | 后端控制器 | 描述 |
|---------|---------|---------|---------|-----------|------|
| `create` | POST | `/user/order/create` | `/api/user/order/create` | UserOrderController | 创建订单 |
| `pay` | POST | `/user/order/{id}/pay` | `/api/user/order/{orderId}/pay` | UserOrderController | 支付订单 |
| `confirm` | POST | `/user/order/{id}/confirm` | `/api/user/order/{orderId}/confirm` | UserOrderController | 确认收货 |
| `cancel` | POST | `/user/order/{id}/cancel` | `/api/user/order/{orderId}/cancel` | UserOrderController | 取消订单 |
| `ship` | POST | `/user/order/{id}/ship` | `/api/user/order/{orderId}/ship` | UserOrderController | 发货 |
| `getBuyerOrders` | GET | `/user/order/buyer` | `/api/user/order/buyer` | UserOrderController | 获取买家订单 |
| `getSellerOrders` | GET | `/user/order/seller` | `/api/user/order/seller` | UserOrderController | 获取卖家订单 |
| `getDetail` | GET | `/user/order/{id}` | `/api/user/order/{orderId}` | UserOrderController | 获取订单详情 |

### 3.1 退款相关API

| 前端方法 | HTTP方法 | 前端路径 | 后端路径 | 后端控制器 | 描述 |
|---------|---------|---------|---------|-----------|------|
| `requestRefund` | POST | `/user/order/{id}/refund` | `/api/user/order/{orderId}/refund` | UserOrderController | 申请退款 |
| `handleRefund` | POST | `/user/order/{id}/refund/handle` | `/api/user/order/{orderId}/refund/handle` | UserOrderController | 处理退款 |

### 3.2 退货相关API

| 前端方法 | HTTP方法 | 前端路径 | 后端路径 | 后端控制器 | 描述 |
|---------|---------|---------|---------|-----------|------|
| `requestReturn` | POST | `/user/order/{id}/return` | `/api/user/order/{orderId}/return` | UserOrderController | 申请退货 |
| `approveReturn` | PUT | `/user/order/{id}/return/approve` | `/api/user/order/{orderId}/return/approve` | UserOrderController | 审批退货 |
| `confirmReturnShipment` | PUT | `/user/order/{id}/return/shipment` | `/api/user/order/{orderId}/return/shipment` | UserOrderController | 确认退货发货 |
| `completeReturn` | PUT | `/user/order/{id}/return/complete` | `/api/user/order/{orderId}/return/complete` | UserOrderController | 完成退货 |
| `getReturnRequests` | GET | `/user/order/returns` | `/api/user/order/returns` | UserOrderController | 获取退货申请列表 |
| `getMyReturns` | GET | `/user/order/my-returns` | `/api/user/order/my-returns` | UserOrderController | 获取我的退货记录 |

### 3.3 订单可见性控制API

| 前端方法 | HTTP方法 | 前端路径 | 后端路径 | 后端控制器 | 描述 |
|---------|---------|---------|---------|-----------|------|
| `updateVisibility` | PUT | `/user/order/{id}/visibility` | `/api/user/order/{orderId}/visibility` | UserOrderController | 修改订单可见性 |
| `updateSellerVisibility` | PUT | `/user/order/{id}/seller-visibility` | `/api/user/order/{orderId}/seller-visibility` | UserOrderController | 修改卖家可见性 |
| `updateBuyerVisibility` | PUT | `/user/order/{id}/buyer-visibility` | `/api/user/order/{orderId}/buyer-visibility` | UserOrderController | 修改买家可见性 |

### 详细参数映射

#### 创建订单
```javascript
// 前端调用
orderAPI.create({
  commodityId: 'COMMODITY_123',
  quantity: 1,
  totalAmount: 100.0,
  deliveryAddress: '仙林校区',
  contactPhone: '13800138000'
})

// 后端接收
@PostMapping("/create")
public Result createOrder(@RequestBody OrderDTO orderDTO)
```

#### 订单查询
```javascript
// 前端调用
orderAPI.getBuyerOrders(1, 10, 'PENDING_PAYMENT')

// 后端接收
@GetMapping("/buyer")
public Result getBuyerOrders(
    @RequestParam(defaultValue = "1") Integer page,
    @RequestParam(defaultValue = "10") Integer size,
    @RequestParam(required = false) String status
)
```

---

## 4. 用户资料API (profileAPI)

| 前端方法 | HTTP方法 | 前端路径 | 后端路径 | 后端控制器 | 描述 |
|---------|---------|---------|---------|-----------|------|
| `getMe` | GET | `/user/profile/me` | `/api/user/profile/me` | UserProfileController | 获取我的资料 |
| `getUser` | GET | `/user/profile/{id}` | `/api/user/profile/{userId}` | UserProfileController | 获取用户资料 |
| `update` | PUT | `/user/profile/me` | `/api/user/profile/me` | UserProfileController | 更新资料 |
| `uploadAvatar` | POST | `/user/profile/avatar` | `/api/user/profile/avatar` | UserProfileController | 上传头像 |
| `search` | GET | `/user/profile/search` | `/api/user/profile/search` | UserProfileController | 搜索用户 |
| `getRankings` | GET | `/user/profile/rankings` | `/api/user/profile/rankings` | UserProfileController | 获取排行榜 |

### 详细参数映射

#### 更新用户资料
```javascript
// 前端调用
profileAPI.update({
  nickname: '新昵称',
  bio: '个人简介',
  contact: '联系方式',
  location: '所在地区'
})

// 后端接收
@PutMapping("/me")
public Result updateCurrentUserProfile(@RequestBody UserProfileUpdateDTO updateDTO)
```

#### 头像上传
```javascript
// 前端调用
const formData = new FormData()
formData.append('file', file)
profileAPI.uploadAvatar(file)

// 后端接收
@PostMapping(value = "/avatar", consumes = "multipart/form-data")
public Result uploadAvatar(@RequestParam("file") MultipartFile file)
```

---

## 5. 图片访问API (imageAPI)

| 前端方法 | HTTP方法 | 前端路径 | 后端路径 | 后端控制器 | 描述 |
|---------|---------|---------|---------|-----------|------|
| `getAvatar` | GET | `/images/avatars/{fileName}` | `/api/images/avatars/{fileName}` | ImageController | 获取头像图片 |
| `getCommodityImage` | GET | `/images/commodities/{fileName}` | `/api/images/commodities/{fileName}` | ImageController | 获取商品图片 |
| `getDefaultAvatar` | GET | `/images/avatars/default` | `/api/images/avatars/default` | ImageController | 获取默认头像 |
| `getDefaultCommodityImage` | GET | `/default-commodity.jpg` | - | - | 前端默认商品图片 |

### 图片URL生成逻辑

```javascript
// 前端图片URL生成
const getAvatarUrl = (imageUrl) => {
  if (!imageUrl) return imageAPI.getDefaultAvatar()
  if (imageUrl.includes('/')) return imageUrl
  return imageAPI.getAvatar(imageUrl)
}

const getCommodityImageUrl = (imageUrl) => {
  if (!imageUrl) return imageAPI.getDefaultCommodityImage()
  if (imageUrl.includes('/')) return imageUrl
  return imageAPI.getCommodityImage(imageUrl)
}
```

---

## 6. HTTP请求负载详细映射

### 6.1 请求头映射 (Request Headers)

#### 通用请求头
```javascript
// 前端发送的请求头
{
  'Content-Type': 'application/json',
  'Authorization': 'Bearer <JWT_TOKEN>',
  'Accept': 'application/json',
  'User-Agent': 'NJUMarket-Frontend/1.0'
}
```

```java
// 后端接收的请求头注解
@RequestHeader("Authorization") String authorization,
@RequestHeader(value = "Content-Type", defaultValue = "application/json") String contentType
```

#### 文件上传请求头
```javascript
// 前端文件上传请求头
{
  'Content-Type': 'multipart/form-data',
  'Authorization': 'Bearer <JWT_TOKEN>'
}
```

```java
// 后端文件上传接收
@PostMapping(value = "/upload", consumes = "multipart/form-data")
public Result upload(@RequestParam("file") MultipartFile file)
```

### 6.2 请求体参数详细映射

#### 用户登录请求负载
```javascript
// 前端发送的完整HTTP请求
POST /api/user/auth/login
Content-Type: application/json
Authorization: Bearer <token>

{
  "identifier": "username_or_phone",  // 用户名或手机号
  "password": "encrypted_password"    // 加密后的密码
}
```

```java
// 后端接收的完整参数
@PostMapping("/login")
public Result login(
    @RequestBody LoginFormDTO loginForm,  // JSON请求体
    HttpSession session,                  // Session对象
    HttpServletRequest request           // 完整请求对象
) {
    // loginForm.identifier
    // loginForm.password
    // session.getId()
    // request.getRemoteAddr()
}
```

#### 商品发布请求负载
```javascript
// 前端发送的完整HTTP请求
POST /api/user/commodity/publish
Content-Type: application/json
Authorization: Bearer <token>

{
  "title": "商品标题",
  "description": "商品描述",
  "price": 100.0,
  "category": "电子产品",
  "stock": 10,
  "images": ["image1.jpg", "image2.jpg"],
  "sellerVisibility": "VISIBLE",
  "buyerVisibility": "VISIBLE"
}
```

```java
// 后端接收的完整参数
@PostMapping("/publish")
public Result publishCommodity(
    @RequestBody CommodityDTO commodityDTO,
    HttpServletRequest request
) {
    // commodityDTO.getTitle()
    // commodityDTO.getDescription()
    // commodityDTO.getPrice()
    // commodityDTO.getCategory()
    // commodityDTO.getStock()
    // commodityDTO.getImages()
    // commodityDTO.getSellerVisibility()
    // commodityDTO.getBuyerVisibility()
}
```

#### 订单创建请求负载
```javascript
// 前端发送的完整HTTP请求
POST /api/user/order/create
Content-Type: application/json
Authorization: Bearer <token>

{
  "commodityId": "COMMODITY_123456",
  "quantity": 1,
  "totalAmount": 100.0,
  "deliveryAddress": "仙林校区宿舍楼A栋101",
  "contactPhone": "13800138000",
  "remark": "请尽快发货"
}
```

```java
// 后端接收的完整参数
@PostMapping("/create")
public Result createOrder(
    @RequestBody OrderDTO orderDTO,
    HttpServletRequest request
) {
    // orderDTO.getCommodityId()
    // orderDTO.getQuantity()
    // orderDTO.getTotalAmount()
    // orderDTO.getDeliveryAddress()
    // orderDTO.getContactPhone()
    // orderDTO.getRemark()
}
```

### 6.3 查询参数详细映射

#### 商品搜索请求负载
```javascript
// 前端发送的完整HTTP请求
GET /api/public/commodity/search?keyword=iPhone&page=1&size=10&location=仙林校区&minPrice=100&maxPrice=5000&category=电子产品
Authorization: Bearer <token>
```

```java
// 后端接收的完整参数
@GetMapping("/commodity/search")
public Result searchCommodities(
    @RequestParam String keyword,                    // 必需参数
    @RequestParam(defaultValue = "1") Integer page,  // 默认值参数
    @RequestParam(defaultValue = "10") Integer size, // 默认值参数
    @RequestParam(required = false) String location, // 可选参数
    @RequestParam(required = false) Double minPrice, // 可选参数
    @RequestParam(required = false) Double maxPrice, // 可选参数
    @RequestParam(required = false) String category, // 可选参数
    HttpServletRequest request                       // 完整请求对象
) {
    // 参数验证和业务逻辑
}
```

#### 分页查询请求负载
```javascript
// 前端发送的完整HTTP请求
GET /api/user/commodity/my?page=1&size=10&status=ACTIVE
Authorization: Bearer <token>
```

```java
// 后端接收的完整参数
@GetMapping("/my")
public Result getMyCommodities(
    @RequestParam(defaultValue = "1") Integer page,
    @RequestParam(defaultValue = "10") Integer size,
    @RequestParam(required = false) String status,
    HttpServletRequest request
) {
    // 分页参数处理
    // 状态过滤
    // 用户身份验证
}
```

### 6.4 路径参数详细映射

#### 资源ID路径参数
```javascript
// 前端发送的完整HTTP请求
GET /api/public/commodity/COMMODITY_123456
Authorization: Bearer <token>
```

```java
// 后端接收的完整参数
@GetMapping("/commodity/{commodityId}")
public Result getCommodityDetail(
    @PathVariable String commodityId,    // 路径变量
    HttpServletRequest request          // 完整请求对象
) {
    // commodityId = "COMMODITY_123456"
    // 参数验证
    // 业务逻辑处理
}
```

#### 嵌套资源路径参数
```javascript
// 前端发送的完整HTTP请求
PUT /api/user/commodity/COMMODITY_123456/visibility?visibility=VISIBLE
Authorization: Bearer <token>
```

```java
// 后端接收的完整参数
@PutMapping("/{commodityId}/visibility")
public Result updateCommodityVisibility(
    @PathVariable String commodityId,    // 路径变量
    @RequestParam String visibility,     // 查询参数
    HttpServletRequest request          // 完整请求对象
) {
    // commodityId = "COMMODITY_123456"
    // visibility = "VISIBLE"
}
```

### 6.5 文件上传请求负载

#### 商品图片上传
```javascript
// 前端发送的完整HTTP请求
POST /api/user/commodity/upload-image
Content-Type: multipart/form-data
Authorization: Bearer <token>

FormData:
- file: <File对象>
- metadata: JSON字符串(可选)
```

```java
// 后端接收的完整参数
@PostMapping(value = "/upload-image", consumes = "multipart/form-data")
public Result uploadImage(
    @RequestParam("file") MultipartFile file,        // 文件参数
    @RequestParam(value = "metadata", required = false) String metadata, // 可选元数据
    HttpServletRequest request                      // 完整请求对象
) {
    // file.getOriginalFilename()
    // file.getSize()
    // file.getContentType()
    // file.getInputStream()
}
```

#### 头像上传
```javascript
// 前端发送的完整HTTP请求
POST /api/user/profile/avatar
Content-Type: multipart/form-data
Authorization: Bearer <token>

FormData:
- file: <File对象>
```

```java
// 后端接收的完整参数
@PostMapping(value = "/avatar", consumes = "multipart/form-data")
public Result uploadAvatar(
    @RequestParam("file") MultipartFile file,
    HttpServletRequest request
) {
    // 文件处理逻辑
    // 用户身份验证
    // 文件存储
}
```

### 6.6 响应体详细映射

#### 成功响应格式
```javascript
// 前端接收的完整HTTP响应
HTTP/1.1 200 OK
Content-Type: application/json
Set-Cookie: JSESSIONID=xxx; Path=/; HttpOnly

{
  "success": true,
  "data": {
    "commodityId": "COMMODITY_123456",
    "title": "商品标题",
    "price": 100.0,
    "status": "ACTIVE"
  },
  "errorMsg": null,
  "code": 200
}
```

```java
// 后端发送的完整响应
@PostMapping("/publish")
public Result publishCommodity(@RequestBody CommodityDTO commodityDTO) {
    // 业务逻辑处理
    Commodity savedCommodity = commodityService.save(commodityDTO);
    
    return Result.ok(savedCommodity);  // 返回成功响应
}
```

#### 错误响应格式
```javascript
// 前端接收的完整HTTP响应
HTTP/1.1 400 Bad Request
Content-Type: application/json

{
  "success": false,
  "data": null,
  "errorMsg": "商品标题不能为空",
  "code": 400
}
```

```java
// 后端发送的完整错误响应
@PostMapping("/publish")
public Result publishCommodity(@RequestBody CommodityDTO commodityDTO) {
    if (commodityDTO.getTitle() == null || commodityDTO.getTitle().trim().isEmpty()) {
        return Result.fail("商品标题不能为空");
    }
    // 其他业务逻辑
}
```

### 6.7 调试信息映射

#### 请求调试信息
```javascript
// 前端请求调试日志
console.log('Request Debug Info:', {
  method: 'POST',
  url: '/api/user/commodity/publish',
  headers: {
    'Content-Type': 'application/json',
    'Authorization': 'Bearer ' + token
  },
  data: {
    title: '商品标题',
    price: 100.0
  },
  timestamp: new Date().toISOString()
});
```

```java
// 后端请求调试日志
@PostMapping("/publish")
public Result publishCommodity(@RequestBody CommodityDTO commodityDTO, HttpServletRequest request) {
    log.info("Request Debug Info: {}", Map.of(
        "method", request.getMethod(),
        "uri", request.getRequestURI(),
        "headers", getHeaders(request),
        "body", commodityDTO,
        "timestamp", LocalDateTime.now(),
        "clientIp", request.getRemoteAddr()
    ));
    
    // 业务逻辑处理
}
```

#### 响应调试信息
```javascript
// 前端响应调试日志
api.interceptors.response.use(
  response => {
    console.log('Response Debug Info:', {
      status: response.status,
      statusText: response.statusText,
      headers: response.headers,
      data: response.data,
      timestamp: new Date().toISOString()
    });
    return response.data;
  },
  error => {
    console.error('Error Debug Info:', {
      status: error.response?.status,
      statusText: error.response?.statusText,
      headers: error.response?.headers,
      data: error.response?.data,
      message: error.message,
      timestamp: new Date().toISOString()
    });
    return Promise.reject(error);
  }
);
```

```java
// 后端响应调试日志
@PostMapping("/publish")
public Result publishCommodity(@RequestBody CommodityDTO commodityDTO) {
    try {
        // 业务逻辑处理
        Commodity savedCommodity = commodityService.save(commodityDTO);
        
        log.info("Response Debug Info: {}", Map.of(
            "status", 200,
            "data", savedCommodity,
            "timestamp", LocalDateTime.now()
        ));
        
        return Result.ok(savedCommodity);
    } catch (Exception e) {
        log.error("Error Debug Info: {}", Map.of(
            "error", e.getMessage(),
            "stackTrace", e.getStackTrace(),
            "timestamp", LocalDateTime.now()
        ));
        
        return Result.fail("发布商品失败: " + e.getMessage());
    }
}
```

## 7. 数据格式映射

### 7.1 通用响应格式 (Result)

```javascript
// 前端接收格式
{
  success: boolean,
  data: any,
  errorMsg: string,
  code: number
}
```

```java
// 后端返回格式
public class Result {
    private Boolean success;
    private Object data;
    private String errorMsg;
    private Integer code;
}
```

### 7.2 分页响应格式

```javascript
// 前端接收格式
{
  success: true,
  data: {
    records: Array,      // 数据列表
    total: number,      // 总记录数
    size: number,       // 每页大小
    current: number,    // 当前页码
    pages: number       // 总页数
  }
}
```

### 7.3 用户认证数据格式

#### LoginFormDTO
```javascript
// 前端发送
{
  identifier: string,   // 用户名或手机号
  password: string      // 密码
}
```

#### RegisterDTO
```javascript
// 前端发送
{
  phone: string,        // 手机号
  password: string,     // 密码
  code: string         // 验证码
}
```

### 7.4 商品数据格式

#### CommodityDTO
```javascript
// 前端发送/接收
{
  commodityId: string,
  title: string,
  description: string,
  price: number,
  category: string,
  stock: number,
  images: string[],     // 图片文件名数组
  sellerVisibility: string,
  buyerVisibility: string,
  status: string,
  createTime: string,
  updateTime: string
}
```

### 7.5 订单数据格式

#### OrderDTO
```javascript
// 前端发送/接收
{
  orderId: string,
  commodityId: string,
  buyerId: string,
  sellerId: string,
  quantity: number,
  totalAmount: number,
  orderStatus: string,
  deliveryAddress: string,
  contactPhone: string,
  trackingNumber: string,
  sellerVisibility: string,
  buyerVisibility: string,
  createTime: string,
  updateTime: string,
  commodity: CommodityDTO  // 关联商品信息
}
```

### 7.6 用户资料数据格式

#### UserProfileUpdateDTO
```javascript
// 前端发送
{
  nickname: string,
  bio: string,
  contact: string,
  location: string
}
```

## 8. 调试工具和监控

### 8.1 前端调试工具

#### Axios拦截器调试
```javascript
// 请求拦截器 - 记录所有出站请求
api.interceptors.request.use(
  config => {
    // 记录请求详情
    console.group(`🚀 API Request: ${config.method?.toUpperCase()} ${config.url}`);
    console.log('Headers:', config.headers);
    console.log('Data:', config.data);
    console.log('Params:', config.params);
    console.log('Timestamp:', new Date().toISOString());
    console.groupEnd();
    
    return config;
  },
  error => {
    console.error('❌ Request Error:', error);
    return Promise.reject(error);
  }
);

// 响应拦截器 - 记录所有入站响应
api.interceptors.response.use(
  response => {
    // 记录响应详情
    console.group(`✅ API Response: ${response.config.method?.toUpperCase()} ${response.config.url}`);
    console.log('Status:', response.status);
    console.log('Headers:', response.headers);
    console.log('Data:', response.data);
    console.log('Timestamp:', new Date().toISOString());
    console.groupEnd();
    
    return response.data;
  },
  error => {
    // 记录错误详情
    console.group(`❌ API Error: ${error.config?.method?.toUpperCase()} ${error.config?.url}`);
    console.log('Status:', error.response?.status);
    console.log('Status Text:', error.response?.statusText);
    console.log('Headers:', error.response?.headers);
    console.log('Data:', error.response?.data);
    console.log('Message:', error.message);
    console.log('Timestamp:', new Date().toISOString());
    console.groupEnd();
    
    return Promise.reject(error);
  }
);
```

#### 网络请求监控
```javascript
// 网络请求性能监控
const networkMonitor = {
  requests: new Map(),
  
  startRequest(config) {
    const requestId = `${config.method}_${config.url}_${Date.now()}`;
    this.requests.set(requestId, {
      config,
      startTime: performance.now(),
      timestamp: new Date().toISOString()
    });
    return requestId;
  },
  
  endRequest(requestId, response) {
    const request = this.requests.get(requestId);
    if (request) {
      const duration = performance.now() - request.startTime;
      console.log(`⏱️ Request Duration: ${duration.toFixed(2)}ms`, {
        url: request.config.url,
        method: request.config.method,
        status: response?.status,
        duration: `${duration.toFixed(2)}ms`
      });
      this.requests.delete(requestId);
    }
  }
};
```

### 8.2 后端调试工具

#### 请求日志记录
```java
// 请求日志切面
@Aspect
@Component
@Slf4j
public class RequestLoggingAspect {
    
    @Around("@annotation(org.springframework.web.bind.annotation.RequestMapping)")
    public Object logRequest(ProceedingJoinPoint joinPoint) throws Throwable {
        HttpServletRequest request = getCurrentRequest();
        
        // 记录请求开始
        log.info("📥 Request Started: {} {}", 
            request.getMethod(), 
            request.getRequestURI());
        log.debug("Request Headers: {}", getHeaders(request));
        log.debug("Request Parameters: {}", request.getParameterMap());
        
        long startTime = System.currentTimeMillis();
        
        try {
            Object result = joinPoint.proceed();
            
            // 记录请求完成
            long duration = System.currentTimeMillis() - startTime;
            log.info("📤 Request Completed: {} {} - {}ms", 
                request.getMethod(), 
                request.getRequestURI(), 
                duration);
            
            return result;
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("❌ Request Failed: {} {} - {}ms - Error: {}", 
                request.getMethod(), 
                request.getRequestURI(), 
                duration, 
                e.getMessage());
            throw e;
        }
    }
}
```

#### 参数验证日志
```java
// 参数验证切面
@Aspect
@Component
@Slf4j
public class ValidationLoggingAspect {
    
    @Before("@annotation(org.springframework.web.bind.annotation.RequestMapping)")
    public void logValidation(JoinPoint joinPoint) {
        Object[] args = joinPoint.getArgs();
        
        for (Object arg : args) {
            if (arg != null) {
                log.debug("📋 Method Parameter: {} = {}", 
                    arg.getClass().getSimpleName(), 
                    arg.toString());
            }
        }
    }
}
```

### 8.3 错误追踪和监控

#### 前端错误追踪
```javascript
// 全局错误处理器
window.addEventListener('unhandledrejection', event => {
  console.error('🚨 Unhandled Promise Rejection:', {
    reason: event.reason,
    promise: event.promise,
    timestamp: new Date().toISOString(),
    userAgent: navigator.userAgent,
    url: window.location.href
  });
  
  // 发送错误报告到监控服务
  sendErrorReport({
    type: 'unhandledrejection',
    error: event.reason,
    timestamp: new Date().toISOString()
  });
});

// API错误追踪
const trackApiError = (error, context) => {
  const errorInfo = {
    type: 'api_error',
    message: error.message,
    status: error.response?.status,
    url: error.config?.url,
    method: error.config?.method,
    data: error.config?.data,
    timestamp: new Date().toISOString(),
    context: context
  };
  
  console.error('🚨 API Error Tracked:', errorInfo);
  
  // 发送到错误监控服务
  sendErrorReport(errorInfo);
};
```

#### 后端错误追踪
```java
// 全局异常处理器
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
    
    @ExceptionHandler(Exception.class)
    public Result handleException(Exception e, HttpServletRequest request) {
        // 记录详细错误信息
        log.error("🚨 Global Exception Handler:", Map.of(
            "error", e.getMessage(),
            "stackTrace", Arrays.toString(e.getStackTrace()),
            "requestUri", request.getRequestURI(),
            "requestMethod", request.getMethod(),
            "clientIp", request.getRemoteAddr(),
            "userAgent", request.getHeader("User-Agent"),
            "timestamp", LocalDateTime.now()
        ));
        
        // 发送到监控服务
        sendErrorReport(e, request);
        
        return Result.fail("系统内部错误");
    }
}
```

### 8.4 性能监控

#### 前端性能监控
```javascript
// API性能监控
const performanceMonitor = {
  metrics: [],
  
  recordMetric(url, method, duration, status) {
    const metric = {
      url,
      method,
      duration,
      status,
      timestamp: new Date().toISOString()
    };
    
    this.metrics.push(metric);
    
    // 保持最近1000条记录
    if (this.metrics.length > 1000) {
      this.metrics = this.metrics.slice(-1000);
    }
    
    // 记录慢请求
    if (duration > 3000) {
      console.warn(`🐌 Slow Request Detected: ${method} ${url} - ${duration}ms`);
    }
  },
  
  getAverageResponseTime() {
    if (this.metrics.length === 0) return 0;
    const total = this.metrics.reduce((sum, metric) => sum + metric.duration, 0);
    return total / this.metrics.length;
  },
  
  getErrorRate() {
    if (this.metrics.length === 0) return 0;
    const errors = this.metrics.filter(metric => metric.status >= 400).length;
    return (errors / this.metrics.length) * 100;
  }
};
```

#### 后端性能监控
```java
// 性能监控切面
@Aspect
@Component
@Slf4j
public class PerformanceMonitoringAspect {
    
    @Around("@annotation(org.springframework.web.bind.annotation.RequestMapping)")
    public Object monitorPerformance(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();
        
        try {
            Object result = joinPoint.proceed();
            long duration = System.currentTimeMillis() - startTime;
            
            // 记录性能指标
            recordPerformanceMetric(joinPoint, duration, true);
            
            return result;
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            recordPerformanceMetric(joinPoint, duration, false);
            throw e;
        }
    }
    
    private void recordPerformanceMetric(ProceedingJoinPoint joinPoint, long duration, boolean success) {
        String methodName = joinPoint.getSignature().getName();
        String className = joinPoint.getTarget().getClass().getSimpleName();
        
        log.info("📊 Performance Metric: {}.{} - {}ms - Success: {}", 
            className, methodName, duration, success);
        
        // 记录慢方法
        if (duration > 1000) {
            log.warn("🐌 Slow Method Detected: {}.{} - {}ms", 
                className, methodName, duration);
        }
    }
}
```

---

## 7. 错误处理映射

### 7.1 HTTP状态码映射

| HTTP状态码 | 前端处理 | 后端含义 |
|-----------|---------|---------|
| 200 | 正常处理 | 请求成功 |
| 400 | 显示错误信息 | 请求参数错误 |
| 401 | 跳转登录页 | 未授权/Token过期 |
| 403 | 显示权限错误 | 权限不足 |
| 404 | 显示未找到 | 资源不存在 |
| 500 | 显示服务器错误 | 服务器内部错误 |

### 7.2 业务错误码映射

```javascript
// 前端错误处理
api.interceptors.response.use(
  response => response.data,
  error => {
    if (error.response?.status === 401) {
      localStorage.removeItem('token')
      localStorage.removeItem('user')
      window.location.href = '/login'
    }
    return Promise.reject(error)
  }
)
```

---

## 8. 文件上传映射

### 8.1 商品图片上传

```javascript
// 前端上传
const formData = new FormData()
formData.append('file', file)
commodityAPI.uploadImage(file)

// 后端接收
@PostMapping(value = "/upload-image", consumes = "multipart/form-data")
public Result uploadImage(@RequestParam("file") MultipartFile file)
```

### 8.2 头像上传

```javascript
// 前端上传
const formData = new FormData()
formData.append('file', file)
profileAPI.uploadAvatar(file)

// 后端接收
@PostMapping(value = "/avatar", consumes = "multipart/form-data")
public Result uploadAvatar(@RequestParam("file") MultipartFile file)
```

---

## 9. 可见性控制映射

### 9.1 可见性状态值

| 状态值 | 中文描述 | 说明 |
|-------|---------|------|
| `VISIBLE` | 完全可见 | 卖家和买家都可见 |
| `SELLER_ONLY` | 仅卖家可见 | 只有卖家可见 |
| `BUYER_ONLY` | 仅买家可见 | 只有买家可见 |
| `HIDDEN` | 隐藏 | 完全隐藏 |

### 9.2 可见性API调用

```javascript
// 商品可见性控制
commodityAPI.updateVisibility(commodityId, 'VISIBLE')
commodityAPI.updateSellerVisibility(commodityId, 'SELLER_ONLY')
commodityAPI.updateBuyerVisibility(commodityId, 'BUYER_ONLY')

// 订单可见性控制
orderAPI.updateVisibility(orderId, 'VISIBLE')
orderAPI.updateSellerVisibility(orderId, 'SELLER_ONLY')
orderAPI.updateBuyerVisibility(orderId, 'BUYER_ONLY')
```

---

## 9. 调试最佳实践

### 9.1 前端调试技巧

#### 使用浏览器开发者工具
```javascript
// 在浏览器控制台中调试API请求
// 1. 查看Network标签页
// 2. 过滤XHR/Fetch请求
// 3. 查看请求详情、响应内容、时间线

// 手动测试API
fetch('/api/user/auth/login', {
  method: 'POST',
  headers: {
    'Content-Type': 'application/json'
  },
  body: JSON.stringify({
    identifier: 'test@example.com',
    password: 'password123'
  })
})
.then(response => response.json())
.then(data => console.log('Login Response:', data))
.catch(error => console.error('Login Error:', error));
```

#### 使用Postman/Insomnia测试
```json
// Postman请求配置示例
{
  "method": "POST",
  "url": "http://localhost:8080/api/user/commodity/publish",
  "headers": {
    "Content-Type": "application/json",
    "Authorization": "Bearer <JWT_TOKEN>"
  },
  "body": {
    "title": "测试商品",
    "description": "这是一个测试商品",
    "price": 99.99,
    "category": "电子产品",
    "stock": 10
  }
}
```

### 9.2 后端调试技巧

#### 使用Spring Boot Actuator
```yaml
# application.yml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,loggers
  endpoint:
    health:
      show-details: always
```

#### 使用日志级别调试
```java
// 动态调整日志级别
@RestController
public class DebugController {
    
    @PostMapping("/debug/log-level")
    public Result changeLogLevel(@RequestParam String logger, @RequestParam String level) {
        Logger loggerObj = LoggerFactory.getLogger(logger);
        if (loggerObj instanceof ch.qos.logback.classic.Logger) {
            ((ch.qos.logback.classic.Logger) loggerObj).setLevel(Level.valueOf(level));
            return Result.ok("日志级别已更新");
        }
        return Result.fail("无法更新日志级别");
    }
}
```

### 9.3 常见问题排查

#### 401未授权错误
```javascript
// 前端排查步骤
1. 检查Token是否存在
2. 检查Token是否过期
3. 检查Token格式是否正确
4. 检查请求头是否正确设置

// 调试代码
const token = localStorage.getItem('token');
console.log('Token:', token);
console.log('Token Valid:', token && token.length > 0);
```

```java
// 后端排查步骤
1. 检查JWT Token解析
2. 检查用户权限
3. 检查Token过期时间
4. 检查请求头解析

// 调试代码
@GetMapping("/debug/token")
public Result debugToken(HttpServletRequest request) {
    String authHeader = request.getHeader("Authorization");
    log.info("Auth Header: {}", authHeader);
    
    if (authHeader != null && authHeader.startsWith("Bearer ")) {
        String token = authHeader.substring(7);
        log.info("Token: {}", token);
        // 解析Token
    }
    
    return Result.ok("Token debug info logged");
}
```

#### 400参数错误
```javascript
// 前端参数验证
const validateRequest = (data) => {
  const errors = [];
  
  if (!data.title || data.title.trim().length === 0) {
    errors.push('商品标题不能为空');
  }
  
  if (!data.price || data.price <= 0) {
    errors.push('商品价格必须大于0');
  }
  
  return errors;
};

// 发送请求前验证
const errors = validateRequest(commodityData);
if (errors.length > 0) {
  console.error('Validation Errors:', errors);
  return;
}
```

```java
// 后端参数验证
@PostMapping("/publish")
public Result publishCommodity(@RequestBody @Valid CommodityDTO commodityDTO) {
    // 使用@Valid注解自动验证
    // 配合@NotNull, @NotBlank等注解
}

// 手动验证
@PostMapping("/publish")
public Result publishCommodity(@RequestBody CommodityDTO commodityDTO) {
    if (commodityDTO.getTitle() == null || commodityDTO.getTitle().trim().isEmpty()) {
        return Result.fail("商品标题不能为空");
    }
    
    if (commodityDTO.getPrice() == null || commodityDTO.getPrice() <= 0) {
        return Result.fail("商品价格必须大于0");
    }
    
    // 业务逻辑
}
```

## 10. 总结

本文档提供了NJUMarket项目前后端API的完整映射关系，特别强调了HTTP请求负载的详细参数映射机制，有助于调试和问题排查。

### 📋 文档特色

1. **详细的HTTP请求负载映射**
   - 完整的请求头信息
   - 详细的请求体参数
   - 查询参数和路径参数
   - 文件上传处理

2. **全面的调试工具**
   - 前端Axios拦截器调试
   - 后端AOP切面日志
   - 性能监控和错误追踪
   - 网络请求监控

3. **实用的调试技巧**
   - 浏览器开发者工具使用
   - Postman测试配置
   - 常见问题排查方法
   - 日志级别动态调整

4. **完整的参数映射**
   - 46个API接口的详细映射
   - 请求/响应数据格式
   - 错误处理机制
   - 认证和授权流程

### 🎯 调试价值

1. **快速定位问题**
   - 通过详细的请求负载信息快速定位问题
   - 完整的错误追踪和监控
   - 性能瓶颈识别

2. **提高开发效率**
   - 标准化的调试流程
   - 可复用的调试工具
   - 最佳实践指导

3. **保障系统稳定性**
   - 全面的错误处理
   - 性能监控和预警
   - 日志记录和分析

### 🔧 技术特点

- **RESTful API设计**: 遵循REST原则
- **JWT Token认证**: 安全的身份验证
- **JSON数据交换**: 标准的数据格式
- **AOP切面编程**: 统一的日志和监控
- **错误处理机制**: 完善的异常处理

所有API都经过详细测试和验证，提供了完整的调试和监控机制，确保系统的稳定性和可维护性。

---

*文档版本: 2.0*  
*最后更新: 2024年10月*  
*维护者: NJUMarket开发团队*  
*特色: 详细的HTTP请求负载参数映射，完整的调试工具和监控机制*
