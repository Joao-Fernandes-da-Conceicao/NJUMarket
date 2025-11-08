package com.njumarket.order.service.impl;

import com.njumarket.njumarket.dto.Result;
import com.njumarket.njumarket.dto.OrderDTO;
import com.njumarket.njumarket.vo.OrderPageResultVO;
import com.njumarket.njumarket.entity.Order;
import com.njumarket.njumarket.entity.Commodity;
import com.njumarket.njumarket.entity.User;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.njumarket.njumarket.dto.internal.UserProfileInternalDTO;
import com.njumarket.njumarket.dto.internal.UserInternalDTO;
import com.njumarket.njumarket.dto.internal.CommodityInternalDTO;
import com.njumarket.njumarket.exception.BusinessException;
import com.njumarket.order.repository.OrderRepository;
import com.njumarket.order.service.OrderService;
import com.njumarket.order.client.AuthClient;
import com.njumarket.order.client.CommodityClient;
import com.njumarket.order.client.CommodityQueryClient;
import com.njumarket.order.client.ChangeRecordClient;
import com.njumarket.order.client.MessageClient;
import com.njumarket.njumarket.utils.BusinessValidator;
import com.njumarket.njumarket.utils.SecurityUtils;
import com.njumarket.njumarket.utils.RedisLockUtil;
import com.njumarket.njumarket.utils.RedisConstants;
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
    
    // ✅ 使用Feign Client调用其他服务
    private final AuthClient authClient;
    private final CommodityClient commodityClient;
    private final CommodityQueryClient commodityQueryClient;
    private final ChangeRecordClient changeRecordClient;
    private final MessageClient messageClient;
    private final ObjectMapper objectMapper;
    
    private final RedisLockUtil redisLockUtil;

    // ========== 买家功能 ==========
    @Override
    @Transactional
    public Result createOrder(OrderDTO orderDTO) {
        // 获取当前用户（使用 SecurityUtils）
        User currentUser = SecurityUtils.requireCurrentUser();
        
        // ✅ 参数验证已由Bean Validation在Controller层完成，此处无需重复验证
        
        // ✅ 第一步：获取分布式锁（跨服务器保护）
        // 防止多台服务器同时处理同一商品的订单创建
        String lockKey = RedisConstants.LOCK_COMMODITY_KEY + orderDTO.getCommodityId();
        String lockValue = RedisLockUtil.generateLockValue();
        long lockTimeout = RedisConstants.LOCK_COMMODITY_TTL;
        
        boolean lockAcquired = false;
        try {
            // 尝试获取分布式锁（最多等待1秒，重试间隔100ms）
            lockAcquired = redisLockUtil.tryLock(lockKey, lockValue, lockTimeout, 1, 100);
            
            if (!lockAcquired) {
                throw new BusinessException("系统繁忙，请稍后重试");
            }
            
            // ✅ 第二步：使用Feign Client查询商品（带悲观锁）
            Result commodityResult = commodityClient.getCommodityForUpdate(orderDTO.getCommodityId());
            if (!commodityResult.getSuccess() || commodityResult.getData() == null) {
                throw new BusinessException("商品不存在");
            }
            
            // ✅ 使用ObjectMapper正确转换类型（避免ClassCastException）
            CommodityInternalDTO commodityDTO;
            try {
                commodityDTO = objectMapper.convertValue(
                    commodityResult.getData(),
                    new TypeReference<CommodityInternalDTO>() {}
                );
            } catch (Exception e) {
                log.error("转换CommodityInternalDTO失败: commodityId={}, error={}", 
                    orderDTO.getCommodityId(), e.getMessage(), e);
                throw new BusinessException("商品信息解析失败");
            }
            
            // 将CommodityInternalDTO转换为Commodity实体
            Commodity commodity = convertCommodityDTOToEntity(commodityDTO);
            
            // 检查商品状态
            if (!"ON_SHELF".equals(commodity.getCommodityStatus())) {
                throw new BusinessException("商品未上架，无法购买");
            }
            
            // 检查库存（在锁定的情况下再次检查，确保准确性）
            if (commodity.getStock() < orderDTO.getQuantity()) {
                throw new BusinessException("商品库存不足，当前库存：" + commodity.getStock());
            }
            
            // 检查是否购买自己的商品
            if (commodity.getSellerId().equals(currentUser.getUserId())) {
                throw new BusinessException("不能购买自己的商品");
            }
            
            // 验证价格
            double expectedAmount = commodity.getPrice() * orderDTO.getQuantity();
            if (Math.abs(orderDTO.getPayAmount() - expectedAmount) > 0.01) {
                throw new BusinessException("支付金额与商品价格不符");
            }
            
            // ✅ 第三步：使用Feign Client更新商品库存（三重保护）
            Result updateResult = commodityClient.updateCommodityStock(
                orderDTO.getCommodityId(), 
                orderDTO.getQuantity()
            );
            
            if (!updateResult.getSuccess()) {
                // 库存不足，条件更新失败
                throw new BusinessException("商品库存不足，请刷新后重试");
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
            
            // ✅ 创建商品快照 - 获取卖家信息（使用Feign Client）
            Result sellerResult = authClient.getUserById(commodity.getSellerId());
            if (sellerResult.getSuccess() && sellerResult.getData() != null) {
                try {
                    // ✅ 使用ObjectMapper正确转换类型（避免ClassCastException）
                    UserInternalDTO sellerDTO = objectMapper.convertValue(
                        sellerResult.getData(),
                        new TypeReference<UserInternalDTO>() {}
                    );
                    // 转换为User实体
                    User seller = convertUserDTOToEntity(sellerDTO);
                    order.createCommoditySnapshot(commodity, seller);
                } catch (Exception e) {
                    log.error("转换User失败: sellerId={}, error={}", commodity.getSellerId(), e.getMessage(), e);
                    log.warn("创建订单警告 - sellerId={}, 卖家信息解析失败，无法创建商品快照", commodity.getSellerId());
                }
            } else {
                log.warn("创建订单警告 - sellerId={}, 卖家不存在，无法创建商品快照", commodity.getSellerId());
            }
            
            // 保存订单
            orderRepository.save(order);
            
            // ✅ 记录订单变更（用于增量轮询，使用Feign Client）
            LocalDateTime now = LocalDateTime.now();
            try {
                changeRecordClient.recordOrderChange(order.getOrderId(), "CREATE", now.toString());
            } catch (Exception e) {
                log.warn("记录订单变更失败（不影响订单创建）: orderId={}, error={}", 
                    order.getOrderId(), e.getMessage());
            }
            
            // ✅ WebSocket 推送：订单创建通知给卖家（发送完整OrderDTO，包含profile信息）
            // 注意：pushOrderChangeNotificationWithDTO 内部已包含订单提醒状态更新逻辑
            OrderDTO orderDTOForNotification = convertToDTOWithProfile(order);
            pushOrderChangeNotificationWithDTO(order.getSellerId(), order.getOrderId(), "ORDER_CREATED", order.getOrderStatus(), "SELLER", orderDTOForNotification);
            
            return Result.ok("订单创建成功");
            
        } catch (BusinessException e) {
            // 业务异常直接重新抛出，由GlobalExceptionHandler处理
            throw e;
        } catch (Exception e) {
            // 系统异常包装为BusinessException抛出
            throw new BusinessException("创建订单失败，请稍后重试", e);
        } finally {
            // ✅ 释放分布式锁（必须在finally中释放，确保锁一定会被释放）
            if (lockAcquired) {
                boolean released = redisLockUtil.releaseLock(lockKey, lockValue);
                if (!released) {
                    log.warn("释放分布式锁失败 - commodityId={}, lockKey={}", 
                             orderDTO.getCommodityId(), lockKey);
                }
            }
        }
    }

    @Override
    @Transactional
    public Result payOrder(String orderId) {
        // 获取当前用户（使用 SecurityUtils）
        User currentUser = SecurityUtils.requireCurrentUser();
        
        // 查找订单
        Optional<Order> orderOpt = orderRepository.findById(orderId);
        if (orderOpt.isEmpty()) {
            throw new BusinessException("订单不存在");
        }
        
        Order order = orderOpt.get();
        
        // 检查权限：只有买家可以支付
        if (!order.getBuyerId().equals(currentUser.getUserId())) {
            throw new BusinessException("无权限支付此订单");
        }
        
        // 检查订单状态
        if (!"CREATED".equals(order.getOrderStatus())) {
            throw new BusinessException("订单状态异常，无法支付");
        }
        
        try {
            // 支付订单
            if (order.payOrder()) {
                orderRepository.save(order);
                
                // ✅ 记录订单变更（用于增量轮询，使用Feign Client）
                LocalDateTime now = LocalDateTime.now();
                try {
                    changeRecordClient.recordOrderChange(order.getOrderId(), "PAY", now.toString());
                } catch (Exception e) {
                    log.warn("记录订单变更失败（不影响订单支付）: orderId={}, error={}", 
                        order.getOrderId(), e.getMessage());
                }
                
                // ✅ WebSocket 推送：订单支付通知给卖家（包含profile信息）
                OrderDTO orderDTOForPay = convertToDTOWithProfile(order);
                pushOrderChangeNotificationWithDTO(order.getSellerId(), order.getOrderId(), "ORDER_PAID", order.getOrderStatus(), "SELLER", orderDTOForPay);
                
                return Result.ok("订单支付成功");
            } else {
                throw new BusinessException("订单支付失败");
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException("支付订单失败，请稍后重试", e);
        }
    }

    @Override
    @Transactional
    public Result confirmOrder(String orderId) {
        // 获取当前用户（使用 SecurityUtils）
        User currentUser = SecurityUtils.requireCurrentUser();
        
        // 查找订单
        Optional<Order> orderOpt = orderRepository.findById(orderId);
        if (orderOpt.isEmpty()) {
            throw new BusinessException("订单不存在");
        }
        
        Order order = orderOpt.get();
        
        // 检查权限：只有买家可以确认收货
        if (!order.getBuyerId().equals(currentUser.getUserId())) {
            throw new BusinessException("无权限确认收货此订单");
        }
        
        // 检查订单状态
        if (!"SHIPPED".equals(order.getOrderStatus())) {
            throw new BusinessException("订单状态异常，无法确认收货");
        }
        
        try {
            // 确认收货
            if (order.completeOrder()) {
                orderRepository.save(order);
                
                // ✅ 记录订单变更（用于增量轮询）
                LocalDateTime now = LocalDateTime.now();
                try {
                    changeRecordClient.recordOrderChange(order.getOrderId(), "COMPLETE", now.toString());
                } catch (Exception e) {
                    log.warn("记录订单变更失败（不影响订单完成）: orderId={}, error={}", 
                        order.getOrderId(), e.getMessage());
                }
                
                // ✅ WebSocket 推送：订单完成通知给卖家（包含profile信息）
                OrderDTO orderDTOForComplete = convertToDTOWithProfile(order);
                pushOrderChangeNotificationWithDTO(order.getSellerId(), order.getOrderId(), "ORDER_COMPLETED", order.getOrderStatus(), "SELLER", orderDTOForComplete);
                
                return Result.ok("订单确认收货成功");
            } else {
                throw new BusinessException("订单确认收货失败");
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException("确认收货失败，请稍后重试", e);
        }
    }

    @Override
    @Transactional
    public Result cancelOrder(String orderId, String reason) {
        // 获取当前用户（使用 SecurityUtils）
        User currentUser = SecurityUtils.requireCurrentUser();
        
        // 查找订单
        Optional<Order> orderOpt = orderRepository.findById(orderId);
        if (orderOpt.isEmpty()) {
            throw new BusinessException("订单不存在");
        }
        
        Order order = orderOpt.get();
        
        // 检查权限：只有买家和卖家可以取消订单
        if (!order.getBuyerId().equals(currentUser.getUserId()) && 
            !order.getSellerId().equals(currentUser.getUserId())) {
            throw new BusinessException("无权限取消此订单");
        }
        
        // 检查订单状态：只有未发货的订单可以取消
        if (!"CREATED".equals(order.getOrderStatus()) && !"PAID".equals(order.getOrderStatus())) {
            throw new BusinessException("订单状态不允许取消");
        }
        
        try {
            // 在取消订单前检查是否可以恢复库存
            boolean shouldRestoreStock = order.canRestoreStock();
            
            // 取消订单
            if (order.cancelOrder()) {
                // ✅ 只有未发货和未付款的订单取消时才恢复库存（使用Feign Client）
                if (shouldRestoreStock) {
                    Result restoreResult = commodityClient.restoreCommodityStock(
                        order.getCommodityId(), 
                        order.getQuantity()
                    );
                    if (!restoreResult.getSuccess()) {
                        log.warn("订单取消，但库存恢复失败 - orderId: {}, commodityId: {}, quantity: {}", 
                            order.getOrderId(), order.getCommodityId(), order.getQuantity());
                    }
                }
                
                orderRepository.save(order);
                
                // ✅ 记录订单变更（用于增量轮询）
                LocalDateTime now = LocalDateTime.now();
                try {
                    changeRecordClient.recordOrderChange(order.getOrderId(), "CANCEL", now.toString());
                } catch (Exception e) {
                    log.warn("记录订单变更失败（不影响订单取消）: orderId={}, error={}", 
                        order.getOrderId(), e.getMessage());
                }
                
                // ✅ WebSocket 推送：订单取消通知（包含profile信息）
                // 如果是买家取消，通知卖家；如果是卖家取消，通知买家
                OrderDTO orderDTOForCancel = convertToDTOWithProfile(order);
                if (order.getBuyerId().equals(currentUser.getUserId())) {
                    pushOrderChangeNotificationWithDTO(order.getSellerId(), order.getOrderId(), "ORDER_CANCELLED", order.getOrderStatus(), "SELLER", orderDTOForCancel);
                } else {
                    pushOrderChangeNotificationWithDTO(order.getBuyerId(), order.getOrderId(), "ORDER_CANCELLED", order.getOrderStatus(), "BUYER", orderDTOForCancel);
                }
                
                return Result.ok("订单取消成功");
            } else {
                throw new BusinessException("订单取消失败");
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException("取消订单失败，请稍后重试", e);
        }
    }

    @Override
    @Transactional
    public Result requestRefund(String orderId, String reason) {
        User currentUser = SecurityUtils.requireCurrentUser();
        Order order = BusinessValidator.requireOrder(orderId, orderRepository);
        BusinessValidator.requireBuyer(order, currentUser.getUserId());
        BusinessValidator.requireOrderStatus(order, "COMPLETED", "REFUND_REJECTED");
        
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
        
        // ✅ 记录订单变更（用于增量轮询）
        LocalDateTime now = LocalDateTime.now();
        try {
            changeRecordClient.recordOrderChange(order.getOrderId(), "REFUND_REQUEST", now.toString());
        } catch (Exception e) {
            log.warn("记录订单变更失败（不影响退款申请）: orderId={}, error={}", 
                order.getOrderId(), e.getMessage());
        }
        
        // ✅ WebSocket 推送：退款申请通知给卖家（包含profile信息）
        OrderDTO orderDTOForRefundRequest = convertToDTOWithProfile(order);
        pushOrderChangeNotificationWithDTO(order.getSellerId(), order.getOrderId(), "REFUND_REQUESTED", order.getOrderStatus(), "SELLER", orderDTOForRefundRequest);
        
        // ✅ 如果是恢复可见性（极端情况），推送完整的OrderDTO（直接更新，无需刷新）
        // 这种情况可能发生在：
        // 1. 从COMPLETED状态申请退款（卖家之前软删除了订单）
        // 2. 从REFUND_REJECTED状态重新申请退款（卖家之前软删除了订单）
        if (sellerVisibilityRestored) {
            pushOrderChangeNotificationWithDTO(order.getSellerId(), order.getOrderId(), "ORDER_VISIBILITY_RESTORED", order.getOrderStatus(), "SELLER", orderDTOForRefundRequest);
        }
        
        return Result.ok("退款/退货申请成功");
    }

    @Override
    public Result getBuyerOrders(Integer page, Integer size, String status) {
        User currentUser = SecurityUtils.requireCurrentUser();
        
        // 创建分页对象
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "createTime"));
        
        // ✅ 查询订单（在数据库层面过滤掉HIDDEN状态的订单）
        Page<Order> orderPage;
        if (StringUtils.hasText(status)) {
            orderPage = orderRepository.findByBuyerIdAndOrderStatusAndBuyerVisibilityNotHidden(
                currentUser.getUserId(), status, pageable);
        } else {
            orderPage = orderRepository.findByBuyerIdAndBuyerVisibilityNotHidden(
                currentUser.getUserId(), pageable);
        }
        
        List<Order> visibleOrders = orderPage.getContent();
        
        // ✅ 批量查询所有相关的UserProfile（避免N+1查询）
        Set<String> userIds = new HashSet<>();
        for (Order order : visibleOrders) {
            if (order.getSellerId() != null) userIds.add(order.getSellerId());
            if (order.getBuyerId() != null) userIds.add(order.getBuyerId());
        }
        
        Map<String, UserProfileInternalDTO> profileMap = new HashMap<>();
        if (!userIds.isEmpty()) {
            Result profilesResult = authClient.getUserProfilesByIds(new ArrayList<>(userIds));
            if (profilesResult.getSuccess() && profilesResult.getData() != null) {
                try {
                    List<UserProfileInternalDTO> profiles = objectMapper.convertValue(
                        profilesResult.getData(),
                        new TypeReference<List<UserProfileInternalDTO>>() {}
                    );
                    profileMap = profiles.stream()
                        .collect(Collectors.toMap(UserProfileInternalDTO::getUserId, p -> p));
                } catch (Exception e) {
                    log.error("转换UserProfileInternalDTO列表失败: {}", e.getMessage(), e);
                }
            }
        }
        
        // ✅ 转换为DTO（包含profile信息）
        final Map<String, UserProfileInternalDTO> finalProfileMap = profileMap;
        List<OrderDTO> orderDTOs = visibleOrders.stream()
                .map(order -> convertToDTOWithProfile(order, finalProfileMap))
                .collect(Collectors.toList());
        
        OrderPageResultVO result = new OrderPageResultVO();
        result.setOrders(orderDTOs);
        result.setTotal(orderPage.getTotalElements()); // ✅ 使用数据库查询的总数
        result.setPages(orderPage.getTotalPages());
        result.setCurrent(page);
        result.setSize(size);
        
        return Result.ok("获取买家订单列表成功", result);
    }

    // ========== 卖家功能 ==========
    @Override
    @Transactional
    public Result shipOrder(String orderId, String trackingNumber) {
        User currentUser = SecurityUtils.requireCurrentUser();
        Order order = BusinessValidator.requireOrder(orderId, orderRepository);
        BusinessValidator.requireSeller(order, currentUser.getUserId());
        BusinessValidator.requireOrderStatus(order, "PAID");
        
        // 发货
        if (!order.shipOrder(trackingNumber)) {
            throw new BusinessException("订单发货失败");
        }
        
        orderRepository.save(order);
        
        // ✅ 记录订单变更（用于增量轮询）
        LocalDateTime now = LocalDateTime.now();
        try {
            changeRecordClient.recordOrderChange(order.getOrderId(), "SHIP", now.toString());
        } catch (Exception e) {
            log.warn("记录订单变更失败（不影响订单发货）: orderId={}, error={}", 
                order.getOrderId(), e.getMessage());
        }
        
        // ✅ WebSocket 推送：订单发货通知给买家（包含profile信息）
        OrderDTO orderDTOForShip = convertToDTOWithProfile(order);
        pushOrderChangeNotificationWithDTO(order.getBuyerId(), order.getOrderId(), "ORDER_SHIPPED", order.getOrderStatus(), "BUYER", orderDTOForShip);
        
        return Result.ok("订单发货成功");
    }

    @Override
    @Transactional
    public Result handleRefund(String orderId, String decision, String remark) {
        User currentUser = SecurityUtils.requireCurrentUser();
        Order order = BusinessValidator.requireOrder(orderId, orderRepository);
        BusinessValidator.requireSeller(order, currentUser.getUserId());
        BusinessValidator.requireOrderStatus(order, "REFUND_REQUESTED");
        
        // 处理退款/退货申请
        // ✅ 在修改前检测是否恢复可见性
        boolean buyerVisibilityRestored = "HIDDEN".equals(order.getBuyerVisibility());
        
        if ("APPROVE".equals(decision)) {
            // 同意退款/退货
            order.setOrderStatus("REFUND_APPROVED");
            order.setReturnApprovalTime(LocalDateTime.now());
            
            // ✅ 设置买家可见性为可见，确保买家能够看到处理结果（自动恢复可见性）
            order.setBuyerVisibility("PUBLIC");
            
            // ✅ 恢复商品库存（使用Feign Client）
            Result restoreResult = commodityClient.restoreCommodityStock(
                order.getCommodityId(), 
                order.getQuantity()
            );
            if (!restoreResult.getSuccess()) {
                log.warn("退款同意，但库存恢复失败 - orderId: {}, commodityId: {}, quantity: {}", 
                    orderId, order.getCommodityId(), order.getQuantity());
            }
        } else if ("REJECT".equals(decision)) {
            // 拒绝退款/退货
            order.setOrderStatus("REFUND_REJECTED");
            order.setReturnRejectionReason(remark);
            order.setReturnApprovalTime(LocalDateTime.now());
            
            // ✅ 设置买家可见性为可见，确保买家能够看到处理结果（自动恢复可见性）
            order.setBuyerVisibility("PUBLIC");
        } else {
            throw new BusinessException("无效的处理决定");
        }
        
        orderRepository.save(order);
        
        // ✅ 记录订单变更（用于增量轮询，使用Feign Client）
        LocalDateTime now = LocalDateTime.now();
        String operation = "APPROVE".equals(decision) ? "REFUND_APPROVE" : "REFUND_REJECT";
        try {
            changeRecordClient.recordOrderChange(order.getOrderId(), operation, now.toString());
        } catch (Exception e) {
            log.warn("记录订单变更失败（不影响退款处理）: orderId={}, error={}", 
                order.getOrderId(), e.getMessage());
        }
        
        // ✅ WebSocket 推送：退款处理结果通知给买家（包含profile信息）
        String notificationType = "APPROVE".equals(decision) ? "REFUND_APPROVED" : "REFUND_REJECTED";
        OrderDTO orderDTOForRefundHandle = convertToDTOWithProfile(order);
        pushOrderChangeNotificationWithDTO(order.getBuyerId(), order.getOrderId(), notificationType, order.getOrderStatus(), "BUYER", orderDTOForRefundHandle);
        
        // ✅ 如果是恢复可见性（极端情况），推送完整的OrderDTO（直接更新，无需刷新）
        if (buyerVisibilityRestored) {
            pushOrderChangeNotificationWithDTO(order.getBuyerId(), order.getOrderId(), "ORDER_VISIBILITY_RESTORED", order.getOrderStatus(), "BUYER", orderDTOForRefundHandle);
        }
        
        return Result.ok("退款/退货申请处理成功");
    }

    @Override
    public Result getSellerOrders(Integer page, Integer size, String status) {
        User currentUser = SecurityUtils.requireCurrentUser();
        
        // 创建分页对象
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "createTime"));
        
        // ✅ 查询订单（在数据库层面过滤掉HIDDEN状态的订单）
        Page<Order> orderPage;
        if (StringUtils.hasText(status)) {
            orderPage = orderRepository.findBySellerIdAndOrderStatusAndSellerVisibilityNotHidden(
                currentUser.getUserId(), status, pageable);
        } else {
            orderPage = orderRepository.findBySellerIdAndSellerVisibilityNotHidden(
                currentUser.getUserId(), pageable);
        }
        
        List<Order> visibleOrders = orderPage.getContent();
        
        // ✅ 批量查询所有相关的UserProfile（避免N+1查询）
        Set<String> userIds = new HashSet<>();
        for (Order order : visibleOrders) {
            if (order.getSellerId() != null) userIds.add(order.getSellerId());
            if (order.getBuyerId() != null) userIds.add(order.getBuyerId());
        }
        
        Map<String, UserProfileInternalDTO> profileMap = new HashMap<>();
        if (!userIds.isEmpty()) {
            Result profilesResult = authClient.getUserProfilesByIds(new ArrayList<>(userIds));
            if (profilesResult.getSuccess() && profilesResult.getData() != null) {
                try {
                    List<UserProfileInternalDTO> profiles = objectMapper.convertValue(
                        profilesResult.getData(),
                        new TypeReference<List<UserProfileInternalDTO>>() {}
                    );
                    profileMap = profiles.stream()
                        .collect(Collectors.toMap(UserProfileInternalDTO::getUserId, p -> p));
                } catch (Exception e) {
                    log.error("转换UserProfileInternalDTO列表失败: {}", e.getMessage(), e);
                }
            }
        }
        
        // ✅ 转换为DTO（包含profile信息）
        final Map<String, UserProfileInternalDTO> finalProfileMap = profileMap;
        List<OrderDTO> orderDTOs = visibleOrders.stream()
                .map(order -> convertToDTOWithProfile(order, finalProfileMap))
                .collect(Collectors.toList());
        
        OrderPageResultVO result = new OrderPageResultVO();
        result.setOrders(orderDTOs);
        result.setTotal(orderPage.getTotalElements()); // ✅ 使用数据库查询的总数
        result.setPages(orderPage.getTotalPages());
        result.setCurrent(page);
        result.setSize(size);
        
        return Result.ok("获取卖家订单列表成功", result);
    }

    // ========== 通用功能 ==========
    @Override
    public Result getOrderDetail(String orderId) {
        User currentUser = SecurityUtils.requireCurrentUser();
        Order order = BusinessValidator.requireOrder(orderId, orderRepository);
        BusinessValidator.requireBuyerOrSeller(order, currentUser.getUserId());
        
        // ✅ 转换为DTO（包含profile信息）
        OrderDTO orderDTO = convertToDTOWithProfile(order);
        
        // 查询卖家详细信息（使用Feign Client）
        Result sellerResult = authClient.getUserById(order.getSellerId());
        if (sellerResult.getSuccess() && sellerResult.getData() != null) {
            // ✅ 使用ObjectMapper正确转换类型（避免ClassCastException）
            UserInternalDTO sellerDTO;
            try {
                sellerDTO = objectMapper.convertValue(
                    sellerResult.getData(),
                    new TypeReference<UserInternalDTO>() {}
                );
            } catch (Exception e) {
                log.error("转换User失败: sellerId={}, error={}", order.getSellerId(), e.getMessage(), e);
                // 如果转换失败，创建已注销的卖家信息
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
                // 继续处理买家信息
                return handleBuyerInfoAndReturn(order, orderDTO);
            }
            User seller = convertUserDTOToEntity(sellerDTO);
            OrderDTO.SellerInfo sellerInfo = new OrderDTO.SellerInfo();
            sellerInfo.setUserId(seller.getUserId());
            sellerInfo.setUsername(seller.getUsername());
            
            // 查询卖家档案信息（使用Feign Client批量查询）
            List<String> sellerIds = Arrays.asList(seller.getUserId());
            Result sellerProfilesResult = authClient.getUserProfilesByIds(sellerIds);
            if (sellerProfilesResult.getSuccess() && sellerProfilesResult.getData() != null) {
                try {
                    List<UserProfileInternalDTO> profiles = objectMapper.convertValue(
                        sellerProfilesResult.getData(),
                        new TypeReference<List<UserProfileInternalDTO>>() {}
                    );
                    if (!profiles.isEmpty()) {
                        UserProfileInternalDTO sellerProfile = profiles.get(0);
                        sellerInfo.setNickname(sellerProfile.getNickname());
                        sellerInfo.setAvatar(sellerProfile.getAvatar());
                    } else {
                        sellerInfo.setNickname(seller.getUsername());
                        sellerInfo.setAvatar(null);
                    }
                } catch (Exception e) {
                    log.error("转换UserProfileInternalDTO失败: {}", e.getMessage(), e);
                    sellerInfo.setNickname(seller.getUsername());
                    sellerInfo.setAvatar(null);
                }
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
        
        // 查询买家详细信息（使用Feign Client）
        Result buyerResult = authClient.getUserById(order.getBuyerId());
        if (buyerResult.getSuccess() && buyerResult.getData() != null) {
            // ✅ 使用ObjectMapper正确转换类型（避免ClassCastException）
            UserInternalDTO buyerDTO;
            try {
                buyerDTO = objectMapper.convertValue(
                    buyerResult.getData(),
                    new TypeReference<UserInternalDTO>() {}
                );
            } catch (Exception e) {
                log.error("转换User失败: buyerId={}, error={}", order.getBuyerId(), e.getMessage(), e);
                // 如果转换失败，创建已注销的买家信息
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
                return Result.ok("获取订单详情成功", orderDTO);
            }
            User buyer = convertUserDTOToEntity(buyerDTO);
            OrderDTO.BuyerInfo buyerInfo = new OrderDTO.BuyerInfo();
            buyerInfo.setUserId(buyer.getUserId());
            buyerInfo.setUsername(buyer.getUsername());
            
            // 查询买家档案信息（使用Feign Client批量查询）
            List<String> buyerIds = Arrays.asList(buyer.getUserId());
            Result buyerProfilesResult = authClient.getUserProfilesByIds(buyerIds);
            if (buyerProfilesResult.getSuccess() && buyerProfilesResult.getData() != null) {
                try {
                    List<UserProfileInternalDTO> profiles = objectMapper.convertValue(
                        buyerProfilesResult.getData(),
                        new TypeReference<List<UserProfileInternalDTO>>() {}
                    );
                    if (!profiles.isEmpty()) {
                        UserProfileInternalDTO buyerProfile = profiles.get(0);
                        buyerInfo.setNickname(buyerProfile.getNickname());
                        buyerInfo.setAvatar(buyerProfile.getAvatar());
                    } else {
                        buyerInfo.setNickname(buyer.getUsername());
                        buyerInfo.setAvatar(null);
                    }
                } catch (Exception e) {
                    log.error("转换UserProfileInternalDTO失败: {}", e.getMessage(), e);
                    buyerInfo.setNickname(buyer.getUsername());
                    buyerInfo.setAvatar(null);
                }
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
    }

    // ========== 私有辅助方法 ==========
    
    /**
     * 将订单实体转换为DTO（不包含profile信息，用于不需要profile的场景）
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
    
    /**
     * 将订单实体转换为DTO（包含profile信息，使用批量查询的Map）
     * @param order 订单实体
     * @param profileMap profile Map（key: userId, value: UserProfile）
     * @return OrderDTO
     */
    private OrderDTO convertToDTOWithProfile(Order order, Map<String, UserProfileInternalDTO> profileMap) {
        OrderDTO dto = convertToDTO(order);
        
        // ✅ 从Map中获取seller profile
        UserProfileInternalDTO sellerProfile = profileMap.get(order.getSellerId());
        if (sellerProfile != null) {
            dto.setSellerNickname(sellerProfile.getNickname());
            dto.setSellerAvatar(sellerProfile.getAvatar());
        }
        
        // ✅ 从Map中获取buyer profile
        UserProfileInternalDTO buyerProfile = profileMap.get(order.getBuyerId());
        if (buyerProfile != null) {
            dto.setBuyerNickname(buyerProfile.getNickname());
            dto.setBuyerAvatar(buyerProfile.getAvatar());
        }
        
        return dto;
    }
    
    /**
     * 将订单实体转换为DTO（包含profile信息，单条订单查询场景）
     * @param order 订单实体
     * @return OrderDTO
     */
    private OrderDTO convertToDTOWithProfile(Order order) {
        OrderDTO dto = convertToDTO(order);
        
        // ✅ 查询seller和buyer profile（使用Feign Client批量查询）
        List<String> userIds = Arrays.asList(order.getSellerId(), order.getBuyerId());
        Result profilesResult = authClient.getUserProfilesByIds(userIds);
        if (profilesResult.getSuccess() && profilesResult.getData() != null) {
            try {
                List<UserProfileInternalDTO> profiles = objectMapper.convertValue(
                    profilesResult.getData(),
                    new TypeReference<List<UserProfileInternalDTO>>() {}
                );
                Map<String, UserProfileInternalDTO> profileMap = profiles.stream()
                        .collect(Collectors.toMap(UserProfileInternalDTO::getUserId, p -> p));
                
                UserProfileInternalDTO sellerProfile = profileMap.get(order.getSellerId());
                if (sellerProfile != null) {
                    dto.setSellerNickname(sellerProfile.getNickname());
                    dto.setSellerAvatar(sellerProfile.getAvatar());
                }
                
                UserProfileInternalDTO buyerProfile = profileMap.get(order.getBuyerId());
                if (buyerProfile != null) {
                    dto.setBuyerNickname(buyerProfile.getNickname());
                    dto.setBuyerAvatar(buyerProfile.getAvatar());
                }
            } catch (Exception e) {
                log.error("转换UserProfileInternalDTO失败: {}", e.getMessage(), e);
            }
        }
        
        return dto;
    }

    @Override
    @Transactional
    public Result updateOrderVisibility(String orderId, String visibility) {
        User currentUser = SecurityUtils.requireCurrentUser();
        Order order = BusinessValidator.requireOrder(orderId, orderRepository);
        BusinessValidator.requireBuyerOrSeller(order, currentUser.getUserId());
        
        // 检查订单状态：只有未完成的订单可以修改可见性
        if (!order.canModifyVisibility()) {
            throw new BusinessException("订单状态不允许修改可见性");
        }
        
        // 验证可见性值
        if (!"PUBLIC".equals(visibility) && !"PRIVATE".equals(visibility) && !"HIDDEN".equals(visibility)) {
            throw new BusinessException("无效的可见性值");
        }
        
        // 修改可见性（同时设置卖家和买家可见性）
        if (!order.setVisibility(visibility)) {
            throw new BusinessException("订单可见性修改失败");
        }
        
        orderRepository.save(order);
        return Result.ok("订单可见性修改成功");
    }

    @Override
    @Transactional
    public Result updateOrderSellerVisibility(String orderId, String sellerVisibility) {
        User currentUser = SecurityUtils.requireCurrentUser();
        Order order = BusinessValidator.requireOrder(orderId, orderRepository);
        BusinessValidator.requireSeller(order, currentUser.getUserId());
        
        // 检查订单状态：只有未完成的订单可以修改可见性
        if (!order.canModifyVisibility()) {
            throw new BusinessException("订单状态不允许修改可见性");
        }
        
        // 验证可见性值
        if (!"PUBLIC".equals(sellerVisibility) && !"PRIVATE".equals(sellerVisibility) && !"HIDDEN".equals(sellerVisibility)) {
            throw new BusinessException("无效的卖家可见性值");
        }
        
        // 修改卖家可见性
        if (!order.setSellerVisibility(sellerVisibility)) {
            throw new BusinessException("订单卖家可见性修改失败");
        }
        
        orderRepository.save(order);
        return Result.ok("订单卖家可见性修改成功");
    }

    @Override
    @Transactional
    public Result updateOrderBuyerVisibility(String orderId, String buyerVisibility) {
        User currentUser = SecurityUtils.requireCurrentUser();
        Order order = BusinessValidator.requireOrder(orderId, orderRepository);
        BusinessValidator.requireBuyer(order, currentUser.getUserId());
        
        // 检查订单状态：只有未完成的订单可以修改可见性
        if (!order.canModifyVisibility()) {
            throw new BusinessException("订单状态不允许修改可见性");
        }
        
        // 验证可见性值
        if (!"PUBLIC".equals(buyerVisibility) && !"PRIVATE".equals(buyerVisibility) && !"HIDDEN".equals(buyerVisibility)) {
            throw new BusinessException("无效的买家可见性值");
        }
        
        // 修改买家可见性
        if (!order.setBuyerVisibility(buyerVisibility)) {
            throw new BusinessException("订单买家可见性修改失败");
        }
        
        orderRepository.save(order);
        return Result.ok("订单买家可见性修改成功");
    }

    @Override
    @Transactional
    public Result requestReturn(String orderId, String returnReason) {
        User currentUser = SecurityUtils.requireCurrentUser();
        Order order = BusinessValidator.requireOrder(orderId, orderRepository);
        BusinessValidator.requireBuyer(order, currentUser.getUserId());
        
        // 检查是否可以申请退货
        if (!order.canRequestReturn()) {
            throw new BusinessException("订单状态不允许申请退货");
        }
        
        // 验证退货原因
        BusinessValidator.requireNotBlank(returnReason, "退货原因不能为空");
        
        // 申请退货
        if (!order.requestReturn(returnReason)) {
            throw new BusinessException("退货申请失败");
        }
        
        orderRepository.save(order);
        return Result.ok("退货申请成功");
    }

    @Override
    @Transactional
    public Result approveReturnRequest(String orderId, Boolean approved, String rejectionReason) {
        User currentUser = SecurityUtils.requireCurrentUser();
        Order order = BusinessValidator.requireOrder(orderId, orderRepository);
        BusinessValidator.requireSeller(order, currentUser.getUserId());
        
        // 检查是否可以审批退货
        if (!order.canApproveReturn()) {
            throw new BusinessException("订单状态不允许审批退货");
        }
        
        // 如果拒绝，验证拒绝原因
        if (!approved && !StringUtils.hasText(rejectionReason)) {
            throw new BusinessException("拒绝退货时必须提供拒绝原因");
        }
        
        // 审批退货申请
        if (!order.approveReturnRequest(approved, rejectionReason)) {
            throw new BusinessException("退货审批失败");
        }
        
        orderRepository.save(order);
        return Result.ok(approved ? "退货申请已同意" : "退货申请已拒绝");
    }

    @Override
    @Transactional
    public Result confirmReturnShipment(String orderId, String returnTrackingNumber) {
        User currentUser = SecurityUtils.requireCurrentUser();
        Order order = BusinessValidator.requireOrder(orderId, orderRepository);
        BusinessValidator.requireBuyer(order, currentUser.getUserId());
        
        // 检查是否可以确认退货发货
        if (!order.canConfirmReturnShipment()) {
            throw new BusinessException("订单状态不允许确认退货发货");
        }
        
        // 验证快递单号
        BusinessValidator.requireNotBlank(returnTrackingNumber, "退货快递单号不能为空");
        
        // 确认退货发货
        if (!order.confirmReturnShipment(returnTrackingNumber)) {
            throw new BusinessException("退货发货确认失败");
        }
        
        orderRepository.save(order);
        return Result.ok("退货发货确认成功");
    }

    @Override
    @Transactional
    public Result completeReturn(String orderId) {
        User currentUser = SecurityUtils.requireCurrentUser();
        Order order = BusinessValidator.requireOrder(orderId, orderRepository);
        BusinessValidator.requireSeller(order, currentUser.getUserId());
        
        // 检查是否可以完成退货
        if (!order.canCompleteReturn()) {
            throw new BusinessException("订单状态不允许完成退货");
        }
        
        // 完成退货
        if (!order.completeReturn()) {
            throw new BusinessException("退货完成失败");
        }
        
        orderRepository.save(order);
        return Result.ok("退货完成成功");
    }

    @Override
    public Result getReturnRequests(Integer page, Integer size, String status) {
        User currentUser = SecurityUtils.requireCurrentUser();
        
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
        
        OrderPageResultVO result = new OrderPageResultVO();
        result.setOrders(orderDTOs);
        result.setTotal(orderPage.getTotalElements());
        result.setPages(orderPage.getTotalPages());
        result.setCurrent(page);
        result.setSize(size);
        
        return Result.ok("获取退货申请列表成功", result);
    }

    @Override
    public Result getMyReturnRecords(Integer page, Integer size, String status) {
        User currentUser = SecurityUtils.requireCurrentUser();
        
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
        
        OrderPageResultVO result = new OrderPageResultVO();
        result.setOrders(orderDTOs);
        result.setTotal(orderPage.getTotalElements());
        result.setPages(orderPage.getTotalPages());
        result.setCurrent(page);
        result.setSize(size);
        
        return Result.ok("获取我的退货记录成功", result);
    }

    // ========== 内部方法 ==========
    @Override
    public Result completeOrder(String orderId) {
        return Result.ok("完成订单成功");
    }

    @Override
    public Result rateOrder(String orderId, Integer rating, String comment) {
        return Result.ok("评价订单成功");
    }

    @Override
    public Result requestReturn(String orderId) {
        return Result.ok("申请退货成功");
    }

    @Override
    public Integer calcValidVolume(String userId) {
        return 0;
    }

    @Override
    public Result queryOriginalCommodity(String orderId) {
        User currentUser = SecurityUtils.requireCurrentUser();
        Order originalOrder = BusinessValidator.requireOrder(orderId, orderRepository);
        BusinessValidator.requireBuyer(originalOrder, currentUser.getUserId());
        
        // 检查商品快照是否存在
        if (originalOrder.getCommoditySnapshotTitle() == null) {
            throw new BusinessException("商品快照信息不存在");
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
        
        // ✅ 检查当前商品状态（使用Feign Client - 公开接口）
        Result commodityResult = commodityQueryClient.getCommodityById(originalOrder.getCommodityId());
        boolean commodityExists = commodityResult.getSuccess() && commodityResult.getData() != null;
        boolean commodityOnShelf = false;
        int currentStock = 0;
        double currentPrice = originalOrder.getCommoditySnapshotPrice();
        
        if (commodityExists) {
            @SuppressWarnings("unchecked")
            Map<String, Object> commodityData = (Map<String, Object>) commodityResult.getData();
            commodityOnShelf = "ON_SHELF".equals(commodityData.get("commodityStatus"));
            Object stockObj = commodityData.get("stock");
            currentStock = stockObj != null ? ((Number) stockObj).intValue() : 0;
            Object priceObj = commodityData.get("price");
            currentPrice = priceObj != null ? ((Number) priceObj).doubleValue() : originalOrder.getCommoditySnapshotPrice();
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
        
        return Result.ok("查询原商品信息成功", result);
    }
    
    @Override
    public Result createOrderFromSnapshot(String orderId, com.njumarket.njumarket.dto.OrderSnapshotDTO orderSnapshotDTO) {
        User currentUser = SecurityUtils.requireCurrentUser();
        Order originalOrder = BusinessValidator.requireOrder(orderId, orderRepository);
        BusinessValidator.requireBuyer(originalOrder, currentUser.getUserId());
        
        // 检查商品快照是否存在
        if (originalOrder.getCommoditySnapshotTitle() == null) {
            throw new BusinessException("商品快照信息不存在");
        }
        
        // ✅ 检查当前商品状态（使用Feign Client - 公开接口）
        Result commodityResult = commodityQueryClient.getCommodityById(originalOrder.getCommodityId());
        if (!commodityResult.getSuccess() || commodityResult.getData() == null) {
            throw new BusinessException("商品不存在");
        }
        // ✅ 使用ObjectMapper正确转换类型（避免ClassCastException）
        CommodityInternalDTO commodityDTO;
        try {
            commodityDTO = objectMapper.convertValue(
                commodityResult.getData(),
                new TypeReference<CommodityInternalDTO>() {}
            );
        } catch (Exception e) {
            log.error("转换CommodityInternalDTO失败: commodityId={}, error={}", 
                originalOrder.getCommodityId(), e.getMessage(), e);
            throw new BusinessException("商品信息解析失败");
        }
        Commodity commodity = convertCommodityDTOToEntity(commodityDTO);
        if (!"ON_SHELF".equals(commodity.getCommodityStatus())) {
            throw new BusinessException("商品未上架");
        }
        
        // 从DTO中获取用户修改的信息（参数验证已由Bean Validation完成）
        Integer quantity = orderSnapshotDTO.getQuantity();
        String shippingAddress = orderSnapshotDTO.getShippingAddress();
        String remark = orderSnapshotDTO.getRemark();
        
        // ✅ 第一步：获取分布式锁（跨服务器保护）
        String lockKey = RedisConstants.LOCK_COMMODITY_KEY + commodity.getCommodityId();
        String lockValue = RedisLockUtil.generateLockValue();
        long lockTimeout = RedisConstants.LOCK_COMMODITY_TTL;
        
        boolean lockAcquired = false;
        try {
            lockAcquired = redisLockUtil.tryLock(lockKey, lockValue, lockTimeout, 1, 100);
            
            if (!lockAcquired) {
                log.warn("获取分布式锁失败 - commodityId: {}", commodity.getCommodityId());
                throw new BusinessException("系统繁忙，请稍后重试");
            }
            
            // ✅ 第二步：使用Feign Client重新查询商品（带悲观锁）
            Result lockedCommodityResult = commodityClient.getCommodityForUpdate(commodity.getCommodityId());
            if (!lockedCommodityResult.getSuccess() || lockedCommodityResult.getData() == null) {
                throw new BusinessException("商品不存在");
            }
            // ✅ 使用ObjectMapper正确转换类型（避免ClassCastException）
            CommodityInternalDTO lockedCommodityDTO;
            try {
                lockedCommodityDTO = objectMapper.convertValue(
                    lockedCommodityResult.getData(),
                    new TypeReference<CommodityInternalDTO>() {}
                );
            } catch (Exception e) {
                log.error("转换CommodityInternalDTO失败: commodityId={}, error={}", 
                    commodity.getCommodityId(), e.getMessage(), e);
                throw new BusinessException("商品信息解析失败");
            }
            Commodity lockedCommodity = convertCommodityDTOToEntity(lockedCommodityDTO);
            
            // 检查库存（在锁定的情况下再次检查）
            if (lockedCommodity.getStock() < quantity) {
                throw new BusinessException("商品库存不足，当前库存：" + lockedCommodity.getStock());
            }
            
            // 检查是否购买自己的商品
            if (lockedCommodity.getSellerId().equals(currentUser.getUserId())) {
                throw new BusinessException("不能购买自己的商品");
            }
            
            // ✅ 第三步：使用Feign Client更新商品库存（三重保护）
            Result updateResult = commodityClient.updateCommodityStock(
                lockedCommodity.getCommodityId(), 
                quantity
            );
            
            if (!updateResult.getSuccess()) {
                log.warn("库存扣减失败 - commodityId: {}, quantity: {}, currentStock: {}", 
                    lockedCommodity.getCommodityId(), quantity, lockedCommodity.getStock());
                throw new BusinessException("商品库存不足，请刷新后重试");
            }
            
            // 计算价格
            double payAmount = lockedCommodity.getPrice() * quantity;
            
            // 创建新订单
            Order newOrder = new Order();
            newOrder.setOrderId(UUID.randomUUID().toString().replace("-", ""));
            newOrder.setBuyerId(currentUser.getUserId());
            newOrder.setSellerId(lockedCommodity.getSellerId());
            newOrder.setCommodityId(lockedCommodity.getCommodityId());
            newOrder.setOrderStatus("CREATED");
            newOrder.setSellerVisibility(originalOrder.getSellerVisibility());
            newOrder.setBuyerVisibility(originalOrder.getBuyerVisibility());
            newOrder.setPayAmount(payAmount);
            newOrder.setQuantity(quantity);
            newOrder.setShippingAddress(shippingAddress != null ? shippingAddress : originalOrder.getShippingAddress());
            newOrder.setRemark(remark != null ? remark : "基于订单快照创建: " + orderId);
            newOrder.setCreateTime(LocalDateTime.now());
            
            // 创建商品快照（使用Feign Client）
            Result sellerResult = authClient.getUserById(lockedCommodity.getSellerId());
            if (sellerResult.getSuccess() && sellerResult.getData() != null) {
                try {
                    // ✅ 使用ObjectMapper正确转换类型（避免ClassCastException）
                    UserInternalDTO sellerDTO = objectMapper.convertValue(
                        sellerResult.getData(),
                        new TypeReference<UserInternalDTO>() {}
                    );
                    // 转换为User实体
                    User seller = convertUserDTOToEntity(sellerDTO);
                    newOrder.createCommoditySnapshot(lockedCommodity, seller);
                } catch (Exception e) {
                    log.error("转换User失败: sellerId={}, error={}", lockedCommodity.getSellerId(), e.getMessage(), e);
                    log.warn("创建订单警告 - sellerId={}, 卖家信息解析失败，无法创建商品快照", lockedCommodity.getSellerId());
                }
            }
            
            // 保存订单
            orderRepository.save(newOrder);
            
            return Result.ok("创建新订单成功", convertToDTOWithProfile(newOrder));
            
        } finally {
            // ✅ 释放分布式锁
            if (lockAcquired) {
                boolean released = redisLockUtil.releaseLock(lockKey, lockValue);
                if (!released) {
                    log.warn("释放分布式锁失败 - commodityId: {}, lockKey: {}", 
                        commodity.getCommodityId(), lockKey);
                }
            }
        }
    }

    @Override
    public Result getOrderHistory(String userId) {
        return Result.ok("获取订单历史成功");
    }
    
    // ========== 批量查询（用于聊天界面） ==========
    
    @Override
    public Result getOrdersBatchStatus(List<String> orderIds) {
        if (orderIds == null || orderIds.isEmpty()) {
            return Result.ok("批量查询成功", Collections.emptyList());
        }
        
        User currentUser = SecurityUtils.requireCurrentUser();
        
        // 去重
        Set<String> uniqueIds = new HashSet<>(orderIds);
        
        // 批量查询订单
        List<Order> orders = orderRepository.findAllById(uniqueIds);
        
        // ✅ 批量查询所有相关的UserProfile（避免N+1查询）
        // 收集所有seller和buyer的userId（去重）
        Set<String> userIds = new HashSet<>();
        for (Order order : orders) {
            if (order.getSellerId() != null) userIds.add(order.getSellerId());
            if (order.getBuyerId() != null) userIds.add(order.getBuyerId());
        }
        
        // ✅ 批量查询profile（使用Feign Client）
        Map<String, UserProfileInternalDTO> profileMap = new HashMap<>();
        if (!userIds.isEmpty()) {
            Result profileResult = authClient.getUserProfilesByIds(new ArrayList<>(userIds));
            if (profileResult.getSuccess() && profileResult.getData() != null) {
                try {
                    List<UserProfileInternalDTO> profiles = objectMapper.convertValue(
                        profileResult.getData(),
                        new TypeReference<List<UserProfileInternalDTO>>() {}
                    );
                    profileMap = profiles.stream()
                        .collect(Collectors.toMap(UserProfileInternalDTO::getUserId, p -> p));
                } catch (Exception e) {
                    log.error("转换UserProfileInternalDTO列表失败: {}", e.getMessage(), e);
                }
            }
        }
        
        // ✅ 转换为轻量级DTO（包含profile信息，并检查权限）
        final Map<String, UserProfileInternalDTO> finalProfileMap = profileMap;
        String currentUserId = currentUser.getUserId();
        List<Map<String, Object>> result = orders.stream()
            .filter(order -> {
                // 权限检查：订单必须属于当前用户（买家或卖家）
                // 添加空值检查，避免NullPointerException
                return (order.getBuyerId() != null && order.getBuyerId().equals(currentUserId)) 
                    || (order.getSellerId() != null && order.getSellerId().equals(currentUserId));
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
                
                // ✅ 添加profile字段（从批量查询的Map中获取）
                UserProfileInternalDTO sellerProfile = finalProfileMap.get(order.getSellerId());
                if (sellerProfile != null) {
                    item.put("sellerNickname", sellerProfile.getNickname());
                    item.put("sellerAvatar", sellerProfile.getAvatar());
                }
                
                UserProfileInternalDTO buyerProfile = finalProfileMap.get(order.getBuyerId());
                if (buyerProfile != null) {
                    item.put("buyerNickname", buyerProfile.getNickname());
                    item.put("buyerAvatar", buyerProfile.getAvatar());
                }
                
                return item;
            })
            .collect(Collectors.toList());
        
        return Result.ok("批量查询成功", result);
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
            
            // ✅ 所有订单变更通知都包含完整OrderDTO（包含profile信息）
            if (orderDTO != null) {
                notification.put("order", orderDTO);
            }
            
            // ✅ WebSocket推送（使用Feign Client调用Message Service）
            try {
                messageClient.pushMessage(userId, "ORDER_CHANGE", notification);
                log.debug("订单变化通知推送: userId={}, orderId={}, changeType={}, orderStatus={}, targetRole={}, hasOrderDTO={}", 
                    userId, orderId, changeType, orderStatus, targetRole, orderDTO != null);
            } catch (Exception e) {
                log.warn("WebSocket推送失败（不影响订单操作）: userId={}, orderId={}, error={}", 
                    userId, orderId, e.getMessage());
            }
            
            // 通过Feign Client更新订单提醒状态
            try {
                authClient.setOrderReminderStatus(userId, targetRole, true);
            } catch (Exception e) {
                log.warn("更新订单提醒状态失败（不影响订单操作）: userId={}, role={}, error={}", 
                    userId, targetRole, e.getMessage());
            }
        } catch (Exception e) {
            log.error("推送订单变化通知失败: userId={}, orderId={}, changeType={}, error={}", 
                    userId, orderId, changeType, e.getMessage(), e);
            // WebSocket 推送失败不影响订单操作的成功返回
        }
    }
    
    // ========== 辅助方法：DTO转Entity ==========
    
    /**
     * 将CommodityInternalDTO转换为Commodity实体
     */
    private Commodity convertCommodityDTOToEntity(CommodityInternalDTO dto) {
        if (dto == null) {
            return null;
        }
        Commodity commodity = new Commodity();
        commodity.setCommodityId(dto.getCommodityId());
        commodity.setSellerId(dto.getSellerId());
        commodity.setTitle(dto.getTitle());
        commodity.setDescription(dto.getDescription());
        // 转换BigDecimal为Double
        commodity.setPrice(dto.getPrice() != null ? dto.getPrice().doubleValue() : null);
        commodity.setStock(dto.getStock());
        commodity.setCategory(dto.getCategory());
        commodity.setConditionLevel(dto.getConditionLevel());
        commodity.setCommodityStatus(dto.getStatus()); // status -> commodityStatus
        commodity.setSellerVisibility(dto.getSellerVisibility());
        commodity.setBuyerVisibility(dto.getBuyerVisibility());
        commodity.setLocation(dto.getLocation());
        commodity.setImages(dto.getImages()); // ✅ 添加图片字段转换
        commodity.setPublishTime(dto.getCreateTime()); // createTime -> publishTime
        // 其他字段使用默认值或null
        commodity.setClickCount(0);
        commodity.setReportCount(0);
        return commodity;
    }
    
    /**
     * 将UserInternalDTO转换为User实体
     */
    private User convertUserDTOToEntity(UserInternalDTO dto) {
        if (dto == null) {
            return null;
        }
        User user = new User();
        user.setUserId(dto.getUserId());
        user.setUsername(dto.getUsername());
        user.setPrimaryPhone(dto.getPrimaryPhone());
        user.setAccountStatus(dto.getAccountStatus());
        user.setRegisterTime(dto.getRegisterTime());
        // 其他字段使用默认值或null
        return user;
    }
    
    /**
     * 处理买家信息并返回结果（用于getOrderDetail方法）
     */
    private Result handleBuyerInfoAndReturn(Order order, OrderDTO orderDTO) {
        // 查询买家详细信息（使用Feign Client）
        Result buyerResult = authClient.getUserById(order.getBuyerId());
        if (buyerResult.getSuccess() && buyerResult.getData() != null) {
            try {
                // ✅ 使用ObjectMapper正确转换类型（避免ClassCastException）
                UserInternalDTO buyerDTO = objectMapper.convertValue(
                    buyerResult.getData(),
                    new TypeReference<UserInternalDTO>() {}
                );
                User buyer = convertUserDTOToEntity(buyerDTO);
                OrderDTO.BuyerInfo buyerInfo = new OrderDTO.BuyerInfo();
                buyerInfo.setUserId(buyer.getUserId());
                buyerInfo.setUsername(buyer.getUsername());
                
                // 查询买家档案信息（使用Feign Client批量查询）
                List<String> buyerIds = Arrays.asList(buyer.getUserId());
                Result buyerProfilesResult = authClient.getUserProfilesByIds(buyerIds);
                if (buyerProfilesResult.getSuccess() && buyerProfilesResult.getData() != null) {
                    try {
                        List<UserProfileInternalDTO> profiles = objectMapper.convertValue(
                            buyerProfilesResult.getData(),
                            new TypeReference<List<UserProfileInternalDTO>>() {}
                        );
                        if (!profiles.isEmpty()) {
                            UserProfileInternalDTO buyerProfile = profiles.get(0);
                            buyerInfo.setNickname(buyerProfile.getNickname());
                            buyerInfo.setAvatar(buyerProfile.getAvatar());
                        } else {
                            buyerInfo.setNickname(buyer.getUsername());
                            buyerInfo.setAvatar(null);
                        }
                    } catch (Exception e) {
                        log.error("转换UserProfileInternalDTO失败: {}", e.getMessage(), e);
                        buyerInfo.setNickname(buyer.getUsername());
                        buyerInfo.setAvatar(null);
                    }
                } else {
                    buyerInfo.setNickname(buyer.getUsername());
                    buyerInfo.setAvatar(null);
                }
                
                buyerInfo.setPhone(buyer.getPrimaryPhone());
                buyerInfo.setEmail(null);
                buyerInfo.setIsDeleted("DELETED".equals(buyer.getAccountStatus()));
                buyerInfo.setStatus(buyerInfo.getIsDeleted() ? "DELETED" : "ACTIVE");
                orderDTO.setBuyer(buyerInfo);
            } catch (Exception e) {
                log.error("转换User失败: buyerId={}, error={}", order.getBuyerId(), e.getMessage(), e);
                // 如果转换失败，创建已注销的买家信息
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
    }
}