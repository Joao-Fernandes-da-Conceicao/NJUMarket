<template>
  <div class="order-detail-container">
    <div v-if="loading" class="loading-container">
      <el-skeleton :rows="10" animated />
    </div>
    
    <div v-else-if="order" class="order-detail">
      <!-- 页面头部 -->
      <div class="page-header">
        <el-button @click="$router.back()" icon="ArrowLeft">返回</el-button>
        <h1>订单详情</h1>
        <div class="order-status">
          <el-tag :type="getStatusType(order.orderStatus)" size="large">
            {{ getStatusText(order.orderStatus) }}
          </el-tag>
        </div>
      </div>

      <!-- 订单基本信息 -->
      <div class="order-info-section">
        <h2>订单信息</h2>
        <div class="info-grid">
          <div class="info-item">
            <label>订单号：</label>
            <span>{{ order.orderId }}</span>
          </div>
          <div class="info-item">
            <label>创建时间：</label>
            <span>{{ formatTime(order.createTime) }}</span>
          </div>
          <div class="info-item">
            <label>支付时间：</label>
            <span>{{ order.payTime ? formatTime(order.payTime) : '未支付' }}</span>
          </div>
          <div class="info-item">
            <label>发货时间：</label>
            <span>{{ order.shipTime ? formatTime(order.shipTime) : '未发货' }}</span>
          </div>
          <div class="info-item">
            <label>完成时间：</label>
            <span>{{ order.completeTime ? formatTime(order.completeTime) : '未完成' }}</span>
          </div>
          <div class="info-item">
            <label>订单总额：</label>
            <span class="total-amount">¥{{ order.payAmount }}</span>
          </div>
        </div>
      </div>

      <!-- 商品信息 -->
      <div class="commodity-section">
        <h2>商品信息</h2>
        <div class="commodity-card">
          <div class="commodity-image">
            <img 
              v-if="getCommodityImage(order)"
              :src="getCommodityImage(order)"
              :alt="getCommodityTitle(order)"
            />
            <div v-else class="no-image">
              <span>暂无照片</span>
            </div>
          </div>
          <div class="commodity-details">
            <h3 class="commodity-title">
              {{ getCommodityTitle(order) }}
              <el-tag 
                v-if="isCommoditySnapshotOffShelf(order)" 
                type="warning" 
                size="small"
                style="margin-left: 8px;"
              >
                已下架
              </el-tag>
            </h3>
            <p class="commodity-price">单价：¥{{ getCommodityPrice(order) }}</p>
            <p class="commodity-quantity">数量：{{ order.quantity }}</p>
            <p v-if="getCommodityLocation(order)" class="commodity-location">
              位置：{{ getCommodityLocation(order) }}
            </p>
            <p v-if="getCommodityCategory(order)" class="commodity-category">
              分类：{{ getCommodityCategory(order) }}
            </p>
            <p v-if="getCommodityCondition(order)" class="commodity-condition">
              成色：{{ getCommodityCondition(order) }}
            </p>
            <div v-if="getCommodityDescription(order)" class="commodity-description">
              <label>商品描述：</label>
              <p>{{ getCommodityDescription(order) }}</p>
            </div>
          </div>
        </div>
      </div>

      <!-- 卖家信息 -->
      <div class="seller-section">
        <h2>卖家信息</h2>
        <div class="seller-card">
          <div class="seller-avatar">
            <img 
              v-if="getSellerAvatar(order) && !isSellerDeleted(order)"
              :src="getAvatarUrl(getSellerAvatar(order))"
              :alt="getSellerName(order)"
            />
            <div v-else class="default-avatar">
              <span>{{ isSellerDeleted(order) ? '已' : getSellerName(order).charAt(0) }}</span>
            </div>
          </div>
          <div class="seller-details">
            <h3>{{ getSellerName(order) }}</h3>
            <p v-if="getSellerPhone(order)" class="seller-contact">
              电话：{{ getSellerPhone(order) }}
            </p>
            <p v-if="getSellerEmail(order)" class="seller-contact">
              邮箱：{{ getSellerEmail(order) }}
            </p>
            <div v-if="isSellerDeleted(order)" class="deleted-notice">
              <el-tag type="danger" size="small">用户已注销</el-tag>
            </div>
            <div v-else class="seller-actions">
              <el-button @click="contactSeller" type="primary" size="small">
                联系卖家
              </el-button>
              <el-button @click="viewSellerProfile" size="small">
                查看资料
              </el-button>
            </div>
          </div>
        </div>
      </div>

      <!-- 买家信息（仅卖家可见） -->
      <div v-if="isSeller" class="buyer-section">
        <h2>买家信息</h2>
        <div class="buyer-card">
          <div class="buyer-avatar">
            <img 
              v-if="getBuyerAvatar(order) && !isBuyerDeleted(order)"
              :src="getAvatarUrl(getBuyerAvatar(order))"
              :alt="getBuyerName(order)"
            />
            <div v-else class="default-avatar">
              <span>{{ isBuyerDeleted(order) ? '已' : getBuyerName(order).charAt(0) }}</span>
            </div>
          </div>
          <div class="buyer-details">
            <h3>{{ getBuyerName(order) }}</h3>
            <p v-if="getBuyerPhone(order)" class="buyer-contact">
              电话：{{ getBuyerPhone(order) }}
            </p>
            <p v-if="getBuyerEmail(order)" class="buyer-contact">
              邮箱：{{ getBuyerEmail(order) }}
            </p>
            <div v-if="isBuyerDeleted(order)" class="deleted-notice">
              <el-tag type="danger" size="small">用户已注销</el-tag>
            </div>
            <div v-else class="buyer-actions">
              <el-button @click="contactBuyer" type="primary" size="small">
                联系买家
              </el-button>
              <el-button @click="viewBuyerProfile" size="small">
                查看资料
              </el-button>
            </div>
          </div>
        </div>
      </div>

      <!-- 收货信息 -->
      <div class="shipping-section">
        <h2>收货信息</h2>
        <div class="shipping-info">
          <div class="info-item">
            <label>收货地址：</label>
            <span>{{ order.shippingAddress || '校内自提' }}</span>
          </div>
          <div v-if="order.trackingNumber" class="info-item">
            <label>物流单号：</label>
            <span>{{ order.trackingNumber }}</span>
          </div>
          <div v-if="order.remark" class="info-item">
            <label>备注：</label>
            <span>{{ order.remark }}</span>
          </div>
        </div>
      </div>

      <!-- 操作按钮 -->
      <div class="action-section">
        <div class="action-buttons">
          <!-- 买家操作 -->
          <template v-if="isBuyer">
            <el-button
              v-if="order.orderStatus === 'CREATED'"
              type="primary"
              @click="handlePay"
            >
              立即支付
            </el-button>
            <el-button
              v-if="order.orderStatus === 'SHIPPED'"
              type="success"
              @click="handleConfirm"
            >
              确认收货
            </el-button>
            <el-button
              v-if="['CREATED', 'PAID'].includes(order.orderStatus)"
              @click="handleCancel"
            >
              取消订单
            </el-button>
            <el-button
              v-if="order.orderStatus === 'COMPLETED'"
              @click="handleRate"
            >
              评价订单
            </el-button>
            <el-button
              v-if="order.orderStatus === 'COMPLETED'"
              @click="handleReorder"
            >
              再来一单
            </el-button>
          </template>

          <!-- 卖家操作 -->
          <template v-if="isSeller">
            <el-button
              v-if="order.orderStatus === 'PAID'"
              type="primary"
              @click="handleShip"
            >
              立即发货
            </el-button>
            <el-button
              v-if="order.orderStatus === 'REFUND_REQUESTED'"
              type="warning"
              @click="handleRefund"
            >
              处理退款
            </el-button>
          </template>
        </div>
      </div>
    </div>

    <div v-else class="empty-state">
      <el-empty description="订单不存在或已删除">
        <el-button @click="$router.back()">返回</el-button>
      </el-empty>
    </div>
  </div>
</template>

<script>
import { ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '../stores/user'
import { orderAPI, imageAPI } from '../api'
import { ElMessage, ElMessageBox } from 'element-plus'

export default {
  name: 'OrderDetail',
  setup() {
    const route = useRoute()
    const router = useRouter()
    const userStore = useUserStore()
    
    const loading = ref(false)
    const order = ref(null)
    
    const user = computed(() => userStore.user)
    const isBuyer = computed(() => order.value && user.value && order.value.buyerId === user.value.userId)
    const isSeller = computed(() => order.value && user.value && order.value.sellerId === user.value.userId)
    
    // 获取订单详情
    const fetchOrderDetail = async () => {
      loading.value = true
      try {
        const orderId = route.params.id
        const response = await orderAPI.getDetail(orderId)
        
        if (response.success) {
          order.value = response.data
        } else {
          ElMessage.error(response.errorMsg || '获取订单详情失败')
          router.back()
        }
      } catch (error) {
        ElMessage.error('获取订单详情失败')
        router.back()
      } finally {
        loading.value = false
      }
    }
    
    // 格式化时间
    const formatTime = (time) => {
      if (!time) return ''
      return new Date(time).toLocaleString()
    }
    
    // 获取状态类型
    const getStatusType = (status) => {
      const statusMap = {
        'CREATED': 'info',
        'PAID': 'warning',
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
        'PAID': '已支付',
        'SHIPPED': '已发货',
        'COMPLETED': '已完成',
        'CANCELLED': '已取消',
        'REFUND_REQUESTED': '退款申请中',
        'REFUND_APPROVED': '退款已批准',
        'REFUND_REJECTED': '退款被拒绝'
      }
      return statusMap[status] || status
    }
    
    // 商品相关方法
    const getCommodityImage = (order) => {
      if (order.commoditySnapshotImages) {
        try {
          const images = JSON.parse(order.commoditySnapshotImages)
          if (images && images.length > 0) {
            return getCommodityImageUrl(images[0])
          }
        } catch (error) {
          return getCommodityImageUrl(order.commoditySnapshotImages)
        }
      }
      if (order.commodity?.images && order.commodity.images.length > 0) {
        return getCommodityImageUrl(order.commodity.images[0])
      }
      return null
    }
    
    const getCommodityImageUrl = (imageUrl) => {
      if (!imageUrl) return imageAPI.getDefaultCommodityImage()
      if (imageUrl.startsWith('http')) return imageUrl
      if (imageUrl.includes('/')) return imageUrl
      const fileName = imageUrl.split('/').pop()
      return imageAPI.getCommodityImage(fileName)
    }
    
    const getCommodityTitle = (order) => {
      return order.commoditySnapshotTitle || order.commodity?.title || '商品标题'
    }
    
    const getCommodityPrice = (order) => {
      return order.commoditySnapshotPrice || order.commodity?.price || 0
    }
    
    const getCommodityLocation = (order) => {
      return order.commoditySnapshotLocation || order.commodity?.location
    }
    
    const getCommodityCategory = (order) => {
      return order.commoditySnapshotCategory || order.commodity?.category
    }
    
    const getCommodityCondition = (order) => {
      return order.commoditySnapshotConditionLevel || order.commodity?.conditionLevel
    }
    
    const getCommodityDescription = (order) => {
      return order.commoditySnapshotDescription || order.commodity?.description
    }
    
    const isCommoditySnapshotOffShelf = (order) => {
      return order.commoditySnapshotStatus && order.commoditySnapshotStatus !== 'ON_SHELF'
    }
    
    // 卖家相关方法
    const getSellerName = (order) => {
      if (order.seller?.isDeleted) {
        return '卖家已注销'
      }
      return order.seller?.nickname || order.seller?.username || order.commoditySnapshotSellerName || '卖家'
    }
    
    const getSellerPhone = (order) => {
      if (order.seller?.isDeleted) {
        return null
      }
      return order.seller?.phone || order.commoditySnapshotSellerPhone
    }
    
    const getSellerEmail = (order) => {
      if (order.seller?.isDeleted) {
        return null
      }
      return order.seller?.email || order.commoditySnapshotSellerEmail
    }
    
    const getSellerAvatar = (order) => {
      if (order.seller?.isDeleted) {
        return null
      }
      return order.seller?.avatar
    }
    
    const isSellerDeleted = (order) => {
      return order.seller?.isDeleted || false
    }
    
    // 买家相关方法
    const getBuyerName = (order) => {
      if (order.buyer?.isDeleted) {
        return '买家已注销'
      }
      return order.buyer?.nickname || order.buyer?.username || '买家'
    }
    
    const getBuyerPhone = (order) => {
      if (order.buyer?.isDeleted) {
        return null
      }
      return order.buyer?.phone
    }
    
    const getBuyerEmail = (order) => {
      if (order.buyer?.isDeleted) {
        return null
      }
      return order.buyer?.email
    }
    
    const getBuyerAvatar = (order) => {
      if (order.buyer?.isDeleted) {
        return null
      }
      return order.buyer?.avatar
    }
    
    const isBuyerDeleted = (order) => {
      return order.buyer?.isDeleted || false
    }
    
    // 头像相关方法
    const getAvatarUrl = (avatar) => {
      if (!avatar) return imageAPI.getDefaultAvatarImage()
      if (avatar.startsWith('http')) return avatar
      if (avatar.includes('/')) return avatar
      const fileName = avatar.split('/').pop()
      return imageAPI.getAvatarImage(fileName)
    }
    
    // 操作处理方法
    const handlePay = async () => {
      try {
        const response = await orderAPI.pay(order.value.orderId)
        if (response.success) {
          ElMessage.success('支付成功')
          fetchOrderDetail()
        } else {
          ElMessage.error(response.errorMsg || '支付失败')
        }
      } catch (error) {
        ElMessage.error('支付失败')
      }
    }
    
    const handleConfirm = async () => {
      try {
        const response = await orderAPI.confirm(order.value.orderId)
        if (response.success) {
          ElMessage.success('确认收货成功')
          fetchOrderDetail()
        } else {
          ElMessage.error(response.errorMsg || '确认收货失败')
        }
      } catch (error) {
        ElMessage.error('确认收货失败')
      }
    }
    
    const handleCancel = async () => {
      try {
        const { value: reason } = await ElMessageBox.prompt('请输入取消原因', '取消订单', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          inputPlaceholder: '请输入取消原因'
        })
        
        const response = await orderAPI.cancel(order.value.orderId, reason)
        if (response.success) {
          ElMessage.success('订单取消成功')
          fetchOrderDetail()
        } else {
          ElMessage.error(response.errorMsg || '取消订单失败')
        }
      } catch (error) {
        if (error !== 'cancel') {
          ElMessage.error('取消订单失败')
        }
      }
    }
    
    const handleShip = async () => {
      try {
        const { value: trackingNumber } = await ElMessageBox.prompt('请输入物流单号', '发货', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          inputPlaceholder: '请输入物流单号'
        })
        
        const response = await orderAPI.ship(order.value.orderId, trackingNumber)
        if (response.success) {
          ElMessage.success('发货成功')
          fetchOrderDetail()
        } else {
          ElMessage.error(response.errorMsg || '发货失败')
        }
      } catch (error) {
        if (error !== 'cancel') {
          ElMessage.error('发货失败')
        }
      }
    }
    
    const handleRate = () => {
      ElMessage.info('评价功能开发中...')
    }
    
    const handleReorder = () => {
      router.push(`/create-order/${order.value.orderId}`)
    }
    
    const handleRefund = () => {
      ElMessage.info('退款处理功能开发中...')
    }
    
    // 联系功能
    const contactSeller = () => {
      ElMessage.info('联系卖家功能开发中...')
    }
    
    const contactBuyer = () => {
      ElMessage.info('联系买家功能开发中...')
    }
    
    const viewSellerProfile = () => {
      if (order.value.sellerId) {
        router.push(`/profile/${order.value.sellerId}`)
      }
    }
    
    const viewBuyerProfile = () => {
      if (order.value.buyerId) {
        router.push(`/profile/${order.value.buyerId}`)
      }
    }
    
    onMounted(() => {
      fetchOrderDetail()
    })
    
    return {
      loading,
      order,
      user,
      isBuyer,
      isSeller,
      formatTime,
      getStatusType,
      getStatusText,
      getCommodityImage,
      getCommodityTitle,
      getCommodityPrice,
      getCommodityLocation,
      getCommodityCategory,
      getCommodityCondition,
      getCommodityDescription,
      isCommoditySnapshotOffShelf,
      getSellerName,
      getSellerPhone,
      getSellerEmail,
      getSellerAvatar,
      isSellerDeleted,
      getBuyerName,
      getBuyerPhone,
      getBuyerEmail,
      getBuyerAvatar,
      isBuyerDeleted,
      getAvatarUrl,
      handlePay,
      handleConfirm,
      handleCancel,
      handleShip,
      handleRate,
      handleReorder,
      handleRefund,
      contactSeller,
      contactBuyer,
      viewSellerProfile,
      viewBuyerProfile
    }
  }
}
</script>

<style scoped>
.order-detail-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px;
}

.loading-container {
  padding: 20px;
}

.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 30px;
  padding-bottom: 20px;
  border-bottom: 1px solid #e4e7ed;
}

.page-header h1 {
  margin: 0;
  color: #303133;
}

.order-info-section,
.commodity-section,
.seller-section,
.buyer-section,
.shipping-section {
  background: #fff;
  border-radius: 8px;
  padding: 20px;
  margin-bottom: 20px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.order-info-section h2,
.commodity-section h2,
.seller-section h2,
.buyer-section h2,
.shipping-section h2 {
  margin: 0 0 20px 0;
  color: #303133;
  font-size: 18px;
}

.info-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
  gap: 15px;
}

.info-item {
  display: flex;
  align-items: center;
}

.info-item label {
  font-weight: 500;
  color: #606266;
  margin-right: 10px;
  min-width: 100px;
}

.info-item span {
  color: #303133;
}

.total-amount {
  font-size: 18px;
  font-weight: bold;
  color: #e74c3c;
}

.commodity-card {
  display: flex;
  gap: 20px;
  align-items: flex-start;
}

.commodity-image {
  width: 200px;
  height: 200px;
  border-radius: 8px;
  overflow: hidden;
  background: #f5f7fa;
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
  color: #909399;
  font-size: 14px;
}

.commodity-details {
  flex: 1;
}

.commodity-title {
  margin: 0 0 10px 0;
  color: #303133;
  font-size: 20px;
  display: flex;
  align-items: center;
}

.commodity-price,
.commodity-quantity,
.commodity-location,
.commodity-category,
.commodity-condition {
  margin: 5px 0;
  color: #606266;
}

.commodity-description {
  margin-top: 15px;
}

.commodity-description label {
  font-weight: 500;
  color: #606266;
}

.commodity-description p {
  margin: 5px 0 0 0;
  color: #303133;
  line-height: 1.6;
}

.seller-card,
.buyer-card {
  display: flex;
  gap: 20px;
  align-items: center;
}

.seller-avatar,
.buyer-avatar {
  width: 80px;
  height: 80px;
  border-radius: 50%;
  overflow: hidden;
  background: #f5f7fa;
  display: flex;
  align-items: center;
  justify-content: center;
}

.seller-avatar img,
.buyer-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.default-avatar {
  width: 100%;
  height: 100%;
  background: #409eff;
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  font-weight: bold;
}

.seller-details,
.buyer-details {
  flex: 1;
}

.seller-details h3,
.buyer-details h3 {
  margin: 0 0 10px 0;
  color: #303133;
  font-size: 18px;
}

.seller-contact,
.buyer-contact {
  margin: 5px 0;
  color: #606266;
}

.seller-actions,
.buyer-actions {
  margin-top: 15px;
  display: flex;
  gap: 10px;
}

.deleted-notice {
  margin-top: 15px;
}

.shipping-info {
  display: flex;
  flex-direction: column;
  gap: 15px;
}

.action-section {
  background: #fff;
  border-radius: 8px;
  padding: 20px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.action-buttons {
  display: flex;
  gap: 15px;
  justify-content: center;
}

.empty-state {
  text-align: center;
  padding: 60px 20px;
}

@media (max-width: 768px) {
  .order-detail-container {
    padding: 10px;
  }
  
  .commodity-card {
    flex-direction: column;
  }
  
  .commodity-image {
    width: 100%;
    height: 250px;
  }
  
  .seller-card,
  .buyer-card {
    flex-direction: column;
    text-align: center;
  }
  
  .action-buttons {
    flex-direction: column;
  }
}
</style>
