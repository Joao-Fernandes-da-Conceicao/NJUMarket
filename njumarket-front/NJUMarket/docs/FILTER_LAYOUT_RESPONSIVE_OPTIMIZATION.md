# 筛选区域响应式布局优化

## 目标
实现移动端和桌面端的筛选区域布局差异化优化

## 完成时间
2025-01-XX

## 实现的布局

### 桌面端（宽度 ≥ 901px）
**布局顺序**（从左到右，同一行）：
1. 商品种类选择器
2. 最低价格输入框
3. 最高价格输入框  
4. 排序方式选择器

**特点**：
- 所有元素横向排列在同一行
- `flex-wrap: nowrap` 防止换行
- 元素间距：20px

### 移动端（宽度 < 901px）
**布局顺序**（从上到下，分三行）：
1. 第一行：商品种类选择器
2. 第二行：最低价格输入框 + 最高价格输入框（横向排列）
3. 第三行：排序方式选择器

**特点**：
- 所有元素垂直排列，分三行
- `flex-direction: column`
- 元素间距：15px
- 价格输入框在第二行保持横向排列

## 修改的文件

### CommodityList.vue

#### HTML结构调整
- 将原来的嵌套结构（`.price-group` 和 `.selectors-group`）拆分为三个独立的顶级元素
- 新的结构：
  ```html
  <div class="filter-section">
    <!-- 商品种类选择器 -->
    <div class="custom-select">...</div>
    
    <!-- 价格区间输入框 -->
    <div class="filter-group price-group">
      <el-input ... />
      <el-input ... />
    </div>
    
    <!-- 排序方式选择器 -->
    <div class="custom-select">...</div>
  </div>
  ```

#### CSS新增和修改

**桌面端样式** (`@media (min-width: 901px)`):
```css
.filter-section {
  flex-wrap: nowrap; /* 防止换行 */
}

.filter-section > * {
  flex-shrink: 0; /* 防止压缩 */
}
```

**移动端样式** (`@media (max-width: 900px)`):
```css
.filter-section {
  flex-direction: column; /* 垂直排列 */
  gap: 15px;
}

/* 使用 order 控制显示顺序 */
.filter-section > .custom-select:first-child {
  order: 1; /* 种类选择器第一行 */
}

.price-group {
  order: 2; /* 价格输入框第二行 */
}

.filter-section > .custom-select:last-child {
  order: 3; /* 排序选择器第三行 */
}

/* 价格输入框组内部横向排列 */
.price-group {
  display: flex;
  flex-direction: row;
  gap: 10px;
}
```

## 优势

1. **响应式设计**：根据屏幕宽度自动调整布局
2. **用户体验**：桌面端充分利用横向空间，移动端避免元素过于拥挤
3. **视觉一致性**：所有筛选元素保持相同的样式（宽度130px，高度32px，圆角20px）
4. **易于维护**：使用CSS媒体查询实现，无需JavaScript判断

## 相关文档
- [搜索组件模块化](SEARCH_COMPONENT_MODULARIZATION.md)
- [前端重构计划](FRONTEND_REFACTOR_PLAN.md)
