import axios from 'axios'
import router from '../router'

const http = axios.create({
  baseURL: process.env.VUE_APP_ADMIN_BASE_URL || 'http://localhost:8080/api/admin',
  timeout: 15000
})

http.interceptors.request.use((config) => {
  const token = localStorage.getItem('adminToken')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

http.interceptors.response.use(
  (res) => res.data,
  (err) => {
    // ✅ 处理401未授权错误
    if (err.response && err.response.status === 401) {
      // 清除token
      localStorage.removeItem('adminToken')
      // 跳转到登录页（如果不是在登录页）
      if (router.currentRoute.value.path !== '/login') {
        router.replace('/login')
      }
      // 可以显示错误提示
      if (err.response.data && err.response.data.message) {
        console.error('登录已过期:', err.response.data.message)
      }
    }
    return Promise.reject(err)
  }
)

export default http


