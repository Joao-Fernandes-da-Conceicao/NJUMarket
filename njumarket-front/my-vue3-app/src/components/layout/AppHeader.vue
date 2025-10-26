<template>
  <header class="app-header">
    <div class="container">
      <div class="nav-content">
        <!-- Logo -->
        <div class="logo">
          <router-link to="/" class="logo-link">
            <h2 class="text-primary">NJUMarket 南大市场</h2>
          </router-link>
        </div>
        
        <!-- 导航菜单 -->
        <nav class="nav-menu">
          <router-link to="/" class="nav-link" :class="{ active: $route.path === '/' }">
            首页
          </router-link>
          <router-link to="/commodities" class="nav-link" :class="{ active: $route.path.startsWith('/commodities') }">
            商品
          </router-link>
          <router-link 
            v-if="isLoggedIn" 
            to="/orders" 
            class="nav-link" 
            :class="{ active: $route.path.startsWith('/orders') || $route.path.startsWith('/seller-orders') }"
          >
            我的订单
          </router-link>
          <router-link 
            v-if="isLoggedIn" 
            to="/my-commodities" 
            class="nav-link" 
            :class="{ active: $route.path.startsWith('/my-commodities') }"
          >
            我的商品
          </router-link>
          <router-link 
            v-if="isLoggedIn" 
            to="/messages" 
            class="nav-link" 
            :class="{ active: $route.path.startsWith('/messages') }"
          >
            <el-badge :value="unreadCount" :hidden="unreadCount === 0" :max="99">
              消息
            </el-badge>
          </router-link>
        </nav>
        
        <!-- 用户操作区 -->
        <div class="nav-actions">
          <template v-if="isLoggedIn">
            <el-dropdown @command="handleCommand" trigger="click">
              <div class="user-info">
                <el-avatar :size="24" :src="getAvatarUrl(getUserAvatar)">
                  {{ getUserDisplayName.charAt(0) }}
                </el-avatar>
                <span class="username">{{ getUserDisplayName }}</span>
                <el-icon class="dropdown-icon">
                  <ArrowDown />
                </el-icon>
              </div>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="profile">
                    <el-icon><User /></el-icon>
                    个人资料
                  </el-dropdown-item>
                  <el-dropdown-item command="user-menu">
                    <el-icon><Setting /></el-icon>
                    用户中心
                  </el-dropdown-item>
                  <el-dropdown-item command="orders">
                    <el-icon><ShoppingCart /></el-icon>
                    我的订单
                  </el-dropdown-item>
                  <el-dropdown-item command="seller-orders">
                    <el-icon><Box /></el-icon>
                    卖家订单
                  </el-dropdown-item>
                  <el-dropdown-item command="publish" divided>
                    <el-icon><Plus /></el-icon>
                    发布商品
                  </el-dropdown-item>
                  <el-dropdown-item command="logout" divided>
                    <el-icon><SwitchButton /></el-icon>
                    退出登录
                  </el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </template>
          <template v-else>
            <el-button type="primary" @click="$router.push('/login')">
              登录
            </el-button>
            <el-button @click="$router.push('/register')">
              注册
            </el-button>
          </template>
        </div>
      </div>
    </div>
  </header>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '../../stores/user'
import { createSafeUserState } from '../../utils/userUtils'
import { getAvatarUrl } from '../../utils/imageUtils'
import { contactAPI } from '../../api'
import { ElMessage, ElMessageBox } from 'element-plus'

const router = useRouter()
const userStore = useUserStore()

// 使用安全的用户状态
const { isLoggedIn, getUserDisplayName, getUserAvatar } = createSafeUserState(userStore)

// 未读消息数
const unreadCount = ref(0)

// 获取未读消息数
const fetchUnreadCount = async () => {
  if (!isLoggedIn.value) {
    unreadCount.value = 0
    return
  }
  
  try {
    const response = await contactAPI.getUnreadCount()
    if (response.success) {
      unreadCount.value = response.data || 0
    }
  } catch (error) {
    console.error('获取未读数失败:', error)
  }
}

// 定时刷新未读数（每30秒）
let unreadInterval = null

onMounted(() => {
  fetchUnreadCount()
  unreadInterval = setInterval(fetchUnreadCount, 30000)
})

onUnmounted(() => {
  if (unreadInterval) {
    clearInterval(unreadInterval)
  }
})

// 处理下拉菜单命令
const handleCommand = async (command) => {
  switch (command) {
    case 'profile':
      router.push('/profile')
      break
    case 'user-menu':
      router.push('/user-menu')
      break
    case 'orders':
      router.push('/orders')
      break
    case 'seller-orders':
      router.push('/seller-orders')
      break
    case 'publish':
      router.push('/publish')
      break
    case 'logout':
      try {
        await ElMessageBox.confirm('确定要退出登录吗？', '提示', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        })
        await userStore.logout()
        ElMessage.success('已退出登录')
        router.push('/')
      } catch (error) {
        if (error !== 'cancel') {
          ElMessage.error('退出登录失败')
        }
      }
      break
  }
}
</script>

<style scoped>
.app-header {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: 1000;
  background: var(--primary-color);
  box-shadow: 0 2px 8px rgba(106, 1, 94, 0.2);
}

.nav-content {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 60px;
}

.logo-link {
  text-decoration: none;
  color: inherit;
}

.logo h2 {
  margin: 0;
  font-size: 24px;
  font-weight: normal;
  color: white;
}

.nav-menu {
  display: flex;
  align-items: center;
  gap: 32px;
}

.nav-link {
  text-decoration: none;
  color: white;
  font-weight: normal;
  padding: 8px 16px;
  border-radius: 20px;
  border: 1px solid white;
  background-color: var(--primary-color);
  transition: all 0.3s ease;
  position: relative;
}

.nav-link:hover {
  background-color: rgba(255, 255, 255, 0.1);
}

.nav-link.active {
  color: var(--primary-color);
  background-color: white;
  border: 1px solid white;
}

.nav-actions {
  display: flex;
  align-items: center;
  gap: 16px;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  border-radius: 20px;
  border: 1px solid white;
  background-color: var(--primary-color);
  cursor: pointer;
  transition: all 0.3s ease;
  min-width: fit-content;
  white-space: nowrap;
}

.user-info:hover {
  background-color: rgba(255, 255, 255, 0.1);
}

.username {
  font-weight: normal;
  color: white;
  font-size: 14px;
}

.dropdown-icon {
  font-size: 12px;
  color: white;
}

/* 登录和注册按钮药丸形状设计 */
.nav-actions .el-button {
  border-radius: 20px; /* 药丸形状 */
  font-weight: normal;
  transition: all 0.3s ease;
}

.nav-actions .el-button--primary {
  background-color: white;
  color: var(--primary-color);
  border: 1px solid white;
}

.nav-actions .el-button--primary:hover {
  background-color: rgba(255, 255, 255, 0.9);
  transform: translateY(-1px);
}

.nav-actions .el-button:not(.el-button--primary) {
  background-color: transparent;
  color: white;
  border: 1px solid white;
}

.nav-actions .el-button:not(.el-button--primary):hover {
  background-color: rgba(255, 255, 255, 0.1);
  transform: translateY(-1px);
}

/* 响应式设计 */
@media (max-width: 768px) {
  .nav-menu {
    gap: 16px;
  }
  
  .nav-link {
    font-size: 14px;
  }
  
  .username {
    display: none;
  }
  
  .nav-actions {
    gap: 8px;
  }
}

@media (max-width: 480px) {
  .nav-menu {
    display: none;
  }
  
  .logo h2 {
    font-size: 20px;
  }
}
</style>
