package com.njumarket.njumarket.service;

import com.njumarket.njumarket.dto.Result;
import com.njumarket.njumarket.dto.CommodityDTO;
import org.springframework.web.multipart.MultipartFile;

/**
 * 商品服务接口
 */
public interface CommodityService {
    
    // ========== 用户端商品管理 ==========
    /**
     * 发布商品
     */
    Result publishCommodity(CommodityDTO commodityDTO);
    
    /**
     * 获取我发布的商品
     */
    Result getMyCommodities(Integer page, Integer size, String status);
    
    /**
     * 更新商品信息
     */
    Result updateCommodity(String commodityId, CommodityDTO commodityDTO);
    
    /**
     * 下架商品
     */
    Result removeCommodity(String commodityId);
    
    /**
     * 重新上架商品
     */
    Result republishCommodity(String commodityId);
    
    /**
     * 上传商品图片
     */
    Result uploadImage(MultipartFile file);
    
    /**
     * 批量操作商品
     */
    Result batchOperation(String[] commodityIds, String operation);
    
    /**
     * 获取商品销售统计
     */
    Result getSalesStatistics(String period);
    
    /**
     * 复制商品
     */
    Result copyCommodity(String commodityId);
    
    // ========== 公共商品浏览 ==========
    /**
     * 搜索商品
     */
    Result searchCommodities(String keyword, Integer page, Integer size, String location, Double minPrice, Double maxPrice, String category);
    
    /**
     * AI语义搜索
     */
    Result aiSearch(String query, String location);
    
    /**
     * 获取商品详情
     */
    Result getCommodityDetail(String commodityId);
    
    /**
     * 获取热门商品
     */
    Result getHotCommodities(Integer limit);
    
    /**
     * 获取最新商品
     */
    Result getLatestCommodities(Integer limit);
    
    /**
     * 获取商品分类
     */
    Result getCategories();
    
    /**
     * 按分类获取商品
     */
    Result getCommoditiesByCategory(String category, Integer page, Integer size);
    
    /**
     * 获取推荐商品
     */
    Result getRecommendedCommodities(String sessionId, Integer limit);
    
    /**
     * 记录商品浏览
     */
    Result recordView(String commodityId, String sessionId);
    
    // ========== 管理端使用 ==========
    /**
     * 获取商品列表（管理端）
     */
    Result getCommodityList(Integer page, Integer size, String status);
    
    /**
     * 强制下架商品（管理端）
     */
    Result removeCommodity(String commodityId, String reason);
}
