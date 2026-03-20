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
import { getProfileFromStorage, saveProfileToStorage, initProfileCache } from '../utils/profileCache'
import ConversationList from '../components/messages/ConversationList.vue'
import ChatWindow from '../components/messages/ChatWindow.vue'

export default {
  name: 'MessagesPage',
  components: {
    ChatDotRound,
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
    // ⚠️ messagesListRef 在 ChatWindow 组件内部，这里不需要定义
    // const messagesListRef = ref(null)
    
    // ✅ 提供增量更新结果给子组件（对话框）
    const incrementalUpdateResult = reactive({
      commodities: [],
      orders: [],
      timestamp: null
    })
    provide('incrementalUpdateResult', incrementalUpdateResult)
    
    // ✅ Profile缓存：使用localStorage存储，避免内存占用过大
    // 初始化缓存（清理过期数据）
    initProfileCache()
    
    /**
     * ✅ 缓存对话双方的profile信息（降级机制：优先使用后端返回的数据，其次是localStorage，最后才查询数据库）
     * 在进入聊天框时调用，后续订单直接使用缓存
     */
    const cacheConversationProfiles = async (conversation) => {
      if (!conversation || !conversation.conversationId) {
        console.warn('⚠️ cacheConversationProfiles: 对话数据无效', conversation)
        return
      }
      
      const conversationId = conversation.conversationId
      const currentUserId = userStore.user?.userId
      const otherUserId = conversation.otherUserId
      
      if (!currentUserId) {
        console.warn('⚠️ cacheConversationProfiles: 当前用户未登录')
        return
      }
      
      if (!otherUserId) {
        console.warn('⚠️ cacheConversationProfiles: 对话对方用户ID缺失')
        return
      }
      
      // ✅ 降级机制1：优先使用后端返回的profile数据（如果后端已经返回了otherUser的profile信息）
      // 后端返回的conversation对象中已经包含了otherUserNickname和otherUserAvatar
      const otherUserProfileFromBackend = conversation.otherUserNickname || conversation.otherUserAvatar
        ? {
            userId: otherUserId,
            nickname: conversation.otherUserNickname || '',
            avatar: conversation.otherUserAvatar || ''
          }
        : null
      
      // ✅ 降级机制2：检查localStorage缓存
      const currentUserProfileFromCache = getProfileFromStorage(currentUserId)
      const otherUserProfileFromCache = getProfileFromStorage(otherUserId)
      
      // 如果都有缓存（或后端已提供），跳过查询
      if (currentUserProfileFromCache && (otherUserProfileFromCache || otherUserProfileFromBackend)) {
        // ✅ 如果后端提供了otherUser的profile但localStorage中没有，保存到localStorage
        if (otherUserProfileFromBackend && !otherUserProfileFromCache) {
          saveProfileToStorage(otherUserId, otherUserProfileFromBackend)
          console.log(`💾 cacheConversationProfiles: 将后端返回的otherUser profile保存到localStorage: ${otherUserId}`)
        }
        console.log(`✅ cacheConversationProfiles: 对话 ${conversationId} 的profile已缓存（localStorage或后端数据），跳过查询`)
        return
      }
      
      console.log(`🔄 cacheConversationProfiles: 开始缓存对话 ${conversationId} 的profile`, {
        currentUserId,
        otherUserId,
        currentUserCached: !!currentUserProfileFromCache,
        otherUserCached: !!otherUserProfileFromCache,
        otherUserFromBackend: !!otherUserProfileFromBackend
      })
      
      // ✅ 降级机制3：对于缺失的profile，优先从localStorage获取，不存在或不可访问时才查询数据库
      const userIds = [currentUserId, otherUserId].filter(Boolean)
      const { profileAPI } = await import('../api')
      
      const profilePromises = userIds.map(async (userId) => {
        // ✅ 优先检查localStorage缓存
        let cachedProfile = getProfileFromStorage(userId)
        
        // ✅ 如果是otherUser且后端已提供数据，使用后端数据
        if (userId === otherUserId && otherUserProfileFromBackend && !cachedProfile) {
          cachedProfile = otherUserProfileFromBackend
          // 保存到localStorage
          saveProfileToStorage(userId, cachedProfile)
          console.log(`✅ cacheConversationProfiles: 使用后端返回的profile并保存到localStorage: ${userId}`)
          return
        }
        
        // 如果localStorage中有且未过期，跳过数据库查询
        if (cachedProfile) {
          console.log(`⏭️ cacheConversationProfiles: 用户 ${userId} 的profile已在localStorage中，跳过数据库查询`)
          return
        }
        
        // ✅ 降级机制4：localStorage不存在或不可访问，才查询数据库
        try {
          console.log(`🔍 cacheConversationProfiles: localStorage中不存在，查询数据库获取用户profile: ${userId}`)
          const response = await profileAPI.getUser(userId)
          if (response.success && response.data) {
            // 保存到localStorage
            saveProfileToStorage(userId, response.data)
            console.log(`✅ cacheConversationProfiles: 用户profile查询成功并保存到localStorage: ${userId}`)
          } else {
            console.warn(`⚠️ cacheConversationProfiles: 用户profile查询失败: ${userId}`, response)
          }
        } catch (error) {
          console.error(`❌ cacheConversationProfiles: 获取用户 ${userId} 的profile失败:`, error)
        }
      })
      
      await Promise.all(profilePromises)
      console.log(`✅ cacheConversationProfiles: 对话 ${conversationId} 的profile缓存完成`)
    }
    
    /**
     * ✅ 从localStorage缓存获取profile信息（优先使用缓存，缓存中没有则查询）
     * @param {string} userId - 用户ID
     * @param {string} conversationId - 对话ID（可选，用于日志记录）
     * @returns {Promise<Object|null>} profile信息，如果不存在返回null
     */
    const getProfileFromCache = async (userId, conversationId = null) => {
      if (!userId) return null
      
      // 从localStorage获取缓存
      const cachedProfile = getProfileFromStorage(userId)
      
      if (cachedProfile) {
        console.log(`✅ getProfileFromCache: 从localStorage获取用户 ${userId} 的profile${conversationId ? ` (对话: ${conversationId})` : ''}`)
        return cachedProfile
      }
      
      // 缓存中没有，返回null（由调用者决定是否查询）
      console.log(`⚠️ getProfileFromCache: 用户 ${userId} 的profile未在localStorage中${conversationId ? ` (对话: ${conversationId})` : ''}`)
      return null
    }
    
    // ✅ 提供profile缓存访问方法给子组件（对话框）- 在函数定义后提供
    const profileCacheProvider = {
      getProfileFromCache,
      // 不再提供 conversationProfileCache，因为已改用localStorage
      // 如果子组件需要，可以通过 getProfileFromCache 访问
    }
    provide('profileCacheProvider', profileCacheProvider)
    
    // 获取头像URL
    const getAvatarUrl = (avatar) => {
      if (!avatar) return ''
      if (avatar.startsWith('http')) return avatar
      return `http://localhost:8080/api/images/avatars/${avatar}`
    }
    
    // 获取对话列表 - 使用 store
    const fetchConversations = async () => {
      await messageStore.fetchConversations()
      // ✅ 滚动逻辑在 ChatWindow 组件内部处理
    }
    
    // 获取未读数 - 使用 store
    const fetchUnreadCount = async () => {
      await messageStore.fetchUnreadCount()
    }
    
    // 选择对话 - 使用 store
    const selectConversation = async (conversation) => {
      await messageStore.selectConversation(conversation)
      
      // ✅ 缓存对话双方的profile信息
      await cacheConversationProfiles(conversation)
      
      // 移动端：显示聊天窗口
      if (isMobile.value) {
        messageStore.showChat()
      }
      
      // ✅ 滚动逻辑在 ChatWindow 组件内部处理（通过 watch currentConversation）
      // 这里不需要手动滚动
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
          // ✅ 滚动逻辑在 ChatWindow 组件内部处理（通过 watch messages）
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
    
    // ✅ 滚动逻辑在 ChatWindow 组件内部处理，因为滚动容器在该组件中
    // 不需要在这里定义滚动函数
    
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
    const updateCommoditiesAndOrders = async (commodities = [], orders = []) => {
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
            sellerId: o.sellerId,
            buyerId: o.buyerId,
            // 注意：如果后端返回了时间戳，这里也可以记录
          })
        })
        console.groupEnd()
      }
      
      // ✅ 订单数据已经包含profile字段（sellerNickname, sellerAvatar, buyerNickname, buyerAvatar）
      // 不再需要单独查询profile，直接使用后端返回的字段
      
      // ✅ 增量更新：只更新已存在的消息中的商品和订单数据
      let updatedCount = 0
      const updatedMessages = []
      
      // ✅ 调试：检查订单匹配情况
      const messageOrderIds = messages.value
        .filter(m => m.orderId)
        .map(m => m.orderId)
      const uniqueMessageOrderIds = [...new Set(messageOrderIds)]
      
      console.log(`🔍 updateCommoditiesAndOrders: 开始遍历${messages.value.length}条消息，查找需要更新的订单`, {
        orderMapSize: orderMap.size,
        orderMapKeys: Array.from(orderMap.keys()),
        messageOrderIdsCount: uniqueMessageOrderIds.length,
        messageOrderIds: uniqueMessageOrderIds,
        hasMatchingOrders: Array.from(orderMap.keys()).some(orderId => uniqueMessageOrderIds.includes(orderId)),
        orderMapOrders: Array.from(orderMap.values()).map(o => ({
          orderId: o.orderId,
          sellerId: o.sellerId,
          buyerId: o.buyerId
        }))
      })
      
      messages.value.forEach((message, messageIndex) => {
        let messageUpdated = false
        const updateInfo = {
          messageIndex,
          messageId: message.messageId,
          updates: []
        }
        
        // ✅ 调试：检查是否有匹配的订单
        if (message.orderId) {
          const orderInMap = orderMap.has(message.orderId)
          console.log(`🔍 updateCommoditiesAndOrders: 消息 ${messageIndex} (messageId=${message.messageId}, orderId=${message.orderId}):`, {
            orderId: message.orderId,
            orderInMap,
            hasOrder: !!message.order,
            orderStatus: message.order?.orderStatus
          })
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
          console.log(`✅ updateCommoditiesAndOrders: 找到需要更新的订单消息 (messageId=${message.messageId}, orderId=${message.orderId})`)
          
          const oldOrder = message.order ? { ...message.order } : null
          const newOrder = orderMap.get(message.orderId)
          
          console.log(`🔍 updateCommoditiesAndOrders: 订单数据详情:`, {
            newOrderFromMap: newOrder,
            oldOrder: oldOrder
          })
          
          // ✅ 订单数据已经包含profile字段，直接使用（后端已返回sellerNickname, sellerAvatar, buyerNickname, buyerAvatar）
          const orderWithProfile = {
            ...newOrder
            // sellerNickname, sellerAvatar, buyerNickname, buyerAvatar 已经由后端返回
          }
          
          console.log(`🔄 updateCommoditiesAndOrders: 更新订单数据 (messageId=${message.messageId}, orderId=${newOrder.orderId}):`, {
            sellerNickname: orderWithProfile.sellerNickname,
            buyerNickname: orderWithProfile.buyerNickname,
            sellerAvatar: orderWithProfile.sellerAvatar,
            buyerAvatar: orderWithProfile.buyerAvatar,
            oldOrderExists: !!message.order,
            oldOrderHasProfile: !!(message.order?.sellerNickname || message.order?.buyerNickname),
            orderWithProfileFull: orderWithProfile
          })
          
          // ✅ 更新订单数据（包含profile信息）- 使用新对象引用确保触发响应式更新
          const oldOrderRef = message.order
          message.order = orderWithProfile
          console.log(`✅ updateCommoditiesAndOrders: 订单对象已更新 (messageId=${message.messageId})`, {
            orderReferenceChanged: oldOrderRef !== message.order,
            messageOrderNow: {
              orderId: message.order?.orderId,
              sellerId: message.order?.sellerId,
              buyerId: message.order?.buyerId,
              sellerNickname: message.order?.sellerNickname,
              sellerAvatar: message.order?.sellerAvatar,
              buyerNickname: message.order?.buyerNickname,
              buyerAvatar: message.order?.buyerAvatar
            },
            shouldTriggerWatch: true
          })
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
      
      console.log(`📊 updateCommoditiesAndOrders: 遍历完成，更新了${updatedCount}条消息`, {
        updatedMessagesCount: updatedMessages.length,
        messagesTotal: messages.value.length,
        orderMapKeys: Array.from(orderMap.keys()),
        orderMapSize: orderMap.size
      })
      
      // ✅ 处理未匹配的订单：检查所有有orderId但没有订单数据的消息
      // 这样可以处理新消息到达时订单数据已经准备好的情况
      const messagesWithoutOrderData = messages.value.filter(msg => {
        return msg.orderId && !msg.order && orderMap.has(msg.orderId)
      })
      
      if (messagesWithoutOrderData.length > 0) {
        console.log(`🔄 updateCommoditiesAndOrders: 发现${messagesWithoutOrderData.length}条消息有orderId但缺少订单数据，立即填充`, {
          messageOrderIds: messagesWithoutOrderData.map(m => m.orderId)
        })
        
        messagesWithoutOrderData.forEach((message) => {
          const newOrder = orderMap.get(message.orderId)
          if (newOrder) {
            console.log(`✅ updateCommoditiesAndOrders: 为消息 ${message.messageId} 填充订单数据 (orderId=${message.orderId})`)
            
            // ✅ 订单数据已经包含profile字段，直接使用（后端已返回sellerNickname, sellerAvatar, buyerNickname, buyerAvatar）
            const orderWithProfile = {
              ...newOrder
              // sellerNickname, sellerAvatar, buyerNickname, buyerAvatar 已经由后端返回
            }
            
            message.order = orderWithProfile
            updatedCount++
            
            console.log(`✅ updateCommoditiesAndOrders: 订单数据填充完成 (messageId=${message.messageId}, orderId=${message.orderId}):`, {
              sellerNickname: orderWithProfile.sellerNickname,
              buyerNickname: orderWithProfile.buyerNickname,
              sellerAvatar: orderWithProfile.sellerAvatar,
              buyerAvatar: orderWithProfile.buyerAvatar
            })
          }
        })
      }
      
      // ✅ 详细日志：记录更新的消息详情
      if (updatedCount > 0) {
        console.group(`✅ 增量更新完成: 更新了${updatedCount}个消息的商品/订单数据`)
        updatedMessages.forEach(updateInfo => {
          console.log(`消息 ${updateInfo.messageIndex + 1} (${updateInfo.messageId}):`, updateInfo.updates)
        })
        console.groupEnd()
      } else {
        console.warn(`⚠️ updateCommoditiesAndOrders: 没有找到匹配的消息进行更新`, {
          orderMapKeys: Array.from(orderMap.keys()),
          messageOrderIds: uniqueMessageOrderIds,
          unmatchedOrderIds: Array.from(orderMap.keys()).filter(orderId => !uniqueMessageOrderIds.includes(orderId))
        })
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
        // 辅助函数：将UTC时间转换为GMT+8时间字符串
        const toGMT8String = (utcIsoString) => {
          const date = new Date(utcIsoString)
          // GMT+8 = UTC+8小时
          const gmt8Date = new Date(date.getTime() + 8 * 60 * 60 * 1000)
          return gmt8Date.toISOString().replace('Z', '+08:00').replace(/\.\d{3}/, '')
        }
        
        console.group(`🔄 ${force ? '强制' : '定期'}增量轮询开始`)
        console.log('📅 轮询时间戳信息:', {
          lastPollTimestamp: lastTimestamp,
          lastPollTimestampUTC: new Date(lastTimestamp).toISOString(),
          lastPollTimestampGMT8: toGMT8String(lastTimestamp),
          currentTimeUTC: pollStartTime,
          currentTimeGMT8: toGMT8String(pollStartTime),
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
            
            // 增量更新前端数据（await异步函数）
            const updatedCount = await updateCommoditiesAndOrders(commodities, orders)
            
            // ✅ 检查返回的订单/商品ID，如果前端消息中没有对应的订单/商品对象，就批量查询获取完整数据
            const orderIdsFromPoll = orders.map(o => o.orderId)
            const commodityIdsFromPoll = commodities.map(c => c.commodityId)
            
            // 查找消息中包含这些订单/商品ID，但没有对应的订单/商品对象的消息
            const messagesNeedingOrderData = messages.value.filter(msg => 
              msg.orderId && orderIdsFromPoll.includes(msg.orderId) && !msg.order
            )
            const messagesNeedingCommodityData = messages.value.filter(msg => 
              msg.commodityId && commodityIdsFromPoll.includes(msg.commodityId) && !msg.commodity
            )
            
            if (messagesNeedingOrderData.length > 0 || messagesNeedingCommodityData.length > 0) {
              console.group('📥 增量轮询返回的订单/商品在前端消息中未存储，批量查询获取完整数据')
              console.log('需要订单数据的消息数:', messagesNeedingOrderData.length)
              console.log('需要商品数据的消息数:', messagesNeedingCommodityData.length)
              console.log('订单ID:', orderIdsFromPoll)
              console.log('商品ID:', commodityIdsFromPoll)
              console.groupEnd()
              
              // 批量查询获取完整数据
              await enrichMessages([...messagesNeedingOrderData, ...messagesNeedingCommodityData])
            }
            
            // ✅ 通知子组件（对话框）增量更新结果
            incrementalUpdateResult.commodities = commodities
            incrementalUpdateResult.orders = orders
            incrementalUpdateResult.timestamp = Date.now()
            
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
      
      // ✅ 订单数据已经包含profile字段（sellerNickname, sellerAvatar, buyerNickname, buyerAvatar）
      // 不再需要单独查询profile，直接使用后端返回的字段
      
      // 4. 填充消息的商品和订单信息（订单包含profile信息）
      messageList.forEach(message => {
        if (message.commodityId && !message.commodity) {
          message.commodity = commodityMap.get(message.commodityId)
        }
        if (message.orderId && !message.order) {
          const order = orderMap.get(message.orderId)
          if (order) {
            // ✅ 订单数据已经包含profile字段，直接使用（后端已返回sellerNickname, sellerAvatar, buyerNickname, buyerAvatar）
            const orderWithProfile = {
              ...order
              // sellerNickname, sellerAvatar, buyerNickname, buyerAvatar 已经由后端返回
            }
            
            console.log(`📝 enrichMessages: 为消息填充订单数据 (messageId=${message.messageId}, orderId=${order.orderId}):`, {
              sellerNickname: orderWithProfile.sellerNickname,
              buyerNickname: orderWithProfile.buyerNickname,
              sellerAvatar: orderWithProfile.sellerAvatar,
              buyerAvatar: orderWithProfile.buyerAvatar,
              orderWithProfileFull: orderWithProfile
            })
            
            message.order = orderWithProfile
            console.log(`✅ enrichMessages: 订单对象已赋值 (messageId=${message.messageId})`, {
              messageOrderNow: {
                orderId: message.order?.orderId,
                sellerId: message.order?.sellerId,
                buyerId: message.order?.buyerId,
                sellerNickname: message.order?.sellerNickname,
                sellerAvatar: message.order?.sellerAvatar,
                buyerNickname: message.order?.buyerNickname,
                buyerAvatar: message.order?.buyerAvatar
              }
            })
          }
        }
      })
      
      return messageList
    }
    
    // ✅ 定期增量轮询更新消息中的商品和订单状态
    const POLL_INTERVAL = 30000
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
    
    // ✅ 保存上一次的消息长度（用于检测新消息）
    const previousMessagesLength = ref(0)
    
    // 监听消息变化，自动获取详细信息
    watch(() => messages.value, async (newMessages, oldMessages) => {
      if (newMessages && newMessages.length > 0) {
        // ✅ 简化逻辑：直接使用批量查询填充新消息中的订单/商品数据
        // 增量轮询本身已经会处理未存储的数据（在incrementalPoll函数中）
        // 使用 ref 保存的长度，而不是依赖 oldMessages（因为响应式更新时可能不准确）
        const oldLength = previousMessagesLength.value
        const newLength = newMessages.length
        
        // ✅ 添加调试日志
        console.log(`🔍 Messages.vue watch触发: oldLength=${oldLength}, newLength=${newLength}, oldMessagesLength=${oldMessages?.length || 'undefined'}`)
        
        // 如果消息数量增加（新消息到达），使用批量查询填充未加载的数据
        if (newLength > oldLength) {
          // 直接使用批量查询填充新消息中的订单/商品数据
                await enrichMessages(newMessages)
          } else {
          // 消息数量未增加（可能是更新现有消息），使用批量查询填充未加载的数据
          await enrichMessages(newMessages)
        }
        
        // ✅ 更新上一次的长度（确保下次能正确检测）
        if (newLength !== previousMessagesLength.value) {
          previousMessagesLength.value = newLength
        }
        
        // ⚠️ 滚动逻辑在 ChatWindow 组件内部处理
        // 这里不需要处理滚动，因为 ChatWindow 组件会监听 messages 变化并自动滚动
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

