<template>
  <div>
    <h2>创建管理员</h2>
    <el-form :model="form" label-width="120px" class="edit-form">
      <el-form-item label="用户名" required>
        <UnifiedInput v-model="form.username" placeholder="请输入用户名" />
      </el-form-item>
      
      <el-form-item label="密码" required>
        <UnifiedInput 
          v-model="form.password" 
          type="password" 
          placeholder="请输入密码（至少6位）" 
          show-password
        />
        <div style="font-size: 12px; color: #999; margin-top: 4px;">
          提示：密码长度不能少于6位
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
        <span style="color: var(--primary-color, #6a015e);">普通管理员</span>
        <div style="font-size: 12px; color: #999; margin-top: 4px; margin-left: auto; width: fit-content;">
          提示：新创建的管理员默认为普通管理员，不允许创建系统管理员
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
      
      <el-form-item label="账户状态">
        <span style="color: var(--primary-color, #6a015e);">活跃</span>
        <div style="font-size: 12px; color: #999; margin-top: 4px; margin-left: auto; width: fit-content;">
          提示：新创建的管理员默认为活跃状态
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

      <el-form-item>
        <UnifiedButton type="primary" @click="create">创建</UnifiedButton>
        <UnifiedButton @click="$router.back()">取消</UnifiedButton>
      </el-form-item>
    </el-form>
  </div>
</template>

<script>
import UnifiedInput from '../components/common/UnifiedInput.vue'
import UnifiedButton from '../components/common/UnifiedButton.vue'
import { ElMessage } from 'element-plus'

export default {
  name: 'AdminCreate',
  components:{ UnifiedInput, UnifiedButton },
  data(){
    return {
      form: {
        username: '', password: '', realName: '', email: '',
        department: '', position: '', permissions: '', remark: ''
      }
    }
  },
  methods:{
    async create(){
      // 验证必填字段
      if (!this.form.username || !this.form.username.trim()) {
        ElMessage.error('用户名不能为空')
        return
      }
      if (!this.form.password || !this.form.password.trim()) {
        ElMessage.error('密码不能为空')
        return
      }
      if (this.form.password.trim().length < 6) {
        ElMessage.error('密码长度不能少于6位')
        return
      }

      // 构建payload
      const payload = {
        username: this.form.username.trim(),
        password: this.form.password.trim(),
        realName: this.form.realName?.trim() || null,
        email: this.form.email?.trim() || null,
        department: this.form.department?.trim() || null,
        position: this.form.position?.trim() || null,
        permissions: this.form.permissions?.trim() || null,
        remark: this.form.remark?.trim() || null
      }

      try {
        const { adminsAPI } = await import('../api/admin/admins')
        const res = await adminsAPI.create(payload)
        if (res && res.success) {
          ElMessage.success('创建管理员成功')
          this.$router.push('/admins')
        } else {
          ElMessage.error(res?.message || '创建失败')
        }
      } catch (error) {
        ElMessage.error('创建失败')
        console.error('创建管理员失败:', error)
      }
    }
  }
}
</script>

<style scoped>
.edit-form { max-width: 720px; }
.edit-form :deep(.unified-input) { width: 100%; }
</style>

