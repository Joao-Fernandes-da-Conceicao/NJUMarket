<template>
  <el-dialog
    v-model="dialogVisible"
    :title="dialogTitle"
    :width="dialogWidth"
    :close-on-click-modal="false"
    @close="handleClose"
    class="select-dialog"
  >
    <div class="dialog-content">
      <!-- 搜索框 -->
      <div class="search-section">
        <UnifiedInput
          v-model="searchKeyword"
          placeholder="搜索商品标题或订单号..."
          clearable
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </UnifiedInput>
      </div>

      <!-- 列表 -->
      <div class="list-section" v-loading="loading">
        <!-- 商品列表 -->
        <div v-if="type === 'commodity'" class="commodity-list">
          <div
            v-for="item in filteredItems"
            :key="item.commodityId"
            class="list-item"
            :class="{ selected: selectedCommodityId === item.commodityId }"
            @click="selectCommodity(item)"
          >
            <div class="item-image" v-if="item.images && item.images.length > 0">
              <img :src="getImageUrl(item.images[0])" :alt="item.title" />
            </div>
            <div class="item-content">
              <h4 class="item-title">{{ item.title }}</h4>
              <div class="item-price">¥{{ formatPrice(item.price) }}</div>
              <div class="item-meta">
                <span>{{ item.category }}</span>
                <span>{{ item.conditionLevel }}</span>
                <span v-if="item.commodityStatus === 'ON_SHELF'" class="status-on-shelf">上架中</span>
              </div>
            </div>
            <div class="item-seller" v-if="item.sellerNickname || item.sellerId">
              <div class="seller-avatar" v-if="item.sellerAvatar">
                <img :src="getAvatarUrl(item.sellerAvatar)" :alt="item.sellerNickname || item.sellerId" />
              </div>
              <div class="seller-info">
                <span class="seller-label">卖家:</span>
                <span class="seller-name">{{ item.sellerNickname || item.sellerId }}</span>
              </div>
            </div>
            <div class="item-check" v-if="selectedCommodityId === item.commodityId">
              <el-icon><Check /></el-icon>
            </div>
          </div>
          <div v-if="filteredItems.length === 0 && !loading" class="empty-state">
            <p>暂无{{ type === 'commodity' ? '商品' : '订单' }}</p>
          </div>
        </div>

        <!-- 订单列表 -->
        <div v-else class="order-list">
          <div
            v-for="item in filteredItems"
            :key="item.orderId"
            class="list-item"
            :class="{ selected: selectedOrderId === item.orderId }"
            @click="selectOrder(item)"
          >
            <div class="item-image" v-if="getOrderImage(item)">
              <img :src="getImageUrl(getOrderImage(item))" :alt="item.commoditySnapshotTitle || '商品'" />
            </div>
            <div class="item-content">
              <div class="item-header">
                <span class="item-id">订单号: {{ item.orderId }}</span>
                <UnifiedTag :type="getStatusType(item.orderStatus)" size="small">
                  {{ getStatusText(item.orderStatus) }}
                </UnifiedTag>
              </div>
              <h4 class="item-title">{{ item.commoditySnapshotTitle || '商品' }}</h4>
              <div class="item-price">¥{{ formatPrice(item.payAmount || item.totalAmount || item.commoditySnapshotPrice || 0) }}</div>
              <div class="item-meta">
                <span>数量: {{ item.quantity }}</span>
                <span>{{ formatTime(item.createTime) }}</span>
              </div>
            </div>
            <div class="item-users">
              <div class="item-user" v-if="item.sellerNickname || item.sellerId">
                <div class="user-avatar" v-if="item.sellerAvatar">
                  <img :src="getAvatarUrl(item.sellerAvatar)" :alt="item.sellerNickname || item.sellerId" />
                </div>
                <div class="user-info">
                  <span class="user-label">卖家:</span>
                  <span class="user-name">{{ item.sellerNickname || item.sellerId }}</span>
                </div>
              </div>
              <div class="item-user" v-if="item.buyerNickname || item.buyerId">
                <div class="user-avatar" v-if="item.buyerAvatar">
                  <img :src="getAvatarUrl(item.buyerAvatar)" :alt="item.buyerNickname || item.buyerId" />
                </div>
                <div class="user-info">
                  <span class="user-label">买家:</span>
                  <span class="user-name">{{ item.buyerNickname || item.buyerId }}</span>
                </div>
              </div>
            </div>
            <div class="item-check" v-if="selectedOrderId === item.orderId">
              <el-icon><Check /></el-icon>
            </div>
          </div>
          <div v-if="filteredItems.length === 0 && !loading" class="empty-state">
            <p>暂无{{ type === 'commodity' ? '商品' : '订单' }}</p>
          </div>
        </div>
      </div>
    </div>

    <template #footer>
      <div class="dialog-footer">
        <UnifiedButton @click="handleCancel">取消选择</UnifiedButton>
        <UnifiedButton type="primary" @click="handleConfirm" :disabled="!hasSelection">
          确定
        </UnifiedButton>
      </div>
    </template>
  </el-dialog>
</template>

<script setup>
/* global defineProps, defineEmits */
import { ref, computed, watch, inject } from 'vue'
import { isMobile as globalIsMobile } from '../../config/responsive'
import { Search, Check } from '@element-plus/icons-vue'
import { commodityAPI, orderAPI, profileAPI } from '../../api'
import { imageAPI } from '../../api'
import { formatPrice, formatTime } from '../../utils/formatUtils'
import UnifiedInput from '../common/UnifiedInput.vue'
import UnifiedButton from '../common/UnifiedButton.vue'
import UnifiedTag from '../common/UnifiedTag.vue'
import { ElMessage } from 'element-plus'

// ✅ 注入父组件（Messages.vue）的增量更新结果
const incrementalUpdateResult = inject('incrementalUpdateResult', null)

// ✅ 注入父组件（Messages.vue）的profile缓存访问方法
const profileCacheProvider = inject('profileCacheProvider', null)

const props = defineProps({
  modelValue: {
    type: Boolean,
    default: false
  },
  type: {
    type: String,
    required: true,
    validator: (value) => ['commodity', 'order'].includes(value)
  },
  defaultId: {
    type: String,
    default: null
  },
  otherUserId: {
    type: String,
    default: null
  },
  currentUserId: {
    type: String,
    default: null
  }
})

const emit = defineEmits(['update:modelValue', 'confirm', 'cancel'])

const dialogVisible = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val)
})

const dialogTitle = computed(() => {
  return props.type === 'commodity' ? '选择商品' : '选择订单'
})

const loading = ref(false)
const items = ref([])
const searchKeyword = ref('')
const selectedCommodityId = ref(null)
const selectedOrderId = ref(null)

// ✅ 更新对话框列表中的商品/订单数据（基于增量轮询结果）- 异步处理profile信息
const updateDialogItemsFromPoll = async (commodities = [], orders = []) => {
  let updatedCount = 0
  
  if (commodities.length > 0 && props.type === 'commodity') {
    const commodityMap = new Map(commodities.map(c => [c.commodityId, c]))
    const existingIds = new Set(items.value.map(item => item.commodityId))
    
    // ✅ 收集需要获取profile的卖家ID
    const sellerIds = []
    commodities.forEach(commodity => {
      if (commodity.sellerId && !commodity.sellerNickname && !commodity.sellerAvatar) {
        sellerIds.push(commodity.sellerId)
      }
    })
    
    // ✅ 批量获取profile信息（优先使用缓存）
    const profileMap = sellerIds.length > 0 ? await fetchUserProfiles(sellerIds) : new Map()
    
    // 更新现有商品
    items.value.forEach((item, index) => {
      if (commodityMap.has(item.commodityId)) {
        const updatedCommodity = commodityMap.get(item.commodityId)
        const sellerProfile = profileMap.get(updatedCommodity.sellerId)
        
        // 合并更新，保留原有数据但更新变更的字段
        items.value[index] = {
          ...item,
          ...updatedCommodity,
          // ✅ 合并profile信息（优先使用查询到的profile，否则使用已有的）
          sellerNickname: sellerProfile ? sellerProfile.nickname : (updatedCommodity.sellerNickname || item.sellerNickname),
          sellerAvatar: sellerProfile ? sellerProfile.avatar : (updatedCommodity.sellerAvatar || item.sellerAvatar)
        }
        updatedCount++
      }
    })
    
    // ✅ 添加新创建的商品到列表最顶端（合并profile信息）
    commodities.forEach(commodity => {
      if (!existingIds.has(commodity.commodityId)) {
        const sellerProfile = profileMap.get(commodity.sellerId)
        const commodityWithProfile = {
          ...commodity,
          sellerNickname: sellerProfile ? sellerProfile.nickname : commodity.sellerNickname,
          sellerAvatar: sellerProfile ? sellerProfile.avatar : commodity.sellerAvatar
        }
        items.value.unshift(commodityWithProfile) // 添加到最前面
        updatedCount++
      }
    })
    
    // ✅ 如果有更新或新商品，重新按发布时间排序
    if (updatedCount > 0) {
      items.value.sort((a, b) => {
        const timeA = a.publishTime ? new Date(a.publishTime).getTime() : 0
        const timeB = b.publishTime ? new Date(b.publishTime).getTime() : 0
        return timeB - timeA // 降序：最新的在前
      })
    }
  }
  
  if (orders.length > 0 && props.type === 'order') {
    const orderMap = new Map(orders.map(o => [o.orderId, o]))
    const existingIds = new Set(items.value.map(item => item.orderId))
    
    // ✅ 收集需要获取profile的卖家/买家ID
    const userIds = []
    orders.forEach(order => {
      if (order.sellerId && !order.sellerNickname && !order.sellerAvatar) {
        userIds.push(order.sellerId)
      }
      if (order.buyerId && !order.buyerNickname && !order.buyerAvatar) {
        userIds.push(order.buyerId)
      }
    })
    
    // ✅ 批量获取profile信息（优先使用缓存）
    const profileMap = userIds.length > 0 ? await fetchUserProfiles(userIds) : new Map()
    
    // 更新现有订单
    items.value.forEach((item, index) => {
      if (orderMap.has(item.orderId)) {
        const updatedOrder = orderMap.get(item.orderId)
        const sellerProfile = profileMap.get(updatedOrder.sellerId)
        const buyerProfile = profileMap.get(updatedOrder.buyerId)
        
        // 合并更新，保留原有数据但更新变更的字段
        items.value[index] = {
          ...item,
          ...updatedOrder,
          // ✅ 合并profile信息（优先使用查询到的profile，否则使用已有的）
          sellerNickname: sellerProfile ? sellerProfile.nickname : (updatedOrder.sellerNickname || item.sellerNickname),
          sellerAvatar: sellerProfile ? sellerProfile.avatar : (updatedOrder.sellerAvatar || item.sellerAvatar),
          buyerNickname: buyerProfile ? buyerProfile.nickname : (updatedOrder.buyerNickname || item.buyerNickname),
          buyerAvatar: buyerProfile ? buyerProfile.avatar : (updatedOrder.buyerAvatar || item.buyerAvatar)
        }
        updatedCount++
      }
    })
    
    // ✅ 添加新创建的订单到列表最顶端（合并profile信息）
    orders.forEach(order => {
      if (!existingIds.has(order.orderId)) {
        const sellerProfile = profileMap.get(order.sellerId)
        const buyerProfile = profileMap.get(order.buyerId)
        const orderWithProfile = {
          ...order,
          sellerNickname: sellerProfile ? sellerProfile.nickname : order.sellerNickname,
          sellerAvatar: sellerProfile ? sellerProfile.avatar : order.sellerAvatar,
          buyerNickname: buyerProfile ? buyerProfile.nickname : order.buyerNickname,
          buyerAvatar: buyerProfile ? buyerProfile.avatar : order.buyerAvatar
        }
        items.value.unshift(orderWithProfile) // 添加到最前面
        updatedCount++
      }
    })
    
    // ✅ 如果有更新或新订单，重新按创建时间排序
    if (updatedCount > 0) {
      items.value.sort((a, b) => {
        const timeA = a.createTime ? new Date(a.createTime).getTime() : 0
        const timeB = b.createTime ? new Date(b.createTime).getTime() : 0
        return timeB - timeA // 降序：最新的在前
      })
    }
  }
  
  return updatedCount
}

const filteredItems = computed(() => {
  if (!searchKeyword.value.trim()) {
    return items.value
  }
  
  const keyword = searchKeyword.value.toLowerCase().trim()
  
  if (props.type === 'commodity') {
    return items.value.filter(item => 
      item.title.toLowerCase().includes(keyword) ||
      item.commodityId.toLowerCase().includes(keyword)
    )
  } else {
    return items.value.filter(item =>
      item.orderId.toLowerCase().includes(keyword) ||
      (item.commoditySnapshotTitle && item.commoditySnapshotTitle.toLowerCase().includes(keyword))
    )
  }
})

const hasSelection = computed(() => {
  return props.type === 'commodity' ? selectedCommodityId.value !== null : selectedOrderId.value !== null
})

// 响应式宽度
const dialogWidth = computed(() => {
  return globalIsMobile.value ? '90%' : '600px'
})

// ✅ 获取用户profile信息（批量）- 优先使用对话级别的缓存
const fetchUserProfiles = async (userIds) => {
  const profileMap = new Map()
  const uniqueUserIds = [...new Set(userIds.filter(id => id))]
  
  if (uniqueUserIds.length === 0) return profileMap
  
  // ✅ 优先从缓存中获取
  const cachePromises = uniqueUserIds.map(async (userId) => {
    if (profileCacheProvider?.getProfileFromCache) {
      try {
        const cachedProfile = await profileCacheProvider.getProfileFromCache(userId, null)
        if (cachedProfile) {
          profileMap.set(userId, cachedProfile)
          return true
        }
      } catch (error) {
        // 忽略缓存获取错误
      }
    }
    return false
  })
  await Promise.all(cachePromises)
  
  // ✅ 对于缓存中没有的用户，进行查询
  const uncachedUserIds = uniqueUserIds.filter(userId => !profileMap.has(userId))
  
  if (uncachedUserIds.length > 0) {
    const queryPromises = uncachedUserIds.map(async (userId) => {
    try {
      const response = await profileAPI.getUser(userId)
      if (response.success && response.data) {
        profileMap.set(userId, response.data)
          // ✅ 保存到localStorage缓存
          const { saveProfileToStorage } = await import('../../utils/profileCache')
          saveProfileToStorage(userId, response.data)
      }
    } catch (error) {
      console.error(`获取用户 ${userId} 的profile失败:`, error)
    }
  })
    await Promise.all(queryPromises)
  }
  
  return profileMap
}

// 获取商品列表（双方的商品）
const fetchCommodities = async () => {
  loading.value = true
  try {
    let allCommodities = []
    
    // 获取当前用户的商品（上架状态）
    if (props.currentUserId) {
      try {
        const myResponse = await commodityAPI.getMy(1, 100, 'ON_SHELF')
        if (myResponse.success) {
          const myCommodities = myResponse.data.commodities || []
          allCommodities.push(...myCommodities)
        }
      } catch (error) {
        console.error('获取当前用户商品失败:', error)
      }
    }
    
    // 获取对方用户的商品（公开可见，上架状态）
    if (props.otherUserId) {
      try {
        const otherResponse = await commodityAPI.getSellerCommodities(props.otherUserId, 1, 100, 'ON_SHELF')
        if (otherResponse.success) {
          const otherCommodities = otherResponse.data.commodities || []
          allCommodities.push(...otherCommodities)
        }
      } catch (error) {
        console.error('获取对方用户商品失败:', error)
      }
    }
    
    // 去重（根据 commodityId）
    const uniqueCommodities = []
    const commodityIds = new Set()
    for (const commodity of allCommodities) {
      if (!commodityIds.has(commodity.commodityId)) {
        commodityIds.add(commodity.commodityId)
        uniqueCommodities.push(commodity)
      }
    }
    
    // 提取所有卖家ID
    const sellerIds = uniqueCommodities.map(c => c.sellerId).filter(id => id)
    
    // 批量获取卖家profile信息
    const profileMap = await fetchUserProfiles(sellerIds)
    
    // 合并profile信息到商品数据
    let commoditiesWithProfile = uniqueCommodities.map(commodity => {
      const profile = profileMap.get(commodity.sellerId)
      if (profile) {
        return {
          ...commodity,
          sellerNickname: profile.nickname || commodity.sellerNickname,
          sellerAvatar: profile.avatar || commodity.sellerAvatar
        }
      }
      return commodity
    })
    
    // ✅ 按发布时间降序排序（最新的在最前面）
    commoditiesWithProfile.sort((a, b) => {
      const timeA = a.publishTime ? new Date(a.publishTime).getTime() : 0
      const timeB = b.publishTime ? new Date(b.publishTime).getTime() : 0
      return timeB - timeA // 降序：最新的在前
    })
    
    items.value = commoditiesWithProfile
    
    // 如果有默认ID，自动选择
    if (props.defaultId) {
      const found = items.value.find(item => item.commodityId === props.defaultId)
      if (found) {
        selectedCommodityId.value = props.defaultId
      }
    }
  } catch (error) {
    ElMessage.error('获取商品列表失败')
  } finally {
    loading.value = false
  }
}

// 获取订单列表（双方的订单）
const fetchOrders = async () => {
  loading.value = true
  try {
    // 获取当前用户与对方用户之间的订单
    let allOrders = []
    
    // 获取当前用户与对方用户之间的订单
    if (props.currentUserId && props.otherUserId) {
      // 获取买家订单（当前用户作为买家）
      try {
        const buyerResponse = await orderAPI.getBuyerOrders(1, 100)
        if (buyerResponse.success) {
          const buyerOrders = buyerResponse.data.orders || []
          allOrders.push(...buyerOrders.filter(order => 
            order.buyerId === props.currentUserId && order.sellerId === props.otherUserId
          ))
        }
      } catch (error) {
        console.error('获取买家订单失败:', error)
      }
      
      // 获取卖家订单（当前用户作为卖家）
      try {
        const sellerResponse = await orderAPI.getSellerOrders(1, 100)
        if (sellerResponse.success) {
          const sellerOrders = sellerResponse.data.orders || []
          allOrders.push(...sellerOrders.filter(order => 
            order.sellerId === props.currentUserId && order.buyerId === props.otherUserId
          ))
        }
      } catch (error) {
        console.error('获取卖家订单失败:', error)
      }
    }
    
    // 去重（根据 orderId）
    const uniqueOrders = []
    const orderIds = new Set()
    for (const order of allOrders) {
      if (!orderIds.has(order.orderId)) {
        orderIds.add(order.orderId)
        uniqueOrders.push(order)
      }
    }
    
    // 提取所有卖家和买家ID
    const userIds = []
    uniqueOrders.forEach(order => {
      if (order.sellerId) userIds.push(order.sellerId)
      if (order.buyerId) userIds.push(order.buyerId)
    })
    
    // 批量获取用户profile信息
    const profileMap = await fetchUserProfiles(userIds)
    
    // 合并profile信息到订单数据
    let ordersWithProfile = uniqueOrders.map(order => {
      const sellerProfile = profileMap.get(order.sellerId)
      const buyerProfile = profileMap.get(order.buyerId)
      
      return {
        ...order,
        sellerNickname: sellerProfile ? (sellerProfile.nickname || order.sellerNickname) : order.sellerNickname,
        sellerAvatar: sellerProfile ? (sellerProfile.avatar || order.sellerAvatar) : order.sellerAvatar,
        buyerNickname: buyerProfile ? (buyerProfile.nickname || order.buyerNickname) : order.buyerNickname,
        buyerAvatar: buyerProfile ? (buyerProfile.avatar || order.buyerAvatar) : order.buyerAvatar
      }
    })
    
    // ✅ 按创建时间降序排序（最新的在最前面）
    ordersWithProfile.sort((a, b) => {
      const timeA = a.createTime ? new Date(a.createTime).getTime() : 0
      const timeB = b.createTime ? new Date(b.createTime).getTime() : 0
      return timeB - timeA // 降序：最新的在前
    })
    
    items.value = ordersWithProfile
    
    // 如果有默认ID，自动选择
    if (props.defaultId) {
      const found = items.value.find(item => item.orderId === props.defaultId)
      if (found) {
        selectedOrderId.value = props.defaultId
      }
    }
  } catch (error) {
    ElMessage.error('获取订单列表失败')
  } finally {
    loading.value = false
  }
}

const selectCommodity = (commodity) => {
  if (selectedCommodityId.value === commodity.commodityId) {
    // 取消选择
    selectedCommodityId.value = null
  } else {
    selectedCommodityId.value = commodity.commodityId
  }
}

const selectOrder = (order) => {
  if (selectedOrderId.value === order.orderId) {
    // 取消选择
    selectedOrderId.value = null
  } else {
    selectedOrderId.value = order.orderId
  }
}

const handleConfirm = () => {
  if (props.type === 'commodity') {
    emit('confirm', selectedCommodityId.value)
  } else {
    emit('confirm', selectedOrderId.value)
  }
  dialogVisible.value = false
}

const handleCancel = () => {
  selectedCommodityId.value = null
  selectedOrderId.value = null
  emit('cancel')
  dialogVisible.value = false
}

const handleClose = () => {
  // 不清空选中状态，保留状态以便下次打开时恢复
  // 只清空搜索关键词
  searchKeyword.value = ''
}

const getImageUrl = (imageUrl) => {
  if (!imageUrl) return imageAPI.getDefaultCommodityImage()
  if (imageUrl.startsWith('http')) return imageUrl
  if (imageUrl.includes('/')) return imageUrl
  const fileName = imageUrl.split('/').pop()
  return imageAPI.getCommodityImage(fileName)
}

const getAvatarUrl = (avatar) => {
  if (!avatar) return ''
  if (avatar.startsWith('http')) return avatar
  return `http://localhost:8080/uploads/avatars/${avatar}`
}

// 获取订单商品快照的第一张图片
const getOrderImage = (order) => {
  // 优先级1: commoditySnapshotImages 字段（字符串或数组）
  if (order.commoditySnapshotImages) {
    if (typeof order.commoditySnapshotImages === 'string') {
      const images = order.commoditySnapshotImages.split(',').map(img => img.trim()).filter(img => img)
      if (images.length > 0) {
        return images[0]
      }
    } else if (Array.isArray(order.commoditySnapshotImages)) {
      if (order.commoditySnapshotImages.length > 0 && order.commoditySnapshotImages[0]) {
        return String(order.commoditySnapshotImages[0]).trim()
      }
    }
  }
  
  // 优先级2: commoditySnapshot.images 字段
  if (order.commoditySnapshot && order.commoditySnapshot.images) {
    if (typeof order.commoditySnapshot.images === 'string') {
      const images = order.commoditySnapshot.images.split(',').map(img => img.trim()).filter(img => img)
      if (images.length > 0) {
        return images[0]
      }
    } else if (Array.isArray(order.commoditySnapshot.images)) {
      if (order.commoditySnapshot.images.length > 0 && order.commoditySnapshot.images[0]) {
        return String(order.commoditySnapshot.images[0]).trim()
      }
    }
  }
  
  // 优先级3: commoditySnapshotImage 字段（字符串或数组）
  if (order.commoditySnapshotImage) {
    if (typeof order.commoditySnapshotImage === 'string') {
      const images = order.commoditySnapshotImage.split(',').map(img => img.trim()).filter(img => img)
      if (images.length > 0) {
        return images[0]
      }
    } else if (Array.isArray(order.commoditySnapshotImage)) {
      if (order.commoditySnapshotImage.length > 0 && order.commoditySnapshotImage[0]) {
        return String(order.commoditySnapshotImage[0]).trim()
      }
    }
  }
  
  // 优先级4: commoditySnapshot 是字符串，尝试解析JSON
  if (typeof order.commoditySnapshot === 'string') {
    try {
      const snapshot = JSON.parse(order.commoditySnapshot)
      if (snapshot.images) {
        if (typeof snapshot.images === 'string') {
          const images = snapshot.images.split(',').map(img => img.trim()).filter(img => img)
          if (images.length > 0) {
            return images[0]
          }
        } else if (Array.isArray(snapshot.images)) {
          if (snapshot.images.length > 0 && snapshot.images[0]) {
            return String(snapshot.images[0]).trim()
          }
        }
      }
    } catch (e) {
      // JSON解析失败，忽略
    }
  }
  
  // 优先级5: commoditySnapshot 是对象
  if (order.commoditySnapshot && typeof order.commoditySnapshot === 'object' && !Array.isArray(order.commoditySnapshot)) {
    if (order.commoditySnapshot.images) {
      if (typeof order.commoditySnapshot.images === 'string') {
        const images = order.commoditySnapshot.images.split(',').map(img => img.trim()).filter(img => img)
        if (images.length > 0) {
          return images[0]
        }
      } else if (Array.isArray(order.commoditySnapshot.images)) {
        if (order.commoditySnapshot.images.length > 0 && order.commoditySnapshot.images[0]) {
          return String(order.commoditySnapshot.images[0]).trim()
        }
      }
    }
  }
  
  return null
}

const getStatusText = (status) => {
  const statusMap = {
    'PENDING': '待付款',
    'CREATED': '待支付',
    'PAID': '已付款',
    'SHIPPED': '已发货',
    'DELIVERED': '已送达',
    'COMPLETED': '已完成',
    'CANCELLED': '已取消',
    'REFUNDED': '已退款',
    'REFUND_PENDING': '退款待处理',
    'REFUND_REQUESTED': '退款中',
    'REFUND_APPROVED': '退款完成',
    'REFUND_REJECTED': '退款被拒',
    'REFUND_PROCESSING': '退款处理中',
    'RETURN_PENDING': '退货待处理',
    'RETURN_APPROVED': '退货已批准',
    'RETURN_REJECTED': '退货被拒',
    'RETURN_PROCESSING': '退货处理中',
    'RETURN_SHIPPED': '退货已发货',
    'RETURN_COMPLETED': '退货已完成'
  }
  return statusMap[status] || status
}

const getStatusType = (status) => {
  const typeMap = {
    'PENDING': 'warning',
    'CREATED': 'warning',
    'PAID': 'info',
    'SHIPPED': 'primary',
    'DELIVERED': 'success',
    'COMPLETED': 'success',
    'CANCELLED': 'danger',
    'REFUNDED': 'info',
    'REFUND_PENDING': 'warning',
    'REFUND_REQUESTED': 'warning',
    'REFUND_APPROVED': 'success',
    'REFUND_REJECTED': 'danger',
    'REFUND_PROCESSING': 'info',
    'RETURN_PENDING': 'warning',
    'RETURN_APPROVED': 'success',
    'RETURN_REJECTED': 'danger',
    'RETURN_PROCESSING': 'info',
    'RETURN_SHIPPED': 'primary',
    'RETURN_COMPLETED': 'success'
  }
  return typeMap[status] || 'info'
}

// 监听弹窗显示，加载数据并恢复选中状态
watch(() => props.modelValue, async (visible) => {
  if (visible) {
    searchKeyword.value = ''
    
    // 恢复选中状态（如果有defaultId）
    if (props.defaultId) {
      if (props.type === 'commodity') {
        selectedCommodityId.value = props.defaultId
      } else {
        selectedOrderId.value = props.defaultId
      }
    }
    
    // 加载列表数据
    if (props.type === 'commodity') {
      await fetchCommodities()
    } else {
      await fetchOrders()
    }
  }
})

// ✅ 监听父组件（Messages.vue）的增量更新结果，实时更新对话框列表
if (incrementalUpdateResult) {
  watch(() => incrementalUpdateResult.timestamp, async (newTimestamp) => {
    // 只有当对话框打开且有时间戳更新时才处理
    if (props.modelValue && newTimestamp && items.value.length > 0) {
      // ✅ await异步函数，确保profile信息被正确获取和合并
      await updateDialogItemsFromPoll(
        incrementalUpdateResult.commodities || [],
        incrementalUpdateResult.orders || []
      )
    }
  })
}

// 监听defaultId变化，更新选中状态（当对话框打开时）
watch(() => props.defaultId, (newId) => {
  if (props.modelValue && newId) {
    if (props.type === 'commodity') {
      selectedCommodityId.value = newId
    } else {
      selectedOrderId.value = newId
    }
  }
})
</script>

<style scoped>
.dialog-content {
  display: flex;
  flex-direction: column;
  gap: 16px;
  max-height: 500px;
}

.search-section {
  flex-shrink: 0;
}

.list-section {
  flex: 1;
  overflow-y: auto;
  min-height: 200px;
}

.commodity-list,
.order-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.list-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.list-item:hover {
  border-color: var(--primary-color);
  background: #f9f9f9;
}

.list-item.selected {
  border-color: var(--primary-color);
  background: rgba(106, 1, 94, 0.05);
}

.item-image {
  width: 60px;
  height: 60px;
  flex-shrink: 0;
  border-radius: 6px;
  overflow: hidden;
  background: #fff;
}

.item-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.item-content {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.item-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.item-id {
  font-size: 12px;
  color: #666;
}

.item-title {
  font-size: 14px;
  font-weight: normal;
  color: #333;
  margin: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.item-price {
  font-size: 16px;
  font-weight: normal;
  color: var(--primary-color);
}

.item-meta {
  display: flex;
  gap: 8px;
  font-size: 12px;
  color: #999;
}

.status-on-shelf {
  color: var(--primary-color);
  font-weight: normal;
}

.item-seller {
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 6px;
  padding-left: 12px;
  font-size: 12px;
  color: #666;
}

.seller-avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  overflow: hidden;
  background: #f0f0f0;
  flex-shrink: 0;
}

.seller-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.seller-info {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 2px;
}

.seller-label {
  color: #999;
  font-size: 11px;
}

.seller-name {
  color: #333;
  font-weight: normal;
  font-size: 12px;
}

.item-users {
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 8px;
  padding-left: 12px;
  font-size: 12px;
}

.item-user {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 4px;
  color: #666;
}

.user-avatar {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  overflow: hidden;
  background: #f0f0f0;
  flex-shrink: 0;
}

.user-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.user-info {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 2px;
}

.user-label {
  color: #999;
  font-size: 11px;
}

.user-name {
  color: #333;
  font-weight: normal;
  font-size: 12px;
}

.item-check {
  flex-shrink: 0;
  width: 24px;
  height: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--primary-color);
  font-size: 18px;
}

.empty-state {
  text-align: center;
  padding: 40px 0;
  color: #999;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

/* 移动端样式 */
@media (max-width: 900px) {
  :deep(.el-dialog.select-dialog) {
    margin: 5vh auto !important;
  }
  
  .dialog-content {
    max-height: 60vh;
  }
  
  .list-section {
    max-height: 50vh;
  }
  
  .list-item {
    padding: 10px;
    gap: 10px;
    flex-wrap: wrap;
  }
  
  .item-image {
    width: 50px;
    height: 50px;
  }
  
  .item-content {
    flex: 1;
    min-width: 0;
  }
  
  .item-title {
    font-size: 13px;
  }
  
  .item-price {
    font-size: 14px;
  }
  
  .item-meta {
    font-size: 11px;
    flex-wrap: wrap;
  }
  
  /* 移动端：商品卡片的卖家信息移到下端，同一行平铺 */
  .item-seller {
    width: 100%;
    flex-direction: row;
    align-items: center;
    justify-content: flex-start;
    padding-left: 0;
    padding-top: 6px;
    gap: 6px;
    order: 999; /* 移到最下方 */
  }
  
  .seller-avatar {
    width: 24px;
    height: 24px;
  }
  
  .seller-info {
    flex-direction: row;
    align-items: center;
    gap: 4px;
  }
  
  .seller-label {
    font-size: 10px;
  }
  
  .seller-name {
    font-size: 11px;
  }
  
  /* 移动端：订单卡片的用户信息移到下端，同一行平铺 */
  .item-users {
    width: 100%;
    flex-direction: row;
    align-items: center;
    justify-content: flex-start;
    padding-left: 0;
    padding-top: 6px;
    gap: 12px;
    order: 999; /* 移到最下方 */
  }
  
  .item-user {
    flex-direction: row;
    align-items: center;
    gap: 4px;
  }
  
  .user-avatar {
    width: 24px;
    height: 24px;
  }
  
  .user-info {
    flex-direction: row;
    align-items: center;
    gap: 4px;
  }
  
  .user-label {
    font-size: 10px;
  }
  
  .user-name {
    font-size: 11px;
  }
  
  .item-check {
    order: 998; /* 保持在用户信息之前 */
  }
}
</style>


