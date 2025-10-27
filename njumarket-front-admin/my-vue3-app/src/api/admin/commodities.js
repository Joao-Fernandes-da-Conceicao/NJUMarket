import http from '../http'

export const commoditiesAPI = {
  list: (page = 1, size = 10, query = {}) =>
    http.get('/commodities', { params: { page, size, ...query } }),
  get: (id) => http.get(`/commodities/${id}`),
  updateStatus: (id, status) => http.put(`/commodities/${id}/status`, null, { params: { status } }),
  remove: (id) => http.delete(`/commodities/${id}`),
  updateFull: (id, payload) => http.put(`/commodities/${id}/full`, payload)
}


