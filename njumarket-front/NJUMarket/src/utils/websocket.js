/**
 * WebSocket 客户端工具类
 * 用于建立和维护 WebSocket 连接，接收实时消息推送
 * 使用 SockJS 和 STOMP 协议
 */

// 引入 SockJS 和 STOMP（需要确保已安装：npm install sockjs-client @stomp/stompjs）
import SockJS from 'sockjs-client'
import { Client as StompClient } from '@stomp/stompjs'

const Stomp = StompClient

class WebSocketClient {
  constructor() {
    this.sock = null
    this.stompClient = null
    this.reconnectAttempts = 0
    this.maxReconnectAttempts = Infinity // 生产环境：无限重连，但限制频率
    this.reconnectDelay = 1000 // 初始延迟1秒
    this.maxReconnectDelay = 60000 // 最大延迟60秒
    this.reconnectTimer = null
    this.messageHandlers = new Map() // 消息类型 -> 处理函数
    this.isConnecting = false
    this.isConnected = false
    this.subscriptions = new Map() // 存储订阅
    this.lastHeartbeatTime = null // 最后心跳时间
    this.heartbeatTimeout = 30000 // 30秒无心跳则认为断开
    this.heartbeatMonitorInterval = null // 心跳监控定时器
    this.heartbeatKeepAliveInterval = null // 心跳保活定时器（备用机制）
    this.isManuallyDisconnected = false // 是否手动断开（手动断开不自动重连）
    
    // 初始化页面可见性和网络状态监听
    this.setupVisibilityHandler()
    this.setupNetworkHandler()
  }
  
  /**
   * 建立 WebSocket 连接
   */
  connect() {
    if (this.isConnecting || this.isConnected) {
      // Connection already exists or is connecting
      return
    }
    
    if (!SockJS || !Stomp) {
      console.error('SockJS or STOMP not loaded, cannot establish connection')
      return
    }
    
    // 优先使用accessToken，兼容旧版本的token
    const accessToken = localStorage.getItem('accessToken') || localStorage.getItem('token')
    if (!accessToken) {
      // Not logged in, cannot establish WebSocket connection
      return
    }
    
    // 确定 WebSocket URL（使用 SockJS）
    const protocol = window.location.protocol === 'https:' ? 'https:' : 'http:'
    const host = process.env.NODE_ENV === 'production' 
      ? window.location.host  // 生产环境：通过 Nginx 代理
      : 'localhost:8080'      // 开发环境：直接连接后端
    
    // Spring WebSocket 使用 /api/ws 端点，SockJS 会自动处理协议升级
    const wsUrl = `${protocol}//${host}/api/ws?token=${accessToken}`
    
    this.isConnecting = true
    
    try {
      // 创建 SockJS 连接
      this.sock = new SockJS(wsUrl)
      
      // 创建 STOMP 客户端
      // 注意：禁用 STOMP 的自动重连，由我们自己的逻辑控制
      this.stompClient = new Stomp({
        webSocketFactory: () => this.sock,
        reconnectDelay: 0, // 禁用 STOMP 的自动重连，使用我们自己的重连机制
        heartbeatIncoming: 10000, // 期望10秒接收一次心跳
        heartbeatOutgoing: 10000, // 每10秒发送一次心跳
        onStompError: (frame) => {
          console.error('STOMP error:', frame)
          this.onStompError(frame)
        },
        // ✅ 使用debug回调来监听所有STOMP帧，尝试捕获心跳帧
        // 注意：STOMP心跳帧可能是二进制帧，不一定会在debug回调中出现
        // 但我们可以通过检查连接状态和定期更新来确保心跳机制正常工作
        debug: (str) => {
          if (!str) return
          
          const trimmed = str.trim().toLowerCase()
          // 尝试识别心跳帧：
          // 1. 心跳帧可能是空字符串或只有换行符
          // 2. 心跳帧可能不包含明显的STOMP命令（如CONNECTED, MESSAGE等）
          // 3. 心跳帧通常很短（小于20字符）
          if (trimmed === '' || trimmed === '\n' || 
              (trimmed.length < 20 && !trimmed.includes('connected') && 
               !trimmed.includes('message') && !trimmed.includes('error') &&
               !trimmed.includes('subscribe') && !trimmed.includes('unsubscribe'))) {
            // 可能是心跳帧，更新心跳时间
            const previousTime = this.lastHeartbeatTime
            this.lastHeartbeatTime = Date.now()
            
            // ✅ 记录心跳确认日志（每10秒一次）
            if (previousTime) {
              const timeSinceLastHeartbeat = this.lastHeartbeatTime - previousTime
              // 只记录接近心跳间隔的更新（8-15秒之间，避免其他空帧误判）
              if (timeSinceLastHeartbeat >= 8000 && timeSinceLastHeartbeat <= 15000) {
                console.log('💓 WebSocket心跳确认', {
                  timestamp: new Date(this.lastHeartbeatTime).toISOString(),
                  timeSinceLastHeartbeat: `${Math.round(timeSinceLastHeartbeat / 1000)}秒`,
                  connectionHealthy: true
                })
              }
            }
          }
          // 其他debug信息不输出，避免日志过多
        }
      })
      
      // ✅ 监听 STOMP 连接关闭事件
      this.stompClient.onWebSocketClose = (event) => {
        this.handleConnectionClose(event)
      }
      
      this.stompClient.onDisconnect = () => {
        this.handleConnectionClose({ wasClean: true, code: 1000 })
      }
      
      // 连接成功回调
      this.stompClient.onConnect = () => {
        const wasReconnecting = this.reconnectAttempts > 0
        
        this.isConnecting = false
        this.isConnected = true
        this.reconnectAttempts = 0 // 重置重连计数
        this.lastHeartbeatTime = Date.now() // 更新心跳时间
        
        // ✅ 连接成功时记录日志
        console.log('💚 WebSocket连接已建立，心跳机制已启动', {
          timestamp: new Date(this.lastHeartbeatTime).toISOString(),
          heartbeatInterval: '10秒',
          heartbeatTimeout: '60秒',
          connectionStatus: 'connected'
        })
        
        // 清除重连定时器
        if (this.reconnectTimer) {
          clearTimeout(this.reconnectTimer)
          this.reconnectTimer = null
        }
        
        // 订阅消息队列
        this.subscribeToMessages()
        
        // 启动心跳监控
        this.setupHeartbeatMonitor()
        
        // ✅ 启动心跳保活机制（备用机制）
        // 即使debug回调未捕获到心跳帧，只要STOMP连接正常，
        // 定期更新时间戳，确保不会因为聊天记录未更新而误判连接失效
        this.startHeartbeatKeepAlive()
        
        // 如果是重连成功，执行重连后处理
        if (wasReconnecting) {
          // 异步执行，不阻塞连接流程
          this.onReconnectSuccess().catch(err => {
            console.error('Reconnection post-processing failed:', err)
          })
        }
        
        // 触发连接成功事件
        this.emit('connected')
      }
      
      // STOMP 连接错误回调
      this.stompClient.onStompError = (frame) => {
        console.error('WebSocket STOMP error:', frame.headers['message'], frame.body)
        this.isConnected = false
        this.emit('error', frame)
        this.scheduleReconnect()
      }
      
      // STOMP WebSocket 关闭回调（STOMP 层面的断开）
      this.stompClient.onWebSocketClose = (event) => {
        // 如果底层 sock.onclose 没有触发，这里作为备用处理
        this.handleConnectionClose(event)
      }
      
      // STOMP 断开连接回调
      this.stompClient.onDisconnect = () => {
        // 如果底层事件没有触发，这里作为备用处理
        this.handleConnectionClose({ wasClean: false, code: 1006 })
      }
      
      // WebSocket 错误回调（底层 SockJS 错误）
      this.sock.onerror = (error) => {
        console.error('WebSocket connection error:', error)
        this.isConnected = false
        this.emit('error', error)
        this.scheduleReconnect()
      }
      
      // 连接关闭回调（底层 SockJS 关闭）
      this.sock.onclose = (event) => {
        this.handleConnectionClose(event)
      }
      
      // 激活 STOMP 客户端
      this.stompClient.activate()
      
    } catch (error) {
      console.error('WebSocket connection failed:', error)
      this.isConnecting = false
      this.scheduleReconnect()
    }
  }
  
  /**
   * 订阅消息队列
   */
  subscribeToMessages() {
    if (!this.stompClient || !this.stompClient.connected) {
      console.warn('Cannot subscribe: STOMP client not connected')
      return
    }
    
    // 订阅用户专属队列：/user/queue/message
    // Spring WebSocket 会自动将 /user/{userId}/queue/message 映射到当前用户
    const destination = '/user/queue/message'
    
    const subscription = this.stompClient.subscribe(destination, (message) => {
      // ✅ 更新心跳时间（收到任何订阅消息都表示连接正常）
      // 注意：STOMP 心跳帧不会触发订阅回调，但业务消息会触发
      // 心跳帧通过debug回调检测和更新
      const previousTime = this.lastHeartbeatTime
      this.lastHeartbeatTime = Date.now()
      
      // ✅ 记录业务消息到达日志（表示连接活跃）
      console.log('📨 [消息接收] 收到WebSocket消息:', {
        timestamp: new Date(this.lastHeartbeatTime).toISOString(),
        destination: destination,
        bodyLength: message.body?.length || 0,
        bodyPreview: message.body?.substring(0, 200) || 'empty'
      })
      
      if (previousTime) {
        const timeSinceLastMessage = this.lastHeartbeatTime - previousTime
        console.log('📨 收到业务消息，连接保持活跃', {
          timestamp: new Date(this.lastHeartbeatTime).toISOString(),
          timeSinceLastMessage: `${Math.round(timeSinceLastMessage / 1000)}秒`,
          connectionHealthy: true
        })
      }
      
      let messageData = null
      let messageId = null
      let messageType = null
      
      try {
        messageData = JSON.parse(message.body)
        messageId = messageData.messageId || messageData.id
        messageType = messageData.type || 'MESSAGE_NEW'
        
        console.log('📨 [消息解析] 消息解析成功:', {
          messageId: messageId,
          messageType: messageType,
          hasMessageData: !!messageData
        })
      } catch (error) {
        console.error('❌ [消息解析失败] Failed to parse message:', error, message.body)
        // ✅ 即使解析失败，也尝试从原始body中提取messageId
        // 尝试使用正则表达式提取messageId（如果body是JSON字符串）
        try {
          const messageIdMatch = message.body.match(/"messageId"\s*:\s*"([^"]+)"/)
          const typeMatch = message.body.match(/"type"\s*:\s*"([^"]+)"/)
          if (messageIdMatch) {
            messageId = messageIdMatch[1]
          }
          if (typeMatch) {
            messageType = typeMatch[1]
          }
          console.log('📨 [消息解析] 通过正则提取:', {
            messageId: messageId,
            messageType: messageType
          })
        } catch (e) {
          // 无法提取，继续处理
          console.error('❌ [消息解析] 正则提取也失败:', e)
        }
      }
      
      // ✅ 关键修复：在收到消息的第一时间发送ACK，而不是在处理完成后
      // 这样即使后续处理失败，ACK也已经发送，确保后端知道消息已送达
      // 只有收到消息才发送ACK，这是真正的"接收端确认"
      if (messageId && messageType) {
        // 立即发送ACK，不等待业务处理完成
        console.log('📤 [ACK准备] 准备发送ACK:', {
          messageId: messageId,
          messageType: messageType,
          timestamp: new Date().toISOString()
        })
        this.sendAck(messageId, messageType)
      } else {
        console.warn('⚠️ [ACK跳过] 收到消息但缺少messageId或type，无法发送ACK:', {
          hasMessageId: !!messageId,
          hasMessageType: !!messageType,
          messageId: messageId,
          messageType: messageType,
          messageBody: message.body?.substring(0, 200) // 只显示前200字符
        })
      }
      
      // 然后进行业务处理（即使处理失败，ACK也已经发送）
      if (messageData) {
        this.handleMessage(messageData)
      } else {
        console.error('❌ [业务处理] 消息解析失败，无法进行业务处理:', message.body)
      }
    })
    
    // 订阅连接确认队列（可选，用于验证连接）
    const connectionDestination = '/user/queue/connection'
    const connectionSubscription = this.stompClient.subscribe(connectionDestination, (message) => {
      // ✅ 更新心跳时间（收到连接确认也表示连接正常）
      this.lastHeartbeatTime = Date.now()
      try {
        const data = JSON.parse(message.body)
        if (data.type === 'CONNECTION_CONFIRMED') {
          // Connection confirmed
        }
      } catch (error) {
        console.error('Failed to parse connection confirmation:', error)
      }
    })
    this.subscriptions.set(connectionDestination, connectionSubscription)
    
    // ✅ 关键修复：STOMP 客户端内部会处理心跳帧，但不会触发订阅回调
    // 我们需要通过监听底层 WebSocket 的原始消息来捕获心跳帧
    // 但由于 STOMP 会接管 WebSocket，我们需要在心跳监控中增加容错机制
    // 如果最近收到过任何消息（订阅消息或连接确认），则认为连接正常
    
    this.subscriptions.set(destination, subscription)
  }
  
  
  /**
   * 处理接收到的消息
   * 支持多种消息类型：MESSAGE_NEW、UNREAD_COUNT_UPDATE 等
   */
  handleMessage(messageData) {
    // 检查消息类型
    const messageType = messageData.type || 'MESSAGE_NEW'
    
    // 获取对应类型的处理函数
    const handler = this.messageHandlers.get(messageType)
    if (handler) {
      try {
        handler(messageData)
      } catch (error) {
        console.error(`Failed to handle ${messageType}:`, error)
      }
    } else {
      console.warn(`No handler registered for ${messageType}:`, messageData)
    }
  }
  
  /**
   * 注册消息处理函数
   * @param {string} messageType 消息类型（如 'MESSAGE_NEW'）
   * @param {Function} handler 处理函数
   */
  on(messageType, handler) {
    if (typeof handler !== 'function') {
      console.error('消息处理函数必须是函数类型')
      return
    }
    this.messageHandlers.set(messageType, handler)
  }
  
  /**
   * 移除消息处理函数
   */
  off(messageType) {
    this.messageHandlers.delete(messageType)
  }
  
  /**
   * 触发事件（用于连接状态变化）
   */
  emit(event, data) {
    const handler = this.messageHandlers.get(`__${event}__`)
    if (handler) {
      handler(data)
    }
  }
  
  /**
   * 监听连接状态变化
   */
  onConnect(handler) {
    this.messageHandlers.set('__connected__', handler)
  }
  
  onDisconnect(handler) {
    this.messageHandlers.set('__disconnected__', handler)
  }
  
  onError(handler) {
    this.messageHandlers.set('__error__', handler)
  }
  
  /**
   * 监听重连成功事件
   */
  onReconnected(handler) {
    this.messageHandlers.set('__reconnected__', handler)
  }
  
  /**
   * 统一的连接关闭处理（避免重复代码，并防止重复处理）
   * @param {CloseEvent|Object} event - 关闭事件对象，包含 wasClean 和 code 属性
   */
  handleConnectionClose(event) {
    // 防止重复处理
    if (!this.isConnected && !this.isConnecting) {
      return
    }
    
    this.isConnecting = false
    this.isConnected = false
    
    // 停止心跳监控和保活机制
    this.stopHeartbeatMonitor()
    this.stopHeartbeatKeepAlive()
    
    this.emit('disconnected')
    
    // 正常关闭时不重连（wasClean=true 且 code=1000）
    if (!this.isManuallyDisconnected) {
      if (!event?.wasClean || event?.code !== 1000) {
        this.scheduleReconnect()
      }
    } else {
      // 手动断开，重置标志
      this.isManuallyDisconnected = false
    }
  }
  
  /**
   * 判断是否应该重连
   */
  shouldReconnect() {
    // 手动断开时，不重连
    if (this.isManuallyDisconnected) {
      return false
    }
    
    // 页面不可见时，暂停重连（节省资源）
    if (typeof document !== 'undefined' && document.hidden) {
      return false
    }
    
    // 网络离线时，不重连（等待网络恢复）
    if (typeof navigator !== 'undefined' && !navigator.onLine) {
      return false
    }
    
    return true
  }
  
  /**
   * 页面可见性变化处理
   */
  setupVisibilityHandler() {
    if (typeof document === 'undefined') return
    
    document.addEventListener('visibilitychange', () => {
      if (!document.hidden && !this.isConnectedState()) {
        // 页面从隐藏变为可见，且连接断开，立即重连
        this.reconnectAttempts = 0 // 重置重连计数
        if (this.shouldReconnect()) {
          this.connect()
        }
      }
    })
  }
  
  /**
   * 网络状态变化处理
   */
  setupNetworkHandler() {
    if (typeof window === 'undefined') return
    
    window.addEventListener('online', () => {
      if (!this.isConnectedState() && this.shouldReconnect()) {
        this.reconnectAttempts = 0 // 重置重连计数
        this.connect()
      }
    })
    
    window.addEventListener('offline', () => {
      // Network offline, will reconnect when online
    })
  }
  
  /**
   * 心跳超时检测
   */
  setupHeartbeatMonitor() {
    // 清除旧的监控
    this.stopHeartbeatMonitor()
    
    // 设置新的监控（每5秒检查一次）
    this.heartbeatMonitorInterval = setInterval(() => {
      if (this.isConnectedState()) {
        // ✅ 如果页面隐藏（切换到其他标签页、最小化窗口等），跳过心跳检测
        // 注意：打开对话框不会使页面隐藏（document.hidden 仍为 false），
        // 对话框打开时页面仍在消息页面，应该继续检测心跳
        if (typeof document !== 'undefined' && document.hidden) {
          return // 页面隐藏时不检测心跳，避免误判
        }
        
        const now = Date.now()
        
        // ✅ 完善的心跳检测逻辑：
        // 1. STOMP 客户端每10秒会自动发送和接收心跳帧（heartbeatIncoming/Outgoing: 10000ms）
        // 2. 心跳帧会在debug回调中被检测并更新 lastHeartbeatTime（通过识别空帧或特殊格式）
        // 3. 业务消息（订阅消息、连接确认）也会更新 lastHeartbeatTime
        // 4. 只要 lastHeartbeatTime 在有效范围内持续更新，就认为连接正常
        //
        // 关键改进：
        // - 即使聊天记录未更新（没有业务消息），只要STOMP心跳帧正常，
        //   lastHeartbeatTime 会通过debug回调每10秒更新一次，不会误判连接失效
        // - 超时时间设置为60秒（6个心跳周期），确保有足够的容错空间
        // - 这样即使偶尔丢失1-2个心跳帧，也不会误判为连接失效
        const effectiveTimeout = 60000 // 60秒，足够容纳6个心跳周期（每10秒一次）
        
        if (this.lastHeartbeatTime && (now - this.lastHeartbeatTime > effectiveTimeout)) {
          console.warn('⚠️ WebSocket心跳超时，开始重连', {
            lastHeartbeatTime: new Date(this.lastHeartbeatTime).toISOString(),
            timeout: `${effectiveTimeout / 1000}秒`,
            elapsed: `${Math.round((now - this.lastHeartbeatTime) / 1000)}秒`,
            reason: '超过60秒未收到心跳帧或业务消息'
          })
          this.disconnect()
          this.isManuallyDisconnected = false // 允许自动重连
          this.scheduleReconnect()
        }
      }
    }, 5000) // 每5秒检查一次
  }
  
  /**
   * 停止心跳监控
   */
  stopHeartbeatMonitor() {
    if (this.heartbeatMonitorInterval) {
      clearInterval(this.heartbeatMonitorInterval)
      this.heartbeatMonitorInterval = null
    }
  }
  
  /**
   * 启动心跳保活机制（备用机制）
   * 即使debug回调未捕获到心跳帧，只要STOMP连接正常，
   * 定期更新时间戳，确保不会因为聊天记录未更新而误判连接失效
   */
  startHeartbeatKeepAlive() {
    // 清除旧的保活定时器
    this.stopHeartbeatKeepAlive()
    
    // 每12秒检查一次连接状态，如果连接正常则更新时间戳
    // 12秒略大于心跳间隔（10秒），确保不会干扰正常心跳检测
    this.heartbeatKeepAliveInterval = setInterval(() => {
      if (this.isConnectedState() && this.stompClient && this.stompClient.connected) {
        // 连接正常，更新心跳时间（作为备用机制）
        const previousTime = this.lastHeartbeatTime
        this.lastHeartbeatTime = Date.now()
        
        // ✅ 记录心跳保活日志（每12秒一次，表示连接正常）
        if (previousTime) {
          const timeSinceLastUpdate = this.lastHeartbeatTime - previousTime
          // 只在接近12秒时记录（避免与其他更新重复）
          if (timeSinceLastUpdate >= 11000 && timeSinceLastUpdate <= 13000) {
            console.log('💚 WebSocket连接保活确认', {
              timestamp: new Date(this.lastHeartbeatTime).toISOString(),
              timeSinceLastUpdate: `${Math.round(timeSinceLastUpdate / 1000)}秒`,
              connectionStatus: 'active',
              note: '连接正常，即使没有业务消息也会保持活跃'
            })
          }
        }
      }
    }, 12000) // 每12秒检查一次
  }
  
  /**
   * 停止心跳保活机制
   */
  stopHeartbeatKeepAlive() {
    if (this.heartbeatKeepAliveInterval) {
      clearInterval(this.heartbeatKeepAliveInterval)
      this.heartbeatKeepAliveInterval = null
    }
  }
  
  /**
   * 重连成功后处理
   */
  async onReconnectSuccess() {
    // 重置重连计数
    this.reconnectAttempts = 0
    
    // 拉取断线期间错过的消息
    await this.fetchMissedMessages()
    
    // 触发重连成功事件
    this.emit('reconnected')
  }
  
  /**
   * 获取断线期间的消息
   */
  async fetchMissedMessages() {
    try {
      // 动态导入避免循环依赖
      const messageStoreModule = await import('../stores/message')
      const { useMessageStore } = messageStoreModule
      const messageStore = useMessageStore()
      
      // 重新获取对话列表和未读数
      await messageStore.fetchConversations()
      
      // 如果当前有打开的对话，重新加载消息
      if (messageStore.selectedConversationId) {
        await messageStore.fetchMessages(messageStore.selectedConversationId)
      }
    } catch (error) {
      console.error('Failed to fetch missed messages:', error)
    }
  }
  
  /**
   * 安排重连（智能重连策略）
   */
  scheduleReconnect() {
    // 检查是否应该重连
    if (!this.shouldReconnect()) {
      return
    }
    
    // 检查是否超过最大重连次数
    if (this.reconnectAttempts >= this.maxReconnectAttempts) {
      console.warn('Maximum reconnection attempts reached, stopping reconnection')
      return
    }
    
    // 清除之前的定时器
    if (this.reconnectTimer) {
      clearTimeout(this.reconnectTimer)
      this.reconnectTimer = null
    }
    
    // 指数退避计算延迟：1s, 2s, 4s, 8s, 16s, 32s, 60s（最大）
    const delay = Math.min(
      this.reconnectDelay * Math.pow(2, this.reconnectAttempts),
      this.maxReconnectDelay
    )
    
    this.reconnectAttempts++
    
    this.reconnectTimer = setTimeout(() => {
      if (!this.isConnectedState()) {
      this.connect()
      }
    }, delay)
  }
  
  /**
   * 断开连接
   */
  disconnect() {
    this.isManuallyDisconnected = true
    
    // 清除重连定时器
    if (this.reconnectTimer) {
      clearTimeout(this.reconnectTimer)
      this.reconnectTimer = null
    }
    
    // 停止心跳监控和保活机制
    this.stopHeartbeatMonitor()
    this.stopHeartbeatKeepAlive()
    
    // 取消所有订阅
    this.subscriptions.forEach((subscription) => {
      subscription.unsubscribe()
    })
    this.subscriptions.clear()
    
    // 断开 STOMP 连接
    if (this.stompClient) {
      this.stompClient.deactivate()
      this.stompClient = null
    }
    
    // 关闭 SockJS 连接
    if (this.sock) {
      this.sock.close()
      this.sock = null
    }
    
    this.isConnecting = false
    this.isConnected = false
  }
  
  /**
   * 获取连接状态
   */
  getState() {
    if (!this.stompClient) {
      return 'CLOSED'
    }
    
    if (this.isConnecting) {
      return 'CONNECTING'
    }
    
    if (this.isConnected && this.stompClient.connected) {
      return 'OPEN'
    }
    
    return 'CLOSED'
  }
  
  /**
   * 检查是否已连接
   */
  isConnectedState() {
    return this.isConnected && this.stompClient && this.stompClient.connected
  }
  
  /**
   * 发送ACK确认消息
   * 前端收到消息后，通过此方法发送ACK确认，告知后端消息已成功接收
   * 
   * @param {string} messageId - 消息ID
   * @param {string} messageType - 消息类型（MESSAGE_NEW, ORDER_CHANGE, UNREAD_COUNT_UPDATE等）
   */
  sendAck(messageId, messageType) {
    if (!this.isConnectedState()) {
      console.warn('❌ WebSocket未连接，无法发送ACK: messageId=', messageId, 'messageType=', messageType)
      return
    }
    
    if (!messageId || !messageType) {
      console.warn('❌ ACK参数不完整: messageId=', messageId, 'messageType=', messageType)
      return
    }
    
    try {
      const ackData = {
        messageId: messageId,
        messageType: messageType
      }
      
      const ackJson = JSON.stringify(ackData)
      
      // ✅ 使用STOMP的publish方法发送ACK消息
      // @stomp/stompjs的publish方法签名：publish(destination, headers, body)
      // 或者：publish({destination, headers, body})
      if (this.stompClient && this.stompClient.publish) {
        // 方式1：使用对象参数（推荐）
        this.stompClient.publish({
          destination: '/app/ack',
          body: ackJson
        })
        
        console.log('✅ [ACK发送] 已发送ACK确认到后端:', {
          messageId: messageId,
          messageType: messageType,
          destination: '/app/ack',
          timestamp: new Date().toISOString()
        })
      } else {
        console.error('❌ STOMP客户端未初始化或publish方法不存在')
      }
    } catch (error) {
      console.error('❌ 发送ACK失败: messageId=', messageId, 'messageType=', messageType, 'error=', error, error.stack)
    }
  }
}

// 单例模式
export const wsClient = new WebSocketClient()

