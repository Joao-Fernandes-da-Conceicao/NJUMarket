<template>
  <div class="conversation-detail">
    <h2>会话详情</h2>
    
    <!-- 会话信息 -->
    <div class="conversation-info">
      <h3>会话信息</h3>
      <div class="info-grid">
        <div class="info-item">
          <label>会话ID：</label>
          <span>{{ conversation.conversationId || '-' }}</span>
        </div>
        <div class="info-item">
          <label>用户1：</label>
          <span>{{ getUser1Name() }} ({{ conversation.userId1 || '-' }})</span>
        </div>
        <div class="info-item">
          <label>用户2：</label>
          <span>{{ getUser2Name() }} ({{ conversation.userId2 || '-' }})</span>
        </div>
        <div class="info-item">
          <label>状态：</label>
          <UnifiedTag :type="statusType(conversation.status)">{{ statusText(conversation.status) }}</UnifiedTag>
        </div>
        <div class="info-item">
          <label>创建时间：</label>
          <span>{{ formatDateTime(conversation.createdAt) }}</span>
        </div>
        <div class="info-item">
          <label>更新时间：</label>
          <span>{{ formatDateTime(conversation.updatedAt) }}</span>
        </div>
        <div class="info-item">
          <label>最后消息（管理端）：</label>
          <span>{{ conversation.lastMessageContent || '-' }}</span>
        </div>
        <div class="info-item">
          <label>最后消息时间（管理端）：</label>
          <span>{{ formatDateTime(conversation.lastMessageTime) }}</span>
        </div>
        <div class="info-item">
          <label>用户1可见的最后消息：</label>
          <span>{{ conversation.user1LastMessageContent || '-' }}</span>
        </div>
        <div class="info-item">
          <label>用户1最后消息时间：</label>
          <span>{{ formatDateTime(conversation.user1LastMessageTime) }}</span>
        </div>
        <div class="info-item">
          <label>用户2可见的最后消息：</label>
          <span>{{ conversation.user2LastMessageContent || '-' }}</span>
        </div>
        <div class="info-item">
          <label>用户2最后消息时间：</label>
          <span>{{ formatDateTime(conversation.user2LastMessageTime) }}</span>
        </div>
        <div class="info-item">
          <label>用户1未读：</label>
          <span>{{ conversation.user1Count ?? 0 }}</span>
        </div>
        <div class="info-item">
          <label>用户2未读：</label>
          <span>{{ conversation.user2Count ?? 0 }}</span>
        </div>
        <div class="info-item">
          <label>用户1可见性：</label>
          <UnifiedTag :type="conversation.user1Visibility ? 'success' : 'danger'">
            {{ conversation.user1Visibility ? '可见' : '不可见（已删除）' }}
          </UnifiedTag>
        </div>
        <div class="info-item">
          <label>用户2可见性：</label>
          <UnifiedTag :type="conversation.user2Visibility ? 'success' : 'danger'">
            {{ conversation.user2Visibility ? '可见' : '不可见（已删除）' }}
          </UnifiedTag>
        </div>
      </div>
    </div>

    <!-- 消息列表 -->
    <div class="messages-section">
      <div class="messages-header">
        <h3>消息列表</h3>
        <UnifiedButton type="primary" @click="loadMessages">刷新</UnifiedButton>
      </div>
      <div class="messages-list" v-loading="messagesLoading">
        <div v-if="messages.length === 0" class="empty-messages">
          <p>暂无消息</p>
        </div>
        <div
          v-for="message in messages"
          :key="message.messageId"
          class="message-item"
          :class="{ 'message-deleted': isMessageDeleted(message) }"
        >
          <!-- 消息气泡（可点击编辑） -->
          <div class="message-bubble-wrapper" @click="editMessage(message)">
            <div class="message-bubble" :class="getMessageBubbleClass(message)">
              <div class="message-header">
                <el-avatar :size="32" :src="getAvatarUrl(getSenderAvatar(message))">
                  <span v-if="!getSenderAvatar(message)">U</span>
                </el-avatar>
                <div class="message-sender-info">
                  <span class="sender-name">{{ getSenderName(message) }}</span>
                  <span class="sender-id">ID: {{ message.senderId }}</span>
                </div>
                <span class="message-time">{{ formatDateTime(message.createdAt) }}</span>
              </div>
              <div class="message-content-text">
                <p v-if="message.messageType === 'TEXT'">{{ message.content }}</p>
                <div v-else-if="message.messageType === 'IMAGE' && message.imageUrl" class="message-image">
                  <img :src="getImageUrl(message.imageUrl)" alt="消息图片" />
                </div>
                <p v-else>{{ message.content || '[非文本消息]' }}</p>
              </div>
            </div>
          </div>
          
          <!-- 商品/订单卡片 -->
          <div class="message-cards" v-if="message.commodityId || message.orderId">
            <!-- 商品卡片 -->
            <div v-if="message.commodityId && message.commodity" class="commodity-card">
              <div class="card-main">
                <div class="card-image" v-if="getCommodityImage(message.commodity)">
                  <img :src="getImageUrl(getCommodityImage(message.commodity))" :alt="message.commodity.title" />
                </div>
                <div class="card-content">
                  <h4 class="card-title">{{ message.commodity.title || '-' }}</h4>
                  <div class="card-price">¥{{ formatPrice(message.commodity.price) }}</div>
                  <div class="card-tags">
                    <UnifiedTag size="small" type="info" v-if="message.commodity.category">{{ message.commodity.category }}</UnifiedTag>
                    <UnifiedTag size="small" type="warning" v-if="message.commodity.conditionLevel">{{ message.commodity.conditionLevel }}</UnifiedTag>
                  </div>
                </div>
              </div>
            </div>
            <!-- 订单卡片 -->
            <div v-if="message.orderId && message.order" class="order-card">
              <div class="card-main">
                <div class="card-image" v-if="getOrderImage(message.order)">
                  <img :src="getImageUrl(getOrderImage(message.order))" :alt="message.order.commoditySnapshotTitle || '商品'" />
                </div>
                <div class="card-content">
                  <h4 class="card-title">{{ message.order.commoditySnapshotTitle || '商品' }}</h4>
                  <div class="card-price">¥{{ formatPrice(message.order.payAmount || message.order.commoditySnapshotPrice || 0) }}</div>
                  <div class="card-status">
                    <UnifiedTag :type="getOrderStatusType(message.order.orderStatus)" size="small">
                      {{ getOrderStatusText(message.order.orderStatus) }}
                    </UnifiedTag>
                  </div>
                </div>
              </div>
            </div>
          </div>
          
          <!-- 消息操作区域（可见性和删除） -->
          <div class="message-actions">
            <div class="visibility-info">
              <span class="visibility-label">可见性：</span>
              <UnifiedTag :type="message.deletedBySender ? 'danger' : 'success'" size="small">
                发送方：{{ message.deletedBySender ? '已删除' : '可见' }}
              </UnifiedTag>
              <UnifiedTag :type="message.deletedByReceiver ? 'danger' : 'success'" size="small" style="margin-left: 8px;">
                接收方：{{ message.deletedByReceiver ? '已删除' : '可见' }}
              </UnifiedTag>
            </div>
            <div class="action-buttons">
              <UnifiedButton size="small" type="primary" @click.stop="editMessage(message)">编辑</UnifiedButton>
              <UnifiedButton size="small" type="danger" @click.stop="deleteMessage(message)">删除</UnifiedButton>
            </div>
          </div>
        </div>
      </div>
      
      <!-- 分页 -->
      <Pagination
        :total="messagesTotal"
        :current-page="messagesPage"
        :page-size="messagesPageSize"
        @page-change="handleMessagesPageChange"
        @page-size-change="handleMessagesSizeChange"
      />
    </div>

    <!-- 返回按钮 -->
    <div class="actions">
      <UnifiedButton @click="$router.back()">返回</UnifiedButton>
    </div>

    <!-- 消息编辑对话框 -->
    <el-dialog
      v-model="messageEditDialogVisible"
      title="编辑消息可见性"
      width="500px"
    >
      <el-form :model="messageEditForm" label-width="120px">
        <el-form-item label="消息ID">
          <span>{{ messageEditForm.messageId }}</span>
        </el-form-item>
        <el-form-item label="发送方">
          <span>{{ getSenderName(messageEditForm) }} ({{ messageEditForm.senderId }})</span>
        </el-form-item>
        <el-form-item label="接收方">
          <span>{{ getReceiverName(messageEditForm) }} ({{ messageEditForm.receiverId }})</span>
        </el-form-item>
        <el-form-item label="消息内容">
          <span>{{ messageEditForm.content || '-' }}</span>
        </el-form-item>
        <el-form-item label="发送方可见性">
          <UnifiedSelect
            v-model="messageEditForm.deletedBySender"
            :options="visibilityOptions"
            placeholder="请选择发送方可见性"
          />
        </el-form-item>
        <el-form-item label="接收方可见性">
          <UnifiedSelect
            v-model="messageEditForm.deletedByReceiver"
            :options="visibilityOptions"
            placeholder="请选择接收方可见性"
          />
        </el-form-item>
        <el-form-item label="已读状态">
          <UnifiedSelect
            v-model="messageEditForm.isRead"
            :options="readStatusOptions"
            placeholder="请选择已读状态"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <UnifiedButton @click="messageEditDialogVisible = false">取消</UnifiedButton>
        <UnifiedButton type="primary" @click="saveMessageEdit">保存</UnifiedButton>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import UnifiedButton from '../components/common/UnifiedButton.vue'
import UnifiedTag from '../components/common/UnifiedTag.vue'
import UnifiedSelect from '../components/common/UnifiedSelect.vue'
import Pagination from '../components/common/Pagination.vue'
import { messagesAPI } from '../api/admin/messages'
import { commoditiesAPI } from '../api/admin/commodities'
import { ordersAPI } from '../api/admin/orders'
import { ElMessage, ElMessageBox } from 'element-plus'

export default {
  name: 'ConversationDetail',
  components: { UnifiedButton, UnifiedTag, UnifiedSelect, Pagination },
  data() {
    return {
      conversation: {},
      messages: [],
      messagesLoading: false,
      messagesTotal: 0,
      messagesPage: 1,
      messagesPageSize: 10,
      messageEditDialogVisible: false,
      messageEditForm: {
        messageId: '',
        senderId: '',
        receiverId: '',
        content: '',
        deletedBySender: false,
        deletedByReceiver: false,
        isRead: false
      },
      visibilityOptions: [
        { label: '可见', value: false },
        { label: '已删除（不可见）', value: true }
      ],
      readStatusOptions: [
        { label: '未读', value: false },
        { label: '已读', value: true }
      ],
      // 缓存商品和订单详情
      commodityCache: {},
      orderCache: {}
    }
  },
  async mounted() {
    const conversationId = this.$route.params.conversationId
    await this.loadConversation(conversationId)
    await this.loadMessages()
  },
  methods: {
    // ✅ 格式化日期时间
    formatDateTime(dateTimeStr) {
      if (!dateTimeStr) return '-'
      try {
        const date = new Date(dateTimeStr)
        if (isNaN(date.getTime())) return dateTimeStr
        
        const year = date.getFullYear()
        const month = String(date.getMonth() + 1).padStart(2, '0')
        const day = String(date.getDate()).padStart(2, '0')
        const hours = String(date.getHours()).padStart(2, '0')
        const minutes = String(date.getMinutes()).padStart(2, '0')
        const seconds = String(date.getSeconds()).padStart(2, '0')
        
        return `${year}-${month}-${day} ${hours}:${minutes}:${seconds}`
      } catch (error) {
        return dateTimeStr
      }
    },
    // ✅ 格式化价格
    formatPrice(price) {
      if (price == null) return '0.00'
      return Number(price).toFixed(2)
    },
    // ✅ 获取用户1昵称
    getUser1Name() {
      if (this.conversation.user1 && this.conversation.user1.nickname) {
        return this.conversation.user1.nickname
      }
      return this.conversation.userId1 || '-'
    },
    // ✅ 获取用户2昵称
    getUser2Name() {
      if (this.conversation.user2 && this.conversation.user2.nickname) {
        return this.conversation.user2.nickname
      }
      return this.conversation.userId2 || '-'
    },
    // ✅ 获取发送者昵称
    getSenderName(message) {
      // 判断发送者是user1还是user2
      if (message.senderId === this.conversation.userId1) {
        return this.getUser1Name()
      } else if (message.senderId === this.conversation.userId2) {
        return this.getUser2Name()
      }
      return message.senderId || '-'
    },
    // ✅ 获取接收者昵称
    getReceiverName(message) {
      // 判断接收者是user1还是user2
      if (message.receiverId === this.conversation.userId1) {
        return this.getUser1Name()
      } else if (message.receiverId === this.conversation.userId2) {
        return this.getUser2Name()
      }
      return message.receiverId || '-'
    },
    // ✅ 获取发送者头像
    getSenderAvatar(message) {
      if (message.senderId === this.conversation.userId1) {
        return this.conversation.user1?.avatar
      } else if (message.senderId === this.conversation.userId2) {
        return this.conversation.user2?.avatar
      }
      return ''
    },
    // ✅ 获取头像URL
    getAvatarUrl(avatarUrl) {
      if (!avatarUrl) return 'http://localhost:8080/uploads/avatars/default-avatar.png'
      if (avatarUrl.startsWith('http')) return avatarUrl
      if (avatarUrl.includes('/')) return avatarUrl
      const fileName = avatarUrl.split('/').pop()
      return `http://localhost:8080/uploads/avatars/${fileName}`
    },
    // ✅ 获取图片URL
    getImageUrl(imageUrl) {
      if (!imageUrl) return ''
      if (imageUrl.startsWith('http')) return imageUrl
      if (imageUrl.includes('/')) return imageUrl
      return `http://localhost:8080/uploads/${imageUrl}`
    },
    // ✅ 获取商品图片
    getCommodityImage(commodity) {
      if (!commodity || !commodity.images) return null
      if (Array.isArray(commodity.images)) {
        return commodity.images[0] || null
      }
      if (typeof commodity.images === 'string') {
        const images = commodity.images.split(',').filter(img => img.trim())
        return images[0] || null
      }
      return null
    },
    // ✅ 获取订单图片
    getOrderImage(order) {
      if (!order) return null
      // 尝试多个可能的字段
      if (order.commoditySnapshotImages) {
        if (typeof order.commoditySnapshotImages === 'string') {
          const images = order.commoditySnapshotImages.split(',').filter(img => img.trim())
          return images[0] || null
        } else if (Array.isArray(order.commoditySnapshotImages)) {
          return order.commoditySnapshotImages[0] || null
        }
      }
      return null
    },
    // ✅ 消息气泡样式类
    getMessageBubbleClass(message) {
      // 根据发送者是user1还是user2决定样式
      if (message.senderId === this.conversation.userId1) {
        return 'bubble-user1'
      } else {
        return 'bubble-user2'
      }
    },
    // ✅ 检查消息是否被删除
    isMessageDeleted(message) {
      return message.deletedBySender || message.deletedByReceiver
    },
    // ✅ 状态文本映射
    statusText(status) {
      const map = {
        'ACTIVE': '活跃',
        'DELETED': '已删除',
        'ARCHIVED': '已归档',
        'BLOCKED': '已屏蔽'
      }
      return map[status] || (status || '-')
    },
    // ✅ 状态类型映射
    statusType(status) {
      const map = {
        'ACTIVE': 'success',
        'DELETED': 'default',
        'ARCHIVED': 'info',
        'BLOCKED': 'danger'
      }
      return map[status] || 'info'
    },
    // ✅ 订单状态文本映射
    getOrderStatusText(status) {
      const map = {
        'CREATED': '待支付',
        'PAID': '已支付',
        'SHIPPED': '已发货',
        'COMPLETED': '已完成',
        'CANCELLED': '已取消',
        'REFUND_REQUESTED': '申请退款',
        'REFUND_APPROVED': '退款通过',
        'REFUND_REJECTED': '退款被拒'
      }
      return map[status] || (status || '-')
    },
    // ✅ 订单状态类型映射
    getOrderStatusType(status) {
      const map = {
        'CREATED': 'warning',
        'PAID': 'info',
        'SHIPPED': 'primary',
        'COMPLETED': 'success',
        'CANCELLED': 'danger',
        'REFUND_REQUESTED': 'warning',
        'REFUND_APPROVED': 'success',
        'REFUND_REJECTED': 'danger'
      }
      return map[status] || 'info'
    },
    // ✅ 加载会话详情
    async loadConversation(conversationId) {
      try {
        const res = await messagesAPI.get(conversationId)
        if (res && res.success) {
          this.conversation = res.data || {}
        } else {
          ElMessage.error('获取会话详情失败')
          this.$router.back()
        }
      } catch (error) {
        ElMessage.error('获取会话详情失败')
        console.error('加载会话详情失败:', error)
        this.$router.back()
      }
    },
    // ✅ 加载消息列表
    async loadMessages() {
      this.messagesLoading = true
      try {
        const conversationId = this.$route.params.conversationId
        const res = await messagesAPI.listByConversation(conversationId, this.messagesPage, this.messagesPageSize)
        if (res && res.success) {
          const messageList = res.data?.list || res.data || []
          this.messages = messageList
          this.messagesTotal = res.data?.total ?? messageList.length
          
          // ✅ 异步加载商品和订单详情（用于显示卡片）
          await this.loadCommoditiesAndOrders(messageList)
        }
      } catch (error) {
        ElMessage.error('加载消息列表失败')
        console.error('加载消息列表失败:', error)
      } finally {
        this.messagesLoading = false
      }
    },
    // ✅ 加载商品和订单详情
    async loadCommoditiesAndOrders(messageList) {
      const promises = []
      
      for (const message of messageList) {
        // 加载商品详情
        if (message.commodityId && !this.commodityCache[message.commodityId]) {
          promises.push(
            commoditiesAPI.get(message.commodityId)
              .then(res => {
                if (res && res.success && res.data) {
                  this.commodityCache[message.commodityId] = res.data
                  message.commodity = res.data
                }
              })
              .catch(err => console.error('加载商品详情失败:', err))
          )
        } else if (message.commodityId && this.commodityCache[message.commodityId]) {
          // 使用缓存
          message.commodity = this.commodityCache[message.commodityId]
        }
        
        // 加载订单详情
        if (message.orderId && !this.orderCache[message.orderId]) {
          promises.push(
            ordersAPI.get(message.orderId)
              .then(res => {
                if (res && res.success && res.data) {
                  this.orderCache[message.orderId] = res.data
                  message.order = res.data
                }
              })
              .catch(err => console.error('加载订单详情失败:', err))
          )
        } else if (message.orderId && this.orderCache[message.orderId]) {
          // 使用缓存
          message.order = this.orderCache[message.orderId]
        }
      }
      
      await Promise.all(promises)
    },
    // ✅ 分页处理
    handleMessagesPageChange(page) {
      this.messagesPage = page
      this.loadMessages()
    },
    handleMessagesSizeChange(size) {
      this.messagesPageSize = size
      this.messagesPage = 1
      this.loadMessages()
    },
    // ✅ 编辑消息
    editMessage(message) {
      this.messageEditForm = {
        messageId: message.messageId,
        senderId: message.senderId,
        receiverId: message.receiverId,
        content: message.content,
        deletedBySender: message.deletedBySender || false,
        deletedByReceiver: message.deletedByReceiver || false,
        isRead: message.isRead || false
      }
      this.messageEditDialogVisible = true
    },
    // ✅ 保存消息编辑
    async saveMessageEdit() {
      try {
        const payload = {
          deletedBySender: this.messageEditForm.deletedBySender,
          deletedByReceiver: this.messageEditForm.deletedByReceiver,
          isRead: this.messageEditForm.isRead
        }
        
        const res = await messagesAPI.updateMessageFull(this.messageEditForm.messageId, payload)
        if (res && res.success) {
          ElMessage.success('保存成功')
          this.messageEditDialogVisible = false
          await this.loadMessages()
        } else {
          ElMessage.error(res?.message || '保存失败')
        }
      } catch (error) {
        ElMessage.error('保存失败')
        console.error('保存消息编辑失败:', error)
      }
    },
    // ✅ 删除消息
    async deleteMessage(message) {
      try {
        await ElMessageBox.confirm(
          `确定要删除消息 "${message.messageId}" 吗？此操作不可恢复。`,
          '确认删除',
          {
            confirmButtonText: '确定',
            cancelButtonText: '取消',
            type: 'warning'
          }
        )
        
        const res = await messagesAPI.deleteMessage(message.messageId)
        if (res && res.success) {
          ElMessage.success('删除成功')
          await this.loadMessages()
        } else {
          ElMessage.error(res?.message || '删除失败')
        }
      } catch (error) {
        if (error !== 'cancel') {
          ElMessage.error('删除失败')
          console.error('删除消息失败:', error)
        }
      }
    }
  }
}
</script>

<style scoped>
.conversation-detail {
  padding: 20px;
  max-width: 1200px;
  margin: 0 auto;
}

.conversation-info {
  background: white;
  border-radius: 12px;
  padding: 20px;
  margin-bottom: 24px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.conversation-info h3 {
  margin: 0 0 16px 0;
  font-size: 18px;
  font-weight: normal;
  color: var(--primary-color, #6a015e);
}

.info-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
  gap: 16px;
}

.info-item {
  display: flex;
  align-items: center;
  gap: 8px;
}

.info-item label {
  font-weight: 500;
  color: #666;
  min-width: 100px;
}

.info-item span {
  color: #333;
}

.messages-section {
  background: white;
  border-radius: 12px;
  padding: 20px;
  margin-bottom: 24px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.messages-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.messages-header h3 {
  margin: 0;
  font-size: 18px;
  font-weight: normal;
  color: var(--primary-color, #6a015e);
}

.messages-list {
  min-height: 400px;
  max-height: 800px;
  overflow-y: auto;
  padding: 12px;
  background: #f9f9f9;
  border-radius: 8px;
}

.empty-messages {
  text-align: center;
  padding: 60px 20px;
  color: #999;
}

.message-item {
  margin-bottom: 24px;
  padding: 16px;
  background: white;
  border-radius: 12px;
  border: 1px solid #e0e0e0;
}

.message-item.message-deleted {
  opacity: 0.6;
  background: #f5f5f5;
}

.message-bubble-wrapper {
  cursor: pointer;
  transition: all 0.2s ease;
}

.message-bubble-wrapper:hover {
  opacity: 0.8;
}

.message-bubble {
  padding: 12px 16px;
  border-radius: 12px;
  background: #f0f0f0;
}

.message-bubble.bubble-user1 {
  background: #e3f2fd;
  border-left: 3px solid var(--primary-color, #6a015e);
}

.message-bubble.bubble-user2 {
  background: #f3e5f5;
  border-left: 3px solid #9c27b0;
}

.message-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 8px;
}

.message-sender-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.sender-name {
  font-size: 14px;
  font-weight: 500;
  color: #333;
}

.sender-id {
  font-size: 12px;
  color: #999;
}

.message-time {
  font-size: 12px;
  color: #999;
}

.message-content-text {
  color: #333;
  line-height: 1.5;
}

.message-content-text p {
  margin: 0;
  word-wrap: break-word;
}

.message-image {
  margin-top: 8px;
}

.message-image img {
  max-width: 300px;
  max-height: 300px;
  border-radius: 8px;
  object-fit: cover;
}

.message-cards {
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px dashed #e0e0e0;
}

.commodity-card,
.order-card {
  padding: 12px;
  background: #f9f9f9;
  border-radius: 8px;
  border: 1px solid #e0e0e0;
  margin-bottom: 8px;
}

.card-main {
  display: flex;
  gap: 12px;
}

.card-image {
  width: 80px;
  height: 80px;
  flex-shrink: 0;
  border-radius: 6px;
  overflow: hidden;
  background: #fff;
}

.card-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.card-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 6px;
  min-width: 0;
}

.card-title {
  font-size: 14px;
  font-weight: normal;
  color: #333;
  margin: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.card-price {
  font-size: 16px;
  font-weight: normal;
  color: var(--primary-color, #6a015e);
}

.card-tags {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
}

.card-status {
  margin-top: 2px;
}

.message-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px dashed #e0e0e0;
}

.visibility-info {
  display: flex;
  align-items: center;
  gap: 8px;
}

.visibility-label {
  font-size: 14px;
  color: #666;
}

.action-buttons {
  display: flex;
  gap: 8px;
}

.actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}
</style>

