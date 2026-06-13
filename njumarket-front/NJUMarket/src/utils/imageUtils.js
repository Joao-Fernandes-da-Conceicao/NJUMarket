import { imageAPI } from '../api'

/**
 * 获取头像URL
 * @param {string} avatarUrl - 头像URL或文件名
 * @returns {string} 完整的头像URL
 */
export const getAvatarUrl = (avatarUrl) => {
  if (!avatarUrl) return 'http://localhost:8080/api/images/avatars/default'
  if (avatarUrl.startsWith('http')) return avatarUrl
  // 以 / 开头的相对路径 → 补全协议和域名
  if (avatarUrl.startsWith('/')) return `http://localhost:8080${avatarUrl}`
  // 纯文件名
  return `http://localhost:8080/api/images/avatars/${avatarUrl}`
}

/**
 * 获取商品图片URL
 * @param {string} imageUrl - 商品图片URL或文件名
 * @returns {string} 完整的商品图片URL
 */
export const getCommodityImageUrl = (imageUrl) => {
  if (!imageUrl) return imageAPI.getDefaultCommodityImage()
  if (imageUrl.startsWith('http')) return imageUrl
  if (imageUrl.startsWith('/')) return `http://localhost:8080${imageUrl}`
  // 逗号分隔的多图片字符串 → 取第一个
  if (imageUrl.includes(',')) {
    imageUrl = imageUrl.split(',')[0].trim()
  }
  // 纯文件名
  return imageAPI.getCommodityImage(imageUrl)
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
