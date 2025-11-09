package com.njumarket.commodity.service;

import com.njumarket.njumarket.dto.Result;
import com.njumarket.commodity.entity.Commodity;
import com.njumarket.commodity.entity.User; // User 实体（Commodity Service专用）

import java.util.List;

/**
 * 商品查询服务接口
 * 专门处理商品查询和可见性逻辑
 * 统一管理所有商品查询相关的功能
 */
public interface CommodityQueryService {
    
    // ========== 公开商品查询 ==========
    
    /**
     * 搜索商品（公开可见）
     * @param keyword 关键词
     * @param page 页码
     * @param size 每页数量
     * @param location 位置
     * @param minPrice 最低价格
     * @param maxPrice 最高价格
     * @param category 分类
     * @param sortBy 排序方式
     * @return 商品分页结果
     */
    Result searchCommodities(String keyword, Integer page, Integer size, String location, Double minPrice, Double maxPrice, String category, String sortBy);
    
    /**
     * 根据分类查询商品（公开可见）
     * @param category 分类
     * @param pageable 分页参数
     * @return 商品分页结果
     */
    Result getCommoditiesByCategory(String category, Integer page, Integer size);
    
    /**
     * 获取商品详情（公开可见）
     * @param commodityId 商品ID
     * @return 商品详情
     */
    Result getCommodityDetail(String commodityId);
    
    /**
     * 获取热门商品（公开可见）
     * @param limit 数量限制
     * @return 商品列表
     */
    Result getHotCommodities(Integer limit);
    
    /**
     * 获取最新商品（公开可见）
     * @param limit 数量限制
     * @return 商品列表
     */
    Result getLatestCommodities(Integer limit);
    
    /**
     * 获取商品分类
     * @return 分类列表
     */
    Result getCategories();
    
    /**
     * 获取推荐商品
     * @param sessionId 会话ID
     * @param limit 数量限制
     * @return 推荐商品列表
     */
    Result getRecommendedCommodities(String sessionId, Integer limit);
    
    /**
     * 记录商品浏览
     * @param commodityId 商品ID
     * @param sessionId 会话ID
     * @return 操作结果
     */
    Result recordView(String commodityId, String sessionId);
    
    /**
     * AI语义搜索
     * @param query 搜索查询
     * @param location 位置偏好
     * @return 搜索结果
     */
    Result aiSearch(String query, String location);
    
    // ========== 用户相关查询 ==========
    
    /**
     * 获取用户的商品（统一的查询接口）
     * @param user 当前用户（可为null，用于权限判断）
     * @param sellerId 卖家ID（如果为null，则查询当前用户的商品）
     * @param status 商品状态（可选，all/DRAFT/PUBLISHED/ON_SHELF/OFF_SHELF）
     *                如果user != null 且 sellerId 等于 user.getUserId()，可以查看草稿
     *                否则只能查看非草稿状态的商品
     * @param page 页码
     * @param size 每页数量
     * @return 商品分页结果
     */
    Result getUserCommodities(User user, String sellerId, String status, Integer page, Integer size);
    
    // ========== 权限检查 ==========
    
    /**
     * 检查商品是否对用户可见
     * @param commodity 商品
     * @param user 用户（可为null）
     * @return 是否可见
     */
    boolean isCommodityVisibleToUser(Commodity commodity, User user);
    
    /**
     * 检查用户是否有权限查看商品详情
     * @param commodity 商品
     * @param user 用户（可为null）
     * @return 是否有权限
     */
    boolean canUserViewCommodity(Commodity commodity, User user);
    
    /**
     * 检查用户是否有权限编辑商品
     * @param commodity 商品
     * @param user 用户
     * @return 是否有权限
     */
    boolean canUserEditCommodity(Commodity commodity, User user);
    
    // ========== 统计信息 ==========
    
    /**
     * 获取商品统计信息
     * @param user 用户
     * @return 统计信息
     */
    Result getCommodityStats(User user);
    
    /**
     * 批量查询商品基本信息（用于聊天界面，轻量级查询）
     * 只返回商品ID、标题、价格、状态等基本信息
     * @param commodityIds 商品ID列表
     * @return 商品基本信息列表
     */
    Result getCommoditiesBatchStatus(List<String> commodityIds);
}

