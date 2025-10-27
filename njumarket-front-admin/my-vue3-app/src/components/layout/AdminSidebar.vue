<template>
  <div class="admin-sidebar">
    <div class="brand">
      <div class="brand-en">NJUMarketDashboard</div>
      <div class="brand-zh">南大集市控制中心</div>
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
      items: [
        { path: '/', label: '概览 Dashboard' },
        { path: '/users', label: '用户管理' },
        { path: '/commodities', label: '商品管理' },
        { path: '/orders', label: '订单管理' },
        { path: '/messages', label: '消息管理' }
      ],
      adminName: '',
      adminLevel: ''
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
}
.brand { color: #fff; margin: 30px 0 30px; text-align: center; }
.brand-en { 
  font-weight: normal; opacity: .9; 
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

.admin-info {
  color: #fff;
  margin: 30px auto 30px;
  border: 2px solid #ffffff;
  border-radius: 9999px;
  padding: 10px 14px;
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  width: 232px;
  box-sizing: border-box;
}
.admin-info.gold { border-color: #FFD700; color: #FFD700; }
.admin-name { font-size: 25px; font-weight: normal; line-height: 1.2; }
.admin-level { font-size: 12.5px; opacity: .95; line-height: 1.2; }
.nav-list { display: flex; flex-direction: column; gap: 12px; align-items: center; margin-top: 75px; }
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
}
.nav-link:hover { background: var(--primary-light); color: #ffffff; }
.nav-link.active {
  background: #ffffff;
  border: 2px solid var(--primary-color);
  color: var(--primary-color);
}

/* 兜底：移除相邻默认间隔，gap 控制 */
.nav-list :deep(a + a) { margin-left: 0; }

.logout-wrap { margin-top: auto; display: flex; justify-content: center; }
.logout { background: transparent; border-color: #ffffff; color: #ffffff; }
.logout:hover { background: rgba(255,255,255,0.12); }
</style>


