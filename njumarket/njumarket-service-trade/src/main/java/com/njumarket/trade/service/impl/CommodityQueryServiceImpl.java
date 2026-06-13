package com.njumarket.trade.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.njumarket.njumarket.dto.Result;
import com.njumarket.njumarket.dto.internal.CommodityInternalDTO;
import com.njumarket.trade.dto.CommodityDTO;
import com.njumarket.trade.dto.internal.CommodityInternalDTOConverter;
import com.njumarket.trade.vo.CommodityPageResultVO;
import com.njumarket.trade.vo.CommodityDetailVO;
import com.njumarket.njumarket.dto.internal.UserProfileInternalDTO;
import com.njumarket.trade.entity.Commodity;
import com.njumarket.trade.entity.User; // User 实体（Commodity Service专用）
import com.njumarket.njumarket.exception.BusinessException;
import com.njumarket.trade.repository.CommodityRepository;
import com.njumarket.trade.service.CommodityQueryService;
import com.njumarket.trade.service.UserCacheService;
import com.njumarket.njumarket.utils.SecurityUtils;
import com.njumarket.njumarket.utils.CacheUtil;
import com.njumarket.njumarket.utils.RedisConstants;
import com.njumarket.trade.search.CommoditySearchResult;
import com.njumarket.trade.search.CommoditySearchService;
import com.njumarket.trade.search.PublicCommodityBrowseLimits;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import jakarta.persistence.criteria.Predicate;
import java.util.*;
import java.util.stream.Collectors;
import java.util.HashMap;
import java.util.Map;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Arrays;

/**
 * 商品查询服务实现类
 * 专门处理商品查询和可见性逻辑
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CommodityQueryServiceImpl implements CommodityQueryService {
    
    private final CommodityRepository commodityRepository;
    private final CommodityInternalDTOConverter commodityInternalDTOConverter;
    private final UserCacheService userCacheService;
    private final CacheUtil cacheUtil;
    private final CommoditySearchService commoditySearchService;
    private final RedisTemplate<String, Object> redisTemplate;
    
    // ========== 公开商品查询实现 ==========
    
    @Override
    public Result searchCommodities(String keyword, Integer page, Integer size, String location, Double minPrice, Double maxPrice, String category, String sortBy) {
        try {
            int p = PublicCommodityBrowseLimits.clampPage(page);
            int s = PublicCommodityBrowseLimits.clampSize(size);
            log.info("搜索商品 - keyword: {}, page: {} (req {}), size: {} (req {}), location: {}, minPrice: {}, maxPrice: {}, category: {}, sortBy: {}",
                    keyword, p, page, s, size, location, minPrice, maxPrice, category, sortBy);

            CommodityPageResultVO elasticResult = trySearchCommoditiesWithElastic(keyword, p, s, location, minPrice, maxPrice, category, sortBy);
            if (elasticResult != null) {
                log.info("ElasticSearch 搜索命中 {} 条数据", elasticResult.getTotal());
                return Result.ok("搜索商品成功", elasticResult);
            }
                
            // 根据排序参数创建Pageable
            Pageable pageable = createPageable(p, s, sortBy);
            
            Page<Commodity> commodityPage;
            
            if (StringUtils.hasText(keyword)) {
                commodityPage = commodityRepository.searchByKeyword(keyword.trim(), pageable);
            } else if (StringUtils.hasText(category)) {
                commodityPage = commodityRepository.findByCategoryAndVisible(category.trim(), pageable);
            } else {
                // 如果没有关键词和分类，返回所有公开商品
                commodityPage = commodityRepository.findByCommodityStatusAndBuyerVisibility(
                    "ON_SHELF", "PUBLIC", pageable
                );
            }
            
            // 过滤价格范围和位置
            List<Commodity> filteredCommodities = commodityPage.getContent().stream()
                    .filter(commodity -> {
                        if (minPrice != null && commodity.getPrice() < minPrice) return false;
                        if (maxPrice != null && commodity.getPrice() > maxPrice) return false;
                        if (StringUtils.hasText(location) && !location.equals(commodity.getLocation())) return false;
                        return true;
                    })
                    .collect(Collectors.toList());
            
            // ✅ 优化：批量查询所有商品的卖家 Profile（避免 N+1 查询）
            List<CommodityDTO> commodityDTOs = convertCommoditiesToDTOWithBatchProfile(filteredCommodities);
            
            CommodityPageResultVO result = new CommodityPageResultVO();
            result.setCommodities(commodityDTOs);
            long cappedTotal = PublicCommodityBrowseLimits.capReportedTotal(commodityPage.getTotalElements());
            result.setTotal(cappedTotal);
            result.setPages(PublicCommodityBrowseLimits.calculateReportedPages(cappedTotal, s));
            result.setCurrent(p);
            result.setSize(s);
            
            log.info("搜索商品成功 - 找到 {} 个商品", commodityDTOs.size());
            return Result.ok("搜索商品成功", result);
            
        } catch (Exception e) {
            log.error("搜索商品失败: {}", e.getMessage(), e);
            throw new BusinessException("搜索失败，请稍后重试");
        }
    }
    
    @Override
    public Result getCommoditiesByCategory(String category, Integer page, Integer size) {
        try {
            int p = PublicCommodityBrowseLimits.clampPage(page);
            int sz = PublicCommodityBrowseLimits.clampSize(size);
            log.info("根据分类查询商品 - category: {}, page: {}, size: {}", category, p, sz);
            
            Pageable pageable = PageRequest.of(p - 1, sz, Sort.by(Sort.Direction.DESC, "publishTime"));
            Page<Commodity> commodityPage = commodityRepository.findByCategoryAndVisible(category, pageable);
            
            // ✅ 优化：批量查询所有商品的卖家 Profile（避免 N+1 查询）
            List<CommodityDTO> commodityDTOs = convertCommoditiesToDTOWithBatchProfile(commodityPage.getContent());
            
            CommodityPageResultVO result = new CommodityPageResultVO();
            result.setCommodities(commodityDTOs);
            long cappedTotal = PublicCommodityBrowseLimits.capReportedTotal(commodityPage.getTotalElements());
            result.setTotal(cappedTotal);
            result.setPages(PublicCommodityBrowseLimits.calculateReportedPages(cappedTotal, sz));
            result.setCurrent(p);
            result.setSize(sz);
            
            return Result.ok("获取分类商品成功", result);
            
        } catch (Exception e) {
            log.error("根据分类查询商品失败: {}", e.getMessage(), e);
            throw new BusinessException("查询失败，请稍后重试");
        }
    }
    
    @Override
    public Result getCommodityDetail(String commodityId) {
        try {
            log.info("获取商品详情 - commodityId: {}", commodityId);
            
            // ✅ 使用缓存（最终一致性）
            String cacheKey = RedisConstants.CACHE_COMMODITY_DETAIL_KEY + commodityId;
            CommodityDTO commodityDTO = cacheUtil.getWithFallback(
                cacheKey,
                RedisConstants.CACHE_COMMODITY_DETAIL_TTL * 60, // 转换为秒
                CommodityDTO.class,
                () -> {
                    // 缓存未命中，从数据库加载
                    Commodity commodity = commodityRepository.findById(commodityId).orElse(null);
                    if (commodity == null) {
                        throw new BusinessException("商品不存在");
                    }
                    
                    // 检查可见性权限（公开接口，允许未登录用户访问）
                    Object userObj = SecurityUtils.getCurrentUser();
                    User currentUser = userObj instanceof User ? (User) userObj : null;
                    if (!canUserViewCommodity(commodity, currentUser)) {
                        throw new BusinessException("无权限查看此商品");
                    }
                    
                    // ✅ 优化：批量查询卖家 Profile（虽然只有一条，但保持一致性）
                    List<Commodity> singleCommodityList = Collections.singletonList(commodity);
                    List<CommodityDTO> dtos = convertCommoditiesToDTOWithBatchProfile(singleCommodityList);
                    return dtos.isEmpty() ? convertToDTO(commodity) : dtos.get(0);
                }
            );
            
            // 点击量 Write-Behind：INCR 写入 Redis，由 ClickCountFlushJob 定时批量刷回 DB
            redisTemplate.opsForValue().increment(RedisConstants.CLICK_COUNT_DELTA_KEY + commodityId);
            
            return Result.ok("获取商品详情成功", commodityDTO);
            
        } catch (Exception e) {
            log.error("获取商品详情失败: {}", e.getMessage(), e);
            throw new BusinessException("获取商品详情失败");
        }
    }
    
    
    @Override
    public Result getHotCommodities(Integer limit) {
        try {
            log.info("获取热门商品 - limit: {}", limit);
            
            // ✅ 使用缓存（最终一致性）
            String cacheKey = RedisConstants.CACHE_COMMODITY_HOT_KEY + ":" + limit;
            List<CommodityDTO> commodityDTOs = cacheUtil.getWithFallback(
                cacheKey,
                RedisConstants.CACHE_COMMODITY_HOT_TTL * 60, // 转换为秒
                new TypeReference<List<CommodityDTO>>() {},
                () -> {
                    // 缓存未命中，从数据库加载
                    Pageable pageable = PageRequest.of(0, limit);
                    List<Commodity> commodities = commodityRepository.findHotCommodities(pageable);
                    // ✅ 优化：批量查询所有商品的卖家 Profile（避免 N+1 查询）
                    return convertCommoditiesToDTOWithBatchProfile(commodities);
                }
            );
            cacheUtil.addCacheKeyToSet(RedisConstants.CACHE_COMMODITY_HOT_KEY_REGISTRY, cacheKey);
            
            return Result.ok("获取热门商品成功", commodityDTOs);
            
        } catch (Exception e) {
            log.error("获取热门商品失败: {}", e.getMessage(), e);
            throw new BusinessException("获取热门商品失败");
        }
    }
    
    @Override
    public Result getLatestCommodities(Integer limit) {
        try {
            log.info("获取最新商品 - limit: {}", limit);
            
            // ✅ 使用缓存（最终一致性）
            String cacheKey = RedisConstants.CACHE_COMMODITY_LATEST_KEY + ":" + limit;
            List<CommodityDTO> commodityDTOs = cacheUtil.getWithFallback(
                cacheKey,
                RedisConstants.CACHE_COMMODITY_LATEST_TTL * 60, // 转换为秒
                new TypeReference<List<CommodityDTO>>() {},
                () -> {
                    // 缓存未命中，从数据库加载
                    Pageable pageable = PageRequest.of(0, limit);
                    List<Commodity> commodities = commodityRepository.findLatestCommodities(pageable);
                    // ✅ 优化：批量查询所有商品的卖家 Profile（避免 N+1 查询）
                    return convertCommoditiesToDTOWithBatchProfile(commodities);
                }
            );
            cacheUtil.addCacheKeyToSet(RedisConstants.CACHE_COMMODITY_LATEST_KEY_REGISTRY, cacheKey);
            
            return Result.ok("获取最新商品成功", commodityDTOs);
            
        } catch (Exception e) {
            log.error("获取最新商品失败: {}", e.getMessage(), e);
            throw new BusinessException("获取最新商品失败");
        }
    }
    
    @Override
    public Result getCategories() {
        try {
            log.info("获取商品分类");
            
            // ✅ 使用缓存（最终一致性，分类变化较少，TTL较长）
            String cacheKey = RedisConstants.CACHE_COMMODITY_CATEGORIES_KEY;
            List<String> categories = cacheUtil.getWithFallback(
                cacheKey,
                RedisConstants.CACHE_COMMODITY_CATEGORIES_TTL * 60, // 转换为秒
                new TypeReference<List<String>>() {},
                () -> {
                    // 这里可以从数据库获取分类，或者返回预定义的分类
                    return List.of(
                        "电子产品", "服装配饰", "图书文具", "生活用品", 
                        "运动户外", "美妆护肤", "食品饮料", "其他"
                    );
                }
            );
            
            return Result.ok("获取分类成功", categories);
            
        } catch (Exception e) {
            log.error("获取商品分类失败: {}", e.getMessage(), e);
            throw new BusinessException("获取分类失败");
        }
    }
    
    @Override
    public Result getRecommendedCommodities(String sessionId, Integer limit) {
        try {
            log.info("获取推荐商品 - sessionId: {}, limit: {}", sessionId, limit);
            
            // 简单的推荐逻辑：返回最新商品
            return getLatestCommodities(limit);
            
        } catch (Exception e) {
            log.error("获取推荐商品失败: {}", e.getMessage(), e);
            throw new BusinessException("获取推荐商品失败");
        }
    }
    
    @Override
    public Result recordView(String commodityId, String sessionId) {
        try {
            log.debug("记录商品浏览 - commodityId: {}, sessionId: {}", commodityId, sessionId);

            // Write-Behind：将点击增量写入 Redis，由 ClickCountFlushJob 定时批量刷回 DB
            // 不再直接 SELECT + UPDATE，彻底避免高并发行锁争用
            String deltaKey = RedisConstants.CLICK_COUNT_DELTA_KEY + commodityId;
            redisTemplate.opsForValue().increment(deltaKey);

            return Result.ok("记录浏览成功");

        } catch (Exception e) {
            log.error("记录商品浏览失败: {}", e.getMessage(), e);
            throw new BusinessException("记录浏览失败");
        }
    }
    
    // ========== 用户相关查询实现 ==========
    
    @Override
    public Result getUserCommodities(User user, String sellerId, String status, Integer page, Integer size) {
        try {
            // 如果 sellerId 为 null，则使用当前用户的 ID
            String targetSellerId = sellerId;
            if (targetSellerId == null && user != null) {
                targetSellerId = user.getUserId();
            }
            
            if (targetSellerId == null) {
                throw new BusinessException("卖家ID不能为空");
            }
            
            boolean isOwnCommodities = user != null && user.getUserId().equals(targetSellerId);

            int reqPage = (page == null || page < 1) ? 1 : page;
            int reqSize = (size == null || size < 1) ? 10 : size;
            if (!isOwnCommodities) {
                reqPage = PublicCommodityBrowseLimits.clampPage(reqPage);
                reqSize = PublicCommodityBrowseLimits.clampSize(reqSize);
            }
            
            log.info("获取用户商品 - user: {}, sellerId: {}, isOwn: {}, status: {}, page: {}, size: {}", 
                    user != null ? user.getUserId() : "anonymous", targetSellerId, isOwnCommodities, status, reqPage, reqSize);
            
            Pageable pageable = PageRequest.of(reqPage - 1, reqSize, Sort.by(Sort.Direction.DESC, "publishTime"));
            Page<Commodity> commodityPage;
            
            // 标准化状态参数
            String normalizedStatus = (status == null || status.trim().isEmpty()) ? "all" : status;
            
            // 如果是查看其他用户的商品，不能查看草稿状态
            if (!isOwnCommodities && "DRAFT".equals(normalizedStatus)) {
                throw new BusinessException("无法查看其他用户的草稿商品");
            }
            
            // 如果是自己的商品，直接查询并返回
            if (isOwnCommodities) {
                // 查询商品
                if ("all".equals(normalizedStatus)) {
                    commodityPage = commodityRepository.findBySellerId(targetSellerId, pageable);
                } else {
                    commodityPage = commodityRepository.findBySellerIdAndCommodityStatus(targetSellerId, normalizedStatus, pageable);
                }
                
                // ✅ 优化：批量查询所有商品的卖家 Profile（避免 N+1 查询）
                List<CommodityDTO> commodityDTOs = convertCommoditiesToDTOWithBatchProfile(commodityPage.getContent());
                
                Map<String, Object> result = new HashMap<>();
                result.put("commodities", commodityDTOs);
                result.put("total", commodityPage.getTotalElements());
                result.put("pages", commodityPage.getTotalPages());
                result.put("current", reqPage);
                result.put("size", reqSize);
                
                return Result.ok("获取用户商品成功", result);
            }
            
            // 查看其他用户的商品，需要过滤草稿和不可见商品
            if ("all".equals(normalizedStatus)) {
                // 查询所有非草稿状态的商品
                Pageable allPageable = PageRequest.of(0, Integer.MAX_VALUE, Sort.by(Sort.Direction.DESC, "publishTime"));
                Page<Commodity> allCommoditiesPage = commodityRepository.findBySellerId(targetSellerId, allPageable);
                
                // 过滤掉草稿和不可见的商品
                List<Commodity> filteredCommodities = new ArrayList<>(allCommoditiesPage.getContent().stream()
                        .filter(c -> !"DRAFT".equals(c.getCommodityStatus()))
                        .filter(c -> isCommodityVisibleToUser(c, user))
                        .collect(Collectors.toList()));
                if (filteredCommodities.size() > PublicCommodityBrowseLimits.MAX_VISIBLE_TOTAL) {
                    filteredCommodities = new ArrayList<>(filteredCommodities.subList(0, PublicCommodityBrowseLimits.MAX_VISIBLE_TOTAL));
                }
                
                // 手动分页
                int start = (reqPage - 1) * reqSize;
                int end = Math.min(start + reqSize, filteredCommodities.size());
                List<Commodity> pagedList = start < filteredCommodities.size() 
                        ? filteredCommodities.subList(start, end) 
                        : Collections.emptyList();
                
                // ✅ 优化：批量查询所有商品的卖家 Profile（避免 N+1 查询）
                List<CommodityDTO> commodityDTOs = convertCommoditiesToDTOWithBatchProfile(pagedList);
                
                Map<String, Object> result = new HashMap<>();
                long visTotal = filteredCommodities.size();
                result.put("commodities", commodityDTOs);
                result.put("total", visTotal);
                result.put("pages", PublicCommodityBrowseLimits.calculateReportedPages(visTotal, reqSize));
                result.put("current", reqPage);
                result.put("size", reqSize);
                
                return Result.ok("获取用户商品成功", result);
            } else {
                // 按状态查询（只支持 ON_SHELF 和 OFF_SHELF）
                if (!"ON_SHELF".equals(normalizedStatus) && !"OFF_SHELF".equals(normalizedStatus) && !"PUBLISHED".equals(normalizedStatus)) {
                    throw new BusinessException("不支持的状态筛选，只支持 ON_SHELF、OFF_SHELF 和 PUBLISHED");
                }
                
                commodityPage = commodityRepository.findBySellerIdAndCommodityStatus(targetSellerId, normalizedStatus, pageable);
                
                // 过滤可见性
                List<Commodity> visibleCommodities = commodityPage.getContent().stream()
                        .filter(commodity -> isCommodityVisibleToUser(commodity, user))
                        .collect(Collectors.toList());
                
                // ✅ 优化：批量查询所有商品的卖家 Profile（避免 N+1 查询）
                List<CommodityDTO> commodityDTOs = convertCommoditiesToDTOWithBatchProfile(visibleCommodities);
                
                Map<String, Object> result = new HashMap<>();
                long capped = PublicCommodityBrowseLimits.capReportedTotal(commodityPage.getTotalElements());
                result.put("commodities", commodityDTOs);
                result.put("total", capped);
                result.put("pages", PublicCommodityBrowseLimits.calculateReportedPages(capped, reqSize));
                result.put("current", reqPage);
                result.put("size", reqSize);
                
                return Result.ok("获取用户商品成功", result);
            }
            
        } catch (Exception e) {
            log.error("获取用户商品失败: {}", e.getMessage(), e);
            throw new BusinessException("获取用户商品失败");
        }
    }
    
    
    // ========== 权限检查实现 ==========
    
    @Override
    public boolean isCommodityVisibleToUser(Commodity commodity, User user) {
        if (commodity == null) return false;
        
        // 商品必须是上架状态
        if (!"ON_SHELF".equals(commodity.getCommodityStatus())) {
            return false;
        }
        
        // 商品必须未被管理端隐藏
        if (!"PUBLIC".equals(commodity.getBuyerVisibility())) {
            return false;
        }
        
        return true;
    }
    
    /**
     * 检查商品是否可以被查询（包括下架商品）
     * 用于"再来一单"等场景，允许查询下架商品但会提示状态
     * @param commodity 商品实体
     * @param user 用户实体
     * @return 是否可以被查询
     */
    public boolean canCommodityBeQueried(Commodity commodity, User user) {
        if (commodity == null) return false;
        
        // 如果是商品所有者，可以查询所有状态的商品
        if (user != null && user.getUserId().equals(commodity.getSellerId())) {
            return true;
        }
        
        // 其他用户只能查询未被管理端隐藏的商品
        return "PUBLIC".equals(commodity.getBuyerVisibility());
    }
    
    /**
     * 检查商品是否可以下单
     * @param commodity 商品实体
     * @return 是否可以下单
     */
    public boolean canCommodityBeOrdered(Commodity commodity) {
        if (commodity == null) return false;
        
        // 只有上架状态的商品可以下单
        return "ON_SHELF".equals(commodity.getCommodityStatus());
    }
    
    @Override
    public boolean canUserViewCommodity(Commodity commodity, User user) {
        if (commodity == null) return false;
        
        // 如果是商品所有者，可以查看所有状态的商品
        if (user != null && user.getUserId().equals(commodity.getSellerId())) {
            return true;
        }
        
        // 其他用户只能查看公开且上架的商品
        return isCommodityVisibleToUser(commodity, user);
    }
    
    @Override
    public boolean canUserEditCommodity(Commodity commodity, User user) {
        if (commodity == null || user == null) return false;
        
        // 只有商品所有者可以编辑
        return user.getUserId().equals(commodity.getSellerId());
    }
    
    /**
     * 根据商品ID查询商品（包括下架商品）
     * 用于"再来一单"等场景
     * @param commodityId 商品ID
     * @return 查询结果
     */
    public Result getCommodityByIdForReorder(String commodityId) {
        try {
            log.info("根据ID获取商品详情（支持下架商品） - commodityId: {}", commodityId);
            
            // 获取当前用户
            Object userObj = SecurityUtils.requireCurrentUser();
        User currentUser = (User) userObj;
            
            Commodity commodity = commodityRepository.findById(commodityId).orElse(null);
            if (commodity == null) {
                throw new BusinessException("商品不存在");
            }
            
            // 检查权限：使用新的查询权限检查
            if (!canCommodityBeQueried(commodity, currentUser)) {
                throw new BusinessException("无权限查看此商品");
            }
            
            // ✅ 优化：批量查询卖家 Profile（虽然只有一条，但保持一致性）
            List<Commodity> singleCommodityList = Collections.singletonList(commodity);
            List<CommodityDTO> dtos = convertCommoditiesToDTOWithBatchProfile(singleCommodityList);
            CommodityDTO commodityDTO = dtos.isEmpty() ? convertToDTO(commodity) : dtos.get(0);
            
            // 添加商品状态信息
            CommodityDetailVO result = new CommodityDetailVO();
            result.setCommodity(commodityDTO);
            result.setCanOrder(canCommodityBeOrdered(commodity));
            result.setIsOffShelf(!canCommodityBeOrdered(commodity));
            result.setStatusMessage(getCommodityStatusMessage(commodity));
            
            return Result.ok("获取商品详情成功", result);
            
        } catch (Exception e) {
            log.error("根据ID获取商品详情失败: {}", e.getMessage(), e);
            throw new BusinessException("获取商品详情失败");
        }
    }
    
    /**
     * 获取商品状态提示信息
     * @param commodity 商品实体
     * @return 状态提示信息
     */
    private String getCommodityStatusMessage(Commodity commodity) {
        if (commodity == null) return "商品不存在";
        
        switch (commodity.getCommodityStatus()) {
            case "ON_SHELF":
                return "商品正常销售中";
            case "OFF_SHELF":
                return "商品已下架";
            case "DRAFT":
                return "商品为草稿状态";
            case "PUBLISHED":
                return "商品已发布但未上架";
            default:
                return "商品状态未知";
        }
    }
    
    // ========== 统计信息实现 ==========
    
    @Override
    public Result getCommodityStats(User user) {
        try {
            log.info("获取商品统计信息 - userId: {}", user.getUserId());
            
            Map<String, Object> stats = new HashMap<>();
            
            // 总商品数
            long totalCommodities = commodityRepository.countBySellerId(user.getUserId());
            stats.put("totalCommodities", totalCommodities);
            
            // 已上架商品数
            long publishedCommodities = commodityRepository.countBySellerIdAndCommodityStatus(user.getUserId(), "ON_SHELF");
            stats.put("publishedCommodities", publishedCommodities);
            
            // 草稿商品数
            long draftCommodities = commodityRepository.countBySellerIdAndCommodityStatus(user.getUserId(), "DRAFT");
            stats.put("draftCommodities", draftCommodities);
            
            // 已售出商品数
            long soldCommodities = commodityRepository.countBySellerIdAndCommodityStatus(user.getUserId(), "SOLD_OUT");
            stats.put("soldCommodities", soldCommodities);
            
            // 总浏览量
            Long totalViews = commodityRepository.sumClickCountBySellerId(user.getUserId());
            stats.put("totalViews", totalViews != null ? totalViews : 0);
            
            return Result.ok("获取统计信息成功", stats);
            
        } catch (Exception e) {
            log.error("获取商品统计信息失败: {}", e.getMessage(), e);
            throw new BusinessException("获取统计信息失败");
        }
    }
    
    // ========== 私有辅助方法 ==========
    
    private CommodityPageResultVO trySearchCommoditiesWithElastic(String keyword,
                                                                  Integer page,
                                                                  Integer size,
                                                                  String location,
                                                                  Double minPrice,
                                                                  Double maxPrice,
                                                                  String category,
                                                                  String sortBy) {
        if (!commoditySearchService.shouldUseElasticSearch(keyword, location, minPrice, maxPrice, category)) {
            return null;
        }
        int safePage = PublicCommodityBrowseLimits.clampPage(page);
        int safeSize = PublicCommodityBrowseLimits.clampSize(size);
        // 获取当前用户ID（如果已登录）
        String userId = SecurityUtils.getCurrentUserId();
        Optional<CommoditySearchResult> searchResultOptional = commoditySearchService.search(
                keyword, safePage, safeSize, location, minPrice, maxPrice, category, sortBy, userId);
        if (searchResultOptional.isEmpty()) {
            return null;
        }
        CommoditySearchResult searchResult = searchResultOptional.get();
        List<String> commodityIds = searchResult.commodityIds();
        if (commodityIds == null || commodityIds.isEmpty()) {
            return buildEmptyPageResult(searchResult.totalHits(), safePage, safeSize);
        }
        List<CommodityInternalDTO> commodities = fetchCommoditiesByOrderedIds(commodityIds);
        if (commodities.isEmpty() && searchResult.totalHits() > 0) {
            return null;
        }
        List<CommodityDTO> commodityDTOs = convertInternalDTOsWithBatchProfile(commodities);
        CommodityPageResultVO result = new CommodityPageResultVO();
        result.setCommodities(commodityDTOs);
        long cappedTotal = PublicCommodityBrowseLimits.capReportedTotal(searchResult.totalHits());
        result.setTotal(cappedTotal);
        result.setPages(PublicCommodityBrowseLimits.calculateReportedPages(cappedTotal, safeSize));
        result.setCurrent(safePage);
        result.setSize(safeSize);
        return result;
    }

    /**
     * 按 ES 返回的 ID 顺序批量获取商品，优先读缓存（Cache Aside）。
     * 去重后查询，最终结果按原 commodityIds 顺序排列。
     */
    private List<CommodityInternalDTO> fetchCommoditiesByOrderedIds(List<String> commodityIds) {
        if (commodityIds == null || commodityIds.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> uniqueIds = new ArrayList<>(new LinkedHashSet<>(commodityIds));
        Map<String, CommodityInternalDTO> commodityMap = batchFetchCommoditiesWithCache(uniqueIds);
        return commodityIds.stream()
            .map(commodityMap::get)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }

    private CommodityPageResultVO buildEmptyPageResult(long totalHits, int page, int size) {
        CommodityPageResultVO result = new CommodityPageResultVO();
        result.setCommodities(Collections.emptyList());
        long capped = PublicCommodityBrowseLimits.capReportedTotal(totalHits);
        result.setTotal(capped);
        result.setPages(PublicCommodityBrowseLimits.calculateReportedPages(capped, size));
        result.setCurrent(page);
        result.setSize(size);
        return result;
    }

    /**
     * 批量转换Commodity实体为CommodityDTO（优化 N+1 查询）
     * 批量查询所有商品的卖家 Profile，然后填充到 DTO
     */
    private List<CommodityDTO> convertCommoditiesToDTOWithBatchProfile(List<Commodity> commodities) {
        if (commodities == null || commodities.isEmpty()) {
            return new ArrayList<>();
        }
        
        // ✅ 收集所有唯一的卖家ID
        Set<String> sellerIds = commodities.stream()
                .map(Commodity::getSellerId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        
        // ✅ 批量查询所有卖家的 Profile（Redis → Feign 回退）
        Map<String, UserProfileInternalDTO> profileMap = userCacheService.getUserProfilesByIds(sellerIds);
        
        // ✅ 转换为 DTO，并从 Map 中获取卖家信息
        final Map<String, UserProfileInternalDTO> finalProfileMap = profileMap;
        return commodities.stream()
                .map(commodity -> {
                    CommodityDTO dto = convertToDTO(commodity);
                    // 从 Map 中获取卖家 Profile 信息（使用内部 DTO）
                    UserProfileInternalDTO sellerProfile = finalProfileMap.get(commodity.getSellerId());
                    if (sellerProfile != null) {
                        dto.setSellerNickname(sellerProfile.getNickname());
                        dto.setSellerAvatar(sellerProfile.getAvatar());
                    }
                    return dto;
                })
                .collect(Collectors.toList());
    }
    
    /**
     * 转换Commodity实体为CommodityDTO（单条商品场景使用，如商品详情）
     */
    private CommodityDTO convertToDTO(Commodity commodity) {
        CommodityDTO dto = new CommodityDTO();
        dto.setCommodityId(commodity.getCommodityId());
        dto.setSellerId(commodity.getSellerId());
        dto.setTitle(commodity.getTitle());
        dto.setDescription(commodity.getDescription());
        dto.setPrice(commodity.getPrice());
        dto.setStock(commodity.getStock());
        dto.setLocation(commodity.getLocation());
        dto.setAddressId(commodity.getAddressId());
        
        // 地址快照字段
        dto.setAddressSnapshotProvince(commodity.getAddressSnapshotProvince());
        dto.setAddressSnapshotCity(commodity.getAddressSnapshotCity());
        dto.setAddressSnapshotDistrict(commodity.getAddressSnapshotDistrict());
        dto.setAddressSnapshotStreet(commodity.getAddressSnapshotStreet());
        dto.setAddressSnapshotDetail(commodity.getAddressSnapshotDetail());
        dto.setAddressSnapshotFull(commodity.getAddressSnapshotFull());
        
        // 地理位置字段
        dto.setLongitude(commodity.getLongitude());
        dto.setLatitude(commodity.getLatitude());
        
        dto.setPublishTime(commodity.getPublishTime());
        dto.setCommodityStatus(commodity.getCommodityStatus());
        dto.setBuyerVisibility(commodity.getBuyerVisibility());
        dto.setCategory(commodity.getCategory());
        dto.setConditionLevel(commodity.getConditionLevel());
        
        // 处理图片字段
        if (commodity.getImages() != null && !commodity.getImages().isEmpty()) {
            dto.setImages(Arrays.asList(commodity.getImages().split(",")));
        } else {
            dto.setImages(new ArrayList<>());
        }
        
        dto.setClickCount(commodity.getClickCount());
        
        return dto;
    }
    
    /**
     * 批量从缓存（Cache Aside）获取商品基础数据，缓存缺失的批量查数据库并回写。
     * 使用 CACHE_COMMODITY_KEY 缓存 CommodityInternalDTO（不含卖家信息）。
     *
     * @return Map&lt;commodityId, CommodityInternalDTO&gt;，保留原始顺序
     */
    private Map<String, CommodityInternalDTO> batchFetchCommoditiesWithCache(Collection<String> ids) {
        if (ids == null || ids.isEmpty()) return Collections.emptyMap();

        Map<String, CommodityInternalDTO> result = new LinkedHashMap<>();
        List<String> missingIds = new ArrayList<>();

        for (String id : ids) {
            CommodityInternalDTO cached = cacheUtil.get(
                RedisConstants.CACHE_COMMODITY_KEY + id, CommodityInternalDTO.class);
            if (cached != null) {
                result.put(id, cached);
            } else {
                missingIds.add(id);
            }
        }

        if (!missingIds.isEmpty()) {
            List<Commodity> commodities = commodityRepository.findAllById(missingIds);
            for (Commodity c : commodities) {
                CommodityInternalDTO dto = commodityInternalDTOConverter.toInternalDTO(c);
                cacheUtil.set(RedisConstants.CACHE_COMMODITY_KEY + c.getCommodityId(),
                    dto, RedisConstants.CACHE_COMMODITY_TTL * 60);
                result.put(c.getCommodityId(), dto);
            }
        }

        return result;
    }

    /**
     * 将 CommodityInternalDTO 列表转换为 CommodityDTO 列表，并批量填充卖家信息。
     */
    private List<CommodityDTO> convertInternalDTOsWithBatchProfile(List<CommodityInternalDTO> internalDTOs) {
        if (internalDTOs == null || internalDTOs.isEmpty()) return new ArrayList<>();

        Set<String> sellerIds = internalDTOs.stream()
            .map(CommodityInternalDTO::getSellerId)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());

        Map<String, UserProfileInternalDTO> profileMap = userCacheService.getUserProfilesByIds(sellerIds);

        return internalDTOs.stream()
            .map(internalDto -> {
                CommodityDTO dto = convertInternalDTOtoCommodityDTO(internalDto);
                UserProfileInternalDTO profile = profileMap.get(internalDto.getSellerId());
                if (profile != null) {
                    dto.setSellerNickname(profile.getNickname());
                    dto.setSellerAvatar(profile.getAvatar());
                }
                return dto;
            })
            .collect(Collectors.toList());
    }

    /**
     * CommodityInternalDTO → CommodityDTO 字段映射。
     */
    private CommodityDTO convertInternalDTOtoCommodityDTO(CommodityInternalDTO src) {
        CommodityDTO dto = new CommodityDTO();
        dto.setCommodityId(src.getCommodityId());
        dto.setSellerId(src.getSellerId());
        dto.setTitle(src.getTitle());
        dto.setDescription(src.getDescription());
        dto.setPrice(src.getPrice() != null ? src.getPrice().doubleValue() : null);
        dto.setStock(src.getStock());
        dto.setLocation(src.getLocation());
        dto.setAddressId(src.getAddressId());
        dto.setAddressSnapshotProvince(src.getAddressSnapshotProvince());
        dto.setAddressSnapshotCity(src.getAddressSnapshotCity());
        dto.setAddressSnapshotDistrict(src.getAddressSnapshotDistrict());
        dto.setAddressSnapshotStreet(src.getAddressSnapshotStreet());
        dto.setAddressSnapshotDetail(src.getAddressSnapshotDetail());
        dto.setAddressSnapshotFull(src.getAddressSnapshotFull());
        dto.setLongitude(src.getLongitude());
        dto.setLatitude(src.getLatitude());
        dto.setPublishTime(src.getCreateTime()); // InternalDTO 中 createTime 对应 publishTime
        dto.setCommodityStatus(src.getStatus()); // InternalDTO 中 status 对应 commodityStatus
        dto.setBuyerVisibility(src.getBuyerVisibility());
        dto.setCategory(src.getCategory());
        dto.setConditionLevel(src.getConditionLevel());
        dto.setClickCount(src.getClickCount());
        if (src.getImages() != null && !src.getImages().isEmpty()) {
            dto.setImages(Arrays.asList(src.getImages().split(",")));
        } else {
            dto.setImages(new ArrayList<>());
        }
        return dto;
    }

    /**
     * 根据排序参数创建Pageable
     * @param page 页码
     * @param size 每页数量
     * @param sortBy 排序方式
     * @return Pageable对象
     */
    private Pageable createPageable(Integer page, Integer size, String sortBy) {
        Sort sort;
        
        if (StringUtils.hasText(sortBy)) {
            switch (sortBy) {
                case "price_asc":
                    sort = Sort.by(Sort.Direction.ASC, "price");
                    break;
                case "price_desc":
                    sort = Sort.by(Sort.Direction.DESC, "price");
                    break;
                case "latest":
                    sort = Sort.by(Sort.Direction.DESC, "publishTime");
                    break;
                default:
                    sort = Sort.by(Sort.Direction.DESC, "publishTime");
                    break;
            }
        } else {
            // 默认按发布时间降序
            sort = Sort.by(Sort.Direction.DESC, "publishTime");
        }
        
        return PageRequest.of(page - 1, size, sort);
    }
    
    // ========== 批量查询（用于聊天界面） ==========
    
    @Override
    public Result getCommoditiesBatchStatus(List<String> commodityIds) {
        try {
            if (commodityIds == null || commodityIds.isEmpty()) {
                return Result.ok("批量查询成功", Collections.emptyList());
            }

            // ✅ Cache Aside：批量读缓存，缺失的从数据库补充并回写
            Set<String> uniqueIds = new LinkedHashSet<>(commodityIds);
            Map<String, CommodityInternalDTO> commodityMap = batchFetchCommoditiesWithCache(uniqueIds);

            // ✅ 批量查询卖家 Profile（Redis → Feign 回退）
            Set<String> sellerIds = commodityMap.values().stream()
                .map(CommodityInternalDTO::getSellerId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
            Map<String, UserProfileInternalDTO> profileMap = userCacheService.getUserProfilesByIds(sellerIds);

            List<Map<String, Object>> result = commodityMap.values().stream()
                .map(c -> {
                    Map<String, Object> item = new HashMap<>();
                    item.put("commodityId", c.getCommodityId());
                    item.put("sellerId", c.getSellerId());
                    item.put("title", c.getTitle());
                    item.put("description", c.getDescription());
                    item.put("price", c.getPrice() != null ? c.getPrice().doubleValue() : null);
                    item.put("commodityStatus", c.getStatus());
                    item.put("stock", c.getStock());
                    item.put("location", c.getLocation());
                    item.put("category", c.getCategory());
                    item.put("conditionLevel", c.getConditionLevel());
                    item.put("publishTime", c.getCreateTime() != null ? c.getCreateTime().toString() : null);

                    if (StringUtils.hasText(c.getImages())) {
                        List<String> imagesList = Arrays.stream(c.getImages().split(","))
                            .map(String::trim)
                            .filter(StringUtils::hasText)
                            .collect(Collectors.toList());
                        item.put("images", imagesList);
                    } else {
                        item.put("images", new ArrayList<>());
                    }

                    UserProfileInternalDTO profile = profileMap.get(c.getSellerId());
                    if (profile != null) {
                        item.put("sellerNickname", profile.getNickname());
                        item.put("sellerAvatar", profile.getAvatar());
                    }
                    return item;
                })
                .collect(Collectors.toList());

            log.info("Batch query commodity status successful - queried {} commodities, returned {} commodities", uniqueIds.size(), result.size());
            return Result.ok("Batch query successful", result);

        } catch (Exception e) {
            log.error("Failed to batch query commodity status: {}", e.getMessage(), e);
            throw new BusinessException("Batch query failed");
        }
    }

    // ========== 内部接口实现 ==========

    @Override
    public CommodityInternalDTO getCommodityByIdInternal(String commodityId) {
        return cacheUtil.getWithFallback(
            RedisConstants.CACHE_COMMODITY_KEY + commodityId,
            RedisConstants.CACHE_COMMODITY_TTL * 60,
            CommodityInternalDTO.class,
            () -> {
                Commodity commodity = commodityRepository.findById(commodityId)
                    .orElseThrow(() -> new BusinessException("商品不存在"));
                return commodityInternalDTOConverter.toInternalDTO(commodity);
            }
        );
    }

    @Override
    public List<CommodityInternalDTO> getCommoditiesByIdsInternal(List<String> commodityIds) {
        if (commodityIds == null || commodityIds.isEmpty()) {
            return Collections.emptyList();
        }
        Map<String, CommodityInternalDTO> resultMap = batchFetchCommoditiesWithCache(commodityIds);
        return new ArrayList<>(resultMap.values());
    }

    @Override
    public Map<String, Object> listCommoditiesInternal(Integer page, Integer size,
                                                        String keyword, String category,
                                                        String conditionLevel, String status,
                                                        String buyerVisibility,
                                                        String sortProp, String sortOrder) {
        Sort sort = Sort.by(Sort.Direction.DESC, "createTime");
        if (StringUtils.hasText(sortProp)) {
            Sort.Direction direction = "desc".equalsIgnoreCase(sortOrder)
                ? Sort.Direction.DESC : Sort.Direction.ASC;
            sort = Sort.by(direction, sortProp);
        }
        Pageable pageable = PageRequest.of(Math.max(0, page - 1), size, sort);

        Specification<Commodity> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (StringUtils.hasText(keyword)) {
                String kw = keyword.trim().toLowerCase();
                predicates.add(cb.or(
                    cb.like(cb.lower(root.get("title")), "%" + kw + "%"),
                    cb.like(cb.lower(root.get("description")), "%" + kw + "%"),
                    cb.like(cb.lower(root.get("commodityId")), "%" + kw + "%")
                ));
            }
            if (StringUtils.hasText(category)) {
                predicates.add(cb.equal(root.get("category"), category.trim()));
            }
            if (StringUtils.hasText(conditionLevel)) {
                predicates.add(cb.equal(root.get("conditionLevel"), conditionLevel.trim()));
            }
            if (StringUtils.hasText(status)) {
                predicates.add(cb.equal(root.get("commodityStatus"), status.trim()));
            }
            if (StringUtils.hasText(buyerVisibility)) {
                predicates.add(cb.equal(root.get("buyerVisibility"), buyerVisibility.trim()));
            }
            return predicates.isEmpty() ? cb.conjunction() : cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<Commodity> commodityPage = commodityRepository.findAll(spec, pageable);
        List<CommodityInternalDTO> dtos = commodityInternalDTOConverter
            .toCommodityInternalDTOList(commodityPage.getContent());

        Map<String, Object> result = new HashMap<>();
        result.put("content", dtos);
        result.put("totalElements", commodityPage.getTotalElements());
        result.put("totalPages", commodityPage.getTotalPages());
        result.put("number", commodityPage.getNumber());
        result.put("size", commodityPage.getSize());
        return result;
    }
}