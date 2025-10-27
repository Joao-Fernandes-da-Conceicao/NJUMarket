<template>
  <el-tabs 
    :model-value="modelValue" 
    @tab-change="handleTabChange"
  >
    <slot></slot>
  </el-tabs>
</template>

<script>
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
    const handleTabChange = (tabName) => {
      emit('update:modelValue', tabName)
      emit('tab-change', tabName)
    }
    
    return {
      handleTabChange
    }
  }
}
</script>
