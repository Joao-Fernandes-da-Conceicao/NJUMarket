<template>
  <div class="ai-chat-container">
      <!-- 左侧聊天列表 -->
      <div class="chat-sidebar">
        <div class="sidebar-header">
          <h3>聊天列表</h3>
          <UnifiedButton text @click="createNewChat" size="small">
            <el-icon><Plus /></el-icon>
            新建对话
          </UnifiedButton>
        </div>
        <div class="chat-list" v-loading="chatListLoading">
          <div 
            v-for="chat in chatList" 
            :key="chat.conversationId"
            class="chat-item"
            :class="{ active: chat.conversationId === conversationId }"
            @click="switchChat(chat.conversationId)"
          >
            <div class="chat-item-title">{{ chat.title || '新对话' }}</div>
            <div class="chat-item-preview">{{ chat.lastMessage || '暂无消息' }}</div>
            <div class="chat-item-time">{{ formatChatTime(chat.lastMessageTime) }}</div>
          </div>
          <div v-if="chatList.length === 0 && !chatListLoading" class="empty-chat-list">
            <p>暂无聊天记录</p>
            <p class="empty-tip">点击"新建对话"开始聊天</p>
          </div>
        </div>
      </div>

      <!-- 右侧聊天区域 -->
      <div class="chat-main">
        <!-- 聊天头部 -->
        <div class="chat-header">
          <div class="header-left">
            <el-icon class="ai-icon"><ChatDotRound /></el-icon>
            <div class="header-info">
              <h2>AI 购物助手</h2>
              <span class="status-text">在线</span>
            </div>
          </div>
          <div class="header-actions">
            <UnifiedButton text @click="clearConversation">清空对话</UnifiedButton>
          </div>
        </div>

      <!-- 消息列表 -->
      <div class="messages-list" ref="messagesListRef" v-loading="loading">
        <div v-if="messages.length === 0" class="empty-state">
          <el-icon class="empty-icon"><ChatDotRound /></el-icon>
          <p>开始与 AI 助手对话吧！</p>
          <p class="empty-tip">例如："我想买一个二手笔记本电脑"</p>
        </div>
        
        <div
          v-for="(message, index) in messages"
          :key="index"
          class="message-item"
          :class="{ 'user-message': message.role === 'user', 'ai-message': message.role === 'assistant' }"
        >
          <div class="message-avatar">
            <el-avatar :size="36" v-if="message.role === 'user'">
              {{ userStore.user?.nickname?.charAt(0) || 'U' }}
            </el-avatar>
            <el-avatar :size="36" class="ai-avatar" v-else>
              <el-icon><ChatDotRound /></el-icon>
            </el-avatar>
          </div>
          
          <div class="message-content">
            <div class="message-bubble">
              <p v-html="formatMessage(message.content)"></p>
            </div>
            
            <!-- 推荐商品卡片 -->
            <div v-if="message.recommendedCommodities && message.recommendedCommodities.length > 0" class="recommended-commodities">
              <div class="recommendations-title">为您推荐：</div>
              <div class="commodities-grid">
                <CommodityCard
                  v-for="commodity in message.recommendedCommodities"
                  :key="commodity.commodityId"
                  :commodity="commodity"
                  :show-seller-info="true"
                  @click="handleCommodityClick(commodity.commodityId)"
                />
              </div>
            </div>
            
            <div class="message-time">{{ formatTime(message.timestamp) }}</div>
          </div>
        </div>
        
        <!-- 加载中指示器 -->
        <div v-if="sending" class="message-item ai-message">
          <div class="message-avatar">
            <el-avatar :size="36" class="ai-avatar">
              <el-icon><ChatDotRound /></el-icon>
            </el-avatar>
          </div>
          <div class="message-content">
            <div class="message-bubble">
              <div class="typing-indicator">
                <span></span>
                <span></span>
                <span></span>
              </div>
            </div>
          </div>
        </div>
      </div>

        <!-- 输入区域 -->
        <div class="input-area">
          <UnifiedInput
            v-model="inputMessage"
            type="textarea"
            :autosize="{ minRows: 2, maxRows: 6 }"
            placeholder="输入您的问题或需求..."
            @keydown.enter.ctrl="sendMessage"
            @keydown.enter.exact.prevent="sendMessage"
          />
          <div class="input-actions">
            <span class="input-tip">Enter 发送，Ctrl + Enter 换行</span>
            <UnifiedButton 
              type="primary" 
              :disabled="!inputMessage.trim() || sending" 
              @click="sendMessage"
            >
              <el-icon v-if="!sending"><Promotion /></el-icon>
              <span v-if="!sending">发送</span>
              <span v-else>发送中...</span>
            </UnifiedButton>
          </div>
        </div>
      </div>
    </div>
</template>

<script setup>
import { ref, onMounted, nextTick, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '../stores/user'
import { commodityAPI } from '../api'
import { ElMessage } from 'element-plus'
import { ChatDotRound, Promotion, Plus } from '@element-plus/icons-vue'
import UnifiedButton from '../components/common/UnifiedButton.vue'
import UnifiedInput from '../components/common/UnifiedInput.vue'
import CommodityCard from '../components/commodity/CommodityCard.vue'
import { formatTime } from '../utils/formatUtils'

const router = useRouter()
const userStore = useUserStore()

const messages = ref([])
const inputMessage = ref('')
const sending = ref(false)
const loading = ref(false)
const conversationId = ref(null)
const messagesListRef = ref(null)
const chatList = ref([])
const chatListLoading = ref(false)

// 发送消息
const sendMessage = async () => {
  console.log('sendMessage 被调用')
  console.log('inputMessage:', inputMessage.value)
  console.log('sending:', sending.value)
  
  if (!inputMessage.value.trim() || sending.value) {
    console.log('消息为空或正在发送，跳过')
    return
  }
  
  const userMessage = inputMessage.value.trim()
  inputMessage.value = ''
  
  console.log('准备发送消息:', userMessage)
  
  // 添加用户消息
  messages.value.push({
    role: 'user',
    content: userMessage,
    timestamp: Date.now()
  })
  
  console.log('用户消息已添加到列表，当前消息数:', messages.value.length)
  
  // 滚动到底部
  await nextTick()
  scrollToBottom()
  
  sending.value = true
  
  // 如果没有 conversationId，生成一个新的
  if (!conversationId.value) {
    conversationId.value = generateConversationId()
  }
  
  try {
    // 非流式 AI 对话
    const response = await commodityAPI.aiAgentChat(userMessage, conversationId.value)
    if (response.success && response.data) {
      const { reply, conversationId: cid, recommendedCommodities } = response.data
      messages.value.push({
        role: 'assistant',
        content: reply || '抱歉，我没有理解你的问题。',
        recommendedCommodities: recommendedCommodities || [],
        timestamp: Date.now()
      })
      if (cid) conversationId.value = cid
    } else {
      messages.value.push({
        role: 'assistant',
        content: '抱歉，我遇到了一些问题，请稍后再试。',
        recommendedCommodities: [],
        timestamp: Date.now()
      })
    }
  } catch (error) {
    console.error('AI 对话失败:', error)
    ElMessage.error('AI 对话失败，请稍后重试')
    messages.value.push({
      role: 'assistant',
      content: '抱歉，我遇到了一些问题，请稍后再试。',
      recommendedCommodities: [],
      timestamp: Date.now()
    })
  } finally {
    sending.value = false
    await nextTick()
    scrollToBottom()
    loadChatList()
  }
}

// 生成对话ID
const generateConversationId = () => {
  return `ai_chat_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`
}

// 加载聊天列表
const loadChatList = async () => {
  chatListLoading.value = true
  try {
    const response = await commodityAPI.getAIChatList(50)
    if (response.success && response.data) {
      chatList.value = response.data
    }
  } catch (error) {
    console.error('加载聊天列表失败:', error)
  } finally {
    chatListLoading.value = false
  }
}

// 切换聊天
const switchChat = async (newConversationId) => {
  if (newConversationId === conversationId.value) {
    return
  }
  
  // 如果正在发送消息，不允许切换
  if (sending.value) {
    ElMessage.warning('正在发送消息，请稍候')
    return
  }
  
  loading.value = true
  conversationId.value = newConversationId
  messages.value = []
  
  try {
    // 加载该chat的消息历史
    const response = await commodityAPI.getAIChatMessages(newConversationId, 100)
    if (response.success && response.data) {
      // 转换消息格式
      messages.value = response.data.map(msg => ({
        role: msg.role === 'user' ? 'user' : 'assistant',
        content: msg.content,
        recommendedCommodities: msg.recommendedCommodities || [], // 从后端获取推荐商品列表
        timestamp: msg.createdAt ? new Date(msg.createdAt).getTime() : Date.now()
      }))
      
      await nextTick()
      scrollToBottom()
    }
  } catch (error) {
    console.error('加载消息历史失败:', error)
    ElMessage.error('加载消息历史失败')
  } finally {
    loading.value = false
  }
}

// 创建新对话
const createNewChat = () => {
  if (sending.value) {
    ElMessage.warning('正在发送消息，请稍候')
    return
  }
  
  conversationId.value = generateConversationId()
  messages.value = []
  ElMessage.success('已创建新对话')
  
  // 刷新聊天列表
  loadChatList()
}

// 清空对话
const clearConversation = () => {
  if (sending.value) {
    ElMessage.warning('正在发送消息，请稍候')
    return
  }
  
  messages.value = []
  if (conversationId.value) {
    // 不清空conversationId，保持当前chat
    ElMessage.success('对话已清空')
  } else {
    conversationId.value = null
    ElMessage.success('对话已清空')
  }
}

// 格式化聊天时间
const formatChatTime = (time) => {
  if (!time) return ''
  const date = new Date(time)
  const now = new Date()
  const diff = now - date
  
  if (diff < 60000) { // 1分钟内
    return '刚刚'
  } else if (diff < 3600000) { // 1小时内
    return `${Math.floor(diff / 60000)}分钟前`
  } else if (diff < 86400000) { // 24小时内
    return `${Math.floor(diff / 3600000)}小时前`
  } else if (diff < 604800000) { // 7天内
    return `${Math.floor(diff / 86400000)}天前`
  } else {
    return date.toLocaleDateString('zh-CN', { month: 'short', day: 'numeric' })
  }
}

// 监听conversationId变化，自动刷新聊天列表
watch(conversationId, (newId) => {
  if (newId) {
    // 延迟刷新，等待消息存储完成
    setTimeout(() => {
      loadChatList()
    }, 1000)
  }
})

// 滚动到底部
const scrollToBottom = () => {
  if (messagesListRef.value) {
    nextTick(() => {
      messagesListRef.value.scrollTop = messagesListRef.value.scrollHeight
    })
  }
}

// 格式化消息（支持 Markdown 基本语法）
const formatMessage = (content) => {
  if (!content) return ''
  
  // 转义 HTML 特殊字符，防止 XSS
  let html = content
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
  
  // Markdown 解析（按顺序处理，避免冲突）
  
  // 1. 代码块（```代码块```）- 先处理，避免被其他规则影响
  html = html.replace(/```([\s\S]*?)```/g, (match, code) => {
    return `<pre class="code-block"><code>${code}</code></pre>`
  })
  
  // 2. 行内代码（`代码`）- 在代码块之后处理
  html = html.replace(/`([^`\n]+)`/g, '<code class="inline-code">$1</code>')
  
  // 3. 加粗（**文本** 或 __文本__）- 在斜体之前处理
  html = html.replace(/\*\*([^*]+?)\*\*/g, '<strong>$1</strong>')
  html = html.replace(/__([^_]+?)__/g, '<strong>$1</strong>')
  
  // 4. 删除线（~~文本~~）
  html = html.replace(/~~([^~]+?)~~/g, '<del>$1</del>')
  
  // 5. 斜体（*文本* 或 _文本_）- 最后处理，避免与加粗冲突
  // 只匹配单个星号或下划线，且前后不是相同字符
  html = html.replace(/(?<!\*)\*([^*\n]+?)\*(?!\*)/g, '<em>$1</em>')
  html = html.replace(/(?<!_)_([^_\n]+?)_(?!_)/g, '<em>$1</em>')
  
  // 6. 换行（\n）
  html = html.replace(/\n/g, '<br>')
  
  return html
}

// 点击商品卡片
const handleCommodityClick = (commodityId) => {
  router.push(`/commodity/${commodityId}`)
}

onMounted(() => {
  console.log('AIChat 组件已挂载')
  console.log('用户登录状态:', userStore.isLoggedIn)
  console.log('用户信息:', userStore.user)
  
  // 加载聊天列表
  loadChatList()
  
  scrollToBottom()
})
</script>

<style scoped>
.ai-chat-container {
  display: flex;
  height: calc(100vh - 200px);
  max-width: 1400px;
  margin: 20px auto;
  padding: 0;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
  overflow: hidden;
}

.chat-sidebar {
  width: 280px;
  border-right: 1px solid #e4e7ed;
  display: flex;
  flex-direction: column;
  background: #f5f7fa;
}

.sidebar-header {
  padding: 16px;
  border-bottom: 1px solid #e4e7ed;
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: #fff;
}

.sidebar-header h3 {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

.chat-list {
  flex: 1;
  overflow-y: auto;
  padding: 8px;
}

.chat-item {
  padding: 12px;
  margin-bottom: 8px;
  background: #fff;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
  border: 1px solid transparent;
}

.chat-item:hover {
  background: #f0f2f5;
  border-color: #e4e7ed;
}

.chat-item.active {
  background: #e6f4ff;
  border-color: #409eff;
}

.chat-item-title {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.chat-item-preview {
  font-size: 12px;
  color: #909399;
  margin-bottom: 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.chat-item-time {
  font-size: 11px;
  color: #c0c4cc;
}

.empty-chat-list {
  padding: 40px 20px;
  text-align: center;
  color: #909399;
}

.empty-chat-list .empty-tip {
  font-size: 12px;
  color: #c0c4cc;
  margin-top: 8px;
}

.chat-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.chat-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 24px;
  border-bottom: 1px solid #e4e7ed;
  background: #fff;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.ai-icon {
  font-size: 32px;
  color: #409eff;
}

.header-info h2 {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  color: #303133;
}

.status-text {
  font-size: 12px;
  color: #67c23a;
}

.messages-list {
  flex: 1;
  overflow-y: auto;
  padding: 24px;
  background: #f5f7fa;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  color: #909399;
}

.empty-icon {
  font-size: 64px;
  margin-bottom: 16px;
  color: #c0c4cc;
}

.empty-state p {
  margin: 8px 0;
  font-size: 16px;
}

.empty-tip {
  font-size: 14px;
  color: #c0c4cc;
}

.message-item {
  display: flex;
  gap: 12px;
  margin-bottom: 24px;
  animation: fadeIn 0.3s ease-in;
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.user-message {
  flex-direction: row-reverse;
}

.message-avatar {
  flex-shrink: 0;
}

.ai-avatar {
  background: #409eff;
  color: #fff;
}

.message-content {
  flex: 1;
  max-width: 70%;
}

.user-message .message-content {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
}

.ai-message .message-content {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
}

.message-bubble {
  padding: 12px 16px;
  border-radius: 12px;
  word-wrap: break-word;
  line-height: 1.6;
}

.user-message .message-bubble {
  background: #409eff;
  color: #fff;
  border-bottom-right-radius: 4px;
}

.ai-message .message-bubble {
  background: #fff;
  color: #303133;
  border: 1px solid #e4e7ed;
  border-bottom-left-radius: 4px;
}

.message-bubble p {
  margin: 0;
}

/* Markdown 样式 */
.message-bubble strong {
  font-weight: 600;
  color: inherit;
}

.message-bubble em {
  font-style: italic;
}

.message-bubble del {
  text-decoration: line-through;
  opacity: 0.7;
}

.message-bubble code {
  font-family: 'Courier New', Courier, monospace;
  font-size: 0.9em;
  padding: 2px 4px;
  border-radius: 3px;
  background: rgba(0, 0, 0, 0.05);
}

.message-bubble .inline-code {
  background: rgba(0, 0, 0, 0.08);
  padding: 2px 6px;
  border-radius: 4px;
  font-size: 0.9em;
}

.message-bubble .code-block {
  background: rgba(0, 0, 0, 0.05);
  padding: 12px;
  border-radius: 6px;
  margin: 8px 0;
  overflow-x: auto;
  border-left: 3px solid #409eff;
}

.message-bubble .code-block code {
  background: transparent;
  padding: 0;
  font-size: 0.9em;
  line-height: 1.5;
  white-space: pre;
}

.user-message .message-bubble .code-block {
  background: rgba(255, 255, 255, 0.2);
  border-left-color: rgba(255, 255, 255, 0.5);
}

.user-message .message-bubble code {
  background: rgba(255, 255, 255, 0.2);
}

.message-time {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
  padding: 0 4px;
}

.recommended-commodities {
  margin-top: 16px;
  width: 100%;
}

.recommendations-title {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 12px;
}

.commodities-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 16px;
}

.typing-indicator {
  display: flex;
  gap: 4px;
  padding: 8px 0;
}

.typing-indicator span {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #c0c4cc;
  animation: typing 1.4s infinite;
}

.typing-indicator span:nth-child(2) {
  animation-delay: 0.2s;
}

.typing-indicator span:nth-child(3) {
  animation-delay: 0.4s;
}

@keyframes typing {
  0%, 60%, 100% {
    transform: translateY(0);
    opacity: 0.7;
  }
  30% {
    transform: translateY(-10px);
    opacity: 1;
  }
}

.input-area {
  padding: 16px 24px;
  border-top: 1px solid #e4e7ed;
  background: #fff;
}

.input-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 12px;
}

.input-tip {
  font-size: 12px;
  color: #909399;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .ai-chat-container {
    height: calc(100vh - 80px);
  }
  
  .message-content {
    max-width: 85%;
  }
  
  .commodities-grid {
    grid-template-columns: repeat(auto-fill, minmax(150px, 1fr));
    gap: 12px;
  }
}
</style>

