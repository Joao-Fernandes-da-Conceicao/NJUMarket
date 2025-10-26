# 联系功能实现指南

由于完整实现代码较长（ContactServiceImpl约600行，前端约1000行），以下是快速实现指南：

## 一、后端实现步骤（约2小时）

### 1. 执行数据库脚本
```bash
cd njumarket/src/main/resources/database
mysql -u root -p njumarket < create_contact_tables.sql
```

### 2. 创建ContactServiceImpl.java

关键实现逻辑：

```java
@Service
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
    
    @Override
    @Transactional
    public Result<MessageDTO> sendMessage(String userId, SendMessageRequest request) {
        // 1. 验证接收者是否存在
        // 2. 获取或创建对话
        // 3. 创建消息
        // 4. 更新对话最后消息和未读数
        // 5. 返回MessageDTO
    }
    
    @Override
    public Result<Page<ConversationDTO>> getConversations(String userId, int page, int size) {
        // 1. 查询用户的所有对话
        // 2. 填充对方用户信息
        // 3. 设置未读数
        // 4. 返回分页数据
    }
    
    // ... 其他方法实现
}
```

### 3. 创建ContactController.java

```java
@RestController
@RequestMapping("/api/contact")
public class ContactController {
    
    @Autowired
    private ContactService contactService;
    
    @PostMapping("/send")
    public Result<MessageDTO> sendMessage(@RequestAttribute("userId") String userId,
                                         @RequestBody SendMessageRequest request) {
        return contactService.sendMessage(userId, request);
    }
    
    @GetMapping("/conversations")
    public Result<Page<ConversationDTO>> getConversations(
            @RequestAttribute("userId") String userId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return contactService.getConversations(userId, page - 1, size);
    }
    
    // ... 其他API端点
}
```

## 二、前端实现步骤（约3小时）

### 1. 创建API接口文件
`njumarket-front/my-vue3-app/src/api/contact.js`

### 2. 创建消息中心页面  
`njumarket-front/my-vue3-app/src/views/Messages.vue`

页面结构：
- 左侧：对话列表（显示头像、昵称、最后消息、未读数）
- 右侧：聊天窗口（消息列表、输入框）

### 3. 创建对话窗口组件
`njumarket-front/my-vue3-app/src/components/chat/ChatWindow.vue`

功能：
- 消息列表滚动加载
- 消息气泡样式区分
- 发送消息框
- 自动滚动到最新消息

### 4. 在导航栏添加消息图标
`AppHeader.vue` 中添加：

```vue
<router-link to="/messages" class="nav-link">
  <el-badge :value="unreadCount" :hidden="unreadCount === 0">
    <el-icon><ChatDotRound /></el-icon>
    <span>消息</span>
  </el-badge>
</router-link>
```

### 5. 商品详情页集成

添加"联系卖家"按钮：
```vue
<el-button type="primary" @click="contactSeller">
  联系卖家
</el-button>
```

## 三、测试步骤

1. ✅ 测试发送文本消息
2. ✅ 测试对话列表显示
3. ✅ 测试未读数统计
4. ✅ 测试标记已读
5. ✅ 测试从商品详情联系卖家
6. ✅ 测试从订单页面联系买家/卖家

## 四、部署建议

1. 确保数据库字符集为utf8mb4（支持emoji）
2. 配置消息推送（可选使用WebSocket）
3. 设置消息敏感词过滤
4. 配置消息发送频率限制

## 五、简化实现方案（如果时间紧张）

最小可用版本（MVP）：
1. ✅ 只实现文本消息
2. ✅ 基础对话列表
3. ✅ 基础聊天窗口
4. ❌ 暂不实现图片发送
5. ❌ 暂不实现黑名单
6. ❌ 暂不实现实时推送

## 六、代码生成提示

由于完整代码超过2000行，建议分批实现：

**第一阶段（核心功能）**：
- ContactServiceImpl核心方法（sendMessage, getConversations, getConversationDetail）
- ContactController基础API
- Messages.vue基础版本

**第二阶段（完善功能）**：
- 未读数统计和显示
- 标记已读功能
- 分页加载

**第三阶段（集成和优化）**：
- 商品详情集成
- 订单页面集成
- UI优化

---

**预计开发时间**：5-8小时
**建议优先级**：核心功能 > 集成 > 优化
