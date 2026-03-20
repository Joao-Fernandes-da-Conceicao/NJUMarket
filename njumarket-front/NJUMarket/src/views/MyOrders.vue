<template>
  <div class="orders-page" @click="handlePageClick">
    <!-- 未登录状态提示 -->
    <div v-if="!isLoggedIn" class="login-prompt">
      <div class="container">
        <div class="prompt-content">
          <el-icon size="64" class="prompt-icon"><User /></el-icon>
          <h2>请先登录</h2>
          <p>您需要登录后才能查看订单信息</p>
          <div class="prompt-actions">
            <el-button type="primary" @click="$router.push('/login')">
              立即登录
            </el-button>
            <el-button @click="$router.push('/register')">
              注册账号
            </el-button>
            <el-button @click="$router.push('/')">
              返回首页
            </el-button>
          </div>
        </div>
      </div>
    </div>
    
    <!-- 已登录状态 - 订单内容 -->
    <div v-else class="orders-content">
      <div class="container">
        <div class="page-header">
          <h1>买家订单</h1>
          <div class="header-actions">
            <el-button type="success" @click="$router.push('/seller-orders')">
              卖家订单
            </el-button>
            <el-button type="primary" @click="$router.push('/commodities')">
              继续购物
            </el-button>
          </div>
        </div>

        <!-- 订单状态筛选 -->
        <OrderTabs 
          v-model="activeTab" 
          @tab-change="handleTabChange"
          :show-refund-rejected="true"
        />

        <!-- 订单列表 -->
        <div class="orders-list" v-loading="loading">
          <div v-if="orders.length === 0 && !loading" class="empty-state">
            <el-empty :description="getEmptyDescription()">
              <el-button type="primary" @click="handleEmptyAction()">
                {{ getEmptyActionText() }}
              </el-button>
            </el-empty>
          </div>

          <OrderCard
            v-for="order in orders"
            :key="order.orderId"
            :order="order"
            type="buyer"
            :class="{ 'action-dropdown-open': showActionSelectId === order.orderId }"
            @order-id-click="showOrderIdDetails"
          >
            <template #actions>
              <!-- 状态相关操作 -->
              <el-button
                v-if="canPay(order)"
                type="primary"
                class="action-btn-desktop"
                @click="handlePay(order.orderId)"
              >
                立即支付
              </el-button>
              <el-button
                v-if="canConfirm(order)"
                type="success"
                class="action-btn-desktop"
                @click="handleConfirm(order.orderId)"
              >
                确认收货
              </el-button>
              <el-button
                v-if="canCancel(order)"
                class="action-btn-desktop"
                @click="handleCancel(order.orderId)"
              >
                取消订单
              </el-button>
              <el-button
                v-if="order.orderStatus === 'COMPLETED'"
                class="action-btn-desktop"
                @click="handleRefund(order.orderId)"
              >
                申请退款
              </el-button>
              <el-button
                v-if="order.orderStatus === 'REFUND_REJECTED'"
                type="warning"
                class="action-btn-desktop"
                @click="handleRefund(order.orderId)"
              >
                重新申请退款
              </el-button>
              
              <!-- 桌面端：查询商品、再下一单、查看详情、删除 -->
              <el-button
                v-if="canQueryCommodity(order)"
                class="action-btn-desktop"
                @click="handleQueryCommodity(order.orderId)"
              >
                查询商品
              </el-button>
              <el-button
                v-if="canCreateNewOrder(order)"
                type="success"
                class="action-btn-desktop"
                @click="handleCreateNewOrder(order.orderId)"
              >
                再下一单
              </el-button>
              <el-button 
                class="action-btn-desktop"
                @click="viewOrderDetail(order.orderId)"
              >
                查看详情
              </el-button>
              <el-button
                v-if="canDeleteOrder(order)"
                type="danger"
                class="action-btn-desktop"
                @click="handleDelete(order.orderId)"
              >
                删除
              </el-button>
              
              <!-- 移动端：操作选择器 -->
              <div class="action-select-mobile" @click.stop="toggleActionSelect(order.orderId)">
                <span class="select-label">操作</span>
                <el-icon class="select-icon"><ArrowDown /></el-icon>
                
                <!-- 弹出式选择器 -->
                <div v-if="showActionSelectId === order.orderId" class="select-popup" @click.stop>
                  <!-- 状态相关操作 -->
                  <div class="popup-option popup-option-primary" v-if="canPay(order)" @click.stop="handlePay(order.orderId); showActionSelectId = null">
                    立即支付
                  </div>
                  <div class="popup-option popup-option-success" v-if="canConfirm(order)" @click.stop="handleConfirm(order.orderId); showActionSelectId = null">
                    确认收货
                  </div>
                  <div class="popup-option" v-if="canCancel(order)" @click.stop="handleCancel(order.orderId); showActionSelectId = null">
                    取消订单
                  </div>
                  <div class="popup-option popup-option-warning" v-if="order.orderStatus === 'COMPLETED'" @click.stop="handleRefund(order.orderId); showActionSelectId = null">
                    申请退款
                  </div>
                  <div class="popup-option popup-option-warning" v-if="order.orderStatus === 'REFUND_REJECTED'" @click.stop="handleRefund(order.orderId); showActionSelectId = null">
                    重新申请退款
                  </div>
                  
                  <!-- 其他操作 -->
                  <div class="popup-option" v-if="canQueryCommodity(order)" @click.stop="handleQueryCommodity(order.orderId); showActionSelectId = null">
                    查询商品
                  </div>
                  <div class="popup-option popup-option-success" v-if="canCreateNewOrder(order)" @click.stop="handleCreateNewOrder(order.orderId); showActionSelectId = null">
                    再下一单
                  </div>
                  <div class="popup-option" @click.stop="viewOrderDetail(order.orderId); showActionSelectId = null">
                    查看详情
                  </div>
                  <div class="popup-option popup-option-delete" v-if="canDeleteOrder(order)" @click.stop="handleDelete(order.orderId); showActionSelectId = null">
                    删除
                  </div>
                </div>
              </div>
            </template>
          </OrderCard>
        </div>

        <!-- 分页 -->
        <Pagination 
          :total="total"
          :current-page="currentPage"
          :page-size="pageSize"
          :show-page-numbers="true"
          @page-change="handleCurrentChange"
          @page-size-change="handleSizeChange"
        />
      </div>
    </div>
  </div>
</template>

<script>
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '../stores/user'
import { useOrderStore } from '../stores/order'
import { orderAPI, imageAPI } from '../api'
import { canPayOrder, canConfirmOrder, canCancelOrder } from '../utils/orderRules'
import { ElMessage } from 'element-plus'
import { ArrowDown } from '@element-plus/icons-vue'
import OrderTabs from '../components/order/OrderTabs.vue'
import OrderCard from '../components/order/OrderCard.vue'
import Pagination from '../components/common/Pagination.vue'

export default {
  name: 'MyOrders',
  components: {
    OrderTabs,
    OrderCard,
    ArrowDown,
    Pagination,
  },
  setup() {
    const router = useRouter()
    const userStore = useUserStore()
    const orderStore = useOrderStore()
    
    const loading = ref(false)
    const orders = ref([])
    const total = ref(0)
    const currentPage = ref(1)
    const pageSize = ref(10)
    const activeTab = ref('all')
    const isMounted = ref(false)
    
    // 翻页器相关 (现在由 Pagination 组件管理)
    
    // 移动端订单号弹出窗口
    const showOrderIdPopup = ref(false)
    const currentOrderId = ref('')
    const showActionSelectId = ref(null)
    
    const user = ref(userStore.user)
    const isLoggedIn = ref(userStore.isLoggedIn)
    
    // 统一规则封装为模板可用的方法
    const canPay = (o) => canPayOrder({ order: o, user: user.value }).canPay
    const canConfirm = (o) => canConfirmOrder({ order: o, user: user.value }).canConfirm
    const canCancel = (o) => canCancelOrder({ order: o, user: user.value }).canCancel
    
    // 显示订单号弹出窗口
    const showOrderIdDetails = (orderId, event) => {
      event.stopPropagation()
      // 如果点击的是同一个订单号，则切换状态
      if (currentOrderId.value === orderId && showOrderIdPopup.value) {
        closeOrderIdPopup()
      } else {
        currentOrderId.value = orderId
        showOrderIdPopup.value = true
      }
    }
    
    // 关闭弹出窗口
    const closeOrderIdPopup = () => {
      showOrderIdPopup.value = false
    }
    
    // 切换操作选择器
    const toggleActionSelect = (orderId) => {
      if (showActionSelectId.value === orderId) {
        showActionSelectId.value = null
      } else {
        showActionSelectId.value = orderId
      }
    }
    
    // 点击外部区域关闭弹出窗口（使用事件对象）
    const handlePageClick = (event) => {
      if (showOrderIdPopup.value) {
        // 检查点击是否在弹出窗口内部
        const target = event.target
        if (!target.closest('.order-id-pill-wrapper')) {
          closeOrderIdPopup()
        }
      }
      
      // 关闭操作选择器
      showActionSelectId.value = null
      
      // 关闭所有选择器
    }
    
    // 获取订单列表
    const fetchOrders = async () => {
      loading.value = true
      try {
        const params = {
          page: currentPage.value,
          size: pageSize.value
        }
        
        if (activeTab.value !== 'all') {
          params.status = activeTab.value
        }
        
        const response = await orderAPI.getBuyerOrders(params.page, params.size, params.status)
        console.log('订单查询响应:', response) // 调试日志
        
        if (response.success) {
          // 处理后端返回的数据结构
          // 后端现在返回: { success: true, data: { orders: [...], total: 数量 }, message: "操作成功" }
          
          if (response.data && response.data.orders) {
            // 后端返回了正确的数据结构
            orders.value = response.data.orders
            total.value = response.data.total || 0
          } else {
            // 后端没有返回数据或数据结构异常
            console.warn('后端返回的数据结构异常:', response.data)
            if (currentPage.value === 1) {
              orders.value = []
              total.value = 0
            }
          }
        } else {
          // API调用失败时，保持当前状态
          console.warn('获取订单列表失败:', response.errorMsg)
        }
      } catch (error) {
        console.error('获取订单列表异常:', error)
        // 网络异常时，保持当前状态，不显示错误消息
      } finally {
        loading.value = false
      }
    }
    
    // 标签页切换
    const handleTabChange = (tabName) => {
      activeTab.value = tabName
      currentPage.value = 1
      fetchOrders()
    }
    
    // 分页大小改变
    const handleSizeChange = (val) => {
      pageSize.value = val
      currentPage.value = 1
      fetchOrders()
    }
    
    // 当前页改变
    const handleCurrentChange = (val) => {
      currentPage.value = val
      fetchOrders()
    }
    
    // 翻页器相关方法 (现在由 Pagination 组件处理)
    const getTotalPages = () => {
      return Math.ceil(total.value / pageSize.value)
    }
    
    // 支付订单
    const handlePay = async (orderId) => {
      const order = orders.value.find(o => o.orderId === orderId)
      if (!order) {
        ElMessage.error('订单不存在')
        return
      }
      
      const check = canPayOrder({ order, user: user.value })
      if (!check.canPay) {
        ElMessage.error(check.reason)
        return
      }
      
      try {
        const response = await orderAPI.pay(orderId)
        if (response.success) {
          ElMessage.success('支付成功')
          fetchOrders()
        } else {
          ElMessage.error(response.errorMsg || '支付失败')
        }
      } catch (error) {
        ElMessage.error('支付失败')
      }
    }
    
    // 确认收货
    const handleConfirm = async (orderId) => {
      const order = orders.value.find(o => o.orderId === orderId)
      if (!order) {
        ElMessage.error('订单不存在')
        return
      }
      
      const check = canConfirmOrder({ order, user: user.value })
      if (!check.canConfirm) {
        ElMessage.error(check.reason)
        return
      }
      
      try {
        const confirmed = confirm('确认收货后订单将完成，请确保商品完好无损')
        if (!confirmed) {
          return
        }
        
        const response = await orderAPI.confirm(orderId)
        if (response.success) {
          ElMessage.success('确认收货成功')
          fetchOrders()
        } else {
          ElMessage.error(response.errorMsg || '确认收货失败')
        }
      } catch (error) {
        ElMessage.error('确认收货失败')
      }
    }
    
    // 取消订单
    const handleCancel = async (orderId) => {
      const order = orders.value.find(o => o.orderId === orderId)
      if (!order) {
        ElMessage.error('订单不存在')
        return
      }
      
      const check = canCancelOrder({ order, user: user.value })
      if (!check.canCancel) {
        ElMessage.error(check.reason)
        return
      }
      
      try {
        const reason = prompt('请输入取消原因')
        if (reason === null) {
          // 用户取消输入
          return
        }
        if (!reason.trim()) {
          ElMessage.error('请输入取消原因')
          return
        }
        
        const response = await orderAPI.cancel(orderId, reason)
        if (response.success) {
          ElMessage.success('订单已取消')
          fetchOrders()
        } else {
          ElMessage.error(response.errorMsg || '取消订单失败')
        }
      } catch (error) {
        ElMessage.error('取消订单失败')
      }
    }
    
    // 申请退款
    const handleRefund = async (orderId) => {
      try {
        // 查找订单状态
        const order = orders.value.find(o => o.orderId === orderId)
        const isReapply = order && order.orderStatus === 'REFUND_REJECTED'
        
        const promptText = isReapply ? '请输入重新申请退款的原因' : '请输入退款原因'
        const refundReason = prompt(promptText)
        if (refundReason === null) {
          // 用户取消输入
          return
        }
        if (!refundReason.trim()) {
          ElMessage.error('请输入退款原因')
          return
        }
        
        const response = await orderAPI.requestRefund(orderId, refundReason)
        if (response.success) {
          const successMsg = isReapply ? '重新申请退款已提交' : '退款申请已提交'
          ElMessage.success(successMsg)
          fetchOrders()
        } else {
          ElMessage.error(response.errorMsg || '申请退款失败')
        }
      } catch (error) {
        ElMessage.error('申请退款失败')
      }
    }
    
    // 查询商品
    const handleQueryCommodity = async (orderId) => {
      try {
        const response = await orderAPI.queryOriginalCommodity(orderId)
        if (response.success) {
          const data = response.data
          const commoditySnapshot = data.commoditySnapshot
          
          // 如果能查到商品，直接跳转到商品详情页
          if (data.commodityExists && data.commodityOnShelf && commoditySnapshot.commodityId) {
            router.push(`/commodity/${commoditySnapshot.commodityId}`)
            return
          }
          
          // 否则显示商品信息
          let message = `商品信息：\n`
          message += `标题：${commoditySnapshot.title}\n`
          message += `价格：¥${commoditySnapshot.price}\n`
          message += `位置：${commoditySnapshot.location}\n`
          message += `分类：${commoditySnapshot.category}\n`
          message += `成色：${commoditySnapshot.conditionLevel}\n`
          message += `卖家：${commoditySnapshot.sellerName}\n`
          
          // 显示联系方式信息
          if (commoditySnapshot.sellerPhone) {
            message += `卖家电话：${commoditySnapshot.sellerPhone}\n`
          }
          if (commoditySnapshot.sellerEmail) {
            message += `卖家邮箱：${commoditySnapshot.sellerEmail}\n`
          }
          
          // 显示快照时间
          if (commoditySnapshot.snapshotTime) {
            const snapshotDate = new Date(commoditySnapshot.snapshotTime).toLocaleString()
            message += `快照时间：${snapshotDate}\n`
          }
          
          message += `\n当前状态：${data.statusMessage}`
          
          if (data.commodityExists && !data.commodityOnShelf) {
            message += `\n当前库存：${data.currentStock}\n`
            message += `当前价格：¥${data.currentPrice}`
            
            // 显示价格变化
            if (data.currentPrice !== commoditySnapshot.price) {
              const priceChange = data.currentPrice - commoditySnapshot.price
              const changeText = priceChange > 0 ? `上涨¥${priceChange.toFixed(2)}` : `下降¥${Math.abs(priceChange).toFixed(2)}`
              message += `\n价格变化：${changeText}`
            }
          }
          
          alert(message)
        } else {
          ElMessage.error(response.errorMsg || '查询商品失败')
        }
      } catch (error) {
        ElMessage.error('查询商品失败')
      }
    }
    
    // 再下一单
    const handleCreateNewOrder = (orderId) => {
      // 跳转到下单页面，传递订单ID
      router.push(`/create-order/${orderId}`)
    }
    
    // 查看订单详情
    const viewOrderDetail = (orderId) => {
      router.push(`/order/${orderId}`)
    }
    
    // 修改订单可见性
    const handleVisibilityChange = async (orderId, visibility) => {
      try {
        const response = await orderAPI.updateVisibility(orderId, visibility)
        if (response.success) {
          ElMessage.success('可见性修改成功')
          fetchOrders()
        } else {
          ElMessage.error(response.errorMsg || '修改失败')
        }
      } catch (error) {
        ElMessage.error('修改失败')
      }
    }
    
    // 删除订单（软删除，通过设置可见性为HIDDEN）
    const handleDelete = async (orderId) => {
      try {
        // 找到要删除的订单
        const order = orders.value.find(o => o.orderId === orderId)
        if (!order) {
          ElMessage.error('订单不存在')
          return
        }
        
        // 检查订单是否有重要事件（如退款申请中）
        const hasImportantEvents = ['REFUND_REQUESTED', 'REFUND_APPROVED', 'REFUND_REJECTED'].includes(order.orderStatus)
        
        let confirmMessage = '确定要删除这个订单吗？删除后可以在历史记录中恢复'
        
        if (hasImportantEvents) {
          confirmMessage = '该订单存在退款等重要事件，删除后当买家操作退款时，订单会重新可见。确定要继续吗？'
        }
        
        const confirmed = confirm(confirmMessage)
        if (!confirmed) {
          return
        }
        
        // 软删除：只设置买家可见性为HIDDEN
        // 如果卖家也删除了，则订单完全隐藏
        // 如果卖家未删除，退款时，订单会重新可见
        const response = await orderAPI.updateBuyerVisibility(orderId, 'HIDDEN')
        if (response.success) {
          ElMessage.success('订单已删除')
          fetchOrders()
        } else {
          // 如果后端拒绝修改可见性，说明订单状态不允许删除
          if (response.errorMsg && response.errorMsg.includes('不允许')) {
            ElMessage.warning('该订单状态不允许删除（可能存在退款等重要事件）')
          } else {
            ElMessage.error(response.errorMsg || '删除失败')
          }
        }
      } catch (error) {
        ElMessage.error('删除失败')
      }
    }
    
    // 获取状态类型
    const getStatusType = (status) => {
      const statusMap = {
        'CREATED': 'warning',
        'PAID': 'info',
        'SHIPPED': 'primary',
        'COMPLETED': 'success',
        'CANCELLED': 'danger',
        'REFUND_REQUESTED': 'warning',
        'REFUND_APPROVED': 'success',
        'REFUND_REJECTED': 'danger'
      }
      return statusMap[status] || 'info'
    }
    
    // 获取状态文本
    const getStatusText = (status) => {
      const statusMap = {
        'CREATED': '待支付',
        'PAID': '待发货',
        'SHIPPED': '待收货',
        'COMPLETED': '已完成',
        'CANCELLED': '已取消',
        'REFUND_REQUESTED': '退款中',
        'REFUND_APPROVED': '退款完成',
        'REFUND_REJECTED': '退款被拒'
      }
      return statusMap[status] || status
    }
    
    // 获取空状态描述
    const getEmptyDescription = () => {
      const statusMap = {
        'all': '暂无订单',
        'CREATED': '暂无待支付订单',
        'PAID': '暂无待发货订单',
        'SHIPPED': '暂无待收货订单',
        'COMPLETED': '暂无已完成订单',
        'CANCELLED': '暂无已取消订单',
        'REFUND_REQUESTED': '暂无退款订单',
        'REFUND_APPROVED': '暂无退款完成订单',
        'REFUND_REJECTED': '暂无退款被拒订单'
      }
      return statusMap[activeTab.value] || '暂无订单'
    }
    
    // 获取空状态操作按钮文本
    const getEmptyActionText = () => {
      const actionMap = {
        'all': '去购物',
        'CREATED': '去购物',
        'PAID': '去购物',
        'SHIPPED': '去购物',
        'COMPLETED': '去购物',
        'CANCELLED': '去购物',
        'REFUND_REQUESTED': '去购物',
        'REFUND_APPROVED': '去购物',
        'REFUND_REJECTED': '去购物'
      }
      return actionMap[activeTab.value] || '去购物'
    }
    
    // 处理空状态操作
    const handleEmptyAction = () => {
      router.push('/commodities')
    }
    
    // 格式化时间
    const formatTime = (time) => {
      if (!time) return ''
      return new Date(time).toLocaleString()
    }
    
    // 获取商品图片URL
    const getCommodityImageUrl = (imageUrl) => {
      if (!imageUrl) return imageAPI.getDefaultCommodityImage()
      
      // 如果已经是完整URL，直接返回
      if (imageUrl.startsWith('http')) return imageUrl
      
      // 如果是文件名，构建完整URL
      if (imageUrl.includes('/')) return imageUrl
      
      // 从URL中提取文件名
      const fileName = imageUrl.split('/').pop()
      return imageAPI.getCommodityImage(fileName)
    }
    
    // 获取商品快照标题
    const getCommoditySnapshotTitle = (order) => {
      return order.commoditySnapshotTitle || order.commodity?.title
    }
    
    // 获取商品快照图片
    const getCommoditySnapshotImage = (order) => {
      if (!order.commoditySnapshotImages) return null
      
      try {
        // 解析JSON格式的图片URL列表
        const images = JSON.parse(order.commoditySnapshotImages)
        if (images && images.length > 0) {
          return getCommodityImageUrl(images[0])
        }
      } catch (error) {
        // 如果不是JSON格式，直接使用
        return getCommodityImageUrl(order.commoditySnapshotImages)
      }
      
      return null
    }
    
    // 检查商品快照是否已下架
    const isCommoditySnapshotOffShelf = (order) => {
      return order.commoditySnapshotStatus === 'OFF_SHELF' || 
             order.commoditySnapshotStatus === 'DRAFT'
    }
    
    // 检查是否可以查询商品
    const canQueryCommodity = (order) => {
      // 必须有商品快照信息
      return order.commoditySnapshotTitle != null
    }
    
    // 检查是否可以再下一单
    const canCreateNewOrder = (order) => {
      // 必须有商品快照信息
      if (!order.commoditySnapshotTitle) {
        return false
      }
      
      return true
    }
    
    // 检查是否可以删除订单
    const canDeleteOrder = (order) => {
      // 允许删除的状态：已取消、已完成、退款完成
      const deletableStatuses = ['CANCELLED', 'COMPLETED', 'REFUND_APPROVED']
      return deletableStatuses.includes(order.orderStatus)
    }
    
    // 登出
    const handleLogout = async () => {
      try {
        await userStore.logout()
        // userStore.logout()会处理跳转，不需要额外的跳转和消息
      } catch (error) {
        ElMessage.error('退出登录失败')
      }
    }
    
    /**
     * ✅ 智能更新订单（根据WebSocket通知）
     * 根据changeType只更新变化的订单卡片，而不是重新加载整个列表
     * @param {String} orderId - 订单ID
     * @param {String} changeType - 变化类型
     * @param {String} orderStatus - 订单状态
     * @param {Object} orderDTO - 完整的订单DTO（可选，ORDER_CREATED时包含）
     */
    const smartUpdateOrder = async (orderId, changeType, orderStatus, orderDTO = null) => {
      console.log('智能更新买家订单:', { orderId, changeType, orderStatus, hasOrderDTO: !!orderDTO })
      
      // 查找订单在列表中的位置
      const orderIndex = orders.value.findIndex(o => o.orderId === orderId)
      
      // ✅ 如果是可见性恢复（极端情况），使用完整的orderDTO直接更新或添加
      if (changeType === 'ORDER_VISIBILITY_RESTORED') {
        console.log('订单可见性已恢复，使用完整OrderDTO更新:', orderId)
        if (orderDTO) {
          if (orderIndex !== -1) {
            // 订单在列表中，直接更新
            orders.value[orderIndex] = orderDTO
            console.log('订单已更新:', orderDTO)
          } else {
            // 订单不在列表中，检查是否应该添加到当前筛选范围
            if (activeTab.value === 'all' || activeTab.value === orderDTO.orderStatus) {
              orders.value.unshift(orderDTO)
              total.value = (total.value || 0) + 1
              console.log('订单已添加到列表:', orderDTO)
            }
          }
        } else {
          // 如果没有提供完整OrderDTO，刷新列表（兼容旧版本）
          console.warn('ORDER_VISIBILITY_RESTORED通知未包含完整OrderDTO，刷新列表')
          fetchOrders()
        }
        return
      }
      
      if (orderIndex === -1) {
        // 订单不在当前列表中（可能是新订单或不在当前筛选范围内）
        // 注意：买家不会收到ORDER_CREATED通知（订单是买家创建的），但保留此逻辑以防万一
        if (changeType === 'ORDER_CREATED') {
          if (orderDTO) {
            // 如果提供了完整OrderDTO，直接添加到列表
            if (activeTab.value === 'all' || activeTab.value === orderDTO.orderStatus) {
              orders.value.unshift(orderDTO)
              total.value = (total.value || 0) + 1
              console.log('新订单已添加到列表:', orderDTO)
            }
          } else {
            // 如果没有提供完整OrderDTO，刷新列表
            if (activeTab.value === 'all') {
              fetchOrders()
            }
          }
        }
        return
      }
      
      // 订单在当前列表中，智能更新
      const order = orders.value[orderIndex]
      
      // 根据changeType更新订单状态和相关字段
      switch (changeType) {
        case 'ORDER_PAID':
          // 订单已支付（买家自己支付的，理论上不应该出现在买家订单通知中）
          // 但为了完整性保留此case
          order.orderStatus = 'PAID'
          break
          
        case 'ORDER_SHIPPED':
          // 订单已发货
          order.orderStatus = 'SHIPPED'
          // 注意：订单卡片不显示trackingNumber，因此不需要异步获取详情
          // 如果将来需要在订单卡片中显示物流单号，可以取消以下注释：
          // orderAPI.getDetail(orderId).then(response => {
          //   if (response.success && response.data) {
          //     const orderIndex = orders.value.findIndex(o => o.orderId === orderId)
          //     if (orderIndex !== -1) {
          //       Object.assign(orders.value[orderIndex], response.data)
          //     }
          //   }
          // }).catch(err => {
          //   console.error('获取订单详情失败:', err)
          // })
          break
          
        case 'ORDER_COMPLETED':
          // 订单已完成
          order.orderStatus = 'COMPLETED'
          break
          
        case 'ORDER_CANCELLED':
          // 订单已取消
          order.orderStatus = 'CANCELLED'
          break
          
        case 'REFUND_APPROVED':
          // 退款已同意
          order.orderStatus = 'REFUND_APPROVED'
          break
          
        case 'REFUND_REJECTED':
          // 退款已拒绝
          order.orderStatus = 'REFUND_REJECTED'
          break
          
        default:
          // 其他变化类型，更新订单状态
          order.orderStatus = orderStatus
      }
      
      // 如果订单状态变化后不在当前筛选范围内，需要从列表中移除
      if (activeTab.value !== 'all' && order.orderStatus !== activeTab.value) {
        orders.value.splice(orderIndex, 1)
        total.value = Math.max(0, total.value - 1)
      } else {
        // 更新响应式（Vue会自动检测到变化）
        orders.value[orderIndex] = { ...order }
      }
    }
    
    onMounted(() => {
      isMounted.value = true
      // ✅ 清除买家订单变化提醒角标
      orderStore.clearBuyerOrderNotification()
      // ✅ 注册订单更新回调
      orderStore.registerOrderUpdateCallback('buyer', smartUpdateOrder)
      fetchOrders()
    })
    
    // 组件卸载时清理
    onUnmounted(() => {
      isMounted.value = false
      // ✅ 取消注册订单更新回调
      orderStore.unregisterOrderUpdateCallback('buyer')
    })
    
    return {
      loading,
      orders,
      total,
      currentPage,
      pageSize,
      activeTab,
      showOrderIdPopup,
      currentOrderId,
      showOrderIdDetails,
      closeOrderIdPopup,
      handlePageClick,
      user,
      isLoggedIn,
      handleTabChange,
      handleSizeChange,
      handleCurrentChange,
      getTotalPages,
      toggleActionSelect,
      showActionSelectId,
      canPay,
      canConfirm,
      canCancel,
      handlePay,
      handleConfirm,
      handleCancel,
      handleRefund,
      handleQueryCommodity,
      handleCreateNewOrder,
      canDeleteOrder,
      viewOrderDetail,
      handleVisibilityChange,
      handleDelete,
      getStatusType,
      getStatusText,
      getEmptyDescription,
      getEmptyActionText,
      handleEmptyAction,
      formatTime,
      getCommodityImageUrl,
      getCommoditySnapshotTitle,
      getCommoditySnapshotImage,
      isCommoditySnapshotOffShelf,
      canQueryCommodity,
      canCreateNewOrder,
      handleLogout
    }
  }
}
</script>

<style scoped>
.orders-page {
  min-height: 100vh;
  background-color: #f5f5f5;
}

/* 登录提示样式 */
.login-prompt {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px 0;
}

.prompt-content {
  background: white;
  border-radius: 16px; /* 使用主页的圆角设计 */
  padding: 60px 40px;
  text-align: center;
  border: none; /* 移除边框 */
  box-shadow: none; /* 移除阴影，使用无框设计 */
  max-width: 500px;
  width: 100%;
}

.prompt-icon {
  color: var(--primary-color);
  margin-bottom: 20px;
}

.prompt-content h2 {
  font-size: 28px;
  font-weight: normal;
  color: var(--primary-color);
  margin-bottom: 16px;
}

.prompt-content p {
  font-size: 16px;
  color: var(--text-secondary);
  margin-bottom: 32px;
  line-height: 1.5;
}

.prompt-actions {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

/* border-radius 由 UnifiedButton 统一管理（9999px） */
.prompt-actions .unified-button :deep(.el-button) {
  height: 48px;
  font-size: 16px;
  font-weight: normal;
}

@media (min-width: 768px) {
  .prompt-actions {
    flex-direction: row;
    justify-content: center;
    gap: 16px;
  }
  
  .prompt-actions .el-button {
    min-width: 120px;
  }
}

.orders-content {
  padding: 40px 0; /* 增加间距以匹配主页设计 */
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 30px;
}


.header-actions {
  display: flex;
  gap: 12px;
}

/* border-radius 由 UnifiedButton 统一管理（9999px） */
.header-actions .unified-button :deep(.el-button) {
  font-weight: normal;
}

.order-tabs {
  background: transparent; /* 透明背景 */
  border-radius: 16px;
  padding: 0;
  margin-bottom: 24px;
  border: none;
  box-shadow: none;
}

.order-tabs :deep(.el-tabs__header) {
  background: white;
  border-radius: 28.3px; /* 药丸形标签页头部 */
  padding: 8px;
  border: 1px solid var(--primary-color);
  margin-bottom: 0;
}

.order-tabs :deep(.el-tabs__nav-wrap::after) {
  display: none; /* 隐藏底部边框线 */
}

.order-tabs :deep(.el-tabs__item) {
  border-radius: 20px; /* 药丸形标签页项 */
  font-weight: normal;
  color: var(--primary-color);
  border: none;
  padding: 8px 20px !important; /* 使用!important确保padding生效 */
  margin: 0 5px; /* 添加左右5px的间距 */
  /* 只动画颜色相关属性，避免触发 ResizeObserver */
  transition: background-color 0.2s ease, color 0.2s ease;
}

.order-tabs :deep(.el-tabs__item:first-child) {
  padding: 8px 20px !important; /* 确保第一个标签的padding */
  margin-left: 0; /* 第一个标签左边无间距 */
  margin-right: 5px; /* 右边保持5px间距 */
}

.order-tabs :deep(.el-tabs__item:last-child) {
  padding: 8px 20px !important; /* 确保最后一个标签的padding */
  margin-left: 5px; /* 左边保持5px间距 */
  margin-right: 0; /* 最后一个标签右边无间距 */
}

.order-tabs :deep(.el-tabs__item:hover) {
  background-color: rgba(106, 1, 94, 0.1);
}

.order-tabs :deep(.el-tabs__item.is-active) {
  background-color: var(--primary-color);
  color: white;
}

.order-tabs :deep(.el-tabs__active-bar) {
  display: none; /* 隐藏默认的活动指示条 */
}

.order-card {
  background: transparent; /* 透明外层卡片 */
  margin-bottom: 24px;
  border: none; /* 移除边框 */
  box-shadow: none; /* 移除阴影 */
  overflow: visible;
  transition: all 0.3s ease;
  padding: 0; /* 移除padding */
  position: relative;
}

.order-card.action-dropdown-open {
  z-index: 1002; /* 低于 header 的 99999 */
}

.order-card:hover {
  transform: translateY(-2px);
}

.order-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 20px;
  background: transparent; /* 透明背景 */
}

.order-id-pill {
  background-color: transparent; /* 透明背景 */
  color: var(--primary-color); /* 主题色文字 */
  border: 1px solid var(--primary-color); /* 主题色边框 */
  padding: 6px 16px;
  border-radius: 20px; /* 药丸形 */
  font-size: 14px;
  font-weight: normal;
  transition: all 0.3s ease;
}

.order-id-pill:hover {
  background-color: rgba(106, 1, 94, 0.05); /* 悬停时轻微填充 */
}

.order-time {
  color: var(--primary-color);
  font-size: 14px;
  font-weight: normal;
}

/* status-pill 已替换为 UnifiedTag（在 OrderCard 组件中），样式由 UnifiedTag 统一管理（border-radius: 9999px） */

.order-content {
  padding: 20px;
  background: transparent; /* 透明背景 */
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.commodity-info {
  display: flex;
  gap: 15px;
  flex: 1;
}

.commodity-image {
  width: 100px;
  height: 100px;
  border-radius: 12px; /* 圆角图片 */
  overflow: hidden;
  background: #f5f5f5;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.commodity-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.no-image {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 100%;
  background: #f0f0f0;
  color: #999;
  font-size: 12px;
}

.commodity-details {
  flex: 1;
}

.commodity-title {
  font-size: 16px;
  font-weight: normal;
  margin-bottom: 8px;
  color: #333;
}

.commodity-price {
  font-size: 18px;
  font-weight: normal;
  color: var(--primary-color);
  margin-bottom: 5px;
}

.commodity-quantity {
  font-size: 14px;
  color: #666;
}

.commodity-location {
  font-size: 14px;
  color: #666;
  margin: 2px 0;
}

.seller-info {
  margin: 5px 0;
}

.seller-name {
  font-size: 14px;
  color: #333;
  font-weight: normal;
  margin: 2px 0;
}

.seller-contact {
  font-size: 12px;
  color: #666;
  margin: 1px 0;
}

.snapshot-time {
  font-size: 11px;
  color: #999;
  margin: 1px 0;
  font-style: italic;
}

.order-actions {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 15px;
}

.order-summary {
  text-align: right;
}

.total-price {
  font-size: 18px;
  font-weight: normal;
  color: var(--primary-color);
  margin: 0;
}

.action-buttons {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  justify-content: flex-end;
}

/* border-radius 由 UnifiedButton 统一管理（9999px） */
.action-buttons .unified-button :deep(.el-button) {
  font-weight: normal;
  margin-left: 0 !important; /* 删除左边距 */
}

/* 翻页器样式已移至 styles/pagination.css */

@media (max-width: 900px) {
  .nav-content {
    flex-direction: column;
    gap: 15px;
  }
  
  .nav-menu {
    gap: 20px;
  }
  
  .page-header {
    flex-direction: column;
    gap: 15px;
    text-align: center;
  }
  
  /* 移动端订单号弹出窗口 */
  .order-id-pill-wrapper {
    position: relative;
  }
  
  .order-id-pill {
    cursor: pointer;
    user-select: none;
    display: inline-flex;
    align-items: center;
    gap: 4px;
  }
  
  .order-id-icon {
    font-size: 10px;
    transition: transform 0.3s ease;
  }
  
  .order-id-icon.arrow-up {
    transform: rotate(180deg);
  }
  
  .order-id-popup {
    position: absolute;
    top: 100%;
    left: 0;
    z-index: 1000;
    margin-top: 4px;
    background: white;
    border: 1px solid var(--primary-color);
    border-radius: 16px;
    box-shadow: 0 4px 16px rgba(106, 1, 94, 0.2);
    padding: 12px 16px;
    min-width: 200px;
    white-space: nowrap;
  }
  
  .order-id-popup-content {
    color: var(--primary-color);
    font-size: 14px;
    font-weight: normal;
    word-break: break-all;
  }
  
  /* 移动端订单内容区域布局 */
  .order-content {
    flex-direction: column;
    align-items: stretch;
    gap: 15px;
  }
  
  /* 移动端订单操作区域和按钮布局 */
  .order-actions {
    flex-direction: row;
    align-items: center;
    justify-content: space-between;
    gap: 15px;
  }
  
  .order-summary {
    text-align: left;
  }
  
  .action-buttons {
    flex-direction: row;
    flex-wrap: wrap;
    justify-content: flex-end;
    flex: 1;
  }
  
  /* 移动端按钮统一高度 */
  .action-buttons .unified-button :deep(.el-button) {
    height: 36px;
    min-width: 100px;
  }
  
  /* 桌面端隐藏操作选择器 */
  .action-select-mobile {
    display: none;
  }
}

/* 桌面端隐藏操作选择器 */
.action-select-mobile {
  display: none;
}

/* 移动端显示操作选择器 */
@media (max-width: 900px) {
  .action-btn-desktop {
    display: none !important;
  }
  
  .action-select-mobile {
    position: relative;
    z-index: 1001;
    display: flex;
    align-items: center;
    justify-content: center;
    padding: 8px 16px;
    background: white;
    border: 1px solid var(--primary-color);
    border-radius: 20px;
    cursor: pointer;
    transition: all 0.3s ease;
    width: 120px;
    height: 36px;
  }
  
  .action-select-mobile:hover {
    border-color: var(--primary-light);
    box-shadow: 0 4px 12px rgba(106, 1, 94, 0.15);
  }
  
  .action-select-mobile .select-label {
    color: var(--primary-color);
    font-size: 14px;
    font-weight: normal;
    white-space: nowrap;
  }
  
  .action-select-mobile .select-icon {
    color: var(--primary-color);
    font-size: 12px;
    margin-left: 6px;
    transition: transform 0.3s ease;
  }
  
  /* 弹出式选择器 */
  .action-select-mobile .select-popup {
    position: absolute;
    top: calc(100% + 4px);
    right: 0;
    z-index: 9999; /* 低于 header 的 99999 */
    background: white;
    border: 1px solid var(--primary-color);
    border-radius: 20px;
    box-shadow: 0 4px 16px rgba(106, 1, 94, 0.2);
    min-width: 120px;
    overflow: hidden;
  }
  
  .action-select-mobile .popup-option {
    padding: 8px 16px;
    cursor: pointer;
    transition: background-color 0.2s ease;
    font-size: 14px;
    color: var(--text-primary);
    border-bottom: 1px solid #f0f0f0;
  }
  
  .action-select-mobile .popup-option:last-child {
    border-bottom: none;
  }
  
  .action-select-mobile .popup-option:hover {
    background-color: rgba(106, 1, 94, 0.1);
  }
  
  /* 删除选项使用红色 */
  .action-select-mobile .popup-option-delete {
    color: #f56c6c;
  }
  
  .action-select-mobile .popup-option-delete:hover {
    background-color: rgba(245, 108, 108, 0.1);
  }
  
  /* 再下一单选项使用绿色 */
  .action-select-mobile .popup-option-success {
    color: #67c23a;
  }
  
  .action-select-mobile .popup-option-success:hover {
    background-color: rgba(103, 194, 58, 0.1);
  }
  
  /* 申请退款选项使用橙色 */
  .action-select-mobile .popup-option-warning {
    color: #e6a23c;
  }
  
  .action-select-mobile .popup-option-warning:hover {
    background-color: rgba(230, 162, 60, 0.1);
  }
  
  /* 立即支付选项使用蓝色 */
  .action-select-mobile .popup-option-primary {
    color: var(--primary-color);
  }
  
  .action-select-mobile .popup-option-primary:hover {
    background-color: rgba(106, 1, 94, 0.1);
  }
}
</style>
