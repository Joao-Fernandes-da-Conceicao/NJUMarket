<template>
  <div class="commodity-card" @click="handleClick">
    <div class="commodity-card__image-wrapper">
      <img
        v-if="commodity.imageUrl"
        :src="commodity.imageUrl"
        :alt="commodity.title"
        class="commodity-card__image"
        @error="onImageError"
      />
      <div v-else class="commodity-card__image-placeholder">
        <el-icon><Picture /></el-icon>
      </div>
      <!-- 已售出标记 -->
      <div v-if="isSold" class="commodity-card__sold-badge">已售出</div>
    </div>
    <div class="commodity-card__info">
      <div class="commodity-card__title">{{ commodity.title || '未知商品' }}</div>
      <div class="commodity-card__price">
        <span class="price-symbol">¥</span>
        <span class="price-value">{{ formatPrice(commodity.price) }}</span>
      </div>
      <div v-if="commodity.status && commodity.status !== 'ON_SHELF'" class="commodity-card__status">
        {{ statusText }}
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { Picture } from '@element-plus/icons-vue'

const props = defineProps({
  commodity: {
    type: Object,
    required: true
  }
})

const emit = defineEmits(['click'])

const isSold = computed(() => props.commodity.status === 'SOLD')

const statusText = computed(() => {
  const map = {
    SOLD: '已售出',
    OFF_SHELF: '已下架',
    DRAFT: '草稿',
  }
  return map[props.commodity.status] || props.commodity.status
})

function formatPrice(price) {
  if (price == null) return '--'
  return Number(price).toFixed(2)
}

function handleClick() {
  if (props.commodity.commodityId) {
    emit('click', props.commodity.commodityId)
  }
}

function onImageError(e) {
  e.target.style.display = 'none'
}
</script>

<style scoped>
.commodity-card {
  display: flex;
  align-items: center;
  background: #fff;
  border: 1px solid #e8e8e8;
  border-radius: 8px;
  padding: 8px;
  cursor: pointer;
  max-width: 240px;
  gap: 10px;
  transition: box-shadow 0.2s;
  margin-top: 6px;
}

.commodity-card:hover {
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.12);
}

.commodity-card__image-wrapper {
  position: relative;
  flex-shrink: 0;
  width: 64px;
  height: 64px;
  border-radius: 6px;
  overflow: hidden;
  background: #f5f5f5;
  display: flex;
  align-items: center;
  justify-content: center;
}

.commodity-card__image {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.commodity-card__image-placeholder {
  font-size: 24px;
  color: #ccc;
}

.commodity-card__sold-badge {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  background: rgba(0, 0, 0, 0.55);
  color: #fff;
  font-size: 11px;
  text-align: center;
  padding: 2px 0;
}

.commodity-card__info {
  flex: 1;
  min-width: 0;
}

.commodity-card__title {
  font-size: 13px;
  color: #333;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  line-height: 1.4;
}

.commodity-card__price {
  margin-top: 4px;
  color: #e4393c;
  font-weight: 600;
  font-size: 14px;
}

.price-symbol {
  font-size: 11px;
}

.commodity-card__status {
  margin-top: 3px;
  font-size: 11px;
  color: #999;
}
</style>
