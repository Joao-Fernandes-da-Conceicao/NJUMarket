import { imageAPI } from '../api'

/**
 * 获取头像URL
 * @param {string} avatarUrl - 头像URL或文件名
 * @returns {string} 完整的头像URL
 */
export const getAvatarUrl = (avatarUrl) => {
  if (!avatarUrl) return imageAPI.getDefaultAvatar()
  
  // 如果已经是完整URL，直接返回
  if (avatarUrl.startsWith('http')) return avatarUrl
  
  // 如果是文件名，构建完整URL
  if (avatarUrl.includes('/')) return avatarUrl
  
  // 从URL中提取文件名
  const fileName = avatarUrl.split('/').pop()
  return imageAPI.getAvatar(fileName)
}

/**
 * 获取商品图片URL
 * @param {string} imageUrl - 商品图片URL或文件名
 * @returns {string} 完整的商品图片URL
 */
export const getCommodityImageUrl = (imageUrl) => {
  if (!imageUrl) return imageAPI.getDefaultCommodityImage()
  
  // 如果已经是完整URL，直接返回
  if (imageUrl.startsWith('http')) return imageUrl
  
  // 如果是文件名，构建完整URL
  if (imageUrl.includes('/')) return imageUrl
  
  // 从URL中提取文件名
  const fileName = imageUrl.split('/').pop()
  return imageAPI.getCommodityImage(fileName)
}

/**
 * 处理图片加载错误
 * @param {Event} event - 图片加载错误事件
 * @param {string} type - 图片类型 ('avatar' | 'commodity')
 */
export const handleImageError = (event, type = 'commodity') => {
  if (type === 'avatar') {
    event.target.src = imageAPI.getDefaultAvatar()
  } else {
    event.target.src = imageAPI.getDefaultCommodityImage()
  }
}
