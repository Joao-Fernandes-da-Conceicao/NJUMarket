package com.njumarket.order.controller;

import com.njumarket.njumarket.dto.Result;
import com.njumarket.njumarket.dto.internal.InternalDTOConverter;
import com.njumarket.njumarket.dto.internal.OrderInternalDTO;
import com.njumarket.njumarket.entity.Order;
import com.njumarket.njumarket.exception.BusinessException;
import com.njumarket.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

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
    
    private final OrderRepository orderRepository;
    private final InternalDTOConverter internalDTOConverter;
    
    /**
     * 获取订单详情（管理端内部接口）
     * 返回内部 DTO，不包含关联对象
     */
    @GetMapping("/order/{orderId}")
    public Result getOrderById(@PathVariable String orderId) {
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new BusinessException("订单不存在"));
        OrderInternalDTO dto = internalDTOConverter.toInternalDTO(order);
        return Result.ok("查询成功", dto);
    }
    
    /**
     * 完整更新订单（管理端内部接口）
     */
    @PutMapping("/order/{orderId}/full")
    public Result updateOrderFull(@PathVariable String orderId,
                                  @RequestBody Map<String, Object> payload) {
        Order o = orderRepository.findById(orderId)
            .orElseThrow(() -> new BusinessException("订单不存在"));
            
            // 更新字段
            Object orderStatus = payload.get("orderStatus");
            if (orderStatus instanceof String) {
                String st = ((String) orderStatus).trim();
                java.util.Set<String> allowedStatus = new java.util.HashSet<>(java.util.Arrays.asList(
                    "PENDING", "PAID", "SHIPPED", "DELIVERED", "COMPLETED", "CANCELLED", "REFUNDED"
                ));
                if (allowedStatus.contains(st)) {
                    o.setOrderStatus(st);
                }
            }
            
            Object trackingNumber = payload.get("trackingNumber");
            if (trackingNumber instanceof String) o.setTrackingNumber(((String) trackingNumber).trim());
            
            Object remark = payload.get("remark");
            if (remark instanceof String) o.setRemark(((String) remark).trim());
            
            Object sellerVisibility = payload.get("sellerVisibility");
            if (sellerVisibility instanceof String) {
                String vis = ((String) sellerVisibility).trim();
                java.util.Set<String> allowedVis = new java.util.HashSet<>(java.util.Arrays.asList("PUBLIC","PRIVATE","HIDDEN"));
                if (allowedVis.contains(vis)) {
                    o.setSellerVisibility(vis);
                }
            }
            
            Object buyerVisibility = payload.get("buyerVisibility");
            if (buyerVisibility instanceof String) {
                String vis = ((String) buyerVisibility).trim();
                java.util.Set<String> allowedVis = new java.util.HashSet<>(java.util.Arrays.asList("PUBLIC","PRIVATE","HIDDEN"));
                if (allowedVis.contains(vis)) {
                    o.setBuyerVisibility(vis);
                }
            }
            
        orderRepository.save(o);
        return Result.ok("更新成功", o);
    }
    
    /**
     * 删除订单（管理端内部接口）
     */
    @DeleteMapping("/order/{orderId}")
    public Result deleteOrder(@PathVariable String orderId) {
        if (!orderRepository.existsById(orderId)) {
            throw new BusinessException("订单不存在");
        }
        orderRepository.deleteById(orderId);
        return Result.ok("删除成功");
    }
    
    /**
     * 检查商品是否有订单（内部接口，供Commodity Service调用）
     */
    @GetMapping("/order/check-commodity/{commodityId}")
    public Result checkCommodityHasOrders(@PathVariable String commodityId) {
        List<Order> orders = orderRepository.findByCommodityId(commodityId);
        if (orders != null && !orders.isEmpty()) {
            throw new BusinessException("该商品已有订单，无法删除");
        }
        return Result.ok("该商品没有订单");
    }
    
    /**
     * 查询订单列表（管理端内部接口）
     */
    @GetMapping("/orders")
    public Result listOrders(@RequestParam(defaultValue = "1") Integer page,
                            @RequestParam(defaultValue = "10") Integer size,
                            @RequestParam(required = false) String keyword,
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
        org.springframework.data.jpa.domain.Specification<Order> spec = (root, query, cb) -> {
            java.util.List<jakarta.persistence.criteria.Predicate> predicates = new java.util.ArrayList<>();
            
            // 关键词搜索：订单ID、商品标题、买家ID、卖家ID（处理空字符串）
            if (keyword != null && !keyword.trim().isEmpty()) {
                String kw = keyword.trim().toLowerCase();
                predicates.add(cb.or(
                    cb.like(cb.lower(root.get("orderId")), "%" + kw + "%"),
                    cb.like(cb.lower(root.get("commodityTitle")), "%" + kw + "%"),
                    cb.like(cb.lower(root.get("buyerId")), "%" + kw + "%"),
                    cb.like(cb.lower(root.get("sellerId")), "%" + kw + "%")
                ));
            }
            
            // 状态筛选（处理空字符串）
            if (status != null && !status.trim().isEmpty()) {
                predicates.add(cb.equal(root.get("orderStatus"), status.trim()));
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
        
        org.springframework.data.domain.Page<Order> orderPage = 
            orderRepository.findAll(spec, pageable);
        
        // 转换为内部 DTO 列表
        List<OrderInternalDTO> orderDTOs = orderPage.getContent().stream()
            .map(internalDTOConverter::toInternalDTO)
            .collect(java.util.stream.Collectors.toList());
        
        // 构建分页结果
        Map<String, Object> result = new java.util.HashMap<>();
        result.put("content", orderDTOs);
        result.put("totalElements", orderPage.getTotalElements());
        result.put("totalPages", orderPage.getTotalPages());
        result.put("number", orderPage.getNumber());
        result.put("size", orderPage.getSize());
        
        return Result.ok("查询成功", result);
    }
}

