<template>
  <div class="create-order-page">
    <div class="create-order-content" v-loading="loading">
      <div class="container">
        <div class="page-header">
          <h1>创建新订单</h1>
          <el-button @click="$router.back()">返回</el-button>
        </div>

        <div v-if="commoditySnapshot" class="order-form">
          <!-- 商品信息展示 -->
          <div class="commodity-section">
            <h2>商品信息</h2>
            <div class="commodity-card">
              <div class="commodity-image">
                <img 
                  v-if="getCommoditySnapshotImage()"
                  :src="getCommoditySnapshotImage()"
                  :alt="commoditySnapshot.title"
                />
                <div v-else class="no-image">
                  <span>暂无照片</span>
                </div>
              </div>
              <div class="commodity-details">
                <h3>{{ commoditySnapshot.title }}</h3>
                <p class="commodity-price">¥{{ commoditySnapshot.price }}</p>
                <p class="commodity-location">位置：{{ commoditySnapshot.location }}</p>
                <p class="commodity-category">分类：{{ commoditySnapshot.category }}</p>
                <p class="commodity-condition">成色：{{ commoditySnapshot.conditionLevel }}</p>
                <div class="seller-info">
                  <p class="seller-name">卖家：{{ commoditySnapshot.sellerName }}</p>
                  <p v-if="commoditySnapshot.sellerPhone" class="seller-contact">
                    电话：{{ commoditySnapshot.sellerPhone }}
                  </p>
                  <p v-if="commoditySnapshot.sellerEmail" class="seller-contact">
                    邮箱：{{ commoditySnapshot.sellerEmail }}
                  </p>
                  <p v-if="commoditySnapshot.snapshotTime" class="snapshot-time">
                    快照时间：{{ formatTime(commoditySnapshot.snapshotTime) }}
                  </p>
                </div>
                
                <!-- 状态提示 -->
                <el-alert
                  v-if="!commodityExists || !commodityOnShelf"
                  :title="statusMessage"
                  type="warning"
                  :closable="false"
                  style="margin-top: 10px;"
                />
                <el-alert
                  v-else-if="currentStock <= 0"
                  title="商品已售罄"
                  type="error"
                  :closable="false"
                  style="margin-top: 10px;"
                />
                <el-alert
                  v-else
                  title="商品正常可购买"
                  type="success"
                  :closable="false"
                  style="margin-top: 10px;"
                />
              </div>
            </div>
          </div>

          <!-- 订单信息表单 -->
          <div class="order-section">
            <h2>订单信息</h2>
            <el-form :model="orderForm" :rules="orderRules" ref="orderFormRef" label-width="100px">
              <el-form-item label="购买数量" prop="quantity">
                <el-input-number
                  v-model="orderForm.quantity"
                  :min="1"
                  :max="commodityExists && commodityOnShelf && currentStock > 0 ? currentStock : 1"
                  :disabled="!commodityExists || !commodityOnShelf || currentStock <= 0"
                />
                <span v-if="commodityExists && commodityOnShelf" class="stock-info">
                  （当前库存：{{ currentStock }}）
                </span>
                <span v-if="commodityExists && commodityOnShelf && currentStock <= 0" class="sold-out-info">
                  （已售罄）
                </span>
              </el-form-item>
              
              <el-form-item label="收货地址" prop="shippingAddress">
                <el-input
                  v-model="orderForm.shippingAddress"
                  placeholder="请输入收货地址"
                  :disabled="!commodityExists || !commodityOnShelf || currentStock <= 0"
                />
              </el-form-item>
              
              <el-form-item label="备注" prop="remark">
                <el-input
                  v-model="orderForm.remark"
                  type="textarea"
                  placeholder="请输入备注信息（可选）"
                  :rows="3"
                  :disabled="!commodityExists || !commodityOnShelf || currentStock <= 0"
                />
              </el-form-item>
              
              <el-form-item label="订单总额">
                <div class="total-amount">
                  <span class="amount-label">¥{{ calculateTotalAmount() }}</span>
                  <span v-if="priceChanged" class="price-change-notice">
                    （价格已更新，原价：¥{{ commoditySnapshot.price }}）
                  </span>
                </div>
              </el-form-item>
            </el-form>
          </div>

          <!-- 操作按钮 -->
          <div class="action-section">
            <el-button size="large" @click="$router.back()">取消</el-button>
              <el-button
                type="primary"
                size="large"
                :disabled="!canCreateOrder"
                @click="handleCreateOrder"
              >
                {{ !canCreateOrder ? (currentStock <= 0 ? '已售罄' : '无法下单') : '确认下单' }}
              </el-button>
          </div>
        </div>

        <div v-else class="empty-state">
          <el-empty description="商品信息加载失败">
            <el-button @click="$router.back()">返回</el-button>
          </el-empty>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
// import { useUserStore } from '../stores/user'
import { orderAPI, imageAPI } from '../api'
import { ElMessage } from 'element-plus'

export default {
  name: 'CreateOrder',
  setup() {
    const route = useRoute()
    const router = useRouter()
    // const userStore = useUserStore()
    
    const loading = ref(false)
    const commoditySnapshot = ref(null)
    const commodityExists = ref(false)
    const commodityOnShelf = ref(false)
    const currentStock = ref(0)
    const currentPrice = ref(0)
    const statusMessage = ref('')
    
    const orderForm = ref({
      quantity: 1,
      shippingAddress: '校内自提',
      remark: ''
    })
    
    const orderFormRef = ref(null)
    
    const orderRules = {
      quantity: [
        { required: true, message: '请输入购买数量', trigger: 'blur' },
        { type: 'number', min: 1, message: '购买数量必须大于0', trigger: 'blur' }
      ],
      shippingAddress: [
        { required: true, message: '请输入收货地址', trigger: 'blur' }
      ]
    }
    
    // 计算属性
    const priceChanged = computed(() => {
      return commoditySnapshot.value && 
             currentPrice.value !== commoditySnapshot.value.price
    })
    
    const canCreateOrder = computed(() => {
      return commodityExists.value && commodityOnShelf.value && 
             orderForm.value.quantity > 0 && 
             orderForm.value.quantity <= currentStock.value
    })
    
    // 获取商品快照图片
    const getCommoditySnapshotImage = () => {
      if (!commoditySnapshot.value?.images) return null
      
      try {
        const images = JSON.parse(commoditySnapshot.value.images)
        if (images && images.length > 0) {
          return getCommodityImageUrl(images[0])
        }
      } catch (error) {
        return getCommodityImageUrl(commoditySnapshot.value.images)
      }
      
      return null
    }
    
    // 获取商品图片URL
    const getCommodityImageUrl = (imageUrl) => {
      if (!imageUrl) return imageAPI.getDefaultCommodityImage()
      
      if (imageUrl.startsWith('http')) return imageUrl
      if (imageUrl.includes('/')) return imageUrl
      
      const fileName = imageUrl.split('/').pop()
      return imageAPI.getCommodityImage(fileName)
    }
    
    // 计算总金额
    const calculateTotalAmount = () => {
      if (!commoditySnapshot.value) return 0
      const price = commodityExists.value && commodityOnShelf.value ? currentPrice.value : commoditySnapshot.value.price
      return (price * orderForm.value.quantity).toFixed(2)
    }
    
    // 格式化时间
    const formatTime = (time) => {
      if (!time) return ''
      return new Date(time).toLocaleString()
    }
    
    // 获取商品信息
    const fetchCommodityInfo = async () => {
      loading.value = true
      try {
        const orderId = route.params.orderId
        const response = await orderAPI.queryOriginalCommodity(orderId)
        
        if (response.success) {
          const data = response.data
          commoditySnapshot.value = data.commoditySnapshot
          commodityExists.value = data.commodityExists
          commodityOnShelf.value = data.commodityOnShelf
          currentStock.value = data.currentStock
          currentPrice.value = data.currentPrice
          statusMessage.value = data.statusMessage
          
          // 设置默认数量
          orderForm.value.quantity = Math.min(1, currentStock.value)
        } else {
          ElMessage.error(response.errorMsg || '获取商品信息失败')
          router.back()
        }
      } catch (error) {
        ElMessage.error('获取商品信息失败')
        router.back()
      } finally {
        loading.value = false
      }
    }
    
    // 创建订单
    const handleCreateOrder = async () => {
      try {
        // 验证表单
        await orderFormRef.value.validate()
        
        if (!canCreateOrder.value) {
          ElMessage.error('当前无法创建订单')
          return
        }
        
        const confirmed = confirm(`确认下单？\n商品：${commoditySnapshot.value.title}\n数量：${orderForm.value.quantity}\n总金额：¥${calculateTotalAmount()}`)
        if (!confirmed) {
          return
        }
        
        loading.value = true
        
        const orderId = route.params.orderId
        const orderData = {
          quantity: orderForm.value.quantity,
          shippingAddress: orderForm.value.shippingAddress,
          remark: orderForm.value.remark
        }
        
        const response = await orderAPI.createOrderFromSnapshot(orderId, orderData)
        
        if (response.success) {
          ElMessage.success('订单创建成功！')
          router.push('/my-orders')
        } else {
          ElMessage.error(response.errorMsg || '创建订单失败')
        }
      } catch (error) {
        if (error !== 'cancel') {
          ElMessage.error('创建订单失败')
        }
      } finally {
        loading.value = false
      }
    }
    
    onMounted(() => {
      fetchCommodityInfo()
    })
    
    return {
      loading,
      commoditySnapshot,
      commodityExists,
      commodityOnShelf,
      currentStock,
      currentPrice,
      statusMessage,
      orderForm,
      orderFormRef,
      orderRules,
      priceChanged,
      canCreateOrder,
      getCommoditySnapshotImage,
      calculateTotalAmount,
      formatTime,
      handleCreateOrder
    }
  }
}
</script>

<style scoped>
.create-order-page {
  min-height: 100vh;
  background-color: #f5f5f5;
}

.create-order-content {
  padding: 30px 0;
}

.container {
  max-width: 800px;
  margin: 0 auto;
  padding: 0 20px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 30px;
}

.page-header h1 {
  font-size: 28px;
  font-weight: normal;
  color: #333;
  margin: 0;
}

.order-form {
  background: white;
  border-radius: 8px;
  padding: 30px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.commodity-section,
.order-section {
  margin-bottom: 30px;
}

.commodity-section h2,
.order-section h2 {
  font-size: 20px;
  font-weight: normal;
  color: #333;
  margin-bottom: 20px;
  padding-bottom: 10px;
  border-bottom: 2px solid #e0e0e0;
}

.commodity-card {
  display: flex;
  gap: 20px;
  padding: 20px;
  background: #f8f9fa;
  border-radius: 8px;
}

.commodity-image {
  width: 120px;
  height: 120px;
  border-radius: 8px;
  overflow: hidden;
  background: #f5f5f5;
  display: flex;
  align-items: center;
  justify-content: center;
}

.commodity-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.no-image {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 100%;
  background: #f0f0f0;
  color: #999;
  font-size: 14px;
}

.commodity-details {
  flex: 1;
}

.commodity-details h3 {
  font-size: 18px;
  font-weight: normal;
  color: #333;
  margin-bottom: 10px;
}

.commodity-price {
  font-size: 20px;
  font-weight: normal;
  color: #e74c3c;
  margin-bottom: 8px;
}

.commodity-location,
.commodity-category,
.commodity-condition {
  font-size: 14px;
  color: #666;
  margin-bottom: 5px;
}

.seller-info {
  margin: 5px 0;
}

.seller-name {
  font-size: 14px;
  color: #333;
  font-weight: normal;
  margin: 2px 0;
}

.seller-contact {
  font-size: 12px;
  color: #666;
  margin: 1px 0;
}

.snapshot-time {
  font-size: 11px;
  color: #999;
  margin: 1px 0;
  font-style: italic;
}

.stock-info {
  font-size: 12px;
  color: #999;
  margin-left: 10px;
}

.sold-out-info {
  font-size: 12px;
  color: #e74c3c;
  margin-left: 10px;
  font-weight: normal;
}

.total-amount {
  display: flex;
  align-items: center;
  gap: 10px;
}

.amount-label {
  font-size: 24px;
  font-weight: normal;
  color: #e74c3c;
}

.price-change-notice {
  font-size: 12px;
  color: #f56c6c;
}

.action-section {
  display: flex;
  justify-content: flex-end;
  gap: 15px;
  padding-top: 20px;
  border-top: 1px solid #e0e0e0;
}

.empty-state {
  text-align: center;
  padding: 60px 0;
}

@media (max-width: 768px) {
  .commodity-card {
    flex-direction: column;
    text-align: center;
  }
  
  .commodity-image {
    width: 100%;
    height: 200px;
  }
  
  .action-section {
    flex-direction: column;
  }
}
</style>
