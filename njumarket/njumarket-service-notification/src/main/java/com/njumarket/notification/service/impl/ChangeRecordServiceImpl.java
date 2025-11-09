package com.njumarket.notification.service.impl;

import com.njumarket.notification.service.ChangeRecordService;
import com.njumarket.njumarket.utils.RedisConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 变更记录服务实现类（时间分片式ZSet优化版）
 * 
 * 优化方案：
 * 1. 时间分片：按小时分片，每个时间片一个ZSet key（如：chat:commodity:changes:2025-01-20:10）
 * 2. 智能TTL管理：每次添加数据时更新当前时间片的TTL，避免数据过早过期
 * 3. 自动清理：查询时自动跳过已过期的时间片
 * 
 * 优势：
 * - 单个key数据量可控（每小时的数据量）
 * - 查询时可以只查询相关时间片，减少扫描范围
 * - TTL更新更精确，只更新当前时间片
 * - 旧数据自动过期，无需手动清理
 * 
 * 时区说明：
 * - 本项目统一使用GMT+8时区（Asia/Shanghai，中国大陆时区）
 * - 所有时间戳的epoch秒数计算都使用GMT+8时区，确保存储和查询时区一致
 * - 前端发送的UTC时间戳会自动转换为GMT+8时区进行比较
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChangeRecordServiceImpl implements ChangeRecordService {
    
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;
    
    // 商品和订单变更记录的前缀（按时间分片）
    private static final String COMMODITY_CHANGE_PREFIX = "chat:commodity:changes:";
    private static final String ORDER_CHANGE_PREFIX = "chat:order:changes:";
    
    // 时间分片格式：yyyy-MM-dd:HH（按小时分片）
    private static final DateTimeFormatter TIME_SLICE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd:HH");
    
    // TTL：24小时（秒）
    private static final long TTL_SECONDS = RedisConstants.CHANGE_RECORD_TTL;
    
    // ✅ 统一使用GMT+8时区（中国大陆时区）
    private static final ZoneOffset GMT_PLUS_8_OFFSET = ZoneOffset.ofHours(8);
    
    /**
     * 根据时间戳获取时间片key
     */
    private String getTimeSliceKey(LocalDateTime timestamp, String prefix) {
        String timeSlice = timestamp.format(TIME_SLICE_FORMATTER);
        return prefix + timeSlice;
    }
    
    /**
     * 解析时间戳字符串为LocalDateTime
     * 支持多种格式，包括纳秒精度（如：2025-11-02T13:47:46.322238800）
     */
    private LocalDateTime parseTimestamp(String timestampStr) {
        try {
            // 尝试直接解析（支持ISO格式，包括纳秒）
            return LocalDateTime.parse(timestampStr);
        } catch (Exception e) {
            try {
                // 如果直接解析失败，尝试移除多余的纳秒位数（保留最多9位）
                if (timestampStr.contains(".")) {
                    String[] parts = timestampStr.split("\\.");
                    if (parts.length == 2) {
                        String dateTimePart = parts[0];
                        String nanoPart = parts[1];
                        // 限制纳秒部分最多9位
                        if (nanoPart.length() > 9) {
                            nanoPart = nanoPart.substring(0, 9);
                        }
                        return LocalDateTime.parse(dateTimePart + "." + nanoPart);
                    }
                }
                return LocalDateTime.parse(timestampStr);
            } catch (Exception e2) {
                log.warn("Failed to parse timestamp: timestamp={}, error={}", timestampStr, e2.getMessage());
                return null;
            }
        }
    }
    
    /**
     * 获取指定时间范围的所有时间片key
     */
    private List<String> getTimeSliceKeys(LocalDateTime startTime, LocalDateTime endTime, String prefix) {
        List<String> keys = new ArrayList<>();
        LocalDateTime current = startTime.withMinute(0).withSecond(0).withNano(0); // 整点开始
        
        while (!current.isAfter(endTime)) {
            keys.add(prefix + current.format(TIME_SLICE_FORMATTER));
            current = current.plusHours(1);
        }
        
        return keys;
    }
    
    @Override
    public void recordCommodityChange(String commodityId, String operation, LocalDateTime timestamp) {
        try {
            String timeSliceKey = getTimeSliceKey(timestamp, COMMODITY_CHANGE_PREFIX);
            // ✅ 统一使用GMT+8时区转换epoch秒数
            long score = timestamp.toEpochSecond(GMT_PLUS_8_OFFSET);
            
            String record = String.format(
                "{\"commodityId\":\"%s\",\"operation\":\"%s\",\"timestamp\":\"%s\"}",
                commodityId, operation, timestamp.toString()
            );
            
            redisTemplate.opsForZSet().add(timeSliceKey, record, score);
            redisTemplate.expire(timeSliceKey, java.time.Duration.ofSeconds(TTL_SECONDS));
            
            log.debug("记录商品变更: commodityId={}, operation={}, timestamp={}, timeSlice={}", 
                commodityId, operation, timestamp, timeSliceKey);
                
        } catch (Exception e) {
            log.error("记录商品变更失败: commodityId={}, error={}", commodityId, e.getMessage(), e);
        }
    }
    
    @Override
    public void recordOrderChange(String orderId, String operation, LocalDateTime timestamp) {
        try {
            String timeSliceKey = getTimeSliceKey(timestamp, ORDER_CHANGE_PREFIX);
            // ✅ 统一使用GMT+8时区转换epoch秒数
            long score = timestamp.toEpochSecond(GMT_PLUS_8_OFFSET);
            
            String record = String.format(
                "{\"orderId\":\"%s\",\"operation\":\"%s\",\"timestamp\":\"%s\"}",
                orderId, operation, timestamp.toString()
            );
            
            // ✅ 写入Redis（移除ping检查以提升性能，Redis连接失败会在写入时抛出异常）
            Boolean added = redisTemplate.opsForZSet().add(timeSliceKey, record, score);
            if (Boolean.TRUE.equals(added)) {
                log.info("✅ 订单变更已写入Redis: orderId={}, operation={}, timestamp={} (GMT+8), timeSlice={}, score={}", 
                    orderId, operation, timestamp, timeSliceKey, score);
            } else {
                log.warn("⚠️ 订单变更写入Redis失败（可能已存在）: orderId={}, operation={}, timeSlice={}", 
                    orderId, operation, timeSliceKey);
            }
            
            // ✅ 设置TTL
            Boolean expired = redisTemplate.expire(timeSliceKey, java.time.Duration.ofSeconds(TTL_SECONDS));
            if (Boolean.TRUE.equals(expired)) {
                log.debug("订单变更记录TTL已设置: timeSlice={}, ttl={}秒", timeSliceKey, TTL_SECONDS);
            } else {
                log.warn("订单变更记录TTL设置失败: timeSlice={}", timeSliceKey);
            }
                
        } catch (Exception e) {
            log.error("❌ 记录订单变更失败: orderId={}, operation={}, timestamp={}, error={}, errorType={}", 
                orderId, operation, timestamp, e.getMessage(), e.getClass().getName(), e);
        }
    }
    
    @Override
    public List<String> getCommodityChangesAfter(LocalDateTime afterTimestamp) {
        try {
            // ✅ 统一使用GMT+8时区转换epoch秒数（与存储时保持一致）
            long minScore = afterTimestamp.toEpochSecond(GMT_PLUS_8_OFFSET);
            // ✅ 使用 >= 而不是 >，避免边界问题
            long exclusiveMinScore = minScore;  // 改为不+1，使用 >= 比较
            
            LocalDateTime now = LocalDateTime.now();
            List<String> timeSliceKeys = getTimeSliceKeys(afterTimestamp, now, COMMODITY_CHANGE_PREFIX);
            
            log.debug("查询商品变更: afterTimestamp={}, minScore={}, exclusiveMinScore={}, timeSliceKeys={}", 
                afterTimestamp, minScore, exclusiveMinScore, timeSliceKeys);
            
            Set<String> allRecords = new HashSet<>();
            for (String timeSliceKey : timeSliceKeys) {
                try {
                    Boolean exists = redisTemplate.hasKey(timeSliceKey);
                    if (Boolean.TRUE.equals(exists)) {
                        // ✅ 使用 >= 比较（inclusive），避免遗漏边界记录
                        Set<Object> records = redisTemplate.opsForZSet()
                            .rangeByScore(timeSliceKey, minScore, Double.MAX_VALUE);
                        if (records != null) {
                            log.debug("时间片 {} 查询到 {} 条记录", timeSliceKey, records.size());
                            for (Object record : records) {
                                if (record != null) {
                                    allRecords.add(record.toString());
                                }
                            }
                        }
                    } else {
                        log.debug("时间片 {} 不存在", timeSliceKey);
                    }
                } catch (Exception e) {
                    log.warn("Failed to query time slice: timeSliceKey={}, error={}", timeSliceKey, e.getMessage());
                }
            }
            
            log.debug("ZSet查询结果: 共 {} 条记录", allRecords.size());
            
            List<String> result = new ArrayList<>();
            for (String record : allRecords) {
                try {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> recordMap = objectMapper.readValue(record, Map.class);
                    Object timestampObj = recordMap.get("timestamp");
                    
                    if (timestampObj != null) {
                        String timestampStr = timestampObj.toString();
                        LocalDateTime recordTimestamp = parseTimestamp(timestampStr);
                        
                        // ✅ 使用 >= 比较（inclusive），确保包含边界时间戳的记录
                        if (recordTimestamp != null && (recordTimestamp.isAfter(afterTimestamp) || recordTimestamp.isEqual(afterTimestamp))) {
                            result.add(record);
                            log.debug("添加商品变更记录: commodityId={}, operation={}, recordTimestamp={}, afterTimestamp={}", 
                                recordMap.get("commodityId"), recordMap.get("operation"), recordTimestamp, afterTimestamp);
                        } else if (recordTimestamp == null) {
                            // 向后兼容：如果无法解析时间戳，也包含该记录
                            result.add(record);
                            log.debug("添加商品变更记录（时间戳解析失败，向后兼容）: record={}", record);
                        } else {
                            log.debug("跳过商品变更记录（时间戳早于查询时间）: recordTimestamp={}, afterTimestamp={}", 
                                recordTimestamp, afterTimestamp);
                        }
                    } else {
                        // 向后兼容：如果没有时间戳字段，也包含该记录
                        result.add(record);
                        log.debug("添加商品变更记录（无时间戳字段，向后兼容）: record={}", record);
                    }
                } catch (Exception e) {
                    log.warn("Failed to parse change record, keeping record - record={}, error={}", record, e.getMessage());
                    result.add(record);
                }
            }
            
            log.info("商品变更查询完成: afterTimestamp={}, ZSet记录数={}, 最终结果数={}", 
                afterTimestamp, allRecords.size(), result.size());
            
            return result;
            
        } catch (Exception e) {
            log.error("Failed to query commodity change records: afterTimestamp={}, error={}", 
                afterTimestamp, e.getMessage(), e);
            return new ArrayList<>();
        }
    }
    
    @Override
    public List<String> getOrderChangesAfter(LocalDateTime afterTimestamp) {
        try {
            // ✅ 统一使用GMT+8时区转换epoch秒数（与存储时保持一致）
            long minScore = afterTimestamp.toEpochSecond(GMT_PLUS_8_OFFSET);
            
            LocalDateTime now = LocalDateTime.now();
            List<String> timeSliceKeys = getTimeSliceKeys(afterTimestamp, now, ORDER_CHANGE_PREFIX);
            
            log.info("查询订单变更: afterTimestamp={} (GMT+8), minScore={}, timeSliceKeys={}", 
                afterTimestamp, minScore, timeSliceKeys);
            
            Set<String> allRecords = new HashSet<>();
            for (String timeSliceKey : timeSliceKeys) {
                try {
                    Boolean exists = redisTemplate.hasKey(timeSliceKey);
                    if (Boolean.TRUE.equals(exists)) {
                        // ✅ 使用 >= 比较（inclusive），避免遗漏边界记录
                        Set<Object> records = redisTemplate.opsForZSet()
                            .rangeByScore(timeSliceKey, minScore, Double.MAX_VALUE);
                        if (records != null) {
                            log.debug("时间片 {} 查询到 {} 条记录", timeSliceKey, records.size());
                            for (Object record : records) {
                                if (record != null) {
                                    allRecords.add(record.toString());
                                }
                            }
                        }
                    } else {
                        log.debug("时间片 {} 不存在", timeSliceKey);
                    }
                } catch (Exception e) {
                    log.warn("Failed to query time slice: timeSliceKey={}, error={}", timeSliceKey, e.getMessage());
                }
            }
            
            log.debug("ZSet查询结果: 共 {} 条记录", allRecords.size());
            
            List<String> result = new ArrayList<>();
            for (String record : allRecords) {
                try {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> recordMap = objectMapper.readValue(record, Map.class);
                    Object timestampObj = recordMap.get("timestamp");
                    
                    if (timestampObj != null) {
                        String timestampStr = timestampObj.toString();
                        LocalDateTime recordTimestamp = parseTimestamp(timestampStr);
                        
                        // ✅ 使用 >= 比较（inclusive），确保包含边界时间戳的记录
                        if (recordTimestamp != null && (recordTimestamp.isAfter(afterTimestamp) || recordTimestamp.isEqual(afterTimestamp))) {
                            result.add(record);
                            log.info("✅ 添加订单变更记录: orderId={}, operation={}, recordTimestamp={} (GMT+8), afterTimestamp={} (GMT+8)", 
                                recordMap.get("orderId"), recordMap.get("operation"), recordTimestamp, afterTimestamp);
                        } else if (recordTimestamp == null) {
                            // 向后兼容：如果无法解析时间戳，也包含该记录
                            result.add(record);
                            log.warn("⚠️ 添加订单变更记录（时间戳解析失败，向后兼容）: record={}", record);
                        } else {
                            log.info("⏭️ 跳过订单变更记录（时间戳早于查询时间）: recordTimestamp={} (GMT+8), afterTimestamp={} (GMT+8), 时间差={}秒", 
                                recordTimestamp, afterTimestamp, 
                                java.time.Duration.between(recordTimestamp, afterTimestamp).getSeconds());
                        }
                    } else {
                        // 向后兼容：如果没有时间戳字段，也包含该记录
                        result.add(record);
                        log.debug("添加订单变更记录（无时间戳字段，向后兼容）: record={}", record);
                    }
                } catch (Exception e) {
                    log.warn("Failed to parse order change record, keeping record - record={}, error={}", record, e.getMessage());
                    result.add(record);
                }
            }
            
            log.info("订单变更查询完成: afterTimestamp={} (GMT+8), minScore={}, ZSet记录数={}, 最终结果数={}", 
                afterTimestamp, minScore, allRecords.size(), result.size());
            
            // ✅ 调试：如果查询到记录但最终结果为空，记录详细信息
            if (allRecords.size() > 0 && result.isEmpty()) {
                log.warn("⚠️ 查询到ZSet记录但最终结果为空，可能存在时间戳比较问题: afterTimestamp={}, ZSet记录数={}", 
                    afterTimestamp, allRecords.size());
                // 记录前几条记录的详细信息
                int count = 0;
                for (String record : allRecords) {
                    if (count >= 3) break;
                    try {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> recordMap = objectMapper.readValue(record, Map.class);
                        String recordTimestampStr = recordMap.get("timestamp") != null ? recordMap.get("timestamp").toString() : "null";
                        LocalDateTime recordTimestamp = parseTimestamp(recordTimestampStr);
                        log.warn("ZSet记录 {}: orderId={}, operation={}, recordTimestamp={}, afterTimestamp={}, isAfter={}, isEqual={}", 
                            count + 1, recordMap.get("orderId"), recordMap.get("operation"), 
                            recordTimestamp, afterTimestamp,
                            recordTimestamp != null ? recordTimestamp.isAfter(afterTimestamp) : false,
                            recordTimestamp != null ? recordTimestamp.isEqual(afterTimestamp) : false);
                    } catch (Exception e) {
                        log.warn("解析ZSet记录失败: record={}, error={}", record, e.getMessage());
                    }
                    count++;
                }
            }
            
            return result;
            
        } catch (Exception e) {
            log.error("Failed to query order change records: afterTimestamp={}, error={}", 
                afterTimestamp, e.getMessage(), e);
            return new ArrayList<>();
        }
    }
}

