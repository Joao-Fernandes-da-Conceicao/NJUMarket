<template>
  <div class="localStorage-debug">
    <h2>localStorage 调试工具</h2>
    
    <div class="debug-section">
      <h3>当前localStorage状态</h3>
      <div class="status-item">
        <strong>Token:</strong> 
        <span :class="tokenStatus.class">{{ tokenStatus.text }}</span>
        <span v-if="tokenStatus.length">(长度: {{ tokenStatus.length }})</span>
      </div>
      <div class="status-item">
        <strong>User:</strong> 
        <span :class="userStatus.class">{{ userStatus.text }}</span>
        <span v-if="userStatus.length">(长度: {{ userStatus.length }})</span>
      </div>
    </div>
    
    <div class="debug-section">
      <h3>操作</h3>
      <div class="button-group">
        <button @click="refreshStatus" class="btn-primary">刷新状态</button>
        <button @click="cleanInvalidData" class="btn-secondary">清理无效数据</button>
        <button @click="clearAllData" class="btn-danger">清除所有数据</button>
      </div>
    </div>
    
    <div class="debug-section" v-if="rawData.token || rawData.user">
      <h3>原始数据</h3>
      <div class="raw-data">
        <div v-if="rawData.token">
          <strong>Token:</strong>
          <pre>{{ rawData.token }}</pre>
        </div>
        <div v-if="rawData.user">
          <strong>User:</strong>
          <pre>{{ rawData.user }}</pre>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useUserStore } from '../stores/user'

const userStore = useUserStore()

const tokenStatus = ref({})
const userStatus = ref({})
const rawData = ref({})

const refreshStatus = () => {
  const token = localStorage.getItem('token')
  const user = localStorage.getItem('user')
  
  // Token状态
  if (!token) {
    tokenStatus.value = { text: '不存在', class: 'status-missing' }
  } else if (token === 'undefined' || token === 'null' || token.trim() === '') {
    tokenStatus.value = { text: '无效', class: 'status-invalid', length: token.length }
  } else {
    tokenStatus.value = { text: '有效', class: 'status-valid', length: token.length }
  }
  
  // User状态
  if (!user) {
    userStatus.value = { text: '不存在', class: 'status-missing' }
  } else if (user === 'undefined' || user === 'null' || user.trim() === '') {
    userStatus.value = { text: '无效', class: 'status-invalid', length: user.length }
  } else {
    try {
      JSON.parse(user)
      userStatus.value = { text: '有效', class: 'status-valid', length: user.length }
    } catch (error) {
      userStatus.value = { text: 'JSON格式错误', class: 'status-invalid', length: user.length }
    }
  }
  
  // 原始数据
  rawData.value = { token, user }
}

const cleanInvalidData = () => {
  userStore.cleanInvalidLocalStorage()
  refreshStatus()
  alert('无效数据已清理')
}

const clearAllData = () => {
  if (confirm('确定要清除所有localStorage数据吗？这将导致用户需要重新登录。')) {
    userStore.clearUserData()
    refreshStatus()
    alert('所有数据已清除')
  }
}

onMounted(() => {
  refreshStatus()
})
</script>

<style scoped>
.localStorage-debug {
  max-width: 800px;
  margin: 20px auto;
  padding: 20px;
  background: white;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.debug-section {
  margin-bottom: 30px;
}

.debug-section h3 {
  margin-bottom: 15px;
  color: #333;
  border-bottom: 2px solid #6A015E;
  padding-bottom: 5px;
}

.status-item {
  margin-bottom: 10px;
  padding: 10px;
  background: #f5f5f5;
  border-radius: 4px;
}

.status-valid {
  color: #28a745;
  font-weight: bold;
}

.status-invalid {
  color: #dc3545;
  font-weight: bold;
}

.status-missing {
  color: #6c757d;
  font-weight: bold;
}

.button-group {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.btn-primary, .btn-secondary, .btn-danger {
  padding: 10px 20px;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
  transition: all 0.3s ease;
}

.btn-primary {
  background-color: #6A015E;
  color: white;
}

.btn-primary:hover {
  background-color: #8B1A7A;
}

.btn-secondary {
  background-color: #6c757d;
  color: white;
}

.btn-secondary:hover {
  background-color: #5a6268;
}

.btn-danger {
  background-color: #dc3545;
  color: white;
}

.btn-danger:hover {
  background-color: #c82333;
}

.raw-data {
  background: #f8f9fa;
  padding: 15px;
  border-radius: 4px;
  border: 1px solid #dee2e6;
}

.raw-data pre {
  background: #e9ecef;
  padding: 10px;
  border-radius: 4px;
  overflow-x: auto;
  font-size: 12px;
  margin-top: 5px;
  word-break: break-all;
}

@media (max-width: 768px) {
  .localStorage-debug {
    margin: 10px;
    padding: 15px;
  }
  
  .button-group {
    flex-direction: column;
  }
  
  .btn-primary, .btn-secondary, .btn-danger {
    width: 100%;
  }
}
</style>
