<template>
  <div class="my-commodities-page">
    <!-- 商品管理内容 -->
    <div class="commodities-content">
      <div class="container">
        <div class="page-header">
          <h1>我的商品</h1>
          <div class="header-actions">
            <el-button type="primary" @click="$router.push('/publish')">
              发布商品
            </el-button>
          </div>
        </div>

        <!-- 商品状态筛选 -->
        <div class="commodity-tabs">
          <SafeTabs 
            v-model="activeTab" 
            @tab-change="handleTabChange"
          >
            <el-tab-pane label="全部" name="all"></el-tab-pane>
            <el-tab-pane label="草稿" name="DRAFT"></el-tab-pane>
            <el-tab-pane label="已上架" name="ON_SHELF"></el-tab-pane>
            <el-tab-pane label="已售完" name="SOLD_OUT"></el-tab-pane>
          </SafeTabs>
        </div>

        <!-- 商品列表 -->
        <div class="commodities-list" v-loading="loading">
          <div v-if="commodities.length === 0 && !loading" class="empty-state">
            <el-empty :description="getEmptyDescription()">
              <el-button type="primary" @click="handleEmptyAction()">
                {{ getEmptyActionText() }}
              </el-button>
            </el-empty>
          </div>

          <div v-for="commodity in commodities" :key="commodity.commodityId" class="commodity-card">
            <div class="commodity-image">
              <img 
                v-if="commodity.images && commodity.images.length > 0"
                :src="getCommodityImageUrl(commodity.images[0])" 
                :alt="commodity.title"
                @click="$router.push(`/commodity/${commodity.commodityId}`)"
              />
              <div v-else class="no-image">
                <span>暂无照片</span>
              </div>
            </div>

            <div class="commodity-info">
              <h3 class="commodity-title" @click="$router.push(`/commodity/${commodity.commodityId}`)">
                {{ commodity.title }}
              </h3>
              <p class="commodity-description">{{ commodity.description }}</p>
              <div class="commodity-meta">
                <span class="price text-primary">¥{{ commodity.price }}</span>
                <span class="stock">库存：{{ commodity.stock }}</span>
                <span class="category">{{ commodity.category }}</span>
              </div>
              <div class="commodity-status">
                <el-tag :type="getStatusType(commodity.commodityStatus)">
                  {{ getStatusText(commodity.commodityStatus) }}
                </el-tag>
                <span class="publish-time">{{ formatTime(commodity.createTime) }}</span>
              </div>
            </div>

            <div class="commodity-actions">
              <div class="action-buttons">
                <!-- 草稿商品：上架按钮 -->
                <el-button
                  v-if="commodity.commodityStatus === 'DRAFT'"
                  type="primary"
                  @click="handleShelf(commodity.commodityId)"
                >
                  上架
                </el-button>
                
                <!-- 已上架商品：下架按钮 -->
                <el-button
                  v-if="commodity.commodityStatus === 'ON_SHELF'"
                  @click="handleUnshelf(commodity.commodityId)"
                >
                  下架
                </el-button>
                
                <!-- 已售完商品：设为草稿按钮 -->
                <el-button
                  v-if="commodity.commodityStatus === 'SOLD_OUT'"
                  @click="handleDraft(commodity.commodityId)"
                >
                  设为草稿
                </el-button>
                
                <!-- 已下架商品：重新上架按钮 -->
                <el-button
                  v-if="commodity.commodityStatus === 'OFF_SHELF'"
                  type="primary"
                  @click="handleRepublish(commodity.commodityId)"
                >
                  重新上架
                </el-button>
                
                <!-- 已上架商品：设为售罄按钮 -->
                <el-button
                  v-if="commodity.commodityStatus === 'ON_SHELF'"
                  type="warning"
                  @click="handleSoldOut(commodity.commodityId)"
                >
                  设为售罄
                </el-button>
                
                <!-- 所有商品：编辑按钮 -->
                <el-button @click="handleEdit(commodity.commodityId)">
                  编辑
                </el-button>
                <el-dropdown @command="(command) => handleVisibilityChange(commodity.commodityId, command)">
                  <el-button>
                    可见性<el-icon><ArrowDown /></el-icon>
                  </el-button>
                  <template #dropdown>
                    <el-dropdown-menu>
                      <el-dropdown-item command="VISIBLE">完全可见</el-dropdown-item>
                      <el-dropdown-item command="SELLER_ONLY">仅卖家可见</el-dropdown-item>
                      <el-dropdown-item command="BUYER_ONLY">仅买家可见</el-dropdown-item>
                      <el-dropdown-item command="HIDDEN">隐藏</el-dropdown-item>
                    </el-dropdown-menu>
                  </template>
                </el-dropdown>
                <el-button
                  type="danger"
                  @click="handleDelete(commodity.commodityId)"
                >
                  删除
                </el-button>
              </div>
            </div>
          </div>
        </div>

        <!-- 分页 -->
        <div class="pagination-wrapper" v-if="total > 0">
          <el-pagination
            v-model:current-page="currentPage"
            v-model:page-size="pageSize"
            :page-sizes="[10, 20, 50]"
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
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '../stores/user'
import { commodityAPI, imageAPI } from '../api'
import { ElMessage } from 'element-plus'
import SafeTabs from '../components/SafeTabs.vue'

export default {
  name: 'MyCommodities',
  components: {
    SafeTabs
  },
  setup() {
    const router = useRouter()
    const userStore = useUserStore()
    
    const loading = ref(false)
    const commodities = ref([])
    const total = ref(0)
    const currentPage = ref(1)
    const pageSize = ref(10)
    const activeTab = ref('all')
    const isMounted = ref(false)
    
    const user = ref(userStore.user)
    
    // 获取商品列表
    const fetchCommodities = async () => {
      loading.value = true
      try {
        const params = {
          page: currentPage.value,
          size: pageSize.value
        }
        
        if (activeTab.value !== 'all') {
          params.status = activeTab.value
        }
        
        const response = await commodityAPI.getMy(params.page, params.size, params.status)
        console.log('API响应:', response) // 调试信息
        if (response.success) {
          // 修复：正确映射后端返回的数据结构
          commodities.value = response.data.commodities || []
          total.value = response.data.total || 0
          currentPage.value = response.data.current || 1
          pageSize.value = response.data.size || 10
          console.log('处理后的商品数据:', commodities.value) // 调试信息
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
    
    // 上架商品
    const handleShelf = async (commodityId) => {
      try {
        const confirmed = confirm('确定要上架这个商品吗？')
        if (!confirmed) {
          return
        }
        
        const response = await commodityAPI.shelf(commodityId)
        if (response.success) {
          ElMessage.success('商品已上架')
          fetchCommodities()
        } else {
          ElMessage.error(response.errorMsg || '上架失败')
        }
      } catch (error) {
        ElMessage.error('上架失败')
      }
    }
    
    // 下架商品
    const handleUnshelf = async (commodityId) => {
      try {
        const confirmed = confirm('确定要下架这个商品吗？')
        if (!confirmed) {
          return
        }
        
        const response = await commodityAPI.unshelf(commodityId)
        if (response.success) {
          ElMessage.success('商品已下架')
          fetchCommodities()
        } else {
          ElMessage.error(response.errorMsg || '下架失败')
        }
      } catch (error) {
        ElMessage.error('下架失败')
      }
    }
    
    // 设为草稿
    const handleDraft = async (commodityId) => {
      try {
        const confirmed = confirm('确定要将此商品设为草稿吗？')
        if (!confirmed) {
          return
        }
        
        const response = await commodityAPI.draft(commodityId)
        if (response.success) {
          ElMessage.success('商品已设为草稿')
          fetchCommodities()
        } else {
          ElMessage.error(response.errorMsg || '设为草稿失败')
        }
      } catch (error) {
        ElMessage.error('设为草稿失败')
      }
    }
    
    // 设为售罄
    const handleSoldOut = async (commodityId) => {
      try {
        const confirmed = confirm('确定要将此商品设为售罄吗？')
        if (!confirmed) {
          return
        }
        
        const response = await commodityAPI.soldOut(commodityId)
        if (response.success) {
          ElMessage.success('商品已设为售罄')
          fetchCommodities()
        } else {
          ElMessage.error(response.errorMsg || '设为售罄失败')
        }
      } catch (error) {
        ElMessage.error('设为售罄失败')
      }
    }
    
    // 重新上架商品
    const handleRepublish = async (commodityId) => {
      try {
        const confirmed = confirm('确定要重新上架这个商品吗？')
        if (!confirmed) {
          return
        }
        
        const response = await commodityAPI.republish(commodityId)
        if (response.success) {
          ElMessage.success('商品已重新上架')
          fetchCommodities()
        } else {
          ElMessage.error(response.errorMsg || '重新上架失败')
        }
      } catch (error) {
        ElMessage.error('重新上架失败')
      }
    }
    
    // 编辑商品
    const handleEdit = (commodityId) => {
      router.push(`/publish?edit=${commodityId}`)
    }
    
    // 修改商品可见性
    const handleVisibilityChange = async (commodityId, visibility) => {
      try {
        const response = await commodityAPI.updateVisibility(commodityId, visibility)
        if (response.success) {
          ElMessage.success('可见性修改成功')
          fetchCommodities()
        } else {
          ElMessage.error(response.errorMsg || '修改失败')
        }
      } catch (error) {
        ElMessage.error('修改失败')
      }
    }
    
    // 删除商品
    const handleDelete = async (commodityId) => {
      try {
        const confirmed = confirm('确定要删除这个商品吗？删除后无法恢复')
        if (!confirmed) {
          return
        }
        
        const response = await commodityAPI.delete(commodityId)
        if (response.success) {
          ElMessage.success('商品已删除')
          fetchCommodities()
        } else {
          ElMessage.error(response.errorMsg || '删除失败')
        }
      } catch (error) {
        ElMessage.error('删除失败')
      }
    }
    
    // 获取状态类型
    const getStatusType = (status) => {
      const statusMap = {
        'DRAFT': 'warning',
        'ON_SHELF': 'success',
        'OFF_SHELF': 'danger',
        'SOLD_OUT': 'info'
      }
      return statusMap[status] || 'info'
    }
    
    // 获取状态文本
    const getStatusText = (status) => {
      const statusMap = {
        'DRAFT': '草稿',
        'ON_SHELF': '已上架',
        'OFF_SHELF': '已下架',
        'SOLD_OUT': '已售完'
      }
      return statusMap[status] || status
    }
    
    // 格式化时间
    const formatTime = (time) => {
      if (!time) return ''
      return new Date(time).toLocaleString()
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
    
    // 获取空状态描述
    const getEmptyDescription = () => {
      const statusMap = {
        'all': '暂无商品',
        'DRAFT': '暂无草稿商品',
        'ON_SHELF': '暂无已上架商品',
        'SOLD_OUT': '暂无售罄商品'
      }
      return statusMap[activeTab.value] || '暂无商品'
    }
    
    // 获取空状态操作按钮文本
    const getEmptyActionText = () => {
      const actionMap = {
        'all': '发布商品',
        'DRAFT': '发布商品',
        'ON_SHELF': '发布商品',
        'SOLD_OUT': '发布商品'
      }
      return actionMap[activeTab.value] || '发布商品'
    }
    
    // 处理空状态操作
    const handleEmptyAction = () => {
      router.push('/publish')
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
      isMounted.value = true
      fetchCommodities()
    })
    
    // 组件卸载时清理
    onUnmounted(() => {
      isMounted.value = false
    })
    
    return {
      loading,
      commodities,
      total,
      currentPage,
      pageSize,
      activeTab,
      user,
      handleTabChange,
      handleSizeChange,
      handleCurrentChange,
      handleShelf,
      handleUnshelf,
      handleDraft,
      handleSoldOut,
      handleRepublish,
      handleEdit,
      handleVisibilityChange,
      handleDelete,
      getStatusType,
      getStatusText,
      getEmptyDescription,
      getEmptyActionText,
      handleEmptyAction,
      formatTime,
      getCommodityImageUrl,
      handleLogout
    }
  }
}
</script>

<style scoped>
.my-commodities-page {
  min-height: 100vh;
  background-color: #f5f5f5;
}

.commodities-content {
  padding: 30px 0;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 30px;
}

.page-header h1 {
  font-size: 28px;
  font-weight: bold;
  color: #333;
  margin: 0;
}

.commodity-tabs {
  background: white;
  border-radius: 8px;
  padding: 20px;
  margin-bottom: 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.commodities-list {
  margin-bottom: 30px;
}

.empty-state {
  text-align: center;
  padding: 60px 0;
}

.commodity-card {
  background: white;
  border-radius: 8px;
  margin-bottom: 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  overflow: hidden;
  display: flex;
  align-items: center;
  padding: 20px;
  gap: 20px;
}

.commodity-image {
  width: 120px;
  height: 120px;
  border-radius: 8px;
  overflow: hidden;
  cursor: pointer;
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
  font-size: 12px;
}

.commodity-info {
  flex: 1;
}

.commodity-title {
  font-size: 18px;
  font-weight: 600;
  margin-bottom: 8px;
  color: #333;
  cursor: pointer;
  transition: color 0.3s ease;
}

.commodity-title:hover {
  color: var(--primary-color);
}

.commodity-description {
  font-size: 14px;
  color: #666;
  margin-bottom: 10px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.commodity-meta {
  display: flex;
  gap: 15px;
  margin-bottom: 10px;
  font-size: 14px;
}

.price {
  font-size: 16px;
  font-weight: bold;
}

.stock {
  color: #666;
}

.category {
  color: #999;
}

.commodity-status {
  display: flex;
  align-items: center;
  gap: 15px;
}

.publish-time {
  font-size: 12px;
  color: #999;
}

.commodity-actions {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.action-buttons {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.pagination-wrapper {
  display: flex;
  justify-content: center;
}

@media (max-width: 768px) {
  .nav-content {
    flex-direction: column;
    gap: 15px;
  }
  
  .nav-menu {
    gap: 20px;
  }
  
  .page-header {
    flex-direction: column;
    gap: 15px;
    text-align: center;
  }
  
  .commodity-card {
    flex-direction: column;
    text-align: center;
  }
  
  .commodity-image {
    width: 100%;
    height: 200px;
  }
  
  .commodity-meta {
    justify-content: center;
  }
  
  .commodity-status {
    justify-content: center;
  }
  
  .action-buttons {
    flex-direction: row;
    flex-wrap: wrap;
    justify-content: center;
  }
}
</style>
