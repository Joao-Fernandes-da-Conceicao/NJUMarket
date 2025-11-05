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
      // ✅ 显示错误提示
      if (err.response.data) {
        const errorMsg = err.response.data.errorMsg || err.response.data.message || '登录已过期，请重新登录'
        console.error('登录已过期:', errorMsg)
        // 动态导入ElMessage显示错误提示
        import('element-plus').then(({ ElMessage }) => {
          ElMessage.error(errorMsg)
        }).catch(() => {
          // 如果ElMessage导入失败，使用console.error
          console.error('无法显示错误提示:', errorMsg)
        })
      }
    }
    // ✅ 处理403禁止访问错误（账户被封禁/暂停）
    else if (err.response && err.response.status === 403) {
      const errorMsg = err.response.data?.errorMsg || err.response.data?.message || '账户已被禁用，无法访问'
      console.error('账户被禁用:', errorMsg)
      // 清除token并跳转到登录页
      localStorage.removeItem('adminToken')
      if (router.currentRoute.value.path !== '/login') {
        router.replace('/login')
      }
      // ✅ 显示错误提示
      import('element-plus').then(({ ElMessage }) => {
        ElMessage.error(errorMsg)
      }).catch(() => {
        console.error('无法显示错误提示:', errorMsg)
      })
    }
    // ✅ 处理其他HTTP错误（400, 500等），显示errorMsg
    else if (err.response?.data) {
      const errorMsg = err.response.data?.errorMsg || err.response.data?.message || '操作失败，请稍后重试'
      import('element-plus').then(({ ElMessage }) => {
        ElMessage.error(errorMsg)
      }).catch(() => {
        console.error('无法显示错误提示:', errorMsg)
      })
    }
    return Promise.reject(err)
  }
)

export default http


