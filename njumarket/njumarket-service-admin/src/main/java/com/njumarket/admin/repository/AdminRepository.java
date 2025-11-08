package com.njumarket.admin.repository;

import com.njumarket.njumarket.entity.Admin;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 管理员数据访问层
 */
@Repository
public interface AdminRepository extends JpaRepository<Admin, String>, 
    org.springframework.data.jpa.repository.JpaSpecificationExecutor<Admin> {
    
    /**
     * 根据用户名查找管理员
     */
    Optional<Admin> findByUsername(String username);
    
    /**
     * 检查用户名是否已存在
     */
    boolean existsByUsername(String username);
    
    /**
     * 根据账户状态查找管理员
     */
    List<Admin> findByAccountStatus(String accountStatus);
    
    /**
     * 根据管理员级别查找
     */
    List<Admin> findByAdminLevel(String adminLevel);
    
    /**
     * 根据部门查找管理员
     */
    List<Admin> findByDepartment(String department);
    
    /**
     * 统计活跃管理员数量
     */
    @Query("SELECT COUNT(a) FROM Admin a WHERE a.accountStatus = 'ACTIVE'")
    Long countActiveAdmins();
    
    /**
     * 统计各管理员级别的数量
     */
    @Query("SELECT a.adminLevel, COUNT(a) FROM Admin a WHERE a.accountStatus = 'ACTIVE' GROUP BY a.adminLevel")
    List<Object[]> countAdminsByLevel();
    
    /**
     * 查找最近登录的管理员
     */
    @Query("SELECT a FROM Admin a WHERE a.lastLoginTime >= :startTime ORDER BY a.lastLoginTime DESC")
    List<Admin> findRecentLoginAdmins(@Param("startTime") LocalDateTime startTime);
    
    /**
     * 根据权限查找管理员
     */
    @Query("SELECT a FROM Admin a WHERE a.permissions LIKE %:permission%")
    List<Admin> findByPermission(@Param("permission") String permission);
    
    /**
     * 查找系统管理员
     */
    @Query("SELECT a FROM Admin a WHERE a.adminLevel = 'SUPER' AND a.accountStatus = 'ACTIVE'")
    List<Admin> findSuperAdmins();
}

