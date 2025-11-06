package com.njumarket.njumarket.service.impl;

import com.njumarket.njumarket.dto.Result;
import com.njumarket.njumarket.dto.CommodityDTO;
import com.njumarket.njumarket.dto.ImageUploadDTO;
import com.njumarket.njumarket.entity.Commodity;
import com.njumarket.njumarket.entity.User;
import com.njumarket.njumarket.exception.BusinessException;
import com.njumarket.njumarket.repository.CommodityRepository;
import com.njumarket.njumarket.repository.OrderRepository;
import com.njumarket.njumarket.service.ChangeRecordService;
import com.njumarket.njumarket.service.CommodityService;
import com.njumarket.njumarket.service.CommodityQueryService;
import com.njumarket.njumarket.service.ImageService;
import com.njumarket.njumarket.utils.BusinessValidator;
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
    private final OrderRepository orderRepository;
    private final ImageService imageService;
    private final CommodityQueryService commodityQueryService;
    private final ChangeRecordService changeRecordService;

    // ========== 商品管理核心功能 ==========
    
    @Override
    @Transactional
    public Result publishCommodity(CommodityDTO commodityDTO) {
        User currentUser = BusinessValidator.requireLogin();
        
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
        User currentUser = BusinessValidator.requireLogin();
        
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
        User currentUser = BusinessValidator.requireLogin();
        Commodity commodity = BusinessValidator.requireCommodity(commodityId, commodityRepository);
        BusinessValidator.requireCommodityOwner(commodity, currentUser.getUserId());
        BusinessValidator.requireCommodityStatus(commodity, "DRAFT");
        
        // 发布商品
        commodity.setCommodityStatus("PUBLISHED");
        commodity.setPublishTime(LocalDateTime.now());
        commodityRepository.save(commodity);
        
        return Result.ok("草稿商品发布成功");
    }
    
    @Override
    @Transactional
    public Result updateCommodity(String commodityId, CommodityDTO commodityDTO) {
        User currentUser = BusinessValidator.requireLogin();
        Commodity commodity = BusinessValidator.requireCommodity(commodityId, commodityRepository);
        BusinessValidator.requireCommodityOwner(commodity, currentUser.getUserId());
        
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
        
        // ✅ 记录商品变更（用于增量轮询）- 只有已上架的商品变更才记录（避免草稿频繁变更）
        if ("ON_SHELF".equals(updatedCommodity.getCommodityStatus())) {
            changeRecordService.recordCommodityChange(commodityId, "UPDATE", now);
        }
        
        return Result.ok("商品更新成功", convertToDTO(updatedCommodity));
    }
    
    @Override
    @Transactional
    public Result deleteCommodity(String commodityId) {
        User currentUser = BusinessValidator.requireLogin();
        Commodity commodity = BusinessValidator.requireCommodity(commodityId, commodityRepository);
        BusinessValidator.requireCommodityOwner(commodity, currentUser.getUserId());
        BusinessValidator.requireNoOrders(commodityId, orderRepository);
        
        // 删除商品
        commodityRepository.delete(commodity);
        
        return Result.ok("商品删除成功");
    }
    
    // ========== 商品状态管理 ==========
    
    @Override
    @Transactional
    public Result shelfCommodity(String commodityId) {
        User currentUser = BusinessValidator.requireLogin();
        Commodity commodity = BusinessValidator.requireCommodity(commodityId, commodityRepository);
        BusinessValidator.requireCommodityOwner(commodity, currentUser.getUserId());
        BusinessValidator.requireCommodityStatus(commodity, "PUBLISHED");
        
        commodity.setCommodityStatus("ON_SHELF");
        LocalDateTime now = LocalDateTime.now();
        commodity.setPublishTime(now);
        commodityRepository.save(commodity);
        
        // ✅ 记录商品变更（用于增量轮询）
        changeRecordService.recordCommodityChange(commodityId, "SHELF", now);
        
        return Result.ok("商品上架成功");
    }
    
    @Override
    @Transactional
    public Result unshelfCommodity(String commodityId) {
        User currentUser = BusinessValidator.requireLogin();
        Commodity commodity = BusinessValidator.requireCommodity(commodityId, commodityRepository);
        BusinessValidator.requireCommodityOwner(commodity, currentUser.getUserId());
        
        commodity.setCommodityStatus("OFF_SHELF");
        LocalDateTime now = LocalDateTime.now();
        commodityRepository.save(commodity);
        
        // ✅ 记录商品变更（用于增量轮询）- 下架操作影响聊天界面显示
        changeRecordService.recordCommodityChange(commodityId, "UNSHELF", now);
        
        return Result.ok("商品下架成功");
    }
    
    @Override
    @Transactional
    public Result draftCommodity(String commodityId) {
        User currentUser = BusinessValidator.requireLogin();
        Commodity commodity = BusinessValidator.requireCommodity(commodityId, commodityRepository);
        BusinessValidator.requireCommodityOwner(commodity, currentUser.getUserId());
        
        // ✅ 获取变更前的状态（用于判断是否需要记录变更）
        String previousStatus = commodity.getCommodityStatus();
        
        commodity.setCommodityStatus("DRAFT");
        LocalDateTime now = LocalDateTime.now();
        commodityRepository.save(commodity);
        
        // ✅ 只有当商品之前是已上架状态（ON_SHELF）时才记录变更
        // 原因：只有已上架的商品才会出现在聊天界面中，设为草稿后需要更新卡片状态
        // 如果商品本来就是草稿或其他状态，不会出现在聊天中，无需记录变更
        if ("ON_SHELF".equals(previousStatus)) {
            changeRecordService.recordCommodityChange(commodityId, "DRAFT", now);
        }
        
        return Result.ok("商品设为草稿成功");
    }
    
    @Override
    @Transactional
    public Result republishCommodity(String commodityId) {
        User currentUser = BusinessValidator.requireLogin();
        Commodity commodity = BusinessValidator.requireCommodity(commodityId, commodityRepository);
        BusinessValidator.requireCommodityOwner(commodity, currentUser.getUserId());
        
        commodity.setCommodityStatus("ON_SHELF");
        LocalDateTime now = LocalDateTime.now();
        commodity.setPublishTime(now);
        commodityRepository.save(commodity);
        
        // ✅ 记录商品变更（用于增量轮询）
        changeRecordService.recordCommodityChange(commodityId, "SHELF", now);
        
        return Result.ok("商品重新上架成功");
    }
    
    // ========== 商品可见性管理 ==========
    
    @Override
    @Transactional
    public Result updateCommodityVisibility(String commodityId, String visibility) {
        User currentUser = BusinessValidator.requireLogin();
        Commodity commodity = BusinessValidator.requireCommodity(commodityId, commodityRepository);
        BusinessValidator.requireCommodityOwner(commodity, currentUser.getUserId());
        
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
        User currentUser = BusinessValidator.requireLogin();
        Commodity commodity = BusinessValidator.requireCommodity(commodityId, commodityRepository);
        BusinessValidator.requireCommodityOwner(commodity, currentUser.getUserId());
        
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
        User currentUser = BusinessValidator.requireLogin();
        Commodity commodity = BusinessValidator.requireCommodity(commodityId, commodityRepository);
        BusinessValidator.requireCommodityOwner(commodity, currentUser.getUserId());
        
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
        User currentUser = BusinessValidator.requireLogin();
        
        ImageUploadDTO uploadResult = imageService.uploadCommodityImage(currentUser.getUserId(), file);
        if (!uploadResult.isSuccess()) {
            throw new BusinessException(uploadResult.getMessage());
        }
        
        return Result.ok("图片上传成功", uploadResult);
    }
    
    @Override
    public Result uploadCommodityImage(String commodityId, MultipartFile file) {
        User currentUser = BusinessValidator.requireLogin();
        Commodity commodity = BusinessValidator.requireCommodity(commodityId, commodityRepository);
        BusinessValidator.requireCommodityOwner(commodity, currentUser.getUserId());
        
        ImageUploadDTO uploadResult = imageService.uploadCommodityImageForCommodity(commodityId, file);
        if (!uploadResult.isSuccess()) {
            throw new BusinessException(uploadResult.getMessage());
        }
        
        return Result.ok("图片上传成功", uploadResult);
    }
    
    // ========== 批量操作 ==========
    
    @Override
    @Transactional
    public Result batchOperation(String[] commodityIds, String operation) {
        BusinessValidator.requireLogin(); // 验证登录，但不使用返回值
        
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
        
        Map<String, Object> result = new HashMap<>();
        result.put("successCount", successCount);
        result.put("failCount", failCount);
        result.put("total", commodityIds.length);
        
        return Result.ok(String.format("批量操作完成，成功%d个，失败%d个", successCount, failCount), result);
    }
    
    @Override
    @Transactional
    public Result copyCommodity(String commodityId) {
        User currentUser = BusinessValidator.requireLogin();
        Commodity originalCommodity = BusinessValidator.requireCommodity(commodityId, commodityRepository);
        BusinessValidator.requireCommodityOwner(originalCommodity, currentUser.getUserId());
        
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
        User currentUser = BusinessValidator.requireLogin();
        
        // 使用CommodityQueryService获取统计信息
        return commodityQueryService.getCommodityStats(currentUser);
    }
    
    // ========== 用户商品管理 ==========
    
    @Override
    public Result getMyCommodities(Integer page, Integer size, String status) {
        User currentUser = BusinessValidator.requireLogin();
        
        // 统一使用 getUserCommodities 方法，sellerId 为 null 表示查询自己的商品
        return commodityQueryService.getUserCommodities(currentUser, null, status, page, size);
    }
    
    @Override
    public Result getMyCommodityDetail(String commodityId) {
        User currentUser = BusinessValidator.requireLogin();
        Commodity commodity = BusinessValidator.requireCommodity(commodityId, commodityRepository);
        BusinessValidator.requireCommodityOwner(commodity, currentUser.getUserId());
        
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
        
        Map<String, Object> result = new HashMap<>();
        result.put("commodities", commodityDTOs);
        result.put("total", commodityPage.getTotalElements());
        result.put("pages", commodityPage.getTotalPages());
        result.put("current", page);
        result.put("size", size);
        
        return Result.ok("获取商品列表成功", result);
    }
    
    @Override
    @Transactional
    public Result removeCommodity(String commodityId, String reason) {
        Commodity commodity = BusinessValidator.requireCommodity(commodityId, commodityRepository);
        
        commodity.setCommodityStatus("OFF_SHELF");
        LocalDateTime now = LocalDateTime.now();
        commodityRepository.save(commodity);
        
        // ✅ 记录商品变更（用于增量轮询）- 管理端强制下架也需要更新聊天界面
        changeRecordService.recordCommodityChange(commodityId, "UNSHELF", now);
        
        return Result.ok("商品强制下架成功");
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