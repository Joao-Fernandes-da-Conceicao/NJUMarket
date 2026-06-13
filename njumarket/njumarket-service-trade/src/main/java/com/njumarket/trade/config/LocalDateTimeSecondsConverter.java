package com.njumarket.trade.config;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;

/**
 * LocalDateTime 与 PostgreSQL timestamp with time zone 的转换器
 * 
 * 功能：
 * 1. 将 LocalDateTime 截断到秒级精度（去除纳秒部分）
 * 2. 正确处理时区转换（PostgreSQL 存储为 UTC，读取时转换为系统时区）
 * 3. 确保存储格式为：yyyy-MM-dd HH:mm:ss+08（秒级精度）
 */
@Converter(autoApply = true)
@Component
public class LocalDateTimeSecondsConverter implements AttributeConverter<LocalDateTime, Timestamp> {

    /**
     * 将 LocalDateTime 转换为 Timestamp（写入数据库）
     * 
     * 1. 截断到秒级精度（去除纳秒部分）
     * 2. 转换为 UTC 时区的 Instant（PostgreSQL timestamp with time zone 内部存储为 UTC）
     * 3. 创建 Timestamp 对象
     */
    @Override
    public Timestamp convertToDatabaseColumn(LocalDateTime attribute) {
        if (attribute == null) {
            return null;
        }
        
        // 截断到秒级精度（去除纳秒部分）
        LocalDateTime truncated = attribute.truncatedTo(ChronoUnit.SECONDS);
        
        // 将 LocalDateTime 转换为 UTC 时区的 Instant
        // 假设 LocalDateTime 是系统时区（中国时区 UTC+8）
        Instant instant = truncated.atZone(ZoneId.systemDefault())
                .withZoneSameInstant(ZoneOffset.UTC)
                .toInstant();
        
        // 创建 Timestamp（精度为秒级）
        return Timestamp.from(instant);
    }

    /**
     * 将 Timestamp 转换为 LocalDateTime（从数据库读取）
     * 
     * 1. Timestamp 内部存储为 UTC 时间
     * 2. 转换为系统时区的 LocalDateTime
     * 3. 截断到秒级精度
     */
    @Override
    public LocalDateTime convertToEntityAttribute(Timestamp dbData) {
        if (dbData == null) {
            return null;
        }
        
        // Timestamp 内部存储为 UTC 时间，转换为系统时区的 LocalDateTime
        LocalDateTime localDateTime = dbData.toInstant()
                .atZone(ZoneOffset.UTC)
                .withZoneSameInstant(ZoneId.systemDefault())
                .toLocalDateTime();
        
        // 截断到秒级精度（确保没有纳秒部分）
        return localDateTime.truncatedTo(ChronoUnit.SECONDS);
    }
}

