<template>
  <el-input
    :model-value="modelValue"
    @update:model-value="onUpdate"
    v-bind="attrs"
    :class="inputClass"
    @keyup="$emit('keyup', $event)"
    @keydown="$emit('keydown', $event)"
    @keypress="$emit('keypress', $event)"
  >
    <template v-for="(_, name) in slots" v-slot:[name]="slotProps">
      <slot :name="name" v-bind="slotProps" />
    </template>
  </el-input>
</template>

<script setup>
/* global defineProps, defineEmits */
import { computed, useAttrs, useSlots } from 'vue'

defineProps({
  modelValue: {
    type: [String, Number],
    default: ''
  }
})
const emit = defineEmits(['update:modelValue','keyup','keydown','keypress'])

const attrs = useAttrs()
const slots = useSlots()

const isTextarea = computed(() => (attrs.type === 'textarea'))

const inputClass = computed(() => {
  return [
    attrs.class,
    'unified-input',
    isTextarea.value ? 'unified-input--textarea' : 'unified-input--pill'
  ]
})

const onUpdate = (val) => {
  emit('update:modelValue', val)
}
</script>

<style scoped>
/* 基础：统一边框与形状（使用 :deep 作用到 el-input 包裹层） */
.unified-input :deep(.el-input__wrapper) {
  border: 1px solid var(--primary-color);
  transition: all 0.2s ease;
}

/* 非多行：药丸型 */
.unified-input--pill :deep(.el-input__wrapper) {
  border-radius: 9999px;
}

/* 多行：圆角矩形 + 默认边框 */
.unified-input--textarea :deep(.el-textarea__inner) {
  border: 1px solid var(--primary-color);
  border-radius: 16px;
  transition: all 0.2s ease;
}
.unified-input--textarea :deep(.el-input__wrapper) {
  border-radius: 16px;
}

/* 悬停/聚焦边框加深（单行与多行） */
.unified-input :deep(.is-focus),
.unified-input :deep(.el-input__wrapper:hover) {
  border-color: var(--primary-light);
}
.unified-input--textarea :deep(.el-textarea__inner:hover),
.unified-input--textarea :deep(.el-textarea__inner:focus) {
  border-color: var(--primary-light);
  outline: none;
}
</style>
