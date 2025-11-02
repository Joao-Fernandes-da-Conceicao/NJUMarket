<template>
  <div class="commodity-list-page" @click="handlePageClick">
    <!-- 页面标题 -->
    <div class="page-header">
      <h1>商品浏览</h1>
    </div>
    
    <!-- 搜索和筛选区域 -->
    <div class="search-filter-section">
      <div class="container">
        <!-- 搜索框区域 -->
        <SearchBox 
          v-model:keyword="searchKeyword"
          placeholder="搜索商品..."
          @search="handleSearch"
        />
        
        <!-- 筛选区域 -->
        <div class="filter-section">
          <!-- 商品种类选择器 -->
          <UnifiedSelect
            v-model="selectedCategory"
            :options="categoryOptions"
            :placeholder="getCategoryLabel(selectedCategory)"
            @change="() => handleFilter()"
          />
          
          <!-- 价格区间输入框 -->
          <div class="filter-group price-group">
            <UnifiedInput
              v-model="minPrice"
              placeholder="最低价格"
              type="number"
              class="pill-input"
              @change="handleFilter"
            />
            <UnifiedInput
              v-model="maxPrice"
              placeholder="最高价格"
              type="number"
              class="pill-input"
              @change="handleFilter"
            />
          </div>
          
          <!-- 排序方式选择器 -->
          <UnifiedSelect
            v-model="sortBy"
            :options="sortOptions"
            :placeholder="getSortLabel(sortBy)"
            @change="() => handleFilter()"
          />
        </div>
      </div>
    </div>

    <!-- 商品列表 -->
    <div class="commodity-list-section">
      <div class="container">
        <div class="list-header">
          <h2 v-if="displayedSearchKeyword">搜索结果：{{ displayedSearchKeyword }}</h2>
          <h2 v-else-if="selectedCategory">分类：{{ selectedCategory }}</h2>
          <h2 v-else>所有商品</h2>
          <span class="total-count">共 {{ total }} 件商品</span>
        </div>
        
        <CommodityGrid 
          :commodities="commodities"
          :loading="loading"
          :show-seller-info="true"
          card-type="browse"
          empty-text="暂无商品"
          :show-empty-action="true"
          empty-action-text="发布商品"
          @commodity-click="handleCommodityClick"
          @empty-action="handleEmptyAction"
        />
        
        <!-- 分页 -->
        <Pagination 
          :total="total"
          :current-page="currentPage"
          :page-size="pageSize"
          :show-page-numbers="false"
          @page-change="handleCurrentChange"
          @page-size-change="handleSizeChange"
        />
      </div>
    </div>
  </div>
</template>

<script>
import { ref, onMounted, computed, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '../stores/user'
import { commodityAPI, imageAPI } from '../api'
import { ElMessage } from 'element-plus'
import SearchBox from '../components/search/SearchBox.vue'
import CommodityGrid from '../components/commodity/CommodityGrid.vue'
import Pagination from '../components/common/Pagination.vue'
import UnifiedSelect from '../components/common/UnifiedSelect.vue'
import UnifiedInput from '../components/common/UnifiedInput.vue'
//import { ArrowDown } from '@element-plus/icons-vue'
//import { User } from '@element-plus/icons-vue'
//import { Search } from '@element-plus/icons-vue'

export default {
  name: 'CommodityList',
  components: {
    SearchBox,
    CommodityGrid,
    Pagination,
    UnifiedSelect,
    UnifiedInput
  },
  setup() {
    const route = useRoute()
    const router = useRouter()
    const userStore = useUserStore()
    
    const loading = ref(false)
    const commodities = ref([])
    const categories = ref([])
    const total = ref(0)
    const currentPage = ref(1)
    const pageSize = ref(20)
    
    const searchKeyword = ref('')
    const displayedSearchKeyword = ref('') // 用于显示的搜索关键字
    const selectedCategory = ref('')
    const minPrice = ref('')
    const maxPrice = ref('')
    const sortBy = ref('')
    
    // 统一化：组件化选择器的选项
    const categoryOptions = computed(() => {
      const base = [{ label: '全部分类', value: '' }]
      return base.concat((categories.value || []).map(c => ({ label: c, value: c })))
    })
    const sortOptions = [
      { label: '默认排序', value: '' },
      { label: '价格从低到高', value: 'price_asc' },
      { label: '价格从高到低', value: 'price_desc' },
      { label: '最新发布', value: 'latest' }
    ]
    
    const isLoggedIn = computed(() => userStore.isLoggedIn)
    const user = computed(() => userStore.user || {})
    
    // 安全获取用户信息
    const getUserDisplayName = computed(() => {
      return user.value?.nickname || user.value?.username || '用户'
    })
    
    // 商品点击处理
    const handleCommodityClick = (commodity) => {
      router.push(`/commodity/${commodity.commodityId}`)
    }
    
    // 空状态处理
    const handleEmptyAction = () => {
      if (isLoggedIn.value) {
        router.push('/publish')
      } else {
        router.push('/login')
      }
    }
    
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
          
          // ✅ 优化：后端已批量返回卖家信息，无需前端逐个查询
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
      displayedSearchKeyword.value = searchKeyword.value // 点击搜索后才更新显示的关键字
      currentPage.value = 1
      fetchCommodities()
    }
    
    // 筛选
    const handleFilter = () => {
      currentPage.value = 1
      fetchCommodities()
    }
    
    // 旧的弹出式选择器方法已废弃，由 UnifiedSelect 代替
    // 保留标签生成方法
    const getSortLabel = (sort) => {
      const sortLabels = {
        '': '默认排序',
        'price_asc': '价格从低到高',
        'price_desc': '价格从高到低',
        'latest': '最新发布'
      }
      return sortLabels[sort] || '默认排序'
    }
    
    const getCategoryLabel = (category) => {
      return category || '全部分类'
    }
    
    // 处理页面点击事件（已无需要关闭的弹窗，保持空实现）
    const handlePageClick = () => {}
    
    // 分页相关方法 (现在由 Pagination 组件处理)
    const getTotalPages = () => {
      return Math.ceil(total.value / pageSize.value)
    }
    
    // ✅ 优化：后端已批量返回卖家信息，无需前端查询
    // 直接使用 commodity.sellerNickname 和 commodity.sellerAvatar
    
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
        displayedSearchKeyword.value = newQuery.keyword // 从路由参数来的也要更新显示
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
      total,
      currentPage,
      pageSize,
      searchKeyword,
      displayedSearchKeyword,
      selectedCategory,
      minPrice,
      maxPrice,
      sortBy,
      categoryOptions,
      sortOptions,
      isLoggedIn,
      user,
      getUserDisplayName,
      handleSearch,
      handleFilter,
      getSortLabel,
      getCategoryLabel,
      getTotalPages,
      handlePageClick,
      getAvatarUrl,
      handleSizeChange,
      handleCurrentChange,
      handleImageError,
      getCommodityImageUrl,
      handleLogout,
      handleCommodityClick,
      handleEmptyAction
    }
  }
}
</script>

<style scoped>
/* 翻页器样式由 Pagination 组件统一管理 */
.commodity-list-page {
  min-height: 100vh;
  background-color: #f5f5f5;
}

.page-header {
  display: none; /* 默认隐藏 */
  padding: 40px 0 20px 0;
  text-align: center;
}

/* 移动端显示标题 */
@media (max-width: 900px) {
  .page-header {
    display: block;
  }
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

/* 筛选区域 - 桌面端：横向排列，顺序为：种类-价格-排序 */
.filter-section {
  display: flex;
  gap: 20px;
  flex-wrap: wrap;
  justify-content: center;
  align-items: center;
}

/* 桌面端：确保所有元素在同一行 */
@media (min-width: 901px) {
  .filter-section {
    flex-wrap: nowrap;
  }
  
  .filter-section > * {
    flex-shrink: 0;
  }
  
  /* 桌面端：价格输入框之间的间距与筛选区域其他元素间距一致 */
  .price-group {
    gap: 20px;
  }
}

.filter-group {
  display: flex;
  align-items: center;
  gap: 20px;
}

/* 移除带标签的选择器样式，现在选择器直接显示值 */

/* 价格输入框内容右移，解决光标掩盖问题 */
.filter-group .pill-input :deep(.el-input__inner) {
  padding-left: 12px; /* 增加左边距，让内容右移 */
}

/* 自定义弹出式选择器样式 */
.custom-select {
  position: relative;
  display: flex;
  align-items: center;
  padding: 6px 16px;
  background: white;
  border: 1px solid var(--primary-color);
  border-radius: 20px;
  cursor: pointer;
  transition: all 0.3s ease;
  min-width: 130px;
  height: 32px;
  box-shadow: 0 2px 8px rgba(106, 1, 94, 0.1);
}

.custom-select:hover {
  border-color: var(--primary-light);
  box-shadow: 0 4px 12px rgba(106, 1, 94, 0.15);
}

.select-value {
  flex: 1;
  color: var(--primary-color);
  font-size: 14px;
  font-weight: normal;
  min-height: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  text-align: center;
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
  width: 130px; /* 固定宽度 */
  max-width: 130px;
  height: 32px; /* 固定高度，与选择器保持一致 */
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
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 20px;
  margin-bottom: 40px;
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
  aspect-ratio: 3 / 2; /* 3:2比例 */
  height: auto;
  overflow: hidden;
  border-radius: calc(12px * var(--mobile-scale, 1));
  margin-bottom: calc(12px * var(--mobile-scale, 1));
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
  display: flex;
  flex-direction: column;
}

.commodity-main-content {
  flex: 1;
  position: relative;
}

/* 药丸型标签 */
.commodity-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-bottom: 8px;
  justify-content: center;
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
  font-size: calc(25px * var(--mobile-scale, 1));
  font-weight: normal;
  color: var(--primary-color);
  margin: 0 0 calc(6px * var(--mobile-scale, 1)) 0;
  line-height: 1.4;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  text-align: center;
}

/* 描述 */
.commodity-description {
  font-size: calc(12px * var(--mobile-scale, 1));
  color: var(--primary-color);
  margin: 0 0 calc(8px * var(--mobile-scale, 1)) 0;
  line-height: 1.4;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  text-align: center;
}

/* 价格 */
.commodity-price {
  font-size: calc(30px * var(--mobile-scale, 1));
  font-weight: normal;
  color: var(--primary-color);
  margin: 0 0 calc(8px * var(--mobile-scale, 1)) 0;
  text-align: center;
}

/* 卖家信息卡片（独立行，居中） */
.seller-info-card {
  position: relative;
  background: transparent;
  border-radius: 12px;
  padding: 8px 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  min-height: 48px;
  height: auto;
  margin-top: 10px;
  border-top: 1px solid #f0f0f0;
  width: 100%;
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
  font-size: calc(20px * var(--mobile-scale, 1));
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

@media (max-width: 900px) {
  .nav-content {
    flex-direction: column;
    gap: 15px;
  }
  
  .nav-menu {
    gap: 20px;
  }
  
  /* 移动端：修改顺序为：价格-商品种类+排序（同一行） */
  .filter-section {
    flex-wrap: wrap;
    align-items: center;
    gap: 15px;
  }
  
  /* 移动端：价格区间在第一行 */
  .price-group {
    order: 1;
    display: flex;
    gap: 15px;
    justify-content: center;
  }
  
  /* 移动端：价格输入框固定宽度 */
  .price-group .pill-input {
    width: 130px;
    flex-shrink: 0;
  }
  
  /* 移动端：商品种类选择器在第二行，与排序共一行 */
  .filter-section > .custom-select {
    order: 2;
    min-width: 130px;
    flex-shrink: 0;
  }
  
  .commodity-grid {
    grid-template-columns: repeat(2, 1fr); /* 移动端强制每行2个卡片 */
    gap: calc(15px * var(--mobile-scale, 1));
  }
  
  .list-header {
    flex-direction: column;
    gap: 10px;
    text-align: center;
  }
}
</style>

