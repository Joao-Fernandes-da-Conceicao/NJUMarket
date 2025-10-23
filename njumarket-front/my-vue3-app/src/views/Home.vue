<!-- eslint-disable vue/multi-word-component-names -->
<template>
  <div class="home">
    <!-- 主要内容 -->
    <main class="main-content">
      <!-- 搜索区域 -->
      <section class="search-section">
        <div class="container">
          <div class="search-box">
            <h1 class="search-title">发现好物，分享生活</h1>
            <div class="search-form">
              <el-input
                v-model="searchKeyword"
                placeholder="搜索商品..."
                size="large"
                class="search-input"
              >
                <template #append>
                  <el-button type="primary" @click="handleSearch">搜索</el-button>
                </template>
              </el-input>
            </div>
            <div class="quick-links">
              <span class="quick-link" @click="searchByCategory('电子产品')">电子产品</span>
              <span class="quick-link" @click="searchByCategory('服装配饰')">服装配饰</span>
              <span class="quick-link" @click="searchByCategory('图书文具')">图书文具</span>
              <span class="quick-link" @click="searchByCategory('生活用品')">生活用品</span>
            </div>
          </div>
        </div>
      </section>

      <!-- 热门商品 -->
      <section class="hot-section">
        <div class="container">
          <h2 class="section-title">热门商品</h2>
          <CommodityGrid 
            :commodities="hotCommodities"
            :loading="hotLoading"
            empty-text="暂无热门商品"
            :show-empty-action="true"
            empty-action-text="浏览所有商品"
            @commodity-click="handleCommodityClick"
            @empty-action="$router.push('/commodities')"
          />
        </div>
      </section>

      <!-- 最新商品 -->
      <section class="latest-section">
        <div class="container">
          <h2 class="section-title">最新商品</h2>
          <CommodityGrid 
            :commodities="latestCommodities"
            :loading="latestLoading"
            empty-text="暂无最新商品"
            :show-empty-action="true"
            empty-action-text="浏览所有商品"
            @commodity-click="handleCommodityClick"
            @empty-action="$router.push('/commodities')"
          />
        </div>
      </section>
    </main>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useCommodityStore } from '../stores/commodity'
import CommodityGrid from '../components/commodity/CommodityGrid.vue'

const router = useRouter()
const commodityStore = useCommodityStore()

// 搜索相关
const searchKeyword = ref('')

// 商品数据
const hotCommodities = ref([])
const latestCommodities = ref([])
const hotLoading = ref(false)
const latestLoading = ref(false)

// 搜索处理
const handleSearch = () => {
  if (searchKeyword.value.trim()) {
    router.push({
      path: '/commodities',
      query: { keyword: searchKeyword.value.trim() }
    })
  }
}

// 分类搜索
const searchByCategory = (category) => {
  router.push({
    path: '/commodities',
    query: { category }
  })
}

// 商品点击处理
const handleCommodityClick = (commodity) => {
  router.push(`/commodity/${commodity.commodityId}`)
}

// 加载热门商品
const loadHotCommodities = async () => {
  hotLoading.value = true
  try {
    const response = await commodityStore.getHotCommodities(8)
    if (response.success) {
      hotCommodities.value = response.data || []
    }
  } catch (error) {
    console.error('加载热门商品失败:', error)
  } finally {
    hotLoading.value = false
  }
}

// 加载最新商品
const loadLatestCommodities = async () => {
  latestLoading.value = true
  try {
    const response = await commodityStore.getLatestCommodities(8)
    if (response.success) {
      latestCommodities.value = response.data || []
    }
  } catch (error) {
    console.error('加载最新商品失败:', error)
  } finally {
    latestLoading.value = false
  }
}

onMounted(() => {
  loadHotCommodities()
  loadLatestCommodities()
})
</script>

<style scoped>
.home {
  min-height: 100vh;
}

.main-content {
  padding-top: 0;
}

.search-section {
  background: linear-gradient(135deg, var(--primary-color) 0%, var(--primary-light) 100%);
  color: white;
  padding: 80px 0;
  text-align: center;
}

.search-title {
  font-size: 48px;
  font-weight: 700;
  margin-bottom: 24px;
  text-shadow: 0 2px 4px rgba(0, 0, 0, 0.3);
}

.search-form {
  max-width: 600px;
  margin: 0 auto 32px;
}

.search-input {
  width: 100%;
}

.search-input :deep(.el-input__wrapper) {
  border-radius: 8px 0 0 8px;
}

.search-input :deep(.el-input-group__append) {
  border-radius: 0 8px 8px 0;
}

.quick-links {
  display: flex;
  justify-content: center;
  gap: 24px;
  flex-wrap: wrap;
}

.quick-link {
  padding: 8px 16px;
  background: rgba(255, 255, 255, 0.2);
  border-radius: 20px;
  cursor: pointer;
  transition: all 0.3s ease;
  font-weight: 500;
}

.quick-link:hover {
  background: rgba(255, 255, 255, 0.3);
  transform: translateY(-2px);
}

.hot-section,
.latest-section {
  padding: 60px 0;
  background: white;
}

.latest-section {
  background: #f8f9fa;
}

.section-title {
  font-size: 32px;
  font-weight: 600;
  text-align: center;
  margin-bottom: 40px;
  color: var(--text-primary);
}

/* 响应式设计 */
@media (max-width: 768px) {
  .search-section {
    padding: 60px 0;
  }
  
  .search-title {
    font-size: 32px;
  }
  
  .quick-links {
    gap: 16px;
  }
  
  .quick-link {
    padding: 6px 12px;
    font-size: 14px;
  }
  
  .section-title {
    font-size: 24px;
    margin-bottom: 24px;
  }
  
  .hot-section,
  .latest-section {
    padding: 40px 0;
  }
}
</style>