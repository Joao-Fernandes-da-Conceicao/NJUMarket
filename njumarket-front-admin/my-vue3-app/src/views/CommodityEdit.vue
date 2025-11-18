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
      <el-divider content-position="left">地址信息</el-divider>
      <el-form-item label="地址ID">
        <UnifiedInput 
          v-model="form.addressId" 
          placeholder="可选：关联用户地址ID（仅作为引用，不影响下方地址快照）" 
        />
        <div style="font-size: 12px; color: #999; margin-top: 4px;">
          注意：地址ID只是数据来源引用，下方的地址快照字段是独立的，可以独立编辑
        </div>
      </el-form-item>
      <el-form-item label="省 / 市 / 区">
        <div class="region-row">
          <UnifiedInput
            v-model="form.addressSnapshotProvince"
            placeholder="省份"
            @input="handleAddressPartChange"
          />
          <UnifiedInput
            v-model="form.addressSnapshotCity"
            placeholder="城市"
            @input="handleAddressPartChange"
          />
          <UnifiedInput
            v-model="form.addressSnapshotDistrict"
            placeholder="区/县"
            @input="handleAddressPartChange"
          />
        </div>
      </el-form-item>
      <el-form-item label="街道">
        <UnifiedInput
          v-model="form.addressSnapshotStreet"
          placeholder="街道/镇"
          @input="handleAddressPartChange"
        />
      </el-form-item>
      <el-form-item label="详细地址">
        <UnifiedInput
          v-model="form.addressSnapshotDetail"
          placeholder="楼栋、门牌等"
          @input="handleAddressPartChange"
        />
      </el-form-item>
      <el-form-item label="地址快照">
        <el-input
          v-model="form.addressSnapshotFull"
          type="textarea"
          rows="3"
          placeholder="默认根据上方字段拼接，可手动调整"
          @input="handleFullAddressInput"
        />
      </el-form-item>
      <el-form-item label="经纬度">
        <div class="coord-row">
          <UnifiedInput
            v-model="form.longitude"
            placeholder="经度"
            @blur="handleManualCoordinateBlur"
          />
          <UnifiedInput
            v-model="form.latitude"
            placeholder="纬度"
            @blur="handleManualCoordinateBlur"
          />
        </div>
      </el-form-item>
      <el-form-item label="地图标注">
        <AddressMapPicker
          v-model="mapLocation"
          :default-location="mapDefaultLocation"
          @change="handleMapLocationChange"
        />
      </el-form-item>
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
import AddressMapPicker from '../components/address/AddressMapPicker.vue'
import { commoditiesAPI } from '../api/admin/commodities'
import { reviewCommodityPayload } from '../utils/commodityReview'
import { ElMessage } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'

export default {
  name: 'CommodityEdit',
  components:{ UnifiedInput, UnifiedButton, UnifiedSelect, AddressMapPicker, Plus },
  data(){
    return {
      form: {
        commodityId: '', sellerId: '', title: '', description: '', price: '', stock: '',
        location: '', addressId: '', addressSnapshotProvince: '', addressSnapshotCity: '',
        addressSnapshotDistrict: '', addressSnapshotStreet: '', addressSnapshotDetail: '',
        addressSnapshotFull: '', longitude: '', latitude: '',
        category: '', conditionLevel: '', commodityStatus: '',
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
      uploadHeaders: {},
      mapLocation: null,
      mapDefaultLocation: { longitude: 118.959, latitude: 32.114 },
      addressFullManuallyEdited: false,
      addressSyncReady: false
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
        addressId: c.addressId || '',
        addressSnapshotProvince: c.addressSnapshotProvince || '',
        addressSnapshotCity: c.addressSnapshotCity || '',
        addressSnapshotDistrict: c.addressSnapshotDistrict || '',
        addressSnapshotStreet: c.addressSnapshotStreet || '',
        addressSnapshotDetail: c.addressSnapshotDetail || '',
        addressSnapshotFull: c.addressSnapshotFull || '',
        longitude: c.longitude ?? '',
        latitude: c.latitude ?? '',
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

      this.addressFullManuallyEdited = false
      this.updateFullAddress(true)

      const lon = typeof c.longitude === 'number' ? c.longitude : Number(c.longitude)
      const lat = typeof c.latitude === 'number' ? c.latitude : Number(c.latitude)
      if (Number.isFinite(lon) && Number.isFinite(lat)) {
        this.mapDefaultLocation = { longitude: lon, latitude: lat }
        this.mapLocation = {
          longitude: lon,
          latitude: lat,
          address: this.form.addressSnapshotFull || this.form.location || ''
        }
      } else {
        this.mapLocation = null
      }
      
      // 处理图片列表
      this.initImageList()
    }
    this.addressSyncReady = true
  },
  watch: {
    'form.addressSnapshotProvince': function () { this.handleAddressPartChange() },
    'form.addressSnapshotCity': function () { this.handleAddressPartChange() },
    'form.addressSnapshotDistrict': function () { this.handleAddressPartChange() },
    'form.addressSnapshotStreet': function () { this.handleAddressPartChange() },
    'form.addressSnapshotDetail': function () { this.handleAddressPartChange() }
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
    handleAddressPartChange() {
      this.updateFullAddress()
    },
    handleFullAddressInput() {
      this.addressFullManuallyEdited = true
    },
    updateFullAddress(force = false) {
      if (!force && this.addressFullManuallyEdited) return
      const parts = [
        this.form.addressSnapshotProvince,
        this.form.addressSnapshotCity,
        this.form.addressSnapshotDistrict,
        this.form.addressSnapshotStreet,
        this.form.addressSnapshotDetail
      ].map(item => (item || '').trim()).filter(Boolean)
      const full = parts.join('')
      this.form.addressSnapshotFull = full
      if (!this.form.location && full) {
        this.form.location = full
      }
    },
    handleMapLocationChange(location) {
      if (!this.addressSyncReady || !location) return
      this.mapLocation = { ...location }
      this.form.longitude = location.longitude
      this.form.latitude = location.latitude
      if (location.province) this.form.addressSnapshotProvince = location.province
      if (location.city) this.form.addressSnapshotCity = location.city
      if (location.district) this.form.addressSnapshotDistrict = location.district
      if (location.streetAddress) this.form.addressSnapshotStreet = location.streetAddress
      if (location.detailAddress) this.form.addressSnapshotDetail = location.detailAddress
      this.addressFullManuallyEdited = false
      if (location.address) {
        this.form.addressSnapshotFull = location.address
      } else {
        this.updateFullAddress(true)
      }
      if (!this.form.location && this.form.addressSnapshotFull) {
        this.form.location = this.form.addressSnapshotFull
      }
    },
    handleManualCoordinateBlur() {
      if (!this.addressSyncReady) return
      const lon = Number(this.form.longitude)
      const lat = Number(this.form.latitude)
      if (!Number.isNaN(lon) && !Number.isNaN(lat)) {
        this.mapLocation = {
          ...(this.mapLocation || {}),
          longitude: lon,
          latitude: lat,
          address: this.form.addressSnapshotFull || (this.mapLocation && this.mapLocation.address) || ''
        }
      }
    },
    async save(){
      const id = this.form.commodityId
      // 构建payload，确保数据类型正确
      // 注意：addressId 只是引用字段，地址快照字段（addressSnapshot*）是独立的，不依赖 addressId
      const payload = {
        commodityId: this.form.commodityId, // ✅ 添加商品ID，验证函数需要
        title: this.form.title || '',
        description: this.form.description || '',
        price: this.form.price !== '' ? Number(this.form.price) : null,
        stock: this.form.stock !== '' ? Number(this.form.stock) : null,
        location: this.form.location || '',
        addressId: this.form.addressId || null, // 仅作为引用，不影响快照字段
        addressSnapshotProvince: this.form.addressSnapshotProvince || null, // 独立字段
        addressSnapshotCity: this.form.addressSnapshotCity || null,
        addressSnapshotDistrict: this.form.addressSnapshotDistrict || null,
        addressSnapshotStreet: this.form.addressSnapshotStreet || null,
        addressSnapshotDetail: this.form.addressSnapshotDetail || null,
        addressSnapshotFull: this.form.addressSnapshotFull || null,
        longitude: this.form.longitude !== '' ? Number(this.form.longitude) : null,
        latitude: this.form.latitude !== '' ? Number(this.form.latitude) : null,
        category: this.form.category || '',
        conditionLevel: this.form.conditionLevel || '',
        commodityStatus: this.form.commodityStatus || '',
        sellerVisibility: this.form.sellerVisibility || '',
        buyerVisibility: this.form.buyerVisibility || '',
        clickCount: this.form.clickCount !== '' ? Number(this.form.clickCount) : null,
        images: this.form.images || ''
      }
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

.region-row {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(0, 1fr));
  gap: 10px;
  width: 100%;
}

.coord-row {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
  width: 100%;
}
</style>

