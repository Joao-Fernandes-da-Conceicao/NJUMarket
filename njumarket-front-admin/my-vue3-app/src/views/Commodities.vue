<template>
  <div>
    <h2>商品管理</h2>
    <div style="margin:12px 0; display:flex; gap:8px; align-items:center; flex-wrap:wrap;">
      <UnifiedInput v-model="keyword" placeholder="搜索（标题/卖家ID/卖家昵称）" style="width: 360px;" />
      <UnifiedButton type="primary" @click="doSearch">搜索</UnifiedButton>
    </div>
    <el-table :data="list" border style="width: 100%" @sort-change="onSortChange" @filter-change="onFilterChange">
      <el-table-column prop="commodityId" label="商品ID" width="240"/>
      <el-table-column prop="title" label="标题" min-width="160"/>
      <el-table-column prop="price" label="价格" width="100"/>
      <el-table-column prop="stock" label="库存" width="80"/>
      <el-table-column
        prop="category"
        label="分类"
        width="140"
        :filters="categoryFilters"
        column-key="category"
        filter-placement="bottom-end"
      />
      <el-table-column
        prop="conditionLevel"
        label="成色"
        width="120"
        :filters="conditionFilters"
        column-key="conditionLevel"
        filter-placement="bottom-end"
      />
      <el-table-column
        prop="commodityStatus"
        label="状态"
        width="120"
        :filters="statusFilters"
        column-key="commodityStatus"
        filter-placement="bottom-end"
      >
        <template #default="{ row }">
          <UnifiedTag :type="cStatusType(row.commodityStatus)">{{ cStatusText(row.commodityStatus) }}</UnifiedTag>
        </template>
      </el-table-column>
      <el-table-column label="首图" width="90">
        <template #default="{ row }">
          <img v-if="firstImage(row)" :src="firstImage(row)" alt="img" style="width:48px;height:48px;border-radius:8px;object-fit:cover;" />
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column label="卖家" min-width="220">
        <template #default="{ row }">
          <div style="display:flex;align-items:center;gap:8px;">
            <img v-if="getSellerAvatar(row)" :src="getSellerAvatar(row)" alt="avatar" style="width:24px;height:24px;border-radius:50%;object-fit:cover;" />
            <div style="display:flex;flex-direction:column;line-height:1.2;">
              <span>{{ getSellerName(row) }}</span>
              <span style="font-size:12px;color:#909399;">{{ row.sellerId }}</span>
            </div>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="地址" min-width="180">
        <template #default="{ row }">
          <div style="font-size:13px;">
            {{ formatCommodityAddress(row) }}
          </div>
        </template>
      </el-table-column>
      <el-table-column prop="publishTime" label="上架时间" width="170" sortable="custom"/>
      <el-table-column
        prop="sellerVisibility"
        label="卖家可见"
        width="120"
        :filters="visibilityFilters"
        column-key="sellerVisibility"
      >
        <template #default="{ row }"><UnifiedTag :type="visType(row.sellerVisibility)">{{ visText(row.sellerVisibility) }}</UnifiedTag></template>
      </el-table-column>
      <el-table-column
        prop="buyerVisibility"
        label="买家可见"
        width="120"
        :filters="visibilityFilters"
        column-key="buyerVisibility"
      >
        <template #default="{ row }"><UnifiedTag :type="visType(row.buyerVisibility)">{{ visText(row.buyerVisibility) }}</UnifiedTag></template>
      </el-table-column>
      <el-table-column prop="clickCount" label="点击" width="90" sortable="custom"/>
      <el-table-column prop="reportCount" label="举报" width="80"/>
      <el-table-column label="操作" width="255">
        <template #default="{ row }">
          <UnifiedButton size="small" @click="shelf(row)">上架</UnifiedButton>
          <UnifiedButton size="small" @click="unshelf(row)">下架</UnifiedButton>
          <UnifiedButton size="small" type="danger" @click="remove(row)">删除</UnifiedButton>
          <UnifiedButton size="small" type="primary" @click="edit(row)">编辑</UnifiedButton>
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
  name: 'Commodities',
  components:{ UnifiedButton, UnifiedInput, UnifiedTag, Pagination },
  data(){ return {
    list: [], total: 0, page: 1, pageSize: 10,
    sellerMap: {},
    keyword: '',
    sortKey: '',
    sortOrder: '', // 'ascending' | 'descending'
    activeFilters: {},
    categoryFilters: [
      { text: '电子产品', value: '电子产品' },
      { text: '服装配饰', value: '服装配饰' },
      { text: '图书文具', value: '图书文具' },
      { text: '生活用品', value: '生活用品' },
      { text: '运动户外', value: '运动户外' },
      { text: '美妆护肤', value: '美妆护肤' },
      { text: '其他', value: '其他' }
    ],
    conditionFilters: [
      { text: '全新', value: '全新' },
      { text: '九成新', value: '九成新' },
      { text: '八成新', value: '八成新' },
      { text: '七成新', value: '七成新' },
      { text: '六成新', value: '六成新' },
      { text: '五成新', value: '五成新' }
    ],
    statusFilters: [
      { text: '草稿', value: 'DRAFT' },
      { text: '已发布', value: 'PUBLISHED' },
      { text: '已上架', value: 'ON_SHELF' },
      { text: '已下架', value: 'OFF_SHELF' }
    ],
    visibilityFilters: [
      { text: '公开', value: 'PUBLIC' },
      { text: '私密', value: 'PRIVATE' },
      { text: '隐藏', value: 'HIDDEN' }
    ]
  } },
  mounted(){ this.loadData() },
  computed:{},
  methods:{
    cStatusText(status){
      const map = {
        'DRAFT': '草稿',
        'PUBLISHED': '已发布',
        'ON_SHELF': '已上架',
        'OFF_SHELF': '已下架'
      }
      return map[status] || status || '-'
    },
    cStatusType(status){
      const map = {
        'DRAFT': 'info',
        'PUBLISHED': 'primary',
        'ON_SHELF': 'success',
        'OFF_SHELF': 'default'
      }
      return map[status] || 'info'
    },
    async loadData(){
      const { commoditiesAPI } = await import('../api/admin/commodities')
      const query = {
        keyword: (this.keyword || '').trim(),
        category: this.activeFilters.category?.[0] || '',
        conditionLevel: this.activeFilters.conditionLevel?.[0] || '',
        status: this.activeFilters.commodityStatus?.[0] || '',
        sellerVisibility: this.activeFilters.sellerVisibility?.[0] || '',
        buyerVisibility: this.activeFilters.buyerVisibility?.[0] || '',
        sortProp: this.sortKey || '',
        sortOrder: this.sortOrder === 'descending' ? 'desc' : (this.sortOrder === 'ascending' ? 'asc' : '')
      }
      const res = await commoditiesAPI.list(this.page, this.pageSize, query)
      if (res && res.success) {
        this.list = res.data?.list || res.data?.commodities || res.data || []
        this.total = res.data?.total ?? this.list.length
        // ✅ 后端已批量查询卖家信息，无需前端再次获取（优化后的代码）
        // 如果后端返回的数据中包含seller信息，直接使用；否则回退到单独查询
        this.list.forEach(row => {
          if (row.seller && row.seller.nickname) {
            // 后端已提供卖家信息，直接使用
            if (!this.sellerMap[row.sellerId]) {
              this.sellerMap[row.sellerId] = {
                profile: {
                  nickname: row.seller.nickname,
                  avatar: row.seller.avatar
                }
              }
            }
          } else {
            // 兼容旧版本：如果后端未提供seller信息，单独查询
            this.fetchSeller(row)
          }
        })
      }
    },
    async fetchSeller(row){
      if (!row || !row.sellerId || this.sellerMap[row.sellerId]) return
      const { usersAPI } = await import('../api/admin/users')
      const u = await usersAPI.get(row.sellerId)
      if (u && u.success) this.sellerMap[row.sellerId] = u.data
    },
    getSellerName(row){
      // ✅ 优先使用后端返回的seller信息
      if (row.seller && row.seller.nickname) {
        return row.seller.nickname
      }
      // 回退到sellerMap（兼容旧版本或单独查询的情况）
      const u = this.sellerMap[row.sellerId]
      if (!u) return row.sellerId || '-'
      return (u.profile?.nickname) || u.username || u.userId
    },
    getSellerAvatar(row){
      // ✅ 优先使用后端返回的seller信息
      if (row.seller && row.seller.avatar) {
        return row.seller.avatar
      }
      // 回退到sellerMap（兼容旧版本或单独查询的情况）
      const u = this.sellerMap[row.sellerId]
      return u?.profile?.avatar || ''
    },
    firstImage(row){
      if (!row || !row.images) return ''
      const arr = row.images.split(',').map(s => s.trim()).filter(Boolean)
      return arr[0] || ''
    },
    formatCommodityAddress(row){
      if (!row) return '-'
      // 优先使用地址快照字段：省+市
      if (row.addressSnapshotProvince && row.addressSnapshotCity) {
        return `${row.addressSnapshotProvince}${row.addressSnapshotCity}`
      }
      // 其次使用完整地址快照
      if (row.addressSnapshotFull) {
        return row.addressSnapshotFull
      }
      // 最后使用旧字段
      return row.location || '-'
    },
    async shelf(row){
      const { commoditiesAPI } = await import('../api/admin/commodities')
      await commoditiesAPI.updateStatus(row.commodityId, 'ON_SHELF')
      this.loadData()
    },
    async unshelf(row){
      const { commoditiesAPI } = await import('../api/admin/commodities')
      await commoditiesAPI.updateStatus(row.commodityId, 'OFF_SHELF')
      this.loadData()
    },
    async remove(row){
      const { commoditiesAPI } = await import('../api/admin/commodities')
      await commoditiesAPI.remove(row.commodityId)
      this.loadData()
    },
    edit(row){
      this.$router.push(`/commodities/${row.commodityId}/edit`)
    },
    visText(v){
      const map = { PUBLIC:'公开', PRIVATE:'私密', HIDDEN:'隐藏' }
      return map[v] || (v || '-')
    },
    visType(v){
      const map = { PUBLIC:'success', PRIVATE:'info', HIDDEN:'default' }
      return map[v] || 'info'
    },
    onSortChange({ prop, order }){
      this.sortKey = prop
      this.sortOrder = order
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
    }
  }
}
</script>


