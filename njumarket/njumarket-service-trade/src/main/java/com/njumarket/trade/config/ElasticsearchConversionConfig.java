package com.njumarket.trade.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.data.convert.WritingConverter;
import org.springframework.data.elasticsearch.core.convert.ElasticsearchCustomConversions;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

@Configuration
public class ElasticsearchConversionConfig {

    private static final DateTimeFormatter OFFSET_WITH_SPACE =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ssX");

    @Bean
    public ElasticsearchCustomConversions elasticsearchCustomConversions() {
        return new ElasticsearchCustomConversions(
            List.of(
                new ObjectToLocalDateTimeConverter(),  // 优先处理 Object 类型（ES 可能返回多种类型）
                new StringToLocalDateTimeConverter(),
                new LocalDateToLocalDateTimeConverter(),
                new LocalDateTimeToStringConverter()
            )
        );
    }
    
    @ReadingConverter
    private static final class ObjectToLocalDateTimeConverter implements Converter<Object, LocalDateTime> {
        @Override
        public LocalDateTime convert(Object source) {
            if (source == null) {
                return null;
            }
            
            // 如果是字符串，直接解析
            if (source instanceof String) {
                String str = (String) source;
                if (!StringUtils.hasText(str)) {
                    return null;
                }
                String trimmed = str.trim();
                
                // 优先尝试解析为 LocalDate（格式：yyyy-MM-dd），这是 ES date_optional_time 格式可能返回的
                try {
                    LocalDate date = LocalDate.parse(trimmed);
                    return date.atStartOfDay();
                } catch (DateTimeParseException e) {
                    // 继续尝试其他格式
                }
                
                // 尝试解析为 LocalDateTime（ISO 格式：yyyy-MM-ddTHH:mm:ss）
                try {
                    return LocalDateTime.parse(trimmed);
                } catch (DateTimeParseException e) {
                    // 继续尝试其他格式
                }
                
                // 尝试解析为带空格的格式（yyyy-MM-dd HH:mm:ss）
                try {
                    return LocalDateTime.parse(trimmed.replace(" ", "T"));
                } catch (DateTimeParseException e) {
                    // 继续尝试其他格式
                }
                
                // 尝试解析为带时区的格式（PostgreSQL 格式：yyyy-MM-dd HH:mm:ss.ffffff+08）
                try {
                    // 处理 PostgreSQL 格式：2025-11-19 19:22:37.953111+08
                    // 先尝试直接解析 OffsetDateTime
                    String normalized = trimmed.replace(" ", "T");
                    // 如果包含时区信息但没有 T，添加 T
                    if (normalized.contains("+") || normalized.endsWith("Z") || 
                        (normalized.length() > 10 && normalized.charAt(10) != 'T')) {
                        if (normalized.charAt(10) != 'T') {
                            normalized = normalized.substring(0, 10) + "T" + normalized.substring(11);
                        }
                    }
                    OffsetDateTime offset = OffsetDateTime.parse(normalized);
                    // 截断到秒级精度
                    return offset.toLocalDateTime().truncatedTo(java.time.temporal.ChronoUnit.SECONDS);
                } catch (DateTimeParseException e) {
                    // 尝试解析为带微秒的格式：2025-11-19 19:22:37.953111+08
                    try {
                        // 移除微秒部分，只保留秒级
                        String withoutMicroseconds = trimmed.replaceAll("\\.\\d{6}", "");  // 移除 .953111
                        String normalized = withoutMicroseconds.replace(" ", "T");
                        if (normalized.charAt(10) != 'T') {
                            normalized = normalized.substring(0, 10) + "T" + normalized.substring(11);
                        }
                        OffsetDateTime offset = OffsetDateTime.parse(normalized);
                        return offset.toLocalDateTime().truncatedTo(java.time.temporal.ChronoUnit.SECONDS);
                    } catch (DateTimeParseException e2) {
                        // 转换失败，记录警告但不抛出异常
                        System.err.println("警告：无法将字符串 '" + trimmed + "' 转换为 LocalDateTime，返回 null");
                        return null;
                    }
                }
            }
            
            // 如果是 LocalDate，直接转换
            if (source instanceof LocalDate) {
                return ((LocalDate) source).atStartOfDay();
            }
            
            // 如果是 Long（时间戳），转换为 LocalDateTime
            if (source instanceof Long) {
                try {
                    return java.time.Instant.ofEpochMilli((Long) source)
                            .atZone(java.time.ZoneId.systemDefault())
                            .toLocalDateTime();
                } catch (Exception e) {
                    System.err.println("警告：无法将 Long " + source + " 转换为 LocalDateTime，返回 null");
                    return null;
                }
            }
            
            // 其他类型，尝试转换为字符串再解析
            try {
                String str = source.toString().trim();
                // 优先尝试 LocalDate 格式
                try {
                    LocalDate date = LocalDate.parse(str);
                    return date.atStartOfDay();
                } catch (DateTimeParseException e) {
                    // 继续尝试 LocalDateTime 格式
                    return LocalDateTime.parse(str);
                }
            } catch (Exception e) {
                // 转换失败，记录警告但不抛出异常
                System.err.println("警告：无法将对象 " + source.getClass().getName() + " (" + source + ") 转换为 LocalDateTime，返回 null");
                return null;
            }
        }
    }

    @ReadingConverter
    private static final class StringToLocalDateTimeConverter implements Converter<String, LocalDateTime> {
        @Override
        public LocalDateTime convert(String source) {
            if (!StringUtils.hasText(source)) {
                return null;
            }
            
            String trimmed = source.trim();
            
            // 优先尝试解析为 LocalDate（格式：yyyy-MM-dd），这是 ES date_optional_time 格式可能返回的
            try {
                LocalDate date = LocalDate.parse(trimmed);
                return date.atStartOfDay();
            } catch (DateTimeParseException e) {
                // 继续尝试其他格式
            }
            
            // 尝试解析为 LocalDateTime（ISO 格式：yyyy-MM-ddTHH:mm:ss）
            try {
                return LocalDateTime.parse(trimmed);
            } catch (DateTimeParseException e) {
                // 继续尝试其他格式
            }
            
            // 尝试解析为带空格的格式（yyyy-MM-dd HH:mm:ss）
            try {
                return LocalDateTime.parse(trimmed.replace(" ", "T"));
            } catch (DateTimeParseException e) {
                // 继续尝试其他格式
            }
            
            // 尝试解析为 OffsetDateTime（PostgreSQL 格式：yyyy-MM-dd HH:mm:ss.ffffff+08）
            try {
                // 处理 PostgreSQL 格式：2025-11-19 19:22:37.953111+08
                String normalized = trimmed.replace(" ", "T");
                // 如果包含时区信息但没有 T，添加 T
                if (normalized.contains("+") || normalized.endsWith("Z") || 
                    (normalized.length() > 10 && normalized.charAt(10) != 'T')) {
                    if (normalized.charAt(10) != 'T') {
                        normalized = normalized.substring(0, 10) + "T" + normalized.substring(11);
                    }
                }
                OffsetDateTime offset = OffsetDateTime.parse(normalized);
                // 截断到秒级精度
                return offset.toLocalDateTime().truncatedTo(java.time.temporal.ChronoUnit.SECONDS);
            } catch (DateTimeParseException e) {
                // 尝试解析为带微秒的格式：2025-11-19 19:22:37.953111+08
                try {
                    // 移除微秒部分，只保留秒级
                    String withoutMicroseconds = trimmed.replaceAll("\\.\\d{6}", "");  // 移除 .953111
                    String normalized = withoutMicroseconds.replace(" ", "T");
                    if (normalized.charAt(10) != 'T') {
                        normalized = normalized.substring(0, 10) + "T" + normalized.substring(11);
                    }
                    OffsetDateTime offset = OffsetDateTime.parse(normalized);
                    return offset.toLocalDateTime().truncatedTo(java.time.temporal.ChronoUnit.SECONDS);
                } catch (DateTimeParseException e2) {
                    // 继续尝试其他格式
                }
            }
            
            // 尝试自定义格式
            try {
                OffsetDateTime offset = OffsetDateTime.parse(trimmed, OFFSET_WITH_SPACE);
                return offset.toLocalDateTime();
            } catch (DateTimeParseException e) {
                // 所有格式都失败，返回 null
            }
            
            // 无法解析的字符串（例如 UUID、hash），直接返回 null，避免抛出异常
            return null;
        }
    }

    @WritingConverter
    private static final class LocalDateTimeToStringConverter implements Converter<LocalDateTime, String> {
        @Override
        public String convert(LocalDateTime source) {
            return source == null ? null : source.toString();
        }
    }

    private static final class LocalDateToLocalDateTimeConverter implements Converter<LocalDate, LocalDateTime> {
        @Override
        public LocalDateTime convert(LocalDate source) {
            return source == null ? null : source.atStartOfDay();
        }
    }

    @FunctionalInterface
    private interface Parser<T> {
        T parse();
    }
}

