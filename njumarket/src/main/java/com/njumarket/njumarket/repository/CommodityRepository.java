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
     * 根据卖家ID查找商品（分页）
     */
    Page<Commodity> findBySellerId(String sellerId, Pageable pageable);
    
    /**
     * 根据卖家ID和状态查找商品（分页）
     */
    Page<Commodity> findBySellerIdAndCommodityStatus(String sellerId, String commodityStatus, Pageable pageable);
    
    /**
     * 根据状态查找商品
     */
    List<Commodity> findByCommodityStatus(String commodityStatus);
    
    /**
     * 根据状态查找商品（分页）
     */
    Page<Commodity> findByCommodityStatus(String commodityStatus, Pageable pageable);
    
    /**
     * 根据卖家可见性查找商品
     */
    List<Commodity> findBySellerVisibility(String sellerVisibility);
    
    /**
     * 根据买家可见性查找商品
     */
    List<Commodity> findByBuyerVisibility(String buyerVisibility);
    
    /**
     * 根据卖家可见性和买家可见性查找商品
     */
    List<Commodity> findBySellerVisibilityAndBuyerVisibility(String sellerVisibility, String buyerVisibility);
    
    /**
     * 根据状态和卖家可见性、买家可见性查找商品（分页）
     */
    Page<Commodity> findByCommodityStatusAndSellerVisibilityAndBuyerVisibility(String commodityStatus, String sellerVisibility, String buyerVisibility, Pageable pageable);
    
    /**
     * 搜索商品（标题和描述）- 只搜索可见的商品
     */
    @Query("SELECT c FROM Commodity c WHERE (c.title LIKE %?1% OR c.description LIKE %?1%) AND c.sellerVisibility = 'PUBLIC' AND c.buyerVisibility = 'PUBLIC' AND c.commodityStatus = 'ON_SHELF'")
    Page<Commodity> searchByKeyword(String keyword, Pageable pageable);
    
    /**
     * 根据分类查找商品 - 只查找可见的商品
     */
    @Query("SELECT c FROM Commodity c WHERE c.category = ?1 AND c.sellerVisibility = 'PUBLIC' AND c.buyerVisibility = 'PUBLIC' AND c.commodityStatus = 'ON_SHELF'")
    Page<Commodity> findByCategoryAndVisible(String category, Pageable pageable);
    
    /**
     * 根据价格范围查找商品 - 只查找可见的商品
     */
    @Query("SELECT c FROM Commodity c WHERE c.price BETWEEN ?1 AND ?2 AND c.sellerVisibility = 'PUBLIC' AND c.buyerVisibility = 'PUBLIC' AND c.commodityStatus = 'ON_SHELF'")
    List<Commodity> findByPriceRange(Double minPrice, Double maxPrice);
    
    /**
     * 获取热门商品（按点击量排序）- 只获取可见的商品
     */
    @Query("SELECT c FROM Commodity c WHERE c.commodityStatus = 'ON_SHELF' AND c.sellerVisibility = 'PUBLIC' AND c.buyerVisibility = 'PUBLIC' ORDER BY c.clickCount DESC")
    List<Commodity> findHotCommodities(Pageable pageable);
    
    /**
     * 获取最新商品 - 只获取可见的商品
     */
    @Query("SELECT c FROM Commodity c WHERE c.commodityStatus = 'ON_SHELF' AND c.sellerVisibility = 'PUBLIC' AND c.buyerVisibility = 'PUBLIC' ORDER BY c.publishTime DESC")
    List<Commodity> findLatestCommodities(Pageable pageable);
    
    /**
     * 统计已上架商品数量
     */
    @Query("SELECT COUNT(c) FROM Commodity c WHERE c.commodityStatus = 'ON_SHELF'")
    Long countPublishedCommodities();
    
    /**
     * 根据卖家ID统计商品数量
     */
    Long countBySellerId(String sellerId);
    
    /**
     * 根据卖家ID和状态统计商品数量
     */
    Long countBySellerIdAndCommodityStatus(String sellerId, String commodityStatus);
    
    /**
     * 根据卖家ID统计总浏览量
     */
    @Query("SELECT SUM(c.clickCount) FROM Commodity c WHERE c.sellerId = ?1")
    Long sumClickCountBySellerId(String sellerId);
}
