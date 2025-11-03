import http from '../http'

export const ordersAPI = {
  list: (page = 1, size = 10, query = {}) => {
    // ✅ 确保keyword始终作为字符串传递（允许数字型文本搜索用户ID等）
    const params = { page, size, ...query }
    if (params.keyword !== undefined && params.keyword !== null && params.keyword !== '') {
      params.keyword = String(params.keyword)
    }
    return http.get('/orders', { params })
  },
  get: (id) => http.get(`/orders/${id}`),
  update: (id, payload) => http.put(`/orders/${id}`, null, { params: payload }),
  updateFull: (id, payload) => http.put(`/orders/${id}/full`, payload),
  remove: (id) => http.delete(`/orders/${id}`)
}


