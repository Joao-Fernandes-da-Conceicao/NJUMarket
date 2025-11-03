<template>
  <div class="custom-page-size-select custom-select" @click.stop="toggle" @blur="handleBlur" tabindex="0">
    <span class="select-value">{{ displayLabel }}</span>
    <el-icon class="select-icon"><ArrowDown /></el-icon>

    <div v-if="open" class="select-popup" @click.stop>
      <div
        v-for="opt in normalizedOptions"
        :key="opt.value"
        class="popup-option"
        @click.stop="select(opt.value)"
      >
        {{ opt.label }}
      </div>
    </div>
  </div>
</template>

<script setup>
/* global defineProps, defineEmits */
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { ArrowDown } from '@element-plus/icons-vue'

// v-model
const props = defineProps({
  modelValue: { type: [String, Number], default: '' },
  options: { type: Array, default: () => [] }, // 支持 ['全部','A'] 或 [{label, value}] 两种
  // 用于显示当前值的文本（可选）。若不提供，则从 options 匹配
  placeholder: { type: String, default: '' }
})

const emit = defineEmits(['update:modelValue', 'change'])

const open = ref(false)

const normalizedOptions = computed(() => {
  return props.options.map(opt => {
    if (typeof opt === 'string') return { label: opt, value: opt }
    return { label: opt.label, value: opt.value }
  })
})

const displayLabel = computed(() => {
  const cur = normalizedOptions.value.find(o => o.value === props.modelValue)
  return cur ? cur.label : (props.placeholder || '')
})

const toggle = () => {
  open.value = !open.value
}

const handleBlur = () => {
  setTimeout(() => {
    open.value = false
  }, 200)
}

const onClickOutside = (e) => {
  const target = e.target
  if (!target.closest('.custom-page-size-select')) {
    open.value = false
  }
}

onMounted(() => {
  document.addEventListener('click', onClickOutside)
})

onUnmounted(() => {
  document.removeEventListener('click', onClickOutside)
})

const select = (val) => {
  emit('update:modelValue', val)
  emit('change', val)
  open.value = false
}
</script>

<style scoped>
/* 统一高度 34px */
.custom-page-size-select.custom-select {
  height: 34px;
  min-height: 34px;
  box-sizing: border-box;
  position: relative; /* 确保弹窗定位基准 */
}

/* 文字靠左 */
.select-value {
  display: flex;
  align-items: center;
  justify-content: flex-start;
  text-align: left;
  height: 100%;
}
</style>

<style>
/* ✅ 弹窗样式必须放在全局样式（非scoped），因为表格可能裁剪弹窗 */
.custom-page-size-select .select-popup {
  position: absolute;
  top: calc(100% + 4px);
  left: 0;
  right: 0;
  background: white;
  border: 1px solid var(--primary-color, #6a015e);
  border-radius: 16px;
  box-shadow: 0 4px 16px rgba(106, 1, 94, 0.2);
  z-index: 9999; /* 确保高于表格层级 */
  margin-top: 0;
  padding: 8px;
  min-width: 100%;
  max-height: 300px;
  overflow-y: auto;
}

.custom-page-size-select .popup-option {
  padding: 8px 12px;
  margin: 2px 0;
  border-radius: 20px;
  color: var(--primary-color, #6a015e);
  font-size: 14px;
  font-weight: normal;
  cursor: pointer;
  transition: all 0.3s ease;
  text-align: left; /* 选项文字也靠左 */
}

.custom-page-size-select .popup-option:hover {
  background-color: var(--primary-color, #6a015e);
  color: white;
}
</style>
