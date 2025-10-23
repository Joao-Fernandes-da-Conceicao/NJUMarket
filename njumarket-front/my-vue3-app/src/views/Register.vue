<template>
  <div class="register-page">
    <div class="register-container">
      <div class="register-form-wrapper">
        <div class="form-header">
          <h2 class="text-primary">注册南大市场</h2>
          <p class="text-secondary">加入我们，开启你的购物之旅</p>
        </div>
        
        <el-form
          ref="registerFormRef"
          :model="registerForm"
          :rules="registerRules"
          class="register-form"
          @submit.prevent="handleRegister"
        >
          <el-form-item prop="phone">
            <el-input
              v-model="registerForm.phone"
              placeholder="请输入手机号"
              size="large"
              prefix-icon="Phone"
            />
          </el-form-item>
          
          <el-form-item prop="code">
            <div class="code-input-group">
              <el-input
                v-model="registerForm.code"
                placeholder="请输入验证码"
                size="large"
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
          
          <el-form-item prop="password">
            <el-input
              v-model="registerForm.password"
              type="password"
              placeholder="请输入密码"
              size="large"
              prefix-icon="Lock"
              show-password
            />
          </el-form-item>
          
          <el-form-item prop="confirmPassword">
            <el-input
              v-model="registerForm.confirmPassword"
              type="password"
              placeholder="请确认密码"
              size="large"
              prefix-icon="Lock"
              show-password
            />
          </el-form-item>
          
          <el-form-item prop="nickname">
            <el-input
              v-model="registerForm.nickname"
              placeholder="请输入昵称（可选）"
              size="large"
              prefix-icon="User"
            />
          </el-form-item>
          
          <el-form-item>
            <el-button
              type="primary"
              size="large"
              class="register-btn"
              :loading="registerLoading"
              @click="handleRegister"
            >
              注册
            </el-button>
          </el-form-item>
        </el-form>
        
        <div class="form-footer">
          <div class="login-link">
            <span class="text-light">已有账号？</span>
            <router-link to="/login" class="text-primary">立即登录</router-link>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '../stores/user'
import { ElMessage } from 'element-plus'

export default {
  name: 'RegisterPage',
  setup() {
    const router = useRouter()
    const userStore = useUserStore()
    
    const registerFormRef = ref()
    const registerLoading = ref(false)
    const codeCountdown = ref(0)
    
    const registerForm = reactive({
      phone: '',
      code: '',
      password: '',
      confirmPassword: '',
      nickname: ''
    })
    
    const registerRules = {
      phone: [
        { required: true, message: '请输入手机号', trigger: 'blur' },
        { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号', trigger: 'blur' }
      ],
      code: [
        { required: true, message: '请输入验证码', trigger: 'blur' },
        { len: 6, message: '验证码为6位数字', trigger: 'blur' }
      ],
      password: [
        { required: true, message: '请输入密码', trigger: 'blur' },
        { min: 6, message: '密码长度不能少于6位', trigger: 'blur' }
      ],
      confirmPassword: [
        { required: true, message: '请确认密码', trigger: 'blur' },
        {
          validator: (rule, value, callback) => {
            if (value !== registerForm.password) {
              callback(new Error('两次输入的密码不一致'))
            } else {
              callback()
            }
          },
          trigger: 'blur'
        }
      ]
    }
    
    // 发送验证码
    const sendCode = async () => {
      if (!registerForm.phone) {
        ElMessage.warning('请先输入手机号')
        return
      }
      
      if (!/^1[3-9]\d{9}$/.test(registerForm.phone)) {
        ElMessage.warning('请输入正确的手机号')
        return
      }
      
      try {
        await userStore.sendCode(registerForm.phone)
        ElMessage.success('验证码已发送')
        startCountdown()
      } catch (error) {
        ElMessage.error(error.message || '发送验证码失败')
      }
    }
    
    // 注册
    const handleRegister = async () => {
      if (!registerFormRef.value) return
      
      await registerFormRef.value.validate(async (valid) => {
        if (valid) {
          registerLoading.value = true
          try {
            await userStore.register({
              phone: registerForm.phone,
              code: registerForm.code,
              password: registerForm.password,
              confirmPassword: registerForm.confirmPassword,
              nickname: registerForm.nickname
            })
            ElMessage.success('注册成功')
            router.push('/')
          } catch (error) {
            ElMessage.error(error.message || '注册失败')
          } finally {
            registerLoading.value = false
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
      registerFormRef,
      registerForm,
      registerRules,
      registerLoading,
      codeCountdown,
      sendCode,
      handleRegister
    }
  }
}
</script>

<style scoped>
.register-page {
  min-height: 100vh;
  background: linear-gradient(135deg, var(--primary-color), var(--primary-light));
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
}

.register-container {
  width: 100%;
  max-width: 400px;
}

.register-form-wrapper {
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

.register-form {
  margin-bottom: 20px;
}

.register-btn {
  width: 100%;
  height: 48px;
  font-size: 16px;
  font-weight: 600;
}

.form-footer {
  text-align: center;
}

.login-link {
  font-size: 14px;
}

.login-link a {
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
  .register-form-wrapper {
    padding: 30px 20px;
  }
  
  .form-header h2 {
    font-size: 24px;
  }
}
</style>
