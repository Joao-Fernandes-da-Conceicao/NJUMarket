package com.njumarket.auth.repository;

import com.njumarket.auth.entity.UserAddress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 用户地址Repository
 */
@Repository
public interface UserAddressRepository extends JpaRepository<UserAddress, String> {
    
    /**
     * 根据用户ID查询所有地址
     * @param userId 用户ID
     * @return 地址列表
     */
    List<UserAddress> findByUserId(String userId);
    
    /**
     * 根据用户ID和启用状态查询地址
     * @param userId 用户ID
     * @param isActive 是否启用
     * @return 地址列表
     */
    List<UserAddress> findByUserIdAndIsActive(String userId, Boolean isActive);
    
    /**
     * 查询用户的默认地址
     * @param userId 用户ID
     * @return 默认地址
     */
    Optional<UserAddress> findByUserIdAndIsDefaultTrue(String userId);
    
    /**
     * 查询用户的默认且启用的地址
     * @param userId 用户ID
     * @return 默认地址
     */
    Optional<UserAddress> findByUserIdAndIsDefaultTrueAndIsActiveTrue(String userId);
    
    /**
     * 根据地址ID和用户ID查询地址（用于权限验证）
     * @param addressId 地址ID
     * @param userId 用户ID
     * @return 地址
     */
    Optional<UserAddress> findByAddressIdAndUserId(String addressId, String userId);
    
    /**
     * 统计用户的地址数量
     * @param userId 用户ID
     * @return 地址数量
     */
    long countByUserId(String userId);
    
    /**
     * 取消用户的所有默认地址（设置其他地址为非默认）
     * @param userId 用户ID
     */
    @Modifying
    @Query("UPDATE UserAddress ua SET ua.isDefault = false WHERE ua.userId = :userId")
    void clearDefaultAddress(@Param("userId") String userId);
    
    /**
     * 设置指定地址为默认地址
     * @param addressId 地址ID
     * @param userId 用户ID
     */
    @Modifying
    @Query("UPDATE UserAddress ua SET ua.isDefault = true WHERE ua.addressId = :addressId AND ua.userId = :userId")
    void setDefaultAddress(@Param("addressId") String addressId, @Param("userId") String userId);
}

