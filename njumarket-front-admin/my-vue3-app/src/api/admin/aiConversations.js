import http from '../http'

export const aiConversationAPI = {
  getConversations: (params) => http.get('/ai-conversations', { params }),
  getConversationDetail: (conversationId) => http.get(`/ai-conversations/${conversationId}`),
  deleteConversation: (conversationId) => http.delete(`/ai-conversations/${conversationId}`),
  batchDeleteConversations: (conversationIds) =>
    http.delete('/ai-conversations/batch', { data: conversationIds }),
  restoreConversation: (conversationId) => http.post(`/ai-conversations/${conversationId}/restore`),
  getStats: () => http.get('/ai-conversations/statistics')
}


