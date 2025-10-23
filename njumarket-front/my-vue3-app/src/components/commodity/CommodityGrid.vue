<template>
  <div class="commodity-grid">
    <div 
      v-for="commodity in commodities" 
      :key="commodity.commodityId"
      class="grid-item"
    >
      <CommodityCard 
        :commodity="commodity"
        :show-seller-info="showSellerInfo"
        @click="handleCommodityClick"
      />
    </div>
    
    <!-- 空状态 -->
    <div v-if="commodities.length === 0 && !loading" class="empty-state">
      <el-empty :description="emptyText">
        <el-button v-if="showEmptyAction" type="primary" @click="handleEmptyAction">
          {{ emptyActionText }}
        </el-button>
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
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 24px;
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
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 24px;
}

.skeleton-card {
  background: white;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  overflow: hidden;
}

.skeleton-image {
  width: 100%;
  height: 200px;
  background: linear-gradient(90deg, #f0f0f0 25%, #e0e0e0 50%, #f0f0f0 75%);
  background-size: 200% 100%;
  animation: loading 1.5s infinite;
}

.skeleton-content {
  padding: 16px;
}

.skeleton-title {
  height: 20px;
  background: linear-gradient(90deg, #f0f0f0 25%, #e0e0e0 50%, #f0f0f0 75%);
  background-size: 200% 100%;
  animation: loading 1.5s infinite;
  border-radius: 4px;
  margin-bottom: 12px;
}

.skeleton-price {
  height: 18px;
  width: 80px;
  background: linear-gradient(90deg, #f0f0f0 25%, #e0e0e0 50%, #f0f0f0 75%);
  background-size: 200% 100%;
  animation: loading 1.5s infinite;
  border-radius: 4px;
  margin-bottom: 8px;
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
@media (max-width: 768px) {
  .commodity-grid {
    grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
    gap: 16px;
    padding: 16px 0;
  }
  
  .loading-state {
    grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
    gap: 16px;
  }
  
  .skeleton-image {
    height: 160px;
  }
}

@media (max-width: 480px) {
  .commodity-grid {
    grid-template-columns: 1fr;
    gap: 12px;
  }
  
  .loading-state {
    grid-template-columns: 1fr;
    gap: 12px;
  }
}
</style>
