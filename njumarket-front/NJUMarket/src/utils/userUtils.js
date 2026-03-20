import { computed } from 'vue'

/**
 * 创建安全的用户状态计算属性
 * @param {Object} userStore - 用户store实例
 * @returns {Object} 包含安全用户状态的对象
 */
export function createSafeUserState(userStore) {
  const isLoggedIn = computed(() => userStore.isLoggedIn)
  const user = computed(() => userStore.user || {})
  
  // 安全获取用户显示名称
  const getUserDisplayName = computed(() => {
    if (!user.value) return '用户'
    
    // 优先使用nickname，然后是username，最后是手机号
    if (user.value.nickname) {
      return user.value.nickname
    }
    
    if (user.value.username) {
      return user.value.username
    }
    
    if (user.value.primaryPhone) {
      return user.value.primaryPhone.replace(/(\d{3})\d{4}(\d{4})/, '$1****$2')
    }
    
    return '用户'
  })
  
  // 安全获取用户头像
  const getUserAvatar = computed(() => {
    if (!user.value) return null
    
    // 检查avatar字段
    if (user.value.avatar) {
      return user.value.avatar
    }
    
    return null
  })
  
  // 安全获取用户ID
  const getUserId = computed(() => {
    return user.value?.userId || null
  })
  
  // 安全获取用户手机号
  const getUserPhone = computed(() => {
    return user.value?.primaryPhone || ''
  })
  
  // 检查是否有完整的profile数据
  const hasProfileData = computed(() => {
    return !!(user.value?.nickname || user.value?.avatar)
  })
  
  return {
    isLoggedIn,
    user,
    getUserDisplayName,
    getUserAvatar,
    getUserId,
    getUserPhone,
    hasProfileData
  }
}

/**
 * 检查用户是否已登录
 * @param {Object} userStore - 用户store实例
 * @returns {boolean} 是否已登录
 */
export function isUserLoggedIn(userStore) {
  return userStore.isLoggedIn && userStore.user !== null
}

/**
 * 获取用户信息，如果未登录返回默认值
 * @param {Object} userStore - 用户store实例
 * @param {*} defaultValue - 默认值
 * @returns {*} 用户信息或默认值
 */
export function getUserInfo(userStore, defaultValue = null) {
  return userStore.user || defaultValue
}
