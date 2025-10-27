/**
 * 订单规则工具 - 统一的前端拦截逻辑
 * 
 * 用于统一管理订单相关的业务规则，避免各页面重复实现或不一致
 */

/**
 * 检查是否可以创建订单
 * @param {Object} options - 选项对象
 * @param {Object} options.commodity - 商品对象
 * @param {Object} options.user - 当前用户对象
 * @param {number} options.quantity - 购买数量
 * @returns {Object} { canOrder: boolean, reason: string }
 */
export function canCreateOrder({ commodity, user, quantity = 1 }) {
  // 1. 用户必须登录
  if (!user) {
    return {
      canOrder: false,
      reason: '请先登录'
    }
  }

  // 2. 商品必须存在
  if (!commodity) {
    return {
      canOrder: false,
      reason: '商品不存在'
    }
  }

  // 3. 商品必须是上架状态（仅ON_SHELF可购买）
  if (commodity.commodityStatus !== 'ON_SHELF') {
    return {
      canOrder: false,
      reason: '商品未上架，无法购买'
    }
  }

  // 4. 商品库存必须大于0
  if (!commodity.stock || commodity.stock <= 0) {
    return {
      canOrder: false,
      reason: '商品已售罄'
    }
  }

  // 5. 购买数量必须大于0且不超过库存
  const quantityNum = Number(quantity) || 0
  if (quantityNum <= 0) {
    return {
      canOrder: false,
      reason: '购买数量必须大于0'
    }
  }

  if (quantityNum > commodity.stock) {
    return {
      canOrder: false,
      reason: `购买数量不能超过库存 ${commodity.stock}`
    }
  }

  // 6. 不能购买自己的商品
  if (commodity.sellerId === user.userId) {
    return {
      canOrder: false,
      reason: '不能购买自己的商品'
    }
  }

  return {
    canOrder: true,
    reason: ''
  }
}

/**
 * 检查是否可以联系卖家
 * @param {Object} options - 选项对象
 * @param {Object} options.commodity - 商品对象
 * @param {Object} options.user - 当前用户对象
 * @returns {Object} { canContact: boolean, reason: string }
 */
export function canContactSeller({ commodity, user }) {
  // 1. 用户必须登录
  if (!user) {
    return {
      canContact: false,
      reason: '请先登录'
    }
  }

  // 2. 商品必须存在
  if (!commodity) {
    return {
      canContact: false,
      reason: '商品不存在'
    }
  }

  // 3. 不能联系自己的商品
  if (commodity.sellerId === user.userId) {
    return {
      canContact: false,
      reason: '自己的商品'
    }
  }

  return {
    canContact: true,
    reason: ''
  }
}

/**
 * 检查商品是否可以购买（简化版）
 * @param {Object} commodity - 商品对象
 * @returns {Object} { canPurchase: boolean, reason: string }
 */
export function canPurchaseCommodity(commodity) {
  // 商品不存在
  if (!commodity) {
    return {
      canPurchase: false,
      reason: '商品不存在'
    }
  }

  // 商品未上架
  if (commodity.commodityStatus !== 'ON_SHELF') {
    return {
      canPurchase: false,
      reason: '商品未上架'
    }
  }

  // 库存不足
  if (!commodity.stock || commodity.stock <= 0) {
    return {
      canPurchase: false,
      reason: '商品已售罄'
    }
  }

  return {
    canPurchase: true,
    reason: ''
  }
}

/**
 * 验证订单数量
 * @param {number} quantity - 购买数量
 * @param {number} stock - 库存数量
 * @returns {Object} { valid: boolean, message: string }
 */
export function validateQuantity(quantity, stock) {
  const quantityNum = Number(quantity) || 0

  if (quantityNum <= 0) {
    return {
      valid: false,
      message: '购买数量必须大于0'
    }
  }

  if (quantityNum > stock) {
    return {
      valid: false,
      message: `购买数量不能超过库存 ${stock}`
    }
  }

  return {
    valid: true,
    message: ''
  }
}

/**
 * 获取商品状态描述
 * @param {string} status - 商品状态
 * @returns {string} 状态描述
 */
export function getCommodityStatusText(status) {
  const statusMap = {
    'DRAFT': '草稿',
    'PUBLISHED': '已发布',
    'ON_SHELF': '已上架',
    'OFF_SHELF': '已下架',
    'SOLD_OUT': '已售罄'
  }
  return statusMap[status] || status
}

/**
 * 获取商品状态类型（用于标签颜色）
 * @param {string} status - 商品状态
 * @returns {string} 标签类型
 */
export function getCommodityStatusType(status) {
  const statusMap = {
    'DRAFT': 'info',
    'PUBLISHED': 'warning',
    'ON_SHELF': 'success',
    'OFF_SHELF': 'info',
    'SOLD_OUT': 'danger'
  }
  return statusMap[status] || 'info'
}

/**
 * 检查是否可以上架商品
 * @param {Object} commodity - 商品对象
 * @returns {Object} { canShelf: boolean, reason: string }
 */
export function canShelfCommodity(commodity) {
  if (!commodity) {
    return {
      canShelf: false,
      reason: '商品不存在'
    }
  }

  // 只有草稿状态的商品可以上架
  if (commodity.commodityStatus !== 'DRAFT') {
    return {
      canShelf: false,
      reason: '只能上架草稿状态的商品'
    }
  }

  // 库存必须大于0
  if (!commodity.stock || commodity.stock <= 0) {
    return {
      canShelf: false,
      reason: '库存不足，无法上架'
    }
  }

  return {
    canShelf: true,
    reason: ''
  }
}

/**
 * 检查是否可以下架商品
 * @param {Object} commodity - 商品对象
 * @returns {Object} { canUnshelf: boolean, reason: string }
 */
export function canUnshelfCommodity(commodity) {
  if (!commodity) {
    return {
      canUnshelf: false,
      reason: '商品不存在'
    }
  }

  // 只有已上架的商品可以下架
  if (commodity.commodityStatus !== 'ON_SHELF') {
    return {
      canUnshelf: false,
      reason: '只能下架已上架的商品'
    }
  }

  return {
    canUnshelf: true,
    reason: ''
  }
}

/**
 * 订单状态规则 - 检查是否可以支付
 * @param {Object} order - 订单对象
 * @param {Object} user - 当前用户对象
 * @returns {Object} { canPay: boolean, reason: string }
 */
export function canPayOrder({ order, user }) {
  if (!user) {
    return {
      canPay: false,
      reason: '请先登录'
    }
  }

  if (!order) {
    return {
      canPay: false,
      reason: '订单不存在'
    }
  }

  // 只有买家可以支付
  if (order.buyerId !== user.userId) {
    return {
      canPay: false,
      reason: '无权限支付此订单'
    }
  }

  // 只有CREATED状态的订单可以支付
  if (order.orderStatus !== 'CREATED') {
    return {
      canPay: false,
      reason: '订单状态异常，无法支付'
    }
  }

  return {
    canPay: true,
    reason: ''
  }
}

/**
 * 订单状态规则 - 检查是否可以确认收货
 * @param {Object} order - 订单对象
 * @param {Object} user - 当前用户对象
 * @returns {Object} { canConfirm: boolean, reason: string }
 */
export function canConfirmOrder({ order, user }) {
  if (!user) {
    return {
      canConfirm: false,
      reason: '请先登录'
    }
  }

  if (!order) {
    return {
      canConfirm: false,
      reason: '订单不存在'
    }
  }

  // 只有买家可以确认收货
  if (order.buyerId !== user.userId) {
    return {
      canConfirm: false,
      reason: '无权限确认收货此订单'
    }
  }

  // 只有SHIPPED状态的订单可以确认收货
  if (order.orderStatus !== 'SHIPPED') {
    return {
      canConfirm: false,
      reason: '订单状态异常，无法确认收货'
    }
  }

  return {
    canConfirm: true,
    reason: ''
  }
}

/**
 * 订单状态规则 - 检查是否可以取消订单
 * @param {Object} order - 订单对象
 * @param {Object} user - 当前用户对象
 * @returns {Object} { canCancel: boolean, reason: string }
 */
export function canCancelOrder({ order, user }) {
  if (!user) {
    return {
      canCancel: false,
      reason: '请先登录'
    }
  }

  if (!order) {
    return {
      canCancel: false,
      reason: '订单不存在'
    }
  }

  // 只有买家或卖家可以取消订单
  const isBuyer = order.buyerId === user.userId
  const isSeller = order.sellerId === user.userId
  if (!isBuyer && !isSeller) {
    return {
      canCancel: false,
      reason: '无权限取消此订单'
    }
  }

  // 只有CREATED或PAID状态的订单可以取消
  if (!['CREATED', 'PAID'].includes(order.orderStatus)) {
    return {
      canCancel: false,
      reason: '订单状态异常，无法取消'
    }
  }

  return {
    canCancel: true,
    reason: ''
  }
}

/**
 * 订单状态规则 - 检查是否可以发货
 * @param {Object} order - 订单对象
 * @param {Object} user - 当前用户对象
 * @returns {Object} { canShip: boolean, reason: string }
 */
export function canShipOrder({ order, user }) {
  if (!user) {
    return {
      canShip: false,
      reason: '请先登录'
    }
  }

  if (!order) {
    return {
      canShip: false,
      reason: '订单不存在'
    }
  }

  // 只有卖家可以发货
  if (order.sellerId !== user.userId) {
    return {
      canShip: false,
      reason: '无权限发货此订单'
    }
  }

  // 只有PAID状态的订单可以发货
  if (order.orderStatus !== 'PAID') {
    return {
      canShip: false,
      reason: '订单状态异常，无法发货'
    }
  }

  return {
    canShip: true,
    reason: ''
  }
}

/**
 * 订单状态规则 - 检查是否可以申请退款
 * @param {Object} order - 订单对象
 * @param {Object} user - 当前用户对象
 * @returns {Object} { canRefund: boolean, reason: string }
 */
export function canApplyRefund({ order, user }) {
  if (!user) {
    return {
      canRefund: false,
      reason: '请先登录'
    }
  }

  if (!order) {
    return {
      canRefund: false,
      reason: '订单不存在'
    }
  }

  // 只有买家可以申请退款
  if (order.buyerId !== user.userId) {
    return {
      canRefund: false,
      reason: '无权限申请退款'
    }
  }

  // 只有PAID或SHIPPED状态的订单可以申请退款
  if (!['PAID', 'SHIPPED'].includes(order.orderStatus)) {
    return {
      canRefund: false,
      reason: '订单状态异常，无法申请退款'
    }
  }

  // 已经申请过退款的不允许重复申请
  if (order.orderStatus === 'REFUND_REQUESTED') {
    return {
      canRefund: false,
      reason: '退款申请已提交，请等待处理'
    }
  }

  return {
    canRefund: true,
    reason: ''
  }
}

