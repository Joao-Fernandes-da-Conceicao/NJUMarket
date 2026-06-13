package com.njumarket.trade.service.impl;

import com.njumarket.njumarket.dto.Result;
import com.njumarket.njumarket.exception.BusinessException;
import com.njumarket.njumarket.utils.SecurityUtils;
import com.njumarket.trade.dto.UpdateOrderAddressDTO;
import com.njumarket.trade.entity.Order;
import com.njumarket.njumarket.model.IUser;
import com.njumarket.trade.repository.OrderRepository;
import com.njumarket.trade.service.OrderManageService;
import com.njumarket.trade.utils.OrderValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderManageServiceImpl implements OrderManageService {

    private final OrderRepository orderRepository;

    private static final Set<String> VALID_VISIBILITY = Set.of("PUBLIC", "PRIVATE", "HIDDEN");

    @Override
    @Transactional
    public Result updateOrderVisibility(String orderId, String visibility) {
        IUser currentUser = SecurityUtils.requireCurrentUser();
        Order order = OrderValidator.requireOrder(orderId, orderRepository);
        OrderValidator.requireBuyerOrSeller(order, currentUser.getUserId());
        if (!order.canModifyVisibility()) throw new BusinessException("订单状态不允许修改可见性");
        if (!VALID_VISIBILITY.contains(visibility)) throw new BusinessException("无效的可见性值");
        if (!order.setVisibility(visibility)) throw new BusinessException("订单可见性修改失败");
        orderRepository.save(order);
        return Result.ok("订单可见性修改成功");
    }

    @Override
    @Transactional
    public Result updateOrderSellerVisibility(String orderId, String sellerVisibility) {
        IUser currentUser = SecurityUtils.requireCurrentUser();
        Order order = OrderValidator.requireOrder(orderId, orderRepository);
        OrderValidator.requireSeller(order, currentUser.getUserId());
        if (!order.canModifyVisibility()) throw new BusinessException("订单状态不允许修改可见性");
        if (!VALID_VISIBILITY.contains(sellerVisibility)) throw new BusinessException("无效的卖家可见性值");
        if (!order.setSellerVisibility(sellerVisibility)) throw new BusinessException("订单卖家可见性修改失败");
        orderRepository.save(order);
        return Result.ok("订单卖家可见性修改成功");
    }

    @Override
    @Transactional
    public Result updateOrderBuyerVisibility(String orderId, String buyerVisibility) {
        IUser currentUser = SecurityUtils.requireCurrentUser();
        Order order = OrderValidator.requireOrder(orderId, orderRepository);
        OrderValidator.requireBuyer(order, currentUser.getUserId());
        if (!order.canModifyVisibility()) throw new BusinessException("订单状态不允许修改可见性");
        if (!VALID_VISIBILITY.contains(buyerVisibility)) throw new BusinessException("无效的买家可见性值");
        if (!order.setBuyerVisibility(buyerVisibility)) throw new BusinessException("订单买家可见性修改失败");
        orderRepository.save(order);
        return Result.ok("订单买家可见性修改成功");
    }

    @Override
    @Transactional
    public Result updateOrderShippingAddress(String orderId, UpdateOrderAddressDTO addressDTO) {
        IUser currentUser = SecurityUtils.requireCurrentUser();
        Order order = OrderValidator.requireOrder(orderId, orderRepository);

        boolean isBuyer = order.getBuyerId().equals(currentUser.getUserId());
        boolean isSeller = order.getSellerId().equals(currentUser.getUserId());
        if (!isBuyer && !isSeller) throw new BusinessException("无权限修改此订单的地址");

        String status = order.getOrderStatus();
        if (!"CREATED".equals(status) && !"PAID".equals(status)) {
            throw new BusinessException("订单状态不允许修改地址，只能在未发货和未支付阶段修改");
        }

        if (isBuyer) {
            order.setShippingAddressSnapshotProvince(addressDTO.getProvince());
            order.setShippingAddressSnapshotCity(addressDTO.getCity());
            order.setShippingAddressSnapshotDistrict(addressDTO.getDistrict());
            order.setShippingAddressSnapshotStreet(addressDTO.getStreetAddress());
            order.setShippingAddressSnapshotDetail(addressDTO.getDetailAddress());
            order.setShippingAddressSnapshotFull(addressDTO.getFullAddress());
            order.setShippingAddressSnapshotRecipientName(addressDTO.getRecipientName());
            order.setShippingAddressSnapshotRecipientPhone(addressDTO.getRecipientPhone());
            if (StringUtils.hasText(addressDTO.getAddressId())) {
                order.setShippingAddressId(addressDTO.getAddressId());
            }
            order.setShippingAddress(addressDTO.getFullAddress());
        } else {
            order.setCommoditySnapshotAddressProvince(addressDTO.getProvince());
            order.setCommoditySnapshotAddressCity(addressDTO.getCity());
            order.setCommoditySnapshotAddressDistrict(addressDTO.getDistrict());
            order.setCommoditySnapshotAddressStreet(addressDTO.getStreetAddress());
            order.setCommoditySnapshotAddressDetail(addressDTO.getDetailAddress());
            order.setCommoditySnapshotAddressFull(addressDTO.getFullAddress());
        }

        orderRepository.save(order);
        log.info("更新订单地址成功 - orderId={}, userId={}, role={}", orderId, currentUser.getUserId(), isBuyer ? "buyer" : "seller");
        return Result.ok("订单地址更新成功");
    }
}
