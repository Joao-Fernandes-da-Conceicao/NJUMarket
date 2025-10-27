<template>
  <div class="commodity-card" @click="handleClick">
    <div class="card-main">
      <div class="card-image" v-if="commodity.images && commodity.images.length > 0">
        <img :src="getImageUrl(commodity.images[0])" :alt="commodity.title" />
      </div>
      <div class="card-content">
        <h4 class="card-title">{{ commodity.title }}</h4>
        <div class="card-price">¥{{ formatPrice(commodity.price) }}</div>
        <div class="card-tags">
          <UnifiedTag size="small" v-if="commodity.category">{{ commodity.category }}</UnifiedTag>
          <UnifiedTag size="small" v-if="commodity.conditionLevel">{{ commodity.conditionLevel }}</UnifiedTag>
        </div>
      </div>
    </div>
    <div class="card-seller" v-if="sellerProfile || commodity.sellerId">
      <div class="seller-avatar" v-if="sellerProfile?.avatar">
        <img :src="getAvatarUrl(sellerProfile.avatar)" :alt="sellerProfile.nickname || commodity.sellerId" />
      </div>
      <span class="seller-name">{{ sellerProfile?.nickname || commodity.sellerId }}</span>
    </div>
  </div>
</template>

<script setup>
/* global defineProps, defineEmits */
import { ref, onMounted } from 'vue'
import { imageAPI, profileAPI } from '../../api'
import { formatPrice } from '../../utils/formatUtils'
import UnifiedTag from '../common/UnifiedTag.vue'

const props = defineProps({
  commodity: {
    type: Object,
    required: true
  }
})

const emit = defineEmits(['click'])

const sellerProfile = ref(null)

// 获取卖家 profile 信息
const fetchSellerProfile = async () => {
  if (!props.commodity.sellerId) return
  
  try {
    const response = await profileAPI.getUser(props.commodity.sellerId)
    if (response.success && response.data) {
      sellerProfile.value = response.data
    }
  } catch (error) {
    console.error('获取卖家profile失败:', error)
  }
}

onMounted(() => {
  // 始终请求 profile 以获取最新的昵称和头像
  if (props.commodity.sellerId) {
    fetchSellerProfile()
  }
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
  return `http://localhost:8080/uploads/avatars/${avatar}`
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

