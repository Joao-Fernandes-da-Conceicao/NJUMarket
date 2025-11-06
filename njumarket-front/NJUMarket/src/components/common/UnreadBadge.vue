<!--
  统一未读角标组件
  统一管理角标的样式、渲染和计数逻辑
  预留扩展空间用于订单变化提醒等场景
-->
<template>
  <!-- 数字角标模式 -->
  <el-badge
    v-if="type === 'number'"
    :value="displayValue"
    :hidden="shouldHide"
    :max="max"
    :class="['unread-badge', 'unread-badge-number', badgeClass]"
  >
    <slot></slot>
  </el-badge>

  <!-- 点状角标模式 -->
  <span
    v-else-if="type === 'dot'"
    :class="['unread-badge', 'unread-badge-dot', { 'has-unread': !shouldHide }, badgeClass]"
  >
    <slot></slot>
  </span>

  <!-- 文本角标模式（用于面板头部等） -->
  <span
    v-else-if="type === 'text'"
    v-show="!shouldHide"
    :class="['unread-badge', 'unread-badge-text', badgeClass]"
  >
    {{ displayValue }}
  </span>

  <!-- 默认模式：无角标内容，仅作为容器 -->
  <span
    v-else
    :class="['unread-badge', 'unread-badge-default', badgeClass]"
  >
    <slot></slot>
  </span>
</template>

<script setup>
/* global defineProps */
import { computed } from 'vue'

const props = defineProps({
  // 未读数量
  count: {
    type: Number,
    default: 0
  },
  // 角标类型：'number'（数字角标，Element Plus el-badge）、'dot'（点状角标）、'text'（文本角标）、'default'（仅容器）
  type: {
    type: String,
    default: 'number',
    validator: (value) => ['number', 'dot', 'text', 'default'].includes(value)
  },
  // 最大显示数字（超过后显示 max+，仅对 number 类型有效）
  max: {
    type: Number,
    default: 99
  },
  // 是否强制隐藏（即使 count > 0）
  forceHide: {
    type: Boolean,
    default: false
  },
  // 自定义类名
  badgeClass: {
    type: String,
    default: ''
  }
})

// 计算显示值
const displayValue = computed(() => {
  if (props.count <= 0) return 0
  if (props.type === 'number' && props.count > props.max) {
    return `${props.max}+`
  }
  return props.count
})

// 是否应该隐藏
const shouldHide = computed(() => {
  return props.forceHide || props.count <= 0
})
</script>

<style scoped>
/* 基础样式 */
.unread-badge {
  position: relative;
  display: inline-block;
}

/* 数字角标样式（使用 Element Plus 的 el-badge，这里主要处理自定义样式） */
.unread-badge-number {
  /* Element Plus el-badge 的样式会自动应用 */
  /* 如需添加自定义样式，可在此处添加 */
}

/* 点状角标样式（默认：右上角） */
.unread-badge-dot.has-unread::after {
  content: '';
  position: absolute;
  top: 0;
  right: 0;
  width: 12px;
  height: 12px;
  background-color: #f56c6c;
  border-radius: 50%;
  border: 2px solid white;
  z-index: 1;
}

/* ✅ v1.3.x: 手机端订单提醒角标特殊样式（垂直居中 + 右移更多，避免遮挡文字） */
.unread-badge-dot.mobile-order-badge.has-unread::after {
  top: 50%;
  right: -16px;
  transform: translateY(-50%);
}

/* 文本角标样式（用于面板头部等场景） */
.unread-badge-text {
  background-color: #f56c6c;
  color: white;
  padding: 2px 8px;
  border-radius: 10px;
  font-size: 12px;
  font-weight: normal;
  margin-left: 8px;
}

/* 默认模式：仅容器，不添加额外样式 */
.unread-badge-default {
  /* 默认模式无需额外样式，保持容器特性 */
}
</style>

<style>
/* 全局样式：统一角标的主题色和样式 */
.unread-badge-number :deep(.el-badge__content) {
  background-color: #f56c6c !important;
  border-color: #f56c6c !important;
  color: white !important;
}

/* 预留：订单变化提醒的样式扩展点 */
.unread-badge-number.order-reminder :deep(.el-badge__content) {
  background-color: #ff9800 !important;
  border-color: #ff9800 !important;
}

.unread-badge-text.order-reminder {
  background-color: #ff9800;
}
</style>
