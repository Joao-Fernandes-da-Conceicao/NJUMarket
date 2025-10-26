import { createRouter, createWebHistory } from 'vue-router'
import Home from '../views/Home.vue'
import Login from '../views/Login.vue'
import Register from '../views/Register.vue'
import CommodityList from '../views/CommodityList.vue'
import CommodityDetail from '../views/CommodityDetail.vue'
import UserProfile from '../views/UserProfile.vue'
import UserMenu from '../views/UserMenu.vue'
import MyOrders from '../views/MyOrders.vue'
import SellerOrders from '../views/SellerOrders.vue'
import MyCommodities from '../views/MyCommodities.vue'
import PublishCommodity from '../views/PublishCommodity.vue'
import CreateOrder from '../views/CreateOrder.vue'
import OrderDetail from '../views/OrderDetail.vue'
import Messages from '../views/Messages.vue'
import LocalStorageDebug from '../views/LocalStorageDebug.vue'

const routes = [
  {
    path: '/',
    name: 'Home',
    component: Home
  },
  {
    path: '/login',
    name: 'Login',
    component: Login
  },
  {
    path: '/register',
    name: 'Register',
    component: Register
  },
  {
    path: '/commodities',
    name: 'CommodityList',
    component: CommodityList
  },
  {
    path: '/commodity/:id',
    name: 'CommodityDetail',
    component: CommodityDetail
  },
  {
    path: '/profile',
    name: 'UserProfile',
    component: UserProfile,
    meta: { requiresAuth: true }
  },
  {
    path: '/profile/:id',
    name: 'UserProfileDetail',
    component: UserProfile,
    meta: { requiresAuth: false }
  },
  {
    path: '/user-menu',
    name: 'UserMenu',
    component: UserMenu,
    meta: { requiresAuth: true }
  },
  {
    path: '/orders',
    name: 'MyOrders',
    component: MyOrders,
    meta: { requiresAuth: true }
  },
  {
    path: '/seller-orders',
    name: 'SellerOrders',
    component: SellerOrders,
    meta: { requiresAuth: true }
  },
  {
    path: '/my-commodities',
    name: 'MyCommodities',
    component: MyCommodities,
    meta: { requiresAuth: true }
  },
  {
    path: '/publish',
    name: 'PublishCommodity',
    component: PublishCommodity,
    meta: { requiresAuth: true }
  },
  {
    path: '/create-order/:orderId',
    name: 'CreateOrder',
    component: CreateOrder,
    meta: { requiresAuth: true }
  },
  {
    path: '/order/:id',
    name: 'OrderDetail',
    component: OrderDetail,
    meta: { requiresAuth: true }
  },
  {
    path: '/messages',
    name: 'Messages',
    component: Messages,
    meta: { requiresAuth: true }
  },
  {
    path: '/debug/localStorage',
    name: 'LocalStorageDebug',
    component: LocalStorageDebug
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 路由守卫
router.beforeEach(async (to, from, next) => {
  // 动态导入Pinia store以避免循环依赖
  const { useUserStore } = await import('../stores/user')
  const userStore = useUserStore()
  
  // 初始化用户状态（如果还没有初始化）
  if (!userStore.isLoggedIn) {
    userStore.initUser()
  }
  
  // 如果已登录用户访问登录/注册页面，重定向到首页
  if ((to.name === 'Login' || to.name === 'Register') && userStore.isLoggedIn) {
    next('/')
    return
  }
  
  // 如果未登录用户访问用户菜单，重定向到登录页
  if (to.name === 'UserMenu' && !userStore.isLoggedIn) {
    next('/login')
    return
  }
  
  // 移除自动重定向到登录页面的逻辑
  // 让用户可以在未登录状态下浏览公开页面
  // 只有在用户主动点击需要登录的功能时才提示登录
  
  next()
})

export default router
