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
/* 文字居中 */
.select-value {
  display: flex;
  align-items: center;
  justify-content: center;
  text-align: center;
}
</style>
