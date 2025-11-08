package com.njumarket.admin.repository;

import com.njumarket.njumarket.entity.Commodity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

/**
 * 商品数据访问层（管理端）
 * 管理端直接访问数据库，不需要通过FeignClient
 */
@Repository
public interface CommodityRepository extends JpaRepository<Commodity, String>, JpaSpecificationExecutor<Commodity> {
}

