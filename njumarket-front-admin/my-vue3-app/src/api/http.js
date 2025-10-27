import axios from 'axios'

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
    return Promise.reject(err)
  }
)

export default http


