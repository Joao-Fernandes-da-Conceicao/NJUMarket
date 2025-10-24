<template>
  <div class="commodity-detail-page">
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
            <h1 class="commodity-title">{{ commodity.title }}</h1>
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
                <span class="value">{{ commodity.location }}</span>
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
              <el-button
                type="primary"
                size="large"
                :disabled="!isLoggedIn || commodity.sellerId === user?.userId"
                @click="handleBuy"
                class="buy-btn"
              >
                {{ commodity.sellerId === user?.userId ? '自己的商品' : '立即购买' }}
              </el-button>
              <el-button
                size="large"
                @click="handleContact"
                class="contact-btn"
              >
                联系卖家
              </el-button>
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
              <el-button type="primary" @click="handleContact">联系卖家</el-button>
              <el-button @click="viewSellerProfile">查看资料</el-button>
            </div>
          </div>
        </div>

        <!-- 相关商品推荐 -->
        <div class="related-section" v-if="relatedCommodities.length > 0">
          <h2>相关推荐</h2>
          <div class="related-grid">
            <div
              v-for="item in relatedCommodities"
              :key="item.commodityId"
              class="related-card"
              @click="$router.push(`/commodity/${item.commodityId}`)"
            >
              <div class="related-image">
                <img
                  :src="getCommodityImageUrl(item.images?.[0])"
                  :alt="item.title"
                />
              </div>
              <div class="related-info">
                <h4>{{ item.title }}</h4>
                <p class="related-price text-primary">¥{{ item.price }}</p>
              </div>
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
import { commodityAPI, orderAPI, imageAPI } from '../api'
import { ElMessage } from 'element-plus'

export default {
  name: 'CommodityDetail',
  setup() {
    const route = useRoute()
    const router = useRouter()
    const userStore = useUserStore()
    
    const loading = ref(false)
    const commodity = ref(null)
    const relatedCommodities = ref([])
    const currentImage = ref('')
    
    const isLoggedIn = computed(() => userStore.isLoggedIn)
    const user = computed(() => userStore.user)
    
    // 获取商品详情
    const fetchCommodityDetail = async () => {
      loading.value = true
      try {
        const commodityId = route.params.id
        const response = await commodityAPI.getDetail(commodityId)
        if (response.success) {
          commodity.value = response.data
          currentImage.value = response.data.images?.[0] || ''
          
          // 记录浏览
          await commodityAPI.recordView(commodityId)
          
          // 获取相关商品
          await fetchRelatedCommodities()
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
    
    // 获取相关商品
    const fetchRelatedCommodities = async () => {
      try {
        const response = await commodityAPI.getByCategory(commodity.value.category, 1, 4)
        if (response.success) {
          // 修复：正确映射后端返回的数据结构
          const relatedData = response.data.commodities || response.data || []
          relatedCommodities.value = relatedData.filter(
            item => item.commodityId !== commodity.value.commodityId
          ).slice(0, 4)
        }
      } catch (error) {
        console.error('获取相关商品失败:', error)
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
    
    // 购买商品 - 简化版：点击即创建订单
    const handleBuy = async () => {
      if (!isLoggedIn.value) {
        ElMessage.warning('请先登录')
        router.push('/login')
        return
      }
      
      if (commodity.value.sellerId === user.value.userId) {
        ElMessage.warning('不能购买自己的商品')
        return
      }
      
      // 检查商品状态
      if (commodity.value.commodityStatus !== 'ON_SHELF') {
        ElMessage.warning('商品未上架，无法购买')
        return
      }
      
      // 检查库存
      if (commodity.value.stock <= 0) {
        ElMessage.warning('商品库存不足')
        return
      }
      
      try {
        const orderData = {
          commodityId: commodity.value.commodityId,
          sellerId: commodity.value.sellerId,
          quantity: 1, // 默认购买数量为1
          payAmount: commodity.value.price,
          shippingAddress: '校内自提', // 默认地址
          remark: ''
        }
        
        const response = await orderAPI.create(orderData)
        if (response.success) {
          ElMessage.success('订单创建成功！')
          router.push('/orders')
        } else {
          ElMessage.error(response.errorMsg || '创建订单失败')
        }
      } catch (error) {
        ElMessage.error('创建订单失败')
      }
    }
    
    // 联系卖家
    const handleContact = () => {
      ElMessage.info('联系卖家功能开发中...')
    }
    
    // 查看卖家资料
    const viewSellerProfile = () => {
      if (commodity.value.sellerId) {
        router.push(`/profile/${commodity.value.sellerId}`)
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
      fetchCommodityDetail()
    })
    
    return {
      loading,
      commodity,
      relatedCommodities,
      currentImage,
      isLoggedIn,
      user,
      handleImageError,
      getCommodityImageUrl,
      handleBuy,
      handleContact,
      viewSellerProfile,
      handleLogout
    }
  }
}
</script>

<style scoped>
.commodity-detail-page {
  min-height: 100vh;
  background-color: #f5f5f5;
}

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
  background: white;
  border-radius: 8px;
  padding: 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
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
  background: white;
  border-radius: 8px;
  padding: 30px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.commodity-title {
  font-size: 28px;
  font-weight: bold;
  margin-bottom: 20px;
  color: #333;
}

.price-section {
  margin-bottom: 30px;
}

.price {
  font-size: 32px;
  font-weight: bold;
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
  font-weight: 500;
  color: #666;
  width: 80px;
}

.value {
  color: #333;
}

.description-section {
  margin-bottom: 30px;
}

.description-section h3 {
  font-size: 18px;
  font-weight: 600;
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
  font-weight: 600;
}

.contact-btn {
  flex: 1;
  height: 50px;
  font-size: 16px;
}

.seller-section {
  margin-bottom: 40px;
}

.seller-card {
  background: white;
  border-radius: 8px;
  padding: 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
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
  font-weight: 600;
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
  font-weight: 600;
}

.seller-actions {
  display: flex;
  gap: 10px;
}

.related-section h2 {
  font-size: 24px;
  font-weight: bold;
  margin-bottom: 20px;
  color: #333;
}

.related-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 20px;
}

.related-card {
  background: white;
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  cursor: pointer;
  transition: transform 0.3s ease;
}

.related-card:hover {
  transform: translateY(-2px);
}

.related-image {
  width: 100%;
  height: 150px;
  overflow: hidden;
}

.related-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.related-info {
  padding: 15px;
}

.related-info h4 {
  font-size: 14px;
  font-weight: 600;
  margin-bottom: 8px;
  color: #333;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.related-price {
  font-size: 16px;
  font-weight: bold;
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
  font-weight: 600;
}

.buy-info .price {
  font-size: 18px;
  font-weight: bold;
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
  font-weight: bold;
  padding-top: 10px;
  border-top: 1px solid #e0e0e0;
}

.total-price {
  color: var(--primary-color);
}

@media (max-width: 768px) {
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
    flex-direction: column;
  }
  
  .seller-card {
    flex-direction: column;
    gap: 20px;
    text-align: center;
  }
  
  .related-grid {
    grid-template-columns: repeat(auto-fill, minmax(150px, 1fr));
    gap: 15px;
  }
}
</style>
