import { defineStore } from 'pinia'
import { contactAPI } from '../api'
import { ElMessage } from 'element-plus'
import { wsClient } from '../utils/websocket'

/**
 * 消息 Store
 * 
 * 注意：后端数据库字段已从 buyer_unread_count/seller_unread_count 
 * 更新为 user_1_count/user_2_count，但前端使用的 conversation.unreadCount 
 * 字段（来自 ConversationDTO）保持不变，因此无需修改前端代码。
 * 未读数通过后端的 getUnreadCountForUser() 方法计算得出。
 */
export const useMessageStore = defineStore('message', {
  state: () => ({
    // 对话列表
    conversations: [],
    // 当前选中的对话
    selectedConversationId: null,
    currentConversation: null,
    // 当前对话的消息列表
    messages: [],
    // 加载状态
    loading: {
      conversations: false,
      messages: false,
      sending: false
    },
    // 未读消息总数（统一从 conversationUnreadMap 计算得出）
    // 后端已更新为使用 user_1_count/user_2_count 字段
    totalUnreadCount: 0,
    // ✅ 6.4.1 状态同步优化：使用Map存储对话未读数，便于快速更新和统一管理
    // 确保所有未读数更新都通过统一方法，避免Store和组件中的不一致
    conversationUnreadMap: new Map(),
    // 移动端聊天窗口显示状态
    showChatWindow: false,
    // WebSocket 连接状态
    wsConnected: false
  }),
  
  getters: {
    // 是否有未读消息
    hasUnread: (state) => state.totalUnreadCount > 0,
    
    // 当前选中的对话信息
    selectedConversation(state) {
      if (!state.selectedConversationId) return null
      return state.conversations.find(c => c.conversationId === state.selectedConversationId)
    },
    
    // 是否正在加载对话
    isLoadingConversations: (state) => state.loading.conversations,
    
    // 是否正在加载消息
    isLoadingMessages: (state) => state.loading.messages,
    
    // 是否正在发送消息
    isSending: (state) => state.loading.sending
  },
  
  actions: {
    // 获取对话列表
    async fetchConversations() {
      this.loading.conversations = true
      try {
        const response = await contactAPI.getConversations()
        if (response.success) {
          this.conversations = response.data.content || []
          
          // ✅ 6.4.1 状态同步优化：从对话列表同步未读数到Map
          // 确保Map与对话列表中的未读数保持一致
          this.conversations.forEach(conv => {
            if (conv.conversationId && conv.unreadCount !== undefined) {
              this.conversationUnreadMap.set(conv.conversationId, conv.unreadCount || 0)
            }
          })
          
          // 重新计算总未读数
          this.totalUnreadCount = Array.from(this.conversationUnreadMap.values())
            .reduce((sum, count) => sum + (count || 0), 0)
        }
        return response
      } catch (error) {
        console.error('获取对话列表失败:', error)
        ElMessage.error('获取对话列表失败')
        throw error
      } finally {
        this.loading.conversations = false
      }
    },
    
    /**
     * 获取未读消息数
     * 后端 API: GET /contact/unread-count
     * 后端已更新为使用 user_1_count/user_2_count 字段进行计算
     */
    async fetchUnreadCount() {
      try {
        const response = await contactAPI.getUnreadCount()
        if (response.success) {
          // ✅ 6.4.1 状态同步优化：使用后端返回的总未读数作为基准
          // 但保留Map机制，后续通过统一方法更新
          const serverTotalUnread = response.data || 0
          this.totalUnreadCount = serverTotalUnread
          
          // 如果后端返回的总未读数与本地Map计算的不一致，以后端为准
          // 但不会覆盖Map，因为Map可能包含更细粒度的对话未读数信息
        }
        return response
      } catch (error) {
        console.error('获取未读数失败:', error)
      }
    },
    
    /**
     * ✅ 6.4.1 状态同步优化：统一更新未读数方法
     * 确保所有未读数更新都通过此方法，避免Store和组件中的不一致
     * 以及WebSocket更新和API更新之间的冲突
     * 
     * @param {string|null} conversationId - 对话ID，如果为null则只更新总未读数
     * @param {number} count - 新的未读数
     * @param {boolean} updateTotal - 是否重新计算总未读数（默认true）
     */
    updateUnreadCount(conversationId, count, updateTotal = true) {
      // 更新对话未读数Map
      if (conversationId) {
        this.conversationUnreadMap.set(conversationId, count || 0)
        
        // 同时更新对话对象中的未读数（保持响应式）
        const conversation = this.conversations.find(
          c => c.conversationId === conversationId
        )
        if (conversation) {
          conversation.unreadCount = count || 0
        }
      }
      
      // 重新计算总未读数（从Map累加）
      if (updateTotal) {
        this.totalUnreadCount = Array.from(this.conversationUnreadMap.values())
          .reduce((sum, count) => sum + (count || 0), 0)
      }
    },
    
    // 选择对话
    async selectConversation(conversation) {
      this.selectedConversationId = conversation.conversationId
      this.currentConversation = conversation
      
      // 获取消息
      await this.fetchMessages(conversation.conversationId)
      
      // ✅ 只要打开对话，立即标记为已读（无论是否有未读数）
      // markAsRead 方法会立即更新本地状态，提供即时UI反馈
      await this.markAsRead(conversation.conversationId)
    },
    
    // 获取消息列表
    async fetchMessages(conversationId) {
      this.loading.messages = true
      try {
        const response = await contactAPI.getConversationDetail(conversationId)
        if (response.success) {
          // 后端已经过滤了双向删除的消息
          this.messages = (response.data.messages || []).reverse()
        }
        return response
      } catch (error) {
        console.error('获取消息失败:', error)
        ElMessage.error('获取消息失败')
        throw error
      } finally {
        this.loading.messages = false
      }
    },
    
    // 发送消息
    async sendMessage(conversationId, receiverId, content, commodityId = null, orderId = null) {
      if (!content.trim()) {
        ElMessage.warning('消息内容不能为空')
        return
      }
      
      if (!conversationId) {
        ElMessage.warning('请先选择一个对话')
        return
      }
      
      // 一个消息只能有一个商品或订单
      if (commodityId && orderId) {
        ElMessage.warning('一个消息只能关联一个商品或订单')
        return
      }
      
      this.loading.sending = true
      try {
        const messageData = {
          conversationId,
          receiverId,
          messageType: 'TEXT',
          content
        }
        
        // 添加商品或订单ID
        if (commodityId) {
          messageData.commodityId = commodityId
        }
        if (orderId) {
          messageData.orderId = orderId
        }
        
        const response = await contactAPI.sendMessage(messageData)
        
        if (response.success) {
          this.messages.push(response.data)
          
          // 更新对话列表中的最后消息
          const conv = this.conversations.find(c => c.conversationId === conversationId)
          if (conv) {
            conv.lastMessageContent = response.data.content
            conv.lastMessageTime = response.data.createdAt
          }
        }
        return response
      } catch (error) {
        console.error('发送消息失败:', error)
        ElMessage.error('发送消息失败')
        throw error
      } finally {
        this.loading.sending = false
      }
    },
    
    // 标记对话为已读
    async markAsRead(conversationId) {
      // 保存旧状态（用于失败时回滚）
      const oldUnreadCount = this.conversationUnreadMap.get(conversationId) || 0
      
      // ✅ 6.4.1 状态同步优化：立即更新本地状态，使用统一更新方法
      // 提供即时的UI反馈，避免等待后端响应
      this.updateUnreadCount(conversationId, 0, true)
      
      try {
        // 调用后端API同步状态
        const response = await contactAPI.markAsRead(conversationId)
        
        if (response.success) {
          // 后端同步成功，再次确认状态（以防后端有额外的更新）
          // 但不再调用fetchUnreadCount，因为WebSocket会推送UNREAD_COUNT_UPDATE事件
          // 这样可以避免API和WebSocket更新的冲突
        } else {
          // 如果后端返回失败，回滚状态
          this.updateUnreadCount(conversationId, oldUnreadCount, true)
        }
        
        return response
      } catch (error) {
        console.error('标记已读失败:', error)
        
        // 回滚状态
        this.updateUnreadCount(conversationId, oldUnreadCount, true)
        
        throw error
      }
    },
    
    // 删除消息
    async deleteMessage(messageId) {
      try {
        const response = await contactAPI.deleteMessage(messageId)
        if (response.success) {
          // 从消息列表中移除
          this.messages = this.messages.filter(msg => msg.messageId !== messageId)
        }
        return response
      } catch (error) {
        console.error('删除消息失败:', error)
        ElMessage.error('删除消息失败')
        throw error
      }
    },
    
    // 删除对话
    async deleteConversation(conversationId) {
      try {
        const response = await contactAPI.deleteConversation(conversationId)
        if (response.success) {
          // 从对话列表中移除
          this.conversations = this.conversations.filter(c => c.conversationId !== conversationId)
          
          // 如果删除的是当前对话，清空选中状态
          if (this.selectedConversationId === conversationId) {
            this.selectedConversationId = null
            this.currentConversation = null
            this.messages = []
          }
          
          // 刷新未读数
          await this.fetchUnreadCount()
        }
        return response
      } catch (error) {
        console.error('删除对话失败:', error)
        ElMessage.error('删除对话失败')
        throw error
      }
    },
    
    // 清空消息内容
    clearMessageContent() {
      // 这个方法由外部调用，因为 messageContent 不在 store 中
    },
    
    // 清空当前对话（用于返回对话列表）
    clearCurrentConversation() {
      this.selectedConversationId = null
      this.currentConversation = null
      this.messages = []
      // ✅ 移动端修复：清空对话时，确保聊天窗口也隐藏
      this.showChatWindow = false
    },
    
    // 显示聊天窗口（移动端）
    showChat() {
      this.showChatWindow = true
    },
    
    // 隐藏聊天窗口（移动端）
    hideChat() {
      this.showChatWindow = false
    },
    
    // 重置状态
    resetState() {
      this.conversations = []
      this.selectedConversationId = null
      this.currentConversation = null
      this.messages = []
      this.loading = {
        conversations: false,
        messages: false,
        sending: false
      }
      this.totalUnreadCount = 0
      // ✅ 6.4.1 状态同步优化：清空未读数Map
      this.conversationUnreadMap.clear()
      this.showChatWindow = false
      this.wsConnected = false
    },
    
    /**
     * 初始化 WebSocket 连接
     * 在用户登录后调用
     */
    initWebSocket() {
      // 注册新消息处理函数
      wsClient.on('MESSAGE_NEW', (messageData) => {
        this.handleWebSocketMessage(messageData)
      })
      
      // ✅ 注册未读数更新处理函数（支持全局角标更新）
      wsClient.on('UNREAD_COUNT_UPDATE', (updateData) => {
        this.handleUnreadCountUpdate(updateData)
      })
      
      // 监听连接状态
      wsClient.onConnect(() => {
        console.log('WebSocket 已连接')
        this.wsConnected = true
      })
      
      wsClient.onDisconnect(() => {
        console.log('WebSocket 已断开')
        this.wsConnected = false
      })
      
      // ✅ 监听重连成功事件，刷新数据
      wsClient.onReconnected(() => {
        console.log('WebSocket 重连成功，数据已自动刷新')
        // fetchMissedMessages 已经在 websocket.js 中调用，这里可以显示提示
      })
      
      // 建立连接
      wsClient.connect()
    },
    
    /**
     * 处理 WebSocket 接收到的消息
     * 实时更新对话列表、消息列表和未读数
     */
    async handleWebSocketMessage(messageData) {
      // 获取当前用户ID（使用动态导入避免循环依赖）
      let currentUserId = null
      try {
        const { useUserStore } = await import('./user')
        const userStore = useUserStore()
        currentUserId = userStore.user?.userId
      } catch (err) {
        console.error('获取用户 store 失败:', err)
        return
      }
      
      if (!currentUserId) {
        console.warn('收到消息但用户未登录，忽略')
        return
      }
      
      // ✅ 检查是否真的在消息页面（避免切换到其他页面后仍自动标记已读）
      let isOnMessagesPage = false
      if (typeof window !== 'undefined') {
        isOnMessagesPage = window.location.pathname.startsWith('/messages')
      }
      
      // ✅ 更新响应式数据，触发 UI 自动更新
      const message = {
        messageId: messageData.messageId,
        conversationId: messageData.conversationId,
        senderId: messageData.senderId,
        receiverId: messageData.receiverId,
        messageType: messageData.messageType || 'TEXT',
        content: messageData.content || '',
        commodityId: messageData.commodityId || null,
        orderId: messageData.orderId || null,
        senderNickname: messageData.senderNickname || '',
        senderAvatar: messageData.senderAvatar || '',
        isRead: messageData.isRead || false,
        // 判断是否是当前用户发送的消息
        isMine: messageData.senderId === currentUserId,
        // createdAt 可能是字符串，需要转换
        createdAt: messageData.createdAt || new Date().toISOString()
      }
      
      // 1. 更新对话列表
      let conversation = this.conversations.find(
        c => c.conversationId === message.conversationId
      )
      
      if (conversation) {
        // ✅ 直接更新响应式对象，自动触发 UI 更新
        conversation.lastMessageContent = message.content || '新消息'
        conversation.lastMessageTime = message.createdAt
        
        // ✅ 移除直接更新未读数的逻辑，统一通过 UNREAD_COUNT_UPDATE 事件更新
        // 未读数更新由后端推送的 UNREAD_COUNT_UPDATE 事件统一处理
        
        // ✅ 将对话移到列表顶部（按最后消息时间排序）
        // 使用 Vue 的响应式特性，重新排序数组
        const index = this.conversations.indexOf(conversation)
        if (index > 0) {
          // 使用 splice 和 unshift 确保响应式更新
          this.conversations.splice(index, 1)
          this.conversations.unshift(conversation)
        }
      } else {
        // 如果对话不在列表中，重新加载对话列表
        console.log('收到新消息，但对话不在列表中，重新加载对话列表, conversationId=', message.conversationId)
        this.fetchConversations().catch(err => {
          console.error('重新加载对话列表失败:', err)
        })
      }
      
      // 2. 如果消息属于当前选中的对话，添加到消息列表
      if (message.conversationId === this.selectedConversationId) {
        // ✅ 直接添加到响应式数组，自动触发 UI 更新
        this.messages.push(message)
        
        // ✅ 如果正在查看此对话且确实在消息页面，延迟标记为已读
        // 这样可以避免时序问题：先收到未读数+1，立即标记已读又收到未读数=0
        // ✅ 同时检查是否在消息页面，避免切换到其他页面后仍自动标记已读
        if (message.receiverId === currentUserId && isOnMessagesPage) {
          // 使用 setTimeout 延迟标记已读，确保后端的未读数更新事件先处理完成
          setTimeout(() => {
            // 再次检查：对话是否仍然被选中且仍在消息页面（用户可能已经切换了页面或对话）
            if (typeof window !== 'undefined' && window.location.pathname.startsWith('/messages') &&
                this.selectedConversationId === message.conversationId) {
          this.markAsRead(message.conversationId).catch(err => {
            console.error('标记消息已读失败:', err)
          })
            }
          }, 300) // 延迟300ms，足够后端推送未读数更新事件
        }
        
        // 触发滚动到底部（通过事件通知，由组件处理）
        // 注意：由于这是在 store 中，无法直接访问 DOM
        // 滚动逻辑在 Messages.vue 中通过 watch messages 实现
      }
      
      // 3. 显示通知（只在消息不属于当前对话时）
      if (message.conversationId !== this.selectedConversationId && message.receiverId === currentUserId) {
        const preview = message.content?.substring(0, 20) || '新消息'
        ElMessage.info({
          message: `收到新消息：${preview}${message.content?.length > 20 ? '...' : ''}`,
          duration: 3000
        })
      }
    },
    
    /**
     * 处理未读数更新事件
     * 统一管理所有未读数更新（新消息增加未读、标记已读减少未读）
     * 用于全局角标更新（包括不在消息页面的情况）
     * 
     * ✅ 6.4.1 状态同步优化：使用统一的updateUnreadCount方法
     * 确保WebSocket更新和API更新不会冲突，所有更新都通过同一路径
     * 
     * 更新两种未读数：
     * 1. totalUnreadCount: 所有对话的未读数之和（用于顶部栏）
     * 2. conversation.unreadCount: 单个对话的未读数（用于侧边栏）
     */
    handleUnreadCountUpdate(updateData) {
      console.log('收到未读数更新:', updateData)
      
      // ✅ 6.4.1 状态同步优化：使用统一方法更新，避免冲突
      if (updateData.conversationId && updateData.conversationUnreadCount !== undefined) {
        // 更新指定对话的未读数（后端推送了单个对话未读数）
        const newCount = updateData.conversationUnreadCount || 0
        const currentCount = this.conversationUnreadMap.get(updateData.conversationId) || 0
        
        // ✅ 重要：只有在新的未读数大于等于当前值，或者为0（标记已读）时才更新
        // 避免旧的标记已读事件覆盖新的未读数（时序问题）
        if (newCount === 0 || newCount >= currentCount) {
          this.updateUnreadCount(updateData.conversationId, newCount, true)
        }
      } else if (updateData.unreadCount !== undefined) {
        // 只有总未读数，没有单个对话未读数
        // 如果总未读数为0，可以推断所有对话都已读
        if (updateData.unreadCount === 0) {
          // 将所有对话的未读数设为0
          this.conversationUnreadMap.forEach((count, conversationId) => {
            if (count > 0) {
              this.updateUnreadCount(conversationId, 0, false)
            }
          })
          this.totalUnreadCount = 0
        } else {
          // 有未读数但不知道具体分布，需要刷新对话列表获取详细信息
          // 但为了性能，延迟刷新，避免频繁请求
          if (this.conversations.length > 0) {
            setTimeout(() => {
              this.fetchConversations().catch(err => {
                console.error('刷新对话列表失败:', err)
              })
            }, 500)
          }
        }
      }
    },
    
    /**
     * 断开 WebSocket 连接
     * 在用户登出时调用
     */
    disconnectWebSocket() {
      // 移除所有消息处理函数
      wsClient.off('MESSAGE_NEW')
      wsClient.off('UNREAD_COUNT_UPDATE')
      
      wsClient.disconnect()
      this.wsConnected = false
    }
  }
})

