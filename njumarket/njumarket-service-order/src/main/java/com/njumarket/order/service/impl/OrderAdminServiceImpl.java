package com.njumarket.order.service.impl;

import com.njumarket.njumarket.dto.Result;
import com.njumarket.njumarket.dto.internal.OrderInternalDTO;
import com.njumarket.njumarket.exception.BusinessException;
import com.njumarket.order.dto.internal.OrderInternalDTOConverter;
import com.njumarket.order.entity.Order;
import com.njumarket.order.repository.OrderRepository;
import com.njumarket.order.service.OrderAdminService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderAdminServiceImpl implements OrderAdminService {

    private final OrderRepository orderRepository;
    private final OrderInternalDTOConverter orderInternalDTOConverter;

    @Override
    public Result getOrderByIdInternal(String orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException("订单不存在"));
        OrderInternalDTO dto = orderInternalDTOConverter.toInternalDTO(order);
        return Result.ok("查询成功", dto);
    }

    @Override
    @Transactional
    public Result updateOrderFull(String orderId, Map<String, Object> payload) {
        Order o = orderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException("订单不存在"));

        Object orderStatus = payload.get("orderStatus");
        if (orderStatus instanceof String) {
            String st = ((String) orderStatus).trim();
            Set<String> allowed = new HashSet<>(Arrays.asList(
                    "PENDING", "PAID", "SHIPPED", "DELIVERED", "COMPLETED", "CANCELLED", "REFUNDED"));
            if (allowed.contains(st)) o.setOrderStatus(st);
        }
        Object trackingNumber = payload.get("trackingNumber");
        if (trackingNumber instanceof String) o.setTrackingNumber(((String) trackingNumber).trim());
        Object remark = payload.get("remark");
        if (remark instanceof String) o.setRemark(((String) remark).trim());

        Set<String> allowedVis = new HashSet<>(Arrays.asList("PUBLIC", "PRIVATE", "HIDDEN"));
        Object sellerVisibility = payload.get("sellerVisibility");
        if (sellerVisibility instanceof String) {
            String vis = ((String) sellerVisibility).trim();
            if (allowedVis.contains(vis)) o.setSellerVisibility(vis);
        }
        Object buyerVisibility = payload.get("buyerVisibility");
        if (buyerVisibility instanceof String) {
            String vis = ((String) buyerVisibility).trim();
            if (allowedVis.contains(vis)) o.setBuyerVisibility(vis);
        }

        orderRepository.save(o);
        log.info("管理端全量更新订单 - orderId={}", orderId);
        return Result.ok("更新成功", o);
    }

    @Override
    @Transactional
    public Result deleteOrderInternal(String orderId) {
        if (!orderRepository.existsById(orderId)) throw new BusinessException("订单不存在");
        orderRepository.deleteById(orderId);
        log.info("管理端删除订单 - orderId={}", orderId);
        return Result.ok("删除成功");
    }

    @Override
    public Result checkCommodityHasOrders(String commodityId) {
        List<Order> orders = orderRepository.findByCommodityId(commodityId);
        if (orders != null && !orders.isEmpty()) throw new BusinessException("该商品已有订单，无法删除");
        return Result.ok("该商品没有订单");
    }

    @Override
    public Result listOrdersInternal(Integer page, Integer size, String keyword, String status,
                                     String sellerVisibility, String buyerVisibility,
                                     String sortProp, String sortOrder) {
        Sort sort = Sort.by(Sort.Direction.DESC, "createTime");
        if (StringUtils.hasText(sortProp)) {
            Sort.Direction dir = "desc".equalsIgnoreCase(sortOrder) ? Sort.Direction.DESC : Sort.Direction.ASC;
            sort = Sort.by(dir, sortProp);
        }
        Pageable pageable = PageRequest.of(Math.max(0, page - 1), size, sort);

        Specification<Order> spec = (root, query, cb) -> {
            List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();
            if (StringUtils.hasText(keyword)) {
                String kw = keyword.trim().toLowerCase();
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("orderId")), "%" + kw + "%"),
                        cb.like(cb.lower(root.get("commodityTitle")), "%" + kw + "%"),
                        cb.like(cb.lower(root.get("buyerId")), "%" + kw + "%"),
                        cb.like(cb.lower(root.get("sellerId")), "%" + kw + "%")
                ));
            }
            if (StringUtils.hasText(status))
                predicates.add(cb.equal(root.get("orderStatus"), status.trim()));
            if (StringUtils.hasText(sellerVisibility))
                predicates.add(cb.equal(root.get("sellerVisibility"), sellerVisibility.trim()));
            if (StringUtils.hasText(buyerVisibility))
                predicates.add(cb.equal(root.get("buyerVisibility"), buyerVisibility.trim()));
            return predicates.isEmpty() ? cb.conjunction()
                    : cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };

        Page<Order> orderPage = orderRepository.findAll(spec, pageable);
        List<OrderInternalDTO> dtos = orderPage.getContent().stream()
                .map(orderInternalDTOConverter::toInternalDTO)
                .collect(Collectors.toList());

        Map<String, Object> result = new HashMap<>();
        result.put("content", dtos);
        result.put("totalElements", orderPage.getTotalElements());
        result.put("totalPages", orderPage.getTotalPages());
        result.put("number", orderPage.getNumber());
        result.put("size", orderPage.getSize());
        return Result.ok("查询成功", result);
    }
}
