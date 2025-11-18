<template>
  <div class="edit-profile-page">
    <div class="edit-profile-content">
      <div class="container">
        <div class="page-header">
          <h1>编辑资料</h1>
          <div class="header-actions">
            <UnifiedButton @click="$router.go(-1)">
              返回
            </UnifiedButton>
          </div>
        </div>

        <div class="profile-form-wrapper" v-loading="loading">
          <el-form
            ref="editFormRef"
            :model="editForm"
            :rules="editRules"
            label-position="right"
            class="profile-form"
          >
            <!-- 基本信息 -->
            <div class="form-section">
              <h3 class="section-title">基本信息</h3>
              
              <el-form-item label="昵称" prop="nickname">
                <UnifiedInput v-model="editForm.nickname" placeholder="请输入昵称" />
              </el-form-item>
              
              <el-form-item label="个人简介" prop="bio">
                <el-input
                  v-model="editForm.bio"
                  type="textarea"
                  placeholder="请输入个人简介"
                  :rows="4"
                  maxlength="200"
                  show-word-limit
                  class="rounded-textarea"
                />
              </el-form-item>

              <el-form-item label="联系方式" prop="contact">
                <el-input
                  v-model="editForm.contact"
                  placeholder="请输入联系方式（如微信、QQ等）"
                  class="pill-input"
                />
              </el-form-item>

              <el-form-item label="所在地区" prop="location">
                <UnifiedInput v-model="editForm.location" placeholder="请输入所在地区" />
              </el-form-item>
              
              <el-form-item label="地址管理">
                <UnifiedButton 
                  type="default" 
                  @click="$router.push('/addresses')"
                >
                  管理收货地址
                </UnifiedButton>
              </el-form-item>
            </div>

            <!-- 提交按钮 -->
            <div class="form-actions">
              <UnifiedButton size="large" @click="handleCancel">
                取消
              </UnifiedButton>
              <UnifiedButton
                type="primary"
                size="large"
                :loading="editLoading"
                @click="handleSave"
              >
                保存
              </UnifiedButton>
            </div>
          </el-form>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '../stores/user'
import { profileAPI } from '../api'
import { ElMessage } from 'element-plus'
import UnifiedButton from '../components/common/UnifiedButton.vue'
import UnifiedInput from '../components/common/UnifiedInput.vue'

export default {
  name: 'EditProfile',
  components: {
    UnifiedButton,
    UnifiedInput
  },
  setup() {
    const router = useRouter()
    const userStore = useUserStore()
    
    const editFormRef = ref()
    const editLoading = ref(false)
    const loading = ref(false)
    
    const editForm = reactive({
      nickname: '',
      bio: '',
      contact: '',
      location: ''
    })
    
    const editRules = {
      nickname: [
        { required: true, message: '请输入昵称', trigger: 'blur' },
        { min: 1, max: 20, message: '昵称长度在 1 到 20 个字符', trigger: 'blur' }
      ]
    }
    
    // 获取用户资料（自动填充）
    const fetchProfile = async () => {
      loading.value = true
      try {
        // 使用 getMe() 获取当前用户的完整资料
        const response = await profileAPI.getMe()
        if (response.success && response.data) {
          const profileData = response.data
          
          // 填充表单数据（类似编辑商品的实现）
          editForm.nickname = profileData.nickname || ''
          // 注意：bio, contact, location 字段在后端可能不存在，先尝试获取
          editForm.bio = profileData.bio || ''
          editForm.contact = profileData.contact || ''
          editForm.location = profileData.location || ''
          
          console.log('用户资料已自动填充:', editForm)
        } else {
          ElMessage.error(response.errorMsg || '获取用户资料失败')
        }
      } catch (error) {
        console.error('获取用户资料失败:', error)
        ElMessage.error('获取用户资料失败')
      } finally {
        loading.value = false
      }
    }
    
    // 保存资料
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
    
    // 取消
    const handleCancel = () => {
      router.go(-1)
    }
    
    onMounted(() => {
      fetchProfile()
    })
    
    return {
      editFormRef,
      editForm,
      editRules,
      editLoading,
      loading,
      handleSave,
      handleCancel
    }
  }
}
</script>

<style scoped>
.edit-profile-page {
  min-height: 100vh;
  background-color: #f5f5f5;
}

.edit-profile-content {
  padding: 40px 0;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 30px;
}

/* 返回按钮药丸型 */
.page-header .el-button {
  border-radius: 20px;
}

.profile-form-wrapper {
  background: transparent;
  border-radius: 16px;
  padding: 40px 60px;
  border: none;
  margin: 0 auto;
}

.form-section {
  margin-bottom: 40px;
  text-align: center;
}

.section-title {
  font-size: 24px;
  font-weight: normal;
  color: var(--primary-color);
  margin-bottom: 20px;
  text-align: center;
}

.profile-form {
  width: 100%;
}

/* 表单项整体居中 */
.profile-form :deep(.el-form-item) {
  margin: 0 auto 20px auto;
  width: 100%;
}

/* 输入框宽度自适应 */
.profile-form :deep(.el-input),
.profile-form :deep(.el-select),
.profile-form :deep(.el-textarea) {
  width: 100%;
}

.form-actions {
  display: flex;
  justify-content: center;
  gap: 20px;
  margin-top: 40px;
  padding-top: 30px;
  border-top: 1px solid #e0e0e0;
}

.form-actions .el-button {
  min-width: 120px;
  margin-left: 0 !important;
  border-radius: 20px; /* 药丸型按钮 */
}

/* 药丸型输入框样式 */
.pill-input :deep(.el-input__wrapper) {
  border-radius: 20px;
  border: 1px solid var(--primary-color);
  background-color: white;
  box-shadow: 0 2px 8px rgba(106, 1, 94, 0.1);
  transition: all 0.3s ease;
}

.pill-input :deep(.el-input__wrapper:hover) {
  border-color: var(--primary-light);
  box-shadow: 0 4px 12px rgba(106, 1, 94, 0.15);
}

.pill-input :deep(.el-input__wrapper.is-focus) {
  border-color: var(--primary-color);
  box-shadow: 0 4px 12px rgba(106, 1, 94, 0.2);
}

.pill-input :deep(.el-input__inner) {
  border-radius: 20px;
  padding-left: 5px;
}

/* 圆角矩形多行输入框样式 */
.rounded-textarea :deep(.el-textarea__inner) {
  border-radius: 16px;
  border: 1px solid var(--primary-color);
  background-color: white;
  box-shadow: 0 2px 8px rgba(106, 1, 94, 0.1);
  transition: all 0.3s ease;
  padding: 12px 12px 12px 17px;
}

.rounded-textarea :deep(.el-textarea__inner:hover) {
  border-color: var(--primary-light);
  box-shadow: 0 4px 12px rgba(106, 1, 94, 0.15);
}

.rounded-textarea :deep(.el-textarea__inner:focus) {
  border-color: var(--primary-color);
  box-shadow: 0 4px 12px rgba(106, 1, 94, 0.2);
}

@media (max-width: 900px) {
  .page-header {
    flex-direction: column;
    gap: 15px;
    text-align: center;
  }

  .profile-form-wrapper {
    padding: 20px;
  }

  .form-actions {
    flex-wrap: wrap;
    gap: 15px;
  }
  
  .form-actions .el-button {
    min-width: 130px;
  }
}
</style>

