# 响应式断点使用指南

## 概述

为了统一管理响应式布局的断点阈值，项目创建了 `config/breakpoints.js` 文件，集中管理所有横向分辨率断点。

## 断点常量

### 定义位置
- **文件**: `src/config/breakpoints.js`
- **CSS变量**: `src/App.vue` 中的 `:root` 样式

### 可用断点

```javascript
// 移动端断点：小于此宽度视为移动端
BREAKPOINT_MOBILE = 900

// 宽屏断点：大于等于此宽度启用宽屏布局
BREAKPOINT_WIDE = 1200

// 超宽屏断点：大于等于此宽度启用超宽屏布局
BREAKPOINT_ULTRA_WIDE = 1600
```

## 使用方法

### 1. 在 JavaScript/Vue 代码中使用

```javascript
import { BREAKPOINT_MOBILE, BREAKPOINT_WIDE, BREAKPOINT_ULTRA_WIDE } from '@/config/breakpoints'

// 动态检测屏幕宽度
const width = window.innerWidth
if (width < BREAKPOINT_MOBILE) {
  // 移动端逻辑
}
if (width >= BREAKPOINT_WIDE) {
  // 宽屏逻辑
}
if (width >= BREAKPOINT_ULTRA_WIDE) {
  // 超宽屏逻辑
}
```

### 2. 在 CSS 中使用（通过 CSS 变量）

```css
/* 使用 CSS 变量 */
@media (max-width: var(--breakpoint-mobile)) {
  /* 移动端样式 */
}

@media (min-width: var(--breakpoint-wide)) {
  /* 宽屏样式 */
}

@media (min-width: var(--breakpoint-ultra-wide)) {
  /* 超宽屏样式 */
}
```

### 3. 在 Vue 组件的 `<style>` 中使用

```vue
<style scoped>
/* 移动端样式 */
@media (max-width: 900px) {
  /* 注意：Vue 的 scoped style 中无法直接使用 CSS 变量作为媒体查询值 */
  /* 需要使用硬编码值，但应该从 breakpoints.js 中引用相同的常量 */
}

/* 宽屏样式 */
@media (min-width: 1200px) {
  /* 同上 */
}
</style>
```

**注意**: 由于 CSS 媒体查询的限制，Vue 的 scoped style 中无法直接使用 CSS 变量作为媒体查询值。建议：
- 在 `<style scoped>` 中使用硬编码值时，确保与 `breakpoints.js` 中的常量一致
- 或者将响应式样式提取到全局样式文件，使用 CSS 变量

### 4. 使用 responsive.js 工具函数

```javascript
import { useResponsive, BREAKPOINTS } from '@/config/responsive'

const { isMobile, isTablet, isDesktop } = useResponsive()

// 或者直接使用 BREAKPOINTS
if (window.innerWidth < BREAKPOINTS.mobile) {
  // ...
}
```

## 已迁移的文件

- ✅ `src/App.vue` - CSS 变量定义
- ✅ `src/config/responsive.js` - 统一响应式检测中心（已合并原 `utils/responsive.js` 的功能）
- ✅ `src/components/layout/AppHeader.vue` - 使用 BREAKPOINT_MOBILE

## 待迁移的媒体查询

以下文件中的媒体查询应该使用统一的断点常量（CSS 中的硬编码值应与 breakpoints.js 保持一致）：

- `src/views/OrderDetail.vue` - `@media (min-width: 1200px)` 和 `@media (max-width: 900px)`
- `src/views/PublishCommodity.vue` - `@media (min-width: 1200px)` 和 `@media (max-width: 900px)`
- `src/views/CreateOrder.vue` - `@media (min-width: 1200px)`
- `src/views/Home.vue` - `@media (min-width: 1600px)` 和 `@media (max-width: 900px)`
- 以及其他所有使用 `900px`、`1200px`、`1600px` 的文件

## 维护建议

1. **修改断点时**：只需要在 `config/breakpoints.js` 中修改常量值
2. **添加新断点时**：在 `breakpoints.js` 中添加新常量，并更新本文档
3. **保持一致性**：确保 CSS 硬编码值与 JavaScript 常量值一致
4. **代码审查**：提交代码时检查是否有新的硬编码断点值

## 最佳实践

1. ✅ **优先使用常量**：在 JavaScript 代码中始终使用导入的常量
2. ✅ **注释说明**：在 CSS 中使用硬编码值时，添加注释说明对应的常量
3. ✅ **统一命名**：使用统一的命名约定（如 `BREAKPOINT_*`）
4. ❌ **避免硬编码**：不要在代码中直接写 `900`、`1200`、`1600` 等数字

## 示例：在组件中使用

```vue
<script setup>
import { BREAKPOINT_MOBILE, BREAKPOINT_WIDE } from '@/config/breakpoints'
import { computed, onMounted, onUnmounted } from 'vue'

const isMobile = computed(() => {
  if (typeof window === 'undefined') return false
  return window.innerWidth < BREAKPOINT_MOBILE
})

const isWide = computed(() => {
  if (typeof window === 'undefined') return false
  return window.innerWidth >= BREAKPOINT_WIDE
})
</script>

<template>
  <div>
    <div v-if="isMobile">移动端内容</div>
    <div v-else-if="isWide">宽屏内容</div>
    <div v-else>桌面端内容</div>
  </div>
</template>

<style scoped>
/* 移动端样式 - 对应 BREAKPOINT_MOBILE (900px) */
@media (max-width: 900px) {
  /* ... */
}

/* 宽屏样式 - 对应 BREAKPOINT_WIDE (1200px) */
@media (min-width: 1200px) {
  /* ... */
}
</style>
```

