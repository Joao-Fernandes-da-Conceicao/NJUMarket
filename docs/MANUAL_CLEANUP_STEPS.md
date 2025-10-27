# 手动清理翻页器样式步骤

由于自动清理工具在 Windows 环境下无法正常运行，请按照以下步骤手动清理翻页器样式。

## 需要清理的文件

1. `src/views/MyOrders.vue` （第1284-1443行）
2. `src/views/SellerOrders.vue` （约第1086行开始）
3. `src/views/MyCommodities.vue` （约第852行开始）

## 清理步骤（以 MyOrders.vue 为例）

1. 打开文件 `src/views/MyOrders.vue`
2. 搜索 `.custom-page-size-select`
3. 找到第一个 `.custom-page-size-select` 样式定义
4. 找到最后一个 `.jumper-input:focus {` 结束的花括号 `}`
5. 删除这两点之间的所有内容（包括这两行本身）
6. 保留注释：`/* 翻页器样式已移至 styles/pagination.css */`

## 验证

清理完成后，确保文件语法正确，运行：
```bash
npm run serve
```

检查翻页器是否正常显示。

## 注意事项

- 已经导入了 `pagination.css`，所以删除这些样式不会影响功能
- 只需删除 `.custom-page-size-select` 到 `.jumper-input:focus` 之间的样式
- 注意不要删除其他无关的样式
