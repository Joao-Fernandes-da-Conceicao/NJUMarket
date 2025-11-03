<template>
  <div>
    <h2>编辑会话</h2>
    <el-form :model="form" label-width="120px" class="edit-form">
      <el-form-item label="会话ID"><span>{{ form.conversationId }}</span></el-form-item>
      <el-form-item label="用户1ID"><span>{{ form.userId1 }}</span></el-form-item>
      <el-form-item label="用户2ID"><span>{{ form.userId2 }}</span></el-form-item>
      <el-form-item label="会话状态">
        <UnifiedSelect 
          v-model="form.status" 
          :options="statusOptions"
          placeholder="请选择会话状态"
        />
      </el-form-item>
      <el-form-item label="用户1可见性">
        <UnifiedSelect 
          v-model="form.user1Visibility" 
          :options="visibilityOptions"
          placeholder="请选择用户1可见性"
        />
      </el-form-item>
      <el-form-item label="用户2可见性">
        <UnifiedSelect 
          v-model="form.user2Visibility" 
          :options="visibilityOptions"
          placeholder="请选择用户2可见性"
        />
      </el-form-item>
      
      <!-- 只读信息 -->
      <el-divider>会话详情（只读）</el-divider>
      <el-form-item label="创建时间"><span>{{ formatDateTime(form.createdAt) }}</span></el-form-item>
      <el-form-item label="更新时间"><span>{{ formatDateTime(form.updatedAt) }}</span></el-form-item>
      <el-form-item label="最后消息内容"><span>{{ form.lastMessageContent || '-' }}</span></el-form-item>
      <el-form-item label="最后消息时间"><span>{{ formatDateTime(form.lastMessageTime) }}</span></el-form-item>
      <el-form-item label="用户1未读数"><span>{{ form.user1Count ?? 0 }}</span></el-form-item>
      <el-form-item label="用户2未读数"><span>{{ form.user2Count ?? 0 }}</span></el-form-item>

      <el-form-item>
        <UnifiedButton type="primary" @click="save">保存</UnifiedButton>
        <UnifiedButton @click="$router.back()">返回</UnifiedButton>
      </el-form-item>
    </el-form>
  </div>
</template>

<script>
import UnifiedButton from '../components/common/UnifiedButton.vue'
import UnifiedSelect from '../components/common/UnifiedSelect.vue'
import { messagesAPI } from '../api/admin/messages'
import { ElMessage } from 'element-plus'

export default {
  name: 'ConversationEdit',
  components: { UnifiedButton, UnifiedSelect },
  data() {
    return {
      form: {
        conversationId: '',
        userId1: '',
        userId2: '',
        status: '',
        user1Visibility: true,
        user2Visibility: true,
        createdAt: '',
        updatedAt: '',
        lastMessageContent: '',
        lastMessageTime: '',
        user1Count: 0,
        user2Count: 0
      },
      // 状态选择器选项
      statusOptions: [
        { label: '活跃', value: 'ACTIVE' },
        { label: '已删除', value: 'DELETED' },
        { label: '已归档', value: 'ARCHIVED' },
        { label: '已屏蔽', value: 'BLOCKED' }
      ],
      // 可见性选择器选项（Boolean类型）
      visibilityOptions: [
        { label: '可见', value: true },
        { label: '不可见（已删除）', value: false }
      ]
    }
  },
  async mounted() {
    const id = this.$route.params.conversationId
    const res = await messagesAPI.get(id)
    if (res && res.success) {
      const c = res.data
      this.form.conversationId = c.conversationId || ''
      this.form.userId1 = c.userId1 || ''
      this.form.userId2 = c.userId2 || ''
      this.form.status = c.status || 'ACTIVE'
      this.form.user1Visibility = c.user1Visibility !== undefined ? c.user1Visibility : true
      this.form.user2Visibility = c.user2Visibility !== undefined ? c.user2Visibility : true
      this.form.createdAt = c.createdAt || ''
      this.form.updatedAt = c.updatedAt || ''
      this.form.lastMessageContent = c.lastMessageContent || ''
      this.form.lastMessageTime = c.lastMessageTime || ''
      this.form.user1Count = c.user1Count ?? 0
      this.form.user2Count = c.user2Count ?? 0
    } else {
      ElMessage.error('获取会话信息失败')
      this.$router.back()
    }
  },
  methods: {
    // ✅ 格式化日期时间（将 ISO 格式转换为可读格式）
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
    async save() {
      try {
        const payload = {
          status: this.form.status,
          user1Visibility: this.form.user1Visibility,
          user2Visibility: this.form.user2Visibility
        }
        
        const res = await messagesAPI.updateFull(this.form.conversationId, payload)
        if (res && res.success) {
          ElMessage.success('保存成功')
          this.$router.back()
        } else {
          ElMessage.error(res?.message || '保存失败')
        }
      } catch (error) {
        ElMessage.error('保存失败')
        console.error('保存会话失败:', error)
      }
    }
  }
}
</script>

<style scoped>
.edit-form {
  max-width: 800px;
}

.edit-form :deep(.custom-select) {
  width: 100%;
}

.edit-form .el-divider {
  margin: 20px 0;
}

.edit-form span {
  color: #666;
}
</style>

