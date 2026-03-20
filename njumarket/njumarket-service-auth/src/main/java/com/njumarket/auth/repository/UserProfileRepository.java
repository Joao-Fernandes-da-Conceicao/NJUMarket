package com.njumarket.auth.repository;

import com.njumarket.auth.entity.UserProfile;
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
     * 根据用户ID列表批量查询用户档案
     * 用于优化 N+1 查询问题
     */
    List<UserProfile> findByUserIdIn(List<String> userIds);
}

