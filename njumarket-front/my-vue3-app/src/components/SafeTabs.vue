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
import { ref } from 'vue'

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
    
    return {
      tabsKey,
      handleTabChange
    }
  }
}
</script>
