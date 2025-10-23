<template>
  <div id="app">
    <AppLayout />
  </div>
</template>

<script>
import { onMounted } from 'vue'
import { useUserStore } from './stores/user'
import AppLayout from './components/layout/AppLayout.vue'

export default {
  name: 'App',
  components: {
    AppLayout
  },
  setup() {
    const userStore = useUserStore()
    
    onMounted(async () => {
      // 先清理无效的localStorage数据
      userStore.cleanInvalidLocalStorage()
      
      // 初始化用户状态
      userStore.initUser()
      
      // 检查并修复用户数据
      setTimeout(async () => {
        await userStore.checkAndFixUserData()
      }, 1000) // 延迟1秒执行，确保初始化完成
    })
    
    return {}
  }
}
</script>

<style>
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

#app {
  font-family: 'PingFang SC', 'Helvetica Neue', Helvetica, 'Microsoft YaHei', Arial, sans-serif;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
  color: #333;
  min-height: 100vh;
  background-color: #f5f5f5;
}

/* 主题色变量 */
:root {
  --primary-color: #6A015E;
  --primary-light: #8B1A7A;
  --primary-dark: #4A003D;
  --text-primary: #6A015E;
  --text-secondary: #666;
  --text-light: #999;
  --bg-primary: #6A015E;
  --bg-white: #ffffff;
  --border-color: #e0e0e0;
  --shadow: 0 2px 8px rgba(106, 1, 94, 0.1);
}

/* 通用样式 */
.container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 20px;
}

.btn-primary {
  background-color: var(--primary-color);
  color: white;
  border: none;
  padding: 12px 24px;
  border-radius: 6px;
  cursor: pointer;
  font-size: 14px;
  transition: all 0.3s ease;
}

.btn-primary:hover {
  background-color: var(--primary-light);
  transform: translateY(-1px);
  box-shadow: var(--shadow);
}

.btn-secondary {
  background-color: white;
  color: var(--primary-color);
  border: 1px solid var(--primary-color);
  padding: 12px 24px;
  border-radius: 6px;
  cursor: pointer;
  font-size: 14px;
  transition: all 0.3s ease;
}

.btn-secondary:hover {
  background-color: var(--primary-color);
  color: white;
}

.card {
  background: white;
  border-radius: 8px;
  box-shadow: var(--shadow);
  padding: 20px;
  margin-bottom: 20px;
}

.text-primary {
  color: var(--text-primary);
}

.text-secondary {
  color: var(--text-secondary);
}

.text-light {
  color: var(--text-light);
}

/* 响应式设计 */
@media (max-width: 768px) {
  .container {
    padding: 0 15px;
  }
  
  .btn-primary,
  .btn-secondary {
    padding: 10px 20px;
    font-size: 13px;
  }
}
</style>
