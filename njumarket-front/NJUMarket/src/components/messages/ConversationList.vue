<template>
  <div class="conversations-panel" :class="{ hidden: hidden }">
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
        @click="$emit('select', conversation)"
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
</template>

<script setup>
/* global defineProps, defineEmits */

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
</style>
