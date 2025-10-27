import http from '../http'

export const usersAPI = {
  list: (page = 1, size = 10, query = {}) =>
    http.get('/users', { params: { page, size, ...query } }),
  get: (userId) => http.get(`/users/${userId}`),
  updateStatus: (userId, status) => http.put(`/users/${userId}/status`, null, { params: { status } }),
  updateBasic: (userId, payload) => http.put(`/users/${userId}`, null, { params: payload }),
  remove: (userId) => http.delete(`/users/${userId}`),
  updateFull: (userId, payload) => http.put(`/users/${userId}/full`, payload)
}


