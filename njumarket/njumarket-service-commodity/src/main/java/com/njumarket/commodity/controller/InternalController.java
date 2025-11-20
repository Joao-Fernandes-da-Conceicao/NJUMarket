package com.njumarket.commodity.controller;

import com.njumarket.njumarket.dto.Result;
import com.njumarket.njumarket.dto.internal.CommodityInternalDTO;
import com.njumarket.commodity.dto.internal.CommodityInternalDTOConverter;
import com.njumarket.commodity.entity.Commodity;
import com.njumarket.njumarket.exception.BusinessException;
import com.njumarket.commodity.repository.CommodityRepository;
import com.njumarket.njumarket.utils.CacheUtil;
import com.njumarket.njumarket.utils.RedisConstants;
import com.njumarket.commodity.search.CommoditySearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 内部API控制器
 * 用于微服务间调用，不对外暴露
 */
@Slf4j
@RestController
@RequestMapping("/api/internal")
@RequiredArgsConstructor
public class InternalController {
    
    private final CommodityRepository commodityRepository;
    private final CommodityInternalDTOConverter commodityInternalDTOConverter;
    private final CacheUtil cacheUtil;
    private final CommoditySearchService commoditySearchService;
    
    /**
     * 根据ID查询商品详情（管理端内部接口）
     * 返回内部 DTO，不包含关联对象
     */
    @GetMapping("/commodity/{commodityId}")
    public Result getCommodityById(@PathVariable String commodityId) {
        Commodity commodity = commodityRepository.findById(commodityId)
            .orElseThrow(() -> new BusinessException("商品不存在"));
        CommodityInternalDTO dto = commodityInternalDTOConverter.toInternalDTO(commodity);
            return Result.ok("查询成功", dto);
    }
    
    /**
     * 查询商品（带悲观锁，用于创建订单）
     * 返回内部 DTO，不包含关联对象
     * 
     * ✅ 注意：findByIdForUpdate 使用 SELECT ... FOR UPDATE，需要写事务（不能是只读事务）
     */
    @GetMapping("/commodity/{commodityId}/for-update")
    @Transactional  // ✅ 添加事务注解，支持 SELECT ... FOR UPDATE（必须是写事务，不能是只读）
    public Result getCommodityForUpdate(@PathVariable String commodityId) {
        Commodity commodity = commodityRepository.findByIdForUpdate(commodityId)
            .orElseThrow(() -> new BusinessException("商品不存在"));
        CommodityInternalDTO dto = commodityInternalDTOConverter.toInternalDTO(commodity);
            return Result.ok("查询成功", dto);
    }
    
    /**
     * 更新商品库存（扣减）
     * 
     * ✅ 注意：@Modifying 查询需要事务，必须添加 @Transactional 注解
     */
    @PostMapping("/commodity/{commodityId}/update-stock")
    @Transactional  // ✅ 添加事务注解，支持 @Modifying 查询
    public Result updateCommodityStock(@PathVariable String commodityId, 
                                      @RequestParam Integer quantity) {
            int updateResult = commodityRepository.updateStockWithCondition(commodityId, quantity);
            if (updateResult == 0) {
            throw new BusinessException("库存不足");
            }
            return Result.ok("库存更新成功");
    }
    
    /**
     * 恢复商品库存（增加）
     * 
     * ✅ 注意：save 操作需要事务，必须添加 @Transactional 注解
     */
    @PostMapping("/commodity/{commodityId}/restore-stock")
    @Transactional  // ✅ 添加事务注解，支持 save 操作
    public Result restoreCommodityStock(@PathVariable String commodityId, 
                                        @RequestParam Integer quantity) {
        Commodity commodity = commodityRepository.findById(commodityId)
            .orElseThrow(() -> new BusinessException("商品不存在"));
            boolean stockUpdated = commodity.updateStock(quantity);
        if (!stockUpdated) {
            throw new BusinessException("库存恢复失败");
        }
                commodityRepository.save(commodity);
                return Result.ok("库存恢复成功");
    }
    
    /**
     * 完整更新商品（管理端内部接口）
     */
    @PutMapping("/commodity/{commodityId}/full")
    public Result updateCommodityFull(@PathVariable String commodityId, 
                                     @RequestBody Map<String, Object> payload) {
        Commodity c = commodityRepository.findById(commodityId)
            .orElseThrow(() -> new BusinessException("商品不存在"));
            
            // 更新字段
            Object title = payload.get("title"); 
            if (title instanceof String) c.setTitle(((String) title).trim());
            
            Object description = payload.get("description"); 
            if (description instanceof String) c.setDescription(((String) description).trim());
            
            // 价格：支持 Number 和 String 类型
            Object price = payload.get("price");
            if (price != null) {
                try {
                    double priceValue = price instanceof Number 
                        ? ((Number) price).doubleValue() 
                        : Double.parseDouble(price.toString().trim());
                    if (priceValue >= 0) {
                        c.setPrice(priceValue);
                    }
                } catch (NumberFormatException | NullPointerException ignored) {
                    // 忽略无效值
                }
            }
            
            // 库存：支持 Number 和 String 类型
            Object stock = payload.get("stock");
            if (stock != null) {
                try {
                    int stockValue = stock instanceof Number 
                        ? ((Number) stock).intValue() 
                        : Integer.parseInt(stock.toString().trim());
                    if (stockValue >= 0) {
                        c.setStock(stockValue);
                    }
                } catch (NumberFormatException | NullPointerException ignored) {
                    // 忽略无效值
                }
            }
            
            Object location = payload.get("location"); 
            if (location instanceof String) c.setLocation(((String) location).trim());
            
            Object category = payload.get("category"); 
            if (category instanceof String) c.setCategory(((String) category).trim());
            
            Object conditionLevel = payload.get("conditionLevel");
            if (conditionLevel instanceof String) {
                String lvl = ((String) conditionLevel).trim();
                java.util.Set<String> allowedLvl = new java.util.HashSet<>(java.util.Arrays.asList(
                    "全新","九成新","八成新","七成新","六成新","五成新"
                ));
                if (!allowedLvl.contains(lvl)) {
                throw new BusinessException("非法的成色等级");
                }
                c.setConditionLevel(lvl);
            }
            
            Object commodityStatus = payload.get("commodityStatus");
            if (commodityStatus instanceof String) {
                String st = ((String) commodityStatus).trim();
                java.util.Set<String> allowedStatus = new java.util.HashSet<>(java.util.Arrays.asList("DRAFT","PUBLISHED","ON_SHELF","OFF_SHELF"));
                if (!allowedStatus.contains(st)) {
                throw new BusinessException("非法的商品状态");
                }
                c.setCommodityStatus(st);
            }
            
            Object images = payload.get("images"); 
            if (images instanceof String) c.setImages(((String) images).trim());
            
            Object addressProvince = payload.get("addressSnapshotProvince");
            if (addressProvince instanceof String) c.setAddressSnapshotProvince(((String) addressProvince).trim());
            Object addressCity = payload.get("addressSnapshotCity");
            if (addressCity instanceof String) c.setAddressSnapshotCity(((String) addressCity).trim());
            Object addressDistrict = payload.get("addressSnapshotDistrict");
            if (addressDistrict instanceof String) c.setAddressSnapshotDistrict(((String) addressDistrict).trim());
            Object addressStreet = payload.get("addressSnapshotStreet");
            if (addressStreet instanceof String) c.setAddressSnapshotStreet(((String) addressStreet).trim());
            Object addressDetail = payload.get("addressSnapshotDetail");
            if (addressDetail instanceof String) c.setAddressSnapshotDetail(((String) addressDetail).trim());
            Object addressFull = payload.get("addressSnapshotFull");
            if (addressFull instanceof String) c.setAddressSnapshotFull(((String) addressFull).trim());
            
            // 点击量：支持 Number 和 String 类型
            Object clickCount = payload.get("clickCount");
            if (clickCount != null) {
                try {
                    int count = clickCount instanceof Number 
                        ? ((Number) clickCount).intValue() 
                        : Integer.parseInt(clickCount.toString().trim());
                    if (count >= 0) {
                        c.setClickCount(count);
                    }
                } catch (NumberFormatException | NullPointerException ignored) {
                    // 忽略无效值
                }
            }
            
            // 分类校验
            if (category instanceof String) {
                String cat = ((String) category).trim();
                java.util.Set<String> allowedCat = new java.util.HashSet<>(java.util.Arrays.asList(
                    "电子产品","服装配饰","图书文具","生活用品","运动户外","美妆护肤","其他"
                ));
                if (!allowedCat.contains(cat)) {
                throw new BusinessException("非法的商品分类");
                }
                c.setCategory(cat);
            }
            
            // 可见性（允许编辑）
            Object sellerVisibility = payload.get("sellerVisibility");
            if (sellerVisibility instanceof String) {
                String vis = ((String) sellerVisibility).trim();
                java.util.Set<String> allowedVis = new java.util.HashSet<>(java.util.Arrays.asList("PUBLIC","PRIVATE","HIDDEN"));
                if (!allowedVis.contains(vis)) {
                throw new BusinessException("非法的卖家可见性");
                }
                c.setSellerVisibility(vis);
            }
            
            Object buyerVisibility = payload.get("buyerVisibility");
            if (buyerVisibility instanceof String) {
                String vis = ((String) buyerVisibility).trim();
                java.util.Set<String> allowedVis = new java.util.HashSet<>(java.util.Arrays.asList("PUBLIC","PRIVATE","HIDDEN"));
                if (!allowedVis.contains(vis)) {
                throw new BusinessException("非法的买家可见性");
                }
                c.setBuyerVisibility(vis);
            }
            
            commodityRepository.save(c);
            commoditySearchService.syncCommodity(c);
            
            // ✅ Cache Aside模式：先更新数据库，再删除缓存
            // 判断是否需要删除列表缓存（状态、可见性、点击量变更会影响列表）
            boolean evictListCache = payload.containsKey("commodityStatus") 
                    || payload.containsKey("sellerVisibility") 
                    || payload.containsKey("buyerVisibility")
                    || payload.containsKey("clickCount"); // 点击量影响热门商品排序
            
            if (cacheUtil != null) {
                try {
                    // 清除商品详情缓存（所有操作都需要删除）
                    cacheUtil.delete(RedisConstants.CACHE_COMMODITY_DETAIL_KEY + commodityId);
                    
                    // 只有在影响列表的操作时才删除列表缓存
                    if (evictListCache) {
                        // 清除热门商品和最新商品缓存（使用通配符删除所有limit的缓存）
                        cacheUtil.deleteByPattern(RedisConstants.CACHE_COMMODITY_HOT_KEY + ":*");
                        cacheUtil.deleteByPattern(RedisConstants.CACHE_COMMODITY_LATEST_KEY + ":*");
                        
                        // 清除商品列表缓存（使用通配符删除所有列表缓存）
                        cacheUtil.deleteByPattern(RedisConstants.CACHE_COMMODITY_LIST_KEY + "*");
                        
                        log.debug("商品缓存已清除（包括列表缓存）: commodityId={}", commodityId);
                    } else {
                        log.debug("商品详情缓存已清除: commodityId={}", commodityId);
                    }
                } catch (Exception e) {
                    log.error("清除商品缓存失败: commodityId={}, error={}", commodityId, e.getMessage(), e);
                    // 缓存清除失败不影响主流程
                }
            }
            
            return Result.ok("更新成功", c);
    }
    
    /**
     * 删除商品（管理端内部接口）
     */
    @DeleteMapping("/commodity/{commodityId}")
    public Result deleteCommodity(@PathVariable String commodityId) {
        Commodity commodity = commodityRepository.findById(commodityId)
            .orElseThrow(() -> new BusinessException("商品不存在"));
        commodityRepository.delete(commodity);
        commoditySearchService.removeCommodity(commodityId);
        
        // ✅ Cache Aside模式：先删除数据库，再删除缓存
        // 清除商品相关缓存（商品删除后需要清除缓存）
        if (cacheUtil != null) {
            try {
                // 清除商品详情缓存
                cacheUtil.delete(RedisConstants.CACHE_COMMODITY_DETAIL_KEY + commodityId);
                
                // 清除热门商品和最新商品缓存（使用通配符删除所有limit的缓存）
                cacheUtil.deleteByPattern(RedisConstants.CACHE_COMMODITY_HOT_KEY + ":*");
                cacheUtil.deleteByPattern(RedisConstants.CACHE_COMMODITY_LATEST_KEY + ":*");
                
                // 清除商品列表缓存（使用通配符删除所有列表缓存）
                cacheUtil.deleteByPattern(RedisConstants.CACHE_COMMODITY_LIST_KEY + "*");
                
                log.debug("商品缓存已清除: commodityId={}", commodityId);
            } catch (Exception e) {
                log.error("清除商品缓存失败: commodityId={}, error={}", commodityId, e.getMessage(), e);
                // 缓存清除失败不影响主流程
            }
        }
        
        return Result.ok("删除成功");
    }
    
    /**
     * 手动重建商品搜索索引
     */
    @PostMapping("/commodity/search/reindex")
    public Result rebuildCommoditySearchIndex() {
        long total = commoditySearchService.rebuildIndex();
        return Result.ok("搜索索引重建成功", Map.of("indexed", total));
    }

    /**
     * 同步指定商品到搜索索引
     */
    @PostMapping("/commodity/{commodityId}/search-sync")
    public Result syncCommoditySearch(@PathVariable String commodityId) {
        Commodity commodity = commodityRepository.findById(commodityId)
            .orElseThrow(() -> new BusinessException("商品不存在"));
        commoditySearchService.syncCommodity(commodity);
        return Result.ok("搜索索引同步成功");
    }
    
    /**
     * 查询商品列表（管理端内部接口）
     */
    @GetMapping("/commodities")
    public Result listCommodities(@RequestParam(defaultValue = "1") Integer page,
                                  @RequestParam(defaultValue = "10") Integer size,
                                  @RequestParam(required = false) String keyword,
                                  @RequestParam(required = false) String category,
                                  @RequestParam(required = false) String conditionLevel,
                                  @RequestParam(required = false) String status,
                                  @RequestParam(required = false) String sellerVisibility,
                                  @RequestParam(required = false) String buyerVisibility,
                                  @RequestParam(required = false) String sortProp,
                                  @RequestParam(required = false) String sortOrder) {
            // 构建分页参数
            org.springframework.data.domain.Sort sort = org.springframework.data.domain.Sort.by(
                org.springframework.data.domain.Sort.Direction.DESC, "createTime"
            );
            if (org.springframework.util.StringUtils.hasText(sortProp)) {
                org.springframework.data.domain.Sort.Direction direction = 
                    "desc".equalsIgnoreCase(sortOrder) ? 
                    org.springframework.data.domain.Sort.Direction.DESC : 
                    org.springframework.data.domain.Sort.Direction.ASC;
                sort = org.springframework.data.domain.Sort.by(direction, sortProp);
            }
            org.springframework.data.domain.Pageable pageable = 
                org.springframework.data.domain.PageRequest.of(Math.max(0, page - 1), size, sort);
            
            // 构建查询条件
            org.springframework.data.jpa.domain.Specification<Commodity> spec = (root, query, cb) -> {
                java.util.List<jakarta.persistence.criteria.Predicate> predicates = new java.util.ArrayList<>();
                
                // 关键词搜索：标题、描述（处理空字符串）
                if (keyword != null && !keyword.trim().isEmpty()) {
                    String kw = keyword.trim().toLowerCase();
                    predicates.add(cb.or(
                        cb.like(cb.lower(root.get("title")), "%" + kw + "%"),
                        cb.like(cb.lower(root.get("description")), "%" + kw + "%"),
                        cb.like(cb.lower(root.get("commodityId")), "%" + kw + "%")
                    ));
                }
                
                // 分类筛选（处理空字符串）
                if (category != null && !category.trim().isEmpty()) {
                    predicates.add(cb.equal(root.get("category"), category.trim()));
                }
                
                // 成色筛选（处理空字符串）
                if (conditionLevel != null && !conditionLevel.trim().isEmpty()) {
                    predicates.add(cb.equal(root.get("conditionLevel"), conditionLevel.trim()));
                }
                
                // 状态筛选（处理空字符串）
                if (status != null && !status.trim().isEmpty()) {
                    predicates.add(cb.equal(root.get("commodityStatus"), status.trim()));
                }
                
                // 卖家可见性筛选（处理空字符串）
                if (sellerVisibility != null && !sellerVisibility.trim().isEmpty()) {
                    predicates.add(cb.equal(root.get("sellerVisibility"), sellerVisibility.trim()));
                }
                
                // 买家可见性筛选（处理空字符串）
                if (buyerVisibility != null && !buyerVisibility.trim().isEmpty()) {
                    predicates.add(cb.equal(root.get("buyerVisibility"), buyerVisibility.trim()));
                }
                
                return predicates.isEmpty() ? cb.conjunction() : 
                    cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
            };
            
            org.springframework.data.domain.Page<Commodity> commodityPage = 
                commodityRepository.findAll(spec, pageable);
            
            // 转换为内部 DTO 列表
            List<CommodityInternalDTO> commodityDTOs = commodityPage.getContent().stream()
                .map(commodityInternalDTOConverter::toInternalDTO)
                .collect(java.util.stream.Collectors.toList());
            
            // 构建分页结果
            Map<String, Object> result = new java.util.HashMap<>();
            result.put("content", commodityDTOs);
            result.put("totalElements", commodityPage.getTotalElements());
            result.put("totalPages", commodityPage.getTotalPages());
            result.put("number", commodityPage.getNumber());
            result.put("size", commodityPage.getSize());
            
            return Result.ok("查询成功", result);
    }
    
}
