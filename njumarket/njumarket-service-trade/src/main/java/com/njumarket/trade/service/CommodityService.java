package com.njumarket.trade.service;

import com.njumarket.njumarket.dto.Result;
import com.njumarket.njumarket.dto.internal.CommodityInternalDTO;
import com.njumarket.trade.dto.CommodityDTO;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

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
     * 修改商品可见性（PUBLIC = 正常可见，HIDDEN = 管理端软隐藏）
     */
    Result updateCommodityVisibility(String commodityId, String visibility);
    
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

    // ========== 内部接口（供服务间调用） ==========

    /**
     * 完整更新商品字段（管理端内部），含搜索索引同步和缓存失效
     */
    CommodityInternalDTO updateCommodityFullInternal(String commodityId, Map<String, Object> payload);

    /**
     * 物理删除商品（管理端内部），含搜索索引和缓存清理
     */
    void deleteCommodityInternal(String commodityId);

    /**
     * 同步指定商品到搜索索引（管理端内部）
     */
    void syncCommoditySearchInternal(String commodityId);

    // ========== 内部接口（供订单服务等调用，库存已移交订单服务） ==========

    /**
     * 查询商品并加悲观锁（内部），用于创建订单时锁定商品行。
     */
    Result getCommodityForUpdate(String commodityId);

    /**
     * 更新商品库存（内部），委托订单服务调整库存；库存数据已迁移至订单服务。
     */
    Result updateCommodityStock(String commodityId, Integer quantity);
}

