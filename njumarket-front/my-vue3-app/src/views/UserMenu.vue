<template>
  <div class="user-menu-page">
    <div class="user-menu-container">
      <div class="user-menu-header">
        <h2 class="text-primary">用户中心</h2>
        <p class="text-secondary">管理您的账户和个人信息</p>
      </div>

      <div class="user-menu-content">
        <!-- 用户信息卡片 -->
        <div class="user-info-card">
          <div class="user-avatar">
            <img 
              v-if="user?.avatar" 
              :src="getAvatarUrl(user.avatar)" 
              :alt="user?.nickname || '用户头像'"
              @error="handleAvatarError"
            />
            <div v-else class="default-avatar">
              <el-icon size="40"><User /></el-icon>
            </div>
          </div>
          <div class="user-details">
            <h3 class="user-name">{{ user?.nickname || '未设置昵称' }}</h3>
            <p class="user-phone">{{ user?.primaryPhone || '未绑定手机号' }}</p>
            <p class="user-bio" v-if="user?.bio">{{ user.bio }}</p>
          </div>
        </div>

        <!-- 功能菜单 -->
        <div class="menu-options">
          <div class="menu-item" @click="goToProfile">
            <el-icon class="menu-icon"><Setting /></el-icon>
            <div class="menu-content">
              <h4>个人资料</h4>
              <p>编辑个人信息和头像</p>
            </div>
            <el-icon class="arrow-icon"><ArrowRight /></el-icon>
          </div>

          <div class="menu-item" @click="goToOrders">
            <el-icon class="menu-icon"><Document /></el-icon>
            <div class="menu-content">
              <h4>我的订单</h4>
              <p>查看和管理订单</p>
            </div>
            <el-icon class="arrow-icon"><ArrowRight /></el-icon>
          </div>

          <div class="menu-item" @click="goToCommodities">
            <el-icon class="menu-icon"><Box /></el-icon>
            <div class="menu-content">
              <h4>我的商品</h4>
              <p>管理发布的商品</p>
            </div>
            <el-icon class="arrow-icon"><ArrowRight /></el-icon>
          </div>

          <div class="menu-item" @click="goToPublish">
            <el-icon class="menu-icon"><Plus /></el-icon>
            <div class="menu-content">
              <h4>发布商品</h4>
              <p>发布新的商品</p>
            </div>
            <el-icon class="arrow-icon"><ArrowRight /></el-icon>
          </div>

          <div class="menu-divider"></div>

          <div class="menu-item logout-item" @click="handleLogout">
            <el-icon class="menu-icon"><SwitchButton /></el-icon>
            <div class="menu-content">
              <h4>退出登录</h4>
              <p>安全退出当前账户</p>
            </div>
            <el-icon class="arrow-icon"><ArrowRight /></el-icon>
          </div>
        </div>

        <!-- 返回按钮 -->
        <div class="back-section">
          <el-button @click="goBack" class="back-btn">
            <el-icon><ArrowLeft /></el-icon>
            返回
          </el-button>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '../stores/user'
import { ElMessage } from 'element-plus'
import { imageAPI } from '../api'

export default {
  name: 'UserMenu',
  setup() {
    const router = useRouter()
    const userStore = useUserStore()
    const user = ref(null)

    // 获取用户信息
    const getUserInfo = () => {
      user.value = userStore.user
    }

    // 获取头像URL
    const getAvatarUrl = (avatarUrl) => {
      if (!avatarUrl) return imageAPI.getDefaultAvatar()
      if (avatarUrl.includes('/')) return avatarUrl
      return imageAPI.getAvatar(avatarUrl)
    }

    // 处理头像错误
    const handleAvatarError = () => {
      console.log('头像加载失败')
    }

    // 跳转到个人资料
    const goToProfile = () => {
      router.push('/profile')
    }

    // 跳转到我的订单
    const goToOrders = () => {
      router.push('/orders')
    }

    // 跳转到我的商品
    const goToCommodities = () => {
      router.push('/my-commodities')
    }

    // 跳转到发布商品
    const goToPublish = () => {
      router.push('/publish')
    }

    // 退出登录
    const handleLogout = async () => {
      const confirmed = confirm('确定要退出登录吗？')
      
      if (!confirmed) {
        return
      }
      
      try {
        await userStore.logout()
      } catch (error) {
        ElMessage.error('退出登录失败')
      }
    }

    // 返回上一页
    const goBack = () => {
      router.go(-1)
    }

    onMounted(() => {
      getUserInfo()
    })

    return {
      user,
      getAvatarUrl,
      handleAvatarError,
      goToProfile,
      goToOrders,
      goToCommodities,
      goToPublish,
      handleLogout,
      goBack
    }
  }
}
</script>

<style scoped>
.user-menu-page {
  min-height: 100vh;
  background-color: #f5f5f5;
  padding: 20px 0;
}

.user-menu-container {
  max-width: 600px;
  margin: 0 auto;
  padding: 0 20px;
}

.user-menu-header {
  text-align: center;
  margin-bottom: 30px;
}

.user-menu-header h2 {
  font-size: 28px;
  margin-bottom: 8px;
}

.user-menu-header p {
  font-size: 16px;
  color: #666;
}

.user-menu-content {
  background: white;
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
  overflow: hidden;
}

.user-info-card {
  display: flex;
  align-items: center;
  padding: 24px;
  background: linear-gradient(135deg, var(--primary-color), var(--primary-light));
  color: white;
}

.user-avatar {
  width: 80px;
  height: 80px;
  border-radius: 50%;
  overflow: hidden;
  margin-right: 20px;
  border: 3px solid rgba(255, 255, 255, 0.3);
}

.user-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.default-avatar {
  width: 100%;
  height: 100%;
  background: rgba(255, 255, 255, 0.2);
  display: flex;
  align-items: center;
  justify-content: center;
}

.user-details {
  flex: 1;
}

.user-name {
  font-size: 20px;
  font-weight: normal;
  margin-bottom: 4px;
}

.user-phone {
  font-size: 14px;
  opacity: 0.9;
  margin-bottom: 8px;
}

.user-bio {
  font-size: 14px;
  opacity: 0.8;
  line-height: 1.4;
}

.menu-options {
  padding: 0;
}

.menu-item {
  display: flex;
  align-items: center;
  padding: 20px 24px;
  cursor: pointer;
  transition: background-color 0.3s ease;
  border-bottom: 1px solid #f0f0f0;
}

.menu-item:hover {
  background-color: #f8f9fa;
}

.menu-item:last-child {
  border-bottom: none;
}

.menu-icon {
  font-size: 24px;
  color: var(--primary-color);
  margin-right: 16px;
  width: 24px;
  text-align: center;
}

.menu-content {
  flex: 1;
}

.menu-content h4 {
  font-size: 16px;
  font-weight: normal;
  color: #333;
  margin-bottom: 4px;
}

.menu-content p {
  font-size: 14px;
  color: #666;
  margin: 0;
}

.arrow-icon {
  font-size: 16px;
  color: #ccc;
}

.logout-item .menu-icon {
  color: #f56c6c;
}

.logout-item .menu-content h4 {
  color: #f56c6c;
}

.menu-divider {
  height: 8px;
  background-color: #f5f5f5;
}

.back-section {
  padding: 20px 24px;
  background-color: #f8f9fa;
  text-align: center;
}

.back-btn {
  background-color: white;
  color: var(--primary-color);
  border: 1px solid var(--primary-color);
  padding: 12px 24px;
  border-radius: 8px;
  font-size: 14px;
  transition: all 0.3s ease;
}

.back-btn:hover {
  background-color: var(--primary-color);
  color: white;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .user-menu-container {
    padding: 0 15px;
  }
  
  .user-info-card {
    padding: 20px;
  }
  
  .user-avatar {
    width: 60px;
    height: 60px;
    margin-right: 16px;
  }
  
  .user-name {
    font-size: 18px;
  }
  
  .menu-item {
    padding: 16px 20px;
  }
  
  .menu-icon {
    font-size: 20px;
    margin-right: 12px;
  }
}
</style>
