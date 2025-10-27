# 订单规则工具使用指南

## 概述

`orderRules.js` 提供了统一的前端拦截逻辑，避免各页面重复实现或不一致。

## 使用场景

### 1. 商品详情页 - 检查是否可以购买

```vue
<template>
  <el-button 
    :disabled="!canBuy.commodity"
    @click="handleBuy"
  >
    {{ canBuy.reason || '立即购买' }}
  </el-button>
</template>

<script setup>
import { computed } from 'vue'
import { canCreateOrder } from '@/utils/orderRules'

const canBuy = computed(() => {
  return canCreateOrder({
    commodity: commodity.value,
    user: user.value,
    quantity: 1
  })
})
</script>
```

### 2. 创建订单页 - 验证表单

```vue
<script setup>
import { computed } from 'vue'
import { canCreateOrder, validateQuantity } from '@/utils/orderRules'

// 检查是否可以提交订单
const canSubmitOrder = computed(() => {
  const check = canCreateOrder({
    commodity: commodity.value,
    user: user.value,
    quantity: orderForm.value.quantity
  })
  return check.canOrder
})

// 验证数量
const quantityValidation = () => {
  const result = validateQuantity(
    orderForm.value.quantity,
    commodity.value.stock
  )
  if (!result.valid) {
    ElMessage.warning(result.message)
    return false
  }
  return true
}
</script>
```

### 3. 商品列表 - 显示购买按钮

```vue
<template>
  <el-button
    :disabled="!item.canBuy.valid"
    @click="handleBuy(item)"
  >
    {{ item.canBuy.reason || '立即购买' }}
  </el-button>
</template>

<script setup>
import { canPurchaseCommodity } from '@/utils/orderRules'

const items = computed(() => {
  return commodityList.value.map(item => ({
    ...item,
    canBuy: canPurchaseCommodity(item)
  }))
})
</script>
```

### 4. 订单详情页 - 操作按钮

```vue
<template>
  <el-button
    v-if="canPay.valid"
    @click="handlePay"
    type="primary"
  >
    立即支付
  </el-button>
  
  <el-button
    v-if="canConfirm.valid"
    @click="handleConfirm"
    type="success"
  >
    确认收货
  </el-button>
  
  <el-button
    v-if="canCancel.valid"
    @click="handleCancel"
  >
    取消订单
  </el-button>
</template>

<script setup>
import { computed } from 'vue'
import { 
  canPayOrder, 
  canConfirmOrder, 
  canCancelOrder 
} from '@/utils/orderRules'

const canPay = computed(() => 
  canPayOrder({ order: order.value, user: user.value })
)

const canConfirm = computed(() => 
  canConfirmOrder({ order: order.value, user: user.value })
)

const canCancel = computed(() => 
  canCancelOrder({ order: order.value, user: user.value })
)
</script>
```

### 5. 卖家订单 - 发货按钮

```vue
<script setup>
import { canShipOrder } from '@/utils/orderRules'

const canShip = computed(() => 
  canShipOrder({ order: order.value, user: user.value })
)
</script>
```

### 6. 我的商品页 - 上架/下架按钮

```vue
<template>
  <el-button
    v-if="canShelf.valid"
    @click="handleShelf"
    type="primary"
  >
    上架
  </el-button>
  
  <el-button
    v-if="canUnshelf.valid"
    @click="handleUnshelf"
  >
    下架
  </el-button>
</template>

<script setup>
import { canShelfCommodity, canUnshelfCommodity } from '@/utils/orderRules'

const canShelf = computed(() => 
  canShelfCommodity(commodity.value)
)

const canUnshelf = computed(() => 
  canUnshelfCommodity(commodity.value)
)
</script>
```

## 可用函数

### 订单相关

- `canCreateOrder({ commodity, user, quantity })` - 检查是否可以创建订单
- `canPayOrder({ order, user })` - 检查是否可以支付订单
- `canConfirmOrder({ order, user })` - 检查是否可以确认收货
- `canCancelOrder({ order, user })` - 检查是否可以取消订单
- `canShipOrder({ order, user })` - 检查是否可以发货
- `canApplyRefund({ order, user })` - 检查是否可以申请退款

### 商品相关

- `canPurchaseCommodity(commodity)` - 检查商品是否可以购买
- `canContactSeller({ commodity, user })` - 检查是否可以联系卖家
- `canShelfCommodity(commodity)` - 检查是否可以上架商品
- `canUnshelfCommodity(commodity)` - 检查是否可以下架商品
- `getCommodityStatusText(status)` - 获取商品状态文本
- `getCommodityStatusType(status)` - 获取商品状态类型

### 工具函数

- `validateQuantity(quantity, stock)` - 验证购买数量

## 返回格式

所有函数都返回以下格式：

```javascript
{
  canOrder: boolean,  // 或 canPay, canConfirm 等
  reason: string      // 错误原因，空字符串表示可以操作
}
```

或验证函数返回：

```javascript
{
  valid: boolean,
  message: string
}
```

## 优势

1. **统一性** - 所有页面使用相同的规则
2. **可维护性** - 规则集中管理，修改一处生效
3. **一致性** - 避免各页面实现不一致
4. **可测试性** - 纯函数，易于单元测试
5. **安全性** - 前端拦截减少恶意请求

## 注意事项

- 前端拦截只是用户体验优化，**真正的安全由后端保证**
- 前后端规则应该保持一致
- 修改规则时需同步更新后端逻辑

