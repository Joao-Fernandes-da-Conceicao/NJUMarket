<template>
  <div class="orders-page">
    <!-- 订单内容 -->
    <div class="orders-content">
      <div class="container">
        <div class="page-header">
          <h1>我的订单</h1>
          <div class="header-actions">
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
            <el-tab-pane label="待支付" name="PENDING_PAYMENT"></el-tab-pane>
            <el-tab-pane label="待发货" name="PENDING_SHIPMENT"></el-tab-pane>
            <el-tab-pane label="待收货" name="SHIPPED"></el-tab-pane>
            <el-tab-pane label="已完成" name="COMPLETED"></el-tab-pane>
            <el-tab-pane label="已取消" name="CANCELLED"></el-tab-pane>
            <el-tab-pane label="退款中" name="REFUNDED"></el-tab-pane>
            <el-tab-pane label="退货中" name="RETURNED"></el-tab-pane>
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
              <div class="order-info">
                <span class="order-id">订单号：{{ order.orderId }}</span>
                <span class="order-time">{{ formatTime(order.createTime) }}</span>
              </div>
              <div class="order-status">
                <el-tag :type="getStatusType(order.orderStatus)">
                  {{ getStatusText(order.orderStatus) }}
                </el-tag>
              </div>
            </div>

            <div class="order-content">
              <div class="commodity-info">
                <div class="commodity-image">
                  <img 
                    v-if="order.commodity?.images && order.commodity.images.length > 0"
                    :src="getCommodityImageUrl(order.commodity.images[0])"
                    :alt="order.commodity?.title"
                  />
                  <div v-else class="no-image">
                    <span>暂无照片</span>
                  </div>
                </div>
                <div class="commodity-details">
                  <h3 class="commodity-title">{{ order.commodity?.title }}</h3>
                  <p class="commodity-price">¥{{ order.payAmount }}</p>
                  <p class="commodity-quantity">数量：{{ order.quantity }}</p>
                </div>
              </div>

              <div class="order-actions">
                <div class="order-summary">
                  <p class="total-price">总计：¥{{ order.payAmount }}</p>
                </div>
                <div class="action-buttons">
                  <el-button
                    v-if="order.orderStatus === 'PENDING_PAYMENT'"
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
                    v-if="['PENDING_PAYMENT', 'PENDING_SHIPMENT'].includes(order.orderStatus)"
                    @click="handleCancel(order.orderId)"
                  >
                    取消订单
                  </el-button>
                  <el-button
                    v-if="order.orderStatus === 'COMPLETED'"
                    @click="handleReturn(order.orderId)"
                  >
                    申请退货
                  </el-button>
                  <el-dropdown @command="(command) => handleVisibilityChange(order.orderId, command)">
                    <el-button>
                      可见性<el-icon><ArrowDown /></el-icon>
                    </el-button>
                    <template #dropdown>
                      <el-dropdown-menu>
                        <el-dropdown-item command="VISIBLE">完全可见</el-dropdown-item>
                        <el-dropdown-item command="SELLER_ONLY">仅卖家可见</el-dropdown-item>
                        <el-dropdown-item command="BUYER_ONLY">仅买家可见</el-dropdown-item>
                        <el-dropdown-item command="HIDDEN">隐藏</el-dropdown-item>
                      </el-dropdown-menu>
                    </template>
                  </el-dropdown>
                  <el-button @click="viewOrderDetail(order.orderId)">
                    查看详情
                  </el-button>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- 分页 -->
        <div class="pagination-wrapper" v-if="total > 0">
          <el-pagination
            v-model:current-page="currentPage"
            v-model:page-size="pageSize"
            :page-sizes="[10, 20, 50]"
            :total="total"
            layout="total, sizes, prev, pager, next, jumper"
            @size-change="handleSizeChange"
            @current-change="handleCurrentChange"
          />
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
    
    const user = ref(userStore.user)
    
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
        if (response.success) {
          orders.value = response.data
          total.value = response.total || 0
        }
      } catch (error) {
        ElMessage.error('获取订单列表失败')
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
    
    // 申请退货
    const handleReturn = async (orderId) => {
      try {
        const returnReason = prompt('请输入退货原因')
        if (returnReason === null) {
          // 用户取消输入
          return
        }
        if (!returnReason.trim()) {
          ElMessage.error('请输入退货原因')
          return
        }
        
        const response = await orderAPI.requestReturn(orderId, returnReason)
        if (response.success) {
          ElMessage.success('退货申请已提交')
          fetchOrders()
        } else {
          ElMessage.error(response.errorMsg || '申请退货失败')
        }
      } catch (error) {
        ElMessage.error('申请退货失败')
      }
    }
    
    // 查看订单详情
    const viewOrderDetail = () => {
      // 这里可以打开订单详情对话框或跳转到详情页面
      ElMessage.info('订单详情功能开发中...')
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
    
    // 获取状态类型
    const getStatusType = (status) => {
      const statusMap = {
        'PENDING_PAYMENT': 'warning',
        'PENDING_SHIPMENT': 'info',
        'SHIPPED': 'primary',
        'COMPLETED': 'success',
        'CANCELLED': 'danger',
        'REFUNDED': 'warning',
        'RETURNED': 'warning'
      }
      return statusMap[status] || 'info'
    }
    
    // 获取状态文本
    const getStatusText = (status) => {
      const statusMap = {
        'PENDING_PAYMENT': '待支付',
        'PENDING_SHIPMENT': '待发货',
        'SHIPPED': '待收货',
        'COMPLETED': '已完成',
        'CANCELLED': '已取消',
        'REFUNDED': '退款中',
        'RETURNED': '退货中'
      }
      return statusMap[status] || status
    }
    
    // 获取空状态描述
    const getEmptyDescription = () => {
      const statusMap = {
        'all': '暂无订单',
        'PENDING_PAYMENT': '暂无待支付订单',
        'PENDING_SHIPMENT': '暂无待发货订单',
        'SHIPPED': '暂无待收货订单',
        'COMPLETED': '暂无已完成订单',
        'CANCELLED': '暂无已取消订单',
        'REFUNDED': '暂无退款订单',
        'RETURNED': '暂无退货订单'
      }
      return statusMap[activeTab.value] || '暂无订单'
    }
    
    // 获取空状态操作按钮文本
    const getEmptyActionText = () => {
      const actionMap = {
        'all': '去购物',
        'PENDING_PAYMENT': '去购物',
        'PENDING_SHIPMENT': '去购物',
        'SHIPPED': '去购物',
        'COMPLETED': '去购物',
        'CANCELLED': '去购物',
        'REFUNDED': '去购物',
        'RETURNED': '去购物'
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
      user,
      handleTabChange,
      handleSizeChange,
      handleCurrentChange,
      handlePay,
      handleConfirm,
      handleCancel,
      handleReturn,
      viewOrderDetail,
      handleVisibilityChange,
      getStatusType,
      getStatusText,
      getEmptyDescription,
      getEmptyActionText,
      handleEmptyAction,
      formatTime,
      getCommodityImageUrl,
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

.orders-content {
  padding: 30px 0;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 30px;
}

.page-header h1 {
  font-size: 28px;
  font-weight: bold;
  color: #333;
  margin: 0;
}

.order-tabs {
  background: white;
  border-radius: 8px;
  padding: 20px;
  margin-bottom: 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.orders-list {
  margin-bottom: 30px;
}

.empty-state {
  text-align: center;
  padding: 60px 0;
}

.order-card {
  background: white;
  border-radius: 8px;
  margin-bottom: 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  overflow: hidden;
}

.order-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 15px 20px;
  background: #f8f9fa;
  border-bottom: 1px solid #e0e0e0;
}

.order-info {
  display: flex;
  gap: 20px;
}

.order-id {
  font-weight: 600;
  color: #333;
}

.order-time {
  color: #999;
  font-size: 14px;
}

.order-content {
  padding: 20px;
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
  width: 80px;
  height: 80px;
  border-radius: 6px;
  overflow: hidden;
  background: #f5f5f5;
  display: flex;
  align-items: center;
  justify-content: center;
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
  font-weight: 600;
  margin-bottom: 8px;
  color: #333;
}

.commodity-price {
  font-size: 18px;
  font-weight: bold;
  color: var(--primary-color);
  margin-bottom: 5px;
}

.commodity-quantity {
  font-size: 14px;
  color: #666;
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
  font-weight: bold;
  color: var(--primary-color);
  margin: 0;
}

.action-buttons {
  display: flex;
  gap: 10px;
}

.pagination-wrapper {
  display: flex;
  justify-content: center;
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
