package com.njumarket.order.controller;

import com.njumarket.njumarket.dto.Result;
import com.njumarket.njumarket.dto.internal.InternalDTOConverter;
import com.njumarket.njumarket.dto.internal.OrderInternalDTO;
import com.njumarket.njumarket.entity.Order;
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
        try {
            Optional<Order> opt = orderRepository.findById(orderId);
            if (opt.isEmpty()) {
                return Result.fail("订单不存在");
            }
            OrderInternalDTO dto = internalDTOConverter.toInternalDTO(opt.get());
            return Result.ok("查询成功", dto);
        } catch (Exception e) {
            log.error("查询订单失败: orderId={}, error={}", orderId, e.getMessage(), e);
            return Result.fail("查询失败");
        }
    }
    
    /**
     * 完整更新订单（管理端内部接口）
     */
    @PutMapping("/order/{orderId}/full")
    public Result updateOrderFull(@PathVariable String orderId,
                                  @RequestBody Map<String, Object> payload) {
        try {
            Optional<Order> opt = orderRepository.findById(orderId);
            if (opt.isEmpty()) {
                return Result.fail("订单不存在");
            }
            Order o = opt.get();
            
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
        } catch (Exception e) {
            log.error("完整更新订单异常: orderId={}, error={}", orderId, e.getMessage(), e);
            return Result.fail("更新失败");
        }
    }
    
    /**
     * 删除订单（管理端内部接口）
     */
    @DeleteMapping("/order/{orderId}")
    public Result deleteOrder(@PathVariable String orderId) {
        try {
            if (!orderRepository.existsById(orderId)) {
                return Result.fail("订单不存在");
            }
            orderRepository.deleteById(orderId);
            return Result.ok("删除成功");
        } catch (Exception e) {
            log.error("删除订单异常: orderId={}, error={}", orderId, e.getMessage(), e);
            return Result.fail("删除失败");
        }
    }
    
    /**
     * 检查商品是否有订单（内部接口，供Commodity Service调用）
     */
    @GetMapping("/order/check-commodity/{commodityId}")
    public Result checkCommodityHasOrders(@PathVariable String commodityId) {
        try {
            List<Order> orders = orderRepository.findByCommodityId(commodityId);
            if (orders != null && !orders.isEmpty()) {
                return Result.fail("该商品已有订单，无法删除");
            }
            return Result.ok("该商品没有订单");
        } catch (Exception e) {
            log.error("检查商品订单失败: commodityId={}, error={}", commodityId, e.getMessage(), e);
            return Result.fail("检查失败");
        }
    }
}

