<!-- eslint-disable vue/multi-word-component-names -->
<template>
  <div class="home">
    <!-- 主要内容 -->
    <main class="main-content">
      <!-- 搜索区域 -->
      <section class="search-section">
        <div class="container">
          <div class="search-box">
            <h1 class="search-title">Welcome To NJU Market<br>欢迎来到南大集市</h1>
            <SearchBox 
              v-model:keyword="searchKeyword"
              placeholder="搜索商品..."
              @search="handleSearch"
            />
            <QuickLinks @click="searchByCategory" />
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
            :show-seller-info="true"
            card-type="simple"
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
            :show-seller-info="true"
            card-type="simple"
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
import SearchBox from '../components/search/SearchBox.vue'
import QuickLinks from '../components/search/QuickLinks.vue'

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
  background: white;
  color: var(--text-primary);
  padding: 100px 0; /* 增加上下内边距以更好利用宽屏空间 */
  text-align: center;
}

.search-title {
  font-size: 48px;
  font-weight: normal;
  margin-bottom: 24px;
  color: var(--primary-color);
}

.hot-section,
.latest-section {
  padding: 80px 0; /* 增加上下内边距以更好利用宽屏空间 */
  background: white;
}

.latest-section {
  background: #f8f9fa;
}

.section-title {
  font-size: 32px;
  font-weight: normal;
  text-align: center;
  margin-bottom: 40px;
  color: var(--text-primary);
}

/* 响应式设计 */
@media (min-width: 1600px) {
  .search-section {
    padding: 120px 0; /* 超宽屏使用更大的间距 */
  }
  
  .search-title {
    font-size: 56px; /* 超宽屏使用更大的标题 */
  }
  
  .hot-section,
  .latest-section {
    padding: 100px 0; /* 超宽屏使用更大的间距 */
  }
  
  .section-title {
    font-size: 40px; /* 超宽屏使用更大的标题 */
  }
}

@media (max-width: 900px) {
  .search-section {
    padding: 60px 0;
  }
  
  .search-title {
    font-size: 32px;
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