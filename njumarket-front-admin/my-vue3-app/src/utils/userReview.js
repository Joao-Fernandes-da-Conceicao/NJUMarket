export function reviewUserPayload(p){
  if (!p) return '无效的提交数据'
  if (!p.userId) return '缺少用户ID'
  if (p.username && p.username.length < 3) return '用户名至少3个字符'
  if (p.primaryPhone && !/^1[3-9]\d{9}$/.test(p.primaryPhone)) return '手机号格式不正确'
  if (p.accountStatus){
    const v = String(p.accountStatus).toUpperCase()
    const allowed = ['ACTIVE','SUSPENDED','BANNED']
    if (!allowed.includes(v)) return '账户状态不合法（ACTIVE/SUSPENDED/BANNED）'
    p.accountStatus = v
  }
  if (p.creditScore !== '' && p.creditScore !== undefined){
    const v = Number(p.creditScore); if (isNaN(v) || v < 0 || v > 100) return '信用分应在0-100之间'
  }
  const ratingKeys = ['buyerRating','sellerRating']
  for (const k of ratingKeys){
    if (p[k] !== '' && p[k] !== undefined){ const v = Number(p[k]); if (isNaN(v) || v < 0 || v > 5) return '评分应在0-5之间' }
  }
  const intKeys = ['totalSales','totalPurchases']
  for (const k of intKeys){
    if (p[k] !== '' && p[k] !== undefined){ const v = Number(p[k]); if (!Number.isInteger(v) || v < 0) return '成交统计应为非负整数' }
  }
  if (p.vipLevel){
    const v = String(p.vipLevel).toUpperCase()
    const allowedLvl = ['NORMAL','BRONZE','SILVER','GOLD','PLATINUM']
    if (!allowedLvl.includes(v)) return '会员等级不合法（NORMAL/BRONZE/SILVER/GOLD/PLATINUM）'
    p.vipLevel = v
  }
  return ''
}

