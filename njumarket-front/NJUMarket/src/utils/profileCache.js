/**
 * Profile缓存工具类
 * 使用localStorage存储用户profile信息，避免内存占用过大
 * 
 * 特性：
 * 1. LRU缓存机制：限制缓存大小，淘汰最久未使用的数据
 * 2. 过期时间机制：定期清理过期缓存
 * 3. 对话级别缓存：按对话ID组织，便于管理
 * 4. 降级机制：如果localStorage不可访问，自动降级到内存Map
 */

const CACHE_PREFIX = 'njumarket_profile_cache_'
const CACHE_TIMESTAMP_PREFIX = 'njumarket_profile_cache_timestamp_'
const MAX_CACHE_SIZE = 200 // 最大缓存200个用户的profile
const CACHE_TTL = 30 * 60 * 1000 // 30分钟过期

// ✅ 检测localStorage是否可用
let localStorageAvailable = false
let fallbackCache = new Map() // 降级用的内存Map缓存
let fallbackCacheTimestamps = new Map() // 降级用的时间戳Map

try {
  // 测试localStorage是否可用
  const testKey = '__localStorage_test__'
  localStorage.setItem(testKey, 'test')
  localStorage.removeItem(testKey)
  localStorageAvailable = true
  console.log('✅ localStorage可用，使用localStorage存储Profile缓存')
} catch (error) {
  localStorageAvailable = false
  console.warn('⚠️ localStorage不可用，降级到内存Map缓存', error)
  fallbackCache = new Map()
  fallbackCacheTimestamps = new Map()
}

/**
 * 获取缓存的key
 */
const getCacheKey = (userId) => {
  return `${CACHE_PREFIX}${userId}`
}

/**
 * 获取时间戳的key
 */
const getTimestampKey = (userId) => {
  return `${CACHE_TIMESTAMP_PREFIX}${userId}`
}

/**
 * 检查缓存是否过期
 */
const isExpired = (userId) => {
  let timestamp = null
  
  if (localStorageAvailable) {
    const timestampKey = getTimestampKey(userId)
    timestamp = localStorage.getItem(timestampKey)
  } else {
    // 降级：从内存Map获取时间戳
    timestamp = fallbackCacheTimestamps.get(userId)
  }
  
  if (!timestamp) return true
  
  const cacheTime = typeof timestamp === 'number' ? timestamp : parseInt(timestamp, 10)
  const now = Date.now()
  
  return (now - cacheTime) > CACHE_TTL
}

/**
 * 获取所有缓存的用户ID（用于LRU管理）
 */
const getAllCachedUserIds = () => {
  if (localStorageAvailable) {
    const userIds = []
    for (let i = 0; i < localStorage.length; i++) {
      const key = localStorage.key(i)
      if (key && key.startsWith(CACHE_PREFIX)) {
        const userId = key.replace(CACHE_PREFIX, '')
        userIds.push(userId)
      }
    }
    return userIds
  } else {
    // 降级：从内存Map获取所有userId
    return Array.from(fallbackCache.keys())
  }
}

/**
 * 获取最久未使用的用户ID（LRU淘汰）
 */
const getLeastRecentlyUsedUserId = () => {
  const userIds = getAllCachedUserIds()
  
  if (userIds.length === 0) return null
  
  let oldestUserId = userIds[0]
  let oldestTimestamp = Infinity
  
  userIds.forEach(userId => {
    let timestamp = 0
    
    if (localStorageAvailable) {
      const timestampKey = getTimestampKey(userId)
      timestamp = parseInt(localStorage.getItem(timestampKey) || '0', 10)
    } else {
      // 降级：从内存Map获取时间戳
      timestamp = fallbackCacheTimestamps.get(userId) || 0
    }
    
    if (timestamp < oldestTimestamp) {
      oldestTimestamp = timestamp
      oldestUserId = userId
    }
  })
  
  return oldestUserId
}

/**
 * 清理过期缓存
 */
const cleanExpiredCache = () => {
  const userIds = getAllCachedUserIds()
  let cleanedCount = 0
  
  userIds.forEach(userId => {
    if (isExpired(userId)) {
      if (localStorageAvailable) {
        const cacheKey = getCacheKey(userId)
        const timestampKey = getTimestampKey(userId)
        localStorage.removeItem(cacheKey)
        localStorage.removeItem(timestampKey)
      } else {
        // 降级：从内存Map删除
        fallbackCache.delete(userId)
        fallbackCacheTimestamps.delete(userId)
      }
      cleanedCount++
    }
  })
  
  if (cleanedCount > 0) {
    console.log(`🧹 Profile缓存清理: 清理了 ${cleanedCount} 个过期缓存`)
  }
  
  return cleanedCount
}

/**
 * 确保缓存大小不超过限制（LRU淘汰）
 */
const ensureCacheSize = () => {
  let currentSize = getAllCachedUserIds().length
  
  while (currentSize >= MAX_CACHE_SIZE) {
    const lruUserId = getLeastRecentlyUsedUserId()
    
    if (!lruUserId) break
    
    if (localStorageAvailable) {
      const cacheKey = getCacheKey(lruUserId)
      const timestampKey = getTimestampKey(lruUserId)
      localStorage.removeItem(cacheKey)
      localStorage.removeItem(timestampKey)
    } else {
      // 降级：从内存Map删除
      fallbackCache.delete(lruUserId)
      fallbackCacheTimestamps.delete(lruUserId)
    }
    
    currentSize--
    console.log(`🗑️ Profile缓存LRU淘汰: 删除用户 ${lruUserId} 的缓存`)
  }
}

/**
 * 从localStorage获取profile缓存
 * @param {string} userId - 用户ID
 * @returns {Object|null} profile信息，如果不存在或过期返回null
 */
export const getProfileFromStorage = (userId) => {
  if (!userId) return null
  
  try {
    // 检查是否过期
    if (isExpired(userId)) {
      // 过期则删除
      if (localStorageAvailable) {
        const cacheKey = getCacheKey(userId)
        const timestampKey = getTimestampKey(userId)
        localStorage.removeItem(cacheKey)
        localStorage.removeItem(timestampKey)
      } else {
        // 降级：从内存Map删除
        fallbackCache.delete(userId)
        fallbackCacheTimestamps.delete(userId)
      }
      return null
    }
    
    let profile = null
    
    if (localStorageAvailable) {
      const cacheKey = getCacheKey(userId)
      const cachedData = localStorage.getItem(cacheKey)
      
      if (!cachedData) return null
      
      profile = JSON.parse(cachedData)
      
      // 更新访问时间戳（LRU）
      const timestampKey = getTimestampKey(userId)
      localStorage.setItem(timestampKey, Date.now().toString())
    } else {
      // 降级：从内存Map获取
      profile = fallbackCache.get(userId)
      if (!profile) return null
      
      // 更新访问时间戳（LRU）
      fallbackCacheTimestamps.set(userId, Date.now())
    }
    
    return profile
  } catch (error) {
    console.error(`❌ 从缓存获取profile失败: userId=${userId}`, error)
    // 如果localStorage出错，尝试降级
    if (localStorageAvailable && error.name !== 'SecurityError') {
      console.warn('⚠️ localStorage出错，尝试降级到内存Map')
      localStorageAvailable = false
      // 递归调用，使用降级机制
      return getProfileFromStorage(userId)
    }
    return null
  }
}

/**
 * 保存profile到localStorage
 * @param {string} userId - 用户ID
 * @param {Object} profile - profile信息
 */
export const saveProfileToStorage = (userId, profile) => {
  if (!userId || !profile) return
  
  try {
    // 清理过期缓存
    cleanExpiredCache()
    
    // 确保缓存大小不超过限制
    ensureCacheSize()
    
    if (localStorageAvailable) {
      const cacheKey = getCacheKey(userId)
      const timestampKey = getTimestampKey(userId)
      
      // 保存profile数据
      localStorage.setItem(cacheKey, JSON.stringify(profile))
      
      // 保存时间戳
      localStorage.setItem(timestampKey, Date.now().toString())
      
      console.log(`✅ Profile缓存保存成功（localStorage）: userId=${userId}`)
    } else {
      // 降级：保存到内存Map
      fallbackCache.set(userId, profile)
      fallbackCacheTimestamps.set(userId, Date.now())
      console.log(`✅ Profile缓存保存成功（内存Map降级）: userId=${userId}`)
    }
  } catch (error) {
    // 如果存储失败（可能因为localStorage空间不足或被禁用），尝试降级到内存Map
    if (localStorageAvailable) {
      if (error.name === 'QuotaExceededError') {
        console.warn('⚠️ localStorage空间不足，尝试清理缓存后重试')
        cleanExpiredCache()
        
        // 再次尝试保存
        try {
          const cacheKey = getCacheKey(userId)
          const timestampKey = getTimestampKey(userId)
          localStorage.setItem(cacheKey, JSON.stringify(profile))
          localStorage.setItem(timestampKey, Date.now().toString())
          console.log(`✅ Profile缓存保存成功（清理后）: userId=${userId}`)
        } catch (retryError) {
          // 清理后仍然失败，降级到内存Map
          console.warn('⚠️ localStorage清理后仍失败，降级到内存Map')
          localStorageAvailable = false
          fallbackCache.set(userId, profile)
          fallbackCacheTimestamps.set(userId, Date.now())
          console.log(`✅ Profile缓存保存成功（降级到内存Map）: userId=${userId}`)
        }
      } else if (error.name === 'SecurityError' || error.name === 'QuotaExceededError') {
        // localStorage被禁用或不可访问，降级到内存Map
        console.warn('⚠️ localStorage不可访问，降级到内存Map', error)
        localStorageAvailable = false
        fallbackCache.set(userId, profile)
        fallbackCacheTimestamps.set(userId, Date.now())
        console.log(`✅ Profile缓存保存成功（降级到内存Map）: userId=${userId}`)
      } else {
        console.error(`❌ Profile缓存保存失败: userId=${userId}`, error)
      }
    } else {
      // 已经在降级模式，直接保存到内存Map
      try {
        fallbackCache.set(userId, profile)
        fallbackCacheTimestamps.set(userId, Date.now())
        console.log(`✅ Profile缓存保存成功（内存Map）: userId=${userId}`)
      } catch (fallbackError) {
        console.error(`❌ Profile缓存保存失败（内存Map）: userId=${userId}`, fallbackError)
      }
    }
  }
}

/**
 * 删除指定用户的profile缓存
 * @param {string} userId - 用户ID
 */
export const removeProfileFromStorage = (userId) => {
  if (!userId) return
  
  try {
    if (localStorageAvailable) {
      const cacheKey = getCacheKey(userId)
      const timestampKey = getTimestampKey(userId)
      localStorage.removeItem(cacheKey)
      localStorage.removeItem(timestampKey)
    } else {
      // 降级：从内存Map删除
      fallbackCache.delete(userId)
      fallbackCacheTimestamps.delete(userId)
    }
    
    console.log(`🗑️ Profile缓存删除成功: userId=${userId}`)
  } catch (error) {
    console.error(`❌ Profile缓存删除失败: userId=${userId}`, error)
  }
}

/**
 * 清除所有profile缓存
 */
export const clearAllProfileCache = () => {
  try {
    const userIds = getAllCachedUserIds()
    
    userIds.forEach(userId => {
      if (localStorageAvailable) {
        const cacheKey = getCacheKey(userId)
        const timestampKey = getTimestampKey(userId)
        localStorage.removeItem(cacheKey)
        localStorage.removeItem(timestampKey)
      } else {
        // 降级：从内存Map删除
        fallbackCache.delete(userId)
        fallbackCacheTimestamps.delete(userId)
      }
    })
    
    console.log(`🧹 Profile缓存全部清除: 共清除 ${userIds.length} 个缓存`)
  } catch (error) {
    console.error('❌ Profile缓存清除失败:', error)
  }
}

/**
 * 获取缓存统计信息
 */
export const getCacheStats = () => {
  const userIds = getAllCachedUserIds()
  //const now = Date.now()
  let expiredCount = 0
  let validCount = 0
  
  userIds.forEach(userId => {
    if (isExpired(userId)) {
      expiredCount++
    } else {
      validCount++
    }
  })
  
  return {
    total: userIds.length,
    valid: validCount,
    expired: expiredCount,
    maxSize: MAX_CACHE_SIZE,
    ttl: CACHE_TTL
  }
}

/**
 * 初始化：清理过期缓存
 */
export const initProfileCache = () => {
  cleanExpiredCache()
  const stats = getCacheStats()
  const storageType = localStorageAvailable ? 'localStorage' : '内存Map（降级模式）'
  console.log(`✅ Profile缓存初始化完成 [${storageType}]`, stats)
}

/**
 * 获取当前使用的存储类型（用于调试）
 */
export const getStorageType = () => {
  return localStorageAvailable ? 'localStorage' : 'memoryMap'
}

