import http from '../http'

export const elasticsearchAPI = {
  // 重建搜索索引
  reindex: () => http.post('/elasticsearch/reindex'),
  
  // 同步指定商品到搜索索引
  syncCommodity: (commodityId) => http.post(`/elasticsearch/sync/${commodityId}`),
  
  // 获取索引统计信息
  getStats: () => http.get('/elasticsearch/stats')
}

