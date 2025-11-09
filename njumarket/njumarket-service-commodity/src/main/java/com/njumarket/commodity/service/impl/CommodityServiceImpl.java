package com.njumarket.commodity.service.impl;

import com.njumarket.njumarket.dto.Result;
import com.njumarket.commodity.dto.CommodityDTO;
import com.njumarket.commodity.vo.CommodityPageResultVO;
import com.njumarket.commodity.vo.CommodityDetailVO;
import com.njumarket.njumarket.vo.BatchOperationResultVO;
import com.njumarket.commodity.entity.Commodity;
import com.njumarket.commodity.entity.User; // User 实体（Commodity Service专用）
import com.njumarket.njumarket.exception.BusinessException;
import com.njumarket.commodity.repository.CommodityRepository;
import com.njumarket.commodity.service.CommodityService;
import com.njumarket.commodity.service.CommodityQueryService;
import com.njumarket.commodity.client.ImageClient;
import com.njumarket.commodity.client.MessageClient;
import com.njumarket.commodity.client.NotificationClient;
import com.njumarket.commodity.client.OrderClient;
import com.njumarket.njumarket.utils.BusinessValidator;
import com.njumarket.njumarket.utils.SecurityUtils;
import com.njumarket.commodity.utils.CommodityValidator;
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
import java.util.*;
import java.util.stream.Collectors;

/**
 * 商品服务实现类
 * 重构后专注于商品管理功能，查询功能已迁移到CommodityQueryService
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CommodityServiceImpl implements CommodityService {

    private final CommodityRepository commodityRepository;
    private final OrderClient orderClient;
    private final ImageClient imageClient;
    private final MessageClient messageClient;
    private final CommodityQueryService commodityQueryService;
    private final NotificationClient notificationClient;
    private final ObjectMapper objectMapper;

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
        commodity.setSellerVisibility("PUBLIC");
        commodity.setBuyerVisibility("PUBLIC");
        commodity.setClickCount(0);
        commodity.setPublishTime(LocalDateTime.now());
        
        // 保存商品
        Commodity savedCommodity = commodityRepository.save(commodity);
        
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
        commodity.setSellerVisibility("PUBLIC");
        commodity.setBuyerVisibility("PUBLIC");
        commodity.setClickCount(0);
        commodity.setPublishTime(LocalDateTime.now());
        
        // 保存商品
        Commodity savedCommodity = commodityRepository.save(commodity);
        
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
        
        // 发布商品
        commodity.setCommodityStatus("PUBLISHED");
        commodity.setPublishTime(LocalDateTime.now());
        commodityRepository.save(commodity);
        
        return Result.ok("草稿商品发布成功");
    }
    
    @Override
    @Transactional
    public Result updateCommodity(String commodityId, CommodityDTO commodityDTO) {
        Object userObj = SecurityUtils.requireCurrentUser();
        User currentUser = (User) userObj;
        Commodity commodity = CommodityValidator.requireCommodity(commodityId, commodityRepository);
        CommodityValidator.requireCommodityOwner(commodity, currentUser.getUserId());
        
        // 更新商品信息
        commodity.setTitle(commodityDTO.getTitle());
        commodity.setDescription(commodityDTO.getDescription());
        commodity.setPrice(commodityDTO.getPrice());
        commodity.setStock(commodityDTO.getStock());
        commodity.setLocation(commodityDTO.getLocation());
        commodity.setCategory(commodityDTO.getCategory());
        commodity.setConditionLevel(commodityDTO.getConditionLevel());
        commodity.setImages(commodityDTO.getImages() != null ? String.join(",", commodityDTO.getImages()) : null);
        
        // 保存更新
        LocalDateTime now = LocalDateTime.now();
        Commodity updatedCommodity = commodityRepository.save(commodity);
        
        // ✅ 记录商品变更并推送通知（使用推送服务）- 只有已上架的商品变更才记录（避免草稿频繁变更）
        if ("ON_SHELF".equals(updatedCommodity.getCommodityStatus())) {
            try {
                notificationClient.recordCommodityChange(commodityId, "UPDATE", now.toString());
                notificationClient.pushCommodityChange(updatedCommodity.getSellerId(), commodityId, "COMMODITY_UPDATED");
            } catch (Exception e) {
                log.warn("记录商品变更或推送失败（不影响商品更新）: commodityId={}, error={}", commodityId, e.getMessage());
            }
        }
        
        return Result.ok("商品更新成功", convertToDTO(updatedCommodity));
    }
    
    @Override
    @Transactional
    public Result deleteCommodity(String commodityId) {
        Object userObj = SecurityUtils.requireCurrentUser();
        User currentUser = (User) userObj;
        Commodity commodity = CommodityValidator.requireCommodity(commodityId, commodityRepository);
        CommodityValidator.requireCommodityOwner(commodity, currentUser.getUserId());
        
        // 通过Feign Client检查是否有订单
        Result checkResult = orderClient.checkCommodityHasOrders(commodityId);
        if (!checkResult.getSuccess()) {
            throw new BusinessException(checkResult.getErrorMsg());
        }
        
        // 删除商品
        commodityRepository.delete(commodity);
        
        return Result.ok("商品删除成功");
    }
    
    // ========== 商品状态管理 ==========
    
    @Override
    @Transactional
    public Result shelfCommodity(String commodityId) {
        Object userObj = SecurityUtils.requireCurrentUser();
        User currentUser = (User) userObj;
        Commodity commodity = CommodityValidator.requireCommodity(commodityId, commodityRepository);
        CommodityValidator.requireCommodityOwner(commodity, currentUser.getUserId());
        CommodityValidator.requireCommodityStatus(commodity, "PUBLISHED");
        
        commodity.setCommodityStatus("ON_SHELF");
        LocalDateTime now = LocalDateTime.now();
        commodity.setPublishTime(now);
        Commodity savedCommodity = commodityRepository.save(commodity);
        
        // ✅ 记录商品变更并推送通知（使用推送服务）
        try {
            notificationClient.recordCommodityChange(commodityId, "SHELF", now.toString());
            notificationClient.pushCommodityChange(savedCommodity.getSellerId(), commodityId, "COMMODITY_SHELVED");
        } catch (Exception e) {
            log.warn("记录商品变更或推送失败（不影响商品上架）: commodityId={}, error={}", commodityId, e.getMessage());
        }
        
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
        LocalDateTime now = LocalDateTime.now();
        Commodity savedCommodity = commodityRepository.save(commodity);
        
        // ✅ 记录商品变更并推送通知（使用推送服务）- 下架操作影响聊天界面显示
        try {
            notificationClient.recordCommodityChange(commodityId, "UNSHELF", now.toString());
            notificationClient.pushCommodityChange(savedCommodity.getSellerId(), commodityId, "COMMODITY_UNSHELVED");
        } catch (Exception e) {
            log.warn("记录商品变更或推送失败（不影响商品下架）: commodityId={}, error={}", commodityId, e.getMessage());
        }
        
        return Result.ok("商品下架成功");
    }
    
    @Override
    @Transactional
    public Result draftCommodity(String commodityId) {
        Object userObj = SecurityUtils.requireCurrentUser();
        User currentUser = (User) userObj;
        Commodity commodity = CommodityValidator.requireCommodity(commodityId, commodityRepository);
        CommodityValidator.requireCommodityOwner(commodity, currentUser.getUserId());
        
        // ✅ 获取变更前的状态（用于判断是否需要记录变更）
        String previousStatus = commodity.getCommodityStatus();
        
        commodity.setCommodityStatus("DRAFT");
        LocalDateTime now = LocalDateTime.now();
        commodityRepository.save(commodity);
        
        // ✅ 只有当商品之前是已上架状态（ON_SHELF）时才记录变更
        // 原因：只有已上架的商品才会出现在聊天界面中，设为草稿后需要更新卡片状态
        // 如果商品本来就是草稿或其他状态，不会出现在聊天中，无需记录变更
        if ("ON_SHELF".equals(previousStatus)) {
            try {
                notificationClient.recordCommodityChange(commodityId, "DRAFT", now.toString());
            } catch (Exception e) {
                log.warn("记录商品变更失败（不影响商品保存为草稿）: commodityId={}, error={}", commodityId, e.getMessage());
            }
        }
        
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
        LocalDateTime now = LocalDateTime.now();
        commodity.setPublishTime(now);
        Commodity savedCommodity = commodityRepository.save(commodity);
        
        // ✅ 记录商品变更并推送通知（使用推送服务）
        try {
            notificationClient.recordCommodityChange(commodityId, "SHELF", now.toString());
            notificationClient.pushCommodityChange(savedCommodity.getSellerId(), commodityId, "COMMODITY_REPUBLISHED");
        } catch (Exception e) {
            log.warn("记录商品变更或推送失败（不影响商品上架）: commodityId={}, error={}", commodityId, e.getMessage());
        }
        
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
        
        // 验证可见性值
        if (!"PUBLIC".equals(visibility) && !"PRIVATE".equals(visibility) && !"HIDDEN".equals(visibility)) {
            throw new BusinessException("无效的可见性值");
        }
        
        commodity.setSellerVisibility(visibility);
        commodity.setBuyerVisibility(visibility);
        commodityRepository.save(commodity);
        
        return Result.ok("商品可见性修改成功");
    }
    
    @Override
    @Transactional
    public Result updateCommoditySellerVisibility(String commodityId, String sellerVisibility) {
        Object userObj = SecurityUtils.requireCurrentUser();
        User currentUser = (User) userObj;
        Commodity commodity = CommodityValidator.requireCommodity(commodityId, commodityRepository);
        CommodityValidator.requireCommodityOwner(commodity, currentUser.getUserId());
        
        // 验证可见性值
        if (!"PUBLIC".equals(sellerVisibility) && !"PRIVATE".equals(sellerVisibility) && !"HIDDEN".equals(sellerVisibility)) {
            throw new BusinessException("无效的卖家可见性值");
        }
        
        commodity.setSellerVisibility(sellerVisibility);
        commodityRepository.save(commodity);
        
        return Result.ok("商品卖家可见性修改成功");
    }
    
    @Override
    @Transactional
    public Result updateCommodityBuyerVisibility(String commodityId, String buyerVisibility) {
        Object userObj = SecurityUtils.requireCurrentUser();
        User currentUser = (User) userObj;
        Commodity commodity = CommodityValidator.requireCommodity(commodityId, commodityRepository);
        CommodityValidator.requireCommodityOwner(commodity, currentUser.getUserId());
        
        // 验证可见性值
        if (!"PUBLIC".equals(buyerVisibility) && !"PRIVATE".equals(buyerVisibility) && !"HIDDEN".equals(buyerVisibility)) {
            throw new BusinessException("无效的买家可见性值");
        }
        
        commodity.setBuyerVisibility(buyerVisibility);
        commodityRepository.save(commodity);
        
        return Result.ok("商品买家可见性修改成功");
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
        newCommodity.setSellerVisibility("PUBLIC");
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
        LocalDateTime now = LocalDateTime.now();
        commodityRepository.save(commodity);
        
        // ✅ 记录商品变更（用于增量轮询）- 管理端强制下架也需要更新聊天界面
        try {
            notificationClient.recordCommodityChange(commodityId, "UNSHELF", now.toString());
        } catch (Exception e) {
            log.warn("记录商品变更失败（不影响商品下架）: commodityId={}, error={}", commodityId, e.getMessage());
        }
        
        return Result.ok("商品强制下架成功");
    }
    
    // ========== 内部方法（用于微服务间调用） ==========
    
    @Override
    @Transactional
    public Result getCommodityForUpdate(String commodityId) {
        Optional<Commodity> commodityOpt = commodityRepository.findByIdForUpdate(commodityId);
        if (commodityOpt.isEmpty()) {
            throw new BusinessException("商品不存在");
        }
        return Result.ok(commodityOpt.get());
    }
    
    @Override
    @Transactional
    public Result updateCommodityStock(String commodityId, Integer quantity) {
        int updateResult = commodityRepository.updateStockWithCondition(commodityId, quantity);
        if (updateResult == 0) {
            throw new BusinessException("商品库存不足，请刷新后重试");
        }
        return Result.ok("库存更新成功");
    }
    
    // ========== 私有辅助方法 ==========
    
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
}

