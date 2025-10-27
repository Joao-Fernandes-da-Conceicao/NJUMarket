import http from '../http'

export const messagesAPI = {
  conversations: (page = 1, size = 10, keyword = '') =>
    http.get('/conversations', { params: { page, size, keyword } }),
  listByConversation: (conversationId, page = 1, size = 10) =>
    http.get(`/conversations/${conversationId}/messages`, { params: { page, size } }),
  deleteConversation: (conversationId) => http.delete(`/conversations/${conversationId}`),
  deleteMessage: (messageId) => http.delete(`/messages/${messageId}`)
}


