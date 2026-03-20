<template>
  <div class="conversations-panel" :class="{ hidden: hidden }">
    <div class="panel-header">
      <h3>消息</h3>
      <UnreadBadge :count="totalUnreadCount" type="text" />
    </div>

    <div class="conversations-list" v-loading="loading">
      <div
        v-for="conversation in conversations"
        :key="conversation.conversationId"
        class="conversation-item-wrapper"
        @touchstart="handleTouchStart($event, conversation)"
        @touchmove="handleTouchMove($event, conversation)"
        @touchend="handleTouchEnd($event, conversation)"
        @mousedown="handleMouseDown($event, conversation)"
        @mouseleave="handleMouseLeave(conversation)"
      >
        <!-- ✅ v1.3.x: 删除按钮（左滑显示） -->
        <div class="conversation-delete-action">
          <el-button 
            type="danger" 
            size="small"
            @click.stop="handleDeleteConversation(conversation)"
            class="delete-button"
          >
            删除
          </el-button>
        </div>
        
        <!-- 对话内容 -->
        <div
          class="conversation-item"
          :class="{ 
            active: selectedConversationId === conversation.conversationId,
            'swiped-left': swipedConversationId === conversation.conversationId
          }"
          :style="{ transform: `translateX(${getTranslateX(conversation.conversationId)}px)` }"
          @click="handleSelectConversation(conversation)"
        >
          <div class="conversation-avatar">
            <UnreadBadge :count="conversation.unreadCount" type="dot">
              <el-avatar :size="48" :src="getAvatarUrl(conversation.otherUserAvatar)">
                {{ conversation.otherUserNickname?.charAt(0) || 'U' }}
              </el-avatar>
            </UnreadBadge>
          </div>

          <div class="conversation-info">
            <div class="info-header">
              <span class="user-name">{{ conversation.otherUserNickname || '用户' }}</span>
              <span v-if="conversation.otherUserIsDeleted" class="deleted-tag">已注销</span>
              <span class="time">{{ formatTime(conversation.lastMessageTime) }}</span>
            </div>
            <div class="last-message">
              <span class="message-content">{{ conversation.lastMessageContent || '暂无消息' }}</span>
            </div>
            <UnreadBadge :count="conversation.unreadCount" type="number" :max="99" class="conversation-badge" />
          </div>
        </div>
      </div>

      <el-empty v-if="conversations.length === 0 && !loading" description="暂无对话" />
    </div>
  </div>
</template>

<script setup>
/* global defineProps, defineEmits */
import { ref, onMounted, onUnmounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import UnreadBadge from '../common/UnreadBadge.vue'
import { useMessageStore } from '../../stores/message'

defineProps({
  conversations: { type: Array, required: true },
  selectedConversationId: { type: String, default: '' },
  totalUnreadCount: { type: Number, default: 0 },
  loading: { type: Boolean, default: false },
  hidden: { type: Boolean, default: false },
  getAvatarUrl: { type: Function, required: true },
  formatTime: { type: Function, required: true }
})

const emit = defineEmits(['select'])

const messageStore = useMessageStore()

// ✅ v1.3.x: 左滑删除相关状态
const swipedConversationId = ref(null) // 当前左滑的对话ID
const touchStartX = ref(0) // 触摸开始的X坐标
const touchStartY = ref(0) // 触摸开始的Y坐标
const currentTranslateX = ref(0) // 当前滑动距离
const isDragging = ref(false) // 是否正在拖动
const deleteButtonWidth = 80 // 删除按钮宽度（px）
const currentDraggingConversation = ref(null) // 当前正在拖动的对话

// ✅ v1.3.x: 处理对话选择
const handleSelectConversation = (conversation) => {
  // 如果当前有左滑的对话，先关闭
  if (swipedConversationId.value && swipedConversationId.value !== conversation.conversationId) {
    swipedConversationId.value = null
    currentTranslateX.value = 0
  }
  emit('select', conversation)
}

// ✅ v1.3.x: 触摸开始（移动端）
const handleTouchStart = (event, conversation) => {
  // 如果点击的是删除按钮，不处理滑动
  if (event.target.closest('.conversation-delete-action')) {
    return
  }
  
  touchStartX.value = event.touches[0].clientX
  touchStartY.value = event.touches[0].clientY
  isDragging.value = false
  
  // 如果当前有其他对话已左滑，先关闭
  if (swipedConversationId.value && swipedConversationId.value !== conversation.conversationId) {
    swipedConversationId.value = null
    currentTranslateX.value = 0
  }
}

// ✅ v1.3.x: 触摸移动（移动端）
const handleTouchMove = (event, conversation) => {
  if (!touchStartX.value) return
  
  const currentX = event.touches[0].clientX
  const currentY = event.touches[0].clientY
  const deltaX = currentX - touchStartX.value
  const deltaY = currentY - touchStartY.value
  
  // 判断是否为水平滑动（水平滑动距离大于垂直滑动距离）
  if (Math.abs(deltaX) > Math.abs(deltaY) && Math.abs(deltaX) > 10) {
    if (!isDragging.value) {
      isDragging.value = true
    }
    event.preventDefault() // 阻止默认滚动
    
    // 只允许向左滑动（deltaX < 0）
    if (deltaX < 0) {
      const translateX = Math.max(deltaX, -deleteButtonWidth)
      currentTranslateX.value = translateX
      swipedConversationId.value = conversation.conversationId
    } else if (swipedConversationId.value === conversation.conversationId) {
      // 向右滑动，恢复位置
      const translateX = Math.min(deltaX, 0)
      currentTranslateX.value = translateX
      if (translateX >= 0) {
        swipedConversationId.value = null
        currentTranslateX.value = 0
      }
    }
  }
}

// ✅ v1.3.x: 触摸结束（移动端）
const handleTouchEnd = (event, conversation) => {
  if (!isDragging.value) {
    touchStartX.value = 0
    touchStartY.value = 0
    return
  }
  
  // 根据滑动距离决定是否显示删除按钮
  if (currentTranslateX.value < -deleteButtonWidth / 2) {
    // 滑动超过一半，显示删除按钮
    currentTranslateX.value = -deleteButtonWidth
    swipedConversationId.value = conversation.conversationId
  } else {
    // 滑动不足一半，恢复原位置
    currentTranslateX.value = 0
    swipedConversationId.value = null
  }
  
  isDragging.value = false
  touchStartX.value = 0
  touchStartY.value = 0
}

// ✅ v1.3.x: 鼠标按下（桌面端）
const handleMouseDown = (event, conversation) => {
  // 如果点击的是删除按钮，不处理滑动
  if (event.target.closest('.conversation-delete-action')) {
    return
  }
  
  touchStartX.value = event.clientX
  touchStartY.value = event.clientY
  isDragging.value = false
  currentDraggingConversation.value = conversation
  
  // 如果当前有其他对话已左滑，先关闭
  if (swipedConversationId.value && swipedConversationId.value !== conversation.conversationId) {
    swipedConversationId.value = null
    currentTranslateX.value = 0
  }
}

// ✅ v1.3.x: 全局鼠标移动（桌面端）
const handleGlobalMouseMove = (event) => {
  if (!currentDraggingConversation.value || !touchStartX.value) return
  
  const currentX = event.clientX
  const currentY = event.clientY
  const deltaX = currentX - touchStartX.value
  const deltaY = currentY - touchStartY.value
  
  // 判断是否为水平拖动（水平拖动距离大于垂直拖动距离）
  if (Math.abs(deltaX) > Math.abs(deltaY) && Math.abs(deltaX) > 10) {
    if (!isDragging.value) {
      isDragging.value = true
    }
    
    const conversation = currentDraggingConversation.value
    
    // 只允许向左拖动（deltaX < 0）
    if (deltaX < 0) {
      const translateX = Math.max(deltaX, -deleteButtonWidth)
      currentTranslateX.value = translateX
      swipedConversationId.value = conversation.conversationId
    } else if (swipedConversationId.value === conversation.conversationId) {
      // 向右拖动，恢复位置
      const translateX = Math.min(deltaX, 0)
      currentTranslateX.value = translateX
      if (translateX >= 0) {
        swipedConversationId.value = null
        currentTranslateX.value = 0
      }
    }
  }
}

// ✅ v1.3.x: 全局鼠标抬起（桌面端）
// eslint-disable-next-line no-unused-vars
const handleGlobalMouseUp = (event) => {
  if (!currentDraggingConversation.value) {
    return
  }
  
  const conversation = currentDraggingConversation.value
  
  if (isDragging.value) {
    // 根据拖动距离决定是否显示删除按钮
    if (currentTranslateX.value < -deleteButtonWidth / 2) {
      // 拖动超过一半，显示删除按钮
      currentTranslateX.value = -deleteButtonWidth
      swipedConversationId.value = conversation.conversationId
    } else {
      // 拖动不足一半，恢复原位置
      currentTranslateX.value = 0
      swipedConversationId.value = null
    }
  }
  
  isDragging.value = false
  touchStartX.value = 0
  touchStartY.value = 0
  currentDraggingConversation.value = null
}

// ✅ v1.3.x: 鼠标离开（桌面端）
const handleMouseLeave = (conversation) => {
  // 如果正在拖动，不处理离开事件
  if (isDragging.value) {
    return
  }
  
  // 桌面端离开时隐藏删除按钮
  if (swipedConversationId.value === conversation.conversationId) {
    swipedConversationId.value = null
    currentTranslateX.value = 0
  }
}

// ✅ v1.3.x: 组件挂载时添加全局事件监听
onMounted(() => {
  document.addEventListener('mousemove', handleGlobalMouseMove)
  document.addEventListener('mouseup', handleGlobalMouseUp)
})

// ✅ v1.3.x: 组件卸载时移除全局事件监听
onUnmounted(() => {
  document.removeEventListener('mousemove', handleGlobalMouseMove)
  document.removeEventListener('mouseup', handleGlobalMouseUp)
})

// ✅ v1.3.x: 获取当前对话的translateX值
const getTranslateX = (conversationId) => {
  if (swipedConversationId.value === conversationId) {
    return currentTranslateX.value
  }
  return 0
}

// ✅ v1.3.x: 删除对话
const handleDeleteConversation = async (conversation) => {
  // 关闭左滑状态
  swipedConversationId.value = null
  currentTranslateX.value = 0
  
  try {
    // 确认删除
    await ElMessageBox.confirm(
      '确定要删除此对话吗？删除后您将无法看到此对话，但对方仍可以看到。',
      '删除对话',
      {
        confirmButtonText: '删除',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    
    // 调用删除API
    const response = await messageStore.deleteConversation(conversation.conversationId)
    
    if (response && response.success) {
      ElMessage.success('对话已删除')
      // Store 中已经更新了 conversations 列表，这里不需要手动更新
    } else {
      ElMessage.error(response?.message || '删除失败')
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除对话失败:', error)
      ElMessage.error('删除对话失败')
    }
  }
}
</script>

<style scoped>
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

/* 统一角标样式已由 UnreadBadge 组件管理，这里可以移除或保留作为后备样式 */

.conversations-list {
  flex: 1;
  overflow-y: auto;
  overflow-x: hidden; /* ✅ v1.3.x: 防止水平滚动 */
}

/* ✅ v1.3.x: 对话项包装器（支持左滑） */
.conversation-item-wrapper {
  position: relative;
  overflow: hidden;
}

/* ✅ v1.3.x: 删除按钮区域 */
.conversation-delete-action {
  position: absolute;
  right: 0;
  top: 0;
  bottom: 0;
  width: 80px;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: #f56c6c;
  z-index: 1;
  margin: 0;
  padding: 0;
  box-sizing: border-box;
  overflow: hidden;
}

.conversation-delete-action .delete-button {
  width: 60px;
  height: 36px;
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

.conversation-item {
  display: flex;
  padding: 15px 20px;
  cursor: pointer;
  transition: transform 0.3s ease, background-color 0.3s ease;
  position: relative;
  background-color: white;
  z-index: 2;
  margin: 0;
  border-bottom: 1px solid #f5f5f5;
}

.conversation-item:hover {
  background-color: #f9f9f9;
}

.conversation-item.active {
  /* ✅ v1.3.x: 使用偏向白色的过渡色（95%白色 + 5%主题色），不透明 */
  background-color: rgb(248, 242, 247);
  border-left: 3px solid var(--primary-color);
}

.conversation-avatar {
  position: relative;
  margin-right: 12px;
}

/* 点状角标样式已由 UnreadBadge 组件管理 */

.conversation-info {
  flex: 1;
  overflow: hidden;
  min-width: 0; /* 确保文本可以正确截断 */
  position: relative; /* 为角标绝对定位提供定位上下文 */
}

.conversation-badge {
  position: absolute;
  top: 50%;
  right: 0;
  transform: translateY(-50%); /* 垂直居中 */
  flex-shrink: 0;
  /* Element Plus el-badge 默认是居中定位，通过 margin-right 调整为靠右边缘 */
  margin-right: 20px; /* 负边距让角标更靠右，可根据实际显示效果调整 */
}

/* 确保 el-badge 内部角标内容靠右边缘显示 */
.conversation-badge :deep(.el-badge__content) {
  right: 0 !important;
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
  padding-right: 50px; /* 为右侧角标预留空间，避免文本被遮挡 */
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

/* ✅ v1.3.x: 左滑状态 */
.conversation-item.swiped-left {
  transform: translateX(-80px);
}
</style>
