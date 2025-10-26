<template>
  <div class="commodity-list-page" @click="handlePageClick">
    <!-- 搜索和筛选区域 -->
    <div class="search-filter-section">
      <div class="container">
        <!-- 搜索框区域 -->
        <div class="search-box">
          <div class="search-form">
            <div class="fake-search-box">
              <input
                v-model="searchKeyword"
                placeholder="搜索商品..."
                class="transparent-input"
                @keyup.enter="handleSearch"
              />
              <button class="search-button" @click="handleSearch">
                <el-icon><Search /></el-icon>
              </button>
            </div>
          </div>
        </div>
        
        <!-- 筛选区域 -->
        <div class="filter-section">
          <div class="filter-group">
            <div class="custom-select" @click.stop="toggleCategorySelect">
              <span class="select-label">商品种类</span>
              <span class="select-value">{{ selectedCategory || '' }}</span>
              <el-icon class="select-icon"><ArrowDown /></el-icon>
              
              <!-- 弹出式选择器 -->
              <div v-if="showCategorySelect" class="select-popup">
                <div class="popup-option" @click="selectCategory('')">全部分类</div>
                <div 
                  v-for="category in categories"
                  :key="category"
                  class="popup-option"
                  @click="selectCategory(category)"
                >
                  {{ category }}
                </div>
              </div>
            </div>
          </div>
          
          <div class="filter-group">
            <el-input
              v-model="minPrice"
              placeholder="最低价格"
              type="number"
              class="pill-input"
              @change="handleFilter"
            />
            <span class="price-separator">-</span>
            <el-input
              v-model="maxPrice"
              placeholder="最高价格"
              type="number"
              class="pill-input"
              @change="handleFilter"
            />
          </div>
          
          <div class="filter-group">
            <div class="custom-select" @click.stop="toggleSortSelect">
              <span class="select-label">排序方式</span>
              <span class="select-value">{{ getSortLabel(sortBy) }}</span>
              <el-icon class="select-icon"><ArrowDown /></el-icon>
              
              <!-- 弹出式选择器 -->
              <div v-if="showSortSelect" class="select-popup">
                <div class="popup-option" @click="selectSort('')">默认排序</div>
                <div class="popup-option" @click="selectSort('price_asc')">价格从低到高</div>
                <div class="popup-option" @click="selectSort('price_desc')">价格从高到低</div>
                <div class="popup-option" @click="selectSort('latest')">最新发布</div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 商品列表 -->
    <div class="commodity-list-section">
      <div class="container">
        <div class="list-header">
          <h2 v-if="searchKeyword">搜索结果：{{ searchKeyword }}</h2>
          <h2 v-else-if="selectedCategory">分类：{{ selectedCategory }}</h2>
          <h2 v-else>所有商品</h2>
          <span class="total-count">共 {{ total }} 件商品</span>
        </div>
        
        <div class="commodity-grid" v-loading="loading">
          <div
            v-for="commodity in commodities"
            :key="commodity.commodityId"
            class="commodity-card"
            @click="$router.push(`/commodity/${commodity.commodityId}`)"
          >
            <!-- 商品图片 -->
            <div class="commodity-image">
              <img 
                v-if="commodity.images && commodity.images.length > 0"
                :src="getCommodityImageUrl(commodity.images[0])" 
                :alt="commodity.title"
                @error="handleImageError"
              />
              <div v-else class="no-image">
                <span>暂无照片</span>
              </div>
            </div>
            
            <!-- 商品信息 -->
            <div class="commodity-info">
              <!-- 药丸型标签 -->
              <div class="commodity-tags">
                <span class="pill-tag">{{ commodity.category }}</span>
                <span class="pill-tag">{{ commodity.conditionLevel }}</span>
                <span v-if="commodity.location" class="pill-tag">{{ commodity.location }}</span>
              </div>
              
              <!-- 标题 -->
              <h3 class="commodity-title">{{ commodity.title }}</h3>
              
              <!-- 描述 -->
              <p class="commodity-description">{{ commodity.description }}</p>
              
              <!-- 价格 -->
              <p class="commodity-price">¥{{ commodity.price }}</p>
              
              <!-- 卖家信息（右下角） -->
              <div class="seller-info-card">
                <div v-if="getSellerInfo(commodity.commodityId)" class="seller-content">
                  <img 
                    v-if="getSellerInfo(commodity.commodityId)?.avatar" 
                    :src="getAvatarUrl(getSellerInfo(commodity.commodityId).avatar)" 
                    :alt="getSellerInfo(commodity.commodityId).nickname"
                    class="seller-avatar"
                  />
                  <div v-else class="seller-avatar default-avatar">
                    {{ getSellerInfo(commodity.commodityId).nickname?.charAt(0) || 'S' }}
                  </div>
                  <span class="seller-name">{{ getSellerInfo(commodity.commodityId).nickname || '卖家' }}</span>
                </div>
                <div v-else-if="isSellerLoading(commodity.commodityId)" class="seller-loading">
                  <div class="seller-avatar skeleton"></div>
                  <div class="seller-name skeleton"></div>
                </div>
                <div v-else class="seller-content">
                  <div class="seller-avatar default-avatar">S</div>
                  <span class="seller-name">卖家</span>
                </div>
              </div>
            </div>
          </div>
        </div>
        
        <!-- 空状态 -->
        <div v-if="!loading && commodities.length === 0" class="empty-state">
          <el-empty description="暂无商品">
            <el-button type="primary" @click="$router.push('/publish')" v-if="isLoggedIn">
              发布商品
            </el-button>
          </el-empty>
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
                <!--div class="popup-option" @click="selectPageSize(1)">1 条/页</div -->
                <div class="popup-option" @click="selectPageSize(10)">10 条/页</div>
                <div class="popup-option" @click="selectPageSize(20)">20 条/页</div>
                <div class="popup-option" @click="selectPageSize(50)">50 条/页</div>
                <div class="popup-option" @click="selectPageSize(100)">100 条/页</div>
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
                  @click="handleCurrentChange(page)"
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
import { ref, onMounted, computed, watch } from 'vue'
import { useRoute } from 'vue-router'
import { useUserStore } from '../stores/user'
import { commodityAPI, imageAPI, profileAPI } from '../api'
import { ElMessage } from 'element-plus'
//import { ArrowDown } from '@element-plus/icons-vue'
//import { User } from '@element-plus/icons-vue'
//import { Search } from '@element-plus/icons-vue'

export default {
  name: 'CommodityList',
  setup() {
    const route = useRoute()
    const userStore = useUserStore()
    
    const loading = ref(false)
    const commodities = ref([])
    const categories = ref([])
    const total = ref(0)
    const currentPage = ref(1)
    const pageSize = ref(20)
    
    const searchKeyword = ref('')
    const selectedCategory = ref('')
    const minPrice = ref('')
    const maxPrice = ref('')
    const sortBy = ref('')
    
    // 弹出式选择器状态
    const showCategorySelect = ref(false)
    const showSortSelect = ref(false)
    const showPageSizeSelect = ref(false)
    
    // 跳转页面输入
    const jumpPage = ref('')
    
    // 卖家信息管理
    const sellerInfoMap = ref(new Map())
    const sellerLoadingMap = ref(new Map())
    
    const isLoggedIn = computed(() => userStore.isLoggedIn)
    const user = computed(() => userStore.user || {})
    
    // 安全获取用户信息
    const getUserDisplayName = computed(() => {
      return user.value?.nickname || user.value?.username || '用户'
    })
    
    // 获取商品列表
    const fetchCommodities = async () => {
      loading.value = true
      try {
        const params = {
          page: currentPage.value,
          size: pageSize.value
        }
        
        if (searchKeyword.value) {
          params.keyword = searchKeyword.value
        }
        if (selectedCategory.value) {
          params.category = selectedCategory.value
        }
        if (minPrice.value) {
          params.minPrice = parseFloat(minPrice.value)
        }
        if (maxPrice.value) {
          params.maxPrice = parseFloat(maxPrice.value)
        }
        if (sortBy.value) {
          params.sortBy = sortBy.value
        }
        
        const response = await commodityAPI.search(params)
        if (response.success) {
          // 修复：正确映射后端返回的数据结构
          commodities.value = response.data.commodities || []
          total.value = response.data.total || 0
          currentPage.value = response.data.current || 1
          pageSize.value = response.data.size || 20
          
          // 查询卖家信息
          commodities.value.forEach(commodity => {
            fetchSellerInfo(commodity)
          })
        }
      } catch (error) {
        ElMessage.error('获取商品列表失败')
      } finally {
        loading.value = false
      }
    }
    
    // 获取分类列表
    const fetchCategories = async () => {
      try {
        const response = await commodityAPI.getCategories()
        if (response.success) {
          categories.value = response.data
        }
      } catch (error) {
        console.error('获取分类失败:', error)
      }
    }
    
    // 搜索
    const handleSearch = () => {
      currentPage.value = 1
      fetchCommodities()
    }
    
    // 筛选
    const handleFilter = () => {
      currentPage.value = 1
      fetchCommodities()
    }
    
    // 弹出式选择器方法
    const toggleCategorySelect = () => {
      showCategorySelect.value = !showCategorySelect.value
      showSortSelect.value = false // 关闭其他选择器
    }
    
    const toggleSortSelect = () => {
      showSortSelect.value = !showSortSelect.value
      showCategorySelect.value = false // 关闭其他选择器
    }
    
    const selectCategory = (category) => {
      selectedCategory.value = category
      showCategorySelect.value = false
      handleFilter()
    }
    
    const selectSort = (sort) => {
      sortBy.value = sort
      showSortSelect.value = false
      handleFilter()
    }
    
    const getSortLabel = (sort) => {
      const sortLabels = {
        '': '',
        'price_asc': '价格从低到高',
        'price_desc': '价格从高到低',
        'latest': '最新发布'
      }
      return sortLabels[sort] || ''
    }
    
    // 点击外部关闭选择器
    const closeSelects = () => {
      showCategorySelect.value = false
      showSortSelect.value = false
      showPageSizeSelect.value = false
    }
    
    // 处理页面点击事件
    const handlePageClick = () => {
      // 关闭所有选择器
      closeSelects()
      
      // 如果跳转输入框有内容，执行跳转
      if (jumpPage.value && String(jumpPage.value).trim() !== '') {
        handleJump()
      }
    }
    
    // 每页显示数量选择器相关方法
    const togglePageSizeSelect = () => {
      showPageSizeSelect.value = !showPageSizeSelect.value
      showCategorySelect.value = false
      showSortSelect.value = false
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
    
    // 卖家信息查询相关方法
    const getSellerInfo = (commodityId) => {
      return sellerInfoMap.value.get(commodityId)
    }
    
    const isSellerLoading = (commodityId) => {
      return sellerLoadingMap.value.get(commodityId) || false
    }
    
    const fetchSellerInfo = async (commodity) => {
      if (!commodity.sellerId) {
        return
      }
      
      const commodityId = commodity.commodityId
      
      // 如果已经查询过，直接返回
      if (sellerInfoMap.value.has(commodityId)) {
        return
      }
      
      try {
        sellerLoadingMap.value.set(commodityId, true)
        const response = await profileAPI.getUser(commodity.sellerId)
        if (response.success) {
          sellerInfoMap.value.set(commodityId, response.data)
        }
      } catch (error) {
        console.error('获取卖家信息失败:', error)
      } finally {
        sellerLoadingMap.value.set(commodityId, false)
      }
    }
    
    const getAvatarUrl = (avatar) => {
      if (!avatar) return ''
      
      // 如果已经是完整URL，直接返回
      if (avatar.startsWith('http')) return avatar
      
      // 构建头像URL
      return imageAPI.getAvatar(avatar)
    }
    
    // 分页大小改变
    const handleSizeChange = (val) => {
      pageSize.value = val
      currentPage.value = 1
      fetchCommodities()
    }
    
    // 当前页改变
    const handleCurrentChange = (val) => {
      currentPage.value = val
      fetchCommodities()
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
    
    // 登出
    const handleLogout = async () => {
      try {
        await userStore.logout()
        // userStore.logout()会处理跳转，不需要额外的跳转和消息
      } catch (error) {
        ElMessage.error('退出登录失败')
      }
    }
    
    // 监听路由参数变化
    watch(() => route.query, (newQuery) => {
      if (newQuery.keyword) {
        searchKeyword.value = newQuery.keyword
      }
      if (newQuery.category) {
        selectedCategory.value = newQuery.category
      }
      fetchCommodities()
    }, { immediate: true })
    
    onMounted(() => {
      fetchCategories()
    })
    
    return {
      loading,
      commodities,
      categories,
      total,
      currentPage,
      pageSize,
      searchKeyword,
      selectedCategory,
      minPrice,
      maxPrice,
      sortBy,
      showCategorySelect,
      showSortSelect,
      showPageSizeSelect,
      jumpPage,
      isLoggedIn,
      user,
      getUserDisplayName,
      handleSearch,
      handleFilter,
      toggleCategorySelect,
      toggleSortSelect,
      togglePageSizeSelect,
      selectCategory,
      selectSort,
      selectPageSize,
      getSortLabel,
      getTotalPages,
      getPageNumbers,
      handleJump,
      handlePageClick,
      closeSelects,
      getSellerInfo,
      isSellerLoading,
      getAvatarUrl,
      handleSizeChange,
      handleCurrentChange,
      handleImageError,
      getCommodityImageUrl,
      handleLogout
    }
  }
}
</script>

<style scoped>
.commodity-list-page {
  min-height: 100vh;
  background-color: #f5f5f5;
}

.search-filter-section {
  background: white;
  padding: 40px 0; /* 增加间距以匹配主页设计 */
  border-bottom: 1px solid #e0e0e0;
}

/* 搜索框区域 */
.search-box {
  margin-bottom: 32px;
  text-align: center;
}

.search-form {
  max-width: 600px;
  margin: 0 auto;
}

.fake-search-box {
  position: relative;
  width: 100%;
  height: 50px;
  background: white;
  border: 1px solid var(--primary-color);
  border-radius: 25px;
  display: flex;
  align-items: center;
  padding: 0 25px;
  box-shadow: 0 2px 8px rgba(106, 1, 94, 0.1);
}

.transparent-input {
  flex: 1;
  border: none;
  outline: none;
  background: transparent;
  font-size: 16px;
  color: var(--text-primary);
  padding: 0;
  margin: 0;
  margin-right: 50px;
}

.transparent-input::placeholder {
  color: var(--text-light);
}

.search-button {
  position: absolute;
  right: 5px;
  top: 50%;
  transform: translateY(-50%);
  width: 40px;
  height: 40px;
  background: var(--primary-color);
  border: none;
  border-radius: 50%;
  color: white;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.3s ease;
}

.search-button:hover {
  background: var(--primary-light);
}

.search-button:active {
  transform: translateY(-50%) scale(0.95);
}

/* 筛选区域 */
.filter-section {
  display: flex;
  gap: 20px;
  flex-wrap: wrap;
  justify-content: center;
}

.filter-group {
  display: flex;
  align-items: center;
  gap: 10px;
}

.price-separator {
  color: #999;
  font-weight: normal;
}

/* 带标签的选择器样式 */
.select-label {
  color: var(--primary-color);
  font-size: 14px;
  font-weight: normal;
  margin-left: 8px;
  white-space: nowrap;
}

.pill-select.with-label :deep(.el-select__placeholder) {
  color: var(--text-light);
  font-weight: normal;
}

.pill-select.with-label :deep(.el-select__selected-item) {
  color: var(--text-primary);
  font-weight: normal;
}

/* 价格输入框内容右移，解决光标掩盖问题 */
.filter-group .pill-input :deep(.el-input__inner) {
  padding-left: 12px; /* 增加左边距，让内容右移 */
}

/* 自定义弹出式选择器样式 */
.custom-select {
  position: relative;
  display: flex;
  align-items: center;
  padding: 8px 16px;
  background: white;
  border: 1px solid var(--primary-color);
  border-radius: 20px;
  cursor: pointer;
  transition: all 0.3s ease;
  min-width: 150px;
}

.custom-select:hover {
  border-color: var(--primary-light);
  box-shadow: 0 4px 12px rgba(106, 1, 94, 0.15);
}

.custom-select .select-label {
  color: var(--primary-color);
  font-size: 14px;
  font-weight: normal;
  margin-right: 8px;
  white-space: nowrap;
}

.select-value {
  flex: 1;
  color: var(--text-primary);
  font-size: 14px;
  font-weight: normal;
  min-height: 20px;
  display: flex;
  align-items: center;
}

.select-icon {
  color: var(--primary-color);
  font-size: 12px;
  margin-left: 8px;
  transition: transform 0.3s ease;
}

.custom-select:hover .select-icon {
  transform: rotate(180deg);
}

/* 弹出式选择器 */
.select-popup {
  position: absolute;
  top: 100%;
  left: 0;
  right: 0;
  background: white;
  border: 1px solid var(--primary-color);
  border-radius: 16px;
  box-shadow: 0 4px 16px rgba(106, 1, 94, 0.2);
  z-index: 1000;
  margin-top: 4px;
  padding: 8px;
}

.popup-option {
  padding: 8px 12px;
  margin: 2px 0;
  border-radius: 20px;
  color: var(--primary-color);
  font-size: 14px;
  font-weight: normal;
  cursor: pointer;
  transition: all 0.3s ease;
  text-align: center;
}

.popup-option:hover {
  background-color: var(--primary-color);
  color: white;
}

/* 药丸型输入框和选择器样式 */
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
}

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

.commodity-list-section {
  padding: 30px 0;
}

.list-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 30px;
}

.list-header h2 {
  font-size: 24px;
  font-weight: normal;
  color: #333;
  margin: 0;
}

.total-count {
  color: #999;
  font-size: 14px;
}

.commodity-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 20px;
  margin-bottom: 40px;
}

.commodity-card {
  background: white;
  border-radius: 16px; /* 使用主页的圆角设计 */
  overflow: hidden;
  border: none; /* 移除边框 */
  box-shadow: none; /* 移除阴影，使用无框设计 */
  cursor: pointer;
  transition: transform 0.3s ease, box-shadow 0.3s ease;
}

.commodity-card:hover {
  transform: translateY(-2px); /* 使用主页的悬停效果 */
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.08); /* 使用主页的阴影设计 */
}

.commodity-card {
  background: transparent; /* 透明外层卡片 */
  border-radius: 16px;
  overflow: hidden;
  border: none;
  box-shadow: none;
  cursor: pointer;
  transition: transform 0.3s ease, box-shadow 0.3s ease;
  position: relative;
}

.commodity-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.08);
}

.commodity-image {
  width: 100%;
  height: 160px;
  overflow: hidden;
  border-radius: 12px;
  margin-bottom: 12px;
}

.commodity-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.no-image {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: #f5f5f5;
  color: #999;
  font-size: 14px;
}

.commodity-info {
  padding: 0;
  position: relative;
  min-height: 140px;
}

/* 药丸型标签 */
.commodity-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-bottom: 8px;
}

.pill-tag {
  background-color: var(--primary-color);
  color: white;
  padding: 4px 8px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: normal;
  white-space: nowrap;
}

/* 标题 */
.commodity-title {
  font-size: 25px;
  font-weight: normal;
  color: var(--primary-color);
  margin: 0 0 6px 0;
  line-height: 1.4;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

/* 描述 */
.commodity-description {
  font-size: 12px;
  color: var(--primary-color);
  margin: 0 0 8px 0;
  line-height: 1.4;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

/* 价格 */
.commodity-price {
  font-size: 30px;
  font-weight: normal;
  color: var(--primary-color);
  margin: 0 0 8px 0;
}

/* 卖家信息卡片（右下角） */
.seller-info-card {
  position: absolute;
  bottom: 0;
  right: 0;
  background: transparent;
  border-radius: 12px;
  padding: 8px 12px;
  display: flex;
  align-items: center;
  justify-content: flex-start;
  gap: 8px;
  min-height: 48px;
  height: auto;
}

.seller-avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  object-fit: cover;
}

.default-avatar {
  background-color: #f0f0f0;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #999;
  font-size: 14px;
  font-weight: normal;
}

.seller-content {
  display: flex;
  align-items: center;
  gap: 8px;
  height: 100%;
}

.seller-name {
  font-size: 20px;
  color: var(--primary-color);
  /* margin-left: 10px; */
  font-weight: normal;
  line-height: 1;
  display: flex;
  align-items: center;
  height: 100%;
}

/* 卖家信息加载状态 */
.seller-loading {
  display: flex;
  align-items: center;
  gap: 6px;
}

.skeleton {
  background: linear-gradient(90deg, #f0f0f0 25%, #e0e0e0 50%, #f0f0f0 75%);
  background-size: 200% 100%;
  animation: loading 1.5s infinite;
}

.seller-loading .seller-avatar.skeleton {
  width: 32px;
  height: 32px;
  border-radius: 50%;
}

.seller-loading .seller-name.skeleton {
  width: 80px;
  height: 14px;
  border-radius: 7px;
}

@keyframes loading {
  0% {
    background-position: 200% 0;
  }
  100% {
    background-position: -200% 0;
  }
}

.category-tag {
  background: rgba(106, 1, 94, 0.1);
  color: var(--primary-color);
}

.condition-tag {
  background: rgba(0, 0, 0, 0.1);
  color: #666;
}

.empty-state {
  text-align: center;
  padding: 60px 0;
}

.pagination-wrapper {
  display: flex;
  justify-content: center;
  margin-top: 40px;
}

.pagination-content {
  display: flex;
  align-items: center;
  gap: 20px;
  flex-wrap: wrap;
  justify-content: center;
}

.pagination-total {
  color: var(--primary-color);
  font-size: 14px;
  font-weight: normal;
}

/* 每页显示数量选择器 */
.custom-page-size-select {
  position: relative;
  display: flex;
  align-items: center;
  padding: 8px 16px;
  background: white;
  border: 1px solid var(--primary-color);
  border-radius: 20px;
  cursor: pointer;
  transition: all 0.3s ease;
  min-width: 120px;
}

.custom-page-size-select:hover {
  border-color: var(--primary-light);
  box-shadow: 0 4px 12px rgba(106, 1, 94, 0.15);
}

.custom-page-size-select .select-label {
  color: var(--primary-color);
  font-size: 14px;
  font-weight: normal;
  margin-right: 8px;
  white-space: nowrap;
}

.custom-page-size-select .select-value {
  flex: 1;
  color: var(--text-primary);
  font-size: 14px;
  font-weight: normal;
  min-height: 20px;
  display: flex;
  align-items: center;
}

.custom-page-size-select .select-icon {
  color: var(--primary-color);
  font-size: 12px;
  margin-left: 8px;
  transition: transform 0.3s ease;
}

.custom-page-size-select:hover .select-icon {
  transform: rotate(180deg);
}

/* 翻页按钮组 */
.pagination-buttons {
  display: flex;
  align-items: center;
  gap: 8px;
}

.page-btn {
  background-color: transparent;
  border: 1px solid var(--primary-color);
  color: var(--primary-color);
  border-radius: 20px;
  padding: 8px 16px;
  font-size: 14px;
  font-weight: normal;
  cursor: pointer;
  transition: all 0.3s ease;
  min-width: 80px;
}

.page-btn:hover:not(:disabled) {
  background-color: var(--primary-color);
  color: white;
  border-color: var(--primary-color);
}

.page-btn:disabled {
  background-color: transparent;
  color: #ccc;
  border-color: #ccc;
  cursor: not-allowed;
}

/* 页码按钮 */
.page-numbers {
  display: flex;
  align-items: center;
  gap: 4px;
}

.page-number {
  background-color: transparent;
  border: 1px solid var(--primary-color);
  color: var(--primary-color);
  border-radius: 20px;
  width: 32px;
  height: 32px;
  font-size: 14px;
  font-weight: normal;
  cursor: pointer;
  transition: all 0.3s ease;
  display: flex;
  align-items: center;
  justify-content: center;
}

.page-number:hover {
  background-color: var(--primary-color);
  color: white;
  border-color: var(--primary-color);
}

.page-number.active {
  background-color: var(--primary-color);
  color: white;
  border-color: var(--primary-color);
}

/* 跳转输入框 */
.page-jumper {
  display: flex;
  align-items: center;
  gap: 8px;
}

.jumper-label {
  color: var(--primary-color);
  font-size: 14px;
  font-weight: normal;
}

.jumper-input {
  background-color: transparent;
  border: 1px solid var(--primary-color);
  color: var(--primary-color);
  border-radius: 20px;
  padding: 8px 12px;
  width: 60px;
  height: 32px;
  font-size: 14px;
  text-align: center;
  transition: all 0.3s ease;
}

.jumper-input:hover,
.jumper-input:focus {
  background-color: var(--primary-color);
  color: white;
  border-color: var(--primary-color);
  outline: none;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .pagination-content {
    flex-direction: column;
    gap: 15px;
  }
  
  .pagination-buttons {
    order: 1;
  }
  
  .custom-page-size-select {
    order: 2;
  }
  
  .page-jumper {
    order: 3;
  }
  
  .pagination-total {
    order: 4;
  }
}

@media (max-width: 768px) {
  .nav-content {
    flex-direction: column;
    gap: 15px;
  }
  
  .nav-menu {
    gap: 20px;
  }
  
  .filter-section {
    flex-direction: column;
    align-items: center;
  }
  
  .commodity-grid {
    grid-template-columns: repeat(auto-fill, minmax(250px, 1fr));
    gap: 15px;
  }
  
  .list-header {
    flex-direction: column;
    gap: 10px;
    text-align: center;
  }
}
</style>

