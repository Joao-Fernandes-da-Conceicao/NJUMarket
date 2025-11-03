<template>
  <div class="user-home-page" @click="handlePageClick">
    <!-- 用户主页内容 -->
    <div class="home-content">
      <div class="container">
        <!-- 返回按钮（仅非本人主页时显示） -->
        <div v-if="!isOwnHome" class="home-back-button" @click="$router.go(-1)">
          <el-icon><ArrowLeft /></el-icon>
        </div>

        <!-- 用户基本信息 -->
        <div class="profile-section" v-loading="profileLoading">
          <div class="profile-main">
            <div class="pill-avatar">
              <el-avatar :size="200" :src="getAvatarUrl(profileData?.avatar)">
                {{ profileData?.nickname?.charAt(0) || 'U' }}
              </el-avatar>
            </div>
            <div class="pill-info">
              <h1 class="pill-username">{{ profileData?.nickname || '用户' }}</h1>
              <div class="pill-user-id">用户ID: {{ profileData?.userId || userId || '未知' }}</div>
              <span class="pill-vip">{{ profileData?.vipLevel || '普通' }}</span>
            </div>
          </div>
          
          <!-- 其余信息 - 在下方居中且同行分布 -->
          <div class="profile-details">
            <div class="stat-item">
              <span class="stat-label">信用分：</span>
              <span class="stat-value">{{ profileData?.creditScore || 0 }}</span>
            </div>
            <div class="stat-item">
              <span class="stat-label">买家评分：</span>
              <span class="stat-value">{{ profileData?.buyerRating || 0 }}</span>
            </div>
            <div class="stat-item">
              <span class="stat-label">卖家评分：</span>
              <span class="stat-value">{{ profileData?.sellerRating || 0 }}</span>
            </div>
            <div class="stat-item register-time">
              注册时间：{{ formatTime(profileData?.userInfo?.registerTime) }}
            </div>
          </div>
          
          <!-- 本人主页：操作按钮 -->
          <div class="profile-actions" v-if="isOwnHome">
            <UnifiedButton type="primary" class="profile-action-btn" @click="showAvatarDialog = true">
              更换头像
            </UnifiedButton>
            <UnifiedButton type="primary" class="profile-action-btn" @click="goToEditProfile">
              编辑资料
            </UnifiedButton>
            <UnifiedButton type="primary" class="profile-action-btn" @click="$router.push('/publish')">
              发布商品
            </UnifiedButton>
          </div>
        </div>

        <!-- 商品状态筛选 -->
        <div class="commodity-tabs">
          <SafeTabs 
            v-model="activeTab" 
            @tab-change="handleTabChange"
          >
            <el-tab-pane label="全部" name="all"></el-tab-pane>
            <el-tab-pane v-if="isOwnHome" label="草稿" name="DRAFT"></el-tab-pane>
            <el-tab-pane label="已发布" name="PUBLISHED"></el-tab-pane>
            <el-tab-pane label="已上架" name="ON_SHELF"></el-tab-pane>
            <el-tab-pane label="已下架" name="OFF_SHELF"></el-tab-pane>
          </SafeTabs>
        </div>

        <!-- 商品列表 -->
        <div class="commodities-list" v-loading="loading">
          <div v-if="commodities.length === 0 && !loading" class="empty-state">
            <el-empty :description="getEmptyDescription()">
              <UnifiedButton v-if="isOwnHome" type="primary" @click="handleEmptyAction()">
                {{ getEmptyActionText() }}
              </UnifiedButton>
            </el-empty>
          </div>

          <MyCommodityCard
            v-for="commodity in commodities"
            :key="commodity.commodityId"
            :commodity="commodity"
            :show-actions="isOwnHome"
            @click="handleCommodityClick"
            @publish="handlePublish"
            @shelf="handleShelf"
            @unshelf="handleUnshelf"
            @republish="handleRepublish"
            @edit="handleEdit"
            @delete="handleDelete"
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

    <!-- 更换头像对话框 -->
    <el-dialog
      v-model="showAvatarDialog"
      title="更换头像"
      width="350px"
      :close-on-click-modal="false"
    >
      <div class="avatar-upload">
        <el-upload
          :action="uploadUrl"
          :headers="uploadHeaders"
          :show-file-list="false"
          :on-success="handleAvatarSuccess"
          :on-error="handleAvatarError"
          :before-upload="beforeAvatarUpload"
          accept="image/*"
        >
          <UnifiedButton type="primary" class="pill-upload-btn">选择头像</UnifiedButton>
        </el-upload>
        <div class="upload-tip">
          <p>支持 JPG、PNG 格式，建议尺寸 200x200 像素</p>
        </div>
      </div>
      
      <template #footer>
        <UnifiedButton @click="showAvatarDialog = false" class="pill-close-btn">关闭</UnifiedButton>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import { ref, onMounted, onUnmounted, computed, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '../stores/user'
import { commodityAPI, profileAPI, imageAPI } from '../api'
import { ElMessage } from 'element-plus'
import { ArrowLeft, ArrowDown } from '@element-plus/icons-vue'
import SafeTabs from '../components/SafeTabs.vue'
import MyCommodityCard from '../components/commodity/MyCommodityCard.vue'
import UnifiedButton from '../components/common/UnifiedButton.vue'
import '../styles/pagination.css'

export default {
  name: 'UserHome',
  components: {
    SafeTabs,
    MyCommodityCard,
    UnifiedButton,
    ArrowLeft,
    ArrowDown
  },
  setup() {
    const route = useRoute()
    const router = useRouter()
    const userStore = useUserStore()
    
    const loading = ref(false)
    const profileLoading = ref(false)
    const commodities = ref([])
    const profileData = ref(null)
    const total = ref(0)
    const currentPage = ref(1)
    const pageSize = ref(10)
    const activeTab = ref('all')
    const showAvatarDialog = ref(false)
    
    // 弹出式选择器状态
    const showPageSizeSelect = ref(false)
    
    // 跳转页面输入
    const jumpPage = ref('')
    
    const userId = computed(() => route.params.userId)
    const user = computed(() => userStore.user)
    const isOwnHome = computed(() => {
      return !userId.value || userId.value === user.value?.userId
    })
    
    // 上传配置
    const uploadUrl = ref('http://localhost:8080/api/user/profile/avatar')
    const uploadHeaders = computed(() => ({
      'Authorization': `Bearer ${userStore.token}`
    }))
    
    // 获取用户资料
    const fetchProfile = async () => {
      profileLoading.value = true
      try {
        const targetUserId = userId.value || user.value?.userId
        let response
        
        if (isOwnHome.value) {
          response = await profileAPI.getMe()
        } else {
          response = await profileAPI.getUser(targetUserId)
        }
        
        if (response.success) {
          profileData.value = response.data
        }
      } catch (error) {
        ElMessage.error('获取用户资料失败')
      } finally {
        profileLoading.value = false
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
        
        if (activeTab.value !== 'all') {
          params.status = activeTab.value
        }
        
        let response
        if (isOwnHome.value) {
          // 本人主页：使用 getMy API
          response = await commodityAPI.getMy(params.page, params.size, params.status)
        } else {
          // 其他用户主页：使用 getSellerCommodities API（不能查看草稿）
          if (params.status === 'DRAFT') {
            // 如果选择草稿但非本人，重置为全部
            activeTab.value = 'all'
            params.status = null
          }
          response = await commodityAPI.getSellerCommodities(
            userId.value,
            params.page,
            params.size,
            params.status || 'all'
          )
        }
        
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
      // 非本人主页不能查看草稿
      if (!isOwnHome.value && tabName === 'DRAFT') {
        ElMessage.warning('无法查看其他用户的草稿商品')
        activeTab.value = 'all'
        return
      }
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
      showPageSizeSelect.value = false
      if (jumpPage.value && String(jumpPage.value).trim() !== '') {
        handleJump()
      }
    }
    
    // 商品操作处理（仅本人主页）
    const handleShelf = async (commodityId) => {
      try {
        const confirmed = confirm('确定要上架这个商品吗？')
        if (!confirmed) return
        
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
    
    const handleUnshelf = async (commodityId) => {
      try {
        const confirmed = confirm('确定要下架这个商品吗？')
        if (!confirmed) return
        
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
    
    const handleRepublish = async (commodityId) => {
      try {
        const confirmed = confirm('确定要重新上架这个商品吗？')
        if (!confirmed) return
        
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
    
    const handlePublish = async (commodityId) => {
      try {
        const confirmed = confirm('确定要发布这个草稿商品吗？')
        if (!confirmed) return
        
        const response = await commodityAPI.publishDraft(commodityId)
        if (response.success) {
          ElMessage.success('商品已发布')
          fetchCommodities()
        } else {
          ElMessage.error(response.errorMsg || '发布失败')
        }
      } catch (error) {
        ElMessage.error('发布失败')
      }
    }
    
    const handleEdit = (commodityId) => {
      router.push(`/publish?edit=${commodityId}`)
    }
    
    const handleDelete = async (commodityId) => {
      try {
        const confirmed = confirm('确定要删除这个商品吗？删除后无法恢复')
        if (!confirmed) return
        
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
      if (avatarUrl.startsWith('http')) return avatarUrl
      if (avatarUrl.includes('/')) return avatarUrl
      const fileName = avatarUrl.split('/').pop()
      return imageAPI.getAvatar(fileName)
    }
    
    // 获取空状态描述
    const getEmptyDescription = () => {
      const statusMap = {
        'all': '暂无商品',
        'DRAFT': '暂无草稿商品',
        'PUBLISHED': '暂无已发布商品',
        'ON_SHELF': '暂无已上架商品',
        'OFF_SHELF': '暂无已下架商品'
      }
      return statusMap[activeTab.value] || '暂无商品'
    }
    
    // 获取空状态操作按钮文本
    const getEmptyActionText = () => {
      return '发布商品'
    }
    
    // 处理空状态操作
    const handleEmptyAction = () => {
      router.push('/publish')
    }
    
    // 跳转到编辑资料页面
    const goToEditProfile = () => {
      router.push('/edit-profile')
    }
    
    // 头像上传前检查
    const beforeAvatarUpload = (file) => {
      const isImage = file.type.startsWith('image/')
      const isLt2M = file.size / 1024 / 1024 < 2
      
      if (!isImage) {
        ElMessage.error('只能上传图片文件!')
        return false
      }
      if (!isLt2M) {
        ElMessage.error('图片大小不能超过 2MB!')
        return false
      }
      return true
    }
    
    // 头像上传成功
    const handleAvatarSuccess = (response) => {
      if (response.success) {
        ElMessage.success('头像更新成功')
        const imageUrl = response.data.imageUrl || response.data
        profileData.value.avatar = imageUrl
        userStore.updateUser({ avatar: imageUrl })
        showAvatarDialog.value = false
      } else {
        ElMessage.error(response.errorMsg || '头像上传失败')
      }
    }
    
    // 头像上传失败
    const handleAvatarError = () => {
      ElMessage.error('头像上传失败')
    }
    
    onMounted(() => {
      // 检查是否需要重定向：如果访问的是自己的主页但使用了 userId 参数，重定向到不带参数的 /home
      if (userId.value && user.value && userId.value === user.value.userId) {
        router.replace('/home')
        return
      }
      
      // 注意：不再在"我的主页"清除卖家订单角标，改为在"卖家订单"页面清除
      
      // 正常加载数据
      fetchProfile()
      fetchCommodities()
    })
    
    // 监听路由变化
    watch(() => route.params.userId, (newUserId) => {
      // 如果路由变化后，userId 是自己的，重定向
      if (newUserId && user.value && newUserId === user.value.userId) {
        router.replace('/home')
        return
      }
      
      // 重新加载数据
      fetchProfile()
      fetchCommodities()
    })
    
    onUnmounted(() => {
      // 清理
    })
    
    return {
      loading,
      profileLoading,
      commodities,
      profileData,
      total,
      currentPage,
      pageSize,
      activeTab,
      showPageSizeSelect,
      jumpPage,
      showAvatarDialog,
      userId,
      user,
      isOwnHome,
      uploadUrl,
      uploadHeaders,
      handleTabChange,
      handleCurrentChange,
      togglePageSizeSelect,
      selectPageSize,
      getTotalPages,
      getPageNumbers,
      handleJump,
      handleShelf,
      handleUnshelf,
      handleRepublish,
      handlePublish,
      handleEdit,
      handleDelete,
      handleCommodityClick,
      getEmptyDescription,
      getEmptyActionText,
      handleEmptyAction,
      formatTime,
      getAvatarUrl,
      goToEditProfile,
      beforeAvatarUpload,
      handleAvatarSuccess,
      handleAvatarError,
      handlePageClick
    }
  }
}
</script>

<style scoped>
.user-home-page {
  min-height: 100vh;
  background-color: #f5f5f5;
}

.home-content {
  padding: 40px 0;
}

/* 返回按钮样式 */
.home-back-button {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: white;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  cursor: pointer;
  transition: all 0.3s ease;
  margin-bottom: 20px;
  color: var(--primary-color);
}

.home-back-button:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.home-back-button .el-icon {
  font-size: 20px;
}

/* Profile 部分样式（复用 UserProfile 的样式） */
.profile-section {
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

.profile-actions {
  display: flex;
  flex-direction: row;
  gap: 10px;
  justify-content: center;
}

.profile-action-btn {
  padding: 10px 30px !important;
  font-size: 16px !important;
}

/* 商品标签页样式（复用 MyCommodities 的样式） */
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

/* 商品列表样式 */
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

/* 头像上传对话框样式 */
.avatar-upload {
  text-align: center;
}

.upload-tip {
  margin-top: 15px;
  color: #999;
  font-size: 12px;
}

.pill-upload-btn,
.pill-close-btn {
  border-radius: 20px;
  padding: 10px 20px;
  min-width: auto;
  width: 120px;
}

/* 响应式设计 */
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
  
  .profile-actions {
    margin-top: 8vw !important;
    margin-bottom: 8vw !important;
  }
  
  .profile-section {
    gap: 20px;
  }
}
</style>

