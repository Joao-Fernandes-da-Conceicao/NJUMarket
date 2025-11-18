<template>
  <div>
    <h2>用户管理</h2>
    <div style="margin:12px 0; display:flex; gap:8px; align-items:center; flex-wrap:wrap;">
      <UnifiedInput v-model="keyword" placeholder="搜索用户（用户名/手机号）" style="width: 320px;" />
      <UnifiedButton type="primary" @click="doSearch">搜索</UnifiedButton>
    </div>
    <el-table :data="list" border style="width: 100%" @sort-change="onSortChange" @filter-change="onFilterChange">
      <el-table-column prop="userId" label="用户ID" width="240"/>
      <el-table-column prop="username" label="用户名"/>
      <el-table-column prop="primaryPhone" label="手机号"/>
      <el-table-column label="昵称" width="140">
        <template #default="{ row }">{{ row.profile?.nickname || '-' }}</template>
      </el-table-column>
      <el-table-column label="头像" width="80">
        <template #default="{ row }">
          <img v-if="row.profile?.avatar" :src="row.profile.avatar" alt="avatar" style="width:36px;height:36px;border-radius:50%;object-fit:cover;"/>
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column
        prop="accountStatus"
        label="账号状态"
        width="120"
        :filters="statusFilters"
        column-key="accountStatus"
        filter-placement="bottom-end"
      >
        <template #default="{ row }">
          <UnifiedTag :type="accountStatusType(row.accountStatus)">
            {{ accountStatusText(row.accountStatus) }}
          </UnifiedTag>
        </template>
      </el-table-column>
      <el-table-column prop="registerTime" label="注册时间" width="180" sortable="custom"/>
      <el-table-column label="信用/评分" width="200">
        <template #default="{ row }">
          信用: {{ row.profile?.creditScore ?? '-' }} / 买: {{ row.profile?.buyerRating ?? '-' }} / 卖: {{ row.profile?.sellerRating ?? '-' }}
        </template>
      </el-table-column>
      <el-table-column label="成交统计" width="160">
        <template #default="{ row }">
          卖出: {{ row.profile?.totalSales ?? '-' }} / 购入: {{ row.profile?.totalPurchases ?? '-' }}
        </template>
      </el-table-column>
      <el-table-column label="会员等级" width="120">
        <template #default="{ row }">
          <UnifiedTag :type="vipLevelType(row.profile?.vipLevel)">
            {{ vipLevelText(row.profile?.vipLevel) }}
          </UnifiedTag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="280">
        <template #default="{ row }">
          <UnifiedButton size="small" @click="disable(row)">禁用</UnifiedButton>
          <UnifiedButton size="small" type="danger" @click="remove(row)">删除</UnifiedButton>
          <UnifiedButton size="small" type="primary" @click="edit(row)">编辑</UnifiedButton>
          <UnifiedButton size="small" type="success" @click="manageAddresses(row)">地址管理</UnifiedButton>
        </template>
      </el-table-column>
    </el-table>
    <Pagination
      :total="total"
      :current-page="page"
      :page-size="pageSize"
      @page-change="handlePageChange"
      @page-size-change="handleSizeChange"
    />
  </div>
</template>

<script>
import UnifiedButton from '../components/common/UnifiedButton.vue'
import UnifiedInput from '../components/common/UnifiedInput.vue'
import UnifiedTag from '../components/common/UnifiedTag.vue'
import Pagination from '../components/common/Pagination.vue'

export default {
  name: 'Users',
  components:{
    UnifiedButton,
    UnifiedInput,
    UnifiedTag,
    Pagination
  },
  data(){ return { list: [], total: 0, page: 1, pageSize: 10, keyword: '', sortKey: '', sortOrder: '', statusFilters: [
    { text: 'ACTIVE', value: 'ACTIVE' },
    { text: 'SUSPENDED', value: 'SUSPENDED' },
    { text: 'BANNED', value: 'BANNED' }
  ], activeFilters: {} } },
  mounted(){ this.loadData() },
  methods:{
    async loadData(){
      const { usersAPI } = await import('../api/admin/users')
      const query = {
        keyword: (this.keyword || '').trim(),
        accountStatus: this.activeFilters.accountStatus?.[0] || '',
        sortProp: this.sortKey || '',
        sortOrder: this.sortOrder === 'descending' ? 'desc' : (this.sortOrder === 'ascending' ? 'asc' : '')
      }
      const res = await usersAPI.list(this.page, this.pageSize, query)
      if (res && res.success) {
        this.list = res.data?.list || res.data?.users || res.data || []
        this.total = res.data?.total ?? this.list.length
      }
    },
    async disable(row){
      const { usersAPI } = await import('../api/admin/users')
      await usersAPI.updateStatus(row.userId, 'SUSPENDED')
      this.loadData()
    },
    async remove(row){
      const { usersAPI } = await import('../api/admin/users')
      await usersAPI.remove(row.userId)
      this.loadData()
    },
    edit(row){
      this.$router.push(`/users/${row.userId}/edit`)
    },
    manageAddresses(row){
      this.$router.push(`/users/${row.userId}/addresses`)
    },
    onSortChange({ prop, order }){
      this.sortKey = prop
      this.sortOrder = order // 'ascending' | 'descending' | null
      this.page = 1
      this.loadData()
    },
    doSearch(){
      this.keyword = (this.keyword || '').trim()
      this.page = 1
      this.loadData()
    },
    onFilterChange(filters){
      this.activeFilters = { ...this.activeFilters, ...filters }
      this.page = 1
      this.loadData()
    },
    handlePageChange(p){
      this.page = p
      this.loadData()
    },
    handleSizeChange(s){
      this.pageSize = s
      this.page = 1
      this.loadData()
    },
    // ✅ 账号状态文本映射
    accountStatusText(status) {
      const map = {
        'ACTIVE': '活跃',
        'SUSPENDED': '已暂停',
        'BANNED': '已封禁'
      }
      return map[status] || (status || '-')
    },
    // ✅ 账号状态类型映射（用于 tag 颜色）
    accountStatusType(status) {
      const map = {
        'ACTIVE': 'success',
        'SUSPENDED': 'warning',
        'BANNED': 'danger'
      }
      return map[status] || 'info'
    },
    // ✅ 会员等级文本映射
    vipLevelText(vipLevel) {
      if (!vipLevel) return '-'
      const map = {
        'NORMAL': '普通',
        'BRONZE': '青铜',
        'SILVER': '白银',
        'GOLD': '黄金',
        'PLATINUM': '铂金'
      }
      return map[vipLevel] || vipLevel
    },
    // ✅ 会员等级类型映射（用于 tag 颜色）
    vipLevelType(vipLevel) {
      if (!vipLevel) return 'info'
      const map = {
        'NORMAL': 'info',
        'BRONZE': 'default',
        'SILVER': '',
        'GOLD': 'warning',
        'PLATINUM': 'success'
      }
      return map[vipLevel] || 'info'
    }
  },
  computed:{}
}
</script>


