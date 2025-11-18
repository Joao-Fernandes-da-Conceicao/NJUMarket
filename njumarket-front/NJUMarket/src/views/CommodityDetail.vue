<template>
  <div class="commodity-detail-page">
    <!-- 页面标题和返回按钮 -->
    <div class="page-header">
      <div class="container">
        <div class="header-content">
          <div class="desktop-back-button" @click="handleBack">
            <el-icon><ArrowLeft /></el-icon>
          </div>
        </div>
      </div>
    </div>
    
    <!-- 商品详情内容 -->
    <div class="detail-content" v-loading="loading">
      <div class="container">
        <div class="detail-wrapper" v-if="commodity">
          <!-- 商品图片 -->
          <div class="image-section">
            <div class="main-image">
              <img 
                v-if="commodity.images && commodity.images.length > 0"
                :src="getCommodityImageUrl(currentImage || commodity.images[0])"
                :alt="commodity.title"
                @error="handleImageError"
              />
              <div v-else class="no-image">
                <span>暂无照片</span>
              </div>
            </div>
            <div class="image-thumbnails" v-if="commodity.images && commodity.images.length > 1">
              <div
                v-for="(image, index) in commodity.images"
                :key="index"
                class="thumbnail"
                :class="{ active: currentImage === image }"
                @click="currentImage = image"
              >
                <img :src="getCommodityImageUrl(image)" :alt="`${commodity.title} ${index + 1}`" />
              </div>
            </div>
          </div>

          <!-- 商品信息 -->
          <div class="info-section">
            <h1 class="commodity-title">
              {{ commodity.title }}
              <!-- 显示商品状态 -->
              <UnifiedTag 
                v-if="!buyCheck.canOrder" 
                type="warning" 
                size="large"
                style="margin-left: 12px;"
              >
                {{ buyCheck.reason }}
              </UnifiedTag>
            </h1>
            <div class="price-section">
              <span class="price text-primary">¥{{ commodity.price }}</span>
              <span class="original-price" v-if="commodity.originalPrice">
                原价：¥{{ commodity.originalPrice }}
              </span>
            </div>
            
            <div class="commodity-meta">
              <div class="meta-item">
                <span class="label">分类：</span>
                <span class="value">{{ commodity.category }}</span>
              </div>
              <div class="meta-item">
                <span class="label">成色：</span>
                <span class="value">{{ commodity.conditionLevel }}</span>
              </div>
              <div class="meta-item">
                <span class="label">位置：</span>
                <!-- 如果有地址快照字段，使用标准格式（省市区-详细地址） -->
                <div v-if="!shouldUseSingleLineAddress(commodity)" class="address-display">
                  <div class="address-region">{{ formatCommodityAddressRegion(commodity) }}</div>
                  <div v-if="formatCommodityAddressDetail(commodity)" class="address-detail">
                    {{ formatCommodityAddressDetail(commodity) }}
                  </div>
                </div>
                <!-- 如果是旧数据（只有location），使用单行显示（废物利用） -->
                <span v-else class="address-single-line">{{ formatCommodityAddressRegion(commodity) }}</span>
              </div>
              <div class="meta-item">
                <span class="label">库存：</span>
                <span class="value">{{ commodity.stock }} 件</span>
              </div>
            </div>

            <div class="description-section">
              <h3>商品描述</h3>
              <p class="description">{{ commodity.description }}</p>
            </div>

            <!-- 操作按钮 -->
            <div class="action-section">
              <UnifiedButton
                type="primary"
                size="large"
                :disabled="!buyCheck.canOrder"
                @click="handleBuy"
                class="buy-btn"
              >
                {{ buyCheck.reason || '立即购买' }}
              </UnifiedButton>
              <UnifiedButton
                size="large"
                :disabled="!contactCheck.canContact"
                @click="handleContact"
                class="contact-btn"
              >
                {{ contactCheck.reason || '联系卖家' }}
              </UnifiedButton>
            </div>
          </div>
        </div>

        <!-- 卖家信息 -->
        <div class="seller-section" v-if="commodity && commodity.seller">
          <div class="seller-card">
            <div class="seller-info">
              <el-avatar :size="60" :src="commodity.seller.avatar">
                {{ commodity.seller.nickname?.charAt(0) || 'S' }}
              </el-avatar>
              <div class="seller-details">
                <h3>{{ commodity.seller.nickname || '卖家' }}</h3>
                <div class="seller-stats">
                  <span class="stat-item">
                    <span class="stat-label">信用分：</span>
                    <span class="stat-value">{{ commodity.seller.creditScore || 0 }}</span>
                  </span>
                  <span class="stat-item">
                    <span class="stat-label">卖家评分：</span>
                    <span class="stat-value">{{ commodity.seller.sellerRating || 0 }}</span>
                  </span>
                </div>
              </div>
            </div>
            <div class="seller-actions">
              <UnifiedButton 
                type="primary" 
                :disabled="!contactCheck.canContact"
                @click="handleContact"
              >
                {{ contactCheck.reason || '联系卖家' }}
              </UnifiedButton>
              <UnifiedButton @click="viewSellerProfile">查看资料</UnifiedButton>
            </div>
          </div>
        </div>
      </div>
    </div>

  </div>
</template>

<script>
import { ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '../stores/user'
import { commodityAPI, contactAPI, imageAPI } from '../api'
import { ElMessage } from 'element-plus'
import { ArrowLeft } from '@element-plus/icons-vue'
import UnifiedButton from '../components/common/UnifiedButton.vue'
import UnifiedTag from '../components/common/UnifiedTag.vue'
import { canCreateOrder, canContactSeller } from '../utils/orderRules'

export default {
  name: 'CommodityDetail',
  components: {
    UnifiedButton,
    UnifiedTag,
    ArrowLeft
  },
  setup() {
    const route = useRoute()
    const router = useRouter()
    const userStore = useUserStore()
    
    const loading = ref(false)
    const commodity = ref(null)
    const currentImage = ref('')
    
    const isLoggedIn = computed(() => userStore.isLoggedIn)
    const user = computed(() => userStore.user)
    
    // 使用统一的规则检查
    const buyCheck = computed(() => {
      if (!commodity.value) return { canOrder: false, reason: '' }
      return canCreateOrder({
        commodity: commodity.value,
        user: user.value,
        quantity: 1
      })
    })
    
    const contactCheck = computed(() => {
      if (!commodity.value) return { canContact: false, reason: '' }
      return canContactSeller({
        commodity: commodity.value,
        user: user.value
      })
    })
    
    // 获取商品详情
    const fetchCommodityDetail = async () => {
      loading.value = true
      try {
        const commodityId = route.params.id
        // 使用标准的商品详情API
        const response = await commodityAPI.getDetail(commodityId)
        if (response.success) {
          commodity.value = response.data
          currentImage.value = response.data.images?.[0] || ''
          
          // 商品状态已通过buyCheck computed属性实时检查
          
          // 注意：后端 getCommodityDetail 已经自动增加浏览量了，不需要额外调用 recordView
        } else {
          ElMessage.error('商品不存在或已下架')
          router.push('/commodities')
        }
      } catch (error) {
        ElMessage.error('获取商品详情失败')
        router.push('/commodities')
      } finally {
        loading.value = false
      }
    }
    
    // 处理图片错误
    const handleImageError = (e) => {
      e.target.src = imageAPI.getDefaultCommodityImage()
    }
    
    // 获取商品图片URL
    const getCommodityImageUrl = (imageUrl) => {
      if (!imageUrl) return imageAPI.getDefaultCommodityImage()
      
      // 如果已经是完整URL，直接返回
      if (imageUrl.startsWith('http')) return imageUrl
      
      // 如果是文件名，构建完整URL
      if (imageUrl.includes('/')) return imageUrl
      
      // 从URL中提取文件名
      const fileName = imageUrl.split('/').pop()
      return imageAPI.getCommodityImage(fileName)
    }
    
    // 购买商品 - 跳转到创建订单页面
    const handleBuy = async () => {
      if (!isLoggedIn.value) {
        ElMessage.warning('请先登录')
        router.push('/login')
        return
      }
      
      // 使用统一的规则检查
      const check = canCreateOrder({
        commodity: commodity.value,
        user: user.value,
        quantity: 1
      })
      
      if (!check.canOrder) {
        alert(check.reason)
        return
      }
      
      // 跳转到创建订单页面
      router.push(`/create-order/${commodity.value.commodityId}`)
    }
    
    // 联系卖家
    const handleContact = async () => {
      if (!isLoggedIn.value) {
        ElMessage.warning('请先登录')
        router.push('/login')
        return
      }
      
      // 使用统一的规则检查
      const check = canContactSeller({
        commodity: commodity.value,
        user: user.value
      })
      
      if (!check.canContact) {
        alert(check.reason)
        return
      }
      
      try {
        const response = await contactAPI.createConversation(
          commodity.value.sellerId,
          commodity.value.commodityId,
          null
        )
        
        if (response.success) {
          router.push({
            path: '/messages',
            query: { 
              conversationId: response.data.conversationId,
              commodityId: commodity.value.commodityId
            }
          })
        }
      } catch (error) {
        console.error('创建对话失败:', error)
        ElMessage.error('创建对话失败')
      }
    }
    
    // 查看卖家主页
    const viewSellerProfile = () => {
      if (commodity.value.sellerId) {
        router.push(`/home/${commodity.value.sellerId}`)
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
    
    // 返回上一页
    const handleBack = () => {
      router.back()
    }
    
    // 获取商品地址（优先使用地址快照）
    const getCommodityAddress = (commodity) => {
      // 优先使用地址快照完整地址
      if (commodity.addressSnapshotFull) {
        return commodity.addressSnapshotFull
      }
      // 兼容旧字段
      return commodity.location || '未设置位置'
    }
    
    // 格式化商品地址的省市区部分
    const formatCommodityAddressRegion = (commodity) => {
      // 优先使用地址快照的省市区字段（结构化数据）
      const parts = []
      if (commodity.addressSnapshotProvince) {
        parts.push(commodity.addressSnapshotProvince)
      }
      if (commodity.addressSnapshotCity) {
        parts.push(commodity.addressSnapshotCity)
      }
      if (commodity.addressSnapshotDistrict) {
        parts.push(commodity.addressSnapshotDistrict)
      }
      
      if (parts.length > 0) {
        return parts.join('')
      }
      
      // 如果没有省市区字段，尝试从完整地址中提取
      if (commodity.addressSnapshotFull) {
        return commodity.addressSnapshotFull
      }
      
      // 兼容旧字段：将 location 作为完整地址显示（废物利用）
      // 注意：旧数据的 location 可能是自由文本，格式不统一，所以作为完整地址单行显示
      return commodity.location || '未设置位置'
    }
    
    // 格式化商品地址的详细地址部分
    const formatCommodityAddressDetail = (commodity) => {
      // 如果有地址快照的详细地址字段，使用结构化数据
      const parts = []
      if (commodity.addressSnapshotStreet) {
        parts.push(commodity.addressSnapshotStreet)
      }
      if (commodity.addressSnapshotDetail) {
        parts.push(commodity.addressSnapshotDetail)
      }
      
      if (parts.length > 0) {
        return parts.join('')
      }
      
      // 如果没有详细地址字段，但有完整地址快照，说明是旧数据格式
      // 这种情况下，详细地址部分为空，完整地址已经在省市区部分显示了
      return ''
    }
    
    // 检查是否应该使用单行显示（旧数据格式）
    const shouldUseSingleLineAddress = (commodity) => {
      // 如果有地址快照的省市区字段，使用标准格式（省市区-详细地址）
      if (commodity.addressSnapshotProvince || commodity.addressSnapshotCity || commodity.addressSnapshotDistrict) {
        return false
      }
      // 如果没有地址快照字段，但有 location，使用单行显示（废物利用）
      if (commodity.location && !commodity.addressSnapshotFull) {
        return true
      }
      return false
    }
    
    onMounted(() => {
      fetchCommodityDetail()
    })
    
    return {
      loading,
      commodity,
      currentImage,
      buyCheck,
      contactCheck,
      isLoggedIn,
      user,
      ArrowLeft,
      handleImageError,
      getCommodityImageUrl,
      handleBuy,
      handleContact,
      viewSellerProfile,
      handleLogout,
      handleBack,
      getCommodityAddress,
      formatCommodityAddressRegion,
      formatCommodityAddressDetail,
      shouldUseSingleLineAddress
    }
  }
}
</script>

<style scoped>
.commodity-detail-page {
  min-height: 100vh;
  background-color: #f5f5f5;
}

.page-header {
  background: transparent;
  padding: 15px 0;
}

.header-content {
  display: flex;
  align-items: center;
  justify-content: flex-start;
}

.back-button {
  background-color: transparent;
  border: transparent;
  color: var(--primary-color);
  transition: all 0.3s ease;
}

.back-button:hover {
  background-color: rgba(106, 1, 94, 0.05);
}

/* 返回键：与聊天卡片完全一致（desktop-back-button）*/
.desktop-back-button { display: block }

.detail-content {
  padding: 30px 0;
}

.detail-wrapper {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 40px;
  margin-bottom: 40px;
}

.image-section {
  background: transparent;
  border-radius: 8px;
  padding: 20px;
}

.main-image {
  width: 100%;
  height: 400px;
  margin-bottom: 20px;
  border-radius: 8px;
  overflow: hidden;
  background: #f5f5f5;
  display: flex;
  align-items: center;
  justify-content: center;
}

.main-image img {
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
  font-size: 18px;
}

.image-thumbnails {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.thumbnail {
  width: 80px;
  height: 80px;
  border-radius: 6px;
  overflow: hidden;
  cursor: pointer;
  border: 2px solid transparent;
  transition: border-color 0.3s ease;
}

.thumbnail.active {
  border-color: var(--primary-color);
}

.thumbnail img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.info-section {
  background: transparent;
  border-radius: 8px;
  padding: 30px;
}

.commodity-title {
  font-size: 28px;
  font-weight: normal;
  margin-bottom: 20px;
  color: var(--primary-color);
}

.price-section {
  margin-bottom: 30px;
}

.price {
  font-size: 32px;
  font-weight: normal;
  margin-right: 15px;
}

.original-price {
  font-size: 16px;
  color: #999;
  text-decoration: line-through;
}

.commodity-meta {
  margin-bottom: 30px;
}

.meta-item {
  display: flex;
  margin-bottom: 10px;
}

.label {
  font-weight: normal;
  color: #666;
  width: 80px;
}

.value {
  color: #333;
}

.address-display {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.address-region {
  color: #333;
  font-weight: normal;
}

.address-detail {
  color: #666;
  font-size: 13px;
  margin-left: 0;
  padding-left: 0;
}

.address-single-line {
  color: #333;
  font-weight: normal;
}

.description-section {
  margin-bottom: 30px;
}

.description-section h3 {
  font-size: 18px;
  font-weight: normal;
  margin-bottom: 15px;
  color: #333;
}

.description {
  line-height: 1.6;
  color: #666;
}

.action-section {
  display: flex;
  gap: 15px;
}

.buy-btn {
  flex: 1;
  height: 50px;
  font-size: 16px;
  font-weight: normal;
  /* border-radius 由 UnifiedButton 统一管理（9999px） */
}

.contact-btn {
  flex: 1;
  height: 50px;
  font-size: 16px;
  margin-left: 0px !important;
  /* border-radius 由 UnifiedButton 统一管理（9999px） */
}

.seller-section {
  margin-bottom: 40px;
}

.seller-card {
  background: transparent;
  border-radius: 8px;
  padding: 20px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.seller-info {
  display: flex;
  align-items: center;
  gap: 15px;
}

.seller-details h3 {
  margin: 0 0 10px 0;
  font-size: 18px;
  font-weight: normal;
  color: var(--primary-color);
}

.seller-stats {
  display: flex;
  gap: 20px;
}

.stat-item {
  font-size: 14px;
}

.stat-label {
  color: #666;
}

.stat-value {
  color: var(--primary-color);
  font-weight: normal;
}

.seller-actions {
  display: flex;
  gap: 10px;
}

.buy-dialog-content {
  padding: 20px 0;
}

.buy-item {
  display: flex;
  gap: 15px;
  margin-bottom: 20px;
  padding: 15px;
  background: #f8f9fa;
  border-radius: 8px;
}

.buy-item img {
  width: 80px;
  height: 80px;
  object-fit: cover;
  border-radius: 6px;
}

.buy-info h3 {
  margin: 0 0 10px 0;
  font-size: 16px;
  font-weight: normal;
}

.buy-info .price {
  font-size: 18px;
  font-weight: normal;
  color: var(--primary-color);
}

.buy-summary {
  margin-top: 20px;
  padding-top: 20px;
  border-top: 1px solid #e0e0e0;
}

.summary-item {
  display: flex;
  justify-content: space-between;
  margin-bottom: 10px;
}

.summary-item.total {
  font-size: 18px;
  font-weight: normal;
  padding-top: 10px;
  border-top: 1px solid #e0e0e0;
}

.total-price {
  color: var(--primary-color);
}

@media (max-width: 900px) {
  .page-header {
    padding: 10px 0;
  }
  
  .back-button {
    font-size: 16px;
    padding: 8px;
  }
  
  .detail-wrapper {
    grid-template-columns: 1fr;
    gap: 20px;
  }
  
  .nav-content {
    flex-direction: column;
    gap: 15px;
  }
  
  .nav-menu {
    gap: 20px;
  }
  
  .action-section {
    flex-wrap: wrap;
  }
  
  .buy-btn,
  .contact-btn {
    min-width: 130px;
  }
  
  .seller-card {
    flex-direction: column;
    gap: 20px;
    text-align: center;
  }
}
</style>
