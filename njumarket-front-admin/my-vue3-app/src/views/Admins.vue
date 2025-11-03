<template>
  <div>
    <h2>管理员账号管理</h2>
    <div style="margin:12px 0; display:flex; gap:8px; align-items:center; flex-wrap:wrap;">
      <UnifiedInput v-model="keyword" placeholder="搜索（用户名/真实姓名/邮箱）" style="width: 360px;" />
      <UnifiedButton type="primary" @click="doSearch">搜索</UnifiedButton>
      <UnifiedButton type="primary" @click="createAdmin">创建管理员</UnifiedButton>
    </div>
    <el-table :data="list" border style="width: 100%" @sort-change="onSortChange" @filter-change="onFilterChange">
      <el-table-column type="expand">
        <template #default="{ row }">
          <div class="admin-expand">
            <div class="expand-section">
              <h4>基本信息</h4>
              <p>管理员ID：{{ row.adminId }}</p>
              <p>用户名：{{ row.username }}</p>
              <p>真实姓名：{{ row.realName || '-' }}</p>
              <p>邮箱：{{ row.email || '-' }}</p>
              <p>部门：{{ row.department || '-' }}</p>
              <p>职位：{{ row.position || '-' }}</p>
            </div>
            <div class="expand-section">
              <h4>权限信息</h4>
              <p>管理员级别：<UnifiedTag :type="row.adminLevel === 'system' ? 'danger' : 'primary'">{{ row.adminLevel === 'system' ? '系统管理员' : '普通管理员' }}</UnifiedTag></p>
              <p>权限列表：{{ row.permissions || '-' }}</p>
              <p>账户状态：<UnifiedTag :type="statusType(row.accountStatus)">{{ statusText(row.accountStatus) }}</UnifiedTag></p>
            </div>
            <div class="expand-section">
              <h4>登录信息</h4>
              <p>最后登录时间：{{ formatDateTime(row.lastLoginTime) }}</p>
              <p>最后登录IP：{{ row.lastLoginIp || '-' }}</p>
              <p>登录次数：{{ row.loginCount ?? 0 }}</p>
              <p>创建时间：{{ formatDateTime(row.createTime) }}</p>
              <p>更新时间：{{ formatDateTime(row.updateTime) }}</p>
            </div>
            <div class="expand-section" v-if="row.remark">
              <h4>备注</h4>
              <p>{{ row.remark }}</p>
            </div>
          </div>
        </template>
      </el-table-column>
      <el-table-column prop="adminId" label="管理员ID" width="240"/>
      <el-table-column prop="username" label="用户名" width="150"/>
      <el-table-column prop="realName" label="真实姓名" width="120"/>
      <el-table-column prop="email" label="邮箱" width="180"/>
      <el-table-column label="级别" width="120">
        <template #default="{ row }">
          <UnifiedTag :type="row.adminLevel === 'system' ? 'danger' : 'primary'">
            {{ row.adminLevel === 'system' ? '系统管理员' : '普通管理员' }}
          </UnifiedTag>
        </template>
      </el-table-column>
      <el-table-column
        prop="accountStatus"
        label="状态"
        width="120"
        :filters="statusFilters"
        column-key="accountStatus"
        filter-placement="bottom-end"
      >
        <template #default="{ row }">
          <UnifiedTag :type="statusType(row.accountStatus)">{{ statusText(row.accountStatus) }}</UnifiedTag>
        </template>
      </el-table-column>
      <el-table-column prop="department" label="部门" width="120"/>
      <el-table-column prop="position" label="职位" width="120"/>
      <el-table-column prop="createTime" label="创建时间" width="170" sortable="custom">
        <template #default="{ row }">
          {{ formatDateTime(row.createTime) }}
        </template>
      </el-table-column>
      <el-table-column prop="lastLoginTime" label="最后登录" width="170" sortable="custom">
        <template #default="{ row }">
          {{ formatDateTime(row.lastLoginTime) || '从未登录' }}
        </template>
      </el-table-column>
      <el-table-column label="操作" width="280">
        <template #default="{ row }">
          <UnifiedButton size="small" type="primary" @click="viewDetail(row)">查看详情</UnifiedButton>
          <UnifiedButton size="small" @click="edit(row)">编辑</UnifiedButton>
          <UnifiedButton size="small" type="danger" @click="remove(row)" :disabled="row.adminLevel === 'system'">删除</UnifiedButton>
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
import { ElMessage, ElMessageBox } from 'element-plus'

export default {
  name: 'Admins',
  components:{ UnifiedButton, UnifiedInput, UnifiedTag, Pagination },
  data(){ 
    return { 
      list: [], total: 0, page: 1, pageSize: 10, keyword: '',
      accountStatus: '', sortProp: '', sortOrder: '',
      statusFilters: [
        { text: '活跃', value: 'ACTIVE' },
        { text: '已暂停', value: 'SUSPENDED' },
        { text: '已封禁', value: 'BANNED' }
      ]
    } 
  },
  mounted(){ this.loadData() },
  methods:{
    createAdmin() {
      this.$router.push('/admins/create')
    },
    statusText(status){
      const map = {
        'ACTIVE': '活跃',
        'SUSPENDED': '已暂停',
        'BANNED': '已封禁'
      }
      return map[status] || status || '-'
    },
    statusType(status){
      const map = {
        'ACTIVE': 'success',
        'SUSPENDED': 'warning',
        'BANNED': 'danger'
      }
      return map[status] || 'info'
    },
    formatDateTime(dateTimeStr) {
      if (!dateTimeStr) return '-'
      try {
        const date = new Date(dateTimeStr)
        if (isNaN(date.getTime())) return dateTimeStr
        
        const year = date.getFullYear()
        const month = String(date.getMonth() + 1).padStart(2, '0')
        const day = String(date.getDate()).padStart(2, '0')
        const hours = String(date.getHours()).padStart(2, '0')
        const minutes = String(date.getMinutes()).padStart(2, '0')
        const seconds = String(date.getSeconds()).padStart(2, '0')
        
        return `${year}-${month}-${day} ${hours}:${minutes}:${seconds}`
      } catch (error) {
        return dateTimeStr
      }
    },
    async loadData(){
      try {
        const { adminsAPI } = await import('../api/admin/admins')
        // ✅ 传递筛选和排序参数
        const res = await adminsAPI.list(
          this.page, 
          this.pageSize, 
          this.keyword ? String(this.keyword) : '', 
          this.accountStatus || '', 
          this.sortProp || '', 
          this.sortOrder || ''
        )
        if (res && res.success) {
          this.list = res.data?.list || res.data || []
          this.total = res.data?.total ?? this.list.length
        } else {
          ElMessage.error(res?.message || '加载管理员列表失败')
        }
      } catch (error) {
        ElMessage.error('加载管理员列表失败')
        console.error('加载管理员列表失败:', error)
      }
    },
    onFilterChange(filters) {
      // ✅ 处理账户状态筛选
      if (filters.accountStatus && filters.accountStatus.length > 0) {
        this.accountStatus = filters.accountStatus[0]
      } else {
        this.accountStatus = ''
      }
      this.page = 1
      this.loadData()
    },
    onSortChange({ prop, order }) {
      // ✅ 处理排序
      if (prop && order) {
        this.sortProp = prop
        this.sortOrder = order === 'ascending' ? 'asc' : 'desc'
      } else {
        this.sortProp = ''
        this.sortOrder = ''
      }
      this.page = 1
      this.loadData()
    },
    doSearch(){
      this.keyword = (this.keyword || '').trim()
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
    viewDetail(row) {
      // 查看详情跳转到编辑页面（展开行已显示详细信息）
      this.$router.push(`/admins/${row.adminId}/edit`)
    },
    edit(row) {
      this.$router.push(`/admins/${row.adminId}/edit`)
    },
    async remove(row) {
      try {
        await ElMessageBox.confirm(
          `确定要删除管理员 "${row.username}" 吗？此操作不可恢复。`,
          '确认删除',
          {
            confirmButtonText: '确定',
            cancelButtonText: '取消',
            type: 'warning'
          }
        )
        
        const { adminsAPI } = await import('../api/admin/admins')
        const res = await adminsAPI.remove(row.adminId)
        if (res && res.success) {
          ElMessage.success('删除成功')
          this.loadData()
        } else {
          ElMessage.error(res?.message || '删除失败')
        }
      } catch (error) {
        if (error !== 'cancel') {
          ElMessage.error('删除失败')
          console.error('删除管理员失败:', error)
        }
      }
    }
  }
}
</script>

<style scoped>
.admin-expand {
  padding: 20px;
  display: flex;
  gap: 30px;
  flex-wrap: wrap;
}

.expand-section {
  flex: 1;
  min-width: 300px;
}

.expand-section h4 {
  margin: 0 0 12px 0;
  font-size: 16px;
  font-weight: normal;
  color: var(--primary-color, #6a015e);
}

.expand-section p {
  margin: 6px 0;
  font-size: 14px;
  color: #666;
}
</style>

