<template>
  <div class="commodity-card" @click="handleClick">
    <div class="card-main">
      <div class="card-image" v-if="commodityImages.length > 0">
        <img :src="getImageUrl(commodityImages[0])" :alt="commodity.title" />
      </div>
      <div class="card-content">
        <h4 class="card-title">{{ commodity.title }}</h4>
        <div class="card-price">¥{{ formatPrice(commodity.price) }}</div>
        <div class="card-tags">
          <UnifiedTag size="small" type="info" v-if="commodity.category">{{ commodity.category }}</UnifiedTag>
          <UnifiedTag size="small" type="warning" v-if="commodity.conditionLevel">{{ commodity.conditionLevel }}</UnifiedTag>
          <UnifiedTag size="small" type="danger" v-if="commodity.commodityStatus === 'SOLD_OUT'">已售完</UnifiedTag>
          <UnifiedTag size="small" type="warning" v-if="commodity.commodityStatus === 'OFF_SHELF'">已下架</UnifiedTag>
        </div>
      </div>
    </div>
    <div class="card-seller" v-if="sellerName || commodity.sellerId">
      <div class="seller-avatar" v-if="sellerAvatar">
        <img :src="getAvatarUrl(sellerAvatar)" :alt="sellerName" />
      </div>
      <span class="seller-name">{{ sellerName }}</span>
    </div>
  </div>
</template>

<script setup>
/* global defineProps, defineEmits */
import { computed } from 'vue'
import { imageAPI } from '../../api'
import { formatPrice } from '../../utils/formatUtils'
import UnifiedTag from '../common/UnifiedTag.vue'

const props = defineProps({
  commodity: {
    type: Object,
    required: true
  }
})

const emit = defineEmits(['click'])

// ✅ 使用批量查询返回的卖家信息，避免 N+1 查询
// 批量查询接口已返回 sellerNickname 和 sellerAvatar
const sellerName = computed(() => {
  return props.commodity.sellerNickname || props.commodity.sellerId || ''
})

const sellerAvatar = computed(() => {
  return props.commodity.sellerAvatar || ''
})

// ✅ 处理图片数组（兼容不同格式）
const commodityImages = computed(() => {
  if (Array.isArray(props.commodity.images)) {
    return props.commodity.images
  }
  // 兼容旧格式：单个 image 字段
  if (props.commodity.image) {
    return [props.commodity.image]
  }
  return []
})

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
  return `http://localhost:8080/api/images/avatars/${avatar}`
}

const handleClick = () => {
  emit('click', props.commodity)
}
</script>

<style scoped>
.commodity-card {
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

.commodity-card:hover {
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

.card-tags {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
}

.card-seller {
  display: flex;
  align-items: center;
  gap: 6px;
  padding-top: 8px;
  border-top: 1px solid #e8e8e8;
  font-size: 12px;
  color: #666;
  width: 100%;
}

.seller-avatar {
  width: 20px;
  height: 20px;
  border-radius: 50%;
  overflow: hidden;
  background: #f0f0f0;
  flex-shrink: 0;
}

.seller-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.seller-name {
  color: #333;
  font-weight: normal;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>

