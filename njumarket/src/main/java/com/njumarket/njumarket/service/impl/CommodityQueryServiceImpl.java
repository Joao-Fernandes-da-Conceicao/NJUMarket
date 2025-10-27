package com.njumarket.njumarket.service.impl;

import com.njumarket.njumarket.dto.Result;
import com.njumarket.njumarket.dto.CommodityDTO;
import com.njumarket.njumarket.entity.Commodity;
import com.njumarket.njumarket.entity.User;
import com.njumarket.njumarket.repository.CommodityRepository;
import com.njumarket.njumarket.service.CommodityQueryService;
import com.njumarket.njumarket.utils.UserHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;
import java.util.HashMap;
import java.util.Map;
import java.util.Collections;

/**
 * 商品查询服务实现类
 * 专门处理商品查询和可见性逻辑
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CommodityQueryServiceImpl implements CommodityQueryService {
    
    private final CommodityRepository commodityRepository;
    
    // ========== 公开商品查询实现 ==========
    
    @Override
    public Result searchCommodities(String keyword, Integer page, Integer size, String location, Double minPrice, Double maxPrice, String category, String sortBy) {
        try {
            log.info("搜索商品 - keyword: {}, page: {}, size: {}, location: {}, minPrice: {}, maxPrice: {}, category: {}, sortBy: {}", 
                    keyword, page, size, location, minPrice, maxPrice, category, sortBy);
                
            // 根据排序参数创建Pageable
            Pageable pageable = createPageable(page, size, sortBy);
            
            Page<Commodity> commodityPage;
            
            if (StringUtils.hasText(keyword)) {
                commodityPage = commodityRepository.searchByKeyword(keyword.trim(), pageable);
            } else if (StringUtils.hasText(category)) {
                commodityPage = commodityRepository.findByCategoryAndVisible(category.trim(), pageable);
            } else {
                // 如果没有关键词和分类，返回所有公开商品
                commodityPage = commodityRepository.findByCommodityStatusAndSellerVisibilityAndBuyerVisibility(
                    "ON_SHELF", "PUBLIC", "PUBLIC", pageable
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
            
            List<CommodityDTO> commodityDTOs = filteredCommodities.stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());
            
            Map<String, Object> result = new HashMap<>();
            result.put("commodities", commodityDTOs);
            result.put("total", commodityPage.getTotalElements());
            result.put("pages", commodityPage.getTotalPages());
            result.put("current", page);
            result.put("size", size);
            
            log.info("搜索商品成功 - 找到 {} 个商品", commodityDTOs.size());
            return Result.ok("搜索商品成功", result);
            
        } catch (Exception e) {
            log.error("搜索商品失败: {}", e.getMessage(), e);
            return Result.fail("搜索失败，请稍后重试");
        }
    }
    
    @Override
    public Result getCommoditiesByCategory(String category, Integer page, Integer size) {
        try {
            log.info("根据分类查询商品 - category: {}, page: {}, size: {}", category, page, size);
            
            Pageable pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "publishTime"));
            Page<Commodity> commodityPage = commodityRepository.findByCategoryAndVisible(category, pageable);
            
            List<CommodityDTO> commodityDTOs = commodityPage.getContent().stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());
            
            Map<String, Object> result = new HashMap<>();
            result.put("commodities", commodityDTOs);
            result.put("total", commodityPage.getTotalElements());
            result.put("pages", commodityPage.getTotalPages());
            result.put("current", page);
            result.put("size", size);
            
            return Result.ok("获取分类商品成功", result);
            
        } catch (Exception e) {
            log.error("根据分类查询商品失败: {}", e.getMessage(), e);
            return Result.fail("查询失败，请稍后重试");
        }
    }
    
    @Override
    public Result getCommodityDetail(String commodityId) {
        try {
            log.info("获取商品详情 - commodityId: {}", commodityId);
            
            Commodity commodity = commodityRepository.findById(commodityId).orElse(null);
            if (commodity == null) {
                return Result.fail("商品不存在");
            }
            
            // 检查可见性权限
            User currentUser = UserHolder.getUser();
            if (!canUserViewCommodity(commodity, currentUser)) {
                return Result.fail("无权限查看此商品");
            }
            
            // 增加点击量
            commodity.setClickCount(commodity.getClickCount() + 1);
            commodityRepository.save(commodity);
            
            CommodityDTO commodityDTO = convertToDTO(commodity);
            return Result.ok("获取商品详情成功", commodityDTO);
            
        } catch (Exception e) {
            log.error("获取商品详情失败: {}", e.getMessage(), e);
            return Result.fail("获取商品详情失败");
        }
    }
    
    @Override
    public Result getHotCommodities(Integer limit) {
        try {
            log.info("获取热门商品 - limit: {}", limit);
            
            Pageable pageable = PageRequest.of(0, limit);
            List<Commodity> commodities = commodityRepository.findHotCommodities(pageable);
            
            List<CommodityDTO> commodityDTOs = commodities.stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());
            
            return Result.ok("获取热门商品成功", commodityDTOs);
            
        } catch (Exception e) {
            log.error("获取热门商品失败: {}", e.getMessage(), e);
            return Result.fail("获取热门商品失败");
        }
    }
    
    @Override
    public Result getLatestCommodities(Integer limit) {
        try {
            log.info("获取最新商品 - limit: {}", limit);
            
            Pageable pageable = PageRequest.of(0, limit);
            List<Commodity> commodities = commodityRepository.findLatestCommodities(pageable);
            
            List<CommodityDTO> commodityDTOs = commodities.stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());
            
            return Result.ok("获取最新商品成功", commodityDTOs);
            
        } catch (Exception e) {
            log.error("获取最新商品失败: {}", e.getMessage(), e);
            return Result.fail("获取最新商品失败");
        }
    }
    
    @Override
    public Result getCategories() {
        try {
            log.info("获取商品分类");
            
            // 这里可以从数据库获取分类，或者返回预定义的分类
            List<String> categories = List.of(
                "电子产品", "服装配饰", "图书文具", "生活用品", 
                "运动户外", "美妆护肤", "食品饮料", "其他"
            );
            
            return Result.ok("获取分类成功", categories);
            
        } catch (Exception e) {
            log.error("获取商品分类失败: {}", e.getMessage(), e);
            return Result.fail("获取分类失败");
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
            return Result.fail("获取推荐商品失败");
        }
    }
    
    @Override
    public Result recordView(String commodityId, String sessionId) {
        try {
            log.info("记录商品浏览 - commodityId: {}, sessionId: {}", commodityId, sessionId);
            
            Commodity commodity = commodityRepository.findById(commodityId).orElse(null);
            if (commodity == null) {
                return Result.fail("商品不存在");
            }
            
            // 增加点击量
            commodity.setClickCount(commodity.getClickCount() + 1);
            commodityRepository.save(commodity);
            
            return Result.ok("记录浏览成功");
            
        } catch (Exception e) {
            log.error("记录商品浏览失败: {}", e.getMessage(), e);
            return Result.fail("记录浏览失败");
        }
    }
    
    @Override
    public Result aiSearch(String query, String location) {
        try {
            log.info("AI语义搜索 - query: {}, location: {}", query, location);
            
            // 简单的AI搜索实现：基于关键词搜索
            return searchCommodities(query, 1, 10, location, null, null, null, null);
            
        } catch (Exception e) {
            log.error("AI语义搜索失败: {}", e.getMessage(), e);
            return Result.fail("AI搜索失败");
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
                return Result.fail("卖家ID不能为空");
            }
            
            boolean isOwnCommodities = user != null && user.getUserId().equals(targetSellerId);
            
            log.info("获取用户商品 - user: {}, sellerId: {}, isOwn: {}, status: {}, page: {}, size: {}", 
                    user != null ? user.getUserId() : "anonymous", targetSellerId, isOwnCommodities, status, page, size);
            
            Pageable pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "publishTime"));
            Page<Commodity> commodityPage;
            
            // 标准化状态参数
            String normalizedStatus = (status == null || status.trim().isEmpty()) ? "all" : status;
            
            // 如果是查看其他用户的商品，不能查看草稿状态
            if (!isOwnCommodities && "DRAFT".equals(normalizedStatus)) {
                return Result.fail("无法查看其他用户的草稿商品");
            }
            
            // 如果是自己的商品，直接查询并返回
            if (isOwnCommodities) {
                // 查询商品
                if ("all".equals(normalizedStatus)) {
                    commodityPage = commodityRepository.findBySellerId(targetSellerId, pageable);
                } else {
                    commodityPage = commodityRepository.findBySellerIdAndCommodityStatus(targetSellerId, normalizedStatus, pageable);
                }
                
                List<CommodityDTO> commodityDTOs = commodityPage.getContent().stream()
                        .map(this::convertToDTO)
                        .collect(Collectors.toList());
                
                Map<String, Object> result = new HashMap<>();
                result.put("commodities", commodityDTOs);
                result.put("total", commodityPage.getTotalElements());
                result.put("pages", commodityPage.getTotalPages());
                result.put("current", page);
                result.put("size", size);
                
                return Result.ok("获取用户商品成功", result);
            }
            
            // 查看其他用户的商品，需要过滤草稿和不可见商品
            if ("all".equals(normalizedStatus)) {
                // 查询所有非草稿状态的商品
                Pageable allPageable = PageRequest.of(0, Integer.MAX_VALUE, Sort.by(Sort.Direction.DESC, "publishTime"));
                Page<Commodity> allCommoditiesPage = commodityRepository.findBySellerId(targetSellerId, allPageable);
                
                // 过滤掉草稿和不可见的商品
                List<Commodity> filteredCommodities = allCommoditiesPage.getContent().stream()
                        .filter(c -> !"DRAFT".equals(c.getCommodityStatus()))
                        .filter(c -> isCommodityVisibleToUser(c, user))
                        .collect(Collectors.toList());
                
                // 手动分页
                int start = (int) pageable.getOffset();
                int end = Math.min(start + size, filteredCommodities.size());
                List<Commodity> pagedList = start < filteredCommodities.size() 
                        ? filteredCommodities.subList(start, end) 
                        : Collections.emptyList();
                
                List<CommodityDTO> commodityDTOs = pagedList.stream()
                        .map(this::convertToDTO)
                        .collect(Collectors.toList());
                
                Map<String, Object> result = new HashMap<>();
                result.put("commodities", commodityDTOs);
                result.put("total", (long) filteredCommodities.size());
                result.put("pages", (int) Math.ceil((double) filteredCommodities.size() / size));
                result.put("current", page);
                result.put("size", size);
                
                return Result.ok("获取用户商品成功", result);
            } else {
                // 按状态查询（只支持 ON_SHELF 和 OFF_SHELF）
                if (!"ON_SHELF".equals(normalizedStatus) && !"OFF_SHELF".equals(normalizedStatus) && !"PUBLISHED".equals(normalizedStatus)) {
                    return Result.fail("不支持的状态筛选，只支持 ON_SHELF、OFF_SHELF 和 PUBLISHED");
                }
                
                commodityPage = commodityRepository.findBySellerIdAndCommodityStatus(targetSellerId, normalizedStatus, pageable);
                
                // 过滤可见性
                List<CommodityDTO> commodityDTOs = commodityPage.getContent().stream()
                        .filter(commodity -> isCommodityVisibleToUser(commodity, user))
                        .map(this::convertToDTO)
                        .collect(Collectors.toList());
                
                Map<String, Object> result = new HashMap<>();
                result.put("commodities", commodityDTOs);
                result.put("total", commodityPage.getTotalElements());
                result.put("pages", commodityPage.getTotalPages());
                result.put("current", page);
                result.put("size", size);
                
                return Result.ok("获取用户商品成功", result);
            }
            
        } catch (Exception e) {
            log.error("获取用户商品失败: {}", e.getMessage(), e);
            return Result.fail("获取用户商品失败");
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
        
        // 商品必须是公开可见
        if (!"PUBLIC".equals(commodity.getSellerVisibility()) || 
            !"PUBLIC".equals(commodity.getBuyerVisibility())) {
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
        
        // 其他用户只能查询公开的商品（包括下架商品）
        return "PUBLIC".equals(commodity.getSellerVisibility()) && 
               "PUBLIC".equals(commodity.getBuyerVisibility());
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
            User currentUser = UserHolder.getUser();
            
            Commodity commodity = commodityRepository.findById(commodityId).orElse(null);
            if (commodity == null) {
                return Result.fail("商品不存在");
            }
            
            // 检查权限：使用新的查询权限检查
            if (!canCommodityBeQueried(commodity, currentUser)) {
                return Result.fail("无权限查看此商品");
            }
            
            // 转换为DTO
            CommodityDTO commodityDTO = convertToDTO(commodity);
            
            // 添加商品状态信息
            Map<String, Object> result = new HashMap<>();
            result.put("commodity", commodityDTO);
            result.put("canOrder", canCommodityBeOrdered(commodity));
            result.put("isOffShelf", !canCommodityBeOrdered(commodity));
            result.put("statusMessage", getCommodityStatusMessage(commodity));
            
            return Result.ok("获取商品详情成功", result);
            
        } catch (Exception e) {
            log.error("根据ID获取商品详情失败: {}", e.getMessage(), e);
            return Result.fail("获取商品详情失败");
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
            return Result.fail("获取统计信息失败");
        }
    }
    
    // ========== 私有辅助方法 ==========
    
    /**
     * 转换Commodity实体为CommodityDTO
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
        dto.setPublishTime(commodity.getPublishTime());
        dto.setCommodityStatus(commodity.getCommodityStatus());
        dto.setSellerVisibility(commodity.getSellerVisibility());
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
}