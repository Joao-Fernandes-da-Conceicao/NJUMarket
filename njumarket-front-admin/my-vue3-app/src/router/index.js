import { createRouter, createWebHistory } from 'vue-router'

const Dashboard = () => import('../views/Dashboard.vue')
const Users = () => import('../views/Users.vue')
const UserEdit = () => import('../views/UserEdit.vue')
const Commodities = () => import('../views/Commodities.vue')
const CommodityEdit = () => import('../views/CommodityEdit.vue')
const Orders = () => import('../views/Orders.vue')
const Messages = () => import('../views/Messages.vue')
const Login = () => import('../views/Login.vue')

const routes = [
  { path: '/login', name: 'Login', component: Login },
  { path: '/', name: 'Dashboard', component: Dashboard },
  { path: '/users', name: 'Users', component: Users },
  { path: '/users/:userId/edit', name: 'UserEdit', component: UserEdit },
  { path: '/commodities', name: 'Commodities', component: Commodities },
  { path: '/commodities/:commodityId/edit', name: 'CommodityEdit', component: CommodityEdit },
  { path: '/orders', name: 'Orders', component: Orders },
  { path: '/messages', name: 'Messages', component: Messages }
]

const router = createRouter({
  history: createWebHistory(process.env.BASE_URL),
  routes
})

router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('adminToken')
  if (to.path === '/login') {
    return token ? next('/') : next()
  }
  if (!token) return next('/login')
  next()
})

export default router


