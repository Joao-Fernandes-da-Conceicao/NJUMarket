package com.njumarket.commodity.repository;

import com.njumarket.commodity.entity.CommoditySnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 商品快照数据访问层
 */
@Repository
public interface CommoditySnapshotRepository extends JpaRepository<CommoditySnapshot, String> {
    
    /**
     * 根据原始商品ID查找快照
     */
    List<CommoditySnapshot> findByOriginalCommodityId(String originalCommodityId);
    
    /**
     * 根据卖家ID查找快照
     */
    List<CommoditySnapshot> findBySellerId(String sellerId);
}

