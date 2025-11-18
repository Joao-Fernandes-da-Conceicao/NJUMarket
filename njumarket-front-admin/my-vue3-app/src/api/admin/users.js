import http from '../http'

export const usersAPI = {
  list: (page = 1, size = 10, query = {}) =>
    http.get('/users', { params: { page, size, ...query } }),
  get: (userId) => http.get(`/users/${userId}`),
  updateStatus: (userId, status) => http.put(`/users/${userId}/status`, null, { params: { status } }),
  updateBasic: (userId, payload) => http.put(`/users/${userId}`, null, { params: payload }),
  remove: (userId) => http.delete(`/users/${userId}`),
  updateFull: (userId, payload) => http.put(`/users/${userId}/full`, payload),
  listAddresses: (userId) => http.get(`/users/${userId}/addresses`),
  createAddress: (userId, payload) => http.post(`/users/${userId}/addresses`, payload),
  updateAddress: (userId, addressId, payload) => http.put(`/users/${userId}/addresses/${addressId}`, payload),
  deleteAddress: (userId, addressId) => http.delete(`/users/${userId}/addresses/${addressId}`),
  setDefaultAddress: (userId, addressId) => http.put(`/users/${userId}/addresses/${addressId}/default`)
}


