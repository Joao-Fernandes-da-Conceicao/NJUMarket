package com.njumarket.admin.repository;

import com.njumarket.njumarket.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

/**
 * 用户数据访问层（管理端）
 * 管理端直接访问数据库，不需要通过FeignClient
 */
@Repository
public interface UserRepository extends JpaRepository<User, String>, JpaSpecificationExecutor<User> {
}

