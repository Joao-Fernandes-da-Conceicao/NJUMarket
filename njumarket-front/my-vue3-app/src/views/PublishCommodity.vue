<template>
  <div class="publish-page">
    <!-- 发布商品内容 -->
    <div class="publish-content">
      <div class="container">
        <div class="page-header">
          <h1>{{ isEdit ? '编辑商品' : '发布商品' }}</h1>
          <div class="header-actions">
            <el-button @click="$router.push('/my-commodities')">
              返回商品管理
            </el-button>
          </div>
        </div>

        <div class="publish-form-wrapper">
          <el-form
            ref="publishFormRef"
            :model="publishForm"
            :rules="publishRules"
            label-width="100px"
            class="publish-form"
          >
            <!-- 基本信息 -->
            <div class="form-section">
              <h3 class="section-title">基本信息</h3>
              
              <el-form-item label="商品标题" prop="title">
                <el-input
                  v-model="publishForm.title"
                  placeholder="请输入商品标题"
                  maxlength="50"
                  show-word-limit
                />
              </el-form-item>

              <el-form-item label="商品描述" prop="description">
                <el-input
                  v-model="publishForm.description"
                  type="textarea"
                  placeholder="请详细描述商品信息"
                  :rows="4"
                  maxlength="500"
                  show-word-limit
                />
              </el-form-item>

              <el-form-item label="商品分类" prop="category">
                <el-select v-model="publishForm.category" placeholder="请选择分类">
                  <el-option label="电子产品" value="电子产品" />
                  <el-option label="服装配饰" value="服装配饰" />
                  <el-option label="图书文具" value="图书文具" />
                  <el-option label="生活用品" value="生活用品" />
                  <el-option label="运动户外" value="运动户外" />
                  <el-option label="美妆护肤" value="美妆护肤" />
                  <el-option label="其他" value="其他" />
                </el-select>
              </el-form-item>

              <el-form-item label="成色等级" prop="conditionLevel">
                <el-select v-model="publishForm.conditionLevel" placeholder="请选择成色">
                  <el-option label="全新" value="全新" />
                  <el-option label="九成新" value="九成新" />
                  <el-option label="八成新" value="八成新" />
                  <el-option label="七成新" value="七成新" />
                  <el-option label="六成新" value="六成新" />
                  <el-option label="五成新" value="五成新" />
                </el-select>
              </el-form-item>
            </div>

            <!-- 价格库存 -->
            <div class="form-section">
              <h3 class="section-title">价格库存</h3>
              
              <el-form-item label="商品价格" prop="price">
                <el-input-number
                  v-model="publishForm.price"
                  :min="0"
                  :precision="2"
                  placeholder="请输入价格"
                  style="width: 200px"
                />
                <span class="unit">元</span>
              </el-form-item>

              <el-form-item label="库存数量" prop="stock">
                <el-input-number
                  v-model="publishForm.stock"
                  :min="1"
                  placeholder="请输入库存"
                  style="width: 200px"
                />
                <span class="unit">件</span>
              </el-form-item>

              <el-form-item label="所在位置" prop="location">
                <el-input
                  v-model="publishForm.location"
                  placeholder="请输入所在位置"
                />
              </el-form-item>
            </div>

            <!-- 商品图片 -->
            <div class="form-section">
              <h3 class="section-title">商品图片</h3>
              
              <el-form-item label="上传图片" prop="images">
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
              <el-button size="large" @click="handleCancel">
                取消
              </el-button>
              <el-button
                v-if="!isEdit"
                type="warning"
                size="large"
                :loading="publishLoading"
                @click="handlePublishAsDraft"
              >
                保存草稿
              </el-button>
              <el-button
                type="primary"
                size="large"
                :loading="publishLoading"
                @click="handlePublish"
              >
                {{ isEdit ? '更新商品' : '发布商品' }}
              </el-button>
              <el-button
                v-if="!isEdit"
                type="success"
                size="large"
                :loading="publishLoading"
                @click="handlePublishAndActivate"
              >
                发布并上架
              </el-button>
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

export default {
  name: 'PublishCommodity',
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
      location: '',
      images: []
    })
    
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
        { type: 'number', min: 0.01, message: '价格必须大于 0', trigger: 'blur' }
      ],
      stock: [
        { required: true, message: '请输入库存数量', trigger: 'blur' },
        { type: 'number', min: 1, message: '库存必须大于 0', trigger: 'blur' }
      ],
      location: [
        { required: true, message: '请输入所在位置', trigger: 'blur' }
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
          router.push('/my-commodities')
        }
      } catch (error) {
        console.error('获取商品详情失败:', error) // 调试信息
        ElMessage.error('获取商品详情失败')
        router.push('/my-commodities')
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
            let response
            if (isEdit.value) {
              response = await commodityAPI.update(editCommodityId.value, publishForm)
            } else {
              response = await commodityAPI.publish(publishForm)
            }
            
            if (response.success) {
              ElMessage.success(isEdit.value ? '商品更新成功' : '商品发布成功')
              router.push('/my-commodities')
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
    const handlePublishAsDraft = async () => {
      if (!publishFormRef.value) return
      
      try {
        await publishFormRef.value.validate()
        publishLoading.value = true
        
        const publishForm = {
          title: publishForm.title,
          description: publishForm.description,
          category: publishForm.category,
          conditionLevel: publishForm.conditionLevel,
          price: publishForm.price,
          stock: publishForm.stock,
          location: publishForm.location,
          images: publishForm.images,
          status: 'DRAFT' // 设置为草稿状态
        }
        
        const response = await commodityAPI.publish(publishForm)
        if (response.success) {
          ElMessage.success('草稿已保存')
          router.push('/my-commodities')
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
        publishLoading.value = true
        
        const publishForm = {
          title: publishForm.title,
          description: publishForm.description,
          category: publishForm.category,
          conditionLevel: publishForm.conditionLevel,
          price: publishForm.price,
          stock: publishForm.stock,
          location: publishForm.location,
          images: publishForm.images,
          status: 'ACTIVE' // 直接设置为在售状态
        }
        
        const response = await commodityAPI.publish(publishForm)
        if (response.success) {
          ElMessage.success('商品已发布并上架')
          router.push('/my-commodities')
        } else {
          ElMessage.error(response.errorMsg || '发布并上架失败')
        }
      } catch (error) {
        ElMessage.error('发布并上架失败')
      } finally {
        publishLoading.value = false
      }
    }
    
    // 取消
    const handleCancel = async () => {
      const confirmed = confirm('是否保存为草稿？')
      if (confirmed) {
        // 用户选择保存草稿
        try {
          await handlePublishAsDraft()
        } catch (error) {
          // 错误已经在handlePublishAsDraft中处理
        }
      } else {
        // 用户选择不保存，直接返回
        router.push('/my-commodities')
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
      handlePublishAsDraft,
      handlePublishAndActivate,
      handleCancel,
      handleLogout
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
  padding: 30px 0;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 30px;
}

.page-header h1 {
  font-size: 28px;
  font-weight: bold;
  color: #333;
  margin: 0;
}

.publish-form-wrapper {
  background: white;
  border-radius: 8px;
  padding: 40px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.form-section {
  margin-bottom: 40px;
}

.section-title {
  font-size: 20px;
  font-weight: 600;
  color: #333;
  margin-bottom: 20px;
  padding-bottom: 10px;
  border-bottom: 2px solid var(--primary-color);
}

.publish-form {
  max-width: 800px;
}

.unit {
  margin-left: 10px;
  color: #666;
  font-size: 14px;
}

.upload-section {
  width: 100%;
}

.upload-tip {
  margin-top: 10px;
  color: #999;
  font-size: 12px;
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
}

@media (max-width: 768px) {
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
    flex-direction: column;
    align-items: center;
  }
}
</style>
