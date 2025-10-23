<template>
  <div class="login-page">
    <div class="login-container">
      <div class="login-form-wrapper">
        <div class="form-header">
          <h2 class="text-primary">登录南大市场</h2>
          <p class="text-secondary">欢迎回来，开始你的购物之旅</p>
        </div>
        
        <el-form
          ref="loginFormRef"
          :model="loginForm"
          :rules="loginRules"
          class="login-form"
          @submit.prevent="handleLogin"
        >
          <el-form-item prop="identifier">
            <el-input
              v-model="loginForm.identifier"
              placeholder="请输入用户名或手机号"
              size="large"
              prefix-icon="User"
            />
          </el-form-item>
          
          <el-form-item prop="password">
            <el-input
              v-model="loginForm.password"
              type="password"
              placeholder="请输入密码"
              size="large"
              prefix-icon="Lock"
              show-password
            />
          </el-form-item>
          
          <el-form-item>
            <el-button
              type="primary"
              size="large"
              class="login-btn"
              :loading="loginLoading"
              @click="handleLogin"
            >
              登录
            </el-button>
          </el-form-item>
        </el-form>
        
        <div class="form-footer">
          <div class="login-methods">
            <el-button
              type="text"
              class="text-primary"
              @click="showCodeLogin = true"
            >
              验证码登录
            </el-button>
          </div>
          
          <div class="register-link">
            <span class="text-light">还没有账号？</span>
            <router-link to="/register" class="text-primary">立即注册</router-link>
          </div>
        </div>
      </div>
    </div>
    
    <!-- 验证码登录对话框 -->
    <el-dialog
      v-model="showCodeLogin"
      title="验证码登录"
      width="400px"
      :close-on-click-modal="false"
    >
      <el-form
        ref="codeFormRef"
        :model="codeForm"
        :rules="codeRules"
        @submit.prevent="handleCodeLogin"
      >
        <el-form-item prop="phone">
          <el-input
            v-model="codeForm.phone"
            placeholder="请输入手机号"
            prefix-icon="Phone"
          />
        </el-form-item>
        
        <el-form-item prop="code">
          <div class="code-input-group">
            <el-input
              v-model="codeForm.code"
              placeholder="请输入验证码"
              prefix-icon="Message"
            />
            <el-button
              type="primary"
              :disabled="codeCountdown > 0"
              @click="sendCode"
            >
              {{ codeCountdown > 0 ? `${codeCountdown}s` : '发送验证码' }}
            </el-button>
          </div>
        </el-form-item>
        
        <el-form-item>
          <el-button
            type="primary"
            size="large"
            class="login-btn"
            :loading="codeLoginLoading"
            @click="handleCodeLogin"
          >
            登录
          </el-button>
        </el-form-item>
      </el-form>
    </el-dialog>
  </div>
</template>

<script>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '../stores/user'
import { ElMessage } from 'element-plus'

export default {
  name: 'LoginPage',
  setup() {
    const router = useRouter()
    const userStore = useUserStore()
    
    const loginFormRef = ref()
    const codeFormRef = ref()
    const loginLoading = ref(false)
    const codeLoginLoading = ref(false)
    const showCodeLogin = ref(false)
    const codeCountdown = ref(0)
    
    const loginForm = reactive({
      identifier: '',
      password: ''
    })
    
    const codeForm = reactive({
      phone: '',
      code: ''
    })
    
    const loginRules = {
      identifier: [
        { required: true, message: '请输入用户名或手机号', trigger: 'blur' },
        { min: 2, max: 20, message: '长度在 2 到 20 个字符', trigger: 'blur' }
      ],
      password: [
        { required: true, message: '请输入密码', trigger: 'blur' },
        { min: 6, message: '密码长度不能少于6位', trigger: 'blur' }
      ]
    }
    
    const codeRules = {
      phone: [
        { required: true, message: '请输入手机号', trigger: 'blur' },
        { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号', trigger: 'blur' }
      ],
      code: [
        { required: true, message: '请输入验证码', trigger: 'blur' },
        { len: 6, message: '验证码为6位数字', trigger: 'blur' }
      ]
    }
    
    // 密码登录
    const handleLogin = async () => {
      if (!loginFormRef.value) return
      
      await loginFormRef.value.validate(async (valid) => {
        if (valid) {
          loginLoading.value = true
          try {
            await userStore.login({
              identifier: loginForm.identifier,
              password: loginForm.password
            })
            ElMessage.success('登录成功')
            router.push('/')
          } catch (error) {
            ElMessage.error(error.message || '登录失败')
          } finally {
            loginLoading.value = false
          }
        }
      })
    }
    
    // 发送验证码
    const sendCode = async () => {
      if (!codeForm.phone) {
        ElMessage.warning('请先输入手机号')
        return
      }
      
      if (!/^1[3-9]\d{9}$/.test(codeForm.phone)) {
        ElMessage.warning('请输入正确的手机号')
        return
      }
      
      try {
        await userStore.sendCode(codeForm.phone)
        ElMessage.success('验证码已发送')
        startCountdown()
      } catch (error) {
        ElMessage.error(error.message || '发送验证码失败')
      }
    }
    
    // 验证码登录
    const handleCodeLogin = async () => {
      if (!codeFormRef.value) return
      
      await codeFormRef.value.validate(async (valid) => {
        if (valid) {
          codeLoginLoading.value = true
          try {
            await userStore.loginByCode(codeForm.phone, codeForm.code)
            ElMessage.success('登录成功')
            showCodeLogin.value = false
            router.push('/')
          } catch (error) {
            ElMessage.error(error.message || '登录失败')
          } finally {
            codeLoginLoading.value = false
          }
        }
      })
    }
    
    // 倒计时
    const startCountdown = () => {
      codeCountdown.value = 60
      const timer = setInterval(() => {
        codeCountdown.value--
        if (codeCountdown.value <= 0) {
          clearInterval(timer)
        }
      }, 1000)
    }
    
    return {
      loginFormRef,
      codeFormRef,
      loginForm,
      codeForm,
      loginRules,
      codeRules,
      loginLoading,
      codeLoginLoading,
      showCodeLogin,
      codeCountdown,
      handleLogin,
      sendCode,
      handleCodeLogin
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

.login-container {
  width: 100%;
  max-width: 400px;
}

.login-form-wrapper {
  background: white;
  border-radius: 12px;
  padding: 40px;
  box-shadow: 0 8px 32px rgba(106, 1, 94, 0.2);
}

.form-header {
  text-align: center;
  margin-bottom: 30px;
}

.form-header h2 {
  font-size: 28px;
  font-weight: bold;
  margin-bottom: 10px;
}

.form-header p {
  font-size: 14px;
}

.login-form {
  margin-bottom: 20px;
}

.login-btn {
  width: 100%;
  height: 48px;
  font-size: 16px;
  font-weight: 600;
}

.form-footer {
  text-align: center;
}

.login-methods {
  margin-bottom: 20px;
}

.register-link {
  font-size: 14px;
}

.register-link a {
  text-decoration: none;
  font-weight: 500;
}

.code-input-group {
  display: flex;
  gap: 10px;
}

.code-input-group .el-input {
  flex: 1;
}

.code-input-group .el-button {
  white-space: nowrap;
}

@media (max-width: 480px) {
  .login-form-wrapper {
    padding: 30px 20px;
  }
  
  .form-header h2 {
    font-size: 24px;
  }
}
</style>
