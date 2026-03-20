package com.njumarket.order.dto;

import com.njumarket.njumarket.dto.internal.UserProfileInternalDTO;
import com.njumarket.order.entity.Order;
import com.njumarket.order.service.UserCacheService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * 订单 DTO 转换器
 * 将 Order 实体转为前端可用的 OrderDTO，支持批量 profile 注入或单条懒查询。
 */
@Component
@RequiredArgsConstructor
public class OrderDTOConverter {

    private final UserCacheService userCacheService;

    /**
     * 基础转换（不含 profile 信息）
     */
    public OrderDTO toDTO(Order order) {
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
        dto.setShippingAddressId(order.getShippingAddressId());

        dto.setShippingAddressSnapshotProvince(order.getShippingAddressSnapshotProvince());
        dto.setShippingAddressSnapshotCity(order.getShippingAddressSnapshotCity());
        dto.setShippingAddressSnapshotDistrict(order.getShippingAddressSnapshotDistrict());
        dto.setShippingAddressSnapshotStreet(order.getShippingAddressSnapshotStreet());
        dto.setShippingAddressSnapshotDetail(order.getShippingAddressSnapshotDetail());
        dto.setShippingAddressSnapshotFull(order.getShippingAddressSnapshotFull());
        dto.setShippingAddressSnapshotRecipientName(order.getShippingAddressSnapshotRecipientName());
        dto.setShippingAddressSnapshotRecipientPhone(order.getShippingAddressSnapshotRecipientPhone());

        dto.setTrackingNumber(order.getTrackingNumber());
        dto.setRemark(order.getRemark());
        dto.setCreateTime(order.getCreateTime() != null ? order.getCreateTime().toString() : null);
        dto.setPayTime(order.getPayTime() != null ? order.getPayTime().toString() : null);
        dto.setShippingTime(order.getShippingTime() != null ? order.getShippingTime().toString() : null);
        dto.setDeliveryTime(order.getDeliveryTime() != null ? order.getDeliveryTime().toString() : null);

        dto.setReturnReason(order.getReturnReason());
        dto.setReturnRequestTime(order.getReturnRequestTime() != null ? order.getReturnRequestTime().toString() : null);
        dto.setReturnApprovalTime(order.getReturnApprovalTime() != null ? order.getReturnApprovalTime().toString() : null);
        dto.setReturnRejectionReason(order.getReturnRejectionReason());
        dto.setReturnTrackingNumber(order.getReturnTrackingNumber());
        dto.setReturnCompletionTime(order.getReturnCompletionTime() != null ? order.getReturnCompletionTime().toString() : null);

        dto.setCommoditySnapshotTitle(order.getCommoditySnapshotTitle());
        dto.setCommoditySnapshotDescription(order.getCommoditySnapshotDescription());
        dto.setCommoditySnapshotPrice(order.getCommoditySnapshotPrice());
        dto.setCommoditySnapshotLocation(order.getCommoditySnapshotLocation());
        dto.setCommoditySnapshotAddressProvince(order.getCommoditySnapshotAddressProvince());
        dto.setCommoditySnapshotAddressCity(order.getCommoditySnapshotAddressCity());
        dto.setCommoditySnapshotAddressDistrict(order.getCommoditySnapshotAddressDistrict());
        dto.setCommoditySnapshotAddressStreet(order.getCommoditySnapshotAddressStreet());
        dto.setCommoditySnapshotAddressDetail(order.getCommoditySnapshotAddressDetail());
        dto.setCommoditySnapshotAddressFull(order.getCommoditySnapshotAddressFull());
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
     * 含 profile 信息转换（批量预查询场景，避免 N+1）
     */
    public OrderDTO toDTOWithProfile(Order order, Map<String, UserProfileInternalDTO> profileMap) {
        OrderDTO dto = toDTO(order);

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

        return dto;
    }

    /**
     * 含 profile 信息转换（单条查询场景，懒加载 profile）
     */
    public OrderDTO toDTOWithProfile(Order order) {
        List<String> userIds = Arrays.asList(order.getSellerId(), order.getBuyerId());
        Map<String, UserProfileInternalDTO> profileMap = userCacheService.getUserProfilesByIds(userIds);
        return toDTOWithProfile(order, profileMap);
    }
}
