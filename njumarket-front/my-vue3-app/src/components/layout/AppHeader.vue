<template>
  <header class="app-header">
    <div class="container">
      <div class="nav-content">
        <!-- Logo -->
        <div class="logo">
          <router-link to="/" class="logo-link">
            <h2 class="text-primary">南大市场</h2>
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
            :class="{ active: $route.path.startsWith('/orders') }"
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
        </nav>
        
        <!-- 用户操作区 -->
        <div class="nav-actions">
          <template v-if="isLoggedIn">
            <el-dropdown @command="handleCommand" trigger="click">
              <div class="user-info">
                <el-avatar :size="32" :src="getAvatarUrl(getUserAvatar)">
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
// import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '../../stores/user'
import { createSafeUserState } from '../../utils/userUtils'
import { getAvatarUrl } from '../../utils/imageUtils'
import { ElMessage, ElMessageBox } from 'element-plus'

const router = useRouter()
const userStore = useUserStore()

// 使用安全的用户状态
const { isLoggedIn, getUserDisplayName, getUserAvatar } = createSafeUserState(userStore)

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
  background: white;
  border-bottom: 1px solid var(--border-color);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
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
  font-weight: 600;
}

.nav-menu {
  display: flex;
  align-items: center;
  gap: 32px;
}

.nav-link {
  text-decoration: none;
  color: var(--text-secondary);
  font-weight: 500;
  padding: 8px 0;
  position: relative;
  transition: color 0.3s ease;
}

.nav-link:hover,
.nav-link.active {
  color: var(--text-primary);
}

.nav-link.active::after {
  content: '';
  position: absolute;
  bottom: -1px;
  left: 0;
  right: 0;
  height: 2px;
  background-color: var(--primary-color);
}

.nav-actions {
  display: flex;
  align-items: center;
  gap: 16px;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  border-radius: 6px;
  cursor: pointer;
  transition: background-color 0.3s ease;
}

.user-info:hover {
  background-color: #f5f5f5;
}

.username {
  font-weight: 500;
  color: var(--text-primary);
}

.dropdown-icon {
  font-size: 12px;
  color: var(--text-light);
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
