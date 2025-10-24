<template>
  <div class="seller-orders-page">
    <!-- 订单内容 -->
    <div class="orders-content">
      <div class="container">
        <div class="page-header">
          <h1>我的订单（卖家）</h1>
          <div class="header-actions">
            <el-button type="primary" @click="$router.push('/commodities')">
              管理商品
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
            <el-tab-pane label="退款申请" name="REFUND_REQUESTED"></el-tab-pane>
            <el-tab-pane label="退款完成" name="REFUND_APPROVED"></el-tab-pane>
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
                  <p class="buyer-info">买家：{{ order.buyer?.nickname || '用户' + order.buyerId }}</p>
                </div>
              </div>

              <div class="order-actions">
                <div class="order-summary">
                  <p class="total-price">总计：¥{{ order.payAmount }}</p>
                </div>
                <div class="action-buttons">
                  <!-- 卖家发货按钮 -->
                  <el-button
                    v-if="order.orderStatus === 'PAID'"
                    type="primary"
                    @click="handleShip(order.orderId)"
                  >
                    发货
                  </el-button>
                  
                  <!-- 卖家取消订单按钮 -->
                  <el-button
                    v-if="['CREATED', 'PAID'].includes(order.orderStatus)"
                    @click="handleCancel(order.orderId)"
                  >
                    取消订单
                  </el-button>
                  
                  <!-- 处理退款申请按钮 -->
                  <el-button
                    v-if="order.orderStatus === 'REFUND_REQUESTED'"
                    type="success"
                    @click="handleApproveRefund(order.orderId)"
                  >
                    同意退款
                  </el-button>
                  <el-button
                    v-if="order.orderStatus === 'REFUND_REQUESTED'"
                    type="danger"
                    @click="handleRejectRefund(order.orderId)"
                  >
                    拒绝退款
                  </el-button>
                  
                  <el-dropdown @command="(command) => handleVisibilityChange(order.orderId, command)">
                    <el-button>
                      可见性<el-icon><ArrowDown /></el-icon>
                    </el-button>
                    <template #dropdown>
                      <el-dropdown-menu>
                        <el-dropdown-item command="PUBLIC">完全可见</el-dropdown-item>
                        <el-dropdown-item command="PRIVATE">仅卖家可见</el-dropdown-item>
                        <el-dropdown-item command="HIDDEN">隐藏</el-dropdown-item>
                      </el-dropdown-menu>
                    </template>
                  </el-dropdown>
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
            :page-sizes="[5, 10, 20, 50]"
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
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { orderAPI, imageAPI } from '../api'
import { ElMessage, ElMessageBox } from 'element-plus'
import SafeTabs from '../components/SafeTabs.vue'

export default {
  name: 'SellerOrders',
  components: {
    SafeTabs
  },
  setup() {
    const router = useRouter()
    
    const loading = ref(false)
    const orders = ref([])
    const total = ref(0)
    const currentPage = ref(1)
    const pageSize = ref(10)
    const activeTab = ref('all')
    
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
    
    // 发货
    const handleShip = async (orderId) => {
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
        'REFUND_APPROVED': '暂无退款完成订单'
      }
      return statusMap[activeTab.value] || '暂无订单'
    }
    
    // 获取空状态操作按钮文本
    const getEmptyActionText = () => {
      return '管理商品'
    }
    
    // 处理空状态操作
    const handleEmptyAction = () => {
      router.push('/my-commodities')
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
      handleTabChange,
      handleSizeChange,
      handleCurrentChange,
      handleShip,
      handleCancel,
      handleApproveRefund,
      handleRejectRefund,
      handleVisibilityChange,
      getStatusType,
      getStatusText,
      getEmptyDescription,
      getEmptyActionText,
      handleEmptyAction,
      formatTime,
      getCommodityImageUrl
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
  padding: 30px 0;
}

.container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 20px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 30px;
}

.page-header h1 {
  margin: 0;
  color: #333;
  font-size: 28px;
}

.order-tabs {
  margin-bottom: 30px;
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
  padding: 20px;
  margin-bottom: 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.order-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding-bottom: 15px;
  border-bottom: 1px solid #eee;
}

.order-info {
  display: flex;
  flex-direction: column;
  gap: 5px;
}

.order-id {
  font-weight: 600;
  color: #333;
}

.order-time {
  color: #666;
  font-size: 14px;
}

.order-content {
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
  width: 80px;
  height: 80px;
  border-radius: 6px;
  overflow: hidden;
  background-color: #f5f5f5;
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
  font-weight: 600;
  color: #333;
}

.commodity-price {
  margin: 0 0 5px 0;
  color: #e74c3c;
  font-weight: 600;
  font-size: 18px;
}

.buyer-info {
  margin: 0;
  color: #666;
  font-size: 14px;
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
  font-weight: 600;
  color: #e74c3c;
}

.action-buttons {
  display: flex;
  gap: 10px;
  align-items: center;
}

.pagination-wrapper {
  display: flex;
  justify-content: center;
  margin-top: 30px;
}

@media (max-width: 768px) {
  .order-content {
    flex-direction: column;
    align-items: flex-start;
    gap: 20px;
  }
  
  .order-actions {
    width: 100%;
    flex-direction: row;
    justify-content: space-between;
    align-items: center;
  }
  
  .action-buttons {
    flex-wrap: wrap;
  }
}
</style>
