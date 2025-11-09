package com.njumarket.commodity.utils;

import com.njumarket.commodity.entity.Commodity;
import com.njumarket.commodity.repository.CommodityRepository;
import com.njumarket.njumarket.exception.BusinessException;
import org.springframework.data.repository.CrudRepository;

/**
 * 商品业务校验工具类
 * 专门用于商品相关的业务校验
 */
public class CommodityValidator {
    
    /**
     * 检查商品是否存在
     * @param commodityId 商品ID
     * @param repository 商品Repository
     * @return 商品对象
     * @throws BusinessException 如果商品不存在
     */
    public static Commodity requireCommodity(String commodityId, CrudRepository<Commodity, String> repository) {
        return repository.findById(commodityId)
            .orElseThrow(() -> new BusinessException("商品不存在"));
    }
    
    /**
     * 检查商品状态
     * @param commodity 商品对象
     * @param expectedStatus 期望的状态
     * @throws BusinessException 如果商品状态不匹配
     */
    public static void requireCommodityStatus(Commodity commodity, String expectedStatus) {
        if (commodity == null) {
            throw new BusinessException("商品不存在");
        }
        if (!expectedStatus.equals(commodity.getCommodityStatus())) {
            throw new BusinessException("商品状态不正确，期望: " + expectedStatus + "，实际: " + commodity.getCommodityStatus());
        }
    }
    
    /**
     * 检查用户是否为商品所有者
     * @param commodity 商品对象
     * @param userId 用户ID
     * @throws BusinessException 如果用户不是商品所有者
     */
    public static void requireCommodityOwner(Commodity commodity, String userId) {
        if (commodity == null) {
            throw new BusinessException("商品不存在");
        }
        if (userId == null || !userId.equals(commodity.getSellerId())) {
            throw new BusinessException("无权限操作此商品");
        }
    }
}

