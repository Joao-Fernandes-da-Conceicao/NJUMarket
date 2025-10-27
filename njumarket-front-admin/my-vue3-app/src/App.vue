<template>
  <div id="app" class="admin-app">
    <template v-if="$route.path !== '/login'">
      <div class="admin-body">
        <admin-sidebar />
        <div class="admin-content">
          <router-view />
        </div>
      </div>
    </template>
    <template v-else>
      <router-view />
    </template>
  </div>
  </template>

<script>
import AdminSidebar from './components/layout/AdminSidebar.vue'

export default {
  name: 'App',
  components: { AdminSidebar }
}
</script>

<style>
html, body, #app { height: 100%; }
body { margin: 0; }
.admin-app { display: flex; flex-direction: column; height: 100%; }
.admin-body { display: flex; flex: 1; min-height: 0; }
.admin-content { flex: 1; padding: 24px; overflow: auto; background: #fff; }

/* 主题色与用户端一致 */
:root {
  --primary-color: #6a015e;
  --primary-light: #8a2a80;
}

/* 同步 Element Plus 主色 */
:root {
  --el-color-primary: var(--primary-color);
}

/* 与用户端一致的中英文字体栈 */
#app {
  font-family: 'Tw Cen MT', '等线', 'DengXian', 'Microsoft YaHei', Arial, sans-serif;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
}

/* 所有页面大标题统一样式 */
.admin-content h2 {
  font-size: 50px;
  color: var(--primary-color);
  font-weight: normal;
  margin: 50px 0 50px 25px;
}
</style>


