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
 * 基于单一标准：屏幕宽度
 */
export function detectScreenType() {
  if (typeof window === 'undefined') return
  
  const width = window.innerWidth
  
  isMobile.value = width < BREAKPOINTS.mobile
  isTablet.value = width >= BREAKPOINTS.mobile && width < BREAKPOINTS.tablet
  isDesktop.value = width >= BREAKPOINTS.tablet
}

/**
 * 初始化响应式检测
 */
export function initResponsive() {
  detectScreenType()
  
  // 使用防抖处理resize事件
  let resizeTimer = null
  const debouncedDetect = () => {
    if (resizeTimer) clearTimeout(resizeTimer)
    resizeTimer = setTimeout(detectScreenType, 150)
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
 */
export function useResponsive() {
  const cleanup = ref(null)
  
  onMounted(() => {
    cleanup.value = initResponsive()
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
