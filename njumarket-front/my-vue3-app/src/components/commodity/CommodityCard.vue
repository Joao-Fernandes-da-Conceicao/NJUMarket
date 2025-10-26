<template>
  <div class="commodity-card" @click="handleClick">
    <div class="commodity-image">
      <img 
        v-if="commodity.images && commodity.images.length > 0"
        :src="getCommodityImageUrl(commodity.images[0])" 
        :alt="commodity.title"
        @error="handleImageError"
      />
      <div v-else class="no-image">
        <el-icon size="48"><Picture /></el-icon>
        <span>暂无照片</span>
      </div>
      
      <!-- 商品状态标签 -->
      <div v-if="commodity.commodityStatus !== 'ON_SHELF'" class="status-badge">
        <el-tag 
          :type="getStatusTagType(commodity.commodityStatus)"
          size="small"
        >
          {{ getStatusText(commodity.commodityStatus) }}
        </el-tag>
      </div>
    </div>
    
    <div class="commodity-info">
      <!-- 左下角区域：标题 + 价格 -->
      <div class="bottom-left-section">
        <h3 class="commodity-title" :title="commodity.title">
          {{ commodity.title }}
        </h3>
        <p class="commodity-price">
          ¥{{ formatPrice(commodity.price) }}
        </p>
      </div>
      
      <!-- 地址和浏览量等元素 - 右下侧，主题色 -->
      <div class="commodity-stats">
        <span class="stat-item">
          <el-icon><Location /></el-icon>
          {{ commodity.location || '未设置位置' }}
        </span>
        <span class="stat-item">
          <el-icon><View /></el-icon>
          {{ commodity.clickCount || 0 }}
        </span>
        <span class="stat-item">
          <el-icon><Clock /></el-icon>
          {{ formatTime(commodity.publishTime) }}
        </span>
      </div>
      
      <!-- 卖家信息 - 统计信息下方 -->
      <div v-if="showSellerInfo && sellerInfo" class="seller-info">
        <el-avatar :size="28" :src="getAvatarUrl(sellerInfo.avatar)">
          {{ sellerInfo.nickname?.charAt(0) || 'S' }}
        </el-avatar>
        <span class="seller-name">{{ sellerInfo.nickname || '卖家' }}</span>
      </div>
      
      <!-- 卖家信息加载状态 -->
      <div v-else-if="showSellerInfo && sellerLoading" class="seller-info">
        <el-skeleton :width="28" :height="28" circle />
        <el-skeleton :width="80" :height="18" />
      </div>
    </div>
  </div>
</template>

<script setup>
import { defineProps, defineEmits, ref, onMounted } from 'vue'
import { getCommodityImageUrl, getAvatarUrl } from '../../utils/imageUtils'
import { formatPrice, formatTime } from '../../utils/formatUtils'
import { profileAPI } from '../../api/index'

const props = defineProps({
  commodity: {
    type: Object,
    required: true
  },
  showSellerInfo: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['click'])

// 卖家信息
const sellerInfo = ref(null)
const sellerLoading = ref(false)

const handleClick = () => {
  emit('click', props.commodity)
}

const handleImageError = (event) => {
  event.target.style.display = 'none'
  const nextElement = event.target.nextElementSibling
  if (nextElement) {
    nextElement.style.display = 'flex'
  }
}

const getStatusTagType = (status) => {
  const statusMap = {
    'DRAFT': 'info',
    'ON_SHELF': 'success',
    'OFF_SHELF': 'danger'
  }
  return statusMap[status] || 'info'
}

const getStatusText = (status) => {
  const statusMap = {
    'DRAFT': '草稿',
    'ON_SHELF': '在售',
    'OFF_SHELF': '已下架'
  }
  return statusMap[status] || '未知'
}

// 获取卖家信息
const fetchSellerInfo = async () => {
  if (!props.showSellerInfo || !props.commodity.sellerId) {
    return
  }
  
  try {
    sellerLoading.value = true
    const response = await profileAPI.getUser(props.commodity.sellerId)
    if (response.success) {
      sellerInfo.value = response.data
    }
  } catch (error) {
    console.error('获取卖家信息失败:', error)
  } finally {
    sellerLoading.value = false
  }
}

onMounted(() => {
  fetchSellerInfo()
})
</script>

<style scoped>
.commodity-card {
  background: white;
  border-radius: 16px;
  overflow: hidden;
  cursor: pointer;
  transition: all 0.3s ease;
  position: relative;
  border: none;
  box-shadow: none;
  display: flex;
  flex-direction: column;
  height: 100%;
}

.commodity-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.08);
}

/* 商品图片区域 - 卡片最上端 */
.commodity-image {
  position: relative;
  width: 100%;
  height: 240px; /* 调整为16:10比例，更适合宽屏 */
  overflow: hidden;
  border-radius: 12px;
  margin-bottom: 12px;
}

.commodity-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.3s ease;
}

.commodity-card:hover .commodity-image img {
  transform: scale(1.05);
}

.no-image {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  background-color: #f5f5f5;
  color: #999;
}

.no-image span {
  margin-top: 8px;
  font-size: 14px;
}

.status-badge {
  position: absolute;
  top: 8px;
  right: 8px;
}

/* 商品信息区域 */
.commodity-info {
  padding: 0 12px 12px 12px;
  display: flex;
  flex-direction: column;
  min-height: 120px; /* 设置最小高度 */
  position: relative;
}

/* 左下角区域：标题 + 价格 */
.bottom-left-section {
  position: absolute;
  bottom: 12px; /* 距离底部12px，避免与边框重叠 */
  left: 12px;
  right: 12px;
  z-index: 2;
}

.commodity-title {
  font-size: 25px;
  font-weight: normal;
  margin: 0 0 4px 0;
  color: var(--primary-color);
  line-height: 1.3;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.commodity-price {
  font-size: 50px;
  font-weight: normal;
  margin: 0;
  color: var(--primary-color);
}

/* 统计信息 - 右下侧，主题色 */
.commodity-stats {
  display: flex;
  flex-direction: column;
  gap: 4px;
  margin-top: auto;
  margin-bottom: 60px; /* 为左下角区域留出空间 */
}

/* 卖家信息 - 统计信息下方 */
.seller-info {
  display: flex;
  align-items: center;
  gap: 10px;
  justify-content: flex-end;
  margin-top: 10px;
  margin-bottom: 8px;
}

.seller-name {
  font-size: 18px;
  color: var(--primary-color);
  line-height: 1.4;
}

.stat-item {
  font-size: 12px;
  color: var(--primary-color);
  display: flex;
  align-items: center;
  gap: 4px;
  justify-content: flex-end;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .commodity-image {
    height: 160px;
    border-radius: 10px;
    margin-bottom: 8px;
  }
  
  .commodity-info {
    padding: 0 8px 8px 8px;
  }
  
  .bottom-left-section {
    left: 8px;
    right: 8px;
  }
  
  .commodity-title {
    font-size: 11px;
    margin-bottom: 3px;
  }
  
  .commodity-price {
    font-size: 18px;
  }
  
  .seller-info {
    margin-top: 8px;
    margin-bottom: 6px;
    gap: 8px;
  }
  
  .seller-name {
    font-size: 16px;
    line-height: 1.4;
  }
  
  .commodity-stats {
    gap: 3px;
  }
  
  .stat-item {
    font-size: 11px;
  }
}
</style>
