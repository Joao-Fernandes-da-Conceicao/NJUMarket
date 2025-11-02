# 架构映射关系文档

本文档详细描述了 NJUMarket v1.0 项目从前端 API 调用到数据库的完整映射关系。

## 📋 目录
- [映射说明](#映射说明)
- [用户认证模块](#用户认证模块)
- [用户资料模块](#用户资料模块)
- [商品模块](#商品模块)
- [订单模块](#订单模块)
- [消息模块](#消息模块)
- [图片模块](#图片模块)
- [管理端模块](#管理端模块)
- [数据库表映射](#数据库表映射)

---

## 映射说明

### 架构层次
```
前端 API (Vue) 
  ↓
Controller (RESTful API)
  ↓
Service (业务逻辑层)
  ↓
Repository (数据访问层)
  ↓
Entity (实体类)
  ↓
数据库表 (MySQL)
```

### 命名规范
- **前端 API**: `src/api/index.js` 中的 API 函数
- **Controller**: `controller/` 目录下的控制器类
- **Service**: `service/` 目录下的服务接口和实现
- **Repository**: `repository/` 目录下的 JPA Repository
- **Entity**: `entity/` 目录下的实体类
- **数据库表**: MySQL 中的实际表名

---

## 用户认证模块

### 前端 API → Controller → Service → Repository → Entity → 数据库

| 前端 API | Controller | Service | Repository | Entity | 数据库表 |
|---------|-----------|---------|-----------|--------|---------|
| `authAPI.login` | `UserAuthController.login` | `UserService.login` | `UserRepository` | `User` | `users` |
| `authAPI.register` | `UserAuthController.registerNew` | `UserService.registerNew` | `UserRepository` | `User` | `users` |
| `authAPI.sendCode` | `UserAuthController.sendCode` | `UserService.sendCode` | - | - | - (模拟验证码) |
| `authAPI.loginByCode` | `UserAuthController.loginByCode` | `UserService.loginByCode` | `UserRepository` | `User` | `users` |
| `authAPI.logout` | `UserAuthController.logout` | `UserService.logout` | - | - | - (Session 清除) |
| `authAPI.resetPassword` | `UserAuthController.resetPassword` | `PasswordService.resetPassword` | `UserRepository` | `User` | `users` |
| `authAPI.getCurrentUser` | `UserAuthController.getCurrentUser` | `UserService.getCurrentUser` | `UserRepository` | `User` | `users` |

**说明**:
- `UserService` 使用 `UserRepository` 操作 `User` 实体
- 验证码功能为模拟实现，不涉及真实短信服务
- Session 存储在应用服务器内存中

---

## 用户资料模块

### 前端 API → Controller → Service → Repository → Entity → 数据库

| 前端 API | Controller | Service | Repository | Entity | 数据库表 |
|---------|-----------|---------|-----------|--------|---------|
| `profileAPI.getMe` | `UserProfileController.getCurrentUserProfile` | `UserProfileService.getCurrentUserProfile` | `UserProfileRepository`, `UserRepository` | `UserProfile`, `User` | `user_profiles`, `users` |
| `profileAPI.getUser` | `UserProfileController.getUserProfile` | `UserProfileService.getUserProfile` | `UserProfileRepository`, `UserRepository` | `UserProfile`, `User` | `user_profiles`, `users` |
| `profileAPI.update` | `UserProfileController.updateProfile` | `UserProfileService.updateProfile` | `UserProfileRepository` | `UserProfile` | `user_profiles` |
| `profileAPI.uploadAvatar` | `UserProfileController.uploadAvatar` | `ImageService.uploadAvatar` | `UserProfileRepository`, `ImageReferenceRepository` | `UserProfile`, `ImageReference` | `user_profiles`, `image_references` |
| `profileAPI.search` | `UserProfileController.searchUsers` | `UserProfileService.searchUsers` | `UserProfileRepository`, `UserRepository` | `UserProfile`, `User` | `user_profiles`, `users` |
| `profileAPI.getRankings` | `UserProfileController.getRankings` | `UserProfileService.getRankings` | `UserProfileRepository` | `UserProfile` | `user_profiles` |

**说明**:
- `UserProfile` 与 `User` 为一对一关系
- 头像上传涉及 `ImageReference` 引用计数管理
- 搜索功能使用 JPA Specification 进行动态查询

---

## 商品模块

### 前端 API → Controller → Service → Repository → Entity → 数据库

#### 公共接口（无需登录）

| 前端 API | Controller | Service | Repository | Entity | 数据库表 |
|---------|-----------|---------|-----------|--------|---------|
| `commodityAPI.search` | `PublicController.searchCommodities` | `CommodityQueryService.searchCommodities` | `CommodityRepository` | `Commodity` | `commodities` |
| `commodityAPI.getDetail` | `PublicController.getCommodityDetail` | `CommodityQueryService.getCommodityDetail` | `CommodityRepository` | `Commodity` | `commodities` |
| `commodityAPI.getHot` | `PublicController.getHotCommodities` | `CommodityQueryService.getHotCommodities` | `CommodityRepository` | `Commodity` | `commodities` |
| `commodityAPI.getLatest` | `PublicController.getLatestCommodities` | `CommodityQueryService.getLatestCommodities` | `CommodityRepository` | `Commodity` | `commodities` |
| `commodityAPI.getCategories` | `PublicController.getCategories` | `CommodityQueryService.getCategories` | `CommodityRepository` | `Commodity` | `commodities` |
| `commodityAPI.getByCategory` | `PublicController.getCommoditiesByCategory` | `CommodityQueryService.getCommoditiesByCategory` | `CommodityRepository` | `Commodity` | `commodities` |
| `commodityAPI.recordView` | `PublicController.recordView` | `CommodityQueryService.recordView` | `CommodityRepository` | `Commodity` | `commodities` |
| `commodityAPI.getSellerCommodities` | `PublicController.getSellerCommodities` | `CommodityQueryService.getUserCommodities` | `CommodityRepository` | `Commodity` | `commodities` |

#### 用户商品管理（需登录）

| 前端 API | Controller | Service | Repository | Entity | 数据库表 |
|---------|-----------|---------|-----------|--------|---------|
| `commodityAPI.publish` | `UserCommodityController.publishCommodity` | `CommodityService.publishCommodity` | `CommodityRepository`, `ImageReferenceRepository` | `Commodity`, `ImageReference` | `commodities`, `image_references` |
| `commodityAPI.createDraft` | `UserCommodityController.createDraft` | `CommodityService.createDraft` | `CommodityRepository` | `Commodity` | `commodities` |
| `commodityAPI.getMy` | `UserCommodityController.getMyCommodities` | `CommodityService.getMyCommodities` | `CommodityRepository` | `Commodity` | `commodities` |
| `commodityAPI.getMyDetail` | `UserCommodityController.getCommodityDetail` | `CommodityService.getCommodityDetail` | `CommodityRepository` | `Commodity` | `commodities` |
| `commodityAPI.update` | `UserCommodityController.updateCommodity` | `CommodityService.updateCommodity` | `CommodityRepository` | `Commodity` | `commodities` |
| `commodityAPI.shelf` | `UserCommodityController.shelfCommodity` | `CommodityService.shelfCommodity` | `CommodityRepository` | `Commodity` | `commodities` |
| `commodityAPI.unshelf` | `UserCommodityController.unshelfCommodity` | `CommodityService.unshelfCommodity` | `CommodityRepository` | `Commodity` | `commodities` |
| `commodityAPI.delete` | `UserCommodityController.deleteCommodity` | `CommodityService.deleteCommodity` | `CommodityRepository`, `ImageReferenceRepository` | `Commodity`, `ImageReference` | `commodities`, `image_references` |
| `commodityAPI.uploadImage` | `ImageController.uploadCommodityImage` | `ImageService.uploadCommodityImage` | `ImageReferenceRepository` | `ImageReference` | `image_references` |
| `commodityAPI.updateVisibility` | `UserCommodityController.updateVisibility` | `CommodityService.updateVisibility` | `CommodityRepository` | `Commodity` | `commodities` |

**说明**:
- `CommodityQueryService` 负责所有查询功能（公共和用户）
- `CommodityService` 负责商品管理功能（CRUD、上下架）
- `ImageReference` 用于管理图片引用计数，支持软删除

---

## 订单模块

### 前端 API → Controller → Service → Repository → Entity → 数据库

| 前端 API | Controller | Service | Repository | Entity | 数据库表 |
|---------|-----------|---------|-----------|--------|---------|
| `orderAPI.create` | `UserOrderController.createOrder` | `OrderService.createOrder` | `OrderRepository`, `CommodityRepository`, `CommoditySnapshotRepository` | `Order`, `Commodity`, `CommoditySnapshot` | `orders`, `commodities`, `commodity_snapshots` |
| `orderAPI.pay` | `UserOrderController.payOrder` | `OrderService.payOrder` | `OrderRepository` | `Order` | `orders` |
| `orderAPI.ship` | `UserOrderController.shipOrder` | `OrderService.shipOrder` | `OrderRepository` | `Order` | `orders` |
| `orderAPI.confirm` | `UserOrderController.confirmOrder` | `OrderService.confirmOrder` | `OrderRepository` | `Order` | `orders` |
| `orderAPI.cancel` | `UserOrderController.cancelOrder` | `OrderService.cancelOrder` | `OrderRepository` | `Order` | `orders` |
| `orderAPI.getBuyerOrders` | `UserOrderController.getBuyerOrders` | `OrderService.getBuyerOrders` | `OrderRepository` | `Order` | `orders` |
| `orderAPI.getSellerOrders` | `UserOrderController.getSellerOrders` | `OrderService.getSellerOrders` | `OrderRepository` | `Order` | `orders` |
| `orderAPI.getDetail` | `UserOrderController.getOrderDetail` | `OrderService.getOrderDetail` | `OrderRepository` | `Order` | `orders` |
| `orderAPI.requestRefund` | `UserOrderController.requestRefund` | `OrderService.requestRefund` | `OrderRepository` | `Order` | `orders` |
| `orderAPI.handleRefund` | `UserOrderController.handleRefund` | `OrderService.handleRefund` | `OrderRepository` | `Order` | `orders` |
| `orderAPI.requestReturn` | `UserOrderController.requestReturn` | `OrderService.requestReturn` | `OrderRepository` | `Order` | `orders` |
| `orderAPI.approveReturn` | `UserOrderController.approveReturn` | `OrderService.approveReturn` | `OrderRepository` | `Order` | `orders` |
| `orderAPI.completeReturn` | `UserOrderController.completeReturn` | `OrderService.completeReturn` | `OrderRepository` | `Order` | `orders` |
| `orderAPI.updateVisibility` | `UserOrderController.updateVisibility` | `OrderService.updateVisibility` | `OrderRepository` | `Order` | `orders` |

**说明**:
- 订单创建时自动生成 `CommoditySnapshot` 快照
- 订单状态流转：CREATED → PAID → SHIPPED → COMPLETED
- 退款/退货状态：REFUND_REQUESTED/RETURN_REQUESTED → APPROVED/REJECTED → COMPLETED
- 订单可见性控制：卖家/买家分别可见

---

## 消息模块

### 前端 API → Controller → Service → Repository → Entity → 数据库

| 前端 API | Controller | Service | Repository | Entity | 数据库表 |
|---------|-----------|---------|-----------|--------|---------|
| `contactAPI.getConversations` | `ContactController.getConversations` | `ContactService.getConversations` | `ConversationRepository`, `UserRepository` | `Conversation`, `User` | `conversations`, `users` |
| `contactAPI.getConversationDetail` | `ContactController.getConversationDetail` | `ContactService.getConversationDetail` | `ConversationRepository`, `MessageRepository` | `Conversation`, `Message` | `conversations`, `messages` |
| `contactAPI.sendMessage` | `ContactController.sendMessage` | `ContactService.sendMessage` | `MessageRepository`, `ConversationRepository` | `Message`, `Conversation` | `messages`, `conversations` |
| `contactAPI.createConversation` | `ContactController.createConversation` | `ContactService.createOrGetConversation` | `ConversationRepository`, `UserRepository` | `Conversation`, `User` | `conversations`, `users` |
| `contactAPI.markAsRead` | `ContactController.markAsRead` | `ContactService.markAsRead` | `ConversationRepository`, `MessageRepository` | `Conversation`, `Message` | `conversations`, `messages` |
| `contactAPI.getUnreadCount` | `ContactController.getUnreadCount` | `ContactService.getUnreadCount` | `ConversationRepository` | `Conversation` | `conversations` |
| `contactAPI.deleteConversation` | `ContactController.deleteConversation` | `ContactService.deleteConversation` | `ConversationRepository` | `Conversation` | `conversations` |
| `contactAPI.deleteMessage` | `ContactController.deleteMessage` | `ContactService.deleteMessage` | `MessageRepository` | `Message` | `messages` |
| `contactAPI.searchMessages` | `ContactController.searchMessages` | `ContactService.searchMessages` | `MessageRepository` | `Message` | `messages` |
| `contactAPI.getConversationWithUser` | `ContactController.getConversationWithUser` | `ContactService.getConversationWithUser` | `ConversationRepository` | `Conversation` | `conversations` |

**说明**:
- 对话采用"一个用户对只允许一个会话"的规则
- 消息支持商品卡片（`commodityId`）和订单卡片（`orderId`）
- 未读数使用 `user_1_count` 和 `user_2_count` 字段（根据用户ID大小关系确定）

---

## 图片模块

### 前端 API → Controller → Service → Repository → Entity → 数据库

| 前端 API | Controller | Service | Repository | Entity | 数据库表 |
|---------|-----------|---------|-----------|--------|---------|
| `imageAPI.upload` | `ImageController.uploadCommodityImage` | `ImageService.uploadCommodityImage` | `ImageReferenceRepository` | `ImageReference` | `image_references` |
| `imageAPI.getAvatar` | `ImageController.getAvatar` | `ImageService.getAvatar` | `ImageReferenceRepository` | `ImageReference` | `image_references` |
| `imageAPI.getCommodityImage` | `ImageController.getCommodityImage` | `ImageService.getCommodityImage` | `ImageReferenceRepository` | `ImageReference` | `image_references` |
| `profileAPI.uploadAvatar` | `UserProfileController.uploadAvatar` | `ImageService.uploadAvatar` | `ImageReferenceRepository`, `UserProfileRepository` | `ImageReference`, `UserProfile` | `image_references`, `user_profiles` |

**说明**:
- 图片通过 `ImageReference` 管理引用计数
- 支持软删除，引用计数为 0 时物理删除
- 头像和商品图片统一管理

---

## 管理端模块

### 前端 API → Controller → Service → Repository → Entity → 数据库

| 前端 API | Controller | Service | Repository | Entity | 数据库表 |
|---------|-----------|---------|-----------|--------|---------|
| `adminAPI.login` | `AdminController.login` | `AdminService.login` | `AdminRepository` | `Admin` | `admins` |
| `adminAPI.getStatistics` | `AdminController.getStatistics` | `AdminService.getStatistics` | `UserRepository`, `CommodityRepository`, `OrderRepository` | `User`, `Commodity`, `Order` | `users`, `commodities`, `orders` |
| `adminAPI.getUsers` | `AdminController.getUsers` | `AdminService.getUsers` | `UserRepository`, `UserProfileRepository` | `User`, `UserProfile` | `users`, `user_profiles` |
| `adminAPI.getUser` | `AdminController.getUser` | `AdminService.getUser` | `UserRepository`, `UserProfileRepository` | `User`, `UserProfile` | `users`, `user_profiles` |
| `adminAPI.updateUser` | `AdminController.updateUser` | `AdminService.updateUser` | `UserRepository`, `UserProfileRepository` | `User`, `UserProfile` | `users`, `user_profiles` |
| `adminAPI.deleteUser` | `AdminController.deleteUser` | `AdminService.deleteUser` | `UserRepository` | `User` | `users` |
| `adminAPI.getCommodities` | `AdminController.getCommodities` | `AdminService.getCommodities` | `CommodityRepository`, `UserRepository`, `UserProfileRepository` | `Commodity`, `User`, `UserProfile` | `commodities`, `users`, `user_profiles` |
| `adminAPI.getCommodity` | `AdminController.getCommodity` | `AdminService.getCommodity` | `CommodityRepository` | `Commodity` | `commodities` |
| `adminAPI.updateCommodity` | `AdminController.updateCommodity` | `AdminService.updateCommodity` | `CommodityRepository` | `Commodity` | `commodities` |
| `adminAPI.deleteCommodity` | `AdminController.deleteCommodity` | `AdminService.deleteCommodity` | `CommodityRepository`, `ImageReferenceRepository` | `Commodity`, `ImageReference` | `commodities`, `image_references` |
| `adminAPI.getOrders` | `AdminController.getOrders` | `AdminService.getOrders` | `OrderRepository`, `UserRepository` | `Order`, `User` | `orders`, `users` |
| `adminAPI.getOrder` | `AdminController.getOrder` | `AdminService.getOrder` | `OrderRepository` | `Order` | `orders` |
| `adminAPI.updateOrder` | `AdminController.updateOrder` | `AdminService.updateOrder` | `OrderRepository` | `Order` | `orders` |
| `adminAPI.deleteOrder` | `AdminController.deleteOrder` | `AdminService.deleteOrder` | `OrderRepository` | `Order` | `orders` |

**说明**:
- 管理端使用 JWT Token 认证
- 支持复杂的搜索、筛选、排序功能（使用 JPA Specification）
- 用户管理包含 `UserProfile` 的联表查询

---

## 数据库表映射

### 核心实体与数据库表

| Entity | 数据库表 | 主要 Repository | 说明 |
|--------|---------|----------------|------|
| `User` | `users` | `UserRepository` | 用户基础信息 |
| `UserProfile` | `user_profiles` | `UserProfileRepository` | 用户详细资料（一对一） |
| `Commodity` | `commodities` | `CommodityRepository` | 商品信息 |
| `CommoditySnapshot` | `commodity_snapshots` | `CommoditySnapshotRepository` | 商品快照（订单用） |
| `Order` | `orders` | `OrderRepository` | 订单信息 |
| `OrderSnapshot` | `order_snapshots` | `OrderSnapshotRepository` | 订单快照（历史记录） |
| `Conversation` | `conversations` | `ConversationRepository` | 会话信息 |
| `Message` | `messages` | `MessageRepository` | 消息内容 |
| `ImageReference` | `image_references` | `ImageReferenceRepository` | 图片引用管理 |
| `Admin` | `admins` | `AdminRepository` | 管理员信息 |
| `Complaint` | `complaints` | `ComplaintRepository` | 投诉记录 |

### 关系说明

- **User ↔ UserProfile**: 一对一关系，`User.userId = UserProfile.userId`
- **User ↔ Commodity**: 一对多关系，`Commodity.sellerId = User.userId`
- **Commodity ↔ CommoditySnapshot**: 一对多关系，快照保存订单时的商品信息
- **User ↔ Order**: 一对多关系（买家/卖家），`Order.buyerId` 和 `Order.sellerId`
- **Order ↔ OrderSnapshot**: 一对一关系，订单历史快照
- **Conversation ↔ Message**: 一对多关系，`Message.conversationId = Conversation.conversationId`
- **ImageReference**: 独立的引用计数表，管理图片生命周期

---

## 数据流向示例

### 示例 1: 用户登录
```
前端: authAPI.login(loginForm)
  ↓
Controller: UserAuthController.login()
  ↓
Service: UserService.login()
  ↓
Repository: UserRepository.findByPhone()
  ↓
Entity: User (查询)
  ↓
数据库: SELECT * FROM users WHERE phone = ?
```

### 示例 2: 创建订单
```
前端: orderAPI.create(orderData)
  ↓
Controller: UserOrderController.createOrder()
  ↓
Service: OrderService.createOrder()
  ↓
Repository: 
  - CommodityRepository.findById() (查询商品)
  - CommoditySnapshotRepository.save() (保存快照)
  - OrderRepository.save() (保存订单)
  ↓
Entity: 
  - Commodity (查询)
  - CommoditySnapshot (新建)
  - Order (新建)
  ↓
数据库: 
  - SELECT * FROM commodities WHERE commodity_id = ?
  - INSERT INTO commodity_snapshots ...
  - INSERT INTO orders ...
```

### 示例 3: 发送消息
```
前端: contactAPI.sendMessage(messageData)
  ↓
Controller: ContactController.sendMessage()
  ↓
Service: ContactService.sendMessage()
  ↓
Repository:
  - ConversationRepository.findByUser1IdAndUser2Id() (查询/创建会话)
  - MessageRepository.save() (保存消息)
  ↓
Entity:
  - Conversation (查询/更新)
  - Message (新建)
  ↓
数据库:
  - SELECT * FROM conversations WHERE user_1_id = ? AND user_2_id = ?
  - INSERT INTO messages ...
  - UPDATE conversations SET last_message_content = ?, user_1_count = ? ...
```

---

## 特殊说明

### 1. 查询服务分离
- **CommodityQueryService**: 专门处理商品查询逻辑（公共接口和用户查询）
- **CommodityService**: 专门处理商品管理逻辑（CRUD、上下架）

### 2. 快照机制
- **CommoditySnapshot**: 订单创建时保存商品快照，防止商品变更影响订单
- **OrderSnapshot**: 订单完成时保存订单快照，用于历史记录

### 3. 图片引用管理
- **ImageReference**: 管理图片引用计数，支持多商品共享同一图片
- 引用计数为 0 时自动物理删除图片文件

### 4. 可见性控制
- **商品可见性**: `sellerVisibility`, `buyerVisibility` 字段控制
- **订单可见性**: `sellerVisibility`, `buyerVisibility` 字段控制

### 5. 会话机制
- **一对一会话**: 一个用户对只允许一个会话（`user_1_id`, `user_2_id` 按大小排序）
- **未读数**: `user_1_count` 和 `user_2_count` 根据用户ID大小关系确定

---

**文档版本**: v1.0  
**最后更新**: 2025-01-27  
**维护者**: NJUMarket 开发团队
