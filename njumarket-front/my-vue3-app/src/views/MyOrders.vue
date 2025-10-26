<template>
  <div class="orders-page">
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
        <div class="order-tabs">
          <SafeTabs 
            v-model="activeTab" 
            @tab-change="handleTabChange"
          >
            <el-tab-pane label="全部" name="all"></el-tab-pane>
            <el-tab-pane label="待支付" name="CREATED"></el-tab-pane>
            <el-tab-pane label="待发货" name="PAID"></el-tab-pane>
            <el-tab-pane label="待收货" name="SHIPPED"></el-tab-pane>
            <el-tab-pane label="已完成" name="COMPLETED"></el-tab-pane>
            <el-tab-pane label="已取消" name="CANCELLED"></el-tab-pane>
            <el-tab-pane label="退款中" name="REFUND_REQUESTED"></el-tab-pane>
            <el-tab-pane label="退款完成" name="REFUND_APPROVED"></el-tab-pane>
            <el-tab-pane label="退款被拒" name="REFUND_REJECTED"></el-tab-pane>
          </SafeTabs>
        </div>

        <!-- 订单列表 -->
        <div class="orders-list" v-loading="loading">
          <div v-if="orders.length === 0 && !loading" class="empty-state">
            <el-empty :description="getEmptyDescription()">
              <el-button type="primary" @click="handleEmptyAction()">
                {{ getEmptyActionText() }}
              </el-button>
            </el-empty>
          </div>

          <div v-for="order in orders" :key="order.orderId" class="order-card">
            <div class="order-header">
              <span class="order-id-pill">订单号：{{ order.orderId }}</span>
              <span class="order-time">{{ formatTime(order.createTime) }}</span>
              <span class="status-pill" :class="'status-' + order.orderStatus">
                {{ getStatusText(order.orderStatus) }}
              </span>
            </div>

            <div class="order-content">
              <div class="commodity-info">
                <div class="commodity-image">
                  <!-- 优先使用商品快照图片，如果没有则使用商品图片 -->
                  <img 
                    v-if="getCommoditySnapshotImage(order) || (order.commodity?.images && order.commodity.images.length > 0)"
                    :src="getCommoditySnapshotImage(order) || getCommodityImageUrl(order.commodity.images[0])"
                    :alt="getCommoditySnapshotTitle(order) || order.commodity?.title"
                  />
                  <div v-else class="no-image">
                    <span>暂无照片</span>
                  </div>
                </div>
                <div class="commodity-details">
                  <h3 class="commodity-title">
                    {{ getCommoditySnapshotTitle(order) || order.commodity?.title }}
                    <!-- 显示商品快照状态提示 -->
                    <el-tag 
                      v-if="isCommoditySnapshotOffShelf(order)" 
                      type="warning" 
                      size="small"
                      style="margin-left: 8px;"
                    >
                      已下架
                    </el-tag>
                  </h3>
                  <p class="commodity-price">¥{{ order.payAmount }}</p>
                  <p class="commodity-quantity">数量：{{ order.quantity }}</p>
                  <!-- 显示商品快照信息 -->
                  <p v-if="order.commoditySnapshotLocation" class="commodity-location">
                    位置：{{ order.commoditySnapshotLocation }}
                  </p>
                  <div v-if="order.commoditySnapshotSellerName" class="seller-info">
                    <p class="seller-name">卖家：{{ order.commoditySnapshotSellerName }}</p>
                    <p v-if="order.commoditySnapshotSellerPhone" class="seller-contact">
                      电话：{{ order.commoditySnapshotSellerPhone }}
                    </p>
                    <p v-if="order.commoditySnapshotSellerEmail" class="seller-contact">
                      邮箱：{{ order.commoditySnapshotSellerEmail }}
                    </p>
                    <p v-if="order.commoditySnapshotTime" class="snapshot-time">
                      快照时间：{{ formatTime(order.commoditySnapshotTime) }}
                    </p>
                  </div>
                </div>
              </div>

              <div class="order-actions">
                <div class="order-summary">
                  <p class="total-price">总计：¥{{ order.payAmount }}</p>
                </div>
                <div class="action-buttons">
                  <el-button
                    v-if="order.orderStatus === 'CREATED'"
                    type="primary"
                    @click="handlePay(order.orderId)"
                  >
                    立即支付
                  </el-button>
                  <el-button
                    v-if="order.orderStatus === 'SHIPPED'"
                    type="success"
                    @click="handleConfirm(order.orderId)"
                  >
                    确认收货
                  </el-button>
                  <el-button
                    v-if="['CREATED', 'PAID'].includes(order.orderStatus)"
                    @click="handleCancel(order.orderId)"
                  >
                    取消订单
                  </el-button>
                  <el-button
                    v-if="order.orderStatus === 'COMPLETED'"
                    @click="handleRefund(order.orderId)"
                  >
                    申请退款
                  </el-button>
                  <el-button
                    v-if="order.orderStatus === 'REFUND_REJECTED'"
                    type="warning"
                    @click="handleRefund(order.orderId)"
                  >
                    重新申请退款
                  </el-button>
                  <!-- 查询商品按钮 -->
                  <el-button
                    v-if="canQueryCommodity(order)"
                    @click="handleQueryCommodity(order.orderId)"
                  >
                    查询商品
                  </el-button>
                  
                  <!-- 再下一单按钮 -->
                  <el-button
                    v-if="canCreateNewOrder(order)"
                    type="success"
                    @click="handleCreateNewOrder(order.orderId)"
                  >
                    再下一单
                  </el-button>
                  <el-button @click="viewOrderDetail(order.orderId)">
                    查看详情
                  </el-button>
                  <el-button
                    v-if="canDeleteOrder(order)"
                    type="danger"
                    @click="handleDelete(order.orderId)"
                  >
                    删除
                  </el-button>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- 分页 -->
        <div class="pagination-wrapper" v-if="total > 0">
          <div class="pagination-content">
            <!-- 总数显示 -->
            <span class="pagination-total">共 {{ total }} 条</span>
            
            <!-- 每页显示数量选择器 -->
            <div class="custom-page-size-select" @click.stop="togglePageSizeSelect">
              <span class="select-label">每页</span>
              <span class="select-value">{{ pageSize }} 条</span>
              <el-icon class="select-icon"><ArrowDown /></el-icon>
              
              <!-- 弹出式选择器 -->
              <div v-if="showPageSizeSelect" class="select-popup">
                <div class="popup-option" @click="selectPageSize(10)">10 条/页</div>
                <div class="popup-option" @click="selectPageSize(20)">20 条/页</div>
                <div class="popup-option" @click="selectPageSize(50)">50 条/页</div>
              </div>
            </div>
            
            <!-- 翻页按钮 -->
            <div class="pagination-buttons">
              <button 
                class="page-btn" 
                :disabled="currentPage <= 1"
                @click="handleCurrentChange(currentPage - 1)"
              >
                上一页
              </button>
              
              <div class="page-numbers">
                <button 
                  v-for="page in getPageNumbers()" 
                  :key="page"
                  class="page-number"
                  :class="{ active: page === currentPage }"
                  @click="page !== '...' && handleCurrentChange(page)"
                  :disabled="page === '...'"
                >
                  {{ page }}
                </button>
              </div>
              
              <button 
                class="page-btn" 
                :disabled="currentPage >= getTotalPages()"
                @click="handleCurrentChange(currentPage + 1)"
              >
                下一页
              </button>
            </div>
            
            <!-- 跳转输入框 -->
            <div class="page-jumper" @click.stop>
              <span class="jumper-label">跳至</span>
              <input 
                v-model="jumpPage" 
                type="number" 
                class="jumper-input"
                :min="1" 
                :max="getTotalPages()"
                @keyup.enter="handleJump"
                @click.stop
              />
              <span class="jumper-label">页</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '../stores/user'
import { orderAPI, imageAPI } from '../api'
import { ElMessage } from 'element-plus'
//import { ArrowDown } from '@element-plus/icons-vue'
import SafeTabs from '../components/SafeTabs.vue'

export default {
  name: 'MyOrders',
  components: {
    SafeTabs
  },
  setup() {
    const router = useRouter()
    const userStore = useUserStore()
    
    const loading = ref(false)
    const orders = ref([])
    const total = ref(0)
    const currentPage = ref(1)
    const pageSize = ref(10)
    const activeTab = ref('all')
    const isMounted = ref(false)
    
    // 翻页器相关
    const showPageSizeSelect = ref(false)
    const jumpPage = ref('')
    
    const user = ref(userStore.user)
    const isLoggedIn = ref(userStore.isLoggedIn)
    
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
    
    // 翻页器相关方法
    const togglePageSizeSelect = () => {
      showPageSizeSelect.value = !showPageSizeSelect.value
    }
    
    const selectPageSize = (size) => {
      pageSize.value = size
      currentPage.value = 1
      showPageSizeSelect.value = false
      fetchOrders()
    }
    
    const getTotalPages = () => {
      return Math.ceil(total.value / pageSize.value)
    }
    
    const getPageNumbers = () => {
      const totalPages = getTotalPages()
      const current = currentPage.value
      const pages = []
      
      if (totalPages <= 7) {
        for (let i = 1; i <= totalPages; i++) {
          pages.push(i)
        }
      } else {
        if (current <= 4) {
          for (let i = 1; i <= 5; i++) {
            pages.push(i)
          }
          pages.push('...')
          pages.push(totalPages)
        } else if (current >= totalPages - 3) {
          pages.push(1)
          pages.push('...')
          for (let i = totalPages - 4; i <= totalPages; i++) {
            pages.push(i)
          }
        } else {
          pages.push(1)
          pages.push('...')
          for (let i = current - 1; i <= current + 1; i++) {
            pages.push(i)
          }
          pages.push('...')
          pages.push(totalPages)
        }
      }
      
      return pages
    }
    
    const handleJump = () => {
      const page = parseInt(jumpPage.value)
      const totalPages = getTotalPages()
      
      if (!jumpPage.value || isNaN(page)) {
        ElMessage.warning('请输入有效的页码')
        jumpPage.value = ''
        return
      }
      
      if (page < 1) {
        ElMessage.warning('页码不能小于1')
        jumpPage.value = ''
        return
      }
      
      if (page > totalPages) {
        ElMessage.warning(`页码不能大于总页数 ${totalPages}`)
        jumpPage.value = ''
        return
      }
      
      handleCurrentChange(page)
      jumpPage.value = ''
    }
    
    // 支付订单
    const handlePay = async (orderId) => {
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
          
          // 构建商品信息显示
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
          
          if (data.commodityExists && data.commodityOnShelf) {
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
    
    onMounted(() => {
      isMounted.value = true
      fetchOrders()
    })
    
    // 组件卸载时清理
    onUnmounted(() => {
      isMounted.value = false
    })
    
    return {
      loading,
      orders,
      total,
      currentPage,
      pageSize,
      activeTab,
      showPageSizeSelect,
      jumpPage,
      user,
      isLoggedIn,
      handleTabChange,
      handleSizeChange,
      handleCurrentChange,
      togglePageSizeSelect,
      selectPageSize,
      getTotalPages,
      getPageNumbers,
      handleJump,
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

.prompt-actions .el-button {
  border-radius: 24px; /* 药丸形状设计 */
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

.page-header h1 {
  font-size: 28px;
  font-weight: normal;
  color: var(--primary-color); /* 主题色标题 */
  margin: 0;
}

.header-actions {
  display: flex;
  gap: 12px;
}

.header-actions .el-button {
  border-radius: 20px; /* 药丸形按钮 */
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
  transition: all 0.3s ease;
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

.status-pill {
  background-color: #f0f0f0;
  color: #666;
  padding: 6px 16px;
  border-radius: 20px; /* 药丸形状态标签 */
  font-size: 14px;
  font-weight: normal;
}

/* 不同状态的颜色 */
.status-pill.status-CREATED {
  background-color: #fef0f0;
  color: #f56c6c;
}

.status-pill.status-PAID {
  background-color: #f0f9ff;
  color: #409eff;
}

.status-pill.status-SHIPPED {
  background-color: #f0f9ff;
  color: #409eff;
}

.status-pill.status-COMPLETED {
  background-color: #f0f9ff;
  color: #67c23a;
}

.status-pill.status-CANCELLED {
  background-color: #f5f5f5;
  color: #909399;
}

.status-pill.status-REFUND_REQUESTED,
.status-pill.status-REFUND_APPROVED,
.status-pill.status-REFUND_REJECTED {
  background-color: #fdf6ec;
  color: #e6a23c;
}

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

.action-buttons .el-button {
  border-radius: 20px; /* 药丸形按钮 */
  font-weight: normal;
  margin-left: 0 !important; /* 删除左边距 */
}

.pagination-wrapper {
  display: flex;
  justify-content: center;
  margin-top: 40px;
}

.pagination-content {
  display: flex;
  align-items: center;
  gap: 20px;
  flex-wrap: wrap;
  justify-content: center;
}

.pagination-total {
  color: var(--primary-color);
  font-size: 14px;
  font-weight: normal;
}

/* 每页显示数量选择器 */
.custom-page-size-select {
  position: relative;
  display: flex;
  align-items: center;
  padding: 8px 16px;
  background: white;
  border: 1px solid var(--primary-color);
  border-radius: 20px;
  cursor: pointer;
  transition: all 0.3s ease;
  min-width: 120px;
}

.custom-page-size-select:hover {
  box-shadow: 0 4px 12px rgba(106, 1, 94, 0.15);
}

.custom-page-size-select .select-label {
  color: var(--primary-color);
  font-size: 14px;
  font-weight: normal;
  margin-right: 8px;
}

.custom-page-size-select .select-value {
  flex: 1;
  color: var(--text-primary);
  font-size: 14px;
  font-weight: normal;
}

.custom-page-size-select .select-icon {
  color: var(--primary-color);
  font-size: 12px;
  margin-left: 8px;
  transition: transform 0.3s ease;
}

.custom-page-size-select:hover .select-icon {
  transform: rotate(180deg);
}

/* 弹出式选择器 */
.select-popup {
  position: absolute;
  top: 100%;
  left: 0;
  right: 0;
  background: white;
  border: 1px solid var(--primary-color);
  border-radius: 16px;
  box-shadow: 0 4px 16px rgba(106, 1, 94, 0.2);
  z-index: 1000;
  margin-top: 4px;
  padding: 8px;
}

.popup-option {
  padding: 8px 12px;
  margin: 2px 0;
  border-radius: 20px;
  color: var(--primary-color);
  font-size: 14px;
  cursor: pointer;
  transition: all 0.3s ease;
  text-align: center;
}

.popup-option:hover {
  background-color: var(--primary-color);
  color: white;
}

/* 翻页按钮组 */
.pagination-buttons {
  display: flex;
  align-items: center;
  gap: 8px;
}

.page-btn {
  background-color: transparent;
  border: 1px solid var(--primary-color);
  color: var(--primary-color);
  border-radius: 20px;
  padding: 8px 16px;
  font-size: 14px;
  font-weight: normal;
  cursor: pointer;
  transition: all 0.3s ease;
  min-width: 80px;
}

.page-btn:hover:not(:disabled) {
  background-color: var(--primary-color);
  color: white;
}

.page-btn:disabled {
  background-color: transparent;
  color: #ccc;
  border-color: #ccc;
  cursor: not-allowed;
}

/* 页码按钮 */
.page-numbers {
  display: flex;
  align-items: center;
  gap: 4px;
}

.page-number {
  background-color: transparent;
  border: 1px solid var(--primary-color);
  color: var(--primary-color);
  border-radius: 20px;
  width: 32px;
  height: 32px;
  font-size: 14px;
  font-weight: normal;
  cursor: pointer;
  transition: all 0.3s ease;
  display: flex;
  align-items: center;
  justify-content: center;
}

.page-number:hover:not(:disabled) {
  background-color: var(--primary-color);
  color: white;
}

.page-number.active {
  background-color: var(--primary-color);
  color: white;
}

.page-number:disabled {
  border-color: transparent;
  color: #999;
  cursor: default;
}

/* 跳转输入框 */
.page-jumper {
  display: flex;
  align-items: center;
  gap: 8px;
}

.jumper-label {
  color: var(--primary-color);
  font-size: 14px;
  font-weight: normal;
}

.jumper-input {
  background-color: transparent;
  border: 1px solid var(--primary-color);
  color: var(--primary-color);
  border-radius: 20px;
  padding: 8px 12px;
  width: 60px;
  height: 32px;
  font-size: 14px;
  text-align: center;
  transition: all 0.3s ease;
}

.jumper-input:hover,
.jumper-input:focus {
  background-color: var(--primary-color);
  color: white;
  outline: none;
}

@media (max-width: 768px) {
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
  
  .order-content {
    flex-direction: column;
    gap: 20px;
  }
  
  .order-actions {
    align-items: center;
  }
  
  .action-buttons {
    flex-wrap: wrap;
    justify-content: center;
  }
}
</style>
