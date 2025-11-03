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
      <el-form-item label="分类">
        <UnifiedSelect 
          v-model="form.category" 
          :options="categoryOptions"
          placeholder="请选择分类"
        />
      </el-form-item>
      <el-form-item label="成色">
        <UnifiedSelect 
          v-model="form.conditionLevel" 
          :options="conditionOptions"
          placeholder="请选择成色"
        />
      </el-form-item>
      <el-form-item label="状态">
        <UnifiedSelect 
          v-model="form.commodityStatus" 
          :options="statusOptions"
          placeholder="请选择状态"
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
      <el-form-item label="点击量"><UnifiedInput v-model="form.clickCount" type="number" placeholder="请输入点击量" /></el-form-item>
      <el-form-item label="图片">
        <div class="upload-section">
          <el-upload
            :action="uploadUrl"
            :headers="uploadHeaders"
            :file-list="fileList"
            :on-success="handleUploadSuccess"
            :on-error="handleUploadError"
            :before-upload="beforeUpload"
            :on-remove="handleRemove"
            list-type="picture-card"
            :limit="6"
            accept="image/*"
          >
            <el-icon><Plus /></el-icon>
          </el-upload>
          <div class="upload-tip">
            <p>支持 JPG、PNG 格式，单张图片不超过 5MB</p>
            <p>建议上传多张图片，最多 6 张</p>
          </div>
        </div>
      </el-form-item>
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
import UnifiedSelect from '../components/common/UnifiedSelect.vue'
import { commoditiesAPI } from '../api/admin/commodities'
import { reviewCommodityPayload } from '../utils/commodityReview'
import { ElMessage } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'

export default {
  name: 'CommodityEdit',
  components:{ UnifiedInput, UnifiedButton, UnifiedSelect, Plus },
  data(){
    return {
      form: {
        commodityId: '', sellerId: '', title: '', description: '', price: '', stock: '',
        location: '', category: '', conditionLevel: '', commodityStatus: '',
        sellerVisibility: '', buyerVisibility: '', publishTime: '', reportCount: 0,
        clickCount: '', images: ''
      },
      // 选择器选项
      categoryOptions: [
        { label: '电子产品', value: '电子产品' },
        { label: '服装配饰', value: '服装配饰' },
        { label: '图书文具', value: '图书文具' },
        { label: '生活用品', value: '生活用品' },
        { label: '运动户外', value: '运动户外' },
        { label: '美妆护肤', value: '美妆护肤' },
        { label: '其他', value: '其他' }
      ],
      conditionOptions: [
        { label: '全新', value: '全新' },
        { label: '九成新', value: '九成新' },
        { label: '八成新', value: '八成新' },
        { label: '七成新', value: '七成新' },
        { label: '六成新', value: '六成新' },
        { label: '五成新', value: '五成新' }
      ],
      statusOptions: [
        { label: '草稿', value: 'DRAFT' },
        { label: '已发布', value: 'PUBLISHED' },
        { label: '已上架', value: 'ON_SHELF' },
        { label: '已下架', value: 'OFF_SHELF' }
      ],
      visibilityOptions: [
        { label: '公开', value: 'PUBLIC' },
        { label: '私密', value: 'PRIVATE' },
        { label: '隐藏', value: 'HIDDEN' }
      ],
      // 图片上传相关
      fileList: [],
      uploadUrl: 'http://localhost:8080/api/user/commodity/upload-image',
      uploadHeaders: {}
    }
  },
  async mounted(){
    // 设置上传请求头（使用管理员token）
    const token = localStorage.getItem('adminToken')
    if (token) {
      this.uploadHeaders = {
        'Authorization': `Bearer ${token}`
      }
    }
    
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
      
      // 处理图片列表
      this.initImageList()
    }
  },
  methods:{
    // 初始化图片列表
    initImageList() {
      if (this.form.images) {
        // 图片可能是逗号分隔的字符串
        const imageUrls = typeof this.form.images === 'string' 
          ? this.form.images.split(',').map(s => s.trim()).filter(Boolean)
          : (Array.isArray(this.form.images) ? this.form.images : [])
        
        this.fileList = imageUrls.map((url, index) => ({
          name: `image-${index}`,
          url: this.getImageUrl(url),
          response: { success: true, data: { imageUrl: url } }
        }))
      }
    },
    // 获取图片URL
    getImageUrl(imageUrl) {
      if (!imageUrl) return ''
      if (imageUrl.startsWith('http')) return imageUrl
      if (imageUrl.includes('/')) return imageUrl
      const fileName = imageUrl.split('/').pop()
      return `http://localhost:8080/uploads/commodities/${fileName}`
    },
    // 上传前检查
    beforeUpload(file) {
      const isImage = file.type.startsWith('image/')
      const isLt5M = file.size / 1024 / 1024 < 5
      
      if (!isImage) {
        ElMessage.error('只能上传图片文件!')
        return false
      }
      if (!isLt5M) {
        ElMessage.error('图片大小不能超过 5MB!')
        return false
      }
      return true
    },
    // 上传成功
    handleUploadSuccess(response) {
      if (response.success) {
        const imageUrl = response.data?.imageUrl || response.data
        // 添加到图片列表
        if (!this.form.images) {
          this.form.images = imageUrl
        } else {
          const images = typeof this.form.images === 'string' 
            ? this.form.images.split(',').map(s => s.trim()).filter(Boolean)
            : (Array.isArray(this.form.images) ? this.form.images : [])
          images.push(imageUrl)
          this.form.images = images.join(',')
        }
        ElMessage.success('图片上传成功')
      } else {
        ElMessage.error(response.errorMsg || '图片上传失败')
      }
    },
    // 上传失败
    handleUploadError() {
      ElMessage.error('图片上传失败')
    },
    // 移除图片
    handleRemove(file) {
      const imageUrl = file.response?.data?.imageUrl || file.response?.data || file.url
      if (!imageUrl) return
      
      // 从图片列表中移除
      if (this.form.images) {
        const images = typeof this.form.images === 'string' 
          ? this.form.images.split(',').map(s => s.trim()).filter(Boolean)
          : (Array.isArray(this.form.images) ? this.form.images : [])
        const updatedImages = images.filter(img => {
          // 处理不同的URL格式
          const imgUrl = img.includes('/') ? img.split('/').pop() : img
          const removeUrl = imageUrl.includes('/') ? imageUrl.split('/').pop() : imageUrl
          return imgUrl !== removeUrl && img !== imageUrl && img !== removeUrl
        })
        this.form.images = updatedImages.length > 0 ? updatedImages.join(',') : ''
      }
    },
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
.edit-form :deep(.custom-select) { width: 100%; }

/* 图片上传区域 */
.upload-section {
  width: 100%;
}

.upload-section :deep(.el-upload) {
  border: 1px dashed var(--primary-color, #6a015e);
  border-radius: 8px;
  cursor: pointer;
  position: relative;
  overflow: hidden;
  transition: all 0.3s ease;
}

.upload-section :deep(.el-upload:hover) {
  border-color: var(--primary-light, #8e2d8e);
}

.upload-section :deep(.el-upload-list) {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 10px;
}

.upload-section :deep(.el-upload-list--picture-card) {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.upload-tip {
  margin-top: 10px;
  font-size: 12px;
  color: #999;
  line-height: 1.5;
}

.upload-tip p {
  margin: 4px 0;
}
</style>

