<template>
  <div class="order-tabs">
    <SafeTabs 
      v-model="activeTab" 
      @tab-change="handleTabChange"
    >
      <el-tab-pane label="全部" name="all"></el-tab-pane>
      <el-tab-pane label="待支付" name="CREATED"></el-tab-pane>
      <el-tab-pane label="待发货" name="PAID"></el-tab-pane>
      <el-tab-pane label="待收货" name="SHIPPED"></el-tab-pane>
      <el-tab-pane label="已完成" name="COMPLETED"></el-tab-pane>
      <el-tab-pane label="已取消" name="CANCELLED"></el-tab-pane>
      <el-tab-pane 
        v-if="showRefundTabs" 
        :label="refundLabel" 
        name="REFUND_REQUESTED"
      ></el-tab-pane>
      <el-tab-pane label="退款完成" name="REFUND_APPROVED"></el-tab-pane>
      <el-tab-pane 
        v-if="showRefundRejected"
        label="退款被拒" 
        name="REFUND_REJECTED"
      ></el-tab-pane>
    </SafeTabs>
  </div>
</template>

<script setup>
import { defineProps, defineEmits, ref, watch } from 'vue'
import SafeTabs from '../SafeTabs.vue'
import { ElTabPane } from 'element-plus'

const props = defineProps({
  modelValue: {
    type: String,
    required: true
  },
  showRefundTabs: {
    type: Boolean,
    default: true
  },
  showRefundRejected: {
    type: Boolean,
    default: false
  },
  refundLabel: {
    type: String,
    default: '退款申请'
  }
})

const emit = defineEmits(['update:modelValue', 'tab-change'])

const activeTab = ref(props.modelValue)

const handleTabChange = (tab) => {
  activeTab.value = tab
  emit('update:modelValue', tab)
  emit('tab-change', tab)
}

// 同步外部值变化
watch(() => props.modelValue, (newValue) => {
  activeTab.value = newValue
})
</script>

<style scoped>
.order-tabs {
  margin-bottom: 24px;
}
</style>

