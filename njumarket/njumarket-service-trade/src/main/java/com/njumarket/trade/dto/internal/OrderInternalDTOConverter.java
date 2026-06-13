package com.njumarket.trade.dto.internal;

import com.njumarket.trade.entity.Order;
import com.njumarket.njumarket.dto.internal.OrderInternalDTO;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 订单内部 DTO 转换器（Order Service专用）
 * 用于将 Order 实体转换为内部 DTO（用于服务间通信）
 */
@Component
public class OrderInternalDTOConverter {
    
    /**
     * 将 Order 转换为 OrderInternalDTO
     */
    public OrderInternalDTO toInternalDTO(Order order) {
        if (order == null) {
            return null;
        }
        
        OrderInternalDTO dto = new OrderInternalDTO();
        dto.setOrderId(order.getOrderId());
        dto.setCommodityId(order.getCommodityId());
        dto.setSellerId(order.getSellerId());
        dto.setBuyerId(order.getBuyerId());
        dto.setOrderStatus(order.getOrderStatus());
        // 转换Double为BigDecimal
        dto.setPayAmount(order.getPayAmount() != null ? java.math.BigDecimal.valueOf(order.getPayAmount()) : null);
        dto.setQuantity(order.getQuantity());
        dto.setTrackingNumber(order.getTrackingNumber());
        dto.setCreateTime(order.getCreateTime());
        dto.setPayTime(order.getPayTime());
        dto.setShippingTime(order.getShippingTime());
        dto.setDeliveryTime(order.getDeliveryTime());
        return dto;
    }
    
    /**
     * 批量转换 Order 列表
     */
    public List<OrderInternalDTO> toOrderInternalDTOList(List<Order> orders) {
        if (orders == null) {
            return null;
        }
        return orders.stream()
            .map(this::toInternalDTO)
            .collect(Collectors.toList());
    }
}

