<template>
  <div class="edit-profile-page">
    <div class="container">
      <div class="page-header">
        <h1>编辑资料</h1>
        <el-button @click="$router.go(-1)">返回</el-button>
      </div>

      <el-card v-loading="loading">
        <el-form
          ref="editFormRef"
          :model="editForm"
          :rules="editRules"
          label-width="100px"
        >
          <el-form-item label="昵称" prop="nickname">
            <el-input v-model="editForm.nickname" placeholder="请输入昵称" />
          </el-form-item>

          <el-form-item label="手机号">
            <div class="phone-display">
              <span>{{ currentPhone || '未设置' }}</span>
              <el-button size="small" @click="showPhoneDialog = true">修改手机号</el-button>
            </div>
          </el-form-item>

          <el-form-item label="地址管理">
            <el-button @click="$router.push('/addresses')">管理收货地址</el-button>
          </el-form-item>

          <el-form-item>
            <el-button @click="handleCancel">取消</el-button>
            <el-button type="primary" :loading="editLoading" @click="handleSave">保存</el-button>
          </el-form-item>
        </el-form>
      </el-card>
    </div>

    <!-- 修改手机号对话框 -->
    <el-dialog v-model="showPhoneDialog" title="修改手机号" width="480px" :close-on-click-modal="false">
      <el-form ref="phoneFormRef" :model="phoneForm" :rules="phoneRules" label-width="100px">
        <el-form-item label="新手机号" prop="newPhone">
          <el-input v-model="phoneForm.newPhone" placeholder="请输入新手机号" />
        </el-form-item>
        <el-form-item label="验证码" prop="code">
          <div class="code-input-group">
            <el-input v-model="phoneForm.code" placeholder="请输入验证码" maxlength="6" />
            <el-button :disabled="phoneCodeCountdown > 0" @click="sendPhoneCode">
              {{ phoneCodeCountdown > 0 ? `${phoneCodeCountdown}s` : '发送验证码' }}
            </el-button>
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showPhoneDialog = false">取消</el-button>
        <el-button type="primary" :loading="updatePhoneLoading" @click="handleUpdatePhone">确认修改</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '../stores/user'
import { profileAPI, authAPI } from '../api'
import { ElMessage } from 'element-plus'

export default {
  name: 'EditProfile',
  setup() {
    const router = useRouter()
    const userStore = useUserStore()

    const editFormRef = ref()
    const phoneFormRef = ref()
    const editLoading = ref(false)
    const loading = ref(false)
    const updatePhoneLoading = ref(false)
    const showPhoneDialog = ref(false)
    const phoneCodeCountdown = ref(0)
    const currentPhone = ref('')

    const editForm = reactive({ nickname: '' })

    const phoneForm = reactive({ newPhone: '', code: '' })

    const editRules = {
      nickname: [
        { required: true, message: '请输入昵称', trigger: 'blur' },
        { min: 1, max: 20, message: '昵称长度在 1 到 20 个字符', trigger: 'blur' }
      ]
    }

    const phoneRules = {
      newPhone: [
        { required: true, message: '请输入新手机号', trigger: 'blur' },
        { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号', trigger: 'blur' }
      ],
      code: [
        { required: true, message: '请输入验证码', trigger: 'blur' },
        { len: 6, message: '验证码为6位数字', trigger: 'blur' }
      ]
    }

    const fetchProfile = async () => {
      loading.value = true
      try {
        const response = await profileAPI.getMe()
        if (response.success && response.data) {
          const profileData = response.data
          editForm.nickname = profileData.nickname || ''
          currentPhone.value = profileData.userInfo?.primaryPhone || profileData.primaryPhone || ''
        } else {
          ElMessage.error(response.errorMsg || '获取用户资料失败')
        }
      } catch (error) {
        ElMessage.error('获取用户资料失败')
      } finally {
        loading.value = false
      }
    }

    const handleSave = async () => {
      if (!editFormRef.value) return
      await editFormRef.value.validate(async (valid) => {
        if (valid) {
          editLoading.value = true
          try {
            const response = await profileAPI.update(editForm)
            if (response.success) {
              ElMessage.success('资料更新成功')
              userStore.updateUser(editForm)
              router.go(-1)
            } else {
              ElMessage.error(response.errorMsg || '更新失败')
            }
          } catch (error) {
            ElMessage.error('更新失败')
          } finally {
            editLoading.value = false
          }
        }
      })
    }

    const handleCancel = () => router.go(-1)

    const sendPhoneCode = async () => {
      if (!phoneForm.newPhone) {
        ElMessage.warning('请先输入新手机号')
        return
      }
      if (!/^1[3-9]\d{9}$/.test(phoneForm.newPhone)) {
        ElMessage.error('请输入正确的手机号')
        return
      }
      try {
        await authAPI.sendCode(phoneForm.newPhone)
        ElMessage.success('验证码已发送')
        phoneCodeCountdown.value = 60
        const timer = setInterval(() => {
          phoneCodeCountdown.value--
          if (phoneCodeCountdown.value <= 0) clearInterval(timer)
        }, 1000)
      } catch (error) {
        ElMessage.error(error.message || '发送验证码失败')
      }
    }

    const handleUpdatePhone = async () => {
      if (!phoneFormRef.value) return
      await phoneFormRef.value.validate(async (valid) => {
        if (valid) {
          updatePhoneLoading.value = true
          try {
            const response = await authAPI.updatePhone({ newPhone: phoneForm.newPhone, code: phoneForm.code })
            if (response.success) {
              ElMessage.success('手机号修改成功')
              currentPhone.value = phoneForm.newPhone
              showPhoneDialog.value = false
              phoneForm.newPhone = ''
              phoneForm.code = ''
              await fetchProfile()
            } else {
              ElMessage.error(response.errorMsg || '修改失败')
            }
          } catch (error) {
            ElMessage.error(error.message || '修改失败')
          } finally {
            updatePhoneLoading.value = false
          }
        }
      })
    }

    onMounted(() => fetchProfile())

    return {
      editFormRef, phoneFormRef, editForm, phoneForm,
      editRules, phoneRules, editLoading, loading,
      updatePhoneLoading, showPhoneDialog, phoneCodeCountdown, currentPhone,
      handleSave, handleCancel, sendPhoneCode, handleUpdatePhone
    }
  }
}
</script>

<style scoped>
.edit-profile-page {
  min-height: 100vh;
  background: #f5f7fa;
  padding: 40px 0;
}

.container {
  max-width: 600px;
  margin: 0 auto;
  padding: 0 20px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.page-header h1 {
  margin: 0;
  font-size: 22px;
}

.phone-display {
  display: flex;
  align-items: center;
  gap: 12px;
}

.code-input-group {
  display: flex;
  gap: 10px;
  width: 100%;
}

.code-input-group .el-input {
  flex: 1;
}
</style>
