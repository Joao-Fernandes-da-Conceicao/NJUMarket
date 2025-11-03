<template>
  <div>
    <h2>订单管理</h2>
    <div style="margin:12px 0; display:flex; gap:8px; align-items:center; flex-wrap:wrap;">
      <UnifiedInput v-model="keyword" placeholder="搜索（买家ID/卖家ID/商品标题快照）" style="width: 360px;" />
      <UnifiedButton type="primary" @click="doSearch">搜索</UnifiedButton>
    </div>
    <el-table :data="list" border style="width: 100%" @sort-change="onSortChange">
      <el-table-column type="expand">
        <template #default="{ row }">
          <div class="order-expand">
            <div class="expand-section">
              <h4>订单信息</h4>
              <p>数量：{{ row.quantity }}</p>
              <p>地址：{{ row.shippingAddress || '-' }}</p>
              <p>备注：{{ row.remark || '-' }}</p>
              <p>可见性：卖家 {{ row.sellerVisibility || '-' }} / 买家 {{ row.buyerVisibility || '-' }}</p>
              <!-- ✅ 买家信息 -->
              <div class="user-info">
                <h5>买家信息</h5>
                <div class="user-detail">
                  <el-avatar :size="40" :src="getAvatarUrl(getBuyerAvatar(row))" class="user-avatar">
                    <span v-if="!getBuyerAvatar(row)">无头像</span>
                  </el-avatar>
                  <div class="user-text">
                    <p class="user-name">{{ getBuyerName(row) }}</p>
                    <p class="user-id">ID: {{ row.buyerId || '-' }}</p>
                  </div>
                </div>
              </div>
              <!-- ✅ 卖家信息 -->
              <div class="user-info">
                <h5>卖家信息</h5>
                <div class="user-detail">
                  <el-avatar :size="40" :src="getAvatarUrl(getSellerAvatar(row))" class="user-avatar">
                    <span v-if="!getSellerAvatar(row)">无头像</span>
                  </el-avatar>
                  <div class="user-text">
                    <p class="user-name">{{ getSellerName(row) }}</p>
                    <p class="user-id">ID: {{ row.sellerId || '-' }}</p>
                  </div>
                </div>
              </div>
            </div>
            <div class="expand-section">
              <h4>退货/退款</h4>
              <p>原因：{{ row.returnReason || '-' }}</p>
              <p>申请时间：{{ row.returnRequestTime || '-' }}</p>
              <p>审批时间：{{ row.returnApprovalTime || '-' }}</p>
              <p>拒绝原因：{{ row.returnRejectionReason || '-' }}</p>
              <p>退货单号：{{ row.returnTrackingNumber || '-' }}</p>
              <p>完成时间：{{ row.returnCompletionTime || '-' }}</p>
            </div>
            <div class="expand-section">
              <h4>商品快照</h4>
              <p>标题：{{ row.commoditySnapshotTitle || '-' }}</p>
              <p>描述：{{ row.commoditySnapshotDescription || '-' }}</p>
              <p>价格：{{ row.commoditySnapshotPrice ?? '-' }}</p>
              <p>分类/成色：{{ row.commoditySnapshotCategory || '-' }} / {{ row.commoditySnapshotConditionLevel || '-' }}</p>
              <p>地点：{{ row.commoditySnapshotLocation || '-' }}</p>
              <p>状态：{{ row.commoditySnapshotStatus || '-' }}</p>
              <p>卖家：{{ row.commoditySnapshotSellerName || '-' }}（{{ row.commoditySnapshotSellerPhone || '-' }} / {{ row.commoditySnapshotSellerEmail || '-' }}）</p>
              <p>快照时间：{{ row.commoditySnapshotTime || '-' }}</p>
              <div class="snapshot-images">
                <img v-for="(img, i) in snapshotImages(row)" :key="i" :src="img" alt="snap" />
                <span v-if="!snapshotImages(row).length">无图片</span>
              </div>
            </div>
          </div>
        </template>
      </el-table-column>
      <el-table-column prop="orderId" label="订单ID" width="280"/>
      <el-table-column prop="buyerId" label="买家" width="180"/>
      <el-table-column prop="sellerId" label="卖家" width="180"/>
      <el-table-column label="状态" width="140">
        <template #default="{ row }">
          <UnifiedTag :type="statusType(row.orderStatus)">{{ statusText(row.orderStatus) }}</UnifiedTag>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="创建时间" width="170" sortable="custom"/>
      <el-table-column prop="commoditySnapshotTitle" label="商品标题(快照)" min-width="160"/>
      <el-table-column label="卖家可见" width="120">
        <template #default="{ row }">
          <UnifiedTag :type="visType(row.sellerVisibility)">{{ visText(row.sellerVisibility) }}</UnifiedTag>
        </template>
      </el-table-column>
      <el-table-column label="买家可见" width="120">
        <template #default="{ row }">
          <UnifiedTag :type="visType(row.buyerVisibility)">{{ visText(row.buyerVisibility) }}</UnifiedTag>
        </template>
      </el-table-column>
      <el-table-column prop="payAmount" label="金额" width="120"/>
      <el-table-column label="操作" width="280">
        <template #default="{ row }">
          <UnifiedButton size="small" @click="edit(row)">编辑</UnifiedButton>
          <UnifiedButton size="small" @click="mark(row,'PAID')">设为已支付</UnifiedButton>
          <UnifiedButton size="small" type="danger" @click="remove(row)">删除</UnifiedButton>
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
  name: 'Orders',
  components:{ UnifiedButton, UnifiedInput, UnifiedTag, Pagination },
  data(){ 
    return { 
      list: [], total: 0, page: 1, pageSize: 10, keyword: '', sortProp: '', sortOrder: ''
    } 
  },
  mounted(){ this.loadData() },
  methods:{
    statusText(status){
      const map = {
        'CREATED': '已创建',
        'PAID': '已支付',
        'SHIPPED': '已发货',
        'COMPLETED': '已完成',
        'CANCELLED': '已取消',
        'REFUND_REQUESTED': '申请退款',
        'REFUND_APPROVED': '退款通过',
        'REFUND_REJECTED': '退款被拒',
        'RETURN_REQUESTED': '申请退货',
        'RETURN_APPROVED': '退货通过',
        'RETURN_REJECTED': '退货被拒',
        'RETURN_COMPLETED': '退货完成'
      }
      return map[status] || status || '-'
    },
    statusType(status){
      const map = {
        'CREATED': 'info',
        'PAID': 'primary',
        'SHIPPED': 'warning',
        'COMPLETED': 'success',
        'CANCELLED': 'default',
        'REFUND_REQUESTED': 'warning',
        'REFUND_APPROVED': 'success',
        'REFUND_REJECTED': 'danger',
        'RETURN_REQUESTED': 'warning',
        'RETURN_APPROVED': 'success',
        'RETURN_REJECTED': 'danger',
        'RETURN_COMPLETED': 'success'
      }
      return map[status] || 'info'
    },
    snapshotImages(row){
      if (!row || !row.commoditySnapshotImages) return []
      return row.commoditySnapshotImages.split(',').map(s => s.trim()).filter(Boolean)
    },
    visText(v){
      const map = { PUBLIC:'公开', PRIVATE:'私密', HIDDEN:'隐藏' }
      return map[v] || (v || '-')
    },
    visType(v){
      const map = { PUBLIC:'success', PRIVATE:'info', HIDDEN:'default' }
      return map[v] || 'info'
    },
    // ✅ 获取买家昵称（优先使用后端返回的buyer信息）
    getBuyerName(row) {
      if (row.buyer && row.buyer.nickname) {
        return row.buyer.nickname
      }
      return row.buyerId || '-'
    },
    // ✅ 获取买家头像（优先使用后端返回的buyer信息）
    getBuyerAvatar(row) {
      if (row.buyer && row.buyer.avatar) {
        return row.buyer.avatar
      }
      return ''
    },
    // ✅ 获取卖家昵称（优先使用后端返回的seller信息）
    getSellerName(row) {
      if (row.seller && row.seller.nickname) {
        return row.seller.nickname
      }
      return row.sellerId || '-'
    },
    // ✅ 获取卖家头像（优先使用后端返回的seller信息）
    getSellerAvatar(row) {
      if (row.seller && row.seller.avatar) {
        return row.seller.avatar
      }
      return ''
    },
    // ✅ 获取头像完整URL
    getAvatarUrl(avatarUrl) {
      if (!avatarUrl) return 'http://localhost:8080/uploads/avatars/default-avatar.png'
      // 如果已经是完整URL，直接返回
      if (avatarUrl.startsWith('http')) return avatarUrl
      // 如果包含完整路径，直接返回
      if (avatarUrl.includes('/')) return avatarUrl
      // 从URL中提取文件名
      const fileName = avatarUrl.split('/').pop()
      return `http://localhost:8080/uploads/avatars/${fileName}`
    },
    async loadData(){
      const { ordersAPI } = await import('../api/admin/orders')
      const res = await ordersAPI.list(this.page, this.pageSize, { keyword: (this.keyword||'').trim(), sortProp: this.sortProp, sortOrder: this.sortOrder === 'descending' ? 'desc' : (this.sortOrder === 'ascending' ? 'asc' : '') })
      if (res && res.success) {
        this.list = res.data?.list || res.data?.orders || res.data || []
        this.total = res.data?.total ?? this.list.length
      }
    },
    doSearch(){
      this.keyword = (this.keyword || '').trim()
      this.page = 1
      this.loadData()
    },
    onSortChange({ prop, order }){
      this.sortProp = prop
      this.sortOrder = order
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
    edit(row) {
      this.$router.push(`/orders/${row.orderId}/edit`)
    },
    async mark(row, status){
      const { ordersAPI } = await import('../api/admin/orders')
      await ordersAPI.update(row.orderId, { status })
      this.loadData()
    },
    async remove(row){
      const { ordersAPI } = await import('../api/admin/orders')
      await ordersAPI.remove(row.orderId)
      this.loadData()
    }
  }
}
</script>

<style scoped>
/* 展开行样式 */
.order-expand {
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

/* ✅ 用户信息显示 */
.user-info {
  margin-top: 20px;
  padding-top: 15px;
  border-top: 1px solid #eee;
}

.user-info h5 {
  margin: 0 0 12px 0;
  font-size: 14px;
  font-weight: normal;
  color: var(--primary-color, #6a015e);
}

.user-detail {
  display: flex;
  align-items: center;
  gap: 12px;
}

.user-avatar {
  flex-shrink: 0;
  border: 2px solid var(--primary-color, #6a015e);
}

.user-text {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.user-name {
  margin: 0;
  font-size: 14px;
  font-weight: 500;
  color: #333;
}

.user-id {
  margin: 0;
  font-size: 12px;
  color: #999;
}

.snapshot-images {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  margin-top: 10px;
}

.snapshot-images img {
  width: 80px;
  height: 80px;
  object-fit: cover;
  border-radius: 8px;
  border: 1px solid #ddd;
}
</style>


