<template>
  <div class="create-order-page">
    <div class="create-order-content" v-loading="loading">
      <div class="container">
        <div class="page-header">
          <h1>创建新订单</h1>
          <div class="header-actions">
            <UnifiedButton @click="$router.back()">
              返回
            </UnifiedButton>
          </div>
        </div>

        <div v-if="commoditySnapshot" class="order-form-wrapper">
          <!-- 商品信息展示 -->
          <div class="form-section">
            <h3 class="section-title">商品信息</h3>
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
                <div class="commodity-status-tag">
                  <UnifiedTag
                    v-if="!commodityExists || !commodityOnShelf"
                    type="warning"
                  >
                    {{ statusMessage }}
                  </UnifiedTag>
                  <UnifiedTag
                    v-else-if="currentStock <= 0"
                    type="danger"
                  >
                    已售罄
                  </UnifiedTag>
                  <UnifiedTag
                    v-else
                    type="success"
                  >
                    正常可购买
                  </UnifiedTag>
                </div>
              </div>
            </div>
          </div>

          <!-- 订单信息表单 -->
          <div class="form-section">
            <h3 class="section-title">订单信息</h3>
            <el-form :model="orderForm" :rules="orderRules" ref="orderFormRef" label-position="left" class="order-form">
              <el-form-item label="购买数量" prop="quantity">
                <UnifiedInput
                  v-model="orderForm.quantity"
                  type="number"
                  placeholder="请输入购买数量（件）"
                  :disabled="!commodityExists || !commodityOnShelf || currentStock <= 0"
                  class="pill-input"
                />
                <span v-if="commodityExists && commodityOnShelf" class="stock-info">
                  （当前库存：{{ currentStock }}）
                </span>
                <span v-if="commodityExists && commodityOnShelf && currentStock <= 0" class="sold-out-info">
                  （已售罄）
                </span>
              </el-form-item>
              
              <el-form-item label="收货地址" prop="shippingAddressId">
                <AddressSelector
                  v-model="orderForm.shippingAddressId"
                  label=""
                  prop="shippingAddressId"
                  placeholder="请选择收货地址"
                  @change="handleAddressChange"
                />
                <!-- 保留原有字段用于兼容（隐藏） -->
                <UnifiedInput
                  v-model="orderForm.shippingAddress"
                  placeholder="请输入收货地址（如果未选择地址）"
                  :disabled="!commodityExists || !commodityOnShelf || currentStock <= 0"
                  class="pill-input"
                  style="display: none;"
                />
              </el-form-item>
              
              <el-form-item label="备注" prop="remark">
                <UnifiedInput
                  v-model="orderForm.remark"
                  type="textarea"
                  placeholder="请输入备注信息（可选）"
                  :rows="3"
                  :disabled="!commodityExists || !commodityOnShelf || currentStock <= 0"
                  class="rounded-textarea"
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

          <!-- 提交按钮 -->
          <div class="form-actions">
            <UnifiedButton size="large" @click="$router.back()">取消</UnifiedButton>
            <UnifiedButton
              type="primary"
              size="large"
              :disabled="!canCreateOrder"
              :loading="loading"
              @click="handleCreateOrder"
            >
              {{ !canCreateOrder ? (currentStock <= 0 ? '已售罄' : '无法下单') : '确认下单' }}
            </UnifiedButton>
          </div>
        </div>

        <div v-else class="empty-state">
          <el-empty description="商品信息加载失败">
            <UnifiedButton @click="$router.back()">返回</UnifiedButton>
          </el-empty>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '../stores/user'
import { orderAPI, imageAPI, commodityAPI, profileAPI } from '../api'
import { ElMessage } from 'element-plus'
import { validateQuantity, canCreateOrder as checkCanCreateOrder } from '../utils/orderRules'
import UnifiedButton from '../components/common/UnifiedButton.vue'
import UnifiedTag from '../components/common/UnifiedTag.vue'
import UnifiedInput from '../components/common/UnifiedInput.vue'
import AddressSelector from '../components/address/AddressSelector.vue'

export default {
  name: 'CreateOrder',
  components: {
    UnifiedButton,
    UnifiedTag,
    UnifiedInput,
    AddressSelector
  },
  setup() {
    const route = useRoute()
    const router = useRouter()
    const userStore = useUserStore()
    
    const loading = ref(false)
    const commoditySnapshot = ref(null)
    const commodityExists = ref(false)
    const commodityOnShelf = ref(false)
    const currentStock = ref(0)
    const currentPrice = ref(0)
    const statusMessage = ref('')
    const isFromOrderSnapshot = ref(false) // 仅用于界面提示
    const sourceCommodityId = ref('') // 最终下单使用的商品ID
    
    const orderForm = ref({
      quantity: '1',
      shippingAddressId: '', // 地址ID
      shippingAddress: '校内自提', // 保留用于兼容
      remark: ''
    })
    
    const orderFormRef = ref(null)
    
    const orderRules = {
      quantity: [
        { required: true, message: '请输入购买数量', trigger: 'blur' },
        {
          validator: (rule, value, callback) => {
            const num = Number(value)
            if (isNaN(num) || num < 1) {
              callback(new Error('购买数量必须大于0'))
            } else if (currentStock.value > 0 && num > currentStock.value) {
              callback(new Error(`购买数量不能超过库存 ${currentStock.value}`))
            } else {
              callback()
            }
          },
          trigger: 'blur'
        }
      ],
      shippingAddressId: [
        { required: false, message: '请选择收货地址', trigger: 'change' }
      ],
      shippingAddress: [
        { required: false, message: '请输入收货地址', trigger: 'blur' }
      ]
    }
    
    // 计算属性
    const priceChanged = computed(() => {
      return commoditySnapshot.value && 
             currentPrice.value !== commoditySnapshot.value.price
    })
    
    const canCreateOrder = computed(() => {
      const quantity = Number(orderForm.value.quantity) || 0
      
      // 商品必须存在且上架
      if (!commodityExists.value || !commodityOnShelf.value) {
        return false
      }
      
      // 验证数量
      const quantityCheck = validateQuantity(quantity, currentStock.value)
      return quantityCheck.valid
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
      const quantity = Number(orderForm.value.quantity) || 0
      return (price * quantity).toFixed(2)
    }
    
    // 格式化时间
    const formatTime = (time) => {
      if (!time) return ''
      return new Date(time).toLocaleString()
    }
    
    // 地址选择变化
    const handleAddressChange = (addressId, address) => {
      if (address) {
        // 如果选择了地址，自动填充shippingAddress字段（用于兼容）
        orderForm.value.shippingAddress = address.fullAddress || ''
      }
    }
    
    // 获取商品信息
    const fetchCommodityInfo = async () => {
      loading.value = true
      try {
        const id = route.params.id
        
        // 首先尝试作为订单ID查询商品快照
        const snapshotResponse = await orderAPI.queryOriginalCommodity(id)
        if (snapshotResponse.success && snapshotResponse.data.commoditySnapshot) {
          // 这是从订单快照创建
          const data = snapshotResponse.data
          commoditySnapshot.value = data.commoditySnapshot
          commodityExists.value = data.commodityExists
          commodityOnShelf.value = data.commodityOnShelf
          currentStock.value = data.currentStock
          currentPrice.value = data.currentPrice
          statusMessage.value = data.statusMessage
          isFromOrderSnapshot.value = true // 标记为订单快照
          
          // 设置默认数量
          orderForm.value.quantity = '1'
          // 记录用于下单的商品ID
          sourceCommodityId.value = data.commoditySnapshot.commodityId || ''
          
          // 检查是否可以购买（订单快照模式）
          if (commoditySnapshot.value && commodityExists.value) {
            const commodity = {
              commodityId: data.commoditySnapshot.commodityId || route.params.id,
              commodityStatus: commodityOnShelf.value ? 'ON_SHELF' : 'DRAFT',
              stock: currentStock.value,
              sellerId: data.commoditySnapshot.sellerId
            }
            
            const check = checkCanCreateOrder({
              commodity: commodity,
              user: userStore.user,
              quantity: 1
            })
            
            if (!check.canOrder) {
              alert(check.reason || '无法购买此商品')
              router.back()
              return
            }
          }
        } else {
          // 标记为商品模式
          isFromOrderSnapshot.value = false
          // 尝试作为商品ID查询当前商品
          const commodityResponse = await commodityAPI.getDetail(id)
          if (commodityResponse.success && commodityResponse.data) {
            const commodity = commodityResponse.data
            
            // 通过sellerId查询卖家信息
            let sellerData = null
            if (commodity.sellerId) {
              try {
                const sellerResponse = await profileAPI.getUser(commodity.sellerId)
                if (sellerResponse.success && sellerResponse.data) {
                  sellerData = sellerResponse.data
                }
              } catch (error) {
                console.error('获取卖家信息失败:', error)
              }
            }
            
            // 构造商品快照对象（包含卖家信息）
            commoditySnapshot.value = {
              title: commodity.title,
              price: commodity.price,
              location: commodity.location,
              category: commodity.category,
              conditionLevel: commodity.conditionLevel,
              images: JSON.stringify(commodity.images || []),
              snapshotTime: new Date().toISOString(),
              // 卖家信息
              sellerName: sellerData?.nickname || commodity.seller?.nickname || '未知卖家',
              sellerPhone: sellerData?.phone || commodity.seller?.phone || '',
              sellerEmail: sellerData?.email || commodity.seller?.email || '',
              sellerId: commodity.sellerId
            }
            
            commodityExists.value = true
            commodityOnShelf.value = commodity.commodityStatus === 'ON_SHELF'
            currentStock.value = commodity.stock
            currentPrice.value = commodity.price
            
            // 设置默认数量
            orderForm.value.quantity = '1'
            // 记录用于下单的商品ID
            sourceCommodityId.value = id
          } else {
            ElMessage.error('获取商品信息失败')
            router.back()
            return
          }
        }
        
        // 检查是否可以购买
        if (commoditySnapshot.value && commodityExists.value) {
          const commodity = {
            commodityId: route.params.id,
            commodityStatus: commodityOnShelf.value ? 'ON_SHELF' : 'DRAFT',
            stock: currentStock.value,
            sellerId: commoditySnapshot.value.sellerId
          }
          
          const check = checkCanCreateOrder({
            commodity: commodity,
            user: userStore.user,
            quantity: 1
          })
          
          if (!check.canOrder) {
            alert(check.reason || '无法购买此商品')
            router.back()
            return
          }
        }
      } catch (error) {
        console.error('获取商品信息失败:', error)
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
        
        const id = route.params.id
        
        // 统一：始终按商品下单（快照仅用于自动填充）
        const commodityIdToOrder = sourceCommodityId.value || id
        
        // ✅ 在下单前再次实时检查商品状态，防止商品在下单过程中被下架
        try {
          const commodityCheckResponse = await commodityAPI.getDetail(commodityIdToOrder)
          if (!commodityCheckResponse.success || !commodityCheckResponse.data) {
            ElMessage.error('商品不存在或已下架')
            loading.value = false
            return
          }
          
          const currentCommodity = commodityCheckResponse.data
          if (currentCommodity.commodityStatus !== 'ON_SHELF') {
            ElMessage.error('商品未上架，无法购买')
            loading.value = false
            // 更新页面状态，显示商品已下架
            commodityOnShelf.value = false
            statusMessage.value = '商品已下架'
            return
          }
          
          // 检查库存是否足够
          const quantityToCheck = Number(orderForm.value.quantity) || 0
          if (currentCommodity.stock < quantityToCheck) {
            ElMessage.error(`商品库存不足，当前库存：${currentCommodity.stock}`)
            loading.value = false
            return
          }
        } catch (checkError) {
          console.error('检查商品状态失败:', checkError)
          ElMessage.error('检查商品状态失败，请稍后重试')
          loading.value = false
          return
        }
        
        const quantityFinal = Number(orderForm.value.quantity) || 0
        const response = await orderAPI.create({
          commodityId: commodityIdToOrder,
          quantity: quantityFinal,
          payAmount: parseFloat(calculateTotalAmount()),
          shippingAddressId: orderForm.value.shippingAddressId || undefined, // 地址ID
          shippingAddress: orderForm.value.shippingAddress || '校内自提', // 保留用于兼容
          remark: orderForm.value.remark
        })
        
        if (response.success) {
          ElMessage.success('订单创建成功！')
          router.push('/orders')
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
      handleCreateOrder,
      handleAddressChange
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
  max-width: 900px;
  margin: 0 auto;
  padding: 0 24px;
}

.page-header {
  text-align: center;
  margin-bottom: 30px;
}

.page-header h1 {
  color: var(--primary-color);
}

.header-actions {
  margin-top: 15px;
}

/* 返回按钮药丸型 */
.page-header .el-button {
  border-radius: 20px;
}

.order-form-wrapper {
  background: transparent;
  border-radius: 16px;
  padding: 32px 24px;
  border: none;
  margin: 0 auto;
  max-width: 760px;
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

.order-form {
  width: 100%;
}

/* 表单项整体居中并对齐 */
.order-form :deep(.el-form-item) {
  margin: 0 auto 20px auto;
  width: 100%;
  max-width: 640px;
  display: flex;
  justify-content: center;
  align-items: flex-start;
}

.order-form :deep(.el-form-item__label) {
  text-align: left;
  min-width: 100px;
  flex-shrink: 0;
}

.order-form :deep(.el-form-item__content) {
  flex: 1;
  max-width: 480px;
}

/* 输入框宽度自适应 */
.order-form :deep(.el-input),
.order-form :deep(.el-select),
.order-form :deep(.el-textarea) {
  width: 100%;
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
  border-radius: 20px;
}

/* 移动端收紧布局 */
@media (max-width: 768px) {
  .container {
    padding: 0 12px;
  }

  .order-form-wrapper {
    padding: 20px 12px;
    max-width: 100%;
  }

  .order-form :deep(.el-form-item) {
    flex-direction: column;
    align-items: flex-start;
    max-width: 100%;
  }

  .order-form :deep(.el-form-item__content) {
    max-width: 100%;
  }
}

.commodity-card {
  display: flex;
  gap: 20px;
  padding: 20px;
  background: transparent; /* 透明背景 */
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
  color: var(--primary-color); /* 主题色 */
  margin-bottom: 10px;
  text-align: left; /* 桌面端靠左 */
}

.commodity-price {
  font-size: 20px;
  font-weight: normal;
  color: var(--primary-color); /* 主题色 */
  margin-bottom: 8px;
  text-align: left; /* 桌面端靠左 */
}

.commodity-location,
.commodity-category,
.commodity-condition {
  font-size: 14px;
  color: var(--primary-color); /* 主题色 */
  margin-bottom: 5px;
  text-align: left; /* 桌面端靠左 */
}

.seller-info {
  margin: 5px 0;
  text-align: left; /* 桌面端靠左 */
}

.seller-name {
  font-size: 14px;
  color: #666; /* 恢复原色 */
  font-weight: normal;
  margin: 2px 0;
}

.seller-contact {
  font-size: 12px;
  color: #999; /* 恢复原色 */
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

.empty-state {
  text-align: center;
  padding: 60px 0;
}

/* border-radius 由 UnifiedButton 统一管理（9999px） */

/* 宽屏适配：增加容器宽度与两栏布局 */
@media (min-width: 1200px) {
  .container {
    max-width: 1100px; /* 控制整体宽度 */
  }
  .order-form-wrapper {
    padding: 32px 20px; /* 两栏时适当内边距 */
  }
  .order-form-wrapper {
    display: grid;
    grid-template-columns: 1.1fr 1fr; /* 左信息、右表单 */
    grid-column-gap: 32px;
    grid-row-gap: 0;
    align-items: start;
  }
  /* 第一段form-section为商品信息，第二段为表单 */
  .order-form-wrapper > .form-section:first-of-type {
    grid-column: 1 / 2;
  }
  .order-form-wrapper > .form-section:nth-of-type(2) {
    grid-column: 2 / 3;
  }
  .form-actions {
    grid-column: 1 / 3; /* 按钮跨两列 */
    justify-content: flex-end; /* 宽屏右对齐 */
  }
  .section-title {
    text-align: left; /* 宽屏左对齐标题 */
  }
  .form-section {
    text-align: left; /* 宽屏左对齐内容 */
  }
  .order-form :deep(.el-form-item) {
    justify-content: flex-start; /* 宽屏左对齐表单 */
  }
}

/* 超宽屏进一步放大容器与列间距 */
@media (min-width: 1600px) {
  .container {
    max-width: 1440px;
  }
  .order-form-wrapper {
    grid-column-gap: 48px;
  }
}

/* 状态标签药丸型 */
.commodity-status-tag {
  margin-top: 10px;
  text-align: left; /* 桌面端靠左 */
}

/* border-radius 由 UnifiedTag 统一管理（9999px） */
.commodity-status-tag :deep(.unified-tag) {
  font-size: 12px;
  padding: 4px 12px;
}
</style>
