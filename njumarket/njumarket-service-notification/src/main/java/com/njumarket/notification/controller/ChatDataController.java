package com.njumarket.notification.controller;

import com.njumarket.njumarket.dto.Result;
import com.njumarket.notification.vo.IncrementalUpdateResultVO;
import com.njumarket.njumarket.exception.BusinessException;
import com.njumarket.notification.service.ChangeRecordService;
import com.njumarket.notification.client.CommodityQueryClient;
import com.njumarket.notification.client.OrderQueryClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

/**
 * 聊天数据控制器（推送服务）
 * 提供增量查询接口，支持聊天界面的增量轮询
 */
@Slf4j
@Tag(name = "聊天数据查询", description = "聊天界面相关的数据查询接口，支持增量轮询")
@RestController
@RequestMapping("/api/user/chat")
@RequiredArgsConstructor
public class ChatDataController {
    
    private final ChangeRecordService changeRecordService;
    private final CommodityQueryClient commodityQueryClient;
    private final OrderQueryClient orderQueryClient;
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
            // ✅ 前端发送UTC时间戳（toISOString()），统一转换为GMT+8时区
            LocalDateTime timestamp;
            try {
                try {
                    // 优先解析带时区的ISO格式（如：2025-11-10T00:30:00.000Z）
                    OffsetDateTime offsetDateTime = OffsetDateTime.parse(lastPollTimestamp);
                    // ✅ 统一转换为GMT+8时区（Asia/Shanghai）
                    timestamp = offsetDateTime.atZoneSameInstant(java.time.ZoneId.of("Asia/Shanghai")).toLocalDateTime();
                    log.info("✅ 时间戳解析 (UTC -> GMT+8): UTC={} -> GMT+8={}", 
                        lastPollTimestamp, timestamp);
                } catch (DateTimeParseException e1) {
                    try {
                        // 尝试解析不带时区的LocalDateTime格式（假设为GMT+8时区）
                        timestamp = LocalDateTime.parse(lastPollTimestamp);
                        log.info("✅ 时间戳解析 (LocalDateTime, assumed GMT+8): {} -> {}", 
                            lastPollTimestamp, timestamp);
                    } catch (DateTimeParseException e2) {
                        // 尝试自定义格式
                        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss[.SSS]");
                        timestamp = LocalDateTime.parse(lastPollTimestamp, formatter);
                        log.info("✅ 时间戳解析 (custom format, assumed GMT+8): {} -> {}", 
                            lastPollTimestamp, timestamp);
                    }
                }
            } catch (Exception e) {
                log.error("Timestamp format error: {}, error message: {}", lastPollTimestamp, e.getMessage(), e);
                throw new BusinessException("Timestamp format error, please use ISO format (e.g., 2025-01-20T10:30:00 or 2025-01-20T10:30:00.000Z)");
            }
            
            // ✅ 记录当前时间（用于调试）
            LocalDateTime currentTime = LocalDateTime.now();
            log.info("当前系统时间 (GMT+8): {}, 查询时间戳 (GMT+8): {}, 时间差: {}秒", 
                currentTime, timestamp, 
                java.time.Duration.between(timestamp, currentTime).getSeconds());
            
            // 1. 使用本地服务获取变更记录
            List<String> commodityChanges = changeRecordService.getCommodityChangesAfter(timestamp);
            List<String> orderChanges = changeRecordService.getOrderChangesAfter(timestamp);
            
            log.info("Redis query result - commodity changes: {}, order changes: {}", 
                commodityChanges.size(), orderChanges.size());
            
            // 2. 解析变更记录，提取ID
            Set<String> commodityIds = new HashSet<>();
            Set<String> orderIds = new HashSet<>();
            
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
            IncrementalUpdateResultVO result = new IncrementalUpdateResultVO();
            
            if (!commodityIds.isEmpty()) {
                try {
                    log.info("Calling commodityQueryClient.getCommoditiesBatchStatus with {} IDs: {}", 
                        commodityIds.size(), commodityIds);
                    Result commodityResult = commodityQueryClient.getCommoditiesBatchStatus(new ArrayList<>(commodityIds));
                    log.info("Commodity query result: success={}, data={}", 
                        commodityResult != null ? commodityResult.getSuccess() : "null",
                        commodityResult != null && commodityResult.getData() != null ? 
                            ((List<?>) commodityResult.getData()).size() + " items" : "null");
                    if (commodityResult != null && commodityResult.getSuccess() && commodityResult.getData() != null) {
                        @SuppressWarnings("unchecked")
                        List<Map<String, Object>> commodityMaps = (List<Map<String, Object>>) commodityResult.getData();
                        result.setCommodities(commodityMaps);
                        log.info("Successfully retrieved {} commodities", commodityMaps.size());
                    } else {
                        log.warn("Failed to batch query commodity status: {}", 
                            commodityResult != null ? commodityResult.getMessage() : "Result is null");
                        result.setCommodities(new ArrayList<>());
                    }
                } catch (Exception e) {
                    log.error("Exception when calling commodityQueryClient.getCommoditiesBatchStatus: {}", 
                        e.getMessage(), e);
                    log.error("Exception class: {}, cause: {}", 
                        e.getClass().getName(), 
                        e.getCause() != null ? e.getCause().getClass().getName() : "null");
                    result.setCommodities(new ArrayList<>());
                }
            } else {
                result.setCommodities(new ArrayList<>());
            }
            
            if (!orderIds.isEmpty()) {
                try {
                    log.info("Calling orderQueryClient.getOrdersBatchStatus with {} IDs: {}", 
                        orderIds.size(), orderIds);
                    Result orderResult = orderQueryClient.getOrdersBatchStatus(new ArrayList<>(orderIds));
                    log.info("Order query result: success={}, data={}", 
                        orderResult != null ? orderResult.getSuccess() : "null",
                        orderResult != null && orderResult.getData() != null ? 
                            ((List<?>) orderResult.getData()).size() + " items" : "null");
                    if (orderResult != null && orderResult.getSuccess() && orderResult.getData() != null) {
                        @SuppressWarnings("unchecked")
                        List<Map<String, Object>> orderMaps = (List<Map<String, Object>>) orderResult.getData();
                        result.setOrders(orderMaps);
                        log.info("Successfully retrieved {} orders", orderMaps.size());
                    } else {
                        log.warn("Failed to batch query order status: {}", 
                            orderResult != null ? orderResult.getMessage() : "Result is null");
                        result.setOrders(new ArrayList<>());
                    }
                } catch (Exception e) {
                    log.error("Exception when calling orderQueryClient.getOrdersBatchStatus: {}", 
                        e.getMessage(), e);
                    log.error("Exception class: {}, cause: {}", 
                        e.getClass().getName(), 
                        e.getCause() != null ? e.getCause().getClass().getName() : "null");
                    result.setOrders(new ArrayList<>());
                }
            } else {
                result.setOrders(new ArrayList<>());
            }
            
            int totalChanges = commodityIds.size() + orderIds.size();
            log.info("Incremental query completed - commodities: {}, orders: {}, total changes: {}", 
                commodityIds.size(), orderIds.size(), totalChanges);
            
            return Result.ok("Incremental query successful", result);
        } catch (BusinessException e) {
            log.error("Business exception in incremental update: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error in incremental update: {}", e.getMessage(), e);
            return Result.fail("Incremental query failed: " + e.getMessage());
        }
    }
}

