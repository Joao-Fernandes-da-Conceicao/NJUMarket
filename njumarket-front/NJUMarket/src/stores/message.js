import { defineStore } from 'pinia'
import { contactAPI } from '../api'
import { ElMessage } from 'element-plus'

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
    // 未读消息总数（从后端 API /contact/unread-count 获取）
    // 后端已更新为使用 user_1_count/user_2_count 字段
    totalUnreadCount: 0,
    // 移动端聊天窗口显示状态
    showChatWindow: false
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
          this.totalUnreadCount = response.data || 0
        }
        return response
      } catch (error) {
        console.error('获取未读数失败:', error)
      }
    },
    
    // 选择对话
    async selectConversation(conversation) {
      this.selectedConversationId = conversation.conversationId
      this.currentConversation = conversation
      
      // 获取消息
      await this.fetchMessages(conversation.conversationId)
      
      // 如果有未读消息，标记为已读
      // conversation.unreadCount 来自后端 ConversationDTO，已更新为使用新的字段名
      if (conversation.unreadCount > 0) {
        await this.markAsRead(conversation.conversationId)
        conversation.unreadCount = 0
        await this.fetchUnreadCount()
      }
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
      try {
        const response = await contactAPI.markAsRead(conversationId)
        if (response.success) {
          // 更新对话的未读数
          const conv = this.conversations.find(c => c.conversationId === conversationId)
          if (conv) {
            conv.unreadCount = 0
          }
        }
        return response
      } catch (error) {
        console.error('标记已读失败:', error)
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
      this.showChatWindow = false
    }
  }
})

