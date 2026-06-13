<template>
  <div class="address-manager-shell">
    <div class="address-manager">
    <div class="manager-header">
      <UnifiedButton type="primary" @click="showAddDialog = true">
        添加新地址
      </UnifiedButton>
    </div>
    
    <div class="address-list" v-loading="loading">
      <div 
        v-for="address in addresses" 
        :key="address.addressId"
        class="address-item"
        :class="{ 'is-default': address.isDefault, 'is-selected': isSelectMode && selectedAddressId === address.addressId }"
        @click="handleSelectAddress(address)"
      >
        <div class="address-header">
          <div class="address-label-badge">
            <span class="label-text">{{ getAddressLabelText(address.addressLabel) }}</span>
            <span v-if="address.isDefault" class="default-badge">默认</span>
          </div>
          <div class="address-actions">
            <UnifiedButton 
              type="text" 
              size="small" 
              @click.stop="handleEdit(address)"
            >
              编辑
            </UnifiedButton>
            <UnifiedButton 
              type="text" 
              size="small" 
              @click.stop="handleSetDefault(address)"
              v-if="!address.isDefault"
            >
              设为默认
            </UnifiedButton>
            <UnifiedButton 
              type="text" 
              size="small" 
              @click.stop="handleDelete(address)"
            >
              删除
            </UnifiedButton>
          </div>
        </div>
        
        <div class="address-content">
          <div class="address-full">{{ address.fullAddress }}</div>
          <div class="address-contact">
            <span>{{ address.recipientName }}</span>
            <span>{{ address.recipientPhone }}</span>
          </div>
        </div>
      </div>
      
      <div v-if="addresses.length === 0 && !loading" class="empty-state">
        <p>暂无地址，请添加新地址</p>
      </div>
    </div>
    
    <transition name="address-form-fade">
      <div 
        v-if="showAddDialog" 
        class="address-form-modal" 
        role="dialog" 
        aria-modal="true"
      >
        <div class="address-form-modal__overlay" @click="handleFormCancel"></div>
        <div class="address-form-modal__panel">
          <div class="address-form-modal__header">
            <h3>{{ editingAddress ? '编辑地址' : '添加新地址' }}</h3>
            <el-icon class="modal-close" @click="handleFormCancel">
              <Close />
            </el-icon>
          </div>
          <div class="address-form-modal__body">
            <AddressForm
              :address="editingAddress"
              @submit="handleFormSubmit"
              @cancel="handleFormCancel"
            />
          </div>
        </div>
      </div>
    </transition>
    </div>
  </div>
</template>

<script>
import { ref, onMounted } from 'vue'
import { addressAPI } from '../../api'
import { ElMessage, ElMessageBox } from 'element-plus'
import UnifiedButton from '../common/UnifiedButton.vue'
import AddressForm from './AddressForm.vue'
import { Close } from '@element-plus/icons-vue'

export default {
  name: 'AddressManager',
  components: {
    UnifiedButton,
    AddressForm,
    Close
  },
  props: {
    isSelectMode: {
      type: Boolean,
      default: false
    }
  },
  emits: ['address-selected', 'close', 'address-changed'],
  setup(props, { emit }) {
    const addresses = ref([])
    const loading = ref(false)
    const showAddDialog = ref(false)
    const editingAddress = ref(null)
    const selectedAddressId = ref('')
    
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
          // 过滤活跃地址，并按默认地址优先排序（默认地址置顶）
          addresses.value = data
            .filter(addr => addr.isActive)
            .sort((a, b) => {
              // 默认地址排在前面
              if (a.isDefault && !b.isDefault) return -1
              if (!a.isDefault && b.isDefault) return 1
              // 如果都是默认或都不是默认，按创建时间倒序（最新的在前）
              return 0
            })
        }
      } catch (error) {
        console.error('加载地址列表失败:', error)
        ElMessage.error('加载地址列表失败')
      } finally {
        loading.value = false
      }
    }
    
    // 选择地址（选择模式）
    const handleSelectAddress = (address) => {
      if (props.isSelectMode) {
        selectedAddressId.value = address.addressId
        emit('address-selected', address)
      }
    }
    
    // 编辑地址
    const handleEdit = (address) => {
      editingAddress.value = { ...address }
      showAddDialog.value = true
    }
    
    // 设置默认地址
    const handleSetDefault = async (address) => {
      try {
        const response = await addressAPI.setDefault(address.addressId)
        if (response.success) {
          ElMessage.success('设置默认地址成功')
          await loadAddresses()
          emit('address-changed')
        } else {
          ElMessage.error(response.errorMsg || '设置默认地址失败')
        }
      } catch (error) {
        console.error('设置默认地址失败:', error)
        ElMessage.error('设置默认地址失败')
      }
    }
    
    // 删除地址
    const handleDelete = async (address) => {
      try {
        await ElMessageBox.confirm(
          '确定要删除这个地址吗？',
          '确认删除',
          {
            confirmButtonText: '确定',
            cancelButtonText: '取消',
            type: 'warning'
          }
        )
        
        const response = await addressAPI.delete(address.addressId)
        if (response.success) {
          ElMessage.success('删除地址成功')
          await loadAddresses()
          emit('address-changed')
        } else {
          ElMessage.error(response.errorMsg || '删除地址失败')
        }
      } catch (error) {
        if (error !== 'cancel') {
          console.error('删除地址失败:', error)
          ElMessage.error('删除地址失败')
        }
      }
    }
    
    // 表单提交
    const handleFormSubmit = async (formData) => {
      try {
        let response
        if (editingAddress.value) {
          // 更新地址
          response = await addressAPI.update(editingAddress.value.addressId, formData)
        } else {
          // 创建地址
          response = await addressAPI.create(formData)
        }
        
        if (response.success) {
          ElMessage.success(editingAddress.value ? '更新地址成功' : '添加地址成功')
          showAddDialog.value = false
          editingAddress.value = null
          await loadAddresses()
          emit('address-changed')
        } else {
          ElMessage.error(response.errorMsg || '操作失败')
        }
      } catch (error) {
        console.error('保存地址失败:', error)
        ElMessage.error('保存地址失败')
      }
    }
    
    const handleFormCancel = () => {
      showAddDialog.value = false
      editingAddress.value = null
    }
    
    onMounted(() => {
      loadAddresses()
    })
    
    return {
      addresses,
      loading,
      showAddDialog,
      editingAddress,
      selectedAddressId,
      getAddressLabelText,
      handleSelectAddress,
      handleEdit,
      handleSetDefault,
      handleDelete,
      handleFormSubmit,
      handleFormCancel
    }
  }
}
</script>

<style scoped>
.address-manager-shell {
  width: 100%;
}

.address-manager {
  max-width: 720px;
  margin: 0 auto;
  width: 100%;
}

.manager-header {
  margin-bottom: 20px;
  text-align: right;
}

.address-list {
  min-height: 200px;
}

.address-item {
  padding: 15px;
  margin-bottom: 15px;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.3s;
}

.address-item:hover {
  border-color: var(--primary-color);
  box-shadow: 0 2px 8px rgba(106, 1, 94, 0.1);
}

.address-item.is-default {
  border-color: var(--primary-color);
  background-color: #fef0f6;
}

.address-item.is-selected {
  border-color: var(--primary-color);
  background-color: #fef0f6;
  box-shadow: 0 2px 12px rgba(106, 1, 94, 0.2);
}

.address-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}

.address-label-badge {
  display: flex;
  align-items: center;
  gap: 8px;
}

.label-text {
  padding: 2px 8px;
  background-color: var(--primary-color);
  color: white;
  border-radius: 4px;
  font-size: 12px;
}

.default-badge {
  padding: 2px 8px;
  background-color: #67c23a;
  color: white;
  border-radius: 4px;
  font-size: 12px;
}

.address-actions {
  display: flex;
  gap: 10px;
}

.address-content {
  margin-top: 10px;
}

.address-full {
  font-size: 14px;
  color: #333;
  margin-bottom: 5px;
}

.address-contact {
  display: flex;
  gap: 15px;
  font-size: 12px;
  color: #666;
}

.empty-state {
  text-align: center;
  padding: 40px;
  color: #999;
}

/* 响应式：移动端样式优化 */
@media (max-width: 768px) {
  .address-manager {
    width: 100%;
    max-width: min(var(--mobile-dialog-max-width, 440px), calc(100vw - 2 * var(--mobile-safe-margin, 6px)));
    padding: 0 var(--mobile-safe-margin, 6px);
    margin: 0 auto;
  }

  .manager-header {
    text-align: center;
  }

  .address-item {
    padding: 12px;
  }

  .address-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 10px;
  }

  .address-actions {
    width: 100%;
    justify-content: flex-start;
    flex-wrap: wrap;
    gap: 6px;
  }

  .address-content {
    font-size: 13px;
  }

  .address-contact {
    flex-direction: column;
    gap: 6px;
    font-size: 12px;
  }

}

:global(.address-form-dialog .el-dialog) {
  max-width: 520px;
  width: min(520px, calc(100vw - 32px));
  margin: 30px auto !important;
  border-radius: 16px;
}

@media (max-width: 768px) {
  :global(.address-form-dialog .el-dialog) {
    max-width: min(var(--mobile-dialog-max-width, 440px), calc(100vw - 2 * var(--mobile-safe-margin, 6px)));
    width: calc(100vw - 2 * var(--mobile-safe-margin, 6px));
    margin: calc(env(safe-area-inset-top, 8px) + 6px) auto var(--mobile-safe-margin, 6px) !important;
  }
  
  :global(.address-form-dialog .el-dialog__body) {
    padding: 16px var(--mobile-safe-margin, 6px) 20px !important;
  }
}

:global(.address-form-overlay) {
  padding: 0 !important;
}

:global(.address-manager-overlay) {
  padding: 0 !important;
}

:global(.address-manager-dialog .el-dialog) {
  max-width: 600px;
  width: min(600px, calc(100vw - 48px));
  margin: 30px auto !important;
  border-radius: 16px;
}

@media (max-width: 900px) {
  :global(.address-manager-dialog .el-dialog) {
    max-width: min(var(--mobile-dialog-max-width, 440px), calc(100vw - 2 * var(--mobile-safe-margin, 6px)));
    width: calc(100vw - 2 * var(--mobile-safe-margin, 6px));
    margin: calc(env(safe-area-inset-top, 8px) + 6px) auto var(--mobile-safe-margin, 6px) !important;
  }
  
  :global(.address-manager-dialog .el-dialog__body) {
    padding: 16px var(--mobile-safe-margin, 6px) 20px !important;
  }
}

:global(.address-manager-dialog) :deep(.address-manager-shell) {
  width: 100%;
  max-width: 100%;
  padding: 0 16px;
  margin: 0;
}

:global(.address-manager-dialog) :deep(.address-manager) {
  max-width: 100%;
  width: 100%;
  margin: 0;
}

@media (max-width: 900px) {
  :global(.address-manager-dialog) :deep(.address-manager-shell) {
    padding: 0 var(--mobile-safe-margin, 6px);
  }
}

/* 自定义新增/编辑地址弹窗 */
.address-form-modal {
  position: fixed;
  inset: 0;
  z-index: 2100;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
}

.address-form-modal__overlay {
  position: absolute;
  inset: 0;
  background: rgba(0, 0, 0, 0.45);
}

.address-form-modal__panel {
  position: relative;
  z-index: 1;
  width: min(520px, calc(100vw - 48px));
  max-height: calc(100vh - 48px);
  background: white;
  border-radius: 16px;
  box-shadow: 0 12px 32px rgba(0, 0, 0, 0.18);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.address-form-modal__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px;
  border-bottom: 1px solid #f0f0f0;
}

.address-form-modal__header h3 {
  margin: 0;
  font-size: 18px;
  color: var(--primary-color);
}

.address-form-modal__body {
  padding: 0 20px 20px;
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

.address-form-fade-enter-active,
.address-form-fade-leave-active {
  transition: opacity 0.2s ease;
}

.address-form-fade-enter-from,
.address-form-fade-leave-to {
  opacity: 0;
}

@media (max-width: 900px) {
  .address-form-modal {
    padding: calc(env(safe-area-inset-top, 8px) + 70px) var(--mobile-safe-margin, 6px) var(--mobile-safe-margin, 6px);
    align-items: flex-start;
    justify-content: flex-start;
  }
  
  .address-form-modal__panel {
    width: calc(100vw - 2 * var(--mobile-safe-margin, 6px));
    max-height: calc(100vh - (env(safe-area-inset-top, 8px) + 70px) - var(--mobile-safe-margin, 6px));
  }
  
  .address-form-modal__body {
    padding: 0 var(--mobile-safe-margin, 6px) var(--mobile-safe-margin, 6px);
  }
}
</style>

