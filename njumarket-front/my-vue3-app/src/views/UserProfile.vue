<template>
  <div class="profile-page">
    <!-- 用户资料内容 -->
    <div class="profile-content">
      <div class="container">
        <div class="profile-wrapper" v-loading="loading">
          <!-- 用户基本信息 -->
          <div class="profile-header">
            <div class="avatar-section">
              <el-avatar :size="120" :src="getAvatarUrl(profileData?.avatar)">
                {{ profileData?.nickname?.charAt(0) || 'U' }}
              </el-avatar>
              <el-button
                v-if="isOwnProfile"
                type="primary"
                size="small"
                @click="showAvatarDialog = true"
                class="change-avatar-btn"
              >
                更换头像
              </el-button>
            </div>
            
            <div class="user-info">
              <h1 class="username">{{ profileData?.nickname || '用户' }}</h1>
              <div class="user-stats">
                <div class="stat-item">
                  <span class="stat-label">信用分：</span>
                  <span class="stat-value">{{ profileData?.creditScore || 0 }}</span>
                </div>
                <div class="stat-item">
                  <span class="stat-label">买家评分：</span>
                  <span class="stat-value">{{ profileData?.buyerRating || 0 }}</span>
                </div>
                <div class="stat-item">
                  <span class="stat-label">卖家评分：</span>
                  <span class="stat-value">{{ profileData?.sellerRating || 0 }}</span>
                </div>
                <div class="stat-item">
                  <span class="stat-label">VIP等级：</span>
                  <span class="stat-value">{{ profileData?.vipLevel || '普通' }}</span>
                </div>
              </div>
              <div class="register-time">
                注册时间：{{ formatTime(profileData?.userInfo?.registerTime) }}
              </div>
            </div>
            
            <div class="profile-actions" v-if="isOwnProfile">
              <el-button type="primary" @click="showEditDialog = true">
                编辑资料
              </el-button>
            </div>
          </div>

          <!-- 用户统计 -->
          <div class="stats-section">
            <div class="stats-grid">
              <div class="stat-card">
                <div class="stat-icon">🛒</div>
                <div class="stat-content">
                  <div class="stat-number">{{ userStats.buyCount || 0 }}</div>
                  <div class="stat-label">购买次数</div>
                </div>
              </div>
              <div class="stat-card">
                <div class="stat-icon">📦</div>
                <div class="stat-content">
                  <div class="stat-number">{{ userStats.sellCount || 0 }}</div>
                  <div class="stat-label">出售次数</div>
                </div>
              </div>
              <div class="stat-card">
                <div class="stat-icon">⭐</div>
                <div class="stat-content">
                  <div class="stat-number">{{ userStats.goodReviewCount || 0 }}</div>
                  <div class="stat-label">好评数</div>
                </div>
              </div>
              <div class="stat-card">
                <div class="stat-icon">💬</div>
                <div class="stat-content">
                  <div class="stat-number">{{ userStats.messageCount || 0 }}</div>
                  <div class="stat-label">消息数</div>
                </div>
              </div>
            </div>
          </div>

          <!-- 最近活动 -->
          <div class="activity-section">
            <h2>最近活动</h2>
            <div class="activity-list">
              <div v-if="recentActivities.length === 0" class="empty-activity">
                <p>暂无最近活动</p>
              </div>
              <div v-for="activity in recentActivities" :key="activity.id" class="activity-item">
                <div class="activity-icon">{{ activity.icon }}</div>
                <div class="activity-content">
                  <div class="activity-title">{{ activity.title }}</div>
                  <div class="activity-time">{{ formatTime(activity.time) }}</div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 编辑资料对话框 -->
    <el-dialog
      v-model="showEditDialog"
      title="编辑资料"
      width="500px"
      :close-on-click-modal="false"
    >
      <el-form
        ref="editFormRef"
        :model="editForm"
        :rules="editRules"
        label-width="80px"
      >
        <el-form-item label="昵称" prop="nickname">
          <el-input v-model="editForm.nickname" placeholder="请输入昵称" />
        </el-form-item>
        
        <el-form-item label="个人简介" prop="bio">
          <el-input 
            v-model="editForm.bio" 
            type="textarea" 
            placeholder="请输入个人简介"
            :rows="3"
            maxlength="200"
            show-word-limit
          />
        </el-form-item>
        
        <el-form-item label="联系方式" prop="contact">
          <el-input v-model="editForm.contact" placeholder="请输入联系方式（如微信、QQ等）" />
        </el-form-item>
        
        <el-form-item label="所在地区" prop="location">
          <el-input v-model="editForm.location" placeholder="请输入所在地区" />
        </el-form-item>
      </el-form>
      
      <template #footer>
        <el-button @click="showEditDialog = false">取消</el-button>
        <el-button type="primary" :loading="editLoading" @click="handleEditProfile">
          保存
        </el-button>
      </template>
    </el-dialog>

    <!-- 更换头像对话框 -->
    <el-dialog
      v-model="showAvatarDialog"
      title="更换头像"
      width="400px"
      :close-on-click-modal="false"
    >
      <div class="avatar-upload">
        <el-upload
          :action="uploadUrl"
          :headers="uploadHeaders"
          :show-file-list="false"
          :on-success="handleAvatarSuccess"
          :on-error="handleAvatarError"
          :before-upload="beforeAvatarUpload"
          accept="image/*"
        >
          <el-button type="primary">选择头像</el-button>
        </el-upload>
        <div class="upload-tip">
          <p>支持 JPG、PNG 格式，建议尺寸 200x200 像素</p>
        </div>
      </div>
      
      <template #footer>
        <el-button @click="showAvatarDialog = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import { ref, reactive, onMounted, computed } from 'vue'
import { useRoute } from 'vue-router'
import { useUserStore } from '../stores/user'
import { profileAPI, imageAPI } from '../api'
import { ElMessage } from 'element-plus'

export default {
  name: 'UserProfile',
  setup() {
    const route = useRoute()
    const userStore = useUserStore()
    
    const loading = ref(false)
    const profileData = ref(null)
    const userStats = ref({})
    const recentActivities = ref([])
    const showEditDialog = ref(false)
    const showAvatarDialog = ref(false)
    const editLoading = ref(false)
    
    const editFormRef = ref()
    const editForm = reactive({
      nickname: '',
      bio: '',
      contact: '',
      location: ''
    })
    
    const editRules = {
      nickname: [
        { required: true, message: '请输入昵称', trigger: 'blur' },
        { min: 2, max: 20, message: '昵称长度在 2 到 20 个字符', trigger: 'blur' }
      ],
      bio: [
        { max: 200, message: '个人简介不能超过 200 个字符', trigger: 'blur' }
      ],
      contact: [
        { max: 100, message: '联系方式不能超过 100 个字符', trigger: 'blur' }
      ],
      location: [
        { max: 50, message: '所在地区不能超过 50 个字符', trigger: 'blur' }
      ]
    }
    
    const user = computed(() => userStore.user)
    const isOwnProfile = computed(() => {
      const userId = route.params.id
      return !userId || userId === user.value?.userId
    })
    
    // 上传配置
    const uploadUrl = ref('http://localhost:8080/api/user/profile/avatar')
    const uploadHeaders = computed(() => ({
      'Authorization': `Bearer ${userStore.token}`
    }))
    
    // 获取用户资料
    const fetchProfile = async () => {
      loading.value = true
      try {
        const userId = route.params.id || user.value?.userId
        let response
        
        if (isOwnProfile.value) {
          response = await profileAPI.getMe()
        } else {
          response = await profileAPI.getUser(userId)
        }
        
        if (response.success) {
          profileData.value = response.data
          editForm.nickname = response.data.nickname || ''
          editForm.bio = response.data.bio || ''
          editForm.contact = response.data.contact || ''
          editForm.location = response.data.location || ''
          
          // 获取用户统计
          await fetchUserStats()
          
          // 获取最近活动
          await fetchRecentActivities()
        }
      } catch (error) {
        ElMessage.error('获取用户资料失败')
      } finally {
        loading.value = false
      }
    }
    
    // 获取用户统计
    const fetchUserStats = async () => {
      try {
        // 这里可以调用统计API，暂时使用模拟数据
        userStats.value = {
          buyCount: 15,
          sellCount: 8,
          goodReviewCount: 23,
          messageCount: 45
        }
      } catch (error) {
        console.error('获取用户统计失败:', error)
      }
    }
    
    // 获取最近活动
    const fetchRecentActivities = async () => {
      try {
        // 这里可以调用活动API，暂时使用模拟数据
        recentActivities.value = [
          {
            id: 1,
            icon: '🛒',
            title: '购买了商品《二手iPhone 13》',
            time: new Date(Date.now() - 2 * 24 * 60 * 60 * 1000)
          },
          {
            id: 2,
            icon: '📦',
            title: '发布了商品《MacBook Pro》',
            time: new Date(Date.now() - 5 * 24 * 60 * 60 * 1000)
          },
          {
            id: 3,
            icon: '⭐',
            title: '收到了好评',
            time: new Date(Date.now() - 7 * 24 * 60 * 60 * 1000)
          }
        ]
      } catch (error) {
        console.error('获取最近活动失败:', error)
      }
    }
    
    // 编辑资料
    const handleEditProfile = async () => {
      if (!editFormRef.value) return
      
      await editFormRef.value.validate(async (valid) => {
        if (valid) {
          editLoading.value = true
          try {
            const response = await profileAPI.update(editForm)
            if (response.success) {
              ElMessage.success('资料更新成功')
              showEditDialog.value = false
              fetchProfile()
              userStore.updateUser(editForm)
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
    
    // 头像上传前检查
    const beforeAvatarUpload = (file) => {
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
    }
    
    // 头像上传成功
    const handleAvatarSuccess = (response) => {
      if (response.success) {
        ElMessage.success('头像更新成功')
        // 后端返回的是ImageUploadDTO，包含imageUrl字段
        const imageUrl = response.data.imageUrl || response.data
        profileData.value.avatar = imageUrl
        userStore.updateUser({ avatar: imageUrl })
        showAvatarDialog.value = false
      } else {
        ElMessage.error(response.errorMsg || '头像上传失败')
      }
    }
    
    // 头像上传失败
    const handleAvatarError = () => {
      ElMessage.error('头像上传失败')
    }
    
    // 格式化时间
    const formatTime = (time) => {
      if (!time) return ''
      return new Date(time).toLocaleString()
    }
    
    // 获取头像URL
    const getAvatarUrl = (avatarUrl) => {
      if (!avatarUrl) return imageAPI.getDefaultAvatar()
      
      // 如果已经是完整URL，直接返回
      if (avatarUrl.startsWith('http')) return avatarUrl
      
      // 如果是文件名，构建完整URL
      if (avatarUrl.includes('/')) return avatarUrl
      
      // 从URL中提取文件名
      const fileName = avatarUrl.split('/').pop()
      return imageAPI.getAvatar(fileName)
    }
    
    // 登出
    const handleLogout = async () => {
      try {
        await userStore.logout()
        // userStore.logout()会处理跳转，不需要额外的跳转和消息
      } catch (error) {
        ElMessage.error('退出登录失败')
      }
    }
    
    onMounted(() => {
      fetchProfile()
    })
    
    return {
      loading,
      profileData,
      userStats,
      recentActivities,
      showEditDialog,
      showAvatarDialog,
      editLoading,
      editFormRef,
      editForm,
      editRules,
      user,
      isOwnProfile,
      uploadUrl,
      uploadHeaders,
      handleEditProfile,
      beforeAvatarUpload,
      handleAvatarSuccess,
      handleAvatarError,
      formatTime,
      getAvatarUrl,
      handleLogout
    }
  }
}
</script>

<style scoped>
.profile-page {
  min-height: 100vh;
  background-color: #f5f5f5;
}

.profile-content {
  padding: 30px 0;
}

.profile-wrapper {
  max-width: 1000px;
  margin: 0 auto;
}

.profile-header {
  background: white;
  border-radius: 12px;
  padding: 40px;
  margin-bottom: 30px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  display: flex;
  align-items: center;
  gap: 30px;
}

.avatar-section {
  text-align: center;
}

.change-avatar-btn {
  margin-top: 15px;
}

.user-info {
  flex: 1;
}

.username {
  font-size: 32px;
  font-weight: normal;
  margin-bottom: 20px;
  color: #333;
}

.user-stats {
  display: flex;
  gap: 30px;
  margin-bottom: 15px;
  flex-wrap: wrap;
}

.stat-item {
  display: flex;
  align-items: center;
  gap: 5px;
}

.stat-label {
  color: #666;
  font-size: 14px;
}

.stat-value {
  color: var(--primary-color);
  font-weight: normal;
  font-size: 16px;
}

.register-time {
  color: #999;
  font-size: 14px;
}

.profile-actions {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.stats-section {
  margin-bottom: 30px;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 20px;
}

.stat-card {
  background: white;
  border-radius: 16px; /* 使用主页的圆角设计 */
  padding: 20px;
  border: none; /* 移除边框 */
  box-shadow: none; /* 移除阴影，使用无框设计 */
  display: flex;
  align-items: center;
  gap: 15px;
}

.stat-icon {
  font-size: 32px;
  width: 60px;
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(106, 1, 94, 0.1);
  border-radius: 50%;
}

.stat-content {
  flex: 1;
}

.stat-number {
  font-size: 24px;
  font-weight: normal;
  color: var(--primary-color);
  margin-bottom: 5px;
}

.stat-label {
  color: #666;
  font-size: 14px;
}

.activity-section {
  background: white;
  border-radius: 16px; /* 使用主页的圆角设计 */
  padding: 30px;
  border: none; /* 移除边框 */
  box-shadow: none; /* 移除阴影，使用无框设计 */
}

.activity-section h2 {
  font-size: 20px;
  font-weight: normal;
  margin-bottom: 20px;
  color: #333;
}

.activity-list {
  max-height: 400px;
  overflow-y: auto;
}

.empty-activity {
  text-align: center;
  padding: 40px 0;
  color: #999;
}

.activity-item {
  display: flex;
  align-items: center;
  gap: 15px;
  padding: 15px 0;
  border-bottom: 1px solid #f0f0f0;
}

.activity-item:last-child {
  border-bottom: none;
}

.activity-icon {
  font-size: 24px;
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(106, 1, 94, 0.1);
  border-radius: 50%;
}

.activity-content {
  flex: 1;
}

.activity-title {
  font-size: 14px;
  color: #333;
  margin-bottom: 5px;
}

.activity-time {
  font-size: 12px;
  color: #999;
}

.avatar-upload {
  text-align: center;
}

.upload-tip {
  margin-top: 15px;
  color: #999;
  font-size: 12px;
}

@media (max-width: 768px) {
  .nav-content {
    flex-direction: column;
    gap: 15px;
  }
  
  .nav-menu {
    gap: 20px;
  }
  
  .profile-header {
    flex-direction: column;
    text-align: center;
    gap: 20px;
  }
  
  .user-stats {
    justify-content: center;
  }
  
  .stats-grid {
    grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
    gap: 15px;
  }
  
  .stat-card {
    flex-direction: column;
    text-align: center;
    gap: 10px;
  }
}
</style>
