<template>
  <div class="commodity-list-page">
    <!-- 搜索和筛选区域 -->
    <div class="search-filter-section">
      <div class="container">
        <div class="search-box">
          <el-input
            v-model="searchKeyword"
            placeholder="搜索商品..."
            size="large"
            class="search-input"
            @keyup.enter="handleSearch"
          >
            <template #append>
              <el-button type="primary" @click="handleSearch">搜索</el-button>
            </template>
          </el-input>
        </div>
        
        <div class="filter-section">
          <div class="filter-group">
            <el-select v-model="selectedCategory" placeholder="选择分类" @change="handleFilter">
              <el-option label="全部分类" value="" />
              <el-option
                v-for="category in categories"
                :key="category"
                :label="category"
                :value="category"
              />
            </el-select>
          </div>
          
          <div class="filter-group">
            <el-input
              v-model="minPrice"
              placeholder="最低价格"
              type="number"
              @change="handleFilter"
            />
            <span class="price-separator">-</span>
            <el-input
              v-model="maxPrice"
              placeholder="最高价格"
              type="number"
              @change="handleFilter"
            />
          </div>
          
          <div class="filter-group">
            <el-select v-model="sortBy" placeholder="排序方式" @change="handleFilter">
              <el-option label="默认排序" value="" />
              <el-option label="价格从低到高" value="price_asc" />
              <el-option label="价格从高到低" value="price_desc" />
              <el-option label="最新发布" value="latest" />
            </el-select>
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
            <div class="commodity-info">
              <h3 class="commodity-title">{{ commodity.title }}</h3>
              <p class="commodity-description">{{ commodity.description }}</p>
              <div class="commodity-meta">
                <p class="commodity-price text-primary">¥{{ commodity.price }}</p>
                <p class="commodity-location text-light">{{ commodity.location }}</p>
              </div>
              <div class="commodity-tags">
                <span class="tag category-tag">{{ commodity.category }}</span>
                <span class="tag condition-tag">{{ commodity.conditionLevel }}</span>
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
          <el-pagination
            v-model:current-page="currentPage"
            v-model:page-size="pageSize"
            :page-sizes="[10, 20, 50, 100]"
            :total="total"
            layout="total, sizes, prev, pager, next, jumper"
            @size-change="handleSizeChange"
            @current-change="handleCurrentChange"
          />
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { ref, onMounted, computed, watch } from 'vue'
import { useRoute } from 'vue-router'
import { useUserStore } from '../stores/user'
import { commodityAPI, imageAPI } from '../api'
import { ElMessage } from 'element-plus'

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
        
        const response = await commodityAPI.search(params)
        if (response.success) {
          // 修复：正确映射后端返回的数据结构
          commodities.value = response.data.commodities || []
          total.value = response.data.total || 0
          currentPage.value = response.data.current || 1
          pageSize.value = response.data.size || 20
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
      isLoggedIn,
      user,
      getUserDisplayName,
      handleSearch,
      handleFilter,
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
  padding: 30px 0;
  border-bottom: 1px solid #e0e0e0;
}

.search-box {
  margin-bottom: 20px;
}

.search-input {
  max-width: 600px;
  margin: 0 auto;
}

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
  font-weight: bold;
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
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  cursor: pointer;
  transition: transform 0.3s ease, box-shadow 0.3s ease;
}

.commodity-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 4px 16px rgba(106, 1, 94, 0.2);
}

.commodity-image {
  width: 100%;
  height: 200px;
  overflow: hidden;
  border-radius: 8px;
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

.commodity-info {
  padding: 15px;
}

.commodity-title {
  font-size: 16px;
  font-weight: 600;
  margin-bottom: 8px;
  color: #333;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.commodity-description {
  font-size: 14px;
  color: #666;
  margin-bottom: 10px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.commodity-meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}

.commodity-price {
  font-size: 18px;
  font-weight: bold;
}

.commodity-location {
  font-size: 12px;
}

.commodity-tags {
  display: flex;
  gap: 8px;
}

.tag {
  padding: 4px 8px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 500;
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
