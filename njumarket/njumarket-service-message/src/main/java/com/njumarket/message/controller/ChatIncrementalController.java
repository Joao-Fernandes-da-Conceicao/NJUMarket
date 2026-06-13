package com.njumarket.message.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.njumarket.message.client.OrderClient;
import com.njumarket.message.repository.MessageRepository;
import com.njumarket.message.service.UserCacheService;
import com.njumarket.njumarket.dto.Result;
import com.njumarket.njumarket.dto.internal.UserProfileInternalDTO;
import com.njumarket.njumarket.model.IUser;
import com.njumarket.njumarket.utils.UserHolder;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 聊天增量轮询接口：返回当前用户消息中涉及的商品和订单的最新状态。
 * 前端通过轮询此接口感知商品/订单状态变化，无需依赖 WebSocket。
 */
@Slf4j
@Tag(name = "聊天增量轮询", description = "轮询聊天中关联商品和订单的最新状态")
@RestController
@RequestMapping("/api/user/chat")
@RequiredArgsConstructor
public class ChatIncrementalController {

    private final MessageRepository messageRepository;
    private final OrderClient orderClient;
    private final UserCacheService userCacheService;
    private final ObjectMapper objectMapper;

    private static final TypeReference<List<Map<String, Object>>> LIST_MAP_TYPE =
            new TypeReference<>() {};

    @Operation(summary = "增量查询商品和订单变更",
               description = "返回当前用户对话中所有关联商品和订单的最新状态，供前端增量刷新使用")
    @GetMapping("/incremental-update")
    public Result getIncrementalUpdate(
            @RequestParam(required = false) Long lastPollTimestamp) {

        IUser currentUser = UserHolder.getUser();
        if (currentUser == null || !StringUtils.hasText(currentUser.getUserId())) {
            return Result.fail("未登录");
        }
        String userId = currentUser.getUserId();

        List<Map<String, Object>> commodities = Collections.emptyList();
        List<Map<String, Object>> orders = Collections.emptyList();

        // 查询该用户消息中关联的商品ID（支持时间戳增量过滤）
        List<String> commodityIds;
        if (lastPollTimestamp != null && lastPollTimestamp > 0) {
            LocalDateTime since = LocalDateTime.ofInstant(
                    Instant.ofEpochMilli(lastPollTimestamp), ZoneId.systemDefault());
            commodityIds = messageRepository.findDistinctCommodityIdsByUserAndSince(userId, since);
        } else {
            commodityIds = messageRepository.findDistinctCommodityIdsByUser(userId);
        }
        if (!commodityIds.isEmpty()) {
            try {
                Result res = orderClient.getCommoditiesBatch(commodityIds);
                if (res != null && Boolean.TRUE.equals(res.getSuccess()) && res.getData() != null) {
                    commodities = objectMapper.convertValue(res.getData(), LIST_MAP_TYPE);
                }
            } catch (Exception e) {
                log.warn("增量轮询：批量查询商品失败: userId={}, error={}", userId, e.getMessage());
            }
        }

        // 查询该用户消息中关联的订单ID（支持时间戳增量过滤）
        List<String> orderIds;
        if (lastPollTimestamp != null && lastPollTimestamp > 0) {
            LocalDateTime since = LocalDateTime.ofInstant(
                    Instant.ofEpochMilli(lastPollTimestamp), ZoneId.systemDefault());
            orderIds = messageRepository.findDistinctOrderIdsByUserAndSince(userId, since);
        } else {
            orderIds = messageRepository.findDistinctOrderIdsByUser(userId);
        }
        if (!orderIds.isEmpty()) {
            try {
                Result res = orderClient.getOrdersBatch(orderIds);
                if (res != null && Boolean.TRUE.equals(res.getSuccess()) && res.getData() != null) {
                    List<Map<String, Object>> rawOrders =
                            objectMapper.convertValue(res.getData(), LIST_MAP_TYPE);

                    // 批量获取卖家和买家 profile（nickname / avatar）
                    Set<String> userIds = new HashSet<>();
                    rawOrders.forEach(o -> {
                        Object sid = o.get("sellerId");
                        Object bid = o.get("buyerId");
                        if (sid != null) userIds.add(sid.toString());
                        if (bid != null) userIds.add(bid.toString());
                    });
                    Map<String, UserProfileInternalDTO> profileMap =
                            userCacheService.getUserProfilesByIds(userIds);

                    orders = rawOrders.stream().map(o -> {
                        Map<String, Object> enriched = new LinkedHashMap<>(o);
                        String sid = o.get("sellerId") != null ? o.get("sellerId").toString() : null;
                        String bid = o.get("buyerId") != null ? o.get("buyerId").toString() : null;
                        UserProfileInternalDTO sp = sid != null ? profileMap.get(sid) : null;
                        UserProfileInternalDTO bp = bid != null ? profileMap.get(bid) : null;
                        enriched.put("sellerNickname", sp != null ? sp.getNickname() : null);
                        enriched.put("sellerAvatar",   sp != null ? sp.getAvatar()   : null);
                        enriched.put("buyerNickname",  bp != null ? bp.getNickname()  : null);
                        enriched.put("buyerAvatar",    bp != null ? bp.getAvatar()    : null);
                        return enriched;
                    }).collect(Collectors.toList());
                }
            } catch (Exception e) {
                log.warn("增量轮询：批量查询订单失败: userId={}, error={}", userId, e.getMessage());
            }
        }

        Map<String, Object> data = new HashMap<>();
        data.put("commodities", commodities);
        data.put("orders", orders);
        return Result.ok("增量更新查询成功", data);
    }
}
