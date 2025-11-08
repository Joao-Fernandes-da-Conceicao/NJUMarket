package com.njumarket.auth.repository;

import com.njumarket.njumarket.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 用户数据访问层
 */
@Repository
public interface UserRepository extends JpaRepository<User, String>, JpaSpecificationExecutor<User> {
    
    /**
     * 根据手机号查找用户（包含UserProfile）
     */
    @Query("SELECT u FROM User u LEFT JOIN FETCH u.userProfile WHERE u.primaryPhone = ?1")
    Optional<User> findByPrimaryPhone(String primaryPhone);
    
    /**
     * 根据用户名查找用户（包含UserProfile）
     */
    @Query("SELECT u FROM User u LEFT JOIN FETCH u.userProfile WHERE u.username = ?1")
    Optional<User> findByUsername(String username);
    
    /**
     * 根据用户名或手机号查找用户（用于登录，包含UserProfile）
     */
    @Query("SELECT u FROM User u LEFT JOIN FETCH u.userProfile WHERE u.primaryPhone = ?1 OR u.username = ?1")
    Optional<User> findByUsernameOrPhone(String identifier);
    
    /**
     * 检查手机号是否已存在
     */
    boolean existsByPrimaryPhone(String primaryPhone);
    
    /**
     * 检查用户名是否已存在
     */
    boolean existsByUsername(String username);
    
    /**
     * 根据账户状态查找用户
     */
    @Query("SELECT u FROM User u WHERE u.accountStatus = ?1")
    java.util.List<User> findByAccountStatus(String accountStatus);
    
    /**
     * 统计活跃用户数
     */
    @Query("SELECT COUNT(u) FROM User u WHERE u.accountStatus = 'ACTIVE'")
    Long countActiveUsers();
}

