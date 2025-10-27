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
      <el-table-column label="操作" width="220">
        <template #default="{ row }">
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
  data(){ return { list: [], total: 0, page: 1, pageSize: 20, keyword: '', sortProp: '', sortOrder: '' } },
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


