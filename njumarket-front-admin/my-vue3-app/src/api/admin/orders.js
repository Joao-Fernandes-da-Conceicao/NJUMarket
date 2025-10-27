import http from '../http'

export const ordersAPI = {
  list: (page = 1, size = 10, query = {}) =>
    http.get('/orders', { params: { page, size, ...query } }),
  get: (id) => http.get(`/orders/${id}`),
  update: (id, payload) => http.put(`/orders/${id}`, null, { params: payload }),
  remove: (id) => http.delete(`/orders/${id}`)
}


