package com.njumarket.commodity.controller;

import com.njumarket.njumarket.dto.Result;
import com.njumarket.njumarket.dto.internal.CommodityInternalDTO;
import com.njumarket.njumarket.dto.internal.InternalDTOConverter;
import com.njumarket.njumarket.entity.Commodity;
import com.njumarket.commodity.repository.CommodityRepository;
import com.njumarket.commodity.service.ChangeRecordService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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
    private final ChangeRecordService changeRecordService;
    private final InternalDTOConverter internalDTOConverter;
    
    /**
     * 查询商品（带悲观锁，用于创建订单）
     * 返回内部 DTO，不包含关联对象
     * 
     * ✅ 注意：findByIdForUpdate 使用 SELECT ... FOR UPDATE，需要写事务（不能是只读事务）
     */
    @GetMapping("/commodity/{commodityId}/for-update")
    @Transactional  // ✅ 添加事务注解，支持 SELECT ... FOR UPDATE（必须是写事务，不能是只读）
    public Result getCommodityForUpdate(@PathVariable String commodityId) {
        try {
            Optional<Commodity> commodityOpt = commodityRepository.findByIdForUpdate(commodityId);
            if (commodityOpt.isEmpty()) {
                return Result.fail("商品不存在");
            }
            CommodityInternalDTO dto = internalDTOConverter.toInternalDTO(commodityOpt.get());
            return Result.ok("查询成功", dto);
        } catch (Exception e) {
            log.error("查询商品失败: commodityId={}, error={}", commodityId, e.getMessage(), e);
            return Result.fail("查询商品失败");
        }
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
        try {
            int updateResult = commodityRepository.updateStockWithCondition(commodityId, quantity);
            if (updateResult == 0) {
                return Result.fail("库存不足");
            }
            return Result.ok("库存更新成功");
        } catch (Exception e) {
            log.error("更新商品库存失败: commodityId={}, quantity={}, error={}", 
                commodityId, quantity, e.getMessage(), e);
            return Result.fail("更新商品库存失败");
        }
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
        try {
            Optional<Commodity> commodityOpt = commodityRepository.findById(commodityId);
            if (commodityOpt.isEmpty()) {
                return Result.fail("商品不存在");
            }
            Commodity commodity = commodityOpt.get();
            boolean stockUpdated = commodity.updateStock(quantity);
            if (stockUpdated) {
                commodityRepository.save(commodity);
                return Result.ok("库存恢复成功");
            } else {
                return Result.fail("库存恢复失败");
            }
        } catch (Exception e) {
            log.error("恢复商品库存失败: commodityId={}, quantity={}, error={}", 
                commodityId, quantity, e.getMessage(), e);
            return Result.fail("恢复商品库存失败");
        }
    }
    
    /**
     * 获取商品变更记录
     */
    @GetMapping("/change-record/commodity")
    public Result getCommodityChangesAfter(@RequestParam String timestamp) {
        try {
            LocalDateTime afterTimestamp = LocalDateTime.parse(timestamp);
            List<String> changes = changeRecordService.getCommodityChangesAfter(afterTimestamp);
            return Result.ok("查询成功", changes);
        } catch (Exception e) {
            log.error("查询商品变更记录失败: {}", e.getMessage(), e);
            return Result.fail("查询商品变更记录失败");
        }
    }
    
    /**
     * 获取订单变更记录
     */
    @GetMapping("/change-record/order")
    public Result getOrderChangesAfter(@RequestParam String timestamp) {
        try {
            LocalDateTime afterTimestamp = LocalDateTime.parse(timestamp);
            List<String> changes = changeRecordService.getOrderChangesAfter(afterTimestamp);
            return Result.ok("查询成功", changes);
        } catch (Exception e) {
            log.error("查询订单变更记录失败: {}", e.getMessage(), e);
            return Result.fail("查询订单变更记录失败");
        }
    }
    
    /**
     * 记录订单变更（内部接口，供Order Service调用）
     */
    @PostMapping("/change-record/order")
    public Result recordOrderChange(@RequestParam String orderId,
                                    @RequestParam String operation,
                                    @RequestParam String timestamp) {
        try {
            LocalDateTime changeTimestamp = LocalDateTime.parse(timestamp);
            changeRecordService.recordOrderChange(orderId, operation, changeTimestamp);
            return Result.ok("记录成功");
        } catch (Exception e) {
            log.error("记录订单变更失败: orderId={}, operation={}, error={}", 
                orderId, operation, e.getMessage(), e);
            return Result.fail("记录订单变更失败");
        }
    }
    
    /**
     * 完整更新商品（管理端内部接口）
     */
    @PutMapping("/commodity/{commodityId}/full")
    public Result updateCommodityFull(@PathVariable String commodityId, 
                                     @RequestBody Map<String, Object> payload) {
        try {
            Optional<Commodity> opt = commodityRepository.findById(commodityId);
            if (opt.isEmpty()) {
                return Result.fail("商品不存在");
            }
            Commodity c = opt.get();
            
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
                    return Result.fail("非法的成色等级");
                }
                c.setConditionLevel(lvl);
            }
            
            Object commodityStatus = payload.get("commodityStatus");
            if (commodityStatus instanceof String) {
                String st = ((String) commodityStatus).trim();
                java.util.Set<String> allowedStatus = new java.util.HashSet<>(java.util.Arrays.asList("DRAFT","PUBLISHED","ON_SHELF","OFF_SHELF"));
                if (!allowedStatus.contains(st)) {
                    return Result.fail("非法的商品状态");
                }
                c.setCommodityStatus(st);
            }
            
            Object images = payload.get("images"); 
            if (images instanceof String) c.setImages(((String) images).trim());
            
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
                    return Result.fail("非法的商品分类");
                }
                c.setCategory(cat);
            }
            
            // 可见性（允许编辑）
            Object sellerVisibility = payload.get("sellerVisibility");
            if (sellerVisibility instanceof String) {
                String vis = ((String) sellerVisibility).trim();
                java.util.Set<String> allowedVis = new java.util.HashSet<>(java.util.Arrays.asList("PUBLIC","PRIVATE","HIDDEN"));
                if (!allowedVis.contains(vis)) {
                    return Result.fail("非法的卖家可见性");
                }
                c.setSellerVisibility(vis);
            }
            
            Object buyerVisibility = payload.get("buyerVisibility");
            if (buyerVisibility instanceof String) {
                String vis = ((String) buyerVisibility).trim();
                java.util.Set<String> allowedVis = new java.util.HashSet<>(java.util.Arrays.asList("PUBLIC","PRIVATE","HIDDEN"));
                if (!allowedVis.contains(vis)) {
                    return Result.fail("非法的买家可见性");
                }
                c.setBuyerVisibility(vis);
            }
            
            commodityRepository.save(c);
            return Result.ok("更新成功", c);
        } catch (Exception e) {
            log.error("完整更新商品异常: commodityId={}, error={}", commodityId, e.getMessage(), e);
            return Result.fail("更新失败");
        }
    }
    
    /**
     * 删除商品（管理端内部接口）
     */
    @DeleteMapping("/commodity/{commodityId}")
    public Result deleteCommodity(@PathVariable String commodityId) {
        try {
            Optional<Commodity> opt = commodityRepository.findById(commodityId);
            if (opt.isEmpty()) {
                return Result.fail("商品不存在");
            }
            commodityRepository.delete(opt.get());
            return Result.ok("删除成功");
        } catch (Exception e) {
            log.error("删除商品异常: commodityId={}, error={}", commodityId, e.getMessage(), e);
            return Result.fail("删除失败");
        }
    }
    
}
