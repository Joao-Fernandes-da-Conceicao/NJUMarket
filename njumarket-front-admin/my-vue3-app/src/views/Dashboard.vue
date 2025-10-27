<template>
  <div>
    <h2>管理概览</h2>
    <div class="stat-grid">
      <div class="stat-card">
        <div class="stat-title">用户总数</div>
        <div class="stat-number">{{ usersTotal }}</div>
      </div>
      <div class="stat-card">
        <div class="stat-title">商品总数</div>
        <div class="stat-number">{{ commoditiesTotal }}</div>
      </div>
      <div class="stat-card">
        <div class="stat-title">订单总数</div>
        <div class="stat-number">{{ ordersTotal }}</div>
      </div>
      <div class="stat-card">
        <div class="stat-title">消息</div>
        <div class="stat-number stat-todo">TODO</div>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'Dashboard',
  data(){ return { usersTotal: 0, commoditiesTotal: 0, ordersTotal: 0 } },
  mounted(){ this.loadTotals() },
  methods:{
    async loadTotals(){
      const [{ usersAPI }, { commoditiesAPI }, { ordersAPI }] = await Promise.all([
        import('../api/admin/users'),
        import('../api/admin/commodities'),
        import('../api/admin/orders')
      ])
      try {
        const [uRes, cRes, oRes] = await Promise.all([
          usersAPI.list(1, 1, {}),
          commoditiesAPI.list(1, 1, {}),
          ordersAPI.list(1, 1, {})
        ])
        if (uRes && uRes.success) this.usersTotal = uRes.data?.total ?? 0
        if (cRes && cRes.success) this.commoditiesTotal = cRes.data?.total ?? 0
        if (oRes && oRes.success) this.ordersTotal = oRes.data?.total ?? 0
      } catch(e){ /* ignore */ }
    }
  }
}
</script>

<style scoped>
.stat-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 50px; margin: 8px 75px 0 75px; }
 .stat-card { background: transparent; border: none; border-radius: 0; text-align: center; height: 300px; display: flex; flex-direction: column; justify-content: center; }
.stat-title { color: var(--primary-color); font-size: 30px; margin-bottom: 12px; }
.stat-number { font-size: 75px; color: var(--primary-color); line-height: 1; }
.stat-todo { color: #909399; }
@media (max-width: 1200px){ .stat-grid { grid-template-columns: repeat(2, 1fr); } }
@media (max-width: 640px){ .stat-grid { grid-template-columns: 1fr; } }
</style>


