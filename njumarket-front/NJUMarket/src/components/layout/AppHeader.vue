<template>
  <header class="app-header">
    <div class="container">
      <div class="nav-content">
        <!-- 移动端用户头像（左上角） -->
        <div class="mobile-user-area">
          <div class="mobile-user-button" @click="toggleMobileMenu">
            <el-avatar :size="32" :src="getAvatarUrl(getUserAvatar)" v-if="isLoggedIn">
              {{ getUserDisplayName.charAt(0) }}
            </el-avatar>
            <el-avatar v-else :size="32" class="not-logged-avatar">
              <el-icon><User /></el-icon>
            </el-avatar>
          </div>
        </div>
        
        <!-- Logo -->
        <div class="logo">
          <router-link to="/" class="logo-link">
            <h2 class="text-primary">NJUMarket 南大集市</h2>
          </router-link>
        </div>
        
        <!-- 导航菜单（桌面端） -->
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
            :class="{ active: $route.path.startsWith('/orders') && !$route.path.startsWith('/seller-orders') }"
          >
            <UnreadBadge :count="buyerOrderHasNew ? 1 : 0" type="dot" :force-hide="!buyerOrderHasNew">
              我的订单
            </UnreadBadge>
          </router-link>
          <router-link 
            v-if="isLoggedIn" 
            to="/seller-orders" 
            class="nav-link" 
            :class="{ active: $route.path.startsWith('/seller-orders') }"
          >
            <UnreadBadge :count="sellerOrderHasNew ? 1 : 0" type="dot" :force-hide="!sellerOrderHasNew">
              卖家订单
            </UnreadBadge>
          </router-link>
          <router-link 
            v-if="isLoggedIn" 
            to="/home" 
            class="nav-link" 
            :class="{ active: $route.path === '/home' }"
          >
            我的主页
          </router-link>
          <router-link 
            v-if="isLoggedIn" 
            to="/messages" 
            class="nav-link" 
            :class="{ active: $route.path.startsWith('/messages') }"
          >
            <UnreadBadge :count="unreadCount" type="number" :max="99">
              消息
            </UnreadBadge>
          </router-link>
        </nav>
        
        <!-- 用户操作区（桌面端） -->
        <div class="nav-actions">
          <template v-if="isLoggedIn">
            <div class="user-info" @click="toggleDesktopMenu" ref="userInfoRef">
              <el-avatar :size="24" :src="getAvatarUrl(getUserAvatar)">
                {{ getUserDisplayName.charAt(0) }}
              </el-avatar>
              <span class="username">{{ getUserDisplayName }}</span>
              <el-icon class="dropdown-icon">
                <ArrowDown />
              </el-icon>
            </div>
            
            <!-- 自定义弹出式菜单 -->
            <transition name="fade">
              <div v-if="showDesktopMenu" class="desktop-menu-popup" @click.stop>
                <div class="popup-item" @click="handleMenuClick('profile')">
                  <el-icon><User /></el-icon>
                  个人资料
                </div>
                <div class="popup-item" @click="handleMenuClick('orders')">
                  <el-icon><ShoppingCart /></el-icon>
                  我的订单
                </div>
                <div class="popup-item" @click="handleMenuClick('seller-orders')">
                  <el-icon><Box /></el-icon>
                  卖家订单
                </div>
                <div class="popup-divider"></div>
                <div class="popup-item" @click="handleMenuClick('publish')">
                  <el-icon><Plus /></el-icon>
                  发布商品
                </div>
                <div class="popup-divider"></div>
                <div class="popup-item popup-item-danger" @click="handleMenuClick('logout')">
                  <el-icon><SwitchButton /></el-icon>
                  退出登录
                </div>
              </div>
            </transition>
          </template>
          <template v-else>
            <UnifiedButton type="primary" @click="$router.push('/login')">
              登录
            </UnifiedButton>
            <UnifiedButton @click="$router.push('/register')">
              注册
            </UnifiedButton>
          </template>
        </div>
      </div>
    </div>
    
    <!-- 移动端侧边栏菜单 -->
    <transition name="slide-right">
      <div v-if="showMobileMenu" class="mobile-menu-overlay" @click="closeMobileMenu">
        <div class="mobile-menu" @click.stop>
          <div class="mobile-menu-header">
            <div class="mobile-user-info" v-if="isLoggedIn" @click.stop>
              <el-avatar :size="48" :src="getAvatarUrl(getUserAvatar)">
                {{ getUserDisplayName.charAt(0) }}
              </el-avatar>
              <span class="mobile-username">{{ getUserDisplayName }}</span>
            </div>
            <div v-else class="mobile-user-info" @click="handleMobileLogin">
              <el-avatar :size="48" class="not-logged-avatar-large">
                <el-icon><User /></el-icon>
              </el-avatar>
              <span class="mobile-username">未登录</span>
            </div>
            <el-icon class="close-icon" @click="closeMobileMenu">
              <Close />
            </el-icon>
          </div>
          
          <div class="mobile-menu-content">
            <div v-if="isLoggedIn" class="mobile-menu-items">
              <div class="mobile-menu-item" @click="handleMobileItem('home')">
                <el-icon><HomeFilled /></el-icon>
                <span>首页</span>
              </div>
              <div class="mobile-menu-item" @click="handleMobileItem('commodities')">
                <el-icon><Goods /></el-icon>
                <span>商品</span>
              </div>
              <div class="mobile-menu-item" @click="handleMobileItem('orders')">
                <el-icon><ShoppingCart /></el-icon>
                <UnreadBadge :count="buyerOrderHasNew ? 1 : 0" type="dot" :force-hide="!buyerOrderHasNew" badge-class="mobile-order-badge">
                  <span>我的订单</span>
                </UnreadBadge>
              </div>
              <div class="mobile-menu-item" @click="handleMobileItem('my-home')">
                <el-icon><Box /></el-icon>
                <span>我的主页</span>
              </div>
              <div class="mobile-menu-item" @click="handleMobileItem('seller-orders')">
                <el-icon><Box /></el-icon>
                <UnreadBadge :count="sellerOrderHasNew ? 1 : 0" type="dot" :force-hide="!sellerOrderHasNew" badge-class="mobile-order-badge">
                  <span>卖家订单</span>
                </UnreadBadge>
              </div>
              <div class="mobile-menu-item" @click="handleMobileItem('messages')">
                <el-icon><Message /></el-icon>
                <span>消息</span>
                <UnreadBadge :count="unreadCount" type="number" :max="99" />
              </div>
              <div class="mobile-menu-item" @click="handleMobileItem('profile')">
                <el-icon><User /></el-icon>
                <span>个人资料</span>
              </div>
              
              <div class="mobile-menu-item mobile-menu-item-danger" @click="handleMobileItem('logout')">
                <el-icon><SwitchButton /></el-icon>
                <span>退出登录</span>
              </div>
            </div>
            <div v-else class="mobile-menu-items">
              <div class="mobile-menu-item" @click="handleMobileItem('home')">
                <el-icon><HomeFilled /></el-icon>
                <span>首页</span>
              </div>
              <div class="mobile-menu-item" @click="handleMobileItem('commodities')">
                <el-icon><Goods /></el-icon>
                <span>商品</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </transition>
  </header>
</template>

<script setup>
import { BREAKPOINT_MOBILE } from '../../config/breakpoints'
import { ref, computed, onMounted, onUnmounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '../../stores/user'
import { useMessageStore } from '../../stores/message'
import { useOrderStore } from '../../stores/order'
import { createSafeUserState } from '../../utils/userUtils'
import { getAvatarUrl } from '../../utils/imageUtils'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowDown, User, ShoppingCart, Box, Plus, SwitchButton, Close, HomeFilled, Goods, Message } from '@element-plus/icons-vue'
import UnifiedButton from '../common/UnifiedButton.vue'
import UnreadBadge from '../common/UnreadBadge.vue'

const router = useRouter()
const userStore = useUserStore()
const messageStore = useMessageStore()
const orderStore = useOrderStore()

// 使用安全的用户状态
const { isLoggedIn, getUserDisplayName, getUserAvatar } = createSafeUserState(userStore)

// 未读消息数 - 使用 store
const unreadCount = computed(() => messageStore.totalUnreadCount)

// 订单变化提醒角标状态
const sellerOrderHasNew = computed(() => orderStore.hasSellerOrderChange)
const buyerOrderHasNew = computed(() => orderStore.hasBuyerOrderChange)

// 移动端检测
const isMobile = ref(false)
const showMobileMenu = ref(false)
const showDesktopMenu = ref(false)
const userInfoRef = ref(null)

// 检测是否为移动设备
const detectMobile = () => {
  const width = window.innerWidth
  const height = window.innerHeight
  const userAgent = navigator.userAgent.toLowerCase()
  
  // 检测移动设备：使用统一的断点常量
  // 宽度小于移动端断点 或者 用户代理包含移动设备标识
  // 同时考虑长宽比，手机通常长宽比较小
  // 判断纵横比：横向/纵向 < 4/3 认为是竖屏设备（手机或平板竖屏）
  // 4:3的比例约为1.33，我们使用1.4作为阈值略大于4:3
  const aspectRatio = width / height
  const isVerticalOrientation = aspectRatio < 1.4 // 4:3约为1.33，设置为1.4
  
  // 判断设备尺寸：检查viewport元标签或使用推断方法
  // 高分辨率手机（如2880x1440）需要考虑设备像素比
  let isSmallDevice = false
  const dpr = window.devicePixelRatio || 1
  
  // 推断设备是否为手机：
  // 1. 如果宽度*设备像素比相对较小，且高度>宽度*1.5
  // 2. 或者UA包含移动设备标识
  // 3. 例如：2880x1440在DPR=2时逻辑宽度=1440，在DPR=4时逻辑宽度=720
  const logicalWidth = width * dpr
  const logicalHeight = height * dpr
  
  // 如果逻辑分辨率在手机范围内（宽度<2000且高度>宽度*1.5）
  if (logicalWidth < 2000 && logicalHeight > logicalWidth * 1.5) {
    isSmallDevice = true
  }
  
  // 判断逻辑：
  // 1. 如果分辨率宽度小于移动端断点，肯定是手机
  // 2. 如果纵横比<1.4且（UA包含移动设备标识 或 是小设备）
  const isMobileWidth = width < BREAKPOINT_MOBILE
  const isMobileUA = /mobile|android|iphone|ipad|phone/i.test(userAgent)
  
  isMobile.value = isMobileWidth || (isVerticalOrientation && (isMobileUA || isSmallDevice))
}

// 移动端菜单控制
const toggleMobileMenu = () => {
  showMobileMenu.value = !showMobileMenu.value
}

const closeMobileMenu = () => {
  showMobileMenu.value = false
}

// 桌面端菜单控制
const toggleDesktopMenu = () => {
  showDesktopMenu.value = !showDesktopMenu.value
}

const closeDesktopMenu = () => {
  showDesktopMenu.value = false
}

// 处理桌面端菜单点击
const handleMenuClick = async (command) => {
  closeDesktopMenu()
  
  setTimeout(() => {
    switch (command) {
      case 'profile':
        router.push('/profile')
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
        handleDesktopLogout()
        break
    }
  }, 100)
}

// 桌面端退出登录
const handleDesktopLogout = async () => {
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
}

// 点击外部区域关闭菜单
const handleClickOutside = (event) => {
  if (userInfoRef.value && !userInfoRef.value.contains(event.target)) {
    closeDesktopMenu()
  }
}

onMounted(() => {
  // 初始检测
  detectMobile()
  
  // 监听窗口大小变化
  window.addEventListener('resize', detectMobile)
  
  // 监听点击外部区域
  document.addEventListener('click', handleClickOutside)
  
  // 初始获取未读数（WebSocket 会实时更新，不需要定时轮询）
  messageStore.fetchUnreadCount()
})

onUnmounted(() => {
  // 移除监听器
  window.removeEventListener('resize', detectMobile)
  document.removeEventListener('click', handleClickOutside)
})

// 移动端菜单项点击处理
const handleMobileItem = (item) => {
  // 先关闭菜单，避免遮挡页面
  showMobileMenu.value = false
  
  // 使用 nextTick 确保 DOM 更新完成，避免 ResizeObserver 警告
  nextTick(() => {
    setTimeout(() => {
      switch (item) {
        case 'home':
          router.push('/')
          break
        case 'commodities':
          router.push('/commodities')
          break
        case 'orders':
          router.push('/orders')
          break
        case 'my-home':
          router.push('/home')
          break
        case 'seller-orders':
          router.push('/seller-orders')
          break
        case 'messages':
          router.push('/messages')
          break
        case 'profile':
          router.push('/profile')
          break
        case 'publish':
          router.push('/publish')
          break
        case 'logout':
          handleMobileLogout()
          break
      }
    }, 250) // 减少延迟时间，因为使用了 nextTick
  })
}

// 移动端登录处理
const handleMobileLogin = () => {
  closeMobileMenu()
  router.push('/login')
}

// 移动端退出登录处理
const handleMobileLogout = async () => {
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
}
</script>

<style scoped>
.app-header {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: 99999; /* 始终占据最高优先级 */
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
  position: relative;
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

/* 自定义弹出式菜单 */
.desktop-menu-popup {
  position: absolute;
  top: calc(100% + 8px);
  right: 0;
  background: white;
  border: 1px solid var(--primary-color);
  border-radius: 20px;
  box-shadow: 0 4px 16px rgba(106, 1, 94, 0.2);
  width: 100%;
  overflow: hidden;
  z-index: 99999; /* 与 header 同级的弹出菜单 */
  align-items: center;
}

.popup-item {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  padding: 10px 16px;
  cursor: pointer;
  transition: all 0.3s ease;
  font-size: 14px;
  color: #333;
  border-radius: 20px;
  margin: 4px 8px;
}

.popup-item:hover {
  background-color: var(--primary-color);
  color: white;
}

.popup-item .el-icon {
  font-size: 16px;
  color: var(--primary-color);
  transition: color 0.3s ease;
}

.popup-item:hover .el-icon {
  color: white;
}

.popup-item-danger {
  color: #f56c6c;
}

.popup-item-danger .el-icon {
  color: #f56c6c;
}

.popup-item-danger:hover {
  background-color: #f56c6c !important;
  color: white;
}

.popup-item-danger:hover .el-icon {
  color: white;
}

.popup-divider {
  display: none;
}

/* 淡入淡出动画 */
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
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

/* 移动端用户区域 */
.mobile-user-area {
  display: none; /* 桌面端默认隐藏 */
  align-items: center;
}

.mobile-user-button {
  cursor: pointer;
  transition: transform 0.2s ease;
}

.mobile-user-button:active {
  transform: scale(0.95);
}

.logo-mobile {
  flex: 1;
  text-align: center;
  margin: 0 auto;
}

.logo h2 {
  margin: 0;
  font-size: 20px;
  font-weight: normal;
  color: white;
}

/* 移动端Logo居中 - 综合移动端查询 */
@media (max-width: 900px) {
  .nav-content {
    position: relative;
  }
  
  /* 移动端显示移动端用户区域 */
  .mobile-user-area {
    display: flex;
    position: absolute;
    left: 0;
    z-index: 10;
  }
  
  /* 移动端隐藏桌面端导航和操作区 */
  .nav-menu,
  .nav-actions {
    display: none;
  }
  
  .logo {
    flex: 1;
    text-align: center;
    position: absolute;
    left: 50%;
    transform: translateX(-50%);
    width: 100%;
    pointer-events: none;
  }
  
  .logo h2 {
    margin: 0;
    pointer-events: auto;
  }
}

/* 移动端侧边栏菜单 */
.mobile-menu-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(0, 0, 0, 0.5);
  z-index: 99998; /* 低于 header 但高于其他内容 */
  display: flex;
  animation: fadeIn 0.3s ease;
}

.mobile-menu {
  position: fixed;
  top: 0;
  left: 0;
  bottom: 0;
  width: 280px;
  max-width: 85vw;
  background-color: white;
  box-shadow: 2px 0 8px rgba(0, 0, 0, 0.15);
  display: flex;
  flex-direction: column;
  animation: slideRight 0.3s ease;
}

.mobile-menu-header {
  padding: 20px;
  background-color: var(--primary-color);
  color: white;
  display: flex;
  align-items: center;
  justify-content: space-between;
  min-height: 80px;
}

.mobile-user-info {
  display: flex;
  align-items: center;
  gap: 12px;
  flex: 1;
}

.mobile-username {
  font-size: 16px;
  font-weight: normal;
  color: white;
}

.not-logged-avatar-large {
  background-color: var(--primary-color);
  cursor: pointer;
}

.not-logged-avatar-large :deep(.el-icon) {
  color: white;
  font-size: 24px;
}

.close-icon {
  font-size: 24px;
  cursor: pointer;
  color: white;
  transition: transform 0.2s ease;
}

.close-icon:hover {
  transform: rotate(90deg);
}

.mobile-menu-content {
  flex: 1;
  overflow-y: auto;
  padding: 20px 0;
}

.mobile-menu-items {
  display: flex;
  flex-direction: column;
}

.mobile-menu-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 20px;
  cursor: pointer;
  transition: background-color 0.2s ease;
  color: var(--primary-color);
  font-size: 15px;
}

.mobile-menu-item:active {
  background-color: #f5f5f5;
}

.mobile-menu-item .el-icon {
  font-size: 20px;
  color: var(--primary-color);
}

.mobile-menu-item-danger {
  color: #f56c6c;
}

.mobile-menu-item-danger .el-icon {
  color: #f56c6c;
}

.mobile-menu-divider {
  display: none;
}

.not-logged-avatar {
  background-color: var(--primary-color);
  cursor: pointer;
}

.not-logged-avatar :deep(.el-icon) {
  color: white;
  font-size: 18px;
}

/* 侧边栏动画 */
.slide-right-enter-active,
.slide-right-leave-active {
  transition: all 0.3s ease;
}

.slide-right-enter-from,
.slide-right-leave-to {
  opacity: 0;
}

.slide-right-enter-from .mobile-menu,
.slide-right-leave-to .mobile-menu {
  transform: translateX(-100%);
}

.slide-right-enter-to .mobile-menu,
.slide-right-leave-from .mobile-menu {
  transform: translateX(0);
}

@keyframes fadeIn {
  from {
    opacity: 0;
  }
  to {
    opacity: 1;
  }
}

@keyframes slideRight {
  from {
    transform: translateX(-100%);
  }
  to {
    transform: translateX(0);
  }
}

/* 响应式设计 - 综合移动端查询 */
@media (max-width: 900px) {
  .nav-link {
    font-size: 14px;
  }
  
  .username {
    display: none;
  }
}
</style>
