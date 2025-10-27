/**
 * 响应式布局检测工具
 * 
 * 判断逻辑：
 * 1. 基于纵横比：横向/纵向 < 1.5 认为是竖屏设备
 * 2. 考虑设备物理尺寸：宽度<900且高度>宽度*1.5推断为小设备
 * 3. 小于8英寸的设备优先按手机布局
 */

import { ref, onMounted, onUnmounted } from 'vue'
import { BREAKPOINT_MOBILE } from '../config/breakpoints'

export const isMobile = ref(false)

/**
 * 检测是否为移动设备
 * 优先使用纵横比和设备尺寸判断，而不是固定的768px
 */
export function detectMobile() {
  if (typeof window === 'undefined') return
  
  const width = window.innerWidth
  const height = window.innerHeight
  const userAgent = navigator.userAgent.toLowerCase()
  
  // 判断纵横比：横向/纵向 < 1.5 认为是竖屏设备（手机或平板竖屏）
  const aspectRatio = width / height
  const isVerticalOrientation = aspectRatio < 1.5 // 4:3约为1.33，设置为1.5略大于4:3
  
  // 判断设备尺寸：检查viewport元标签或使用推断方法
  // 简单推断：如果高分辨率且分辨率较小，可能是手机
  // 例如：iPhone 13 390x844 约6.1英寸
  // 如果逻辑像素宽度小于移动端断点且高度较大，可能是手机
  let isSmallDevice = false
  if (width < BREAKPOINT_MOBILE && height > width * 1.5) {
    isSmallDevice = true
  }
  
  // 判断逻辑：
  // 1. 如果分辨率宽度小于移动端断点，肯定是手机
  // 2. 如果纵横比<1.5且设备较小或UA包含移动设备标识，按手机布局
  const isMobileWidth = width < BREAKPOINT_MOBILE
  const isMobileUA = /mobile|android|iphone|ipad|phone/i.test(userAgent)
  
  isMobile.value = isMobileWidth || (isVerticalOrientation && (isMobileUA || isSmallDevice))
}

/**
 * 初始化响应式检测
 */
export function initResponsive() {
  detectMobile()
  
  // 使用防抖处理resize事件
  let resizeTimer = null
  const debouncedDetect = () => {
    if (resizeTimer) clearTimeout(resizeTimer)
    resizeTimer = setTimeout(detectMobile, 150)
  }
  
  window.addEventListener('resize', debouncedDetect)
  window.addEventListener('orientationchange', debouncedDetect)
  
  return () => {
    window.removeEventListener('resize', debouncedDetect)
    window.removeEventListener('orientationchange', debouncedDetect)
  }
}

/**
 * 在组件中使用响应式检测的组合式函数
 */
export function useResponsive() {
  let cleanup = null
  
  onMounted(() => {
    cleanup = initResponsive()
  })
  
  onUnmounted(() => {
    if (cleanup) cleanup()
  })
  
  return { isMobile }
}


