<template>
  <div>
    <h2>编辑订单</h2>
    <el-form :model="form" label-width="120px" class="edit-form">
      <el-form-item label="订单ID"><span>{{ form.orderId }}</span></el-form-item>
      <el-form-item label="买家ID"><span>{{ form.buyerId }}</span></el-form-item>
      <el-form-item label="卖家ID"><span>{{ form.sellerId }}</span></el-form-item>
      <el-form-item label="商品ID"><span>{{ form.commodityId }}</span></el-form-item>
      <el-form-item label="订单状态">
        <UnifiedSelect 
          v-model="form.orderStatus" 
          :options="statusOptions"
          placeholder="请选择订单状态"
        />
      </el-form-item>
      <el-form-item label="卖家可见性">
        <UnifiedSelect 
          v-model="form.sellerVisibility" 
          :options="visibilityOptions"
          placeholder="请选择卖家可见性"
        />
      </el-form-item>
      <el-form-item label="买家可见性">
        <UnifiedSelect 
          v-model="form.buyerVisibility" 
          :options="visibilityOptions"
          placeholder="请选择买家可见性"
        />
      </el-form-item>
      
      <!-- 只读信息 -->
      <el-divider>订单详情（只读）</el-divider>
      <el-form-item label="支付金额"><span>{{ form.payAmount ?? '-' }}</span></el-form-item>
      <el-form-item label="创建时间"><span>{{ form.createTime || '-' }}</span></el-form-item>
      <el-form-item label="支付时间"><span>{{ form.payTime || '-' }}</span></el-form-item>
      <el-form-item label="发货时间"><span>{{ form.shippingTime || '-' }}</span></el-form-item>
      <el-form-item label="收货时间"><span>{{ form.deliveryTime || '-' }}</span></el-form-item>
      <el-form-item label="物流单号"><span>{{ form.trackingNumber || '-' }}</span></el-form-item>
      <el-form-item label="数量"><span>{{ form.quantity ?? '-' }}</span></el-form-item>
      <el-form-item label="收货地址"><span>{{ form.shippingAddress || '-' }}</span></el-form-item>
      <el-form-item label="备注"><span>{{ form.remark || '-' }}</span></el-form-item>

      <el-form-item>
        <UnifiedButton type="primary" @click="save">保存</UnifiedButton>
        <UnifiedButton @click="$router.back()">返回</UnifiedButton>
      </el-form-item>
    </el-form>
  </div>
</template>

<script>
import UnifiedInput from '../components/common/UnifiedInput.vue'
import UnifiedButton from '../components/common/UnifiedButton.vue'
import UnifiedSelect from '../components/common/UnifiedSelect.vue'
import { ordersAPI } from '../api/admin/orders'
import { ElMessage } from 'element-plus'

export default {
  name: 'OrderEdit',
  components: { UnifiedInput, UnifiedButton, UnifiedSelect },
  data() {
    return {
      form: {
        orderId: '',
        buyerId: '',
        sellerId: '',
        commodityId: '',
        orderStatus: '',
        sellerVisibility: '',
        buyerVisibility: '',
        payAmount: '',
        createTime: '',
        payTime: '',
        shippingTime: '',
        deliveryTime: '',
        trackingNumber: '',
        quantity: '',
        shippingAddress: '',
        remark: ''
      },
      // 状态选择器选项
      statusOptions: [
        { label: '已创建', value: 'CREATED' },
        { label: '已支付', value: 'PAID' },
        { label: '已发货', value: 'SHIPPED' },
        { label: '已完成', value: 'COMPLETED' },
        { label: '已取消', value: 'CANCELLED' },
        { label: '申请退款', value: 'REFUND_REQUESTED' },
        { label: '退款通过', value: 'REFUND_APPROVED' },
        { label: '退款被拒', value: 'REFUND_REJECTED' },
        { label: '申请退货', value: 'RETURN_REQUESTED' },
        { label: '退货通过', value: 'RETURN_APPROVED' },
        { label: '退货被拒', value: 'RETURN_REJECTED' },
        { label: '退货完成', value: 'RETURN_COMPLETED' }
      ],
      // 可见性选择器选项
      visibilityOptions: [
        { label: '公开', value: 'PUBLIC' },
        { label: '私密', value: 'PRIVATE' },
        { label: '隐藏', value: 'HIDDEN' }
      ]
    }
  },
  async mounted() {
    const id = this.$route.params.orderId
    const res = await ordersAPI.get(id)
    if (res && res.success) {
      const o = res.data
      this.form.orderId = o.orderId || ''
      this.form.buyerId = o.buyerId || ''
      this.form.sellerId = o.sellerId || ''
      this.form.commodityId = o.commodityId || ''
      this.form.orderStatus = o.orderStatus || ''
      this.form.sellerVisibility = o.sellerVisibility || ''
      this.form.buyerVisibility = o.buyerVisibility || ''
      this.form.payAmount = o.payAmount ?? ''
      this.form.createTime = o.createTime || ''
      this.form.payTime = o.payTime || ''
      this.form.shippingTime = o.shippingTime || ''
      this.form.deliveryTime = o.deliveryTime || ''
      this.form.trackingNumber = o.trackingNumber || ''
      this.form.quantity = o.quantity ?? ''
      this.form.shippingAddress = o.shippingAddress || ''
      this.form.remark = o.remark || ''
    } else {
      ElMessage.error('获取订单信息失败')
      this.$router.back()
    }
  },
  methods: {
    async save() {
      try {
        const payload = {
          orderStatus: this.form.orderStatus,
          sellerVisibility: this.form.sellerVisibility,
          buyerVisibility: this.form.buyerVisibility
        }
        
        const res = await ordersAPI.updateFull(this.form.orderId, payload)
        if (res && res.success) {
          ElMessage.success('保存成功')
          this.$router.back()
        } else {
          ElMessage.error(res?.message || '保存失败')
        }
      } catch (error) {
        ElMessage.error('保存失败')
        console.error('保存订单失败:', error)
      }
    }
  }
}
</script>

<style scoped>
.edit-form {
  max-width: 800px;
}

.edit-form :deep(.unified-input) {
  width: 100%;
}

.edit-form :deep(.custom-select) {
  width: 100%;
}

.edit-form .el-divider {
  margin: 20px 0;
}

.edit-form span {
  color: #666;
}
</style>

