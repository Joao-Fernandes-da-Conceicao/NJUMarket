<template>
  <div class="order-card" @click="handleClick">
    <div class="card-main">
      <div class="card-image" v-if="getOrderImage(order)">
        <img :src="getImageUrl(getOrderImage(order))" :alt="order.commoditySnapshotTitle || '商品'" />
      </div>
      <div class="card-content">
        <h4 class="card-title">{{ order.commoditySnapshotTitle || '商品' }}</h4>
        <div class="card-price">¥{{ formatPrice(order.payAmount || order.totalAmount || order.commoditySnapshotPrice || 0) }}</div>
        <div class="card-status">
          <UnifiedTag :type="getStatusType(order.orderStatus)" size="small">
            {{ getStatusText(order.orderStatus) }}
          </UnifiedTag>
        </div>
      </div>
    </div>
    <div class="card-users" v-if="order.sellerId || order.buyerId">
      <!-- ✅ 调试：打印渲染时的数据 -->
      <!-- {{ console.log('🎨 OrderCard渲染: sellerNickname=', order.sellerNickname, 'sellerAvatar=', order.sellerAvatar, 'buyerNickname=', order.buyerNickname, 'buyerAvatar=', order.buyerAvatar) || '' }} -->
      <div class="card-user" v-if="order.sellerId">
        <div class="user-avatar" v-if="order.sellerAvatar">
          <img :src="getAvatarUrl(order.sellerAvatar)" :alt="order.sellerNickname || order.sellerId" />
        </div>
        <span class="user-label">卖家:</span>
        <span class="user-name">{{ order.sellerNickname || order.sellerId }}</span>
      </div>
      <div class="card-user" v-if="order.buyerId">
        <div class="user-avatar" v-if="order.buyerAvatar">
          <img :src="getAvatarUrl(order.buyerAvatar)" :alt="order.buyerNickname || order.buyerId" />
        </div>
        <span class="user-label">买家:</span>
        <span class="user-name">{{ order.buyerNickname || order.buyerId }}</span>
      </div>
    </div>
  </div>
</template>

<script setup>
/* global defineProps, defineEmits */
import { onMounted, watch } from 'vue'
import { formatPrice } from '../../utils/formatUtils'
import { imageAPI } from '../../api'
import UnifiedTag from '../common/UnifiedTag.vue'

const props = defineProps({
  order: {
    type: Object,
    required: true
  }
})

const emit = defineEmits(['click'])

// ✅ 调试日志：组件挂载时打印订单数据
onMounted(() => {
  console.log(`📦 OrderCard: 组件已挂载 (orderId=${props.order.orderId})`, {
    orderId: props.order.orderId,
    sellerId: props.order.sellerId,
    buyerId: props.order.buyerId,
    sellerNickname: props.order.sellerNickname,
    sellerAvatar: props.order.sellerAvatar,
    buyerNickname: props.order.buyerNickname,
    buyerAvatar: props.order.buyerAvatar,
    hasSellerProfile: !!(props.order.sellerNickname || props.order.sellerAvatar),
    hasBuyerProfile: !!(props.order.buyerNickname || props.order.buyerAvatar),
    fullOrder: props.order
  })
})

// ✅ 调试日志：监听订单数据变化
watch(() => props.order, (newOrder, oldOrder) => {
  console.log(`🔄 OrderCard: 订单数据变化 (orderId=${newOrder?.orderId})`, {
    orderId: newOrder?.orderId,
    sellerId: newOrder?.sellerId,
    buyerId: newOrder?.buyerId,
    sellerNickname: newOrder?.sellerNickname,
    sellerAvatar: newOrder?.sellerAvatar,
    buyerNickname: newOrder?.buyerNickname,
    buyerAvatar: newOrder?.buyerAvatar,
    hasSellerProfile: !!(newOrder?.sellerNickname || newOrder?.sellerAvatar),
    hasBuyerProfile: !!(newOrder?.buyerNickname || newOrder?.buyerAvatar),
    profileChanged: {
      sellerNickname: oldOrder?.sellerNickname !== newOrder?.sellerNickname,
      sellerAvatar: oldOrder?.sellerAvatar !== newOrder?.sellerAvatar,
      buyerNickname: oldOrder?.buyerNickname !== newOrder?.buyerNickname,
      buyerAvatar: oldOrder?.buyerAvatar !== newOrder?.buyerAvatar
    },
    newOrderFull: newOrder
  })
}, { deep: true, immediate: false })

const getStatusText = (status) => {
  const statusMap = {
    'PENDING': '待付款',
    'CREATED': '待支付',
    'PAID': '已付款',
    'SHIPPED': '已发货',
    'DELIVERED': '已送达',
    'COMPLETED': '已完成',
    'CANCELLED': '已取消',
    'REFUNDED': '已退款',
    'REFUND_PENDING': '退款待处理',
    'REFUND_REQUESTED': '退款中',
    'REFUND_APPROVED': '退款完成',
    'REFUND_REJECTED': '退款被拒',
    'REFUND_PROCESSING': '退款处理中',
    'RETURN_PENDING': '退货待处理',
    'RETURN_APPROVED': '退货已批准',
    'RETURN_REJECTED': '退货被拒',
    'RETURN_PROCESSING': '退货处理中',
    'RETURN_SHIPPED': '退货已发货',
    'RETURN_COMPLETED': '退货已完成'
  }
  return statusMap[status] || status
}

const getStatusType = (status) => {
  const typeMap = {
    'PENDING': 'warning',
    'CREATED': 'warning',
    'PAID': 'info',
    'SHIPPED': 'primary',
    'DELIVERED': 'success',
    'COMPLETED': 'success',
    'CANCELLED': 'danger',
    'REFUNDED': 'info',
    'REFUND_PENDING': 'warning',
    'REFUND_REQUESTED': 'warning',
    'REFUND_APPROVED': 'success',
    'REFUND_REJECTED': 'danger',
    'REFUND_PROCESSING': 'info',
    'RETURN_PENDING': 'warning',
    'RETURN_APPROVED': 'success',
    'RETURN_REJECTED': 'danger',
    'RETURN_PROCESSING': 'info',
    'RETURN_SHIPPED': 'primary',
    'RETURN_COMPLETED': 'success'
  }
  return typeMap[status] || 'info'
}

const getImageUrl = (imageUrl) => {
  if (!imageUrl) return imageAPI.getDefaultCommodityImage()
  if (imageUrl.startsWith('http')) return imageUrl
  if (imageUrl.includes('/')) return imageUrl
  const fileName = imageUrl.split('/').pop()
  return imageAPI.getCommodityImage(fileName)
}

const getAvatarUrl = (avatar) => {
  if (!avatar) return ''
  if (avatar.startsWith('http')) return avatar
  return `http://localhost:8080/uploads/avatars/${avatar}`
}

// 获取订单商品快照的第一张图片
const getOrderImage = (order) => {
  // 优先级1: commoditySnapshotImages 字段（字符串或数组）
  if (order.commoditySnapshotImages) {
    if (typeof order.commoditySnapshotImages === 'string') {
      const images = order.commoditySnapshotImages.split(',').map(img => img.trim()).filter(img => img)
      if (images.length > 0) {
        return images[0]
      }
    } else if (Array.isArray(order.commoditySnapshotImages)) {
      if (order.commoditySnapshotImages.length > 0 && order.commoditySnapshotImages[0]) {
        return String(order.commoditySnapshotImages[0]).trim()
      }
    }
  }
  
  // 优先级2: commoditySnapshot.images 字段
  if (order.commoditySnapshot && order.commoditySnapshot.images) {
    if (typeof order.commoditySnapshot.images === 'string') {
      const images = order.commoditySnapshot.images.split(',').map(img => img.trim()).filter(img => img)
      if (images.length > 0) {
        return images[0]
      }
    } else if (Array.isArray(order.commoditySnapshot.images)) {
      if (order.commoditySnapshot.images.length > 0 && order.commoditySnapshot.images[0]) {
        return String(order.commoditySnapshot.images[0]).trim()
      }
    }
  }
  
  // 优先级3: commoditySnapshotImage 字段（字符串或数组）
  if (order.commoditySnapshotImage) {
    if (typeof order.commoditySnapshotImage === 'string') {
      const images = order.commoditySnapshotImage.split(',').map(img => img.trim()).filter(img => img)
      if (images.length > 0) {
        return images[0]
      }
    } else if (Array.isArray(order.commoditySnapshotImage)) {
      if (order.commoditySnapshotImage.length > 0 && order.commoditySnapshotImage[0]) {
        return String(order.commoditySnapshotImage[0]).trim()
      }
    }
  }
  
  // 优先级4: commoditySnapshot 是字符串，尝试解析JSON
  if (typeof order.commoditySnapshot === 'string') {
    try {
      const snapshot = JSON.parse(order.commoditySnapshot)
      if (snapshot.images) {
        if (typeof snapshot.images === 'string') {
          const images = snapshot.images.split(',').map(img => img.trim()).filter(img => img)
          if (images.length > 0) {
            return images[0]
          }
        } else if (Array.isArray(snapshot.images)) {
          if (snapshot.images.length > 0 && snapshot.images[0]) {
            return String(snapshot.images[0]).trim()
          }
        }
      }
    } catch (e) {
      // JSON解析失败，忽略
    }
  }
  
  // 优先级5: commoditySnapshot 是对象
  if (order.commoditySnapshot && typeof order.commoditySnapshot === 'object' && !Array.isArray(order.commoditySnapshot)) {
    if (order.commoditySnapshot.images) {
      if (typeof order.commoditySnapshot.images === 'string') {
        const images = order.commoditySnapshot.images.split(',').map(img => img.trim()).filter(img => img)
        if (images.length > 0) {
          return images[0]
        }
      } else if (Array.isArray(order.commoditySnapshot.images)) {
        if (order.commoditySnapshot.images.length > 0 && order.commoditySnapshot.images[0]) {
          return String(order.commoditySnapshot.images[0]).trim()
        }
      }
    }
  }
  
  return null
}

const handleClick = () => {
  emit('click', props.order)
}
</script>

<style scoped>
.order-card {
  display: flex;
  flex-direction: column;
  padding: 12px;
  background: #f9f9f9;
  border-radius: 8px;
  border: 1px solid #e0e0e0;
  cursor: pointer;
  transition: all 0.2s ease;
  margin-top: 8px;
  max-width: 300px;
  width: 100%;
  box-sizing: border-box;
  gap: 8px;
}

.order-card:hover {
  background: #f0f0f0;
  border-color: var(--primary-color);
}

.card-main {
  display: flex;
  gap: 12px;
}

.card-image {
  width: 80px;
  height: 80px;
  flex-shrink: 0;
  border-radius: 6px;
  overflow: hidden;
  background: #fff;
}

.card-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.card-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 6px;
  min-width: 0;
}

.card-title {
  font-size: 14px;
  font-weight: normal;
  color: #333;
  margin: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.card-price {
  font-size: 16px;
  font-weight: normal;
  color: var(--primary-color);
}

.card-status {
  margin-top: 2px;
}

.card-users {
  display: flex;
  flex-direction: row;
  gap: 12px;
  padding-top: 8px;
  border-top: 1px solid #e8e8e8;
  width: 100%;
  flex-wrap: wrap;
}

.card-user {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  flex: 1;
  min-width: 0;
}

.user-avatar {
  width: 20px;
  height: 20px;
  border-radius: 50%;
  overflow: hidden;
  background: #f0f0f0;
  flex-shrink: 0;
}

.user-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.user-label {
  color: #999;
  flex-shrink: 0;
}

.user-name {
  color: #333;
  font-weight: normal;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  min-width: 0;
}
</style>

