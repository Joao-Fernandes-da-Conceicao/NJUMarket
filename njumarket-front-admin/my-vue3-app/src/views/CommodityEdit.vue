<template>
  <div>
    <h2>编辑商品</h2>
    <el-form :model="form" label-width="100px" class="edit-form">
      <el-form-item label="商品ID"><span>{{ form.commodityId }}</span></el-form-item>
      <el-form-item label="卖家ID"><span>{{ form.sellerId }}</span></el-form-item>
      <el-form-item label="标题"><UnifiedInput v-model="form.title" placeholder="请输入标题" /></el-form-item>
      <el-form-item label="描述"><UnifiedInput v-model="form.description" type="textarea" placeholder="请输入描述" /></el-form-item>
      <el-form-item label="价格"><UnifiedInput v-model="form.price" type="number" placeholder="请输入价格" /></el-form-item>
      <el-form-item label="库存"><UnifiedInput v-model="form.stock" type="number" placeholder="请输入库存" /></el-form-item>
      <el-form-item label="位置"><UnifiedInput v-model="form.location" placeholder="请输入位置" /></el-form-item>
      <el-form-item label="分类"><UnifiedInput v-model="form.category" placeholder="电子产品/服装配饰/图书文具/生活用品/运动户外/美妆护肤/其他" /></el-form-item>
      <el-form-item label="成色"><UnifiedInput v-model="form.conditionLevel" placeholder="请输入成色等级" /></el-form-item>
      <el-form-item label="状态"><UnifiedInput v-model="form.commodityStatus" placeholder="DRAFT/PUBLISHED/ON_SHELF/OFF_SHELF" /></el-form-item>
      <el-form-item label="卖家可见性"><UnifiedInput v-model="form.sellerVisibility" placeholder="PUBLIC/PRIVATE/HIDDEN" /></el-form-item>
      <el-form-item label="买家可见性"><UnifiedInput v-model="form.buyerVisibility" placeholder="PUBLIC/PRIVATE/HIDDEN" /></el-form-item>
      <el-form-item label="点击量"><UnifiedInput v-model="form.clickCount" type="number" placeholder="请输入点击量" /></el-form-item>
      <el-form-item label="图片(逗号)"><UnifiedInput v-model="form.images" type="textarea" placeholder="以半角逗号分隔URL" /></el-form-item>
      <el-form-item label="上架时间"><span>{{ form.publishTime || '-' }}</span></el-form-item>
      <el-form-item label="举报计数"><span>{{ form.reportCount ?? 0 }}</span></el-form-item>
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
import { commoditiesAPI } from '../api/admin/commodities'
import { reviewCommodityPayload } from '../utils/commodityReview'

export default {
  name: 'CommodityEdit',
  components:{ UnifiedInput, UnifiedButton },
  data(){
    return {
      form: {
        commodityId: '', sellerId: '', title: '', description: '', price: '', stock: '',
        location: '', category: '', conditionLevel: '', commodityStatus: '',
        sellerVisibility: '', buyerVisibility: '', publishTime: '', reportCount: 0,
        clickCount: '', images: ''
      }
    }
  },
  async mounted(){
    const id = this.$route.params.commodityId
    const res = await commoditiesAPI.get(id)
    if (res && res.success) {
      const c = res.data
      this.form = {
        commodityId: c.commodityId,
        sellerId: c.sellerId || '',
        title: c.title || '',
        description: c.description || '',
        price: c.price ?? '',
        stock: c.stock ?? '',
        location: c.location || '',
        category: c.category || '',
        conditionLevel: c.conditionLevel || '',
        commodityStatus: c.commodityStatus || '',
        sellerVisibility: c.sellerVisibility || '',
        buyerVisibility: c.buyerVisibility || '',
        publishTime: c.publishTime || '',
        reportCount: c.reportCount ?? 0,
        clickCount: c.clickCount ?? '',
        images: c.images || ''
      }
    }
  },
  methods:{
    async save(){
      const id = this.form.commodityId
      const payload = { ...this.form }
      const err = reviewCommodityPayload(payload)
      if (err) { this.$message.error(err); return }
      const res = await commoditiesAPI.updateFull(id, payload)
      if (res && res.success) {
        this.$message.success('保存成功')
        this.$router.back()
      } else {
        this.$message.error(res?.message || '保存失败')
      }
    }
  }
}
</script>

<style scoped>
.edit-form { max-width: 720px; }
.edit-form :deep(.unified-input) { width: 100%; }
</style>

