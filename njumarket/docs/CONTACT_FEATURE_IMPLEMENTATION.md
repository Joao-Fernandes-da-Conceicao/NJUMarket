# 卖家与买家联系功能实现文档

## 项目概述
本文档描述了NJU Market项目中卖家与买家之间联系功能的完整实现方案。

## 一、已完成的数据库和实体层

### 1. 数据库表结构
- ✅ `conversations` 表：对话表
- ✅ `messages` 表：消息表  
- ✅ `contact_blacklist` 表：黑名单表
- ✅ `message_notification_settings` 表：通知设置表

### 2. 实体类
- ✅ `Conversation.java`：对话实体
- ✅ `Message.java`：消息实体

### 3. Repository层
- ✅ `ConversationRepository.java`：对话数据访问
- ✅ `MessageRepository.java`：消息数据访问

### 4. DTO类
- ✅ `ConversationDTO.java`：对话数据传输对象
- ✅ `MessageDTO.java`：消息数据传输对象
- ✅ `SendMessageRequest.java`：发送消息请求对象

## 二、待实现的后端功能

### 1. Service实现类

需要创建 `ContactServiceImpl.java`，实现以下核心功能：

```java
package com.njumarket.njumarket.service.impl;

@Service
@Transactional
public class ContactServiceImpl implements ContactService {
    
    @Autowired
    private ConversationRepository conversationRepository;
    
    @Autowired
    private MessageRepository messageRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private UserProfileRepository userProfileRepository;
    
    @Autowired
    private CommodityRepository commodityRepository;
    
    // 主要方法：
    // 1. sendMessage() - 发送消息
    // 2. getConversations() - 获取对话列表
    // 3. getConversationDetail() - 获取对话详情
    // 4. getOrCreateConversation() - 获取或创建对话
    // 5. markConversationAsRead() - 标记已读
    // 6. getUnreadCount() - 获取未读数
}
```

### 2. Controller层

需要创建 `ContactController.java`：

```java
package com.njumarket.njumarket.controller;

@RestController
@RequestMapping("/api/contact")
public class ContactController {
    
    @Autowired
    private ContactService contactService;
    
    // POST /api/contact/send - 发送消息
    // GET /api/contact/conversations - 获取对话列表
    // GET /api/contact/conversations/{id} - 获取对话详情
    // POST /api/contact/conversations/create - 创建对话
    // POST /api/contact/conversations/{id}/read - 标记已读
    // GET /api/contact/unread-count - 获取未读数
    // DELETE /api/contact/conversations/{id} - 删除对话
}
```

## 三、前端实现方案

### 1. API接口层

创建 `njumarket-front/my-vue3-app/src/api/contact.js`：

```javascript
import request from './index'

export const contactAPI = {
  // 发送消息
  sendMessage(data) {
    return request.post('/api/contact/send', data)
  },
  
  // 获取对话列表
  getConversations(page = 1, size = 20) {
    return request.get('/api/contact/conversations', {
      params: { page, size }
    })
  },
  
  // 获取对话详情
  getConversationDetail(conversationId, page = 1, size = 50) {
    return request.get(`/api/contact/conversations/${conversationId}`, {
      params: { page, size }
    })
  },
  
  // 创建或获取对话
  createConversation(data) {
    return request.post('/api/contact/conversations/create', data)
  },
  
  // 标记对话为已读
  markAsRead(conversationId) {
    return request.post(`/api/contact/conversations/${conversationId}/read`)
  },
  
  // 获取未读消息数
  getUnreadCount() {
    return request.get('/api/contact/unread-count')
  }
}
```

### 2. 消息中心页面

创建 `njumarket-front/my-vue3-app/src/views/Messages.vue`：

主要功能：
- 左侧：对话列表，显示联系人头像、昵称、最后消息、未读数
- 右侧：消息窗口，显示聊天记录，发送消息框

### 3. 对话窗口组件

创建 `njumarket-front/my-vue3-app/src/components/chat/ChatWindow.vue`：

功能：
- 消息列表（支持滚动加载更多）
- 消息气泡（区分自己和对方）
- 消息输入框
- 发送按钮

### 4. 路由配置

在 `router/index.js` 中添加：

```javascript
{
  path: '/messages',
  name: 'Messages',
  component: () => import('@/views/Messages.vue'),
  meta: { requiresAuth: true }
}
```

### 5. 导航栏集成

在 `AppHeader.vue` 中添加消息图标：

```vue
<el-badge :value="unreadCount" :hidden="unreadCount === 0">
  <el-icon @click="$router.push('/messages')">
    <ChatDotRound />
  </el-icon>
</el-badge>
```

## 四、集成方案

### 1. 商品详情页集成

在商品详情页添加"联系卖家"按钮：

```vue
<el-button 
  type="primary" 
  @click="contactSeller"
  :disabled="isOwnCommodity">
  联系卖家
</el-button>

<script>
const contactSeller = async () => {
  try {
    const response = await contactAPI.createConversation({
      receiverId: commodity.sellerId,
      commodityId: commodity.commodityId
    })
    if (response.success) {
      router.push({
        path: '/messages',
        query: { conversationId: response.data.conversationId }
      })
    }
  } catch (error) {
    ElMessage.error('创建对话失败')
  }
}
</script>
```

### 2. 订单页面集成

在订单详情页添加"联系买家/卖家"按钮：

```vue
<el-button 
  type="primary" 
  @click="contactUser">
  {{ isBuyer ? '联系卖家' : '联系买家' }}
</el-button>
```

## 五、安全和优化考虑

### 1. 权限验证
- 用户只能查看自己参与的对话
- 用户只能发送消息给自己有业务往来的用户

### 2. 性能优化
- 消息列表分页加载
- 使用Redis缓存未读消息数
- WebSocket实现实时消息推送（可选）

### 3. 用户体验
- 消息发送失败重试机制
- 消息状态（发送中、已发送、已读）
- 输入状态提示

## 六、实施步骤

1. ✅ 执行数据库脚本创建表
2. ⏳ 实现Service层业务逻辑
3. ⏳ 实现Controller层接口
4. ⏳ 测试后端API
5. ⏳ 实现前端API调用层
6. ⏳ 实现消息中心页面
7. ⏳ 实现对话窗口组件
8. ⏳ 集成到商品详情和订单页面
9. ⏳ 整体测试和优化

## 七、后续扩展功能

- [ ] 实时消息推送（WebSocket）
- [ ] 图片发送功能
- [ ] 消息撤回功能
- [ ] 消息搜索功能
- [ ] 黑名单功能
- [ ] 消息通知设置
- [ ] 表情包支持
- [ ] 语音消息支持

## 八、数据库执行说明

在执行数据库脚本前，请确保：
1. 备份现有数据
2. 检查字符集设置（utf8mb4）
3. 检查表名是否冲突
4. 执行顺序：先执行 `create_contact_tables.sql`

执行命令：
```bash
mysql -u [username] -p [database_name] < create_contact_tables.sql
```

## 九、技术栈

- **后端**：Spring Boot, JPA, MySQL
- **前端**：Vue 3, Element Plus, Axios
- **实时通信**：WebSocket (可选)
- **缓存**：Redis (可选)

---

**文档创建日期**：2025-10-26
**最后更新**：2025-10-26
**状态**：数据库和基础架构已完成，Service和Controller待实现
