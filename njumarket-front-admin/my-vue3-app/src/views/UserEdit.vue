<template>
  <div>
    <h2>编辑用户</h2>
    <el-form :model="form" label-width="100px" class="edit-form">
      <el-form-item label="用户ID"><span>{{ form.userId }}</span></el-form-item>
      <el-form-item label="用户名">
        <UnifiedInput v-model="form.username" placeholder="请输入用户名" />
      </el-form-item>
      <el-form-item label="手机号">
        <UnifiedInput v-model="form.primaryPhone" placeholder="请输入手机号" />
      </el-form-item>
      <el-form-item label="状态">
        <UnifiedSelect 
          v-model="form.accountStatus" 
          :options="accountStatusOptions"
          placeholder="请选择状态"
        />
      </el-form-item>

      <el-form-item label="昵称">
        <UnifiedInput v-model="form.nickname" placeholder="请输入昵称" />
      </el-form-item>
      <el-form-item label="头像">
        <div class="avatar-upload-section">
          <el-avatar :size="80" :src="getAvatarUrl(form.avatar)" class="avatar-preview">
            <span v-if="!form.avatar">暂无头像</span>
          </el-avatar>
          <div class="upload-controls">
            <el-upload
              :action="uploadUrl"
              :headers="uploadHeaders"
              :show-file-list="false"
              :on-success="handleAvatarSuccess"
              :on-error="handleAvatarError"
              :before-upload="beforeAvatarUpload"
              accept="image/*"
            >
              <UnifiedButton type="primary" size="small">上传头像</UnifiedButton>
            </el-upload>
            <div class="upload-tip">支持 JPG、PNG 格式，建议尺寸 200x200 像素，不超过 2MB</div>
          </div>
        </div>
      </el-form-item>
      <el-form-item label="信用分">
        <UnifiedInput v-model="form.creditScore" type="number" placeholder="0-100" />
      </el-form-item>
      <el-form-item label="买家评分">
        <UnifiedInput v-model="form.buyerRating" type="number" placeholder="0-5" />
      </el-form-item>
      <el-form-item label="卖家评分">
        <UnifiedInput v-model="form.sellerRating" type="number" placeholder="0-5" />
      </el-form-item>
      <el-form-item label="卖出次数">
        <UnifiedInput v-model="form.totalSales" type="number" placeholder="整数" />
      </el-form-item>
      <el-form-item label="购入次数">
        <UnifiedInput v-model="form.totalPurchases" type="number" placeholder="整数" />
      </el-form-item>
      <el-form-item label="会员等级">
        <UnifiedSelect 
          v-model="form.vipLevel" 
          :options="vipLevelOptions"
          placeholder="请选择会员等级"
        />
      </el-form-item>

      <el-form-item>
        <UnifiedButton type="primary" @click="save">保存</UnifiedButton>
        <UnifiedButton @click="$router.back()">返回</UnifiedButton>
      </el-form-item>
    </el-form>
  </div>
</template>

<script>
import UnifiedInput from '../components/common/UnifiedInput.vue'
import UnifiedButton from '../components/common/UnifiedButton.vue'
import UnifiedSelect from '../components/common/UnifiedSelect.vue'
import { usersAPI } from '../api/admin/users'
import { reviewUserPayload } from '../utils/userReview'
import { ElMessage } from 'element-plus'

export default {
  name: 'UserEdit',
  components:{ UnifiedInput, UnifiedButton, UnifiedSelect },
  data(){
    return {
      form: {
        userId: '', username: '', primaryPhone: '', accountStatus: '',
        nickname: '', avatar: '', creditScore: '', buyerRating: '', sellerRating: '',
        totalSales: '', totalPurchases: '', vipLevel: ''
      },
      // 状态选择器选项
      accountStatusOptions: [
        { label: '正常', value: 'ACTIVE' },
        { label: '已暂停', value: 'SUSPENDED' },
        { label: '已封禁', value: 'BANNED' }
      ],
      // 会员等级选择器选项
      vipLevelOptions: [
        { label: '普通', value: 'NORMAL' },
        { label: '青铜', value: 'BRONZE' },
        { label: '白银', value: 'SILVER' },
        { label: '黄金', value: 'GOLD' },
        { label: '铂金', value: 'PLATINUM' }
      ],
      // 头像上传配置
      uploadUrl: 'http://localhost:8080/api/user/profile/avatar',
      uploadHeaders: {}
    }
  },
  async mounted(){
    // 设置上传请求头（使用管理员token）
    const token = localStorage.getItem('adminToken')
    if (token) {
      this.uploadHeaders = {
        'Authorization': `Bearer ${token}`
      }
    }
    
    const id = this.$route.params.userId
    const res = await usersAPI.get(id)
    if (res && res.success) {
      const u = res.data
      this.form.userId = u.userId
      this.form.username = u.username || ''
      this.form.primaryPhone = u.primaryPhone || ''
      this.form.accountStatus = u.accountStatus || ''
      const p = u.profile || {}
      this.form.nickname = p.nickname || ''
      this.form.avatar = p.avatar || ''
      this.form.creditScore = p.creditScore ?? ''
      this.form.buyerRating = p.buyerRating ?? ''
      this.form.sellerRating = p.sellerRating ?? ''
      this.form.totalSales = p.totalSales ?? ''
      this.form.totalPurchases = p.totalPurchases ?? ''
      this.form.vipLevel = p.vipLevel || ''
    }
  },
  methods:{
    // 获取头像URL
    getAvatarUrl(avatarUrl) {
      if (!avatarUrl) return 'http://localhost:8080/uploads/avatars/default-avatar.png'
      // 如果已经是完整URL，直接返回
      if (avatarUrl.startsWith('http')) return avatarUrl
      // 如果是文件名，构建完整URL
      if (avatarUrl.includes('/')) return avatarUrl
      // 从URL中提取文件名
      const fileName = avatarUrl.split('/').pop()
      return `http://localhost:8080/uploads/avatars/${fileName}`
    },
    // 头像上传前检查
    beforeAvatarUpload(file) {
      const isImage = file.type.startsWith('image/')
      const isLt2M = file.size / 1024 / 1024 < 2
      
      if (!isImage) {
        ElMessage.error('只能上传图片文件!')
        return false
      }
      if (!isLt2M) {
        ElMessage.error('图片大小不能超过 2MB!')
        return false
      }
      return true
    },
    // 头像上传成功
    handleAvatarSuccess(response) {
      if (response.success) {
        ElMessage.success('头像上传成功')
        // 后端返回的是ImageUploadDTO，包含imageUrl字段
        const imageUrl = response.data?.imageUrl || response.data
        this.form.avatar = imageUrl
      } else {
        ElMessage.error(response.errorMsg || '头像上传失败')
      }
    },
    // 头像上传失败
    handleAvatarError() {
      ElMessage.error('头像上传失败')
    },
    async save(){
      const payload = { ...this.form }
      const err = reviewUserPayload(payload)
      if (err) { this.$message.error(err); return }
      const id = this.form.userId
      const res = await usersAPI.updateFull(id, payload)
      if (res && res.success) {
        this.$message.success('保存成功')
        this.$router.back()
      } else {
        this.$message.error(res?.message || '保存失败')
      }
    }
  }
}
</script>

<style scoped>
.edit-form { max-width: 720px; }
.edit-form :deep(.unified-input) { width: 100%; }
.edit-form :deep(.custom-select) { width: 100%; }

/* 头像上传区域 */
.avatar-upload-section {
  display: flex;
  align-items: center;
  gap: 20px;
}

.avatar-preview {
  flex-shrink: 0;
  border: 2px solid var(--primary-color, #6a015e);
  border-radius: 8px;
}

.upload-controls {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.upload-tip {
  font-size: 12px;
  color: #999;
  line-height: 1.5;
}

@media (max-width: 600px) {
  .avatar-upload-section {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>

