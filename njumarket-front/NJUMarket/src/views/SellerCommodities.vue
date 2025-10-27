<template>
  <div class="seller-commodities-page" @click="handlePageClick">
    <!-- 商品内容 -->
    <div class="commodities-content">
      <div class="container">
        <!-- 卖家信息展示（和 UserProfile 一样的信息） -->
        <div class="seller-profile-section" v-loading="profileLoading">
          <div class="profile-main">
            <div class="pill-avatar">
              <el-avatar :size="200" :src="getAvatarUrl(sellerProfile?.avatar)">
                {{ sellerProfile?.nickname?.charAt(0) || 'U' }}
              </el-avatar>
            </div>
            <div class="pill-info">
              <h1 class="pill-username">{{ sellerProfile?.nickname || '卖家' }}</h1>
              <div class="pill-user-id">用户ID: {{ sellerId }}</div>
              <span class="pill-vip">{{ sellerProfile?.vipLevel || '普通' }}</span>
            </div>
          </div>
          
          <!-- 其余信息 - 在下方居中且同行分布 -->
          <div class="profile-details">
            <div class="stat-item">
              <span class="stat-label">信用分：</span>
              <span class="stat-value">{{ sellerProfile?.creditScore || 0 }}</span>
            </div>
            <div class="stat-item">
              <span class="stat-label">买家评分：</span>
              <span class="stat-value">{{ sellerProfile?.buyerRating || 0 }}</span>
            </div>
            <div class="stat-item">
              <span class="stat-label">卖家评分：</span>
              <span class="stat-value">{{ sellerProfile?.sellerRating || 0 }}</span>
            </div>
            <div class="stat-item register-time">
              注册时间：{{ formatTime(sellerProfile?.userInfo?.registerTime) }}
            </div>
          </div>
        </div>

        <!-- 商品状态筛选 -->
        <div class="commodity-tabs">
          <SafeTabs 
            v-model="activeTab" 
            @tab-change="handleTabChange"
          >
            <el-tab-pane label="全部" name="all"></el-tab-pane>
            <el-tab-pane label="已发布" name="PUBLISHED"></el-tab-pane>
            <el-tab-pane label="已上架" name="ON_SHELF"></el-tab-pane>
            <el-tab-pane label="已下架" name="OFF_SHELF"></el-tab-pane>
          </SafeTabs>
        </div>

        <!-- 商品列表 -->
        <div class="commodities-list" v-loading="loading">
          <div v-if="commodities.length === 0 && !loading" class="empty-state">
            <el-empty :description="getEmptyDescription()" />
          </div>

          <CommodityCard
            v-for="commodity in commodities"
            :key="commodity.commodityId"
            :commodity="commodity"
            :show-seller-info="false"
            @click="handleCommodityClick"
          />
        </div>

        <!-- 分页 -->
        <div class="pagination-wrapper" v-if="total > 0">
          <div class="pagination-content">
            <!-- 总数显示 -->
            <span class="pagination-total">共 {{ total }} 条</span>
            
            <!-- 每页显示数量选择器 -->
            <div class="custom-page-size-select" @click.stop="togglePageSizeSelect">
              <span class="select-label">每页</span>
              <span class="select-value">{{ pageSize }} 条</span>
              <el-icon class="select-icon"><ArrowDown /></el-icon>
              
              <!-- 弹出式选择器 -->
              <div v-if="showPageSizeSelect" class="select-popup">
                <div class="popup-option" @click="selectPageSize(10)">10 条/页</div>
                <div class="popup-option" @click="selectPageSize(20)">20 条/页</div>
                <div class="popup-option" @click="selectPageSize(50)">50 条/页</div>
              </div>
            </div>
            
            <!-- 翻页按钮 -->
            <div class="pagination-buttons">
              <button 
                class="page-btn" 
                :disabled="currentPage <= 1"
                @click="handleCurrentChange(currentPage - 1)"
              >
                上一页
              </button>
              
              <div class="page-numbers">
                <button 
                  v-for="page in getPageNumbers()" 
                  :key="page"
                  class="page-number"
                  :class="{ active: page === currentPage }"
                  @click="page !== '...' && handleCurrentChange(page)"
                  :disabled="page === '...'"
                >
                  {{ page }}
                </button>
              </div>
              
              <button 
                class="page-btn" 
                :disabled="currentPage >= getTotalPages()"
                @click="handleCurrentChange(currentPage + 1)"
              >
                下一页
              </button>
            </div>
            
            <!-- 跳转输入框 -->
            <div class="page-jumper" @click.stop>
              <span class="jumper-label">跳至</span>
              <input 
                v-model="jumpPage" 
                type="number" 
                class="jumper-input"
                :min="1" 
                :max="getTotalPages()"
                @keyup.enter="handleJump"
                @click.stop
              />
              <span class="jumper-label">页</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { ref, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { commodityAPI, profileAPI, imageAPI } from '../api'
import { ElMessage } from 'element-plus'
import SafeTabs from '../components/SafeTabs.vue'
import CommodityCard from '../components/commodity/CommodityCard.vue'
import { ArrowDown } from '@element-plus/icons-vue'
import '../styles/pagination.css'

export default {
  name: 'SellerCommodities',
  components: {
    SafeTabs,
    CommodityCard,
    ArrowDown
  },
  setup() {
    const route = useRoute()
    const router = useRouter()
    
    const sellerId = ref(route.params.sellerId)
    const loading = ref(false)
    const profileLoading = ref(false)
    const commodities = ref([])
    const sellerProfile = ref(null)
    const total = ref(0)
    const currentPage = ref(1)
    const pageSize = ref(10)
    const activeTab = ref('all')
    const isMounted = ref(false)
    
    // 弹出式选择器状态
    const showPageSizeSelect = ref(false)
    
    // 跳转页面输入
    const jumpPage = ref('')
    
    // 获取卖家信息
    const fetchSellerProfile = async () => {
      if (!sellerId.value) {
        return
      }
      
      profileLoading.value = true
      try {
        const response = await profileAPI.getUser(sellerId.value)
        if (response.success) {
          sellerProfile.value = response.data
        }
      } catch (error) {
        console.error('获取卖家信息失败:', error)
      } finally {
        profileLoading.value = false
      }
    }
    
    // 获取商品列表
    const fetchCommodities = async () => {
      if (!sellerId.value) {
        return
      }
      
      loading.value = true
      try {
        const params = {
          page: currentPage.value,
          size: pageSize.value
        }
        
        if (activeTab.value !== 'all') {
          params.status = activeTab.value
        }
        
        const response = await commodityAPI.getSellerCommodities(sellerId.value, params.page, params.size, params.status)
        if (response.success) {
          commodities.value = response.data.commodities || []
          total.value = response.data.total || 0
          currentPage.value = response.data.current || 1
          pageSize.value = response.data.size || 10
        }
      } catch (error) {
        ElMessage.error('获取商品列表失败')
      } finally {
        loading.value = false
      }
    }
    
    // 标签页切换
    const handleTabChange = (tabName) => {
      activeTab.value = tabName
      currentPage.value = 1
      fetchCommodities()
    }
    
    // 当前页改变
    const handleCurrentChange = (val) => {
      currentPage.value = val
      fetchCommodities()
    }
    
    // 每页显示数量选择器相关方法
    const togglePageSizeSelect = () => {
      showPageSizeSelect.value = !showPageSizeSelect.value
    }
    
    const selectPageSize = (size) => {
      pageSize.value = size
      currentPage.value = 1
      showPageSizeSelect.value = false
      fetchCommodities()
    }
    
    // 分页相关方法
    const getTotalPages = () => {
      return Math.ceil(total.value / pageSize.value)
    }
    
    const getPageNumbers = () => {
      const totalPages = getTotalPages()
      const current = currentPage.value
      const pages = []
      
      if (totalPages <= 7) {
        for (let i = 1; i <= totalPages; i++) {
          pages.push(i)
        }
      } else {
        if (current <= 4) {
          for (let i = 1; i <= 5; i++) {
            pages.push(i)
          }
          pages.push('...')
          pages.push(totalPages)
        } else if (current >= totalPages - 3) {
          pages.push(1)
          pages.push('...')
          for (let i = totalPages - 4; i <= totalPages; i++) {
            pages.push(i)
          }
        } else {
          pages.push(1)
          pages.push('...')
          for (let i = current - 1; i <= current + 1; i++) {
            pages.push(i)
          }
          pages.push('...')
          pages.push(totalPages)
        }
      }
      
      return pages
    }
    
    const handleJump = () => {
      const page = parseInt(jumpPage.value)
      const totalPages = getTotalPages()
      
      if (!jumpPage.value || isNaN(page)) {
        ElMessage.warning('请输入有效的页码')
        jumpPage.value = ''
        return
      }
      
      if (page < 1) {
        ElMessage.warning('页码不能小于1')
        jumpPage.value = ''
        return
      }
      
      if (page > totalPages) {
        ElMessage.warning(`页码不能大于总页数 ${totalPages}`)
        jumpPage.value = ''
        return
      }
      
      handleCurrentChange(page)
      jumpPage.value = ''
    }
    
    // 处理页面点击事件
    const handlePageClick = () => {
      // 关闭所有选择器
      showPageSizeSelect.value = false
      
      // 如果跳转输入框有内容，执行跳转
      if (jumpPage.value && String(jumpPage.value).trim() !== '') {
        handleJump()
      }
    }
    
    // 商品点击处理
    const handleCommodityClick = (commodity) => {
      router.push(`/commodity/${commodity.commodityId}`)
    }
    
    // 格式化时间
    const formatTime = (time) => {
      if (!time) return ''
      return new Date(time).toLocaleString()
    }
    
    // 获取头像URL
    const getAvatarUrl = (avatarUrl) => {
      if (!avatarUrl) return imageAPI.getDefaultAvatar()
      
      // 如果已经是完整URL，直接返回
      if (avatarUrl.startsWith('http')) return avatarUrl
      
      // 如果是文件名，构建完整URL
      if (avatarUrl.includes('/')) return avatarUrl
      
      // 从URL中提取文件名
      const fileName = avatarUrl.split('/').pop()
      return imageAPI.getAvatar(fileName)
    }
    
    // 获取空状态描述
    const getEmptyDescription = () => {
      const statusMap = {
        'all': '暂无商品',
        'PUBLISHED': '暂无已发布商品',
        'ON_SHELF': '暂无已上架商品',
        'OFF_SHELF': '暂无已下架商品'
      }
      return statusMap[activeTab.value] || '暂无商品'
    }
    
    onMounted(() => {
      isMounted.value = true
      fetchSellerProfile()
      fetchCommodities()
    })
    
    // 组件卸载时清理
    onUnmounted(() => {
      isMounted.value = false
    })
    
    return {
      sellerId,
      loading,
      profileLoading,
      commodities,
      sellerProfile,
      total,
      currentPage,
      pageSize,
      activeTab,
      showPageSizeSelect,
      jumpPage,
      handleTabChange,
      handleCurrentChange,
      togglePageSizeSelect,
      selectPageSize,
      getTotalPages,
      getPageNumbers,
      handleJump,
      handleCommodityClick,
      getEmptyDescription,
      formatTime,
      getAvatarUrl,
      handlePageClick
    }
  }
}
</script>

<style scoped>
.seller-commodities-page {
  min-height: 100vh;
  background-color: #f5f5f5;
}

.commodities-content {
  padding: 40px 0;
}

/* 卖家信息展示样式（和 UserProfile 一致） */
.seller-profile-section {
  background: transparent;
  border: none;
  box-shadow: none;
  border-radius: 0;
  padding: 0;
  margin-bottom: 50px;
  display: flex;
  flex-direction: column;
  gap: 100px;
  align-items: center;
}

.profile-main {
  display: flex;
  align-items: center;
  gap: 40px;
  padding: 0;
  background: transparent;
  border: none;
  width: fit-content;
  margin: 50px auto 0;
}

.pill-avatar {
  flex-shrink: 0;
}

.pill-avatar :deep(.el-avatar) {
  border-radius: 50%;
}

.profile-main :deep(.el-avatar__inner) {
  border-radius: 50%;
}

.pill-info {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.pill-username {
  font-size: 60px;
  font-weight: normal;
  margin: 0;
  margin-left: 20px;
  color: var(--primary-color);
}

.pill-user-id {
  font-size: 16px;
  color: #999;
  margin-left: 20px;
  margin-top: 5px;
}

.pill-vip {
  background: var(--primary-color);
  color: white;
  padding: 5px 20px;
  border-radius: 30px;
  font-size: 20px;
  font-weight: normal;
  width: fit-content;
  margin-left: 20px;
  margin-right: 20px;
}

.profile-details {
  display: flex;
  flex-direction: row;
  gap: 30px;
  justify-content: center;
  align-items: center;
  flex-wrap: wrap;
}

.stat-item {
  display: flex;
  align-items: center;
  gap: 8px;
  white-space: nowrap;
}

.stat-item.register-time {
  color: var(--primary-color);
  font-size: 18px;
}

.stat-item .stat-label {
  color: var(--primary-color);
  font-size: 18px;
}

.stat-item .stat-value {
  color: var(--primary-color);
  font-weight: normal;
  font-size: 22px;
}

.commodity-tabs {
  background: transparent;
  border-radius: 16px;
  padding: 0;
  margin-bottom: 24px;
  border: none;
  box-shadow: none;
}

.commodity-tabs :deep(.el-tabs__header) {
  background: white;
  border-radius: 28.3px;
  padding: 8px;
  border: 1px solid var(--primary-color);
  margin-bottom: 0;
}

.commodity-tabs :deep(.el-tabs__nav-wrap::after) {
  display: none;
}

.commodity-tabs :deep(.el-tabs__item) {
  border-radius: 20px;
  font-weight: normal;
  color: var(--primary-color);
  border: none;
  padding: 8px 20px !important;
  margin: 0 5px;
  transition: all 0.3s ease;
}

.commodity-tabs :deep(.el-tabs__item:first-child) {
  padding: 8px 20px !important;
  margin-left: 0;
  margin-right: 5px;
}

.commodity-tabs :deep(.el-tabs__item:last-child) {
  padding: 8px 20px !important;
  margin-left: 5px;
  margin-right: 0;
}

.commodity-tabs :deep(.el-tabs__item:hover) {
  background-color: rgba(106, 1, 94, 0.1);
}

.commodity-tabs :deep(.el-tabs__item.is-active) {
  background-color: var(--primary-color);
  color: white;
}

.commodity-tabs :deep(.el-tabs__active-bar) {
  display: none;
}

.commodities-list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 20px;
  margin-bottom: 30px;
  padding: 20px 0;
}

.empty-state {
  grid-column: 1 / -1;
  text-align: center;
  padding: 60px 0;
}

@media (min-width: 1600px) {
  .commodities-list {
    gap: 24px;
  }
}

@media (max-width: 900px) {
  .commodities-list {
    grid-template-columns: repeat(2, 1fr);
    gap: 12px;
    padding: 16px 0;
  }
  
  .profile-main {
    flex-direction: column;
    gap: 4vw;
    margin: 3vw auto 8vw;
    align-items: center;
  }
  
  .pill-info {
    align-items: center;
  }
  
  .pill-username {
    font-size: 12vw;
    margin-left: 0 !important;
    text-align: center;
  }
  
  .pill-user-id {
    font-size: 3vw;
    margin-left: 0 !important;
    text-align: center;
  }
  
  .pill-vip {
    font-size: 2.5vw;
    padding: 1vw 5vw;
    border-radius: 999px;
    margin-left: 0 !important;
    margin-right: 0 !important;
    margin-top: 1vw;
  }
  
  .profile-details {
    margin-top: 8vw !important;
    margin-bottom: 8vw !important;
  }
  
  .seller-profile-section {
    gap: 20px;
  }
}
</style>

