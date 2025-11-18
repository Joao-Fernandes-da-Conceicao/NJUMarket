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
      <!-- 可编辑的地址快照 -->
      <el-divider>收货地址快照（可编辑）</el-divider>
      <el-form-item label="地图标注">
        <AddressMapPicker
          v-model="shippingMapLocation"
          :default-location="shippingMapDefaultLocation"
          @change="handleShippingMapLocationChange"
        />
      </el-form-item>
      <el-form-item label="省 / 市 / 区">
        <div class="region-row">
          <UnifiedInput
            v-model="form.shippingAddressSnapshotProvince"
            placeholder="省份"
            @input="handleShippingAddressPartChange"
          />
          <UnifiedInput
            v-model="form.shippingAddressSnapshotCity"
            placeholder="城市"
            @input="handleShippingAddressPartChange"
          />
          <UnifiedInput
            v-model="form.shippingAddressSnapshotDistrict"
            placeholder="区/县"
            @input="handleShippingAddressPartChange"
          />
        </div>
      </el-form-item>
      <el-form-item label="街道">
        <UnifiedInput
          v-model="form.shippingAddressSnapshotStreet"
          placeholder="街道/镇"
          @input="handleShippingAddressPartChange"
        />
      </el-form-item>
      <el-form-item label="详细地址">
        <UnifiedInput
          v-model="form.shippingAddressSnapshotDetail"
          placeholder="楼栋、门牌等"
          @input="handleShippingAddressPartChange"
        />
      </el-form-item>
      <el-form-item label="完整地址">
        <el-input
          v-model="form.shippingAddressSnapshotFull"
          type="textarea"
          rows="2"
          placeholder="默认根据上方字段拼接，可手动调整"
          @input="handleShippingFullAddressInput"
        />
      </el-form-item>
      <el-form-item label="收货人姓名">
        <UnifiedInput v-model="form.shippingAddressSnapshotRecipientName" placeholder="请输入收货人姓名" />
      </el-form-item>
      <el-form-item label="收货人电话">
        <UnifiedInput v-model="form.shippingAddressSnapshotRecipientPhone" placeholder="请输入收货人电话" />
      </el-form-item>

      <el-divider>发货地址快照（可编辑）</el-divider>
      <el-form-item label="地图标注">
        <AddressMapPicker
          v-model="commodityMapLocation"
          :default-location="commodityMapDefaultLocation"
          @change="handleCommodityMapLocationChange"
        />
      </el-form-item>
      <el-form-item label="省 / 市 / 区">
        <div class="region-row">
          <UnifiedInput
            v-model="form.commoditySnapshotAddressProvince"
            placeholder="省份"
            @input="handleCommodityAddressPartChange"
          />
          <UnifiedInput
            v-model="form.commoditySnapshotAddressCity"
            placeholder="城市"
            @input="handleCommodityAddressPartChange"
          />
          <UnifiedInput
            v-model="form.commoditySnapshotAddressDistrict"
            placeholder="区/县"
            @input="handleCommodityAddressPartChange"
          />
        </div>
      </el-form-item>
      <el-form-item label="街道">
        <UnifiedInput
          v-model="form.commoditySnapshotAddressStreet"
          placeholder="街道/镇"
          @input="handleCommodityAddressPartChange"
        />
      </el-form-item>
      <el-form-item label="详细地址">
        <UnifiedInput
          v-model="form.commoditySnapshotAddressDetail"
          placeholder="楼栋、门牌等"
          @input="handleCommodityAddressPartChange"
        />
      </el-form-item>
      <el-form-item label="完整地址">
        <el-input
          v-model="form.commoditySnapshotAddressFull"
          type="textarea"
          rows="2"
          placeholder="默认根据上方字段拼接，可手动调整"
          @input="handleCommodityFullAddressInput"
        />
      </el-form-item>

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
import AddressMapPicker from '../components/address/AddressMapPicker.vue'
import { ordersAPI } from '../api/admin/orders'
import { ElMessage } from 'element-plus'

export default {
  name: 'OrderEdit',
  components: { UnifiedInput, UnifiedButton, UnifiedSelect, AddressMapPicker },
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
        shippingAddressSnapshotProvince: '',
        shippingAddressSnapshotCity: '',
        shippingAddressSnapshotDistrict: '',
        shippingAddressSnapshotStreet: '',
        shippingAddressSnapshotDetail: '',
        shippingAddressSnapshotFull: '',
        shippingAddressSnapshotRecipientName: '',
        shippingAddressSnapshotRecipientPhone: '',
        commoditySnapshotAddressProvince: '',
        commoditySnapshotAddressCity: '',
        commoditySnapshotAddressDistrict: '',
        commoditySnapshotAddressStreet: '',
        commoditySnapshotAddressDetail: '',
        commoditySnapshotAddressFull: '',
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
      ],
      // 地图位置相关
      shippingMapLocation: null,
      shippingMapDefaultLocation: { longitude: 118.959, latitude: 32.114 }, // 默认南京大学位置
      commodityMapLocation: null,
      commodityMapDefaultLocation: { longitude: 118.959, latitude: 32.114 },
      shippingAddressFullManuallyEdited: false,
      commodityAddressFullManuallyEdited: false,
      addressSyncReady: false
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
      this.form.shippingAddressSnapshotProvince = o.shippingAddressSnapshotProvince || ''
      this.form.shippingAddressSnapshotCity = o.shippingAddressSnapshotCity || ''
      this.form.shippingAddressSnapshotDistrict = o.shippingAddressSnapshotDistrict || ''
      this.form.shippingAddressSnapshotStreet = o.shippingAddressSnapshotStreet || ''
      this.form.shippingAddressSnapshotDetail = o.shippingAddressSnapshotDetail || ''
      this.form.shippingAddressSnapshotFull = o.shippingAddressSnapshotFull || ''
      this.form.shippingAddressSnapshotRecipientName = o.shippingAddressSnapshotRecipientName || ''
      this.form.shippingAddressSnapshotRecipientPhone = o.shippingAddressSnapshotRecipientPhone || ''
      this.form.commoditySnapshotAddressProvince = o.commoditySnapshotAddressProvince || ''
      this.form.commoditySnapshotAddressCity = o.commoditySnapshotAddressCity || ''
      this.form.commoditySnapshotAddressDistrict = o.commoditySnapshotAddressDistrict || ''
      this.form.commoditySnapshotAddressStreet = o.commoditySnapshotAddressStreet || ''
      this.form.commoditySnapshotAddressDetail = o.commoditySnapshotAddressDetail || ''
      this.form.commoditySnapshotAddressFull = o.commoditySnapshotAddressFull || ''

      // 初始化完整地址
      this.shippingAddressFullManuallyEdited = false
      this.commodityAddressFullManuallyEdited = false
      this.updateShippingFullAddress(true)
      this.updateCommodityFullAddress(true)

      // 初始化收货地址地图位置（根据地址快照）
      if (this.form.shippingAddressSnapshotFull) {
        this.shippingMapLocation = {
          address: this.form.shippingAddressSnapshotFull
        }
      }

      // 初始化发货地址地图位置（根据地址快照）
      if (this.form.commoditySnapshotAddressFull) {
        this.commodityMapLocation = {
          address: this.form.commoditySnapshotAddressFull
        }
      }

      this.addressSyncReady = true
    } else {
      ElMessage.error('获取订单信息失败')
      this.$router.back()
    }
  },
  methods: {
    formatShippingAddress(order){
      if (!order) return '-'
      if (order.shippingAddressSnapshotFull) return order.shippingAddressSnapshotFull
      const parts = []
      if (order.shippingAddressSnapshotProvince) parts.push(order.shippingAddressSnapshotProvince)
      if (order.shippingAddressSnapshotCity) parts.push(order.shippingAddressSnapshotCity)
      if (order.shippingAddressSnapshotDistrict) parts.push(order.shippingAddressSnapshotDistrict)
      if (order.shippingAddressSnapshotStreet) parts.push(order.shippingAddressSnapshotStreet)
      if (order.shippingAddressSnapshotDetail) parts.push(order.shippingAddressSnapshotDetail)
      return parts.length ? parts.join('') : (order.shippingAddress || '-')
    },
    formatCommodityAddress(order){
      if (!order) return '-'
      if (order.commoditySnapshotAddressFull) return order.commoditySnapshotAddressFull
      const parts = []
      if (order.commoditySnapshotAddressProvince) parts.push(order.commoditySnapshotAddressProvince)
      if (order.commoditySnapshotAddressCity) parts.push(order.commoditySnapshotAddressCity)
      if (order.commoditySnapshotAddressDistrict) parts.push(order.commoditySnapshotAddressDistrict)
      if (order.commoditySnapshotAddressStreet) parts.push(order.commoditySnapshotAddressStreet)
      if (order.commoditySnapshotAddressDetail) parts.push(order.commoditySnapshotAddressDetail)
      return parts.length ? parts.join('') : '-'
    },
    // 收货地址处理方法
    handleShippingAddressPartChange() {
      this.updateShippingFullAddress()
    },
    handleShippingFullAddressInput() {
      this.shippingAddressFullManuallyEdited = true
    },
    updateShippingFullAddress(force = false) {
      if (!force && this.shippingAddressFullManuallyEdited) return
      const parts = [
        this.form.shippingAddressSnapshotProvince,
        this.form.shippingAddressSnapshotCity,
        this.form.shippingAddressSnapshotDistrict,
        this.form.shippingAddressSnapshotStreet,
        this.form.shippingAddressSnapshotDetail
      ].map(item => (item || '').trim()).filter(Boolean)
      this.form.shippingAddressSnapshotFull = parts.join('')
    },
    handleShippingMapLocationChange(location) {
      if (!this.addressSyncReady || !location) return
      this.shippingMapLocation = { ...location }
      if (location.province) this.form.shippingAddressSnapshotProvince = location.province
      if (location.city) this.form.shippingAddressSnapshotCity = location.city
      if (location.district) this.form.shippingAddressSnapshotDistrict = location.district
      if (location.streetAddress) this.form.shippingAddressSnapshotStreet = location.streetAddress
      if (location.detailAddress) this.form.shippingAddressSnapshotDetail = location.detailAddress
      this.shippingAddressFullManuallyEdited = false
      if (location.address) {
        this.form.shippingAddressSnapshotFull = location.address
      } else {
        this.updateShippingFullAddress(true)
      }
    },
    // 发货地址处理方法
    handleCommodityAddressPartChange() {
      this.updateCommodityFullAddress()
    },
    handleCommodityFullAddressInput() {
      this.commodityAddressFullManuallyEdited = true
    },
    updateCommodityFullAddress(force = false) {
      if (!force && this.commodityAddressFullManuallyEdited) return
      const parts = [
        this.form.commoditySnapshotAddressProvince,
        this.form.commoditySnapshotAddressCity,
        this.form.commoditySnapshotAddressDistrict,
        this.form.commoditySnapshotAddressStreet,
        this.form.commoditySnapshotAddressDetail
      ].map(item => (item || '').trim()).filter(Boolean)
      this.form.commoditySnapshotAddressFull = parts.join('')
    },
    handleCommodityMapLocationChange(location) {
      if (!this.addressSyncReady || !location) return
      this.commodityMapLocation = { ...location }
      if (location.province) this.form.commoditySnapshotAddressProvince = location.province
      if (location.city) this.form.commoditySnapshotAddressCity = location.city
      if (location.district) this.form.commoditySnapshotAddressDistrict = location.district
      if (location.streetAddress) this.form.commoditySnapshotAddressStreet = location.streetAddress
      if (location.detailAddress) this.form.commoditySnapshotAddressDetail = location.detailAddress
      this.commodityAddressFullManuallyEdited = false
      if (location.address) {
        this.form.commoditySnapshotAddressFull = location.address
      } else {
        this.updateCommodityFullAddress(true)
      }
    },
    async save() {
      try {
        const payload = {
          orderStatus: this.form.orderStatus,
          sellerVisibility: this.form.sellerVisibility,
          buyerVisibility: this.form.buyerVisibility,
          // 收货地址快照字段
          shippingAddressSnapshotProvince: this.form.shippingAddressSnapshotProvince || null,
          shippingAddressSnapshotCity: this.form.shippingAddressSnapshotCity || null,
          shippingAddressSnapshotDistrict: this.form.shippingAddressSnapshotDistrict || null,
          shippingAddressSnapshotStreet: this.form.shippingAddressSnapshotStreet || null,
          shippingAddressSnapshotDetail: this.form.shippingAddressSnapshotDetail || null,
          shippingAddressSnapshotFull: this.form.shippingAddressSnapshotFull || null,
          shippingAddressSnapshotRecipientName: this.form.shippingAddressSnapshotRecipientName || null,
          shippingAddressSnapshotRecipientPhone: this.form.shippingAddressSnapshotRecipientPhone || null,
          // 发货地址快照字段
          commoditySnapshotAddressProvince: this.form.commoditySnapshotAddressProvince || null,
          commoditySnapshotAddressCity: this.form.commoditySnapshotAddressCity || null,
          commoditySnapshotAddressDistrict: this.form.commoditySnapshotAddressDistrict || null,
          commoditySnapshotAddressStreet: this.form.commoditySnapshotAddressStreet || null,
          commoditySnapshotAddressDetail: this.form.commoditySnapshotAddressDetail || null,
          commoditySnapshotAddressFull: this.form.commoditySnapshotAddressFull || null
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

.address-block {
  display: flex;
  flex-direction: column;
  gap: 4px;
  margin: 0;
}

.region-row {
  display: flex;
  gap: 8px;
}

.region-row :deep(.unified-input) {
  flex: 1;
}

.address-block p {
  margin: 0;
  line-height: 1.5;
}

.address-sub {
  font-size: 13px;
  color: #888;
}
</style>

