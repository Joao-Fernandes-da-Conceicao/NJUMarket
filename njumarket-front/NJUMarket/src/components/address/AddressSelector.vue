<template>
  <div class="address-selector">
    <el-form-item :label="label" :prop="prop" :required="required">
      <div class="address-selector-content">
        <!-- 地址选择下拉框 -->
        <el-select
          v-model="selectedAddressId"
          :options="addressOptions"
          :placeholder="placeholder"
          @change="handleAddressChange"
          class="address-select"
        />
        
        <!-- 管理地址按钮 -->
        <el-button 
          type="default" 
          size="small" 
          @click="showManageDialog = true"
          class="manage-btn"
        >
          管理地址
        </el-button>
      </div>
      
      <!-- 显示选中的地址信息 -->
      <div v-if="selectedAddress" class="selected-address-info">
        <div class="address-detail">
          <span class="address-label" v-if="selectedAddress.addressLabel">
            {{ getAddressLabelText(selectedAddress.addressLabel) }}
          </span>
          <span class="address-text">{{ selectedAddress.fullAddress }}</span>
          <span v-if="selectedAddress.isDefault" class="default-badge">默认</span>
        </div>
        <div class="address-contact" v-if="showContact">
          <span>{{ selectedAddress.recipientName }}</span>
          <span>{{ selectedAddress.recipientPhone }}</span>
        </div>
      </div>
    </el-form-item>
    
    <!-- 地址管理对话框 -->
    <transition name="address-manager-fade">
      <div 
        v-if="showManageDialog" 
        class="address-manager-modal" 
        role="dialog" 
        aria-modal="true"
      >
        <div class="address-manager-modal__overlay" @click="handleCloseManage"></div>
        <div class="address-manager-modal__panel">
          <div class="address-manager-modal__header">
            <h3>管理地址</h3>
            <el-icon class="modal-close" @click="handleCloseManage">
              <Close />
            </el-icon>
          </div>
          <div class="address-manager-modal__body">
            <AddressManager 
              :is-select-mode="true"
              @address-selected="handleAddressSelected"
              @address-changed="handleAddressChanged"
            />
          </div>
        </div>
      </div>
    </transition>
  </div>
</template>

<script>
import { ref, computed, watch, onMounted } from 'vue'
import { addressAPI } from '../../api'
import { ElMessage } from 'element-plus'
import AddressManager from './AddressManager.vue'
import { Close } from '@element-plus/icons-vue'

export default {
  name: 'AddressSelector',
  components: {
    AddressManager,
    Close
  },
  props: {
    modelValue: {
      type: String,
      default: ''
    },
    label: {
      type: String,
      default: '收货地址'
    },
    prop: {
      type: String,
      default: 'shippingAddressId'
    },
    required: {
      type: Boolean,
      default: true
    },
    placeholder: {
      type: String,
      default: '请选择收货地址'
    },
    showContact: {
      type: Boolean,
      default: true
    }
  },
  emits: ['update:modelValue', 'change'],
  setup(props, { emit }) {
    const selectedAddressId = ref(props.modelValue || '')
    const addresses = ref([])
    const loading = ref(false)
    const showManageDialog = ref(false)
    
    // 地址选项
    const addressOptions = computed(() => {
      const options = addresses.value.map(addr => ({
        label: `${addr.fullAddress}${addr.isDefault ? ' (默认)' : ''}`,
        value: addr.addressId
      }))
      return options
    })
    
    // 选中的地址
    const selectedAddress = computed(() => {
      return addresses.value.find(addr => addr.addressId === selectedAddressId.value)
    })
    
    // 获取地址标签文本
    const getAddressLabelText = (label) => {
      const labelMap = {
        'HOME': '家',
        'SCHOOL': '学校',
        'COMPANY': '公司',
        'OTHER': '其他'
      }
      return labelMap[label] || label
    }
    
    // 加载地址列表
    const loadAddresses = async () => {
      loading.value = true
      try {
        // 不传userId，获取当前登录用户的地址
        const response = await addressAPI.getUserAddresses()
        if (response.success && response.data) {
          // 如果返回的是数组，直接使用；如果是对象，取data字段
          const data = Array.isArray(response.data) ? response.data : (response.data.data || [])
          addresses.value = data.filter(addr => addr.isActive)
          
          // 如果没有选中地址，自动选择默认地址
          if (!selectedAddressId.value && addresses.value.length > 0) {
            const defaultAddr = addresses.value.find(addr => addr.isDefault)
            if (defaultAddr) {
              selectedAddressId.value = defaultAddr.addressId
              emit('update:modelValue', defaultAddr.addressId)
            }
          }
        }
      } catch (error) {
        console.error('加载地址列表失败:', error)
        ElMessage.error('加载地址列表失败')
      } finally {
        loading.value = false
      }
    }
    
    // 地址选择变化
    const handleAddressChange = (addressId) => {
      emit('update:modelValue', addressId)
      emit('change', addressId, selectedAddress.value)
    }
    
    const handleCloseManage = () => {
      showManageDialog.value = false
    }
    
    // 从地址管理面板选择地址
    const handleAddressSelected = (address) => {
      selectedAddressId.value = address.addressId
      emit('update:modelValue', address.addressId)
      emit('change', address.addressId, address)
      showManageDialog.value = false
      // 重新加载地址列表
      loadAddresses()
    }
    
    // 当地址列表发生变化时（创建、编辑、删除、设置默认）
    const handleAddressChanged = () => {
      // 重新加载地址列表以获取最新数据
      loadAddresses()
    }
    
    // 监听对话框关闭事件，确保在关闭时也重新加载地址列表
    watch(() => showManageDialog.value, (isOpen) => {
      // 当对话框关闭时，重新加载地址列表
      if (!isOpen) {
        loadAddresses()
      }
    })
    
    // 监听外部值变化
    watch(() => props.modelValue, (newVal) => {
      if (newVal !== selectedAddressId.value) {
        selectedAddressId.value = newVal
      }
    })
    
    onMounted(() => {
      loadAddresses()
    })
    
    return {
      selectedAddressId,
      addresses,
      loading,
      showManageDialog,
      addressOptions,
      selectedAddress,
      getAddressLabelText,
      handleAddressChange,
      handleAddressSelected,
      handleAddressChanged,
      loadAddresses,
      handleCloseManage
    }
  }
}
</script>

<style scoped>
.address-selector {
  max-width: 640px;
  width: 100%;
}

.address-selector :deep(.el-form-item) {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  width: 100%;
}

.address-selector :deep(.el-form-item__content) {
  width: 100%;
  margin-left: 0 !important;
  display: flex;
  flex-direction: column;
  align-items: stretch;
}

.address-selector-content {
  display: flex;
  gap: 10px;
  align-items: center;
}

.address-select {
  flex: 1;
}

.manage-btn {
  flex-shrink: 0;
}

.selected-address-info {
  margin-top: 10px;
  padding: 10px;
  background-color: #f5f5f5;
  border-radius: 8px;
}

.address-detail {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 5px;
}

.address-label {
  padding: 2px 8px;
  background-color: var(--primary-color);
  color: white;
  border-radius: 4px;
  font-size: 12px;
}

.address-text {
  flex: 1;
  color: #333;
  font-size: 14px;
}

.default-badge {
  padding: 2px 8px;
  background-color: #67c23a;
  color: white;
  border-radius: 4px;
  font-size: 12px;
}

.address-contact {
  display: flex;
  gap: 15px;
  font-size: 12px;
  color: #666;
}

/* 响应式：移动端改为纵向布局 */
@media (max-width: 768px) {
  .address-selector {
    max-width: 100%;
  }

  .address-selector :deep(.el-form-item__label) {
    display: inline-block;
    width: 100%;
    text-align: left;
    margin-bottom: 6px;
  }

  .address-selector-content {
    flex-direction: column;
    align-items: stretch;
  }

  .manage-btn {
    width: 100%;
    justify-content: center;
  }

  .selected-address-info {
    font-size: 13px;
  }

  .address-contact {
    flex-direction: column;
    gap: 6px;
  }
}

.address-manager-modal {
  position: fixed;
  inset: 0;
  z-index: 2000;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
}

.address-manager-modal__overlay {
  position: absolute;
  inset: 0;
  background: rgba(0, 0, 0, 0.45);
}

.address-manager-modal__panel {
  position: relative;
  z-index: 1;
  width: min(600px, calc(100vw - 48px));
  max-height: calc(100vh - 48px);
  background: white;
  border-radius: 16px;
  box-shadow: 0 12px 32px rgba(0, 0, 0, 0.18);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.address-manager-modal__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px;
  border-bottom: 1px solid #f0f0f0;
}

.address-manager-modal__header h3 {
  margin: 0;
  font-size: 18px;
  color: var(--primary-color);
}

.address-manager-modal__body {
  padding: 0 12px 20px;
  overflow-y: auto;
}

.modal-close {
  cursor: pointer;
  font-size: 18px;
  color: #999;
}

.modal-close:hover {
  color: var(--primary-color);
}

.address-manager-fade-enter-active,
.address-manager-fade-leave-active {
  transition: opacity 0.2s ease;
}

.address-manager-fade-enter-from,
.address-manager-fade-leave-to {
  opacity: 0;
}

@media (max-width: 900px) {
  .address-manager-modal {
    padding: calc(env(safe-area-inset-top, 8px) + 70px) var(--mobile-safe-margin, 6px) var(--mobile-safe-margin, 6px);
    align-items: flex-start;
    justify-content: flex-start;
  }
  
  .address-manager-modal__panel {
    width: calc(100vw - 2 * var(--mobile-safe-margin, 6px));
    max-height: calc(100vh - (env(safe-area-inset-top, 8px) + 70px) - var(--mobile-safe-margin, 6px));
    border-radius: 16px;
  }
  
  .address-manager-modal__body {
    padding: 0 var(--mobile-safe-margin, 6px) var(--mobile-safe-margin, 6px);
  }
}

</style>

