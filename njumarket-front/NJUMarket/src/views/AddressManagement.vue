<template>
  <div class="address-management-page">
    <div class="address-management-content">
      <div class="container">
        <div class="page-header">
          <h1>地址管理</h1>
          <div class="header-actions">
            <UnifiedButton @click="$router.go(-1)">
              返回
            </UnifiedButton>
          </div>
        </div>
        
        <AddressManager />
      </div>
    </div>
  </div>
</template>

<script>
import { defineComponent } from 'vue'
import UnifiedButton from '../components/common/UnifiedButton.vue'
import AddressManager from '../components/address/AddressManager.vue'

export default defineComponent({
  name: 'AddressManagement',
  components: {
    UnifiedButton,
    AddressManager
  }
})
</script>

<style scoped>
.address-management-page {
  min-height: 100vh;
  background-color: #f5f5f5;
}

.address-management-content {
  padding: 40px 0;
}

.container {
  max-width: 760px;
  margin: 0 auto;
  padding: 0 24px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 30px;
}

.page-header h1 {
  color: var(--primary-color);
}

@media (max-width: 768px) {
  .address-management-content {
    padding: 24px 0;
  }

  .container {
    max-width: min(375px, calc(100vw - 16px));
    padding: 0 8px;
  }

  .page-header {
    flex-direction: column;
    gap: 12px;
    align-items: flex-start;
  }
}
</style>

