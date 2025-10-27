<!--
  统一标签组件
  继承 Element Plus 的 el-tag，统一设置 border-radius: 9999px（完全圆形/药丸形）
  保持所有其他属性和样式不变
-->
<template>
  <el-tag
    v-bind="$attrs"
    :class="tagClass"
  >
    <slot></slot>
  </el-tag>
</template>

<script setup>
import { computed } from 'vue'

// 添加统一的样式类，直接应用到 el-tag 上
const tagClass = computed(() => {
  return ['unified-tag']
})
</script>

<style scoped>
/* 统一样式：完全圆形/药丸形 - 直接作用于 el-tag */
.unified-tag {
  border-radius: 9999px !important;
}
</style>

<style>
/* 全局样式：确保在所有情况下都应用药丸形，覆盖 Element Plus 默认样式 */
.el-tag.unified-tag,
.unified-tag.el-tag,
.unified-tag.el-tag--default,
.unified-tag.el-tag--primary,
.unified-tag.el-tag--success,
.unified-tag.el-tag--info,
.unified-tag.el-tag--warning,
.unified-tag.el-tag--danger,
.unified-tag.el-tag--small,
.unified-tag.el-tag--large,
.unified-tag.el-tag.is-round {
  border-radius: 9999px !important;
}
</style>

