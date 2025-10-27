<template>
  <div class="commodity-grid">
    <div 
      v-for="commodity in commodities" 
      :key="commodity.commodityId"
      class="grid-item"
    >
      <CommodityCard 
        v-if="cardType === 'simple'"
        :commodity="commodity"
        :show-seller-info="showSellerInfo"
        @click="handleCommodityClick"
      />
      <BrowseCommodityCard 
        v-else
        :commodity="commodity"
        :show-seller-info="showSellerInfo"
        @click="handleCommodityClick"
      />
    </div>
    
    <!-- 空状态 -->
    <div v-if="commodities.length === 0 && !loading" class="empty-state">
      <el-empty :description="emptyText">
        <UnifiedButton v-if="showEmptyAction" type="primary" @click="handleEmptyAction">
          {{ emptyActionText }}
        </UnifiedButton>
      </el-empty>
    </div>
    
    <!-- 加载状态 -->
    <div v-if="loading" class="loading-state">
      <div v-for="n in 6" :key="n" class="skeleton-card">
        <div class="skeleton-image"></div>
        <div class="skeleton-content">
          <div class="skeleton-title"></div>
          <div class="skeleton-price"></div>
          <div class="skeleton-location"></div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { defineProps, defineEmits } from 'vue'
import CommodityCard from './CommodityCard.vue'
import BrowseCommodityCard from './BrowseCommodityCard.vue'
import UnifiedButton from '../common/UnifiedButton.vue'

defineProps({
  commodities: {
    type: Array,
    default: () => []
  },
  loading: {
    type: Boolean,
    default: false
  },
  showSellerInfo: {
    type: Boolean,
    default: false
  },
  cardType: {
    type: String,
    default: 'browse', // 'simple' 或 'browse'
    validator: (value) => ['simple', 'browse'].includes(value)
  },
  emptyText: {
    type: String,
    default: '暂无商品'
  },
  showEmptyAction: {
    type: Boolean,
    default: false
  },
  emptyActionText: {
    type: String,
    default: '发布商品'
  }
})

const emit = defineEmits(['commodity-click', 'empty-action'])

const handleCommodityClick = (commodity) => {
  emit('commodity-click', commodity)
}

const handleEmptyAction = () => {
  emit('empty-action')
}
</script>

<style scoped>
.commodity-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 20px;
  padding: 20px 0;
}

.grid-item {
  width: 100%;
}

.empty-state {
  grid-column: 1 / -1;
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 300px;
}

.loading-state {
  grid-column: 1 / -1;
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 20px;
}

.skeleton-card {
  background: white;
  border-radius: 16px;
  overflow: hidden;
  border: none;
  box-shadow: none;
}

.skeleton-image {
  width: 100%;
  height: 160px;
  background: linear-gradient(90deg, #f0f0f0 25%, #e0e0e0 50%, #f0f0f0 75%);
  background-size: 200% 100%;
  animation: loading 1.5s infinite;
  border-radius: 12px;
  margin-bottom: 12px;
}

.skeleton-content {
  padding: 0 4px 12px 4px;
}

.skeleton-title {
  height: 20px;
  background: linear-gradient(90deg, #f0f0f0 25%, #e0e0e0 50%, #f0f0f0 75%);
  background-size: 200% 100%;
  animation: loading 1.5s infinite;
  border-radius: 4px;
  margin-bottom: 8px;
}

.skeleton-price {
  height: 18px;
  width: 80px;
  background: linear-gradient(90deg, #f0f0f0 25%, #e0e0e0 50%, #f0f0f0 75%);
  background-size: 200% 100%;
  animation: loading 1.5s infinite;
  border-radius: 4px;
  margin-bottom: 6px;
}

.skeleton-location {
  height: 14px;
  width: 120px;
  background: linear-gradient(90deg, #f0f0f0 25%, #e0e0e0 50%, #f0f0f0 75%);
  background-size: 200% 100%;
  animation: loading 1.5s infinite;
  border-radius: 4px;
}

@keyframes loading {
  0% {
    background-position: 200% 0;
  }
  100% {
    background-position: -200% 0;
  }
}

/* 响应式设计 */
@media (min-width: 1600px) {
  .commodity-grid {
    grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
    gap: 24px;
  }
  
  .loading-state {
    grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
    gap: 24px;
  }
}

@media (max-width: 900px) {
  .commodity-grid {
    grid-template-columns: repeat(2, 1fr); /* 移动端强制每行2个卡片 */
    gap: calc(12px * var(--mobile-scale, 1));
    padding: calc(16px * var(--mobile-scale, 1)) 0;
  }
  
  .loading-state {
    grid-template-columns: repeat(2, 1fr); /* 移动端强制每行2个卡片 */
    gap: calc(12px * var(--mobile-scale, 1));
  }
  
  .skeleton-image {
    height: 160px;
  }
}

</style>
