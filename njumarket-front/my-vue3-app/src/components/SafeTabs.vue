<template>
  <el-tabs 
    :model-value="modelValue" 
    @tab-change="handleTabChange"
    :key="tabsKey"
  >
    <slot></slot>
  </el-tabs>
</template>

<script>
import { ref, watch, nextTick } from 'vue'

export default {
  name: 'SafeTabs',
  props: {
    modelValue: {
      type: String,
      default: ''
    }
  },
  emits: ['update:modelValue', 'tab-change'],
  setup(props, { emit }) {
    const tabsKey = ref(`safe-tabs-${Date.now()}`)
    
    const handleTabChange = (tabName) => {
      emit('update:modelValue', tabName)
      emit('tab-change', tabName)
    }
    
    // 监听modelValue变化，必要时重新创建组件
    watch(() => props.modelValue, (newVal, oldVal) => {
      if (newVal !== oldVal) {
        nextTick(() => {
          tabsKey.value = `safe-tabs-${Date.now()}-${newVal}`
        })
      }
    })
    
    return {
      tabsKey,
      handleTabChange
    }
  }
}
</script>
