<!--
  统一按钮组件
  继承 Element Plus 的 el-button，统一设置 border-radius: 9999px（完全圆形/药丸形）
  保持所有其他属性和样式不变
-->
<template>
  <el-button
    v-bind="$attrs"
    :class="buttonClass"
  >
    <slot></slot>
  </el-button>
</template>

<script setup>
import { computed } from 'vue'

// 添加统一的样式类，直接应用到 el-button 上
const buttonClass = computed(() => {
  return ['unified-button']
})
</script>

<style scoped>
/* 统一样式：完全圆形/药丸形 - 直接作用于 el-button */
.unified-button {
  border-radius: 9999px !important;
}
</style>

<style>
/* 全局样式：确保在所有情况下都应用药丸形，覆盖 Element Plus 默认样式 */
.el-button.unified-button,
.unified-button.el-button,
.unified-button.el-button--default,
.unified-button.el-button--primary,
.unified-button.el-button--success,
.unified-button.el-button--info,
.unified-button.el-button--warning,
.unified-button.el-button--danger,
.unified-button.el-button--small,
.unified-button.el-button--large,
.unified-button.el-button.is-disabled,
.unified-button.el-button.is-circle,
.unified-button.el-button.is-round {
  border-radius: 9999px !important;
}

/* primary：主题色底 + 白字 */
.unified-button.el-button--primary {
  border: 1px solid var(--primary-color);
  background-color: var(--primary-color);
  color: #ffffff;
}
.unified-button.el-button--primary:hover {
  background-color: var(--primary-light);
  border-color: var(--primary-light);
}

/* 覆盖“未设置类型”的默认按钮（排除其他类型/风格）依旧为白底紫边 */
.unified-button:not(.el-button--primary):not(.el-button--success):not(.el-button--warning):not(.el-button--danger):not(.el-button--info):not(.is-text):not(.is-link):not(.is-plain) {
  border: 1px solid var(--primary-color);
  background-color: #ffffff;
  color: var(--primary-color);
}
.unified-button:not(.el-button--primary):not(.el-button--success):not(.el-button--warning):not(.el-button--danger):not(.el-button--info):not(.is-text):not(.is-link):not(.is-plain):hover {
  background-color: rgba(106, 1, 94, 0.05);
  border-color: var(--primary-light);
}
</style>

