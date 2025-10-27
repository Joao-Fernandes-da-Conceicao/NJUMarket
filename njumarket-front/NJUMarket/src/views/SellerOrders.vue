<template>
  <div class="seller-orders-page" @click="handlePageClick">
    <!-- 订单内容 -->
    <div class="orders-content">
      <div class="container">
        <div class="page-header">
          <h1>卖家订单</h1>
          <div class="header-actions">
            <UnifiedButton type="info" @click="$router.push('/orders')">
              买家订单
            </UnifiedButton>
            <UnifiedButton type="primary" @click="$router.push('/home')">
              管理商品
            </UnifiedButton>
          </div>
        </div>

        <!-- 订单状态筛选 -->
        <OrderTabs 
          v-model="activeTab" 
          @tab-change="handleTabChange"
          :refund-label="'退款申请'"
          :show-refund-rejected="true"
        />

        <!-- 订单列表 -->
        <div class="orders-list" v-loading="loading">
          <div v-if="orders.length === 0 && !loading" class="empty-state">
            <el-empty :description="getEmptyDescription()">
              <UnifiedButton type="primary" @click="handleEmptyAction()">
                {{ getEmptyActionText() }}
              </UnifiedButton>
            </el-empty>
          </div>

          <OrderCard
            v-for="order in orders"
            :key="order.orderId"
            :order="order"
            type="seller"
            :class="{ 'action-dropdown-open': showActionSelectId === order.orderId }"
            @order-id-click="showOrderIdDetails"
          >
            <template #actions>
              <!-- 卖家发货按钮 -->
              <UnifiedButton
                v-if="canShip(order)"
                type="primary"
                class="action-btn-desktop"
                @click="handleShip(order.orderId)"
              >
                发货
              </UnifiedButton>

              <!-- 卖家取消订单按钮 -->
              <UnifiedButton
                v-if="canCancel(order)"
                class="action-btn-desktop"
                @click="handleCancel(order.orderId)"
              >
                取消订单
              </UnifiedButton>
              
              <!-- 处理退款申请按钮 -->
              <UnifiedButton
                v-if="order.orderStatus === 'REFUND_REQUESTED'"
                type="success"
                class="action-btn-desktop"
                @click="handleApproveRefund(order.orderId)"
              >
                同意退款
              </UnifiedButton>
              <UnifiedButton
                v-if="order.orderStatus === 'REFUND_REQUESTED'"
                type="danger"
                class="action-btn-desktop"
                @click="handleRejectRefund(order.orderId)"
              >
                拒绝退款
              </UnifiedButton>
              
              <!-- 桌面端：查看和删除按钮 -->
              <UnifiedButton 
                class="action-btn-desktop"
                @click="viewOrderDetail(order.orderId)"
              >
                查看详情
              </UnifiedButton>
              <UnifiedButton
                v-if="canDeleteOrder(order)"
                type="danger"
                class="action-btn-desktop"
                @click="handleDelete(order.orderId)"
              >
                删除
              </UnifiedButton>
              
              <!-- 移动端：操作选择器 -->
              <div class="action-select-mobile" @click.stop="toggleActionSelect(order.orderId)">
                <span class="select-label">操作</span>
                <el-icon class="select-icon"><ArrowDown /></el-icon>
                
                <!-- 弹出式选择器 -->
                <div v-if="showActionSelectId === order.orderId" class="select-popup" @click.stop>
                  <!-- 卖家状态相关操作 -->
                  <div class="popup-option popup-option-primary" v-if="canShip(order)" @click.stop="handleShip(order.orderId); showActionSelectId = null">
                    发货
                  </div>
                  <div class="popup-option" v-if="canCancel(order)" @click.stop="handleCancel(order.orderId); showActionSelectId = null">
                    取消订单
                  </div>
                  <div class="popup-option popup-option-success" v-if="order.orderStatus === 'REFUND_REQUESTED'" @click.stop="handleApproveRefund(order.orderId); showActionSelectId = null">
                    同意退款
                  </div>
                  <div class="popup-option popup-option-danger" v-if="order.orderStatus === 'REFUND_REQUESTED'" @click.stop="handleRejectRefund(order.orderId); showActionSelectId = null">
                    拒绝退款
                  </div>
                  
                  <!-- 其他操作 -->
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
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '../stores/user'
import { orderAPI, imageAPI } from '../api'
import { ElMessage, ElMessageBox } from 'element-plus'
import { canShipOrder, canCancelOrder } from '../utils/orderRules'
import { ArrowDown } from '@element-plus/icons-vue'
import OrderTabs from '../components/order/OrderTabs.vue'
import OrderCard from '../components/order/OrderCard.vue'
import Pagination from '../components/common/Pagination.vue'
import UnifiedButton from '../components/common/UnifiedButton.vue'

export default {
  name: 'SellerOrders',
  components: {
    OrderTabs,
    OrderCard,
    ArrowDown,
    Pagination,
    UnifiedButton
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
    
    const user = ref(userStore.user)
    
    // 统一规则封装为模板可用的方法
    const canShip = (o) => canShipOrder({ order: o, user: user.value }).canShip
    const canCancel = (o) => canCancelOrder({ order: o, user: user.value }).canCancel
    
    // (pagination 现在由组件管理)
    
    // 移动端订单号弹出窗口
    const showOrderIdPopup = ref(false)
    const currentOrderId = ref('')
    const showActionSelectId = ref(null)
    
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
      // 关闭订单号弹出窗口
      if (showOrderIdPopup.value) {
        const target = event.target
        if (!target.closest('.order-id-pill-wrapper')) {
          closeOrderIdPopup()
        }
      }
      
      // 关闭操作选择器
      showActionSelectId.value = null
      
      // 关闭所有选择器
    }
    
    // 获取卖家订单列表
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
        
        const response = await orderAPI.getSellerOrders(params.page, params.size, params.status)
        console.log('卖家订单查询响应:', response) // 调试日志
        
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
          console.warn('获取卖家订单列表失败:', response.errorMsg)
        }
      } catch (error) {
        console.error('获取卖家订单列表异常:', error)
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
    
    // 分页相关方法 (现在由 Pagination 组件处理)
    const getTotalPages = () => {
      return Math.ceil(total.value / pageSize.value)
    }
    
    // 发货
    const handleShip = async (orderId) => {
      const order = orders.value.find(o => o.orderId === orderId)
      if (!order) {
        ElMessage.error('订单不存在')
        return
      }
      
      const check = canShipOrder({ order, user: user.value })
      if (!check.canShip) {
        ElMessage.error(check.reason)
        return
      }
      
      try {
        const { value: trackingNumber } = await ElMessageBox.prompt('请输入快递单号', '发货', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          inputPattern: /.+/,
          inputErrorMessage: '快递单号不能为空'
        })
        
        const response = await orderAPI.ship(orderId, trackingNumber)
        if (response.success) {
          ElMessage.success('发货成功')
          fetchOrders()
        } else {
          ElMessage.error(response.errorMsg || '发货失败')
        }
      } catch (error) {
        if (error !== 'cancel') {
          ElMessage.error('发货失败')
        }
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
        await ElMessageBox.confirm('确定要取消这个订单吗？', '取消订单', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        })
        
        const response = await orderAPI.cancel(orderId, '卖家取消')
        if (response.success) {
          ElMessage.success('订单已取消')
          fetchOrders()
        } else {
          ElMessage.error(response.errorMsg || '取消订单失败')
        }
      } catch (error) {
        if (error !== 'cancel') {
          ElMessage.error('取消订单失败')
        }
      }
    }
    
    // 同意退款
    const handleApproveRefund = async (orderId) => {
      try {
        await ElMessageBox.confirm('确定要同意这个退款申请吗？', '同意退款', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        })
        
        const response = await orderAPI.handleRefund(orderId, 'APPROVE', '同意退款')
        if (response.success) {
          ElMessage.success('退款申请已同意')
          fetchOrders()
        } else {
          ElMessage.error(response.errorMsg || '处理退款申请失败')
        }
      } catch (error) {
        if (error !== 'cancel') {
          ElMessage.error('处理退款申请失败')
        }
      }
    }
    
    // 拒绝退款
    const handleRejectRefund = async (orderId) => {
      try {
        const { value: reason } = await ElMessageBox.prompt('请输入拒绝原因', '拒绝退款', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          inputPattern: /.+/,
          inputErrorMessage: '拒绝原因不能为空'
        })
        
        const response = await orderAPI.handleRefund(orderId, 'REJECT', reason)
        if (response.success) {
          ElMessage.success('退款申请已拒绝')
          fetchOrders()
        } else {
          ElMessage.error(response.errorMsg || '处理退款申请失败')
        }
      } catch (error) {
        if (error !== 'cancel') {
          ElMessage.error('处理退款申请失败')
        }
      }
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
          confirmMessage = '该订单存在退款等重要事件，删除后当卖家处理退款时，订单会重新可见。确定要继续吗？'
        }
        
        const confirmed = confirm(confirmMessage)
        if (!confirmed) {
          return
        }
        
        // 软删除：只设置卖家可见性为HIDDEN
        // 如果买家也删除了，则订单完全隐藏
        // 如果买家未删除，退款时，订单会重新可见
        const response = await orderAPI.updateSellerVisibility(orderId, 'HIDDEN')
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
    
    // 检查是否可以删除订单
    const canDeleteOrder = (order) => {
      // 允许删除的状态：已取消、已完成、退款完成
      const deletableStatuses = ['CANCELLED', 'COMPLETED', 'REFUND_APPROVED']
      return deletableStatuses.includes(order.orderStatus)
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
        'REFUND_REQUESTED': '退款申请',
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
        'REFUND_REQUESTED': '暂无退款申请',
        'REFUND_APPROVED': '暂无退款完成订单',
        'REFUND_REJECTED': '暂无退款被拒订单'
      }
      return statusMap[activeTab.value] || '暂无订单'
    }
    
    // 获取空状态操作按钮文本
    const getEmptyActionText = () => {
      return '管理商品'
    }
    
    // 处理空状态操作
    const handleEmptyAction = () => {
      router.push('/home')
    }
    
    // 查看订单详情
    const viewOrderDetail = (orderId) => {
      router.push(`/order/${orderId}`)
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
    
    onMounted(() => {
      fetchOrders()
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
      handleTabChange,
      handleSizeChange,
      handleCurrentChange,
      getTotalPages,
      toggleActionSelect,
      showActionSelectId,
      canShip,
      canCancel,
      handleShip,
      handleCancel,
      handleApproveRefund,
      handleRejectRefund,
      handleVisibilityChange,
      handleDelete,
      canDeleteOrder,
      viewOrderDetail,
      getStatusType,
      getStatusText,
      getEmptyDescription,
      getEmptyActionText,
      handleEmptyAction,
      formatTime,
      getCommodityImageUrl,
      getCommoditySnapshotTitle,
      getCommoditySnapshotImage,
      isCommoditySnapshotOffShelf
    }
  }
}
</script>

<style scoped>
.seller-orders-page {
  min-height: 100vh;
  background-color: #f5f5f5;
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
  border-radius: 28.3px; /* 与买家订单一致的药丸形头部 */
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

.orders-list {
  margin-bottom: 30px;
}

.empty-state {
  text-align: center;
  padding: 60px 0;
}

/* border-radius 由 UnifiedButton 统一管理（9999px） */

.order-card {
  background: transparent; /* 透明外层卡片 */
  padding: 0; /* 移除padding */
  margin-bottom: 24px;
  border: none; /* 移除边框 */
  box-shadow: none; /* 移除阴影 */
  overflow: visible;
  transition: all 0.3s ease;
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
  align-items: center;
  gap: 15px;
  flex: 1;
}

.commodity-image {
  width: 100px;
  height: 100px;
  border-radius: 12px; /* 圆角图片 */
  overflow: hidden;
  background-color: #f5f5f5;
  flex-shrink: 0;
}

.commodity-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.no-image {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #999;
  font-size: 12px;
}

.commodity-details {
  flex: 1;
}

.commodity-title {
  margin: 0 0 8px 0;
  font-size: 16px;
  font-weight: normal;
  color: #333;
}

.commodity-price {
  margin: 0 0 5px 0;
  color: #e74c3c;
  font-weight: normal;
  font-size: 18px;
}

.buyer-info {
  margin: 0;
  color: #666;
  font-size: 14px;
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
  margin: 0;
  font-size: 18px;
  font-weight: normal;
  color: #e74c3c;
}

.action-buttons {
  display: flex;
  gap: 10px;
  align-items: center;
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
  
  /* 弹出式选择器 - 样式参考翻页器的custom-page-size-select */
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
  
  /* 同意退款选项使用绿色 */
  .action-select-mobile .popup-option-success {
    color: #67c23a;
  }
  
  .action-select-mobile .popup-option-success:hover {
    background-color: rgba(103, 194, 58, 0.1);
  }
  
  /* 拒绝退款选项使用红色 */
  .action-select-mobile .popup-option-danger {
    color: #f56c6c;
  }
  
  .action-select-mobile .popup-option-danger:hover {
    background-color: rgba(245, 108, 108, 0.1);
  }
  
  /* 发货选项使用主题色 */
  .action-select-mobile .popup-option-primary {
    color: var(--primary-color);
  }
  
  .action-select-mobile .popup-option-primary:hover {
    background-color: rgba(106, 1, 94, 0.1);
  }
}
</style>
