import request from './index'

export const contactAPI = {
  /**
   * 发送消息
   */
  sendMessage(data) {
    return request.post('/contact/send', data)
  },
  
  /**
   * 获取对话列表
   */
  getConversations(page = 1, size = 20) {
    return request.get('/contact/conversations', {
      params: { page, size }
    })
  },
  
  /**
   * 获取对话详情（包含消息历史）
   */
  getConversationDetail(conversationId, page = 1, size = 50) {
    return request.get(`/contact/conversations/${conversationId}`, {
      params: { page, size }
    })
  },
  
  /**
   * 创建或获取对话
   */
  createConversation(otherUserId, commodityId = null, orderId = null) {
    return request.post('/contact/conversations/create', null, {
      params: { otherUserId, commodityId, orderId }
    })
  },
  
  /**
   * 标记对话为已读
   */
  markAsRead(conversationId) {
    return request.post(`/contact/conversations/${conversationId}/read`)
  },
  
  /**
   * 获取未读消息总数
   */
  getUnreadCount() {
    return request.get('/contact/unread-count')
  },
  
  /**
   * 删除对话
   */
  deleteConversation(conversationId) {
    return request.delete(`/contact/conversations/${conversationId}`)
  },
  
  /**
   * 删除消息
   */
  deleteMessage(messageId) {
    return request.delete(`/contact/messages/${messageId}`)
  },
  
  /**
   * 搜索消息
   */
  searchMessages(conversationId, keyword, page = 1, size = 20) {
    return request.get(`/contact/conversations/${conversationId}/search`, {
      params: { keyword, page, size }
    })
  },
  
  /**
   * 获取与特定用户的对话
   */
  getConversationWithUser(otherUserId) {
    return request.get(`/contact/conversations/with/${otherUserId}`)
  }
}

