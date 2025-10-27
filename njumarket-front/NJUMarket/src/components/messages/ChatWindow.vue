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
        :default-id="defaultCommodityId"
        :other-user-id="currentConversation?.otherUserId"
        :current-user-id="currentUserId"
        @confirm="handleCommoditySelected"
        @cancel="handleCommodityCancel"
      />
      
      <SelectCommodityOrOrderDialog
        v-model="orderDialogVisible"
        type="order"
        :default-id="defaultOrderId"
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
const scrollToBottom = () => {
  if (messagesListRef.value) {
    messagesListRef.value.scrollTop = messagesListRef.value.scrollHeight
  }
}

onMounted(async () => {
  await nextTick()
  scrollToBottom()
})

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
  selectedCommodityId.value = null
}

const handleOrderSelected = (orderId) => {
  selectedOrderId.value = orderId
  selectedCommodityId.value = null // 一个消息只能有一个商品或订单
}

const handleOrderCancel = () => {
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
  emit('send', {
    commodityId: selectedCommodityId.value,
    orderId: selectedOrderId.value
  })
  // 发送后清空选择
  selectedCommodityId.value = null
  selectedOrderId.value = null
  nextTick(() => scrollToBottom())
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
