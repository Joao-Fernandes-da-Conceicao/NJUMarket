package com.njumarket.njumarket.dto.internal;

import com.njumarket.njumarket.entity.Commodity;
import com.njumarket.njumarket.entity.Order;
import com.njumarket.njumarket.entity.User;
import com.njumarket.njumarket.entity.UserProfile;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 内部 DTO 转换器
 * 用于将实体类转换为内部 DTO（用于服务间通信）
 */
@Component
public class InternalDTOConverter {
    
    /**
     * 将 UserProfile 转换为 UserProfileInternalDTO
     */
    public UserProfileInternalDTO toInternalDTO(UserProfile profile) {
        if (profile == null) {
            return null;
        }
        
        UserProfileInternalDTO dto = new UserProfileInternalDTO();
        dto.setProfileId(profile.getProfileId());
        dto.setUserId(profile.getUserId());
        dto.setNickname(profile.getNickname());
        dto.setAvatar(profile.getAvatar());
        // location 和 bio 字段在 UserProfile 实体中不存在，设置为 null
        dto.setLocation(null);
        dto.setBio(null);
        dto.setSellerOrderHasNew(profile.getSellerOrderHasNew());
        dto.setBuyerOrderHasNew(profile.getBuyerOrderHasNew());
        dto.setCreditScore(profile.getCreditScore());
        dto.setBuyerRating(profile.getBuyerRating());
        dto.setSellerRating(profile.getSellerRating());
        return dto;
    }
    
    /**
     * 批量转换 UserProfile 列表
     */
    public List<UserProfileInternalDTO> toUserProfileInternalDTOList(List<UserProfile> profiles) {
        if (profiles == null) {
            return null;
        }
        return profiles.stream()
            .map(this::toInternalDTO)
            .collect(Collectors.toList());
    }
    
    /**
     * 将 User 转换为 UserInternalDTO
     */
    public UserInternalDTO toInternalDTO(User user) {
        if (user == null) {
            return null;
        }
        
        UserInternalDTO dto = new UserInternalDTO();
        dto.setUserId(user.getUserId());
        dto.setUsername(user.getUsername());
        dto.setPrimaryPhone(user.getPrimaryPhone());
        dto.setAccountStatus(user.getAccountStatus());
        dto.setRegisterTime(user.getRegisterTime());
        return dto;
    }
    
    /**
     * 批量转换 User 列表
     */
    public List<UserInternalDTO> toUserInternalDTOList(List<User> users) {
        if (users == null) {
            return null;
        }
        return users.stream()
            .map(this::toInternalDTO)
            .collect(Collectors.toList());
    }
    
    /**
     * 将 Commodity 转换为 CommodityInternalDTO
     */
    public CommodityInternalDTO toInternalDTO(Commodity commodity) {
        if (commodity == null) {
            return null;
        }
        
        CommodityInternalDTO dto = new CommodityInternalDTO();
        dto.setCommodityId(commodity.getCommodityId());
        dto.setSellerId(commodity.getSellerId());
        dto.setTitle(commodity.getTitle());
        dto.setDescription(commodity.getDescription());
        // 转换Double为BigDecimal
        dto.setPrice(commodity.getPrice() != null ? java.math.BigDecimal.valueOf(commodity.getPrice()) : null);
        dto.setStock(commodity.getStock());
        dto.setCategory(commodity.getCategory());
        dto.setConditionLevel(commodity.getConditionLevel());
        dto.setStatus(commodity.getCommodityStatus()); // 使用commodityStatus
        dto.setSellerVisibility(commodity.getSellerVisibility());
        dto.setBuyerVisibility(commodity.getBuyerVisibility());
        dto.setLocation(commodity.getLocation());
        dto.setImages(commodity.getImages()); // ✅ 添加图片字段转换
        dto.setCreateTime(commodity.getPublishTime()); // 使用publishTime作为createTime
        dto.setUpdateTime(null); // Commodity实体没有updateTime字段
        return dto;
    }
    
    /**
     * 批量转换 Commodity 列表
     */
    public List<CommodityInternalDTO> toCommodityInternalDTOList(List<Commodity> commodities) {
        if (commodities == null) {
            return null;
        }
        return commodities.stream()
            .map(this::toInternalDTO)
            .collect(Collectors.toList());
    }
    
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
        dto.setSellerVisibility(order.getSellerVisibility());
        dto.setBuyerVisibility(order.getBuyerVisibility());
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

