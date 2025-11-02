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
        <UnifiedTag 
          :type="getStatusTagType(commodity.commodityStatus)"
          size="small"
        >
          {{ getStatusText(commodity.commodityStatus) }}
        </UnifiedTag>
      </div>
    </div>
    
    <!-- 统计信息 - 图片正下方，一行平铺 -->
    <div class="commodity-stats">
      <span class="stat-item">
        <el-icon><Location /></el-icon>
        {{ commodity.location || '未设置位置' }}
        <span class="stat-divider">|</span>
        <el-icon><View /></el-icon>
        {{ commodity.clickCount || 0 }}
        <span class="stat-divider">|</span>
        <el-icon><Clock /></el-icon>
        {{ formatTime(commodity.publishTime) }}
      </span>
    </div>
    
    <div class="commodity-info">
      <!-- 标题 + 价格 -->
      <h3 class="commodity-title" :title="commodity.title">
        {{ commodity.title }}
      </h3>
      <p class="commodity-price">
        ¥{{ formatPrice(commodity.price) }}
      </p>
      
      <!-- 卖家信息 - 独立行，居中显示，可点击跳转 -->
      <div 
        v-if="showSellerInfo && sellerInfo" 
        class="seller-info" 
        @click.stop="handleSellerClick"
      >
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
import { defineProps, defineEmits, computed } from 'vue'
import { useRouter } from 'vue-router'
import { getCommodityImageUrl, getAvatarUrl } from '../../utils/imageUtils'
import { formatPrice, formatTime } from '../../utils/formatUtils'
import UnifiedTag from '../common/UnifiedTag.vue'

const router = useRouter()

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

// 卖家信息点击跳转
const handleSellerClick = () => {
  if (props.commodity.sellerId) {
    router.push(`/home/${props.commodity.sellerId}`)
  }
}

// ✅ 优化：直接使用后端返回的卖家信息，无需额外查询
const sellerInfo = computed(() => {
  if (!props.showSellerInfo || !props.commodity.sellerId) {
    return null
  }
  // 后端已通过批量查询返回 sellerNickname 和 sellerAvatar
  return {
    nickname: props.commodity.sellerNickname || '卖家',
    avatar: props.commodity.sellerAvatar
  }
})

const sellerLoading = computed(() => false) // 后端已返回，无需加载
</script>

<style scoped>
.commodity-card {
  background: transparent;
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
  aspect-ratio: 3 / 2; /* 3:2比例 */
  height: auto;
  overflow: hidden;
  border-radius: calc(12px * var(--mobile-scale, 1));
  margin-bottom: calc(12px * var(--mobile-scale, 1));
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

/* 统计信息 - 图片正下方，一行平铺 */
.commodity-stats {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: calc(6px * var(--mobile-scale, 1));
  padding: 0 calc(12px * var(--mobile-scale, 1));
  margin-bottom: 5px;
  flex-wrap: nowrap;
}

.stat-divider {
  color: #ddd;
  font-size: 12px;
  padding: 0 2px;
}

/* 商品信息区域 */
.commodity-info {
  padding: 0 calc(12px * var(--mobile-scale, 1)) calc(12px * var(--mobile-scale, 1));
  display: flex;
  flex-direction: column;
  min-height: calc(80px * var(--mobile-scale, 1));
  position: relative;
}

.commodity-title {
  font-size: 25px;
  font-weight: normal;
  margin: 0 0 calc(4px * var(--mobile-scale, 1));
  color: var(--primary-color);
  line-height: 1.3;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  text-align: center;
}

.commodity-price {
  font-size: 30px;
  font-weight: normal;
  margin: 0;
  color: var(--primary-color);
  text-align: center;
}

/* 卖家信息 - 独立行，居中显示 */
.seller-info {
  display: flex;
  align-items: center;
  gap: calc(10px * var(--mobile-scale, 1));
  justify-content: center;
  margin-top: calc(10px * var(--mobile-scale, 1));
  margin-bottom: calc(8px * var(--mobile-scale, 1));
  border-top: 1px solid #f0f0f0;
  padding-top: calc(10px * var(--mobile-scale, 1));
  width: 100%;
  cursor: pointer;
  transition: all 0.3s ease;
}

.seller-info:hover {
  opacity: 0.8;
}

.seller-name {
  font-size: calc(20px * var(--mobile-scale, 1));
  color: var(--primary-color);
  line-height: 1.4;
}

.stat-item {
  font-size: calc(12px * var(--mobile-scale, 1));
  color: var(--primary-color);
  display: flex;
  align-items: center;
  gap: calc(4px * var(--mobile-scale, 1));
  white-space: nowrap;
}

/* 响应式设计 - 综合移动端查询 */
/* 注意：此文件中的所有数值已通过 CSS 变量 --mobile-scale 自动缩放 */
@media (max-width: 900px) {
  .commodity-image {
    aspect-ratio: 3 / 2; /* 3:2比例，移动端保持一致 */
    border-radius: 10px;
    margin-bottom: calc(8px * var(--mobile-scale, 1));
  }
  
  .commodity-info {
    padding: 0 calc(8px * var(--mobile-scale, 1)) calc(8px * var(--mobile-scale, 1));
  }
  
  .bottom-left-section {
    left: calc(8px * var(--mobile-scale, 1));
    right: calc(8px * var(--mobile-scale, 1));
  }
}
</style>
