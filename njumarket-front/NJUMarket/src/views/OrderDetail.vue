<template>
  <div class="order-detail-container">
    <div v-if="loading" class="loading-container">
      <el-skeleton :rows="10" animated />
    </div>
    
    <div v-else-if="order" class="order-detail-wrapper">
      <!-- 页面头部 -->
      <div class="page-header">
        <h1>订单详情</h1>
        <div class="page-header-actions">
          <el-button 
            @click="$router.back()" 
            icon="ArrowLeft"
            class="back-button"
            :round="true"
          >
            返回
          </el-button>
          <el-tag 
            :type="getStatusType(order.orderStatus)" 
            size="large"
            class="status-tag"
          >
            {{ getStatusText(order.orderStatus) }}
          </el-tag>
        </div>
      </div>

      <!-- 主要内容区域（宽屏分栏） -->
      <div class="order-detail">
        <!-- 左栏：订单信息和商品信息 -->
        <div class="order-left-column">
          <!-- 订单基本信息 -->
          <div class="order-info-section">
        <h2>订单信息</h2>
        <div class="info-grid">
          <div class="info-item">
            <label>订单号：</label>
            <span>{{ order.orderId }}</span>
          </div>
          <div class="info-item">
            <label>创建时间：</label>
            <span>{{ formatTime(order.createTime) }}</span>
          </div>
          <div class="info-item">
            <label>支付时间：</label>
            <span>{{ getPayTimeDisplay(order) }}</span>
          </div>
          <div class="info-item">
            <label>发货时间：</label>
            <span>{{ order.shippingTime ? formatTime(order.shippingTime) : '未发货' }}</span>
          </div>
          <div class="info-item">
            <label>签收时间：</label>
            <span>{{ order.deliveryTime ? formatTime(order.deliveryTime) : '未签收' }}</span>
          </div>
          <div class="info-item">
            <label>订单总额：</label>
            <span class="total-amount">¥{{ order.payAmount }}</span>
          </div>
        </div>
      </div>

      <!-- 商品信息 -->
      <div class="commodity-section">
        <h2>商品信息</h2>
        <div class="commodity-card">
          <div class="commodity-image">
            <img 
              v-if="getCommodityImage(order)"
              :src="getCommodityImage(order)"
              :alt="getCommodityTitle(order)"
            />
            <div v-else class="no-image">
              <span>暂无照片</span>
            </div>
          </div>
          <div class="commodity-details">
            <h3 class="commodity-title">
              {{ getCommodityTitle(order) }}
              <el-tag 
                v-if="isCommoditySnapshotOffShelf(order)" 
                type="warning" 
                size="small"
                style="margin-left: 8px;"
              >
                已下架
              </el-tag>
            </h3>
            <p class="commodity-price">单价：¥{{ getCommodityPrice(order) }}</p>
            <p class="commodity-quantity">数量：{{ order.quantity }}</p>
            <!-- 商品位置：使用地址快照字段，按省市区-详细地址格式显示 -->
            <div v-if="formatCommodityAddress(order)" class="commodity-location">
              <label>位置：</label>
              <!-- 如果有地址快照字段，使用标准格式（省市区-详细地址） -->
              <div v-if="!shouldUseSingleLineCommodityAddress(order)" class="address-display">
                <div class="address-region">
                  {{ formatCommodityAddressRegion(order) }}
                </div>
                <div v-if="formatCommodityAddressDetail(order)" class="address-detail">
                  {{ formatCommodityAddressDetail(order) }}
                </div>
              </div>
              <!-- 如果是旧数据（只有commoditySnapshotLocation），使用单行显示（废物利用） -->
              <span v-else class="address-single-line">{{ formatCommodityAddressRegion(order) }}</span>
            </div>
            <p v-if="getCommodityCategory(order)" class="commodity-category">
              分类：{{ getCommodityCategory(order) }}
            </p>
            <p v-if="getCommodityCondition(order)" class="commodity-condition">
              成色：{{ getCommodityCondition(order) }}
            </p>
            <div v-if="getCommodityDescription(order)" class="commodity-description">
              <label>商品描述：</label>
              <p>{{ getCommodityDescription(order) }}</p>
            </div>
          </div>
        </div>
      </div>
        </div>

        <!-- 右栏：其他信息 -->
        <div class="order-right-column">
          <!-- 卖家信息（仅买家可见） -->
          <div v-if="isBuyer" class="seller-section">
        <h2>卖家信息</h2>
        <div class="seller-card">
          <div class="seller-avatar">
            <img 
              v-if="getSellerAvatar(order) && !isSellerDeleted(order)"
              :src="getAvatarUrl(getSellerAvatar(order))"
              :alt="getSellerName(order)"
            />
            <div v-else class="default-avatar">
              <span>{{ isSellerDeleted(order) ? '已' : getSellerName(order).charAt(0) }}</span>
            </div>
          </div>
          <div class="seller-details">
            <h3>{{ getSellerName(order) }}</h3>
            <p v-if="getSellerPhone(order)" class="seller-contact">
              电话：{{ getSellerPhone(order) }}
            </p>
            <p v-if="getSellerEmail(order)" class="seller-contact">
              邮箱：{{ getSellerEmail(order) }}
            </p>
            <div v-if="isSellerDeleted(order)" class="deleted-notice">
              <el-tag type="danger" size="small">用户已注销</el-tag>
            </div>
            <div v-else class="seller-actions">
              <el-button @click="contactSeller" type="primary" size="small">
                联系卖家
              </el-button>
              <el-button @click="viewSellerProfile" size="small">
                查看资料
              </el-button>
            </div>
          </div>
        </div>
      </div>

      <!-- 买家信息（仅卖家可见） -->
      <div v-if="isSeller" class="buyer-section">
        <h2>买家信息</h2>
        <div class="buyer-card">
          <div class="buyer-avatar">
            <img 
              v-if="getBuyerAvatar(order) && !isBuyerDeleted(order)"
              :src="getAvatarUrl(getBuyerAvatar(order))"
              :alt="getBuyerName(order)"
            />
            <div v-else class="default-avatar">
              <span>{{ isBuyerDeleted(order) ? '已' : getBuyerName(order).charAt(0) }}</span>
            </div>
          </div>
          <div class="buyer-details">
            <h3>{{ getBuyerName(order) }}</h3>
            <p v-if="getBuyerPhone(order)" class="buyer-contact">
              电话：{{ getBuyerPhone(order) }}
            </p>
            <p v-if="getBuyerEmail(order)" class="buyer-contact">
              邮箱：{{ getBuyerEmail(order) }}
            </p>
            <div v-if="isBuyerDeleted(order)" class="deleted-notice">
              <el-tag type="danger" size="small">用户已注销</el-tag>
            </div>
            <div v-else class="buyer-actions">
              <el-button @click="contactBuyer" type="primary" size="small">
                联系买家
              </el-button>
              <el-button @click="viewBuyerProfile" size="small">
                查看资料
              </el-button>
            </div>
          </div>
        </div>
      </div>

      <!-- 收货信息 -->
      <div class="shipping-section">
        <div class="section-header">
        <h2>收货信息</h2>
          <!-- 修改地址按钮：只有买家或卖家本人，且订单状态为 CREATED 或 PAID 时显示 -->
          <el-button 
            v-if="canModifyAddress"
            type="primary" 
            size="small"
            @click="showEditAddressDialog = true"
          >
            修改地址
          </el-button>
        </div>
        <div class="shipping-info">
          <!-- 收货人信息（如果有快照） -->
          <div v-if="getShippingRecipientName(order)" class="info-item">
            <label>收货人：</label>
            <span>{{ getShippingRecipientName(order) }}</span>
          </div>
          <div v-if="getShippingRecipientPhone(order)" class="info-item">
            <label>联系电话：</label>
            <span>{{ getShippingRecipientPhone(order) }}</span>
          </div>
          
          <!-- 使用地址快照字段，按省市区-详细地址格式显示 -->
          <div v-if="formatShippingAddress(order)" class="info-item">
            <label>收货地址：</label>
            <!-- 如果有地址快照字段，使用标准格式（省市区-详细地址） -->
            <div v-if="!shouldUseSingleLineShippingAddress(order)" class="address-display">
              <div class="address-region">
                {{ formatShippingAddressRegion(order) }}
          </div>
              <div v-if="formatShippingAddressDetail(order)" class="address-detail">
                {{ formatShippingAddressDetail(order) }}
              </div>
            </div>
            <!-- 如果是旧数据（只有shippingAddress），使用单行显示（废物利用） -->
            <span v-else class="address-single-line">{{ formatShippingAddressRegion(order) }}</span>
          </div>
          <div v-else class="info-item">
            <label>收货地址：</label>
            <span>校内自提</span>
          </div>
          
          <div v-if="order.trackingNumber" class="info-item">
            <label>物流单号：</label>
            <span>{{ order.trackingNumber }}</span>
          </div>
          <div v-if="order.remark" class="info-item">
            <label>备注：</label>
            <span>{{ order.remark }}</span>
          </div>
        </div>
      </div>
      
      <!-- 修改地址自定义弹窗 -->
      <transition name="address-manager-fade">
        <div 
          v-if="showEditAddressDialog" 
          class="address-manager-modal"
          role="dialog"
          aria-modal="true"
        >
          <div class="address-manager-modal__overlay" @click="showEditAddressDialog = false"></div>
          <div class="address-manager-modal__panel">
            <div class="address-manager-modal__header">
              <h3>{{ isBuyer ? '修改收货地址' : '修改发货地址' }}</h3>
              <el-icon class="modal-close" @click="showEditAddressDialog = false">
                <Close />
              </el-icon>
            </div>
            <div class="address-manager-modal__body">
              <AddressSelector
                v-model="selectedAddressId"
                label="选择地址"
                prop="addressId"
                placeholder="请选择地址"
                @change="handleAddressChange"
              />
            </div>
            <div class="address-manager-modal__footer">
              <el-button @click="showEditAddressDialog = false">取消</el-button>
              <el-button type="primary" @click="handleUpdateAddress" :loading="updatingAddress">
                确认修改
              </el-button>
            </div>
          </div>
        </div>
      </transition>

      <!-- 退货进度信息 -->
      <div v-if="hasReturnInfo(order)" class="return-section">
        <h2>退货进度</h2>
        <div class="return-info">
          <div v-if="order.returnReason" class="info-item">
            <label>退货原因：</label>
            <span>{{ order.returnReason }}</span>
          </div>
          <div v-if="order.returnRequestTime" class="info-item">
            <label>申请时间：</label>
            <span>{{ formatTime(order.returnRequestTime) }}</span>
            <span v-if="hasMultipleReturnRequests(order)" class="return-history-hint">
              （当前申请）
            </span>
          </div>
          <div v-if="shouldShowApprovalTime(order)" class="info-item">
            <label>审批通过时间：</label>
            <span>{{ formatTime(order.returnApprovalTime) }}</span>
          </div>
          <div v-if="order.returnTrackingNumber" class="info-item">
            <label>退货物流单号：</label>
            <span>{{ order.returnTrackingNumber }}</span>
          </div>
          <div v-if="order.returnCompletionTime" class="info-item">
            <label>完成时间：</label>
            <span>{{ formatTime(order.returnCompletionTime) }}</span>
          </div>
        </div>
      </div>

      <!-- 退货拒绝信息 -->
      <div v-if="hasReturnRejectionInfo(order)" class="return-rejection-section">
        <h2>{{ getRejectionSectionTitle(order) }}</h2>
        <div class="return-rejection-info">
          <div v-if="order.returnRejectionReason" class="info-item">
            <label>拒绝原因：</label>
            <span>{{ order.returnRejectionReason }}</span>
          </div>
          <div v-if="isRejectionTime(order)" class="info-item">
            <label>拒绝时间：</label>
            <span>{{ formatTime(order.returnApprovalTime) }}</span>
          </div>
          <div v-if="order.returnRejectionReason && order.orderStatus === 'REFUND_APPROVED' && !isRejectionTime(order)" class="info-item">
            <label>历史记录：</label>
            <span class="return-history-hint">
              此前曾被拒绝，现已重新申请并通过
            </span>
          </div>
        </div>
      </div>

          <!-- 操作按钮 -->
          <div class="action-section">
        <div class="action-buttons">
          <!-- 买家操作 -->
          <template v-if="isBuyer">
            <el-button
              v-if="payCheck.canPay"
              type="primary"
              @click="handlePay"
            >
              立即支付
            </el-button>
            <el-button
              v-if="confirmCheck.canConfirm"
              type="success"
              @click="handleConfirm"
            >
              确认收货
            </el-button>
            <el-button
              v-if="cancelCheck.canCancel"
              @click="handleCancel"
            >
              取消订单
            </el-button>
            <el-button
              v-if="order.orderStatus === 'COMPLETED'"
              @click="handleRate"
            >
              评价订单
            </el-button>
            <el-button
              v-if="order.orderStatus === 'COMPLETED'"
              @click="handleReorder"
            >
              再来一单
            </el-button>
          </template>

          <!-- 卖家操作 -->
          <template v-if="isSeller">
            <el-button
              v-if="shipCheck.canShip"
              type="primary"
              @click="handleShip"
            >
              立即发货
            </el-button>
            <el-button
              v-if="order.orderStatus === 'REFUND_REQUESTED'"
              type="warning"
              @click="handleRefund"
            >
              处理退款
            </el-button>
          </template>
        </div>
      </div>
        </div>
      </div>
    </div>

    <div v-else class="empty-state">
      <el-empty description="订单不存在或已删除">
        <el-button @click="$router.back()">返回</el-button>
      </el-empty>
    </div>
  </div>
</template>

<script>
import { ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '../stores/user'
import { orderAPI, imageAPI, contactAPI } from '../api'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Close } from '@element-plus/icons-vue'
import { canPayOrder, canConfirmOrder, canCancelOrder, canShipOrder } from '../utils/orderRules'
import AddressSelector from '../components/address/AddressSelector.vue'

export default {
  name: 'OrderDetail',
  components: {
    Close,
    AddressSelector
  },
  setup() {
    const route = useRoute()
    const router = useRouter()
    const userStore = useUserStore()
    
    const loading = ref(false)
    const order = ref(null)
    
    const isLoggedIn = computed(() => userStore.isLoggedIn)
    const user = computed(() => userStore.user)
    const isBuyer = computed(() => order.value && user.value && order.value.buyerId === user.value.userId)
    const isSeller = computed(() => order.value && user.value && order.value.sellerId === user.value.userId)
    
    // 检查是否可以修改地址：只有买家或卖家本人，且订单状态为 CREATED 或 PAID
    const canModifyAddress = computed(() => {
      if (!order.value || !user.value) return false
      if (!isBuyer.value && !isSeller.value) return false
      const status = order.value.orderStatus
      return status === 'CREATED' || status === 'PAID'
    })
    
    // 地址修改相关状态
    const showEditAddressDialog = ref(false)
    const selectedAddressId = ref('')
    const selectedAddress = ref(null)
    const updatingAddress = ref(false)
    
    // 统一规则检查（基于当前订单和用户）
    const payCheck = computed(() => {
      if (!order.value) return { canPay: false, reason: '' }
      return canPayOrder({ order: order.value, user: user.value })
    })
    const confirmCheck = computed(() => {
      if (!order.value) return { canConfirm: false, reason: '' }
      return canConfirmOrder({ order: order.value, user: user.value })
    })
    const cancelCheck = computed(() => {
      if (!order.value) return { canCancel: false, reason: '' }
      return canCancelOrder({ order: order.value, user: user.value })
    })
    const shipCheck = computed(() => {
      if (!order.value) return { canShip: false, reason: '' }
      return canShipOrder({ order: order.value, user: user.value })
    })
    
    // 获取订单详情
    const fetchOrderDetail = async () => {
      loading.value = true
      try {
        const orderId = route.params.id
        const response = await orderAPI.getDetail(orderId)
        
        if (response.success) {
          order.value = response.data
        } else {
          ElMessage.error(response.errorMsg || '获取订单详情失败')
          router.back()
        }
      } catch (error) {
        ElMessage.error('获取订单详情失败')
        router.back()
      } finally {
        loading.value = false
      }
    }
    
    // 格式化时间
    const formatTime = (time) => {
      if (!time) return ''
      return new Date(time).toLocaleString()
    }
    
    // 获取支付时间
    const getPayTime = (order) => {
      return order.payTime || null
    }
    
    // 获取支付时间显示文本
    const getPayTimeDisplay = (order) => {
      // 如果订单状态是 CREATED，说明未支付
      if (order.orderStatus === 'CREATED') {
        return '未支付'
      }
      // 如果有支付时间，显示时间
      const payTime = getPayTime(order)
      if (payTime) {
        return formatTime(payTime)
      }
      // 如果订单状态是 PAID 或更高，说明已支付，但没有记录具体支付时间
      const paidStatuses = ['PAID', 'SHIPPED', 'COMPLETED', 'REFUND_REQUESTED', 'REFUND_APPROVED', 'REFUND_REJECTED', 'RETURN_REQUESTED', 'RETURN_APPROVED', 'RETURN_REJECTED', 'RETURN_COMPLETED']
      if (paidStatuses.includes(order.orderStatus)) {
        return '已支付（时间未记录）'
      }
      return '未支付'
    }
    
    // 获取状态类型
    const getStatusType = (status) => {
      const statusMap = {
        'CREATED': 'info',
        'PAID': 'warning',
        'SHIPPED': 'primary',
        'COMPLETED': 'success',
        'CANCELLED': 'danger',
        'REFUND_REQUESTED': 'warning',
        'REFUND_APPROVED': 'success',
        'REFUND_REJECTED': 'danger'
      }
      return statusMap[status] || 'info'
    }
    
    // 获取状态文本
    const getStatusText = (status) => {
      const statusMap = {
        'CREATED': '待支付',
        'PAID': '已支付',
        'SHIPPED': '已发货',
        'COMPLETED': '已完成',
        'CANCELLED': '已取消',
        'REFUND_REQUESTED': '退款申请中',
        'REFUND_APPROVED': '退款已批准',
        'REFUND_REJECTED': '退款被拒绝'
      }
      return statusMap[status] || status
    }
    
    // 商品相关方法
    const getCommodityImage = (order) => {
      if (order.commoditySnapshotImages) {
        try {
          const images = JSON.parse(order.commoditySnapshotImages)
          if (images && images.length > 0) {
            return getCommodityImageUrl(images[0])
          }
        } catch (error) {
          return getCommodityImageUrl(order.commoditySnapshotImages)
        }
      }
      if (order.commodity?.images && order.commodity.images.length > 0) {
        return getCommodityImageUrl(order.commodity.images[0])
      }
      return null
    }
    
    const getCommodityImageUrl = (imageUrl) => {
      if (!imageUrl) return imageAPI.getDefaultCommodityImage()
      if (imageUrl.startsWith('http')) return imageUrl
      if (imageUrl.includes('/')) return imageUrl
      const fileName = imageUrl.split('/').pop()
      return imageAPI.getCommodityImage(fileName)
    }
    
    const getCommodityTitle = (order) => {
      return order.commoditySnapshotTitle || order.commodity?.title || '商品标题'
    }
    
    const getCommodityPrice = (order) => {
      return order.commoditySnapshotPrice || order.commodity?.price || 0
    }
    
    // 获取商品位置（兼容方法，保留用于其他可能的使用场景）
    const getCommodityLocation = (order) => {
      // 优先使用商品地址快照的完整地址
      if (order.commoditySnapshotAddressFull) {
        return order.commoditySnapshotAddressFull
      }
      // 兼容：使用商品快照的location字段
      if (order.commoditySnapshotLocation) {
        return order.commoditySnapshotLocation
      }
      // 兼容：如果商品快照没有location，尝试从商品实体获取
      return order.commodity?.addressSnapshotFull || order.commodity?.location
    }
    
    // 格式化商品地址（判断是否有地址数据）
    const formatCommodityAddress = (order) => {
      // 如果有省市区字段，使用标准格式
      if (order.commoditySnapshotAddressProvince || 
          order.commoditySnapshotAddressCity || 
          order.commoditySnapshotAddressDistrict) {
        return true
      }
      // 如果有完整地址快照，也返回true
      if (order.commoditySnapshotAddressFull) {
        return true
      }
      // 如果有旧字段 commoditySnapshotLocation，也返回true（废物利用）
      if (order.commoditySnapshotLocation) {
        return true
      }
      return false
    }
    
    // 格式化商品地址的省市区部分
    const formatCommodityAddressRegion = (order) => {
      // 优先使用地址快照的省市区字段（结构化数据）
      const parts = []
      if (order.commoditySnapshotAddressProvince) {
        parts.push(order.commoditySnapshotAddressProvince)
      }
      if (order.commoditySnapshotAddressCity) {
        parts.push(order.commoditySnapshotAddressCity)
      }
      if (order.commoditySnapshotAddressDistrict) {
        parts.push(order.commoditySnapshotAddressDistrict)
      }
      
      if (parts.length > 0) {
        return parts.join('')
      }
      
      // 如果没有省市区字段，尝试从完整地址快照中提取
      if (order.commoditySnapshotAddressFull) {
        return order.commoditySnapshotAddressFull
      }
      
      // 兼容旧字段：将 commoditySnapshotLocation 作为完整地址显示（废物利用）
      return order.commoditySnapshotLocation || ''
    }
    
    // 格式化商品地址的详细地址部分
    const formatCommodityAddressDetail = (order) => {
      // 如果有地址快照的详细地址字段，使用结构化数据
      const parts = []
      if (order.commoditySnapshotAddressStreet) {
        parts.push(order.commoditySnapshotAddressStreet)
      }
      if (order.commoditySnapshotAddressDetail) {
        parts.push(order.commoditySnapshotAddressDetail)
      }
      
      if (parts.length > 0) {
        return parts.join('')
      }
      
      // 如果没有详细地址字段，但有完整地址快照，说明是旧数据格式
      // 这种情况下，详细地址部分为空，完整地址已经在省市区部分显示了
      return ''
    }
    
    // 检查是否应该使用单行显示（旧数据格式）
    const shouldUseSingleLineCommodityAddress = (order) => {
      // 如果有地址快照的省市区字段，使用标准格式（省市区-详细地址）
      if (order.commoditySnapshotAddressProvince || 
          order.commoditySnapshotAddressCity || 
          order.commoditySnapshotAddressDistrict) {
        return false
      }
      // 如果没有地址快照字段，但有 commoditySnapshotLocation，使用单行显示（废物利用）
      if (order.commoditySnapshotLocation && !order.commoditySnapshotAddressFull) {
        return true
      }
      return false
    }
    
    // 获取收货地址完整信息（优先使用地址快照）
    const getShippingAddressFull = (order) => {
      return order.shippingAddressSnapshotFull
    }
    
    // 获取收货人姓名
    const getShippingRecipientName = (order) => {
      return order.shippingAddressSnapshotRecipientName
    }
    
    // 获取收货人电话
    const getShippingRecipientPhone = (order) => {
      return order.shippingAddressSnapshotRecipientPhone
    }
    
    // 格式化收货地址（省市区-详细地址）
    const formatShippingAddress = (order) => {
      // 如果有省市区字段，使用标准格式
      if (order.shippingAddressSnapshotProvince || 
          order.shippingAddressSnapshotCity || 
          order.shippingAddressSnapshotDistrict) {
        return true
      }
      // 如果有完整地址快照，也返回true
      if (order.shippingAddressSnapshotFull) {
        return true
      }
      // 如果有旧字段 shippingAddress，也返回true（废物利用）
      if (order.shippingAddress) {
        return true
      }
      return false
    }
    
    // 格式化收货地址的省市区部分
    const formatShippingAddressRegion = (order) => {
      // 优先使用地址快照的省市区字段（结构化数据）
      const parts = []
      if (order.shippingAddressSnapshotProvince) {
        parts.push(order.shippingAddressSnapshotProvince)
      }
      if (order.shippingAddressSnapshotCity) {
        parts.push(order.shippingAddressSnapshotCity)
      }
      if (order.shippingAddressSnapshotDistrict) {
        parts.push(order.shippingAddressSnapshotDistrict)
      }
      
      if (parts.length > 0) {
        return parts.join('')
      }
      
      // 如果没有省市区字段，尝试从完整地址快照中提取
      if (order.shippingAddressSnapshotFull) {
        return order.shippingAddressSnapshotFull
      }
      
      // 兼容旧字段：将 shippingAddress 作为完整地址显示（废物利用）
      // 注意：旧数据的 shippingAddress 可能是自由文本，格式不统一，所以作为完整地址单行显示
      return order.shippingAddress || ''
    }
    
    // 格式化收货地址的详细地址部分
    const formatShippingAddressDetail = (order) => {
      // 如果有地址快照的详细地址字段，使用结构化数据
      const parts = []
      if (order.shippingAddressSnapshotStreet) {
        parts.push(order.shippingAddressSnapshotStreet)
      }
      if (order.shippingAddressSnapshotDetail) {
        parts.push(order.shippingAddressSnapshotDetail)
      }
      
      if (parts.length > 0) {
        return parts.join('')
      }
      
      // 如果没有详细地址字段，但有完整地址快照，说明是旧数据格式
      // 这种情况下，详细地址部分为空，完整地址已经在省市区部分显示了
      return ''
    }
    
    // 检查是否应该使用单行显示（旧数据格式）
    const shouldUseSingleLineShippingAddress = (order) => {
      // 如果有地址快照的省市区字段，使用标准格式（省市区-详细地址）
      if (order.shippingAddressSnapshotProvince || 
          order.shippingAddressSnapshotCity || 
          order.shippingAddressSnapshotDistrict) {
        return false
      }
      // 如果没有地址快照字段，但有 shippingAddress，使用单行显示（废物利用）
      if (order.shippingAddress && !order.shippingAddressSnapshotFull) {
        return true
      }
      return false
    }
    
    const getCommodityCategory = (order) => {
      return order.commoditySnapshotCategory || order.commodity?.category
    }
    
    const getCommodityCondition = (order) => {
      return order.commoditySnapshotConditionLevel || order.commodity?.conditionLevel
    }
    
    const getCommodityDescription = (order) => {
      return order.commoditySnapshotDescription || order.commodity?.description
    }
    
    const isCommoditySnapshotOffShelf = (order) => {
      return order.commoditySnapshotStatus && order.commoditySnapshotStatus !== 'ON_SHELF'
    }
    
    // 卖家相关方法
    const getSellerName = (order) => {
      if (order.seller?.isDeleted) {
        return '卖家已注销'
      }
      return order.seller?.nickname || order.seller?.username || order.commoditySnapshotSellerName || '卖家'
    }
    
    const getSellerPhone = (order) => {
      if (order.seller?.isDeleted) {
        return null
      }
      return order.seller?.phone || order.commoditySnapshotSellerPhone
    }
    
    const getSellerEmail = (order) => {
      if (order.seller?.isDeleted) {
        return null
      }
      return order.seller?.email || order.commoditySnapshotSellerEmail
    }
    
    const getSellerAvatar = (order) => {
      if (order.seller?.isDeleted) {
        return null
      }
      return order.seller?.avatar
    }
    
    const isSellerDeleted = (order) => {
      return order.seller?.isDeleted || false
    }
    
    // 买家相关方法
    const getBuyerName = (order) => {
      if (order.buyer?.isDeleted) {
        return '买家已注销'
      }
      return order.buyer?.nickname || order.buyer?.username || '买家'
    }
    
    const getBuyerPhone = (order) => {
      if (order.buyer?.isDeleted) {
        return null
      }
      return order.buyer?.phone
    }
    
    const getBuyerEmail = (order) => {
      if (order.buyer?.isDeleted) {
        return null
      }
      return order.buyer?.email
    }
    
    const getBuyerAvatar = (order) => {
      if (order.buyer?.isDeleted) {
        return null
      }
      return order.buyer?.avatar
    }
    
    const isBuyerDeleted = (order) => {
      return order.buyer?.isDeleted || false
    }
    
    // 头像相关方法
    const getAvatarUrl = (avatar) => {
      if (!avatar) return imageAPI.getDefaultAvatarImage()
      if (avatar.startsWith('http')) return avatar
      if (avatar.includes('/')) return avatar
      const fileName = avatar.split('/').pop()
      return imageAPI.getAvatarImage(fileName)
    }
    
    // 操作处理方法
    const handlePay = async () => {
      if (!payCheck.value.canPay) {
        alert(payCheck.value.reason)
        return
      }
      
      try {
        const response = await orderAPI.pay(order.value.orderId)
        if (response.success) {
          ElMessage.success('支付成功')
          fetchOrderDetail()
        } else {
          ElMessage.error(response.errorMsg || '支付失败')
        }
      } catch (error) {
        ElMessage.error('支付失败')
      }
    }
    
    const handleConfirm = async () => {
      try {
        const response = await orderAPI.confirm(order.value.orderId)
        if (response.success) {
          ElMessage.success('确认收货成功')
          fetchOrderDetail()
        } else {
          ElMessage.error(response.errorMsg || '确认收货失败')
        }
      } catch (error) {
        ElMessage.error('确认收货失败')
      }
    }
    
    const handleCancel = async () => {
      try {
        const { value: reason } = await ElMessageBox.prompt('请输入取消原因', '取消订单', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          inputPlaceholder: '请输入取消原因'
        })
        
        const response = await orderAPI.cancel(order.value.orderId, reason)
        if (response.success) {
          ElMessage.success('订单取消成功')
          fetchOrderDetail()
        } else {
          ElMessage.error(response.errorMsg || '取消订单失败')
        }
      } catch (error) {
        if (error !== 'cancel') {
          ElMessage.error('取消订单失败')
        }
      }
    }
    
    const handleShip = async () => {
      try {
        const { value: trackingNumber } = await ElMessageBox.prompt('请输入物流单号', '发货', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          inputPlaceholder: '请输入物流单号'
        })
        
        const response = await orderAPI.ship(order.value.orderId, trackingNumber)
        if (response.success) {
          ElMessage.success('发货成功')
          fetchOrderDetail()
        } else {
          ElMessage.error(response.errorMsg || '发货失败')
        }
      } catch (error) {
        if (error !== 'cancel') {
          ElMessage.error('发货失败')
        }
      }
    }
    
    const handleRate = () => {
      ElMessage.info('评价功能开发中...')
    }
    
    const handleReorder = () => {
      router.push(`/create-order/${order.value.orderId}`)
    }
    
    const handleRefund = () => {
      ElMessage.info('退款处理功能开发中...')
    }
    
    // 联系功能
    const contactSeller = async () => {
      if (!isLoggedIn.value) {
        ElMessage.warning('请先登录')
        router.push('/login')
        return
      }
      
      // 判断当前用户是买家还是卖家
      const isBuyer = order.value.buyerId === user.value?.userId
      const contactUserId = isBuyer ? order.value.sellerId : order.value.buyerId
      
      try {
        const response = await contactAPI.createConversation(
          contactUserId,
          null,
          order.value.orderId
        )
        
        if (response.success) {
          router.push({
            path: '/messages',
            query: { 
              conversationId: response.data.conversationId,
              orderId: order.value.orderId
            }
          })
        }
      } catch (error) {
        console.error('创建对话失败:', error)
        ElMessage.error('创建对话失败')
      }
    }
    
    const contactBuyer = async () => {
      if (!isLoggedIn.value) {
        ElMessage.warning('请先登录')
        router.push('/login')
        return
      }
      
      // 判断当前用户是买家还是卖家
      const isBuyer = order.value.buyerId === user.value?.userId
      const contactUserId = isBuyer ? order.value.sellerId : order.value.buyerId
      
      try {
        const response = await contactAPI.createConversation(
          contactUserId,
          null,
          order.value.orderId
        )
        
        if (response.success) {
          router.push({
            path: '/messages',
            query: { 
              conversationId: response.data.conversationId,
              orderId: order.value.orderId
            }
          })
        }
      } catch (error) {
        console.error('创建对话失败:', error)
        ElMessage.error('创建对话失败')
      }
    }
    
    const viewSellerProfile = () => {
      if (order.value.sellerId) {
        router.push(`/profile/${order.value.sellerId}`)
      }
    }
    
    const viewBuyerProfile = () => {
      if (order.value.buyerId) {
        router.push(`/profile/${order.value.buyerId}`)
      }
    }
    
    // 检查是否有退货进度信息
    // 注意：即使有拒绝信息，也可能有后续的申请和审批，所以应该同时显示
    const hasReturnInfo = (order) => {
      // 检查退货进度相关字段是否有非null值
      // 注意：如果有审批时间但没有拒绝原因，说明是审批通过
      // 如果有拒绝原因但申请时间在拒绝时间之后，说明已重新申请
      return !!(order.returnReason || 
                order.returnRequestTime || 
                (order.returnApprovalTime && !order.returnRejectionReason) ||
                order.returnTrackingNumber || 
                order.returnCompletionTime)
    }
    
    // 检查是否有退货拒绝信息
    const hasReturnRejectionInfo = (order) => {
      // 检查退货拒绝相关字段是否有非null值
      return !!(order.returnRejectionReason)
    }
    
    // 检查是否有多次退货申请（申请时间在拒绝时间之后）
    const hasMultipleReturnRequests = (order) => {
      if (!order.returnRequestTime || !order.returnApprovalTime || !order.returnRejectionReason) {
        return false
      }
      // 如果申请时间在拒绝时间之后，说明是重新申请
      const requestTime = new Date(order.returnRequestTime)
      const rejectionTime = new Date(order.returnApprovalTime)
      return requestTime > rejectionTime
    }
    
    // 判断是否应该显示审批通过时间
    // 只在订单状态表示已通过时显示
    const shouldShowApprovalTime = (order) => {
      if (!order.returnApprovalTime) {
        return false
      }
      // 如果订单状态是已通过或已完成，returnApprovalTime是审批通过时间
      const approvedStatuses = ['REFUND_APPROVED', 'RETURN_APPROVED', 'RETURN_COMPLETED']
      if (approvedStatuses.includes(order.orderStatus)) {
        // 如果有拒绝原因，说明之前被拒过，但现在是已通过状态，returnApprovalTime是同意时间
        // 如果returnRequestTime存在且returnApprovalTime > returnRequestTime，说明这是同意时间
        if (order.returnRequestTime) {
          return new Date(order.returnApprovalTime) >= new Date(order.returnRequestTime)
        }
        // 如果没有申请时间但有通过状态，显示
        return true
      }
      return false
    }
    
    // 判断returnApprovalTime是否是拒绝时间（而不是同意时间）
    // 只在订单状态是拒绝状态时，returnApprovalTime才是拒绝时间
    const isRejectionTime = (order) => {
      if (!order.returnApprovalTime || !order.returnRejectionReason) {
        return false
      }
      // 如果订单状态是拒绝状态，returnApprovalTime一定是拒绝时间
      const rejectedStatuses = ['REFUND_REJECTED', 'RETURN_REJECTED']
      return rejectedStatuses.includes(order.orderStatus)
    }
    
    // 获取拒绝信息区域的标题
    const getRejectionSectionTitle = (order) => {
      // 如果订单当前状态是拒绝状态，显示"退货拒绝"
      const rejectedStatuses = ['REFUND_REJECTED', 'RETURN_REJECTED']
      if (rejectedStatuses.includes(order.orderStatus)) {
        return '退货拒绝'
      }
      // 如果订单已通过但有拒绝原因，说明是历史记录
      return '退货拒绝历史'
    }
    
    // 处理地址选择变化
    const handleAddressChange = (addressId, address) => {
      selectedAddressId.value = addressId
      selectedAddress.value = address
    }
    
    // 更新订单地址
    const handleUpdateAddress = async () => {
      if (!selectedAddress.value) {
        ElMessage.warning('请先选择一个地址')
        return
      }
      
      if (!order.value) {
        ElMessage.error('订单信息不存在')
        return
      }
      
      updatingAddress.value = true
      try {
        // 构建地址更新数据
        const addressData = {
          addressId: selectedAddress.value.addressId || null,
          province: selectedAddress.value.province,
          city: selectedAddress.value.city,
          district: selectedAddress.value.district,
          streetAddress: selectedAddress.value.streetAddress,
          detailAddress: selectedAddress.value.detailAddress || '',
          fullAddress: selectedAddress.value.fullAddress,
          recipientName: selectedAddress.value.recipientName,
          recipientPhone: selectedAddress.value.recipientPhone
        }
        
        const response = await orderAPI.updateShippingAddress(order.value.orderId, addressData)
        
        if (response.success) {
          ElMessage.success('地址更新成功')
          showEditAddressDialog.value = false
          // 重新获取订单详情以刷新显示
          await fetchOrderDetail()
        } else {
          ElMessage.error(response.message || '地址更新失败')
        }
      } catch (error) {
        console.error('更新订单地址失败:', error)
        ElMessage.error(error.response?.data?.message || '地址更新失败，请稍后重试')
      } finally {
        updatingAddress.value = false
      }
    }
    
    onMounted(() => {
      fetchOrderDetail()
    })
    
    return {
      loading,
      order,
      user,
      isBuyer,
      isSeller,
      canModifyAddress,
      showEditAddressDialog,
      selectedAddressId,
      selectedAddress,
      updatingAddress,
      handleAddressChange,
      handleUpdateAddress,
      payCheck,
      confirmCheck,
      cancelCheck,
      shipCheck,
      formatTime,
      getPayTime,
      getPayTimeDisplay,
      hasReturnInfo,
      hasReturnRejectionInfo,
      hasMultipleReturnRequests,
      shouldShowApprovalTime,
      isRejectionTime,
      getRejectionSectionTitle,
      getStatusType,
      getStatusText,
      getCommodityImage,
      getCommodityTitle,
      getCommodityPrice,
      getCommodityLocation,
      formatCommodityAddress,
      formatCommodityAddressRegion,
      formatCommodityAddressDetail,
      shouldUseSingleLineCommodityAddress,
      getCommodityCategory,
      getCommodityCondition,
      getCommodityDescription,
      isCommoditySnapshotOffShelf,
      getShippingAddressFull,
      getShippingRecipientName,
      getShippingRecipientPhone,
      formatShippingAddress,
      formatShippingAddressRegion,
      formatShippingAddressDetail,
      shouldUseSingleLineShippingAddress,
      getSellerName,
      getSellerPhone,
      getSellerEmail,
      getSellerAvatar,
      isSellerDeleted,
      getBuyerName,
      getBuyerPhone,
      getBuyerEmail,
      getBuyerAvatar,
      isBuyerDeleted,
      getAvatarUrl,
      handlePay,
      handleConfirm,
      handleCancel,
      handleShip,
      handleRate,
      handleReorder,
      handleRefund,
      contactSeller,
      contactBuyer,
      viewSellerProfile,
      viewBuyerProfile
    }
  }
}
</script>

<style scoped>
.order-detail-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px;
}

.order-detail-wrapper {
  width: 100%;
}

.order-detail {
  display: block;
}

.order-left-column,
.order-right-column {
  width: 100%;
}

.loading-container {
  padding: 20px;
}

.page-header {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 15px;
  margin-bottom: 30px;
  padding-bottom: 20px;
  border-bottom: 1px solid #e4e7ed;
}

.page-header h1 {
  margin: 0;
  color: var(--primary-color);
  text-align: center;
}

.page-header-actions {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  gap: 12px;
}

/* border-radius 由 UnifiedButton 统一管理（9999px） */
.order-detail .unified-button :deep(.el-button) {
  max-width: 150px; /* 统一的最大长度 */
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  padding-left: 16px;
  padding-right: 16px;
}

/* .back-button border-radius 由 UnifiedButton 统一管理（9999px） */

/* border-radius 由 UnifiedTag 统一管理（9999px） */


.order-info-section,
.commodity-section,
.seller-section,
.buyer-section,
.shipping-section,
.return-section,
.return-rejection-section {
  background: transparent;
  border-radius: 8px;
  padding: 20px;
  margin-bottom: 20px;
}

.order-info-section h2,
.commodity-section h2,
.seller-section h2,
.buyer-section h2,
.shipping-section h2,
.return-section h2,
.return-rejection-section h2 {
  margin: 0 0 20px 0;
  color: var(--primary-color);
  font-size: 18px;
}

.return-section {
  border-left: 4px solid var(--el-color-warning);
}

.return-rejection-section {
  border-left: 4px solid var(--el-color-danger);
}

.return-info,
.return-rejection-info {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.return-history-hint {
  margin-left: 8px;
  color: #999;
  font-size: 12px;
  font-style: italic;
}

.info-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
  gap: 15px;
}

.info-item {
  display: flex;
  align-items: center;
}

.info-item label {
  font-weight: normal;
  color: #606266;
  margin-right: 10px;
  min-width: 100px;
}

.info-item span {
  color: #303133;
  word-break: break-all;
  word-wrap: break-word;
}

.address-display {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.address-region {
  color: #303133;
  font-weight: normal;
}

.address-detail {
  color: #606266;
  font-size: 14px;
  margin-left: 0;
  padding-left: 0;
}

.address-single-line {
  color: #303133;
  font-weight: normal;
}

.total-amount {
  font-size: 18px;
  font-weight: normal;
  color: #e74c3c;
}

.commodity-card {
  display: flex;
  gap: 20px;
  align-items: flex-start;
}

.commodity-image {
  width: 200px;
  height: 200px;
  border-radius: 8px;
  overflow: hidden;
  background: #f5f7fa;
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
  color: #909399;
  font-size: 14px;
}

.commodity-details {
  flex: 1;
  min-width: 0;
  max-width: none;
}

.commodity-title {
  margin: 0 0 10px 0;
  color: var(--primary-color);
  font-size: 20px;
  display: flex;
  align-items: center;
}

.commodity-price,
.commodity-quantity,
.commodity-location,
.commodity-category,
.commodity-condition {
  margin: 5px 0;
  color: #606266;
}

.commodity-location {
  display: flex;
  align-items: flex-start;
  gap: 8px;
}

.commodity-location label {
  font-weight: normal;
  color: #606266;
  margin-right: 0;
  min-width: auto;
  flex-shrink: 0;
}

.commodity-description {
  margin-top: 15px;
}

.commodity-description label {
  font-weight: normal;
  color: #606266;
}

.commodity-description p {
  margin: 5px 0 0 0;
  color: #303133;
  line-height: 1.6;
}

.seller-card,
.buyer-card {
  display: flex;
  gap: 20px;
  align-items: center;
}

.seller-avatar,
.buyer-avatar {
  width: 80px;
  height: 80px;
  border-radius: 50%;
  overflow: hidden;
  background: #f5f7fa;
  display: flex;
  align-items: center;
  justify-content: center;
}

.seller-avatar img,
.buyer-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.default-avatar {
  width: 100%;
  height: 100%;
  background: #409eff;
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  font-weight: normal;
}

.seller-details,
.buyer-details {
  flex: 1;
}

.seller-details h3,
.buyer-details h3 {
  margin: 0 0 10px 0;
  color: #303133;
  font-size: 18px;
}

.seller-contact,
.buyer-contact {
  margin: 5px 0;
  color: #606266;
}

.seller-actions,
.buyer-actions {
  margin-top: 15px;
  display: flex;
  gap: 10px;
}

.deleted-notice {
  margin-top: 15px;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.section-header h2 {
  margin: 0;
}

.shipping-info {
  display: flex;
  flex-direction: column;
  gap: 15px;
}

.action-section {
  background: transparent;
  border-radius: 8px;
  padding: 20px;
}

.action-buttons {
  display: flex;
  gap: 15px;
  justify-content: center;
}

.empty-state {
  text-align: center;
  padding: 60px 20px;
}

@media (min-width: 1200px) {
  /* 宽屏容器更宽 */
  .order-detail-container {
    max-width: 1280px;
  }
  
  /* 宽屏两栏布局：左（订单信息+商品信息），右（其他信息） */
  .order-detail {
    display: grid;
    grid-template-columns: 1.1fr 1fr;
    grid-column-gap: 32px;
    grid-row-gap: 0;
    align-items: start;
  }
  
  .order-left-column {
    grid-column: 1 / 2;
  }
  
  .order-right-column {
    grid-column: 2 / 3;
  }
}

@media (min-width: 1600px) {
  .order-detail-container {
    max-width: 1440px;
  }
  
  .order-detail {
    grid-column-gap: 48px;
  }
}

@media (max-width: 900px) {
  .order-detail-container {
    padding: 10px;
  }
  
  .commodity-card {
    flex-direction: column;
  }
  
  .commodity-image {
    width: 100%;
    height: 250px;
  }
  
  .seller-card,
  .buyer-card {
    flex-direction: column;
    text-align: center;
  }
}

/* 修改地址弹窗宽度与安全边距 */
/* 订单修改地址弹窗与 AddressSelector 共享 */
.address-manager-modal {
  position: fixed;
  inset: 0;
  z-index: 2200;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
}

.address-manager-modal__overlay {
  position: absolute;
  inset: 0;
  background: rgba(0, 0, 0, 0.45);
}

.address-manager-modal__panel {
  position: relative;
  z-index: 1;
  width: min(600px, calc(100vw - 48px));
  max-height: calc(100vh - 48px);
  background: white;
  border-radius: 16px;
  box-shadow: 0 12px 32px rgba(0, 0, 0, 0.18);
  display: flex;
  flex-direction: column;
}

.address-manager-modal__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px;
  border-bottom: 1px solid #f0f0f0;
}

.address-manager-modal__body {
  padding: 12px 20px 20px;
  overflow-y: auto;
}

.address-manager-modal__footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  padding: 16px 20px 20px;
  border-top: 1px solid #f0f0f0;
}

.address-manager-modal__header h3 {
  margin: 0;
  font-size: 18px;
  color: var(--primary-color);
}

.modal-close {
  cursor: pointer;
  font-size: 18px;
  color: #999;
}

.modal-close:hover {
  color: var(--primary-color);
}

@media (max-width: 900px) {
  .address-manager-modal {
    padding: calc(env(safe-area-inset-top, 8px) + 70px) var(--mobile-safe-margin, 6px) var(--mobile-safe-margin, 6px);
    align-items: flex-start;
    justify-content: flex-start;
  }
  
  .address-manager-modal__panel {
    width: calc(100vw - 2 * var(--mobile-safe-margin, 6px));
    max-height: calc(100vh - (env(safe-area-inset-top, 8px) + 70px) - var(--mobile-safe-margin, 6px));
  }
  
  .address-manager-modal__body {
    padding: 12px var(--mobile-safe-margin, 6px) var(--mobile-safe-margin, 6px);
  }
  
  .address-manager-modal__footer {
    flex-direction: column;
    padding: var(--mobile-safe-margin, 6px);
  }
  
  .address-manager-modal__footer :deep(.el-button) {
    width: 100%;
    margin-left: 0 !important;
  }
}
</style>
