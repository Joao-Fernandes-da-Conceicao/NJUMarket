package com.njumarket.order.controller;

import com.njumarket.njumarket.dto.Result;
import com.njumarket.order.service.OrderService;
import com.njumarket.order.client.ChangeRecordClient;
import com.njumarket.order.client.CommodityQueryClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

/**
 * 聊天数据控制器
 * 提供增量查询接口，支持聊天界面的增量轮询
 * 
 * 时区说明：
 * - 前端发送UTC时间戳（ISO 8601格式，如：2025-01-20T10:30:00.000Z）
 * - 后端转换为系统时区（GMT+8）用于与Redis记录中的时间戳比较
 * - Redis ZSet的score使用UTC epoch秒数计算（与ChangeRecordServiceImpl保持一致）
 * - 由于项目固定部署在GMT+8时区，这种时区混用不会影响功能正确性
 */
@Slf4j
@Tag(name = "聊天数据查询", description = "聊天界面相关的数据查询接口，支持增量轮询")
@RestController
@RequestMapping("/api/user/chat")
@RequiredArgsConstructor
public class ChatDataController {
    
    private final ChangeRecordClient changeRecordClient;
    private final CommodityQueryClient commodityQueryClient;
    private final OrderService orderService;
    private final ObjectMapper objectMapper;
    
    /**
     * 增量查询商品和订单的变更
     * @param lastPollTimestamp 上次轮询的时间戳（ISO格式，如：2025-01-20T10:30:00）
     * @return 包含变更的商品和订单列表
     */
    @Operation(summary = "增量查询商品和订单变更", 
               description = "根据上次轮询的时间戳，查询之后变更的商品和订单信息，用于聊天界面增量更新")
    @GetMapping("/incremental-update")
    public Result getIncrementalUpdate(@RequestParam String lastPollTimestamp) {
        try {
            log.info("Incremental update request - lastPollTimestamp: {}", lastPollTimestamp);
            
            // Parse timestamp (supports various ISO formats)
            LocalDateTime timestamp;
            try {
                // Try parsing standard ISO format (may contain milliseconds and timezone)
                try {
                    // First try parsing with timezone (e.g., 2025-01-20T10:30:00.000Z)
                    OffsetDateTime offsetDateTime = OffsetDateTime.parse(lastPollTimestamp);
                    // Keep as UTC LocalDateTime for consistent comparison with Redis records
                    // Redis records store timestamps using LocalDateTime.now() which is in system timezone
                    // But we convert UTC to system timezone for fair comparison
                    timestamp = offsetDateTime.atZoneSameInstant(java.time.ZoneId.systemDefault()).toLocalDateTime();
                    log.debug("Timestamp parsed successfully (OffsetDateTime): {} -> {} (converted to system timezone: {})", 
                        lastPollTimestamp, timestamp, java.time.ZoneId.systemDefault());
                } catch (DateTimeParseException e1) {
                    // If no timezone, try parsing LocalDateTime format (e.g., 2025-01-20T10:30:00)
                    try {
                        timestamp = LocalDateTime.parse(lastPollTimestamp);
                        log.debug("Timestamp parsed successfully (LocalDateTime): {} -> {}", lastPollTimestamp, timestamp);
                    } catch (DateTimeParseException e2) {
                        // Try parsing with milliseconds but no timezone (e.g., 2025-01-20T10:30:00.000)
                        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss[.SSS]");
                        timestamp = LocalDateTime.parse(lastPollTimestamp, formatter);
                        log.debug("Timestamp parsed successfully (custom format): {} -> {}", lastPollTimestamp, timestamp);
                    }
                }
            } catch (Exception e) {
                log.error("Timestamp format error: {}, error message: {}", lastPollTimestamp, e.getMessage());
                return Result.fail("Timestamp format error, please use ISO format (e.g., 2025-01-20T10:30:00 or 2025-01-20T10:30:00.000Z)");
            }
            
            // Note: timestamp已转换为系统时区，但epoch秒数计算使用UTC（与Redis ZSet score计算方式一致）
            log.info("Timestamp parsed - original: {}, parsed: {}, epoch seconds (UTC): {}", 
                lastPollTimestamp, timestamp, timestamp.toEpochSecond(ZoneOffset.UTC));
            
            // ✅ 1. 使用Feign Client获取变更记录
            Result commodityChangesResult = changeRecordClient.getCommodityChangesAfter(timestamp.toString());
            Result orderChangesResult = changeRecordClient.getOrderChangesAfter(timestamp.toString());
            
            @SuppressWarnings("unchecked")
            List<String> commodityChanges = commodityChangesResult.getSuccess() && commodityChangesResult.getData() != null 
                ? (List<String>) commodityChangesResult.getData() : new ArrayList<>();
            @SuppressWarnings("unchecked")
            List<String> orderChanges = orderChangesResult.getSuccess() && orderChangesResult.getData() != null 
                ? (List<String>) orderChangesResult.getData() : new ArrayList<>();
            
            log.info("Redis query result - commodity changes: {}, order changes: {}", 
                commodityChanges.size(), orderChanges.size());
            
            // 2. 解析变更记录，提取ID
            Set<String> commodityIds = new HashSet<>();
            Set<String> orderIds = new HashSet<>();
            
            // 解析商品变更记录
            for (String changeRecord : commodityChanges) {
                try {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> record = objectMapper.readValue(changeRecord, Map.class);
                    Object commodityIdObj = record.get("commodityId");
                    if (commodityIdObj != null) {
                        commodityIds.add(commodityIdObj.toString());
                    }
                } catch (Exception e) {
                    log.warn("Failed to parse commodity change record: {}, error: {}", changeRecord, e.getMessage());
                }
            }
            
            // 解析订单变更记录
            for (String changeRecord : orderChanges) {
                try {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> record = objectMapper.readValue(changeRecord, Map.class);
                    Object orderIdObj = record.get("orderId");
                    if (orderIdObj != null) {
                        orderIds.add(orderIdObj.toString());
                    }
                } catch (Exception e) {
                    log.warn("Failed to parse order change record: {}, error: {}", changeRecord, e.getMessage());
                }
            }
            
            // 3. 批量查询商品和订单的最新状态
            Map<String, Object> result = new HashMap<>();
            
            // ✅ 查询商品（使用Feign Client）
            if (!commodityIds.isEmpty()) {
                Result commodityResult = commodityQueryClient.getCommoditiesBatchStatus(
                    new ArrayList<>(commodityIds)
                );
                if (commodityResult.getSuccess()) {
                    result.put("commodities", commodityResult.getData());
                } else {
                    log.warn("Failed to batch query commodity status: {}", commodityResult.getMessage());
                    result.put("commodities", new ArrayList<>());
                }
            } else {
                result.put("commodities", new ArrayList<>());
            }
            
            // Query orders
            if (!orderIds.isEmpty()) {
                Result orderResult = orderService.getOrdersBatchStatus(new ArrayList<>(orderIds));
                if (orderResult.getSuccess()) {
                    result.put("orders", orderResult.getData());
                } else {
                    log.warn("Failed to batch query order status: {}", orderResult.getMessage());
                    result.put("orders", new ArrayList<>());
                }
            } else {
                result.put("orders", new ArrayList<>());
            }
            
            // 4. Return result
            int totalChanges = commodityIds.size() + orderIds.size();
            log.info("Incremental query completed - commodities: {}, orders: {}, total changes: {}", 
                commodityIds.size(), orderIds.size(), totalChanges);
            
            return Result.ok("Incremental query successful", result);
            
        } catch (Exception e) {
            log.error("Incremental query failed: {}", e.getMessage(), e);
            return Result.fail("Incremental query failed: " + e.getMessage());
        }
    }
}
