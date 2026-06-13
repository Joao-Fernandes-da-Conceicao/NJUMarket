<template>
  <div class="publish-page">
    <!-- 发布商品内容 -->
    <div class="publish-content">
      <div class="container">
        <div class="page-header">
          <h1>{{ isEdit ? '编辑商品' : '发布商品' }}</h1>
          <div class="header-actions">
            <UnifiedButton @click="$router.push('/home')">
              返回商品管理
            </UnifiedButton>
          </div>
        </div>

        <div class="publish-form-wrapper">
          <el-form
            ref="publishFormRef"
            :model="publishForm"
            :rules="publishRules"
            label-position="right"
            class="publish-form"
          >
            <!-- 基本信息 -->
            <div class="form-section">
              <h3 class="section-title">基本信息</h3>
              
              <el-form-item label="商品标题" prop="title">
                <UnifiedInput
                  v-model="publishForm.title"
                  placeholder="请输入商品标题"
                  maxlength="50"
                  show-word-limit
                />
              </el-form-item>

              <el-form-item label="商品描述" prop="description">
                <UnifiedInput
                  v-model="publishForm.description"
                  type="textarea"
                  placeholder="请详细描述商品信息"
                  :rows="8"
                  maxlength="500"
                  show-word-limit
                  class="rounded-textarea"
                />
              </el-form-item>

              <el-form-item label="商品分类" prop="category">
                <UnifiedSelect
                  v-model="publishForm.category"
                  :options="categoryOptions"
                  :placeholder="'请选择分类'"
                />
              </el-form-item>

              <el-form-item label="成色等级" prop="conditionLevel">
                <UnifiedSelect
                  v-model="publishForm.conditionLevel"
                  :options="conditionOptions"
                  :placeholder="'请选择成色'"
                />
              </el-form-item>
            </div>

            <!-- 价格库存和商品图片（合并为一个section） -->
            <div class="form-section form-section-right">
              <h3 class="section-title">价格库存</h3>
              
              <el-form-item label="商品价格" prop="price">
                <UnifiedInput
                  v-model="publishForm.price"
                  placeholder="请输入商品价格"
                  type="number"
                  min="0"
                  step="0.01"
                  class="pill-input"
                />
              </el-form-item>

              <el-form-item label="库存数量" prop="stock">
                <UnifiedInput
                  v-model="publishForm.stock"
                  placeholder="请输入库存数量"
                  type="number"
                  min="1"
                  step="1"
                  class="pill-input"
                />
              </el-form-item>

              <el-form-item label="所在位置" prop="addressId">
                <AddressSelector
                  v-model="publishForm.addressId"
                  label=""
                  prop="addressId"
                  placeholder="请选择商品所在位置"
                  :show-contact="false"
                  @change="handleAddressChange"
                />
                <!-- 保留原有字段用于兼容（隐藏） -->
                <UnifiedInput
                  v-model="publishForm.location"
                  placeholder="请输入所在位置（如果未选择地址）"
                  class="pill-input"
                  style="display: none;"
                />
              </el-form-item>

              <h3 class="section-title section-title-margin-top">商品图片</h3>
              
              <el-form-item prop="images">
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
            </div>

            <!-- 提交按钮 -->
            <div class="form-actions">
              <UnifiedButton size="large" @click="handleCancel">
                取消
              </UnifiedButton>
              <UnifiedButton
                v-if="!isEdit"
                type="warning"
                size="large"
                :loading="publishLoading"
                @click="handleSaveAsDraft"
              >
                保存草稿
              </UnifiedButton>
              <UnifiedButton
                type="primary"
                size="large"
                :loading="publishLoading"
                @click="handlePublish"
              >
                {{ isEdit ? '更新商品' : '发布商品' }}
              </UnifiedButton>
              <UnifiedButton
                v-if="!isEdit"
                type="success"
                size="large"
                :loading="publishLoading"
                @click="handlePublishAndActivate"
              >
                发布并上架
              </UnifiedButton>
            </div>
          </el-form>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { ref, reactive, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '../stores/user'
import { commodityAPI } from '../api'
import { ElMessage } from 'element-plus'
import UnifiedButton from '../components/common/UnifiedButton.vue'
import UnifiedSelect from '../components/common/UnifiedSelect.vue'
import UnifiedInput from '../components/common/UnifiedInput.vue'
import AddressSelector from '../components/address/AddressSelector.vue'

export default {
  name: 'PublishCommodity',
  components: {
    UnifiedButton,
    UnifiedSelect,
    UnifiedInput,
    AddressSelector
  },
  setup() {
    const route = useRoute()
    const router = useRouter()
    const userStore = useUserStore()
    
    const publishFormRef = ref()
    const publishLoading = ref(false)
    const fileList = ref([])
    const isEdit = ref(false)
    const editCommodityId = ref('')
    
    const publishForm = reactive({
      title: '',
      description: '',
      category: '',
      conditionLevel: '',
      price: null,
      stock: 1,
      addressId: '', // 地址ID
      location: '', // 保留用于兼容
      images: []
    })
    
    // 统一选择器选项
    const categoryOptions = [
      { label: '电子产品', value: '电子产品' },
      { label: '服装配饰', value: '服装配饰' },
      { label: '图书文具', value: '图书文具' },
      { label: '生活用品', value: '生活用品' },
      { label: '运动户外', value: '运动户外' },
      { label: '美妆护肤', value: '美妆护肤' },
      { label: '其他', value: '其他' }
    ]

    const conditionOptions = [
      { label: '全新', value: '全新' },
      { label: '九成新', value: '九成新' },
      { label: '八成新', value: '八成新' },
      { label: '七成新', value: '七成新' },
      { label: '六成新', value: '六成新' },
      { label: '五成新', value: '五成新' }
    ]

    const publishRules = {
      title: [
        { required: true, message: '请输入商品标题', trigger: 'blur' },
        { min: 2, max: 50, message: '标题长度在 2 到 50 个字符', trigger: 'blur' }
      ],
      description: [
        { required: true, message: '请输入商品描述', trigger: 'blur' },
        { min: 10, max: 500, message: '描述长度在 10 到 500 个字符', trigger: 'blur' }
      ],
      category: [
        { required: true, message: '请选择商品分类', trigger: 'change' }
      ],
      conditionLevel: [
        { required: true, message: '请选择成色等级', trigger: 'change' }
      ],
      price: [
        { required: true, message: '请输入商品价格', trigger: 'blur' },
        { 
          validator: (rule, value, callback) => {
            const num = parseFloat(value)
            if (isNaN(num) || num <= 0) {
              callback(new Error('价格必须大于 0'))
            } else {
              callback()
            }
          }, 
          trigger: 'blur' 
        }
      ],
      stock: [
        { required: true, message: '请输入库存数量', trigger: 'blur' },
        { 
          validator: (rule, value, callback) => {
            const num = parseInt(value)
            if (isNaN(num) || num < 1) {
              callback(new Error('库存必须大于 0'))
            } else {
              callback()
            }
          }, 
          trigger: 'blur' 
        }
      ],
      addressId: [
        { required: false, message: '请选择所在位置', trigger: 'change' }
      ],
      location: [
        { required: false, message: '请输入所在位置', trigger: 'blur' }
      ]
    }
    
    const user = computed(() => userStore.user)
    
    // 上传配置
    const uploadUrl = ref('http://localhost:8080/api/user/commodity/upload-image')
    const uploadHeaders = computed(() => ({
      'Authorization': `Bearer ${userStore.token}`
    }))
    
    // 上传前检查
    const beforeUpload = (file) => {
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
    }
    
    // 上传成功
    const handleUploadSuccess = (response) => {
      if (response.success) {
        // 后端返回的是ImageUploadDTO，包含imageUrl字段
        const imageUrl = response.data.imageUrl || response.data
        publishForm.images.push(imageUrl)
        ElMessage.success('图片上传成功')
      } else {
        ElMessage.error(response.errorMsg || '图片上传失败')
      }
    }
    
    // 上传失败
    const handleUploadError = () => {
      ElMessage.error('图片上传失败')
    }
    
    // 移除图片
    const handleRemove = (file) => {
      const imageUrl = file.response?.data?.imageUrl || file.response?.data
      const index = publishForm.images.findIndex(img => img === imageUrl)
      if (index > -1) {
        publishForm.images.splice(index, 1)
      }
    }
    
    // 获取商品详情（编辑模式）
    const fetchCommodityDetail = async () => {
      if (!isEdit.value) return
      
      try {
        console.log('获取商品详情，商品ID:', editCommodityId.value) // 调试信息
        const response = await commodityAPI.getMyDetail(editCommodityId.value)
        console.log('商品详情响应:', response) // 调试信息
        
        if (response.success) {
          const commodity = response.data
          console.log('商品数据:', commodity) // 调试信息
          
          // 填充表单数据
          publishForm.title = commodity.title || ''
          publishForm.description = commodity.description || ''
          publishForm.category = commodity.category || ''
          publishForm.conditionLevel = commodity.conditionLevel || ''
          publishForm.price = commodity.price || 0
          publishForm.stock = commodity.stock || 1
          publishForm.location = commodity.location || ''
          publishForm.addressId = commodity.addressId || ''
          
          // 处理图片数据
          if (commodity.images && commodity.images.length > 0) {
            publishForm.images = commodity.images
            // 设置文件列表
            fileList.value = commodity.images.map((img, index) => ({
              name: `image-${index}`,
              url: img,
              response: { data: img }
            }))
          } else {
            publishForm.images = []
            fileList.value = []
          }
          
          console.log('表单数据已填充:', publishForm) // 调试信息
        } else {
          ElMessage.error(response.errorMsg || '获取商品详情失败')
          router.push('/home')
        }
      } catch (error) {
        console.error('获取商品详情失败:', error) // 调试信息
        ElMessage.error('获取商品详情失败')
        router.push('/home')
      }
    }
    
    // 发布商品
    const handlePublish = async () => {
      if (!publishFormRef.value) return
      
      await publishFormRef.value.validate(async (valid) => {
        if (valid) {
          if (publishForm.images.length === 0) {
            ElMessage.warning('请至少上传一张商品图片')
            return
          }
          
          publishLoading.value = true
          try {
            // 转换价格和库存为数字
            const formData = {
              ...publishForm,
              price: parseFloat(publishForm.price),
              stock: parseInt(publishForm.stock)
            }
            
            let response
            if (isEdit.value) {
              response = await commodityAPI.update(editCommodityId.value, formData)
            } else {
              response = await commodityAPI.publish(formData)
            }
            
            if (response.success) {
              ElMessage.success(isEdit.value ? '商品更新成功' : '商品发布成功')
              router.push('/home')
            } else {
              ElMessage.error(response.errorMsg || '操作失败')
            }
          } catch (error) {
            ElMessage.error('操作失败')
          } finally {
            publishLoading.value = false
          }
        }
      })
    }
    
    // 保存草稿
    const handleSaveAsDraft = async () => {
      if (!publishFormRef.value) return
      
      try {
        await publishFormRef.value.validate()
        publishLoading.value = true
        
        const draftData = {
          title: publishForm.title,
          description: publishForm.description,
          category: publishForm.category,
          conditionLevel: publishForm.conditionLevel,
          price: parseFloat(publishForm.price),
          stock: parseInt(publishForm.stock),
          location: publishForm.location,
          images: publishForm.images
        }
        
        const response = await commodityAPI.createDraft(draftData)
        if (response.success) {
          ElMessage.success('草稿已保存')
          router.push('/home')
        } else {
          ElMessage.error(response.errorMsg || '保存草稿失败')
        }
      } catch (error) {
        ElMessage.error('保存草稿失败')
      } finally {
        publishLoading.value = false
      }
    }
    
    // 发布并上架
    const handlePublishAndActivate = async () => {
      if (!publishFormRef.value) return
      
      try {
        await publishFormRef.value.validate()
        if (publishForm.images.length === 0) {
          ElMessage.warning('请至少上传一张商品图片')
          return
        }
        
        publishLoading.value = true
        
        // 先发布商品
        const publishData = {
          title: publishForm.title,
          description: publishForm.description,
          category: publishForm.category,
          conditionLevel: publishForm.conditionLevel,
          price: parseFloat(publishForm.price),
          stock: parseInt(publishForm.stock),
          location: publishForm.location,
          images: publishForm.images
        }
        
        const publishResponse = await commodityAPI.publish(publishData)
        if (publishResponse.success) {
          // 发布成功后立即上架
          const commodityId = publishResponse.data.commodityId
          const shelfResponse = await commodityAPI.shelf(commodityId)
          if (shelfResponse.success) {
            ElMessage.success('商品已发布并上架')
            router.push('/home')
          } else {
            ElMessage.warning('商品已发布，但上架失败：' + shelfResponse.errorMsg)
            router.push('/home')
          }
        } else {
          ElMessage.error(publishResponse.errorMsg || '发布失败')
        }
      } catch (error) {
        ElMessage.error('发布并上架失败')
      } finally {
        publishLoading.value = false
      }
    }
    
    // 地址选择变化
    const handleAddressChange = (addressId, address) => {
      if (address) {
        // 如果选择了地址，自动填充location字段（用于兼容）
        publishForm.location = address.fullAddress || ''
      }
    }
    
    // 取消
    const handleCancel = async () => {
      // 编辑模式下直接返回，不询问是否保存草稿
      if (isEdit.value) {
        router.push('/home')
        return
      }
      
      // 发布模式下询问是否保存草稿
      const confirmed = confirm('是否保存为草稿？')
      if (confirmed) {
        // 用户选择保存草稿
        try {
          await handleSaveAsDraft()
        } catch (error) {
          // 错误已经在handleSaveAsDraft中处理
        }
      } else {
        // 用户选择不保存，直接返回
        router.push('/home')
      }
    }
    
    // 登出
    const handleLogout = async () => {
      try {
        await userStore.logout()
        // userStore.logout()会处理跳转，不需要额外的跳转和消息
      } catch (error) {
        ElMessage.error('退出登录失败')
      }
    }
    
    onMounted(() => {
      // 检查是否为编辑模式
      if (route.query.edit) {
        isEdit.value = true
        editCommodityId.value = route.query.edit
        fetchCommodityDetail()
      }
    })
    
    return {
      publishFormRef,
      publishForm,
      publishRules,
      publishLoading,
      fileList,
      isEdit,
      user,
      uploadUrl,
      uploadHeaders,
      beforeUpload,
      handleUploadSuccess,
      handleUploadError,
      handleRemove,
      handlePublish,
      handleSaveAsDraft,
      handlePublishAndActivate,
      handleCancel,
      handleLogout,
      handleAddressChange,
      categoryOptions,
      conditionOptions
    }
  }
}
</script>

<style scoped>
.publish-page {
  min-height: 100vh;
  background-color: #f5f5f5;
}

.publish-content {
  padding: 40px 0; /* 增加间距以匹配主页设计 */
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 30px;
}

/* 返回按钮药丸型 */
.page-header .el-button {
  border-radius: 20px;
}

.publish-form-wrapper {
  background: transparent;
  border-radius: 16px; /* 使用主页的圆角设计 */
  padding: 40px 60px; /* 增加左右内边距 */
  border: none; /* 移除边框 */
  margin: 0 auto; /* 居中 */
}

.form-section {
  margin-bottom: 40px;
  text-align: center;
}

.section-title {
  font-size: 24px;
  font-weight: normal;
  color: var(--primary-color);
  margin-bottom: 20px;
  text-align: center;
}

.publish-form {
  width: 100%; /* 填满父容器 */
}

/* 表单项整体居中 */
.publish-form :deep(.el-form-item) {
  margin: 0 auto 20px auto; /* 添加底部间距 */
  width: 100%; /* 铺满父容器 */
}

/* 输入框宽度自适应 */
.publish-form :deep(.el-input),
.publish-form :deep(.el-select),
.publish-form :deep(.el-textarea) {
  width: 100%;
}

.unit {
  margin-left: 10px;
  color: #666;
  font-size: 14px;
}

.upload-section {
  width: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.upload-section :deep(.el-upload) {
  margin: 0 auto;
}

.upload-section :deep(.el-upload-list) {
  display: flex;
  justify-content: center;
  flex-wrap: wrap;
  margin: 0 auto;
}

.upload-section :deep(.el-upload-list--picture-card) {
  width: 100%;
  display: flex;
  justify-content: center;
  flex-wrap: wrap;
}

.upload-tip {
  margin-top: 10px;
  color: #999;
  font-size: 12px;
  text-align: center;
}

.upload-tip p {
  margin: 5px 0;
}

.form-actions {
  display: flex;
  justify-content: center;
  gap: 20px;
  margin-top: 40px;
  padding-top: 30px;
  border-top: 1px solid #e0e0e0;
}

.form-actions .el-button {
  min-width: 120px;
  margin-left: 0 !important;
  border-radius: 20px; /* 药丸型按钮 */
}

/* 药丸型输入框样式 */
.pill-input :deep(.el-input__wrapper) {
  border-radius: 20px;
  border: 1px solid var(--primary-color);
  background-color: white;
  box-shadow: 0 2px 8px rgba(106, 1, 94, 0.1);
  transition: all 0.3s ease;
}


.pill-input :deep(.el-input__wrapper:hover) {
  border-color: var(--primary-light);
  box-shadow: 0 4px 12px rgba(106, 1, 94, 0.15);
}

.pill-input :deep(.el-input__wrapper.is-focus) {
  border-color: var(--primary-color);
  box-shadow: 0 4px 12px rgba(106, 1, 94, 0.2);
}

.pill-input :deep(.el-input__inner) {
  border-radius: 20px;
  padding-left: 5px;
}


/* 药丸型选择器样式 */
.pill-select :deep(.el-select__wrapper) {
  border-radius: 20px;
  border: 1px solid var(--primary-color);
  background-color: white;
  box-shadow: 0 2px 8px rgba(106, 1, 94, 0.1);
  transition: all 0.3s ease;
}

.pill-select :deep(.el-select__wrapper:hover) {
  border-color: var(--primary-light);
  box-shadow: 0 4px 12px rgba(106, 1, 94, 0.15);
}

.pill-select :deep(.el-select__wrapper.is-focus) {
  border-color: var(--primary-color);
  box-shadow: 0 4px 12px rgba(106, 1, 94, 0.2);
}

/* 选择器文字左边距 */
.pill-select :deep(.el-select__placeholder),
.pill-select :deep(.el-select__selected-item) {
  padding-left: 5px;
}

/* 圆角矩形多行输入框样式 */
.rounded-textarea :deep(.el-textarea__inner) {
  border-radius: 16px;
  border: 1px solid var(--primary-color);
  background-color: white;
  box-shadow: 0 2px 8px rgba(106, 1, 94, 0.1);
  transition: all 0.3s ease;
  padding: 12px 12px 12px 17px;
}

.rounded-textarea :deep(.el-textarea__inner:hover) {
  border-color: var(--primary-light);
  box-shadow: 0 4px 12px rgba(106, 1, 94, 0.15);
}

.rounded-textarea :deep(.el-textarea__inner:focus) {
  border-color: var(--primary-color);
  box-shadow: 0 4px 12px rgba(106, 1, 94, 0.2);
}

/* 仅本页：选择器文字左对齐 */
:deep(.custom-page-size-select .select-value) {
  justify-content: flex-start;
  text-align: left;
}

/* 仅本页：选择器尺寸与其它输入框保持一致（宽度填满，固定高度） */
:deep(.custom-page-size-select) {
  width: 100%;
  min-width: 0;
  height: 40px; /* 与常用输入框高度对齐 */
  padding-top: 0;
  padding-bottom: 0;
  display: flex;
  align-items: center;
}

/* 选择器内部文字行高适配高度 */
:deep(.custom-page-size-select .select-value) {
  line-height: 40px;
}

/* 仅本页：选择器弹窗的选项文字左对齐 */
:deep(.select-popup .popup-option) {
  text-align: left;
}

@media (min-width: 1200px) {
  /* 宽屏容器更宽 */
  .container {
    max-width: 1280px;
  }
  /* 宽屏表单两栏布局：左（基本信息），右（价格库存+商品图片） */
  .publish-form {
    display: grid;
    grid-template-columns: 1.1fr 1fr;
    grid-template-rows: auto auto; /* 两行：第一行两个section，第二行按钮 */
    grid-column-gap: 32px;
    grid-row-gap: 0;
    align-items: start;
  }
  /* 基本信息在左列 */
  .publish-form > .form-section:first-of-type {
    grid-column: 1 / 2;
    grid-row: 1 / 2;
  }
  /* 价格库存和商品图片合并后的section在右列 */
  .publish-form > .form-section-right {
    grid-column: 2 / 3;
    grid-row: 1 / 2;
  }
  /* 操作按钮跨两列且靠右 */
  .publish-form > .form-actions {
    grid-column: 1 / 3;
    grid-row: 2 / 3;
    justify-content: flex-end;
  }
  
  /* 右栏section中的商品图片标题间距 */
  .section-title-margin-top {
    margin-top: 40px;
  }
  /* 宽屏下标题与内容左对齐 */
  .form-section {
    text-align: left;
  }
  .section-title {
    text-align: left;
  }
  /* 宽屏下表单项左对齐 */
  .publish-form :deep(.el-form-item) {
    max-width: none;
    margin-left: 0;
    margin-right: 0;
  }
}

@media (min-width: 1600px) {
  .container {
    max-width: 1440px;
  }
  .publish-form {
    grid-column-gap: 48px;
  }
}

@media (max-width: 900px) {
  .nav-content {
    flex-direction: column;
    gap: 15px;
  }
  
  .nav-menu {
    gap: 20px;
  }
  
  .page-header {
    flex-direction: column;
    gap: 15px;
    text-align: center;
  }
  
  .publish-form-wrapper {
    padding: 20px;
  }
  
  .form-actions {
    flex-wrap: wrap;
    align-items: center;
    justify-content: center;
  }
  
  .form-actions .el-button {
    width: 130px;
    flex-shrink: 0;
  }
}
</style>
