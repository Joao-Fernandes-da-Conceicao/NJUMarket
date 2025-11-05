<template>
  <div class="login-page">
    <div class="login-container">
      <div class="login-form-wrapper">
        <div class="form-header">
          <h2 class="text-primary">管理员登录</h2>
          <p class="text-secondary">NJUMarketAdmin</p>
        </div>

        <el-form
          ref="loginFormRef"
          :model="loginForm"
          :rules="loginRules"
          class="login-form"
          @submit.prevent="handleLogin"
        >
          <el-form-item prop="identifier">
            <UnifiedInput
              v-model="loginForm.identifier"
              placeholder="请输入用户名或手机号"
              size="large"
              :prefix-icon="User"
              @keyup.enter="handleLogin"
            />
          </el-form-item>

          <el-form-item prop="password">
            <UnifiedInput
              v-model="loginForm.password"
              type="password"
              placeholder="请输入密码"
              size="large"
              :prefix-icon="Lock"
              show-password
              @keyup.enter="handleLogin"
            />
          </el-form-item>

          <el-form-item>
            <UnifiedButton
              type="primary"
              size="large"
              class="login-btn"
              :loading="loginLoading"
              @click="handleLogin"
            >
              登录
            </UnifiedButton>
          </el-form-item>
        </el-form>

        <div class="form-footer">
          <div class="login-methods">
            <!-- 管理端不提供验证码登录，这里预留结构，无功能按钮 -->
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import UnifiedInput from '../components/common/UnifiedInput.vue'
import UnifiedButton from '../components/common/UnifiedButton.vue'
import { User, Lock } from '@element-plus/icons-vue'

export default {
  name: 'AdminLogin',
  components:{
    UnifiedInput,
    UnifiedButton
  },
  data(){
    return {
      loginForm: { identifier: '', password: '' },
      loginRules: {
        identifier: [
          { required: true, message: '请输入用户名或手机号', trigger: 'blur' },
          { min: 3, message: '至少3个字符', trigger: 'blur' }
        ],
        password: [
          { required: true, message: '请输入密码', trigger: 'blur' },
          { min: 6, message: '至少6位密码', trigger: 'blur' }
        ]
      },
      loginLoading: false,
      User,
      Lock
    }
  },
  methods:{
    async handleLogin(){
      const form = this.$refs.loginFormRef
      if (!form) return
      form.validate(async (valid) => {
        if (!valid) return
        this.loginLoading = true
        try {
          const { authAPI } = await import('../api/admin/auth')
          // 管理端后端接收的是 username/password，这里将 identifier 映射为 username
          const payload = { username: this.loginForm.identifier, password: this.loginForm.password }
          const res = await authAPI.login(payload)
          if (res && res.success) {
            const token = res.data?.token || res.data?.accessToken || res.token
            if (token) localStorage.setItem('adminToken', token)
            this.$router.replace('/')
          } else {
            // ✅ 显式弹窗显示errorMsg
            const errorMsg = res?.errorMsg || res?.message || '登录失败'
            this.$message.error(errorMsg)
          }
        } catch (e) {
          // ✅ 显式弹窗显示错误信息
          const errorMsg = e.response?.data?.errorMsg || e.response?.data?.message || e.message || '登录异常'
          this.$message.error(errorMsg)
        } finally {
          this.loginLoading = false
        }
      })
    }
  }
}
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  background: linear-gradient(135deg, var(--primary-color), var(--primary-light));
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
}

.login-container { width: 100%; max-width: 400px;}

.login-form-wrapper {
  background: #fff;
  border-radius: 16px;
  padding: 40px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.08);
}

.form-header { text-align: center; margin-bottom: 30px; }
.form-header h2 { font-size: 28px; font-weight: normal; margin-bottom: 10px; }
.form-header p { font-size: 14px; }

.login-form { margin-bottom: 20px; }
.login-btn { width: 100%; height: 48px; font-size: 16px; font-weight: normal; border-radius: 24px; }

/* 明确标注统一组件，确保样式可控 */
.login-form :deep(.unified-input) { width: 100%; }

/* 输入框药丸与交互效果一致 */
.login-form :deep(.el-input__wrapper) {
  border-radius: 24px;
  border: 1px solid var(--primary-color);
  box-shadow: 0 2px 8px rgba(106, 1, 94, 0.1);
  transition: all 0.3s ease;
}
.login-form :deep(.el-input__wrapper:hover) {
  border-color: var(--primary-light);
  box-shadow: 0 4px 12px rgba(106, 1, 94, 0.15);
}
.login-form :deep(.el-input__wrapper.is-focus) {
  border-color: var(--primary-color);
  box-shadow: 0 4px 12px rgba(106, 1, 94, 0.2);
}
.login-form :deep(.el-input__prefix) { padding-left: 12px; }
.login-form :deep(.el-input__inner) { border-radius: 24px; padding-left: 12px; }

.login-btn :deep(.el-button) { width: 100%; height: 48px; font-size: 16px; border-radius: 24px; }

@media (max-width: 480px) {
  .login-form-wrapper { padding: 30px 20px; }
  .form-header h2 { font-size: 24px; }
}
</style>


