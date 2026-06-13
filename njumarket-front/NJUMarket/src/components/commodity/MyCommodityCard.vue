<template>
  <div class="commodity-card">
    <!-- 商品图片区域 - 顶部 -->
    <div class="commodity-image">
      <img 
        v-if="commodity.images && commodity.images.length > 0"
        :src="getCommodityImageUrl(commodity.images[0])" 
        :alt="commodity.title"
        @click="handleClick"
      />
      <div v-else class="no-image">
        <el-icon size="48"><Picture /></el-icon>
        <span>暂无照片</span>
      </div>
      
      <!-- 商品状态标签 -->
      <div class="status-badge">
        <UnifiedTag 
          :type="getStatusType(commodity.commodityStatus)"
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
        {{ getLocationDisplay(commodity) }}
        <span class="stat-divider">|</span>
        <el-icon><View /></el-icon>
        {{ commodity.clickCount || 0 }}
        <span class="stat-divider">|</span>
        <el-icon><ShoppingBag /></el-icon>
        {{ commodity.stock || 0 }}
      </span>
    </div>
    
    <!-- 商品信息区域 -->
    <div class="commodity-info">
      <!-- 标题 -->
      <h3 class="commodity-title" :title="commodity.title" @click="handleClick">
        {{ commodity.title }}
      </h3>
      
      <!-- 描述 -->
      <p v-if="commodity.description" class="commodity-description">
        {{ commodity.description }}
      </p>
      
      <!-- 标签（分类和成色） -->
      <div v-if="commodity.category || commodity.conditionLevel" class="commodity-tags">
        <UnifiedTag v-if="commodity.category" size="small" type="info">
          {{ commodity.category }}
        </UnifiedTag>
        <UnifiedTag v-if="commodity.conditionLevel" size="small" type="warning">
          {{ commodity.conditionLevel }}
        </UnifiedTag>
      </div>
      
      <!-- 价格 -->
      <p class="commodity-price">
        ¥{{ formatPriceValue(commodity.price) }}
      </p>
    </div>
    
    <!-- 操作按钮区域（仅当 showActions 为 true 时显示） -->
    <div v-if="showActions" class="commodity-actions">
      <div class="action-buttons">
        <!-- 草稿商品：发布按钮 -->
        <UnifiedButton
          v-if="commodity.commodityStatus === 'DRAFT'"
          type="primary"
          @click="$emit('publish', commodity.commodityId)"
        >
          发布
        </UnifiedButton>
        
        <!-- 已发布商品：上架按钮 -->
        <UnifiedButton
          v-if="commodity.commodityStatus === 'PUBLISHED'"
          type="success"
          @click="$emit('shelf', commodity.commodityId)"
        >
          上架
        </UnifiedButton>
        
        <!-- 已上架商品：下架按钮 -->
        <UnifiedButton
          v-if="commodity.commodityStatus === 'ON_SHELF'"
          @click="$emit('unshelf', commodity.commodityId)"
        >
          下架
        </UnifiedButton>
        
        <!-- 已下架商品：重新上架按钮 -->
        <UnifiedButton
          v-if="commodity.commodityStatus === 'OFF_SHELF'"
          type="primary"
          @click="$emit('republish', commodity.commodityId)"
        >
          重新上架
        </UnifiedButton>
        
        <!-- 所有商品：编辑按钮 -->
        <UnifiedButton @click="$emit('edit', commodity.commodityId)">
          编辑
        </UnifiedButton>
        
        <UnifiedButton
          type="danger"
          @click="$emit('delete', commodity.commodityId)"
        >
          删除
        </UnifiedButton>
      </div>
    </div>
  </div>
</template>

<script setup>
import { defineProps, defineEmits } from 'vue'
import { imageAPI } from '../../api'
import { formatPrice } from '../../utils/formatUtils'
import { Picture, Location, View, ShoppingBag } from '@element-plus/icons-vue'
import UnifiedButton from '../common/UnifiedButton.vue'
import UnifiedTag from '../common/UnifiedTag.vue'

const props = defineProps({
  commodity: {
    type: Object,
    required: true
  },
  // 是否显示操作按钮（编辑、删除、发布等），默认显示
  showActions: {
    type: Boolean,
    default: true
  }
})

const emit = defineEmits(['publish', 'shelf', 'unshelf', 'republish', 'edit', 'delete', 'click'])

// 获取商品图片URL
const getCommodityImageUrl = (imageUrl) => {
  if (!imageUrl) return ''
  
  // 如果已经是完整URL，直接返回
  if (imageUrl.startsWith('http')) return imageUrl
  
  // 构建商品图片URL
  return imageAPI.getCommodityImage(imageUrl)
}

// 获取状态类型
const getStatusType = (status) => {
  const statusMap = {
    'DRAFT': 'info',
    'PUBLISHED': 'warning',
    'ON_SHELF': 'success',
    'OFF_SHELF': 'danger',
    'DELETED': 'danger'
  }
  return statusMap[status] || 'info'
}

// 获取状态文本
const getStatusText = (status) => {
  const statusMap = {
    'DRAFT': '草稿',
    'PUBLISHED': '已发布',
    'ON_SHELF': '已上架',
    'OFF_SHELF': '已下架',
    'DELETED': '已删除'
  }
  return statusMap[status] || status
}

// 格式化价格（使用导入的 formatPrice，但需要确保是数字类型）
const formatPriceValue = (price) => {
  if (price == null) return '0.00'
  const numPrice = typeof price === 'number' ? price : parseFloat(price)
  return formatPrice(numPrice || 0)
}

// 获取位置显示文本（省+市）
const getLocationDisplay = (commodity) => {
  // 优先使用地址快照的省+市
  if (commodity.addressSnapshotProvince && commodity.addressSnapshotCity) {
    return `${commodity.addressSnapshotProvince}${commodity.addressSnapshotCity}`
  }
  // 如果只有省或只有市，也显示
  if (commodity.addressSnapshotProvince) {
    return commodity.addressSnapshotProvince
  }
  if (commodity.addressSnapshotCity) {
    return commodity.addressSnapshotCity
  }
  // 最后回退到旧字段 location（兼容旧数据）
  return commodity.location || '未设置位置'
}

// 商品点击处理
const handleClick = () => {
  emit('click', props.commodity)
}
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
  margin-bottom: 24px;
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
  border-radius: 12px;
  margin-bottom: 12px;
}

.commodity-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.3s ease;
  cursor: pointer;
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
  z-index: 1;
}

/* border-radius 由 UnifiedTag 统一管理（9999px） */

/* 统计信息 - 图片正下方，一行平铺 */
.commodity-stats {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 6px;
  padding: 0 12px;
  margin-bottom: 8px;
  flex-wrap: nowrap;
}

.stat-item {
  font-size: 12px;
  color: var(--primary-color);
  display: flex;
  align-items: center;
  gap: 4px;
  white-space: nowrap;
}

.stat-divider {
  color: #ddd;
  font-size: 12px;
  padding: 0 2px;
}

/* 商品信息区域 */
.commodity-info {
  padding: 0 12px 0;
  display: flex;
  flex-direction: column;
}

.commodity-title {
  font-size: 25px;
  font-weight: normal;
  margin: 0 0 8px;
  color: var(--primary-color);
  line-height: 1.3;
  display: -webkit-box;
  line-clamp: 2;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  text-align: center;
  cursor: pointer;
  transition: color 0.3s ease;
}

.commodity-title:hover {
  color: var(--primary-light);
}

/* 描述 */
.commodity-description {
  font-size: 12px;
  color: var(--primary-color);
  margin: 0 0 8px 0;
  display: -webkit-box;
  line-clamp: 2;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  text-align: center;
}

/* 标签 */
.commodity-tags {
  display: flex;
  justify-content: center;
  gap: 8px;
  margin-bottom: 8px;
  flex-wrap: wrap;
}

/* border-radius 由 UnifiedTag 统一管理（9999px） */
.commodity-tags :deep(.unified-tag) {
  font-size: 10px;
  padding: 2px 8px;
}

.commodity-price {
  font-size: 30px;
  font-weight: normal;
  margin: 0 0 8px 0;
  color: var(--primary-color);
  text-align: center;
}

/* 操作按钮区域 */
.commodity-actions {
  padding: 8px 12px 12px;
  border-top: 1px solid #f0f0f0;
  margin-top: 8px;
}

.action-buttons {
  display: flex;
  flex-direction: row;
  flex-wrap: wrap;
  gap: 8px;
  justify-content: center;
}

.action-buttons .el-button {
  border-radius: 20px; /* 药丸形按钮 */
  font-weight: normal;
  margin-left: 0 !important;
  flex: 1;
  min-width: calc(33.333% - 6px);
  max-width: calc(33.333% - 6px);
}

/* 响应式设计 */
@media (max-width: 900px) {
  .commodity-image {
    aspect-ratio: 3 / 2;
    border-radius: 10px;
    margin-bottom: 8px;
  }
  
  .commodity-info {
    padding: 0 8px 0;
  }
  
  .commodity-actions {
    padding: 6px 8px 8px;
    margin-top: 6px;
  }
  
  .action-buttons {
    flex-direction: row;
    flex-wrap: wrap;
    justify-content: center;
  }
  
  .action-buttons .el-button {
    flex: 1;
    min-width: calc(50% - 4px);
    max-width: none;
  }
}
</style>
