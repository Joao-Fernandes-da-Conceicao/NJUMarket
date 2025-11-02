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
          <!-- ✅ 移动端修复：移动端只在选中对话且showChatWindow为true时显示，桌面端只在选中对话时显示 -->
          <div class="chat-panel" :class="{ 
            'hidden': isMobile ? (!showChatWindow || !selectedConversationId) : (!selectedConversationId)
          }">
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
import { ref, computed, onMounted, onUnmounted, watch, nextTick, provide, reactive } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '../stores/user'
import { useMessageStore } from '../stores/message'
import { ElMessage } from 'element-plus'
import { ChatDotRound } from '@element-plus/icons-vue'
import { formatTime } from '../utils/formatUtils'
import { isMobile as globalIsMobile, detectMobile } from '../config/responsive'
import { commodityAPI, orderAPI, chatAPI } from '../api'
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
    
    // ✅ 修复桌面端错误：创建计算属性来访问 globalIsMobile
    const isMobile = computed(() => globalIsMobile.value)
    
    const messageContent = ref('')
    const messagesListRef = ref(null)
    
    // ✅ 提供增量更新结果给子组件（对话框）
    const incrementalUpdateResult = reactive({
      commodities: [],
      orders: [],
      timestamp: null
    })
    provide('incrementalUpdateResult', incrementalUpdateResult)
    
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
      if (isMobile.value) {
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
    
    // ✅ 时间戳管理（localStorage）
    const LAST_POLL_TIMESTAMP_KEY = 'chat_last_poll_timestamp'
    
    const getLastPollTimestamp = () => {
      const timestamp = localStorage.getItem(LAST_POLL_TIMESTAMP_KEY)
      if (timestamp) {
        return timestamp
      }
      // 如果没有，返回当前时间（首次查询）
      return new Date().toISOString()
    }
    
    const updateLastPollTimestamp = () => {
      localStorage.setItem(LAST_POLL_TIMESTAMP_KEY, new Date().toISOString())
    }
    
    // ✅ 增量更新商品和订单数据（仅更新已存在的消息）
    const updateCommoditiesAndOrders = (commodities = [], orders = []) => {
      // 建立 Map
      const commodityMap = new Map(commodities.map(c => [c.commodityId, c]))
      const orderMap = new Map(orders.map(o => [o.orderId, o]))
      
      // ✅ 详细日志：记录接收到的变更数据
      if (commodities.length > 0) {
        console.group('📦 接收到的商品变更数据')
        commodities.forEach((c, index) => {
          console.log(`商品 ${index + 1}:`, {
            commodityId: c.commodityId,
            title: c.title,
            price: c.price,
            status: c.commodityStatus,
            // 注意：如果后端返回了时间戳，这里也可以记录
          })
        })
        console.groupEnd()
      }
      
      if (orders.length > 0) {
        console.group('📋 接收到的订单变更数据')
        orders.forEach((o, index) => {
          console.log(`订单 ${index + 1}:`, {
            orderId: o.orderId,
            orderStatus: o.orderStatus,
            payAmount: o.payAmount,
            // 注意：如果后端返回了时间戳，这里也可以记录
          })
        })
        console.groupEnd()
      }
      
      // ✅ 增量更新：只更新已存在的消息中的商品和订单数据
      let updatedCount = 0
      const updatedMessages = []
      
      messages.value.forEach((message, messageIndex) => {
        let messageUpdated = false
        const updateInfo = {
          messageIndex,
          messageId: message.messageId,
          updates: []
        }
        
        if (message.commodityId && commodityMap.has(message.commodityId)) {
          const oldCommodity = message.commodity ? { ...message.commodity } : null
          const newCommodity = commodityMap.get(message.commodityId)
          
          // 更新商品数据（覆盖旧数据）
          message.commodity = newCommodity
          updatedCount++
          messageUpdated = true
          
          updateInfo.updates.push({
            type: 'commodity',
            commodityId: message.commodityId,
            oldPrice: oldCommodity?.price,
            newPrice: newCommodity?.price,
            oldStatus: oldCommodity?.commodityStatus,
            newStatus: newCommodity?.commodityStatus,
            isNew: !oldCommodity, // 是否是首次加载
          })
        }
        
        if (message.orderId && orderMap.has(message.orderId)) {
          const oldOrder = message.order ? { ...message.order } : null
          const newOrder = orderMap.get(message.orderId)
          
          // 更新订单数据（覆盖旧数据）
          message.order = newOrder
          updatedCount++
          messageUpdated = true
          
          updateInfo.updates.push({
            type: 'order',
            orderId: message.orderId,
            oldStatus: oldOrder?.orderStatus,
            newStatus: newOrder?.orderStatus,
            oldPayAmount: oldOrder?.payAmount,
            newPayAmount: newOrder?.payAmount,
            isNew: !oldOrder, // 是否是首次加载
          })
        }
        
        if (messageUpdated) {
          updatedMessages.push(updateInfo)
        }
      })
      
      // ✅ 详细日志：记录更新的消息详情
      if (updatedCount > 0) {
        console.group(`✅ 增量更新完成: 更新了${updatedCount}个消息的商品/订单数据`)
        updatedMessages.forEach(updateInfo => {
          console.log(`消息 ${updateInfo.messageIndex + 1} (${updateInfo.messageId}):`, updateInfo.updates)
        })
        console.groupEnd()
      }
      // 注意：如果 updatedCount 为 0，说明变更数据不匹配当前消息，这是正常情况
      // （可能是其他对话的消息，或者变更数据已过期）
      
      return updatedCount
    }
    
    // ✅ 增量轮询：获取变更并更新
    const incrementalPoll = async (force = false) => {
          try {
        const lastTimestamp = getLastPollTimestamp()
        const pollStartTime = new Date().toISOString()
        
        // ✅ 详细日志：轮询开始信息
        console.group(`🔄 ${force ? '强制' : '定期'}增量轮询开始`)
        console.log('📅 轮询时间戳信息:', {
          lastPollTimestamp: lastTimestamp,
          lastPollTimestampDate: new Date(lastTimestamp).toISOString(),
          currentTime: pollStartTime,
          timeDifference: Math.round((new Date(pollStartTime) - new Date(lastTimestamp)) / 1000) + '秒',
        })
        console.log('📊 当前消息状态:', {
          totalMessages: messages.value?.length || 0,
          messagesWithCommodity: messages.value?.filter(m => m.commodityId)?.length || 0,
          messagesWithOrder: messages.value?.filter(m => m.orderId)?.length || 0,
          commodityIds: [...new Set(messages.value?.filter(m => m.commodityId).map(m => m.commodityId) || [])],
          orderIds: [...new Set(messages.value?.filter(m => m.orderId).map(m => m.orderId) || [])],
        })
        console.groupEnd()
        
        const response = await chatAPI.getIncrementalUpdate(lastTimestamp)
        
        if (response.success && response.data) {
          const { commodities = [], orders = [] } = response.data
          
          // ✅ 详细日志：API响应信息
          console.group(`📥 API响应: ${commodities.length}个商品, ${orders.length}个订单`)
          console.log('完整响应数据:', response.data)
          console.groupEnd()
          
          if (commodities.length > 0 || orders.length > 0) {
            console.log(`📦 增量轮询获取到变更: 商品${commodities.length}个, 订单${orders.length}个`)
            
            // 增量更新前端数据
            const updatedCount = updateCommoditiesAndOrders(commodities, orders)
            
            // ✅ 通知子组件（对话框）增量更新结果
            incrementalUpdateResult.commodities = commodities
            incrementalUpdateResult.orders = orders
            incrementalUpdateResult.timestamp = Date.now()
            
            // ✅ 如果强制轮询且找不到新数据，可能是时间戳问题
            if (force && updatedCount === 0) {
              console.warn('⚠️ 强制轮询未找到新数据，可能需要全量查询')
              // 可以选择全量查询或提示用户
            }
            
            // ✅ 详细日志：更新前的时间戳状态
            const beforeUpdateTimestamp = localStorage.getItem(LAST_POLL_TIMESTAMP_KEY)
            
            // 更新轮询时间戳
            updateLastPollTimestamp()
            
            // ✅ 详细日志：更新后的时间戳状态
            const afterUpdateTimestamp = localStorage.getItem(LAST_POLL_TIMESTAMP_KEY)
            
            console.group('⏰ 时间戳更新')
            console.log('更新前:', {
              timestamp: beforeUpdateTimestamp,
              date: beforeUpdateTimestamp ? new Date(beforeUpdateTimestamp).toISOString() : 'N/A',
            })
            console.log('更新后:', {
              timestamp: afterUpdateTimestamp,
              date: afterUpdateTimestamp ? new Date(afterUpdateTimestamp).toISOString() : 'N/A',
            })
            console.log('时间差:', afterUpdateTimestamp && beforeUpdateTimestamp
              ? Math.round((new Date(afterUpdateTimestamp) - new Date(beforeUpdateTimestamp)) / 1000) + '秒'
              : 'N/A')
            console.groupEnd()
            
            return { commodities, orders, updatedCount }
          } else {
            console.log('✅ 增量轮询无新变更')
            // ✅ 即使没有新变更，也清空之前的增量更新结果（避免使用旧数据）
            incrementalUpdateResult.commodities = []
            incrementalUpdateResult.orders = []
            if (!force) {
              // ✅ 详细日志：即使没有变更也更新时间戳的情况
              const beforeUpdateTimestamp = localStorage.getItem(LAST_POLL_TIMESTAMP_KEY)
              updateLastPollTimestamp()
              const afterUpdateTimestamp = localStorage.getItem(LAST_POLL_TIMESTAMP_KEY)
              
              console.log('⏰ 无变更但更新时间戳:', {
                before: beforeUpdateTimestamp,
                after: afterUpdateTimestamp,
              })
            }
            return { commodities: [], orders: [], updatedCount: 0 }
          }
        } else {
          console.error('❌ 增量轮询失败:', {
            errorMsg: response.errorMsg,
            message: response.message,
            fullResponse: response,
          })
          return { commodities: [], orders: [], updatedCount: 0 }
            }
          } catch (error) {
        console.error('❌ 增量轮询异常:', {
          error,
          errorMessage: error.message,
          errorStack: error.stack,
        })
        throw error
      }
    }
    
    // ✅ 优化：批量获取消息的详细信息（商品/订单），避免N+1查询（初始加载时使用）
    const enrichMessages = async (messageList) => {
      if (!messageList || messageList.length === 0) {
        return messageList
      }
      
      // 1. 收集所有需要查询的商品ID和订单ID（去重，且只查询未加载的）
      const commodityIds = [...new Set(messageList
        .filter(m => m.commodityId && !m.commodity)
        .map(m => m.commodityId)
      )]
      
      const orderIds = [...new Set(messageList
        .filter(m => m.orderId && !m.order)
        .map(m => m.orderId)
      )]
      
      // 2. 批量查询（并行执行）
      let commodityMap = new Map()
      let orderMap = new Map()
      
      const [commodityResponse, orderResponse] = await Promise.all([
        commodityIds.length > 0 
          ? commodityAPI.getBatchStatus(commodityIds).catch(err => {
              console.error('批量查询商品状态失败:', err)
              return { success: false, data: [] }
            })
          : Promise.resolve({ success: true, data: [] }),
        orderIds.length > 0
          ? orderAPI.getBatchStatus(orderIds).catch(err => {
              console.error('批量查询订单状态失败:', err)
              return { success: false, data: [] }
            })
          : Promise.resolve({ success: true, data: [] })
      ])
      
      // 3. 建立 Map 用于快速查找
      if (commodityResponse.success && commodityResponse.data) {
        commodityMap = new Map(
          commodityResponse.data.map(c => [c.commodityId, c])
        )
      }
      
      if (orderResponse.success && orderResponse.data) {
        orderMap = new Map(
          orderResponse.data.map(o => [o.orderId, o])
        )
      }
      
      // 4. 填充消息的商品和订单信息
      messageList.forEach(message => {
        if (message.commodityId && !message.commodity) {
          message.commodity = commodityMap.get(message.commodityId)
        }
        if (message.orderId && !message.order) {
          message.order = orderMap.get(message.orderId)
        }
      })
      
      return messageList
    }
    
    // ✅ 定期增量轮询更新消息中的商品和订单状态
    const POLL_INTERVAL = 30000 // 30秒轮询一次
    let pollTimer = null
    let isPolling = false // 防止并发轮询
    
    const startPolling = () => {
      // 清除旧的定时器
      if (pollTimer) {
        clearInterval(pollTimer)
        console.log('🔄 清除旧的轮询定时器')
      }
      
      // ✅ 初始化时间戳（如果首次加载）
      const existingTimestamp = localStorage.getItem(LAST_POLL_TIMESTAMP_KEY)
      if (!existingTimestamp) {
        updateLastPollTimestamp()
        const newTimestamp = localStorage.getItem(LAST_POLL_TIMESTAMP_KEY)
        console.log('🆕 初始化轮询时间戳:', {
          timestamp: newTimestamp,
          date: new Date(newTimestamp).toISOString(),
        })
      } else {
        console.log('📅 使用已存在的时间戳:', {
          timestamp: existingTimestamp,
          date: new Date(existingTimestamp).toISOString(),
        })
      }
      
      console.log(`⏰ 启动定期轮询，间隔: ${POLL_INTERVAL / 1000}秒`)
      
      // 只在消息页面且有消息时才轮询
      pollTimer = setInterval(async () => {
        if (messages.value && messages.value.length > 0 && 
            window.location.pathname.startsWith('/messages') &&
            !isPolling) {
          try {
            isPolling = true
            console.log(`⏱️ [定时器触发] 定期增量轮询：更新商品和订单状态 (${new Date().toISOString()})`)
            await incrementalPoll(false) // 非强制轮询
          } catch (error) {
            console.error('❌ 定期增量轮询失败:', error)
          } finally {
            isPolling = false
          }
        } else {
          // 记录为什么跳过轮询
          const reason = []
          if (!messages.value || messages.value.length === 0) reason.push('无消息')
          if (!window.location.pathname.startsWith('/messages')) reason.push('不在消息页面')
          if (isPolling) reason.push('正在轮询中')
          console.log(`⏸️ 跳过轮询: ${reason.join(', ')}`)
        }
      }, POLL_INTERVAL)
    }
    
    const stopPolling = () => {
      if (pollTimer) {
        clearInterval(pollTimer)
        pollTimer = null
      }
    }
    
    // ✅ 强制立即增量轮询（用于新消息检测）
    const forceIncrementalPoll = async () => {
      if (isPolling) {
        console.warn('⚠️ 正在轮询中，跳过强制轮询', {
          currentTime: new Date().toISOString(),
          isPolling,
        })
        return
      }
      
      try {
        isPolling = true
        console.group('🚀 强制增量轮询：立即查询最新变更')
        console.log('触发时间:', new Date().toISOString())
        console.log('触发原因: 检测到新消息包含未加载的商品/订单')
        console.groupEnd()
        
        const result = await incrementalPoll(true) // 强制轮询
        
        if (result.updatedCount === 0) {
          console.warn('⚠️ 强制轮询未找到新数据，可能需要等待定期轮询', {
            receivedCommodities: result.commodities?.length || 0,
            receivedOrders: result.orders?.length || 0,
            updatedCount: result.updatedCount,
          })
        } else {
          console.log('✅ 强制轮询成功更新数据', {
            updatedCount: result.updatedCount,
            commoditiesReceived: result.commodities?.length || 0,
            ordersReceived: result.orders?.length || 0,
          })
        }
        
        return result
      } catch (error) {
        console.error('❌ 强制增量轮询失败:', {
          error,
          errorMessage: error.message,
          errorStack: error.stack,
          timestamp: new Date().toISOString(),
        })
        throw error
      } finally {
        isPolling = false
      }
    }
    
    // 监听消息变化，自动获取详细信息和滚动到底部
    watch(() => messages.value, async (newMessages, oldMessages) => {
      if (newMessages && newMessages.length > 0) {
        // ✅ 初始加载时：全量查询未加载的商品/订单
        await enrichMessages(newMessages)
        
        // ✅ 检测新消息中是否有商品/订单ID但未加载数据
        const unloadedCommodities = newMessages
          .filter(msg => msg.commodityId && !msg.commodity)
          .map(msg => ({ messageId: msg.messageId, commodityId: msg.commodityId }))
        const unloadedOrders = newMessages
          .filter(msg => msg.orderId && !msg.order)
          .map(msg => ({ messageId: msg.messageId, orderId: msg.orderId }))
        
        const hasNewCommodityOrOrder = unloadedCommodities.length > 0 || unloadedOrders.length > 0
        
        if (hasNewCommodityOrOrder) {
          console.group('🔍 检测到新消息包含未加载的商品/订单，准备强制增量轮询')
          console.log('未加载的商品:', unloadedCommodities)
          console.log('未加载的订单:', unloadedOrders)
          console.log('延迟500ms后执行强制轮询，确保后端变更记录已写入Redis')
          console.groupEnd()
          
          // 延迟一点时间，确保后端变更记录已写入Redis
          setTimeout(() => {
            forceIncrementalPoll().catch(err => {
              console.error('❌ 强制轮询失败，将等待定期轮询:', err)
            })
          }, 500) // 延迟500ms后强制轮询
        }
        
        // ✅ 当有新消息时（通过 WebSocket 接收），滚动到底部
        if (!oldMessages || newMessages.length > oldMessages.length) {
          await nextTick()
          scrollToBottom()
        }
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
      resizeTimer = setTimeout(() => {
        detectMobile()
        // ✅ 移动端修复：窗口大小变化时，如果是移动端且没有选中对话，确保聊天窗口隐藏
        if (isMobile.value && !selectedConversationId.value) {
          messageStore.hideChat()
        }
      }, 150)
    }
    
    onMounted(() => {
      // 初始化响应式检测
      detectMobile()
      
      // ✅ 移动端修复：确保初始状态下聊天窗口隐藏且没有选中对话
      if (isMobile.value) {
        messageStore.clearCurrentConversation()
        messageStore.hideChat()
      }
      
      // 监听窗口大小变化
      window.addEventListener('resize', handleResize)
      
      // ✅ 启动定期轮询
      startPolling()
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
      
      // ✅ 停止定期轮询
      stopPolling()
      
      // ✅ 清空当前选中的对话，避免切换到其他页面后仍自动标记已读
      messageStore.clearCurrentConversation()
    })
    
    // 返回对话列表 - 使用 store
    const backToConversations = () => {
      // ✅ 移动端修复：先清空对话（会触发hideChat），再确保隐藏
      messageStore.clearCurrentConversation()
      messageStore.hideChat()
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
      isMobile,
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

