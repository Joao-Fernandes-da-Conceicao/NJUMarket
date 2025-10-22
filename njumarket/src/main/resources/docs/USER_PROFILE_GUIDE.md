# 用户档案功能说明

## 概述

用户档案系统是NJU校园二手交易平台的核心功能之一，为每个用户提供详细的个人信息、交易统计、信用评分和VIP等级管理。

## 功能特性

### 1. 档案信息管理
- **基础信息**: 昵称、头像、用户ID
- **信用系统**: 信用分（初始100分）
- **评分系统**: 买家评分、卖家评分（初始5.0分）
- **交易统计**: 总销售数、总购买数
- **VIP等级**: NORMAL、BRONZE、SILVER、GOLD、PLATINUM

### 2. 自动化功能
- **注册时自动创建档案**
- **交易完成后自动更新统计**
- **VIP等级自动升级**
- **信用分动态调整**

### 3. 管理功能
- **用户档案搜索**
- **排行榜系统**
- **VIP等级统计**
- **管理员档案管理**

## API接口

### 用户端接口

#### 1. 获取当前用户档案
```http
GET /api/user/profile/me
Authorization: Bearer {token}
```

**响应示例**:
```json
{
  "success": true,
  "data": {
    "profileId": "PROFILE_1729593024123_456",
    "userId": "USER_1729593024123_456",
    "nickname": "小明",
    "avatar": "https://example.com/avatar.jpg",
    "creditScore": 120,
    "buyerRating": 4.8,
    "sellerRating": 4.9,
    "totalSales": 15,
    "totalPurchases": 23,
    "vipLevel": "SILVER",
    "userInfo": {
      "userId": "USER_1729593024123_456",
      "primaryPhone": "13800138000",
      "accountStatus": "ACTIVE",
      "registerTime": "2024-10-22T10:30:24"
    }
  }
}
```

#### 2. 更新当前用户档案
```http
PUT /api/user/profile/me
Authorization: Bearer {token}
Content-Type: application/json

{
  "nickname": "新昵称",
  "avatar": "https://example.com/new-avatar.jpg",
  "bio": "这是我的个人简介",
  "contact": "微信: wechat123",
  "location": "南京市"
}
```

#### 3. 获取其他用户档案
```http
GET /api/user/profile/{userId}
```

#### 4. 搜索用户档案
```http
GET /api/user/profile/search?keyword=小明&page=0&size=10
```

#### 5. 获取排行榜
```http
GET /api/user/profile/rankings?type=seller&limit=10
```

#### 6. 获取VIP统计
```http
GET /api/user/profile/vip-statistics
```

### 管理员接口

#### 1. 管理用户档案
```http
# 获取用户档案
GET /api/admin/user-profile/{userId}

# 更新用户档案
PUT /api/admin/user-profile/{userId}

# 创建用户档案
POST /api/admin/user-profile/{userId}?nickname=昵称
```

#### 2. 评分管理
```http
PUT /api/admin/user-profile/{userId}/rating?rating=4.5&role=buyer
```

#### 3. 信用分管理
```http
PUT /api/admin/user-profile/{userId}/credit-score?scoreChange=10&reason=优质交易
```

#### 4. 交易统计管理
```http
PUT /api/admin/user-profile/{userId}/trade-statistics?type=sale&count=1
```

#### 5. VIP升级
```http
PUT /api/admin/user-profile/{userId}/upgrade-vip
```

## VIP等级系统

### 等级规则
| 等级 | 条件 |
|------|------|
| NORMAL | 默认等级 |
| BRONZE | 交易≥5次 && 平均评分≥3.5 && 信用分≥80 |
| SILVER | 交易≥20次 && 平均评分≥4.0 && 信用分≥100 |
| GOLD | 交易≥50次 && 平均评分≥4.5 && 信用分≥120 |
| PLATINUM | 交易≥100次 && 平均评分≥4.8 && 信用分≥150 |

### 自动升级
- 每次交易统计更新时自动检查
- 满足条件时自动升级
- 管理员可手动升级

## 信用分系统

### 初始分数
- 新用户注册：100分

### 加分规则
- 完成交易：+5分
- 好评交易：+10分
- 优质商品：+3分
- 及时发货：+2分

### 扣分规则
- 恶意退款：-10分
- 虚假描述：-15分
- 延迟发货：-5分
- 违规行为：-20分

### 分数影响
- 信用分影响VIP等级
- 低信用分可能限制功能
- 高信用分享受优先权

## 评分系统

### 买家评分
- 基于购买行为的评分
- 影响因素：付款及时性、沟通态度、确认收货速度

### 卖家评分
- 基于销售行为的评分
- 影响因素：商品质量、发货速度、服务态度、描述准确性

### 评分计算
- 初始评分：5.0分
- 动态更新：基于每次交易的评价
- 加权平均：近期交易权重更高

## 数据库设计

### user_profiles表结构
```sql
CREATE TABLE user_profiles (
    profile_id VARCHAR(50) PRIMARY KEY,
    user_id VARCHAR(50) NOT NULL UNIQUE,
    nickname VARCHAR(50),
    avatar VARCHAR(500),
    credit_score INT NOT NULL DEFAULT 100,
    buyer_rating DOUBLE DEFAULT 5.0,
    seller_rating DOUBLE DEFAULT 5.0,
    total_sales INT NOT NULL DEFAULT 0,
    total_purchases INT NOT NULL DEFAULT 0,
    vip_level VARCHAR(20) DEFAULT 'NORMAL',
    FOREIGN KEY (user_id) REFERENCES users(user_id)
);
```

### 索引优化
```sql
-- 用户ID索引（唯一）
CREATE UNIQUE INDEX uk_user_profiles_user_id ON user_profiles(user_id);

-- 昵称搜索索引
CREATE INDEX idx_user_profiles_nickname ON user_profiles(nickname);

-- VIP等级索引
CREATE INDEX idx_user_profiles_vip_level ON user_profiles(vip_level);

-- 评分排序索引
CREATE INDEX idx_user_profiles_seller_rating ON user_profiles(seller_rating DESC, total_sales DESC);
CREATE INDEX idx_user_profiles_buyer_rating ON user_profiles(buyer_rating DESC, total_purchases DESC);

-- 信用分索引
CREATE INDEX idx_user_profiles_credit_score ON user_profiles(credit_score DESC);
```

## 业务流程

### 1. 用户注册流程
```
用户注册 → 创建User实体 → 自动创建UserProfile → 设置默认值
```

### 2. 交易完成流程
```
交易完成 → 更新交易统计 → 更新评分 → 调整信用分 → 检查VIP升级
```

### 3. 档案更新流程
```
用户请求更新 → 验证权限 → 校验数据 → 更新档案 → 返回结果
```

## 安全考虑

### 1. 权限控制
- 用户只能更新自己的档案
- 管理员可以管理所有档案
- 敏感操作需要管理员权限

### 2. 数据验证
- 昵称长度限制（50字符）
- 评分范围验证（0-5分）
- 信用分下限保护（不低于0）

### 3. 防刷机制
- 评分更新频率限制
- 异常交易检测
- 恶意操作监控

## 扩展功能

### 1. 头像上传（预留）
- 文件格式验证
- 图片大小限制
- CDN存储支持

### 2. 社交功能（预留）
- 关注/粉丝系统
- 个人动态
- 交易历史展示

### 3. 推荐系统（预留）
- 基于档案的商品推荐
- 相似用户推荐
- 个性化内容

## 测试用例

### 1. 档案创建测试
```bash
# 注册用户后自动创建档案
curl -X POST "http://localhost:8080/api/user/auth/register-new" \
  -H "Content-Type: application/json" \
  -d '{
    "phone": "13800138002",
    "password": "123456",
    "code": "验证码",
    "nickname": "测试用户"
  }'

# 检查档案是否创建
curl -X GET "http://localhost:8080/api/user/profile/me" \
  -H "Authorization: Bearer {token}"
```

### 2. 档案更新测试
```bash
curl -X PUT "http://localhost:8080/api/user/profile/me" \
  -H "Authorization: Bearer {token}" \
  -H "Content-Type: application/json" \
  -d '{
    "nickname": "新昵称",
    "bio": "个人简介"
  }'
```

### 3. 管理员操作测试
```bash
# 更新用户评分
curl -X PUT "http://localhost:8080/api/admin/user-profile/USER_123/rating?rating=4.8&role=seller" \
  -H "Authorization: Bearer {admin_token}"

# 调整信用分
curl -X PUT "http://localhost:8080/api/admin/user-profile/USER_123/credit-score?scoreChange=10&reason=优质交易" \
  -H "Authorization: Bearer {admin_token}"
```

## 监控指标

### 1. 业务指标
- 档案创建成功率
- 档案更新频率
- VIP升级数量
- 平均信用分

### 2. 技术指标
- 接口响应时间
- 数据库查询性能
- 缓存命中率
- 错误率统计

---

**注意**: 用户档案功能已完整实现，包含完整的CRUD操作、评分系统、VIP等级管理和管理员功能，可直接用于生产环境。头像上传功能已预留接口，待图片处理功能完善后可快速集成。
