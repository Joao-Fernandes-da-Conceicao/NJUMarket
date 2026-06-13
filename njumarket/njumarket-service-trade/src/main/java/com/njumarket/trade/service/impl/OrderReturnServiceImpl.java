package com.njumarket.trade.service.impl;

import com.njumarket.njumarket.dto.Result;
import com.njumarket.njumarket.exception.BusinessException;
import com.njumarket.njumarket.utils.BusinessValidator;
import com.njumarket.njumarket.utils.SecurityUtils;
import com.njumarket.trade.dto.OrderDTO;
import com.njumarket.trade.dto.OrderDTOConverter;
import com.njumarket.trade.entity.Order;
import com.njumarket.njumarket.model.IUser;
import com.njumarket.trade.mq.OrderEventProducer;
import com.njumarket.trade.repository.CommodityRepository;
import com.njumarket.trade.repository.OrderRepository;
import com.njumarket.trade.service.OrderReturnService;
import com.njumarket.trade.utils.OrderValidator;
import com.njumarket.trade.vo.OrderPageResultVO;
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
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderReturnServiceImpl implements OrderReturnService {

    private final OrderRepository orderRepository;
    private final CommodityRepository commodityRepository;
    private final OrderEventProducer orderEventProducer;
    private final OrderDTOConverter orderDTOConverter;

    @Override
    @Transactional
    public Result requestRefund(String orderId, String reason) {
        IUser currentUser = SecurityUtils.requireCurrentUser();
        Order order = OrderValidator.requireOrder(orderId, orderRepository);
        OrderValidator.requireBuyer(order, currentUser.getUserId());
        OrderValidator.requireOrderStatus(order, "COMPLETED", "REFUND_REJECTED");

        order.setOrderStatus("REFUND_REQUESTED");
        order.setReturnReason(reason);
        order.setReturnRequestTime(LocalDateTime.now());

        boolean sellerVisibilityRestored = "HIDDEN".equals(order.getSellerVisibility());
        order.setSellerVisibility("PUBLIC");

        orderRepository.save(order);

        try {
            orderEventProducer.sendOrderEvent(order.getSellerId(), order.getOrderId(), "REFUND_REQUESTED", "SELLER");
            if (sellerVisibilityRestored) {
                orderEventProducer.sendOrderEvent(order.getSellerId(), order.getOrderId(), "ORDER_VISIBILITY_RESTORED", "SELLER");
            }
        } catch (Exception e) {
            log.warn("发送订单事件失败: orderId={}, error={}", order.getOrderId(), e.getMessage());
        }

        return Result.ok("退款申请成功");
    }

    @Override
    @Transactional
    public Result handleRefund(String orderId, String decision, String remark) {
        IUser currentUser = SecurityUtils.requireCurrentUser();
        Order order = OrderValidator.requireOrder(orderId, orderRepository);
        OrderValidator.requireSeller(order, currentUser.getUserId());
        OrderValidator.requireOrderStatus(order, "REFUND_REQUESTED");

        boolean buyerVisibilityRestored = "HIDDEN".equals(order.getBuyerVisibility());

        if ("APPROVE".equals(decision)) {
            order.setOrderStatus("REFUND_APPROVED");
            order.setReturnApprovalTime(LocalDateTime.now());
            order.setBuyerVisibility("PUBLIC");
            int restored = commodityRepository.restoreStock(order.getCommodityId(), order.getQuantity());
            if (restored == 0) log.warn("退款同意，库存恢复失败 - orderId={}, commodityId={}", orderId, order.getCommodityId());
        } else if ("REJECT".equals(decision)) {
            order.setOrderStatus("REFUND_REJECTED");
            order.setReturnRejectionReason(remark);
            order.setReturnApprovalTime(LocalDateTime.now());
            order.setBuyerVisibility("PUBLIC");
        } else {
            throw new BusinessException("无效的处理决定");
        }

        orderRepository.save(order);

        String eventType = "APPROVE".equals(decision) ? "REFUND_APPROVED" : "REFUND_REJECTED";
        try {
            orderEventProducer.sendOrderEvent(order.getBuyerId(), order.getOrderId(), eventType, "BUYER");
            if (buyerVisibilityRestored) {
                orderEventProducer.sendOrderEvent(order.getBuyerId(), order.getOrderId(), "ORDER_VISIBILITY_RESTORED", "BUYER");
            }
        } catch (Exception e) {
            log.warn("发送订单事件失败: orderId={}, error={}", order.getOrderId(), e.getMessage());
        }

        return Result.ok("退款申请处理成功");
    }

    @Override
    @Transactional
    public Result requestReturn(String orderId, String returnReason) {
        IUser currentUser = SecurityUtils.requireCurrentUser();
        Order order = OrderValidator.requireOrder(orderId, orderRepository);
        OrderValidator.requireBuyer(order, currentUser.getUserId());
        if (!order.canRequestReturn()) throw new BusinessException("订单状态不允许申请退货");
        BusinessValidator.requireNotBlank(returnReason, "退货原因不能为空");
        if (!order.requestReturn(returnReason)) throw new BusinessException("退货申请失败");
        orderRepository.save(order);
        return Result.ok("退货申请成功");
    }

    @Override
    @Transactional
    public Result approveReturnRequest(String orderId, Boolean approved, String rejectionReason) {
        IUser currentUser = SecurityUtils.requireCurrentUser();
        Order order = OrderValidator.requireOrder(orderId, orderRepository);
        OrderValidator.requireSeller(order, currentUser.getUserId());
        if (!order.canApproveReturn()) throw new BusinessException("订单状态不允许审批退货");
        if (!approved && !StringUtils.hasText(rejectionReason)) throw new BusinessException("拒绝退货时必须提供拒绝原因");
        if (!order.approveReturnRequest(approved, rejectionReason)) throw new BusinessException("退货审批失败");
        orderRepository.save(order);
        return Result.ok(approved ? "退货申请已同意" : "退货申请已拒绝");
    }

    @Override
    @Transactional
    public Result confirmReturnShipment(String orderId, String returnTrackingNumber) {
        IUser currentUser = SecurityUtils.requireCurrentUser();
        Order order = OrderValidator.requireOrder(orderId, orderRepository);
        OrderValidator.requireBuyer(order, currentUser.getUserId());
        if (!order.canConfirmReturnShipment()) throw new BusinessException("订单状态不允许确认退货发货");
        BusinessValidator.requireNotBlank(returnTrackingNumber, "退货快递单号不能为空");
        if (!order.confirmReturnShipment(returnTrackingNumber)) throw new BusinessException("退货发货确认失败");
        orderRepository.save(order);
        return Result.ok("退货发货确认成功");
    }

    @Override
    @Transactional
    public Result completeReturn(String orderId) {
        IUser currentUser = SecurityUtils.requireCurrentUser();
        Order order = OrderValidator.requireOrder(orderId, orderRepository);
        OrderValidator.requireSeller(order, currentUser.getUserId());
        if (!order.canCompleteReturn()) throw new BusinessException("订单状态不允许完成退货");
        if (!order.completeReturn()) throw new BusinessException("退货完成失败");
        orderRepository.save(order);
        return Result.ok("退货完成成功");
    }

    @Override
    public Result getReturnRequests(Integer page, Integer size, String status) {
        IUser currentUser = SecurityUtils.requireCurrentUser();
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "returnRequestTime"));
        Page<Order> orderPage = StringUtils.hasText(status)
                ? orderRepository.findBySellerIdAndOrderStatus(currentUser.getUserId(), status, pageable)
                : orderRepository.findBySellerIdAndOrderStatusIn(currentUser.getUserId(),
                        Arrays.asList("RETURN_REQUESTED", "RETURN_APPROVED", "RETURN_REJECTED", "RETURN_COMPLETED"), pageable);
        return buildPageResult(orderPage, page, size, "获取退货申请列表成功");
    }

    @Override
    public Result getMyReturnRecords(Integer page, Integer size, String status) {
        IUser currentUser = SecurityUtils.requireCurrentUser();
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "returnRequestTime"));
        Page<Order> orderPage = StringUtils.hasText(status)
                ? orderRepository.findByBuyerIdAndOrderStatus(currentUser.getUserId(), status, pageable)
                : orderRepository.findByBuyerIdAndOrderStatusIn(currentUser.getUserId(),
                        Arrays.asList("RETURN_REQUESTED", "RETURN_APPROVED", "RETURN_REJECTED", "RETURN_COMPLETED"), pageable);
        return buildPageResult(orderPage, page, size, "获取退货记录成功");
    }

    private Result buildPageResult(Page<Order> orderPage, int page, int size, String message) {
        List<OrderDTO> dtos = orderPage.getContent().stream()
                .map(orderDTOConverter::toDTO)
                .collect(Collectors.toList());
        OrderPageResultVO result = new OrderPageResultVO();
        result.setOrders(dtos);
        result.setTotal(orderPage.getTotalElements());
        result.setPages(orderPage.getTotalPages());
        result.setCurrent(page);
        result.setSize(size);
        return Result.ok(message, result);
    }
}
