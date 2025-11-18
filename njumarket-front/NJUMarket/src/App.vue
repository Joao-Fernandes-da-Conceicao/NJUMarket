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
  font-family: 'Tw Cen MT', '等线', 'DengXian', 'Microsoft YaHei', Arial, sans-serif;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
  color: #333;
  min-height: 100vh;
  background-color: #f5f5f5;
}

/* ===== 主题色变量 ===== */
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
  
  /* ===== 断点配置 ===== */
  --breakpoint-mobile: 900px;
  --breakpoint-wide: 1200px;
  --breakpoint-ultra-wide: 1600px;
  --breakpoint-tablet: 1024px;
  --breakpoint-desktop: 1400px;
  
  /* ===== 容器配置 ===== */
  --container-max-width: 1400px;
  --container-padding: 24px;
  
  /* ===== 间距系统 ===== */
  --spacing-xs: 8px;
  --spacing-sm: 12px;
  --spacing-md: 20px;
  --spacing-lg: 32px;
  --spacing-xl: 40px;
  
  /* ===== 圆角系统 ===== */
  --radius-sm: 12px;
  --radius-md: 16px;
  --radius-lg: 20px;
  --radius-pill: 20px;
  
  /* ===== 响应式缩放因子 ===== */
  --mobile-scale: 1;
  
  /* ===== 移动端安全边距与弹窗宽度 ===== */
  --mobile-safe-margin: 8px;
  --mobile-dialog-max-width: 400px;
}

/* ===== 通用布局 ===== */
.container {
  max-width: var(--container-max-width);
  margin: 0 auto;
  padding: 0 var(--container-padding);
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

/* ===== 全局页面标题样式 ===== */
.page-header h1 {
  font-size: 45px;
  font-weight: normal;
  color: var(--primary-color);
  margin: 0 0 25px 0;
}

/* ===== 响应式设计 ===== */

/* 平板 */
@media (min-width: 1024px) and (max-width: 1399px) {
  .container {
    padding: 0 var(--spacing-md);
  }
}

/* 超宽屏 */
@media (min-width: 1600px) {
  .container {
    max-width: 1600px;
    padding: 0 var(--spacing-lg);
  }
}

/* 移动端 */
@media (max-width: 900px) {
  :root {
    --mobile-scale: 0.75; /* 移动端缩小到0.85倍以适应更小的卡片 */
    --container-padding: 15px;
    --mobile-safe-margin: 6px;
    --mobile-dialog-max-width: 440px;
  }
  
  .container {
    padding: 0 var(--container-padding);
  }
  
  .btn-primary,
  .btn-secondary {
    padding: 10px 20px;
    font-size: 13px;
  }
}

/* 全局空状态按钮药丸型 */
:global(.el-empty .unified-button :deep(.el-button)) {
  border-radius: 9999px !important;
}
</style>
