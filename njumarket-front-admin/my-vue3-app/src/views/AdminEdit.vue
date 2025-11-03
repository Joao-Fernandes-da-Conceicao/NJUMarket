<template>
  <div>
    <h2>编辑管理员</h2>
    <el-form :model="form" label-width="120px" class="edit-form">
      <el-form-item label="管理员ID"><span>{{ form.adminId }}</span></el-form-item>
      
      <el-form-item label="用户名" required>
        <UnifiedInput v-model="form.username" placeholder="请输入用户名" />
      </el-form-item>
      
      <el-form-item label="密码">
        <UnifiedInput 
          v-model="form.password" 
          type="password" 
          placeholder="留空则不修改密码（至少6位）" 
          show-password
        />
        <div style="font-size: 12px; color: #999; margin-top: 4px;">
          提示：留空则不修改密码，输入新密码将重置该管理员的密码
        </div>
      </el-form-item>
      
      <el-form-item label="真实姓名">
        <UnifiedInput v-model="form.realName" placeholder="请输入真实姓名" />
      </el-form-item>
      
      <el-form-item label="邮箱">
        <UnifiedInput v-model="form.email" placeholder="请输入邮箱" />
      </el-form-item>
      
      <el-form-item label="部门">
        <UnifiedInput v-model="form.department" placeholder="请输入部门" />
      </el-form-item>
      
      <el-form-item label="职位">
        <UnifiedInput v-model="form.position" placeholder="请输入职位" />
      </el-form-item>
      
      <el-form-item label="管理员级别">
        <span style="color: var(--primary-color, #6a015e);">{{ form.adminLevel === 'system' ? '系统管理员' : form.adminLevel === 'administrator' ? '普通管理员' : form.adminLevel || '-' }}</span>
        <div style="font-size: 12px; color: #999; margin-top: 4px; margin-left: auto; width: fit-content;">
          提示：管理员级别为固定字段，不可修改
        </div>
      </el-form-item>
      
      <el-form-item label="权限列表">
        <UnifiedInput 
          v-model="form.permissions" 
          placeholder="权限列表（JSON格式或逗号分隔）" 
          type="textarea"
          :rows="3"
        />
      </el-form-item>
      
      <el-form-item label="账户状态" required>
        <UnifiedSelect 
          v-if="form.adminLevel !== 'system'"
          v-model="form.accountStatus" 
          :options="accountStatusOptions"
          placeholder="请选择账户状态"
        />
        <span v-else style="color: var(--primary-color, #6a015e);">{{ form.accountStatus === 'ACTIVE' ? '活跃' : form.accountStatus === 'SUSPENDED' ? '已暂停' : form.accountStatus === 'BANNED' ? '已封禁' : form.accountStatus || '-' }}</span>
        <div v-if="form.adminLevel === 'system'" style="font-size: 12px; color: #999; margin-top: 4px; margin-left: auto; width: fit-content;">
          提示：系统管理员的账户状态不允许修改
        </div>
      </el-form-item>
      
      <el-form-item label="备注">
        <UnifiedInput 
          v-model="form.remark" 
          placeholder="备注信息" 
          type="textarea"
          :rows="3"
        />
      </el-form-item>

      <!-- 只读字段：客观信息 -->
      <el-divider>客观信息（不可编辑）</el-divider>
      <el-form-item label="创建时间">
        <span>{{ formatDateTime(form.createTime) }}</span>
      </el-form-item>
      <el-form-item label="更新时间">
        <span>{{ formatDateTime(form.updateTime) }}</span>
      </el-form-item>
      <el-form-item label="最后登录时间">
        <span>{{ formatDateTime(form.lastLoginTime) }}</span>
      </el-form-item>
      <el-form-item label="最后登录IP">
        <span>{{ form.lastLoginIp || '-' }}</span>
      </el-form-item>
      <el-form-item label="登录次数">
        <span>{{ form.loginCount ?? 0 }}</span>
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
import { ElMessage } from 'element-plus'

export default {
  name: 'AdminEdit',
  components:{ UnifiedInput, UnifiedButton, UnifiedSelect },
  data(){
    return {
      form: {
        adminId: '', username: '', password: '', realName: '', email: '',
        department: '', position: '', adminLevel: '', permissions: '',
        accountStatus: '', remark: '',
        // 只读字段
        createTime: '', updateTime: '', lastLoginTime: '', lastLoginIp: '', loginCount: 0
      },
      // 账户状态选择器选项
      accountStatusOptions: [
        { label: '活跃', value: 'ACTIVE' },
        { label: '已暂停', value: 'SUSPENDED' },
        { label: '已封禁', value: 'BANNED' }
      ]
    }
  },
  async mounted(){
    // ✅ 加载要编辑的管理员信息
    const id = this.$route.params.adminId
    const { adminsAPI } = await import('../api/admin/admins')
    const res = await adminsAPI.get(id)
    if (res && res.success) {
      const admin = res.data
      this.form.adminId = admin.adminId || ''
      this.form.username = admin.username || ''
      this.form.password = '' // 密码不显示
      this.form.realName = admin.realName || ''
      this.form.email = admin.email || ''
      this.form.department = admin.department || ''
      this.form.position = admin.position || ''
      this.form.adminLevel = admin.adminLevel || 'administrator'
      this.form.permissions = admin.permissions || ''
      this.form.accountStatus = admin.accountStatus || 'ACTIVE'
      this.form.remark = admin.remark || ''
      // 只读字段
      this.form.createTime = admin.createTime
      this.form.updateTime = admin.updateTime
      this.form.lastLoginTime = admin.lastLoginTime
      this.form.lastLoginIp = admin.lastLoginIp
      this.form.loginCount = admin.loginCount || 0
    } else {
      ElMessage.error(res?.message || '加载管理员信息失败')
      this.$router.back()
    }
  },
  methods:{
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
    async save(){
      // 验证必填字段
      if (!this.form.username || !this.form.username.trim()) {
        ElMessage.error('用户名不能为空')
        return
      }
      // ✅ 如果不是系统管理员，才需要验证账户状态
      if (this.form.adminLevel !== 'system' && !this.form.accountStatus) {
        ElMessage.error('请选择账户状态')
        return
      }

      // 构建payload（只包含要更新的字段，不包括adminLevel）
      const payload = {}
      if (this.form.username) payload.username = this.form.username.trim()
      if (this.form.password && this.form.password.trim()) {
        // 如果密码不为空，才包含在payload中
        if (this.form.password.trim().length < 6) {
          ElMessage.error('密码长度不能少于6位')
          return
        }
        payload.password = this.form.password.trim()
      }
      if (this.form.realName !== undefined) payload.realName = this.form.realName?.trim() || null
      if (this.form.email !== undefined) payload.email = this.form.email?.trim() || null
      if (this.form.department !== undefined) payload.department = this.form.department?.trim() || null
      if (this.form.position !== undefined) payload.position = this.form.position?.trim() || null
      // ✅ adminLevel不再作为可编辑字段，不包含在payload中
      if (this.form.permissions !== undefined) payload.permissions = this.form.permissions?.trim() || null
      // ✅ 如果不是系统管理员，才包含accountStatus在payload中
      if (this.form.adminLevel !== 'system' && this.form.accountStatus) {
        payload.accountStatus = this.form.accountStatus
      }
      if (this.form.remark !== undefined) payload.remark = this.form.remark?.trim() || null

      try {
        const { adminsAPI } = await import('../api/admin/admins')
        const res = await adminsAPI.updateFull(this.form.adminId, payload)
        if (res && res.success) {
          ElMessage.success('保存成功')
          this.$router.back()
        } else {
          ElMessage.error(res?.message || '保存失败')
        }
      } catch (error) {
        ElMessage.error('保存失败')
        console.error('保存管理员信息失败:', error)
      }
    }
  }
}
</script>

<style scoped>
.edit-form { max-width: 720px; }
.edit-form :deep(.unified-input) { width: 100%; }
.edit-form :deep(.custom-select) { width: 100%; }
</style>

