import axios from 'axios'

// 创建axios实例
const api = axios.create({
  baseURL: 'http://localhost:8080/api',
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json'
  }
})

// 刷新Token的Promise（防止并发刷新）
let refreshTokenPromise = null

// 请求拦截器
api.interceptors.request.use(
  config => {
    // 优先使用accessToken，兼容旧版本的token
    const accessToken = localStorage.getItem('accessToken') || localStorage.getItem('token')
    if (accessToken) {
      config.headers.Authorization = `Bearer ${accessToken}`
    }
    return config
  },
  error => {
    return Promise.reject(error)
  }
)

// 响应拦截器
api.interceptors.response.use(
  response => {
    // ✅ 如果响应中success=false，也显示errorMsg弹窗
    if (response.data && response.data.success === false) {
      const errorMsg = response.data.errorMsg || response.data.message || '操作失败'
      // 在浏览器环境中显示错误提示
      if (typeof window !== 'undefined') {
        import('element-plus').then(({ ElMessage }) => {
          ElMessage.error(errorMsg)
        })
      }
    }
    return response.data
  },
  async error => {
    const originalRequest = error.config
    
    // ✅ 处理401错误：尝试自动刷新Token
    if (error.response?.status === 401 && !originalRequest._retry) {
      // 排除刷新Token接口本身，避免无限循环
      if (originalRequest.url === '/user/auth/refresh-token') {
        // RefreshToken也失效了，清除数据并跳转登录
        if (typeof window !== 'undefined') {
          import('../stores/user').then(({ useUserStore }) => {
            const userStore = useUserStore()
            userStore.clearUserData()
          })
          
          import('element-plus').then(({ ElMessage }) => {
            ElMessage.error('登录已过期，请重新登录')
          })
        }
        return Promise.reject(error)
      }
      
      // 标记请求，防止重复刷新
      originalRequest._retry = true
      
      // 获取RefreshToken
      const refreshToken = localStorage.getItem('refreshToken')
      
      if (refreshToken && typeof window !== 'undefined') {
        try {
          // 如果已经有刷新请求在进行，等待它完成
          if (refreshTokenPromise) {
            await refreshTokenPromise
          } else {
            // 创建新的刷新请求
            refreshTokenPromise = (async () => {
              try {
                const { useUserStore } = await import('../stores/user')
                const userStore = useUserStore()
                await userStore.refreshAccessToken()
              } finally {
                refreshTokenPromise = null
              }
            })()
            await refreshTokenPromise
          }
          
          // 刷新成功，使用新的AccessToken重试原请求
          const newAccessToken = localStorage.getItem('accessToken')
          if (newAccessToken) {
            originalRequest.headers.Authorization = `Bearer ${newAccessToken}`
            return api(originalRequest)
          }
        } catch (refreshError) {
          // 刷新失败，清除数据
          console.error('Token自动刷新失败:', refreshError)
          if (typeof window !== 'undefined') {
            import('../stores/user').then(({ useUserStore }) => {
              const userStore = useUserStore()
              userStore.clearUserData()
            })
            
            import('element-plus').then(({ ElMessage }) => {
              ElMessage.error('登录已过期，请重新登录')
            })
          }
          return Promise.reject(refreshError)
        }
      } else {
        // 没有RefreshToken，清除数据
        if (typeof window !== 'undefined') {
          import('../stores/user').then(({ useUserStore }) => {
            const userStore = useUserStore()
            userStore.clearUserData()
          })
          
          import('element-plus').then(({ ElMessage }) => {
            const errorMsg = error.response?.data?.errorMsg || error.response?.data?.message || '用户未登录，请先登录'
            ElMessage.error(errorMsg)
          })
        }
      }
    } else if (error.response?.status === 403) {
      // ✅ 处理403禁止访问错误（账户被封禁/暂停）
      if (typeof window !== 'undefined') {
        // 动态导入ElMessage显示错误提示
        import('element-plus').then(({ ElMessage }) => {
          const errorMsg = error.response?.data?.errorMsg || error.response?.data?.message || '账户已被禁用，无法访问'
          ElMessage.error(errorMsg)
        })
        
        // 清除用户数据（账户被封禁，需要重新登录）
        import('../stores/user').then(({ useUserStore }) => {
          const userStore = useUserStore()
          userStore.clearUserData()
        })
      }
    } else if (error.response?.data) {
      // ✅ 处理其他HTTP错误（400, 500等），显示errorMsg
      if (typeof window !== 'undefined') {
        import('element-plus').then(({ ElMessage }) => {
          const errorMsg = error.response?.data?.errorMsg || error.response?.data?.message || '操作失败，请稍后重试'
          ElMessage.error(errorMsg)
        })
      }
    }
    return Promise.reject(error)
  }
)

// 用户认证相关API
export const authAPI = {
  // 登录
  login: (data) => api.post('/user/auth/login', data),
  
  // 注册
  register: (data) => api.post('/user/auth/register-new', data),
  
  // 发送验证码
  sendCode: (phone) => api.post('/user/auth/send-code', null, { params: { phone } }),
  
  // 验证码登录
  loginByCode: (phone, code) => api.post('/user/auth/login-by-code', null, { 
    params: { phone, code } 
  }),
  
  // 登出
  logout: () => api.post('/user/auth/logout'),
  
  // 重置密码
  resetPassword: (data) => api.post('/user/auth/reset-password', data),
  
  // 获取当前用户信息
  getCurrentUser: () => api.get('/user/auth/me'),
  
  // ✅ 刷新Token
  refreshToken: (refreshToken) => api.post('/user/auth/refresh-token', { refreshToken }),
  
  // 修改手机号
  updatePhone: (data) => api.post('/user/auth/update-phone', data)
}

// 商品相关API
export const commodityAPI = {
  // 搜索商品
  search: (params) => api.get('/public/commodity/search', { params }),
  
  // 获取商品详情
  getDetail: (id) => api.get(`/public/commodity/${id}`),
  
  // 获取热门商品
  getHot: (limit = 10) => api.get('/public/commodity/hot', { params: { limit } }),
  
  // 获取最新商品
  getLatest: (limit = 10) => api.get('/public/commodity/latest', { params: { limit } }),
  
  // 获取分类
  getCategories: () => api.get('/public/commodity/categories'),
  
  // 按分类获取商品
  getByCategory: (category, page = 1, size = 10) => 
    api.get(`/public/commodity/category/${category}`, { params: { page, size } }),
  
  // 记录浏览
  recordView: (id, sessionId) => 
    api.post(`/public/commodity/${id}/view`, null, { params: { sessionId } }),
  
  // 发布商品
  publish: (data) => api.post('/user/commodity/publish', data),
  
  // 创建草稿商品
  createDraft: (data) => api.post('/user/commodity/draft', data),
  
  // 上架商品
  shelf: (id) => api.post(`/user/commodity/${id}/shelf`),
  
  // 下架商品
  unshelf: (id) => api.post(`/user/commodity/${id}/unshelf`),
  
  // 重新上架
  republish: (id) => api.post(`/user/commodity/${id}/republish`),
  
  // 发布草稿商品
  publishDraft: (id) => api.post(`/user/commodity/${id}/publish`),
  
  // 获取我的商品
  getMy: (page = 1, size = 10, status) => 
    api.get('/user/commodity/my', { params: { page, size, status } }),
  
  // 获取指定卖家的商品（公开接口）
  getSellerCommodities: (sellerId, page = 1, size = 10, status = 'all') =>
    api.get(`/public/commodity/seller/${sellerId}`, { params: { page, size, status } }),
  
  // 获取我的商品详情（编辑用）
  getMyDetail: (id) => api.get(`/user/commodity/${id}`),
  
  // 更新商品
  update: (id, data) => api.put(`/user/commodity/${id}`, data),
  
  // 下架商品
  remove: (id) => api.post(`/user/commodity/${id}/remove`),
  
  // 重新上架
  // republish: (id) => api.post(`/user/commodity/${id}/republish`),
  
  // 删除商品
  delete: (id) => api.delete(`/user/commodity/${id}`),
  
  // 上传图片（通用）
  uploadImage: (file) => {
    const formData = new FormData()
    formData.append('file', file)
    return api.post('/user/commodity/upload-image', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
  },
  
  // 为指定商品上传图片
  uploadCommodityImage: (commodityId, file) => {
    const formData = new FormData()
    formData.append('file', file)
    return api.post(`/user/commodity/${commodityId}/upload-image`, formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
  },
  
  // 修改商品可见性（同时设置卖家和买家可见性）
  updateVisibility: (id, visibility) => 
    api.put(`/user/commodity/${id}/visibility`, null, { params: { visibility } }),
  
  // 修改商品卖家可见性
  updateSellerVisibility: (id, sellerVisibility) => 
    api.put(`/user/commodity/${id}/seller-visibility`, null, { params: { sellerVisibility } }),
  
  // 修改商品买家可见性
  updateBuyerVisibility: (id, buyerVisibility) => 
    api.put(`/user/commodity/${id}/buyer-visibility`, null, { params: { buyerVisibility } }),
  
  // ✅ 批量查询商品状态（用于聊天界面，轻量级查询）
  getBatchStatus: (commodityIds) => 
    api.post('/user/commodity/batch-status', commodityIds),
  
  // ✅ AI Agent 对话（超时时间 30 秒，因为 AI Agent 处理较慢）
  aiAgentChat: (message, conversationId) => 
    api.post('/user/ai-agent/chat', null, { 
      params: { message, conversationId },
      timeout: 60000 // 60 秒超时
    }),
  // ✅ AI Agent 流式对话（使用 SSE，支持认证）
  aiAgentChatStream: (message, conversationId, onToken, onComplete, onError) => {
    // 使用 fetch API 接收 SSE 流式数据（支持自定义请求头，包含认证信息）
    const params = new URLSearchParams({ message });
    if (conversationId) {
      params.append('conversationId', conversationId);
    }
    
    // 获取 baseURL 和 token
    const baseURL = api.defaults.baseURL || '';
    const accessToken = localStorage.getItem('accessToken') || localStorage.getItem('token');
    const url = `${baseURL}/user/ai-agent/chat-stream?${params.toString()}`;
    
    // 构建请求头，包含认证信息
    const headers = {
      'Accept': 'text/event-stream',
      'Cache-Control': 'no-cache'
    };
    if (accessToken) {
      headers['Authorization'] = `Bearer ${accessToken}`;
    }
    
    // 使用 fetch 接收 SSE 流
    let abortController = new AbortController();
    let isClosed = false;
    
    fetch(url, {
      method: 'GET',
      headers: headers,
      signal: abortController.signal
    })
    .then(response => {
      if (!response.ok) {
        throw new Error(`HTTP error! status: ${response.status}`);
      }
      
      const reader = response.body.getReader();
      const decoder = new TextDecoder();
      let buffer = '';
      
      // 解析 buffer 中的 SSE 事件，返回 true 表示已处理 complete/error 并结束
      const processSSEBuffer = (buf) => {
        const lines = buf.split('\n');
        let eventType = 'message';
        let eventData = '';
        for (const line of lines) {
          if (line.startsWith('event:')) {
            eventType = line.substring(6).trim();
          } else if (line.startsWith('data:')) {
            eventData = line.substring(5).trim();
          } else if (line === '') {
            if (eventData) {
              if (eventType === 'token') {
                if (onToken) onToken(eventData);
              } else if (eventType === 'complete') {
                try {
                  const data = JSON.parse(eventData);
                  if (onComplete) onComplete(data);
                  return true;
                } catch (e) {
                  console.error('解析完成事件失败:', e);
                  if (onError) onError('解析完成事件失败');
                  return true;
                }
              } else if (eventType === 'error') {
                try {
                  const data = JSON.parse(eventData);
                  if (onError) onError(data.error || '流式对话失败');
                } catch (e) {
                  if (onError) onError('流式对话失败');
                }
                return true;
              }
            }
            eventType = 'message';
            eventData = '';
          }
        }
        return false;
      };

      const readStream = () => {
        reader.read().then(({ done, value }) => {
          if (done) {
            // 流结束：先处理剩余 buffer（complete 可能随最后一块到达）
            if (buffer.trim()) {
              const ended = processSSEBuffer(buffer + '\n\n');
              if (ended) {
                isClosed = true;
                return;
              }
            }
            if (!isClosed) {
              isClosed = true;
              if (onError) {
                onError('连接已关闭');
              }
            }
            return;
          }
          
          if (value && value.length) {
            buffer += decoder.decode(value, { stream: true });
          }
          const lines = buffer.split('\n');
          buffer = lines.pop() || '';
          
          const ended = processSSEBuffer(lines.join('\n') + '\n');
          if (ended) {
            isClosed = true;
            return;
          }
          
          readStream();
        }).catch(error => {
          if (!isClosed && error.name !== 'AbortError') {
            console.error('读取流失败:', error);
            if (onError) {
              onError('连接失败，请稍后重试');
            }
            isClosed = true;
          }
        });
      };
      
      readStream();
    })
    .catch(error => {
      if (!isClosed && error.name !== 'AbortError') {
        console.error('SSE 连接失败:', error);
        if (onError) {
          onError('连接失败，请稍后重试');
        }
        isClosed = true;
      }
    });
    
    // 返回一个可以关闭的对象
    return {
      close: () => {
        if (!isClosed) {
          isClosed = true;
          abortController.abort();
        }
      }
    };
  },
  // ✅ 获取用户的所有AI聊天列表
  getAIChatList: (limit = 50) =>
    api.get('/user/ai-agent/chats', { 
      params: { limit },
      timeout: 10000
    }),
  // ✅ 获取指定chat的消息列表
  getAIChatMessages: (conversationId, limit = 100) =>
    api.get(`/user/ai-agent/chats/${conversationId}/messages`, { 
      params: { limit },
      timeout: 10000
    })
}

// 订单相关API
export const orderAPI = {
  // 创建订单
  create: (data) => api.post('/user/order/create', data),
  
  // 支付订单
  pay: (id) => api.post(`/user/order/${id}/pay`),
  
  // 确认收货
  confirm: (id) => api.post(`/user/order/${id}/confirm`),
  
  // 取消订单
  cancel: (id, reason) => api.post(`/user/order/${id}/cancel`, null, { params: { reason } }),
  
  // 发货
  ship: (id, trackingNumber) => 
    api.post(`/user/order/${id}/ship`, null, { params: { trackingNumber } }),
  
  // 获取买家订单
  getBuyerOrders: (page = 1, size = 10, status) => 
    api.get('/user/order/buyer', { params: { page, size, status } }),
  
  // 获取卖家订单
  getSellerOrders: (page = 1, size = 10, status) => 
    api.get('/user/order/seller', { params: { page, size, status } }),
  
  // 获取订单详情
  getDetail: (id) => api.get(`/user/order/${id}`),
  
  // 申请退款
  requestRefund: (id, reason) => 
    api.post(`/user/order/${id}/refund`, null, { params: { reason } }),
  
  // 处理退款
  handleRefund: (id, decision, remark) => 
    api.post(`/user/order/${id}/refund/handle`, null, { 
      params: { decision, remark } 
    }),
  
  // 申请退货
  requestReturn: (id, returnReason) => 
    api.post(`/user/order/${id}/return`, null, { params: { returnReason } }),
  
  // 审批退货
  approveReturn: (id, approved, rejectionReason) => 
    api.put(`/user/order/${id}/return/approve`, null, { 
      params: { approved, rejectionReason } 
    }),
  
  // 确认退货发货
  confirmReturnShipment: (id, returnTrackingNumber) => 
    api.put(`/user/order/${id}/return/shipment`, null, { 
      params: { returnTrackingNumber } 
    }),
  
  // 完成退货
  completeReturn: (id) => api.put(`/user/order/${id}/return/complete`),
  
  // 获取退货申请列表
  getReturnRequests: (page = 1, size = 10, status) => 
    api.get('/user/order/returns', { params: { page, size, status } }),
  
  // 获取我的退货记录
  getMyReturns: (page = 1, size = 10, status) => 
    api.get('/user/order/my-returns', { params: { page, size, status } }),
  
  // 修改订单可见性（同时设置卖家和买家可见性）
  updateVisibility: (id, visibility) => 
    api.put(`/user/order/${id}/visibility`, null, { params: { visibility } }),
  
  // 修改订单卖家可见性
  updateSellerVisibility: (id, sellerVisibility) => 
    api.put(`/user/order/${id}/seller-visibility`, null, { params: { sellerVisibility } }),
  
  // 修改订单买家可见性
  updateBuyerVisibility: (id, buyerVisibility) => 
    api.put(`/user/order/${id}/buyer-visibility`, null, { params: { buyerVisibility } }),
  
  // 查询原商品信息（基于商品快照）
  queryOriginalCommodity: (orderId) => api.get(`/user/order/${orderId}/query-commodity`),
  
  // 基于快照创建新订单
  createOrderFromSnapshot: (orderId, orderData) => api.post(`/user/order/${orderId}/create-from-snapshot`, orderData),
  
  // ✅ 批量查询订单状态（用于聊天界面，轻量级查询）
  getBatchStatus: (orderIds) => 
    api.post('/user/order/batch-status', orderIds),
  
  // 更新订单收货地址（买家或卖家都可以更新）
  updateShippingAddress: (orderId, data) => 
    api.put(`/user/order/${orderId}/shipping-address`, data)
}

// 用户资料相关API
export const profileAPI = {
  // 获取我的资料
  getMe: () => api.get('/user/profile/me'),
  
  // 获取用户资料
  getUser: (id) => api.get(`/user/profile/${id}`),
  
  // 更新资料（使用新的UserProfileUpdateDTO）
  update: (data) => api.put('/user/profile/me', data),
  
  // 上传头像到 Image 服务（第一步），返回 imageUrl
  uploadAvatar: (file) => {
    const formData = new FormData()
    formData.append('file', file)
    return api.post('/user/image/upload-avatar', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
  },

  // 将头像 URL 写入用户档案（第二步）
  setAvatarUrl: (imageUrl) =>
    api.post('/user/profile/avatar', null, { params: { imageUrl } }),
  
  // 搜索用户
  search: (keyword, page = 1, size = 10) => 
    api.get('/user/profile/search', { params: { keyword, page, size } }),
  
  // 获取排行榜
  getRankings: (type, page = 1, size = 10) => 
    api.get('/user/profile/rankings', { params: { type, page, size } }),
  
  // ✅ v1.3.x: 清除订单提醒状态
  clearOrderReminder: (role) => 
    api.post('/user/profile/order-reminder/clear', null, { params: { role } }),
  
  // ✅ v1.3.x: 获取订单提醒状态
  getOrderReminderStatus: () => 
    api.get('/user/profile/order-reminder/status')
}

// 图片相关API
export const imageAPI = {
  // 获取头像图片
  getAvatar: (fileName) => `http://localhost:8080/api/images/avatars/${fileName}`,
  
  // 获取商品图片
  getCommodityImage: (fileName) => `http://localhost:8080/api/images/commodities/${fileName}`,
  
  // 获取默认头像
  getDefaultAvatar: () => `http://localhost:8080/api/images/avatars/default`,
  
  // 获取默认商品图片
  getDefaultCommodityImage: () => '/default-commodity.jpg',
  
  // 上传商品图片（通用）
  upload: (file) => {
    const formData = new FormData()
    formData.append('file', file)
    return api.post('/user/commodity/upload-image', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
  },
  
  // 为指定商品上传图片
  uploadForCommodity: (commodityId, file) => {
    const formData = new FormData()
    formData.append('file', file)
    return api.post(`/user/commodity/${commodityId}/upload-image`, formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
  }
}

// 导入并导出contactAPI
export { contactAPI } from './contact'

// ✅ 聊天数据相关API（增量查询）
export const chatAPI = {
  // 增量查询商品和订单变更
  getIncrementalUpdate: (lastPollTimestamp) =>
    api.get('/user/chat/incremental-update', { 
      params: { lastPollTimestamp } 
    })
}

// 地址相关API
export const addressAPI = {
  // 创建地址
  create: (data) => api.post('/auth/addresses', data),
  
  // 更新地址
  update: (addressId, data) => api.put(`/auth/addresses/${addressId}`, data),
  
  // 删除地址
  delete: (addressId) => api.delete(`/auth/addresses/${addressId}`),
  
  // 获取地址详情
  getById: (addressId) => api.get(`/auth/addresses/${addressId}`),
  
  // 获取用户的所有地址（userId可选，不传则获取当前登录用户的地址）
  getUserAddresses: (userId) => {
    const params = userId ? { userId } : {}
    return api.get('/auth/addresses', { params })
  },
  
  // 获取用户的默认地址（userId可选，不传则获取当前登录用户的默认地址）
  getDefaultAddress: (userId) => {
    const params = userId ? { userId } : {}
    return api.get('/auth/addresses/default', { params })
  },
  
  // 设置默认地址
  setDefault: (addressId) => api.put(`/auth/addresses/${addressId}/default`),
  
  // 启用/禁用地址
  setActive: (addressId, isActive) => 
    api.put(`/auth/addresses/${addressId}/active`, null, { params: { isActive } })
}

export default api
