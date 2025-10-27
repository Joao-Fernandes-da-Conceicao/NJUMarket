<template>
  <div>
    <h2>编辑用户</h2>
    <el-form :model="form" label-width="100px" class="edit-form">
      <el-form-item label="用户ID"><span>{{ form.userId }}</span></el-form-item>
      <el-form-item label="用户名">
        <UnifiedInput v-model="form.username" placeholder="请输入用户名" />
      </el-form-item>
      <el-form-item label="手机号">
        <UnifiedInput v-model="form.primaryPhone" placeholder="请输入手机号" />
      </el-form-item>
      <el-form-item label="状态">
        <UnifiedInput v-model="form.accountStatus" placeholder="ACTIVE/SUSPENDED/BANNED" />
      </el-form-item>

      <el-form-item label="昵称">
        <UnifiedInput v-model="form.nickname" placeholder="请输入昵称" />
      </el-form-item>
      <el-form-item label="头像URL">
        <UnifiedInput v-model="form.avatar" placeholder="请输入头像URL" />
      </el-form-item>
      <el-form-item label="信用分">
        <UnifiedInput v-model="form.creditScore" type="number" placeholder="0-100" />
      </el-form-item>
      <el-form-item label="买家评分">
        <UnifiedInput v-model="form.buyerRating" type="number" placeholder="0-5" />
      </el-form-item>
      <el-form-item label="卖家评分">
        <UnifiedInput v-model="form.sellerRating" type="number" placeholder="0-5" />
      </el-form-item>
      <el-form-item label="卖出次数">
        <UnifiedInput v-model="form.totalSales" type="number" placeholder="整数" />
      </el-form-item>
      <el-form-item label="购入次数">
        <UnifiedInput v-model="form.totalPurchases" type="number" placeholder="整数" />
      </el-form-item>
      <el-form-item label="会员等级">
        <UnifiedInput v-model="form.vipLevel" placeholder="NORMAL/BRONZE/SILVER/GOLD/PLATINUM" />
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
import { usersAPI } from '../api/admin/users'
import { reviewUserPayload } from '../utils/userReview'

export default {
  name: 'UserEdit',
  components:{ UnifiedInput, UnifiedButton },
  data(){
    return {
      form: {
        userId: '', username: '', primaryPhone: '', accountStatus: '',
        nickname: '', avatar: '', creditScore: '', buyerRating: '', sellerRating: '',
        totalSales: '', totalPurchases: '', vipLevel: ''
      }
    }
  },
  async mounted(){
    const id = this.$route.params.userId
    const res = await usersAPI.get(id)
    if (res && res.success) {
      const u = res.data
      this.form.userId = u.userId
      this.form.username = u.username || ''
      this.form.primaryPhone = u.primaryPhone || ''
      this.form.accountStatus = u.accountStatus || ''
      const p = u.profile || {}
      this.form.nickname = p.nickname || ''
      this.form.avatar = p.avatar || ''
      this.form.creditScore = p.creditScore ?? ''
      this.form.buyerRating = p.buyerRating ?? ''
      this.form.sellerRating = p.sellerRating ?? ''
      this.form.totalSales = p.totalSales ?? ''
      this.form.totalPurchases = p.totalPurchases ?? ''
      this.form.vipLevel = p.vipLevel || ''
    }
  },
  methods:{
    async save(){
      const payload = { ...this.form }
      const err = reviewUserPayload(payload)
      if (err) { this.$message.error(err); return }
      const id = this.form.userId
      const res = await usersAPI.updateFull(id, payload)
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

