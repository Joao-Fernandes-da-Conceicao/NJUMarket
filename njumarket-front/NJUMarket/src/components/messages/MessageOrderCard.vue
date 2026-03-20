<template>
  <div class="order-card" @click="handleClick">
    <div class="order-card__icon">
      <el-icon><ShoppingCart /></el-icon>
    </div>
    <div class="order-card__info">
      <div class="order-card__label">订单</div>
      <div class="order-card__id">{{ shortOrderId }}</div>
      <div v-if="displayStatus" class="order-card__status" :class="statusClass">
        {{ displayStatus }}
      </div>
    </div>
    <div class="order-card__arrow">
      <el-icon><ArrowRight /></el-icon>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { ShoppingCart, ArrowRight } from '@element-plus/icons-vue'

const props = defineProps({
  orderId: {
    type: String,
    required: true
  },
  // 由父组件从 orderStore.orderStatusCache 注入，ORDER_CHANGE 推送驱动更新
  currentStatus: {
    type: String,
    default: null
  }
})

const emit = defineEmits(['click'])

const shortOrderId = computed(() => {
  if (!props.orderId) return '--'
  return props.orderId.length > 16 ? '...' + props.orderId.slice(-12) : props.orderId
})

const STATUS_TEXT_MAP = {
  CREATED: '待付款',
  PAID: '待发货',
  SHIPPED: '已发货',
  COMPLETED: '已完成',
  CANCELLED: '已取消',
  REFUND_REQUESTED: '退款申请中',
  REFUNDED: '已退款',
}

const displayStatus = computed(() => {
  if (!props.currentStatus) return null
  return STATUS_TEXT_MAP[props.currentStatus] || props.currentStatus
})

const statusClass = computed(() => {
  const map = {
    CREATED: 'status--pending',
    PAID: 'status--paid',
    SHIPPED: 'status--shipped',
    COMPLETED: 'status--done',
    CANCELLED: 'status--cancelled',
    REFUND_REQUESTED: 'status--refund',
    REFUNDED: 'status--done',
  }
  return map[props.currentStatus] || ''
})

function handleClick() {
  emit('click', props.orderId)
}
</script>

<style scoped>
.order-card {
  display: flex;
  align-items: center;
  background: #fff;
  border: 1px solid #e8e8e8;
  border-radius: 8px;
  padding: 8px 10px;
  cursor: pointer;
  max-width: 220px;
  gap: 8px;
  transition: box-shadow 0.2s;
  margin-top: 6px;
}

.order-card:hover {
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.12);
}

.order-card__icon {
  font-size: 22px;
  color: #409eff;
  flex-shrink: 0;
}

.order-card__info {
  flex: 1;
  min-width: 0;
}

.order-card__label {
  font-size: 12px;
  color: #999;
}

.order-card__id {
  font-size: 12px;
  color: #666;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  margin-top: 2px;
}

.order-card__arrow {
  color: #ccc;
  font-size: 14px;
  flex-shrink: 0;
}

.order-card__status {
  margin-top: 3px;
  font-size: 11px;
  font-weight: 500;
}

.status--pending  { color: #e6a23c; }
.status--paid     { color: #409eff; }
.status--shipped  { color: #67c23a; }
.status--done     { color: #909399; }
.status--cancelled { color: #f56c6c; }
.status--refund   { color: #e6a23c; }
</style>
