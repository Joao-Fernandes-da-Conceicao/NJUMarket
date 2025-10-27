# 南大市场前端项目

基于Vue3的用户端电商平台前端项目，对接NJUMarket后端API。

## 项目特性

- 🎨 使用主题色 #6A015E 的现代化UI设计
- 📱 响应式设计，支持移动端和桌面端
- 🔐 完整的用户认证系统（登录、注册、验证码）
- 🛒 商品浏览、搜索、详情查看
- 📦 订单管理（购买、支付、确认收货、退货）
- 👤 用户资料管理
- 🏪 商品发布和管理功能

## 技术栈

- **前端框架**: Vue 3
- **路由**: Vue Router 4
- **状态管理**: Pinia
- **UI组件库**: Element Plus
- **HTTP客户端**: Axios
- **构建工具**: Vue CLI

## 项目结构

```
src/
├── api/           # API接口封装
├── components/    # 公共组件
├── router/         # 路由配置
├── stores/        # Pinia状态管理
├── views/         # 页面组件
│   ├── Home.vue              # 首页
│   ├── Login.vue             # 登录页
│   ├── Register.vue          # 注册页
│   ├── CommodityList.vue     # 商品列表
│   ├── CommodityDetail.vue   # 商品详情
│   ├── MyOrders.vue         # 我的订单
│   ├── MyCommodities.vue    # 我的商品
│   ├── PublishCommodity.vue  # 发布商品
│   └── UserProfile.vue      # 用户资料
├── App.vue        # 根组件
└── main.js        # 入口文件
```

## 主要功能

### 用户认证
- 用户名/手机号+密码登录
- 手机号+验证码登录
- 用户注册
- 密码重置

### 商品功能
- 商品浏览和搜索
- 商品分类筛选
- 商品详情查看
- 商品发布和管理
- 图片上传
- 智能商品展示（无照片时显示"暂无照片"）

### 订单功能
- 创建订单
- 订单支付
- 订单状态管理
- 退货申请
- 订单历史查看

### 用户功能
- 个人资料管理（支持昵称、个人简介、联系方式、所在地区）
- 头像上传
- 用户统计信息

### 可见性控制
- 商品可见性管理（完全可见、仅卖家可见、仅买家可见、隐藏）
- 订单可见性管理（完全可见、仅卖家可见、仅买家可见、隐藏）

### 退货功能
- 申请退货
- 退货审批
- 退货状态跟踪

## 安装和运行

1. 安装依赖
```bash
npm install
```

2. 启动开发服务器
```bash
npm run serve
```

3. 构建生产版本
```bash
npm run build
```

## API配置

项目默认连接后端API地址：`http://localhost:8080/api`

如需修改API地址，请编辑 `src/api/index.js` 文件中的 `baseURL` 配置。

## 主题色配置

项目使用主题色 #6A015E，在 `src/App.vue` 中定义了CSS变量：

```css
:root {
  --primary-color: #6A015E;
  --primary-light: #8B1A7A;
  --primary-dark: #4A003D;
  --text-primary: #6A015E;
  --bg-primary: #6A015E;
}
```

## 浏览器支持

- Chrome >= 87
- Firefox >= 78
- Safari >= 14
- Edge >= 88

## 开发说明

1. 确保后端服务已启动并运行在 `http://localhost:8080`
2. 前端项目运行在 `http://localhost:8080`（默认端口）
3. 所有API请求会自动添加认证token（如果用户已登录）
4. 图片上传功能需要后端支持multipart/form-data格式

## 图片接口更新

项目已更新以适配后端的新图片接口：

### 图片上传
- **商品图片上传**: `/api/user/commodity/upload-image`
- **指定商品图片上传**: `/api/user/commodity/{commodityId}/upload-image`
- **头像上传**: `/api/user/profile/avatar`

### 可见性控制
- **商品可见性**: `PUT /api/user/commodity/{commodityId}/visibility`
- **商品卖家可见性**: `PUT /api/user/commodity/{commodityId}/seller-visibility`
- **商品买家可见性**: `PUT /api/user/commodity/{commodityId}/buyer-visibility`
- **订单可见性**: `PUT /api/user/order/{orderId}/visibility`
- **订单卖家可见性**: `PUT /api/user/order/{orderId}/seller-visibility`
- **订单买家可见性**: `PUT /api/user/order/{orderId}/buyer-visibility`

### 退货功能
- **申请退货**: `POST /api/user/order/{orderId}/return`
- **审批退货**: `PUT /api/user/order/{orderId}/return/approve`
- **确认退货发货**: `PUT /api/user/order/{orderId}/return/shipment`
- **完成退货**: `PUT /api/user/order/{orderId}/return/complete`

### 图片访问
- **头像图片**: `/api/images/avatars/{fileName}`
- **商品图片**: `/api/images/commodities/{fileName}`
- **默认头像**: `/api/images/avatars/default`

### 图片处理工具
项目提供了统一的图片处理工具函数 (`src/utils/imageUtils.js`)：
- `getAvatarUrl()` - 获取头像URL
- `getCommodityImageUrl()` - 获取商品图片URL
- `handleImageError()` - 处理图片加载错误

### 后端返回格式
图片上传接口返回 `ImageUploadDTO` 格式：
```json
{
  "success": true,
  "data": {
    "imageUrl": "http://localhost:8080/api/images/commodities/filename.jpg",
    "fileName": "filename.jpg",
    "fileSize": 102400,
    "contentType": "image/jpeg",
    "uploadTime": 1729593024123
  }
}
```

## 注意事项

- 项目使用Element Plus组件库，确保所有依赖正确安装
- 图片上传功能需要配置正确的后端API地址
- 生产环境部署时需要配置正确的API地址
- 建议使用HTTPS协议部署生产环境
- 图片文件会缓存在浏览器中，缓存时间为1小时