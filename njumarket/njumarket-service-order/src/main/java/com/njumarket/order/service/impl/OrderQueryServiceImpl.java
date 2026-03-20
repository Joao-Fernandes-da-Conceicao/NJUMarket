package com.njumarket.order.service.impl;

import com.njumarket.njumarket.dto.Result;
import com.njumarket.njumarket.dto.internal.UserInternalDTO;
import com.njumarket.njumarket.dto.internal.UserProfileInternalDTO;
import com.njumarket.njumarket.exception.BusinessException;
import com.njumarket.njumarket.utils.SecurityUtils;
import com.njumarket.order.client.CommodityQueryClient;
import com.njumarket.order.dto.OrderDTO;
import com.njumarket.order.dto.OrderDTOConverter;
import com.njumarket.order.entity.Order;
import com.njumarket.order.entity.User;
import com.njumarket.order.repository.CommodityInventoryRepository;
import com.njumarket.order.repository.OrderRepository;
import com.njumarket.order.service.OrderQueryService;
import com.njumarket.order.service.UserCacheService;
import com.njumarket.order.utils.OrderValidator;
import com.njumarket.order.vo.OrderPageResultVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderQueryServiceImpl implements OrderQueryService {

    private final OrderRepository orderRepository;
    private final CommodityInventoryRepository inventoryRepository;
    private final UserCacheService userCacheService;
    private final CommodityQueryClient commodityQueryClient;
    private final OrderDTOConverter orderDTOConverter;

    @Override
    public Result getOrderDetail(String orderId) {
        User currentUser = (User) SecurityUtils.requireCurrentUser();
        Order order = OrderValidator.requireOrder(orderId, orderRepository);
        OrderValidator.requireBuyerOrSeller(order, currentUser.getUserId());

        OrderDTO orderDTO = orderDTOConverter.toDTOWithProfile(order);

        UserInternalDTO sellerDTO = userCacheService.getUserById(order.getSellerId());
        orderDTO.setSeller(buildSellerInfo(sellerDTO, order.getSellerId()));

        UserInternalDTO buyerDTO = userCacheService.getUserById(order.getBuyerId());
        orderDTO.setBuyer(buildBuyerInfo(buyerDTO, order.getBuyerId()));

        return Result.ok("获取订单详情成功", orderDTO);
    }

    @Override
    public Result getBuyerOrders(Integer page, Integer size, String status) {
        User currentUser = (User) SecurityUtils.requireCurrentUser();
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "createTime"));

        Page<Order> orderPage = StringUtils.hasText(status)
                ? orderRepository.findByBuyerIdAndOrderStatusAndBuyerVisibilityNotHidden(currentUser.getUserId(), status, pageable)
                : orderRepository.findByBuyerIdAndBuyerVisibilityNotHidden(currentUser.getUserId(), pageable);

        return buildPageResult(orderPage, page, size, "获取买家订单列表成功");
    }

    @Override
    public Result getSellerOrders(Integer page, Integer size, String status) {
        User currentUser = (User) SecurityUtils.requireCurrentUser();
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "createTime"));

        Page<Order> orderPage = StringUtils.hasText(status)
                ? orderRepository.findBySellerIdAndOrderStatusAndSellerVisibilityNotHidden(currentUser.getUserId(), status, pageable)
                : orderRepository.findBySellerIdAndSellerVisibilityNotHidden(currentUser.getUserId(), pageable);

        return buildPageResult(orderPage, page, size, "获取卖家订单列表成功");
    }

    @Override
    public Result getOrdersBatchStatus(List<String> orderIds) {
        if (orderIds == null || orderIds.isEmpty()) {
            return Result.ok("批量查询成功", Collections.emptyList());
        }

        User currentUser = (User) SecurityUtils.requireCurrentUser();
        String currentUserId = currentUser.getUserId();

        Set<String> uniqueIds = new HashSet<>(orderIds);
        List<Order> orders = orderRepository.findAllById(uniqueIds);

        Set<String> userIds = new HashSet<>();
        orders.forEach(o -> {
            if (o.getSellerId() != null) userIds.add(o.getSellerId());
            if (o.getBuyerId() != null) userIds.add(o.getBuyerId());
        });
        Map<String, UserProfileInternalDTO> profileMap = userCacheService.getUserProfilesByIds(userIds);

        List<Map<String, Object>> result = orders.stream()
                .filter(o -> (o.getBuyerId() != null && o.getBuyerId().equals(currentUserId))
                        || (o.getSellerId() != null && o.getSellerId().equals(currentUserId)))
                .map(o -> buildBatchItem(o, profileMap))
                .collect(Collectors.toList());

        return Result.ok("批量查询成功", result);
    }

    @Override
    public Result queryOriginalCommodity(String orderId) {
        User currentUser = (User) SecurityUtils.requireCurrentUser();
        Order originalOrder = OrderValidator.requireOrder(orderId, orderRepository);
        OrderValidator.requireBuyer(originalOrder, currentUser.getUserId());

        if (originalOrder.getCommoditySnapshotTitle() == null) throw new BusinessException("商品快照信息不存在");

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

        Result commodityResult = commodityQueryClient.getCommodityById(originalOrder.getCommodityId());
        boolean commodityExists = commodityResult.getSuccess() && commodityResult.getData() != null;
        boolean commodityOnShelf = false;
        double currentPrice = originalOrder.getCommoditySnapshotPrice();

        if (commodityExists) {
            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) commodityResult.getData();
            commodityOnShelf = "ON_SHELF".equals(data.get("commodityStatus"));
            Object priceObj = data.get("price");
            currentPrice = priceObj != null ? ((Number) priceObj).doubleValue() : currentPrice;
        }

        int currentStock = inventoryRepository.findByCommodityId(originalOrder.getCommodityId())
                .map(inv -> inv.getAvailableQuantity()).orElse(0);

        Map<String, Object> result = new HashMap<>();
        result.put("commoditySnapshot", commodityInfo);
        result.put("commodityExists", commodityExists);
        result.put("commodityOnShelf", commodityOnShelf);
        result.put("currentStock", currentStock);
        result.put("currentPrice", currentPrice);
        result.put("isOffShelf", originalOrder.isCommoditySnapshotOffShelf());
        result.put("statusMessage", !commodityExists ? "商品已被删除或不存在"
                : !commodityOnShelf ? "商品已下架" : "商品正常可购买");

        return Result.ok("查询原商品信息成功", result);
    }

    @Override
    public Result getOrderHistory(String userId) {
        return Result.ok("获取订单历史成功");
    }

    @Override
    public Integer calcValidVolume(String userId) {
        return 0;
    }

    // ---- 私有辅助 ----

    private Result buildPageResult(Page<Order> orderPage, int page, int size, String message) {
        List<Order> orders = orderPage.getContent();

        Set<String> userIds = new HashSet<>();
        orders.forEach(o -> {
            if (o.getSellerId() != null) userIds.add(o.getSellerId());
            if (o.getBuyerId() != null) userIds.add(o.getBuyerId());
        });
        Map<String, UserProfileInternalDTO> profileMap = userCacheService.getUserProfilesByIds(userIds);

        List<OrderDTO> dtos = orders.stream()
                .map(o -> orderDTOConverter.toDTOWithProfile(o, profileMap))
                .collect(Collectors.toList());

        OrderPageResultVO result = new OrderPageResultVO();
        result.setOrders(dtos);
        result.setTotal(orderPage.getTotalElements());
        result.setPages(orderPage.getTotalPages());
        result.setCurrent(page);
        result.setSize(size);
        return Result.ok(message, result);
    }

    private Map<String, Object> buildBatchItem(Order o, Map<String, UserProfileInternalDTO> profileMap) {
        Map<String, Object> item = new HashMap<>();
        item.put("orderId", o.getOrderId());
        item.put("orderStatus", o.getOrderStatus());
        item.put("payAmount", o.getPayAmount());
        item.put("quantity", o.getQuantity());
        item.put("commodityId", o.getCommodityId());
        item.put("buyerId", o.getBuyerId());
        item.put("sellerId", o.getSellerId());
        item.put("trackingNumber", o.getTrackingNumber());
        item.put("createTime", o.getCreateTime() != null ? o.getCreateTime().toString() : null);
        item.put("payTime", o.getPayTime() != null ? o.getPayTime().toString() : null);
        item.put("shippingTime", o.getShippingTime() != null ? o.getShippingTime().toString() : null);
        item.put("deliveryTime", o.getDeliveryTime() != null ? o.getDeliveryTime().toString() : null);
        item.put("commoditySnapshotTitle", o.getCommoditySnapshotTitle());
        item.put("commoditySnapshotDescription", o.getCommoditySnapshotDescription());
        item.put("commoditySnapshotPrice", o.getCommoditySnapshotPrice());
        item.put("commoditySnapshotLocation", o.getCommoditySnapshotLocation());
        item.put("commoditySnapshotAddressProvince", o.getCommoditySnapshotAddressProvince());
        item.put("commoditySnapshotAddressCity", o.getCommoditySnapshotAddressCity());
        item.put("commoditySnapshotAddressDistrict", o.getCommoditySnapshotAddressDistrict());
        item.put("commoditySnapshotAddressStreet", o.getCommoditySnapshotAddressStreet());
        item.put("commoditySnapshotAddressDetail", o.getCommoditySnapshotAddressDetail());
        item.put("commoditySnapshotAddressFull", o.getCommoditySnapshotAddressFull());
        item.put("commoditySnapshotCategory", o.getCommoditySnapshotCategory());
        item.put("commoditySnapshotConditionLevel", o.getCommoditySnapshotConditionLevel());
        item.put("commoditySnapshotImages", o.getCommoditySnapshotImages());
        item.put("commoditySnapshotStatus", o.getCommoditySnapshotStatus());
        item.put("commoditySnapshotSellerName", o.getCommoditySnapshotSellerName());
        item.put("commoditySnapshotSellerPhone", o.getCommoditySnapshotSellerPhone());
        item.put("commoditySnapshotSellerEmail", o.getCommoditySnapshotSellerEmail());
        item.put("commoditySnapshotTime", o.getCommoditySnapshotTime() != null ? o.getCommoditySnapshotTime().toString() : null);

        UserProfileInternalDTO sellerProfile = profileMap.get(o.getSellerId());
        if (sellerProfile != null) { item.put("sellerNickname", sellerProfile.getNickname()); item.put("sellerAvatar", sellerProfile.getAvatar()); }
        UserProfileInternalDTO buyerProfile = profileMap.get(o.getBuyerId());
        if (buyerProfile != null) { item.put("buyerNickname", buyerProfile.getNickname()); item.put("buyerAvatar", buyerProfile.getAvatar()); }
        return item;
    }

    private OrderDTO.SellerInfo buildSellerInfo(UserInternalDTO dto, String sellerId) {
        OrderDTO.SellerInfo info = new OrderDTO.SellerInfo();
        if (dto == null) {
            info.setUserId(sellerId); info.setUsername("已注销用户"); info.setNickname("卖家已注销");
            info.setIsDeleted(true); info.setStatus("DELETED"); return info;
        }
        info.setUserId(dto.getUserId()); info.setUsername(dto.getUsername()); info.setPhone(dto.getPrimaryPhone());
        info.setIsDeleted("DELETED".equals(dto.getAccountStatus()));
        info.setStatus(info.getIsDeleted() ? "DELETED" : "ACTIVE");
        Map<String, UserProfileInternalDTO> pm = userCacheService.getUserProfilesByIds(Collections.singletonList(dto.getUserId()));
        UserProfileInternalDTO profile = pm.get(dto.getUserId());
        if (profile != null) { info.setNickname(profile.getNickname()); info.setAvatar(profile.getAvatar()); }
        else { info.setNickname(dto.getUsername()); }
        return info;
    }

    private OrderDTO.BuyerInfo buildBuyerInfo(UserInternalDTO dto, String buyerId) {
        OrderDTO.BuyerInfo info = new OrderDTO.BuyerInfo();
        if (dto == null) {
            info.setUserId(buyerId); info.setUsername("已注销用户"); info.setNickname("买家已注销");
            info.setIsDeleted(true); info.setStatus("DELETED"); return info;
        }
        info.setUserId(dto.getUserId()); info.setUsername(dto.getUsername()); info.setPhone(dto.getPrimaryPhone());
        info.setIsDeleted("DELETED".equals(dto.getAccountStatus()));
        info.setStatus(info.getIsDeleted() ? "DELETED" : "ACTIVE");
        Map<String, UserProfileInternalDTO> pm = userCacheService.getUserProfilesByIds(Collections.singletonList(dto.getUserId()));
        UserProfileInternalDTO profile = pm.get(dto.getUserId());
        if (profile != null) { info.setNickname(profile.getNickname()); info.setAvatar(profile.getAvatar()); }
        else { info.setNickname(dto.getUsername()); }
        return info;
    }
}
