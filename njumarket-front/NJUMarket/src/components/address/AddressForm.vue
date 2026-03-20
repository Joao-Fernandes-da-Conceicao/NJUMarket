<template>
  <div class="address-form">
    <el-form
      ref="formRef"
      :model="form"
      :rules="rules"
      label-width="100px"
    >
      <el-form-item label="收货人姓名" prop="recipientName">
        <el-input
          v-model="form.recipientName"
          placeholder="请输入收货人姓名"
        />
      </el-form-item>
      
      <el-form-item label="收货人电话" prop="recipientPhone">
        <el-input
          v-model="form.recipientPhone"
          placeholder="请输入收货人电话"
        />
      </el-form-item>
      
      <el-form-item label="省份" prop="province">
        <el-input
          v-model="form.province"
          placeholder="请输入省份"
        />
      </el-form-item>
      
      <el-form-item label="城市" prop="city">
        <el-input
          v-model="form.city"
          placeholder="请输入城市"
        />
      </el-form-item>
      
      <el-form-item label="区/县" prop="district">
        <el-input
          v-model="form.district"
          placeholder="请输入区/县"
        />
      </el-form-item>
      
      <el-form-item label="街道地址" prop="streetAddress">
        <el-input
          v-model="form.streetAddress"
          placeholder="请输入街道地址"
        />
      </el-form-item>
      
      <el-form-item label="详细地址" prop="detailAddress">
        <el-input
          v-model="form.detailAddress"
          type="textarea"
          placeholder="请输入详细地址（楼栋、门牌号等，可选）"
          :rows="2"
        />
      </el-form-item>
      
      <!-- 地图选择器 -->
      <el-form-item label="地图选择" v-if="showMapPicker">
        <el-collapse v-model="activeCollapse" @change="handleCollapseChange">
          <el-collapse-item name="map">
            <template #title>
              <span>在地图上选择位置（自动获取经纬度）</span>
            </template>
            <AddressMapPicker
              ref="mapPickerRef"
              v-model="mapLocation"
              @change="handleMapLocationChange"
            />
          </el-collapse-item>
        </el-collapse>
      </el-form-item>
      
      <el-form-item label="地址标签" prop="addressLabel">
        <el-select
          v-model="form.addressLabel"
          :options="labelOptions"
          placeholder="请选择地址标签"
        />
      </el-form-item>
      
      <el-form-item label="设为默认" prop="isDefault">
        <el-switch v-model="form.isDefault" />
      </el-form-item>
      
      <el-form-item>
        <div class="form-actions">
          <el-button @click="handleCancel">取消</el-button>
          <el-button type="primary" @click="handleSubmit">确定</el-button>
        </div>
      </el-form-item>
    </el-form>
  </div>
</template>

<script>
import { ref, reactive, watch } from 'vue'
import AddressMapPicker from './AddressMapPicker.vue'

export default {
  name: 'AddressForm',
  components: {
    AddressMapPicker
  },
  props: {
    address: {
      type: Object,
      default: null
    },
    showMapPicker: {
      type: Boolean,
      default: true
    }
  },
  emits: ['submit', 'cancel'],
  setup(props, { emit }) {
    const formRef = ref(null)
    const mapPickerRef = ref(null)
    
    const form = reactive({
      recipientName: '',
      recipientPhone: '',
      province: '',
      city: '',
      district: '',
      streetAddress: '',
      detailAddress: '',
      addressLabel: 'HOME',
      isDefault: false,
      longitude: null,
      latitude: null
    })
    
    const activeCollapse = ref([])
    const mapLocation = ref(null)
    
    // 处理折叠面板展开/折叠事件
    const handleCollapseChange = (activeNames) => {
      // 如果地图面板展开（activeNames 包含 'map'）
      if (activeNames.includes('map')) {
        // 延迟一下，确保DOM已更新（折叠面板展开动画需要时间）
        setTimeout(() => {
          if (mapPickerRef.value && mapPickerRef.value.resizeMap) {
            mapPickerRef.value.resizeMap()
          }
        }, 400) // 增加延迟时间，确保折叠动画完成
      }
    }
    
    const labelOptions = [
      { label: '家', value: 'HOME' },
      { label: '学校', value: 'SCHOOL' },
      { label: '公司', value: 'COMPANY' },
      { label: '其他', value: 'OTHER' }
    ]
    
    const rules = {
      recipientName: [
        { required: true, message: '请输入收货人姓名', trigger: 'blur' }
      ],
      recipientPhone: [
        { required: true, message: '请输入收货人电话', trigger: 'blur' },
        { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号码', trigger: 'blur' }
      ],
      province: [
        { required: true, message: '请输入省份', trigger: 'blur' }
      ],
      city: [
        { required: true, message: '请输入城市', trigger: 'blur' }
      ],
      district: [
        { required: true, message: '请输入区/县', trigger: 'blur' }
      ],
      streetAddress: [
        { required: true, message: '请输入街道地址', trigger: 'blur' }
      ]
    }
    
    // 监听props变化，填充表单
    watch(() => props.address, (newAddress) => {
      if (newAddress) {
        form.recipientName = newAddress.recipientName || ''
        form.recipientPhone = newAddress.recipientPhone || ''
        form.province = newAddress.province || ''
        form.city = newAddress.city || ''
        form.district = newAddress.district || ''
        form.streetAddress = newAddress.streetAddress || ''
        form.detailAddress = newAddress.detailAddress || ''
        form.addressLabel = newAddress.addressLabel || 'HOME'
        form.isDefault = newAddress.isDefault || false
        form.longitude = newAddress.longitude || null
        form.latitude = newAddress.latitude || null
        
        // 如果有经纬度，设置地图位置
        if (newAddress.longitude && newAddress.latitude) {
          mapLocation.value = {
            longitude: newAddress.longitude,
            latitude: newAddress.latitude,
            address: newAddress.fullAddress || ''
          }
        }
      } else {
        // 重置表单
        form.recipientName = ''
        form.recipientPhone = ''
        form.province = ''
        form.city = ''
        form.district = ''
        form.streetAddress = ''
        form.detailAddress = ''
        form.addressLabel = 'HOME'
        form.isDefault = false
        form.longitude = null
        form.latitude = null
        mapLocation.value = null
      }
    }, { immediate: true })
    
    // 地图位置变化处理
    const handleMapLocationChange = (location) => {
      if (location) {
        // 自动填充地址信息
        if (location.province) form.province = location.province
        if (location.city) {
          form.city = location.city
          // 如果没有三级行政区，将城市名复制到区/县
          if (!location.district && location.city) {
            form.district = location.city
          } else if (location.district) {
            form.district = location.district
          }
        }
        if (location.streetAddress) form.streetAddress = location.streetAddress
        if (location.detailAddress) form.detailAddress = location.detailAddress
        
        // 设置经纬度
        form.longitude = location.longitude
        form.latitude = location.latitude
      }
    }
    
    // 监听城市字段变化，如果区/县为空，自动用城市填充
    watch(() => form.city, (newCity) => {
      // 只有当区/县为空且城市不为空时，才自动填充
      if (newCity && !form.district) {
        form.district = newCity
      }
    })
    
    // 提交表单
    const handleSubmit = async () => {
      if (!formRef.value) return
      
      await formRef.value.validate((valid) => {
        if (valid) {
          // 如果区/县为空，使用城市名填充（处理没有三级行政区的城市）
          const district = form.district || form.city || ''
          
          // 构建完整地址
          let fullAddress = form.province + form.city + district + form.streetAddress
          if (form.detailAddress) {
            fullAddress += form.detailAddress
          }
          
          const formData = {
            ...form,
            district: district, // 确保 district 字段有值
            fullAddress,
            longitude: form.longitude,
            latitude: form.latitude
          }
          
          emit('submit', formData)
        }
      })
    }
    
    // 取消
    const handleCancel = () => {
      emit('cancel')
    }
    
    return {
      formRef,
      mapPickerRef,
      form,
      rules,
      labelOptions,
      activeCollapse,
      mapLocation,
      handleCollapseChange,
      handleMapLocationChange,
      handleSubmit,
      handleCancel
    }
  }
}
</script>

<style scoped>
.address-form {
  padding: 20px 0;
  max-width: 640px;
  margin: 0 auto;
}

.address-form :deep(.el-form-item__content) {
  margin-left: 0 !important;
}

.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  width: 100%;
}

/* 响应式：移动端表单标签换行显示 */
@media (max-width: 768px) {
  .address-form {
    max-width: 100%;
    padding: 10px 0;
  }

  .address-form :deep(.el-form-item) {
    flex-direction: column;
    align-items: flex-start;
  }

  .address-form :deep(.el-form-item__label) {
    width: 100%;
    text-align: left;
    margin-bottom: 6px;
  }

  .address-form :deep(.el-form-item__content) {
    width: 100%;
  }

  .address-form :deep(.el-input),
  .address-form :deep(.el-textarea),
  .address-form :deep(.el-select),
  .address-form :deep(.el-switch) {
    width: 100%;
  }

  .form-actions {
    flex-direction: column;
  }

  .form-actions :deep(.unified-button) {
    width: 100%;
    margin-left: 0 !important;
  }

  .form-actions :deep(.el-button) {
    width: 100%;
    margin-left: 0 !important;
  }
}
</style>

