import http from '../http'

export const adminsAPI = {
  list: (page = 1, size = 10, keyword = '', accountStatus = '', sortProp = '', sortOrder = '') => {
    // ✅ 确保keyword始终作为字符串传递
    const params = { page, size }
    if (keyword) {
      params.keyword = String(keyword)
    }
    if (accountStatus) {
      params.accountStatus = accountStatus
    }
    if (sortProp) {
      params.sortProp = sortProp
      params.sortOrder = sortOrder || 'desc'
    }
    // ✅ baseURL已包含/api/admin，这里只需要相对路径
    return http.get('/list', { params })
  },
  get: (adminId) => http.get(`/${adminId}`),
  create: (payload) => http.post('/create', payload),
  updateFull: (adminId, payload) => http.put(`/${adminId}/full`, payload),
  remove: (adminId) => http.delete(`/${adminId}`)
}

