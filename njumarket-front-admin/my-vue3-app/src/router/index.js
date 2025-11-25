import { createRouter, createWebHistory } from 'vue-router'

const Dashboard = () => import('../views/Dashboard.vue')
const Users = () => import('../views/Users.vue')
const UserEdit = () => import('../views/UserEdit.vue')
const UserAddressManagement = () => import('../views/UserAddressManagement.vue')
const Commodities = () => import('../views/Commodities.vue')
const CommodityEdit = () => import('../views/CommodityEdit.vue')
const Orders = () => import('../views/Orders.vue')
const OrderEdit = () => import('../views/OrderEdit.vue')
const Messages = () => import('../views/Messages.vue')
const ConversationDetail = () => import('../views/ConversationDetail.vue')
const ConversationEdit = () => import('../views/ConversationEdit.vue')
const Admins = () => import('../views/Admins.vue')
const AdminEdit = () => import('../views/AdminEdit.vue')
const AdminCreate = () => import('../views/AdminCreate.vue')
const Elasticsearch = () => import('../views/Elasticsearch.vue')
const AIEngine = () => import('../views/AIEngine.vue')
const Login = () => import('../views/Login.vue')

const routes = [
  { path: '/login', name: 'Login', component: Login },
  { path: '/', name: 'Dashboard', component: Dashboard },
  { path: '/users', name: 'Users', component: Users },
  { path: '/users/:userId/edit', name: 'UserEdit', component: UserEdit },
  { path: '/users/:userId/addresses', name: 'UserAddressManagement', component: UserAddressManagement },
  { path: '/commodities', name: 'Commodities', component: Commodities },
  { path: '/commodities/:commodityId/edit', name: 'CommodityEdit', component: CommodityEdit },
  { path: '/orders', name: 'Orders', component: Orders },
  { path: '/orders/:orderId/edit', name: 'OrderEdit', component: OrderEdit },
  { path: '/messages', name: 'Messages', component: Messages },
  { path: '/messages/:conversationId/detail', name: 'ConversationDetail', component: ConversationDetail },
  { path: '/messages/:conversationId/edit', name: 'ConversationEdit', component: ConversationEdit },
  { path: '/admins', name: 'Admins', component: Admins },
  { path: '/admins/create', name: 'AdminCreate', component: AdminCreate },
  { path: '/admins/:adminId/edit', name: 'AdminEdit', component: AdminEdit },
  { path: '/elasticsearch', name: 'Elasticsearch', component: Elasticsearch },
  { path: '/ai-engine', name: 'AIEngine', component: AIEngine }
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


