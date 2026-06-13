package com.njumarket.trade.service.impl;

import com.njumarket.njumarket.dto.Result;
import com.njumarket.njumarket.dto.internal.CommodityInternalDTO;
import com.njumarket.trade.dto.CommodityDTO;
import com.njumarket.trade.dto.internal.CommodityInternalDTOConverter;
import com.njumarket.trade.vo.CommodityPageResultVO;
import com.njumarket.njumarket.vo.BatchOperationResultVO;
import com.njumarket.trade.entity.Commodity;
import com.njumarket.trade.entity.User; // User 实体（Commodity Service专用）
import com.njumarket.njumarket.exception.BusinessException;
import com.njumarket.trade.repository.CommodityRepository;
import com.njumarket.trade.service.CommodityService;
import com.njumarket.trade.service.CommodityQueryService;
import com.njumarket.trade.client.AuthClient;
import com.njumarket.trade.client.ImageClient;
import com.njumarket.trade.service.InventoryService;
import com.njumarket.trade.service.OrderAdminService;
import com.njumarket.njumarket.dto.internal.AddressInternalDTO;
import com.njumarket.njumarket.utils.SecurityUtils;
import com.njumarket.njumarket.utils.CacheUtil;
import com.njumarket.njumarket.utils.RedisConstants;
import com.njumarket.trade.utils.CommodityValidator;
import com.njumarket.trade.search.CommoditySearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;
import java.util.Map;

/**
 * 商品服务实现类，专注于商品管理（写操作）。查询功能见 CommodityQueryService。
 *
 * ── 商品状态机 ──────────────────────────────────────────────────────────────
 *
 *   状态值         语义                  买家可见  进最新缓存  可下单
 *   DRAFT          草稿，尚未提交         ✗        ✗          ✗
 *   PUBLISHED      已提交但未上架         ✗        ✗          ✗
 *   ON_SHELF       正式上架，公开售卖      ✓        ✓          ✓
 *   OFF_SHELF      已下架                 ✗        ✗          ✗
 *
 * ── 合法转换 ────────────────────────────────────────────────────────────────
 *
 *   入口（创建）
 *     publishCommodity()      → PUBLISHED   （直发，跳过草稿）
 *     createDraftCommodity()  → DRAFT       （先存草稿）
 *     copyCommodity()         → DRAFT       （复制副本）
 *
 *   卖家操作
 *     publishDraftCommodity() DRAFT       → PUBLISHED
 *     shelfCommodity()        PUBLISHED   → ON_SHELF     ← 唯一进最新缓存的入口
 *     unshelfCommodity()      ON_SHELF    → OFF_SHELF
 *     draftCommodity()        *           → DRAFT        （任意状态退回草稿）
 *     republishCommodity()    *           → ON_SHELF     （直接重新上架）
 *
 *   管理端
 *     removeCommodity()       *           → OFF_SHELF    （强制下架）
 *     deleteCommodity()       *           → [删除]       （需无关联订单）
 *
 * ── 每次状态变更的联动 ───────────────────────────────────────────────────────
 *
 *   syncCommoditySearchIndex()   所有状态变更后同步 ES 索引
 *   evictCommodityCache()        所有状态变更后清除 Redis 缓存（详情+列表+热门+最新）
 *   appendCommodityToLatestCaches() 仅 ON_SHELF 时写入最新商品缓存
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CommodityServiceImpl implements CommodityService {

    private final CommodityRepository commodityRepository;
    private final CommodityInternalDTOConverter commodityInternalDTOConverter;
    private final AuthClient authClient;
    private final InventoryService inventoryService;
    private final OrderAdminService orderAdminService;
    private final ImageClient imageClient;
    private final CommodityQueryService commodityQueryService;
    private final ObjectMapper objectMapper;
    private final CacheUtil cacheUtil;
    private final CommoditySearchService commoditySearchService;

    // ✅ 统一使用GMT+8时区（中国大陆时区）
    private static final ZoneId GMT_PLUS_8_ZONE = ZoneId.of("Asia/Shanghai");
    private static final TypeReference<List<CommodityDTO>> COMMODITY_DTO_LIST_TYPE =
            new TypeReference<List<CommodityDTO>>() {};
    
    /**
     * 获取GMT+8时区的当前时间
     * 用于记录变更时统一时区，确保时间片key正确
     */
    private LocalDateTime nowGMT8() {
        return LocalDateTime.now(GMT_PLUS_8_ZONE);
    }

    // ========== 商品管理核心功能 ==========
    
    @Override
    @Transactional
    public Result publishCommodity(CommodityDTO commodityDTO) {
        Object userObj = SecurityUtils.requireCurrentUser();
        User currentUser = (User) userObj;
        
        // 创建商品实体
        Commodity commodity = new Commodity();
        commodity.setCommodityId(generateCommodityId());
        commodity.setSellerId(currentUser.getUserId());
        commodity.setTitle(commodityDTO.getTitle());
        commodity.setDescription(commodityDTO.getDescription());
        commodity.setPrice(commodityDTO.getPrice());
        commodity.setStock(commodityDTO.getStock());
        commodity.setLocation(commodityDTO.getLocation());
        commodity.setCategory(commodityDTO.getCategory());
        commodity.setConditionLevel(commodityDTO.getConditionLevel());
        commodity.setImages(commodityDTO.getImages() != null ? String.join(",", commodityDTO.getImages()) : null);
        commodity.setCommodityStatus("PUBLISHED"); // 默认设为已发布但未上架
        commodity.setBuyerVisibility("PUBLIC");
        commodity.setClickCount(0);
        commodity.setPublishTime(LocalDateTime.now());
        
        // ✅ 创建地址快照 - 获取地址信息（使用Feign Client）
        setCommodityAddress(commodity, commodityDTO, currentUser.getUserId());
        
        // 保存商品（PUBLISHED 状态，未上架，不进入最新商品缓存）
        Commodity savedCommodity = commodityRepository.save(commodity);
        syncCommoditySearchIndex(savedCommodity);
        
        return Result.ok("商品发布成功", convertToDTO(savedCommodity));
    }
    
    @Override
    @Transactional
    public Result createDraftCommodity(CommodityDTO commodityDTO) {
        Object userObj = SecurityUtils.requireCurrentUser();
        User currentUser = (User) userObj;
        
        // 创建商品实体
        Commodity commodity = new Commodity();
        commodity.setCommodityId(generateCommodityId());
        commodity.setSellerId(currentUser.getUserId());
        commodity.setTitle(commodityDTO.getTitle());
        commodity.setDescription(commodityDTO.getDescription());
        commodity.setPrice(commodityDTO.getPrice());
        commodity.setStock(commodityDTO.getStock());
        commodity.setLocation(commodityDTO.getLocation());
        commodity.setCategory(commodityDTO.getCategory());
        commodity.setConditionLevel(commodityDTO.getConditionLevel());
        commodity.setImages(commodityDTO.getImages() != null ? String.join(",", commodityDTO.getImages()) : null);
        commodity.setCommodityStatus("DRAFT"); // 设为草稿状态
        commodity.setBuyerVisibility("PUBLIC");
        commodity.setClickCount(0);
        commodity.setPublishTime(LocalDateTime.now());
        
        // ✅ 创建地址快照 - 获取地址信息（使用Feign Client）
        setCommodityAddress(commodity, commodityDTO, currentUser.getUserId());
        
        // 保存草稿（DRAFT 状态，不进入最新商品缓存）
        Commodity savedCommodity = commodityRepository.save(commodity);
        syncCommoditySearchIndex(savedCommodity);
        
        return Result.ok("草稿商品创建成功", convertToDTO(savedCommodity));
    }
    
    @Override
    @Transactional
    public Result publishDraftCommodity(String commodityId) {
        Object userObj = SecurityUtils.requireCurrentUser();
        User currentUser = (User) userObj;
        Commodity commodity = CommodityValidator.requireCommodity(commodityId, commodityRepository);
        CommodityValidator.requireCommodityOwner(commodity, currentUser.getUserId());
        CommodityValidator.requireCommodityStatus(commodity, "DRAFT");
        
        // 发布草稿为 PUBLISHED（仍未上架，不进入最新商品缓存）
        commodity.setCommodityStatus("PUBLISHED");
        commodity.setPublishTime(LocalDateTime.now());
        commodityRepository.save(commodity);
        syncCommoditySearchIndex(commodity);
        
        return Result.ok("草稿商品发布成功");
    }
    
    @Override
    @Transactional
    public Result updateCommodity(String commodityId, CommodityDTO commodityDTO) {
        Object userObj = SecurityUtils.requireCurrentUser();
        User currentUser = (User) userObj;
        Commodity commodity = CommodityValidator.requireCommodity(commodityId, commodityRepository);
        CommodityValidator.requireCommodityOwner(commodity, currentUser.getUserId());

        boolean wasOnShelf = "ON_SHELF".equals(commodity.getCommodityStatus());
        Integer oldStock = commodity.getStock() != null ? commodity.getStock() : 0;
        int newStockVal = commodityDTO.getStock() != null ? commodityDTO.getStock() : 0;
        boolean stockChanged = wasOnShelf && (oldStock.intValue() != newStockVal);

        // 更新商品信息
        commodity.setTitle(commodityDTO.getTitle());
        commodity.setDescription(commodityDTO.getDescription());
        commodity.setPrice(commodityDTO.getPrice());
        commodity.setStock(commodityDTO.getStock());
        commodity.setLocation(commodityDTO.getLocation());
        commodity.setCategory(commodityDTO.getCategory());
        commodity.setConditionLevel(commodityDTO.getConditionLevel());
        commodity.setImages(commodityDTO.getImages() != null ? String.join(",", commodityDTO.getImages()) : null);

        // ✅ 更新地址快照 - 获取地址信息（使用Feign Client）
        setCommodityAddress(commodity, commodityDTO, currentUser.getUserId());

        // 保存更新
        Commodity updatedCommodity = commodityRepository.save(commodity);
        syncCommoditySearchIndex(updatedCommodity);

        // ON_SHELF 且库存变更：同步到订单服务；失败则回滚商品表并抛错（补偿保证一致性）
        if (stockChanged) {
            try {
                Result adjustResult = inventoryService.adjustInventory(commodityId, newStockVal);
                if (adjustResult == null || !Boolean.TRUE.equals(adjustResult.getSuccess())) {
                    updatedCommodity.setStock(oldStock);
                    commodityRepository.save(updatedCommodity);
                    throw new BusinessException(adjustResult != null && adjustResult.getErrorMsg() != null
                            ? adjustResult.getErrorMsg() : "库存同步失败，请重试");
                }
            } catch (BusinessException e) {
                throw e;
            } catch (Exception e) {
                log.warn("库存调整 Feign 失败，回滚商品库存: commodityId={}, error={}", commodityId, e.getMessage());
                updatedCommodity.setStock(oldStock);
                commodityRepository.save(updatedCommodity);
                throw new BusinessException("库存同步失败，请重试", e);
            }
        }

        // ✅ 清除商品相关缓存（最终一致性：写入时删除缓存）
        evictCommodityCache(commodityId, false);

        return Result.ok("商品更新成功", convertToDTO(updatedCommodity));
    }
    
    @Override
    @Transactional
    public Result deleteCommodity(String commodityId) {
        Object userObj = SecurityUtils.requireCurrentUser();
        User currentUser = (User) userObj;
        Commodity commodity = CommodityValidator.requireCommodity(commodityId, commodityRepository);
        CommodityValidator.requireCommodityOwner(commodity, currentUser.getUserId());
        
        orderAdminService.checkCommodityHasOrders(commodityId);
        
        // 删除商品
        commodityRepository.delete(commodity);
        removeCommodityFromSearchIndex(commodityId);

        // 删除时归零库存（防止孤悬库存记录被误用）
        zeroInventoryInOrderService(commodityId);

        // ✅ 清除商品相关缓存
        evictCommodityCache(commodityId);
        
        return Result.ok("商品删除成功");
    }
    
    // ========== 商品状态管理 ==========

    /**
     * 上架成功后，将库存同步到订单服务（非阻塞：失败只记录警告，不回滚上架）。
     */
    private void syncInventoryToOrderService(Commodity commodity) {
        try {
            int stock = commodity.getStock() != null ? commodity.getStock() : 0;
            inventoryService.syncInventory(commodity.getCommodityId(), stock, stock);
        } catch (Exception e) {
            log.warn("库存同步到订单服务失败（不影响上架）: commodityId={}, error={}",
                    commodity.getCommodityId(), e.getMessage());
        }
    }

    /**
     * 下架/草稿时，将订单服务中的库存归零，禁止后续下单（非阻塞）。
     */
    private void zeroInventoryInOrderService(String commodityId) {
        try {
            inventoryService.zeroInventory(commodityId);
        } catch (Exception e) {
            log.warn("库存归零失败（不影响操作）: commodityId={}, error={}", commodityId, e.getMessage());
        }
    }

    @Override
    @Transactional
    public Result shelfCommodity(String commodityId) {
        Object userObj = SecurityUtils.requireCurrentUser();
        User currentUser = (User) userObj;
        Commodity commodity = CommodityValidator.requireCommodity(commodityId, commodityRepository);
        CommodityValidator.requireCommodityOwner(commodity, currentUser.getUserId());
        CommodityValidator.requireCommodityStatus(commodity, "PUBLISHED");
        
        commodity.setCommodityStatus("ON_SHELF");
        LocalDateTime now = nowGMT8();
        commodity.setPublishTime(now);
        Commodity savedCommodity = commodityRepository.save(commodity);
        syncCommoditySearchIndex(savedCommodity);
        
        // 上架时将库存同步到订单服务
        syncInventoryToOrderService(savedCommodity);

        // ✅ 清除商品相关缓存
        evictCommodityCache(commodityId);
        appendCommodityToLatestCaches(savedCommodity);
        
        return Result.ok("商品上架成功");
    }
    
    @Override
    @Transactional
    public Result unshelfCommodity(String commodityId) {
        Object userObj = SecurityUtils.requireCurrentUser();
        User currentUser = (User) userObj;
        Commodity commodity = CommodityValidator.requireCommodity(commodityId, commodityRepository);
        CommodityValidator.requireCommodityOwner(commodity, currentUser.getUserId());
        
        commodity.setCommodityStatus("OFF_SHELF");
        Commodity savedCommodity = commodityRepository.save(commodity);
        syncCommoditySearchIndex(savedCommodity);

        // 下架时归零库存，禁止下单
        zeroInventoryInOrderService(commodityId);

        // ✅ 清除商品相关缓存
        evictCommodityCache(commodityId);
        
        return Result.ok("商品下架成功");
    }
    
    @Override
    @Transactional
    public Result draftCommodity(String commodityId) {
        Object userObj = SecurityUtils.requireCurrentUser();
        User currentUser = (User) userObj;
        Commodity commodity = CommodityValidator.requireCommodity(commodityId, commodityRepository);
        CommodityValidator.requireCommodityOwner(commodity, currentUser.getUserId());
        
        commodity.setCommodityStatus("DRAFT");
        commodityRepository.save(commodity);
        syncCommoditySearchIndex(commodity);

        // 退草稿时归零库存
        zeroInventoryInOrderService(commodityId);

        // 清除商品相关缓存（商品状态变更后需要清除缓存）
        evictCommodityCache(commodityId);
        
        return Result.ok("商品设为草稿成功");
    }
    
    @Override
    @Transactional
    public Result republishCommodity(String commodityId) {
        Object userObj = SecurityUtils.requireCurrentUser();
        User currentUser = (User) userObj;
        Commodity commodity = CommodityValidator.requireCommodity(commodityId, commodityRepository);
        CommodityValidator.requireCommodityOwner(commodity, currentUser.getUserId());
        
        commodity.setCommodityStatus("ON_SHELF");
        commodity.setPublishTime(nowGMT8());
        Commodity savedCommodity = commodityRepository.save(commodity);
        syncCommoditySearchIndex(savedCommodity);
        evictCommodityCache(commodityId);
        appendCommodityToLatestCaches(savedCommodity);

        // 重新上架时同步库存
        syncInventoryToOrderService(savedCommodity);
        
        return Result.ok("商品重新上架成功");
    }
    
    // ========== 商品可见性管理 ==========
    
    @Override
    @Transactional
    public Result updateCommodityVisibility(String commodityId, String visibility) {
        Object userObj = SecurityUtils.requireCurrentUser();
        User currentUser = (User) userObj;
        Commodity commodity = CommodityValidator.requireCommodity(commodityId, commodityRepository);
        CommodityValidator.requireCommodityOwner(commodity, currentUser.getUserId());

        if (!"PUBLIC".equals(visibility) && !"HIDDEN".equals(visibility)) {
            throw new BusinessException("无效的可见性值，只允许 PUBLIC 或 HIDDEN");
        }
        commodity.setBuyerVisibility(visibility);
        commodityRepository.save(commodity);
        syncCommoditySearchIndex(commodity);
        evictCommodityCache(commodityId);

        return Result.ok("商品可见性修改成功");
    }
    
    // ========== 图片管理 ==========
    
    @Override
    public Result uploadImage(MultipartFile file) {
        Object userObj = SecurityUtils.requireCurrentUser();
        User currentUser = (User) userObj;
        
        try {
            Result uploadResult = imageClient.uploadCommodityImage(currentUser.getUserId(), file);
            if (!uploadResult.getSuccess() || uploadResult.getData() == null) {
                throw new BusinessException(uploadResult.getErrorMsg() != null ? uploadResult.getErrorMsg() : "图片上传失败");
            }
            
            // ⚠️ 注意：不直接引用 ImageUploadDTO，避免跨服务依赖
            // 从 Result.getData() 中提取数据（Feign Client 返回的是 LinkedHashMap）
            Map<String, Object> uploadResponse;
            try {
                @SuppressWarnings("unchecked")
                Map<String, Object> dataMap = (Map<String, Object>) uploadResult.getData();
                uploadResponse = new HashMap<>(dataMap);
            } catch (ClassCastException e) {
                log.error("解析图片上传响应失败: error={}", e.getMessage(), e);
                throw new BusinessException("图片上传信息解析失败");
            }
            return Result.ok("图片上传成功", uploadResponse);
        } catch (Exception e) {
            throw new BusinessException("图片上传失败: " + e.getMessage());
        }
    }
    
    @Override
    public Result uploadCommodityImage(String commodityId, MultipartFile file) {
        Object userObj = SecurityUtils.requireCurrentUser();
        User currentUser = (User) userObj;
        Commodity commodity = CommodityValidator.requireCommodity(commodityId, commodityRepository);
        CommodityValidator.requireCommodityOwner(commodity, currentUser.getUserId());
        
        try {
            Result uploadResult = imageClient.uploadCommodityImageForCommodity(commodityId, file);
            if (!uploadResult.getSuccess() || uploadResult.getData() == null) {
                throw new BusinessException(uploadResult.getErrorMsg() != null ? uploadResult.getErrorMsg() : "图片上传失败");
            }
            
            // ⚠️ 注意：不直接引用 ImageUploadDTO，避免跨服务依赖
            // 从 Result.getData() 中提取数据（Feign Client 返回的是 LinkedHashMap）
            Map<String, Object> uploadResponse;
            try {
                @SuppressWarnings("unchecked")
                Map<String, Object> dataMap = (Map<String, Object>) uploadResult.getData();
                uploadResponse = new HashMap<>(dataMap);
            } catch (ClassCastException e) {
                log.error("解析图片上传响应失败: error={}", e.getMessage(), e);
                throw new BusinessException("图片上传信息解析失败");
            }
            return Result.ok("图片上传成功", uploadResponse);
        } catch (Exception e) {
            throw new BusinessException("图片上传失败: " + e.getMessage());
        }
    }
    
    // ========== 批量操作 ==========
    
    @Override
    @Transactional
    public Result batchOperation(String[] commodityIds, String operation) {
        SecurityUtils.requireCurrentUser(); // 验证登录，但不使用返回值
        
        int successCount = 0;
        int failCount = 0;
        
        for (String commodityId : commodityIds) {
            try {
                switch (operation) {
                    case "shelf":
                        shelfCommodity(commodityId);
                        break;
                    case "unshelf":
                        unshelfCommodity(commodityId);
                        break;
                    case "draft":
                        draftCommodity(commodityId);
                        break;
                    case "delete":
                        deleteCommodity(commodityId);
                        break;
                    default:
                        log.warn("未知的批量操作: {}", operation);
                        failCount++;
                        continue;
                }
                successCount++;
            } catch (BusinessException e) {
                log.warn("批量操作商品失败 - commodityId: {}, operation: {}, error: {}", 
                    commodityId, operation, e.getMessage());
                failCount++;
            }
        }
        
        BatchOperationResultVO result = new BatchOperationResultVO();
        result.setSuccessCount(successCount);
        result.setFailCount(failCount);
        result.setTotal(commodityIds.length);
        
        return Result.ok(String.format("批量操作完成，成功%d个，失败%d个", successCount, failCount), result);
    }
    
    @Override
    @Transactional
    public Result copyCommodity(String commodityId) {
        Object userObj = SecurityUtils.requireCurrentUser();
        User currentUser = (User) userObj;
        Commodity originalCommodity = CommodityValidator.requireCommodity(commodityId, commodityRepository);
        CommodityValidator.requireCommodityOwner(originalCommodity, currentUser.getUserId());
        
        // 创建新商品
        Commodity newCommodity = new Commodity();
        newCommodity.setCommodityId(generateCommodityId());
        newCommodity.setSellerId(currentUser.getUserId());
        newCommodity.setTitle(originalCommodity.getTitle() + " (副本)");
        newCommodity.setDescription(originalCommodity.getDescription());
        newCommodity.setPrice(originalCommodity.getPrice());
        newCommodity.setStock(originalCommodity.getStock());
        newCommodity.setLocation(originalCommodity.getLocation());
        newCommodity.setCategory(originalCommodity.getCategory());
        newCommodity.setConditionLevel(originalCommodity.getConditionLevel());
        newCommodity.setImages(originalCommodity.getImages());
        newCommodity.setCommodityStatus("DRAFT");
        newCommodity.setBuyerVisibility("PUBLIC");
        newCommodity.setClickCount(0);
        newCommodity.setPublishTime(LocalDateTime.now());
        
        Commodity savedCommodity = commodityRepository.save(newCommodity);
        
        return Result.ok("商品复制成功", convertToDTO(savedCommodity));
    }
    
    // ========== 统计和分析 ==========
    
    @Override
    public Result getSalesStatistics(String period) {
        Object userObj = SecurityUtils.requireCurrentUser();
        User currentUser = (User) userObj;
        
        // 使用CommodityQueryService获取统计信息
        return commodityQueryService.getCommodityStats(currentUser);
    }
    
    // ========== 用户商品管理 ==========
    
    @Override
    public Result getMyCommodities(Integer page, Integer size, String status) {
        Object userObj = SecurityUtils.requireCurrentUser();
        User currentUser = (User) userObj;
        
        // 统一使用 getUserCommodities 方法，sellerId 为 null 表示查询自己的商品
        return commodityQueryService.getUserCommodities(currentUser, null, status, page, size);
    }
    
    @Override
    public Result getMyCommodityDetail(String commodityId) {
        Object userObj = SecurityUtils.requireCurrentUser();
        User currentUser = (User) userObj;
        Commodity commodity = CommodityValidator.requireCommodity(commodityId, commodityRepository);
        CommodityValidator.requireCommodityOwner(commodity, currentUser.getUserId());
        
        return Result.ok("获取商品详情成功", convertToDTO(commodity));
    }
    
    // ========== 管理端功能 ==========
    
    @Override
    public Result getCommodityList(Integer page, Integer size, String status) {
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "publishTime"));
        Page<Commodity> commodityPage;
        
        if (StringUtils.hasText(status)) {
            commodityPage = commodityRepository.findByCommodityStatus(status, pageable);
        } else {
            commodityPage = commodityRepository.findAll(pageable);
        }
        
        List<CommodityDTO> commodityDTOs = commodityPage.getContent().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
        
        CommodityPageResultVO result = new CommodityPageResultVO();
        result.setCommodities(commodityDTOs);
        result.setTotal(commodityPage.getTotalElements());
        result.setPages(commodityPage.getTotalPages());
        result.setCurrent(page);
        result.setSize(size);
        
        return Result.ok("获取商品列表成功", result);
    }
    
    @Override
    @Transactional
    public Result removeCommodity(String commodityId, String reason) {
        Commodity commodity = CommodityValidator.requireCommodity(commodityId, commodityRepository);
        
        commodity.setCommodityStatus("OFF_SHELF");
        commodityRepository.save(commodity);
        syncCommoditySearchIndex(commodity);
        evictCommodityCache(commodityId);
        
        return Result.ok("商品强制下架成功");
    }
    
    // ========== 私有辅助方法 ==========
    
    /**
     * 清除商品相关缓存（最终一致性：写入时删除缓存）
     * 当商品更新、删除、上架、下架等操作时调用
     * 
     * @param commodityId 商品ID
     * @param evictListCache 是否删除列表缓存（热门商品、最新商品、商品列表）
     *                       true: 删除所有缓存（用于状态、可见性变更等影响列表的操作）
     *                       false: 只删除商品详情缓存（用于价格、描述等详情变更）
     */
    private void evictCommodityCache(String commodityId, boolean evictListCache) {
        try {
            // 清除商品详情缓存和基础数据缓存（所有操作都需要删除）
            cacheUtil.delete(RedisConstants.CACHE_COMMODITY_DETAIL_KEY + commodityId);
            cacheUtil.delete(RedisConstants.CACHE_COMMODITY_KEY + commodityId);
            
            // 只有在影响列表的操作时才删除列表缓存
            if (evictListCache) {
            // 根据缓存内容精准清除热门/最新缓存
            evictHotCachesContainingCommodity(commodityId);
            evictLatestCachesContainingCommodity(commodityId);
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
    
    /**
     * 清除商品相关缓存（兼容旧方法，默认删除所有缓存）
     * 
     * @param commodityId 商品ID
     */
    private void evictCommodityCache(String commodityId) {
        evictCommodityCache(commodityId, true);
    }

    private void syncCommoditySearchIndex(Commodity commodity) {
        try {
            commoditySearchService.syncCommodity(commodity);
        } catch (Exception e) {
            log.warn("同步商品搜索索引失败: commodityId={}, error={}", commodity.getCommodityId(), e.getMessage());
        }
    }

    private void removeCommodityFromSearchIndex(String commodityId) {
        try {
            commoditySearchService.removeCommodity(commodityId);
        } catch (Exception e) {
            log.warn("从搜索索引删除商品失败: commodityId={}, error={}", commodityId, e.getMessage());
        }
    }
    
    private void evictHotCachesContainingCommodity(String commodityId) {
        try {
            Set<String> cacheKeys = cacheUtil.getCacheKeySet(RedisConstants.CACHE_COMMODITY_HOT_KEY_REGISTRY);
            if (cacheKeys.isEmpty()) {
                return;
            }
            for (String cacheKey : cacheKeys) {
                List<CommodityDTO> cachedList = cacheUtil.get(cacheKey, COMMODITY_DTO_LIST_TYPE);
                if (cachedList == null) {
                    cacheUtil.removeCacheKeyFromSet(RedisConstants.CACHE_COMMODITY_HOT_KEY_REGISTRY, cacheKey);
                    continue;
                }
                boolean contains = cachedList.stream()
                        .filter(Objects::nonNull)
                        .anyMatch(dto -> commodityId.equals(dto.getCommodityId()));
                if (contains) {
                    cacheUtil.delete(cacheKey);
                    cacheUtil.removeCacheKeyFromSet(RedisConstants.CACHE_COMMODITY_HOT_KEY_REGISTRY, cacheKey);
                }
            }
        } catch (Exception e) {
            log.warn("清除热门商品缓存失败: commodityId={}, error={}", commodityId, e.getMessage());
        }
    }
    
    private void evictLatestCachesContainingCommodity(String commodityId) {
        try {
            Set<String> cacheKeys = cacheUtil.getCacheKeySet(RedisConstants.CACHE_COMMODITY_LATEST_KEY_REGISTRY);
            if (cacheKeys.isEmpty()) {
                return;
            }
            for (String cacheKey : cacheKeys) {
                List<CommodityDTO> cachedList = cacheUtil.get(cacheKey, COMMODITY_DTO_LIST_TYPE);
                if (cachedList == null) {
                    cacheUtil.removeCacheKeyFromSet(RedisConstants.CACHE_COMMODITY_LATEST_KEY_REGISTRY, cacheKey);
                    continue;
                }
                boolean contains = cachedList.stream()
                        .filter(Objects::nonNull)
                        .anyMatch(dto -> commodityId.equals(dto.getCommodityId()));
                if (contains) {
                    cacheUtil.delete(cacheKey);
                    cacheUtil.removeCacheKeyFromSet(RedisConstants.CACHE_COMMODITY_LATEST_KEY_REGISTRY, cacheKey);
                }
            }
        } catch (Exception e) {
            log.warn("清除最新商品缓存失败: commodityId={}, error={}", commodityId, e.getMessage());
        }
    }
    
    private void appendCommodityToLatestCaches(Commodity commodity) {
        if (commodity == null || !shouldAppearInLatestCaches(commodity)) {
            return;
        }
        try {
            Set<String> cacheKeys = cacheUtil.getCacheKeySet(RedisConstants.CACHE_COMMODITY_LATEST_KEY_REGISTRY);
            if (cacheKeys.isEmpty()) {
                return;
            }
            CommodityDTO dto = convertToDTO(commodity);
            for (String cacheKey : cacheKeys) {
                List<CommodityDTO> cachedList = cacheUtil.get(cacheKey, COMMODITY_DTO_LIST_TYPE);
                if (cachedList == null) {
                    cacheUtil.removeCacheKeyFromSet(RedisConstants.CACHE_COMMODITY_LATEST_KEY_REGISTRY, cacheKey);
                    continue;
                }
                int limit = parseLimitFromLatestCacheKey(cacheKey);
                if (limit <= 0) {
                    continue;
                }
                LinkedList<CommodityDTO> updated = new LinkedList<>(cachedList);
                updated.removeIf(item -> item != null && commodity.getCommodityId().equals(item.getCommodityId()));
                updated.addFirst(dto);
                while (updated.size() > limit) {
                    updated.removeLast();
                }
                cacheUtil.set(cacheKey, updated, RedisConstants.CACHE_COMMODITY_LATEST_TTL * 60);
            }
        } catch (Exception e) {
            log.warn("更新最新商品缓存失败: commodityId={}, error={}", commodity.getCommodityId(), e.getMessage());
        }
    }
    
    private boolean shouldAppearInLatestCaches(Commodity commodity) {
        if (commodity == null || !StringUtils.hasText(commodity.getCommodityStatus())) {
            return false;
        }
        String status = commodity.getCommodityStatus();
        return "ON_SHELF".equalsIgnoreCase(status);
    }
    
    private int parseLimitFromLatestCacheKey(String cacheKey) {
        if (!StringUtils.hasText(cacheKey)) {
            return -1;
        }
        int lastColon = cacheKey.lastIndexOf(':');
        if (lastColon < 0 || lastColon == cacheKey.length() - 1) {
            return -1;
        }
        try {
            return Integer.parseInt(cacheKey.substring(lastColon + 1));
        } catch (NumberFormatException e) {
            log.warn("最新商品缓存key无法解析limit: cacheKey={}", cacheKey);
            return -1;
        }
    }
    
    /**
     * 生成商品ID
     */
    private String generateCommodityId() {
        return "COMMODITY_" + System.currentTimeMillis() + "_" + new Random().nextInt(1000);
    }
    
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
     * 设置商品地址信息
     * 
     * 重要设计原则：
     * 1. 地址快照字段（address_snapshot_*）是完全独立的，不依赖 address_id
     * 2. 如果 DTO 中已经提供了快照字段，优先使用这些字段（快照字段已独立设置）
     * 3. 只有在快照字段为空时，才从 address_id 获取地址信息并填充到快照字段
     * 4. address_id 只是用于"填充"快照字段的数据来源，一旦快照字段设置完成，就完全独立
     * 5. 快照字段一旦设置，就完全独立，即使 address_id 对应的地址被删除或修改，快照也不受影响
     * 
     * @param commodity 商品实体
     * @param commodityDTO 商品DTO（可能包含addressId或快照字段）
     * @param userId 用户ID
     */
    private void setCommodityAddress(Commodity commodity, CommodityDTO commodityDTO, String userId) {
        try {
            // ✅ 优先检查：如果 DTO 中已经提供了快照字段，直接使用（快照字段已独立设置）
            boolean hasSnapshotFields = StringUtils.hasText(commodityDTO.getAddressSnapshotProvince()) ||
                                       StringUtils.hasText(commodityDTO.getAddressSnapshotCity()) ||
                                       StringUtils.hasText(commodityDTO.getAddressSnapshotFull());
            
            if (hasSnapshotFields) {
                // 快照字段已独立提供，直接使用
                commodity.setAddressSnapshotProvince(commodityDTO.getAddressSnapshotProvince());
                commodity.setAddressSnapshotCity(commodityDTO.getAddressSnapshotCity());
                commodity.setAddressSnapshotDistrict(commodityDTO.getAddressSnapshotDistrict());
                commodity.setAddressSnapshotStreet(commodityDTO.getAddressSnapshotStreet());
                commodity.setAddressSnapshotDetail(commodityDTO.getAddressSnapshotDetail());
                commodity.setAddressSnapshotFull(commodityDTO.getAddressSnapshotFull());
                
                // 如果提供了经纬度，也直接使用
                if (commodityDTO.getLongitude() != null && commodityDTO.getLatitude() != null) {
                    commodity.setLongitude(commodityDTO.getLongitude());
                    commodity.setLatitude(commodityDTO.getLatitude());
                    commodity.setLocationGeography(
                        String.format("POINT(%s %s)", commodityDTO.getLongitude(), commodityDTO.getLatitude())
                    );
                }
                
                // address_id 只是作为引用保存（如果提供了）
                if (StringUtils.hasText(commodityDTO.getAddressId())) {
                    commodity.setAddressId(commodityDTO.getAddressId());
                }
                
                return; // 快照字段已独立设置，不需要从 address_id 获取
            }
            
            // ✅ 只有在快照字段为空时，才从 address_id 获取地址信息并填充到快照字段
            AddressInternalDTO addressDTO = null;
            
            // 如果传了地址ID，则根据ID获取地址
            if (StringUtils.hasText(commodityDTO.getAddressId())) {
                Result addressResult = authClient.getAddressById(commodityDTO.getAddressId());
                if (addressResult.getSuccess() && addressResult.getData() != null) {
                    addressDTO = objectMapper.convertValue(
                        addressResult.getData(),
                        new TypeReference<AddressInternalDTO>() {}
                    );
                    commodity.setAddressId(commodityDTO.getAddressId());
                }
            }
            
            // 如果没有传地址ID或获取失败，则使用默认地址
            if (addressDTO == null) {
                Result defaultAddressResult = authClient.getDefaultAddress(userId);
                if (defaultAddressResult.getSuccess() && defaultAddressResult.getData() != null) {
                    addressDTO = objectMapper.convertValue(
                        defaultAddressResult.getData(),
                        new TypeReference<AddressInternalDTO>() {}
                    );
                    if (addressDTO != null && StringUtils.hasText(addressDTO.getAddressId())) {
                        commodity.setAddressId(addressDTO.getAddressId());
                    }
                }
            }
            
            // 如果获取到地址信息，写入地址快照和地理位置
            if (addressDTO != null) {
                // 写入地址快照（从 address_id 填充）
                commodity.setAddressSnapshotProvince(addressDTO.getProvince());
                commodity.setAddressSnapshotCity(addressDTO.getCity());
                commodity.setAddressSnapshotDistrict(addressDTO.getDistrict());
                commodity.setAddressSnapshotStreet(addressDTO.getStreetAddress());
                commodity.setAddressSnapshotDetail(addressDTO.getDetailAddress());
                commodity.setAddressSnapshotFull(addressDTO.getFullAddress());
                
                // 写入地理位置（用于地理搜索和距离计算）
                if (addressDTO.getLongitude() != null && addressDTO.getLatitude() != null) {
                    commodity.setLongitude(addressDTO.getLongitude());
                    commodity.setLatitude(addressDTO.getLatitude());
                    // 构建 PostGIS WKT 格式：POINT(longitude latitude)
                    commodity.setLocationGeography(
                        String.format("POINT(%s %s)", addressDTO.getLongitude(), addressDTO.getLatitude())
                    );
                }
            } else {
                // 如果无法获取地址信息，使用原有location字段作为快照
                log.warn("创建/更新商品警告 - userId={}, 无法获取地址信息，使用原有location字段", userId);
                if (StringUtils.hasText(commodityDTO.getLocation())) {
                    commodity.setAddressSnapshotFull(commodityDTO.getLocation());
                }
            }
        } catch (Exception e) {
            log.error("获取地址信息失败: userId={}, error={}", userId, e.getMessage(), e);
            log.warn("创建/更新商品警告 - userId={}, 地址信息获取失败，使用原有location字段", userId);
            // 地址获取失败不影响商品创建/更新，使用原有字段
            if (StringUtils.hasText(commodityDTO.getLocation())) {
                commodity.setAddressSnapshotFull(commodityDTO.getLocation());
            }
        }
    }

    // ========== 内部接口实现 ==========

    private static final Set<String> ALLOWED_CONDITION_LEVELS = new HashSet<>(
        Arrays.asList("全新", "九成新", "八成新", "七成新", "六成新", "五成新"));
    private static final Set<String> ALLOWED_STATUSES = new HashSet<>(
        Arrays.asList("DRAFT", "PUBLISHED", "ON_SHELF", "OFF_SHELF"));
    private static final Set<String> ALLOWED_CATEGORIES = new HashSet<>(
        Arrays.asList("电子产品", "服装配饰", "图书文具", "生活用品", "运动户外", "美妆护肤", "其他"));

    @Override
    public CommodityInternalDTO updateCommodityFullInternal(String commodityId, Map<String, Object> payload) {
        Commodity c = commodityRepository.findById(commodityId)
            .orElseThrow(() -> new BusinessException("商品不存在"));

        Object title = payload.get("title");
        if (title instanceof String) c.setTitle(((String) title).trim());

        Object description = payload.get("description");
        if (description instanceof String) c.setDescription(((String) description).trim());

        Object price = payload.get("price");
        if (price != null) {
            try {
                double v = price instanceof Number
                    ? ((Number) price).doubleValue()
                    : Double.parseDouble(price.toString().trim());
                if (v >= 0) c.setPrice(v);
            } catch (NumberFormatException ignored) {}
        }

        Object stock = payload.get("stock");
        if (stock != null) {
            try {
                int v = stock instanceof Number
                    ? ((Number) stock).intValue()
                    : Integer.parseInt(stock.toString().trim());
                if (v >= 0) c.setStock(v);
            } catch (NumberFormatException ignored) {}
        }

        Object location = payload.get("location");
        if (location instanceof String) c.setLocation(((String) location).trim());

        Object category = payload.get("category");
        if (category instanceof String) {
            String cat = ((String) category).trim();
            if (!ALLOWED_CATEGORIES.contains(cat)) throw new BusinessException("非法的商品分类");
            c.setCategory(cat);
        }

        Object conditionLevel = payload.get("conditionLevel");
        if (conditionLevel instanceof String) {
            String lvl = ((String) conditionLevel).trim();
            if (!ALLOWED_CONDITION_LEVELS.contains(lvl)) throw new BusinessException("非法的成色等级");
            c.setConditionLevel(lvl);
        }

        Object commodityStatus = payload.get("commodityStatus");
        if (commodityStatus instanceof String) {
            String st = ((String) commodityStatus).trim();
            if (!ALLOWED_STATUSES.contains(st)) throw new BusinessException("非法的商品状态");
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

        Object clickCount = payload.get("clickCount");
        if (clickCount != null) {
            try {
                int v = clickCount instanceof Number
                    ? ((Number) clickCount).intValue()
                    : Integer.parseInt(clickCount.toString().trim());
                if (v >= 0) c.setClickCount(v);
            } catch (NumberFormatException ignored) {}
        }

        Object buyerVisibility = payload.get("buyerVisibility");
        if (buyerVisibility instanceof String) {
            String vis = ((String) buyerVisibility).trim();
            if (!"PUBLIC".equals(vis) && !"HIDDEN".equals(vis)) {
                throw new BusinessException("非法的可见性值，只允许 PUBLIC 或 HIDDEN");
            }
            c.setBuyerVisibility(vis);
        }

        commodityRepository.save(c);
        commoditySearchService.syncCommodity(c);

        // Cache Aside：先更新数据库，再删缓存（复用统一的缓存清理逻辑）
        boolean evictListCache = payload.containsKey("commodityStatus")
            || payload.containsKey("buyerVisibility")
            || payload.containsKey("clickCount");
        evictCommodityCache(commodityId, evictListCache);

        return commodityInternalDTOConverter.toInternalDTO(c);
    }

    @Override
    public void deleteCommodityInternal(String commodityId) {
        Commodity commodity = commodityRepository.findById(commodityId)
            .orElseThrow(() -> new BusinessException("商品不存在"));
        commodityRepository.delete(commodity);
        commoditySearchService.removeCommodity(commodityId);
        evictCommodityCache(commodityId, true);
    }

    @Override
    public void syncCommoditySearchInternal(String commodityId) {
        Commodity commodity = commodityRepository.findById(commodityId)
            .orElseThrow(() -> new BusinessException("商品不存在"));
        commoditySearchService.syncCommodity(commodity);
    }

    @Override
    public Result getCommodityForUpdate(String commodityId) {
        return commodityRepository.findByIdForUpdate(commodityId)
                .map(commodityInternalDTOConverter::toInternalDTO)
                .map(dto -> Result.ok("查询成功", dto))
                .orElse(Result.fail("商品不存在或无法锁定"));
    }

    @Override
    public Result updateCommodityStock(String commodityId, Integer quantity) {
        if (quantity == null || quantity < 0) {
            return Result.fail("库存数量无效");
        }
        return inventoryService.adjustInventory(commodityId, quantity.intValue());
    }
}

