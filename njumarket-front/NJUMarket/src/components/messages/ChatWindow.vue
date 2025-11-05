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
        <template v-if="!message.isMine">
          <div class="message-avatar">
            <el-avatar :size="36" :src="getAvatarUrl(message.senderAvatar)">
              {{ message.senderNickname?.charAt(0) || 'U' }}
            </el-avatar>
          </div>
          <div class="message-content">
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
          <div class="message-content self">
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
import { ref, watch, onMounted, nextTick, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '../../stores/user'
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
// ✅ 保存"是否在底部"状态，解决渲染过快导致的检测问题
const wasAtBottom = ref(true) // 默认假设在底部，用于首次加载和发送消息时的滚动

// ✅ 检测滚动条是否在底部（允许一定的误差范围，因为浮点数计算）
const isAtBottom = () => {
  if (!messagesListRef.value) {
    console.warn('isAtBottom: messagesListRef 未绑定')
    return false
  }
  const { scrollTop, scrollHeight, clientHeight } = messagesListRef.value
  // 允许 100px 的误差范围，因为有时滚动位置可能略有偏差（特别是消息刚添加时）
  const distance = scrollHeight - scrollTop - clientHeight
  const isAtBottom = distance <= 100
  console.log('检测滚动位置:', { scrollTop, scrollHeight, clientHeight, distance, isAtBottom })
  return isAtBottom
}

// ✅ 更新"是否在底部"状态（通过滚动事件监听）
const updateWasAtBottom = () => {
  wasAtBottom.value = isAtBottom()
}

// ✅ 滚动到底部（使用平滑滚动或立即滚动）
const scrollToBottom = (smooth = false) => {
  if (!messagesListRef.value) {
    console.warn('scrollToBottom: messagesListRef 未绑定')
    return
  }
  
  const scrollHeight = messagesListRef.value.scrollHeight
  console.log('执行滚动到底部:', { smooth, scrollHeight })
  
  if (smooth) {
    messagesListRef.value.scrollTo({
      top: scrollHeight,
      behavior: 'smooth'
    })
  } else {
    messagesListRef.value.scrollTop = scrollHeight
  }
  
  // ✅ 验证滚动是否成功
  setTimeout(() => {
    const afterScroll = messagesListRef.value.scrollTop
    const finalHeight = messagesListRef.value.scrollHeight
    console.log('滚动后验证:', { scrollTop: afterScroll, scrollHeight: finalHeight, success: Math.abs(finalHeight - afterScroll - messagesListRef.value.clientHeight) < 10 })
  }, smooth ? 500 : 50)
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
  // ✅ 设置滚动事件监听，实时更新 wasAtBottom 状态
  if (messagesListRef.value) {
    // 使用防抖监听滚动，避免频繁更新
    let scrollTimeout
    messagesListRef.value.addEventListener('scroll', () => {
      clearTimeout(scrollTimeout)
      scrollTimeout = setTimeout(() => {
        updateWasAtBottom()
      }, 100)
    }, { passive: true })
  }
  
  // ✅ 初始加载时滚动到底部（等待卡片渲染完成）
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

// ✅ 监听 messages 变化，智能滚动到底部
// 使用两种方式监听：1) 监听数组引用变化 2) 监听数组长度变化
watch(() => props.messages?.length, (newLength, oldLength) => {
  console.log('🔍 watch messages.length 触发:', { newLength, oldLength, messages: props.messages?.length, wasAtBottom: wasAtBottom.value })
  
  // 首次加载（oldLength 为 undefined 或 0）
  if (!oldLength || oldLength === 0) {
    if (newLength && newLength > 0) {
      console.log('✅ 首次加载消息，滚动到底部')
      wasAtBottom.value = true // 首次加载时假设在底部
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
  
  // 当有新消息时（长度增加）
  if (newLength && newLength > oldLength) {
    // ✅ 立即检查最新消息是否包含卡片（在DOM渲染前）
    const latestMessage = props.messages?.[newLength - 1]
    const hasCardMessage = latestMessage?.commodityId || latestMessage?.orderId
    
    // ✅ 在消息渲染前立即保存滚动状态（如果之前没有保存，则立即检测）
    // 这样可以避免因为卡片等大内容渲染导致检测失败的问题
    // 对于接收消息的情况，wasAtBottom 应该已经通过滚动事件更新了
    // 但为了确保准确性，如果检测到可能刚添加消息，立即更新一次
    const currentAtBottom = isAtBottom()
    // 如果检测到已经在底部（可能是刚添加消息但DOM还没完全渲染），更新状态
    if (currentAtBottom) {
      wasAtBottom.value = true
    }
    const shouldScroll = wasAtBottom.value
    
    console.log('✅ 检测到新消息，准备滚动:', { 
      oldLength, 
      newLength, 
      increase: newLength - oldLength, 
      wasAtBottom: wasAtBottom.value,
      currentAtBottom,
      hasCardMessage
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

// ✅ 监听对话切换，确保切换对话后滚动到底部
watch(() => props.currentConversation, async (newConv, oldConv) => {
  if (newConv && newConv.conversationId !== oldConv?.conversationId) {
    // 切换到新对话时，重置状态并滚动到底部（等待卡片渲染完成）
    wasAtBottom.value = true // 切换对话时假设在底部
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
.messages-list {
  flex: 1 1 0; /* 允许缩小到0，占满剩余空间 */
  overflow-y: auto;
  padding: 20px;
  display: flex;
  flex-direction: column;
  gap: 15px;
  min-height: 0; /* 允许缩小到内容大小 */
}

.message-item { display: flex; gap: 10px; align-items: flex-start; width: 100%; }
.message-item.my-message { justify-content: flex-end; }
.message-avatar { flex-shrink: 0; width: 36px; height: 36px; }

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
