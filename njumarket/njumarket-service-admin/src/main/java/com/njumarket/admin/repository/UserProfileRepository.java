package com.njumarket.admin.repository;

import com.njumarket.admin.entity.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 用户档案数据访问层（管理端）
 * 管理端直接访问数据库，不需要通过FeignClient
 */
@Repository
public interface UserProfileRepository extends JpaRepository<UserProfile, String> {
    
    /**
     * 根据用户ID查找用户档案
     */
    java.util.Optional<UserProfile> findByUserId(String userId);
    
    /**
     * 根据用户ID列表批量查询用户档案
     * 用于优化 N+1 查询问题
     */
    List<UserProfile> findByUserIdIn(List<String> userIds);
}

