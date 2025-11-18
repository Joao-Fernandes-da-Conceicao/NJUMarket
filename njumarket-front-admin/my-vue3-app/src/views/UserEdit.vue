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

    <el-divider content-position="left">地址管理</el-divider>
    <div class="address-panel">
      <div class="address-panel__header">
        <h3>用户地址</h3>
        <UnifiedButton size="small" type="primary" @click="openAddressDialog()">新增地址</UnifiedButton>
      </div>
      <el-table
        :data="addresses"
        stripe
        border
        class="address-table"
        v-loading="addressLoading"
        empty-text="暂无地址信息"
      >
        <el-table-column prop="recipientName" label="收货人" width="120" />
        <el-table-column prop="recipientPhone" label="手机号" width="140" />
        <el-table-column label="地区" min-width="180">
          <template #default="scope">
            {{ formatRegion(scope.row) }}
          </template>
        </el-table-column>
        <el-table-column label="详细地址" min-width="220">
          <template #default="scope">
            {{ scope.row.streetAddress }} {{ scope.row.detailAddress || '' }}
          </template>
        </el-table-column>
        <el-table-column prop="addressLabel" label="标签" width="90" />
        <el-table-column label="状态" width="150">
          <template #default="scope">
            <el-tag size="small" type="success" v-if="scope.row.isDefault">默认</el-tag>
            <el-tag size="small" :type="scope.row.isActive ? 'info' : 'danger'">
              {{ scope.row.isActive ? '启用' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="240" fixed="right">
          <template #default="scope">
            <el-space wrap size="small">
              <UnifiedButton size="small" @click="openAddressDialog(scope.row)">编辑</UnifiedButton>
              <UnifiedButton
                size="small"
                type="primary"
                :disabled="scope.row.isDefault"
                @click="handleSetDefault(scope.row)"
              >
                设为默认
              </UnifiedButton>
              <UnifiedButton
                size="small"
                type="danger"
                plain
                @click="handleDeleteAddress(scope.row)"
              >
                删除
              </UnifiedButton>
            </el-space>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog
      v-model="addressDialogVisible"
      :title="addressDialogTitle"
      width="640px"
      destroy-on-close
    >
      <el-form :model="addressForm" label-width="100px" class="address-form">
        <el-form-item label="收货人">
          <UnifiedInput v-model="addressForm.recipientName" placeholder="请输入收货人" />
        </el-form-item>
        <el-form-item label="手机号">
          <UnifiedInput v-model="addressForm.recipientPhone" placeholder="请输入手机号" />
        </el-form-item>
        <el-form-item label="省份">
          <UnifiedInput v-model="addressForm.province" placeholder="请输入省份" />
        </el-form-item>
        <el-form-item label="城市">
          <UnifiedInput v-model="addressForm.city" placeholder="请输入城市" />
        </el-form-item>
        <el-form-item label="区/县">
          <UnifiedInput v-model="addressForm.district" placeholder="请输入区/县" />
        </el-form-item>
        <el-form-item label="街道">
          <UnifiedInput v-model="addressForm.streetAddress" placeholder="请输入街道" />
        </el-form-item>
        <el-form-item label="详细地址">
          <UnifiedInput v-model="addressForm.detailAddress" placeholder="楼栋/门牌等" />
        </el-form-item>
        <el-form-item label="标签">
          <UnifiedSelect
            v-model="addressForm.addressLabel"
            :options="addressLabelOptions"
            placeholder="请选择标签"
          />
        </el-form-item>
        <el-form-item label="默认地址">
          <el-switch v-model="addressForm.isDefault" />
        </el-form-item>
        <el-form-item label="启用状态">
          <el-switch v-model="addressForm.isActive" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-space>
          <UnifiedButton @click="addressDialogVisible = false">取消</UnifiedButton>
          <UnifiedButton type="primary" @click="saveAddress" :loading="addressSaving">保存</UnifiedButton>
        </el-space>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import UnifiedInput from '../components/common/UnifiedInput.vue'
import UnifiedButton from '../components/common/UnifiedButton.vue'
import UnifiedSelect from '../components/common/UnifiedSelect.vue'
import { usersAPI } from '../api/admin/users'
import { reviewUserPayload } from '../utils/userReview'
import { ElMessage, ElMessageBox } from 'element-plus'

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
      uploadHeaders: {},
      // 地址管理
      addresses: [],
      addressLoading: false,
      addressDialogVisible: false,
      addressSaving: false,
      editingAddressId: '',
      addressForm: {
        recipientName: '',
        recipientPhone: '',
        province: '',
        city: '',
        district: '',
        streetAddress: '',
        detailAddress: '',
        addressLabel: 'HOME',
        isDefault: false,
        isActive: true
      },
      addressLabelOptions: [
        { label: '家庭', value: 'HOME' },
        { label: '学校', value: 'SCHOOL' },
        { label: '公司', value: 'COMPANY' },
        { label: '其他', value: 'OTHER' }
      ]
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

    await this.loadAddresses()
  },
  computed: {
    addressDialogTitle () {
      return this.editingAddressId ? '编辑地址' : '新增地址'
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
    async loadAddresses() {
      this.addressLoading = true
      try {
        const res = await usersAPI.listAddresses(this.form.userId)
        if (res && res.success) {
          this.addresses = Array.isArray(res.data) ? res.data : []
        }
      } finally {
        this.addressLoading = false
      }
    },
    formatRegion(row) {
      if (!row) return ''
      return [row.province, row.city, row.district].filter(Boolean).join(' / ')
    },
    openAddressDialog(address) {
      if (address) {
        this.editingAddressId = address.addressId
        this.addressForm = {
          recipientName: address.recipientName || '',
          recipientPhone: address.recipientPhone || '',
          province: address.province || '',
          city: address.city || '',
          district: address.district || '',
          streetAddress: address.streetAddress || '',
          detailAddress: address.detailAddress || '',
          addressLabel: address.addressLabel || 'HOME',
          isDefault: Boolean(address.isDefault),
          isActive: address.isActive !== false
        }
      } else {
        this.editingAddressId = ''
        this.addressForm = {
          recipientName: '',
          recipientPhone: '',
          province: '',
          city: '',
          district: '',
          streetAddress: '',
          detailAddress: '',
          addressLabel: 'HOME',
          isDefault: false,
          isActive: true
        }
      }
      this.addressDialogVisible = true
    },
    async saveAddress() {
      const requiredFields = ['recipientName', 'recipientPhone', 'province', 'city', 'district', 'streetAddress']
      for (const field of requiredFields) {
        if (!this.addressForm[field] || !this.addressForm[field].toString().trim()) {
          this.$message.error('请完整填写地址必填项')
          return
        }
      }
      this.addressSaving = true
      try {
        let res
        const payload = { ...this.addressForm }
        if (this.editingAddressId) {
          res = await usersAPI.updateAddress(this.form.userId, this.editingAddressId, payload)
        } else {
          res = await usersAPI.createAddress(this.form.userId, payload)
        }
        if (res && res.success) {
          this.$message.success('保存成功')
          this.addressDialogVisible = false
          await this.loadAddresses()
        } else {
          this.$message.error(res?.message || '保存失败')
        }
      } finally {
        this.addressSaving = false
      }
    },
    async handleDeleteAddress(address) {
      if (!address || !address.addressId) return
      try {
        await ElMessageBox.confirm('确认删除该地址吗？此操作不可撤销。', '提示', {
          confirmButtonText: '删除',
          cancelButtonText: '取消',
          type: 'warning'
        })
      } catch (err) {
        return
      }
      const res = await usersAPI.deleteAddress(this.form.userId, address.addressId)
      if (res && res.success) {
        this.$message.success('删除成功')
        await this.loadAddresses()
      } else {
        this.$message.error(res?.message || '删除失败')
      }
    },
    async handleSetDefault(address) {
      if (!address || !address.addressId) return
      const res = await usersAPI.setDefaultAddress(this.form.userId, address.addressId)
      if (res && res.success) {
        this.$message.success('已设为默认地址')
        await this.loadAddresses()
      } else {
        this.$message.error(res?.message || '操作失败')
      }
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

.address-panel {
  margin-top: 16px;
  padding: 16px;
  border: 1px solid #f0f0f0;
  border-radius: 8px;
  background: #fff;
}

.address-panel__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.address-panel__header h3 {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
}

.address-table {
  width: 100%;
}

.address-form :deep(.unified-input),
.address-form :deep(.custom-select) {
  width: 100%;
}

@media (max-width: 600px) {
  .avatar-upload-section {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>

