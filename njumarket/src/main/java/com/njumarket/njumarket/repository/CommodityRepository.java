package com.njumarket.njumarket.repository;

import com.njumarket.njumarket.entity.Commodity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 商品数据访问层
 */
@Repository
public interface CommodityRepository extends JpaRepository<Commodity, String> {
    
    /**
     * 根据卖家ID查找商品
     */
    List<Commodity> findBySellerId(String sellerId);
    
    /**
     * 根据状态查找商品
     */
    List<Commodity> findByCommodityStatus(String commodityStatus);
    
    /**
     * 搜索商品（标题和描述）
     */
    @Query("SELECT c FROM Commodity c WHERE c.title LIKE %?1% OR c.description LIKE %?1%")
    Page<Commodity> searchByKeyword(String keyword, Pageable pageable);
    
    /**
     * 根据价格范围查找商品
     */
    @Query("SELECT c FROM Commodity c WHERE c.price BETWEEN ?1 AND ?2")
    List<Commodity> findByPriceRange(Double minPrice, Double maxPrice);
    
    /**
     * 获取热门商品（按点击量排序）
     */
    @Query("SELECT c FROM Commodity c WHERE c.commodityStatus = 'PUBLISHED' ORDER BY c.clickCount DESC")
    List<Commodity> findHotCommodities(Pageable pageable);
    
    /**
     * 统计已发布商品数量
     */
    @Query("SELECT COUNT(c) FROM Commodity c WHERE c.commodityStatus = 'PUBLISHED'")
    Long countPublishedCommodities();
}
