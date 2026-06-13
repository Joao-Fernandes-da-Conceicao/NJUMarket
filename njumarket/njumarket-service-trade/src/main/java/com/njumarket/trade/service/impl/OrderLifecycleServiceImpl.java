package com.njumarket.trade.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.njumarket.njumarket.dto.Result;
import com.njumarket.njumarket.dto.internal.CommodityInternalDTO;
import com.njumarket.njumarket.dto.internal.UserInternalDTO;
import com.njumarket.njumarket.exception.BusinessException;
import com.njumarket.njumarket.utils.SecurityUtils;
import com.njumarket.trade.service.CommodityQueryService;
import com.njumarket.trade.client.AuthClient;
import com.njumarket.trade.dto.OrderDTO;
import com.njumarket.trade.dto.OrderDTOConverter;
import com.njumarket.trade.dto.OrderSnapshotDTO;
import com.njumarket.trade.entity.Order;
import com.njumarket.njumarket.model.IUser;
import com.njumarket.trade.mq.OrderEventProducer;
import com.njumarket.trade.repository.CommodityRepository;
import com.njumarket.trade.repository.OrderRepository;
import com.njumarket.trade.service.OrderLifecycleService;
import com.njumarket.trade.service.UserCacheService;
import com.njumarket.trade.utils.OrderValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderLifecycleServiceImpl implements OrderLifecycleService {

    private final OrderRepository orderRepository;
    private final CommodityRepository commodityRepository;
    private final AuthClient authClient;
    private final CommodityQueryService commodityQueryService;
    private final ObjectMapper objectMapper;
    private final UserCacheService userCacheService;
    private final OrderEventProducer orderEventProducer;
    private final OrderDTOConverter orderDTOConverter;

    @Override
    @Transactional
    public Result createOrder(OrderDTO orderDTO) {
        IUser currentUser = SecurityUtils.requireCurrentUser();

        CommodityInternalDTO commodityDTO = commodityQueryService.getCommodityByIdInternal(orderDTO.getCommodityId());

        if (!"ON_SHELF".equals(commodityDTO.getStatus())) {
            throw new BusinessException("商品未上架，无法购买");
        }
        if (commodityDTO.getSellerId().equals(currentUser.getUserId())) {
            throw new BusinessException("不能购买自己的商品");
        }
        double expectedAmount = (commodityDTO.getPrice() != null ? commodityDTO.getPrice().doubleValue() : 0.0) * orderDTO.getQuantity();
        if (Math.abs(orderDTO.getPayAmount() - expectedAmount) > 0.01) {
            throw new BusinessException("支付金额与商品价格不符");
        }

        int deducted = commodityRepository.updateStockWithCondition(orderDTO.getCommodityId(), orderDTO.getQuantity());
        if (deducted == 0) {
            throw new BusinessException("商品库存不足，请刷新后重试");
        }

        Order order = new Order();
        order.setOrderId(UUID.randomUUID().toString().replace("-", ""));
        order.setBuyerId(currentUser.getUserId());
        order.setSellerId(commodityDTO.getSellerId());
        order.setCommodityId(commodityDTO.getCommodityId());
        order.setOrderStatus("CREATED");
        order.setSellerVisibility(orderDTO.getSellerVisibility() != null ? orderDTO.getSellerVisibility() : "PUBLIC");
        order.setBuyerVisibility(orderDTO.getBuyerVisibility() != null ? orderDTO.getBuyerVisibility() : "PUBLIC");
        order.setPayAmount(orderDTO.getPayAmount());
        order.setQuantity(orderDTO.getQuantity());
        order.setShippingAddress(orderDTO.getShippingAddress());
        order.setRemark(orderDTO.getRemark());
        order.setCreateTime(LocalDateTime.now());

        // 收货地址快照
        try {
            boolean hasSnapshotFields = StringUtils.hasText(orderDTO.getShippingAddressSnapshotProvince())
                    || StringUtils.hasText(orderDTO.getShippingAddressSnapshotCity())
                    || StringUtils.hasText(orderDTO.getShippingAddressSnapshotFull());
            if (hasSnapshotFields) {
                order.setShippingAddressSnapshotProvince(orderDTO.getShippingAddressSnapshotProvince());
                order.setShippingAddressSnapshotCity(orderDTO.getShippingAddressSnapshotCity());
                order.setShippingAddressSnapshotDistrict(orderDTO.getShippingAddressSnapshotDistrict());
                order.setShippingAddressSnapshotStreet(orderDTO.getShippingAddressSnapshotStreet());
                order.setShippingAddressSnapshotDetail(orderDTO.getShippingAddressSnapshotDetail());
                order.setShippingAddressSnapshotFull(orderDTO.getShippingAddressSnapshotFull());
                order.setShippingAddressSnapshotRecipientName(orderDTO.getShippingAddressSnapshotRecipientName());
                order.setShippingAddressSnapshotRecipientPhone(orderDTO.getShippingAddressSnapshotRecipientPhone());
                if (StringUtils.hasText(orderDTO.getShippingAddressId())) {
                    order.setShippingAddressId(orderDTO.getShippingAddressId());
                }
            } else {
                fillShippingAddressFromRemote(order, orderDTO, currentUser.getUserId());
            }
        } catch (Exception e) {
            log.warn("地址快照获取失败，降级使用 shippingAddress: userId={}, error={}", currentUser.getUserId(), e.getMessage());
            if (StringUtils.hasText(orderDTO.getShippingAddress())) {
                order.setShippingAddressSnapshotFull(orderDTO.getShippingAddress());
            }
        }

        // 商品快照
        UserInternalDTO sellerDTO = userCacheService.getUserById(commodityDTO.getSellerId());
        if (sellerDTO != null) {
            try {
                String commodityLocation = StringUtils.hasText(commodityDTO.getAddressSnapshotFull())
                        ? commodityDTO.getAddressSnapshotFull() : commodityDTO.getLocation();
                order.createCommoditySnapshot(
                        commodityDTO.getTitle(), commodityDTO.getDescription(),
                        commodityDTO.getPrice() != null ? commodityDTO.getPrice().doubleValue() : null,
                        commodityLocation, commodityDTO.getCategory(), commodityDTO.getConditionLevel(),
                        commodityDTO.getImages(), commodityDTO.getStatus(),
                        sellerDTO.getUsername() != null ? sellerDTO.getUsername() : "",
                        sellerDTO.getPrimaryPhone() != null ? sellerDTO.getPrimaryPhone() : "", "",
                        commodityDTO.getAddressSnapshotProvince(), commodityDTO.getAddressSnapshotCity(),
                        commodityDTO.getAddressSnapshotDistrict(), commodityDTO.getAddressSnapshotStreet(),
                        commodityDTO.getAddressSnapshotDetail(), commodityDTO.getAddressSnapshotFull()
                );
            } catch (Exception e) {
                log.warn("商品快照创建失败: sellerId={}, error={}", commodityDTO.getSellerId(), e.getMessage());
            }
        }

        orderRepository.save(order);

        try { orderEventProducer.sendOrderEvent(order.getSellerId(), order.getOrderId(), "ORDER_CREATED", "SELLER"); }
        catch (Exception e) { log.warn("发送订单事件失败: orderId={}, error={}", order.getOrderId(), e.getMessage()); }
        try { authClient.setOrderReminderStatus(order.getSellerId(), "SELLER", true); }
        catch (Exception e) { log.warn("更新订单提醒状态失败: userId={}, error={}", order.getSellerId(), e.getMessage()); }

        return Result.ok("订单创建成功");
    }

    @Override
    @Transactional
    public Result payOrder(String orderId) {
        IUser currentUser = SecurityUtils.requireCurrentUser();
        Order order = OrderValidator.requireOrder(orderId, orderRepository);
        if (!order.getBuyerId().equals(currentUser.getUserId())) throw new BusinessException("无权限支付此订单");
        if (!"CREATED".equals(order.getOrderStatus())) throw new BusinessException("订单状态异常，无法支付");

        if (!order.payOrder()) throw new BusinessException("订单支付失败");
        orderRepository.save(order);

        try { orderEventProducer.sendOrderEvent(order.getSellerId(), order.getOrderId(), "ORDER_PAID", "SELLER"); }
        catch (Exception e) { log.warn("发送订单事件失败: orderId={}, error={}", order.getOrderId(), e.getMessage()); }
        try { authClient.setOrderReminderStatus(order.getSellerId(), "SELLER", true); }
        catch (Exception e) { log.warn("更新订单提醒状态失败: userId={}, error={}", order.getSellerId(), e.getMessage()); }

        return Result.ok("订单支付成功");
    }

    @Override
    @Transactional
    public Result confirmOrder(String orderId) {
        IUser currentUser = SecurityUtils.requireCurrentUser();
        Order order = OrderValidator.requireOrder(orderId, orderRepository);
        if (!order.getBuyerId().equals(currentUser.getUserId())) throw new BusinessException("无权限确认收货此订单");
        if (!"SHIPPED".equals(order.getOrderStatus())) throw new BusinessException("订单状态异常，无法确认收货");

        if (!order.completeOrder()) throw new BusinessException("订单确认收货失败");
        orderRepository.save(order);

        try { orderEventProducer.sendOrderEvent(order.getSellerId(), order.getOrderId(), "ORDER_COMPLETED", "SELLER"); }
        catch (Exception e) { log.warn("发送订单事件失败: orderId={}, error={}", order.getOrderId(), e.getMessage()); }
        try { authClient.setOrderReminderStatus(order.getSellerId(), "SELLER", true); }
        catch (Exception e) { log.warn("更新订单提醒状态失败: userId={}, error={}", order.getSellerId(), e.getMessage()); }

        return Result.ok("订单确认收货成功");
    }

    @Override
    @Transactional
    public Result cancelOrder(String orderId, String reason) {
        IUser currentUser = SecurityUtils.requireCurrentUser();
        Order order = OrderValidator.requireOrder(orderId, orderRepository);
        if (!order.getBuyerId().equals(currentUser.getUserId()) && !order.getSellerId().equals(currentUser.getUserId())) {
            throw new BusinessException("无权限取消此订单");
        }
        if (!"CREATED".equals(order.getOrderStatus()) && !"PAID".equals(order.getOrderStatus())) {
            throw new BusinessException("订单状态不允许取消");
        }

        boolean shouldRestoreStock = order.canRestoreStock();
        if (!order.cancelOrder()) throw new BusinessException("订单取消失败");

        if (shouldRestoreStock) {
            int restored = commodityRepository.restoreStock(order.getCommodityId(), order.getQuantity());
            if (restored == 0) {
                log.warn("订单取消，但库存恢复失败 - orderId={}, commodityId={}", order.getOrderId(), order.getCommodityId());
            }
        }

        orderRepository.save(order);

        try {
            boolean buyerCancelled = order.getBuyerId().equals(currentUser.getUserId());
            String notifyUserId = buyerCancelled ? order.getSellerId() : order.getBuyerId();
            String recipientRole  = buyerCancelled ? "SELLER" : "BUYER";
            orderEventProducer.sendOrderEvent(notifyUserId, order.getOrderId(), "ORDER_CANCELLED", recipientRole);
        } catch (Exception e) { log.warn("发送订单事件失败: orderId={}, error={}", order.getOrderId(), e.getMessage()); }

        return Result.ok("订单取消成功");
    }

    @Override
    @Transactional
    public Result shipOrder(String orderId, String trackingNumber) {
        IUser currentUser = SecurityUtils.requireCurrentUser();
        Order order = OrderValidator.requireOrder(orderId, orderRepository);
        OrderValidator.requireSeller(order, currentUser.getUserId());
        OrderValidator.requireOrderStatus(order, "PAID");

        if (!order.shipOrder(trackingNumber)) throw new BusinessException("订单发货失败");
        orderRepository.save(order);

        try { orderEventProducer.sendOrderEvent(order.getBuyerId(), order.getOrderId(), "ORDER_SHIPPED", "BUYER"); }
        catch (Exception e) { log.warn("发送订单事件失败: orderId={}, error={}", order.getOrderId(), e.getMessage()); }
        try { authClient.setOrderReminderStatus(order.getBuyerId(), "BUYER", true); }
        catch (Exception e) { log.warn("更新订单提醒状态失败: userId={}, error={}", order.getBuyerId(), e.getMessage()); }

        return Result.ok("订单发货成功");
    }

    @Override
    public Result completeOrder(String orderId) {
        return Result.ok("完成订单成功");
    }

    @Override
    public Result rateOrder(String orderId, Integer rating, String comment) {
        return Result.ok("评价订单成功");
    }

    @Override
    @Transactional
    public Result createOrderFromSnapshot(String orderId, OrderSnapshotDTO orderSnapshotDTO) {
        IUser currentUser = SecurityUtils.requireCurrentUser();
        Order originalOrder = OrderValidator.requireOrder(orderId, orderRepository);
        OrderValidator.requireBuyer(originalOrder, currentUser.getUserId());

        if (originalOrder.getCommoditySnapshotTitle() == null) throw new BusinessException("商品快照信息不存在");

        CommodityInternalDTO commodityDTO = commodityQueryService.getCommodityByIdInternal(originalOrder.getCommodityId());

        if (!"ON_SHELF".equals(commodityDTO.getStatus())) throw new BusinessException("商品未上架");
        if (commodityDTO.getSellerId().equals(currentUser.getUserId())) throw new BusinessException("不能购买自己的商品");

        Integer quantity = orderSnapshotDTO.getQuantity();
        int deducted = commodityRepository.updateStockWithCondition(commodityDTO.getCommodityId(), quantity);
        if (deducted == 0) throw new BusinessException("商品库存不足，请刷新后重试");

        double payAmount = (commodityDTO.getPrice() != null ? commodityDTO.getPrice().doubleValue() : 0.0) * quantity;

        Order newOrder = new Order();
        newOrder.setOrderId(UUID.randomUUID().toString().replace("-", ""));
        newOrder.setBuyerId(currentUser.getUserId());
        newOrder.setSellerId(commodityDTO.getSellerId());
        newOrder.setCommodityId(commodityDTO.getCommodityId());
        newOrder.setOrderStatus("CREATED");
        newOrder.setSellerVisibility(originalOrder.getSellerVisibility());
        newOrder.setBuyerVisibility(originalOrder.getBuyerVisibility());
        newOrder.setPayAmount(payAmount);
        newOrder.setQuantity(quantity);
        newOrder.setShippingAddress(orderSnapshotDTO.getShippingAddress() != null
                ? orderSnapshotDTO.getShippingAddress() : originalOrder.getShippingAddress());
        newOrder.setRemark(orderSnapshotDTO.getRemark() != null
                ? orderSnapshotDTO.getRemark() : "基于订单快照创建: " + orderId);
        newOrder.setCreateTime(LocalDateTime.now());

        UserInternalDTO snapshotSellerDTO = userCacheService.getUserById(commodityDTO.getSellerId());
        if (snapshotSellerDTO != null) {
            String location = StringUtils.hasText(commodityDTO.getAddressSnapshotFull())
                    ? commodityDTO.getAddressSnapshotFull() : commodityDTO.getLocation();
            newOrder.createCommoditySnapshot(
                    commodityDTO.getTitle(), commodityDTO.getDescription(),
                    commodityDTO.getPrice() != null ? commodityDTO.getPrice().doubleValue() : null,
                    location, commodityDTO.getCategory(), commodityDTO.getConditionLevel(),
                    commodityDTO.getImages(), commodityDTO.getStatus(),
                    snapshotSellerDTO.getUsername() != null ? snapshotSellerDTO.getUsername() : "",
                    snapshotSellerDTO.getPrimaryPhone() != null ? snapshotSellerDTO.getPrimaryPhone() : "", "",
                    commodityDTO.getAddressSnapshotProvince(), commodityDTO.getAddressSnapshotCity(),
                    commodityDTO.getAddressSnapshotDistrict(), commodityDTO.getAddressSnapshotStreet(),
                    commodityDTO.getAddressSnapshotDetail(), commodityDTO.getAddressSnapshotFull()
            );
        }

        orderRepository.save(newOrder);

        try { orderEventProducer.sendOrderEvent(newOrder.getSellerId(), newOrder.getOrderId(), "ORDER_CREATED", "SELLER"); }
        catch (Exception e) { log.warn("发送订单事件失败: orderId={}, error={}", newOrder.getOrderId(), e.getMessage()); }

        return Result.ok("创建新订单成功", orderDTOConverter.toDTOWithProfile(newOrder));
    }

    // ---- 私有辅助 ----

    private void fillShippingAddressFromRemote(Order order, OrderDTO orderDTO, String userId) {
        com.njumarket.njumarket.dto.internal.AddressInternalDTO addressDTO = null;
        if (StringUtils.hasText(orderDTO.getShippingAddressId())) {
            Result r = authClient.getAddressById(orderDTO.getShippingAddressId());
            if (r.getSuccess() && r.getData() != null) {
                try {
                    addressDTO = objectMapper.convertValue(r.getData(),
                            new TypeReference<com.njumarket.njumarket.dto.internal.AddressInternalDTO>() {});
                    order.setShippingAddressId(orderDTO.getShippingAddressId());
                } catch (Exception ignored) {}
            }
        }
        if (addressDTO == null) {
            Result r = authClient.getDefaultAddress(userId);
            if (r.getSuccess() && r.getData() != null) {
                try {
                    addressDTO = objectMapper.convertValue(r.getData(),
                            new TypeReference<com.njumarket.njumarket.dto.internal.AddressInternalDTO>() {});
                    if (addressDTO != null && StringUtils.hasText(addressDTO.getAddressId())) {
                        order.setShippingAddressId(addressDTO.getAddressId());
                    }
                } catch (Exception ignored) {}
            }
        }
        if (addressDTO != null) {
            order.setShippingAddressSnapshotProvince(addressDTO.getProvince());
            order.setShippingAddressSnapshotCity(addressDTO.getCity());
            order.setShippingAddressSnapshotDistrict(addressDTO.getDistrict());
            order.setShippingAddressSnapshotStreet(addressDTO.getStreetAddress());
            order.setShippingAddressSnapshotDetail(addressDTO.getDetailAddress());
            order.setShippingAddressSnapshotFull(addressDTO.getFullAddress());
            order.setShippingAddressSnapshotRecipientName(addressDTO.getRecipientName());
            order.setShippingAddressSnapshotRecipientPhone(addressDTO.getRecipientPhone());
        } else if (StringUtils.hasText(orderDTO.getShippingAddress())) {
            order.setShippingAddressSnapshotFull(orderDTO.getShippingAddress());
        }
    }
}
