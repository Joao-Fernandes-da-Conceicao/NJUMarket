import { defineStore } from 'pinia'
import { authAPI } from '../api'

export const useUserStore = defineStore('user', {
  state: () => ({
    user: null,
    token: null,
    isLoggedIn: false
  }),
  
  getters: {
    userInfo: (state) => state.user,
    isAuthenticated: (state) => state.isLoggedIn
  },
  
  actions: {
    // 初始化用户状态
    initUser() {
      const token = localStorage.getItem('token')
      const userStr = localStorage.getItem('user')
      
      console.log('初始化用户状态:', { 
        hasToken: !!token, 
        hasUserStr: !!userStr,
        tokenLength: token?.length,
        userStrLength: userStr?.length,
        userStrContent: userStr // 添加内容查看
      })
      
      if (token && userStr) {
        try {
          // 检查用户数据是否为有效字符串
          if (userStr === 'undefined' || userStr === 'null' || userStr.trim() === '') {
            console.warn('用户数据无效:', userStr)
            this.clearUserData()
            return
          }
          
          // 验证token是否过期
          if (this.isTokenExpired(token)) {
            console.warn('Token已过期，清除用户数据')
            this.clearUserData()
            return
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
          
          this.token = token
          this.user = userData
          this.isLoggedIn = true
          
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
      this.token = null
      this.isLoggedIn = false
      
      // 清理localStorage
      localStorage.removeItem('token')
      localStorage.removeItem('user')
      
      console.log('用户数据已清除')
    },
    // 登录
    async login(loginData) {
      const response = await authAPI.login(loginData)
      if (response.success) {
        this.token = response.data.token
        this.user = response.data.userInfo || response.data.user // 兼容两种数据结构
        this.isLoggedIn = true
        localStorage.setItem('token', this.token)
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
        
        return response
      } else {
        throw new Error(response.errorMsg)
      }
    },
    
    // 验证码登录
    async loginByCode(phone, code) {
      const response = await authAPI.loginByCode(phone, code)
      if (response.success) {
        this.token = response.data.token
        this.user = response.data.userInfo || response.data.user // 兼容两种数据结构
        this.isLoggedIn = true
        localStorage.setItem('token', this.token)
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
        
        return response
      } else {
        throw new Error(response.errorMsg)
      }
    },
    
    // 注册
    async register(registerData) {
      const response = await authAPI.register(registerData)
      if (response.success) {
        this.token = response.data.token
        this.user = response.data.userInfo || response.data.user // 兼容两种数据结构
        this.isLoggedIn = true
        localStorage.setItem('token', this.token)
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
      const userStr = localStorage.getItem('user')
      
      // 检查token
      if (token && (token === 'undefined' || token === 'null' || token.trim() === '')) {
        console.log('发现无效token，清理中...')
        localStorage.removeItem('token')
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
