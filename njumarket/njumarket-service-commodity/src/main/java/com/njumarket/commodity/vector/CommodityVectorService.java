package com.njumarket.commodity.vector;

import com.njumarket.commodity.entity.Commodity;

/**
 * 商品向量化服务接口
 */
public interface CommodityVectorService {
    
    /**
     * 为商品生成向量并存储
     * @param commodity 商品实体
     */
    void generateAndStoreVector(Commodity commodity);
    
    /**
     * 更新商品向量
     * @param commodity 商品实体
     */
    void updateVector(Commodity commodity);
    
    /**
     * 删除商品向量
     * @param commodityId 商品ID
     */
    void deleteVector(String commodityId);
    
    /**
     * 批量生成向量（用于历史数据迁移）
     * @param batchSize 批次大小
     */
    void batchGenerateVectors(int batchSize);
}

