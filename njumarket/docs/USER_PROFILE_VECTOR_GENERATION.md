# 用户画像向量生成功能

## 功能概述

用户画像向量生成功能整合了多个数据源，为每个用户生成个性化的向量表示，用于商品推荐和相似用户搜索。

## 数据源整合

用户画像生成会整合以下数据源：

1. **用户基本信息**：用户名、昵称、VIP等级、信用分、评分等
2. **用户地址**：默认地址或常用地址（省市区+详细地址）
3. **发布的商品**：用户发布的商品标题、分类、价格等信息（最多50个）
4. **订单信息**：作为买家和卖家的订单数量统计
5. **AI聊天记录**：与AI助手的对话记录（最多30条）
6. **用户聊天记录**：与其他用户的聊天记录（最多30条）

## 单次 curl 生成画像

### 接口说明

**接口地址**：`POST /api/internal/user/{userId}/generate-profile-vector`

**说明**：单次 curl 即可触发用户画像向量生成，异步处理，整合所有数据源。

### 使用示例

```bash
# 生成指定用户的画像向量
curl -X POST "http://localhost:8091/api/internal/user/USER_123/generate-profile-vector"
```

### 响应示例

```json
{
  "success": true,
  "errorMsg": null,
  "data": null,
  "total": null,
  "message": "用户画像向量生成成功（异步处理中）"
}
```

## 查询用户画像向量

### 接口说明

**接口地址**：`GET /api/internal/user/{userId}/profile-vector`

**说明**：查询已生成的用户画像向量。

### 使用示例

```bash
# 查询用户画像向量
curl "http://localhost:8091/api/internal/user/USER_123/profile-vector"
```

### 响应示例

```json
{
  "success": true,
  "errorMsg": null,
  "data": [0.123, 0.456, 0.789, ...],
  "total": null,
  "message": "查询成功"
}
```

## 空值处理

系统对空值进行了完善的容错处理：

- 如果用户没有任何商品，会跳过商品信息
- 如果用户没有任何订单，会跳过订单信息
- 如果用户没有地址，会跳过地址信息
- 如果用户没有聊天记录，会跳过聊天记录
- 即使所有数据都为空，也会生成基于用户ID的基本画像

## 实现细节

### 数据源获取

1. **商品数据**：通过 `CommodityClient.getUserCommodities()` 获取
2. **订单数据**：通过 `OrderClient.getUserOrders()` 获取（分别查询买家和卖家角色）
3. **地址数据**：直接从 `UserAddressRepository` 查询
4. **AI聊天记录**：通过 `CommodityClient.getAIChatHistory()` 获取
5. **用户聊天记录**：通过 `MessageClient.getUserChatHistory()` 获取

### 文本构建

所有数据源会被整合成一个文本描述，然后通过 LangChain4j 的 `EmbeddingModel` 生成向量。

文本格式示例：
```
用户名：testuser。昵称：测试用户。VIP等级：NORMAL。信用分：100。买家评分：5.0。卖家评分：5.0。
常用地址：江苏省南京市鼓楼区南京大学。
发布的商品：二手笔记本电脑（电子产品），价格2000元；二手自行车（交通工具），价格300元；共5个商品。
订单情况：作为买家3单，作为卖家2单。
AI聊天偏好：想要买一台笔记本电脑；价格不要太贵；共10条聊天记录。
聊天偏好：你好，这个商品还在吗？；价格可以便宜点吗？；共20条聊天记录。
```

### 向量存储

生成的向量存储在 `nju_market.user_profile_vectors` 表中，包含：
- `user_id`：用户ID（主键）
- `embedding`：向量数据（pgvector类型，2000维）
- `content`：原始文本内容
- `metadata`：元数据（JSON格式，包含VIP等级、信用分等）
- `created_at`：创建时间
- `updated_at`：更新时间

## 注意事项

1. **异步处理**：画像生成是异步的，调用接口后立即返回，实际生成在后台进行
2. **数据更新**：当用户数据发生变化时，需要重新生成画像向量
3. **性能考虑**：生成过程会调用多个服务，可能需要一定时间
4. **容错机制**：即使某些数据源获取失败，也会继续生成画像（使用已获取的数据）

## 相关接口

### Commodity Service 内部接口

- `GET /api/internal/commodity/seller/{sellerId}`：获取用户发布的商品
- `GET /api/internal/commodity/ai-chat-history/{userId}`：获取AI聊天记录

### Order Service 内部接口

- `GET /api/internal/order/user/{userId}?role=buyer`：获取买家订单
- `GET /api/internal/order/user/{userId}?role=seller`：获取卖家订单

### Message Service 内部接口

- `GET /api/internal/message/user-chat-history/{userId}`：获取用户聊天记录

## conversation_vectors 完整利用

### 实现说明

`conversation_vectors` 表存储了用户与 AI 助手的对话历史，包括：
- `conversation_id`：对话ID
- `message_id`：消息ID
- `user_id`：用户ID
- `content`：消息内容
- `role`：消息角色（user/assistant）
- `embedding`：消息向量
- `metadata`：元数据（JSON格式）
- `created_at`：创建时间

### 获取AI聊天记录

在用户画像生成时，系统会：
1. 从 `conversation_vectors` 表中查询用户的所有AI聊天记录
2. 按时间倒序排列（最新的在前）
3. 提取用户消息（role = "user"）用于画像生成
4. 最多提取5条用户消息，每条消息最多50个字符

### 内部接口

**接口地址**：`GET /api/internal/commodity/ai-chat-history/{userId}`

**参数**：
- `userId`：用户ID（路径参数）
- `limit`：返回数量限制（查询参数，默认50）

**响应示例**：
```json
{
  "success": true,
  "errorMsg": null,
  "data": [
    {
      "conversationId": "conv_123",
      "messageId": "msg_456",
      "content": "我想要买一台笔记本电脑",
      "role": "user"
    },
    {
      "conversationId": "conv_123",
      "messageId": "msg_457",
      "content": "价格不要太贵",
      "role": "user"
    }
  ],
  "total": null,
  "message": "查询成功"
}
```

## 全量生成用户画像（curl 人工操控）

由于本项目以学习为主，不提供自动批量生成功能，而是通过 curl 命令人工操控。

### 单用户生成

```bash
# 生成指定用户的画像向量
curl -X POST "http://localhost:8091/api/internal/user/USER_123/generate-profile-vector"
```

### 批量生成（使用脚本）

如果需要为多个用户生成画像，可以使用以下脚本：

**Windows (PowerShell)**：
```powershell
# 用户ID列表
$userIds = @("USER_001", "USER_002", "USER_003")

foreach ($userId in $userIds) {
    Write-Host "生成用户画像: $userId"
    curl -X POST "http://localhost:8091/api/internal/user/$userId/generate-profile-vector"
    Start-Sleep -Seconds 2  # 避免请求过快
}
```

**Linux/Mac (Bash)**：
```bash
#!/bin/bash
# 用户ID列表
userIds=("USER_001" "USER_002" "USER_003")

for userId in "${userIds[@]}"; do
    echo "生成用户画像: $userId"
    curl -X POST "http://localhost:8091/api/internal/user/$userId/generate-profile-vector"
    sleep 2  # 避免请求过快
done
```

### 从数据库获取所有用户ID

如果需要为所有用户生成画像，可以先从数据库获取用户ID列表：

```sql
-- 获取所有用户ID
SELECT user_id FROM nju_market.users WHERE account_status = 'ACTIVE';
```

然后使用脚本批量生成。

## 后续优化

1. **增量更新**：支持增量更新，只更新变化的数据源
2. **缓存机制**：缓存生成的画像向量，减少重复计算
3. **数据源扩展**：可以添加更多数据源，如浏览历史、收藏记录等
4. **批量生成接口**：如果需要，可以添加批量生成接口（接受用户ID列表）

