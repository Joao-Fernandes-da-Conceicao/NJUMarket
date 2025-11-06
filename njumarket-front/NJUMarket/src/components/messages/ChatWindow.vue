<template>
  <div class="chat-window">
    <!-- 聊天头部 -->
    <div class="chat-header">
      <div class="desktop-back-button" @click="$emit('back')">
        <el-icon><ArrowLeft /></el-icon>
      </div>

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
        <UnifiedButton text @click="$emit('view-profile', currentConversation?.otherUserId)">查看主页</UnifiedButton>
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
        <!-- ✅ v1.3.x: 消息操作菜单（点击消息显示） -->
        <div 
          v-if="showMessageMenu && selectedMessage?.messageId === message.messageId"
          class="message-menu"
          :style="menuPosition"
          @click.stop
        >
          <UnifiedButton 
            type="danger" 
            size="small"
            @click="handleDeleteMessage(message)"
            class="delete-button"
          >
            删除
          </UnifiedButton>
        </div>
        
        <template v-if="!message.isMine">
          <div class="message-avatar">
            <el-avatar :size="36" :src="getAvatarUrl(message.senderAvatar)">
              {{ message.senderNickname?.charAt(0) || 'U' }}
            </el-avatar>
          </div>
          <div class="message-content" @click="handleMessageClick($event, message)">
            <div class="message-bubble"><p>{{ message.content }}</p></div>
            <!-- 商品卡片 -->
            <div class="card-wrapper">
              <MessageCommodityCard
                v-if="message.commodityId && message.commodity"
                :commodity="message.commodity"
                @click="handleCommodityClick"
              />
              <!-- 订单卡片 -->
              <MessageOrderCard
                v-if="message.orderId && message.order"
                :order="message.order"
                @click="handleOrderClick"
              />
            </div>
            <div class="message-meta-other">
              <span class="message-time">{{ formatTime(message.createdAt) }}</span>
              <span v-if="message.isRead" class="read-status">已读</span>
            </div>
          </div>
        </template>
        <template v-else>
          <div class="message-content self" @click="handleMessageClick($event, message)">
            <div class="message-bubble self"><p>{{ message.content }}</p></div>
            <!-- 商品卡片 -->
            <div class="card-wrapper self">
              <MessageCommodityCard
                v-if="message.commodityId && message.commodity"
                :commodity="message.commodity"
                @click="handleCommodityClick"
              />
              <!-- 订单卡片 -->
              <MessageOrderCard
                v-if="message.orderId && message.order"
                :order="message.order"
                @click="handleOrderClick"
              />
            </div>
            <div class="message-meta-self">
              <span class="message-time">{{ formatTime(message.createdAt) }}</span>
              <span v-if="message.isRead" class="read-status">已读</span>
            </div>
          </div>
          <div class="message-avatar">
            <el-avatar :size="36" :src="getAvatarUrl(message.senderAvatar)">
              {{ message.senderNickname?.charAt(0) || 'U' }}
            </el-avatar>
          </div>
        </template>
      </div>
    </div>

    <!-- 输入区域 -->
    <div class="message-input-area">
      <UnifiedInput
        v-model="localValue"
        type="textarea"
        :autosize="{ minRows: 2, maxRows: 6 }"
        placeholder="输入消息..."
        @keydown.enter.ctrl="onSend"
      />
      <div class="input-actions">
        <span class="input-tip">Ctrl + Enter 发送</span>
        <div class="buttons-group">
          <UnifiedButton 
            class="action-btn consult-commodity-btn" 
            :type="selectedCommodityId ? 'primary' : 'default'"
            @click="showCommodityDialog"
          >
            咨询商品
          </UnifiedButton>
          <UnifiedButton 
            class="action-btn consult-order-btn" 
            :type="selectedOrderId ? 'primary' : 'default'"
            @click="showOrderDialog"
          >
            咨询订单
          </UnifiedButton>
          <UnifiedButton 
            type="primary" 
            class="action-btn send-btn" 
            :disabled="!localValue.trim() || sending" 
            @click="onSend"
          >
            发送
          </UnifiedButton>
        </div>
      </div>
      
      <!-- 选择商品/订单弹窗 -->
      <SelectCommodityOrOrderDialog
        v-model="commodityDialogVisible"
        type="commodity"
        :default-id="selectedCommodityId || defaultCommodityId"
        :other-user-id="currentConversation?.otherUserId"
        :current-user-id="currentUserId"
        @confirm="handleCommoditySelected"
        @cancel="handleCommodityCancel"
      />
      
      <SelectCommodityOrOrderDialog
        v-model="orderDialogVisible"
        type="order"
        :default-id="selectedOrderId || defaultOrderId"
        :other-user-id="currentConversation?.otherUserId"
        :current-user-id="currentUserId"
        @confirm="handleOrderSelected"
        @cancel="handleOrderCancel"
      />
    </div>
  </div>
</template>

<script setup>
/* global defineProps, defineEmits */
import { ref, watch, onMounted, onUnmounted, nextTick, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '../../stores/user'
import { useMessageStore } from '../../stores/message'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowLeft } from '@element-plus/icons-vue'
import UnifiedButton from '../common/UnifiedButton.vue'
import UnifiedInput from '../common/UnifiedInput.vue'
import MessageCommodityCard from './CommodityCard.vue'
import MessageOrderCard from './OrderCard.vue'
import SelectCommodityOrOrderDialog from './SelectCommodityOrOrderDialog.vue'

const props = defineProps({
  currentConversation: { type: Object, default: null },
  messages: { type: Array, default: () => [] },
  messagesLoading: { type: Boolean, default: false },
  modelValue: { type: String, default: '' },
  sending: { type: Boolean, default: false },
  getAvatarUrl: { type: Function, required: true },
  formatTime: { type: Function, required: true },
  defaultCommodityId: { type: String, default: null },
  defaultOrderId: { type: String, default: null }
})

const userStore = useUserStore()
const currentUserId = computed(() => userStore.user?.userId)

const emit = defineEmits(['update:modelValue', 'send', 'back', 'view-profile'])

const router = useRouter()
const commodityDialogVisible = ref(false)
const orderDialogVisible = ref(false)
const selectedCommodityId = ref(null)
const selectedOrderId = ref(null)

const localValue = ref(props.modelValue)
watch(() => props.modelValue, v => { localValue.value = v })
watch(localValue, v => emit('update:modelValue', v))

const messagesListRef = ref(null)
// ✅ v1.3.x: 保存"是否在底部"状态（CSS反转后，底部是 scrollTop=0）
const wasAtBottom = ref(true) // 默认假设在底部，用于首次加载和发送消息时的滚动
const isLoadingMore = ref(false) // 是否正在加载历史消息
const hasMoreMessages = ref(true) // 是否还有更多历史消息
// ✅ v1.3.x: 自动扩展功能 - 停留在顶部时自动加载历史消息
const topStayTimer = ref(null) // 停留在顶部的定时器
const topStayDuration = 2000 // 停留在顶部2秒后自动加载（单位：毫秒）
// ✅ v1.3.x: 标记是否正在加载历史消息（用于区分新消息和历史消息）
const isLoadingHistory = ref(false) // 是否正在加载历史消息（添加到数组末尾）
// ✅ v1.3.x: 消息操作菜单相关状态
const showMessageMenu = ref(false) // 是否显示消息操作菜单
const selectedMessage = ref(null) // 当前选中的消息
const menuPosition = ref({ top: '0px', left: '0px' }) // 菜单位置

// ✅ v1.3.x: 检测滚动条是否在底部（标准滚动：底部是 scrollTop 接近 scrollHeight）
const isAtBottom = () => {
  if (!messagesListRef.value) {
    console.warn('isAtBottom: messagesListRef 未绑定')
    return false
  }
  const { scrollTop, scrollHeight, clientHeight } = messagesListRef.value
  
  // ✅ v1.3.x: 标准滚动方式，底部是 scrollTop 接近 scrollHeight - clientHeight
  const distance = scrollHeight - scrollTop - clientHeight
  const isAtBottom = distance <= 100 // 允许 100px 的误差范围
  
  console.log('检测滚动位置（标准）:', { scrollTop, scrollHeight, clientHeight, distance, isAtBottom })
  return isAtBottom
}

// ✅ v1.3.x: 检测滚动条是否在顶部（标准滚动：顶部是 scrollTop=0）
const isAtTop = () => {
  if (!messagesListRef.value) {
    return false
  }
  const { scrollTop, scrollHeight, clientHeight } = messagesListRef.value
  
  // ✅ v1.3.x: 标准滚动方式，顶部是 scrollTop 接近 0
  const isAtTop = scrollTop <= 100 // 允许 100px 的误差范围
  
  console.log('检测滚动位置（顶部，标准）:', { 
    scrollTop, 
    scrollHeight, 
    clientHeight, 
    isAtTop 
  })
  return isAtTop
}

// ✅ v1.3.x: 更新"是否在底部"状态（通过滚动事件监听）
const updateWasAtBottom = () => {
  wasAtBottom.value = isAtBottom()
}

// ✅ v1.3.x: 滚动到底部（标准滚动：底部是 scrollTop = scrollHeight - clientHeight）
const scrollToBottom = (smooth = false) => {
  if (!messagesListRef.value) {
    console.warn('scrollToBottom: messagesListRef 未绑定')
    return
  }
  
  const scrollHeight = messagesListRef.value.scrollHeight
  const clientHeight = messagesListRef.value.clientHeight
  const targetScrollTop = scrollHeight - clientHeight
  
  console.log('执行滚动到底部（标准）:', { smooth, scrollHeight, clientHeight, targetScrollTop })
  
  if (smooth) {
    messagesListRef.value.scrollTo({
      top: targetScrollTop,
      behavior: 'smooth'
    })
  } else {
    messagesListRef.value.scrollTop = targetScrollTop
  }
  
  // ✅ 验证滚动是否成功
  setTimeout(() => {
    const afterScroll = messagesListRef.value.scrollTop
    const finalHeight = messagesListRef.value.scrollHeight
    const finalClientHeight = messagesListRef.value.clientHeight
    const distance = finalHeight - afterScroll - finalClientHeight
    console.log('滚动后验证（标准）:', { scrollTop: afterScroll, scrollHeight: finalHeight, clientHeight: finalClientHeight, distance, success: distance <= 10 })
  }, smooth ? 500 : 50)
}

// ✅ v1.3.x: 加载历史消息（向上滚动时触发或自动扩展时触发）
const loadMoreMessages = async () => {
  if (isLoadingMore.value || !hasMoreMessages.value || !props.messages || props.messages.length === 0) {
    return
  }
  
  // ✅ v1.3.x: 获取最早的消息时间（ASC排序：第一个元素是最旧的）
  const oldestMessage = props.messages[0]
  if (!oldestMessage || !oldestMessage.createdAt) {
    return
  }
  
  isLoadingMore.value = true
  // ✅ v1.3.x: 标记正在加载历史消息（添加到数组末尾）
  isLoadingHistory.value = true
  
  // ✅ v1.3.x: 清除自动扩展定时器（避免重复加载）
  if (topStayTimer.value) {
    clearTimeout(topStayTimer.value)
    topStayTimer.value = null
  }
  
  try {
    // ✅ v1.3.x: 保存当前滚动位置和高度（用于保持滚动位置）
    const scrollContainer = messagesListRef.value
    
    // ✅ v1.3.x: 标准滚动方式：记录第一个可见消息元素，加载后滚动回该位置
    const messageItems = scrollContainer.querySelectorAll('.message-item')
    let referenceMessage = null
    let referenceOffsetTop = 0
    
    if (messageItems.length > 0) {
      // 找到第一个可见的消息（在顶部）
      referenceMessage = messageItems[0]
      if (referenceMessage) {
        referenceOffsetTop = referenceMessage.offsetTop
      }
    }
    
    const oldScrollHeight = scrollContainer.scrollHeight
    const oldScrollTop = scrollContainer.scrollTop
    
    console.log('加载历史消息前:', { 
      oldScrollHeight, 
      oldScrollTop, 
      referenceOffsetTop,
      messagesCount: props.messages.length
    })
    
    // 调用 Store 方法加载历史消息
    const messageStore = useMessageStore()
    const result = await messageStore.loadMoreMessages(
      props.currentConversation?.conversationId,
      oldestMessage.createdAt,
      50
    )
    
    if (result && result.hasMore !== undefined) {
      hasMoreMessages.value = result.hasMore
    }
    
    // 等待DOM更新
    await nextTick()
    
    // ✅ v1.3.x: 恢复滚动位置（标准滚动方式）
    const newScrollHeight = scrollContainer.scrollHeight
    const heightDiff = newScrollHeight - oldScrollHeight
    
    // 方法1：如果有参考消息，滚动到该消息的位置
    if (referenceMessage) {
      // 重新查找该消息（DOM已更新，新消息已添加到开头）
      const newMessageItems = scrollContainer.querySelectorAll('.message-item')
      // ✅ v1.3.x: 对于标准滚动，当新消息添加到数组开头时，
      // 原来的第一个消息（index 0）会变成第二个（index = 新消息数量）
      // 但我们需要找到原来的第一个消息的新位置
      // 由于新消息添加到开头，原来的第一个消息的索引会增加
      const newMessagesCount = newMessageItems.length - messageItems.length
      if (newMessagesCount > 0 && newMessageItems.length > newMessagesCount) {
        // 原来的第一个消息现在在新位置（索引 = newMessagesCount）
        const newReferenceMessage = newMessageItems[newMessagesCount]
        if (newReferenceMessage) {
          const newOffsetTop = newReferenceMessage.offsetTop
          // 计算需要滚动的距离
          const scrollOffset = newOffsetTop - referenceOffsetTop
          // 标准滚动：向上滚动（查看更早的消息）需要增加 scrollTop
          scrollContainer.scrollTop = oldScrollTop + scrollOffset
          console.log('使用参考消息恢复滚动位置:', {
            oldScrollTop,
            referenceOffsetTop,
            newOffsetTop,
            scrollOffset,
            newScrollTop: scrollContainer.scrollTop
          })
        }
      }
    } else {
      // 方法2：如果没有参考消息，使用高度差计算
      // 标准滚动：当内容在顶部增加时，需要增加 scrollTop 来保持视觉位置
      scrollContainer.scrollTop = oldScrollTop + heightDiff
    }
    
    console.log('加载历史消息完成:', { 
      oldScrollHeight, 
      newScrollHeight, 
      heightDiff,
      oldScrollTop,
      newScrollTop: scrollContainer.scrollTop,
      hasMore: hasMoreMessages.value 
    })
    
    // ✅ v1.3.x: 加载完成后，如果仍在顶部且有更多消息，重新启动自动扩展定时器
    // 延迟检查，确保滚动位置已稳定
    setTimeout(() => {
      if (hasMoreMessages.value && isAtTop()) {
        startAutoLoadTimer()
      }
    }, 100)
  } catch (error) {
    console.error('加载历史消息失败:', error)
    ElMessage.error('加载历史消息失败')
  } finally {
    isLoadingMore.value = false
    // ✅ v1.3.x: 延迟清除历史消息标记，确保 watch 能够正确识别
    // 增加延迟时间，确保滚动位置已稳定且 watch 已处理完成
    setTimeout(() => {
      isLoadingHistory.value = false
      console.log('清除历史消息加载标记')
    }, 300)
  }
}

// ✅ v1.3.x: 启动自动加载定时器（停留在顶部时）
const startAutoLoadTimer = () => {
  // 清除现有定时器
  if (topStayTimer.value) {
    clearTimeout(topStayTimer.value)
  }
  
  // 检查是否可以加载
  if (isLoadingMore.value || !hasMoreMessages.value || !props.messages || props.messages.length === 0) {
    console.log('无法启动自动加载定时器:', { 
      isLoadingMore: isLoadingMore.value, 
      hasMoreMessages: hasMoreMessages.value, 
      messagesCount: props.messages?.length 
    })
    return
  }
  
  console.log('启动自动加载定时器，将在', topStayDuration, 'ms后加载历史消息')
  
  // 设置定时器
  topStayTimer.value = setTimeout(() => {
    // 再次检查是否仍在顶部
    const stillAtTop = isAtTop()
    const canLoad = !isLoadingMore.value && hasMoreMessages.value
    
    console.log('自动加载定时器触发:', { 
      stillAtTop, 
      canLoad, 
      isLoadingMore: isLoadingMore.value, 
      hasMoreMessages: hasMoreMessages.value 
    })
    
    if (stillAtTop && canLoad) {
      console.log('自动扩展：停留在顶部超过', topStayDuration, 'ms，开始加载历史消息')
      loadMoreMessages()
    } else {
      console.log('自动加载条件不满足，取消加载')
    }
    topStayTimer.value = null
  }, topStayDuration)
}

// ✅ v1.3.x: 清除自动加载定时器
const clearAutoLoadTimer = () => {
  if (topStayTimer.value) {
    clearTimeout(topStayTimer.value)
    topStayTimer.value = null
  }
}
// ✅ 检查卡片元素是否已渲染（通过检查消息中的卡片元素）
const checkCardsRendered = () => {
  if (!messagesListRef.value || !props.messages || props.messages.length === 0) {
    // 没有消息，认为不需要渲染卡片
    return true
  }
  
  // ✅ 统计哪些消息应该有卡片（基于消息数据）
  let expectedCardCount = 0
  let renderedCardCount = 0
  
  // 遍历所有消息，检查哪些应该有卡片
  props.messages.forEach((message, index) => {
    const shouldHaveCommodityCard = message.commodityId && message.commodity
    const shouldHaveOrderCard = message.orderId && message.order
    
    if (shouldHaveCommodityCard || shouldHaveOrderCard) {
      expectedCardCount++
      
      // ✅ 找到对应的DOM元素（通过索引）
      const messageItems = messagesListRef.value.querySelectorAll('.message-item')
      const messageItem = messageItems[index]
      
      if (messageItem) {
        // 检查card-wrapper是否存在
        const cardWrapper = messageItem.querySelector('.card-wrapper')
        if (cardWrapper) {
          // ✅ 检查对应的卡片元素是否存在且已渲染（高度>0）
          let cardFound = false
          
          if (shouldHaveCommodityCard) {
            const commodityCard = cardWrapper.querySelector('.commodity-card')
            if (commodityCard && commodityCard.offsetHeight > 0) {
              cardFound = true
            }
          }
          
          if (shouldHaveOrderCard) {
            const orderCard = cardWrapper.querySelector('.order-card')
            if (orderCard && orderCard.offsetHeight > 0) {
              cardFound = true
            }
          }
          
          if (cardFound) {
            renderedCardCount++
          }
        }
      }
    }
  })
  
  const allCardsRendered = expectedCardCount === 0 || renderedCardCount === expectedCardCount
  return allCardsRendered
}

// ✅ 等待DOM稳定后再滚动（检测卡片渲染和DOM高度稳定性）
const waitForStableScrollHeight = (maxWaitTime = 2000, checkInterval = 50, requiredStableChecks = 4, minWaitTime = 300) => {
  return new Promise((resolve) => {
    if (!messagesListRef.value) {
      resolve()
      return
    }
    
    const startTime = Date.now()
    let lastHeight = messagesListRef.value.scrollHeight
    let stableCount = 0
    let elapsed = 0
    
    const checkStability = () => {
      if (!messagesListRef.value) {
        resolve()
        return
      }
      
      const currentHeight = messagesListRef.value.scrollHeight
      const timeElapsed = Date.now() - startTime
      
      // ✅ 检查卡片是否已渲染（如果消息中有卡片）
      const cardsRendered = checkCardsRendered()
      
      // ✅ 检查高度是否稳定
      const heightStable = currentHeight === lastHeight
      
      if (heightStable) {
        stableCount++
      } else {
        // 高度还在变化，重置计数器
        stableCount = 0
        lastHeight = currentHeight
      }
      
      // ✅ 同时满足以下条件才认为稳定：
      // 1. 高度连续稳定多次（4次，200ms）
      // 2. 卡片已渲染（如果有卡片）
      // 3. 至少等待最小时间（300ms，避免空窗期误判）
      const heightStableEnough = stableCount >= requiredStableChecks
      const minTimePassed = timeElapsed >= minWaitTime
      
      if (heightStableEnough && cardsRendered && minTimePassed) {
        resolve()
        return
      }
      
      elapsed += checkInterval
      
      if (elapsed >= maxWaitTime) {
        // 超时后强制滚动（2秒）
        resolve()
        return
      }
      
      setTimeout(checkStability, checkInterval)
    }
    
    checkStability()
  })
}

onMounted(async () => {
  await nextTick()
  // ✅ v1.3.x: 设置滚动事件监听，实时更新 wasAtBottom 状态和检测向上滚动
  if (messagesListRef.value) {
    // 使用防抖监听滚动，避免频繁更新
    let scrollTimeout
    messagesListRef.value.addEventListener('scroll', () => {
      clearTimeout(scrollTimeout)
      scrollTimeout = setTimeout(() => {
        updateWasAtBottom()
        
        // ✅ v1.3.x: 检测是否滚动到顶部（向上滚动）
        const atTop = isAtTop()
        console.log('滚动事件检测:', { 
          atTop, 
          hasMoreMessages: hasMoreMessages.value, 
          isLoadingMore: isLoadingMore.value,
          messagesCount: props.messages?.length
        })
        
        if (atTop) {
          // 如果正在加载或没有更多消息，清除自动扩展定时器
          if (isLoadingMore.value || !hasMoreMessages.value) {
            console.log('清除自动扩展定时器（条件不满足）:', { 
              isLoadingMore: isLoadingMore.value, 
              hasMoreMessages: hasMoreMessages.value 
            })
            clearAutoLoadTimer()
          } else {
            // 在顶部且有更多消息，启动自动扩展定时器
            console.log('启动自动扩展定时器')
            startAutoLoadTimer()
          }
        } else {
          // 不在顶部，清除自动扩展定时器
          clearAutoLoadTimer()
        }
      }, 100)
    }, { passive: true })
  }
  
  // ✅ v1.3.x: 初始加载时滚动到底部（标准滚动：底部是 scrollTop = scrollHeight - clientHeight）
  wasAtBottom.value = true // 初始状态设为在底部
  if (messagesListRef.value && props.messages && props.messages.length > 0) {
    // 检查是否有卡片消息，需要等待渲染
    const hasCardMessages = props.messages.some(m => m.commodityId || m.orderId)
    await nextTick()
    if (hasCardMessages) {
      // 有卡片消息，等待DOM稳定和卡片渲染完成
      await waitForStableScrollHeight(2000, 50, 4, 500)
    }
    scrollToBottom()
  }
})

// ✅ v1.3.x: 组件卸载时清除定时器
onUnmounted(() => {
  clearAutoLoadTimer()
})

// ✅ v1.3.x: 监听 messages 变化，智能滚动到底部（标准滚动）
// 使用两种方式监听：1) 监听数组引用变化 2) 监听数组长度变化
watch(() => props.messages?.length, (newLength, oldLength) => {
  console.log('🔍 watch messages.length 触发（标准）:', { newLength, oldLength, messages: props.messages?.length, wasAtBottom: wasAtBottom.value })
  
  // 首次加载（oldLength 为 undefined 或 0）
  if (!oldLength || oldLength === 0) {
    if (newLength && newLength > 0) {
      console.log('✅ 首次加载消息，滚动到底部（标准）')
      wasAtBottom.value = true // 首次加载时假设在底部
      hasMoreMessages.value = true // 重置是否有更多消息
      nextTick(async () => {
        // 检查是否有卡片消息，需要等待渲染
        const hasCardMessages = props.messages?.some(m => m.commodityId || m.orderId)
        if (hasCardMessages) {
          // 有卡片消息，等待DOM稳定和卡片渲染完成
          await waitForStableScrollHeight(2000, 50, 4, 500)
        }
        scrollToBottom()
      })
    }
    return
  }
  
  // ✅ v1.3.x: 当有新消息时（长度增加）
  // ASC排序：新消息添加到数组末尾（index length-1），历史消息添加到数组开头（index 0）
  if (newLength && newLength > oldLength) {
    // ✅ v1.3.x: 智能判断：区分新消息和历史消息
    // 新消息：添加到数组末尾（最新消息），用户应该在底部
    // 历史消息：添加到数组开头（更早的消息），用户应该在顶部（查看历史）
    
    // 检查是否是加载历史消息（添加到数组开头）
    if (isLoadingHistory.value) {
      console.log('✅ 检测到历史消息加载，不自动滚动到底部:', { 
        oldLength, 
        newLength, 
        increase: newLength - oldLength,
        isLoadingHistory: isLoadingHistory.value,
        isLoadingMore: isLoadingMore.value
      })
      // 历史消息加载完成，不自动滚动，保持当前位置
      // ✅ v1.3.x: 确保 wasAtBottom 不会被设置为 true，避免后续误判
      wasAtBottom.value = false
      return
    }
    
    // ✅ v1.3.x: 检查最新消息（数组的最后一个元素）
    const latestMessage = props.messages?.[props.messages.length - 1]
    const hasCardMessage = latestMessage?.commodityId || latestMessage?.orderId
    
    // ✅ 在消息渲染前立即保存滚动状态
    const currentAtBottom = isAtBottom()
    if (currentAtBottom) {
      wasAtBottom.value = true
    }
    const shouldScroll = wasAtBottom.value
    
    console.log('✅ 检测到新消息，准备滚动（反转后）:', { 
      oldLength, 
      newLength, 
      increase: newLength - oldLength, 
      wasAtBottom: wasAtBottom.value,
      currentAtBottom,
      hasCardMessage,
      isLoadingHistory: isLoadingHistory.value
    })
    
    // ✅ 延迟一点，确保DOM完全更新（等待Vue渲染引擎完成卡片渲染）
    nextTick(async () => {
      if (shouldScroll) {
        // ✅ 如果有卡片消息，等待DOM稳定和卡片渲染完成
        if (hasCardMessage) {
          await waitForStableScrollHeight(2000, 50, 4, 500)
        }
        
        // ✅ 再次确认是否在底部（防止用户在等待期间手动滚动）
        const stillAtBottom = wasAtBottom.value || isAtBottom()
        
        if (stillAtBottom) {
          scrollToBottom(true) // 使用平滑滚动，更自然
          // 滚动后更新状态
          setTimeout(() => {
            wasAtBottom.value = true
          }, 300)
        }
      }
    })
  }
}, { immediate: true })

// ✅ 同时监听数组引用变化（作为备用）
watch(() => props.messages, (newMessages, oldMessages) => {
  console.log('🔍 watch messages 数组引用触发:', { 
    newLength: newMessages?.length, 
    oldLength: oldMessages?.length,
    newRef: newMessages,
    oldRef: oldMessages
  })
}, { deep: false })

// ✅ v1.3.x: 监听对话切换，确保切换对话后滚动到底部（标准滚动）
watch(() => props.currentConversation, async (newConv, oldConv) => {
  if (newConv && newConv.conversationId !== oldConv?.conversationId) {
    // ✅ v1.3.x: 清除自动扩展定时器
    clearAutoLoadTimer()
    
    // ✅ v1.3.x: 重置历史消息加载标记
    isLoadingHistory.value = false
    
    // 切换到新对话时，重置状态并滚动到底部（等待卡片渲染完成）
    wasAtBottom.value = true // 切换对话时假设在底部
    hasMoreMessages.value = true // 重置是否有更多消息
    await nextTick()
    // 检查是否有卡片消息，需要等待渲染
    if (props.messages && props.messages.length > 0) {
      const hasCardMessages = props.messages.some(m => m.commodityId || m.orderId)
      if (hasCardMessages) {
        // 有卡片消息，等待DOM稳定和卡片渲染完成
        await waitForStableScrollHeight(2000, 50, 4, 500)
      }
    }
    scrollToBottom()
  }
}, { immediate: true })

// 监听默认ID，自动打开弹窗
watch(() => props.defaultCommodityId, (id) => {
  if (id) {
    selectedCommodityId.value = id
    commodityDialogVisible.value = true
  }
})

watch(() => props.defaultOrderId, (id) => {
  if (id) {
    selectedOrderId.value = id
    orderDialogVisible.value = true
  }
})

const showCommodityDialog = () => {
  commodityDialogVisible.value = true
}

const showOrderDialog = () => {
  orderDialogVisible.value = true
}

const handleCommoditySelected = (commodityId) => {
  selectedCommodityId.value = commodityId
  selectedOrderId.value = null // 一个消息只能有一个商品或订单
}

const handleCommodityCancel = () => {
  // 取消选择时清空状态
  selectedCommodityId.value = null
}

const handleOrderSelected = (orderId) => {
  selectedOrderId.value = orderId
  selectedCommodityId.value = null // 一个消息只能有一个商品或订单
}

const handleOrderCancel = () => {
  // 取消选择时清空状态
  selectedOrderId.value = null
}

const handleCommodityClick = (commodity) => {
  router.push(`/commodity/${commodity.commodityId}`)
}

const handleOrderClick = (order) => {
  router.push(`/order/${order.orderId}`)
}

const onSend = () => {
  if (!localValue.value.trim() || props.sending) return
  
  // ✅ 发送消息前，保存当前是否在底部的状态
  // 这样可以避免因为卡片消息渲染导致的检测失败问题
  wasAtBottom.value = isAtBottom()
  console.log('📤 发送消息前保存滚动状态:', { wasAtBottom: wasAtBottom.value })
  
  emit('send', {
    commodityId: selectedCommodityId.value,
    orderId: selectedOrderId.value
  })
  // 发送后清空选择
  selectedCommodityId.value = null
  selectedOrderId.value = null
  // ✅ 滚动逻辑通过 watch messages 自动处理（发送消息后 messages 会更新）
  // 使用 wasAtBottom 状态来判断是否需要滚动
}

// ✅ v1.3.x: 处理消息点击（所有消息都可以软删除，只影响自己的视图）
const handleMessageClick = (event, message) => {
  // 如果点击的是商品卡片或订单卡片，不显示菜单
  if (event.target.closest('.card-wrapper') || event.target.closest('.message-menu')) {
    return
  }
  
  // 切换菜单显示状态（如果点击的是同一个消息，则关闭；否则显示）
  if (selectedMessage.value?.messageId === message.messageId && showMessageMenu.value) {
    showMessageMenu.value = false
    selectedMessage.value = null
    return
  }
  
  showMessageMenu.value = true
  selectedMessage.value = message
  
  // ✅ 计算菜单位置：显示在消息气泡的上方
  // 菜单使用 position: absolute，相对于 .message-item 定位
  const messageBubble = event.currentTarget.querySelector('.message-bubble')
  const messageItem = event.currentTarget.closest('.message-item')
  
  if (messageBubble && messageItem) {
    const bubbleRect = messageBubble.getBoundingClientRect()
    const itemRect = messageItem.getBoundingClientRect()
    const menuHeight = 40 // 菜单高度（估算值）
    const offset = 8 // 菜单与气泡的间距
    
    // 计算气泡相对于消息项的位置
    const bubbleTopRelativeToItem = bubbleRect.top - itemRect.top
    
    menuPosition.value = {
      top: `${bubbleTopRelativeToItem - menuHeight - offset}px`,
      left: `${bubbleRect.left - itemRect.left}px`
    }
  } else {
    // 备用方案：使用点击位置（相对于消息项）
    if (messageItem) {
      const itemRect = messageItem.getBoundingClientRect()
      menuPosition.value = {
        top: `${event.clientY - itemRect.top - 48}px`,
        left: `${event.clientX - itemRect.left}px`
      }
    }
  }
  
  // 点击其他地方关闭菜单
  // ✅ 保存消息项元素的引用，避免在回调中 event.currentTarget 为 null
  const messageElement = event.currentTarget.closest('.message-item')
  
  const closeMenu = (e) => {
    // 检查点击是否在消息项或菜单之外
    if (messageElement && !messageElement.contains(e.target)) {
      // 检查点击是否在菜单内部（通过查找当前选中消息的菜单元素）
      const menuElement = messageElement.querySelector('.message-menu')
      if (!menuElement || !menuElement.contains(e.target)) {
        showMessageMenu.value = false
        selectedMessage.value = null
        document.removeEventListener('click', closeMenu)
      }
    }
  }
  
  nextTick(() => {
    document.addEventListener('click', closeMenu)
  })
}

// ✅ v1.3.x: 删除消息
const handleDeleteMessage = async (message) => {
  // 关闭菜单
  showMessageMenu.value = false
  selectedMessage.value = null
  
  try {
    // 确认删除
    await ElMessageBox.confirm(
      '确定要删除这条消息吗？删除后您将无法看到此消息，但对方仍可以看到。',
      '删除消息',
      {
        confirmButtonText: '删除',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    
    // 调用删除API
    const messageStore = useMessageStore()
    const response = await messageStore.deleteMessage(message.messageId)
    
    if (response && response.success) {
      ElMessage.success('消息已删除')
      // Store 中已经更新了 messages 列表，这里不需要手动更新
    } else {
      ElMessage.error(response?.message || '删除失败')
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除消息失败:', error)
      ElMessage.error('删除消息失败')
    }
  }
}
</script>

<style scoped>
.chat-window {
  display: flex;
  flex-direction: column;
  height: 100%;
}

/* 聊天头部与用户信息 */
.chat-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px;
  border-bottom: 1px solid #f0f0f0;
}
.desktop-back-button { display: none; cursor: pointer; }
.chat-user-info { display: flex; align-items: center; gap: 12px; }
.user-info .user-name { font-size: 16px; color: var(--primary-color); font-weight: normal; }
.deleted-tag { font-size: 12px; color: #999; background: #f5f5f5; padding: 2px 6px; border-radius: 4px; margin-left: 8px; }

/* 消息列表（同步原布局与字体设置） */
/* ✅ v1.3.x: 标准滚动方式，最新消息在底部 */
.messages-list {
  flex: 1 1 0; /* 允许缩小到0，占满剩余空间 */
  overflow-y: auto;
  padding: 20px;
  display: flex;
  flex-direction: column; /* ✅ v1.3.x: 标准顺序，最旧消息在顶部，最新消息在底部 */
  gap: 15px;
  min-height: 0; /* 允许缩小到内容大小 */
}

.message-item { 
  display: flex; 
  gap: 10px; 
  align-items: flex-start; 
  width: 100%; 
  position: relative; /* ✅ v1.3.x: 为消息菜单定位 */
}
.message-item.my-message { justify-content: flex-end; }
.message-avatar { flex-shrink: 0; width: 36px; height: 36px; }

/* ✅ v1.3.x: 消息操作菜单样式 */
.message-menu {
  position: absolute;
  background: transparent;
  border-radius: 8px;
  box-shadow: none;
  z-index: 1000;
  padding: 4px;
  border: none;
}

.message-menu .delete-button {
  width: 100%;
  min-width: 100px;
}

.message-content { display: flex; flex-direction: column; max-width: 60%; min-width: 100px; flex: 1 1 0; overflow: visible; text-align: left; align-items: flex-start; }
.message-content.self { align-items: flex-end; flex: 1 1 0; }
.message-item .self { margin-left: auto; text-align: left; }

.message-bubble {
  background-color: #f0f0f0;
  padding: 10px 15px;
  border-radius: 12px;
  word-wrap: break-word;
  word-break: break-word;
  overflow-wrap: break-word;
  max-width: 100%;
  box-sizing: border-box;
  width: fit-content;
  min-width: 0;
}
.message-bubble.self { background: var(--primary-color); color: #fff; }

.message-bubble p { margin: 0; font-size: 14px; line-height: 1.5; font-weight: normal; word-wrap: break-word; word-break: break-word; overflow-wrap: break-word; white-space: normal; overflow: hidden; text-align: left; }

.message-time { font-size: 12px; color: #999; margin-top: 4px; font-weight: normal; }
.message-meta-other { display: flex; align-items: center; gap: 8px; margin-top: 4px; justify-content: flex-start; }
.message-meta-self { display: flex; align-items: center; gap: 8px; margin-top: 4px; justify-content: flex-end; }
.read-status { font-size: 12px; color: #67c23a; margin-top: 4px; font-weight: normal; }

/* 卡片包装器，控制对齐 */
.card-wrapper {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  margin-top: 8px;
  width: 100%;
  max-width: 300px;
}

.card-wrapper.self {
  align-items: flex-end;
  margin-left: auto;
}

/* 输入区域（同步原位置与尺寸） */
.message-input-area {
  border-top: 1px solid #f0f0f0;
  padding: 15px 20px;
  box-sizing: border-box;
  max-width: 100%;
  display: flex;
  flex-direction: column;
  flex-shrink: 0;
  overflow-y: auto;
  max-height: 250px;
}
.message-input-area :deep(.el-textarea__inner) {
  border-radius: 12px;
  border-color: #e0e0e0;
  resize: none;
  max-width: 100%;
  box-sizing: border-box;
}
.message-input-area :deep(.el-textarea__inner:focus) { border-color: var(--primary-color); }

.input-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 10px;
  max-width: 100%;
  box-sizing: border-box;
  gap: 12px;
}

.input-tip { 
  font-size: 12px; 
  color: #999; 
  font-weight: normal; 
  flex-shrink: 0;
}

.input-actions .el-button { 
  border-radius: 20px; 
}

/* 桌面端：按钮组靠右，可以稍微窄一点 */
.buttons-group {
  display: flex;
  gap: 8px;
  flex-shrink: 1;
  min-width: 0;
}

/* 桌面端：三个按钮等宽，但整体宽度可以自适应 */
.buttons-group .action-btn {
  flex: 1;
  min-width: 0;
}

/* 统一所有按钮的高度 */
.input-actions .action-btn {
  height: 40px !important;
  line-height: 40px !important;
  padding: 0 16px !important;
  box-sizing: border-box;
}


@media (max-width: 900px) {
  .desktop-back-button { display: block; }
  .message-input-area { padding: 10px 16px 8px 16px; display: flex; flex-direction: column; gap: 8px; }
  .message-input-area :deep(.el-textarea__inner) { width: 100% !important; max-width: 100% !important; min-height: 60px; }
  .input-actions { 
    flex-direction: row; 
    gap: 8px; 
    width: 100%; 
    margin-top: 8px;
    align-items: stretch;
    justify-content: flex-start;
  }
  .input-tip { display: none; }
  /* 移动端：三个按钮等大小同一行，直接放在 input-actions 下 */
  .buttons-group {
    flex: 1;
    display: flex;
    gap: 8px;
    width: 100%;
  }
  .input-actions .action-btn {
    flex: 1;
    height: 40px !important;
    line-height: 40px !important;
    padding: 0 16px !important;
    font-size: 14px;
    min-width: 0;
    box-sizing: border-box;
  }
  /* 移动端：消息内容占满整行，不避让头像 */
  .message-content { 
    max-width: calc(100% - 46px); /* 减去头像宽度 + gap */
    flex: 1 1 0;
    min-width: 0;
  }
  .message-content.self { 
    max-width: calc(100% - 46px);
    flex: 1 1 0;
    min-width: 0;
  }
}
</style>
