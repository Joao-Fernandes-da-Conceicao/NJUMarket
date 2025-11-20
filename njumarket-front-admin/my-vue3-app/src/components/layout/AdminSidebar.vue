<template>
  <div class="admin-sidebar">
    <div class="brand">
      <div class="brand-en">NJUMarketAdmin</div>
      <div class="brand-zh">南大集市管理系统</div>
    </div>

    <div class="admin-info" :class="{ gold: isSystem }">
      <div class="admin-name">{{ adminName || '管理员' }}</div>
      <div class="admin-level">{{ adminLevel || '—' }}</div>
    </div>

    <div class="nav-list">
      <router-link
        v-for="item in items"
        :key="item.path"
        :to="item.path"
        :class="['nav-link', { active: isActive(item.path) }]"
      >
        {{ item.label }}
      </router-link>
    </div>
    <div class="logout-wrap">
      <a class="nav-link logout" href="javascript:void(0);" @click="logout">退出</a>
    </div>
  </div>
  </template>

<script>
export default {
  name: 'AdminSidebar',
  data(){
    return {
      adminName: '',
      adminLevel: '',
      allItems: [
        { path: '/', label: '概览 Dashboard' },
        { path: '/users', label: '用户管理' },
        { path: '/commodities', label: '商品管理' },
        { path: '/orders', label: '订单管理' },
        { path: '/messages', label: '消息管理' },
        // ✅ 管理员管理：只对system权限显示
        { path: '/admins', label: '管理员管理', requiresSystem: true },
        // ✅ Elasticsearch 管理：只对system权限显示
        { path: '/elasticsearch', label: 'ES 索引管理', requiresSystem: true }
      ]
    }
  },
  methods:{
    isActive(path){ return this.$route.path === path },
    async fetchAdmin(){
      try {
        const { authAPI } = await import('../../api/admin/auth')
        const res = await authAPI.me()
        if (res && res.success) {
          this.adminName = res.data?.realName || res.data?.username || '管理员'
          this.adminLevel = res.data?.adminLevel || ''
        }
      } catch(e) {}
    },
    async logout(){
      try {
        const { authAPI } = await import('../../api/admin/auth')
        await authAPI.logout()
      } catch(e) {}
      localStorage.removeItem('adminToken')
      this.$router.replace('/login')
    }
  },
  computed: {
    isSystem(){
      if (!this.adminLevel) return false
      return String(this.adminLevel).toLowerCase() === 'system'
    },
    // ✅ 根据权限过滤导航项
    items(){
      return this.allItems.filter(item => {
        // ✅ 过滤：只有system权限才能看到管理员管理
        if (item.requiresSystem && !this.isSystem) {
          return false
        }
        return true
      })
    }
  },
  mounted(){ this.fetchAdmin() }
}
</script>

<style scoped>
.admin-sidebar {
  width: 300px;
  background: var(--primary-color);
  padding: 16px 16px 20px;
  display: flex;
  flex-direction: column;
  box-sizing: border-box;
  height: 100vh;
  overflow-y: auto;
  overflow-x: hidden;
}

/* ✅ 侧边栏滚动条样式优化 */
.admin-sidebar::-webkit-scrollbar {
  width: 6px;
}
.admin-sidebar::-webkit-scrollbar-track {
  background: rgba(255, 255, 255, 0.1);
}
.admin-sidebar::-webkit-scrollbar-thumb {
  background: rgba(255, 255, 255, 0.3);
  border-radius: 3px;
}
.admin-sidebar::-webkit-scrollbar-thumb:hover {
  background: rgba(255, 255, 255, 0.5);
}

/* ✅ 品牌区域 - 固定顶部，可压缩 */
.brand { 
  color: #fff; 
  margin: 20px 0 20px; 
  text-align: center;
  flex-shrink: 0;
}

.brand-en { 
  font-weight: normal; 
  opacity: .9; 
  font-size: 27px;
  line-height: 1.2;
  word-break: break-word; 
}

.brand-zh { 
  font-weight: normal; 
  font-size: 30px;
  line-height: 1.2; 
  word-break: break-word;
}

/* ✅ 管理员信息 - 可压缩 */
.admin-info {
  color: #fff;
  margin: 20px auto 20px;
  border: 2px solid #ffffff;
  border-radius: 9999px;
  padding: 10px 14px;
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  width: 232px;
  box-sizing: border-box;
  flex-shrink: 0;
}
.admin-info.gold { border-color: #FFD700; color: #FFD700; }
.admin-name { font-size: 25px; font-weight: normal; line-height: 1.2; }
.admin-level { font-size: 12.5px; opacity: .95; line-height: 1.2; }

/* ✅ 导航列表 - 可滚动，自适应间距 */
.nav-list { 
  display: flex; 
  flex-direction: column; 
  gap: 12px; 
  align-items: center; 
  margin-top: 20px;
  flex: 1;
  min-height: 0;
  overflow-y: auto;
}

.nav-link {
  width: 232px;
  height: 48px;
  line-height: 48px;
  font-weight: normal;
  box-sizing: border-box;
  margin: 10px 0 10px;
  padding: 0 20px;
  display: inline-block;
  text-align: center;
  border-radius: 9999px;
  text-decoration: none;
  border: 2px solid #ffffff;
  color: #ffffff;
  background: var(--primary-color);
  transition: all .2s ease;
  flex-shrink: 0;
}
.nav-link:hover { background: var(--primary-light); color: #ffffff; }
.nav-link.active {
  background: #ffffff;
  border: 2px solid var(--primary-color);
  color: var(--primary-color);
}

/* 兜底：移除相邻默认间隔，gap 控制 */
.nav-list :deep(a + a) { margin-left: 0; }

/* ✅ 退出按钮 - 固定在底部 */
.logout-wrap { 
  margin-top: auto; 
  display: flex; 
  justify-content: center;
  flex-shrink: 0;
  padding-top: 20px;
}
.logout { background: transparent; border-color: #ffffff; color: #ffffff; }
.logout:hover { background: rgba(255,255,255,0.12); }

/* ✅ 响应式设计：小屏幕（高度 < 900px） */
@media (max-height: 900px) {
  .brand {
    margin: 15px 0 15px;
  }
  .brand-en {
    font-size: 24px;
  }
  .brand-zh {
    font-size: 26px;
  }
  .admin-info {
    margin: 15px auto 15px;
    padding: 8px 12px;
  }
  .admin-name {
    font-size: 22px;
  }
  .admin-level {
    font-size: 11px;
  }
  .nav-list {
    margin-top: 15px;
    gap: 10px;
  }
  .nav-link {
    height: 44px;
    line-height: 44px;
    margin: 8px 0 8px;
  }
}

/* ✅ 响应式设计：超小屏幕（高度 < 700px） */
@media (max-height: 700px) {
  .brand {
    margin: 12px 0 12px;
  }
  .brand-en {
    font-size: 20px;
  }
  .brand-zh {
    font-size: 22px;
  }
  .admin-info {
    margin: 12px auto 12px;
    padding: 6px 10px;
  }
  .admin-name {
    font-size: 18px;
  }
  .admin-level {
    font-size: 10px;
  }
  .nav-list {
    margin-top: 10px;
    gap: 8px;
  }
  .nav-link {
    height: 40px;
    line-height: 40px;
    margin: 6px 0 6px;
    font-size: 14px;
  }
  .logout-wrap {
    padding-top: 15px;
  }
}
</style>


