package com.njumarket.notification.controller;

import com.njumarket.njumarket.dto.Result;
import com.njumarket.notification.service.ChangeRecordService;
import com.njumarket.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

/**
 * 推送服务内部接口控制器
 * 供其他服务调用推送功能和变更记录功能
 */
@Slf4j
@RestController
@RequestMapping("/api/internal")
@RequiredArgsConstructor
public class InternalController {
    
    private final ChangeRecordService changeRecordService;
    private final NotificationService notificationService;
    
    // ✅ 统一使用GMT+8时区（中国大陆时区）
    private static final ZoneId GMT_PLUS_8_ZONE = ZoneId.of("Asia/Shanghai");
    private static final ZoneOffset GMT_PLUS_8_OFFSET = ZoneOffset.ofHours(8);
    
    // ========== 变更记录接口 ==========
    
    /**
     * 获取商品变更记录
     */
    @GetMapping("/change-record/commodity")
    public Result getCommodityChangesAfter(@RequestParam String timestamp) {
        LocalDateTime afterTimestamp = LocalDateTime.parse(timestamp);
        List<String> changes = changeRecordService.getCommodityChangesAfter(afterTimestamp);
        return Result.ok("查询成功", changes);
    }
    
    /**
     * 获取订单变更记录
     */
    @GetMapping("/change-record/order")
    public Result getOrderChangesAfter(@RequestParam String timestamp) {
        LocalDateTime afterTimestamp = LocalDateTime.parse(timestamp);
        List<String> changes = changeRecordService.getOrderChangesAfter(afterTimestamp);
        return Result.ok("查询成功", changes);
    }
    
    /**
     * 记录商品变更
     * ✅ 时间戳解析：假设传入的时间戳是系统默认时区的LocalDateTime字符串（无时区信息）
     * 为了统一，我们将其视为GMT+8时区的时间戳
     */
    @PostMapping("/change-record/commodity")
    public Result recordCommodityChange(@RequestParam String commodityId,
                                       @RequestParam String operation,
                                       @RequestParam String timestamp) {
        try {
            // ✅ 解析时间戳（假设为GMT+8时区，与查询时保持一致）
            LocalDateTime changeTime = LocalDateTime.parse(timestamp);
            log.debug("记录商品变更: commodityId={}, operation={}, timestamp={} (解析为GMT+8)", 
                commodityId, operation, changeTime);
            changeRecordService.recordCommodityChange(commodityId, operation, changeTime);
            return Result.ok("记录成功");
        } catch (Exception e) {
            log.error("记录商品变更失败: commodityId={}, operation={}, timestamp={}, error={}", 
                commodityId, operation, timestamp, e.getMessage(), e);
            return Result.fail("记录失败: " + e.getMessage());
        }
    }
    
    /**
     * 记录订单变更
     * ✅ 时间戳解析：假设传入的时间戳是系统默认时区的LocalDateTime字符串（无时区信息）
     * 为了统一，我们将其视为GMT+8时区的时间戳
     */
    @PostMapping("/change-record/order")
    public Result recordOrderChange(@RequestParam String orderId,
                                   @RequestParam String operation,
                                   @RequestParam String timestamp) {
        try {
            log.info("收到订单变更记录请求: orderId={}, operation={}, timestamp={}", orderId, operation, timestamp);
            
            // ✅ 解析时间戳（支持多种格式，假设为GMT+8时区）
            LocalDateTime changeTime;
            try {
                changeTime = LocalDateTime.parse(timestamp);
            } catch (Exception e) {
                // 尝试其他格式
                try {
                    // 尝试移除纳秒部分（如果存在）
                    if (timestamp.contains(".")) {
                        String[] parts = timestamp.split("\\.");
                        changeTime = LocalDateTime.parse(parts[0]);
                    } else {
                        throw e;
                    }
                } catch (Exception e2) {
                    log.error("时间戳解析失败: timestamp={}, error={}", timestamp, e.getMessage());
                    return Result.fail("时间戳格式错误: " + timestamp);
                }
            }
            
            log.info("时间戳解析成功: timestamp={} -> changeTime={} (解析为GMT+8)", timestamp, changeTime);
            
            // ✅ 记录变更
            changeRecordService.recordOrderChange(orderId, operation, changeTime);
            
            log.info("订单变更记录请求处理完成: orderId={}, operation={}", orderId, operation);
            return Result.ok("记录成功");
            
        } catch (Exception e) {
            log.error("记录订单变更异常: orderId={}, operation={}, timestamp={}, error={}", 
                orderId, operation, timestamp, e.getMessage(), e);
            return Result.fail("记录失败: " + e.getMessage());
        }
    }
    
    // ========== 推送接口 ==========
    
    /**
     * 推送订单变更通知
     */
    @PostMapping("/notification/order-change")
    public Result pushOrderChange(@RequestParam String userId,
                                 @RequestParam String orderId,
                                 @RequestParam String operation) {
        notificationService.pushOrderChange(userId, orderId, operation);
        return Result.ok("推送成功");
    }
    
    /**
     * 推送商品变更通知
     */
    @PostMapping("/notification/commodity-change")
    public Result pushCommodityChange(@RequestParam String userId,
                                     @RequestParam String commodityId,
                                     @RequestParam String operation) {
        notificationService.pushCommodityChange(userId, commodityId, operation);
        return Result.ok("推送成功");
    }
    
    /**
     * 推送聊天消息通知
     */
    @PostMapping("/notification/message")
    public Result pushMessage(@RequestParam String userId,
                             @RequestBody Object messageData) {
        notificationService.pushMessage(userId, messageData);
        return Result.ok("推送成功");
    }
    
    /**
     * 通用推送接口（支持所有消息类型）
     * 用于Message服务推送各种类型的消息（MESSAGE_NEW、CONVERSATION_RESTORED、MESSAGE_READ等）
     */
    @PostMapping("/notification/push")
    public Result push(@RequestParam String userId,
                      @RequestParam(required = false) String messageType,
                      @RequestBody Object messageData) {
        // 如果messageType为空，尝试从messageData中提取
        if (messageType == null || messageType.trim().isEmpty()) {
            if (messageData instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> dataMap = (Map<String, Object>) messageData;
                messageType = (String) dataMap.get("type");
            }
        }
        
        // 根据messageType选择不同的推送方法
        if (messageType == null || messageType.trim().isEmpty()) {
            messageType = "MESSAGE_NEW"; // 默认类型
        }
        
        if ("MESSAGE_NEW".equals(messageType)) {
            notificationService.pushMessage(userId, messageData);
        } else if ("UNREAD_COUNT_UPDATE".equals(messageType)) {
            // 从messageData中提取unreadCount
            if (messageData instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> dataMap = (Map<String, Object>) messageData;
                Object unreadCountObj = dataMap.get("unreadCount");
                if (unreadCountObj instanceof Number) {
                    notificationService.pushUnreadCountUpdate(userId, ((Number) unreadCountObj).intValue());
                } else {
                    return Result.fail("unreadCount格式错误");
                }
            } else {
                return Result.fail("messageData格式错误");
            }
        } else {
            // 其他类型（CONVERSATION_RESTORED、MESSAGE_READ等）使用通用推送
            notificationService.pushGenericMessage(userId, messageData, messageType);
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

