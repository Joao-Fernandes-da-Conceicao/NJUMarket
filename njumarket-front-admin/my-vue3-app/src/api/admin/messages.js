import http from '../http'

export const messagesAPI = {
  conversations: (page = 1, size = 10, keyword = '') => {
    // ✅ 确保keyword始终作为字符串传递（允许数字型文本搜索用户ID等）
    const params = { page, size, keyword: keyword ? String(keyword) : keyword }
    return http.get('/conversations', { params })
  },
  get: (conversationId) => http.get(`/conversations/${conversationId}`),
  updateFull: (conversationId, payload) => http.put(`/conversations/${conversationId}/full`, payload),
  listByConversation: (conversationId, page = 1, size = 10) =>
    http.get(`/conversations/${conversationId}/messages`, { params: { page, size } }),
  deleteConversation: (conversationId) => http.delete(`/conversations/${conversationId}`),
  getMessage: (messageId) => http.get(`/messages/${messageId}`),
  updateMessageFull: (messageId, payload) => http.put(`/messages/${messageId}/full`, payload),
  deleteMessage: (messageId) => http.delete(`/messages/${messageId}`)
}


