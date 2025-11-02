/**
 * 响应式配置中心
 * 统一管理所有响应式相关的配置和工具
 */

import { ref, onMounted, onUnmounted } from 'vue'
import { BREAKPOINT_MOBILE, BREAKPOINT_WIDE, BREAKPOINT_ULTRA_WIDE } from './breakpoints'

// 统一的断点配置（从 breakpoints.js 导入以确保一致性）
export const BREAKPOINTS = {
  mobile: BREAKPOINT_MOBILE, // 移动端断点：900px
  wide: BREAKPOINT_WIDE, // 宽屏断点：1200px
  ultraWide: BREAKPOINT_ULTRA_WIDE, // 超宽屏断点：1600px
  tablet: 1024, // 平板断点（保留向后兼容）
  desktop: 1400, // 桌面断点（保留向后兼容）
}

// 响应式状态
export const isMobile = ref(false)
export const isTablet = ref(false)
export const isDesktop = ref(false)

/**
 * 检测当前屏幕类型
 * 支持两种模式：
 * 1. 简单模式：仅基于屏幕宽度（默认）
 * 2. 智能模式：基于宽度、纵横比、设备标识等综合判断（用于复杂场景）
 * 
 * @param {boolean} useSmartDetection - 是否使用智能检测模式
 */
export function detectScreenType(useSmartDetection = false) {
  if (typeof window === 'undefined') return
  
  const width = window.innerWidth
  
  // 简单模式：仅基于宽度（推荐使用）
  if (!useSmartDetection) {
    isMobile.value = width < BREAKPOINTS.mobile
    isTablet.value = width >= BREAKPOINTS.mobile && width < BREAKPOINTS.tablet
    isDesktop.value = width >= BREAKPOINTS.tablet
    return
  }
  
  // 智能模式：综合判断（兼容旧逻辑）
  const height = window.innerHeight
  const userAgent = navigator.userAgent.toLowerCase()
  
  // 判断纵横比：横向/纵向 < 1.5 认为是竖屏设备
  const aspectRatio = width / height
  const isVerticalOrientation = aspectRatio < 1.5
  
  // 判断设备尺寸
  let isSmallDevice = false
  if (width < BREAKPOINTS.mobile && height > width * 1.5) {
    isSmallDevice = true
  }
  
  // 综合判断
  const isMobileWidth = width < BREAKPOINTS.mobile
  const isMobileUA = /mobile|android|iphone|ipad|phone/i.test(userAgent)
  
  isMobile.value = isMobileWidth || (isVerticalOrientation && (isMobileUA || isSmallDevice))
  isTablet.value = !isMobile.value && width >= BREAKPOINTS.mobile && width < BREAKPOINTS.tablet
  isDesktop.value = !isMobile.value && !isTablet.value
}

/**
 * 检测移动设备（兼容旧 API，使用智能检测）
 * @deprecated 推荐使用 detectScreenType()，此函数保留以兼容旧代码
 */
export function detectMobile() {
  detectScreenType(true)
}

/**
 * 初始化响应式检测
 * @param {boolean} useSmartDetection - 是否使用智能检测模式
 */
export function initResponsive(useSmartDetection = false) {
  detectScreenType(useSmartDetection)
  
  // 使用防抖处理resize事件
  let resizeTimer = null
  const debouncedDetect = () => {
    if (resizeTimer) clearTimeout(resizeTimer)
    resizeTimer = setTimeout(() => detectScreenType(useSmartDetection), 150)
  }
  
  window.addEventListener('resize', debouncedDetect)
  window.addEventListener('orientationchange', debouncedDetect)
  
  return () => {
    window.removeEventListener('resize', debouncedDetect)
    window.removeEventListener('orientationchange', debouncedDetect)
  }
}

/**
 * 组合式函数：在组件中使用响应式检测
 * @param {boolean} useSmartDetection - 是否使用智能检测模式
 */
export function useResponsive(useSmartDetection = false) {
  const cleanup = ref(null)
  
  onMounted(() => {
    cleanup.value = initResponsive(useSmartDetection)
  })
  
  onUnmounted(() => {
    if (cleanup.value) cleanup.value()
  })
  
  return { isMobile, isTablet, isDesktop }
}

/**
 * CSS媒体查询辅助函数
 */
export const mediaQuery = {
  mobile: `(max-width: ${BREAKPOINTS.mobile - 1}px)`,
  tablet: `(min-width: ${BREAKPOINTS.mobile}px) and (max-width: ${BREAKPOINTS.tablet - 1}px)`,
  desktop: `(min-width: ${BREAKPOINTS.tablet}px)`,
  wide: `(min-width: ${BREAKPOINTS.wide}px)`,
  ultraWide: `(min-width: ${BREAKPOINTS.ultraWide}px)`,
}

// 默认导出
export default {
  BREAKPOINTS,
  isMobile,
  isTablet,
  isDesktop,
  detectScreenType,
  initResponsive,
  useResponsive,
  mediaQuery,
}
