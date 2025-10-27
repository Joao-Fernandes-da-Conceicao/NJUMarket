/**
 * 响应式布局断点常量
 * 
 * 统一管理所有响应式断点，确保CSS媒体查询和JavaScript动态检测使用相同的值
 */

// 移动端断点：小于此宽度视为移动端
export const BREAKPOINT_MOBILE = 900

// 宽屏断点：大于等于此宽度启用宽屏布局
export const BREAKPOINT_WIDE = 1200

// 超宽屏断点：大于等于此宽度启用超宽屏布局
export const BREAKPOINT_ULTRA_WIDE = 1600

// 断点媒体查询字符串（用于CSS变量和内联样式）
export const BREAKPOINT_MEDIA_QUERIES = {
  mobile: `@media (max-width: ${BREAKPOINT_MOBILE}px)`,
  wide: `@media (min-width: ${BREAKPOINT_WIDE}px)`,
  ultraWide: `@media (min-width: ${BREAKPOINT_ULTRA_WIDE}px)`,
  mobileOnly: `@media (max-width: ${BREAKPOINT_MOBILE}px)`,
  desktop: `@media (min-width: ${BREAKPOINT_MOBILE + 1}px)`,
  wideOnly: `@media (min-width: ${BREAKPOINT_WIDE}px) and (max-width: ${BREAKPOINT_ULTRA_WIDE - 1}px)`,
  ultraWideOnly: `@media (min-width: ${BREAKPOINT_ULTRA_WIDE}px)`
}

// 默认导出
export default {
  BREAKPOINT_MOBILE,
  BREAKPOINT_WIDE,
  BREAKPOINT_ULTRA_WIDE,
  BREAKPOINT_MEDIA_QUERIES
}

