<template>
  <div class="profile-page">
    <!-- 用户资料内容 -->
    <div class="profile-content">
      <div class="container">
        <div class="profile-wrapper" v-loading="loading">
          <!-- 返回按钮 -->
          <div class="profile-back-button" @click="$router.go(-1)">
            <el-icon><ArrowLeft /></el-icon>
          </div>
          
          <!-- 用户基本信息 -->
          <div class="profile-header">
            <!-- 头像、昵称和会员等级 -->
            <div class="profile-main">
              <div class="pill-avatar">
                <el-avatar :size="200" :src="getAvatarUrl(profileData?.avatar)">
                  {{ profileData?.nickname?.charAt(0) || 'U' }}
                </el-avatar>
              </div>
              <div class="pill-info">
                <h1 class="pill-username">{{ profileData?.nickname || '用户' }}</h1>
                <div class="pill-user-id">用户ID: {{ profileData?.userId || '未知' }}</div>
              </div>
            </div>
            
            <!-- 其余信息 - 在下方居中且同行分布 -->
            <div class="profile-details">
              <div class="stat-item register-time">
                注册时间：{{ formatTime(profileData?.userInfo?.registerTime) }}
              </div>
            </div>
            
            <div class="profile-actions" v-if="isOwnProfile">
              <el-button type="primary" @click="showAvatarDialog = true">更换头像</el-button>
              <el-button type="primary" @click="goToEditProfile">编辑资料</el-button>
            </div>

            <!-- 查看卖家主页入口（在最下面） -->
            <div class="view-commodities-section">
              <el-button type="primary" @click="viewSellerCommodities">查看TA的主页</el-button>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 更换头像对话框 -->
    <el-dialog
      v-model="showAvatarDialog"
      title="更换头像"
      width="350px"
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
import { ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '../stores/user'
import { profileAPI, imageAPI } from '../api'
import { ElMessage } from 'element-plus'
import { ArrowLeft } from '@element-plus/icons-vue'

export default {
  name: 'UserProfile',
  components: { ArrowLeft },
  setup() {
    const route = useRoute()
    const router = useRouter()
    const userStore = useUserStore()
    
    const loading = ref(false)
    const profileData = ref(null)
    const showAvatarDialog = ref(false)
    
    const user = computed(() => userStore.user)
    const isOwnProfile = computed(() => {
      const userId = route.params.id
      return !userId || userId === user.value?.userId
    })
    
    // 上传配置（第一步上传到 Image 服务，第二步由 handleAvatarSuccess 写入档案）
    const uploadUrl = ref('http://localhost:8080/api/user/image/upload-avatar')
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
        }
      } catch (error) {
        ElMessage.error('获取用户资料失败')
      } finally {
        loading.value = false
      }
    }
    
    // 跳转到编辑资料页面
    const goToEditProfile = () => {
      router.push('/edit-profile')
    }
    
    // 查看卖家主页
    const viewSellerCommodities = () => {
      const userId = route.params.id || user.value?.userId
      if (userId) {
        router.push(`/home/${userId}`)
      }
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
    
    // 头像上传成功（el-upload 上传到 Image 服务后回调）
    const handleAvatarSuccess = async (response) => {
      if (!response.success) {
        ElMessage.error(response.errorMsg || '头像上传失败')
        return
      }
      // Image 服务返回 ImageUploadDTO，取 imageUrl 字段
      const imageUrl = response.data?.imageUrl || response.data
      try {
        // 第二步：将 imageUrl 写入 Auth 服务的用户档案
        await profileAPI.setAvatarUrl(imageUrl)
        profileData.value.avatar = imageUrl
        userStore.updateUser({ avatar: imageUrl })
        showAvatarDialog.value = false
        ElMessage.success('头像更新成功')
      } catch (err) {
        ElMessage.error('头像 URL 保存失败，请重试')
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
      showAvatarDialog,
      user,
      isOwnProfile,
      uploadUrl,
      uploadHeaders,
      goToEditProfile,
      viewSellerCommodities,
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

/* 返回按钮样式 */
.profile-back-button {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: #f5f5f5;
  cursor: pointer;
  transition: all 0.3s ease;
  margin-bottom: 20px;
  color: var(--primary-color);
}

.profile-back-button:hover {
  background: #e0e0e0;
}

.profile-back-button .el-icon {
  font-size: 20px;
}

.profile-header {
  background: transparent; /* 设置为透明 */
  border: none; /* 移除边框 */
  box-shadow: none; /* 移除阴影 */
  border-radius: 0; /* 移除圆角 */
  padding: 0; /* 移除内边距 */
  margin-bottom: 1.875vw; /* 相对单位，从30px改为1.875vw */
  display: flex;
  flex-direction: column;
  gap: 6.25vw; /* 相对单位，从100px改为6.25vw */
  align-items: center; /* 居中对齐 */
}

/* 药丸型框样式 */
.profile-main {
  display: flex;
  align-items: center;
  gap: 2.5vw; /* 相对单位，从40px改为2.5vw */
  padding: 0;
  background: transparent;
  border: none; /* 移除边框 */
  width: fit-content;
  margin: 3vw auto 0; /* 相对上边距，从50px改为3vw */
}

.pill-avatar {
  flex-shrink: 0;
}

.pill-avatar :deep(.el-avatar) {
  border-radius: 50%; /* 改成正圆 */
}

.profile-main :deep(.el-avatar__inner) {
  border-radius: 50%; /* 改成正圆 */
}

.pill-info {
  display: flex;
  flex-direction: column;
  gap: 1.25vw; /* 相对单位，从20px改为1.25vw */
}

.pill-username {
  font-size: 3.75vw; /* 相对单位，从60px改为3.75vw */
  font-weight: normal;
  margin: 0;
  margin-left: 1.25vw; /* 相对单位，从20px改为1.25vw */
  color: var(--primary-color); /* 改为主题色 */
}

.pill-user-id {
  font-size: 1vw; /* 相对单位，从16px改为1vw */
  color: #999; /* 灰色 */
  margin-left: 1.25vw; /* 相对单位，从20px改为1.25vw */
  margin-top: 0.3vw; /* 相对单位，从5px改为0.3vw */
}

.pill-vip {
  background: var(--primary-color);
  color: white;
  padding: 0.3vw 1.25vw; /* 相对单位，从5px 20px改为0.3vw 1.25vw */
  border-radius: 1.875vw; /* 相对单位，从30px改为1.875vw */
  font-size: 1.25vw; /* 电脑端等级字号为id的1.25倍（1vw * 1.25 = 1.25vw） */
  font-weight: normal;
  width: fit-content;
  margin-left: 1.25vw; /* 相对单位，从20px改为1.25vw */
  margin-right: 1.25vw; /* 相对单位，从20px改为1.25vw */
}

.profile-details {
  display: flex;
  flex-direction: row; /* 同行分布 */
  gap: 1.875vw; /* 相对单位，从30px改为1.875vw */
  justify-content: center; /* 居中对齐 */
  align-items: center; /* 垂直居中 */
  flex-wrap: wrap; /* 允许换行 */
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
  gap: 8px;
  white-space: nowrap; /* 防止换行 */
}

.stat-item.register-time {
  color: var(--primary-color); /* 改为主题色 */
  font-size: 18px; /* 从16px增大到18px */
}

.stat-item .stat-label {
  color: var(--primary-color); /* 改为主题色 */
  font-size: 18px; /* 从14px增大到18px */
}

.stat-item .stat-value {
  color: var(--primary-color);
  font-weight: normal;
  font-size: 22px; /* 从16px增大到22px */
}

.register-time {
  color: #999;
  font-size: 14px;
}

  .profile-actions {
    display: flex;
    flex-direction: row;
    gap: 10px;
    justify-content: center;
  }
  
  .view-commodities-section {
    display: flex;
    justify-content: center;
    margin-top: 40px;
  }
  
  .view-commodities-btn {
    padding: 12px 40px !important;
    font-size: 18px !important;
  }

/* border-radius 由 UnifiedButton 统一管理（9999px） */
.profile-action-btn {
  padding: 10px 30px !important;
  font-size: 16px !important;
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

/* 药丸型按钮样式 */
.pill-upload-btn,
.pill-close-btn {
  border-radius: 20px;
  padding: 10px 20px;
  min-width: auto;
  width: 120px;
}

@media (max-width: 900px) {
  .profile-main {
    flex-direction: column; /* 手机端垂直排列 */
    gap: 4vw; /* 增大间距 */
    margin: 3vw auto 8vw; /* 手机端增加下边距8vw */
    align-items: center; /* 手机端居中对齐 */
  }
  
  .pill-info {
    align-items: center; /* 手机端内部元素居中 */
  }
  
  .pill-username {
    font-size: 12vw; /* 手机端更大字体，从5vw增大到7vw */
    margin-left: 0 !important; /* 移除左间距 */
    text-align: center; /* 居中文本 */
  }
  
  .pill-user-id {
    font-size: 3vw; /* 手机端更易读 */
    margin-left: 0 !important; /* 移除左间距 */
    text-align: center; /* 居中文本 */
  }
  
  .pill-vip {
    font-size: 2.5vw; /* 手机端适中字体 */
    padding: 1vw 5vw; /* 手机端增加左右padding实现药丸型 */
    border-radius: 999px; /* 手机端真药丸型 */
    margin-left: 0 !important; /* 移除左间距 */
    margin-right: 0 !important; /* 移除右间距 */
    margin-top: 1vw;
  }
  
  .profile-details {
    margin-top: 8vw !important; /* 手机端增大上方间距 */
    margin-bottom: 8vw !important; /* 手机端增大下方间距 */
  }
  
  .profile-actions {
    margin-top: 8vw !important; /* 手机端增大上方间距 */
    margin-bottom: 8vw !important; /* 手机端增大下方间距 */
  }
  
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
