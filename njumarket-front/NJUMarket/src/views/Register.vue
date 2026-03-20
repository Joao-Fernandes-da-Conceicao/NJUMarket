<template>
  <div class="register-page">
    <div class="register-container">
      <div class="register-form-wrapper">
        <div class="form-header">
          <h2 class="text-primary">注册南大集市</h2>
          <p class="text-secondary">加入我们，开启你的购物之旅</p>
        </div>
        
        <el-form
          ref="registerFormRef"
          :model="registerForm"
          :rules="registerRules"
          class="register-form"
          @submit.prevent="handleRegister"
        >
          <el-form-item prop="username">
            <el-input
              v-model="registerForm.username"
              placeholder="请输入用户名"
              size="large"
            />
          </el-form-item>
          
          <el-form-item prop="password">
            <el-input
              v-model="registerForm.password"
              type="password"
              placeholder="请输入密码"
              size="large"
              show-password
            />
          </el-form-item>
          
          <el-form-item prop="confirmPassword">
            <el-input
              v-model="registerForm.confirmPassword"
              type="password"
              placeholder="请再次输入密码"
              size="large"
              show-password
            />
          </el-form-item>
          
          <el-form-item prop="phone">
            <el-input
              v-model="registerForm.phone"
              placeholder="请输入手机号"
              size="large"
            />
          </el-form-item>
          
          <el-form-item prop="email">
            <el-input
              v-model="registerForm.email"
              placeholder="请输入邮箱（可选）"
              size="large"
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
  components: {
  },
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
  border-radius: 16px; /* 使用主页的圆角设计 */
  padding: 40px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.08); /* 使用主页的阴影设计 */
}

.form-header {
  text-align: center;
  margin-bottom: 30px;
}

.form-header h2 {
  font-size: 28px;
  font-weight: normal;
  margin-bottom: 10px;
}

.form-header p {
  font-size: 14px;
}

.register-form {
  margin-bottom: 20px;
}

/* 输入框药丸形状设计 */
.register-form :deep(.el-input__wrapper) {
  border-radius: 24px; /* 药丸形状 */
  border: 1px solid var(--primary-color);
  box-shadow: 0 2px 8px rgba(106, 1, 94, 0.1);
  transition: all 0.3s ease;
}

.register-form :deep(.el-input__wrapper:hover) {
  border-color: var(--primary-light);
  box-shadow: 0 4px 12px rgba(106, 1, 94, 0.15);
}

.register-form :deep(.el-input__wrapper.is-focus) {
  border-color: var(--primary-color);
  box-shadow: 0 4px 12px rgba(106, 1, 94, 0.2);
}

/* 修复图标覆盖光标的问题 */
.register-form :deep(.el-input__prefix) {
  padding-left: 12px; /* 增加左侧内边距 */
}

.register-form :deep(.el-input__inner) {
  border-radius: 24px; /* 确保内部输入框也是药丸形状 */
  padding-left: 12px; /* 为图标预留空间，避免与光标重叠 */
}

.register-btn {
  width: 100%;
  height: 48px;
  font-size: 16px;
  font-weight: normal;
  border-radius: 24px; /* 药丸形状设计 */
}

.form-footer {
  text-align: center;
}

.login-link {
  font-size: 14px;
}

.login-link a {
  text-decoration: none;
  font-weight: normal;
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
  border-radius: 24px; /* 验证码按钮也使用药丸形状 */
  height: 41.6px;
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
