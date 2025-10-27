<template>
  <div class="messages-page">
    <!-- 未登录提示 -->
    <div v-if="!isLoggedIn" class="login-prompt">
      <div class="prompt-content">
        <el-icon class="prompt-icon"><ChatDotRound /></el-icon>
        <h2>请先登录</h2>
        <p>登录后即可查看和发送消息</p>
        <div class="prompt-actions">
          <UnifiedButton type="primary" @click="$router.push('/login')">立即登录</UnifiedButton>
          <UnifiedButton @click="$router.push('/register')">注册账号</UnifiedButton>
        </div>
      </div>
    </div>
    
    <!-- 消息内容 -->
    <div v-else class="messages-content">
      <div class="container">
        <div class="messages-layout">
          <!-- 一级窗口：对话列表 -->
          <ConversationList
            :hidden="showChatWindow"
            :conversations="conversations"
            :selected-conversation-id="selectedConversationId"
            :total-unread-count="totalUnreadCount"
            :loading="loading"
            :get-avatar-url="getAvatarUrl"
            :format-time="formatTime"
            @select="selectConversation"
          />
          
          <!-- 二级窗口：聊天窗口 -->
          <div class="chat-panel" :class="{ 'hidden': !showChatWindow }">
            <div v-if="!selectedConversationId" class="chat-empty">
              <el-icon class="empty-icon"><ChatDotRound /></el-icon>
              <p>选择一个对话开始聊天</p>
            </div>
            
            <ChatWindow
              v-else
              :current-conversation="currentConversation"
              :messages="messages"
              :messages-loading="messagesLoading"
              v-model="messageContent"
              :sending="sending"
              :get-avatar-url="getAvatarUrl"
              :format-time="formatTime"
              :default-commodity-id="defaultCommodityId"
              :default-order-id="defaultOrderId"
              @send="sendMessage"
              @back="backToConversations"
              @view-profile="viewUserProfile"
            />
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { ref, computed, onMounted, onUnmounted, watch, nextTick } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '../stores/user'
import { useMessageStore } from '../stores/message'
import { ElMessage } from 'element-plus'
import { ChatDotRound } from '@element-plus/icons-vue'
import { formatTime } from '../utils/formatUtils'
import { isMobile as globalIsMobile, detectMobile } from '../utils/responsive'
import { commodityAPI, orderAPI } from '../api'
import UnifiedButton from '../components/common/UnifiedButton.vue'
import ConversationList from '../components/messages/ConversationList.vue'
import ChatWindow from '../components/messages/ChatWindow.vue'

export default {
  name: 'MessagesPage',
  components: {
    ChatDotRound,
    UnifiedButton,
    ConversationList,
    ChatWindow
  },
  setup() {
    const router = useRouter()
    const route = useRoute()
    const userStore = useUserStore()
    const messageStore = useMessageStore()
    
    const isLoggedIn = computed(() => userStore.isLoggedIn)
    const userNickname = computed(() => userStore.user?.nickname)
    const userAvatar = computed(() => userStore.user?.avatar)
    
    // 从 store 获取状态
    const loading = computed(() => messageStore.loading.conversations)
    const messagesLoading = computed(() => messageStore.loading.messages)
    const sending = computed(() => messageStore.loading.sending)
    const conversations = computed(() => messageStore.conversations)
    const messages = computed(() => messageStore.messages)
    const selectedConversationId = computed({
      get: () => messageStore.selectedConversationId,
      set: (val) => { messageStore.selectedConversationId = val }
    })
    const currentConversation = computed({
      get: () => messageStore.currentConversation,
      set: (val) => { messageStore.currentConversation = val }
    })
    const totalUnreadCount = computed(() => messageStore.totalUnreadCount)
    const showChatWindow = computed({
      get: () => messageStore.showChatWindow,
      set: (val) => { messageStore.showChatWindow = val }
    })
    
    const messageContent = ref('')
    const messagesListRef = ref(null)
    
    // 获取头像URL
    const getAvatarUrl = (avatar) => {
      if (!avatar) return ''
      if (avatar.startsWith('http')) return avatar
      return `http://localhost:8080/uploads/avatars/${avatar}`
    }
    
    // 获取对话列表 - 使用 store
    const fetchConversations = async () => {
      await messageStore.fetchConversations()
      await nextTick()
      scrollToBottom()
    }
    
    // 获取未读数 - 使用 store
    const fetchUnreadCount = async () => {
      await messageStore.fetchUnreadCount()
    }
    
    // 选择对话 - 使用 store
    const selectConversation = async (conversation) => {
      await messageStore.selectConversation(conversation)
      
      // 移动端：显示聊天窗口
      if (globalIsMobile.value) {
        messageStore.showChat()
      }
      
      await nextTick()
      scrollToBottom()
    }
    
    // 发送消息 - 使用 store
    const sendMessage = async (options = {}) => {
      if (!messageContent.value.trim()) {
        return
      }
      
      if (!selectedConversationId.value) {
        ElMessage.warning('请先选择一个对话')
        return
      }
      
      try {
        const response = await messageStore.sendMessage(
          selectedConversationId.value,
          currentConversation.value.otherUserId,
          messageContent.value,
          options.commodityId || null,
          options.orderId || null
        )
        
        if (response.success) {
          messageContent.value = ''
          await nextTick()
          scrollToBottom()
        }
      } catch (error) {
        // 错误已在 store 中处理
      }
    }
    
    // 查看用户主页
    const viewUserProfile = (userId) => {
      if (userId) {
        router.push(`/home/${userId}`)
      }
    }
    
    // 滚动到底部
    const scrollToBottom = () => {
      if (messagesListRef.value) {
        messagesListRef.value.scrollTop = messagesListRef.value.scrollHeight
      }
    }
    
    // 从路由参数获取商品/订单ID
    const defaultCommodityId = computed(() => route.query.commodityId || null)
    const defaultOrderId = computed(() => route.query.orderId || null)
    
    // 获取消息的详细信息（商品/订单）
    const enrichMessages = async (messageList) => {
      const enrichPromises = messageList.map(async (message) => {
        // 如果有商品ID，获取商品信息
        if (message.commodityId && !message.commodity) {
          try {
            const response = await commodityAPI.getDetail(message.commodityId)
            if (response.success) {
              message.commodity = response.data
            }
          } catch (error) {
            console.error('获取商品信息失败:', error)
          }
        }
        
        // 如果有订单ID，获取订单信息
        if (message.orderId && !message.order) {
          try {
            const response = await orderAPI.getDetail(message.orderId)
            if (response.success) {
              message.order = response.data
            }
          } catch (error) {
            console.error('获取订单信息失败:', error)
          }
        }
        
        return message
      })
      
      return Promise.all(enrichPromises)
    }
    
    // 监听消息变化，自动获取详细信息
    watch(() => messages.value, async (newMessages) => {
      if (newMessages && newMessages.length > 0) {
        await enrichMessages(newMessages)
      }
    }, { immediate: true, deep: true })
    
    // 监听路由变化
    watch(() => router.currentRoute.value.query.conversationId, async (conversationId) => {
      if (conversationId) {
        const conversation = conversations.value.find(c => c.conversationId === conversationId)
        if (conversation) {
          await selectConversation(conversation)
        }
      }
    })
    
    // 监听窗口大小变化
    let resizeTimer = null
    const handleResize = () => {
      if (resizeTimer) clearTimeout(resizeTimer)
      resizeTimer = setTimeout(detectMobile, 150)
    }
    
    onMounted(() => {
      // 初始化响应式检测
      detectMobile()
      
      // 监听窗口大小变化
      window.addEventListener('resize', handleResize)
      window.addEventListener('orientationchange', handleResize)
      
      if (isLoggedIn.value) {
        fetchConversations()
        fetchUnreadCount()
        
        // 如果URL中有conversationId，自动选择
        const conversationId = router.currentRoute.value.query.conversationId
        if (conversationId) {
          nextTick(() => {
            const conversation = conversations.value.find(c => c.conversationId === conversationId)
            if (conversation) {
              selectConversation(conversation)
            }
          })
        }
      }
    })
    
    onUnmounted(() => {
      // 清理监听器
      window.removeEventListener('resize', handleResize)
      window.removeEventListener('orientationchange', handleResize)
    })
    
    // 返回对话列表 - 使用 store
    const backToConversations = () => {
      messageStore.hideChat()
      messageStore.clearCurrentConversation()
    }
    
    return {
      isLoggedIn,
      userNickname,
      userAvatar,
      loading,
      messagesLoading,
      sending,
      conversations,
      messages,
      selectedConversationId,
      currentConversation,
      messageContent,
      totalUnreadCount,
      messagesListRef,
      showChatWindow,
      getAvatarUrl,
      formatTime,
      selectConversation,
      sendMessage,
      viewUserProfile,
      backToConversations,
      defaultCommodityId,
      defaultOrderId
    }
  }
}
</script>

<style scoped>
.messages-page {
  min-height: 100vh;
  background-color: #f5f5f5;
}

/* 未登录提示 */
.login-prompt {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 80vh;
}

.prompt-content {
  text-align: center;
  padding: 40px;
  background: white;
  border-radius: 16px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.08);
  max-width: 400px;
}

.prompt-icon {
  font-size: 64px;
  color: var(--primary-color);
  margin-bottom: 20px;
}

.prompt-content h2 {
  color: var(--primary-color);
  margin-bottom: 10px;
  font-weight: normal;
}

.prompt-content p {
  color: #666;
  margin-bottom: 30px;
  font-weight: normal;
}

.prompt-actions {
  display: flex;
  gap: 10px;
  justify-content: center;
}

.prompt-actions .el-button {
  border-radius: 24px;
  height: 48px;
  font-size: 16px;
  font-weight: normal;
}

/* 消息内容 */
.messages-content {
  padding: 40px 0;
}

.messages-layout {
  display: grid;
  grid-template-columns: 320px 1fr;
  gap: 20px;
  height: calc(100vh - 180px);
  min-height: 600px;
}

/* 对话列表面板 */
.conversations-panel {
  background: white;
  border-radius: 16px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.08);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.panel-header {
  padding: 20px;
  border-bottom: 1px solid #f0f0f0;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.panel-header h3 {
  color: var(--primary-color);
  font-size: 18px;
  font-weight: normal;
  margin: 0;
}

.unread-badge {
  background-color: #f56c6c;
  color: white;
  padding: 2px 8px;
  border-radius: 10px;
  font-size: 12px;
  font-weight: normal;
}

.conversations-list {
  flex: 1;
  overflow-y: auto;
}

.conversation-item {
  display: flex;
  padding: 15px 20px;
  cursor: pointer;
  transition: background-color 0.3s ease;
  border-bottom: 1px solid #f5f5f5;
}

.conversation-item:hover {
  background-color: #f9f9f9;
}

.conversation-item.active {
  background-color: rgba(106, 1, 94, 0.05);
  border-left: 3px solid var(--primary-color);
}

.conversation-avatar {
  position: relative;
  margin-right: 12px;
}

.unread-dot {
  position: absolute;
  top: 0;
  right: 0;
  width: 12px;
  height: 12px;
  background-color: #f56c6c;
  border-radius: 50%;
  border: 2px solid white;
}

.conversation-info {
  flex: 1;
  overflow: hidden;
}

.info-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 6px;
}

.user-name {
  font-size: 15px;
  color: #333;
  font-weight: normal;
}

.deleted-tag {
  font-size: 12px;
  color: #999;
  background: #f5f5f5;
  padding: 2px 6px;
  border-radius: 4px;
  margin-left: 8px;
}

.time {
  font-size: 12px;
  color: #999;
  font-weight: normal;
}

.last-message {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.message-content {
  font-size: 13px;
  color: #666;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  flex: 1;
  font-weight: normal;
}

/* 聊天面板 */
.chat-panel {
  background: white;
  border-radius: 16px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.08);
  display: flex;
  flex-direction: column;
  overflow: hidden;
  height: 100%; /* 确保占满父容器高度 */
}

.chat-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  color: #999;
}

.empty-icon {
  font-size: 64px;
  margin-bottom: 20px;
}

.chat-window {
  display: flex;
  flex-direction: column;
  height: 100%;
}

.chat-header {
  padding: 20px;
  border-bottom: 1px solid #f0f0f0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

/* 桌面端返回按钮样式 */
.desktop-back-button {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: #f5f5f5;
  cursor: pointer;
  transition: all 0.3s ease;
  flex-shrink: 0;
  color: var(--primary-color);
}

.desktop-back-button:hover {
  background: #e0e0e0;
}

.desktop-back-button .el-icon {
  font-size: 20px;
}

.chat-user-info {
  display: flex;
  align-items: center;
  gap: 12px;
  flex: 1;
}

.user-info {
  display: flex;
  flex-direction: column;
}

.user-info .user-name {
  font-size: 16px;
  color: #333;
  font-weight: normal;
}

.chat-actions .el-button {
  color: var(--primary-color);
  border-radius: 20px;
}

/* 消息列表 */
.messages-list {
  flex: 1 1 0; /* 允许缩小到0，占满剩余空间 */
  overflow-y: auto;
  padding: 20px;
  display: flex;
  flex-direction: column;
  gap: 15px;
  min-height: 0; /* 允许缩小到内容大小 */
}

.message-item {
  display: flex;
  gap: 10px;
  align-items: flex-start;
}

.message-item.my-message {
  justify-content: flex-end;
}

.message-avatar {
  flex-shrink: 0;
  width: 36px;
  height: 36px;
}

.message-content {
  display: flex;
  flex-direction: column;
  max-width: 60%;
  min-width: 100px;
  flex: 0 1 auto; /* 允许缩小但保持内容 */
  overflow: visible; /* 允许气泡扩展 */
}

.message-item.my-message .message-content {
  align-items: flex-end;
}

.message-bubble {
  background-color: #f0f0f0;
  padding: 10px 15px;
  border-radius: 12px;
  word-wrap: break-word;
  word-break: break-word; /* 强制换行，处理长单词 */
  overflow-wrap: break-word; /* 现代浏览器标准 */
  max-width: 100%;
  box-sizing: border-box; /* 包含padding和border */
  width: fit-content; /* 根据内容自适应宽度 */
  min-width: 0; /* 允许缩小 */
}

.message-item.my-message .message-bubble {
  background-color: var(--primary-color);
  color: white;
}

.message-bubble p {
  margin: 0;
  font-size: 14px;
  line-height: 1.5;
  font-weight: normal;
  word-wrap: break-word;
  word-break: break-word; /* 强制换行 */
  overflow-wrap: break-word; /* 现代浏览器标准 */
  white-space: normal; /* 允许换行 */
  overflow: hidden; /* 防止溢出 */
}

.message-time {
  font-size: 12px;
  color: #999;
  margin-top: 4px;
  font-weight: normal;
}

/* 消息元信息（时间和已读状态） */
.message-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 4px;
  justify-content: flex-end;
}

.message-meta-other {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 4px;
  justify-content: flex-start; /* 左对齐 */
}

.read-status {
  font-size: 12px;
  color: #67c23a;
  margin-top: 4px; /* 添加与message-time相同的上边距 */
  font-weight: normal;
}

/* 消息输入区域 */
.message-input-area {
  border-top: 1px solid #f0f0f0;
  padding: 15px 20px;
  box-sizing: border-box; /* 确保padding计算在内 */
  max-width: 100%;
  display: flex;
  flex-direction: column;
  flex-shrink: 0; /* 防止输入区域被压缩 */
  overflow-y: auto; /* 如果内容过多，允许滚动 */
  max-height: 250px; /* 限制最大高度，防止超出容器 */
}

.message-input-area :deep(.el-textarea__inner) {
  border-radius: 12px;
  border-color: #e0e0e0;
  resize: none;
  max-width: 100%; /* 防止输入框过宽 */
  box-sizing: border-box; /* 确保padding和border计算在内 */
}

.message-input-area :deep(.el-textarea__inner):focus {
  border-color: var(--primary-color);
}

.input-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 10px;
  max-width: 100%; /* 防止操作栏过宽 */
  box-sizing: border-box;
}

.input-tip {
  font-size: 12px;
  color: #999;
  font-weight: normal;
}

.input-actions .el-button {
  border-radius: 20px;
}

/* 响应式设计 */
@media (max-width: 900px) {
  .messages-layout {
    grid-template-columns: 1fr;
    position: relative;
  }
  
  .conversations-panel {
    position: absolute;
    top: 0;
    left: 0;
    width: 100%;
    height: 100%;
    z-index: 1;
    background: white;
  }
  
  .conversations-panel.hidden {
    display: none;
  }
  
  .chat-panel {
    position: absolute;
    top: 0;
    left: 0;
    width: 100%;
    height: 100%;
    z-index: 2;
    background: white;
  }
  
  .chat-panel.hidden {
    display: none;
  }
  
  /* 移动端输入区域优化 - 调整上下间距 */
  .message-input-area {
    padding: 10px 16px 8px 16px; /* 上padding 10px，下padding 8px，左右保持 */
    display: flex;
    flex-direction: column;
    gap: 8px; /* 减小输入框和按钮之间的间距 */
  }
  
  .message-input-area :deep(.el-textarea__inner) {
    width: 100% !important;
    max-width: 100% !important;
    min-height: 60px; /* 减小输入框高度 */
  }
  
  .input-actions {
    display: flex;
    flex-direction: column;
    gap: 6px; /* 减小操作栏内部间距 */
    width: 100%;
    margin-top: 0; /* 移除额外的上边距 */
  }
  
  .input-tip {
    display: none; /* 移动端隐藏提示 */
  }
  
  .input-actions .el-button {
    width: 100%;
    padding: 10px 16px; /* 适当减小按钮的上下padding */
  }
}
</style>

