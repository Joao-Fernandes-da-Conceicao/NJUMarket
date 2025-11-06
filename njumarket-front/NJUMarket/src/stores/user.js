import { defineStore } from 'pinia'
import { authAPI } from '../api'

export const useUserStore = defineStore('user', {
  state: () => ({
    user: null,
    accessToken: null,
    refreshToken: null,
    isLoggedIn: false
  }),
  
  getters: {
    userInfo: (state) => state.user,
    isAuthenticated: (state) => state.isLoggedIn,
    // 兼容旧代码：token指向accessToken
    token: (state) => state.accessToken
  },
  
  actions: {
    // 初始化用户状态
    initUser() {
      const accessToken = localStorage.getItem('accessToken') || localStorage.getItem('token') // 兼容旧版本
      const refreshToken = localStorage.getItem('refreshToken')
      const userStr = localStorage.getItem('user')
      
      console.log('初始化用户状态:', { 
        hasAccessToken: !!accessToken, 
        hasRefreshToken: !!refreshToken,
        hasUserStr: !!userStr,
        accessTokenLength: accessToken?.length,
        refreshTokenLength: refreshToken?.length,
        userStrLength: userStr?.length
      })
      
      if (accessToken && userStr) {
        try {
          // 检查用户数据是否为有效字符串
          if (userStr === 'undefined' || userStr === 'null' || userStr.trim() === '') {
            console.warn('用户数据无效:', userStr)
            this.clearUserData()
            return
          }
          
          // 验证accessToken是否过期
          if (this.isTokenExpired(accessToken)) {
            console.warn('AccessToken已过期')
            // 如果有refreshToken，尝试刷新（但不阻塞初始化）
            if (refreshToken && !this.isTokenExpired(refreshToken)) {
              console.log('检测到RefreshToken有效，将在首次请求时自动刷新')
              // 不在这里刷新，让响应拦截器处理
            } else {
              console.warn('RefreshToken也无效，清除用户数据')
              this.clearUserData()
              return
            }
          }
          
          // 解析用户数据
          const userData = JSON.parse(userStr)
          
          // 验证用户数据格式
          if (!userData || typeof userData !== 'object') {
            console.warn('用户数据格式无效')
            this.clearUserData()
            return
          }
          
          // 检查必要字段
          if (!userData.userId) {
            console.warn('用户数据缺少userId字段')
            this.clearUserData()
            return
          }
          
          this.accessToken = accessToken
          this.refreshToken = refreshToken
          this.user = userData
          this.isLoggedIn = true
          
          // 同步更新localStorage（如果旧版本只有token，迁移到accessToken）
          if (accessToken && !localStorage.getItem('accessToken')) {
            localStorage.setItem('accessToken', accessToken)
          }
          
          console.log('用户状态初始化成功:', {
            userId: userData.userId,
            nickname: userData.nickname,
            avatar: userData.avatar,
            hasProfile: !!(userData.nickname || userData.avatar)
          })
          
          // 如果已登录，初始化 WebSocket 连接（异步，不阻塞初始化）
          import('./message').then(({ useMessageStore }) => {
            const messageStore = useMessageStore()
            messageStore.initWebSocket()
          }).catch(err => {
            console.error('初始化 WebSocket 失败:', err)
          })
          
          // ✅ 初始化订单变化提醒的 WebSocket 监听
          import('./order').then(({ useOrderStore }) => {
            const orderStore = useOrderStore()
            orderStore.initWebSocketListeners()
            
            // ✅ v1.3.x: 从后端获取订单提醒状态（应用启动时扫描profile）
            orderStore.fetchOrderReminderStatus().catch(err => {
              console.warn('获取订单提醒状态失败（不影响应用启动）:', err)
            })
          }).catch(err => {
            console.error('初始化订单 WebSocket 监听失败:', err)
          })
          
        } catch (error) {
          console.error('解析用户信息失败:', error)
          console.error('原始用户数据:', userStr)
          this.clearUserData()
        }
      } else {
        console.log('没有找到token或用户数据')
      }
    },
    
    // 检查token是否过期
    isTokenExpired(token) {
      try {
        // 检查token格式
        if (!token || typeof token !== 'string') {
          console.warn('Token格式无效')
          return true
        }
        
        // 检查JWT token是否有3个部分
        const parts = token.split('.')
        if (parts.length !== 3) {
          console.warn('JWT token格式不正确')
          return true
        }
        
        // 解析JWT token payload
        const payload = parts[1]
        if (!payload) {
          console.warn('JWT token payload为空')
          return true
        }
        
        // Base64解码
        const decodedPayload = atob(payload)
        const payloadObj = JSON.parse(decodedPayload)
        
        // 检查是否有过期时间
        if (!payloadObj.exp) {
          console.warn('Token中没有过期时间')
          return true
        }
        
        const currentTime = Math.floor(Date.now() / 1000)
        const isExpired = payloadObj.exp < currentTime
        
        if (isExpired) {
          console.warn('Token已过期', {
            expiredAt: new Date(payloadObj.exp * 1000),
            currentTime: new Date(currentTime * 1000)
          })
        }
        
        return isExpired
      } catch (error) {
        console.error('Token解析失败:', error)
        return true // 解析失败认为已过期
      }
    },
    
    // 清除用户数据
    clearUserData() {
      console.log('清除用户数据')
      this.user = null
      this.accessToken = null
      this.refreshToken = null
      this.isLoggedIn = false
      
      // 清理localStorage
      localStorage.removeItem('token') // 兼容旧版本
      localStorage.removeItem('accessToken')
      localStorage.removeItem('refreshToken')
      localStorage.removeItem('user')
      
      console.log('用户数据已清除')
    },
    
    // 保存Token（AccessToken和RefreshToken）
    saveTokens(accessToken, refreshToken) {
      this.accessToken = accessToken
      this.refreshToken = refreshToken
      localStorage.setItem('accessToken', accessToken)
      if (refreshToken) {
        localStorage.setItem('refreshToken', refreshToken)
      }
      // 兼容旧版本：同时保存token
      localStorage.setItem('token', accessToken)
    },
    
    // 刷新Token
    async refreshAccessToken() {
      if (!this.refreshToken) {
        throw new Error('没有RefreshToken，无法刷新')
      }
      
      try {
        const response = await authAPI.refreshToken(this.refreshToken)
        if (response.success && response.data) {
          const { accessToken, refreshToken: newRefreshToken } = response.data
          this.saveTokens(accessToken, newRefreshToken)
          console.log('Token刷新成功')
          return true
        } else {
          throw new Error(response.errorMsg || 'Token刷新失败')
        }
      } catch (error) {
        console.error('Token刷新失败:', error)
        this.clearUserData()
        throw error
      }
    },
    // 登录
    async login(loginData) {
      const response = await authAPI.login(loginData)
      if (response.success) {
        // 保存AccessToken和RefreshToken
        const accessToken = response.data.accessToken || response.data.token // 兼容旧版本
        const refreshToken = response.data.refreshToken
        this.saveTokens(accessToken, refreshToken)
        
        // 保存用户信息
        this.user = response.data.userInfo || response.data.user // 兼容两种数据结构
        this.isLoggedIn = true
        localStorage.setItem('user', JSON.stringify(this.user))
        
        console.log('登录成功，用户数据:', this.user)
        
        // 初始化 WebSocket 连接
        const { useMessageStore } = await import('./message')
        const messageStore = useMessageStore()
        messageStore.initWebSocket()
        
        // ✅ 初始化订单变化提醒的 WebSocket 监听
        const { useOrderStore } = await import('./order')
        const orderStore = useOrderStore()
        orderStore.initWebSocketListeners()
        
        // ✅ v1.3.x: 从登录响应中初始化订单提醒状态（向后兼容）
        if (response.data.orderReminderStatus) {
          orderStore.setSellerOrderHasNew(response.data.orderReminderStatus.sellerOrderHasNew || false)
          orderStore.setBuyerOrderHasNew(response.data.orderReminderStatus.buyerOrderHasNew || false)
        }
        
        return response
      } else {
        // ✅ 优先使用errorMsg，如果为空则使用message
        const errorMsg = response.errorMsg || response.message || '登录失败'
        throw new Error(errorMsg)
      }
    },
    
    // 验证码登录
    async loginByCode(phone, code) {
      const response = await authAPI.loginByCode(phone, code)
      if (response.success) {
        // 保存AccessToken和RefreshToken
        const accessToken = response.data.accessToken || response.data.token // 兼容旧版本
        const refreshToken = response.data.refreshToken
        this.saveTokens(accessToken, refreshToken)
        
        // 保存用户信息
        this.user = response.data.userInfo || response.data.user // 兼容两种数据结构
        this.isLoggedIn = true
        localStorage.setItem('user', JSON.stringify(this.user))
        
        console.log('验证码登录成功，用户数据:', this.user)
        
        // 初始化 WebSocket 连接
        const { useMessageStore } = await import('./message')
        const messageStore = useMessageStore()
        messageStore.initWebSocket()
        
        // ✅ 初始化订单变化提醒的 WebSocket 监听
        const { useOrderStore } = await import('./order')
        const orderStore = useOrderStore()
        orderStore.initWebSocketListeners()
        
        // ✅ v1.3.x: 从登录响应中初始化订单提醒状态（向后兼容）
        if (response.data.orderReminderStatus) {
          orderStore.setSellerOrderHasNew(response.data.orderReminderStatus.sellerOrderHasNew || false)
          orderStore.setBuyerOrderHasNew(response.data.orderReminderStatus.buyerOrderHasNew || false)
        }
        
        return response
      } else {
        // ✅ 优先使用errorMsg，如果为空则使用message
        const errorMsg = response.errorMsg || response.message || '登录失败'
        throw new Error(errorMsg)
      }
    },
    
    // 注册
    async register(registerData) {
      const response = await authAPI.register(registerData)
      if (response.success) {
        // 保存AccessToken和RefreshToken
        const accessToken = response.data.accessToken || response.data.token // 兼容旧版本
        const refreshToken = response.data.refreshToken
        this.saveTokens(accessToken, refreshToken)
        
        // 保存用户信息
        this.user = response.data.userInfo || response.data.user // 兼容两种数据结构
        this.isLoggedIn = true
        localStorage.setItem('user', JSON.stringify(this.user))
        
        console.log('注册成功，用户数据:', this.user)
        
        // 初始化 WebSocket 连接
        const { useMessageStore } = await import('./message')
        const messageStore = useMessageStore()
        messageStore.initWebSocket()
        
        // ✅ 初始化订单变化提醒的 WebSocket 监听
        const { useOrderStore } = await import('./order')
        const orderStore = useOrderStore()
        orderStore.initWebSocketListeners()
        
        return response
      } else {
        throw new Error(response.errorMsg)
      }
    },
    
    // 登出
    async logout() {
      try {
        await authAPI.logout()
      } catch (error) {
        console.error('登出请求失败:', error)
      } finally {
        // 断开 WebSocket 连接
        const { useMessageStore } = await import('./message')
        const messageStore = useMessageStore()
        messageStore.disconnectWebSocket()
        
        // ✅ 清除订单变化提醒的 WebSocket 监听
        const { useOrderStore } = await import('./order')
        const orderStore = useOrderStore()
        orderStore.clearWebSocketListeners()
        
        this.clearUserData()
        
        // 跳转到首页
        if (typeof window !== 'undefined') {
          window.location.href = '/'
        }
      }
    },
    
    // 发送验证码
    async sendCode(phone) {
      return await authAPI.sendCode(phone)
    },
    
    // 重置密码
    async resetPassword(passwordData) {
      return await authAPI.resetPassword(passwordData)
    },
    
    // 更新用户信息
    updateUser(userData) {
      this.user = { ...this.user, ...userData }
      localStorage.setItem('user', JSON.stringify(this.user))
    },
    
    // 刷新用户信息（从服务器获取最新数据）
    async refreshUserInfo() {
      try {
        console.log('刷新用户信息...')
        const response = await authAPI.getCurrentUser()
        if (response.success) {
          this.user = response.data
          localStorage.setItem('user', JSON.stringify(this.user))
          console.log('用户信息刷新成功:', {
            userId: this.user.userId,
            nickname: this.user.nickname,
            avatar: this.user.avatar
          })
          return response
        } else {
          console.warn('刷新用户信息失败:', response.message)
          return response
        }
      } catch (error) {
        console.error('刷新用户信息异常:', error)
        throw error
      }
    },
    
    // 检查并修复用户数据
    async checkAndFixUserData() {
      // 只有在已登录状态下才检查用户数据
      if (!this.isLoggedIn || !this.user) {
        console.log('用户未登录，跳过用户数据检查')
        return
      }
      
      // 检查是否有profile数据
      const hasProfileData = !!(this.user.nickname || this.user.avatar)
      
      if (!hasProfileData) {
        console.log('用户数据缺少profile信息，尝试刷新...')
        try {
          await this.refreshUserInfo()
        } catch (error) {
          console.error('刷新用户数据失败:', error)
          // 如果刷新失败，可能是token过期，清除用户数据
          this.clearUserData()
        }
      }
    },
    
    // 手动清理localStorage中的无效数据
    cleanInvalidLocalStorage() {
      console.log('手动清理localStorage中的无效数据')
      
      const token = localStorage.getItem('token')
      const accessToken = localStorage.getItem('accessToken')
      const refreshToken = localStorage.getItem('refreshToken')
      const userStr = localStorage.getItem('user')
      
      // 检查token（兼容旧版本）
      if (token && (token === 'undefined' || token === 'null' || token.trim() === '')) {
        console.log('发现无效token，清理中...')
        localStorage.removeItem('token')
      }
      
      // 检查accessToken
      if (accessToken && (accessToken === 'undefined' || accessToken === 'null' || accessToken.trim() === '')) {
        console.log('发现无效accessToken，清理中...')
        localStorage.removeItem('accessToken')
      }
      
      // 检查refreshToken
      if (refreshToken && (refreshToken === 'undefined' || refreshToken === 'null' || refreshToken.trim() === '')) {
        console.log('发现无效refreshToken，清理中...')
        localStorage.removeItem('refreshToken')
      }
      
      // 检查用户数据
      if (userStr && (userStr === 'undefined' || userStr === 'null' || userStr.trim() === '')) {
        console.log('发现无效用户数据，清理中...')
        localStorage.removeItem('user')
      }
      
      console.log('localStorage清理完成')
    }
  }
})
