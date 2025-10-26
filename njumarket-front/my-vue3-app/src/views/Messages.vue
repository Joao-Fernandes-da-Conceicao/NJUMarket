<template>
  <div class="messages-page">
    <!-- 未登录提示 -->
    <div v-if="!isLoggedIn" class="login-prompt">
      <div class="prompt-content">
        <el-icon class="prompt-icon"><ChatDotRound /></el-icon>
        <h2>请先登录</h2>
        <p>登录后即可查看和发送消息</p>
        <div class="prompt-actions">
          <el-button type="primary" @click="$router.push('/login')">立即登录</el-button>
          <el-button @click="$router.push('/register')">注册账号</el-button>
        </div>
      </div>
    </div>
    
    <!-- 消息内容 -->
    <div v-else class="messages-content">
      <div class="container">
        <div class="messages-layout">
          <!-- 左侧：对话列表 -->
          <div class="conversations-panel">
            <div class="panel-header">
              <h3>消息</h3>
              <span class="unread-badge" v-if="totalUnreadCount > 0">{{ totalUnreadCount }}</span>
            </div>
            
            <div class="conversations-list" v-loading="loading">
              <div 
                v-for="conversation in conversations" 
                :key="conversation.conversationId"
                class="conversation-item"
                :class="{ active: selectedConversationId === conversation.conversationId }"
                @click="selectConversation(conversation)"
              >
                <div class="conversation-avatar">
                  <el-avatar :size="48" :src="getAvatarUrl(conversation.otherUserAvatar)">
                    {{ conversation.otherUserNickname?.charAt(0) || 'U' }}
                  </el-avatar>
                  <span v-if="conversation.unreadCount > 0" class="unread-dot"></span>
                </div>
                
                <div class="conversation-info">
                  <div class="info-header">
                    <span class="user-name">{{ conversation.otherUserNickname || '用户' }}</span>
                    <span v-if="conversation.otherUserIsDeleted" class="deleted-tag">已注销</span>
                    <span class="time">{{ formatTime(conversation.lastMessageTime) }}</span>
                  </div>
                  <div class="last-message">
                    <span class="message-content">{{ conversation.lastMessageContent || '暂无消息' }}</span>
                    <el-badge v-if="conversation.unreadCount > 0" :value="conversation.unreadCount" class="unread-count" />
                  </div>
                </div>
              </div>
              
              <el-empty v-if="conversations.length === 0 && !loading" description="暂无对话" />
            </div>
          </div>
          
          <!-- 右侧：聊天窗口 -->
          <div class="chat-panel">
            <div v-if="!selectedConversationId" class="chat-empty">
              <el-icon class="empty-icon"><ChatDotRound /></el-icon>
              <p>选择一个对话开始聊天</p>
            </div>
            
            <div v-else class="chat-window">
              <!-- 聊天头部 -->
              <div class="chat-header">
                <div class="chat-user-info">
                  <el-avatar :size="40" :src="getAvatarUrl(currentConversation?.otherUserAvatar)">
                    {{ currentConversation?.otherUserNickname?.charAt(0) || 'U' }}
                  </el-avatar>
                  <div class="user-info">
                    <span class="user-name">{{ currentConversation?.otherUserNickname || '用户' }}</span>
                    <span v-if="currentConversation?.otherUserIsDeleted" class="deleted-tag">已注销</span>
                  </div>
                </div>
                <div class="chat-actions">
                  <el-button 
                    text 
                    @click="viewUserProfile(currentConversation?.otherUserId)">
                    查看资料
                  </el-button>
                </div>
              </div>
              
              <!-- 消息列表 -->
              <div class="messages-list" ref="messagesListRef" v-loading="messagesLoading">
                <div 
                  v-for="message in messages" 
                  :key="message.messageId"
                  class="message-item"
                  :class="{ 'my-message': message.isMine }"
                >
                  <!-- 对方消息：头像在左边 -->
                  <template v-if="!message.isMine">
                    <div class="message-avatar">
                      <el-avatar :size="36" :src="getAvatarUrl(message.senderAvatar)">
                        {{ message.senderNickname?.charAt(0) || 'U' }}
                      </el-avatar>
                    </div>
                    
                    <div class="message-content">
                      <div class="message-bubble">
                        <p>{{ message.content }}</p>
                      </div>
                      <div class="message-time">{{ formatTime(message.createdAt) }}</div>
                    </div>
                  </template>
                  
                  <!-- 我的消息：头像在右边，内容右对齐 -->
                  <template v-else>
                    <div class="message-content">
                      <div class="message-bubble">
                        <p>{{ message.content }}</p>
                      </div>
                      <div class="message-meta">
                        <span v-if="message.isRead" class="read-status">已读</span>
                        <span class="message-time">{{ formatTime(message.createdAt) }}</span>
                      </div>
                    </div>
                    
                    <div class="message-avatar">
                      <el-avatar :size="36" :src="getAvatarUrl(userAvatar)">
                        {{ userNickname?.charAt(0) || 'U' }}
                      </el-avatar>
                    </div>
                  </template>
                </div>
                
                <el-empty v-if="messages.length === 0 && !messagesLoading" description="暂无消息，开始聊天吧" />
              </div>
              
              <!-- 消息输入框 -->
              <div class="message-input-area">
                <el-input
                  v-model="messageContent"
                  type="textarea"
                  :rows="3"
                  placeholder="输入消息..."
                  @keyup.ctrl.enter="sendMessage"
                />
                <div class="input-actions">
                  <span class="input-tip">Ctrl + Enter 发送</span>
                  <el-button 
                    type="primary" 
                    :disabled="!messageContent.trim() || sending"
                    @click="sendMessage">
                    发送
                  </el-button>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { ref, computed, onMounted, watch, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '../stores/user'
import { contactAPI } from '../api'
import { ElMessage } from 'element-plus'
import { ChatDotRound } from '@element-plus/icons-vue'
import { formatTime } from '../utils/formatUtils'

export default {
  name: 'MessagesPage',
  components: {
    ChatDotRound
  },
  setup() {
    const router = useRouter()
    const userStore = useUserStore()
    
    const isLoggedIn = computed(() => userStore.isLoggedIn)
    const userNickname = computed(() => userStore.user?.nickname)
    const userAvatar = computed(() => userStore.user?.avatar)
    
    const loading = ref(false)
    const messagesLoading = ref(false)
    const sending = ref(false)
    const conversations = ref([])
    const messages = ref([])
    const selectedConversationId = ref(null)
    const currentConversation = ref(null)
    const messageContent = ref('')
    const totalUnreadCount = ref(0)
    const messagesListRef = ref(null)
    
    // 获取头像URL
    const getAvatarUrl = (avatar) => {
      if (!avatar) return ''
      if (avatar.startsWith('http')) return avatar
      return `http://localhost:8080/uploads/avatars/${avatar}`
    }
    
    // 获取对话列表
    const fetchConversations = async () => {
      loading.value = true
      try {
        const response = await contactAPI.getConversations()
        if (response.success) {
          conversations.value = response.data.content || []
        }
      } catch (error) {
        console.error('获取对话列表失败:', error)
        ElMessage.error('获取对话列表失败')
      } finally {
        loading.value = false
      }
    }
    
    // 获取未读数
    const fetchUnreadCount = async () => {
      try {
        const response = await contactAPI.getUnreadCount()
        if (response.success) {
          totalUnreadCount.value = response.data || 0
        }
      } catch (error) {
        console.error('获取未读数失败:', error)
      }
    }
    
    // 选择对话
    const selectConversation = async (conversation) => {
      selectedConversationId.value = conversation.conversationId
      currentConversation.value = conversation
      await fetchMessages(conversation.conversationId)
      
      // 标记为已读
      if (conversation.unreadCount > 0) {
        await markAsRead(conversation.conversationId)
        conversation.unreadCount = 0
        fetchUnreadCount()
      }
    }
    
    // 获取消息列表
    const fetchMessages = async (conversationId) => {
      messagesLoading.value = true
      try {
        const response = await contactAPI.getConversationDetail(conversationId)
        if (response.success) {
          messages.value = (response.data.messages || []).reverse()
          await nextTick()
          scrollToBottom()
        }
      } catch (error) {
        console.error('获取消息失败:', error)
        ElMessage.error('获取消息失败')
      } finally {
        messagesLoading.value = false
      }
    }
    
    // 发送消息
    const sendMessage = async () => {
      if (!messageContent.value.trim()) {
        return
      }
      
      if (!selectedConversationId.value) {
        ElMessage.warning('请先选择一个对话')
        return
      }
      
      sending.value = true
      try {
        const response = await contactAPI.sendMessage({
          conversationId: selectedConversationId.value,
          receiverId: currentConversation.value.otherUserId,
          messageType: 'TEXT',
          content: messageContent.value
        })
        
        if (response.success) {
          messages.value.push(response.data)
          messageContent.value = ''
          await nextTick()
          scrollToBottom()
          
          // 更新对话列表中的最后消息
          const conv = conversations.value.find(c => c.conversationId === selectedConversationId.value)
          if (conv) {
            conv.lastMessageContent = response.data.content
            conv.lastMessageTime = response.data.createdAt
          }
        }
      } catch (error) {
        console.error('发送消息失败:', error)
        ElMessage.error('发送消息失败')
      } finally {
        sending.value = false
      }
    }
    
    // 标记为已读
    const markAsRead = async (conversationId) => {
      try {
        await contactAPI.markAsRead(conversationId)
      } catch (error) {
        console.error('标记已读失败:', error)
      }
    }
    
    // 查看用户资料
    const viewUserProfile = (userId) => {
      if (userId) {
        router.push(`/profile/${userId}`)
      }
    }
    
    // 滚动到底部
    const scrollToBottom = () => {
      if (messagesListRef.value) {
        messagesListRef.value.scrollTop = messagesListRef.value.scrollHeight
      }
    }
    
    // 监听路由变化
    watch(() => router.currentRoute.value.query.conversationId, async (conversationId) => {
      if (conversationId) {
        const conversation = conversations.value.find(c => c.conversationId === conversationId)
        if (conversation) {
          await selectConversation(conversation)
        }
      }
    })
    
    onMounted(() => {
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
      getAvatarUrl,
      formatTime,
      selectConversation,
      sendMessage,
      viewUserProfile
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
}

.chat-user-info {
  display: flex;
  align-items: center;
  gap: 12px;
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
  flex: 1;
  overflow-y: auto;
  padding: 20px;
  display: flex;
  flex-direction: column;
  gap: 15px;
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
}

.message-item.my-message .message-content {
  align-items: flex-end;
}

.message-bubble {
  background-color: #f0f0f0;
  padding: 10px 15px;
  border-radius: 12px;
  word-wrap: break-word;
  max-width: 100%;
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

.read-status {
  font-size: 12px;
  color: #67c23a;
  font-weight: normal;
}

/* 消息输入区域 */
.message-input-area {
  border-top: 1px solid #f0f0f0;
  padding: 15px 20px;
}

.message-input-area :deep(.el-textarea__inner) {
  border-radius: 12px;
  border-color: #e0e0e0;
  resize: none;
}

.message-input-area :deep(.el-textarea__inner):focus {
  border-color: var(--primary-color);
}

.input-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 10px;
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
@media (max-width: 768px) {
  .messages-layout {
    grid-template-columns: 1fr;
  }
  
  .conversations-panel {
    display: none;
  }
  
  .chat-panel {
    display: block;
  }
}
</style>

