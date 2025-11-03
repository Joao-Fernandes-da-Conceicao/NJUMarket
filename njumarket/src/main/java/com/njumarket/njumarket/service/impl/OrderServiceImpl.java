package com.njumarket.njumarket.service.impl;

import com.njumarket.njumarket.dto.Result;
import com.njumarket.njumarket.dto.OrderDTO;
import com.njumarket.njumarket.entity.Order;
import com.njumarket.njumarket.entity.Commodity;
import com.njumarket.njumarket.entity.User;
import com.njumarket.njumarket.entity.UserProfile;
import com.njumarket.njumarket.repository.OrderRepository;
import com.njumarket.njumarket.repository.CommodityRepository;
import com.njumarket.njumarket.repository.UserRepository;
import com.njumarket.njumarket.repository.UserProfileRepository;
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
    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final com.njumarket.njumarket.service.WebSocketRetryService webSocketRetryService;

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
            
            // 创建商品快照 - 获取卖家信息
            Optional<User> sellerOpt = userRepository.findById(commodity.getSellerId());
            if (sellerOpt.isPresent()) {
                order.createCommoditySnapshot(commodity, sellerOpt.get());
            } else {
                log.warn("卖家不存在，无法创建商品快照 - sellerId: {}", commodity.getSellerId());
            }
            
            // 保存订单
            orderRepository.save(order);
            
            // 减少商品库存
            commodity.updateStock(-orderDTO.getQuantity());
            commodityRepository.save(commodity);
            
            // ✅ WebSocket 推送：订单创建通知给卖家（发送完整OrderDTO，类似消息发送完整MessageDTO）
            OrderDTO orderDTOForNotification = convertToDTO(order);
            pushOrderChangeNotificationWithDTO(order.getSellerId(), order.getOrderId(), "ORDER_CREATED", order.getOrderStatus(), "SELLER", orderDTOForNotification);
            
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
                
                // ✅ WebSocket 推送：订单支付通知给卖家
                pushOrderChangeNotification(order.getSellerId(), order.getOrderId(), "ORDER_PAID", order.getOrderStatus(), "SELLER");
                
                log.info("订单支付成功 - orderId: {}, payTime: {}", orderId, order.getPayTime());
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
                
                // ✅ WebSocket 推送：订单完成通知给卖家
                pushOrderChangeNotification(order.getSellerId(), order.getOrderId(), "ORDER_COMPLETED", order.getOrderStatus(), "SELLER");
                
                log.info("订单确认收货成功 - orderId: {}, deliveryTime: {}", 
                    orderId, order.getDeliveryTime());
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
            
            // 在取消订单前检查是否可以恢复库存
            boolean shouldRestoreStock = order.canRestoreStock();
            
            // 取消订单
            if (order.cancelOrder()) {
                // 只有未发货和未付款的订单取消时才恢复库存
                if (shouldRestoreStock) {
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
                } else {
                    log.info("订单取消成功，但订单状态不允许恢复库存 - orderId: {}, orderStatus: {}", 
                        orderId, order.getOrderStatus());
                }
                
                orderRepository.save(order);
                
                // ✅ WebSocket 推送：订单取消通知
                // 如果是买家取消，通知卖家；如果是卖家取消，通知买家
                if (order.getBuyerId().equals(currentUser.getUserId())) {
                    pushOrderChangeNotification(order.getSellerId(), order.getOrderId(), "ORDER_CANCELLED", order.getOrderStatus(), "SELLER");
                } else {
                    pushOrderChangeNotification(order.getBuyerId(), order.getOrderId(), "ORDER_CANCELLED", order.getOrderStatus(), "BUYER");
                }
                
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
            
            // ✅ 设置卖家可见性为可见，确保卖家能够看到退款申请（自动恢复可见性）
            // 如果订单之前被卖家软删除（HIDDEN），此时会自动恢复可见性
            // 注意：无论订单之前是从COMPLETED还是REFUND_REJECTED状态申请退款，都需要恢复可见性
            boolean sellerVisibilityRestored = "HIDDEN".equals(order.getSellerVisibility());
            order.setSellerVisibility("PUBLIC");
            
            orderRepository.save(order);
            
            // ✅ WebSocket 推送：退款申请通知给卖家
            pushOrderChangeNotification(order.getSellerId(), order.getOrderId(), "REFUND_REQUESTED", order.getOrderStatus(), "SELLER");
            
            // ✅ 如果是恢复可见性（极端情况），推送完整的OrderDTO（直接更新，无需刷新）
            // 这种情况可能发生在：
            // 1. 从COMPLETED状态申请退款（卖家之前软删除了订单）
            // 2. 从REFUND_REJECTED状态重新申请退款（卖家之前软删除了订单）
            if (sellerVisibilityRestored) {
                OrderDTO orderDTOForRestored = convertToDTO(order);
                pushOrderChangeNotificationWithDTO(order.getSellerId(), order.getOrderId(), "ORDER_VISIBILITY_RESTORED", order.getOrderStatus(), "SELLER", orderDTOForRestored);
            }
            
            log.info("退款/退货申请成功 - orderId: {}, sellerVisibilityRestored: {}", 
                    orderId, sellerVisibilityRestored);
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
            
            // 过滤掉买家不可见的订单（HIDDEN状态的订单）
            List<Order> visibleOrders = orderPage.getContent().stream()
                    .filter(order -> !"HIDDEN".equals(order.getBuyerVisibility()))
                    .collect(Collectors.toList());
            
            // 转换为DTO
            List<OrderDTO> orderDTOs = visibleOrders.stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());
            
            Map<String, Object> result = new HashMap<>();
            result.put("orders", orderDTOs);
            result.put("total", (long) visibleOrders.size()); // 使用过滤后的总数
            result.put("pages", (int) Math.ceil((double) visibleOrders.size() / size));
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
                
                // ✅ WebSocket 推送：订单发货通知给买家
                pushOrderChangeNotification(order.getBuyerId(), order.getOrderId(), "ORDER_SHIPPED", order.getOrderStatus(), "BUYER");
                
                log.info("订单发货成功 - orderId: {}, shippingTime: {}, trackingNumber: {}", 
                    orderId, order.getShippingTime(), trackingNumber);
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
            // ✅ 在修改前检测是否恢复可见性
            boolean buyerVisibilityRestored = "HIDDEN".equals(order.getBuyerVisibility());
            
            if ("APPROVE".equals(decision)) {
                // 同意退款/退货
                order.setOrderStatus("REFUND_APPROVED");
                order.setReturnApprovalTime(LocalDateTime.now());
                
                // ✅ 设置买家可见性为可见，确保买家能够看到处理结果（自动恢复可见性）
                order.setBuyerVisibility("PUBLIC");
                
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
                
                log.info("退款/退货申请已同意 - orderId: {}, buyerVisibilityRestored: {}", orderId, buyerVisibilityRestored);
            } else if ("REJECT".equals(decision)) {
                // 拒绝退款/退货
                order.setOrderStatus("REFUND_REJECTED");
                order.setReturnRejectionReason(remark);
                order.setReturnApprovalTime(LocalDateTime.now());
                
                // ✅ 设置买家可见性为可见，确保买家能够看到处理结果（自动恢复可见性）
                order.setBuyerVisibility("PUBLIC");
                
                log.info("退款/退货申请已拒绝 - orderId: {}, buyerVisibilityRestored: {}", orderId, buyerVisibilityRestored);
            } else {
                return Result.fail("无效的处理决定");
            }
            
            orderRepository.save(order);
            
            // ✅ WebSocket 推送：退款处理结果通知给买家
            String notificationType = "APPROVE".equals(decision) ? "REFUND_APPROVED" : "REFUND_REJECTED";
            pushOrderChangeNotification(order.getBuyerId(), order.getOrderId(), notificationType, order.getOrderStatus(), "BUYER");
            
            // ✅ 如果是恢复可见性（极端情况），推送完整的OrderDTO（直接更新，无需刷新）
            if (buyerVisibilityRestored) {
                OrderDTO orderDTOForRestored = convertToDTO(order);
                pushOrderChangeNotificationWithDTO(order.getBuyerId(), order.getOrderId(), "ORDER_VISIBILITY_RESTORED", order.getOrderStatus(), "BUYER", orderDTOForRestored);
            }
            
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
            
            // 过滤掉卖家不可见的订单（HIDDEN状态的订单）
            List<Order> visibleOrders = orderPage.getContent().stream()
                    .filter(order -> !"HIDDEN".equals(order.getSellerVisibility()))
                    .collect(Collectors.toList());
            
            // 转换为DTO
            List<OrderDTO> orderDTOs = visibleOrders.stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());
            
            Map<String, Object> result = new HashMap<>();
            result.put("orders", orderDTOs);
            result.put("total", (long) visibleOrders.size()); // 使用过滤后的总数
            result.put("pages", (int) Math.ceil((double) visibleOrders.size() / size));
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
            
            // 查询卖家详细信息
            Optional<User> sellerOpt = userRepository.findById(order.getSellerId());
            if (sellerOpt.isPresent()) {
                User seller = sellerOpt.get();
                OrderDTO.SellerInfo sellerInfo = new OrderDTO.SellerInfo();
                sellerInfo.setUserId(seller.getUserId());
                sellerInfo.setUsername(seller.getUsername());
                
                // 查询卖家档案信息
                Optional<UserProfile> sellerProfileOpt = userProfileRepository.findByUserId(seller.getUserId());
                if (sellerProfileOpt.isPresent()) {
                    UserProfile sellerProfile = sellerProfileOpt.get();
                    sellerInfo.setNickname(sellerProfile.getNickname());
                    sellerInfo.setAvatar(sellerProfile.getAvatar());
                } else {
                    sellerInfo.setNickname(seller.getUsername());
                    sellerInfo.setAvatar(null);
                }
                
                sellerInfo.setPhone(seller.getPrimaryPhone());
                sellerInfo.setEmail(null); // 邮箱需要从contact_info表查询，这里暂时设为null
                sellerInfo.setIsDeleted("DELETED".equals(seller.getAccountStatus()));
                sellerInfo.setStatus(sellerInfo.getIsDeleted() ? "DELETED" : "ACTIVE");
                orderDTO.setSeller(sellerInfo);
            } else {
                // 卖家不存在，创建已注销的卖家信息
                OrderDTO.SellerInfo sellerInfo = new OrderDTO.SellerInfo();
                sellerInfo.setUserId(order.getSellerId());
                sellerInfo.setUsername("已注销用户");
                sellerInfo.setNickname("卖家已注销");
                sellerInfo.setAvatar(null);
                sellerInfo.setPhone(null);
                sellerInfo.setEmail(null);
                sellerInfo.setIsDeleted(true);
                sellerInfo.setStatus("DELETED");
                orderDTO.setSeller(sellerInfo);
            }
            
            // 查询买家详细信息
            Optional<User> buyerOpt = userRepository.findById(order.getBuyerId());
            if (buyerOpt.isPresent()) {
                User buyer = buyerOpt.get();
                OrderDTO.BuyerInfo buyerInfo = new OrderDTO.BuyerInfo();
                buyerInfo.setUserId(buyer.getUserId());
                buyerInfo.setUsername(buyer.getUsername());
                
                // 查询买家档案信息
                Optional<UserProfile> buyerProfileOpt = userProfileRepository.findByUserId(buyer.getUserId());
                if (buyerProfileOpt.isPresent()) {
                    UserProfile buyerProfile = buyerProfileOpt.get();
                    buyerInfo.setNickname(buyerProfile.getNickname());
                    buyerInfo.setAvatar(buyerProfile.getAvatar());
                } else {
                    buyerInfo.setNickname(buyer.getUsername());
                    buyerInfo.setAvatar(null);
                }
                
                buyerInfo.setPhone(buyer.getPrimaryPhone());
                buyerInfo.setEmail(null); // 邮箱需要从contact_info表查询，这里暂时设为null
                buyerInfo.setIsDeleted("DELETED".equals(buyer.getAccountStatus()));
                buyerInfo.setStatus(buyerInfo.getIsDeleted() ? "DELETED" : "ACTIVE");
                orderDTO.setBuyer(buyerInfo);
            } else {
                // 买家不存在，创建已注销的买家信息
                OrderDTO.BuyerInfo buyerInfo = new OrderDTO.BuyerInfo();
                buyerInfo.setUserId(order.getBuyerId());
                buyerInfo.setUsername("已注销用户");
                buyerInfo.setNickname("买家已注销");
                buyerInfo.setAvatar(null);
                buyerInfo.setPhone(null);
                buyerInfo.setEmail(null);
                buyerInfo.setIsDeleted(true);
                buyerInfo.setStatus("DELETED");
                orderDTO.setBuyer(buyerInfo);
            }
            
            return Result.ok("获取订单详情成功", orderDTO);
            
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
        dto.setCreateTime(order.getCreateTime() != null ? order.getCreateTime().toString() : null);
        dto.setPayTime(order.getPayTime() != null ? order.getPayTime().toString() : null);
        dto.setShippingTime(order.getShippingTime() != null ? order.getShippingTime().toString() : null);
        dto.setDeliveryTime(order.getDeliveryTime() != null ? order.getDeliveryTime().toString() : null);
        
        // 退货相关字段
        dto.setReturnReason(order.getReturnReason());
        dto.setReturnRequestTime(order.getReturnRequestTime() != null ? order.getReturnRequestTime().toString() : null);
        dto.setReturnApprovalTime(order.getReturnApprovalTime() != null ? order.getReturnApprovalTime().toString() : null);
        dto.setReturnRejectionReason(order.getReturnRejectionReason());
        dto.setReturnTrackingNumber(order.getReturnTrackingNumber());
        dto.setReturnCompletionTime(order.getReturnCompletionTime() != null ? order.getReturnCompletionTime().toString() : null);
        
        // 商品快照字段
        dto.setCommoditySnapshotTitle(order.getCommoditySnapshotTitle());
        dto.setCommoditySnapshotDescription(order.getCommoditySnapshotDescription());
        dto.setCommoditySnapshotPrice(order.getCommoditySnapshotPrice());
        dto.setCommoditySnapshotLocation(order.getCommoditySnapshotLocation());
        dto.setCommoditySnapshotCategory(order.getCommoditySnapshotCategory());
        dto.setCommoditySnapshotConditionLevel(order.getCommoditySnapshotConditionLevel());
        dto.setCommoditySnapshotImages(order.getCommoditySnapshotImages());
        dto.setCommoditySnapshotStatus(order.getCommoditySnapshotStatus());
        dto.setCommoditySnapshotSellerName(order.getCommoditySnapshotSellerName());
        dto.setCommoditySnapshotSellerPhone(order.getCommoditySnapshotSellerPhone());
        dto.setCommoditySnapshotSellerEmail(order.getCommoditySnapshotSellerEmail());
        dto.setCommoditySnapshotTime(order.getCommoditySnapshotTime() != null ? order.getCommoditySnapshotTime().toString() : null);
        
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
    public Result queryOriginalCommodity(String orderId) {
        try {
            log.info("查询原商品信息 - orderId: {}", orderId);
            
            // 获取当前用户
            User currentUser = UserHolder.getUser();
            if (currentUser == null) {
                return Result.fail("用户未登录");
            }
            
            // 查找原订单
            Optional<Order> orderOpt = orderRepository.findById(orderId);
            if (orderOpt.isEmpty()) {
                return Result.fail("原订单不存在");
            }
            
            Order originalOrder = orderOpt.get();
            
            // 检查权限：只有买家可以查询
            if (!originalOrder.getBuyerId().equals(currentUser.getUserId())) {
                return Result.fail("无权限操作此订单");
            }
            
            // 检查商品快照是否存在
            if (originalOrder.getCommoditySnapshotTitle() == null) {
                return Result.fail("商品快照信息不存在");
            }
            
            // 构建商品快照信息
            Map<String, Object> commodityInfo = new HashMap<>();
            commodityInfo.put("commodityId", originalOrder.getCommodityId());
            commodityInfo.put("title", originalOrder.getCommoditySnapshotTitle());
            commodityInfo.put("description", originalOrder.getCommoditySnapshotDescription());
            commodityInfo.put("price", originalOrder.getCommoditySnapshotPrice());
            commodityInfo.put("location", originalOrder.getCommoditySnapshotLocation());
            commodityInfo.put("category", originalOrder.getCommoditySnapshotCategory());
            commodityInfo.put("conditionLevel", originalOrder.getCommoditySnapshotConditionLevel());
            commodityInfo.put("images", originalOrder.getCommoditySnapshotImages());
            commodityInfo.put("status", originalOrder.getCommoditySnapshotStatus());
            commodityInfo.put("sellerName", originalOrder.getCommoditySnapshotSellerName());
            commodityInfo.put("sellerPhone", originalOrder.getCommoditySnapshotSellerPhone());
            commodityInfo.put("sellerEmail", originalOrder.getCommoditySnapshotSellerEmail());
            commodityInfo.put("snapshotTime", originalOrder.getCommoditySnapshotTime());
            
            // 检查当前商品状态
            Optional<Commodity> commodityOpt = commodityRepository.findById(originalOrder.getCommodityId());
            boolean commodityExists = commodityOpt.isPresent();
            boolean commodityOnShelf = false;
            int currentStock = 0;
            double currentPrice = originalOrder.getCommoditySnapshotPrice();
            
            if (commodityExists) {
                Commodity commodity = commodityOpt.get();
                commodityOnShelf = "ON_SHELF".equals(commodity.getCommodityStatus());
                currentStock = commodity.getStock();
                currentPrice = commodity.getPrice();
            }
            
            Map<String, Object> result = new HashMap<>();
            result.put("commoditySnapshot", commodityInfo);
            result.put("commodityExists", commodityExists);
            result.put("commodityOnShelf", commodityOnShelf);
            result.put("currentStock", currentStock);
            result.put("currentPrice", currentPrice);
            result.put("isOffShelf", originalOrder.isCommoditySnapshotOffShelf());
            
            // 设置状态消息
            String statusMessage;
            if (!commodityExists) {
                statusMessage = "商品已被删除或不存在";
            } else if (!commodityOnShelf) {
                statusMessage = "商品已下架";
            } else {
                statusMessage = "商品正常可购买";
            }
            result.put("statusMessage", statusMessage);
            
            log.info("查询原商品信息成功 - orderId: {}, commodityExists: {}, commodityOnShelf: {}", 
                orderId, commodityExists, commodityOnShelf);
            return Result.ok("查询原商品信息成功", result);
            
        } catch (Exception e) {
            log.error("查询原商品信息失败", e);
            return Result.fail("查询原商品信息失败：" + e.getMessage());
        }
    }
    
    @Override
    public Result createOrderFromSnapshot(String orderId, Map<String, Object> orderData) {
        try {
            log.info("基于快照创建新订单 - orderId: {}", orderId);
            
            // 获取当前用户
            User currentUser = UserHolder.getUser();
            if (currentUser == null) {
                return Result.fail("用户未登录");
            }
            
            // 查找原订单
            Optional<Order> orderOpt = orderRepository.findById(orderId);
            if (orderOpt.isEmpty()) {
                return Result.fail("原订单不存在");
            }
            
            Order originalOrder = orderOpt.get();
            
            // 检查权限：只有买家可以创建订单
            if (!originalOrder.getBuyerId().equals(currentUser.getUserId())) {
                return Result.fail("无权限操作此订单");
            }
            
            // 检查商品快照是否存在
            if (originalOrder.getCommoditySnapshotTitle() == null) {
                return Result.fail("商品快照信息不存在");
            }
            
            // 检查当前商品状态
            Optional<Commodity> commodityOpt = commodityRepository.findById(originalOrder.getCommodityId());
            if (commodityOpt.isEmpty()) {
                return Result.fail("商品不存在，无法下单");
            }
            
            Commodity commodity = commodityOpt.get();
            
            // 检查商品当前状态
            if (!"ON_SHELF".equals(commodity.getCommodityStatus())) {
                return Result.fail("商品当前未上架，无法下单");
            }
            
            // 从请求数据中获取用户修改的信息
            Integer quantity = (Integer) orderData.get("quantity");
            String shippingAddress = (String) orderData.get("shippingAddress");
            String remark = (String) orderData.get("remark");
            
            // 验证数量
            if (quantity == null || quantity <= 0) {
                return Result.fail("购买数量必须大于0");
            }
            
            // 检查库存
            if (commodity.getStock() < quantity) {
                return Result.fail("商品库存不足，当前库存：" + commodity.getStock());
            }
            
            // 检查是否购买自己的商品
            if (commodity.getSellerId().equals(currentUser.getUserId())) {
                return Result.fail("不能购买自己的商品");
            }
            
            // 计算价格
            double payAmount = commodity.getPrice() * quantity;
            
            // 创建新订单
            Order newOrder = new Order();
            newOrder.setOrderId(UUID.randomUUID().toString().replace("-", ""));
            newOrder.setBuyerId(currentUser.getUserId());
            newOrder.setSellerId(commodity.getSellerId());
            newOrder.setCommodityId(commodity.getCommodityId());
            newOrder.setOrderStatus("CREATED");
            newOrder.setSellerVisibility(originalOrder.getSellerVisibility());
            newOrder.setBuyerVisibility(originalOrder.getBuyerVisibility());
            newOrder.setPayAmount(payAmount);
            newOrder.setQuantity(quantity);
            newOrder.setShippingAddress(shippingAddress != null ? shippingAddress : originalOrder.getShippingAddress());
            newOrder.setRemark(remark != null ? remark : "基于订单快照创建: " + orderId);
            newOrder.setCreateTime(LocalDateTime.now());
            
            // 创建商品快照
            Optional<User> sellerOpt = userRepository.findById(commodity.getSellerId());
            if (sellerOpt.isPresent()) {
                newOrder.createCommoditySnapshot(commodity, sellerOpt.get());
            }
            
            // 保存订单
            orderRepository.save(newOrder);
            
            // 减少商品库存
            commodity.updateStock(-quantity);
            commodityRepository.save(commodity);
            
            log.info("基于快照创建新订单成功 - 原订单: {}, 新订单: {}", orderId, newOrder.getOrderId());
            return Result.ok("创建新订单成功", convertToDTO(newOrder));
            
        } catch (Exception e) {
            log.error("基于快照创建新订单失败", e);
            return Result.fail("创建新订单失败：" + e.getMessage());
        }
    }

    @Override
    public Result getOrderHistory(String userId) {
        log.info("获取订单历史 - userId: {}", userId);
        return Result.ok("获取订单历史成功");
    }
    
    // ========== 批量查询（用于聊天界面） ==========
    
    @Override
    public Result getOrdersBatchStatus(List<String> orderIds) {
        try {
            if (orderIds == null || orderIds.isEmpty()) {
                return Result.ok("批量查询成功", Collections.emptyList());
            }
            
            // 获取当前用户（用于权限检查）
            User currentUser = UserHolder.getUser();
            if (currentUser == null) {
                return Result.fail("用户未登录");
            }
            
            // 去重
            Set<String> uniqueIds = new HashSet<>(orderIds);
            
            // 批量查询订单
            List<Order> orders = orderRepository.findAllById(uniqueIds);
            
            // 转换为轻量级DTO（只包含基本信息，并检查权限）
            List<Map<String, Object>> result = orders.stream()
                .filter(order -> {
                    // 权限检查：订单必须属于当前用户（买家或卖家）
                    return order.getBuyerId().equals(currentUser.getUserId()) 
                        || order.getSellerId().equals(currentUser.getUserId());
                })
                .map(order -> {
                    Map<String, Object> item = new HashMap<>();
                    item.put("orderId", order.getOrderId());
                    item.put("orderStatus", order.getOrderStatus());
                    item.put("payAmount", order.getPayAmount());
                    item.put("quantity", order.getQuantity());
                    item.put("commodityId", order.getCommodityId());
                    item.put("buyerId", order.getBuyerId());
                    item.put("sellerId", order.getSellerId());
                    item.put("trackingNumber", order.getTrackingNumber());
                    item.put("createTime", order.getCreateTime() != null ? order.getCreateTime().toString() : null);
                    item.put("payTime", order.getPayTime() != null ? order.getPayTime().toString() : null);
                    item.put("shippingTime", order.getShippingTime() != null ? order.getShippingTime().toString() : null);
                    item.put("deliveryTime", order.getDeliveryTime() != null ? order.getDeliveryTime().toString() : null);
                    
                    // ✅ 添加商品快照字段（用于聊天界面显示）
                    item.put("commoditySnapshotTitle", order.getCommoditySnapshotTitle());
                    item.put("commoditySnapshotDescription", order.getCommoditySnapshotDescription());
                    item.put("commoditySnapshotPrice", order.getCommoditySnapshotPrice());
                    item.put("commoditySnapshotLocation", order.getCommoditySnapshotLocation());
                    item.put("commoditySnapshotCategory", order.getCommoditySnapshotCategory());
                    item.put("commoditySnapshotConditionLevel", order.getCommoditySnapshotConditionLevel());
                    item.put("commoditySnapshotImages", order.getCommoditySnapshotImages());
                    item.put("commoditySnapshotStatus", order.getCommoditySnapshotStatus());
                    item.put("commoditySnapshotSellerName", order.getCommoditySnapshotSellerName());
                    item.put("commoditySnapshotSellerPhone", order.getCommoditySnapshotSellerPhone());
                    item.put("commoditySnapshotSellerEmail", order.getCommoditySnapshotSellerEmail());
                    item.put("commoditySnapshotTime", order.getCommoditySnapshotTime() != null ? order.getCommoditySnapshotTime().toString() : null);
                    
                    return item;
                })
                .collect(Collectors.toList());
            
            log.info("批量查询订单状态成功 - 查询{}个，返回{}个", uniqueIds.size(), result.size());
            return Result.ok("批量查询成功", result);
            
        } catch (Exception e) {
            log.error("批量查询订单状态失败: {}", e.getMessage(), e);
            return Result.fail("批量查询失败");
        }
    }
    
    /**
     * 推送订单变化通知（轻量级，只包含基本信息）
     * @param userId 接收通知的用户ID
     * @param orderId 订单ID
     * @param changeType 变化类型
     * @param orderStatus 订单状态
     * @param targetRole 目标角色（"SELLER" 或 "BUYER"），用于前端判断显示哪个角标
     */
    private void pushOrderChangeNotification(String userId, String orderId, String changeType, String orderStatus, String targetRole) {
        pushOrderChangeNotificationWithDTO(userId, orderId, changeType, orderStatus, targetRole, null);
    }
    
    /**
     * 推送订单变化通知（支持发送完整OrderDTO）
     * @param userId 接收通知的用户ID
     * @param orderId 订单ID
     * @param changeType 变化类型（ORDER_CREATED, ORDER_PAID, ORDER_SHIPPED, ORDER_COMPLETED, ORDER_CANCELLED, REFUND_REQUESTED, REFUND_APPROVED, REFUND_REJECTED）
     * @param orderStatus 订单状态
     * @param targetRole 目标角色（"SELLER" 或 "BUYER"），用于前端判断显示哪个角标
     * @param orderDTO 完整的订单DTO（可选，ORDER_CREATED时发送完整订单信息，类似消息发送完整MessageDTO）
     */
    private void pushOrderChangeNotificationWithDTO(String userId, String orderId, String changeType, String orderStatus, String targetRole, OrderDTO orderDTO) {
        try {
            Map<String, Object> notification = new java.util.HashMap<>();
            notification.put("type", "ORDER_CHANGE");
            notification.put("orderId", orderId);
            notification.put("changeType", changeType);
            notification.put("orderStatus", orderStatus);
            notification.put("targetRole", targetRole); // ✅ 添加目标角色字段，用于前端判断
            notification.put("timestamp", LocalDateTime.now().toString());
            
            // ✅ 如果是订单创建或可见性恢复，发送完整OrderDTO（类似消息发送完整MessageDTO）
            if (orderDTO != null && ("ORDER_CREATED".equals(changeType) || "ORDER_VISIBILITY_RESTORED".equals(changeType))) {
                notification.put("order", orderDTO);
            }
            
            // 使用重试服务推送订单变化通知（带重试机制）
            webSocketRetryService.pushWithRetry(userId, notification, "ORDER_CHANGE");
            log.debug("订单变化通知推送尝试（带重试）: userId={}, orderId={}, changeType={}, orderStatus={}, targetRole={}, hasOrderDTO={}", 
                    userId, orderId, changeType, orderStatus, targetRole, orderDTO != null);
        } catch (Exception e) {
            log.error("推送订单变化通知失败: userId={}, orderId={}, changeType={}, error={}", 
                    userId, orderId, changeType, e.getMessage(), e);
            // WebSocket 推送失败不影响订单操作的成功返回
        }
    }
}