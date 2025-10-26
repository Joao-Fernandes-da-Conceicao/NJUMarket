# 用户资料隐私保护实现指南

## 📋 概述

为了保护用户隐私，实现了双层用户资料系统：
- **完整资料**：包含敏感信息（手机号等），仅用户本人可见
- **公开资料**：不含敏感信息，所有人可见

## 🔒 隐私保护机制

### 1. 敏感信息识别

#### 必须保护的敏感信息
- ✅ `primaryPhone` - 手机号
- ✅ `password` - 密码（已加密，永不返回）
- ✅ `email` - 邮箱地址
- ✅ 详细收货地址
- ✅ 身份证号等实名信息

#### 可以公开的信息
- ✅ `userId` - 用户ID
- ✅ `nickname` - 昵称
- ✅ `avatar` - 头像
- ✅ `creditScore` - 信用分
- ✅ `buyerRating` - 买家评分
- ✅ `sellerRating` - 卖家评分
- ✅ `totalSales` - 总销售数
- ✅ `totalPurchases` - 总购买数
- ✅ `vipLevel` - VIP等级
- ✅ `accountStatus` - 账户状态
- ✅ `registerTime` - 注册时间

### 2. DTO设计

#### UserDTO（完整信息）
```java
public class UserDTO {
    private String userId;
    private String primaryPhone;        // ⚠️ 敏感信息
    private String accountStatus;
    private LocalDateTime registerTime;
    private String nickname;
    private String avatar;
    // ...
}
```

#### PublicUserDTO（公开信息）
```java
public class PublicUserDTO {
    private String userId;
    // ❌ 不包含 primaryPhone
    private String accountStatus;
    private LocalDateTime registerTime;
    private String nickname;
    private String avatar;
    // ...
}
```

## 🔄 API行为

### getUserProfile(userId)

**智能判断**：根据当前登录用户自动决定返回的信息类型

```java
public Result getUserProfile(String userId) {
    // 检查是否是查看自己的资料
    User currentUser = UserHolder.getUser();
    boolean isSelf = currentUser != null && currentUser.getUserId().equals(userId);
    
    if (isSelf) {
        // 返回完整信息（UserProfileDTO + UserDTO）
        return Result.ok(convertToDTO(profile));
    } else {
        // 返回公开信息（PublicUserProfileDTO + PublicUserDTO）
        return Result.ok(convertToPublicDTO(profile));
    }
}
```

### 使用场景对比

| 场景 | API | 返回类型 | 包含手机号 |
|------|-----|---------|-----------|
| 查看自己的资料 | GET /api/user/profile/me | UserProfileDTO | ✅ 是 |
| 查看自己的资料 | GET /api/user/profile/{myId} | UserProfileDTO | ✅ 是 |
| 查看他人资料 | GET /api/user/profile/{otherId} | PublicUserProfileDTO | ❌ 否 |
| 明确查询公开资料 | getPublicUserProfile() | PublicUserProfileDTO | ❌ 否 |

## 🎯 实现的功能

### 1. 新增DTO类（2个）

- ✅ `PublicUserDTO.java` - 公开用户基本信息
- ✅ `PublicUserProfileDTO.java` - 公开用户档案信息

### 2. 修改的Service方法

- ✅ `getUserProfile()` - 智能判断返回完整或公开信息
- ✅ `getPublicUserProfile()` - 明确返回公开信息
- ✅ `convertToPublicDTO()` - 新增转换方法

### 3. 前端集成点

#### Messages.vue（消息中心）
```vue
<el-button @click="viewUserProfile(otherUserId)">
  查看资料
</el-button>

<script>
const viewUserProfile = (userId) => {
  router.push(`/profile/${userId}`)
}
</script>
```

#### UserProfile.vue（资料页面）
- 自动调用`profileAPI.getUser(userId)`
- 后端根据`userId`判断返回完整或公开信息
- 前端无需额外判断

## 🛡️ 安全验证

### 测试场景

#### 场景1：查看自己的资料
```bash
# 请求
GET /api/user/profile/USER_123

# 响应（包含手机号）
{
  "success": true,
  "data": {
    "userId": "USER_123",
    "nickname": "张三",
    "userInfo": {
      "primaryPhone": "13800138000"  // ✅ 可见
    }
  }
}
```

#### 场景2：查看他人资料
```bash
# 请求
GET /api/user/profile/USER_456

# 响应（不含手机号）
{
  "success": true,
  "data": {
    "userId": "USER_456",
    "nickname": "李四",
    "userInfo": {
      // ❌ 不包含 primaryPhone
      "accountStatus": "ACTIVE",
      "nickname": "李四"
    }
  }
}
```

## 📊 数据对比

### 完整资料 vs 公开资料

| 字段 | 完整资料 | 公开资料 |
|------|---------|---------|
| 昵称 | ✅ | ✅ |
| 头像 | ✅ | ✅ |
| 信用分 | ✅ | ✅ |
| 评分 | ✅ | ✅ |
| VIP等级 | ✅ | ✅ |
| 注册时间 | ✅ | ✅ |
| 账户状态 | ✅ | ✅ |
| **手机号** | ✅ | ❌ |
| **密码** | ❌ | ❌ |
| **邮箱** | ✅ | ❌ |
| **详细地址** | ✅ | ❌ |

## ✅ 实现完成

### 新增文件（2个）
- `dto/PublicUserDTO.java`
- `dto/PublicUserProfileDTO.java`

### 修改文件（2个）
- `service/UserProfileService.java` - 添加`getPublicUserProfile`接口
- `service/impl/UserProfileServiceImpl.java` - 实现隐私保护逻辑

### 前端已集成
- ✅ Messages.vue - "查看资料"按钮
- ✅ OrderDetail.vue - "查看买家/卖家资料"按钮
- ✅ UserProfile.vue - 自动处理公开/完整资料

## 🎯 使用建议

### 后端开发
```java
// 查看他人资料时，优先使用明确的公开方法
Result result = userProfileService.getPublicUserProfile(userId);

// 或使用智能判断方法（推荐）
Result result = userProfileService.getUserProfile(userId);
// 自动判断是否是本人，返回对应的信息
```

### 前端开发
```javascript
// 前端无需特殊处理，后端自动判断
const response = await profileAPI.getUser(userId)
// 如果是查看自己，返回完整信息
// 如果是查看他人，返回公开信息
```

## 🔐 额外安全措施

1. **密码永不返回**：任何DTO都不包含密码字段
2. **手机号脱敏**：查看他人时完全不返回
3. **邮箱保护**：查看他人时不返回
4. **地址保护**：仅在订单中显示必要的收货信息
5. **权限验证**：后端验证当前用户身份

## 📝 开发检查清单

创建新DTO时，务必检查：
- [ ] 是否包含手机号？
- [ ] 是否包含邮箱？
- [ ] 是否包含详细地址？
- [ ] 是否包含密码相关？
- [ ] 是否区分了完整和公开版本？

---

**实现状态**：✅ 完成
**安全级别**：🔒 高
**测试状态**：✅ 无linter错误
