package com.njumarket.njumarket.service.impl;

import com.njumarket.njumarket.dto.Result;
import com.njumarket.njumarket.dto.OrderDTO;
import com.njumarket.njumarket.entity.Order;
import com.njumarket.njumarket.entity.Commodity;
import com.njumarket.njumarket.entity.User;
import com.njumarket.njumarket.repository.OrderRepository;
import com.njumarket.njumarket.repository.CommodityRepository;
import com.njumarket.njumarket.service.OrderService;
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

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 订单服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final CommodityRepository commodityRepository;

    // ========== 买家功能 ==========
    @Override
    @Transactional
    public Result createOrder(OrderDTO orderDTO) {
        try {
            log.info("创建订单 - orderDTO: {}", orderDTO);
            
            // 获取当前用户
            User currentUser = UserHolder.getUser();
            if (currentUser == null) {
                return Result.fail("用户未登录");
            }
            
            // 验证订单信息
            if (!StringUtils.hasText(orderDTO.getCommodityId())) {
                return Result.fail("商品ID不能为空");
            }
            if (orderDTO.getQuantity() == null || orderDTO.getQuantity() <= 0) {
                return Result.fail("购买数量必须大于0");
            }
            if (orderDTO.getPayAmount() == null || orderDTO.getPayAmount() <= 0) {
                return Result.fail("支付金额必须大于0");
            }
            
            // 查找商品
            Optional<Commodity> commodityOpt = commodityRepository.findById(orderDTO.getCommodityId());
            if (commodityOpt.isEmpty()) {
                return Result.fail("商品不存在");
            }
            
            Commodity commodity = commodityOpt.get();
            
            // 检查商品状态
            if (!"ON_SHELF".equals(commodity.getCommodityStatus())) {
                return Result.fail("商品未上架，无法购买");
            }
            
            // 检查库存
            if (commodity.getStock() < orderDTO.getQuantity()) {
                return Result.fail("商品库存不足");
            }
            
            // 检查是否购买自己的商品
            if (commodity.getSellerId().equals(currentUser.getUserId())) {
                return Result.fail("不能购买自己的商品");
            }
            
            // 验证价格
            double expectedAmount = commodity.getPrice() * orderDTO.getQuantity();
            if (Math.abs(orderDTO.getPayAmount() - expectedAmount) > 0.01) {
                return Result.fail("支付金额与商品价格不符");
            }
            
            // 创建订单
            Order order = new Order();
            order.setOrderId(UUID.randomUUID().toString().replace("-", ""));
            order.setBuyerId(currentUser.getUserId());
            order.setSellerId(commodity.getSellerId());
            order.setCommodityId(commodity.getCommodityId());
            order.setOrderStatus("CREATED");
            order.setSellerVisibility(orderDTO.getSellerVisibility() != null ? orderDTO.getSellerVisibility() : "PUBLIC");
            order.setBuyerVisibility(orderDTO.getBuyerVisibility() != null ? orderDTO.getBuyerVisibility() : "PUBLIC");
            order.setPayAmount(orderDTO.getPayAmount());
            order.setQuantity(orderDTO.getQuantity());
            order.setShippingAddress(orderDTO.getShippingAddress());
            order.setRemark(orderDTO.getRemark());
            order.setCreateTime(LocalDateTime.now());
            
            // 保存订单
            orderRepository.save(order);
            
            // 减少商品库存
            commodity.updateStock(-orderDTO.getQuantity());
            commodityRepository.save(commodity);
            
            log.info("订单创建成功 - orderId: {}", order.getOrderId());
            return Result.ok("订单创建成功");
            
        } catch (Exception e) {
            log.error("创建订单失败", e);
            return Result.fail("创建订单失败：" + e.getMessage());
        }
    }

    @Override
    @Transactional
    public Result payOrder(String orderId) {
        try {
            log.info("支付订单 - orderId: {}", orderId);
            
            // 获取当前用户
            User currentUser = UserHolder.getUser();
            if (currentUser == null) {
                return Result.fail("用户未登录");
            }
            
            // 查找订单
            Optional<Order> orderOpt = orderRepository.findById(orderId);
            if (orderOpt.isEmpty()) {
                return Result.fail("订单不存在");
            }
            
            Order order = orderOpt.get();
            
            // 检查权限：只有买家可以支付
            if (!order.getBuyerId().equals(currentUser.getUserId())) {
                return Result.fail("无权限支付此订单");
            }
            
            // 检查订单状态
            if (!"CREATED".equals(order.getOrderStatus())) {
                return Result.fail("订单状态异常，无法支付");
            }
            
            // 支付订单
            if (order.payOrder()) {
                orderRepository.save(order);
                log.info("订单支付成功 - orderId: {}", orderId);
                return Result.ok("订单支付成功");
            } else {
                return Result.fail("订单支付失败");
            }
            
        } catch (Exception e) {
            log.error("支付订单失败", e);
            return Result.fail("支付订单失败：" + e.getMessage());
        }
    }

    @Override
    @Transactional
    public Result confirmOrder(String orderId) {
        try {
            log.info("确认收货 - orderId: {}", orderId);
            
            // 获取当前用户
            User currentUser = UserHolder.getUser();
            if (currentUser == null) {
                return Result.fail("用户未登录");
            }
            
            // 查找订单
            Optional<Order> orderOpt = orderRepository.findById(orderId);
            if (orderOpt.isEmpty()) {
                return Result.fail("订单不存在");
            }
            
            Order order = orderOpt.get();
            
            // 检查权限：只有买家可以确认收货
            if (!order.getBuyerId().equals(currentUser.getUserId())) {
                return Result.fail("无权限确认收货此订单");
            }
            
            // 检查订单状态
            if (!"SHIPPED".equals(order.getOrderStatus())) {
                return Result.fail("订单状态异常，无法确认收货");
            }
            
            // 确认收货
            if (order.completeOrder()) {
                orderRepository.save(order);
                log.info("订单确认收货成功 - orderId: {}", orderId);
                return Result.ok("订单确认收货成功");
            } else {
                return Result.fail("订单确认收货失败");
            }
            
        } catch (Exception e) {
            log.error("确认收货失败", e);
            return Result.fail("确认收货失败：" + e.getMessage());
        }
    }

    @Override
    @Transactional
    public Result cancelOrder(String orderId, String reason) {
        try {
            log.info("取消订单 - orderId: {}, reason: {}", orderId, reason);
            
            // 获取当前用户
            User currentUser = UserHolder.getUser();
            if (currentUser == null) {
                return Result.fail("用户未登录");
            }
            
            // 查找订单
            Optional<Order> orderOpt = orderRepository.findById(orderId);
            if (orderOpt.isEmpty()) {
                return Result.fail("订单不存在");
            }
            
            Order order = orderOpt.get();
            
            // 检查权限：只有买家和卖家可以取消订单
            if (!order.getBuyerId().equals(currentUser.getUserId()) && 
                !order.getSellerId().equals(currentUser.getUserId())) {
                return Result.fail("无权限取消此订单");
            }
            
            // 检查订单状态：只有未发货的订单可以取消
            if (!"CREATED".equals(order.getOrderStatus()) && !"PAID".equals(order.getOrderStatus())) {
                return Result.fail("订单状态不允许取消");
            }
            
            // 取消订单
            if (order.cancelOrder()) {
                // 恢复商品库存
                Optional<Commodity> commodityOpt = commodityRepository.findById(order.getCommodityId());
                if (commodityOpt.isPresent()) {
                    Commodity commodity = commodityOpt.get();
                    boolean stockUpdated = commodity.updateStock(order.getQuantity());
                    if (stockUpdated) {
                        commodityRepository.save(commodity);
                        log.info("订单取消成功，库存已恢复 - orderId: {}, commodityId: {}, quantity: {}", 
                            orderId, order.getCommodityId(), order.getQuantity());
                    } else {
                        log.warn("订单取消成功，但库存恢复失败 - orderId: {}, commodityId: {}, quantity: {}", 
                            orderId, order.getCommodityId(), order.getQuantity());
                    }
                } else {
                    log.warn("订单取消成功，但商品不存在 - orderId: {}, commodityId: {}", 
                        orderId, order.getCommodityId());
                }
                
                orderRepository.save(order);
                log.info("订单取消成功 - orderId: {}", orderId);
                return Result.ok("订单取消成功");
            } else {
                return Result.fail("订单取消失败");
            }
            
        } catch (Exception e) {
            log.error("取消订单失败", e);
            return Result.fail("取消订单失败：" + e.getMessage());
        }
    }

    @Override
    @Transactional
    public Result requestRefund(String orderId, String reason) {
        try {
            log.info("申请退款/退货 - orderId: {}, reason: {}", orderId, reason);
            
            // 获取当前用户
            User currentUser = UserHolder.getUser();
            if (currentUser == null) {
                return Result.fail("用户未登录");
            }
            
            // 查找订单
            Optional<Order> orderOpt = orderRepository.findById(orderId);
            if (orderOpt.isEmpty()) {
                return Result.fail("订单不存在");
            }
            
            Order order = orderOpt.get();
            
            // 检查权限：只有买家可以申请退款/退货
            if (!order.getBuyerId().equals(currentUser.getUserId())) {
                return Result.fail("无权限申请此订单的退款/退货");
            }
            
            // 检查订单状态：只有已完成或退款被拒的订单可以申请退款/退货
            if (!"COMPLETED".equals(order.getOrderStatus()) && !"REFUND_REJECTED".equals(order.getOrderStatus())) {
                return Result.fail("订单状态不允许申请退款/退货");
            }
            
            // 设置退款/退货状态
            order.setOrderStatus("REFUND_REQUESTED");
            order.setReturnReason(reason);
            order.setReturnRequestTime(LocalDateTime.now());
            
            orderRepository.save(order);
            
            log.info("退款/退货申请成功 - orderId: {}", orderId);
            return Result.ok("退款/退货申请成功");
            
        } catch (Exception e) {
            log.error("申请退款/退货失败", e);
            return Result.fail("申请退款/退货失败：" + e.getMessage());
        }
    }

    @Override
    public Result getBuyerOrders(Integer page, Integer size, String status) {
        try {
            log.info("获取买家订单列表 - page: {}, size: {}, status: {}", page, size, status);
            
            // 获取当前用户
            User currentUser = UserHolder.getUser();
            if (currentUser == null) {
                return Result.fail("用户未登录");
            }
            
            // 创建分页对象
            Pageable pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "createTime"));
            
            // 查询订单
            Page<Order> orderPage;
            if (StringUtils.hasText(status)) {
                orderPage = orderRepository.findByBuyerIdAndOrderStatus(currentUser.getUserId(), status, pageable);
            } else {
                orderPage = orderRepository.findByBuyerId(currentUser.getUserId(), pageable);
            }
            
            // 转换为DTO
            List<OrderDTO> orderDTOs = orderPage.getContent().stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());
            
            Map<String, Object> result = new HashMap<>();
            result.put("orders", orderDTOs);
            result.put("total", orderPage.getTotalElements());
            result.put("pages", orderPage.getTotalPages());
            result.put("current", page);
            result.put("size", size);
            
            return Result.ok("获取买家订单列表成功", result);
            
        } catch (Exception e) {
            log.error("获取买家订单列表失败", e);
            return Result.fail("获取买家订单列表失败：" + e.getMessage());
        }
    }

    // ========== 卖家功能 ==========
    @Override
    @Transactional
    public Result shipOrder(String orderId, String trackingNumber) {
        try {
            log.info("发货 - orderId: {}, trackingNumber: {}", orderId, trackingNumber);
            
            // 获取当前用户
            User currentUser = UserHolder.getUser();
            if (currentUser == null) {
                return Result.fail("用户未登录");
            }
            
            // 查找订单
            Optional<Order> orderOpt = orderRepository.findById(orderId);
            if (orderOpt.isEmpty()) {
                return Result.fail("订单不存在");
            }
            
            Order order = orderOpt.get();
            
            // 检查权限：只有卖家可以发货
            if (!order.getSellerId().equals(currentUser.getUserId())) {
                return Result.fail("无权限发货此订单");
            }
            
            // 检查订单状态
            if (!"PAID".equals(order.getOrderStatus())) {
                return Result.fail("订单状态异常，无法发货");
            }
            
            // 发货
            if (order.shipOrder(trackingNumber)) {
                orderRepository.save(order);
                log.info("订单发货成功 - orderId: {}", orderId);
                return Result.ok("订单发货成功");
            } else {
                return Result.fail("订单发货失败");
            }
            
        } catch (Exception e) {
            log.error("发货失败", e);
            return Result.fail("发货失败：" + e.getMessage());
        }
    }

    @Override
    @Transactional
    public Result handleRefund(String orderId, String decision, String remark) {
        try {
            log.info("处理退款/退货申请 - orderId: {}, decision: {}, remark: {}", orderId, decision, remark);
            
            // 获取当前用户
            User currentUser = UserHolder.getUser();
            if (currentUser == null) {
                return Result.fail("用户未登录");
            }
            
            // 查找订单
            Optional<Order> orderOpt = orderRepository.findById(orderId);
            if (orderOpt.isEmpty()) {
                return Result.fail("订单不存在");
            }
            
            Order order = orderOpt.get();
            
            // 检查权限：只有卖家可以处理退款/退货申请
            if (!order.getSellerId().equals(currentUser.getUserId())) {
                return Result.fail("无权限处理此订单的退款/退货申请");
            }
            
            // 检查订单状态：只有退款/退货申请中的订单可以处理
            if (!"REFUND_REQUESTED".equals(order.getOrderStatus())) {
                return Result.fail("订单状态不允许处理退款/退货申请");
            }
            
            // 处理退款/退货申请
            if ("APPROVE".equals(decision)) {
                // 同意退款/退货
                order.setOrderStatus("REFUND_APPROVED");
                order.setReturnApprovalTime(LocalDateTime.now());
                
                // 恢复商品库存
                Optional<Commodity> commodityOpt = commodityRepository.findById(order.getCommodityId());
                if (commodityOpt.isPresent()) {
                    Commodity commodity = commodityOpt.get();
                    boolean stockUpdated = commodity.updateStock(order.getQuantity());
                    if (stockUpdated) {
                        commodityRepository.save(commodity);
                        log.info("退款同意，库存已恢复 - orderId: {}, commodityId: {}, quantity: {}", 
                            orderId, order.getCommodityId(), order.getQuantity());
                    } else {
                        log.warn("退款同意，但库存恢复失败 - orderId: {}, commodityId: {}, quantity: {}", 
                            orderId, order.getCommodityId(), order.getQuantity());
                    }
                } else {
                    log.warn("退款同意，但商品不存在 - orderId: {}, commodityId: {}", 
                        orderId, order.getCommodityId());
                }
                
                log.info("退款/退货申请已同意 - orderId: {}", orderId);
            } else if ("REJECT".equals(decision)) {
                // 拒绝退款/退货
                order.setOrderStatus("REFUND_REJECTED");
                order.setReturnRejectionReason(remark);
                order.setReturnApprovalTime(LocalDateTime.now());
                log.info("退款/退货申请已拒绝 - orderId: {}", orderId);
            } else {
                return Result.fail("无效的处理决定");
            }
            
            orderRepository.save(order);
            
            return Result.ok("退款/退货申请处理成功");
            
        } catch (Exception e) {
            log.error("处理退款/退货申请失败", e);
            return Result.fail("处理退款/退货申请失败：" + e.getMessage());
        }
    }

    @Override
    public Result getSellerOrders(Integer page, Integer size, String status) {
        try {
            log.info("获取卖家订单列表 - page: {}, size: {}, status: {}", page, size, status);
            
            // 获取当前用户
            User currentUser = UserHolder.getUser();
            if (currentUser == null) {
                return Result.fail("用户未登录");
            }
            
            // 创建分页对象
            Pageable pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "createTime"));
            
            // 查询订单
            Page<Order> orderPage;
            if (StringUtils.hasText(status)) {
                orderPage = orderRepository.findBySellerIdAndOrderStatus(currentUser.getUserId(), status, pageable);
            } else {
                orderPage = orderRepository.findBySellerId(currentUser.getUserId(), pageable);
            }
            
            // 转换为DTO
            List<OrderDTO> orderDTOs = orderPage.getContent().stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());
            
            Map<String, Object> result = new HashMap<>();
            result.put("orders", orderDTOs);
            result.put("total", orderPage.getTotalElements());
            result.put("pages", orderPage.getTotalPages());
            result.put("current", page);
            result.put("size", size);
            
            return Result.ok("获取卖家订单列表成功", result);
            
        } catch (Exception e) {
            log.error("获取卖家订单列表失败", e);
            return Result.fail("获取卖家订单列表失败：" + e.getMessage());
        }
    }

    // ========== 通用功能 ==========
    @Override
    public Result getOrderDetail(String orderId) {
        try {
            log.info("获取订单详情 - orderId: {}", orderId);
            
            // 获取当前用户
            User currentUser = UserHolder.getUser();
            if (currentUser == null) {
                return Result.fail("用户未登录");
            }
            
            // 查找订单
            Optional<Order> orderOpt = orderRepository.findById(orderId);
            if (orderOpt.isEmpty()) {
                return Result.fail("订单不存在");
            }
            
            Order order = orderOpt.get();
            
            // 检查权限：只有买家和卖家可以查看订单详情
            if (!order.getBuyerId().equals(currentUser.getUserId()) && 
                !order.getSellerId().equals(currentUser.getUserId())) {
                return Result.fail("无权限查看此订单");
            }
            
            // 转换为DTO
            OrderDTO orderDTO = convertToDTO(order);
            
            return Result.ok("获取订单详情成功");
            
        } catch (Exception e) {
            log.error("获取订单详情失败", e);
            return Result.fail("获取订单详情失败：" + e.getMessage());
        }
    }

    // ========== 私有辅助方法 ==========
    
    /**
     * 将订单实体转换为DTO
     */
    private OrderDTO convertToDTO(Order order) {
        OrderDTO dto = new OrderDTO();
        dto.setOrderId(order.getOrderId());
        dto.setBuyerId(order.getBuyerId());
        dto.setSellerId(order.getSellerId());
        dto.setCommodityId(order.getCommodityId());
        dto.setOrderStatus(order.getOrderStatus());
        dto.setSellerVisibility(order.getSellerVisibility());
        dto.setBuyerVisibility(order.getBuyerVisibility());
        dto.setPayAmount(order.getPayAmount());
        dto.setQuantity(order.getQuantity());
        dto.setShippingAddress(order.getShippingAddress());
        dto.setTrackingNumber(order.getTrackingNumber());
        dto.setRemark(order.getRemark());
        
        // 退货相关字段
        dto.setReturnReason(order.getReturnReason());
        dto.setReturnRequestTime(order.getReturnRequestTime() != null ? order.getReturnRequestTime().toString() : null);
        dto.setReturnApprovalTime(order.getReturnApprovalTime() != null ? order.getReturnApprovalTime().toString() : null);
        dto.setReturnRejectionReason(order.getReturnRejectionReason());
        dto.setReturnTrackingNumber(order.getReturnTrackingNumber());
        dto.setReturnCompletionTime(order.getReturnCompletionTime() != null ? order.getReturnCompletionTime().toString() : null);
        
        return dto;
    }

    @Override
    @Transactional
    public Result updateOrderVisibility(String orderId, String visibility) {
        try {
            log.info("修改订单可见性 - orderId: {}, visibility: {}", orderId, visibility);
            
            // 获取当前用户
            User currentUser = UserHolder.getUser();
            if (currentUser == null) {
                return Result.fail("用户未登录");
            }
            
            // 查找订单
            Optional<Order> orderOpt = orderRepository.findById(orderId);
            if (orderOpt.isEmpty()) {
                return Result.fail("订单不存在");
            }
            
            Order order = orderOpt.get();
            
            // 检查权限：只有买家和卖家可以修改订单可见性
            if (!order.getBuyerId().equals(currentUser.getUserId()) && 
                !order.getSellerId().equals(currentUser.getUserId())) {
                return Result.fail("无权限修改此订单的可见性");
            }
            
            // 检查订单状态：只有未完成的订单可以修改可见性
            if (!order.canModifyVisibility()) {
                return Result.fail("订单状态不允许修改可见性");
            }
            
            // 验证可见性值
            if (!"PUBLIC".equals(visibility) && !"PRIVATE".equals(visibility) && !"HIDDEN".equals(visibility)) {
                return Result.fail("无效的可见性值");
            }
            
            // 修改可见性（同时设置卖家和买家可见性）
            if (order.setVisibility(visibility)) {
                orderRepository.save(order);
                log.info("订单可见性修改成功 - orderId: {}, visibility: {}", orderId, visibility);
                return Result.ok("订单可见性修改成功");
            } else {
                return Result.fail("订单可见性修改失败");
            }
            
        } catch (Exception e) {
            log.error("修改订单可见性失败", e);
            return Result.fail("修改订单可见性失败：" + e.getMessage());
        }
    }

    @Override
    @Transactional
    public Result updateOrderSellerVisibility(String orderId, String sellerVisibility) {
        try {
            log.info("修改订单卖家可见性 - orderId: {}, sellerVisibility: {}", orderId, sellerVisibility);
            
            // 获取当前用户
            User currentUser = UserHolder.getUser();
            if (currentUser == null) {
                return Result.fail("用户未登录");
            }
            
            // 查找订单
            Optional<Order> orderOpt = orderRepository.findById(orderId);
            if (orderOpt.isEmpty()) {
                return Result.fail("订单不存在");
            }
            
            Order order = orderOpt.get();
            
            // 检查权限：只有卖家可以修改卖家可见性
            if (!order.getSellerId().equals(currentUser.getUserId())) {
                return Result.fail("无权限修改此订单的卖家可见性");
            }
            
            // 检查订单状态：只有未完成的订单可以修改可见性
            if (!order.canModifyVisibility()) {
                return Result.fail("订单状态不允许修改可见性");
            }
            
            // 验证可见性值
            if (!"PUBLIC".equals(sellerVisibility) && !"PRIVATE".equals(sellerVisibility) && !"HIDDEN".equals(sellerVisibility)) {
                return Result.fail("无效的卖家可见性值");
            }
            
            // 修改卖家可见性
            if (order.setSellerVisibility(sellerVisibility)) {
                orderRepository.save(order);
                log.info("订单卖家可见性修改成功 - orderId: {}, sellerVisibility: {}", orderId, sellerVisibility);
                return Result.ok("订单卖家可见性修改成功");
            } else {
                return Result.fail("订单卖家可见性修改失败");
            }
            
        } catch (Exception e) {
            log.error("修改订单卖家可见性失败", e);
            return Result.fail("修改订单卖家可见性失败：" + e.getMessage());
        }
    }

    @Override
    @Transactional
    public Result updateOrderBuyerVisibility(String orderId, String buyerVisibility) {
        try {
            log.info("修改订单买家可见性 - orderId: {}, buyerVisibility: {}", orderId, buyerVisibility);
            
            // 获取当前用户
            User currentUser = UserHolder.getUser();
            if (currentUser == null) {
                return Result.fail("用户未登录");
            }
            
            // 查找订单
            Optional<Order> orderOpt = orderRepository.findById(orderId);
            if (orderOpt.isEmpty()) {
                return Result.fail("订单不存在");
            }
            
            Order order = orderOpt.get();
            
            // 检查权限：只有买家可以修改买家可见性
            if (!order.getBuyerId().equals(currentUser.getUserId())) {
                return Result.fail("无权限修改此订单的买家可见性");
            }
            
            // 检查订单状态：只有未完成的订单可以修改可见性
            if (!order.canModifyVisibility()) {
                return Result.fail("订单状态不允许修改可见性");
            }
            
            // 验证可见性值
            if (!"PUBLIC".equals(buyerVisibility) && !"PRIVATE".equals(buyerVisibility) && !"HIDDEN".equals(buyerVisibility)) {
                return Result.fail("无效的买家可见性值");
            }
            
            // 修改买家可见性
            if (order.setBuyerVisibility(buyerVisibility)) {
                orderRepository.save(order);
                log.info("订单买家可见性修改成功 - orderId: {}, buyerVisibility: {}", orderId, buyerVisibility);
                return Result.ok("订单买家可见性修改成功");
            } else {
                return Result.fail("订单买家可见性修改失败");
            }
            
        } catch (Exception e) {
            log.error("修改订单买家可见性失败", e);
            return Result.fail("修改订单买家可见性失败：" + e.getMessage());
        }
    }

    @Override
    @Transactional
    public Result requestReturn(String orderId, String returnReason) {
        try {
            log.info("申请退货 - orderId: {}, returnReason: {}", orderId, returnReason);
            
            // 获取当前用户
            User currentUser = UserHolder.getUser();
            if (currentUser == null) {
                return Result.fail("用户未登录");
            }
            
            // 查找订单
            Optional<Order> orderOpt = orderRepository.findById(orderId);
            if (orderOpt.isEmpty()) {
                return Result.fail("订单不存在");
            }
            
            Order order = orderOpt.get();
            
            // 检查权限：只有买家可以申请退货
            if (!order.getBuyerId().equals(currentUser.getUserId())) {
                return Result.fail("无权限申请此订单的退货");
            }
            
            // 检查是否可以申请退货
            if (!order.canRequestReturn()) {
                return Result.fail("订单状态不允许申请退货");
            }
            
            // 验证退货原因
            if (!StringUtils.hasText(returnReason)) {
                return Result.fail("退货原因不能为空");
            }
            
            // 申请退货
            if (order.requestReturn(returnReason)) {
                orderRepository.save(order);
                log.info("退货申请成功 - orderId: {}", orderId);
                return Result.ok("退货申请成功");
            } else {
                return Result.fail("退货申请失败");
            }
            
        } catch (Exception e) {
            log.error("申请退货失败", e);
            return Result.fail("申请退货失败：" + e.getMessage());
        }
    }

    @Override
    @Transactional
    public Result approveReturnRequest(String orderId, Boolean approved, String rejectionReason) {
        try {
            log.info("审批退货申请 - orderId: {}, approved: {}, rejectionReason: {}", orderId, approved, rejectionReason);
            
            // 获取当前用户
            User currentUser = UserHolder.getUser();
            if (currentUser == null) {
                return Result.fail("用户未登录");
            }
            
            // 查找订单
            Optional<Order> orderOpt = orderRepository.findById(orderId);
            if (orderOpt.isEmpty()) {
                return Result.fail("订单不存在");
            }
            
            Order order = orderOpt.get();
            
            // 检查权限：只有卖家可以审批退货
            if (!order.getSellerId().equals(currentUser.getUserId())) {
                return Result.fail("无权限审批此订单的退货申请");
            }
            
            // 检查是否可以审批退货
            if (!order.canApproveReturn()) {
                return Result.fail("订单状态不允许审批退货");
            }
            
            // 如果拒绝，验证拒绝原因
            if (!approved && !StringUtils.hasText(rejectionReason)) {
                return Result.fail("拒绝退货时必须提供拒绝原因");
            }
            
            // 审批退货申请
            if (order.approveReturnRequest(approved, rejectionReason)) {
                orderRepository.save(order);
                log.info("退货审批成功 - orderId: {}, approved: {}", orderId, approved);
                return Result.ok(approved ? "退货申请已同意" : "退货申请已拒绝");
            } else {
                return Result.fail("退货审批失败");
            }
            
        } catch (Exception e) {
            log.error("审批退货申请失败", e);
            return Result.fail("审批退货申请失败：" + e.getMessage());
        }
    }

    @Override
    @Transactional
    public Result confirmReturnShipment(String orderId, String returnTrackingNumber) {
        try {
            log.info("确认退货发货 - orderId: {}, returnTrackingNumber: {}", orderId, returnTrackingNumber);
            
            // 获取当前用户
            User currentUser = UserHolder.getUser();
            if (currentUser == null) {
                return Result.fail("用户未登录");
            }
            
            // 查找订单
            Optional<Order> orderOpt = orderRepository.findById(orderId);
            if (orderOpt.isEmpty()) {
                return Result.fail("订单不存在");
            }
            
            Order order = orderOpt.get();
            
            // 检查权限：只有买家可以确认退货发货
            if (!order.getBuyerId().equals(currentUser.getUserId())) {
                return Result.fail("无权限确认此订单的退货发货");
            }
            
            // 检查是否可以确认退货发货
            if (!order.canConfirmReturnShipment()) {
                return Result.fail("订单状态不允许确认退货发货");
            }
            
            // 验证快递单号
            if (!StringUtils.hasText(returnTrackingNumber)) {
                return Result.fail("退货快递单号不能为空");
            }
            
            // 确认退货发货
            if (order.confirmReturnShipment(returnTrackingNumber)) {
                orderRepository.save(order);
                log.info("退货发货确认成功 - orderId: {}", orderId);
                return Result.ok("退货发货确认成功");
            } else {
                return Result.fail("退货发货确认失败");
            }
            
        } catch (Exception e) {
            log.error("确认退货发货失败", e);
            return Result.fail("确认退货发货失败：" + e.getMessage());
        }
    }

    @Override
    @Transactional
    public Result completeReturn(String orderId) {
        try {
            log.info("完成退货 - orderId: {}", orderId);
            
            // 获取当前用户
            User currentUser = UserHolder.getUser();
            if (currentUser == null) {
                return Result.fail("用户未登录");
            }
            
            // 查找订单
            Optional<Order> orderOpt = orderRepository.findById(orderId);
            if (orderOpt.isEmpty()) {
                return Result.fail("订单不存在");
            }
            
            Order order = orderOpt.get();
            
            // 检查权限：只有卖家可以完成退货
            if (!order.getSellerId().equals(currentUser.getUserId())) {
                return Result.fail("无权限完成此订单的退货");
            }
            
            // 检查是否可以完成退货
            if (!order.canCompleteReturn()) {
                return Result.fail("订单状态不允许完成退货");
            }
            
            // 完成退货
            if (order.completeReturn()) {
                orderRepository.save(order);
                log.info("退货完成成功 - orderId: {}", orderId);
                return Result.ok("退货完成成功");
            } else {
                return Result.fail("退货完成失败");
            }
            
        } catch (Exception e) {
            log.error("完成退货失败", e);
            return Result.fail("完成退货失败：" + e.getMessage());
        }
    }

    @Override
    public Result getReturnRequests(Integer page, Integer size, String status) {
        try {
            log.info("获取退货申请列表 - page: {}, size: {}, status: {}", page, size, status);
            
            // 获取当前用户
            User currentUser = UserHolder.getUser();
            if (currentUser == null) {
                return Result.fail("用户未登录");
            }
            
            // 构建分页参数
            Pageable pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "returnRequestTime"));
            
            // 查询退货申请
            Page<Order> orderPage;
            if (StringUtils.hasText(status)) {
                orderPage = orderRepository.findBySellerIdAndOrderStatus(currentUser.getUserId(), status, pageable);
            } else {
                // 查询所有退货相关状态的订单
                List<String> returnStatuses = Arrays.asList("RETURN_REQUESTED", "RETURN_APPROVED", "RETURN_REJECTED", "RETURN_COMPLETED");
                orderPage = orderRepository.findBySellerIdAndOrderStatusIn(currentUser.getUserId(), returnStatuses, pageable);
            }
            
            // 转换为DTO
            List<OrderDTO> orderDTOs = orderPage.getContent().stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());
            
            Map<String, Object> result = new HashMap<>();
            result.put("orders", orderDTOs);
            result.put("total", orderPage.getTotalElements());
            result.put("pages", orderPage.getTotalPages());
            result.put("current", page);
            result.put("size", size);
            
            log.info("获取退货申请列表成功 - 总数: {}", orderPage.getTotalElements());
            return Result.ok("获取退货申请列表成功", result);
            
        } catch (Exception e) {
            log.error("获取退货申请列表失败", e);
            return Result.fail("获取退货申请列表失败：" + e.getMessage());
        }
    }

    @Override
    public Result getMyReturnRecords(Integer page, Integer size, String status) {
        try {
            log.info("获取我的退货记录 - page: {}, size: {}, status: {}", page, size, status);
            
            // 获取当前用户
            User currentUser = UserHolder.getUser();
            if (currentUser == null) {
                return Result.fail("用户未登录");
            }
            
            // 构建分页参数
            Pageable pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "returnRequestTime"));
            
            // 查询退货记录
            Page<Order> orderPage;
            if (StringUtils.hasText(status)) {
                orderPage = orderRepository.findByBuyerIdAndOrderStatus(currentUser.getUserId(), status, pageable);
            } else {
                // 查询所有退货相关状态的订单
                List<String> returnStatuses = Arrays.asList("RETURN_REQUESTED", "RETURN_APPROVED", "RETURN_REJECTED", "RETURN_COMPLETED");
                orderPage = orderRepository.findByBuyerIdAndOrderStatusIn(currentUser.getUserId(), returnStatuses, pageable);
            }
            
            // 转换为DTO
            List<OrderDTO> orderDTOs = orderPage.getContent().stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());
            
            Map<String, Object> result = new HashMap<>();
            result.put("orders", orderDTOs);
            result.put("total", orderPage.getTotalElements());
            result.put("pages", orderPage.getTotalPages());
            result.put("current", page);
            result.put("size", size);
            
            log.info("获取我的退货记录成功 - 总数: {}", orderPage.getTotalElements());
            return Result.ok("获取我的退货记录成功", result);
            
        } catch (Exception e) {
            log.error("获取我的退货记录失败", e);
            return Result.fail("获取我的退货记录失败：" + e.getMessage());
        }
    }

    // ========== 内部方法 ==========
    @Override
    public Result completeOrder(String orderId) {
        log.info("完成订单 - orderId: {}", orderId);
        return Result.ok("完成订单成功");
    }

    @Override
    public Result rateOrder(String orderId, Integer rating, String comment) {
        log.info("评价订单 - orderId: {}, rating: {}, comment: {}", orderId, rating, comment);
        return Result.ok("评价订单成功");
    }

    @Override
    public Result requestReturn(String orderId) {
        log.info("申请退货 - orderId: {}", orderId);
        return Result.ok("申请退货成功");
    }

    @Override
    public Integer calcValidVolume(String userId) {
        log.info("计算有效交易量 - userId: {}", userId);
        return 0;
    }

    @Override
    public Result getOrderHistory(String userId) {
        log.info("获取订单历史 - userId: {}", userId);
        return Result.ok("获取订单历史成功");
    }
}