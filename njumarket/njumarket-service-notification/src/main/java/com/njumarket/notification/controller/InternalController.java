package com.njumarket.notification.controller;

import com.njumarket.njumarket.dto.Result;
import com.njumarket.notification.service.ChangeRecordService;
import com.njumarket.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
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
     */
    @PostMapping("/change-record/commodity")
    public Result recordCommodityChange(@RequestParam String commodityId,
                                       @RequestParam String operation,
                                       @RequestParam String timestamp) {
        LocalDateTime changeTime = LocalDateTime.parse(timestamp);
        changeRecordService.recordCommodityChange(commodityId, operation, changeTime);
        return Result.ok("记录成功");
    }
    
    /**
     * 记录订单变更
     */
    @PostMapping("/change-record/order")
    public Result recordOrderChange(@RequestParam String orderId,
                                   @RequestParam String operation,
                                   @RequestParam String timestamp) {
        try {
            log.info("收到订单变更记录请求: orderId={}, operation={}, timestamp={}", orderId, operation, timestamp);
            
            // ✅ 解析时间戳（支持多种格式）
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
            
            log.info("时间戳解析成功: timestamp={} -> changeTime={}", timestamp, changeTime);
            
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
     * 推送未读消息数更新
     */
    @PostMapping("/notification/unread-count")
    public Result pushUnreadCountUpdate(@RequestParam String userId,
                                       @RequestParam Integer unreadCount) {
        notificationService.pushUnreadCountUpdate(userId, unreadCount);
        return Result.ok("推送成功");
    }
}

