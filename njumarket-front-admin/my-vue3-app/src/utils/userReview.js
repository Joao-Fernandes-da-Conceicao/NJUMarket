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
  return ''
}

