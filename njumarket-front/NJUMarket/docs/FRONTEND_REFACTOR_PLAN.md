# 前端项目重构计划

## 问题分析

当前前端项目存在的主要问题：

1. **多重响应式检测机制**
   - `utils/responsive.js` 有一套复杂的检测逻辑
   - `AppHeader.vue` 有独立的检测逻辑
   - 各页面有不同的媒体查询断点（768px、900px等）
   
2. **硬编码问题**
   - 断点值分散在多个文件中
   - 魔法数字（280px、320px、1600px等）
   - CSS 变量滥用

3. **CSS 重复**
   - 翻页器样式在每个页面重复定义
   - 商品卡片样式重复
   - 响应式规则分散

## 重构方案

### 第一阶段：统一配置中心 ✅

创建 `config/responsive.js`，集中管理：
- 所有断点（BREAKPOINTS）
- 响应式状态（isMobile, isTablet, isDesktop）
- 统一的检测函数

### 第二阶段：重构 CSS 体系

#### 2.1 统一样式变量
在 `App.vue` 中定义：
```css
:root {
  --breakpoint-mobile: 900px;
  --breakpoint-tablet: 1024px;
  --container-max-width: 1400px;
  
  /* 统一间距 */
  --spacing-xs: 8px;
  --spacing-sm: 12px;
  --spacing-md: 20px;
  --spacing-lg: 32px;
  
  /* 统一圆角 */
  --radius-sm: 12px;
  --radius-md: 16px;
  --radius-lg: 20px;
  --radius-pill: 20px;
}
```

#### 2.2 提取共用组件样式
创建共享样式文件：
- `styles/pagination.css` - 翻页器样式
- `styles/commodity-card.css` - 商品卡片样式
- `styles/layout.css` - 布局样式

### 第三阶段：简化组件逻辑

移除各组件中的重复检测逻辑，统一使用 `useResponsive()`

### 第四阶段：媒体查询优化

统一使用变量：
```css
@media (max-width: var(--breakpoint-mobile)) {
  /* 移动端样式 */
}
```

## 实施步骤

### 第一阶段：基础设施 ✅

1. ✅ **已完成**：创建统一配置中心 (`config/responsive.js`)
2. ✅ **已完成**：更新 App.vue 的 CSS 变量系统
3. ✅ **已完成**：创建翻页器统一样式 (`styles/pagination.css`)

### 第二阶段：应用统一样式 🔄

**下一步计划**：
1. 在需要翻页器的页面中导入 `pagination.css`
2. 移除各页面中重复的翻页器样式定义
3. 测试各个页面确保正常工作

**待处理页面**：
- CommodityList.vue
- MyOrders.vue
- SellerOrders.vue
- MyCommodities.vue

### 第三阶段：响应式检测统一

1. 替换各页面中的响应式检测逻辑
2. 统一使用 `config/responsive.js` 中的工具

### 第四阶段：媒体查询优化

1. 统一使用 CSS 变量进行媒体查询
2. 清理重复的响应式规则

## 注意事项

- 保持向后兼容
- 每次改动后测试各个页面
- 确保移动端和桌面端都正常工作
