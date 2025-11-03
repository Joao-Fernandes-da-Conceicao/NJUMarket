<template>
  <div>
    <h2>消息管理</h2>
    <div style="margin:12px 0; display:flex; gap:8px; align-items:center; flex-wrap:wrap;">
      <UnifiedInput v-model="keyword" placeholder="搜索（用户ID/消息内容）" style="width: 360px;" />
      <UnifiedButton type="primary" @click="doSearch">搜索</UnifiedButton>
    </div>
    <el-table :data="list" border style="width: 100%">
      <el-table-column type="expand">
        <template #default="{ row }">
          <div class="conversation-expand">
            <div class="expand-section">
              <h4>会话信息</h4>
              <p>会话ID：{{ row.conversationId }}</p>
              <p>创建时间：{{ row.createdAt || '-' }}</p>
              <p>更新时间：{{ row.updatedAt || '-' }}</p>
              <p>状态：{{ row.status || '-' }}</p>
              <p>用户1可见性：{{ row.user1Visibility ? '可见' : '不可见（已删除）' }}</p>
              <p>用户2可见性：{{ row.user2Visibility ? '可见' : '不可见（已删除）' }}</p>
            </div>
            <div class="expand-section">
              <h4>未读消息数</h4>
              <p>用户1未读：{{ row.user1Count ?? 0 }}</p>
              <p>用户2未读：{{ row.user2Count ?? 0 }}</p>
            </div>
            <div class="expand-section">
              <h4>最后一条消息</h4>
              <p>内容：{{ row.lastMessageContent || '-' }}</p>
              <p>时间：{{ row.lastMessageTime || '-' }}</p>
            </div>
          </div>
        </template>
      </el-table-column>
      <el-table-column prop="conversationId" label="会话ID" width="280"/>
      <el-table-column label="用户1" width="220">
        <template #default="{ row }">
          <div class="user-cell">
            <el-avatar :size="40" :src="getAvatarUrl(getUser1Avatar(row))" class="user-avatar">
              <span v-if="!getUser1Avatar(row)">无头像</span>
            </el-avatar>
            <div class="user-text">
              <p class="user-name">{{ getUser1Name(row) }}</p>
              <p class="user-id">ID: {{ row.userId1 || '-' }}</p>
            </div>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="用户2" width="220">
        <template #default="{ row }">
          <div class="user-cell">
            <el-avatar :size="40" :src="getAvatarUrl(getUser2Avatar(row))" class="user-avatar">
              <span v-if="!getUser2Avatar(row)">无头像</span>
            </el-avatar>
            <div class="user-text">
              <p class="user-name">{{ getUser2Name(row) }}</p>
              <p class="user-id">ID: {{ row.userId2 || '-' }}</p>
            </div>
          </div>
        </template>
      </el-table-column>
      <el-table-column prop="lastMessageContent" label="最后消息（管理端）" min-width="200" show-overflow-tooltip/>
      <el-table-column prop="lastMessageTime" label="最后消息时间（管理端）" width="180" sortable="custom"/>
      <el-table-column prop="user1LastMessageContent" label="用户1可见的最后消息" min-width="200" show-overflow-tooltip/>
      <el-table-column prop="user1LastMessageTime" label="用户1最后消息时间" width="180" sortable="custom"/>
      <el-table-column prop="user2LastMessageContent" label="用户2可见的最后消息" min-width="200" show-overflow-tooltip/>
      <el-table-column prop="user2LastMessageTime" label="用户2最后消息时间" width="180" sortable="custom"/>
      <el-table-column label="创建时间" width="170" sortable="custom">
        <template #default="{ row }">
          {{ formatDateTime(row.createdAt) }}
        </template>
      </el-table-column>
      <el-table-column label="更新时间" width="170" sortable="custom">
        <template #default="{ row }">
          {{ formatDateTime(row.updatedAt) }}
        </template>
      </el-table-column>
      <el-table-column label="用户1可见性" width="130">
        <template #default="{ row }">
          <UnifiedTag :type="row.user1Visibility ? 'success' : 'danger'">
            {{ row.user1Visibility ? '可见' : '不可见（已删除）' }}
          </UnifiedTag>
        </template>
      </el-table-column>
      <el-table-column label="用户2可见性" width="130">
        <template #default="{ row }">
          <UnifiedTag :type="row.user2Visibility ? 'success' : 'danger'">
            {{ row.user2Visibility ? '可见' : '不可见（已删除）' }}
          </UnifiedTag>
        </template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="120">
        <template #default="{ row }">
          <UnifiedTag :type="statusType(row.status)">{{ statusText(row.status) }}</UnifiedTag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="250">
        <template #default="{ row }">
          <UnifiedButton size="small" type="primary" @click="viewDetail(row)">查看详情</UnifiedButton>
          <UnifiedButton size="small" @click="edit(row)">编辑</UnifiedButton>
          <UnifiedButton size="small" type="danger" @click="remove(row)">删除</UnifiedButton>
        </template>
      </el-table-column>
    </el-table>
    <Pagination
      :total="total"
      :current-page="page"
      :page-size="pageSize"
      @page-change="handlePageChange"
      @page-size-change="handleSizeChange"
    />
  </div>
</template>

<script>
import UnifiedButton from '../components/common/UnifiedButton.vue'
import UnifiedInput from '../components/common/UnifiedInput.vue'
import UnifiedTag from '../components/common/UnifiedTag.vue'
import Pagination from '../components/common/Pagination.vue'
import { messagesAPI } from '../api/admin/messages'
import { ElMessage, ElMessageBox } from 'element-plus'

export default {
  name: 'Messages',
  components: { UnifiedButton, UnifiedInput, UnifiedTag, Pagination },
  data() {
    return {
      list: [],
      total: 0,
      page: 1,
      pageSize: 10,
      keyword: ''
    }
  },
  mounted() {
    this.loadData()
  },
  methods: {
    // ✅ 获取用户1昵称（优先使用后端返回的user1信息）
    getUser1Name(row) {
      if (row.user1 && row.user1.nickname) {
        return row.user1.nickname
      }
      return row.userId1 || '-'
    },
    // ✅ 获取用户1头像（优先使用后端返回的user1信息）
    getUser1Avatar(row) {
      if (row.user1 && row.user1.avatar) {
        return row.user1.avatar
      }
      return ''
    },
    // ✅ 获取用户2昵称（优先使用后端返回的user2信息）
    getUser2Name(row) {
      if (row.user2 && row.user2.nickname) {
        return row.user2.nickname
      }
      return row.userId2 || '-'
    },
    // ✅ 获取用户2头像（优先使用后端返回的user2信息）
    getUser2Avatar(row) {
      if (row.user2 && row.user2.avatar) {
        return row.user2.avatar
      }
      return ''
    },
    // ✅ 获取头像完整URL
    getAvatarUrl(avatarUrl) {
      if (!avatarUrl) return 'http://localhost:8080/uploads/avatars/default-avatar.png'
      // 如果已经是完整URL，直接返回
      if (avatarUrl.startsWith('http')) return avatarUrl
      // 如果包含完整路径，直接返回
      if (avatarUrl.includes('/')) return avatarUrl
      // 从URL中提取文件名
      const fileName = avatarUrl.split('/').pop()
      return `http://localhost:8080/uploads/avatars/${fileName}`
    },
    statusText(status) {
      const map = {
        'ACTIVE': '活跃',
        'DELETED': '已删除',
        'BLOCKED': '已屏蔽'
      }
      return map[status] || (status || '-')
    },
    statusType(status) {
      const map = {
        'ACTIVE': 'success',
        'DELETED': 'default',
        'BLOCKED': 'danger'
      }
      return map[status] || 'info'
    },
    // ✅ 格式化日期时间（将 ISO 格式转换为可读格式）
    formatDateTime(dateTimeStr) {
      if (!dateTimeStr) return '-'
      try {
        // 处理 ISO 格式：2025-10-26T10:56:55
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
    async loadData() {
      try {
        const res = await messagesAPI.conversations(this.page, this.pageSize, this.keyword)
        if (res && res.success) {
          this.list = res.data?.list || res.data || []
          this.total = res.data?.total ?? this.list.length
        }
      } catch (error) {
        ElMessage.error('加载会话列表失败')
        console.error('加载会话列表失败:', error)
      }
    },
    doSearch() {
      this.keyword = (this.keyword || '').trim()
      this.page = 1
      this.loadData()
    },
    handlePageChange(p) {
      this.page = p
      this.loadData()
    },
    handleSizeChange(s) {
      this.pageSize = s
      this.page = 1
      this.loadData()
    },
    viewDetail(row) {
      this.$router.push(`/messages/${row.conversationId}/detail`)
    },
    edit(row) {
      this.$router.push(`/messages/${row.conversationId}/edit`)
    },
    async remove(row) {
      try {
        await ElMessageBox.confirm(
          `确定要删除会话 "${row.conversationId}" 吗？此操作将删除该会话及其所有消息，且不可恢复。`,
          '确认删除',
          {
            confirmButtonText: '确定',
            cancelButtonText: '取消',
            type: 'warning'
          }
        )
        
        const res = await messagesAPI.deleteConversation(row.conversationId)
        if (res && res.success) {
          ElMessage.success('删除成功')
          this.loadData()
        } else {
          ElMessage.error(res?.message || '删除失败')
        }
      } catch (error) {
        if (error !== 'cancel') {
          ElMessage.error('删除失败')
          console.error('删除会话失败:', error)
        }
      }
    }
  }
}
</script>

<style scoped>
/* 用户信息单元格 */
.user-cell {
  display: flex;
  align-items: center;
  gap: 12px;
}

.user-avatar {
  flex-shrink: 0;
  border: 2px solid var(--primary-color, #6a015e);
}

.user-text {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-width: 0;
}

.user-name {
  margin: 0;
  font-size: 14px;
  font-weight: 500;
  color: #333;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.user-id {
  margin: 0;
  font-size: 12px;
  color: #999;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* 展开行样式 */
.conversation-expand {
  padding: 20px;
  display: flex;
  gap: 30px;
  flex-wrap: wrap;
}

.expand-section {
  flex: 1;
  min-width: 300px;
}

.expand-section h4 {
  margin: 0 0 12px 0;
  font-size: 16px;
  font-weight: normal;
  color: var(--primary-color, #6a015e);
}

.expand-section p {
  margin: 6px 0;
  font-size: 14px;
  color: #666;
}
</style>
