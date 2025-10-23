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
      <h3 class="commodity-title" :title="commodity.title">
        {{ commodity.title }}
      </h3>
      
      <div class="commodity-meta">
        <p class="commodity-price text-primary">
          ¥{{ formatPrice(commodity.price) }}
        </p>
        <p class="commodity-location text-light">
          <el-icon><Location /></el-icon>
          {{ commodity.location || '未设置位置' }}
        </p>
      </div>
      
      <div class="commodity-stats">
        <span class="stat-item">
          <el-icon><View /></el-icon>
          {{ commodity.clickCount || 0 }}
        </span>
        <span class="stat-item">
          <el-icon><Clock /></el-icon>
          {{ formatTime(commodity.publishTime) }}
        </span>
      </div>
      
      <!-- 卖家信息 -->
      <div v-if="showSellerInfo && commodity.sellerInfo" class="seller-info">
        <el-avatar :size="20" :src="getAvatarUrl(commodity.sellerInfo.avatar)">
          {{ commodity.sellerInfo.nickname?.charAt(0) || 'S' }}
        </el-avatar>
        <span class="seller-name">{{ commodity.sellerInfo.nickname || '卖家' }}</span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { defineProps, defineEmits } from 'vue'
import { getCommodityImageUrl, getAvatarUrl } from '../../utils/imageUtils'
import { formatPrice, formatTime } from '../../utils/formatUtils'

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
    'SOLD_OUT': 'warning',
    'OFF_SHELF': 'danger'
  }
  return statusMap[status] || 'info'
}

const getStatusText = (status) => {
  const statusMap = {
    'DRAFT': '草稿',
    'ON_SHELF': '在售',
    'SOLD_OUT': '已售出',
    'OFF_SHELF': '已下架'
  }
  return statusMap[status] || '未知'
}
</script>

<style scoped>
.commodity-card {
  background: white;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  overflow: hidden;
  cursor: pointer;
  transition: all 0.3s ease;
  position: relative;
}

.commodity-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.15);
}

.commodity-image {
  position: relative;
  width: 100%;
  height: 200px;
  overflow: hidden;
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

.commodity-info {
  padding: 16px;
}

.commodity-title {
  font-size: 16px;
  font-weight: 600;
  margin: 0 0 12px 0;
  color: #333;
  line-height: 1.4;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.commodity-meta {
  margin-bottom: 12px;
}

.commodity-price {
  font-size: 18px;
  font-weight: 700;
  margin: 0 0 8px 0;
}

.commodity-location {
  font-size: 14px;
  margin: 0;
  display: flex;
  align-items: center;
  gap: 4px;
}

.commodity-stats {
  display: flex;
  gap: 16px;
  margin-bottom: 12px;
}

.stat-item {
  font-size: 12px;
  color: #999;
  display: flex;
  align-items: center;
  gap: 4px;
}

.seller-info {
  display: flex;
  align-items: center;
  gap: 8px;
  padding-top: 12px;
  border-top: 1px solid #f0f0f0;
}

.seller-name {
  font-size: 14px;
  color: #666;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .commodity-image {
    height: 160px;
  }
  
  .commodity-info {
    padding: 12px;
  }
  
  .commodity-title {
    font-size: 14px;
  }
  
  .commodity-price {
    font-size: 16px;
  }
}
</style>
