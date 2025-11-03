import http from '../http'

export const commoditiesAPI = {
  list: (page = 1, size = 10, query = {}) => {
    // ✅ 确保keyword始终作为字符串传递（允许数字型文本搜索用户ID等）
    const params = { page, size, ...query }
    if (params.keyword !== undefined && params.keyword !== null && params.keyword !== '') {
      params.keyword = String(params.keyword)
    }
    return http.get('/commodities', { params })
  },
  get: (id) => http.get(`/commodities/${id}`),
  updateStatus: (id, status) => http.put(`/commodities/${id}/status`, null, { params: { status } }),
  remove: (id) => http.delete(`/commodities/${id}`),
  updateFull: (id, payload) => http.put(`/commodities/${id}/full`, payload)
}


