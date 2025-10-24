package com.njumarket.njumarket.service;

import com.njumarket.njumarket.dto.Result;
import com.njumarket.njumarket.dto.CommodityDTO;
import org.springframework.web.multipart.MultipartFile;

/**
 * 商品服务接口
 * 重构后的商品服务，专注于商品管理功能
 * 查询功能已迁移到CommodityQueryService
 */
public interface CommodityService {
    
    // ========== 商品管理核心功能 ==========
    
    /**
     * 发布商品
     */
    Result publishCommodity(CommodityDTO commodityDTO);
    
    /**
     * 创建草稿商品
     */
    Result createDraftCommodity(CommodityDTO commodityDTO);
    
    /**
     * 发布草稿商品
     */
    Result publishDraftCommodity(String commodityId);
    
    /**
     * 更新商品信息
     */
    Result updateCommodity(String commodityId, CommodityDTO commodityDTO);
    
    /**
     * 删除商品（只有没有下单的商品可以删除）
     */
    Result deleteCommodity(String commodityId);
    
    // ========== 商品状态管理 ==========
    
    /**
     * 上架商品（发布+上架）
     */
    Result shelfCommodity(String commodityId);
    
    /**
     * 下架商品
     */
    Result unshelfCommodity(String commodityId);
    
    /**
     * 设为草稿
     */
    Result draftCommodity(String commodityId);
    
    /**
     * 重新上架商品
     */
    Result republishCommodity(String commodityId);
    
    // ========== 商品可见性管理 ==========
    
    /**
     * 修改商品可见性（同时设置卖家和买家可见性）
     */
    Result updateCommodityVisibility(String commodityId, String visibility);
    
    /**
     * 修改商品卖家可见性
     */
    Result updateCommoditySellerVisibility(String commodityId, String sellerVisibility);
    
    /**
     * 修改商品买家可见性
     */
    Result updateCommodityBuyerVisibility(String commodityId, String buyerVisibility);
    
    // ========== 图片管理 ==========
    
    /**
     * 上传商品图片
     */
    Result uploadImage(MultipartFile file);
    
    /**
     * 为指定商品上传图片
     */
    Result uploadCommodityImage(String commodityId, MultipartFile file);
    
    // ========== 批量操作 ==========
    
    /**
     * 批量操作商品
     */
    Result batchOperation(String[] commodityIds, String operation);
    
    /**
     * 复制商品
     */
    Result copyCommodity(String commodityId);
    
    // ========== 统计和分析 ==========
    
    /**
     * 获取商品销售统计
     */
    Result getSalesStatistics(String period);
    
    // ========== 用户商品管理 ==========
    
    /**
     * 获取我发布的商品
     */
    Result getMyCommodities(Integer page, Integer size, String status);
    
    /**
     * 获取我发布的单个商品详情
     */
    Result getMyCommodityDetail(String commodityId);
    
    // ========== 管理端功能 ==========
    
    /**
     * 获取商品列表（管理端）
     */
    Result getCommodityList(Integer page, Integer size, String status);
    
    /**
     * 强制下架商品（管理端）
     */
    Result removeCommodity(String commodityId, String reason);
}