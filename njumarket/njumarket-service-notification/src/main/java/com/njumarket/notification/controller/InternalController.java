package com.njumarket.notification.controller;

import com.njumarket.njumarket.dto.Result;
import com.njumarket.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 推送服务内部接口控制器
 * 供其他服务（message-service 等）调用 WebSocket 推送功能
 */
@Slf4j
@RestController
@RequestMapping("/api/internal")
@RequiredArgsConstructor
public class InternalController {

    private final NotificationService notificationService;

    /**
     * 推送订单变更通知（供 order-service 通过 Feign 调用）
     */
    @PostMapping("/notification/order-change")
    public Result pushOrderChange(@RequestParam String userId,
                                  @RequestParam String orderId,
                                  @RequestParam String operation) {
        notificationService.pushOrderChange(userId, orderId, operation);
        return Result.ok("推送成功");
    }

    /**
     * 通用推送接口（供 message-service 通过 Feign 调用）
     * 支持：MESSAGE_NEW、CONVERSATION_RESTORED、MESSAGE_READ、UNREAD_COUNT_UPDATE 等
     */
    @PostMapping("/notification/push")
    public Result push(@RequestParam String userId,
                       @RequestParam(required = false) String messageType,
                       @RequestBody Object messageData) {
        if (messageType == null || messageType.isBlank()) {
            if (messageData instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> dataMap = (Map<String, Object>) messageData;
                messageType = (String) dataMap.get("type");
            }
        }
        if (messageType == null || messageType.isBlank()) {
            messageType = "MESSAGE_NEW";
        }

        switch (messageType) {
            case "MESSAGE_NEW" -> notificationService.pushMessage(userId, messageData);
            case "UNREAD_COUNT_UPDATE" -> {
                if (messageData instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> dataMap = (Map<String, Object>) messageData;
                    Object cnt = dataMap.get("unreadCount");
                    if (cnt instanceof Number) {
                        notificationService.pushUnreadCountUpdate(userId, ((Number) cnt).intValue());
                    } else {
                        return Result.fail("unreadCount 格式错误");
                    }
                } else {
                    return Result.fail("messageData 格式错误");
                }
            }
            default -> notificationService.pushGenericMessage(userId, messageData, messageType);
        }

        return Result.ok("推送成功");
    }

    /**
     * 推送未读消息数更新
     */
    @PostMapping("/notification/unread-count")
    public Result pushUnreadCountUpdate(@RequestParam String userId,
                                        @RequestParam Integer unreadCount) {
        notificationService.pushUnreadCountUpdate(userId, unreadCount);
        return Result.ok("推送成功");
    }
}
