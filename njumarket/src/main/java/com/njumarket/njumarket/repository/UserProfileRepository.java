package com.njumarket.njumarket.repository;

import com.njumarket.njumarket.entity.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 用户档案数据访问层
 */
@Repository
public interface UserProfileRepository extends JpaRepository<UserProfile, String> {
    
    /**
     * 根据用户ID查找用户档案
     */
    Optional<UserProfile> findByUserId(String userId);
    
    /**
     * 检查用户是否已有档案
     */
    boolean existsByUserId(String userId);
    
    /**
     * 根据昵称模糊查询用户档案
     */
    List<UserProfile> findByNicknameContaining(String nickname);
    
    /**
     * 根据VIP等级查询用户档案
     */
    List<UserProfile> findByVipLevel(String vipLevel);
    
    /**
     * 查询信用分大于指定值的用户
     */
    @Query("SELECT up FROM UserProfile up WHERE up.creditScore >= ?1")
    List<UserProfile> findByCreditScoreGreaterThanEqual(Integer creditScore);
    
    /**
     * 查询卖家评分排行榜
     */
    @Query("SELECT up FROM UserProfile up ORDER BY up.sellerRating DESC, up.totalSales DESC")
    List<UserProfile> findTopSellersByRating();
    
    /**
     * 查询买家评分排行榜
     */
    @Query("SELECT up FROM UserProfile up ORDER BY up.buyerRating DESC, up.totalPurchases DESC")
    List<UserProfile> findTopBuyersByRating();
    
    /**
     * 统计各VIP等级用户数量
     */
    @Query("SELECT up.vipLevel, COUNT(up) FROM UserProfile up GROUP BY up.vipLevel")
    List<Object[]> countByVipLevel();
}
