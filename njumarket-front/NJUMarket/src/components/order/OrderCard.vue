<template>
  <div class="order-card">
    <div class="order-header">
      <div class="order-id-pill-wrapper" @click="handleOrderIdClick">
        <span class="order-id-pill">
          <span class="order-id-label">订单号</span>
          <el-icon class="order-id-icon" :class="{ 'arrow-up': showOrderId }">
            <ArrowDown />
          </el-icon>
        </span>
        
        <!-- 订单号弹出窗口 -->
        <div v-if="showOrderId" class="order-id-popup" @click.stop>
          <div class="order-id-popup-content">{{ order.orderId }}</div>
        </div>
      </div>
      <span class="order-time">{{ formatTime(order.createTime) }}</span>
      <el-tag 
        :type="getStatusType(order.orderStatus)" 
        size="small"
        class="order-status-tag"
      >
        {{ getStatusText(order.orderStatus) }}
      </el-tag>
    </div>

    <div class="order-content">
      <div class="commodity-info">
        <div class="commodity-image">
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
          
          <!-- 买家订单显示卖家信息（复合显示：头像+昵称+ID） -->
          <div v-if="type === 'buyer'" class="user-info seller-info">
            <span class="user-label">卖家：</span>
            <div class="user-profile">
              <el-avatar 
                :size="24" 
                :src="getAvatarUrl(order.sellerAvatar)"
                class="user-avatar"
              >
                {{ (order.sellerNickname || order.sellerId || 'U')?.charAt(0) }}
              </el-avatar>
              <span class="user-name">{{ order.sellerNickname || '用户' + order.sellerId }}</span>
              <span class="user-id">({{ order.sellerId }})</span>
            </div>
          </div>
          
          <!-- 卖家订单显示买家信息（复合显示：头像+昵称+ID） -->
          <div v-if="type === 'seller'" class="user-info buyer-info">
            <span class="user-label">买家：</span>
            <div class="user-profile">
              <el-avatar 
                :size="24" 
                :src="getAvatarUrl(order.buyerAvatar)"
                class="user-avatar"
              >
                {{ (order.buyerNickname || order.buyerId || 'U')?.charAt(0) }}
              </el-avatar>
              <span class="user-name">{{ order.buyerNickname || '用户' + order.buyerId }}</span>
              <span class="user-id">({{ order.buyerId }})</span>
            </div>
          </div>
          
          <!-- 买家订单：显示商品地址（商品所在位置） -->
          <div v-if="type === 'buyer' && formatCommodityAddress(order)" class="commodity-location">
            <span class="location-label">位置：</span>
            <!-- 如果有地址快照字段，使用标准格式（省市区-详细地址） -->
            <div v-if="!shouldUseSingleLineCommodityAddress(order)" class="address-display">
              <div class="address-region">
                {{ formatCommodityAddressRegion(order) }}
              </div>
              <div v-if="formatCommodityAddressDetail(order)" class="address-detail">
                {{ formatCommodityAddressDetail(order) }}
              </div>
            </div>
            <!-- 如果是旧数据（只有commoditySnapshotLocation），使用单行显示（废物利用） -->
            <span v-else class="address-single-line">{{ formatCommodityAddressRegion(order) }}</span>
          </div>
          
          <!-- 卖家订单：显示收货地址（用户给的地址） -->
          <div v-if="type === 'seller' && formatShippingAddress(order)" class="commodity-location">
            <span class="location-label">收货地址：</span>
            <!-- 如果有地址快照字段，使用标准格式（省市区-详细地址） -->
            <div v-if="!shouldUseSingleLineShippingAddress(order)" class="address-display">
              <div class="address-region">
                {{ formatShippingAddressRegion(order) }}
              </div>
              <div v-if="formatShippingAddressDetail(order)" class="address-detail">
                {{ formatShippingAddressDetail(order) }}
              </div>
            </div>
            <!-- 如果是旧数据（只有shippingAddress），使用单行显示（废物利用） -->
            <span v-else class="address-single-line">{{ formatShippingAddressRegion(order) }}</span>
          </div>
          
          <!-- 商品快照信息 -->
          <p v-if="order.commoditySnapshotTime" class="snapshot-time">
            快照时间：{{ formatTime(order.commoditySnapshotTime) }}
          </p>
        </div>
      </div>

      <div class="order-actions">
        <div class="order-summary">
          <p class="total-price">总计：¥{{ order.payAmount }}</p>
        </div>
        <div class="action-buttons">
          <slot name="actions"></slot>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { defineProps, defineEmits, ref } from 'vue'
import { ArrowDown } from '@element-plus/icons-vue'
import { ElAvatar } from 'element-plus'
import { formatTime } from '../../utils/formatUtils'
import { imageAPI } from '../../api'

const props = defineProps({
  order: {
    type: Object,
    required: true
  },
  type: {
    type: String,
    required: true,
    validator: (value) => ['buyer', 'seller'].includes(value)
  }
})

const emit = defineEmits(['order-id-click'])

const showOrderId = ref(false)

const handleOrderIdClick = (e) => {
  e.stopPropagation()
  // 切换弹出状态
  showOrderId.value = !showOrderId.value
  // 同时通知父组件
  emit('order-id-click', props.order.orderId, e)
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

// 获取商品快照标题
const getCommoditySnapshotTitle = (order) => {
  return order.commoditySnapshotTitle || order.commodity?.title || null
}

// 判断商品是否已下架
const isCommoditySnapshotOffShelf = (order) => {
  return order.commoditySnapshotStatus === 'OFF_SHELF' || 
         order.commoditySnapshotStatus === 'DRAFT'
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
    'REFUND_REJECTED': 'danger',
    'RETURN_REQUESTED': 'warning',
    'RETURN_APPROVED': 'success',
    'RETURN_REJECTED': 'danger',
    'RETURN_COMPLETED': 'success'
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
    'REFUND_REJECTED': '退款被拒',
    'RETURN_REQUESTED': '退货中',
    'RETURN_APPROVED': '退货完成',
    'RETURN_REJECTED': '退货被拒',
    'RETURN_COMPLETED': '退货完成'
  }
  return statusMap[status] || status
}

// 获取头像URL
const getAvatarUrl = (avatar) => {
  if (!avatar) return ''
  if (avatar.startsWith('http')) return avatar
  return `http://localhost:8080/api/images/avatars/${avatar}`
}

// 格式化商品地址（判断是否有地址数据）
const formatCommodityAddress = (order) => {
  // 如果有省市区字段，使用标准格式
  if (order.commoditySnapshotAddressProvince || 
      order.commoditySnapshotAddressCity || 
      order.commoditySnapshotAddressDistrict) {
    return true
  }
  // 如果有完整地址快照，也返回true
  if (order.commoditySnapshotAddressFull) {
    return true
  }
  // 如果有旧字段 commoditySnapshotLocation，也返回true（废物利用）
  if (order.commoditySnapshotLocation) {
    return true
  }
  return false
}

// 格式化商品地址的省市区部分
const formatCommodityAddressRegion = (order) => {
  // 优先使用地址快照的省市区字段（结构化数据）
  const parts = []
  if (order.commoditySnapshotAddressProvince) {
    parts.push(order.commoditySnapshotAddressProvince)
  }
  if (order.commoditySnapshotAddressCity) {
    parts.push(order.commoditySnapshotAddressCity)
  }
  if (order.commoditySnapshotAddressDistrict) {
    parts.push(order.commoditySnapshotAddressDistrict)
  }
  
  if (parts.length > 0) {
    return parts.join('')
  }
  
  // 如果没有省市区字段，尝试从完整地址快照中提取
  if (order.commoditySnapshotAddressFull) {
    return order.commoditySnapshotAddressFull
  }
  
  // 兼容旧字段：将 commoditySnapshotLocation 作为完整地址显示（废物利用）
  return order.commoditySnapshotLocation || ''
}

// 格式化商品地址的详细地址部分
const formatCommodityAddressDetail = (order) => {
  // 如果有地址快照的详细地址字段，使用结构化数据
  const parts = []
  if (order.commoditySnapshotAddressStreet) {
    parts.push(order.commoditySnapshotAddressStreet)
  }
  if (order.commoditySnapshotAddressDetail) {
    parts.push(order.commoditySnapshotAddressDetail)
  }
  
  if (parts.length > 0) {
    return parts.join('')
  }
  
  // 如果没有详细地址字段，但有完整地址快照，说明是旧数据格式
  // 这种情况下，详细地址部分为空，完整地址已经在省市区部分显示了
  return ''
}

// 检查是否应该使用单行显示（旧数据格式）
const shouldUseSingleLineCommodityAddress = (order) => {
  // 如果有地址快照的省市区字段，使用标准格式（省市区-详细地址）
  if (order.commoditySnapshotAddressProvince || 
      order.commoditySnapshotAddressCity || 
      order.commoditySnapshotAddressDistrict) {
    return false
  }
  // 如果没有地址快照字段，但有 commoditySnapshotLocation，使用单行显示（废物利用）
  if (order.commoditySnapshotLocation && !order.commoditySnapshotAddressFull) {
    return true
  }
  return false
}

// 格式化收货地址（判断是否有地址数据）
const formatShippingAddress = (order) => {
  // 如果有省市区字段，使用标准格式
  if (order.shippingAddressSnapshotProvince || 
      order.shippingAddressSnapshotCity || 
      order.shippingAddressSnapshotDistrict) {
    return true
  }
  // 如果有完整地址快照，也返回true
  if (order.shippingAddressSnapshotFull) {
    return true
  }
  // 如果有旧字段 shippingAddress，也返回true（废物利用）
  if (order.shippingAddress) {
    return true
  }
  return false
}

// 格式化收货地址的省市区部分
const formatShippingAddressRegion = (order) => {
  // 优先使用地址快照的省市区字段（结构化数据）
  const parts = []
  if (order.shippingAddressSnapshotProvince) {
    parts.push(order.shippingAddressSnapshotProvince)
  }
  if (order.shippingAddressSnapshotCity) {
    parts.push(order.shippingAddressSnapshotCity)
  }
  if (order.shippingAddressSnapshotDistrict) {
    parts.push(order.shippingAddressSnapshotDistrict)
  }
  
  if (parts.length > 0) {
    return parts.join('')
  }
  
  // 如果没有省市区字段，尝试从完整地址快照中提取
  if (order.shippingAddressSnapshotFull) {
    return order.shippingAddressSnapshotFull
  }
  
  // 兼容旧字段：将 shippingAddress 作为完整地址显示（废物利用）
  return order.shippingAddress || ''
}

// 格式化收货地址的详细地址部分
const formatShippingAddressDetail = (order) => {
  // 如果有地址快照的详细地址字段，使用结构化数据
  const parts = []
  if (order.shippingAddressSnapshotStreet) {
    parts.push(order.shippingAddressSnapshotStreet)
  }
  if (order.shippingAddressSnapshotDetail) {
    parts.push(order.shippingAddressSnapshotDetail)
  }
  
  if (parts.length > 0) {
    return parts.join('')
  }
  
  // 如果没有详细地址字段，但有完整地址快照，说明是旧数据格式
  // 这种情况下，详细地址部分为空，完整地址已经在省市区部分显示了
  return ''
}

// 检查是否应该使用单行显示（旧数据格式）
const shouldUseSingleLineShippingAddress = (order) => {
  // 如果有地址快照的省市区字段，使用标准格式（省市区-详细地址）
  if (order.shippingAddressSnapshotProvince || 
      order.shippingAddressSnapshotCity || 
      order.shippingAddressSnapshotDistrict) {
    return false
  }
  // 如果没有地址快照字段，但有 shippingAddress，使用单行显示（废物利用）
  if (order.shippingAddress && !order.shippingAddressSnapshotFull) {
    return true
  }
  return false
}
</script>

<style scoped>
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
  align-items: center;
  padding: 12px 20px;
  background: transparent; /* 透明背景 */
  margin-bottom: 0;
  position: relative;
}

.order-id-pill-wrapper {
  position: relative;
  cursor: pointer;
  z-index: 1;
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
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.order-id-pill:hover {
  background-color: rgba(106, 1, 94, 0.05); /* 悬停时轻微填充 */
}

.order-id-label {
  color: var(--primary-color);
}

.order-id-icon {
  font-size: 12px;
  transition: transform 0.3s ease;
  color: var(--primary-color);
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
  border-radius: 20.3px;
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

.order-time {
  color: var(--primary-color);
  font-size: 14px;
  font-weight: normal;
  position: absolute;
  left: 50%;
  transform: translateX(-50%);
  white-space: nowrap;
}

/* status-pill 已替换为 UnifiedTag，样式由 UnifiedTag 统一管理（border-radius: 9999px） */
/* 颜色和类型通过 UnifiedTag 的 type 属性控制 */

.order-status-tag {
  margin-left: auto;
  z-index: 1;
}

.order-content {
  padding: 20px;
  background: transparent; /* 透明背景 */
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.commodity-info {
  flex: 1;
  display: flex;
  gap: 15px;
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
  word-break: break-word;
}

.commodity-price {
  font-size: 18px;
  font-weight: normal;
  color: var(--primary-color);
  margin-bottom: 5px;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 8px 0;
  font-size: 14px;
}

.user-label {
  color: #999;
  flex-shrink: 0;
}

.user-profile {
  display: flex;
  align-items: center;
  gap: 6px;
  flex: 1;
  min-width: 0;
}

.user-avatar {
  flex-shrink: 0;
}

.user-name {
  color: #333;
  font-weight: normal;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  flex-shrink: 0;
}

.user-id {
  color: #999;
  font-size: 12px;
  flex-shrink: 0;
  white-space: nowrap;
}

.seller-info, .buyer-info {
  font-size: 14px;
  color: #666;
  margin: 8px 0;
}

.commodity-quantity {
  font-size: 14px;
  color: #666;
}

.commodity-location {
  font-size: 14px;
  color: #666;
  margin: 0;
  display: flex;
  align-items: flex-start;
  gap: 4px;
}

.location-label {
  flex-shrink: 0;
  color: #999;
}

.address-display {
  display: flex;
  flex-direction: column;
  gap: 2px;
  flex: 1;
}

.address-region {
  color: #666;
  font-weight: normal;
}

.address-detail {
  color: #999;
  font-size: 12px;
  margin-left: 0;
  padding-left: 0;
}

.address-single-line {
  color: #666;
  font-weight: normal;
  flex: 1;
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
  padding-top: 15px;
  border-top: 1px solid #f0f0f0;
  width: 100%;
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

@media (max-width: 900px) {
  /* 移动端订单号弹出窗口 */
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
  
  .commodity-info {
    flex-direction: row;
  }
  
  .commodity-image {
    width: 100px;
    height: 100px;
  }
  
  /* 移动端卖家/买家信息允许换行 */
  .user-info {
    flex-wrap: wrap;
  }
  
  .user-profile {
    flex-wrap: wrap;
  }
  
  .user-name, .user-id {
    white-space: normal;
    word-break: break-all;
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
  .action-buttons .el-button {
    height: 36px;
    min-width: 100px;
  }
}
</style>

