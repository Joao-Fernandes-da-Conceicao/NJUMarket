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
        class="conversation-item"
        :class="{ active: selectedConversationId === conversation.conversationId }"
        @click="$emit('select', conversation)"
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

      <el-empty v-if="conversations.length === 0 && !loading" description="暂无对话" />
    </div>
  </div>
</template>

<script setup>
/* global defineProps, defineEmits */
import UnreadBadge from '../common/UnreadBadge.vue'

defineProps({
  conversations: { type: Array, required: true },
  selectedConversationId: { type: String, default: '' },
  totalUnreadCount: { type: Number, default: 0 },
  loading: { type: Boolean, default: false },
  hidden: { type: Boolean, default: false },
  getAvatarUrl: { type: Function, required: true },
  formatTime: { type: Function, required: true }
})

defineEmits(['select'])
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
</style>
