package com.njumarket.njumarket.service.impl;

import com.njumarket.njumarket.service.ChangeRecordService;
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
 * - 本项目假设运行环境时区为GMT+8（中国大陆时区）
 * - 代码中混用系统时间（LocalDateTime.now()）和UTC时间（ZoneOffset.UTC）进行epoch秒数计算
 * - 由于时区固定，8小时的恒定偏移不会影响功能正确性（所有时间戳都以相同方式处理）
 * - 如需部署到其他时区，需要统一时区处理逻辑
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
                // 例如：2025-11-02T13:47:46.322238800 -> 2025-11-02T13:47:46.322238800（正常）
                // 或者：2025-11-02T13:47:46.322 -> 2025-11-02T13:47:46.322
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
                // If no decimal point, parse directly
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
            // 获取当前时间片key（按小时分片）
            String timeSliceKey = getTimeSliceKey(timestamp, COMMODITY_CHANGE_PREFIX);
            
            // 将时间戳转换为Unix时间戳（秒）作为score
            // Note: timestamp是系统时间（GMT+8），但使用UTC计算epoch秒数
            // 由于项目固定部署在GMT+8时区，这种混用不会影响功能正确性
            long score = timestamp.toEpochSecond(ZoneOffset.UTC);
            
            // 构建变更记录JSON字符串
            // 格式: {"commodityId":"xxx","operation":"UPDATE","timestamp":"2025-01-20T10:30:00"}
            String record = String.format(
                "{\"commodityId\":\"%s\",\"operation\":\"%s\",\"timestamp\":\"%s\"}",
                commodityId, operation, timestamp.toString()
            );
            
            // 添加到对应时间片的Sorted Set
            redisTemplate.opsForZSet().add(timeSliceKey, record, score);
            
            // ✅ 优化：每次添加数据时更新当前时间片的TTL（确保数据不会过早过期）
            // 只有当前活跃的时间片会保持TTL，旧时间片自动过期
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
            // 获取当前时间片key（按小时分片）
            String timeSliceKey = getTimeSliceKey(timestamp, ORDER_CHANGE_PREFIX);
            
            // 将时间戳转换为Unix时间戳（秒）作为score
            // Note: timestamp是系统时间（GMT+8），但使用UTC计算epoch秒数
            // 由于项目固定部署在GMT+8时区，这种混用不会影响功能正确性
            long score = timestamp.toEpochSecond(ZoneOffset.UTC);
            
            // 构建变更记录JSON字符串
            String record = String.format(
                "{\"orderId\":\"%s\",\"operation\":\"%s\",\"timestamp\":\"%s\"}",
                orderId, operation, timestamp.toString()
            );
            
            // 添加到对应时间片的Sorted Set
            redisTemplate.opsForZSet().add(timeSliceKey, record, score);
            
            // ✅ 优化：每次添加数据时更新当前时间片的TTL
            redisTemplate.expire(timeSliceKey, java.time.Duration.ofSeconds(TTL_SECONDS));
            
            log.debug("记录订单变更: orderId={}, operation={}, timestamp={}, timeSlice={}", 
                orderId, operation, timestamp, timeSliceKey);
                
        } catch (Exception e) {
            log.error("记录订单变更失败: orderId={}, error={}", orderId, e.getMessage(), e);
        }
    }
    
    @Override
    public List<String> getCommodityChangesAfter(LocalDateTime afterTimestamp) {
        try {
            // 将时间戳转换为Unix时间戳（秒）
            // Note: afterTimestamp是系统时区时间，但使用UTC计算epoch秒数进行ZSet查询
            // 由于记录时也使用相同方式计算score，查询时保持一致即可
            long minScore = afterTimestamp.toEpochSecond(ZoneOffset.UTC);
            
            // ✅ 优化：使用 exclusive 查询（严格大于 afterTimestamp），避免重复查询相同时间戳的记录
            // 将 minScore 加 1，实现 exclusive（不包含边界）
            long exclusiveMinScore = minScore + 1;
            
            // ✅ 时间分片查询：获取从afterTimestamp到现在的所有时间片key
            LocalDateTime now = LocalDateTime.now();
            List<String> timeSliceKeys = getTimeSliceKeys(afterTimestamp, now, COMMODITY_CHANGE_PREFIX);
            
            // 从所有相关时间片中查询数据（并行查询）
            Set<String> allRecords = new HashSet<>();
            for (String timeSliceKey : timeSliceKeys) {
                try {
                    // 检查key是否存在（避免查询不存在的key）
                    Boolean exists = redisTemplate.hasKey(timeSliceKey);
                    if (Boolean.TRUE.equals(exists)) {
                        // ✅ 使用 exclusiveMinScore 实现严格大于查询（ZSet层面过滤）
                        Set<Object> records = redisTemplate.opsForZSet()
                            .rangeByScore(timeSliceKey, exclusiveMinScore, Double.MAX_VALUE);
                        if (records != null) {
                            for (Object record : records) {
                                if (record != null) {
                                    allRecords.add(record.toString());
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    // 某个时间片查询失败不影响其他时间片
                    log.warn("Failed to query time slice: timeSliceKey={}, error={}", timeSliceKey, e.getMessage());
                }
            }
            
            // ✅ 应用层精确过滤：解析JSON中的timestamp字段，只返回严格大于afterTimestamp的记录
            // 这样可以避免因为ZSet score精度（秒级）导致的重复查询问题
            List<String> result = new ArrayList<>();
            int filteredCount = 0;
            int passedCount = 0;
            
            log.debug("Starting application-layer filtering - afterTimestamp={}, ZSet returned {} records", 
                afterTimestamp, allRecords.size());
            
            for (String record : allRecords) {
                try {
                    // Parse JSON record
                    @SuppressWarnings("unchecked")
                    Map<String, Object> recordMap = objectMapper.readValue(record, Map.class);
                    Object timestampObj = recordMap.get("timestamp");
                    Object commodityIdObj = recordMap.get("commodityId");
                    
                    if (timestampObj != null) {
                        // Parse timestamp string to LocalDateTime
                        String timestampStr = timestampObj.toString();
                        LocalDateTime recordTimestamp = parseTimestamp(timestampStr);
                        
                        // Only keep records strictly greater than afterTimestamp (precise comparison to avoid duplicates)
                        if (recordTimestamp != null) {
                            boolean isAfter = recordTimestamp.isAfter(afterTimestamp);
                            
                            if (isAfter) {
                                result.add(record);
                                passedCount++;
                                log.debug("Record passed filter - commodityId={}, recordTimestamp={}, afterTimestamp={}, isAfter={}", 
                                    commodityIdObj, recordTimestamp, afterTimestamp, isAfter);
                            } else {
                                filteredCount++;
                                log.warn("Record filtered (timestamp not greater than afterTimestamp) - commodityId={}, recordTimestamp={}, afterTimestamp={}, isAfter={}", 
                                    commodityIdObj, recordTimestamp, afterTimestamp, isAfter);
                            }
                        } else {
                            // Parse failed, but keep record (backward compatibility)
                            log.warn("Timestamp parse failed, keeping record - commodityId={}, timestampStr={}", 
                                commodityIdObj, timestampStr);
                            result.add(record);
                            passedCount++;
                        }
                    } else {
                        // If JSON has no timestamp field, keep record (backward compatibility)
                        log.warn("JSON has no timestamp field, keeping record - commodityId={}", commodityIdObj);
                        result.add(record);
                        passedCount++;
                    }
                } catch (Exception e) {
                    // Parse failed, keep original record (backward compatibility)
                    log.warn("Failed to parse change record, keeping record - record={}, error={}", record, e.getMessage());
                    result.add(record);
                    passedCount++;
                }
            }
            
            log.info("Application-layer filtering completed - afterTimestamp={}, timeSlices={}, ZSet returned {} records, filtered {} records, passed {} records, final result {} records", 
                afterTimestamp, timeSliceKeys.size(), allRecords.size(), filteredCount, passedCount, result.size());
            
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
            // 将时间戳转换为Unix时间戳（秒）
            // Note: afterTimestamp是系统时区时间，但使用UTC计算epoch秒数进行ZSet查询
            // 由于记录时也使用相同方式计算score，查询时保持一致即可
            long minScore = afterTimestamp.toEpochSecond(ZoneOffset.UTC);
            
            // ✅ 优化：使用 exclusive 查询（严格大于 afterTimestamp），避免重复查询相同时间戳的记录
            // 将 minScore 加 1，实现 exclusive（不包含边界）
            long exclusiveMinScore = minScore + 1;
            
            // ✅ 时间分片查询：获取从afterTimestamp到现在的所有时间片key
            LocalDateTime now = LocalDateTime.now();
            List<String> timeSliceKeys = getTimeSliceKeys(afterTimestamp, now, ORDER_CHANGE_PREFIX);
            
            // 从所有相关时间片中查询数据
            Set<String> allRecords = new HashSet<>();
            for (String timeSliceKey : timeSliceKeys) {
                try {
                    // 检查key是否存在（避免查询不存在的key）
                    Boolean exists = redisTemplate.hasKey(timeSliceKey);
                    if (Boolean.TRUE.equals(exists)) {
                        // ✅ 使用 exclusiveMinScore 实现严格大于查询（ZSet层面过滤）
                        Set<Object> records = redisTemplate.opsForZSet()
                            .rangeByScore(timeSliceKey, exclusiveMinScore, Double.MAX_VALUE);
                        if (records != null) {
                            for (Object record : records) {
                                if (record != null) {
                                    allRecords.add(record.toString());
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    // 某个时间片查询失败不影响其他时间片
                    log.warn("Failed to query time slice: timeSliceKey={}, error={}", timeSliceKey, e.getMessage());
                }
            }
            
            // ✅ 应用层精确过滤：解析JSON中的timestamp字段，只返回严格大于afterTimestamp的记录
            // 这样可以避免因为ZSet score精度（秒级）导致的重复查询问题
            List<String> result = new ArrayList<>();
            int filteredCount = 0;
            int passedCount = 0;
            
            log.debug("Starting application-layer filtering (orders) - afterTimestamp={}, ZSet returned {} records", 
                afterTimestamp, allRecords.size());
            
            for (String record : allRecords) {
                try {
                    // Parse JSON record
                    @SuppressWarnings("unchecked")
                    Map<String, Object> recordMap = objectMapper.readValue(record, Map.class);
                    Object timestampObj = recordMap.get("timestamp");
                    Object orderIdObj = recordMap.get("orderId");
                    
                    if (timestampObj != null) {
                        // Parse timestamp string to LocalDateTime
                        String timestampStr = timestampObj.toString();
                        LocalDateTime recordTimestamp = parseTimestamp(timestampStr);
                        
                        // Only keep records strictly greater than afterTimestamp (precise comparison to avoid duplicates)
                        if (recordTimestamp != null) {
                            boolean isAfter = recordTimestamp.isAfter(afterTimestamp);
                            
                            if (isAfter) {
                                result.add(record);
                                passedCount++;
                                log.debug("Order record passed filter - orderId={}, recordTimestamp={}, afterTimestamp={}, isAfter={}", 
                                    orderIdObj, recordTimestamp, afterTimestamp, isAfter);
                            } else {
                                filteredCount++;
                                log.warn("Order record filtered (timestamp not greater than afterTimestamp) - orderId={}, recordTimestamp={}, afterTimestamp={}, isAfter={}", 
                                    orderIdObj, recordTimestamp, afterTimestamp, isAfter);
                            }
                        } else {
                            // Parse failed, but keep record (backward compatibility)
                            log.warn("Order timestamp parse failed, keeping record - orderId={}, timestampStr={}", 
                                orderIdObj, timestampStr);
                            result.add(record);
                            passedCount++;
                        }
                    } else {
                        // If JSON has no timestamp field, keep record (backward compatibility)
                        log.warn("JSON has no timestamp field, keeping order record - orderId={}", orderIdObj);
                        result.add(record);
                        passedCount++;
                    }
                } catch (Exception e) {
                    // Parse failed, keep original record (backward compatibility)
                    log.warn("Failed to parse order change record, keeping record - record={}, error={}", record, e.getMessage());
                    result.add(record);
                    passedCount++;
                }
            }
            
            log.info("Application-layer filtering completed (orders) - afterTimestamp={}, timeSlices={}, ZSet returned {} records, filtered {} records, passed {} records, final result {} records", 
                afterTimestamp, timeSliceKeys.size(), allRecords.size(), filteredCount, passedCount, result.size());
            
            return result;
            
        } catch (Exception e) {
            log.error("Failed to query order change records: afterTimestamp={}, error={}", 
                afterTimestamp, e.getMessage(), e);
            return new ArrayList<>();
        }
    }
}

