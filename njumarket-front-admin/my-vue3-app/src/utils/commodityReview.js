export function reviewCommodityPayload(p){
  if (!p) return '无效的提交数据'
  if (!p.commodityId) return '缺少商品ID'
  if (p.price !== '' && p.price !== undefined){ const v = Number(p.price); if (isNaN(v) || v < 0) return '价格必须为非负数' }
  if (p.stock !== '' && p.stock !== undefined){ const v = Number(p.stock); if (!Number.isInteger(v) || v < 0) return '库存必须为非负整数' }
  if (p.commodityStatus){
    const v = String(p.commodityStatus).toUpperCase()
    const allowed = ['DRAFT','PUBLISHED','ON_SHELF','OFF_SHELF']
    if (!allowed.includes(v)) return '商品状态不合法（DRAFT/PUBLISHED/ON_SHELF/OFF_SHELF）'
    p.commodityStatus = v
  }
  if (p.conditionLevel){
    const lvl = String(p.conditionLevel).trim()
    const allowedLvl = ['全新','九成新','八成新','七成新','六成新','五成新','中古']
    if (!allowedLvl.includes(lvl)) return '成色等级不合法（全新/九成新/八成新/七成新/六成新/五成新）'
    p.conditionLevel = lvl
  }
  if (p.category){
    const cat = String(p.category).trim()
    const allowedCat = ['电子产品','服装配饰','图书文具','生活用品','运动户外','美妆护肤','其他']
    if (!allowedCat.includes(cat)) return '分类不合法（电子产品/服装配饰/图书文具/生活用品/运动户外/美妆护肤/其他）'
    p.category = cat
  }
  if (p.sellerVisibility){
    const v = String(p.sellerVisibility).toUpperCase()
    const allowedVis = ['PUBLIC','PRIVATE','HIDDEN']
    if (!allowedVis.includes(v)) return '卖家可见性不合法（PUBLIC/PRIVATE/HIDDEN）'
    p.sellerVisibility = v
  }
  if (p.buyerVisibility){
    const v = String(p.buyerVisibility).toUpperCase()
    const allowedVis = ['PUBLIC','PRIVATE','HIDDEN']
    if (!allowedVis.includes(v)) return '买家可见性不合法（PUBLIC/PRIVATE/HIDDEN）'
    p.buyerVisibility = v
  }
  return ''
}

