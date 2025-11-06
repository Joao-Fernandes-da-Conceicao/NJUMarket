import { defineStore } from 'pinia'
import { wsClient } from '../utils/websocket'

/**
 * 订单 Store
 * 用于管理订单变化提醒的角标状态和通知列表
 * 
 * ✅ 预留功能：弹出式提醒卡片
 * - 保存完整的通知信息，包括 changeType, orderId, orderStatus, timestamp
 * - 提供通知列表供弹出式卡片组件使用
 * - 支持触发通知事件，便于组件监听并显示弹出式卡片
 */
export const useOrderStore = defineStore('order', {
  state: () => ({
    // 卖家订单变化提醒（用于"我的主页"角标）
    sellerOrderHasNew: false,
    // 买家订单变化提醒（用于"我的订单"角标）
    buyerOrderHasNew: false,
    
    // ✅ 预留：通知列表（用于弹出式提醒卡片）
    // 保存最近收到的订单变化通知，最多保存10条
    notifications: [],
    // 最大通知数量
    maxNotifications: 10,
    // 通知事件监听器列表
    notificationListeners: [],
    // ✅ 订单更新回调（用于在相关页面时智能更新订单）
    orderUpdateCallbacks: {
      buyer: null, // 买家订单页面更新回调
      seller: null // 卖家订单页面更新回调
    }
  }),
  
  getters: {
    // 是否有卖家订单变化
    hasSellerOrderChange: (state) => state.sellerOrderHasNew,
    
    // 是否有买家订单变化
    hasBuyerOrderChange: (state) => state.buyerOrderHasNew,
    
    // ✅ 预留：获取未读通知数量（可用于弹出式卡片计数）
    unreadNotificationCount: (state) => state.notifications.length,
    
    // ✅ 预留：获取最新的通知（用于显示弹出式卡片）
    latestNotification: (state) => {
      return state.notifications.length > 0 
        ? state.notifications[state.notifications.length - 1] 
        : null
    },
    
    // ✅ 预留：获取卖家相关的通知（根据 targetRole 筛选）
    sellerNotifications: (state) => {
      return state.notifications.filter(n => n.targetRole === 'SELLER')
    },
    
    // ✅ 预留：获取买家相关的通知（根据 targetRole 筛选）
    buyerNotifications: (state) => {
      return state.notifications.filter(n => n.targetRole === 'BUYER')
    }
  },
  
  actions: {
    /**
     * ✅ v1.3.x: 从后端获取订单提醒状态（应用启动时调用）
     */
    async fetchOrderReminderStatus() {
      try {
        const { profileAPI } = await import('../api')
        const response = await profileAPI.getOrderReminderStatus()
        if (response && response.success) {
          const status = response.data || {}
          this.sellerOrderHasNew = status.sellerOrderHasNew || false
          this.buyerOrderHasNew = status.buyerOrderHasNew || false
          console.log('订单提醒状态已从后端加载:', {
            sellerOrderHasNew: this.sellerOrderHasNew,
            buyerOrderHasNew: this.buyerOrderHasNew
          })
        }
      } catch (error) {
        // ✅ 兼容性处理：如果API失败（可能是旧版本后端），不影响应用启动
        console.warn('获取订单提醒状态失败（不影响应用启动）:', error)
        // 保持默认值 false
      }
    },
    
    /**
     * 设置卖家订单变化提醒
     */
    setSellerOrderHasNew(hasNew) {
      this.sellerOrderHasNew = hasNew
    },
    
    /**
     * 设置买家订单变化提醒
     */
    setBuyerOrderHasNew(hasNew) {
      this.buyerOrderHasNew = hasNew
    },
    
    /**
     * 清除卖家订单变化提醒（进入"卖家订单"页面时调用）
     */
    async clearSellerOrderNotification() {
      this.sellerOrderHasNew = false
      
      // ✅ v1.3.x: 同步清除后端数据库状态（向后兼容，如果API失败不影响前端状态）
      try {
        const { profileAPI } = await import('../api')
        await profileAPI.clearOrderReminder('SELLER')
      } catch (error) {
        console.warn('清除卖家订单提醒状态失败（不影响前端状态）:', error)
      }
      
      // ✅ 预留：可选择清除卖家相关的通知（用于弹出式卡片）
      // 如果需要清除，可以取消下面的注释：
      // this.notifications = this.notifications.filter(n => n.targetRole !== 'SELLER')
    },
    
    /**
     * 清除买家订单变化提醒（进入"我的订单"页面时调用）
     */
    async clearBuyerOrderNotification() {
      this.buyerOrderHasNew = false
      
      // ✅ v1.3.x: 同步清除后端数据库状态（向后兼容，如果API失败不影响前端状态）
      try {
        const { profileAPI } = await import('../api')
        await profileAPI.clearOrderReminder('BUYER')
      } catch (error) {
        console.warn('清除买家订单提醒状态失败（不影响前端状态）:', error)
      }
      
      // ✅ 预留：可选择清除买家相关的通知（用于弹出式卡片）
      // 如果需要清除，可以取消下面的注释：
      // this.notifications = this.notifications.filter(n => n.targetRole !== 'BUYER')
    },
    
    /**
     * 处理订单变化通知
     * @param {Object} notification - WebSocket推送的订单变化通知
     * 包含字段：type, orderId, changeType, orderStatus, targetRole, timestamp
     */
    handleOrderChangeNotification(notification) {
      const changeType = notification.changeType
      const orderId = notification.orderId
      const orderStatus = notification.orderStatus
      const targetRole = notification.targetRole // ✅ 目标角色：SELLER 或 BUYER
      const timestamp = notification.timestamp || new Date().toISOString()
      
      // ✅ 提取完整订单DTO（如果存在）- 必须在创建notificationItem之前定义
      const orderDTO = notification.order || null
      
      console.log('收到订单变化通知:', { changeType, orderId, orderStatus, targetRole, timestamp, hasOrderDTO: !!orderDTO })
      
      // ✅ 预留：保存通知到列表（用于弹出式卡片显示）
      const notificationItem = {
        id: `${orderId}_${changeType}_${Date.now()}`, // 唯一ID
        orderId,
        changeType,
        orderStatus,
        targetRole, // ✅ 保存目标角色
        order: orderDTO || null, // ✅ 保存完整订单DTO（ORDER_CREATED时包含）
        timestamp,
        read: false, // 是否已读（预留，用于区分已读/未读）
        createdAt: new Date().toISOString() // 本地接收时间
      }
      
      // 添加到通知列表（保持最多maxNotifications条）
      this.notifications.push(notificationItem)
      if (this.notifications.length > this.maxNotifications) {
        this.notifications.shift() // 移除最旧的通知
      }
      
      // ✅ 根据 targetRole 判断应该显示哪个角标（而不是根据 changeType）
      // 这样可以避免 ORDER_CANCELLED 和 ORDER_COMPLETED 等双方都可能收到的情况导致的角标错误
      const currentPath = window.location.pathname
      
      if (targetRole === 'SELLER') {
        // 卖家通知
        if (currentPath.startsWith('/seller-orders')) {
          // ✅ 在卖家订单页面，调用智能更新回调（传递完整订单DTO）
          if (this.orderUpdateCallbacks.seller) {
            try {
              this.orderUpdateCallbacks.seller(orderId, changeType, orderStatus, orderDTO)
            } catch (error) {
              console.error('调用卖家订单更新回调失败:', error)
            }
          }
        } else {
          // 不在卖家订单页面，显示角标
          this.setSellerOrderHasNew(true)
        }
      } else if (targetRole === 'BUYER') {
        // 买家通知
        if (currentPath === '/orders' || currentPath.startsWith('/orders/')) {
          // ✅ 在买家订单页面，调用智能更新回调（传递完整订单DTO）
          if (this.orderUpdateCallbacks.buyer) {
            try {
              this.orderUpdateCallbacks.buyer(orderId, changeType, orderStatus, orderDTO)
            } catch (error) {
              console.error('调用买家订单更新回调失败:', error)
            }
          }
        } else {
          // 不在买家订单页面，显示角标
          // 注意：/seller-orders 是卖家订单，不在此判断范围内
          this.setBuyerOrderHasNew(true)
        }
      } else {
        // 兼容旧版本：如果没有 targetRole，使用 changeType 判断（向后兼容）
        console.warn('收到未包含 targetRole 的通知，使用 changeType 判断:', notification)
        const sellerNotifications = [
          'ORDER_CREATED',
          'ORDER_PAID',
          'ORDER_COMPLETED',
          'ORDER_CANCELLED',
          'REFUND_REQUESTED'
        ]
        
        const buyerNotifications = [
          'ORDER_SHIPPED',
          'ORDER_COMPLETED',
          'ORDER_CANCELLED',
          'REFUND_APPROVED',
          'REFUND_REJECTED'
        ]
        
        if (sellerNotifications.includes(changeType)) {
          if (!currentPath.startsWith('/seller-orders')) {
            this.setSellerOrderHasNew(true)
          }
        }
        
        if (buyerNotifications.includes(changeType)) {
          if (currentPath !== '/orders' && !currentPath.startsWith('/orders/')) {
            this.setBuyerOrderHasNew(true)
          }
        }
      }
      
      // ✅ 预留：触发通知事件（供弹出式卡片组件监听）
      this.triggerNotificationEvent(notificationItem)
    },
    
    /**
     * ✅ 预留：触发通知事件
     * 供弹出式提醒卡片组件监听并显示通知
     * @param {Object} notificationItem - 通知项
     */
    triggerNotificationEvent(notificationItem) {
      // 触发所有注册的监听器
      this.notificationListeners.forEach(listener => {
        try {
          listener(notificationItem)
        } catch (error) {
          console.error('通知监听器执行失败:', error)
        }
      })
    },
    
    /**
     * ✅ 预留：注册通知监听器
     * 用于弹出式卡片组件监听新通知
     * @param {Function} listener - 监听器函数，接收 notificationItem 参数
     * @returns {Function} 取消监听的函数
     */
    onNotification(listener) {
      if (typeof listener !== 'function') {
        console.error('通知监听器必须是函数')
        return () => {}
      }
      
      this.notificationListeners.push(listener)
      
      // 返回取消监听的函数
      return () => {
        const index = this.notificationListeners.indexOf(listener)
        if (index > -1) {
          this.notificationListeners.splice(index, 1)
        }
      }
    },
    
    /**
     * ✅ 预留：标记通知为已读
     * @param {String} notificationId - 通知ID
     */
    markNotificationAsRead(notificationId) {
      const notification = this.notifications.find(n => n.id === notificationId)
      if (notification) {
        notification.read = true
      }
    },
    
    /**
     * ✅ 预留：清除所有通知
     */
    clearAllNotifications() {
      this.notifications = []
    },
    
    /**
     * ✅ 预留：清除指定订单的通知
     * @param {String} orderId - 订单ID
     */
    clearOrderNotifications(orderId) {
      this.notifications = this.notifications.filter(n => n.orderId !== orderId)
    },
    
    /**
     * ✅ 预留：获取通知文本（用于弹出式卡片显示）
     * @param {String} changeType - 变化类型
     * @returns {String} 通知文本
     */
    getNotificationText(changeType) {
      const textMap = {
        'ORDER_CREATED': '新订单',
        'ORDER_PAID': '订单已支付',
        'ORDER_SHIPPED': '订单已发货',
        'ORDER_COMPLETED': '订单已完成',
        'ORDER_CANCELLED': '订单已取消',
        'REFUND_REQUESTED': '收到退款申请',
        'REFUND_APPROVED': '退款已同意',
        'REFUND_REJECTED': '退款已拒绝',
        'ORDER_VISIBILITY_RESTORED': '订单已恢复显示' // ✅ 可见性恢复通知
      }
      return textMap[changeType] || '订单状态变化'
    },
    
    /**
     * 初始化WebSocket监听
     * 在应用启动时调用
     */
    initWebSocketListeners() {
      // 注册订单变化通知的处理函数
      wsClient.on('ORDER_CHANGE', (notification) => {
        this.handleOrderChangeNotification(notification)
      })
    },
    
    /**
     * 清除WebSocket监听
     * 在应用卸载时调用
     */
    clearWebSocketListeners() {
      wsClient.off('ORDER_CHANGE')
    },
    
    /**
     * ✅ 注册订单更新回调
     * 用于在订单页面时智能更新订单，而不是重新加载整个列表
     * @param {String} role - 'buyer' 或 'seller'
     * @param {Function} callback - 更新回调函数 (orderId, changeType, orderStatus) => void
     */
    registerOrderUpdateCallback(role, callback) {
      if (role === 'buyer' || role === 'seller') {
        this.orderUpdateCallbacks[role] = callback
      } else {
        console.error('无效的角色类型:', role)
      }
    },
    
    /**
     * ✅ 取消注册订单更新回调
     * @param {String} role - 'buyer' 或 'seller'
     */
    unregisterOrderUpdateCallback(role) {
      if (role === 'buyer' || role === 'seller') {
        this.orderUpdateCallbacks[role] = null
      }
    }
  }
})

/**
 * ✅ 预留功能使用示例：弹出式提醒卡片组件
 * 
 * 在 Vue 组件中使用：
 * 
 * ```vue
 * <template>
 *   <div>
 *     <!-- 弹出式通知卡片 -->
 *     <OrderNotificationCard
 *       v-if="showNotification"
 *       :notification="currentNotification"
 *       @close="handleNotificationClose"
 *       @view-order="handleViewOrder"
 *     />
 *   </div>
 * </template>
 * 
 * <script setup>
 * import { ref, onMounted, onUnmounted } from 'vue'
 * import { useRouter } from 'vue-router'
 * import { useOrderStore } from '../stores/order'
 * 
 * const router = useRouter()
 * const orderStore = useOrderStore()
 * 
 * const showNotification = ref(false)
 * const currentNotification = ref(null)
 * 
 * // 注册通知监听器
 * const unsubscribe = orderStore.onNotification((notification) => {
 *   // 显示弹出式卡片
 *   currentNotification.value = notification
 *   showNotification.value = true
 *   
 *   // 自动隐藏（可选）
 *   setTimeout(() => {
 *     showNotification.value = false
 *   }, 5000)
 * })
 * 
 * const handleNotificationClose = () => {
 *   showNotification.value = false
 *   if (currentNotification.value) {
 *     orderStore.markNotificationAsRead(currentNotification.value.id)
 *   }
 * }
 * 
 * const handleViewOrder = (orderId) => {
 *   router.push(`/orders/${orderId}`)
 *   handleNotificationClose()
 * }
 * 
 * onMounted(() => {
 *   // 已自动注册监听器
 * })
 * 
 * onUnmounted(() => {
 *   // 取消监听器
 *   unsubscribe()
 * })
 * </script>
 * ```
 * 
 * 通知数据结构：
 * {
 *   id: "orderId_changeType_timestamp",
 *   orderId: "xxx",
 *   changeType: "ORDER_CREATED" | "ORDER_PAID" | ...,
 *   orderStatus: "CREATED" | "PAID" | ...,
 *   timestamp: "2025-01-20T10:30:00",
 *   read: false,
 *   createdAt: "2025-01-20T10:30:00"
 * }
 * 
 * 可用的 changeType：
 * - ORDER_CREATED: 新订单（卖家）
 * - ORDER_PAID: 订单已支付（卖家）
 * - ORDER_SHIPPED: 订单已发货（买家）
 * - ORDER_COMPLETED: 订单已完成（卖家和买家）
 * - ORDER_CANCELLED: 订单已取消（卖家和买家）
 * - REFUND_REQUESTED: 收到退款申请（卖家）
 * - REFUND_APPROVED: 退款已同意（买家）
 * - REFUND_REJECTED: 退款已拒绝（买家）
 */

