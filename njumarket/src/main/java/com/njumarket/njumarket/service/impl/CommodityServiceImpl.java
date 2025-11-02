package com.njumarket.njumarket.service.impl;

import com.njumarket.njumarket.dto.Result;
import com.njumarket.njumarket.dto.CommodityDTO;
import com.njumarket.njumarket.dto.ImageUploadDTO;
import com.njumarket.njumarket.entity.Commodity;
import com.njumarket.njumarket.entity.Order;
import com.njumarket.njumarket.entity.User;
import com.njumarket.njumarket.repository.CommodityRepository;
import com.njumarket.njumarket.repository.OrderRepository;
import com.njumarket.njumarket.service.ChangeRecordService;
import com.njumarket.njumarket.service.CommodityService;
import com.njumarket.njumarket.service.CommodityQueryService;
import com.njumarket.njumarket.service.ImageService;
import com.njumarket.njumarket.utils.UserHolder;
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
        try {
            log.info("发布商品 - commodityDTO: {}", commodityDTO);
            
            // 获取当前用户
            User currentUser = UserHolder.getUser();
            if (currentUser == null) {
                return Result.fail("用户未登录");
            }
            
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
            
            log.info("商品发布成功 - commodityId: {}", savedCommodity.getCommodityId());
            return Result.ok("商品发布成功", convertToDTO(savedCommodity));
            
        } catch (Exception e) {
            log.error("发布商品失败: {}", e.getMessage(), e);
            return Result.fail("发布商品失败，请稍后重试");
        }
    }
    
    @Override
    @Transactional
    public Result createDraftCommodity(CommodityDTO commodityDTO) {
        try {
            log.info("创建草稿商品 - commodityDTO: {}", commodityDTO);
            
            // 获取当前用户
            User currentUser = UserHolder.getUser();
            if (currentUser == null) {
                return Result.fail("用户未登录");
            }
            
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
            
            log.info("草稿商品创建成功 - commodityId: {}", savedCommodity.getCommodityId());
            return Result.ok("草稿商品创建成功", convertToDTO(savedCommodity));
            
        } catch (Exception e) {
            log.error("创建草稿商品失败: {}", e.getMessage(), e);
            return Result.fail("创建草稿商品失败，请稍后重试");
        }
    }
    
    @Override
    @Transactional
    public Result publishDraftCommodity(String commodityId) {
        try {
            log.info("发布草稿商品 - commodityId: {}", commodityId);
            
            // 获取当前用户
            User currentUser = UserHolder.getUser();
            if (currentUser == null) {
                return Result.fail("用户未登录");
            }
            
            // 查找商品
            Commodity commodity = commodityRepository.findById(commodityId).orElse(null);
            if (commodity == null) {
                return Result.fail("商品不存在");
            }
            
            // 检查权限
            if (!commodityQueryService.canUserEditCommodity(commodity, currentUser)) {
                return Result.fail("无权限操作此商品");
            }
            
            // 检查商品状态：只有草稿状态的商品可以发布
            if (!"DRAFT".equals(commodity.getCommodityStatus())) {
                return Result.fail("只有草稿状态的商品可以发布");
            }
            
            // 发布商品
            commodity.setCommodityStatus("PUBLISHED");
            commodity.setPublishTime(LocalDateTime.now());
            commodityRepository.save(commodity);
            
            log.info("草稿商品发布成功 - commodityId: {}", commodityId);
            return Result.ok("草稿商品发布成功");
            
        } catch (Exception e) {
            log.error("发布草稿商品失败: {}", e.getMessage(), e);
            return Result.fail("发布草稿商品失败");
        }
    }
    
    @Override
    @Transactional
    public Result updateCommodity(String commodityId, CommodityDTO commodityDTO) {
        try {
            log.info("更新商品 - commodityId: {}, commodityDTO: {}", commodityId, commodityDTO);
            
            // 获取当前用户
            User currentUser = UserHolder.getUser();
            if (currentUser == null) {
                return Result.fail("用户未登录");
            }
            
            // 查找商品
            Commodity commodity = commodityRepository.findById(commodityId).orElse(null);
            if (commodity == null) {
                return Result.fail("商品不存在");
            }
            
            // 检查权限
            if (!commodityQueryService.canUserEditCommodity(commodity, currentUser)) {
                return Result.fail("无权限编辑此商品");
            }
            
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
            
            log.info("商品更新成功 - commodityId: {}", commodityId);
            return Result.ok("商品更新成功", convertToDTO(updatedCommodity));
            
        } catch (Exception e) {
            log.error("更新商品失败: {}", e.getMessage(), e);
            return Result.fail("更新商品失败，请稍后重试");
        }
    }
    
    @Override
    @Transactional
    public Result deleteCommodity(String commodityId) {
        try {
            log.info("删除商品 - commodityId: {}", commodityId);
            
            // 获取当前用户
            User currentUser = UserHolder.getUser();
            if (currentUser == null) {
                return Result.fail("用户未登录");
            }
            
            // 查找商品
            Commodity commodity = commodityRepository.findById(commodityId).orElse(null);
            if (commodity == null) {
                return Result.fail("商品不存在");
            }
            
            // 检查权限
            if (!commodityQueryService.canUserEditCommodity(commodity, currentUser)) {
                return Result.fail("无权限删除此商品");
            }
            
            // 检查是否有订单
            List<Order> orders = orderRepository.findByCommodityId(commodityId);
            if (!orders.isEmpty()) {
                return Result.fail("该商品已有订单，无法删除");
            }
            
            // 删除商品
            commodityRepository.delete(commodity);
            
            log.info("商品删除成功 - commodityId: {}", commodityId);
            return Result.ok("商品删除成功");
            
        } catch (Exception e) {
            log.error("删除商品失败: {}", e.getMessage(), e);
            return Result.fail("删除商品失败，请稍后重试");
        }
    }
    
    // ========== 商品状态管理 ==========
    
    @Override
    @Transactional
    public Result shelfCommodity(String commodityId) {
        try {
            log.info("上架商品 - commodityId: {}", commodityId);
            
            User currentUser = UserHolder.getUser();
            if (currentUser == null) {
                return Result.fail("用户未登录");
            }
            
            Commodity commodity = commodityRepository.findById(commodityId).orElse(null);
            if (commodity == null) {
                return Result.fail("商品不存在");
            }
            
            if (!commodityQueryService.canUserEditCommodity(commodity, currentUser)) {
                return Result.fail("无权限操作此商品");
            }
            
            // 检查商品状态：只有已发布的商品可以上架
            if (!"PUBLISHED".equals(commodity.getCommodityStatus())) {
                return Result.fail("只有已发布的商品可以上架，请先发布商品");
            }
            
            commodity.setCommodityStatus("ON_SHELF");
            LocalDateTime now = LocalDateTime.now();
            commodity.setPublishTime(now);
            commodityRepository.save(commodity);
            
            // ✅ 记录商品变更（用于增量轮询）
            changeRecordService.recordCommodityChange(commodityId, "SHELF", now);
            
            log.info("商品上架成功 - commodityId: {}", commodityId);
            return Result.ok("商品上架成功");
            
        } catch (Exception e) {
            log.error("上架商品失败: {}", e.getMessage(), e);
            return Result.fail("上架商品失败");
        }
    }
    
    @Override
    @Transactional
    public Result unshelfCommodity(String commodityId) {
        try {
            log.info("下架商品 - commodityId: {}", commodityId);
            
            User currentUser = UserHolder.getUser();
            if (currentUser == null) {
                return Result.fail("用户未登录");
            }
            
            Commodity commodity = commodityRepository.findById(commodityId).orElse(null);
            if (commodity == null) {
                return Result.fail("商品不存在");
            }
            
            if (!commodityQueryService.canUserEditCommodity(commodity, currentUser)) {
                return Result.fail("无权限操作此商品");
            }
            
            commodity.setCommodityStatus("OFF_SHELF");
            LocalDateTime now = LocalDateTime.now();
            commodityRepository.save(commodity);
            
            // ✅ 记录商品变更（用于增量轮询）- 下架操作影响聊天界面显示
            changeRecordService.recordCommodityChange(commodityId, "UNSHELF", now);
            
            log.info("商品下架成功 - commodityId: {}", commodityId);
            return Result.ok("商品下架成功");
            
        } catch (Exception e) {
            log.error("下架商品失败: {}", e.getMessage(), e);
            return Result.fail("下架商品失败");
        }
    }
    
    @Override
    @Transactional
    public Result draftCommodity(String commodityId) {
        try {
            log.info("设为草稿 - commodityId: {}", commodityId);
            
            User currentUser = UserHolder.getUser();
            if (currentUser == null) {
                return Result.fail("用户未登录");
            }
            
            Commodity commodity = commodityRepository.findById(commodityId).orElse(null);
            if (commodity == null) {
                return Result.fail("商品不存在");
            }
            
            if (!commodityQueryService.canUserEditCommodity(commodity, currentUser)) {
                return Result.fail("无权限操作此商品");
            }
            
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
                log.debug("商品从已上架状态设为草稿，已记录变更: commodityId={}", commodityId);
            } else {
                log.debug("商品从非上架状态设为草稿，跳过变更记录: commodityId={}, previousStatus={}", 
                    commodityId, previousStatus);
            }
            
            log.info("商品设为草稿成功 - commodityId: {}", commodityId);
            return Result.ok("商品设为草稿成功");
            
        } catch (Exception e) {
            log.error("设为草稿失败: {}", e.getMessage(), e);
            return Result.fail("设为草稿失败");
        }
    }
    
    @Override
    @Transactional
    public Result republishCommodity(String commodityId) {
        try {
            log.info("重新上架商品 - commodityId: {}", commodityId);
            
            User currentUser = UserHolder.getUser();
            if (currentUser == null) {
                return Result.fail("用户未登录");
            }
            
            Commodity commodity = commodityRepository.findById(commodityId).orElse(null);
            if (commodity == null) {
                return Result.fail("商品不存在");
            }
            
            if (!commodityQueryService.canUserEditCommodity(commodity, currentUser)) {
                return Result.fail("无权限操作此商品");
            }
            
            commodity.setCommodityStatus("ON_SHELF");
            LocalDateTime now = LocalDateTime.now();
            commodity.setPublishTime(now);
            commodityRepository.save(commodity);
            
            // ✅ 记录商品变更（用于增量轮询）
            changeRecordService.recordCommodityChange(commodityId, "SHELF", now);
            
            log.info("商品重新上架成功 - commodityId: {}", commodityId);
            return Result.ok("商品重新上架成功");
            
        } catch (Exception e) {
            log.error("重新上架商品失败: {}", e.getMessage(), e);
            return Result.fail("重新上架商品失败");
        }
    }
    
    // ========== 商品可见性管理 ==========
    
    @Override
    @Transactional
    public Result updateCommodityVisibility(String commodityId, String visibility) {
        try {
            log.info("修改商品可见性 - commodityId: {}, visibility: {}", commodityId, visibility);
            
            User currentUser = UserHolder.getUser();
            if (currentUser == null) {
                return Result.fail("用户未登录");
            }
            
            Commodity commodity = commodityRepository.findById(commodityId).orElse(null);
            if (commodity == null) {
                return Result.fail("商品不存在");
            }
            
            if (!commodityQueryService.canUserEditCommodity(commodity, currentUser)) {
                return Result.fail("无权限操作此商品");
            }
            
            commodity.setSellerVisibility(visibility);
            commodity.setBuyerVisibility(visibility);
            commodityRepository.save(commodity);
            
            log.info("商品可见性修改成功 - commodityId: {}", commodityId);
            return Result.ok("商品可见性修改成功");
            
        } catch (Exception e) {
            log.error("修改商品可见性失败: {}", e.getMessage(), e);
            return Result.fail("修改商品可见性失败");
        }
    }
    
    @Override
    @Transactional
    public Result updateCommoditySellerVisibility(String commodityId, String sellerVisibility) {
        try {
            log.info("修改商品卖家可见性 - commodityId: {}, sellerVisibility: {}", commodityId, sellerVisibility);
            
            User currentUser = UserHolder.getUser();
            if (currentUser == null) {
                return Result.fail("用户未登录");
            }
            
            Commodity commodity = commodityRepository.findById(commodityId).orElse(null);
            if (commodity == null) {
                return Result.fail("商品不存在");
            }
            
            if (!commodityQueryService.canUserEditCommodity(commodity, currentUser)) {
                return Result.fail("无权限操作此商品");
            }
            
            commodity.setSellerVisibility(sellerVisibility);
            commodityRepository.save(commodity);
            
            log.info("商品卖家可见性修改成功 - commodityId: {}", commodityId);
            return Result.ok("商品卖家可见性修改成功");
            
        } catch (Exception e) {
            log.error("修改商品卖家可见性失败: {}", e.getMessage(), e);
            return Result.fail("修改商品卖家可见性失败");
        }
    }
    
    @Override
    @Transactional
    public Result updateCommodityBuyerVisibility(String commodityId, String buyerVisibility) {
        try {
            log.info("修改商品买家可见性 - commodityId: {}, buyerVisibility: {}", commodityId, buyerVisibility);
            
            User currentUser = UserHolder.getUser();
            if (currentUser == null) {
                return Result.fail("用户未登录");
            }
            
            Commodity commodity = commodityRepository.findById(commodityId).orElse(null);
            if (commodity == null) {
                return Result.fail("商品不存在");
            }
            
            if (!commodityQueryService.canUserEditCommodity(commodity, currentUser)) {
                return Result.fail("无权限操作此商品");
            }
            
            commodity.setBuyerVisibility(buyerVisibility);
            commodityRepository.save(commodity);
            
            log.info("商品买家可见性修改成功 - commodityId: {}", commodityId);
            return Result.ok("商品买家可见性修改成功");
            
        } catch (Exception e) {
            log.error("修改商品买家可见性失败: {}", e.getMessage(), e);
            return Result.fail("修改商品买家可见性失败");
        }
    }
    
    // ========== 图片管理 ==========
    
    @Override
    public Result uploadImage(MultipartFile file) {
        try {
            log.info("上传商品图片 - fileName: {}", file.getOriginalFilename());
            
            User currentUser = UserHolder.getUser();
            if (currentUser == null) {
                return Result.fail("用户未登录");
            }
            
            ImageUploadDTO uploadResult = imageService.uploadCommodityImage(currentUser.getUserId(), file);
            if (uploadResult.isSuccess()) {
                return Result.ok("图片上传成功", uploadResult);
            } else {
                return Result.fail(uploadResult.getMessage());
            }
            
        } catch (Exception e) {
            log.error("上传图片失败: {}", e.getMessage(), e);
            return Result.fail("上传图片失败");
        }
    }
    
    @Override
    public Result uploadCommodityImage(String commodityId, MultipartFile file) {
        try {
            log.info("为商品上传图片 - commodityId: {}, fileName: {}", commodityId, file.getOriginalFilename());
            
            User currentUser = UserHolder.getUser();
            if (currentUser == null) {
                return Result.fail("用户未登录");
            }
            
            Commodity commodity = commodityRepository.findById(commodityId).orElse(null);
            if (commodity == null) {
                return Result.fail("商品不存在");
            }
            
            if (!commodityQueryService.canUserEditCommodity(commodity, currentUser)) {
                return Result.fail("无权限操作此商品");
            }
            
            ImageUploadDTO uploadResult = imageService.uploadCommodityImageForCommodity(commodityId, file);
            if (uploadResult.isSuccess()) {
                return Result.ok("图片上传成功", uploadResult);
            } else {
                return Result.fail(uploadResult.getMessage());
            }
            
        } catch (Exception e) {
            log.error("上传商品图片失败: {}", e.getMessage(), e);
            return Result.fail("上传商品图片失败");
        }
    }
    
    // ========== 批量操作 ==========
    
    @Override
    @Transactional
    public Result batchOperation(String[] commodityIds, String operation) {
        try {
            log.info("批量操作商品 - commodityIds: {}, operation: {}", Arrays.toString(commodityIds), operation);
            
            User currentUser = UserHolder.getUser();
            if (currentUser == null) {
                return Result.fail("用户未登录");
            }
            
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
                } catch (Exception e) {
                    log.error("批量操作商品失败 - commodityId: {}, operation: {}", commodityId, operation, e);
                    failCount++;
                }
            }
            
            log.info("批量操作完成 - 成功: {}, 失败: {}", successCount, failCount);
            return Result.ok(String.format("批量操作完成，成功%d个，失败%d个", successCount, failCount));
            
        } catch (Exception e) {
            log.error("批量操作商品失败: {}", e.getMessage(), e);
            return Result.fail("批量操作失败");
        }
    }
    
    @Override
    @Transactional
    public Result copyCommodity(String commodityId) {
        try {
            log.info("复制商品 - commodityId: {}", commodityId);
            
            User currentUser = UserHolder.getUser();
            if (currentUser == null) {
                return Result.fail("用户未登录");
            }
            
            Commodity originalCommodity = commodityRepository.findById(commodityId).orElse(null);
            if (originalCommodity == null) {
                return Result.fail("原商品不存在");
            }
            
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
            
            log.info("商品复制成功 - newCommodityId: {}", savedCommodity.getCommodityId());
            return Result.ok("商品复制成功", convertToDTO(savedCommodity));
            
        } catch (Exception e) {
            log.error("复制商品失败: {}", e.getMessage(), e);
            return Result.fail("复制商品失败");
        }
    }
    
    // ========== 统计和分析 ==========
    
    @Override
    public Result getSalesStatistics(String period) {
        try {
            log.info("获取商品销售统计 - period: {}", period);
            
            User currentUser = UserHolder.getUser();
            if (currentUser == null) {
                return Result.fail("用户未登录");
            }
            
            // 使用CommodityQueryService获取统计信息
            return commodityQueryService.getCommodityStats(currentUser);
            
        } catch (Exception e) {
            log.error("获取销售统计失败: {}", e.getMessage(), e);
            return Result.fail("获取销售统计失败");
        }
    }
    
    // ========== 用户商品管理 ==========
    
    @Override
    public Result getMyCommodities(Integer page, Integer size, String status) {
        try {
            log.info("获取我的商品 - page: {}, size: {}, status: {}", page, size, status);
            
            User currentUser = UserHolder.getUser();
            if (currentUser == null) {
                return Result.fail("用户未登录");
            }
            
            // 统一使用 getUserCommodities 方法，sellerId 为 null 表示查询自己的商品
            return commodityQueryService.getUserCommodities(currentUser, null, status, page, size);
            
        } catch (Exception e) {
            log.error("获取我的商品失败: {}", e.getMessage(), e);
            return Result.fail("获取我的商品失败");
        }
    }
    
    @Override
    public Result getMyCommodityDetail(String commodityId) {
        try {
            log.info("获取我的商品详情 - commodityId: {}", commodityId);
            
            User currentUser = UserHolder.getUser();
            if (currentUser == null) {
                return Result.fail("用户未登录");
            }
            
            Commodity commodity = commodityRepository.findById(commodityId).orElse(null);
            if (commodity == null) {
                return Result.fail("商品不存在");
            }
            
            if (!commodityQueryService.canUserEditCommodity(commodity, currentUser)) {
                return Result.fail("无权限查看此商品");
            }
            
            return Result.ok("获取商品详情成功", convertToDTO(commodity));
            
        } catch (Exception e) {
            log.error("获取我的商品详情失败: {}", e.getMessage(), e);
            return Result.fail("获取商品详情失败");
        }
    }
    
    // ========== 管理端功能 ==========
    
    @Override
    public Result getCommodityList(Integer page, Integer size, String status) {
        try {
            log.info("获取商品列表（管理端） - page: {}, size: {}, status: {}", page, size, status);
            
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
            
        } catch (Exception e) {
            log.error("获取商品列表失败: {}", e.getMessage(), e);
            return Result.fail("获取商品列表失败");
        }
    }
    
    @Override
    @Transactional
    public Result removeCommodity(String commodityId, String reason) {
        try {
            log.info("强制下架商品（管理端） - commodityId: {}, reason: {}", commodityId, reason);
            
            Commodity commodity = commodityRepository.findById(commodityId).orElse(null);
            if (commodity == null) {
                return Result.fail("商品不存在");
            }
            
            commodity.setCommodityStatus("OFF_SHELF");
            LocalDateTime now = LocalDateTime.now();
            commodityRepository.save(commodity);
            
            // ✅ 记录商品变更（用于增量轮询）- 管理端强制下架也需要更新聊天界面
            changeRecordService.recordCommodityChange(commodityId, "UNSHELF", now);
            
            log.info("商品强制下架成功 - commodityId: {}", commodityId);
            return Result.ok("商品强制下架成功");
            
        } catch (Exception e) {
            log.error("强制下架商品失败: {}", e.getMessage(), e);
            return Result.fail("强制下架商品失败");
        }
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